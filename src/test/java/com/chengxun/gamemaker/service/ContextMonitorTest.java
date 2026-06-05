package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.engine.ClaudeCliEngine;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.manager.ContextManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ContextMonitor 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ContextMonitorTest {

    @Mock
    private AgentManager agentManager;

    @Mock
    private ClaudeCliEngine cliEngine;

    @Mock
    private ContextManager contextManager;

    @InjectMocks
    private ContextMonitor contextMonitor;

    @Test
    void testGetAllHealthStatusEmpty() {
        Map<String, ContextMonitor.ContextHealthStatus> result = contextMonitor.getAllHealthStatus();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetHealthStatusNotFound() {
        ContextMonitor.ContextHealthStatus result = contextMonitor.getHealthStatus("nonexistent");

        assertNull(result);
    }

    @Test
    void testGetHealthSummary() {
        Map<String, Object> summary = contextMonitor.getHealthSummary();

        assertNotNull(summary);
        assertEquals(0, summary.get("total"));
        assertEquals(0, summary.get("healthy"));
    }

    @Test
    void testUpdateActivityTime() {
        contextMonitor.updateActivityTime("test-agent");

        // 应该不抛异常
        assertNotNull(contextMonitor.getHealthSummary());
    }

    @Test
    void testManualRecoverAgentNotFound() {
        when(agentManager.getAgent("nonexistent")).thenReturn(null);

        boolean result = contextMonitor.manualRecover("nonexistent");

        assertFalse(result);
    }

    @Test
    void testCheckAgentContextWithNoAgent() {
        when(agentManager.getAllAgents()).thenReturn(List.of());

        // 不应该抛异常
        contextMonitor.checkAllAgentsContext();

        verify(agentManager).getAllAgents();
    }

    @Test
    void testContextHealthStatusMarkHealthy() {
        ContextMonitor.ContextHealthStatus status = new ContextMonitor.ContextHealthStatus("test-agent");

        status.markHealthy();

        assertTrue(status.isHealthy());
        assertNull(status.getIssue());
        assertEquals(ContextMonitor.ContextHealthStatus.Severity.NORMAL, status.getSeverity());
    }

    @Test
    void testContextHealthStatusMarkUnhealthy() {
        ContextMonitor.ContextHealthStatus status = new ContextMonitor.ContextHealthStatus("test-agent");

        status.markUnhealthy("进程死亡", ContextMonitor.ContextHealthStatus.Severity.ERROR);

        assertFalse(status.isHealthy());
        assertEquals("进程死亡", status.getIssue());
        assertEquals(ContextMonitor.ContextHealthStatus.Severity.ERROR, status.getSeverity());
    }

    @Test
    void testContextHealthStatusMarkRecoveryAttempted() {
        ContextMonitor.ContextHealthStatus status = new ContextMonitor.ContextHealthStatus("test-agent");

        status.markRecoveryAttempted();

        assertNotNull(status.getLastRecovery());
    }
}
