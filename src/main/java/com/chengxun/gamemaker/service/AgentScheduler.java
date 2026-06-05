package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.agent.BaseAgent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.model.AgentDefinition;
import com.chengxun.gamemaker.model.TaskAssignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent调度服务
 * 负责Agent的调度、任务分配和上下文管理
 *
 * 主要功能：
 * - 智能任务分配
 * - 自动上下文压缩
 * - 上下文恢复
 * - Agent生命周期管理
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class AgentScheduler {

    private static final Logger log = LoggerFactory.getLogger(AgentScheduler.class);

    @Autowired
    private AgentManager agentManager;

    @Autowired
    private ContextCompactor contextCompactor;

    @Autowired(required = false)
    private com.chengxun.gamemaker.web.service.BusinessMetricsService metricsService;

    @Autowired(required = false)
    private AgentRollbackService rollbackService;

    /** Agent最后活动时间 */
    private final Map<String, Long> agentLastActivity = new ConcurrentHashMap<>();

    /**
     * 分配任务给Agent
     *
     * @param agentId Agent ID
     * @param task 任务
     */
    public void assignTask(String agentId, TaskAssignment task) {
        Agent agent = agentManager.getAgent(agentId);
        if (agent == null) {
            log.error("Agent not found: {}", agentId);
            return;
        }

        // 检查资源配额
        if (!agentManager.canAcceptTask(agentId)) {
            log.warn("Agent {} resource quota exceeded, task rejected", agentId);
            return;
        }

        // 创建执行前快照（用于失败回滚）
        if (rollbackService != null) {
            rollbackService.createSnapshot(agentId, "before_task_" + task.getId());
        }

        // 记录任务开始
        agentManager.startAgentTask(agentId);

        // 更新最后活动时间
        agentLastActivity.put(agentId, System.currentTimeMillis());

        // 检查是否需要压缩上下文
        if (agent instanceof BaseAgent baseAgent) {
            baseAgent.autoCompactIfNeeded();
        }

        // 分配任务
        agent.assignTask(task);
        if (metricsService != null) metricsService.recordAgentCall();
        log.info("Task assigned to agent: {} - {}", agentId, task.getTitle());
    }

    /**
     * 发送消息给Agent
     *
     * @param agentId Agent ID
     * @param message 消息内容
     * @return Agent的回复
     */
    public String sendMessage(String agentId, String message) {
        if (metricsService != null) metricsService.recordAgentCall();
        Agent agent = agentManager.getAgent(agentId);
        if (agent == null) {
            log.error("Agent not found: {}", agentId);
            return "Agent不存在: " + agentId;
        }

        // 更新最后活动时间
        agentLastActivity.put(agentId, System.currentTimeMillis());

        // 检查是否需要压缩上下文
        if (agent instanceof BaseAgent baseAgent) {
            baseAgent.autoCompactIfNeeded();
        }

        // 发送消息
        return agent.sendMessage(message);
    }

    /**
     * 压缩Agent的上下文
     *
     * @param agentId Agent ID
     * @return 压缩后的摘要
     */
    public String compactAgentContext(String agentId) {
        Agent agent = agentManager.getAgent(agentId);
        if (agent == null) {
            return "Agent不存在: " + agentId;
        }

        if (agent instanceof BaseAgent baseAgent) {
            return baseAgent.compactContext();
        }

        return "Agent不支持上下文压缩";
    }

    /**
     * 恢复Agent的上下文
     *
     * @param agentId Agent ID
     * @return 恢复的上下文提示
     */
    public String recoverAgentContext(String agentId) {
        Agent agent = agentManager.getAgent(agentId);
        if (agent == null) {
            return "Agent不存在: " + agentId;
        }

        if (agent instanceof BaseAgent baseAgent) {
            // 调用ContextCompactor进行恢复
            return contextCompactor.recoverContext(agentId, baseAgent.getCurrentProject());
        }

        return "Agent不支持上下文恢复";
    }

    /**
     * 获取Agent的压缩历史
     *
     * @param agentId Agent ID
     * @return 压缩记录列表
     */
    public List<Map<String, Object>> getAgentCompactHistory(String agentId) {
        Agent agent = agentManager.getAgent(agentId);
        if (agent == null) {
            return List.of();
        }

        if (agent instanceof BaseAgent baseAgent) {
            return baseAgent.getCompactHistory();
        }

        return List.of();
    }

    /**
     * 启动Agent
     *
     * @param agentId Agent ID
     */
    public void startAgent(String agentId) {
        Agent agent = agentManager.getAgent(agentId);
        if (agent == null) {
            log.error("Agent not found: {}", agentId);
            return;
        }

        agent.start();
        agentLastActivity.put(agentId, System.currentTimeMillis());
        log.info("Agent started: {}", agentId);
    }

    /**
     * 停止Agent
     *
     * @param agentId Agent ID
     */
    public void stopAgent(String agentId) {
        Agent agent = agentManager.getAgent(agentId);
        if (agent == null) {
            log.error("Agent not found: {}", agentId);
            return;
        }

        // 停止前压缩上下文
        if (agent instanceof BaseAgent baseAgent) {
            baseAgent.compactContext();
        }

        agent.stop();
        agentLastActivity.remove(agentId);
        log.info("Agent stopped: {}", agentId);
    }

    /**
     * 定期检查并压缩长时间运行的Agent上下文
     * 每30分钟执行一次
     */
    @Scheduled(fixedRate = 1800000) // 30分钟
    public void autoCompactLongRunningAgents() {
        log.debug("Checking agents for auto-compaction...");

        List<Agent> allAgents = agentManager.getAllAgents();
        for (Agent agent : allAgents) {
            if (agent instanceof BaseAgent baseAgent && agent.isAlive()) {
                try {
                    baseAgent.autoCompactIfNeeded();
                } catch (Exception e) {
                    log.error("Failed to auto-compact context for agent: {}", agent.getId(), e);
                }
            }
        }
    }

    /**
     * 获取Agent状态信息
     *
     * @param agentId Agent ID
     * @return 状态信息
     */
    public Map<String, Object> getAgentStatus(String agentId) {
        Agent agent = agentManager.getAgent(agentId);
        if (agent == null) {
            return Map.of("status", "NOT_FOUND");
        }

        Map<String, Object> status = new java.util.HashMap<>();
        status.put("id", agent.getId());
        status.put("name", agent.getName());
        status.put("role", agent.getRole());
        status.put("alive", agent.isAlive());
        status.put("busy", agent.isBusy());

        // 上下文信息
        if (agent instanceof BaseAgent baseAgent) {
            status.put("shouldCompact", baseAgent.shouldCompactContext());
            status.put("compactHistoryCount", baseAgent.getCompactHistory().size());
        }

        // 最后活动时间
        Long lastActivity = agentLastActivity.get(agentId);
        if (lastActivity != null) {
            status.put("lastActivity", lastActivity);
            status.put("idleMinutes", (System.currentTimeMillis() - lastActivity) / 60000);
        }

        return status;
    }

    /**
     * 获取所有Agent的状态
     *
     * @return Agent状态列表
     */
    public List<Map<String, Object>> getAllAgentStatus() {
        return agentManager.getAllAgents().stream()
            .map(agent -> getAgentStatus(agent.getId()))
            .toList();
    }

    // ===== 调度配置和任务队列 =====

    /** 调度配置 */
    private final Map<String, Object> schedulerConfig = new ConcurrentHashMap<>();

    {
        schedulerConfig.put("autoCompactEnabled", true);
        schedulerConfig.put("autoCompactIntervalMinutes", 30);
        schedulerConfig.put("goalIterationEnabled", true);
        schedulerConfig.put("goalIterationIntervalMinutes", 5);
        schedulerConfig.put("maxConcurrentTasks", 10);
        schedulerConfig.put("taskTimeoutMinutes", 60);
    }

    /**
     * 获取任务队列（当前所有Agent的任务状态）
     *
     * @return 任务队列列表
     */
    public List<Map<String, Object>> getTaskQueue() {
        List<Map<String, Object>> tasks = new java.util.ArrayList<>();
        for (Agent agent : agentManager.getAllAgents()) {
            Map<String, Object> task = new java.util.HashMap<>();
            task.put("taskId", agent.getId() + "_current");
            task.put("agentId", agent.getId());
            task.put("title", agent.isBusy() ? "执行中" : "空闲");
            task.put("priority", "NORMAL");
            task.put("status", agent.isBusy() ? "RUNNING" : "IDLE");
            task.put("createdAt", agentLastActivity.getOrDefault(agent.getId(), System.currentTimeMillis()));
            tasks.add(task);
        }
        return tasks;
    }

    /**
     * 获取调度配置
     *
     * @return 配置信息
     */
    public Map<String, Object> getSchedulerConfig() {
        Map<String, Object> result = new java.util.HashMap<>(schedulerConfig);
        result.put("registeredProjects", agentManager.getRegisteredProjectIds().size());
        result.put("totalAgents", agentManager.getAllAgents().size());
        // 兼容前端字段名
        result.put("intervalSeconds", (int) schedulerConfig.getOrDefault("goalIterationIntervalMinutes", 5) * 60);
        return result;
    }

    /**
     * 更新调度配置
     *
     * @param config 新配置
     */
    public void updateSchedulerConfig(Map<String, Object> config) {
        schedulerConfig.putAll(config);
        log.info("Scheduler config updated: {}", config);
    }

    /**
     * 手动触发调度
     */
    public void triggerSchedule() {
        log.info("Manual schedule trigger requested");
        driveGoalIterations();
    }

    // ===== 目标驱动调度 =====

    /**
     * 定时驱动项目目标迭代
     * 每 5 分钟检查一次所有项目的 ProducerAgent，驱动目标进度
     */
    @Scheduled(fixedRate = 300000, initialDelay = 60000)
    public void driveGoalIterations() {
        long startTime = System.currentTimeMillis();
        List<String> projectIds = agentManager.getRegisteredProjectIds();
        int processed = 0;

        // 更新活跃 Agent 指标
        if (metricsService != null) {
            int aliveCount = (int) agentManager.getAllAgents().stream().filter(Agent::isAlive).count();
            metricsService.setActiveAgentCount(aliveCount);
        }

        for (String projectId : projectIds) {
            try {
                Agent producer = agentManager.getAgent(projectId, "producer");
                if (producer == null || !producer.isAlive()) continue;

                producer.work();
                processed++;
            } catch (Exception e) {
                log.error("Error driving goal iteration for project {}: {}", projectId, e.getMessage());
                // 任务失败时尝试回滚
                if (rollbackService != null) {
                    String agentId = projectId + ":producer";
                    if (rollbackService.hasSnapshot(agentId)) {
                        log.info("Rolling back agent {} due to error", agentId);
                        rollbackService.rollback(agentId);
                    }
                }
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        if (duration > 5000 || projectIds.size() > 20) {
            log.warn("Goal iteration completed: {} projects processed in {}ms (total: {})",
                processed, duration, projectIds.size());
        }
    }
}
