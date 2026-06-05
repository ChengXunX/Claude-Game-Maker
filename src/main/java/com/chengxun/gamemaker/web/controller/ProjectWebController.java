package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.agent.ProducerAgent;
import com.chengxun.gamemaker.config.AppConfig;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.model.AgentDefinition;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.service.GoalService;
import com.chengxun.gamemaker.service.TemplateService;
import com.chengxun.gamemaker.web.entity.ProjectMember;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.OperationLogService;
import com.chengxun.gamemaker.web.service.ProjectPermissionService;
import com.chengxun.gamemaker.web.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 项目管理控制器
 *
 * @author chengxun
 * @since 1.0.0
 */
@Controller
@RequestMapping({"/projects", "/api/projects"})
public class ProjectWebController {

    private static final Logger log = LoggerFactory.getLogger(ProjectWebController.class);

    /** 禁止作为项目目录的路径 */
    private static final Set<String> FORBIDDEN_PATHS = Set.of(
        "/", "/root", "/home", "/etc", "/var", "/usr", "/bin", "/sbin",
        "/tmp", "/proc", "/sys", "/dev", "/boot", "/lib", "/lib64",
        "C:\\", "C:\\Windows", "C:\\Program Files", "C:\\Users"
    );

    private final ProjectManager projectManager;
    private final TemplateService templateService;
    private final ProjectPermissionService permissionService;
    private final UserService userService;
    private final OperationLogService logService;
    private final AgentManager agentManager;
    private final AppConfig appConfig;
    private final GoalService goalService;

    public ProjectWebController(ProjectManager projectManager, TemplateService templateService,
                                ProjectPermissionService permissionService, UserService userService,
                                OperationLogService logService, AgentManager agentManager,
                                AppConfig appConfig, GoalService goalService) {
        this.projectManager = projectManager;
        this.templateService = templateService;
        this.permissionService = permissionService;
        this.userService = userService;
        this.logService = logService;
        this.agentManager = agentManager;
        this.appConfig = appConfig;
        this.goalService = goalService;
    }

    // ===== Web页面 =====

    @GetMapping
    public String listProjects(Model model, Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        List<GameProject> projects;
        if (user != null && user.isAdmin()) {
            projects = projectManager.getAllProjects();
        } else if (user != null) {
            List<String> projectIds = permissionService.getUserProjectIds(user.getId());
            projects = projectIds.stream()
                .map(projectManager::getProject)
                .filter(p -> p != null)
                .toList();
        } else {
            projects = List.of();
        }
        model.addAttribute("projects", projects);
        model.addAttribute("username", authentication.getName());
        return "projects";
    }

