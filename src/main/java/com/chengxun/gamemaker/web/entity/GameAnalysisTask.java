package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 游戏分析任务实体
 * 持久化存储深度分析任务，支持重启后恢复历史记录
 *
 * @author chengxun
 * @since 2.0.0
 */
@Entity
@Table(name = "game_analysis_tasks", indexes = {
    @Index(name = "idx_analysis_project", columnList = "projectId"),
    @Index(name = "idx_analysis_status", columnList = "status"),
    @Index(name = "idx_analysis_time", columnList = "createdAt")
})
public class GameAnalysisTask {

    @Id
    @Column(length = 50)
    private String taskId;

    /** 项目 ID */
    @Column(nullable = false, length = 100)
    private String projectId;

    /** 项目名称 */
    @Column(length = 200)
    private String projectName;

    /** 项目目录 */
    @Column(length = 500)
    private String projectDir;

    /** 项目目标 */
    @Column(columnDefinition = "TEXT")
    private String projectGoal;

    /** 请求者 */
    @Column(length = 100)
    private String requestedBy;

    /** 任务状态：PENDING, RUNNING, COMPLETED, FAILED */
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    /** 进度 0-100 */
    @Column(nullable = false)
    private int progress = 0;

    /** 创建时间 */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** 开始时间 */
    private LocalDateTime startedAt;

    /** 完成时间 */
    private LocalDateTime completedAt;

    /** 错误信息 */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /** 分析结果 - 综合评分 */
    private Integer overallScore;

    /** 分析结果 - 可运行性评分 */
    private Integer runnableScore;

    /** 分析结果 - 可玩性评分 */
    private Integer playableScore;

    /** 分析结果 - 完整性评分 */
    private Integer completenessScore;

    /** 分析结果 - UI/UX 评分 */
    private Integer uiuxScore;

    /** 分析结果 - 代码质量评分 */
    private Integer codeQualityScore;

    /** 分析摘要 */
    @Column(columnDefinition = "TEXT")
    private String summary;

    /** 优点（JSON 格式） */
    @Column(columnDefinition = "TEXT")
    private String strengthsJson;

    /** 问题（JSON 格式） */
    @Column(columnDefinition = "TEXT")
    private String issuesJson;

    /** 建议（JSON 格式） */
    @Column(columnDefinition = "TEXT")
    private String suggestionsJson;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getProjectDir() { return projectDir; }
    public void setProjectDir(String projectDir) { this.projectDir = projectDir; }

    public String getProjectGoal() { return projectGoal; }
    public void setProjectGoal(String projectGoal) { this.projectGoal = projectGoal; }

    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    /** 判断任务是否成功完成 */
    public boolean isSuccess() { return "COMPLETED".equals(status) && errorMessage == null; }

    public Integer getOverallScore() { return overallScore; }
    public void setOverallScore(Integer overallScore) { this.overallScore = overallScore; }

    public Integer getRunnableScore() { return runnableScore; }
    public void setRunnableScore(Integer runnableScore) { this.runnableScore = runnableScore; }

    public Integer getPlayableScore() { return playableScore; }
    public void setPlayableScore(Integer playableScore) { this.playableScore = playableScore; }

    public Integer getCompletenessScore() { return completenessScore; }
    public void setCompletenessScore(Integer completenessScore) { this.completenessScore = completenessScore; }

    public Integer getUiuxScore() { return uiuxScore; }
    public void setUiuxScore(Integer uiuxScore) { this.uiuxScore = uiuxScore; }

    public Integer getCodeQualityScore() { return codeQualityScore; }
    public void setCodeQualityScore(Integer codeQualityScore) { this.codeQualityScore = codeQualityScore; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getStrengthsJson() { return strengthsJson; }
    public void setStrengthsJson(String strengthsJson) { this.strengthsJson = strengthsJson; }

    public String getIssuesJson() { return issuesJson; }
    public void setIssuesJson(String issuesJson) { this.issuesJson = issuesJson; }

    public String getSuggestionsJson() { return suggestionsJson; }
    public void setSuggestionsJson(String suggestionsJson) { this.suggestionsJson = suggestionsJson; }
}
