package com.chengxun.gamemaker.manager;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.agent.ProducerAgent;
import com.chengxun.gamemaker.agent.ServerDevAgent;
import com.chengxun.gamemaker.config.AppConfig;
import com.chengxun.gamemaker.engine.ClaudeCliEngine;
import com.chengxun.gamemaker.engine.MessageBus;
import com.chengxun.gamemaker.feishu.FeishuBotService;
import com.chengxun.gamemaker.model.AgentDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AgentManager {

    private static final Logger log = LoggerFactory.getLogger(AgentManager.class);

    private final AppConfig appConfig;
    private final ClaudeCliEngine cliEngine;
    private final MessageBus messageBus;
    private final ContextManager contextManager;
    private final MemoryManager memoryManager;
    private final SkillManager skillManager;
    private final FeishuBotService feishuService;

    private final Map<String, Agent> agents = new ConcurrentHashMap<>();

    public AgentManager(AppConfig appConfig,
                       ClaudeCliEngine cliEngine,
                       MessageBus messageBus,
                       ContextManager contextManager,
                       MemoryManager memoryManager,
                       SkillManager skillManager,
                       FeishuBotService feishuService) {
        this.appConfig = appConfig;
        this.cliEngine = cliEngine;
        this.messageBus = messageBus;
        this.contextManager = contextManager;
        this.memoryManager = memoryManager;
        this.skillManager = skillManager;
        this.feishuService = feishuService;
    }

    public Agent createProducerAgent(AgentDefinition definition) {
        ProducerAgent producer = new ProducerAgent(definition, cliEngine, messageBus,
                contextManager, memoryManager, skillManager, this, feishuService);
        producer.initialize();
        producer.start();
        agents.put(definition.getId(), producer);
        log.info("Producer agent created: {}", definition.getName());
        return producer;
    }

    public Agent createAgent(AgentDefinition definition) {
        if ("producer".equals(definition.getRole())) {
            return createProducerAgent(definition);
        }

        ServerDevAgent agent = new ServerDevAgent(definition, cliEngine, messageBus, contextManager, memoryManager, skillManager);
        agent.initialize();
        agent.start();
        agents.put(definition.getId(), agent);

        log.info("Agent created: {} ({})", definition.getName(), definition.getId());
        return agent;
    }

    public Agent getAgent(String agentId) {
        return agents.get(agentId);
    }

    public List<Agent> getAllAgents() {
        return new ArrayList<>(agents.values());
    }

    public List<Agent> getAgentsByRole(String role) {
        return agents.values().stream()
            .filter(a -> a.getRole().equals(role))
            .toList();
    }

    public void removeAgent(String agentId) {
        Agent agent = agents.remove(agentId);
        if (agent != null) {
            agent.stop();
            messageBus.unregisterAgent(agentId);
            log.info("Agent removed: {}", agentId);
        }
    }

    public void stopAll() {
        agents.values().forEach(Agent::stop);
        log.info("All agents stopped");
    }

    public Map<String, Object> getAgentStatus(String agentId) {
        Agent agent = agents.get(agentId);
        if (agent == null) {
            return null;
        }

        Map<String, Object> status = new java.util.HashMap<>();
        status.put("id", agent.getId());
        status.put("name", agent.getName());
        status.put("role", agent.getRole());
        status.put("busy", agent.isBusy());
        status.put("alive", agent.isAlive());
        status.put("taskCount", agent.getTasks().size());

        // 添加上下文信息
        if (agent instanceof com.chengxun.gamemaker.agent.BaseAgent baseAgent) {
            status.put("conversationId", baseAgent.getCurrentConversationId());
            status.put("workingMemorySize", baseAgent.getAgentContext().getWorkingMemory().size());
            status.put("learnedPatternsCount", baseAgent.getAgentContext().getLearnedPatterns().size());
        }

        return status;
    }
}
