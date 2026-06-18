package com.chengxun.gamemaker.model;

import java.util.*;

public class AgentDefinition {
    /** Agent 原始 ID（模板定义的，如 producer、server-dev） */
    private String id;
    /** Agent 显示名称 */
    private String name;
    /** Agent 角色（producer、server-dev、ui-dev 等） */
    private String role;
    private String description;
    private String agentsFile;
    private String apiKey;
    private String apiUrl;
    private String model;
    private String sessionId;
    private String workDir;
    private AgentStatus status;
    private boolean parent;
    private String parentId;

    /** 所属项目 ID（项目级隔离的关键字段） */
    private String projectId;
    /** 运行时 ID（由 projectId + role 组成，全局唯一，格式: projectId:role） */
    private String runtimeId;
    /** 当前使用的 Token ID（池化模式下记录实际分配的 Token） */
    private Long assignedTokenId;
    /** 推理深度 1-5: 1=快速 2=标准 3=深入 4=全面 5=极致 */
    private int reasoningDepth = 3;

    /** 思维模式 1-5: 控制AI的思维风格和约束程度
     *  1=高度严谨: 极度保守、精确执行、零风险容忍
     *  2=严谨: 稳健保守、注重规范、最小风险
     *  3=平衡: 兼顾效率与质量、适度灵活（默认）
     *  4=创新: 鼓励创意、接受适度风险、探索新方案
     *  5=突破: 大胆突破、颠覆常规、追求极致创意
     */
    private int thinkingMode = 3;

    /**
     * 根据角色获取默认思维模式
     * 开发类角色默认偏严谨，策划类角色默认偏创新
     *
     * @param role Agent 角色
     * @return 默认思维模式 1-5
     */
    public static int getDefaultThinkingMode(String role) {
        if (role == null) return 3;
        return switch (role) {
            // 开发类：严谨精确
            case "server-dev", "client-dev", "ui-dev" -> 2;
            case "git-commit" -> 1;
            // 测试验证：严谨
            case "verification" -> 2;
            // 策划类：创新探索
            case "system-planner", "numerical-planner" -> 4;
            // 制作人：平衡（需要协调+规划+适度创新）
            case "producer" -> 3;
            // 默认：平衡
            default -> 3;
        };
    }

    // ===== 自定义标签系统 =====

    /** 自定义标签（用于灵活分类和筛选Agent） */
    private Map<String, String> tags = new HashMap<>();

    /** Agent能力标签（标识Agent具备的能力） */
    private Set<String> capabilities = new HashSet<>();

    /** 支持的文件类型（如：java, py, js, png, psd等） */
    private Set<String> supportedFileTypes = new HashSet<>();

    /** 不支持的功能（如：image_generation, 3d_rendering等） */
    private Set<String> unsupportedFeatures = new HashSet<>();

    // ===== 项目参与配置 =====

    /** 是否默认参与项目（制作人默认 true，其他角色默认 false） */
    private boolean defaultInProject = false;

    /** 当前是否在项目中（运行时状态） */
    private boolean inProject = false;

    // ===== 上下文配置 =====

    /** 最大上下文大小（token数） */
    private int maxContextSize = 100000;

    /** 当前上下文使用量（token数） */
    private int currentContextUsage = 0;

    /** 上下文警告阈值（百分比） */
    private int contextWarningThreshold = 80;

    // ===== API能力配置 =====

    /** 是否支持图片生成 */
    private boolean supportsImageGeneration = false;

    /** 是否支持代码执行 */
    private boolean supportsCodeExecution = true;

    /** 是否支持文件操作 */
    private boolean supportsFileOperations = true;

    /** API提供商（openai, anthropic, local等） */
    private String apiProvider = "anthropic";

    // ===== 待生效的 Token 配置 =====

    /** 待生效的 API Key（等待当前任务完成后应用） */
    private String pendingApiKey;

    /** 待生效的 API URL */
    private String pendingApiUrl;

    /** 待生效的模型 */
    private String pendingModel;

    /** 是否有待生效的配置变更 */
    private boolean hasPendingConfig = false;

