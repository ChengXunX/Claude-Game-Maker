package com.chengxun.gamemaker.agent;

import com.chengxun.gamemaker.engine.ClaudeCliEngine;
import com.chengxun.gamemaker.engine.MessageBus;
import com.chengxun.gamemaker.manager.*;
import com.chengxun.gamemaker.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 通用 Agent 实现
 * 替代所有角色特定的 Agent 子类，行为完全由配置和提示词驱动
 *
 * 设计理念：
 * - 一个类处理所有角色，角色差异通过 RolePromptLibrary 驱动
 * - 支持自定义角色，无需编写新代码
 * - 任务处理流程统一：接收任务 → 构建 prompt → 调用 AI → 保存结果 → 通知相关方
 *
 * @author chengxun
 * @since 2.0.0
 */
public class GenericAgent extends BaseAgent {

    private static final Logger log = LoggerFactory.getLogger(GenericAgent.class);

    /** 角色提示词库 */
    private final RolePromptLibrary rolePromptLibrary;

    /** 游戏运行时验证服务（延迟注入） */
    private com.chengxun.gamemaker.service.GameRuntimeVerifier gameRuntimeVerifier;

    /**
     * 设置游戏运行时验证服务
     */
    public void setGameRuntimeVerifier(com.chengxun.gamemaker.service.GameRuntimeVerifier gameRuntimeVerifier) {
        this.gameRuntimeVerifier = gameRuntimeVerifier;
    }

    /** 上次角色特有工作执行时间 */
    private LocalDateTime lastRoleWorkTime;

    /** 角色特有工作冷却时间（分钟） */
    private static final int ROLE_WORK_COOLDOWN_MINUTES = 60;

    public GenericAgent(AgentDefinition definition,
                       ClaudeCliEngine cliEngine,
                       MessageBus messageBus,
                       ContextManager contextManager,
                       MemoryManager memoryManager,
                       SkillManager skillManager,
                       ProjectManager projectManager,
                       RolePromptLibrary rolePromptLibrary) {
        super(definition, cliEngine, messageBus, contextManager, memoryManager, skillManager, projectManager);
        this.rolePromptLibrary = rolePromptLibrary;
    }

    // ===== 核心工作循环 =====

    @Override
    protected void doWork() {
        log.info("Agent [{}] working... (role={})", getName(), getRole());

        // 1. 处理待办任务
        processPendingTasks();

        // 2. 如果有工作流分配的待处理/进行中任务，跳过角色特有工作
        boolean hasActiveTask = tasks.stream()
            .anyMatch(t -> t.getStatus() == TaskAssignment.TaskStatus.PENDING
                || t.getStatus() == TaskAssignment.TaskStatus.IN_PROGRESS);
        if (hasActiveTask) {
            log.debug("有活跃任务，跳过角色特有工作");
            return;
        }

        // 3. 执行角色特有的周期性工作（带冷却时间）
        if (shouldDoRoleWork()) {
            doRoleSpecificWork();
            lastRoleWorkTime = LocalDateTime.now();
        }

        // 4. 检查是否需要压缩上下文
        if (shouldCompactContext()) {
            compactContext();
        }
    }

