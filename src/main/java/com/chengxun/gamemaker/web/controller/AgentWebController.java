package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.agent.BaseAgent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.manager.SkillManager;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.model.Skill;
import com.chengxun.gamemaker.web.entity.ProjectMember;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.OperationLogService;
import com.chengxun.gamemaker.web.service.ProjectPermissionService;
import com.chengxun.gamemaker.web.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * Agent 管理控制器
 * 提供项目级 Agent 的查看、操作、管理等功能
 *
 * 路由设计：
 * - /agents — 项目选择页面（列出用户有权限的项目）
 * - /agents/project/{projectId} — 项目下的 Agent 列表
 * - /agents/project/{projectId}/{agentRole} — Agent 详情
 *
 * @author chengxun
 * @since 1.0.0
 */
@Controller
@RequestMapping("/agents")
public class AgentWebController {

    private final AgentManager agentManager;
    private final SkillManager skillManager;
    private final ProjectManager projectManager;
    private final UserService userService;
    private final OperationLogService logService;
    private final ProjectPermissionService permissionService;

    public AgentWebController(AgentManager agentManager, SkillManager skillManager,
                             ProjectManager projectManager, UserService userService,
                             OperationLogService logService, ProjectPermissionService permissionService) {
        this.agentManager = agentManager;
        this.skillManager = skillManager;
        this.projectManager = projectManager;
        this.userService = userService;
        this.logService = logService;
        this.permissionService = permissionService;
    }

    // ===== 项目选择页面 =====

