package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * Git仓库实体
 * 支持一个项目下多个Git仓库（如策划、服务端、客户端）
 *
 * 主要功能：
 * - 管理多个Git仓库
 * - 支持不同目录结构
 * - 关联项目和Agent
 *
 * @author chengxun
 * @since 1.0.0
 */
@Entity
@Table(name = "git_repositories", indexes = {
    @Index(name = "idx_git_project", columnList = "projectId"),
    @Index(name = "idx_git_type", columnList = "repositoryType"),
    @Index(name = "idx_git_status", columnList = "status")
})
public class GitRepository {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 仓库名称 */
    @NotBlank(message = "name 不能为空")
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    /** 仓库描述 */
    @Column(name = "description", length = 500)
    private String description;

    /** 所属项目ID */
    @Column(name = "project_id", length = 100, nullable = false)
    private String projectId;

    /** 仓库类型 */
    @NotBlank(message = "repositoryType 不能为空")
    @Column(name = "repository_type", length = 50, nullable = false)
    private String repositoryType;

    /** Git远程仓库URL */
    @Column(name = "remote_url", length = 500)
    private String remoteUrl;

    /** 本地仓库路径 */
    @NotBlank(message = "localPath 不能为空")
    @Column(name = "local_path", length = 500, nullable = false)
    private String localPath;

    /** 相对于项目根目录的路径 */
    @Column(name = "relative_path", length = 500)
    private String relativePath;

    /** 默认分支 */
    @Column(name = "default_branch", length = 100)
    private String defaultBranch = "main";

    /** 当前分支 */
    @Column(name = "current_branch", length = 100)
    private String currentBranch = "main";

    /** 仓库状态 */
    @Column(name = "status", length = 20)
    private String status = "ACTIVE";

    /** 关联的Agent ID列表（JSON格式） */
    @Column(name = "assigned_agents", columnDefinition = "TEXT")
    private String assignedAgents;

    /** 审查规则（JSON格式） */
    @Column(name = "review_rules", columnDefinition = "TEXT")
    private String reviewRules;

    /** 是否启用自动审查 */
    @Column(name = "auto_review_enabled")
    private Boolean autoReviewEnabled = true;

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * 仓库类型枚举
     */
    public enum RepositoryType {
        /** 策划文档 */
        DESIGN("策划文档"),
        /** 服务端代码 */
        SERVER("服务端"),
        /** 客户端代码 */
        CLIENT("客户端"),
        /** 共享库 */
        SHARED("共享库"),
        /** 资源文件 */
        ASSETS("资源文件"),
        /** 配置文件 */
        CONFIG("配置文件"),
        /** 其他 */
        OTHER("其他");

        private final String displayName;

        RepositoryType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 仓库状态枚举
     */
    public enum Status {
        /** 活跃 */
        ACTIVE,
        /** 已归档 */
        ARCHIVED,
        /** 已禁用 */
        DISABLED
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getRepositoryType() { return repositoryType; }
    public void setRepositoryType(String repositoryType) { this.repositoryType = repositoryType; }

    public String getRemoteUrl() { return remoteUrl; }
    public void setRemoteUrl(String remoteUrl) { this.remoteUrl = remoteUrl; }

    public String getLocalPath() { return localPath; }
    public void setLocalPath(String localPath) { this.localPath = localPath; }

    public String getRelativePath() { return relativePath; }
    public void setRelativePath(String relativePath) { this.relativePath = relativePath; }

    public String getDefaultBranch() { return defaultBranch; }
    public void setDefaultBranch(String defaultBranch) { this.defaultBranch = defaultBranch; }

    public String getCurrentBranch() { return currentBranch; }
    public void setCurrentBranch(String currentBranch) { this.currentBranch = currentBranch; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAssignedAgents() { return assignedAgents; }
    public void setAssignedAgents(String assignedAgents) { this.assignedAgents = assignedAgents; }

    public String getReviewRules() { return reviewRules; }
    public void setReviewRules(String reviewRules) { this.reviewRules = reviewRules; }

    public Boolean getAutoReviewEnabled() { return autoReviewEnabled; }
    public void setAutoReviewEnabled(Boolean autoReviewEnabled) { this.autoReviewEnabled = autoReviewEnabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 获取仓库类型显示名称
     */
    public String getRepositoryTypeDisplayName() {
        try {
            return RepositoryType.valueOf(repositoryType).getDisplayName();
        } catch (IllegalArgumentException e) {
            return repositoryType;
        }
    }

    /**
     * 获取完整本地路径
     * @param projectWorkDir 项目工作目录
     * @return 完整路径
     */
    public String getFullLocalPath(String projectWorkDir) {
        if (relativePath != null && !relativePath.isEmpty()) {
            return projectWorkDir + "/" + relativePath;
        }
        return localPath;
    }
}
