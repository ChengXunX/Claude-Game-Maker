package com.chengxun.gamemaker.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Agent监控指标收集器
 * 收集Agent相关的性能指标
 *
 * 主要功能：
 * - 记录Agent工作耗时
 * - 统计Agent任务成功/失败次数
 * - 统计Agent消息发送/接收次数
 * - 监控Agent队列积压情况
 *
 * 指标列表：
 * - agent.work.duration: Agent工作耗时
 * - agent.work.total: Agent任务总数
 * - agent.message.total: Agent消息总数
 * - agent.queue.size: Agent队列大小
 * - agent.error.total: Agent错误总数
 *
 * @author chengxun
 * @since 1.0.0
 */
@Component
public class AgentMetrics {

    private static final Logger log = LoggerFactory.getLogger(AgentMetrics.class);

    private final MeterRegistry meterRegistry;

    /** Agent工作耗时计时器缓存 */
    private final ConcurrentHashMap<String, Timer> workTimers = new ConcurrentHashMap<>();

    /** Agent任务计数器缓存 */
    private final ConcurrentHashMap<String, Counter> taskCounters = new ConcurrentHashMap<>();

    /** Agent消息计数器缓存 */
    private final ConcurrentHashMap<String, Counter> messageCounters = new ConcurrentHashMap<>();

    /** Agent错误计数器缓存 */
    private final ConcurrentHashMap<String, Counter> errorCounters = new ConcurrentHashMap<>();

    public AgentMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        log.info("Agent监控指标收集器初始化完成");
    }

    /**
     * 记录Agent工作耗时
     *
     * @param agentId Agent ID
     * @param duration 耗时（毫秒）
     * @param success 是否成功
     */
    public void recordAgentWork(String agentId, long duration, boolean success) {
        try {
            // 获取或创建计时器
            Timer timer = workTimers.computeIfAbsent(agentId,
                id -> Timer.builder("agent.work.duration")
                    .description("Agent工作耗时")
                    .tag("agentId", id)
                    .register(meterRegistry));

            // 记录耗时
            timer.record(duration, TimeUnit.MILLISECONDS);

            // 记录任务计数
            String counterKey = agentId + "." + (success ? "success" : "failure");
            Counter counter = taskCounters.computeIfAbsent(counterKey,
                key -> Counter.builder("agent.work.total")
                    .description("Agent任务总数")
                    .tag("agentId", agentId)
                    .tag("success", String.valueOf(success))
                    .register(meterRegistry));

            counter.increment();

            log.debug("Agent工作指标已记录 - Agent: {}, 耗时: {}ms, 成功: {}", agentId, duration, success);
        } catch (Exception e) {
            log.error("记录Agent工作指标异常: {}", e.getMessage());
        }
    }

    /**
     * 记录Agent消息
     *
     * @param fromAgent 发送方Agent ID
     * @param toAgent 接收方Agent ID
     * @param type 消息类型
     */
    public void recordAgentMessage(String fromAgent, String toAgent, String type) {
        try {
            String counterKey = fromAgent + "." + toAgent + "." + type;
            Counter counter = messageCounters.computeIfAbsent(counterKey,
                key -> Counter.builder("agent.message.total")
                    .description("Agent消息总数")
                    .tag("from", fromAgent)
                    .tag("to", toAgent)
                    .tag("type", type)
                    .register(meterRegistry));

            counter.increment();

            log.debug("Agent消息指标已记录 - 从{}到{}, 类型: {}", fromAgent, toAgent, type);
        } catch (Exception e) {
            log.error("记录Agent消息指标异常: {}", e.getMessage());
        }
    }

    /**
     * 记录Agent错误
     *
     * @param agentId Agent ID
     * @param errorType 错误类型
     */
    public void recordAgentError(String agentId, String errorType) {
        try {
            String counterKey = agentId + "." + errorType;
            Counter counter = errorCounters.computeIfAbsent(counterKey,
                key -> Counter.builder("agent.error.total")
                    .description("Agent错误总数")
                    .tag("agentId", agentId)
                    .tag("errorType", errorType)
                    .register(meterRegistry));

            counter.increment();

            log.warn("Agent错误指标已记录 - Agent: {}, 错误类型: {}", agentId, errorType);
        } catch (Exception e) {
            log.error("记录Agent错误指标异常: {}", e.getMessage());
        }
    }

    /**
     * 记录Agent队列大小
     *
     * @param agentId Agent ID
     * @param queueSize 队列大小
     */
    public void recordAgentQueueSize(String agentId, int queueSize) {
        try {
            meterRegistry.gauge("agent.queue.size",
                io.micrometer.core.instrument.Tags.of("agentId", agentId),
                queueSize);

            log.debug("Agent队列大小已记录 - Agent: {}, 大小: {}", agentId, queueSize);
        } catch (Exception e) {
            log.error("记录Agent队列大小异常: {}", e.getMessage());
        }
    }

    /**
     * 记录Agent内存使用
     *
     * @param agentId Agent ID
     * @param memoryUsed 内存使用量（MB）
     */
    public void recordAgentMemoryUsage(String agentId, long memoryUsed) {
        try {
            meterRegistry.gauge("agent.memory.used",
                io.micrometer.core.instrument.Tags.of("agentId", agentId),
                memoryUsed);

            log.debug("Agent内存使用已记录 - Agent: {}, 使用: {}MB", agentId, memoryUsed);
        } catch (Exception e) {
            log.error("记录Agent内存使用异常: {}", e.getMessage());
        }
    }
}