    /** 推理深度描述 */
    public static String getReasoningDepthLabel(int depth) {
        return switch (depth) {
            case 1 -> "快速 (Quick)";
            case 2 -> "标准 (Standard)";
            case 3 -> "深入 (Deep)";
            case 4 -> "全面 (Thorough)";
            case 5 -> "极致 (Extreme)";
            default -> "深入 (Deep)";
        };
    }

    /** 获取推理深度对应的系统指令 */
    public static String getReasoningDepthInstruction(int depth) {
        return switch (depth) {
            case 1 -> "[推理模式: 快速] 请快速给出简洁直接的回答，不需要详细分析，关注最关键的要点即可。";
            case 2 -> "[推理模式: 标准] 请给出清晰合理的回答，包含必要的分析和说明。";
            case 3 -> "[推理模式: 深入] 请进行深入分析，考虑多个角度，给出详细的推理过程和结论。";
            case 4 -> "[推理模式: 全面] 请进行全面深入的分析，考虑各种可能性、边界情况和潜在风险，给出详尽的方案。";
            case 5 -> "[推理模式: 极致] 请进行极致深度的思考和分析，穷举所有可能性，考虑短期和长期影响，给出最优方案并说明权衡。";
            default -> getReasoningDepthInstruction(3);
        };
    }

    /**
     * 将推理深度映射到 Claude CLI 的 --effort 参数
     * CLI 支持: low, medium, high, xhigh, max
     *
     * @param depth 推理深度 1-5
     * @return CLI effort 级别字符串
     */
    public static String reasoningDepthToEffort(int depth) {
        return switch (depth) {
            case 1 -> "low";
            case 2 -> "medium";
            case 3 -> "high";
            case 4 -> "xhigh";
            case 5 -> "max";
            default -> "high";
        };
    }

    /** 思维模式描述 */
    public static String getThinkingModeLabel(int mode) {
        return switch (mode) {
            case 1 -> "高度严谨 (Ultra-Rigorous)";
            case 2 -> "严谨 (Rigorous)";
            case 3 -> "平衡 (Balanced)";
            case 4 -> "创新 (Creative)";
            case 5 -> "突破 (Breakthrough)";
            default -> "平衡 (Balanced)";
        };
    }

    /**
     * 获取思维模式对应的系统指令
     * 通过 prompt 层面控制 AI 的思维风格，弥补 CLI 不支持 temperature 的不足
     *
     * @param mode 思维模式 1-5
     * @return 注入到系统提示词中的思维风格指令
     */
    public static String getThinkingModeInstruction(int mode) {
        return switch (mode) {
            case 1 -> "[思维模式: 高度严谨]\n" +
                "你的思维必须极度严谨和保守：\n" +
                "- 每一步决策都要有明确依据，不做任何未经验证的假设\n" +
                "- 严格遵循既有规范和最佳实践，不引入未经验证的新方案\n" +
                "- 代码修改必须最小化，只改必要的部分\n" +
                "- 优先选择最成熟、最稳定的技术方案\n" +
                "- 对任何风险零容忍，宁可保守也不冒险";

            case 2 -> "[思维模式: 严谨]\n" +
                "你的思维应当稳健保守：\n" +
                "- 注重规范和标准，遵循项目既有模式\n" +
                "- 决策前充分评估风险，选择经过验证的方案\n" +
                "- 代码修改要有明确目的，避免不必要的变更\n" +
                "- 优先保证正确性和稳定性，其次考虑效率\n" +
                "- 遇到不确定时，选择更保守的路径";

            case 3 -> "[思维模式: 平衡]\n" +
                "你的思维应当兼顾效率与质量：\n" +
                "- 在规范和灵活性之间找到平衡点\n" +
                "- 优先使用经过验证的方案，但不排斥合理的新思路\n" +
                "- 权衡开发速度和代码质量，找到最优平衡\n" +
                "- 适度创新，但要确保变更可控\n" +
                "- 遇到问题时，综合考虑多种方案的优劣";

            case 4 -> "[思维模式: 创新]\n" +
                "你的思维应当鼓励创意和探索：\n" +
                "- 主动探索更优雅、更高效的实现方式\n" +
                "- 不拘泥于既有模式，敢于提出新方案\n" +
                "- 在保证功能正确的前提下，尝试创新的架构和设计\n" +
                "- 接受适度风险以换取更好的解决方案\n" +
                "- 善于借鉴其他领域的优秀实践";

            case 5 -> "[思维模式: 突破]\n" +
                "你的思维应当大胆突破常规：\n" +
                "- 跳出固有框架，用全新的视角审视问题\n" +
                "- 追求极致的创意和设计，不满足于平庸的方案\n" +
                "- 敢于挑战既有架构，提出颠覆性的改进\n" +
                "- 在安全边界内大胆实验，追求最优解\n" +
                "- 将创新放在首位，用创意驱动实现";

            default -> getThinkingModeInstruction(3);
        };
    }
    
