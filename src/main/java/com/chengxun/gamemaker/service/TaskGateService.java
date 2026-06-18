package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.config.SystemConstants;
import com.chengxun.gamemaker.manager.ContextManager;
import com.chengxun.gamemaker.model.AgentContext;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.web.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 任务门禁服务
 * Agent 尝试停止时，检查是否有未完成的任务，nudge 继续
 *
 * 核心思想：
 * 当 Agent 说"我完成了"时，门禁机制会检查：
 * 1. 是否有状态为 open/in_progress 的任务
 * 2. 是否有待处理的 TODO/FIXME
 * 3. 是否有未验证的变更
 * 如果有，门禁会返回提示，要求 Agent 继续工作
 *
 * 防止 Agent "乐观停止" —— 即 Agent 认为完成了但实际上遗漏了任务
 *
 * 灵感来源：任务门禁与完成度检查机制
 *
 * @author chengxun
 * @since 3.0.0
 */
@Service
public class TaskGateService {

    private static final Logger log = LoggerFactory.getLogger(TaskGateService.class);

    /** 主会话最大门禁反应次数 */
    private static final int MAX_GATE_REACT_MAIN = 3;

    /** 子代理最大门禁反应次数 */
    private static final int MAX_GATE_REACT_SUBAGENT = 2;

    /** 任务状态模式：匹配 "任务状态: xxx" 或 "status: xxx" */
    private static final Pattern TASK_STATUS_PATTERN = Pattern.compile(
        "(?:任务状态|status|state)\\s*[:：]\\s*(open|in_progress|in-progress|blocked|pending|进行中|待处理|阻塞)",
        Pattern.CASE_INSENSITIVE
    );

    /** TODO 模式 */
    private static final Pattern TODO_PATTERN = Pattern.compile(
        "(?:TODO|FIXME|待完成|待处理|待实现|待开发|待测试|待验证|下一步|需要|还需|还要|尚未)",
        Pattern.CASE_INSENSITIVE
    );

    /** 未验证变更模式 */
    private static final Pattern UNVERIFIED_PATTERN = Pattern.compile(
        "(?:已修改|已创建|已更新|已删除|已添加|修改了|创建了|更新了|删除了|添加了)(?:.*?)(?:但|还|尚未|未|没有)(?:.*?)(?:验证|测试|确认|检查)",
        Pattern.CASE_INSENSITIVE
    );

    @Autowired
    private ContextManager contextManager;

    @Autowired
    private SystemConfigService configService;

    /**
     * 检查门禁：Agent 尝试停止时调用
     *
     * @param agentId Agent ID
     * @param project 项目
     * @param isSubAgent 是否为子代理
     * @param currentGateCount 当前已触发门禁次数
     * @return 门禁结果
     */
    public GateResult checkGate(String agentId, GameProject project, boolean isSubAgent, int currentGateCount) {
        int maxReact = isSubAgent ? MAX_GATE_REACT_SUBAGENT : MAX_GATE_REACT_MAIN;

        // 超过最大反应次数，放行
        if (currentGateCount >= maxReact) {
            log.info("门禁放行: agent={}, 已达最大反应次数 {}/{}", agentId, currentGateCount, maxReact);
            return GateResult.pass("已达最大门禁反应次数，放行");
        }

        // 加载近期对话
        List<ContextManager.ConversationMessage> recentMessages =
            contextManager.getRecentMessages(agentId, project, 30);

        if (recentMessages.isEmpty()) {
            return GateResult.pass("无近期对话");
        }

        // 检查未完成任务
        List<String> pendingTasks = findPendingTasks(recentMessages);
        if (!pendingTasks.isEmpty()) {
            String nudge = buildNudgeMessage(pendingTasks, "未完成任务");
            log.info("门禁拦截: agent={}, 发现 {} 个未完成任务", agentId, pendingTasks.size());
            return GateResult.block(nudge, pendingTasks);
        }

        // 检查 TODO/FIXME
        List<String> todos = findTodos(recentMessages);
        if (!todos.isEmpty()) {
            String nudge = buildNudgeMessage(todos, "待处理事项");
            log.info("门禁拦截: agent={}, 发现 {} 个待处理事项", agentId, todos.size());
            return GateResult.block(nudge, todos);
        }

        // 检查未验证变更
        List<String> unverified = findUnverifiedChanges(recentMessages);
        if (!unverified.isEmpty()) {
            String nudge = buildNudgeMessage(unverified, "未验证变更");
            log.info("门禁拦截: agent={}, 发现 {} 个未验证变更", agentId, unverified.size());
            return GateResult.block(nudge, unverified);
        }

        return GateResult.pass("所有任务已完成");
    }

