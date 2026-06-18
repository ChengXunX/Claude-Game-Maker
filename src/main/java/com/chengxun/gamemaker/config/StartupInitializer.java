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
import com.chengxun.gamemaker.service.AgentInterventionService;
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
    private final AgentInterventionService interventionService;

    public StartupInitializer(AppConfig appConfig,
                             AgentConfig agentConfig,
                             AgentManager agentManager,
                             ContextManager contextManager,
                             SkillManager skillManager,
                             ProjectManager projectManager,
                             AgentPresetService presetService,
                             ApiTokenService tokenService,
                             AgentInterventionService interventionService) {
        this.appConfig = appConfig;
        this.agentConfig = agentConfig;
        this.agentManager = agentManager;
        this.contextManager = contextManager;
        this.skillManager = skillManager;
        this.projectManager = projectManager;
        this.presetService = presetService;
        this.tokenService = tokenService;
        this.interventionService = interventionService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Game Maker starting up...");

        createDataDirectories();
        restorePersistentData();  // 从持久化目录恢复知识库和技能
        initSystemPresets();
        restoreExistingProjectAgents();
        restoreTokenBindings();
        restorePendingInterventions();
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
     * 从持久化目录恢复知识库和技能数据
     * 持久化目录 (data-persist/) 被 git 追踪，系统重置后可自动恢复
     */
    private void restorePersistentData() {
        Path persistDir = Path.of("data-persist");
        if (!Files.exists(persistDir)) {
            log.debug("No persistent data directory found, skipping restore");
            return;
        }

        try {
            // 恢复知识库
            Path kbSource = persistDir.resolve("knowledge-base");
            Path kbTarget = Path.of(appConfig.getDataDir(), "knowledge-base");
            if (Files.exists(kbSource)) {
                copyDirectory(kbSource, kbTarget);
                log.info("Restored knowledge-base from persistent storage");
            }

            // 恢复技能
            Path skillSource = persistDir.resolve("skills");
            Path skillTarget = Path.of(appConfig.getDataDir(), "skills");
            if (Files.exists(skillSource)) {
                copyDirectory(skillSource, skillTarget);
                log.info("Restored skills from persistent storage");
            }
        } catch (Exception e) {
            log.error("Failed to restore persistent data", e);
        }
    }

    /**
     * 递归复制目录
     */
    private void copyDirectory(Path source, Path target) throws Exception {
        Files.createDirectories(target);
        try (var stream = Files.walk(source)) {
            stream.forEach(srcPath -> {
                try {
                    Path destPath = target.resolve(source.relativize(srcPath));
                    if (Files.isDirectory(srcPath)) {
                        Files.createDirectories(destPath);
                    } else {
                        Files.createDirectories(destPath.getParent());
                        Files.copy(srcPath, destPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (Exception e) {
                    log.warn("Failed to copy {}: {}", srcPath, e.getMessage());
                }
            });
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
     * 恢复已有项目的 Agent 实例
     * 从项目配置中读取 agentIds，为每个 Agent 创建运行实例
     *
     * 区别于从 yml 配置创建：这里是从已持久化的项目配置恢复
     */
    private void restoreExistingProjectAgents() {
        try {
            java.util.List<GameProject> projects = projectManager.getAllProjects();
            int restoredCount = 0;

            for (GameProject project : projects) {
                // 跳过已归档的项目，不恢复其 Agent
                if (project.getStatus() == GameProject.ProjectStatus.ARCHIVED) {
                    log.debug("Skipping archived project: {}", project.getName());
                    continue;
                }

                java.util.List<String> agentIds = project.getAgentIds();
                if (agentIds == null || agentIds.isEmpty()) {
                    continue;
                }

                log.info("Restoring agents for project: {} ({} agents)", project.getName(), agentIds.size());

                for (String agentRuntimeId : agentIds) {
                    // 运行时 ID 格式：projectId:agentRole
                    int lastColon = agentRuntimeId.lastIndexOf(':');
                    String role = lastColon > 0 ? agentRuntimeId.substring(lastColon + 1) : agentRuntimeId;

                    // 跳过已存在的 Agent
                    if (agentManager.getAgent(agentRuntimeId) != null) {
                        log.debug("Agent already exists, skipping: {}", agentRuntimeId);
                        continue;
                    }

                    try {
                        AgentDefinition definition = AgentDefinition.builder()
                            .id(role)
                            .name(getDefaultAgentName(role))
                            .role(role)
                            .description("")
                            .workDir(project.getWorkDir())
                            .projectId(project.getId())
                            .apiKey(appConfig.getClaude().getApiKey())
                            .apiUrl(appConfig.getClaude().getApiUrl())
                            .model(appConfig.getClaude().getModel())
                            .parent("producer".equals(role))
                            .build();

                        agentManager.createAgent(definition);
                        restoredCount++;
                        log.info("Agent restored: {} for project: {}", agentRuntimeId, project.getName());
                    } catch (Exception e) {
                        log.warn("Failed to restore agent {} for project {}: {}",
                            agentRuntimeId, project.getName(), e.getMessage());
                    }
                }
            }

            if (restoredCount > 0) {
                log.info("Restored {} agents for existing projects", restoredCount);
            }
        } catch (Exception e) {
            log.error("Failed to restore existing project agents", e);
        }
    }

    /**
     * 获取角色的默认名称
     */
    private String getDefaultAgentName(String role) {
        return switch (role) {
            case "producer" -> "制作人";
            case "server-dev" -> "服务端开发";
            case "client-dev" -> "客户端开发";
            case "ui-dev" -> "UI设计";
            case "system-planner" -> "系统策划";
            case "numerical-planner" -> "数值策划";
            case "tester" -> "测试工程师";
            case "git-commit" -> "Git专员";
            default -> role;
        };
    }

    /**
     * 初始化默认技能
     */
    private void initDefaultSkills() {
        log.info("Default skills initialized: {}", skillManager.getAllGlobalSkills().size());
    }

    /**
     * 恢复待处理的干预指令
     * 系统重启后，数据库中状态为 PENDING/ACKNOWLEDGED 的干预需要重新投递给对应 Agent
     * 避免干预因重启而永久丢失
     */
    private void restorePendingInterventions() {
        try {
            java.util.List<com.chengxun.gamemaker.web.entity.AgentIntervention> pending =
                interventionService.getAllPendingInterventions();

            if (pending.isEmpty()) {
                log.info("No pending interventions to restore");
                return;
            }

            int restored = 0;
            for (com.chengxun.gamemaker.web.entity.AgentIntervention intervention : pending) {
                String agentId = intervention.getAgentId();
                Agent agent = agentManager.getAgent(agentId);

                if (agent != null && agent.isAlive() && agent instanceof com.chengxun.gamemaker.agent.BaseAgent baseAgent) {
                    // 重新构建干预消息并投递给 Agent
                    String message = buildInterventionMessage(intervention);
                    com.chengxun.gamemaker.model.AgentMessage agentMessage = com.chengxun.gamemaker.model.AgentMessage.builder()
                        .fromAgentId("system")
                        .toAgentId(agentId)
                        .type(com.chengxun.gamemaker.model.AgentMessage.MessageType.SYSTEM)
                        .content(message)
                        .build();

                    baseAgent.receiveMessage(agentMessage);
                    restored++;
                    log.info("Restored pending intervention {} to agent {}", intervention.getInterventionNo(), agentId);
                } else {
                    log.debug("Skipping intervention {} - agent {} not available", intervention.getInterventionNo(), agentId);
                }
            }

            if (restored > 0) {
                log.info("Restored {} pending interventions to agents", restored);
            }
        } catch (Exception e) {
            log.error("Failed to restore pending interventions", e);
        }
    }

    /**
     * 构建干预消息内容（与 AgentInterventionService.buildInterventionMessage 一致）
     */
    private String buildInterventionMessage(com.chengxun.gamemaker.web.entity.AgentIntervention intervention) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 人工干预通知\n\n");
        sb.append("**干预编号**: ").append(intervention.getInterventionNo()).append("\n");
        sb.append("**干预类型**: ").append(intervention.getInterventionTypeDescription()).append("\n");
        sb.append("**干预人**: ").append(intervention.getUsername()).append(" (").append(intervention.getUserRole()).append(")\n\n");

        if (intervention.getInstruction() != null) {
            sb.append("**指令内容**:\n").append(intervention.getInstruction()).append("\n\n");
        }
        if (intervention.getReason() != null) {
            sb.append("**干预原因**:\n").append(intervention.getReason()).append("\n\n");
        }

        sb.append("请确认并执行此干预指令。");
        return sb.toString();
    }

    /**
     * 恢复 Token 分配
     * 池化模式下，启动时主动为每个活跃 Agent 从 Token 池中获取最佳可用 Token
     * 避免重启后 Agent 使用系统默认配置直到第一次任务完成
     */
    private void restoreTokenBindings() {
        try {
            List<ApiToken> allTokens = tokenService.getAllTokens();
            long activeCount = allTokens.stream().filter(ApiToken::isActive).count();
            log.info("Token pool ready: {} active tokens available", activeCount);

            if (activeCount == 0) return;

            // 为每个活跃 Agent 主动分配最佳 Token
            int assigned = 0;
            for (Agent agent : agentManager.getAllAgents()) {
                if (!agent.isAlive()) continue;

                ApiToken token = tokenService.findBestTokenForRole(agent.getRole());
                if (token == null) continue;

                // 将 Token 配置应用到 Agent Definition
                if (token.getApiKey() != null) agent.getDefinition().setApiKey(token.getApiKey());
                if (token.getApiUrl() != null) agent.getDefinition().setApiUrl(token.getApiUrl());
                if (token.getModel() != null) agent.getDefinition().setModel(token.getModel());
                if (token.getContextWindow() != null) agent.getDefinition().setMaxContextSize(token.getContextWindow());
                agent.getDefinition().setAssignedTokenId(token.getId());
                assigned++;

                log.info("Restored token '{}' to agent {} (role: {})",
                    token.getName(), agent.getId(), agent.getRole());
            }

            if (assigned > 0) {
                log.info("Restored token allocation for {} agents", assigned);
            }
        } catch (Exception e) {
            log.error("Failed to restore token bindings", e);
        }
    }
}
