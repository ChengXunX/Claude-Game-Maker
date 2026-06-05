package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 招聘申请实体
 * 记录制作人发起的招聘申请和审批流程
 *
 * 流程：
 * 1. 制作人发起招聘申请
 * 2. 系统通知管理员
 * 3. 管理员审批（同意/驳回）
 * 4. 同意后制作人执行招聘
 * 5. 驳回后制作人可重新申请或取消
 *
 * @author chengxun
 * @since 1.0.0
 */
@Entity
@Table(name = "recruitment_requests", indexes = {
    @Index(name = "idx_recruit_status", columnList = "status"),
    @Index(name = "idx_recruit_producer", columnList = "producerId"),
    @Index(name = "idx_recruit_created", columnList = "createdAt")
})
public class RecruitmentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 申请编号 */
    @Column(name = "request_no", length = 50, unique = true)
    private String requestNo;

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

    // ===== 招聘信息 =====

    /** 招聘角色 */
    @Column(name = "role", length = 50, nullable = false)
    private String role;

    /** 角色名称 */
    @Column(name = "role_name", length = 100)
    private String roleName;

    /** 员工姓名 */
    @Column(name = "employee_name", length = 100, nullable = false)
    private String employeeName;

    /** 角色描述 */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 能力标签（JSON格式） */
    @Column(name = "capabilities", columnDefinition = "TEXT")
    private String capabilities;

    /** 支持的文件类型（JSON格式） */
    @Column(name = "supported_file_types", columnDefinition = "TEXT")
    private String supportedFileTypes;

    /** 工作目录 */
    @Column(name = "work_dir", length = 500)
    private String workDir;

    /** 招聘原因 */
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    /** 是否自定义角色 */
    @Column(name = "is_custom_role")
    private Boolean isCustomRole = false;

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

    /** 重新申请次数 */
    @Column(name = "revision_count")
    private Integer revisionCount = 0;

    /** 原申请ID（重新申请时关联） */
    @Column(name = "original_request_id")
    private Long originalRequestId;

    // ===== 执行信息 =====

    /** 创建的Agent ID */
    @Column(name = "created_agent_id", length = 50)
    private String createdAgentId;

    /** 执行时间 */
    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    // ===== 时间戳 =====

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * 申请状态枚举
     */
    public enum Status {
        /** 待审批 */
        PENDING,
        /** 已批准 */
        APPROVED,
        /** 已驳回 */
        REJECTED,
        /** 已执行（招聘完成） */
        EXECUTED,
        /** 已取消 */
        CANCELLED,
        /** 重新申请中 */
        REVISED
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
     * 执行招聘
     */
    public void execute(String agentId) {
        this.status = Status.EXECUTED.name();
        this.createdAgentId = agentId;
        this.executedAt = LocalDateTime.now();
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
     * 创建重新申请
     */
    public RecruitmentRequest createRevision() {
        RecruitmentRequest revision = new RecruitmentRequest();
        revision.requestNo = this.requestNo + "-R" + (this.revisionCount + 1);
        revision.producerId = this.producerId;
        revision.producerName = this.producerName;
        revision.projectId = this.projectId;
        revision.projectName = this.projectName;
        revision.role = this.role;
        revision.roleName = this.roleName;
        revision.employeeName = this.employeeName;
        revision.description = this.description;
        revision.capabilities = this.capabilities;
        revision.supportedFileTypes = this.supportedFileTypes;
        revision.workDir = this.workDir;
        revision.isCustomRole = this.isCustomRole;
        revision.originalRequestId = this.id;
        revision.revisionCount = this.revisionCount + 1;
        revision.status = Status.PENDING.name();

        // 标记原申请为已修订
        this.status = Status.REVISED.name();
        this.updatedAt = LocalDateTime.now();

        return revision;
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
            case "REVISED" -> "已重新申请";
            default -> status;
        };
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRequestNo() { return requestNo; }
    public void setRequestNo(String requestNo) { this.requestNo = requestNo; }

    public String getProducerId() { return producerId; }
    public void setProducerId(String producerId) { this.producerId = producerId; }

    public String getProducerName() { return producerName; }
    public void setProducerName(String producerName) { this.producerName = producerName; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCapabilities() { return capabilities; }
    public void setCapabilities(String capabilities) { this.capabilities = capabilities; }

    public String getSupportedFileTypes() { return supportedFileTypes; }
    public void setSupportedFileTypes(String supportedFileTypes) { this.supportedFileTypes = supportedFileTypes; }

    public String getWorkDir() { return workDir; }
    public void setWorkDir(String workDir) { this.workDir = workDir; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Boolean getIsCustomRole() { return isCustomRole; }
    public void setIsCustomRole(Boolean isCustomRole) { this.isCustomRole = isCustomRole; }

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

    public Integer getRevisionCount() { return revisionCount; }
    public void setRevisionCount(Integer revisionCount) { this.revisionCount = revisionCount; }

    public Long getOriginalRequestId() { return originalRequestId; }
    public void setOriginalRequestId(Long originalRequestId) { this.originalRequestId = originalRequestId; }

    public String getCreatedAgentId() { return createdAgentId; }
    public void setCreatedAgentId(String createdAgentId) { this.createdAgentId = createdAgentId; }

    public LocalDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
