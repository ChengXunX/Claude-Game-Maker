package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 多轮推理记录实体
 * 记录 Agent 每次多轮推理（Think→Plan→Act→Verify）的完整过程
 *
 * @author chengxun
 * @since 3.0.0
 */
@Entity
@Table(name = "multi_turn_records", indexes = {
    @Index(name = "idx_mtr_agent", columnList = "agent_id"),
    @Index(name = "idx_mtr_project", columnList = "project_id"),
    @Index(name = "idx_mtr_status", columnList = "status"),
    @Index(name = "idx_mtr_time", columnList = "created_at")
})
public class MultiTurnRecord {

    public enum ReasoningStatus { THINKING, PLANNING, EXECUTING, VERIFYING, PASSED, FAILED, MAX_TURNS }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_id", nullable = false)
    private String agentId;

    @Column(name = "project_id")
    private String projectId;

    @Column(name = "task_id")
    private String taskId;

    @Column(name = "task_description", columnDefinition = "TEXT")
    private String taskDescription;

    /** 当前轮次（从1开始） */
    @Column(name = "turn_number")
    private Integer turnNumber = 1;

    /** 最大轮次 */
    @Column(name = "max_turns")
    private Integer maxTurns = 3;

    /** Think 阶段结果 */
    @Column(name = "think_result", columnDefinition = "TEXT")
    private String thinkResult;

    /** Plan 阶段结果 */
    @Column(name = "plan_result", columnDefinition = "TEXT")
    private String planResult;

    /** Act 阶段结果 */
    @Column(name = "act_result", columnDefinition = "TEXT")
    private String actResult;

    /** Verify 阶段结果 */
    @Column(name = "verify_result", columnDefinition = "TEXT")
    private String verifyResult;

    /** 验证是否通过 */
    @Column(name = "verify_passed")
    private Boolean verifyPassed;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private ReasoningStatus status = ReasoningStatus.THINKING;

    /** 总耗时（毫秒） */
    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getTaskDescription() { return taskDescription; }
    public void setTaskDescription(String taskDescription) { this.taskDescription = taskDescription; }
    public Integer getTurnNumber() { return turnNumber; }
    public void setTurnNumber(Integer turnNumber) { this.turnNumber = turnNumber; }
    public Integer getMaxTurns() { return maxTurns; }
    public void setMaxTurns(Integer maxTurns) { this.maxTurns = maxTurns; }
    public String getThinkResult() { return thinkResult; }
    public void setThinkResult(String thinkResult) { this.thinkResult = thinkResult; }
    public String getPlanResult() { return planResult; }
    public void setPlanResult(String planResult) { this.planResult = planResult; }
    public String getActResult() { return actResult; }
    public void setActResult(String actResult) { this.actResult = actResult; }
    public String getVerifyResult() { return verifyResult; }
    public void setVerifyResult(String verifyResult) { this.verifyResult = verifyResult; }
    public Boolean getVerifyPassed() { return verifyPassed; }
    public void setVerifyPassed(Boolean verifyPassed) { this.verifyPassed = verifyPassed; }
    public ReasoningStatus getStatus() { return status; }
    public void setStatus(ReasoningStatus status) { this.status = status; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
