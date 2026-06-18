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

    /**
     * 配额类型枚举
     * 控制 Token 的使用量限制方式
     */
    public enum QuotaType {
        UNLIMITED,      // 无限制（默认）
        TOTAL,          // 总量配额（如月度总量，用完即止）
        SLIDING_WINDOW  // 滑动窗口（如火山引擎 5 小时窗口，窗口内限额，窗口过后自动恢复）
    }

    /**
     * 提供商类型枚举
     * 标识 Token 对应的 AI 服务提供商类型
     */
    public enum ProviderType {
        ANTHROPIC,              // Anthropic Claude（默认）
        OPENAI_COMPATIBLE,      // OpenAI 兼容格式（DeepSeek/MiniMax/智谱等）
        SUNO,                   // Suno 音乐生成
        ELEVENLABS,             // ElevenLabs 音效生成
        DALL_E,                 // DALL-E 图片生成
        STABILITY,              // Stability AI (Stable Diffusion)
        ZHIPU_IMAGE,            // 智谱图片生成
        CUSTOM_RESOURCE         // 自定义资源 API
    }

    /**
     * 资源类型枚举
     * 标识 Token 生成的内容类型
     */
    public enum ResourceType {
        TEXT,           // 文本/代码（默认，用于代码类 Agent）
        AUDIO,          // 音频（音乐、音效）
        IMAGE,          // 图片（精灵、UI 素材）
        MULTIMODAL      // 多模态（文本+图片）
    }

    /** Token 用途，默认 AGENT */
    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", length = 20)
    private TokenPurpose purpose = TokenPurpose.AGENT;

    /** 提供商类型，默认 ANTHROPIC */
    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", length = 30)
    private ProviderType providerType = ProviderType.ANTHROPIC;

    /** 资源类型，默认 TEXT */
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", length = 20)
    private ResourceType resourceType = ResourceType.TEXT;

    // ===== 配额管理 =====

    /** 配额类型：无限制 / 总量 / 滑动窗口 */
    @Enumerated(EnumType.STRING)
    @Column(name = "quota_type", length = 20)
    private QuotaType quotaType = QuotaType.UNLIMITED;

    /** 配额总量（token 数），0 表示不限 */
    @Column(name = "quota_total")
    private Long quotaTotal = 0L;

    /** 滑动窗口时长（秒），如 5 小时 = 18000 */
    @Column(name = "window_seconds")
    private Integer windowSeconds = 0;

    /** 最大并发 Agent 数，0 表示不限 */
    @Column(name = "max_concurrent_agents")
    private Integer maxConcurrentAgents = 0;

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

    public QuotaType getQuotaType() { return quotaType; }
    public void setQuotaType(QuotaType quotaType) { this.quotaType = quotaType; }

    public Long getQuotaTotal() { return quotaTotal; }
    public void setQuotaTotal(Long quotaTotal) { this.quotaTotal = quotaTotal; }

    public Integer getWindowSeconds() { return windowSeconds; }
    public void setWindowSeconds(Integer windowSeconds) { this.windowSeconds = windowSeconds; }

    public Integer getMaxConcurrentAgents() { return maxConcurrentAgents; }
    public void setMaxConcurrentAgents(Integer maxConcurrentAgents) { this.maxConcurrentAgents = maxConcurrentAgents; }

    public ProviderType getProviderType() { return providerType; }
    public void setProviderType(ProviderType providerType) { this.providerType = providerType; }

    public ResourceType getResourceType() { return resourceType; }
    public void setResourceType(ResourceType resourceType) { this.resourceType = resourceType; }

    public boolean isActive() { return status == TokenStatus.ACTIVE; }
    /** 是否有 Agent 在使用（池化模式：基于使用量判断） */
    public boolean isInUse() { return usageCount != null && usageCount > 0; }

    /**
     * 检查此 Token 是否适用于指定的 Agent 角色
     *
     * 匹配规则：
     * 1. 如果 agentTags 为空，视为通用 Token
     * 2. 如果 agentTags 包含该角色，匹配
     * 3. 如果以上都不匹配，检查 resourceType 与角色的兼容性
     *
     * @param agentRole Agent 角色
     * @return true 如果适用于该角色
     */
    public boolean isSuitableForRole(String agentRole) {
        // 1. agentTags 匹配（显式指定的角色）
        if (agentTags != null && !agentTags.isEmpty()) {
            String[] tags = agentTags.split(",");
            for (String tag : tags) {
                if (tag.trim().equalsIgnoreCase(agentRole)) {
                    return true;
                }
            }
            // 有 agentTags 但不包含此角色，不匹配
            return false;
        }

        // 2. agentTags 为空，根据 resourceType 判断兼容性
        if (resourceType == null || resourceType == ResourceType.TEXT) {
            // TEXT Token 适用于所有代码类角色
            return isCodeAgentRole(agentRole);
        }
        if (resourceType == ResourceType.AUDIO) {
            return "audio-dev".equals(agentRole);
        }
        if (resourceType == ResourceType.IMAGE) {
            return "tech-artist".equals(agentRole) || "ui-dev".equals(agentRole);
        }
        // MULTIMODAL 适用于所有角色
        return true;
    }

    /**
     * 判断是否为代码类 Agent 角色（使用 TEXT Token）
     */
    private boolean isCodeAgentRole(String role) {
        return switch (role) {
            case "server-dev", "client-dev", "git-commit", "system-planner",
                 "numerical-planner", "tester", "security-expert", "data-analyst",
                 "devops", "performance-engineer", "ai-engineer", "narrative-planner",
                 "level-design", "localization", "product-manager",
                 "producer", "verifier", "multi-agent", "ui-dev",
                 "system-optimizer", "game-designer" -> true;
            default -> false;
        };
    }

    public String getMaskedApiKey() {
        if (apiKey == null || apiKey.length() < 10) return "****";
        return apiKey.substring(0, 6) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
