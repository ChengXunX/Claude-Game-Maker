package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.service.PerformanceManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 绩效管理Web控制器
 * 处理绩效管理页面路由
 *
 * @author chengxun
 * @since 1.0.0
 */
@Controller
public class PerformanceManagementWebController {

    private static final Logger log = LoggerFactory.getLogger(PerformanceManagementWebController.class);

    @Autowired
    private PerformanceManagementService performanceService;

    /**
     * 绩效管理页面
     */
    @GetMapping("/performance-management")
    @PreAuthorize("hasAuthority('PERM_agents:view')")
    public String performanceManagementPage(Model model) {
        log.info("Loading performance management page");

        // 获取待审批解雇申请
        model.addAttribute("pendingDismissals", performanceService.getPendingDismissalRequests());

        // 获取统计
        model.addAttribute("stats", performanceService.getPerformanceStatistics());

        return "performance-management";
    }
}
