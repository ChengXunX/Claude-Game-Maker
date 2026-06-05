package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.AgentFile;
import com.chengxun.gamemaker.web.entity.AgentFile.FileDirection;
import com.chengxun.gamemaker.web.entity.AgentFile.FileSource;
import com.chengxun.gamemaker.web.repository.AgentFileRepository;
import com.chengxun.gamemaker.web.websocket.NotificationWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Agent 文件服务
 * 管理文件上传下载、版本历史、配额控制
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
@Transactional
public class AgentFileService {

    private static final Logger log = LoggerFactory.getLogger(AgentFileService.class);
    private static final DateTimeFormatter DIR_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /** 单文件大小限制（默认 50MB） */
    @Value("${app.files.max-size:52428800}")
    private long maxFileSize;

    /** Agent 文件总配额（默认 500MB） */
    @Value("${app.files.quota:524288000}")
    private long agentQuota;

    /** 文件存储根目录 */
    @Value("${app.files.storage-dir:data/files}")
    private String storageDir;

    private final AgentFileRepository fileRepository;
    private final NotificationWebSocketHandler wsHandler;

    public AgentFileService(AgentFileRepository fileRepository,
                            NotificationWebSocketHandler wsHandler) {
        this.fileRepository = fileRepository;
        this.wsHandler = wsHandler;
    }

    // ===== 上传 =====

    /**
     * 上传文件到 Agent
     */
    public AgentFile uploadFile(String agentId, String projectId, MultipartFile file,
                                 String uploadedBy, String remark) throws IOException {
        // 验证文件大小
        if (file.getSize() > maxFileSize) {
            throw new RuntimeException(String.format("文件大小超过限制（最大 %d MB）", maxFileSize / 1024 / 1024));
        }

        // 检查配额
        long currentUsage = fileRepository.sumFileSizeByAgentId(agentId);
        if (currentUsage + file.getSize() > agentQuota) {
            throw new RuntimeException(String.format("存储空间不足（已用 %d MB，配额 %d MB）",
                currentUsage / 1024 / 1024, agentQuota / 1024 / 1024));
        }

        // 生成存储路径
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            fileName = "file_" + System.currentTimeMillis();
        }
        String relativePath = generatePath(projectId, agentId, fileName);
        Path absolutePath = Path.of(storageDir, relativePath);

        // 创建目录
        Files.createDirectories(absolutePath.getParent());

        // 保存文件
        file.transferTo(absolutePath.toFile());

        // 计算哈希
        String hash = computeHash(absolutePath);

        // 检查是否同名文件（版本递增）
        int version = 1;
        Long parentVersionId = null;
        Optional<AgentFile> existing = fileRepository
            .findFirstByAgentIdAndFileNameAndDeletedFalseOrderByVersionDesc(agentId, fileName);
        if (existing.isPresent()) {
            version = existing.get().getVersion() + 1;
            parentVersionId = existing.get().getId();
        }

        // 保存记录
        AgentFile agentFile = new AgentFile();
        agentFile.setAgentId(agentId);
        agentFile.setProjectId(projectId);
        agentFile.setFileName(fileName);
        agentFile.setFilePath(relativePath);
        agentFile.setFileSize(file.getSize());
        agentFile.setMimeType(file.getContentType());
        agentFile.setSource(FileSource.USER_UPLOAD);
        agentFile.setDirection(FileDirection.INBOUND);
        agentFile.setVersion(version);
        agentFile.setParentVersionId(parentVersionId);
        agentFile.setFileHash(hash);
        agentFile.setCreatedBy(uploadedBy);
        agentFile.setRemark(remark);

        AgentFile saved = fileRepository.save(agentFile);

        // WebSocket 推送
        sendFileNotification(agentId, "file_uploaded", saved);

