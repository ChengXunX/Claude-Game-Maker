package com.chengxun.gamemaker.manager;

import com.chengxun.gamemaker.config.AppConfig;
import com.chengxun.gamemaker.model.Skill;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class SkillManager {

    private static final Logger log = LoggerFactory.getLogger(SkillManager.class);

    private final AppConfig appConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Skill> globalSkills = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Skill>> projectSkills = new ConcurrentHashMap<>();

    public SkillManager(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @PostConstruct
    public void init() {
        loadBuiltinSkills();
        loadCustomGlobalSkills();
    }

    private void loadBuiltinSkills() {
        Path skillsDir = Path.of("src/main/resources/skills");
        if (!Files.exists(skillsDir)) {
            log.warn("Skills directory not found: {}", skillsDir);
            return;
        }

        try {
            Files.list(skillsDir)
                .filter(p -> p.toString().endsWith(".md"))
                .forEach(this::loadSkillFromFile);
            log.info("Loaded {} builtin skills", globalSkills.size());
        } catch (IOException e) {
            log.error("Failed to load builtin skills", e);
        }
    }

    private void loadSkillFromFile(Path path) {
        try {
            String content = Files.readString(path);
            String fileName = path.getFileName().toString().replace(".md", "");

            String[] parts = content.split("---", 3);
            if (parts.length < 3) {
                log.warn("Invalid skill file format: {}", path);
                return;
            }

            Map<String, String> metadata = parseMetadata(parts[1]);
            String prompt = parts[2].trim();

            Skill skill = Skill.builder()
                .id(fileName)
                .name(metadata.getOrDefault("name", fileName))
                .description(metadata.getOrDefault("description", ""))
                .category("builtin")
                .triggerPattern(metadata.getOrDefault("trigger", fileName))
                .prompt(prompt)
                .examples(parseExamples(metadata.getOrDefault("examples", "")))
                .build();

            globalSkills.put(skill.getId(), skill);
            log.debug("Loaded skill: {}", skill.getId());
        } catch (Exception e) {
            log.error("Failed to load skill from file: {}", path, e);
        }
    }

    private Map<String, String> parseMetadata(String metadataStr) {
        Map<String, String> metadata = new HashMap<>();
        for (String line : metadataStr.split("\n")) {
            line = line.trim();
            if (line.contains(":")) {
                String[] kv = line.split(":", 2);
                metadata.put(kv[0].trim(), kv[1].trim());
            }
        }
        return metadata;
    }

    private List<String> parseExamples(String examplesStr) {
        if (examplesStr == null || examplesStr.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(examplesStr.split("\\|"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }

    // ===== 全局 SKILL 操作 =====

    public void registerGlobalSkill(Skill skill) {
        globalSkills.put(skill.getId(), skill);
        log.info("Global skill registered: {}", skill.getId());
    }

    public Skill getGlobalSkill(String skillId) {
        return globalSkills.get(skillId);
    }

    public List<Skill> getAllGlobalSkills() {
        return new ArrayList<>(globalSkills.values());
    }

    public void removeGlobalSkill(String skillId) {
        globalSkills.remove(skillId);
        log.info("Global skill removed: {}", skillId);
    }

    public void saveGlobalSkillToFile(Skill skill) {
        String skillDir = appConfig.getDataDir() + "/skills";
        try {
            Path dir = Path.of(skillDir);
            Files.createDirectories(dir);

            String examplesStr = skill.getExamples() != null ? String.join(" | ", skill.getExamples()) : "";
            String category = skill.getCategory() != null ? skill.getCategory() : "custom";
            String content = String.format("---\nname: %s\ndescription: %s\ntrigger: %s\ncategory: %s\nexamples: %s\n---\n\n%s",
                skill.getName(), skill.getDescription(), skill.getTriggerPattern(), category, examplesStr, skill.getPrompt());

            Path skillPath = dir.resolve(skill.getId() + ".md");
            Files.writeString(skillPath, content);
            log.info("Global skill saved to file: {}", skillPath);
        } catch (IOException e) {
            log.error("Failed to save global skill to file: {}", skill.getId(), e);
        }
    }

    public void deleteGlobalSkillFile(String skillId) {
        String skillDir = appConfig.getDataDir() + "/skills";
        try {
            Path skillPath = Path.of(skillDir, skillId + ".md");
            if (Files.exists(skillPath)) {
                Files.delete(skillPath);
                log.info("Global skill file deleted: {}", skillPath);
            }
        } catch (IOException e) {
            log.error("Failed to delete global skill file: {}", skillId, e);
        }
    }

    public void loadCustomGlobalSkills() {
        String skillDir = appConfig.getDataDir() + "/skills";
        Path dir = Path.of(skillDir);
        if (!Files.exists(dir)) {
            return;
        }

        try {
            Files.list(dir)
                .filter(p -> p.toString().endsWith(".md"))
                .forEach(path -> {
                    Skill skill = loadSkillFromPath(path);
                    if (skill != null) {
                        // 从文件内容中读取原始 category，不强制覆盖
                        String content = readSkillFileContent(path);
                        String originalCategory = extractFrontmatterField(content, "category");
                        if (originalCategory != null && !originalCategory.isEmpty()) {
                            skill.setCategory(originalCategory);
                        } else {
                            skill.setCategory("custom");
                        }
                        globalSkills.put(skill.getId(), skill);
                    }
                });
            log.info("Loaded custom global skills from: {}", skillDir);
        } catch (IOException e) {
            log.error("Failed to load custom global skills", e);
        }
    }

    /**
     * 读取技能文件原始内容
     */
    private String readSkillFileContent(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * 从 YAML frontmatter 中提取字段
     */
    private String extractFrontmatterField(String content, String fieldName) {
        if (content == null) return null;
        String[] lines = content.split("\n");
        boolean inFrontmatter = false;
        for (String line : lines) {
            if (line.trim().equals("---")) {
                if (inFrontmatter) break;
                inFrontmatter = true;
                continue;
            }
            if (inFrontmatter && line.startsWith(fieldName + ":")) {
                return line.substring(fieldName.length() + 1).trim();
            }
        }
        return null;
    }

    // ===== 项目级别 SKILL 操作 =====

    public void loadProjectSkills(String projectId, String projectSkillsDir) {
        Path skillsDir = Path.of(projectSkillsDir);
        if (!Files.exists(skillsDir)) {
            log.debug("No project skills directory: {}", projectSkillsDir);
            return;
        }

        Map<String, Skill> skills = new LinkedHashMap<>();
        try {
            Files.list(skillsDir)
                .filter(p -> p.toString().endsWith(".md"))
                .forEach(path -> {
                    Skill skill = loadSkillFromPath(path);
                    if (skill != null) {
                        skill.setCategory("project");
                        skills.put(skill.getId(), skill);
                    }
                });
            projectSkills.put(projectId, skills);
            log.info("Loaded {} project skills for: {}", skills.size(), projectId);
        } catch (IOException e) {
            log.error("Failed to load project skills for: {}", projectId, e);
        }
    }

    private Skill loadSkillFromPath(Path path) {
        try {
            String content = Files.readString(path);
            String fileName = path.getFileName().toString().replace(".md", "");

            String[] parts = content.split("---", 3);
            if (parts.length < 3) {
                log.warn("Invalid skill file format: {}", path);
                return null;
            }

            Map<String, String> metadata = parseMetadata(parts[1]);
            String prompt = parts[2].trim();

            return Skill.builder()
                .id(fileName)
                .name(metadata.getOrDefault("name", fileName))
                .description(metadata.getOrDefault("description", ""))
                .triggerPattern(metadata.getOrDefault("trigger", fileName))
                .prompt(prompt)
                .examples(parseExamples(metadata.getOrDefault("examples", "")))
                .build();
        } catch (Exception e) {
            log.error("Failed to load skill from file: {}", path, e);
            return null;
        }
    }

    public void registerProjectSkill(String projectId, Skill skill) {
        projectSkills.computeIfAbsent(projectId, k -> new LinkedHashMap<>()).put(skill.getId(), skill);
        log.info("Project skill registered: {} for project: {}", skill.getId(), projectId);
    }

    public Map<String, Skill> getProjectSkills(String projectId) {
        return projectSkills.getOrDefault(projectId, new LinkedHashMap<>());
    }

    /**
     * 获取所有项目的技能映射
     *
     * @return 所有项目技能的映射，key为项目ID
     */
    public Map<String, Map<String, Skill>> getAllProjectSkills() {
        return projectSkills;
    }

    // ===== SKILL 匹配和查询 =====

    public List<Skill> matchSkills(String taskDescription, String projectId) {
        String lowerTask = taskDescription.toLowerCase();
        List<Skill> matched = new ArrayList<>();

        // 先匹配项目级别 SKILL
        Map<String, Skill> projSkills = projectSkills.get(projectId);
        if (projSkills != null) {
            matched.addAll(matchSkillsFromMap(lowerTask, projSkills));
        }

        // 再匹配全局 SKILL
        matched.addAll(matchSkillsFromMap(lowerTask, globalSkills));

        return matched;
    }

    private List<Skill> matchSkillsFromMap(String lowerTask, Map<String, Skill> skillsMap) {
        return skillsMap.values().stream()
            .filter(skill -> {
                String pattern = skill.getTriggerPattern().toLowerCase();
                String[] keywords = pattern.split("[,|]");
                for (String keyword : keywords) {
                    if (lowerTask.contains(keyword.trim())) {
                        return true;
                    }
                }
                return false;
            })
            .sorted((a, b) -> Integer.compare(b.getUsageCount(), a.getUsageCount()))
            .collect(Collectors.toList());
    }

    public List<Skill> getAllSkills(String projectId) {
        List<Skill> all = new ArrayList<>();

        // 项目级别 SKILL
        if (projectId != null && !projectId.isEmpty()) {
            Map<String, Skill> projSkills = projectSkills.get(projectId);
            if (projSkills != null) {
                all.addAll(projSkills.values());
            }
        }

        // 全局 SKILL
        all.addAll(globalSkills.values());

        return all;
    }

    public String buildSkillPrompt(List<Skill> matchedSkills) {
        if (matchedSkills.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("\n## 可用技能\n\n");
        sb.append("以下是与当前任务相关的技能，请参考使用：\n\n");

        for (Skill skill : matchedSkills) {
            sb.append(skill.toPromptSection()).append("\n");
        }

        return sb.toString();
    }

    public void recordSkillUsage(String skillId, String projectId) {
        // 先查找项目级别
        Map<String, Skill> projSkills = projectSkills.get(projectId);
        if (projSkills != null && projSkills.containsKey(skillId)) {
            projSkills.get(skillId).recordUsage();
            return;
        }

        // 再查找全局
        Skill skill = globalSkills.get(skillId);
        if (skill != null) {
            skill.recordUsage();
        }
    }

    /**
     * 将项目级技能提升为全局技能
     * 从项目技能中移除，注册为全局技能并保存到文件
     *
     * @param skillId 技能 ID
     * @param projectId 项目 ID
     * @return 提升的技能，如果不存在返回 null
     */
    public Skill promoteToGlobal(String skillId, String projectId) {
        Map<String, Skill> projSkills = projectSkills.get(projectId);
        if (projSkills == null) return null;

        Skill skill = projSkills.remove(skillId);
        if (skill == null) return null;

        skill.setCategory("global");
        globalSkills.put(skill.getId(), skill);
        saveGlobalSkillToFile(skill);

        log.info("Skill promoted from project to global: {} (project: {})", skillId, projectId);
        return skill;
    }

    public void saveLearnedSkill(Skill skill, String agentId, String projectId, String projectSkillsDir) {
        skill.setCategory("learned");
        registerProjectSkill(projectId, skill);

        String skillContent = String.format("---\nname: %s\ndescription: %s\ntrigger: %s\n---\n\n%s",
            skill.getName(), skill.getDescription(), skill.getTriggerPattern(), skill.getPrompt());

        try {
            Path skillPath = Path.of(projectSkillsDir, skill.getId() + ".md");
            Files.createDirectories(skillPath.getParent());
            Files.writeString(skillPath, skillContent);
            log.info("Learned skill saved: {} for project: {}", skill.getId(), projectId);
        } catch (IOException e) {
            log.error("Failed to save learned skill: {}", skill.getId(), e);
        }
    }
}
