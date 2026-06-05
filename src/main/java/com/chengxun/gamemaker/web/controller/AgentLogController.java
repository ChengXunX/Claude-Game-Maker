package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.web.entity.AgentLog;
import com.chengxun.gamemaker.web.service.AgentLogService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * Agent日志管理控制器
 * 提供日志查看、筛选和导出功能
 */
@Controller
@RequestMapping({"/admin/agent-logs", "/api/admin/agent-logs"})
@PreAuthorize("hasAuthority('PERM_logs:view')")
public class AgentLogController {

    private final AgentLogService agentLogService;
    private final AgentManager agentManager;

    public AgentLogController(AgentLogService agentLogService, AgentManager agentManager) {
        this.agentLogService = agentLogService;
        this.agentManager = agentManager;
    }

    @GetMapping
    public String listLogs(
            @RequestParam(required = false) String agentId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Model model, Authentication authentication) {

        Page<AgentLog> logPage = agentLogService.searchLogs(
            agentId, action, level, keyword, startTime, endTime, page, size);

        model.addAttribute("logPage", logPage);
        model.addAttribute("logs", logPage.getContent());

        // 筛选条件回显
        model.addAttribute("filterAgentId", agentId);
        model.addAttribute("filterAction", action);
        model.addAttribute("filterLevel", level);
        model.addAttribute("filterKeyword", keyword);
        model.addAttribute("filterStartTime", startTime);
        model.addAttribute("filterEndTime", endTime);

        // 下拉选项
        model.addAttribute("agents", agentManager.getAllAgents());
        model.addAttribute("actions", Arrays.asList(AgentLog.Action.values()));
        model.addAttribute("levels", Arrays.asList(AgentLog.Level.values()));

        // 统计
        model.addAttribute("totalCount", agentLogService.getTotalCount());
        model.addAttribute("statsByAction", agentLogService.getStatsByAction());
        model.addAttribute("statsByAgent", agentLogService.getStatsByAgent());

        model.addAttribute("username", authentication.getName());
        return "admin/agent-logs";
    }

    /**
     * 获取Agent日志列表（API接口，返回JSON）
     */
    @GetMapping("/api/list")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_logs:view')")
    public Page<AgentLog> listLogsApi(
            @RequestParam(required = false) String agentId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return agentLogService.searchLogs(agentId, action, level, keyword, startTime, endTime, page, size);
    }

    /**
     * 导出日志为CSV格式
     */
    @GetMapping("/export")
    public void exportLogs(
            @RequestParam(required = false) String agentId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            HttpServletResponse response) throws Exception {

        // 设置响应头
        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        String filename = "agent_logs_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);

        // 添加BOM头，确保Excel正确识别UTF-8编码
        response.getOutputStream().write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF});

        // 查询日志
        List<AgentLog> logs = agentLogService.searchLogs(agentId, action, level, keyword, startTime, endTime);

        // 写入CSV
        PrintWriter writer = response.getWriter();
        // 写入表头
        writer.println("ID,Agent ID,Agent名称,操作,级别,消息,详情,IP地址,创建时间");

        // 写入数据
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (AgentLog log : logs) {
            writer.printf("%d,%s,%s,%s,%s,%s,%s,%s,%s%n",
                log.getId(),
                escapeCsv(log.getAgentId()),
                escapeCsv(log.getAgentName()),
                escapeCsv(log.getAction() != null ? log.getAction() : ""),
                escapeCsv(log.getLevel() != null ? log.getLevel() : ""),
                escapeCsv(log.getSummary()),
                escapeCsv(log.getDetail()),
                escapeCsv(""),
                log.getCreatedAt() != null ? log.getCreatedAt().format(dtf) : ""
            );
        }

        writer.flush();
    }

    /**
     * 导出操作日志为CSV格式
     */
    @GetMapping("/export-operations")
    public void exportOperationLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            HttpServletResponse response) throws Exception {

        // 设置响应头
        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        String filename = "operation_logs_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);

        // 添加BOM头
        response.getOutputStream().write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF});

        // 查询日志
        List<com.chengxun.gamemaker.web.entity.OperationLog> logs =
            agentLogService.searchOperationLogs(userId, action, startTime, endTime);

        // 写入CSV
        PrintWriter writer = response.getWriter();
        writer.println("ID,用户ID,操作,目标,详情,IP地址,创建时间");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (com.chengxun.gamemaker.web.entity.OperationLog log : logs) {
            writer.printf("%d,%s,%s,%s,%s,%s,%s%n",
                log.getId(),
                log.getUserId(),
                escapeCsv(log.getAction()),
                escapeCsv(log.getTargetName()),
                escapeCsv(log.getDetail()),
                escapeCsv(log.getIpAddress()),
                log.getCreatedAt() != null ? log.getCreatedAt().format(dtf) : ""
            );
        }

        writer.flush();
    }

    /**
     * CSV字段转义
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        // 如果包含逗号、双引号或换行符，需要用双引号包裹
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
