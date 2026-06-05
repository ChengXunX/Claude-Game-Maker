package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 权限申请实体
 * 用户可以申请额外的权限，由超级管理员审批
 *
 * @author chengxun
 * @since 2.0.0
 */
@Entity
@Table(name = "permission_requests", indexes = {
    @Index(name = "idx_pr_user", columnList = "userId"),
    @Index(name = "idx_pr_status", columnList = "status"),
    @Index(name = "idx_pr_created", columnList = "createdAt")
})
public class PermissionRequest {

    public enum RequestStatus {
        PENDING,    // 待审批
        APPROVED,   // 已批准
        REJECTED,   // 已拒绝
        CANCELLED   // 已取消
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 申请人用户 ID */
    @Column(nullable = false)
    private Long userId;

    /** 申请人用户名（冗余，方便查询） */
    @Column(nullable = false, length = 100)
    private String username;

    /** 申请的权限标识 */
    @Column(nullable = false, length = 100)
    private String permission;

    /** 申请原因 */
    @Column(columnDefinition = "TEXT")
    private String reason;

    /** 申请状态 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status;

    /** 审批者用户 ID */
    private Long approverId;

    /** 审批者用户名 */
    @Column(length = 100)
    private String approverName;

    /** 审批意见 */
    @Column(columnDefinition = "TEXT")
    private String approvalComment;

    /** 审批时间 */
    private LocalDateTime approvedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = RequestStatus.PENDING;
    }

    // ===== Getters & Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    public Long getApproverId() { return approverId; }
    public void setApproverId(Long approverId) { this.approverId = approverId; }

    public String getApproverName() { return approverName; }
    public void setApproverName(String approverName) { this.approverName = approverName; }

    public String getApprovalComment() { return approvalComment; }
    public void setApprovalComment(String approvalComment) { this.approvalComment = approvalComment; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isPending() { return status == RequestStatus.PENDING; }

    @Override
    public String toString() {
        return String.format("PermissionRequest[user=%s, perm=%s, status=%s]", username, permission, status);
    }
}
