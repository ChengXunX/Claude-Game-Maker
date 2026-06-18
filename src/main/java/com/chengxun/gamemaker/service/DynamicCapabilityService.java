package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.web.entity.AgentCapability;
import com.chengxun.gamemaker.web.repository.AgentCapabilityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态能力服务
 * 管理角色能力模板，支持动态添加和热加载
 *
 * 核心功能：
 * - 维护角色能力模板库
 * - 为新招聘的角色自动创建能力
 * - 支持运行时添加自定义能力
 * - 热加载能力变更
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class DynamicCapabilityService {

    private static final Logger log = LoggerFactory.getLogger(DynamicCapabilityService.class);

    private final AgentCapabilityRepository capabilityRepository;
    private final CapabilityRegistry capabilityRegistry;

    /**
     * 角色能力模板缓存
     * key: role
     * value: 该角色的能力模板列表
     */
    private final Map<String, List<CapabilityTemplate>> roleTemplates = new ConcurrentHashMap<>();

    public DynamicCapabilityService(AgentCapabilityRepository capabilityRepository,
                                     CapabilityRegistry capabilityRegistry) {
        this.capabilityRepository = capabilityRepository;
        this.capabilityRegistry = capabilityRegistry;
        initRoleTemplates();
    }

    /**
     * 能力模板定义
     */
    public static class CapabilityTemplate {
        private final String capabilityName;
        private final String displayName;
        private final String description;
        private final String category;
        private final boolean requiresApproval;
        private final String approvalType;
        private final int priority;
        private final String paramSchema;
        private final String executionType;
        private final String promptTemplate;

        public CapabilityTemplate(String capabilityName, String displayName, String description,
                                   String category, int priority, String paramSchema) {
            this(capabilityName, displayName, description, category, false, null, priority, paramSchema, "prompt", null);
        }

        public CapabilityTemplate(String capabilityName, String displayName, String description,
                                   String category, boolean requiresApproval, String approvalType,
                                   int priority, String paramSchema, String executionType, String promptTemplate) {
            this.capabilityName = capabilityName;
            this.displayName = displayName;
            this.description = description;
            this.category = category;
            this.requiresApproval = requiresApproval;
            this.approvalType = approvalType;
            this.priority = priority;
            this.paramSchema = paramSchema;
            this.executionType = executionType;
            this.promptTemplate = promptTemplate;
        }

        // Getters
        public String getCapabilityName() { return capabilityName; }
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public String getCategory() { return category; }
        public boolean isRequiresApproval() { return requiresApproval; }
        public String getApprovalType() { return approvalType; }
        public int getPriority() { return priority; }
        public String getParamSchema() { return paramSchema; }
        public String getExecutionType() { return executionType; }
        public String getPromptTemplate() { return promptTemplate; }
    }

    // ===== 角色模板初始化 =====

    private void initRoleTemplates() {
        // 通用能力模板（所有角色都有）
        List<CapabilityTemplate> commonTemplates = List.of(
            new CapabilityTemplate("sendMessage", "发送消息", "向其他 Agent 发送消息", "communication", 20,
                "{\"targetAgent\":\"string|required\",\"content\":\"string|required\"}"),
            new CapabilityTemplate("saveKnowledge", "保存知识", "将知识保存到记忆系统", "monitoring", 21,
                "{\"key\":\"string|required\",\"value\":\"string|required\"}"),
            new CapabilityTemplate("compactContext", "压缩上下文", "压缩当前对话上下文", "monitoring", 22, "{}"),
            new CapabilityTemplate("reportStatus", "汇报状态", "向制作人汇报当前状态", "communication", 23,
                "{\"status\":\"string|required\",\"details\":\"string\"}")
        );

        // 为每个角色添加通用能力
        String[] allRoles = {"producer", "server-dev", "client-dev", "ui-dev", "system-planner",
            "numerical-planner", "git-commit", "tester", "security-expert", "data-analyst",
            "tech-artist", "product-manager", "localization", "ai-engineer",
            "performance-engineer", "audio-dev", "narrative-planner", "level-design", "devops"};

        for (String role : allRoles) {
            roleTemplates.computeIfAbsent(role, k -> new ArrayList<>()).addAll(commonTemplates);
        }

        // 初始化各角色的专属能力模板
        initProducerTemplates();
        initServerDevTemplates();
        initSystemPlannerTemplates();
        initNumericalPlannerTemplates();
        initGitCommitTemplates();
        initUiDevTemplates();
        initSecurityExpertTemplates();
        initDataAnalystTemplates();
        initTechArtistTemplates();
        initProductManagerTemplates();
        initLocalizationTemplates();
        initAiEngineerTemplates();
        initPerformanceEngineerTemplates();
        initAudioDevTemplates();
        initNarrativePlannerTemplates();
        initLevelDesignTemplates();
        initDevOpsTemplates();
        initTesterTemplates();

        log.info("Role capability templates initialized: {} roles", roleTemplates.size());
    }

    private void initProducerTemplates() {
        List<CapabilityTemplate> templates = roleTemplates.get("producer");
        // 资源调度能力（制作人可以协调资源生成）
        templates.add(new CapabilityTemplate("generateMusic", "协调生成音乐", "协调 audio-dev 生成背景音乐", "resource_coordination", 1,
            "{\"prompt\":\"string|required\",\"targetAgent\":\"string\",\"priority\":\"enum:HIGH,MEDIUM,LOW\"}"));
        templates.add(new CapabilityTemplate("generateSprite", "协调生成图片", "协调 tech-artist/ui-dev 生成美术资源", "resource_coordination", 2,
            "{\"prompt\":\"string|required\",\"targetAgent\":\"string\",\"priority\":\"enum:HIGH,MEDIUM,LOW\"}"));
        // 项目管理能力
        templates.add(new CapabilityTemplate("reviewGameDesign", "审查游戏设计", "审查游戏设计方案的完整性和可行性", "design_review", 3,
            "{\"designType\":\"enum:system,level,numerical,ui,narrative|required\",\"designName\":\"string|required\"}"));
        templates.add(new CapabilityTemplate("monitorTeamHealth", "监控团队健康", "分析团队成员工作负载和协作效率", "team_management", 4,
            "{\"metrics\":\"enum:workload,idle_rate,blockers,collaboration\"}"));
        templates.add(new CapabilityTemplate("conductRetrospective", "回顾分析", "对已完成的里程碑进行回顾分析", "team_management", 3,
            "{\"milestoneId\":\"string|required\",\"scope\":\"enum:process,technical,team\"}"));
        templates.add(new CapabilityTemplate("optimizeWorkflow", "优化工作流", "分析当前工作流瓶颈，提出优化方案", "project_management", 4,
            "{\"workflowType\":\"enum:development,review,deployment,testing|required\"}"));
        templates.add(new CapabilityTemplate("defineQualityGate", "定义质量门禁", "为项目阶段定义质量门禁标准", "quality_control", 5,
            "{\"gateType\":\"enum:code_review,testing,security,performance|required\",\"criteria\":\"string|required\"}"));
        templates.add(new CapabilityTemplate("addAgentCapability", "追加Agent能力", "为项目下的 Agent 追加已有的系统能力", "team_management", 6,
            "{\"agentId\":\"string|required\",\"capabilityName\":\"string|required\",\"reason\":\"string\"}"));
        templates.add(new CapabilityTemplate("createAgentCapability", "新建Agent能力", "为项目下的 Agent 创建全新的自定义能力", "team_management", 7,
            "{\"agentId\":\"string|required\",\"capabilityName\":\"string|required\",\"displayName\":\"string|required\",\"description\":\"string|required\",\"promptTemplate\":\"string|required\"}"));

        // 版本迭代类能力
        templates.add(new CapabilityTemplate("evaluateVersion", "评估当前版本", "评估当前版本的质量和完成度，给出1-10分的评分", "version_management", 30,
            "{\"projectId\":\"string\"}"));
        templates.add(new CapabilityTemplate("planNextVersion", "规划下一版本", "根据当前版本评估结果，规划下一版本的目标和里程碑", "version_management", 31,
            "{\"projectId\":\"string\",\"currentScore\":\"number\"}"));
        templates.add(new CapabilityTemplate("upgradeVersion", "执行版本升级", "创建新版本历史，重置里程碑状态，开始下一版本迭代", "version_management", 32,
            "{\"projectId\":\"string\",\"newVersion\":\"string|required\",\"newGoal\":\"string\",\"reason\":\"string\"}"));
        templates.add(new CapabilityTemplate("checkVersionIteration", "检查版本迭代", "检查当前版本是否完成，是否需要进入下一版本迭代", "version_management", 33,
            "{\"projectId\":\"string\"}"));
        templates.add(new CapabilityTemplate("stopAllProjectAgents", "停止项目Agent", "目标完成时停止项目内所有Agent", "version_management", 34,
            "{\"projectId\":\"string\",\"reason\":\"string\"}"));
    }

    private void initServerDevTemplates() {
        List<CapabilityTemplate> templates = roleTemplates.get("server-dev");
        templates.add(new CapabilityTemplate("generateApiDoc", "生成 API 文档", "根据代码自动生成 API 接口文档", "code", 6,
            "{\"targetPath\":\"string\",\"format\":\"enum:openapi,markdown\"}"));
        templates.add(new CapabilityTemplate("planDatabaseMigration", "数据库迁移规划", "规划数据库迁移方案", "code", 7,
            "{\"migrationType\":\"enum:schema_change,data_migration,index_optimize|required\"}"));
        templates.add(new CapabilityTemplate("scanDependencies", "依赖安全扫描", "扫描项目依赖的安全漏洞", "code", 8,
            "{\"scope\":\"enum:all,new,changed\",\"severity\":\"enum:critical,high,medium,low\"}"));
        templates.add(new CapabilityTemplate("optimizeQuery", "SQL 查询优化", "分析和优化 SQL 查询性能", "code", 9,
            "{\"targetPath\":\"string\",\"queryType\":\"enum:select,join,aggregate\"}"));
    }

    private void initSystemPlannerTemplates() {
        List<CapabilityTemplate> templates = roleTemplates.get("system-planner");
        templates.add(new CapabilityTemplate("analyzeCompetitorDesign", "竞品设计分析", "分析同类游戏的系统设计优劣", "task", 7,
            "{\"competitors\":\"string|required\",\"focus\":\"enum:core_loop,progression,monetization,social,ui_ux\"}"));
        templates.add(new CapabilityTemplate("mapUserJourney", "用户旅程映射", "设计完整的用户旅程", "task", 8,
            "{\"persona\":\"string|required\",\"journeyScope\":\"enum:onboarding,core_loop,endgame,monetization\"}"));
        templates.add(new CapabilityTemplate("mapSystemDependencies", "系统依赖映射", "分析各系统间的依赖关系", "task", 9,
            "{\"scope\":\"enum:all,core,combat,economy,social\"}"));
        templates.add(new CapabilityTemplate("analyzePlayerPsychology", "玩家心理分析", "运用心理学原理分析玩家动机", "task", 10,
            "{\"playerType\":\"enum:achiever,explorer,socializer,killer\"}"));
    }

    private void initNumericalPlannerTemplates() {
        List<CapabilityTemplate> templates = roleTemplates.get("numerical-planner");
        templates.add(new CapabilityTemplate("runSimulation", "数值仿真", "模拟玩家行为，预测数值走势", "task", 5,
            "{\"simulationType\":\"enum:combat,economy,progression,gacha\",\"playerCount\":\"number\"}"));
        templates.add(new CapabilityTemplate("analyzeStatistics", "统计分析", "对数值设计进行统计分析", "task", 6,
            "{\"dataType\":\"enum:damage,health,economy,drop_rate\",\"analysisType\":\"enum:distribution,outlier,trend\"}"));
        templates.add(new CapabilityTemplate("visualizeEconomyFlow", "经济流可视化", "生成游戏经济系统的产出/消耗流向图", "task", 7,
            "{\"currencyType\":\"string\",\"scope\":\"enum:daily,weekly,monthly,full\"}"));
        templates.add(new CapabilityTemplate("optimizeGachaProbability", "抽卡概率优化", "优化抽卡/扭蛋概率", "task", 8,
            "{\"gachaType\":\"string|required\",\"pity\":\"boolean\"}"));
    }

    private void initGitCommitTemplates() {
        List<CapabilityTemplate> templates = roleTemplates.get("git-commit");
        templates.add(new CapabilityTemplate("manageBranchStrategy", "分支策略管理", "管理 Git 分支策略", "task", 6,
            "{\"action\":\"enum:create,merge,delete,list\",\"branchType\":\"enum:feature,release,hotfix\"}"));
        templates.add(new CapabilityTemplate("generateReleaseNotes", "生成发布说明", "根据提交记录自动生成版本发布说明", "task", 7,
            "{\"fromTag\":\"string\",\"toTag\":\"string\",\"format\":\"enum:markdown,html\"}"));
        templates.add(new CapabilityTemplate("trackCodeOwnership", "代码所有权追踪", "追踪代码文件的主要贡献者", "task", 8,
            "{\"targetPath\":\"string\",\"depth\":\"enum:file,directory,module\"}"));
    }

    private void initUiDevTemplates() {
        List<CapabilityTemplate> templates = roleTemplates.get("ui-dev");
        // 资源生成能力（优先级最高）
        templates.add(new CapabilityTemplate("generateSprite", "生成图标素材", "使用 AI 生成游戏图标和装饰素材", "resource_generation", 1,
            "{\"prompt\":\"string|required\",\"size\":\"string\",\"style\":\"string\"}"));
        templates.add(new CapabilityTemplate("generateUIAsset", "生成UI素材", "使用 AI 生成按钮、面板、背景等 UI 素材", "resource_generation", 2,
            "{\"prompt\":\"string|required\",\"assetType\":\"enum:icon,button,background,panel\",\"size\":\"string\"}"));
        // UI 开发能力
        templates.add(new CapabilityTemplate("manageDesignSystem", "设计系统管理", "管理和维护统一的设计系统", "task", 5,
            "{\"action\":\"enum:create,update,audit,export\",\"scope\":\"enum:colors,typography,spacing,components\"}"));
        templates.add(new CapabilityTemplate("manageAnimationLibrary", "动画库管理", "管理和优化 UI 动画库", "task", 6,
            "{\"action\":\"enum:create,optimize,audit\",\"animationType\":\"enum:transition,micro_interaction,loading,feedback\"}"));
        templates.add(new CapabilityTemplate("auditAccessibility", "无障碍审计", "审计 UI 的无障碍支持", "task", 7,
            "{\"standard\":\"enum:wcag_a,wcag_aa,wcag_aaa\",\"scope\":\"enum:full,page,component\"}"));
        templates.add(new CapabilityTemplate("designThemeSystem", "主题系统设计", "设计和实现多主题切换系统", "task", 8,
            "{\"themeType\":\"enum:dark_mode,seasonal,custom\",\"scope\":\"enum:colors,components,full\"}"));
    }

    private void initSecurityExpertTemplates() {
        List<CapabilityTemplate> templates = roleTemplates.get("security-expert");
        templates.add(new CapabilityTemplate("auditCode", "代码安全审计", "对代码进行安全审计", "task", 1,
            "{\"targetPath\":\"string\",\"scope\":\"enum:full,api,auth,data\"}"));
        templates.add(new CapabilityTemplate("scanVulnerability", "漏洞扫描", "扫描安全漏洞", "task", 2,
            "{\"scanType\":\"enum:sql_injection,xss,csrf,auth\",\"target\":\"string\"}"));
        templates.add(new CapabilityTemplate("createSecurityReport", "安全报告", "生成安全审计报告", "task", 3,
            "{\"scope\":\"string\",\"includeRecommendations\":\"boolean\"}"));
        templates.add(new CapabilityTemplate("alertSecurityRisk", "安全预警", "发现安全风险时预警", "communication", 4,
            "{\"riskType\":\"string|required\",\"severity\":\"enum:critical,high,medium,low|required\",\"description\":\"string|required\"}"));
        templates.add(new CapabilityTemplate("buildThreatModel", "威胁建模", "对系统进行威胁建模", "task", 5,
            "{\"scope\":\"enum:full,api,auth,data,infrastructure\",\"methodology\":\"enum:stride,kill_chain,owasp\"}"));
        templates.add(new CapabilityTemplate("auditCompliance", "合规审计", "审计系统是否符合安全合规要求", "task", 6,
            "{\"standard\":\"enum:gdpr,pci_dss,iso27001,owasp_top10\"}"));
        templates.add(new CapabilityTemplate("handleSecurityIncident", "安全事件响应", "处理安全事件", "communication", 7,
            "{\"incidentType\":\"enum:data_breach,unauthorized_access,malware,ddos\",\"severity\":\"enum:critical,high,medium,low|required\"}"));
    }

    private void initDataAnalystTemplates() {
        List<CapabilityTemplate> templates = roleTemplates.get("data-analyst");
        templates.add(new CapabilityTemplate("analyzeRetention", "留存分析", "分析用户留存数据", "task", 1,
            "{\"period\":\"enum:day1,day7,day30\",\"segment\":\"string\"}"));
        templates.add(new CapabilityTemplate("analyzeMonetization", "付费分析", "分析用户付费行为", "task", 2,
            "{\"metrics\":\"enum:arpu,arppu,ltv,conversion\",\"period\":\"string\"}"));
        templates.add(new CapabilityTemplate("analyzeUserBehavior", "行为分析", "分析用户行为路径", "task", 3,
            "{\"metrics\":\"enum:retention,engagement,funnel,cohort\",\"period\":\"string\"}"));
        templates.add(new CapabilityTemplate("designABTest", "设计 AB 测试", "设计 AB 测试方案", "task", 4,
            "{\"hypothesis\":\"string|required\",\"metrics\":\"string\",\"sampleSize\":\"number\"}"));
        templates.add(new CapabilityTemplate("generateDataReport", "数据报告", "生成数据分析报告", "task", 5,
            "{\"reportType\":\"enum:daily,weekly,monthly,custom|required\",\"metrics\":\"string\"}"));
        templates.add(new CapabilityTemplate("buildPredictiveModel", "预测建模", "构建用户流失、付费等预测模型", "task", 6,
            "{\"modelType\":\"enum:churn_prediction,ltv_prediction,segmentation\",\"targetMetric\":\"string\"}"));
        templates.add(new CapabilityTemplate("designFunnelOptimization", "漏斗优化", "分析转化漏斗，识别流失环节", "task", 7,
            "{\"funnelType\":\"enum:onboarding,purchase,engagement,retention\"}"));
    }

    private void initTechArtistTemplates() {
        List<CapabilityTemplate> templates = roleTemplates.get("tech-artist");
        // 资源生成能力（优先级最高）
        templates.add(new CapabilityTemplate("generateSprite", "生成精灵图", "使用 AI 生成 2D 精灵图/贴图", "resource_generation", 1,
            "{\"prompt\":\"string|required\",\"size\":\"string\",\"style\":\"string\"}"));
        templates.add(new CapabilityTemplate("generateUIAsset", "生成UI素材", "使用 AI 生成 UI 界面素材", "resource_generation", 2,
            "{\"prompt\":\"string|required\",\"assetType\":\"enum:icon,button,background,panel\",\"size\":\"string\"}"));
        // 技术美术能力
        templates.add(new CapabilityTemplate("createShader", "创建 Shader", "创建或修改 Shader", "task", 3,
            "{\"shaderType\":\"enum:surface,post_process,particle,ui|required\",\"requirements\":\"string|required\"}"));
        templates.add(new CapabilityTemplate("optimizeRendering", "优化渲染", "优化渲染性能", "task", 4,
            "{\"targetPath\":\"string\",\"optimizationType\":\"enum:drawcall,shader,memory,overdraw\"}"));
        templates.add(new CapabilityTemplate("createArtTool", "美术工具", "创建自动化工具", "task", 5,
            "{\"toolType\":\"enum:batch_convert,validation,preview,export|required\",\"requirements\":\"string\"}"));
        templates.add(new CapabilityTemplate("optimizeAssetPipeline", "资产管线优化", "优化美术资产的导入和加载流程", "task", 6,
            "{\"assetType\":\"enum:texture,mesh,animation,material\",\"optimization\":\"enum:size,load_time,quality\"}"));
        templates.add(new CapabilityTemplate("generateLOD", "LOD 自动生成", "为 3D 模型自动生成多级细节层次", "task", 7,
            "{\"targetPath\":\"string\",\"lodLevels\":\"number\"}"));
    }

    private void initProductManagerTemplates() {
        List<CapabilityTemplate> templates = roleTemplates.get("product-manager");
        templates.add(new CapabilityTemplate("analyzeRequirements", "需求分析", "分析产品需求", "task", 1,
            "{\"requirement\":\"string|required\",\"stakeholders\":\"string\"}"));
        templates.add(new CapabilityTemplate("createPRD", "需求文档", "创建 PRD 文档", "task", 2,
            "{\"featureName\":\"string|required\",\"userStory\":\"string\",\"acceptanceCriteria\":\"string\"}"));
        templates.add(new CapabilityTemplate("prioritizeFeatures", "优先级排序", "评估特性优先级", "task", 3,
            "{\"features\":\"string|required\",\"criteria\":\"enum:impact,effort,risk,value\"}"));
        templates.add(new CapabilityTemplate("designUserJourney", "用户旅程", "设计用户旅程", "task", 4,
            "{\"scenario\":\"string|required\",\"persona\":\"string\"}"));
        templates.add(new CapabilityTemplate("mapUserStory", "用户故事映射", "创建完整的用户故事地图", "task", 5,
            "{\"persona\":\"string|required\",\"scope\":\"enum:full_feature,release,mvp\"}"));
        templates.add(new CapabilityTemplate("planRoadmap", "路线图规划", "制定产品路线图", "task", 6,
            "{\"timeHorizon\":\"enum:quarter,half_year,year\",\"focus\":\"enum:features,tech_debt,growth\"}"));
    }

    private void initLocalizationTemplates() {
        List<CapabilityTemplate> templates = roleTemplates.get("localization");
        templates.add(new CapabilityTemplate("translateText", "翻译文本", "翻译文本到目标语言", "task", 1,
            "{\"text\":\"string|required\",\"targetLang\":\"string|required\",\"context\":\"string\"}"));
        templates.add(new CapabilityTemplate("reviewTranslation", "审查翻译", "审查翻译质量", "task", 2,
            "{\"file\":\"string\",\"targetLang\":\"string\",\"focus\":\"enum:accuracy,fluency,consistency\"}"));
        templates.add(new CapabilityTemplate("checkLocalization", "本地化检查", "检查适配问题", "task", 3,
            "{\"targetLang\":\"string\",\"checkType\":\"enum:text_length,encoding,cultural,format\"}"));
        templates.add(new CapabilityTemplate("manageTerminology", "术语管理", "管理术语表", "task", 4,
            "{\"action\":\"enum:add,update,query,export\",\"term\":\"string\",\"definition\":\"string\"}"));
        templates.add(new CapabilityTemplate("manageTranslationMemory", "翻译记忆库", "管理翻译记忆库", "task", 5,
            "{\"action\":\"enum:add,query,export,import\",\"sourceLang\":\"string\",\"targetLang\":\"string\"}"));
        templates.add(new CapabilityTemplate("auditCulturalSensitivity", "文化敏感审核", "审核内容的文化敏感性", "task", 6,
            "{\"targetRegion\":\"string\",\"contentType\":\"enum:text,image,audio,ui\"}"));
    }

    private void initAiEngineerTemplates() {
        List<CapabilityTemplate> templates = roleTemplates.get("ai-engineer");
        templates.add(new CapabilityTemplate("createBehaviorTree", "行为树", "创建 NPC 行为树", "task", 1,
            "{\"npcType\":\"string|required\",\"behaviors\":\"string|required\"}"));
        templates.add(new CapabilityTemplate("implementPathfinding", "寻路算法", "实现寻路算法", "task", 2,
            "{\"algorithm\":\"enum:astar,navmesh,flowfield|required\",\"requirements\":\"string\"}"));
        templates.add(new CapabilityTemplate("designDialogueSystem", "对话系统", "设计对话系统", "task", 3,
            "{\"dialogueType\":\"enum:branching,ai_generated,hybrid\",\"features\":\"string\"}"));
        templates.add(new CapabilityTemplate("optimizeAI", "优化 AI", "优化 AI 性能", "task", 4,
            "{\"target\":\"enum:behavior_tree,pathfinding,dialogue\",\"optimization\":\"enum:speed,memory,quality\"}"));
        templates.add(new CapabilityTemplate("evaluateModel", "模型评估", "评估 AI 模型的性能和效果", "task", 5,
            "{\"modelType\":\"enum:behavior_tree,pathfinding,dialogue,recommendation\",\"metrics\":\"enum:accuracy,speed,memory\"}"));
        templates.add(new CapabilityTemplate("optimizeInference", "推理优化", "优化 AI 推理性能", "task", 6,
            "{\"target\":\"enum:behavior_tree,pathfinding,dialogue\",\"technique\":\"enum:caching,quantization,pruning,batching\"}"));
    }

    private void initPerformanceEngineerTemplates() {
        List<CapabilityTemplate> templates = roleTemplates.get("performance-engineer");
        templates.add(new CapabilityTemplate("profilePerformance", "性能分析", "系统性能分析", "task", 1,
            "{\"target\":\"enum:cpu,gpu,memory,network\",\"scope\":\"enum:full,specific\"}"));
        templates.add(new CapabilityTemplate("identifyBottleneck", "瓶颈定位", "定位性能瓶颈", "task", 2,
            "{\"symptom\":\"string|required\",\"component\":\"enum:client,server,database,api\"}"));
        templates.add(new CapabilityTemplate("optimizeCode", "代码优化", "优化代码性能", "task", 3,
            "{\"targetPath\":\"string\",\"optimizationType\":\"enum:algorithm,caching,async,batch\"}"));
        templates.add(new CapabilityTemplate("runLoadTest", "压力测试", "执行压力测试", "task", 4,
            "{\"concurrentUsers\":\"number\",\"duration\":\"string\",\"scenario\":\"string\"}"));
        templates.add(new CapabilityTemplate("setupDistributedTracing", "分布式追踪", "配置分布式追踪系统", "task", 5,
            "{\"system\":\"enum:jaeger,zipkin,skywalking\",\"scope\":\"enum:full,critical_path\"}"));
        templates.add(new CapabilityTemplate("planCapacity", "容量规划", "根据业务增长预测进行容量规划", "task", 6,
            "{\"metric\":\"enum:qps,storage,bandwidth,concurrent_users\",\"timeHorizon\":\"enum:month,quarter,year\"}"));
    }

    private void initAudioDevTemplates() {
        List<CapabilityTemplate> templates = roleTemplates.get("audio-dev");
        // 资源生成能力（优先级最高）
        templates.add(new CapabilityTemplate("generateMusic", "生成音乐", "使用 AI 生成背景音乐", "resource_generation", 1,
            "{\"prompt\":\"string|required\",\"style\":\"string\",\"instrumental\":\"boolean\",\"title\":\"string\"}"));
        templates.add(new CapabilityTemplate("generateSoundEffect", "生成音效", "使用 AI 生成游戏音效", "resource_generation", 2,
            "{\"prompt\":\"string|required\",\"sfxType\":\"enum:ui,combat,environment,character\",\"duration\":\"string\"}"));
        // 设计能力
        templates.add(new CapabilityTemplate("designSoundEffect", "音效设计", "设计游戏音效规格", "task", 3,
            "{\"sfxType\":\"enum:ui,combat,environment,character|required\",\"requirements\":\"string\"}"));
        templates.add(new CapabilityTemplate("planMusic", "音乐规划", "规划背景音乐方案", "task", 4,
            "{\"scene\":\"string\",\"mood\":\"string\",\"style\":\"string\"}"));
        templates.add(new CapabilityTemplate("designAudioSystem", "音频系统", "设计音频架构", "task", 5,
            "{\"systemType\":\"enum:mixer,3d_audio,adaptive,events\",\"requirements\":\"string\"}"));
        templates.add(new CapabilityTemplate("designAdaptiveMusic", "自适应音乐系统", "设计根据游戏状态动态切换的音乐系统", "task", 6,
            "{\"scene\":\"string\",\"states\":\"string\",\"transitionType\":\"enum:crossfade,layer,switch\"}"));
        templates.add(new CapabilityTemplate("designSpatialAudio", "空间音频设计", "设计 3D 空间音频方案", "task", 7,
            "{\"environment\":\"enum:indoor,outdoor,underwater,urban\",\"features\":\"enum:reverb,occlusion,doppler\"}"));
    }

    private void initNarrativePlannerTemplates() {
        List<CapabilityTemplate> templates = roleTemplates.get("narrative-planner");
        templates.add(new CapabilityTemplate("buildWorldview", "世界观", "构建游戏世界观", "task", 1,
            "{\"scope\":\"enum:world,region,faction\",\"depth\":\"enum:brief,detailed,comprehensive\"}"));
        templates.add(new CapabilityTemplate("designCharacter", "角色设计", "设计游戏角色", "task", 2,
            "{\"roleType\":\"enum:protagonist,antagonist,npc,side\",\"importance\":\"enum:main,secondary,minor\"}"));
        templates.add(new CapabilityTemplate("writeStoryline", "剧情编写", "编写游戏剧情", "task", 3,
            "{\"storyType\":\"enum:main_quest,side_quest,hidden_event\",\"chapter\":\"string\"}"));
        templates.add(new CapabilityTemplate("designDialogue", "对话设计", "设计角色对话", "task", 4,
            "{\"character\":\"string\",\"scene\":\"string\",\"branching\":\"boolean\"}"));
        templates.add(new CapabilityTemplate("designBranchingNarrative", "分支叙事设计", "设计多分支叙事结构", "task", 5,
            "{\"branchPoint\":\"string\",\"options\":\"string\",\"consequences\":\"string\"}"));
        templates.add(new CapabilityTemplate("mapCharacterRelationships", "角色关系图", "设计和维护角色关系网络图", "task", 6,
            "{\"scope\":\"enum:main_cast,full_world,faction\",\"relationshipType\":\"enum:ally,rival,romance,family\"}"));
    }

    private void initLevelDesignTemplates() {
        List<CapabilityTemplate> templates = roleTemplates.get("level-design");
        templates.add(new CapabilityTemplate("designLevel", "关卡设计", "设计游戏关卡", "task", 1,
            "{\"levelType\":\"enum:tutorial,challenge,boss,hidden\",\"difficulty\":\"enum:easy,medium,hard\"}"));
        templates.add(new CapabilityTemplate("layoutMap", "地图布局", "设计地图布局", "task", 2,
            "{\"mapSize\":\"enum:small,medium,large\",\"theme\":\"string\"}"));
        templates.add(new CapabilityTemplate("configureEnemies", "敌人配置", "配置关卡敌人", "task", 3,
            "{\"enemyTypes\":\"string\",\"difficulty\":\"enum:easy,medium,hard\"}"));
        templates.add(new CapabilityTemplate("designDifficultyCurve", "难度曲线", "设计难度曲线", "task", 4,
            "{\"totalLevels\":\"number\",\"peakLevel\":\"number\"}"));
        templates.add(new CapabilityTemplate("generateProceduralLevel", "程序化关卡生成", "使用算法生成关卡布局和内容", "task", 5,
            "{\"algorithm\":\"enum:random_walk,cellular_automata,grammar,constraint\",\"constraints\":\"string\"}"));
        templates.add(new CapabilityTemplate("analyzeHeatmap", "热力图分析", "分析玩家在关卡中的行为热力图", "task", 6,
            "{\"levelId\":\"string\",\"metric\":\"enum:movement,death,engagement,time_spent\"}"));
    }

    private void initDevOpsTemplates() {
        List<CapabilityTemplate> templates = roleTemplates.get("devops");
        templates.add(new CapabilityTemplate("setupCICD", "CI/CD 配置", "配置持续集成流水线", "task", 1,
            "{\"platform\":\"enum:jenkins,github_actions,gitlab_ci\",\"stages\":\"string\"}"));
        templates.add(new CapabilityTemplate("deployService", "部署服务", "部署服务到环境", "task",
            true, "DEPLOY", 2, "{\"environment\":\"enum:dev,staging,prod|required\",\"version\":\"string\"}", "java", null));
        templates.add(new CapabilityTemplate("configureMonitoring", "监控配置", "配置系统监控", "task", 3,
            "{\"metrics\":\"string\",\"thresholds\":\"string\",\"channels\":\"string\"}"));
        templates.add(new CapabilityTemplate("manageContainers", "容器管理", "管理 Docker 容器", "task", 4,
            "{\"action\":\"enum:build,deploy,scale,monitor\",\"service\":\"string\"}"));
        templates.add(new CapabilityTemplate("setupIaC", "基础设施即代码", "使用 Terraform/Pulumi 管理基础设施", "task", 5,
            "{\"tool\":\"enum:terraform,pulumi,cloudformation\",\"scope\":\"enum:compute,storage,network,full\"}"));
        templates.add(new CapabilityTemplate("setupObservability", "可观测性工程", "配置日志、指标、追踪三位一体的可观测性系统", "task", 6,
            "{\"components\":\"enum:logs,metrics,traces\",\"tool\":\"enum:prometheus,grafana,elk,jaeger\"}"));
    }

    private void initTesterTemplates() {
        List<CapabilityTemplate> templates = roleTemplates.get("tester");
        templates.add(new CapabilityTemplate("createTestPlan", "测试计划", "制定测试计划", "task", 1,
            "{\"testScope\":\"string|required\",\"testTypes\":\"enum:functional,performance,compatibility,security\"}"));
        templates.add(new CapabilityTemplate("executeTest", "执行测试", "执行测试用例", "task", 2,
            "{\"testType\":\"enum:functional,performance,regression|required\",\"scope\":\"string\"}"));
        templates.add(new CapabilityTemplate("reportBug", "报告缺陷", "创建缺陷报告", "task", 3,
            "{\"title\":\"string|required\",\"severity\":\"enum:critical,high,medium,low|required\",\"steps\":\"string|required\"}"));
        templates.add(new CapabilityTemplate("manageBugList", "缺陷管理", "管理 Bug 列表", "task", 4,
            "{\"action\":\"enum:list,assign,prioritize,close|required\",\"bugId\":\"string\"}"));
        templates.add(new CapabilityTemplate("runPerformanceTest", "性能测试", "执行游戏性能测试", "task", 5,
            "{\"testType\":\"enum:fps,memory,load_time,network\",\"target\":\"string\",\"duration\":\"string\"}"));
        templates.add(new CapabilityTemplate("generateTestData", "测试数据生成", "生成测试数据", "task", 6,
            "{\"dataType\":\"enum:user,game_data,transaction,edge_case\",\"count\":\"number\"}"));
        templates.add(new CapabilityTemplate("manageRegressionSuite", "回归测试套件", "管理和执行回归测试套件", "task", 7,
            "{\"action\":\"enum:create,run,update,report\",\"scope\":\"enum:smoke,full,critical\"}"));
    }

    // ===== 核心功能 =====

    /**
     * 为新招聘的角色创建能力
     * 根据角色类型从模板创建能力定义
     *
     * @param role      角色标识
     * @param projectId 项目 ID（可选）
     * @return 创建的能力数量
     */
    public int createCapabilitiesForRole(String role, String projectId) {
        List<CapabilityTemplate> templates = roleTemplates.get(role);
        if (templates == null || templates.isEmpty()) {
            log.warn("No capability templates found for role: {}, using generic templates", role);
            templates = getGenericTemplates();
        }

        int created = 0;
        for (CapabilityTemplate template : templates) {
            // 检查是否已存在
            AgentCapability existing = capabilityRepository
                .findByCapabilityNameAndAgentRoleAndProjectId(
                    template.getCapabilityName(), role, projectId)
                .orElse(null);

            if (existing != null) {
                log.debug("Capability already exists: {} for role {}", template.getCapabilityName(), role);
                continue;
            }

            // 创建能力定义
            AgentCapability cap = new AgentCapability(role, template.getCapabilityName(),
                template.getDisplayName(), template.getDescription(), template.getCategory());
            cap.setRequiresApproval(template.isRequiresApproval());
            cap.setApprovalType(template.getApprovalType());
            cap.setPriority(template.getPriority());
            cap.setParamSchema(template.getParamSchema());
            cap.setExecutionType(template.getExecutionType());
            cap.setPromptTemplate(template.getPromptTemplate());
            cap.setEnabled(true);
            cap.setProjectId(projectId);

            capabilityRepository.save(cap);
            created++;
        }

        // 热加载
        if (created > 0) {
            capabilityRegistry.reloadCapabilities(role);
            log.info("Created {} capabilities for role {} (project: {})", created, role, projectId);
        }

        return created;
    }

    /**
     * 为角色添加自定义能力
     *
     * @param role           角色标识
     * @param capabilityName 能力名称
     * @param displayName    显示名称
     * @param description    描述
     * @param category       分类
     * @param paramSchema    参数 Schema
     * @param projectId      项目 ID（可选）
     * @return 创建的能力
     */
    public AgentCapability addCustomCapability(String role, String capabilityName, String displayName,
                                                String description, String category, String paramSchema,
                                                String projectId) {
        // 检查是否已存在
        AgentCapability existing = capabilityRepository
            .findByCapabilityNameAndAgentRoleAndProjectId(capabilityName, role, projectId)
            .orElse(null);

        if (existing != null) {
            throw new RuntimeException("能力已存在: " + capabilityName + " (角色: " + role + ")");
        }

        AgentCapability cap = new AgentCapability(role, capabilityName, displayName, description, category);
        cap.setParamSchema(paramSchema);
        cap.setEnabled(true);
        cap.setProjectId(projectId);
        cap.setPriority(100); // 自定义能力优先级较低

        AgentCapability saved = capabilityRepository.save(cap);

        // 热加载
        capabilityRegistry.reloadCapabilities(role);

        log.info("Custom capability added: {} for role {} (project: {})", capabilityName, role, projectId);
        return saved;
    }

    /**
     * 为角色添加 Prompt 类型的自定义能力
     * 这种能力通过调用 AI 执行，不需要 Java 代码
     *
     * @param role           角色标识
     * @param capabilityName 能力名称
     * @param displayName    显示名称
     * @param description    描述
     * @param promptTemplate Prompt 模板（支持 {paramName} 占位符）
     * @param paramSchema    参数 Schema
     * @param projectId      项目 ID（可选）
     * @return 创建的能力
     */
    public AgentCapability addPromptCapability(String role, String capabilityName, String displayName,
                                                String description, String promptTemplate,
                                                String paramSchema, String projectId) {
        AgentCapability cap = new AgentCapability(role, capabilityName, displayName, description, "custom");
        cap.setExecutionType("prompt");
        cap.setPromptTemplate(promptTemplate);
        cap.setParamSchema(paramSchema);
        cap.setEnabled(true);
        cap.setProjectId(projectId);
        cap.setPriority(100);

        AgentCapability saved = capabilityRepository.save(cap);

        // 热加载
        capabilityRegistry.reloadCapabilities(role);

        log.info("Prompt capability added: {} for role {}", capabilityName, role);
        return saved;
    }

    /**
     * 为角色添加消息类型的能力
     * 这种能力通过向目标 Agent 发送消息执行
     *
     * @param role             角色标识
     * @param capabilityName   能力名称
     * @param displayName      显示名称
     * @param description      描述
     * @param targetAgentRole  目标 Agent 角色
     * @param promptTemplate   消息模板（可选）
     * @param paramSchema      参数 Schema
     * @param projectId        项目 ID（可选）
     * @return 创建的能力
     */
    public AgentCapability addMessageCapability(String role, String capabilityName, String displayName,
                                                 String description, String targetAgentRole,
                                                 String promptTemplate, String paramSchema, String projectId) {
        AgentCapability cap = new AgentCapability(role, capabilityName, displayName, description, "collaboration");
        cap.setExecutionType("message");
        cap.setTargetAgentRole(targetAgentRole);
        cap.setPromptTemplate(promptTemplate);
        cap.setParamSchema(paramSchema);
        cap.setEnabled(true);
        cap.setProjectId(projectId);
        cap.setPriority(100);

        AgentCapability saved = capabilityRepository.save(cap);

        // 热加载
        capabilityRegistry.reloadCapabilities(role);

        log.info("Message capability added: {} for role {} -> {}", capabilityName, role, targetAgentRole);
        return saved;
    }

    /**
     * 获取角色的能力模板列表
     *
     * @param role 角色标识
     * @return 能力模板列表
     */
    public List<CapabilityTemplate> getRoleTemplates(String role) {
        return roleTemplates.getOrDefault(role, getGenericTemplates());
    }

    /**
     * 获取所有可用的角色模板
     *
     * @return 角色 -> 模板列表
     */
    public Map<String, List<CapabilityTemplate>> getAllRoleTemplates() {
        return new HashMap<>(roleTemplates);
    }

    /**
     * 注册新的角色能力模板
     * 用于运行时动态注册新角色
     *
     * @param role      角色标识
     * @param templates 能力模板列表
     */
    public void registerRoleTemplates(String role, List<CapabilityTemplate> templates) {
        roleTemplates.put(role, new ArrayList<>(templates));
        log.info("Registered {} capability templates for role: {}", templates.size(), role);
    }

    /**
     * 获取通用能力模板（用于未知角色）
     */
    private List<CapabilityTemplate> getGenericTemplates() {
        return List.of(
            new CapabilityTemplate("sendMessage", "发送消息", "向其他 Agent 发送消息", "communication", 20,
                "{\"targetAgent\":\"string|required\",\"content\":\"string|required\"}"),
            new CapabilityTemplate("saveKnowledge", "保存知识", "将知识保存到记忆系统", "monitoring", 21,
                "{\"key\":\"string|required\",\"value\":\"string|required\"}"),
            new CapabilityTemplate("compactContext", "压缩上下文", "压缩当前对话上下文", "monitoring", 22, "{}"),
            new CapabilityTemplate("reportStatus", "汇报状态", "向制作人汇报当前状态", "communication", 23,
                "{\"status\":\"string|required\",\"details\":\"string\"}"),
            new CapabilityTemplate("executeTask", "执行任务", "执行分配的任务", "task", 1,
                "{\"taskDescription\":\"string|required\"}"),
            new CapabilityTemplate("reportProgress", "汇报进度", "汇报任务进度", "communication", 2,
                "{\"taskId\":\"string\",\"progress\":\"string|required\"}")
        );
    }

    /**
     * 批量启用/禁用角色能力
     *
     * @param role           角色标识
     * @param capabilityNames 能力名称列表
     * @param enabled        是否启用
     * @param projectId      项目 ID（可选）
     * @return 操作的数量
     */
    public int batchToggleCapabilities(String role, List<String> capabilityNames, boolean enabled, String projectId) {
        int count = 0;
        for (String capName : capabilityNames) {
            AgentCapability cap = capabilityRepository
                .findByCapabilityNameAndAgentRoleAndProjectId(capName, role, projectId)
                .orElse(null);

            if (cap != null && cap.isEnabled() != enabled) {
                cap.setEnabled(enabled);
                capabilityRepository.save(cap);
                count++;
            }
        }

        if (count > 0) {
            capabilityRegistry.reloadCapabilities(role);
            log.info("Batch {} {} capabilities for role {} (project: {})",
                enabled ? "enabled" : "disabled", count, role, projectId);
        }

        return count;
    }
}
