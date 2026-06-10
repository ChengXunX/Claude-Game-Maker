package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.manager.*;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.model.Skill;
import com.chengxun.gamemaker.service.GameTemplateService;
import com.chengxun.gamemaker.web.entity.McpServer;
import com.chengxun.gamemaker.web.service.*;
import com.chengxun.gamemaker.web.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 内部 API 控制器
 * 提供给 MCP 服务器使用的 API 端点
 * 通过请求头中的 JWT Token 进行手动鉴权
 *
 * @author chengxun
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/internal")
@Tag(name = "内部API", description = "提供给 MCP 服务器使用的 API 端点")
public class InternalApiController {

    private static final Logger log = LoggerFactory.getLogger(InternalApiController.class);

    @Autowired
    private AgentManager agentManager;

    @Autowired
    private ProjectManager projectManager;

    @Autowired
    private SkillManager skillManager;

    @Autowired
    private WorkflowEngine workflowEngine;

    @Autowired
    private GameTemplateService gameTemplateService;

    @Autowired
    private AgentHealthService agentHealthService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 手动验证 JWT Token
     * 从请求头中提取 Token 并验证有效性
     *
     * @param request HTTP 请求
     * @return 验证通过返回用户名，失败返回 null
     */
    private String authenticate(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("内部API调用缺少 Authorization 头");
            return null;
        }
        String token = authHeader.substring(7);
        if (!jwtUtils.validateToken(token)) {
            log.warn("内部API调用 Token 无效");
            return null;
        }
        String username = jwtUtils.getUsernameFromToken(token);
        log.info("内部API鉴权成功: {}", username);
        return username;
    }

    /**
     * 返回未授权错误
     */
    private Map<String, Object> unauthorized() {
        return Map.of("success", false, "error", "未授权：请提供有效的 JWT Token");
    }

    /**
     * 获取所有工作流模板
     */
    @GetMapping("/workflow-templates")
    @Operation(summary = "获取工作流模板列表", description = "获取所有工作流模板")
    public Map<String, Object> getWorkflowTemplates(HttpServletRequest request) {
        if (authenticate(request) == null) return unauthorized();
        log.info("内部API: 获取工作流模板列表");
        var templates = workflowEngine.getAllTemplates();
        List<Map<String, Object>> list = new ArrayList<>();
        for (var tpl : templates) {
            Map<String, Object> info = new HashMap<>();
            info.put("id", tpl.getId());
            info.put("name", tpl.getName());
            info.put("description", tpl.getDescription());
            info.put("stepCount", tpl.getSteps() != null ? tpl.getSteps().size() : 0);
            list.add(info);
        }
        return Map.of("success", true, "templates", list, "total", list.size());
    }

    /**
     * 创建工作流模板
     */
    @PostMapping("/workflow-templates")
    @Operation(summary = "创建工作流模板", description = "创建新的工作流模板")
    public Map<String, Object> createWorkflowTemplate(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        if (authenticate(request) == null) return unauthorized();
        String id = (String) params.get("id");
        String name = (String) params.get("name");
        String description = (String) params.getOrDefault("description", "");
        log.info("内部API: 创建工作流模板 - id: {}, name: {}", id, name);
        try {
            var template = workflowEngine.createTemplate(id, name, description, null);
            return Map.of("success", true, "message", "工作流模板创建成功: " + name, "templateId", template.getId());
        } catch (Exception e) {
            return Map.of("success", false, "error", "创建工作流模板失败: " + e.getMessage());
        }
    }

    /**
     * 删除工作流模板
     */
    @DeleteMapping("/workflow-templates/{templateId}")
    @Operation(summary = "删除工作流模板", description = "删除指定的工作流模板")
    public Map<String, Object> deleteWorkflowTemplate(HttpServletRequest request, @PathVariable String templateId) {
        if (authenticate(request) == null) return unauthorized();
        log.info("内部API: 删除工作流模板 - templateId: {}", templateId);
        try {
            boolean deleted = workflowEngine.deleteTemplate(templateId);
            if (deleted) {
                return Map.of("success", true, "message", "工作流模板已删除: " + templateId);
            } else {
                return Map.of("success", false, "error", "模板不存在或为内置模板，无法删除");
            }
        } catch (Exception e) {
            return Map.of("success", false, "error", "删除失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有 Agent 列表
     */
    @GetMapping("/agents")
    @Operation(summary = "获取 Agent 列表", description = "获取所有 Agent 的状态信息")
    public Map<String, Object> getAgents(HttpServletRequest request) {
        if (authenticate(request) == null) return unauthorized();
        log.info("内部API: 获取 Agent 列表");
        var agents = agentManager.getAllAgents();
        List<Map<String, Object>> list = new ArrayList<>();
        for (var agent : agents) {
            Map<String, Object> info = new HashMap<>();
            info.put("id", agent.getId());
            info.put("name", agent.getName());
            info.put("role", agent.getRole());
            info.put("alive", agent.isAlive());
            info.put("busy", agent.isBusy());
            list.add(info);
        }
        return Map.of("success", true, "agents", list, "total", list.size());
    }

    /**
     * 获取 Agent 健康状态
     */
    @GetMapping("/agents/health")
    @Operation(summary = "获取 Agent 健康状态", description = "获取所有 Agent 的健康状态")
    public Map<String, Object> getAgentHealth(HttpServletRequest request) {
        if (authenticate(request) == null) return unauthorized();
        log.info("内部API: 获取 Agent 健康状态");
        try {
            var healthList = agentHealthService.getAllAgentHealth();
            return Map.of("success", true, "health", healthList, "total", healthList.size());
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * 获取所有项目列表
     */
    @GetMapping("/projects")
    @Operation(summary = "获取项目列表", description = "获取所有游戏项目")
    public Map<String, Object> getProjects(HttpServletRequest request) {
        if (authenticate(request) == null) return unauthorized();
        log.info("内部API: 获取项目列表");
        var projects = projectManager.getAllProjects();
        List<Map<String, Object>> list = new ArrayList<>();
        for (var project : projects) {
            Map<String, Object> info = new HashMap<>();
            info.put("id", project.getId());
            info.put("name", project.getName());
            info.put("status", project.getStatus());
            info.put("goal", project.getGoal());
            list.add(info);
        }
        return Map.of("success", true, "projects", list, "total", list.size());
    }

    /**
     * 创建项目
     */
    @PostMapping("/projects")
    @Operation(summary = "创建项目", description = "创建新的游戏项目")
    public Map<String, Object> createProject(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        if (authenticate(request) == null) return unauthorized();
        String name = (String) params.get("name");
        String description = (String) params.getOrDefault("description", "");
        String workDir = (String) params.getOrDefault("workDir", "/opt/gamemaker/projects/" + name);
        log.info("内部API: 创建项目 - name: {}", name);
        try {
            var project = projectManager.createProject(name, description, workDir);
            return Map.of("success", true, "message", "项目创建成功: " + name, "projectId", project.getId());
        } catch (Exception e) {
            return Map.of("success", false, "error", "创建项目失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有游戏模板
     */
    @GetMapping("/game-templates")
    @Operation(summary = "获取游戏模板列表", description = "获取所有游戏模板")
    public Map<String, Object> getGameTemplates(HttpServletRequest request) {
        if (authenticate(request) == null) return unauthorized();
        log.info("内部API: 获取游戏模板列表");
        try {
            var templates = gameTemplateService.getAllTemplates();
            List<Map<String, Object>> list = new ArrayList<>();
            for (var tpl : templates) {
                Map<String, Object> info = new HashMap<>();
                info.put("id", tpl.getId());
                info.put("name", tpl.getName());
                info.put("description", tpl.getDescription());
                info.put("keywords", tpl.getKeywords());
                list.add(info);
            }
            return Map.of("success", true, "templates", list, "total", list.size());
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * 创建游戏模板
     */
    @PostMapping("/game-templates")
    @Operation(summary = "创建游戏模板", description = "创建新的游戏模板")
    public Map<String, Object> createGameTemplate(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        if (authenticate(request) == null) return unauthorized();
        String name = (String) params.get("name");
        String description = (String) params.getOrDefault("description", "");
        String gameType = (String) params.getOrDefault("gameType", "general");
        log.info("内部API: 创建游戏模板 - name: {}", name);
        try {
            String id = "custom-" + System.currentTimeMillis();
            var template = gameTemplateService.createTemplate(id, name, description, List.of(gameType), gameType, description);
            return Map.of("success", true, "message", "游戏模板创建成功: " + name, "templateId", template.getId());
        } catch (Exception e) {
            return Map.of("success", false, "error", "创建游戏模板失败: " + e.getMessage());
        }
    }

    /**
     * 删除游戏模板
     */
    @DeleteMapping("/game-templates/{templateId}")
    @Operation(summary = "删除游戏模板", description = "删除指定的游戏模板")
    public Map<String, Object> deleteGameTemplate(HttpServletRequest request, @PathVariable String templateId) {
        if (authenticate(request) == null) return unauthorized();
        log.info("内部API: 删除游戏模板 - templateId: {}", templateId);
        try {
            boolean deleted = gameTemplateService.deleteTemplate(templateId);
            if (deleted) {
                return Map.of("success", true, "message", "游戏模板已删除: " + templateId);
            } else {
                return Map.of("success", false, "error", "模板不存在或为内置模板，无法删除");
            }
        } catch (Exception e) {
            return Map.of("success", false, "error", "删除失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有技能
     */
    @GetMapping("/skills")
    @Operation(summary = "获取技能列表", description = "获取所有技能")
    public Map<String, Object> getSkills(HttpServletRequest request) {
        if (authenticate(request) == null) return unauthorized();
        log.info("内部API: 获取技能列表");
        var skills = skillManager.getAllGlobalSkills();
        List<Map<String, Object>> list = new ArrayList<>();
        for (var skill : skills) {
            Map<String, Object> info = new HashMap<>();
            info.put("id", skill.getId());
            info.put("name", skill.getName());
            info.put("description", skill.getDescription());
            info.put("category", skill.getCategory());
            list.add(info);
        }
        return Map.of("success", true, "skills", list, "total", list.size());
    }

    /**
     * 创建技能
     */
    @PostMapping("/skills")
    @Operation(summary = "创建技能", description = "创建新的技能")
    public Map<String, Object> createSkill(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        if (authenticate(request) == null) return unauthorized();
        String name = (String) params.get("name");
        String description = (String) params.get("description");
        String prompt = (String) params.get("prompt");
        String category = (String) params.getOrDefault("category", "custom");
        String triggerPattern = (String) params.getOrDefault("triggerPattern", name);
        log.info("内部API: 创建技能 - name: {}", name);

        Skill skill = Skill.builder()
            .id("custom-" + System.currentTimeMillis())
            .name(name)
            .description(description)
            .category(category)
            .triggerPattern(triggerPattern)
            .prompt(prompt)
            .build();

        skillManager.registerGlobalSkill(skill);
        return Map.of("success", true, "message", "技能创建成功: " + name, "skillId", skill.getId());
    }

    /**
     * 获取所有工作流实例
     */
    @GetMapping("/workflows/instances")
    @Operation(summary = "获取工作流实例列表", description = "获取所有运行中的工作流实例")
    public Map<String, Object> getWorkflowInstances(HttpServletRequest request) {
        if (authenticate(request) == null) return unauthorized();
        log.info("内部API: 获取工作流实例列表");
        var instances = workflowEngine.getRunningInstances();
        List<Map<String, Object>> list = new ArrayList<>();
        for (var inst : instances) {
            Map<String, Object> info = new HashMap<>();
            info.put("id", inst.getId());
            info.put("templateId", inst.getTemplateId());
            info.put("projectId", inst.getProjectId());
            info.put("status", inst.getStatus().name());
            info.put("createdAt", inst.getCreatedAt() != null ? inst.getCreatedAt().toString() : null);
            list.add(info);
        }
        return Map.of("success", true, "instances", list, "total", list.size());
    }

    /**
     * 启动工作流
     */
    @PostMapping("/workflows/start")
    @Operation(summary = "启动工作流", description = "启动新的工作流实例")
    public Map<String, Object> startWorkflow(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        if (authenticate(request) == null) return unauthorized();
        String templateId = (String) params.get("templateId");
        String projectId = (String) params.get("projectId");
        @SuppressWarnings("unchecked")
        Map<String, String> parameters = (Map<String, String>) params.getOrDefault("parameters", Map.of());
        log.info("内部API: 启动工作流 - templateId: {}, projectId: {}", templateId, projectId);
        try {
            var instance = workflowEngine.startWorkflow(templateId, projectId, parameters);
            return Map.of("success", true, "message", "工作流已启动", "instanceId", instance.getId(),
                "templateId", templateId, "projectId", projectId);
        } catch (Exception e) {
            return Map.of("success", false, "error", "启动工作流失败: " + e.getMessage());
        }
    }

    /**
     * 获取系统告警
     */
    @GetMapping("/alerts")
    @Operation(summary = "获取系统告警", description = "获取系统告警列表")
    public Map<String, Object> getAlerts(HttpServletRequest request) {
        if (authenticate(request) == null) return unauthorized();
        log.info("内部API: 获取系统告警");
        try {
            var alerts = alertService.getAllAlerts(0, 20);
            return Map.of("success", true, "alerts", alerts.getContent(), "total", alerts.getTotalElements());
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * 获取系统信息（综合监控数据）
     * 返回 JVM、磁盘、线程、Agent、项目、数据库等全面信息
     */
    @GetMapping("/system/info")
    @Operation(summary = "获取系统信息", description = "获取系统运行环境信息")
    public Map<String, Object> getSystemInfo(HttpServletRequest request) {
        if (authenticate(request) == null) return unauthorized();
        try {
            Runtime runtime = Runtime.getRuntime();
            java.lang.management.MemoryMXBean memoryBean = java.lang.management.ManagementFactory.getMemoryMXBean();
            java.lang.management.ThreadMXBean threadBean = java.lang.management.ManagementFactory.getThreadMXBean();
            java.lang.management.RuntimeMXBean runtimeBean = java.lang.management.ManagementFactory.getRuntimeMXBean();

            // JVM 信息
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
            long heapMax = memoryBean.getHeapMemoryUsage().getMax() / (1024 * 1024);
            long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed() / (1024 * 1024);
            long totalUsed = (memoryBean.getHeapMemoryUsage().getUsed() + memoryBean.getNonHeapMemoryUsage().getUsed()) / (1024 * 1024);
            long totalMax = runtime.maxMemory() / (1024 * 1024);

            Map<String, Object> jvm = new HashMap<>();
            jvm.put("heapUsedMB", heapUsed);
            jvm.put("heapMaxMB", heapMax);
            jvm.put("nonHeapUsedMB", nonHeapUsed);
            jvm.put("usedMemoryMB", totalUsed);
            jvm.put("maxMemoryMB", totalMax);
            jvm.put("availableProcessors", runtime.availableProcessors());
            jvm.put("javaVersion", System.getProperty("java.version"));

            // 运行时间
            long uptimeMs = runtimeBean.getUptime();
            long days = uptimeMs / 86400000;
            long hours = (uptimeMs % 86400000) / 3600000;
            long minutes = (uptimeMs % 3600000) / 60000;
            jvm.put("uptimeFormatted", days > 0 ? days + "天" + hours + "时" + minutes + "分" : hours + "时" + minutes + "分");

            // 线程信息
            Map<String, Object> threads = new HashMap<>();
            threads.put("threadCount", threadBean.getThreadCount());
            threads.put("peakThreadCount", threadBean.getPeakThreadCount());
            threads.put("daemonThreadCount", threadBean.getDaemonThreadCount());

            // 磁盘信息
            java.io.File root = new java.io.File("/");
            Map<String, Object> disk = new HashMap<>();
            long totalSpace = root.getTotalSpace() / (1024 * 1024 * 1024);
            long freeSpace = root.getFreeSpace() / (1024 * 1024 * 1024);
            long usedSpace = totalSpace - freeSpace;
            disk.put("totalGB", totalSpace);
            disk.put("freeGB", freeSpace);
            disk.put("usedGB", usedSpace);
            disk.put("usagePercent", totalSpace > 0 ? Math.round(usedSpace * 100.0 / totalSpace) : 0);

            // Agent 信息
            var agents = agentManager.getAllAgents();
            long aliveCount = agents.stream().filter(a -> a.isAlive()).count();
            long busyCount = agents.stream().filter(a -> a.isBusy()).count();
            Map<String, Object> agentInfo = new HashMap<>();
            agentInfo.put("total", agents.size());
            agentInfo.put("alive", aliveCount);
            agentInfo.put("busy", busyCount);

            // Agent 列表
            List<Map<String, Object>> agentList = new ArrayList<>();
            for (var agent : agents) {
                Map<String, Object> agentData = new HashMap<>();
                agentData.put("id", agent.getId());
                agentData.put("name", agent.getName());
                agentData.put("role", agent.getRole());
                agentData.put("alive", agent.isAlive());
                agentData.put("busy", agent.isBusy());
                agentList.add(agentData);
            }

            // 项目信息
            var projects = projectManager.getAllProjects();
            long activeProjects = projects.stream().filter(p -> "ACTIVE".equals(p.getStatus())).count();
            Map<String, Object> projectInfo = new HashMap<>();
            projectInfo.put("total", projects.size());
            projectInfo.put("active", activeProjects);

            // 系统负载
            double loadAverage = -1;
            try {
                com.sun.management.OperatingSystemMXBean osBean =
                    (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
                loadAverage = osBean.getSystemLoadAverage();
            } catch (Exception ignored) {}

            // 数据库状态
            Map<String, Object> database = new HashMap<>();
            database.put("status", "UP");
            database.put("version", "MySQL");

            // 组装返回
            Map<String, Object> result = new HashMap<>();
            result.put("jvm", jvm);
            result.put("threads", threads);
            result.put("disk", disk);
            result.put("agents", agentInfo);
            result.put("agentList", agentList);
            result.put("projects", projectInfo);
            result.put("osName", System.getProperty("os.name") + " " + System.getProperty("os.version"));
            result.put("loadAverage", loadAverage >= 0 ? String.format("%.2f", loadAverage) : "N/A");
            result.put("database", database);
            result.put("javaVersion", System.getProperty("java.version"));
            result.put("availableProcessors", runtime.availableProcessors());

            return result;
        } catch (Exception e) {
            log.error("获取系统信息失败", e);
            return Map.of("error", e.getMessage());
        }
    }
}
