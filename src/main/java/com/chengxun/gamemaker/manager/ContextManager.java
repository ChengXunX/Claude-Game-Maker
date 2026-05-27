package com.chengxun.gamemaker.manager;

import com.chengxun.gamemaker.config.AppConfig;
import com.chengxun.gamemaker.model.AgentDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class ContextManager {
    
    private static final Logger log = LoggerFactory.getLogger(ContextManager.class);
    
    private final AppConfig appConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public ContextManager(AppConfig appConfig) {
        this.appConfig = appConfig;
    }
    
    public void saveContext(String agentId, AgentDefinition definition) {
        Path contextPath = getContextPath(agentId);
        try {
            Files.createDirectories(contextPath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(contextPath.toFile(), definition);
            log.debug("Context saved for agent: {}", agentId);
        } catch (IOException e) {
            log.error("Failed to save context for agent: {}", agentId, e);
        }
    }
    
    public AgentDefinition loadContext(String agentId) {
        Path contextPath = getContextPath(agentId);
        if (!Files.exists(contextPath)) {
            return null;
        }
        
        try {
            return objectMapper.readValue(contextPath.toFile(), AgentDefinition.class);
        } catch (IOException e) {
            log.error("Failed to load context for agent: {}", agentId, e);
            return null;
        }
    }
    
    public void saveSessionId(String agentId, String sessionId) {
        AgentDefinition definition = loadContext(agentId);
        if (definition == null) {
            definition = AgentDefinition.builder()
                .id(agentId)
                .sessionId(sessionId)
                .build();
        } else {
            definition.setSessionId(sessionId);
        }
        saveContext(agentId, definition);
    }
    
    public String loadSessionId(String agentId) {
        AgentDefinition definition = loadContext(agentId);
        return definition != null ? definition.getSessionId() : null;
    }
    
    public void deleteContext(String agentId) {
        Path contextPath = getContextPath(agentId);
        try {
            Files.deleteIfExists(contextPath);
            log.info("Context deleted for agent: {}", agentId);
        } catch (IOException e) {
            log.error("Failed to delete context for agent: {}", agentId, e);
        }
    }
    
    private Path getContextPath(String agentId) {
        return Path.of(appConfig.getContextsDir(), agentId + ".json");
    }
}
