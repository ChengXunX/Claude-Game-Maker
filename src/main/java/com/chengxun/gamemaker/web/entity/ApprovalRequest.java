package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 审批请求实体
 * 用于管理制作人 Agent 的敏感操作审批
 *
 * @author chengxun
 * @since 1.0.0
 */
@Entity
@Table(name = "approval_requests")
public class ApprovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属项目 ID */
    @Column(name = "project_id", nullable = false, length = 100)
    private String projectId;

    /** 请求者 ID（通常是制作人 Agent） */
    @Column(name = "requester_id", nullable = false, length = 100)
    private String requesterId;

    /** 请求者名称 */
    @Column(name = "requester_name", length = 100)
    private String requesterName;

    /** 请求类型：CREATE_AGENT, ASSIGN_API, DELETE_AGENT, CHANGE_CONFIG 等 */
    @Column(name = "request_type", nullable = false, length = 50)
    private String requestType;

    /** 请求数据（JSON 格式） */
    @Column(name = "request_data", columnDefinition = "TEXT")
    private String requestData;

    /** 请求描述（人类可读） */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 审批状态 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    /** 审批者 ID（管理员/用户） */
    @Column(name = "approver_id", length = 100)
    private String approverId;

    /** 审批者名称 */
    @Column(name = "approver_name", length = 100)
    private String approverName;

    /** 审批意见 */
    @Column(name = "approval_comment", columnDefinition = "TEXT")
    private String approvalComment;

    /** 创建时间 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** 审批时间 */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /** 过期时间 */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /** 优先级：1-10，数值越小优先级越高 */
    @Column(name = "priority")
    private Integer priority = 5;

    public enum ApprovalStatus {
        PENDING,    // 待审批
        APPROVED,   // 已批准
        REJECTED,   // 已拒绝
        EXPIRED,    // 已过期
        CANCELLED   // 已取消
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (expiresAt == null) {
            // 默认 24 小时过期
            expiresAt = createdAt.plusHours(24);
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getRequesterId() { return requesterId; }
    public void setRequesterId(String requesterId) { this.requesterId = requesterId; }

    public String getRequesterName() { return requesterName; }
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }

    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }

    public String getRequestData() { return requestData; }
    public void setRequestData(String requestData) { this.requestData = requestData; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ApprovalStatus getStatus() { return status; }
    public void setStatus(ApprovalStatus status) { this.status = status; }

    public String getApproverId() { return approverId; }
    public void setApproverId(String approverId) { this.approverId = approverId; }

    public String getApproverName() { return approverName; }
    public void setApproverName(String approverName) { this.approverName = approverName; }

    public String getApprovalComment() { return approvalComment; }
    public void setApprovalComment(String approvalComment) { this.approvalComment = approvalComment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public boolean isPending() { return status == ApprovalStatus.PENDING; }
    public boolean isApproved() { return status == ApprovalStatus.APPROVED; }
    public boolean isRejected() { return status == ApprovalStatus.REJECTED; }
    public boolean isExpired() { return expiresAt != null && LocalDateTime.now().isAfter(expiresAt); }
}
