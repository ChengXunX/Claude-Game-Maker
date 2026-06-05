package com.chengxun.gamemaker.agent;

import com.chengxun.gamemaker.engine.ClaudeCliEngine;
import com.chengxun.gamemaker.engine.MessageBus;
import com.chengxun.gamemaker.manager.ContextManager;
import com.chengxun.gamemaker.manager.MemoryManager;
import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.manager.SkillManager;
import com.chengxun.gamemaker.model.*;
import com.chengxun.gamemaker.service.ContextCompactor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BaseAgent单元测试
 *
 * @author chengxun
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class BaseAgentTest {

    @Mock
    private ClaudeCliEngine cliEngine;

    @Mock
    private MessageBus messageBus;

    @Mock
    private ContextManager contextManager;

    @Mock
    private MemoryManager memoryManager;

    @Mock
    private SkillManager skillManager;

    @Mock
    private ProjectManager projectManager;

    @Mock
    private ContextCompactor contextCompactor;

    private TestAgent agent;
    private AgentDefinition definition;

    @BeforeEach
    void setUp() {
        definition = AgentDefinition.builder()
            .id("test-agent-1")
            .name("Test Agent")
            .role("test")
            .workDir("test-project")
            .projectId("test-project")
            .build();

        agent = new TestAgent(definition, cliEngine, messageBus, contextManager,
            memoryManager, skillManager, projectManager);
        agent.setContextCompactor(contextCompactor);

        // 初始化agentContext
        AgentContext context = new AgentContext();
        context.setWorkDir("test-project");
        agent.agentContext = context;

        // 初始化currentProject
        GameProject project = new GameProject();
        project.setId("test-project");
        project.setName("Test Project");
        agent.currentProject = project;

        // 初始化currentConversationId
        agent.currentConversationId = "test-conversation-1";
    }

    @Test
    void testAgentInitialization() {
        // 准备
        when(projectManager.getOrCreateProject("test-project")).thenReturn(new GameProject());
        when(contextManager.getOrCreateContext(anyString(), any())).thenReturn(new AgentContext());

        // 执行
        agent.initialize();

        // 验证：运行时 ID = projectId:role
        assertEquals("test-project:test", agent.getId());
        assertEquals("Test Agent", agent.getName());
        assertEquals("test", agent.getRole());
        assertEquals("test-project", agent.getProjectId());
        verify(messageBus).registerAgent(agent, "test-project");
    }

    @Test
    void testAgentStartStop() {
        // 启动
        agent.start();
        assertTrue(agent.isAlive());
        assertFalse(agent.isBusy());

        // 停止
        agent.stop();
        assertFalse(agent.isAlive());
    }

    @Test
    void testSendMessage() {
        // 准备：运行时 ID = projectId:role = test-project:test
        agent.start();
        when(cliEngine.sendMessage(eq("test-project:test"), any(), anyString(), eq("test-project"), any(), any(), any(), any()))
            .thenReturn("Response from AI");
        when(contextManager.loadConversation(eq("test-project:test"), any(), anyString())).thenReturn(new java.util.ArrayList<>());

        // 执行
        String response = agent.sendMessage("Test message");

        // 验证
        assertEquals("Response from AI", response);
    }

    @Test
    void testReceiveMessage() {
        // 准备
        AgentMessage message = AgentMessage.builder()
            .fromAgentId("other-agent")
            .toAgentId("test-agent-1")
            .type(AgentMessage.MessageType.TASK)
            .content("Test task")
            .build();

        // 执行
        agent.receiveMessage(message);

        // 验证
        List<AgentMessage> pending = agent.getPendingMessages();
        assertEquals(1, pending.size());
        assertEquals("Test task", pending.get(0).getContent());
    }

    @Test
    void testAssignTask() {
        // 准备
        TaskAssignment task = new TaskAssignment();
        task.setId("task-1");
        task.setTitle("Test Task");
        task.setAssignerId("producer");

        // 执行
        agent.assignTask(task);

        // 验证
        List<TaskAssignment> tasks = agent.getTasks();
        assertEquals(1, tasks.size());
        assertEquals("Test Task", tasks.get(0).getTitle());
    }

    @Test
    void testContextCompaction() {
        // 准备：运行时 ID = test-project:test
        when(contextCompactor.compactContext(eq("test-project:test"), any(), anyString())).thenReturn("Compacted summary");

        // 执行
        String result = agent.compactContext();

        // 验证
        assertEquals("Compacted summary", result);
    }

    @Test
    void testContextRecovery() {
        // 准备
        when(contextCompactor.recoverContext(anyString(), any())).thenReturn("Recovered context");

        // 执行
        String result = agent.recoverContext();

        // 验证
        assertEquals("Recovered context", result);
        verify(contextCompactor).recoverContext(anyString(), any());
    }

    @Test
    void testMemoryOperations() {
        // 保存记忆（运行时 ID = test-project:test）
        agent.saveMemory("key1", "value1");
        verify(memoryManager, atLeastOnce()).saveMemory(any(GameProject.class), eq("test-project:test"), eq("general"), eq("key1"), eq("value1"));

        // 加载记忆
        when(memoryManager.loadMemory(any(GameProject.class), eq("test-project:test"), eq("general"), eq("key1"))).thenReturn("value1");
        String value = agent.loadMemory("key1");
        assertEquals("value1", value);
    }

    @Test
    void testKnowledgeOperations() {
        // 保存知识
        agent.saveKnowledge("topic1", "knowledge content");
        verify(memoryManager, atLeastOnce()).saveMemory(any(GameProject.class), eq("test-project:test"), eq("knowledge"), eq("topic1"), eq("knowledge content"));

        // 加载知识
        when(memoryManager.loadMemory(any(GameProject.class), eq("test-project:test"), eq("knowledge"), eq("topic1"))).thenReturn("knowledge content");
        String knowledge = agent.loadKnowledge("topic1");
        assertEquals("knowledge content", knowledge);
    }

    @Test
    void testExperienceOperations() {
        // 保存经验
        agent.saveExperience("exp1", "experience content");
        verify(memoryManager, atLeastOnce()).saveMemory(any(GameProject.class), eq("test-project:test"), eq("experiences"), eq("exp1"), eq("experience content"));

        // 加载经验
        when(memoryManager.loadMemory(any(GameProject.class), eq("test-project:test"), eq("experiences"), eq("exp1"))).thenReturn("experience content");
        String experience = agent.loadExperience("exp1");
        assertEquals("experience content", experience);
    }

    @Test
    void testReviewMechanism() {
        // 准备
        agent.start();

        // 请求审查
        agent.requestReview("other-agent", "Please review this");

        // 验证
        verify(messageBus).send(any(AgentMessage.class));
    }

    @Test
    void testShouldCompactContext() {
        // 准备：模拟大量消息
        List<ContextManager.ConversationMessage> messages = new java.util.ArrayList<>();
        for (int i = 0; i < 60; i++) {
            messages.add(new ContextManager.ConversationMessage("user", "message " + i));
        }
        when(contextManager.getRecentMessages(anyString(), any(), eq(100))).thenReturn(messages);

        // 执行
        boolean shouldCompact = agent.shouldCompactContext();

        // 验证
        assertTrue(shouldCompact);
    }

    /**
     * 测试用Agent实现
     */
    private static class TestAgent extends BaseAgent {

        public TestAgent(AgentDefinition definition, ClaudeCliEngine cliEngine, MessageBus messageBus,
                        ContextManager contextManager, MemoryManager memoryManager,
                        SkillManager skillManager, ProjectManager projectManager) {
            super(definition, cliEngine, messageBus, contextManager, memoryManager, skillManager, projectManager);
        }

        @Override
        protected void doWork() {
            // 测试实现
        }

        @Override
        protected void handleMessage(AgentMessage message) {
            // 测试实现
        }
    }
}
