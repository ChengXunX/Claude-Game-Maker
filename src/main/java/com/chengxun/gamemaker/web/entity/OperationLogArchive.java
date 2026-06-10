package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 操作日志归档实体
 * 用于存储从 operation_logs 迁移过来的旧数据
 * 结构与 OperationLog 完全一致，仅表名不同
 *
 * @author chengxun
 * @since 2.0.0
 */
@Entity
@Table(name = "operation_logs_archive", indexes = {
    @Index(name = "idx_archive_created", columnList = "createdAt"),
    @Index(name = "idx_archive_action", columnList = "operation")
})
public class OperationLogArchive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 50)
    private String username;

    @Column(name = "agent_id", length = 50)
    private String agentId;

    @Column(name = "operation", length = 50, nullable = false)
    private String action;

    @Column(name = "target_type", length = 50)
    private String targetType;

    @Column(name = "target_id", length = 100)
    private String targetId;

    @Column(name = "target_name", length = 200)
    private String targetName;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(length = 20)
    private String level;

    @Column(length = 20)
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "request_params", columnDefinition = "TEXT")
    private String requestParams;

    @Column(name = "response_data", columnDefinition = "TEXT")
    private String responseData;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "project_id", length = 100)
    private String projectId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

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
}
