package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 版本迭代记录实体
 * 保存每次版本迭代的评估结果、规划结果等，支持历史查询和趋势分析
 *
 * @author chengxun
 * @since 2.0.0
 */
@Entity
@Table(name = "version_iteration_records", indexes = {
    @Index(name = "idx_vir_project", columnList = "projectId"),
    @Index(name = "idx_vir_version", columnList = "projectId, version"),
    @Index(name = "idx_vir_created", columnList = "createdAt")
})
public class VersionIterationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 项目 ID */
    @Column(nullable = false, length = 100)
    private String projectId;

    /** 版本号 */
    @Column(nullable = false, length = 50)
    private String version;

    /** 评估分数（1-10） */
    @Column(nullable = false)
    private int evaluationScore;

    /** 是否通过验收 */
    @Column(nullable = false)
    private boolean passed;

    /** 评估详情 */
    @Column(columnDefinition = "TEXT")
    private String evaluationDetails;

    /** 优点列表（JSON 数组） */
    @Column(columnDefinition = "TEXT")
    private String strengths;

    /** 待改进列表（JSON 数组） */
    @Column(columnDefinition = "TEXT")
    private String improvements;

    /** 评估建议 */
    @Column(columnDefinition = "TEXT")
    private String recommendation;

    /** 是否需要下一版本 */
    @Column(nullable = false)
    private boolean needNextVersion;

    /** 下一版本号 */
    @Column(length = 50)
    private String nextVersion;

    /** 下一版本目标 */
    @Column(columnDefinition = "TEXT")
    private String nextGoal;

    /** 下一版本里程碑（JSON 数组） */
    @Column(columnDefinition = "TEXT")
    private String nextMilestones;

    /** 规划理由 */
    @Column(columnDefinition = "TEXT")
    private String planReason;

    /** 里程碑完成数 */
    @Column(nullable = false)
    private int completedMilestones;

    /** 里程碑总数 */
    @Column(nullable = false)
    private int totalMilestones;

    /** 迭代结果：COMPLETED（目标完成）、ITERATED（继续迭代）、IMPROVED（需要改进） */
    @Column(nullable = false, length = 20)
    private String result;

    /** 创建时间 */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ===== 构造函数 =====

    public VersionIterationRecord() {}

    public VersionIterationRecord(String projectId, String version) {
        this.projectId = projectId;
        this.version = version;
    }

    // ===== Getters & Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public int getEvaluationScore() { return evaluationScore; }
    public void setEvaluationScore(int evaluationScore) { this.evaluationScore = evaluationScore; }

    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }

    public String getEvaluationDetails() { return evaluationDetails; }
    public void setEvaluationDetails(String evaluationDetails) { this.evaluationDetails = evaluationDetails; }

    public String getStrengths() { return strengths; }
    public void setStrengths(String strengths) { this.strengths = strengths; }

    public String getImprovements() { return improvements; }
    public void setImprovements(String improvements) { this.improvements = improvements; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

    public boolean isNeedNextVersion() { return needNextVersion; }
    public void setNeedNextVersion(boolean needNextVersion) { this.needNextVersion = needNextVersion; }

    public String getNextVersion() { return nextVersion; }
    public void setNextVersion(String nextVersion) { this.nextVersion = nextVersion; }

    public String getNextGoal() { return nextGoal; }
    public void setNextGoal(String nextGoal) { this.nextGoal = nextGoal; }

    public String getNextMilestones() { return nextMilestones; }
    public void setNextMilestones(String nextMilestones) { this.nextMilestones = nextMilestones; }

    public String getPlanReason() { return planReason; }
    public void setPlanReason(String planReason) { this.planReason = planReason; }

    public int getCompletedMilestones() { return completedMilestones; }
    public void setCompletedMilestones(int completedMilestones) { this.completedMilestones = completedMilestones; }

    public int getTotalMilestones() { return totalMilestones; }
    public void setTotalMilestones(int totalMilestones) { this.totalMilestones = totalMilestones; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return String.format("VersionIterationRecord[%s:%s] score=%d, passed=%s, result=%s",
            projectId, version, evaluationScore, passed, result);
    }
}
