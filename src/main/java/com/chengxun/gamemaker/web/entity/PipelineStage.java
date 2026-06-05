package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 流水线阶段实体
 * 定义流水线的各个执行阶段
 *
 * 主要阶段：
 * - 代码拉取（Checkout）
 * - 代码检查（Lint/Scan）
 * - 单元测试（Unit Test）
 * - 构建打包（Build）
 * - 集成测试（Integration Test）
 * - 部署（Deploy）
 *
 * @author chengxun
 * @since 1.0.0
 */
@Entity
@Table(name = "pipeline_stages", indexes = {
    @Index(name = "idx_stage_pipeline", columnList = "pipelineId"),
    @Index(name = "idx_stage_order", columnList = "stageOrder")
})
public class PipelineStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联流水线ID */
    @Column(name = "pipeline_id", nullable = false)
    private Long pipelineId;

    /** 阶段名称 */
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    /** 阶段描述 */
    @Column(name = "description", length = 500)
    private String description;

    /** 阶段类型 */
    @Column(name = "stage_type", length = 50)
    private String stageType;

    /** 执行顺序 */
    @Column(name = "stage_order", nullable = false)
    private Integer stageOrder;

    /** 执行命令（JSON数组格式） */
    @Column(name = "commands", columnDefinition = "TEXT")
    private String commands;

    /** 阶段状态 */
    @Column(name = "status", length = 20)
    private String status = "PENDING";

    /** 执行日志 */
    @Column(name = "log", columnDefinition = "TEXT")
    private String log;

    /** 错误信息 */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** 开始时间 */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /** 完成时间 */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** 执行耗时（秒） */
    @Column(name = "duration_seconds")
    private Long durationSeconds;

    /** 是否失败时继续 */
    @Column(name = "continue_on_failure")
    private boolean continueOnFailure = false;

    /** 超时时间（秒） */
    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds = 300;

    /**
     * 阶段类型枚举
     */
    public enum StageType {
        /** 代码拉取 */
        CHECKOUT,
        /** 代码检查 */
        LINT,
        /** 单元测试 */
        UNIT_TEST,
        /** 构建打包 */
        BUILD,
        /** 集成测试 */
        INTEGRATION_TEST,
        /** 部署 */
        DEPLOY,
        /** 通知 */
        NOTIFY,
        /** 自定义脚本 */
        SCRIPT
    }

    // 业务方法

    /**
     * 开始执行
     */
    public void start() {
        this.status = "RUNNING";
        this.startedAt = LocalDateTime.now();
        this.log = "";
    }

    /**
     * 执行成功
     */
    public void complete() {
        this.status = "SUCCESS";
        this.completedAt = LocalDateTime.now();
        if (this.startedAt != null) {
            this.durationSeconds = java.time.Duration.between(this.startedAt, this.completedAt).getSeconds();
        }
    }

    /**
     * 执行失败
     */
    public void fail(String errorMessage) {
        this.status = "FAILED";
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
        if (this.startedAt != null) {
            this.durationSeconds = java.time.Duration.between(this.startedAt, this.completedAt).getSeconds();
        }
    }

    /**
     * 添加日志
     */
    public void appendLog(String message) {
        if (this.log == null) {
            this.log = "";
        }
        this.log += message + "\n";
    }

    /**
     * 获取状态描述
     */
    public String getStatusDescription() {
        return switch (status) {
            case "PENDING" -> "等待执行";
            case "RUNNING" -> "执行中";
            case "SUCCESS" -> "成功";
            case "FAILED" -> "失败";
            case "SKIPPED" -> "已跳过";
            default -> status;
        };
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPipelineId() { return pipelineId; }
    public void setPipelineId(Long pipelineId) { this.pipelineId = pipelineId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStageType() { return stageType; }
    public void setStageType(String stageType) { this.stageType = stageType; }

    public Integer getStageOrder() { return stageOrder; }
    public void setStageOrder(Integer stageOrder) { this.stageOrder = stageOrder; }

    public String getCommands() { return commands; }
    public void setCommands(String commands) { this.commands = commands; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getLog() { return log; }
    public void setLog(String log) { this.log = log; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public Long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Long durationSeconds) { this.durationSeconds = durationSeconds; }

    public boolean isContinueOnFailure() { return continueOnFailure; }
    public void setContinueOnFailure(boolean continueOnFailure) { this.continueOnFailure = continueOnFailure; }

    public Integer getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(Integer timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
}
