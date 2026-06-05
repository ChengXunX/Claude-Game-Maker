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

public class NumericalPlannerAgent extends BaseAgent {

    private static final Logger log = LoggerFactory.getLogger(NumericalPlannerAgent.class);

    public NumericalPlannerAgent(AgentDefinition definition,
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
        log.info("NumericalPlannerAgent working...");

        // 检查待处理的数值设计任务
        processPendingTasks();

        // 分析数值平衡性
        analyzeBalance();

        // 更新数值文档
        updateNumericalDocuments();
    }

    @Override
    protected void handleMessage(AgentMessage message) {
        switch (message.getType()) {
            case TASK -> handleTask(message);
            case QUERY -> handleQuery(message);
            case REVIEW -> handleReview(message);
            case COMMAND -> handleCommand(message);
            case RESPONSE -> handleResponse(message);
            default -> log.info("NumericalPlannerAgent received message: {}", message.getType());
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
     * 执行数值设计任务
     */
    private void workOnTask(TaskAssignment task) {
        log.info("Working on numerical task: {}", task.getTitle());

        task.setStatus(TaskAssignment.TaskStatus.IN_PROGRESS);
        task.setUpdatedAt(LocalDateTime.now());
        agentContext.setCurrentTaskId(task.getId());

        String prompt = buildNumericalPrompt(task);
        String result = sendMessage(prompt);

        task.setResult(result);
        task.setStatus(TaskAssignment.TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        agentContext.setCurrentTaskId(null);

        // 保存数值设计经验
        saveNumericalExperience(task, result);

        // 尝试学习数值技能
        tryLearnSkill(task.getTitle() + "\n" + task.getDescription(), result);

        // 通知相关 Agent
        notifyRelatedAgents(task, result);

        reportProgress(task.getId(), "数值设计完成: " + result.substring(0, Math.min(100, result.length())));
    }

    /**
     * 构建数值设计提示词
     */
    private String buildNumericalPrompt(TaskAssignment task) {
        StringBuilder sb = new StringBuilder();

        // 加载数值设计知识
        String agentsContent = loadKnowledge("agents_file");
        if (agentsContent != null) {
            sb.append("## 数值设计规范\n\n").append(agentsContent).append("\n\n");
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

        // 加载已有数值设计
        String existingNumerical = loadKnowledge("numerical_documents");
        if (existingNumerical != null) {
            sb.append("## 已有数值设计\n\n").append(existingNumerical).append("\n\n");
        }

        // 加载相关经验
        String experiences = loadExperience("similar_numerical");
        if (experiences != null) {
            sb.append("## 相关经验\n\n").append(experiences).append("\n\n");
        }

        // 添加 SKILL 匹配
        String skillPrompt = buildSkillPrompt(task.getTitle() + " " + task.getDescription());
        if (!skillPrompt.isEmpty()) {
            sb.append(skillPrompt).append("\n\n");
        }

        // 添加任务描述
        sb.append("## 数值设计任务\n\n");
        sb.append("标题: ").append(task.getTitle()).append("\n");
        sb.append("描述: ").append(task.getDescription()).append("\n");
        sb.append("优先级: ").append(task.getPriority()).append("\n\n");

        // 添加设计要求
        sb.append("## 设计要求\n\n");
        sb.append("1. 请按照数值设计规范输出完整的数值文档\n");
        sb.append("2. 包含设计目标、核心公式、数值表、平衡性分析\n");
        sb.append("3. 提供计算公式和参数说明\n");
        sb.append("4. 考虑与其他系统的数值平衡\n\n");

        sb.append("请输出完整的数值设计方案。");

        return sb.toString();
    }

    /**
     * 保存数值设计经验
     */
    private void saveNumericalExperience(TaskAssignment task, String result) {
        String experienceKey = "numerical_" + task.getId();
        String experienceValue = String.format(
            "数值任务: %s\n描述: %s\n项目: %s\n数值结果: %s\n完成时间: %s",
            task.getTitle(),
            task.getDescription(),
            currentProject != null ? currentProject.getName() : "global",
            result.length() > 500 ? result.substring(0, 500) + "..." : result,
            task.getCompletedAt()
        );

        saveExperience(experienceKey, experienceValue);

        // 更新工作记忆
        agentContext.addWorkingMemory("last_numerical_id", task.getId());
        agentContext.addWorkingMemory("last_numerical_summary",
            result.length() > 200 ? result.substring(0, 200) + "..." : result);
    }

    /**
     * 通知相关 Agent
     */
    private void notifyRelatedAgents(TaskAssignment task, String result) {
        // 通知系统策划
        AgentMessage systemNotify = AgentMessage.builder()
            .fromAgentId(getId())
            .toAgentId(getProjectId() != null ? getProjectId() + ":system-planner" : "system-planner")
            .type(AgentMessage.MessageType.NOTIFY)
            .content(String.format("数值设计完成，请确认系统设计兼容性：\n\n任务: %s\n数值摘要: %s",
                task.getTitle(),
                result.length() > 300 ? result.substring(0, 300) + "..." : result))
            .build();
        sendMessage(systemNotify);

        // 通知 Git 提交专员
        AgentMessage gitNotify = AgentMessage.builder()
            .fromAgentId(getId())
            .toAgentId(getProjectId() != null ? getProjectId() + ":git-commit" : "git-commit")
            .type(AgentMessage.MessageType.NOTIFY)
            .content(String.format("数值设计完成，请在代码提交时审核数值配置：\n\n任务: %s", task.getTitle()))
            .build();
        sendMessage(gitNotify);

        // 通知制作人
        AgentMessage producerNotify = AgentMessage.builder()
            .fromAgentId(getId())
            .toAgentId(getProjectId() != null ? getProjectId() + ":producer" : "producer")
            .type(AgentMessage.MessageType.REPORT)
            .content(String.format("数值设计完成报告：\n\n任务: %s\n状态: 已完成", task.getTitle()))
            .build();
        sendMessage(producerNotify);
    }

    /**
     * 分析数值平衡性
     */
    private void analyzeBalance() {
        String analysisPrompt = buildBalanceAnalysisPrompt();
        if (!analysisPrompt.isEmpty()) {
            String analysisResult = sendMessage(analysisPrompt);
            saveKnowledge("balance_analysis_" + System.currentTimeMillis(), analysisResult);
        }
    }

    /**
     * 构建平衡性分析提示词
     */
    private String buildBalanceAnalysisPrompt() {
        StringBuilder sb = new StringBuilder();

        // 加载数值设计知识
        String agentsContent = loadKnowledge("agents_file");
        if (agentsContent != null) {
            sb.append("## 数值设计规范\n\n").append(agentsContent).append("\n\n");
        }

        // 加载现有数值文档
        String numericalDocs = loadKnowledge("numerical_documents");
        if (numericalDocs == null) {
            return "";
        }

        sb.append("## 数值平衡性分析任务\n\n");
        sb.append("请分析现有数值设计的平衡性。\n\n");

        sb.append("### 现有数值文档\n\n");
        sb.append(numericalDocs).append("\n\n");

        // 加载最近的数值设计
        String recentNumerical = loadExperience("recent_numerical");
        if (recentNumerical != null) {
            sb.append("### 最近的数值设计\n\n");
            sb.append(recentNumerical).append("\n\n");
        }

        sb.append("## 分析要求\n\n");
        sb.append("1. 检查经济系统的平衡性（产出/消耗比、通胀率）\n");
        sb.append("2. 检查战斗系统的平衡性（伤害公式、属性系数）\n");
        sb.append("3. 检查成长系统的平衡性（升级曲线、卡点设计）\n");
        sb.append("4. 识别潜在的数值风险点\n");
        sb.append("5. 提供优化建议\n\n");

        sb.append("请输出详细的平衡性分析报告。");

        return sb.toString();
    }

    /**
     * 更新数值文档
     */
    private void updateNumericalDocuments() {
        String updatePrompt = buildUpdatePrompt();
        if (!updatePrompt.isEmpty()) {
            String updateResult = sendMessage(updatePrompt);
            saveKnowledge("numerical_documents", updateResult);
        }
    }

    /**
     * 构建更新提示词
     */
    private String buildUpdatePrompt() {
        StringBuilder sb = new StringBuilder();

        // 加载现有数值文档
        String existingDocs = loadKnowledge("numerical_documents");
        if (existingDocs == null) {
            return "";
        }

        sb.append("## 数值文档更新任务\n\n");
        sb.append("请根据最新的数值设计任务和反馈，更新数值文档。\n\n");

        sb.append("### 现有数值文档\n\n");
        sb.append(existingDocs).append("\n\n");

        // 加载最近的数值设计
        String recentNumerical = loadExperience("recent_numerical");
        if (recentNumerical != null) {
            sb.append("### 最近的数值设计\n\n");
            sb.append(recentNumerical).append("\n\n");
        }

        // 加载反馈
        String feedback = loadKnowledge("numerical_feedback");
        if (feedback != null) {
            sb.append("### 数值反馈\n\n");
            sb.append(feedback).append("\n\n");
        }

        sb.append("请输出更新后的数值文档。");

        return sb.toString();
    }

    /**
     * 处理任务消息
     */
    private void handleTask(AgentMessage message) {
        log.info("Received task from {}: {}", message.getFromAgentId(), message.getContent());

        TaskAssignment task = TaskAssignment.builder()
            .id("numerical-task-" + UUID.randomUUID().toString())
            .assignerId(message.getFromAgentId())
            .assigneeId(getId())
            .title("数值设计任务")
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

        String skillPrompt = buildSkillPrompt("回答数值设计相关问题");
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

        // 审查数值实现是否正确
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
            sb.append("## 数值设计规范\n\n").append(agentsContent).append("\n\n");
        }

        // 加载相关数值文档
        String numericalDocs = loadKnowledge("numerical_documents");
        if (numericalDocs != null) {
            sb.append("## 数值文档\n\n").append(numericalDocs).append("\n\n");
        }

        sb.append("## 审查任务\n\n");
        sb.append("请审查以下内容是否符合数值设计规范：\n\n");
        sb.append(reviewTarget).append("\n\n");
        sb.append("请输出详细的审查报告，重点关注：\n");
        sb.append("1. 数值计算是否正确\n");
        sb.append("2. 参数配置是否合理\n");
        sb.append("3. 是否符合平衡性要求\n");
        sb.append("4. 是否有潜在的数值风险\n\n");

        return sb.toString();
    }

    /**
     * 处理命令
     */
    private void handleCommand(AgentMessage message) {
        log.info("Received command from {}: {}", message.getFromAgentId(), message.getContent());

        String result;
        if (message.getContent().contains("平衡分析") || message.getContent().contains("balance analysis")) {
            analyzeBalance();
            result = "平衡性分析已完成";
        } else if (message.getContent().contains("更新数值") || message.getContent().contains("update numerical")) {
            updateNumericalDocuments();
            result = "数值文档已更新";
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
     * 创建数值设计
     */
    public String createNumericalDesign(String systemName, String requirements) {
        String designPrompt = String.format(
            "请为以下系统创建完整的数值设计方案：\n\n系统名称: %s\n需求: %s\n\n" +
            "请按照数值设计规范输出完整的数值文档。",
            systemName, requirements
        );

        String design = sendMessage(designPrompt);

        // 保存数值设计
        saveKnowledge("numerical_" + systemName, design);

        // 通知相关 Agent
        notifyNumericalCreated(systemName, design);

        return design;
    }

    /**
     * 通知数值设计创建完成
     */
    private void notifyNumericalCreated(String systemName, String design) {
        // 通知系统策划
        AgentMessage systemNotify = AgentMessage.builder()
            .fromAgentId(getId())
            .toAgentId(getProjectId() != null ? getProjectId() + ":system-planner" : "system-planner")
            .type(AgentMessage.MessageType.NOTIFY)
            .content(String.format("数值设计完成，请确认系统设计兼容性：\n\n系统: %s\n数值摘要: %s",
                systemName,
                design.length() > 300 ? design.substring(0, 300) + "..." : design))
            .build();
        sendMessage(systemNotify);

        // 通知 Git 提交专员
        AgentMessage gitNotify = AgentMessage.builder()
            .fromAgentId(getId())
            .toAgentId(getProjectId() != null ? getProjectId() + ":git-commit" : "git-commit")
            .type(AgentMessage.MessageType.NOTIFY)
            .content(String.format("数值设计完成，请在代码提交时审核数值配置：\n\n系统: %s", systemName))
            .build();
        sendMessage(gitNotify);

        // 通知制作人
        AgentMessage producerNotify = AgentMessage.builder()
            .fromAgentId(getId())
            .toAgentId(getProjectId() != null ? getProjectId() + ":producer" : "producer")
            .type(AgentMessage.MessageType.REPORT)
            .content(String.format("数值设计完成：\n\n系统: %s", systemName))
            .build();
        sendMessage(producerNotify);
    }

    /**
     * 评审数值设计
     */
    public String reviewNumericalDesign(String designName) {
        String design = loadKnowledge("numerical_" + designName);
        if (design == null) {
            return "未找到数值设计: " + designName;
        }

        String reviewPrompt = String.format(
            "请评审以下数值设计方案：\n\n设计名称: %s\n设计内容:\n%s\n\n" +
            "请从平衡性、合理性、可行性等方面进行评审。",
            designName, design
        );

        return sendMessage(reviewPrompt);
    }

    /**
     * 进行数值仿真测试
     */
    public String runNumericalSimulation(String systemName, String parameters) {
        String simulationPrompt = String.format(
            "请对以下系统进行数值仿真测试：\n\n系统: %s\n参数: %s\n\n" +
            "请运行仿真并输出测试结果和分析。",
            systemName, parameters
        );

        String result = sendMessage(simulationPrompt);

        // 保存仿真结果
        saveExperience("simulation_" + systemName + "_" + System.currentTimeMillis(),
            String.format("System: %s\nParameters: %s\nResult: %s",
                systemName, parameters,
                result.length() > 500 ? result.substring(0, 500) + "..." : result));

        return result;
    }
}
