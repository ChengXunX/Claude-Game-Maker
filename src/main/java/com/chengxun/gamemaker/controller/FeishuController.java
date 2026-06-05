package com.chengxun.gamemaker.controller;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.agent.ProducerAgent;
import com.chengxun.gamemaker.feishu.FeishuBotService;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.web.entity.Pipeline;
import com.chengxun.gamemaker.web.service.PipelineService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/feishu")
public class FeishuController {

    private static final Logger log = LoggerFactory.getLogger(FeishuController.class);

    private final FeishuBotService feishuService;
    private final AgentManager agentManager;
    private final ProjectManager projectManager;
    private final PipelineService pipelineService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FeishuController(FeishuBotService feishuService, AgentManager agentManager,
                            ProjectManager projectManager, PipelineService pipelineService) {
        this.feishuService = feishuService;
        this.agentManager = agentManager;
        this.projectManager = projectManager;
        this.pipelineService = pipelineService;
    }

    // ===== 飞书事件订阅回调 =====

    @PostMapping("/event")
    public ResponseEntity<Map<String, Object>> handleEvent(@RequestBody String body) {
        try {
            JsonNode node = objectMapper.readTree(body);

            // 处理 URL 验证请求
            if (node.has("type") && "url_verification".equals(node.get("type").asText())) {
                Map<String, Object> response = new HashMap<>();
                response.put("challenge", node.get("challenge").asText());
                return ResponseEntity.ok(response);
            }

            // 处理事件回调
            if (node.has("header") && node.has("event")) {
                JsonNode header = node.get("header");
                JsonNode event = node.get("event");

                String eventType = header.get("event_type").asText();

                switch (eventType) {
                    case "im.message.receive_v1" -> handleMessageEvent(event);
                    case "im.chat.member.bot.added_v1" -> handleBotAddedEvent(event);
                    default -> log.info("Unhandled event type: {}", eventType);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("code", 0);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to handle Feishu event", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", -1);
            response.put("msg", "Internal error");
            return ResponseEntity.ok(response);
        }
    }

    private void handleMessageEvent(JsonNode event) {
        try {
            JsonNode message = event.get("message");
            String chatId = message.get("chat_id").asText();
            String messageId = message.get("message_id").asText();
            String msgType = message.get("message_type").asText();

            if (!"text".equals(msgType)) {
                log.info("Ignoring non-text message: {}", msgType);
                return;
            }

            JsonNode content = objectMapper.readTree(message.get("content").asText());
            String text = content.get("text").asText().trim();

            log.info("Received message from Feishu chat {}: {}", chatId, text);

            // 处理命令
            processCommand(chatId, text);
        } catch (Exception e) {
            log.error("Failed to handle message event", e);
        }
    }

    private void handleBotAddedEvent(JsonNode event) {
        try {
            String chatId = event.get("chat_id").asText();
            log.info("Bot added to chat: {}", chatId);

            // 自动设置为默认群
            feishuService.setDefaultChatId(chatId);

            feishuService.sendMessage(chatId, "👋 你好！我是 ChengXun Game Maker 的智能助手。\n\n" +
                "我可以帮助你管理游戏开发团队，支持以下命令：\n\n" +
                "**项目管理**\n" +
                "- `创建项目 <名称> <目录>` - 创建新游戏项目\n" +
                "- `项目列表` - 查看所有项目\n\n" +
                "**Agent 管理**\n" +
                "- `状态` - 查看所有 Agent 状态\n" +
                "- `分配 <Agent> <目录>` - 为 Agent 分配工作目录\n\n" +
                "**任务管理**\n" +
                "- `任务 <Agent> <内容>` - 向 Agent 发送任务\n" +
                "- `查询 <Agent> <问题>` - 向 Agent 提问\n\n" +
                "**其他**\n" +
                "- `帮助` - 显示帮助信息");
        } catch (Exception e) {
            log.error("Failed to handle bot added event", e);
        }
    }

    // ===== 命令处理 =====

    private void processCommand(String chatId, String text) {
        // 确保 chatId 已设置
        if (feishuService.getDefaultChatId() == null) {
            feishuService.setDefaultChatId(chatId);
        }

        String[] parts = text.split("\\s+", 3);
        if (parts.length == 0) return;

        String command = parts[0].toLowerCase();

        switch (command) {
            case "帮助", "help" -> handleHelp(chatId);
            case "状态", "status" -> handleStatus(chatId);
            case "项目列表", "projects" -> handleProjectList(chatId);
            case "创建项目", "create" -> handleCreateProject(chatId, parts);
            case "分配", "assign" -> handleAssignWorkDir(chatId, parts);
            case "任务", "task" -> handleTask(chatId, parts);
            case "查询", "query" -> handleQuery(chatId, parts);
            case "approve" -> handleApproval(chatId, true);
            case "reject" -> handleApproval(chatId, false);
            // 流水线相关命令
            case "流水线", "pipeline" -> handlePipelineCommand(chatId, parts);
            case "构建", "build" -> handleBuild(chatId, parts);
            case "部署", "deploy" -> handleDeploy(chatId, parts);
            case "测试", "test" -> handleTest(chatId, parts);
            default -> feishuService.sendMessage(chatId, "❓ 未知命令: " + command + "\n\n输入 **帮助** 查看可用命令");
        }
    }

    private void handleHelp(String chatId) {
        feishuService.sendMessage(chatId, "📖 **可用命令**\n\n" +
            "**项目管理**\n" +
            "- `创建项目 <名称> <目录>` - 创建新游戏项目\n" +
            "- `项目列表` - 查看所有项目\n\n" +
            "**Agent 管理**\n" +
            "- `状态` - 查看所有 Agent 状态\n" +
            "- `分配 <Agent名称> <目录>` - 为 Agent 分配工作目录\n\n" +
            "**任务管理**\n" +
            "- `任务 <Agent名称> <内容>` - 向 Agent 发送任务\n" +
            "- `查询 <Agent名称> <问题>` - 向 Agent 提问\n\n" +
            "**审批**\n" +
            "- `approve` - 批准审批请求\n" +
            "- `reject` - 拒绝审批请求");
    }

    private void handleStatus(String chatId) {
        StringBuilder sb = new StringBuilder("**📊 系统状态**\n\n");

        sb.append("**Agent 状态：**\n");
        for (Agent agent : agentManager.getAllAgents()) {
            sb.append(String.format("- %s (%s): %s %s\n",
                agent.getName(),
                agent.getRole(),
                agent.isAlive() ? "🟢" : "🔴",
                agent.isBusy() ? "忙碌" : "空闲"));
        }

        sb.append("\n**项目列表：**\n");
        for (GameProject project : projectManager.getAllProjects()) {
            sb.append(String.format("- %s (%s)\n", project.getName(), project.getWorkDir()));
        }

        feishuService.sendMessage(chatId, sb.toString());
    }

    private void handleProjectList(String chatId) {
        StringBuilder sb = new StringBuilder("**📁 项目列表**\n\n");

        if (projectManager.getAllProjects().isEmpty()) {
            sb.append("暂无项目\n\n使用 `创建项目 <名称> <目录>` 创建新项目");
        } else {
            for (GameProject project : projectManager.getAllProjects()) {
                sb.append(String.format("- **%s**\n  目录: %s\n  状态: %s\n  Agent数: %d\n\n",
                    project.getName(),
                    project.getWorkDir(),
                    project.getStatus(),
                    project.getAgentIds().size()));
            }
        }

        feishuService.sendMessage(chatId, sb.toString());
    }

    private void handleCreateProject(String chatId, String[] parts) {
        if (parts.length < 3) {
            feishuService.sendMessage(chatId, "❌ 用法: `创建项目 <名称> <目录>`");
            return;
        }

        String name = parts[1];
        String workDir = parts[2];

        try {
            GameProject project = projectManager.createProject(name, "由飞书创建的游戏项目", workDir);
            feishuService.sendMessage(chatId, String.format("✅ 项目创建成功\n\n- 名称: %s\n- 目录: %s\n- ID: %s",
                project.getName(), project.getWorkDir(), project.getId()));
        } catch (Exception e) {
            feishuService.sendMessage(chatId, "❌ 项目创建失败: " + e.getMessage());
        }
    }

    private void handleAssignWorkDir(String chatId, String[] parts) {
        if (parts.length < 3) {
            feishuService.sendMessage(chatId, "❌ 用法: `分配 <Agent名称> <目录>`");
            return;
        }

        String agentName = parts[1];
        String workDir = parts[2];

        Agent targetAgent = agentManager.getAllAgents().stream()
            .filter(a -> a.getName().contains(agentName) || a.getId().contains(agentName))
            .findFirst()
            .orElse(null);

        if (targetAgent == null) {
            feishuService.sendMessage(chatId, "❌ 未找到 Agent: " + agentName);
            return;
        }

        if (targetAgent instanceof ProducerAgent producer) {
            producer.assignWorkDir(targetAgent.getId(), workDir);
            feishuService.sendMessage(chatId, String.format("✅ 已为 %s 分配工作目录: %s", targetAgent.getName(), workDir));
        } else {
            targetAgent.getDefinition().setWorkDir(workDir);
            targetAgent.saveContext();
            feishuService.sendMessage(chatId, String.format("✅ 已为 %s 分配工作目录: %s", targetAgent.getName(), workDir));
        }
    }

    private void handleTask(String chatId, String[] parts) {
        if (parts.length < 3) {
            feishuService.sendMessage(chatId, "❌ 用法: `任务 <Agent名称> <内容>`");
            return;
        }

        String agentName = parts[1];
        String taskContent = parts[2];

        Agent targetAgent = agentManager.getAllAgents().stream()
            .filter(a -> a.getName().contains(agentName) || a.getId().contains(agentName))
            .findFirst()
            .orElse(null);

        if (targetAgent == null) {
            feishuService.sendMessage(chatId, "❌ 未找到 Agent: " + agentName);
            return;
        }

        // 发送任务给 Agent
        com.chengxun.gamemaker.model.AgentMessage taskMsg = com.chengxun.gamemaker.model.AgentMessage.createTask(
            "feishu-user", targetAgent.getId(), taskContent);
        targetAgent.receiveMessage(taskMsg);

        feishuService.sendMessage(chatId, String.format("✅ 任务已发送给 %s\n\n任务内容: %s",
            targetAgent.getName(), taskContent));
    }

    private void handleQuery(String chatId, String[] parts) {
        if (parts.length < 3) {
            feishuService.sendMessage(chatId, "❌ 用法: `查询 <Agent名称> <问题>`");
            return;
        }

        String agentName = parts[1];
        String queryContent = parts[2];

        Agent targetAgent = agentManager.getAllAgents().stream()
            .filter(a -> a.getName().contains(agentName) || a.getId().contains(agentName))
            .findFirst()
            .orElse(null);

        if (targetAgent == null) {
            feishuService.sendMessage(chatId, "❌ 未找到 Agent: " + agentName);
            return;
        }

        // 发送查询给 Agent
        com.chengxun.gamemaker.model.AgentMessage queryMsg = com.chengxun.gamemaker.model.AgentMessage.builder()
            .fromAgentId("feishu-user")
            .toAgentId(targetAgent.getId())
            .type(com.chengxun.gamemaker.model.AgentMessage.MessageType.QUERY)
            .content(queryContent)
            .build();
        targetAgent.receiveMessage(queryMsg);

        feishuService.sendMessage(chatId, String.format("✅ 查询已发送给 %s\n\n问题: %s\n\n请等待回复...",
            targetAgent.getName(), queryContent));
    }

    /**
     * 处理审批请求
     * 记录审批结果并通知相关Agent
     *
     * @param chatId 飞书聊天ID
     * @param approved 是否批准
     */
    private void handleApproval(String chatId, boolean approved) {
        log.info("Approval request processed: chatId={}, approved={}", chatId, approved);

        // 通知所有Producer Agent审批结果
        agentManager.getAllAgents().stream()
            .filter(a -> "producer".equals(a.getRole()))
            .forEach(agent -> {
                com.chengxun.gamemaker.model.AgentMessage approvalMsg = com.chengxun.gamemaker.model.AgentMessage.builder()
                    .fromAgentId("feishu-user")
                    .toAgentId(agent.getId())
                    .type(com.chengxun.gamemaker.model.AgentMessage.MessageType.SYSTEM)
                    .content(approved ? "审批已通过" : "审批已被拒绝")
                    .build();
                agent.receiveMessage(approvalMsg);
            });

        feishuService.sendMessage(chatId, approved ? "✅ 已批准，已通知制作人" : "❌ 已拒绝，已通知制作人");
    }

    // ===== 流水线相关命令 =====

    /**
     * 处理流水线命令
     */
    private void handlePipelineCommand(String chatId, String[] parts) {
        if (parts.length < 2) {
            showPipelineHelp(chatId);
            return;
        }

        String subCommand = parts[1].toLowerCase();

        switch (subCommand) {
            case "列表", "list" -> handlePipelineList(chatId);
            case "触发", "trigger" -> handlePipelineTrigger(chatId, parts);
            case "状态", "status" -> handlePipelineStatus(chatId, parts);
            case "创建", "create" -> handlePipelineCreate(chatId, parts);
            default -> showPipelineHelp(chatId);
        }
    }

    /**
     * 显示流水线帮助信息
     */
    private void showPipelineHelp(String chatId) {
        feishuService.sendMessage(chatId, "🔧 **流水线命令帮助**\n\n" +
            "**查看流水线**\n" +
            "- `流水线 列表` - 查看所有流水线\n" +
            "- `流水线 状态 <编号>` - 查看流水线状态\n\n" +
            "**触发执行**\n" +
            "- `流水线 触发 <编号>` - 触发指定流水线\n" +
            "- `构建 <项目名>` - 快速触发构建\n" +
            "- `测试 <项目名>` - 快速触发测试\n" +
            "- `部署 <项目名>` - 快速触发部署\n\n" +
            "**创建流水线**\n" +
            "- `流水线 创建 <项目名> <类型>` - 创建新流水线\n" +
            "  类型: build/test/deploy/full");
    }

    /**
     * 查看流水线列表
     */
    private void handlePipelineList(String chatId) {
        List<Pipeline> pipelines = pipelineService.getAllPipelines();

        if (pipelines.isEmpty()) {
            feishuService.sendMessage(chatId, "📋 **流水线列表**\n\n暂无流水线\n\n使用 `流水线 创建 <项目名> <类型>` 创建新流水线");
            return;
        }

        StringBuilder sb = new StringBuilder("📋 **流水线列表**\n\n");
        for (Pipeline pipeline : pipelines) {
            sb.append(String.format("- **%s** (%s)\n  项目: %s\n  状态: %s\n  类型: %s\n\n",
                pipeline.getName(),
                pipeline.getPipelineNo(),
                pipeline.getProjectName(),
                pipeline.getStatusDescription(),
                pipeline.getPipelineTypeDescription()));
        }

        feishuService.sendMessage(chatId, sb.toString());
    }

    /**
     * 触发流水线执行
     */
    private void handlePipelineTrigger(String chatId, String[] parts) {
        if (parts.length < 3) {
            feishuService.sendMessage(chatId, "❌ 用法: `流水线 触发 <编号>`\n\n例如: `流水线 触发 PL-20260529-ABC123`");
            return;
        }

        String pipelineNo = parts[2];

        // 查找流水线
        List<Pipeline> pipelines = pipelineService.getAllPipelines();
        Pipeline targetPipeline = pipelines.stream()
            .filter(p -> p.getPipelineNo().equals(pipelineNo))
            .findFirst()
            .orElse(null);

        if (targetPipeline == null) {
            feishuService.sendMessage(chatId, "❌ 未找到流水线: " + pipelineNo);
            return;
        }

        if (targetPipeline.isRunning()) {
            feishuService.sendMessage(chatId, "⚠️ 流水线正在执行中，无法重复触发");
            return;
        }

        try {
            // 触发执行
            Pipeline pipeline = pipelineService.triggerPipeline(
                targetPipeline.getId(), 0L, "飞书用户", "FEISHU");

            feishuService.sendMessage(chatId, String.format("✅ 流水线已触发执行\n\n" +
                "- 名称: %s\n" +
                "- 编号: %s\n" +
                "- 项目: %s\n" +
                "- 类型: %s\n\n" +
                "请等待执行完成，可使用 `流水线 状态 %s` 查看进度",
                pipeline.getName(),
                pipeline.getPipelineNo(),
                pipeline.getProjectName(),
                pipeline.getPipelineTypeDescription(),
                pipeline.getPipelineNo()));

        } catch (Exception e) {
            feishuService.sendMessage(chatId, "❌ 触发失败: " + e.getMessage());
        }
    }

    /**
     * 查看流水线状态
     */
    private void handlePipelineStatus(String chatId, String[] parts) {
        if (parts.length < 3) {
            feishuService.sendMessage(chatId, "❌ 用法: `流水线 状态 <编号>`");
            return;
        }

        String pipelineNo = parts[2];

        // 查找流水线
        List<Pipeline> pipelines = pipelineService.getAllPipelines();
        Pipeline targetPipeline = pipelines.stream()
            .filter(p -> p.getPipelineNo().equals(pipelineNo))
            .findFirst()
            .orElse(null);

        if (targetPipeline == null) {
            feishuService.sendMessage(chatId, "❌ 未找到流水线: " + pipelineNo);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📊 **流水线状态**\n\n");
        sb.append(String.format("- 名称: %s\n", targetPipeline.getName()));
        sb.append(String.format("- 编号: %s\n", targetPipeline.getPipelineNo()));
        sb.append(String.format("- 项目: %s\n", targetPipeline.getProjectName()));
        sb.append(String.format("- 类型: %s\n", targetPipeline.getPipelineTypeDescription()));
        sb.append(String.format("- 状态: %s\n", targetPipeline.getStatusDescription()));
        sb.append(String.format("- 进度: %d%%\n", targetPipeline.getProgress()));

        if (targetPipeline.getCurrentStage() != null) {
            sb.append(String.format("- 当前阶段: %s\n", targetPipeline.getCurrentStage()));
        }

        if (targetPipeline.getTriggeredByName() != null) {
            sb.append(String.format("- 触发人: %s\n", targetPipeline.getTriggeredByName()));
        }

        if (targetPipeline.getDurationSeconds() != null) {
            sb.append(String.format("- 耗时: %d秒\n", targetPipeline.getDurationSeconds()));
        }

        feishuService.sendMessage(chatId, sb.toString());
    }

    /**
     * 创建流水线
     */
    private void handlePipelineCreate(String chatId, String[] parts) {
        if (parts.length < 4) {
            feishuService.sendMessage(chatId, "❌ 用法: `流水线 创建 <项目名> <类型>`\n\n" +
                "类型: build/test/deploy/full\n\n" +
                "例如: `流水线 创建 my-game full`");
            return;
        }

        String projectName = parts[2];
        String pipelineType = parts[3].toUpperCase();

        // 查找项目
        GameProject targetProject = projectManager.getAllProjects().stream()
            .filter(p -> p.getName().contains(projectName) || p.getId().contains(projectName))
            .findFirst()
            .orElse(null);

        if (targetProject == null) {
            feishuService.sendMessage(chatId, "❌ 未找到项目: " + projectName);
            return;
        }

        // 验证流水线类型
        if (!List.of("BUILD", "TEST", "DEPLOY", "FULL").contains(pipelineType)) {
            feishuService.sendMessage(chatId, "❌ 无效的流水线类型: " + pipelineType + "\n\n有效类型: build/test/deploy/full");
            return;
        }

        try {
            Pipeline pipeline = pipelineService.createPipeline(
                targetProject.getName() + " - " + getPipelineTypeName(pipelineType),
                "由飞书创建的流水线",
                targetProject.getId(),
                pipelineType,
                null
            );

            feishuService.sendMessage(chatId, String.format("✅ 流水线创建成功\n\n" +
                "- 名称: %s\n" +
                "- 编号: %s\n" +
                "- 项目: %s\n" +
                "- 类型: %s\n\n" +
                "使用 `流水线 触发 %s` 开始执行",
                pipeline.getName(),
                pipeline.getPipelineNo(),
                pipeline.getProjectName(),
                pipeline.getPipelineTypeDescription(),
                pipeline.getPipelineNo()));

        } catch (Exception e) {
            feishuService.sendMessage(chatId, "❌ 创建失败: " + e.getMessage());
        }
    }

    /**
     * 快速构建
     */
    private void handleBuild(String chatId, String[] parts) {
        if (parts.length < 2) {
            feishuService.sendMessage(chatId, "❌ 用法: `构建 <项目名>`");
            return;
        }

        String projectName = parts[1];
        quickTriggerPipeline(chatId, projectName, "BUILD", "构建");
    }

    /**
     * 快速测试
     */
    private void handleTest(String chatId, String[] parts) {
        if (parts.length < 2) {
            feishuService.sendMessage(chatId, "❌ 用法: `测试 <项目名>`");
            return;
        }

        String projectName = parts[1];
        quickTriggerPipeline(chatId, projectName, "TEST", "测试");
    }

    /**
     * 快速部署
     */
    private void handleDeploy(String chatId, String[] parts) {
        if (parts.length < 2) {
            feishuService.sendMessage(chatId, "❌ 用法: `部署 <项目名>`");
            return;
        }

        String projectName = parts[1];
        quickTriggerPipeline(chatId, projectName, "DEPLOY", "部署");
    }

    /**
     * 快速触发流水线
     */
    private void quickTriggerPipeline(String chatId, String projectName, String pipelineType, String actionName) {
        // 查找项目
        GameProject targetProject = projectManager.getAllProjects().stream()
            .filter(p -> p.getName().contains(projectName) || p.getId().contains(projectName))
            .findFirst()
            .orElse(null);

        if (targetProject == null) {
            feishuService.sendMessage(chatId, "❌ 未找到项目: " + projectName);
            return;
        }

        // 查找或创建流水线
        List<Pipeline> projectPipelines = pipelineService.getProjectPipelines(targetProject.getId());
        Pipeline targetPipeline = projectPipelines.stream()
            .filter(p -> p.getPipelineType().equals(pipelineType))
            .findFirst()
            .orElse(null);

        if (targetPipeline == null) {
            // 创建新流水线
            targetPipeline = pipelineService.createPipeline(
                targetProject.getName() + " - " + actionName,
                "快速" + actionName + "流水线",
                targetProject.getId(),
                pipelineType,
                null
            );
        }

        if (targetPipeline.isRunning()) {
            feishuService.sendMessage(chatId, "⚠️ 流水线正在执行中，请等待完成");
            return;
        }

        try {
            Pipeline pipeline = pipelineService.triggerPipeline(
                targetPipeline.getId(), 0L, "飞书用户", "FEISHU");

            feishuService.sendMessage(chatId, String.format("✅ %s流水线已触发\n\n" +
                "- 项目: %s\n" +
                "- 编号: %s\n\n" +
                "请等待执行完成",
                actionName,
                targetProject.getName(),
                pipeline.getPipelineNo()));

        } catch (Exception e) {
            feishuService.sendMessage(chatId, "❌ 触发失败: " + e.getMessage());
        }
    }

    /**
     * 获取流水线类型名称
     */
    private String getPipelineTypeName(String type) {
        return switch (type) {
            case "BUILD" -> "构建";
            case "TEST" -> "测试";
            case "DEPLOY" -> "部署";
            case "FULL" -> "完整流水线";
            default -> type;
        };
    }
}
