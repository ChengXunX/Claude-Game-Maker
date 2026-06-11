package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "api_tokens")
public class ApiToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属用户 ID */
    @Column(name = "user_id")
    private Long userId;

    /** Token 唯一标识 */
    @Column(name = "token", unique = true, length = 255)
    private String token;

    @NotBlank(message = "Token 名称不能为空")
    @Column(name = "token_name", nullable = false, length = 100)
    private String name;

    @NotBlank(message = "API Key 不能为空")
    @Column(name = "api_key", nullable = false)
    private String apiKey;

    @Column(name = "api_url", length = 500)
    private String apiUrl;

    @Column(length = 100)
    private String model;

    @Column(name = "max_tokens")
    private Integer maxTokens = 4096;

    /** 上下文窗口大小（token数），默认 200k，支持 1M 长上下文 */
    @Column(name = "context_window")
    private Integer contextWindow = 200000;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TokenStatus status = TokenStatus.ACTIVE;

    /** 适用的 Agent 角色标签，逗号分隔，如 "server-dev,client-dev,ui-dev" */
    @Column(name = "agent_tags", length = 500)
    private String agentTags;

    /** Token 优先级，数值越小优先级越高，用于自动分配 */
    @Column(name = "priority")
    private Integer priority = 10;

    @Column(name = "assigned_agent_id", length = 50)
    private String assignedAgentId;

    @Column(name = "assigned_agent_name", length = 100)
    private String assignedAgentName;

    @Column(name = "usage_count")
    private Long usageCount = 0L;

    @Column(name = "total_tokens_used")
    private Long totalTokensUsed = 0L;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(length = 500)
    private String description;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum TokenStatus {
        ACTIVE, EXHAUSTED, DISABLED, EXPIRED
    }

    /**
     * Token 用途枚举
     * 用于隔离不同场景的 Token，防止 Agent 使用 AI 助手的 Token
     */
    public enum TokenPurpose {
        AGENT,          // Agent 专用
        AI_ASSISTANT,   // AI 助手专用
        SHARED          // 共享（兼容旧数据）
    }

    /** Token 用途，默认 AGENT */
    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", length = 20)
    private TokenPurpose purpose = TokenPurpose.AGENT;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void recordUsage(long tokensUsed) {
        this.usageCount++;
        this.totalTokensUsed += tokensUsed;
        this.lastUsedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }

    public Integer getContextWindow() { return contextWindow; }
    public void setContextWindow(Integer contextWindow) { this.contextWindow = contextWindow; }

    public TokenStatus getStatus() { return status; }
    public void setStatus(TokenStatus status) { this.status = status; }

    public String getAgentTags() { return agentTags; }
    public void setAgentTags(String agentTags) { this.agentTags = agentTags; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public String getAssignedAgentId() { return assignedAgentId; }
    public void setAssignedAgentId(String assignedAgentId) { this.assignedAgentId = assignedAgentId; }

    public String getAssignedAgentName() { return assignedAgentName; }
    public void setAssignedAgentName(String assignedAgentName) { this.assignedAgentName = assignedAgentName; }

    public Long getUsageCount() { return usageCount; }
    public void setUsageCount(Long usageCount) { this.usageCount = usageCount; }

    public Long getTotalTokensUsed() { return totalTokensUsed; }
    public void setTotalTokensUsed(Long totalTokensUsed) { this.totalTokensUsed = totalTokensUsed; }

    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public TokenPurpose getPurpose() { return purpose; }
    public void setPurpose(TokenPurpose purpose) { this.purpose = purpose; }

    public boolean isActive() { return status == TokenStatus.ACTIVE; }
    public boolean isAssigned() { return assignedAgentId != null && !assignedAgentId.isEmpty(); }

    /**
     * 检查此 Token 是否适用于指定的 Agent 角色
     *
     * @param agentRole Agent 角色
     * @return true 如果适用于该角色
     */
    public boolean isSuitableForRole(String agentRole) {
        if (agentTags == null || agentTags.isEmpty()) {
            return true; // 没有标签限制，适用于所有角色
        }
        String[] tags = agentTags.split(",");
        for (String tag : tags) {
            if (tag.trim().equalsIgnoreCase(agentRole)) {
                return true;
            }
        }
        return false;
    }

    public String getMaskedApiKey() {
        if (apiKey == null || apiKey.length() < 10) return "****";
        return apiKey.substring(0, 6) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
