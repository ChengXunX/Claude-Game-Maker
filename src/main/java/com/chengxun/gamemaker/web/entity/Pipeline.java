package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

/**
 * CICD流水线实体
 * 定义项目的CI/CD流水线配置和执行记录
 *
 * 主要功能：
 * - 定义流水线阶段（构建、测试、部署等）
 * - 记录流水线执行历史
 * - 支持手动触发执行
 *
 * @author chengxun
 * @since 1.0.0
 */
@Entity
@Table(name = "pipelines", indexes = {
    @Index(name = "idx_pipeline_project", columnList = "projectId"),
    @Index(name = "idx_pipeline_status", columnList = "status"),
    @Index(name = "idx_pipeline_created", columnList = "createdAt")
})
public class Pipeline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 流水线编号 */
    @Column(name = "pipeline_no", length = 50, unique = true)
    private String pipelineNo;

    /** 流水线名称 */
    @NotBlank(message = "name 不能为空")
    @Column(name = "name", length = 200, nullable = false)
    private String name;

    /** 流水线描述 */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 所属项目ID */
    @Column(name = "project_id", nullable = false)
    private String projectId;

    /** 项目名称 */
    @Column(name = "project_name", length = 200)
    private String projectName;

    /** 流水线类型 */
    @Column(name = "pipeline_type", length = 50)
    private String pipelineType;

    /** 流水线配置（JSON格式） */
    @Column(name = "config", columnDefinition = "TEXT")
    private String config;

    /** Git分支 */
    @Column(name = "git_branch", length = 100)
    private String gitBranch;

    /** Git Commit */
    @Column(name = "git_commit", length = 100)
    private String gitCommit;

    /** 流水线状态 */
    @Column(name = "status", length = 20)
    private String status = "IDLE";

    /** 执行进度（0-100） */
    @Column(name = "progress")
    private Integer progress = 0;

    /** 当前执行阶段 */
    @Column(name = "current_stage", length = 100)
    private String currentStage;

    /** 开始时间 */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /** 完成时间 */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** 执行耗时（秒） */
    @Column(name = "duration_seconds")
    private Long durationSeconds;

    /** 触发方式 */
    @Column(name = "trigger_type", length = 50)
    private String triggerType;

    /** 触发人ID */
    @Column(name = "triggered_by")
    private Long triggeredBy;

    /** 触发人名称 */
    @Column(name = "triggered_by_name", length = 100)
    private String triggeredByName;

    /** 执行日志 */
    @Column(name = "execution_log", columnDefinition = "TEXT")
    private String executionLog;

    /** 错误信息 */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // ===== 自动触发和审批相关字段 =====

    /** 是否自动触发 */
    @Column(name = "auto_triggered")
    private boolean autoTriggered = false;

    /** 是否需要审批（生产环境部署等重大节点） */
    @Column(name = "requires_approval")
    private boolean requiresApproval = false;

    /** 审批状态 */
    @Column(name = "approval_status", length = 20)
    private String approvalStatus; // PENDING, APPROVED, REJECTED

    /** 审批人ID */
    @Column(name = "approved_by")
    private Long approvedBy;

    /** 审批人名称 */
    @Column(name = "approved_by_name", length = 100)
    private String approvedByName;

    /** 审批时间 */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /** 审批备注 */
    @Column(name = "approval_comment", columnDefinition = "TEXT")
    private String approvalComment;

    /** 是否为生产环境部署 */
    @Column(name = "production_deploy")
    private boolean productionDeploy = false;

    /** 自动触发条件（JSON格式） */
    @Column(name = "auto_trigger_config", columnDefinition = "TEXT")
    private String autoTriggerConfig;

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 流水线状态枚举
     */
    public enum Status {
        /** 空闲 */
        IDLE,
        /** 等待执行 */
        PENDING,
        /** 执行中 */
        RUNNING,
        /** 成功 */
        SUCCESS,
        /** 失败 */
        FAILED,
        /** 已取消 */
        CANCELLED,
        /** 已暂停 */
        PAUSED
    }

    /**
     * 流水线类型枚举
     */
    public enum PipelineType {
        /** 构建 */
        BUILD,
        /** 测试 */
        TEST,
        /** 部署 */
        DEPLOY,
        /** 完整流水线（构建+测试+部署） */
        FULL,
        /** 自定义 */
        CUSTOM
    }

    /**
     * 触发方式枚举
     */
    public enum TriggerType {
        /** 手动触发 */
        MANUAL,
        /** 飞书触发 */
        FEISHU,
        /** 定时触发 */
        SCHEDULED,
        /** Git触发 */
        GIT_PUSH,
        /** 自动触发 */
        AUTO,
        /** 审批后触发 */
        APPROVAL
    }

    /**
     * 审批状态枚举
     */
    public enum ApprovalStatus {
        /** 待审批 */
        PENDING,
        /** 已批准 */
        APPROVED,
        /** 已拒绝 */
        REJECTED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 业务方法

    /**
     * 开始执行
     */
    public void startExecution(String triggerType, Long triggeredBy, String triggeredByName) {
        this.status = Status.RUNNING.name();
        this.triggerType = triggerType;
        this.triggeredBy = triggeredBy;
        this.triggeredByName = triggeredByName;
        this.startedAt = LocalDateTime.now();
        this.progress = 0;
        this.executionLog = "";
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新进度
     */
    public void updateProgress(int progress, String stage) {
        this.progress = progress;
        this.currentStage = stage;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 添加执行日志
     */
    public void appendLog(String log) {
        if (this.executionLog == null) {
            this.executionLog = "";
        }
        this.executionLog += "[" + LocalDateTime.now() + "] " + log + "\n";
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 执行成功
     */
    public void completeSuccess() {
        this.status = Status.SUCCESS.name();
        this.progress = 100;
        this.completedAt = LocalDateTime.now();
        if (this.startedAt != null) {
            this.durationSeconds = java.time.Duration.between(this.startedAt, this.completedAt).getSeconds();
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 执行失败
     */
    public void completeFailure(String errorMessage) {
        this.status = Status.FAILED.name();
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
        if (this.startedAt != null) {
            this.durationSeconds = java.time.Duration.between(this.startedAt, this.completedAt).getSeconds();
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 取消执行
     */
    public void cancel() {
        this.status = Status.CANCELLED.name();
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 获取状态描述
     */
    public String getStatusDescription() {
        return switch (status) {
            case "IDLE" -> "空闲";
            case "PENDING" -> "等待执行";
            case "RUNNING" -> "执行中";
            case "SUCCESS" -> "成功";
            case "FAILED" -> "失败";
            case "CANCELLED" -> "已取消";
            case "PAUSED" -> "已暂停";
            default -> status;
        };
    }

    /**
     * 获取流水线类型描述
     */
    public String getPipelineTypeDescription() {
        return switch (pipelineType) {
            case "BUILD" -> "构建";
            case "TEST" -> "测试";
            case "DEPLOY" -> "部署";
            case "FULL" -> "完整流水线";
            case "CUSTOM" -> "自定义";
            default -> pipelineType;
        };
    }

    /**
     * 检查是否正在执行
     */
    public boolean isRunning() {
        return Status.RUNNING.name().equals(status) || Status.PENDING.name().equals(status);
    }

    /**
     * 检查是否已完成
     */
    public boolean isCompleted() {
        return Status.SUCCESS.name().equals(status) ||
               Status.FAILED.name().equals(status) ||
               Status.CANCELLED.name().equals(status);
    }

    // ===== 审批相关方法 =====

    /**
     * 提交审批
     */
    public void submitApproval() {
        this.approvalStatus = ApprovalStatus.PENDING.name();
        this.status = Status.PENDING.name();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 批准
     */
    public void approve(Long approvedBy, String approvedByName, String comment) {
        this.approvalStatus = ApprovalStatus.APPROVED.name();
        this.approvedBy = approvedBy;
        this.approvedByName = approvedByName;
        this.approvedAt = LocalDateTime.now();
        this.approvalComment = comment;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 拒绝
     */
    public void reject(Long rejectedBy, String rejectedByName, String reason) {
        this.approvalStatus = ApprovalStatus.REJECTED.name();
        this.approvedBy = rejectedBy;
        this.approvedByName = rejectedByName;
        this.approvedAt = LocalDateTime.now();
        this.approvalComment = reason;
        this.status = Status.CANCELLED.name();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 检查是否需要审批
     */
    public boolean needsApproval() {
        return requiresApproval && ApprovalStatus.PENDING.name().equals(approvalStatus);
    }

    /**
     * 检查是否已批准
     */
    public boolean isApproved() {
        return ApprovalStatus.APPROVED.name().equals(approvalStatus);
    }

    /**
     * 检查是否可以执行
     */
    public boolean canExecute() {
        // 如果需要审批，必须先批准
        if (requiresApproval && !isApproved()) {
            return false;
        }
        // 必须是空闲或失败状态
        return Status.IDLE.name().equals(status) ||
               Status.SUCCESS.name().equals(status) ||
               Status.FAILED.name().equals(status);
    }

    /**
     * 获取审批状态描述
     */
    public String getApprovalStatusDescription() {
        if (approvalStatus == null) {
            return "无需审批";
        }
        return switch (approvalStatus) {
            case "PENDING" -> "待审批";
            case "APPROVED" -> "已批准";
            case "REJECTED" -> "已拒绝";
            default -> approvalStatus;
        };
    }

    /**
     * 暂停执行
     */
    public void pause() {
        if (Status.RUNNING.name().equals(status)) {
            this.status = Status.PAUSED.name();
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 恢复执行
     */
    public void resume() {
        if (Status.PAUSED.name().equals(status)) {
            this.status = Status.RUNNING.name();
            this.updatedAt = LocalDateTime.now();
        }
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPipelineNo() { return pipelineNo; }
    public void setPipelineNo(String pipelineNo) { this.pipelineNo = pipelineNo; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getPipelineType() { return pipelineType; }
    public void setPipelineType(String pipelineType) { this.pipelineType = pipelineType; }

    public String getConfig() { return config; }
    public void setConfig(String config) { this.config = config; }

    public String getGitBranch() { return gitBranch; }
    public void setGitBranch(String gitBranch) { this.gitBranch = gitBranch; }

    public String getGitCommit() { return gitCommit; }
    public void setGitCommit(String gitCommit) { this.gitCommit = gitCommit; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }

    public String getCurrentStage() { return currentStage; }
    public void setCurrentStage(String currentStage) { this.currentStage = currentStage; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public Long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Long durationSeconds) { this.durationSeconds = durationSeconds; }

    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }

    public Long getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(Long triggeredBy) { this.triggeredBy = triggeredBy; }

    public String getTriggeredByName() { return triggeredByName; }
    public void setTriggeredByName(String triggeredByName) { this.triggeredByName = triggeredByName; }

    public String getExecutionLog() { return executionLog; }
    public void setExecutionLog(String executionLog) { this.executionLog = executionLog; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    // 自动触发和审批相关字段

    public boolean isAutoTriggered() { return autoTriggered; }
    public void setAutoTriggered(boolean autoTriggered) { this.autoTriggered = autoTriggered; }

    public boolean isRequiresApproval() { return requiresApproval; }
    public void setRequiresApproval(boolean requiresApproval) { this.requiresApproval = requiresApproval; }

    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }

    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }

    public String getApprovedByName() { return approvedByName; }
    public void setApprovedByName(String approvedByName) { this.approvedByName = approvedByName; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public String getApprovalComment() { return approvalComment; }
    public void setApprovalComment(String approvalComment) { this.approvalComment = approvalComment; }

    public boolean isProductionDeploy() { return productionDeploy; }
    public void setProductionDeploy(boolean productionDeploy) { this.productionDeploy = productionDeploy; }

    public String getAutoTriggerConfig() { return autoTriggerConfig; }
    public void setAutoTriggerConfig(String autoTriggerConfig) { this.autoTriggerConfig = autoTriggerConfig; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
