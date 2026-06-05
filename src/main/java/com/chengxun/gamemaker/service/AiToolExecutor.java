package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.manager.*;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.model.Skill;
import com.chengxun.gamemaker.web.entity.McpServer;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * AI工具执行器
 * 执行AI助手调用的所有系统工具
 *
 * 工具分类：
 * - Agent 管理：查询、发送任务、暂停、恢复、干预
 * - 项目管理：查询、创建、从模板创建
 * - 工作流管理：查询模板、创建模板、启动工作流
 * - 游戏模板：查询、创建
 * - 技能管理：查询、创建
 * - 系统监控：告警、日志、资源、健康
 * - 配置管理：系统配置、Token、用户、角色
 * - CI/CD：流水线、代码审查、Git仓库
 *
 * @author chengxun
 * @since 1.0.0
 */
@Component
public class AiToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(AiToolExecutor.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AgentManager agentManager;

    @Autowired
    private ProjectManager projectManager;

    @Autowired
    private SkillManager skillManager;

    @Autowired
    private CapabilityRegistry capabilityRegistry;

    @Autowired
    private McpService mcpService;

    @Autowired
    private UserService userService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SystemConfigService configService;

    @Autowired
    private ApiTokenService tokenService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private AgentLogService agentLogService;

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private WorkflowEngine workflowEngine;

    @Autowired
    private PipelineService pipelineService;

    @Autowired
    private GitRepositoryService gitRepositoryService;

    @Autowired
    private CodeReviewService codeReviewService;

    @Autowired
    private GameTemplateService gameTemplateService;

    @Autowired
    private AgentInterventionService interventionService;

    @Autowired
    private AgentHealthService agentHealthService;

    /**
     * 执行工具调用
     *
     * @param toolName 工具名称
     * @param params 参数
     * @param username 当前用户名
     * @return 执行结果
     */
    public Map<String, Object> executeTool(String toolName, Map<String, Object> params, String username) {
        log.info("执行工具: {} - 用户: {} - 参数: {}", toolName, username, params);

        try {
            switch (toolName) {
                // ===== Agent 管理 =====
                case "list_agents":
                    return executeListAgents();
                case "send_agent_task":
                    return executeSendAgentTask(params);
                case "intervene_agent":
                    return executeInterveneAgent(params, username);
                case "pause_agent":
                    return executePauseAgent(params);
                case "resume_agent":
                    return executeResumeAgent(params);
                case "get_agent_health":
                    return executeGetAgentHealth();
                case "get_agent_logs":
                    return executeGetAgentLogs(params);

                // ===== 项目管理 =====
                case "list_projects":
                    return executeListProjects();
                case "create_project":
                    return executeCreateProject(params);
                case "create_project_from_template":
                    return executeCreateProjectFromTemplate(params);

                // ===== 工作流管理 =====
                case "list_workflow_templates":
                    return executeListWorkflowTemplates();
                case "create_workflow_template":
                    return executeCreateWorkflowTemplate(params);
                case "delete_workflow_template":
                    return executeDeleteWorkflowTemplate(params);
                case "start_workflow":
                    return executeStartWorkflow(params);
                case "list_workflow_instances":
                    return executeListWorkflowInstances();
                case "cancel_workflow":
                    return executeCancelWorkflow(params);
                case "pause_workflow":
                    return executePauseWorkflow(params);
                case "resume_workflow":
                    return executeResumeWorkflow(params);

                // ===== 游戏模板 =====
                case "list_game_templates":
                    return executeListGameTemplates();
                case "create_game_template":
                    return executeCreateGameTemplate(params);
                case "delete_game_template":
                    return executeDeleteGameTemplate(params);

                // ===== 技能管理 =====
                case "list_skills":
                    return executeListSkills();
                case "create_skill":
                    return executeCreateSkill(params);

                // ===== MCP 服务 =====
                case "list_mcp_servers":
                    return executeListMcpServers();
                case "add_mcp_server":
                    return executeAddMcpServer(params);
                case "test_mcp_server":
                    return executeTestMcpServer(params);

                // ===== 系统监控 =====
                case "list_alerts":
                    return executeListAlerts();
                case "acknowledge_alert":
                    return executeAcknowledgeAlert(params, username);
                case "resolve_alert":
                    return executeResolveAlert(params, username);
                case "list_notifications":
                    return executeListNotifications(username);
                case "mark_notification_read":
                    return executeMarkNotificationRead(params);
                case "mark_all_notifications_read":
                    return executeMarkAllNotificationsRead(username);
                case "get_resource_usage":
                    return executeGetResourceUsage();
                case "get_operation_logs":
                    return executeGetOperationLogs();

                // ===== 配置管理 =====
                case "list_configs":
                    return executeListConfigs();
                case "update_config":
                    return executeUpdateConfig(params);
                case "list_tokens":
                    return executeListTokens();
                case "list_users":
                    return executeListUsers();
                case "list_roles":
                    return executeListRoles();

                // ===== CI/CD =====
                case "list_pipelines":
                    return executeListPipelines();
                case "trigger_pipeline":
                    return executeTriggerPipeline(params);
                case "list_git_repos":
                    return executeListGitRepos();
                case "list_reviews":
                    return executeListReviews();

                // ===== 能力管理 =====
                case "list_capabilities":
                    return executeListCapabilities();
                case "create_capability":
                    return executeCreateCapability(params);

                // ===== 系统信息 =====
                case "get_system_info":
                    return executeGetSystemInfo();
                case "get_diagnostic":
                    return executeGetDiagnostic();

                default:
                    return Map.of("success", false, "error", "未知工具: " + toolName);
            }
        } catch (Exception e) {
            log.error("工具执行失败: {}", toolName, e);
            return Map.of("success", false, "error", "执行失败: " + e.getMessage());
        }
    }

    // ===== Agent 管理 =====

    private Map<String, Object> executeListAgents() {
        var agents = agentManager.getAllAgents();
        List<Map<String, Object>> list = new ArrayList<>();
        for (var agent : agents) {
            Map<String, Object> info = new HashMap<>();
            info.put("id", agent.getId());
            info.put("name", agent.getName());
            info.put("role", agent.getRole());
            info.put("alive", agent.isAlive());
            info.put("busy", agent.isBusy());
            list.add(info);
        }
        return Map.of("success", true, "agents", list, "total", list.size());
    }

    private Map<String, Object> executeSendAgentTask(Map<String, Object> params) {
        String agentId = (String) params.get("agentId");
        String task = (String) params.get("task");
        try {
            // 通过干预服务发送任务指令
            User adminUser = userService.getUserByUsername("admin");
            Long userId = adminUser != null ? adminUser.getId() : 0L;
            var result = interventionService.sendInstruction(userId, "AI助手", "ADMIN",
                agentId, task, "AI助手分配任务");
            return Map.of("success", true, "message", "任务已发送给 " + agentId,
                "interventionId", result.getId());
        } catch (Exception e) {
            return Map.of("success", false, "error", "发送任务失败: " + e.getMessage());
        }
    }

    private Map<String, Object> executeInterveneAgent(Map<String, Object> params, String username) {
        String agentId = (String) params.get("agentId");
        String instruction = (String) params.get("instruction");
        String reason = (String) params.getOrDefault("reason", "");
        try {
            User user = userService.getUserByUsername(username);
            Long userId = user != null ? user.getId() : 0L;
            String userRole = user != null && user.getRole() != null ? user.getRole().getName() : "USER";
            var result = interventionService.sendInstruction(userId, username, userRole, agentId, instruction, reason);
            return Map.of("success", true, "message", "干预指令已发送给 " + agentId, "interventionId", result.getId());
        } catch (Exception e) {
            return Map.of("success", false, "error", "干预失败: " + e.getMessage());
        }
    }

    private Map<String, Object> executePauseAgent(Map<String, Object> params) {
        String agentId = (String) params.get("agentId");
        String reason = (String) params.getOrDefault("reason", "管理员暂停");
        try {
            // 使用默认管理员身份暂停
            interventionService.pauseAgent(0L, "system", "ADMIN", agentId, reason);
            return Map.of("success", true, "message", "Agent已暂停: " + agentId);
        } catch (Exception e) {
            return Map.of("success", false, "error", "暂停失败: " + e.getMessage());
        }
    }

    private Map<String, Object> executeResumeAgent(Map<String, Object> params) {
        String agentId = (String) params.get("agentId");
        try {
            interventionService.resumeAgent(0L, "system", "ADMIN", agentId, "恢复运行");
            return Map.of("success", true, "message", "Agent已恢复: " + agentId);
        } catch (Exception e) {
            return Map.of("success", false, "error", "恢复失败: " + e.getMessage());
        }
    }

    private Map<String, Object> executeGetAgentHealth() {
        try {
            var healthList = agentHealthService.getAllAgentHealth();
            return Map.of("success", true, "health", healthList, "total", healthList.size());
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private Map<String, Object> executeGetAgentLogs(Map<String, Object> params) {
        String agentId = (String) params.get("agentId");
        String level = (String) params.getOrDefault("level", null);
        int limit = params.containsKey("limit") ? ((Number) params.get("limit")).intValue() : 20;
        try {
            var page = agentLogService.searchLogs(agentId, null, level, null, null, null, 0, limit);
            return Map.of("success", true, "logs", page.getContent(), "total", page.getTotalElements());
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    // ===== 项目管理 =====

    private Map<String, Object> executeListProjects() {
        var projects = projectManager.getAllProjects();
        List<Map<String, Object>> list = new ArrayList<>();
        for (var project : projects) {
            Map<String, Object> info = new HashMap<>();
            info.put("id", project.getId());
            info.put("name", project.getName());
            info.put("status", project.getStatus());
            info.put("goal", project.getGoal());
            list.add(info);
        }
        return Map.of("success", true, "projects", list, "total", list.size());
    }

    private Map<String, Object> executeCreateProject(Map<String, Object> params) {
        String name = (String) params.get("name");
        String description = (String) params.getOrDefault("description", "");
        String workDir = (String) params.getOrDefault("workDir", "/opt/gamemaker/projects/" + name);
        try {
            var project = projectManager.createProject(name, description, workDir);
            return Map.of("success", true, "message", "项目创建成功: " + name, "projectId", project.getId());
        } catch (Exception e) {
            return Map.of("success", false, "error", "创建项目失败: " + e.getMessage());
        }
    }

    private Map<String, Object> executeCreateProjectFromTemplate(Map<String, Object> params) {
        String templateId = (String) params.get("templateId");
        String projectName = (String) params.get("projectName");
        String description = (String) params.getOrDefault("description", "");
        try {
            // 从游戏模板获取配置，然后创建项目
            var template = gameTemplateService.getTemplate(templateId);
            if (template == null) {
                return Map.of("success", false, "error", "模板不存在: " + templateId);
            }
            String workDir = "/opt/gamemaker/projects/" + projectName;
            var project = projectManager.createProject(projectName, description, workDir, templateId);
            return Map.of("success", true, "message", "项目创建成功: " + projectName + " (模板: " + template.getName() + ")",
                "projectId", project.getId());
        } catch (Exception e) {
            return Map.of("success", false, "error", "从模板创建项目失败: " + e.getMessage());
        }
    }

    // ===== 工作流管理 =====

    private Map<String, Object> executeListWorkflowTemplates() {
        var templates = workflowEngine.getAllTemplates();
        List<Map<String, Object>> list = new ArrayList<>();
        for (var tpl : templates) {
            Map<String, Object> info = new HashMap<>();
            info.put("id", tpl.getId());
            info.put("name", tpl.getName());
            info.put("description", tpl.getDescription());
            info.put("stepCount", tpl.getSteps() != null ? tpl.getSteps().size() : 0);
            list.add(info);
        }
        return Map.of("success", true, "templates", list, "total", list.size());
    }

    private Map<String, Object> executeCreateWorkflowTemplate(Map<String, Object> params) {
        String id = (String) params.get("id");
        String name = (String) params.get("name");
        String description = (String) params.getOrDefault("description", "");
        try {
            var template = workflowEngine.createTemplate(id, name, description, null);
            return Map.of("success", true, "message", "工作流模板创建成功: " + name, "templateId", template.getId());
        } catch (Exception e) {
            return Map.of("success", false, "error", "创建工作流模板失败: " + e.getMessage());
        }
    }

    private Map<String, Object> executeDeleteWorkflowTemplate(Map<String, Object> params) {
        String templateId = (String) params.get("templateId");
        try {
            boolean deleted = workflowEngine.deleteTemplate(templateId);
            if (deleted) {
                return Map.of("success", true, "message", "工作流模板已删除: " + templateId);
            } else {
                return Map.of("success", false, "error", "模板不存在或为内置模板，无法删除");
            }
        } catch (Exception e) {
            return Map.of("success", false, "error", "删除失败: " + e.getMessage());
        }
    }

    private Map<String, Object> executeStartWorkflow(Map<String, Object> params) {
        String templateId = (String) params.get("templateId");
        String projectId = (String) params.get("projectId");
        @SuppressWarnings("unchecked")
        Map<String, String> parameters = (Map<String, String>) params.getOrDefault("parameters", Map.of());
        try {
            var instance = workflowEngine.startWorkflow(templateId, projectId, parameters);
            return Map.of("success", true, "message", "工作流已启动", "instanceId", instance.getId(),
                "templateId", templateId, "projectId", projectId);
        } catch (Exception e) {
            return Map.of("success", false, "error", "启动工作流失败: " + e.getMessage());
        }
    }

    private Map<String, Object> executeListWorkflowInstances() {
        var instances = workflowEngine.getRunningInstances();
        List<Map<String, Object>> list = new ArrayList<>();
        for (var inst : instances) {
            Map<String, Object> info = new HashMap<>();
            info.put("id", inst.getId());
            info.put("templateId", inst.getTemplateId());
            info.put("projectId", inst.getProjectId());
            info.put("status", inst.getStatus().name());
            info.put("createdAt", inst.getCreatedAt() != null ? inst.getCreatedAt().toString() : null);
            list.add(info);
        }
        return Map.of("success", true, "instances", list, "total", list.size());
    }

    private Map<String, Object> executeCancelWorkflow(Map<String, Object> params) {
        String instanceId = (String) params.get("instanceId");
        try {
            workflowEngine.cancelWorkflow(instanceId);
            return Map.of("success", true, "message", "工作流已取消: " + instanceId);
        } catch (Exception e) {
            return Map.of("success", false, "error", "取消失败: " + e.getMessage());
        }
    }

    private Map<String, Object> executePauseWorkflow(Map<String, Object> params) {
        String instanceId = (String) params.get("instanceId");
        try {
            workflowEngine.pauseWorkflow(instanceId);
            return Map.of("success", true, "message", "工作流已暂停: " + instanceId);
        } catch (Exception e) {
            return Map.of("success", false, "error", "暂停失败: " + e.getMessage());
        }
    }

    private Map<String, Object> executeResumeWorkflow(Map<String, Object> params) {
        String instanceId = (String) params.get("instanceId");
        try {
            workflowEngine.resumeWorkflow(instanceId);
            return Map.of("success", true, "message", "工作流已恢复: " + instanceId);
        } catch (Exception e) {
            return Map.of("success", false, "error", "恢复失败: " + e.getMessage());
        }
    }

    // ===== 游戏模板 =====

    private Map<String, Object> executeListGameTemplates() {
        try {
            var templates = gameTemplateService.getAllTemplates();
            List<Map<String, Object>> list = new ArrayList<>();
            for (var tpl : templates) {
                Map<String, Object> info = new HashMap<>();
                info.put("id", tpl.getId());
                info.put("name", tpl.getName());
                info.put("description", tpl.getDescription());
                info.put("keywords", tpl.getKeywords());
                list.add(info);
            }
            return Map.of("success", true, "templates", list, "total", list.size());
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private Map<String, Object> executeCreateGameTemplate(Map<String, Object> params) {
        String name = (String) params.get("name");
        String description = (String) params.getOrDefault("description", "");
        String gameType = (String) params.getOrDefault("gameType", "general");
        try {
            String id = "custom-" + System.currentTimeMillis();
            var template = gameTemplateService.createTemplate(id, name, description, List.of(gameType), gameType, description);
            return Map.of("success", true, "message", "游戏模板创建成功: " + name, "templateId", template.getId());
        } catch (Exception e) {
            return Map.of("success", false, "error", "创建游戏模板失败: " + e.getMessage());
        }
    }

    private Map<String, Object> executeDeleteGameTemplate(Map<String, Object> params) {
        String templateId = (String) params.get("templateId");
        try {
            boolean deleted = gameTemplateService.deleteTemplate(templateId);
            if (deleted) {
                return Map.of("success", true, "message", "游戏模板已删除: " + templateId);
            } else {
                return Map.of("success", false, "error", "模板不存在或为内置模板，无法删除");
            }
        } catch (Exception e) {
            return Map.of("success", false, "error", "删除失败: " + e.getMessage());
        }
    }

    // ===== 技能管理 =====

    private Map<String, Object> executeListSkills() {
        var skills = skillManager.getAllGlobalSkills();
        List<Map<String, Object>> list = new ArrayList<>();
        for (var skill : skills) {
            Map<String, Object> info = new HashMap<>();
            info.put("id", skill.getId());
            info.put("name", skill.getName());
            info.put("description", skill.getDescription());
            info.put("category", skill.getCategory());
            list.add(info);
        }
        return Map.of("success", true, "skills", list, "total", list.size());
    }

    private Map<String, Object> executeCreateSkill(Map<String, Object> params) {
        String name = (String) params.get("name");
        String description = (String) params.get("description");
        String prompt = (String) params.get("prompt");
        String category = (String) params.getOrDefault("category", "custom");
        String triggerPattern = (String) params.getOrDefault("triggerPattern", name);

        Skill skill = Skill.builder()
            .id("custom-" + System.currentTimeMillis())
            .name(name)
            .description(description)
            .category(category)
            .triggerPattern(triggerPattern)
            .prompt(prompt)
            .build();

        skillManager.registerGlobalSkill(skill);
        return Map.of("success", true, "message", "技能创建成功: " + name, "skillId", skill.getId());
    }

    // ===== MCP 服务 =====

    private Map<String, Object> executeListMcpServers() {
        try {
            var servers = mcpService.getAllServers();
            return Map.of("success", true, "servers", servers, "total", servers.size());
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private Map<String, Object> executeAddMcpServer(Map<String, Object> params) {
        String name = (String) params.get("name");
        String command = (String) params.get("command");
        String args = (String) params.getOrDefault("args", "[]");
        String projectId = (String) params.get("projectId");
        try {
            McpServer server = new McpServer();
            server.setName(name);
            server.setCommand(command);
            server.setArgs(args);
            server.setProjectId(projectId);
            server.setEnabled(true);
            var saved = mcpService.saveServer(server);
            return Map.of("success", true, "message", "MCP服务器添加成功: " + name, "serverId", saved.getId());
        } catch (Exception e) {
            return Map.of("success", false, "error", "添加MCP服务器失败: " + e.getMessage());
        }
    }

    private Map<String, Object> executeTestMcpServer(Map<String, Object> params) {
        String serverId = (String) params.get("serverId");
        try {
            var result = mcpService.testAndDiscover(Long.parseLong(serverId));
            return Map.of("success", true, "result", result);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    // ===== 系统监控 =====

    private Map<String, Object> executeListAlerts() {
        try {
            var alerts = alertService.getAllAlerts(0, 20);
            return Map.of("success", true, "alerts", alerts.getContent(), "total", alerts.getTotalElements());
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private Map<String, Object> executeAcknowledgeAlert(Map<String, Object> params, String username) {
        Long alertId = ((Number) params.get("alertId")).longValue();
        try {
            alertService.acknowledgeAlert(alertId, username);
            return Map.of("success", true, "message", "告警已确认: " + alertId);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private Map<String, Object> executeResolveAlert(Map<String, Object> params, String username) {
        Long alertId = ((Number) params.get("alertId")).longValue();
        String resolution = (String) params.getOrDefault("resolution", "");
        try {
            alertService.resolveAlert(alertId, username, resolution);
            return Map.of("success", true, "message", "告警已解决: " + alertId);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private Map<String, Object> executeListNotifications(String username) {
        try {
            User user = userService.getUserByUsername(username);
            if (user == null) {
                return Map.of("success", false, "error", "用户不存在");
            }
            var notifications = notificationService.getUserNotifications(user.getId(), 0, 20);
            return Map.of("success", true, "notifications", notifications.getContent(), "total", notifications.getTotalElements());
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private Map<String, Object> executeMarkNotificationRead(Map<String, Object> params) {
        Long notificationId = ((Number) params.get("notificationId")).longValue();
        try {
            notificationService.markAsRead(notificationId, null);
            return Map.of("success", true, "message", "通知已标记为已读");
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private Map<String, Object> executeMarkAllNotificationsRead(String username) {
        try {
            User user = userService.getUserByUsername(username);
            if (user == null) {
                return Map.of("success", false, "error", "用户不存在");
            }
            notificationService.markAllAsRead(user.getId());
            return Map.of("success", true, "message", "所有通知已标记为已读");
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private Map<String, Object> executeGetResourceUsage() {
        try {
            Runtime runtime = Runtime.getRuntime();
            Map<String, Object> usage = new HashMap<>();
            usage.put("totalMemoryMB", runtime.totalMemory() / 1024 / 1024);
            usage.put("freeMemoryMB", runtime.freeMemory() / 1024 / 1024);
            usage.put("usedMemoryMB", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);
            usage.put("maxMemoryMB", runtime.maxMemory() / 1024 / 1024);
            usage.put("availableProcessors", runtime.availableProcessors());
            return Map.of("success", true, "usage", usage);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private Map<String, Object> executeGetOperationLogs() {
        try {
            var logs = operationLogService.getRecentLogs();
            return Map.of("success", true, "logs", logs, "total", logs.size());
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    // ===== 配置管理 =====

    private Map<String, Object> executeListConfigs() {
        try {
            var configs = configService.getAllConfigs();
            return Map.of("success", true, "configs", configs, "total", configs.size());
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private Map<String, Object> executeUpdateConfig(Map<String, Object> params) {
        String key = (String) params.get("key");
        String value = (String) params.get("value");
        try {
            configService.setConfig(key, value);
            return Map.of("success", true, "message", "配置已更新: " + key);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private Map<String, Object> executeListTokens() {
        try {
            var tokens = tokenService.getAllTokens();
            return Map.of("success", true, "tokens", tokens, "total", tokens.size());
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private Map<String, Object> executeListUsers() {
        try {
            var users = userService.getAllUsers();
            List<Map<String, Object>> list = new ArrayList<>();
            for (var user : users) {
                Map<String, Object> info = new HashMap<>();
                info.put("id", user.getId());
                info.put("username", user.getUsername());
                info.put("nickname", user.getNickname());
                info.put("role", user.getRole() != null ? user.getRole().getName() : "无");
                info.put("status", user.getStatus());
                list.add(info);
            }
            return Map.of("success", true, "users", list, "total", list.size());
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private Map<String, Object> executeListRoles() {
        try {
            var roles = roleService.getAllRoles();
            return Map.of("success", true, "roles", roles, "total", roles.size());
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    // ===== CI/CD =====

    private Map<String, Object> executeListPipelines() {
        try {
            var pipelines = pipelineService.getAllPipelines();
            List<Map<String, Object>> list = new ArrayList<>();
            for (var p : pipelines) {
                Map<String, Object> info = new HashMap<>();
                info.put("id", p.getId());
                info.put("name", p.getName());
                info.put("status", p.getStatus());
                info.put("projectId", p.getProjectId());
                info.put("progress", p.getProgress());
                list.add(info);
            }
            return Map.of("success", true, "pipelines", list, "total", list.size());
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private Map<String, Object> executeTriggerPipeline(Map<String, Object> params) {
        Long pipelineId = ((Number) params.get("pipelineId")).longValue();
        try {
            var pipeline = pipelineService.triggerPipeline(pipelineId, 0L, "AI助手", "MANUAL");
            return Map.of("success", true, "message", "流水线已触发: " + pipeline.getName(), "pipelineId", pipeline.getId());
        } catch (Exception e) {
            return Map.of("success", false, "error", "触发流水线失败: " + e.getMessage());
        }
    }

    private Map<String, Object> executeListGitRepos() {
        try {
            var repos = gitRepositoryService.getProjectRepositories("all");
            List<Map<String, Object>> list = new ArrayList<>();
            for (var repo : repos) {
                Map<String, Object> info = new HashMap<>();
                info.put("id", repo.getId());
                info.put("name", repo.getName());
                info.put("remoteUrl", repo.getRemoteUrl());
                info.put("projectId", repo.getProjectId());
                info.put("branch", repo.getCurrentBranch());
                info.put("status", repo.getStatus());
                list.add(info);
            }
            return Map.of("success", true, "repos", list, "total", list.size());
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private Map<String, Object> executeListReviews() {
        try {
            var reviews = codeReviewService.getAllReviews(0, 20);
            return Map.of("success", true, "reviews", reviews.getContent(), "total", reviews.getTotalElements());
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    // ===== 能力管理 =====

    private Map<String, Object> executeListCapabilities() {
        var capabilities = capabilityRegistry.getAllCapabilities();
        List<Map<String, Object>> list = new ArrayList<>();
        for (var cap : capabilities) {
            Map<String, Object> info = new HashMap<>();
            info.put("id", cap.getId());
            info.put("capabilityName", cap.getCapabilityName());
            info.put("displayName", cap.getDisplayName());
            info.put("agentRole", cap.getAgentRole());
            info.put("enabled", cap.isEnabled());
            list.add(info);
        }
        return Map.of("success", true, "capabilities", list, "total", list.size());
    }

    private Map<String, Object> executeCreateCapability(Map<String, Object> params) {
        try {
            // 能力通过数据库直接管理，此处返回指引
            return Map.of("success", true, "message",
                "能力定义需要通过数据库或管理界面创建。能力名: " + params.get("capabilityName")
                + ", 适用角色: " + params.get("agentRole"));
        } catch (Exception e) {
            return Map.of("success", false, "error", "创建能力失败: " + e.getMessage());
        }
    }

    // ===== 系统信息 =====

    private Map<String, Object> executeGetSystemInfo() {
        try {
            Runtime runtime = Runtime.getRuntime();
            Map<String, Object> info = new HashMap<>();
            info.put("javaVersion", System.getProperty("java.version"));
            info.put("osName", System.getProperty("os.name"));
            info.put("osArch", System.getProperty("os.arch"));
            info.put("availableProcessors", runtime.availableProcessors());
            info.put("totalMemoryMB", runtime.totalMemory() / 1024 / 1024);
            info.put("maxMemoryMB", runtime.maxMemory() / 1024 / 1024);
            info.put("agentCount", agentManager.getAllAgents().size());
            info.put("projectCount", projectManager.getAllProjects().size());
            return Map.of("success", true, "system", info);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private Map<String, Object> executeGetDiagnostic() {
        try {
            Map<String, Object> diagnostic = new HashMap<>();
            diagnostic.put("agents", agentManager.getAllAgents().size());
            diagnostic.put("projects", projectManager.getAllProjects().size());
            diagnostic.put("skills", skillManager.getAllGlobalSkills().size());
            diagnostic.put("workflows", workflowEngine.getRunningInstances().size());
            return Map.of("success", true, "diagnostic", diagnostic);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }
}
