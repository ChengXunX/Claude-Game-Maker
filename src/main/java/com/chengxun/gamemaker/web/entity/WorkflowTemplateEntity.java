package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 工作流模板持久化实体
 * 用于存储用户自定义的工作流模板，内置模板仍从代码加载
 *
 * 主要功能：
 * - 持久化用户通过API创建的工作流模板
 * - 支持JSON格式存储步骤定义
 * - 区分内置模板和用户自定义模板
 *
 * @author chengxun
 * @since 1.0.0
 */
@Entity
@Table(name = "workflow_templates", indexes = {
    @Index(name = "idx_wf_tpl_builtin", columnList = "builtin")
})
public class WorkflowTemplateEntity {

    /** 模板ID（主键） */
    @Id
    @Column(name = "id", length = 64)
    private String id;

    /** 模板名称 */
    @Column(name = "name", length = 128, nullable = false)
    private String name;

    /** 模板描述 */
    @Column(name = "description", length = 512)
    private String description;

    /** 步骤定义JSON，格式：[{"id":"step-1","name":"步骤名","agentRole":"角色","taskDescription":"描述","dependencies":[],"parallel":false,"requiresApproval":false}] */
    @Column(name = "steps_json", columnDefinition = "TEXT")
    private String stepsJson;

    /** 是否为内置模板（内置模板不显示此记录，由代码加载） */
    @Column(name = "builtin")
    private boolean builtin = false;

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Column(name = "updated_at")
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

    // ===== Getters and Setters =====

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStepsJson() { return stepsJson; }
    public void setStepsJson(String stepsJson) { this.stepsJson = stepsJson; }

    public boolean isBuiltin() { return builtin; }
    public void setBuiltin(boolean builtin) { this.builtin = builtin; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
