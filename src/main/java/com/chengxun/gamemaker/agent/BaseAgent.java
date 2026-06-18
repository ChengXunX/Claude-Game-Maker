package com.chengxun.gamemaker.agent;

import com.chengxun.gamemaker.engine.ClaudeCliEngine;
import com.chengxun.gamemaker.engine.MessageBus;
import com.chengxun.gamemaker.manager.ContextManager;
import com.chengxun.gamemaker.manager.MemoryManager;
import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.manager.SkillManager;
import com.chengxun.gamemaker.model.*;
import com.chengxun.gamemaker.service.*;
import com.chengxun.gamemaker.web.entity.AgentCapability;
import com.chengxun.gamemaker.web.service.AgentLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public abstract class BaseAgent implements Agent {

    protected static final Logger log = LoggerFactory.getLogger(BaseAgent.class);

    protected final AgentDefinition definition;
    protected final ClaudeCliEngine cliEngine;
    protected final MessageBus messageBus;
    protected final ContextManager contextManager;
    protected final MemoryManager memoryManager;
    protected final SkillManager skillManager;
    protected final ProjectManager projectManager;

    /** 上下文压缩服务 - 通过setter注入 */
    protected ContextCompactor contextCompactor;

    /** 能力注册服务 - 通过setter注入 */
    protected CapabilityRegistry capabilityRegistry;

    /** 能力输出解析器 - 通过setter注入 */
    protected CapabilityOutputParser capabilityOutputParser;

    /** 能力执行引擎 - 通过setter注入 */
    protected CapabilityExecutionEngine capabilityExecutionEngine;

    /** 能力拦截器 - 通过setter注入 */
    protected CapabilityInterceptor capabilityInterceptor;

    /** 上下文监控服务 - 通过setter注入 */
    protected ContextMonitor contextMonitor;

    /** MCP 服务 - 通过setter注入 */
    protected com.chengxun.gamemaker.service.McpService mcpService;

    /** Agent 日志服务 - 通过setter注入 */
    protected AgentLogService agentLogService;

    /** 知识进化服务 - 通过setter注入，连接知识库自学习与 Agent 自进化 */
    protected KnowledgeEvolutionService knowledgeEvolutionService;

    /** 通知服务 - 通过setter注入，用于发送告警通知 */
    protected com.chengxun.gamemaker.web.service.NotificationService notificationService;

    /** 告警服务 - 通过setter注入，用于创建系统告警 */
    protected com.chengxun.gamemaker.web.service.AlertService alertService;

    /** Token 服务 - 通过setter注入，用于记录 Token 消耗 */
    protected com.chengxun.gamemaker.web.service.ApiTokenService apiTokenService;

    /** Agent 管理器 - 通过setter注入，用于 Token 自动分配 */
    protected com.chengxun.gamemaker.manager.AgentManager agentManagerRef;

    /** 干预服务 - 通过setter注入，用于处理人工干预消息 */
    protected com.chengxun.gamemaker.service.AgentInterventionService interventionService;

    /** 项目看板 - 通过setter注入，用于获取团队协作上下文 */
    protected com.chengxun.gamemaker.service.ProjectBoard projectBoard;

    /** 事件总线 - 通过setter注入，用于发布和订阅事件 */
    protected com.chengxun.gamemaker.service.EventBus eventBus;

    /** Token 预算控制 - 通过setter注入 */
    protected com.chengxun.gamemaker.service.TokenBudgetService tokenBudgetService;

    /** Prompt 缓存服务 - 通过setter注入 */
    protected com.chengxun.gamemaker.service.PromptCacheService promptCacheService;

    /** 消息去重合并器 - 通过setter注入 */
    protected com.chengxun.gamemaker.service.MessageDeduplicator messageDeduplicator;

    /** 系统配置服务 - 通过setter注入，用于读取运行时配置 */
    protected com.chengxun.gamemaker.web.service.SystemConfigService configService;

    /** 协作增强服务 - 通过setter注入 */
    protected com.chengxun.gamemaker.service.CollaborationEnhancer collaborationEnhancer;

    /** 会话累计 Token 数（用于触发自动压缩） */
    protected volatile long sessionTokenCount = 0;

    /**
     * 消息到达回调
     * 当收到新消息时触发，用于事件驱动的即时任务处理
     * 由 AgentManager 在创建 Agent 时设置
     */
    private volatile Runnable messageArrivedCallback;

    protected AgentContext agentContext;
    protected GameProject currentProject;
    protected String currentConversationId;

    protected final ConcurrentLinkedQueue<AgentMessage> pendingMessages = new ConcurrentLinkedQueue<>();
    protected final List<TaskAssignment> tasks = new CopyOnWriteArrayList<>();

    protected volatile boolean busy = false;
    protected volatile boolean running = false;

    /** OpenAI 兼容引擎（可选注入，用于 MiniMax 等非 Anthropic API） */
    protected com.chengxun.gamemaker.engine.OpenAiCompatibleEngine openAiEngine;

    protected BaseAgent(AgentDefinition definition,
                       ClaudeCliEngine cliEngine,
                       MessageBus messageBus,
                       ContextManager contextManager,
                       MemoryManager memoryManager,
                       SkillManager skillManager,
                       ProjectManager projectManager) {
        this.definition = definition;
        this.cliEngine = cliEngine;
        this.messageBus = messageBus;
        this.contextManager = contextManager;
        this.memoryManager = memoryManager;
        this.skillManager = skillManager;
        this.projectManager = projectManager;
    }

    /** 设置 OpenAI 兼容引擎 */
    public void setOpenAiEngine(com.chengxun.gamemaker.engine.OpenAiCompatibleEngine openAiEngine) {
        this.openAiEngine = openAiEngine;
    }

    /**
     * 设置上下文压缩服务
     * @param contextCompactor 上下文压缩服务
     */
    public void setContextCompactor(ContextCompactor contextCompactor) {
        this.contextCompactor = contextCompactor;
    }

    /** 设置能力注册服务 */
    public void setCapabilityRegistry(CapabilityRegistry capabilityRegistry) {
        this.capabilityRegistry = capabilityRegistry;
    }

    /** 设置能力输出解析器 */
    public void setCapabilityOutputParser(CapabilityOutputParser capabilityOutputParser) {
        this.capabilityOutputParser = capabilityOutputParser;
    }

    /** 设置能力执行引擎 */
    public void setCapabilityExecutionEngine(CapabilityExecutionEngine capabilityExecutionEngine) {
        this.capabilityExecutionEngine = capabilityExecutionEngine;
    }

    /** 设置能力拦截器 */
    public void setCapabilityInterceptor(CapabilityInterceptor capabilityInterceptor) {
        this.capabilityInterceptor = capabilityInterceptor;
    }

    /** 设置上下文监控服务 */
    public void setContextMonitor(ContextMonitor contextMonitor) {
        this.contextMonitor = contextMonitor;
    }

    /** 设置 MCP 服务 */
    public void setMcpService(com.chengxun.gamemaker.service.McpService mcpService) {
        this.mcpService = mcpService;
    }

    /** 设置 Agent 日志服务 */
    public void setAgentLogService(AgentLogService agentLogService) {
        this.agentLogService = agentLogService;
    }

    /** 设置知识进化服务 */
    public void setKnowledgeEvolutionService(KnowledgeEvolutionService knowledgeEvolutionService) {
        this.knowledgeEvolutionService = knowledgeEvolutionService;
    }

    /** 设置通知服务 */
    public void setNotificationService(com.chengxun.gamemaker.web.service.NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** 设置告警服务 */
    public void setAlertService(com.chengxun.gamemaker.web.service.AlertService alertService) {
        this.alertService = alertService;
    }

    /** 设置 Token 服务 */
    public void setApiTokenService(com.chengxun.gamemaker.web.service.ApiTokenService apiTokenService) {
        this.apiTokenService = apiTokenService;
    }

    /** 设置 Agent 管理器引用 */
    public void setAgentManagerRef(com.chengxun.gamemaker.manager.AgentManager agentManagerRef) {
        this.agentManagerRef = agentManagerRef;
    }

    public void setInterventionService(com.chengxun.gamemaker.service.AgentInterventionService interventionService) {
        this.interventionService = interventionService;
    }

    public void setProjectBoard(com.chengxun.gamemaker.service.ProjectBoard projectBoard) {
        this.projectBoard = projectBoard;
    }

    public void setEventBus(com.chengxun.gamemaker.service.EventBus eventBus) {
        this.eventBus = eventBus;
    }

    /** 设置 Token 预算控制 */
    public void setTokenBudgetService(com.chengxun.gamemaker.service.TokenBudgetService tokenBudgetService) {
        this.tokenBudgetService = tokenBudgetService;
    }

    /** 设置 Prompt 缓存服务 */
    public void setPromptCacheService(com.chengxun.gamemaker.service.PromptCacheService promptCacheService) {
        this.promptCacheService = promptCacheService;
    }

    /** 设置消息去重合并器 */
    public void setMessageDeduplicator(com.chengxun.gamemaker.service.MessageDeduplicator messageDeduplicator) {
        this.messageDeduplicator = messageDeduplicator;
    }

    /** 设置系统配置服务 */
    public void setConfigService(com.chengxun.gamemaker.web.service.SystemConfigService configService) {
        this.configService = configService;
    }

    /** 设置协作增强服务 */
    public void setCollaborationEnhancer(com.chengxun.gamemaker.service.CollaborationEnhancer collaborationEnhancer) {
        this.collaborationEnhancer = collaborationEnhancer;
    }

    /**
     * 设置消息到达回调
     * 当收到新消息时触发回调，用于事件驱动调度
     *
     * @param callback 回调函数
     */
    public void setMessageArrivedCallback(Runnable callback) {
        this.messageArrivedCallback = callback;
    }

    /**
     * 记录 Agent 日志
     *
     * @param action 操作类型
     * @param level 日志级别
     * @param summary 摘要
     * @param detail 详情
     */
    protected void logAgent(String action, String level, String summary, String detail) {
        if (agentLogService != null) {
            try {
                String projectId = currentProject != null ? currentProject.getId() : null;
                // 自动获取当前任务 ID
                String taskId = agentContext != null ? agentContext.getCurrentTaskId() : null;
                agentLogService.logAsync(getId(), getName(), action, level, summary, detail,
                    projectId, taskId, null, null);
            } catch (Exception e) {
                log.warn("Failed to write agent log: {}", e.getMessage());
            }
        }
    }

    /**
     * 记录 Agent 日志（简化版）
     */
    protected void logAgent(String action, String level, String summary) {
        logAgent(action, level, summary, null);
    }

    /**
     * 记录 Agent 信息日志
     */
    protected void logInfo(String action, String summary) {
        logAgent(action, "INFO", summary);
    }

    /**
     * 记录 Agent 警告日志
     */
    protected void logWarn(String action, String summary, String detail) {
        logAgent(action, "WARN", summary, detail);
    }

    /**
     * 记录 Agent 错误日志
     */
    protected void logError(String action, String summary, String detail) {
        logAgent(action, "ERROR", summary, detail);
    }

    /**
     * 记录 Agent 决策日志
     */
    protected void logDecision(String summary, String decision) {
        if (agentLogService != null) {
            try {
                agentLogService.decision(getId(), getName(), summary, decision);
            } catch (Exception e) {
                log.warn("Failed to write agent decision log: {}", e.getMessage());
            }
        }
    }

    /**
     * 记录 Agent AI 调用日志
     */
    protected void logAiCall(String summary, long durationMs) {
        if (agentLogService != null) {
            try {
                agentLogService.aiCall(getId(), getName(), summary, durationMs);
            } catch (Exception e) {
                log.warn("Failed to write agent AI call log: {}", e.getMessage());
            }
        }
    }

    /**
     * 记录 Agent AI 调用日志（含输入输出详情）
     *
     * @param summary    摘要
     * @param input      AI 输入内容
     * @param output     AI 输出内容
     * @param durationMs 耗时
     */
    protected void logAiCall(String summary, String input, String output, long durationMs) {
        if (agentLogService != null) {
            try {
                agentLogService.aiCall(getId(), getName(), summary, input, output, durationMs);
            } catch (Exception e) {
                log.warn("Failed to write agent AI call log: {}", e.getMessage());
            }
        }
    }

    /**
     * 记录 Agent 任务日志
     */
    protected void logTask(String action, String summary, String taskId) {
        if (agentLogService != null) {
            try {
                String projectId = currentProject != null ? currentProject.getId() : null;
                agentLogService.taskLog(getId(), getName(), action, summary, taskId, projectId);
            } catch (Exception e) {
                log.warn("Failed to write agent task log: {}", e.getMessage());
            }
        }
    }

    /**
     * 记录 Agent 任务日志（含详情）
     */
    protected void logTask(String action, String summary, String detail, String taskId) {
        if (agentLogService != null) {
            try {
                String projectId = currentProject != null ? currentProject.getId() : null;
                agentLogService.logAsync(getId(), getName(), action, "INFO", summary, detail, projectId, taskId, null, null);
            } catch (Exception e) {
                log.warn("Failed to write agent task log: {}", e.getMessage());
            }
        }
    }

    /**
     * 获取 Agent 运行时 ID
     * 格式: projectId:agentRole（如 project-123:producer）
     * 全局唯一，用于消息路由和 Agent 查找
     */
    @Override
    public String getId() {
        return definition.getEffectiveId();
    }

    /**
     * 获取 Agent 原始角色（如 producer、server-dev）
     */
    public String getAgentRole() {
        return definition.getRole();
    }

    /**
     * 获取所属项目 ID
     */
    public String getProjectId() {
        return definition.getProjectId();
    }

    @Override
    public String getName() {
        return definition.getName();
    }

    @Override
    public String getRole() {
        return definition.getRole();
    }

    @Override
    public AgentDefinition getDefinition() {
        return definition;
    }

    public GameProject getCurrentProject() {
        return currentProject;
    }

    public AgentContext getAgentContext() {
        return agentContext;
    }

    public String getCurrentConversationId() {
        return currentConversationId;
    }

    @Override
    public void initialize() {
        String initProjectName = definition.getProjectId() != null ? definition.getProjectId() : "全局";
        log.info("Initializing agent: {} ({})", getName(), getId());
        logInfo("AGENT_STARTED", String.format("Agent 开始初始化，角色: %s，项目: %s", getRole(), initProjectName));

        // 初始化项目
        initProject();

        // 加载或创建上下文
        agentContext = contextManager.getOrCreateContext(getId(), currentProject);
        if (agentContext.getSessionId() != null) {
            definition.setSessionId(agentContext.getSessionId());
        }
        if (agentContext.getWorkDir() != null) {
            definition.setWorkDir(agentContext.getWorkDir());
        }

        // 初始化记忆分类
        initMemoryCategories();

        // 加载默认记忆
        loadDefaultMemory();

        // 加载项目级别 SKILL
        loadProjectSkills();

        // 注册到消息总线（携带 projectId 实现项目级消息隔离）
        String projectId = getProjectId();
        if (projectId != null) {
            messageBus.registerAgent(this, projectId);
        } else {
            messageBus.registerAgent(this);
        }

        // 【重要】检查是否有旧的上下文需要恢复
        // 如果 Agent 重启，应该恢复之前的工作上下文，避免丢失状态
        if (agentContext != null && agentContext.getSessionId() != null) {
            log.info("检测到旧的会话上下文，尝试恢复: sessionId={}", agentContext.getSessionId());
            try {
                recoverContextInternal();
            } catch (Exception e) {
                log.warn("恢复上下文失败，将开始新的对话: {}", e.getMessage());
            }
        }

        // 开始新的对话
        startNewConversation();

        String initializedProjectName = currentProject != null ? currentProject.getName() : "全局";
        log.info("Agent initialized: {} (runtimeId={}) for project: {}", getName(), getId(), initializedProjectName);
        logInfo("AGENT_STARTED", String.format("Agent 初始化完成，角色: %s，项目: %s，工作目录: %s",
            getRole(), initializedProjectName, definition.getWorkDir() != null ? definition.getWorkDir() : "未设置"));
    }

    protected void initProject() {
        String workDir = definition.getWorkDir();
        if (workDir != null && !workDir.isEmpty()) {
            currentProject = projectManager.getOrCreateProject(workDir);
            // 确保 definition 的 projectId 与项目一致
            if (definition.getProjectId() == null) {
                definition.setProjectId(currentProject.getId());
            }
            currentProject.addAgent(getId());
            projectManager.saveProjectConfig(currentProject);
        }
    }

    protected void initMemoryCategories() {
        if (currentProject != null) {
            // 项目级别记忆目录
            memoryManager.initAgentMemoryCategories(currentProject, getId());
        }
    }

    protected void loadDefaultMemory() {
        // 加载 AGENTS.md 文件内容
        String agentsFile = definition.getAgentsFile();
        if (agentsFile != null && !agentsFile.isEmpty()) {
            try {
                java.nio.file.Path path = java.nio.file.Path.of(agentsFile);
                if (java.nio.file.Files.exists(path)) {
                    String content = java.nio.file.Files.readString(path);
                    saveKnowledge("agents_file", content);
                }
            } catch (Exception e) {
                log.warn("Failed to load agents file for {}: {}", getId(), e.getMessage());
            }
        }

        // 加载项目规则
        if (currentProject != null) {
            String rules = projectManager.loadProjectRules(currentProject.getId());
            if (rules != null) {
                saveKnowledge("project_rules", rules);
            }
        }
    }

    protected void loadProjectSkills() {
        if (currentProject != null) {
            skillManager.loadProjectSkills(currentProject.getId(), currentProject.getSkillsDir());
        }
    }

    protected void startNewConversation() {
        currentConversationId = UUID.randomUUID().toString();
        log.debug("New conversation started: {}", currentConversationId);
    }

    @Override
    public void start() {
        if (running) {
            log.warn("Agent {} is already running", getName());
            return;
        }
        running = true;
        String projectId = getProjectId();
        if (projectId != null) {
            messageBus.registerAgent(this, projectId);
        } else {
            messageBus.registerAgent(this);
        }
        String projectName = currentProject != null ? currentProject.getName() : "全局";
        log.info("Agent started: {} (runtimeId={})", getName(), getId());
        logInfo("AGENT_STARTED", String.format("Agent 已启动，角色: %s，项目: %s", getRole(), projectName));
    }

    @Override
    public void stop() {
        String projectName = currentProject != null ? currentProject.getName() : "全局";
        logInfo("AGENT_STOPPED", String.format("Agent 正在停止，角色: %s，项目: %s", getRole(), projectName));
        running = false;
        messageBus.unregisterAgent(getId());
        saveContext();
        createSnapshot();
        log.info("Agent stopped: {}", getName());
        logInfo("AGENT_STOPPED", String.format("Agent 已停止，角色: %s，项目: %s", getRole(), projectName));
    }

    @Override
    public boolean isBusy() {
        return busy;
    }

    @Override
    public boolean isAlive() {
        return running;
    }

    @Override
    public void work() {
        if (!running) {
            return;
        }

        // 即使 Agent 正在忙，也要处理消息队列（干预等高优先级消息不能被阻塞）
        if (!busy) {
            processPendingMessagesAndWork();
        } else {
            // busy 状态下只处理消息，不执行 doWork
            processPendingMessages();
        }
    }

    private void processPendingMessagesAndWork() {
        busy = true;
        // 为本工作周期生成唯一 taskId，使所有日志自动关联
        String workTaskId = "work-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
        agentContext.setCurrentTaskId(workTaskId);

        long startTime = System.currentTimeMillis();
        String projectName = currentProject != null ? currentProject.getName() : "全局";
        int pendingMsgCount = pendingMessages.size();
        int taskCount = tasks.size();
        try {
            logAgent("TASK_STARTED", "INFO",
                String.format("[%s] %s 开始工作周期，项目: %s，待处理消息: %d，已有任务: %d",
                    workTaskId, getName(), projectName, pendingMsgCount, taskCount),
                String.format("Agent ID: %s\n角色: %s\n项目: %s\n工作目录: %s\n会话ID: %s\n待处理消息: %d\n已有任务: %d\n工作周期ID: %s",
                    getId(), getRole(), projectName,
                    definition.getWorkDir() != null ? definition.getWorkDir() : "未设置",
                    definition.getSessionId() != null ? definition.getSessionId() : "无",
                    pendingMsgCount, taskCount, workTaskId));
            processPendingMessages();
            doWork();
            long duration = System.currentTimeMillis() - startTime;
            logAgent("TASK_COMPLETED", "INFO",
                String.format("[%s] %s 工作周期完成，项目: %s，耗时: %dms",
                    workTaskId, getName(), projectName, duration),
                String.format("Agent ID: %s\n角色: %s\n项目: %s\n工作周期ID: %s\n耗时: %dms\n完成时间: %s",
                    getId(), getRole(), projectName, workTaskId, duration,
                    java.time.LocalDateTime.now()));
        } catch (Exception e) {
            log.error("Error in agent work: {}", getName(), e);
            logError("TASK_FAILED",
                String.format("[%s] %s 工作周期失败，项目: %s，异常: %s", workTaskId, getName(), projectName, e.getClass().getSimpleName()),
                String.format("Agent ID: %s\n角色: %s\n项目: %s\n工作周期ID: %s\n异常类型: %s\n异常信息: %s\n堆栈:\n%s",
                    getId(), getRole(), projectName, workTaskId,
                    e.getClass().getName(), e.getMessage(),
                    e.getStackTrace().length > 0 ? java.util.Arrays.stream(e.getStackTrace()).limit(10).map(StackTraceElement::toString).collect(java.util.stream.Collectors.joining("\n")) : "无"));
        } finally {
            agentContext.setCurrentTaskId(null);
            busy = false;
        }
    }

    protected abstract void doWork();

    protected void processPendingMessages() {
        AgentMessage message;
        while ((message = pendingMessages.poll()) != null) {
            try {
                // 拦截干预消息，自动处理（不依赖子类实现）
                if (message.getType() == AgentMessage.MessageType.SYSTEM
                    && message.getContent() != null
                    && message.getContent().contains("人工干预通知")) {
                    handleInterventionMessage(message);
                    continue;
                }
                handleMessage(message);
            } catch (Exception e) {
                log.error("Error processing message for agent {}: {}", getName(), e.getMessage());
            }
        }
    }

    /**
     * 处理人工干预消息
     * 自动确认干预、将指令注入上下文、等待后续工作周期真正执行
     *
     * 【重要设计原则】
     * 对于 INSTRUCTION 类型的干预，不能立即标记为"已执行"，
     * 因为 AI 的回复只是"说"它会执行，并没有真正调用任何能力。
     * 正确的做法是：把干预指令注入到 Agent 的工作上下文中，
     * 让 Agent 在后续的工作周期中，基于干预指令做出真正的决策和行动。
     *
     * @param message 干预消息
     */
    private void handleInterventionMessage(AgentMessage message) {
        String content = message.getContent();
        log.info("Agent [{}] 收到人工干预消息，开始处理", getName());

        // 从消息中提取干预编号
        String interventionNo = extractInterventionNo(content);

        // 找到对应的干预记录
        if (interventionService == null) {
            log.warn("干预服务未注入，无法处理干预消息");
            return;
        }

        com.chengxun.gamemaker.web.entity.AgentIntervention intervention = null;
        if (interventionNo != null) {
            // 通过编号查找
            intervention = interventionService.getInterventionsByNo(interventionNo);
        }

        if (intervention == null) {
            log.warn("未找到干预记录，编号: {}", interventionNo);
            return;
        }

        try {
            // 1. 确认干预
            if (intervention.getStatus() == com.chengxun.gamemaker.web.entity.AgentIntervention.Status.PENDING) {
                interventionService.acknowledgeIntervention(intervention.getId(), getName() + " 已收到干预指令");
            }

            // 2. 将干预指令注入到工作上下文中（而不是立即执行）
            String instruction = intervention.getInstruction();
            if (instruction != null && !instruction.isEmpty()) {
                log.info("Agent [{}] 将干预指令注入工作上下文: {}", getName(),
                    instruction.length() > 100 ? instruction.substring(0, 100) + "..." : instruction);

                // 将干预指令保存到工作记忆中，让后续工作周期可以看到并执行
                String interventionKey = "pending_intervention_" + interventionNo;
                agentContext.addWorkingMemory(interventionKey, instruction);

                // 同时保存到经验记忆中，让 buildWorkContext 可以加载
                saveExperience("intervention_" + interventionNo,
                    String.format("[干预指令 %s] 类型: %s | 指令: %s | 状态: 待执行",
                        interventionNo,
                        intervention.getInterventionTypeDescription(),
                        instruction != null ? instruction : "无"));

                // 3. 标记干预为"已确认，等待执行"（而不是"已执行"）
                // 注：这里不调用 executeIntervention，而是让后续工作周期来真正执行
                logInfo("INTERVENTION_RECEIVED", String.format("已收到干预指令 [%s]，类型: %s，将在下一个工作周期中执行\n\n指令: %s",
                    interventionNo, intervention.getInterventionTypeDescription(),
                    instruction != null && instruction.length() > 200
                        ? instruction.substring(0, 200) + "..." : instruction));
            }

        } catch (Exception e) {
            log.error("处理干预消息失败: {}", e.getMessage());
            try {
                interventionService.rejectIntervention(intervention.getId(), "处理失败: " + e.getMessage());
            } catch (Exception ex) {
                log.error("标记干预失败也失败了: {}", ex.getMessage());
            }
        }
    }

    /**
     * 从干预消息内容中提取干预编号
     * 消息格式: **干预编号**: INT-20260608-XXXXXX
     */
    private String extractInterventionNo(String content) {
        if (content == null) return null;
        java.util.regex.Matcher matcher = java.util.regex.Pattern
            .compile("\\*?\\*?干预编号\\*?\\*?[：:]\\s*(INT-[\\w-]+)")
            .matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }

    protected abstract void handleMessage(AgentMessage message);

    @Override
    public String sendMessage(String message) {
        return sendMessageInternal(message, true);
    }

    /**
     * 调用 AI 但不保存到对话历史，也不处理能力调用
     * 用于内部辅助功能（如生成 checklist、任务分解），避免污染主对话上下文
     *
     * @param message 消息内容
     * @return AI 响应
     */
    protected String callAiDirect(String message) {
        return sendMessageInternal(message, false, true);
    }

    private String sendMessageInternal(String message, boolean saveToConversation) {
        return sendMessageInternal(message, saveToConversation, false);
    }

    private String sendMessageInternal(String message, boolean saveToConversation, boolean skipCapabilityProcessing) {
        if (!running) {
            return "Agent is not running";
        }

        // 检查 Token 预算
        if (tokenBudgetService != null && !tokenBudgetService.checkBudget(getId())) {
            log.warn("Agent {} 已超出 Token 预算，暂停调用", getId());
            return "Token 预算已用完，请联系管理员增加配额。";
        }

        busy = true;
        long startTime = System.currentTimeMillis();
        try {

            // 构建系统指令（推理深度 + 思维模式，合并为一个指令）
            String systemInstruction = buildSystemInstruction();

            // 注入能力列表 prompt（使用缓存）
            String capabilityPrompt = buildCapabilityPromptCached();

            // 注入协作上下文（使用缓存）
            String collaborationContext = buildCollaborationContextPromptCached();

            // 组装增强消息：系统指令 + 能力 + 协作上下文 + 原始消息
            StringBuilder enhanced = new StringBuilder();
            if (!systemInstruction.isEmpty()) {
                enhanced.append(systemInstruction).append("\n\n");
            }
            if (!capabilityPrompt.isEmpty()) {
                enhanced.append(capabilityPrompt).append("\n\n");
            }
            if (!collaborationContext.isEmpty()) {
                enhanced.append(collaborationContext).append("\n\n");
            }
            enhanced.append(message);
            String enhancedMessage = enhanced.toString();

            if (saveToConversation) {
                saveConversationMessage("user", message);
            }

            // 生成 MCP 配置（使用缓存）
            String mcpConfig = buildMcpConfigCached();

            String sessionId = definition.getSessionId();

            // 根据 API URL 选择引擎：OpenAI 兼容 API 使用专用引擎，Anthropic API 使用 Claude CLI
            com.chengxun.gamemaker.engine.ClaudeCliEngine.AiCallResult aiResult;
            String apiUrl = definition.getApiUrl();
            String apiKey = definition.getApiKey();
            String model = definition.getModel();

            if (openAiEngine != null && com.chengxun.gamemaker.engine.OpenAiCompatibleEngine.isOpenAiCompatible(apiUrl)) {
                // OpenAI 兼容 API（MiniMax、DeepSeek 等）：直接调用，不经过 Claude CLI
                aiResult = openAiEngine.sendMessage(getId(), enhancedMessage, apiKey, apiUrl, model);
            } else {
                // Anthropic API：使用 Claude CLI
                String effort = AgentDefinition.reasoningDepthToEffort(definition.getReasoningDepth());
                aiResult = cliEngine.sendMessageWithTokenUsage(
                    getId(), sessionId, enhancedMessage,
                    definition.getWorkDir(), apiKey,
                    apiUrl, model, mcpConfig, effort
                );
            }
            String response = aiResult.response;

            String newSessionId = cliEngine.getSessionId(getId());
            if (newSessionId != null && !newSessionId.equals(sessionId)) {
                definition.setSessionId(newSessionId);
                agentContext.setSessionId(newSessionId);
                contextManager.updateSessionId(getId(), currentProject, newSessionId);
            }

            // 更新上下文监控活动时间
            if (contextMonitor != null) {
                contextMonitor.updateActivityTime(getId());
            }

            // 记录 AI 调用完成
            long duration = System.currentTimeMillis() - startTime;
            int respLen = response != null ? response.length() : 0;
            String logInput = message.length() > 5000 ? message.substring(0, 5000) + "\n...[截断，总长: " + message.length() + "]" : message;
            String logOutput = response != null && response.length() > 10000
                ? response.substring(0, 10000) + "\n...[截断，总长: " + response.length() + "]" : response;
            String tokenSource = aiResult.hasPreciseTokens ? "精确" : "估算";
            logAiCall(String.format("AI 调用完成，输入长度: %d，响应长度: %d，耗时: %dms，Token(%s): 入=%d 出=%d",
                message.length(), respLen, duration, tokenSource, aiResult.inputTokens, aiResult.outputTokens),
                logInput, logOutput, duration);

            // 使用精确 Token 数据记录消耗
            recordTokenUsagePrecise(aiResult.inputTokens, aiResult.outputTokens);

            // 累计会话 Token 数
            sessionTokenCount += aiResult.inputTokens + aiResult.outputTokens;

            // 解析 Claude 输出中的能力调用（callAiDirect 模式跳过）
            if (!skipCapabilityProcessing) {
                response = processCapabilityActions(response);
            }

            // 检测 AI 响应中的权限错误，自动通知管理员
            detectAndNotifyPermissionErrors(response);

            saveConversationMessage("assistant", response);

            agentContext.addWorkingMemory("last_message", message.length() > 100 ? message.substring(0, 100) : message);
            agentContext.addWorkingMemory("last_response", response.length() > 100 ? response.substring(0, 100) : response);

            // 基于 token 计数的自动压缩触发
            autoCompactByTokenCount();

            return response;
        } finally {
            busy = false;
        }
    }

    /** 对话消息保存的同步锁 */
    private final Object conversationLock = new Object();

    /**
     * 保存对话消息
     * 保存后自动检查窗口大小，超出时裁剪旧消息
     */
    protected void saveConversationMessage(String role, String content) {
        synchronized (conversationLock) {
            ContextManager.ConversationMessage msg = new ContextManager.ConversationMessage(role, content);
            msg.setTaskId(agentContext.getCurrentTaskId());

            List<ContextManager.ConversationMessage> messages = contextManager.loadConversation(getId(), currentProject, currentConversationId);
            messages.add(msg);
            contextManager.saveConversation(getId(), currentProject, currentConversationId, messages);

            // 窗口截断：超过阈值时裁剪旧消息
            int windowSize = 50;
            if (configService != null) {
                windowSize = configService.getInt(
                    com.chengxun.gamemaker.config.SystemConstants.AGENT_CONTEXT_WINDOW_SIZE, 50);
            }

            if (messages.size() > windowSize + 10) {
                contextManager.trimConversation(getId(), currentProject, currentConversationId, windowSize);
            }
        }
    }

    @Override
    public void receiveMessage(AgentMessage message) {
        pendingMessages.offer(message);
        String content = message.getContent() != null ? message.getContent() : "";
        String contentPreview = content.length() > 80 ? content.substring(0, 80) + "..." : content;
        log.debug("Agent {} received message from {}", getName(), message.getFromAgentId());
        // summary 存摘要，detail 存完整消息
        logAgent("MESSAGE_RECEIVED", "INFO",
            String.format("收到来自 %s 的消息，类型: %s，内容预览: %s",
                message.getFromAgentId(), message.getType(), contentPreview),
            String.format("来源: %s\n目标: %s\n类型: %s\n消息ID: %s\n完整内容:\n%s",
                message.getFromAgentId(), message.getToAgentId(), message.getType(),
                message.getId() != null ? message.getId() : "无",
                content.length() > 5000 ? content.substring(0, 5000) + "\n...[截断]" : content));

        // 事件驱动：收到消息后触发回调，让调度器立即安排 Agent 工作
        // 避免等待下一个定时周期（5分钟）才处理消息
        if (messageArrivedCallback != null) {
            try {
                messageArrivedCallback.run();
            } catch (Exception e) {
                log.debug("Message arrived callback failed for agent {}: {}", getId(), e.getMessage());
            }
        }
    }

    @Override
    public List<AgentMessage> getPendingMessages() {
        return new ArrayList<>(pendingMessages);
    }

    /**
     * 清除所有待处理消息
     * 用于取消任务时清空消息队列
     */
    public void clearPendingMessages() {
        int count = pendingMessages.size();
        pendingMessages.clear();
        if (count > 0) {
            log.info("已清除 Agent {} 的 {} 条待处理消息", getName(), count);
        }
    }

    @Override
    public void sendMessage(AgentMessage message) {
        message.setFromAgentId(getId());
        messageBus.send(message);
        logInfo("MESSAGE_SENT", "发送消息给 " + message.getToAgentId() + "，类型: " + message.getType());
    }

    @Override
    public void assignTask(TaskAssignment task) {
        tasks.add(task);
        agentContext.setCurrentTaskId(task.getId());
        String desc = task.getDescription() != null && !task.getDescription().isEmpty()
            ? task.getDescription() : "无";
        String summary = desc.length() > 100 ? desc.substring(0, 100) + "..." : desc;
        log.info("Task assigned to {}: {}", getName(), task.getTitle());
        // summary 存摘要，detail 存完整描述
        logTask("TASK_RECEIVED",
            String.format("接收任务: %s，描述: %s", task.getTitle(), summary),
            String.format("任务ID: %s\n任务标题: %s\n分配者: %s\n完整描述:\n%s",
                task.getId(), task.getTitle(),
                task.getAssignerId() != null ? task.getAssignerId() : "系统",
                desc),
            task.getId());

        // 自动发送任务确认给分配者
        sendTaskConfirmation(task);
    }

    /**
     * 发送任务确认消息
     * 当 Agent 收到任务时，自动向分配者发送确认
     */
    private void sendTaskConfirmation(TaskAssignment task) {
        if (task.getAssignerId() == null || task.getAssignerId().isEmpty()) return;

        try {
            AgentMessage confirmation = AgentMessage.builder()
                .fromAgentId(getId())
                .toAgentId(task.getAssignerId())
                .type(AgentMessage.MessageType.RESPONSE)
                .content(String.format("TASK_ACCEPTED:%s|%s|%s",
                    task.getId(), task.getTitle(), getName()))
                .build();
            sendMessage(confirmation);
            log.info("任务确认已发送: {} -> {} (任务: {})", getName(), task.getAssignerId(), task.getTitle());
        } catch (Exception e) {
            log.warn("发送任务确认失败: {}", e.getMessage());
        }
    }

    @Override
    public List<TaskAssignment> getTasks() {
        return new ArrayList<>(tasks);
    }

    @Override
    public void reportProgress(String taskId, String progress) {
        for (TaskAssignment task : tasks) {
            if (task.getId().equals(taskId)) {
                AgentMessage report = AgentMessage.createReport(getId(),
                    String.format("Task [%s] progress: %s", task.getTitle(), progress));
                report.setToAgentId(task.getAssignerId());
                sendMessage(report);
                break;
            }
        }
    }

    // ===== 上下文管理增强 =====

    /**
     * 保存 API 配置到 AgentContext（持久化）
     * 用于 Token 分配时将 API 配置持久化到文件
     *
     * @param apiKey API Key
     * @param apiUrl API URL
     * @param model  模型
     */
    public void saveApiConfigToContext(String apiKey, String apiUrl, String model) {
        if (apiKey != null) agentContext.setApiKey(apiKey);
        if (apiUrl != null) agentContext.setApiUrl(apiUrl);
        if (model != null) agentContext.setModel(model);
        // 持久化到文件
        saveContext();
        log.info("Saved API config to context: agentId={}, apiKey={}..., apiUrl={}, model={}",
            getId(), apiKey != null ? apiKey.substring(0, Math.min(10, apiKey.length())) : "null",
            apiUrl, model);
    }

    @Override
    public void saveContext() {
        agentContext.setWorkDir(definition.getWorkDir());
        agentContext.setSessionId(definition.getSessionId());
        agentContext.setReasoningDepth(definition.getReasoningDepth());
        contextManager.saveContext(agentContext, currentProject);
    }

    @Override
    public void loadContext() {
        agentContext = contextManager.loadContext(getId(), currentProject);
        if (agentContext != null) {
            definition.setSessionId(agentContext.getSessionId());
            definition.setWorkDir(agentContext.getWorkDir());
            if (agentContext.getReasoningDepth() != null) {
                definition.setReasoningDepth(agentContext.getReasoningDepth());
            }
        }
    }

    protected void createSnapshot() {
        List<ContextManager.ConversationMessage> recentMessages = contextManager.getRecentMessages(getId(), currentProject, 20);
        contextManager.createSnapshot(getId(), currentProject, agentContext, recentMessages);
        contextManager.cleanupOldSnapshots(getId(), currentProject, 5);
    }

    /**
     * 恢复上下文（内部使用）
     */
    protected void recoverContextInternal() {
        log.info("Recovering context for agent: {} in project: {}", getId(),
            currentProject != null ? currentProject.getName() : "global");

        Map<String, Object> snapshot = contextManager.loadLatestSnapshot(getId(), currentProject);
        if (snapshot != null) {
            log.info("Found snapshot, recovering...");

            if (snapshot.containsKey("context")) {
                agentContext.addWorkingMemory("recovery_time", LocalDateTime.now().toString());
                agentContext.addWorkingMemory("recovery_source", "snapshot");
            }

            if (snapshot.containsKey("recentMessages")) {
                String summary = buildConversationSummary(snapshot);
                agentContext.addWorkingMemory("conversation_summary", summary);
            }
        }

        Map<String, String> knowledge = loadAllKnowledge();
        Map<String, String> experiences = loadAllExperiences();

        String recoveryPrompt = buildRecoveryPrompt(knowledge, experiences);
        if (!recoveryPrompt.isEmpty()) {
            log.info("Sending recovery prompt to rebuild context");
            sendMessage(recoveryPrompt);
        }
    }

    @SuppressWarnings("unchecked")
    protected String buildConversationSummary(Map<String, Object> snapshot) {
        Object messagesObj = snapshot.get("recentMessages");
        if (messagesObj instanceof List<?> messages) {
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (Object msgObj : messages) {
                if (msgObj instanceof Map<?, ?> msg && count < 5) {
                    String role = (String) msg.get("role");
                    String content = (String) msg.get("content");
                    if (content != null && content.length() > 100) {
                        content = content.substring(0, 100) + "...";
                    }
                    sb.append(String.format("- [%s]: %s\n", role, content));
                    count++;
                }
            }
            return sb.toString();
        }
        return "";
    }

    protected String buildRecoveryPrompt(Map<String, String> knowledge, Map<String, String> experiences) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 上下文恢复\n\n");
        sb.append("由于会话中断，需要恢复工作上下文。\n\n");

        if (!knowledge.isEmpty()) {
            sb.append("### 已有知识\n");
            knowledge.forEach((key, value) -> {
                String summary = value.length() > 200 ? value.substring(0, 200) + "..." : value;
                sb.append(String.format("- **%s**: %s\n", key, summary));
            });
            sb.append("\n");
        }

        if (!experiences.isEmpty()) {
            sb.append("### 已有经验\n");
            experiences.forEach((key, value) -> {
                String summary = value.length() > 200 ? value.substring(0, 200) + "..." : value;
                sb.append(String.format("- **%s**: %s\n", key, summary));
            });
            sb.append("\n");
        }

        sb.append("请基于以上信息恢复工作上下文，并确认你已准备好继续工作。");
        return sb.toString();
    }

    // ===== 记忆管理（项目级别） =====

    @Override
    public void saveMemory(String key, String value) {
        if (currentProject != null) {
            memoryManager.saveMemory(currentProject, getId(), "general", key, value);
        } else {
            log.warn("No project assigned to agent {}, cannot save memory", getId());
        }
    }

    @Override
    public String loadMemory(String key) {
        if (currentProject != null) {
            return memoryManager.loadMemory(currentProject, getId(), "general", key);
        }
        return null;
    }

    public void saveKnowledge(String key, String value) {
        if (currentProject != null) {
            memoryManager.saveMemory(currentProject, getId(), "knowledge", key, value);
        } else {
            log.warn("saveKnowledge 失败: Agent {} 的 currentProject 为空，key={} 未保存", getId(), key);
        }
    }

    public String loadKnowledge(String key) {
        if (currentProject != null) {
            return memoryManager.loadMemory(currentProject, getId(), "knowledge", key);
        }
        return null;
    }

    public void saveExperience(String key, String value) {
        if (currentProject == null) return;
        if (key == null || key.isEmpty()) return;
        if (value == null || value.isEmpty()) return;

        // 过滤掉包含 null 的无效经验
        if (key.contains("null") || value.contains("null")) {
            log.debug("Skipping experience with null content: key={}", key);
            return;
        }

        memoryManager.saveMemory(currentProject, getId(), "experiences", key, value);
        agentContext.addLearnedPattern(key);

        // 跨 Agent 经验共享：将经验同步给同项目其他 Agent
        if (collaborationEnhancer != null) {
            try {
                collaborationEnhancer.shareExperience(currentProject.getId(), getId(), key, value);
            } catch (Exception e) {
                log.debug("Failed to share experience: {}", e.getMessage());
            }
        }
    }

    public String loadExperience(String key) {
        if (currentProject != null) {
            return memoryManager.loadMemory(currentProject, getId(), "experiences", key);
        }
        return null;
    }

    public void saveSkill(String key, String value) {
        if (currentProject != null) {
            memoryManager.saveMemory(currentProject, getId(), "skills", key, value);
        }
    }

    public String loadSkill(String key) {
        if (currentProject != null) {
            return memoryManager.loadMemory(currentProject, getId(), "skills", key);
        }
        return null;
    }

    protected Map<String, String> loadAllKnowledge() {
        if (currentProject != null) {
            return memoryManager.getCategoryMemory(currentProject, getId(), "knowledge");
        }
        return new HashMap<>();
    }

    protected Map<String, String> loadAllExperiences() {
        if (currentProject != null) {
            return memoryManager.getCategoryMemory(currentProject, getId(), "experiences");
        }
        return new HashMap<>();
    }

    // ===== 互相监督机制 =====

    /**
     * 向指定 Agent 发送审查请求
     */
    public void requestReview(String targetAgentId, String reviewContent) {
        AgentMessage reviewRequest = AgentMessage.createReview(getId(), targetAgentId, reviewContent);
        sendMessage(reviewRequest);
        log.info("Sent review request to {}: {}", targetAgentId,
            reviewContent.length() > 100 ? reviewContent.substring(0, 100) + "..." : reviewContent);
    }

    /**
     * 向制作人汇报审查结果
     */
    public void reportReviewToProducer(String reviewResult) {
        AgentMessage report = AgentMessage.createReport(getId(), reviewResult);
        // 使用运行时 ID 定位同项目的制作人
        String projectId = getProjectId();
        String producerId = projectId != null ? projectId + ":producer" : "producer";
        report.setToAgentId(producerId);
        sendMessage(report);
        log.info("Reported review result to producer: {}", producerId);
    }

    /**
     * 选择合适的审查者（返回运行时 ID）
     * 审查者在同一项目内选择
     */
    protected String selectReviewer() {
        String role = getRole();
        String reviewerRole = switch (role) {
            case "server-dev" -> "git-commit";
            case "system-planner" -> "numerical-planner";
            case "numerical-planner" -> "system-planner";
            case "git-commit" -> "producer";
            case "producer" -> null;
            default -> "git-commit";
        };
        if (reviewerRole == null) return null;
        String projectId = getProjectId();
        return projectId != null ? projectId + ":" + reviewerRole : reviewerRole;
    }

    /**
     * 处理审查请求（子类可重写）
     */
    protected String processReviewRequest(String reviewContent) {
        String skillPrompt = buildSkillPrompt("审查和审核");
        String fullPrompt = skillPrompt + "\n\n请审查以下内容：\n\n" + reviewContent;
        return sendMessage(fullPrompt);
    }

    /**
     * 发送审查结果给请求者
     */
    public void sendReviewResult(String requesterId, String reviewResult) {
        AgentMessage response = AgentMessage.builder()
            .fromAgentId(getId())
            .toAgentId(requesterId)
            .type(AgentMessage.MessageType.RESPONSE)
            .content(reviewResult)
            .build();
        sendMessage(response);
    }

    /**
     * 保存审查记录
     */
    protected void saveReviewRecord(String reviewId, String reviewContent, String reviewResult) {
        String recordKey = "review_" + reviewId;
        String recordValue = String.format(
            "审查时间: %s\n审查内容: %s\n审查结果: %s",
            java.time.LocalDateTime.now(),
            reviewContent.length() > 200 ? reviewContent.substring(0, 200) + "..." : reviewContent,
            reviewResult.length() > 500 ? reviewResult.substring(0, 500) + "..." : reviewResult
        );
        saveExperience(recordKey, recordValue);
    }

    // ===== 结构化协作协议 =====

    /**
     * 发送结构化任务请求
     * 标准化的任务分配格式，让接收方明确知道要做什么
     *
     * @param targetAgentId 目标Agent
     * @param taskTitle 任务标题
     * @param taskDescription 任务描述
     * @param acceptanceCriteria 验收标准
     * @param priority 优先级（HIGH/MEDIUM/LOW）
     * @param dependencies 依赖任务
     */
    public void sendStructuredTask(String targetAgentId, String taskTitle, String taskDescription,
                                   String acceptanceCriteria, String priority, String dependencies) {
        String structuredContent = String.format(
            "【结构化任务请求】\n\n" +
            "任务ID: %s\n" +
            "任务标题: %s\n" +
            "优先级: %s\n" +
            "依赖: %s\n\n" +
            "## 任务描述\n%s\n\n" +
            "## 验收标准\n%s\n\n" +
            "## 注意事项\n" +
            "- 请按验收标准逐项完成\n" +
            "- 完成后请发送结构化完成报告\n" +
            "- 如遇阻塞请及时上报",
            java.util.UUID.randomUUID().toString().substring(0, 8),
            taskTitle,
            priority != null ? priority : "MEDIUM",
            dependencies != null ? dependencies : "无",
            taskDescription,
            acceptanceCriteria
        );

        AgentMessage message = AgentMessage.builder()
            .fromAgentId(getId())
            .toAgentId(targetAgentId)
            .type(AgentMessage.MessageType.TASK)
            .content(structuredContent)
            .build();
        sendMessage(message);
        log.info("已发送结构化任务: {} -> {}", getId(), targetAgentId);
    }

    /**
     * 发送结构化完成报告
     * 标准化的任务完成报告格式
     *
     * @param taskTitle 任务标题
     * @param completedCriteria 已完成的验收标准
     * @param output 输出产物
     * @param issues 遇到的问题
     */
    public void sendStructuredCompletion(String taskTitle, String completedCriteria,
                                         String output, String issues) {
        String structuredContent = String.format(
            "【结构化完成报告】\n\n" +
            "任务标题: %s\n" +
            "完成时间: %s\n\n" +
            "## 验收标准完成情况\n%s\n\n" +
            "## 输出产物\n%s\n\n" +
            "## 遇到的问题\n%s",
            taskTitle,
            java.time.LocalDateTime.now(),
            completedCriteria,
            output,
            issues != null ? issues : "无"
        );

        // 记录任务产出到 ProjectBoard（任务依赖传递）
        if (collaborationEnhancer != null && currentProject != null) {
            try {
                collaborationEnhancer.recordTaskOutput(currentProject.getId(), getId(), taskTitle, output);
            } catch (Exception e) {
                log.debug("Failed to record task output: {}", e.getMessage());
            }
        }

        // 发送给ProducerAgent
        String producerId = getProjectId() + ":producer";
        AgentMessage message = AgentMessage.builder()
            .fromAgentId(getId())
            .toAgentId(producerId)
            .type(AgentMessage.MessageType.REPORT)
            .content(structuredContent)
            .build();
        sendMessage(message);
        log.info("已发送结构化完成报告: {}", taskTitle);
    }

    /**
     * 发送结构化阻塞报告
     * 当任务遇到阻塞时的标准报告格式
     *
     * @param taskTitle 任务标题
     * @param blocker 阻塞原因
     * @param impact 影响范围
     * @param suggestion 建议解决方案
     */
    public void sendStructuredBlocker(String taskTitle, String blocker, String impact, String suggestion) {
        String structuredContent = String.format(
            "【结构化阻塞报告】\n\n" +
            "任务标题: %s\n" +
            "报告时间: %s\n" +
            "严重级别: HIGH\n\n" +
            "## 阻塞原因\n%s\n\n" +
            "## 影响范围\n%s\n\n" +
            "## 建议解决方案\n%s\n\n" +
            "## 需要的支持\n" +
            "- 请尽快处理阻塞问题\n" +
            "- 如需要其他Agent协助请指定",
            taskTitle,
            java.time.LocalDateTime.now(),
            blocker,
            impact,
            suggestion
        );

        // 发送给ProducerAgent
        String producerId = getProjectId() + ":producer";
        AgentMessage message = AgentMessage.builder()
            .fromAgentId(getId())
            .toAgentId(producerId)
            .type(AgentMessage.MessageType.SYSTEM)
            .content(structuredContent)
            .priority(8) // 高优先级
            .build();
        sendMessage(message);
        log.warn("已发送结构化阻塞报告: {}", taskTitle);
    }

    /**
     * 发送结构化验证请求
     * 请求验证某个任务或里程碑的完成情况
     *
     * @param targetAgentId 目标Agent（通常是verifier）
     * @param milestoneId 里程碑ID
     * @param verificationCriteria 验证标准
     * @param currentOutput 当前输出
     */
    public void sendStructuredVerificationRequest(String targetAgentId, String milestoneId,
                                                   String verificationCriteria, String currentOutput) {
        String structuredContent = String.format(
            "【结构化验证请求】\n\n" +
            "里程碑ID: %s\n" +
            "请求时间: %s\n\n" +
            "## 验证标准\n%s\n\n" +
            "## 当前输出\n%s\n\n" +
            "## 请求\n" +
            "请验证以上输出是否满足验证标准，并给出通过/不通过的判断和改进建议。",
            milestoneId,
            java.time.LocalDateTime.now(),
            verificationCriteria,
            currentOutput
        );

        AgentMessage message = AgentMessage.builder()
            .fromAgentId(getId())
            .toAgentId(targetAgentId)
            .type(AgentMessage.MessageType.TASK)
            .content(structuredContent)
            .build();
        sendMessage(message);
        log.info("已发送结构化验证请求: milestone={}", milestoneId);
    }

    // ===== 上下文压缩和恢复 =====

    /**
     * 压缩当前对话上下文
     * 类似/compact命令，将长对话压缩成摘要
     *
     * @return 压缩后的摘要
     */
    public String compactContext() {
        if (contextCompactor == null) {
            log.warn("ContextCompactor not available for agent: {}", getId());
            return "上下文压缩服务不可用";
        }

        log.info("Compacting context for agent: {}", getId());
        String summary = contextCompactor.compactContext(getId(), currentProject, currentConversationId);

        // 清除 Claude CLI 会话，下次调用将创建全新会话
        // 否则 --resume sessionId 会恢复整个历史，压缩无效
        definition.setSessionId(null);
        if (agentContext != null) {
            agentContext.setSessionId(null);
        }

        // 关闭旧的 CLI 进程，下次调用将创建全新会话
        if (cliEngine != null) {
            cliEngine.destroyProcess(getId());
        }

        // 开始新的对话
        startNewConversation();

        log.info("Context compacted and session reset for agent: {}", getId());
        return summary;
    }

    /**
     * 恢复上下文
     * 从快照、记忆、技能等多种来源恢复工作上下文
     *
     * @return 恢复的上下文提示
     */
    public String recoverContext() {
        if (contextCompactor == null) {
            log.warn("ContextCompactor not available for agent: {}", getId());
            return "上下文恢复服务不可用";
        }

        log.info("Recovering context for agent: {}", getId());
        return contextCompactor.recoverContext(getId(), currentProject);
    }

    /**
     * 获取压缩历史
     *
     * @return 压缩记录列表
     */
    public List<Map<String, Object>> getCompactHistory() {
        if (contextCompactor == null || currentProject == null) {
            return Collections.emptyList();
        }
        return contextCompactor.getCompactHistory(getId(), currentProject);
    }

    /**
     * 检查是否需要压缩上下文
     * 基于对话消息数量和时间判断
     *
     * @return 是否需要压缩
     */
    public boolean shouldCompactContext() {
        // 获取最近的消息数量
        List<ContextManager.ConversationMessage> recentMessages =
            contextManager.getRecentMessages(getId(), currentProject, 100);

        // 如果消息超过50条，建议压缩
        if (recentMessages.size() > 50) {
            return true;
        }

        // 检查上次压缩时间
        String lastCompactTime = agentContext.getWorkingMemory("last_compact_time");
        if (lastCompactTime != null) {
            try {
                LocalDateTime lastCompact = LocalDateTime.parse(lastCompactTime);
                // 如果超过2小时未压缩，建议压缩
                if (lastCompact.plusHours(2).isBefore(LocalDateTime.now())) {
                    return true;
                }
            } catch (Exception e) {
                // 解析失败，忽略
            }
        }

        return false;
    }

    /**
     * 自动压缩上下文（如果需要）
     */
    public void autoCompactIfNeeded() {
        if (shouldCompactContext()) {
            log.info("Auto-compacting context for agent: {}", getId());
            compactContext();
        }
    }

    // ===== 能力系统 =====

    /**
     * 构建能力列表 prompt
     * 将当前 Agent 角色的可用能力格式化为 prompt 注入到 Claude 的输入中
     *
     * @return 能力列表 prompt 段落，如果能力系统不可用则返回空字符串
     */
    protected String buildCapabilityPrompt() {
        if (capabilityRegistry == null) {
            return "";
        }

        String projectId = currentProject != null ? currentProject.getId() : null;
        List<AgentCapability> capabilities = capabilityRegistry.getCapabilities(getRole(), projectId);
        if (capabilities.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## 你的能力\n\n");
        sb.append("你可以通过输出结构化 JSON 来调用以下能力。当你需要执行操作时，请输出以下格式：\n\n");
        sb.append("```json\n");
        sb.append("{\n");
        sb.append("  \"thinking\": \"你的分析思考过程\",\n");
        sb.append("  \"actions\": [\n");
        sb.append("    {\n");
        sb.append("      \"action\": \"能力名称\",\n");
        sb.append("      \"params\": {\"参数名\": \"参数值\"},\n");
        sb.append("      \"reason\": \"调用原因\"\n");
        sb.append("    }\n");
        sb.append("  ],\n");
        sb.append("  \"response\": \"给用户的自然语言回复\"\n");
        sb.append("}\n");
        sb.append("```\n\n");
        sb.append("### 可用能力列表\n\n");

        // 收集所有关联技能
        Set<String> allRelatedSkillIds = new LinkedHashSet<>();

        for (int i = 0; i < capabilities.size(); i++) {
            AgentCapability cap = capabilities.get(i);
            sb.append(String.format("%d. **%s** — %s\n", i + 1, cap.getCapabilityName(), cap.getDisplayName()));
            if (cap.getDescription() != null) {
                sb.append("   ").append(cap.getDescription()).append("\n");
            }
            if (cap.getParamSchema() != null && !cap.getParamSchema().isEmpty()) {
                sb.append("   参数: ").append(cap.getParamSchema()).append("\n");
            }
            if (cap.isRequiresApproval()) {
                sb.append("   需要审批: 是\n");
            }
            // 收集关联技能 ID
            if (cap.getRelatedSkillIds() != null && !cap.getRelatedSkillIds().isEmpty()) {
                for (String skillId : cap.getRelatedSkillIds().split(",")) {
                    String trimmed = skillId.trim();
                    if (!trimmed.isEmpty()) {
                        allRelatedSkillIds.add(trimmed);
                    }
                }
                sb.append("   关联技能: ").append(cap.getRelatedSkillIds()).append("\n");
            }
            sb.append("\n");
        }

        // 注入关联技能摘要（只注入名称+描述+触发词，不注入完整 prompt 模板）
        // 完整 prompt 模板在技能匹配时由 buildSkillPrompt 按需注入
        if (!allRelatedSkillIds.isEmpty() && skillManager != null) {
            StringBuilder skillSection = new StringBuilder();
            for (String skillId : allRelatedSkillIds) {
                Skill skill = skillManager.getGlobalSkill(skillId);
                if (skill == null && projectId != null) {
                    Map<String, Skill> projectSkills = skillManager.getProjectSkills(projectId);
                    skill = projectSkills.get(skillId);
                }
                if (skill != null) {
                    skillSection.append(String.format("- %s: %s", skill.getName(),
                        skill.getDescription() != null ? skill.getDescription() : skill.getName()));
                    if (skill.getTriggerPattern() != null) {
                        skillSection.append(String.format(" (触发: %s)", skill.getTriggerPattern()));
                    }
                    skillSection.append("\n");
                }
            }

            if (skillSection.length() > 0) {
                sb.append("### 关联技能\n\n");
                sb.append("以下技能与你的能力相关，执行时参考：\n");
                sb.append(skillSection);
                sb.append("\n");
            }
        }

        sb.append("**重要规则:**\n");
        sb.append("- 只能调用上面列出的能力，不要调用未列出的操作\n");
        sb.append("- 需要审批的操作会自动提交审批，不会立即执行\n");
        sb.append("- 可以在一次回复中调用多个能力\n");
        sb.append("- 如果不需要调用能力，直接输出普通文本即可\n\n");

        // 注入 MCP 工具列表（如果 Agent 绑定了 MCP 工具）
        if (mcpService != null && projectId != null) {
            String mcpToolPrompt = mcpService.buildMcpToolPrompt(getRole(), projectId);
            if (!mcpToolPrompt.isEmpty()) {
                sb.append(mcpToolPrompt);
            }
        }

        return sb.toString();
    }

    /**
     * 构建能力列表 prompt（带缓存）
     * 使用 PromptCacheService 缓存结果，避免每次 sendMessage 都重建
     * 能力变更时缓存自动失效
     *
     * @return 缓存的能力 prompt
     */
    protected String buildCapabilityPromptCached() {
        if (promptCacheService == null || capabilityRegistry == null) {
            return buildCapabilityPrompt();
        }

        String projectId = currentProject != null ? currentProject.getId() : null;
        String cached = promptCacheService.getCachedCapabilityPrompt(getRole(), projectId);
        if (cached != null) {
            return cached;
        }

        // 缓存未命中，重建并缓存
        String prompt = buildCapabilityPrompt();
        if (!prompt.isEmpty()) {
            promptCacheService.cacheCapabilityPrompt(getRole(), projectId, prompt);
        }
        return prompt;
    }

    /**
     * 构建系统指令（推理深度 + 思维模式）
     * 合并推理深度和思维模式为一个指令，减少重复注入
     *
     * @return 系统指令字符串
     */
    protected String buildSystemInstruction() {
        int depth = definition.getReasoningDepth();
        int mode = definition.getThinkingMode();

        StringBuilder sb = new StringBuilder();

        // 推理深度指令
        String depthInstruction = AgentDefinition.getReasoningDepthInstruction(depth);
        if (depthInstruction != null && !depthInstruction.isEmpty()) {
            sb.append(depthInstruction);
        }

        // 思维模式指令
        String modeInstruction = AgentDefinition.getThinkingModeInstruction(mode);
        if (modeInstruction != null && !modeInstruction.isEmpty()) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append(modeInstruction);
        }

        return sb.toString();
    }

    /**
     * 构建协作上下文注入 prompt（带缓存）
     * 使用 PromptCacheService 缓存结果，避免每次 sendMessage 都重建
     *
     * @return 缓存的协作上下文 prompt
     */
    protected String buildCollaborationContextPromptCached() {
        if (promptCacheService == null) {
            return buildCollaborationContextPrompt();
        }

        String projectId = getProjectId();
        if (projectId == null) {
            return buildCollaborationContextPrompt();
        }

        String cached = promptCacheService.getCachedCollaborationContext(getRole(), projectId);
        if (cached != null) {
            return cached;
        }

        // 缓存未命中，重建并缓存
        String prompt = buildCollaborationContextPrompt();
        if (!prompt.isEmpty()) {
            promptCacheService.cacheCollaborationContext(getRole(), projectId, prompt);
        }
        return prompt;
    }

    /**
     * 构建协作上下文注入 prompt
     * 整合三大协作增强：
     * 1. 团队状态（ProjectBoard）
     * 2. 游戏开发知识（CollaborationEnhancer）
     * 3. 团队成员最新产出（任务依赖传递）
     *
     * @return 协作上下文 prompt，如果不可用或未启用则返回空字符串
     */
    protected String buildCollaborationContextPrompt() {
        if (projectBoard == null) return "";

        String projectId = getProjectId();
        if (projectId == null) return "";

        try {
            StringBuilder fullContext = new StringBuilder();

            // 1. 团队状态（原有功能）
            String teamContext = projectBoard.buildAgentContext(projectId, getId(), eventBus);
            if (teamContext != null && !teamContext.isEmpty()) {
                fullContext.append(teamContext);
            }

            // 2. 游戏开发知识注入
            if (collaborationEnhancer != null) {
                String gameKnowledge = collaborationEnhancer.buildGameKnowledgeContext(projectId, getRole());
                if (!gameKnowledge.isEmpty()) {
                    fullContext.append(gameKnowledge);
                }

                // 3. 团队成员最新产出（任务依赖传递）
                String taskOutput = collaborationEnhancer.buildTaskOutputContext(projectId, getId());
                if (!taskOutput.isEmpty()) {
                    fullContext.append(taskOutput);
                }
            }

            if (fullContext.length() == 0) return "";

            // 从 SystemConfigService 读取最大长度
            int maxLength = 2000;
            if (configService != null) {
                maxLength = configService.getInt(
                    com.chengxun.gamemaker.config.SystemConstants.CONTEXT_COLLABORATION_MAX_LENGTH, 2000);
            }

            String result = fullContext.toString();
            if (result.length() > maxLength) {
                result = result.substring(0, maxLength) + "\n...(协作上下文已截断)";
            }

            return result;
        } catch (Exception e) {
            log.debug("Failed to build collaboration context: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 构建 MCP 配置（带缓存）
     * 使用 PromptCacheService 缓存 MCP 配置，避免每次调用都重新生成
     *
     * @return MCP 配置 JSON，不可用返回 null
     */
    protected String buildMcpConfigCached() {
        if (mcpService == null) return null;

        String projectId = getProjectId();
        if (projectId == null) return null;

        // 尝试从缓存获取
        if (promptCacheService != null) {
            String cached = promptCacheService.getCachedMcpConfig(getRole(), projectId);
            if (cached != null) return cached;
        }

        // 缓存未命中，重新生成
        String mcpConfig = mcpService.generateMcpConfig(getRole(), projectId);
        if (mcpConfig != null && promptCacheService != null) {
            promptCacheService.cacheMcpConfig(getRole(), projectId, mcpConfig);
        }
        return mcpConfig;
    }

    /**
     * 处理 Claude 输出中的能力调用
     * 解析输出文本，提取结构化的能力调用并执行
     *
     * @param response Claude 的原始输出
     * @return 处理后的响应文本（包含能力执行结果）
     */
    protected String processCapabilityActions(String response) {
        if (capabilityOutputParser == null || capabilityExecutionEngine == null) {
            return response;
        }

        try {
            // 解析输出
            CapabilityOutputParser.ParseResult parseResult = capabilityOutputParser.parse(response);

            if (!parseResult.hasActions()) {
                return response; // 没有能力调用，返回原始响应
            }

            log.info("Parsed {} capability actions from agent {} output",
                parseResult.getActions().size(), getId());

            // 执行能力调用
            List<CapabilityResult> results = capabilityExecutionEngine.executeCalls(this, parseResult.getActions());

            // 构建结果摘要
            StringBuilder resultSummary = new StringBuilder();
            if (parseResult.getResponse() != null && !parseResult.getResponse().isEmpty()) {
                resultSummary.append(parseResult.getResponse());
            }

            boolean hasActions = false;
            for (int i = 0; i < results.size(); i++) {
                CapabilityResult result = results.get(i);
                CapabilityCall call = parseResult.getActions().get(i);

                if (!hasActions) {
                    resultSummary.append("\n\n---\n### 能力调用结果\n\n");
                    hasActions = true;
                }

                resultSummary.append(String.format("- **%s**: %s",
                    call.getCapabilityName(), result.getStatus()));
                if (result.isSuccess() && result.getData() != null) {
                    resultSummary.append(" — ").append(result.getData());
                } else if (result.isPendingApproval()) {
                    resultSummary.append(" — 已提交审批 (ID: ").append(result.getApprovalRequestId()).append(")");
                } else if (result.isFailed()) {
                    resultSummary.append(" — ").append(result.getError());
                }
                resultSummary.append("\n");
            }

            return resultSummary.toString();

        } catch (Exception e) {
            log.error("Error processing capability actions for agent {}", getId(), e);
            return response; // 出错时返回原始响应
        }
    }

    // ===== SKILL 相关 =====

    /** 技能 prompt 注入最大长度 */
    private static final int MAX_SKILL_PROMPT_LENGTH = 800;

    protected String buildSkillPrompt(String taskDescription) {
        String projectId = currentProject != null ? currentProject.getId() : null;
        List<Skill> matchedSkills = skillManager.matchSkills(taskDescription, projectId);
        if (matchedSkills.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("\n## 可用技能\n\n");

        // 只注入摘要（名称+描述+触发词），不注入完整 prompt 模板
        // 完整 prompt 模板在技能实际被触发时才加载
        for (Skill skill : matchedSkills) {
            sb.append(String.format("- **%s**: %s", skill.getName(),
                skill.getDescription() != null ? skill.getDescription() : ""));
            if (skill.getTriggerPattern() != null) {
                sb.append(String.format(" (触发: %s)", skill.getTriggerPattern()));
            }
            sb.append("\n");
            skillManager.recordSkillUsage(skill.getId(), projectId);
        }

        sb.append("\n技能详情请参考项目技能目录。\n");

        // 截断保护
        String result = sb.toString();
        if (result.length() > MAX_SKILL_PROMPT_LENGTH) {
            result = result.substring(0, MAX_SKILL_PROMPT_LENGTH) + "...";
        }
        return result;
    }

    protected void tryLearnSkill(String taskDescription, String result) {
        if (currentProject == null) return;

        String summaryPrompt = String.format(
            "请分析以下任务执行过程，如果发现可复用的模式，请提取为技能。\n\n" +
            "任务描述：%s\n\n" +
            "执行结果：%s\n\n" +
            "如果这是一个可复用的技能，请用以下格式输出：\n" +
            "SKILL_NAME: 技能名称\n" +
            "SKILL_DESC: 技能描述\n" +
            "SKILL_TRIGGER: 触发关键词（逗号分隔）\n" +
            "SKILL_PROMPT: 技能的prompt模板\n\n" +
            "如果没有发现可复用模式，请输出：NO_SKILL",
            taskDescription, result.length() > 500 ? result.substring(0, 500) + "..." : result
        );

        String response = sendMessage(summaryPrompt);
        if (response != null && !response.contains("NO_SKILL")) {
            parseAndSaveLearnedSkill(response);
        }
    }

    protected void parseAndSaveLearnedSkill(String response) {
        try {
            String name = extractField(response, "SKILL_NAME");
            String desc = extractField(response, "SKILL_DESC");
            String trigger = extractField(response, "SKILL_TRIGGER");
            String prompt = extractField(response, "SKILL_PROMPT");

            if (name != null && prompt != null && currentProject != null) {
                Skill skill = Skill.builder()
                    .id("learned-" + System.currentTimeMillis())
                    .name(name)
                    .description(desc != null ? desc : name)
                    .category("learned")
                    .triggerPattern(trigger != null ? trigger : name)
                    .prompt(prompt)
                    .build();

                skillManager.saveLearnedSkill(skill, getId(), currentProject.getId(), currentProject.getSkillsDir());
                log.info("Learned new skill: {} for project: {}", skill.getId(), currentProject.getId());

                // 将学习到的技能推送到知识进化服务，促进全局知识共享
                if (knowledgeEvolutionService != null) {
                    knowledgeEvolutionService.promoteLearnedSkill(skill, getId());
                }
            }
        } catch (Exception e) {
            log.debug("Failed to parse learned skill: {}", e.getMessage());
        }
    }

    private String extractField(String text, String fieldName) {
        String pattern = fieldName + ":";
        int start = text.indexOf(pattern);
        if (start == -1) return null;
        start += pattern.length();
        int end = text.indexOf("\n", start);
        if (end == -1) end = text.length();
        return text.substring(start, end).trim();
    }

    // ===== 知识库集成 =====

    /**
     * 查询与任务相关的知识
     * 从知识进化服务中搜索相关的模式、解决方案和最佳实践
     *
     * @param taskDescription 任务描述
     * @return 格式化的知识文本，可直接注入 prompt；服务不可用时返回空字符串
     */
    protected String queryRelevantKnowledge(String taskDescription) {
        if (knowledgeEvolutionService == null || taskDescription == null) {
            return "";
        }
        return knowledgeEvolutionService.queryRelevantKnowledge(taskDescription, getRole());
    }

    /**
     * 将任务完成信息反馈到知识库
     * 成功的任务提取模式，失败的任务记录解决方案
     *
     * @param taskDescription 任务描述
     * @param result 执行结果
     * @param success 是否成功
     */
    protected void feedTaskCompletionToKB(String taskDescription, String result, boolean success) {
        if (knowledgeEvolutionService == null) return;
        try {
            knowledgeEvolutionService.extractInsightsFromTaskCompletion(getId(), taskDescription, result, success);
        } catch (Exception e) {
            log.debug("Failed to feed task completion to knowledge base: {}", e.getMessage());
        }
    }

    // ===== Token 消耗记录 =====

    /**
     * 记录精确 Token 消耗
     * 使用 Claude CLI 返回的真实 token 数据，同时更新 Token 绑定记录和预算服务
     *
     * @param inputTokens  输入 Token 数（精确值或估算值）
     * @param outputTokens 输出 Token 数（精确值或估算值）
     */
    private void recordTokenUsagePrecise(long inputTokens, long outputTokens) {
        long totalTokens = inputTokens + outputTokens;
        if (totalTokens <= 0) return;

        // 更新 Token 使用记录（池化模式：按角色查找最佳 Token）
        if (apiTokenService != null) {
            try {
                com.chengxun.gamemaker.web.entity.ApiToken token =
                    apiTokenService.findBestTokenForRole(getRole());
                if (token == null && agentManagerRef != null) {
                    token = autoAssignTokenIfNeeded();
                }
                if (token != null) {
                    apiTokenService.recordUsage(token.getId(), totalTokens);
                }
            } catch (Exception e) {
                log.debug("Failed to record token usage: {}", e.getMessage());
            }
        }

        // 更新 Token 预算服务
        if (tokenBudgetService != null) {
            tokenBudgetService.recordUsage(getId(), inputTokens, outputTokens);
        }
    }

    /**
     * 基于 token 计数触发自动压缩
     * 当会话累计 token 超过阈值时，自动执行上下文压缩
     */
    private void autoCompactByTokenCount() {
        if (contextCompactor == null || currentProject == null) return;

        try {
            // 从 SystemConfigService 读取阈值
            long threshold = 80000;
            if (configService != null) {
                threshold = configService.getInt(
                    com.chengxun.gamemaker.config.SystemConstants.CONTEXT_COMPACT_TOKEN_THRESHOLD, 80000);
            }

            if (sessionTokenCount >= threshold) {
                log.info("Session token count ({}) exceeded threshold ({}), auto-compacting for agent: {}",
                    sessionTokenCount, threshold, getId());
                compactContext();
                sessionTokenCount = 0;
            }
        } catch (Exception e) {
            log.debug("Auto compact by token count failed: {}", e.getMessage());
        }
    }

    /**
     * 自动从 Token 池获取可用 Token（池化模式，不做排他绑定）
     * 多个 Agent 可以共享同一个 Token 池
     *
     * @return 匹配的 Token，如果没有可用 Token 则返回 null
     */
    private com.chengxun.gamemaker.web.entity.ApiToken autoAssignTokenIfNeeded() {
        if (apiTokenService == null) return null;

        try {
            com.chengxun.gamemaker.web.entity.ApiToken token =
                apiTokenService.findBestTokenForRole(getRole());
            if (token == null) return null;

            // 池化模式：将 Token 配置应用到 Agent Definition，记录使用的 Token ID
            if (token.getApiKey() != null) definition.setApiKey(token.getApiKey());
            if (token.getApiUrl() != null) definition.setApiUrl(token.getApiUrl());
            if (token.getModel() != null) definition.setModel(token.getModel());
            definition.setAssignedTokenId(token.getId());

            log.info("Auto-allocated token '{}' to agent {} (role: {}, pool mode)",
                token.getName(), getId(), getRole());
            return token;
        } catch (Exception e) {
            log.debug("Failed to auto-allocate token for agent {}: {}", getId(), e.getMessage());
            return null;
        }
    }

    // ===== 权限错误检测和告警 =====

    /**
     * 权限错误关键词模式（不区分大小写）
     * 只匹配真正的文件系统权限错误，避免 AI 正常讨论代码时误触发
     */
    private static final java.util.List<String> PERMISSION_ERROR_PATTERNS = java.util.List.of(
        "permission denied",
        "access denied",
        "operation not permitted",
        "read-only file system",
        "eacces",
        "eperm"
    );

    /**
     * 文件系统错误上下文关键词（用于二次确认，必须同时包含上述模式之一 + 以下关键词之一）
     * 防止 "permission denied" 出现在非文件操作上下文中误报
     */
    private static final java.util.List<String> FILE_CONTEXT_KEYWORDS = java.util.List.of(
        "file",
        "directory",
        "dir",
        "path",
        "folder",
        "/home/",
        "/var/",
        "/tmp/",
        "mkdir",
        "rm ",
        "cp ",
        "mv ",
        "chmod",
        "chown"
    );

    /** 上次权限错误通知时间（防止频繁通知） */
    private volatile long lastPermissionErrorNotifyTime = 0;

    /** 权限错误通知最小间隔（毫秒）：10分钟 */
    private static final long PERMISSION_ERROR_NOTIFY_COOLDOWN = 600000;

    /**
     * 检测 AI 响应中的文件权限错误，自动通知管理员并创建告警
     *
     * 检测逻辑：
     * 1. 扫描 AI 响应文本中的权限错误关键词
     * 2. 如果检测到，记录错误日志
     * 3. 通知管理员（带冷却时间，防止频繁通知）
     * 4. 创建系统告警
     *
     * @param response AI 的响应文本
     */
    protected void detectAndNotifyPermissionErrors(String response) {
        if (response == null || response.isEmpty()) return;

        String lowerResponse = response.toLowerCase();

        // 第一层：匹配权限错误关键词
        boolean hasPermissionError = PERMISSION_ERROR_PATTERNS.stream()
            .anyMatch(lowerResponse::contains);
        if (!hasPermissionError) return;

        // 第二层：确认是文件系统上下文（而非数据库、网络等其他场景）
        boolean hasFileContext = FILE_CONTEXT_KEYWORDS.stream()
            .anyMatch(lowerResponse::contains);
        if (!hasFileContext) {
            log.debug("检测到权限关键词但非文件系统上下文，跳过: {}", getId());
            return;
        }

        // 冷却检查：避免同一 Agent 短时间内重复通知
        long now = System.currentTimeMillis();
        if (now - lastPermissionErrorNotifyTime < PERMISSION_ERROR_NOTIFY_COOLDOWN) {
            log.debug("权限错误通知冷却中，跳过: {}", getId());
            return;
        }
        lastPermissionErrorNotifyTime = now;

        String projectName = currentProject != null ? currentProject.getName() : "全局";
        String errorSummary = String.format("Agent [%s] 在项目 [%s] 中遇到文件权限错误", getName(), projectName);

        // 提取错误上下文（包含错误信息的行）
        String errorContext = extractPermissionErrorContext(response);

        // 记录 Agent 错误日志
        logError("FILE_PERMISSION_ERROR", errorSummary, errorContext);

        log.warn("检测到文件权限错误: agent={}, project={}", getId(), projectName);

        // 通知管理员
        notifyAdminPermissionError(errorSummary, errorContext);

        // 创建系统告警
        createPermissionErrorAlert(errorSummary, errorContext);
    }

    /**
     * 从 AI 响应中提取权限错误的上下文信息
     */
    private String extractPermissionErrorContext(String response) {
        StringBuilder context = new StringBuilder();
        String[] lines = response.split("\n");
        for (String line : lines) {
            String lowerLine = line.toLowerCase();
            boolean hasPermissionKeyword = PERMISSION_ERROR_PATTERNS.stream().anyMatch(lowerLine::contains);
            boolean hasFileContext = FILE_CONTEXT_KEYWORDS.stream().anyMatch(lowerLine::contains);
            if (hasPermissionKeyword && hasFileContext) {
                context.append(line.trim()).append("\n");
            }
        }
        // 最多返回 500 字符
        String result = context.toString().trim();
        return result.length() > 500 ? result.substring(0, 500) + "..." : result;
    }

    /**
     * 通知管理员文件权限错误
     * 通过 NotificationService 发送多渠道通知（站内信、邮件、飞书、钉钉）
     */
    private void notifyAdminPermissionError(String summary, String detail) {
        if (notificationService == null) return;

        try {
            java.util.Map<String, String> variables = java.util.Map.of(
                "agentName", getName(),
                "agentRole", getRole(),
                "projectName", currentProject != null ? currentProject.getName() : "全局",
                "errorDetail", detail.length() > 200 ? detail.substring(0, 200) + "..." : detail,
                "title", "Agent 文件权限错误",
                "content", summary + "\n\n" + detail,
                "ruleName", "Agent 文件权限错误",
                "priorityDesc", "警告",
                "triggerValue", "权限不足",
                "thresholdValue", "需要可写权限"
            );
            notificationService.notifyAdmins("alert", "ALERT",
                variables, com.chengxun.gamemaker.web.entity.Notification.NotificationType.WARNING);
        } catch (Exception e) {
            log.warn("发送权限错误通知失败: {}", e.getMessage());
        }
    }

    /**
     * 创建系统告警
     */
    private void createPermissionErrorAlert(String summary, String detail) {
        if (alertService == null) return;
        try {
            com.chengxun.gamemaker.web.entity.AlertRecord alert = new com.chengxun.gamemaker.web.entity.AlertRecord();
            alert.setTitle(summary);
            alert.setDetail(detail);
            alert.setPriority("WARNING");
            alert.setStatus("PENDING");
            alert.setMetric("AGENT_PERMISSION_ERROR");
            alert.setAgentId(getId());
            alert.setAgentName(getName());
            alert.setProjectId(getProjectId());
            alert.setRuleName("Agent 文件权限错误检测");
            alert.setCreatedAt(java.time.LocalDateTime.now());
            alertService.saveAlert(alert);
        } catch (Exception e) {
            log.debug("创建告警失败: {}", e.getMessage());
        }
    }
}
