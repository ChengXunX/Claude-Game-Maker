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

    protected AgentContext agentContext;
    protected GameProject currentProject;
    protected String currentConversationId;

    protected final ConcurrentLinkedQueue<AgentMessage> pendingMessages = new ConcurrentLinkedQueue<>();
    protected final List<TaskAssignment> tasks = new CopyOnWriteArrayList<>();

    protected volatile boolean busy = false;
    protected volatile boolean running = false;

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
                agentLogService.logAsync(getId(), getName(), action, level, summary, detail,
                    projectId, null, null, null);
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
        log.info("Initializing agent: {} ({})", getName(), getId());
        logInfo("AGENT_STARTED", "Agent 开始初始化");

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

        // 开始新的对话
        startNewConversation();

        log.info("Agent initialized: {} (runtimeId={}) for project: {}", getName(), getId(),
            currentProject != null ? currentProject.getName() : "global");
        logInfo("AGENT_STARTED", "Agent 初始化完成，项目: " + (currentProject != null ? currentProject.getName() : "全局"));
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
        log.info("Agent started: {} (runtimeId={})", getName(), getId());
        logInfo("AGENT_STARTED", "Agent 已启动");
    }

    @Override
    public void stop() {
        logInfo("AGENT_STOPPED", "Agent 正在停止");
        running = false;
        messageBus.unregisterAgent(getId());
        saveContext();
        createSnapshot();
        log.info("Agent stopped: {}", getName());
        logInfo("AGENT_STOPPED", "Agent 已停止");
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
        if (!running || busy) {
            return;
        }

        busy = true;
        long startTime = System.currentTimeMillis();
        try {
            logInfo("TASK_STARTED", "开始执行工作任务");
            processPendingMessages();
            doWork();
            long duration = System.currentTimeMillis() - startTime;
            logInfo("TASK_COMPLETED", "工作任务完成，耗时: " + duration + "ms");
        } catch (Exception e) {
            log.error("Error in agent work: {}", getName(), e);
            logError("TASK_FAILED", "工作任务执行失败: " + e.getMessage(), null);
        } finally {
            busy = false;
        }
    }

    protected abstract void doWork();

    protected void processPendingMessages() {
        AgentMessage message;
        while ((message = pendingMessages.poll()) != null) {
            try {
                handleMessage(message);
            } catch (Exception e) {
                log.error("Error processing message for agent {}: {}", getName(), e.getMessage());
            }
        }
    }

    protected abstract void handleMessage(AgentMessage message);

    @Override
    public String sendMessage(String message) {
        if (!running) {
            return "Agent is not running";
        }

        busy = true;
        long startTime = System.currentTimeMillis();
        try {
            // 记录 AI 调用开始
            logInfo("AI_CALL", "开始调用 AI，消息长度: " + message.length());

            // 注入推理深度指令
            int depth = definition.getReasoningDepth();
            String depthInstruction = AgentDefinition.getReasoningDepthInstruction(depth);

            // 注入能力列表 prompt（如果能力系统可用）
            String capabilityPrompt = buildCapabilityPrompt();
            String enhancedMessage = depthInstruction + "\n\n" + capabilityPrompt + "\n\n" + message;

            saveConversationMessage("user", message);

            // 生成 MCP 配置
            String mcpConfig = null;
            if (mcpService != null) {
                String projectId = getProjectId();
                if (projectId != null) {
                    mcpConfig = mcpService.generateMcpConfig(getRole(), projectId);
                }
            }

            String sessionId = definition.getSessionId();
            String response = cliEngine.sendMessage(
                getId(),
                sessionId,
                enhancedMessage,
                definition.getWorkDir(),
                definition.getApiKey(),
                definition.getApiUrl(),
                definition.getModel(),
                mcpConfig
            );

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
            logAiCall("AI 调用完成，响应长度: " + (response != null ? response.length() : 0), duration);

            // 解析 Claude 输出中的能力调用
            response = processCapabilityActions(response);

            saveConversationMessage("assistant", response);

            agentContext.addWorkingMemory("last_message", message.length() > 100 ? message.substring(0, 100) : message);
            agentContext.addWorkingMemory("last_response", response.length() > 100 ? response.substring(0, 100) : response);

            return response;
        } finally {
            busy = false;
        }
    }

    /** 对话消息保存的同步锁 */
    private final Object conversationLock = new Object();

    protected void saveConversationMessage(String role, String content) {
        synchronized (conversationLock) {
            ContextManager.ConversationMessage msg = new ContextManager.ConversationMessage(role, content);
            msg.setTaskId(agentContext.getCurrentTaskId());

            List<ContextManager.ConversationMessage> messages = contextManager.loadConversation(getId(), currentProject, currentConversationId);
            messages.add(msg);
            contextManager.saveConversation(getId(), currentProject, currentConversationId, messages);
        }
    }

    @Override
    public void receiveMessage(AgentMessage message) {
        pendingMessages.offer(message);
        log.debug("Agent {} received message from {}", getName(), message.getFromAgentId());
        logInfo("MESSAGE_RECEIVED", "收到来自 " + message.getFromAgentId() + " 的消息，类型: " + message.getType());
    }

    @Override
    public List<AgentMessage> getPendingMessages() {
        return new ArrayList<>(pendingMessages);
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
        log.info("Task assigned to {}: {}", getName(), task.getTitle());
        logTask("TASK_RECEIVED", "接收任务: " + task.getTitle(), task.getId());
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

    @Override
    public void saveContext() {
        agentContext.setWorkDir(definition.getWorkDir());
        agentContext.setSessionId(definition.getSessionId());
        contextManager.saveContext(agentContext, currentProject);
    }

    @Override
    public void loadContext() {
        agentContext = contextManager.loadContext(getId(), currentProject);
        if (agentContext != null) {
            definition.setSessionId(agentContext.getSessionId());
            definition.setWorkDir(agentContext.getWorkDir());
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
        }
    }

    public String loadKnowledge(String key) {
        if (currentProject != null) {
            return memoryManager.loadMemory(currentProject, getId(), "knowledge", key);
        }
        return null;
    }

    public void saveExperience(String key, String value) {
        if (currentProject != null) {
            memoryManager.saveMemory(currentProject, getId(), "experiences", key, value);
            agentContext.addLearnedPattern(key);
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

        // 开始新的对话
        startNewConversation();

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

        // 注入关联技能的 prompt 内容
        if (!allRelatedSkillIds.isEmpty() && skillManager != null) {
            StringBuilder skillSection = new StringBuilder();
            for (String skillId : allRelatedSkillIds) {
                Skill skill = skillManager.getGlobalSkill(skillId);
                if (skill == null && projectId != null) {
                    Map<String, Skill> projectSkills = skillManager.getProjectSkills(projectId);
                    skill = projectSkills.get(skillId);
                }
                if (skill != null && skill.getPrompt() != null && !skill.getPrompt().isEmpty()) {
                    skillSection.append(String.format("\n#### 技能: %s\n", skill.getName()));
                    if (skill.getDescription() != null) {
                        skillSection.append(skill.getDescription()).append("\n\n");
                    }
                    skillSection.append(skill.getPrompt()).append("\n");
                }
            }

            if (skillSection.length() > 0) {
                sb.append("### 关联技能参考\n\n");
                sb.append("以下技能与你的能力相关，请在执行时参考：\n");
                sb.append(skillSection);
                sb.append("\n");
            }
        }

        sb.append("**重要规则:**\n");
        sb.append("- 只能调用上面列出的能力，不要调用未列出的操作\n");
        sb.append("- 需要审批的操作会自动提交审批，不会立即执行\n");
        sb.append("- 可以在一次回复中调用多个能力\n");
        sb.append("- 如果不需要调用能力，直接输出普通文本即可\n\n");

        return sb.toString();
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

    protected String buildSkillPrompt(String taskDescription) {
        String projectId = currentProject != null ? currentProject.getId() : null;
        List<Skill> matchedSkills = skillManager.matchSkills(taskDescription, projectId);
        if (matchedSkills.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("\n## 可用技能\n\n");
        sb.append("以下是与当前任务相关的技能，请参考使用：\n\n");

        for (Skill skill : matchedSkills) {
            sb.append(skill.toPromptSection()).append("\n");
            skillManager.recordSkillUsage(skill.getId(), projectId);
        }

        return sb.toString();
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
}
