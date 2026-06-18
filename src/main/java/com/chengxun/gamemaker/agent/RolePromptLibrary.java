package com.chengxun.gamemaker.agent;

import com.chengxun.gamemaker.web.entity.AgentPreset;
import com.chengxun.gamemaker.web.repository.AgentPresetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * 角色提示词库
 * 管理所有 Agent 角色的系统提示词模板，支持内置角色和自定义角色
 *
 * 数据来源优先级：
 * 1. 数据库（agent_presets 表）— 运行时动态添加/进化的角色
 * 2. 文件（classpath:agents/*.md）— 内置默认角色
 * 3. 通用兜底提示词 — 未找到角色时的默认值
 *
 * 设计理念：
 * - 角色行为完全由提示词驱动，而非继承
 * - 内置角色从文件加载作为默认值，运行时可被数据库版本覆盖
 * - 新增角色可通过文件（开发时）或数据库（运行时）两种方式
 * - 知识进化服务可自动优化角色提示词并持久化到数据库
 *
 * @author chengxun
 * @since 2.0.0
 */
@Component
public class RolePromptLibrary {

    private static final Logger log = LoggerFactory.getLogger(RolePromptLibrary.class);

    /** 角色文件目录（classpath 下） */
    private static final String AGENTS_DIR = "src/main/resources/agents";

    /** 内置角色提示词缓存（从文件加载的默认值） */
    private final Map<String, String> builtinPrompts = new ConcurrentHashMap<>();

    /** 数据库角色提示词缓存（运行时动态值，优先级高于 builtin） */
    private final Map<String, String> dbPrompts = new ConcurrentHashMap<>();

    /** 自定义角色提示词缓存（通过 API 注册的临时角色） */
    private final Map<String, String> customPrompts = new ConcurrentHashMap<>();

    /** 角色默认通知目标（完成任务后通知谁） */
    private final Map<String, Set<String>> defaultNotifyTargets = new ConcurrentHashMap<>();

    /** 角色默认审查者 */
    private final Map<String, String> defaultReviewers = new ConcurrentHashMap<>();

    /** 角色中文名称映射 */
    private final Map<String, String> roleNames = new ConcurrentHashMap<>();

    /** 数据库仓库（延迟注入，避免初始化顺序问题） */
    @Autowired
    @Lazy
    private AgentPresetRepository presetRepository;

    public RolePromptLibrary() {
        // 构造函数中只加载文件（数据库在 @PostConstruct 中加载）
        loadBuiltinRolesFromFiles();
    }

    /**
     * Spring 初始化完成后从数据库加载角色
     * 数据库版本会覆盖文件版本
     */
    @jakarta.annotation.PostConstruct
    public void initFromDatabase() {
        loadRolesFromDatabase();
    }

    /** 角色提示词最大长度（字符数），超出则截断 */
    private static final int MAX_PROMPT_LENGTH = 4000;

    /**
     * 获取角色的系统提示词
     * 优先级：自定义 > 数据库 > 文件 > 通用兜底
     * 输出超过 MAX_PROMPT_LENGTH 时自动截断
     *
     * @param role 角色标识
     * @return 系统提示词
     */
    public String getPrompt(String role) {
        // 1. 自定义角色（最高优先级）
        String prompt = customPrompts.get(role);
        if (prompt != null) return truncateIfNeeded(prompt);

        // 2. 数据库角色
        prompt = dbPrompts.get(role);
        if (prompt != null) return truncateIfNeeded(prompt);

        // 3. 文件内置角色
        prompt = builtinPrompts.get(role);
        if (prompt != null) return truncateIfNeeded(prompt);

        // 4. 通用兜底
        return truncateIfNeeded(buildGenericPrompt(role));
    }

    /**
     * 截断超长提示词
     * 保留前 MAX_PROMPT_LENGTH 字符，超出部分丢弃
     *
     * @param prompt 原始提示词
     * @return 截断后的提示词
     */
    private String truncateIfNeeded(String prompt) {
        if (prompt == null || prompt.length() <= MAX_PROMPT_LENGTH) {
            return prompt;
        }
        return prompt.substring(0, MAX_PROMPT_LENGTH) + "\n...(已截断)";
    }

    /**
     * 保存角色提示词到数据库
     * 用于知识进化、AI 自动生成、手动编辑等场景
     *
     * @param roleId   角色标识
     * @param prompt   系统提示词
     * @param name     角色中文名称
     * @param notifyTargets 通知目标（逗号分隔）
     * @param reviewer 审查者角色
     */
    public void saveToDatabase(String roleId, String prompt, String name, String notifyTargets, String reviewer) {
        saveToDatabase(roleId, prompt, name, notifyTargets, reviewer, "manual");
    }

