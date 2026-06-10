package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 告警规则实体
 * 定义系统监控的告警规则和阈值
 *
 * 主要功能：
 * - 定义告警条件和阈值
 * - 配置告警级别和通知方式
 * - 支持启用/禁用规则
 *
 * @author chengxun
 * @since 1.0.0
 */
@Entity
@Table(name = "alert_rules", indexes = {
    @Index(name = "idx_alert_rule_type", columnList = "rule_type"),
    @Index(name = "idx_alert_rule_enabled", columnList = "enabled"),
    @Index(name = "idx_alert_rule_priority", columnList = "priority")
})
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 规则名称 */
    @NotBlank(message = "规则名称不能为空")
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    /** 规则描述 */
    @Column(name = "description", length = 500)
    private String description;

    /** 规则类型 */
    @NotBlank(message = "规则类型不能为空")
    @Column(name = "rule_type", length = 50, nullable = false)
    private String ruleType;

    /** 监控指标 */
    @Column(name = "metric_name", length = 100, nullable = false)
    private String metric;

    /** 比较运算符 */
    @Column(name = "operator", length = 20, nullable = false)
    private String operator;

    /** 阈值 */
    @Column(name = "threshold", nullable = false)
    private Double threshold;

    /** 告警级别 */
    @Column(name = "priority", length = 20, nullable = false)
    private String priority = "MEDIUM";

    /** 持续时间（秒） */
    @Column(name = "duration_seconds")
    private Integer durationSeconds = 0;

    /** 通知方式 */
    @Column(name = "notify_method", length = 20)
    private String notifyMethod = "SYSTEM";

    /** 通知目标（邮箱、Webhook等） */
    @Transient
    private String notifyTarget;

    /** 是否启用 */
    @Column(name = "enabled")
    private boolean enabled = true;

    /** 创建者 */
    @Transient
    private String createdBy;

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * 告警规则类型枚举
     */
    public enum RuleType {
        /** Agent响应时间 */
        AGENT_RESPONSE_TIME,
        /** Agent错误率 */
        AGENT_ERROR_RATE,
        /** Agent负载 */
        AGENT_LOAD,
        /** API调用频率 */
        API_CALL_RATE,
        /** Token消耗 */
        TOKEN_USAGE,
        /** 系统资源 */
        SYSTEM_RESOURCE,
        /** 任务失败率 */
        TASK_FAILURE_RATE,
        /** 自定义 */
        CUSTOM
    }

    /**
     * 告警级别枚举
     */
    public enum Priority {
        /** 低优先级 */
        LOW,
        /** 中优先级 */
        MEDIUM,
        /** 高优先级 */
        HIGH,
        /** 紧急 */
        CRITICAL
    }

    /**
     * 比较运算符枚举
     */
    public enum Operator {
        /** 大于 */
        GT,
        /** 大于等于 */
        GTE,
        /** 小于 */
        LT,
        /** 小于等于 */
        LTE,
        /** 等于 */
        EQ,
        /** 不等于 */
        NEQ
    }

    /**
     * 通知方式枚举
     */
    public enum NotifyMethod {
        /** 系统通知 */
        SYSTEM,
        /** 邮件 */
        EMAIL,
        /** Webhook */
        WEBHOOK,
        /** 飞书 */
        FEISHU,
        /** 多种方式 */
        MULTI
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }

    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public Double getThreshold() { return threshold; }
    public void setThreshold(Double threshold) { this.threshold = threshold; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public String getNotifyMethod() { return notifyMethod; }
    public void setNotifyMethod(String notifyMethod) { this.notifyMethod = notifyMethod; }

    public String getNotifyTarget() { return notifyTarget; }
    public void setNotifyTarget(String notifyTarget) { this.notifyTarget = notifyTarget; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
