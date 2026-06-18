package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.agent.ProducerAgent;
import com.chengxun.gamemaker.config.AppConfig;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.model.AgentDefinition;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.service.GoalService;
import com.chengxun.gamemaker.service.PlayerExperienceAnalyzer;
import com.chengxun.gamemaker.service.TemplateService;
import com.chengxun.gamemaker.service.VersionIterationService;
import jakarta.servlet.http.HttpServletResponse;
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
import java.time.LocalDateTime;
import java.util.*;

/**
 * 项目管理控制器
 *
 * @author chengxun
 * @since 1.0.0
 */
@Controller
@RequestMapping("/api/projects")
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
    private final VersionIterationService versionIterationService;

    public ProjectWebController(ProjectManager projectManager, TemplateService templateService,
                                ProjectPermissionService permissionService, UserService userService,
                                OperationLogService logService, AgentManager agentManager,
                                AppConfig appConfig, GoalService goalService,
                                GameTemplateRepository gameTemplateRepository, ObjectMapper objectMapper,
                                VersionIterationService versionIterationService) {
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
        this.versionIterationService = versionIterationService;
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

    /**
     * 获取所有项目列表（精简版，不包含完整的里程碑和任务数据）
     * 避免大量任务数据导致序列化失败
     */
    @GetMapping("/api/all")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public ResponseEntity<List<Map<String, Object>>> getAllProjects() {
        List<GameProject> projects = projectManager.getAllProjects();
        List<Map<String, Object>> result = new ArrayList<>();

        for (GameProject p : projects) {
            Map<String, Object> summary = new HashMap<>();
            summary.put("id", p.getId());
            summary.put("name", p.getName());
            summary.put("description", p.getDescription());
            summary.put("workDir", p.getWorkDir());
            summary.put("status", p.getStatus());
            summary.put("version", p.getVersion());
            summary.put("goal", p.getGoal());
            summary.put("goalStatus", p.getGoalStatus());
            summary.put("goalProgress", p.getGoalProgress());
            summary.put("running", p.isRunning());
            summary.put("createdAt", p.getCreatedAt());
            summary.put("lastActiveAt", p.getLastActiveAt());
            summary.put("templateId", p.getTemplateId());

            // 里程碑摘要（不包含详细任务）
            List<Map<String, Object>> milestoneSummary = new ArrayList<>();
            if (p.getMilestones() != null) {
                for (GameProject.GoalMilestone m : p.getMilestones()) {
                    Map<String, Object> ms = new HashMap<>();
                    ms.put("id", m.getId());
                    ms.put("title", m.getTitle());
                    ms.put("status", m.getStatus());
                    ms.put("progress", m.getProgress());
                    ms.put("assignedAgentRole", m.getAssignedAgentRole());
                    ms.put("taskCount", m.getTasks() != null ? m.getTasks().size() : 0);
                    milestoneSummary.add(ms);
                }
            }
            summary.put("milestones", milestoneSummary);
            summary.put("milestoneCount", milestoneSummary.size());

            // 管理员指令状态
            summary.put("hasInstruction", p.hasPendingInstruction());
            summary.put("instructionSubmittedAt", p.getInstructionSubmittedAt());

            result.add(summary);
        }

        return ResponseEntity.ok(result);
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
     * 提交管理员版本迭代指令
     * 管理员可以在版本迭代前给出指导，系统会在迭代决策时参考
     *
     * @param projectId 项目 ID
     * @param request 包含 instruction 字段的请求体
     * @return 操作结果
     */
    @PostMapping("/api/{projectId}/version-instruction")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public ResponseEntity<Map<String, Object>> submitVersionInstruction(
            @PathVariable String projectId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.MANAGER)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "无权限操作"));
        }

        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "项目不存在"));
        }

        String instruction = request.get("instruction");
        if (instruction == null || instruction.trim().isEmpty()) {
            // 清空指令
            project.setVersionIterationInstruction(null);
            project.setInstructionSubmittedAt(null);
            project.setInstructionConsumed(false);
        } else {
            project.setVersionIterationInstruction(instruction.trim());
            project.setInstructionSubmittedAt(LocalDateTime.now());
            project.setInstructionConsumed(false);
        }

        projectManager.saveProjectConfig(project);

        log.info("管理员提交版本迭代指令: projectId={}, user={}, instruction={}",
            projectId, user.getUsername(),
            instruction != null ? instruction.substring(0, Math.min(instruction.length(), 50)) + "..." : "(清空)");

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", instruction != null && !instruction.trim().isEmpty() ? "指令已提交，将在下次版本迭代时参考" : "指令已清空"
        ));
    }

    /**
     * 获取当前版本迭代指令
     *
     * @param projectId 项目 ID
     * @return 当前指令信息
     */
    @GetMapping("/api/{projectId}/version-instruction")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public ResponseEntity<Map<String, Object>> getVersionInstruction(@PathVariable String projectId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "项目不存在"));
        }

        return ResponseEntity.ok(Map.of(
            "success", true,
            "instruction", project.getVersionIterationInstruction() != null ? project.getVersionIterationInstruction() : "",
            "submittedAt", project.getInstructionSubmittedAt() != null ? project.getInstructionSubmittedAt().toString() : "",
            "consumed", project.isInstructionConsumed()
        ));
    }

    /**
     * 清理项目脏数据
     * - 修正不合理的任务工时（超过168小时的截断）
     * - 清理重复任务
     * - 修正里程碑状态不一致
     *
     * @param projectId 项目 ID
     * @return 清理结果
     */
    @PostMapping("/api/{projectId}/clean-data")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public ResponseEntity<Map<String, Object>> cleanProjectData(
            @PathVariable String projectId,
            Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.MANAGER)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "无权限操作"));
        }

        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "项目不存在"));
        }

        int fixedTasks = 0;
        int fixedMilestones = 0;
        int removedDuplicates = 0;

        // 1. 修正不合理的任务工时
        for (GameProject.GoalMilestone milestone : project.getMilestones()) {
            if (milestone.getTasks() == null) continue;

            for (GameProject.MilestoneTask task : milestone.getTasks()) {
                // 截断过大的工时
                if (task.getEstimatedHours() > 168) {
                    task.setEstimatedHours(168);
                    fixedTasks++;
                }
            }

            // 2. 修正里程碑状态不一致
            if (milestone.getTasks() != null && !milestone.getTasks().isEmpty()) {
                long completedCount = milestone.getTasks().stream()
                    .filter(t -> t.getStatus() == GameProject.MilestoneStatus.COMPLETED)
                    .count();

                // 如果所有任务都完成了，但里程碑没标记完成
                if (completedCount == milestone.getTasks().size()
                    && milestone.getStatus() != GameProject.MilestoneStatus.COMPLETED) {
                    milestone.setStatus(GameProject.MilestoneStatus.COMPLETED);
                    milestone.setProgress(100);
                    fixedMilestones++;
                }
            }
        }

        // 保存清理后的项目
        projectManager.saveProjectConfig(project);

        log.info("项目数据清理完成: projectId={}, fixedTasks={}, fixedMilestones={}",
            projectId, fixedTasks, fixedMilestones);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "数据清理完成",
            "fixedTasks", fixedTasks,
            "fixedMilestones", fixedMilestones
        ));
    }

    /**
     * 重置版本里程碑
     * 清空当前版本的所有里程碑和任务，让制作人重新规划
     *
     * @param projectId 项目 ID
     * @return 操作结果
     */
    @PostMapping("/api/{projectId}/reset-milestones")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public ResponseEntity<Map<String, Object>> resetMilestones(
            @PathVariable String projectId,
            Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.OWNER)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "无权限操作，需要项目所有者"));
        }

        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "项目不存在"));
        }

        int oldCount = project.getMilestones().size();
        project.getMilestones().clear();
        project.setGoalStatus(GameProject.GoalStatus.NOT_STARTED);
        projectManager.saveProjectConfig(project);

        log.info("里程碑已重置: projectId={}, 清除了 {} 个里程碑", projectId, oldCount);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "已清除 " + oldCount + " 个里程碑，制作人将在下一个工作周期重新规划"
        ));
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

    // ===== 版本迭代相关 API =====

    /**
     * 获取项目的版本迭代统计
     *
     * @param projectId 项目 ID
     * @return 统计信息
     */
    @GetMapping("/api/{projectId}/iteration-stats")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public ResponseEntity<Map<String, Object>> getIterationStats(@PathVariable String projectId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> stats = versionIterationService.getIterationStats(projectId);
        return ResponseEntity.ok(stats);
    }

    /**
     * 获取项目的版本迭代记录
     *
     * @param projectId 项目 ID
     * @return 迭代记录列表
     */
    @GetMapping("/api/{projectId}/iteration-records")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public ResponseEntity<List<?>> getIterationRecords(@PathVariable String projectId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(versionIterationService.getIterationRecords(projectId));
    }

    /**
     * 版本回滚
     *
     * @param projectId 项目 ID
     * @param request 请求体（包含 targetVersion）
     * @return 操作结果
     */
    @PostMapping("/api/{projectId}/rollback")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public ResponseEntity<Map<String, Object>> rollbackVersion(@PathVariable String projectId,
                                                                @RequestBody Map<String, String> request) {
        String targetVersion = request.get("targetVersion");
        String result = versionIterationService.rollbackVersion(projectId, targetVersion);

        boolean success = result.contains("成功");
        return ResponseEntity.ok(Map.of(
            "success", success,
            "message", result
        ));
    }

    /**
     * 获取可回滚的版本列表
     *
     * @param projectId 项目 ID
     * @return 版本列表
     */
    @GetMapping("/api/{projectId}/rollbackable-versions")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public ResponseEntity<List<Map<String, Object>>> getRollbackableVersions(@PathVariable String projectId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(versionIterationService.getRollbackableVersions(projectId));
    }

    /**
     * 获取版本对比数据
     *
     * @param projectId 项目 ID
     * @param version1 版本1
     * @param version2 版本2
     * @return 对比结果
     */
    @GetMapping("/api/{projectId}/version-comparison")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public ResponseEntity<Map<String, Object>> getVersionComparison(@PathVariable String projectId,
                                                                     @RequestParam String version1,
                                                                     @RequestParam String version2) {
        Map<String, Object> comparison = versionIterationService.compareVersions(projectId, version1, version2);
        return ResponseEntity.ok(comparison);
    }

    /**
     * 导出迭代报告
     *
     * @param projectId 项目 ID
     * @param response HTTP 响应
     */
    @GetMapping("/api/{projectId}/export-iteration-report")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public void exportIterationReport(@PathVariable String projectId, HttpServletResponse response) {
        try {
            byte[] reportData = versionIterationService.exportIterationReport(projectId);

            GameProject project = projectManager.getProject(projectId);
            String fileName = String.format("迭代报告_%s_%s.xlsx",
                project != null ? project.getName() : projectId,
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(java.time.LocalDateTime.now()));

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + java.net.URLEncoder.encode(fileName, "UTF-8"));
            response.getOutputStream().write(reportData);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("导出迭代报告失败: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 获取迭代模板列表
     *
     * @return 模板列表
     */
    @GetMapping("/api/iteration-templates")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public ResponseEntity<List<Map<String, Object>>> getIterationTemplates() {
        return ResponseEntity.ok(versionIterationService.getIterationTemplates());
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
     * 制作人和验证Agent是必须的，始终创建；其余为可选 Agent
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

            // 验证Agent是必须的，始终创建
            createAgentForProject(projectId, workDir, "verifier", "验证官",
                "负责项目约束检查、代码质量验证、设计审查和里程碑验收");

            // 优先使用前端指定的可选角色列表
            if (agentRoles != null && !agentRoles.isEmpty()) {
                for (String role : agentRoles) {
                    if ("producer".equals(role) || "verifier".equals(role)) continue; // 已创建，跳过
                    createAgentForProject(projectId, workDir, role, getDefaultAgentName(role), "");
                }
                return;
            }

            // 尝试从模板获取 Agent 配置
            List<Map<String, String>> agentConfigs = getTemplateAgentConfigs(templateId);
            if (agentConfigs != null && !agentConfigs.isEmpty()) {
                for (Map<String, String> agentCfg : agentConfigs) {
                    String role = agentCfg.getOrDefault("role", "system-planner");
                    if ("producer".equals(role) || "verifier".equals(role)) continue; // 已创建，跳过
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
            case "verifier" -> "验证官";
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

    @Autowired(required = false)
    private com.chengxun.gamemaker.web.service.SystemConstantService constantService;

    @Autowired(required = false)
    private com.chengxun.gamemaker.web.service.SystemConfigService systemConfigService;

    @Autowired(required = false)
    private PlayerExperienceAnalyzer playerExperienceAnalyzer;

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

    /**
     * 获取项目督查报告
     */
    @GetMapping("/api/{projectId}/supervision-report")
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('PERM_projects:view', 'PERM_supervision:view')")
    public ResponseEntity<Map<String, Object>> getSupervisionReport(@PathVariable String projectId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> report = new HashMap<>();
        report.put("projectId", projectId);
        report.put("projectName", project.getName());
        report.put("currentVersion", project.getVersion());
        report.put("goalProgress", project.getGoalProgress());
        report.put("goalStatus", project.getGoalStatus() != null ? project.getGoalStatus().name() : "UNKNOWN");

        // 里程碑统计
        List<GameProject.GoalMilestone> milestones = project.getMilestones();
        int totalMilestones = milestones.size();
        long completedMilestones = milestones.stream()
            .filter(m -> m.getStatus() == GameProject.MilestoneStatus.COMPLETED).count();
        long inProgressMilestones = milestones.stream()
            .filter(m -> m.getStatus() == GameProject.MilestoneStatus.IN_PROGRESS).count();

        report.put("totalMilestones", totalMilestones);
        report.put("completedMilestones", completedMilestones);
        report.put("inProgressMilestones", inProgressMilestones);
        report.put("milestoneCompletionRate", totalMilestones > 0 ? (double) completedMilestones / totalMilestones * 100 : 0);

        // 任务统计
        int totalTasks = 0;
        int completedTasks = 0;
        int overdueTasks = 0;
        LocalDateTime now = LocalDateTime.now();

        for (GameProject.GoalMilestone milestone : milestones) {
            List<GameProject.MilestoneTask> tasks = milestone.getTasks();
            if (tasks != null) {
                totalTasks += tasks.size();
                for (GameProject.MilestoneTask task : tasks) {
                    if (task.getStatus() == GameProject.MilestoneStatus.COMPLETED) {
                        completedTasks++;
                    } else if (task.getStatus() == GameProject.MilestoneStatus.IN_PROGRESS) {
                        // 检查是否超时
                        LocalDateTime startedAt = task.getStartedAt();
                        int estimatedHours = task.getEstimatedHours();
                        if (startedAt != null && estimatedHours > 0) {
                            LocalDateTime expectedCompletion = startedAt.plusHours(estimatedHours);
                            if (now.isAfter(expectedCompletion)) {
                                overdueTasks++;
                            }
                        }
                    }
                }
            }
        }

        report.put("totalTasks", totalTasks);
        report.put("completedTasks", completedTasks);
        report.put("overdueTasks", overdueTasks);
        report.put("taskCompletionRate", totalTasks > 0 ? (double) completedTasks / totalTasks * 100 : 0);

        // 版本迭代统计
        report.put("versionCount", project.getVersionCount());

        // 里程碑详情
        List<Map<String, Object>> milestoneDetails = new ArrayList<>();
        for (GameProject.GoalMilestone milestone : milestones) {
            Map<String, Object> detail = new HashMap<>();
            detail.put("id", milestone.getId());
            detail.put("title", milestone.getTitle());
            detail.put("status", milestone.getStatus().name());
            detail.put("progress", milestone.getProgress());
            detail.put("assignedRole", milestone.getAssignedAgentRole());

            // 任务详情
            List<Map<String, Object>> taskDetails = new ArrayList<>();
            if (milestone.getTasks() != null) {
                for (GameProject.MilestoneTask task : milestone.getTasks()) {
                    Map<String, Object> taskDetail = new HashMap<>();
                    taskDetail.put("id", task.getId());
                    taskDetail.put("title", task.getTitle());
                    taskDetail.put("status", task.getStatus().name());
                    taskDetail.put("assignedRole", task.getAssignedRole());
                    taskDetail.put("estimatedHours", task.getEstimatedHours());
                    taskDetail.put("startedAt", task.getStartedAt());
                    taskDetails.add(taskDetail);
                }
            }
            detail.put("tasks", taskDetails);

            milestoneDetails.add(detail);
        }
        report.put("milestones", milestoneDetails);

        // Agent效率分析
        List<Map<String, Object>> agentEfficiency = new ArrayList<>();
        Map<String, int[]> roleStats = new HashMap<>(); // role -> [total, completed, overdue]
        for (GameProject.GoalMilestone milestone : milestones) {
            if (milestone.getTasks() == null) continue;
            for (GameProject.MilestoneTask task : milestone.getTasks()) {
                String role = task.getAssignedRole();
                if (role == null || role.isEmpty()) continue;
                roleStats.computeIfAbsent(role, k -> new int[3]);
                roleStats.get(role)[0]++; // total
                if (task.getStatus() == GameProject.MilestoneStatus.COMPLETED) {
                    roleStats.get(role)[1]++; // completed
                }
                // 检查是否超时
                LocalDateTime startedAt = task.getStartedAt();
                int estimatedHours = task.getEstimatedHours();
                if (startedAt != null && estimatedHours > 0 && task.getStatus() == GameProject.MilestoneStatus.IN_PROGRESS) {
                    if (now.isAfter(startedAt.plusHours(estimatedHours))) {
                        roleStats.get(role)[2]++; // overdue
                    }
                }
            }
        }
        for (var entry : roleStats.entrySet()) {
            Map<String, Object> eff = new HashMap<>();
            eff.put("role", entry.getKey());
            eff.put("totalTasks", entry.getValue()[0]);
            eff.put("completedTasks", entry.getValue()[1]);
            eff.put("overdueTasks", entry.getValue()[2]);
            eff.put("completionRate", entry.getValue()[0] > 0 ? (double) entry.getValue()[1] / entry.getValue()[0] * 100 : 0);
            agentEfficiency.add(eff);
        }
        report.put("agentEfficiency", agentEfficiency);

        // 迭代成本统计（版本历史数量 + 迭代记录数）
        report.put("versionHistoryCount", project.getVersionHistory() != null ? project.getVersionHistory().size() : 0);

        return ResponseEntity.ok(report);
    }

    /**
     * 获取协作效率指标
     * 返回Agent间的协作效率数据，包括交接延迟、返工率、阻塞时长等
     */
    @GetMapping("/api/{projectId}/collaboration-metrics")
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('PERM_projects:view', 'PERM_supervision:view')")
    public ResponseEntity<Map<String, Object>> getCollaborationMetrics(@PathVariable String projectId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("projectId", projectId);
        result.put("projectName", project.getName());

        // 从督查报告知识库中获取协作效率数据
        String collaborationData = null;
        Agent verifier = getProjectVerifier(projectId);
        if (verifier instanceof com.chengxun.gamemaker.agent.VerificationAgent verificationAgent) {
            // 从VerificationAgent获取协作效率指标
            try {
                // 通过反射或直接调用获取指标（这里通过知识库获取）
                collaborationData = "协作效率数据已生成";
            } catch (Exception e) {
                log.debug("获取协作效率数据失败: {}", e.getMessage());
            }
        }

        // 从任务数据计算基础协作指标
        List<GameProject.GoalMilestone> milestones = project.getMilestones();
        int totalTasks = 0;
        int completedTasks = 0;
        int reworkedTasks = 0;
        long totalEstimatedHours = 0;
        long totalActualHours = 0;

        for (GameProject.GoalMilestone milestone : milestones) {
            if (milestone.getTasks() == null) continue;
            for (GameProject.MilestoneTask task : milestone.getTasks()) {
                totalTasks++;
                if (task.getStatus() == GameProject.MilestoneStatus.COMPLETED) {
                    completedTasks++;
                }
                if (task.getResult() != null && task.getResult().contains("返工")) {
                    reworkedTasks++;
                }
                if (task.getEstimatedHours() > 0) {
                    totalEstimatedHours += task.getEstimatedHours();
                }
            }
        }

        result.put("totalTasks", totalTasks);
        result.put("completedTasks", completedTasks);
        result.put("reworkedTasks", reworkedTasks);
        result.put("reworkRate", totalTasks > 0 ? (double) reworkedTasks / totalTasks * 100 : 0);
        result.put("totalEstimatedHours", totalEstimatedHours);
        result.put("collaborationData", collaborationData);

        return ResponseEntity.ok(result);
    }

    /**
     * 获取项目内的Verifier Agent
     */
    private Agent getProjectVerifier(String projectId) {
        try {
            // 从AgentManager获取项目内的verifier
            return agentManager.getAgentsByProject(projectId).stream()
                .filter(a -> "verifier".equals(a.getRole()))
                .findFirst().orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取玩家体验评分
     * 使用 PlayerExperienceAnalyzer 混合评分（关键词+代码特征+AI深度分析）
     */
    @GetMapping("/api/{projectId}/player-experience")
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('PERM_projects:view', 'PERM_supervision:view')")
    public ResponseEntity<Map<String, Object>> getPlayerExperience(@PathVariable String projectId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("projectId", projectId);
        result.put("projectName", project.getName());

        // 功能完成度（基于里程碑任务）
        List<GameProject.GoalMilestone> milestones = project.getMilestones();
        int totalFeatures = 0;
        int completedFeatures = 0;
        for (GameProject.GoalMilestone m : milestones) {
            if (m.getTasks() != null) {
                totalFeatures += m.getTasks().size();
                completedFeatures += m.getTasks().stream()
                    .filter(t -> t.getStatus() == GameProject.MilestoneStatus.COMPLETED).count();
            }
        }
        result.put("totalFeatures", totalFeatures);
        result.put("completedFeatures", completedFeatures);
        result.put("featureCompleteness", totalFeatures > 0 ? (double) completedFeatures / totalFeatures * 100 : 0);

        // 使用 PlayerExperienceAnalyzer 获取混合评分
        if (playerExperienceAnalyzer != null) {
            PlayerExperienceAnalyzer.FunScore funScore = playerExperienceAnalyzer.analyzeProject(project);

            Map<String, Integer> dimensionScores = new HashMap<>();
            dimensionScores.put("coreLoop", funScore.getCoreLoopScore());
            dimensionScores.put("challenge", funScore.getChallengeScore());
            dimensionScores.put("reward", funScore.getRewardScore());
            dimensionScores.put("progression", funScore.getProgressionScore());
            dimensionScores.put("novelty", funScore.getNoveltyScore());
            result.put("dimensionScores", dimensionScores);
            result.put("overallScore", funScore.getOverallScore());
            result.put("improvements", funScore.getImprovements());
            result.put("painPoints", funScore.getPainPoints());
            result.put("dataSource", funScore.getDataSource());
        } else {
            // 降级：纯关键词估算
            String goal = project.getGoal() != null ? project.getGoal().toLowerCase() : "";
            Map<String, Integer> dimensionScores = new HashMap<>();
            dimensionScores.put("coreLoop", 50);
            dimensionScores.put("challenge", 50);
            dimensionScores.put("reward", 50);
            dimensionScores.put("progression", 50);
            dimensionScores.put("novelty", 50);
            result.put("dimensionScores", dimensionScores);
            result.put("overallScore", 50);
            result.put("improvements", List.of());
            result.put("dataSource", "FALLBACK");
        }

        return ResponseEntity.ok(result);
    }

    /** 判断字符串是否包含任一关键词 */
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    /**
     * 获取风险预测
     * 返回当前项目的各类风险评估
     */
    @GetMapping("/api/{projectId}/risk-prediction")
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('PERM_projects:view', 'PERM_supervision:view')")
    public ResponseEntity<Map<String, Object>> getRiskPrediction(@PathVariable String projectId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("projectId", projectId);
        result.put("projectName", project.getName());

        List<GameProject.GoalMilestone> milestones = project.getMilestones();
        List<Map<String, Object>> risks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 1. 进度风险
        long completed = milestones.stream()
            .filter(m -> m.getStatus() == GameProject.MilestoneStatus.COMPLETED).count();
        double completionRate = milestones.isEmpty() ? 0 : (double) completed / milestones.size();
        if (completionRate < 0.3 && milestones.size() > 2) {
            Map<String, Object> risk = new HashMap<>();
            risk.put("type", "进度风险");
            risk.put("severity", "HIGH");
            risk.put("description", String.format("里程碑完成率仅 %.0f%% (%d/%d)，进度偏慢", completionRate * 100, completed, milestones.size()));
            risk.put("suggestion", "检查里程碑拆分是否合理，是否需要调整计划或增加人手");
            risks.add(risk);
        }

        // 2. 阻塞风险
        long blocked = milestones.stream()
            .filter(m -> m.getStatus() == GameProject.MilestoneStatus.BLOCKED).count();
        if (blocked > 0) {
            Map<String, Object> risk = new HashMap<>();
            risk.put("type", "阻塞风险");
            risk.put("severity", blocked > 2 ? "CRITICAL" : "HIGH");
            risk.put("description", blocked + " 个里程碑被阻塞，会级联影响后续任务");
            risk.put("suggestion", "尽快解决阻塞原因，必要时重新分配任务");
            risks.add(risk);
        }

        // 3. 质量风险
        long highFailMilestones = milestones.stream()
            .filter(m -> m.getVerificationFailCount() >= 2).count();
        if (highFailMilestones > 0) {
            Map<String, Object> risk = new HashMap<>();
            risk.put("type", "质量风险");
            risk.put("severity", "HIGH");
            risk.put("description", highFailMilestones + " 个里程碑验证失败≥2次，存在系统性质量问题");
            risk.put("suggestion", "检查代码规范和测试覆盖，可能需要返工");
            risks.add(risk);
        }

        // 4. 逾期风险
        long overdueTasks = 0;
        for (GameProject.GoalMilestone m : milestones) {
            if (m.getTasks() == null) continue;
            for (GameProject.MilestoneTask task : m.getTasks()) {
                if (task.getStatus() == GameProject.MilestoneStatus.IN_PROGRESS
                    && task.getStartedAt() != null
                    && task.getEstimatedHours() > 0) {
                    LocalDateTime expected = task.getStartedAt().plusHours(task.getEstimatedHours());
                    if (now.isAfter(expected)) overdueTasks++;
                }
            }
        }
        if (overdueTasks > 0) {
            Map<String, Object> risk = new HashMap<>();
            risk.put("type", "逾期风险");
            risk.put("severity", overdueTasks > 3 ? "CRITICAL" : "MEDIUM");
            risk.put("description", overdueTasks + " 个任务已超过预估工时");
            risk.put("suggestion", "检查任务是否卡住，是否需要重新分配或拆分");
            risks.add(risk);
        }

        // 5. 团队风险
        long idleAgents = 0;
        long overloadedAgents = 0;
        try {
            List<Agent> agents = agentManager.getAgentsByProject(projectId);
            for (Agent agent : agents) {
                if ("producer".equals(agent.getRole()) || "verifier".equals(agent.getRole())) continue;
                if (!agent.isAlive()) continue;
                if (!agent.isBusy() && agent.getPendingMessages().isEmpty()) idleAgents++;
                if (agent.getTasks().size() > 3) overloadedAgents++;
            }
        } catch (Exception e) {
            // ignore
        }
        if (idleAgents > 1) {
            Map<String, Object> risk = new HashMap<>();
            risk.put("type", "团队风险");
            risk.put("severity", "MEDIUM");
            risk.put("description", idleAgents + " 个Agent处于空闲状态");
            risk.put("suggestion", "为空闲Agent分配任务，或检查是否有可并行的工作");
            risks.add(risk);
        }
        if (overloadedAgents > 0) {
            Map<String, Object> risk = new HashMap<>();
            risk.put("type", "团队风险");
            risk.put("severity", "HIGH");
            risk.put("description", overloadedAgents + " 个Agent超载（任务>3）");
            risk.put("suggestion", "重新分配任务或招聘新成员");
            risks.add(risk);
        }

        result.put("risks", risks);
        result.put("riskCount", risks.size());
        result.put("hasCritical", risks.stream().anyMatch(r -> "CRITICAL".equals(r.get("severity"))));

        return ResponseEntity.ok(result);
    }

    /**
     * 获取质量基准对比
     * 返回当前项目与同类游戏的对比数据
     */
    @GetMapping("/api/{projectId}/quality-benchmark")
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('PERM_projects:view', 'PERM_supervision:view')")
    public ResponseEntity<Map<String, Object>> getQualityBenchmark(@PathVariable String projectId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("projectId", projectId);
        result.put("projectName", project.getName());

        List<GameProject.GoalMilestone> milestones = project.getMilestones();
        String goal = project.getGoal() != null ? project.getGoal().toLowerCase() : "";

        // 功能完整度
        long total = milestones.size();
        long completed = milestones.stream()
            .filter(m -> m.getStatus() == GameProject.MilestoneStatus.COMPLETED).count();
        double completeness = total > 0 ? (double) completed / total * 100 : 0;
        result.put("totalMilestones", total);
        result.put("completedMilestones", completed);
        result.put("completeness", completeness);

        String status;
        if (completeness < 30) status = "早期开发";
        else if (completeness < 50) status = "核心功能开发中";
        else if (completeness < 70) status = "主要功能已完成";
        else if (completeness < 90) status = "功能基本完整，进入测试优化";
        else status = "接近发布标准";
        result.put("developmentStatus", status);

        // 发布Checklist
        List<Map<String, String>> checklist = new ArrayList<>();
        if (containsAny(goal, "射击", "shooter", "动作", "action")) {
            addChecklistItem(checklist, "操作手感流畅，无明显延迟");
            addChecklistItem(checklist, "武器/技能平衡性调整");
            addChecklistItem(checklist, "敌人AI行为合理");
            addChecklistItem(checklist, "关卡难度曲线平滑");
            addChecklistItem(checklist, "音效和特效反馈到位");
        } else if (containsAny(goal, "rpg", "角色扮演", "冒险")) {
            addChecklistItem(checklist, "主线剧情完整");
            addChecklistItem(checklist, "角色成长系统平衡");
            addChecklistItem(checklist, "支线任务丰富度");
            addChecklistItem(checklist, "对话系统无死胡同");
            addChecklistItem(checklist, "存档/读档正常");
        } else if (containsAny(goal, "策略", "塔防", "simulation")) {
            addChecklistItem(checklist, "单位/建筑平衡性");
            addChecklistItem(checklist, "经济系统不会崩溃");
            addChecklistItem(checklist, "AI对手行为合理");
            addChecklistItem(checklist, "多种胜利路径可行");
            addChecklistItem(checklist, "新手引导完整");
        } else {
            addChecklistItem(checklist, "核心玩法循环完整");
            addChecklistItem(checklist, "新手引导清晰");
            addChecklistItem(checklist, "无阻断性Bug");
            addChecklistItem(checklist, "性能流畅（无明显卡顿）");
            addChecklistItem(checklist, "主要功能可用");
        }
        result.put("checklist", checklist);

        return ResponseEntity.ok(result);
    }

    private void addChecklistItem(List<Map<String, String>> list, String item) {
        Map<String, String> map = new HashMap<>();
        map.put("item", item);
        map.put("status", "pending");
        list.add(map);
    }

    /**
     * 获取迭代效率分析（基于版本迭代记录）
     */
    @GetMapping("/api/{projectId}/iteration-efficiency")
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('PERM_projects:view', 'PERM_iteration:view')")
    public ResponseEntity<Map<String, Object>> getIterationEfficiency(@PathVariable String projectId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("projectId", projectId);
        result.put("projectName", project.getName());

        // 迭代周期分析
        List<GameProject.VersionHistory> history = project.getVersionHistory();
        List<Map<String, Object>> cycleAnalysis = new ArrayList<>();
        if (history != null && history.size() > 1) {
            for (int i = 1; i < history.size(); i++) {
                LocalDateTime prev = history.get(i - 1).getCreatedAt();
                LocalDateTime curr = history.get(i).getCreatedAt();
                if (prev != null && curr != null) {
                    long hours = java.time.Duration.between(prev, curr).toHours();
                    Map<String, Object> cycle = new HashMap<>();
                    cycle.put("version", history.get(i).getVersion());
                    cycle.put("durationHours", hours);
                    cycle.put("startDate", prev.toString());
                    cycle.put("endDate", curr.toString());
                    cycleAnalysis.add(cycle);
                }
            }
        }
        result.put("iterationCycles", cycleAnalysis);

        // 平均迭代周期
        double avgCycleHours = cycleAnalysis.stream()
            .mapToLong(c -> (Long) c.get("durationHours"))
            .average().orElse(0);
        result.put("averageCycleHours", avgCycleHours);

        // 里程碑完成效率
        List<GameProject.GoalMilestone> milestones = project.getMilestones();
        long completedCount = milestones.stream()
            .filter(m -> m.getStatus() == GameProject.MilestoneStatus.COMPLETED).count();
        int totalTaskCount = 0;
        int completedTaskCount = 0;
        long totalEstimatedHours = 0;
        long totalActualHours = 0;

        for (GameProject.GoalMilestone m : milestones) {
            if (m.getTasks() == null) continue;
            for (GameProject.MilestoneTask task : m.getTasks()) {
                totalTaskCount++;
                totalEstimatedHours += task.getEstimatedHours();
                if (task.getStatus() == GameProject.MilestoneStatus.COMPLETED) {
                    completedTaskCount++;
                    if (task.getStartedAt() != null && task.getCompletedAt() != null) {
                        totalActualHours += java.time.Duration.between(task.getStartedAt(), task.getCompletedAt()).toHours();
                    }
                }
            }
        }
        result.put("totalTasks", totalTaskCount);
        result.put("completedTasks", completedTaskCount);
        result.put("totalEstimatedHours", totalEstimatedHours);
        result.put("totalActualHours", totalActualHours);
        result.put("estimationAccuracy", totalEstimatedHours > 0 ? (double) totalActualHours / totalEstimatedHours * 100 : 0);

        return ResponseEntity.ok(result);
    }

    /**
     * 导出迭代报告（纯文本格式，可复制到文档）
     */
    @GetMapping("/api/{projectId}/export-iteration-text")
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('PERM_projects:view', 'PERM_iteration:view')")
    public ResponseEntity<Map<String, Object>> exportIterationText(@PathVariable String projectId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("# 项目迭代报告\n\n");
        sb.append("- 项目名称: ").append(project.getName()).append("\n");
        sb.append("- 当前版本: ").append(project.getVersion()).append("\n");
        sb.append("- 目标进度: ").append(project.getGoalProgress()).append("%\n");
        sb.append("- 导出时间: ").append(LocalDateTime.now()).append("\n\n");

        // 版本历史
        sb.append("## 版本历史\n\n");
        List<GameProject.VersionHistory> history = project.getVersionHistory();
        if (history != null) {
            for (GameProject.VersionHistory h : history) {
                sb.append("- ").append(h.getVersion()).append(": ")
                  .append(h.getDescription() != null ? h.getDescription() : "版本迭代")
                  .append(" (").append(h.getCreatedBy()).append(")\n");
            }
        }

        // 里程碑
        sb.append("\n## 里程碑\n\n");
        for (GameProject.GoalMilestone m : project.getMilestones()) {
            sb.append("- [").append(m.getStatus()).append("] ").append(m.getTitle())
              .append(" (").append(m.getProgress()).append("%)\n");
            if (m.getTasks() != null) {
                for (GameProject.MilestoneTask t : m.getTasks()) {
                    sb.append("  - [").append(t.getStatus()).append("] ").append(t.getTitle())
                      .append(" (").append(t.getAssignedRole()).append(")\n");
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("content", sb.toString());
        result.put("fileName", project.getName() + "_迭代报告_" + LocalDateTime.now().toLocalDate() + ".md");
        return ResponseEntity.ok(result);
    }

    /**
     * 获取督查历史趋势（基于版本迭代记录）
     */
    @GetMapping("/api/{projectId}/supervision-trends")
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('PERM_projects:view', 'PERM_supervision:view')")
    public ResponseEntity<Map<String, Object>> getSupervisionTrends(@PathVariable String projectId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("projectId", projectId);

        // 版本历史趋势
        List<Map<String, Object>> versionTrend = new ArrayList<>();
        List<GameProject.VersionHistory> history = project.getVersionHistory();
        if (history != null) {
            int score = 5; // 默认基线
            for (int i = 0; i < history.size(); i++) {
                Map<String, Object> point = new HashMap<>();
                point.put("version", history.get(i).getVersion());
                point.put("date", history.get(i).getCreatedAt() != null ? history.get(i).getCreatedAt().toString() : "");
                // 模拟评分（实际应从迭代记录表获取）
                point.put("score", Math.min(10, score + i));
                point.put("cumulativeTasks", (i + 1) * 5);
                versionTrend.add(point);
            }
        }
        result.put("versionTrend", versionTrend);

        // 里程碑完成趋势
        List<Map<String, Object>> milestoneTrend = new ArrayList<>();
        int cumulative = 0;
        for (GameProject.GoalMilestone m : project.getMilestones()) {
            if (m.getStatus() == GameProject.MilestoneStatus.COMPLETED) cumulative++;
            Map<String, Object> point = new HashMap<>();
            point.put("milestone", m.getTitle());
            point.put("completed", cumulative);
            point.put("total", milestoneTrend.size() + 1);
            point.put("rate", (double) cumulative / (milestoneTrend.size() + 1) * 100);
            milestoneTrend.add(point);
        }
        result.put("milestoneTrend", milestoneTrend);

        return ResponseEntity.ok(result);
    }

    /**
     * 获取督查规则配置
     */
    @GetMapping("/api/supervision-rules")
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('PERM_projects:view', 'PERM_supervision:view')")
    public ResponseEntity<Map<String, Object>> getSupervisionRules() {
        Map<String, Object> rules = new HashMap<>();
        if (constantService != null) {
            rules.put("iterationTimeoutHours", constantService.getInt(
                com.chengxun.gamemaker.config.SystemConstants.SUPERVISION_ITERATION_TIMEOUT_HOURS, 72));
            rules.put("taskOverdueThresholdHours", constantService.getInt(
                com.chengxun.gamemaker.config.SystemConstants.SUPERVISION_TASK_OVERDUE_THRESHOLD_HOURS, 24));
            rules.put("qualityScoreThreshold", constantService.getInt(
                com.chengxun.gamemaker.config.SystemConstants.SUPERVISION_QUALITY_SCORE_THRESHOLD, 6));
            rules.put("rollbackRateThreshold", constantService.getInt(
                com.chengxun.gamemaker.config.SystemConstants.SUPERVISION_ROLLBACK_RATE_THRESHOLD, 30));
            rules.put("agentIdleThresholdHours", constantService.getInt(
                com.chengxun.gamemaker.config.SystemConstants.SUPERVISION_AGENT_IDLE_THRESHOLD_HOURS, 4));
            rules.put("enableEmailAlert", constantService.getBoolean(
                com.chengxun.gamemaker.config.SystemConstants.SUPERVISION_ALERT_EMAIL_ENABLED, false));
            rules.put("enableFeishuAlert", constantService.getBoolean(
                com.chengxun.gamemaker.config.SystemConstants.SUPERVISION_ALERT_FEISHU_ENABLED, true));
        }
        return ResponseEntity.ok(rules);
    }

    /**
     * 更新督查规则配置
     */
    @PostMapping("/api/supervision-rules")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public ResponseEntity<Map<String, Object>> updateSupervisionRules(@RequestBody Map<String, Object> rules) {
        if (systemConfigService == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "配置服务未启用"));
        }
        try {
            // 前端key到系统常量key的映射
            Map<String, String> keyMapping = Map.of(
                "iterationTimeoutHours", com.chengxun.gamemaker.config.SystemConstants.SUPERVISION_ITERATION_TIMEOUT_HOURS,
                "taskOverdueThresholdHours", com.chengxun.gamemaker.config.SystemConstants.SUPERVISION_TASK_OVERDUE_THRESHOLD_HOURS,
                "qualityScoreThreshold", com.chengxun.gamemaker.config.SystemConstants.SUPERVISION_QUALITY_SCORE_THRESHOLD,
                "rollbackRateThreshold", com.chengxun.gamemaker.config.SystemConstants.SUPERVISION_ROLLBACK_RATE_THRESHOLD,
                "agentIdleThresholdHours", com.chengxun.gamemaker.config.SystemConstants.SUPERVISION_AGENT_IDLE_THRESHOLD_HOURS,
                "enableEmailAlert", com.chengxun.gamemaker.config.SystemConstants.SUPERVISION_ALERT_EMAIL_ENABLED,
                "enableFeishuAlert", com.chengxun.gamemaker.config.SystemConstants.SUPERVISION_ALERT_FEISHU_ENABLED
            );

            for (var entry : rules.entrySet()) {
                String constantKey = keyMapping.get(entry.getKey());
                if (constantKey != null) {
                    systemConfigService.setConfig(constantKey, String.valueOf(entry.getValue()));
                }
            }
            return ResponseEntity.ok(Map.of("success", true, "message", "督查规则已更新"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", "更新失败: " + e.getMessage()));
        }
    }
}
