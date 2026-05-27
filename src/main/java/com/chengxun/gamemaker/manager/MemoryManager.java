package com.chengxun.gamemaker.manager;

import com.chengxun.gamemaker.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class MemoryManager {

    private static final Logger log = LoggerFactory.getLogger(MemoryManager.class);

    private final AppConfig appConfig;
    private final Map<String, Map<String, String>> memoryCache = new HashMap<>();

    public MemoryManager(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    // ===== 分类记忆操作 =====

    public void saveMemory(String agentId, String category, String key, String value) {
        String cacheKey = category + "/" + key;
        memoryCache.computeIfAbsent(agentId, k -> new HashMap<>()).put(cacheKey, value);

        Path memoryPath = getMemoryPath(agentId, category, key);
        try {
            Files.createDirectories(memoryPath.getParent());
            Files.writeString(memoryPath, value);
            log.debug("Memory saved for agent: {}, category: {}, key: {}", agentId, category, key);
        } catch (IOException e) {
            log.error("Failed to save memory for agent: {}, category: {}, key: {}", agentId, category, key, e);
        }
    }

    public String loadMemory(String agentId, String category, String key) {
        String cacheKey = category + "/" + key;
        Map<String, String> agentMemory = memoryCache.get(agentId);
        if (agentMemory != null && agentMemory.containsKey(cacheKey)) {
            return agentMemory.get(cacheKey);
        }

        Path memoryPath = getMemoryPath(agentId, category, key);
        if (!Files.exists(memoryPath)) {
            return null;
        }

        try {
            String value = Files.readString(memoryPath);
            memoryCache.computeIfAbsent(agentId, k -> new HashMap<>()).put(cacheKey, value);
            return value;
        } catch (IOException e) {
            log.error("Failed to load memory for agent: {}, category: {}, key: {}", agentId, category, key, e);
            return null;
        }
    }

    public void deleteMemory(String agentId, String category, String key) {
        String cacheKey = category + "/" + key;
        Map<String, String> agentMemory = memoryCache.get(agentId);
        if (agentMemory != null) {
            agentMemory.remove(cacheKey);
        }

        Path memoryPath = getMemoryPath(agentId, category, key);
        try {
            Files.deleteIfExists(memoryPath);
            log.info("Memory deleted for agent: {}, category: {}, key: {}", agentId, category, key);
        } catch (IOException e) {
            log.error("Failed to delete memory for agent: {}, category: {}, key: {}", agentId, category, key, e);
        }
    }

    public Map<String, String> getCategoryMemory(String agentId, String category) {
        Map<String, String> result = new HashMap<>();
        Path categoryDir = getCategoryPath(agentId, category);
        if (!Files.exists(categoryDir)) {
            return result;
        }

        try {
            Files.list(categoryDir)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        String key = path.getFileName().toString().replace(".md", "").replace(".txt", "");
                        String value = Files.readString(path);
                        result.put(key, value);
                    } catch (IOException e) {
                        log.error("Failed to read memory file: {}", path, e);
                    }
                });
        } catch (IOException e) {
            log.error("Failed to list memory for agent: {}, category: {}", agentId, category, e);
        }

        return result;
    }

    public Map<String, String> getAllMemory(String agentId) {
        Map<String, String> result = new HashMap<>();
        Path agentMemoryDir = Path.of(appConfig.getMemoryDir(), agentId);
        if (!Files.exists(agentMemoryDir)) {
            return result;
        }

        try {
            Files.walk(agentMemoryDir, 2)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        Path relative = agentMemoryDir.relativize(path);
                        String key = relative.toString().replace(".md", "").replace(".txt", "").replace("\\", "/");
                        String value = Files.readString(path);
                        result.put(key, value);
                    } catch (IOException e) {
                        log.error("Failed to read memory file: {}", path, e);
                    }
                });
        } catch (IOException e) {
            log.error("Failed to list all memory for agent: {}", agentId, e);
        }

        return result;
    }

    public List<String> searchMemory(String agentId, String keyword) {
        Map<String, String> allMemory = getAllMemory(agentId);
        return allMemory.entrySet().stream()
            .filter(entry -> entry.getKey().contains(keyword) || entry.getValue().contains(keyword))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    public void createCategory(String agentId, String category) {
        Path categoryPath = getCategoryPath(agentId, category);
        try {
            Files.createDirectories(categoryPath);
            log.debug("Category created for agent: {}, category: {}", agentId, category);
        } catch (IOException e) {
            log.error("Failed to create category for agent: {}, category: {}", agentId, category, e);
        }
    }

    // ===== 兼容旧接口 =====

    public void saveMemory(String agentId, String key, String value) {
        saveMemory(agentId, "general", key, value);
    }

    public String loadMemory(String agentId, String key) {
        return loadMemory(agentId, "general", key);
    }

    // ===== 路径方法 =====

    private Path getCategoryPath(String agentId, String category) {
        return Path.of(appConfig.getMemoryDir(), agentId, category);
    }

    private Path getMemoryPath(String agentId, String category, String key) {
        String fileName = key.endsWith(".md") ? key : key + ".md";
        return Path.of(appConfig.getMemoryDir(), agentId, category, fileName);
    }
}
