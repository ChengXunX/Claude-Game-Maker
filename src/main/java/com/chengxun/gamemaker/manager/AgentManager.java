package com.chengxun.gamemaker.manager;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.agent.BaseAgent;
import com.chengxun.gamemaker.agent.GenericAgent;
import com.chengxun.gamemaker.agent.GitCommitAgent;
import com.chengxun.gamemaker.agent.NumericalPlannerAgent;
import com.chengxun.gamemaker.agent.ProducerAgent;
import com.chengxun.gamemaker.agent.ServerDevAgent;
import com.chengxun.gamemaker.agent.SystemPlannerAgent;
import com.chengxun.gamemaker.agent.UiDevAgent;
import com.chengxun.gamemaker.agent.VerificationAgent;
import com.chengxun.gamemaker.config.AppConfig;
import org.springframework.context.annotation.Lazy;
import com.chengxun.gamemaker.engine.ClaudeCliEngine;
import com.chengxun.gamemaker.engine.MessageBus;
import com.chengxun.gamemaker.feishu.FeishuBotService;
import com.chengxun.gamemaker.model.AgentDefinition;
import com.chengxun.gamemaker.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 管理器
 * 负责 Agent 的创建、销毁、调度和生命周期管理
 *
 * 核心设计：项目级隔离
 * - 存储结构：projectId -> agentRole -> Agent（嵌套 ConcurrentHashMap）
 * - 每个项目内的 Agent 完全独立，不同项目同角色 Agent 互不影响
 * - Agent 运行时 ID 格式：projectId:agentRole（全局唯一）
 * - 管理员可跨项目查看所有 Agent
 *
 * @author chengxun
 * @since 1.0.0
 */
@Component
public class AgentManager {

    private static final Logger log = LoggerFactory.getLogger(AgentManager.class);

    private final AppConfig appConfig;
    private final ClaudeCliEngine cliEngine;
    private final MessageBus messageBus;
    private final ContextManager contextManager;
    private final MemoryManager memoryManager;
    private final SkillManager skillManager;
    private final ProjectManager projectManager;
    private final FeishuBotService feishuService;
    private final ContextCompactor contextCompactor;
    private final CapabilityRegistry capabilityRegistry;
    private final CapabilityOutputParser capabilityOutputParser;
    private final CapabilityExecutionEngine capabilityExecutionEngine;
    private final CapabilityInterceptor capabilityInterceptor;
    private final ContextMonitor contextMonitor;

    @Autowired(required = false)
    private GoalService goalService;

    @Autowired(required = false)
    private DistributedLockService lockService;

    @Autowired(required = false)
    private com.chengxun.gamemaker.service.McpService mcpService;

    @Autowired(required = false)
    private com.chengxun.gamemaker.web.service.AgentLogService agentLogService;

    @Autowired(required = false)
    private com.chengxun.gamemaker.service.GameTemplateService gameTemplateService;

    @Autowired(required = false)
    private com.chengxun.gamemaker.service.GameRuntimeVerifier gameRuntimeVerifier;

    @Autowired(required = false)
    private com.chengxun.gamemaker.web.service.WorkflowEngine workflowEngine;

    @Autowired(required = false)
    private com.chengxun.gamemaker.web.service.ApprovalService approvalService;

    @Autowired(required = false)
    private com.chengxun.gamemaker.service.ApprovalCallbackService approvalCallbackService;

    @Autowired(required = false)
    private com.chengxun.gamemaker.service.PerformanceManagementService performanceManagementService;

    @Autowired(required = false)
    private com.chengxun.gamemaker.service.GameDesignReviewService gameDesignReviewService;

    @Autowired(required = false)
    private com.chengxun.gamemaker.service.KnowledgeEvolutionService knowledgeEvolutionService;

    @Autowired(required = false)
    private com.chengxun.gamemaker.web.service.NotificationService notificationService;

    @Autowired(required = false)
    private com.chengxun.gamemaker.web.service.AlertService alertService;

    @Lazy
    @Autowired(required = false)
    private com.chengxun.gamemaker.web.service.ApiTokenService apiTokenService;

