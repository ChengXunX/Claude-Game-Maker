package com.chengxun.gamemaker.web.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AI助手会话实体
 * 存储用户的聊天会话
 *
 * @author chengxun
 * @since 2.0.0
 */
@Entity
@Table(name = "chat_sessions", indexes = {
    @Index(name = "idx_chat_sessions_user", columnList = "userId"),
    @Index(name = "idx_chat_sessions_updated", columnList = "updatedAt")
})
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 会话标题 */
    @Column(name = "title", length = 200, nullable = false)
    private String title = "新对话";

    /** 会话来源：web=网页端, feishu=飞书端 */
    @Column(name = "source", length = 20)
    private String source = "web";

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    /** 消息列表（列表接口不返回，详情接口单独加载） */
    @JsonIgnore
    @OneToMany(mappedBy = "sessionId", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<ChatMessage> messages = new ArrayList<>();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<ChatMessage> getMessages() { return messages; }
    public void setMessages(List<ChatMessage> messages) { this.messages = messages; }
}
