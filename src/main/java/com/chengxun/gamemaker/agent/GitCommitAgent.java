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

public class GitCommitAgent extends BaseAgent {

    /** 上次代码审查时间 */
    private LocalDateTime lastReviewTime;

    /** 代码审查冷却时间（分钟）：避免每轮都调用 Claude CLI */
    private static final int REVIEW_COOLDOWN_MINUTES = 30;

    private static final Logger log = LoggerFactory.getLogger(GitCommitAgent.class);

    public GitCommitAgent(AgentDefinition definition,
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
        log.info("GitCommitAgent working...");

        // 处理待处理的审查请求（优先处理人工请求）
        processReviewRequests();

        // 如果有工作流分配的待处理/进行中任务，跳过例行审查
        boolean hasActiveTask = tasks.stream()
            .anyMatch(t -> t.getStatus() == TaskAssignment.TaskStatus.PENDING
                || t.getStatus() == TaskAssignment.TaskStatus.IN_PROGRESS);
        if (hasActiveTask) {
            log.debug("有活跃任务，跳过例行代码审查");
            return;
        }

        // 检查待审核的提交（带冷却时间，避免每轮都调用 Claude CLI）
        if (shouldReview()) {
            String reviewResult = reviewRecentCommits();
            if (reviewResult != null) {
                saveKnowledge("last_review", reviewResult);
            }
            lastReviewTime = LocalDateTime.now();
        }
    }

