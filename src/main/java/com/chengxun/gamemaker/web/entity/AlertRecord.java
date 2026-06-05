package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 告警记录实体
 * 记录系统触发的告警事件
 *
 * 主要功能：
 * - 记录告警触发时间、规则、状态
 * - 支持告警确认和解决
 * - 提供告警历史查询
 *
 * @author chengxun
 * @since 1.0.0
 */
@Entity
@Table(name = "alert_records", indexes = {
    @Index(name = "idx_alert_record_rule_id", columnList = "ruleId"),
    @Index(name = "idx_alert_record_status", columnList = "status"),
    @Index(name = "idx_alert_record_priority", columnList = "priority"),
    @Index(name = "idx_alert_record_created", columnList = "createdAt")
})
public class AlertRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联的告警规则ID */
    @Column(name = "rule_id")
    private Long ruleId;

    /** 告警规则名称 */
    @Column(name = "rule_name", length = 100)
    private String ruleName;

    /** 告警级别 */
    @Column(name = "priority", length = 20, nullable = false)
    private String priority;

    /** 告警状态 */
    @Column(name = "status", length = 20, nullable = false)
    private String status = "PENDING";

    /** 告警标题 */
    @Column(name = "title", length = 200, nullable = false)
    private String title;

    /** 告警详情 */
    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    /** 触发值 */
    @Column(name = "trigger_value")
    private Double triggerValue;

    /** 阈值 */
    @Column(name = "threshold_value")
    private Double thresholdValue;

    /** 监控指标 */
    @Column(name = "metric", length = 100)
    private String metric;

    /** 相关Agent ID */
    @Column(name = "agent_id", length = 50)
    private String agentId;

    /** 相关Agent名称 */
    @Column(name = "agent_name", length = 100)
    private String agentName;

    /** 相关项目ID */
    @Column(name = "project_id", length = 100)
    private String projectId;

    /** 确认人 */
    @Column(name = "acknowledged_by", length = 50)
    private String acknowledgedBy;

    /** 确认时间 */
    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    /** 解决人 */
    @Column(name = "resolved_by", length = 50)
    private String resolvedBy;

    /** 解决时间 */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /** 解决方案 */
    @Column(name = "resolution", columnDefinition = "TEXT")
    private String resolution;

    /** 通知状态 */
    @Column(name = "notified", length = 20)
    private String notified = "PENDING";

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * 告警状态枚举
     */
    public enum Status {
        /** 待处理 */
        PENDING,
        /** 已确认 */
        ACKNOWLEDGED,
        /** 处理中 */
        IN_PROGRESS,
        /** 已解决 */
        RESOLVED,
        /** 已忽略 */
        IGNORED
    }

    /**
     * 通知状态枚举
     */
    public enum NotifyStatus {
        /** 待发送 */
        PENDING,
        /** 已发送 */
        SENT,
        /** 发送失败 */
        FAILED,
        /** 无需通知 */
        SKIPPED
    }

    // 业务方法

    /**
     * 确认告警
     * @param acknowledgedBy 确认人
     */
    public void acknowledge(String acknowledgedBy) {
        this.status = Status.ACKNOWLEDGED.name();
        this.acknowledgedBy = acknowledgedBy;
        this.acknowledgedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 开始处理告警
     * @param resolver 处理人
     */
    public void startProgress(String resolver) {
        this.status = Status.IN_PROGRESS.name();
        this.resolvedBy = resolver;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 解决告警
     * @param resolvedBy 解决人
     * @param resolution 解决方案
     */
    public void resolve(String resolvedBy, String resolution) {
        this.status = Status.RESOLVED.name();
        this.resolvedBy = resolvedBy;
        this.resolvedAt = LocalDateTime.now();
        this.resolution = resolution;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 忽略告警
     * @param ignoredBy 忽略人
     */
    public void ignore(String ignoredBy) {
        this.status = Status.IGNORED.name();
        this.resolvedBy = ignoredBy;
        this.resolvedAt = LocalDateTime.now();
        this.resolution = "Ignored";
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 标记通知已发送
     */
    public void markNotified() {
        this.notified = NotifyStatus.SENT.name();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 标记通知失败
     */
    public void markNotifyFailed() {
        this.notified = NotifyStatus.FAILED.name();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 获取状态描述
     */
    public String getStatusDescription() {
        return switch (status) {
            case "PENDING" -> "待处理";
            case "ACKNOWLEDGED" -> "已确认";
            case "IN_PROGRESS" -> "处理中";
            case "RESOLVED" -> "已解决";
            case "IGNORED" -> "已忽略";
            default -> status;
        };
    }

    /**
     * 获取优先级描述
     */
    public String getPriorityDescription() {
        return switch (priority) {
            case "LOW" -> "低";
            case "MEDIUM" -> "中";
            case "HIGH" -> "高";
            case "CRITICAL" -> "紧急";
            default -> priority;
        };
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public Double getTriggerValue() { return triggerValue; }
    public void setTriggerValue(Double triggerValue) { this.triggerValue = triggerValue; }

    public Double getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(Double thresholdValue) { this.thresholdValue = thresholdValue; }

    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getAcknowledgedBy() { return acknowledgedBy; }
    public void setAcknowledgedBy(String acknowledgedBy) { this.acknowledgedBy = acknowledgedBy; }

    public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }

    public String getNotified() { return notified; }
    public void setNotified(String notified) { this.notified = notified; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
