package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 工作流审批持久化实体
 * 记录需要审批的步骤及其审批状态
 *
 * 主要功能：
 * - 记录审批请求和审批结果
 * - 关联审批人信息
 * - 支持审批意见记录
 *
 * @author chengxun
 * @since 2.0.0
 */
@Entity
@Table(name = "workflow_approvals", indexes = {
    @Index(name = "idx_wfa_instance", columnList = "instance_id"),
    @Index(name = "idx_wfa_status", columnList = "status"),
    @Index(name = "idx_wfa_approver", columnList = "approver_id")
})
public class WorkflowApprovalEntity {

    /** 审批ID（主键，自增） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联的实例ID */
    @Column(name = "instance_id", length = 64, nullable = false)
    private String instanceId;

    /** 关联的步骤ID */
    @Column(name = "step_id", length = 64, nullable = false)
    private String stepId;

    /** 审批人用户ID */
    @Column(name = "approver_id")
    private Long approverId;

    /** 审批人用户名 */
    @Column(name = "approver_name", length = 50)
    private String approverName;

    /** 状态: PENDING/APPROVED/REJECTED */
    @Column(name = "status", length = 20, nullable = false)
    private String status = "PENDING";

    /** 审批意见 */
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    /** 请求时间 */
    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    /** 决定时间 */
    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @PrePersist
    protected void onCreate() {
        requestedAt = LocalDateTime.now();
    }

    // ===== Getters and Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }

    public String getStepId() { return stepId; }
    public void setStepId(String stepId) { this.stepId = stepId; }

    public Long getApproverId() { return approverId; }
    public void setApproverId(Long approverId) { this.approverId = approverId; }

    public String getApproverName() { return approverName; }
    public void setApproverName(String approverName) { this.approverName = approverName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }

    public LocalDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }
}
