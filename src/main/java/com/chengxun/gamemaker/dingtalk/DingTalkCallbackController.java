package com.chengxun.gamemaker.dingtalk;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.agent.ProducerAgent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.model.AgentMessage;
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

/**
 * 钉钉机器人控制器
 * 处理钉钉消息回调和命令处理
 *
 * 主要功能：
 * - 接收钉钉群消息
 * - 处理用户命令
 * - 管理Agent和项目
 * - 流水线管理
 *
 * 支持的命令：
 * - 帮助/help - 显示帮助信息
 * - 状态/status - 查看系统状态
 * - 项目列表/projects - 查看项目列表
 * - 创建项目/create - 创建新项目
 * - 分配/assign - 分配Agent工作目录
 * - 任务/task - 向Agent发送任务
 * - 查询/query - 向Agent提问
 * - approve - 批准审批
 * - reject - 拒绝审批
 * - 流水线/pipeline - 流水线管理
 * - 构建/build - 快速构建
 * - 测试/test - 快速测试
 * - 部署/deploy - 快速部署
 *
 * @author chengxun
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/dingtalk-bot")
public class DingTalkCallbackController {

    private static final Logger log = LoggerFactory.getLogger(DingTalkCallbackController.class);

    private final DingTalkService dingTalkService;
    private final AgentManager agentManager;
    private final ProjectManager projectManager;
    private final PipelineService pipelineService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DingTalkCallbackController(DingTalkService dingTalkService, AgentManager agentManager,
                               ProjectManager projectManager, PipelineService pipelineService) {
        this.dingTalkService = dingTalkService;
        this.agentManager = agentManager;
        this.projectManager = projectManager;
        this.pipelineService = pipelineService;
    }

    // ===== 消息回调 =====

    /**
     * 处理钉钉消息回调
     * 钉钉机器人接收到的消息会通过此接口回调
     *
     * @param body 请求体
     * @return 响应
     */
    @PostMapping("/callback")
    public ResponseEntity<Map<String, Object>> handleMessage(@RequestBody String body) {
        try {
            JsonNode node = objectMapper.readTree(body);

            // 提取消息内容
            String msgType = node.has("msgtype") ? node.get("msgtype").asText() : "";
            String text = "";
            String senderId = "";

            if ("text".equals(msgType) && node.has("text")) {
                text = node.get("text").get("content").asText().trim();
            }

            if (node.has("senderId")) {
                senderId = node.get("senderId").asText();
            } else if (node.has("senderNick")) {
                senderId = node.get("senderNick").asText();
            }

            log.info("Received DingTalk message from {}: {}", senderId, text);

            // 处理命令
            if (!text.isEmpty()) {
                processCommand(text);
            }

            // 返回成功响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to handle DingTalk message", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "处理失败");
            return ResponseEntity.ok(response);
        }
    }

    // ===== 命令处理 =====

    /**
     * 处理用户命令
     *
     * @param text 命令文本
     */
    private void processCommand(String text) {
        String[] parts = text.split("\\s+", 3);
        if (parts.length == 0) return;

        String command = parts[0].toLowerCase();

        switch (command) {
            case "帮助", "help" -> handleHelp();
            case "状态", "status" -> handleStatus();
            case "项目列表", "projects" -> handleProjectList();
            case "创建项目", "create" -> handleCreateProject(parts);
            case "分配", "assign" -> handleAssignWorkDir(parts);
            case "任务", "task" -> handleTask(parts);
            case "查询", "query" -> handleQuery(parts);
            case "approve" -> handleApproval(true);
            case "reject" -> handleApproval(false);
            case "流水线", "pipeline" -> handlePipelineCommand(parts);
            case "构建", "build" -> handleBuild(parts);
            case "测试", "test" -> handleTest(parts);
            case "部署", "deploy" -> handleDeploy(parts);
            default -> dingTalkService.sendMarkdown("未知命令",
                "### ❓ 未知命令\n\n命令: " + command + "\n\n输入 **帮助** 查看可用命令");
        }
    }

    /**
     * 显示帮助信息
     */
    private void handleHelp() {
        dingTalkService.sendMarkdown("帮助信息",
            "### 📖 可用命令\n\n" +
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
            "- `reject` - 拒绝审批请求\n\n" +
            "**流水线**\n" +
            "- `流水线 列表` - 查看流水线\n" +
            "- `流水线 触发 <编号>` - 触发流水线\n" +
            "- `构建 <项目名>` - 快速构建\n" +
            "- `测试 <项目名>` - 快速测试\n" +
            "- `部署 <项目名>` - 快速部署");
    }

    /**
     * 显示系统状态
     */
    private void handleStatus() {
        StringBuilder sb = new StringBuilder("### 📊 系统状态\n\n");

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

        dingTalkService.sendMarkdown("系统状态", sb.toString());
    }

    /**
     * 显示项目列表
     */
    private void handleProjectList() {
        StringBuilder sb = new StringBuilder("### 📁 项目列表\n\n");

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

        dingTalkService.sendMarkdown("项目列表", sb.toString());
    }

    /**
     * 创建项目
     *
     * @param parts 命令参数
     */
    private void handleCreateProject(String[] parts) {
        if (parts.length < 3) {
            dingTalkService.sendMarkdown("创建项目", "### ❌ 用法\n\n`创建项目 <名称> <目录>`");
            return;
        }

        String name = parts[1];
        String workDir = parts[2];

        try {
            GameProject project = projectManager.createProject(name, "由钉钉创建的游戏项目", workDir);
            dingTalkService.sendMarkdown("创建项目成功",
                String.format("### ✅ 项目创建成功\n\n- 名称: %s\n- 目录: %s\n- ID: %s",
                    project.getName(), project.getWorkDir(), project.getId()));
        } catch (Exception e) {
            dingTalkService.sendMarkdown("创建项目失败",
                "### ❌ 项目创建失败\n\n" + e.getMessage());
        }
    }

    /**
     * 分配Agent工作目录
     *
     * @param parts 命令参数
     */
    private void handleAssignWorkDir(String[] parts) {
        if (parts.length < 3) {
            dingTalkService.sendMarkdown("分配目录", "### ❌ 用法\n\n`分配 <Agent名称> <目录>`");
            return;
        }

        String agentName = parts[1];
        String workDir = parts[2];

        Agent targetAgent = findAgent(agentName);
        if (targetAgent == null) {
            dingTalkService.sendMarkdown("分配目录", "### ❌ 未找到 Agent\n\n" + agentName);
            return;
        }

        if (targetAgent instanceof ProducerAgent producer) {
            producer.assignWorkDir(targetAgent.getId(), workDir);
        } else {
            targetAgent.getDefinition().setWorkDir(workDir);
            targetAgent.saveContext();
        }

        dingTalkService.sendMarkdown("分配目录成功",
            String.format("### ✅ 分配成功\n\n已为 **%s** 分配工作目录: %s", targetAgent.getName(), workDir));
    }

    /**
     * 向Agent发送任务
     *
     * @param parts 命令参数
     */
    private void handleTask(String[] parts) {
        if (parts.length < 3) {
            dingTalkService.sendMarkdown("发送任务", "### ❌ 用法\n\n`任务 <Agent名称> <内容>`");
            return;
        }

        String agentName = parts[1];
        String taskContent = parts[2];

        Agent targetAgent = findAgent(agentName);
        if (targetAgent == null) {
            dingTalkService.sendMarkdown("发送任务", "### ❌ 未找到 Agent\n\n" + agentName);
            return;
        }

        // 发送任务给 Agent
        AgentMessage taskMsg = AgentMessage.createTask("dingtalk-user", targetAgent.getId(), taskContent);
        targetAgent.receiveMessage(taskMsg);

        dingTalkService.sendMarkdown("任务已发送",
            String.format("### ✅ 任务已发送\n\n**Agent**: %s\n**任务内容**: %s",
                targetAgent.getName(), taskContent));
    }

    /**
     * 向Agent提问
     *
     * @param parts 命令参数
     */
    private void handleQuery(String[] parts) {
        if (parts.length < 3) {
            dingTalkService.sendMarkdown("查询", "### ❌ 用法\n\n`查询 <Agent名称> <问题>`");
            return;
        }

        String agentName = parts[1];
        String queryContent = parts[2];

        Agent targetAgent = findAgent(agentName);
        if (targetAgent == null) {
            dingTalkService.sendMarkdown("查询", "### ❌ 未找到 Agent\n\n" + agentName);
            return;
        }

        // 发送查询给 Agent
        AgentMessage queryMsg = AgentMessage.builder()
            .fromAgentId("dingtalk-user")
            .toAgentId(targetAgent.getId())
            .type(AgentMessage.MessageType.QUERY)
            .content(queryContent)
            .build();
        targetAgent.receiveMessage(queryMsg);

        dingTalkService.sendMarkdown("查询已发送",
            String.format("### ✅ 查询已发送\n\n**Agent**: %s\n**问题**: %s\n\n请等待回复...",
                targetAgent.getName(), queryContent));
    }

    /**
     * 处理审批请求
     *
     * @param approved 是否批准
     */
    private void handleApproval(boolean approved) {
        log.info("Approval request processed: approved={}", approved);

        // 通知所有Producer Agent审批结果
        agentManager.getAllAgents().stream()
            .filter(a -> "producer".equals(a.getRole()))
            .forEach(agent -> {
                AgentMessage approvalMsg = AgentMessage.builder()
                    .fromAgentId("dingtalk-user")
                    .toAgentId(agent.getId())
                    .type(AgentMessage.MessageType.SYSTEM)
                    .content(approved ? "审批已通过" : "审批已被拒绝")
                    .build();
                agent.receiveMessage(approvalMsg);
            });

        dingTalkService.sendMarkdown("审批结果",
            approved ? "### ✅ 已批准\n\n已通知制作人" : "### ❌ 已拒绝\n\n已通知制作人");
    }

    // ===== 流水线相关命令 =====

    /**
     * 处理流水线命令
     *
     * @param parts 命令参数
     */
    private void handlePipelineCommand(String[] parts) {
        if (parts.length < 2) {
            showPipelineHelp();
            return;
        }

        String subCommand = parts[1].toLowerCase();

        switch (subCommand) {
            case "列表", "list" -> handlePipelineList();
            case "触发", "trigger" -> handlePipelineTrigger(parts);
            case "状态", "status" -> handlePipelineStatus(parts);
            case "创建", "create" -> handlePipelineCreate(parts);
            default -> showPipelineHelp();
        }
    }

    /**
     * 显示流水线帮助
     */
    private void showPipelineHelp() {
        dingTalkService.sendMarkdown("流水线帮助",
            "### 🔧 流水线命令帮助\n\n" +
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
    private void handlePipelineList() {
        List<Pipeline> pipelines = pipelineService.getAllPipelines();

        if (pipelines.isEmpty()) {
            dingTalkService.sendMarkdown("流水线列表",
                "### 📋 流水线列表\n\n暂无流水线\n\n使用 `流水线 创建 <项目名> <类型>` 创建新流水线");
            return;
        }

        StringBuilder sb = new StringBuilder("### 📋 流水线列表\n\n");
        for (Pipeline pipeline : pipelines) {
            sb.append(String.format("- **%s** (%s)\n  项目: %s\n  状态: %s\n  类型: %s\n\n",
                pipeline.getName(),
                pipeline.getPipelineNo(),
                pipeline.getProjectName(),
                pipeline.getStatusDescription(),
                pipeline.getPipelineTypeDescription()));
        }

        dingTalkService.sendMarkdown("流水线列表", sb.toString());
    }

    /**
     * 触发流水线执行
     *
     * @param parts 命令参数
     */
    private void handlePipelineTrigger(String[] parts) {
        if (parts.length < 3) {
            dingTalkService.sendMarkdown("触发流水线",
                "### ❌ 用法\n\n`流水线 触发 <编号>`\n\n例如: `流水线 触发 PL-20260529-ABC123`");
            return;
        }

        String pipelineNo = parts[2];

        // 查找流水线
        Pipeline targetPipeline = findPipeline(pipelineNo);
        if (targetPipeline == null) {
            dingTalkService.sendMarkdown("触发流水线", "### ❌ 未找到流水线\n\n" + pipelineNo);
            return;
        }

        if (targetPipeline.isRunning()) {
            dingTalkService.sendMarkdown("触发流水线", "### ⚠️ 流水线正在执行中\n\n无法重复触发");
            return;
        }

        try {
            Pipeline pipeline = pipelineService.triggerPipeline(
                targetPipeline.getId(), 0L, "钉钉用户", "DINGTALK");

            dingTalkService.sendMarkdown("流水线已触发",
                String.format("### ✅ 流水线已触发执行\n\n" +
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
            dingTalkService.sendMarkdown("触发失败", "### ❌ 触发失败\n\n" + e.getMessage());
        }
    }

    /**
     * 查看流水线状态
     *
     * @param parts 命令参数
     */
    private void handlePipelineStatus(String[] parts) {
        if (parts.length < 3) {
            dingTalkService.sendMarkdown("流水线状态", "### ❌ 用法\n\n`流水线 状态 <编号>`");
            return;
        }

        String pipelineNo = parts[2];

        Pipeline targetPipeline = findPipeline(pipelineNo);
        if (targetPipeline == null) {
            dingTalkService.sendMarkdown("流水线状态", "### ❌ 未找到流水线\n\n" + pipelineNo);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("### 📊 流水线状态\n\n");
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

        dingTalkService.sendMarkdown("流水线状态", sb.toString());
    }

    /**
     * 创建流水线
     *
     * @param parts 命令参数
     */
    private void handlePipelineCreate(String[] parts) {
        if (parts.length < 4) {
            dingTalkService.sendMarkdown("创建流水线",
                "### ❌ 用法\n\n`流水线 创建 <项目名> <类型>`\n\n类型: build/test/deploy/full\n\n例如: `流水线 创建 my-game full`");
            return;
        }

        String projectName = parts[2];
        String pipelineType = parts[3].toUpperCase();

        // 查找项目
        GameProject targetProject = findProject(projectName);
        if (targetProject == null) {
            dingTalkService.sendMarkdown("创建流水线", "### ❌ 未找到项目\n\n" + projectName);
            return;
        }

        // 验证流水线类型
        if (!List.of("BUILD", "TEST", "DEPLOY", "FULL").contains(pipelineType)) {
            dingTalkService.sendMarkdown("创建流水线",
                "### ❌ 无效的流水线类型\n\n" + pipelineType + "\n\n有效类型: build/test/deploy/full");
            return;
        }

        try {
            Pipeline pipeline = pipelineService.createPipeline(
                targetProject.getName() + " - " + getPipelineTypeName(pipelineType),
                "由钉钉创建的流水线",
                targetProject.getId(),
                pipelineType,
                null
            );

            dingTalkService.sendMarkdown("创建流水线成功",
                String.format("### ✅ 流水线创建成功\n\n" +
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
            dingTalkService.sendMarkdown("创建流水线失败", "### ❌ 创建失败\n\n" + e.getMessage());
        }
    }

    /**
     * 快速构建
     *
     * @param parts 命令参数
     */
    private void handleBuild(String[] parts) {
        if (parts.length < 2) {
            dingTalkService.sendMarkdown("构建", "### ❌ 用法\n\n`构建 <项目名>`");
            return;
        }
        quickTriggerPipeline(parts[1], "BUILD", "构建");
    }

    /**
     * 快速测试
     *
     * @param parts 命令参数
     */
    private void handleTest(String[] parts) {
        if (parts.length < 2) {
            dingTalkService.sendMarkdown("测试", "### ❌ 用法\n\n`测试 <项目名>`");
            return;
        }
        quickTriggerPipeline(parts[1], "TEST", "测试");
    }

    /**
     * 快速部署
     *
     * @param parts 命令参数
     */
    private void handleDeploy(String[] parts) {
        if (parts.length < 2) {
            dingTalkService.sendMarkdown("部署", "### ❌ 用法\n\n`部署 <项目名>`");
            return;
        }
        quickTriggerPipeline(parts[1], "DEPLOY", "部署");
    }

    /**
     * 快速触发流水线
     *
     * @param projectName 项目名称
     * @param pipelineType 流水线类型
     * @param actionName 操作名称
     */
    private void quickTriggerPipeline(String projectName, String pipelineType, String actionName) {
        // 查找项目
        GameProject targetProject = findProject(projectName);
        if (targetProject == null) {
            dingTalkService.sendMarkdown(actionName, "### ❌ 未找到项目\n\n" + projectName);
            return;
        }

        // 查找或创建流水线
        List<Pipeline> projectPipelines = pipelineService.getProjectPipelines(targetProject.getId());
        Pipeline targetPipeline = projectPipelines.stream()
            .filter(p -> p.getPipelineType().equals(pipelineType))
            .findFirst()
            .orElse(null);

        if (targetPipeline == null) {
            targetPipeline = pipelineService.createPipeline(
                targetProject.getName() + " - " + actionName,
                "快速" + actionName + "流水线",
                targetProject.getId(),
                pipelineType,
                null
            );
        }

        if (targetPipeline.isRunning()) {
            dingTalkService.sendMarkdown(actionName, "### ⚠️ 流水线正在执行中\n\n请等待完成");
            return;
        }

        try {
            Pipeline pipeline = pipelineService.triggerPipeline(
                targetPipeline.getId(), 0L, "钉钉用户", "DINGTALK");

            dingTalkService.sendMarkdown(actionName + "已触发",
                String.format("### ✅ %s流水线已触发\n\n" +
                    "- 项目: %s\n" +
                    "- 编号: %s\n\n" +
                    "请等待执行完成",
                    actionName,
                    targetProject.getName(),
                    pipeline.getPipelineNo()));
        } catch (Exception e) {
            dingTalkService.sendMarkdown(actionName + "失败", "### ❌ 触发失败\n\n" + e.getMessage());
        }
    }

    // ===== 辅助方法 =====

    /**
     * 查找Agent
     *
     * @param agentName Agent名称或ID
     * @return Agent实例
     */
    private Agent findAgent(String agentName) {
        return agentManager.getAllAgents().stream()
            .filter(a -> a.getName().contains(agentName) || a.getId().contains(agentName))
            .findFirst()
            .orElse(null);
    }

    /**
     * 查找项目
     *
     * @param projectName 项目名称或ID
     * @return 项目实例
     */
    private GameProject findProject(String projectName) {
        return projectManager.getAllProjects().stream()
            .filter(p -> p.getName().contains(projectName) || p.getId().contains(projectName))
            .findFirst()
            .orElse(null);
    }

    /**
     * 查找流水线
     *
     * @param pipelineNo 流水线编号
     * @return 流水线实例
     */
    private Pipeline findPipeline(String pipelineNo) {
        return pipelineService.getAllPipelines().stream()
            .filter(p -> p.getPipelineNo().equals(pipelineNo))
            .findFirst()
            .orElse(null);
    }

    /**
     * 获取流水线类型名称
     *
     * @param type 类型代码
     * @return 类型名称
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
