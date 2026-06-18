package com.chengxun.gamemaker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SkillDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(SkillDiscoveryService.class);

    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile("^---\\s*\\n(.*?)\\n---\\s*\\n(.*)$", Pattern.DOTALL);
    private static final Pattern FIELD_PATTERN = Pattern.compile("^(\\w+):\\s*(.+)$");

    public List<DiscoveredSkill> discoverSkills(Path projectDir) {
        List<DiscoveredSkill> skills = new ArrayList<>();
        if (projectDir == null || !Files.exists(projectDir)) return skills;

        List<Path> searchPaths = List.of(
            projectDir.resolve(".game-maker").resolve("skills"),
            projectDir.resolve(".claude").resolve("skills"),
            projectDir.resolve(".agents").resolve("skills")
        );

        for (Path searchPath : searchPaths) {
            if (Files.exists(searchPath)) {
                try { discoverInDirectory(searchPath, skills); }
                catch (IOException e) { log.warn("Search skill dir failed: {}", searchPath, e); }
            }
        }

        log.info("Found {} skills in {}", skills.size(), projectDir);
        return skills;
    }

    private void discoverInDirectory(Path dir, List<DiscoveredSkill> skills) throws IOException {
        if (!Files.exists(dir) || !Files.isDirectory(dir)) return;
        Files.walk(dir, 3)
            .filter(p -> p.getFileName().toString().equals("SKILL.md"))
            .forEach(skillPath -> {
                try {
                    DiscoveredSkill skill = parseSkillFile(skillPath);
                    if (skill != null) skills.add(skill);
                } catch (Exception e) { log.warn("Parse skill file failed: {}", skillPath, e); }
            });
    }

    public DiscoveredSkill parseSkillFile(Path skillPath) {
        try {
            String content = Files.readString(skillPath);
            return parseSkillContent(content, skillPath);
        } catch (IOException e) {
            log.error("Read skill file failed: {}", skillPath, e);
            return null;
        }
    }

    private DiscoveredSkill parseSkillContent(String content, Path filePath) {
        Matcher matcher = FRONTMATTER_PATTERN.matcher(content);
        if (!matcher.find()) {
            log.warn("Skill file missing frontmatter: {}", filePath);
            return null;
        }

        String frontmatter = matcher.group(1);
        String body = matcher.group(2);

        String name = null;
        String description = null;
        boolean hidden = false;

        for (String line : frontmatter.split("\n")) {
            Matcher fieldMatcher = FIELD_PATTERN.matcher(line.trim());
            if (fieldMatcher.matches()) {
                String key = fieldMatcher.group(1);
                String value = fieldMatcher.group(2).trim();
                switch (key) {
                    case "name" -> name = value;
                    case "description" -> description = value;
                    case "hidden" -> hidden = Boolean.parseBoolean(value);
                }
            }
        }

        if (name == null || name.isEmpty()) {
            log.warn("Skill file missing name: {}", filePath);
            return null;
        }
        if (description == null) description = "";

        return new DiscoveredSkill(name, description, body.trim(), filePath.toString(), hidden);
    }

    public record DiscoveredSkill(String name, String description, String content, String filePath, boolean hidden) {}
}
