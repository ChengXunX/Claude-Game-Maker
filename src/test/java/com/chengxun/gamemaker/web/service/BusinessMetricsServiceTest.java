package com.chengxun.gamemaker.web.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BusinessMetricsService 单元测试
 */
class BusinessMetricsServiceTest {

    private BusinessMetricsService metricsService;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new BusinessMetricsService(meterRegistry);
    }

    @Test
    void testRecordAgentCall() {
        metricsService.recordAgentCall();
        metricsService.recordAgentCall();
        metricsService.recordAgentCall();

        assertEquals(3.0, meterRegistry.counter("game_maker.agent.calls").count());
    }

    @Test
    void testRecordCapabilityCall() {
        metricsService.recordCapabilityCall();

        assertEquals(1.0, meterRegistry.counter("game_maker.capability.calls").count());
    }

    @Test
    void testRecordApproval() {
        metricsService.recordApproval();

        // 审批计数器有 tag: type=all
        var counter = meterRegistry.find("game_maker.approval.operations")
            .tag("type", "all").counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }

    @Test
    void testSetActiveAgentCount() {
        metricsService.setActiveAgentCount(5);

        // Gauge 注册在构造函数中，通过 meterRegistry 查找
        var gauge = meterRegistry.find("game_maker.agents.active").gauge();
        assertNotNull(gauge);
        assertEquals(5.0, gauge.value());
    }

    @Test
    void testSetPendingApprovalCount() {
        metricsService.setPendingApprovalCount(3);

        var gauge = meterRegistry.find("game_maker.approvals.pending").gauge();
        assertNotNull(gauge);
        assertEquals(3.0, gauge.value());
    }

    @Test
    void testRecordTokenConsumption() {
        metricsService.recordTokenConsumption(1000);
        metricsService.recordTokenConsumption(500);

        assertEquals(1500.0, meterRegistry.counter("game_maker.token.consumption").count());
    }

    @Test
    void testRecordApprovalDuration() {
        metricsService.recordApprovalDuration(1500);

        assertEquals(1, meterRegistry.timer("game_maker.approval.duration").count());
    }

    @Test
    void testRecordCapabilityDuration() {
        metricsService.recordCapabilityDuration(500);

        assertEquals(1, meterRegistry.timer("game_maker.capability.duration").count());
    }

    @Test
    void testMultipleOperations() {
        metricsService.recordAgentCall();
        metricsService.recordAgentCall();
        metricsService.recordCapabilityCall();
        metricsService.recordApproval();
        metricsService.setActiveAgentCount(3);

        assertEquals(2.0, meterRegistry.counter("game_maker.agent.calls").count());
        assertEquals(1.0, meterRegistry.counter("game_maker.capability.calls").count());

        var approvalCounter = meterRegistry.find("game_maker.approval.operations")
            .tag("type", "all").counter();
        assertNotNull(approvalCounter);
        assertEquals(1.0, approvalCounter.count());

        var gauge = meterRegistry.find("game_maker.agents.active").gauge();
        assertNotNull(gauge);
        assertEquals(3.0, gauge.value());
    }
}
