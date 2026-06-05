package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.web.service.DeepHealthCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.chengxun.gamemaker.web.entity.ProjectMember;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.ProjectPermissionService;
import com.chengxun.gamemaker.web.service.UserService;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统健康检查控制器
 * 提供系统状态和健康检查接口
 *
 * @author chengxun
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/system")
@Tag(name = "系统健康检查", description = "系统状态和健康检查接口")
public class HealthController {

    private final AgentManager agentManager;
    private final ProjectManager projectManager;
    private final ProjectPermissionService permissionService;
    private final UserService userService;
    private final DeepHealthCheckService deepHealthCheckService;

    public HealthController(AgentManager agentManager, ProjectManager projectManager,
                           ProjectPermissionService permissionService, UserService userService,
                           DeepHealthCheckService deepHealthCheckService) {
        this.agentManager = agentManager;
        this.projectManager = projectManager;
        this.permissionService = permissionService;
        this.userService = userService;
        this.deepHealthCheckService = deepHealthCheckService;
    }

    /**
     * 获取系统健康状态
     *
     * @return 系统健康信息
     */
    @GetMapping
    @Operation(summary = "系统健康检查", description = "返回系统整体健康状态")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        
        // 检查各个组件状态
        Map<String, Object> components = new HashMap<>();
        
        // 数据库状态
        components.put("database", checkDatabaseHealth());
        
        // 缓存状态
        components.put("cache", checkCacheHealth());
        
        // Agent 状态
        components.put("agents", checkAgentsHealth());
        
        health.put("components", components);
        
        return ResponseEntity.ok(health);
    }

    /**
     * 获取系统详细信息
     *
     * @return 系统详细信息
     */
    @GetMapping("/info")
    @Operation(summary = "系统信息", description = "返回系统详细配置信息")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "ChengXun Game Maker");
        info.put("version", "1.0.0");
        info.put("timestamp", LocalDateTime.now());

        // 操作系统信息
        info.put("osName", System.getProperty("os.name"));
        info.put("osArch", System.getProperty("os.arch"));
        info.put("osVersion", System.getProperty("os.version"));

        // JVM 信息
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> jvm = new HashMap<>();
        jvm.put("javaVersion", System.getProperty("java.version"));
        jvm.put("maxMemory", runtime.maxMemory() / 1024 / 1024 + " MB");
        jvm.put("totalMemory", runtime.totalMemory() / 1024 / 1024 + " MB");
        jvm.put("freeMemory", runtime.freeMemory() / 1024 / 1024 + " MB");
        jvm.put("availableProcessors", runtime.availableProcessors());
        jvm.put("usedMemory", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024 + " MB");
        info.put("jvm", jvm);

        // Agent 统计
        Map<String, Object> agentStats = new java.util.HashMap<>();
        List<?> agents = agentManager.getAllAgents();
        agentStats.put("total", agents.size());
        agentStats.put("busy", agents.stream().filter(a -> ((com.chengxun.gamemaker.agent.Agent) a).isBusy()).count());
        agentStats.put("alive", agents.stream().filter(a -> ((com.chengxun.gamemaker.agent.Agent) a).isAlive()).count());
        info.put("agents", agentStats);

        // 项目统计
        info.put("totalProjects", projectManager.getAllProjects().size());

        return ResponseEntity.ok(info);
    }

    /**
     * 检查数据库健康状态
     *
     * @return 数据库状态信息
     */
    private Map<String, Object> checkDatabaseHealth() {
        Map<String, Object> dbHealth = new HashMap<>();
        try {
            // 这里可以添加实际的数据库连接检查
            dbHealth.put("status", "UP");
            dbHealth.put("database", "MySQL");
        } catch (Exception e) {
            dbHealth.put("status", "DOWN");
            dbHealth.put("error", e.getMessage());
        }
        return dbHealth;
    }

    /**
     * 检查缓存健康状态
     *
     * @return 缓存状态信息
     */
    private Map<String, Object> checkCacheHealth() {
        Map<String, Object> cacheHealth = new HashMap<>();
        try {
            // 这里可以添加实际的 Redis 连接检查
            cacheHealth.put("status", "UP");
            cacheHealth.put("type", "Redis");
        } catch (Exception e) {
            cacheHealth.put("status", "DOWN");
            cacheHealth.put("error", e.getMessage());
        }
        return cacheHealth;
    }

    /**
     * 获取项目的 Agent 健康状态
     *
     * @param projectId 项目 ID
     * @return 项目的 Agent 状态信息
     */
    @GetMapping("/project/{projectId}")
    @Operation(summary = "项目 Agent 健康检查")
    public ResponseEntity<?> projectHealth(@PathVariable String projectId,
                                                              Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.VIEWER)) {
            return ResponseEntity.status(403).body(com.chengxun.gamemaker.web.dto.ErrorResponse.forbidden("无权限访问该项目"));
        }

        Map<String, Object> health = new HashMap<>();
        List<?> agents = agentManager.getAgentsByProject(projectId);
        health.put("projectId", projectId);
        health.put("totalAgents", agents.size());
        health.put("aliveAgents", agents.stream().filter(a -> ((com.chengxun.gamemaker.agent.Agent) a).isAlive()).count());
        health.put("busyAgents", agents.stream().filter(a -> ((com.chengxun.gamemaker.agent.Agent) a).isBusy()).count());
        health.put("status", agents.isEmpty() ? "DOWN" : "UP");
        health.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(health);
    }

    /**
     * 检查 Agent 健康状态
     *
     * @return Agent 状态信息
     */
    private Map<String, Object> checkAgentsHealth() {
        Map<String, Object> agentHealth = new HashMap<>();
        List<?> agents = agentManager.getAllAgents();
        agentHealth.put("total", agents.size());
        agentHealth.put("status", agents.isEmpty() ? "DOWN" : "UP");
        return agentHealth;
    }

    /**
     * 获取深度健康检查结果
     * 包含数据库、Agent、磁盘、内存等详细信息
     */
    @GetMapping("/health/deep")
    @Operation(summary = "深度健康检查", description = "返回系统各组件的详细健康状态")
    @PreAuthorize("hasAnyAuthority('PERM_system:monitor', 'PERM_admin:manage')")
    public ResponseEntity<Map<String, Object>> deepHealth() {
        return ResponseEntity.ok(deepHealthCheckService.getDetailedHealth());
    }
}
