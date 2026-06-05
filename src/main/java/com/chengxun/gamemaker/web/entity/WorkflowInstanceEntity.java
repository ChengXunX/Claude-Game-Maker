package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 工作流实例持久化实体
 * 记录每次工作流启动后的运行状态和上下文数据
 *
 * 主要功能：
 * - 持久化工作流实例的生命周期状态
 * - 存储启动参数和全局上下文（步骤间共享数据）
 * - 支持暂停/恢复等状态流转
 *
 * @author chengxun
 * @since 2.0.0
 */
@Entity
@Table(name = "workflow_instances", indexes = {
    @Index(name = "idx_wfi_template", columnList = "template_id"),
    @Index(name = "idx_wfi_project", columnList = "project_id"),
    @Index(name = "idx_wfi_status", columnList = "status"),
    @Index(name = "idx_wfi_created", columnList = "created_at")
})
public class WorkflowInstanceEntity {

    /** 实例ID（主键，UUID） */
    @Id
    @Column(name = "id", length = 64)
    private String id;

    /** 关联的模板ID */
    @Column(name = "template_id", length = 64, nullable = false)
    private String templateId;

    /** 所属项目ID */
    @Column(name = "project_id", length = 100)
    private String projectId;

    /** 状态: CREATED/RUNNING/PAUSED/COMPLETED/FAILED/CANCELLED */
    @Column(name = "status", length = 20, nullable = false)
    private String status = "CREATED";

    /** 启动参数JSON */
    @Column(name = "parameters_json", columnDefinition = "TEXT")
    private String parametersJson;

    /** 全局上下文JSON（步骤间共享数据） */
    @Column(name = "context_json", columnDefinition = "TEXT")
    private String contextJson;

    /** 错误信息 */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** 启动时间 */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /** 完成时间 */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ===== Getters and Setters =====

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getParametersJson() { return parametersJson; }
    public void setParametersJson(String parametersJson) { this.parametersJson = parametersJson; }

    public String getContextJson() { return contextJson; }
    public void setContextJson(String contextJson) { this.contextJson = contextJson; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