    @Autowired(required = false)
    private com.chengxun.gamemaker.service.AgentInterventionService interventionService;

    @Autowired(required = false)
    private com.chengxun.gamemaker.service.ProjectBoard projectBoard;

    @Autowired(required = false)
    private com.chengxun.gamemaker.service.EventBus eventBus;

    @Autowired(required = false)
    private com.chengxun.gamemaker.service.TokenBudgetService tokenBudgetService;

    @Autowired(required = false)
    private com.chengxun.gamemaker.service.PromptCacheService promptCacheService;

    @Autowired(required = false)
    private com.chengxun.gamemaker.service.MessageDeduplicator messageDeduplicator;

    @Autowired(required = false)
    private com.chengxun.gamemaker.service.CollaborationEnhancer collaborationEnhancer;

    @Autowired(required = false)
    private com.chengxun.gamemaker.service.VersionIterationService versionIterationService;

    @Autowired(required = false)
    private com.chengxun.gamemaker.web.service.SystemConfigService systemConfigService;

    @Autowired(required = false)
    private com.chengxun.gamemaker.service.PlayerExperienceAnalyzer playerExperienceAnalyzer;

    @Autowired(required = false)
    private com.chengxun.gamemaker.engine.OpenAiCompatibleEngine openAiEngine;

    @Autowired(required = false)
    private com.chengxun.gamemaker.service.TaskRebalanceService taskRebalanceService;

    @Autowired(required = false)
    private com.chengxun.gamemaker.service.CollaborationMetricsService collaborationMetricsService;

    @Autowired(required = false)
    private com.chengxun.gamemaker.service.ProjectDiscussionService projectDiscussionService;

    @Autowired(required = false)
    private com.chengxun.gamemaker.service.DynamicCapabilityService dynamicCapabilityService;

    @Autowired(required = false)
    private com.chengxun.gamemaker.service.ResourceAssetService resourceAssetService;

    @Autowired
    private com.chengxun.gamemaker.agent.RolePromptLibrary rolePromptLibrary;

    /** Agent 调度器（延迟注入，避免循环依赖）- 用于事件驱动的消息处理 */
    @Lazy
    @Autowired(required = false)
    private com.chengxun.gamemaker.service.AgentScheduler agentScheduler;

    /** Agent 资源配额缓存 */
    private final ConcurrentHashMap<String, AgentResourceQuota> quotaMap = new ConcurrentHashMap<>();

    /**
     * 项目级 Agent 存储
     * 外层 key: projectId
     * 内层 key: agentRole（如 producer、server-dev）
     * 保证不同项目的 Agent 完全隔离
     */
    private final Map<String, Map<String, Agent>> projectAgents = new ConcurrentHashMap<>();

    public AgentManager(AppConfig appConfig,
                       ClaudeCliEngine cliEngine,
                       MessageBus messageBus,
                       ContextManager contextManager,
                       MemoryManager memoryManager,
                       SkillManager skillManager,
                       ProjectManager projectManager,
                       FeishuBotService feishuService,
                       ContextCompactor contextCompactor,
                       CapabilityRegistry capabilityRegistry,
                       CapabilityOutputParser capabilityOutputParser,
                       @Lazy CapabilityExecutionEngine capabilityExecutionEngine,
                       CapabilityInterceptor capabilityInterceptor,
                       @Lazy ContextMonitor contextMonitor) {
        this.appConfig = appConfig;
        this.cliEngine = cliEngine;
        this.messageBus = messageBus;
        this.contextManager = contextManager;
        this.memoryManager = memoryManager;
        this.skillManager = skillManager;
        this.projectManager = projectManager;
        this.feishuService = feishuService;
        this.contextCompactor = contextCompactor;
        this.capabilityRegistry = capabilityRegistry;
        this.capabilityOutputParser = capabilityOutputParser;
        this.capabilityExecutionEngine = capabilityExecutionEngine;
        this.capabilityInterceptor = capabilityInterceptor;
        this.contextMonitor = contextMonitor;
    }

    // ===== 创建 Agent =====

