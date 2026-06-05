package com.chengxun.gamemaker.web.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.Authentication;

/**
 * 监控Dashboard Web控制器
 * 处理监控页面路由
 *
 * @author chengxun
 * @since 1.0.0
 */
@Controller
public class MonitoringDashboardWebController {

    /**
     * 监控中心页面
     */
    @GetMapping("/monitoring")
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    public String monitoringPage() {
        return "monitoring-dashboard";
    }

    /**
     * 增强版监控仪表盘（含图表）
     */
    @GetMapping("/monitoring/enhanced")
    @PreAuthorize("hasAnyAuthority('PERM_system:monitor', 'PERM_admin:manage')")
    public String enhancedDashboard(Model model, Authentication authentication) {
        model.addAttribute("username", authentication.getName());
        return "monitoring/enhanced";
    }
}
