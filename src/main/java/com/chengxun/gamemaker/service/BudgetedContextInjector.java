package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.config.SystemConstants;
import com.chengxun.gamemaker.manager.ContextManager;
import com.chengxun.gamemaker.manager.MemoryManager;
import com.chengxun.gamemaker.model.AgentContext;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.web.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 预算上下文注入器
 * 按 token 预算 + 重要性排序注入上下文
 *
 * 与 ContextCompactor 的区别：
 * - ContextCompactor：压缩对话历史，生成摘要
 * - BudgetedContextInjector：智能选择注入哪些上下文，按重要性分配 token 预算
 *
 * 注入源优先级（从高到低）：
 * 1. 当前任务状态（最重要）
 * 2. 检查点摘要
 * 3. 近期关键决策
 * 4. 相关知识（按相关性排序）
 * 5. 工作记忆
 * 6. 可用技能列表
 *
 * 灵感来源：预算上下文注入机制
 *
 * @author chengxun
 * @since 3.0.0
 */
@Service
public class BudgetedContextInjector {

    private static final Logger log = LoggerFactory.getLogger(BudgetedContextInjector.class);

    /** 默认总 token 预算 */
    private static final int DEFAULT_TOTAL_BUDGET = 8000;

    /** 各源的预算分配比例 */
    private static final double TASK_STATE_RATIO = 0.20;      // 20% 给任务状态
    private static final double CHECKPOINT_RATIO = 0.20;       // 20% 给检查点
    private static final double DECISIONS_RATIO = 0.15;        // 15% 给关键决策
    private static final double KNOWLEDGE_RATIO = 0.25;        // 25% 给知识库
    private static final double WORKING_MEMORY_RATIO = 0.10;   // 10% 给工作记忆
    private static final double SKILLS_RATIO = 0.10;           // 10% 给技能列表

    /** 估算每 token 约 4 个字符（中文约 2 字符/token，英文约 4 字符/token） */
    private static final double CHARS_PER_TOKEN = 3.0;

    @Autowired
    private ContextManager contextManager;

    @Autowired
    private MemoryManager memoryManager;

    @Autowired
    private CheckpointService checkpointService;

    @Autowired
    private SystemConfigService configService;

    /**
     * 构建预算注入的上下文 Prompt
     *
     * @param agentId Agent ID
     * @param project 项目
     * @param totalBudget 总 token 预算（0 = 使用默认值）
     * @return 注入的上下文文本
     */
    public String injectContext(String agentId, GameProject project, int totalBudget) {
        if (totalBudget <= 0) {
            totalBudget = DEFAULT_TOTAL_BUDGET;
        }

        log.debug("构建预算注入: agent={}, budget={}", agentId, totalBudget);

        StringBuilder context = new StringBuilder();
        int usedTokens = 0;

        // 1. 任务状态（最高优先级）
        int taskBudget = (int) (totalBudget * TASK_STATE_RATIO);
        String taskState = buildTaskStateSection(agentId, project, taskBudget);
        if (!taskState.isEmpty()) {
            context.append(taskState);
            usedTokens += estimateTokens(taskState);
        }

        // 2. 检查点摘要
        int checkpointBudget = (int) (totalBudget * CHECKPOINT_RATIO);
        String checkpoint = buildCheckpointSection(agentId, project, checkpointBudget);
        if (!checkpoint.isEmpty()) {
            context.append(checkpoint);
            usedTokens += estimateTokens(checkpoint);
        }

        // 3. 关键决策
        int decisionsBudget = (int) (totalBudget * DECISIONS_RATIO);
        String decisions = buildDecisionsSection(agentId, project, decisionsBudget);
        if (!decisions.isEmpty()) {
            context.append(decisions);
            usedTokens += estimateTokens(decisions);
        }

        // 4. 相关知识
        int knowledgeBudget = (int) (totalBudget * KNOWLEDGE_RATIO);
        String knowledge = buildKnowledgeSection(agentId, project, knowledgeBudget);
        if (!knowledge.isEmpty()) {
            context.append(knowledge);
            usedTokens += estimateTokens(knowledge);
        }

        // 5. 工作记忆
        int memoryBudget = (int) (totalBudget * WORKING_MEMORY_RATIO);
        String workingMemory = buildWorkingMemorySection(agentId, project, memoryBudget);
        if (!workingMemory.isEmpty()) {
            context.append(workingMemory);
            usedTokens += estimateTokens(workingMemory);
        }

        // 6. 技能列表
        int skillsBudget = (int) (totalBudget * SKILLS_RATIO);
        String skills = buildSkillsSection(project, skillsBudget);
        if (!skills.isEmpty()) {
            context.append(skills);
            usedTokens += estimateTokens(skills);
        }

        log.debug("预算注入完成: agent={}, usedTokens={}/{}", agentId, usedTokens, totalBudget);
        return context.toString();
    }