    /**
     * 创建制作人 Agent（项目级）
     *
     * @param definition Agent 定义（必须包含 projectId）
     * @return 创建的 Agent 实例
     */
    public synchronized Agent createProducerAgent(AgentDefinition definition) {
        String projectId = definition.getProjectId();
        String agentRole = definition.getRole();

        if (projectId == null) {
            log.error("Cannot create producer agent without projectId");
            throw new IllegalArgumentException("projectId is required for agent creation");
        }

        // 检查项目内是否已存在同角色 Agent
        Map<String, Agent> projectAgentMap = projectAgents.computeIfAbsent(projectId, k -> new ConcurrentHashMap<>());
        if (projectAgentMap.containsKey(agentRole)) {
            log.warn("Agent already exists in project {}: role={}", projectId, agentRole);
            return projectAgentMap.get(agentRole);
        }

        // 制作人使用最高推理深度（全面分析）
        definition.setReasoningDepth(4);
        // 制作人思维模式：平衡（需要协调+规划+适度创新）
        if (definition.getThinkingMode() == 3) { // 仅在未自定义时设置默认值
            definition.setThinkingMode(AgentDefinition.getDefaultThinkingMode("producer"));
        }

        ProducerAgent producer = new ProducerAgent(definition, cliEngine, messageBus,
                contextManager, memoryManager, skillManager, projectManager, this, feishuService);
        injectCapabilityServices(producer);
        if (goalService != null) {
            producer.setGoalService(goalService);
        }
        if (gameTemplateService != null) {
            producer.setGameTemplateService(gameTemplateService);
        }
        if (gameRuntimeVerifier != null) {
            producer.setGameRuntimeVerifier(gameRuntimeVerifier);
        }
        if (workflowEngine != null) {
            producer.setWorkflowEngine(workflowEngine);
        }
        if (approvalService != null) {
            producer.setApprovalService(approvalService);
        }
        if (approvalCallbackService != null) {
            producer.setApprovalCallbackService(approvalCallbackService);
        }
        if (performanceManagementService != null) {
            producer.setPerformanceManagementService(performanceManagementService);
        }
        if (gameDesignReviewService != null) {
            producer.setGameDesignReviewService(gameDesignReviewService);
        }
        if (playerExperienceAnalyzer != null) {
            producer.setPlayerExperienceAnalyzer(playerExperienceAnalyzer);
        }
        if (taskRebalanceService != null) {
            producer.setTaskRebalanceService(taskRebalanceService);
        }
        if (collaborationMetricsService != null) {
            producer.setCollaborationMetricsService(collaborationMetricsService);
        }
        if (projectDiscussionService != null) {
            producer.setProjectDiscussionService(projectDiscussionService);
        }
        if (dynamicCapabilityService != null) {
            producer.setDynamicCapabilityService(dynamicCapabilityService);
        }
        if (resourceAssetService != null) {
            producer.setResourceAssetService(resourceAssetService);
        }
        if (versionIterationService != null) {
            producer.setVersionIterationService(versionIterationService);
        }
        producer.initialize();
        producer.start();
        projectAgentMap.put(agentRole, producer);

        log.info("Producer agent created: {} (runtimeId={}) for project: {}",
            definition.getName(), definition.getEffectiveId(), projectId);
        return producer;
    }

