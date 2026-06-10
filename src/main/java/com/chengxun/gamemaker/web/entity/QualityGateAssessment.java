package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 质量门禁评估结果实体
 * 持久化存储质量评估历史记录
 *
 * @author chengxun
 * @since 2.0.0
 */
@Entity
@Table(name = "quality_gate_assessments", indexes = {
    @Index(name = "idx_qga_project", columnList = "projectId"),
    @Index(name = "idx_qga_time", columnList = "assessedAt")
})
public class QualityGateAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 项目 ID */
    @Column(nullable = false, length = 100)
    private String projectId;

    /** 项目名称 */
    @Column(length = 200)
    private String projectName;

    /** 综合评分 (0-100) */
    @Column(nullable = false)
    private int overallScore;

    /** 是否通过 */
    @Column(nullable = false)
    private boolean passed;

    /** 质量等级：EXCELLENT, GOOD, ACCEPTABLE, POOR, CRITICAL */
    @Column(length = 20)
    private String qualityLevel;

    /** 阻塞项（JSON 格式） */
    @Column(columnDefinition = "TEXT")
    private String blockersJson;

    /** 建议（JSON 格式） */
    @Column(columnDefinition = "TEXT")
    private String recommendationsJson;

    /** 各门禁检查结果（JSON 格式） */
    @Column(columnDefinition = "TEXT")
    private String gateResultsJson;

    /** 评估时间 */
    @Column(nullable = false)
    private LocalDateTime assessedAt;

    /** 请求者 */
    @Column(length = 100)
    private String requestedBy;

    @PrePersist
    protected void onCreate() {
        if (assessedAt == null) {
            assessedAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public int getOverallScore() { return overallScore; }
    public void setOverallScore(int overallScore) { this.overallScore = overallScore; }

    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }

    public String getQualityLevel() { return qualityLevel; }
    public void setQualityLevel(String qualityLevel) { this.qualityLevel = qualityLevel; }

    public String getBlockersJson() { return blockersJson; }
    public void setBlockersJson(String blockersJson) { this.blockersJson = blockersJson; }

    public String getRecommendationsJson() { return recommendationsJson; }
    public void setRecommendationsJson(String recommendationsJson) { this.recommendationsJson = recommendationsJson; }

    public String getGateResultsJson() { return gateResultsJson; }
    public void setGateResultsJson(String gateResultsJson) { this.gateResultsJson = gateResultsJson; }

    public LocalDateTime getAssessedAt() { return assessedAt; }
    public void setAssessedAt(LocalDateTime assessedAt) { this.assessedAt = assessedAt; }

    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }
}
