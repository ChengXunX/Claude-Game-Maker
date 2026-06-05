package com.chengxun.gamemaker.agent;

import com.chengxun.gamemaker.engine.ClaudeCliEngine;
import com.chengxun.gamemaker.engine.MessageBus;
import com.chengxun.gamemaker.manager.*;
import com.chengxun.gamemaker.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class SystemPlannerAgent extends BaseAgent {

    private static final Logger log = LoggerFactory.getLogger(SystemPlannerAgent.class);

    public SystemPlannerAgent(AgentDefinition definition,
                             ClaudeCliEngine cliEngine,
                             MessageBus messageBus,
                             ContextManager contextManager,
                             MemoryManager memoryManager,
                             SkillManager skillManager,
                             ProjectManager projectManager) {
        super(definition, cliEngine, messageBus, contextManager, memoryManager, skillManager, projectManager);
    }

    @Override
    protected void doWork() {
        log.info("SystemPlannerAgent working...");

        // 检查待处理的设计任务
        processPendingTasks();

        // 协调与其他 Agent 的工作
        coordinateWithAgents();

        // 更新设计文档
        updateDesignDocuments();
    }

    @Override
    protected void handleMessage(AgentMessage message) {
        switch (message.getType()) {
            case TASK -> handleTask(message);
            case QUERY -> handleQuery(message);
            case REVIEW -> handleReview(message);
            case COMMAND -> handleCommand(message);
            case RESPONSE -> handleResponse(message);
            default -> log.info("SystemPlannerAgent received message: {}", message.getType());
        }
    }

    /**
     * 处理待处理的任务
     */
    private void processPendingTasks() {
        List<TaskAssignment> pendingTasks = tasks.stream()
            .filter(t -> t.getStatus() == TaskAssignment.TaskStatus.PENDING)
            .sorted((a, b) -> b.getPriority().compareTo(a.getPriority()))
            .toList();

        if (!pendingTasks.isEmpty()) {
            TaskAssignment task = pendingTasks.get(0);
            workOnTask(task);
        }
    }

    /**
     * 执行设计任务
     */
    private void workOnTask(TaskAssignment task) {
        log.info("Working on design task: {}", task.getTitle());

        task.setStatus(TaskAssignment.TaskStatus.IN_PROGRESS);
        task.setUpdatedAt(LocalDateTime.now());
        agentContext.setCurrentTaskId(task.getId());

        String prompt = buildDesignPrompt(task);
        String result = sendMessage(prompt);

        task.setResult(result);
        task.setStatus(TaskAssignment.TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        agentContext.setCurrentTaskId(null);

        // 保存设计经验
        saveDesignExperience(task, result);

        // 尝试学习设计技能
        tryLearnSkill(task.getTitle() + "\n" + task.getDescription(), result);

        // 通知相关 Agent
        notifyRelatedAgents(task, result);

        reportProgress(task.getId(), "设计完成: " + result.substring(0, Math.min(100, result.length())));
    }

    /**
     * 构建设计提示词
     */
    private String buildDesignPrompt(TaskAssignment task) {
        StringBuilder sb = new StringBuilder();

        // 加载设计知识
        String agentsContent = loadKnowledge("agents_file");
        if (agentsContent != null) {
            sb.append("## 设计规范\n\n").append(agentsContent).append("\n\n");
        }

        // 加载项目上下文
        if (currentProject != null) {
            sb.append("## 项目信息\n\n");
            sb.append("- 项目名称: ").append(currentProject.getName()).append("\n");
            sb.append("- 项目描述: ").append(currentProject.getDescription()).append("\n\n");

            // 加载项目规则
            String projectRules = projectManager.loadProjectRules(currentProject.getId());
            if (projectRules != null) {
                sb.append("## 项目规范\n\n").append(projectRules).append("\n\n");
            }
        }

        // 加载已有设计
        String existingDesigns = loadKnowledge("design_documents");
        if (existingDesigns != null) {
            sb.append("## 已有设计\n\n").append(existingDesigns).append("\n\n");
        }

        // 加载相关经验
        String experiences = loadExperience("similar_designs");
        if (experiences != null) {
            sb.append("## 相关经验\n\n").append(experiences).append("\n\n");
        }

        // 添加 SKILL 匹配
        String skillPrompt = buildSkillPrompt(task.getTitle() + " " + task.getDescription());
        if (!skillPrompt.isEmpty()) {
            sb.append(skillPrompt).append("\n\n");
        }

        // 添加任务描述
        sb.append("## 设计任务\n\n");
        sb.append("标题: ").append(task.getTitle()).append("\n");
        sb.append("描述: ").append(task.getDescription()).append("\n");
        sb.append("优先级: ").append(task.getPriority()).append("\n\n");

        // 添加设计要求
        sb.append("## 设计要求\n\n");
        sb.append("1. 请按照设计规范输出完整的设计文档\n");
        sb.append("2. 包含系统概述、架构设计、功能详情、交互设计、数据设计\n");
        sb.append("3. 考虑与现有系统的兼容性\n");
        sb.append("4. 提供验收标准和测试要点\n\n");

        sb.append("请输出完整的设计方案。");

        return sb.toString();
    }

    /**
     * 保存设计经验
     */
    private void saveDesignExperience(TaskAssignment task, String result) {
        String experienceKey = "design_" + task.getId();
        String experienceValue = String.format(
            "设计任务: %s\n描述: %s\n项目: %s\n设计结果: %s\n完成时间: %s",
            task.getTitle(),
            task.getDescription(),
            currentProject != null ? currentProject.getName() : "global",
            result.length() > 500 ? result.substring(0, 500) + "..." : result,
            task.getCompletedAt()
        );

        saveExperience(experienceKey, experienceValue);

        // 更新工作记忆
        agentContext.addWorkingMemory("last_design_id", task.getId());
        agentContext.addWorkingMemory("last_design_summary",
            result.length() > 200 ? result.substring(0, 200) + "..." : result);
    }

    /**
     * 通知相关 Agent
     */
    private void notifyRelatedAgents(TaskAssignment task, String result) {
        // 通知数值策划
        AgentMessage numericalNotify = AgentMessage.builder()
            .fromAgentId(getId())
            .toAgentId(getProjectId() != null ? getProjectId() + ":numerical-planner" : "numerical-planner")
            .type(AgentMessage.MessageType.NOTIFY)
            .content(String.format("新的系统设计完成，请评估数值需求：\n\n任务: %s\n设计摘要: %s",
                task.getTitle(),
                result.length() > 300 ? result.substring(0, 300) + "..." : result))
            .build();
        sendMessage(numericalNotify);

        // 通知制作人
        AgentMessage producerNotify = AgentMessage.builder()
            .fromAgentId(getId())
            .toAgentId(getProjectId() != null ? getProjectId() + ":producer" : "producer")
            .type(AgentMessage.MessageType.REPORT)
            .content(String.format("系统设计完成报告：\n\n任务: %s\n状态: 已完成", task.getTitle()))
            .build();
        sendMessage(producerNotify);
    }

    /**
     * 协调与其他 Agent 的工作
     */
    private void coordinateWithAgents() {
        // 检查是否有需要协调的事项
        String coordinationPrompt = buildCoordinationPrompt();
        if (!coordinationPrompt.isEmpty()) {
            String coordinationResult = sendMessage(coordinationPrompt);
            saveKnowledge("coordination_log_" + System.currentTimeMillis(), coordinationResult);
        }
    }

    /**
     * 构建协调提示词
     */
    private String buildCoordinationPrompt() {
        StringBuilder sb = new StringBuilder();

        sb.append("## 团队协调任务\n\n");
        sb.append("请检查以下协调事项：\n\n");
        sb.append("1. 与数值策划的协调：确认数值需求是否满足\n");
        sb.append("2. 与开发团队的协调：确认技术可行性\n");
        sb.append("3. 与 Git 提交专员的协调：确认实现符合设计\n\n");

        // 加载待协调事项
        String pendingCoordination = loadKnowledge("pending_coordination");
        if (pendingCoordination != null) {
            sb.append("## 待协调事项\n\n").append(pendingCoordination).append("\n\n");
        }

        sb.append("请输出协调建议和行动项。");

        return sb.toString();
    }

    /**
     * 更新设计文档
     */
    private void updateDesignDocuments() {
        String updatePrompt = buildUpdatePrompt();
        if (!updatePrompt.isEmpty()) {
            String updateResult = sendMessage(updatePrompt);
            saveKnowledge("design_documents", updateResult);
        }
    }

    /**
     * 构建更新提示词
     */
    private String buildUpdatePrompt() {
        StringBuilder sb = new StringBuilder();

        // 加载现有设计文档
        String existingDocs = loadKnowledge("design_documents");
        if (existingDocs == null) {
            return "";
        }

        sb.append("## 设计文档更新任务\n\n");
        sb.append("请根据最新的设计任务和反馈，更新设计文档。\n\n");

        sb.append("### 现有设计文档\n\n");
        sb.append(existingDocs).append("\n\n");

        // 加载最近的设计任务
        String recentDesigns = loadExperience("recent_designs");
        if (recentDesigns != null) {
            sb.append("### 最近的设计任务\n\n");
            sb.append(recentDesigns).append("\n\n");
        }

        // 加载反馈
        String feedback = loadKnowledge("design_feedback");
        if (feedback != null) {
            sb.append("### 设计反馈\n\n");
            sb.append(feedback).append("\n\n");
        }

        sb.append("请输出更新后的设计文档。");

        return sb.toString();
    }

    /**
     * 处理任务消息
     */
    private void handleTask(AgentMessage message) {
        log.info("Received task from {}: {}", message.getFromAgentId(), message.getContent());

        TaskAssignment task = TaskAssignment.builder()
            .id("design-task-" + UUID.randomUUID().toString())
            .assignerId(message.getFromAgentId())
            .assigneeId(getId())
            .title("系统设计任务")
            .description(message.getContent())
            .status(TaskAssignment.TaskStatus.PENDING)
            .priority(TaskAssignment.TaskPriority.HIGH)
            .build();

        assignTask(task);
    }

    /**
     * 处理查询
     */
    private void handleQuery(AgentMessage message) {
        log.info("Received query from {}: {}", message.getFromAgentId(), message.getContent());

        String skillPrompt = buildSkillPrompt("回答系统设计相关问题");
        String fullPrompt = skillPrompt + "\n\n请回答: " + message.getContent();
        String response = sendMessage(fullPrompt);

        AgentMessage responseMsg = AgentMessage.builder()
            .fromAgentId(getId())
            .toAgentId(message.getFromAgentId())
            .type(AgentMessage.MessageType.RESPONSE)
            .content(response)
            .build();
        sendMessage(responseMsg);

        saveExperience("query_" + System.currentTimeMillis(),
            String.format("Query from %s: %s\nResponse: %s",
                message.getFromAgentId(), message.getContent(),
                response.length() > 200 ? response.substring(0, 200) + "..." : response));
    }

    /**
     * 处理审查请求
     */
    private void handleReview(AgentMessage message) {
        log.info("Received review request from {}: {}", message.getFromAgentId(), message.getContent());

        // 审查设计实现是否符合设计
        String reviewPrompt = buildReviewPrompt(message.getContent());
        String reviewResult = sendMessage(reviewPrompt);

        // 返回审查结果
        AgentMessage response = AgentMessage.builder()
            .fromAgentId(getId())
            .toAgentId(message.getFromAgentId())
            .type(AgentMessage.MessageType.RESPONSE)
            .content(reviewResult)
            .build();
        sendMessage(response);

        // 保存审查记录
        saveExperience("review_" + System.currentTimeMillis(),
            String.format("Review from %s: %s\nResult: %s",
                message.getFromAgentId(), message.getContent(),
                reviewResult.length() > 500 ? reviewResult.substring(0, 500) + "..." : reviewResult));
    }

    /**
     * 构建审查提示词
     */
    private String buildReviewPrompt(String reviewTarget) {
        StringBuilder sb = new StringBuilder();

        String agentsContent = loadKnowledge("agents_file");
        if (agentsContent != null) {
            sb.append("## 设计规范\n\n").append(agentsContent).append("\n\n");
        }

        // 加载相关设计文档
        String designDocs = loadKnowledge("design_documents");
        if (designDocs != null) {
            sb.append("## 设计文档\n\n").append(designDocs).append("\n\n");
        }

        sb.append("## 审查任务\n\n");
        sb.append("请审查以下内容是否符合设计规范：\n\n");
        sb.append(reviewTarget).append("\n\n");
        sb.append("请输出详细的审查报告。");

        return sb.toString();
    }

    /**
     * 处理命令
     */
    private void handleCommand(AgentMessage message) {
        log.info("Received command from {}: {}", message.getFromAgentId(), message.getContent());

        String result;
        if (message.getContent().contains("更新设计") || message.getContent().contains("update design")) {
            updateDesignDocuments();
            result = "设计文档已更新";
        } else if (message.getContent().contains("协调") || message.getContent().contains("coordinate")) {
            coordinateWithAgents();
            result = "协调任务已执行";
        } else {
            result = "未知命令: " + message.getContent();
        }

        AgentMessage report = AgentMessage.createReport(getId(),
            String.format("命令执行结果:\n%s", result));
        report.setToAgentId(message.getFromAgentId());
        sendMessage(report);
    }

    /**
     * 处理响应
     */
    private void handleResponse(AgentMessage message) {
        log.info("Received response from {}: {}", message.getFromAgentId(), message.getContent());
        saveKnowledge("response_" + System.currentTimeMillis(), message.getContent());
    }

    /**
     * 创建系统设计
     */
    public String createSystemDesign(String systemName, String requirements) {
        String designPrompt = String.format(
            "请为以下系统创建完整的设计方案：\n\n系统名称: %s\n需求: %s\n\n" +
            "请按照设计规范输出完整的设计文档。",
            systemName, requirements
        );

        String design = sendMessage(designPrompt);

        // 保存设计
        saveKnowledge("design_" + systemName, design);

        // 通知相关 Agent
        notifyDesignCreated(systemName, design);

        return design;
    }

    /**
     * 通知设计创建完成
     */
    private void notifyDesignCreated(String systemName, String design) {
        // 通知数值策划
        AgentMessage numericalNotify = AgentMessage.builder()
            .fromAgentId(getId())
            .toAgentId(getProjectId() != null ? getProjectId() + ":numerical-planner" : "numerical-planner")
            .type(AgentMessage.MessageType.NOTIFY)
            .content(String.format("新系统设计完成，请评估数值需求：\n\n系统: %s\n设计摘要: %s",
                systemName,
                design.length() > 300 ? design.substring(0, 300) + "..." : design))
            .build();
        sendMessage(numericalNotify);

        // 通知制作人
        AgentMessage producerNotify = AgentMessage.builder()
            .fromAgentId(getId())
            .toAgentId(getProjectId() != null ? getProjectId() + ":producer" : "producer")
            .type(AgentMessage.MessageType.REPORT)
            .content(String.format("系统设计完成：\n\n系统: %s", systemName))
            .build();
        sendMessage(producerNotify);
    }

    /**
     * 评审设计方案
     */
    public String reviewDesign(String designName) {
        String design = loadKnowledge("design_" + designName);
        if (design == null) {
            return "未找到设计: " + designName;
        }

        String reviewPrompt = String.format(
            "请评审以下设计方案：\n\n设计名称: %s\n设计内容:\n%s\n\n" +
            "请从完整性、可行性、合理性等方面进行评审。",
            designName, design
        );

        return sendMessage(reviewPrompt);
    }
}