    /**
     * 创建 Agent（项目级）
     * 根据 AgentDefinition 中的 role 创建对应类型的 Agent
     *
     * @param definition Agent 定义（必须包含 projectId）
     * @return 创建的 Agent 实例
     */
    public synchronized Agent createAgent(AgentDefinition definition) {
        if ("producer".equals(definition.getRole())) {
            return createProducerAgent(definition);
        }

        String projectId = definition.getProjectId();
        String agentRole = definition.getRole();

        if (projectId == null) {
            log.error("Cannot create agent without projectId");
            throw new IllegalArgumentException("projectId is required for agent creation");
        }

        // 检查项目内是否已存在同角色 Agent
        Map<String, Agent> projectAgentMap = projectAgents.computeIfAbsent(projectId, k -> new ConcurrentHashMap<>());
        if (projectAgentMap.containsKey(agentRole)) {
            log.warn("Agent already exists in project {}: role={}", projectId, agentRole);
            return projectAgentMap.get(agentRole);
        }

        // 设置角色默认思维模式（仅在未自定义时）
        if (definition.getThinkingMode() == 3) {
            definition.setThinkingMode(AgentDefinition.getDefaultThinkingMode(agentRole));
        }

        Agent agent;
        switch (agentRole) {
            case "server-dev" -> {
                agent = new ServerDevAgent(definition, cliEngine, messageBus, contextManager, memoryManager, skillManager, projectManager);
            }
            case "git-commit" -> {
                agent = new GitCommitAgent(definition, cliEngine, messageBus, contextManager, memoryManager, skillManager, projectManager);
            }
            case "system-planner" -> {
                SystemPlannerAgent planner = new SystemPlannerAgent(definition, cliEngine, messageBus, contextManager, memoryManager, skillManager, projectManager);
                if (playerExperienceAnalyzer != null) {
                    planner.setPlayerExperienceAnalyzer(playerExperienceAnalyzer);
                }
                agent = planner;
            }
            case "numerical-planner" -> {
                agent = new NumericalPlannerAgent(definition, cliEngine, messageBus, contextManager, memoryManager, skillManager, projectManager);
            }
            case "ui-dev" -> {
                agent = new UiDevAgent(definition, cliEngine, messageBus, contextManager, memoryManager, skillManager, projectManager);
            }
            case "verifier" -> {
                agent = new VerificationAgent(definition, cliEngine, messageBus, contextManager, memoryManager, skillManager, projectManager);
            }
            // 以下角色使用 GenericAgent，行为由角色提示词驱动
            case "audio-dev", "tech-artist", "tester", "security-expert",
                 "data-analyst", "product-manager", "localization",
                 "ai-engineer", "performance-engineer", "narrative-planner",
                 "level-design", "devops" -> {
                agent = new GenericAgent(definition, cliEngine, messageBus, contextManager, memoryManager, skillManager, projectManager, rolePromptLibrary);
            }
            // 自定义角色：使用 GenericAgent，提示词来自数据库或文件
            default -> {
                log.info("未知角色 '{}'，使用 GenericAgent（提示词驱动）", agentRole);
                agent = new GenericAgent(definition, cliEngine, messageBus, contextManager, memoryManager, skillManager, projectManager, rolePromptLibrary);
            }
        }

        // 注入上下文压缩服务和能力系统
        if (agent instanceof BaseAgent baseAgent) {
            injectCapabilityServices(baseAgent);
        }

        // 注入游戏运行时验证服务
        if (gameRuntimeVerifier != null) {
            if (agent instanceof ServerDevAgent serverDevAgent) {
                serverDevAgent.setGameRuntimeVerifier(gameRuntimeVerifier);
            }
            if (agent instanceof VerificationAgent verificationAgent) {
                verificationAgent.setGameRuntimeVerifier(gameRuntimeVerifier);
            }
            if (agent instanceof GenericAgent genericAgent) {
                genericAgent.setGameRuntimeVerifier(gameRuntimeVerifier);
            }
        }

        // 注入目标服务（用于 GenericAgent 更新里程碑任务状态）
        if (goalService != null) {
            if (agent instanceof GenericAgent genericAgent) {
                genericAgent.setGoalService(goalService);
            }
        }

        // 注入游戏设计审查服务
        if (gameDesignReviewService != null) {
            if (agent instanceof VerificationAgent verificationAgent) {
                verificationAgent.setGameDesignReviewService(gameDesignReviewService);
            }
        }

        // 注入事件总线
        if (eventBus != null) {
            if (agent instanceof VerificationAgent verificationAgent) {
                verificationAgent.setEventBus(eventBus);
            }
        }

        // 注入项目看板
        if (projectBoard != null) {
            if (agent instanceof VerificationAgent verificationAgent) {
                verificationAgent.setProjectBoard(projectBoard);
            }
        }

        // 注入通知服务
        if (notificationService != null) {
            if (agent instanceof VerificationAgent verificationAgent) {
                verificationAgent.setNotificationService(notificationService);
            }
        }

        // 注入Agent管理器（用于督查功能）
        if (agent instanceof VerificationAgent verificationAgent) {
            verificationAgent.setAgentManager(this);
        }

        // 注入版本迭代服务（用于迭代监督）
        if (versionIterationService != null) {
            if (agent instanceof VerificationAgent verificationAgent) {
                verificationAgent.setVersionIterationService(versionIterationService);
            }
        }

        // 注入系统配置服务（用于督查规则配置）
        if (systemConfigService != null) {
            if (agent instanceof VerificationAgent verificationAgent) {
                verificationAgent.setSystemConfigService(systemConfigService);
            }
        }

        // 注入任务重平衡服务（用于负载监控）
        if (taskRebalanceService != null) {
            if (agent instanceof VerificationAgent verificationAgent) {
                verificationAgent.setTaskRebalanceService(taskRebalanceService);
            }
        }

        // 注入协作效率度量服务（用于效率指标）
        if (collaborationMetricsService != null) {
            if (agent instanceof VerificationAgent verificationAgent) {
                verificationAgent.setCollaborationMetricsService(collaborationMetricsService);
            }
        }

        agent.initialize();

        // 自动分配 Token：如果 Agent 没有配置 API Key，尝试从 Token 池中匹配
        autoAssignToken(agent, agentRole);

        // 设置消息到达回调：收到消息时立即触发 Agent 工作，不再等待下一个定时周期
        if (agent instanceof BaseAgent baseAgent && agentScheduler != null) {
            baseAgent.setMessageArrivedCallback(() -> {
                agentScheduler.onMessageArrived(agent);
            });
        }

        agent.start();
        projectAgentMap.put(agentRole, agent);

        log.info("Agent created: {} (runtimeId={}) for project: {}",
            definition.getName(), definition.getEffectiveId(), projectId);
        return agent;
    }

