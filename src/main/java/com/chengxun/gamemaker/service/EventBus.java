package com.chengxun.gamemaker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 事件总线
 * 支持 Agent 之间的事件驱动协作，解耦发布者和订阅者
 *
 * 核心能力：
 * 1. 项目级隔离：事件只在同一项目内传递
 * 2. 类型化事件：按事件类型订阅，避免接收无关事件
 * 3. 异步处理：事件处理不阻塞发布者
 * 4. 历史记录：保留最近事件用于上下文构建
 *
 * @author chengxun
 * @since 2.0.0
 */
@Component
public class EventBus {

    private static final Logger log = LoggerFactory.getLogger(EventBus.class);

    /** 事件订阅表：projectId:eventType -> handlers */
    private final Map<String, List<Consumer<ProjectEvent>>> subscriptions = new ConcurrentHashMap<>();

    /** 事件历史：projectId -> recent events（保留最近100条） */
    private final Map<String, List<ProjectEvent>> eventHistory = new ConcurrentHashMap<>();

    /** 最大历史记录数 */
    private static final int MAX_HISTORY_SIZE = 100;

    /**
     * 项目事件
     */
    public static class ProjectEvent {
        private final String eventId;
        private final String projectId;
        private final String eventType;
        private final String sourceAgentId;
        private final Map<String, Object> data;
        private final LocalDateTime timestamp;

        public ProjectEvent(String projectId, String eventType, String sourceAgentId, Map<String, Object> data) {
            this.eventId = UUID.randomUUID().toString();
            this.projectId = projectId;
            this.eventType = eventType;
            this.sourceAgentId = sourceAgentId;
            this.data = data != null ? data : new HashMap<>();
            this.timestamp = LocalDateTime.now();
        }

