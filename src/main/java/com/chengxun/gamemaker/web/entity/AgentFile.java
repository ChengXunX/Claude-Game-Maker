package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Agent 文件实体
 * 管理 Agent 的文件收发、版本历史、配额控制
 *
 * @author chengxun
 * @since 2.0.0
 */
@Entity
@Table(name = "agent_files", indexes = {
    @Index(name = "idx_af_agent", columnList = "agentId"),
    @Index(name = "idx_af_project", columnList = "projectId"),
    @Index(name = "idx_af_name", columnList = "fileName"),
    @Index(name = "idx_af_source", columnList = "source"),
    @Index(name = "idx_af_created", columnList = "createdAt")
})
public class AgentFile {

    /** 文件来源 */
    public enum FileSource {
        /** 用户上传 */
        USER_UPLOAD,
        /** Agent 生成 */
        AGENT_GENERATED,
        /** MCP 工具 */
        MCP,
        /** 其他 Agent */
        AGENT_TRANSFER
    }

    /** 传输方向 */
    public enum FileDirection {
        /** 入站（用户/外部 → Agent） */
        INBOUND,
        /** 出站（Agent → 用户/外部） */
        OUTBOUND
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Agent 运行时 ID */
    @Column(nullable = false, length = 200)
    private String agentId;

    /** 项目 ID */
    @Column(nullable = false, length = 100)
    private String projectId;

    /** 文件名 */
    @Column(nullable = false, length = 500)
    private String fileName;

    /** 存储路径（相对路径） */
    @Column(nullable = false, length = 1000)
    private String filePath;

    /** 文件大小（字节） */
    @Column(nullable = false)
    private long fileSize;

    /** MIME 类型 */
    @Column(length = 100)
    private String mimeType;

    /** 文件来源 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FileSource source;

    /** 传输方向 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FileDirection direction;

    /** 版本号（从 1 开始） */
    @Column(nullable = false)
    private int version = 1;

    /** 父版本 ID（用于版本链） */
    private Long parentVersionId;

    /** 文件哈希（SHA-256） */
    @Column(length = 64)
    private String fileHash;

    /** 上传者（用户名或 agentId） */
    @Column(length = 200)
    private String createdBy;

    /** 备注 */
    @Column(length = 500)
    private String remark;

    /** 是否已删除（软删除） */
    @Column(nullable = false)
    private boolean deleted = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ===== Getters & Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public FileSource getSource() { return source; }
    public void setSource(FileSource source) { this.source = source; }

    public FileDirection getDirection() { return direction; }
    public void setDirection(FileDirection direction) { this.direction = direction; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public Long getParentVersionId() { return parentVersionId; }
    public void setParentVersionId(Long parentVersionId) { this.parentVersionId = parentVersionId; }

    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return String.format("AgentFile[%s] agent=%s, size=%d, v%d", fileName, agentId, fileSize, version);
    }
}
