package com.chengxun.gamemaker.model;

import java.time.LocalDateTime;
import java.util.*;

public class Skill {
    private String id;
    private String name;
    private String description;
    private String category;       // "builtin" | "learned" | "custom"
    private String triggerPattern; // keywords for matching
    private String prompt;         // prompt template
    private List<String> examples = new ArrayList<>();
    private int usageCount;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;

    public Skill() {
        this.createdAt = LocalDateTime.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Skill skill = new Skill();

        public Builder id(String id) { skill.id = id; return this; }
        public Builder name(String name) { skill.name = name; return this; }
        public Builder description(String description) { skill.description = description; return this; }
        public Builder category(String category) { skill.category = category; return this; }
        public Builder triggerPattern(String triggerPattern) { skill.triggerPattern = triggerPattern; return this; }
        public Builder prompt(String prompt) { skill.prompt = prompt; return this; }
        public Builder examples(List<String> examples) { skill.examples = examples; return this; }
        public Skill build() { return skill; }
    }

    public void recordUsage() {
        this.usageCount++;
        this.lastUsedAt = LocalDateTime.now();
    }

    public String toPromptSection() {
        StringBuilder sb = new StringBuilder();
        sb.append("### SKILL: ").append(name).append("\n");
        sb.append("**").append(description).append("**\n\n");
        sb.append(prompt).append("\n");
        if (!examples.isEmpty()) {
            sb.append("\n**示例：**\n");
            for (String example : examples) {
                sb.append("- ").append(example).append("\n");
            }
        }
        return sb.toString();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTriggerPattern() { return triggerPattern; }
    public void setTriggerPattern(String triggerPattern) { this.triggerPattern = triggerPattern; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public List<String> getExamples() { return examples; }
    public void setExamples(List<String> examples) { this.examples = examples; }

    public int getUsageCount() { return usageCount; }
    public void setUsageCount(int usageCount) { this.usageCount = usageCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
}