    // ===== 查询 Agent =====

    /**
     * 通过运行时 ID 获取 Agent
     * 运行时 ID 格式：projectId:agentRole
     *
     * @param runtimeId 运行时 ID
     * @return Agent 实例，不存在返回 null
     */
    public Agent getAgent(String runtimeId) {
        if (runtimeId == null) return null;

        // 解析运行时 ID: projectId:agentRole
        int lastColon = runtimeId.lastIndexOf(':');
        if (lastColon > 0) {
            String projectId = runtimeId.substring(0, lastColon);
            String agentRole = runtimeId.substring(lastColon + 1);
            return getAgent(projectId, agentRole);
        }

        // 兼容旧格式：遍历所有项目查找（多项目同角色时返回第一个找到的，不确定）
        log.debug("Agent lookup by legacy ID '{}', consider using runtime ID format 'projectId:agentRole'", runtimeId);
        for (Map<String, Agent> projectMap : projectAgents.values()) {
            Agent agent = projectMap.get(runtimeId);
            if (agent != null) return agent;
        }
        return null;
    }

    /**
     * 获取指定项目的指定角色 Agent
     *
     * @param projectId 项目 ID
     * @param agentRole Agent 角色（如 producer、server-dev）
     * @return Agent 实例，不存在返回 null
     */
    public Agent getAgent(String projectId, String agentRole) {
        Map<String, Agent> projectMap = projectAgents.get(projectId);
        if (projectMap == null) return null;
        return projectMap.get(agentRole);
    }

    /**
     * 获取所有 Agent（管理员视角，跨项目）
     *
     * @return 所有项目的 Agent 列表
     */
    public List<Agent> getAllAgents() {
        List<Agent> result = new ArrayList<>();
        for (Map<String, Agent> projectMap : projectAgents.values()) {
            result.addAll(projectMap.values());
        }
        return result;
    }

    /**
     * 获取指定项目的所有 Agent
     *
     * @param projectId 项目 ID
     * @return 该项目下的 Agent 列表
     */
    public List<Agent> getAgentsByProject(String projectId) {
        Map<String, Agent> projectMap = projectAgents.get(projectId);
        if (projectMap == null) return Collections.emptyList();
        return new ArrayList<>(projectMap.values());
    }

