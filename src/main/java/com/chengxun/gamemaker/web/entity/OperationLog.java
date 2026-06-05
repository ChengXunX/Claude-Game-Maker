package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 操作审计日志实体
 * 记录系统中所有重要操作，支持审计和回溯
 *
 * 主要功能：
 * - 记录用户操作
 * - 记录Agent操作
 * - 记录系统事件
 * - 支持操作回溯
 *
 * @author chengxun
 * @since 1.0.0
 */
@Entity
@Table(name = "operation_logs", indexes = {
    @Index(name = "idx_audit_user", columnList = "userId"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_target", columnList = "targetType"),
    @Index(name = "idx_audit_created", columnList = "createdAt"),
    @Index(name = "idx_audit_level", columnList = "level")
})
public class OperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户ID */
    @Column(name = "user_id")
    private Long userId;

    /** 用户名 */
    @Column(name = "username", length = 50)
    private String username;

    /** Agent ID（如果是Agent操作） */
    @Column(name = "agent_id", length = 50)
    private String agentId;

    /** 操作类型 */
    @NotBlank(message = "action 不能为空")
    @Column(length = 50, nullable = false)
    private String action;

    /** 目标类型 */
    @Column(name = "target_type", length = 50)
    private String targetType;

    /** 目标ID */
    @Column(name = "target_id", length = 100)
    private String targetId;

    /** 目标名称 */
    @Column(name = "target_name", length = 200)
    private String targetName;

    /** 操作详情 */
    @Column(columnDefinition = "TEXT")
    private String detail;

    /** 操作级别 */
    @Column(length = 20)
    private String level = "INFO";

    /** 操作状态 */
    @Column(length = 20)
    private String status = "SUCCESS";

    /** 错误信息（如果失败） */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** IP地址 */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /** User Agent */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /** 请求参数（JSON格式） */
    @Column(name = "request_params", columnDefinition = "TEXT")
    private String requestParams;

    /** 响应数据（JSON格式） */
    @Column(name = "response_data", columnDefinition = "TEXT")
    private String responseData;

    /** 耗时（毫秒） */
    @Column(name = "duration_ms")
    private Long durationMs;

    /** 项目ID */
    @Column(name = "project_id", length = 100)
    private String projectId;

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 操作类型枚举
     */
    public enum Action {
        // 用户操作
        USER_LOGIN, USER_LOGOUT, USER_REGISTER,
        USER_CREATE, USER_UPDATE, USER_DELETE,

        // Agent操作
        AGENT_CREATE, AGENT_UPDATE, AGENT_DELETE,
        AGENT_START, AGENT_STOP, AGENT_RESTART,

        // 项目操作
        PROJECT_CREATE, PROJECT_UPDATE, PROJECT_DELETE,

        // 任务操作
        TASK_CREATE, TASK_ASSIGN, TASK_COMPLETE, TASK_FAIL,

        // Token操作
        TOKEN_CREATE, TOKEN_UPDATE, TOKEN_DELETE,

        // 配置操作
        CONFIG_UPDATE,

        // 角色操作
        ROLE_CREATE, ROLE_UPDATE, ROLE_DELETE,

        // 系统操作
        SYSTEM_STARTUP, SYSTEM_SHUTDOWN,
        ALERT_TRIGGER, ALERT_RESOLVE,

        // 代码审查
        REVIEW_SUBMIT, REVIEW_APPROVE, REVIEW_REJECT,

        // 其他
        CUSTOM
    }

    /**
     * 操作级别枚举
     */
    public enum Level {
        DEBUG, INFO, WARN, ERROR
    }

    /**
     * 操作状态枚举
     */
    public enum Status {
        SUCCESS, FAILURE, PENDING
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getRequestParams() { return requestParams; }
    public void setRequestParams(String requestParams) { this.requestParams = requestParams; }

    public String getResponseData() { return responseData; }
    public void setResponseData(String responseData) { this.responseData = responseData; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /**
     * 获取操作描述
     */
    public String getActionDescription() {
        return switch (action) {
            case "USER_LOGIN" -> "用户登录";
            case "USER_LOGOUT" -> "用户登出";
            case "USER_REGISTER" -> "用户注册";
            case "USER_CREATE" -> "创建用户";
            case "USER_UPDATE" -> "更新用户";
            case "USER_DELETE" -> "删除用户";
            case "AGENT_CREATE" -> "创建Agent";
            case "AGENT_UPDATE" -> "更新Agent";
            case "AGENT_DELETE" -> "删除Agent";
            case "AGENT_START" -> "启动Agent";
            case "AGENT_STOP" -> "停止Agent";
            case "AGENT_RESTART" -> "重启Agent";
            case "PROJECT_CREATE" -> "创建项目";
            case "PROJECT_UPDATE" -> "更新项目";
            case "PROJECT_DELETE" -> "删除项目";
            case "TASK_CREATE" -> "创建任务";
            case "TASK_ASSIGN" -> "分配任务";
            case "TASK_COMPLETE" -> "完成任务";
            case "TASK_FAIL" -> "任务失败";
            case "TOKEN_CREATE" -> "创建Token";
            case "TOKEN_UPDATE" -> "更新Token";
            case "TOKEN_DELETE" -> "删除Token";
            case "CONFIG_UPDATE" -> "更新配置";
            case "ROLE_CREATE" -> "创建角色";
            case "ROLE_UPDATE" -> "更新角色";
            case "ROLE_DELETE" -> "删除角色";
            case "ALERT_TRIGGER" -> "触发告警";
            case "ALERT_RESOLVE" -> "解决告警";
            case "REVIEW_SUBMIT" -> "提交审查";
            case "REVIEW_APPROVE" -> "审查通过";
            case "REVIEW_REJECT" -> "审查拒绝";
            default -> action;
        };
    }
}
