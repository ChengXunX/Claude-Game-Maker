package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Agent 能力定义实体
 * 定义每个 Agent 角色拥有的可调用能力
 *
 * 能力系统的核心数据模型：
 * - 每个 agentRole 有一组预定义的能力
 * - 能力可以按 projectId 覆盖（项目级定制）
 * - 能力可以动态启用/禁用、配置审批策略
 * - 能力的 paramSchema 定义了参数格式，供 AI 输出解析和验证使用
 *
 * @author chengxun
 * @since 2.0.0
 */
@Entity
@Table(name = "agent_capabilities", indexes = {
    @Index(name = "idx_cap_role", columnList = "agentRole"),
    @Index(name = "idx_cap_role_project", columnList = "agentRole, projectId"),
    @Index(name = "idx_cap_name_role", columnList = "capabilityName, agentRole"),
    @Index(name = "idx_cap_enabled", columnList = "enabled")
})
public class AgentCapability {

    /** 能力 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Agent 角色：producer, server-dev, system-planner, numerical-planner, git-commit, ui-dev */
    @Column(nullable = false, length = 50)
    private String agentRole;

    /** 能力唯一标识：createAgent, assignApiConfig, sendTaskToAgent 等 */
    @Column(nullable = false, length = 100)
    private String capabilityName;

    /** 中文显示名称 */
    @Column(length = 100)
    private String displayName;

    /** 能力描述（会注入到 Claude 的 prompt 中） */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** 参数 Schema（JSON 格式），定义参数名、类型、是否必填 */
    @Column(columnDefinition = "TEXT")
    private String paramSchema;

    /** 对应的 Java 方法名（用于反射调用或执行器分派） */
    @Column(length = 100)
    private String methodName;

    /** 是否需要审批 */
    @Column(nullable = false)
    private boolean requiresApproval = false;

    /** 审批类型（如 CREATE_AGENT, ASSIGN_API 等，对应 ApprovalRequest.requestType） */
    @Column(length = 50)
    private String approvalType;

    /**
     * 执行方式：
     * - java: 使用注册的 Java 执行器（默认）
     * - prompt: 使用 promptTemplate 调用 Claude（纯 AI 能力）
     * - message: 向目标 Agent 发送消息
     */
    @Column(length = 20, nullable = false)
    private String executionType = "java";

    /**
     * Prompt 模板（executionType=prompt 时使用）
     * 支持参数占位符：{paramName} 会被替换为实际参数值
     * 示例：请为以下系统创建 UI 设计方案：{systemName}，需求：{requirements}
     */
    @Column(columnDefinition = "TEXT")
    private String promptTemplate;

    /**
     * 目标 Agent 角色（executionType=message 时使用）
     * 消息会发送给该项目下的指定角色 Agent
     */
    @Column(length = 50)
    private String targetAgentRole;

    /** 是否启用（可热关闭某个能力） */
    @Column(nullable = false)
    private boolean enabled = true;

    /** 优先级（数字越小越靠前，在 prompt 中的展示顺序） */
    @Column(nullable = false)
    private int priority = 5;

    /** 能力分类：agent_management, api_config, task, communication, monitoring, project */
    @Column(length = 50)
    private String category;

    /**
     * 项目 ID（null 表示全局默认，非 null 表示项目级覆盖）
     * 项目级能力会覆盖同名的全局默认能力
     */
    @Column(length = 100)
    private String projectId;

    /** 失败重试次数（0 表示不重试） */
    @Column(nullable = false)
    private int maxRetry = 0;

    /** 调用冷却时间（秒，0 表示无冷却） */
    @Column(nullable = false)
    private int cooldownSeconds = 0;

    /** 能力执行超时时间（秒，0 表示使用默认值 30 秒） */
    @Column(nullable = false)
    private int timeoutSeconds = 0;

    /**
     * 关联的技能 ID 列表（逗号分隔）
     * 执行能力时，这些技能的 prompt 会自动注入到 AI 的上下文中
     * 示例: "git-commit-standard,code-review-checklist"
     */
    @Column(columnDefinition = "TEXT")
    private String relatedSkillIds;

    /** 创建时间 */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ===== 构造函数 =====

    public AgentCapability() {}

    public AgentCapability(String agentRole, String capabilityName, String displayName,
                           String description, String category) {
        this.agentRole = agentRole;
        this.capabilityName = capabilityName;
        this.displayName = displayName;
        this.description = description;
        this.category = category;
    }

    // ===== Getters & Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAgentRole() { return agentRole; }
    public void setAgentRole(String agentRole) { this.agentRole = agentRole; }

    public String getCapabilityName() { return capabilityName; }
    public void setCapabilityName(String capabilityName) { this.capabilityName = capabilityName; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getParamSchema() { return paramSchema; }
    public void setParamSchema(String paramSchema) { this.paramSchema = paramSchema; }

    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }

    public boolean isRequiresApproval() { return requiresApproval; }
    public void setRequiresApproval(boolean requiresApproval) { this.requiresApproval = requiresApproval; }

    public String getApprovalType() { return approvalType; }
    public void setApprovalType(String approvalType) { this.approvalType = approvalType; }

    public String getExecutionType() { return executionType; }
    public void setExecutionType(String executionType) { this.executionType = executionType; }

    public String getPromptTemplate() { return promptTemplate; }
    public void setPromptTemplate(String promptTemplate) { this.promptTemplate = promptTemplate; }

    public String getTargetAgentRole() { return targetAgentRole; }
    public void setTargetAgentRole(String targetAgentRole) { this.targetAgentRole = targetAgentRole; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public int getMaxRetry() { return maxRetry; }
    public void setMaxRetry(int maxRetry) { this.maxRetry = maxRetry; }

    public int getCooldownSeconds() { return cooldownSeconds; }
    public void setCooldownSeconds(int cooldownSeconds) { this.cooldownSeconds = cooldownSeconds; }

    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

    public String getRelatedSkillIds() { return relatedSkillIds; }
    public void setRelatedSkillIds(String relatedSkillIds) { this.relatedSkillIds = relatedSkillIds; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // ===== 辅助方法 =====

    /**
     * 是否为全局默认能力（projectId 为空）
     */
    public boolean isGlobal() {
        return projectId == null || projectId.isEmpty();
    }

    /**
     * 是否为项目级覆盖能力
     */
    public boolean isProjectOverride() {
        return projectId != null && !projectId.isEmpty();
    }

    /**
     * 获取超时时间（如果未配置则返回默认 30 秒）
     */
    public int getEffectiveTimeoutSeconds() {
        return timeoutSeconds > 0 ? timeoutSeconds : 30;
    }

    @Override
    public String toString() {
        return String.format("AgentCapability[%s:%s] enabled=%s, approval=%s",
            agentRole, capabilityName, enabled, requiresApproval);
    }
}
