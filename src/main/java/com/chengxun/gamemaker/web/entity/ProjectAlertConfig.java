package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 项目告警配置实体
 * 定义项目级别的告警规则定制
 *
 * 设计原则：
 * - 告警规则全局定义（AlertRule）
 * - 项目可以启用/禁用特定规则
 * - 项目可以覆盖阈值和优先级
 * - 没有配置的项目使用全局规则默认值
 *
 * @author chengxun
 * @since 1.0.0
 */
@Entity
@Table(name = "project_alert_config", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"project_id", "rule_id"})
})
public class ProjectAlertConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 项目 ID */
    @NotBlank(message = "项目ID不能为空")
    @Column(name = "project_id", nullable = false, length = 100)
    private String projectId;

    /** 告警规则 ID（关联 AlertRule.id） */
    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    /** 是否在该项目中启用该规则 */
    @Column(name = "enabled")
    private boolean enabled = true;

    /** 自定义阈值覆盖（为空则使用全局规则阈值） */
    @Column(name = "custom_threshold")
    private Double customThreshold;

    /** 自定义优先级覆盖（为空则使用全局规则优先级） */
    @Column(name = "custom_priority", length = 20)
    private String customPriority;

    /** 自定义持续时间覆盖（秒） */
    @Column(name = "custom_duration_seconds")
    private Integer customDurationSeconds;

    /** 备注 */
    @Column(length = 500)
    private String remark;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Double getCustomThreshold() { return customThreshold; }
    public void setCustomThreshold(Double customThreshold) { this.customThreshold = customThreshold; }

    public String getCustomPriority() { return customPriority; }
    public void setCustomPriority(String customPriority) { this.customPriority = customPriority; }

    public Integer getCustomDurationSeconds() { return customDurationSeconds; }
    public void setCustomDurationSeconds(Integer customDurationSeconds) { this.customDurationSeconds = customDurationSeconds; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
