package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.config.SystemConstants;
import com.chengxun.gamemaker.web.entity.AgentCapability;
import com.chengxun.gamemaker.web.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prompt 缓存服务
 * 缓存能力列表 Prompt 和 MCP 配置，避免每次 sendMessage 都重建
 *
 * 主要功能：
 * - 缓存能力 Prompt（按 role:projectId 维度）
 * - 缓存 MCP 配置（按 role:projectId 维度）
 * - 能力变更时自动失效对应缓存
 * - TTL 过期自动清除
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class PromptCacheService {

    private static final Logger log = LoggerFactory.getLogger(PromptCacheService.class);

    @Autowired
    private SystemConfigService configService;

    @Autowired(required = false)
    private CapabilityRegistry capabilityRegistry;

    @jakarta.annotation.PostConstruct
    public void init() {
        // 注册为能力变更监听器，能力定义变更时自动清除对应角色的缓存
        if (capabilityRegistry != null) {
            capabilityRegistry.addChangeListener(this::invalidateCapabilityPrompt);
            log.info("PromptCacheService registered as capability change listener");
        }
    }

    /**
     * 能力 Prompt 缓存
     * key: role:projectId
     * value: 缓存条目
     */
    private final ConcurrentHashMap<String, CacheEntry> capabilityPromptCache = new ConcurrentHashMap<>();

    /**
     * MCP 配置缓存
     * key: role:projectId
     * value: 缓存条目
     */
    private final ConcurrentHashMap<String, CacheEntry> mcpConfigCache = new ConcurrentHashMap<>();

    /**
     * 协作上下文缓存
     * key: role:projectId
     * value: 缓存条目
     */
    private final ConcurrentHashMap<String, CacheEntry> collaborationContextCache = new ConcurrentHashMap<>();

    /**
     * 缓存条目
     */
    private static class CacheEntry {
        final String value;
        final long createdAt;
        final long ttlMillis;

        CacheEntry(String value, long ttlMillis) {
            this.value = value;
            this.createdAt = System.currentTimeMillis();
            this.ttlMillis = ttlMillis;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - createdAt > ttlMillis;
        }
    }

    /**
     * 获取缓存的能力 Prompt
     *
     * @param role Agent 角色
     * @param projectId 项目 ID（可为 null）
     * @return 缓存的 Prompt，不存在或已过期返回 null
     */
    public String getCachedCapabilityPrompt(String role, String projectId) {
        String key = buildKey(role, projectId);
        CacheEntry entry = capabilityPromptCache.get(key);
        if (entry == null || entry.isExpired()) {
            if (entry != null) {
                capabilityPromptCache.remove(key);
            }
            return null;
        }
        return entry.value;
    }

    /**
     * 缓存能力 Prompt
     *
     * @param role Agent 角色
     * @param projectId 项目 ID（可为 null）
     * @param prompt Prompt 内容
     */
    public void cacheCapabilityPrompt(String role, String projectId, String prompt) {
        String key = buildKey(role, projectId);
        long ttl = configService.getInt(SystemConstants.AGENT_CAPABILITY_PROMPT_CACHE_TTL, 300) * 1000L;
        capabilityPromptCache.put(key, new CacheEntry(prompt, ttl));
        log.debug("Cached capability prompt for key: {} (ttl: {}ms)", key, ttl);
    }

    /**
     * 获取缓存的 MCP 配置
     *
     * @param role Agent 角色
     * @param projectId 项目 ID（可为 null）
     * @return 缓存的 MCP 配置 JSON，不存在或已过期返回 null
     */
    public String getCachedMcpConfig(String role, String projectId) {
        String key = buildKey(role, projectId);
        CacheEntry entry = mcpConfigCache.get(key);
        if (entry == null || entry.isExpired()) {
            if (entry != null) {
                mcpConfigCache.remove(key);
            }
            return null;
        }
        return entry.value;
    }

    /**
     * 缓存 MCP 配置
     *
     * @param role Agent 角色
     * @param projectId 项目 ID（可为 null）
     * @param mcpConfig MCP 配置 JSON
     */
    public void cacheMcpConfig(String role, String projectId, String mcpConfig) {
        String key = buildKey(role, projectId);
        long ttl = configService.getInt(SystemConstants.AGENT_CAPABILITY_PROMPT_CACHE_TTL, 300) * 1000L;
        mcpConfigCache.put(key, new CacheEntry(mcpConfig, ttl));
        log.debug("Cached MCP config for key: {}", key);
    }

    /**
     * 获取缓存的协作上下文
     *
     * @param role Agent 角色
     * @param projectId 项目 ID（可为 null）
     * @return 缓存的协作上下文，不存在或已过期返回 null
     */
    public String getCachedCollaborationContext(String role, String projectId) {
        String key = buildKey(role, projectId);
        CacheEntry entry = collaborationContextCache.get(key);
        if (entry == null || entry.isExpired()) {
            if (entry != null) {
                collaborationContextCache.remove(key);
            }
            return null;
        }
        return entry.value;
    }

    /**
     * 缓存协作上下文
     *
     * @param role Agent 角色
     * @param projectId 项目 ID（可为 null）
     * @param context 协作上下文内容
     */
    public void cacheCollaborationContext(String role, String projectId, String context) {
        String key = buildKey(role, projectId);
        long ttl = configService.getInt(SystemConstants.AGENT_COLLABORATION_CONTEXT_CACHE_TTL, 60) * 1000L;
        collaborationContextCache.put(key, new CacheEntry(context, ttl));
        log.debug("Cached collaboration context for key: {} (ttl: {}ms)", key, ttl);
    }

    /**
     * 使指定角色的能力 Prompt 缓存失效
     * 当能力定义变更时调用
     *
     * @param role Agent 角色
     */
    public void invalidateCapabilityPrompt(String role) {
        String prefix = role + ":";
        capabilityPromptCache.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
        log.info("Invalidated capability prompt cache for role: {}", role);
    }

    /**
     * 使所有缓存失效
     */
    public void invalidateAll() {
        capabilityPromptCache.clear();
        mcpConfigCache.clear();
        collaborationContextCache.clear();
        log.info("All prompt caches invalidated");
    }

    /**
     * 清理过期缓存条目
     */
    public void cleanupExpired() {
        int removed = 0;
        removed += capabilityPromptCache.entrySet().removeIf(e -> e.getValue().isExpired()) ? 1 : 0;
        removed += mcpConfigCache.entrySet().removeIf(e -> e.getValue().isExpired()) ? 1 : 0;
        removed += collaborationContextCache.entrySet().removeIf(e -> e.getValue().isExpired()) ? 1 : 0;
        if (removed > 0) {
            log.debug("Cleaned up expired cache entries");
        }
    }

    /**
     * 获取缓存统计
     */
    public Map<String, Object> getStats() {
        return Map.of(
            "capabilityPromptCacheSize", capabilityPromptCache.size(),
            "mcpConfigCacheSize", mcpConfigCache.size(),
            "collaborationContextCacheSize", collaborationContextCache.size()
        );
    }

    private String buildKey(String role, String projectId) {
        return projectId != null ? role + ":" + projectId : role + ":global";
    }
}
