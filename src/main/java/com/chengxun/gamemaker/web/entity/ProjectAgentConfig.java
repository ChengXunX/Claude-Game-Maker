package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 项目级Agent配置实体
 * 存储每个项目中Agent的自定义配置，包括提示词、权重等
 *
 * 设计思路：
 * - 每个项目中的Agent可以有自己的提示词配置
 * - 支持AI优化提示词
 * - 记录优化历史，便于回滚和学习
 *
 * @author chengxun
 * @since 2.0.0
 */
@Entity
@Table(name = "project_agent_configs", indexes = {
    @Index(name = "idx_pac_project", columnList = "projectId"),
    @Index(name = "idx_pac_agent", columnList = "projectId, agentRole"),
    @Index(name = "idx_pac_active", columnList = "projectId, agentRole, isActive")
})
public class ProjectAgentConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 项目ID */
    @Column(name = "project_id", nullable = false, length = 100)
    private String projectId;

    /** Agent角色 */
    @Column(name = "agent_role", nullable = false, length = 50)
    private String agentRole;

    /** 自定义系统提示词（追加到默认提示词之后） */
    @Column(name = "custom_system_prompt", columnDefinition = "TEXT")
    private String customSystemPrompt;

    /** 自定义能力提示词（描述该Agent在项目中的具体职责） */
    @Column(name = "custom_capability_prompt", columnDefinition = "TEXT")
    private String customCapabilityPrompt;

    /** 职责权重（JSON格式，描述各职责的重要程度） */
    @Column(name = "responsibility_weights", columnDefinition = "TEXT")
    private String responsibilityWeights;

    /** 绩效评分权重（JSON格式，如 {"quality":1.4,"efficiency":1.2,"collaboration":1.0,"innovation":0.8}） */
    @Column(name = "performance_weights", length = 300)
    private String performanceWeights;

    /** 项目特定上下文（JSON格式，存储项目相关信息） */
    @Column(name = "project_context", columnDefinition = "TEXT")
    private String projectContext;

    /** 优化建议（AI生成的优化建议） */
    @Column(name = "optimization_suggestions", columnDefinition = "TEXT")
    private String optimizationSuggestions;

    /** 是否启用自定义配置 */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    /** 版本号（用于配置版本管理） */
    @Column(name = "version", nullable = false)
    private int version = 1;

    /** 创建时间 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Column(name = "updated_at", nullable = false)
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

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getAgentRole() { return agentRole; }
    public void setAgentRole(String agentRole) { this.agentRole = agentRole; }

    public String getCustomSystemPrompt() { return customSystemPrompt; }
    public void setCustomSystemPrompt(String customSystemPrompt) { this.customSystemPrompt = customSystemPrompt; }

    public String getCustomCapabilityPrompt() { return customCapabilityPrompt; }
    public void setCustomCapabilityPrompt(String customCapabilityPrompt) { this.customCapabilityPrompt = customCapabilityPrompt; }

    public String getResponsibilityWeights() { return responsibilityWeights; }
    public void setResponsibilityWeights(String responsibilityWeights) { this.responsibilityWeights = responsibilityWeights; }

    public String getPerformanceWeights() { return performanceWeights; }
    public void setPerformanceWeights(String performanceWeights) { this.performanceWeights = performanceWeights; }

    public String getProjectContext() { return projectContext; }
    public void setProjectContext(String projectContext) { this.projectContext = projectContext; }

    public String getOptimizationSuggestions() { return optimizationSuggestions; }
    public void setOptimizationSuggestions(String optimizationSuggestions) { this.optimizationSuggestions = optimizationSuggestions; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
