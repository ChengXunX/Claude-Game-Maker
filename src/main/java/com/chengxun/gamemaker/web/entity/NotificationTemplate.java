package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 通知模板实体
 * 存储邮件、飞书等渠道的通知模板配置
 *
 * 主要功能：
 * - 支持多渠道模板管理（邮件、飞书、站内信）
 * - 支持变量替换（${variableName}格式）
 * - 支持模板分类（告警、任务、Agent、系统）
 *
 * 变量说明：
 * - ${title} - 标题
 * - ${content} - 内容
 * - ${time} - 当前时间
 * - ${priority} / ${priorityDesc} - 优先级
 * - ${ruleName} - 规则名称
 * - ${metric} - 指标名称
 * - ${triggerValue} - 触发值
 * - ${thresholdValue} - 阈值
 * - ${agentId} / ${agentName} - Agent信息
 * - ${taskTitle} - 任务标题
 * - ${taskResult} - 任务结果
 * - ${projectName} - 项目名称
 * - ${userName} - 用户名称
 * - ${systemName} - 系统名称
 *
 * @author chengxun
 * @since 1.0.0
 */
@Entity
@Table(name = "notification_templates", indexes = {
    @Index(name = "idx_template_channel", columnList = "channel"),
    @Index(name = "idx_template_category", columnList = "category"),
    @Index(name = "idx_template_enabled", columnList = "enabled")
})
public class NotificationTemplate {

    /**
     * 通知渠道枚举
     */
    public enum Channel {
        /** 邮件 */
        EMAIL,
        /** 飞书 */
        FEISHU,
        /** 钉钉 */
        DINGTALK,
        /** 站内信 */
        SYSTEM
    }

    /**
     * 模板分类枚举
     */
    public enum Category {
        /** 告警相关 */
        ALERT,
        /** 任务相关 */
        TASK,
        /** Agent相关 */
        AGENT,
        /** 系统通知 */
        SYSTEM
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 模板编码（唯一标识） */
    @NotBlank(message = "模板编码不能为空")
    @Column(name = "template_code", unique = true, nullable = false, length = 50)
    private String templateCode;

    /** 模板名称 */
    @NotBlank(message = "模板名称不能为空")
    @Column(name = "template_name", nullable = false, length = 100)
    private String templateName;

    /** 通知渠道：EMAIL, FEISHU, SYSTEM */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Channel channel;

    /** 分类：ALERT, TASK, AGENT, SYSTEM */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Category category;

    /** 主题（邮件主题/飞书标题） */
    @Column(length = 200)
    private String subject;

    /** 内容模板 */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /** 描述 */
    @Column(length = 500)
    private String description;

    /** 是否启用 */
    @Column(nullable = false)
    private boolean enabled = true;

    /** 是否系统内置（内置模板不可删除） */
    @Column(name = "system_builtin")
    private boolean systemBuiltin = false;

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    public Channel getChannel() { return channel; }
    public void setChannel(Channel channel) { this.channel = channel; }

    public void setChannel(String channel) {
        this.channel = Channel.valueOf(channel.toUpperCase());
    }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public void setCategory(String category) {
        this.category = Category.valueOf(category.toUpperCase());
    }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isSystemBuiltin() { return systemBuiltin; }
    public void setSystemBuiltin(boolean systemBuiltin) { this.systemBuiltin = systemBuiltin; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
