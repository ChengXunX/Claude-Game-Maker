package com.chengxun.gamemaker.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * AI助手消息实体
 * 存储聊天消息
 *
 * @author chengxun
 * @since 2.0.0
 */
@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_chat_messages_session", columnList = "sessionId")
})
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 会话ID */
    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    /** 角色：user/assistant */
    @Column(name = "role", length = 20, nullable = false)
    private String role;

    /** 消息内容 */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /** 思考过程 */
    @Column(name = "thinking", columnDefinition = "TEXT")
    private String thinking;

    /** 工具调用记录（JSON格式） */
    @Column(name = "tool_uses", columnDefinition = "TEXT")
    private String toolUses;

    /** 任务记录（JSON格式） */
    @Column(name = "tasks", columnDefinition = "TEXT")
    private String tasks;

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getThinking() { return thinking; }
    public void setThinking(String thinking) { this.thinking = thinking; }

    public String getToolUses() { return toolUses; }
    public void setToolUses(String toolUses) { this.toolUses = toolUses; }

    public String getTasks() { return tasks; }
    public void setTasks(String tasks) { this.tasks = tasks; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
