package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 项目讨论消息实体
 * 存储项目讨论中的每条消息
 *
 * @author chengxun
 * @since 2.0.0
 */
@Entity
@Table(name = "project_discussion_messages", indexes = {
    @Index(name = "idx_pdm_discussion", columnList = "discussionId"),
    @Index(name = "idx_pdm_created", columnList = "createdAt")
})
public class ProjectDiscussionMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属讨论会话ID */
    @Column(name = "discussion_id", nullable = false)
    private Long discussionId;

    /** 角色：user/assistant/system */
    @Column(name = "role", length = 20, nullable = false)
    private String role;

    /** 消息内容 */
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /** 发送者用户名（user角色时填充） */
    @Column(name = "sender", length = 50)
    private String sender;

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDiscussionId() { return discussionId; }
    public void setDiscussionId(Long discussionId) { this.discussionId = discussionId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
