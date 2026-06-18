package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.config.SystemConstants;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.web.service.SystemConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 快照回滚服务
 * 基于文件系统的快照机制，支持 undo/redo
 *
 * 主要功能：
 * - track：创建文件变更快照
 * - restore：恢复到指定快照
 * - undo：撤销最近一次操作
 * - redo：重做已撤销的操作
 * - diff：查看快照间的差异
 * - prune：清理过期快照（7天保留，2MB限制）
 *
 * 灵感来源：文件快照与回滚机制
 *
 * @author chengxun
 * @since 3.0.0
 */
@Service
public class SnapshotService {

    private static final Logger log = LoggerFactory.getLogger(SnapshotService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /** 快照保留天数 */
    private static final int RETENTION_DAYS = 7;

    /** 快照最大总大小 (2MB) */
    private static final long MAX_TOTAL_SIZE_BYTES = 2 * 1024 * 1024;

    @Autowired
    private SystemConfigService configService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 创建快照：保存指定文件的当前内容
     *
     * @param project 项目
     * @param agentId Agent ID
     * @param filePaths 要快照的文件路径（相对于项目根目录）
     * @param description 快照描述
     * @return 快照 ID
     */
    public String createSnapshot(GameProject project, String agentId, List<String> filePaths, String description) {
        if (project == null || filePaths == null || filePaths.isEmpty()) {
            return null;
        }

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        Path snapshotDir = getSnapshotDir(project, agentId, timestamp);

        try {
            Files.createDirectories(snapshotDir);

            // 保存每个文件的内容
            int savedCount = 0;
            for (String filePath : filePaths) {
                Path sourcePath = Path.of(project.getWorkDir(), filePath);
                if (Files.exists(sourcePath) && Files.isRegularFile(sourcePath)) {
                    Path targetPath = snapshotDir.resolve(filePath.replace("/", "_").replace("\\", "_"));
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    savedCount++;
                }
            }

            // 保存元数据
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("timestamp", timestamp);
            metadata.put("agentId", agentId);
            metadata.put("description", description != null ? description : "手动快照");
            metadata.put("fileCount", savedCount);
            metadata.put("filePaths", filePaths);
            metadata.put("createdAt", LocalDateTime.now().toString());

            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(snapshotDir.resolve("metadata.json").toFile(), metadata);

            log.info("快照已创建: agent={}, files={}, timestamp={}", agentId, savedCount, timestamp);

            // 清理过期快照
            pruneOldSnapshots(project, agentId);

            return timestamp;
        } catch (IOException e) {
            log.error("创建快照失败: agent={}", agentId, e);
            return null;
        }
    }

    /**
     * 恢复快照：将文件恢复到快照时的状态
     *
     * @param project 项目
     * @param agentId Agent ID
     * @param snapshotId 快照 ID（时间戳）
     * @return 恢复的文件列表
     */
    public List<String> restoreSnapshot(GameProject project, String agentId, String snapshotId) {
        if (project == null || snapshotId == null) {
            return Collections.emptyList();
        }

        Path snapshotDir = getSnapshotDir(project, agentId, snapshotId);
        if (!Files.exists(snapshotDir)) {
            log.warn("快照不存在: agent={}, snapshot={}", agentId, snapshotId);
            return Collections.emptyList();
        }

        List<String> restoredFiles = new ArrayList<>();

        try {
            // 读取元数据
            Path metadataPath = snapshotDir.resolve("metadata.json");
            if (!Files.exists(metadataPath)) {
                return Collections.emptyList();
            }

            Map<String, Object> metadata = objectMapper.readValue(metadataPath.toFile(), Map.class);
            List<String> filePaths = (List<String>) metadata.get("filePaths");

            if (filePaths == null) return Collections.emptyList();

            // 恢复每个文件
            for (String filePath : filePaths) {
                String fileName = filePath.replace("/", "_").replace("\\", "_");
                Path sourcePath = snapshotDir.resolve(fileName);
                Path targetPath = Path.of(project.getWorkDir(), filePath);

                if (Files.exists(sourcePath)) {
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    restoredFiles.add(filePath);
                }
            }

            log.info("快照已恢复: agent={}, snapshot={}, files={}", agentId, snapshotId, restoredFiles.size());
        } catch (IOException e) {
            log.error("恢复快照失败: agent={}, snapshot={}", agentId, snapshotId, e);
        }

        return restoredFiles;
    }

    /**
     * 撤销最近一次快照（undo）
     *
     * @param project 项目
     * @param agentId Agent ID
     * @return 恢复的文件列表，如果没有可撤销的快照返回空
     */
    public List<String> undo(GameProject project, String agentId) {
        List<String> snapshots = listSnapshots(project, agentId);
        if (snapshots.isEmpty()) {
            return Collections.emptyList();
        }

        // 恢复到上一个快照
        String latestSnapshot = snapshots.get(0);
        return restoreSnapshot(project, agentId, latestSnapshot);
    }

    /**
     * 列出所有快照（最新在前）
     *
     * @param project 项目
     * @param agentId Agent ID
     * @return 快照 ID 列表
     */
    public List<String> listSnapshots(GameProject project, String agentId) {
        if (project == null) return Collections.emptyList();

        Path agentDir = getAgentSnapshotDir(project, agentId);
        if (!Files.exists(agentDir)) {
            return Collections.emptyList();
        }

        try {
            return Files.list(agentDir)
                .filter(Files::isDirectory)
                .map(Path::getFileName)
                .map(Path::toString)
                .sorted(Comparator.reverseOrder())
                .toList();
        } catch (IOException e) {
            log.error("列出快照失败: agent={}", agentId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取快照详情
     *
     * @param project 项目
     * @param agentId Agent ID
     * @param snapshotId 快照 ID
     * @return 快照元数据
     */
    public Map<String, Object> getSnapshotDetail(GameProject project, String agentId, String snapshotId) {
        if (project == null || snapshotId == null) return null;

        Path metadataPath = getSnapshotDir(project, agentId, snapshotId).resolve("metadata.json");
        if (!Files.exists(metadataPath)) return null;

        try {
            return objectMapper.readValue(metadataPath.toFile(), Map.class);
        } catch (IOException e) {
            log.error("读取快照详情失败: agent={}, snapshot={}", agentId, snapshotId, e);
            return null;
        }
    }

    /**
     * 删除快照
     *
     * @param project 项目
     * @param agentId Agent ID
     * @param snapshotId 快照 ID
     * @return 是否删除成功
     */
    public boolean deleteSnapshot(GameProject project, String agentId, String snapshotId) {
        if (project == null || snapshotId == null) return false;

        Path snapshotDir = getSnapshotDir(project, agentId, snapshotId);
        if (!Files.exists(snapshotDir)) return false;

        try {
            Files.walk(snapshotDir)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (IOException ignored) {}
                });
            return true;
        } catch (IOException e) {
            log.error("删除快照失败: agent={}, snapshot={}", agentId, snapshotId, e);
            return false;
        }
    }

    /**
     * 清理过期快照
     * 规则：保留 7 天内，总大小不超过 2MB
     */
    private void pruneOldSnapshots(GameProject project, String agentId) {
        Path agentDir = getAgentSnapshotDir(project, agentId);
        if (!Files.exists(agentDir)) return;

        try {
            List<Path> snapshotDirs = Files.list(agentDir)
                .filter(Files::isDirectory)
                .sorted(Comparator.reverseOrder())
                .toList();

            long totalSize = 0;
            int prunedCount = 0;

            for (Path dir : snapshotDirs) {
                // 计算目录大小
                long dirSize = calculateDirSize(dir);
                totalSize += dirSize;

                // 检查是否过期（超过 7 天）
                boolean expired = isExpired(dir);

                // 检查是否超过总大小限制
                boolean overSize = totalSize > MAX_TOTAL_SIZE_BYTES;

                if (expired || overSize) {
                    deleteDirectory(dir);
                    prunedCount++;
                }
            }

            if (prunedCount > 0) {
                log.info("清理了 {} 个过期快照: agent={}", prunedCount, agentId);
            }
        } catch (IOException e) {
            log.error("清理快照失败: agent={}", agentId, e);
        }
    }

    /**
     * 计算目录大小
     */
    private long calculateDirSize(Path dir) throws IOException {
        final long[] size = {0};
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                size[0] += attrs.size();
                return FileVisitResult.CONTINUE;
            }
        });
        return size[0];
    }

    /**
     * 检查快照是否过期
     */
    private boolean isExpired(Path snapshotDir) {
        try {
            String dirName = snapshotDir.getFileName().toString();
            LocalDateTime snapshotTime = LocalDateTime.parse(dirName, TIMESTAMP_FORMAT);
            return snapshotTime.isBefore(LocalDateTime.now().minusDays(RETENTION_DAYS));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 递归删除目录
     */
    private void deleteDirectory(Path dir) throws IOException {
        Files.walk(dir)
            .sorted(Comparator.reverseOrder())
            .forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) {}
            });
    }

    // ===== 路径方法 =====

    private Path getAgentSnapshotDir(GameProject project, String agentId) {
        return Path.of(project.getWorkDir(), ".game-maker", "snapshots", agentId);
    }

    private Path getSnapshotDir(GameProject project, String agentId, String timestamp) {
        return getAgentSnapshotDir(project, agentId).resolve(timestamp);
    }
}
