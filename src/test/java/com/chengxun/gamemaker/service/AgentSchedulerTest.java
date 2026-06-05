package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.agent.BaseAgent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.model.TaskAssignment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AgentScheduler单元测试
 *
 * @author chengxun
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class AgentSchedulerTest {

    @Mock
    private AgentManager agentManager;

    @Mock
    private ContextCompactor contextCompactor;

    @InjectMocks
    private AgentScheduler agentScheduler;

    private Agent mockAgent;
    private BaseAgent mockBaseAgent;

    @BeforeEach
    void setUp() {
        mockAgent = mock(Agent.class);
        mockBaseAgent = mock(BaseAgent.class);
    }

    @Test
    void testAssignTask() {
        // 准备
        when(agentManager.getAgent("agent-1")).thenReturn(mockBaseAgent);
        when(agentManager.canAcceptTask("agent-1")).thenReturn(true);

        TaskAssignment task = new TaskAssignment();
        task.setId("task-1");
        task.setTitle("Test Task");

        // 执行
        agentScheduler.assignTask("agent-1", task);

        // 验证
        verify(mockBaseAgent).assignTask(task);
    }

    @Test
    void testAssignTaskToNonExistentAgent() {
        // 准备
        when(agentManager.getAgent("non-existent")).thenReturn(null);

        TaskAssignment task = new TaskAssignment();
        task.setId("task-1");
        task.setTitle("Test Task");

        // 执行 - 不应抛出异常
        agentScheduler.assignTask("non-existent", task);

        // 验证
        verify(mockBaseAgent, never()).assignTask(any());
    }

    @Test
    void testSendMessage() {
        // 准备
        when(agentManager.getAgent("agent-1")).thenReturn(mockBaseAgent);
        when(mockBaseAgent.sendMessage("Test message")).thenReturn("Response");

        // 执行
        String response = agentScheduler.sendMessage("agent-1", "Test message");

        // 验证
        assertEquals("Response", response);
        verify(mockBaseAgent).sendMessage("Test message");
    }

    @Test
    void testSendMessageToNonExistentAgent() {
        // 准备
        when(agentManager.getAgent("non-existent")).thenReturn(null);

        // 执行
        String response = agentScheduler.sendMessage("non-existent", "Test message");

        // 验证
        assertTrue(response.contains("Agent不存在"));
    }

    @Test
    void testStartAgent() {
        // 准备
        when(agentManager.getAgent("agent-1")).thenReturn(mockAgent);

        // 执行
        agentScheduler.startAgent("agent-1");

        // 验证
        verify(mockAgent).start();
    }

    @Test
    void testStopAgent() {
        // 准备
        when(agentManager.getAgent("agent-1")).thenReturn(mockBaseAgent);

        // 执行
        agentScheduler.stopAgent("agent-1");

        // 验证
        verify(mockBaseAgent).stop();
    }

    @Test
    void testCompactAgentContext() {
        // 准备
        when(agentManager.getAgent("agent-1")).thenReturn(mockBaseAgent);
        when(mockBaseAgent.compactContext()).thenReturn("Compacted");

        // 执行
        String result = agentScheduler.compactAgentContext("agent-1");

        // 验证
        assertEquals("Compacted", result);
    }

    @Test
    void testGetAgentStatus() {
        // 准备
        when(agentManager.getAgent("agent-1")).thenReturn(mockBaseAgent);
        when(mockBaseAgent.getId()).thenReturn("agent-1");
        when(mockBaseAgent.getName()).thenReturn("Test Agent");
        when(mockBaseAgent.getRole()).thenReturn("test");
        when(mockBaseAgent.isAlive()).thenReturn(true);
        when(mockBaseAgent.isBusy()).thenReturn(false);

        // 执行
        Map<String, Object> status = agentScheduler.getAgentStatus("agent-1");

        // 验证
        assertEquals("agent-1", status.get("id"));
        assertEquals("Test Agent", status.get("name"));
        assertEquals("test", status.get("role"));
        assertEquals(true, status.get("alive"));
        assertEquals(false, status.get("busy"));
    }

    @Test
    void testGetAllAgentStatus() {
        // 准备
        Agent agent1 = mock(Agent.class);
        Agent agent2 = mock(Agent.class);

        when(agent1.getId()).thenReturn("agent-1");
        when(agent1.getName()).thenReturn("Agent 1");
        when(agent1.getRole()).thenReturn("role-1");
        when(agent1.isAlive()).thenReturn(true);
        when(agent1.isBusy()).thenReturn(false);

        when(agent2.getId()).thenReturn("agent-2");
        when(agent2.getName()).thenReturn("Agent 2");
        when(agent2.getRole()).thenReturn("role-2");
        when(agent2.isAlive()).thenReturn(true);
        when(agent2.isBusy()).thenReturn(true);

        when(agentManager.getAllAgents()).thenReturn(List.of(agent1, agent2));
        when(agentManager.getAgent("agent-1")).thenReturn(agent1);
        when(agentManager.getAgent("agent-2")).thenReturn(agent2);

        // 执行
        List<Map<String, Object>> statuses = agentScheduler.getAllAgentStatus();

        // 验证
        assertEquals(2, statuses.size());
    }
}