    @GetMapping("/{projectId}")
    public String projectDetail(@PathVariable String projectId, Model model, Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.VIEWER)) {
            return "redirect:/projects";
        }

        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return "redirect:/projects";
        }

        model.addAttribute("project", project);
        model.addAttribute("rules", projectManager.loadProjectRules(projectId));
        model.addAttribute("username", authentication.getName());
        return "project-detail";
    }

    /**
     * T04: 加权限控制
     * T05: 创建者自动加入项目成员
     * T06: workDir 安全校验
     * T07: 重复目录检查
     */
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public String createProject(@RequestParam String name,
                               @RequestParam String workDir,
                               @RequestParam(required = false) String description,
                               @RequestParam(required = false) String templateId,
                               @RequestParam(required = false) String goal,
                               @RequestParam(required = false) String goalType,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        // T06: workDir 安全校验
        String dirError = validateWorkDir(workDir);
        if (dirError != null) {
            redirectAttributes.addFlashAttribute("error", dirError);
            return "redirect:/projects";
        }

        // T07: 重复目录检查
        if (projectManager.isDirectoryIndexed(workDir)) {
            redirectAttributes.addFlashAttribute("error", "该目录已被其他项目使用: " + workDir);
            return "redirect:/projects";
        }

        try {
            GameProject project = projectManager.createProject(name, description, workDir, templateId);

            // T05: 创建者自动加入为 OWNER
            User user = userService.getUserByUsername(authentication.getName());
            if (user != null) {
                permissionService.addMember(project.getId(), user.getId(), ProjectMember.ProjectRole.OWNER);
            }

            // T08: 设置项目目标
            if (goal != null && !goal.isBlank()) {
                GameProject.GoalType type = GameProject.GoalType.CUSTOM;
                if (goalType != null && !goalType.isBlank()) {
                    try {
                        type = GameProject.GoalType.valueOf(goalType.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        // 使用默认值
                    }
                }
                project.setGoal(goal);
                project.setGoalType(type);
                project.setGoalStatus(GameProject.GoalStatus.NOT_STARTED);
                projectManager.saveProjectConfig(project);
            }

            // T09: 自动创建默认 Agent（producer + server-dev）
            autoCreateDefaultAgents(project);

            logService.log(user != null ? user.getId() : null, "CREATE_PROJECT",
                project.getName(), "Created project at " + workDir, project.getId());

            redirectAttributes.addFlashAttribute("success", "项目创建成功: " + project.getName());
            return "redirect:/projects/" + project.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "项目创建失败: " + e.getMessage());
            return "redirect:/projects";
        }
    }

    @PostMapping("/{projectId}/rules")
    public String updateRules(@PathVariable String projectId,
                             @RequestParam String rules,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.DEVELOPER)) {
            redirectAttributes.addFlashAttribute("error", "无权限修改项目规范");
            return "redirect:/projects";
        }

        projectManager.saveProjectRules(projectId, rules);
        redirectAttributes.addFlashAttribute("success", "项目规范已更新");
        return "redirect:/projects/" + projectId;
    }

    /**
     * T13+T16: 设置/更新项目目标（立即触发分解）
     */
    @PostMapping("/{projectId}/goal")
    public String setGoal(@PathVariable String projectId,
                          @RequestParam String goal,
                          @RequestParam(required = false) String goalType,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.MANAGER)) {
            redirectAttributes.addFlashAttribute("error", "无权限设置项目目标");
            return "redirect:/projects/" + projectId;
        }

        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            redirectAttributes.addFlashAttribute("error", "项目不存在");
            return "redirect:/projects";
        }

        GameProject.GoalType type = GameProject.GoalType.CUSTOM;
        if (goalType != null && !goalType.isBlank()) {
            try { type = GameProject.GoalType.valueOf(goalType.toUpperCase()); } catch (Exception e) {}
        }

        goalService.createGoal(projectId, goal, type, null);
        redirectAttributes.addFlashAttribute("success", "目标已设置，Producer 将自动分解任务");

        // T13: 立即触发 Producer 工作
        Agent producer = agentManager.getAgent(projectId, "producer");
        if (producer != null && producer.isAlive()) {
            producer.work();
        }

        return "redirect:/projects/" + projectId;
    }

    /**
     * T16: 确认目标完成（从 REVIEW 变为 COMPLETED）
     */
    @PostMapping("/{projectId}/goal/confirm")
    public String confirmGoal(@PathVariable String projectId,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.OWNER)) {
            redirectAttributes.addFlashAttribute("error", "无权限确认目标完成");
            return "redirect:/projects/" + projectId;
        }

        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return "redirect:/projects";
        }

        if (project.getGoalStatus() == GameProject.GoalStatus.REVIEW) {
            project.setGoalStatus(GameProject.GoalStatus.COMPLETED);
            project.setGoalProgress(100);
            project.touch();
            projectManager.saveProjectConfig(project);
            redirectAttributes.addFlashAttribute("success", "目标已确认完成！");
        } else {
            redirectAttributes.addFlashAttribute("warning", "目标当前状态不支持确认");
        }

        return "redirect:/projects/" + projectId;
    }

    /**
     * T16: 暂停/恢复目标
     */
    @PostMapping("/{projectId}/goal/toggle")
    public String toggleGoal(@PathVariable String projectId,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.MANAGER)) {
            redirectAttributes.addFlashAttribute("error", "无权限操作");
            return "redirect:/projects/" + projectId;
        }

        GameProject project = projectManager.getProject(projectId);
        if (project == null) return "redirect:/projects";

        if (project.getGoalStatus() == GameProject.GoalStatus.PAUSED) {
            goalService.resumeGoal(projectId);
            redirectAttributes.addFlashAttribute("success", "目标已恢复");
        } else if (project.isGoalActive()) {
            goalService.pauseGoal(projectId);
            redirectAttributes.addFlashAttribute("success", "目标已暂停");
        }

        return "redirect:/projects/" + projectId;
    }

    // ===== API接口 =====

    @GetMapping("/api/all")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public ResponseEntity<List<GameProject>> getAllProjects() {
        return ResponseEntity.ok(projectManager.getAllProjects());
    }

    @GetMapping("/api/{projectId}")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public ResponseEntity<GameProject> getProject(@PathVariable String projectId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(project);
    }

    /**
     * T04+T05+T06+T07: 创建项目 API 加固
     * 支持创建时设置目标和API配置
     */
    @PostMapping("/api/create")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public ResponseEntity<Map<String, Object>> createProjectApi(@RequestBody Map<String, String> request,
                                                                 Authentication authentication) {
        String name = request.get("name");
        String description = request.get("description");
        String workDir = request.get("workDir");
        String templateId = request.get("templateId");
        String goal = request.get("goal");
        String goalType = request.get("goalType");
        String apiKey = request.get("apiKey");
        String apiUrl = request.get("apiUrl");
        String model = request.get("model");

        if (name == null || name.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "项目名称不能为空"));
        }
        if (workDir == null || workDir.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "工作目录不能为空"));
        }

        // T06: 安全校验
        String dirError = validateWorkDir(workDir);
        if (dirError != null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", dirError));
        }

        // T07: 重复检查
        if (projectManager.isDirectoryIndexed(workDir)) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "该目录已被其他项目使用"));
        }

        try {
            GameProject project = projectManager.createProject(name, description, workDir, templateId);

            // T05: 创建者自动加入
            User user = userService.getUserByUsername(authentication.getName());
            if (user != null) {
                permissionService.addMember(project.getId(), user.getId(), ProjectMember.ProjectRole.OWNER);
            }

            // 设置项目目标（如果提供了）
            if (goal != null && !goal.isEmpty()) {
                GameProject.GoalType type = GameProject.GoalType.CUSTOM;
                if (goalType != null && !goalType.isEmpty()) {
                    try { type = GameProject.GoalType.valueOf(goalType.toUpperCase()); } catch (Exception e) {}
                }
                goalService.createGoal(project.getId(), goal, type, null);
                log.info("Goal set for project {}: {}", project.getId(), goal);
            }

            // 保存全局API配置（如果提供了）
            if (apiKey != null && !apiKey.isEmpty() || apiUrl != null && !apiUrl.isEmpty()) {
                project.getMetadata().put("globalApiKey", apiKey != null ? apiKey : "");
                project.getMetadata().put("globalApiUrl", apiUrl != null ? apiUrl : "");
                project.getMetadata().put("globalModel", model != null ? model : "");
                projectManager.saveProjectConfig(project);
                log.info("API config saved for project {}", project.getId());
            }

            return ResponseEntity.ok(Map.of(
                "success", true, "message", "项目创建成功",
                "projectId", project.getId(), "projectName", project.getName()
            ));
        } catch (Exception e) {
            log.error("Failed to create project", e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "项目创建失败: " + e.getMessage()));
        }
    }

    @PostMapping("/api/import")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public ResponseEntity<Map<String, Object>> importProject(@RequestBody Map<String, String> request,
                                                              Authentication authentication) {
        String workDir = request.get("workDir");
        String goal = request.get("goal");
        String goalType = request.get("goalType");

        if (workDir == null || workDir.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "项目目录不能为空"));
        }

        // T06: 安全校验
        String dirError = validateWorkDir(workDir);
        if (dirError != null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", dirError));
        }

        if (projectManager.isDirectoryIndexed(workDir)) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "该项目目录已被索引"));
        }

        try {
            GameProject project = projectManager.importProject(workDir);
            if (project != null) {
                // T05: 导入者自动加入
                User user = userService.getUserByUsername(authentication.getName());
                if (user != null) {
                    permissionService.addMember(project.getId(), user.getId(), ProjectMember.ProjectRole.OWNER);
                }

                // 设置项目目标（如果提供了）
                if (goal != null && !goal.isEmpty()) {
                    GameProject.GoalType type = GameProject.GoalType.CUSTOM;
                    if (goalType != null && !goalType.isEmpty()) {
                        try { type = GameProject.GoalType.valueOf(goalType.toUpperCase()); } catch (Exception e) {}
                    }
                    goalService.createGoal(project.getId(), goal, type, null);
                    log.info("Goal set for imported project {}: {}", project.getId(), goal);
                }

                return ResponseEntity.ok(Map.of(
                    "success", true, "message", "项目导入成功",
                    "projectId", project.getId(), "projectName", project.getName()
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "项目导入失败，请检查目录是否存在"));
            }
        } catch (Exception e) {
            log.error("Failed to import project", e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "项目导入失败: " + e.getMessage()));
        }
    }

    @PostMapping("/api/{projectId}/remove")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public ResponseEntity<Map<String, Object>> removeProject(@PathVariable String projectId) {
        boolean removed = projectManager.removeFromIndex(projectId);
        if (removed) {
            permissionService.removeAllMembers(projectId);
            return ResponseEntity.ok(Map.of("success", true, "message", "项目已从索引中移除"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "项目不存在"));
        }
    }

    @PostMapping("/api/{projectId}/refresh")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public ResponseEntity<Map<String, Object>> refreshProject(@PathVariable String projectId) {
        GameProject project = projectManager.refreshProject(projectId);
        if (project != null) {
            return ResponseEntity.ok(Map.of("success", true, "message", "项目已刷新", "project", project));
        } else {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "项目不存在"));
        }
    }

    @GetMapping("/api/check-directory")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public ResponseEntity<Map<String, Object>> checkDirectory(@RequestParam String workDir) {
        boolean indexed = projectManager.isDirectoryIndexed(workDir);
        boolean forbidden = isForbiddenPath(workDir);
        return ResponseEntity.ok(Map.of(
            "indexed", indexed,
            "forbidden", forbidden,
            "workDir", workDir
        ));
    }

    // ===== 安全校验 =====

    /**
     * T06: 校验 workDir 是否安全
     */
    private String validateWorkDir(String workDir) {
        if (workDir == null || workDir.isBlank()) {
            return "工作目录不能为空";
        }

        Path path = Path.of(workDir).toAbsolutePath().normalize();
        String normalized = path.toString();

        // 检查禁止路径
        if (isForbiddenPath(normalized)) {
            return "不允许使用系统目录作为项目目录: " + normalized;
        }

        // 检查是否在项目根目录下（可选，更安全）
        // if (!normalized.startsWith(appConfig.getProjectsDir())) {
        //     return "项目必须创建在指定目录下";
        // }

        return null;
    }

    private boolean isForbiddenPath(String path) {
        String normalized = Path.of(path).toAbsolutePath().normalize().toString();
        return FORBIDDEN_PATHS.stream().anyMatch(forbidden ->
            normalized.equals(forbidden) || normalized.startsWith(forbidden + "/"));
    }

    /**
     * T09: 自动创建默认 Agent
     * 创建项目后自动创建 producer 和 server-dev Agent
     */
    private void autoCreateDefaultAgents(GameProject project) {
        try {
            String projectId = project.getId();
            String workDir = project.getWorkDir();

            // 创建 producer Agent
            AgentDefinition producerDef = AgentDefinition.builder()
                .id("producer")
                .name("制作人")
                .role("producer")
                .description("项目制作人，负责协调团队和任务分配")
                .workDir(workDir)
                .projectId(projectId)
                .apiKey(appConfig.getClaude().getApiKey())
                .apiUrl(appConfig.getClaude().getApiUrl())
                .model(appConfig.getClaude().getModel())
                .parent(true)
                .build();
            agentManager.createAgent(producerDef);

            // 创建 server-dev Agent
            AgentDefinition serverDef = AgentDefinition.builder()
                .id("server-dev")
                .name("服务端开发")
                .role("server-dev")
                .description("服务端开发 Agent，负责后端逻辑和 API")
                .workDir(workDir)
                .projectId(projectId)
                .apiKey(appConfig.getClaude().getApiKey())
                .apiUrl(appConfig.getClaude().getApiUrl())
                .model(appConfig.getClaude().getModel())
                .build();
            agentManager.createAgent(serverDef);

            log.info("Default agents created for project: {}", projectId);
        } catch (Exception e) {
            log.warn("Failed to create default agents for project {}: {}", project.getId(), e.getMessage());
        }
    }
}