    /**
     * 按角色查找 Agent（跨项目）
     *
     * @param role Agent 角色
     * @return 所有项目中该角色的 Agent 列表
     */
    public List<Agent> getAgentsByRole(String role) {
        List<Agent> result = new ArrayList<>();
        for (Map<String, Agent> projectMap : projectAgents.values()) {
            Agent agent = projectMap.get(role);
            if (agent != null) result.add(agent);
        }
        return result;
    }

    /**
     * 获取所有已注册的项目 ID
     */
    public List<String> getRegisteredProjectIds() {
        return new ArrayList<>(projectAgents.keySet());
    }

    // ===== 移除 Agent =====

    /**
     * 通过运行时 ID 移除 Agent
     *
     * @param runtimeId 运行时 ID（projectId:agentRole）
     */
    public void removeAgent(String runtimeId) {
        if (runtimeId == null) return;

        int lastColon = runtimeId.lastIndexOf(':');
        if (lastColon > 0) {
            String projectId = runtimeId.substring(0, lastColon);
            String agentRole = runtimeId.substring(lastColon + 1);
            removeAgent(projectId, agentRole);
        }
    }

    /**
     * 移除指定项目的指定角色 Agent
     *
     * @param projectId 项目 ID
     * @param agentRole Agent 角色
     */
    public void removeAgent(String projectId, String agentRole) {
        Map<String, Agent> projectMap = projectAgents.get(projectId);
        if (projectMap == null) return;

        Agent agent = projectMap.remove(agentRole);
        if (agent != null) {
            agent.stop();
            messageBus.unregisterAgent(agent.getId());
            // 清理资源配额缓存
            quotaMap.remove(agent.getId());
            log.info("Agent removed: {} from project {}", agentRole, projectId);
        }

        // 如果项目下没有 Agent 了，清理空 Map
        if (projectMap.isEmpty()) {
            projectAgents.remove(projectId);
        }
    }

    /**
     * 为 Agent 注入能力系统、监控、MCP 和日志相关服务
     */
    private void injectCapabilityServices(BaseAgent agent) {
        agent.setContextCompactor(contextCompactor);
        agent.setCapabilityRegistry(capabilityRegistry);
        agent.setCapabilityOutputParser(capabilityOutputParser);
        agent.setCapabilityExecutionEngine(capabilityExecutionEngine);
        agent.setCapabilityInterceptor(capabilityInterceptor);
        agent.setContextMonitor(contextMonitor);
        agent.setMcpService(mcpService);
        agent.setAgentLogService(agentLogService);
        agent.setKnowledgeEvolutionService(knowledgeEvolutionService);
        agent.setNotificationService(notificationService);
        agent.setAlertService(alertService);
        agent.setApiTokenService(apiTokenService);
        agent.setAgentManagerRef(this);
        agent.setInterventionService(interventionService);
        agent.setProjectBoard(projectBoard);
        agent.setEventBus(eventBus);
        agent.setTokenBudgetService(tokenBudgetService);
        agent.setPromptCacheService(promptCacheService);
        agent.setMessageDeduplicator(messageDeduplicator);
        agent.setConfigService(systemConfigService);
        if (openAiEngine != null) {
            agent.setOpenAiEngine(openAiEngine);
        }
        agent.setCollaborationEnhancer(collaborationEnhancer);
    }

    /**
     * 自动为 Agent 分配 Token
     * 当 Agent 没有配置 API Key 时，从 Token 池中查找匹配角色的最佳 Token 并绑定
     *
     * @param agent Agent 实例
     * @param agentRole Agent 角色
     */
    private void autoAssignToken(Agent agent, String agentRole) {
        if (apiTokenService == null) return;

        // 已有 API Key 则跳过
        if (agent.getDefinition().getApiKey() != null && !agent.getDefinition().getApiKey().isEmpty()) {
            return;
        }

        try {
            com.chengxun.gamemaker.web.entity.ApiToken token = apiTokenService.findBestTokenForRole(agentRole);
            if (token == null) {
                log.debug("No available token for role: {}", agentRole);
                return;
            }

            // 池化模式：将 Token 的 API 配置应用到 Agent，记录使用的 Token ID
            agent.getDefinition().setApiKey(token.getApiKey());
            agent.getDefinition().setApiUrl(token.getApiUrl());
            agent.getDefinition().setModel(token.getModel());
            agent.getDefinition().setAssignedTokenId(token.getId());

            // 【修复】使用 saveApiConfigToContext 保存 API 配置到 context.json
            if (agent instanceof com.chengxun.gamemaker.agent.BaseAgent baseAgent) {
                baseAgent.saveApiConfigToContext(token.getApiKey(), token.getApiUrl(), token.getModel());
            } else {
                agent.saveContext();
            }

            log.info("Auto-allocated token '{}' to agent {} (role: {}, pool mode)",
                token.getName(), agent.getId(), agentRole);
        } catch (Exception e) {
            log.warn("Failed to auto-allocate token for agent {}: {}", agent.getId(), e.getMessage());
        }
    }

