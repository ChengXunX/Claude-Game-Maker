package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户通知偏好实体
 * 存储用户对每种通知类型、每个渠道的接收偏好
 *
 * 设计理念：
 * - 每个用户可以独立配置每种通知的接收方式
 * - 支持多渠道：站内信、邮件、钉钉、飞书、Webhook
 * - 权限控制：只显示用户有权限看到的通知类型
 * - 可扩展：新增通知类型只需在 NOTIFICATION_TYPES 中注册
 *
 * @author chengxun
 * @since 2.0.0
 */
@Entity
@Table(name = "notification_preferences", indexes = {
    @Index(name = "idx_np_user", columnList = "userId"),
    @Index(name = "idx_np_type", columnList = "notificationType"),
    @Index(name = "idx_np_user_type", columnList = "userId, notificationType, channel", unique = true)
})
public class NotificationPreference {

    /** 通知渠道 */
    public enum Channel {
        IN_APP,     // 站内信
        EMAIL,      // 邮件
        DINGTALK,   // 钉钉
        FEISHU,     // 飞书
        WEBHOOK     // 自定义 Webhook
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户 ID */
    @Column(nullable = false)
    private Long userId;

    /** 通知类型（如 approval, task, alert, system） */
    @Column(nullable = false, length = 50)
    private String notificationType;

    /** 通知渠道 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Channel channel;

    /** 是否启用 */
    @Column(nullable = false)
    private boolean enabled = true;

    /** 是否免打扰（延迟发送） */
    @Column(nullable = false)
    private boolean doNotDisturb = false;

    /** 免打扰开始时间（HH:mm 格式） */
    @Column(length = 10)
    private String quietStart;

    /** 免打扰结束时间（HH:mm 格式） */
    @Column(length = 10)
    private String quietEnd;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

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

    // ===== Getters & Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }

    public Channel getChannel() { return channel; }
    public void setChannel(Channel channel) { this.channel = channel; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isDoNotDisturb() { return doNotDisturb; }
    public void setDoNotDisturb(boolean doNotDisturb) { this.doNotDisturb = doNotDisturb; }

    public String getQuietStart() { return quietStart; }
    public void setQuietStart(String quietStart) { this.quietStart = quietStart; }

    public String getQuietEnd() { return quietEnd; }
    public void setQuietEnd(String quietEnd) { this.quietEnd = quietEnd; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return String.format("NotificationPref[user=%d, type=%s, channel=%s, enabled=%b]",
            userId, notificationType, channel, enabled);
    }
}
