package com.chengxun.gamemaker.manager;

import com.chengxun.gamemaker.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Component
public class MemoryManager {
    
    private static final Logger log = LoggerFactory.getLogger(MemoryManager.class);
    
    private final AppConfig appConfig;
    private final Map<String, Map<String, String>> memoryCache = new HashMap<>();
    
    public MemoryManager(AppConfig appConfig) {
        this.appConfig = appConfig;
    }
    
    public void saveMemory(String agentId, String key, String value) {
        memoryCache.computeIfAbsent(agentId, k -> new HashMap<>()).put(key, value);
        
        Path memoryPath = getMemoryPath(agentId, key);
        try {
            Files.createDirectories(memoryPath.getParent());
            Files.writeString(memoryPath, value);
            log.debug("Memory saved for agent: {}, key: {}", agentId, key);
        } catch (IOException e) {
            log.error("Failed to save memory for agent: {}, key: {}", agentId, key, e);
        }
    }
    
    public String loadMemory(String agentId, String key) {
        Map<String, String> agentMemory = memoryCache.get(agentId);
        if (agentMemory != null && agentMemory.containsKey(key)) {
            return agentMemory.get(key);
        }
        
        Path memoryPath = getMemoryPath(agentId, key);
        if (!Files.exists(memoryPath)) {
            return null;
        }
        
        try {
            String value = Files.readString(memoryPath);
            memoryCache.computeIfAbsent(agentId, k -> new HashMap<>()).put(key, value);
            return value;
        } catch (IOException e) {
            log.error("Failed to load memory for agent: {}, key: {}", agentId, key, e);
            return null;
        }
    }
    
    public void deleteMemory(String agentId, String key) {
        Map<String, String> agentMemory = memoryCache.get(agentId);
        if (agentMemory != null) {
            agentMemory.remove(key);
        }
        
        Path memoryPath = getMemoryPath(agentId, key);
        try {
            Files.deleteIfExists(memoryPath);
            log.info("Memory deleted for agent: {}, key: {}", agentId, key);
        } catch (IOException e) {
            log.error("Failed to delete memory for agent: {}, key: {}", agentId, key, e);
        }
    }
    
    public Map<String, String> getAllMemory(String agentId) {
        Map<String, String> result = new HashMap<>();
        
        Path agentMemoryDir = Path.of(appConfig.getMemoryDir(), agentId);
        if (!Files.exists(agentMemoryDir)) {
            return result;
        }
        
        try {
            Files.list(agentMemoryDir)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        String key = path.getFileName().toString();
                        String value = Files.readString(path);
                        result.put(key, value);
                    } catch (IOException e) {
                        log.error("Failed to read memory file: {}", path, e);
                    }
                });
        } catch (IOException e) {
            log.error("Failed to list memory for agent: {}", agentId, e);
        }
        
        return result;
    }
    
    private Path getMemoryPath(String agentId, String key) {
        return Path.of(appConfig.getMemoryDir(), agentId, key + ".txt");
    }
}
