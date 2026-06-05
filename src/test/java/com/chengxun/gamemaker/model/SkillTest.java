package com.chengxun.gamemaker.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SkillTest {

    // ===== Builder =====

    @Test
    void builder_setsAllFields() {
        List<String> examples = Arrays.asList("example1", "example2");
        Skill skill = Skill.builder()
                .id("skill-1")
                .name("TestSkill")
                .description("A test skill")
                .category("custom")
                .triggerPattern("test,skill")
                .prompt("Do something")
                .examples(examples)
                .build();

        assertEquals("skill-1", skill.getId());
        assertEquals("TestSkill", skill.getName());
        assertEquals("A test skill", skill.getDescription());
        assertEquals("custom", skill.getCategory());
        assertEquals("test,skill", skill.getTriggerPattern());
        assertEquals("Do something", skill.getPrompt());
        assertEquals(examples, skill.getExamples());
    }

    @Test
    void constructor_setsCreatedAt() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        Skill skill = new Skill();
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertNotNull(skill.getCreatedAt());
        assertFalse(skill.getCreatedAt().isBefore(before));
        assertFalse(skill.getCreatedAt().isAfter(after));
    }

    @Test
    void constructor_initializesEmptyExamples() {
        Skill skill = new Skill();
        assertNotNull(skill.getExamples());
        assertTrue(skill.getExamples().isEmpty());
    }

    @Test
    void constructor_initializesUsageCountToZero() {
        Skill skill = new Skill();
        assertEquals(0, skill.getUsageCount());
    }

    // ===== Getters / Setters =====

    @Test
    void settersOverrideValues() {
        Skill skill = new Skill();
        LocalDateTime now = LocalDateTime.now();

        skill.setId("id");
        skill.setName("name");
        skill.setDescription("desc");
        skill.setCategory("builtin");
        skill.setTriggerPattern("pattern");
        skill.setPrompt("prompt");
        skill.setExamples(List.of("a"));
        skill.setUsageCount(5);
        skill.setCreatedAt(now);
        skill.setLastUsedAt(now);

        assertEquals("id", skill.getId());
        assertEquals("name", skill.getName());
        assertEquals("desc", skill.getDescription());
        assertEquals("builtin", skill.getCategory());
        assertEquals("pattern", skill.getTriggerPattern());
        assertEquals("prompt", skill.getPrompt());
        assertEquals(List.of("a"), skill.getExamples());
        assertEquals(5, skill.getUsageCount());
        assertEquals(now, skill.getCreatedAt());
        assertEquals(now, skill.getLastUsedAt());
    }

    // ===== recordUsage =====

    @Test
    void recordUsage_incrementsCountAndSetsLastUsedAt() {
        Skill skill = new Skill();
        assertNull(skill.getLastUsedAt());

        skill.recordUsage();
        assertEquals(1, skill.getUsageCount());
        assertNotNull(skill.getLastUsedAt());

        skill.recordUsage();
        assertEquals(2, skill.getUsageCount());
    }

    // ===== toPromptSection =====

    @Test
    void toPromptSection_withExamples() {
        Skill skill = Skill.builder()
                .name("CodeReview")
                .description("Reviews code quality")
                .prompt("Review the following code...")
                .examples(List.of("Check naming conventions", "Find bugs"))
                .build();

        String section = skill.toPromptSection();

        assertTrue(section.contains("### SKILL: CodeReview"));
        assertTrue(section.contains("**Reviews code quality**"));
        assertTrue(section.contains("Review the following code..."));
        assertTrue(section.contains("**示例：**"));
        assertTrue(section.contains("- Check naming conventions"));
        assertTrue(section.contains("- Find bugs"));
    }

    @Test
    void toPromptSection_withoutExamples() {
        Skill skill = Skill.builder()
                .name("SimpleSkill")
                .description("Simple")
                .prompt("Do it")
                .build();

        String section = skill.toPromptSection();

        assertTrue(section.contains("### SKILL: SimpleSkill"));
        assertTrue(section.contains("**Simple**"));
        assertTrue(section.contains("Do it"));
        assertFalse(section.contains("示例"));
    }
}
