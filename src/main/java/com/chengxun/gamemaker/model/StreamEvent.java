package com.chengxun.gamemaker.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

/**
 * 流式事件模型
 * 表示Claude CLI输出的各类事件
 *
 * 事件类型：
 * - thinking: 思考过程
 * - text: 文本响应
 * - tool_use: 工具调用
 * - tool_result: 工具结果
 * - task: 任务创建/更新
 * - error: 错误
 * - complete: 完成
 *
 * @author chengxun
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StreamEvent {

    /** 事件类型 */
    private String type;

    /** 事件内容 */
    private String content;

    /** 工具名称（tool_use类型时使用） */
    private String toolName;

    /** 工具输入（tool_use类型时使用） */
    private Map<String, Object> toolInput;

    /** 工具结果（tool_result类型时使用） */
    private String toolResult;

    /** 任务ID（task类型时使用） */
    private String taskId;

    /** 任务标题（task类型时使用） */
    private String taskTitle;

    /** 任务状态（task类型时使用） */
    private String taskStatus;

    /** 错误信息（error类型时使用） */
    private String errorMessage;

    /** 时间戳 */
    private long timestamp;

    /** 原始JSON（用于调试） */
    private String rawJson;

    public StreamEvent() {
        this.timestamp = System.currentTimeMillis();
    }

    public StreamEvent(String type, String content) {
        this.type = type;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }

    public Map<String, Object> getToolInput() { return toolInput; }
    public void setToolInput(Map<String, Object> toolInput) { this.toolInput = toolInput; }

    public String getToolResult() { return toolResult; }
    public void setToolResult(String toolResult) { this.toolResult = toolResult; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getTaskTitle() { return taskTitle; }
    public void setTaskTitle(String taskTitle) { this.taskTitle = taskTitle; }

    public String getTaskStatus() { return taskStatus; }
    public void setTaskStatus(String taskStatus) { this.taskStatus = taskStatus; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getRawJson() { return rawJson; }
    public void setRawJson(String rawJson) { this.rawJson = rawJson; }

    /**
     * 创建思考事件
     */
    public static StreamEvent thinking(String content) {
        StreamEvent event = new StreamEvent("thinking", content);
        return event;
    }

    /**
     * 创建文本事件
     */
    public static StreamEvent text(String content) {
        StreamEvent event = new StreamEvent("text", content);
        return event;
    }

    /**
     * 创建工具调用事件
     */
    public static StreamEvent toolUse(String toolName, Map<String, Object> toolInput) {
        StreamEvent event = new StreamEvent("tool_use", null);
        event.setToolName(toolName);
        event.setToolInput(toolInput);
        return event;
    }

    /**
     * 创建工具结果事件
     */
    public static StreamEvent toolResult(String toolName, String result) {
        StreamEvent event = new StreamEvent("tool_result", null);
        event.setToolName(toolName);
        event.setToolResult(result);
        return event;
    }

    /**
     * 创建任务事件
     */
    public static StreamEvent task(String taskId, String title, String status) {
        StreamEvent event = new StreamEvent("task", null);
        event.setTaskId(taskId);
        event.setTaskTitle(title);
        event.setTaskStatus(status);
        return event;
    }

    /**
     * 创建错误事件
     */
    public static StreamEvent error(String message) {
        StreamEvent event = new StreamEvent("error", null);
        event.setErrorMessage(message);
        return event;
    }

    /**
     * 创建完成事件
     */
    public static StreamEvent complete(String content) {
        StreamEvent event = new StreamEvent("complete", content);
        return event;
    }

    @Override
    public String toString() {
        return "StreamEvent{type='" + type + "', content='" + (content != null ? content.substring(0, Math.min(50, content.length())) + "..." : "null") + "'}";
    }
}
