package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.service.AgentScheduler;
import com.chengxun.gamemaker.web.entity.ProjectMember;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.ProjectPermissionService;
import com.chengxun.gamemaker.web.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Agent调度控制器
 *
 * 权限模型：
 * - agentId 是运行时 ID（projectId:agentRole）
 * - 操作 Agent 时校验用户对该项目的权限
 *
 * @author chengxun
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/agent-scheduler")
public class AgentSchedulerController {

    private static final Logger log = LoggerFactory.getLogger(AgentSchedulerController.class);

    @Autowired
    private AgentScheduler agentScheduler;

    @Autowired
    private ProjectPermissionService permissionService;

    @Autowired
    private UserService userService;

    @PostMapping("/{agentId}/compact")
    @PreAuthorize("hasAnyAuthority('PERM_agents:manage', 'PERM_admin:manage')")
    public ResponseEntity<?> compactContext(@PathVariable String agentId,
                                                               Authentication authentication) {
        if (!checkAgentAccess(agentId, authentication, ProjectMember.ProjectRole.DEVELOPER)) {
            return ResponseEntity.status(403).body(com.chengxun.gamemaker.web.dto.ErrorResponse.forbidden("无权限操作该 Agent"));
        }
        log.info("Compacting context for agent: {}", agentId);
        String summary = agentScheduler.compactAgentContext(agentId);
        return ResponseEntity.ok(Map.of("status", "success", "agentId", agentId, "summary", summary));
    }

    @PostMapping("/{agentId}/recover")
    @PreAuthorize("hasAnyAuthority('PERM_agents:manage', 'PERM_admin:manage')")
    public ResponseEntity<?> recoverContext(@PathVariable String agentId,
                                                               Authentication authentication) {
        if (!checkAgentAccess(agentId, authentication, ProjectMember.ProjectRole.DEVELOPER)) {
            return ResponseEntity.status(403).body(com.chengxun.gamemaker.web.dto.ErrorResponse.forbidden("无权限操作该 Agent"));
        }
        log.info("Recovering context for agent: {}", agentId);
        String recoveryPrompt = agentScheduler.recoverAgentContext(agentId);
        return ResponseEntity.ok(Map.of("status", "success", "agentId", agentId, "recoveryPrompt", recoveryPrompt));
    }

