package com.chengxun.gamemaker.model;

import java.time.LocalDateTime;
import java.util.*;

public class GameProject {
    private String id;
    private String name;
    private String description;
    private String workDir;              // 项目工作目录
    private String projectConfigDir;     // .game-maker 目录
    private ProjectStatus status;
    private List<String> agentIds = new ArrayList<>();
    private Map<String, String> metadata = new HashMap<>();
    private LocalDateTime createdAt;
    private LocalDateTime lastActiveAt;

    public enum ProjectStatus {
        CREATED, ACTIVE, PAUSED, COMPLETED, ARCHIVED
    }

    public GameProject() {
        this.createdAt = LocalDateTime.now();
        this.lastActiveAt = LocalDateTime.now();
        this.status = ProjectStatus.CREATED;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final GameProject project = new GameProject();

        public Builder id(String id) { project.id = id; return this; }
        public Builder name(String name) { project.name = name; return this; }
        public Builder description(String description) { project.description = description; return this; }
        public Builder workDir(String workDir) { project.workDir = workDir; return this; }
        public Builder status(ProjectStatus status) { project.status = status; return this; }
        public GameProject build() {
            if (project.workDir != null) {
                project.projectConfigDir = project.workDir + "/.game-maker";
            }
            return project;
        }
    }

    public void addAgent(String agentId) {
        if (!agentIds.contains(agentId)) {
            agentIds.add(agentId);
        }
    }

    public void removeAgent(String agentId) {
        agentIds.remove(agentId);
    }

    public void touch() {
        this.lastActiveAt = LocalDateTime.now();
    }

    public String getSkillsDir() {
        return projectConfigDir + "/skills";
    }

    public String getMemoryDir() {
        return projectConfigDir + "/memory";
    }

    public String getContextsDir() {
        return projectConfigDir + "/contexts";
    }

    public String getProjectConfigFile() {
        return projectConfigDir + "/project.json";
    }

    public String getProjectRulesFile() {
        return projectConfigDir + "/rules.md";
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getWorkDir() { return workDir; }
    public void setWorkDir(String workDir) { this.workDir = workDir; }

    public String getProjectConfigDir() { return projectConfigDir; }
    public void setProjectConfigDir(String projectConfigDir) { this.projectConfigDir = projectConfigDir; }

    public ProjectStatus getStatus() { return status; }
    public void setStatus(ProjectStatus status) { this.status = status; }

    public List<String> getAgentIds() { return agentIds; }
    public void setAgentIds(List<String> agentIds) { this.agentIds = agentIds; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(LocalDateTime lastActiveAt) { this.lastActiveAt = lastActiveAt; }
}
