package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 迭代适应记录实体
 * 记录项目的迭代策略调整历史
 *
 * @author chengxun
 * @since 3.0.0
 */
@Entity
@Table(name = "iteration_adapt_records", indexes = {
    @Index(name = "idx_iar_project", columnList = "project_id"),
    @Index(name = "idx_iar_phase", columnList = "phase"),
    @Index(name = "idx_iar_time", columnList = "created_at")
})
public class IterationAdaptRecord {

    /** 项目阶段 */
    public enum ProjectPhase {
        /** 早期：快速验证核心玩法 */
        EARLY,
        /** 中期：功能完善和优化 */
        MID,
        /** 后期：稳定性和质量 */
        LATE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private String projectId;

    /** 项目阶段 */
    @Enumerated(EnumType.STRING)
    @Column(name = "phase", length = 20)
    private ProjectPhase phase;

    /** 版本号 */
    @Column(name = "version", length = 50)
    private String version;

    /** 版本迭代次数 */
    @Column(name = "iteration_count")
    private Integer iterationCount;

    /** 旧策略 */
    @Column(name = "old_strategy", length = 50)
    private String oldStrategy;

    /** 新策略 */
    @Column(name = "new_strategy", length = 50)
    private String newStrategy;

    /** 旧通过分数 */
    @Column(name = "old_pass_score")
    private Integer oldPassScore;

    /** 新通过分数 */
    @Column(name = "new_pass_score")
    private Integer newPassScore;

    /** 调整原因 */
    @Column(name = "reason", length = 500)
    private String reason;

    /** 是否已应用 */
    @Column(name = "applied")
    private Boolean applied = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public ProjectPhase getPhase() { return phase; }
    public void setPhase(ProjectPhase phase) { this.phase = phase; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public Integer getIterationCount() { return iterationCount; }
    public void setIterationCount(Integer iterationCount) { this.iterationCount = iterationCount; }
    public String getOldStrategy() { return oldStrategy; }
    public void setOldStrategy(String oldStrategy) { this.oldStrategy = oldStrategy; }
    public String getNewStrategy() { return newStrategy; }
    public void setNewStrategy(String newStrategy) { this.newStrategy = newStrategy; }
    public Integer getOldPassScore() { return oldPassScore; }
    public void setOldPassScore(Integer oldPassScore) { this.oldPassScore = oldPassScore; }
    public Integer getNewPassScore() { return newPassScore; }
    public void setNewPassScore(Integer newPassScore) { this.newPassScore = newPassScore; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Boolean getApplied() { return applied; }
    public void setApplied(Boolean applied) { this.applied = applied; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