    public enum AgentStatus {
        IDLE, WORKING, WAITING, ERROR, STOPPED
    }
    
    public AgentDefinition() {}

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getAgentsFile() { return agentsFile; }
    public void setAgentsFile(String agentsFile) { this.agentsFile = agentsFile; }
    
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    
    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
    
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getWorkDir() { return workDir; }
    public void setWorkDir(String workDir) { this.workDir = workDir; }
    
    public AgentStatus getStatus() { return status; }
    public void setStatus(AgentStatus status) { this.status = status; }
    
    public boolean isParent() { return parent; }
    public void setParent(boolean parent) { this.parent = parent; }
    
    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) {
        this.projectId = projectId;
        // 当 projectId 变化时，自动更新 runtimeId
        if (projectId != null && role != null) {
            this.runtimeId = projectId + ":" + role;
        }
    }

    public String getRuntimeId() { return runtimeId; }
    public void setRuntimeId(String runtimeId) { this.runtimeId = runtimeId; }

    public Long getAssignedTokenId() { return assignedTokenId; }
    public void setAssignedTokenId(Long assignedTokenId) { this.assignedTokenId = assignedTokenId; }

    /**
     * 获取运行时 ID
     * 如果 runtimeId 未设置，自动生成: projectId:role
     * 如果都没有，返回原始 id
     */
    public String getEffectiveId() {
        if (runtimeId != null) return runtimeId;
        if (projectId != null && role != null) return projectId + ":" + role;
        return id;
    }

    public int getReasoningDepth() { return reasoningDepth; }
    public void setReasoningDepth(int reasoningDepth) {
        this.reasoningDepth = Math.max(1, Math.min(5, reasoningDepth));
    }

    public int getThinkingMode() { return thinkingMode; }
    public void setThinkingMode(int thinkingMode) {
        this.thinkingMode = Math.max(1, Math.min(5, thinkingMode));
    }

    // ===== 自定义标签相关 =====

    public Map<String, String> getTags() { return tags; }
    public void setTags(Map<String, String> tags) { this.tags = tags != null ? tags : new HashMap<>(); }

    public String getTag(String key) { return tags.get(key); }
    public void setTag(String key, String value) { tags.put(key, value); }
    public void removeTag(String key) { tags.remove(key); }

    public Set<String> getCapabilities() { return capabilities; }
    public void setCapabilities(Set<String> capabilities) { this.capabilities = capabilities != null ? capabilities : new HashSet<>(); }
    public void addCapability(String capability) { capabilities.add(capability); }
    public void removeCapability(String capability) { capabilities.remove(capability); }
    public boolean hasCapability(String capability) { return capabilities.contains(capability); }

    public Set<String> getSupportedFileTypes() { return supportedFileTypes; }
    public void setSupportedFileTypes(Set<String> types) { this.supportedFileTypes = types != null ? types : new HashSet<>(); }
    public void addSupportedFileType(String type) { supportedFileTypes.add(type.toLowerCase()); }
    public boolean supportsFileType(String type) { return supportedFileTypes.contains(type.toLowerCase()); }

    public Set<String> getUnsupportedFeatures() { return unsupportedFeatures; }
    public void setUnsupportedFeatures(Set<String> features) { this.unsupportedFeatures = features != null ? features : new HashSet<>(); }
    public void addUnsupportedFeature(String feature) { unsupportedFeatures.add(feature); }
    public boolean supportsFeature(String feature) { return !unsupportedFeatures.contains(feature); }

    // ===== 项目参与配置相关 =====

    /** 是否默认参与项目 */
    public boolean isDefaultInProject() { return defaultInProject; }
    public void setDefaultInProject(boolean defaultInProject) { this.defaultInProject = defaultInProject; }

    /** 当前是否在项目中 */
    public boolean isInProject() { return inProject; }
    public void setInProject(boolean inProject) { this.inProject = inProject; }

    // ===== 上下文相关 =====

    public int getMaxContextSize() { return maxContextSize; }
    public void setMaxContextSize(int maxContextSize) { this.maxContextSize = maxContextSize; }

    public int getCurrentContextUsage() { return currentContextUsage; }
    public void setCurrentContextUsage(int usage) { this.currentContextUsage = usage; }

    public int getContextWarningThreshold() { return contextWarningThreshold; }
    public void setContextWarningThreshold(int threshold) { this.contextWarningThreshold = threshold; }

    /** 获取上下文使用百分比 */
    public double getContextUsagePercent() {
        if (maxContextSize == 0) return 0;
        return (double) currentContextUsage / maxContextSize * 100;
    }

    /** 检查上下文是否需要压缩 */
    public boolean isContextNearLimit() {
        return getContextUsagePercent() >= contextWarningThreshold;
    }

    /** 获取剩余上下文空间 */
    public int getRemainingContextSize() {
        return Math.max(0, maxContextSize - currentContextUsage);
    }

    // ===== API能力相关 =====

    public boolean isSupportsImageGeneration() { return supportsImageGeneration; }
    public void setSupportsImageGeneration(boolean supports) { this.supportsImageGeneration = supports; }

    public boolean isSupportsCodeExecution() { return supportsCodeExecution; }
    public void setSupportsCodeExecution(boolean supports) { this.supportsCodeExecution = supports; }

    public boolean isSupportsFileOperations() { return supportsFileOperations; }
    public void setSupportsFileOperations(boolean supports) { this.supportsFileOperations = supports; }

    public String getApiProvider() { return apiProvider; }
    public void setApiProvider(String provider) { this.apiProvider = provider; }

    // ===== 待生效配置相关 =====

    public String getPendingApiKey() { return pendingApiKey; }
    public void setPendingApiKey(String pendingApiKey) { this.pendingApiKey = pendingApiKey; }

    public String getPendingApiUrl() { return pendingApiUrl; }
    public void setPendingApiUrl(String pendingApiUrl) { this.pendingApiUrl = pendingApiUrl; }

    public String getPendingModel() { return pendingModel; }
    public void setPendingModel(String pendingModel) { this.pendingModel = pendingModel; }

    public boolean isHasPendingConfig() { return hasPendingConfig; }
    public void setHasPendingConfig(boolean hasPendingConfig) { this.hasPendingConfig = hasPendingConfig; }

    /**
     * 设置待生效的 API 配置
     *
     * @param apiKey API Key
     * @param apiUrl API URL
     * @param model  模型
     */
    public void setPendingApiConfig(String apiKey, String apiUrl, String model) {
        this.pendingApiKey = apiKey;
        this.pendingApiUrl = apiUrl;
        this.pendingModel = model;
        this.hasPendingConfig = true;
    }

    /**
     * 应用待生效的配置
     * 将待生效的配置应用到当前配置
     *
     * @return true 如果有配置被应用
     */
    public boolean applyPendingConfig() {
        if (!hasPendingConfig) {
            return false;
        }

        this.apiKey = this.pendingApiKey;
        this.apiUrl = this.pendingApiUrl;
        this.model = this.pendingModel;

        // 清除待生效配置
        this.pendingApiKey = null;
        this.pendingApiUrl = null;
        this.pendingModel = null;
        this.hasPendingConfig = false;

        return true;
    }

    /**
     * 清除待生效的配置
     */
    public void clearPendingConfig() {
        this.pendingApiKey = null;
        this.pendingApiUrl = null;
        this.pendingModel = null;
        this.hasPendingConfig = false;
    }

    // ===== Builder增强 =====

    public static class Builder {
        private final AgentDefinition def = new AgentDefinition();

        public Builder id(String id) { def.id = id; return this; }
        public Builder name(String name) { def.name = name; return this; }
        public Builder role(String role) { def.role = role; return this; }
        public Builder description(String description) { def.description = description; return this; }
        public Builder agentsFile(String agentsFile) { def.agentsFile = agentsFile; return this; }
        public Builder apiKey(String apiKey) { def.apiKey = apiKey; return this; }
        public Builder apiUrl(String apiUrl) { def.apiUrl = apiUrl; return this; }
        public Builder model(String model) { def.model = model; return this; }
        public Builder sessionId(String sessionId) { def.sessionId = sessionId; return this; }
        public Builder workDir(String workDir) { def.workDir = workDir; return this; }
        public Builder status(AgentStatus status) { def.status = status; return this; }
        public Builder parent(boolean parent) { def.parent = parent; return this; }
        public Builder parentId(String parentId) { def.parentId = parentId; return this; }
        public Builder reasoningDepth(int reasoningDepth) { def.reasoningDepth = reasoningDepth; return this; }
        public Builder thinkingMode(int thinkingMode) { def.thinkingMode = thinkingMode; return this; }
        public Builder projectId(String projectId) { def.projectId = projectId; return this; }
        public Builder runtimeId(String runtimeId) { def.runtimeId = runtimeId; return this; }
        public Builder assignedTokenId(Long assignedTokenId) { def.assignedTokenId = assignedTokenId; return this; }

        public Builder tag(String key, String value) { def.tags.put(key, value); return this; }
        public Builder tags(Map<String, String> tags) { def.tags.putAll(tags); return this; }
        public Builder capability(String capability) { def.capabilities.add(capability); return this; }
        public Builder capabilities(Set<String> capabilities) { def.capabilities.addAll(capabilities); return this; }
        public Builder supportedFileType(String type) { def.supportedFileTypes.add(type.toLowerCase()); return this; }
        public Builder unsupportedFeature(String feature) { def.unsupportedFeatures.add(feature); return this; }
        public Builder maxContextSize(int size) { def.maxContextSize = size; return this; }
        public Builder supportsImageGeneration(boolean supports) { def.supportsImageGeneration = supports; return this; }
        public Builder supportsCodeExecution(boolean supports) { def.supportsCodeExecution = supports; return this; }
        public Builder supportsFileOperations(boolean supports) { def.supportsFileOperations = supports; return this; }
        public Builder apiProvider(String provider) { def.apiProvider = provider; return this; }
        public Builder defaultInProject(boolean defaultInProject) { def.defaultInProject = defaultInProject; return this; }
        public Builder inProject(boolean inProject) { def.inProject = inProject; return this; }

        public AgentDefinition build() {
            // 自动生成 runtimeId: projectId:role
            if (def.projectId != null && def.role != null && def.runtimeId == null) {
                def.runtimeId = def.projectId + ":" + def.role;
            }
            return def;
        }
    }

    // ===== 预设角色模板 =====

    /**
     * 创建UI/美术Agent定义（带项目ID）
     * 注意：如果API不支持图片生成，会自动禁用相关功能
     */
    public static AgentDefinition createUiAgent(String projectId, String id, String name, String workDir, boolean supportsImage) {
        return createUiAgentBuilder(id, name, workDir, supportsImage).projectId(projectId).build();
    }

    /**
     * 创建UI/美术Agent定义
     * 注意：如果API不支持图片生成，会自动禁用相关功能
     */
    public static AgentDefinition createUiAgent(String id, String name, String workDir, boolean supportsImage) {
        return createUiAgentBuilder(id, name, workDir, supportsImage).build();
    }

    private static Builder createUiAgentBuilder(String id, String name, String workDir, boolean supportsImage) {
        Builder builder = builder()
            .id(id)
            .name(name)
            .role("ui-dev")
            .description("UI/美术开发Agent，负责界面设计、图标制作、视觉效果")
            .workDir(workDir)
            .capability("ui_design")
            .capability("css_styling")
            .capability("responsive_design")
            .supportedFileType("html")
            .supportedFileType("css")
            .supportedFileType("svg")
            .tag("department", "client")
            .tag("speciality", "ui")
            .maxContextSize(80000);

        if (supportsImage) {
            builder.capability("image_generation")
                .capability("icon_design")
                .supportedFileType("png")
                .supportedFileType("jpg")
                .supportedFileType("svg")
                .supportsImageGeneration(true);
        } else {
            builder.unsupportedFeature("image_generation")
                .tag("note", "API不支持图片生成，需要使用预制资源或SVG")
                .supportsImageGeneration(false);
        }

        return builder;
    }

    /**
     * 创建服务端Agent定义（带项目ID）
     */
    public static AgentDefinition createServerAgent(String projectId, String id, String name, String workDir) {
        return createServerAgent(id, name, workDir).withProjectId(projectId);
    }

    /**
     * 创建服务端Agent定义
     */
    public static AgentDefinition createServerAgent(String id, String name, String workDir) {
        return builder()
            .id(id)
            .name(name)
            .role("server-dev")
            .description("服务端开发Agent，负责后端逻辑、API接口、数据库设计")
            .workDir(workDir)
            .capability("backend_development")
            .capability("api_design")
            .capability("database_design")
            .capability("server_architecture")
            .supportedFileType("java")
            .supportedFileType("py")
            .supportedFileType("go")
            .supportedFileType("sql")
            .supportedFileType("json")
            .tag("department", "server")
            .tag("speciality", "backend")
            .maxContextSize(120000)
            .build();
    }

    /**
     * 创建客户端Agent定义（带项目ID）
     */
    public static AgentDefinition createClientAgent(String projectId, String id, String name, String workDir) {
        return createClientAgent(id, name, workDir).withProjectId(projectId);
    }

    /**
     * 创建客户端Agent定义
     */
    public static AgentDefinition createClientAgent(String id, String name, String workDir) {
        return builder()
            .id(id)
            .name(name)
            .role("client-dev")
            .description("客户端开发Agent，负责前端逻辑、交互实现、性能优化")
            .workDir(workDir)
            .capability("frontend_development")
            .capability("game_logic")
            .capability("performance_optimization")
            .supportedFileType("js")
            .supportedFileType("ts")
            .supportedFileType("html")
            .supportedFileType("css")
            .supportedFileType("lua")
            .tag("department", "client")
            .tag("speciality", "frontend")
            .maxContextSize(100000)
            .build();
    }

    /**
     * 创建策划Agent定义（带项目ID）
     */
    public static AgentDefinition createPlannerAgent(String projectId, String id, String name, String workDir) {
        return createPlannerAgent(id, name, workDir).withProjectId(projectId);
    }

    /**
     * 创建策划Agent定义
     */
    public static AgentDefinition createPlannerAgent(String id, String name, String workDir) {
        return builder()
            .id(id)
            .name(name)
            .role("system-planner")
            .description("系统策划Agent，负责游戏系统设计、玩法策划、文档编写")
            .workDir(workDir)
            .capability("game_design")
            .capability("system_design")
            .capability("documentation")
            .supportedFileType("md")
            .supportedFileType("txt")
            .supportedFileType("json")
            .supportedFileType("yaml")
            .tag("department", "design")
            .tag("speciality", "system")
            .maxContextSize(150000)
            .build();
    }

    /**
     * 创建测试Agent定义（带项目ID）
     */
    public static AgentDefinition createTesterAgent(String projectId, String id, String name, String workDir) {
        return createTesterAgent(id, name, workDir).withProjectId(projectId);
    }

    /**
     * 创建测试Agent定义
     */
    public static AgentDefinition createTesterAgent(String id, String name, String workDir) {
        return builder()
            .id(id)
            .name(name)
            .role("tester")
            .description("测试Agent，负责功能测试、性能测试、Bug报告")
            .workDir(workDir)
            .capability("testing")
            .capability("bug_reporting")
            .capability("test_automation")
            .supportedFileType("java")
            .supportedFileType("py")
            .supportedFileType("js")
            .tag("department", "qa")
            .tag("speciality", "testing")
            .maxContextSize(80000)
            .build();
    }

    /**
     * 设置项目 ID（链式调用）
     */
    public AgentDefinition withProjectId(String projectId) {
        this.projectId = projectId;
        if (projectId != null && role != null) {
            this.runtimeId = projectId + ":" + role;
        }
        return this;
    }
}
