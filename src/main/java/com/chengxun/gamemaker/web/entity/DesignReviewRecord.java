package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 设计审查记录实体
 * 保存游戏设计审查的历史记录
 *
 * @author chengxun
 * @since 3.0.0
 */
@Entity
@Table(name = "design_review_records", indexes = {
    @Index(name = "idx_drr_project", columnList = "project_id"),
    @Index(name = "idx_drr_time", columnList = "created_at")
})
public class DesignReviewRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private String projectId;

    @Column(name = "project_name", length = 200)
    private String projectName;

    /** 审查评分（0-100） */
    @Column(name = "score")
    private Integer score;

    /** 是否通过 */
    @Column(name = "passed")
    private Boolean passed;

    /** 审查摘要 */
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    /** 设计亮点（JSON数组） */
    @Column(name = "strengths", columnDefinition = "TEXT")
    private String strengths;

    /** 发现的问题（JSON数组） */
    @Column(name = "issues", columnDefinition = "TEXT")
    private String issues;

    /** 完整报告 */
    @Column(name = "report", columnDefinition = "TEXT")
    private String report;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public Boolean getPassed() { return passed; }
    public void setPassed(Boolean passed) { this.passed = passed; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getStrengths() { return strengths; }
    public void setStrengths(String strengths) { this.strengths = strengths; }
    public String getIssues() { return issues; }
    public void setIssues(String issues) { this.issues = issues; }
    public String getReport() { return report; }
    public void setReport(String report) { this.report = report; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
