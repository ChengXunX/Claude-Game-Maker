package com.chengxun.gamemaker.manager;

import com.chengxun.gamemaker.config.AppConfig;
import com.chengxun.gamemaker.config.CacheConfig;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.service.TemplateService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 项目管理器
 * 负责游戏项目的创建、加载、索引和管理
 *
 * 主要功能：
 * - 项目可以放在任意目录，通过索引文件统一管理
 * - 支持创建新项目（可选模板）
 * - 支持导入已有项目
 * - 支持从索引中移除项目（不删除实际文件）
 *
 * 索引文件位置：data/project-index.json
 *
 * @author chengxun
 * @since 1.0.0
 */
@Component
public class ProjectManager {

    private static final Logger log = LoggerFactory.getLogger(ProjectManager.class);

    /** 索引文件名 */
    private static final String INDEX_FILE = "project-index.json";

    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;
    private final TemplateService templateService;

    /** 项目缓存，key为项目ID */
    private final Map<String, GameProject> projects = new ConcurrentHashMap<>();

    /** 项目索引，key为项目ID，value为项目路径 */
    private Map<String, String> projectIndex = new ConcurrentHashMap<>();

    /** 项目级写锁，防止并发写同一项目的 project.json */
    private final Map<String, Object> projectLocks = new ConcurrentHashMap<>();

    /** 自注入用于解决 Spring AOP 代理问题，使内部方法调用的缓存注解生效 */
    private final ProjectManager self;

    public ProjectManager(AppConfig appConfig, ObjectMapper objectMapper,
                          TemplateService templateService, @Lazy ProjectManager self) {
        this.appConfig = appConfig;
        this.objectMapper = objectMapper;
        this.templateService = templateService;
        this.self = self;
        init();
    }

    /**
     * 初始化，加载项目索引
     */
    private void init() {
        loadIndex();
        loadAllProjects();
        log.info("ProjectManager initialized: {} projects loaded", projects.size());
    }

    // ===== 索引管理 =====

    /**
     * 获取索引文件路径
     */
    private Path getIndexFilePath() {
        return Path.of(appConfig.getDataDir(), INDEX_FILE);
    }

    /**
     * 加载项目索引
     */
    private void loadIndex() {
        Path indexPath = getIndexFilePath();
        if (!Files.exists(indexPath)) {
            log.info("Project index file not found, creating new one");
            projectIndex = new ConcurrentHashMap<>();
            saveIndex();
            return;
        }

        try {
            Map<String, String> loaded = objectMapper.readValue(indexPath.toFile(),
                new TypeReference<LinkedHashMap<String, String>>() {});
            projectIndex = new ConcurrentHashMap<>(loaded);
            log.info("Loaded project index: {} entries", projectIndex.size());
        } catch (IOException e) {
            log.error("Failed to load project index", e);
            projectIndex = new ConcurrentHashMap<>();
        }
    }

