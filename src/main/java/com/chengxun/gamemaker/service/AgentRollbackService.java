package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.agent.BaseAgent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.manager.ContextManager;
import com.chengxun.gamemaker.model.AgentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 回滚服务
 * 支持 Agent 执行失败时回滚到上一个稳定状态
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class AgentRollbackService {

    private static final Logger log = LoggerFactory.getLogger(AgentRollbackService.class);

    private final AgentManager agentManager;
    private final ContextManager contextManager;

    /** Agent 状态快照缓存 */
    private final ConcurrentHashMap<String, AgentSnapshot> snapshotCache = new ConcurrentHashMap<>();

    public AgentRollbackService(AgentManager agentManager, ContextManager contextManager) {
        this.agentManager = agentManager;
        this.contextManager = contextManager;
    }

    /**
     * Agent 状态快照
     */
    public static class AgentSnapshot {
        private final String agentId;
        private final AgentContext context;
        private final String conversationId;
        private final LocalDateTime timestamp;
        private final String description;

        public AgentSnapshot(String agentId, AgentContext context, String conversationId, String description) {
            this.agentId = agentId;
            this.context = context;
            this.conversationId = conversationId;
            this.timestamp = LocalDateTime.now();
            this.description = description;
        }

        public String getAgentId() { return agentId; }
        public AgentContext getContext() { return context; }
        public String getConversationId() { return conversationId; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getDescription() { return description; }
    }

    /**
     * 创建 Agent 状态快照
     */
    public void createSnapshot(String agentId, String description) {
        Agent agent = agentManager.getAgent(agentId);
        if (!(agent instanceof BaseAgent baseAgent)) {
            log.warn("Cannot create snapshot for non-BaseAgent: {}", agentId);
            return;
        }

        AgentContext context = baseAgent.getAgentContext();
        String conversationId = baseAgent.getCurrentConversationId();

        if (context != null) {
            // 深拷贝上下文
            AgentContext snapshotContext = deepCopyContext(context);
            snapshotCache.put(agentId, new AgentSnapshot(agentId, snapshotContext, conversationId, description));
            log.info("Snapshot created for agent {}: {}", agentId, description);
        }
    }

    /**
     * 回滚到最近的快照
     */
    public boolean rollback(String agentId) {
        AgentSnapshot snapshot = snapshotCache.get(agentId);
        if (snapshot == null) {
            log.warn("No snapshot found for agent: {}", agentId);
            return false;
        }

        Agent agent = agentManager.getAgent(agentId);
        if (!(agent instanceof BaseAgent baseAgent)) {
            log.warn("Cannot rollback non-BaseAgent: {}", agentId);
            return false;
        }

        try {
            // 恢复上下文
            AgentContext currentContext = baseAgent.getAgentContext();
            if (currentContext != null && snapshot.getContext() != null) {
                currentContext.setWorkingMemory(snapshot.getContext().getWorkingMemory());
                currentContext.setProjectSummary(snapshot.getContext().getProjectSummary());
            }

            // 保存恢复后的上下文
            baseAgent.saveContext();

            log.info("Agent {} rolled back to snapshot: {}", agentId, snapshot.getDescription());
            return true;
        } catch (Exception e) {
            log.error("Failed to rollback agent {}", agentId, e);
            return false;
        }
    }

    /**
     * 检查是否有可用的快照
     */
    public boolean hasSnapshot(String agentId) {
        return snapshotCache.containsKey(agentId);
    }

    /**
     * 获取快照信息
     */
    public AgentSnapshot getSnapshot(String agentId) {
        return snapshotCache.get(agentId);
    }

    /**
     * 清除快照
     */
    public void clearSnapshot(String agentId) {
        snapshotCache.remove(agentId);
    }

    /**
     * 深拷贝上下文
     */
    private AgentContext deepCopyContext(AgentContext source) {
        AgentContext copy = new AgentContext();
        copy.setAgentId(source.getAgentId());
        copy.setProjectSummary(source.getProjectSummary());
        copy.setCurrentTaskId(source.getCurrentTaskId());
        copy.setSessionId(source.getSessionId());
        copy.setWorkDir(source.getWorkDir());

        // 深拷贝工作记忆（WorkingMemoryItem 是可变对象，需要逐个复制）
        if (source.getWorkingMemory() != null) {
            java.util.List<AgentContext.WorkingMemoryItem> copiedMemory = new java.util.ArrayList<>();
            for (AgentContext.WorkingMemoryItem item : source.getWorkingMemory()) {
                copiedMemory.add(new AgentContext.WorkingMemoryItem(item.getKey(), item.getValue(), item.getUpdatedAt()));
            }
            copy.setWorkingMemory(copiedMemory);
        }

        // 深拷贝已学习模式
        if (source.getLearnedPatterns() != null) {
            copy.setLearnedPatterns(new java.util.ArrayList<>(source.getLearnedPatterns()));
        }

        return copy;
    }
}
