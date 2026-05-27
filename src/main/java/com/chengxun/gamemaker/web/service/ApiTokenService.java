package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.agent.ProducerAgent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.web.entity.ApiToken;
import com.chengxun.gamemaker.web.repository.ApiTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApiTokenService {

    private static final Logger log = LoggerFactory.getLogger(ApiTokenService.class);

    private final ApiTokenRepository tokenRepository;
    private final AgentManager agentManager;

    public ApiTokenService(ApiTokenRepository tokenRepository, AgentManager agentManager) {
        this.tokenRepository = tokenRepository;
        this.agentManager = agentManager;
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

    public ApiToken assignToken(Long tokenId, String agentId) {
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
            agent.getDefinition().setApiKey(token.getApiKey());
            agent.getDefinition().setApiUrl(token.getApiUrl());
            agent.getDefinition().setModel(token.getModel());
            agent.saveContext();
        }

        log.info("Token {} assigned to agent {}", tokenId, agentId);
        return saved;
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

    public long getActiveTokenCount() {
        return tokenRepository.countByStatus(ApiToken.TokenStatus.ACTIVE);
    }

    public long getAssignedTokenCount() {
        return tokenRepository.findAll().stream()
            .filter(ApiToken::isAssigned)
            .count();
    }
}