        log.info("File uploaded: {} to agent {} ({} bytes, v{})", fileName, agentId, file.getSize(), version);
        return saved;
    }

    /**
     * Agent 生成文件（记录元数据，文件已存在于工作目录）
     */
    public AgentFile registerAgentFile(String agentId, String projectId, String fileName,
                                        long fileSize, FileSource source) {
        AgentFile agentFile = new AgentFile();
        agentFile.setAgentId(agentId);
        agentFile.setProjectId(projectId);
        agentFile.setFileName(fileName);
        agentFile.setFilePath(projectId + "/" + agentId + "/" + fileName);
        agentFile.setFileSize(fileSize);
        agentFile.setSource(source);
        agentFile.setDirection(FileDirection.OUTBOUND);
        agentFile.setCreatedBy(agentId);

        // 版本递增
        Optional<AgentFile> existing = fileRepository
            .findFirstByAgentIdAndFileNameAndDeletedFalseOrderByVersionDesc(agentId, fileName);
        if (existing.isPresent()) {
            agentFile.setVersion(existing.get().getVersion() + 1);
            agentFile.setParentVersionId(existing.get().getId());
        }

        AgentFile saved = fileRepository.save(agentFile);
        sendFileNotification(agentId, "file_generated", saved);

        log.info("Agent file registered: {} from agent {} ({} bytes)", fileName, agentId, fileSize);
        return saved;
    }

    // ===== 下载 =====

    /**
     * 获取文件路径（用于下载）
     */
    public Path getFilePath(Long fileId) {
        AgentFile file = fileRepository.findById(fileId)
            .orElseThrow(() -> new RuntimeException("文件不存在"));
        if (file.isDeleted()) {
            throw new RuntimeException("文件已删除");
        }
        return Path.of(storageDir, file.getFilePath());
    }

    /**
     * 获取文件元数据
     */
    public AgentFile getFile(Long fileId) {
        return fileRepository.findById(fileId)
            .orElseThrow(() -> new RuntimeException("文件不存在"));
    }

    // ===== 列表查询 =====

    /** 获取 Agent 的文件列表 */
    public Page<AgentFile> getAgentFiles(String agentId, Pageable pageable) {
        return fileRepository.findByAgentIdAndDeletedFalseOrderByCreatedAtDesc(agentId, pageable);
    }

    /** 获取 Agent 的出站文件 */
    public Page<AgentFile> getAgentOutboundFiles(String agentId, Pageable pageable) {
        return fileRepository.findByAgentIdAndDirectionAndDeletedFalseOrderByCreatedAtDesc(
            agentId, FileDirection.OUTBOUND, pageable);
    }

    /** 获取项目的文件列表 */
    public Page<AgentFile> getProjectFiles(String projectId, Pageable pageable) {
        return fileRepository.findByProjectIdAndDeletedFalseOrderByCreatedAtDesc(projectId, pageable);
    }

    /** 搜索文件 */
    public Page<AgentFile> searchFiles(String agentId, String keyword, Pageable pageable) {
        return fileRepository.searchByFileName(agentId, keyword, pageable);
    }

    /** 获取文件版本历史 */
    public List<AgentFile> getFileVersions(String agentId, String fileName) {
        return fileRepository.findByAgentIdAndFileNameAndDeletedFalseOrderByVersionDesc(agentId, fileName);
    }

    // ===== 删除 =====

    /** 软删除文件 */
    public void deleteFile(Long fileId, String deletedBy) {
        AgentFile file = fileRepository.findById(fileId)
            .orElseThrow(() -> new RuntimeException("文件不存在"));
        file.setDeleted(true);
        fileRepository.save(file);

        sendFileNotification(file.getAgentId(), "file_deleted", file);
        log.info("File deleted: {} by {}", file.getFileName(), deletedBy);
    }

    // ===== 配额查询 =====

    /** 获取 Agent 存储使用情况 */
    public Map<String, Object> getStorageUsage(String agentId) {
        long usedBytes = fileRepository.sumFileSizeByAgentId(agentId);
        long fileCount = fileRepository.countByAgentIdAndDeletedFalse(agentId);

        Map<String, Object> usage = new HashMap<>();
        usage.put("usedBytes", usedBytes);
        usage.put("usedMB", usedBytes / 1024 / 1024);
        usage.put("quotaBytes", agentQuota);
        usage.put("quotaMB", agentQuota / 1024 / 1024);
        usage.put("usagePercent", agentQuota > 0 ? Math.round(usedBytes * 100.0 / agentQuota) : 0);
        usage.put("fileCount", fileCount);
        return usage;
    }

    // ===== 工具方法 =====

    private String generatePath(String projectId, String agentId, String fileName) {
        String datePath = LocalDateTime.now().format(DIR_FORMAT);
        String safeName = fileName.replaceAll("[^a-zA-Z0-9._\\-\\u4e00-\\u9fa5]", "_");
        return projectId + "/" + agentId + "/" + datePath + "/" + System.currentTimeMillis() + "_" + safeName;
    }

    private String computeHash(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(Files.readAllBytes(path));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private void sendFileNotification(String agentId, String eventType, AgentFile file) {
        try {
            wsHandler.broadcast(Map.of(
                "type", eventType,
                "agentId", agentId,
                "fileId", file.getId(),
                "fileName", file.getFileName(),
                "fileSize", file.getFileSize(),
                "version", file.getVersion(),
                "timestamp", LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            log.warn("Failed to send file notification: {}", e.getMessage());
        }
    }
}