    /**
     * 保存角色提示词到数据库（带进化来源）
     *
     * @param roleId         角色标识
     * @param prompt         系统提示词
     * @param name           角色中文名称
     * @param notifyTargets  通知目标（逗号分隔）
     * @param reviewer       审查者角色
     * @param evolutionSource 进化来源：manual / ai / evolution
     */
    public void saveToDatabase(String roleId, String prompt, String name, String notifyTargets, String reviewer, String evolutionSource) {
        if (presetRepository == null) {
            log.warn("数据库未就绪，角色 {} 仅保存到内存", roleId);
            customPrompts.put(roleId, prompt);
            return;
        }

        try {
            // 查找已有的系统内置预设
            List<AgentPreset> existing = presetRepository.findByRole(roleId);
            AgentPreset preset = existing.stream()
                .filter(AgentPreset::isSystem)
                .findFirst()
                .orElse(null);

            if (preset == null) {
                preset = new AgentPreset();
                preset.setRole(roleId);
                preset.setSystem(true);
                preset.setPromptVersion(0);
            }

            // 版本号 +1
            preset.setPromptVersion(preset.getPromptVersion() + 1);
            preset.setPrompt(prompt);
            preset.setRoleName(name != null ? name : roleId);
            preset.setNotifyTargets(notifyTargets != null ? notifyTargets : "producer");
            preset.setReviewer(reviewer);
            preset.setName(name != null ? name : roleId);
            preset.setLastEvolutionSource(evolutionSource);
            preset.setLastEvolutionAt(java.time.LocalDateTime.now());
            presetRepository.save(preset);

            // 更新内存缓存
            dbPrompts.put(roleId, prompt);
            if (name != null) roleNames.put(roleId, name);
            if (notifyTargets != null) {
                defaultNotifyTargets.put(roleId, new HashSet<>(Arrays.asList(notifyTargets.split("[,，]\\s*"))));
            }
            if (reviewer != null) defaultReviewers.put(roleId, reviewer);

            log.info("角色 {} 已保存到数据库 (v{}, 来源: {})", roleId, preset.getPromptVersion(), evolutionSource);
        } catch (Exception e) {
            log.error("保存角色 {} 到数据库失败", roleId, e);
        }
    }

