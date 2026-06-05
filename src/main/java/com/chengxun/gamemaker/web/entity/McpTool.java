package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * MCP Tool 实体
 * 存储从 MCP Server 发现的工具定义
 *
 * @author chengxun
 * @since 2.0.0
 */
@Entity
@Table(name = "mcp_tools", indexes = {
    @Index(name = "idx_mcp_tool_server", columnList = "serverId"),
    @Index(name = "idx_mcp_tool_name", columnList = "toolName"),
    @Index(name = "idx_mcp_tool_enabled", columnList = "enabled")
})
public class McpTool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属 Server ID */
    @Column(nullable = false)
    private Long serverId;

    /** 工具名称（MCP 原始名称） */
    @Column(nullable = false, length = 100)
    private String toolName;

    /** 显示名称（中文） */
    @Column(length = 100)
    private String displayName;

    /** 工具描述 */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** 输入参数 Schema（JSON 格式） */
    @Column(columnDefinition = "TEXT")
    private String inputSchema;

    /** 工具分类 */
    @Column(length = 50)
    private String category;

    /** 是否需要审批才能调用 */
    @Column(nullable = false)
    private boolean requiresApproval = false;

    /** 是否启用 */
    @Column(nullable = false)
    private boolean enabled = true;

    /** 调用次数统计 */
    @Column(nullable = false)
    private long callCount = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
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

    // ===== Getters & Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }

    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getInputSchema() { return inputSchema; }
    public void setInputSchema(String inputSchema) { this.inputSchema = inputSchema; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isRequiresApproval() { return requiresApproval; }
    public void setRequiresApproval(boolean requiresApproval) { this.requiresApproval = requiresApproval; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public long getCallCount() { return callCount; }
    public void setCallCount(long callCount) { this.callCount = callCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return String.format("McpTool[%s] server=%d, requiresApproval=%b", toolName, serverId, requiresApproval);
    }
}
