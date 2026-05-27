package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.manager.SkillManager;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.web.entity.OperationLog;
import com.chengxun.gamemaker.web.service.OperationLogService;
import com.chengxun.gamemaker.web.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class DashboardController {

    private final AgentManager agentManager;
    private final ProjectManager projectManager;
    private final SkillManager skillManager;
    private final UserService userService;
    private final OperationLogService logService;

    public DashboardController(AgentManager agentManager, ProjectManager projectManager,
                              SkillManager skillManager, UserService userService, OperationLogService logService) {
        this.agentManager = agentManager;
        this.projectManager = projectManager;
        this.skillManager = skillManager;
        this.userService = userService;
        this.logService = logService;
    }

    @GetMapping("/")
    public String dashboard(Model model, Authentication authentication) {
        // 统计数据
        List<Agent> agents = agentManager.getAllAgents();
        List<GameProject> projects = projectManager.getAllProjects();

        model.addAttribute("agentCount", agents.size());
        model.addAttribute("projectCount", projects.size());
        model.addAttribute("skillCount", skillManager.getAllGlobalSkills().size());
        model.addAttribute("pendingCount", userService.getPendingCount());

        // Agent 列表
        model.addAttribute("agents", agents);

        // 项目列表
        model.addAttribute("projects", projects);

        // 最近操作
        model.addAttribute("recentLogs", logService.getRecentLogs());

        // 当前用户
        model.addAttribute("username", authentication.getName());
        model.addAttribute("isAdmin", authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));

        return "dashboard";
    }
}
