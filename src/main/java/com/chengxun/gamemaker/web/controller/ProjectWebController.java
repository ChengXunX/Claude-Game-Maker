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
import com.chengxun.gamemaker.web.entity.GameTemplateEntity;
import com.chengxun.gamemaker.web.entity.ProjectMember;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.repository.GameTemplateRepository;
import com.chengxun.gamemaker.web.service.OperationLogService;
import com.chengxun.gamemaker.web.service.ProjectPermissionService;
import com.chengxun.gamemaker.web.service.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Path;
import java.util.*;

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
    private final GameTemplateRepository gameTemplateRepository;
    private final ObjectMapper objectMapper;

    public ProjectWebController(ProjectManager projectManager, TemplateService templateService,
                                ProjectPermissionService permissionService, UserService userService,
                                OperationLogService logService, AgentManager agentManager,
                                AppConfig appConfig, GoalService goalService,
                                GameTemplateRepository gameTemplateRepository, ObjectMapper objectMapper) {
        this.projectManager = projectManager;
        this.templateService = templateService;
        this.permissionService = permissionService;
        this.userService = userService;
        this.logService = logService;
        this.agentManager = agentManager;
        this.appConfig = appConfig;
        this.goalService = goalService;
        this.gameTemplateRepository = gameTemplateRepository;
        this.objectMapper = objectMapper;
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
            autoCreateDefaultAgents(project, templateId);

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
            boolean completed = goalService.completeGoal(projectId);
            if (completed) {
                redirectAttributes.addFlashAttribute("success", "目标已确认完成！所有 Agent 已停止，项目已归档。");
            } else {
                redirectAttributes.addFlashAttribute("error", "目标完成操作失败");
            }
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
    @SuppressWarnings("unchecked")
    @PostMapping("/api/create")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public ResponseEntity<Map<String, Object>> createProjectApi(@RequestBody Map<String, Object> request,
                                                                 Authentication authentication) {
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        String workDir = (String) request.get("workDir");
        String templateId = (String) request.get("templateId");
        String goal = (String) request.get("goal");
        String goalType = (String) request.get("goalType");
        String apiKey = (String) request.get("apiKey");
        String apiUrl = (String) request.get("apiUrl");
        String model = (String) request.get("model");

        // 前端传来的 Agent 角色列表
        List<String> agentRoles = null;
        Object agentsObj = request.get("agents");
        if (agentsObj instanceof List<?> list) {
            agentRoles = list.stream().map(Object::toString).toList();
        }

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

            // 根据前端选择或模板创建默认 Agent
            autoCreateDefaultAgents(project, templateId, agentRoles);

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
        // 先停止并移除该项目的所有 Agent，防止继续消耗 Token
        agentManager.removeProjectAgents(projectId);

        boolean removed = projectManager.removeFromIndex(projectId);
        if (removed) {
            permissionService.removeAllMembers(projectId);
            return ResponseEntity.ok(Map.of("success", true, "message", "项目已从索引中移除"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "项目不存在"));
        }
    }

    @PostMapping("/api/{projectId}/archive")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public ResponseEntity<Map<String, Object>> archiveProject(@PathVariable String projectId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "项目不存在"));
        }

        // 停止该项目的所有 Agent
        agentManager.removeProjectAgents(projectId);

        // 更新项目状态为归档
        project.setStatus(GameProject.ProjectStatus.ARCHIVED);
        projectManager.saveProjectConfig(project);

        log.info("项目已归档: {}", projectId);
        return ResponseEntity.ok(Map.of("success", true, "message", "项目已归档"));
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

    /**
     * 获取项目的里程碑列表
     *
     * @param projectId 项目 ID
     * @return 里程碑列表
     */
    @GetMapping("/api/{projectId}/milestones")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public ResponseEntity<List<GameProject.GoalMilestone>> getMilestones(@PathVariable String projectId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }
        List<GameProject.GoalMilestone> milestones = goalService.getMilestones(projectId);
        return ResponseEntity.ok(milestones);
    }

    /**
     * 获取项目的目录配置
     *
     * @param projectId 项目 ID
     * @return 目录配置列表
     */
    @GetMapping("/api/{projectId}/directories")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public ResponseEntity<Map<String, GameProject.DirectoryConfig>> getDirectories(@PathVariable String projectId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(project.getDirectoryConfigs());
    }

    /**
     * 添加或更新项目目录配置
     *
     * @param projectId 项目 ID
     * @param config 目录配置
     * @return 操作结果
     */
    @PostMapping("/api/{projectId}/directories")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public ResponseEntity<Map<String, Object>> addDirectory(@PathVariable String projectId,
                                                            @RequestBody GameProject.DirectoryConfig config) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "项目不存在"));
        }

        if (config.getPath() == null || config.getPath().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "目录路径不能为空"));
        }

        project.addDirectoryConfig(config);
        projectManager.saveProjectConfig(project);

        return ResponseEntity.ok(Map.of("success", true, "message", "目录配置已保存"));
    }

    /**
     * 删除项目目录配置
     *
     * @param projectId 项目 ID
     * @param dirPath 目录路径
     * @return 操作结果
     */
    @DeleteMapping("/api/{projectId}/directories/{dirPath}")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public ResponseEntity<Map<String, Object>> removeDirectory(@PathVariable String projectId,
                                                               @PathVariable String dirPath) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "项目不存在"));
        }

        // URL 解码路径
        String decodedPath = java.net.URLDecoder.decode(dirPath, java.nio.charset.StandardCharsets.UTF_8);
        project.removeDirectoryConfig(decodedPath);
        projectManager.saveProjectConfig(project);

        return ResponseEntity.ok(Map.of("success", true, "message", "目录配置已删除"));
    }

    /**
     * 人工验证里程碑
     *
     * @param projectId 项目 ID
     * @param milestoneId 里程碑 ID
     * @param request 验证请求
     * @return 操作结果
     */
    @PostMapping("/api/{projectId}/milestones/{milestoneId}/verify")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public ResponseEntity<Map<String, Object>> verifyMilestone(@PathVariable String projectId,
                                                                @PathVariable String milestoneId,
                                                                @RequestBody Map<String, Object> request,
                                                                Authentication authentication) {
        log.info("收到验证里程碑请求: projectId={}, milestoneId={}, passed={}", projectId, milestoneId, request.get("passed"));

        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            log.warn("验证失败：项目不存在 {}", projectId);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "项目不存在"));
        }

        User user = userService.getUserByUsername(authentication.getName());
        if (user == null) {
            log.warn("验证失败：用户不存在");
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "用户不存在"));
        }

        Boolean passed = (Boolean) request.get("passed");
        String comment = (String) request.get("comment");

        if (passed == null) {
            log.warn("验证失败：验证结果为空");
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "验证结果不能为空"));
        }

        try {
            boolean result = goalService.verifyMilestoneManually(projectId, milestoneId, passed, comment, user.getUsername());
            log.info("验证结果: projectId={}, milestoneId={}, passed={}, result={}", projectId, milestoneId, passed, result);
            if (result) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", passed ? "里程碑验证通过" : "里程碑验证未通过",
                    "passed", passed
                ));
            } else {
                log.warn("验证失败：里程碑不存在或状态不允许验证, milestoneId={}", milestoneId);
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "验证失败，里程碑不存在或状态不允许验证"));
            }
        } catch (Exception e) {
            log.error("验证里程碑失败", e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "验证失败: " + e.getMessage()));
        }
    }

    /**
     * 获取项目验证文档
     *
     * @param projectId 项目 ID
     * @return 验证文档内容
     */
    @GetMapping("/api/{projectId}/verification-doc")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public ResponseEntity<Map<String, Object>> getVerificationDoc(@PathVariable String projectId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> doc = new HashMap<>();
        doc.put("projectId", projectId);
        doc.put("projectName", project.getName());
        doc.put("goal", project.getGoal());
        doc.put("goalStatus", project.getGoalStatus());
        doc.put("goalProgress", project.getGoalProgress());

        // 里程碑验证信息
        List<Map<String, Object>> milestones = new ArrayList<>();
        for (GameProject.GoalMilestone milestone : project.getMilestones()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", milestone.getId());
            m.put("title", milestone.getTitle());
            m.put("description", milestone.getDescription());
            m.put("status", milestone.getStatus());
            m.put("progress", milestone.getProgress());
            m.put("verificationCriteria", milestone.getVerificationCriteria());
            m.put("verificationResult", milestone.getVerificationResult());
            m.put("verificationFailCount", milestone.getVerificationFailCount());
            milestones.add(m);
        }
        doc.put("milestones", milestones);

        // 项目规则
        String rules = projectManager.loadProjectRules(projectId);
        doc.put("rules", rules);

        return ResponseEntity.ok(doc);
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
        // 只精确匹配禁止路径，不禁止子目录（如 /home/user/project 是合法的）
        return FORBIDDEN_PATHS.contains(normalized);
    }

    /**
     * T09: 自动创建默认 Agent
     * 创建项目后自动创建 producer 和 server-dev Agent
     */
    /**
     * 创建项目的默认 Agent
     *
     * 优先级：前端指定的角色列表 > 模板配置 > 默认（系统策划）
     *
     * @param project 项目
     * @param templateId 模板ID（可选）
     */
    private void autoCreateDefaultAgents(GameProject project, String templateId) {
        autoCreateDefaultAgents(project, templateId, null);
    }

    /**
     * 创建项目的默认 Agent
     * 制作人是必须的，始终创建；其余为可选 Agent
     *
     * @param project 项目
     * @param templateId 模板ID（可选）
     * @param agentRoles 前端指定的可选 Agent 角色列表（可选，优先使用）
     */
    private void autoCreateDefaultAgents(GameProject project, String templateId, List<String> agentRoles) {
        try {
            String projectId = project.getId();
            String workDir = project.getWorkDir();

            // 制作人是必须的，始终创建
            createAgentForProject(projectId, workDir, "producer", "制作人",
                "协调团队、分配任务、审查工作");

            // 优先使用前端指定的可选角色列表
            if (agentRoles != null && !agentRoles.isEmpty()) {
                for (String role : agentRoles) {
                    if ("producer".equals(role)) continue; // 制作人已创建，跳过
                    createAgentForProject(projectId, workDir, role, getDefaultAgentName(role), "");
                }
                return;
            }

            // 尝试从模板获取 Agent 配置
            List<Map<String, String>> agentConfigs = getTemplateAgentConfigs(templateId);
            if (agentConfigs != null && !agentConfigs.isEmpty()) {
                for (Map<String, String> agentCfg : agentConfigs) {
                    String role = agentCfg.getOrDefault("role", "system-planner");
                    if ("producer".equals(role)) continue; // 制作人已创建，跳过
                    String name = agentCfg.getOrDefault("name", role);
                    String desc = agentCfg.getOrDefault("description", "");
                    createAgentForProject(projectId, workDir, role, name, desc);
                }
                return;
            }

            // 默认创建系统策划
            createAgentForProject(projectId, workDir, "system-planner", "系统策划",
                "负责游戏系统设计和规划，将创意转化为可实现的系统方案");
        } catch (Exception e) {
            log.warn("Failed to create default agents for project {}: {}", project.getId(), e.getMessage());
        }
    }

    /**
     * 为项目创建单个 Agent
     */
    private void createAgentForProject(String projectId, String workDir, String role, String name, String description) {
        AgentDefinition.Builder builder = AgentDefinition.builder()
            .id(role)
            .name(name)
            .role(role)
            .description(description)
            .workDir(workDir)
            .projectId(projectId)
            .apiKey(appConfig.getClaude().getApiKey())
            .apiUrl(appConfig.getClaude().getApiUrl())
            .model(appConfig.getClaude().getModel());

        if ("producer".equals(role)) {
            builder.parent(true);
        }

        agentManager.createAgent(builder.build());
        log.info("Agent created: {} ({}) for project: {}", name, role, projectId);
    }

    /**
     * 获取角色的默认名称
     */
    private String getDefaultAgentName(String role) {
        return switch (role) {
            case "producer" -> "制作人";
            case "server-dev" -> "服务端开发";
            case "client-dev" -> "客户端开发";
            case "ui-dev" -> "UI设计";
            case "system-planner" -> "系统策划";
            case "numerical-planner" -> "数值策划";
            case "tester" -> "测试工程师";
            case "git-commit" -> "Git专员";
            case "security-expert" -> "安全工程师";
            case "data-analyst" -> "数据分析师";
            case "ai-engineer" -> "AI工程师";
            case "devops" -> "运维工程师";
            case "audio-dev" -> "音频设计师";
            case "narrative-planner" -> "剧情策划";
            case "level-design" -> "关卡设计师";
            case "performance-engineer" -> "性能优化";
            case "tech-artist" -> "技术美术";
            case "product-manager" -> "产品经理";
            case "localization" -> "本地化";
            default -> role;
        };
    }

    /**
     * 从游戏模板中解析 Agent 配置列表
     *
     * @param templateId 模板ID
     * @return Agent 配置列表，每个包含 role/name/description；无配置返回 null
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, String>> getTemplateAgentConfigs(String templateId) {
        if (templateId == null || templateId.isEmpty()) {
            return null;
        }

        try {
            // 先从数据库模板查
            Optional<GameTemplateEntity> dbTemplate = gameTemplateRepository.findById(templateId);
            if (dbTemplate.isPresent() && dbTemplate.get().getConfigJson() != null) {
                Map<String, Object> config = objectMapper.readValue(
                    dbTemplate.get().getConfigJson(),
                    new TypeReference<Map<String, Object>>() {}
                );
                Object agents = config.get("agents");
                if (agents instanceof List<?> list && !list.isEmpty()) {
                    return (List<Map<String, String>>) (List<?>) list;
                }
            }

            // 再从文件模板查
            Map<String, Object> fileTemplate = templateService.getTemplate(templateId);
            if (fileTemplate != null && fileTemplate.containsKey("agents")) {
                Object agents = fileTemplate.get("agents");
                if (agents instanceof List<?> list && !list.isEmpty()) {
                    return (List<Map<String, String>>) (List<?>) list;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse template agent configs for {}: {}", templateId, e.getMessage());
        }

        return null;
    }

    // ===== 项目看板 API =====

    @Autowired(required = false)
    private com.chengxun.gamemaker.service.ProjectBoard projectBoard;

    @Autowired(required = false)
    private com.chengxun.gamemaker.service.EventBus eventBus;

    /**
     * 获取项目看板数据
     */
    @GetMapping("/api/{projectId}/board")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public ResponseEntity<Map<String, Object>> getProjectBoard(@PathVariable String projectId) {
        if (projectBoard == null) {
            return ResponseEntity.ok(Map.of("error", "看板服务未启用"));
        }

        com.chengxun.gamemaker.service.ProjectBoard.BoardData board = projectBoard.getBoard(projectId);
        if (board == null) {
            return ResponseEntity.ok(Map.of(
                "projectId", projectId,
                "agentStatuses", Map.of(),
                "taskCards", List.of(),
                "blockers", List.of(),
                "sharedContext", Map.of()
            ));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("projectId", projectId);
        response.put("agentStatuses", board.agentStatuses);
        response.put("taskCards", board.taskCards);
        response.put("blockers", board.blockers);
        response.put("sharedContext", board.sharedContext);
        response.put("lastUpdated", board.lastUpdated);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取项目看板摘要（供 Agent 上下文使用）
     */
    @GetMapping("/api/{projectId}/board/summary")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public ResponseEntity<Map<String, String>> getProjectBoardSummary(@PathVariable String projectId) {
        if (projectBoard == null) {
            return ResponseEntity.ok(Map.of("summary", "看板服务未启用"));
        }

        String summary = projectBoard.buildBoardSummary(projectId);
        return ResponseEntity.ok(Map.of("summary", summary));
    }

    /**
     * 获取项目最近事件
     */
    @GetMapping("/api/{projectId}/events")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public ResponseEntity<List<Map<String, Object>>> getProjectEvents(
            @PathVariable String projectId,
            @RequestParam(defaultValue = "20") int limit) {
        if (eventBus == null) {
            return ResponseEntity.ok(List.of());
        }

        List<com.chengxun.gamemaker.service.EventBus.ProjectEvent> events = eventBus.getRecentEvents(projectId, limit);
        List<Map<String, Object>> result = new ArrayList<>();
        for (com.chengxun.gamemaker.service.EventBus.ProjectEvent event : events) {
            Map<String, Object> item = new HashMap<>();
            item.put("eventId", event.getEventId());
            item.put("eventType", event.getEventType());
            item.put("sourceAgentId", event.getSourceAgentId());
            item.put("data", event.getData());
            item.put("timestamp", event.getTimestamp());
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }
}
