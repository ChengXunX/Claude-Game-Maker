package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * Agent健康指标实体
 * 记录Agent的健康状态和性能指标
 *
 * 主要指标：
 * - 响应时间
 * - 错误率
 * - 内存使用
 * - 任务完成率
 * - 最后活动时间
 *
 * @author chengxun
 * @since 1.0.0
 */
@Entity
@Table(name = "agent_health", indexes = {
    @Index(name = "idx_health_agent", columnList = "agentId"),
    @Index(name = "idx_health_project", columnList = "projectId"),
    @Index(name = "idx_health_status", columnList = "healthStatus"),
    @Index(name = "idx_health_time", columnList = "checkTime")
})
public class AgentHealth {

    /**
     * 健康状态枚举
     */
    public enum HealthStatus {
        /** 健康 */
        HEALTHY,
        /** 警告 */
        WARNING,
        /** 不健康 */
        UNHEALTHY,
        /** 离线 */
        OFFLINE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属项目 ID */
    @Column(name = "project_id", length = 100)
    private String projectId;

    /** Agent ID */
    @NotBlank(message = "agentId 不能为空")
    @Column(name = "agent_id", nullable = false)
    private String agentId;

    /** Agent名称 */
    @Column(name = "agent_name")
    private String agentName;

    /** Agent角色 */
    @Column(name = "agent_role")
    private String agentRole;

    /** 健康状态 */
    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", nullable = false)
    private HealthStatus healthStatus;

    /** 平均响应时间（毫秒） */
    @Column(name = "avg_response_time_ms")
    private Long avgResponseTimeMs;

    /** 最大响应时间（毫秒） */
    @Column(name = "max_response_time_ms")
    private Long maxResponseTimeMs;

    /** 总请求数 */
    @Column(name = "total_requests")
    private Long totalRequests = 0L;

    /** 成功请求数 */
    @Column(name = "successful_requests")
    private Long successfulRequests = 0L;

    /** 失败请求数 */
    @Column(name = "failed_requests")
    private Long failedRequests = 0L;

    /** 错误率（百分比） */
    @Column(name = "error_rate")
    private Double errorRate = 0.0;

    /** 内存使用量（MB） */
    @Column(name = "memory_usage_mb")
    private Long memoryUsageMb;

    /** CPU使用率（百分比） */
    @Column(name = "cpu_usage_percent")
    private Double cpuUsagePercent;

    /** 活跃任务数 */
    @Column(name = "active_tasks")
    private Integer activeTasks = 0;

    /** 已完成任务数 */
    @Column(name = "completed_tasks")
    private Long completedTasks = 0L;

    /** 任务完成率（百分比） */
    @Column(name = "task_completion_rate")
    private Double taskCompletionRate = 0.0;

    /** 最后活动时间 */
    @Column(name = "last_activity_time")
    private LocalDateTime lastActivityTime;

    /** 最后错误时间 */
    @Column(name = "last_error_time")
    private LocalDateTime lastErrorTime;

    /** 最后错误信息 */
    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

    /** 连续错误次数 */
    @Column(name = "consecutive_errors")
    private Integer consecutiveErrors = 0;

    /** 正常运行时间（秒） */
    @Column(name = "uptime_seconds")
    private Long uptimeSeconds = 0L;

    /** 检查时间 */
    @Column(name = "check_time", nullable = false)
    private LocalDateTime checkTime;

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (checkTime == null) {
            checkTime = LocalDateTime.now();
        }
    }

    // 业务方法

    /**
     * 记录请求
     */
    public void recordRequest(long responseTimeMs, boolean success) {
        totalRequests++;
        if (success) {
            successfulRequests++;
            consecutiveErrors = 0;
        } else {
            failedRequests++;
            consecutiveErrors++;
            lastErrorTime = LocalDateTime.now();
        }

        // 更新响应时间
        if (avgResponseTimeMs == null) {
            avgResponseTimeMs = responseTimeMs;
        } else {
            avgResponseTimeMs = (avgResponseTimeMs * (totalRequests - 1) + responseTimeMs) / totalRequests;
        }
        if (maxResponseTimeMs == null || responseTimeMs > maxResponseTimeMs) {
            maxResponseTimeMs = responseTimeMs;
        }

        // 更新错误率
        errorRate = (double) failedRequests / totalRequests * 100;

        // 更新健康状态
        updateHealthStatus();

        lastActivityTime = LocalDateTime.now();
        checkTime = LocalDateTime.now();
    }

    /**
     * 记录任务完成
     */
    public void recordTaskCompletion(boolean success) {
        if (success) {
            completedTasks++;
        }
        if (activeTasks > 0) {
            activeTasks--;
        }

        // 更新任务完成率
        if (completedTasks > 0) {
            taskCompletionRate = (double) completedTasks / (completedTasks + failedRequests) * 100;
        }

        checkTime = LocalDateTime.now();
    }

    /**
     * 更新健康状态
     */
    private void updateHealthStatus() {
        if (consecutiveErrors >= 10 || errorRate > 50) {
            healthStatus = HealthStatus.UNHEALTHY;
        } else if (consecutiveErrors >= 3 || errorRate > 20 || avgResponseTimeMs > 5000) {
            healthStatus = HealthStatus.WARNING;
        } else {
            healthStatus = HealthStatus.HEALTHY;
        }
    }

