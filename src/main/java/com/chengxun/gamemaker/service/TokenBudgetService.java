package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.config.SystemConstants;
import com.chengxun.gamemaker.web.service.NotificationService;
import com.chengxun.gamemaker.web.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token 预算控制服务
 * 管理每个 Agent 的每日 Token 消耗预算，超预算时暂停 Agent 并通知管理员
 *
 * 主要功能：
 * - 跟踪每个 Agent 的每日 Token 消耗
 * - 检查是否超出预算限制
 * - 超预算时暂停 Agent 并发送告警
 * - 每日自动重置计数器
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class TokenBudgetService {

    private static final Logger log = LoggerFactory.getLogger(TokenBudgetService.class);

    @Autowired
    private SystemConfigService configService;

    @Autowired(required = false)
    private NotificationService notificationService;

    /**
     * Agent Token 使用记录
     * key: agentId
     * value: 使用记录
     */
    private final ConcurrentHashMap<String, AgentTokenUsage> usageMap = new ConcurrentHashMap<>();

    /**
     * Agent Token 使用记录
     */
    public static class AgentTokenUsage {
        /** 今日已使用 Token 数 */
        private long usedToday;
        /** 今日输入 Token 数 */
        private long inputTokensToday;
        /** 今日输出 Token 数 */
        private long outputTokensToday;
        /** 今日调用次数 */
        private long callCountToday;
        /** 最后更新日期（用于检测日期切换） */
        private LocalDate lastDate;
        /** 最近一次调用的 Token 数 */
        private long lastCallTokens;
        /** 最近一次调用时间 */
        private long lastCallTime;

        public AgentTokenUsage() {
            this.lastDate = LocalDate.now();
        }

        /** 如果日期已切换，重置计数器 */
        public void resetIfNewDay() {
            LocalDate today = LocalDate.now();
            if (!today.equals(lastDate)) {
                this.usedToday = 0;
                this.inputTokensToday = 0;
                this.outputTokensToday = 0;
                this.callCountToday = 0;
                this.lastDate = today;
            }
        }

        // Getters and setters
        public long getUsedToday() { return usedToday; }
        public long getInputTokensToday() { return inputTokensToday; }
        public long getOutputTokensToday() { return outputTokensToday; }
        public long getCallCountToday() { return callCountToday; }
        public long getLastCallTokens() { return lastCallTokens; }
        public long getLastCallTime() { return lastCallTime; }
    }

    /**
     * 记录 Token 使用并检查预算
     *
     * @param agentId Agent ID
     * @param inputTokens 输入 Token 数
     * @param outputTokens 输出 Token 数
     * @return 是否超出预算（true=正常，false=已超预算）
     */
    public boolean recordUsage(String agentId, long inputTokens, long outputTokens) {
        AgentTokenUsage usage = usageMap.computeIfAbsent(agentId, k -> new AgentTokenUsage());
        synchronized (usage) {
            usage.resetIfNewDay();
            long totalTokens = inputTokens + outputTokens;
            usage.usedToday += totalTokens;
            usage.inputTokensToday += inputTokens;
            usage.outputTokensToday += outputTokens;
            usage.callCountToday++;
            usage.lastCallTokens = totalTokens;
            usage.lastCallTime = System.currentTimeMillis();
        }

        // 检查预算
        long budget = getDailyBudget();
        if (budget <= 0) return true; // 不限制

        long threshold = configService.getInt(SystemConstants.AGENT_TOKEN_BUDGET_ALERT_THRESHOLD, 80);
        long alertLevel = budget * threshold / 100;

        if (usage.usedToday >= budget) {
            log.error("Agent {} 已超出每日 Token 预算: {}/{}", agentId, usage.usedToday, budget);
            notifyAdminTokenBudgetExceeded(agentId, usage.usedToday, budget);
            return false;
        } else if (usage.usedToday >= alertLevel) {
            log.warn("Agent {} Token 消耗接近预算: {}/{} ({}%)", agentId, usage.usedToday, budget, threshold);
        }

        // 单次调用告警
        long perCallAlert = configService.getInt(SystemConstants.AGENT_TOKEN_ALERT_PER_CALL, 10000);
        long lastCallTokens = usage.getLastCallTokens();
        if (lastCallTokens > perCallAlert) {
            log.warn("Agent {} 单次调用 Token 消耗过高: {} (阈值: {})", agentId, lastCallTokens, perCallAlert);
        }

        return true;
    }

    /**
     * 检查 Agent 是否在预算内
     *
     * @param agentId Agent ID
     * @return true=在预算内，false=已超预算
     */
    public boolean checkBudget(String agentId) {
        long budget = getDailyBudget();
        if (budget <= 0) return true; // 不限制

        AgentTokenUsage usage = usageMap.get(agentId);
        if (usage == null) return true;

        synchronized (usage) {
            usage.resetIfNewDay();
            return usage.usedToday < budget;
        }
    }

    /**
     * 获取 Agent 剩余预算
     *
     * @param agentId Agent ID
     * @return 剩余 Token 数，-1 表示不限制
     */
    public long getRemainingBudget(String agentId) {
        long budget = getDailyBudget();
        if (budget <= 0) return -1; // 不限制

        AgentTokenUsage usage = usageMap.get(agentId);
        if (usage == null) return budget;

        synchronized (usage) {
            usage.resetIfNewDay();
            return Math.max(0, budget - usage.usedToday);
        }
    }

    /**
     * 获取 Agent 今日使用统计
     *
     * @param agentId Agent ID
     * @return 使用统计，不存在返回 null
     */
    public AgentTokenUsage getUsage(String agentId) {
        AgentTokenUsage usage = usageMap.get(agentId);
        if (usage != null) {
            synchronized (usage) {
                usage.resetIfNewDay();
            }
        }
        return usage;
    }

    /**
     * 获取所有 Agent 的使用统计
     */
    public Map<String, AgentTokenUsage> getAllUsage() {
        usageMap.values().forEach(u -> { synchronized (u) { u.resetIfNewDay(); } });
        return Map.copyOf(usageMap);
    }

    /**
     * 获取每日 Token 预算（从配置读取）
     */
    private long getDailyBudget() {
        return configService.getLong(SystemConstants.AGENT_TOKEN_BUDGET_DAILY, 0);
    }

    /**
     * 通知管理员 Token 预算超限
     */
    private void notifyAdminTokenBudgetExceeded(String agentId, long used, long budget) {
        if (notificationService == null) return;
        try {
            notificationService.sendSystemNotification(
                null,
                "Agent Token 预算超限",
                String.format("Agent **%s** 已超出每日 Token 预算。\n\n已使用: %,d tokens\n预算上限: %,d tokens\n\nAgent 已暂停，请检查 Token 配额。",
                    agentId, used, budget),
                com.chengxun.gamemaker.web.entity.Notification.NotificationType.SYSTEM
            );
        } catch (Exception e) {
            log.warn("Failed to send token budget notification: {}", e.getMessage());
        }
    }
}
