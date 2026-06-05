package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 项目通知配置实体
 * 定义项目级别的通知目标和渠道选择
 *
 * 设计原则：
 * - 通知模板本身全局统一（NotificationTemplate）
 * - 但给谁通知、通过什么渠道通知可以按项目定制
 * - 项目可以启用/禁用特定模板，自定义通知目标
 *
 * @author chengxun
 * @since 1.0.0
 */
@Entity
@Table(name = "project_notification_config", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"project_id", "template_code"})
})
public class ProjectNotificationConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 项目 ID */
    @NotBlank(message = "项目ID不能为空")
    @Column(name = "project_id", nullable = false, length = 100)
    private String projectId;

    /** 关联的模板编码（对应 NotificationTemplate.templateCode） */
    @NotBlank(message = "模板编码不能为空")
    @Column(name = "template_code", nullable = false, length = 50)
    private String templateCode;

    /** 是否启用该通知 */
    @Column(name = "enabled")
    private boolean enabled = true;

    /** 通知渠道覆盖（为空则使用模板默认渠道） */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel_override", length = 20)
    private NotificationTemplate.Channel channelOverride;

    /** 自定义通知目标（JSON 数组，如飞书群 ID、邮件地址列表） */
    @Column(name = "recipients", columnDefinition = "TEXT")
    private String recipients;

    /** 自定义通知人（JSON 数组，用户 ID 列表） */
    @Column(name = "notify_users", columnDefinition = "TEXT")
    private String notifyUsers;

    /** 通知频率限制（秒，0 表示不限制） */
    @Column(name = "rate_limit_seconds")
    private Integer rateLimitSeconds = 0;

    /** 备注 */
    @Column(length = 500)
    private String remark;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public NotificationTemplate.Channel getChannelOverride() { return channelOverride; }
    public void setChannelOverride(NotificationTemplate.Channel channelOverride) { this.channelOverride = channelOverride; }

    public String getRecipients() { return recipients; }
    public void setRecipients(String recipients) { this.recipients = recipients; }

    public String getNotifyUsers() { return notifyUsers; }
    public void setNotifyUsers(String notifyUsers) { this.notifyUsers = notifyUsers; }

    public Integer getRateLimitSeconds() { return rateLimitSeconds; }
    public void setRateLimitSeconds(Integer rateLimitSeconds) { this.rateLimitSeconds = rateLimitSeconds; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
