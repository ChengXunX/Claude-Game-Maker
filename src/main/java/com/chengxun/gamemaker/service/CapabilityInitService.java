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
        // 新增角色
        initTesterCapabilities();
        initSecurityExpertCapabilities();
        initDataAnalystCapabilities();
        initTechArtistCapabilities();
        initProductManagerCapabilities();
        initLocalizationCapabilities();
        initAiEngineerCapabilities();
        initPerformanceEngineerCapabilities();
        initAudioDevCapabilities();
        initNarrativePlannerCapabilities();
        initLevelDesignCapabilities();
        initDevOpsCapabilities();
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

        // ===== 5.1 游戏验证类 =====

        save(role, "verifyGameProject", "验证游戏项目", "验证游戏项目的结构完整性和代码质量",
            "verification", false, null, ++priority,
            "{\"projectDir\":\"string\",\"includeQualityAnalysis\":\"boolean|default=true\"}");

        save(role, "verifyGameQuality", "深度质量分析", "使用 AI 深度分析游戏的可玩性、玩法完整性、UI/UX 质量",
            "verification", false, null, ++priority,
            "{\"projectDir\":\"string\",\"projectName\":\"string\",\"projectGoal\":\"string\"}");

        save(role, "verifyAndImprove", "验证并改进", "验证游戏项目并在失败时自动生成改进建议",
            "verification", false, null, ++priority,
            "{\"projectDir\":\"string\",\"autoImprove\":\"boolean|default=false\",\"targetAgent\":\"string\"}");

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

        // ===== 8. 战略决策升级类 =====

        save(role, "escalateStrategicDecision", "升级战略决策", "将重大战略决策升级为人工审批，适用于项目方向、玩法大调整等关键决策",
            "strategic_decision", true, "ESCALATE_DECISION", ++priority,
            "{\"decisionType\":\"enum:PROJECT_DIRECTION,GAMEPLAY_CHANGE,ARCHITECTURE_CHANGE,BUDGET_ALLOCATION,TEAM_RESTRUCTURE,TECHNOLOGY_STACK,RELEASE_STRATEGY|required\",\"description\":\"string|required\",\"impact\":\"string|required\",\"options\":\"string\"}");

        save(role, "requestDelivery", "申请项目交付", "当项目满足交付条件时，申请管理员审批交付。这是重大决策，需要人工确认",
            "strategic_decision", true, "DELIVERY", ++priority,
            "{\"reason\":\"string\"}");

        save(role, "evaluateNextVersion", "评估下一版本", "评估当前版本完成情况，决定是否需要规划下一个版本或申请交付",
            "project_management", false, null, ++priority,
            "{}");

        save(role, "evaluateCurrentVersion", "评估当前版本", "分析当前版本的Agent绩效、人手缺失与冗余，生成版本评估报告",
            "project_management", false, null, ++priority,
            "{\"milestoneId\":\"string|required\"}");

        // ===== 9. 测试管理类 =====

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

        // 代码开发能力
        save(role, "executeCode", "执行代码任务", "在工作目录中执行代码开发任务",
            "code", false, null, 1,
            "{\"taskDescription\":\"string|required\"}");

        save(role, "reviewCode", "审查代码", "审查指定代码文件或目录的质量",
            "code", false, null, 2,
            "{\"targetPath\":\"string\",\"scope\":\"string\"}");

        save(role, "commitCode", "提交代码", "将工作目录中的代码变更提交到 Git",
            "code", false, null, 3,
            "{\"message\":\"string\",\"files\":\"string\"}");

        save(role, "debugCode", "调试代码", "调试代码问题",
            "code", false, null, 4,
            "{\"targetPath\":\"string\",\"issue\":\"string|required\"}");

        save(role, "refactorCode", "重构代码", "重构代码以提高质量",
            "code", false, null, 5,
            "{\"targetPath\":\"string\",\"refactorType\":\"enum:extract,inline,movemethod,renames\"}");

        // 测试能力
        save(role, "runTests", "运行测试", "运行项目测试",
            "testing", false, null, 10,
            "{\"testType\":\"enum:unit,integration,all\",\"scope\":\"string\"}");

        // 构建和部署能力
        save(role, "buildProject", "构建项目", "构建项目",
            "build", false, null, 20,
            "{\"buildType\":\"enum:debug,release\",\"platform\":\"string\"}");

        // 汇报能力
        save(role, "reportProgress", "汇报进度", "向制作人汇报当前任务进度",
            "communication", false, null, 30,
            "{\"taskId\":\"string\",\"progress\":\"string|required\"}");

        log.info("ServerDev capabilities initialized");
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

        save(role, "decomposeTasks", "分解任务", "将里程碑分解为具体可执行的任务，包含输入/输出/验收标准",
            "project_management", false, null, 6,
            "{\"milestoneId\":\"string|required\",\"milestoneTitle\":\"string|required\",\"milestoneDescription\":\"string\",\"goal\":\"string\"}");

        log.info("SystemPlanner capabilities initialized: 6");
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

    // ===== Tester 能力集 =====

    private void initTesterCapabilities() {
        String role = "tester";

        // 测试计划和执行
        save(role, "createTestPlan", "制定测试计划", "为项目制定测试计划和测试用例",
            "testing", false, null, 1,
            "{\"testScope\":\"string|required\",\"testTypes\":\"enum:functional,performance,compatibility,security\",\"priority\":\"enum:high,medium,low\"}");

        save(role, "executeTest", "执行测试", "执行测试用例并记录结果",
            "testing", false, null, 2,
            "{\"testType\":\"enum:functional,performance,regression|required\",\"scope\":\"string\"}");

        save(role, "runAutomatedTest", "运行自动化测试", "运行自动化测试脚本",
            "testing", false, null, 3,
            "{\"testScript\":\"string|required\",\"environment\":\"string\"}");

        // 缺陷管理
        save(role, "reportBug", "报告缺陷", "发现 Bug 时创建缺陷报告",
            "testing", false, null, 10,
            "{\"title\":\"string|required\",\"severity\":\"enum:critical,high,medium,low|required\",\"steps\":\"string|required\",\"expected\":\"string\",\"actual\":\"string\"}");

        save(role, "manageBugList", "管理缺陷列表", "查看和管理项目中的 Bug 列表",
            "testing", false, null, 11,
            "{\"action\":\"enum:list,assign,prioritize,close|required\",\"bugId\":\"string\",\"assignee\":\"string\"}");

        // 质量分析
        save(role, "reviewTestResults", "审查测试结果", "审查测试报告，评估质量状态",
            "testing", false, null, 20,
            "{\"testReportId\":\"string\",\"focus\":\"string\"}");

        save(role, "analyzeQuality", "质量分析", "分析代码和测试质量",
            "testing", false, null, 21,
            "{\"scope\":\"enum:code,tests,coverage\",\"metrics\":\"string\"}");

        save(role, "verifyGameQuality", "游戏质量验证", "验证游戏项目的质量",
            "testing", false, null, 22,
            "{\"projectDir\":\"string\",\"projectName\":\"string\"}");

        // 汇报能力
        save(role, "reportProgress", "汇报进度", "向制作人汇报测试进度",
            "communication", false, null, 30,
            "{\"taskId\":\"string\",\"progress\":\"string|required\"}");

        log.info("Tester capabilities initialized");
    }

    // ===== SecurityExpert 能力集 =====

    private void initSecurityExpertCapabilities() {
        String role = "security-expert";

        save(role, "auditCode", "代码安全审计", "对代码进行安全审计，检测潜在漏洞",
            "task", false, null, 1,
            "{\"targetPath\":\"string\",\"scope\":\"enum:full,api,auth,data\",\"depth\":\"enum:basic,thorough\"}");

        save(role, "scanVulnerability", "漏洞扫描", "扫描项目中的安全漏洞",
            "task", false, null, 2,
            "{\"scanType\":\"enum:sql_injection,xss,csrf,auth\",\"target\":\"string\"}");

        save(role, "reviewSecurityDesign", "审查安全设计", "审查系统架构的安全性",
            "task", false, null, 3,
            "{\"designType\":\"enum:auth,data,api,infra\",\"description\":\"string\"}");

        save(role, "createSecurityReport", "生成安全报告", "生成安全审计报告",
            "task", false, null, 4,
            "{\"scope\":\"string\",\"includeRecommendations\":\"boolean\"}");

        save(role, "validateInput", "输入验证检查", "检查输入验证逻辑的安全性",
            "task", false, null, 5,
            "{\"targetPath\":\"string\",\"inputType\":\"enum:form,api,url\"}");

        save(role, "alertSecurityRisk", "安全风险预警", "发现安全风险时向管理员预警",
            "communication", false, null, 6,
            "{\"riskType\":\"string|required\",\"severity\":\"enum:critical,high,medium,low|required\",\"description\":\"string|required\"}");

        save(role, "reportProgress", "汇报进度", "向制作人汇报安全审计进度",
            "communication", false, null, 7,
            "{\"taskId\":\"string\",\"progress\":\"string|required\"}");

        log.info("SecurityExpert capabilities initialized: 7");
    }

    // ===== DataAnalyst 能力集 =====

    private void initDataAnalystCapabilities() {
        String role = "data-analyst";

        save(role, "analyzeRetention", "留存分析", "分析用户留存数据",
            "task", false, null, 1,
            "{\"period\":\"enum:day1,day7,day30\",\"segment\":\"string\",\"startDate\":\"string\",\"endDate\":\"string\"}");

        save(role, "analyzeMonetization", "付费分析", "分析用户付费行为",
            "task", false, null, 2,
            "{\"metrics\":\"enum:arpu,arppu,ltv,conversion\",\"period\":\"string\",\"segment\":\"string\"}");

        save(role, "analyzeUserBehavior", "行为分析", "分析用户行为路径和模式",
            "task", false, null, 3,
            "{\"metrics\":\"enum:retention,engagement,funnel,cohort\",\"period\":\"string\"}");

        save(role, "designABTest", "设计 AB 测试", "设计 AB 测试实验方案",
            "task", false, null, 4,
            "{\"hypothesis\":\"string|required\",\"metrics\":\"string\",\"sampleSize\":\"number\"}");

        save(role, "generateDataReport", "生成数据报告", "生成数据分析报告",
            "task", false, null, 5,
            "{\"reportType\":\"enum:daily,weekly,monthly,custom|required\",\"metrics\":\"string\"}");

        save(role, "collectFeedback", "收集用户反馈", "从各渠道收集用户反馈",
            "task", false, null, 6,
            "{\"channels\":\"enum:appstore,social,survey,feedback|required\",\"period\":\"string\"}");

        save(role, "analyzeFeedback", "分析用户反馈", "分析用户反馈，提取关键洞察",
            "task", false, null, 7,
            "{\"feedbackId\":\"string\",\"focus\":\"string\"}");

        save(role, "reportProgress", "汇报进度", "向制作人汇报数据分析进度",
            "communication", false, null, 8,
            "{\"taskId\":\"string\",\"progress\":\"string|required\"}");

        log.info("DataAnalyst capabilities initialized: 8");
    }

    // ===== TechArtist 能力集 =====

    private void initTechArtistCapabilities() {
        String role = "tech-artist";

        save(role, "createShader", "创建 Shader", "创建或修改 Shader 代码",
            "task", false, null, 1,
            "{\"shaderType\":\"enum:surface,post_process,particle,ui|required\",\"requirements\":\"string|required\"}");

        save(role, "optimizeRendering", "优化渲染", "优化渲染性能",
            "task", false, null, 2,
            "{\"targetPath\":\"string\",\"optimizationType\":\"enum:drawcall,shader,memory,overdraw\"}");

        save(role, "createArtTool", "创建美术工具", "创建批量处理或自动化工具",
            "task", false, null, 3,
            "{\"toolType\":\"enum:batch_convert,validation,preview,export|required\",\"requirements\":\"string\"}");

        save(role, "reviewShader", "审查 Shader", "审查 Shader 代码的性能和质量",
            "task", false, null, 4,
            "{\"targetPath\":\"string\",\"focus\":\"enum:performance,quality,compatibility\"}");

        save(role, "setupRenderPipeline", "配置渲染管线", "配置或优化渲染管线",
            "task", false, null, 5,
            "{\"pipelineType\":\"enum:forward,deferred,hybrid\",\"requirements\":\"string\"}");

        save(role, "reportProgress", "汇报进度", "向制作人汇报技术美术进度",
            "communication", false, null, 6,
            "{\"taskId\":\"string\",\"progress\":\"string|required\"}");

        log.info("TechArtist capabilities initialized: 6");
    }

    // ===== ProductManager 能力集 =====

    private void initProductManagerCapabilities() {
        String role = "product-manager";

        save(role, "analyzeRequirements", "需求分析", "分析和评估产品需求",
            "task", false, null, 1,
            "{\"requirement\":\"string|required\",\"stakeholders\":\"string\",\"priority\":\"enum:high,medium,low\"}");

        save(role, "createPRD", "创建需求文档", "创建产品需求文档 (PRD)",
            "task", false, null, 2,
            "{\"featureName\":\"string|required\",\"userStory\":\"string\",\"acceptanceCriteria\":\"string\"}");

        save(role, "prioritizeFeatures", "特性优先级", "评估和排序产品特性的优先级",
            "task", false, null, 3,
            "{\"features\":\"string|required\",\"criteria\":\"enum:impact,effort,risk,value\"}");

        save(role, "analyzeCompetitor", "竞品分析", "分析竞品的优劣势",
            "task", false, null, 4,
            "{\"competitors\":\"string|required\",\"focus\":\"enum:features,monetization,ux,market\"}");

        save(role, "designUserJourney", "用户旅程设计", "设计用户旅程和体验流程",
            "task", false, null, 5,
            "{\"scenario\":\"string|required\",\"persona\":\"string\"}");

        save(role, "reviewDesign", "审查设计", "审查设计方案的用户体验",
            "task", false, null, 6,
            "{\"designType\":\"enum:ui,ux,flow,feature\",\"target\":\"string\"}");

        save(role, "reportProgress", "汇报进度", "向制作人汇报产品进度",
            "communication", false, null, 7,
            "{\"taskId\":\"string\",\"progress\":\"string|required\"}");

        log.info("ProductManager capabilities initialized: 7");
    }

    // ===== Localization 能力集 =====

    private void initLocalizationCapabilities() {
        String role = "localization";

        save(role, "translateText", "翻译文本", "将文本翻译为目标语言",
            "task", false, null, 1,
            "{\"text\":\"string|required\",\"sourceLang\":\"string\",\"targetLang\":\"string|required\",\"context\":\"string\"}");

        save(role, "reviewTranslation", "审查翻译", "审查翻译质量",
            "task", false, null, 2,
            "{\"file\":\"string\",\"targetLang\":\"string\",\"focus\":\"enum:accuracy,fluency,consistency\"}");

        save(role, "checkLocalization", "本地化检查", "检查本地化适配问题",
            "task", false, null, 3,
            "{\"targetLang\":\"string\",\"checkType\":\"enum:text_length,encoding,cultural,format\"}");

        save(role, "manageTerminology", "术语管理", "管理游戏术语表",
            "task", false, null, 4,
            "{\"action\":\"enum:add,update,query,export\",\"term\":\"string\",\"definition\":\"string\"}");

        save(role, "createGlossary", "创建术语表", "创建多语言术语表",
            "task", false, null, 5,
            "{\"domain\":\"string\",\"languages\":\"string\"}");

        save(role, "reportProgress", "汇报进度", "向制作人汇报本地化进度",
            "communication", false, null, 6,
            "{\"taskId\":\"string\",\"progress\":\"string|required\"}");

        log.info("Localization capabilities initialized: 6");
    }

    // ===== AiEngineer 能力集 =====

    private void initAiEngineerCapabilities() {
        String role = "ai-engineer";

        save(role, "createBehaviorTree", "创建行为树", "为 NPC 创建行为树",
            "task", false, null, 1,
            "{\"npcType\":\"string|required\",\"behaviors\":\"string|required\",\"complexity\":\"enum:simple,medium,complex\"}");

        save(role, "implementPathfinding", "实现寻路算法", "实现或优化寻路算法",
            "task", false, null, 2,
            "{\"algorithm\":\"enum:astar,navmesh,flowfield|required\",\"requirements\":\"string\"}");

        save(role, "designDialogueSystem", "设计对话系统", "设计分支对话系统",
            "task", false, null, 3,
            "{\"dialogueType\":\"enum:branching,ai_generated,hybrid\",\"features\":\"string\"}");

        save(role, "trainModel", "训练模型", "训练机器学习模型",
            "task", false, null, 4,
            "{\"modelType\":\"enum:classification,regression,reinforcement\",\"dataset\":\"string\",\"objective\":\"string\"}");

        save(role, "optimizeAI", "优化 AI 性能", "优化 AI 系统的性能",
            "task", false, null, 5,
            "{\"target\":\"enum:behavior_tree,pathfinding,dialogue\",\"optimization\":\"enum:speed,memory,quality\"}");

        save(role, "reportProgress", "汇报进度", "向制作人汇报 AI 开发进度",
            "communication", false, null, 6,
            "{\"taskId\":\"string\",\"progress\":\"string|required\"}");

        log.info("AiEngineer capabilities initialized: 6");
    }

    // ===== PerformanceEngineer 能力集 =====

    private void initPerformanceEngineerCapabilities() {
        String role = "performance-engineer";

        save(role, "profilePerformance", "性能分析", "对系统进行性能分析",
            "task", false, null, 1,
            "{\"target\":\"enum:cpu,gpu,memory,network\",\"scope\":\"enum:full,specific\",\"duration\":\"string\"}");

        save(role, "identifyBottleneck", "瓶颈定位", "定位性能瓶颈",
            "task", false, null, 2,
            "{\"symptom\":\"string|required\",\"component\":\"enum:client,server,database,api\"}");

        save(role, "optimizeCode", "代码优化", "优化代码性能",
            "task", false, null, 3,
            "{\"targetPath\":\"string\",\"optimizationType\":\"enum:algorithm,caching,async,batch\"}");

        save(role, "setupMonitoring", "配置监控", "配置性能监控",
            "task", false, null, 4,
            "{\"metrics\":\"string\",\"thresholds\":\"string\",\"alerting\":\"boolean\"}");

        save(role, "runLoadTest", "压力测试", "执行压力测试",
            "task", false, null, 5,
            "{\"concurrentUsers\":\"number\",\"duration\":\"string\",\"scenario\":\"string\"}");

        save(role, "generatePerformanceReport", "生成性能报告", "生成性能分析报告",
            "task", false, null, 6,
            "{\"scope\":\"string\",\"includeRecommendations\":\"boolean\"}");

        save(role, "reportProgress", "汇报进度", "向制作人汇报性能优化进度",
            "communication", false, null, 7,
            "{\"taskId\":\"string\",\"progress\":\"string|required\"}");

        log.info("PerformanceEngineer capabilities initialized: 7");
    }

    // ===== AudioDev 能力集 =====

    private void initAudioDevCapabilities() {
        String role = "audio-dev";

        save(role, "designSoundEffect", "设计音效", "设计游戏音效",
            "task", false, null, 1,
            "{\"sfxType\":\"enum:ui,combat,environment,character|required\",\"requirements\":\"string\"}");

        save(role, "planMusic", "规划音乐", "规划背景音乐方案",
            "task", false, null, 2,
            "{\"scene\":\"string\",\"mood\":\"string\",\"style\":\"string\"}");

        save(role, "designAudioSystem", "设计音频系统", "设计音频架构",
            "task", false, null, 3,
            "{\"systemType\":\"enum:mixer,3d_audio,adaptive,events\",\"requirements\":\"string\"}");

        save(role, "optimizeAudio", "优化音频", "优化音频资源和播放",
            "task", false, null, 4,
            "{\"optimizationType\":\"enum:format,memory,latency,quality\",\"target\":\"string\"}");

        save(role, "createAudioAssets", "创建音频资源", "创建或处理音频资源",
            "task", false, null, 5,
            "{\"assetType\":\"enum:sfx,bgm,voice,ambient\",\"format\":\"enum:wav,ogg,mp3\"}");

        save(role, "reportProgress", "汇报进度", "向制作人汇报音频开发进度",
            "communication", false, null, 6,
            "{\"taskId\":\"string\",\"progress\":\"string|required\"}");

        log.info("AudioDev capabilities initialized: 6");
    }

    // ===== NarrativePlanner 能力集 =====

    private void initNarrativePlannerCapabilities() {
        String role = "narrative-planner";

        save(role, "buildWorldview", "构建世界观", "构建游戏世界观设定",
            "task", false, null, 1,
            "{\"scope\":\"enum:world,region,faction\",\"depth\":\"enum:brief,detailed,comprehensive\"}");

        save(role, "designCharacter", "设计角色", "设计游戏角色",
            "task", false, null, 2,
            "{\"roleType\":\"enum:protagonist,antagonist,npc,side\",\"importance\":\"enum:main,secondary,minor\"}");

        save(role, "writeStoryline", "编写剧情", "编写游戏剧情",
            "task", false, null, 3,
            "{\"storyType\":\"enum:main_quest,side_quest,hidden_event\",\"chapter\":\"string\"}");

        save(role, "designDialogue", "设计对话", "设计角色对话",
            "task", false, null, 4,
            "{\"character\":\"string\",\"scene\":\"string\",\"branching\":\"boolean\"}");

        save(role, "createStoryBible", "创建故事圣经", "创建完整的故事设定文档",
            "task", false, null, 5,
            "{\"scope\":\"enum:full,characters,world,plot\"}");

        save(role, "reviewNarrative", "审查剧情", "审查剧情设计的一致性",
            "task", false, null, 6,
            "{\"target\":\"string\",\"focus\":\"enum:consistency,pacing,character,dialogue\"}");

        save(role, "reportProgress", "汇报进度", "向制作人汇报剧情设计进度",
            "communication", false, null, 7,
            "{\"taskId\":\"string\",\"progress\":\"string|required\"}");

        log.info("NarrativePlanner capabilities initialized: 7");
    }

    // ===== LevelDesign 能力集 =====

    private void initLevelDesignCapabilities() {
        String role = "level-design";

        save(role, "designLevel", "设计关卡", "设计游戏关卡",
            "task", false, null, 1,
            "{\"levelType\":\"enum:tutorial,challenge,boss,hidden\",\"difficulty\":\"enum:easy,medium,hard\"}");

        save(role, "layoutMap", "地图布局", "设计地图布局",
            "task", false, null, 2,
            "{\"mapSize\":\"enum:small,medium,large\",\"theme\":\"string\",\"pathType\":\"enum:linear,open,branching\"}");

        save(role, "configureEnemies", "配置敌人", "配置关卡敌人",
            "task", false, null, 3,
            "{\"enemyTypes\":\"string\",\"difficulty\":\"enum:easy,medium,hard\",\"spawnPattern\":\"string\"}");

        save(role, "designDifficultyCurve", "设计难度曲线", "设计关卡难度曲线",
            "task", false, null, 4,
            "{\"totalLevels\":\"number\",\"peakLevel\":\"number\",\"restLevels\":\"string\"}");

        save(role, "placeRewards", "配置奖励", "配置关卡奖励",
            "task", false, null, 5,
            "{\"rewardType\":\"enum:currency,item,unlock,experience\",\"distribution\":\"string\"}");

        save(role, "reviewLevel", "审查关卡", "审查关卡设计质量",
            "task", false, null, 6,
            "{\"levelId\":\"string\",\"focus\":\"enum:flow,difficulty,rewards,pacing\"}");

        save(role, "reportProgress", "汇报进度", "向制作人汇报关卡设计进度",
            "communication", false, null, 7,
            "{\"taskId\":\"string\",\"progress\":\"string|required\"}");

        log.info("LevelDesign capabilities initialized: 7");
    }

    // ===== DevOps 能力集 =====

    private void initDevOpsCapabilities() {
        String role = "devops";

        save(role, "setupCICD", "配置 CI/CD", "配置持续集成/持续部署流水线",
            "task", false, null, 1,
            "{\"platform\":\"enum:jenkins,github_actions,gitlab_ci\",\"stages\":\"string\"}");

        save(role, "deployService", "部署服务", "部署服务到指定环境",
            "task", true, "DEPLOY", 2,
            "{\"environment\":\"enum:dev,staging,prod|required\",\"version\":\"string\",\"strategy\":\"enum:rolling,blue_green,canary\"}");

        save(role, "configureMonitoring", "配置监控", "配置系统监控和告警",
            "task", false, null, 3,
            "{\"metrics\":\"string\",\"thresholds\":\"string\",\"channels\":\"string\"}");

        save(role, "optimizeInfrastructure", "优化基础设施", "优化服务器和基础设施",
            "task", false, null, 4,
            "{\"target\":\"enum:compute,storage,network\",\"optimization\":\"enum:cost,performance,reliability\"}");

        save(role, "manageContainers", "管理容器", "管理 Docker 容器",
            "task", false, null, 5,
            "{\"action\":\"enum:build,deploy,scale,monitor\",\"service\":\"string\"}");

        save(role, "setupHotUpdate", "配置热更新", "配置不停服更新机制",
            "task", false, null, 6,
            "{\"strategy\":\"enum:client_patch,config_reload,hotfix\",\"scope\":\"string\"}");

        save(role, "createBuild", "创建构建", "创建项目构建",
            "task", false, null, 7,
            "{\"buildType\":\"enum:debug,release|required\",\"platform\":\"enum:android,ios,web,pc\"}");

        save(role, "reportProgress", "汇报进度", "向制作人汇报运维进度",
            "communication", false, null, 8,
            "{\"taskId\":\"string\",\"progress\":\"string|required\"}");

        log.info("DevOps capabilities initialized: 8");
    }

    // ===== 通用能力（所有角色） =====

    private void initCommonCapabilities() {
        String[] roles = {"producer", "server-dev", "system-planner", "numerical-planner", "git-commit", "ui-dev",
            "tester", "security-expert", "data-analyst", "tech-artist", "product-manager",
            "localization", "ai-engineer", "performance-engineer", "audio-dev",
            "narrative-planner", "level-design", "devops"};

        for (String role : roles) {
            // ===== 通信能力 =====
            save(role, "sendMessage", "发送消息", "向其他 Agent 发送消息",
                "communication", false, null, 20,
                "{\"targetAgent\":\"string|required\",\"content\":\"string|required\",\"type\":\"string\"}");

            save(role, "broadcastMessage", "广播消息", "向项目内所有 Agent 广播消息",
                "communication", false, null, 21,
                "{\"content\":\"string|required\",\"messageType\":\"enum:info,warning,urgent\"}");

            save(role, "requestHelp", "请求帮助", "向其他 Agent 请求技术帮助",
                "communication", false, null, 22,
                "{\"targetAgent\":\"string\",\"question\":\"string|required\"}");

            // ===== 知识管理能力 =====
            save(role, "saveKnowledge", "保存知识", "将知识保存到记忆系统",
                "knowledge", false, null, 30,
                "{\"key\":\"string|required\",\"value\":\"string|required\"}");

            save(role, "loadKnowledge", "加载知识", "从记忆系统加载知识",
                "knowledge", false, null, 31,
                "{\"key\":\"string|required\"}");

            save(role, "queryKnowledge", "查询知识", "查询知识库中的相关知识",
                "knowledge", false, null, 32,
                "{\"query\":\"string|required\",\"category\":\"string\"}");

            // ===== 上下文管理能力 =====
            save(role, "compactContext", "压缩上下文", "压缩当前对话上下文以释放空间",
                "context", false, null, 40,
                "{}");

            save(role, "recoverContext", "恢复上下文", "从快照恢复工作上下文",
                "context", false, null, 41,
                "{}");

            // ===== 状态汇报能力 =====
            save(role, "reportStatus", "汇报状态", "向制作人汇报当前工作状态",
                "communication", false, null, 50,
                "{\"status\":\"string|required\",\"details\":\"string\"}");

            save(role, "reportProgress", "汇报进度", "汇报任务进度",
                "communication", false, null, 51,
                "{\"taskId\":\"string\",\"progress\":\"string|required\"}");

            // ===== 验证能力 =====
            save(role, "verifyWithCriteria", "标准验证", "根据指定标准验证任务完成情况",
                "verification", false, null, 60,
                "{\"criteria\":\"array\",\"projectRoot\":\"string\",\"reportContent\":\"string\"}");

            save(role, "verifyGameProject", "验证游戏项目", "验证游戏项目的结构完整性和代码质量",
                "verification", false, null, 61,
                "{\"projectDir\":\"string\",\"includeQualityAnalysis\":\"boolean|default=false\"}");

            save(role, "verifyGameQuality", "深度质量分析", "使用 AI 深度分析游戏质量",
                "verification", false, null, 62,
                "{\"projectDir\":\"string\",\"projectName\":\"string\",\"projectGoal\":\"string\"}");

            // ===== 任务管理能力 =====
            save(role, "executeTask", "执行任务", "执行分配的任务",
                "task", false, null, 70,
                "{\"taskDescription\":\"string|required\"}");

            save(role, "requestReview", "请求审查", "请求其他 Agent 审查工作成果",
                "task", false, null, 71,
                "{\"targetAgent\":\"string|required\",\"reviewContent\":\"string|required\"}");
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
