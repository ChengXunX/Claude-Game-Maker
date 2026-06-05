package com.chengxun.gamemaker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 文件备份服务
 * 定期备份重要文件到 data/backups/ 目录
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class FileBackupService {

    private static final Logger log = LoggerFactory.getLogger(FileBackupService.class);

    /** 备份目录 */
    private static final String BACKUP_DIR = "data/backups";

    /** 需要备份的目录 */
    private static final String[] BACKUP_DIRS = {
        "data/memory",
        "data/contexts",
        "data/knowledge-base",
        "data/settings.properties"
    };

    /** 最大备份数量 */
    private static final int MAX_BACKUPS = 10;

    /**
     * 执行备份（每天凌晨 2 点）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledBackup() {
        log.info("开始定时备份...");
        BackupResult result = performBackup();
        log.info("定时备份完成: 文件数={}, 大小={}KB, 耗时={}ms",
            result.getFileCount(), result.getTotalSize() / 1024, result.getDurationMs());
    }

    /**
     * 执行备份
     */
    public BackupResult performBackup() {
        long startTime = System.currentTimeMillis();
        BackupResult result = new BackupResult();

        try {
            // 创建备份目录
            Path backupDir = Path.of(BACKUP_DIR);
            Files.createDirectories(backupDir);

            // 生成备份文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String backupFileName = "backup_" + timestamp + ".zip";
            Path backupPath = backupDir.resolve(backupFileName);

            // 创建 ZIP 文件
            AtomicInteger fileCount = new AtomicInteger(0);
            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(backupPath))) {
                for (String dir : BACKUP_DIRS) {
                    Path sourcePath = Path.of(dir);
                    if (Files.exists(sourcePath)) {
                        if (Files.isDirectory(sourcePath)) {
                            addDirectoryToZip(zos, sourcePath, sourcePath, fileCount);
                        } else {
                            addFileToZip(zos, sourcePath, sourcePath.getParent(), fileCount);
                        }
                    }
                }
            }

            result.setSuccess(true);
            result.setBackupPath(backupPath.toString());
            result.setFileCount(fileCount.get());
            result.setTotalSize(Files.size(backupPath));
            result.setDurationMs(System.currentTimeMillis() - startTime);

            // 清理旧备份
            cleanupOldBackups();

        } catch (Exception e) {
            result.setSuccess(false);
            result.setError(e.getMessage());
            log.error("备份失败", e);
        }

        return result;
    }

    /**
     * 添加目录到 ZIP
     */
    private void addDirectoryToZip(ZipOutputStream zos, Path source, Path root, AtomicInteger count) throws IOException {
        try (Stream<Path> stream = Files.walk(source)) {
            stream.filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        addFileToZip(zos, path, root, count);
                    } catch (IOException e) {
                        log.warn("跳过文件: {}", path, e);
                    }
                });
        }
    }

    /**
     * 添加文件到 ZIP
     */
    private void addFileToZip(ZipOutputStream zos, Path file, Path root, AtomicInteger count) throws IOException {
        String entryName = root.relativize(file).toString().replace("\\", "/");
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        Files.copy(file, zos);
        zos.closeEntry();
        count.incrementAndGet();
    }

    /**
     * 清理旧备份
     */
    private void cleanupOldBackups() {
        try {
            Path backupDir = Path.of(BACKUP_DIR);
            if (!Files.exists(backupDir)) return;

            List<Path> backups = new ArrayList<>();
            try (Stream<Path> stream = Files.list(backupDir)) {
                stream.filter(p -> p.toString().endsWith(".zip"))
                    .sorted(Comparator.reverseOrder())
                    .forEach(backups::add);
            }

            // 删除多余的备份
            if (backups.size() > MAX_BACKUPS) {
                for (int i = MAX_BACKUPS; i < backups.size(); i++) {
                    Files.delete(backups.get(i));
                    log.info("删除旧备份: {}", backups.get(i).getFileName());
                }
            }
        } catch (IOException e) {
            log.error("清理旧备份失败", e);
        }
    }

    /**
     * 获取备份列表
     */
    public List<BackupInfo> getBackupList() {
        List<BackupInfo> backups = new ArrayList<>();
        Path backupDir = Path.of(BACKUP_DIR);

        if (!Files.exists(backupDir)) {
            return backups;
        }

        try (Stream<Path> stream = Files.list(backupDir)) {
            stream.filter(p -> p.toString().endsWith(".zip"))
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                        BackupInfo info = new BackupInfo();
                        info.setFileName(path.getFileName().toString());
                        info.setFilePath(path.toString());
                        info.setFileSize(attrs.size());
                        info.setCreatedAt(LocalDateTime.ofInstant(attrs.creationTime().toInstant(), java.time.ZoneId.systemDefault()));
                        backups.add(info);
                    } catch (IOException e) {
                        log.warn("读取备份信息失败: {}", path, e);
                    }
                });
        } catch (IOException e) {
            log.error("获取备份列表失败", e);
        }

        return backups;
    }

    // ===== 内部类 =====

    public static class BackupResult {
        private boolean success;
        private String backupPath;
        private int fileCount;
        private long totalSize;
        private long durationMs;
        private String error;

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getBackupPath() { return backupPath; }
        public void setBackupPath(String backupPath) { this.backupPath = backupPath; }
        public int getFileCount() { return fileCount; }
        public void setFileCount(int fileCount) { this.fileCount = fileCount; }
        public long getTotalSize() { return totalSize; }
        public void setTotalSize(long totalSize) { this.totalSize = totalSize; }
        public long getDurationMs() { return durationMs; }
        public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }

    public static class BackupInfo {
        private String fileName;
        private String filePath;
        private long fileSize;
        private LocalDateTime createdAt;

        // Getters and Setters
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}
