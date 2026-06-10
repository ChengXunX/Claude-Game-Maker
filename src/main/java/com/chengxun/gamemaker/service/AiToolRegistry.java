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

        // ===== 文件管理 =====

        // 49. 搜索文件
        Map<String, ParameterDef> searchFilesParams = new HashMap<>();
        searchFilesParams.put("keyword", new ParameterDef("string", "搜索关键词（文件名）", true));
        searchFilesParams.put("agentId", new ParameterDef("string", "限定Agent（可选）", false));
        registerTool(new AiTool(
            "search_files",
            "搜索Agent文件（按文件名关键词）",
            searchFilesParams,
            "PERM_agents:view"
        ));

        // 50. 列出文件
        Map<String, ParameterDef> listFilesParams = new HashMap<>();
        listFilesParams.put("agentId", new ParameterDef("string", "Agent ID（可选，不填返回全部）", false));
        registerTool(new AiTool(
            "list_files",
            "列出Agent文件列表",
            listFilesParams,
            "PERM_agents:view"
        ));

        // 51. 获取文件存储用量
        Map<String, ParameterDef> fileUsageParams = new HashMap<>();
        fileUsageParams.put("agentId", new ParameterDef("string", "Agent ID", true));
        registerTool(new AiTool(
            "get_file_usage",
            "获取Agent的文件存储使用情况",
            fileUsageParams,
            "PERM_agents:view"
        ));

        // ===== Agent 招聘 =====

        // 52. 招聘Agent
        Map<String, ParameterDef> recruitParams = new HashMap<>();
        recruitParams.put("producerId", new ParameterDef("string", "制作人Agent ID", true));
        recruitParams.put("role", new ParameterDef("string", "Agent角色（如 server-dev, tester）", true));
        recruitParams.put("name", new ParameterDef("string", "Agent名称", false));
        registerTool(new AiTool(
            "recruit_agent",
            "招聘新的Agent到项目（需要指定制作人）",
            recruitParams,
            "PERM_agents:manage"
        ));

        // 53. 列出已招聘的Agent
        registerTool(new AiTool(
            "list_recruited_agents",
            "获取已招聘的Agent列表（含角色、状态、能力）",
            Map.of(),
            "PERM_agents:view"
        ));

        // 54. 删除Agent（非核心）
        Map<String, ParameterDef> deleteAgentParams = new HashMap<>();
        deleteAgentParams.put("agentId", new ParameterDef("string", "要删除的Agent ID", true));
        registerTool(new AiTool(
            "delete_agent",
            "删除非核心Agent（制作人不可删除）",
            deleteAgentParams,
            "PERM_agents:manage"
        ));

        // ===== 项目详情 =====

        // 55. 获取项目详情
        Map<String, ParameterDef> projectDetailParams = new HashMap<>();
        projectDetailParams.put("projectId", new ParameterDef("string", "项目ID", true));
        registerTool(new AiTool(
            "get_project_detail",
            "获取项目详细信息（工作目录、目标、Agent列表、状态）",
            projectDetailParams,
            "PERM_projects:view"
        ));

        // 56. 设置项目目标
        Map<String, ParameterDef> setGoalParams = new HashMap<>();
        setGoalParams.put("projectId", new ParameterDef("string", "项目ID", true));
        setGoalParams.put("goal", new ParameterDef("string", "项目目标描述", true));
        setGoalParams.put("goalType", new ParameterDef("string", "目标类型(GAME_DEVELOPMENT/FEATURE/BUG_FIX/REFACTOR/CUSTOM)", false));
        registerTool(new AiTool(
            "set_project_goal",
            "设置项目目标（会触发制作人分解任务）",
            setGoalParams,
            "PERM_projects:manage"
        ));

        // 57. 获取项目里程碑
        Map<String, ParameterDef> milestoneParams = new HashMap<>();
        milestoneParams.put("projectId", new ParameterDef("string", "项目ID", true));
        registerTool(new AiTool(
            "get_project_milestones",
            "获取项目的里程碑列表和进度",
            milestoneParams,
            "PERM_projects:view"
        ));

        // ===== 调度器 =====

        // 58. 获取调度器状态
        registerTool(new AiTool(
            "get_scheduler_status",
            "获取Agent调度器状态（运行中/暂停、任务队列）",
            Map.of(),
            "PERM_system:monitor"
        ));

        // 59. 触发调度
        registerTool(new AiTool(
            "trigger_schedule",
            "手动触发一次Agent调度（检查所有Agent是否有待处理任务）",
            Map.of(),
            "PERM_agents:manage"
        ));

        // ===== 诊断 =====

        // 60. 运行系统诊断
        registerTool(new AiTool(
            "run_diagnostic",
            "运行完整系统诊断（检查Agent健康、磁盘空间、数据库连接等）",
            Map.of(),
            "PERM_system:monitor"
        ));

        // 61. 快速自检
        registerTool(new AiTool(
            "quick_health_check",
            "快速自检（轻量级，检查核心服务是否正常）",
            Map.of(),
            null
        ));

        // ===== 代码浏览 =====

        // 62. 浏览项目代码
        Map<String, ParameterDef> browseCodeParams = new HashMap<>();
        browseCodeParams.put("projectId", new ParameterDef("string", "项目ID", true));
        browseCodeParams.put("path", new ParameterDef("string", "目录路径（默认根目录）", false));
        registerTool(new AiTool(
            "browse_code",
            "浏览项目的代码目录结构",
            browseCodeParams,
            "PERM_projects:view"
        ));

        // 63. 读取代码文件
        Map<String, ParameterDef> readCodeParams = new HashMap<>();
        readCodeParams.put("projectId", new ParameterDef("string", "项目ID", true));
        readCodeParams.put("path", new ParameterDef("string", "文件路径", true));
        registerTool(new AiTool(
            "read_code_file",
            "读取项目中的代码文件内容",
            readCodeParams,
            "PERM_projects:view"
        ));

        // ===== 通知模板 =====

        // 64. 列出通知模板
        registerTool(new AiTool(
            "list_notification_templates",
            "获取所有通知模板（邮件、飞书、钉钉等渠道）",
            Map.of(),
            "PERM_notification:view"
        ));

        // ===== 自定义Agent模板 =====

        // 65. 列出自定义Agent模板
        registerTool(new AiTool(
            "list_custom_agent_templates",
            "获取用户自定义的Agent模板（可复用的Agent配置）",
            Map.of(),
            "PERM_agents:view"
        ));

        // 66. 创建自定义Agent模板
        Map<String, ParameterDef> createCustomTplParams = new HashMap<>();
        createCustomTplParams.put("role", new ParameterDef("string", "角色标识（英文）", true));
        createCustomTplParams.put("name", new ParameterDef("string", "模板名称", true));
        createCustomTplParams.put("description", new ParameterDef("string", "模板描述", false));
        createCustomTplParams.put("systemPrompt", new ParameterDef("string", "系统提示词", false));
        registerTool(new AiTool(
            "create_custom_agent_template",
            "创建自定义Agent模板（用于批量招聘同类Agent）",
            createCustomTplParams,
            "PERM_agents:manage"
        ));

        // ===== MCP 管理 =====

        // 67. 查询MCP服务器详情
        Map<String, ParameterDef> getMcpServerParams = new HashMap<>();
        getMcpServerParams.put("serverId", new ParameterDef("number", "MCP服务器ID", true));
        registerTool(new AiTool(
            "get_mcp_server",
            "获取MCP服务器详情（配置、工具列表、连接状态）",
            getMcpServerParams,
            "PERM_agents:view"
        ));

        // 68. 切换MCP服务器启用状态
        Map<String, ParameterDef> toggleMcpParams = new HashMap<>();
        toggleMcpParams.put("serverId", new ParameterDef("number", "MCP服务器ID", true));
        registerTool(new AiTool(
            "toggle_mcp_server",
            "启用或禁用MCP服务器",
            toggleMcpParams,
            "PERM_agents:manage"
        ));

        // 69. 删除MCP服务器
        Map<String, ParameterDef> deleteMcpParams = new HashMap<>();
        deleteMcpParams.put("serverId", new ParameterDef("number", "MCP服务器ID", true));
        registerTool(new AiTool(
            "delete_mcp_server",
            "删除MCP服务器及其所有工具",
            deleteMcpParams,
            "PERM_agents:manage"
        ));

        // 70. 查询MCP工具列表
        Map<String, ParameterDef> listMcpToolsParams = new HashMap<>();
        listMcpToolsParams.put("serverId", new ParameterDef("number", "MCP服务器ID", true));
        registerTool(new AiTool(
            "list_mcp_tools",
            "获取MCP服务器的工具列表",
            listMcpToolsParams,
            "PERM_agents:view"
        ));

        // 71. 绑定MCP工具给Agent
        Map<String, ParameterDef> bindMcpParams = new HashMap<>();
        bindMcpParams.put("agentRole", new ParameterDef("string", "Agent角色", true));
        bindMcpParams.put("projectId", new ParameterDef("string", "项目ID", true));
        bindMcpParams.put("serverId", new ParameterDef("number", "MCP服务器ID", true));
        registerTool(new AiTool(
            "bind_mcp_to_agent",
            "将MCP服务器绑定到Agent（Agent可使用该服务器的所有工具）",
            bindMcpParams,
            "PERM_agents:manage"
        ));

        // 72. 从模板安装MCP服务器
        Map<String, ParameterDef> installMcpParams = new HashMap<>();
        installMcpParams.put("templateKey", new ParameterDef("string", "模板标识（如 unity-mcp, godot-mcp, redis-mcp）", true));
        installMcpParams.put("projectId", new ParameterDef("string", "关联项目ID", false));
        registerTool(new AiTool(
            "install_mcp_from_template",
            "从预置模板安装MCP服务器（支持Unity、Godot、Redis、Steam、PlayFab、Firebase、Jira等）",
            installMcpParams,
            "PERM_agents:manage"
        ));

        // ===== 知识库 =====

        // 73. 查询知识库统计
        registerTool(new AiTool(
            "get_knowledge_stats",
            "获取知识库统计信息（解决方案数、最佳实践数等）",
            Map.of(),
            "PERM_agents:view"
        ));

        // 68. 查询问题解决方案
        Map<String, ParameterDef> solutionParams = new HashMap<>();
        solutionParams.put("problemType", new ParameterDef("string", "问题类型（如 compilation_error, runtime_error, logic_bug）", true));
        registerTool(new AiTool(
            "get_solutions",
            "查询特定类型问题的历史解决方案",
            solutionParams,
            "PERM_agents:view"
        ));

        // ===== 版本迭代 =====

        // 74. 发起版本迭代
        Map<String, ParameterDef> versionIterationParams = new HashMap<>();
        versionIterationParams.put("projectId", new ParameterDef("string", "项目ID", true));
        versionIterationParams.put("requirements", new ParameterDef("string", "迭代需求描述", true));
        versionIterationParams.put("version", new ParameterDef("string", "目标版本号", false));
        versionIterationParams.put("priority", new ParameterDef("string", "优先级（HIGH/MEDIUM/LOW）", false));
        versionIterationParams.put("deadline", new ParameterDef("string", "截止时间", false));
        registerTool(new AiTool(
            "start_version_iteration",
            "对已完成项目发起版本迭代，制作人会分析需求并启动新的开发流程",
            versionIterationParams,
            "PERM_version:manage"
        ));

        // ===== 项目Agent配置 =====

        // 75. 获取项目Agent配置列表
        Map<String, ParameterDef> listAgentConfigsParams = new HashMap<>();
        listAgentConfigsParams.put("projectId", new ParameterDef("string", "项目ID", true));
        registerTool(new AiTool(
            "list_project_agent_configs",
            "获取项目中所有Agent的自定义配置",
            listAgentConfigsParams,
            "PERM_agent:view"
        ));

        // 76. 获取Agent配置
        Map<String, ParameterDef> getAgentConfigParams = new HashMap<>();
        getAgentConfigParams.put("projectId", new ParameterDef("string", "项目ID", true));
        getAgentConfigParams.put("agentRole", new ParameterDef("string", "Agent角色", true));
        registerTool(new AiTool(
            "get_agent_config",
            "获取项目中指定角色Agent的配置",
            getAgentConfigParams,
            "PERM_agent:view"
        ));

        // 77. 保存Agent配置
        Map<String, ParameterDef> saveAgentConfigParams = new HashMap<>();
        saveAgentConfigParams.put("projectId", new ParameterDef("string", "项目ID", true));
        saveAgentConfigParams.put("agentRole", new ParameterDef("string", "Agent角色", true));
        saveAgentConfigParams.put("customSystemPrompt", new ParameterDef("string", "自定义系统提示词", false));
        saveAgentConfigParams.put("customCapabilityPrompt", new ParameterDef("string", "自定义能力提示词", false));
        saveAgentConfigParams.put("projectContext", new ParameterDef("string", "项目特定上下文", false));
        registerTool(new AiTool(
            "save_agent_config",
            "保存或更新项目中指定角色Agent的配置",
            saveAgentConfigParams,
            "PERM_agent:config"
        ));

        // 78. AI优化Agent提示词
        Map<String, ParameterDef> optimizeAgentParams = new HashMap<>();
        optimizeAgentParams.put("projectId", new ParameterDef("string", "项目ID", true));
        optimizeAgentParams.put("agentRole", new ParameterDef("string", "Agent角色", true));
        registerTool(new AiTool(
            "optimize_agent_prompt",
            "使用AI分析项目需求，优化Agent的提示词配置",
            optimizeAgentParams,
            "PERM_agent:optimize"
        ));

        // 79. 获取Agent职责权重
        Map<String, ParameterDef> getAgentWeightsParams = new HashMap<>();
        getAgentWeightsParams.put("projectId", new ParameterDef("string", "项目ID", true));
        getAgentWeightsParams.put("agentRole", new ParameterDef("string", "Agent角色", true));
        registerTool(new AiTool(
            "get_agent_weights",
            "获取项目中指定角色Agent的职责权重",
            getAgentWeightsParams,
            "PERM_agent:view"
        ));

        // ===== 能力管理 =====

        // 80. 获取能力列表
        Map<String, ParameterDef> listCapabilitiesParams = new HashMap<>();
        listCapabilitiesParams.put("agentRole", new ParameterDef("string", "Agent角色（可选）", false));
        listCapabilitiesParams.put("category", new ParameterDef("string", "能力分类（可选）", false));
        registerTool(new AiTool(
            "list_capabilities",
            "获取能力列表，可按角色或分类筛选",
            listCapabilitiesParams,
            "PERM_agents:view"
        ));

        // 81. 创建能力
        Map<String, ParameterDef> createCapabilityParams = new HashMap<>();
        createCapabilityParams.put("agentRole", new ParameterDef("string", "Agent角色", true));
        createCapabilityParams.put("capabilityName", new ParameterDef("string", "能力名称", true));
        createCapabilityParams.put("displayName", new ParameterDef("string", "显示名称", true));
        createCapabilityParams.put("description", new ParameterDef("string", "能力描述", true));
        createCapabilityParams.put("category", new ParameterDef("string", "能力分类", false));
        createCapabilityParams.put("executionType", new ParameterDef("string", "执行方式（java/prompt/message）", false));
        createCapabilityParams.put("paramSchema", new ParameterDef("string", "参数Schema（JSON格式）", false));
        registerTool(new AiTool(
            "create_capability",
            "创建新的Agent能力",
            createCapabilityParams,
            "PERM_agents:manage"
        ));

        // 82. 切换能力启用状态
        Map<String, ParameterDef> toggleCapabilityParams = new HashMap<>();
        toggleCapabilityParams.put("capabilityId", new ParameterDef("number", "能力ID", true));
        registerTool(new AiTool(
            "toggle_capability",
            "启用或禁用Agent能力",
            toggleCapabilityParams,
            "PERM_agents:manage"
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

        sb.append("### 文件管理\n");
        appendToolByNames(sb, userPermissions, "search_files", "list_files", "get_file_usage");

        sb.append("### Agent 招聘\n");
        appendToolByNames(sb, userPermissions, "recruit_agent", "list_recruited_agents", "delete_agent");

        sb.append("### 项目详情\n");
        appendToolByNames(sb, userPermissions, "get_project_detail", "set_project_goal", "get_project_milestones");

        sb.append("### 调度器\n");
        appendToolByNames(sb, userPermissions, "get_scheduler_status", "trigger_schedule");

        sb.append("### 系统诊断\n");
        appendToolByNames(sb, userPermissions, "run_diagnostic", "quick_health_check");

        sb.append("### 代码浏览\n");
        appendToolByNames(sb, userPermissions, "browse_code", "read_code_file");

        sb.append("### 通知模板 & 自定义模板\n");
        appendToolByNames(sb, userPermissions, "list_notification_templates", "list_custom_agent_templates", "create_custom_agent_template");

        sb.append("### MCP 管理（外部工具集成）\n");
        sb.append("- 查询 MCP 服务器：`curl -s http://127.0.0.1:19922/api/mcp/servers`\n");
        sb.append("- 添加 MCP 服务器：`curl -X POST http://127.0.0.1:19922/api/mcp/servers -H 'Content-Type: application/json' -d '{...}'`\n");
        sb.append("- 测试 MCP 服务器：`curl -X POST http://127.0.0.1:19922/api/mcp/servers/{id}/test`\n");
        sb.append("- 从模板安装 MCP：`curl -X POST http://127.0.0.1:19922/api/mcp/servers/install -H 'Content-Type: application/json' -d '{\"templateKey\":\"unity-mcp\",\"projectId\":\"...\"}'`\n");
        sb.append("- 绑定 MCP 到 Agent：`curl -X POST http://127.0.0.1:19922/api/mcp/bindings -H 'Content-Type: application/json' -d '{...}'`\n\n");
        sb.append("可用 MCP 模板：unity-mcp（Unity编辑器）、godot-mcp（Godot引擎）、redis-mcp（Redis缓存）、");
        sb.append("steam-mcp（Steam平台）、playfab-mcp（PlayFab后端）、firebase-mcp（Firebase服务）、jira-mcp（Jira项目管理）\n\n");
        appendToolByNames(sb, userPermissions, "list_mcp_servers", "get_mcp_server", "add_mcp_server",
            "test_mcp_server", "toggle_mcp_server", "delete_mcp_server", "list_mcp_tools",
            "bind_mcp_to_agent", "install_mcp_from_template");

        sb.append("### 知识库\n");
        appendToolByNames(sb, userPermissions, "get_knowledge_stats", "get_solutions");

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
