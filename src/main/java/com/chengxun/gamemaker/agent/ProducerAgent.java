package com.chengxun.gamemaker.agent;

import com.chengxun.gamemaker.engine.ClaudeCliEngine;
import com.chengxun.gamemaker.engine.MessageBus;
import com.chengxun.gamemaker.feishu.FeishuBotService;
import com.chengxun.gamemaker.manager.*;
import com.chengxun.gamemaker.model.*;
import com.chengxun.gamemaker.service.GoalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ProducerAgent extends BaseAgent {

    private static final Logger log = LoggerFactory.getLogger(ProducerAgent.class);

    private final AgentManager agentManager;
    private final FeishuBotService feishuService;

    /** 目标管理服务（延迟注入） */
    @Autowired(required = false)
    private GoalService goalService;

    public ProducerAgent(AgentDefinition definition,
                        ClaudeCliEngine cliEngine,
                        MessageBus messageBus,
                        ContextManager contextManager,
                        MemoryManager memoryManager,
                        SkillManager skillManager,
                        ProjectManager projectManager,
                        AgentManager agentManager,
                        FeishuBotService feishuService) {
        super(definition, cliEngine, messageBus, contextManager, memoryManager, skillManager, projectManager);
        this.agentManager = agentManager;
        this.feishuService = feishuService;
    }

    /**
     * 设置目标管理服务
     */
    public void setGoalService(GoalService goalService) {
        this.goalService = goalService;
    }

    @Override
    protected void doWork() {
        log.info("Producer working...");

        // 目标驱动：如果项目有目标，驱动迭代
        if (currentProject != null && currentProject.hasGoal() && goalService != null) {
            driveGoalIteration();
        }

        // 自动优化团队
        autoOptimizeTeam();

        String teamStatus = getTeamStatus();
        processApprovals();
        generateWorkInstructions();
        reportToUser(teamStatus);

        saveProjectStatus(teamStatus);
    }

    /**
     * 驱动目标迭代 — 核心循环
     * 检查目标状态，分解任务，分配给 Agent，检查进度
     */
    private void driveGoalIteration() {
        String projectId = getProjectId();
        if (projectId == null) return;

        GameProject.GoalStatus goalStatus = currentProject.getGoalStatus();

        switch (goalStatus) {
            case NOT_STARTED -> {
                // 目标未开始，开始分解
                log.info("Goal not started, decomposing: {}", currentProject.getGoal());
                decomposeGoal();
            }
            case DECOMPOSING -> {
                // 正在分解中，等待完成
                log.info("Goal decomposing...");
            }
            case IN_PROGRESS -> {
                // 进行中，检查进度并分配任务
                checkAndAssignMilestoneTasks();
                checkGoalProgress();
            }
            case REVIEW -> {
                // 审查中，等待人工确认
                log.info("Goal in review, waiting for confirmation");
            }
            case PAUSED -> {
                log.info("Goal is paused");
            }
            case COMPLETED -> {
                log.info("Goal is completed");
            }
        }
    }

    /**
     * 分解目标为里程碑
     * 使用 AI 分析目标，生成里程碑和任务
     */
    private void decomposeGoal() {
        String projectId = getProjectId();
        if (projectId == null || goalService == null) return;

        // 标记为分解中
        currentProject.setGoalStatus(GameProject.GoalStatus.DECOMPOSING);
        projectManager.saveProjectConfig(currentProject);
        saveContext();

        String goalPrompt = String.format(
            "你是一个项目管理专家。请分析以下项目目标，将其分解为可执行的里程碑。\n\n" +
            "项目名称：%s\n" +
            "项目目标：%s\n" +
            "目标类型：%s\n\n" +
            "请按以下格式输出里程碑（每个里程碑一行）：\n" +
            "MILESTONE: 顺序号 | 标题 | 描述 | 负责角色(server-dev/client-dev/ui-dev/system-planner/numerical-planner/tester/git-commit) | 依赖的里程碑序号(逗号分隔,无依赖填0)\n\n" +
            "示例：\n" +
            "MILESTONE: 1 | 系统设计 | 设计游戏核心系统架构 | system-planner | 0\n" +
            "MILESTONE: 2 | 服务端开发 | 实现后端逻辑和API | server-dev | 1\n\n" +
            "请只输出 MILESTONE 行，不要有其他内容。",
            currentProject.getName(),
            currentProject.getGoal(),
            currentProject.getGoalType() != null ? currentProject.getGoalType().name() : "CUSTOM"
        );

        String response = sendMessage(goalPrompt);
        if (response == null || response.isEmpty()) {
            log.warn("Failed to decompose goal, will retry next cycle");
            currentProject.setGoalStatus(GameProject.GoalStatus.NOT_STARTED);
            return;
        }

        // 解析里程碑
        parseAndSaveMilestones(projectId, response);
    }

    /**
     * 解析 AI 返回的里程碑并保存
     * 容错处理：支持多种分隔符、缺失字段、格式变体
     */
    private void parseAndSaveMilestones(String projectId, String response) {
        String[] lines = response.split("\n");
        int milestoneCount = 0;
        int autoOrder = 0;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // 兼容多种格式：MILESTONE:、Milestone:、里程碑：
            String marker = null;
            if (line.toUpperCase().contains("MILESTONE:")) {
                marker = "MILESTONE:";
            } else if (line.contains("里程碑：") || line.contains("里程碑:")) {
                marker = line.contains("里程碑：") ? "里程碑：" : "里程碑:";
            }
            if (marker == null) continue;

            try {
                String content = line.substring(line.indexOf(marker) + marker.length()).trim();

                // 支持多种分隔符：|、，、,、Tab
                String[] parts = content.split("[|，,\t]+");
                if (parts.length < 2) {
                    log.warn("Milestone line too short, skipping: {}", line);
                    continue;
                }

                // 解析顺序号（如果有的话）
                int order = ++autoOrder;
                int titleIdx = 0;
                try {
                    order = Integer.parseInt(parts[0].trim());
                    titleIdx = 1;
                } catch (NumberFormatException e) {
                    // 第一个字段不是数字，说明没有顺序号
                }

                String title = parts[titleIdx].trim();
                if (title.isEmpty()) continue;

                String description = (parts.length > titleIdx + 1) ? parts[titleIdx + 1].trim() : title;
                String role = (parts.length > titleIdx + 2) ? normalizeRole(parts[titleIdx + 2].trim()) : "server-dev";

                List<String> dependencies = new java.util.ArrayList<>();
                if (parts.length > titleIdx + 3) {
                    String deps = parts[titleIdx + 3].trim();
                    if (!deps.isEmpty() && !"0".equals(deps) && !"无".equals(deps) && !"none".equalsIgnoreCase(deps)) {
                        for (String dep : deps.split("[,，]")) {
                            String d = dep.trim().replaceAll("[^0-9]", "");
                            if (!d.isEmpty()) dependencies.add(d);
                        }
                    }
                }

                goalService.addMilestone(projectId, title, description, role, order, dependencies);
                milestoneCount++;
                log.info("Parsed milestone {}: {} ({})", order, title, role);

            } catch (Exception e) {
                log.warn("Failed to parse milestone line: {} - {}", line, e.getMessage());
            }
        }

        if (milestoneCount > 0) {
            currentProject.setGoalStatus(GameProject.GoalStatus.IN_PROGRESS);
            log.info("Goal decomposed into {} milestones for project: {}", milestoneCount, projectId);
        } else {
            currentProject.setGoalStatus(GameProject.GoalStatus.NOT_STARTED);
            log.warn("No milestones parsed from response, will retry. Response: {}",
                response.length() > 200 ? response.substring(0, 200) + "..." : response);
        }

        projectManager.saveProjectConfig(currentProject);
        saveContext();
    }

    /**
     * 角色名称标准化（兼容中文和英文）
     */
    private String normalizeRole(String role) {
        return switch (role.toLowerCase()) {
            case "服务端", "后端", "server", "backend" -> "server-dev";
            case "客户端", "前端", "client", "frontend" -> "client-dev";
            case "ui", "美术", "界面" -> "ui-dev";
            case "策划", "系统策划", "planner", "system" -> "system-planner";
            case "数值", "数值策划", "numerical" -> "numerical-planner";
            case "测试", "qa", "tester", "test" -> "tester";
            case "git", "版本", "version" -> "git-commit";
            case "制作人", "producer", "pm" -> "producer";
            default -> "server-dev";
        };
    }

    /**
     * 检查里程碑并将任务分配给对应 Agent
     * 如果需要的 Agent 不存在，自动招聘
     */
    private void checkAndAssignMilestoneTasks() {
        String projectId = getProjectId();
        if (projectId == null || goalService == null) return;

        GameProject.GoalMilestone next = goalService.getNextExecutableMilestone(projectId);
        if (next == null) return;

        // 查找对应的 Agent
        String targetAgentId = projectId + ":" + next.getAssignedAgentRole();
        Agent targetAgent = agentManager.getAgent(targetAgentId);

        // 如果 Agent 不存在，自动招聘
        if (targetAgent == null) {
            log.info("Agent not found for milestone {}, auto-recruiting: {}", next.getId(), next.getAssignedAgentRole());
            targetAgent = autoRecruitAgent(next.getAssignedAgentRole(), next.getTitle());
            if (targetAgent == null) {
                log.error("Failed to auto-recruit agent for role: {}", next.getAssignedAgentRole());
                return;
            }
        }

        // 如果里程碑没有任务，生成任务
        if (next.getTasks().isEmpty()) {
            generateMilestoneTasks(projectId, next);
        }

        // 将待执行的任务发送给 Agent
        for (GameProject.MilestoneTask task : next.getTasks()) {
            if (task.getStatus() == GameProject.MilestoneStatus.PENDING) {
                // 发送任务消息
                AgentMessage taskMsg = AgentMessage.builder()
                    .fromAgentId(getId())
                    .toAgentId(targetAgentId)
                    .type(AgentMessage.MessageType.TASK)
                    .content(String.format("[里程碑任务] %s\n\n里程碑: %s\n任务: %s\n\n完成后请汇报进度。",
                        currentProject.getGoal(), next.getTitle(), task.getDescription()))
                    .build();
                sendMessage(taskMsg);

                // 标记为进行中
                goalService.updateTaskStatus(projectId, next.getId(), task.getId(),
                    GameProject.MilestoneStatus.IN_PROGRESS, null);
            }
        }

        // 更新里程碑状态
        if (next.getStatus() == GameProject.MilestoneStatus.PENDING) {
            goalService.updateMilestoneProgress(projectId, next.getId(), 1);
        }
    }

    /**
     * 为里程碑生成具体任务
     * 容错处理：兼容多种格式
     */
    private void generateMilestoneTasks(String projectId, GameProject.GoalMilestone milestone) {
        String taskPrompt = String.format(
            "请为以下里程碑生成3-5个具体可执行的任务：\n\n" +
            "里程碑：%s\n" +
            "描述：%s\n" +
            "负责角色：%s\n\n" +
            "请每行输出一个任务，格式：\n" +
            "TASK: 任务描述\n\n" +
            "也可以用 - 或数字编号开头。请只输出任务行。",
            milestone.getTitle(),
            milestone.getDescription() != null ? milestone.getDescription() : "无",
            milestone.getAssignedAgentRole()
        );

        String response = sendMessage(taskPrompt);
        if (response == null) return;

        int taskCount = 0;
        for (String line : response.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // 兼容多种格式：TASK:、Task:、任务：、-、1.、1）
            String taskDesc = null;
            if (line.toUpperCase().contains("TASK:")) {
                taskDesc = line.substring(line.toUpperCase().indexOf("TASK:") + "TASK:".length()).trim();
            } else if (line.contains("任务：") || line.contains("任务:")) {
                String marker = line.contains("任务：") ? "任务：" : "任务:";
                taskDesc = line.substring(line.indexOf(marker) + marker.length()).trim();
            } else if (line.matches("^[-*]\\s+.*")) {
                taskDesc = line.replaceFirst("^[-*]\\s+", "").trim();
            } else if (line.matches("^\\d+[.、)）]\\s*.*")) {
                taskDesc = line.replaceFirst("^\\d+[.、)）]\\s*", "").trim();
            }

            if (taskDesc != null && !taskDesc.isEmpty()) {
                goalService.addTask(projectId, milestone.getId(), taskDesc);
                taskCount++;
            }
        }

        if (taskCount == 0) {
            log.warn("No tasks generated for milestone: {}. Response: {}",
                milestone.getTitle(),
                response.length() > 200 ? response.substring(0, 200) + "..." : response);
        }
    }

    /**
     * 检查目标整体进度
     */
    private void checkGoalProgress() {
        String projectId = getProjectId();
        if (projectId == null || goalService == null) return;

        // 重新加载项目获取最新状态
        GameProject project = projectManager.getProject(projectId);
        if (project == null) return;

        // 检查是否应该进入审查
        if (goalService.shouldEnterReview(projectId)) {
            project.setGoalStatus(GameProject.GoalStatus.REVIEW);
            projectManager.saveProjectConfig(project);

            // 通知用户目标已完成，等待确认
            if (feishuService.isEnabled()) {
                feishuService.sendMessage(String.format(
                    "项目 [%s] 的目标已全部完成！\n\n目标: %s\n\n请确认验收。",
                    project.getName(), project.getGoal()
                ));
            }
            log.info("Goal entered review for project: {}", projectId);
        }

        // 检查截止时间
        if (project.getGoalDeadline() != null && LocalDateTime.now().isAfter(project.getGoalDeadline())) {
            log.warn("Goal deadline passed for project: {}", projectId);
            saveExperience("goal_deadline_passed_" + projectId,
                "项目目标截止时间已过: " + project.getGoalDeadline());
        }
    }

    @Override
    protected void handleMessage(AgentMessage message) {
        switch (message.getType()) {
            case REPORT -> handleReport(message);
            case QUERY -> handleQuery(message);
            case APPROVAL -> handleApprovalRequest(message);
            case RESPONSE -> handleResponse(message);
            case REVIEW -> handleReview(message);
            default -> log.info("Producer received message: {}", message.getType());
        }
    }

    private String getTeamStatus() {
        // 只获取当前项目下的 Agent（项目级隔离）
        String projectId = getProjectId();
        List<Agent> agents = projectId != null ?
            agentManager.getAgentsByProject(projectId) :
            agentManager.getAllAgents();

        StringBuilder sb = new StringBuilder("**团队状态报告**\n\n");

        if (currentProject != null) {
            sb.append("**项目**: ").append(currentProject.getName()).append("\n");
            sb.append("**工作目录**: ").append(currentProject.getWorkDir()).append("\n\n");
        }

        for (Agent agent : agents) {
            sb.append(String.format("- **%s** (%s): %s\n",
                agent.getName(),
                agent.getRole(),
                agent.isBusy() ? "忙碌" : "空闲"));
        }

        return sb.toString();
    }

    private void processApprovals() {
        List<AgentMessage> approvals = pendingMessages.stream()
            .filter(m -> m.getType() == AgentMessage.MessageType.APPROVAL)
            .collect(Collectors.toList());

        for (AgentMessage approval : approvals) {
            if (feishuService.isEnabled()) {
                feishuService.sendApprovalRequest(approval.getContent());
            } else {
                log.info("Feishu not enabled, approval request logged: {}", approval.getContent());
            }
        }
    }

    private void generateWorkInstructions() {
        log.info("Generating work instructions...");
        String skillPrompt = buildSkillPrompt("制定工作计划和任务分配");
        if (!skillPrompt.isEmpty()) {
            String instructions = sendMessage(skillPrompt);
            saveKnowledge("work_instructions", instructions);
        }
    }

    private void reportToUser(String status) {
        if (feishuService.isEnabled()) {
            feishuService.sendMessage(status);
        }
    }

    private void saveProjectStatus(String status) {
        agentContext.setProjectSummary(status);
        saveContext();
    }

    private void handleReport(AgentMessage message) {
        log.info("Received report from {}: {}", message.getFromAgentId(), message.getContent());
        saveMemory("last_report_" + message.getFromAgentId(), message.getContent());
        saveKnowledge("team_report_" + System.currentTimeMillis(), message.getContent());
    }

    private void handleQuery(AgentMessage message) {
        log.info("Received query from {}: {}", message.getFromAgentId(), message.getContent());
        String response = processQuery(message.getContent());
        AgentMessage responseMsg = AgentMessage.builder()
            .fromAgentId(getId())
            .toAgentId(message.getFromAgentId())
            .type(AgentMessage.MessageType.RESPONSE)
            .content(response)
            .build();
        sendMessage(responseMsg);
    }

    private void handleApprovalRequest(AgentMessage message) {
        log.info("Approval request from {}: {}", message.getFromAgentId(), message.getContent());
        if (feishuService.isEnabled()) {
            feishuService.sendApprovalRequest(
                String.format("来自 %s 的审批请求:\n%s",
                    message.getFromAgentId(),
                    message.getContent())
            );
        } else {
            log.info("Feishu not enabled, approval request logged: {}", message.getContent());
        }
    }

    private void handleResponse(AgentMessage message) {
        log.info("Received response from {}: {}", message.getFromAgentId(), message.getContent());
    }

    private void handleReview(AgentMessage message) {
        log.info("Received review request from {}: {}", message.getFromAgentId(), message.getContent());

        // 制作人处理审查请求，主要是协调和决策
        String reviewPrompt = buildSkillPrompt("审查和协调");
        String fullPrompt = reviewPrompt + "\n\n请审查以下内容并给出协调建议：\n\n" + message.getContent();
        String reviewResult = sendMessage(fullPrompt);

        // 保存审查记录
        saveExperience("review_" + System.currentTimeMillis(),
            String.format("Review from %s: %s\nResult: %s",
                message.getFromAgentId(), message.getContent(),
                reviewResult.length() > 500 ? reviewResult.substring(0, 500) + "..." : reviewResult));

        // 返回审查结果
        AgentMessage response = AgentMessage.builder()
            .fromAgentId(getId())
            .toAgentId(message.getFromAgentId())
            .type(AgentMessage.MessageType.RESPONSE)
            .content(reviewResult)
            .build();
        sendMessage(response);
    }

    private String processQuery(String query) {
        String skillPrompt = buildSkillPrompt("回答问题和查询");
        String fullPrompt = skillPrompt + "\n\n请回答以下问题: " + query;
        return sendMessage(fullPrompt);
    }

    public String requestHiring(String role, String requirements) {
        String hiringPlan = String.format(
            "**招聘请求**\n\n" +
            "角色: %s\n" +
            "要求:\n%s\n\n" +
            "请确认是否批准此次招聘。",
            role, requirements
        );

        if (feishuService.isEnabled()) {
            feishuService.sendApprovalRequest(hiringPlan);
        } else {
            log.info("Feishu not enabled, hiring request logged: {}", hiringPlan);
        }
        saveExperience("hiring_" + role, hiringPlan);
        return "招聘请求已发送，等待批准";
    }

    /**
     * 自动招聘 Agent
     * 当里程碑需要某个角色但没有对应 Agent 时，自动招聘
     *
     * @param role 需要的角色
     * @param taskDescription 任务描述
     * @return 创建的 Agent，如果创建失败返回 null
     */
    private Agent autoRecruitAgent(String role, String taskDescription) {
        String projectId = getProjectId();
        if (projectId == null) return null;

        log.info("Auto-recruiting agent for role: {} in project: {}", role, projectId);

        // 生成 Agent 名称
        String name = generateAgentName(role);

        // 使用项目全局 API 配置
        GameProject project = projectManager.getProject(projectId);
        String workDir = project != null ? project.getWorkDir() : null;

        try {
            Agent agent = createAgent(name, role, null, workDir);

            // 如果项目有全局 API 配置，自动分配
            if (agent != null && project != null) {
                String globalApiKey = project.getMetadata().get("globalApiKey");
                String globalApiUrl = project.getMetadata().get("globalApiUrl");
                String globalModel = project.getMetadata().get("globalModel");

                if (globalApiKey != null && !globalApiKey.isEmpty()) {
                    agent.getDefinition().setApiKey(globalApiKey);
                }
                if (globalApiUrl != null && !globalApiUrl.isEmpty()) {
                    agent.getDefinition().setApiUrl(globalApiUrl);
                }
                if (globalModel != null && !globalModel.isEmpty()) {
                    agent.getDefinition().setModel(globalModel);
                }
            }

            // 通知管理员
            if (feishuService != null && feishuService.isEnabled()) {
                feishuService.sendMessage(String.format(
                    "🤖 自动招聘通知\n\n项目: %s\n角色: %s\n名称: %s\n任务: %s",
                    projectId, role, name, taskDescription
                ));
            }

            log.info("Auto-recruited agent: {} ({}) for project: {}", name, role, projectId);
            return agent;

        } catch (Exception e) {
            log.error("Failed to auto-recruit agent for role: {}", role, e);
            return null;
        }
    }

    /**
     * 生成 Agent 名称
     */
    private String generateAgentName(String role) {
        String prefix = switch (role) {
            case "server-dev" -> "服务端开发";
            case "client-dev" -> "客户端开发";
            case "ui-dev" -> "UI设计";
            case "system-planner" -> "系统策划";
            case "numerical-planner" -> "数值策划";
            case "tester" -> "测试工程师";
            case "git-commit" -> "Git专员";
            default -> role;
        };
        return prefix + "-" + System.currentTimeMillis() % 10000;
    }

    /**
     * 自动优化团队
     * 检查团队成员状态，自动调整团队配置
     */
    private void autoOptimizeTeam() {
        String projectId = getProjectId();
        if (projectId == null) return;

        List<Agent> agents = agentManager.getAgentsByProject(projectId);
        GameProject project = projectManager.getProject(projectId);
        if (project == null) return;

        // 检查是否有长期空闲的 Agent
        for (Agent agent : agents) {
            if ("producer".equals(agent.getRole())) continue; // 跳过制作人

            // 如果 Agent 长期空闲且没有任务，考虑解雇
            if (!agent.isBusy() && agent.getPendingMessages().isEmpty()) {
                // 检查是否有待执行的里程碑任务需要这个角色
                boolean needed = isRoleNeeded(projectId, agent.getRole());
                if (!needed) {
                    log.info("Agent {} ({}) is idle and not needed, considering dismissal", agent.getName(), agent.getRole());
                    // 这里可以添加自动解雇逻辑，但为了安全起见，先记录日志
                    saveExperience("idle_agent_" + agent.getId(),
                        String.format("Agent %s (%s) is idle and may not be needed", agent.getName(), agent.getRole()));
                }
            }
        }
    }

    /**
     * 检查角色是否被需要
     */
    private boolean isRoleNeeded(String projectId, String role) {
        if (goalService == null) return true; // 如果没有目标服务，默认认为需要

        // 检查是否有下一个可执行的里程碑需要这个角色
        GameProject.GoalMilestone nextMilestone = goalService.getNextExecutableMilestone(projectId);
        if (nextMilestone != null && role.equals(nextMilestone.getAssignedAgentRole())) {
            return true;
        }

        // 检查项目目标状态
        GameProject project = projectManager.getProject(projectId);
        if (project != null && project.getGoalStatus() == GameProject.GoalStatus.IN_PROGRESS) {
            // 项目进行中，默认认为需要所有已招聘的角色
            return true;
        }

        return false;
    }

    /**
     * 创建 Agent（纯执行，审批由能力系统拦截器处理）
     *
     * @param name      Agent 名称
     * @param role      Agent 角色
     * @param agentsFile Agent 配置文件
     * @param workDir   工作目录
     * @return 创建的 Agent
     */
    public Agent createAgent(String name, String role, String agentsFile, String workDir) {
        String projectId = getProjectId();

        AgentDefinition newDef = AgentDefinition.builder()
            .id(role.toLowerCase().replace(" ", "-") + "-" + System.currentTimeMillis())
            .name(name)
            .role(role)
            .agentsFile(agentsFile)
            .workDir(workDir)
            .projectId(projectId)
            .status(AgentDefinition.AgentStatus.IDLE)
            .build();

        saveExperience("create_agent_" + newDef.getEffectiveId(),
            String.format("Created agent: %s (%s) for project: %s, workDir: %s", name, role, projectId, workDir));

        return agentManager.createAgent(newDef);
    }

    /**
     * 分配 API 配置（纯执行，审批由能力系统拦截器处理）
     *
     * @param agentId Agent ID
     * @param apiKey  API Key
     * @param apiUrl  API URL
     * @param model   模型
     */
    public void assignApiConfig(String agentId, String apiKey, String apiUrl, String model) {
        Agent agent = agentManager.getAgent(agentId);
        if (agent != null) {
            createSnapshot();

            if (apiKey != null) agent.getDefinition().setApiKey(apiKey);
            if (apiUrl != null) agent.getDefinition().setApiUrl(apiUrl);
            if (model != null) agent.getDefinition().setModel(model);

            if (apiUrl != null || model != null) {
                agentContext.addApiConfig(apiUrl, model);
            }
            agent.saveContext();

            log.info("Assigned API config to agent: {}", agentId);

            if (agent instanceof BaseAgent baseAgent) {
                baseAgent.recoverContext();
            }
        }
    }

    public void assignWorkDir(String agentId, String workDir) {
        Agent agent = agentManager.getAgent(agentId);
        if (agent != null) {
            createSnapshot();

            agent.getDefinition().setWorkDir(workDir);

            if (agent instanceof BaseAgent baseAgent) {
                baseAgent.initProject();
                baseAgent.loadProjectSkills();
                baseAgent.recoverContext();
            }

            agent.saveContext();
            log.info("Assigned workDir to agent: {} -> {}", agentId, workDir);
        }
    }

    public void notifyQuotaExhausted(String agentId) {
        if (feishuService.isEnabled()) {
            feishuService.sendMessage(
                String.format("⚠️ Agent %s 的 API 配额不足，请及时更换或重置。", agentId)
            );
        } else {
            log.warn("Agent {} 的 API 配额不足", agentId);
        }
        saveExperience("quota_exhausted_" + agentId, "API quota exhausted at " + java.time.LocalDateTime.now());
    }
}
