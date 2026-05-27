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
import java.util.*;
import java.util.stream.Collectors;

@Component
public class SkillManager {

    private static final Logger log = LoggerFactory.getLogger(SkillManager.class);

    private final AppConfig appConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Skill> skills = new LinkedHashMap<>();

    public SkillManager(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @PostConstruct
    public void init() {
        loadBuiltinSkills();
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
            log.info("Loaded {} builtin skills", skills.size());
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

            skills.put(skill.getId(), skill);
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

    // ===== SKILL 操作 =====

    public void registerSkill(Skill skill) {
        skills.put(skill.getId(), skill);
        log.info("Skill registered: {}", skill.getId());
    }

    public Skill getSkill(String skillId) {
        return skills.get(skillId);
    }

    public List<Skill> getAllSkills() {
        return new ArrayList<>(skills.values());
    }

    public List<Skill> getSkillsByCategory(String category) {
        return skills.values().stream()
            .filter(s -> category.equals(s.getCategory()))
            .collect(Collectors.toList());
    }

    public List<Skill> matchSkills(String taskDescription) {
        String lowerTask = taskDescription.toLowerCase();
        return skills.values().stream()
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

    public void recordSkillUsage(String skillId) {
        Skill skill = skills.get(skillId);
        if (skill != null) {
            skill.recordUsage();
        }
    }

    public void saveLearnedSkill(Skill skill, String agentId) {
        skill.setCategory("learned");
        registerSkill(skill);

        MemoryManager memoryManager = new MemoryManager(appConfig);
        String skillContent = String.format("---\nname: %s\ndescription: %s\ntrigger: %s\n---\n\n%s",
            skill.getName(), skill.getDescription(), skill.getTriggerPattern(), skill.getPrompt());
        memoryManager.saveMemory(agentId, "skills", skill.getId(), skillContent);
        log.info("Learned skill saved: {} for agent: {}", skill.getId(), agentId);
    }
}
