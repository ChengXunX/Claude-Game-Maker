package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.model.AiTool;
import com.chengxun.gamemaker.model.AiTool.ParameterDef;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * AI工具注册中心
 * 管理AI助手可以使用的所有工具
 *
 * @author chengxun
 * @since 1.0.0
 */
@Component
public class AiToolRegistry {

    private final Map<String, AiTool> tools = new LinkedHashMap<>();

    public AiToolRegistry() {
        registerDefaultTools();
    }

    /**
     * 注册默认工具
     */
    private void registerDefaultTools() {
        // 1. 查询Agent列表
        registerTool(new AiTool(
            "list_agents",
            "获取系统中所有Agent的列表和状态",
            Map.of(),
            "PERM_agents:view"
        ));

        // 2. 发送Agent任务
        Map<String, ParameterDef> sendTaskParams = new HashMap<>();
        sendTaskParams.put("agentId", new ParameterDef("string", "Agent ID", true));
        sendTaskParams.put("task", new ParameterDef("string", "任务内容", true));
        registerTool(new AiTool(
            "send_agent_task",
            "向指定Agent发送任务",
            sendTaskParams,
            "PERM_agents:task"
        ));

        // 3. 查询项目列表
        registerTool(new AiTool(
            "list_projects",
            "获取所有游戏项目列表",
            Map.of(),
            "PERM_projects:view"
        ));

        // 4. 创建游戏项目
        Map<String, ParameterDef> createProjectParams = new HashMap<>();
        createProjectParams.put("name", new ParameterDef("string", "项目名称", true));
        createProjectParams.put("description", new ParameterDef("string", "项目描述", false));
        createProjectParams.put("templateId", new ParameterDef("string", "游戏模板ID", false));
        registerTool(new AiTool(
            "create_project",
            "创建新的游戏项目",
            createProjectParams,
            "PERM_projects:manage"
        ));

        // 5. 查询MCP服务器
        registerTool(new AiTool(
            "list_mcp_servers",
            "获取所有MCP服务器列表",
            Map.of(),
            "PERM_agents:view"
        ));

        // 6. 添加MCP服务器
        Map<String, ParameterDef> addMcpParams = new HashMap<>();
        addMcpParams.put("name", new ParameterDef("string", "服务器名称", true));
        addMcpParams.put("command", new ParameterDef("string", "启动命令", true));
        addMcpParams.put("args", new ParameterDef("string", "命令参数（JSON数组格式）", false));
        addMcpParams.put("projectId", new ParameterDef("string", "关联项目ID", false));
        registerTool(new AiTool(
            "add_mcp_server",
            "添加新的MCP服务器",
            addMcpParams,
            "PERM_agents:manage"
        ));

        // 7. 测试MCP服务器
        Map<String, ParameterDef> testMcpParams = new HashMap<>();
        testMcpParams.put("serverId", new ParameterDef("string", "MCP服务器ID", true));
        registerTool(new AiTool(
            "test_mcp_server",
            "测试MCP服务器连接",
            testMcpParams,
            "PERM_agents:view"
        ));

        // 8. 查询技能列表
        registerTool(new AiTool(
            "list_skills",
            "获取所有可用技能列表",
            Map.of(),
            "PERM_skills:view"
        ));

        // 9. 创建技能
        Map<String, ParameterDef> createSkillParams = new HashMap<>();
        createSkillParams.put("name", new ParameterDef("string", "技能名称", true));
        createSkillParams.put("description", new ParameterDef("string", "技能描述", true));
        createSkillParams.put("category", new ParameterDef("string", "技能分类", false));
        createSkillParams.put("triggerPattern", new ParameterDef("string", "触发关键词", false));
        createSkillParams.put("prompt", new ParameterDef("string", "技能提示词内容", true));
        registerTool(new AiTool(
            "create_skill",
            "创建新的Agent技能",
            createSkillParams,
            "PERM_skills:manage"
        ));

        // 10. 查询游戏模板
        registerTool(new AiTool(
            "list_game_templates",
            "获取所有游戏开发模板",
            Map.of(),
            "PERM_skills:view"
        ));

        // 11. 使用模板创建项目
        Map<String, ParameterDef> createFromTemplateParams = new HashMap<>();
        createFromTemplateParams.put("templateId", new ParameterDef("string", "模板ID", true));
        createFromTemplateParams.put("projectName", new ParameterDef("string", "项目名称", true));
        createFromTemplateParams.put("description", new ParameterDef("string", "项目描述", false));
        registerTool(new AiTool(
            "create_project_from_template",
            "使用游戏模板创建项目",
            createFromTemplateParams,
            "PERM_projects:manage"
        ));

        // 12. 查询告警
        registerTool(new AiTool(
            "list_alerts",
            "获取系统告警列表",
            Map.of(),
            "PERM_system:monitor"
        ));

        // 13. 查询通知
        registerTool(new AiTool(
            "list_notifications",
            "获取当前用户的通知列表",
            Map.of(),
            "PERM_notification:view"
        ));

        // 14. 查询能力列表
        registerTool(new AiTool(
            "list_capabilities",
            "获取所有Agent能力定义",
            Map.of(),
            "PERM_agents:view"
        ));

        // 15. 创建能力
        Map<String, ParameterDef> createCapParams = new HashMap<>();
        createCapParams.put("capabilityName", new ParameterDef("string", "能力标识名", true));
        createCapParams.put("displayName", new ParameterDef("string", "显示名称", true));
        createCapParams.put("agentRole", new ParameterDef("string", "适用角色", true));
        createCapParams.put("executionType", new ParameterDef("string", "执行类型(java/prompt/message)", false));
        createCapParams.put("description", new ParameterDef("string", "能力描述", false));
        createCapParams.put("category", new ParameterDef("string", "分类(task/communication/monitoring/project/code/deploy)", false));
        registerTool(new AiTool(
            "create_capability",
            "创建新的Agent能力",
            createCapParams,
            "PERM_agents:manage"
        ));

        // 16. 查询系统配置
        registerTool(new AiTool(
            "list_configs",
            "获取系统配置列表",
            Map.of(),
            "PERM_system:view"
        ));

        // 17. 查询Token使用情况
        registerTool(new AiTool(
            "list_tokens",
            "获取API Token列表和使用情况",
            Map.of(),
            "PERM_tokens:view"
        ));

        // 18. 查询用户列表
        registerTool(new AiTool(
            "list_users",
            "获取系统用户列表",
            Map.of(),
            "PERM_users:manage"
        ));

        // 19. 查询角色列表
        registerTool(new AiTool(
            "list_roles",
            "获取系统角色列表",
            Map.of(),
            "PERM_roles:manage"
        ));

        // 20. 调用任意API
        Map<String, ParameterDef> callApiParams = new HashMap<>();
        callApiParams.put("method", new ParameterDef("string", "HTTP方法(GET/POST/PUT/DELETE)", true));
        callApiParams.put("path", new ParameterDef("string", "API路径", true));
        callApiParams.put("body", new ParameterDef("string", "请求体(JSON格式)", false));
        registerTool(new AiTool(
            "call_api",
            "调用系统API接口",
            callApiParams,
            null  // 需要根据具体API检查权限
        ));

        // 21. 查询Agent日志
        Map<String, ParameterDef> agentLogsParams = new HashMap<>();
        agentLogsParams.put("agentId", new ParameterDef("string", "Agent ID", false));
        agentLogsParams.put("level", new ParameterDef("string", "日志级别(INFO/WARN/ERROR)", false));
        agentLogsParams.put("limit", new ParameterDef("number", "返回条数", false));
        registerTool(new AiTool(
            "get_agent_logs",
            "获取Agent运行日志",
            agentLogsParams,
            "PERM_logs:view"
        ));

        // 22. 查询操作日志
        registerTool(new AiTool(
            "get_operation_logs",
            "获取系统操作日志",
            Map.of(),
            "PERM_logs:view"
        ));

        // 23. 查询资源使用
        registerTool(new AiTool(
            "get_resource_usage",
            "获取系统资源使用情况",
            Map.of(),
            "PERM_system:monitor"
        ));

        // 24. 查询流水线
        registerTool(new AiTool(
            "list_pipelines",
            "获取CI/CD流水线列表",
            Map.of(),
            "PERM_pipeline:view"
        ));

        // 25. 查询Git仓库
        registerTool(new AiTool(
            "list_git_repos",
            "获取Git仓库列表",
            Map.of(),
            "PERM_projects:view"
        ));

        // 26. 查询代码审查
        registerTool(new AiTool(
            "list_reviews",
            "获取代码审查记录",
            Map.of(),
            "PERM_code:review"
        ));

        // 27. Agent干预
        Map<String, ParameterDef> interveneParams = new HashMap<>();
        interveneParams.put("agentId", new ParameterDef("string", "Agent ID", true));
        interveneParams.put("instruction", new ParameterDef("string", "干预指令", true));
        interveneParams.put("reason", new ParameterDef("string", "干预原因", false));
        registerTool(new AiTool(
            "intervene_agent",
            "向Agent发送人工干预指令",
            interveneParams,
            "PERM_agents:manage"
        ));

        // 28. 暂停Agent
        Map<String, ParameterDef> pauseParams = new HashMap<>();
        pauseParams.put("agentId", new ParameterDef("string", "Agent ID", true));
        pauseParams.put("reason", new ParameterDef("string", "暂停原因", false));
        registerTool(new AiTool(
            "pause_agent",
            "暂停指定Agent",
            pauseParams,
            "PERM_agents:manage"
        ));

        // 29. 恢复Agent
        Map<String, ParameterDef> resumeParams = new HashMap<>();
        resumeParams.put("agentId", new ParameterDef("string", "Agent ID", true));
        registerTool(new AiTool(
            "resume_agent",
            "恢复指定Agent运行",
            resumeParams,
            "PERM_agents:manage"
        ));

        // 30. 查询Agent健康状态
        registerTool(new AiTool(
            "get_agent_health",
            "获取Agent健康状态",
            Map.of(),
            "PERM_agents:view"
        ));
    }

