package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 权限定义实体
 * 存储系统中所有可分配的权限定义
 * 管理员可以新增、编辑、禁用权限定义
 *
 * @author chengxun
 * @since 2.0.0
 */
@Entity
@Table(name = "permission_definitions", indexes = {
    @Index(name = "idx_pd_key", columnList = "permissionKey", unique = true),
    @Index(name = "idx_pd_category", columnList = "category"),
    @Index(name = "idx_pd_enabled", columnList = "enabled")
})
public class PermissionDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 权限标识（如 PERM_agents:manage） */
    @Column(nullable = false, unique = true, length = 100)
    private String permissionKey;

    /** 权限名称（中文） */
    @Column(nullable = false, length = 100)
    private String name;

    /** 权限描述 */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** 权限分类 */
    @Column(nullable = false, length = 50)
    private String category;

    /** 是否启用（禁用后不可分配） */
    @Column(nullable = false)
    private boolean enabled = true;

    /** 是否系统内置（内置权限不可删除） */
    @Column(name = "\"system\"", nullable = false)
    private boolean system = false;

    /** 排序顺序 */
    @Column(nullable = false)
    private int sortOrder = 0;

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

    // ===== Getters & Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPermissionKey() { return permissionKey; }
    public void setPermissionKey(String permissionKey) { this.permissionKey = permissionKey; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isSystem() { return system; }
    public void setSystem(boolean system) { this.system = system; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return String.format("PermissionDefinition[%s] %s (%s)", permissionKey, name, category);
    }
}
