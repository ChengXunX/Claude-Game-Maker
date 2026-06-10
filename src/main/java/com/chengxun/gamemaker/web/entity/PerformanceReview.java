package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 绩效评审实体
 * 记录制作人对团队成员的绩效打分
 *
 * 评分维度：
 * - 任务完成质量 (qualityScore)
 * - 工作效率 (efficiencyScore)
 * - 协作能力 (collaborationScore)
 * - 创新能力 (innovationScore)
 * - 综合评分 (overallScore)
 *
 * @author chengxun
 * @since 1.0.0
 */
@Entity
@Table(name = "performance_reviews", indexes = {
    @Index(name = "idx_review_agent", columnList = "agentId"),
    @Index(name = "idx_review_producer", columnList = "producerId"),
    @Index(name = "idx_review_project", columnList = "projectId"),
    @Index(name = "idx_review_period", columnList = "reviewPeriod"),
    @Index(name = "idx_review_created", columnList = "createdAt")
})
public class PerformanceReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 评审编号 */
    @Column(name = "review_no", length = 50, unique = true)
    private String reviewNo;

    /** 被评审Agent ID */
    @Column(name = "agent_id", length = 50, nullable = false)
    private String agentId;

    /** 被评审Agent名称 */
    @Column(name = "agent_name", length = 100)
    private String agentName;

    /** 被评审Agent角色 */
    @Column(name = "agent_role", length = 50)
    private String agentRole;

    /** 评审人（制作人Agent ID） */
    @Column(name = "producer_id", length = 50, nullable = false)
    private String producerId;

    /** 评审人名称 */
    @Column(name = "producer_name", length = 100)
    private String producerName;

    /** 项目ID */
    @Column(name = "project_id", length = 100)
    private String projectId;

    /** 项目名称 */
    @Column(name = "project_name", length = 200)
    private String projectName;

    /** 评审周期（如：2026-Q1, 2026-05） */
    @NotBlank(message = "reviewPeriod 不能为空")
    @Column(name = "review_period", length = 20, nullable = false)
    private String reviewPeriod;

    // ===== 评分维度 =====

    /** 任务完成质量 (0-100) */
    @Column(name = "quality_score")
    private Integer qualityScore;

    /** 工作效率 (0-100) */
    @Column(name = "efficiency_score")
    private Integer efficiencyScore;

    /** 协作能力 (0-100) */
    @Column(name = "collaboration_score")
    private Integer collaborationScore;

    /** 创新能力 (0-100) */
    @Column(name = "innovation_score")
    private Integer innovationScore;

    /** 综合评分 (0-100) */
    @Column(name = "overall_score")
    private Integer overallScore;

    /** 角色权重配置（用于自定义角色，格式：quality:1.4,efficiency:1.2,collaboration:1.0,innovation:0.8） */
    @Column(name = "role_weights", length = 200)
    private String roleWeights;

    // ===== 评价内容 =====

    /** 优点 */
    @Column(name = "strengths", columnDefinition = "TEXT")
    private String strengths;

    /** 待改进 */
    @Column(name = "improvements", columnDefinition = "TEXT")
    private String improvements;

    /** 具体评价 */
    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    /** 工作亮点 */
    @Column(name = "highlights", columnDefinition = "TEXT")
    private String highlights;

    // ===== 状态 =====

    /** 评审状态 */
    @Column(name = "status", length = 20)
    private String status = "COMPLETED";

    /** 是否为警告评审 */
    @Column(name = "is_warning")
    private Boolean isWarning = false;

    /** 警告原因 */
    @Column(name = "warning_reason", columnDefinition = "TEXT")
    private String warningReason;

    // ===== 时间戳 =====

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * 评审状态枚举
     */
    public enum Status {
        DRAFT,      // 草稿
        COMPLETED,  // 已完成
        DISPUTED    // 有异议
    }

    /**
     * 计算综合评分（支持角色差异化权重）
     *
     * 不同角色的权重配置：
     * - 开发类（server-dev, client-dev, ui-dev）：质量权重高
     * - 策划类（system-planner, numerical-planner）：创新权重高
     * - 测试类（tester）：质量权重最高
     * - 其他：平均权重
     */
    public void calculateOverallScore() {
        // 获取角色权重
        double[] weights = getRoleWeights(agentRole);

        double weightedSum = 0;
        double totalWeight = 0;

        if (qualityScore != null) {
            weightedSum += qualityScore * weights[0];
            totalWeight += weights[0];
        }
        if (efficiencyScore != null) {
            weightedSum += efficiencyScore * weights[1];
            totalWeight += weights[1];
        }
        if (collaborationScore != null) {
            weightedSum += collaborationScore * weights[2];
            totalWeight += weights[2];
        }
        if (innovationScore != null) {
            weightedSum += innovationScore * weights[3];
            totalWeight += weights[3];
        }

        if (totalWeight > 0) {
            this.overallScore = (int) Math.round(weightedSum / totalWeight);
        } else {
            this.overallScore = 0;
        }
    }

    /**
     * 角色权重缓存（支持自定义角色配置）
     * key: 角色名称
     * value: [质量权重, 效率权重, 协作权重, 创新权重]
     */
    private static final java.util.Map<String, double[]> ROLE_WEIGHTS = new java.util.HashMap<>();

    // 预设角色权重
    static {
        // 开发类：质量优先
        ROLE_WEIGHTS.put("server-dev", new double[]{1.4, 1.2, 1.0, 0.8});
        ROLE_WEIGHTS.put("client-dev", new double[]{1.4, 1.2, 1.0, 0.8});
        ROLE_WEIGHTS.put("ui-dev", new double[]{1.4, 1.2, 1.0, 0.8});
        // 策划类：创新优先
        ROLE_WEIGHTS.put("system-planner", new double[]{1.0, 0.8, 1.2, 1.4});
        ROLE_WEIGHTS.put("numerical-planner", new double[]{1.0, 0.8, 1.2, 1.4});
        // 测试类：质量最高
        ROLE_WEIGHTS.put("tester", new double[]{1.6, 1.0, 1.0, 0.6});
        // Git专员：效率优先
        ROLE_WEIGHTS.put("git-commit", new double[]{1.0, 1.4, 1.0, 0.8});
        // 制作人：均衡
        ROLE_WEIGHTS.put("producer", new double[]{1.0, 1.0, 1.2, 1.0});
    }

    /**
     * 获取角色权重配置
     * 返回数组：[质量权重, 效率权重, 协作权重, 创新权重]
     *
     * 优先级：
     * 1. 自定义配置（从数据库/配置文件加载）
     * 2. 预设角色权重
     * 3. 默认平均权重
     */
    private double[] getRoleWeights(String role) {
        if (role == null) return new double[]{1.0, 1.0, 1.0, 1.0};

        // 先查预设权重
        double[] weights = ROLE_WEIGHTS.get(role);
        if (weights != null) {
            return weights;
        }

        // 自定义角色使用默认平均权重
        // 可以通过 roleWeights 字段覆盖
        if (this.roleWeights != null && !this.roleWeights.isEmpty()) {
            return parseRoleWeights(this.roleWeights);
        }

        // 默认平均权重
        return new double[]{1.0, 1.0, 1.0, 1.0};
    }

    /**
     * 解析权重配置字符串
     * 格式：quality:1.4,efficiency:1.2,collaboration:1.0,innovation:0.8
     */
    private double[] parseRoleWeights(String weightsStr) {
        double[] weights = new double[]{1.0, 1.0, 1.0, 1.0};
        try {
            String[] parts = weightsStr.split(",");
            for (String part : parts) {
                String[] kv = part.split(":");
                if (kv.length == 2) {
                    String key = kv[0].trim();
                    double value = Double.parseDouble(kv[1].trim());
                    switch (key) {
                        case "quality" -> weights[0] = value;
                        case "efficiency" -> weights[1] = value;
                        case "collaboration" -> weights[2] = value;
                        case "innovation" -> weights[3] = value;
                    }
                }
            }
        } catch (Exception e) {
            // 解析失败使用默认权重
        }
        return weights;
    }

    /**
     * 动态添加角色权重配置
     * 用于自定义角色注册时设置权重
     *
     * @param role 角色名称
     * @param qualityWeight 质量权重
     * @param efficiencyWeight 效率权重
     * @param collaborationWeight 协作权重
     * @param innovationWeight 创新权重
     */
    public static void registerRoleWeights(String role, double qualityWeight, double efficiencyWeight,
                                            double collaborationWeight, double innovationWeight) {
        ROLE_WEIGHTS.put(role, new double[]{qualityWeight, efficiencyWeight, collaborationWeight, innovationWeight});
    }

    /**
     * 获取所有已配置的角色权重
     */
    public static java.util.Map<String, double[]> getAllRoleWeights() {
        return new java.util.HashMap<>(ROLE_WEIGHTS);
    }

    /**
     * 获取评分等级
     */
    public String getGrade() {
        if (overallScore == null) return "N/A";
        if (overallScore >= 90) return "A";
        if (overallScore >= 80) return "B";
        if (overallScore >= 70) return "C";
        if (overallScore >= 60) return "D";
        return "F";
    }

    /**
     * 是否低分（低于60分）
     */
    public boolean isLowScore() {
        return overallScore != null && overallScore < 60;
    }

    /**
     * 是否需要警告（低于50分）
     */
    public boolean needsWarning() {
        return overallScore != null && overallScore < 50;
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReviewNo() { return reviewNo; }
    public void setReviewNo(String reviewNo) { this.reviewNo = reviewNo; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }

    public String getAgentRole() { return agentRole; }
    public void setAgentRole(String agentRole) { this.agentRole = agentRole; }

    public String getProducerId() { return producerId; }
    public void setProducerId(String producerId) { this.producerId = producerId; }

    public String getProducerName() { return producerName; }
    public void setProducerName(String producerName) { this.producerName = producerName; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getReviewPeriod() { return reviewPeriod; }
    public void setReviewPeriod(String reviewPeriod) { this.reviewPeriod = reviewPeriod; }

    public Integer getQualityScore() { return qualityScore; }
    public void setQualityScore(Integer qualityScore) { this.qualityScore = qualityScore; }

    public Integer getEfficiencyScore() { return efficiencyScore; }
    public void setEfficiencyScore(Integer efficiencyScore) { this.efficiencyScore = efficiencyScore; }

    public Integer getCollaborationScore() { return collaborationScore; }
    public void setCollaborationScore(Integer collaborationScore) { this.collaborationScore = collaborationScore; }

    public Integer getInnovationScore() { return innovationScore; }
    public void setInnovationScore(Integer innovationScore) { this.innovationScore = innovationScore; }

    public Integer getOverallScore() { return overallScore; }
    public void setOverallScore(Integer overallScore) { this.overallScore = overallScore; }

    public String getRoleWeights() { return roleWeights; }
    public void setRoleWeights(String roleWeights) { this.roleWeights = roleWeights; }

    public String getStrengths() { return strengths; }
    public void setStrengths(String strengths) { this.strengths = strengths; }

    public String getImprovements() { return improvements; }
    public void setImprovements(String improvements) { this.improvements = improvements; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public String getHighlights() { return highlights; }
    public void setHighlights(String highlights) { this.highlights = highlights; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getIsWarning() { return isWarning; }
    public void setIsWarning(Boolean isWarning) { this.isWarning = isWarning; }

    public String getWarningReason() { return warningReason; }
    public void setWarningReason(String warningReason) { this.warningReason = warningReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
