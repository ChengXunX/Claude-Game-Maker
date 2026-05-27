package com.chengxun.gamemaker.agent;

import com.chengxun.gamemaker.engine.ClaudeCliEngine;
import com.chengxun.gamemaker.engine.MessageBus;
import com.chengxun.gamemaker.manager.ContextManager;
import com.chengxun.gamemaker.manager.MemoryManager;
import com.chengxun.gamemaker.manager.SkillManager;
import com.chengxun.gamemaker.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ServerDevAgent extends BaseAgent {

    private static final Logger log = LoggerFactory.getLogger(ServerDevAgent.class);

    public ServerDevAgent(AgentDefinition definition,
                         ClaudeCliEngine cliEngine,
                         MessageBus messageBus,
                         ContextManager contextManager,
                         MemoryManager memoryManager,
                         SkillManager skillManager) {
        super(definition, cliEngine, messageBus, contextManager, memoryManager, skillManager);
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
        task.setUpdatedAt(LocalDateTime.now());
        agentContext.setCurrentTaskId(task.getId());

        // 构建包含 SKILL 的 prompt
        String prompt = buildTaskPrompt(task);
        String result = sendMessage(prompt);

        task.setResult(result);
        task.setStatus(TaskAssignment.TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        agentContext.setCurrentTaskId(null);

        // 保存任务完成经验
        saveTaskExperience(task, result);

        // 尝试学习新 SKILL
        tryLearnSkill(task.getTitle() + "\n" + task.getDescription(), result);

        reportProgress(task.getId(), "Completed: " + result.substring(0, Math.min(100, result.length())));
    }

    private String buildTaskPrompt(TaskAssignment task) {
        StringBuilder sb = new StringBuilder();

        // 加载工作约束
        String agentsContent = loadKnowledge("agents_file");
        if (agentsContent != null) {
            sb.append("## 工作约束\n\n").append(agentsContent).append("\n\n");
        }

        // 加载相关知识
        String projectKnowledge = loadKnowledge("project_architecture");
        if (projectKnowledge != null) {
            sb.append("## 项目知识\n\n").append(projectKnowledge).append("\n\n");
        }

        // 加载相关经验
        String experiences = loadExperience("similar_tasks");
        if (experiences != null) {
            sb.append("## 相关经验\n\n").append(experiences).append("\n\n");
        }

        // 添加 SKILL 匹配
        String skillPrompt = buildSkillPrompt(task.getTitle() + " " + task.getDescription());
        if (!skillPrompt.isEmpty()) {
            sb.append(skillPrompt).append("\n\n");
        }

        // 添加任务描述
        sb.append("## 任务\n\n");
        sb.append("标题: ").append(task.getTitle()).append("\n");
        sb.append("描述: ").append(task.getDescription()).append("\n");
        sb.append("优先级: ").append(task.getPriority()).append("\n\n");

        // 添加工作记忆
        String lastWork = agentContext.getWorkingMemory("last_task_result");
        if (lastWork != null) {
            sb.append("## 上次工作结果\n\n").append(lastWork).append("\n\n");
        }

        sb.append("请执行以上任务，并报告完成情况。");

        return sb.toString();
    }

    private void saveTaskExperience(TaskAssignment task, String result) {
        String experienceKey = "task_" + task.getId();
        String experienceValue = String.format(
            "任务: %s\n描述: %s\n结果: %s\n完成时间: %s",
            task.getTitle(),
            task.getDescription(),
            result.length() > 500 ? result.substring(0, 500) + "..." : result,
            task.getCompletedAt()
        );

        saveExperience(experienceKey, experienceValue);

        // 更新工作记忆
        agentContext.addWorkingMemory("last_task_id", task.getId());
        agentContext.addWorkingMemory("last_task_result", result.length() > 200 ? result.substring(0, 200) + "..." : result);
    }

    private void handleTask(AgentMessage message) {
        log.info("Received task from {}: {}", message.getFromAgentId(), message.getContent());

        TaskAssignment task = TaskAssignment.builder()
            .id("task-" + UUID.randomUUID().toString())
            .assignerId(message.getFromAgentId())
            .assigneeId(getId())
            .title(message.getContent())
            .description(message.getContent())
            .status(TaskAssignment.TaskStatus.PENDING)
            .priority(TaskAssignment.TaskPriority.MEDIUM)
            .build();

        assignTask(task);

        // 保存任务接收经验
        saveExperience("received_task_" + task.getId(),
            String.format("Received task from %s: %s", message.getFromAgentId(), message.getContent()));
    }

    private void handleQuery(AgentMessage message) {
        log.info("Received query from {}: {}", message.getFromAgentId(), message.getContent());

        // 使用 SKILL 系统处理查询
        String skillPrompt = buildSkillPrompt("回答技术问题");
        String fullPrompt = skillPrompt + "\n\n请回答: " + message.getContent();
        String response = sendMessage(fullPrompt);

        AgentMessage responseMsg = AgentMessage.builder()
            .fromAgentId(getId())
            .toAgentId(message.getFromAgentId())
            .type(AgentMessage.MessageType.RESPONSE)
            .content(response)
            .build();
        sendMessage(responseMsg);

        // 保存查询经验
        saveExperience("query_" + System.currentTimeMillis(),
            String.format("Query from %s: %s\nResponse: %s",
                message.getFromAgentId(), message.getContent(),
                response.length() > 200 ? response.substring(0, 200) + "..." : response));
    }

    private void handleResponse(AgentMessage message) {
        log.info("Received response from {}: {}", message.getFromAgentId(), message.getContent());
        // 保存响应到知识库
        saveKnowledge("response_" + System.currentTimeMillis(), message.getContent());
    }

    private void handleCommand(AgentMessage message) {
        log.info("Received command from {}: {}", message.getFromAgentId(), message.getContent());

        // 使用 SKILL 系统执行命令
        String skillPrompt = buildSkillPrompt("执行命令和操作");
        String fullPrompt = skillPrompt + "\n\n执行命令: " + message.getContent();
        String result = sendMessage(fullPrompt);

        AgentMessage report = AgentMessage.createReport(getId(),
            String.format("命令执行结果:\n%s", result));
        report.setToAgentId(message.getFromAgentId());
        sendMessage(report);

        // 保存命令执行经验
        saveExperience("command_" + System.currentTimeMillis(),
            String.format("Command from %s: %s\nResult: %s",
                message.getFromAgentId(), message.getContent(),
                result.length() > 200 ? result.substring(0, 200) + "..." : result));
    }
}