    /**
     * 构建任务状态段
     */
    private String buildTaskStateSection(String agentId, GameProject project, int budget) {
        AgentContext context = contextManager.loadContext(agentId, project);
        if (context == null) return "";

        StringBuilder section = new StringBuilder();
        section.append("### 当前任务状态\n");

        if (context.getCurrentTaskId() != null) {
            section.append("- 当前任务: ").append(context.getCurrentTaskId()).append("\n");
        }

        section.append("\n");
        return truncateToBudget(section.toString(), budget);
    }

    /**
     * 构建检查点摘要段
     */
    private String buildCheckpointSection(String agentId, GameProject project, int budget) {
        Map<String, Object> checkpoint = checkpointService.loadLatestCheckpoint(agentId, project);
        if (checkpoint == null) return "";

        StringBuilder section = new StringBuilder();
        section.append("### 最近检查点\n");
        section.append("- 时间: ").append(checkpoint.getOrDefault("createdAt", "未知")).append("\n");
        section.append("- 原因: ").append(checkpoint.getOrDefault("reason", "未知")).append("\n");
        section.append("- 消息数: ").append(checkpoint.getOrDefault("messageCount", 0)).append("\n\n");

        return truncateToBudget(section.toString(), budget);
    }

    /**
     * 构建关键决策段
     */
    private String buildDecisionsSection(String agentId, GameProject project, int budget) {
        Map<String, String> knowledge = memoryManager.getCategoryMemory(project, agentId, "knowledge");
        if (knowledge.isEmpty()) return "";

        StringBuilder section = new StringBuilder();
        section.append("### 关键决策\n");

        int usedChars = 0;
        int maxChars = (int) (budget * CHARS_PER_TOKEN);

        for (Map.Entry<String, String> entry : knowledge.entrySet()) {
            if (usedChars >= maxChars) break;

            String value = entry.getValue();
            if (value.contains("决策") || value.contains("方案") || value.contains("决定")) {
                String line = String.format("- %s: %s\n", entry.getKey(),
                    value.length() > 80 ? value.substring(0, 80) + "..." : value);
                section.append(line);
                usedChars += line.length();
            }
        }

        section.append("\n");
        return section.toString();
    }

    /**
     * 构建知识段（按重要性排序）
     */
    private String buildKnowledgeSection(String agentId, GameProject project, int budget) {
        // 合并多个知识类别
        List<Map.Entry<String, String>> allKnowledge = new ArrayList<>();

        for (String category : List.of("knowledge", "experiences", "general")) {
            Map<String, String> catKnowledge = memoryManager.getCategoryMemory(project, agentId, category);
            allKnowledge.addAll(catKnowledge.entrySet());
        }

        if (allKnowledge.isEmpty()) return "";

        // 按 key 排序（最新的在前，因为 key 通常包含时间戳）
        allKnowledge.sort((a, b) -> b.getKey().compareTo(a.getKey()));

        StringBuilder section = new StringBuilder();
        section.append("### 相关知识\n");

        int usedChars = 0;
        int maxChars = (int) (budget * CHARS_PER_TOKEN);
        int count = 0;

        for (Map.Entry<String, String> entry : allKnowledge) {
            if (usedChars >= maxChars || count >= 5) break;

            String value = entry.getValue();
            String summary = value.length() > 100 ? value.substring(0, 100) + "..." : value;
            String line = String.format("- **%s**: %s\n", entry.getKey(), summary);
            section.append(line);
            usedChars += line.length();
            count++;
        }

        section.append("\n");
        return section.toString();
    }

    /**
     * 构建工作记忆段
     */
    private String buildWorkingMemorySection(String agentId, GameProject project, int budget) {
        AgentContext context = contextManager.loadContext(agentId, project);
        if (context == null) return "";

        List<AgentContext.WorkingMemoryItem> workingMemory = context.getWorkingMemory();
        if (workingMemory == null || workingMemory.isEmpty()) return "";

        StringBuilder section = new StringBuilder();
        section.append("### 工作记忆\n");

        int usedChars = 0;
        int maxChars = (int) (budget * CHARS_PER_TOKEN);

        for (AgentContext.WorkingMemoryItem item : workingMemory) {
            if (usedChars >= maxChars) break;

            String line = String.format("- %s: %s\n", item.getKey(), item.getValue());
            section.append(line);
            usedChars += line.length();
        }

        section.append("\n");
        return section.toString();
    }

    /**
     * 构建技能列表段
     */
    private String buildSkillsSection(GameProject project, int budget) {
        if (project == null) return "";

        // 通过 SkillManager 获取技能（需要注入，但这里用简化方式）
        // 实际使用时会通过 ContextCompactor 的 recoverContext 获取
        return "";
    }

    /**
     * 截断文本到指定 token 预算
     */
    private String truncateToBudget(String text, int budget) {
        int maxChars = (int) (budget * CHARS_PER_TOKEN);
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars) + "\n...(已截断)\n";
    }

    /**
     * 估算文本的 token 数
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        return (int) (text.length() / CHARS_PER_TOKEN);
    }
}
