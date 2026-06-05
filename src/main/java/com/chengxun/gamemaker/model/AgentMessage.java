package com.chengxun.gamemaker.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class AgentMessage {
    private String id;
    private String fromAgentId;
    private String toAgentId;
    private MessageType type;
    private String content;
    private String metadata;
    private LocalDateTime timestamp;

    /** 消息优先级：0-9，数字越大优先级越高 */
    private Integer priority = 5;

    /** 消息状态 */
    private MessageStatus status = MessageStatus.PENDING;

    /** 重试次数 */
    private Integer retryCount = 0;

    /** 错误信息 */
    private String error;

    /** 时间戳（毫秒） */
    private Long timestampMs;

    public enum MessageType {
        TASK, REPORT, QUERY, RESPONSE, COMMAND, NOTIFY, APPROVAL, REVIEW, SYSTEM
    }

    public enum MessageStatus {
        PENDING, PROCESSING, PROCESSED, RETRYING, FAILED
    }
    
    public AgentMessage() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
    }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private final AgentMessage msg = new AgentMessage();

        public Builder id(String id) { msg.id = id; return this; }
        public Builder fromAgentId(String from) { msg.fromAgentId = from; return this; }
        public Builder toAgentId(String to) { msg.toAgentId = to; return this; }
        public Builder type(MessageType type) { msg.type = type; return this; }
        public Builder content(String content) { msg.content = content; return this; }
        public Builder metadata(String metadata) { msg.metadata = metadata; return this; }
        public Builder timestamp(LocalDateTime timestamp) { msg.timestamp = timestamp; return this; }
        public Builder priority(Integer priority) { msg.priority = priority; return this; }
        public Builder status(MessageStatus status) { msg.status = status; return this; }

        public AgentMessage build() {
            msg.timestampMs = System.currentTimeMillis();
            return msg;
        }
    }
    
    // Static factory methods
    public static AgentMessage createTask(String from, String to, String content) {
        return builder()
                .fromAgentId(from)
                .toAgentId(to)
                .type(MessageType.TASK)
                .content(content)
                .build();
    }
    
    public static AgentMessage createReport(String from, String content) {
        return builder()
                .fromAgentId(from)
                .type(MessageType.REPORT)
                .content(content)
                .build();
    }
    
    public static AgentMessage createApproval(String from, String content) {
        return builder()
                .fromAgentId(from)
                .type(MessageType.APPROVAL)
                .content(content)
                .build();
    }

    public static AgentMessage createReview(String from, String to, String content) {
        return builder()
                .fromAgentId(from)
                .toAgentId(to)
                .type(MessageType.REVIEW)
                .content(content)
                .build();
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getFromAgentId() { return fromAgentId; }
    public void setFromAgentId(String fromAgentId) { this.fromAgentId = fromAgentId; }
    
    public String getToAgentId() { return toAgentId; }
    public void setToAgentId(String toAgentId) { this.toAgentId = toAgentId; }
    
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public MessageStatus getStatus() { return status; }
    public void setStatus(MessageStatus status) { this.status = status; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public Long getTimestampMs() { return timestampMs; }
    public void setTimestampMs(Long timestampMs) { this.timestampMs = timestampMs; }
}
