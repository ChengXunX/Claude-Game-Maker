package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.service.AgentRecruitmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 招聘Web控制器
 * 处理招聘页面路由
 *
 * @author chengxun
 * @since 1.0.0
 */
@Controller
public class RecruitmentWebController {

    private static final Logger log = LoggerFactory.getLogger(RecruitmentWebController.class);

    @Autowired
    private AgentRecruitmentService recruitmentService;

    /**
     * 招聘页面
     */
    @GetMapping("/recruitment")
    @PreAuthorize("hasAuthority('PERM_agent:view')")
    public String recruitmentPage(Model model) {
        log.info("Loading recruitment page");

        // 获取预设角色
        model.addAttribute("presetRoles", recruitmentService.getPresetRoleTemplates());

        // 获取招聘统计
        model.addAttribute("stats", recruitmentService.getRecruitmentStats());

        return "recruitment";
    }
}
