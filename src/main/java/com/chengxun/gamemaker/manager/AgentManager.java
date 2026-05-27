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
    private final FeishuBotService feishuService;
    
    private final Map<String, Agent> agents = new ConcurrentHashMap<>();
    
    public AgentManager(AppConfig appConfig,
                       ClaudeCliEngine cliEngine,
                       MessageBus messageBus,
                       ContextManager contextManager,
                       MemoryManager memoryManager,
                       FeishuBotService feishuService) {
        this.appConfig = appConfig;
        this.cliEngine = cliEngine;
        this.messageBus = messageBus;
        this.contextManager = contextManager;
        this.memoryManager = memoryManager;
        this.feishuService = feishuService;
    }
    
    public Agent createProducerAgent(AgentDefinition definition) {
        ProducerAgent producer = new ProducerAgent(definition, cliEngine, messageBus,
                contextManager, memoryManager, this, feishuService);
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
        
        ServerDevAgent agent = new ServerDevAgent(definition, cliEngine, messageBus, contextManager, memoryManager);
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
}
