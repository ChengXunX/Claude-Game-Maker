package com.chengxun.gamemaker.engine;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.model.AgentMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * MessageBus单元测试
 *
 * @author chengxun
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class MessageBusTest {

    private MessageBus messageBus;

    @BeforeEach
    void setUp() {
        messageBus = new MessageBus();
    }

    @Test
    void testRegisterAgent() {
        // 准备
        Agent agent = mock(Agent.class);
        when(agent.getId()).thenReturn("agent-1");

        // 执行
        messageBus.registerAgent(agent);

        // 验证
        Map<String, Agent> agents = messageBus.getAgents();
        assertEquals(1, agents.size());
        assertTrue(agents.containsKey("agent-1"));
    }

    @Test
    void testUnregisterAgent() {
        // 准备
        Agent agent = mock(Agent.class);
        when(agent.getId()).thenReturn("agent-1");
        messageBus.registerAgent(agent);

        // 执行
        messageBus.unregisterAgent("agent-1");

        // 验证
        Map<String, Agent> agents = messageBus.getAgents();
        assertEquals(0, agents.size());
    }

    @Test
    void testSendMessageToSpecificAgent() {
        // 准备
        Agent agent1 = mock(Agent.class);
        Agent agent2 = mock(Agent.class);
        when(agent1.getId()).thenReturn("agent-1");
        when(agent2.getId()).thenReturn("agent-2");
        messageBus.registerAgent(agent1);
        messageBus.registerAgent(agent2);

        AgentMessage message = AgentMessage.builder()
            .fromAgentId("agent-1")
            .toAgentId("agent-2")
            .type(AgentMessage.MessageType.TASK)
            .content("Test task")
            .build();

        // 执行
        messageBus.send(message);

        // 验证
        verify(agent2).receiveMessage(message);
        verify(agent1, never()).receiveMessage(any());
    }

    @Test
    void testBroadcastMessage() {
        // 准备
        Agent agent1 = mock(Agent.class);
        Agent agent2 = mock(Agent.class);
        Agent agent3 = mock(Agent.class);
        when(agent1.getId()).thenReturn("agent-1");
        when(agent2.getId()).thenReturn("agent-2");
        when(agent3.getId()).thenReturn("agent-3");
        messageBus.registerAgent(agent1);
        messageBus.registerAgent(agent2);
        messageBus.registerAgent(agent3);

        AgentMessage message = AgentMessage.builder()
            .fromAgentId("agent-1")
            .type(AgentMessage.MessageType.NOTIFY)
            .content("Broadcast message")
            .build();

        // 执行
        messageBus.send(message);

        // 验证
        verify(agent1, never()).receiveMessage(message); // 发送者不接收
        verify(agent2).receiveMessage(message);
        verify(agent3).receiveMessage(message);
    }

    @Test
    void testSendToNonExistentAgent() {
        // 准备
        Agent agent1 = mock(Agent.class);
        when(agent1.getId()).thenReturn("agent-1");
        messageBus.registerAgent(agent1);

        AgentMessage message = AgentMessage.builder()
            .fromAgentId("agent-1")
            .toAgentId("non-existent")
            .type(AgentMessage.MessageType.TASK)
            .content("Test task")
            .build();

        // 执行 - 不应抛出异常
        messageBus.send(message);

        // 验证 - 没有Agent接收到消息
        verify(agent1, never()).receiveMessage(any());
    }

    @Test
    void testGetAgentsReturnsCopy() {
        // 准备
        Agent agent = mock(Agent.class);
        when(agent.getId()).thenReturn("agent-1");
        messageBus.registerAgent(agent);

        // 执行
        Map<String, Agent> agents1 = messageBus.getAgents();
        Map<String, Agent> agents2 = messageBus.getAgents();

        // 验证
        assertNotSame(agents1, agents2);
        assertEquals(agents1.size(), agents2.size());
    }
}
