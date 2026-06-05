package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.service.ExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    /**
     * 导出 Agent 状态为 CSV
     */
    @GetMapping("/agents/csv")
    @PreAuthorize("hasAnyAuthority('PERM_agents:view', 'PERM_admin:manage')")
    public ResponseEntity<byte[]> exportAgentsCsv() {
        List<String> headers = List.of("ID", "名称", "角色", "状态", "项目");
        List<Map<String, Object>> rows = new ArrayList<>();
        // TODO: 从 AgentManager 获取数据填充 rows

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
        List<String> headers = List.of("时间", "用户", "操作", "详情");
        List<Map<String, Object>> rows = new ArrayList<>();
        // TODO: 从 OperationLogService 获取数据

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
