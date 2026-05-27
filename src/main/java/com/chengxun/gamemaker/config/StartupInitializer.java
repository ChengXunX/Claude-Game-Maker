package com.chengxun.gamemaker.config;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.manager.ContextManager;
import com.chengxun.gamemaker.manager.MemoryManager;
import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.manager.SkillManager;
import com.chengxun.gamemaker.model.AgentContext;
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
    private final SkillManager skillManager;
    private final ProjectManager projectManager;

    public StartupInitializer(AppConfig appConfig,
                             AgentConfig agentConfig,
                             AgentManager agentManager,
                             ContextManager contextManager,
                             MemoryManager memoryManager,
                             SkillManager skillManager,
                             ProjectManager projectManager) {
        this.appConfig = appConfig;
        this.agentConfig = agentConfig;
        this.agentManager = agentManager;
        this.contextManager = contextManager;
        this.memoryManager = memoryManager;
        this.skillManager = skillManager;
        this.projectManager = projectManager;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Game Maker starting up...");

        createDataDirectories();
        createAgents();
        loadAgentsFiles();
        initDefaultSkills();

        log.info("Game Maker startup complete. Agents: {}, Skills: {}, Projects: {}",
            agentManager.getAllAgents().size(),
            skillManager.getAllGlobalSkills().size(),
            projectManager.getAllProjects().size());
    }

    private void createDataDirectories() {
        try {
            Files.createDirectories(Path.of(appConfig.getDataDir()));
            Files.createDirectories(Path.of(appConfig.getProjectsDir()));
            Files.createDirectories(Path.of(appConfig.getContextsDir()));
            Files.createDirectories(Path.of(appConfig.getMemoryDir()));
            log.info("Data directories created");
        } catch (Exception e) {
            log.error("Failed to create data directories", e);
        }
    }

    private void createAgents() {
        agentConfig.getDefinitions().forEach((key, def) -> {
            AgentDefinition definition = AgentDefinition.builder()
                .id(key)
                .name(def.getName())
                .role(def.getRole())
                .description(def.getDescription())
                .agentsFile(def.getAgentsFile())
                .workDir(appConfig.getDefaultWorkDir())
                .apiKey(appConfig.getClaude().getApiKey())
                .apiUrl(appConfig.getClaude().getApiUrl())
                .model(appConfig.getClaude().getModel())
                .status(AgentDefinition.AgentStatus.IDLE)
                .parent("producer".equals(def.getRole()))
                .build();

            // 尝试加载保存的上下文（全局）
            AgentContext savedContext = contextManager.loadContext(key, null);
            if (savedContext != null) {
                definition.setSessionId(savedContext.getSessionId());
                if (savedContext.getWorkDir() != null) {
                    definition.setWorkDir(savedContext.getWorkDir());
                }
                log.info("Restored context for agent: {}", key);
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
                        memoryManager.saveMemory(key, "knowledge", "agents_file", content);
                        log.info("Loaded agents file for {}: {}", key, def.getAgentsFile());
                    }
                } catch (Exception e) {
                    log.warn("Failed to load agents file for {}: {}", key, e.getMessage());
                }
            }
        });
    }

    private void initDefaultSkills() {
        log.info("Default skills initialized: {}", skillManager.getAllGlobalSkills().size());
    }
}