    // ===== 停止管理 =====

    /**
     * 停止指定项目的所有 Agent
     *
     * @param projectId 项目 ID
     */
    public void stopProject(String projectId) {
        Map<String, Agent> projectMap = projectAgents.get(projectId);
        if (projectMap == null) return;

        projectMap.values().forEach(Agent::stop);
        log.info("All agents stopped for project: {}", projectId);
    }

    /**
     * 移除指定项目的所有 Agent（停止 + 清理内存）
     * 项目删除时调用，确保 Agent 不会继续运行和消耗 Token
     *
     * @param projectId 项目 ID
     */
    public void removeProjectAgents(String projectId) {
        Map<String, Agent> projectMap = projectAgents.remove(projectId);
        if (projectMap == null) return;

        for (Map.Entry<String, Agent> entry : projectMap.entrySet()) {
            Agent agent = entry.getValue();
            try {
                agent.stop();
                messageBus.unregisterAgent(agent.getId());
                quotaMap.remove(agent.getId());
            } catch (Exception e) {
                log.warn("Failed to stop agent {} during project removal: {}",
                    agent.getId(), e.getMessage());
            }
        }
        log.info("All agents removed for project: {} ({} agents)", projectId, projectMap.size());
    }

    /**
     * 停止所有项目的所有 Agent（系统关闭时调用）
     */
    public void stopAll() {
        for (Map.Entry<String, Map<String, Agent>> entry : projectAgents.entrySet()) {
            entry.getValue().values().forEach(Agent::stop);
            log.info("Agents stopped for project: {}", entry.getKey());
        }
        log.info("All agents stopped across all projects");
    }

    // ===== 资源配额 =====

    /**
     * 获取 Agent 的资源配额
     */
    public AgentResourceQuota getResourceQuota(String agentId) {
        return quotaMap.computeIfAbsent(agentId, AgentResourceQuota::defaultQuota);
    }

    /**
     * 检查 Agent 是否可以接受新任务
     */
    public boolean canAcceptTask(String agentId) {
        return getResourceQuota(agentId).canAcceptTask();
    }

    /**
     * 记录 Agent 任务开始
     */
    public boolean startAgentTask(String agentId) {
        return getResourceQuota(agentId).startTask();
    }

    /**
     * 记录 Agent 任务完成
     */
    public void completeAgentTask(String agentId) {
        getResourceQuota(agentId).completeTask();
    }

    // ===== 状态查询 =====

    /**
     * 获取 Agent 状态（通过运行时 ID）
     *
     * @param runtimeId 运行时 ID（projectId:agentRole）
     * @return Agent 状态信息，不存在返回 null
     */
    public Map<String, Object> getAgentStatus(String runtimeId) {
        Agent agent = getAgent(runtimeId);
        if (agent == null) {
            return null;
        }
        return buildAgentStatus(agent);
    }

    /**
     * 获取指定项目的指定角色 Agent 状态
     */
    public Map<String, Object> getAgentStatus(String projectId, String agentRole) {
        Agent agent = getAgent(projectId, agentRole);
        if (agent == null) {
            return null;
        }
        return buildAgentStatus(agent);
    }

    /**
     * 获取指定项目所有 Agent 的状态
     */
    public List<Map<String, Object>> getProjectAgentStatuses(String projectId) {
        List<Map<String, Object>> statuses = new ArrayList<>();
        List<Agent> agents = getAgentsByProject(projectId);
        for (Agent agent : agents) {
            statuses.add(buildAgentStatus(agent));
        }
        return statuses;
    }

