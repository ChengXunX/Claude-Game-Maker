package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.entity.AlertRecord;
import com.chengxun.gamemaker.web.entity.AlertRule;
import com.chengxun.gamemaker.web.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 告警 API 控制器
 * 提供告警相关的 REST API 接口
 *
 * @author chengxun
 * @since 2.0.0
 */
@RestController
@RequestMapping("/api/alerts")
@Tag(name = "告警管理", description = "告警相关接口")
public class AlertApiController {

    private static final Logger log = LoggerFactory.getLogger(AlertApiController.class);

    private final AlertService alertService;

    public AlertApiController(AlertService alertService) {
        this.alertService = alertService;
    }

    /**
     * 获取告警列表
     */
    @GetMapping
    @Operation(summary = "获取告警列表")
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    public ResponseEntity<Page<AlertRecord>> getAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(alertService.getAllAlerts(page, size));
    }

    /**
     * 获取告警统计
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取告警统计")
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(alertService.getAlertStatistics());
    }

    /**
     * 获取告警规则列表
     */
    @GetMapping("/rules")
    @Operation(summary = "获取告警规则")
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    public ResponseEntity<List<AlertRule>> getRules() {
        return ResponseEntity.ok(alertService.getAllRules());
    }

    /**
     * 创建告警规则
     */
    @PostMapping("/rules")
    @Operation(summary = "创建告警规则")
    @PreAuthorize("hasAuthority('PERM_system:manage')")
    public ResponseEntity<AlertRule> createRule(@RequestBody AlertRule rule) {
        return ResponseEntity.ok(alertService.createRule(rule));
    }

    /**
     * 更新告警规则
     */
    @PutMapping("/rules/{id}")
    @Operation(summary = "更新告警规则")
    @PreAuthorize("hasAuthority('PERM_system:manage')")
    public ResponseEntity<AlertRule> updateRule(@PathVariable Long id, @RequestBody AlertRule rule) {
        rule.setId(id);
        return ResponseEntity.ok(alertService.updateRule(rule));
    }

    /**
     * 删除告警规则
     */
    @DeleteMapping("/rules/{id}")
    @Operation(summary = "删除告警规则")
    @PreAuthorize("hasAuthority('PERM_system:manage')")
    public ResponseEntity<?> deleteRule(@PathVariable Long id) {
        alertService.deleteRule(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * 确认告警
     */
    @PostMapping("/{id}/acknowledge")
    @Operation(summary = "确认告警")
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    public ResponseEntity<?> acknowledgeAlert(@PathVariable Long id) {
        alertService.acknowledgeAlert(id, "system");
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * 解决告警
     */
    @PostMapping("/{id}/resolve")
    @Operation(summary = "解决告警")
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    public ResponseEntity<?> resolveAlert(@PathVariable Long id, @RequestBody Map<String, String> request) {
        alertService.resolveAlert(id, "system", request.get("resolution"));
        return ResponseEntity.ok(Map.of("success", true));
    }
}
