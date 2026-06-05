package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.agent.ProducerAgent;
import com.chengxun.gamemaker.engine.ClaudeCliEngine;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.web.entity.ApiToken;
import com.chengxun.gamemaker.web.repository.ApiTokenRepository;
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
    private final AgentManager agentManager;
    private final ClaudeCliEngine cliEngine;

    public ApiTokenService(ApiTokenRepository tokenRepository, AgentManager agentManager,
                           ClaudeCliEngine cliEngine) {
        this.tokenRepository = tokenRepository;
        this.agentManager = agentManager;
        this.cliEngine = cliEngine;
    }

    public List<ApiToken> getAllTokens() {
        return tokenRepository.findAll();
    }

    public ApiToken getTokenById(Long id) {
        return tokenRepository.findById(id).orElse(null);
    }

    public List<ApiToken> getActiveTokens() {
        return tokenRepository.findByStatus(ApiToken.TokenStatus.ACTIVE);
    }

    public List<ApiToken> getTokensByAgent(String agentId) {
        return tokenRepository.findByAssignedAgentId(agentId);
    }

    public ApiToken createToken(String name, String apiKey, String apiUrl, String model,
                                Integer maxTokens, String description, String createdBy) {
        ApiToken token = new ApiToken();
        token.setName(name);
        token.setApiKey(apiKey);
        token.setApiUrl(apiUrl);
        token.setModel(model);
        token.setMaxTokens(maxTokens);
        token.setDescription(description);
        token.setCreatedBy(createdBy);
        token.setStatus(ApiToken.TokenStatus.ACTIVE);

        ApiToken saved = tokenRepository.save(token);
        log.info("API token created: {} by {}", name, createdBy);
        return saved;
    }

    public ApiToken updateToken(Long id, String name, String apiKey, String apiUrl,
                                String model, Integer maxTokens, String description) {
        ApiToken token = tokenRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Token not found"));

        token.setName(name);
        if (apiKey != null && !apiKey.isEmpty()) {
            token.setApiKey(apiKey);
        }
        token.setApiUrl(apiUrl);
        token.setModel(model);
        token.setMaxTokens(maxTokens);
        token.setDescription(description);

        ApiToken saved = tokenRepository.save(token);
        log.info("API token updated: {}", id);
        return saved;
    }

    public void deleteToken(Long id) {
        ApiToken token = tokenRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Token not found"));

        // 如果已分配给 agent，先取消分配
        if (token.isAssigned()) {
            unassignToken(id);
        }

        tokenRepository.delete(token);
        log.info("API token deleted: {}", id);
    }

    /**
     * 绑定 Token 到 Agent
     *
     * @param tokenId    Token ID
     * @param agentId    Agent ID
     * @param activation 生效方式: "immediate" 立即生效, "pending" 等待任务完成
     * @return 更新后的 Token
     */
    public ApiToken assignToken(Long tokenId, String agentId, String activation) {
        ApiToken token = tokenRepository.findById(tokenId)
            .orElseThrow(() -> new RuntimeException("Token not found"));

        if (!token.isActive()) {
            throw new RuntimeException("Token is not active");
        }

        // 获取 agent 名称
        Agent agent = agentManager.getAgent(agentId);
        String agentName = agent != null ? agent.getName() : agentId;

        token.setAssignedAgentId(agentId);
        token.setAssignedAgentName(agentName);

        ApiToken saved = tokenRepository.save(token);

        // 如果是 ProducerAgent，调用分配 API 配置
        if (agent instanceof ProducerAgent producer) {
            producer.assignApiConfig(agentId, token.getApiKey(), token.getApiUrl(), token.getModel());
        } else if (agent != null) {
            if ("pending".equals(activation)) {
                // 等待任务完成：设置待生效配置
                agent.getDefinition().setPendingApiConfig(
                    token.getApiKey(), token.getApiUrl(), token.getModel());
                log.info("Token {} assigned to agent {} (pending activation)", tokenId, agentId);
            } else {
                // 立即生效：直接更新配置并重启进程
                agent.getDefinition().setApiKey(token.getApiKey());
                agent.getDefinition().setApiUrl(token.getApiUrl());
                agent.getDefinition().setModel(token.getModel());
                agent.saveContext();

                // 重启 Agent 的 CLI 进程，使新的 API 配置生效
                restartAgentProcess(agentId);
                log.info("Token {} assigned to agent {} (immediate activation)", tokenId, agentId);
            }
        }

        return saved;
    }

    /**
     * 绑定 Token 到 Agent（默认立即生效）
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

    public ApiToken unassignToken(Long tokenId) {
        ApiToken token = tokenRepository.findById(tokenId)
            .orElseThrow(() -> new RuntimeException("Token not found"));

        String oldAgentId = token.getAssignedAgentId();
        token.setAssignedAgentId(null);
        token.setAssignedAgentName(null);

        ApiToken saved = tokenRepository.save(token);
        log.info("Token {} unassigned from agent {}", tokenId, oldAgentId);
        return saved;
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
        return tokenRepository.save(token);
    }

    public ApiToken getActiveTokenForAgent(String agentId) {
        return tokenRepository.findByAssignedAgentIdAndStatus(agentId, ApiToken.TokenStatus.ACTIVE)
            .orElse(null);
    }

    /**
     * 根据 Agent 角色查找最佳可用 Token
     *
     * 选择规则：
     * 1. 必须是 ACTIVE 状态
     * 2. 未分配给其他 Agent
     * 3. agentTags 包含该角色（或为空表示通用）
     * 4. 按 priority 升序排序（数值越小优先级越高）
     *
     * @param agentRole Agent 角色
     * @return 最佳 Token，如果没有合适的则返回 null
     */
    public ApiToken findBestTokenForRole(String agentRole) {
        return tokenRepository.findAll().stream()
            .filter(ApiToken::isActive)
            .filter(t -> !t.isAssigned())
            .filter(t -> t.isSuitableForRole(agentRole))
            .min((a, b) -> Integer.compare(
                a.getPriority() != null ? a.getPriority() : 10,
                b.getPriority() != null ? b.getPriority() : 10
            ))
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
            if (!token.isActive() || token.isAssigned()) {
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

    public long getAssignedTokenCount() {
        return tokenRepository.findAll().stream()
            .filter(ApiToken::isAssigned)
            .count();
    }
}
