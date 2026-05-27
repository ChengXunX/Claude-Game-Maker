package com.chengxun.gamemaker.agent;

import com.chengxun.gamemaker.engine.ClaudeCliEngine;
import com.chengxun.gamemaker.engine.MessageBus;
import com.chengxun.gamemaker.manager.ContextManager;
import com.chengxun.gamemaker.manager.MemoryManager;
import com.chengxun.gamemaker.model.AgentDefinition;
import com.chengxun.gamemaker.model.AgentMessage;
import com.chengxun.gamemaker.model.TaskAssignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ServerDevAgent extends BaseAgent {
    
    private static final Logger log = LoggerFactory.getLogger(ServerDevAgent.class);
    
    public ServerDevAgent(AgentDefinition definition,
                         ClaudeCliEngine cliEngine,
                         MessageBus messageBus,
                         ContextManager contextManager,
                         MemoryManager memoryManager) {
        super(definition, cliEngine, messageBus, contextManager, memoryManager);
    }
    
    @Override
    protected void doWork() {
        log.info("ServerDev working...");
        
        List<TaskAssignment> pendingTasks = getPendingTasks();
        if (!pendingTasks.isEmpty()) {
            TaskAssignment task = pendingTasks.get(0);
            workOnTask(task);
        }
    }
    
    @Override
    protected void handleMessage(AgentMessage message) {
        switch (message.getType()) {
            case TASK -> handleTask(message);
            case QUERY -> handleQuery(message);
            case RESPONSE -> handleResponse(message);
            case COMMAND -> handleCommand(message);
            default -> log.info("ServerDev received message: {}", message.getType());
        }
    }
    
    private List<TaskAssignment> getPendingTasks() {
        return tasks.stream()
            .filter(t -> t.getStatus() == TaskAssignment.TaskStatus.PENDING)
            .sorted((a, b) -> b.getPriority().compareTo(a.getPriority()))
            .toList();
    }
    
    private void workOnTask(TaskAssignment task) {
        log.info("Working on task: {}", task.getTitle());
        
        task.setStatus(TaskAssignment.TaskStatus.IN_PROGRESS);
        task.setUpdatedAt(java.time.LocalDateTime.now());
        
        String prompt = buildTaskPrompt(task);
        String result = sendMessage(prompt);
        
        task.setResult(result);
        task.setStatus(TaskAssignment.TaskStatus.COMPLETED);
        task.setCompletedAt(java.time.LocalDateTime.now());
        
        reportProgress(task.getId(), "Completed: " + result.substring(0, Math.min(100, result.length())));
    }
    
    private String buildTaskPrompt(TaskAssignment task) {
        StringBuilder sb = new StringBuilder();
        
        String agentsContent = loadMemory("agents_file");
        if (agentsContent != null) {
            sb.append("## 工作约束\n\n").append(agentsContent).append("\n\n");
        }
        
        sb.append("## 任务\n\n");
        sb.append("标题: ").append(task.getTitle()).append("\n");
        sb.append("描述: ").append(task.getDescription()).append("\n");
        sb.append("优先级: ").append(task.getPriority()).append("\n\n");
        sb.append("请执行以上任务，并报告完成情况。");
        
        return sb.toString();
    }
    
    private void handleTask(AgentMessage message) {
        log.info("Received task from {}: {}", message.getFromAgentId(), message.getContent());
        
        TaskAssignment task = TaskAssignment.builder()
            .id("task-" + System.currentTimeMillis())
            .assignerId(message.getFromAgentId())
            .assigneeId(getId())
            .title(message.getContent())
            .description(message.getContent())
            .status(TaskAssignment.TaskStatus.PENDING)
            .priority(TaskAssignment.TaskPriority.MEDIUM)
            .build();
        
        assignTask(task);
    }
    
    private void handleQuery(AgentMessage message) {
        log.info("Received query from {}: {}", message.getFromAgentId(), message.getContent());
        
        String response = sendMessage("请回答: " + message.getContent());
        
        AgentMessage responseMsg = AgentMessage.builder()
            .fromAgentId(getId())
            .toAgentId(message.getFromAgentId())
            .type(AgentMessage.MessageType.RESPONSE)
            .content(response)
            .build();
        sendMessage(responseMsg);
    }
    
    private void handleResponse(AgentMessage message) {
        log.info("Received response from {}: {}", message.getFromAgentId(), message.getContent());
    }
    
    private void handleCommand(AgentMessage message) {
        log.info("Received command from {}: {}", message.getFromAgentId(), message.getContent());
        
        String result = sendMessage("执行命令: " + message.getContent());
        
        AgentMessage report = AgentMessage.createReport(getId(), 
            String.format("命令执行结果:\n%s", result));
        report.setToAgentId(message.getFromAgentId());
        sendMessage(report);
    }
}
