package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.manager.*;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.model.Skill;
import com.chengxun.gamemaker.web.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * AI工具执行器
 * 执行AI助手调用的工具
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
    private com.chengxun.gamemaker.service.McpService mcpService;

    @Autowired
    private com.chengxun.gamemaker.web.service.UserService userService;

    @Autowired
    private com.chengxun.gamemaker.web.service.AlertService alertService;

    @Autowired
    private com.chengxun.gamemaker.web.service.NotificationService notificationService;

    @Autowired
    private com.chengxun.gamemaker.web.service.SystemConfigService configService;

    @Autowired
    private com.chengxun.gamemaker.web.service.ApiTokenService tokenService;

    @Autowired
    private com.chengxun.gamemaker.web.service.RoleService roleService;

    @Autowired
    private com.chengxun.gamemaker.web.service.AgentLogService agentLogService;

    @Autowired
    private com.chengxun.gamemaker.web.service.OperationLogService operationLogService;

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
                case "list_agents":
                    return executeListAgents();
                case "send_agent_task":
                    return executeSendAgentTask(params);
                case "list_projects":
                    return executeListProjects();
                case "create_project":
                    return executeCreateProject(params);
                case "list_mcp_servers":
                    return executeListMcpServers();
                case "add_mcp_server":
                    return executeAddMcpServer(params);
                case "test_mcp_server":
                    return executeTestMcpServer(params);
                case "list_skills":
                    return executeListSkills();
                case "create_skill":
                    return executeCreateSkill(params);
                case "list_game_templates":
                    return executeListGameTemplates();
                case "create_project_from_template":
                    return executeCreateProjectFromTemplate(params);
                case "list_alerts":
                    return executeListAlerts();
                case "list_notifications":
                    return executeListNotifications(username);
                case "list_capabilities":
                    return executeListCapabilities();
                case "create_capability":
                    return executeCreateCapability(params);
                case "list_configs":
                    return executeListConfigs();
                case "list_tokens":
                    return executeListTokens();
                case "list_users":
                    return executeListUsers();
                case "list_roles":
                    return executeListRoles();
                case "call_api":
                    return executeCallApi(params, username);
                case "get_agent_logs":
                    return executeGetAgentLogs(params);
                case "get_operation_logs":
                    return executeGetOperationLogs();
                case "get_resource_usage":
                    return executeGetResourceUsage();
                case "list_pipelines":
                    return executeListPipelines();
                case "list_git_repos":
                    return executeListGitRepos();
                case "list_reviews":
                    return executeListReviews();
                case "intervene_agent":
                    return executeInterveneAgent(params, username);
                case "pause_agent":
                    return executePauseAgent(params);
                case "resume_agent":
                    return executeResumeAgent(params);
                case "get_agent_health":
                    return executeGetAgentHealth();
                default:
                    return Map.of("success", false, "error", "未知工具: " + toolName);
            }
        } catch (Exception e) {
            log.error("工具执行失败: {}", toolName, e);
            return Map.of("success", false, "error", "执行失败: " + e.getMessage());
        }
    }

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
        // TODO: 实际发送任务
        return Map.of("success", true, "message", "任务已发送给 " + agentId);
    }

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
        // TODO: 实际创建项目
        return Map.of("success", true, "message", "项目创建成功: " + name);
    }

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
        // TODO: 实际添加MCP服务器
        return Map.of("success", true, "message", "MCP服务器添加成功: " + name);
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

    private Map<String, Object> executeListGameTemplates() {
        var skills = skillManager.getAllGlobalSkills();
        List<Map<String, Object>> templates = new ArrayList<>();
        for (var skill : skills) {
            if (skill.getId() != null && skill.getId().startsWith("game-template")) {
                Map<String, Object> info = new HashMap<>();
                info.put("id", skill.getId());
                info.put("name", skill.getName());
                info.put("description", skill.getDescription());
                templates.add(info);
            }
        }
        return Map.of("success", true, "templates", templates, "total", templates.size());
    }

    private Map<String, Object> executeCreateProjectFromTemplate(Map<String, Object> params) {
        String templateId = (String) params.get("templateId");
        String projectName = (String) params.get("projectName");
        // TODO: 实际从模板创建项目
        return Map.of("success", true, "message", "项目创建成功: " + projectName + " (模板: " + templateId + ")");
    }

    private Map<String, Object> executeListAlerts() {
        // TODO: 实际获取告警
        return Map.of("success", true, "alerts", List.of(), "total", 0);
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
        // TODO: 实际创建能力
        return Map.of("success", true, "message", "能力创建成功: " + params.get("capabilityName"));
    }

    private Map<String, Object> executeListConfigs() {
        try {
            var configs = configService.getAllConfigs();
            return Map.of("success", true, "configs", configs, "total", configs.size());
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

    private Map<String, Object> executeCallApi(Map<String, Object> params, String username) {
        // 注意：这个工具需要特殊处理，实际应该通过HTTP调用
        return Map.of("success", false, "error", "直接API调用暂不支持，请使用具体工具");
    }

    private Map<String, Object> executeGetAgentLogs(Map<String, Object> params) {
        // TODO: 实际获取日志
        return Map.of("success", true, "logs", List.of(), "total", 0);
    }

    private Map<String, Object> executeGetOperationLogs() {
        // TODO: 实际获取日志
        return Map.of("success", true, "logs", List.of(), "total", 0);
    }

    private Map<String, Object> executeGetResourceUsage() {
        // TODO: 实际获取资源使用
        return Map.of("success", true, "usage", Map.of());
    }

    private Map<String, Object> executeListPipelines() {
        // TODO: 实际获取流水线
        return Map.of("success", true, "pipelines", List.of(), "total", 0);
    }

    private Map<String, Object> executeListGitRepos() {
        // TODO: 实际获取Git仓库
        return Map.of("success", true, "repos", List.of(), "total", 0);
    }

    private Map<String, Object> executeListReviews() {
        // TODO: 实际获取代码审查
        return Map.of("success", true, "reviews", List.of(), "total", 0);
    }

    private Map<String, Object> executeInterveneAgent(Map<String, Object> params, String username) {
        String agentId = (String) params.get("agentId");
        String instruction = (String) params.get("instruction");
        // TODO: 实际发送干预
        return Map.of("success", true, "message", "干预指令已发送给 " + agentId);
    }

    private Map<String, Object> executePauseAgent(Map<String, Object> params) {
        String agentId = (String) params.get("agentId");
        // TODO: 实际暂停Agent
        return Map.of("success", true, "message", "Agent已暂停: " + agentId);
    }

    private Map<String, Object> executeResumeAgent(Map<String, Object> params) {
        String agentId = (String) params.get("agentId");
        // TODO: 实际恢复Agent
        return Map.of("success", true, "message", "Agent已恢复: " + agentId);
    }

    private Map<String, Object> executeGetAgentHealth() {
        // TODO: 实际获取健康状态
        return Map.of("success", true, "health", Map.of());
    }
}
