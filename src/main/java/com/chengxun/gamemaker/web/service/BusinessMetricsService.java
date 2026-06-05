package com.chengxun.gamemaker.web.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 业务指标服务
 * 收集和暴露自定义业务指标
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class BusinessMetricsService {

    private final MeterRegistry meterRegistry;

    /** Agent 调用计数器 */
    private final Counter agentCallCounter;

    /** Token 消耗计数器 */
    private final Counter tokenConsumptionCounter;

    /** 审批操作计数器 */
    private final Counter approvalCounter;

    /** 能力调用计数器 */
    private final Counter capabilityCallCounter;

    /** 当前活跃 Agent 数 */
    private final AtomicInteger activeAgentCount = new AtomicInteger(0);

    /** 当前待审批数 */
    private final AtomicInteger pendingApprovalCount = new AtomicInteger(0);

    /** 审批耗时计时器 */
    private final Timer approvalTimer;

    /** 能力调用耗时计时器 */
    private final Timer capabilityTimer;

    public BusinessMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // 注册计数器
        this.agentCallCounter = Counter.builder("game_maker.agent.calls")
            .description("Agent 调用总次数")
            .register(meterRegistry);

        this.tokenConsumptionCounter = Counter.builder("game_maker.token.consumption")
            .description("Token 消耗总量")
            .register(meterRegistry);

        this.approvalCounter = Counter.builder("game_maker.approval.operations")
            .description("审批操作次数")
            .tag("type", "all")
            .register(meterRegistry);

        this.capabilityCallCounter = Counter.builder("game_maker.capability.calls")
            .description("能力调用总次数")
            .register(meterRegistry);

        // 注册 Gauge
        Gauge.builder("game_maker.agents.active", activeAgentCount, AtomicInteger::get)
            .description("当前活跃 Agent 数")
            .register(meterRegistry);

        Gauge.builder("game_maker.approvals.pending", pendingApprovalCount, AtomicInteger::get)
            .description("当前待审批数")
            .register(meterRegistry);

        // 注册计时器
        this.approvalTimer = Timer.builder("game_maker.approval.duration")
            .description("审批操作耗时")
            .register(meterRegistry);

        this.capabilityTimer = Timer.builder("game_maker.capability.duration")
            .description("能力调用耗时")
            .register(meterRegistry);
    }

    /** 记录 Agent 调用 */
    public void recordAgentCall() {
        agentCallCounter.increment();
    }

    /** 记录 Token 消耗 */
    public void recordTokenConsumption(long tokens) {
        tokenConsumptionCounter.increment(tokens);
    }

    /** 记录审批操作 */
    public void recordApproval() {
        approvalCounter.increment();
    }

    /** 记录能力调用 */
    public void recordCapabilityCall() {
        capabilityCallCounter.increment();
    }

    /** 记录审批耗时 */
    public void recordApprovalDuration(long durationMs) {
        approvalTimer.record(java.time.Duration.ofMillis(durationMs));
    }

    /** 记录能力调用耗时 */
    public void recordCapabilityDuration(long durationMs) {
        capabilityTimer.record(java.time.Duration.ofMillis(durationMs));
    }

    /** 更新活跃 Agent 数 */
    public void setActiveAgentCount(int count) {
        activeAgentCount.set(count);
    }

    /** 更新待审批数 */
    public void setPendingApprovalCount(int count) {
        pendingApprovalCount.set(count);
    }
}
