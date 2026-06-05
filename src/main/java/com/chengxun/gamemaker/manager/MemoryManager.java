package com.chengxun.gamemaker.manager;

import com.chengxun.gamemaker.model.GameProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 记忆管理器
 * 负责 Agent 记忆的存储、加载和管理
 *
 * 记忆存储规则：
 * - 项目级别记忆：存储在项目的 .game-maker/memory/{agentId}/{category}/ 目录下
 * - 全局记忆（兼容）：存储在 data/memory/{agentId}/{category}/ 目录下
 * - 优先加载项目级别记忆，如果项目不存在则使用全局记忆
 *
 * 记忆分类：
 * - skills: 可复用的技能和模式
 * - knowledge: 领域知识和最佳实践
 * - experiences: 历史经验和教训
 * - preferences: 用户和项目偏好
 * - general: 通用记忆
 *
 * @author chengxun
 * @since 1.0.0
 */
@Component
public class MemoryManager {

    private static final Logger log = LoggerFactory.getLogger(MemoryManager.class);

    /** 记忆缓存，key为 "agentId/category/key" */
    private final Map<String, String> memoryCache = new ConcurrentHashMap<>();

    // ===== 项目级别记忆操作 =====

    /**
     * 保存记忆（项目级别）
     *
     * @param project 项目（不能为空）
     * @param agentId Agent ID
     * @param category 记忆分类
     * @param key 记忆键
     * @param value 记忆值
     */
    public void saveMemory(GameProject project, String agentId, String category, String key, String value) {
        String cacheKey = buildCacheKey(project.getId(), agentId, category, key);
        memoryCache.put(cacheKey, value);

        Path memoryPath = getProjectMemoryPath(project, agentId, category, key);
        try {
            Files.createDirectories(memoryPath.getParent());
            Files.writeString(memoryPath, value);
            log.debug("Memory saved: project={}, agent={}, category={}, key={}",
                project.getId(), agentId, category, key);
        } catch (IOException e) {
            log.error("Failed to save memory: project={}, agent={}, category={}, key={}",
                project.getId(), agentId, category, key, e);
        }
    }

    /**
     * 加载记忆（项目级别）
     *
     * @param project 项目（不能为空）
     * @param agentId Agent ID
     * @param category 记忆分类
     * @param key 记忆键
     * @return 记忆值，不存在返回 null
     */
    public String loadMemory(GameProject project, String agentId, String category, String key) {
        String cacheKey = buildCacheKey(project.getId(), agentId, category, key);
        if (memoryCache.containsKey(cacheKey)) {
            return memoryCache.get(cacheKey);
        }

        Path memoryPath = getProjectMemoryPath(project, agentId, category, key);
        if (!Files.exists(memoryPath)) {
            return null;
        }

        try {
            String value = Files.readString(memoryPath);
            memoryCache.put(cacheKey, value);
            return value;
        } catch (IOException e) {
            log.error("Failed to load memory: project={}, agent={}, category={}, key={}",
                project.getId(), agentId, category, key, e);
            return null;
        }
    }

    /**
     * 删除记忆（项目级别）
     *
     * @param project 项目（不能为空）
     * @param agentId Agent ID
     * @param category 记忆分类
     * @param key 记忆键
     */
    public void deleteMemory(GameProject project, String agentId, String category, String key) {
        String cacheKey = buildCacheKey(project.getId(), agentId, category, key);
        memoryCache.remove(cacheKey);

        Path memoryPath = getProjectMemoryPath(project, agentId, category, key);
        try {
            Files.deleteIfExists(memoryPath);
            log.info("Memory deleted: project={}, agent={}, category={}, key={}",
                project.getId(), agentId, category, key);
        } catch (IOException e) {
            log.error("Failed to delete memory: project={}, agent={}, category={}, key={}",
                project.getId(), agentId, category, key, e);
        }
    }

