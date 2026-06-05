package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 代码审查实体
 * 记录Agent提交代码的审查流程
 *
 * 主要功能：
 * - 记录代码提交和审查状态
 * - 支持自动和人工审查
 * - 审查结果反馈
 *
 * @author chengxun
 * @since 1.0.0
 */
@Entity
@Table(name = "code_reviews", indexes = {
    @Index(name = "idx_review_agent", columnList = "agentId"),
    @Index(name = "idx_review_project", columnList = "projectId"),
    @Index(name = "idx_review_status", columnList = "status"),
    @Index(name = "idx_review_created", columnList = "createdAt")
})
public class CodeReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 审查标题 */
    @Column(name = "title", length = 200, nullable = false)
    private String title;

    /** 审查描述 */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 提交Agent ID */
    @Column(name = "agent_id", length = 50, nullable = false)
    private String agentId;

    /** 提交Agent名称 */
    @Column(name = "agent_name", length = 100)
    private String agentName;

    /** 项目ID */
    @Column(name = "project_id", length = 100)
    private String projectId;

    /** Git仓库ID */
    @Column(name = "git_repository_id")
    private Long gitRepositoryId;

    /** Git仓库名称 */
    @Column(name = "git_repository_name", length = 100)
    private String gitRepositoryName;

    /** 仓库类型 */
    @Column(name = "repository_type", length = 50)
    private String repositoryType;

    /** 分支名称 */
    @Column(name = "branch", length = 100)
    private String branch;

    /** 提交哈希 */
    @Column(name = "commit_hash", length = 50)
    private String commitHash;

    /** 变更文件列表（JSON格式） */
    @Column(name = "changed_files", columnDefinition = "TEXT")
    private String changedFiles;

    /** 代码差异 */
    @Column(name = "diff_content", columnDefinition = "TEXT")
    private String diffContent;

    /** 审查状态 */
    @Column(name = "status", length = 20, nullable = false)
    private String status = "PENDING";

    /** 审查类型 */
    @Column(name = "review_type", length = 20)
    private String reviewType = "AUTO";

    /** 审查人 */
    @Column(name = "reviewer", length = 50)
    private String reviewer;

    /** 审查评分（0-100） */
    @Column(name = "score")
    private Integer score;

    /** 审查意见 */
    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    /** 自动审查结果（JSON格式） */
    @Column(name = "auto_review_result", columnDefinition = "TEXT")
    private String autoReviewResult;

    /** 问题数量 */
    @Column(name = "issue_count")
    private Integer issueCount = 0;

    /** 警告数量 */
    @Column(name = "warning_count")
    private Integer warningCount = 0;

    /** 提交时间 */
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    /** 审查完成时间 */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * 审查状态枚举
     */
    public enum Status {
        /** 待审查 */
        PENDING,
        /** 审查中 */
        IN_REVIEW,
        /** 审查通过 */
        APPROVED,
        /** 需要修改 */
        CHANGES_REQUESTED,
        /** 已拒绝 */
        REJECTED
    }

    /**
     * 审查类型枚举
     */
    public enum ReviewType {
        /** 自动审查 */
        AUTO,
        /** 人工审查 */
        MANUAL,
        /** 混合审查 */
        MIXED
    }

    // 业务方法

    /**
     * 开始审查
     * @param reviewer 审查人
     */
    public void startReview(String reviewer) {
        this.status = Status.IN_REVIEW.name();
        this.reviewer = reviewer;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 审查通过
     * @param score 评分
     * @param comment 审查意见
     */
    public void approve(Integer score, String comment) {
        this.status = Status.APPROVED.name();
        this.score = score;
        this.reviewComment = comment;
        this.reviewedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 请求修改
     * @param comment 审查意见
     */
    public void requestChanges(String comment) {
        this.status = Status.CHANGES_REQUESTED.name();
        this.reviewComment = comment;
        this.reviewedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 拒绝审查
     * @param comment 审查意见
     */
    public void reject(String comment) {
        this.status = Status.REJECTED.name();
        this.reviewComment = comment;
        this.reviewedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 获取状态描述
     */
    public String getStatusDescription() {
        return switch (status) {
            case "PENDING" -> "待审查";
            case "IN_REVIEW" -> "审查中";
            case "APPROVED" -> "已通过";
            case "CHANGES_REQUESTED" -> "需要修改";
            case "REJECTED" -> "已拒绝";
            default -> status;
        };
    }

    /**
     * 获取审查类型描述
     */
    public String getReviewTypeDescription() {
        return switch (reviewType) {
            case "AUTO" -> "自动审查";
            case "MANUAL" -> "人工审查";
            case "MIXED" -> "混合审查";
            default -> reviewType;
        };
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public Long getGitRepositoryId() { return gitRepositoryId; }
    public void setGitRepositoryId(Long gitRepositoryId) { this.gitRepositoryId = gitRepositoryId; }

    public String getGitRepositoryName() { return gitRepositoryName; }
    public void setGitRepositoryName(String gitRepositoryName) { this.gitRepositoryName = gitRepositoryName; }

    public String getRepositoryType() { return repositoryType; }
    public void setRepositoryType(String repositoryType) { this.repositoryType = repositoryType; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getCommitHash() { return commitHash; }
    public void setCommitHash(String commitHash) { this.commitHash = commitHash; }

    public String getChangedFiles() { return changedFiles; }
    public void setChangedFiles(String changedFiles) { this.changedFiles = changedFiles; }

    public String getDiffContent() { return diffContent; }
    public void setDiffContent(String diffContent) { this.diffContent = diffContent; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReviewType() { return reviewType; }
    public void setReviewType(String reviewType) { this.reviewType = reviewType; }

    public String getReviewer() { return reviewer; }
    public void setReviewer(String reviewer) { this.reviewer = reviewer; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }

    public String getAutoReviewResult() { return autoReviewResult; }
    public void setAutoReviewResult(String autoReviewResult) { this.autoReviewResult = autoReviewResult; }

    public Integer getIssueCount() { return issueCount; }
    public void setIssueCount(Integer issueCount) { this.issueCount = issueCount; }

    public Integer getWarningCount() { return warningCount; }
    public void setWarningCount(Integer warningCount) { this.warningCount = warningCount; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
