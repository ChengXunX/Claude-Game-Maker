package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.web.entity.AgentCapability;
import com.chengxun.gamemaker.web.repository.AgentCapabilityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * 能力初始化服务
 * 在应用启动时为每个 Agent 角色初始化默认能力集
 * 只在数据库中尚无能力数据时执行初始化
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class CapabilityInitService {

    private static final Logger log = LoggerFactory.getLogger(CapabilityInitService.class);

    private final AgentCapabilityRepository capabilityRepository;
    private final CapabilityRegistry capabilityRegistry;

    public CapabilityInitService(AgentCapabilityRepository capabilityRepository,
                                  CapabilityRegistry capabilityRegistry) {
        this.capabilityRepository = capabilityRepository;
        this.capabilityRegistry = capabilityRegistry;
    }

    /**
     * 应用启动后初始化默认能力
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initDefaultCapabilities() {
        if (capabilityRepository.count() > 0) {
            log.info("Capabilities already initialized ({} records), skipping", capabilityRepository.count());
            capabilityRegistry.reloadAll();
            return;
        }

        log.info("Initializing default capabilities for all agent roles...");

        initProducerCapabilities();
        initServerDevCapabilities();
        initSystemPlannerCapabilities();
        initNumericalPlannerCapabilities();
        initGitCommitCapabilities();
        initUiDevCapabilities();
        initCommonCapabilities();

        capabilityRegistry.reloadAll();
        log.info("Default capabilities initialized successfully");
    }

    // ===== Producer 能力集 =====
    /**
     * 初始化制作人能力集
     *
     * 基于现实中游戏制作人的职责，分为以下几类：
     * 1. 项目规划与管理 - 制定计划、里程碑、资源调度
     * 2. 团队组建与管理 - 招聘、任务分配、绩效评估
     * 3. 产品设计与方向 - 游戏概念、核心玩法、市场定位
     * 4. 质量把控 - 设计审查、代码审查、测试验收
     * 5. 沟通与汇报 - 进度汇报、风险预警、决策请求
     * 6. 商业决策 - 预算管理、商业模式、发行策略
     */
    private void initProducerCapabilities() {
        String role = "producer";
        int priority = 0;

        // ===== 1. 团队管理类 =====

        save(role, "createAgent", "招聘团队成员", "根据项目需求招聘新的 Agent 团队成员，支持预设角色和自定义角色",
            "team_management", true, "CREATE_AGENT", ++priority,
            "{\"name\":\"string|required\",\"role\":\"string|required\",\"workDir\":\"string\",\"description\":\"string\"}");

        save(role, "deleteAgent", "解雇团队成员", "解雇不适合的 Agent 团队成员，此操作不可逆",
            "team_management", true, "DELETE_AGENT", ++priority,
            "{\"agentId\":\"string|required\",\"reason\":\"string|required\"}");

        save(role, "assignWorkDir", "分配工作职责", "为 Agent 分配或调整工作职责和工作目录",
            "team_management", false, null, ++priority,
            "{\"agentId\":\"string|required\",\"workDir\":\"string|required\"}");

        save(role, "assignApiConfig", "配置工作环境", "为 Agent 配置 API 密钥、模型等开发环境",
            "team_management", true, "ASSIGN_API", ++priority,
            "{\"agentId\":\"string|required\",\"apiUrl\":\"string\",\"model\":\"string\"}");

        save(role, "changeAgentConfig", "调整成员配置", "调整 Agent 的配置参数以优化工作表现",
            "team_management", true, "CHANGE_CONFIG", ++priority,
            "{\"agentId\":\"string|required\",\"configKey\":\"string|required\",\"configValue\":\"string|required\"}");

        save(role, "optimizeAgentRole", "优化成员能力", "根据项目进展优化 Agent 的能力组合和工作方式",
            "team_management", false, null, ++priority,
            "{\"agentId\":\"string|required\",\"optimizationType\":\"enum:capabilities,workflow,focus|required\",\"reason\":\"string\"}");

        save(role, "evaluateAgentPerformance", "评估成员绩效", "定期评估 Agent 的工作表现，提供反馈和改进建议",
            "team_management", false, null, ++priority,
            "{\"agentId\":\"string|required\",\"evaluationCriteria\":\"string\",\"period\":\"string\"}");

        // ===== 2. 项目管理类 =====

        save(role, "setProjectGoal", "设定项目目标", "设定或调整项目的整体目标和方向",
            "project_management", true, "SET_GOAL", ++priority,
            "{\"goal\":\"string|required\",\"goalType\":\"enum:GAME_DEVELOPMENT,BUG_FIX,FEATURE,REFACTOR,CUSTOM|required\",\"deadline\":\"string\"}");

        save(role, "decomposeGoal", "分解项目目标", "将项目目标分解为可执行的里程碑和任务",
            "project_management", false, null, ++priority,
            "{\"milestones\":\"string|required\",\"assignRoles\":\"string\"}");

        save(role, "updateMilestone", "更新里程碑", "更新项目里程碑的状态、进度或调整计划",
            "project_management", false, null, ++priority,
            "{\"milestoneId\":\"string|required\",\"status\":\"enum:PENDING,IN_PROGRESS,COMPLETED,BLOCKED\",\"progress\":\"number\"}");

        save(role, "adjustProjectPlan", "调整项目计划", "根据实际情况调整项目计划、资源分配或优先级",
            "project_management", true, "ADJUST_PLAN", ++priority,
            "{\"adjustmentType\":\"enum:priority,resource,timeline,scope|required\",\"description\":\"string|required\",\"reason\":\"string\"}");

        save(role, "manageProjectRisk", "管理项目风险", "识别、评估和应对项目风险",
            "project_management", false, null, ++priority,
            "{\"riskType\":\"enum:schedule,quality,resource,technical|required\",\"severity\":\"enum:low,medium,high,critical|required\",\"description\":\"string|required\",\"mitigation\":\"string\"}");

        // ===== 3. 任务分配类 =====

        save(role, "sendTaskToAgent", "分配任务", "向指定 Agent 分配具体的工作任务",
            "task_management", false, null, ++priority,
            "{\"targetAgent\":\"string|required\",\"taskContent\":\"string|required\",\"priority\":\"enum:low,medium,high,urgent\",\"deadline\":\"string\"}");

        save(role, "broadcastMessage", "广播通知", "向项目内所有 Agent 发送重要通知或指令",
            "task_management", false, null, ++priority,
            "{\"content\":\"string|required\",\"messageType\":\"enum:info,warning,urgent\"}");

        save(role, "requestReview", "发起审查", "请求 Agent 审查某项工作成果或设计方案",
            "task_management", false, null, ++priority,
            "{\"targetAgent\":\"string|required\",\"reviewContent\":\"string|required\",\"reviewType\":\"enum:code,design,document\"}");

        save(role, "coordinateTeamWork", "协调团队协作", "协调多个 Agent 之间的工作协作和依赖关系",
            "task_management", false, null, ++priority,
            "{\"agents\":\"string|required\",\"collaborationType\":\"enum:sequential,parallel,review|required\",\"description\":\"string|required\"}");

        // ===== 4. 产品设计类 =====

        save(role, "defineGameConcept", "定义游戏概念", "定义游戏的核心概念、玩法和特色",
            "product_design", false, null, ++priority,
            "{\"gameName\":\"string|required\",\"genre\":\"string\",\"coreGameplay\":\"string|required\",\"targetAudience\":\"string\",\"uniqueFeatures\":\"string\"}");

        save(role, "setDesignDirection", "设定设计方向", "为游戏设定整体的设计方向和风格指南",
            "product_design", false, null, ++priority,
            "{\"artStyle\":\"string\",\"gameplayStyle\":\"string\",\"technicalRequirements\":\"string\",\"designPrinciples\":\"string\"}");

        save(role, "prioritizeFeatures", "特性优先级", "评估和排序游戏特性的优先级",
            "product_design", false, null, ++priority,
            "{\"features\":\"string|required\",\"prioritizationCriteria\":\"enum:impact,effort,risk,value\"}");

        save(role, "summarizeGameDirection", "总结游戏方向", "分析项目进展，总结游戏特征，规划发展方向",
            "product_design", false, null, ++priority,
            "{\"scope\":\"enum:current,milestone,full|default=current\",\"includeRecommendations\":\"boolean|default=true\"}");

        // ===== 5. 质量把控类 =====

        save(role, "reviewDesign", "审查设计方案", "审查游戏设计方案的完整性、可行性和创新性",
            "quality_control", false, null, ++priority,
            "{\"designType\":\"enum:system,level,numerical,ui|required\",\"designName\":\"string|required\"}");

        save(role, "reviewCode", "审查代码质量", "审查代码实现的质量、性能和可维护性",
            "quality_control", false, null, ++priority,
            "{\"targetAgent\":\"string\",\"scope\":\"enum:recent,specific,full\",\"focus\":\"string\"}");

        save(role, "acceptDeliverable", "验收交付物", "验收 Agent 完成的工作成果",
            "quality_control", false, null, ++priority,
            "{\"agentId\":\"string|required\",\"deliverableType\":\"enum:code,design,document,art|required\",\"acceptanceCriteria\":\"string\"}");

        save(role, "manageBugs", "管理缺陷", "跟踪和管理项目中的缺陷和问题",
            "quality_control", false, null, ++priority,
            "{\"action\":\"enum:report,assign,prioritize,close|required\",\"bugId\":\"string\",\"severity\":\"enum:low,medium,high,critical\"}");

        // ===== 6. 监控与报告类 =====

        save(role, "queryAgentStatus", "查询成员状态", "查询指定 Agent 的当前工作状态和任务进展",
            "monitoring", false, null, ++priority,
            "{\"agentId\":\"string|required\"}");

        save(role, "getProjectStatus", "获取项目状态", "获取项目的整体进展、里程碑完成情况和团队状态",
            "monitoring", false, null, ++priority,
            "{\"detailLevel\":\"enum:summary,normal,detailed|default=normal\"}");

        save(role, "generateProgressReport", "生成进度报告", "生成项目进度报告，用于汇报和存档",
            "monitoring", false, null, ++priority,
            "{\"reportType\":\"enum:daily,weekly,milestone,custom|required\",\"recipients\":\"string\"}");

        save(role, "alertRisk", "风险预警", "向管理员发送风险预警通知",
            "monitoring", false, null, ++priority,
            "{\"riskType\":\"string|required\",\"severity\":\"enum:low,medium,high,critical|required\",\"description\":\"string|required\",\"suggestedAction\":\"string\"}");

        // ===== 7. 沟通协调类 =====

        save(role, "reportToAdmin", "向管理员汇报", "向系统管理员汇报重要事项、决策请求或风险预警",
            "communication", false, null, ++priority,
            "{\"reportType\":\"enum:progress,decision,risk,achievement|required\",\"content\":\"string|required\",\"urgency\":\"enum:normal,high,urgent\"}");

        save(role, "notifyUser", "通知用户", "通过飞书等渠道向用户发送通知消息",
            "communication", false, null, ++priority,
            "{\"message\":\"string|required\",\"channel\":\"enum:feishu,email,system\",\"messageType\":\"enum:info,warning,success\"}");

        save(role, "requestDecision", "请求决策", "向管理员请求重要决策的批准",
            "communication", true, "REQUEST_DECISION", ++priority,
            "{\"decisionType\":\"string|required\",\"options\":\"string|required\",\"recommendation\":\"string\",\"rationale\":\"string\"}");

        // ===== 8. 测试管理类 =====

        save(role, "createTestPlan", "制定测试计划", "为项目制定测试计划和测试用例",
            "testing", false, null, ++priority,
            "{\"testScope\":\"string|required\",\"testTypes\":\"enum:functional,performance,compatibility,security\",\"priority\":\"enum:high,medium,low\"}");

        save(role, "reviewTestResults", "审查测试结果", "审查测试报告，评估质量状态",
            "testing", false, null, ++priority,
            "{\"testReportId\":\"string\",\"focus\":\"string\"}");

        save(role, "manageBugList", "管理缺陷列表", "查看和管理项目中的Bug列表",
            "testing", false, null, ++priority,
            "{\"action\":\"enum:list,assign,prioritize,close|required\",\"bugId\":\"string\",\"assignee\":\"string\"}");

        // ===== 9. 部署发布类 =====

        save(role, "createBuild", "创建构建", "触发项目构建，生成可部署的产物",
            "deployment", false, null, ++priority,
            "{\"buildType\":\"enum:debug,release|required\",\"environment\":\"enum:dev,staging,prod\",\"version\":\"string\"}");

        save(role, "deployToEnvironment", "部署到环境", "将构建产物部署到指定环境",
            "deployment", true, "DEPLOY", ++priority,
            "{\"environment\":\"enum:dev,staging,prod|required\",\"version\":\"string\",\"rollbackVersion\":\"string\"}");

        save(role, "publishRelease", "发布版本", "正式发布新版本",
            "deployment", true, "PUBLISH_RELEASE", ++priority,
            "{\"version\":\"string|required\",\"releaseNotes\":\"string\",\"channels\":\"string\"}");

        // ===== 10. 数据分析类 =====

        save(role, "analyzeUserBehavior", "分析用户行为", "分析用户行为数据，洞察用户偏好",
            "analytics", false, null, ++priority,
            "{\"metrics\":\"enum:retention,engagement,conversion,funnel|required\",\"period\":\"string\",\"segment\":\"string\"}");

        save(role, "generateDataReport", "生成数据报告", "生成数据分析报告",
            "analytics", false, null, ++priority,
            "{\"reportType\":\"enum:daily,weekly,monthly,custom|required\",\"metrics\":\"string\"}");

        // ===== 11. 用户反馈类 =====

        save(role, "collectFeedback", "收集用户反馈", "从各渠道收集用户反馈",
            "feedback", false, null, ++priority,
            "{\"channels\":\"enum:appstore,social,survey,feedback|required\",\"period\":\"string\"}");

        save(role, "analyzeFeedback", "分析用户反馈", "分析用户反馈，提取关键洞察",
            "feedback", false, null, ++priority,
            "{\"feedbackId\":\"string\",\"focus\":\"string\"}");

        save(role, "createFeedbackResponse", "创建反馈响应", "针对用户反馈创建响应方案",
            "feedback", false, null, ++priority,
            "{\"feedbackId\":\"string|required\",\"responseType\":\"enum:fix,explain,plan,decline|required\",\"description\":\"string\"}");

        // ===== 12. 预算管理类 =====

        save(role, "setBudget", "设置预算", "设置项目预算和资源配额",
            "budget", true, "SET_BUDGET", ++priority,
            "{\"totalBudget\":\"number|required\",\"breakdown\":\"string\",\"period\":\"string\"}");

        save(role, "trackExpenses", "跟踪支出", "跟踪项目支出情况",
            "budget", false, null, ++priority,
            "{\"period\":\"string\",\"category\":\"string\"}");

        save(role, "optimizeResourceUsage", "优化资源使用", "分析资源使用情况，提出优化建议",
            "budget", false, null, ++priority,
            "{\"resourceType\":\"enum:api,compute,storage,bandwidth\",\"analysisPeriod\":\"string\"}");

        log.info("Producer capabilities initialized: {}", priority);
    }

    // ===== ServerDev 能力集 =====

    private void initServerDevCapabilities() {
        String role = "server-dev";

        save(role, "executeCode", "执行代码任务", "在工作目录中执行代码开发任务",
            "task", false, null, 1,
            "{\"taskDescription\":\"string|required\"}");

        save(role, "reviewCode", "审查代码", "审查指定代码文件或目录的质量",
            "task", false, null, 2,
            "{\"targetPath\":\"string\",\"scope\":\"string\"}");

        save(role, "reportProgress", "汇报进度", "向制作人汇报当前任务进度",
            "communication", false, null, 3,
            "{\"taskId\":\"string\",\"progress\":\"string|required\"}");

        save(role, "requestHelp", "请求帮助", "向其他 Agent 请求技术帮助",
            "communication", false, null, 4,
            "{\"targetAgent\":\"string\",\"question\":\"string|required\"}");

        save(role, "commitCode", "提交代码", "将工作目录中的代码变更提交到 Git",
            "task", false, null, 5,
            "{\"message\":\"string\",\"files\":\"string\"}");

        log.info("ServerDev capabilities initialized: 5");
    }

    // ===== SystemPlanner 能力集 =====

    private void initSystemPlannerCapabilities() {
        String role = "system-planner";

        save(role, "createDesign", "创建设计方案", "为指定系统创建完整的设计方案",
            "task", false, null, 1,
            "{\"systemName\":\"string|required\",\"requirements\":\"string|required\"}");

        save(role, "reviewDesign", "评审设计", "评审已有设计方案的完整性、可行性和合理性",
            "task", false, null, 2,
            "{\"designName\":\"string|required\"}");

        save(role, "coordinateWithAgent", "协调 Agent", "与其他 Agent 进行工作协调",
            "communication", false, null, 3,
            "{\"targetAgent\":\"string|required\",\"topic\":\"string|required\"}");

        save(role, "updateDocuments", "更新文档", "更新项目设计文档",
            "task", false, null, 4,
            "{\"documentType\":\"string\",\"content\":\"string\"}");

        save(role, "reportProgress", "汇报进度", "向制作人汇报设计进度",
            "communication", false, null, 5,
            "{\"taskId\":\"string\",\"progress\":\"string|required\"}");

        log.info("SystemPlanner capabilities initialized: 5");
    }

    // ===== NumericalPlanner 能力集 =====

    private void initNumericalPlannerCapabilities() {
        String role = "numerical-planner";

        save(role, "createNumericalDesign", "创建数值设计", "创建游戏数值设计方案",
            "task", false, null, 1,
            "{\"systemName\":\"string|required\",\"requirements\":\"string\"}");

        save(role, "reviewNumerical", "评审数值", "评审数值设计的合理性",
            "task", false, null, 2,
            "{\"targetDesign\":\"string|required\"}");

        save(role, "balanceCheck", "平衡性检查", "检查数值设计的平衡性",
            "task", false, null, 3,
            "{\"scope\":\"string\"}");

        save(role, "reportProgress", "汇报进度", "向制作人汇报数值策划进度",
            "communication", false, null, 4,
            "{\"taskId\":\"string\",\"progress\":\"string|required\"}");

        log.info("NumericalPlanner capabilities initialized: 4");
    }

    // ===== GitCommit 能力集 =====

    private void initGitCommitCapabilities() {
        String role = "git-commit";

        save(role, "reviewRecentCommits", "审查最近提交", "审查最近的 Git 提交记录",
            "task", false, null, 1,
            "{\"count\":\"number\"}");

        save(role, "reviewAgentCommit", "审查 Agent 提交", "审查指定 Agent 的代码提交",
            "task", false, null, 2,
            "{\"agentId\":\"string|required\"}");

        save(role, "checkAuthorInfo", "检查作者信息", "检查提交的作者信息是否规范",
            "task", false, null, 3,
            "{\"commitId\":\"string\"}");

        save(role, "sendAlert", "发送警报", "向管理员发送 Git 相关警报",
            "communication", false, null, 4,
            "{\"alertType\":\"string|required\",\"content\":\"string|required\"}");

        save(role, "reportProgress", "汇报进度", "向制作人汇报 Git 管理进度",
            "communication", false, null, 5,
            "{\"taskId\":\"string\",\"progress\":\"string|required\"}");

        log.info("GitCommit capabilities initialized: 5");
    }

    // ===== UiDev 能力集 =====

    private void initUiDevCapabilities() {
        String role = "ui-dev";

        save(role, "createUI", "创建 UI 组件", "创建前端 UI 组件或页面",
            "task", false, null, 1,
            "{\"componentName\":\"string|required\",\"requirements\":\"string\"}");

        save(role, "reviewUI", "审查 UI", "审查 UI 组件的质量和一致性",
            "task", false, null, 2,
            "{\"targetPath\":\"string\"}");

        save(role, "optimizeResponsive", "优化响应式", "优化 UI 的响应式布局",
            "task", false, null, 3,
            "{\"targetPath\":\"string\",\"breakpoints\":\"string\"}");

        save(role, "reportProgress", "汇报进度", "向制作人汇报 UI 开发进度",
            "communication", false, null, 4,
            "{\"taskId\":\"string\",\"progress\":\"string|required\"}");

        log.info("UiDev capabilities initialized: 4");
    }

    // ===== 通用能力（所有角色） =====

    private void initCommonCapabilities() {
        String[] roles = {"producer", "server-dev", "system-planner", "numerical-planner", "git-commit", "ui-dev"};

        for (String role : roles) {
            save(role, "sendMessage", "发送消息", "向其他 Agent 发送消息",
                "communication", false, null, 20,
                "{\"targetAgent\":\"string|required\",\"content\":\"string|required\",\"type\":\"string\"}");

            save(role, "saveKnowledge", "保存知识", "将知识保存到记忆系统",
                "monitoring", false, null, 21,
                "{\"key\":\"string|required\",\"value\":\"string|required\"}");

            save(role, "compactContext", "压缩上下文", "压缩当前对话上下文以释放空间",
                "monitoring", false, null, 22,
                "{}");

            save(role, "reportStatus", "汇报状态", "向制作人汇报当前工作状态",
                "communication", false, null, 23,
                "{\"status\":\"string|required\",\"details\":\"string\"}");
        }

        log.info("Common capabilities initialized for {} roles", roles.length);
    }

    // ===== 工具方法 =====

    private void save(String agentRole, String capabilityName, String displayName,
                      String description, String category, boolean requiresApproval,
                      String approvalType, int priority, String paramSchema) {
        AgentCapability cap = new AgentCapability(agentRole, capabilityName, displayName, description, category);
        cap.setRequiresApproval(requiresApproval);
        cap.setApprovalType(approvalType);
        cap.setPriority(priority);
        cap.setParamSchema(paramSchema);
        cap.setEnabled(true);
        capabilityRepository.save(cap);
    }
}