    /**
     * Agent 管理首页 — 项目选择
     * 管理员看到所有项目，普通用户只看到自己参与的项目
     */
    @GetMapping
    public String listProjects(Model model, Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        List<GameProject> projects;

        if (user != null && user.isAdmin()) {
            // 管理员看到所有项目
            projects = projectManager.getAllProjects();
        } else if (user != null) {
            // 普通用户只看到自己参与的项目
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
        return "agents";
    }

    // ===== 项目级 Agent 管理 =====

    /**
     * 项目下的 Agent 列表
     */
    @GetMapping("/project/{projectId}")
    public String listProjectAgents(@PathVariable String projectId,
                                   Model model, Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.VIEWER)) {
            return "redirect:/agents";
        }

        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return "redirect:/agents";
        }

        List<Agent> agents = agentManager.getAgentsByProject(projectId);
        model.addAttribute("project", project);
        model.addAttribute("agents", agents);
        model.addAttribute("username", authentication.getName());
        return "project-agents";
    }

    /**
     * Agent 详情页
     */
    @GetMapping("/project/{projectId}/{agentRole}")
    public String agentDetail(@PathVariable String projectId,
                             @PathVariable String agentRole,
                             Model model, Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.VIEWER)) {
            return "redirect:/agents";
        }

        Agent agent = agentManager.getAgent(projectId, agentRole);
        if (agent == null) {
            return "redirect:/agents/project/" + projectId;
        }

        Map<String, Object> status = agentManager.getAgentStatus(projectId, agentRole);
        model.addAttribute("status", status);
        model.addAttribute("agentId", agent.getId());
        model.addAttribute("projectId", projectId);
        model.addAttribute("agentRole", agentRole);

        // 推理深度
        int depth = agent.getDefinition().getReasoningDepth();
        model.addAttribute("reasoningDepth", depth);
        model.addAttribute("reasoningDepthLabel", com.chengxun.gamemaker.model.AgentDefinition.getReasoningDepthLabel(depth));

        // 获取 SKILL 列表
        List<Skill> skills = skillManager.getAllSkills(projectId);
        model.addAttribute("skills", skills);

        GameProject project = projectManager.getProject(projectId);
        model.addAttribute("project", project);
        model.addAttribute("username", authentication.getName());
        return "agent-detail";
    }

    // ===== Agent 操作 =====

    /**
     * 发送任务给 Agent
     */
    @PostMapping("/project/{projectId}/{agentRole}/task")
    public String sendTask(@PathVariable String projectId,
                          @PathVariable String agentRole,
                          @RequestParam String taskContent,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.DEVELOPER)) {
            redirectAttributes.addFlashAttribute("error", "权限不足");
            return "redirect:/agents";
        }

        Agent agent = agentManager.getAgent(projectId, agentRole);
        if (agent == null) {
            redirectAttributes.addFlashAttribute("error", "Agent 不存在");
            return "redirect:/agents/project/" + projectId;
        }

        com.chengxun.gamemaker.model.AgentMessage taskMsg = com.chengxun.gamemaker.model.AgentMessage.createTask(
            authentication.getName(), agent.getId(), taskContent);
        agent.receiveMessage(taskMsg);

        redirectAttributes.addFlashAttribute("success", "任务已发送");
        return "redirect:/agents/project/" + projectId + "/" + agentRole;
    }

    /**
     * 发送查询给 Agent
     */
    @PostMapping("/project/{projectId}/{agentRole}/query")
    public String sendQuery(@PathVariable String projectId,
                           @PathVariable String agentRole,
                           @RequestParam String queryContent,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.DEVELOPER)) {
            redirectAttributes.addFlashAttribute("error", "权限不足");
            return "redirect:/agents";
        }

        Agent agent = agentManager.getAgent(projectId, agentRole);
        if (agent == null) {
            redirectAttributes.addFlashAttribute("error", "Agent 不存在");
            return "redirect:/agents/project/" + projectId;
        }

        com.chengxun.gamemaker.model.AgentMessage queryMsg = com.chengxun.gamemaker.model.AgentMessage.builder()
            .fromAgentId(authentication.getName())
            .toAgentId(agent.getId())
            .type(com.chengxun.gamemaker.model.AgentMessage.MessageType.QUERY)
            .content(queryContent)
            .build();
        agent.receiveMessage(queryMsg);

        redirectAttributes.addFlashAttribute("success", "查询已发送");
        return "redirect:/agents/project/" + projectId + "/" + agentRole;
    }

    /**
     * 启动 Agent
     */
    @PostMapping("/project/{projectId}/{agentRole}/start")
    public String startAgent(@PathVariable String projectId,
                            @PathVariable String agentRole,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.MANAGER)) {
            redirectAttributes.addFlashAttribute("error", "权限不足");
            return "redirect:/agents";
        }

        Agent agent = agentManager.getAgent(projectId, agentRole);
        if (agent == null) {
            redirectAttributes.addFlashAttribute("error", "Agent 不存在");
            return "redirect:/agents/project/" + projectId;
        }

        if (agent.isAlive()) {
            redirectAttributes.addFlashAttribute("warning", "Agent 已在运行中");
            return "redirect:/agents/project/" + projectId + "/" + agentRole;
        }

        // T11: 检查 API Key 配置
        String apiKey = agent.getDefinition().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Agent 未配置 API Key，请先配置 Token");
            return "redirect:/agents/project/" + projectId + "/" + agentRole;
        }

        try {
            agent.start();
            logService.log(getUserId(authentication), "START_AGENT", agent.getName(), "Started agent", null);
            redirectAttributes.addFlashAttribute("success", "Agent " + agent.getName() + " 已启动");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "启动失败: " + e.getMessage());
        }
        return "redirect:/agents/project/" + projectId + "/" + agentRole;
    }

    /**
     * 停止 Agent
     */
    @PostMapping("/project/{projectId}/{agentRole}/stop")
    public String stopAgent(@PathVariable String projectId,
                           @PathVariable String agentRole,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.MANAGER)) {
            redirectAttributes.addFlashAttribute("error", "权限不足");
            return "redirect:/agents";
        }

        Agent agent = agentManager.getAgent(projectId, agentRole);
        if (agent == null) {
            redirectAttributes.addFlashAttribute("error", "Agent 不存在");
            return "redirect:/agents/project/" + projectId;
        }

        if (!agent.isAlive()) {
            redirectAttributes.addFlashAttribute("warning", "Agent 已停止");
            return "redirect:/agents/project/" + projectId + "/" + agentRole;
        }

        try {
            agent.stop();
            logService.log(getUserId(authentication), "STOP_AGENT", agent.getName(), "Stopped agent", null);
            redirectAttributes.addFlashAttribute("success", "Agent " + agent.getName() + " 已停止");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "停止失败: " + e.getMessage());
        }
        return "redirect:/agents/project/" + projectId + "/" + agentRole;
    }

    /**
     * 重启 Agent
     */
    @PostMapping("/project/{projectId}/{agentRole}/restart")
    public String restartAgent(@PathVariable String projectId,
                              @PathVariable String agentRole,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.MANAGER)) {
            redirectAttributes.addFlashAttribute("error", "权限不足");
            return "redirect:/agents";
        }

        Agent agent = agentManager.getAgent(projectId, agentRole);
        if (agent == null) {
            redirectAttributes.addFlashAttribute("error", "Agent 不存在");
            return "redirect:/agents/project/" + projectId;
        }

        try {
            if (agent.isAlive()) {
                agent.stop();
            }
            agent.initialize();
            agent.start();
            logService.log(getUserId(authentication), "RESTART_AGENT", agent.getName(), "Restarted agent", null);
            redirectAttributes.addFlashAttribute("success", "Agent " + agent.getName() + " 已重启");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "重启失败: " + e.getMessage());
        }
        return "redirect:/agents/project/" + projectId + "/" + agentRole;
    }

    /**
     * 更新推理深度
     */
    @PostMapping("/project/{projectId}/{agentRole}/reasoning-depth")
    public String updateReasoningDepth(@PathVariable String projectId,
                                      @PathVariable String agentRole,
                                      @RequestParam int reasoningDepth,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.MANAGER)) {
            redirectAttributes.addFlashAttribute("error", "权限不足");
            return "redirect:/agents";
        }

        Agent agent = agentManager.getAgent(projectId, agentRole);
        if (agent == null) {
            redirectAttributes.addFlashAttribute("error", "Agent 不存在");
            return "redirect:/agents/project/" + projectId;
        }

        try {
            agent.getDefinition().setReasoningDepth(reasoningDepth);
            String label = com.chengxun.gamemaker.model.AgentDefinition.getReasoningDepthLabel(reasoningDepth);
            logService.log(getUserId(authentication), "UPDATE_REASONING_DEPTH", agent.getName(),
                "Reasoning depth set to " + label, null);
            redirectAttributes.addFlashAttribute("success", "推理深度已设置为: " + label);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "设置失败: " + e.getMessage());
        }
        return "redirect:/agents/project/" + projectId + "/" + agentRole;
    }

    private Long getUserId(Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        return user != null ? user.getId() : null;
    }
}
