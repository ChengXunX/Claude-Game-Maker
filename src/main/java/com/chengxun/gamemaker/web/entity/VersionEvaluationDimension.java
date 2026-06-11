package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 版本评估维度配置实体
 * 定义版本评估的维度、权重和评分标准
 *
 * @author chengxun
 * @since 2.0.0
 */
@Entity
@Table(name = "version_evaluation_dimensions", indexes = {
    @Index(name = "idx_ved_enabled", columnList = "enabled"),
    @Index(name = "idx_ved_order", columnList = "displayOrder")
})
public class VersionEvaluationDimension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 维度标识（如 functionality, code_quality, ux, performance） */
    @Column(nullable = false, unique = true, length = 50)
    private String dimensionKey;

    /** 显示名称 */
    @Column(nullable = false, length = 100)
    private String displayName;

    /** 维度描述 */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** 权重（1-100，所有维度权重之和应为100） */
    @Column(nullable = false)
    private int weight = 10;

    /** 评分标准说明 */
    @Column(columnDefinition = "TEXT")
    private String criteria;

    /** 评估提示词（用于AI评估） */
    @Column(columnDefinition = "TEXT")
    private String evaluationPrompt;

    /** 最低分数（低于此分数视为不通过） */
    @Column(nullable = false)
    private int minScore = 5;

    /** 是否启用 */
    @Column(nullable = false)
    private boolean enabled = true;

    /** 显示顺序 */
    @Column(nullable = false)
    private int displayOrder = 0;

    /** 是否系统内置（内置维度不可删除） */
    @Column(nullable = false)
    private boolean systemBuiltin = false;

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

    public VersionEvaluationDimension() {}

    public VersionEvaluationDimension(String dimensionKey, String displayName, int weight) {
        this.dimensionKey = dimensionKey;
        this.displayName = displayName;
        this.weight = weight;
    }

    // ===== Getters & Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDimensionKey() { return dimensionKey; }
    public void setDimensionKey(String dimensionKey) { this.dimensionKey = dimensionKey; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getWeight() { return weight; }
    public void setWeight(int weight) { this.weight = weight; }

    public String getCriteria() { return criteria; }
    public void setCriteria(String criteria) { this.criteria = criteria; }

    public String getEvaluationPrompt() { return evaluationPrompt; }
    public void setEvaluationPrompt(String evaluationPrompt) { this.evaluationPrompt = evaluationPrompt; }

    public int getMinScore() { return minScore; }
    public void setMinScore(int minScore) { this.minScore = minScore; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public boolean isSystemBuiltin() { return systemBuiltin; }
    public void setSystemBuiltin(boolean systemBuiltin) { this.systemBuiltin = systemBuiltin; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return String.format("VersionEvaluationDimension[%s] %s (weight=%d)", dimensionKey, displayName, weight);
    }
}