    /**
     * 从对话中查找未完成任务
     */
    private List<String> findPendingTasks(List<ContextManager.ConversationMessage> messages) {
        List<String> tasks = new ArrayList<>();

        // 只检查最近 10 条消息
        int start = Math.max(0, messages.size() - 10);
        for (int i = start; i < messages.size(); i++) {
            String content = messages.get(i).getContent();
            if (content == null) continue;

            Matcher matcher = TASK_STATUS_PATTERN.matcher(content);
            while (matcher.find()) {
                String taskLine = extractLine(content, matcher.start());
                if (!taskLine.isEmpty()) {
                    tasks.add(taskLine);
                }
            }
        }

        return tasks.stream().distinct().limit(5).toList();
    }

    /**
     * 查找 TODO/FIXME 事项
     */
    private List<String> findTodos(List<ContextManager.ConversationMessage> messages) {
        List<String> todos = new ArrayList<>();

        int start = Math.max(0, messages.size() - 10);
        for (int i = start; i < messages.size(); i++) {
            String content = messages.get(i).getContent();
            if (content == null) continue;

            Matcher matcher = TODO_PATTERN.matcher(content);
            while (matcher.find()) {
                String todoLine = extractLine(content, matcher.start());
                if (!todoLine.isEmpty() && todoLine.length() < 100) {
                    todos.add(todoLine);
                }
            }
        }

        return todos.stream().distinct().limit(5).toList();
    }

    /**
     * 查找未验证变更
     */
    private List<String> findUnverifiedChanges(List<ContextManager.ConversationMessage> messages) {
        List<String> changes = new ArrayList<>();

        int start = Math.max(0, messages.size() - 10);
        for (int i = start; i < messages.size(); i++) {
            String content = messages.get(i).getContent();
            if (content == null) continue;

            Matcher matcher = UNVERIFIED_PATTERN.matcher(content);
            while (matcher.find()) {
                String changeLine = extractLine(content, matcher.start());
                if (!changeLine.isEmpty()) {
                    changes.add(changeLine);
                }
            }
        }

        return changes.stream().distinct().limit(3).toList();
    }

    /**
     * 提取匹配位置所在行
     */
    private String extractLine(String content, int matchStart) {
        int lineStart = content.lastIndexOf('\n', matchStart) + 1;
        int lineEnd = content.indexOf('\n', matchStart);
        if (lineEnd < 0) lineEnd = content.length();
        String line = content.substring(lineStart, lineEnd).trim();
        return line.length() > 100 ? line.substring(0, 100) + "..." : line;
    }

    /**
     * 构建 nudge 提示消息
     */
    private String buildNudgeMessage(List<String> items, String category) {
        StringBuilder nudge = new StringBuilder();
        nudge.append(String.format("## 门禁检查: 发现 %s\n\n", category));
        nudge.append("你尝试停止工作，但以下事项尚未完成：\n\n");
        for (int i = 0; i < items.size(); i++) {
            nudge.append(String.format("%d. %s\n", i + 1, items.get(i)));
        }
        nudge.append("\n请继续处理以上事项，或明确说明为什么可以忽略。");
        return nudge.toString();
    }

    // ===== 内部类 =====

    /**
     * 门禁结果
     */
    public record GateResult(boolean passed, String message, List<String> pendingItems) {
        static GateResult pass(String message) {
            return new GateResult(true, message, Collections.emptyList());
        }

        static GateResult block(String message, List<String> items) {
            return new GateResult(false, message, items);
        }
    }
}
