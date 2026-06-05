package com.chengxun.gamemaker.manager;

import com.chengxun.gamemaker.config.AppConfig;
import com.chengxun.gamemaker.model.Skill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillManagerTest {

    @Mock
    private AppConfig appConfig;

    private SkillManager skillManager;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Use lenient because many tests don't exercise file I/O
        lenient().when(appConfig.getDataDir()).thenReturn(tempDir.toString());
        skillManager = new SkillManager(appConfig);
        // skip init() to avoid loading from real filesystem
    }

    // ===== Global skill CRUD =====

    @Test
    void registerGlobalSkill_thenGetGlobalSkill() {
        Skill skill = Skill.builder().id("s1").name("Skill1").build();
        skillManager.registerGlobalSkill(skill);

        assertEquals(skill, skillManager.getGlobalSkill("s1"));
    }

    @Test
    void getGlobalSkill_returnsNullForUnknownId() {
        assertNull(skillManager.getGlobalSkill("unknown"));
    }

    @Test
    void getAllGlobalSkills_returnsAllRegistered() {
        skillManager.registerGlobalSkill(Skill.builder().id("a").name("A").build());
        skillManager.registerGlobalSkill(Skill.builder().id("b").name("B").build());

        List<Skill> all = skillManager.getAllGlobalSkills();
        assertEquals(2, all.size());
    }

    @Test
    void removeGlobalSkill_removesSkill() {
        Skill skill = Skill.builder().id("s1").name("Skill1").build();
        skillManager.registerGlobalSkill(skill);
        skillManager.removeGlobalSkill("s1");

        assertNull(skillManager.getGlobalSkill("s1"));
    }

    // ===== Project skill CRUD =====

    @Test
    void registerProjectSkill_thenGetProjectSkills() {
        Skill skill = Skill.builder().id("ps1").name("ProjSkill").build();
        skillManager.registerProjectSkill("proj-1", skill);

        Map<String, Skill> skills = skillManager.getProjectSkills("proj-1");
        assertEquals(1, skills.size());
        assertEquals(skill, skills.get("ps1"));
    }

    @Test
    void getProjectSkills_returnsEmptyMapForUnknownProject() {
        Map<String, Skill> skills = skillManager.getProjectSkills("nonexistent");
        assertNotNull(skills);
        assertTrue(skills.isEmpty());
    }

    @Test
    void registerProjectSkill_multipleSkillsForSameProject() {
        skillManager.registerProjectSkill("proj-1", Skill.builder().id("a").name("A").build());
        skillManager.registerProjectSkill("proj-1", Skill.builder().id("b").name("B").build());

        Map<String, Skill> skills = skillManager.getProjectSkills("proj-1");
        assertEquals(2, skills.size());
    }

    // ===== matchSkills =====

    @Test
    void matchSkills_matchesGlobalSkillByKeyword() {
        skillManager.registerGlobalSkill(Skill.builder()
                .id("git").name("Git").triggerPattern("git,commit,push").build());

        List<Skill> matched = skillManager.matchSkills("please commit the code", "any-project");
        assertEquals(1, matched.size());
        assertEquals("git", matched.get(0).getId());
    }

    @Test
    void matchSkills_matchesProjectSkillFirst() {
        skillManager.registerGlobalSkill(Skill.builder()
                .id("global").name("Global").triggerPattern("deploy").build());
        skillManager.registerProjectSkill("proj-1", Skill.builder()
                .id("proj").name("Proj").triggerPattern("deploy").build());

        List<Skill> matched = skillManager.matchSkills("deploy the app", "proj-1");
        assertEquals(2, matched.size());
        // project skill comes first
        assertEquals("proj", matched.get(0).getId());
        assertEquals("global", matched.get(1).getId());
    }

    @Test
    void matchSkills_returnsEmptyWhenNoMatch() {
        skillManager.registerGlobalSkill(Skill.builder()
                .id("git").name("Git").triggerPattern("git,commit").build());

        List<Skill> matched = skillManager.matchSkills("write documentation", "any-project");
        assertTrue(matched.isEmpty());
    }

    @Test
    void matchSkills_sortsByUsageCountDescending() {
        Skill high = Skill.builder().id("high").name("High").triggerPattern("code").build();
        high.setUsageCount(10);
        Skill low = Skill.builder().id("low").name("Low").triggerPattern("code").build();
        low.setUsageCount(1);

        skillManager.registerGlobalSkill(low);
        skillManager.registerGlobalSkill(high);

        List<Skill> matched = skillManager.matchSkills("write code", "any-project");
        assertEquals(2, matched.size());
        assertEquals("high", matched.get(0).getId());
        assertEquals("low", matched.get(1).getId());
    }

    @Test
    void matchSkills_matchesMultipleKeywords() {
        skillManager.registerGlobalSkill(Skill.builder()
                .id("db").name("DB").triggerPattern("database,sql,query").build());

        List<Skill> m1 = skillManager.matchSkills("run a sql query", "any-project");
        assertEquals(1, m1.size());

        List<Skill> m2 = skillManager.matchSkills("check the database", "any-project");
        assertEquals(1, m2.size());
    }

    // ===== getAllSkills =====

    @Test
    void getAllSkills_returnsProjectAndGlobalSkills() {
        skillManager.registerGlobalSkill(Skill.builder().id("g1").name("G1").build());
        skillManager.registerProjectSkill("proj-1", Skill.builder().id("p1").name("P1").build());

        List<Skill> all = skillManager.getAllSkills("proj-1");
        assertEquals(2, all.size());
    }

    @Test
    void getAllSkills_onlyGlobalWhenNoProjectSkills() {
        skillManager.registerGlobalSkill(Skill.builder().id("g1").name("G1").build());

        List<Skill> all = skillManager.getAllSkills("proj-1");
        assertEquals(1, all.size());
        assertEquals("g1", all.get(0).getId());
    }

    // ===== buildSkillPrompt =====

    @Test
    void buildSkillPrompt_returnsEmptyForNoSkills() {
        assertEquals("", skillManager.buildSkillPrompt(List.of()));
    }

    @Test
    void buildSkillPrompt_containsAllSkillSections() {
        Skill s1 = Skill.builder().id("a").name("SkillA").description("DescA").prompt("P1").build();
        Skill s2 = Skill.builder().id("b").name("SkillB").description("DescB").prompt("P2").build();

        String prompt = skillManager.buildSkillPrompt(List.of(s1, s2));

        assertTrue(prompt.contains("## 可用技能"));
        assertTrue(prompt.contains("### SKILL: SkillA"));
        assertTrue(prompt.contains("### SKILL: SkillB"));
    }

    // ===== recordSkillUsage =====

    @Test
    void recordSkillUsage_incrementsProjectSkillUsage() {
        Skill skill = Skill.builder().id("ps1").name("PS").build();
        skillManager.registerProjectSkill("proj-1", skill);

        skillManager.recordSkillUsage("ps1", "proj-1");
        assertEquals(1, skill.getUsageCount());

        skillManager.recordSkillUsage("ps1", "proj-1");
        assertEquals(2, skill.getUsageCount());
    }

    @Test
    void recordSkillUsage_incrementsGlobalSkillUsage() {
        Skill skill = Skill.builder().id("gs1").name("GS").build();
        skillManager.registerGlobalSkill(skill);

        skillManager.recordSkillUsage("gs1", "proj-1");
        assertEquals(1, skill.getUsageCount());
    }

    @Test
    void recordSkillUsage_prefersProjectOverGlobal() {
        Skill projectSkill = Skill.builder().id("shared").name("ProjVer").build();
        Skill globalSkill = Skill.builder().id("shared").name("GlobalVer").build();
        skillManager.registerProjectSkill("proj-1", projectSkill);
        skillManager.registerGlobalSkill(globalSkill);

        skillManager.recordSkillUsage("shared", "proj-1");
        assertEquals(1, projectSkill.getUsageCount());
        assertEquals(0, globalSkill.getUsageCount());
    }

    @Test
    void recordSkillUsage_noopForUnknownSkill() {
        // should not throw
        assertDoesNotThrow(() -> skillManager.recordSkillUsage("unknown", "proj-1"));
    }

    // ===== loadProjectSkills =====

    @Test
    void loadProjectSkills_loadsFromDirectory() throws IOException {
        Path skillsDir = tempDir.resolve("project-skills");
        Files.createDirectories(skillsDir);

        String content = "---\nname: MySkill\ndescription: Test skill\ntrigger: mytrigger\n---\n\nPrompt content here";
        Files.writeString(skillsDir.resolve("my-skill.md"), content);

        skillManager.loadProjectSkills("proj-1", skillsDir.toString());

        Map<String, Skill> skills = skillManager.getProjectSkills("proj-1");
        assertEquals(1, skills.size());

        Skill loaded = skills.get("my-skill");
        assertNotNull(loaded);
        assertEquals("MySkill", loaded.getName());
        assertEquals("Test skill", loaded.getDescription());
        assertEquals("mytrigger", loaded.getTriggerPattern());
        assertEquals("Prompt content here", loaded.getPrompt());
        assertEquals("project", loaded.getCategory());
    }

    @Test
    void loadProjectSkills_skipsInvalidFiles() throws IOException {
        Path skillsDir = tempDir.resolve("bad-skills");
        Files.createDirectories(skillsDir);
        Files.writeString(skillsDir.resolve("bad.md"), "no metadata delimiters");

        skillManager.loadProjectSkills("proj-1", skillsDir.toString());

        Map<String, Skill> skills = skillManager.getProjectSkills("proj-1");
        assertTrue(skills.isEmpty());
    }

    @Test
    void loadProjectSkills_noopWhenDirDoesNotExist() {
        skillManager.loadProjectSkills("proj-1", "/nonexistent/dir");
        Map<String, Skill> skills = skillManager.getProjectSkills("proj-1");
        assertNotNull(skills);
        assertTrue(skills.isEmpty());
    }

    // ===== loadCustomGlobalSkills =====

    @Test
    void loadCustomGlobalSkills_loadsFromDataDir() throws IOException {
        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);

        String content = "---\nname: CustomSkill\ndescription: Custom\ncustom trigger\n---\n\nCustom prompt";
        Files.writeString(skillsDir.resolve("custom.md"), content);

        skillManager.loadCustomGlobalSkills();

        Skill loaded = skillManager.getGlobalSkill("custom");
        assertNotNull(loaded);
        assertEquals("CustomSkill", loaded.getName());
        assertEquals("custom", loaded.getCategory());
    }

    // ===== saveGlobalSkillToFile =====

    @Test
    void saveGlobalSkillToFile_createsFile() throws IOException {
        Skill skill = Skill.builder()
                .id("file-skill")
                .name("FileSkill")
                .description("Saved skill")
                .triggerPattern("file")
                .prompt("File prompt")
                .examples(List.of("ex1", "ex2"))
                .build();

        skillManager.saveGlobalSkillToFile(skill);

        Path savedFile = tempDir.resolve("skills").resolve("file-skill.md");
        assertTrue(Files.exists(savedFile));

        String content = Files.readString(savedFile);
        assertTrue(content.contains("name: FileSkill"));
        assertTrue(content.contains("description: Saved skill"));
        assertTrue(content.contains("trigger: file"));
        assertTrue(content.contains("File prompt"));
    }

    // ===== deleteGlobalSkillFile =====

    @Test
    void deleteGlobalSkillFile_removesExistingFile() throws IOException {
        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);
        Files.writeString(skillsDir.resolve("to-delete.md"), "content");

        skillManager.deleteGlobalSkillFile("to-delete");

        assertFalse(Files.exists(skillsDir.resolve("to-delete.md")));
    }

    @Test
    void deleteGlobalSkillFile_noopWhenFileDoesNotExist() {
        // should not throw
        assertDoesNotThrow(() -> skillManager.deleteGlobalSkillFile("nonexistent"));
    }
}
