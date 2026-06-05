package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.manager.SkillManager;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.web.entity.ProjectMember;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.OperationLogService;
import com.chengxun.gamemaker.web.service.ProjectPermissionService;
import com.chengxun.gamemaker.web.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;

/**
 * 仪表盘控制器
 *
 * 权限模型：
 * - 管理员：系统概览（CPU、内存、全部 Agent/项目统计）+ 可按项目筛选
 * - 普通用户：只显示自己参与的项目数据
 *
 * @author chengxun
 * @since 1.0.0
 */
@Controller
public class DashboardController {

    private final AgentManager agentManager;
    private final ProjectManager projectManager;
    private final SkillManager skillManager;
    private final UserService userService;
    private final OperationLogService logService;
    private final ProjectPermissionService permissionService;

    public DashboardController(AgentManager agentManager, ProjectManager projectManager,
                              SkillManager skillManager, UserService userService,
                              OperationLogService logService, ProjectPermissionService permissionService) {
        this.agentManager = agentManager;
        this.projectManager = projectManager;
        this.skillManager = skillManager;
        this.userService = userService;
        this.logService = logService;
        this.permissionService = permissionService;
    }

    @GetMapping("/")
    public String dashboard(Model model, Authentication authentication,
                           @RequestParam(required = false) String projectId) {
        User user = userService.getUserByUsername(authentication.getName());
        boolean isAdmin = user != null && user.isAdmin();

        // 获取用户可见的项目
        List<GameProject> visibleProjects;
        if (isAdmin) {
            visibleProjects = projectManager.getAllProjects();
        } else if (user != null) {
            List<String> projectIds = permissionService.getUserProjectIds(user.getId());
            visibleProjects = projectIds.stream()
                .map(projectManager::getProject)
                .filter(p -> p != null)
                .toList();
        } else {
            visibleProjects = Collections.emptyList();
        }

        // T02: 普通用户无项目时跳转到项目列表页
        if (!isAdmin && visibleProjects.isEmpty()) {
            return "redirect:/projects";
        }

        // 如果指定了项目，检查权限
        GameProject selectedProject = null;
        if (projectId != null && !projectId.isEmpty()) {
            if (isAdmin || permissionService.isProjectMember(user, projectId)) {
                selectedProject = projectManager.getProject(projectId);
            }
        }

        // 获取 Agent 列表（按项目或全部）
        List<Agent> agents;
        if (selectedProject != null) {
            agents = agentManager.getAgentsByProject(selectedProject.getId());
        } else if (isAdmin) {
            agents = agentManager.getAllAgents();
        } else {
            // 普通用户：聚合所有参与项目的 Agent
            agents = visibleProjects.stream()
                .flatMap(p -> agentManager.getAgentsByProject(p.getId()).stream())
                .toList();
        }

        // 统计数据
        model.addAttribute("agentCount", agents.size());
        model.addAttribute("projectCount", visibleProjects.size());
        model.addAttribute("skillCount", skillManager.getAllGlobalSkills().size());
        model.addAttribute("pendingCount", userService.getPendingCount());

        // 列表数据
        model.addAttribute("agents", agents);
        model.addAttribute("projects", visibleProjects);
        model.addAttribute("selectedProject", selectedProject);
        model.addAttribute("selectedProjectId", projectId);

        // 最近操作
        model.addAttribute("recentLogs", logService.getRecentLogs());

        // 用户信息
        model.addAttribute("username", authentication.getName());
        model.addAttribute("isAdmin", isAdmin);

        return "dashboard";
    }
}
