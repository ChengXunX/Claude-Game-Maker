package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.entity.ProjectMember;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.ProjectPermissionService;
import com.chengxun.gamemaker.web.service.ResourceUsageService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 资源使用统计控制器
 *
 * 权限模型：
 * - 系统级资源统计（Token 消耗、API 配额）：需要 PERM_system:monitor 权限
 * - 项目级资源统计：需要项目成员权限
 *
 * @author chengxun
 * @since 1.0.0
 */
@Controller
@RequestMapping("/resources")
public class ResourceUsageController {

    private static final Logger log = LoggerFactory.getLogger(ResourceUsageController.class);

    @Autowired
    private ResourceUsageService resourceUsageService;

    @Autowired
    private ProjectPermissionService permissionService;

    @Autowired
    private UserService userService;

    // ===== 页面路由 =====

    /**
     * 资源使用统计主页（系统级，需要管理员权限）
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    public String resourceDashboard(Model model) {
        model.addAttribute("todayStats", resourceUsageService.getTodayStats());
        model.addAttribute("weeklyTrend", resourceUsageService.getWeeklyTrend());
        model.addAttribute("monthlyStats", resourceUsageService.getMonthlyStats());
        model.addAttribute("agentRanking", resourceUsageService.getAgentRanking(7));
        model.addAttribute("tokenTrend", resourceUsageService.getTokenTrend(30));
        return "resources/dashboard";
    }

    /**
     * 项目资源统计页面
     */
    @GetMapping("/project/{projectId}")
    public String projectResourceDashboard(@PathVariable String projectId, Model model,
                                            Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.VIEWER)) {
            return "redirect:/resources?error=无权限";
        }

        model.addAttribute("projectId", projectId);
        model.addAttribute("todayStats", resourceUsageService.getTodayStats());
        return "resources/dashboard";
    }

    // ===== API接口 =====

    @GetMapping("/api/today")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    public ResponseEntity<Map<String, Object>> getTodayStats() {
        return ResponseEntity.ok(resourceUsageService.getTodayStats());
    }

    @GetMapping("/api/date/{date}")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    public ResponseEntity<Map<String, Object>> getDateStats(@PathVariable LocalDate date) {
        return ResponseEntity.ok(resourceUsageService.getDateStats(date));
    }

    @GetMapping("/api/range")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    public ResponseEntity<Map<String, Object>> getDateRangeStats(
            @RequestParam LocalDate startDate, @RequestParam LocalDate endDate) {
        return ResponseEntity.ok(resourceUsageService.getDateRangeStats(startDate, endDate));
    }

    @GetMapping("/api/weekly")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    public ResponseEntity<Map<String, Object>> getWeeklyTrend() {
        return ResponseEntity.ok(resourceUsageService.getWeeklyTrend());
    }

    @GetMapping("/api/monthly")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    public ResponseEntity<Map<String, Object>> getMonthlyTrend() {
        return ResponseEntity.ok(resourceUsageService.getMonthlyTrend());
    }

    @GetMapping("/api/agents")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    public ResponseEntity<List<Map<String, Object>>> getAgentRanking(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(resourceUsageService.getAgentRanking(days));
    }

    @GetMapping("/api/trend")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    public ResponseEntity<Map<String, Long>> getTokenTrend(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(resourceUsageService.getTokenTrend(days));
    }

    @GetMapping("/api/monthly-stats")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    public ResponseEntity<Map<String, Object>> getMonthlyStats() {
        return ResponseEntity.ok(resourceUsageService.getMonthlyStats());
    }
}
