package com.chengxun.gamemaker.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Agent 资源配额管理
 * 每个 Agent 独立的资源配额控制
 *
 * @author chengxun
 * @since 2.0.0
 */
public class AgentResourceQuota {

    /** Agent ID */
    private final String agentId;

    /** 最大并发任务数 */
    private final int maxConcurrentTasks;

    /** 每分钟最大 API 调用次数 */
    private final int maxApiCallsPerMinute;

    /** 每小时最大 Token 消耗 */
    private final long maxTokensPerHour;

    /** 当前并发任务数 */
    private final AtomicInteger currentTasks = new AtomicInteger(0);

    /** 当前分钟 API 调用次数 */
    private final AtomicInteger apiCallsThisMinute = new AtomicInteger(0);

    /** 当前小时 Token 消耗 */
    private final AtomicLong tokensThisHour = new AtomicLong(0);

    /** 上次重置时间 */
    private volatile long lastMinuteReset = System.currentTimeMillis();
    private volatile long lastHourReset = System.currentTimeMillis();

    public AgentResourceQuota(String agentId, int maxConcurrentTasks,
                               int maxApiCallsPerMinute, long maxTokensPerHour) {
        this.agentId = agentId;
        this.maxConcurrentTasks = maxConcurrentTasks;
        this.maxApiCallsPerMinute = maxApiCallsPerMinute;
        this.maxTokensPerHour = maxTokensPerHour;
    }

    /** 默认配额 */
    public static AgentResourceQuota defaultQuota(String agentId) {
        return new AgentResourceQuota(agentId, 3, 60, 100000);
    }

    /** 检查是否可以接受新任务 */
    public boolean canAcceptTask() {
        return currentTasks.get() < maxConcurrentTasks;
    }

    /** 检查是否可以发起 API 调用 */
    public boolean canMakeApiCall() {
        resetIfNeeded();
        return apiCallsThisMinute.get() < maxApiCallsPerMinute;
    }

    /** 检查是否还有 Token 配额 */
    public boolean hasTokenQuota(long tokens) {
        resetIfNeeded();
        return tokensThisHour.get() + tokens <= maxTokensPerHour;
    }

    /** 开始任务 */
    public boolean startTask() {
        if (!canAcceptTask()) return false;
        currentTasks.incrementAndGet();
        return true;
    }

    /** 完成任务 */
    public void completeTask() {
        currentTasks.decrementAndGet();
    }

    /** 记录 API 调用 */
    public void recordApiCall() {
        resetIfNeeded();
        apiCallsThisMinute.incrementAndGet();
    }

    /** 记录 Token 消耗 */
    public void recordTokenConsumption(long tokens) {
        resetIfNeeded();
        tokensThisHour.addAndGet(tokens);
    }

    /** 重置计数器 */
    private void resetIfNeeded() {
        long now = System.currentTimeMillis();

        if (now - lastMinuteReset >= 60000) {
            apiCallsThisMinute.set(0);
            lastMinuteReset = now;
        }

        if (now - lastHourReset >= 3600000) {
            tokensThisHour.set(0);
            lastHourReset = now;
        }
    }

    /** 获取使用率 */
    public double getTaskUsage() {
        return maxConcurrentTasks > 0 ? (double) currentTasks.get() / maxConcurrentTasks : 0;
    }

    public double getApiCallUsage() {
        resetIfNeeded();
        return maxApiCallsPerMinute > 0 ? (double) apiCallsThisMinute.get() / maxApiCallsPerMinute : 0;
    }

    public double getTokenUsage() {
        resetIfNeeded();
        return maxTokensPerHour > 0 ? (double) tokensThisHour.get() / maxTokensPerHour : 0;
    }

    // Getters
    public String getAgentId() { return agentId; }
    public int getMaxConcurrentTasks() { return maxConcurrentTasks; }
    public int getMaxApiCallsPerMinute() { return maxApiCallsPerMinute; }
    public long getMaxTokensPerHour() { return maxTokensPerHour; }
    public int getCurrentTasks() { return currentTasks.get(); }
    public int getApiCallsThisMinute() { return apiCallsThisMinute.get(); }
    public long getTokensThisHour() { return tokensThisHour.get(); }
}
