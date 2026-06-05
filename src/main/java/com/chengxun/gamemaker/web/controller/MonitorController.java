package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.config.AppConfig;
import com.chengxun.gamemaker.engine.MessageBus;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.manager.SkillManager;
import com.chengxun.gamemaker.web.entity.ApiToken;
import com.chengxun.gamemaker.web.entity.ProjectMember;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.ApiTokenService;
import com.chengxun.gamemaker.web.service.ProjectPermissionService;
import com.chengxun.gamemaker.web.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.List;

/**
 * 监控控制器
 *
 * 权限模型：
 * - 系统级指标（CPU、JVM、内存、Token）：仅管理员可见
 * - 项目级指标（Agent 状态、项目资源）：有项目权限的用户可见
 *
 * @author chengxun
 * @since 1.0.0
 */
@Controller
@RequestMapping({"/monitor", "/api/monitor"})
public class MonitorController {

    private final AgentManager agentManager;
    private final MessageBus messageBus;
    private final ApiTokenService tokenService;
    private final ProjectManager projectManager;
    private final SkillManager skillManager;
    private final AppConfig appConfig;
    private final ProjectPermissionService permissionService;
    private final UserService userService;

    public MonitorController(AgentManager agentManager, MessageBus messageBus,
                            ApiTokenService tokenService, ProjectManager projectManager,
                            SkillManager skillManager, AppConfig appConfig,
                            ProjectPermissionService permissionService, UserService userService) {
        this.agentManager = agentManager;
        this.messageBus = messageBus;
        this.tokenService = tokenService;
        this.projectManager = projectManager;
        this.skillManager = skillManager;
        this.appConfig = appConfig;
        this.permissionService = permissionService;
        this.userService = userService;
    }

    /**
     * 系统监控主页（管理员看全局，普通用户看自己的项目）
     */
    @GetMapping
    public String monitor(Model model, Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        boolean isAdmin = user != null && user.isAdmin();

        // Agent 统计
        List<Agent> agents;
        if (isAdmin) {
            agents = agentManager.getAllAgents();
        } else if (user != null) {
            List<String> projectIds = permissionService.getUserProjectIds(user.getId());
            agents = projectIds.stream()
                .flatMap(pid -> agentManager.getAgentsByProject(pid).stream())
                .toList();
        } else {
            agents = List.of();
        }

        long onlineCount = agents.stream().filter(Agent::isAlive).count();
        long busyCount = agents.stream().filter(Agent::isBusy).count();
        long offlineCount = agents.size() - onlineCount;

        model.addAttribute("agents", agents);
        model.addAttribute("agentTotal", agents.size());
        model.addAttribute("onlineCount", onlineCount);
        model.addAttribute("busyCount", busyCount);
        model.addAttribute("offlineCount", offlineCount);

        // 系统级指标：仅管理员
        if (isAdmin) {
            List<ApiToken> allTokens = tokenService.getAllTokens();
            long activeTokenCount = allTokens.stream().filter(t -> t.getStatus() == ApiToken.TokenStatus.ACTIVE).count();
            long exhaustedTokenCount = allTokens.stream().filter(t -> t.getStatus() == ApiToken.TokenStatus.EXHAUSTED).count();
            long disabledTokenCount = allTokens.stream().filter(t -> t.getStatus() == ApiToken.TokenStatus.DISABLED).count();
            long assignedTokenCount = allTokens.stream().filter(ApiToken::isAssigned).count();
            long totalUsage = allTokens.stream().mapToLong(ApiToken::getUsageCount).sum();
            long totalTokensUsed = allTokens.stream().mapToLong(ApiToken::getTotalTokensUsed).sum();

            model.addAttribute("tokenTotal", allTokens.size());
            model.addAttribute("activeTokenCount", activeTokenCount);
            model.addAttribute("exhaustedTokenCount", exhaustedTokenCount);
            model.addAttribute("disabledTokenCount", disabledTokenCount);
            model.addAttribute("assignedTokenCount", assignedTokenCount);
            model.addAttribute("totalUsage", totalUsage);
            model.addAttribute("totalTokensUsed", totalTokensUsed);
            model.addAttribute("projectCount", projectManager.getAllProjects().size());
            model.addAttribute("skillCount", skillManager.getAllGlobalSkills().size());
            model.addAttribute("registeredAgents", messageBus.getAgents().size());

            // JVM 信息
            RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
            MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
            long uptimeMs = runtime.getUptime();
            model.addAttribute("uptimeHours", uptimeMs / 3600000);
            model.addAttribute("uptimeMinutes", (uptimeMs % 3600000) / 60000);
            model.addAttribute("jvmVersion", System.getProperty("java.version"));
            model.addAttribute("maxMemoryMB", Runtime.getRuntime().maxMemory() / 1048576);
            model.addAttribute("usedMemoryMB", memory.getHeapMemoryUsage().getUsed() / 1048576);
            model.addAttribute("threadCount", Thread.activeCount());

            // 调度器配置
            model.addAttribute("schedulerEnabled", appConfig.getScheduler().isEnabled());
            model.addAttribute("producerInterval", appConfig.getScheduler().getProducerIntervalMs() / 60000);
            model.addAttribute("agentInterval", appConfig.getScheduler().getAgentIntervalMs() / 60000);
        }

        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("username", authentication.getName());
        return "monitor";
    }

    /**
     * 项目监控页面
     */
    @GetMapping("/project/{projectId}")
    public String projectMonitor(@PathVariable String projectId, Model model, Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.VIEWER)) {
            return "redirect:/monitor?error=无权限";
        }

        List<Agent> agents = agentManager.getAgentsByProject(projectId);
        long onlineCount = agents.stream().filter(Agent::isAlive).count();
        long busyCount = agents.stream().filter(Agent::isBusy).count();

        model.addAttribute("agents", agents);
        model.addAttribute("agentTotal", agents.size());
        model.addAttribute("onlineCount", onlineCount);
        model.addAttribute("busyCount", busyCount);
        model.addAttribute("offlineCount", agents.size() - onlineCount);
        model.addAttribute("projectId", projectId);
        model.addAttribute("project", projectManager.getProject(projectId));
        model.addAttribute("isAdmin", user != null && user.isAdmin());
        model.addAttribute("username", authentication.getName());
        return "monitor";
    }
}