    /**
     * 判断是否需要执行角色特有工作
     */
    private boolean shouldDoRoleWork() {
        if (lastRoleWorkTime == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(lastRoleWorkTime.plusMinutes(ROLE_WORK_COOLDOWN_MINUTES));
    }

    /**
     * 处理待办任务
     * 取优先级最高的待处理任务执行
     */
    private void processPendingTasks() {
        List<TaskAssignment> pendingTasks = tasks.stream()
            .filter(t -> t.getStatus() == TaskAssignment.TaskStatus.PENDING)
            .sorted((a, b) -> b.getPriority().compareTo(a.getPriority()))
            .toList();

        if (!pendingTasks.isEmpty()) {
            workOnTask(pendingTasks.get(0));
        }
    }

    /**
     * 执行任务的核心流程
     * 构建上下文丰富的 prompt → 调用 AI → 运行时验证 → 失败则修复重试 → 保存结果 → 通知相关方
     *
     * @param task 待执行的任务
     */
    private void workOnTask(TaskAssignment task) {
        log.info("Agent [{}] working on task: {}", getName(), task.getTitle());

        // 记录任务开始日志
        logTask("TASK_RECEIVED", String.format("接收任务: %s", task.getTitle()), task.getId());

        // 标记任务进行中
        task.setStatus(TaskAssignment.TaskStatus.IN_PROGRESS);
        task.setUpdatedAt(LocalDateTime.now());
        agentContext.setCurrentTaskId(task.getId());

        // 构建带上下文的 prompt
        String prompt = buildTaskPrompt(task);

        // 迭代修复循环：生成 → 验证 → 修复 → 再验证
        int maxFixAttempts = 3;
        String result = null;
        long totalAiTime = 0;

        for (int attempt = 0; attempt <= maxFixAttempts; attempt++) {
            long startTime = System.currentTimeMillis();

            // 调用 AI（sendMessage 自动注入能力 prompt、MCP 配置）
            result = sendMessage(prompt);

            long duration = System.currentTimeMillis() - startTime;
            totalAiTime += duration;

            // 记录 AI 调用日志
            String logInput = prompt.length() > 3000 ? prompt.substring(0, 3000) + "\n...[截断]" : prompt;
            String logOutput = result != null && result.length() > 10000
                ? result.substring(0, 10000) + "\n...[截断]" : result;
            logAiCall(String.format("任务 [%s] AI 调用完成 (第%d次)，耗时 %dms", task.getTitle(), attempt + 1, duration),
                logInput, logOutput, duration);

            // 运行时验证（仅对游戏开发类任务）
            if (gameRuntimeVerifier != null && currentProject != null && isGameDevRole(getRole())) {
                String projectDir = currentProject.getWorkDir();
                if (projectDir != null) {
                    log.info("任务 [{}] 运行时验证 (第{}次尝试)", task.getTitle(), attempt + 1);
                    com.chengxun.gamemaker.service.GameRuntimeVerifier.VerifyResult verifyResult =
                        gameRuntimeVerifier.verify(projectDir);

                    if (verifyResult.isSuccess()) {
                        log.info("任务 [{}] 运行时验证通过: {}", task.getTitle(), verifyResult.getMessage());
                        if (attempt > 0) {
                            logInfo("FIX_SUCCESS", String.format("任务 [%s] 经过%d次修复后验证通过", task.getTitle(), attempt));
                        }
                        break; // 验证通过，退出循环
                    }

                    // 验证失败
                    if (attempt < maxFixAttempts) {
                        log.warn("任务 [{}] 运行时验证失败 (第{}次): {}", task.getTitle(), attempt + 1, verifyResult.getError());
                        logWarn("VERIFY_FAILED", String.format("任务 [%s] 运行时验证失败 (第%d次): %s",
                            task.getTitle(), attempt + 1, verifyResult.getError()), null);

                        // 构建修复 prompt：包含原始任务 + 验证失败信息
                        prompt = buildFixPrompt(task, result, verifyResult);
                        continue; // 重试
                    } else {
                        // 最后一次仍然失败
                        log.warn("任务 [{}] 经过{}次修复仍未通过运行时验证: {}", task.getTitle(), maxFixAttempts, verifyResult.getError());
                        logWarn("FIX_EXHAUSTED", String.format("任务 [%s] 经过%d次修复仍未通过验证: %s",
                            task.getTitle(), maxFixAttempts, verifyResult.getError()), null);
                    }
                }
            } else {
                // 非游戏开发任务，不需要运行时验证
                break;
            }
        }

        // 保存任务结果
        task.setResult(result);
        task.setStatus(TaskAssignment.TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        agentContext.setCurrentTaskId(null);

        // 记录任务完成决策日志（含 AI 输出摘要）
        String resultSummary = result != null && result.length() > 5000
            ? result.substring(0, 5000) + "\n...[截断]" : result;
        logDecision(
            String.format("任务完成: %s (耗时 %ds)", task.getTitle(), totalAiTime / 1000),
            String.format("## 任务信息\n- 标题: %s\n- 描述: %s\n- 优先级: %s\n\n## AI 输出\n\n%s",
                task.getTitle(), task.getDescription(), task.getPriority(), resultSummary)
        );

        // 保存经验和学习技能
        saveTaskExperience(task, result);
        tryLearnSkill(task.getTitle() + "\n" + task.getDescription(), result);

        // 将任务完成信息反馈到知识库，促进知识自进化
        feedTaskCompletionToKB(task.getTitle() + "\n" + task.getDescription(), result, true);

        // 通知相关 Agent
        notifyTaskCompleted(task, result);

        // 汇报进度
        String summary = result != null && result.length() > 100 ? result.substring(0, 100) + "..." : result;
        reportProgress(task.getId(), "已完成: " + summary);

        // 记录任务完成日志
        logTask("TASK_COMPLETED", String.format("任务完成: %s，耗时 %ds", task.getTitle(), totalAiTime / 1000), task.getId());
    }

    /**
     * 判断是否为游戏开发类角色（需要运行时验证）
     */
    private boolean isGameDevRole(String role) {
        return "client-dev".equals(role) || "ui-dev".equals(role);
    }

    /**
     * 构建修复 prompt
     * 当运行时验证失败时，将失败信息反馈给 AI 要求修复
     *
     * @param task 原始任务
     * @param lastResult 上次 AI 输出
     * @param verifyResult 验证结果
     * @return 修复 prompt
     */
    private String buildFixPrompt(TaskAssignment task, String lastResult,
                                   com.chengxun.gamemaker.service.GameRuntimeVerifier.VerifyResult verifyResult) {
        StringBuilder sb = new StringBuilder();

        // 角色提示词
        String rolePrompt = rolePromptLibrary.getPrompt(getRole());
        sb.append(rolePrompt).append("\n\n");

        sb.append("## 紧急修复任务\n\n");
        sb.append("你之前完成的任务【").append(task.getTitle()).append("】在运行时验证中失败，需要修复。\n\n");

        sb.append("### 验证失败信息\n");
        sb.append(verifyResult.toSummary()).append("\n");

        if (currentProject != null) {
            sb.append("### 项目信息\n");
            sb.append("- 工作目录: ").append(currentProject.getWorkDir()).append("\n");
            sb.append("- 目标: ").append(currentProject.getGoal()).append("\n\n");
        }

        sb.append("### 原始任务描述\n");
        sb.append(task.getDescription()).append("\n\n");

        sb.append("### 修复要求\n");
        sb.append("1. 仔细分析验证失败的原因\n");
        sb.append("2. 修复所有导致验证失败的问题\n");
        sb.append("3. 确保代码语法正确、文件引用正确\n");
        sb.append("4. 确保入口文件结构完整\n");
        sb.append("5. 所有引用的资源文件必须存在\n\n");

        sb.append("请立即修复以上问题。");

        return sb.toString();
    }

    /**
     * 构建任务 prompt
     * 注入角色知识、项目上下文、工作规范、相关经验、协作上下文
     *
     * @param task 任务
     * @return 完整的 prompt
     */
    private String buildTaskPrompt(TaskAssignment task) {
        StringBuilder sb = new StringBuilder();

        // 1. 角色系统提示词（定义角色身份和专业领域）
        String rolePrompt = rolePromptLibrary.getPrompt(getRole());
        sb.append(rolePrompt).append("\n\n");

        // 2. 项目上下文
        if (currentProject != null) {
            sb.append("## 项目信息\n\n");
            sb.append("- 项目名称: ").append(currentProject.getName()).append("\n");
            sb.append("- 工作目录: ").append(currentProject.getWorkDir()).append("\n");
            if (currentProject.getDescription() != null) {
                sb.append("- 项目描述: ").append(currentProject.getDescription()).append("\n");
            }
            sb.append("\n");

            // 加载项目规则
            String projectRules = projectManager.loadProjectRules(currentProject.getId());
            if (projectRules != null) {
                sb.append("## 项目规范\n\n").append(projectRules).append("\n\n");
            }
        }

        // 3. 协作上下文（团队状态、任务依赖、最近事件）
        if (currentProject != null && projectBoard != null) {
            String collaborationContext = projectBoard.buildAgentContext(
                currentProject.getId(), getId(), eventBus);
            if (!collaborationContext.isEmpty()) {
                sb.append(collaborationContext);
            }
        }

        // 4. 工作约束（AGENTS.md）
        String agentsContent = loadKnowledge("agents_file");
        if (agentsContent != null) {
            sb.append("## 工作约束\n\n").append(agentsContent).append("\n\n");
        }

        // 5. 相关知识和经验
        String knowledge = loadKnowledge("project_architecture");
        if (knowledge != null) {
            sb.append("## 项目知识\n\n").append(knowledge).append("\n\n");
        }

        String experience = loadExperience("similar_tasks");
        if (experience != null) {
            sb.append("## 相关经验\n\n").append(experience).append("\n\n");
        }

        // 6. 知识库相关知识（从全局知识库中查询相关模式、解决方案和最佳实践）
        String relevantKnowledge = queryRelevantKnowledge(task.getTitle() + " " + task.getDescription());
        if (!relevantKnowledge.isEmpty()) {
            sb.append("## 知识库参考\n\n").append(relevantKnowledge).append("\n\n");
        }

        // 7. 匹配技能
        String skillPrompt = buildSkillPrompt(task.getTitle() + " " + task.getDescription());
        if (!skillPrompt.isEmpty()) {
            sb.append(skillPrompt).append("\n\n");
        }

        // 8. 任务描述
        sb.append("## 任务\n\n");
        sb.append("- 标题: ").append(task.getTitle()).append("\n");
        sb.append("- 描述: ").append(task.getDescription()).append("\n");
        sb.append("- 优先级: ").append(task.getPriority()).append("\n\n");

        // 9. 上次工作结果（如果有）
        String lastWork = agentContext.getWorkingMemory("last_task_result");
        if (lastWork != null) {
            sb.append("## 上次工作结果\n\n").append(lastWork).append("\n\n");
        }

        // 10. 工作思维
        sb.append("## 工作思维\n\n");
        sb.append("- 先做出能用的版本，再优化到好用\n");
        sb.append("- 如果发现当前方案有问题，主动提出改进方案\n");
        sb.append("- 不要盲目遵循模板或规范，以实际效果为准\n");
        sb.append("- 遇到不确定的设计决策，先实现最简单的方案，后续迭代改进\n\n");

        // 11. 质量自检清单
        sb.append("## 质量自检清单\n\n");
        sb.append("完成任务前，请逐项检查：\n");
        sb.append("- [ ] 代码是否有完整的中文注释（类注释、方法注释、关键逻辑注释）\n");
        sb.append("- [ ] 是否遵循项目代码规范\n");
        sb.append("- [ ] 是否处理了边界条件和异常情况\n");
        sb.append("- [ ] 是否与团队其他成员的代码兼容\n");
        sb.append("- [ ] 输出文件路径是否正确\n\n");

        // 11. 输出格式要求
        sb.append("## 输出格式要求\n\n");
        sb.append("- 报告中列出所有创建/修改的文件路径\n");
        sb.append("- 说明每个变更的原因和作用\n");
        sb.append("- 如果遇到阻塞问题，明确说明并请求协助\n\n");

        sb.append("请在工作目录 ").append(definition.getWorkDir()).append(" 中执行以上任务，并报告完成情况。");

        return sb.toString();
    }

    /**
     * 角色特有的周期性工作
     * 通过配置驱动，不同角色执行不同的附加逻辑
     */
    private void doRoleSpecificWork() {
        // 通过标签配置的角色特有行为
        String periodicTask = definition.getTag("periodic_task");
        if (periodicTask != null && !periodicTask.isEmpty()) {
            String result = sendMessage(periodicTask);
            saveKnowledge("periodic_" + System.currentTimeMillis(), result);
        }
    }

    // ===== 消息处理 =====

    @Override
    protected void handleMessage(AgentMessage message) {
        switch (message.getType()) {
            case TASK -> handleTaskMessage(message);
            case QUERY -> handleQueryMessage(message);
            case REVIEW -> handleReviewMessage(message);
            case COMMAND -> handleCommandMessage(message);
            case RESPONSE -> handleResponseMessage(message);
            case NOTIFY -> handleNotifyMessage(message);
            case REPORT -> handleReportMessage(message);
            default -> log.info("Agent [{}] received message: {}", getName(), message.getType());
        }
    }

    /**
     * 处理任务消息
     * 将消息内容作为新任务加入队列
     */
    private void handleTaskMessage(AgentMessage message) {
        log.info("Agent [{}] received task from {}: {}", getName(), message.getFromAgentId(),
            message.getContent().length() > 80 ? message.getContent().substring(0, 80) + "..." : message.getContent());

        TaskAssignment task = TaskAssignment.builder()
            .id("task-" + UUID.randomUUID().toString())
            .assignerId(message.getFromAgentId())
            .assigneeId(getId())
            .title(extractTaskTitle(message.getContent()))
            .description(message.getContent())
            .status(TaskAssignment.TaskStatus.PENDING)
            .priority(TaskAssignment.TaskPriority.MEDIUM)
            .build();

        assignTask(task);

        saveExperience("received_task_" + task.getId(),
            String.format("收到任务 from %s: %s", message.getFromAgentId(),
                message.getContent().length() > 200 ? message.getContent().substring(0, 200) + "..." : message.getContent()));
    }

    /**
     * 处理查询消息
     * 调用 AI 回答问题
     */
    private void handleQueryMessage(AgentMessage message) {
        log.info("Agent [{}] received query from {}: {}", getName(), message.getFromAgentId(), message.getContent());

        String rolePrompt = rolePromptLibrary.getPrompt(getRole());
        String fullPrompt = rolePrompt + "\n\n请以你的专业身份回答以下问题:\n\n" + message.getContent();
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
     * 处理审查消息
     * 调用 AI 审查代码/文档/设计
     */
    private void handleReviewMessage(AgentMessage message) {
        log.info("Agent [{}] received review request from {}", getName(), message.getFromAgentId());

        String rolePrompt = rolePromptLibrary.getPrompt(getRole());
        String fullPrompt = rolePrompt + "\n\n## 审查任务\n\n请审查以下内容并给出详细意见：\n\n" + message.getContent();
        String reviewResult = sendMessage(fullPrompt);

        AgentMessage response = AgentMessage.builder()
            .fromAgentId(getId())
            .toAgentId(message.getFromAgentId())
            .type(AgentMessage.MessageType.RESPONSE)
            .content(reviewResult)
            .build();
        sendMessage(response);

        saveExperience("review_" + System.currentTimeMillis(),
            String.format("Review from %s: %s\nResult: %s",
                message.getFromAgentId(), message.getContent(),
                reviewResult.length() > 500 ? reviewResult.substring(0, 500) + "..." : reviewResult));
    }

    /**
     * 处理命令消息
     * 执行特定命令
     */
    private void handleCommandMessage(AgentMessage message) {
        log.info("Agent [{}] received command from {}: {}", getName(), message.getFromAgentId(), message.getContent());

        String rolePrompt = rolePromptLibrary.getPrompt(getRole());
        String fullPrompt = rolePrompt + "\n\n## 命令执行\n\n请执行以下命令:\n\n" + message.getContent();
        String result = sendMessage(fullPrompt);

        AgentMessage report = AgentMessage.createReport(getId(),
            String.format("命令执行结果:\n%s", result));
        report.setToAgentId(message.getFromAgentId());
        sendMessage(report);

        saveExperience("command_" + System.currentTimeMillis(),
            String.format("Command from %s: %s\nResult: %s",
                message.getFromAgentId(), message.getContent(),
                result.length() > 200 ? result.substring(0, 200) + "..." : result));
    }

    /**
     * 处理响应消息
     * 保存为知识
     */
    private void handleResponseMessage(AgentMessage message) {
        log.info("Agent [{}] received response from {}", getName(), message.getFromAgentId());
        saveKnowledge("response_" + System.currentTimeMillis(), message.getContent());
    }

    /**
     * 处理通知消息
     * 记录日志
     */
    private void handleNotifyMessage(AgentMessage message) {
        log.info("Agent [{}] received notification from {}: {}", getName(), message.getFromAgentId(), message.getContent());
        saveKnowledge("notification_" + System.currentTimeMillis(), message.getContent());
    }

    /**
     * 处理汇报消息
     * 记录日志
     */
    private void handleReportMessage(AgentMessage message) {
        log.info("Agent [{}] received report from {}: {}", getName(), message.getFromAgentId(), message.getContent());
        saveMemory("last_report_" + message.getFromAgentId(), message.getContent());
    }

    // ===== 通知机制 =====

    /**
     * 任务完成后通知相关 Agent
     * 通知目标由角色配置决定
     */
    private void notifyTaskCompleted(TaskAssignment task, String result) {
        Set<String> notifyTargets = rolePromptLibrary.getNotifyTargets(getRole());

        for (String targetRole : notifyTargets) {
            if (targetRole.equals(getRole())) continue; // 不通知自己

            String targetAgentId = getProjectId() != null ? getProjectId() + ":" + targetRole : targetRole;

            AgentMessage notify = AgentMessage.builder()
                .fromAgentId(getId())
                .toAgentId(targetAgentId)
                .type(AgentMessage.MessageType.NOTIFY)
                .content(String.format("[%s] 任务完成通知\n\n任务: %s\n结果摘要: %s",
                    getRole(), task.getTitle(),
                    result.length() > 300 ? result.substring(0, 300) + "..." : result))
                .build();
            sendMessage(notify);
        }
    }

    // ===== 工具方法 =====

    /**
     * 从任务内容中提取标题
     */
    private String extractTaskTitle(String content) {
        if (content == null || content.isEmpty()) return "未命名任务";

        // 尝试提取第一行作为标题
        String[] lines = content.split("\n");
        String firstLine = lines[0].trim();

        // 如果第一行太长，截取前 50 个字符
        if (firstLine.length() > 50) {
            return firstLine.substring(0, 50) + "...";
        }

        return firstLine.isEmpty() ? "未命名任务" : firstLine;
    }

    /**
     * 保存任务经验
     */
    private void saveTaskExperience(TaskAssignment task, String result) {
        String experienceKey = "task_" + task.getId();
        String experienceValue = String.format(
            "任务: %s\n描述: %s\n角色: %s\n项目: %s\n结果: %s\n完成时间: %s",
            task.getTitle(),
            task.getDescription(),
            getRole(),
            currentProject != null ? currentProject.getName() : "global",
            result.length() > 500 ? result.substring(0, 500) + "..." : result,
            task.getCompletedAt()
        );

        saveExperience(experienceKey, experienceValue);

        agentContext.addWorkingMemory("last_task_id", task.getId());
        agentContext.addWorkingMemory("last_task_result",
            result.length() > 200 ? result.substring(0, 200) + "..." : result);
    }
}
