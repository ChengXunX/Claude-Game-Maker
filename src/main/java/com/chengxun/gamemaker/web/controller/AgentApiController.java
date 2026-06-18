package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.model.AgentDefinition;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.web.entity.ProjectMember;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.OperationLogService;
import com.chengxun.gamemaker.web.service.ProjectPermissionService;
import com.chengxun.gamemaker.web.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent API 控制器
 * 提供 Agent 的 REST API 接口（返回 JSON）
 *
 * @author chengxun
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/agents")
@Tag(name = "Agent API", description = "Agent 管理接口")
public class AgentApiController {

    private static final Logger log = LoggerFactory.getLogger(AgentApiController.class);

    private final AgentManager agentManager;
    private final ProjectManager projectManager;
    private final UserService userService;
    private final OperationLogService logService;
    private final ProjectPermissionService permissionService;

    public AgentApiController(AgentManager agentManager, ProjectManager projectManager,
                             UserService userService, OperationLogService logService,
                             ProjectPermissionService permissionService) {
        this.agentManager = agentManager;
        this.projectManager = projectManager;
        this.userService = userService;
        this.logService = logService;
        this.permissionService = permissionService;
    }

    /**
     * 获取所有 Agent 列表（精简版，避免加载大量任务数据导致序列化失败）
     * 需要agents:view权限
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_agents:view')")
    @Operation(summary = "获取所有 Agent")
    public ResponseEntity<List<Map<String, Object>>> getAllAgents() {
        List<Map<String, Object>> agents = agentManager.getAllAgents().stream()
            .map(agent -> {
                Map<String, Object> info = new HashMap<>();
                info.put("id", agent.getId());
                info.put("name", agent.getName());
                info.put("role", agent.getRole());
                info.put("busy", agent.isBusy());
                info.put("alive", agent.isAlive());
                // 使用 try-catch 避免任务数据加载失败导致整个 API 失败
                try {
                    info.put("tasks", agent.getTasks() != null ? agent.getTasks().size() : 0);
                } catch (Exception e) {
                    info.put("tasks", 0);
                }
                return info;
            })
            .toList();
        return ResponseEntity.ok(agents);
    }

    /**
     * 获取项目下的 Agent 列表
     * 需要agents:view权限
     */
    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAuthority('PERM_agents:view')")
    @Operation(summary = "获取项目 Agent 列表")
    public ResponseEntity<List<Map<String, Object>>> getProjectAgents(@PathVariable String projectId) {
        List<Map<String, Object>> agents = agentManager.getProjectAgentStatuses(projectId);
        return ResponseEntity.ok(agents);
    }