        // Getters
        public String getEventId() { return eventId; }
        public String getProjectId() { return projectId; }
        public String getEventType() { return eventType; }
        public String getSourceAgentId() { return sourceAgentId; }
        public Map<String, Object> getData() { return data; }
        public LocalDateTime getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return String.format("[%s] %s from %s: %s", eventType, projectId, sourceAgentId, data);
        }
    }

    // ===== 事件类型常量 =====

    /** 任务完成 */
    public static final String TASK_COMPLETED = "TASK_COMPLETED";

    /** 里程碑进度更新 */
    public static final String MILESTONE_UPDATED = "MILESTONE_UPDATED";

    /** 里程碑完成 */
    public static final String MILESTONE_COMPLETED = "MILESTONE_COMPLETED";

    /** 发现阻塞 */
    public static final String BLOCKER_DETECTED = "BLOCKER_DETECTED";

    /** 阻塞解除 */
    public static final String BLOCKER_RESOLVED = "BLOCKER_RESOLVED";

    /** 代码就绪 */
    public static final String CODE_READY = "CODE_READY";

    /** 代码变更（Agent 完成代码修改后发布） */
    public static final String CODE_CHANGED = "CODE_CHANGED";

    /** 验证结果（游戏验证完成后发布） */
    public static final String VERIFY_RESULT = "VERIFY_RESULT";

    /** 设计文档就绪 */
    public static final String DESIGN_READY = "DESIGN_READY";

    /** Agent 状态变更 */
    public static final String AGENT_STATUS_CHANGED = "AGENT_STATUS_CHANGED";

    /** 风险预警 */
    public static final String RISK_ALERT = "RISK_ALERT";

    /** 需求变更 */
    public static final String REQUIREMENT_CHANGED = "REQUIREMENT_CHANGED";

    /** 工作流启动 */
    public static final String WORKFLOW_STARTED = "WORKFLOW_STARTED";

    /** 工作流完成 */
    public static final String WORKFLOW_COMPLETED = "WORKFLOW_COMPLETED";

    /**
     * 发布事件
     *
     * @param projectId 项目 ID
     * @param eventType 事件类型
     * @param sourceAgentId 发布者 Agent ID
     * @param data 事件数据
     */
    public void publish(String projectId, String eventType, String sourceAgentId, Map<String, Object> data) {
        if (projectId == null || eventType == null) return;

        ProjectEvent event = new ProjectEvent(projectId, eventType, sourceAgentId, data);

        // 记录到历史
        addToHistory(projectId, event);

        // 通知订阅者
        String subscriptionKey = projectId + ":" + eventType;
        List<Consumer<ProjectEvent>> handlers = subscriptions.get(subscriptionKey);
        if (handlers != null && !handlers.isEmpty()) {
            for (Consumer<ProjectEvent> handler : handlers) {
                try {
                    handler.accept(event);
                } catch (Exception e) {
                    log.error("Event handler failed for event {}: {}", eventType, e.getMessage());
                }
            }
        }

        // 也通知通配符订阅者
        String wildcardKey = projectId + ":*";
        List<Consumer<ProjectEvent>> wildcardHandlers = subscriptions.get(wildcardKey);
        if (wildcardHandlers != null && !wildcardHandlers.isEmpty()) {
            for (Consumer<ProjectEvent> handler : wildcardHandlers) {
                try {
                    handler.accept(event);
                } catch (Exception e) {
                    log.error("Wildcard event handler failed for event {}: {}", eventType, e.getMessage());
                }
            }
        }

        log.debug("Event published: {}:{} from {}", projectId, eventType, sourceAgentId);
    }

    /**
     * 发布事件（简化版，无数据）
     */
    public void publish(String projectId, String eventType, String sourceAgentId) {
        publish(projectId, eventType, sourceAgentId, null);
    }

    /**
     * 订阅事件
     *
     * @param projectId 项目 ID
     * @param eventType 事件类型（使用 "*" 订阅所有事件）
     * @param handler 事件处理器
     */
    public void subscribe(String projectId, String eventType, Consumer<ProjectEvent> handler) {
        if (projectId == null || eventType == null || handler == null) return;

        String key = projectId + ":" + eventType;
        subscriptions.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(handler);
        log.debug("Subscribed to event: {}:{}", projectId, eventType);
    }

    /**
     * 取消订阅
     */
    public void unsubscribe(String projectId, String eventType, Consumer<ProjectEvent> handler) {
        if (projectId == null || eventType == null || handler == null) return;

        String key = projectId + ":" + eventType;
        List<Consumer<ProjectEvent>> handlers = subscriptions.get(key);
        if (handlers != null) {
            handlers.remove(handler);
        }
    }

    /**
     * 获取项目最近事件
     *
     * @param projectId 项目 ID
     * @param limit 最大数量
     * @return 最近事件列表
     */
    public List<ProjectEvent> getRecentEvents(String projectId, int limit) {
        List<ProjectEvent> history = eventHistory.get(projectId);
        if (history == null || history.isEmpty()) {
            return new ArrayList<>();
        }

        int fromIndex = Math.max(0, history.size() - limit);
        return new ArrayList<>(history.subList(fromIndex, history.size()));
    }

    /**
     * 获取项目指定类型的最近事件
     */
    public List<ProjectEvent> getRecentEvents(String projectId, String eventType, int limit) {
        List<ProjectEvent> history = eventHistory.get(projectId);
        if (history == null || history.isEmpty()) {
            return new ArrayList<>();
        }

        return history.stream()
            .filter(e -> eventType.equals(e.getEventType()))
            .limit(limit)
            .toList();
    }

    /**
     * 添加到历史记录
     */
    private void addToHistory(String projectId, ProjectEvent event) {
        List<ProjectEvent> history = eventHistory.computeIfAbsent(projectId, k -> new ArrayList<>());
        synchronized (history) {
            history.add(event);
            // 超过上限时移除最旧的
            while (history.size() > MAX_HISTORY_SIZE) {
                history.remove(0);
            }
        }
    }

    /**
     * 清理项目事件历史
     */
    public void clearHistory(String projectId) {
        eventHistory.remove(projectId);
    }
}
