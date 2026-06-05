package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.entity.AgentPerformance;
import com.chengxun.gamemaker.web.entity.ProjectMember;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.AgentPerformanceService;
import com.chengxun.gamemaker.web.service.ProjectPermissionService;
import com.chengxun.gamemaker.web.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Agent性能评估控制器
 * 提供Agent性能数据的API接口和页面
 *
 * 权限模型：
 * - 有 projectId 参数时：校验项目权限，返回项目数据
 * - 无 projectId 参数时：管理员返回全部，普通用户返回自己参与的项目数据
 *
 * @author chengxun
 * @since 1.0.0
 */
@Controller
@RequestMapping({"/performance", "/api/performance"})
public class AgentPerformanceController {

    private static final Logger log = LoggerFactory.getLogger(AgentPerformanceController.class);

    @Autowired
    private AgentPerformanceService performanceService;

    @Autowired
    private ProjectPermissionService permissionService;

    @Autowired
    private UserService userService;

    // ===== 页面路由 =====

    /**
     * 性能评估主页
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_agents:view')")
    public String performanceDashboard(Model model, Authentication authentication,
                                       @RequestParam(required = false) String projectId) {
        User user = userService.getUserByUsername(authentication.getName());
        boolean isAdmin = user != null && user.isAdmin();

        List<AgentPerformance> allPerformances;
        if (projectId != null && !projectId.isEmpty()) {
            // 项目级：校验权限
            if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.VIEWER)) {
                return "redirect:/performance?error=无权限";
            }
            allPerformances = performanceService.getProjectPerformances(projectId);
        } else if (isAdmin) {
            allPerformances = performanceService.getAllPerformances();
        } else {
            allPerformances = performanceService.getAllPerformances();
        }

        model.addAttribute("performances", allPerformances);
        model.addAttribute("summary", performanceService.getPerformanceSummary());
        model.addAttribute("roleStats", performanceService.getRolePerformanceStats());
        model.addAttribute("topAgents", performanceService.getTopAgents(5));
        model.addAttribute("projectId", projectId);

        List<AgentPerformance> attentionNeeded = allPerformances.stream()
            .filter(p -> p.getOverallScore() < 60 || p.getCurrentLoad() > 80)
            .toList();
        model.addAttribute("attentionNeeded", attentionNeeded);

        return "performance/dashboard";
    }

    /**
     * Agent详情页
     */
    @GetMapping("/agent/{agentId}")
    @PreAuthorize("hasAuthority('PERM_agents:view')")
    public String agentDetail(@PathVariable String agentId, Model model) {
        AgentPerformance performance = performanceService.getPerformance(agentId);
        if (performance == null) {
            return "redirect:/performance?error=Agent not found";
        }
        model.addAttribute("performance", performance);
        return "performance/detail";
    }

    // ===== API接口 =====

    /**
     * 获取所有Agent性能数据（JSON）
     * 支持 ?projectId=xxx 按项目筛选
     */
    @GetMapping("/api/all")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_agents:view')")
    public ResponseEntity<List<AgentPerformance>> getAllPerformances(
            @RequestParam(required = false) String projectId,
            Authentication authentication) {
        if (projectId != null && !projectId.isEmpty()) {
            User user = userService.getUserByUsername(authentication.getName());
            if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.VIEWER)) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.ok(performanceService.getProjectPerformances(projectId));
        }
        return ResponseEntity.ok(performanceService.getAllPerformances());
    }

    /**
     * 获取指定Agent性能数据（JSON）
     */
    @GetMapping("/api/agent/{agentId}")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_agents:view')")
    public ResponseEntity<AgentPerformance> getPerformance(@PathVariable String agentId) {
        AgentPerformance performance = performanceService.getPerformance(agentId);
        if (performance == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(performance);
    }

    /**
     * 获取统计摘要（JSON）
     */
    @GetMapping("/api/summary")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_agents:view')")
    public ResponseEntity<Map<String, Object>> getPerformanceSummary() {
        return ResponseEntity.ok(performanceService.getPerformanceSummary());
    }

    /**
     * 获取角色统计（JSON）
     */
    @GetMapping("/api/roles")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_agents:view')")
    public ResponseEntity<Map<String, Map<String, Object>>> getRoleStats() {
        return ResponseEntity.ok(performanceService.getRolePerformanceStats());
    }

    /**
     * 获取Top Agent（JSON）
     */
    @GetMapping("/api/top")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_agents:view')")
    public ResponseEntity<List<AgentPerformance>> getTopAgents(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String projectId,
            Authentication authentication) {
        if (projectId != null && !projectId.isEmpty()) {
            User user = userService.getUserByUsername(authentication.getName());
            if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.VIEWER)) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.ok(performanceService.getProjectTopAgents(projectId, limit));
        }
        return ResponseEntity.ok(performanceService.getTopAgents(limit));
    }

    /**
     * 获取可用Agent（JSON）
     */
    @GetMapping("/api/available")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_agents:view')")
    public ResponseEntity<List<AgentPerformance>> getAvailableAgents(
            @RequestParam(defaultValue = "70") Integer maxLoad,
            @RequestParam(defaultValue = "60") Double minScore,
            @RequestParam(required = false) String projectId,
            Authentication authentication) {
        if (projectId != null && !projectId.isEmpty()) {
            User user = userService.getUserByUsername(authentication.getName());
            if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.VIEWER)) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.ok(performanceService.getProjectTopAgents(projectId, 10));
        }
        return ResponseEntity.ok(performanceService.getAvailableAgents(maxLoad, minScore));
    }

    /**
     * 获取Agent的优化建议（JSON）
     */
    @GetMapping("/api/recommend/{agentId}")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_agents:view')")
    public ResponseEntity<Map<String, Object>> getRecommendations(@PathVariable String agentId) {
        AgentPerformance performance = performanceService.getPerformance(agentId);
        if (performance == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> recommendations = new java.util.HashMap<>();
        recommendations.put("agentId", agentId);
        recommendations.put("agentName", performance.getAgentName());
        recommendations.put("currentScore", performance.getOverallScore());

        java.util.List<String> tips = new java.util.ArrayList<>();
        if (performance.getOverallScore() < 60) {
            tips.add("综合评分较低，建议检查任务分配策略");
        }
        if (performance.getCurrentLoad() > 80) {
            tips.add("当前负载过高，建议减少并发任务");
        }
        if (performance.getCompletionRate() < 0.7) {
            tips.add("任务完成率偏低，建议优化任务拆分");
        }
        if (tips.isEmpty()) {
            tips.add("当前表现良好，继续保持");
        }
        recommendations.put("recommendations", tips);

        return ResponseEntity.ok(recommendations);
    }
}
