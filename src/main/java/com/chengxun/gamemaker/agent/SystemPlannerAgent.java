package com.chengxun.gamemaker.agent;

import com.chengxun.gamemaker.engine.ClaudeCliEngine;
import com.chengxun.gamemaker.engine.MessageBus;
import com.chengxun.gamemaker.manager.*;
import com.chengxun.gamemaker.model.*;
import com.chengxun.gamemaker.service.PlayerExperienceAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class SystemPlannerAgent extends BaseAgent {

    /** 上次协调时间，避免每轮都调用 Claude CLI */
    private LocalDateTime lastCoordinationTime;

    /** 上次更新设计文档时间 */
    private LocalDateTime lastDesignUpdateTime;

    /** 协调冷却时间（分钟）：至少间隔这么久才再次协调 */
    private static final int COORDINATION_COOLDOWN_MINUTES = 30;

    /** 设计文档更新冷却时间（分钟） */
    private static final int DESIGN_UPDATE_COOLDOWN_MINUTES = 60;

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

    /** 上次提出改进方案时间 */
    private LocalDateTime lastImprovementTime;

    /** 改进方案冷却时间（分钟） */
    private static final int IMPROVEMENT_COOLDOWN_MINUTES = 45;

    /** 玩家体验分析器（延迟注入） */
    private com.chengxun.gamemaker.service.PlayerExperienceAnalyzer playerExperienceAnalyzer;

    /** 设置玩家体验分析器 */
    public void setPlayerExperienceAnalyzer(com.chengxun.gamemaker.service.PlayerExperienceAnalyzer analyzer) {
        this.playerExperienceAnalyzer = analyzer;
    }

    @Override
    protected void doWork() {
        log.info("SystemPlannerAgent working...");

        // 检查待处理的设计任务
        processPendingTasks();

        // 如果有工作流分配的待处理/进行中任务，跳过协调和文档更新，专注完成任务
        boolean hasActiveTask = tasks.stream()
            .anyMatch(t -> t.getStatus() == TaskAssignment.TaskStatus.PENDING
                || t.getStatus() == TaskAssignment.TaskStatus.IN_PROGRESS);
        if (hasActiveTask) {
            log.debug("有活跃任务，跳过协调和文档更新");
            return;
        }

        // 协调与其他 Agent 的工作（带冷却时间，避免每轮都调用 Claude CLI）
        if (shouldCoordinate()) {
            coordinateWithAgents();
            lastCoordinationTime = LocalDateTime.now();
        }

        // 更新设计文档（带冷却时间）
        if (shouldUpdateDesign()) {
            updateDesignDocuments();
            lastDesignUpdateTime = LocalDateTime.now();
        }

        // 主动提出设计改进（每期都要提出更好玩的内容）
        if (shouldProposeImprovement()) {
            proposeDesignImprovement();
            lastImprovementTime = LocalDateTime.now();
        }
    }

    /**
     * 判断是否需要提出改进方案
     */
    private boolean shouldProposeImprovement() {
        if (currentProject == null || !currentProject.hasGoal()) return false;

        // 检查项目是否在进行中
        if (currentProject.getGoalStatus() != GameProject.GoalStatus.IN_PROGRESS) return false;

        // 冷却时间检查
        if (lastImprovementTime != null) {
            if (!LocalDateTime.now().isAfter(lastImprovementTime.plusMinutes(IMPROVEMENT_COOLDOWN_MINUTES))) {
                return false;
            }
        }

        return true;
    }

    /**
     * 主动提出设计改进方案
     * 每个迭代周期都要思考如何让游戏更好玩
     */
    private void proposeDesignImprovement() {
        if (currentProject == null) return;

        log.info("系统策划主动提出设计改进方案");

        String improvementPrompt = buildImprovementPrompt();
        String response = sendMessage(improvementPrompt);

        if (response != null && !response.isEmpty()) {
            // 保存改进方案
            saveKnowledge("improvement_proposal_" + System.currentTimeMillis(), response);

            // 通知制作人
            notifyProducerForImprovement(response);

            log.info("设计改进方案已提出并通知制作人");
        }
    }

    /**
     * 构建改进方案提示词
     */
    private String buildImprovementPrompt() {
        StringBuilder sb = new StringBuilder();

        sb.append("## 设计改进任务\n\n");
        sb.append("你是一个游戏系统策划，需要为当前项目提出设计改进方案，让游戏更好玩、更有吸引力。\n\n");

        // 项目信息
        sb.append("### 项目信息\n");
        sb.append("- 项目名称: ").append(currentProject.getName()).append("\n");
        sb.append("- 项目目标: ").append(currentProject.getGoal()).append("\n");
        sb.append("- 当前版本: ").append(currentProject.getVersion()).append("\n");
        sb.append("- 目标进度: ").append(currentProject.getGoalProgress()).append("%\n\n");

        // 当前里程碑
        if (currentProject.getMilestones() != null) {
            sb.append("### 当前迭代状态\n");
            for (var m : currentProject.getMilestones()) {
                sb.append("- [").append(m.getStatus()).append("] ").append(m.getTitle());
                if (m.getStatus() == GameProject.MilestoneStatus.IN_PROGRESS) {
                    sb.append(" ← 当前迭代");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        // 已有设计文档
        String existingDesigns = loadKnowledge("design_documents");
        if (existingDesigns != null) {
            sb.append("### 已有设计\n");
            sb.append(existingDesigns.length() > 1000 ?
                existingDesigns.substring(0, 1000) + "...\n" : existingDesigns + "\n");
            sb.append("\n");
        }

        // 历史改进经验
        String improvementHistory = loadExperience("improvement_proposals");
        if (improvementHistory != null) {
            sb.append("### 历史改进方案\n");
            sb.append(improvementHistory.length() > 500 ?
                improvementHistory.substring(0, 500) + "...\n" : improvementHistory + "\n");
            sb.append("\n");
        }

        // 【新增】玩家体验评分 — 从玩家视角找出最需要改进的地方
        if (playerExperienceAnalyzer != null) {
            try {
                PlayerExperienceAnalyzer.FunScore funScore = playerExperienceAnalyzer.analyzeProject(currentProject);
                sb.append("### 玩家体验诊断\n\n");
                sb.append(String.format("综合趣味度: **%d/100**\n", funScore.getOverallScore()));
                sb.append(String.format("- 核心循环: %d | 挑战感: %d | 奖励反馈: %d | 进度感: %d | 新颖度: %d\n\n",
                    funScore.getCoreLoopScore(), funScore.getChallengeScore(),
                    funScore.getRewardScore(), funScore.getProgressionScore(), funScore.getNoveltyScore()));

                if (!funScore.getPainPoints().isEmpty()) {
                    sb.append("**预测痛点（优先解决）：**\n");
                    for (String point : funScore.getPainPoints()) {
                        sb.append("- ").append(point).append("\n");
                    }
                    sb.append("\n");
                }

                if (!funScore.getImprovements().isEmpty()) {
                    sb.append("**改进建议（按影响度排序）：**\n");
                    for (String improvement : funScore.getImprovements()) {
                        sb.append("- ").append(improvement).append("\n");
                    }
                    sb.append("\n");
                }

                // 竞品参考
                String competitorRef = playerExperienceAnalyzer.getCompetitorReference(
                    currentProject.getGoal() != null ? currentProject.getGoal() : "");
                if (!competitorRef.isEmpty()) {
                    sb.append(competitorRef);
                }
            } catch (Exception e) {
                log.debug("玩家体验分析失败: {}", e.getMessage());
            }
        }

        sb.append("## 请提出改进方案\n\n");
        sb.append("请从以下角度思考如何让游戏更好玩：\n\n");
        sb.append("1. **核心玩法改进**: 核心循环是否足够有趣？有什么可以增强的？\n");
        sb.append("2. **新功能建议**: 添加什么新功能能让游戏更有吸引力？\n");
        sb.append("3. **体验优化**: 哪些地方的用户体验可以改善？\n");
        sb.append("4. **内容丰富**: 可以增加什么内容让游戏更丰富？\n");
        sb.append("5. **社交互动**: 是否可以增加社交/竞技元素？\n\n");
        sb.append("### 输出格式\n\n");
        sb.append("请输出3-5个具体的改进方案，每个方案包含：\n");
        sb.append("- 方案名称\n");
        sb.append("- 预期效果\n");
        sb.append("- 实现难度（低/中/高）\n");
        sb.append("- 优先级建议\n\n");
        sb.append("请给出具体、可执行的改进方案。");

        return sb.toString();
    }

    /**
     * 通知制作人改进方案
     */
    private void notifyProducerForImprovement(String improvementProposal) {
        String producerId = getProjectId() + ":producer";

        String message = String.format(
            "【设计改进提案】\n\n" +
            "系统策划提出了以下改进方案，让游戏更好玩：\n\n%s\n\n" +
            "请评估这些方案并决定是否纳入下一个迭代。",
            improvementProposal.length() > 1500 ?
                improvementProposal.substring(0, 1500) + "..." : improvementProposal
        );

        AgentMessage agentMessage = AgentMessage.builder()
            .fromAgentId(getId())
            .toAgentId(producerId)
            .type(AgentMessage.MessageType.REPORT)
            .content(message)
            .build();
        sendMessage(agentMessage);

        log.info("已通知制作人设计改进方案");
    }

    /**
     * 判断是否需要协调：有未处理的协调消息 或 超过冷却时间
     */
    private boolean shouldCoordinate() {
        // 有来自其他 Agent 的未处理消息时，需要协调
        boolean hasUnprocessedMessages = !getPendingMessages().isEmpty();
        if (hasUnprocessedMessages) {
            return true;
        }
        // 超过冷却时间后才允许再次协调
        if (lastCoordinationTime == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(lastCoordinationTime.plusMinutes(COORDINATION_COOLDOWN_MINUTES));
    }

    /**
     * 判断是否需要更新设计文档：有新的设计输入 或 超过冷却时间
     */
    private boolean shouldUpdateDesign() {
        if (lastDesignUpdateTime == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(lastDesignUpdateTime.plusMinutes(DESIGN_UPDATE_COOLDOWN_MINUTES));
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

        log.info("SystemPlanner pending tasks: {}, total tasks: {}", pendingTasks.size(), tasks.size());
        for (TaskAssignment t : tasks) {
            log.debug("  Task: {} status={}", t.getTitle(), t.getStatus());
        }

        if (!pendingTasks.isEmpty()) {
            TaskAssignment task = pendingTasks.get(0);
            workOnTask(task);
        }
    }

    /**
     * 执行设计任务
     */
    private void workOnTask(TaskAssignment task) {
        log.info("Working on design task: {} (taskId={})", task.getTitle(), task.getId());

        task.setStatus(TaskAssignment.TaskStatus.IN_PROGRESS);
        task.setUpdatedAt(LocalDateTime.now());
        agentContext.setCurrentTaskId(task.getId());

        try {
            String prompt = buildDesignPrompt(task);
            log.info("Sending design prompt to Claude CLI (length={})", prompt.length());
            String result = sendMessage(prompt);
            log.info("Received design result from Claude CLI (length={})", result != null ? result.length() : 0);

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
        } catch (Exception e) {
            log.error("Design task failed: {} (taskId={})", task.getTitle(), task.getId(), e);
            task.setStatus(TaskAssignment.TaskStatus.FAILED);
            task.setError(e.getMessage());
            task.setCompletedAt(LocalDateTime.now());
            agentContext.setCurrentTaskId(null);
        }
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
     * 只有当存在实际待协调事项时才返回内容，否则返回空字符串避免无意义的 Claude CLI 调用
     */
    private String buildCoordinationPrompt() {
        // 加载待协调事项
        String pendingCoordination = loadKnowledge("pending_coordination");

        // 如果没有待协调事项，不生成协调提示
        if (pendingCoordination == null || pendingCoordination.trim().isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## 团队协调任务\n\n");
        sb.append("## 待协调事项\n\n").append(pendingCoordination).append("\n\n");
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
