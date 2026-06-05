package com.chengxun.gamemaker.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class TaskAssignment {
    private String id;
    private String assignerId;
    private String assigneeId;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    private String result;

    /** 重试次数 */
    private Integer retryCount = 0;

    /** 错误信息 */
    private String error;

    /** 开始时间（毫秒时间戳） */
    private Long startedAt;

    /** 创建时间（毫秒时间戳） */
    private Long createdAtMs;

    /** 完成时间（毫秒时间戳） */
    private Long completedAtMs;

    /** 优先级数值（用于队列排序） */
    private Integer priorityValue = 5;

    public enum TaskStatus {
        PENDING, IN_PROGRESS, PROCESSING, COMPLETED, FAILED, CANCELLED, RETRYING, REJECTED
    }
    
    public enum TaskPriority {
        LOW, MEDIUM, HIGH, URGENT
    }
    
    public TaskAssignment() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private final TaskAssignment task = new TaskAssignment();
        
        public Builder id(String id) { task.id = id; return this; }
        public Builder assignerId(String assignerId) { task.assignerId = assignerId; return this; }
        public Builder assigneeId(String assigneeId) { task.assigneeId = assigneeId; return this; }
        public Builder title(String title) { task.title = title; return this; }
        public Builder description(String description) { task.description = description; return this; }
        public Builder status(TaskStatus status) { task.status = status; return this; }
        public Builder priority(TaskPriority priority) { task.priority = priority; return this; }
        public Builder createdAt(LocalDateTime createdAt) { task.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { task.updatedAt = updatedAt; return this; }
        public Builder completedAt(LocalDateTime completedAt) { task.completedAt = completedAt; return this; }
        public Builder result(String result) { task.result = result; return this; }
        
        public TaskAssignment build() { return task; }
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getAssignerId() { return assignerId; }
    public void setAssignerId(String assignerId) { this.assignerId = assignerId; }
    
    public String getAssigneeId() { return assigneeId; }
    public void setAssigneeId(String assigneeId) { this.assigneeId = assigneeId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    
    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public Long getStartedAt() { return startedAt; }
    public void setStartedAt(Long startedAt) { this.startedAt = startedAt; }

    public Long getCreatedAtMs() { return createdAtMs; }
    public void setCreatedAtMs(Long createdAtMs) { this.createdAtMs = createdAtMs; }

    public Long getCompletedAtMs() { return completedAtMs; }
    public void setCompletedAtMs(Long completedAtMs) { this.completedAtMs = completedAtMs; }

    public Integer getPriorityValue() { return priorityValue; }
    public void setPriorityValue(Integer priorityValue) { this.priorityValue = priorityValue; }
}
