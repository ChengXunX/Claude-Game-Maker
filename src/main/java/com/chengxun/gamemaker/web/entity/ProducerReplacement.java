package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 制作人更换记录实体
 * 记录管理员对制作人的解雇/更换操作
 *
 * 说明：
 * - 解雇制作人实际上是"更换"制作人
 * - 新制作人会继承项目，但可能有不同的规范
 * - 保留原制作人的历史记录
 * - 新制作人需要重新配置职能文档
 *
 * @author chengxun
 * @since 1.0.0
 */
@Entity
@Table(name = "producer_replacements", indexes = {
    @Index(name = "idx_replacement_project", columnList = "projectId"),
    @Index(name = "idx_replacement_old", columnList = "oldProducerId"),
    @Index(name = "idx_replacement_new", columnList = "newProducerId"),
    @Index(name = "idx_replacement_created", columnList = "createdAt")
})
public class ProducerReplacement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 更换编号 */
    @Column(name = "replacement_no", length = 50, unique = true)
    private String replacementNo;

    // ===== 原制作人信息 =====

    /** 原制作人Agent ID */
    @Column(name = "old_producer_id", length = 50, nullable = false)
    private String oldProducerId;

    /** 原制作人名称 */
    @Column(name = "old_producer_name", length = 100)
    private String oldProducerName;

    /** 原制作人创建时间 */
    @Column(name = "old_producer_created_at")
    private LocalDateTime oldProducerCreatedAt;

    /** 原制作人历史摘要 */
    @Column(name = "old_producer_history", columnDefinition = "TEXT")
    private String oldProducerHistory;

    // ===== 新制作人信息 =====

    /** 新制作人Agent ID */
    @Column(name = "new_producer_id", length = 50)
    private String newProducerId;

    /** 新制作人名称 */
    @Column(name = "new_producer_name", length = 100)
    private String newProducerName;

    // ===== 项目信息 =====

    /** 项目ID */
    @Column(name = "project_id", length = 100, nullable = false)
    private String projectId;

    /** 项目名称 */
    @Column(name = "project_name", length = 200)
    private String projectName;

    // ===== 更换原因 =====

    /** 更换原因类型 */
    @Column(name = "reason_type", length = 50)
    private String reasonType;

    /** 更换原因详情 */
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    /** 新规范/文档说明 */
    @Column(name = "new_guidelines", columnDefinition = "TEXT")
    private String newGuidelines;

    // ===== 审批信息 =====

    /** 操作人（管理员用户ID） */
    @Column(name = "admin_id")
    private Long adminId;

    /** 操作人名称 */
    @Column(name = "admin_name", length = 50)
    private String adminName;

    /** 操作时间 */
    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    /** 状态 */
    @Column(name = "status", length = 20)
    private String status = "COMPLETED";

    // ===== 时间戳 =====

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * 更换原因类型
     */
    public enum ReasonType {
        PERFORMANCE_ISSUE,    // 绩效问题
        MANAGEMENT_ISSUE,     // 管理问题
        PROJECT_NEEDS,        // 项目需求
        OPTIMIZATION,         // 优化升级
        POLICY_CHANGE,        // 政策变更
        OTHER                 // 其他
    }

    /**
     * 获取原因类型描述
     */
    public String getReasonTypeDescription() {
        return switch (reasonType) {
            case "PERFORMANCE_ISSUE" -> "绩效问题";
            case "MANAGEMENT_ISSUE" -> "管理问题";
            case "PROJECT_NEEDS" -> "项目需求";
            case "OPTIMIZATION" -> "优化升级";
            case "POLICY_CHANGE" -> "政策变更";
            case "OTHER" -> "其他";
            default -> reasonType;
        };
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReplacementNo() { return replacementNo; }
    public void setReplacementNo(String replacementNo) { this.replacementNo = replacementNo; }

    public String getOldProducerId() { return oldProducerId; }
    public void setOldProducerId(String oldProducerId) { this.oldProducerId = oldProducerId; }

    public String getOldProducerName() { return oldProducerName; }
    public void setOldProducerName(String oldProducerName) { this.oldProducerName = oldProducerName; }

    public LocalDateTime getOldProducerCreatedAt() { return oldProducerCreatedAt; }
    public void setOldProducerCreatedAt(LocalDateTime oldProducerCreatedAt) { this.oldProducerCreatedAt = oldProducerCreatedAt; }

    public String getOldProducerHistory() { return oldProducerHistory; }
    public void setOldProducerHistory(String oldProducerHistory) { this.oldProducerHistory = oldProducerHistory; }

    public String getNewProducerId() { return newProducerId; }
    public void setNewProducerId(String newProducerId) { this.newProducerId = newProducerId; }

    public String getNewProducerName() { return newProducerName; }
    public void setNewProducerName(String newProducerName) { this.newProducerName = newProducerName; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getReasonType() { return reasonType; }
    public void setReasonType(String reasonType) { this.reasonType = reasonType; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getNewGuidelines() { return newGuidelines; }
    public void setNewGuidelines(String newGuidelines) { this.newGuidelines = newGuidelines; }

    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }

    public String getAdminName() { return adminName; }
    public void setAdminName(String adminName) { this.adminName = adminName; }

    public LocalDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
