package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 人工干预记录实体
 * 记录用户对Agent的干预操作，用于追踪和审计
 *
 * 主要功能：
 * - 记录干预类型和内容
 * - 支持不同紧急程度
 * - 支持不同有效期设置
 *
 * @deprecated 此类已废弃，请使用 {@link AgentIntervention} 替代。
 *             AgentIntervention 提供了更完整的干预功能，包括决策覆盖、方向调整等。
 *             此类保留仅为向后兼容，将在未来版本中移除。
 */
@Deprecated
@Entity
@Table(name = "interventions")
public class Intervention {

    /** 干预ID，自增主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 目标Agent ID */
    @Column(nullable = false)
    private String agentId;

    /** 干预用户ID */
    @Column(nullable = false)
    private Long userId;

    /** 干预类型 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterventionType type;

    /** 干预内容，支持Markdown格式 */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /** 紧急程度 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UrgencyLevel urgency = UrgencyLevel.NORMAL;

    /** 有效期 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DurationType duration = DurationType.ONCE;

    /** 创建时间 */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 是否已处理 */
    @Column(nullable = false)
    private boolean processed = false;

    /** 处理时间 */
    private LocalDateTime processedAt;

    /**
     * 干预类型枚举
     */
    public enum InterventionType {
        /** 方向指导 - 提供新的工作方向 */
        GUIDANCE,
        /** 行为纠正 - 纠正错误行为 */
        CORRECTION,
        /** 优先级调整 - 调整任务优先级 */
        PRIORITY,
        /** 约束添加 - 添加新的约束条件 */
        CONSTRAINT,
        /** 上下文补充 - 补充重要上下文信息 */
        CONTEXT
    }

    /**
     * 紧急程度枚举
     */
    public enum UrgencyLevel {
        /** 普通 */
        NORMAL,
        /** 高 - 需要立即处理 */
        HIGH,
        /** 紧急 - 必须立即响应 */
        CRITICAL
    }

    /**
     * 有效期类型枚举
     */
    public enum DurationType {
        /** 仅本次 - 仅影响当前任务 */
        ONCE,
        /** 本次会话 - 影响整个会话期间 */
        SESSION,
        /** 永久 - 作为长期指导原则 */
        PERMANENT
    }

    /** JPA生命周期回调，设置创建时间 */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /** JPA生命周期回调，设置更新时间 */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public InterventionType getType() {
        return type;
    }

    public void setType(InterventionType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public UrgencyLevel getUrgency() {
        return urgency;
    }

    public void setUrgency(UrgencyLevel urgency) {
        this.urgency = urgency;
    }

    public DurationType getDuration() {
        return duration;
    }

    public void setDuration(DurationType duration) {
        this.duration = duration;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
