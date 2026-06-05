package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 工作流审计日志持久化实体
 * 记录工作流所有关键操作的审计信息
 *
 * 审计的操作类型：
 * - 实例级: INSTANCE_CREATED/STARTED/PAUSED/RESUMED/COMPLETED/FAILED/CANCELLED
 * - 步骤级: STEP_STARTED/COMPLETED/FAILED/RETRIED/TIMEOUT
 * - 审批级: APPROVAL_REQUESTED/APPROVED/REJECTED
 *
 * @author chengxun
 * @since 2.0.0
 */
@Entity
@Table(name = "workflow_audit_logs", indexes = {
    @Index(name = "idx_wfal_instance", columnList = "instance_id"),
    @Index(name = "idx_wfal_action", columnList = "action"),
    @Index(name = "idx_wfal_created", columnList = "created_at")
})
public class WorkflowAuditLogEntity {

    /** 日志ID（主键，自增） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联的实例ID */
    @Column(name = "instance_id", length = 64, nullable = false)
    private String instanceId;

    /** 关联的步骤ID（可为空，表示实例级操作） */
    @Column(name = "step_id", length = 64)
    private String stepId;

    /** 操作类型 */
    @Column(name = "action", length = 50, nullable = false)
    private String action;

    /** 操作者类型: SYSTEM/USER/AGENT */
    @Column(name = "actor_type", length = 20, nullable = false)
    private String actorType = "SYSTEM";

    /** 操作者ID */
    @Column(name = "actor_id", length = 100)
    private String actorId;

    /** 操作者名称 */
    @Column(name = "actor_name", length = 100)
    private String actorName;

    /** 详情JSON */
    @Column(name = "detail_json", columnDefinition = "TEXT")
    private String detailJson;

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ===== Getters and Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }

    public String getStepId() { return stepId; }
    public void setStepId(String stepId) { this.stepId = stepId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getActorType() { return actorType; }
    public void setActorType(String actorType) { this.actorType = actorType; }

    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }

    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }

    public String getDetailJson() { return detailJson; }
    public void setDetailJson(String detailJson) { this.detailJson = detailJson; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
