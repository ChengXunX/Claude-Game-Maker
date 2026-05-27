package com.chengxun.gamemaker.config;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.manager.ContextManager;
import com.chengxun.gamemaker.manager.MemoryManager;
import com.chengxun.gamemaker.model.AgentDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class StartupInitializer {
    
    private static final Logger log = LoggerFactory.getLogger(StartupInitializer.class);
    
    private final AppConfig appConfig;
    private final AgentConfig agentConfig;
    private final AgentManager agentManager;
    private final ContextManager contextManager;
    private final MemoryManager memoryManager;
    
    public StartupInitializer(AppConfig appConfig,
                             AgentConfig agentConfig,
                             AgentManager agentManager,
                             ContextManager contextManager,
                             MemoryManager memoryManager) {
        this.appConfig = appConfig;
        this.agentConfig = agentConfig;
        this.agentManager = agentManager;
        this.contextManager = contextManager;
        this.memoryManager = memoryManager;
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Game Maker starting up...");
        
        createAgents();
        loadAgentsFiles();
        
        log.info("Game Maker startup complete. Agents: {}", agentManager.getAllAgents().size());
    }
    
    private void createAgents() {
        agentConfig.getDefinitions().forEach((key, def) -> {
            AgentDefinition definition = AgentDefinition.builder()
                .id(key)
                .name(def.getName())
                .role(def.getRole())
                .description(def.getDescription())
                .agentsFile(def.getAgentsFile())
                .apiKey(appConfig.getClaude().getApiKey())
                .apiUrl(appConfig.getClaude().getApiUrl())
                .model(appConfig.getClaude().getModel())
                .status(AgentDefinition.AgentStatus.IDLE)
                .parent("producer".equals(def.getRole()))
                .build();
            
            AgentDefinition saved = contextManager.loadContext(key);
            if (saved != null) {
                definition.setSessionId(saved.getSessionId());
                definition.setWorkDir(saved.getWorkDir());
            }
            
            agentManager.createAgent(definition);
            log.info("Agent created: {} ({})", def.getName(), key);
        });
    }
    
    private void loadAgentsFiles() {
        agentConfig.getDefinitions().forEach((key, def) -> {
            if (def.getAgentsFile() != null && !def.getAgentsFile().isEmpty()) {
                try {
                    Path path = Path.of(def.getAgentsFile());
                    if (Files.exists(path)) {
                        String content = Files.readString(path);
                        memoryManager.saveMemory(key, "agents_file", content);
                        log.info("Loaded agents file for {}: {}", key, def.getAgentsFile());
                    }
                } catch (Exception e) {
                    log.warn("Failed to load agents file for {}: {}", key, e.getMessage());
                }
            }
        });
    }
}
