package com.chengxun.gamemaker.agent;

import com.chengxun.gamemaker.engine.ClaudeCliEngine;
import com.chengxun.gamemaker.engine.MessageBus;
import com.chengxun.gamemaker.feishu.FeishuBotService;
import com.chengxun.gamemaker.manager.*;
import com.chengxun.gamemaker.model.*;
import com.chengxun.gamemaker.service.ApprovalCallbackService;
import com.chengxun.gamemaker.service.GameTemplateService;
import com.chengxun.gamemaker.service.GoalService;
import com.chengxun.gamemaker.service.PerformanceManagementService;
import com.chengxun.gamemaker.web.entity.ApprovalRequest;
import com.chengxun.gamemaker.web.entity.DismissalRequest;
import com.chengxun.gamemaker.web.entity.PerformanceReview;
import com.chengxun.gamemaker.web.service.ApprovalService;
import com.chengxun.gamemaker.web.service.WorkflowEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class ProducerAgent extends BaseAgent {

    private static final Logger log = LoggerFactory.getLogger(ProducerAgent.class);

    private final AgentManager agentManager;
    private final FeishuBotService feishuService;

    /** 目标管理服务（延迟注入） */
    @Autowired(required = false)
    private GoalService goalService;

    /** 游戏模板服务（延迟注入） */
    private GameTemplateService gameTemplateService;

    /** 工作流引擎（延迟注入） */
    @Autowired(required = false)
    private WorkflowEngine workflowEngine;

    /** 审批回调服务（延迟注入） */
    @Autowired(required = false)
    private ApprovalCallbackService approvalCallbackService;

    /** 审批服务（延迟注入） */
    @Autowired(required = false)
    private ApprovalService approvalService;

    /** 绩效管理服务（延迟注入） */
    @Autowired(required = false)
    private PerformanceManagementService performanceManagementService;

    /** 当前暂停的里程碑 ID（等待审批） */
    private String pendingApprovalMilestoneId;

    /** 当前暂停的工作流实例 ID */
    private String pendingWorkflowInstanceId;

    public ProducerAgent(AgentDefinition definition,
                        ClaudeCliEngine cliEngine,
                        MessageBus messageBus,
                        ContextManager contextManager,
                        MemoryManager memoryManager,
                        SkillManager skillManager,
                        ProjectManager projectManager,
                        AgentManager agentManager,
                        FeishuBotService feishuService) {
        super(definition, cliEngine, messageBus, contextManager, memoryManager, skillManager, projectManager);
        this.agentManager = agentManager;
        this.feishuService = feishuService;
    }

    /**
     * 设置目标管理服务
     */
    public void setGoalService(GoalService goalService) {
        this.goalService = goalService;
    }

    /**
     * 设置游戏模板服务
     */
    public void setGameTemplateService(GameTemplateService gameTemplateService) {
        this.gameTemplateService = gameTemplateService;
    }

    /**
     * 设置工作流引擎
     */
    public void setWorkflowEngine(WorkflowEngine workflowEngine) {
        this.workflowEngine = workflowEngine;
    }

    /**
     * 设置审批服务
     */
    public void setApprovalService(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    /**
     * 设置审批回调服务
     */
    public void setApprovalCallbackService(ApprovalCallbackService approvalCallbackService) {
        this.approvalCallbackService = approvalCallbackService;
    }

    /**
     * 设置绩效管理服务
     */
    public void setPerformanceManagementService(PerformanceManagementService performanceManagementService) {
        this.performanceManagementService = performanceManagementService;
    }

    @Override
    protected void doWork() {
        log.info("Producer working...");

        // 1. 收集项目上下文
        String context = buildWorkContext();

        // 2. 构建工作 prompt（包含上下文 + 能力引导）
        String prompt = buildWorkPrompt(context);

        // 3. 调用 AI — sendMessage 自动注入能力 prompt、MCP 配置、技能
        //    AI 的响应会通过 processCapabilityActions 自动执行能力调用
        String response = sendMessage(prompt);

        // 4. 硬编码兜底：如果 AI 未处理关键逻辑，自动触发
        ensureCriticalActions();

        // 5. 处理审批和通知
        processApprovals();
        reportToUser(context);

        // 6. 保存项目状态
        saveProjectStatus(context);
    }

    /**
     * 构建工作上下文
     * 收集项目信息、团队状态、目标进度、游戏模板等，供 AI 决策使用
     *
     * @return 格式化的上下文文本
     */
    private String buildWorkContext() {
        StringBuilder ctx = new StringBuilder();

        // 项目信息
        if (currentProject != null) {
            ctx.append("## 项目信息\n");
            ctx.append("- 名称: ").append(currentProject.getName()).append("\n");
            ctx.append("- 工作目录: ").append(currentProject.getWorkDir()).append("\n");
            ctx.append("- 目标: ").append(currentProject.hasGoal() ? currentProject.getGoal() : "未设置").append("\n");
            ctx.append("- 目标状态: ").append(currentProject.getGoalStatus()).append("\n");
            ctx.append("- 进度: ").append(currentProject.getGoalProgress()).append("%\n\n");

            // 游戏模板匹配
            if (gameTemplateService != null && currentProject.hasGoal()) {
                List<GameTemplateService.GameTemplate> matched = gameTemplateService.matchTemplates(currentProject.getGoal());
                if (!matched.isEmpty()) {
                    GameTemplateService.GameTemplate best = matched.get(0);
                    ctx.append("## 匹配的游戏模板\n");
                    ctx.append("- 模板: ").append(best.getName()).append("\n");
                    ctx.append("- 描述: ").append(best.getDescription()).append("\n");
                    if (best.getContent() != null && !best.getContent().isEmpty()) {
                        String content = best.getContent();
                        ctx.append("- 模板内容:\n").append(content.length() > 2000 ? content.substring(0, 2000) + "..." : content).append("\n");
                    }
                    ctx.append("\n");
                }
            }
        }

        // 团队状态
        String projectId = getProjectId();
        if (projectId != null) {
            List<Agent> agents = agentManager.getAgentsByProject(projectId);
            ctx.append("## 团队成员\n");
            if (agents.isEmpty()) {
                ctx.append("（暂无团队成员）\n");
            } else {
                for (Agent agent : agents) {
                    ctx.append(String.format("- %s (%s): %s\n",
                        agent.getName(), agent.getRole(), agent.isBusy() ? "忙碌" : "空闲"));
                }
            }
            ctx.append("\n");
        }

        // 目标里程碑进度
        if (currentProject != null && currentProject.hasGoal() && goalService != null) {
            GameProject.GoalMilestone next = goalService.getNextExecutableMilestone(projectId);
            if (next != null) {
                ctx.append("## 下一个可执行里程碑\n");
                ctx.append("- 标题: ").append(next.getTitle()).append("\n");
                ctx.append("- 描述: ").append(next.getDescription() != null ? next.getDescription() : "无").append("\n");
                ctx.append("- 负责角色: ").append(next.getAssignedAgentRole()).append("\n");
                ctx.append("- 状态: ").append(next.getStatus()).append("\n\n");
            }
        }

        // 待处理审批
        List<AgentMessage> approvals = pendingMessages.stream()
            .filter(m -> m.getType() == AgentMessage.MessageType.APPROVAL)
            .collect(Collectors.toList());
        if (!approvals.isEmpty()) {
            ctx.append("## 待处理审批\n");
            for (AgentMessage approval : approvals) {
                ctx.append("- 来自 ").append(approval.getFromAgentId()).append(": ").append(approval.getContent()).append("\n");
            }
            ctx.append("\n");
        }

        // 知识库洞察（从全局知识库中查询与项目相关的知识）
        if (knowledgeEvolutionService != null && currentProject != null) {
            String query = currentProject.hasGoal() ? currentProject.getGoal() : currentProject.getName();
            String insights = knowledgeEvolutionService.queryRelevantKnowledge(query, "producer");
            if (!insights.isEmpty()) {
                ctx.append("## 知识库洞察\n\n");
                ctx.append(insights);
                ctx.append("\n");
            }
        }

        return ctx.toString();
    }

    /**
     * 构建工作 prompt
     * 基于上下文引导 AI 使用能力系统做出决策
     *
     * @param context 项目上下文
     * @return 发送给 AI 的 prompt
     */
    private String buildWorkPrompt(String context) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是项目制作人，负责协调整个游戏开发项目。你拥有最高决策权，可以根据项目需要：\n");
        prompt.append("- 自由招聘任何角色的团队成员\n");
        prompt.append("- 选择或创建工作流模板\n");
        prompt.append("- 分解目标并分配任务\n");
        prompt.append("- 管理项目进度和风险\n\n");

        prompt.append("## 当前项目状态\n\n");
        prompt.append(context);

        prompt.append("## 你的核心职责\n\n");
        prompt.append("### 1. 目标管理\n");
        prompt.append("- 如果项目还没有目标，使用 setProjectGoal 能力设定目标\n");
        prompt.append("- 如果项目有目标但没有里程碑，使用 decomposeGoal 能力分解目标\n");
        prompt.append("- 目标分解时，为每个里程碑指定负责人角色和依赖关系\n\n");

        prompt.append("### 2. 团队管理\n");
        prompt.append("- 根据里程碑需要，发起招聘审批请求\n");
        prompt.append("- 你可以请求招聘任意角色，包括自定义角色\n");
        prompt.append("- 招聘需要管理员审批，审批通过后自动创建 Agent\n\n");

        prompt.append("### 3. 工作流管理\n");
        prompt.append("- 使用 selectWorkflow 能力选择合适的工作流模板\n");
        prompt.append("- 如果没有合适的工作流，使用 createWorkflow 能力创建新工作流\n");
        prompt.append("- 使用 startWorkflow 能力启动工作流执行\n\n");

        prompt.append("### 4. 任务分配\n");
        prompt.append("- 如果有可执行的里程碑且有对应 Agent，使用 sendTaskToAgent 能力分配任务\n");
        prompt.append("- 也可以直接启动工作流，让工作流自动分配任务\n\n");

        prompt.append("### 5. 监控与优化\n");
        prompt.append("- 定期使用 getProjectStatus 能力检查项目进度\n");
        prompt.append("- 如果团队成员长期空闲，使用 optimizeAgentRole 能力优化\n");
        prompt.append("- 如果有风险，使用 alertRisk 能力预警\n\n");

        prompt.append("### 6. 动态团队评估\n");
        prompt.append("- 使用 evaluateProjectNeeds 能力分析项目当前需求\n");
        prompt.append("- 使用 evaluateAgent 能力对 Agent 进行绩效打分\n");
        prompt.append("- 使用 batchEvaluateTeam 能力批量评估团队\n");
        prompt.append("- 使用 requestDismissAgent 能力发起解雇低绩效 Agent\n");
        prompt.append("- 评估时需考虑：任务完成率、质量评分、响应速度\n\n");

        prompt.append("## 重要原则\n\n");
        prompt.append("1. 团队招聘必须经过管理员审批，你需要发起招聘审批请求\n");
        prompt.append("2. 工作流选择和创建不需要审批，但会通知管理员\n");
        prompt.append("3. 重大操作（如删除Agent、部署上线）需要审批，会暂停等待\n");
        prompt.append("4. 所有操作都会记录日志，管理员可以随时查看\n\n");

        prompt.append("请分析当前状态，选择最合适的操作执行。可以同时执行多个操作。\n");

        return prompt.toString();
    }

    /**
     * 硬编码兜底：确保关键业务逻辑不被遗漏
     * 如果 AI 未调用相关能力，自动触发
     */
    private void ensureCriticalActions() {
        String projectId = getProjectId();
        if (projectId == null || currentProject == null) return;

        // 如果项目有目标但状态为 NOT_STARTED，自动触发目标分解
        if (currentProject.hasGoal() && currentProject.getGoalStatus() == GameProject.GoalStatus.NOT_STARTED
            && goalService != null) {
            log.info("兜底：目标未开始，自动触发目标分解");
            decomposeGoal();
        }

        // 如果有可执行的里程碑，检查是否需要启动工作流或分配任务
        if (goalService != null && currentProject.getGoalStatus() == GameProject.GoalStatus.IN_PROGRESS) {
            GameProject.GoalMilestone next = goalService.getNextExecutableMilestone(projectId);
            if (next != null) {
                // 检查是否有对应 Agent
                String targetAgentId = projectId + ":" + next.getAssignedAgentRole();
                Agent targetAgent = agentManager.getAgent(targetAgentId);

                if (targetAgent != null && !targetAgent.isBusy()) {
                    // 有 Agent，检查是否需要启动工作流
                    if (shouldStartWorkflow(next)) {
                        log.info("兜底：有可执行里程碑，尝试启动工作流");
                        autoSelectAndStartWorkflow(next);
                    } else {
                        log.info("兜底：有可执行里程碑，自动触发任务分配");
                        checkAndAssignMilestoneTasks();
                    }
                }
            }

            // 定期检查团队绩效（每10个工作周期检查一次）
            if (System.currentTimeMillis() % 10 == 0) {
                log.info("定期检查：触发动态团队评估");
                checkTeamAndSuggestOptimization();
            }

            // 定期推动知识进化（每15个工作周期检查一次）
            if (System.currentTimeMillis() % 15 == 0) {
                log.info("定期检查：推动团队知识进化");
                evolveTeamKnowledge();
            }
        }
    }

    /**
     * 判断是否应该启动工作流
     * 如果里程碑有多个相关步骤，或者需要并行执行，建议使用工作流
     */
    private boolean shouldStartWorkflow(GameProject.GoalMilestone milestone) {
        // 如果里程碑有多个任务，建议使用工作流
        if (milestone.getTasks() != null && milestone.getTasks().size() > 2) {
            return true;
        }
        // 如果里程碑涉及多个角色协作，建议使用工作流
        // 这里可以根据实际情况扩展
        return false;
    }

    /**
     * 自动选择并启动工作流
     * 根据里程碑特征选择合适的工作流模板，如果没有则创建新的
     */
    private void autoSelectAndStartWorkflow(GameProject.GoalMilestone milestone) {
        String projectId = getProjectId();
        if (projectId == null || workflowEngine == null) return;

        // 选择工作流模板
        String templateId = selectWorkflowTemplate(milestone);
        if (templateId == null) {
            // 没有合适的工作流，创建新的
            templateId = createCustomWorkflow(milestone);
        }

        if (templateId != null) {
            // 启动工作流
            try {
                Map<String, String> params = new HashMap<>();
                params.put("milestoneId", milestone.getId());
                params.put("milestoneTitle", milestone.getTitle());
                params.put("goal", currentProject.getGoal());

                WorkflowEngine.WorkflowInstance instance = workflowEngine.startWorkflow(templateId, projectId, params);
                pendingWorkflowInstanceId = instance.getId();

                logInfo("WORKFLOW_STARTED", "工作流已启动: " + templateId + "，实例ID: " + instance.getId());
                sendNotificationToAdmin("WORKFLOW_STARTED",
                    String.format("项目 [%s] 已自动启动工作流\n模板: %s\n里程碑: %s",
                        currentProject.getName(), templateId, milestone.getTitle()));
            } catch (Exception e) {
                log.error("启动工作流失败: {}", e.getMessage());
                // 回退到直接任务分配
                checkAndAssignMilestoneTasks();
            }
        }
    }

    /**
     * 选择合适的工作流模板
     * 根据里程碑特征匹配最合适的模板
     */
    private String selectWorkflowTemplate(GameProject.GoalMilestone milestone) {
        if (workflowEngine == null) return null;

        List<WorkflowEngine.WorkflowTemplate> templates = workflowEngine.getAllTemplates();
        if (templates.isEmpty()) return null;

        String role = milestone.getAssignedAgentRole();
        String title = milestone.getTitle().toLowerCase();

        // 根据角色和标题特征匹配模板
        if (title.contains("设计") || title.contains("策划")) {
            return "planning-full";
        } else if (title.contains("测试") || title.contains("验证")) {
            return "minimal"; // 开发→测试→上线
        } else if (title.contains("部署") || title.contains("上线")) {
            return "hotfix"; // 紧急修复流程
        } else if ("server-dev".equals(role)) {
            return "server-only-dev";
        } else if ("client-dev".equals(role) || "ui-dev".equals(role)) {
            return "client-only-dev";
        }

        // 默认使用标准流程
        return "standard-game-dev";
    }

    /**
     * 创建自定义工作流
     * 当没有合适的工作流模板时，根据里程碑特征创建新的
     */
    private String createCustomWorkflow(GameProject.GoalMilestone milestone) {
        if (workflowEngine == null) return null;

        String templateId = "custom-" + milestone.getId();
        String role = milestone.getAssignedAgentRole();

        // 创建简单的工作流：分析→执行→测试
        List<WorkflowEngine.WorkflowStep> steps = new ArrayList<>();

        WorkflowEngine.WorkflowStep analyzeStep = new WorkflowEngine.WorkflowStep(
            "analyze", "需求分析", "system-planner",
            "分析任务需求: " + milestone.getTitle()
        );

        WorkflowEngine.WorkflowStep devStep = new WorkflowEngine.WorkflowStep(
            "develop", "任务执行", role,
            milestone.getDescription() != null ? milestone.getDescription() : milestone.getTitle()
        );
        devStep.addDependency("analyze");

        WorkflowEngine.WorkflowStep testStep = new WorkflowEngine.WorkflowStep(
            "test", "验证测试", "tester",
            "验证任务完成质量: " + milestone.getTitle()
        );
        testStep.addDependency("develop");

        steps.add(analyzeStep);
        steps.add(devStep);
        steps.add(testStep);

        WorkflowEngine.WorkflowTemplate template = workflowEngine.createTemplate(
            templateId,
            "自定义-" + milestone.getTitle(),
            "为里程碑自动创建的工作流: " + milestone.getTitle(),
            steps
        );

        logInfo("WORKFLOW_CREATED", "已创建自定义工作流: " + templateId);
        sendNotificationToAdmin("WORKFLOW_CREATED",
            String.format("已为里程碑 [%s] 创建自定义工作流\n模板ID: %s\n包含步骤: %s",
                milestone.getTitle(), templateId, steps.stream()
                    .map(WorkflowEngine.WorkflowStep::getName)
                    .collect(Collectors.joining(" → "))));

        return templateId;
    }

    /**
     * 驱动目标迭代 — 核心循环
     * 检查目标状态，分解任务，分配给 Agent，检查进度
     */
    private void driveGoalIteration() {
        String projectId = getProjectId();
        if (projectId == null) return;

        GameProject.GoalStatus goalStatus = currentProject.getGoalStatus();

        switch (goalStatus) {
            case NOT_STARTED -> {
                // 目标未开始，开始分解
                log.info("Goal not started, decomposing: {}", currentProject.getGoal());
                decomposeGoal();
            }
            case DECOMPOSING -> {
                // 正在分解中，等待完成
                log.info("Goal decomposing...");
            }
            case IN_PROGRESS -> {
                // 进行中，检查进度并分配任务
                checkAndAssignMilestoneTasks();
                checkGoalProgress();
            }
            case REVIEW -> {
                // 审查中，等待人工确认
                log.info("Goal in review, waiting for confirmation");
            }
            case PAUSED -> {
                log.info("Goal is paused");
            }
            case COMPLETED -> {
                log.info("Goal is completed");
            }
        }
    }

    /**
     * 分解目标为里程碑
     * 使用 AI 分析目标，生成里程碑和任务
     */
    private void decomposeGoal() {
        String projectId = getProjectId();
        if (projectId == null || goalService == null) return;

        // 标记为分解中
        currentProject.setGoalStatus(GameProject.GoalStatus.DECOMPOSING);
        projectManager.saveProjectConfig(currentProject);
        saveContext();

        String goalPrompt = String.format(
            "你是一个项目管理专家。请分析以下项目目标，将其分解为可执行的里程碑。\n\n" +
            "项目名称：%s\n" +
            "项目目标：%s\n" +
            "目标类型：%s\n\n" +
            "请按以下格式输出里程碑（每个里程碑一行）：\n" +
            "MILESTONE: 顺序号 | 标题 | 描述 | 负责角色(server-dev/client-dev/ui-dev/system-planner/numerical-planner/tester/git-commit) | 依赖的里程碑序号(逗号分隔,无依赖填0)\n\n" +
            "示例：\n" +
            "MILESTONE: 1 | 系统设计 | 设计游戏核心系统架构 | system-planner | 0\n" +
            "MILESTONE: 2 | 服务端开发 | 实现后端逻辑和API | server-dev | 1\n\n" +
            "请只输出 MILESTONE 行，不要有其他内容。",
            currentProject.getName(),
            currentProject.getGoal(),
            currentProject.getGoalType() != null ? currentProject.getGoalType().name() : "CUSTOM"
        );

        String response = sendMessage(goalPrompt);
        if (response == null || response.isEmpty()) {
            log.warn("Failed to decompose goal, will retry next cycle");
            currentProject.setGoalStatus(GameProject.GoalStatus.NOT_STARTED);
            return;
        }

        // 解析里程碑
        parseAndSaveMilestones(projectId, response);
    }

    /**
     * 解析 AI 返回的里程碑并保存
     * 容错处理：支持多种分隔符、缺失字段、格式变体
     * 依赖解析：先创建所有里程碑，再将序号依赖转换为里程碑 ID
     */
    private void parseAndSaveMilestones(String projectId, String response) {
        String[] lines = response.split("\n");
        int milestoneCount = 0;
        int autoOrder = 0;

        // 临时存储：序号 -> 里程碑对象（用于依赖解析）
        Map<Integer, GameProject.GoalMilestone> orderToMilestone = new java.util.LinkedHashMap<>();
        // 临时存储：序号 -> 依赖序号列表
        Map<Integer, List<Integer>> orderToDependencyOrders = new java.util.HashMap<>();

        // 第一遍：创建所有里程碑（不设置依赖）
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // 兼容多种格式：MILESTONE:、Milestone:、里程碑：
            String marker = null;
            if (line.toUpperCase().contains("MILESTONE:")) {
                marker = "MILESTONE:";
            } else if (line.contains("里程碑：") || line.contains("里程碑:")) {
                marker = line.contains("里程碑：") ? "里程碑：" : "里程碑:";
            }
            if (marker == null) continue;

            try {
                String content = line.substring(line.indexOf(marker) + marker.length()).trim();

                // 支持多种分隔符：|、，、,、Tab
                String[] parts = content.split("[|，,\t]+");
                if (parts.length < 2) {
                    log.warn("Milestone line too short, skipping: {}", line);
                    continue;
                }

                // 解析顺序号（如果有的话）
                int order = ++autoOrder;
                int titleIdx = 0;
                try {
                    order = Integer.parseInt(parts[0].trim());
                    titleIdx = 1;
                } catch (NumberFormatException e) {
                    // 第一个字段不是数字，说明没有顺序号
                }

                String title = parts[titleIdx].trim();
                if (title.isEmpty()) continue;

                String description = (parts.length > titleIdx + 1) ? parts[titleIdx + 1].trim() : title;
                String role = (parts.length > titleIdx + 2) ? normalizeRole(parts[titleIdx + 2].trim()) : "server-dev";

                // 解析依赖序号（暂存，后面转换）
                List<Integer> depOrders = new java.util.ArrayList<>();
                if (parts.length > titleIdx + 3) {
                    String deps = parts[titleIdx + 3].trim();
                    if (!deps.isEmpty() && !"0".equals(deps) && !"无".equals(deps) && !"none".equalsIgnoreCase(deps)) {
                        for (String dep : deps.split("[,，]")) {
                            String d = dep.trim().replaceAll("[^0-9]", "");
                            if (!d.isEmpty()) {
                                try {
                                    depOrders.add(Integer.parseInt(d));
                                } catch (NumberFormatException e) {
                                    // 忽略非数字
                                }
                            }
                        }
                    }
                }

                // 创建里程碑（不设置依赖）
                GameProject.GoalMilestone milestone = goalService.addMilestone(projectId, title, description, role, order, new java.util.ArrayList<>());
                orderToMilestone.put(order, milestone);
                orderToDependencyOrders.put(order, depOrders);

                milestoneCount++;
                log.info("Parsed milestone {}: {} ({})", order, title, role);

            } catch (Exception e) {
                log.warn("Failed to parse milestone line: {} - {}", line, e.getMessage());
            }
        }

        // 第二遍：将序号依赖转换为里程碑 ID
        for (Map.Entry<Integer, List<Integer>> entry : orderToDependencyOrders.entrySet()) {
            int order = entry.getKey();
            List<Integer> depOrders = entry.getValue();

            if (depOrders.isEmpty()) continue;

            GameProject.GoalMilestone milestone = orderToMilestone.get(order);
            if (milestone == null) continue;

            List<String> milestoneIds = new java.util.ArrayList<>();
            for (int depOrder : depOrders) {
                GameProject.GoalMilestone depMilestone = orderToMilestone.get(depOrder);
                if (depMilestone != null) {
                    milestoneIds.add(depMilestone.getId());
                } else {
                    log.warn("依赖的里程碑不存在: 序号 {}", depOrder);
                }
            }

            // 更新里程碑的依赖
            if (!milestoneIds.isEmpty()) {
                milestone.setDependencies(milestoneIds);
                log.info("里程碑 {} ({}) 依赖: {}", order, milestone.getTitle(), milestoneIds);
            }
        }

        if (milestoneCount > 0) {
            currentProject.setGoalStatus(GameProject.GoalStatus.IN_PROGRESS);
            log.info("Goal decomposed into {} milestones for project: {}", milestoneCount, projectId);
        } else {
            currentProject.setGoalStatus(GameProject.GoalStatus.NOT_STARTED);
            log.warn("No milestones parsed from response, will retry. Response: {}",
                response.length() > 200 ? response.substring(0, 200) + "..." : response);
        }

        projectManager.saveProjectConfig(currentProject);
        saveContext();
    }

    /**
     * 角色名称标准化（兼容中文和英文）
     */
    private String normalizeRole(String role) {
        return switch (role.toLowerCase()) {
            case "服务端", "后端", "server", "backend" -> "server-dev";
            case "客户端", "前端", "client", "frontend" -> "client-dev";
            case "ui", "美术", "界面" -> "ui-dev";
            case "策划", "系统策划", "planner", "system" -> "system-planner";
            case "数值", "数值策划", "numerical" -> "numerical-planner";
            case "测试", "qa", "tester", "test" -> "tester";
            case "git", "版本", "version" -> "git-commit";
            case "制作人", "producer", "pm" -> "producer";
            default -> "server-dev";
        };
    }

    /**
     * 检查里程碑并将任务分配给对应 Agent
     * 如果需要的 Agent 不存在，发起招聘审批请求
     */
    private void checkAndAssignMilestoneTasks() {
        String projectId = getProjectId();
        if (projectId == null || goalService == null) return;

        // 检查是否有暂停的审批
        if (pendingApprovalMilestoneId != null) {
            log.info("等待审批完成，里程碑: {}", pendingApprovalMilestoneId);
            return;
        }

        GameProject.GoalMilestone next = goalService.getNextExecutableMilestone(projectId);
        if (next == null) return;

        // 查找对应的 Agent
        String targetAgentId = projectId + ":" + next.getAssignedAgentRole();
        Agent targetAgent = agentManager.getAgent(targetAgentId);

        // 如果 Agent 不存在，发起招聘审批请求
        if (targetAgent == null) {
            log.info("Agent not found for milestone {}, requesting recruit approval: {}", next.getId(), next.getAssignedAgentRole());
            requestRecruitApproval(next.getAssignedAgentRole(), next.getTitle());
            return;
        }

        // 如果里程碑没有任务，生成任务
        if (next.getTasks().isEmpty()) {
            generateMilestoneTasks(projectId, next);
        }

        // 将待执行的任务发送给 Agent
        for (GameProject.MilestoneTask task : next.getTasks()) {
            if (task.getStatus() == GameProject.MilestoneStatus.PENDING) {
                // 构建包含目录信息的任务消息
                String taskContent = buildTaskContent(next, task);

                // 发送任务消息
                AgentMessage taskMsg = AgentMessage.builder()
                    .fromAgentId(getId())
                    .toAgentId(targetAgentId)
                    .type(AgentMessage.MessageType.TASK)
                    .content(taskContent)
                    .build();
                sendMessage(taskMsg);

                // 标记为进行中
                goalService.updateTaskStatus(projectId, next.getId(), task.getId(),
                    GameProject.MilestoneStatus.IN_PROGRESS, null);
            }
        }

        // 更新里程碑状态
        if (next.getStatus() == GameProject.MilestoneStatus.PENDING) {
            goalService.updateMilestoneProgress(projectId, next.getId(), 1);
        }
    }

    /**
     * 构建包含目录信息的任务内容
     * 在任务 prompt 中包含项目目录结构和可访问目录信息，帮助 Agent 理解项目结构
     *
     * @param milestone 里程碑
     * @param task 任务
     * @return 格式化的任务内容
     */
    private String buildTaskContent(GameProject.GoalMilestone milestone, GameProject.MilestoneTask task) {
        StringBuilder content = new StringBuilder();

        // 任务基本信息
        content.append("[里程碑任务] ").append(currentProject.getGoal()).append("\n\n");
        content.append("里程碑: ").append(milestone.getTitle()).append("\n");
        content.append("任务: ").append(task.getDescription()).append("\n");

        // 项目目录结构
        String dirConfigText = currentProject.getDirectoryConfigText();
        if (!dirConfigText.isEmpty()) {
            content.append("\n## 项目目录结构\n");
            content.append(dirConfigText);
        }

        // 获取可访问的目录列表
        List<String> accessibleDirs = getAccessibleDirs(milestone);

        // 本次任务可访问的目录
        if (accessibleDirs != null && !accessibleDirs.isEmpty()) {
            content.append("\n## 本次任务可访问的目录\n");
            for (String dir : accessibleDirs) {
                content.append("- ").append(dir);
                // 如果有目录配置，添加描述
                GameProject.DirectoryConfig config = currentProject.getDirectoryConfigs().get(dir);
                if (config != null) {
                    content.append(": ").append(config.getDescription());
                }
                content.append("\n");
            }
        }

        content.append("\n完成后请汇报进度。");

        return content.toString();
    }

    /**
     * 获取里程碑可访问的目录列表
     * 优先使用里程碑中设置的可访问目录，如果没有则从模板中获取
     *
     * @param milestone 里程碑
     * @return 可访问的目录路径列表
     */
    private List<String> getAccessibleDirs(GameProject.GoalMilestone milestone) {
        // 优先使用里程碑中设置的可访问目录
        List<String> accessibleDirs = milestone.getAccessibleDirs();
        if (accessibleDirs != null && !accessibleDirs.isEmpty()) {
            return accessibleDirs;
        }

        // 如果里程碑没有设置，尝试从模板中获取该角色可访问的目录
        if (gameTemplateService != null && currentProject != null) {
            String templateId = currentProject.getTemplateId();
            if (templateId != null && !templateId.isEmpty()) {
                String role = milestone.getAssignedAgentRole();
                List<String> templateDirs = gameTemplateService.getAccessibleDirsForRole(templateId, role);
                if (!templateDirs.isEmpty()) {
                    log.info("从模板 {} 获取角色 {} 可访问的目录: {}", templateId, role, templateDirs);
                    return templateDirs;
                }
            }
        }

        // 如果都没有，返回空列表（表示没有目录限制）
        return Collections.emptyList();
    }

    /**
     * 发起招聘审批请求
     * 团队招聘必须经过管理员审批
     *
     * @param role 需要招聘的角色
     * @param taskDescription 任务描述
     */
    private void requestRecruitApproval(String role, String taskDescription) {
        String projectId = getProjectId();
        if (projectId == null) return;

        String name = generateAgentName(role);
        String workDir = currentProject.getWorkDir();

        // 暂停流程等待审批
        String approvalId = "recruit-" + role + "-" + System.currentTimeMillis();
        pendingApprovalMilestoneId = approvalId;

        String requestData = String.format(
            "{\"role\":\"%s\",\"name\":\"%s\",\"workDir\":\"%s\",\"taskDescription\":\"%s\"}",
            role, name, workDir, taskDescription
        );

        String description = String.format(
            "招聘 %s (%s) 用于任务: %s",
            name, role, taskDescription
        );

        // 使用 ApprovalService 创建审批请求
        if (approvalService != null) {
            try {
                ApprovalRequest approvalRequest = approvalService.createRequest(
                    projectId,
                    getId(),
                    "CREATE_AGENT",
                    requestData,
                    description
                );
                logInfo("RECRUIT_APPROVAL_SENT", "已发起招聘审批请求，ID: " + approvalRequest.getId() + "，角色: " + role);
            } catch (Exception e) {
                log.error("创建审批请求失败: {}", e.getMessage());
                // 回退到直接通知
                sendNotificationToAdmin("RECRUIT_APPROVAL_REQUIRED",
                    String.format("【招聘审批请求】\n\n项目: %s\n角色: %s\n名称: %s\n任务: %s",
                        currentProject.getName(), role, name, taskDescription));
            }
        } else {
            // ApprovalService 不可用，使用直接通知
            sendNotificationToAdmin("RECRUIT_APPROVAL_REQUIRED",
                String.format("【招聘审批请求】\n\n项目: %s\n角色: %s\n名称: %s\n任务: %s",
                    currentProject.getName(), role, name, taskDescription));
        }

        // 保存待审批信息
        saveExperience("pending_recruit_" + approvalId,
            String.format("招聘审批中: %s (%s)，任务: %s", name, role, taskDescription));
    }

    /**
     * 招聘审批完成回调
     * 审批通过后自动创建 Agent 并分配任务
     *
     * @param milestoneId 审批 ID
     * @param approved 是否通过
     * @param role 角色
     * @param comment 审批意见
     */
    public void onRecruitApprovalCompleted(String milestoneId, boolean approved, String role, String comment) {
        if (!milestoneId.equals(pendingApprovalMilestoneId)) {
            log.warn("收到不匹配的招聘审批回调: expected={}, received={}", pendingApprovalMilestoneId, milestoneId);
            return;
        }

        pendingApprovalMilestoneId = null;

        if (approved) {
            logInfo("RECRUIT_APPROVED", "招聘审批通过: " + role + "，开始创建 Agent");

            // 创建 Agent
            String name = generateAgentName(role);
            String workDir = currentProject.getWorkDir();
            String projectId = getProjectId();

            try {
                AgentDefinition newDef = AgentDefinition.builder()
                    .id(role + "-" + System.currentTimeMillis() % 10000)
                    .name(name)
                    .role(role)
                    .description("经审批招聘，角色: " + role)
                    .workDir(workDir)
                    .projectId(projectId)
                    .status(AgentDefinition.AgentStatus.IDLE)
                    .build();

                Agent newAgent = agentManager.createAgent(newDef);

                sendNotificationToAdmin("RECRUIT_COMPLETED",
                    String.format("【招聘完成】\n\n项目: %s\n角色: %s\n名称: %s\n\nAgent 已创建，将自动分配任务。",
                        currentProject.getName(), role, name));

                logInfo("RECRUIT_COMPLETED", "Agent 已创建: " + name + " (" + role + ")");

                // 继续执行任务分配
                checkAndAssignMilestoneTasks();

            } catch (Exception e) {
                log.error("创建 Agent 失败: {}", e.getMessage());
                logError("RECRUIT_FAILED", "创建 Agent 失败: " + role, e.getMessage());
            }
        } else {
            logInfo("RECRUIT_REJECTED", "招聘审批被拒绝: " + role + "，原因: " + comment);

            sendNotificationToAdmin("RECRUIT_REJECTED",
                String.format("【招聘被拒绝】\n\n角色: %s\n原因: %s\n\n流程已停止，请手动处理。",
                    role, comment));
        }
    }

    /**
     * 为里程碑生成具体任务
     * 容错处理：兼容多种格式
     */
    private void generateMilestoneTasks(String projectId, GameProject.GoalMilestone milestone) {
        String taskPrompt = String.format(
            "请为以下里程碑生成3-5个具体可执行的任务：\n\n" +
            "里程碑：%s\n" +
            "描述：%s\n" +
            "负责角色：%s\n\n" +
            "请每行输出一个任务，格式：\n" +
            "TASK: 任务描述\n\n" +
            "也可以用 - 或数字编号开头。请只输出任务行。",
            milestone.getTitle(),
            milestone.getDescription() != null ? milestone.getDescription() : "无",
            milestone.getAssignedAgentRole()
        );

        String response = sendMessage(taskPrompt);
        if (response == null) return;

        int taskCount = 0;
        for (String line : response.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // 兼容多种格式：TASK:、Task:、任务：、-、1.、1）
            String taskDesc = null;
            if (line.toUpperCase().contains("TASK:")) {
                taskDesc = line.substring(line.toUpperCase().indexOf("TASK:") + "TASK:".length()).trim();
            } else if (line.contains("任务：") || line.contains("任务:")) {
                String marker = line.contains("任务：") ? "任务：" : "任务:";
                taskDesc = line.substring(line.indexOf(marker) + marker.length()).trim();
            } else if (line.matches("^[-*]\\s+.*")) {
                taskDesc = line.replaceFirst("^[-*]\\s+", "").trim();
            } else if (line.matches("^\\d+[.、)）]\\s*.*")) {
                taskDesc = line.replaceFirst("^\\d+[.、)）]\\s*", "").trim();
            }

            if (taskDesc != null && !taskDesc.isEmpty()) {
                goalService.addTask(projectId, milestone.getId(), taskDesc);
                taskCount++;
            }
        }

        if (taskCount == 0) {
            log.warn("No tasks generated for milestone: {}. Response: {}",
                milestone.getTitle(),
                response.length() > 200 ? response.substring(0, 200) + "..." : response);
        }
    }

    /**
     * 检查目标整体进度
     */
    private void checkGoalProgress() {
        String projectId = getProjectId();
        if (projectId == null || goalService == null) return;

        // 重新加载项目获取最新状态
        GameProject project = projectManager.getProject(projectId);
        if (project == null) return;

        // 检查是否应该进入审查
        if (goalService.shouldEnterReview(projectId)) {
            project.setGoalStatus(GameProject.GoalStatus.REVIEW);
            projectManager.saveProjectConfig(project);

            // 通知用户目标已完成，等待确认
            if (feishuService.isEnabled()) {
                feishuService.sendMessage(String.format(
                    "项目 [%s] 的目标已全部完成！\n\n目标: %s\n\n请确认验收。",
                    project.getName(), project.getGoal()
                ));
            }
            log.info("Goal entered review for project: {}", projectId);
        }

        // 检查截止时间
        if (project.getGoalDeadline() != null && LocalDateTime.now().isAfter(project.getGoalDeadline())) {
            log.warn("Goal deadline passed for project: {}", projectId);
            saveExperience("goal_deadline_passed_" + projectId,
                "项目目标截止时间已过: " + project.getGoalDeadline());
        }
    }

    @Override
    protected void handleMessage(AgentMessage message) {
        switch (message.getType()) {
            case REPORT -> handleReport(message);
            case QUERY -> handleQuery(message);
            case APPROVAL -> handleApprovalRequest(message);
            case RESPONSE -> handleResponse(message);
            case REVIEW -> handleReview(message);
            default -> log.info("Producer received message: {}", message.getType());
        }
    }

    private String getTeamStatus() {
        // 只获取当前项目下的 Agent（项目级隔离）
        String projectId = getProjectId();
        List<Agent> agents = projectId != null ?
            agentManager.getAgentsByProject(projectId) :
            agentManager.getAllAgents();

        StringBuilder sb = new StringBuilder("**团队状态报告**\n\n");

        if (currentProject != null) {
            sb.append("**项目**: ").append(currentProject.getName()).append("\n");
            sb.append("**工作目录**: ").append(currentProject.getWorkDir()).append("\n\n");
        }

        for (Agent agent : agents) {
            sb.append(String.format("- **%s** (%s): %s\n",
                agent.getName(),
                agent.getRole(),
                agent.isBusy() ? "忙碌" : "空闲"));
        }

        return sb.toString();
    }

    private void processApprovals() {
        List<AgentMessage> approvals = pendingMessages.stream()
            .filter(m -> m.getType() == AgentMessage.MessageType.APPROVAL)
            .collect(Collectors.toList());

        for (AgentMessage approval : approvals) {
            if (feishuService.isEnabled()) {
                feishuService.sendApprovalRequest(approval.getContent());
            } else {
                log.info("Feishu not enabled, approval request logged: {}", approval.getContent());
            }
        }
    }

    private void generateWorkInstructions() {
        log.info("Generating work instructions...");
        String skillPrompt = buildSkillPrompt("制定工作计划和任务分配");
        if (!skillPrompt.isEmpty()) {
            String instructions = sendMessage(skillPrompt);
            saveKnowledge("work_instructions", instructions);
        }
    }

    private void reportToUser(String status) {
        if (feishuService.isEnabled()) {
            feishuService.sendMessage(status);
        }
    }

    private void saveProjectStatus(String status) {
        agentContext.setProjectSummary(status);
        saveContext();
    }

    private void handleReport(AgentMessage message) {
        log.info("Received report from {}: {}", message.getFromAgentId(), message.getContent());
        saveMemory("last_report_" + message.getFromAgentId(), message.getContent());
        saveKnowledge("team_report_" + System.currentTimeMillis(), message.getContent());
    }

    private void handleQuery(AgentMessage message) {
        log.info("Received query from {}: {}", message.getFromAgentId(), message.getContent());
        String response = processQuery(message.getContent());
        AgentMessage responseMsg = AgentMessage.builder()
            .fromAgentId(getId())
            .toAgentId(message.getFromAgentId())
            .type(AgentMessage.MessageType.RESPONSE)
            .content(response)
            .build();
        sendMessage(responseMsg);
    }

    private void handleApprovalRequest(AgentMessage message) {
        log.info("Approval request from {}: {}", message.getFromAgentId(), message.getContent());
        if (feishuService.isEnabled()) {
            feishuService.sendApprovalRequest(
                String.format("来自 %s 的审批请求:\n%s",
                    message.getFromAgentId(),
                    message.getContent())
            );
        } else {
            log.info("Feishu not enabled, approval request logged: {}", message.getContent());
        }
    }

    private void handleResponse(AgentMessage message) {
        log.info("Received response from {}: {}", message.getFromAgentId(), message.getContent());
    }

    private void handleReview(AgentMessage message) {
        log.info("Received review request from {}: {}", message.getFromAgentId(), message.getContent());

        // 制作人处理审查请求，主要是协调和决策
        String reviewPrompt = buildSkillPrompt("审查和协调");
        String fullPrompt = reviewPrompt + "\n\n请审查以下内容并给出协调建议：\n\n" + message.getContent();
        String reviewResult = sendMessage(fullPrompt);

        // 保存审查记录
        saveExperience("review_" + System.currentTimeMillis(),
            String.format("Review from %s: %s\nResult: %s",
                message.getFromAgentId(), message.getContent(),
                reviewResult.length() > 500 ? reviewResult.substring(0, 500) + "..." : reviewResult));

        // 返回审查结果
        AgentMessage response = AgentMessage.builder()
            .fromAgentId(getId())
            .toAgentId(message.getFromAgentId())
            .type(AgentMessage.MessageType.RESPONSE)
            .content(reviewResult)
            .build();
        sendMessage(response);
    }

    private String processQuery(String query) {
        String skillPrompt = buildSkillPrompt("回答问题和查询");
        String fullPrompt = skillPrompt + "\n\n请回答以下问题: " + query;
        return sendMessage(fullPrompt);
    }

    public String requestHiring(String role, String requirements) {
        String hiringPlan = String.format(
            "**招聘请求**\n\n" +
            "角色: %s\n" +
            "要求:\n%s\n\n" +
            "请确认是否批准此次招聘。",
            role, requirements
        );

        if (feishuService.isEnabled()) {
            feishuService.sendApprovalRequest(hiringPlan);
        } else {
            log.info("Feishu not enabled, hiring request logged: {}", hiringPlan);
        }
        saveExperience("hiring_" + role, hiringPlan);
        return "招聘请求已发送，等待批准";
    }

    /**
     * 发送通知给管理员
     *
     * @param notificationType 通知类型
     * @param content 通知内容
     */
    private void sendNotificationToAdmin(String notificationType, String content) {
        // 通过飞书发送通知
        if (feishuService != null && feishuService.isEnabled()) {
            feishuService.sendMessage(String.format(
                "📢 制作人通知\n\n类型: %s\n\n%s",
                notificationType, content
            ));
        }

        // 记录日志
        logInfo(notificationType, content);

        // 保存到经验
        saveExperience(notificationType.toLowerCase() + "_" + System.currentTimeMillis(), content);
    }

    /**
     * 发起自动招聘请求
     * 当里程碑需要某个角色但没有对应 Agent 时，向管理员发起招聘审批请求
     *
     * @param role 需要的角色
     * @param taskDescription 任务描述
     * @return true 如果请求已发送，false 如果发送失败
     */
    private boolean requestAutoRecruit(String role, String taskDescription) {
        String projectId = getProjectId();
        if (projectId == null) return false;

        log.info("Requesting auto-recruit for role: {} in project: {}", role, projectId);

        // 生成招聘请求描述
        String name = generateAgentName(role);
        String requestData = String.format(
            "{\"role\":\"%s\",\"name\":\"%s\",\"reason\":\"里程碑任务需要: %s\"}",
            role, name, taskDescription
        );
        String description = String.format(
            "【自动招聘请求】\n\n项目: %s\n需要角色: %s\n建议名称: %s\n任务需求: %s\n\n请审批是否招聘该成员。",
            currentProject != null ? currentProject.getName() : projectId,
            role, name, taskDescription
        );

        // 发送招聘请求到管理员
        requestHiring(role, description);

        // 通知管理员
        sendNotificationToAdmin("AUTO_RECRUIT_REQUEST", description);

        logInfo("AUTO_RECRUIT_REQUEST", "已发起自动招聘请求，角色: " + role + "，等待管理员审批");
        return true;
    }

    /**
     * 发起自动优化建议
     * 检查团队成员状态，向管理员发起优化建议
     *
     * @param agentId 需要优化的 Agent ID
     * @param reason 优化原因
     * @param suggestion 优化建议
     */
    private void requestTeamOptimization(String agentId, String reason, String suggestion) {
        String projectId = getProjectId();
        if (projectId == null) return;

        Agent targetAgent = agentManager.getAgent(agentId);
        if (targetAgent == null) return;

        String description = String.format(
            "【团队优化建议】\n\n项目: %s\nAgent: %s (%s)\n角色: %s\n\n原因: %s\n\n建议: %s\n\n请审批是否执行优化。",
            currentProject != null ? currentProject.getName() : projectId,
            targetAgent.getName(), agentId, targetAgent.getRole(),
            reason, suggestion
        );

        // 发送优化建议到管理员
        sendNotificationToAdmin("TEAM_OPTIMIZATION", description);

        logInfo("TEAM_OPTIMIZATION", "已发起团队优化建议: " + agentId);
    }

    /**
     * 生成 Agent 名称
     */
    private String generateAgentName(String role) {
        String prefix = switch (role) {
            case "server-dev" -> "服务端开发";
            case "client-dev" -> "客户端开发";
            case "ui-dev" -> "UI设计";
            case "system-planner" -> "系统策划";
            case "numerical-planner" -> "数值策划";
            case "tester" -> "测试工程师";
            case "git-commit" -> "Git专员";
            default -> role;
        };
        return prefix + "-" + System.currentTimeMillis() % 10000;
    }

    /**
     * 检查团队状态并发起优化建议
     * 检查团队成员状态，向管理员发起优化建议
     */
    private void checkTeamAndSuggestOptimization() {
        String projectId = getProjectId();
        if (projectId == null) return;

        List<Agent> agents = agentManager.getAgentsByProject(projectId);
        GameProject project = projectManager.getProject(projectId);
        if (project == null) return;

        // 检查是否有长期空闲的 Agent
        for (Agent agent : agents) {
            if ("producer".equals(agent.getRole())) continue; // 跳过制作人

            // 如果 Agent 长期空闲且没有任务，建议优化
            if (!agent.isBusy() && agent.getPendingMessages().isEmpty()) {
                // 检查是否有待执行的里程碑任务需要这个角色
                boolean needed = isRoleNeeded(projectId, agent.getRole());
                if (!needed) {
                    log.info("Agent {} ({}) is idle and not needed, suggesting optimization", agent.getName(), agent.getRole());
                    requestTeamOptimization(
                        agent.getId(),
                        "Agent 长期空闲且当前项目不需要该角色",
                        "建议解雇该 Agent 以节省资源"
                    );
                }
            }
        }
    }

    /**
     * 检查角色是否被需要
     */
    private boolean isRoleNeeded(String projectId, String role) {
        if (goalService == null) return true; // 如果没有目标服务，默认认为需要

        // 检查是否有下一个可执行的里程碑需要这个角色
        GameProject.GoalMilestone nextMilestone = goalService.getNextExecutableMilestone(projectId);
        if (nextMilestone != null && role.equals(nextMilestone.getAssignedAgentRole())) {
            return true;
        }

        // 检查项目目标状态
        GameProject project = projectManager.getProject(projectId);
        if (project != null && project.getGoalStatus() == GameProject.GoalStatus.IN_PROGRESS) {
            // 项目进行中，默认认为需要所有已招聘的角色
            return true;
        }

        return false;
    }

    /**
     * 推动团队知识进化
     * 触发知识库自进化，并将积累的知识改进推送给团队 Agent
     */
    private void evolveTeamKnowledge() {
        if (knowledgeEvolutionService == null) return;

        String projectId = getProjectId();
        if (projectId == null) return;

        try {
            // 1. 触发知识进化
            knowledgeEvolutionService.selfEvolve();

            // 2. 获取进化统计
            Map<String, Object> stats = knowledgeEvolutionService.getEvolutionStats();
            int learnedSkills = ((Number) stats.getOrDefault("learnedSkillsCount", 0)).intValue();
            int patterns = ((Number) stats.getOrDefault("learnedPatternsCount", 0)).intValue();

            if (learnedSkills > 0 || patterns > 0) {
                log.info("知识进化统计: 学到技能={}, 已学习模式={}", learnedSkills, patterns);

                // 3. 向团队广播知识进化通知
                String notification = String.format(
                    "知识库已更新：学到 %d 个全局技能，%d 个成功模式。请在工作中参考知识库中的最佳实践。",
                    learnedSkills, patterns);

                List<Agent> agents = agentManager.getAgentsByProject(projectId);
                for (Agent agent : agents) {
                    if (!agent.getId().equals(getId()) && agent instanceof BaseAgent baseAgent) {
                        baseAgent.saveKnowledge("knowledge_evolution_update", notification);
                    }
                }

                logInfo("KNOWLEDGE_EVOLUTION", "已向团队推送知识进化更新");
            }
        } catch (Exception e) {
            log.error("推动知识进化失败: {}", e.getMessage());
        }
    }

    /**
     * 发起创建 Agent 审批请求
     * 团队招聘必须经过管理员审批
     *
     * @param name      Agent 名称
     * @param role      Agent 角色
     * @param agentsFile Agent 配置文件
     * @param workDir   工作目录
     */
    public void requestCreateAgent(String name, String role, String agentsFile, String workDir) {
        String projectId = getProjectId();

        String approvalId = "create-agent-" + System.currentTimeMillis();
        pendingApprovalMilestoneId = approvalId;

        String requestData = String.format(
            "{\"name\":\"%s\",\"role\":\"%s\",\"agentsFile\":\"%s\",\"workDir\":\"%s\"}",
            name, role, agentsFile != null ? agentsFile : "", workDir
        );

        String description = String.format(
            "创建 Agent: %s (%s)，工作目录: %s",
            name, role, workDir
        );

        // 使用 ApprovalService 创建审批请求
        if (approvalService != null) {
            try {
                ApprovalRequest approvalRequest = approvalService.createRequest(
                    projectId,
                    getId(),
                    "CREATE_AGENT",
                    requestData,
                    description
                );
                logInfo("CREATE_AGENT_APPROVAL_SENT", "已发起创建 Agent 审批请求，ID: " + approvalRequest.getId());
            } catch (Exception e) {
                log.error("创建审批请求失败: {}", e.getMessage());
                // 回退到直接通知
                sendNotificationToAdmin("CREATE_AGENT_APPROVAL_REQUIRED",
                    String.format("【创建 Agent 审批请求】\n\n项目: %s\n角色: %s\n名称: %s\n工作目录: %s",
                        currentProject != null ? currentProject.getName() : projectId, role, name, workDir));
            }
        } else {
            // ApprovalService 不可用，使用直接通知
            sendNotificationToAdmin("CREATE_AGENT_APPROVAL_REQUIRED",
                String.format("【创建 Agent 审批请求】\n\n项目: %s\n角色: %s\n名称: %s\n工作目录: %s",
                    currentProject != null ? currentProject.getName() : projectId, role, name, workDir));
        }

        logInfo("CREATE_AGENT_APPROVAL_SENT", "已发起创建 Agent 审批请求: " + name + " (" + role + ")");
    }

    /**
     * 创建 Agent 审批完成回调
     * 审批通过后自动创建 Agent
     *
     * @param milestoneId 审批 ID
     * @param approved 是否通过
     * @param name Agent 名称
     * @param role Agent 角色
     * @param agentsFile 配置文件
     * @param workDir 工作目录
     * @param comment 审批意见
     */
    public void onCreateAgentApprovalCompleted(String milestoneId, boolean approved,
                                                String name, String role, String agentsFile,
                                                String workDir, String comment) {
        if (!milestoneId.equals(pendingApprovalMilestoneId)) {
            log.warn("收到不匹配的创建 Agent 审批回调: expected={}, received={}", pendingApprovalMilestoneId, milestoneId);
            return;
        }

        pendingApprovalMilestoneId = null;

        if (approved) {
            String projectId = getProjectId();

            AgentDefinition newDef = AgentDefinition.builder()
                .id(role.toLowerCase().replace(" ", "-") + "-" + System.currentTimeMillis())
                .name(name)
                .role(role)
                .agentsFile(agentsFile)
                .workDir(workDir)
                .projectId(projectId)
                .status(AgentDefinition.AgentStatus.IDLE)
                .build();

            Agent agent = agentManager.createAgent(newDef);

            sendNotificationToAdmin("AGENT_CREATED",
                String.format("【Agent 已创建】\n\n项目: %s\n角色: %s\n名称: %s\n\n审批通过，Agent 已加入团队。",
                    currentProject != null ? currentProject.getName() : projectId, role, name));

            logInfo("AGENT_CREATED", "Agent 已创建: " + name + " (" + role + ")");

            saveExperience("create_agent_" + newDef.getEffectiveId(),
                String.format("Created agent: %s (%s) for project: %s, workDir: %s", name, role, projectId, workDir));

            // 继续执行任务分配
            checkAndAssignMilestoneTasks();
        } else {
            logInfo("CREATE_AGENT_REJECTED", "创建 Agent 审批被拒绝: " + name + " (" + role + ")，原因: " + comment);

            sendNotificationToAdmin("CREATE_AGENT_REJECTED",
                String.format("【创建 Agent 被拒绝】\n\n角色: %s\n名称: %s\n原因: %s\n\n流程已停止，请手动处理。",
                    role, name, comment));
        }
    }

    /**
     * 暂停流程等待审批
     * 用于重大操作（如删除Agent、部署上线）
     *
     * @param milestoneId 里程碑 ID
     * @param approvalType 审批类型
     * @param reason 审批原因
     */
    public void pauseForApproval(String milestoneId, String approvalType, String reason) {
        String projectId = getProjectId();
        if (projectId == null) return;

        pendingApprovalMilestoneId = milestoneId;

        String description = String.format(
            "【审批请求】\n\n项目: %s\n类型: %s\n里程碑: %s\n原因: %s\n\n流程已暂停，等待审批完成后自动继续。",
            currentProject != null ? currentProject.getName() : projectId,
            approvalType,
            milestoneId,
            reason
        );

        // 通知管理员需要审批
        sendNotificationToAdmin("APPROVAL_REQUIRED", description);

        logInfo("APPROVAL_PAUSED", "流程已暂停等待审批: " + approvalType);
    }

    /**
     * 审批完成回调
     * 审批通过后自动继续流程
     *
     * @param milestoneId 里程碑 ID
     * @param approved 是否通过
     * @param comment 审批意见
     */
    public void onApprovalCompleted(String milestoneId, boolean approved, String comment) {
        if (!milestoneId.equals(pendingApprovalMilestoneId)) {
            log.warn("收到不匹配的审批回调: expected={}, received={}", pendingApprovalMilestoneId, milestoneId);
            return;
        }

        pendingApprovalMilestoneId = null;

        if (approved) {
            logInfo("APPROVAL_APPROVED", "审批通过，流程继续: " + comment);
            // 继续执行
            checkAndAssignMilestoneTasks();
        } else {
            logInfo("APPROVAL_REJECTED", "审批被拒绝: " + comment);
            // 通知管理员
            sendNotificationToAdmin("APPROVAL_REJECTED",
                String.format("审批被拒绝，流程已停止\n原因: %s", comment));
        }
    }

    /**
     * 启动工作流
     * 制作人可以选择或创建工作流来执行里程碑
     *
     * @param templateId 工作流模板 ID
     * @param milestoneId 里程碑 ID（可选）
     * @return 工作流实例 ID
     */
    public String startWorkflow(String templateId, String milestoneId) {
        String projectId = getProjectId();
        if (projectId == null || workflowEngine == null) return null;

        Map<String, String> params = new HashMap<>();
        if (milestoneId != null) {
            params.put("milestoneId", milestoneId);
        }
        params.put("goal", currentProject != null ? currentProject.getGoal() : "");

        try {
            WorkflowEngine.WorkflowInstance instance = workflowEngine.startWorkflow(templateId, projectId, params);
            pendingWorkflowInstanceId = instance.getId();

            logInfo("WORKFLOW_STARTED", "工作流已启动: " + templateId);
            sendNotificationToAdmin("WORKFLOW_STARTED",
                String.format("项目 [%s] 启动了工作流\n模板: %s\n实例ID: %s",
                    currentProject != null ? currentProject.getName() : projectId,
                    templateId, instance.getId()));

            return instance.getId();
        } catch (Exception e) {
            log.error("启动工作流失败: {}", e.getMessage());
            logError("WORKFLOW_START_FAILED", "启动工作流失败: " + templateId, e.getMessage());
            return null;
        }
    }

    /**
     * 选择工作流模板
     * 制作人根据项目需求选择合适的工作流
     *
     * @param criteria 选择标准（如 "游戏开发"、"快速原型"、"紧急修复"）
     * @return 推荐的模板 ID 列表
     */
    public List<String> selectWorkflow(String criteria) {
        if (workflowEngine == null) return List.of();

        List<WorkflowEngine.WorkflowTemplate> templates = workflowEngine.getAllTemplates();
        List<String> recommended = new ArrayList<>();

        String lowerCriteria = criteria.toLowerCase();

        for (WorkflowEngine.WorkflowTemplate template : templates) {
            String name = template.getName().toLowerCase();
            String desc = template.getDescription() != null ? template.getDescription().toLowerCase() : "";

            // 根据关键词匹配
            if (name.contains(lowerCriteria) || desc.contains(lowerCriteria)) {
                recommended.add(template.getId());
            }
        }

        // 如果没有精确匹配，返回默认推荐
        if (recommended.isEmpty()) {
            if (lowerCriteria.contains("游戏") || lowerCriteria.contains("完整")) {
                recommended.add("standard-game-dev");
            } else if (lowerCriteria.contains("服务端") || lowerCriteria.contains("后端")) {
                recommended.add("server-only-dev");
            } else if (lowerCriteria.contains("客户端") || lowerCriteria.contains("前端")) {
                recommended.add("client-only-dev");
            } else if (lowerCriteria.contains("快速") || lowerCriteria.contains("原型")) {
                recommended.add("rapid-prototype");
            } else if (lowerCriteria.contains("修复") || lowerCriteria.contains("紧急")) {
                recommended.add("hotfix");
            } else {
                // 返回所有模板
                templates.forEach(t -> recommended.add(t.getId()));
            }
        }

        return recommended;
    }

    /**
     * 创建自定义工作流
     * 制作人可以根据项目需求创建新的工作流模板
     *
     * @param name 工作流名称
     * @param description 描述
     * @param steps 步骤列表（JSON 格式）
     * @return 创建的模板 ID
     */
    public String createCustomWorkflow(String name, String description, String steps) {
        if (workflowEngine == null) return null;

        String templateId = "custom-" + UUID.randomUUID().toString().substring(0, 8);

        try {
            // 解析步骤（简化实现，实际应该解析 JSON）
            List<WorkflowEngine.WorkflowStep> stepList = new ArrayList<>();

            // 默认创建简单的工作流
            WorkflowEngine.WorkflowStep step1 = new WorkflowEngine.WorkflowStep(
                "step1", "执行任务", "server-dev", description
            );
            stepList.add(step1);

            workflowEngine.createTemplate(templateId, name, description, stepList);

            logInfo("WORKFLOW_CREATED", "已创建工作流: " + name + " (" + templateId + ")");
            sendNotificationToAdmin("WORKFLOW_CREATED",
                String.format("【新工作流已创建】\n\n名称: %s\nID: %s\n描述: %s\n\n由制作人创建，无需审批。",
                    name, templateId, description));

            return templateId;
        } catch (Exception e) {
            log.error("创建工作流失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 分配 API 配置（纯执行，审批由能力系统拦截器处理）
     *
     * @param agentId Agent ID
     * @param apiKey  API Key
     * @param apiUrl  API URL
     * @param model   模型
     */
    public void assignApiConfig(String agentId, String apiKey, String apiUrl, String model) {
        Agent agent = agentManager.getAgent(agentId);
        if (agent != null) {
            createSnapshot();

            if (apiKey != null) agent.getDefinition().setApiKey(apiKey);
            if (apiUrl != null) agent.getDefinition().setApiUrl(apiUrl);
            if (model != null) agent.getDefinition().setModel(model);

            if (apiUrl != null || model != null) {
                agentContext.addApiConfig(apiUrl, model);
            }
            agent.saveContext();

            log.info("Assigned API config to agent: {}", agentId);

            if (agent instanceof BaseAgent baseAgent) {
                baseAgent.recoverContext();
            }
        }
    }

    public void assignWorkDir(String agentId, String workDir) {
        Agent agent = agentManager.getAgent(agentId);
        if (agent != null) {
            createSnapshot();

            agent.getDefinition().setWorkDir(workDir);

            if (agent instanceof BaseAgent baseAgent) {
                baseAgent.initProject();
                baseAgent.loadProjectSkills();
                baseAgent.recoverContext();
            }

            agent.saveContext();
            log.info("Assigned workDir to agent: {} -> {}", agentId, workDir);
        }
    }

    public void notifyQuotaExhausted(String agentId) {
        if (feishuService.isEnabled()) {
            feishuService.sendMessage(
                String.format("⚠️ Agent %s 的 API 配额不足，请及时更换或重置。", agentId)
            );
        } else {
            log.warn("Agent {} 的 API 配额不足", agentId);
        }
        saveExperience("quota_exhausted_" + agentId, "API quota exhausted at " + java.time.LocalDateTime.now());
    }

    // ===== 动态评估、打分和解雇功能（接入已有 PerformanceManagementService） =====

    /**
     * 动态评估项目需求
     * 分析当前项目状态，评估是否需要招聘新 Agent 或解雇低绩效 Agent
     *
     * @return 评估结果报告
     */
    public String evaluateProjectNeeds() {
        String projectId = getProjectId();
        if (projectId == null) return "项目未设置";

        StringBuilder report = new StringBuilder();
        report.append("## 项目需求评估报告\n\n");

        // 1. 收集当前团队状态
        List<Agent> agents = agentManager.getAgentsByProject(projectId);
        report.append("### 当前团队成员\n");
        for (Agent agent : agents) {
            if ("producer".equals(agent.getRole())) continue;

            // 从 PerformanceManagementService 获取绩效数据
            List<PerformanceReview> reviews = performanceManagementService != null
                ? performanceManagementService.getAgentReviews(agent.getId())
                : List.of();

            if (!reviews.isEmpty()) {
                PerformanceReview latest = reviews.get(0);
                report.append(String.format("- %s (%s): 评分=%d, 等级=%s\n",
                    agent.getName(), agent.getRole(),
                    latest.getOverallScore(),
                    latest.getGrade()));
            } else {
                report.append(String.format("- %s (%s): 暂无绩效记录\n",
                    agent.getName(), agent.getRole()));
            }
        }

        // 2. 分析项目目标需求
        if (currentProject != null && currentProject.hasGoal() && goalService != null) {
            report.append("\n### 目标里程碑需求\n");
            List<GameProject.GoalMilestone> milestones = goalService.getMilestones(projectId);
            Set<String> requiredRoles = new HashSet<>();
            for (GameProject.GoalMilestone m : milestones) {
                if (m.getStatus() != GameProject.MilestoneStatus.COMPLETED) {
                    requiredRoles.add(m.getAssignedAgentRole());
                }
            }
            report.append("未完成里程碑需要的角色: ").append(requiredRoles).append("\n");

            // 3. 检查是否有缺失的角色
            Set<String> existingRoles = agents.stream()
                .map(Agent::getRole)
                .collect(Collectors.toSet());
            Set<String> missingRoles = new HashSet<>(requiredRoles);
            missingRoles.removeAll(existingRoles);

            if (!missingRoles.isEmpty()) {
                report.append("\n### 建议招聘的角色\n");
                for (String role : missingRoles) {
                    report.append("- ").append(role).append("\n");
                }
            }
        }

        // 4. 识别有警告的 Agent
        if (performanceManagementService != null) {
            List<DismissalRequest> pendingRequests = performanceManagementService.getPendingDismissalRequests();
            if (!pendingRequests.isEmpty()) {
                report.append("\n### 待审批的解雇申请\n");
                for (DismissalRequest req : pendingRequests) {
                    report.append(String.format("- %s (%s): %s，警告 %d 次\n",
                        req.getAgentName(), req.getAgentRole(),
                        req.getReason(), req.getWarningCount()));
                }
            }
        }

        // 5. 识别空闲且不需要的 Agent
        List<Agent> idleAgents = agents.stream()
            .filter(a -> !"producer".equals(a.getRole()))
            .filter(a -> !a.isBusy() && !isRoleNeeded(projectId, a.getRole()))
            .collect(Collectors.toList());

        if (!idleAgents.isEmpty()) {
            report.append("\n### 空闲且当前不需要的 Agent\n");
            for (Agent agent : idleAgents) {
                report.append(String.format("- %s (%s)\n", agent.getName(), agent.getRole()));
            }
        }

        return report.toString();
    }

    /**
     * 评估 Agent 表现并打分
     * 使用已有的 PerformanceManagementService.submitReview()
     *
     * @param agentId Agent ID
     * @param qualityScore 质量评分 (0-100)
     * @param efficiencyScore 效率评分 (0-100)
     * @param collaborationScore 协作评分 (0-100)
     * @param innovationScore 创新评分 (0-100)
     * @param strengths 优点
     * @param improvements 待改进
     * @param comments 评价
     */
    public void evaluateAgent(String agentId, Integer qualityScore, Integer efficiencyScore,
                               Integer collaborationScore, Integer innovationScore,
                               String strengths, String improvements, String comments) {
        String projectId = getProjectId();
        if (projectId == null || performanceManagementService == null) return;

        Agent agent = agentManager.getAgent(agentId);
        if (agent == null) {
            log.warn("评估失败：Agent 不存在 {}", agentId);
            return;
        }

        try {
            // 生成评审周期（当前月份）
            String reviewPeriod = java.time.YearMonth.now().toString();

            // 调用已有的绩效管理服务
            PerformanceReview review = performanceManagementService.submitReview(
                getId(),           // 制作人 ID
                agentId,           // 被评审 Agent ID
                projectId,         // 项目 ID
                reviewPeriod,      // 评审周期
                qualityScore,
                efficiencyScore,
                collaborationScore,
                innovationScore,
                strengths,
                improvements,
                comments,
                null               // highlights
            );

            logInfo("AGENT_EVALUATED", String.format("Agent %s (%s) 绩效评审完成: 综合评分=%d, 等级=%s",
                agent.getName(), agent.getRole(), review.getOverallScore(), review.getGrade()));

            sendNotificationToAdmin("AGENT_EVALUATED",
                String.format("【Agent 绩效评估】\n\n项目: %s\nAgent: %s (%s)\n综合评分: %d\n等级: %s\n质量: %d, 效率: %d, 协作: %d, 创新: %d",
                    currentProject != null ? currentProject.getName() : projectId,
                    agent.getName(), agent.getRole(),
                    review.getOverallScore(), review.getGrade(),
                    qualityScore, efficiencyScore, collaborationScore, innovationScore));

        } catch (Exception e) {
            log.error("绩效评审失败: {}", e.getMessage());
            logError("EVALUATION_FAILED", "绩效评审失败: " + agent.getName(), e.getMessage());
        }
    }

    /**
     * 发起解雇申请
     * 使用已有的 PerformanceManagementService.submitDismissalRequest()
     *
     * @param agentId 要解雇的 Agent ID
     * @param reasonType 原因类型（LOW_PERFORMANCE/ATTITUDE_ISSUE/REDUNDANT_ROLE/OTHER）
     * @param reason 解雇原因
     */
    public void requestDismissAgent(String agentId, String reasonType, String reason) {
        String projectId = getProjectId();
        if (projectId == null || performanceManagementService == null) return;

        Agent agent = agentManager.getAgent(agentId);
        if (agent == null) {
            log.warn("解雇失败：Agent 不存在 {}", agentId);
            return;
        }

        // 检查是否是核心角色
        if ("producer".equals(agent.getRole())) {
            logInfo("DISMISS_REJECTED", "不能解雇制作人角色");
            sendNotificationToAdmin("DISMISS_REJECTED",
                "【解雇被拒绝】\n\n原因: 不能解雇制作人角色");
            return;
        }

        try {
            // 调用已有的绩效管理服务
            DismissalRequest request = performanceManagementService.submitDismissalRequest(
                getId(),       // 制作人 ID
                agentId,       // 被解雇 Agent ID
                reasonType,    // 原因类型
                reason         // 原因详情
            );

            logInfo("DISMISS_REQUEST_SENT", String.format("解雇申请已提交: %s (%s)，申请编号: %s",
                agent.getName(), agent.getRole(), request.getRequestNo()));

            sendNotificationToAdmin("DISMISS_REQUEST_SENT",
                String.format("【解雇申请已提交】\n\n项目: %s\nAgent: %s (%s)\n申请编号: %s\n原因类型: %s\n原因: %s\n\n等待管理员审批。",
                    currentProject != null ? currentProject.getName() : projectId,
                    agent.getName(), agent.getRole(),
                    request.getRequestNo(),
                    request.getReasonTypeDescription(),
                    reason));

        } catch (Exception e) {
            log.error("提交解雇申请失败: {}", e.getMessage());
            logError("DISMISS_REQUEST_FAILED", "提交解雇申请失败: " + agent.getName(), e.getMessage());
        }
    }

    /**
     * 批量评估团队成员
     * AI 驱动的团队绩效评估
     */
    public void batchEvaluateTeam() {
        String projectId = getProjectId();
        if (projectId == null) return;

        List<Agent> agents = agentManager.getAgentsByProject(projectId);
        if (agents.isEmpty()) return;

        // 收集每个 Agent 的工作上下文
        StringBuilder evalContext = new StringBuilder();
        evalContext.append("## 团队成员评估\n\n");

        for (Agent agent : agents) {
            if ("producer".equals(agent.getRole())) continue;

            evalContext.append(String.format("### %s (%s)\n", agent.getName(), agent.getRole()));
            evalContext.append(String.format("- 状态: %s\n", agent.isBusy() ? "忙碌" : "空闲"));

            // 从 PerformanceManagementService 获取历史绩效
            if (performanceManagementService != null) {
                List<PerformanceReview> reviews = performanceManagementService.getAgentReviews(agent.getId());
                if (!reviews.isEmpty()) {
                    PerformanceReview latest = reviews.get(0);
                    evalContext.append(String.format("- 最近评分: %d (等级: %s)\n",
                        latest.getOverallScore(), latest.getGrade()));
                    evalContext.append(String.format("- 质量: %d, 效率: %d, 协作: %d, 创新: %d\n",
                        latest.getQualityScore(), latest.getEfficiencyScore(),
                        latest.getCollaborationScore(), latest.getInnovationScore()));
                } else {
                    evalContext.append("- 暂无绩效记录\n");
                }

                // 警告次数
                Long warningCount = performanceManagementService.getWarningCount(agent.getId());
                if (warningCount > 0) {
                    evalContext.append(String.format("- 警告次数: %d\n", warningCount));
                }
            }
            evalContext.append("\n");
        }

        // 使用 AI 分析并给出建议
        String evalPrompt = buildWorkPrompt(evalContext.toString()) +
            "\n\n请基于以上团队成员的表现，给出绩效评估建议（每个成员一行，格式：EVAL: agentId | 质量评分 | 效率评分 | 协作评分 | 创新评分 | 优点 | 待改进 | 评价）";

        String response = sendMessage(evalPrompt);
        if (response == null) return;

        // 解析 AI 的评估建议并应用
        parseAndApplyEvaluations(response);
    }

    /**
     * 解析 AI 的评估建议并应用
     */
    private void parseAndApplyEvaluations(String response) {
        String projectId = getProjectId();
        if (projectId == null) return;

        for (String line : response.split("\n")) {
            line = line.trim();
            if (!line.toUpperCase().contains("EVAL:")) continue;

            try {
                String content = line.substring(line.toUpperCase().indexOf("EVAL:") + "EVAL:".length()).trim();
                String[] parts = content.split("[|｜]+");
                if (parts.length < 5) continue;

                String agentIdOrRole = parts[0].trim();
                int qualityScore = Integer.parseInt(parts[1].trim());
                int efficiencyScore = Integer.parseInt(parts[2].trim());
                int collaborationScore = Integer.parseInt(parts[3].trim());
                int innovationScore = Integer.parseInt(parts[4].trim());
                String strengths = parts.length > 5 ? parts[5].trim() : "";
                String improvements = parts.length > 6 ? parts[6].trim() : "";
                String comments = parts.length > 7 ? parts[7].trim() : "AI 自动评估";

                // 查找 Agent
                Agent agent = findAgentByIdOrRole(agentIdOrRole, projectId);
                if (agent != null && performanceManagementService != null) {
                    // 调用绩效评审
                    evaluateAgent(agent.getId(), qualityScore, efficiencyScore,
                        collaborationScore, innovationScore, strengths, improvements, comments);
                }
            } catch (Exception e) {
                log.warn("解析评估建议失败: {}", line, e);
            }
        }
    }

    /**
     * 通过 ID 或角色查找 Agent
     */
    private Agent findAgentByIdOrRole(String idOrRole, String projectId) {
        // 先尝试直接查找
        Agent agent = agentManager.getAgent(idOrRole);
        if (agent != null) return agent;

        // 尝试按角色查找
        List<Agent> agents = agentManager.getAgentsByProject(projectId);
        for (Agent a : agents) {
            if (a.getRole().equals(idOrRole) || a.getName().contains(idOrRole)) {
                return a;
            }
        }
        return null;
    }
}
