package com.chengxun.gamemaker.engine;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.model.AgentMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存消息总线（默认实现）
 * 基于内存的同步消息传递，适用于开发和测试环境
 *
 * 核心设计：项目级消息隔离
 * - Agent 注册时携带 projectId，消息只在同一项目内传递
 * - 广播只广播给同项目的 Agent，不会跨项目
 * - 不同项目的 Agent 完全隔离，互不影响
 *
 * @author chengxun
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(name = "app.messaging.type", havingValue = "memory", matchIfMissing = true)
public class MessageBus implements MessageBusInterface {

    private static final Logger log = LoggerFactory.getLogger(MessageBus.class);

    /**
     * 全局 Agent 映射表（key: 运行时 ID = projectId:agentRole）
     * 用于通过 ID 快速查找 Agent
     */
    private final Map<String, Agent> agents = new ConcurrentHashMap<>();

    /**
     * Agent 到 projectId 的映射（key: 运行时 ID, value: projectId）
     * 用于在消息路由时确定 Agent 所属项目
     */
    private final Map<String, String> agentProjectMap = new ConcurrentHashMap<>();

    /**
     * 项目级 Agent 分组（key: projectId, value: 该项目下的 Agent 映射）
     * 用于广播时只发送给同项目 Agent
     */
    private final Map<String, Map<String, Agent>> projectAgents = new ConcurrentHashMap<>();

    @Override
    public void registerAgent(Agent agent) {
        // 无项目关联的注册（向后兼容）
        agents.put(agent.getId(), agent);
        log.info("Agent registered with message bus (no project): {}", agent.getId());
    }

    @Override
    public void registerAgent(Agent agent, String projectId) {
        String agentId = agent.getId();
        agents.put(agentId, agent);
        agentProjectMap.put(agentId, projectId);
        projectAgents.computeIfAbsent(projectId, k -> new ConcurrentHashMap<>()).put(agentId, agent);
        log.info("Agent registered with message bus: {} for project: {}", agentId, projectId);
    }

    @Override
    public void unregisterAgent(String agentId) {
        Agent removed = agents.remove(agentId);
        String projectId = agentProjectMap.remove(agentId);

        if (projectId != null) {
            Map<String, Agent> projectMap = projectAgents.get(projectId);
            if (projectMap != null) {
                projectMap.remove(agentId);
                // 清理空的项目映射
                if (projectMap.isEmpty()) {
                    projectAgents.remove(projectId);
                }
            }
        }

        if (removed != null) {
            log.info("Agent unregistered from message bus: {} (project: {})", agentId, projectId);
        }
    }

    @Override
    public void send(AgentMessage message) {
        if (message.getToAgentId() == null || message.getToAgentId().isEmpty()) {
            broadcast(message);
        } else {
            deliver(message);
        }
    }

    /**
     * 发送消息到指定 Agent
     * 消息只在同一项目内投递
     */
    private void deliver(AgentMessage message) {
        Agent recipient = agents.get(message.getToAgentId());
        if (recipient != null) {
            // 检查发送者和接收者是否在同一项目
            String senderProject = agentProjectMap.get(message.getFromAgentId());
            String recipientProject = agentProjectMap.get(message.getToAgentId());

            if (senderProject != null && recipientProject != null && !senderProject.equals(recipientProject)) {
                log.warn("Cross-project message blocked: {} -> {} (projects: {} -> {})",
                    message.getFromAgentId(), message.getToAgentId(), senderProject, recipientProject);
                return;
            }

            recipient.receiveMessage(message);
            log.debug("Message delivered from {} to {}",
                message.getFromAgentId(), message.getToAgentId());
        } else {
            log.debug("Agent not found: {}", message.getToAgentId());
        }
    }

    /**
     * 广播消息给同项目的所有 Agent
     * 只广播给与发送者同一项目的 Agent
     */
    private void broadcast(AgentMessage message) {
        String senderProject = agentProjectMap.get(message.getFromAgentId());

        if (senderProject != null) {
            // 项目内广播
            Map<String, Agent> projectMap = projectAgents.get(senderProject);
            if (projectMap != null) {
                int count = 0;
                for (Map.Entry<String, Agent> entry : projectMap.entrySet()) {
                    if (!entry.getKey().equals(message.getFromAgentId())) {
                        entry.getValue().receiveMessage(message);
                        count++;
                    }
                }
                log.debug("Message broadcast from {} to {} agents in project {}",
                    message.getFromAgentId(), count, senderProject);
            }
        } else {
            // 发送者没有项目关联，向后兼容：广播给所有无项目关联的 Agent
            int count = 0;
            for (Map.Entry<String, Agent> entry : agents.entrySet()) {
                if (!entry.getKey().equals(message.getFromAgentId()) && !agentProjectMap.containsKey(entry.getKey())) {
                    entry.getValue().receiveMessage(message);
                    count++;
                }
            }
            log.debug("Message broadcast from {} to {} unprojected agents",
                message.getFromAgentId(), count);
        }
    }

    @Override
    public Map<String, Agent> getAgents() {
        return new ConcurrentHashMap<>(agents);
    }

    @Override
    public Map<String, Agent> getProjectAgents(String projectId) {
        Map<String, Agent> projectMap = projectAgents.get(projectId);
        return projectMap != null ? new ConcurrentHashMap<>(projectMap) : new ConcurrentHashMap<>();
    }
}
