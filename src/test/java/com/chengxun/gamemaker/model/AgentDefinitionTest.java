package com.chengxun.gamemaker.model;

import com.chengxun.gamemaker.model.AgentDefinition.AgentStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentDefinitionTest {

    // ===== Builder =====

    @Test
    void builder_setsAllFields() {
        AgentDefinition def = AgentDefinition.builder()
                .id("agent-1")
                .name("TestAgent")
                .role("developer")
                .description("A test agent")
                .agentsFile("agents.yml")
                .apiKey("sk-abc")
                .apiUrl("https://api.test.com")
                .model("claude-sonnet")
                .sessionId("sess-1")
                .workDir("/tmp/work")
                .status(AgentStatus.WORKING)
                .parent(true)
                .parentId("parent-1")
                .reasoningDepth(4)
                .build();

        assertEquals("agent-1", def.getId());
        assertEquals("TestAgent", def.getName());
        assertEquals("developer", def.getRole());
        assertEquals("A test agent", def.getDescription());
        assertEquals("agents.yml", def.getAgentsFile());
        assertEquals("sk-abc", def.getApiKey());
        assertEquals("https://api.test.com", def.getApiUrl());
        assertEquals("claude-sonnet", def.getModel());
        assertEquals("sess-1", def.getSessionId());
        assertEquals("/tmp/work", def.getWorkDir());
        assertEquals(AgentStatus.WORKING, def.getStatus());
        assertTrue(def.isParent());
        assertEquals("parent-1", def.getParentId());
        assertEquals(4, def.getReasoningDepth());
    }

    @Test
    void builder_defaults_reasoningDepthIsThree() {
        AgentDefinition def = AgentDefinition.builder().id("x").build();
        assertEquals(3, def.getReasoningDepth());
        assertFalse(def.isParent());
        assertNull(def.getStatus());
    }

    // ===== Getters / Setters =====

    @Test
    void settersOverrideBuilderValues() {
        AgentDefinition def = AgentDefinition.builder()
                .id("old")
                .name("old-name")
                .status(AgentStatus.IDLE)
                .build();

        def.setId("new");
        def.setName("new-name");
        def.setRole("tester");
        def.setDescription("desc");
        def.setAgentsFile("af");
        def.setApiKey("key");
        def.setApiUrl("url");
        def.setModel("model");
        def.setSessionId("sid");
        def.setWorkDir("/dir");
        def.setStatus(AgentStatus.ERROR);
        def.setParent(true);
        def.setParentId("pid");

        assertEquals("new", def.getId());
        assertEquals("new-name", def.getName());
        assertEquals("tester", def.getRole());
        assertEquals("desc", def.getDescription());
        assertEquals("af", def.getAgentsFile());
        assertEquals("key", def.getApiKey());
        assertEquals("url", def.getApiUrl());
        assertEquals("model", def.getModel());
        assertEquals("sid", def.getSessionId());
        assertEquals("/dir", def.getWorkDir());
        assertEquals(AgentStatus.ERROR, def.getStatus());
        assertTrue(def.isParent());
        assertEquals("pid", def.getParentId());
    }

    // ===== Reasoning depth clamping =====

    @Test
    void setReasoningDepth_clampsToRange() {
        AgentDefinition def = AgentDefinition.builder().build();

        def.setReasoningDepth(0);
        assertEquals(1, def.getReasoningDepth());

        def.setReasoningDepth(-5);
        assertEquals(1, def.getReasoningDepth());

        def.setReasoningDepth(6);
        assertEquals(5, def.getReasoningDepth());

        def.setReasoningDepth(100);
        assertEquals(5, def.getReasoningDepth());
    }

    @Test
    void setReasoningDepth_acceptsValidRange() {
        AgentDefinition def = AgentDefinition.builder().build();
        for (int i = 1; i <= 5; i++) {
            def.setReasoningDepth(i);
            assertEquals(i, def.getReasoningDepth());
        }
    }

    // ===== getReasoningDepthLabel =====

    @Test
    void getReasoningDepthLabel_returnsCorrectLabels() {
        assertEquals("快速 (Quick)", AgentDefinition.getReasoningDepthLabel(1));
        assertEquals("标准 (Standard)", AgentDefinition.getReasoningDepthLabel(2));
        assertEquals("深入 (Deep)", AgentDefinition.getReasoningDepthLabel(3));
        assertEquals("全面 (Thorough)", AgentDefinition.getReasoningDepthLabel(4));
        assertEquals("极致 (Extreme)", AgentDefinition.getReasoningDepthLabel(5));
    }

    @Test
    void getReasoningDepthLabel_defaultsForOutOfRange() {
        assertEquals("深入 (Deep)", AgentDefinition.getReasoningDepthLabel(0));
        assertEquals("深入 (Deep)", AgentDefinition.getReasoningDepthLabel(6));
        assertEquals("深入 (Deep)", AgentDefinition.getReasoningDepthLabel(-1));
    }

    // ===== getReasoningDepthInstruction =====

    @Test
    void getReasoningDepthInstruction_returnsNonEmptyForAllDepths() {
        for (int i = 1; i <= 5; i++) {
            String instruction = AgentDefinition.getReasoningDepthInstruction(i);
            assertNotNull(instruction);
            assertFalse(instruction.isEmpty());
            assertTrue(instruction.contains("[推理模式:"));
        }
    }

    @Test
    void getReasoningDepthInstruction_defaultFallsBackToDepth3() {
        String instruction = AgentDefinition.getReasoningDepthInstruction(99);
        String depth3 = AgentDefinition.getReasoningDepthInstruction(3);
        assertEquals(depth3, instruction);
    }

    // ===== AgentStatus enum =====

    @Test
    void agentStatus_hasExpectedValues() {
        AgentStatus[] values = AgentStatus.values();
        assertEquals(5, values.length);
        assertNotNull(AgentStatus.IDLE);
        assertNotNull(AgentStatus.WORKING);
        assertNotNull(AgentStatus.WAITING);
        assertNotNull(AgentStatus.ERROR);
        assertNotNull(AgentStatus.STOPPED);
    }
}
