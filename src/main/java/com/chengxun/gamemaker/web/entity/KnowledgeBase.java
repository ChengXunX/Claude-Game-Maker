package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * AI 知识库实体
 * 存储系统知识、项目知识和提示词模板
 *
 * @author chengxun
 * @since 1.0.0
 */
@Entity
@Table(name = "knowledge_base")
public class KnowledgeBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 知识类别：system, project, prompt_template, security_rule */
    @Column(name = "category", nullable = false, length = 50)
    private String category;

    /** 知识键（唯一标识） */
    @Column(name = "knowledge_key", nullable = false, length = 200)
    private String knowledgeKey;

    /** 知识标题 */
    @Column(name = "title", nullable = false, length = 500)
    private String title;

    /** 知识内容 */
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /** 所属项目 ID（null 表示系统级知识） */
    @Column(name = "project_id", length = 100)
    private String projectId;

    /** 访问级别：public, project, admin, system */
    @Column(name = "access_level", nullable = false, length = 20)
    private String accessLevel = "project";

    /** 所需权限（逗号分隔） */
    @Column(name = "required_permissions", length = 500)
    private String requiredPermissions;

    /** 标签（逗号分隔） */
    @Column(name = "tags", length = 500)
    private String tags;

    /** 是否启用 */
    @Column(name = "enabled")
    private boolean enabled = true;

    /** 优先级（用于排序） */
    @Column(name = "priority")
    private Integer priority = 10;

    /** 创建者 */
    @Column(name = "created_by", length = 100)
    private String createdBy;

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    /** 使用次数 */
    @Column(name = "usage_count")
    private Long usageCount = 0L;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getKnowledgeKey() { return knowledgeKey; }
    public void setKnowledgeKey(String knowledgeKey) { this.knowledgeKey = knowledgeKey; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getAccessLevel() { return accessLevel; }
    public void setAccessLevel(String accessLevel) { this.accessLevel = accessLevel; }

    public String getRequiredPermissions() { return requiredPermissions; }
    public void setRequiredPermissions(String requiredPermissions) { this.requiredPermissions = requiredPermissions; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Long getUsageCount() { return usageCount; }
    public void setUsageCount(Long usageCount) { this.usageCount = usageCount; }

    public void incrementUsage() {
        this.usageCount = (this.usageCount == null ? 0 : this.usageCount) + 1;
    }
}