    /**
     * 获取角色的进化元数据（版本号、来源、时间）
     *
     * @param roleId 角色标识
     * @return 元数据 Map，不存在返回 null
     */
    public Map<String, Object> getEvolutionMeta(String roleId) {
        if (presetRepository == null) return null;
        try {
            List<AgentPreset> existing = presetRepository.findByRole(roleId);
            return existing.stream()
                .filter(AgentPreset::isSystem)
                .findFirst()
                .map(p -> {
                    Map<String, Object> meta = new LinkedHashMap<>();
                    meta.put("version", p.getPromptVersion());
                    meta.put("source", p.getLastEvolutionSource());
                    meta.put("lastEvolutionAt", p.getLastEvolutionAt());
                    return meta;
                })
                .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从数据库删除角色提示词（回退到文件版本）
     *
     * @param roleId 角色标识
     */
    public void removeFromDatabase(String roleId) {
        if (presetRepository == null) return;

        try {
            List<AgentPreset> existing = presetRepository.findByRole(roleId);
            for (AgentPreset preset : existing) {
                if (preset.isSystem() && preset.getPrompt() != null) {
                    preset.setPrompt(null);
                    presetRepository.save(preset);
                }
            }
            dbPrompts.remove(roleId);
            log.info("角色 {} 的数据库版本已清除，回退到文件版本", roleId);
        } catch (Exception e) {
            log.error("清除角色 {} 数据库版本失败", roleId, e);
        }
    }

    /**
     * 注册自定义角色提示词（临时，不持久化）
     */
    public void registerCustomRole(String role, String prompt) {
        customPrompts.put(role, prompt);
    }

    /**
     * 注册自定义角色提示词（含通知目标和审查者）
     */
    public void registerCustomRole(String role, String prompt, Set<String> notifyTargets, String reviewer) {
        customPrompts.put(role, prompt);
        if (notifyTargets != null) defaultNotifyTargets.put(role, notifyTargets);
        if (reviewer != null) defaultReviewers.put(role, reviewer);
    }

    /**
     * 移除自定义角色
     */
    public void removeCustomRole(String role) {
        customPrompts.remove(role);
        defaultNotifyTargets.remove(role);
        defaultReviewers.remove(role);
        roleNames.remove(role);
    }

    /**
     * 获取角色完成任务后的默认通知目标
     */
    public Set<String> getNotifyTargets(String role) {
        Set<String> targets = defaultNotifyTargets.get(role);
        return targets != null ? targets : Set.of("producer");
    }

    /**
     * 获取角色的默认审查者
     */
    public String getReviewer(String role) {
        return defaultReviewers.get(role);
    }

    /**
     * 获取角色的中文名称
     */
    public String getRoleName(String role) {
        return roleNames.getOrDefault(role, role);
    }

    /**
     * 判断角色是否为已知角色
     */
    public boolean isKnownRole(String role) {
        return customPrompts.containsKey(role) || dbPrompts.containsKey(role) || builtinPrompts.containsKey(role);
    }

    /**
     * 获取所有已注册的角色标识
     */
    public Set<String> getAllRoles() {
        Set<String> roles = new HashSet<>(builtinPrompts.keySet());
        roles.addAll(dbPrompts.keySet());
        roles.addAll(customPrompts.keySet());
        return roles;
    }

    /**
     * 获取所有内置角色的摘要信息（用于前端展示）
     */
    public List<Map<String, Object>> getBuiltinRoleSummaries() {
        List<Map<String, Object>> summaries = new ArrayList<>();
        for (String role : getAllRoles()) {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("role", role);
            summary.put("name", roleNames.getOrDefault(role, role));
            summary.put("reviewer", defaultReviewers.get(role));
            summary.put("notifyTargets", defaultNotifyTargets.getOrDefault(role, Set.of("producer")));
            summary.put("source", dbPrompts.containsKey(role) ? "database" : "file");
            summaries.add(summary);
        }
        return summaries;
    }

    /**
     * 刷新数据库缓存（知识进化后调用）
     */
    public void refreshFromDatabase() {
        loadRolesFromDatabase();
        log.info("角色缓存已从数据库刷新");
    }

    // ===== 数据库加载 =====

    /**
     * 从数据库加载角色提示词
     * 数据库版本覆盖文件版本
     */
    private void loadRolesFromDatabase() {
        if (presetRepository == null) {
            log.debug("数据库仓库未就绪，跳过数据库角色加载");
            return;
        }

        try {
            List<AgentPreset> presets = presetRepository.findBySystemTrue();
            int loaded = 0;
            for (AgentPreset preset : presets) {
                String roleId = preset.getRole();
                if (roleId == null || roleId.isEmpty()) continue;

                // 加载提示词（数据库版本覆盖文件版本）
                if (preset.getPrompt() != null && !preset.getPrompt().isEmpty()) {
                    dbPrompts.put(roleId, preset.getPrompt());
                }

                // 加载元数据
                if (preset.getRoleName() != null) {
                    roleNames.put(roleId, preset.getRoleName());
                }
                if (preset.getNotifyTargets() != null && !preset.getNotifyTargets().isEmpty()) {
                    defaultNotifyTargets.put(roleId,
                        new HashSet<>(Arrays.asList(preset.getNotifyTargets().split("[,，]\\s*"))));
                }
                if (preset.getReviewer() != null) {
                    defaultReviewers.put(roleId, preset.getReviewer());
                }
                loaded++;
            }
            log.info("从数据库加载 {} 个角色配置，其中 {} 个有自定义提示词",
                loaded, dbPrompts.size());
        } catch (Exception e) {
            log.error("从数据库加载角色失败", e);
        }
    }

    // ===== 文件加载 =====

    /**
     * 从 agents/ 目录加载所有内置角色
     */
    private void loadBuiltinRolesFromFiles() {
        Path agentsDir = Paths.get(AGENTS_DIR);
        if (!Files.exists(agentsDir)) {
            log.warn("角色文件目录不存在: {}，尝试从 classpath 加载", agentsDir);
            loadFromClasspath();
            return;
        }

        try (Stream<Path> files = Files.list(agentsDir)) {
            files.filter(p -> p.toString().endsWith(".md"))
                 .filter(p -> !p.getFileName().toString().startsWith("_"))
                 .sorted()
                 .forEach(this::loadRoleFromFile);
            log.info("从文件加载 {} 个内置角色", builtinPrompts.size());
        } catch (IOException e) {
            log.error("加载角色文件失败", e);
        }
    }

    /**
     * 从 classpath 加载角色文件（打包后的 jar 内）
     */
    private void loadFromClasspath() {
        try {
            var resource = getClass().getClassLoader().getResource("agents");
            if (resource == null) {
                log.warn("classpath:agents/ 目录不存在，使用空角色库");
                return;
            }
            var uri = resource.toURI();
            if ("jar".equals(uri.getScheme())) {
                try (var fs = java.nio.file.FileSystems.newFileSystem(uri, Map.of())) {
                    Path jarAgents = fs.getPath("agents");
                    try (Stream<Path> files = Files.list(jarAgents)) {
                        files.filter(p -> p.toString().endsWith(".md"))
                             .filter(p -> !p.getFileName().toString().startsWith("_"))
                             .forEach(this::loadRoleFromResource);
                    }
                }
            } else {
                Path agentsPath = Paths.get(uri);
                try (Stream<Path> files = Files.list(agentsPath)) {
                    files.filter(p -> p.toString().endsWith(".md"))
                         .filter(p -> !p.getFileName().toString().startsWith("_"))
                         .forEach(this::loadRoleFromFile);
                }
            }
            log.info("从 classpath 加载 {} 个内置角色", builtinPrompts.size());
        } catch (Exception e) {
            log.error("从 classpath 加载角色文件失败", e);
        }
    }

    private void loadRoleFromResource(Path path) {
        try {
            String fileName = path.getFileName().toString().replace(".md", "");
            String content = Files.readString(path);
            parseAndRegisterRole(fileName, content, true);
        } catch (Exception e) {
            log.error("加载角色资源失败: {}", path, e);
        }
    }

    private void loadRoleFromFile(Path filePath) {
        try {
            String fileName = filePath.getFileName().toString().replace(".md", "");
            String content = Files.readString(filePath);
            parseAndRegisterRole(fileName, content, true);
        } catch (Exception e) {
            log.error("加载角色文件失败: {}", filePath, e);
        }
    }

    /**
     * 解析角色文件内容并注册
     *
     * @param roleId    角色ID（文件名）
     * @param content   文件完整内容
     * @param isBuiltin 是否为内置角色
     */
    private void parseAndRegisterRole(String roleId, String content, boolean isBuiltin) {
        String[] parts = content.split("---", 3);
        String prompt;
        Map<String, String> metadata = new HashMap<>();

        if (parts.length >= 3) {
            metadata = parseMetadata(parts[1]);
            prompt = parts[2].trim();
        } else {
            prompt = content.trim();
        }

        if (isBuiltin) {
            builtinPrompts.put(roleId, prompt);
        }

        String name = metadata.get("name");
        if (name != null) roleNames.put(roleId, name);

        String notifyTargets = metadata.get("notifyTargets");
        if (notifyTargets != null && !notifyTargets.isEmpty()) {
            defaultNotifyTargets.put(roleId, new HashSet<>(Arrays.asList(notifyTargets.split("[,，]\\s*"))));
        }

        String reviewer = metadata.get("reviewer");
        if (reviewer != null && !reviewer.isEmpty()) {
            defaultReviewers.put(roleId, reviewer);
        }
    }

    private Map<String, String> parseMetadata(String metadataStr) {
        Map<String, String> metadata = new HashMap<>();
        for (String line : metadataStr.split("\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int colonIdx = line.indexOf(':');
            if (colonIdx > 0) {
                metadata.put(line.substring(0, colonIdx).trim(), line.substring(colonIdx + 1).trim());
            }
        }
        return metadata;
    }

    private String buildGenericPrompt(String role) {
        return String.format("""
            # 角色：%s

            ## 身份定位
            你是游戏开发团队的 %s 角色。你是一个专业的游戏开发者，具备扎实的技术能力和良好的团队协作意识。

            ## 工作原则
            1. 专注于你的专业领域，高质量完成分配的任务
            2. 与团队成员保持良好协作，主动了解其他成员的工作进展
            3. 遇到不确定的问题及时向上级（制作人）汇报，不要擅自做决定
            4. 所有代码输出必须有充分的中文注释（类注释、方法注释、关键逻辑注释）
            5. 完成任务后及时报告进度，包含完成内容和遇到的问题

            ## 代码规范
            - 类名使用大驼峰（PascalCase），方法名使用小驼峰（camelCase）
            - 常量使用全大写下划线分隔（UPPER_SNAKE_CASE）
            - 每个公共方法必须有 Javadoc 注释
            - 复杂业务逻辑必须有行内注释

            ## 输出要求
            - 代码文件必须有完整的文件路径
            - 设计文档必须有清晰的结构
            - 所有变更必须说明原因
            - 列出所有创建/修改的文件清单
            """, role, role);
    }
}
