package com.chengxun.gamemaker.engine;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.model.AgentMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MessageBus {
    
    private static final Logger log = LoggerFactory.getLogger(MessageBus.class);
    
    private final Map<String, Agent> agents = new ConcurrentHashMap<>();
    
    public void registerAgent(Agent agent) {
        agents.put(agent.getId(), agent);
        log.info("Agent registered with message bus: {}", agent.getId());
    }
    
    public void unregisterAgent(String agentId) {
        agents.remove(agentId);
        log.info("Agent unregistered from message bus: {}", agentId);
    }
    
    public void send(AgentMessage message) {
        if (message.getToAgentId() == null || message.getToAgentId().isEmpty()) {
            broadcast(message);
        } else {
            deliver(message);
        }
    }
    
    private void deliver(AgentMessage message) {
        Agent recipient = agents.get(message.getToAgentId());
        if (recipient != null) {
            recipient.receiveMessage(message);
            log.debug("Message delivered from {} to {}", 
                message.getFromAgentId(), message.getToAgentId());
        } else {
            log.warn("Agent not found: {}", message.getToAgentId());
        }
    }
    
    private void broadcast(AgentMessage message) {
        for (Map.Entry<String, Agent> entry : agents.entrySet()) {
            if (!entry.getKey().equals(message.getFromAgentId())) {
                entry.getValue().receiveMessage(message);
            }
        }
        log.debug("Message broadcast from {} to {} agents", 
            message.getFromAgentId(), agents.size() - 1);
    }
    
    public Map<String, Agent> getAgents() {
        return new ConcurrentHashMap<>(agents);
    }
}
