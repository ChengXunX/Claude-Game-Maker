package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 文档索引实体
 * 用于快速搜索和检索文档
 *
 * @author chengxun
 * @since 1.0.0
 */
@Entity
@Table(name = "document_index", indexes = {
    @Index(name = "idx_doc_path", columnList = "filePath"),
    @Index(name = "idx_doc_type", columnList = "docType"),
    @Index(name = "idx_doc_agent", columnList = "agentId"),
    @Index(name = "idx_doc_project", columnList = "projectId")
})
public class DocumentIndex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 文件路径 */
    @Column(nullable = false)
    private String filePath;

    /** 文件名 */
    @Column(nullable = false)
    private String fileName;

    /** 文档类型 */
    @NotBlank(message = "docType 不能为空")
    @Column(nullable = false)
    private String docType;

    /** 关联的 Agent ID */
    private String agentId;

    /** 关联的项目 ID */
    private String projectId;

    /** 文档标题 */
    private String title;

    /** 文档摘要 */
    @Column(length = 500)
    private String summary;

    /** 关键词（逗号分隔） */
    @Column(length = 500)
    private String keywords;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 内容哈希（用于去重） */
    private String contentHash;

    /** 创建时间 */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /** 版本号 */
    @Column(nullable = false)
    private Integer version = 1;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        version++;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
