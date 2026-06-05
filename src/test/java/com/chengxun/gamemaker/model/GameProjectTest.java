package com.chengxun.gamemaker.model;

import com.chengxun.gamemaker.model.GameProject.ProjectStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class GameProjectTest {

    // ===== Constructor defaults =====

    @Test
    void constructor_setsDefaults() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        GameProject project = new GameProject();
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertEquals(ProjectStatus.CREATED, project.getStatus());
        assertNotNull(project.getCreatedAt());
        assertNotNull(project.getLastActiveAt());
        assertFalse(project.getCreatedAt().isBefore(before));
        assertFalse(project.getCreatedAt().isAfter(after));
        assertNotNull(project.getAgentIds());
        assertTrue(project.getAgentIds().isEmpty());
        assertNotNull(project.getMetadata());
        assertTrue(project.getMetadata().isEmpty());
    }

    // ===== Builder =====

    @Test
    void builder_setsFieldsAndComputesConfigDir() {
        GameProject project = GameProject.builder()
                .id("proj-1")
                .name("TestProject")
                .description("A test project")
                .workDir("/home/user/project")
                .status(ProjectStatus.ACTIVE)
                .build();

        assertEquals("proj-1", project.getId());
        assertEquals("TestProject", project.getName());
        assertEquals("A test project", project.getDescription());
        assertEquals("/home/user/project", project.getWorkDir());
        assertEquals(ProjectStatus.ACTIVE, project.getStatus());
        assertEquals("/home/user/project/.game-maker", project.getProjectConfigDir());
    }

    @Test
    void builder_doesNotSetConfigDirWhenWorkDirIsNull() {
        GameProject project = GameProject.builder()
                .id("proj-2")
                .name("NoDir")
                .build();

        assertNull(project.getWorkDir());
        assertNull(project.getProjectConfigDir());
    }

    // ===== Path helpers =====

    @Test
    void pathHelpers_returnCorrectPaths() {
        GameProject project = GameProject.builder()
                .workDir("/proj")
                .build();

        assertEquals("/proj/.game-maker", project.getProjectConfigDir());
        assertEquals("/proj/.game-maker/skills", project.getSkillsDir());
        assertEquals("/proj/.game-maker/memory", project.getMemoryDir());
        assertEquals("/proj/.game-maker/contexts", project.getContextsDir());
        assertEquals("/proj/.game-maker/project.json", project.getProjectConfigFile());
        assertEquals("/proj/.game-maker/rules.md", project.getProjectRulesFile());
    }

    // ===== addAgent =====

    @Test
    void addAgent_addsNewAgent() {
        GameProject project = new GameProject();
        project.addAgent("agent-1");

        assertEquals(1, project.getAgentIds().size());
        assertTrue(project.getAgentIds().contains("agent-1"));
    }

    @Test
    void addAgent_doesNotAddDuplicate() {
        GameProject project = new GameProject();
        project.addAgent("agent-1");
        project.addAgent("agent-1");

        assertEquals(1, project.getAgentIds().size());
    }

    @Test
    void addAgent_allowsMultipleDifferent() {
        GameProject project = new GameProject();
        project.addAgent("agent-1");
        project.addAgent("agent-2");
        project.addAgent("agent-3");

        assertEquals(3, project.getAgentIds().size());
    }

    // ===== removeAgent =====

    @Test
    void removeAgent_removesExistingAgent() {
        GameProject project = new GameProject();
        project.addAgent("agent-1");
        project.addAgent("agent-2");
        project.removeAgent("agent-1");

        assertEquals(1, project.getAgentIds().size());
        assertFalse(project.getAgentIds().contains("agent-1"));
        assertTrue(project.getAgentIds().contains("agent-2"));
    }

    @Test
    void removeAgent_noopWhenAgentNotPresent() {
        GameProject project = new GameProject();
        project.addAgent("agent-1");
        project.removeAgent("nonexistent");

        assertEquals(1, project.getAgentIds().size());
    }

    // ===== touch =====

    @Test
    void touch_updatesLastActiveAt() {
        GameProject project = new GameProject();
        LocalDateTime original = project.getLastActiveAt();

        // Ensure a measurable time difference
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        project.touch();

        assertNotNull(project.getLastActiveAt());
        assertFalse(project.getLastActiveAt().isBefore(original));
    }

    // ===== Getters / Setters =====

    @Test
    void settersOverrideValues() {
        GameProject project = new GameProject();
        LocalDateTime now = LocalDateTime.now();

        project.setId("id");
        project.setName("name");
        project.setDescription("desc");
        project.setWorkDir("/wd");
        project.setProjectConfigDir("/pcd");
        project.setStatus(ProjectStatus.COMPLETED);
        project.setMetadata(java.util.Map.of("k", "v"));
        project.setCreatedAt(now);
        project.setLastActiveAt(now);

        assertEquals("id", project.getId());
        assertEquals("name", project.getName());
        assertEquals("desc", project.getDescription());
        assertEquals("/wd", project.getWorkDir());
        assertEquals("/pcd", project.getProjectConfigDir());
        assertEquals(ProjectStatus.COMPLETED, project.getStatus());
        assertEquals("v", project.getMetadata().get("k"));
        assertEquals(now, project.getCreatedAt());
        assertEquals(now, project.getLastActiveAt());
    }

    // ===== ProjectStatus enum =====

    @Test
    void projectStatus_hasExpectedValues() {
        ProjectStatus[] values = ProjectStatus.values();
        assertEquals(5, values.length);
        assertNotNull(ProjectStatus.CREATED);
        assertNotNull(ProjectStatus.ACTIVE);
        assertNotNull(ProjectStatus.PAUSED);
        assertNotNull(ProjectStatus.COMPLETED);
        assertNotNull(ProjectStatus.ARCHIVED);
    }
}
