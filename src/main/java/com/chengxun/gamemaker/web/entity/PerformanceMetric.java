package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 性能指标实体
 * 记录系统和Agent的性能监控数据
 *
 * 主要功能：
 * - 记录Agent响应时间
 * - 统计API调用次数
 * - 监控系统资源使用
 *
 * @author chengxun
 * @since 1.0.0
 */
@Entity
@Table(name = "performance_metrics", indexes = {
    @Index(name = "idx_metric_name", columnList = "metricName"),
    @Index(name = "idx_metric_type", columnList = "metricType"),
    @Index(name = "idx_metric_agent", columnList = "agentId"),
    @Index(name = "idx_metric_created", columnList = "createdAt")
})
public class PerformanceMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 指标名称 */
    @Column(name = "metric_name", length = 100, nullable = false)
    private String metricName;

    /** 指标类型 */
    @NotBlank(message = "metricType 不能为空")
    @Column(name = "metric_type", length = 50, nullable = false)
    private String metricType;

    /** 指标值 */
    @Column(name = "metric_value", nullable = false)
    private Double value;

    /** 指标单位 */
    @Column(name = "unit", length = 20)
    private String unit;

    /** 相关Agent ID */
    @Column(name = "agent_id", length = 100)
    private String agentId;

    /** 相关Agent名称 */
    @Column(name = "agent_name", length = 100)
    private String agentName;

    /** 相关项目ID */
    @Column(name = "project_id", length = 100)
    private String projectId;

    /** 相关API路径 */
    @Column(name = "api_path", length = 500)
    private String apiPath;

    /** 标签（JSON格式） */
    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 指标类型枚举
     */
    public enum MetricType {
        /** Agent响应时间 */
        AGENT_RESPONSE_TIME,
        /** Agent调用次数 */
        AGENT_CALL_COUNT,
        /** API响应时间 */
        API_RESPONSE_TIME,
        /** API调用次数 */
        API_CALL_COUNT,
        /** Token消耗 */
        TOKEN_USAGE,
        /** 内存使用 */
        MEMORY_USAGE,
        /** CPU使用 */
        CPU_USAGE,
        /** 任务执行时间 */
        TASK_DURATION,
        /** 错误率 */
        ERROR_RATE,
        /** 自定义 */
        CUSTOM
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMetricName() { return metricName; }
    public void setMetricName(String metricName) { this.metricName = metricName; }

    public String getMetricType() { return metricType; }
    public void setMetricType(String metricType) { this.metricType = metricType; }

    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getApiPath() { return apiPath; }
    public void setApiPath(String apiPath) { this.apiPath = apiPath; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
