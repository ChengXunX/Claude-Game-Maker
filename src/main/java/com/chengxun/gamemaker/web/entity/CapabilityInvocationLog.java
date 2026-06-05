package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 能力调用日志实体
 * 记录每次 Agent 能力调用的完整信息，用于审计和问题排查
 *
 * @author chengxun
 * @since 2.0.0
 */
@Entity
@Table(name = "capability_invocation_logs", indexes = {
    @Index(name = "idx_cil_agent", columnList = "agentId"),
    @Index(name = "idx_cil_project", columnList = "projectId"),
    @Index(name = "idx_cil_status", columnList = "status"),
    @Index(name = "idx_cil_created", columnList = "createdAt"),
    @Index(name = "idx_cil_cap_name", columnList = "capabilityName")
})
public class CapabilityInvocationLog {

    /** 调用状态枚举 */
    public enum InvocationStatus {
        /** 待审批 */
        PENDING_APPROVAL,
        /** 已批准（等待执行） */
        APPROVED,
        /** 已执行 */
        EXECUTED,
        /** 执行失败 */
        FAILED,
        /** 审批被拒绝 */
        REJECTED,
        /** 审批超时 */
        TIMEOUT,
        /** 已取消 */
        CANCELLED
    }

    /** 日志 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Agent 运行时 ID（格式：projectId:agentRole） */
    @Column(nullable = false, length = 200)
    private String agentId;

    /** 项目 ID */
    @Column(length = 100)
    private String projectId;

    /** 能力名称 */
    @Column(nullable = false, length = 100)
    private String capabilityName;

    /** 调用参数（JSON 格式） */
    @Column(columnDefinition = "TEXT")
    private String params;

    /** 执行结果 */
    @Column(columnDefinition = "TEXT")
    private String result;

    /** 调用状态 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InvocationStatus status;

    /** 关联的审批请求 ID */
    private Long approvalRequestId;

    /** 错误信息（执行失败时） */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /** 执行耗时（毫秒） */
    private Long durationMs;

    /** 调用原因（AI 输出的 reason 字段） */
    @Column(columnDefinition = "TEXT")
    private String reason;

    /** 创建时间 */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 审批/执行完成时间 */
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ===== 构造函数 =====

    public CapabilityInvocationLog() {}

    public CapabilityInvocationLog(String agentId, String capabilityName, InvocationStatus status) {
        this.agentId = agentId;
        this.capabilityName = capabilityName;
        this.status = status;
    }

    // ===== Getters & Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getCapabilityName() { return capabilityName; }
    public void setCapabilityName(String capabilityName) { this.capabilityName = capabilityName; }

    public String getParams() { return params; }
    public void setParams(String params) { this.params = params; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public InvocationStatus getStatus() { return status; }
    public void setStatus(InvocationStatus status) { this.status = status; }

    public Long getApprovalRequestId() { return approvalRequestId; }
    public void setApprovalRequestId(Long approvalRequestId) { this.approvalRequestId = approvalRequestId; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    // ===== 辅助方法 =====

    /** 标记为已完成 */
    public void markCompleted(InvocationStatus finalStatus) {
        this.status = finalStatus;
        this.completedAt = LocalDateTime.now();
    }

    /** 标记执行成功 */
    public void markSuccess(String result) {
        this.result = result;
        markCompleted(InvocationStatus.EXECUTED);
    }

    /** 标记执行失败 */
    public void markFailed(String errorMessage) {
        this.errorMessage = errorMessage;
        markCompleted(InvocationStatus.FAILED);
    }

    /** 标记待审批 */
    public void markPendingApproval(Long approvalRequestId) {
        this.approvalRequestId = approvalRequestId;
        this.status = InvocationStatus.PENDING_APPROVAL;
    }

    @Override
    public String toString() {
        return String.format("InvocationLog[%s:%s] agent=%s, status=%s",
            capabilityName, id, agentId, status);
    }
}
