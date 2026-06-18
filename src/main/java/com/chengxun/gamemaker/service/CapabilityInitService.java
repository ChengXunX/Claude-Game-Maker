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
            log.info("Capabilities already initialized ({} records), checking for new capabilities...", capabilityRepository.count());
            ensureNewCapabilities();
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
        initVerifierCapabilities();
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
        initLspCodeUnderstandingCapabilities();
        initAgentToolsCapabilities();

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
            "team_management", false, null, ++priority,
            "{\"agentId\":\"string|required\",\"apiUrl\":\"string\",\"model\":\"string\"}");

        save(role, "changeAgentConfig", "调整成员配置", "调整 Agent 的配置参数以优化工作表现",
            "team_management", false, null, ++priority,
            "{\"agentId\":\"string|required\",\"configKey\":\"string|required\",\"configValue\":\"string|required\"}");

        save(role, "optimizeAgentRole", "优化成员能力", "根据项目进展优化 Agent 的能力组合和工作方式",
            "team_management", false, null, ++priority,
            "{\"agentId\":\"string|required\",\"optimizationType\":\"enum:capabilities,workflow,focus|required\",\"reason\":\"string\"}");

        save(role, "addAgentCapability", "追加Agent能力", "为项目下的 Agent 追加已有的系统能力，扩展其工作范围",
            "team_management", false, null, ++priority,
            "{\"agentId\":\"string|required\",\"capabilityName\":\"string|required\",\"reason\":\"string\"}");

        save(role, "createAgentCapability", "新建Agent能力", "为项目下的 Agent 创建全新的自定义能力（Prompt类型），支持特定场景的定制化需求",
            "team_management", false, null, ++priority,
            "{\"agentId\":\"string|required\",\"capabilityName\":\"string|required\",\"displayName\":\"string|required\",\"description\":\"string|required\",\"promptTemplate\":\"string|required\",\"paramSchema\":\"string\"}");

        save(role, "evaluateAgentPerformance", "评估成员绩效", "定期评估 Agent 的工作表现，提供反馈和改进建议",
            "team_management", false, null, ++priority,
            "{\"agentId\":\"string|required\",\"evaluationCriteria\":\"string\",\"period\":\"string\"}");

        // ===== 2. 项目管理类 =====

        save(role, "setProjectGoal", "设定项目目标", "设定或调整项目的整体目标和方向",
            "project_management", false, null, ++priority,
            "{\"goal\":\"string|required\",\"goalType\":\"enum:GAME_DEVELOPMENT,BUG_FIX,FEATURE,REFACTOR,CUSTOM\",\"deadline\":\"string\"}");

        save(role, "decomposeGoal", "分解项目目标", "将项目目标分解为可执行的里程碑和任务",
            "project_management", false, null, ++priority,
            "{\"milestones\":\"string\",\"assignRoles\":\"string\"}");

        save(role, "updateMilestone", "更新里程碑", "更新项目里程碑的状态、进度或调整计划",
            "project_management", false, null, ++priority,
            "{\"milestoneId\":\"string|required\",\"status\":\"enum:PENDING,IN_PROGRESS,COMPLETED,BLOCKED\",\"progress\":\"number\"}");

        save(role, "addTaskToMilestone", "添加任务", "为里程碑添加新任务（用于干预新增需求）",
            "project_management", false, null, ++priority,
            "{\"milestoneId\":\"string|required\",\"title\":\"string|required\",\"description\":\"string\",\"assignedRole\":\"string\",\"priority\":\"enum:HIGH,MEDIUM,LOW\"}");

        save(role, "updateTask", "更新任务", "修改现有任务的标题、描述、负责角色或优先级",
            "project_management", false, null, ++priority,
            "{\"milestoneId\":\"string|required\",\"taskId\":\"string|required\",\"title\":\"string\",\"description\":\"string\",\"assignedRole\":\"string\",\"priority\":\"enum:HIGH,MEDIUM,LOW\"}");

        save(role, "adjustProjectPlan", "调整项目计划", "根据实际情况调整项目计划、资源分配或优先级",
            "project_management", false, null, ++priority,
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

        // ===== 13. 游戏设计评审类 =====

        save(role, "reviewGameDesign", "审查游戏设计", "审查游戏设计方案的完整性、可行性和创新性，提供专业反馈",
            "design_review", false, null, ++priority,
            "{\"designType\":\"enum:system,level,numerical,ui,narrative|required\",\"designName\":\"string|required\",\"focus\":\"string\"}");

        save(role, "analyzeGameplayLoop", "分析核心循环", "分析游戏核心循环的合理性，评估留存和心流设计",
            "design_review", false, null, ++priority,
            "{\"loopType\":\"enum:short,mid,long|required\",\"description\":\"string\"}");

        // ===== 14. 团队健康监控类 =====

        save(role, "monitorTeamHealth", "监控团队健康", "分析团队成员工作负载、空闲率和协作效率，识别潜在问题",
            "team_management", false, null, ++priority,
            "{\"metrics\":\"enum:workload,idle_rate,blockers,collaboration\",\"period\":\"string\"}");

        save(role, "conductRetrospective", "回顾分析", "对已完成的里程碑进行回顾分析，总结经验教训和改进点",
            "team_management", false, null, ++priority,
            "{\"milestoneId\":\"string|required\",\"scope\":\"enum:process,technical,team\"}");

        // ===== 15. 工作流优化类 =====

        save(role, "optimizeWorkflow", "优化工作流", "分析当前工作流瓶颈，提出优化方案并实施改进",
            "project_management", false, null, ++priority,
            "{\"workflowType\":\"enum:development,review,deployment,testing|required\",\"optimizationGoal\":\"string\"}");

        save(role, "defineQualityGate", "定义质量门禁", "为项目阶段定义质量门禁标准，确保交付质量",
            "quality_control", false, null, ++priority,
            "{\"gateType\":\"enum:code_review,testing,security,performance|required\",\"criteria\":\"string|required\"}");

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

        // API 设计与文档能力
        save(role, "generateApiDoc", "生成 API 文档", "根据代码自动生成 API 接口文档",
            "code", false, null, 6,
            "{\"targetPath\":\"string\",\"format\":\"enum:openapi,markdown\"}");

        save(role, "planDatabaseMigration", "数据库迁移规划", "规划数据库迁移方案，评估风险和回滚策略",
            "code", false, null, 7,
            "{\"migrationType\":\"enum:schema_change,data_migration,index_optimize|required\",\"description\":\"string\"}");

        save(role, "scanDependencies", "依赖安全扫描", "扫描项目依赖的安全漏洞和版本风险",
            "code", false, null, 8,
            "{\"scope\":\"enum:all,new,changed\",\"severity\":\"enum:critical,high,medium,low\"}");

        save(role, "optimizeQuery", "SQL 查询优化", "分析和优化 SQL 查询性能",
            "code", false, null, 9,
            "{\"targetPath\":\"string\",\"queryType\":\"enum:select,join,aggregate\"}");

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

        save(role, "decomposeTasks", "分解任务", "将里程碑分解为具体可执行的任务，包含输入/输出/验收标准",
            "project_management", false, null, 6,
            "{\"milestoneId\":\"string|required\",\"milestoneTitle\":\"string|required\",\"milestoneDescription\":\"string\",\"goal\":\"string\"}");

        // 竞品分析与设计增强能力
        save(role, "analyzeCompetitorDesign", "竞品设计分析", "分析同类游戏的系统设计优劣，提取可借鉴的设计模式",
            "task", false, null, 7,
            "{\"competitors\":\"string|required\",\"focus\":\"enum:core_loop,progression,monetization,social,ui_ux\"}");

        save(role, "mapUserJourney", "用户旅程映射", "设计完整的用户旅程，从新手到核心玩家的成长路径",
            "task", false, null, 8,
            "{\"persona\":\"string|required\",\"journeyScope\":\"enum:onboarding,core_loop,endgame,monetization\"}");

        save(role, "mapSystemDependencies", "系统依赖映射", "分析各系统间的依赖关系，识别耦合点和潜在冲突",
            "task", false, null, 9,
            "{\"scope\":\"enum:all,core,combat,economy,social\"}");

        save(role, "analyzePlayerPsychology", "玩家心理分析", "运用心理学原理分析玩家动机、留存驱动和付费心理",
            "task", false, null, 10,
            "{\"playerType\":\"enum:achiever,explorer,socializer,killer\",\"analysisFocus\":\"enum:retention,monetization,engagement\"}");

        log.info("SystemPlanner capabilities initialized: 10");
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

        // 数值分析增强能力
        save(role, "runSimulation", "数值仿真", "模拟玩家行为，预测数值走势和极端情况",
            "task", false, null, 5,
            "{\"simulationType\":\"enum:combat,economy,progression,gacha\",\"playerCount\":\"number\",\"duration\":\"string\"}");

        save(role, "analyzeStatistics", "统计分析", "对数值设计进行统计分析，检测异常值和分布问题",
            "task", false, null, 6,
            "{\"dataType\":\"enum:damage,health,economy,drop_rate\",\"analysisType\":\"enum:distribution,outlier,trend\"}");

        save(role, "visualizeEconomyFlow", "经济流可视化", "生成游戏经济系统的产出/消耗流向图",
            "task", false, null, 7,
            "{\"currencyType\":\"string\",\"scope\":\"enum:daily,weekly,monthly,full\"}");

        save(role, "optimizeGachaProbability", "抽卡概率优化", "优化抽卡/扭蛋概率，平衡玩家体验和商业目标",
            "task", false, null, 8,
            "{\"gachaType\":\"string|required\",\"pity\":\"boolean\",\"targetRevenue\":\"string\"}");

        save(role, "analyzeProgressionCurve", "进阶曲线分析", "分析玩家进阶曲线，检测卡点和断崖",
            "task", false, null, 9,
            "{\"progressionType\":\"enum:level,skill,equipment,castle\",\"maxLevel\":\"number\"}");

        log.info("NumericalPlanner capabilities initialized: 9");
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

        // Git 管理增强能力
        save(role, "manageBranchStrategy", "分支策略管理", "管理 Git 分支策略，确保代码流清晰有序",
            "task", false, null, 6,
            "{\"action\":\"enum:create,merge,delete,list\",\"branchType\":\"enum:feature,release,hotfix\",\"branchName\":\"string\"}");

        save(role, "generateReleaseNotes", "生成发布说明", "根据提交记录自动生成版本发布说明",
            "task", false, null, 7,
            "{\"fromTag\":\"string\",\"toTag\":\"string\",\"format\":\"enum:markdown,html\"}");

        save(role, "trackCodeOwnership", "代码所有权追踪", "追踪代码文件的主要贡献者和审查者",
            "task", false, null, 8,
            "{\"targetPath\":\"string\",\"depth\":\"enum:file,directory,module\"}");

        save(role, "resolveMergeConflict", "合并冲突指导", "提供合并冲突的解决建议和最佳实践",
            "task", false, null, 9,
            "{\"conflictFiles\":\"string\",\"strategy\":\"enum:accept_theirs,accept_ours,manual\"}");

        log.info("GitCommit capabilities initialized: 9");
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

        // UI 增强能力
        save(role, "manageDesignSystem", "设计系统管理", "管理和维护统一的设计系统，包括组件库、颜色、字体等",
            "task", false, null, 5,
            "{\"action\":\"enum:create,update,audit,export\",\"scope\":\"enum:colors,typography,spacing,components\"}");

        save(role, "manageAnimationLibrary", "动画库管理", "管理和优化 UI 动画库，确保流畅的交互体验",
            "task", false, null, 6,
            "{\"action\":\"enum:create,optimize,audit\",\"animationType\":\"enum:transition,micro_interaction,loading,feedback\"}");

        save(role, "auditAccessibility", "无障碍审计", "审计 UI 的无障碍支持，确保可访问性标准",
            "task", false, null, 7,
            "{\"standard\":\"enum:wcag_a,wcag_aa,wcag_aaa\",\"scope\":\"enum:full,page,component\"}");

        save(role, "designThemeSystem", "主题系统设计", "设计和实现多主题切换系统",
            "task", false, null, 8,
            "{\"themeType\":\"enum:dark_mode,seasonal,custom\",\"scope\":\"enum:colors,components,full\"}");

        log.info("UiDev capabilities initialized: 8");
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

        // 测试增强能力
        save(role, "runPerformanceTest", "性能测试", "执行游戏性能测试，包括帧率、内存、加载时间等",
            "testing", false, null, 30,
            "{\"testType\":\"enum:fps,memory,load_time,network\",\"target\":\"string\",\"duration\":\"string\"}");

        save(role, "runSecurityTest", "安全测试", "执行安全测试，检测常见漏洞",
            "testing", false, null, 31,
            "{\"scanType\":\"enum:sql_injection,xss,csrf,auth,api\",\"target\":\"string\"}");

        save(role, "generateTestData", "测试数据生成", "生成测试数据，覆盖边界条件和异常场景",
            "testing", false, null, 32,
            "{\"dataType\":\"enum:user,game_data,transaction,edge_case\",\"count\":\"number\"}");

        save(role, "manageTestEnvironment", "测试环境管理", "管理测试环境的配置和状态",
            "testing", false, null, 33,
            "{\"action\":\"enum:create,reset,configure,status\",\"environment\":\"enum:dev,staging,qa\"}");

        save(role, "manageRegressionSuite", "回归测试套件", "管理和执行回归测试套件",
            "testing", false, null, 34,
            "{\"action\":\"enum:create,run,update,report\",\"scope\":\"enum:smoke,full,critical\"}");

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

        // 安全增强能力
        save(role, "buildThreatModel", "威胁建模", "对系统进行威胁建模，识别攻击面和潜在威胁",
            "task", false, null, 8,
            "{\"scope\":\"enum:full,api,auth,data,infrastructure\",\"methodology\":\"enum:stride,kill_chain,owasp\"}");

        save(role, "auditCompliance", "合规审计", "审计系统是否符合安全合规要求",
            "task", false, null, 9,
            "{\"standard\":\"enum:gdpr,pci_dss,iso27001,owasp_top10\",\"scope\":\"string\"}");

        save(role, "createSecurityTraining", "安全培训材料", "创建安全培训材料和最佳实践指南",
            "task", false, null, 10,
            "{\"topic\":\"enum:secure_coding,phishing,data_protection,incident_response\",\"audience\":\"enum:developer,qa,all\"}");

        save(role, "handleSecurityIncident", "安全事件响应", "处理安全事件，制定响应和恢复方案",
            "communication", true, "SECURITY_INCIDENT", 11,
            "{\"incidentType\":\"enum:data_breach,unauthorized_access,malware,ddos\",\"severity\":\"enum:critical,high,medium,low|required\",\"description\":\"string|required\"}");

        log.info("SecurityExpert capabilities initialized: 11");
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

        // 数据分析增强能力
        save(role, "buildPredictiveModel", "预测建模", "构建用户流失、付费等预测模型",
            "task", false, null, 9,
            "{\"modelType\":\"enum:churn_prediction,ltv_prediction,segmentation\",\"targetMetric\":\"string\"}");

        save(role, "manageDataQuality", "数据质量管理", "监控数据质量，识别数据异常和缺失",
            "task", false, null, 10,
            "{\"dataSource\":\"string\",\"checkType\":\"enum:completeness,accuracy,consistency,freshness\"}");

        save(role, "designFunnelOptimization", "漏斗优化", "分析转化漏斗，识别流失环节并提出优化方案",
            "task", false, null, 11,
            "{\"funnelType\":\"enum:onboarding,purchase,engagement,retention\",\"stage\":\"string\"}");

        log.info("DataAnalyst capabilities initialized: 11");
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

        // 技术美术增强能力
        save(role, "optimizeAssetPipeline", "资产管线优化", "优化美术资产的导入、处理和加载流程",
            "task", false, null, 7,
            "{\"assetType\":\"enum:texture,mesh,animation,material\",\"optimization\":\"enum:size,load_time,quality\"}");

        save(role, "generateLOD", "LOD 自动生成", "为 3D 模型自动生成多级细节层次",
            "task", false, null, 8,
            "{\"targetPath\":\"string\",\"lodLevels\":\"number\",\"distanceThresholds\":\"string\"}");

        save(role, "createBatchProcessor", "批处理工具", "创建批量处理工具，自动化重复性美术工作",
            "task", false, null, 9,
            "{\"processorType\":\"enum:texture_resize,format_convert,naming_convention,atlas_packing\",\"inputPath\":\"string\"}");

        log.info("TechArtist capabilities initialized: 9");
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

        // 产品经理增强能力
        save(role, "mapUserStory", "用户故事映射", "创建完整的用户故事地图，梳理功能全景",
            "task", false, null, 8,
            "{\"persona\":\"string|required\",\"scope\":\"enum:full_feature,release,mvp\"}");

        save(role, "planRoadmap", "路线图规划", "制定产品路线图，规划版本节奏",
            "task", false, null, 9,
            "{\"timeHorizon\":\"enum:quarter,half_year,year\",\"focus\":\"enum:features,tech_debt,growth\"}");

        save(role, "defineOKR", "OKR 制定", "制定产品 OKR，对齐团队目标",
            "task", false, null, 10,
            "{\"objective\":\"string|required\",\"keyResults\":\"string\",\"period\":\"enum:quarter,month\"}");

        log.info("ProductManager capabilities initialized: 10");
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

        // 本地化增强能力
        save(role, "manageTranslationMemory", "翻译记忆库", "管理翻译记忆库，提高翻译效率和一致性",
            "task", false, null, 7,
            "{\"action\":\"enum:add,query,export,import\",\"sourceLang\":\"string\",\"targetLang\":\"string\"}");

        save(role, "auditCulturalSensitivity", "文化敏感审核", "审核内容的文化敏感性，避免文化冲突",
            "task", false, null, 8,
            "{\"targetRegion\":\"string\",\"contentType\":\"enum:text,image,audio,ui\",\"riskLevel\":\"enum:high,medium,low\"}");

        log.info("Localization capabilities initialized: 8");
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

        // AI 工程增强能力
        save(role, "evaluateModel", "模型评估", "评估 AI 模型的性能和效果",
            "task", false, null, 7,
            "{\"modelType\":\"enum:behavior_tree,pathfinding,dialogue,recommendation\",\"metrics\":\"enum:accuracy,speed,memory\"}");

        save(role, "optimizeInference", "推理优化", "优化 AI 推理性能，降低计算开销",
            "task", false, null, 8,
            "{\"target\":\"enum:behavior_tree,pathfinding,dialogue\",\"technique\":\"enum:caching,quantization,pruning,batching\"}");

        save(role, "auditAISafety", "AI 安全审计", "审计 AI 系统的安全性和公平性",
            "task", false, null, 9,
            "{\"scope\":\"enum:behavior,dialogue,recommendation\",\"concern\":\"enum:safety,fairness,transparency\"}");

        log.info("AiEngineer capabilities initialized: 9");
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

        // 性能工程增强能力
        save(role, "setupDistributedTracing", "分布式追踪", "配置分布式追踪系统，定位跨服务性能问题",
            "task", false, null, 8,
            "{\"system\":\"enum:jaeger,zipkin,skywalking\",\"scope\":\"enum:full,critical_path\"}");

        save(role, "planCapacity", "容量规划", "根据业务增长预测进行容量规划",
            "task", false, null, 9,
            "{\"metric\":\"enum:qps,storage,bandwidth,concurrent_users\",\"timeHorizon\":\"enum:month,quarter,year\"}");

        save(role, "defineSLA", "SLA 制定", "制定服务级别协议和性能基准",
            "task", false, null, 10,
            "{\"service\":\"string|required\",\"availability\":\"string\",\"responseTime\":\"string\"}");

        log.info("PerformanceEngineer capabilities initialized: 10");
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

        // 音频增强能力
        save(role, "designAdaptiveMusic", "自适应音乐系统", "设计根据游戏状态动态切换的音乐系统",
            "task", false, null, 7,
            "{\"scene\":\"string\",\"states\":\"string\",\"transitionType\":\"enum:crossfade,layer,switch\"}");

        save(role, "designSpatialAudio", "空间音频设计", "设计 3D 空间音频方案，增强沉浸感",
            "task", false, null, 8,
            "{\"environment\":\"enum:indoor,outdoor,underwater,urban\",\"features\":\"enum:reverb,occlusion,doppler\"}");

        save(role, "designAudioEventSystem", "音频事件系统", "设计事件驱动的音频触发系统",
            "task", false, null, 9,
            "{\"eventSource\":\"enum:gameplay,ui,environment,character\",\"priority\":\"enum:high,medium,low\"}");

        log.info("AudioDev capabilities initialized: 9");
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

        // 剧情增强能力
        save(role, "designBranchingNarrative", "分支叙事设计", "设计多分支叙事结构，支持玩家选择影响剧情",
            "task", false, null, 8,
            "{\"branchPoint\":\"string\",\"options\":\"string\",\"consequences\":\"string\"}");

        save(role, "mapCharacterRelationships", "角色关系图", "设计和维护角色关系网络图",
            "task", false, null, 9,
            "{\"scope\":\"enum:main_cast,full_world,faction\",\"relationshipType\":\"enum:ally,rival,romance,family\"}");

        save(role, "checkDialogueConsistency", "对话一致性检查", "检查对话内容与角色设定的一致性",
            "task", false, null, 10,
            "{\"character\":\"string\",\"targetPath\":\"string\",\"checkType\":\"enum:personality,lore,tone\"}");

        log.info("NarrativePlanner capabilities initialized: 10");
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

        // 关卡设计增强能力
        save(role, "generateProceduralLevel", "程序化关卡生成", "使用算法生成关卡布局和内容",
            "task", false, null, 8,
            "{\"algorithm\":\"enum:random_walk,cellular_automata,grammar,constraint\",\"constraints\":\"string\"}");

        save(role, "analyzeHeatmap", "热力图分析", "分析玩家在关卡中的行为热力图，优化布局",
            "task", false, null, 9,
            "{\"levelId\":\"string\",\"metric\":\"enum:movement,death,engagement,time_spent\"}");

        save(role, "automateLevelTest", "关卡自动化测试", "自动化测试关卡的可通关性和平衡性",
            "task", false, null, 10,
            "{\"levelId\":\"string\",\"testType\":\"enum:completion,performance,bug_detection\",\"iterations\":\"number\"}");

        log.info("LevelDesign capabilities initialized: 10");
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

        // 运维增强能力
        save(role, "setupIaC", "基础设施即代码", "使用 Terraform/Pulumi 管理基础设施",
            "task", false, null, 9,
            "{\"tool\":\"enum:terraform,pulumi,cloudformation\",\"scope\":\"enum:compute,storage,network,full\"}");

        save(role, "setupObservability", "可观测性工程", "配置日志、指标、追踪三位一体的可观测性系统",
            "task", false, null, 10,
            "{\"components\":\"enum:logs,metrics,traces\",\"tool\":\"enum:prometheus,grafana,elk,jaeger\"}");

        save(role, "handleIncident", "事故响应", "处理生产环境事故，执行应急响应流程",
            "communication", true, "INCIDENT", 11,
            "{\"severity\":\"enum:critical,high,medium,low|required\",\"description\":\"string|required\",\"affectedService\":\"string\"}");

        save(role, "defineSLO", "SLO 制定", "制定服务级别目标和错误预算",
            "task", false, null, 12,
            "{\"service\":\"string\",\"availability\":\"string\",\"latency\":\"string\",\"errorBudget\":\"string\"}");

        log.info("DevOps capabilities initialized: 12");
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

    // ===== LSP 代码理解能力集 =====
    /**
     * 初始化 LSP 代码理解能力
     *
     * 基于 Language Server Protocol 的代码理解能力，让 Agent 具备类似 IDE 的代码分析能力：
     * 1. 跳转定义 - 查找符号的定义位置
     * 2. 查找引用 - 查找符号在项目中的所有引用
     * 3. 代码诊断 - 分析代码中的错误、警告和建议
     * 4. 符号信息 - 获取符号的详细信息（类型、参数、文档）
     * 5. 工作区符号搜索 - 在项目中搜索符号
     *
     * 适用角色：代码密集型角色（server-dev, ui-dev, tester, verifier, security-expert,
     *           performance-engineer, ai-engineer, tech-artist, devops）
     *
     * 执行方式：prompt 类型，通过 AI 分析代码实现 LSP 级别的代码理解
     */
    private void initLspCodeUnderstandingCapabilities() {
        // 适用的代码密集型角色
        String[] codeRoles = {
            "server-dev", "client-dev", "ui-dev", "tester", "verifier",
            "security-expert", "performance-engineer", "ai-engineer",
            "tech-artist", "devops"
        };

        for (String role : codeRoles) {
            // 跳转定义：查找符号（类、方法、变量）的定义位置
            save(role, "lspGoToDefinition", "跳转定义", "查找符号（类、方法、变量、接口）的定义位置，返回定义所在的文件路径、行号和上下文代码",
                "code_intelligence", false, null, 80,
                "{\"symbol\":\"string|required\",\"scope\":\"string\"}",
                "prompt",
                "请在项目中查找符号 \"{symbol}\" 的定义位置。\n\n" +
                "分析步骤：\n" +
                "1. 在项目目录中搜索该符号的定义（class、function、method、interface、type、const 等）\n" +
                "2. 返回定义所在的文件路径、行号\n" +
                "3. 展示定义的完整代码上下文（包含前后 5 行）\n" +
                "4. 说明符号的类型、参数、返回值等信息\n\n" +
                "如果找到多个同名定义，全部列出并说明区别。");

            // 查找引用：查找符号在项目中的所有引用
            save(role, "lspFindReferences", "查找引用", "查找符号在项目中的所有引用位置，包括调用、赋值、导入等",
                "code_intelligence", false, null, 81,
                "{\"symbol\":\"string|required\",\"includeDeclaration\":\"boolean\"}",
                "prompt",
                "请在项目中查找符号 \"{symbol}\" 的所有引用位置。\n\n" +
                "分析步骤：\n" +
                "1. 搜索项目中所有引用该符号的文件\n" +
                "2. 对每个引用，说明引用类型（调用、赋值、导入、继承、实现等）\n" +
                "3. 展示引用的代码上下文\n" +
                "4. 统计引用总数和分布情况\n\n" +
                "如果 includeDeclaration 为 true，同时包含定义位置。");

            // 代码诊断：分析代码中的问题
            save(role, "lspCodeDiagnostics", "代码诊断", "分析代码文件中的错误、警告和改进建议，类似 IDE 的实时诊断",
                "code_intelligence", false, null, 82,
                "{\"targetPath\":\"string|required\",\"severity\":\"enum:error,warning,info,all\"}",
                "prompt",
                "请对文件 \"{targetPath}\" 进行代码诊断分析。\n\n" +
                "诊断维度：\n" +
                "1. **语法错误**：语法不正确、缺少分号/括号等\n" +
                "2. **类型错误**：类型不匹配、未定义的变量/方法\n" +
                "3. **逻辑警告**：可能的空指针、未处理的异常、死代码\n" +
                "4. **代码规范**：命名规范、代码风格、最佳实践\n" +
                "5. **性能建议**：潜在的性能问题、优化建议\n" +
                "6. **安全风险**：注入风险、敏感信息泄露等\n\n" +
                "输出格式：\n" +
                "- 严重程度：ERROR/WARNING/INFO\n" +
                "- 位置：文件:行号\n" +
                "- 描述：问题说明\n" +
                "- 建议：修复方案");

            // 符号信息：获取符号的详细信息
            save(role, "lspSymbolInfo", "符号信息", "获取符号的详细信息，包括类型签名、文档注释、所属模块等",
                "code_intelligence", false, null, 83,
                "{\"symbol\":\"string|required\",\"context\":\"string\"}",
                "prompt",
                "请获取符号 \"{symbol}\" 的详细信息。\n\n" +
                "分析内容：\n" +
                "1. **类型签名**：完整的类型声明（参数类型、返回类型）\n" +
                "2. **文档注释**：Javadoc/注释内容\n" +
                "3. **所属模块**：所在文件、包/命名空间、类\n" +
                "4. **可见性**：public/private/protected\n" +
                "5. **使用示例**：从项目中提取该符号的典型使用方式\n" +
                "6. **相关符号**：同类型的相关符号推荐\n\n" +
                "如果 context 非空，在该上下文中分析符号的具体含义。");

            // 工作区符号搜索
            save(role, "lspWorkspaceSymbols", "符号搜索", "在项目中搜索符号，支持模糊匹配，返回匹配的符号列表",
                "code_intelligence", false, null, 84,
                "{\"query\":\"string|required\",\"symbolKind\":\"enum:class,method,function,variable,interface,enum,all\"}",
                "prompt",
                "请在项目中搜索匹配 \"{query}\" 的符号。\n\n" +
                "搜索策略：\n" +
                "1. 精确匹配：符号名完全匹配\n" +
                "2. 前缀匹配：符号名以 query 开头\n" +
                "3. 模糊匹配：符号名包含 query（不区分大小写）\n" +
                "4. 如果 symbolKind 非 all，只搜索指定类型的符号\n\n" +
                "输出格式（每条）：\n" +
                "- 符号名称\n" +
                "- 类型（class/method/function/variable/interface/enum）\n" +
                "- 所在文件:行号\n" +
                "- 简短描述（如有注释）\n\n" +
                "按匹配度排序，最多返回 20 个结果。");
        }

        log.info("LSP Code Understanding capabilities initialized for {} roles", codeRoles.length);
    }

    // ===== Verifier 能力集 =====
    /**
     * 初始化验证官能力集
     *
     * 验证官的核心能力：
     * 1. 结构验证 - 项目目录结构完整性检查
     * 2. 代码质量 - 代码规范、最佳实践检查
     * 3. 设计审查 - 游戏设计文档审查
     * 4. 里程碑验证 - 里程碑交付物验收
     * 5. 约束规则 - 项目约束规则检查
     * 6. 报告生成 - 验证报告生成和保存
     */
    private void initVerifierCapabilities() {
        String role = "verifier";
        int priority = 0;

        // ===== 结构验证 =====
        save(role, "verifyProjectStructure", "项目结构验证", "验证项目目录结构完整性",
            "verification", false, null, ++priority,
            "{\"projectDir\":\"string|required\"}");

        save(role, "verifyFileIntegrity", "文件完整性检查", "检查关键文件是否存在且格式正确",
            "verification", false, null, ++priority,
            "{\"projectDir\":\"string|required\",\"requiredFiles\":\"array\"}");

        // ===== 代码质量 =====
        save(role, "verifyCodeQuality", "代码质量检查", "检查代码规范、文件大小、空文件等",
            "quality", false, null, ++priority,
            "{\"projectDir\":\"string|required\",\"maxFileSizeKB\":\"number|default=1024\"}");

        save(role, "checkSensitiveFiles", "敏感文件检查", "检查项目中是否包含敏感信息文件",
            "security", false, null, ++priority,
            "{\"projectDir\":\"string|required\"}");

        // ===== 设计审查 =====
        save(role, "reviewGameDesign", "游戏设计审查", "审查游戏设计文档的合理性和完整性",
            "review", false, null, ++priority,
            "{\"projectGoal\":\"string|required\",\"designDocuments\":\"string\"}");

        save(role, "reviewDesignCompleteness", "设计完整性检查", "检查设计文档是否覆盖核心功能",
            "review", false, null, ++priority,
            "{\"projectDir\":\"string|required\"}");

        // ===== 里程碑验证 =====
        save(role, "verifyMilestone", "里程碑验证", "验证里程碑交付物是否满足验收标准",
            "verification", false, null, ++priority,
            "{\"milestoneId\":\"string|required\",\"criteria\":\"array\"}");

        save(role, "verifyMilestoneDeliverables", "交付物验证", "验证里程碑的交付物完整性",
            "verification", false, null, ++priority,
            "{\"milestoneId\":\"string|required\",\"projectDir\":\"string|required\"}");

        // ===== 约束规则 =====
        save(role, "checkProjectConstraints", "项目约束检查", "检查项目是否遵守预定义的约束规则",
            "compliance", false, null, ++priority,
            "{\"projectDir\":\"string|required\",\"rules\":\"array\"}");

        save(role, "checkDirectorySize", "目录大小检查", "检查项目目录大小是否在限制范围内",
            "compliance", false, null, ++priority,
            "{\"projectDir\":\"string|required\",\"maxSizeMB\":\"number|default=1024\"}");

        // ===== 报告生成 =====
        save(role, "generateVerificationReport", "生成验证报告", "生成完整的验证报告并保存到知识库",
            "reporting", false, null, ++priority,
            "{\"includeDetails\":\"boolean|default=true\"}");

        save(role, "getVerificationSummary", "获取验证摘要", "获取当前验证状态的摘要信息",
            "reporting", false, null, ++priority,
            "{}");

        // ===== 事件处理 =====
        save(role, "onCodeCommitted", "代码提交事件处理", "收到代码提交事件后触发增量验证",
            "event", false, null, ++priority,
            "{\"commitInfo\":\"object\"}");

        save(role, "onMilestoneChanged", "里程碑变更事件处理", "收到里程碑状态变更事件后触发验证",
            "event", false, null, ++priority,
            "{\"milestoneId\":\"string|required\",\"newStatus\":\"string|required\"}");

        log.info("Verifier capabilities initialized: {} capabilities", priority);
    }

    // ===== Agent 工具能力 =====
    /**
     * 初始化 Agent 工具能力集
     *
     * 这些能力对应 /api/agent-tools/* 端点，提供给 Agent 作为可调用的工具：
     * 1. 快照管理 - 创建、查看、恢复、撤销文件快照
     * 2. 会话分叉 - 创建、查看、合并、丢弃会话分叉
     * 3. 子代理 - 创建、查看、终止子代理
     * 4. 工具权限 - 管理 Agent 的工具调用权限规则
     */
    private void initAgentToolsCapabilities() {
        // 快照管理：所有开发类角色可用
        String[] devRoles = {"producer", "server-dev", "client-dev", "ui-dev", "tester",
            "security-expert", "performance-engineer", "ai-engineer", "tech-artist", "devops"};

        for (String role : devRoles) {
            save(role, "createSnapshot", "创建快照", "对指定文件创建快照，保存当前状态以便后续恢复",
                "snapshot", false, null, 90,
                "{\"projectId\":\"string|required\",\"agentId\":\"string|required\",\"filePaths\":\"array|required\",\"description\":\"string\"}");

            save(role, "listSnapshots", "查看快照", "查看指定项目和 Agent 的所有快照列表",
                "snapshot", false, null, 91,
                "{\"projectId\":\"string|required\",\"agentId\":\"string|required\"}");

            save(role, "restoreSnapshot", "恢复快照", "恢复指定快照，将文件还原到快照时的状态",
                "snapshot", false, null, 92,
                "{\"projectId\":\"string|required\",\"agentId\":\"string|required\",\"snapshotId\":\"string|required\"}");

            save(role, "undoSnapshot", "撤销快照恢复", "撤销最近一次快照恢复操作",
                "snapshot", false, null, 93,
                "{\"projectId\":\"string|required\",\"agentId\":\"string|required\"}");
        }

        // 会话分叉：制作人和高级角色可用
        String[] leadRoles = {"producer", "server-dev", "system-planner", "security-expert", "ai-engineer"};

        for (String role : leadRoles) {
            save(role, "createSessionFork", "创建会话分叉", "从当前会话分叉出一个探索性分支，用于尝试不同方案",
                "session", false, null, 95,
                "{\"projectId\":\"string|required\",\"agentId\":\"string|required\",\"description\":\"string\"}");

            save(role, "listSessionForks", "查看会话分叉", "查看指定 Agent 的所有会话分叉",
                "session", false, null, 96,
                "{\"parentAgentId\":\"string|required\"}");

            save(role, "mergeSessionFork", "合并会话分叉", "将分叉的上下文合并回主会话",
                "session", false, null, 97,
                "{\"forkId\":\"string|required\",\"strategy\":\"string\"}");

            save(role, "discardSessionFork", "丢弃会话分叉", "丢弃不需要的会话分叉",
                "session", false, null, 98,
                "{\"forkId\":\"string|required\"}");
        }

        // 子代理：制作人和高级角色可用
        for (String role : leadRoles) {
            save(role, "spawnSubAgent", "创建子代理", "创建子代理来并行处理子任务",
                "subagent", false, null, 100,
                "{\"parentAgentId\":\"string|required\",\"projectId\":\"string|required\",\"task\":\"string|required\",\"role\":\"string\"}");

            save(role, "listSubAgents", "查看子代理", "查看指定父代理的所有子代理",
                "subagent", false, null, 101,
                "{\"parentAgentId\":\"string|required\"}");

            save(role, "terminateSubAgent", "终止子代理", "终止运行中的子代理",
                "subagent", false, null, 102,
                "{\"subAgentId\":\"string|required\"}");
        }

        // 工具权限：仅制作人可用
        save("producer", "setToolPermissions", "设置工具权限", "设置 Agent 的工具调用权限规则（允许/拒绝特定工具和命令模式）",
            "security", false, null, 105,
            "{\"agentId\":\"string|required\",\"permissions\":\"array|required\"}");

        log.info("Agent tools capabilities initialized for {} dev roles (snapshot), {} lead roles (fork/subagent), producer (tool-perm)",
            devRoles.length, leadRoles.length);
    }

    // ===== 工具方法 =====

    private void save(String agentRole, String capabilityName, String displayName,
                      String description, String category, boolean requiresApproval,
                      String approvalType, int priority, String paramSchema) {
        save(agentRole, capabilityName, displayName, description, category,
            requiresApproval, approvalType, priority, paramSchema, "java", null);
    }

    /**
     * 保存能力定义（支持 executionType 和 promptTemplate）
     *
     * @param agentRole      Agent 角色
     * @param capabilityName 能力名称
     * @param displayName    显示名称
     * @param description    描述
     * @param category       分类
     * @param requiresApproval 是否需要审批
     * @param approvalType   审批类型
     * @param priority       优先级
     * @param paramSchema    参数 Schema
     * @param executionType  执行类型：java/prompt/message
     * @param promptTemplate Prompt 模板（executionType=prompt 时使用）
     */
    private void save(String agentRole, String capabilityName, String displayName,
                      String description, String category, boolean requiresApproval,
                      String approvalType, int priority, String paramSchema,
                      String executionType, String promptTemplate) {
        AgentCapability cap = new AgentCapability(agentRole, capabilityName, displayName, description, category);
        cap.setRequiresApproval(requiresApproval);
        cap.setApprovalType(approvalType);
        cap.setPriority(priority);
        cap.setParamSchema(paramSchema);
        cap.setExecutionType(executionType != null ? executionType : "java");
        cap.setPromptTemplate(promptTemplate);
        cap.setEnabled(true);
        capabilityRepository.save(cap);
    }

    /**
     * 检查并补充新增的能力定义
     * 在已有能力数据时调用，只添加缺失的能力，不覆盖已有记录
     */
    private void ensureNewCapabilities() {
        String[] roles = {"producer", "server-dev", "system-planner", "numerical-planner", "git-commit", "ui-dev",
            "tester", "security-expert", "data-analyst", "tech-artist", "product-manager",
            "localization", "ai-engineer", "performance-engineer", "audio-dev",
            "narrative-planner", "level-design", "devops"};

        int added = 0;
        for (String role : roles) {
            if (capabilityRepository.findByCapabilityNameAndAgentRoleAndProjectIdIsNull("addTaskToMilestone", role).isEmpty()) {
                save(role, "addTaskToMilestone", "添加任务", "为里程碑添加新任务（用于干预新增需求）",
                    "project_management", false, null, 100,
                    "{\"milestoneId\":\"string|required\",\"title\":\"string|required\",\"description\":\"string\",\"assignedRole\":\"string\",\"priority\":\"enum:HIGH,MEDIUM,LOW\"}");
                added++;
            }
            if (capabilityRepository.findByCapabilityNameAndAgentRoleAndProjectIdIsNull("updateTask", role).isEmpty()) {
                save(role, "updateTask", "更新任务", "修改现有任务的标题、描述、负责角色或优先级",
                    "project_management", false, null, 101,
                    "{\"milestoneId\":\"string|required\",\"taskId\":\"string|required\",\"title\":\"string\",\"description\":\"string\",\"assignedRole\":\"string\",\"priority\":\"enum:HIGH,MEDIUM,LOW\"}");
                added++;
            }
        }

        // MCP 工具调用能力（所有角色通用）
        for (String role : roles) {
            if (capabilityRepository.findByCapabilityNameAndAgentRoleAndProjectIdIsNull("callMcpTool", role).isEmpty()) {
                save(role, "callMcpTool", "调用MCP工具", "调用外部 MCP Server 提供的工具（如图片生成、音频生成等）",
                    "mcp", false, null, 210,
                    "{\"toolName\":\"string|required\",\"arguments\":\"string\"}");
                added++;
            }
        }

        // 资源生成能力（只注册给资源类角色和制作人）
        String[] resourceRoles = {"audio-dev", "tech-artist", "ui-dev", "producer"};
        for (String role : resourceRoles) {
            if (capabilityRepository.findByCapabilityNameAndAgentRoleAndProjectIdIsNull("generateMusic", role).isEmpty()) {
                save(role, "generateMusic", "生成音乐", "使用 AI 生成游戏背景音乐",
                    "resource_generation", false, null, 200,
                    "{\"prompt\":\"string|required\",\"style\":\"string\",\"instrumental\":\"boolean\",\"title\":\"string\"}");
                added++;
            }
            if (capabilityRepository.findByCapabilityNameAndAgentRoleAndProjectIdIsNull("generateSoundEffect", role).isEmpty()) {
                save(role, "generateSoundEffect", "生成音效", "使用 AI 生成游戏音效",
                    "resource_generation", false, null, 201,
                    "{\"prompt\":\"string|required\",\"sfxType\":\"enum:ui,combat,environment,character\",\"duration\":\"string\"}");
                added++;
            }
            if (capabilityRepository.findByCapabilityNameAndAgentRoleAndProjectIdIsNull("generateSprite", role).isEmpty()) {
                save(role, "generateSprite", "生成精灵图", "使用 AI 生成 2D 精灵图/贴图",
                    "resource_generation", false, null, 202,
                    "{\"prompt\":\"string|required\",\"size\":\"string\",\"style\":\"string\"}");
                added++;
            }
            if (capabilityRepository.findByCapabilityNameAndAgentRoleAndProjectIdIsNull("generateUIAsset", role).isEmpty()) {
                save(role, "generateUIAsset", "生成UI素材", "使用 AI 生成 UI 界面素材",
                    "resource_generation", false, null, 203,
                    "{\"prompt\":\"string|required\",\"assetType\":\"enum:icon,button,background,panel\",\"size\":\"string\"}");
                added++;
            }
        }
        if (added > 0) {
            log.info("补充了 {} 个新增能力定义", added);
        }
    }
}
