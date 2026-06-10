package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.agent.BaseAgent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.model.AgentMessage;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.model.GameProject.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 协作协调器
 * 管理 Agent 间的依赖关系、协作流程、冲突解决
 *
 * 核心能力：
 * 1. 依赖管理 - 自动处理任务间依赖
 * 2. 协作流程 - 协调多 Agent 协作完成复杂任务
 * 3. 冲突检测 - 检测并解决 Agent 间的冲突
 * 4. 信息同步 - 确保关键信息在 Agent 间同步
 * 5. 状态追踪 - 追踪整个协作流程的状态
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class CollaborationCoordinator {

    private static final Logger log = LoggerFactory.getLogger(CollaborationCoordinator.class);

    private final AgentManager agentManager;

    /** 协作会话追踪 */
    private final ConcurrentHashMap<String, CollaborationSession> activeSessions = new ConcurrentHashMap<>();

    /** Agent 依赖图 */
    private final ConcurrentHashMap<String, Set<String>> dependencyGraph = new ConcurrentHashMap<>();

    /** 消息队列（用于异步协作） */
    private final ConcurrentHashMap<String, List<PendingMessage>> messageQueues = new ConcurrentHashMap<>();

    public CollaborationCoordinator(AgentManager agentManager) {
        this.agentManager = agentManager;
    }

    /**
     * 协作会话
     */
    public static class CollaborationSession {
        String sessionId;
        String projectId;
        String milestoneId;
        List<String> participantAgentIds;
        CollaborationStatus status;
        LocalDateTime startTime;
        LocalDateTime endTime;
        Map<String, Object> sharedContext;
        List<CollaborationStep> steps;

        public CollaborationSession(String sessionId, String projectId, String milestoneId) {
            this.sessionId = sessionId;
            this.projectId = projectId;
            this.milestoneId = milestoneId;
            this.participantAgentIds = new ArrayList<>();
            this.status = CollaborationStatus.INITIATED;
            this.startTime = LocalDateTime.now();
            this.sharedContext = new LinkedHashMap<>();
            this.steps = new ArrayList<>();
        }
    }

    /**
     * 协作步骤
     */
    public static class CollaborationStep {
        String stepId;
        String agentId;
        String action;
        StepStatus status;
        LocalDateTime startTime;
        LocalDateTime endTime;
        String result;
        Map<String, Object> metadata;

        public CollaborationStep(String stepId, String agentId, String action) {
            this.stepId = stepId;
            this.agentId = agentId;
            this.action = action;
            this.status = StepStatus.PENDING;
            this.metadata = new LinkedHashMap<>();
        }
    }

    /**
     * 待处理消息
     */
    static class PendingMessage {
        String fromAgentId;
        String toAgentId;
        AgentMessage.MessageType type;
        String content;
        LocalDateTime createdAt;
        int priority;
    }

    /**
     * 协作状态
     */
    public enum CollaborationStatus {
        INITIATED,      // 已发起
        IN_PROGRESS,    // 进行中
        WAITING_INPUT,  // 等待输入
        COMPLETED,      // 已完成
        FAILED,         // 失败
        CANCELLED       // 已取消
    }

    /**
     * 步骤状态
     */
    public enum StepStatus {
        PENDING,        // 待执行
        IN_PROGRESS,    // 执行中
        COMPLETED,      // 已完成
        FAILED,         // 失败
        SKIPPED         // 已跳过
    }

    /**
     * 发起协作会话
     *
     * @param projectId 项目 ID
     * @param milestoneId 里程碑 ID
     * @param participantRoles 参与角色列表
     * @return 协作会话
     */
    public CollaborationSession initiateCollaboration(String projectId, String milestoneId,
                                                        List<String> participantRoles) {
        String sessionId = "collab-" + projectId + "-" + milestoneId + "-" + System.currentTimeMillis();

        CollaborationSession session = new CollaborationSession(sessionId, projectId, milestoneId);

        // 查找参与的 Agent
        for (String role : participantRoles) {
            String agentId = projectId + ":" + role;
            Agent agent = agentManager.getAgent(agentId);
            if (agent != null && agent.isAlive()) {
                session.participantAgentIds.add(agentId);
            } else {
                log.warn("协作会话: Agent {} 不存在或不可用，跳过", agentId);
            }
        }

        if (session.participantAgentIds.isEmpty()) {
            log.error("协作会话: 没有可用的参与 Agent");
            return null;
        }

        activeSessions.put(sessionId, session);
        log.info("协作会话已发起: {}, 参与者: {}", sessionId, session.participantAgentIds);

        return session;
    }

    /**
     * 添加协作步骤
     */
    public void addCollaborationStep(String sessionId, String agentId, String action) {
        CollaborationSession session = activeSessions.get(sessionId);
        if (session == null) return;

        String stepId = sessionId + "-step-" + session.steps.size();
        CollaborationStep step = new CollaborationStep(stepId, agentId, action);
        session.steps.add(step);

        log.debug("协作步骤已添加: {} -> {}", sessionId, action);
    }

    /**
     * 执行协作步骤
     */
    public boolean executeStep(String sessionId, int stepIndex) {
        CollaborationSession session = activeSessions.get(sessionId);
        if (session == null || stepIndex >= session.steps.size()) {
            return false;
        }

        CollaborationStep step = session.steps.get(stepIndex);
        Agent agent = agentManager.getAgent(step.agentId);

        if (agent == null || !agent.isAlive()) {
            step.status = StepStatus.FAILED;
            step.result = "Agent 不可用";
            return false;
        }

        step.status = StepStatus.IN_PROGRESS;
        step.startTime = LocalDateTime.now();
        session.status = CollaborationStatus.IN_PROGRESS;

        // 发送协作消息给 Agent
        String message = buildCollaborationMessage(session, step);
        AgentMessage collabMsg = AgentMessage.builder()
            .fromAgentId("coordinator")
            .toAgentId(step.agentId)
            .type(AgentMessage.MessageType.TASK)
            .content(message)
            .build();

        agent.receiveMessage(collabMsg);

        log.info("协作步骤执行: {} -> {} - {}", sessionId, step.agentId, step.action);
        return true;
    }

    /**
     * 完成协作步骤
     */
    public void completeStep(String sessionId, int stepIndex, boolean success, String result) {
        CollaborationSession session = activeSessions.get(sessionId);
        if (session == null || stepIndex >= session.steps.size()) return;

        CollaborationStep step = session.steps.get(stepIndex);
        step.status = success ? StepStatus.COMPLETED : StepStatus.FAILED;
        step.endTime = LocalDateTime.now();
        step.result = result;

        // 检查是否所有步骤都完成
        boolean allCompleted = session.steps.stream()
            .allMatch(s -> s.status == StepStatus.COMPLETED || s.status == StepStatus.SKIPPED);

        if (allCompleted) {
            session.status = CollaborationStatus.COMPLETED;
            session.endTime = LocalDateTime.now();
            log.info("协作会话完成: {}", sessionId);

            // 通知所有参与者
            notifyParticipants(session, "协作任务已完成");
        }

        // 检查是否有失败步骤
        boolean hasFailed = session.steps.stream()
            .anyMatch(s -> s.status == StepStatus.FAILED);

        if (hasFailed && session.status != CollaborationStatus.COMPLETED) {
            session.status = CollaborationStatus.FAILED;
            log.warn("协作会话失败: {}", sessionId);
        }
    }

    /**
     * 构建协作消息
     */
    private String buildCollaborationMessage(CollaborationSession session, CollaborationStep step) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 协作任务\n\n");
        sb.append("**会话ID**: ").append(session.sessionId).append("\n");
        sb.append("**项目**: ").append(session.projectId).append("\n");
        sb.append("**任务**: ").append(step.action).append("\n\n");

        // 添加共享上下文
        if (!session.sharedContext.isEmpty()) {
            sb.append("### 共享上下文\n\n");
            session.sharedContext.forEach((key, value) ->
                sb.append("- **").append(key).append("**: ").append(value).append("\n"));
            sb.append("\n");
        }

        // 添加其他参与者的状态
        sb.append("### 团队状态\n\n");
        for (String agentId : session.participantAgentIds) {
            if (!agentId.equals(step.agentId)) {
                Agent other = agentManager.getAgent(agentId);
                if (other != null) {
                    sb.append("- ").append(other.getName()).append(" (").append(other.getRole()).append("): ")
                      .append(other.isBusy() ? "忙碌" : "空闲").append("\n");
                }
            }
        }

        return sb.toString();
    }

    /**
     * 通知所有参与者
     */
    private void notifyParticipants(CollaborationSession session, String message) {
        for (String agentId : session.participantAgentIds) {
            Agent agent = agentManager.getAgent(agentId);
            if (agent != null && agent.isAlive()) {
                AgentMessage notification = AgentMessage.builder()
                    .fromAgentId("coordinator")
                    .toAgentId(agentId)
                    .type(AgentMessage.MessageType.NOTIFY)
                    .content(message)
                    .build();
                agent.receiveMessage(notification);
            }
        }
    }

    /**
     * 更新共享上下文
     */
    public void updateSharedContext(String sessionId, String key, Object value) {
        CollaborationSession session = activeSessions.get(sessionId);
        if (session != null) {
            session.sharedContext.put(key, value);

            // 通知所有参与者上下文更新
            String updateMsg = String.format("共享上下文已更新: %s = %s", key, value);
            notifyParticipants(session, updateMsg);
        }
    }

    /**
     * 检测并解决依赖
     *
     * @param projectId 项目 ID
     * @param milestones 里程碑列表
     * @return 可以并行执行的里程碑组
     */
    public List<List<GoalMilestone>> detectParallelGroups(String projectId, List<GoalMilestone> milestones) {
        List<List<GoalMilestone>> groups = new ArrayList<>();
        Set<String> processed = new HashSet<>();

        for (GoalMilestone milestone : milestones) {
            if (processed.contains(milestone.getId())) continue;
            if (milestone.getStatus() == MilestoneStatus.COMPLETED) continue;

            // 找出所有可以并行执行的里程碑
            List<GoalMilestone> parallelGroup = new ArrayList<>();
            parallelGroup.add(milestone);
            processed.add(milestone.getId());

            // 检查其他里程碑是否可以并行
            for (GoalMilestone other : milestones) {
                if (processed.contains(other.getId())) continue;
                if (other.getStatus() == MilestoneStatus.COMPLETED) continue;

                // 检查是否有依赖关系
                if (!hasDependency(milestone, other) && !hasDependency(other, milestone)) {
                    // 检查是否使用不同的 Agent 角色
                    if (!milestone.getAssignedAgentRole().equals(other.getAssignedAgentRole())) {
                        parallelGroup.add(other);
                        processed.add(other.getId());
                    }
                }
            }

            groups.add(parallelGroup);
        }

        return groups;
    }

    /**
     * 检查两个里程碑是否有依赖关系
     */
    private boolean hasDependency(GoalMilestone from, GoalMilestone to) {
        if (to.getDependencies() == null) return false;
        return to.getDependencies().contains(from.getId());
    }

    /**
     * 获取协作会话
     */
    public CollaborationSession getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }

    /**
     * 获取项目的所有协作会话
     */
    public List<CollaborationSession> getProjectSessions(String projectId) {
        return activeSessions.values().stream()
            .filter(s -> projectId.equals(s.projectId))
            .collect(Collectors.toList());
    }

    /**
     * 获取协作统计
     */
    public Map<String, Object> getCollaborationStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        long totalSessions = activeSessions.size();
        long completedSessions = activeSessions.values().stream()
            .filter(s -> s.status == CollaborationStatus.COMPLETED)
            .count();
        long failedSessions = activeSessions.values().stream()
            .filter(s -> s.status == CollaborationStatus.FAILED)
            .count();

        stats.put("totalSessions", totalSessions);
        stats.put("completedSessions", completedSessions);
        stats.put("failedSessions", failedSessions);
        stats.put("successRate", totalSessions > 0 ? (double) completedSessions / totalSessions * 100 : 0);

        // 参与者统计
        Map<String, Integer> participantStats = new LinkedHashMap<>();
        activeSessions.values().forEach(s ->
            s.participantAgentIds.forEach(id ->
                participantStats.merge(id, 1, Integer::sum)));
        stats.put("participantCounts", participantStats);

        return stats;
    }

    /**
     * 清理过期会话
     */
    public void cleanupExpiredSessions(int maxAgeHours) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(maxAgeHours);

        activeSessions.entrySet().removeIf(entry -> {
            CollaborationSession session = entry.getValue();
            if (session.endTime != null && session.endTime.isBefore(cutoff)) {
                return true;
            }
            if (session.startTime.isBefore(cutoff) &&
                session.status != CollaborationStatus.IN_PROGRESS) {
                return true;
            }
            return false;
        });
    }
}