    /**
     * 检查是否需要重启
     */
    public boolean needsRestart() {
        return healthStatus == HealthStatus.UNHEALTHY && consecutiveErrors >= 10;
    }

    /**
     * 检查是否响应缓慢（AI任务阈值：超过5分钟视为缓慢）
     */
    public boolean isSlowResponse() {
        return avgResponseTimeMs != null && avgResponseTimeMs > 300000;
    }

    /**
     * 获取健康分数（0-100）
     * 综合评估：错误率、响应时间、连续错误、活跃度
     * 响应时间阈值针对AI工作负载优化（AI任务通常需要30秒~5分钟）
     */
    public int getHealthScore() {
        int score = 100;

        // 错误率扣分
        if (errorRate > 50) score -= 50;
        else if (errorRate > 20) score -= 30;
        else if (errorRate > 10) score -= 15;

        // 响应时间扣分（AI任务阈值：10分钟+才扣分）
        if (avgResponseTimeMs != null) {
            if (avgResponseTimeMs > 600000) score -= 40;      // > 10分钟
            else if (avgResponseTimeMs > 300000) score -= 25;  // > 5分钟
            else if (avgResponseTimeMs > 120000) score -= 10;  // > 2分钟
        }

        // 连续错误扣分
        if (consecutiveErrors >= 10) score -= 30;
        else if (consecutiveErrors >= 5) score -= 20;
        else if (consecutiveErrors >= 3) score -= 10;

        // 活跃度扣分：最后活动时间距今过久
        if (lastActivityTime != null) {
            long idleMinutes = java.time.Duration.between(lastActivityTime, java.time.LocalDateTime.now()).toMinutes();
            if (idleMinutes > 120) score -= 25;      // 超过2小时无活动
            else if (idleMinutes > 60) score -= 15;  // 超过1小时
            else if (idleMinutes > 30) score -= 5;   // 超过30分钟
        }

        return Math.max(0, score);
    }

    // Getters and Setters

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

    public HealthStatus getHealthStatus() { return healthStatus; }
    public void setHealthStatus(HealthStatus healthStatus) { this.healthStatus = healthStatus; }

    public Long getAvgResponseTimeMs() { return avgResponseTimeMs; }
    public void setAvgResponseTimeMs(Long avgResponseTimeMs) { this.avgResponseTimeMs = avgResponseTimeMs; }

    public Long getMaxResponseTimeMs() { return maxResponseTimeMs; }
    public void setMaxResponseTimeMs(Long maxResponseTimeMs) { this.maxResponseTimeMs = maxResponseTimeMs; }

    public Long getTotalRequests() { return totalRequests; }
    public void setTotalRequests(Long totalRequests) { this.totalRequests = totalRequests; }

    public Long getSuccessfulRequests() { return successfulRequests; }
    public void setSuccessfulRequests(Long successfulRequests) { this.successfulRequests = successfulRequests; }

    public Long getFailedRequests() { return failedRequests; }
    public void setFailedRequests(Long failedRequests) { this.failedRequests = failedRequests; }

    public Double getErrorRate() { return errorRate; }
    public void setErrorRate(Double errorRate) { this.errorRate = errorRate; }

    public Long getMemoryUsageMb() { return memoryUsageMb; }
    public void setMemoryUsageMb(Long memoryUsageMb) { this.memoryUsageMb = memoryUsageMb; }

    public Double getCpuUsagePercent() { return cpuUsagePercent; }
    public void setCpuUsagePercent(Double cpuUsagePercent) { this.cpuUsagePercent = cpuUsagePercent; }

    public Integer getActiveTasks() { return activeTasks; }
    public void setActiveTasks(Integer activeTasks) { this.activeTasks = activeTasks; }

    public Long getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(Long completedTasks) { this.completedTasks = completedTasks; }

    public Double getTaskCompletionRate() { return taskCompletionRate; }
    public void setTaskCompletionRate(Double taskCompletionRate) { this.taskCompletionRate = taskCompletionRate; }

    public LocalDateTime getLastActivityTime() { return lastActivityTime; }
    public void setLastActivityTime(LocalDateTime lastActivityTime) { this.lastActivityTime = lastActivityTime; }

    public LocalDateTime getLastErrorTime() { return lastErrorTime; }
    public void setLastErrorTime(LocalDateTime lastErrorTime) { this.lastErrorTime = lastErrorTime; }

    public String getLastErrorMessage() { return lastErrorMessage; }
    public void setLastErrorMessage(String lastErrorMessage) { this.lastErrorMessage = lastErrorMessage; }

    public Integer getConsecutiveErrors() { return consecutiveErrors; }
    public void setConsecutiveErrors(Integer consecutiveErrors) { this.consecutiveErrors = consecutiveErrors; }

    public Long getUptimeSeconds() { return uptimeSeconds; }
    public void setUptimeSeconds(Long uptimeSeconds) { this.uptimeSeconds = uptimeSeconds; }

    public LocalDateTime getCheckTime() { return checkTime; }
    public void setCheckTime(LocalDateTime checkTime) { this.checkTime = checkTime; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