    /**
     * 获取分类下的所有记忆（项目级别）
     *
     * @param project 项目（不能为空）
     * @param agentId Agent ID
     * @param category 记忆分类
     * @return 记忆键值对
     */
    public Map<String, String> getCategoryMemory(GameProject project, String agentId, String category) {
        Map<String, String> result = new HashMap<>();
        Path categoryDir = getProjectCategoryPath(project, agentId, category);
        if (!Files.exists(categoryDir)) {
            return result;
        }

        try {
            Files.list(categoryDir)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        String key = path.getFileName().toString()
                            .replace(".md", "")
                            .replace(".txt", "")
                            .replace(".json", "");
                        String value = Files.readString(path);
                        result.put(key, value);
                    } catch (IOException e) {
                        log.error("Failed to read memory file: {}", path, e);
                    }
                });
        } catch (IOException e) {
            log.error("Failed to list memory: project={}, agent={}, category={}",
                project.getId(), agentId, category, e);
        }

        return result;
    }

    /**
     * 获取 Agent 的所有记忆（项目级别）
     *
     * @param project 项目（不能为空）
     * @param agentId Agent ID
     * @return 所有记忆键值对
     */
    public Map<String, String> getAllMemory(GameProject project, String agentId) {
        Map<String, String> result = new HashMap<>();
        Path agentMemoryDir = getProjectAgentMemoryPath(project, agentId);
        if (!Files.exists(agentMemoryDir)) {
            return result;
        }

        try {
            Files.walk(agentMemoryDir, 2)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        Path relative = agentMemoryDir.relativize(path);
                        String key = relative.toString()
                            .replace(".md", "")
                            .replace(".txt", "")
                            .replace(".json", "")
                            .replace("\\", "/");
                        String value = Files.readString(path);
                        result.put(key, value);
                    } catch (IOException e) {
                        log.error("Failed to read memory file: {}", path, e);
                    }
                });
        } catch (IOException e) {
            log.error("Failed to list all memory: project={}, agent={}", project.getId(), agentId, e);
        }

        return result;
    }

    /**
     * 搜索记忆（项目级别）
     *
     * @param project 项目（不能为空）
     * @param agentId Agent ID
     * @param keyword 搜索关键词
     * @return 匹配的记忆键列表
     */
    public List<String> searchMemory(GameProject project, String agentId, String keyword) {
        Map<String, String> allMemory = getAllMemory(project, agentId);
        return allMemory.entrySet().stream()
            .filter(entry -> entry.getKey().contains(keyword) || entry.getValue().contains(keyword))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /**
     * 创建记忆分类目录（项目级别）
     *
     * @param project 项目（不能为空）
     * @param agentId Agent ID
     * @param category 记忆分类
     */
    public void createCategory(GameProject project, String agentId, String category) {
        Path categoryPath = getProjectCategoryPath(project, agentId, category);
        try {
            Files.createDirectories(categoryPath);
            log.debug("Category created: project={}, agent={}, category={}",
                project.getId(), agentId, category);
        } catch (IOException e) {
            log.error("Failed to create category: project={}, agent={}, category={}",
                project.getId(), agentId, category, e);
        }
    }

    /**
     * 初始化 Agent 的所有记忆分类（项目级别）
     *
     * @param project 项目（不能为空）
     * @param agentId Agent ID
     */
    public void initAgentMemoryCategories(GameProject project, String agentId) {
        createCategory(project, agentId, "skills");
        createCategory(project, agentId, "knowledge");
        createCategory(project, agentId, "experiences");
        createCategory(project, agentId, "preferences");
        createCategory(project, agentId, "general");
        log.info("Memory categories initialized: project={}, agent={}", project.getId(), agentId);
    }

    // ===== 全局记忆操作（兼容旧版本） =====

    /**
     * 保存记忆（全局，兼容旧版本）
     *
     * @param globalMemoryDir 全局记忆目录
     * @param agentId Agent ID
     * @param category 记忆分类
     * @param key 记忆键
     * @param value 记忆值
     */
    @Deprecated
    public void saveGlobalMemory(String globalMemoryDir, String agentId, String category, String key, String value) {
        String cacheKey = buildCacheKey("global", agentId, category, key);
        memoryCache.put(cacheKey, value);

        Path memoryPath = getGlobalMemoryPath(globalMemoryDir, agentId, category, key);
        try {
            Files.createDirectories(memoryPath.getParent());
            Files.writeString(memoryPath, value);
            log.debug("Global memory saved: agent={}, category={}, key={}", agentId, category, key);
        } catch (IOException e) {
            log.error("Failed to save global memory: agent={}, category={}, key={}", agentId, category, key, e);
        }
    }

    /**
     * 加载记忆（全局，兼容旧版本）
     */
    @Deprecated
    public String loadGlobalMemory(String globalMemoryDir, String agentId, String category, String key) {
        String cacheKey = buildCacheKey("global", agentId, category, key);
        if (memoryCache.containsKey(cacheKey)) {
            return memoryCache.get(cacheKey);
        }

        Path memoryPath = getGlobalMemoryPath(globalMemoryDir, agentId, category, key);
        if (!Files.exists(memoryPath)) {
            return null;
        }

        try {
            String value = Files.readString(memoryPath);
            memoryCache.put(cacheKey, value);
            return value;
        } catch (IOException e) {
            log.error("Failed to load global memory: agent={}, category={}, key={}", agentId, category, key, e);
            return null;
        }
    }

    // ===== 路径方法 =====

    /**
     * 获取项目级别的记忆路径
     */
    private Path getProjectMemoryPath(GameProject project, String agentId, String category, String key) {
        String fileName = key.endsWith(".md") ? key : key + ".md";
        return Path.of(project.getMemoryDir(), agentId, category, fileName);
    }

    /**
     * 获取项目级别的分类路径
     */
    private Path getProjectCategoryPath(GameProject project, String agentId, String category) {
        return Path.of(project.getMemoryDir(), agentId, category);
    }

    /**
     * 获取项目级别的 Agent 记忆根路径
     */
    private Path getProjectAgentMemoryPath(GameProject project, String agentId) {
        return Path.of(project.getMemoryDir(), agentId);
    }

    /**
     * 获取全局记忆路径（兼容）
     */
    private Path getGlobalMemoryPath(String globalMemoryDir, String agentId, String category, String key) {
        String fileName = key.endsWith(".md") ? key : key + ".md";
        return Path.of(globalMemoryDir, agentId, category, fileName);
    }

    /**
     * 构建缓存键
     */
    private String buildCacheKey(String projectId, String agentId, String category, String key) {
        return projectId + "/" + agentId + "/" + category + "/" + key;
    }
}
