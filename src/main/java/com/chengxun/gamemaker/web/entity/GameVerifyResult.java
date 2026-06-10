package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 游戏验证结果实体
 * 持久化存储项目的验证结果，支持重启后恢复
 *
 * @author chengxun
 * @since 2.0.0
 */
@Entity
@Table(name = "game_verify_results", indexes = {
    @Index(name = "idx_verify_project", columnList = "projectId"),
    @Index(name = "idx_verify_time", columnList = "verifiedAt")
})
public class GameVerifyResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 项目 ID */
    @Column(nullable = false, length = 100)
    private String projectId;

    /** 项目名称 */
    @Column(length = 200)
    private String projectName;

    /** 验证是否通过 */
    @Column(nullable = false)
    private boolean success;

    /** 验证成功时的消息 */
    @Column(columnDefinition = "TEXT")
    private String message;

    /** 验证失败时的错误信息 */
    @Column(columnDefinition = "TEXT")
    private String error;

    /** 警告列表（JSON 格式） */
    @Column(columnDefinition = "TEXT")
    private String warningsJson;

    /** 验证时间 */
    @Column(nullable = false)
    private LocalDateTime verifiedAt;

    /** 验证类型：QUICK(快速验证)、DEEP(深度分析) */
    @Column(length = 20)
    private String verifyType = "QUICK";

    /** 深度分析相关字段 */
    private Integer overallScore;
    private Integer runnableScore;
    private Integer playableScore;
    private Integer completenessScore;
    private Integer uiuxScore;
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
        if (verifiedAt == null) {
            verifiedAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getWarningsJson() { return warningsJson; }
    public void setWarningsJson(String warningsJson) { this.warningsJson = warningsJson; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public String getVerifyType() { return verifyType; }
    public void setVerifyType(String verifyType) { this.verifyType = verifyType; }

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
