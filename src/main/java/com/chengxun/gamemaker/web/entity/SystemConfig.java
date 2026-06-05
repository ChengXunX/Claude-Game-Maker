package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 系统配置实体
 * 用于存储可动态配置的系统参数，避免硬编码常量
 *
 * 分层设计：
 * - projectId = null：全局默认配置
 * - projectId != null：项目级覆盖配置
 * - 查询时先查项目配置，没有则回退到全局配置
 */
@Entity
@Table(name = "system_configs", indexes = {
    @Index(name = "idx_config_project", columnList = "project_id")
})
public class SystemConfig {

    /** 配置分组：安全相关 */
    public static final String GROUP_SECURITY = "security";
    /** 配置分组：Agent相关 */
    public static final String GROUP_AGENT = "agent";
    /** 配置分组：邮件相关 */
    public static final String GROUP_EMAIL = "email";
    /** 配置分组：通知相关 */
    public static final String GROUP_NOTIFICATION = "notification";
    /** 配置分组：系统通用 */
    public static final String GROUP_SYSTEM = "system";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属项目 ID（null 表示全局默认配置） */
    @Column(name = "project_id", length = 100)
    private String projectId;

    /** 配置键 */
    @NotBlank(message = "configKey 不能为空")
    @Column(name = "config_key", nullable = false, length = 100)
    private String configKey;

    /** 配置值 */
    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;

    /** 配置描述 */
    @Column(length = 500)
    private String description;

    /** 配置分组 */
    @Column(name = "config_group", length = 50)
    private String group;

    /** 数据类型：string, number, boolean, json */
    @Column(name = "value_type", length = 20)
    private String valueType = "string";

    /** 是否系统内置（内置配置不可删除） */
    @Column(name = "is_system_builtin")
    private boolean systemBuiltin = false;

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

    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }

    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }

    public String getValueType() { return valueType; }
    public void setValueType(String valueType) { this.valueType = valueType; }

    public boolean isSystemBuiltin() { return systemBuiltin; }
    public void setSystemBuiltin(boolean systemBuiltin) { this.systemBuiltin = systemBuiltin; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    /**
     * 获取整数值
     */
    public int getIntValue(int defaultValue) {
        try {
            return Integer.parseInt(configValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 获取长整数值
     */
    public long getLongValue(long defaultValue) {
        try {
            return Long.parseLong(configValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 获取布尔值
     */
    public boolean getBooleanValue(boolean defaultValue) {
        if (configValue == null) return defaultValue;
        return Boolean.parseBoolean(configValue);
    }
}
