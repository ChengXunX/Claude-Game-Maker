package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.agent.BaseAgent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.model.AgentDefinition;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.model.TaskAssignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

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

    @Autowired(required = false)
    private com.chengxun.gamemaker.web.service.AgentHealthService healthService;

    @Autowired(required = false)
    private GoalService goalService;

    @Autowired(required = false)
    private com.chengxun.gamemaker.manager.ProjectManager projectManager;

    /** Agent最后活动时间 */
    private final Map<String, Long> agentLastActivity = new ConcurrentHashMap<>();

    /**
     * 事件驱动的即时调度线程池
     * 当 Agent 收到消息时，通过此线程池立即触发工作，不再等待定时周期
     */
    private final ExecutorService immediateExecutor = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "agent-immediate-dispatch");
        t.setDaemon(true);
        return t;
    });

    /**
     * Agent 忙碌标记（防止同一 Agent 被重复调度）
     * key: agentId, value: 是否正在被立即调度
     */
    private final Map<String, AtomicBoolean> immediateScheduling = new ConcurrentHashMap<>();

    /** 启动唤醒是否已执行（防止重复执行） */
    private final AtomicBoolean startupWakeDone = new AtomicBoolean(false);

    /**
     * 启动后唤醒所有 ProducerAgent
     * 系统重启后 Agent 不会自动开始工作（调度器只在有待处理消息时才驱动），
     * 需要主动发送一条唤醒消息让 Producer 开始检查项目状态和分配任务。
     *
     * 延迟 20 秒执行，等待所有 Agent 初始化完成。
     */
    @jakarta.annotation.PostConstruct
    public void wakeAllProducersOnStartup() {
        // 用延迟线程执行，避免阻塞 Spring 初始化
        Thread startupThread = new Thread(() -> {
            try {
                // 等待 Agent 初始化完成
                Thread.sleep(20000);

                if (!startupWakeDone.compareAndSet(false, true)) return;

                List<String> projectIds = agentManager.getRegisteredProjectIds();
                int woken = 0;
                for (String projectId : projectIds) {
                    try {
                        Agent producer = agentManager.getAgent(projectId, "producer");
                        if (producer == null || !producer.isAlive()) continue;

                        // 发送系统唤醒消息
                        com.chengxun.gamemaker.model.AgentMessage wakeMsg =
                            com.chengxun.gamemaker.model.AgentMessage.builder()
                                .fromAgentId("system")
                                .toAgentId(projectId + ":producer")
                                .type(com.chengxun.gamemaker.model.AgentMessage.MessageType.SYSTEM)
                                .content("[系统重启] 系统已重启完成，请检查项目状态、恢复工作上下文，继续推进目标和里程碑。")
                                .priority(9)
                                .build();
                        producer.receiveMessage(wakeMsg);
                        woken++;
                        log.info("启动唤醒 Producer: {}", projectId);
                    } catch (Exception e) {
                        log.warn("启动唤醒 Producer 失败 ({}): {}", projectId, e.getMessage());
                    }
                }

                if (woken > 0) {
                    log.info("启动唤醒完成，已唤醒 {} 个 ProducerAgent", woken);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "startup-producer-wake");
        startupThread.setDaemon(true);
        startupThread.start();
    }

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

    // ===== 事件驱动调度 =====

    /**
     * 消息到达时触发的即时调度
     * 由 BaseAgent.receiveMessage() 通过回调触发
     * 避免 Agent 收到消息后等待 5 分钟才处理
     *
     * @param agent 收到消息的 Agent
     */
    public void onMessageArrived(Agent agent) {
        if (agent == null || !agent.isAlive()) return;

        String agentId = agent.getId();
        AtomicBoolean flag = immediateScheduling.computeIfAbsent(agentId, k -> new AtomicBoolean(false));

        // 防止重复调度：如果该 Agent 已经在立即调度队列中，跳过
        if (!flag.compareAndSet(false, true)) {
            log.debug("Agent {} already queued for immediate dispatch, skipping", agentId);
            return;
        }

        immediateExecutor.submit(() -> {
            try {
                // 短暂延迟（200ms），让消息批量到达后再一起处理
                // 避免连续多条消息触发多次 AI 调用
                Thread.sleep(200);

                log.info("Event-driven dispatch: agent {} processing pending messages", agentId);
                driveAgentWork(agent);
            } catch (Exception e) {
                log.error("Immediate dispatch failed for agent {}: {}", agentId, e.getMessage());
            } finally {
                flag.set(false);
            }
        });
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
     * 获取任务队列（真实的里程碑任务 + Agent 待处理消息）
     *
     * 数据来源：
     * 1. GoalService 的里程碑任务（项目目标分解后的具体任务）
     * 2. Agent 的待处理消息（其他 Agent 分配的协作任务）
     *
     * @return 任务队列列表
     */
    public List<Map<String, Object>> getTaskQueue() {
        List<Map<String, Object>> tasks = new java.util.ArrayList<>();

        // 1. 从 GoalService 获取所有项目的里程碑任务
        if (goalService != null) {
            for (String projectId : agentManager.getRegisteredProjectIds()) {
                try {
                    GameProject project = projectManager != null
                        ? projectManager.getProject(projectId) : null;
                    if (project == null) continue;

                    List<GameProject.GoalMilestone> milestones = goalService.getMilestones(projectId);
                    for (GameProject.GoalMilestone milestone : milestones) {
                        // 跳过已完成的里程碑
                        if (milestone.getStatus() == GameProject.MilestoneStatus.COMPLETED) continue;

                        // 里程碑本身作为一个任务条目
                        Map<String, Object> task = new java.util.HashMap<>();
                        task.put("taskId", milestone.getId());
                        task.put("agentId", projectId + ":" + milestone.getAssignedAgentRole());
                        task.put("agentName", milestone.getAssignedAgentRole());
                        task.put("title", milestone.getTitle());
                        task.put("description", milestone.getDescription());
                        task.put("priority", milestone.getOrder() <= 1 ? "HIGH" : "NORMAL");
                        task.put("status", mapMilestoneStatus(milestone.getStatus()));
                        task.put("type", "MILESTONE");
                        task.put("projectId", projectId);
                        task.put("projectName", project.getName());
                        task.put("createdAt", System.currentTimeMillis());
                        tasks.add(task);

                        // 里程碑下的子任务
                        if (milestone.getTasks() != null) {
                            for (GameProject.MilestoneTask mTask : milestone.getTasks()) {
                                if (mTask.getStatus() == GameProject.MilestoneStatus.COMPLETED) continue;

                                Map<String, Object> subTask = new java.util.HashMap<>();
                                subTask.put("taskId", mTask.getId());
                                subTask.put("agentId", projectId + ":" + milestone.getAssignedAgentRole());
                                subTask.put("agentName", milestone.getAssignedAgentRole());
                                subTask.put("title", mTask.getDescription());
                                subTask.put("priority", "NORMAL");
                                subTask.put("status", mapMilestoneStatus(mTask.getStatus()));
                                subTask.put("type", "TASK");
                                subTask.put("parentMilestone", milestone.getTitle());
                                subTask.put("projectId", projectId);
                                subTask.put("projectName", project.getName());
                                subTask.put("createdAt", System.currentTimeMillis());
                                tasks.add(subTask);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("Failed to load tasks for project {}: {}", projectId, e.getMessage());
                }
            }
        }

        // 2. 补充 Agent 的待处理消息作为任务条目
        for (Agent agent : agentManager.getAllAgents()) {
            int pendingCount = agent.getPendingMessages().size();
            if (pendingCount > 0 || agent.isBusy()) {
                Map<String, Object> task = new java.util.HashMap<>();
                task.put("taskId", agent.getId() + "_runtime");
                task.put("agentId", agent.getId());
                task.put("agentName", agent.getName());
                task.put("title", agent.isBusy() ? "AI 调用执行中" : pendingCount + " 条待处理消息");
                task.put("priority", "NORMAL");
                task.put("status", agent.isBusy() ? "RUNNING" : "PENDING");
                task.put("type", "AGENT_RUNTIME");
                task.put("pendingMessages", pendingCount);
                task.put("createdAt", agentLastActivity.getOrDefault(agent.getId(), System.currentTimeMillis()));
                tasks.add(task);
            }
        }

        return tasks;
    }

    /**
     * 里程碑状态映射为前端显示状态
     */
    private String mapMilestoneStatus(GameProject.MilestoneStatus status) {
        if (status == null) return "PENDING";
        return switch (status) {
            case PENDING -> "PENDING";
            case IN_PROGRESS -> "PROCESSING";
            case COMPLETED -> "COMPLETED";
            case BLOCKED -> "BLOCKED";
            default -> "PENDING";
        };
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

    /**
     * 取消任务
     * 支持多种任务类型：里程碑任务、子任务、Agent 运行时任务
     *
     * @param taskId 任务 ID
     * @return 是否取消成功
     */
    public boolean cancelTask(String taskId) {
        if (taskId == null || taskId.isEmpty()) {
            return false;
        }

        log.info("尝试取消任务: {}", taskId);

        // 1. 处理 Agent 运行时任务（格式：agentId_runtime）
        if (taskId.endsWith("_runtime")) {
            String agentId = taskId.replace("_runtime", "");
            return cancelAgentTask(agentId);
        }

        // 2. 处理 Agent 当前任务（格式：agentId_current）
        if (taskId.endsWith("_current")) {
            String agentId = taskId.replace("_current", "");
            return cancelAgentTask(agentId);
        }

        // 3. 处理里程碑任务和子任务
        if (goalService != null) {
            for (String projectId : agentManager.getRegisteredProjectIds()) {
                try {
                    // 查找里程碑
                    GameProject.GoalMilestone milestone = goalService.getMilestones(projectId).stream()
                        .filter(m -> taskId.equals(m.getId()))
                        .findFirst()
                        .orElse(null);

                    if (milestone != null) {
                        return cancelMilestone(projectId, milestone);
                    }

                    // 查找子任务
                    for (GameProject.GoalMilestone m : goalService.getMilestones(projectId)) {
                        GameProject.MilestoneTask task = m.getTasks().stream()
                            .filter(t -> taskId.equals(t.getId()))
                            .findFirst()
                            .orElse(null);

                        if (task != null) {
                            return cancelMilestoneTask(projectId, m, task);
                        }
                    }
                } catch (Exception e) {
                    log.debug("查找任务失败: projectId={}, error={}", projectId, e.getMessage());
                }
            }
        }

        log.warn("取消任务失败：任务不存在 {}", taskId);
        return false;
    }

    /**
     * 取消 Agent 当前任务
     */
    private boolean cancelAgentTask(String agentId) {
        Agent agent = agentManager.getAgent(agentId);
        if (agent == null) {
            log.warn("取消任务失败：Agent 不存在 {}", agentId);
            return false;
        }

        try {
            // 停止 Agent
            agent.stop();

            // 清除待处理消息
            if (agent instanceof com.chengxun.gamemaker.agent.BaseAgent baseAgent) {
                baseAgent.clearPendingMessages();
            }

            // 重新启动 Agent
            agent.start();

            log.info("已取消 Agent {} 的当前任务，已清除消息队列", agentId);
            return true;
        } catch (Exception e) {
            log.error("取消任务失败：{}", e.getMessage());
            return false;
        }
    }

    /**
     * 取消里程碑
     */
    private boolean cancelMilestone(String projectId, GameProject.GoalMilestone milestone) {
        try {
            milestone.setStatus(GameProject.MilestoneStatus.BLOCKED);
            milestone.setBlockedReason("手动取消");

            if (milestone.getTasks() != null) {
                for (GameProject.MilestoneTask task : milestone.getTasks()) {
                    if (task.getStatus() != GameProject.MilestoneStatus.COMPLETED) {
                        task.setStatus(GameProject.MilestoneStatus.PENDING);
                        task.setResult("任务已取消");
                    }
                }
            }

            if (projectManager != null) {
                GameProject project = projectManager.getProject(projectId);
                if (project != null) {
                    project.touch();
                    projectManager.saveProjectConfig(project);
                }
            }

            log.info("已取消里程碑: {} (项目: {})", milestone.getTitle(), projectId);
            return true;
        } catch (Exception e) {
            log.error("取消里程碑失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 取消子任务
     */
    private boolean cancelMilestoneTask(String projectId, GameProject.GoalMilestone milestone,
                                         GameProject.MilestoneTask task) {
        try {
            task.setStatus(GameProject.MilestoneStatus.PENDING);
            task.setResult("任务已取消");

            if (goalService != null) {
                goalService.recalculateMilestoneProgress(projectId, milestone.getId());
            }

            if (projectManager != null) {
                GameProject project = projectManager.getProject(projectId);
                if (project != null) {
                    project.touch();
                    projectManager.saveProjectConfig(project);
                }
            }

            log.info("已取消任务: {} (里程碑: {})", task.getDescription(), milestone.getTitle());
            return true;
        } catch (Exception e) {
            log.error("取消任务失败: {}", e.getMessage());
            return false;
        }
    }

    // ===== 目标驱动调度 =====

    /**
     * 制作人快速调度循环
     * 制作人需要比工人更频繁地运行（60秒 vs 5分钟），以便：
     * - 快速响应工人完成的任务
     * - 及时鞭策空闲/慢速的 Agent
     * - 实时监控项目进展和任务状态
     */
    @Scheduled(fixedRate = 60000, initialDelay = 30000)
    public void driveProducerFastLoop() {
        List<String> projectIds = agentManager.getRegisteredProjectIds();

        for (String projectId : projectIds) {
            try {
                Agent producer = agentManager.getAgent(projectId, "producer");
                if (producer == null || !producer.isAlive()) continue;

                // 制作人不忙时才驱动（避免打断正在进行的 AI 调用）
                if (!producer.isBusy()) {
                    boolean shouldDrive = false;

                    // 有待处理消息时驱动
                    int pendingCount = producer.getPendingMessages().size();
                    if (pendingCount > 0) {
                        log.debug("Producer has {} pending messages, driving work", pendingCount);
                        shouldDrive = true;
                    }

                    // 目标正在分解中时也需要驱动（分解超时后需要重试）
                    if (!shouldDrive && projectManager != null) {
                        GameProject project = projectManager.getProject(projectId);
                        if (project != null && project.getGoalStatus() == GameProject.GoalStatus.DECOMPOSING) {
                            long startedAt = project.getGoalDecomposeStartedAt();
                            if (startedAt == 0 || System.currentTimeMillis() - startedAt > 5 * 60 * 1000) {
                                log.debug("Producer goal decomposing with stale state, driving work");
                                shouldDrive = true;
                            }
                        }
                    }

                    // 始终驱动：Producer 需要定期检查里程碑推进、任务调度等
                    if (!shouldDrive) {
                        log.debug("Producer idle, periodic drive");
                        shouldDrive = true;
                    }

                    if (shouldDrive) {
                        driveAgentWork(producer);
                    }
                }
            } catch (Exception e) {
                log.error("Producer fast loop error for project {}: {}", projectId, e.getMessage());
            }
        }
    }

    /**
     * 唤醒制作人
     * 当工人 Agent 完成任务或发生重要事件时，通过事件驱动立即唤醒制作人
     * 让制作人能够实时响应，而不是等待下一个调度周期
     *
     * @param projectId 项目 ID
     * @param reason 唤醒原因
     */
    public void wakeProducer(String projectId, String reason) {
        if (projectId == null) return;

        Agent producer = agentManager.getAgent(projectId, "producer");
        if (producer == null || !producer.isAlive()) return;

        // 如果制作人正在忙碌，不打断
        if (producer.isBusy()) {
            log.debug("Producer is busy, skipping wake-up for: {}", reason);
            return;
        }

        // 通过立即调度线程池唤醒制作人
        immediateExecutor.submit(() -> {
            try {
                log.info("Waking up producer for project {}: {}", projectId, reason);
                driveAgentWork(producer);
            } catch (Exception e) {
                log.error("Producer wake-up failed for project {}: {}", projectId, e.getMessage());
            }
        });
    }

    /**
     * 定时驱动所有 Agent 工作
     * 每 5 分钟检查一次所有项目的 Agent，驱动目标进度和任务执行
     *
     * 执行顺序：先 Producer（决策和任务分发），再其他 Agent（执行任务）
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

        // 第一轮：驱动 ProducerAgent（决策和任务分发）
        // 注意：制作人已有独立的快速调度（60秒），这里仅处理积压消息
        for (String projectId : projectIds) {
            try {
                Agent producer = agentManager.getAgent(projectId, "producer");
                if (producer == null || !producer.isAlive()) continue;

                // 只有当制作人有积压消息时才在慢循环中驱动
                if (producer.getPendingMessages().size() > 0) {
                    driveAgentWork(producer);
                    processed++;
                }
            } catch (Exception e) {
                log.error("Error driving producer for project {}: {}", projectId, e.getMessage());
                if (rollbackService != null) {
                    String agentId = projectId + ":producer";
                    if (rollbackService.hasSnapshot(agentId)) {
                        log.info("Rolling back agent {} due to error", agentId);
                        rollbackService.rollback(agentId);
                    }
                }
            }
        }

        // 第二轮：驱动所有非生产者 Agent（执行任务、处理消息）
        for (Agent agent : agentManager.getAllAgents()) {
            if ("producer".equals(agent.getRole())) continue;
            if (!agent.isAlive()) continue;

            try {
                // 有待处理消息、忙碌、或有待执行任务时驱动
                int pendingCount = agent.getPendingMessages().size();
                boolean hasPendingTasks = agent instanceof com.chengxun.gamemaker.agent.BaseAgent ba
                    && ba.hasPendingTasks();
                if (pendingCount > 0 || agent.isBusy() || hasPendingTasks) {
                    driveAgentWork(agent);
                    processed++;
                }
            } catch (Exception e) {
                log.error("Error driving agent {}: {}", agent.getId(), e.getMessage());
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        if (duration > 5000 || projectIds.size() > 20) {
            log.warn("Goal iteration completed: {} agents processed in {}ms (total projects: {})",
                processed, duration, projectIds.size());
        }
    }

    /**
     * 驱动单个 Agent 执行工作，并记录健康指标
     *
     * @param agent 目标 Agent
     */
    private void driveAgentWork(Agent agent) {
        long workStart = System.currentTimeMillis();
        boolean success = true;
        String errorMessage = null;

        try {
            agent.work();
        } catch (Exception e) {
            success = false;
            errorMessage = e.getMessage();
            throw e;
        } finally {
            long responseTime = System.currentTimeMillis() - workStart;
            agentLastActivity.put(agent.getId(), System.currentTimeMillis());

            // 记录健康指标
            if (healthService != null) {
                healthService.recordRequest(agent.getId(), responseTime, success, errorMessage);
            }
        }
    }
}
