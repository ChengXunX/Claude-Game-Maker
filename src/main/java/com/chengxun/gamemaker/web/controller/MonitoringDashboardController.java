package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.entity.ProjectMember;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.AgentHealthService;
import com.chengxun.gamemaker.web.service.AlertService;
import com.chengxun.gamemaker.web.service.MetricsCollectorService;
import com.chengxun.gamemaker.web.service.ProjectPermissionService;
import com.chengxun.gamemaker.web.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 监控Dashboard控制器
 * 提供监控数据的REST API
 *
 * 主要功能：
 * - 获取系统监控指标
 * - 获取告警统计
 * - 获取Agent健康状态
 *
 * @author chengxun
 * @since 1.0.0
 */
@Tag(name = "监控Dashboard", description = "系统监控数据API")
@RestController
@RequestMapping("/api/monitoring")
public class MonitoringDashboardController {

    @Autowired
    private MetricsCollectorService metricsService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private AgentHealthService healthService;

    @Autowired
    private ProjectPermissionService permissionService;

    @Autowired
    private UserService userService;

    /**
     * 获取系统概览
     *
     * @return 系统概览数据
     */
    @Operation(summary = "获取系统概览", description = "获取系统监控概览数据")
    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    public ResponseEntity<Map<String, Object>> getSystemOverview() {
        Map<String, Object> overview = new HashMap<>();

        // API调用统计
        overview.put("apiCalls", metricsService.getApiCallStatistics());

        // Agent任务统计
        overview.put("agentTasks", metricsService.getAgentTaskStatistics());

        // 错误统计
        overview.put("errors", metricsService.getErrorStatistics());

        // 告警统计
        overview.put("alerts", alertService.getAlertStatistics());

        // Agent健康统计
        overview.put("agentHealth", healthService.getHealthStatistics());

        return ResponseEntity.ok(overview);
    }

    /**
     * 获取API调用指标
     *
     * @return API调用指标
     */
    @Operation(summary = "获取API调用指标", description = "获取API调用统计")
    @GetMapping("/api-calls")
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    public ResponseEntity<Map<String, Object>> getApiCallMetrics() {
        return ResponseEntity.ok(metricsService.getApiCallStatistics());
    }

    /**
     * 获取Agent任务指标
     *
     * @return Agent任务指标
     */
    @Operation(summary = "获取Agent任务指标", description = "获取Agent任务统计")
    @GetMapping("/agent-tasks")
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    public ResponseEntity<Map<String, Object>> getAgentTaskMetrics() {
        return ResponseEntity.ok(metricsService.getAgentTaskStatistics());
    }

    /**
     * 获取错误指标
     *
     * @return 错误指标
     */
    @Operation(summary = "获取错误指标", description = "获取错误统计")
    @GetMapping("/errors")
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    public ResponseEntity<Map<String, Object>> getErrorMetrics() {
        return ResponseEntity.ok(metricsService.getErrorStatistics());
    }

    /**
     * 获取告警统计
     *
     * @return 告警统计
     */
    @Operation(summary = "获取告警统计", description = "获取告警统计数据")
    @GetMapping("/alerts")
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    public ResponseEntity<Map<String, Object>> getAlertStatistics() {
        return ResponseEntity.ok(alertService.getAlertStatistics());
    }

    /**
     * 获取Agent健康状态
     *
     * @return Agent健康状态
     */
    @Operation(summary = "获取Agent健康状态", description = "获取Agent健康状态统计")
    @GetMapping("/agent-health")
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    public ResponseEntity<Map<String, Object>> getAgentHealth() {
        return ResponseEntity.ok(healthService.getHealthStatistics());
    }

    /**
     * 重置监控计数器
     *
     * @return 操作结果
     */
    @Operation(summary = "重置计数器", description = "重置所有监控计数器")
    @PostMapping("/reset")
    @PreAuthorize("hasAuthority('PERM_system:monitor:manage')")
    public ResponseEntity<Map<String, Object>> resetCounters() {
        metricsService.resetCounters();

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "计数器已重置"
        ));
    }

    /**
     * 获取项目的 Agent 健康状态
     */
    @Operation(summary = "获取项目 Agent 健康", description = "获取指定项目的 Agent 健康状态")
    @GetMapping("/project/{projectId}/agent-health")
    public ResponseEntity<?> getProjectAgentHealth(
            @PathVariable String projectId, Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.VIEWER)) {
            return ResponseEntity.status(403).body(com.chengxun.gamemaker.web.dto.ErrorResponse.forbidden("无权限访问该项目"));
        }
        return ResponseEntity.ok(healthService.getProjectHealthStatistics(projectId));
    }
}