    private Map<String, Object> buildAgentStatus(Agent agent) {
        Map<String, Object> status = new java.util.HashMap<>();
        status.put("id", agent.getId());
        status.put("runtimeId", agent.getId());
        status.put("name", agent.getName());
        status.put("role", agent.getRole());
        status.put("busy", agent.isBusy());
        status.put("alive", agent.isAlive());
        // 使用 try-catch 避免任务数据加载失败导致整个 API 失败
        try {
            status.put("taskCount", agent.getTasks() != null ? agent.getTasks().size() : 0);
        } catch (Exception e) {
            status.put("taskCount", 0);
        }
        status.put("reasoningDepth", agent.getDefinition().getReasoningDepth());
        status.put("thinkingMode", agent.getDefinition().getThinkingMode());

        if (agent instanceof BaseAgent baseAgent) {
            status.put("conversationId", baseAgent.getCurrentConversationId());
            if (baseAgent.getAgentContext() != null) {
                status.put("workingMemorySize", baseAgent.getAgentContext().getWorkingMemory().size());
                status.put("learnedPatternsCount", baseAgent.getAgentContext().getLearnedPatterns().size());
            }

            if (baseAgent.getCurrentProject() != null) {
                status.put("projectId", baseAgent.getCurrentProject().getId());
                status.put("projectName", baseAgent.getCurrentProject().getName());
                status.put("workDir", baseAgent.getCurrentProject().getWorkDir());
            }
        }

        // Token 信息：根据 assignedTokenId 查询当前使用的 Token
        if (apiTokenService != null) {
            try {
                Long tokenId = agent.getDefinition().getAssignedTokenId();
                com.chengxun.gamemaker.web.entity.ApiToken token = null;
                if (tokenId != null) {
                    token = apiTokenService.getTokenById(tokenId);
                    // Token 可能已被删除或禁用，回退到按角色查找
                    if (token == null || !token.isActive()) {
                        token = apiTokenService.findBestTokenForRole(agent.getRole());
                    }
                } else {
                    token = apiTokenService.findBestTokenForRole(agent.getRole());
                }
                if (token != null) {
                    status.put("tokenId", token.getId());
                    status.put("tokenName", token.getName());
                    status.put("tokenModel", token.getModel());
                    status.put("tokenApiUrl", token.getApiUrl());
                    status.put("tokenMaskedKey", token.getMaskedApiKey());
                } else {
                    status.put("tokenId", null);
                    status.put("tokenName", null);
                }
            } catch (Exception e) {
                log.debug("Failed to get token info for agent {}: {}", agent.getId(), e.getMessage());
                status.put("tokenId", null);
                status.put("tokenName", null);
            }
        }

        return status;
    }

    /**
     * 设置 Agent 的推理深度（全局生效）
     *
     * @param projectId 项目 ID
     * @param agentRole Agent 角色
     * @param depth 推理深度 (1-5)
     * @return 是否设置成功
     */
    public boolean setReasoningDepth(String projectId, String agentRole, int depth) {
        Agent agent = getAgent(projectId, agentRole);
        if (agent == null) {
            log.warn("Agent not found for setting reasoning depth: {}:{}", projectId, agentRole);
            return false;
        }

        // 验证深度范围
        int validDepth = Math.max(1, Math.min(5, depth));
        agent.getDefinition().setReasoningDepth(validDepth);

        // 持久化到上下文
        if (agent instanceof com.chengxun.gamemaker.agent.BaseAgent baseAgent) {
            baseAgent.saveContext();
        }

        log.info("Set reasoning depth for agent {}:{} to {}", projectId, agentRole, validDepth);
        return true;
    }

    /**
     * 获取 Agent 的推理深度
     *
     * @param projectId 项目 ID
     * @param agentRole Agent 角色
     * @return 推理深度，Agent 不存在返回 -1
     */
    public int getReasoningDepth(String projectId, String agentRole) {
        Agent agent = getAgent(projectId, agentRole);
        if (agent == null) {
            return -1;
        }
        return agent.getDefinition().getReasoningDepth();
    }
}