    /**
     * 注册工具
     */
    public void registerTool(AiTool tool) {
        tools.put(tool.getName(), tool);
    }

    /**
     * 获取工具
     */
    public AiTool getTool(String name) {
        return tools.get(name);
    }

    /**
     * 获取所有工具
     */
    public Collection<AiTool> getAllTools() {
        return tools.values();
    }

    /**
     * 获取用户可用的工具（根据权限过滤）
     */
    public List<AiTool> getAvailableTools(Set<String> userPermissions) {
        List<AiTool> available = new ArrayList<>();
        for (AiTool tool : tools.values()) {
            if (tool.getPermission() == null || userPermissions.contains(tool.getPermission())) {
                available.add(tool);
            }
        }
        return available;
    }

    /**
     * 生成工具说明文本（用于系统提示词）
     */
    public String generateToolDescriptions(Set<String> userPermissions) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 可用工具\n\n");
        sb.append("你可以使用以下工具来执行操作。使用格式：\n");
        sb.append("```tool_call\n{\"tool\": \"工具名\", \"params\": {参数}}\n```\n\n");

        for (AiTool tool : getAvailableTools(userPermissions)) {
            sb.append("### ").append(tool.getName()).append("\n");
            sb.append("**说明**: ").append(tool.getDescription()).append("\n");

            if (tool.getParameters() != null && !tool.getParameters().isEmpty()) {
                sb.append("**参数**:\n");
                for (Map.Entry<String, ParameterDef> entry : tool.getParameters().entrySet()) {
                    ParameterDef param = entry.getValue();
                    sb.append("- `").append(entry.getKey()).append("`");
                    sb.append(" (").append(param.getType()).append(")");
                    if (param.isRequired()) sb.append(" [必填]");
                    sb.append(": ").append(param.getDescription()).append("\n");
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
