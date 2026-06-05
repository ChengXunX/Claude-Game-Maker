package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 站内信通知实体
 * 用于存储系统内部通知消息
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 接收用户ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 通知标题 */
    @NotBlank(message = "title 不能为空")
    @Column(nullable = false, length = 200)
    private String title;

    /** 通知内容 */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** 通知类型 */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private NotificationType type;

    /** 通知渠道 */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private NotificationChannel channel;

    /** 是否已读 */
    @Column(name = "is_read")
    private boolean read = false;

    /** 关联的业务ID（如任务ID、Agent ID等） */
    @Column(name = "reference_id", length = 100)
    private String referenceId;

    /** 关联的业务类型 */
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    /** 跳转链接 */
    @Column(name = "link", length = 500)
    private String link;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public enum NotificationType {
        INFO,       // 普通信息
        WARNING,    // 警告
        ERROR,      // 错误
        SUCCESS,    // 成功
        TASK,       // 任务相关
        AGENT,      // Agent相关
        SYSTEM      // 系统通知
    }

    public enum NotificationChannel {
        SYSTEM,     // 站内信
        EMAIL,      // 邮件
        FEISHU,     // 飞书
        DINGTALK    // 钉钉
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public NotificationChannel getChannel() { return channel; }
    public void setChannel(NotificationChannel channel) { this.channel = channel; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }

    /**
     * 标记为已读
     */
    public void markAsRead() {
        this.read = true;
        this.readAt = LocalDateTime.now();
    }
}
