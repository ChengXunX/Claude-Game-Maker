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
    }

    private void initTechArtistTemplates() {
        List<CapabilityTemplate> templates = roleTemplates.get("tech-artist");
        templates.add(new CapabilityTemplate("createShader", "创建 Shader", "创建或修改 Shader", "task", 1,
            "{\"shaderType\":\"enum:surface,post_process,particle,ui|required\",\"requirements\":\"string|required\"}"));
        templates.add(new CapabilityTemplate("optimizeRendering", "优化渲染", "优化渲染性能", "task", 2,
            "{\"targetPath\":\"string\",\"optimizationType\":\"enum:drawcall,shader,memory,overdraw\"}"));
        templates.add(new CapabilityTemplate("createArtTool", "美术工具", "创建自动化工具", "task", 3,
            "{\"toolType\":\"enum:batch_convert,validation,preview,export|required\",\"requirements\":\"string\"}"));
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
    }

    private void initAudioDevTemplates() {
        List<CapabilityTemplate> templates = roleTemplates.get("audio-dev");
        templates.add(new CapabilityTemplate("designSoundEffect", "音效设计", "设计游戏音效", "task", 1,
            "{\"sfxType\":\"enum:ui,combat,environment,character|required\",\"requirements\":\"string\"}"));
        templates.add(new CapabilityTemplate("planMusic", "音乐规划", "规划背景音乐", "task", 2,
            "{\"scene\":\"string\",\"mood\":\"string\",\"style\":\"string\"}"));
        templates.add(new CapabilityTemplate("designAudioSystem", "音频系统", "设计音频架构", "task", 3,
            "{\"systemType\":\"enum:mixer,3d_audio,adaptive,events\",\"requirements\":\"string\"}"));
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
