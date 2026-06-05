package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.agent.Agent;
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
     * 获取所有 Agent 列表
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
                info.put("tasks", agent.getTasks().size());
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
        info.put("tasks", agent.getTasks().size());
        info.put("projectId", projectId);
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
}
