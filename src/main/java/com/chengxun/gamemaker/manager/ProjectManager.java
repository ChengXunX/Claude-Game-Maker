package com.chengxun.gamemaker.manager;

import com.chengxun.gamemaker.config.AppConfig;
import com.chengxun.gamemaker.model.GameProject;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ProjectManager {

    private static final Logger log = LoggerFactory.getLogger(ProjectManager.class);

    private final AppConfig appConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, GameProject> projects = new ConcurrentHashMap<>();

    public ProjectManager(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    // ===== 项目管理 =====

    public GameProject createProject(String name, String description, String workDir) {
        String projectId = "project-" + System.currentTimeMillis();
        GameProject project = GameProject.builder()
            .id(projectId)
            .name(name)
            .description(description)
            .workDir(workDir)
            .build();

        // 创建项目配置目录
        initProjectDirectories(project);

        // 保存项目配置
        saveProjectConfig(project);

        projects.put(projectId, project);
        log.info("Project created: {} ({})", name, projectId);
        return project;
    }

    public GameProject loadProject(String workDir) {
        Path configPath = Path.of(workDir, ".game-maker", "project.json");
        if (!Files.exists(configPath)) {
            return null;
        }

        try {
            GameProject project = objectMapper.readValue(configPath.toFile(), GameProject.class);
            projects.put(project.getId(), project);
            log.info("Project loaded: {} ({})", project.getName(), project.getId());
            return project;
        } catch (IOException e) {
            log.error("Failed to load project from: {}", workDir, e);
            return null;
        }
    }

    public GameProject getOrCreateProject(String workDir) {
        // 查找已加载的项目
        for (GameProject project : projects.values()) {
            if (workDir.equals(project.getWorkDir())) {
                return project;
            }
        }

        // 尝试加载现有项目
        GameProject project = loadProject(workDir);
        if (project != null) {
            return project;
        }

        // 创建新项目
        Path workPath = Path.of(workDir);
        String projectName = workPath.getFileName().toString();
        return createProject(projectName, "Game project: " + projectName, workDir);
    }

    public GameProject getProject(String projectId) {
        return projects.get(projectId);
    }

    public List<GameProject> getAllProjects() {
        return new ArrayList<>(projects.values());
    }

    public void saveProjectConfig(GameProject project) {
        Path configPath = Path.of(project.getProjectConfigFile());
        try {
            Files.createDirectories(configPath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(configPath.toFile(), project);
            log.debug("Project config saved: {}", project.getId());
        } catch (IOException e) {
            log.error("Failed to save project config: {}", project.getId(), e);
        }
    }

    // ===== 项目级别资源 =====

    public void initProjectDirectories(GameProject project) {
        try {
            Files.createDirectories(Path.of(project.getProjectConfigDir()));
            Files.createDirectories(Path.of(project.getSkillsDir()));
            Files.createDirectories(Path.of(project.getMemoryDir()));
            Files.createDirectories(Path.of(project.getContextsDir()));
            log.info("Project directories initialized: {}", project.getId());
        } catch (IOException e) {
            log.error("Failed to create project directories: {}", project.getId(), e);
        }
    }

    public String getProjectSkillsDir(String projectId) {
        GameProject project = projects.get(projectId);
        return project != null ? project.getSkillsDir() : null;
    }

    public String getProjectMemoryDir(String projectId) {
        GameProject project = projects.get(projectId);
        return project != null ? project.getMemoryDir() : null;
    }

    public String getProjectContextsDir(String projectId) {
        GameProject project = projects.get(projectId);
        return project != null ? project.getContextsDir() : null;
    }

    // ===== 项目规则 =====

    public void saveProjectRules(String projectId, String rules) {
        GameProject project = projects.get(projectId);
        if (project == null) return;

        Path rulesPath = Path.of(project.getProjectRulesFile());
        try {
            Files.createDirectories(rulesPath.getParent());
            Files.writeString(rulesPath, rules);
            log.info("Project rules saved: {}", projectId);
        } catch (IOException e) {
            log.error("Failed to save project rules: {}", projectId, e);
        }
    }

    public String loadProjectRules(String projectId) {
        GameProject project = projects.get(projectId);
        if (project == null) return null;

        Path rulesPath = Path.of(project.getProjectRulesFile());
        if (!Files.exists(rulesPath)) return null;

        try {
            return Files.readString(rulesPath);
        } catch (IOException e) {
            log.error("Failed to load project rules: {}", projectId, e);
            return null;
        }
    }

    // ===== 项目元数据 =====

    public void setProjectMetadata(String projectId, String key, String value) {
        GameProject project = projects.get(projectId);
        if (project == null) return;

        project.getMetadata().put(key, value);
        saveProjectConfig(project);
    }

    public String getProjectMetadata(String projectId, String key) {
        GameProject project = projects.get(projectId);
        return project != null ? project.getMetadata().get(key) : null;
    }
}
