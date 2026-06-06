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

        // ===== 工作流管理 =====

        // 31. 查询工作流模板
        registerTool(new AiTool(
            "list_workflow_templates",
            "获取所有工作流模板列表（包括内置和自定义模板）",
            Map.of(),
            "PERM_workflow:view"
        ));

        // 32. 创建工作流模板
        Map<String, ParameterDef> createWfParams = new HashMap<>();
        createWfParams.put("id", new ParameterDef("string", "模板ID（英文标识，如 my-workflow）", true));
        createWfParams.put("name", new ParameterDef("string", "模板名称", true));
        createWfParams.put("description", new ParameterDef("string", "模板描述", false));
        registerTool(new AiTool(
            "create_workflow_template",
            "创建新的工作流模板",
            createWfParams,
            "PERM_workflow:manage"
        ));

        // 33. 删除工作流模板
        Map<String, ParameterDef> deleteWfParams = new HashMap<>();
        deleteWfParams.put("templateId", new ParameterDef("string", "要删除的模板ID", true));
        registerTool(new AiTool(
            "delete_workflow_template",
            "删除自定义工作流模板（内置模板无法删除）",
            deleteWfParams,
            "PERM_workflow:manage"
        ));

        // 34. 启动工作流
        Map<String, ParameterDef> startWfParams = new HashMap<>();
        startWfParams.put("templateId", new ParameterDef("string", "工作流模板ID", true));
        startWfParams.put("projectId", new ParameterDef("string", "关联的项目ID", true));
        startWfParams.put("parameters", new ParameterDef("object", "工作流参数（JSON对象）", false));
        registerTool(new AiTool(
            "start_workflow",
            "使用指定模板启动工作流实例",
            startWfParams,
            "PERM_workflow:manage"
        ));

        // 35. 查询运行中的工作流
        registerTool(new AiTool(
            "list_workflow_instances",
            "获取所有运行中的工作流实例",
            Map.of(),
            "PERM_workflow:view"
        ));

        // 36. 取消工作流
        Map<String, ParameterDef> cancelWfParams = new HashMap<>();
        cancelWfParams.put("instanceId", new ParameterDef("string", "工作流实例ID", true));
        registerTool(new AiTool(
            "cancel_workflow",
            "取消运行中的工作流实例",
            cancelWfParams,
            "PERM_workflow:manage"
        ));

        // 37. 暂停工作流
        Map<String, ParameterDef> pauseWfParams = new HashMap<>();
        pauseWfParams.put("instanceId", new ParameterDef("string", "工作流实例ID", true));
        registerTool(new AiTool(
            "pause_workflow",
            "暂停运行中的工作流实例",
            pauseWfParams,
            "PERM_workflow:manage"
        ));

        // 38. 恢复工作流
        Map<String, ParameterDef> resumeWfParams = new HashMap<>();
        resumeWfParams.put("instanceId", new ParameterDef("string", "工作流实例ID", true));
        registerTool(new AiTool(
            "resume_workflow",
            "恢复暂停的工作流实例",
            resumeWfParams,
            "PERM_workflow:manage"
        ));

        // ===== 游戏模板管理 =====

        // 39. 创建游戏模板
        Map<String, ParameterDef> createGameTplParams = new HashMap<>();
        createGameTplParams.put("name", new ParameterDef("string", "模板名称", true));
        createGameTplParams.put("description", new ParameterDef("string", "模板描述", false));
        createGameTplParams.put("gameType", new ParameterDef("string", "游戏类型（如 rpg、slg、casual）", false));
        registerTool(new AiTool(
            "create_game_template",
            "创建新的游戏模板",
            createGameTplParams,
            "PERM_skills:manage"
        ));

        // 40. 删除游戏模板
        Map<String, ParameterDef> deleteGameTplParams = new HashMap<>();
        deleteGameTplParams.put("templateId", new ParameterDef("string", "要删除的模板ID", true));
        registerTool(new AiTool(
            "delete_game_template",
            "删除自定义游戏模板（内置模板无法删除）",
            deleteGameTplParams,
            "PERM_skills:manage"
        ));

        // ===== 告警管理 =====

        // 41. 确认告警
        Map<String, ParameterDef> ackAlertParams = new HashMap<>();
        ackAlertParams.put("alertId", new ParameterDef("number", "告警ID", true));
        registerTool(new AiTool(
            "acknowledge_alert",
            "确认告警（表示已知晓）",
            ackAlertParams,
            "PERM_system:monitor"
        ));

        // 42. 解决告警
        Map<String, ParameterDef> resolveAlertParams = new HashMap<>();
        resolveAlertParams.put("alertId", new ParameterDef("number", "告警ID", true));
        resolveAlertParams.put("resolution", new ParameterDef("string", "解决方案说明", false));
        registerTool(new AiTool(
            "resolve_alert",
            "标记告警为已解决",
            resolveAlertParams,
            "PERM_system:monitor"
        ));

        // ===== 通知管理 =====

        // 43. 标记通知已读
        Map<String, ParameterDef> readNotifParams = new HashMap<>();
        readNotifParams.put("notificationId", new ParameterDef("number", "通知ID", true));
        registerTool(new AiTool(
            "mark_notification_read",
            "标记单条通知为已读",
            readNotifParams,
            "PERM_notification:view"
        ));

        // 44. 全部标记已读
        registerTool(new AiTool(
            "mark_all_notifications_read",
            "标记所有通知为已读",
            Map.of(),
            "PERM_notification:view"
        ));

        // ===== 配置管理 =====

        // 45. 更新系统配置
        Map<String, ParameterDef> updateConfigParams = new HashMap<>();
        updateConfigParams.put("key", new ParameterDef("string", "配置键", true));
        updateConfigParams.put("value", new ParameterDef("string", "配置值", true));
        registerTool(new AiTool(
            "update_config",
            "更新系统配置项",
            updateConfigParams,
            "PERM_system:config:manage"
        ));

        // ===== CI/CD =====

        // 46. 触发流水线
        Map<String, ParameterDef> triggerPipelineParams = new HashMap<>();
        triggerPipelineParams.put("pipelineId", new ParameterDef("number", "流水线ID", true));
        registerTool(new AiTool(
            "trigger_pipeline",
            "触发CI/CD流水线执行",
            triggerPipelineParams,
            "PERM_pipeline:execute"
        ));

        // ===== 系统信息 =====

        // 47. 获取系统信息
        registerTool(new AiTool(
            "get_system_info",
            "获取系统运行环境信息（Java版本、OS、内存、CPU等）",
            Map.of(),
            null
        ));

        // 48. 获取系统自检
        registerTool(new AiTool(
            "get_diagnostic",
            "获取系统自检结果（Agent、项目、技能、工作流统计）",
            Map.of(),
            "PERM_system:monitor"
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
        sb.append("## 可用 API（使用 curl 调用）\n\n");
        sb.append("⚠️ **重要**：所有操作都必须通过 API 完成，不要尝试修改代码文件。\n");
        sb.append("⚠️ **你拥有完整的系统访问能力**：所有 API 都可以直接调用获取实时数据，不需要查看代码。\n\n");
        sb.append("API 调用方式：使用 curl 调用 http://127.0.0.1:19922/api/...\n\n");
        sb.append("示例：\n");
        sb.append("```bash\ncurl -s http://127.0.0.1:19922/api/workflow-templates\n```\n");
        sb.append("```bash\ncurl -s http://127.0.0.1:19922/api/agents\n```\n");
        sb.append("```bash\ncurl -s http://127.0.0.1:19922/api/projects\n```\n\n");

        // 按类别分组
        sb.append("### 项目管理\n");
        sb.append("- 查询项目：`curl -s http://127.0.0.1:19922/api/projects`\n");
        sb.append("- 创建项目：`curl -X POST http://127.0.0.1:19922/api/projects -H 'Content-Type: application/json' -d '{...}'`\n\n");

        sb.append("### 工作流管理\n");
        sb.append("- 查询工作流模板：`curl -s http://127.0.0.1:19922/api/workflow-templates`\n");
        sb.append("- 创建工作流模板：`curl -X POST http://127.0.0.1:19922/api/workflow-templates -H 'Content-Type: application/json' -d '{...}'`\n");
        sb.append("- 删除工作流模板：`curl -X DELETE http://127.0.0.1:19922/api/workflow-templates/{id}`\n");
        sb.append("- 启动工作流：`curl -X POST http://127.0.0.1:19922/api/workflows/start -H 'Content-Type: application/json' -d '{...}'`\n");
        sb.append("- 查询工作流实例：`curl -s http://127.0.0.1:19922/api/workflows/instances`\n\n");

        sb.append("### Agent 管理\n");
        sb.append("- 查询 Agent 列表：`curl -s http://127.0.0.1:19922/api/agents`\n");
        sb.append("- 发送 Agent 任务：`curl -X POST http://127.0.0.1:19922/api/agents/{id}/task -H 'Content-Type: application/json' -d '{...}'`\n");
        sb.append("- 干预 Agent：`curl -X POST http://127.0.0.1:19922/api/agents/{id}/intervene -H 'Content-Type: application/json' -d '{...}'`\n");
        sb.append("- 暂停 Agent：`curl -X POST http://127.0.0.1:19922/api/agents/{id}/pause`\n");
        sb.append("- 恢复 Agent：`curl -X POST http://127.0.0.1:19922/api/agents/{id}/resume`\n\n");

        sb.append("### 游戏模板\n");
        sb.append("- 查询游戏模板：`curl -s http://127.0.0.1:19922/api/game-templates`\n");
        sb.append("- 创建游戏模板：`curl -X POST http://127.0.0.1:19922/api/game-templates -H 'Content-Type: application/json' -d '{...}'`\n");
        sb.append("- 删除游戏模板：`curl -X DELETE http://127.0.0.1:19922/api/game-templates/{id}`\n\n");

        sb.append("### 技能管理\n");
        appendToolByNames(sb, userPermissions, "list_skills", "create_skill");

        sb.append("### 系统监控\n");
        appendToolByNames(sb, userPermissions, "list_alerts", "acknowledge_alert", "resolve_alert",
            "get_resource_usage", "get_system_info", "get_diagnostic");

        sb.append("### 配置管理\n");
        appendToolByNames(sb, userPermissions, "list_configs", "update_config", "list_tokens", "list_users", "list_roles");

        sb.append("### 通用API调用\n");
        appendToolByNames(sb, userPermissions, "call_api");

        sb.append("### 代码审查 & CI/CD\n");
        appendToolByNames(sb, userPermissions, "list_reviews", "list_pipelines", "trigger_pipeline", "list_git_repos");

        return sb.toString();
    }

    /**
     * 按名称追加工具说明
     */
    private void appendToolByNames(StringBuilder sb, Set<String> userPermissions, String... toolNames) {
        for (String name : toolNames) {
            AiTool tool = tools.get(name);
            if (tool == null) continue;
            if (tool.getPermission() != null && !userPermissions.contains(tool.getPermission())) continue;

            sb.append("- **`").append(name).append("`**：").append(tool.getDescription());
            if (tool.getParameters() != null && !tool.getParameters().isEmpty()) {
                sb.append(" | 参数: ");
                boolean first = true;
                for (Map.Entry<String, ParameterDef> entry : tool.getParameters().entrySet()) {
                    if (!first) sb.append(", ");
                    sb.append(entry.getKey());
                    if (entry.getValue().isRequired()) sb.append("(必填)");
                    first = false;
                }
            }
            sb.append("\n");
        }
        sb.append("\n");
    }
}
