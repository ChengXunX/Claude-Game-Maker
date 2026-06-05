package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 解雇申请实体
 * 记录制作人发起的解雇申请和审批流程
 *
 * 流程：
 * 1. 绩效过低 → 制作人发出警告
 * 2. 多次警告后 → 制作人发起解雇申请
 * 3. 管理员审批 → 同意/驳回
 * 4. 同意后 → 执行解雇
 * 5. 驳回后 → 制作人可重新申请或取消
 *
 * @author chengxun
 * @since 1.0.0
 */
@Entity
@Table(name = "dismissal_requests", indexes = {
    @Index(name = "idx_dismissal_agent", columnList = "agentId"),
    @Index(name = "idx_dismissal_producer", columnList = "producerId"),
    @Index(name = "idx_dismissal_status", columnList = "status"),
    @Index(name = "idx_dismissal_created", columnList = "createdAt")
})
public class DismissalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 申请编号 */
    @Column(name = "request_no", length = 50, unique = true)
    private String requestNo;

    // ===== 被解雇Agent信息 =====

    /** Agent ID */
    @Column(name = "agent_id", length = 50, nullable = false)
    private String agentId;

    /** Agent名称 */
    @Column(name = "agent_name", length = 100)
    private String agentName;

    /** Agent角色 */
    @Column(name = "agent_role", length = 50)
    private String agentRole;

    /** 是否为系统预设角色 */
    @Column(name = "is_system_role")
    private Boolean isSystemRole = false;

    // ===== 申请人信息 =====

    /** 制作人Agent ID */
    @Column(name = "producer_id", length = 50, nullable = false)
    private String producerId;

    /** 制作人名称 */
    @Column(name = "producer_name", length = 100)
    private String producerName;

    /** 项目ID */
    @Column(name = "project_id", length = 100)
    private String projectId;

    /** 项目名称 */
    @Column(name = "project_name", length = 200)
    private String projectName;

    // ===== 解雇原因 =====

    /** 解雇原因类型 */
    @Column(name = "reason_type", length = 50)
    private String reasonType;

    /** 解雇原因详情 */
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    /** 警告次数 */
    @Column(name = "warning_count")
    private Integer warningCount = 0;

    /** 最近一次警告时间 */
    @Column(name = "last_warning_at")
    private LocalDateTime lastWarningAt;

    /** 最近警告原因 */
    @Column(name = "last_warning_reason", columnDefinition = "TEXT")
    private String lastWarningReason;

    /** 绩效评分历史（JSON格式） */
    @Column(name = "performance_history", columnDefinition = "TEXT")
    private String performanceHistory;

    /** 连续低分期数 */
    @Column(name = "consecutive_low_score_periods")
    private Integer consecutiveLowScorePeriods = 0;

    // ===== 审批信息 =====

    /** 申请状态 */
    @Column(name = "status", length = 20, nullable = false)
    private String status = "PENDING";

    /** 审批人（管理员用户ID） */
    @Column(name = "approver_id")
    private Long approverId;

    /** 审批人名称 */
    @Column(name = "approver_name", length = 50)
    private String approverName;

    /** 审批时间 */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /** 审批意见 */
    @Column(name = "approval_comment", columnDefinition = "TEXT")
    private String approvalComment;

    /** 驳回原因 */
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // ===== 执行信息 =====

    /** 执行时间 */
    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    /** 执行人 */
    @Column(name = "executed_by", length = 50)
    private String executedBy;

    // ===== 时间戳 =====

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * 申请状态枚举
     */
    public enum Status {
        PENDING,    // 待审批
        APPROVED,   // 已批准
        REJECTED,   // 已驳回
        EXECUTED,   // 已执行
        CANCELLED   // 已取消
    }

    /**
     * 解雇原因类型枚举
     */
    public enum ReasonType {
        LOW_PERFORMANCE,      // 绩效过低
        MULTIPLE_WARNINGS,    // 多次警告
        INCOMPETENCE,         // 能力不足
        ATTITUDE_ISSUE,       // 态度问题
        PROJECT_NEEDS,        // 项目需求变更
        OTHER                 // 其他
    }

    // 业务方法

    /**
     * 批准申请
     */
    public void approve(Long approverId, String approverName, String comment) {
        this.status = Status.APPROVED.name();
        this.approverId = approverId;
        this.approverName = approverName;
        this.approvalComment = comment;
        this.approvedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 驳回申请
     */
    public void reject(Long approverId, String approverName, String reason) {
        this.status = Status.REJECTED.name();
        this.approverId = approverId;
        this.approverName = approverName;
        this.rejectionReason = reason;
        this.approvedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 执行解雇
     */
    public void execute(String executedBy) {
        this.status = Status.EXECUTED.name();
        this.executedAt = LocalDateTime.now();
        this.executedBy = executedBy;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 取消申请
     */
    public void cancel() {
        this.status = Status.CANCELLED.name();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 获取状态描述
     */
    public String getStatusDescription() {
        return switch (status) {
            case "PENDING" -> "待审批";
            case "APPROVED" -> "已批准";
            case "REJECTED" -> "已驳回";
            case "EXECUTED" -> "已执行";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }

    /**
     * 获取原因类型描述
     */
    public String getReasonTypeDescription() {
        return switch (reasonType) {
            case "LOW_PERFORMANCE" -> "绩效过低";
            case "MULTIPLE_WARNINGS" -> "多次警告";
            case "INCOMPETENCE" -> "能力不足";
            case "ATTITUDE_ISSUE" -> "态度问题";
            case "PROJECT_NEEDS" -> "项目需求变更";
            case "OTHER" -> "其他";
            default -> reasonType;
        };
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRequestNo() { return requestNo; }
    public void setRequestNo(String requestNo) { this.requestNo = requestNo; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }

    public String getAgentRole() { return agentRole; }
    public void setAgentRole(String agentRole) { this.agentRole = agentRole; }

    public Boolean getIsSystemRole() { return isSystemRole; }
    public void setIsSystemRole(Boolean isSystemRole) { this.isSystemRole = isSystemRole; }

    public String getProducerId() { return producerId; }
    public void setProducerId(String producerId) { this.producerId = producerId; }

    public String getProducerName() { return producerName; }
    public void setProducerName(String producerName) { this.producerName = producerName; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getReasonType() { return reasonType; }
    public void setReasonType(String reasonType) { this.reasonType = reasonType; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Integer getWarningCount() { return warningCount; }
    public void setWarningCount(Integer warningCount) { this.warningCount = warningCount; }

    public LocalDateTime getLastWarningAt() { return lastWarningAt; }
    public void setLastWarningAt(LocalDateTime lastWarningAt) { this.lastWarningAt = lastWarningAt; }

    public String getLastWarningReason() { return lastWarningReason; }
    public void setLastWarningReason(String lastWarningReason) { this.lastWarningReason = lastWarningReason; }

    public String getPerformanceHistory() { return performanceHistory; }
    public void setPerformanceHistory(String performanceHistory) { this.performanceHistory = performanceHistory; }

    public Integer getConsecutiveLowScorePeriods() { return consecutiveLowScorePeriods; }
    public void setConsecutiveLowScorePeriods(Integer consecutiveLowScorePeriods) { this.consecutiveLowScorePeriods = consecutiveLowScorePeriods; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getApproverId() { return approverId; }
    public void setApproverId(Long approverId) { this.approverId = approverId; }

    public String getApproverName() { return approverName; }
    public void setApproverName(String approverName) { this.approverName = approverName; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public String getApprovalComment() { return approvalComment; }
    public void setApprovalComment(String approvalComment) { this.approvalComment = approvalComment; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public LocalDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }

    public String getExecutedBy() { return executedBy; }
    public void setExecutedBy(String executedBy) { this.executedBy = executedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
