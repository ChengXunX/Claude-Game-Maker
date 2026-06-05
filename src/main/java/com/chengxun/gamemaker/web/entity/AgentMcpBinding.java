package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Agent MCP 绑定实体
 * 记录 Agent 与 MCP Server/Tool 的分配关系
 *
 * 分配粒度：项目 + 角色
 * - 同一项目下同角色的 Agent 共享 MCP 配置
 * - toolId 为 null 表示绑定整个 Server（所有工具）
 * - toolId 非 null 表示绑定特定工具
 *
 * @author chengxun
 * @since 2.0.0
 */
@Entity
@Table(name = "agent_mcp_bindings", indexes = {
    @Index(name = "idx_amb_agent", columnList = "agentRole, projectId"),
    @Index(name = "idx_amb_server", columnList = "serverId"),
    @Index(name = "idx_amb_tool", columnList = "toolId")
})
public class AgentMcpBinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Agent 角色（如 producer, server-dev） */
    @Column(nullable = false, length = 50)
    private String agentRole;

    /** 项目 ID */
    @Column(nullable = false, length = 100)
    private String projectId;

    /** MCP Server ID */
    @Column(nullable = false)
    private Long serverId;

    /** MCP Tool ID（null 表示绑定整个 Server） */
    private Long toolId;

    /** 是否启用 */
    @Column(nullable = false)
    private boolean enabled = true;

    /** 优先级（数字越小越靠前） */
    @Column(nullable = false)
    private int priority = 5;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ===== Getters & Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAgentRole() { return agentRole; }
    public void setAgentRole(String agentRole) { this.agentRole = agentRole; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }

    public Long getToolId() { return toolId; }
    public void setToolId(Long toolId) { this.toolId = toolId; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return String.format("McpBinding[%s:%s] server=%d, tool=%s",
            projectId, agentRole, serverId, toolId != null ? toolId.toString() : "ALL");
    }
}