    @GetMapping("/{agentId}/compact-history")
    @PreAuthorize("hasAnyAuthority('PERM_agents:view', 'PERM_agents:manage', 'PERM_admin:manage')")
    public ResponseEntity<List<Map<String, Object>>> getCompactHistory(@PathVariable String agentId,
                                                                        Authentication authentication) {
        if (!checkAgentAccess(agentId, authentication, ProjectMember.ProjectRole.VIEWER)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(agentScheduler.getAgentCompactHistory(agentId));
    }

    @GetMapping("/{agentId}/status")
    @PreAuthorize("hasAnyAuthority('PERM_agents:view', 'PERM_agents:manage', 'PERM_admin:manage')")
    public ResponseEntity<?> getAgentStatus(@PathVariable String agentId,
                                                               Authentication authentication) {
        if (!checkAgentAccess(agentId, authentication, ProjectMember.ProjectRole.VIEWER)) {
            return ResponseEntity.status(403).build();
        }
        Map<String, Object> status = agentScheduler.getAgentStatus(agentId);
        if ("NOT_FOUND".equals(status.get("status"))) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyAuthority('PERM_agents:view', 'PERM_agents:manage', 'PERM_admin:manage')")
    public ResponseEntity<Map<String, Object>> getAllAgentStatus() {
        List<Map<String, Object>> agents = agentScheduler.getAllAgentStatus();
        Map<String, Object> summary = new java.util.HashMap<>();
        summary.put("totalAgents", agents.size());
        summary.put("runningAgents", agents.stream().filter(a -> Boolean.TRUE.equals(a.get("alive"))).count());
        summary.put("idleAgents", agents.stream().filter(a -> Boolean.TRUE.equals(a.get("alive")) && !Boolean.TRUE.equals(a.get("busy"))).count());
        summary.put("busyAgents", agents.stream().filter(a -> Boolean.TRUE.equals(a.get("busy"))).count());
        summary.put("agents", agents);
        return ResponseEntity.ok(summary);
    }

    /**
     * 获取任务队列
     */
    @GetMapping("/tasks")
    @PreAuthorize("hasAnyAuthority('PERM_agents:view', 'PERM_agents:manage', 'PERM_admin:manage')")
    public ResponseEntity<List<Map<String, Object>>> getTaskQueue() {
        return ResponseEntity.ok(agentScheduler.getTaskQueue());
    }

    /**
     * 获取调度配置
     */
    @GetMapping("/config")
    @PreAuthorize("hasAnyAuthority('PERM_agents:view', 'PERM_agents:manage', 'PERM_admin:manage')")
    public ResponseEntity<Map<String, Object>> getConfig() {
        return ResponseEntity.ok(agentScheduler.getSchedulerConfig());
    }

    /**
     * 更新调度配置
     */
    @PutMapping("/config")
    @PreAuthorize("hasAnyAuthority('PERM_agents:manage', 'PERM_admin:manage')")
    public ResponseEntity<Map<String, Object>> updateConfig(@RequestBody Map<String, Object> config) {
        agentScheduler.updateSchedulerConfig(config);
        return ResponseEntity.ok(Map.of("status", "success", "message", "配置已更新"));
    }

    /**
     * 手动触发调度
     */
    @PostMapping("/trigger")
    @PreAuthorize("hasAnyAuthority('PERM_agents:manage', 'PERM_admin:manage')")
    public ResponseEntity<Map<String, Object>> triggerSchedule() {
        agentScheduler.triggerSchedule();
        return ResponseEntity.ok(Map.of("status", "success", "message", "调度已触发"));
    }

    /**
     * 取消任务
     */
    @PostMapping("/tasks/{taskId}/cancel")
    @PreAuthorize("hasAnyAuthority('PERM_agents:manage', 'PERM_admin:manage')")
    public ResponseEntity<Map<String, Object>> cancelTask(@PathVariable String taskId) {
        try {
            boolean cancelled = agentScheduler.cancelTask(taskId);
            if (cancelled) {
                return ResponseEntity.ok(Map.of("status", "success", "message", "任务已取消"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "任务不存在或无法取消"));
            }
        } catch (Exception e) {
            log.error("取消任务失败: {}", taskId, e);
            return ResponseEntity.internalServerError().body(Map.of("status", "error", "message", "取消失败: " + e.getMessage()));
        }
    }

    @PostMapping("/{agentId}/start")
    @PreAuthorize("hasAnyAuthority('PERM_agents:manage', 'PERM_admin:manage')")
    public ResponseEntity<?> startAgent(@PathVariable String agentId,
                                                           Authentication authentication) {
        if (!checkAgentAccess(agentId, authentication, ProjectMember.ProjectRole.MANAGER)) {
            return ResponseEntity.status(403).body(com.chengxun.gamemaker.web.dto.ErrorResponse.forbidden("无权限操作该 Agent"));
        }
        agentScheduler.startAgent(agentId);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Agent已启动: " + agentId));
    }

    @PostMapping("/{agentId}/stop")
    @PreAuthorize("hasAnyAuthority('PERM_agents:manage', 'PERM_admin:manage')")
    public ResponseEntity<?> stopAgent(@PathVariable String agentId,
                                                          Authentication authentication) {
        if (!checkAgentAccess(agentId, authentication, ProjectMember.ProjectRole.MANAGER)) {
            return ResponseEntity.status(403).body(com.chengxun.gamemaker.web.dto.ErrorResponse.forbidden("无权限操作该 Agent"));
        }
        agentScheduler.stopAgent(agentId);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Agent已停止: " + agentId));
    }

    /**
     * 检查用户是否有权限操作指定 Agent
     */
    private boolean checkAgentAccess(String agentId, Authentication authentication,
                                      ProjectMember.ProjectRole requiredRole) {
        User user = userService.getUserByUsername(authentication.getName());
        if (user == null) return false;
        if (user.isAdmin()) return true;

        String projectId = extractProjectId(agentId);
        if (projectId == null) return true;

        return permissionService.hasProjectAccess(user, projectId, requiredRole);
    }

    private String extractProjectId(String runtimeId) {
        if (runtimeId == null) return null;
        int lastColon = runtimeId.lastIndexOf(':');
        return lastColon > 0 ? runtimeId.substring(0, lastColon) : null;
    }
}
