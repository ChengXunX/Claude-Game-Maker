package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 工作流步骤执行持久化实体
 * 记录每个步骤的执行状态、输入输出数据、重试信息
 *
 * 主要功能：
 * - 持久化步骤级别的执行状态
 * - 存储步骤的输入/输出数据（支持步骤间数据传递）
 * - 记录重试次数和超时配置
 * - 关联执行该步骤的Agent信息
 *
 * @author chengxun
 * @since 2.0.0
 */
@Entity
@Table(name = "workflow_step_executions", indexes = {
    @Index(name = "idx_wfse_instance", columnList = "instance_id"),
    @Index(name = "idx_wfse_status", columnList = "status"),
    @Index(name = "idx_wfse_agent", columnList = "agent_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_wfse_instance_step", columnNames = {"instance_id", "step_id"})
})
public class WorkflowStepExecutionEntity {

    /** 执行ID（主键，自增） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联的实例ID */
    @Column(name = "instance_id", length = 64, nullable = false)
    private String instanceId;

    /** 步骤ID（模板中定义的） */
    @Column(name = "step_id", length = 64, nullable = false)
    private String stepId;

    /** 分配的Agent运行时ID */
    @Column(name = "agent_id", length = 100)
    private String agentId;

    /** Agent角色 */
    @Column(name = "agent_role", length = 50)
    private String agentRole;

    /** 状态: PENDING/WAITING_DEPENDENCIES/READY/RUNNING/COMPLETED/FAILED/SKIPPED */
    @Column(name = "status", length = 20, nullable = false)
    private String status = "PENDING";

    /** 输入数据JSON（从依赖步骤收集） */
    @Column(name = "input_data_json", columnDefinition = "TEXT")
    private String inputDataJson;

    /** 输出数据JSON（本步骤执行结果） */
    @Column(name = "output_data_json", columnDefinition = "TEXT")
    private String outputDataJson;

    /** 错误信息 */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** 当前重试次数 */
    @Column(name = "retry_count")
    private Integer retryCount = 0;

    /** 最大重试次数 */
    @Column(name = "max_retries")
    private Integer maxRetries = 3;

    /** 超时时间（分钟） */
    @Column(name = "timeout_minutes")
    private Integer timeoutMinutes = 30;

    /** 开始时间 */
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }

    public String getStepId() { return stepId; }
    public void setStepId(String stepId) { this.stepId = stepId; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getAgentRole() { return agentRole; }
    public void setAgentRole(String agentRole) { this.agentRole = agentRole; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getInputDataJson() { return inputDataJson; }
    public void setInputDataJson(String inputDataJson) { this.inputDataJson = inputDataJson; }

    public String getOutputDataJson() { return outputDataJson; }
    public void setOutputDataJson(String outputDataJson) { this.outputDataJson = outputDataJson; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }

    public Integer getTimeoutMinutes() { return timeoutMinutes; }
    public void setTimeoutMinutes(Integer timeoutMinutes) { this.timeoutMinutes = timeoutMinutes; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
