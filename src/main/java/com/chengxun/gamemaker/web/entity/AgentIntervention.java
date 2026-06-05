package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * Agent干预记录实体
 * 记录人工（管理员、用户）对Agent决策和方向的干预
 *
 * 干预类型：
 * - 指令干预：给Agent发送新指令
 * - 决策覆盖：覆盖Agent的某个决策
 * - 方向调整：调整Agent的工作方向
 * - 暂停/恢复：暂停或恢复Agent工作
 * - 优先级调整：调整任务优先级
 *
 * @author chengxun
 * @since 1.0.0
 */
@Entity
@Table(name = "agent_interventions", indexes = {
    @Index(name = "idx_intervention_agent", columnList = "agentId"),
    @Index(name = "idx_intervention_user", columnList = "userId"),
    @Index(name = "idx_intervention_type", columnList = "interventionType"),
    @Index(name = "idx_intervention_created", columnList = "createdAt")
})
public class AgentIntervention {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 乐观锁版本号 */
    @Version
    private Long version;

    /** 干预编号 */
    @Column(name = "intervention_no", length = 50, unique = true)
    private String interventionNo;

    // ===== Agent信息 =====

    /** Agent ID */
    @NotBlank(message = "agentId 不能为空")
    @Column(name = "agent_id", length = 50, nullable = false)
    private String agentId;

    /** Agent名称 */
    @Column(name = "agent_name", length = 100)
    private String agentName;

    /** Agent角色 */
    @Column(name = "agent_role", length = 50)
    private String agentRole;

    /** 项目ID */
    @Column(name = "project_id", length = 100)
    private String projectId;

    // ===== 干预人信息 =====

    /** 干预人用户ID */
    @Column(name = "user_id")
    private Long userId;

    /** 干预人用户名 */
    @Column(name = "username", length = 50)
    private String username;

    /** 干预人角色（admin/user/producer） */
    @Column(name = "user_role", length = 20)
    private String userRole;

    // ===== 干预内容 =====

