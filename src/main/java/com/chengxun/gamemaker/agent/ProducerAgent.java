package com.chengxun.gamemaker.agent;

import com.chengxun.gamemaker.engine.ClaudeCliEngine;
import com.chengxun.gamemaker.engine.MessageBus;
import com.chengxun.gamemaker.feishu.FeishuBotService;
import com.chengxun.gamemaker.manager.*;
import com.chengxun.gamemaker.model.*;
import com.chengxun.gamemaker.service.*;
import com.chengxun.gamemaker.web.entity.AgentCapability;
import com.chengxun.gamemaker.web.entity.AgentIntervention;
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

    /** 游戏运行时验证服务（延迟注入） */
    private com.chengxun.gamemaker.service.GameRuntimeVerifier gameRuntimeVerifier;

    /** 工作流引擎（延迟注入） */
    @Autowired(required = false)
    private WorkflowEngine workflowEngine;

    /** 审批回调服务（延迟注入） */
    @Autowired(required = false)
    private ApprovalCallbackService approvalCallbackService;

    /** 审批服务（延迟注入） */
    @Autowired(required = false)
    private ApprovalService approvalService;

    /** 能力注册表（延迟注入） */
    @Autowired(required = false)
    private CapabilityRegistry capabilityRegistry;

    /** 通知服务（延迟注入） */
    @Autowired(required = false)
    private com.chengxun.gamemaker.web.service.NotificationService notificationService;

    /** 绩效管理服务（延迟注入） */
    @Autowired(required = false)
    private PerformanceManagementService performanceManagementService;

    /** 事件总线（延迟注入） */
    @Autowired(required = false)
    private EventBus eventBus;

    /** 项目看板（延迟注入） */
    @Autowired(required = false)
    private ProjectBoard projectBoard;

    /** 版本评估服务（延迟注入） */
    @Autowired(required = false)
    private VersionEvaluationService versionEvaluationService;

    /** 当前暂停的里程碑 ID（等待审批） */
    private String pendingApprovalMilestoneId;

    /** 当前暂停的工作流实例 ID */
    private String pendingWorkflowInstanceId;

    /** 工作流启动时间，用于卡住检测 */
    private java.time.LocalDateTime workflowStartTime;

    /** 工作流卡住超时时间（分钟）：超过此时间无进展则自动取消 */
    private static final int WORKFLOW_STUCK_TIMEOUT_MINUTES = 15;

    /** 已被拒绝的里程碑 ID 集合（防止重复发起审批） */
    private final Set<String> rejectedMilestones = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** 最近失败的里程碑工作流（避免重复尝试），key: milestoneId, value: 失败时间戳ms */
    private final java.util.concurrent.ConcurrentHashMap<String, Long> failedWorkflowMilestones = new java.util.concurrent.ConcurrentHashMap<>();
    /** 失败工作流冷却时间：30 分钟 */
    private static final long FAILED_WORKFLOW_COOLDOWN_MS = 1800000;

    /** 工作周期计数器，用于定期触发绩效检查等操作 */
    private long workCycleCount = 0;

    /**
     * 任务分配追踪表
     * 记录制作人分配给各 Agent 的任务及其开始时间
     * key: agentId, value: 任务信息（标题 + 分配时间）
     */
    private final java.util.concurrent.ConcurrentHashMap<String, TaskTracker> assignedTasks = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 任务追踪记录
     */
    private static class TaskTracker {
        final String taskTitle;
        final long assignedAt;
        final String milestoneId;
        final int initialProgress; // 分配时的进度，用于检测是否卡住

        TaskTracker(String taskTitle, String milestoneId) {
            this(taskTitle, milestoneId, 0);
        }

        TaskTracker(String taskTitle, String milestoneId, int initialProgress) {
            this.taskTitle = taskTitle;
            this.assignedAt = System.currentTimeMillis();
            this.milestoneId = milestoneId;
            this.initialProgress = initialProgress;
        }

        long getElapsedMinutes() {
            return (System.currentTimeMillis() - assignedAt) / 60000;
        }

        Map<String, Object> toMap() {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("taskTitle", taskTitle);
            map.put("milestoneId", milestoneId);
            map.put("assignedAt", assignedAt);
            map.put("elapsedMinutes", getElapsedMinutes());
            return map;
        }
    }

    /**
     * 获取制作人状态摘要
     * 供前端展示使用
     */
    public Map<String, Object> getProducerStatus() {
        Map<String, Object> status = new java.util.HashMap<>();
        status.put("workCycleCount", workCycleCount);
        status.put("pendingApproval", pendingApprovalMilestoneId != null);
        status.put("pendingApprovalId", pendingApprovalMilestoneId);
        status.put("activeWorkflow", pendingWorkflowInstanceId != null);
        status.put("activeWorkflowId", pendingWorkflowInstanceId);
        status.put("rejectedMilestones", rejectedMilestones.size());

        // 任务追踪
        List<Map<String, Object>> tasks = new java.util.ArrayList<>();
        assignedTasks.forEach((agentId, tracker) -> {
            Map<String, Object> task = tracker.toMap();
            task.put("agentId", agentId);
            Agent agent = agentManager.getAgent(agentId);
            if (agent != null) {
                task.put("agentName", agent.getName());
                task.put("agentBusy", agent.isBusy());
                task.put("agentRole", agent.getRole());
            }
            tasks.add(task);
        });
        status.put("assignedTasks", tasks);
        status.put("assignedTaskCount", tasks.size());

        return status;
    }

    /**
     * 本轮工作周期收到的报告摘要
     * 在 processPendingMessages → handleReport 中填充，
     * 在 buildWorkContext 中读取，让 AI 能看到本轮收到的团队报告
     */
    private final java.util.concurrent.CopyOnWriteArrayList<String> currentCycleReports = new java.util.concurrent.CopyOnWriteArrayList<>();

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
     * 设置游戏运行时验证服务
     */
    public void setGameRuntimeVerifier(com.chengxun.gamemaker.service.GameRuntimeVerifier gameRuntimeVerifier) {
        this.gameRuntimeVerifier = gameRuntimeVerifier;
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
     * 获取审批服务
     */
    public ApprovalService getApprovalService() {
        return approvalService;
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
        workCycleCount++;

        // 更新看板状态
        updateBoardStatus("WORKING", "统筹项目");

        // 清空上一轮的报告缓存
        currentCycleReports.clear();

        // 0. 检查并执行待处理的干预指令（最高优先级）
        // 干预指令优先于一切，包括项目完成状态
        if (executePendingInterventions()) {
            log.info("已执行待处理的干预指令，跳过本轮常规工作");
            return;
        }

        // 检查项目目标是否已完成，如果是则停止 Agent
        // 注意：必须先检查干预，再判断是否停止
        if (currentProject != null && currentProject.hasGoal()
            && currentProject.getGoalStatus() == GameProject.GoalStatus.COMPLETED) {
            log.info("项目目标已完成，停止制作人 Agent");
            logInfo("GOAL_COMPLETED", "项目目标已完成，Agent 即将停止");
            stop();
            return;
        }

        // 0.1 检查版本迭代干预
        if (workCycleCount % 3 == 0) {  // 每3个周期检查一次
            checkVersionIterationInterventions();
        }

        // 1. 检查正在运行的工作流状态
        checkRunningWorkflowStatus();

        // 2. 定期检查（不受审批等待影响，每周期都检查）
        // 鞭策机制：每2个周期（约2分钟）检查一次团队状态
        if (workCycleCount % 2 == 0) {
            whipTeamMembers();
        }
        if (workCycleCount % 10 == 0) {
            log.info("定期检查：触发动态团队评估（周期 {}）", workCycleCount);
            checkTeamAndSuggestOptimization();
        }
        if (workCycleCount % 15 == 0) {
            log.info("定期检查：推动团队知识进化（周期 {}）", workCycleCount);
            evolveTeamKnowledge();
        }
        // 每20个周期（约20分钟）进行一次团队绩效评估
        if (workCycleCount % 20 == 0 && performanceManagementService != null) {
            log.info("定期检查：触发团队绩效评估（周期 {}）", workCycleCount);
            batchEvaluateTeam();
        }

        // 每30个周期（约30分钟）进行一次游戏项目验证
        if (workCycleCount % 30 == 0) {
            log.info("定期检查：触发游戏项目验证（周期 {}）", workCycleCount);
            verifyGameProjectPeriodically();
        }

        // 3. 审批等待模式：暂停核心 AI 决策，只做轻量操作
        if (pendingApprovalMilestoneId != null) {
            log.info("审批等待中: {}，暂停核心决策，仅处理消息和检查状态", pendingApprovalMilestoneId);
            logInfo("APPROVAL_WAITING", String.format("等待审批完成，ID: %s，暂停里程碑推进", pendingApprovalMilestoneId));
            // 轻量操作：处理消息、检查工作流状态
            processApprovals();
            return;
        }

        // 3. 收集项目上下文（包含已处理的报告摘要，让 AI 看到团队动态）
        String context = buildWorkContext();

        // 4. 构建工作 prompt（包含上下文 + 能力引导）
        String prompt = buildWorkPrompt(context);

        // 5. 调用 AI — sendMessage 自动注入能力 prompt、MCP 配置、技能
        //    AI 的响应会通过 processCapabilityActions 自动执行能力调用
        String response = sendMessage(prompt);

        // 6. 记录本次工作决策（含上下文和 AI 响应）
        String logContext = context.length() > 5000 ? context.substring(0, 5000) + "\n...[截断]" : context;
        String logResponse = response != null && response.length() > 10000
            ? response.substring(0, 10000) + "\n...[截断]" : response;
        // 从 AI 响应中提取有意义的摘要（取前200字符作为摘要）
        String decisionSummary = "制作人工作决策 — 项目: " + (currentProject != null ? currentProject.getName() : "全局");
        if (response != null && !response.isEmpty()) {
            String firstLine = response.split("\n")[0].trim();
            if (firstLine.length() > 5) {
                decisionSummary = "制作人决策: " + (firstLine.length() > 200 ? firstLine.substring(0, 200) : firstLine);
            }
        }
        logDecision(decisionSummary,
            "## 工作上下文\n\n" + logContext + "\n\n## AI 决策输出\n\n" + logResponse);

        // 7. 硬编码兜底：如果 AI 未处理关键逻辑，自动触发
        ensureCriticalActions();

        // 8. 处理审批和通知
        processApprovals();
        reportToUser(context);

        // 9. 保存项目状态
        saveProjectStatus(context);
    }

    /**
     * 监听工作流审批请求事件
     * 对于PRODUCER级别的审批，制作人自动审批通过
     * 对于HUMAN级别的审批，通知管理员处理
     */
    @org.springframework.context.event.EventListener
    @org.springframework.scheduling.annotation.Async
    public void onWorkflowApprovalRequired(com.chengxun.gamemaker.web.service.WorkflowEngine.ApprovalRequiredEvent event) {
        String instanceId = event.getInstanceId();
        String stepId = event.getStepId();

        log.info("收到工作流审批请求: instance={}, step={}", instanceId, stepId);

        // 获取工作流实例和步骤信息
        if (workflowEngine == null) {
            log.warn("工作流引擎未注入，无法处理审批请求");
            return;
        }

        com.chengxun.gamemaker.web.service.WorkflowEngine.WorkflowInstance instance = workflowEngine.getInstance(instanceId);
        if (instance == null) {
            log.warn("工作流实例不存在: {}", instanceId);
            return;
        }

        // 获取步骤信息
        com.chengxun.gamemaker.web.service.WorkflowEngine.WorkflowTemplate template = workflowEngine.getTemplate(instance.getTemplateId());
        if (template == null) {
            log.warn("工作流模板不存在: {}", instance.getTemplateId());
            return;
        }

        // 查找对应的步骤
        com.chengxun.gamemaker.web.service.WorkflowEngine.WorkflowStep step = template.getSteps().stream()
            .filter(s -> s.getId().equals(stepId))
            .findFirst()
            .orElse(null);

        if (step == null) {
            log.warn("工作流步骤不存在: {}", stepId);
            return;
        }

        String approvalLevel = step.getApprovalLevel();

        // 根据审批级别处理
        if ("PRODUCER".equals(approvalLevel)) {
            // 制作人自动审批
            log.info("制作人自动审批PRODUCER级别步骤: step={}, instance={}", stepId, instanceId);
            boolean success = workflowEngine.producerApproveStep(instanceId, stepId, getId(), "制作人自动审批");
            if (success) {
                logInfo("WORKFLOW_PRODUCER_APPROVED", String.format("制作人已审批步骤: %s (实例: %s)", step.getName(), instanceId));
                sendNotificationToAdmin("WORKFLOW_PRODUCER_APPROVED",
                    String.format("制作人已自动审批工作流步骤\n步骤: %s\n实例: %s", step.getName(), instanceId));
            } else {
                log.warn("制作人审批失败: step={}, instance={}", stepId, instanceId);
            }
        } else if ("HUMAN".equals(approvalLevel)) {
            // 通知管理员需要人工审批
            log.info("HUMAN级别审批，通知管理员: step={}, instance={}", stepId, instanceId);
            logInfo("WORKFLOW_HUMAN_APPROVAL_NEEDED", String.format("需要人工审批: %s (实例: %s)", step.getName(), instanceId));
            sendNotificationToAdmin("WORKFLOW_HUMAN_APPROVAL_NEEDED",
                String.format("【需要人工审批】\n\n步骤: %s\n实例: %s\n重要程度: %s\n\n请前往工作流管理页面审批。",
                    step.getName(), instanceId, step.getImportance()));
        }
    }

    /**
     * 解析报告中的完成信号，更新里程碑状态
     * 在 handleReport() 中直接调用，确保里程碑及时推进
     *
     * 【重要】不能完全相信AI报告，必须进行验证后才能标记完成
     *
     * @param fromAgent 发送者 Agent ID
     * @param content   报告内容
     * @return true 如果检测到完成信号并处理了里程碑
     */
    private boolean tryHandleCompletionReport(String fromAgent, String content) {
        String projectId = getProjectId();
        if (projectId == null || goalService == null) return false;

        String lowerContent = content.toLowerCase();

        // 检测完成信号关键词
        boolean hasCompletionSignal = lowerContent.contains("complete")
            || lowerContent.contains("完成")
            || lowerContent.contains("done")
            || lowerContent.contains("finished")
            || lowerContent.contains("通过")
            || lowerContent.contains("approved")
            || lowerContent.contains("design_complete")
            || lowerContent.contains("review_pass");

        if (!hasCompletionSignal) return false;

        // 从发送者 ID 中提取角色
        String senderRole = extractRoleFromAgentId(fromAgent);

        // 查找该角色当前 IN_PROGRESS 的里程碑
        GameProject.GoalMilestone milestone = findInProgressMilestoneForRole(projectId, senderRole);
        if (milestone == null) {
            log.debug("处理报告：未找到角色 {} 的进行中里程碑", senderRole);
            return false;
        }

        // 检查报告中是否包含质量评分
        int qualityScore = extractQualityScore(content);

        // 如果评分 >= 60 或无评分但有完成信号，进行验证
        if (qualityScore > 0 && qualityScore < 60) {
            log.info("处理报告：里程碑 [{}] 质量评分 {} 低于阈值，不自动完成",
                milestone.getTitle(), qualityScore);
            sendReplyToAgent(fromAgent,
                String.format("收到你的报告。质量评分 %d/100 低于通过标准(60分)，请改进后重新汇报。\n改进要求: %s",
                    qualityScore, extractImprovementSuggestion(content)));
            return true; // 已处理（拒绝）
        }

        // 【重要】进行验证，不能直接相信报告
        logInfo("VERIFICATION_START", String.format("开始验证里程碑 [%s] 的完成情况...", milestone.getTitle()));

        VerificationResult verificationResult = verifyMilestoneCompletion(projectId, milestone, fromAgent, content);

        if (verificationResult.passed) {
            // 验证通过，标记里程碑完成
            goalService.updateMilestoneProgress(projectId, milestone.getId(), 100);
            milestone.setVerificationResult("验证通过: " + verificationResult.details);
            milestone.setLastVerificationTime(java.time.LocalDateTime.now().toString());
            projectManager.saveProjectConfig(currentProject);

            logInfo("MILESTONE_VERIFIED_COMPLETED",
                String.format("里程碑 [%s] 验证通过并标记完成。验证详情: %s",
                    milestone.getTitle(), verificationResult.details));

            // 更新项目概况
            updateProjectOverviewAfterMilestone(milestone, true);

            // 回复 Agent 确认收到
            sendReplyToAgent(fromAgent,
                String.format("收到你的完成报告。里程碑 [%s] 已验证通过并标记完成！\n验证结果: %s",
                    milestone.getTitle(), verificationResult.details));
        } else {
            // 验证失败，要求返工
            milestone.setVerificationFailCount(milestone.getVerificationFailCount() + 1);
            milestone.setVerificationResult("验证失败: " + verificationResult.details);
            milestone.setLastVerificationTime(java.time.LocalDateTime.now().toString());
            projectManager.saveProjectConfig(currentProject);

            logInfo("MILESTONE_VERIFICATION_FAILED",
                String.format("里程碑 [%s] 验证失败（第%d次）。失败原因: %s",
                    milestone.getTitle(), milestone.getVerificationFailCount(), verificationResult.details));

            // 如果失败次数过多，发出告警
            if (milestone.getVerificationFailCount() >= 3) {
                logWarn("MILESTONE_VERIFICATION_ALERT",
                    String.format("里程碑 [%s] 已连续验证失败%d次，需要人工介入",
                        milestone.getTitle(), milestone.getVerificationFailCount()), null);
            }

            // 回复 Agent 要求返工
            sendReplyToAgent(fromAgent,
                String.format("收到你的完成报告，但验证未通过。请改进后重新汇报。\n\n" +
                    "验证失败原因:\n%s\n\n" +
                    "需要满足的验证标准:\n%s\n\n" +
                    "这是第%d次验证失败，请认真对待。",
                    verificationResult.details,
                    String.join("\n", milestone.getVerificationCriteria()),
                    milestone.getVerificationFailCount()));
        }

        return true;
    }

    /**
     * 验证结果类
     */
    private static class VerificationResult {
        boolean passed;
        String details;
        List<String> failedCriteria;

        VerificationResult(boolean passed, String details, List<String> failedCriteria) {
            this.passed = passed;
            this.details = details;
            this.failedCriteria = failedCriteria;
        }

        static VerificationResult success(String details) {
            return new VerificationResult(true, details, new ArrayList<>());
        }

        static VerificationResult failure(String details, List<String> failedCriteria) {
            return new VerificationResult(false, details, failedCriteria);
        }
    }

    /**
     * 验证里程碑是否真正完成
     * 【重要】不能完全相信AI报告，必须进行实际验证
     *
     * @param projectId 项目ID
     * @param milestone 里程碑对象
     * @param fromAgent 报告来源Agent
     * @param reportContent 报告内容
     * @return 验证结果
     */
    private VerificationResult verifyMilestoneCompletion(String projectId, GameProject.GoalMilestone milestone,
                                                          String fromAgent, String reportContent) {
        List<String> criteria = milestone.getVerificationCriteria();
        String role = milestone.getAssignedAgentRole();

        // 【重要】优先使用能力系统进行验证
        if (capabilityExecutionEngine != null && capabilityRegistry != null) {
            log.info("里程碑 [{}] 使用能力系统进行验证", milestone.getTitle());

            // 构建验证参数
            Map<String, Object> verifyParams = new HashMap<>();
            verifyParams.put("criteria", criteria);
            verifyParams.put("projectRoot", currentProject.getWorkDir());
            verifyParams.put("reportContent", reportContent);

            // 查找该角色的验证能力
            List<AgentCapability> verificationCaps = capabilityRegistry.getCapabilitiesByCategory("verification");
            if (verificationCaps != null && !verificationCaps.isEmpty()) {
                // 过滤出当前角色的能力
                List<AgentCapability> roleCaps = verificationCaps.stream()
                    .filter(cap -> role.equals(cap.getAgentRole()))
                    .collect(Collectors.toList());

                if (!roleCaps.isEmpty()) {
                    // 使用第一个可用的验证能力
                    AgentCapability verifyCap = roleCaps.get(0);
                    log.info("使用能力 [{}] 进行验证", verifyCap.getCapabilityName());

                    // 创建能力调用
                    CapabilityCall verifyCall = new CapabilityCall(verifyCap.getCapabilityName(), verifyParams, "里程碑验证");

                    // 执行验证能力
                    CapabilityResult result = capabilityExecutionEngine.executeCall(this, verifyCall);

                    if (result.isSuccess()) {
                        String resultData = result.getData() != null ? result.getData().toString() : "验证通过";
                        return VerificationResult.success("能力系统验证通过: " + resultData);
                    } else if (result.isFailed()) {
                        String error = result.getError() != null ? result.getError() : "验证失败";
                        return VerificationResult.failure("能力系统验证失败: " + error, criteria);
                    }
                }
            }

            // 如果没有专用验证能力，尝试使用通用验证能力
            CapabilityCall verifyCall = new CapabilityCall("verifyWithCriteria", verifyParams, "通用验证");
            CapabilityResult result = capabilityExecutionEngine.executeCall(this, verifyCall);

            if (result.isSuccess()) {
                String resultData = result.getData() != null ? result.getData().toString() : "验证通过";
                return VerificationResult.success("通用能力验证通过: " + resultData);
            } else if (result.isFailed()) {
                String error = result.getError() != null ? result.getError() : "验证失败";
                return VerificationResult.failure("通用能力验证失败: " + error, criteria);
            }
        }

        // 【兼容】如果没有能力系统或能力系统不可用，使用硬编码验证逻辑
        log.info("里程碑 [{}] 使用默认验证逻辑", milestone.getTitle());

        List<String> passedCriteria = new ArrayList<>();
        List<String> failedCriteria = new ArrayList<>();

        // 如果没有验证标准，使用默认验证
        if (criteria.isEmpty()) {
            log.info("里程碑 [{}] 没有定义验证标准，使用默认验证", milestone.getTitle());
            return verifyWithDefaultCriteria(projectId, milestone, reportContent);
        }

        log.info("验证里程碑 [{}]，共{}条验证标准", milestone.getTitle(), criteria.size());

        // 逐条验证
        for (String criterion : criteria) {
            boolean passed = verifySingleCriterion(projectId, milestone, criterion, reportContent);
            if (passed) {
                passedCriteria.add("✓ " + criterion);
            } else {
                failedCriteria.add("✗ " + criterion);
            }
        }

        // 构建验证详情
        StringBuilder details = new StringBuilder();
        details.append(String.format("通过 %d/%d 条标准\n", passedCriteria.size(), criteria.size()));

        if (!failedCriteria.isEmpty()) {
            details.append("未通过的标准:\n");
            failedCriteria.forEach(f -> details.append("  ").append(f).append("\n"));
        }

        boolean allPassed = failedCriteria.isEmpty();

        return allPassed ?
            VerificationResult.success(details.toString()) :
            VerificationResult.failure(details.toString(), failedCriteria);
    }

    /**
     * 使用默认标准验证里程碑
     * 当里程碑没有定义验证标准时使用
     *
     * @param projectId 项目ID
     * @param milestone 里程碑对象
     * @param reportContent 报告内容
     * @return 验证结果
     */
    private VerificationResult verifyWithDefaultCriteria(String projectId, GameProject.GoalMilestone milestone,
                                                          String reportContent) {
        List<String> passedCriteria = new ArrayList<>();
        List<String> failedCriteria = new ArrayList<>();

        String role = milestone.getAssignedAgentRole();
        String projectRoot = currentProject.getWorkDir();

        // 通用验证：使用 GameRuntimeVerifier 验证项目结构（适用于所有角色）
        if (gameRuntimeVerifier != null && projectRoot != null) {
            log.info("里程碑 [{}] 触发项目结构验证", milestone.getTitle());
            GameRuntimeVerifier.VerifyResult runtimeResult = gameRuntimeVerifier.verify(projectRoot);
            if (runtimeResult.isSuccess()) {
                passedCriteria.add("✓ 项目结构验证通过: " + runtimeResult.getMessage());
                if (runtimeResult.hasWarnings()) {
                    for (String warning : runtimeResult.getWarnings()) {
                        passedCriteria.add("⚠ " + warning);
                    }
                }
            } else {
                failedCriteria.add("✗ 项目结构验证失败: " + runtimeResult.getError());
            }
        }

        // 角色特定验证：根据角色职责检查产出质量
        switch (role) {
            case "system-planner":
            case "numerical-planner":
                // 策划类：检查是否有设计文档产出
                if (reportContent.contains("设计") || reportContent.contains("方案")
                    || reportContent.contains("文档") || reportContent.contains("design")) {
                    passedCriteria.add("✓ 报告包含设计内容");
                } else {
                    failedCriteria.add("✗ 报告缺少设计内容");
                }
                break;

            case "tester":
                // 测试类：检查是否有测试结果
                if (reportContent.contains("测试") || reportContent.contains("test")
                    || reportContent.contains("验证") || reportContent.contains("verify")) {
                    passedCriteria.add("✓ 报告包含测试/验证内容");
                } else {
                    failedCriteria.add("✗ 报告缺少测试/验证内容");
                }
                break;

            default:
                // 开发类和其他角色：检查报告质量
                if (reportContent.length() > 200) {
                    passedCriteria.add("✓ 报告内容详细");
                } else if (reportContent.length() > 50) {
                    passedCriteria.add("✓ 报告有一定内容");
                } else {
                    failedCriteria.add("✗ 报告内容过少");
                }
        }

        // 构建验证详情
        StringBuilder details = new StringBuilder();
        details.append(String.format("默认验证：通过 %d/%d 条标准\n", passedCriteria.size(),
            passedCriteria.size() + failedCriteria.size()));

        if (!failedCriteria.isEmpty()) {
            details.append("未通过的标准:\n");
            failedCriteria.forEach(f -> details.append("  ").append(f).append("\n"));
        }

        boolean allPassed = failedCriteria.isEmpty();

        return allPassed ?
            VerificationResult.success(details.toString()) :
            VerificationResult.failure(details.toString(), failedCriteria);
    }

    /**
     * 检查项目中是否有源代码文件（通用，不限定具体语言）
     */
    private boolean hasCodeFiles(String projectRoot) {
        try {
            java.io.File rootDir = new java.io.File(projectRoot);
            return hasFilesWithExtension(rootDir,
                ".java", ".py", ".js", ".ts", ".jsx", ".tsx", ".vue", ".svelte",
                ".c", ".cpp", ".h", ".hpp", ".cs", ".go", ".rs", ".rb", ".php",
                ".swift", ".kt", ".dart", ".lua", ".r", ".zig", ".nim", ".ex",
                ".gd", ".gdscript", ".shader", ".hlsl", ".glsl");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查项目中是否有文档文件
     */
    private boolean hasDocFiles(String projectRoot) {
        try {
            java.io.File rootDir = new java.io.File(projectRoot);
            return hasFilesWithExtension(rootDir, ".md", ".txt", ".doc", ".docx", ".pdf");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 递归检查目录中是否有指定扩展名的文件
     */
    private boolean hasFilesWithExtension(java.io.File dir, String... extensions) {
        if (!dir.exists() || !dir.isDirectory()) return false;

        java.io.File[] files = dir.listFiles();
        if (files == null) return false;

        for (java.io.File file : files) {
            if (file.isDirectory()) {
                // 跳过 node_modules 和 .git
                if (!file.getName().equals("node_modules") && !file.getName().equals(".git")) {
                    if (hasFilesWithExtension(file, extensions)) return true;
                }
            } else {
                String name = file.getName().toLowerCase();
                for (String ext : extensions) {
                    if (name.endsWith(ext)) return true;
                }
            }
        }
        return false;
    }

    /**
     * 验证单个标准
     *
     * @param projectId 项目ID
     * @param milestone 里程碑对象
     * @param criterion 验证标准
     * @param reportContent 报告内容
     * @return 是否通过
     */
    private boolean verifySingleCriterion(String projectId, GameProject.GoalMilestone milestone,
                                           String criterion, String reportContent) {
        String projectRoot = currentProject.getWorkDir();
        String lowerCriterion = criterion.toLowerCase();

        // 1. 检查文件是否存在
        if (lowerCriterion.contains(".js") || lowerCriterion.contains(".html") ||
            lowerCriterion.contains(".css") || lowerCriterion.contains(".json") ||
            lowerCriterion.contains(".md") || lowerCriterion.contains(".txt")) {

            // 提取文件路径
            String filePath = extractFilePath(criterion);
            if (filePath != null) {
                java.io.File file = new java.io.File(projectRoot, filePath);
                if (!file.exists()) {
                    log.info("验证失败：文件不存在 - {}", filePath);
                    return false;
                }
                // 检查文件是否为空
                if (file.length() == 0) {
                    log.info("验证失败：文件为空 - {}", filePath);
                    return false;
                }
                log.info("验证通过：文件存在且非空 - {}", filePath);
                return true;
            }
        }

        // 2. 检查报告中是否包含相关内容
        if (lowerCriterion.contains("实现") || lowerCriterion.contains("包含") ||
            lowerCriterion.contains("存在") || lowerCriterion.contains("输出")) {

            // 从报告中提取关键信息
            String keyword = extractKeyword(criterion);
            if (keyword != null && reportContent.contains(keyword)) {
                log.info("验证通过：报告中包含关键词 - {}", keyword);
                return true;
            }
        }

        // 3. 检查代码中的方法/函数是否存在
        if (lowerCriterion.contains("方法") || lowerCriterion.contains("函数") ||
            lowerCriterion.contains("function") || lowerCriterion.contains("method")) {

            String methodName = extractMethodName(criterion);
            if (methodName != null) {
                // 在项目中搜索该方法
                boolean found = searchMethodInProject(projectRoot, methodName);
                if (found) {
                    log.info("验证通过：方法存在 - {}", methodName);
                    return true;
                } else {
                    log.info("验证失败：方法不存在 - {}", methodName);
                    return false;
                }
            }
        }

        // 4. 对于策划类任务，检查文档内容
        if (milestone.getAssignedAgentRole().equals("system-planner") ||
            milestone.getAssignedAgentRole().equals("numerical-planner")) {

            // 检查报告中是否包含必要的内容
            if (reportContent.length() > 100) {  // 报告有一定长度
                log.info("验证通过：策划报告有足够内容");
                return true;
            }
        }

        // 5. 默认：检查报告中是否提到了这个标准
        if (reportContent.contains(criterion) || reportContent.contains(criterion.replace("；", ";"))) {
            log.info("验证通过：报告中提到了标准 - {}", criterion);
            return true;
        }

        log.info("验证失败：无法验证标准 - {}", criterion);
        return false;
    }

    /**
     * 从标准中提取文件路径
     */
    private String extractFilePath(String criterion) {
        // 匹配 src/xxx/xxx.js 或 docs/xxx.md 等格式
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(src/[\\w/]+\\.[\\w]+|docs/[\\w/]+\\.[\\w]+)").matcher(criterion);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 从标准中提取关键词
     */
    private String extractKeyword(String criterion) {
        // 提取引号中的内容或关键名词
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("[\"'](.*?)[\"']").matcher(criterion);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // 提取"包含"后面的内容
        matcher = java.util.regex.Pattern.compile("包含(.+?)(?:；|;|$)").matcher(criterion);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

    /**
     * 从标准中提取方法名
     */
    private String extractMethodName(String criterion) {
        // 匹配 checkMatch() 或 removeTiles() 等格式
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\w+)\\s*\\(\\)").matcher(criterion);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 在项目中搜索方法
     */
    private boolean searchMethodInProject(String projectRoot, String methodName) {
        try {
            java.io.File rootDir = new java.io.File(projectRoot);
            return searchInDirectory(rootDir, methodName);
        } catch (Exception e) {
            log.warn("搜索方法时出错: {}", e.getMessage());
            return false;
        }
    }

    private boolean searchInDirectory(java.io.File dir, String methodName) {
        if (!dir.exists() || !dir.isDirectory()) return false;

        java.io.File[] files = dir.listFiles();
        if (files == null) return false;

        for (java.io.File file : files) {
            if (file.isDirectory()) {
                // 跳过 node_modules 和 .git
                if (!file.getName().equals("node_modules") && !file.getName().equals(".git")) {
                    if (searchInDirectory(file, methodName)) return true;
                }
            } else if (file.getName().endsWith(".js") || file.getName().endsWith(".ts")) {
                // 搜索方法定义
                try {
                    String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
                    if (content.contains("function " + methodName) ||
                        content.contains(methodName + "(") ||
                        content.contains("def " + methodName)) {
                        return true;
                    }
                } catch (Exception e) {
                    // 忽略读取错误
                }
            }
        }
        return false;
    }

    /**
     * 从 Agent 运行时 ID 中提取角色
     * 格式: projectId:role → role
     */
    private String extractRoleFromAgentId(String agentId) {
        if (agentId == null) return "";
        int lastColon = agentId.lastIndexOf(':');
        return lastColon > 0 ? agentId.substring(lastColon + 1) : agentId;
    }

    /**
     * 查找指定角色当前 IN_PROGRESS 的里程碑
     */
    private GameProject.GoalMilestone findInProgressMilestoneForRole(String projectId, String role) {
        List<GameProject.GoalMilestone> milestones = goalService.getMilestones(projectId);
        return milestones.stream()
            .filter(m -> m.getStatus() == GameProject.MilestoneStatus.IN_PROGRESS)
            .filter(m -> role.equals(m.getAssignedAgentRole()))
            .findFirst()
            .orElse(null);
    }

    /**
     * 从报告内容中提取质量评分
     * 支持格式: "85/100"、"评分: 85"、"score: 85"、"质量: 85分"
     *
     * @return 评分 (0-100)，未找到返回 0
     */
    private int extractQualityScore(String content) {
        if (content == null) return 0;
        // 匹配 "85/100" 格式
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d{1,3})\\s*/\\s*100").matcher(content);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        // 匹配 "评分: 85" 或 "score: 85"
        matcher = java.util.regex.Pattern.compile("(?:评分|score|质量)[:：]?\\s*(\\d{1,3})").matcher(content);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }

    /**
     * 从报告内容中提取改进建议
     */
    private String extractImprovementSuggestion(String content) {
        if (content == null) return "请提升质量后重新汇报";
        // 尝试找到 "建议" 或 "improvement" 后面的内容
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(?:建议|改进|improvement)[:：]?\\s*(.{10,100})").matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "请提升质量后重新汇报";
    }

    /**
     * 向指定 Agent 发送回复消息
     */
    private void sendReplyToAgent(String targetAgentId, String content) {
        AgentMessage reply = AgentMessage.builder()
            .fromAgentId(getId())
            .toAgentId(targetAgentId)
            .type(AgentMessage.MessageType.RESPONSE)
            .content(content)
            .build();
        sendMessage(reply);
        log.debug("已回复 Agent {}: {}", targetAgentId,
            content.length() > 80 ? content.substring(0, 80) + "..." : content);
    }

    /**
     * 构建工作上下文
     * 收集项目信息、团队状态、目标进度、待处理消息等，供 AI 决策使用
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

            // 游戏模板匹配（作为参考，不是约束）
            if (gameTemplateService != null && currentProject.hasGoal()) {
                List<GameTemplateService.GameTemplate> matched = gameTemplateService.matchTemplates(currentProject.getGoal());
                if (!matched.isEmpty()) {
                    GameTemplateService.GameTemplate best = matched.get(0);
                    ctx.append("## 参考模板（仅供参考，可自由发挥）\n");
                    ctx.append("- 类型: ").append(best.getName()).append("\n");
                    ctx.append("- 描述: ").append(best.getDescription()).append("\n");
                    ctx.append("\n**重要**: 以下是同类游戏的参考方案，不是必须遵循的规范。");
                    ctx.append("你应该根据项目目标和团队能力，选择最适合的实现方式。");
                    ctx.append("如果参考模板的方案不够好，大胆改进或替换。\n\n");
                    if (best.getContent() != null && !best.getContent().isEmpty()) {
                        String content = best.getContent();
                        ctx.append("参考内容:\n").append(content.length() > 2000 ? content.substring(0, 2000) + "..." : content).append("\n");
                    }
                    ctx.append("\n");
                }
            }
        }

        // 团队状态（含详细信息）
        String projectId = getProjectId();
        if (projectId != null) {
            List<Agent> agents = agentManager.getAgentsByProject(projectId);
            ctx.append("## 团队成员\n");
            if (agents.isEmpty()) {
                ctx.append("（暂无团队成员）\n");
            } else {
                for (Agent agent : agents) {
                    String status = agent.isBusy() ? "🔴 忙碌" : (agent.isAlive() ? "🟢 空闲" : "⚫ 离线");
                    ctx.append(String.format("- %s (%s): %s", agent.getName(), agent.getRole(), status));
                    // 显示任务追踪信息
                    TaskTracker tracker = assignedTasks.get(agent.getId());
                    if (tracker != null) {
                        ctx.append(String.format(" | 当前任务: %s (已耗时 %d 分钟)", tracker.taskTitle, tracker.getElapsedMinutes()));
                    }
                    ctx.append("\n");
                }
            }
            ctx.append("\n");

            // 里程碑统计
            if (goalService != null) {
                List<GameProject.GoalMilestone> milestones = goalService.getMilestones(projectId);
                if (milestones != null && !milestones.isEmpty()) {
                    long completed = milestones.stream().filter(m -> m.getStatus() == GameProject.MilestoneStatus.COMPLETED).count();
                    long inProgress = milestones.stream().filter(m -> m.getStatus() == GameProject.MilestoneStatus.IN_PROGRESS).count();
                    long blocked = milestones.stream().filter(m -> m.getStatus() == GameProject.MilestoneStatus.BLOCKED).count();
                    long pending = milestones.stream().filter(m -> m.getStatus() == GameProject.MilestoneStatus.PENDING).count();
                    ctx.append("## 里程碑统计\n");
                    ctx.append(String.format("- 总数: %d | 已完成: %d | 进行中: %d | 阻塞: %d | 待开始: %d\n",
                        milestones.size(), completed, inProgress, blocked, pending));
                    double progress = milestones.isEmpty() ? 0 : (double) completed / milestones.size() * 100;
                    ctx.append(String.format("- 完成率: %.1f%%\n\n", progress));

                    // 显示阻塞的里程碑详情
                    if (blocked > 0) {
                        ctx.append("### 阻塞的里程碑（需要人工介入）\n");
                        milestones.stream()
                            .filter(m -> m.getStatus() == GameProject.MilestoneStatus.BLOCKED)
                            .forEach(m -> {
                                ctx.append(String.format("- [%s] %s — 阻塞原因: %s\n",
                                    m.getAssignedAgentRole(), m.getTitle(),
                                    m.getBlockedReason() != null ? m.getBlockedReason() : "未知"));
                            });
                        ctx.append("\n");
                    }
                }
            }
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

        // 任务追踪状态
        if (!assignedTasks.isEmpty()) {
            ctx.append("## 任务追踪\n");
            ctx.append("以下是你之前分配的任务，正在追踪执行情况：\n\n");
            assignedTasks.forEach((agentId, tracker) -> {
                Agent agent = agentManager.getAgent(agentId);
                String agentStatus = agent != null ? (agent.isBusy() ? "执行中" : "已完成/空闲") : "未知";
                String urgency = tracker.getElapsedMinutes() > 30 ? "⚠️ 可能卡住" : "";
                ctx.append(String.format("- %s → %s | 耗时 %d 分钟 | 状态: %s %s\n",
                    agentId, tracker.taskTitle, tracker.getElapsedMinutes(), agentStatus, urgency));
            });
            ctx.append("\n");
        }

        // 本轮收到的团队报告（processPendingMessages 已处理，这里展示摘要给 AI）
        if (!currentCycleReports.isEmpty()) {
            ctx.append("## 本轮收到的团队报告\n");
            ctx.append("以下是本轮工作周期中收到的团队报告，已处理完毕，请据此决策下一步行动：\n\n");
            for (String report : currentCycleReports) {
                ctx.append("- ").append(report).append("\n");
            }
            ctx.append("\n");
        }

        // 待处理的招聘拒绝决策（管理员拒绝了你的招聘请求，你需要分析原因并决策）
        if (!pendingRejectionDecisions.isEmpty()) {
            ctx.append("## 待处理的招聘拒绝决策\n\n");
            ctx.append("**重要：管理员拒绝了你的招聘请求，请仔细分析拒绝原因，做出合理决策。**\n\n");
            for (PendingRejectionDecision decision : pendingRejectionDecisions) {
                // 查找受影响的里程碑
                List<GameProject.GoalMilestone> affected = goalService != null
                    ? goalService.getMilestones(projectId).stream()
                        .filter(m -> decision.role.equals(m.getAssignedAgentRole()))
                        .filter(m -> m.getStatus() != GameProject.MilestoneStatus.COMPLETED)
                        .toList()
                    : List.of();
                ctx.append(String.format("- 角色: %s\n  拒绝原因: %s\n  受影响里程碑: %d 个\n",
                    decision.role, decision.reason, affected.size()));
                for (GameProject.GoalMilestone m : affected) {
                    ctx.append(String.format("    - [%s] %s (状态: %s)\n",
                        m.getAssignedAgentRole(), m.getTitle(), m.getStatus()));
                }
            }
            ctx.append("\n");
            // 清空队列（已展示给 AI，由 AI 决策后处理）
            pendingRejectionDecisions.clear();
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

        // 历史审批驳回经验（从记忆中加载）
        Map<String, String> allExperiences = loadAllExperiences();
        List<String> rejections = allExperiences.entrySet().stream()
            .filter(e -> e.getKey().startsWith("approval_rejected_"))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
        if (!rejections.isEmpty()) {
            ctx.append("## 历史审批驳回记录\n\n");
            ctx.append("以下是之前被管理员驳回的审批，请在决策时参考这些教训，避免重复犯错：\n\n");
            int count = 0;
            for (String rej : rejections) {
                if (count >= 5) break; // 最多显示5条
                ctx.append("- ").append(rej.length() > 200 ? rej.substring(0, 200) + "..." : rej).append("\n");
                count++;
            }
            ctx.append("\n");
        }

        // 历史干预指令记录（从记忆中加载）
        List<String> interventions = allExperiences.entrySet().stream()
            .filter(e -> e.getKey().startsWith("intervention_"))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
        if (!interventions.isEmpty()) {
            ctx.append("## 历史干预指令（必须遵守）\n\n");
            ctx.append("**管理员已通过干预指令明确了方向，你必须在后续决策中严格遵守：**\n\n");
            for (String intervention : interventions) {
                ctx.append("- ").append(intervention).append("\n");
            }
            ctx.append("\n");
        }

        // 待执行的干预指令（从工作记忆中加载）
        if (agentContext != null) {
            Map<String, String> workingMemory = agentContext.getAllWorkingMemory();
            List<String> activeInterventions = workingMemory.entrySet().stream()
                .filter(e -> e.getKey().startsWith("active_intervention_"))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
            if (!activeInterventions.isEmpty()) {
                ctx.append("## 待执行的干预指令（当前工作周期必须处理）\n\n");
                ctx.append("**管理员已发出干预指令，你必须在本轮工作中立即执行：**\n\n");
                for (String intervention : activeInterventions) {
                    ctx.append("- ").append(intervention).append("\n");
                }
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

        prompt.append("# 角色：项目制作人（Producer）\n\n");
        prompt.append("你是游戏项目的核心管理者，相当于真实游戏公司的制作人。你的职责是：\n");
        prompt.append("- 统筹全局，确保项目按时、高质量交付\n");
        prompt.append("- 协调团队，让每个成员发挥最大价值\n");
        prompt.append("- 管理风险，提前发现并解决问题\n");
        prompt.append("- 做出决策，推动项目持续前进\n\n");

        prompt.append("## 当前项目状态\n\n");
        prompt.append(context);

        prompt.append("\n## 决策框架\n\n");
        prompt.append("每次工作周期，你需要按以下顺序思考和行动：\n\n");

        prompt.append("### 第一步：态势感知\n");
        prompt.append("先理解当前发生了什么：\n");
        prompt.append("- 项目整体进展如何？里程碑完成率是多少？\n");
        prompt.append("- 团队成员各自在做什么？有没有人空闲或卡住？\n");
        prompt.append("- 有没有收到团队报告？报告中有什么关键信息？\n");
        prompt.append("- 有没有待处理的审批？\n\n");

        prompt.append("### 第二步：风险识别\n");
        prompt.append("主动发现潜在问题：\n");
        prompt.append("- 有没有里程碑被阻塞超过预期时间？\n");
        prompt.append("- 有没有 Agent 长时间没有产出？\n");
        prompt.append("- 依赖关系是否合理？有没有循环依赖？\n");
        prompt.append("- 资源是否充足？是否需要招聘新角色？\n");
        prompt.append("- 技术方案是否可行？有没有需要调整的地方？\n\n");

        prompt.append("### 第三步：决策与行动\n");
        prompt.append("基于态势和风险分析，选择最有效的行动：\n");
        prompt.append("- 优先推进阻塞最严重的里程碑\n");
        prompt.append("- 为空闲 Agent 分配有意义的任务\n");
        prompt.append("- 对卡住的 Agent 发送催促或重新分配任务\n");
        prompt.append("- 必要时调整计划或招聘新成员\n\n");

        prompt.append("### 第四步：质量把关\n");
        prompt.append("确保交付质量：\n");
        prompt.append("- 审查团队报告，判断工作质量是否达标\n");
        prompt.append("- 对质量不达标的工作，要求返工或给出改进建议\n");
        prompt.append("- 记录质量问题到知识库，避免重复犯错\n\n");

        prompt.append("## 核心能力\n\n");

        prompt.append("### 目标管理\n");
        prompt.append("- setProjectGoal: 设定项目目标\n");
        prompt.append("- decomposeGoal: 分解目标为里程碑\n\n");

        prompt.append("### 团队管理\n");
        prompt.append("- sendTaskToAgent: 向指定 Agent 分配任务\n");
        prompt.append("- requestRecruit: 发起招聘审批\n");
        prompt.append("- evaluateAgent / batchEvaluateTeam: 评估团队绩效\n\n");

        prompt.append("### 工作流管理\n");
        prompt.append("- selectWorkflow / createWorkflow / startWorkflow: 管理工作流\n\n");

        prompt.append("### 监控与优化\n");
        prompt.append("- getProjectStatus: 检查项目状态\n");
        prompt.append("- alertRisk: 预警风险\n");
        prompt.append("- escalateStrategicDecision: 升级重大决策到人工审批\n\n");

        prompt.append("### 项目概况管理\n");
        prompt.append("- updateProjectOverview: 更新项目概况（包含部署信息、运行状态等）\n");
        prompt.append("  **重要**: 当项目有实际运行的服务（如开发服务器、已部署的应用）时，");
        prompt.append("必须调用此能力更新概况，包含端口号、访问地址、运行状态(running=true)。\n\n");

        prompt.append("## 决策原则\n\n");
        prompt.append("1. **主动推进**：不要等待指令，主动发现问题并解决\n");
        prompt.append("2. **效率优先**：多个 Agent 可以并行工作时，同时分配任务\n");
        prompt.append("3. **质量第一**：宁可慢一点也要保证质量，避免返工\n");
        prompt.append("4. **风险前置**：提前识别风险并预警，不要等问题爆发\n");
        prompt.append("5. **学习改进**：从每次失败中学习，更新知识库\n");
        prompt.append("6. **数据一致**：当发现实际完成情况与里程碑进度不一致时，必须立即调用 updateMilestone 修复数据，不要只报告问题不解决\n\n");

        prompt.append("## 【重要】发现问题必须立即行动\n\n");
        prompt.append("作为制作人，你的职责不只是发现问题，更要解决问题。当你在态势感知中发现以下情况时，必须立即采取行动：\n\n");
        prompt.append("- **数据不同步**：实际已完成但里程碑进度未更新 → 调用 updateMilestone 更新进度\n");
        prompt.append("- **卡住的 Agent**：Agent 长时间无产出 → 调用 sendTaskToAgent 重新分配或催促\n");
        prompt.append("- **阻塞的里程碑**：里程碑被阻塞 → 分析原因并解除阻塞\n");
        prompt.append("- **质量问题**：团队产出不达标 → 要求返工或给出改进建议\n\n");
        prompt.append("**不要只在决策中描述问题，而不采取行动解决问题。描述问题只是第一步，解决问题才是你的核心职责。**\n\n");

        prompt.append("## 招聘拒绝处理\n\n");
        prompt.append("当上下文中出现[待处理的招聘拒绝决策]时，说明管理员拒绝了你的招聘请求。\n");
        prompt.append("**你必须仔细阅读管理员的拒绝原因，并据此做出决策：**\n\n");
        prompt.append("- 如果拒绝原因表明该角色确实不需要（如'不需要'、'没必要'、'跳过'等），");
        prompt.append("则将该角色的所有未完成里程碑标记为完成（使用 skipMilestone 能力），项目继续推进\n");
        prompt.append("- 如果拒绝原因给出了替代方案（如'用 client-dev 代替'、'先做别的'），");
        prompt.append("则按管理员指示调整计划\n");
        prompt.append("- 如果拒绝原因不明确，可以请求管理员进一步说明\n\n");
        prompt.append("**不要忽略拒绝原因，也不要重复发起被拒绝的招聘。**\n\n");

        prompt.append("### 可用的里程碑管理能力\n");
        prompt.append("- skipMilestone: 跳过里程碑（当管理员判定不需要时）\n");
        prompt.append("- blockMilestone: 阻塞里程碑（需要人工进一步指示时）\n\n");

        prompt.append("## 重大决策升级\n\n");
        prompt.append("以下决策需要升级到人工审批（使用 escalateStrategicDecision）：\n");
        prompt.append("- PROJECT_DIRECTION: 项目方向重大调整\n");
        prompt.append("- GAMEPLAY_CHANGE: 核心玩法重大变更\n");
        prompt.append("- MAJOR_GAMEPLAY_DESIGN: 新增核心系统（PvP、交易、公会等）\n");
        prompt.append("- ARCHITECTURE_CHANGE: 技术架构重大变更\n");
        prompt.append("- BUDGET_ALLOCATION / TEAM_RESTRUCTURE / TECHNOLOGY_STACK / RELEASE_STRATEGY\n\n");
        prompt.append("小调整（数值平衡、UI优化、Bug修复）可自主决策。\n\n");

        prompt.append("## 输出要求\n\n");
        prompt.append("请分析当前状态，按照决策框架逐步思考，然后执行最有效的行动。\n");
        prompt.append("可以同时执行多个操作。如果当前没有需要处理的事项，说明原因即可。\n");

        return prompt.toString();
    }

    /**
     * 硬编码兜底：确保关键业务逻辑不被遗漏
     * 如果 AI 未调用相关能力，自动触发
     */
    private void ensureCriticalActions() {
        String projectId = getProjectId();
        if (projectId == null || currentProject == null) return;

        // 审批等待中：跳过所有里程碑推进逻辑
        if (pendingApprovalMilestoneId != null) {
            log.debug("有审批等待中: {}，跳过里程碑推进", pendingApprovalMilestoneId);
            return;
        }

        // 如果项目有目标但状态为 NOT_STARTED，自动触发目标分解
        if (currentProject.hasGoal() && currentProject.getGoalStatus() == GameProject.GoalStatus.NOT_STARTED
            && goalService != null) {
            log.info("兜底：目标未开始，自动触发目标分解");
            decomposeGoal();
        }

        // 如果有可执行的里程碑，自主寻找并启动工作流
        if (goalService != null && currentProject.getGoalStatus() == GameProject.GoalStatus.IN_PROGRESS) {
            // 如果已有工作流在运行，不重复启动
            if (pendingWorkflowInstanceId != null && workflowEngine != null) {
                WorkflowEngine.WorkflowInstance running = workflowEngine.getInstance(pendingWorkflowInstanceId);
                if (running != null && (running.getStatus() == WorkflowEngine.WorkflowStatus.RUNNING
                    || running.getStatus() == WorkflowEngine.WorkflowStatus.PAUSED)) {
                    log.info("已有工作流运行中: {}，跳过启动", pendingWorkflowInstanceId);
                } else {
                    // 工作流已完成或失败，清除引用
                    log.info("工作流 {} 已结束，状态: {}，准备推进下一阶段",
                        pendingWorkflowInstanceId, running != null ? running.getStatus() : "未知");
                    pendingWorkflowInstanceId = null;
                }
            }

            // 【优化】先检查是否有卡住的 IN_PROGRESS 里程碑需要推进
            checkAndProgressStuckMilestones(projectId);

            // 里程碑推进
            if (pendingWorkflowInstanceId == null) {
                GameProject.GoalMilestone next = goalService.getNextExecutableMilestone(projectId);
                if (next != null) {
                    // 检查该里程碑是否已被拒绝，避免重复发起审批
                    if (rejectedMilestones.contains(next.getId())) {
                        log.debug("里程碑 [{}] 之前已被拒绝，跳过", next.getTitle());
                        return;
                    }

                    String targetAgentId = projectId + ":" + next.getAssignedAgentRole();
                    Agent targetAgent = agentManager.getAgent(targetAgentId);

                    if (targetAgent != null && !targetAgent.isBusy()) {
                        log.info("兜底：有可执行里程碑 [{}]，自主寻找合适工作流", next.getTitle());
                        autoSelectAndStartWorkflow(next);
                        // 追踪任务分配
                        assignedTasks.put(targetAgentId, new TaskTracker(next.getTitle(), next.getId(), next.getProgress()));
                    } else if (targetAgent != null && targetAgent.isBusy()) {
                        // Agent 正在忙，检查是否卡住太久
                        TaskTracker tracker = assignedTasks.get(targetAgentId);
                        if (tracker != null && tracker.getElapsedMinutes() > 30) {
                            log.info("Agent {} 已忙碌超过30分钟，可能卡住: {}", targetAgent.getName(), tracker.taskTitle);
                            logWarn("TASK_STUCK", String.format("Agent %s 任务可能卡住: %s（已耗时 %d 分钟）",
                                targetAgent.getName(), tracker.taskTitle, tracker.getElapsedMinutes()), null);
                        }
                    } else if (targetAgent == null) {
                        String role = next.getAssignedAgentRole();
                        // 检查招聘是否已被拒绝
                        if (hasRejectedRecruitForRole(projectId, role)) {
                            // 读取拒绝原因，存入待决策队列交给 AI 分析
                            String rejectionReason = getRecruitRejectionReason(projectId, role);
                            log.info("里程碑 [{}] 角色 {} 招聘被拒，原因: {}", next.getTitle(), role, rejectionReason);
                            handleRecruitRejection(role, rejectionReason);
                            // 将里程碑标记为 BLOCKED，避免下一周期重复检测为 PENDING
                            if (next.getStatus() != GameProject.MilestoneStatus.BLOCKED) {
                                next.setStatus(GameProject.MilestoneStatus.BLOCKED);
                                next.setBlockedReason("角色 " + role + " 的招聘被拒绝，等待制作人决策。原因: " + rejectionReason);
                                projectManager.saveProjectConfig(currentProject);
                            }
                        } else {
                            log.info("里程碑 [{}] 需要角色 {}，但无对应 Agent，发起招聘", next.getTitle(), role);
                            requestRecruitApproval(role, next.getTitle());
                        }
                    }
                } else {
                    // 没有可执行的里程碑，检查是否所有里程碑都已完成
                    checkGoalCompletion(projectId);
                }
            }
        }
    }

    /**
     * 检查并推进卡住的 IN_PROGRESS 里程碑
     * 深度优化：多维度检测卡住原因，智能处理
     */
    private void checkAndProgressStuckMilestones(String projectId) {
        if (goalService == null || currentProject == null) return;

        List<GameProject.GoalMilestone> inProgressMilestones = currentProject.getInProgressMilestones();
        if (inProgressMilestones.isEmpty()) return;

        for (GameProject.GoalMilestone milestone : inProgressMilestones) {
            String targetAgentId = projectId + ":" + milestone.getAssignedAgentRole();
            Agent targetAgent = agentManager.getAgent(targetAgentId);

            // 1. 检查任务完成情况 - 智能推进
            checkAndAutoCompleteMilestone(projectId, milestone);

            // 2. 检查 Agent 存活状态
            if (targetAgent == null) {
                log.info("里程碑 [{}] 负责角色 {} 无对应 Agent，发起招聘", milestone.getTitle(), milestone.getAssignedAgentRole());
                requestRecruitApproval(milestone.getAssignedAgentRole(), milestone.getTitle());
                continue;
            }

            // 3. 检查任务超时（任何进度都检查）
            checkTaskTimeout(projectId, milestone, targetAgent);

            // 4. 检查是否卡住（进度低于20%且有任务，原来10%太严格）
            if (milestone.getProgress() < 20 && !milestone.getTasks().isEmpty()) {
                handleStuckMilestone(projectId, milestone, targetAgent, targetAgentId);
            }

            // 5. 尝试并行推进多个里程碑
            tryParallelMilestoneProgress(projectId, milestone);
        }
    }

    /**
     * 检查并自动完成里程碑
     * 当所有任务都完成时，自动标记里程碑完成
     */
    private void checkAndAutoCompleteMilestone(String projectId, GameProject.GoalMilestone milestone) {
        if (milestone.getTasks().isEmpty()) return;

        long completedTasks = milestone.getTasks().stream()
            .filter(t -> t.getStatus() == GameProject.MilestoneStatus.COMPLETED)
            .count();
        long totalTasks = milestone.getTasks().size();

        // 所有任务都完成了，但里程碑没标记为完成
        if (totalTasks > 0 && completedTasks == totalTasks && milestone.getStatus() != GameProject.MilestoneStatus.COMPLETED) {
            log.info("里程碑 [{}] 所有任务已完成 ({}/{})，自动标记完成",
                milestone.getTitle(), completedTasks, totalTasks);

            // 设置验证结果并标记完成
            milestone.setVerificationResult("自动验证：所有任务已完成 (" + completedTasks + "/" + totalTasks + ")");
            milestone.setLastVerificationTime(java.time.LocalDateTime.now().toString());
            goalService.updateMilestoneProgress(projectId, milestone.getId(), 100);

            logInfo("MILESTONE_AUTO_COMPLETED", String.format(
                "里程碑 [%s] 自动完成（%d/%d 任务完成）", milestone.getTitle(), completedTasks, totalTasks));

            // 清除任务追踪
            String targetAgentId = projectId + ":" + milestone.getAssignedAgentRole();
            assignedTasks.remove(targetAgentId);

            // 触发版本评估
            triggerVersionEvaluation(milestone);

            // 自动触发游戏验证（验证"好不好玩"）
            triggerGameVerification(projectId);
        }

        // 部分任务完成时，更新进度
        if (completedTasks > 0 && completedTasks < totalTasks) {
            int expectedProgress = (int) ((double) completedTasks / totalTasks * 100);
            if (milestone.getProgress() < expectedProgress - 10) {
                // 进度落后于任务完成率，触发重新计算
                log.info("里程碑 [{}] 进度落后于任务完成率: {}% vs {}%，重新计算",
                    milestone.getTitle(), milestone.getProgress(), expectedProgress);
                milestone.setProgress(expectedProgress);
                projectManager.saveProjectConfig(currentProject);
            }
        }
    }

    /**
     * 检查任务超时
     * 超时的任务自动标记为失败或重新分配
     * 不再限制进度阈值，任何进度的卡住都能检测
     */
    private void checkTaskTimeout(String projectId, GameProject.GoalMilestone milestone, Agent targetAgent) {
        TaskTracker tracker = assignedTasks.get(targetAgent.getId());
        if (tracker == null) return;

        long elapsedMinutes = tracker.getElapsedMinutes();

        // 超时阈值：45分钟（原来60分钟太长）
        if (elapsedMinutes > 45) {
            // 检查进度是否有增长：如果分配后进度没变化，说明卡住了
            if (milestone.getProgress() <= tracker.initialProgress) {
                logWarn("TASK_TIMEOUT", String.format(
                    "任务超时: 里程碑 [%s]，Agent: %s，已耗时 %d 分钟，进度: %d%%（分配时: %d%%）",
                    milestone.getTitle(), targetAgent.getName(), elapsedMinutes,
                    milestone.getProgress(), tracker.initialProgress), null);

                // 如果 Agent 空闲但任务没进展，重新分配
                if (!targetAgent.isBusy()) {
                    log.info("Agent {} 空闲但任务无进展，重新分配", targetAgent.getName());

                    // 重置任务状态
                    for (GameProject.MilestoneTask task : milestone.getTasks()) {
                        if (task.getStatus() == GameProject.MilestoneStatus.IN_PROGRESS) {
                            task.setStatus(GameProject.MilestoneStatus.PENDING);
                        }
                    }
                    projectManager.saveProjectConfig(currentProject);

                    // 重新启动工作流
                    autoSelectAndStartWorkflow(milestone);
                    assignedTasks.put(targetAgent.getId(), new TaskTracker(milestone.getTitle(), milestone.getId(), milestone.getProgress()));
                } else {
                    // Agent 忙碌但进度没增长，发送催促
                    log.info("Agent {} 忙碌但进度未增长，发送催促", targetAgent.getName());
                    try {
                        String urgePrompt = String.format(
                            "请尽快完成当前任务「%s」，已耗时 %d 分钟。如果遇到问题请报告。",
                            milestone.getTitle(), elapsedMinutes);
                        // 轻量催促，不打断当前工作
                        logInfo("TASK_URGE", String.format("催促 Agent %s 完成: %s", targetAgent.getName(), milestone.getTitle()));
                    } catch (Exception e) {
                        log.debug("催促消息发送失败: {}", e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * 处理卡住的里程碑
     */
    private void handleStuckMilestone(String projectId, GameProject.GoalMilestone milestone,
                                       Agent targetAgent, String targetAgentId) {
        log.info("检测到卡住的里程碑: {} (进度: {}%, 任务数: {})",
            milestone.getTitle(), milestone.getProgress(), milestone.getTasks().size());

        if (!targetAgent.isBusy()) {
            // Agent 空闲，重新分配任务
            log.info("里程碑 [{}] 负责 Agent 空闲，重新分配任务", milestone.getTitle());

            // 检查任务状态，将 PENDING 任务设为 IN_PROGRESS
            boolean hasProgressed = false;
            for (GameProject.MilestoneTask task : milestone.getTasks()) {
                if (task.getStatus() == GameProject.MilestoneStatus.PENDING) {
                    task.setStatus(GameProject.MilestoneStatus.IN_PROGRESS);
                    hasProgressed = true;
                }
            }

            if (hasProgressed) {
                projectManager.saveProjectConfig(currentProject);
                log.info("里程碑 [{}] 任务状态已更新", milestone.getTitle());
            }

            // 启动工作流或直接分配任务
            autoSelectAndStartWorkflow(milestone);
            assignedTasks.put(targetAgentId, new TaskTracker(milestone.getTitle(), milestone.getId(), milestone.getProgress()));
        } else {
            // Agent 忙碌，检查是否卡住太久
            TaskTracker tracker = assignedTasks.get(targetAgentId);
            if (tracker != null && tracker.getElapsedMinutes() > 60) {
                logWarn("MILESTONE_STUCK", String.format(
                    "里程碑 [%s] 卡住超过60分钟，负责 Agent: %s，进度: %d%%",
                    milestone.getTitle(), targetAgent.getName(), milestone.getProgress()), null);

                // 通知管理员
                sendNotificationToAdmin("MILESTONE_STUCK",
                    String.format("里程碑 [%s] 可能卡住\n负责角色: %s\n当前进度: %d%%\n已耗时: %d 分钟",
                        milestone.getTitle(), milestone.getAssignedAgentRole(),
                        milestone.getProgress(), tracker.getElapsedMinutes()));
            }
        }
    }

    /**
     * 尝试并行推进多个里程碑
     * 当有多个空闲 Agent 时，可以同时推进多个里程碑
     */
    private void tryParallelMilestoneProgress(String projectId, GameProject.GoalMilestone currentMilestone) {
        if (goalService == null) return;

        // 获取所有待执行的里程碑
        List<GameProject.GoalMilestone> allMilestones = goalService.getMilestones(projectId);
        List<GameProject.GoalMilestone> pendingMilestones = allMilestones.stream()
            .filter(m -> m.getStatus() == GameProject.MilestoneStatus.PENDING)
            .filter(m -> m.areDependenciesMet(allMilestones))
            .filter(m -> !m.getId().equals(currentMilestone.getId())) // 排除当前里程碑
            .collect(Collectors.toList());

        if (pendingMilestones.isEmpty()) return;

        // 检查是否有空闲的 Agent 可以并行处理
        for (GameProject.GoalMilestone pending : pendingMilestones) {
            String agentId = projectId + ":" + pending.getAssignedAgentRole();
            Agent agent = agentManager.getAgent(agentId);

            if (agent != null && !agent.isBusy() && agent.isAlive()) {
                // 检查该 Agent 是否已经有任务在追踪
                TaskTracker existingTracker = assignedTasks.get(agentId);
                if (existingTracker == null) {
                    // 空闲 Agent，可以并行分配任务
                    log.info("并行推进：为空闲 Agent {} 分配里程碑 [{}]", agent.getName(), pending.getTitle());
                    autoSelectAndStartWorkflow(pending);
                    assignedTasks.put(agentId, new TaskTracker(pending.getTitle(), pending.getId(), pending.getProgress()));
                }
            }
        }
    }

    /**
     * 检查目标是否已完成所有里程碑
     * 如果所有里程碑都已完成，将目标状态更新为 REVIEW
     * 如果所有里程碑都已完成或阻塞（无 PENDING/IN_PROGRESS），通知管理员
     */
    private void checkGoalCompletion(String projectId) {
        if (goalService == null) return;

        List<GameProject.GoalMilestone> milestones = goalService.getMilestones(projectId);
        if (milestones == null || milestones.isEmpty()) return;

        boolean allCompleted = milestones.stream()
            .allMatch(m -> m.getStatus() == GameProject.MilestoneStatus.COMPLETED);

        if (allCompleted) {
            log.info("所有里程碑已完成，将目标状态更新为 REVIEW");
            currentProject.setGoalStatus(GameProject.GoalStatus.REVIEW);
            projectManager.saveProjectConfig(currentProject);
            logInfo("GOAL_REVIEW", "所有里程碑已完成，项目目标进入审查阶段");
        } else {
            // 检查是否有被阻塞的里程碑
            long blockedCount = milestones.stream()
                .filter(m -> m.getStatus() == GameProject.MilestoneStatus.BLOCKED)
                .count();
            long pendingCount = milestones.stream()
                .filter(m -> m.getStatus() == GameProject.MilestoneStatus.PENDING)
                .count();
            long inProgressCount = milestones.stream()
                .filter(m -> m.getStatus() == GameProject.MilestoneStatus.IN_PROGRESS)
                .count();

            // 所有未完成的里程碑都被阻塞（没有可推进的 PENDING/IN_PROGRESS）
            if (blockedCount > 0 && pendingCount == 0 && inProgressCount == 0) {
                logWarn("PROJECT_STUCK", String.format(
                    "项目已阻塞：%d 个里程碑被阻塞，无可用 Agent 推进，需要人工介入", blockedCount), null);
                sendNotificationToAdmin("PROJECT_STUCK",
                    String.format("【项目阻塞告警】\n\n项目: %s\n阻塞里程碑数: %d\n\n有里程碑因为 Agent 招聘被拒绝而无法推进，请手动处理。",
                        currentProject.getName(), blockedCount));
            } else {
                // 检查是否有空闲的 Agent，主动分配优化任务
                checkIdleAgentsAndOptimize(projectId);
            }
        }
    }

    /**
     * 检查里程碑是否在失败冷却期内
     * 冷却期内的里程碑不再尝试启动工作流，避免无限循环
     */
    private boolean isMilestoneInFailedCooldown(String milestoneId) {
        Long failedAt = failedWorkflowMilestones.get(milestoneId);
        if (failedAt == null) return false;
        if (System.currentTimeMillis() - failedAt > FAILED_WORKFLOW_COOLDOWN_MS) {
            failedWorkflowMilestones.remove(milestoneId);
            return false;
        }
        return true;
    }

    /**
     * 检查指定角色是否有可用 Agent
     *
     * @param projectId 项目 ID
     * @param role      角色 ID
     * @return true 如果该角色有存活的 Agent
     */
    private boolean hasAvailableAgent(String projectId, String role) {
        Agent agent = agentManager.getAgent(projectId + ":" + role);
        return agent != null && agent.isAlive();
    }

    /**
     * 检查工作流模板所需角色是否都有可用 Agent
     *
     * @param templateId 工作流模板 ID
     * @param projectId  项目 ID
     * @return true 如果工作流可以执行
     */
    private boolean isWorkflowViable(String templateId, String projectId) {
        if (workflowEngine == null) return true;
        WorkflowEngine.WorkflowTemplate template = workflowEngine.getTemplate(templateId);
        if (template == null) return true;

        for (WorkflowEngine.WorkflowStep step : template.getSteps()) {
            if (!hasAvailableAgent(projectId, step.getAgentRole())) {
                log.debug("工作流 {} 不可行：缺少角色 {} 的 Agent", templateId, step.getAgentRole());
                return false;
            }
        }
        return true;
    }

    /**
     * 检查空闲 Agent 并主动分配优化任务
     * 当有 Agent 空闲但有未完成里程碑时，尝试推进
     */
    private void checkIdleAgentsAndOptimize(String projectId) {
        List<Agent> agents = agentManager.getAgentsByProject(projectId);
        long idleCount = agents.stream()
            .filter(a -> !"producer".equals(a.getRole()))
            .filter(a -> a.isAlive() && !a.isBusy())
            .count();

        if (idleCount > 0) {
            log.info("有 {} 个 Agent 空闲，检查是否有可推进的任务", idleCount);
            // 尝试查找被阻塞但可以推进的里程碑
            List<GameProject.GoalMilestone> milestones = goalService.getMilestones(projectId);
            if (milestones != null) {
                for (GameProject.GoalMilestone m : milestones) {
                    if (m.getStatus() == GameProject.MilestoneStatus.PENDING
                        || m.getStatus() == GameProject.MilestoneStatus.IN_PROGRESS) {
                        // 跳过冷却中的里程碑，避免无限循环
                        if (isMilestoneInFailedCooldown(m.getId())) {
                            continue;
                        }
                        String targetAgentId = projectId + ":" + m.getAssignedAgentRole();
                        Agent targetAgent = agentManager.getAgent(targetAgentId);
                        if (targetAgent != null && !targetAgent.isBusy() && targetAgent.isAlive()) {
                            log.info("发现空闲 Agent [{}]({}) 有待处理里程碑 [{}]，尝试启动工作流",
                                targetAgent.getName(), targetAgent.getRole(), m.getTitle());
                            autoSelectAndStartWorkflow(m);
                            assignedTasks.put(targetAgentId, new TaskTracker(m.getTitle(), m.getId(), m.getProgress()));
                            break; // 一次只启动一个
                        }
                    }
                }
            }
        }
    }

    /**
     * 执行待处理的干预指令
     * 从工作记忆中查找待执行的干预指令，并真正执行相应的业务逻辑
     *
     * 【重要】这是干预系统的真正执行入口，而不是 BaseAgent.handleInterventionMessage 中的 AI 回复
     *
     * @return true 如果执行了干预指令（应该跳过本轮常规工作），false 如果没有待执行的干预
     */
    private boolean executePendingInterventions() {
        if (agentContext == null || interventionService == null) return false;

        String projectId = getProjectId();
        if (projectId == null) return false;

        // 从工作记忆中查找待执行的干预指令
        Map<String, String> workingMemory = agentContext.getAllWorkingMemory();
        List<String> pendingInterventionKeys = workingMemory.keySet().stream()
            .filter(key -> key.startsWith("pending_intervention_"))
            .collect(Collectors.toList());

        if (pendingInterventionKeys.isEmpty()) return false;

        log.info("发现 {} 个待执行的干预指令", pendingInterventionKeys.size());

        for (String key : pendingInterventionKeys) {
            String interventionNo = key.replace("pending_intervention_", "");
            String instruction = workingMemory.get(key);

            logInfo("INTERVENTION_EXECUTING", String.format("开始执行干预指令 [%s]: %s",
                interventionNo, instruction.length() > 100 ? instruction.substring(0, 100) + "..." : instruction));

            try {
                // 解析干预指令，执行相应的业务逻辑
                boolean executed = executeInterventionByType(interventionNo, instruction);

                if (executed) {
                    // 执行成功，清除工作记忆中的待执行标记
                    agentContext.removeWorkingMemory(key);

                    // 更新经验记忆状态
                    saveExperience("intervention_" + interventionNo,
                        String.format("[干预指令 %s] 指令: %s | 状态: 已执行 | 执行时间: %s",
                            interventionNo,
                            instruction != null ? instruction : "无",
                            java.time.LocalDateTime.now()));

                    // 标记干预记录为已执行
                    interventionService.executeInterventionByNo(interventionNo,
                        String.format("制作人已执行干预指令，执行时间: %s", java.time.LocalDateTime.now()));

                    logInfo("INTERVENTION_EXECUTED", String.format("干预指令 [%s] 已成功执行", interventionNo));
                } else {
                    log.warn("干预指令 [{}] 执行失败，将在下一个工作周期重试", interventionNo);
                }

            } catch (Exception e) {
                log.error("执行干预指令 [{}] 失败: {}", interventionNo, e.getMessage());
                logError("INTERVENTION_EXECUTION_FAILED",
                    String.format("执行干预指令 [%s] 失败: %s", interventionNo, e.getMessage()),
                    e.getStackTrace() != null && e.getStackTrace().length > 0
                        ? java.util.Arrays.stream(e.getStackTrace()).limit(5)
                            .map(StackTraceElement::toString).collect(Collectors.joining("\n"))
                        : "无堆栈信息");
            }
        }

        return true;
    }

    /**
     * 执行干预指令
     * 制作人是AI，有自己的理解能力，所有干预命令都交给AI处理
     * AI会根据干预内容自主决定调用什么能力（跳过、推进、重新规划等）
     *
     * @param interventionNo 干预编号
     * @param instruction 干预指令内容
     * @return true 如果执行成功，false 如果执行失败
     */
    private boolean executeInterventionByType(String interventionNo, String instruction) {
        if (instruction == null || instruction.isEmpty()) return false;

        // 所有干预命令都交给AI在后续工作周期中处理
        // AI会根据干预内容自主理解并调用合适的能力
        log.info("干预指令 [{}] 已接收，将由AI在后续工作周期中处理: {}", interventionNo,
            instruction.length() > 200 ? instruction.substring(0, 200) + "..." : instruction);

        // 记录干预日志
        logInfo("INTERVENTION_RECEIVED", String.format("收到干预指令: %s",
            instruction.length() > 200 ? instruction.substring(0, 200) + "..." : instruction));

        // 保存到工作记忆中，使用 pending_intervention_ 前缀，让 executePendingInterventions 可以找到并执行
        agentContext.addWorkingMemory("pending_intervention_" + interventionNo, instruction);

        // 返回 false，让 executePendingInterventions 在下一个工作周期处理
        // 这样干预命令会被正确地注入到工作上下文中，由AI处理
        return false;
    }

    /**
     * 检查版本迭代干预
     * 当管理员对已完成项目发起版本迭代时，制作人需要分析需求并重新启动迭代
     */
    private void checkVersionIterationInterventions() {
        String projectId = getProjectId();
        if (projectId == null || interventionService == null) return;

        // 查询发给当前制作人的版本迭代干预
        String producerAgentId = projectId + ":producer";
        List<AgentIntervention> pendingInterventions = interventionService.getPendingInterventions(producerAgentId);

        for (AgentIntervention intervention : pendingInterventions) {
            if (intervention.getInterventionType() == AgentIntervention.InterventionType.VERSION_ITERATION) {
                logInfo("VERSION_ITERATION_RECEIVED", String.format("收到版本迭代请求: %s", intervention.getInterventionNo()));

                try {
                    // 1. 确认干预
                    interventionService.acknowledgeIntervention(intervention.getId(), "制作人已收到版本迭代请求，开始分析");

                    // 2. 解析迭代需求
                    String requirements = intervention.getInstruction();
                    Map<String, String> additionalData = intervention.getAdditionalData();
                    String iterationProjectId = additionalData != null ? additionalData.get("projectId") : projectId;
                    String version = additionalData != null ? additionalData.get("version") : null;

                    // 3. 分析需求并制定迭代计划
                    logInfo("VERSION_ITERATION_ANALYSIS", String.format("开始分析版本迭代需求: %s", requirements));

                    // 重置项目目标状态，开始新的迭代
                    if (currentProject != null) {
                        // 保存当前版本信息
                        String currentVersion = currentProject.getVersion();
                        logInfo("VERSION_BACKUP", String.format("备份当前版本: %s", currentVersion));

                        // 更新项目目标
                        String newGoal = String.format("[版本迭代 %s] %s\n\n原始需求:\n%s",
                                version != null ? version : "v" + (currentProject.getVersionCount() + 1),
                                currentProject.getGoal(),
                                requirements);
                        currentProject.setGoal(newGoal);
                        currentProject.setGoalStatus(GameProject.GoalStatus.NOT_STARTED);
                        currentProject.setGoalProgress(0);

                        // 清除旧的里程碑（保留历史）
                        List<GameProject.GoalMilestone> oldMilestones = new ArrayList<>(currentProject.getMilestones());
                        currentProject.getMilestones().clear();

                        // 更新版本信息
                        currentProject.setVersion(version != null ? version : "v" + (currentProject.getVersionCount() + 1));
                        currentProject.incrementVersionCount();

                        projectManager.saveProjectConfig(currentProject);

                        // 4. 执行干预
                        interventionService.executeIntervention(intervention.getId(),
                                String.format("版本迭代已启动，新版本: %s，旧里程碑数: %d", currentProject.getVersion(), oldMilestones.size()));

                        // 5. 重新分解目标
                        logInfo("VERSION_ITERATION_START", String.format("开始分解新版本目标: %s", currentProject.getVersion()));
                        decomposeGoal();

                        // 6. 通知管理员
                        sendNotificationToAdmin("VERSION_ITERATION_STARTED",
                                String.format("项目 [%s] 版本迭代已启动\n新版本: %s\n需求: %s",
                                        currentProject.getName(), currentProject.getVersion(),
                                        requirements.length() > 100 ? requirements.substring(0, 100) + "..." : requirements));
                    }

                } catch (Exception e) {
                    log.error("处理版本迭代干预失败: {}", e.getMessage());
                    try {
                        interventionService.rejectIntervention(intervention.getId(), "处理失败: " + e.getMessage());
                    } catch (Exception ex) {
                        log.error("拒绝干预失败: {}", ex.getMessage());
                    }
                }
            }
        }
    }

    /**
     * 定期验证游戏项目
     * 每30个周期（约30分钟）自动验证项目质量
     * 验证失败时生成改进建议并通知负责 Agent
     */
    private void verifyGameProjectPeriodically() {
        String projectId = getProjectId();
        if (projectId == null || currentProject == null) return;

        String projectDir = currentProject.getWorkDir();
        if (projectDir == null || projectDir.isEmpty()) return;

        // 检查 GameRuntimeVerifier 是否可用
        if (gameRuntimeVerifier == null) {
            log.debug("GameRuntimeVerifier 不可用，跳过定期验证");
            return;
        }

        logInfo("PERIODIC_VERIFY_START", String.format("开始定期验证项目: %s", currentProject.getName()));

        try {
            // 1. 结构验证
            GameRuntimeVerifier.VerifyResult structResult = gameRuntimeVerifier.verify(projectDir);
            if (!structResult.isSuccess()) {
                logWarn("PERIODIC_VERIFY_FAILED", "结构验证失败: " + structResult.toSummary(), null);
                return;
            }

            // 2. 质量分析
            GameRuntimeVerifier.QualityAnalysisResult qualityResult =
                gameRuntimeVerifier.analyzeQuality(projectDir, currentProject.getName(), currentProject.getGoal());

            if (!qualityResult.isSuccess()) {
                log.warn("定期验证：质量分析失败: {}", qualityResult.getError());
                return;
            }

            // 3. 记录验证结果
            int overallScore = qualityResult.getOverallScore();
            logInfo("PERIODIC_VERIFY_RESULT", String.format("项目质量评分: %d/100", overallScore));

            // 4. 如果质量不足，生成改进建议并通知负责 Agent
            if (overallScore < 60) {
                logWarn("PERIODIC_VERIFY_LOW_QUALITY",
                    String.format("项目质量不足 (%d/100)，需要改进", overallScore), null);

                // 构建改进建议
                StringBuilder improvement = new StringBuilder();
                improvement.append(String.format("项目 [%s] 定期验证未通过（总分: %d/100）\n\n",
                    currentProject.getName(), overallScore));
                improvement.append("需要改进的方面:\n");

                if (qualityResult.getPlayableScore() < 60) {
                    improvement.append(String.format("- 可玩性 (%d/100): %s\n",
                        qualityResult.getPlayableScore(), "需要完善核心玩法循环"));
                }
                if (qualityResult.getCompletenessScore() < 60) {
                    improvement.append(String.format("- 玩法完整性 (%d/100): %s\n",
                        qualityResult.getCompletenessScore(), "需要补充缺失的游戏机制"));
                }
                if (qualityResult.getUiuxScore() < 60) {
                    improvement.append(String.format("- UI/UX 质量 (%d/100): %s\n",
                        qualityResult.getUiuxScore(), "需要改进界面设计和交互"));
                }
                if (qualityResult.getCodeQualityScore() < 60) {
                    improvement.append(String.format("- 代码质量 (%d/100): %s\n",
                        qualityResult.getCodeQualityScore(), "需要重构和优化代码"));
                }

                improvement.append("\n改进建议:\n");
                qualityResult.getSuggestions().forEach(s -> improvement.append("- ").append(s).append("\n"));

                // 查找负责的 Agent 并发送改进任务
                String targetAgentId = findResponsibleAgent(projectId);
                if (targetAgentId != null) {
                    AgentMessage improveMsg = AgentMessage.builder()
                        .fromAgentId(getId())
                        .toAgentId(targetAgentId)
                        .type(AgentMessage.MessageType.TASK)
                        .content(improvement.toString())
                        .build();
                    sendMessage(improveMsg);
                    logInfo("PERIODIC_VERIFY_IMPROVE_SENT",
                        String.format("已向 %s 发送改进任务", targetAgentId));
                } else {
                    log.warn("定期验证：未找到负责的 Agent，无法发送改进任务");
                }

                // 通知管理员
                sendNotificationToAdmin("PERIODIC_VERIFY_FAILED",
                    String.format("项目 [%s] 定期验证未通过\n总分: %d/100\n需要改进",
                        currentProject.getName(), overallScore));
            } else {
                logInfo("PERIODIC_VERIFY_PASSED",
                    String.format("项目质量良好 (%d/100)", overallScore));
            }

            // 将验证结果保存到知识库
            if (knowledgeEvolutionService != null) {
                try {
                    Map<String, Integer> dimensionScores = new HashMap<>();
                    dimensionScores.put("runnable", qualityResult.getRunnableScore());
                    dimensionScores.put("playable", qualityResult.getPlayableScore());
                    dimensionScores.put("completeness", qualityResult.getCompletenessScore());
                    dimensionScores.put("uiux", qualityResult.getUiuxScore());
                    dimensionScores.put("codeQuality", qualityResult.getCodeQualityScore());

                    knowledgeEvolutionService.learnFromGameVerification(
                        getId(), projectId, currentProject.getName(),
                        overallScore, dimensionScores,
                        qualityResult.getIssues(), qualityResult.getSuggestions(),
                        overallScore >= 60);

                    log.info("定期验证结果已保存到知识库: project={}, score={}",
                        currentProject.getName(), overallScore);
                } catch (Exception e) {
                    log.warn("保存定期验证结果到知识库失败: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("定期验证失败: {}", e.getMessage());
            logError("PERIODIC_VERIFY_ERROR", "定期验证异常: " + e.getMessage(), e.toString());
        }
    }

    /**
     * 查找负责项目开发的 Agent
     * 优先返回 server-dev，其次返回第一个非 producer 的 Agent
     */
    private String findResponsibleAgent(String projectId) {
        List<Agent> agents = agentManager.getAgentsByProject(projectId);
        if (agents.isEmpty()) return null;

        // 优先返回 server-dev
        for (Agent agent : agents) {
            if ("server-dev".equals(agent.getRole()) && agent.isAlive()) {
                return agent.getId();
            }
        }

        // 其次返回第一个非 producer 的 Agent
        for (Agent agent : agents) {
            if (!"producer".equals(agent.getRole()) && agent.isAlive()) {
                return agent.getId();
            }
        }

        return null;
    }

    /**
     * 鞭策机制：主动催促空闲和慢速的 Agent
     * 检查所有团队成员的工作状态，对空闲 Agent 发送任务催促
     */
    private void whipTeamMembers() {
        String projectId = getProjectId();
        if (projectId == null || goalService == null) return;

        List<Agent> agents = agentManager.getAgentsByProject(projectId);
        if (agents.isEmpty()) return;

        // 1. 检查空闲 Agent - 如果有待处理的里程碑，催促他们工作
        for (Agent agent : agents) {
            if ("producer".equals(agent.getRole())) continue;
            if (!agent.isAlive()) continue;

            String agentId = agent.getId();

            if (!agent.isBusy() && agent.getPendingMessages().isEmpty()) {
                // 空闲 Agent - 检查是否有他们应该做的工作
                GameProject.GoalMilestone nextForRole = findNextMilestoneForRole(projectId, agent.getRole());
                if (nextForRole != null) {
                    // 冷却中的里程碑不鞭策（工作流刚失败，重复催促无意义）
                    if (isMilestoneInFailedCooldown(nextForRole.getId())) {
                        log.debug("里程碑 [{}] 在冷却期内，跳过鞭策 Agent [{}]", nextForRole.getTitle(), agent.getName());
                        continue;
                    }
                    // 有工作可做但空闲着，发送催促
                    String nudge = String.format(
                        "你目前处于空闲状态，但有待处理的里程碑任务：[%s] %s。请主动检查工作队列并开始执行。",
                        nextForRole.getAssignedAgentRole(), nextForRole.getTitle());
                    AgentMessage nudgeMsg = AgentMessage.builder()
                        .fromAgentId(getId())
                        .toAgentId(agentId)
                        .type(AgentMessage.MessageType.NOTIFY)
                        .content(nudge)
                        .priority(1) // 高优先级
                        .build();
                    messageBus.send(nudgeMsg);
                    log.info("鞭策: 催促空闲 Agent [{}]({}) 开始任务 [{}] (里程碑角色: {})",
                        agent.getName(), agent.getRole(), nextForRole.getTitle(), nextForRole.getAssignedAgentRole());
                }
            }
        }

        // 2. 检查任务分配表 - 清理已完成的任务
        assignedTasks.entrySet().removeIf(entry -> {
            Agent agent = agentManager.getAgent(entry.getKey());
            return agent == null || !agent.isAlive() || (!agent.isBusy() && agent.getPendingMessages().isEmpty());
        });
    }

    /**
     * 查找指定角色的下一个待处理里程碑
     */
    private GameProject.GoalMilestone findNextMilestoneForRole(String projectId, String role) {
        List<GameProject.GoalMilestone> milestones = goalService.getMilestones(projectId);
        if (milestones == null) return null;

        return milestones.stream()
            .filter(m -> role.equals(m.getAssignedAgentRole()))
            .filter(m -> m.getStatus() == GameProject.MilestoneStatus.PENDING
                || m.getStatus() == GameProject.MilestoneStatus.IN_PROGRESS)
            .filter(m -> m.areDependenciesMet(milestones))
            .findFirst()
            .orElse(null);
    }

    /**
     * 判断是否应该启动工作流
     * 制作人自主决策：只要有可执行里程碑就尝试启动工作流
     */
    private boolean shouldStartWorkflow(GameProject.GoalMilestone milestone) {
        // 制作人自主决策：默认使用工作流来驱动里程碑执行
        return true;
    }

    /**
     * 直接将里程碑任务分配给指定 Agent（不走工作流）
     * 当工作流不可行或失败时，直接把任务发给目标 Agent
     *
     * @param projectId  项目 ID
     * @param milestone  里程碑
     * @param targetAgent 目标 Agent
     */
    private void directAssignMilestone(String projectId, GameProject.GoalMilestone milestone, Agent targetAgent) {
        // 如果里程碑没有任务，先生成
        if (milestone.getTasks().isEmpty()) {
            generateMilestoneTasks(projectId, milestone);
        }

        String targetAgentId = targetAgent.getId();
        for (GameProject.MilestoneTask task : milestone.getTasks()) {
            if (task.getStatus() == GameProject.MilestoneStatus.PENDING) {
                String taskContent = buildTaskContent(milestone, task);
                AgentMessage taskMsg = AgentMessage.builder()
                    .fromAgentId(getId())
                    .toAgentId(targetAgentId)
                    .type(AgentMessage.MessageType.TASK)
                    .content(taskContent)
                    .build();
                sendMessage(taskMsg);

                goalService.updateTaskStatus(projectId, milestone.getId(), task.getId(),
                    GameProject.MilestoneStatus.IN_PROGRESS, null);
            }
        }

        if (milestone.getStatus() == GameProject.MilestoneStatus.PENDING) {
            goalService.updateMilestoneProgress(projectId, milestone.getId(), 1);
        }

        assignedTasks.put(targetAgentId, new TaskTracker(milestone.getTitle(), milestone.getId(), milestone.getProgress()));
        log.info("已直接分配里程碑 [{}] 给 Agent [{}]", milestone.getTitle(), targetAgent.getName());
    }

    /**
     * 检查正在运行的工作流状态
     * 如果工作流已完成，推进到下一个里程碑
     */
    private void checkRunningWorkflowStatus() {
        if (pendingWorkflowInstanceId == null || workflowEngine == null) return;

        WorkflowEngine.WorkflowInstance instance = workflowEngine.getInstance(pendingWorkflowInstanceId);
        if (instance == null) {
            log.warn("工作流实例不存在: {}，清除引用", pendingWorkflowInstanceId);
            pendingWorkflowInstanceId = null;
            workflowStartTime = null;
            return;
        }

        switch (instance.getStatus()) {
            case COMPLETED -> {
                logInfo("WORKFLOW_COMPLETED", String.format("工作流已完成: %s，模板: %s",
                    pendingWorkflowInstanceId, instance.getTemplateId()));
                pendingWorkflowInstanceId = null;
                workflowStartTime = null;

                // 【重要】工作流完成不等于任务完成，需要验证
                String milestoneId = instance.getParameters().get("milestoneId");
                if (milestoneId != null && goalService != null) {
                    String projectId = getProjectId();
                    GameProject.GoalMilestone milestone = goalService.getMilestones(projectId).stream()
                        .filter(m -> milestoneId.equals(m.getId()))
                        .findFirst()
                        .orElse(null);

                    if (milestone != null) {
                        // 进行验证
                        logInfo("WORKFLOW_VERIFICATION", String.format("工作流完成，开始验证里程碑 [%s]...", milestone.getTitle()));

                        // 构建验证报告
                        String verificationReport = buildWorkflowCompletionReport(instance, milestone);
                        VerificationResult verificationResult = verifyMilestoneCompletion(projectId, milestone, "workflow-engine", verificationReport);

                        if (verificationResult.passed) {
                            // 验证通过，标记完成
                            goalService.updateMilestoneProgress(projectId, milestoneId, 100);
                            milestone.setVerificationResult("工作流验证通过: " + verificationResult.details);
                            milestone.setLastVerificationTime(java.time.LocalDateTime.now().toString());
                            projectManager.saveProjectConfig(currentProject);
                            logInfo("MILESTONE_ADVANCED", "里程碑进度已更新: " + milestoneId);
                        } else {
                            // 验证失败，要求返工
                            milestone.setVerificationFailCount(milestone.getVerificationFailCount() + 1);
                            milestone.setVerificationResult("工作流验证失败: " + verificationResult.details);
                            milestone.setLastVerificationTime(java.time.LocalDateTime.now().toString());
                            projectManager.saveProjectConfig(currentProject);

                            logWarn("WORKFLOW_VERIFICATION_FAILED",
                                String.format("工作流完成后验证失败: %s。失败原因: %s", milestone.getTitle(), verificationResult.details), null);

                            // 通知目标Agent返工
                            String targetAgentId = projectId + ":" + milestone.getAssignedAgentRole();
                            Agent targetAgent = agentManager.getAgent(targetAgentId);
                            if (targetAgent != null && targetAgent.isAlive()) {
                                sendReplyToAgent(targetAgentId,
                                    String.format("你负责的里程碑 [%s] 工作流已完成，但验证未通过。请根据以下反馈进行改进：\n\n%s\n\n需要满足的验证标准:\n%s",
                                        milestone.getTitle(), verificationResult.details,
                                        String.join("\n", milestone.getVerificationCriteria())));
                            }
                        }
                    }
                }
            }
            case FAILED -> {
                String failedMilestoneId = instance.getParameters() != null
                    ? instance.getParameters().get("milestoneId") : null;
                logWarn("WORKFLOW_FAILED", String.format("工作流失败: %s，模板: %s，里程碑: %s",
                    pendingWorkflowInstanceId, instance.getTemplateId(), failedMilestoneId), null);

                // 记录到失败冷却表，避免无限重试
                if (failedMilestoneId != null) {
                    failedWorkflowMilestones.put(failedMilestoneId, System.currentTimeMillis());
                    log.info("里程碑 [{}] 已进入工作流失败冷却期（{}分钟），不再重复尝试",
                        failedMilestoneId, FAILED_WORKFLOW_COOLDOWN_MS / 60000);
                }

                pendingWorkflowInstanceId = null;
                workflowStartTime = null;

                // 回退：如果目标角色 Agent 存在，直接分配任务；否则发起招聘
                if (failedMilestoneId != null && goalService != null) {
                    String projectId = getProjectId();
                    List<GameProject.GoalMilestone> milestones = goalService.getMilestones(projectId);
                    if (milestones != null) {
                        milestones.stream()
                            .filter(m -> failedMilestoneId.equals(m.getId()))
                            .findFirst()
                            .ifPresent(m -> {
                                Agent target = agentManager.getAgent(projectId + ":" + m.getAssignedAgentRole());
                                if (target != null && target.isAlive()) {
                                    log.info("工作流失败，直接将任务分配给目标 Agent: {}", target.getName());
                                    directAssignMilestone(projectId, m, target);
                                } else {
                                    log.info("工作流失败且目标角色无 Agent，发起招聘: {}", m.getAssignedAgentRole());
                                    requestRecruitApproval(m.getAssignedAgentRole(), m.getTitle());
                                }
                            });
                    }
                }
            }
            case CANCELLED -> {
                logInfo("WORKFLOW_CANCELLED", "工作流已取消: " + pendingWorkflowInstanceId);
                pendingWorkflowInstanceId = null;
                workflowStartTime = null;
            }
            case RUNNING, PAUSED -> {
                // 检测工作流是否卡住
                if (workflowStartTime != null) {
                    long elapsedMinutes = java.time.Duration.between(workflowStartTime, java.time.LocalDateTime.now()).toMinutes();
                    if (elapsedMinutes >= WORKFLOW_STUCK_TIMEOUT_MINUTES) {
                        logWarn("WORKFLOW_STUCK", String.format("工作流已运行 %d 分钟无进展，自动取消。实例: %s，模板: %s",
                            elapsedMinutes, pendingWorkflowInstanceId, instance.getTemplateId()), null);

                        // 记录到失败冷却表
                        String stuckMilestoneId = instance.getParameters() != null
                            ? instance.getParameters().get("milestoneId") : null;
                        if (stuckMilestoneId != null) {
                            failedWorkflowMilestones.put(stuckMilestoneId, System.currentTimeMillis());
                        }

                        // 取消卡住的工作流
                        try {
                            workflowEngine.cancelWorkflow(pendingWorkflowInstanceId);
                        } catch (Exception e) {
                            log.warn("取消工作流失败: {}", e.getMessage());
                        }
                        pendingWorkflowInstanceId = null;
                        workflowStartTime = null;
                        // 回退到直接任务分配
                        log.info("工作流卡住取消，回退到直接任务分配");
                        checkAndAssignMilestoneTasks();
                        return;
                    }
                }
                log.debug("工作流运行中: {}，状态: {}", pendingWorkflowInstanceId, instance.getStatus());
            }
        }
    }

    /**
     * 构建工作流完成时的验证报告
     *
     * @param instance 工作流实例
     * @param milestone 里程碑对象
     * @return 验证报告内容
     */
    private String buildWorkflowCompletionReport(WorkflowEngine.WorkflowInstance instance, GameProject.GoalMilestone milestone) {
        StringBuilder report = new StringBuilder();

        report.append("工作流已完成报告\n");
        report.append("================\n\n");

        report.append("里程碑: ").append(milestone.getTitle()).append("\n");
        report.append("描述: ").append(milestone.getDescription()).append("\n");
        report.append("负责角色: ").append(milestone.getAssignedAgentRole()).append("\n\n");

        // 添加工作流步骤信息
        report.append("执行的步骤:\n");
        if (instance.getStepExecutions() != null) {
            for (WorkflowEngine.StepExecution stepExec : instance.getStepExecutions().values()) {
                report.append("- ").append(stepExec.getStepId())
                      .append(" (").append(stepExec.getStatus()).append(")\n");
                if (stepExec.getResult() != null && !stepExec.getResult().isEmpty()) {
                    report.append("  结果: ").append(stepExec.getResult().substring(0, Math.min(100, stepExec.getResult().length()))).append("...\n");
                }
            }
        }

        report.append("\n任务描述:\n");
        milestone.getTasks().forEach(task ->
            report.append("- ").append(task.getDescription()).append("\n")
        );

        return report.toString();
    }

    /**
     * 自动选择并启动工作流
     * 智能选择优先级：
     * 1. 检查是否有相同里程碑正在运行的工作流 → 复用
     * 2. 检查是否有相同角色+相似任务的已完成工作流 → 参考
     * 3. 检查预制模板是否匹配 → 使用
     * 4. 创建自定义工作流 → 最后选择
     */
    private void autoSelectAndStartWorkflow(GameProject.GoalMilestone milestone) {
        String projectId = getProjectId();
        if (projectId == null || workflowEngine == null) return;

        // 优先级1：检查是否有相同里程碑正在运行的工作流
        String existingInstanceId = findRunningWorkflowForMilestone(projectId, milestone.getId());
        if (existingInstanceId != null) {
            log.info("里程碑 [{}] 已有运行中的工作流 {}，复用", milestone.getTitle(), existingInstanceId);
            pendingWorkflowInstanceId = existingInstanceId;
            return;
        }

        // 优先级2：选择工作流模板（内部会检查已完成的相似工作流）
        String templateId = selectWorkflowTemplate(milestone);

        // 优先级3：检查模板是否可行（所有步骤都有 Agent），不可用则降级到自定义工作流
        if (templateId != null && !isWorkflowViable(templateId, projectId)) {
            log.info("预制模板 {} 不可行（缺少角色 Agent），降级到自定义工作流", templateId);
            templateId = null;
        }

        // 优先级4：创建自定义工作流
        if (templateId == null) {
            // 先检查是否有可复用的自定义工作流模板
            templateId = findReusableCustomTemplate(projectId, milestone);
            if (templateId == null) {
                templateId = createCustomWorkflow(milestone);
            }
        }

        // 最终检查：自定义工作流是否可行
        if (templateId != null && !isWorkflowViable(templateId, projectId)) {
            log.warn("工作流不可行（缺少角色 Agent），跳过此里程碑，等待招聘或人工分配");
            return;
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
                workflowStartTime = java.time.LocalDateTime.now();

                // 追踪任务分配
                String targetAgentId = projectId + ":" + milestone.getAssignedAgentRole();
                assignedTasks.put(targetAgentId, new TaskTracker(milestone.getTitle(), milestone.getId(), milestone.getProgress()));

                logInfo("WORKFLOW_STARTED", "工作流已启动: " + templateId + "，实例ID: " + instance.getId());

                // 发布事件
                publishEvent(EventBus.WORKFLOW_STARTED, Map.of(
                    "templateId", templateId,
                    "instanceId", instance.getId(),
                    "milestoneId", milestone.getId()
                ));

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
     * 查找指定里程碑正在运行的工作流实例
     */
    private String findRunningWorkflowForMilestone(String projectId, String milestoneId) {
        if (workflowEngine == null) return null;

        List<WorkflowEngine.WorkflowInstance> instances = workflowEngine.getInstancesByProject(projectId);
        for (WorkflowEngine.WorkflowInstance instance : instances) {
            if (instance.getStatus() == WorkflowEngine.WorkflowStatus.RUNNING
                || instance.getStatus() == WorkflowEngine.WorkflowStatus.PAUSED) {
                // 检查工作流参数中是否包含此里程碑
                Map<String, String> params = instance.getParameters();
                if (params != null && milestoneId.equals(params.get("milestoneId"))) {
                    return instance.getId();
                }
            }
        }
        return null;
    }

    /**
     * 查找可复用的自定义工作流模板
     * 基于角色和里程碑特征匹配
     */
    private String findReusableCustomTemplate(String projectId, GameProject.GoalMilestone milestone) {
        if (workflowEngine == null) return null;

        String role = milestone.getAssignedAgentRole();
        String title = milestone.getTitle().toLowerCase();

        // 查找相同角色的已完成工作流
        List<WorkflowEngine.WorkflowTemplate> templates = workflowEngine.getAllTemplates();
        for (WorkflowEngine.WorkflowTemplate template : templates) {
            if (template.getId().startsWith("custom-") && template.getId().contains(role)) {
                // 检查是否有使用此模板的已完成实例
                List<WorkflowEngine.WorkflowInstance> instances = workflowEngine.getInstancesByProject(projectId);
                for (WorkflowEngine.WorkflowInstance instance : instances) {
                    if (template.getId().equals(instance.getTemplateId())
                        && instance.getStatus() == WorkflowEngine.WorkflowStatus.COMPLETED) {
                        log.info("找到可复用的工作流模板: {}", template.getId());
                        return template.getId();
                    }
                }
            }
        }
        return null;
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

        // 检查是否已有完成的策划工作流
        boolean hasCompletedPlanningWorkflow = hasCompletedWorkflowOfType("planning-full");

        // 根据角色和标题特征匹配模板
        if (title.contains("设计") || title.contains("策划")) {
            // 如果策划工作流已完成，使用标准开发流程
            if (hasCompletedPlanningWorkflow) {
                log.info("策划工作流已完成，使用标准开发流程处理里程碑: {}", milestone.getTitle());
                return "standard-game-dev";
            }
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
     * 检查是否有已完成的指定类型工作流
     */
    private boolean hasCompletedWorkflowOfType(String templateId) {
        if (workflowEngine == null || currentProject == null) return false;

        // 检查所有工作流实例，看是否有指定模板的已完成实例
        List<WorkflowEngine.WorkflowInstance> instances = workflowEngine.getInstancesByProject(currentProject.getId());
        return instances.stream()
            .anyMatch(instance -> templateId.equals(instance.getTemplateId())
                && instance.getStatus() == WorkflowEngine.WorkflowStatus.COMPLETED);
    }

    /**
     * 创建自定义工作流
     * 只包含有对应角色 Agent 的步骤，没有的步骤直接跳过
     */
    private String createCustomWorkflow(GameProject.GoalMilestone milestone) {
        if (workflowEngine == null) return null;

        String templateId = "custom-" + milestone.getId();
        String projectId = getProjectId();
        String role = milestone.getAssignedAgentRole();

        List<WorkflowEngine.WorkflowStep> steps = new ArrayList<>();
        String lastStepId = null;

        // 步骤 1：需求分析 — 需要 system-planner
        if (hasAvailableAgent(projectId, "system-planner")) {
            WorkflowEngine.WorkflowStep analyzeStep = new WorkflowEngine.WorkflowStep(
                "analyze", "需求分析", "system-planner",
                "分析任务需求: " + milestone.getTitle()
            );
            steps.add(analyzeStep);
            lastStepId = "analyze";
        }

        // 步骤 2：任务执行 — 目标角色
        WorkflowEngine.WorkflowStep devStep = new WorkflowEngine.WorkflowStep(
            "develop", "任务执行", role,
            milestone.getDescription() != null ? milestone.getDescription() : milestone.getTitle()
        );
        if (lastStepId != null) {
            devStep.addDependency(lastStepId);
        }
        steps.add(devStep);
        lastStepId = "develop";

        // 步骤 3：验证测试 — 需要 tester
        if (hasAvailableAgent(projectId, "tester")) {
            WorkflowEngine.WorkflowStep testStep = new WorkflowEngine.WorkflowStep(
                "test", "验证测试", "tester",
                "验证任务完成质量: " + milestone.getTitle()
            );
            testStep.addDependency(lastStepId);
            steps.add(testStep);
        }

        WorkflowEngine.WorkflowTemplate template = workflowEngine.createTemplate(
            templateId,
            "自定义-" + milestone.getTitle(),
            "为里程碑自动创建的工作流: " + milestone.getTitle(),
            steps
        );

        logInfo("WORKFLOW_CREATED", String.format("已创建自定义工作流: %s（%d 步骤）", templateId, steps.size()));
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
                log.info("Goal is completed, stopping producer agent");
                logInfo("GOAL_COMPLETED", "项目目标已完成，Agent 即将停止");
                stop();
            }
        }
    }

    /**
     * 从能力系统调用的目标分解方法
     * 供 CapabilityExecutionEngine 调用
     */
    public void decomposeGoalFromCapability() {
        decomposeGoal();
    }

    /**
     * 检查是否可以交付项目
     * 交付条件：
     * 1. 所有里程碑已完成
     * 2. 没有阻塞问题
     * 3. 项目目标已满足
     *
     * @return 是否可以交付
     */
    public boolean canDeliverProject() {
        String projectId = getProjectId();
        if (projectId == null || goalService == null) return false;

        List<GameProject.GoalMilestone> milestones = goalService.getMilestones(projectId);
        if (milestones.isEmpty()) return false;

        // 检查是否所有里程碑都已完成
        boolean allCompleted = milestones.stream()
            .allMatch(m -> m.getStatus() == GameProject.MilestoneStatus.COMPLETED);
        if (!allCompleted) return false;

        // 检查项目目标是否满足
        return currentProject.isGoalCompleted();
    }

    /**
     * 请求交付审批（重大决策）
     * 交付是重大决策，需要管理员审批
     *
     * @return 是否成功发起审批
     */
    public boolean requestDeliveryApproval() {
        String projectId = getProjectId();
        if (projectId == null) return false;

        // 检查是否可以交付
        if (!canDeliverProject()) {
            log.warn("项目不满足交付条件，无法发起交付审批");
            return false;
        }

        // 构建交付报告
        StringBuilder report = new StringBuilder();
        report.append("## 项目交付申请\n\n");

        // 项目信息
        report.append("### 项目信息\n");
        report.append("- 项目名称: ").append(currentProject.getName()).append("\n");
        report.append("- 项目目标: ").append(currentProject.getGoal()).append("\n");
        report.append("- 项目进度: ").append(currentProject.getGoalProgress()).append("%\n\n");

        // 里程碑完成情况
        List<GameProject.GoalMilestone> milestones = goalService.getMilestones(projectId);
        report.append("### 里程碑完成情况\n");
        for (GameProject.GoalMilestone m : milestones) {
            report.append(String.format("- [%s] %s\n",
                m.getStatus() == GameProject.MilestoneStatus.COMPLETED ? "✅" : "⏳",
                m.getTitle()));
        }
        report.append("\n");

        // 创建审批请求
        if (approvalService != null) {
            try {
                Map<String, Object> deliveryData = new HashMap<>();
                deliveryData.put("projectId", projectId);
                deliveryData.put("projectName", currentProject.getName());
                deliveryData.put("milestoneCount", milestones.size());
                deliveryData.put("completedCount", milestones.stream()
                    .filter(m -> m.getStatus() == GameProject.MilestoneStatus.COMPLETED).count());

                var approvalRequest = approvalService.createRequest(
                    projectId,
                    getId(),
                    "DELIVERY",
                    deliveryData.toString(),
                    report.toString()
                );

                logInfo("DELIVERY_APPROVAL_REQUESTED", String.format("项目交付审批已发起: %s", currentProject.getName()));
                sendNotificationToAdmin("DELIVERY_APPROVAL",
                    String.format("【项目交付申请】\n\n项目: %s\n里程碑: %d/%d 已完成\n\n请前往审批页面处理。",
                        currentProject.getName(),
                        milestones.stream().filter(m -> m.getStatus() == GameProject.MilestoneStatus.COMPLETED).count(),
                        milestones.size()));

                return true;
            } catch (Exception e) {
                log.error("创建交付审批失败: {}", e.getMessage());
                return false;
            }
        }

        return false;
    }

    /**
     * 评估是否需要下一个版本
     * 如果项目目标已满足，申请交付；否则规划下一个版本
     */
    public void evaluateAndPlanNextVersion() {
        String projectId = getProjectId();
        if (projectId == null || goalService == null) return;

        List<GameProject.GoalMilestone> milestones = goalService.getMilestones(projectId);

        // 检查是否所有里程碑都已完成
        boolean allCompleted = milestones.stream()
            .allMatch(m -> m.getStatus() == GameProject.MilestoneStatus.COMPLETED);

        if (allCompleted) {
            // 所有里程碑完成，检查是否可以交付
            logInfo("ALL_MILESTONES_COMPLETED", "所有里程碑已完成，评估是否可以交付");

            if (canDeliverProject()) {
                // 申请交付审批
                requestDeliveryApproval();
            } else {
                // 项目目标未完全满足，规划下一个版本
                logInfo("PLANNING_NEXT_VERSION", "项目目标未完全满足，规划下一个版本");
                decomposeGoal();
            }
        } else {
            // 还有未完成的里程碑，继续推进
            log.info("还有未完成的里程碑，继续推进");
        }
    }

    /**
     * 评估当前版本
     * 分析 Agent 绩效、人手缺失与冗余，生成版本评估报告
     *
     * @param milestoneId 里程碑 ID
     * @return 评估报告文本
     */
    public String evaluateCurrentVersion(String milestoneId) {
        String projectId = getProjectId();
        if (projectId == null || versionEvaluationService == null) {
            return "版本评估服务不可用";
        }

        VersionEvaluationService.VersionEvaluation evaluation =
            versionEvaluationService.evaluateVersion(projectId, milestoneId);

        if (evaluation == null) {
            return "版本评估失败";
        }

        // 发送评估通知
        sendVersionEvaluationNotification(evaluation);

        return versionEvaluationService.generateReport(evaluation);
    }

    /**
     * 发送版本评估通知
     */
    private void sendVersionEvaluationNotification(VersionEvaluationService.VersionEvaluation evaluation) {
        // 检查是否有人手问题需要通知
        if (!evaluation.getMissingRoles().isEmpty()) {
            sendNotificationToAdmin("VERSION_STAFFING_ISSUE",
                String.format("【版本评估 - 人手缺失】\n\n项目: %s\n版本: %s\n缺失角色: %s\n\n建议招聘以下角色: %s",
                    currentProject != null ? currentProject.getName() : getProjectId(),
                    evaluation.getMilestoneTitle(),
                    String.join(", ", evaluation.getMissingRoles()),
                    String.join(", ", evaluation.getMissingRoles())));
        }

        // 检查是否有低绩效 Agent
        long lowPerformers = evaluation.getAgentEvaluations().values().stream()
            .filter(a -> a.getOverallScore() < 50)
            .count();
        if (lowPerformers > 0) {
            sendNotificationToAdmin("VERSION_LOW_PERFORMANCE",
                String.format("【版本评估 - 低绩效警告】\n\n项目: %s\n版本: %s\n低绩效Agent数: %d\n综合评分: %d/100",
                    currentProject != null ? currentProject.getName() : getProjectId(),
                    evaluation.getMilestoneTitle(),
                    lowPerformers,
                    evaluation.getOverallScore()));
        }
    }

    /**
     * 获取版本评估结果（供 API 使用）
     */
    public VersionEvaluationService.VersionEvaluation getVersionEvaluation(String milestoneId) {
        String projectId = getProjectId();
        if (projectId == null || versionEvaluationService == null) return null;
        return versionEvaluationService.evaluateVersion(projectId, milestoneId);
    }

    /**
     * 里程碑完成后自动触发游戏验证
     * 验证游戏是否"好玩"，验证结果会自动反馈给 Agent 生成修复任务
     *
     * @param projectId 项目 ID
     */
    private void triggerGameVerification(String projectId) {
        if (gameRuntimeVerifier == null || currentProject == null) return;

        String workDir = currentProject.getWorkDir();
        if (workDir == null || workDir.isEmpty()) return;

        logInfo("AUTO_VERIFY", "里程碑完成，自动触发游戏验证: " + projectId);

        try {
            // 执行结构验证
            GameRuntimeVerifier.VerifyResult verifyResult = gameRuntimeVerifier.verify(workDir);
            if (!verifyResult.isSuccess()) {
                logInfo("VERIFY_FAILED", "游戏结构验证失败: " + verifyResult.getError());
                return;
            }

            // 触发 AI 深度分析（如果 AI 服务可用）
            // 通过 EventBus 发布验证请求，由 GameAnalysisTaskService 异步处理
            if (eventBus != null) {
                Map<String, Object> data = new HashMap<>();
                data.put("action", "auto_verify");
                data.put("projectId", projectId);
                data.put("workDir", workDir);
                eventBus.publish(projectId, EventBus.VERIFY_RESULT, getId(), data);
            }

            logInfo("AUTO_VERIFY_TRIGGERED", "游戏验证已触发，结果将在分析完成后自动反馈给 Agent");
        } catch (Exception e) {
            log.warn("自动触发游戏验证失败: {}", e.getMessage());
        }
    }

    /**
     * 触发版本评估
     * 里程碑完成后自动评估版本，检查人手问题和绩效
     */
    private void triggerVersionEvaluation(GameProject.GoalMilestone milestone) {
        if (versionEvaluationService == null) return;

        String projectId = getProjectId();
        if (projectId == null) return;

        log.info("触发版本评估: 里程碑={}", milestone.getTitle());

        try {
            VersionEvaluationService.VersionEvaluation evaluation =
                versionEvaluationService.evaluateVersion(projectId, milestone.getId());

            if (evaluation == null) return;

            // 记录评估结果
            String report = versionEvaluationService.generateReport(evaluation);
            logInfo("VERSION_EVALUATED", String.format("版本 [%s] 评估完成，综合评分: %d/100",
                milestone.getTitle(), evaluation.getOverallScore()));

            // 保存评估经验
            saveExperience("version_evaluation_" + milestone.getId(), report);

            // 检查是否需要通知管理员
            sendVersionEvaluationNotification(evaluation);

            // 评估是否需要下一个版本
            evaluateAndPlanNextVersion();

        } catch (Exception e) {
            log.error("版本评估失败: {}", e.getMessage());
        }
    }

    /**
     * 构建项目上下文信息（用于 decomposeGoal prompt）
     * 包含团队配置、已有代码结构、技术栈等信息
     *
     * @return 格式化的上下文文本
     */
    private String buildProjectContextForDecompose() {
        StringBuilder ctx = new StringBuilder();

        // 团队配置（已有的 Agent 角色）
        if (currentProject != null) {
            List<String> agentRoles = currentProject.getAgentIds();
            if (agentRoles != null && !agentRoles.isEmpty()) {
                ctx.append("- 团队配置: ").append(String.join(", ", agentRoles)).append("\n");
            }

            // 工作目录信息
            if (currentProject.getWorkDir() != null) {
                ctx.append("- 工作目录: ").append(currentProject.getWorkDir()).append("\n");
            }
        }

        // 已有里程碑（如果是重新分解）
        if (goalService != null && currentProject != null) {
            List<GameProject.GoalMilestone> existing = goalService.getMilestones(currentProject.getId());
            if (existing != null && !existing.isEmpty()) {
                ctx.append("- 已有里程碑数: ").append(existing.size()).append("\n");
                long completed = existing.stream()
                    .filter(m -> m.getStatus() == GameProject.MilestoneStatus.COMPLETED).count();
                ctx.append("- 已完成里程碑数: ").append(completed).append("\n");
            }
        }

        if (ctx.length() > 0) {
            return "\n## 项目上下文\n\n" + ctx + "\n";
        }
        return "";
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

        // 构建项目上下文信息
        String projectContext = buildProjectContextForDecompose();

        String goalPrompt = String.format(
            "你是一个游戏项目管理专家。请将以下项目目标分解为多个迭代周期（里程碑）。\n\n" +
            "## 项目信息\n\n" +
            "- 项目名称：%s\n" +
            "- 项目目标：%s\n" +
            "- 目标类型：%s\n" +
            "%s\n" +
            "## 核心理念\n\n" +
            "游戏开发是**迭代式**的，不是瀑布式的。每个迭代周期都应该：\n" +
            "1. 产出一个**可玩的版本**（哪怕功能有限）\n" +
            "2. **所有角色协作**完成（策划、开发、美术、测试）\n" +
            "3. 基于上一个迭代的反馈进行改进\n\n" +
            "## 输出格式\n\n" +
            "每个里程碑代表一个迭代周期，格式如下：\n" +
            "MILESTONE: 顺序号 | 标题 | 描述 | 依赖序号 | 验证标准\n\n" +
            "注意：\n" +
            "- 不需要指定单一角色，因为每个迭代都需要多角色协作\n" +
            "- 验证标准必须是**可自动化验证**的具体指标（如：接口返回200、页面可渲染、测试通过率>80%）\n" +
            "- 每个里程碑的验证标准至少包含3个可检查项\n\n" +
            "## 迭代分解原则\n\n" +
            "1. **第一个迭代**：最小可玩版本（核心玩法原型）\n" +
            "   - 策划：核心玩法设计文档\n" +
            "   - 开发：基础功能实现（项目骨架+核心逻辑）\n" +
            "   - 美术：临时资源（占位图/基础UI）\n" +
            "   - 测试：基本功能验证\n\n" +
            "2. **后续迭代**：逐步完善\n" +
            "   - 每个迭代增加新功能或改进现有功能\n" +
            "   - 每个迭代结束时都有一个可玩的版本\n" +
            "   - 优先实现核心功能，后做锦上添花的功能\n\n" +
            "3. **最后迭代**：打磨和优化\n" +
            "   - 性能优化（帧率、加载时间）\n" +
            "   - UI打磨（动画、音效、视觉一致性）\n" +
            "   - 全面测试（边界条件、兼容性）\n\n" +
            "## 依赖关系要求\n\n" +
            "- 识别任务间的**强依赖**（必须先完成A才能做B）和**弱依赖**（A做好了B更方便）\n" +
            "- 尽量让无依赖的任务可以**并行开发**\n" +
            "- 用序号表示依赖关系，0表示无依赖\n\n" +
            "## 示例（三消游戏）\n\n" +
            "MILESTONE: 1 | 核心玩法原型 | 实现基本的三消消除机制，可以在浏览器中玩 | 0 | 8x8棋盘可渲染;交换相邻方块可触发消除;相同方块3个以上连成一线会消除;消除后方块会下落;有基本的分数显示\n" +
            "MILESTONE: 2 | 关卡系统 | 添加关卡选择和进度管理 | 1 | 有关卡选择界面;至少5个关卡;每个关卡有目标分数;通关后可进入下一关;有返回按钮\n" +
            "MILESTONE: 3 | 道具系统 | 添加道具和特殊方块 | 2 | 有至少3种道具;道具可使用;特殊方块可生成;道具效果正确\n" +
            "MILESTONE: 4 | UI美化 | 美化界面和动画效果 | 3 | 界面美观;消除动画流畅;音效适配;整体视觉协调\n" +
            "MILESTONE: 5 | 打磨发布 | 性能优化和全面测试 | 4 | 帧率稳定60fps;无明显bug;加载时间<3秒;可发布\n\n" +
            "## 迭代思维\n\n" +
            "- 不要试图一次设计出完美方案，先做出最小可玩版本\n" +
            "- 每个迭代结束后，根据验证结果调整后续计划\n" +
            "- 如果发现之前的方案不好，大胆修改，不要被初始计划束缚\n" +
            "- 优先保证'能玩'，再追求'好玩'，最后打磨'精致'\n\n" +
            "请根据项目目标，输出适合的迭代里程碑。每个里程碑一行，不要有其他内容。",
            currentProject.getName(),
            currentProject.getGoal(),
            currentProject.getGoalType() != null ? currentProject.getGoalType().name() : "CUSTOM",
            projectContext
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
     *
     * 新格式（迭代式）：MILESTONE: 顺序号 | 标题 | 描述 | 依赖序号 | 验证标准
     * 旧格式（瀑布式）：MILESTONE: 顺序号 | 标题 | 描述 | 负责角色 | 依赖序号 | 验证标准
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

                // 智能判断是新格式还是旧格式
                // 新格式：顺序号 | 标题 | 描述 | 依赖序号 | 验证标准
                // 旧格式：顺序号 | 标题 | 描述 | 负责角色 | 依赖序号 | 验证标准
                String role = "multi-agent"; // 新格式默认为多角色协作
                int depIdx = titleIdx + 2;
                int criteriaIdx = titleIdx + 3;

                if (parts.length > titleIdx + 2) {
                    String thirdField = parts[titleIdx + 2].trim();
                    // 检查第三个字段是否是角色名
                    String possibleRole = normalizeRole(thirdField);
                    if (possibleRole != null) {
                        // 旧格式：有角色字段
                        role = possibleRole;
                        depIdx = titleIdx + 3;
                        criteriaIdx = titleIdx + 4;
                    }
                    // 否则是新格式：第三个字段是依赖序号
                }

                // 解析依赖序号（暂存，后面转换）
                List<Integer> depOrders = new java.util.ArrayList<>();
                if (parts.length > depIdx) {
                    String deps = parts[depIdx].trim();
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

                // 解析验证标准
                List<String> criteria = new java.util.ArrayList<>();
                if (parts.length > criteriaIdx) {
                    String criteriaStr = parts[criteriaIdx].trim();
                    if (!criteriaStr.isEmpty()) {
                        for (String c : criteriaStr.split("[;；]")) {
                            String criterion = c.trim();
                            if (!criterion.isEmpty()) {
                                criteria.add(criterion);
                            }
                        }
                    }
                }

                // 创建里程碑（不设置依赖）
                GameProject.GoalMilestone milestone = goalService.addMilestone(projectId, title, description, role, order, new java.util.ArrayList<>());
                milestone.setVerificationCriteria(criteria);
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

            // 校验所有里程碑角色是否有对应的 Agent，缺失的提前发起招聘
            validateMilestoneRolesAndRecruit(projectId, orderToMilestone.values());

            // 调用策划 Agent 进行详细任务分解
            decomposeMilestoneTasks(projectId, orderToMilestone.values());
        } else {
            currentProject.setGoalStatus(GameProject.GoalStatus.NOT_STARTED);
            log.warn("No milestones parsed from response, will retry. Response: {}",
                response.length() > 200 ? response.substring(0, 200) + "..." : response);
        }

        projectManager.saveProjectConfig(currentProject);
        saveContext();
    }

    /**
     * 调用策划 Agent 进行详细任务分解
     * 将每个里程碑分解为具体的可执行任务，包含输入/输出/验收标准
     */
    private void decomposeMilestoneTasks(String projectId, Collection<GameProject.GoalMilestone> milestones) {
        // 查找系统策划 Agent
        Agent plannerAgent = agentManager.getAgent(projectId, "system-planner");
        if (plannerAgent == null) {
            log.info("系统策划 Agent 不存在，跳过详细任务分解");
            return;
        }

        log.info("开始调用策划 Agent 进行详细任务分解");

        for (GameProject.GoalMilestone milestone : milestones) {
            try {
                // 构建任务分解提示词
                String prompt = buildTaskDecompositionPrompt(milestone);

                // 发送给策划 Agent
                String response = plannerAgent.sendMessage(prompt);
                if (response == null || response.isEmpty()) {
                    log.warn("策划 Agent 任务分解失败: milestone={}", milestone.getTitle());
                    continue;
                }

                // 解析任务列表
                List<com.chengxun.gamemaker.model.TaskTemplate> tasks = parseTaskTemplates(response, milestone.getId());

                if (!tasks.isEmpty()) {
                    // 将任务保存到里程碑
                    for (com.chengxun.gamemaker.model.TaskTemplate taskTemplate : tasks) {
                        GameProject.MilestoneTask task = new GameProject.MilestoneTask();
                        task.setTitle(taskTemplate.getTitle());
                        task.setDescription(taskTemplate.getDescription());
                        task.setAssignedRole(taskTemplate.getAssignedRole());
                        task.setPriority(taskTemplate.getPriority());
                        task.setEstimatedHours(taskTemplate.getEstimatedHours());
                        task.setInputRequirements(taskTemplate.getInputRequirements());
                        task.setOutputDeliverables(taskTemplate.getOutputDeliverables());
                        task.setAcceptanceCriteria(taskTemplate.getAcceptanceCriteria());
                        milestone.getTasks().add(task);
                    }

                    log.info("里程碑 [{}] 已分解为 {} 个任务", milestone.getTitle(), tasks.size());

                    // 发布事件
                    publishEvent("TASKS_DECOMPOSED", Map.of(
                        "milestoneId", milestone.getId(),
                        "milestoneTitle", milestone.getTitle(),
                        "taskCount", tasks.size()
                    ));
                }
            } catch (Exception e) {
                log.error("任务分解失败: milestone={}, error={}", milestone.getTitle(), e.getMessage());
            }
        }

        // 保存项目配置
        projectManager.saveProjectConfig(currentProject);
        log.info("所有里程碑任务分解完成");
    }

    /**
     * 构建任务分解提示词
     * 为迭代式里程碑生成多角色协作的任务分解
     */
    private String buildTaskDecompositionPrompt(GameProject.GoalMilestone milestone) {
        StringBuilder sb = new StringBuilder();
        sb.append("请将以下迭代里程碑分解为多个角色协作的具体任务。\n\n");

        sb.append("## 里程碑信息\n");
        sb.append("- 标题：").append(milestone.getTitle()).append("\n");
        if (milestone.getDescription() != null && !milestone.getDescription().isEmpty()) {
            sb.append("- 描述：").append(milestone.getDescription()).append("\n");
        }
        if (currentProject != null && currentProject.getGoal() != null) {
            sb.append("- 项目目标：").append(currentProject.getGoal()).append("\n");
        }
        if (milestone.getVerificationCriteria() != null && !milestone.getVerificationCriteria().isEmpty()) {
            sb.append("- 验证标准：\n");
            for (String criteria : milestone.getVerificationCriteria()) {
                sb.append("  - ").append(criteria).append("\n");
            }
        }
        sb.append("\n");

        sb.append("## 重要：这是一个迭代里程碑\n\n");
        sb.append("每个迭代里程碑都需要**多角色协作**完成，产出一个**可玩的版本**。\n");
        sb.append("请为以下每个角色分配具体任务：\n\n");

        sb.append("## 输出格式要求\n");
        sb.append("请按以下格式输出每个任务（每个任务用 TASK 分隔）：\n\n");
        sb.append("TASK: 任务标题 | 负责角色 | 任务描述 | 输入要求 | 输出产物 | 验收标准1;验收标准2 | 优先级 | 预估工时 | 依赖序号\n\n");

        sb.append("## 角色说明\n");
        sb.append("- system-planner: 系统策划，负责游戏系统设计和策划案\n");
        sb.append("- numerical-planner: 数值策划，负责数值平衡和经济系统\n");
        sb.append("- client-dev: 客户端开发，负责游戏前端逻辑、UI交互、游戏核心玩法实现\n");
        sb.append("- ui-dev: UI开发，负责界面设计实现、视觉效果、动画\n");
        sb.append("- server-dev: 服务端开发，负责后端逻辑、数据存储、多人联网\n");
        sb.append("- tester: 测试，负责质量验证\n");
        sb.append("- git-commit: 版本管理，负责代码提交和版本控制\n\n");

        sb.append("## 分解原则\n");
        sb.append("1. **每个迭代都需要多角色协作**，不要只分配给一个角色\n");
        sb.append("2. 策划角色：设计这个迭代需要的功能和玩法\n");
        sb.append("3. 开发角色：实现策划设计的功能\n");
        sb.append("4. 美术角色：实现界面和视觉效果\n");
        sb.append("5. 测试角色：验证这个迭代的可玩性\n");
        sb.append("6. 每个任务应该是一个独立可交付的工作单元\n");
        sb.append("7. 任务粒度适中，通常 2-8 小时可完成\n");
        sb.append("8. 合理设置依赖关系，支持并行执行\n\n");

        sb.append("请只输出 TASK 行，不要有其他内容。");

        return sb.toString();
    }

    /**
     * 解析任务模板列表
     */
    private List<com.chengxun.gamemaker.model.TaskTemplate> parseTaskTemplates(String response, String milestoneId) {
        List<com.chengxun.gamemaker.model.TaskTemplate> tasks = new ArrayList<>();
        String[] lines = response.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (!line.startsWith("TASK:") && !line.startsWith("TASK：")) continue;

            try {
                String content = line.substring(line.indexOf(":") + 1).trim();
                if (content.isEmpty()) {
                    content = line.substring(line.indexOf("：") + 1).trim();
                }

                String[] parts = content.split("\\|");
                if (parts.length < 6) continue;

                com.chengxun.gamemaker.model.TaskTemplate task = new com.chengxun.gamemaker.model.TaskTemplate();
                task.setTitle(parts[0].trim());
                task.setAssignedRole(normalizeRole(parts[1].trim()));
                task.setDescription(parts[2].trim());
                task.setInputRequirements(parts[3].trim());
                task.setOutputDeliverables(parts[4].trim());
                task.setMilestoneId(milestoneId);

                // 解析验收标准
                String criteriaStr = parts[5].trim();
                if (!criteriaStr.isEmpty()) {
                    String[] criteria = criteriaStr.split("[;；]");
                    List<String> criteriaList = new ArrayList<>();
                    for (String c : criteria) {
                        String trimmed = c.trim();
                        if (!trimmed.isEmpty()) {
                            criteriaList.add(trimmed);
                        }
                    }
                    task.setAcceptanceCriteria(criteriaList);
                }

                // 解析优先级
                if (parts.length > 6) {
                    task.setPriority(parts[6].trim().toUpperCase());
                }

                // 解析预估工时
                if (parts.length > 7) {
                    try {
                        task.setEstimatedHours(Integer.parseInt(parts[7].trim().replaceAll("[^0-9]", "")));
                    } catch (NumberFormatException e) {
                        task.setEstimatedHours(4); // 默认 4 小时
                    }
                }

                // 解析依赖
                if (parts.length > 8) {
                    String depsStr = parts[8].trim();
                    if (!depsStr.isEmpty() && !"0".equals(depsStr) && !"无".equals(depsStr)) {
                        List<String> deps = new ArrayList<>();
                        for (String dep : depsStr.split("[,，]")) {
                            String trimmed = dep.trim().replaceAll("[^0-9]", "");
                            if (!trimmed.isEmpty()) {
                                deps.add(trimmed);
                            }
                        }
                        task.setDependencies(deps);
                    }
                }

                if (task.getAssignedRole() != null) {
                    tasks.add(task);
                    log.info("Parsed task: {} -> {}", task.getTitle(), task.getAssignedRole());
                }
            } catch (Exception e) {
                log.warn("Failed to parse task line: {} - {}", line, e.getMessage());
            }
        }

        return tasks;
    }

    /**
     * 角色名称标准化（兼容中文和英文）
     * 支持模糊匹配：去除空格、大小写不敏感、匹配关键词子串
     *
     * @param role 原始角色名称（中文或英文）
     * @return 标准化后的角色ID，无法识别时返回 null
     */
    private String normalizeRole(String role) {
        if (role == null) return null;
        // 预处理：去空格、转小写
        String normalized = role.replaceAll("\\s+", "").toLowerCase();

        // 精确匹配
        String result = switch (normalized) {
            case "服务端", "后端", "server", "backend", "server-dev", "serverdev" -> "server-dev";
            case "客户端", "前端", "client", "frontend", "client-dev", "clientdev" -> "client-dev";
            case "ui", "美术", "界面", "ui-dev", "uidev" -> "ui-dev";
            case "策划", "系统策划", "planner", "system", "system-planner", "systemplanner" -> "system-planner";
            case "数值", "数值策划", "numerical", "numerical-planner", "numericalplanner" -> "numerical-planner";
            case "测试", "qa", "tester", "test" -> "tester";
            case "git", "版本", "version", "git-commit", "gitcommit" -> "git-commit";
            case "制作人", "producer", "pm" -> "producer";
            default -> null;
        };

        // 精确匹配失败，尝试子串模糊匹配
        if (result == null) {
            if (normalized.contains("数值") || normalized.contains("numerical")) {
                result = "numerical-planner";
            } else if (normalized.contains("系统") || normalized.contains("策划") || normalized.contains("planner")) {
                result = "system-planner";
            } else if (normalized.contains("服务") || normalized.contains("后端") || normalized.contains("server")) {
                result = "server-dev";
            } else if (normalized.contains("客户") || normalized.contains("前端") || normalized.contains("client")) {
                result = "client-dev";
            } else if (normalized.contains("美术") || normalized.contains("界面")) {
                result = "ui-dev";
            } else if (normalized.contains("测试") || normalized.contains("test")) {
                result = "tester";
            } else if (normalized.contains("git") || normalized.contains("版本")) {
                result = "git-commit";
            } else if (normalized.contains("制作人") || normalized.contains("producer")) {
                result = "producer";
            }
        }

        if (result == null) {
            log.warn("无法识别的角色名称: '{}'，已跳过。请检查 AI 输出格式。", role);
        }
        return result;
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

        // 检查是否需要生成项目运行规则
        checkAndGenerateProjectRules(projectId);

        GameProject.GoalMilestone next = goalService.getNextExecutableMilestone(projectId);
        if (next == null) return;

        // 检查该里程碑是否已被拒绝
        if (rejectedMilestones.contains(next.getId())) {
            log.debug("里程碑 [{}] 之前已被拒绝，跳过任务分配", next.getTitle());
            return;
        }

        // 查找对应的 Agent（精确角色匹配）
        String targetAgentId = projectId + ":" + next.getAssignedAgentRole();
        Agent targetAgent = agentManager.getAgent(targetAgentId);

        // Agent 不存在，发起招聘
        if (targetAgent == null) {
            log.info("里程碑 [{}] 需要角色 {}，无对应 Agent，发起招聘",
                next.getTitle(), next.getAssignedAgentRole());
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
     * 检查并生成项目运行规则
     * 当项目目标开始时，自动生成并通知项目运行和部署规则
     *
     * @param projectId 项目 ID
     */
    private void checkAndGenerateProjectRules(String projectId) {
        if (currentProject == null) return;

        // 检查是否已经生成过规则
        String existingRules = projectManager.loadProjectRules(projectId);
        if (existingRules != null && !existingRules.isEmpty()) {
            return;
        }

        // 生成项目运行规则
        String rules = generateProjectRules();

        // 保存规则到项目
        projectManager.saveProjectRules(projectId, rules);

        // 同时更新项目的 deploymentRules 字段
        currentProject.setDeploymentRules(rules);
        projectManager.saveProjectConfig(currentProject);

        // 通知团队成员
        notifyTeamProjectRules(projectId, rules);

        log.info("项目运行规则已生成并通知: {}", projectId);
    }

    /**
     * 生成项目运行规则
     *
     * @return 项目运行规则文本
     */
    private String generateProjectRules() {
        StringBuilder rules = new StringBuilder();

        rules.append("# 项目运行规则\n\n");
        rules.append("## 项目信息\n");
        rules.append("- 项目名称: ").append(currentProject.getName()).append("\n");
        rules.append("- 项目目标: ").append(currentProject.getGoal()).append("\n");
        rules.append("- 工作目录: ").append(currentProject.getWorkDir()).append("\n\n");

        rules.append("## 开发规范\n");
        rules.append("1. **代码规范**\n");
        rules.append("   - 遵循项目现有的代码风格\n");
        rules.append("   - 所有代码必须有中文注释\n");
        rules.append("   - 提交前必须进行代码审查\n\n");

        rules.append("2. **目录结构**\n");
        String dirConfigText = currentProject.getDirectoryConfigText();
        if (!dirConfigText.isEmpty()) {
            rules.append(dirConfigText);
        } else {
            rules.append("   - 按模块组织代码\n");
            rules.append("   - 保持目录结构清晰\n\n");
        }

        rules.append("3. **版本管理**\n");
        rules.append("   - 使用 Git 进行版本控制\n");
        rules.append("   - 提交信息必须清晰描述变更\n");
        rules.append("   - 重要节点创建标签\n\n");

        rules.append("## 运行和部署\n");
        rules.append("1. **本地运行**\n");
        rules.append("   - 按照项目文档中的说明启动项目\n");
        rules.append("   - 确保所有依赖已安装\n\n");

        rules.append("2. **测试验证**\n");
        rules.append("   - 完成功能后必须进行测试\n");
        rules.append("   - 记录测试结果和问题\n\n");

        rules.append("3. **部署流程**\n");
        rules.append("   - 遵循项目的部署文档\n");
        rules.append("   - 部署前确认所有测试通过\n\n");

        rules.append("## 协作规范\n");
        rules.append("1. **沟通机制**\n");
        rules.append("   - 通过消息系统进行沟通\n");
        rules.append("   - 及时汇报工作进度\n\n");

        rules.append("2. **问题处理**\n");
        rules.append("   - 遇到问题及时上报\n");
        rules.append("   - 记录问题和解决方案\n\n");

        rules.append("3. **知识共享**\n");
        rules.append("   - 将经验记录到知识库\n");
        rules.append("   - 定期进行技术分享\n");

        return rules.toString();
    }

    /**
     * 通知团队成员项目运行规则
     *
     * @param projectId 项目 ID
     * @param rules 项目运行规则
     */
    private void notifyTeamProjectRules(String projectId, String rules) {
        // 获取项目的所有 Agent
        List<Agent> agents = agentManager.getAgentsByProject(projectId);
        if (agents == null || agents.isEmpty()) return;

        // 构建通知消息
        String notification = String.format(
            "【项目运行规则通知】\n\n项目 [%s] 已启动，以下是项目运行和部署规则：\n\n%s\n\n请严格遵守以上规则。",
            currentProject.getName(),
            rules.length() > 500 ? rules.substring(0, 500) + "..." : rules
        );

        // 向所有 Agent 发送通知
        for (Agent agent : agents) {
            if (!agent.getId().equals(getId())) { // 不通知自己
                AgentMessage notifyMsg = AgentMessage.builder()
                    .fromAgentId(getId())
                    .toAgentId(agent.getId())
                    .type(AgentMessage.MessageType.NOTIFY)
                    .content(notification)
                    .build();
                sendMessage(notifyMsg);
            }
        }

        // 同时发送管理员通知
        sendNotificationToAdmin("PROJECT_RULES_GENERATED",
            String.format("项目 [%s] 运行规则已生成\n\n%s", currentProject.getName(),
                rules.length() > 300 ? rules.substring(0, 300) + "..." : rules));
    }

    /**
     * 里程碑完成后更新项目概况
     *
     * @param milestone 完成的里程碑
     * @param passed 是否验证通过
     */
    private void updateProjectOverviewAfterMilestone(GameProject.GoalMilestone milestone, boolean passed) {
        if (currentProject == null) return;

        // 构建项目概况
        StringBuilder overview = new StringBuilder();
        overview.append("# 项目概况\n\n");

        // 项目信息
        overview.append("## 项目信息\n");
        overview.append("- **项目名称**: ").append(currentProject.getName()).append("\n");
        overview.append("- **项目目标**: ").append(currentProject.getGoal() != null ? currentProject.getGoal() : "未设置").append("\n");
        overview.append("- **工作目录**: `").append(currentProject.getWorkDir()).append("`\n");
        overview.append("- **当前状态**: ").append(getGoalStatusDescription(currentProject.getGoalStatus())).append("\n");
        overview.append("- **运行状态**: ").append(currentProject.isRunning() ? "运行中" : "未运行").append("\n\n");

        // 部署信息
        String deploymentRules = currentProject.getDeploymentRules();
        if (deploymentRules != null && !deploymentRules.isEmpty()) {
            overview.append("## 部署信息\n");
            overview.append(deploymentRules).append("\n\n");
        }

        // 团队成员
        String projectId = getProjectId();
        if (projectId != null) {
            List<Agent> agents = agentManager.getAgentsByProject(projectId);
            if (!agents.isEmpty()) {
                overview.append("## 团队成员\n");
                for (Agent agent : agents) {
                    String status = agent.isBusy() ? "工作中" : (agent.isAlive() ? "空闲" : "离线");
                    overview.append("- **").append(agent.getName()).append("** (").append(agent.getRole()).append("): ").append(status).append("\n");
                }
                overview.append("\n");
            }
        }

        // 里程碑进度
        overview.append("## 里程碑进度\n");
        int completed = 0;
        int total = currentProject.getMilestones().size();
        for (GameProject.GoalMilestone m : currentProject.getMilestones()) {
            String statusIcon = switch (m.getStatus()) {
                case COMPLETED -> "✅";
                case IN_PROGRESS -> "🔄";
                case BLOCKED -> "🚫";
                default -> "⏳";
            };
            overview.append("- ").append(statusIcon).append(" **").append(m.getTitle()).append("**");
            if (m.getAssignedAgentRole() != null) {
                overview.append(" — ").append(m.getAssignedAgentRole());
            }
            if (m.getStatus() == GameProject.MilestoneStatus.COMPLETED) {
                completed++;
            }
            if (m.getBlockedReason() != null) {
                overview.append(" (阻塞: ").append(m.getBlockedReason()).append(")");
            }
            overview.append("\n");
        }
        double progress = total > 0 ? (double) completed / total * 100 : 0;
        overview.append(String.format("\n**完成率**: %d/%d (%.0f%%)\n\n", completed, total, progress));

        // 最近完成的里程碑
        overview.append("## 最近完成\n");
        overview.append("- **里程碑**: ").append(milestone.getTitle()).append("\n");
        overview.append("- **负责角色**: ").append(milestone.getAssignedAgentRole()).append("\n");
        overview.append("- **验证结果: ").append(passed ? "通过 ✅" : "未通过 ❌").append("\n");
        if (milestone.getVerificationResult() != null) {
            overview.append("- **验证详情**: ").append(milestone.getVerificationResult()).append("\n");
        }
        overview.append("- **完成时间**: ").append(java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");

        // 更新项目概况
        currentProject.setProjectOverview(overview.toString());
        projectManager.saveProjectConfig(currentProject);

        log.info("项目概况已更新: {}", currentProject.getName());
    }

    /**
     * 获取目标状态的中文描述
     */
    private String getGoalStatusDescription(GameProject.GoalStatus status) {
        if (status == null) return "未设置";
        return switch (status) {
            case NOT_STARTED -> "未开始";
            case DECOMPOSING -> "分解中";
            case IN_PROGRESS -> "进行中";
            case REVIEW -> "审查中";
            case COMPLETED -> "已完成";
            case PAUSED -> "已暂停";
        };
    }

    /**
     * 构建包含目录信息的任务内容
     * 在任务 prompt 中包含项目目录结构、可访问目录、上游里程碑产出和游戏模板参考代码
     *
     * @param milestone 里程碑
     * @param task 任务
     * @return 格式化的任务内容
     */
    private String buildTaskContent(GameProject.GoalMilestone milestone, GameProject.MilestoneTask task) {
        StringBuilder content = new StringBuilder();

        // 任务基本信息 + 完整目标描述
        content.append("[里程碑任务]\n\n");
        content.append("## 项目目标\n").append(currentProject.getGoal()).append("\n\n");
        content.append("## 当前里程碑\n");
        content.append("- 标题: ").append(milestone.getTitle()).append("\n");
        content.append("- 描述: ").append(milestone.getDescription() != null ? milestone.getDescription() : "无").append("\n");
        content.append("- 负责角色: ").append(milestone.getAssignedAgentRole()).append("\n");
        content.append("- 任务: ").append(task.getDescription()).append("\n");

        // 注入上游里程碑的完整产出（策划文档、数值配置等）
        String upstreamOutputs = collectUpstreamOutputs(milestone);
        if (!upstreamOutputs.isEmpty()) {
            content.append("\n## 上游工作产出（设计文档/配置）\n");
            content.append("以下是依赖你的上游角色已完成的工作产出，请基于这些内容进行开发：\n\n");
            content.append(upstreamOutputs);
        }

        // 注入游戏模板参考代码（让开发Agent有可参考的实现）
        if (gameTemplateService != null && currentProject.getGoal() != null) {
            List<GameTemplateService.GameTemplate> matchedTemplates = gameTemplateService.matchTemplates(currentProject.getGoal());
            if (!matchedTemplates.isEmpty()) {
                GameTemplateService.GameTemplate bestTemplate = matchedTemplates.get(0);
                String templateContent = bestTemplate.getContent();
                if (templateContent != null && !templateContent.isEmpty()) {
                    content.append("\n## 游戏模板参考代码\n");
                    content.append("以下是与本项目匹配的游戏模板实现，请参考其架构和核心逻辑：\n\n");
                    // 截取合理长度，避免token爆炸
                    content.append(templateContent.length() > 4000 ? templateContent.substring(0, 4000) + "\n...(参考模板截断，请参考以上架构)" : templateContent);
                    content.append("\n");
                }
            }
        }

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
     * 收集上游里程碑的完整产出
     * 遍历当前里程碑的所有依赖里程碑，从项目工作目录中读取它们产出的文件内容
     *
     * @param milestone 当前里程碑
     * @return 格式化的上游产出文本，无产出时返回空字符串
     */
    private String collectUpstreamOutputs(GameProject.GoalMilestone milestone) {
        String projectId = getProjectId();
        if (projectId == null || goalService == null || milestone.getDependencies() == null) {
            return "";
        }

        StringBuilder outputs = new StringBuilder();
        List<GameProject.GoalMilestone> allMilestones = goalService.getMilestones(projectId);
        if (allMilestones == null) return "";

        for (String depId : milestone.getDependencies()) {
            // 查找依赖里程碑
            GameProject.GoalMilestone depMilestone = allMilestones.stream()
                .filter(m -> depId.equals(m.getId()))
                .findFirst()
                .orElse(null);

            if (depMilestone == null) continue;

            outputs.append("### ").append(depMilestone.getTitle());
            outputs.append(" (").append(depMilestone.getAssignedAgentRole()).append(")\n");

            // 读取该里程碑产出的文件
            List<String> outputFiles = collectMilestoneOutputFiles(depMilestone);
            if (!outputFiles.isEmpty()) {
                for (String filePath : outputFiles) {
                    try {
                        java.io.File file = new java.io.File(filePath);
                        if (file.exists() && file.length() > 0) {
                            String fileContent = new String(java.nio.file.Files.readAllBytes(file.toPath()));
                            // 截取单个文件的最大长度
                            if (fileContent.length() > 2000) {
                                fileContent = fileContent.substring(0, 2000) + "\n...(截断)";
                            }
                            outputs.append("**文件: ").append(file.getName()).append("**\n```\n");
                            outputs.append(fileContent);
                            outputs.append("\n```\n\n");
                        }
                    } catch (Exception e) {
                        log.debug("读取上游产出文件失败: {}", filePath);
                    }
                }
            } else {
                // 没有具体文件，显示里程碑描述
                if (depMilestone.getDescription() != null) {
                    outputs.append(depMilestone.getDescription()).append("\n\n");
                }
            }
        }

        return outputs.toString();
    }

    /**
     * 收集里程碑产出的文件路径列表
     * 从里程碑的验证标准中提取文件路径，同时扫描工作目录中的相关文件
     *
     * @param milestone 里程碑
     * @return 产出文件的绝对路径列表
     */
    private List<String> collectMilestoneOutputFiles(GameProject.GoalMilestone milestone) {
        List<String> files = new java.util.ArrayList<>();
        String projectRoot = currentProject.getWorkDir();
        if (projectRoot == null) return files;

        // 从验证标准中提取文件路径
        if (milestone.getVerificationCriteria() != null) {
            for (String criterion : milestone.getVerificationCriteria()) {
                String filePath = extractFilePath(criterion);
                if (filePath != null) {
                    java.io.File file = new java.io.File(projectRoot, filePath);
                    if (file.exists()) {
                        files.add(file.getAbsolutePath());
                    }
                }
            }
        }

        // 如果从标准中没提取到，扫描工作目录中的文档和配置文件
        if (files.isEmpty()) {
            String role = milestone.getAssignedAgentRole();
            if ("system-planner".equals(role) || "numerical-planner".equals(role)) {
                // 策划角色：扫描 .md .txt .json .yaml 文件
                scanFilesByExtension(new java.io.File(projectRoot), files, ".md", ".txt", ".json", ".yaml", ".yml");
            }
        }

        return files;
    }

    /**
     * 递归扫描目录中指定扩展名的文件
     */
    private void scanFilesByExtension(java.io.File dir, List<String> result, String... extensions) {
        if (!dir.exists() || !dir.isDirectory()) return;
        java.io.File[] files = dir.listFiles();
        if (files == null) return;

        for (java.io.File file : files) {
            if (file.isDirectory()) {
                if (!file.getName().equals("node_modules") && !file.getName().equals(".git")
                    && !file.getName().equals(".claude")) {
                    scanFilesByExtension(file, result, extensions);
                }
            } else {
                String name = file.getName().toLowerCase();
                for (String ext : extensions) {
                    if (name.endsWith(ext)) {
                        result.add(file.getAbsolutePath());
                        break;
                    }
                }
            }
        }
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
     * 校验里程碑角色是否有对应的 Agent，缺失的提前发起招聘
     * 在里程碑解析完成后调用，确保所有需要的角色在任务开始前就位
     *
     * @param projectId 项目ID
     * @param milestones 已解析的里程碑列表
     */
    private void validateMilestoneRolesAndRecruit(String projectId, Collection<GameProject.GoalMilestone> milestones) {
        if (agentManager == null || milestones == null) return;

        // 收集所有里程碑需要的角色（去重）
        java.util.Set<String> requiredRoles = milestones.stream()
            .map(GameProject.GoalMilestone::getAssignedAgentRole)
            .filter(Objects::nonNull)
            .collect(java.util.stream.Collectors.toSet());

        // 检查每个角色是否有对应的 Agent
        for (String role : requiredRoles) {
            String agentId = projectId + ":" + role;
            Agent agent = agentManager.getAgent(agentId);
            if (agent == null) {
                log.info("里程碑需要角色 [{}] 但无对应 Agent，发起招聘审批", role);
                requestRecruitApproval(role, "项目里程碑需要该角色");
            }
        }
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

        // 防止重复发起审批：已有审批在等待中，跳过
        if (pendingApprovalMilestoneId != null) {
            log.debug("已有审批等待中: {}，跳过对 {} 的重复审批", pendingApprovalMilestoneId, role);
            return;
        }

        // 检查该角色是否已经有Agent存在
        String agentId = projectId + ":" + role;
        Agent existingAgent = agentManager.getAgent(agentId);
        if (existingAgent != null && existingAgent.isAlive()) {
            log.debug("角色 {} 的 Agent 已存在: {}，跳过重复招聘", role, agentId);
            return;
        }

        // 检查是否有相同角色的待审批招聘请求
        if (hasPendingRecruitForRole(projectId, role)) {
            log.debug("角色 {} 已有待审批的招聘请求，跳过重复发起", role);
            return;
        }

        // 防止对已拒绝的角色重复发起：检查数据库中的拒绝记录
        if (hasRejectedRecruitForRole(projectId, role)) {
            log.debug("角色 {} 的招聘之前已被拒绝，跳过重复发起", role);
            return;
        }

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
     * 检查是否有指定角色的待审批招聘请求
     *
     * @param projectId 项目ID
     * @param role 角色
     * @return 是否有待审批的招聘请求
     */
    private boolean hasPendingRecruitForRole(String projectId, String role) {
        if (approvalService == null) return false;

        try {
            // 查询该角色的待审批招聘请求
            List<ApprovalRequest> pendingRequests = approvalService.getPendingRequestsByProject(projectId);
            return pendingRequests.stream()
                .anyMatch(req -> "CREATE_AGENT".equals(req.getRequestType()) &&
                    req.getRequestData() != null &&
                    req.getRequestData().contains("\"role\":\"" + role + "\""));
        } catch (Exception e) {
            log.warn("检查待审批招聘请求失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查该角色是否有被拒绝的招聘请求（从数据库查询，重启后仍有效）
     */
    private boolean hasRejectedRecruitForRole(String projectId, String role) {
        if (approvalService == null) return false;

        try {
            List<ApprovalRequest> allRequests = approvalService.getRequestsByProject(projectId);
            return allRequests.stream()
                .anyMatch(req -> "CREATE_AGENT".equals(req.getRequestType()) &&
                    req.getStatus() == ApprovalRequest.ApprovalStatus.REJECTED &&
                    req.getRequestData() != null &&
                    req.getRequestData().contains("\"role\":\"" + role + "\""));
        } catch (Exception e) {
            log.warn("检查已拒绝招聘请求失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取指定角色招聘被拒的原因
     *
     * @param projectId 项目ID
     * @param role 角色
     * @return 拒绝原因，未找到返回 "未知原因"
     */
    private String getRecruitRejectionReason(String projectId, String role) {
        if (approvalService == null) return "未知原因";

        try {
            List<ApprovalRequest> allRequests = approvalService.getRequestsByProject(projectId);
            return allRequests.stream()
                .filter(req -> "CREATE_AGENT".equals(req.getRequestType()) &&
                    req.getStatus() == ApprovalRequest.ApprovalStatus.REJECTED &&
                    req.getRequestData() != null &&
                    req.getRequestData().contains("\"role\":\"" + role + "\""))
                .map(ApprovalRequest::getApprovalComment)
                .filter(c -> c != null && !c.isEmpty())
                .findFirst()
                .orElse("未知原因");
        } catch (Exception e) {
            log.warn("获取招聘拒绝原因失败: {}", e.getMessage());
            return "未知原因";
        }
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

            // 记录拒绝标记，防止重复发起
            rejectedMilestones.add("recruit-" + role);

            // 记录驳回原因到知识库，供后续决策参考
            saveExperience("approval_rejected_recruit_" + role + "_" + System.currentTimeMillis(),
                String.format("招聘 %s 被管理员拒绝。原因: %s。教训: 招聘前需更充分地说明必要性。", role, comment));

            // 【关键】分析拒绝原因，采取适应性行动
            handleRecruitRejection(role, comment);
        }
    }

    /**
     * 待处理的招聘拒绝决策
     * 记录管理员拒绝招聘时的信息，等待制作人 AI 在下个工作周期中分析并决策
     */
    private static class PendingRejectionDecision {
        final String role;
        final String reason;
        final long timestamp;

        PendingRejectionDecision(String role, String reason) {
            this.role = role;
            this.reason = reason;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /** 待 AI 处理的招聘拒绝列表（由 onRecruitApprovalCompleted 写入，由 doWork 读取并清空） */
    private final java.util.concurrent.CopyOnWriteArrayList<PendingRejectionDecision> pendingRejectionDecisions = new java.util.concurrent.CopyOnWriteArrayList<>();

    /**
     * 处理招聘被拒绝后的适应性决策
     * 不做硬编码判断，将拒绝信息存储为"待决策"，由制作人 AI 在下一个工作周期中自主分析并采取行动
     *
     * @param role 被拒绝招聘的角色
     * @param reason 管理员填写的拒绝原因
     */
    private void handleRecruitRejection(String role, String reason) {
        // 去重：如果该角色已有待处理的拒绝决策，不重复添加
        boolean alreadyQueued = pendingRejectionDecisions.stream()
            .anyMatch(d -> d.role.equals(role));
        if (alreadyQueued) {
            log.debug("角色 {} 已有待处理的拒绝决策，跳过重复添加", role);
            return;
        }

        // 存储为待决策，交给 AI 处理
        pendingRejectionDecisions.add(new PendingRejectionDecision(role, reason));

        logInfo("REJECTION_QUEUED", String.format("招聘 %s 被拒绝，原因: %s。已加入待决策队列，等待 AI 分析。", role, reason));

        // 记录驳回原因到知识库，供后续决策参考
        saveExperience("approval_rejected_recruit_" + role + "_" + System.currentTimeMillis(),
            String.format("招聘 %s 被管理员拒绝。原因: %s。", role, reason));
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
        String fromAgent = message.getFromAgentId();
        String content = message.getContent();
        log.info("Received report from {}: {}", fromAgent, content);

        saveMemory("last_report_" + fromAgent, content);
        saveKnowledge("team_report_" + System.currentTimeMillis(), content);

        // 缓存到本轮报告列表，供 buildWorkContext 使用
        String preview = content != null && content.length() > 200
            ? content.substring(0, 200) + "..." : content;
        currentCycleReports.add(String.format("[%s] %s", fromAgent, preview));

        // 核心：尝试解析完成信号并更新里程碑
        // 这是解决"协调无响应"的关键——收到完成报告后立即推进里程碑并回复
        boolean handled = tryHandleCompletionReport(fromAgent, content);

        if (handled) {
            // 任务完成时，将完成信息反馈到知识库，促进知识自进化
            feedTaskCompletionToKB("来自 " + fromAgent + " 的任务报告", content, true);
        } else {
            // 非完成报告：给予简短回应，让 Agent 知道消息已收到
            sendReplyToAgent(fromAgent,
                String.format("收到你的报告。我会在下一个工作周期中处理。报告摘要: %s",
                    content != null && content.length() > 100
                        ? content.substring(0, 100) + "..." : content));
        }
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
        String content = message.getContent();
        log.info("Received response from {}: {}", message.getFromAgentId(),
            content.length() > 100 ? content.substring(0, 100) + "..." : content);

        // 处理任务确认消息
        if (content != null && content.startsWith("TASK_ACCEPTED:")) {
            handleTaskConfirmation(message.getFromAgentId(), content);
        }
    }

    /**
     * 处理任务确认消息
     * 当 Agent 确认收到任务时，更新任务追踪状态
     */
    private void handleTaskConfirmation(String agentId, String content) {
        try {
            // 格式：TASK_ACCEPTED:taskId|taskTitle|agentName
            String data = content.substring("TASK_ACCEPTED:".length());
            String[] parts = data.split("\\|");
            if (parts.length >= 2) {
                String taskId = parts[0];
                String taskTitle = parts[1];
                String agentName = parts.length > 2 ? parts[2] : agentId;

                logInfo("TASK_CONFIRMED", String.format("Agent %s 已确认接收任务: %s", agentName, taskTitle));
                log.info("任务确认: {} 已确认接收任务 '{}' (ID: {})", agentName, taskTitle, taskId);

                // 更新看板状态
                if (projectBoard != null && currentProject != null) {
                    projectBoard.updateAgentStatus(currentProject.getId(), agentId,
                        null, agentName, "ACCEPTED", taskTitle);
                }
            }
        } catch (Exception e) {
            log.warn("处理任务确认消息失败: {}", e.getMessage());
        }
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

        // 发送站内信通知到管理员
        if (notificationService != null) {
            try {
                String title = getNotificationTitle(notificationType);
                com.chengxun.gamemaker.web.entity.Notification.NotificationType type =
                    getNotificationType(notificationType);
                // 向所有管理员发送通知（使用偏好系统定义的 key）
                String prefKey = mapToPreferenceKey(notificationType);
                notificationService.notifyAdmins(
                    prefKey,
                    "PRODUCER_" + notificationType,
                    java.util.Map.of("title", title, "content", content),
                    type);
            } catch (Exception e) {
                log.debug("发送管理员通知失败: {}", e.getMessage());
            }
        }

        // 记录日志
        logInfo(notificationType, content);

        // 保存到经验
        saveExperience(notificationType.toLowerCase() + "_" + System.currentTimeMillis(), content);
    }

    /**
     * 发布事件到事件总线
     *
     * @param eventType 事件类型
     * @param data 事件数据
     */
    private void publishEvent(String eventType, Map<String, Object> data) {
        if (eventBus == null || currentProject == null) return;
        try {
            eventBus.publish(currentProject.getId(), eventType, getId(), data);
        } catch (Exception e) {
            log.debug("发布事件失败: {}", e.getMessage());
        }
    }

    /**
     * 更新项目看板状态
     *
     * @param status Agent 状态
     * @param currentTask 当前任务
     */
    private void updateBoardStatus(String status, String currentTask) {
        if (projectBoard == null || currentProject == null) return;
        try {
            projectBoard.updateAgentStatus(
                currentProject.getId(),
                getId(),
                getRole(),
                getName(),
                status,
                currentTask
            );
        } catch (Exception e) {
            log.debug("更新看板失败: {}", e.getMessage());
        }
    }

    /**
     * 根据通知类型生成通知标题
     */
    private String getNotificationTitle(String notificationType) {
        return switch (notificationType) {
            case "WORKFLOW_STARTED" -> "工作流已启动";
            case "WORKFLOW_CREATED" -> "新工作流已创建";
            case "WORKFLOW_HUMAN_APPROVAL_NEEDED" -> "需要人工审批";
            case "WORKFLOW_PRODUCER_APPROVED" -> "工作流已自动审批";
            case "VERSION_ITERATION_STARTED" -> "版本迭代已启动";
            case "RECRUIT_APPROVAL_REQUIRED" -> "招聘审批请求";
            case "RECRUIT_COMPLETED" -> "招聘完成";
            case "RECRUIT_REJECTED" -> "招聘被拒绝";
            case "CREATE_AGENT_APPROVAL_REQUIRED" -> "创建Agent审批请求";
            case "AGENT_CREATED" -> "Agent已创建";
            case "CREATE_AGENT_REJECTED" -> "创建Agent被拒绝";
            case "APPROVAL_REQUIRED" -> "需要审批";
            case "APPROVAL_REJECTED" -> "审批被拒绝";
            case "AGENT_EVALUATED" -> "Agent绩效评估";
            case "DISMISS_REQUEST_SENT" -> "解雇申请已提交";
            case "DISMISS_REJECTED" -> "解雇被拒绝";
            case "TEAM_OPTIMIZATION" -> "团队优化";
            case "AUTO_RECRUIT_REQUEST" -> "自动招聘请求";
            default -> "制作人通知";
        };
    }

    /**
     * 根据通知类型确定通知级别
     */
    private com.chengxun.gamemaker.web.entity.Notification.NotificationType getNotificationType(String notificationType) {
        return switch (notificationType) {
            case "WORKFLOW_HUMAN_APPROVAL_NEEDED", "APPROVAL_REQUIRED",
                 "RECRUIT_APPROVAL_REQUIRED", "CREATE_AGENT_APPROVAL_REQUIRED",
                 "DISMISS_REQUEST_SENT" -> com.chengxun.gamemaker.web.entity.Notification.NotificationType.TASK;
            case "APPROVAL_REJECTED", "RECRUIT_REJECTED", "CREATE_AGENT_REJECTED",
                 "DISMISS_REJECTED" -> com.chengxun.gamemaker.web.entity.Notification.NotificationType.WARNING;
            case "AGENT_EVALUATED", "TEAM_OPTIMIZATION",
                 "AUTO_RECRUIT_REQUEST" -> com.chengxun.gamemaker.web.entity.Notification.NotificationType.INFO;
            default -> com.chengxun.gamemaker.web.entity.Notification.NotificationType.SYSTEM;
        };
    }

    /**
     * 将细粒度通知类型映射到偏好系统定义的 key
     * 偏好系统只有: alert, approval, agent_status, performance, project, system
     */
    private String mapToPreferenceKey(String notificationType) {
        return switch (notificationType) {
            // 审批相关 → approval
            case "WORKFLOW_HUMAN_APPROVAL_NEEDED", "APPROVAL_REQUIRED",
                 "RECRUIT_APPROVAL_REQUIRED", "CREATE_AGENT_APPROVAL_REQUIRED",
                 "DISMISS_REQUEST_SENT", "APPROVAL_REJECTED", "RECRUIT_REJECTED",
                 "CREATE_AGENT_REJECTED", "DISMISS_REJECTED",
                 "RECRUIT_COMPLETED" -> "approval";
            // Agent 生命周期 → agent_status
            case "AGENT_CREATED", "AGENT_EVALUATED", "TEAM_OPTIMIZATION",
                 "AUTO_RECRUIT_REQUEST" -> "agent_status";
            // 工作流/项目相关 → system
            case "WORKFLOW_STARTED", "WORKFLOW_CREATED", "WORKFLOW_PRODUCER_APPROVED",
                 "VERSION_ITERATION_STARTED" -> "system";
            default -> "system";
        };
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

        String name = generateAgentName(role);
        String requestData = String.format(
            "{\"role\":\"%s\",\"name\":\"%s\",\"workDir\":\"%s\",\"taskDescription\":\"%s\"}",
            role, name, currentProject != null ? currentProject.getWorkDir() : "", taskDescription
        );
        String description = String.format(
            "招聘 %s (%s) 用于任务: %s", name, role, taskDescription
        );

        // 创建真实的审批请求
        if (approvalService != null) {
            try {
                ApprovalRequest approvalRequest = approvalService.createRequest(
                    projectId, getId(), "CREATE_AGENT", requestData, description);
                logInfo("AUTO_RECRUIT_APPROVAL", "已发起自动招聘审批，ID: " + approvalRequest.getId() + "，角色: " + role);
                return true;
            } catch (Exception e) {
                log.error("创建自动招聘审批请求失败: {}", e.getMessage());
            }
        }

        // 降级：仅通知
        sendNotificationToAdmin("AUTO_RECRUIT_REQUEST",
            String.format("【自动招聘请求】（审批服务不可用）\n\n项目: %s\n角色: %s\n任务: %s",
                currentProject != null ? currentProject.getName() : projectId, role, taskDescription));
        return true;
    }

    /**
     * 发起自动优化建议
     * 检查团队成员状态，创建解雇审批请求并通知管理员
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

        // 核心角色不允许解雇
        if ("producer".equals(targetAgent.getRole())) {
            logInfo("TEAM_OPTIMIZATION_SKIPPED", "不能优化制作人角色");
            return;
        }

        // 创建真实的解雇审批请求
        if (performanceManagementService != null) {
            try {
                DismissalRequest request = performanceManagementService.submitDismissalRequest(
                    getId(),           // 制作人 ID
                    agentId,           // 被优化 Agent ID
                    "REDUNDANT_ROLE",  // 原因类型：角色冗余
                    reason + " | 建议: " + suggestion
                );

                String description = String.format(
                    "【团队优化建议】\n\n项目: %s\nAgent: %s (%s)\n角色: %s\n\n原因: %s\n建议: %s\n申请编号: %s\n\n请前往「绩效管理」页面审批。",
                    currentProject != null ? currentProject.getName() : projectId,
                    targetAgent.getName(), agentId, targetAgent.getRole(),
                    reason, suggestion, request.getRequestNo()
                );

                sendNotificationToAdmin("TEAM_OPTIMIZATION", description);
                logInfo("TEAM_OPTIMIZATION", "已创建解雇审批请求: " + agentId + ", 申请编号: " + request.getRequestNo());
            } catch (Exception e) {
                log.error("创建解雇审批请求失败: {}", e.getMessage());
                logError("TEAM_OPTIMIZATION_FAILED", "创建审批请求失败: " + agentId, e.getMessage());
            }
        } else {
            // 绩效管理服务不可用，仅发送通知
            String description = String.format(
                "【团队优化建议】（仅通知，审批服务不可用）\n\n项目: %s\nAgent: %s (%s)\n角色: %s\n\n原因: %s\n建议: %s",
                currentProject != null ? currentProject.getName() : projectId,
                targetAgent.getName(), agentId, targetAgent.getRole(),
                reason, suggestion
            );
            sendNotificationToAdmin("TEAM_OPTIMIZATION", description);
            logInfo("TEAM_OPTIMIZATION", "已发送优化通知（无审批服务）: " + agentId);
        }
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
            case "audio-dev" -> "音频设计";
            case "narrative-planner" -> "剧情策划";
            case "level-design" -> "关卡设计";
            case "devops" -> "运维工程师";
            case "security-expert" -> "安全工程师";
            case "data-analyst" -> "数据分析师";
            case "tech-artist" -> "技术美术";
            case "product-manager" -> "产品经理";
            case "localization" -> "本地化专员";
            case "ai-engineer" -> "AI工程师";
            case "performance-engineer" -> "性能优化师";
            default -> role;
        };
        return prefix + "-" + System.currentTimeMillis() % 10000;
    }

    /**
     * 检查团队状态并发起优化建议
     * 使用综合评估体系（7个维度加权）评估每个 Agent，而非简单规则判断
     */
    private void checkTeamAndSuggestOptimization() {
        String projectId = getProjectId();
        if (projectId == null) return;

        List<Agent> agents = agentManager.getAgentsByProject(projectId);
        GameProject project = projectManager.getProject(projectId);
        if (project == null) return;

        StringBuilder evalReport = new StringBuilder();
        evalReport.append("## 团队综合评估报告\n\n");
        evalReport.append("评估维度: 绩效评分(25) + 任务完成率(20) + 工作质量(15) + 协作能力(10) + 角色需求(15) + 活跃程度(10) + 历史贡献(5)\n\n");

        for (Agent agent : agents) {
            if ("producer".equals(agent.getRole())) continue;

            // 使用综合评估体系
            DismissalEvaluationResult evaluation = comprehensiveAgentEvaluation(agent);

            evalReport.append(evaluation.toReport()).append("\n");

            // 根据评估结论采取行动
            switch (evaluation.recommendation) {
                case "DISMISS" -> {
                    // 综合评分过低，发起解雇申请
                    log.warn("团队评估: Agent {} ({}) 综合评分 {:.1f}，建议解雇",
                        evaluation.agentName, evaluation.role, evaluation.totalScore);
                    logInfo("TEAM_EVALUATION", String.format("Agent %s (%s): 综合评分 %.1f - 建议解雇",
                        evaluation.agentName, evaluation.role, evaluation.totalScore));

                    String reason = String.format("综合评估 %.1f/100。扣分项: %s",
                        evaluation.totalScore, String.join("; ", evaluation.penaltyReasons));
                    requestTeamOptimization(evaluation.agentId, "综合评估不合格", reason);
                }
                case "WARN" -> {
                    // 综合评分偏低，发出警告
                    log.warn("团队评估: Agent {} ({}) 综合评分 {:.1f}，需密切观察",
                        evaluation.agentName, evaluation.role, evaluation.totalScore);
                    logInfo("TEAM_EVALUATION", String.format("Agent %s (%s): 综合评分 %.1f - 需密切观察",
                        evaluation.agentName, evaluation.role, evaluation.totalScore));

                    // 发出警告通知（不解雇）
                    sendNotificationToAdmin("TEAM_WARNING",
                        String.format("【团队评估警告】\n\n项目: %s\nAgent: %s (%s)\n综合评分: %.1f/100\n\n%s\n\n扣分项: %s\n\n请关注该 Agent 的表现。",
                            currentProject != null ? currentProject.getName() : projectId,
                            evaluation.agentName, evaluation.role, evaluation.totalScore,
                            evaluation.recommendationText,
                            String.join("; ", evaluation.penaltyReasons)));
                }
                case "OBSERVE" -> {
                    log.info("团队评估: Agent {} ({}) 综合评分 {:.1f}，继续观察",
                        evaluation.agentName, evaluation.role, evaluation.totalScore);
                }
                case "KEEP" -> {
                    log.debug("团队评估: Agent {} ({}) 综合评分 {:.1f}，表现良好",
                        evaluation.agentName, evaluation.role, evaluation.totalScore);
                }
            }
        }

        // 检查里程碑需要但缺失的角色，发起招聘
        if (goalService != null) {
            List<GameProject.GoalMilestone> milestones = goalService.getMilestones(projectId);
            if (milestones != null) {
                Set<String> existingRoles = agents.stream()
                    .map(Agent::getRole)
                    .collect(Collectors.toSet());

                for (GameProject.GoalMilestone m : milestones) {
                    if (m.getStatus() == GameProject.MilestoneStatus.COMPLETED) continue;
                    String neededRole = m.getAssignedAgentRole();
                    if (neededRole != null && !existingRoles.contains(neededRole)) {
                        log.info("团队评估: 里程碑 [{}] 需要角色 {} 但无对应 Agent，发起招聘", m.getTitle(), neededRole);
                        requestRecruitApproval(neededRole, m.getTitle());
                        evalReport.append(String.format("\n**需招聘**: %s（里程碑: %s）\n", neededRole, m.getTitle()));
                    }
                }
            }
        }

        log.info("团队评估完成:\n{}", evalReport);
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

            // 记录驳回原因到知识库
            saveExperience("approval_rejected_create_agent_" + role + "_" + System.currentTimeMillis(),
                String.format("创建 Agent %s (%s) 被管理员拒绝。原因: %s。教训: 需更充分说明招聘必要性。", name, role, comment));

            logDecision("创建 Agent 审批被驳回 — " + name + " (" + role + ")",
                String.format("驳回原因: %s\n\n制作人将根据驳回原因调整策略。", comment));

            sendNotificationToAdmin("CREATE_AGENT_REJECTED",
                String.format("【创建 Agent 被拒绝】\n\n角色: %s\n名称: %s\n原因: %s\n\n制作人将根据原因优化后重试。",
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

            // 记录拒绝标记，防止重复发起
            rejectedMilestones.add(milestoneId);

            // 记录驳回原因到知识库
            saveExperience("approval_rejected_" + milestoneId,
                String.format("审批被拒绝。原因: %s。审批ID: %s", comment, milestoneId));

            // 记录驳回决策日志
            logDecision("审批被驳回 — ID: " + milestoneId,
                String.format("驳回原因: %s\n\n制作人将根据驳回原因在下一工作周期中调整策略。", comment));

            sendNotificationToAdmin("APPROVAL_REJECTED",
                String.format("审批被拒绝，原因: %s\n\n制作人将根据原因优化后重试。", comment));
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
            workflowStartTime = java.time.LocalDateTime.now();

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

            // 保存 API 配置到 AgentContext（持久化）
            if (agent instanceof BaseAgent baseAgent) {
                baseAgent.saveApiConfigToContext(apiKey, apiUrl, model);
            }

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

        // 生成评审周期（当前月份）
        String reviewPeriod = java.time.YearMonth.now().toString();

        // 检查是否已经评审过，如果已评审则跳过
        if (performanceManagementService.hasReviewedInPeriod(agentId, reviewPeriod)) {
            log.info("Agent {} 在评审周期 {} 已经评审过，跳过", agent.getName(), reviewPeriod);
            return;
        }

        try {
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

    // ===== 综合解雇评估体系 =====

    /**
     * 综合评估 Agent 是否应该被解雇
     * 从 7 个维度加权评估，而非仅看绩效分数
     *
     * 评分维度（总分 100）：
     * 1. 绩效评分 (25%) - 制作人历史绩效评审综合分
     * 2. 任务完成率 (20%) - 近期任务完成 vs 失败比例
     * 3. 工作质量 (15%) - 错误率、返工率
     * 4. 协作能力 (10%) - 绩效评审中的协作分
     * 5. 角色需求 (15%) - 当前项目是否还需要该角色
     * 6. 活跃程度 (10%) - 近期是否有有效产出
     * 7. 历史贡献 (5%) - 长期累计贡献
     *
     * @param agent 被评估的 Agent
     * @return 评估结果
     */
    private DismissalEvaluationResult comprehensiveAgentEvaluation(Agent agent) {
        String agentId = agent.getId();
        String role = agent.getRole();
        String projectId = getProjectId();

        DismissalEvaluationResult result = new DismissalEvaluationResult();
        result.agentId = agentId;
        result.agentName = agent.getName();
        result.role = role;

        // 时间窗口
        java.time.LocalDateTime since24h = java.time.LocalDateTime.now().minusHours(24);
        java.time.LocalDateTime since7d = java.time.LocalDateTime.now().minusDays(7);

        // ===== 维度 1: 绩效评分 (25 分满分) =====
        double performanceScore = 0;
        if (performanceManagementService != null) {
            List<PerformanceReview> reviews = performanceManagementService.getAgentReviews(agentId);
            if (!reviews.isEmpty()) {
                PerformanceReview latest = reviews.get(0);
                Integer overall = latest.getOverallScore();
                if (overall != null) {
                    performanceScore = overall / 100.0 * 25;
                    result.latestPerformanceScore = overall;
                    result.latestGrade = latest.getGrade();
                }
                // 检查连续低分期数
                int consecutiveLow = 0;
                for (PerformanceReview r : reviews) {
                    Integer score = r.getOverallScore();
                    if (score != null && score < 60) {
                        consecutiveLow++;
                    } else {
                        break; // 非连续低分就停止
                    }
                }
                result.consecutiveLowScorePeriods = consecutiveLow;
            }
        }
        result.dimensionScores.put("绩效评分", performanceScore);

        // ===== 维度 2: 任务完成率 (20 分满分) =====
        double taskScore = 10; // 默认中等
        if (agentLogService != null) {
            long completed7d = agentLogService.getCompletedTaskCount(agentId, since7d);
            long failed7d = agentLogService.getFailedTaskCount(agentId, since7d);
            long total7d = completed7d + failed7d;
            result.completedTasks7d = completed7d;
            result.failedTasks7d = failed7d;

            if (total7d > 0) {
                double completionRate = (double) completed7d / total7d;
                taskScore = completionRate * 20;
            } else {
                // 7天无任务记录
                taskScore = 5; // 低于平均
                result.noTaskRecord = true;
            }
        }
        result.dimensionScores.put("任务完成率", taskScore);

        // ===== 维度 3: 工作质量 (15 分满分) =====
        double qualityScore = 7.5; // 默认中等
        if (agentLogService != null) {
            long errors24h = agentLogService.getErrorCount(agentId, since24h);
            long errors7d = agentLogService.getErrorCount(agentId, since7d);
            result.errorCount24h = errors24h;
            result.errorCount7d = errors7d;

            // 错误越少分越高
            if (errors7d == 0) {
                qualityScore = 15;
            } else if (errors7d <= 3) {
                qualityScore = 12;
            } else if (errors7d <= 10) {
                qualityScore = 7.5;
            } else {
                qualityScore = Math.max(0, 15 - errors7d);
            }
        }
        result.dimensionScores.put("工作质量", qualityScore);

        // ===== 维度 4: 协作能力 (10 分满分) =====
        double collaborationScore = 5; // 默认中等
        if (performanceManagementService != null) {
            List<PerformanceReview> reviews = performanceManagementService.getAgentReviews(agentId);
            if (!reviews.isEmpty()) {
                PerformanceReview latest = reviews.get(0);
                Integer collab = latest.getCollaborationScore();
                if (collab != null) {
                    collaborationScore = collab / 100.0 * 10;
                    result.collaborationScore = collab;
                }
            }
        }
        result.dimensionScores.put("协作能力", collaborationScore);

        // ===== 维度 5: 角色需求 (15 分满分) =====
        boolean roleNeeded = isRoleNeeded(projectId, role);
        double roleScore = roleNeeded ? 15 : 0;
        result.roleNeeded = roleNeeded;
        result.dimensionScores.put("角色需求", roleScore);

        // ===== 维度 6: 活跃程度 (10 分满分) =====
        double activityScore = 5; // 默认中等
        if (agentLogService != null) {
            long aiCalls24h = agentLogService.getAiCallCount(agentId, since24h);
            long completed24h = agentLogService.getCompletedTaskCount(agentId, since24h);
            result.aiCalls24h = aiCalls24h;
            result.completedTasks24h = completed24h;

            boolean isBusy = agent.isBusy();
            boolean hasWork = aiCalls24h > 0 || completed24h > 0;

            if (isBusy) {
                activityScore = 10; // 正在工作
            } else if (hasWork) {
                activityScore = 8;  // 近期有活动
            } else {
                // 24小时无活动
                long aiCalls7d = agentLogService.getAiCallCount(agentId, since7d);
                if (aiCalls7d > 0) {
                    activityScore = 4; // 7天内有活动但最近空闲
                } else {
                    activityScore = 1; // 长期无活动
                }
            }
        }
        result.dimensionScores.put("活跃程度", activityScore);

        // ===== 维度 7: 历史贡献 (5 分满分) =====
        double historyScore = 2.5; // 默认中等
        if (agentLogService != null) {
            long totalCompleted = agentLogService.getCompletedTaskCount(agentId, java.time.LocalDateTime.of(2020, 1, 1, 0, 0));
            result.totalCompletedTasks = totalCompleted;

            if (totalCompleted >= 50) {
                historyScore = 5;
            } else if (totalCompleted >= 20) {
                historyScore = 4;
            } else if (totalCompleted >= 10) {
                historyScore = 3;
            } else if (totalCompleted >= 5) {
                historyScore = 2;
            } else {
                historyScore = 1;
            }
        }
        result.dimensionScores.put("历史贡献", historyScore);

        // ===== 计算总分 =====
        result.totalScore = performanceScore + taskScore + qualityScore + collaborationScore
            + roleScore + activityScore + historyScore;

        // ===== 生成评估结论 =====
        if (result.totalScore < 30) {
            result.recommendation = "DISMISS";
            result.recommendationText = "强烈建议解雇：综合评分过低（" + String.format("%.1f", result.totalScore) + "/100），多个维度表现不佳";
        } else if (result.totalScore < 45) {
            result.recommendation = "WARN";
            result.recommendationText = "建议警告：综合评分偏低（" + String.format("%.1f", result.totalScore) + "/100），需密切观察";
        } else if (result.totalScore < 60) {
            result.recommendation = "OBSERVE";
            result.recommendationText = "继续观察：综合评分一般（" + String.format("%.1f", result.totalScore) + "/100），有改进空间";
        } else {
            result.recommendation = "KEEP";
            result.recommendationText = "保留：综合评分良好（" + String.format("%.1f", result.totalScore) + "/100）";
        }

        // 生成详细的扣分原因
        result.penaltyReasons = generatePenaltyReasons(result);

        return result;
    }

    /**
     * 生成扣分原因列表
     */
    private List<String> generatePenaltyReasons(DismissalEvaluationResult result) {
        List<String> reasons = new ArrayList<>();

        if (result.latestPerformanceScore != null && result.latestPerformanceScore < 60) {
            reasons.add(String.format("绩效评分低 (%d/100)", result.latestPerformanceScore));
        }
        if (result.consecutiveLowScorePeriods >= 2) {
            reasons.add(String.format("连续 %d 个评审周期低分", result.consecutiveLowScorePeriods));
        }
        if (result.failedTasks7d > result.completedTasks7d && result.failedTasks7d > 0) {
            reasons.add(String.format("7天失败任务(%d)多于完成(%d)", result.failedTasks7d, result.completedTasks7d));
        }
        if (result.errorCount7d > 10) {
            reasons.add(String.format("7天错误次数过多 (%d次)", result.errorCount7d));
        }
        if (!result.roleNeeded) {
            reasons.add("当前项目不需要该角色");
        }
        if (result.noTaskRecord) {
            reasons.add("7天内无任务记录");
        }
        if (result.completedTasks24h == 0 && result.aiCalls24h == 0 && !result.noTaskRecord) {
            reasons.add("24小时无任何活动");
        }

        return reasons;
    }

    /**
     * 解雇评估结果
     */
    private static class DismissalEvaluationResult {
        String agentId;
        String agentName;
        String role;
        double totalScore; // 0-100
        String recommendation; // DISMISS, WARN, OBSERVE, KEEP
        String recommendationText;
        Map<String, Double> dimensionScores = new LinkedHashMap<>();
        List<String> penaltyReasons = new ArrayList<>();

        // 详细数据
        Integer latestPerformanceScore;
        String latestGrade;
        int consecutiveLowScorePeriods;
        long completedTasks24h;
        long failedTasks7d;
        long completedTasks7d;
        long aiCalls24h;
        long errorCount24h;
        long errorCount7d;
        long totalCompletedTasks;
        Integer collaborationScore;
        boolean roleNeeded;
        boolean noTaskRecord;

        /**
         * 生成评估报告
         */
        String toReport() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("### %s (%s) 综合评估\n\n", agentName, role));
            sb.append(String.format("**总分: %.1f/100 | 结论: %s**\n\n", totalScore, recommendationText));

            sb.append("#### 各维度评分\n");
            for (Map.Entry<String, Double> entry : dimensionScores.entrySet()) {
                double maxScore = switch (entry.getKey()) {
                    case "绩效评分" -> 25;
                    case "任务完成率" -> 20;
                    case "工作质量" -> 15;
                    case "协作能力" -> 10;
                    case "角色需求" -> 15;
                    case "活跃程度" -> 10;
                    case "历史贡献" -> 5;
                    default -> 10;
                };
                sb.append(String.format("- %s: %.1f/%.0f\n", entry.getKey(), entry.getValue(), maxScore));
            }

            if (!penaltyReasons.isEmpty()) {
                sb.append("\n#### 扣分项\n");
                for (String reason : penaltyReasons) {
                    sb.append("- ").append(reason).append("\n");
                }
            }

            sb.append(String.format("\n#### 关键数据\n"));
            sb.append(String.format("- 24h: 完成%d 失败%d AI调用%d 错误%d\n",
                completedTasks24h, failedTasks7d > 0 ? failedTasks7d : 0, aiCalls24h, errorCount24h));
            sb.append(String.format("- 7d: 完成%d 错误%d\n", completedTasks7d, errorCount7d));
            sb.append(String.format("- 累计完成: %d 个任务\n", totalCompletedTasks));

            return sb.toString();
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
