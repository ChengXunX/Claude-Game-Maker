package com.chengxun.gamemaker.web.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 项目讨论会话实体
 * 支持用户与项目进行多轮对话，讨论项目方向
 * 对话结束后可手动生成会议纪要，纪要自动同步给制作人Agent
 *
 * @author chengxun
 * @since 2.0.0
 */
@Entity
@Table(name = "project_discussions", indexes = {
    @Index(name = "idx_pd_project", columnList = "projectId"),
    @Index(name = "idx_pd_user", columnList = "userId"),
    @Index(name = "idx_pd_status", columnList = "status"),
    @Index(name = "idx_pd_updated", columnList = "updatedAt")
})
public class ProjectDiscussion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联项目ID */
    @Column(name = "project_id", nullable = false, length = 100)
    private String projectId;

    /** 发起用户ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 发起用户名 */
    @Column(name = "username", length = 50)
    private String username;

    /** 会话标题（自动生成或用户自定义） */
    @Column(name = "title", length = 200)
    private String title;

    /** 会话状态：ACTIVE=进行中, MINUTES_GENERATED=已生成纪要, ARCHIVED=已归档 */
    @Column(name = "status", length = 30, nullable = false)
    private String status = "ACTIVE";

    /** 会议纪要（手动生成后填充） */
    @Column(name = "meeting_minutes", columnDefinition = "TEXT")
    private String meetingMinutes;

    /** 纪要是否已同步给制作人 */
    @Column(name = "synced_to_producer")
    private boolean syncedToProducer = false;

    /** 同步给制作人的时间 */
    @Column(name = "synced_at")
    private LocalDateTime syncedAt;

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    /** 消息列表 */
    @JsonIgnore
    @OneToMany(mappedBy = "discussionId", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<ProjectDiscussionMessage> messages = new ArrayList<>();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMeetingMinutes() { return meetingMinutes; }
    public void setMeetingMinutes(String meetingMinutes) { this.meetingMinutes = meetingMinutes; }

    public boolean isSyncedToProducer() { return syncedToProducer; }
    public void setSyncedToProducer(boolean syncedToProducer) { this.syncedToProducer = syncedToProducer; }

    public LocalDateTime getSyncedAt() { return syncedAt; }
    public void setSyncedAt(LocalDateTime syncedAt) { this.syncedAt = syncedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<ProjectDiscussionMessage> getMessages() { return messages; }
    public void setMessages(List<ProjectDiscussionMessage> messages) { this.messages = messages; }
}
