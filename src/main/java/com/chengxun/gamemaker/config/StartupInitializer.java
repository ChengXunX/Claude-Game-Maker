package com.chengxun.gamemaker.config;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.manager.ContextManager;
import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.manager.SkillManager;
import com.chengxun.gamemaker.model.AgentDefinition;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.web.entity.ApiToken;
import com.chengxun.gamemaker.web.service.AgentPresetService;
import com.chengxun.gamemaker.web.service.ApiTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 启动初始化器
 * 负责系统启动时的初始化工作
 *
 * 主要功能：
 * - 创建系统数据目录
 * - 初始化系统内置 Agent 预设
 * - 如果配置了默认工作目录，创建默认项目并启动 Agent
 * - 初始化默认技能
 *
 * @author chengxun
 * @since 1.0.0
 */
@Component
public class StartupInitializer {

    private static final Logger log = LoggerFactory.getLogger(StartupInitializer.class);

    private final AppConfig appConfig;
    private final AgentConfig agentConfig;
    private final AgentManager agentManager;
    private final ContextManager contextManager;
    private final SkillManager skillManager;
    private final ProjectManager projectManager;
    private final AgentPresetService presetService;
    private final ApiTokenService tokenService;

    public StartupInitializer(AppConfig appConfig,
                             AgentConfig agentConfig,
                             AgentManager agentManager,
                             ContextManager contextManager,
                             SkillManager skillManager,
                             ProjectManager projectManager,
                             AgentPresetService presetService,
                             ApiTokenService tokenService) {
        this.appConfig = appConfig;
        this.agentConfig = agentConfig;
        this.agentManager = agentManager;
        this.contextManager = contextManager;
        this.skillManager = skillManager;
        this.projectManager = projectManager;
        this.presetService = presetService;
        this.tokenService = tokenService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Game Maker starting up...");

        createDataDirectories();
        initSystemPresets();
        createDefaultProjectAgents();
        restoreTokenBindings();
        initDefaultSkills();

        log.info("Game Maker startup complete. Agents: {}, Skills: {}, Projects: {}",
            agentManager.getAllAgents().size(),
            skillManager.getAllGlobalSkills().size(),
            projectManager.getAllProjects().size());
    }

    /**
     * 创建系统数据目录
     */
    private void createDataDirectories() {
        try {
            Files.createDirectories(Path.of(appConfig.getDataDir()));
            Files.createDirectories(Path.of(appConfig.getTemplatesDir()));
            log.info("Data directories created: data={}, templates={}",
                appConfig.getDataDir(), appConfig.getTemplatesDir());
        } catch (Exception e) {
            log.error("Failed to create data directories", e);
        }
    }

    /**
     * 初始化系统内置 Agent 预设
     */
    private void initSystemPresets() {
        try {
            presetService.initSystemPresets();
            log.info("System presets initialized");
        } catch (Exception e) {
            log.error("Failed to initialize system presets", e);
        }
    }

    /**
     * 创建默认项目的 Agent
     * 如果配置了默认工作目录，创建一个默认项目并启动配置中的 Agent
     *
     * 注意：Agent 必须绑定到项目，不能独立存在
     */
    private void createDefaultProjectAgents() {
        String defaultWorkDir = appConfig.getDefaultWorkDir();
        if (defaultWorkDir == null || defaultWorkDir.isEmpty()) {
            log.info("No default workDir configured, skipping default project agent creation");
            return;
        }

        // 获取或创建默认项目
        GameProject defaultProject = projectManager.getOrCreateProject(defaultWorkDir);
        String projectId = defaultProject.getId();

        log.info("Creating agents for default project: {} ({})", defaultProject.getName(), projectId);

        agentConfig.getDefinitions().forEach((key, def) -> {
            AgentDefinition definition = AgentDefinition.builder()
                .id(key)
                .name(def.getName())
                .role(def.getRole())
                .description(def.getDescription())
                .agentsFile(def.getAgentsFile())
                .workDir(defaultWorkDir)
                .projectId(projectId)
                .apiKey(appConfig.getClaude().getApiKey())
                .apiUrl(appConfig.getClaude().getApiUrl())
                .model(appConfig.getClaude().getModel())
                .status(AgentDefinition.AgentStatus.IDLE)
                .parent("producer".equals(def.getRole()))
                .build();

            agentManager.createAgent(definition);
            log.info("Agent created: {} (runtimeId={}) for project: {}",
                def.getName(), definition.getEffectiveId(), projectId);
        });
    }

    /**
     * 初始化默认技能
     */
    private void initDefaultSkills() {
        log.info("Default skills initialized: {}", skillManager.getAllGlobalSkills().size());
    }

    /**
     * 恢复 Token-Agent 绑定
     * 从数据库中读取已绑定的 Token，将 API 配置应用到对应的 Agent
     *
     * 这样即使 Agent 重启，也能使用绑定的 Token 的 API 配置
     */
    private void restoreTokenBindings() {
        try {
            List<ApiToken> allTokens = tokenService.getAllTokens();
            int restoredCount = 0;

            for (ApiToken token : allTokens) {
                if (token.getAssignedAgentId() != null && !token.getAssignedAgentId().isEmpty()
                    && token.isActive()) {

                    Agent agent = agentManager.getAgent(token.getAssignedAgentId());
                    if (agent != null) {
                        // 恢复 Token 的 API 配置到 Agent
                        agent.getDefinition().setApiKey(token.getApiKey());
                        agent.getDefinition().setApiUrl(token.getApiUrl());
                        agent.getDefinition().setModel(token.getModel());
                        restoredCount++;

                        log.info("Restored token binding: {} -> agent {} (apiUrl={}, model={})",
                            token.getName(), token.getAssignedAgentId(),
                            token.getApiUrl(), token.getModel());
                    } else {
                        log.warn("Agent not found for token binding: {} -> agent {}",
                            token.getName(), token.getAssignedAgentId());
                    }
                }
            }

            if (restoredCount > 0) {
                log.info("Restored {} token bindings", restoredCount);
            }
        } catch (Exception e) {
            log.error("Failed to restore token bindings", e);
        }
    }
}
