package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 系统常量实体
 * 统一管理系统中的所有可配置常量
 *
 * @author chengxun
 * @since 2.0.0
 */
@Entity
@Table(name = "system_constants", indexes = {
    @Index(name = "idx_sc_key", columnList = "constantKey", unique = true),
    @Index(name = "idx_sc_group", columnList = "groupName")
})
public class SystemConstant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 常量标识（如 agent.max-message-backlog） */
    @Column(nullable = false, unique = true, length = 100)
    private String constantKey;

    /** 显示名称 */
    @Column(nullable = false, length = 100)
    private String displayName;

    /** 描述 */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** 当前值 */
    @Column(name = "\"value\"", nullable = false, length = 500)
    private String value;

    /** 默认值 */
    @Column(nullable = false, length = 500)
    private String defaultValue;

    /** 值类型：int, long, boolean, string */
    @Column(nullable = false, length = 20)
    private String valueType = "string";

    /** 分组：agent, file, security, rate-limit, performance, notification */
    @Column(nullable = false, length = 50)
    private String groupName;

    /** 单位（如 ms, MB, 次, 分钟） */
    @Column(length = 20)
    private String unit;

    /** 最小值（数值类型） */
    private Long minValue;

    /** 最大值（数值类型） */
    private Long maxValue;

    /** 是否需要重启生效 */
    @Column(nullable = false)
    private boolean requireRestart = false;

    /** 是否系统内置（不可删除） */
    @Column(nullable = false)
    private boolean systemBuiltin = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

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

    // ===== Getters & Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getConstantKey() { return constantKey; }
    public void setConstantKey(String constantKey) { this.constantKey = constantKey; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }

    public String getValueType() { return valueType; }
    public void setValueType(String valueType) { this.valueType = valueType; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Long getMinValue() { return minValue; }
    public void setMinValue(Long minValue) { this.minValue = minValue; }

    public Long getMaxValue() { return maxValue; }
    public void setMaxValue(Long maxValue) { this.maxValue = maxValue; }

    public boolean isRequireRestart() { return requireRestart; }
    public void setRequireRestart(boolean requireRestart) { this.requireRestart = requireRestart; }

    public boolean isSystemBuiltin() { return systemBuiltin; }
    public void setSystemBuiltin(boolean systemBuiltin) { this.systemBuiltin = systemBuiltin; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // ===== 辅助方法 =====

    public int getIntValue() {
        try { return Integer.parseInt(value); } catch (Exception e) { return 0; }
    }

    public long getLongValue() {
        try { return Long.parseLong(value); } catch (Exception e) { return 0; }
    }

    public boolean getBooleanValue() {
        return "true".equalsIgnoreCase(value);
    }
}
