package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.agent.BaseAgent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.model.AgentDefinition;
import com.chengxun.gamemaker.model.AgentDefinition.AgentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent招聘服务
 * 负责制作人Agent招聘新员工（Agent）
 *
 * 主要功能：
 * - 创建自定义角色Agent
 * - 分配能力
 * - 管理招聘流程
 * - 处理未知角色
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class AgentRecruitmentService {

    private static final Logger log = LoggerFactory.getLogger(AgentRecruitmentService.class);

    @Autowired
    private AgentManager agentManager;

    @Autowired
    private AgentCapabilityService capabilityService;

    @Autowired
    private DynamicCapabilityService dynamicCapabilityService;

    /** 核心Agent角色（不可删除） */
    private static final Set<String> CORE_ROLES = Set.of(
        "producer"  // 制作人是核心角色
    );

    /** 默认参与项目的角色 */
    private static final Set<String> DEFAULT_IN_PROJECT_ROLES = Set.of(
        "producer"  // 制作人默认在项目内
    );

    /** 预设角色模板 */
    private static final Map<String, RoleTemplate> PRESET_TEMPLATES = new HashMap<>();

    /** 用户自定义角色模板 */
    private final Map<String, RoleTemplate> customTemplates = new HashMap<>();

    static {
        // 预设角色模板
        PRESET_TEMPLATES.put("server-dev", new RoleTemplate(
            "server-dev", "服务端开发", "负责后端逻辑、API接口、数据库设计",
            Set.of("backend_development", "api_design", "database_design"),
            Set.of("java", "py", "go", "sql", "json")
        ));

        PRESET_TEMPLATES.put("client-dev", new RoleTemplate(
            "client-dev", "客户端开发", "负责前端逻辑、交互实现、性能优化",
            Set.of("frontend_development", "game_logic", "performance_optimization"),
            Set.of("js", "ts", "html", "css", "lua")
        ));

        PRESET_TEMPLATES.put("ui-dev", new RoleTemplate(
            "ui-dev", "UI设计", "负责界面设计、图标制作、视觉效果",
            Set.of("ui_design", "css_styling", "responsive_design"),
            Set.of("html", "css", "svg", "png")
        ));

        PRESET_TEMPLATES.put("system-planner", new RoleTemplate(
            "system-planner", "系统策划", "负责游戏系统设计、玩法策划、文档编写",
            Set.of("game_design", "system_design", "documentation"),
            Set.of("md", "txt", "json", "yaml")
        ));

        PRESET_TEMPLATES.put("numerical-planner", new RoleTemplate(
            "numerical-planner", "数值策划", "负责游戏数值平衡、经济系统、成长曲线",
            Set.of("numerical_design", "balance_tuning", "economy_design"),
            Set.of("xlsx", "csv", "json")
        ));

        PRESET_TEMPLATES.put("tester", new RoleTemplate(
            "tester", "测试工程师", "负责功能测试、性能测试、Bug报告",
            Set.of("testing", "bug_reporting", "test_automation"),
            Set.of("java", "py", "js")
        ));

        PRESET_TEMPLATES.put("git-commit", new RoleTemplate(
            "git-commit", "Git专员", "负责版本管理、代码提交、分支管理",
            Set.of("version_control", "git_operations"),
            Set.of("all")
        ));

        // 新增角色模板
        PRESET_TEMPLATES.put("security-expert", new RoleTemplate(
            "security-expert", "安全工程师", "负责代码安全审计、漏洞检测、反作弊系统",
            Set.of("security_audit", "vulnerability_detection", "anti_cheat"),
            Set.of("java", "py", "js", "sql")
        ));

        PRESET_TEMPLATES.put("data-analyst", new RoleTemplate(
            "data-analyst", "数据分析师", "负责玩家行为分析、留存分析、付费分析",
            Set.of("data_analysis", "retention_analysis", "monetization_analysis"),
            Set.of("py", "sql", "csv", "json")
        ));

        PRESET_TEMPLATES.put("tech-artist", new RoleTemplate(
            "tech-artist", "技术美术", "负责Shader开发、渲染优化、美术工具开发",
            Set.of("shader_development", "rendering_optimization", "art_tools"),
            Set.of("hlsl", "glsl", "shader", "py")
        ));

        PRESET_TEMPLATES.put("product-manager", new RoleTemplate(
            "product-manager", "产品经理", "负责产品规划、需求分析、用户体验设计",
            Set.of("product_planning", "requirement_analysis", "ux_design"),
            Set.of("md", "txt", "json")
        ));

        PRESET_TEMPLATES.put("localization", new RoleTemplate(
            "localization", "本地化专员", "负责多语言翻译、文化适配、本地化流程",
            Set.of("translation", "cultural_adaptation", "localization_management"),
            Set.of("json", "csv", "xml", "properties")
        ));

        PRESET_TEMPLATES.put("ai-engineer", new RoleTemplate(
            "ai-engineer", "AI工程师", "负责NPC行为AI、寻路算法、对话系统",
            Set.of("behavior_tree", "pathfinding", "dialogue_system"),
            Set.of("py", "java", "json", "yaml")
        ));

        PRESET_TEMPLATES.put("performance-engineer", new RoleTemplate(
            "performance-engineer", "性能优化师", "负责性能分析、瓶颈定位、优化方案",
            Set.of("performance_analysis", "profiling", "optimization"),
            Set.of("java", "py", "cpp", "json")
        ));

        PRESET_TEMPLATES.put("audio-dev", new RoleTemplate(
            "audio-dev", "音频设计师", "负责音效设计、背景音乐、音频系统",
            Set.of("sound_design", "music_composition", "audio_system"),
            Set.of("wav", "ogg", "mp3", "json")
        ));

        PRESET_TEMPLATES.put("narrative-planner", new RoleTemplate(
            "narrative-planner", "剧情策划", "负责世界观构建、角色设定、剧情设计",
            Set.of("worldbuilding", "character_design", "story_design"),
            Set.of("md", "txt", "json")
        ));

        PRESET_TEMPLATES.put("level-design", new RoleTemplate(
            "level-design", "关卡设计师", "负责关卡流程、地图布局、难度曲线",
            Set.of("level_design", "map_layout", "difficulty_curve"),
            Set.of("json", "yaml", "tmx")
        ));

        PRESET_TEMPLATES.put("devops", new RoleTemplate(
            "devops", "运维工程师", "负责CI/CD、服务器部署、监控告警",
            Set.of("cicd", "deployment", "monitoring"),
            Set.of("sh", "yaml", "dockerfile", "json")
        ));
    }

    /**
     * 角色模板
     */
    public static class RoleTemplate {
        private final String role;
        private final String name;
        private final String description;
        private final Set<String> defaultCapabilities;
        private final Set<String> supportedFileTypes;

        public RoleTemplate(String role, String name, String description,
                           Set<String> defaultCapabilities, Set<String> supportedFileTypes) {
            this.role = role;
            this.name = name;
            this.description = description;
            this.defaultCapabilities = defaultCapabilities;
            this.supportedFileTypes = supportedFileTypes;
        }

        // Getters
        public String getRole() { return role; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public Set<String> getDefaultCapabilities() { return defaultCapabilities; }
        public Set<String> getSupportedFileTypes() { return supportedFileTypes; }
    }

    /**
     * 招聘请求
     */
    public static class RecruitmentRequest {
        private String name;
        private String role;
        private String description;
        private Set<String> capabilities;
        private Set<String> supportedFileTypes;
        private Map<String, String> tags;
        private int maxContextSize;
        private boolean supportsImageGeneration;
        private String workDir;
        /** 是否加入项目（默认 false，制作人招聘时默认 true） */
        private boolean inProject = false;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Set<String> getCapabilities() { return capabilities; }
        public void setCapabilities(Set<String> capabilities) { this.capabilities = capabilities; }
        public Set<String> getSupportedFileTypes() { return supportedFileTypes; }
        public void setSupportedFileTypes(Set<String> types) { this.supportedFileTypes = types; }
        public Map<String, String> getTags() { return tags; }
        public void setTags(Map<String, String> tags) { this.tags = tags; }
        public boolean isInProject() { return inProject; }
        public void setInProject(boolean inProject) { this.inProject = inProject; }
        public int getMaxContextSize() { return maxContextSize; }
        public void setMaxContextSize(int size) { this.maxContextSize = size; }
        public boolean isSupportsImageGeneration() { return supportsImageGeneration; }
        public void setSupportsImageGeneration(boolean supports) { this.supportsImageGeneration = supports; }
        public String getWorkDir() { return workDir; }
        public void setWorkDir(String workDir) { this.workDir = workDir; }
    }

    /**
     * 获取预设角色模板列表
     */
    public List<Map<String, Object>> getPresetRoleTemplates() {
        return PRESET_TEMPLATES.values().stream()
            .map(template -> {
                Map<String, Object> map = new HashMap<>();
                map.put("role", template.getRole());
                map.put("name", template.getName());
                map.put("description", template.getDescription());
                map.put("capabilities", template.getDefaultCapabilities());
                map.put("fileTypes", template.getSupportedFileTypes());
                return map;
            })
            .collect(Collectors.toList());
    }

    /**
     * 获取角色模板
     */
    public RoleTemplate getRoleTemplate(String role) {
        return PRESET_TEMPLATES.get(role);
    }

    /**
     * 招聘新Agent（使用预设模板）
     *
     * @param producerId 制作人Agent ID
     * @param role 角色类型
     * @param name 新Agent名称
     * @param workDir 工作目录
     * @return 创建的Agent
     */
    public Agent recruitAgent(String producerId, String role, String name, String workDir) {
        // 验证制作人身份
        if (!isProducer(producerId)) {
            throw new RuntimeException("只有制作人Agent可以招聘新员工");
        }

        // 自动创建角色能力（如果不存在）
        String projectId = extractProjectId(producerId);
        dynamicCapabilityService.createCapabilitiesForRole(role, projectId);

        // 检查是否是预设角色
        RoleTemplate template = PRESET_TEMPLATES.get(role);
        if (template != null) {
            return recruitFromTemplate(producerId, template, name, workDir);
        } else {
            // 未知角色，创建自定义角色
            return recruitCustomRole(producerId, role, name, workDir);
        }
    }

    /**
     * 招聘自定义角色Agent（自动创建角色模板）
     * 制作人可以根据需要直接招聘任意角色，系统会自动创建角色模板
     *
     * @param producerId 制作人Agent ID
     * @param role 角色标识（如 "audio-dev", "security-expert" 等）
     * @param roleName 角色名称（如 "音频开发", "安全专家" 等）
     * @param name 新Agent名称
     * @param description 角色描述
     * @param capabilities 能力列表
     * @param supportedFileTypes 支持的文件类型
     * @param workDir 工作目录
     * @return 创建的Agent
     */
    public Agent recruitCustomAgent(String producerId, String role, String roleName, String name,
                                     String description, Set<String> capabilities,
                                     Set<String> supportedFileTypes, String workDir) {
        // 验证制作人身份
        if (!isProducer(producerId)) {
            throw new RuntimeException("只有制作人Agent可以招聘新员工");
        }

        // 自动创建角色能力
        String projectId = extractProjectId(producerId);
        dynamicCapabilityService.createCapabilitiesForRole(role, projectId);

        // 如果角色模板不存在，自动创建
        if (!PRESET_TEMPLATES.containsKey(role) && !customTemplates.containsKey(role)) {
            createCustomRoleTemplate(role, roleName != null ? roleName : role,
                description != null ? description : "自定义角色: " + role,
                capabilities, supportedFileTypes);
            log.info("Auto-created custom role template: {} - {}", role, roleName);
        }

        // 执行招聘
        return recruitCustomRole(producerId, role, name, workDir, description, capabilities, supportedFileTypes);
    }

    /**
     * 招聘自定义角色（带详细配置）
     */
    private Agent recruitCustomRole(String producerId, String role, String name, String workDir,
                                     String description, Set<String> capabilities,
                                     Set<String> supportedFileTypes) {
        String agentId = generateAgentId(role);
        String projectId = extractProjectId(producerId);
        boolean defaultInProject = DEFAULT_IN_PROJECT_ROLES.contains(role);

        AgentDefinition.Builder builder = AgentDefinition.builder()
            .id(agentId)
            .name(name)
            .role(role)
            .description(description != null ? description : "自定义角色: " + role)
            .workDir(workDir)
            .projectId(projectId)
            .tag("recruited_by", producerId)
            .tag("recruitment_time", String.valueOf(System.currentTimeMillis()))
            .tag("custom_role", "true")
            .maxContextSize(100000)
            .defaultInProject(defaultInProject)
            .inProject(defaultInProject);  // 默认在项目内的角色自动加入项目

        // 设置能力
        if (capabilities != null && !capabilities.isEmpty()) {
            builder.capabilities(capabilities);
        }

        // 设置支持的文件类型
        if (supportedFileTypes != null) {
            for (String type : supportedFileTypes) {
                builder.supportedFileType(type);
            }
        }

        AgentDefinition definition = builder.build();
        Agent agent = agentManager.createAgent(definition);

        log.info("Custom role agent recruited by {}: {} ({}) - role: {} for project: {}",
            producerId, name, agentId, role, projectId);

        return agent;
    }

    /**
     * 使用预设模板招聘
     */
    private Agent recruitFromTemplate(String producerId, RoleTemplate template, String name, String workDir) {
        String agentId = generateAgentId(template.getRole());
        String projectId = extractProjectId(producerId);
        boolean defaultInProject = DEFAULT_IN_PROJECT_ROLES.contains(template.getRole());

        AgentDefinition.Builder builder = AgentDefinition.builder()
            .id(agentId)
            .name(name)
            .role(template.getRole())
            .description(template.getDescription())
            .workDir(workDir)
            .projectId(projectId)
            .capabilities(template.getDefaultCapabilities())
            .tag("recruited_by", producerId)
            .tag("recruitment_time", String.valueOf(System.currentTimeMillis()))
            .maxContextSize(100000)
            .defaultInProject(defaultInProject)
            .inProject(defaultInProject);  // 默认在项目内的角色自动加入项目

        // 添加支持的文件类型
        for (String type : template.getSupportedFileTypes()) {
            builder.supportedFileType(type);
        }

        AgentDefinition definition = builder.build();

        Agent agent = agentManager.createAgent(definition);
        log.info("Agent recruited by {}: {} ({}) for project: {} (inProject: {})",
            producerId, name, agentId, projectId, defaultInProject);

        return agent;
    }

    /**
     * 招聘自定义角色（基础版本）
     */
    private Agent recruitCustomRole(String producerId, String role, String name, String workDir) {
        return recruitCustomRole(producerId, role, name, workDir, null, null, null);
    }

    /**
     * 完整招聘流程（带详细配置）
     *
     * @param producerId 制作人Agent ID
     * @param request 招聘请求
     * @return 创建的Agent
     */
    public Agent recruitAgentFull(String producerId, RecruitmentRequest request) {
        // 验证制作人身份
        if (!isProducer(producerId)) {
            throw new RuntimeException("只有制作人Agent可以招聘新员工");
        }

        String agentId = generateAgentId(request.getRole());
        String projectId = extractProjectId(producerId);

        // 自动创建角色能力
        dynamicCapabilityService.createCapabilitiesForRole(request.getRole(), projectId);

        // 判断是否在项目内：制作人默认在项目内，其他角色由请求指定
        boolean shouldBeInProject = DEFAULT_IN_PROJECT_ROLES.contains(request.getRole()) || request.isInProject();

        AgentDefinition.Builder builder = AgentDefinition.builder()
            .id(agentId)
            .name(request.getName())
            .role(request.getRole())
            .description(request.getDescription() != null ? request.getDescription() : "由制作人招聘")
            .workDir(request.getWorkDir())
            .projectId(projectId)
            .tag("recruited_by", producerId)
            .tag("recruitment_time", String.valueOf(System.currentTimeMillis()))
            .defaultInProject(shouldBeInProject)
            .inProject(shouldBeInProject);  // 根据配置决定是否加入项目

        // 设置能力
        if (request.getCapabilities() != null) {
            builder.capabilities(request.getCapabilities());
        }

        // 设置支持的文件类型
        if (request.getSupportedFileTypes() != null) {
            for (String type : request.getSupportedFileTypes()) {
                builder.supportedFileType(type);
            }
        }

        // 设置标签
        if (request.getTags() != null) {
            builder.tags(request.getTags());
        }

        // 设置上下文大小
        if (request.getMaxContextSize() > 0) {
            builder.maxContextSize(request.getMaxContextSize());
        } else {
            builder.maxContextSize(100000);
        }

        // 设置图片生成支持
        builder.supportsImageGeneration(request.isSupportsImageGeneration());

        AgentDefinition definition = builder.build();
        Agent agent = agentManager.createAgent(definition);

        log.info("Agent recruited with full config by {}: {} ({})", producerId, request.getName(), agentId);

        return agent;
    }

    /**
     * 为已存在的Agent添加能力
     */
    public void addCapabilityToAgent(String producerId, String agentId, String capability) {
        if (!isProducer(producerId)) {
            throw new RuntimeException("只有制作人Agent可以修改员工能力");
        }

        capabilityService.addCapability(agentId, capability);
        log.info("Capability {} added to agent {} by producer {}", capability, agentId, producerId);
    }

    /**
     * 为已存在的Agent设置标签
     */
    public void setAgentTag(String producerId, String agentId, String key, String value) {
        if (!isProducer(producerId)) {
            throw new RuntimeException("只有制作人Agent可以修改员工标签");
        }

        capabilityService.setAgentTag(agentId, key, value);
        log.info("Tag {}={} set for agent {} by producer {}", key, value, agentId, producerId);
    }

    /**
     * 解雇Agent（删除非核心Agent）
     */
    public boolean dismissAgent(String producerId, String agentId) {
        if (!isProducer(producerId)) {
            throw new RuntimeException("只有制作人Agent可以解雇员工");
        }

        Agent agent = agentManager.getAgent(agentId);
        if (agent == null) {
            return false;
        }

        // 检查是否是核心角色
        if (isCoreRole(agent.getRole())) {
            throw new RuntimeException("不能解雇核心角色Agent: " + agent.getRole());
        }

        agentManager.removeAgent(agentId);
        log.info("Agent {} dismissed by producer {}", agentId, producerId);

        return true;
    }

    /**
     * 检查是否是制作人
     */
    private boolean isProducer(String agentId) {
        Agent agent = agentManager.getAgent(agentId);
        return agent != null && "producer".equals(agent.getRole());
    }

    /**
     * 从运行时 ID 中提取项目 ID
     * 运行时 ID 格式：projectId:agentRole
     *
     * @param runtimeId 运行时 ID
     * @return 项目 ID，无法提取返回 null
     */
    private String extractProjectId(String runtimeId) {
        if (runtimeId == null) return null;
        int lastColon = runtimeId.lastIndexOf(':');
        if (lastColon > 0) {
            return runtimeId.substring(0, lastColon);
        }
        return null;
    }

    /**
     * 检查是否是核心角色
     */
    private boolean isCoreRole(String role) {
        return CORE_ROLES.contains(role);
    }

    /**
     * 检查Agent是否是核心Agent（不可删除）
     */
    public boolean isCoreAgent(String agentId) {
        Agent agent = agentManager.getAgent(agentId);
        if (agent == null) {
            return false;
        }
        return isCoreRole(agent.getRole());
    }

    /**
     * 生成Agent ID
     */
    private String generateAgentId(String role) {
        String prefix = role.replaceAll("[^a-z]", "");
        if (prefix.isEmpty()) {
            prefix = "agent";
        }
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return prefix + "-" + suffix;
    }

    /**
     * 获取可招聘的角色列表
     * 包括预设角色和用户自定义角色
     */
    public List<Map<String, Object>> getRecruitableRoles() {
        List<Map<String, Object>> roles = new ArrayList<>();

        // 预设角色
        for (RoleTemplate template : PRESET_TEMPLATES.values()) {
            Map<String, Object> role = new HashMap<>();
            role.put("role", template.getRole());
            role.put("name", template.getName());
            role.put("description", template.getDescription());
            role.put("preset", true);
            role.put("capabilities", template.getDefaultCapabilities());
            roles.add(role);
        }

        // 用户自定义角色
        for (RoleTemplate template : customTemplates.values()) {
            Map<String, Object> role = new HashMap<>();
            role.put("role", template.getRole());
            role.put("name", template.getName());
            role.put("description", template.getDescription());
            role.put("preset", false);
            role.put("capabilities", template.getDefaultCapabilities());
            roles.add(role);
        }

        return roles;
    }

    /**
     * 创建自定义角色模板
     *
     * @param role 角色标识
     * @param name 角色名称
     * @param description 角色描述
     * @param capabilities 默认能力
     * @param supportedFileTypes 支持的文件类型
     * @return 创建的角色模板
     */
    public RoleTemplate createCustomRoleTemplate(String role, String name, String description,
                                                  Set<String> capabilities, Set<String> supportedFileTypes) {
        // 检查是否与预设角色冲突
        if (PRESET_TEMPLATES.containsKey(role)) {
            throw new RuntimeException("角色标识 '" + role + "' 已被预设角色使用");
        }

        // 检查是否已存在
        if (customTemplates.containsKey(role)) {
            throw new RuntimeException("自定义角色 '" + role + "' 已存在");
        }

        RoleTemplate template = new RoleTemplate(role, name, description,
            capabilities != null ? capabilities : Set.of(),
            supportedFileTypes != null ? supportedFileTypes : Set.of());

        customTemplates.put(role, template);
        log.info("Custom role template created: {} - {}", role, name);

        return template;
    }

    /**
     * 更新自定义角色模板
     *
     * @param role 角色标识
     * @param name 角色名称
     * @param description 角色描述
     * @param capabilities 默认能力
     * @param supportedFileTypes 支持的文件类型
     * @return 更新的角色模板
     */
    public RoleTemplate updateCustomRoleTemplate(String role, String name, String description,
                                                  Set<String> capabilities, Set<String> supportedFileTypes) {
        // 检查是否是预设角色
        if (PRESET_TEMPLATES.containsKey(role)) {
            throw new RuntimeException("不能修改预设角色");
        }

        RoleTemplate existing = customTemplates.get(role);
        if (existing == null) {
            throw new RuntimeException("自定义角色 '" + role + "' 不存在");
        }

        RoleTemplate updated = new RoleTemplate(role, name, description,
            capabilities != null ? capabilities : existing.getDefaultCapabilities(),
            supportedFileTypes != null ? supportedFileTypes : existing.getSupportedFileTypes());

        customTemplates.put(role, updated);
        log.info("Custom role template updated: {} - {}", role, name);

        return updated;
    }

    /**
     * 删除自定义角色模板
     *
     * @param role 角色标识
     * @return 是否删除成功
     */
    public boolean deleteCustomRoleTemplate(String role) {
        // 检查是否是预设角色
        if (PRESET_TEMPLATES.containsKey(role)) {
            throw new RuntimeException("不能删除预设角色");
        }

        RoleTemplate removed = customTemplates.remove(role);
        if (removed != null) {
            log.info("Custom role template deleted: {}", role);
            return true;
        }
        return false;
    }

    /**
     * 获取自定义角色模板列表
     */
    public List<Map<String, Object>> getCustomRoleTemplates() {
        return customTemplates.values().stream()
            .map(template -> {
                Map<String, Object> map = new HashMap<>();
                map.put("role", template.getRole());
                map.put("name", template.getName());
                map.put("description", template.getDescription());
                map.put("capabilities", template.getDefaultCapabilities());
                map.put("fileTypes", template.getSupportedFileTypes());
                return map;
            })
            .collect(Collectors.toList());
    }

    /**
     * 获取已招聘的Agent列表
     */
    public List<Map<String, Object>> getRecruitedAgents() {
        return agentManager.getAllAgents().stream()
            .filter(agent -> agent.getDefinition().getTag("recruited_by") != null)
            .map(agent -> {
                Map<String, Object> info = new HashMap<>();
                info.put("id", agent.getId());
                info.put("name", agent.getName());
                info.put("role", agent.getRole());
                info.put("recruitedBy", agent.getDefinition().getTag("recruited_by"));
                info.put("capabilities", agent.getDefinition().getCapabilities());
                info.put("isCore", isCoreRole(agent.getRole()));
                return info;
            })
            .collect(Collectors.toList());
    }

    /**
     * 获取招聘统计
     */
    public Map<String, Object> getRecruitmentStats() {
        Map<String, Object> stats = new HashMap<>();

        List<Agent> allAgents = agentManager.getAllAgents();
        List<Agent> recruitedAgents = allAgents.stream()
            .filter(agent -> agent.getDefinition().getTag("recruited_by") != null)
            .collect(Collectors.toList());

        stats.put("totalAgents", allAgents.size());
        stats.put("recruitedAgents", recruitedAgents.size());
        stats.put("coreAgents", allAgents.stream()
            .filter(agent -> isCoreRole(agent.getRole()))
            .count());

        // 按角色统计
        Map<String, Long> roleCounts = recruitedAgents.stream()
            .collect(Collectors.groupingBy(Agent::getRole, Collectors.counting()));
        stats.put("roleCounts", roleCounts);

        return stats;
    }
}