    /**
     * 保存项目索引
     */
    private synchronized void saveIndex() {
        Path indexPath = getIndexFilePath();
        try {
            Files.createDirectories(indexPath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(indexPath.toFile(), projectIndex);
            log.debug("Project index saved: {} entries", projectIndex.size());
        } catch (IOException e) {
            log.error("Failed to save project index", e);
        }
    }

    /**
     * 加载所有项目
     */
    private void loadAllProjects() {
        for (Map.Entry<String, String> entry : projectIndex.entrySet()) {
            String projectId = entry.getKey();
            String workDir = entry.getValue();

            try {
                GameProject project = loadProjectFromDir(projectId, workDir);
                if (project != null) {
                    projects.put(projectId, project);
                }
            } catch (Exception e) {
                log.warn("Failed to load project {} from {}", projectId, workDir, e);
            }
        }
    }

    /**
     * 从目录加载项目
     */
    private GameProject loadProjectFromDir(String projectId, String workDir) {
        Path configPath = Path.of(workDir, ".game-maker", "project.json");
        if (Files.exists(configPath)) {
            try {
                GameProject project = objectMapper.readValue(configPath.toFile(), GameProject.class);
                // 确保项目ID和路径与索引一致
                project.setId(projectId);
                project.setWorkDir(workDir);
                return project;
            } catch (IOException e) {
                log.error("Failed to load project config: {}", configPath, e);
            }
        }

        // 如果没有配置文件，创建一个基本的项目对象
        Path workPath = Path.of(workDir);
        return GameProject.builder()
            .id(projectId)
            .name(workPath.getFileName().toString())
            .description("Imported project")
            .workDir(workDir)
            .build();
    }

    // ===== 项目管理 =====

    /**
     * 创建项目（不使用模板）
     */
    public GameProject createProject(String name, String description, String workDir) {
        return createProject(name, description, workDir, null);
    }

    /**
     * 创建项目（支持模板）
     *
     * @param name 项目名称
     * @param description 项目描述
     * @param workDir 工作目录（可以是任意位置）
     * @param templateId 模板ID（可选）
     * @return 创建的项目
     */
    public GameProject createProject(String name, String description, String workDir, String templateId) {
        String projectId = "project-" + System.currentTimeMillis();

        // 规范化路径
        Path workPath = Path.of(workDir).toAbsolutePath().normalize();
        workDir = workPath.toString();

        GameProject project = GameProject.builder()
            .id(projectId)
            .name(name)
            .description(description)
            .workDir(workDir)
            .templateId(templateId)
            .build();

        // 创建项目配置目录
        initProjectDirectories(project);

        // 应用模板（如果有）
        if (templateId != null && !templateId.isEmpty()) {
            boolean applied = templateService.applyTemplate(templateId, workPath);
            if (applied) {
                log.info("Template {} applied to project {}", templateId, projectId);
            } else {
                log.warn("Failed to apply template {} to project {}", templateId, projectId);
            }
        }

        // 保存项目配置
        saveProjectConfig(project);

        // 添加到索引
        projectIndex.put(projectId, workDir);
        saveIndex();

        projects.put(projectId, project);
        log.info("Project created: {} ({}) at {}", name, projectId, workDir);
        return project;
    }

    /**
     * 导入已有项目
     *
     * @param workDir 项目目录
     * @return 导入的项目，如果已存在则返回现有项目
     */
    public GameProject importProject(String workDir) {
        // 规范化路径
        Path workPath = Path.of(workDir).toAbsolutePath().normalize();
        workDir = workPath.toString();

        // 检查是否已在索引中
        for (Map.Entry<String, String> entry : projectIndex.entrySet()) {
            if (workDir.equals(entry.getValue())) {
                log.info("Project already indexed: {} at {}", entry.getKey(), workDir);
                return projects.get(entry.getKey());
            }
        }

        // 检查目录是否存在
        if (!Files.exists(workPath) || !Files.isDirectory(workPath)) {
            log.error("Directory does not exist: {}", workDir);
            return null;
        }

        // 尝试加载现有配置
        String projectId = null;
        Path configPath = workPath.resolve(".game-maker/project.json");
        if (Files.exists(configPath)) {
            try {
                GameProject existing = objectMapper.readValue(configPath.toFile(), GameProject.class);
                projectId = existing.getId();
            } catch (IOException e) {
                log.warn("Failed to read existing project config", e);
            }
        }

        // 如果没有现有配置，生成新的项目ID
        if (projectId == null) {
            projectId = "project-" + System.currentTimeMillis();
        }

        // 创建或加载项目
        GameProject project;
        if (Files.exists(configPath)) {
            try {
                project = objectMapper.readValue(configPath.toFile(), GameProject.class);
            } catch (IOException e) {
                log.error("Failed to load project config", e);
                return null;
            }
        } else {
            Path workPathName = Path.of(workDir);
            project = GameProject.builder()
                .id(projectId)
                .name(workPathName.getFileName().toString())
                .description("Imported project")
                .workDir(workDir)
                .build();
        }

        // 确保项目配置目录存在
        initProjectDirectories(project);

        // 保存项目配置
        saveProjectConfig(project);

        // 添加到索引
        projectIndex.put(projectId, workDir);
        saveIndex();

        projects.put(projectId, project);
        log.info("Project imported: {} ({}) from {}", project.getName(), projectId, workDir);
        return project;
    }

    /**
     * 从索引中移除项目（不删除实际文件）
     *
     * @param projectId 项目ID
     * @return 是否成功移除
     */
    public boolean removeFromIndex(String projectId) {
        if (!projectIndex.containsKey(projectId)) {
            return false;
        }

        projectIndex.remove(projectId);
        projects.remove(projectId);
        saveIndex();

        log.info("Project removed from index: {}", projectId);
        return true;
    }

    /**
     * 刷新项目（重新加载配置）
     *
     * @param projectId 项目ID
     * @return 刷新后的项目
     */
    public GameProject refreshProject(String projectId) {
        String workDir = projectIndex.get(projectId);
        if (workDir == null) {
            log.warn("Project not found in index: {}", projectId);
            return null;
        }

        GameProject project = loadProjectFromDir(projectId, workDir);
        if (project != null) {
            projects.put(projectId, project);
            log.info("Project refreshed: {}", projectId);
        }
        return project;
    }

    /**
     * 检查目录是否已被索引
     *
     * @param workDir 项目目录
     * @return 是否已被索引
     */
    public boolean isDirectoryIndexed(String workDir) {
        Path workPath = Path.of(workDir).toAbsolutePath().normalize();
        String normalizedDir = workPath.toString();
        return projectIndex.containsValue(normalizedDir);
    }

    /**
     * 获取项目的配置目录
     * 优先使用项目自己的 .game-maker 目录，如果没有则使用系统目录
     */
    public String getProjectConfigDir(GameProject project) {
        // 项目自己的配置目录
        String projectConfigDir = project.getProjectConfigDir();
        if (projectConfigDir != null && Files.exists(Path.of(projectConfigDir))) {
            return projectConfigDir;
        }

        // 使用系统目录（向后兼容）
        return Path.of(appConfig.getDataDir(), "projects", project.getId()).toString();
    }

    /**
     * 获取项目技能目录
     */
    public String getProjectSkillsDir(String projectId) {
        GameProject project = projects.get(projectId);
        if (project == null) return null;
        return project.getSkillsDir();
    }

    /**
     * 获取项目记忆目录
     */
    public String getProjectMemoryDir(String projectId) {
        GameProject project = projects.get(projectId);
        if (project == null) return null;
        return project.getMemoryDir();
    }

    /**
     * 获取项目上下文目录
     */
    public String getProjectContextsDir(String projectId) {
        GameProject project = projects.get(projectId);
        if (project == null) return null;
        return project.getContextsDir();
    }

    /**
     * 加载项目
     */
    public GameProject loadProject(String workDir) {
        // 规范化路径
        Path workPath = Path.of(workDir).toAbsolutePath().normalize();
        String normalizedDir = workPath.toString();

        // 先从索引中查找
        for (Map.Entry<String, String> entry : projectIndex.entrySet()) {
            if (normalizedDir.equals(entry.getValue())) {
                return projects.get(entry.getKey());
            }
        }

        // 尝试从目录加载
        Path configPath = workPath.resolve(".game-maker/project.json");
        if (Files.exists(configPath)) {
            try {
                GameProject project = objectMapper.readValue(configPath.toFile(), GameProject.class);
                // 添加到索引
                projectIndex.put(project.getId(), normalizedDir);
                saveIndex();
                projects.put(project.getId(), project);
                return project;
            } catch (IOException e) {
                log.error("Failed to load project from: {}", workDir, e);
            }
        }

        return null;
    }

    /**
     * 获取或创建项目
     */
    public GameProject getOrCreateProject(String workDir) {
        // 规范化路径
        Path workPath = Path.of(workDir).toAbsolutePath().normalize();
        String normalizedDir = workPath.toString();

        // 查找已加载的项目
        for (GameProject project : projects.values()) {
            if (normalizedDir.equals(project.getWorkDir())) {
                return project;
            }
        }

        // 尝试加载现有项目
        GameProject project = loadProject(workDir);
        if (project != null) {
            return project;
        }

        // 创建新项目
        String projectName = workPath.getFileName().toString();
        return createProject(projectName, "Game project: " + projectName, workDir);
    }

    /**
     * 获取项目
     */
    @Cacheable(value = CacheConfig.CACHE_PROJECTS, key = "#projectId")
    public GameProject getProject(String projectId) {
        return projects.get(projectId);
    }

    /**
     * 获取所有项目
     */
    @Cacheable(value = CacheConfig.CACHE_PROJECTS, key = "'all'")
    public List<GameProject> getAllProjects() {
        return new ArrayList<>(projects.values());
    }

    /**
     * 保存项目配置
     */
    @CacheEvict(value = CacheConfig.CACHE_PROJECTS, allEntries = true)
    public void saveProjectConfig(GameProject project) {
        // 项目级锁，防止并发写同一项目的 project.json
        Object lock = projectLocks.computeIfAbsent(project.getId(), k -> new Object());
        synchronized (lock) {
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
    }

    /**
     * 初始化项目目录
     */
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

    // ===== 项目规则 =====

    @CacheEvict(value = CacheConfig.CACHE_PROJECTS, key = "#projectId + '-rules'")
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

    @Cacheable(value = CacheConfig.CACHE_PROJECTS, key = "#projectId + '-rules'")
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
