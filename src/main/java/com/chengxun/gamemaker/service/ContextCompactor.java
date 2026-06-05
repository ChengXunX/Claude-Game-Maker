package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.manager.ContextManager;
import com.chengxun.gamemaker.manager.MemoryManager;
import com.chengxun.gamemaker.manager.SkillManager;
import com.chengxun.gamemaker.model.AgentContext;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.model.Skill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 上下文压缩服务
 * 负责Agent上下文的压缩和恢复
 *
 * 主要功能：
 * - 压缩对话历史，保留关键信息
 * - 生成上下文摘要
 * - 从多种来源恢复上下文
 * - 整合skill、memory、memo等能力
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class ContextCompactor {

    private static final Logger log = LoggerFactory.getLogger(ContextCompactor.class);

    @Autowired
    private ContextManager contextManager;

    @Autowired
    private MemoryManager memoryManager;

    @Autowired
    private SkillManager skillManager;

    /**
     * 压缩上下文（类似/compact命令）
     * 将长对话历史压缩成摘要，保留关键信息
     *
     * @param agentId Agent ID
     * @param project 项目
     * @param conversationId 对话ID
     * @return 压缩后的摘要
     */
    public String compactContext(String agentId, GameProject project, String conversationId) {
        log.info("Compacting context for agent: {} in project: {}", agentId,
            project != null ? project.getId() : "global");

        // 1. 加载对话历史
        List<ContextManager.ConversationMessage> messages =
            contextManager.loadConversation(agentId, project, conversationId);

        if (messages.isEmpty()) {
            return "没有对话历史需要压缩";
        }

        // 2. 生成对话摘要
        String conversationSummary = generateConversationSummary(messages);

        // 3. 提取关键决策和任务
        List<String> keyDecisions = extractKeyDecisions(messages);
        List<String> activeTasks = extractActiveTasks(messages);

        // 4. 保存压缩结果到记忆
        String compactKey = "compact_" + conversationId;
        String compactValue = buildCompactContent(conversationSummary, keyDecisions, activeTasks);
        memoryManager.saveMemory(project, agentId, "experiences", compactKey, compactValue);

        // 5. 更新Agent上下文
        AgentContext context = contextManager.getOrCreateContext(agentId, project);
        context.addWorkingMemory("last_compact_time", LocalDateTime.now().toString());
        context.addWorkingMemory("last_compact_summary", conversationSummary.length() > 200 ?
            conversationSummary.substring(0, 200) + "..." : conversationSummary);
        contextManager.saveContext(context, project);

        log.info("Context compacted for agent: {} - {} messages processed", agentId, messages.size());
        return conversationSummary;
    }

    /**
     * 生成对话摘要
     */
    private String generateConversationSummary(List<ContextManager.ConversationMessage> messages) {
        StringBuilder summary = new StringBuilder();
        summary.append("## 对话摘要\n\n");

        // 统计信息
        long userMessages = messages.stream().filter(m -> "user".equals(m.getRole())).count();
        long assistantMessages = messages.stream().filter(m -> "assistant".equals(m.getRole())).count();
        summary.append(String.format("- 用户消息: %d条\n", userMessages));
        summary.append(String.format("- 助手回复: %d条\n", assistantMessages));
        summary.append(String.format("- 时间范围: %s 至 %s\n\n",
            messages.get(0).getTimestamp(),
            messages.get(messages.size() - 1).getTimestamp()));

        // 提取主要话题
        summary.append("### 主要话题\n");
        List<String> topics = extractTopics(messages);
        for (String topic : topics) {
            summary.append("- ").append(topic).append("\n");
        }
        summary.append("\n");

        // 提取关键决策
        List<String> decisions = extractKeyDecisions(messages);
        if (!decisions.isEmpty()) {
            summary.append("### 关键决策\n");
            for (String decision : decisions) {
                summary.append("- ").append(decision).append("\n");
            }
            summary.append("\n");
        }

        return summary.toString();
    }

    /**
     * 提取主要话题
     */
    private List<String> extractTopics(List<ContextManager.ConversationMessage> messages) {
        Set<String> topics = new LinkedHashSet<>();

        for (ContextManager.ConversationMessage msg : messages) {
            String content = msg.getContent();
            if (content == null) continue;

            // 识别任务相关话题
            if (content.contains("任务") || content.contains("task")) {
                topics.add("任务讨论");
            }
            if (content.contains("bug") || content.contains("错误") || content.contains("修复")) {
                topics.add("问题修复");
            }
            if (content.contains("设计") || content.contains("架构")) {
                topics.add("设计讨论");
            }
            if (content.contains("测试") || content.contains("验证")) {
                topics.add("测试验证");
            }
            if (content.contains("优化") || content.contains("性能")) {
                topics.add("性能优化");
            }
        }

        return new ArrayList<>(topics);
    }

    /**
     * 提取关键决策
     */
    private List<String> extractKeyDecisions(List<ContextManager.ConversationMessage> messages) {
        List<String> decisions = new ArrayList<>();

        for (ContextManager.ConversationMessage msg : messages) {
            String content = msg.getContent();
            if (content == null) continue;

            // 识别决策性语句
            if (content.contains("决定") || content.contains("选择") || content.contains("确认")) {
                String decision = content.length() > 100 ? content.substring(0, 100) + "..." : content;
                decisions.add(decision);
            }
        }

        return decisions.stream().distinct().limit(10).collect(Collectors.toList());
    }

    /**
     * 提取活跃任务
     */
    private List<String> extractActiveTasks(List<ContextManager.ConversationMessage> messages) {
        List<String> tasks = new ArrayList<>();

        for (ContextManager.ConversationMessage msg : messages) {
            String content = msg.getContent();
            if (content == null) continue;

            // 识别任务描述
            if (content.contains("TODO") || content.contains("待完成") || content.contains("需要")) {
                String task = content.length() > 80 ? content.substring(0, 80) + "..." : content;
                tasks.add(task);
            }
        }

        return tasks.stream().distinct().limit(5).collect(Collectors.toList());
    }

    /**
     * 构建压缩内容
     */
    private String buildCompactContent(String summary, List<String> decisions, List<String> tasks) {
        StringBuilder content = new StringBuilder();
        content.append(summary);

        if (!decisions.isEmpty()) {
            content.append("\n### 关键决策\n");
            for (String decision : decisions) {
                content.append("- ").append(decision).append("\n");
            }
        }

        if (!tasks.isEmpty()) {
            content.append("\n### 待处理任务\n");
            for (String task : tasks) {
                content.append("- ").append(task).append("\n");
            }
        }

        return content.toString();
    }

    /**
     * 恢复上下文
     * 从多种来源恢复Agent的工作上下文
     *
     * @param agentId Agent ID
     * @param project 项目
     * @return 恢复的上下文提示
     */
    public String recoverContext(String agentId, GameProject project) {
        log.info("Recovering context for agent: {} in project: {}", agentId,
            project != null ? project.getId() : "null");

        if (project == null) {
            return "## 上下文恢复\n\n项目未指定，无法恢复上下文。";
        }

        StringBuilder recoveryPrompt = new StringBuilder();
        recoveryPrompt.append("## 上下文恢复\n\n");
        recoveryPrompt.append("由于会话中断，正在从多个来源恢复工作上下文...\n\n");

        // 1. 从快照恢复
        Map<String, Object> snapshot = contextManager.loadLatestSnapshot(agentId, project);
        if (snapshot != null) {
            recoveryPrompt.append("### 从快照恢复\n");
            recoveryPrompt.append("- 找到最近的上下文快照\n");
            if (snapshot.containsKey("recentMessages")) {
                recoveryPrompt.append("- 包含最近的对话记录\n");
            }
            recoveryPrompt.append("\n");
        }

        // 2. 从记忆恢复
        recoveryPrompt.append("### 已有知识\n");
        Map<String, String> knowledge = memoryManager.getCategoryMemory(project, agentId, "knowledge");
        if (!knowledge.isEmpty()) {
            for (Map.Entry<String, String> entry : knowledge.entrySet()) {
                String summary = entry.getValue().length() > 100 ?
                    entry.getValue().substring(0, 100) + "..." : entry.getValue();
                recoveryPrompt.append(String.format("- **%s**: %s\n", entry.getKey(), summary));
            }
        } else {
            recoveryPrompt.append("- 暂无已保存的知识\n");
        }
        recoveryPrompt.append("\n");

        // 3. 从经验恢复
        recoveryPrompt.append("### 已有经验\n");
        Map<String, String> experiences = memoryManager.getCategoryMemory(project, agentId, "experiences");
        if (!experiences.isEmpty()) {
            for (Map.Entry<String, String> entry : experiences.entrySet()) {
                String summary = entry.getValue().length() > 100 ?
                    entry.getValue().substring(0, 100) + "..." : entry.getValue();
                recoveryPrompt.append(String.format("- **%s**: %s\n", entry.getKey(), summary));
            }
        } else {
            recoveryPrompt.append("- 暂无已保存的经验\n");
        }
        recoveryPrompt.append("\n");

        // 4. 从技能恢复
        recoveryPrompt.append("### 可用技能\n");
        String projectId = project.getId();
        List<Skill> skills = skillManager.getAllSkills(projectId);
        if (!skills.isEmpty()) {
            for (Skill skill : skills.stream().limit(5).collect(Collectors.toList())) {
                recoveryPrompt.append(String.format("- **%s**: %s\n", skill.getName(), skill.getDescription()));
            }
        } else {
            recoveryPrompt.append("- 暂无可用技能\n");
        }
        recoveryPrompt.append("\n");

        // 5. 从上下文恢复
        AgentContext context = contextManager.loadContext(agentId, project);
        if (context != null) {
            recoveryPrompt.append("### 工作状态\n");
            if (context.getCurrentTaskId() != null) {
                recoveryPrompt.append(String.format("- 当前任务: %s\n", context.getCurrentTaskId()));
            }
            List<AgentContext.WorkingMemoryItem> workingMemory = context.getWorkingMemory();
            if (workingMemory != null && !workingMemory.isEmpty()) {
                recoveryPrompt.append("- 工作记忆:\n");
                for (AgentContext.WorkingMemoryItem item : workingMemory) {
                    recoveryPrompt.append(String.format("  - %s: %s\n", item.getKey(), item.getValue()));
                }
            }
            recoveryPrompt.append("\n");
        }

        recoveryPrompt.append("请基于以上信息恢复工作上下文，并确认你已准备好继续工作。");

        log.info("Context recovery completed for agent: {}", agentId);
        return recoveryPrompt.toString();
    }

    /**
     * 获取压缩历史
     *
     * @param agentId Agent ID
     * @param project 项目
     * @return 压缩记录列表
     */
    public List<Map<String, Object>> getCompactHistory(String agentId, GameProject project) {
        List<Map<String, Object>> history = new ArrayList<>();

        if (project == null) {
            return history;
        }

        Map<String, String> experiences = memoryManager.getCategoryMemory(project, agentId, "experiences");
        for (Map.Entry<String, String> entry : experiences.entrySet()) {
            if (entry.getKey().startsWith("compact_")) {
                Map<String, Object> record = new HashMap<>();
                record.put("key", entry.getKey());
                record.put("summary", entry.getValue().length() > 200 ?
                    entry.getValue().substring(0, 200) + "..." : entry.getValue());
                history.add(record);
            }
        }

        return history;
    }

    /**
     * 清理旧的压缩记录
     *
     * @param agentId Agent ID
     * @param project 项目
     * @param keepCount 保留数量
     */
    public void cleanupOldCompacts(String agentId, GameProject project, int keepCount) {
        if (project == null) {
            return;
        }

        Map<String, String> experiences = memoryManager.getCategoryMemory(project, agentId, "experiences");
        List<String> compactKeys = experiences.keySet().stream()
            .filter(k -> k.startsWith("compact_"))
            .sorted(Comparator.reverseOrder())
            .collect(Collectors.toList());

        if (compactKeys.size() > keepCount) {
            for (int i = keepCount; i < compactKeys.size(); i++) {
                memoryManager.deleteMemory(project, agentId, "experiences", compactKeys.get(i));
            }
            log.info("Cleaned up {} old compact records for agent: {}",
                compactKeys.size() - keepCount, agentId);
        }
    }
}