    /** 干预类型 */
    @Column(name = "intervention_type", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private InterventionType interventionType;

    /** 干预原因 */
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    /** 干预指令/内容 */
    @Column(name = "instruction", columnDefinition = "TEXT")
    private String instruction;

    /** 原始决策（如果是覆盖决策） */
    @Column(name = "original_decision", columnDefinition = "TEXT")
    private String originalDecision;

    /** 新决策 */
    @Column(name = "new_decision", columnDefinition = "TEXT")
    private String newDecision;

    /** 相关任务ID */
    @Column(name = "task_id", length = 100)
    private String taskId;

    /** 相关消息ID */
    @Column(name = "message_id", length = 100)
    private String messageId;

    // ===== 执行状态 =====

    /** 干预状态 */
    @Column(name = "status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    /** Agent确认时间 */
    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    /** Agent确认信息 */
    @Column(name = "acknowledgement", columnDefinition = "TEXT")
    private String acknowledgement;

    /** 执行时间 */
    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    /** 执行结果 */
    @Column(name = "execution_result", columnDefinition = "TEXT")
    private String executionResult;

    // ===== 时间戳 =====

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * 干预类型枚举
     */
    public enum InterventionType {
        /** 指令干预：给Agent发送新指令 */
        INSTRUCTION,
        /** 决策覆盖：覆盖Agent的某个决策 */
        DECISION_OVERRIDE,
        /** 方向调整：调整Agent的工作方向 */
        DIRECTION_CHANGE,
        /** 暂停工作 */
        PAUSE,
        /** 恢复工作 */
        RESUME,
        /** 优先级调整 */
        PRIORITY_CHANGE,
        /** 任务取消 */
        TASK_CANCEL,
        /** 任务重新分配 */
        TASK_REASSIGN,
        /** 紧急指令 */
        URGENT_INSTRUCTION,
        /** 其他 */
        OTHER
    }

    /**
     * 干预状态枚举
     */
    public enum Status {
        /** 待处理 */
        PENDING,
        /** 已确认 */
        ACKNOWLEDGED,
        /** 执行中 */
        EXECUTING,
        /** 已完成 */
        COMPLETED,
        /** 已拒绝 */
        REJECTED,
        /** 已取消 */
        CANCELLED
    }

    // 业务方法

    /**
     * Agent确认干预
     * 只有PENDING状态才能转为ACKNOWLEDGED
     */
    public void acknowledge(String acknowledgement) {
        if (this.status != Status.PENDING) {
            throw new IllegalStateException("只有待处理状态的干预才能确认，当前状态: " + getStatusDescription());
        }
        this.status = Status.ACKNOWLEDGED;
        this.acknowledgement = acknowledgement;
        this.acknowledgedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 开始执行
     * 只有PENDING或ACKNOWLEDGED状态才能转为EXECUTING
     */
    public void startExecution() {
        if (this.status != Status.PENDING && this.status != Status.ACKNOWLEDGED) {
            throw new IllegalStateException("只有待处理或已确认状态的干预才能开始执行，当前状态: " + getStatusDescription());
        }
        this.status = Status.EXECUTING;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 完成执行
     * 只有EXECUTING状态才能转为COMPLETED
     */
    public void complete(String result) {
        if (this.status != Status.EXECUTING) {
            throw new IllegalStateException("只有执行中状态的干预才能标记为完成，当前状态: " + getStatusDescription());
        }
        this.status = Status.COMPLETED;
        this.executionResult = result;
        this.executedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 拒绝干预
     * 只有PENDING、ACKNOWLEDGED状态才能拒绝
     */
    public void reject(String reason) {
        if (this.status != Status.PENDING && this.status != Status.ACKNOWLEDGED) {
            throw new IllegalStateException("只有待处理或已确认状态的干预才能拒绝，当前状态: " + getStatusDescription());
        }
        this.status = Status.REJECTED;
        this.executionResult = "拒绝原因: " + reason;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 取消干预
     * 只有PENDING状态才能取消
     */
    public void cancel() {
        if (this.status != Status.PENDING) {
            throw new IllegalStateException("只有待处理状态的干预才能取消，当前状态: " + getStatusDescription());
        }
        this.status = Status.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 获取干预类型描述
     */
    public String getInterventionTypeDescription() {
        if (interventionType == null) return "未知";
        return switch (interventionType) {
            case INSTRUCTION -> "指令干预";
            case DECISION_OVERRIDE -> "决策覆盖";
            case DIRECTION_CHANGE -> "方向调整";
            case PAUSE -> "暂停工作";
            case RESUME -> "恢复工作";
            case PRIORITY_CHANGE -> "优先级调整";
            case TASK_CANCEL -> "任务取消";
            case TASK_REASSIGN -> "任务重新分配";
            case URGENT_INSTRUCTION -> "紧急指令";
            case OTHER -> "其他";
        };
    }

    /**
     * 获取状态描述
     */
    public String getStatusDescription() {
        if (status == null) return "未知";
        return switch (status) {
            case PENDING -> "待处理";
            case ACKNOWLEDGED -> "已确认";
            case EXECUTING -> "执行中";
            case COMPLETED -> "已完成";
            case REJECTED -> "已拒绝";
            case CANCELLED -> "已取消";
        };
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getInterventionNo() { return interventionNo; }
    public void setInterventionNo(String interventionNo) { this.interventionNo = interventionNo; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }

    public String getAgentRole() { return agentRole; }
    public void setAgentRole(String agentRole) { this.agentRole = agentRole; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public InterventionType getInterventionType() { return interventionType; }
    public void setInterventionType(InterventionType interventionType) { this.interventionType = interventionType; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getInstruction() { return instruction; }
    public void setInstruction(String instruction) { this.instruction = instruction; }

    public String getOriginalDecision() { return originalDecision; }
    public void setOriginalDecision(String originalDecision) { this.originalDecision = originalDecision; }

    public String getNewDecision() { return newDecision; }
    public void setNewDecision(String newDecision) { this.newDecision = newDecision; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }

    public String getAcknowledgement() { return acknowledgement; }
    public void setAcknowledgement(String acknowledgement) { this.acknowledgement = acknowledgement; }

    public LocalDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }

    public String getExecutionResult() { return executionResult; }
    public void setExecutionResult(String executionResult) { this.executionResult = executionResult; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