    /**
     * 判断是否需要进行代码审查
     */
    private boolean shouldReview() {
        if (lastReviewTime == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(lastReviewTime.plusMinutes(REVIEW_COOLDOWN_MINUTES));
    }

    @Override
    protected void handleMessage(AgentMessage message) {
        switch (message.getType()) {
            case REVIEW -> handleReviewRequest(message);
            case QUERY -> handleQuery(message);
            case COMMAND -> handleCommand(message);
            case TASK -> handleTask(message);
            default -> log.info("GitCommitAgent received message: {}", message.getType());
        }
    }

    /**
     * 审核最近的提交
     */
    private String reviewRecentCommits() {
        String workDir = definition.getWorkDir();
        if (workDir == null || workDir.isEmpty()) {
            log.warn("No work directory configured for git commit review");
            return null;
        }

        String prompt = buildReviewPrompt();
        String result = sendMessage(prompt);

        // 检查是否包含 AI 作者信息
        if (containsAIAuthorInfo(result)) {
            sendAlertToProducer("发现 AI 作者信息", result);
        }

        // 保存审核记录
        saveReviewRecord(result);

        return result;
    }

    /**
     * 构建审核提示词
     */
    private String buildReviewPrompt() {
        StringBuilder sb = new StringBuilder();

        // 加载审核知识
        String agentsContent = loadKnowledge("agents_file");
        if (agentsContent != null) {
            sb.append("## 审核规范\n\n").append(agentsContent).append("\n\n");
        }

        // 加载项目规则
        if (currentProject != null) {
            String projectRules = projectManager.loadProjectRules(currentProject.getId());
            if (projectRules != null) {
                sb.append("## 项目规范\n\n").append(projectRules).append("\n\n");
            }
        }

        sb.append("## 审核任务\n\n");
        sb.append("请审核最近的 git 提交，执行以下检查：\n\n");
        sb.append("1. **代码质量检查**\n");
        sb.append("   - 代码逻辑是否正确\n");
        sb.append("   - 是否存在潜在 bug\n");
        sb.append("   - 是否有安全漏洞\n\n");

        sb.append("2. **注释规范检查**\n");
        sb.append("   - 关键代码是否有中文注释\n");
        sb.append("   - 注释是否清晰、准确\n");
        sb.append("   - 复杂逻辑是否有说明\n\n");

        sb.append("3. **作者信息检查**\n");
        sb.append("   - 是否包含 AI 作者信息（如 Co-Authored-By: Claude 等）\n");
        sb.append("   - 是否使用正确的 git 用户配置\n");
        sb.append("   - 提交者信息是否与项目成员一致\n\n");

        sb.append("4. **提交信息检查**\n");
        sb.append("   - 是否符合 Conventional Commits 格式\n");
        sb.append("   - 描述是否清晰、准确\n");
        sb.append("   - 是否包含必要的变更说明\n\n");

        sb.append("请执行以下命令获取提交信息：\n");
        sb.append("```bash\n");
        sb.append("git log --oneline -5\n");
        sb.append("git diff HEAD~1..HEAD\n");
        sb.append("git show HEAD\n");
        sb.append("```\n\n");

        sb.append("请输出详细的审核报告，格式如下：\n");
        sb.append("```\n");
        sb.append("📋 Git 提交审核报告\n\n");
        sb.append("【提交信息】\n");
        sb.append("- Commit: <hash>\n");
        sb.append("- Author: <name> <email>\n");
        sb.append("- Message: <message>\n\n");
        sb.append("【审核结果】\n");
        sb.append("✅ 通过 / ❌ 不通过\n\n");
        sb.append("【检查详情】\n");
        sb.append("1. 代码质量：✅/❌ <说明>\n");
        sb.append("2. 注释规范：✅/❌ <说明>\n");
        sb.append("3. 作者信息：✅/❌ <说明>\n");
        sb.append("4. 提交格式：✅/❌ <说明>\n");
        sb.append("5. 安全检查：✅/❌ <说明>\n\n");
        sb.append("【问题详情】\n");
        sb.append("- 问题1：<描述>\n\n");
        sb.append("【修改建议】\n");
        sb.append("- 建议1：<描述>\n");
        sb.append("```\n");

        return sb.toString();
    }

    /**
     * 检查是否包含 AI 作者信息
     */
    private boolean containsAIAuthorInfo(String content) {
        if (content == null) return false;

        String lowerContent = content.toLowerCase();
        String[] aiPatterns = {
            "co-authored-by: claude",
            "co-authored-by: openai",
            "co-authored-by: anthropic",
            "generated with claude",
            "generated by claude",
            "generated with chatgpt",
            "generated by chatgpt",
            "anthropic",
            "claude code",
            "openai"
        };

        for (String pattern : aiPatterns) {
            if (lowerContent.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 向制作人发送警报
     */
    private void sendAlertToProducer(String title, String content) {
        AgentMessage alert = AgentMessage.builder()
            .fromAgentId(getId())
            .toAgentId(getProjectId() != null ? getProjectId() + ":producer" : "producer")
            .type(AgentMessage.MessageType.NOTIFY)
            .content(String.format("⚠️ **%s**\n\n%s", title, content))
            .build();
        sendMessage(alert);

        log.warn("Alert sent to producer: {}", title);
    }

    /**
     * 保存审核记录
     */
    private void saveReviewRecord(String reviewResult) {
        String recordKey = "review_" + System.currentTimeMillis();
        saveExperience(recordKey, reviewResult);

        // 更新工作记忆
        agentContext.addWorkingMemory("last_review_time", LocalDateTime.now().toString());
        agentContext.addWorkingMemory("last_review_result",
            reviewResult.length() > 200 ? reviewResult.substring(0, 200) + "..." : reviewResult);
    }

    /**
     * 处理审查请求
     */
    private void handleReviewRequest(AgentMessage message) {
        log.info("Received review request from {}: {}", message.getFromAgentId(), message.getContent());

        // 执行审核
        String reviewPrompt = buildSpecificReviewPrompt(message.getContent());
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
        saveExperience("review_request_" + System.currentTimeMillis(),
            String.format("Review request from %s: %s\nResult: %s",
                message.getFromAgentId(), message.getContent(),
                reviewResult.length() > 500 ? reviewResult.substring(0, 500) + "..." : reviewResult));
    }

    /**
     * 构建特定审核提示词
     */
    private String buildSpecificReviewPrompt(String reviewTarget) {
        StringBuilder sb = new StringBuilder();

        String agentsContent = loadKnowledge("agents_file");
        if (agentsContent != null) {
            sb.append("## 审核规范\n\n").append(agentsContent).append("\n\n");
        }

        sb.append("## 审核任务\n\n");
        sb.append("请审核以下内容：\n\n");
        sb.append(reviewTarget).append("\n\n");
        sb.append("请按照审核规范进行详细审核，并输出审核报告。");

        return sb.toString();
    }

    /**
     * 处理查询
     */
    private void handleQuery(AgentMessage message) {
        log.info("Received query from {}: {}", message.getFromAgentId(), message.getContent());

        String response;
        if (message.getContent().contains("审核") || message.getContent().contains("review")) {
            String lastReview = loadExperience("last_review");
            response = lastReview != null ? lastReview : "暂无审核记录";
        } else {
            String skillPrompt = buildSkillPrompt("回答审核相关问题");
            response = sendMessage(skillPrompt + "\n\n请回答: " + message.getContent());
        }

        AgentMessage responseMsg = AgentMessage.builder()
            .fromAgentId(getId())
            .toAgentId(message.getFromAgentId())
            .type(AgentMessage.MessageType.RESPONSE)
            .content(response)
            .build();
        sendMessage(responseMsg);
    }

    /**
     * 处理命令
     */
    private void handleCommand(AgentMessage message) {
        log.info("Received command from {}: {}", message.getFromAgentId(), message.getContent());

        String result;
        if (message.getContent().contains("立即审核") || message.getContent().contains("review now")) {
            result = reviewRecentCommits();
            if (result == null) result = "审核失败：未配置工作目录";
        } else {
            result = "未知命令: " + message.getContent();
        }

        AgentMessage report = AgentMessage.createReport(getId(),
            String.format("命令执行结果:\n%s", result));
        report.setToAgentId(message.getFromAgentId());
        sendMessage(report);
    }

    /**
     * 处理任务
     */
    private void handleTask(AgentMessage message) {
        log.info("Received task from {}: {}", message.getFromAgentId(), message.getContent());

        TaskAssignment task = TaskAssignment.builder()
            .id("review-task-" + UUID.randomUUID().toString())
            .assignerId(message.getFromAgentId())
            .assigneeId(getId())
            .title("代码审核任务")
            .description(message.getContent())
            .status(TaskAssignment.TaskStatus.PENDING)
            .priority(TaskAssignment.TaskPriority.HIGH)
            .build();

        assignTask(task);
    }

    /**
     * 处理待处理的审查请求
     */
    private void processReviewRequests() {
        List<AgentMessage> reviewRequests = pendingMessages.stream()
            .filter(m -> m.getType() == AgentMessage.MessageType.REVIEW)
            .toList();

        for (AgentMessage request : reviewRequests) {
            handleReviewRequest(request);
        }
    }

    /**
     * 主动审查其他 Agent 的提交
     */
    public void reviewAgentCommit(String agentId, String commitInfo) {
        String reviewPrompt = String.format(
            "请审核 Agent [%s] 的提交：\n\n%s\n\n按照审核规范进行审核。",
            agentId, commitInfo
        );

        String result = sendMessage(reviewPrompt);

        // 保存审查记录
        saveExperience("agent_commit_review_" + agentId + "_" + System.currentTimeMillis(),
            String.format("Agent: %s\nCommit: %s\nResult: %s",
                agentId, commitInfo,
                result.length() > 500 ? result.substring(0, 500) + "..." : result));

        // 如果发现问题，通知对应 Agent
        if (result.contains("❌") || result.contains("不通过")) {
            AgentMessage notify = AgentMessage.builder()
                .fromAgentId(getId())
                .toAgentId(agentId)
                .type(AgentMessage.MessageType.NOTIFY)
                .content(String.format("您的提交存在以下问题：\n\n%s", result))
                .build();
            sendMessage(notify);
        }
    }
}
