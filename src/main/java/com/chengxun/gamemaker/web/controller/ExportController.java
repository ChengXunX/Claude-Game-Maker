package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.web.entity.OperationLog;
import com.chengxun.gamemaker.web.service.ExportService;
import com.chengxun.gamemaker.web.service.OperationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 数据导出控制器
 * 提供 CSV/JSON 格式的数据导出接口
 *
 * @author chengxun
 * @since 2.0.0
 */
@RestController
@RequestMapping("/api/export")
@PreAuthorize("isAuthenticated()")
public class ExportController {

    private static final Logger log = LoggerFactory.getLogger(ExportController.class);
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ExportService exportService;
    private final AgentManager agentManager;
    private final OperationLogService operationLogService;

    public ExportController(ExportService exportService,
                            AgentManager agentManager,
                            OperationLogService operationLogService) {
        this.exportService = exportService;
        this.agentManager = agentManager;
        this.operationLogService = operationLogService;
    }

    /**
     * 导出 Agent 状态为 CSV
     */
    @GetMapping("/agents/csv")
    @PreAuthorize("hasAnyAuthority('PERM_agents:view', 'PERM_admin:manage')")
    public ResponseEntity<byte[]> exportAgentsCsv() {
        List<String> headers = List.of("ID", "名称", "角色", "状态", "忙碌", "项目ID");
        List<Map<String, Object>> rows = new ArrayList<>();

        for (Agent agent : agentManager.getAllAgents()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("ID", agent.getId());
            row.put("名称", agent.getName());
            row.put("角色", agent.getRole());
            row.put("状态", agent.isAlive() ? "运行中" : "已停止");
            row.put("忙碌", agent.isBusy() ? "是" : "否");
            // 从 Agent ID 中提取项目 ID（格式: projectId:role）
            String agentId = agent.getId();
            String projId = agentId.contains(":") ? agentId.substring(0, agentId.lastIndexOf(':')) : "";
            row.put("项目ID", projId);
            rows.add(row);
        }

        byte[] csv = exportService.exportCsv(headers, rows);
        String filename = exportService.generateFilename("agents", "csv");

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .body(csv);
    }

    /**
     * 导出操作日志为 CSV
     */
    @GetMapping("/logs/csv")
    @PreAuthorize("hasAnyAuthority('PERM_system:monitor', 'PERM_admin:manage')")
    public ResponseEntity<byte[]> exportLogsCsv() {
        List<String> headers = List.of("时间", "用户", "操作", "IP地址", "详情");
        List<Map<String, Object>> rows = new ArrayList<>();

        // 获取最近操作日志
        List<OperationLog> logs = operationLogService.getRecentLogs();
        for (OperationLog log : logs) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("时间", log.getCreatedAt() != null ? log.getCreatedAt().format(DT_FMT) : "");
            row.put("用户", log.getUsername() != null ? log.getUsername() : "");
            row.put("操作", log.getAction() != null ? log.getAction() : "");
            row.put("IP地址", log.getIpAddress() != null ? log.getIpAddress() : "");
            row.put("详情", log.getDetail() != null ? log.getDetail() : "");
            rows.add(row);
        }

        byte[] csv = exportService.exportCsv(headers, rows);
        String filename = exportService.generateFilename("logs", "csv");

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .body(csv);
    }

    /**
     * 通用 JSON 导出
     */
    @PostMapping("/json")
    public ResponseEntity<byte[]> exportJson(@RequestBody List<Map<String, Object>> data) {
        byte[] json = exportService.exportJson(data);
        String filename = exportService.generateFilename("export", "json");

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
            .contentType(MediaType.APPLICATION_JSON)
            .body(json);
    }
}
