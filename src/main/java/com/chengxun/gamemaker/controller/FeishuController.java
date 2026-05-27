package com.chengxun.gamemaker.controller;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.agent.ProducerAgent;
import com.chengxun.gamemaker.feishu.FeishuBotService;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.model.GameProject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/feishu")
public class FeishuController {

    private static final Logger log = LoggerFactory.getLogger(FeishuController.class);

    private final FeishuBotService feishuService;
    private final AgentManager agentManager;
    private final ProjectManager projectManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FeishuController(FeishuBotService feishuService, AgentManager agentManager, ProjectManager projectManager) {
        this.feishuService = feishuService;
        this.agentManager = agentManager;
        this.projectManager = projectManager;
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

    private void handleApproval(String chatId, boolean approved) {
        // TODO: 处理审批逻辑
        feishuService.sendMessage(chatId, approved ? "✅ 已批准" : "❌ 已拒绝");
    }
}
