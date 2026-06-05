package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.entity.AgentLog;
import com.chengxun.gamemaker.web.repository.AgentLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 日志详情Web控制器
 * 处理日志详情页面路由
 *
 * @author chengxun
 * @since 1.0.0
 */
@Controller
public class LogDetailWebController {

    private static final Logger log = LoggerFactory.getLogger(LogDetailWebController.class);

    @Autowired
    private AgentLogRepository agentLogRepository;

    /**
     * Agent日志详情页面
     */
    @GetMapping("/admin/agent-logs/{id}")
    @PreAuthorize("hasAuthority('PERM_log:view')")
    public String agentLogDetail(@PathVariable Long id, Model model) {
        log.info("Loading agent log detail: {}", id);

        AgentLog agentLog = agentLogRepository.findById(id).orElse(null);
        if (agentLog == null) {
            return "redirect:/admin/agent-logs?error=Log not found";
        }

        model.addAttribute("log", agentLog);
        return "log-detail";
    }

    /**
     * 操作日志详情页面
     */
    @GetMapping("/admin/operation-logs/{id}")
    @PreAuthorize("hasAuthority('PERM_log:view')")
    public String operationLogDetail(@PathVariable Long id, Model model) {
        log.info("Loading operation log detail: {}", id);

        // 这里需要OperationLogRepository
        // 暂时重定向到agent日志
        return "redirect:/admin/agent-logs";
    }
}
