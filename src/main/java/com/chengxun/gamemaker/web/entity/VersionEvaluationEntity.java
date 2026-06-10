package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 版本评估持久化实体
 * 将版本评估结果保存到数据库，支持历史查询和趋势分析
 *
 * @author chengxun
 * @since 2.0.0
 */
@Entity
@Table(name = "version_evaluations", indexes = {
    @Index(name = "idx_ve_project", columnList = "projectId"),
    @Index(name = "idx_ve_milestone", columnList = "milestoneId")
})
public class VersionEvaluationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 项目 ID */
    @Column(nullable = false)
    private String projectId;

    /** 里程碑 ID */
    @Column(nullable = false)
    private String milestoneId;

    /** 里程碑标题 */
    private String milestoneTitle;

    /** 评估时间 */
    @Column(nullable = false)
    private LocalDateTime evaluatedAt;

    /** 效率评分 (0-100) */
    private int efficiencyScore;

    /** 质量评分 (0-100) */
    private int qualityScore;

    /** 综合评分 (0-100) */
    private int overallScore;

    /** 缺失角色（JSON 数组） */
    @Column(columnDefinition = "TEXT")
    private String missingRoles;

    /** 冗余角色（JSON 数组） */
    @Column(columnDefinition = "TEXT")
    private String redundantRoles;

    /** 建议列表（JSON 数组） */
    @Column(columnDefinition = "TEXT")
    private String recommendations;

    /** Agent 评估详情（JSON） */
    @Column(columnDefinition = "TEXT")
    private String agentEvaluationsJson;

    @PrePersist
    protected void onCreate() {
        if (evaluatedAt == null) {
            evaluatedAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getMilestoneId() { return milestoneId; }
    public void setMilestoneId(String milestoneId) { this.milestoneId = milestoneId; }

    public String getMilestoneTitle() { return milestoneTitle; }
    public void setMilestoneTitle(String milestoneTitle) { this.milestoneTitle = milestoneTitle; }

    public LocalDateTime getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(LocalDateTime evaluatedAt) { this.evaluatedAt = evaluatedAt; }

    public int getEfficiencyScore() { return efficiencyScore; }
    public void setEfficiencyScore(int efficiencyScore) { this.efficiencyScore = efficiencyScore; }

    public int getQualityScore() { return qualityScore; }
    public void setQualityScore(int qualityScore) { this.qualityScore = qualityScore; }

    public int getOverallScore() { return overallScore; }
    public void setOverallScore(int overallScore) { this.overallScore = overallScore; }

    public String getMissingRoles() { return missingRoles; }
    public void setMissingRoles(String missingRoles) { this.missingRoles = missingRoles; }

    public String getRedundantRoles() { return redundantRoles; }
    public void setRedundantRoles(String redundantRoles) { this.redundantRoles = redundantRoles; }

    public String getRecommendations() { return recommendations; }
    public void setRecommendations(String recommendations) { this.recommendations = recommendations; }

    public String getAgentEvaluationsJson() { return agentEvaluationsJson; }
    public void setAgentEvaluationsJson(String agentEvaluationsJson) { this.agentEvaluationsJson = agentEvaluationsJson; }
}
