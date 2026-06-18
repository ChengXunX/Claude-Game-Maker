package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.agent.ProducerAgent;
import com.chengxun.gamemaker.engine.ClaudeCliEngine;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.web.entity.ApiToken;
import com.chengxun.gamemaker.web.entity.TokenUsageRecord;
import com.chengxun.gamemaker.web.repository.ApiTokenRepository;
import com.chengxun.gamemaker.web.repository.TokenUsageRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ApiTokenService {

    private static final Logger log = LoggerFactory.getLogger(ApiTokenService.class);

    private final ApiTokenRepository tokenRepository;
    private final TokenUsageRecordRepository usageRecordRepository;
    private final AgentManager agentManager;
    private final ClaudeCliEngine cliEngine;
    private final SystemConfigService configService;

    public ApiTokenService(ApiTokenRepository tokenRepository, TokenUsageRecordRepository usageRecordRepository,
                           AgentManager agentManager, ClaudeCliEngine cliEngine, SystemConfigService configService) {
        this.tokenRepository = tokenRepository;
        this.usageRecordRepository = usageRecordRepository;
        this.agentManager = agentManager;
        this.cliEngine = cliEngine;
        this.configService = configService;
    }

    public List<ApiToken> getAllTokens() {
        return tokenRepository.findAll();
    }

    /**
     * 保存 Token（创建或更新）
     */
    public ApiToken saveToken(ApiToken token) {
        return tokenRepository.save(token);
    }

    public ApiToken getTokenById(Long id) {
        return tokenRepository.findById(id).orElse(null);
    }

    public List<ApiToken> getActiveTokens() {
        return tokenRepository.findByStatus(ApiToken.TokenStatus.ACTIVE);
    }

    /** 获取 Agent 当前使用的 Token（池化模式：按角色查找最佳 Token） */
    public ApiToken getTokensByAgent(String agentId) {
        Agent agent = agentManager.getAgent(agentId);
        if (agent == null) return null;
        return findBestTokenForRole(agent.getRole());
    }

    public ApiToken createToken(String name, String apiKey, String apiUrl, String model,
                                Integer maxTokens, Integer contextWindow, String description, String createdBy) {
        return createToken(name, apiKey, apiUrl, model, maxTokens, contextWindow, description, createdBy, null);
    }

    /**
     * 创建 Token（支持指定用途）
     */
    public ApiToken createToken(String name, String apiKey, String apiUrl, String model,
                                Integer maxTokens, Integer contextWindow, String description,
                                String createdBy, ApiToken.TokenPurpose purpose) {
        ApiToken token = new ApiToken();
        token.setToken("token-" + java.util.UUID.randomUUID().toString().replace("-", ""));
        token.setName(name);
        token.setApiKey(apiKey);
        token.setApiUrl(apiUrl);
        token.setModel(model);
        token.setMaxTokens(maxTokens);
        token.setContextWindow(contextWindow != null ? contextWindow : 200000);
        token.setDescription(description);
        token.setCreatedBy(createdBy);
        token.setStatus(ApiToken.TokenStatus.ACTIVE);
        token.setPurpose(purpose != null ? purpose : ApiToken.TokenPurpose.AGENT);

        ApiToken saved = tokenRepository.save(token);
        log.info("API token created: {} by {} (contextWindow={}, purpose={})", name, createdBy, token.getContextWindow(), token.getPurpose());
        return saved;
    }

    public ApiToken updateToken(Long id, String name, String apiKey, String apiUrl,
                                String model, Integer maxTokens, Integer contextWindow, String description) {
        ApiToken token = tokenRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Token not found"));

        token.setName(name);
        if (apiKey != null && !apiKey.isEmpty()) {
            token.setApiKey(apiKey);
        }
        token.setApiUrl(apiUrl);
        token.setModel(model);
        token.setMaxTokens(maxTokens);
        if (contextWindow != null) {
            token.setContextWindow(contextWindow);
        }
        token.setDescription(description);

        ApiToken saved = tokenRepository.save(token);
        log.info("API token updated: {} (contextWindow={})", id, token.getContextWindow());
        return saved;
    }

    public void deleteToken(Long id) {
        ApiToken token = tokenRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Token not found"));

        tokenRepository.delete(token);
        log.info("API token deleted: {}", id);
    }

    /**
     * 将 Token 应用到 Agent（池化模式，不做排他绑定）
     * 多个 Agent 可以同时使用同一个 Token
     *
     * @param tokenId    Token ID
     * @param agentId    Agent ID
     * @param activation 生效方式: "immediate" 立即生效, "pending" 等待任务完成
     * @return Token
     */
    public ApiToken assignToken(Long tokenId, String agentId, String activation) {
        ApiToken token = tokenRepository.findById(tokenId)
            .orElseThrow(() -> new RuntimeException("Token not found"));

        if (!token.isActive()) {
            throw new RuntimeException("Token is not active");
        }

        Agent agent = agentManager.getAgent(agentId);

        // 更新 Agent 的上下文窗口大小
        if (agent != null && token.getContextWindow() != null) {
            agent.getDefinition().setMaxContextSize(token.getContextWindow());
            log.info("Updated agent {} contextWindow to {}", agentId, token.getContextWindow());
        }

        // 将 Token 的 API 配置应用到 Agent（池化模式：记录使用的 Token ID）
        if (agent instanceof ProducerAgent producer) {
            producer.assignApiConfig(agentId, token.getApiKey(), token.getApiUrl(), token.getModel());
            producer.getDefinition().setAssignedTokenId(tokenId);
            if ("immediate".equals(activation)) {
                restartAgentProcess(agentId);
            }
            log.info("Token {} applied to ProducerAgent {} ({} activation)", tokenId, agentId, activation);
        } else if (agent != null) {
            if ("pending".equals(activation)) {
                agent.getDefinition().setPendingApiConfig(
                    token.getApiKey(), token.getApiUrl(), token.getModel());
                log.info("Token {} applied to agent {} (pending activation)", tokenId, agentId);
            } else {
                agent.getDefinition().setApiKey(token.getApiKey());
                agent.getDefinition().setApiUrl(token.getApiUrl());
                agent.getDefinition().setModel(token.getModel());
                agent.getDefinition().setAssignedTokenId(tokenId);
                agent.saveContext();
                restartAgentProcess(agentId);
                log.info("Token {} applied to agent {} (immediate activation)", tokenId, agentId);
            }
        }

        return token;
    }

    /**
     * 将 Token 应用到 Agent（默认立即生效）
     */
    public ApiToken assignToken(Long tokenId, String agentId) {
        return assignToken(tokenId, agentId, "immediate");
    }

    /**
     * 应用 Agent 的待生效配置
     * 在 Agent 完成当前任务后调用
     *
     * @param agentId Agent ID
     * @return true 如果有配置被应用
     */
    public boolean applyPendingConfig(String agentId) {
        Agent agent = agentManager.getAgent(agentId);
        if (agent == null) {
            return false;
        }

        boolean applied = agent.getDefinition().applyPendingConfig();
        if (applied) {
            // 重启 Agent 的 CLI 进程
            restartAgentProcess(agentId);
            log.info("Applied pending config for agent: {}", agentId);
        }

        return applied;
    }

    /**
     * 重启 Agent 的 CLI 进程
     * 当 Token 配置变更时，需要重启进程使新的环境变量生效
     *
     * @param agentId Agent ID
     */
    private void restartAgentProcess(String agentId) {
        try {
            cliEngine.destroyProcess(agentId);
            log.info("Restarted CLI process for agent: {}", agentId);
        } catch (Exception e) {
            log.warn("Failed to restart CLI process for agent: {}", agentId, e);
        }
    }

    public ApiToken updateTokenStatus(Long id, ApiToken.TokenStatus status) {
        ApiToken token = tokenRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Token not found"));

        token.setStatus(status);
        ApiToken saved = tokenRepository.save(token);
        log.info("Token {} status updated to {}", id, status);
        return saved;
    }

    public ApiToken recordUsage(Long tokenId, long tokensUsed) {
        ApiToken token = tokenRepository.findById(tokenId)
            .orElseThrow(() -> new RuntimeException("Token not found"));

        token.recordUsage(tokensUsed);
        ApiToken saved = tokenRepository.save(token);

        // 写入使用记录（用于滑动窗口配额计算）
        if (token.getQuotaType() == ApiToken.QuotaType.SLIDING_WINDOW) {
            usageRecordRepository.save(new TokenUsageRecord(tokenId, tokensUsed));
        }

        return saved;
    }

    // ===== 配额管理 =====

    /**
     * 获取 Token 滑动窗口内的已用量
     * 查询窗口时间范围内的累计使用记录
     *
     * @param tokenId Token ID
     * @return 窗口内已用 token 数
     */
    public long getWindowUsage(Long tokenId) {
        ApiToken token = tokenRepository.findById(tokenId).orElse(null);
        if (token == null || token.getQuotaType() != ApiToken.QuotaType.SLIDING_WINDOW) {
            return 0;
        }
        int windowSec = token.getWindowSeconds() != null ? token.getWindowSeconds() : 0;
        if (windowSec <= 0) return 0;
        LocalDateTime windowStart = LocalDateTime.now().minusSeconds(windowSec);
        return usageRecordRepository.sumUsageSince(tokenId, windowStart);
    }

    /**
     * 获取 Token 剩余可用量
     * 根据配额类型计算：
     * - UNLIMITED: 返回 Long.MAX_VALUE
     * - TOTAL: 总量 - 已用总量
     * - SLIDING_WINDOW: 窗口总量 - 窗口内已用量
     *
     * @param tokenId Token ID
     * @return 剩余可用 token 数，-1 表示无限制
     */
    public long getRemainingQuota(Long tokenId) {
        ApiToken token = tokenRepository.findById(tokenId).orElse(null);
        if (token == null) return 0;

        ApiToken.QuotaType qt = token.getQuotaType();
        if (qt == null) return Long.MAX_VALUE; // 未设置配额类型视为无限制
        switch (qt) {
            case UNLIMITED:
                return Long.MAX_VALUE;
            case TOTAL:
                long total = token.getQuotaTotal() != null ? token.getQuotaTotal() : 0;
                long used = token.getTotalTokensUsed() != null ? token.getTotalTokensUsed() : 0;
                return Math.max(0, total - used);
            case SLIDING_WINDOW:
                long quota = token.getQuotaTotal() != null ? token.getQuotaTotal() : 0;
                long windowUsed = getWindowUsage(tokenId);
                return Math.max(0, quota - windowUsed);
            default:
                return Long.MAX_VALUE;
        }
    }

    /**
     * 获取 Token 当前并发 Agent 数
     * 通过 AgentManager 查询正在使用该 Token 的 Agent 数量
     *
     * @param tokenId Token ID
     * @return 当前使用该 Token 的 Agent 数量
     */
    public int getCurrentConcurrentAgents(Long tokenId) {
        ApiToken token = tokenRepository.findById(tokenId).orElse(null);
        if (token == null) return 0;

        int count = 0;
        for (Agent agent : agentManager.getAllAgents()) {
            if (!agent.isAlive()) continue;
            // 检查 Agent 的 API Key 是否匹配该 Token
            String agentKey = agent.getDefinition().getApiKey();
            if (agentKey != null && agentKey.equals(token.getApiKey())) {
                count++;
            }
        }
        return count;
    }

    /**
     * 检查 Token 是否可用（有余量且未超并发限制）
     *
     * @param tokenId Token ID
     * @return true 如果 Token 可用
     */
    public boolean isTokenAvailable(Long tokenId) {
        ApiToken token = tokenRepository.findById(tokenId).orElse(null);
        if (token == null || !token.isActive()) return false;

        // 检查配额余量
        if (token.getQuotaType() != ApiToken.QuotaType.UNLIMITED) {
            long remaining = getRemainingQuota(tokenId);
            if (remaining <= 0) {
                log.debug("Token {} 配额已用尽 (quotaType={})", tokenId, token.getQuotaType());
                return false;
            }
        }

        // 检查并发限制
        if (token.getMaxConcurrentAgents() != null && token.getMaxConcurrentAgents() > 0) {
            int current = getCurrentConcurrentAgents(tokenId);
            if (current >= token.getMaxConcurrentAgents()) {
                log.debug("Token {} 并发已满 ({}/{})", tokenId, current, token.getMaxConcurrentAgents());
                return false;
            }
        }

        return true;
    }

    /**
     * 获取 Token 余量百分比（0-100）
     * 用于前端进度条展示
     *
     * @param tokenId Token ID
     * @return 余量百分比，-1 表示无限制
     */
    public double getQuotaUsagePercent(Long tokenId) {
        ApiToken token = tokenRepository.findById(tokenId).orElse(null);
        if (token == null || token.getQuotaType() == ApiToken.QuotaType.UNLIMITED) {
            return -1; // 无限制
        }

        long total = token.getQuotaTotal() != null ? token.getQuotaTotal() : 0;
        if (total <= 0) return -1;

        long remaining = getRemainingQuota(tokenId);
        return (double) remaining / total * 100;
    }

    /**
     * 清理过期的滑动窗口使用记录
     * 应定期调用，删除超出最大窗口时长的记录
     */
    @Transactional
    public void cleanupExpiredUsageRecords() {
        // 保留最近 48 小时的记录（覆盖所有可能的窗口）
        LocalDateTime cutoff = LocalDateTime.now().minusHours(48);
        usageRecordRepository.deleteByUsedAtBefore(cutoff);
        log.debug("Cleaned up token usage records before {}", cutoff);
    }

    /**
     * 根据 Agent 角色查找最佳可用 Token（负载均衡分配）
     *
     * 分配规则：
     * 1. 必须是 ACTIVE 状态
     * 2. 排除 AI_ASSISTANT 专用 Token（隔离）
     * 3. agentTags 包含该角色（或为空表示通用）
     * 4. 必须有余量（配额未耗尽）
     * 5. 未超过并发限制
     * 6. 负载均衡：优先级 → 当前并发数（少优先） → 余量% → maxTokens
     *
     * @param agentRole Agent 角色
     * @return 最佳 Token，如果没有合适的则返回 null
     */
    public ApiToken findBestTokenForRole(String agentRole) {
        List<ApiToken> candidates = tokenRepository.findAll().stream()
            .filter(ApiToken::isActive)
            .filter(t -> t.getPurpose() != ApiToken.TokenPurpose.AI_ASSISTANT)
            .filter(t -> t.isSuitableForRole(agentRole))
            .filter(t -> isTokenAvailable(t.getId()))
            .collect(java.util.stream.Collectors.toList());

        if (candidates.isEmpty()) return null;

        // 计算角色期望的资源类型
        ApiToken.ResourceType expectedType = getExpectedResourceType(agentRole);

        // 按优先级分组
        Map<Integer, List<ApiToken>> byPriority = candidates.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                t -> t.getPriority() != null ? t.getPriority() : 10));

        // 取最高优先级组（数值最小）
        int bestPriority = byPriority.keySet().stream().min(Integer::compareTo).orElse(10);
        List<ApiToken> bestGroup = byPriority.get(bestPriority);

        // 在同优先级组内，按负载均衡排序
        return bestGroup.stream()
            .sorted((a, b) -> {
                // 1. 资源类型匹配的优先（精确匹配 > 通用 TEXT）
                boolean aMatch = a.getResourceType() == expectedType;
                boolean bMatch = b.getResourceType() == expectedType;
                if (aMatch != bMatch) return aMatch ? -1 : 1;

                // 2. 并发数少的优先（负载均衡核心）
                int ca = getCurrentConcurrentAgents(a.getId());
                int cb = getCurrentConcurrentAgents(b.getId());
                if (ca != cb) return Integer.compare(ca, cb);

                // 3. 余量百分比多的优先
                double ra = getQuotaRemainingPercent(a);
                double rb = getQuotaRemainingPercent(b);
                if (Double.compare(ra, rb) != 0) {
                    return Double.compare(rb, ra);
                }

                // 4. maxTokens 大的优先
                int ma = a.getMaxTokens() != null ? a.getMaxTokens() : 4096;
                int mb = b.getMaxTokens() != null ? b.getMaxTokens() : 4096;
                return Integer.compare(mb, ma);
            })
            .findFirst()
            .orElse(null);
    }

    /**
     * 根据 Agent 角色获取期望的资源类型
     */
    private ApiToken.ResourceType getExpectedResourceType(String agentRole) {
        return switch (agentRole) {
            case "audio-dev" -> ApiToken.ResourceType.AUDIO;
            case "tech-artist", "ui-dev" -> ApiToken.ResourceType.IMAGE;
            default -> ApiToken.ResourceType.TEXT;
        };
    }

    /**
     * 获取 Token 余量百分比（内部方法，直接使用实体避免重复查询）
     */
    private double getQuotaRemainingPercent(ApiToken token) {
        if (token.getQuotaType() == ApiToken.QuotaType.UNLIMITED) return 100.0;
        long total = token.getQuotaTotal() != null ? token.getQuotaTotal() : 0;
        if (total <= 0) return 100.0;
        long remaining = getRemainingQuota(token.getId());
        return (double) remaining / total * 100;
    }

    /**
     * 获取 Token 并发剩余容量（内部方法）
     */
    private int getConcurrentRemaining(ApiToken token) {
        int max = token.getMaxConcurrentAgents() != null ? token.getMaxConcurrentAgents() : 0;
        if (max <= 0) return Integer.MAX_VALUE; // 无限制
        int current = getCurrentConcurrentAgents(token.getId());
        return Math.max(0, max - current);
    }

    /**
     * 获取当前 Token 分配策略
     *
     * @return "system" 或 "producer"，默认 "system"
     */
    public String getAllocationStrategy() {
        return configService.getString("token.allocation.strategy", "system");
    }

    /**
     * 查找 AI 助手专用 Token
     * 优先返回 purpose=AI_ASSISTANT 的活跃 Token
     *
     * @return AI 助手专用 Token，没有则返回 null
     */
    public ApiToken findAiAssistantToken() {
        return tokenRepository.findAll().stream()
            .filter(ApiToken::isActive)
            .filter(t -> t.getPurpose() == ApiToken.TokenPurpose.AI_ASSISTANT)
            .findFirst()
            .orElse(null);
    }

    /**
     * 获取所有可用的 Token（按角色分组）
     *
     * @return 按角色分组的 Token 列表
     */
    public Map<String, List<ApiToken>> getAvailableTokensByRole() {
        Map<String, List<ApiToken>> result = new HashMap<>();
        List<ApiToken> allTokens = tokenRepository.findAll();

        for (ApiToken token : allTokens) {
            if (!token.isActive()) {
                continue;
            }
            // 排除 AI 助手专用 Token
            if (token.getPurpose() == ApiToken.TokenPurpose.AI_ASSISTANT) {
                continue;
            }

            if (token.getAgentTags() == null || token.getAgentTags().isEmpty()) {
                // 通用 Token
                result.computeIfAbsent("general", k -> new ArrayList<>()).add(token);
            } else {
                // 按角色分组
                String[] tags = token.getAgentTags().split(",");
                for (String tag : tags) {
                    String role = tag.trim();
                    if (!role.isEmpty()) {
                        result.computeIfAbsent(role, k -> new ArrayList<>()).add(token);
                    }
                }
            }
        }

        return result;
    }

    public long getActiveTokenCount() {
        return tokenRepository.countByStatus(ApiToken.TokenStatus.ACTIVE);
    }

    /** 获取使用中的 Token 数量（池化模式：有使用记录即为使用中） */
    public long getAssignedTokenCount() {
        return tokenRepository.findAll().stream()
            .filter(ApiToken::isInUse)
            .count();
    }
}
