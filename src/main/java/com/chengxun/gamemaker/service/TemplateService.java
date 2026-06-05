package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.config.AppConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模板管理服务
 * 负责游戏项目模板的加载、管理和应用
 *
 * 主要功能：
 * - 加载内置模板（classpath）
 * - 加载用户自定义模板（配置目录）
 * - 获取模板列表
 * - 应用模板到新项目
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class TemplateService {

    private static final Logger log = LoggerFactory.getLogger(TemplateService.class);

    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;

    /** 内置模板缓存 */
    private final Map<String, Map<String, Object>> builtinTemplates = new LinkedHashMap<>();

    /** 用户自定义模板缓存 */
    private final Map<String, Map<String, Object>> customTemplates = new LinkedHashMap<>();

    public TemplateService(AppConfig appConfig, ObjectMapper objectMapper) {
        this.appConfig = appConfig;
        this.objectMapper = objectMapper;
        init();
    }

    /**
     * 初始化，加载所有模板
     */
    private void init() {
        loadBuiltinTemplates();
        loadCustomTemplates();
        log.info("Templates loaded: {} builtin, {} custom", builtinTemplates.size(), customTemplates.size());
    }

    /**
     * 加载内置模板
     * 从 classpath:templates/ 目录加载
     */
    private void loadBuiltinTemplates() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:templates/*/README.md");

            for (Resource resource : resources) {
                try {
                    String templateDir = resource.getURL().getPath();
                    // 提取模板目录名
                    String templateId = extractTemplateId(templateDir);
                    if (templateId != null) {
                        Map<String, Object> templateInfo = loadTemplateFromResource(templateId, resource);
                        if (templateInfo != null) {
                            builtinTemplates.put(templateId, templateInfo);
                            log.debug("Loaded builtin template: {}", templateId);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to load builtin template: {}", resource.getFilename(), e);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to scan builtin templates", e);
        }
    }

    /**
     * 从资源加载模板信息
     */
    private Map<String, Object> loadTemplateFromResource(String templateId, Resource readmeResource) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("id", templateId);
        template.put("source", "builtin");

        try {
            // 读取 README 获取模板描述
            String readmeContent = new String(readmeResource.getInputStream().readAllBytes());
            template.put("readme", readmeContent);

            // 提取模板名称和描述
            String[] lines = readmeContent.split("\n");
            for (String line : lines) {
                if (line.startsWith("# ")) {
                    template.put("name", line.substring(2).trim());
                }
                if (line.startsWith("## ")) {
                    template.put("description", line.substring(3).trim());
                    break;
                }
            }

            // 检查是否有配置文件
            String basePath = "classpath:templates/" + templateId + "/";
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

            // 检查 plant-components.json
            if (resolver.getResource(basePath + "plant-components.json").exists()) {
                template.put("hasPlantComponents", true);
            }

            // 检查 zombie-templates.json
            if (resolver.getResource(basePath + "zombie-templates.json").exists()) {
                template.put("hasZombieTemplates", true);
            }

            // 检查 diy-system-config.json
            if (resolver.getResource(basePath + "diy-system-config.json").exists()) {
                template.put("hasDiyConfig", true);
            }

            return template;
        } catch (IOException e) {
            log.warn("Failed to read template: {}", templateId, e);
            return null;
        }
    }

    /**
     * 加载用户自定义模板
     * 从配置的 templatesDir 目录加载
     */
    private void loadCustomTemplates() {
        Path templatesPath = Path.of(appConfig.getTemplatesDir());
        if (!Files.exists(templatesPath)) {
            try {
                Files.createDirectories(templatesPath);
                log.info("Created templates directory: {}", templatesPath);
            } catch (IOException e) {
                log.error("Failed to create templates directory: {}", templatesPath, e);
                return;
            }
        }

        try {
            Files.list(templatesPath)
                .filter(Files::isDirectory)
                .forEach(templateDir -> {
                    try {
                        String templateId = templateDir.getFileName().toString();
                        Map<String, Object> templateInfo = loadTemplateFromDirectory(templateId, templateDir);
                        if (templateInfo != null) {
                            customTemplates.put(templateId, templateInfo);
                            log.debug("Loaded custom template: {}", templateId);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to load custom template: {}", templateDir, e);
                    }
                });
        } catch (IOException e) {
            log.error("Failed to scan custom templates", e);
        }
    }

    /**
     * 从目录加载模板信息
     */
    private Map<String, Object> loadTemplateFromDirectory(String templateId, Path templateDir) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("id", templateId);
        template.put("source", "custom");
        template.put("path", templateDir.toString());

        // 读取 README
        Path readmePath = templateDir.resolve("README.md");
        if (Files.exists(readmePath)) {
            try {
                String readmeContent = Files.readString(readmePath);
                template.put("readme", readmeContent);

                // 提取模板名称和描述
                String[] lines = readmeContent.split("\n");
                for (String line : lines) {
                    if (line.startsWith("# ")) {
                        template.put("name", line.substring(2).trim());
                    }
                    if (line.startsWith("## ")) {
                        template.put("description", line.substring(3).trim());
                        break;
                    }
                }
            } catch (IOException e) {
                log.warn("Failed to read README: {}", readmePath, e);
            }
        }

        // 读取 template.json 配置文件（如果有）
        Path configPath = templateDir.resolve("template.json");
        if (Files.exists(configPath)) {
            try {
                Map<String, Object> config = objectMapper.readValue(configPath.toFile(), Map.class);
                template.putAll(config);
            } catch (IOException e) {
                log.warn("Failed to read template config: {}", configPath, e);
            }
        }

        // 检查配置文件
        template.put("hasPlantComponents", Files.exists(templateDir.resolve("plant-components.json")));
        template.put("hasZombieTemplates", Files.exists(templateDir.resolve("zombie-templates.json")));
        template.put("hasDiyConfig", Files.exists(templateDir.resolve("diy-system-config.json")));
        template.put("hasDirectoryStructure", Files.exists(templateDir.resolve("directory-structure.json")));

        return template;
    }

    /**
     * 获取所有模板列表
     *
     * @return 模板列表
     */
    public List<Map<String, Object>> getAllTemplates() {
        List<Map<String, Object>> templates = new ArrayList<>();

        // 添加内置模板
        for (Map.Entry<String, Map<String, Object>> entry : builtinTemplates.entrySet()) {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("id", entry.getKey());
            summary.put("name", entry.getValue().getOrDefault("name", entry.getKey()));
            summary.put("description", entry.getValue().getOrDefault("description", ""));
            summary.put("source", "builtin");
            summary.put("hasPlantComponents", entry.getValue().getOrDefault("hasPlantComponents", false));
            summary.put("hasZombieTemplates", entry.getValue().getOrDefault("hasZombieTemplates", false));
            summary.put("hasDiyConfig", entry.getValue().getOrDefault("hasDiyConfig", false));
            templates.add(summary);
        }

        // 添加自定义模板
        for (Map.Entry<String, Map<String, Object>> entry : customTemplates.entrySet()) {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("id", entry.getKey());
            summary.put("name", entry.getValue().getOrDefault("name", entry.getKey()));
            summary.put("description", entry.getValue().getOrDefault("description", ""));
            summary.put("source", "custom");
            summary.put("hasPlantComponents", entry.getValue().getOrDefault("hasPlantComponents", false));
            summary.put("hasZombieTemplates", entry.getValue().getOrDefault("hasZombieTemplates", false));
            summary.put("hasDiyConfig", entry.getValue().getOrDefault("hasDiyConfig", false));
            templates.add(summary);
        }

        return templates;
    }

    /**
     * 获取模板详情
     *
     * @param templateId 模板ID
     * @return 模板详情
     */
    public Map<String, Object> getTemplate(String templateId) {
        // 先查找内置模板
        if (builtinTemplates.containsKey(templateId)) {
            return builtinTemplates.get(templateId);
        }

        // 再查找自定义模板
        if (customTemplates.containsKey(templateId)) {
            return customTemplates.get(templateId);
        }

        return null;
    }

    /**
     * 应用模板到项目目录
     *
     * @param templateId 模板ID
     * @param projectDir 项目目录
     * @return 是否成功
     */
    public boolean applyTemplate(String templateId, Path projectDir) {
        Map<String, Object> template = getTemplate(templateId);
        if (template == null) {
            log.error("Template not found: {}", templateId);
            return false;
        }

        String source = (String) template.get("source");
        try {
            if ("builtin".equals(source)) {
                return applyBuiltinTemplate(templateId, projectDir);
            } else if ("custom".equals(source)) {
                return applyCustomTemplate(templateId, projectDir);
            }
        } catch (Exception e) {
            log.error("Failed to apply template: {} to {}", templateId, projectDir, e);
        }

        return false;
    }

    /**
     * 应用内置模板
     */
    private boolean applyBuiltinTemplate(String templateId, Path projectDir) throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        String basePath = "classpath:templates/" + templateId + "/";

        // 复制游戏配置文件到 config/ 目录
        String[] configFiles = {
            "plant-components.json",
            "zombie-templates.json",
            "diy-system-config.json",
            "README.md"
        };

        for (String configFile : configFiles) {
            Resource resource = resolver.getResource(basePath + configFile);
            if (resource.exists()) {
                Path targetPath = projectDir.resolve("config").resolve(configFile);
                Files.createDirectories(targetPath.getParent());
                try (InputStream is = resource.getInputStream()) {
                    Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    log.debug("Copied template config file: {}", configFile);
                }
            }
        }

        // 复制模板记忆文件到 .game-maker/memory/ 目录（如果有）
        copyTemplateMemoryFiles(basePath, projectDir, resolver);

        // 复制模板技能文件到 .game-maker/skills/ 目录（如果有）
        copyTemplateSkillFiles(basePath, projectDir, resolver);

        // 创建模板指定的目录结构
        createTemplateDirectories(templateId, projectDir);

        // 创建 .game-maker 基础目录
        Files.createDirectories(projectDir.resolve(".game-maker"));
        Files.createDirectories(projectDir.resolve(".game-maker/memory"));
        Files.createDirectories(projectDir.resolve(".game-maker/skills"));
        Files.createDirectories(projectDir.resolve(".game-maker/contexts"));

        log.info("Applied builtin template: {} to {}", templateId, projectDir);
        return true;
    }

    /**
     * 复制模板记忆文件
     */
    private void copyTemplateMemoryFiles(String basePath, Path projectDir,
                                           PathMatchingResourcePatternResolver resolver) throws IOException {
        // 检查模板中是否有 memory 目录
        Resource memoryDir = resolver.getResource(basePath + "memory/");
        if (memoryDir.exists()) {
            Path targetMemoryDir = projectDir.resolve(".game-maker/memory");
            Files.createDirectories(targetMemoryDir);

            // 复制所有记忆文件
            Resource[] memoryFiles = resolver.getResources(basePath + "memory/**/*");
            for (Resource file : memoryFiles) {
                if (file.isReadable() && !file.getFilename().isEmpty()) {
                    String relativePath = file.getURL().getPath()
                        .substring(file.getURL().getPath().indexOf("memory/") + 7);
                    Path targetPath = targetMemoryDir.resolve(relativePath);
                    Files.createDirectories(targetPath.getParent());
                    try (InputStream is = file.getInputStream()) {
                        Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        log.debug("Copied template memory file: {}", relativePath);
                    }
                }
            }
        }

        // 检查模板中是否有 agents-memory 目录（按 Agent 分类的记忆）
        Resource agentsMemoryDir = resolver.getResource(basePath + "agents-memory/");
        if (agentsMemoryDir.exists()) {
            Path targetMemoryDir = projectDir.resolve(".game-maker/memory");
            Files.createDirectories(targetMemoryDir);

            Resource[] agentDirs = resolver.getResources(basePath + "agents-memory/*");
            for (Resource agentDir : agentDirs) {
                if (agentDir.exists()) {
                    String agentId = agentDir.getFilename();
                    Path agentTargetDir = targetMemoryDir.resolve(agentId);
                    Files.createDirectories(agentTargetDir);

                    Resource[] categoryDirs = resolver.getResources(basePath + "agents-memory/" + agentId + "/*");
                    for (Resource categoryDir : categoryDirs) {
                        if (categoryDir.exists()) {
                            String category = categoryDir.getFilename();
                            Path categoryTargetDir = agentTargetDir.resolve(category);
                            Files.createDirectories(categoryTargetDir);

                            Resource[] files = resolver.getResources(
                                basePath + "agents-memory/" + agentId + "/" + category + "/*");
                            for (Resource file : files) {
                                if (file.isReadable() && !file.getFilename().isEmpty()) {
                                    Path targetPath = categoryTargetDir.resolve(file.getFilename());
                                    try (InputStream is = file.getInputStream()) {
                                        Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                                        log.debug("Copied template memory: {}/{}/{}", agentId, category, file.getFilename());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 复制模板技能文件
     */
    private void copyTemplateSkillFiles(String basePath, Path projectDir,
                                          PathMatchingResourcePatternResolver resolver) throws IOException {
        // 检查模板中是否有 skills 目录
        Resource skillsDir = resolver.getResource(basePath + "skills/");
        if (skillsDir.exists()) {
            Path targetSkillsDir = projectDir.resolve(".game-maker/skills");
            Files.createDirectories(targetSkillsDir);

            Resource[] skillFiles = resolver.getResources(basePath + "skills/**/*");
            for (Resource file : skillFiles) {
                if (file.isReadable() && !file.getFilename().isEmpty()) {
                    String relativePath = file.getURL().getPath()
                        .substring(file.getURL().getPath().indexOf("skills/") + 7);
                    Path targetPath = targetSkillsDir.resolve(relativePath);
                    Files.createDirectories(targetPath.getParent());
                    try (InputStream is = file.getInputStream()) {
                        Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        log.debug("Copied template skill file: {}", relativePath);
                    }
                }
            }
        }
    }

    /**
     * 应用自定义模板
     */
    private boolean applyCustomTemplate(String templateId, Path projectDir) throws IOException {
        Map<String, Object> template = customTemplates.get(templateId);
        String templatePath = (String) template.get("path");
        Path sourceDir = Path.of(templatePath);

        if (!Files.exists(sourceDir)) {
            log.error("Custom template directory not found: {}", templatePath);
            return false;
        }

        // 复制游戏配置文件到 config/ 目录
        String[] configFiles = {
            "plant-components.json",
            "zombie-templates.json",
            "diy-system-config.json",
            "README.md",
            "template.json"
        };

        for (String configFile : configFiles) {
            Path sourceFile = sourceDir.resolve(configFile);
            if (Files.exists(sourceFile)) {
                Path targetPath = projectDir.resolve("config").resolve(configFile);
                Files.createDirectories(targetPath.getParent());
                Files.copy(sourceFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
                log.debug("Copied template config file: {}", configFile);
            }
        }

        // 复制记忆文件到 .game-maker/memory/ 目录
        Path memoryDir = sourceDir.resolve("memory");
        if (Files.exists(memoryDir)) {
            Path targetMemoryDir = projectDir.resolve(".game-maker/memory");
            copyDirectory(memoryDir, targetMemoryDir);
            log.debug("Copied template memory directory");
        }

        // 复制技能文件到 .game-maker/skills/ 目录
        Path skillsDir = sourceDir.resolve("skills");
        if (Files.exists(skillsDir)) {
            Path targetSkillsDir = projectDir.resolve(".game-maker/skills");
            copyDirectory(skillsDir, targetSkillsDir);
            log.debug("Copied template skills directory");
        }

        // 复制上下文文件到 .game-maker/contexts/ 目录
        Path contextsDir = sourceDir.resolve("contexts");
        if (Files.exists(contextsDir)) {
            Path targetContextsDir = projectDir.resolve(".game-maker/contexts");
            copyDirectory(contextsDir, targetContextsDir);
            log.debug("Copied template contexts directory");
        }

        // 检查是否有目录结构定义
        Path structureFile = sourceDir.resolve("directory-structure.json");
        if (Files.exists(structureFile)) {
            Map<String, List<String>> structure = objectMapper.readValue(structureFile.toFile(), Map.class);
            createDirectoriesFromStructure(structure, projectDir);
        } else {
            // 使用默认目录结构
            createTemplateDirectories(templateId, projectDir);
        }

        // 创建 .game-maker 基础目录（如果不存在）
        Files.createDirectories(projectDir.resolve(".game-maker"));
        Files.createDirectories(projectDir.resolve(".game-maker/memory"));
        Files.createDirectories(projectDir.resolve(".game-maker/skills"));
        Files.createDirectories(projectDir.resolve(".game-maker/contexts"));

        log.info("Applied custom template: {} to {}", templateId, projectDir);
        return true;
    }

    /**
     * 递归复制目录
     */
    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source)
            .forEach(sourcePath -> {
                try {
                    Path targetPath = target.resolve(source.relativize(sourcePath));
                    if (Files.isDirectory(sourcePath)) {
                        Files.createDirectories(targetPath);
                    } else {
                        Files.createDirectories(targetPath.getParent());
                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    log.error("Failed to copy: {} to {}", sourcePath, target, e);
                }
            });
    }

    /**
     * 创建模板指定的目录结构
     */
    private void createTemplateDirectories(String templateId, Path projectDir) throws IOException {
        GameProjectTemplateService.GameType gameType = parseGameType(templateId);
        if (gameType == null) {
            // 使用默认目录结构
            createDefaultDirectories(projectDir);
            return;
        }

        GameProjectTemplateService templateService = new GameProjectTemplateService();
        Map<String, List<String>> structure = templateService.getProjectDirectoryStructure(gameType);

        createDirectoriesFromStructure(structure, projectDir);
    }

    /**
     * 从结构定义创建目录
     */
    private void createDirectoriesFromStructure(Map<String, List<String>> structure, Path projectDir) throws IOException {
        for (Map.Entry<String, List<String>> entry : structure.entrySet()) {
            String parentDir = entry.getKey();
            for (String subDir : entry.getValue()) {
                Path dirPath = projectDir.resolve(parentDir).resolve(subDir.replace("/", ""));
                Files.createDirectories(dirPath);
            }
        }
    }

    /**
     * 创建默认目录结构
     */
    private void createDefaultDirectories(Path projectDir) throws IOException {
        String[] dirs = {
            "design/docs",
            "design/specs",
            "server/src",
            "server/config",
            "client/src",
            "client/assets",
            "client/scripts",
            "shared/proto",
            "tools/build"
        };

        for (String dir : dirs) {
            Files.createDirectories(projectDir.resolve(dir));
        }
    }

    /**
     * 解析游戏类型
     */
    private GameProjectTemplateService.GameType parseGameType(String templateId) {
        try {
            return GameProjectTemplateService.GameType.valueOf(templateId.toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 刷新模板缓存
     */
    public void refreshTemplates() {
        builtinTemplates.clear();
        customTemplates.clear();
        loadBuiltinTemplates();
        loadCustomTemplates();
        log.info("Templates refreshed: {} builtin, {} custom", builtinTemplates.size(), customTemplates.size());
    }

    /**
     * 从模板创建自定义模板
     *
     * @param templateId 基础模板ID
     * @param newTemplateId 新模板ID
     * @param name 新模板名称
     * @param description 新模板描述
     * @return 是否成功
     */
    public boolean createCustomTemplate(String templateId, String newTemplateId, String name, String description) {
        Map<String, Object> baseTemplate = getTemplate(templateId);
        if (baseTemplate == null) {
            log.error("Base template not found: {}", templateId);
            return false;
        }

        Path newTemplateDir = Path.of(appConfig.getTemplatesDir(), newTemplateId);
        try {
            Files.createDirectories(newTemplateDir);

            // 创建 template.json
            Map<String, Object> templateConfig = new LinkedHashMap<>();
            templateConfig.put("id", newTemplateId);
            templateConfig.put("name", name);
            templateConfig.put("description", description);
            templateConfig.put("baseTemplate", templateId);
            templateConfig.put("createdAt", System.currentTimeMillis());

            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(newTemplateDir.resolve("template.json").toFile(), templateConfig);

            // 创建 README
            String readme = "# " + name + "\n\n## " + description + "\n\nBased on template: " + templateId;
            Files.writeString(newTemplateDir.resolve("README.md"), readme);

            // 复制基础模板的配置文件
            if (baseTemplate.containsKey("path")) {
                Path sourceDir = Path.of((String) baseTemplate.get("path"));
                String[] files = {"plant-components.json", "zombie-templates.json", "diy-system-config.json"};
                for (String file : files) {
                    Path source = sourceDir.resolve(file);
                    if (Files.exists(source)) {
                        Files.copy(source, newTemplateDir.resolve(file), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }

            // 刷新模板列表
            refreshTemplates();

            log.info("Created custom template: {} based on {}", newTemplateId, templateId);
            return true;
        } catch (IOException e) {
            log.error("Failed to create custom template: {}", newTemplateId, e);
            return false;
        }
    }

    /**
     * 提取模板ID
     */
    private String extractTemplateId(String path) {
        // 从路径中提取模板目录名
        // 例如: /path/to/templates/plants-vs-zombies/README.md -> plants-vs-zombies
        String[] parts = path.replace("\\", "/").split("/");
        for (int i = parts.length - 2; i >= 0; i--) {
            if (parts[i].equals("templates")) {
                return parts[i + 1];
            }
        }
        return null;
    }
}
