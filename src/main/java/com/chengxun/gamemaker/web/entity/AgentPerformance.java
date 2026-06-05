package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * Agent性能评估实体
 * 记录Agent的各项性能指标，用于能力评估和任务分配优化
 *
 * 主要功能：
 * - 统计任务完成率
 * - 计算执行效率
 * - 评估工作质量
 * - 记录历史表现趋势
 *
 * @author chengxun
 * @since 1.0.0
 */
@Entity
@Table(name = "agent_performance", indexes = {
    @Index(name = "idx_perf_agent_id", columnList = "agentId"),
    @Index(name = "idx_perf_project", columnList = "projectId"),
    @Index(name = "idx_perf_role", columnList = "agentRole"),
    @Index(name = "idx_perf_updated", columnList = "updatedAt")
})
public class AgentPerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属项目 ID */
    @Column(name = "project_id", length = 100)
    private String projectId;

    /** Agent ID（同一项目内唯一） */
    @NotBlank(message = "agentId 不能为空")
    @Column(name = "agent_id", length = 50, nullable = false)
    private String agentId;

    /** Agent名称 */
    @Column(name = "agent_name", length = 100)
    private String agentName;

    /** Agent角色 */
    @Column(name = "agent_role", length = 50)
    private String agentRole;

    // ===== 任务统计 =====

    /** 总任务数 */
    @Column(name = "total_tasks")
    private Integer totalTasks = 0;

    /** 已完成任务数 */
    @Column(name = "completed_tasks")
    private Integer completedTasks = 0;

    /** 失败任务数 */
    @Column(name = "failed_tasks")
    private Integer failedTasks = 0;

    /** 进行中任务数 */
    @Column(name = "in_progress_tasks")
    private Integer inProgressTasks = 0;

    // ===== 效率指标 =====

    /** 平均任务完成时间（毫秒） */
    @Column(name = "avg_completion_time_ms")
    private Long avgCompletionTimeMs = 0L;

    /** 最快完成时间（毫秒） */
    @Column(name = "min_completion_time_ms")
    private Long minCompletionTimeMs = 0L;

    /** 最慢完成时间（毫秒） */
    @Column(name = "max_completion_time_ms")
    private Long maxCompletionTimeMs = 0L;

    // ===== 质量指标 =====

    /** 平均质量评分（0-100） */
    @Column(name = "avg_quality_score")
    private Double avgQualityScore = 0.0;

    /** 最高质量评分 */
    @Column(name = "max_quality_score")
    private Double maxQualityScore = 0.0;

    /** 最低质量评分 */
    @Column(name = "min_quality_score")
    private Double minQualityScore = 0.0;

    /** 代码审查通过率（0-100） */
    @Column(name = "review_pass_rate")
    private Double reviewPassRate = 0.0;

    // ===== 负载指标 =====

    /** 当前负载（0-100） */
    @Column(name = "current_load")
    private Integer currentLoad = 0;

    /** 历史平均负载 */
    @Column(name = "avg_load")
    private Double avgLoad = 0.0;

    // ===== 综合评分 =====

    /** 综合能力评分（0-100） */
    @Column(name = "overall_score")
    private Double overallScore = 0.0;

    /** 可靠性评分（0-100） */
    @Column(name = "reliability_score")
    private Double reliabilityScore = 0.0;

    /** 效率评分（0-100） */
    @Column(name = "efficiency_score")
    private Double efficiencyScore = 0.0;

    // ===== 时间戳 =====

    /** 首次任务时间 */
    @Column(name = "first_task_at")
    private LocalDateTime firstTaskAt;

    /** 最后任务时间 */
    @Column(name = "last_task_at")
    private LocalDateTime lastTaskAt;

    /** 最后评估时间 */
    @Column(name = "last_evaluated_at")
    private LocalDateTime lastEvaluatedAt;

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ===== 构造函数 =====

    public AgentPerformance() {
    }

    public AgentPerformance(String agentId, String agentName, String agentRole) {
        this.agentId = agentId;
        this.agentName = agentName;
        this.agentRole = agentRole;
        this.firstTaskAt = LocalDateTime.now();
        this.lastTaskAt = LocalDateTime.now();
    }

    public AgentPerformance(String projectId, String agentId, String agentName, String agentRole) {
        this.projectId = projectId;
        this.agentId = agentId;
        this.agentName = agentName;
        this.agentRole = agentRole;
        this.firstTaskAt = LocalDateTime.now();
        this.lastTaskAt = LocalDateTime.now();
    }

    // ===== 业务方法 =====

    /**
     * 计算完成率
     * @return 完成率百分比（0-100）
     */
    public double getCompletionRate() {
        if (totalTasks == 0) return 0.0;
        return (double) completedTasks / totalTasks * 100;
    }

    /**
     * 计算失败率
     * @return 失败率百分比（0-100）
     */
    public double getFailureRate() {
        if (totalTasks == 0) return 0.0;
        return (double) failedTasks / totalTasks * 100;
    }

    /**
     * 更新任务统计
     * @param completed 是否完成
     * @param durationMs 耗时（毫秒）
     * @param qualityScore 质量评分（可选）
     */
    public void updateTaskStats(boolean completed, long durationMs, Double qualityScore) {
        this.totalTasks++;
        this.lastTaskAt = LocalDateTime.now();

        if (completed) {
            this.completedTasks++;
            updateCompletionTimeStats(durationMs);
        } else {
            this.failedTasks++;
        }

        if (qualityScore != null) {
            updateQualityScoreStats(qualityScore);
        }

        recalculateScores();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新完成时间统计
     */
    private void updateCompletionTimeStats(long durationMs) {
        if (minCompletionTimeMs == 0 || durationMs < minCompletionTimeMs) {
            minCompletionTimeMs = durationMs;
        }
        if (durationMs > maxCompletionTimeMs) {
            maxCompletionTimeMs = durationMs;
        }

        // 计算移动平均
        if (avgCompletionTimeMs == 0) {
            avgCompletionTimeMs = durationMs;
        } else {
            avgCompletionTimeMs = (avgCompletionTimeMs + durationMs) / 2;
        }
    }

    /**
     * 更新质量评分统计
     */
    private void updateQualityScoreStats(double qualityScore) {
        if (maxQualityScore == 0 || qualityScore > maxQualityScore) {
            maxQualityScore = qualityScore;
        }
        if (minQualityScore == 0 || qualityScore < minQualityScore) {
            minQualityScore = qualityScore;
        }

        // 计算移动平均
        if (avgQualityScore == 0) {
            avgQualityScore = qualityScore;
        } else {
            avgQualityScore = (avgQualityScore + qualityScore) / 2;
        }
    }

    /**
     * 重新计算综合评分
     */
    public void recalculateScores() {
        // 可靠性评分：基于完成率和失败率
        this.reliabilityScore = calculateReliabilityScore();

        // 效率评分：基于完成时间
        this.efficiencyScore = calculateEfficiencyScore();

        // 综合评分：加权平均
        this.overallScore = (reliabilityScore * 0.4) + (efficiencyScore * 0.3) + (avgQualityScore * 0.3);
    }

    /**
     * 计算可靠性评分
     */
    private double calculateReliabilityScore() {
        if (totalTasks == 0) return 50.0; // 默认分数

        double completionRate = getCompletionRate();
        double failureRate = getFailureRate();

        // 完成率越高、失败率越低，可靠性越高
        return Math.max(0, Math.min(100, completionRate - (failureRate * 2)));
    }

    /**
     * 计算效率评分
     */
    private double calculateEfficiencyScore() {
        if (avgCompletionTimeMs == 0) return 50.0;

        // 假设基准时间：1小时（3600000毫秒）
        long baselineMs = 3600000;

        if (avgCompletionTimeMs <= baselineMs) {
            // 快于基准，高分
            return 100.0 - ((double) avgCompletionTimeMs / baselineMs * 50);
        } else {
            // 慢于基准，低分
            return Math.max(0, 50.0 - (((double) avgCompletionTimeMs - baselineMs) / baselineMs * 50));
        }
    }

    /**
     * 更新当前负载
     * @param load 负载值（0-100）
     */
    public void updateLoad(int load) {
        this.currentLoad = Math.max(0, Math.min(100, load));
        this.avgLoad = (avgLoad + currentLoad) / 2;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 标记任务开始
     */
    public void markTaskStarted() {
        this.inProgressTasks++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 标记任务结束
     */
    public void markTaskCompleted() {
        if (inProgressTasks > 0) {
            this.inProgressTasks--;
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 获取性能等级
     * @return 等级：S, A, B, C, D
     */
    public String getPerformanceGrade() {
        if (overallScore >= 90) return "S";
        if (overallScore >= 80) return "A";
        if (overallScore >= 70) return "B";
        if (overallScore >= 60) return "C";
        return "D";
    }

    /**
     * 获取状态描述
     */
    public String getStatusDescription() {
        if (totalTasks == 0) return "新加入";
        if (currentLoad >= 80) return "高负载";
        if (currentLoad >= 50) return "中等负载";
        if (overallScore >= 80) return "优秀";
        if (overallScore >= 60) return "良好";
        return "需要改进";
    }

    // ===== Getters and Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }

    public String getAgentRole() { return agentRole; }
    public void setAgentRole(String agentRole) { this.agentRole = agentRole; }

    public Integer getTotalTasks() { return totalTasks; }
    public void setTotalTasks(Integer totalTasks) { this.totalTasks = totalTasks; }

    public Integer getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(Integer completedTasks) { this.completedTasks = completedTasks; }

    public Integer getFailedTasks() { return failedTasks; }
    public void setFailedTasks(Integer failedTasks) { this.failedTasks = failedTasks; }

    public Integer getInProgressTasks() { return inProgressTasks; }
    public void setInProgressTasks(Integer inProgressTasks) { this.inProgressTasks = inProgressTasks; }

    public Long getAvgCompletionTimeMs() { return avgCompletionTimeMs; }
    public void setAvgCompletionTimeMs(Long avgCompletionTimeMs) { this.avgCompletionTimeMs = avgCompletionTimeMs; }

    public Long getMinCompletionTimeMs() { return minCompletionTimeMs; }
    public void setMinCompletionTimeMs(Long minCompletionTimeMs) { this.minCompletionTimeMs = minCompletionTimeMs; }

    public Long getMaxCompletionTimeMs() { return maxCompletionTimeMs; }
    public void setMaxCompletionTimeMs(Long maxCompletionTimeMs) { this.maxCompletionTimeMs = maxCompletionTimeMs; }

    public Double getAvgQualityScore() { return avgQualityScore; }
    public void setAvgQualityScore(Double avgQualityScore) { this.avgQualityScore = avgQualityScore; }

    public Double getMaxQualityScore() { return maxQualityScore; }
    public void setMaxQualityScore(Double maxQualityScore) { this.maxQualityScore = maxQualityScore; }

    public Double getMinQualityScore() { return minQualityScore; }
    public void setMinQualityScore(Double minQualityScore) { this.minQualityScore = minQualityScore; }

    public Double getReviewPassRate() { return reviewPassRate; }
    public void setReviewPassRate(Double reviewPassRate) { this.reviewPassRate = reviewPassRate; }

    public Integer getCurrentLoad() { return currentLoad; }
    public void setCurrentLoad(Integer currentLoad) { this.currentLoad = currentLoad; }

    public Double getAvgLoad() { return avgLoad; }
    public void setAvgLoad(Double avgLoad) { this.avgLoad = avgLoad; }

    public Double getOverallScore() { return overallScore; }
    public void setOverallScore(Double overallScore) { this.overallScore = overallScore; }

    public Double getReliabilityScore() { return reliabilityScore; }
    public void setReliabilityScore(Double reliabilityScore) { this.reliabilityScore = reliabilityScore; }

    public Double getEfficiencyScore() { return efficiencyScore; }
    public void setEfficiencyScore(Double efficiencyScore) { this.efficiencyScore = efficiencyScore; }

    public LocalDateTime getFirstTaskAt() { return firstTaskAt; }
    public void setFirstTaskAt(LocalDateTime firstTaskAt) { this.firstTaskAt = firstTaskAt; }

    public LocalDateTime getLastTaskAt() { return lastTaskAt; }
    public void setLastTaskAt(LocalDateTime lastTaskAt) { this.lastTaskAt = lastTaskAt; }

    public LocalDateTime getLastEvaluatedAt() { return lastEvaluatedAt; }
    public void setLastEvaluatedAt(LocalDateTime lastEvaluatedAt) { this.lastEvaluatedAt = lastEvaluatedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
