package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.service.RecruitmentApprovalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 招聘审批Web控制器
 * 处理招聘审批页面路由
 *
 * @author chengxun
 * @since 1.0.0
 */
@Controller
public class RecruitmentApprovalWebController {

    private static final Logger log = LoggerFactory.getLogger(RecruitmentApprovalWebController.class);

    @Autowired
    private RecruitmentApprovalService approvalService;

    /**
     * 招聘审批页面
     */
    @GetMapping("/recruitment-approval")
    @PreAuthorize("hasAuthority('PERM_admin:manage')")
    public String approvalPage(Model model) {
        log.info("Loading recruitment approval page");

        // 获取待审批申请
        model.addAttribute("pendingRequests", approvalService.getPendingRequests());

        // 获取统计
        model.addAttribute("stats", approvalService.getRequestStatistics());

        return "recruitment-approval";
    }
}
