package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 绩效评审实体
 * 记录制作人对团队成员的绩效打分
 *
 * 评分维度：
 * - 任务完成质量 (qualityScore)
 * - 工作效率 (efficiencyScore)
 * - 协作能力 (collaborationScore)
 * - 创新能力 (innovationScore)
 * - 综合评分 (overallScore)
 *
 * @author chengxun
 * @since 1.0.0
 */
@Entity
@Table(name = "performance_reviews", indexes = {
    @Index(name = "idx_review_agent", columnList = "agentId"),
    @Index(name = "idx_review_producer", columnList = "producerId"),
    @Index(name = "idx_review_project", columnList = "projectId"),
    @Index(name = "idx_review_period", columnList = "reviewPeriod"),
    @Index(name = "idx_review_created", columnList = "createdAt")
})
public class PerformanceReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 评审编号 */
    @Column(name = "review_no", length = 50, unique = true)
    private String reviewNo;

    /** 被评审Agent ID */
    @Column(name = "agent_id", length = 50, nullable = false)
    private String agentId;

    /** 被评审Agent名称 */
    @Column(name = "agent_name", length = 100)
    private String agentName;

    /** 被评审Agent角色 */
    @Column(name = "agent_role", length = 50)
    private String agentRole;

    /** 评审人（制作人Agent ID） */
    @Column(name = "producer_id", length = 50, nullable = false)
    private String producerId;

    /** 评审人名称 */
    @Column(name = "producer_name", length = 100)
    private String producerName;

    /** 项目ID */
    @Column(name = "project_id", length = 100)
    private String projectId;

    /** 项目名称 */
    @Column(name = "project_name", length = 200)
    private String projectName;

    /** 评审周期（如：2026-Q1, 2026-05） */
    @NotBlank(message = "reviewPeriod 不能为空")
    @Column(name = "review_period", length = 20, nullable = false)
    private String reviewPeriod;

    // ===== 评分维度 =====

    /** 任务完成质量 (0-100) */
    @Column(name = "quality_score")
    private Integer qualityScore;

    /** 工作效率 (0-100) */
    @Column(name = "efficiency_score")
    private Integer efficiencyScore;

    /** 协作能力 (0-100) */
    @Column(name = "collaboration_score")
    private Integer collaborationScore;

    /** 创新能力 (0-100) */
    @Column(name = "innovation_score")
    private Integer innovationScore;

    /** 综合评分 (0-100) */
    @Column(name = "overall_score")
    private Integer overallScore;

    // ===== 评价内容 =====

    /** 优点 */
    @Column(name = "strengths", columnDefinition = "TEXT")
    private String strengths;

    /** 待改进 */
    @Column(name = "improvements", columnDefinition = "TEXT")
    private String improvements;

    /** 具体评价 */
    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    /** 工作亮点 */
    @Column(name = "highlights", columnDefinition = "TEXT")
    private String highlights;

    // ===== 状态 =====

    /** 评审状态 */
    @Column(name = "status", length = 20)
    private String status = "COMPLETED";

    /** 是否为警告评审 */
    @Column(name = "is_warning")
    private Boolean isWarning = false;

    /** 警告原因 */
    @Column(name = "warning_reason", columnDefinition = "TEXT")
    private String warningReason;

    // ===== 时间戳 =====

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * 评审状态枚举
     */
    public enum Status {
        DRAFT,      // 草稿
        COMPLETED,  // 已完成
        DISPUTED    // 有异议
    }

    /**
     * 计算综合评分
     */
    public void calculateOverallScore() {
        int total = 0;
        int count = 0;

        if (qualityScore != null) { total += qualityScore; count++; }
        if (efficiencyScore != null) { total += efficiencyScore; count++; }
        if (collaborationScore != null) { total += collaborationScore; count++; }
        if (innovationScore != null) { total += innovationScore; count++; }

        if (count > 0) {
            this.overallScore = total / count;
        } else {
            this.overallScore = 0;
        }
    }

    /**
     * 获取评分等级
     */
    public String getGrade() {
        if (overallScore == null) return "N/A";
        if (overallScore >= 90) return "A";
        if (overallScore >= 80) return "B";
        if (overallScore >= 70) return "C";
        if (overallScore >= 60) return "D";
        return "F";
    }

    /**
     * 是否低分（低于60分）
     */
    public boolean isLowScore() {
        return overallScore != null && overallScore < 60;
    }

    /**
     * 是否需要警告（低于50分）
     */
    public boolean needsWarning() {
        return overallScore != null && overallScore < 50;
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReviewNo() { return reviewNo; }
    public void setReviewNo(String reviewNo) { this.reviewNo = reviewNo; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }

    public String getAgentRole() { return agentRole; }
    public void setAgentRole(String agentRole) { this.agentRole = agentRole; }

    public String getProducerId() { return producerId; }
    public void setProducerId(String producerId) { this.producerId = producerId; }

    public String getProducerName() { return producerName; }
    public void setProducerName(String producerName) { this.producerName = producerName; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getReviewPeriod() { return reviewPeriod; }
    public void setReviewPeriod(String reviewPeriod) { this.reviewPeriod = reviewPeriod; }

    public Integer getQualityScore() { return qualityScore; }
    public void setQualityScore(Integer qualityScore) { this.qualityScore = qualityScore; }

    public Integer getEfficiencyScore() { return efficiencyScore; }
    public void setEfficiencyScore(Integer efficiencyScore) { this.efficiencyScore = efficiencyScore; }

    public Integer getCollaborationScore() { return collaborationScore; }
    public void setCollaborationScore(Integer collaborationScore) { this.collaborationScore = collaborationScore; }

    public Integer getInnovationScore() { return innovationScore; }
    public void setInnovationScore(Integer innovationScore) { this.innovationScore = innovationScore; }

    public Integer getOverallScore() { return overallScore; }
    public void setOverallScore(Integer overallScore) { this.overallScore = overallScore; }

    public String getStrengths() { return strengths; }
    public void setStrengths(String strengths) { this.strengths = strengths; }

    public String getImprovements() { return improvements; }
    public void setImprovements(String improvements) { this.improvements = improvements; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public String getHighlights() { return highlights; }
    public void setHighlights(String highlights) { this.highlights = highlights; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getIsWarning() { return isWarning; }
    public void setIsWarning(Boolean isWarning) { this.isWarning = isWarning; }

    public String getWarningReason() { return warningReason; }
    public void setWarningReason(String warningReason) { this.warningReason = warningReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