    /**
     * 获取 Agent 详情
     * 返回 Agent 的完整信息，包括任务列表、工作记忆、推理深度等
     */
    @GetMapping("/project/{projectId}/{agentRole}")
    @Operation(summary = "获取 Agent 详情")
    public ResponseEntity<Map<String, Object>> getAgentDetail(@PathVariable String projectId,
                                                              @PathVariable String agentRole) {
        Agent agent = agentManager.getAgent(projectId, agentRole);
        if (agent == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> info = new HashMap<>();
        info.put("id", agent.getId());
        info.put("name", agent.getName());
        info.put("role", agent.getRole());
        info.put("busy", agent.isBusy());
        info.put("alive", agent.isAlive());
        info.put("projectId", projectId);

        // 任务列表
        List<Map<String, Object>> taskList = agent.getTasks().stream().map(t -> {
            Map<String, Object> task = new HashMap<>();
            task.put("id", t.getId());
            task.put("title", t.getTitle());
            task.put("status", t.getStatus().name());
            task.put("priority", t.getPriority().name());
            task.put("createdAt", t.getCreatedAt());
            task.put("completedAt", t.getCompletedAt());
            return task;
        }).toList();
        info.put("tasks", taskList);
        info.put("taskCount", taskList.size());

        // 当前任务
        var currentTask = agent.getTasks().stream()
            .filter(t -> t.getStatus() == com.chengxun.gamemaker.model.TaskAssignment.TaskStatus.IN_PROGRESS)
            .findFirst().orElse(null);
        if (currentTask != null) {
            Map<String, Object> current = new HashMap<>();
            current.put("id", currentTask.getId());
            current.put("title", currentTask.getTitle());
            current.put("description", currentTask.getDescription());
            current.put("status", currentTask.getStatus().name());
            info.put("currentTask", current);
        }

        // 推理深度
        if (agent instanceof com.chengxun.gamemaker.agent.BaseAgent baseAgent) {
            info.put("reasoningDepth", baseAgent.getDefinition().getReasoningDepth());
            info.put("workDir", baseAgent.getDefinition().getWorkDir());
        }

        // 待处理消息数
        info.put("pendingMessages", agent.getPendingMessages().size());

        return ResponseEntity.ok(info);
    }

    /**
     * 启动 Agent
     */
    @PostMapping("/project/{projectId}/{agentRole}/start")
    @Operation(summary = "启动 Agent")
    public ResponseEntity<Map<String, Object>> startAgent(@PathVariable String projectId,
                                                          @PathVariable String agentRole,
                                                          Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.MANAGER)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "权限不足"));
        }

        Agent agent = agentManager.getAgent(projectId, agentRole);
        if (agent == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Agent 不存在"));
        }

        if (agent.isAlive()) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Agent 已在运行中"));
        }

        try {
            agent.start();
            logService.log(user.getId(), "START_AGENT", agent.getName(), "Started agent", null);
            return ResponseEntity.ok(Map.of("success", true, "message", "Agent " + agent.getName() + " 已启动"));
        } catch (Exception e) {
            log.error("启动 Agent 失败", e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "启动失败: " + e.getMessage()));
        }
    }

    /**
     * 停止 Agent
     */
    @PostMapping("/project/{projectId}/{agentRole}/stop")
    @Operation(summary = "停止 Agent")
    public ResponseEntity<Map<String, Object>> stopAgent(@PathVariable String projectId,
                                                         @PathVariable String agentRole,
                                                         Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.MANAGER)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "权限不足"));
        }

        Agent agent = agentManager.getAgent(projectId, agentRole);
        if (agent == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Agent 不存在"));
        }

        if (!agent.isAlive()) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Agent 已停止"));
        }

        try {
            agent.stop();
            logService.log(user.getId(), "STOP_AGENT", agent.getName(), "Stopped agent", null);
            return ResponseEntity.ok(Map.of("success", true, "message", "Agent " + agent.getName() + " 已停止"));
        } catch (Exception e) {
            log.error("停止 Agent 失败", e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "停止失败: " + e.getMessage()));
        }
    }

    /**
     * 重启 Agent
     */
    @PostMapping("/project/{projectId}/{agentRole}/restart")
    @Operation(summary = "重启 Agent")
    public ResponseEntity<Map<String, Object>> restartAgent(@PathVariable String projectId,
                                                            @PathVariable String agentRole,
                                                            Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.MANAGER)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "权限不足"));
        }

        Agent agent = agentManager.getAgent(projectId, agentRole);
        if (agent == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Agent 不存在"));
        }

        try {
            if (agent.isAlive()) {
                agent.stop();
                Thread.sleep(1000);
            }
            agent.start();
            logService.log(user.getId(), "RESTART_AGENT", agent.getName(), "Restarted agent", null);
            return ResponseEntity.ok(Map.of("success", true, "message", "Agent " + agent.getName() + " 已重启"));
        } catch (Exception e) {
            log.error("重启 Agent 失败", e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "重启失败: " + e.getMessage()));
        }
    }

    /**
     * 发送任务给 Agent
     */
    @PostMapping("/project/{projectId}/{agentRole}/task")
    @Operation(summary = "发送任务")
    public ResponseEntity<Map<String, Object>> sendTask(@PathVariable String projectId,
                                                        @PathVariable String agentRole,
                                                        @RequestBody Map<String, String> request,
                                                        Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.MANAGER)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "权限不足"));
        }

        Agent agent = agentManager.getAgent(projectId, agentRole);
        if (agent == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Agent 不存在"));
        }

        String taskContent = request.get("content");
        if (taskContent == null || taskContent.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "任务内容不能为空"));
        }

        try {
            // 使用 AgentMessage 发送任务
            com.chengxun.gamemaker.model.AgentMessage taskMsg = com.chengxun.gamemaker.model.AgentMessage.createTask(
                "user", agent.getId(), taskContent);
            agent.receiveMessage(taskMsg);
            logService.log(user.getId(), "SEND_TASK", agent.getName(), "Task sent", null);
            return ResponseEntity.ok(Map.of("success", true, "message", "任务已发送"));
        } catch (Exception e) {
            log.error("发送任务失败", e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "发送失败: " + e.getMessage()));
        }
    }

    /**
     * 向 Agent 提问
     */
    @PostMapping("/project/{projectId}/{agentRole}/query")
    @Operation(summary = "向 Agent 提问")
    public ResponseEntity<Map<String, Object>> queryAgent(@PathVariable String projectId,
                                                          @PathVariable String agentRole,
                                                          @RequestBody Map<String, String> request,
                                                          Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.VIEWER)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "权限不足"));
        }

        Agent agent = agentManager.getAgent(projectId, agentRole);
        if (agent == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Agent 不存在"));
        }

        String queryContent = request.get("content");
        if (queryContent == null || queryContent.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "问题内容不能为空"));
        }

        try {
            // 使用 AgentMessage 发送查询
            com.chengxun.gamemaker.model.AgentMessage queryMsg = com.chengxun.gamemaker.model.AgentMessage.createTask(
                "user", agent.getId(), queryContent);
            agent.receiveMessage(queryMsg);
            return ResponseEntity.ok(Map.of("success", true, "message", "问题已发送"));
        } catch (Exception e) {
            log.error("发送问题失败", e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "发送失败: " + e.getMessage()));
        }
    }

    /**
     * 设置 Agent 推理深度（全局生效）
     *
     * @param projectId 项目 ID
     * @param agentRole Agent 角色
     * @param request 包含 reasoningDepth 的请求体
     * @return 操作结果
     */
    @PutMapping("/project/{projectId}/{agentRole}/reasoning-depth")
    @Operation(summary = "设置推理深度", description = "设置 Agent 的推理深度，全局生效（1=快速 2=标准 3=深入 4=全面 5=极致）")
    public ResponseEntity<Map<String, Object>> setReasoningDepth(
            @PathVariable String projectId,
            @PathVariable String agentRole,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.MANAGER)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "权限不足"));
        }

        Object depthObj = request.get("reasoningDepth");
        if (depthObj == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "推理深度不能为空"));
        }

        int depth;
        try {
            depth = Integer.parseInt(depthObj.toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "推理深度必须是 1-5 的整数"));
        }

        if (depth < 1 || depth > 5) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "推理深度必须在 1-5 之间"));
        }

        boolean success = agentManager.setReasoningDepth(projectId, agentRole, depth);
        if (success) {
            String depthLabel = com.chengxun.gamemaker.model.AgentDefinition.getReasoningDepthLabel(depth);
            logService.log(user.getId(), "SET_REASONING_DEPTH", agentRole,
                "Set reasoning depth to " + depth + " (" + depthLabel + ")", null);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "推理深度已设置为: " + depthLabel,
                "reasoningDepth", depth,
                "reasoningDepthLabel", depthLabel
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Agent 不存在"));
        }
    }

    /**
     * 获取 Agent 推理深度
     *
     * @param projectId 项目 ID
     * @param agentRole Agent 角色
     * @return 推理深度信息
     */
    @GetMapping("/project/{projectId}/{agentRole}/reasoning-depth")
    @Operation(summary = "获取推理深度")
    public ResponseEntity<Map<String, Object>> getReasoningDepth(
            @PathVariable String projectId,
            @PathVariable String agentRole) {
        int depth = agentManager.getReasoningDepth(projectId, agentRole);
        if (depth == -1) {
            return ResponseEntity.notFound().build();
        }
        String depthLabel = com.chengxun.gamemaker.model.AgentDefinition.getReasoningDepthLabel(depth);
        return ResponseEntity.ok(Map.of(
            "reasoningDepth", depth,
            "reasoningDepthLabel", depthLabel
        ));
    }

    /**
     * 设置 Agent 思维模式
     *
     * @param projectId 项目 ID
     * @param agentRole Agent 角色
     * @param request   包含 thinkingMode 的请求体
     * @return 操作结果
     */
    @PutMapping("/project/{projectId}/{agentRole}/thinking-mode")
    @Operation(summary = "设置思维模式", description = "设置 Agent 的思维模式（1-5）：1=高度严谨 2=严谨 3=平衡 4=创新 5=突破")
    public ResponseEntity<Map<String, Object>> setThinkingMode(
            @PathVariable String projectId,
            @PathVariable String agentRole,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.MANAGER)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "权限不足"));
        }

        Object modeObj = request.get("thinkingMode");
        if (modeObj == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "思维模式值不能为空"));
        }

        int thinkingMode;
        try {
            thinkingMode = Integer.parseInt(modeObj.toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "思维模式值必须是 1-5 的整数"));
        }

        if (thinkingMode < 1 || thinkingMode > 5) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "思维模式值必须在 1-5 之间"));
        }

        Agent agent = agentManager.getAgent(projectId, agentRole);
        if (agent == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Agent 不存在"));
        }

        agent.getDefinition().setThinkingMode(thinkingMode);
        String modeLabel = AgentDefinition.getThinkingModeLabel(thinkingMode);
        logService.log(user.getId(), "SET_THINKING_MODE", agentRole,
            "Set thinking mode to " + thinkingMode + " (" + modeLabel + ")", null);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", String.format("思维模式已设置为 %s", modeLabel),
            "thinkingMode", thinkingMode,
            "thinkingModeLabel", modeLabel
        ));
    }
}
