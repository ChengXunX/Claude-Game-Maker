package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "agent_logs", indexes = {
    @Index(name = "idx_agent_log_agent_id", columnList = "agentId"),
    @Index(name = "idx_agent_log_action", columnList = "action"),
    @Index(name = "idx_agent_log_created", columnList = "createdAt"),
    @Index(name = "idx_agent_log_level", columnList = "level")
})
public class AgentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_id", length = 50)
    private String agentId;

    @Column(name = "agent_name", length = 100)
    private String agentName;

    @Column(length = 50)
    private String action;

    @Column(length = 20)
    private String level = "INFO"; // DEBUG, INFO, WARN, ERROR

    @Column(length = 500)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String detail;

    /** 关联的项目 ID */
    @Column(name = "project_id", length = 100)
    private String projectId;

    /** 关联的任务 ID */
    @Column(name = "task_id", length = 100)
    private String taskId;

    /** 决策原因 / AI 输出片段 */
    @Column(columnDefinition = "TEXT")
    private String decision;

    /** 耗时（毫秒） */
    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Level {
        DEBUG, INFO, WARN, ERROR
    }

    public enum Action {
        /** Agent 接收任务 */
        TASK_RECEIVED,
        /** Agent 开始执行任务 */
        TASK_STARTED,
        /** Agent 完成任务 */
        TASK_COMPLETED,
        /** Agent 任务失败 */
        TASK_FAILED,
        /** Agent 调用 AI */
        AI_CALL,
        /** Agent AI 响应 */
        AI_RESPONSE,
        /** Agent 执行命令 */
        COMMAND_EXEC,
        /** Agent 读取文件 */
        FILE_READ,
        /** Agent 写入文件 */
        FILE_WRITE,
        /** Agent 做出决策 */
        DECISION,
        /** Agent 发送消息 */
        MESSAGE_SENT,
        /** Agent 接收消息 */
        MESSAGE_RECEIVED,
        /** Agent 启动 */
        AGENT_STARTED,
        /** Agent 停止 */
        AGENT_STOPPED,
        /** Agent 错误 */
        AGENT_ERROR,
        /** 系统事件 */
        SYSTEM
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
