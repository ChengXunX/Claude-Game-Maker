package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.model.GameProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务重平衡服务
 * 根据Agent实时负载动态调整任务分配，避免忙的忙死、闲的闲死
 *
 * 重平衡策略：
 * 1. 超载检测：Agent任务数>3且忙碌时长>30分钟 → 触发重平衡
 * 2. 空闲检测：Agent无任务>15分钟 → 主动分配待处理任务
 * 3. 阻塞迁移：任务阻塞>20分钟 → 尝试迁移给其他可用Agent
 * 4. 冲突检测：多个Agent操作同一资源 → 发出冲突警告
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class TaskRebalanceService {

    private static final Logger log = LoggerFactory.getLogger(TaskRebalanceService.class);

    /** Agent负载记录：agentId -> LoadInfo */
    private final Map<String, AgentLoadInfo> loadMap = new ConcurrentHashMap<>();

    /** 阻塞任务记录：taskId -> 阻塞开始时间 */
    private final Map<String, LocalDateTime> blockedTasks = new ConcurrentHashMap<>();

    /** 资源锁定表：resourceKey -> 锁定的AgentId */
    private final Map<String, String> resourceLocks = new ConcurrentHashMap<>();

    /** 超载阈值：任务数 */
    private static final int OVERLOAD_TASK_THRESHOLD = 3;

    /** 超载阈值：忙碌时长（分钟） */
    private static final long OVERLOAD_BUSY_MINUTES = 30;

    /** 空闲阈值（分钟） */
    private static final long IDLE_THRESHOLD_MINUTES = 15;

    /** 阻塞阈值（分钟） */
    private static final long BLOCKED_THRESHOLD_MINUTES = 20;

    /**
     * Agent负载信息
     */
    public static class AgentLoadInfo {
        private final String agentId;
        private final String agentRole;
        private int taskCount;
        private long busyMinutes;
        private String status; // IDLE, NORMAL, OVERLOADED, BLOCKED
        private LocalDateTime lastActiveTime;
        private String currentTaskTitle;

        public AgentLoadInfo(String agentId, String agentRole) {
            this.agentId = agentId;
            this.agentRole = agentRole;
            this.status = "IDLE";
            this.lastActiveTime = LocalDateTime.now();
        }

        public String getAgentId() { return agentId; }
        public String getAgentRole() { return agentRole; }
        public int getTaskCount() { return taskCount; }
        public void setTaskCount(int count) { this.taskCount = count; }
        public long getBusyMinutes() { return busyMinutes; }
        public void setBusyMinutes(long minutes) { this.busyMinutes = minutes; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getLastActiveTime() { return lastActiveTime; }
        public void setLastActiveTime(LocalDateTime time) { this.lastActiveTime = time; }
        public String getCurrentTaskTitle() { return currentTaskTitle; }
        public void setCurrentTaskTitle(String title) { this.currentTaskTitle = title; }
    }

    /**
     * 重平衡建议
     */
    public static class RebalanceAction {
        public enum Type { MIGRATE_TASK, SPLIT_TASK, REASSIGN_IDLE, RESOLVE_CONFLICT }

        private final Type type;
        private final String fromAgentId;
        private final String toAgentId;
        private final String taskDescription;
        private final String reason;

        public RebalanceAction(Type type, String fromAgentId, String toAgentId, String taskDescription, String reason) {
            this.type = type;
            this.fromAgentId = fromAgentId;
            this.toAgentId = toAgentId;
            this.taskDescription = taskDescription;
            this.reason = reason;
        }

        public Type getType() { return type; }
        public String getFromAgentId() { return fromAgentId; }
        public String getToAgentId() { return toAgentId; }
        public String getTaskDescription() { return taskDescription; }
        public String getReason() { return reason; }

        public String toReadableText() {
            return switch (type) {
                case MIGRATE_TASK -> String.format("迁移任务: %s → %s（原因: %s）", fromAgentId, toAgentId, reason);
                case SPLIT_TASK -> String.format("拆分任务: %s 的任务过大，建议拆分（原因: %s）", fromAgentId, reason);
                case REASSIGN_IDLE -> String.format("分配空闲: %s 空闲，建议分配新任务", toAgentId);
                case RESOLVE_CONFLICT -> String.format("资源冲突: %s 和 %s 可能操作同一资源（%s）", fromAgentId, toAgentId, reason);
            };
        }
    }

    /**
     * 更新Agent负载信息
     *
     * @param agent Agent实例
     * @param taskCount 当前任务数
     */
    public void updateAgentLoad(Agent agent, int taskCount) {
        String agentId = agent.getId();
        AgentLoadInfo loadInfo = loadMap.computeIfAbsent(agentId,
            k -> new AgentLoadInfo(agentId, agent.getRole()));

        loadInfo.setTaskCount(taskCount);
        loadInfo.setLastActiveTime(LocalDateTime.now());

        if (agent.isBusy()) {
            loadInfo.setBusyMinutes(loadInfo.getBusyMinutes() + 1);
        } else {
            loadInfo.setBusyMinutes(0);
        }

        // 判断状态
        if (taskCount == 0 && loadInfo.getBusyMinutes() == 0) {
            loadInfo.setStatus("IDLE");
        } else if (taskCount >= OVERLOAD_TASK_THRESHOLD && loadInfo.getBusyMinutes() >= OVERLOAD_BUSY_MINUTES) {
            loadInfo.setStatus("OVERLOADED");
        } else if (taskCount > 0) {
            loadInfo.setStatus("NORMAL");
        }
    }

    /**
     * 标记任务阻塞
     *
     * @param taskId 任务ID
     */
    public void markTaskBlocked(String taskId) {
        blockedTasks.putIfAbsent(taskId, LocalDateTime.now());
    }

    /**
     * 取消任务阻塞标记
     *
     * @param taskId 任务ID
     */
    public void markTaskUnblocked(String taskId) {
        blockedTasks.remove(taskId);
    }

    /**
     * 锁定资源（防止冲突）
     *
     * @param resourceKey 资源标识（如文件路径）
     * @param agentId 锁定的Agent
     * @return true如果锁定成功，false如果已被其他Agent锁定
     */
    public boolean lockResource(String resourceKey, String agentId) {
        String existing = resourceLocks.putIfAbsent(resourceKey, agentId);
        if (existing == null) return true;
        return existing.equals(agentId); // 自己重复锁定算成功
    }

    /**
     * 释放资源锁
     *
     * @param resourceKey 资源标识
     * @param agentId 释放的Agent
     */
    public void unlockResource(String resourceKey, String agentId) {
        resourceLocks.remove(resourceKey, agentId);
    }

    /**
     * 执行重平衡检查
     * 返回需要执行的重平衡操作列表
     *
     * @param agents 项目内所有Agent
     * @return 重平衡建议列表
     */
    public List<RebalanceAction> checkRebalance(List<Agent> agents) {
        List<RebalanceAction> actions = new ArrayList<>();

        // 更新所有Agent的负载
        for (Agent agent : agents) {
            AgentLoadInfo loadInfo = loadMap.get(agent.getId());
            if (loadInfo == null) continue;

            // 检查超载
            if ("OVERLOADED".equals(loadInfo.getStatus())) {
                // 找一个空闲的Agent来接收任务
                Agent idleAgent = agents.stream()
                    .filter(a -> {
                        AgentLoadInfo li = loadMap.get(a.getId());
                        return li != null && "IDLE".equals(li.getStatus()) && !a.getId().equals(agent.getId());
                    })
                    .findFirst().orElse(null);

                if (idleAgent != null) {
                    actions.add(new RebalanceAction(
                        RebalanceAction.Type.MIGRATE_TASK,
                        agent.getId(),
                        idleAgent.getId(),
                        loadInfo.getCurrentTaskTitle() != null ? loadInfo.getCurrentTaskTitle() : "待迁移任务",
                        String.format("%s 超载（%d个任务，忙碌%d分钟），%s 空闲",
                            agent.getName(), loadInfo.getTaskCount(), loadInfo.getBusyMinutes(), idleAgent.getName())
                    ));
                } else {
                    actions.add(new RebalanceAction(
                        RebalanceAction.Type.SPLIT_TASK,
                        agent.getId(),
                        null,
                        loadInfo.getCurrentTaskTitle() != null ? loadInfo.getCurrentTaskTitle() : "待拆分任务",
                        String.format("%s 超载且无空闲Agent，建议拆分任务", agent.getName())
                    ));
                }
            }

            // 检查空闲
            if ("IDLE".equals(loadInfo.getStatus())) {
                long idleMinutes = java.time.Duration.between(loadInfo.getLastActiveTime(), LocalDateTime.now()).toMinutes();
                if (idleMinutes >= IDLE_THRESHOLD_MINUTES) {
                    actions.add(new RebalanceAction(
                        RebalanceAction.Type.REASSIGN_IDLE,
                        null,
                        agent.getId(),
                        null,
                        String.format("%s 已空闲 %d 分钟，建议分配新任务", agent.getName(), idleMinutes)
                    ));
                }
            }
        }

        // 检查阻塞超时
        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<String, LocalDateTime> entry : blockedTasks.entrySet()) {
            long blockedMinutes = java.time.Duration.between(entry.getValue(), now).toMinutes();
            if (blockedMinutes >= BLOCKED_THRESHOLD_MINUTES) {
                actions.add(new RebalanceAction(
                    RebalanceAction.Type.MIGRATE_TASK,
                    "blocked",
                    null,
                    "任务 " + entry.getKey(),
                    String.format("任务已阻塞 %d 分钟，建议重新分配或拆分", blockedMinutes)
                ));
            }
        }

        // 检查资源冲突
        Map<String, List<String>> resourceToAgents = new HashMap<>();
        resourceLocks.forEach((resource, agentId) -> {
            resourceToAgents.computeIfAbsent(resource, k -> new ArrayList<>()).add(agentId);
        });
        for (Map.Entry<String, List<String>> entry : resourceToAgents.entrySet()) {
            if (entry.getValue().size() > 1) {
                actions.add(new RebalanceAction(
                    RebalanceAction.Type.RESOLVE_CONFLICT,
                    entry.getValue().get(0),
                    entry.getValue().get(1),
                    entry.getKey(),
                    "多个Agent同时锁定同一资源"
                ));
            }
        }

        return actions;
    }

    /**
     * 获取所有Agent负载信息（供前端展示）
     *
     * @return 负载信息列表
     */
    public List<AgentLoadInfo> getAllLoadInfo() {
        return new ArrayList<>(loadMap.values());
    }

    /**
     * 获取负载摘要文本
     */
    public String getLoadSummary(List<Agent> agents) {
        StringBuilder sb = new StringBuilder();
        sb.append("### Agent 负载状态\n\n");

        for (Agent agent : agents) {
            AgentLoadInfo loadInfo = loadMap.get(agent.getId());
            if (loadInfo == null) continue;

            String statusIcon = switch (loadInfo.getStatus()) {
                case "IDLE" -> "空闲";
                case "NORMAL" -> "正常";
                case "OVERLOADED" -> "超载";
                case "BLOCKED" -> "阻塞";
                default -> "未知";
            };

            sb.append(String.format("- %s (%s): %s | 任务数: %d | 忙碌: %d分钟\n",
                agent.getName(), agent.getRole(), statusIcon,
                loadInfo.getTaskCount(), loadInfo.getBusyMinutes()));
        }

        // 检查并添加重平衡建议
        List<RebalanceAction> actions = checkRebalance(agents);
        if (!actions.isEmpty()) {
            sb.append("\n### 重平衡建议\n\n");
            for (RebalanceAction action : actions) {
                sb.append("- ").append(action.toReadableText()).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * 清理过期数据
     */
    public void cleanup() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(2);
        blockedTasks.entrySet().removeIf(e -> e.getValue().isBefore(threshold));
        loadMap.entrySet().removeIf(e -> e.getValue().getLastActiveTime().isBefore(threshold));
    }
}
