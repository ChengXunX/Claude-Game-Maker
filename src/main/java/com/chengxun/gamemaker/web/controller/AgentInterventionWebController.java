package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.service.AgentInterventionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Agent干预Web控制器
 * 处理干预页面路由
 *
 * @author chengxun
 * @since 1.0.0
 */
@Controller
public class AgentInterventionWebController {

    private static final Logger log = LoggerFactory.getLogger(AgentInterventionWebController.class);

    private final AgentInterventionService interventionService;

    /**
     * 构造函数注入依赖
     *
     * @param interventionService 干预服务
     */
    public AgentInterventionWebController(AgentInterventionService interventionService) {
        this.interventionService = interventionService;
    }

    /**
     * Agent干预页面
     */
    @GetMapping("/agent-intervention")
    @PreAuthorize("hasAnyAuthority('PERM_agents:view', 'PERM_agents:manage', 'PERM_admin:manage')")
    public String interventionPage(Model model) {
        log.info("Loading agent intervention page");

        // 获取待处理干预
        model.addAttribute("pendingInterventions", interventionService.getAllPendingInterventions());

        // 获取统计
        model.addAttribute("stats", interventionService.getInterventionStatistics());

        return "agent-intervention";
    }
}
