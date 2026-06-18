package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 质量预测记录实体
 * 记录对项目版本的质量预测结果
 *
 * @author chengxun
 * @since 3.0.0
 */
@Entity
@Table(name = "quality_predictions", indexes = {
    @Index(name = "idx_qp_project", columnList = "project_id"),
    @Index(name = "idx_qp_version", columnList = "version"),
    @Index(name = "idx_qp_time", columnList = "created_at")
})
public class QualityPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private String projectId;

    @Column(name = "project_name", length = 200)
    private String projectName;

    @Column(name = "version", length = 50)
    private String version;

    /** 预测通过概率（0-100） */
    @Column(name = "pass_probability")
    private Integer passProbability;

    /** 风险等级：LOW, MEDIUM, HIGH, CRITICAL */
    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    /** 风险因素（JSON数组） */
    @Column(name = "risk_factors", columnDefinition = "TEXT")
    private String riskFactors;

    /** 改进建议（JSON数组） */
    @Column(name = "improvement_suggestions", columnDefinition = "TEXT")
    private String improvementSuggestions;

    /** 因子详情（JSON对象） */
    @Column(name = "factors_detail", columnDefinition = "TEXT")
    private String factorsDetail;

    /** 历史通过率 */
    @Column(name = "historical_pass_rate")
    private Double historicalPassRate;

    /** 当前里程碑完成率 */
    @Column(name = "milestone_completion_rate")
    private Double milestoneCompletionRate;

    /** 验证失败次数 */
    @Column(name = "verification_fail_count")
    private Integer verificationFailCount;

    /** Agent平均错误率 */
    @Column(name = "avg_agent_error_rate")
    private Double avgAgentErrorRate;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public Integer getPassProbability() { return passProbability; }
    public void setPassProbability(Integer passProbability) { this.passProbability = passProbability; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getRiskFactors() { return riskFactors; }
    public void setRiskFactors(String riskFactors) { this.riskFactors = riskFactors; }
    public String getImprovementSuggestions() { return improvementSuggestions; }
    public void setImprovementSuggestions(String improvementSuggestions) { this.improvementSuggestions = improvementSuggestions; }
    public String getFactorsDetail() { return factorsDetail; }
    public void setFactorsDetail(String factorsDetail) { this.factorsDetail = factorsDetail; }
    public Double getHistoricalPassRate() { return historicalPassRate; }
    public void setHistoricalPassRate(Double historicalPassRate) { this.historicalPassRate = historicalPassRate; }
    public Double getMilestoneCompletionRate() { return milestoneCompletionRate; }
    public void setMilestoneCompletionRate(Double milestoneCompletionRate) { this.milestoneCompletionRate = milestoneCompletionRate; }
    public Integer getVerificationFailCount() { return verificationFailCount; }
    public void setVerificationFailCount(Integer verificationFailCount) { this.verificationFailCount = verificationFailCount; }
    public Double getAvgAgentErrorRate() { return avgAgentErrorRate; }
    public void setAvgAgentErrorRate(Double avgAgentErrorRate) { this.avgAgentErrorRate = avgAgentErrorRate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
