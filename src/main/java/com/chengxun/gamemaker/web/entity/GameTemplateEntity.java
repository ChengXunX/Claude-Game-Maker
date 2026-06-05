package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 游戏模板持久化实体
 * 用于存储用户自定义的游戏模板，内置模板仍从代码加载
 *
 * 主要功能：
 * - 持久化用户通过API创建的游戏模板
 * - 支持JSON格式存储模板配置
 * - 区分内置模板和用户自定义模板
 *
 * @author chengxun
 * @since 1.0.0
 */
@Entity
@Table(name = "game_templates", indexes = {
    @Index(name = "idx_game_tpl_builtin", columnList = "builtin")
})
public class GameTemplateEntity {

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

    /** 游戏类型 */
    @Column(name = "game_type", length = 64)
    private String gameType;

    /** 模板配置JSON，包含技术栈、目录结构、Agent配置等 */
    @Column(name = "config_json", columnDefinition = "TEXT")
    private String configJson;

    /** 是否为内置模板 */
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

    public String getGameType() { return gameType; }
    public void setGameType(String gameType) { this.gameType = gameType; }

    public String getConfigJson() { return configJson; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }

    public boolean isBuiltin() { return builtin; }
    public void setBuiltin(boolean builtin) { this.builtin = builtin; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
