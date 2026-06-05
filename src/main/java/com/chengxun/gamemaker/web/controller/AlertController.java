package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.entity.AlertRecord;
import com.chengxun.gamemaker.web.entity.AlertRule;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.AlertService;
import com.chengxun.gamemaker.web.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 告警控制器
 * 提供告警规则和告警记录的管理接口
 *
 * @author chengxun
 * @since 1.0.0
 */
@Controller
@RequestMapping("/alerts")
public class AlertController {

    private static final Logger log = LoggerFactory.getLogger(AlertController.class);

    @Autowired
    private AlertService alertService;

    @Autowired
    private UserService userService;

    // ===== 页面路由 =====

    /**
     * 告警管理主页
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    public String alertDashboard(Model model) {
        log.info("Loading alert dashboard");

        // 获取告警统计
        Map<String, Object> statistics = alertService.getAlertStatistics();
        model.addAttribute("statistics", statistics);

        // 获取待处理告警
        List<AlertRecord> pendingAlerts = alertService.getPendingAlerts();
        model.addAttribute("pendingAlerts", pendingAlerts);

        // 获取高优先级告警
        List<AlertRecord> highPriorityAlerts = alertService.getHighPriorityAlerts();
        model.addAttribute("highPriorityAlerts", highPriorityAlerts);

        // 获取最近告警（分页）
        Page<AlertRecord> recentAlerts = alertService.getAllAlerts(0, 20);
        model.addAttribute("recentAlerts", recentAlerts.getContent());

        // 获取告警规则
        List<AlertRule> rules = alertService.getAllRules();
        model.addAttribute("rules", rules);

        return "alerts/dashboard";
    }

    /**
     * 告警规则管理页
     */
    @GetMapping("/rules")
    @PreAuthorize("hasAuthority('PERM_system:monitor:manage')")
    public String alertRules(Model model) {
        log.info("Loading alert rules");

        List<AlertRule> rules = alertService.getAllRules();
        model.addAttribute("rules", rules);

        return "alerts/rules";
    }

    /**
     * 创建/编辑规则页
     */
    @GetMapping("/rules/{id}")
    @PreAuthorize("hasAuthority('PERM_system:monitor:manage')")
    public String ruleForm(@PathVariable(required = false) Long id, Model model) {
        if (id != null) {
            AlertRule rule = alertService.getRule(id);
            model.addAttribute("rule", rule);
        } else {
            model.addAttribute("rule", new AlertRule());
        }
        return "alerts/rule-form";
    }

    /**
     * 告警详情页
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    public String alertDetail(@PathVariable Long id, Model model) {
        AlertRecord alert = alertService.getAlert(id);
        model.addAttribute("alert", alert);
        return "alerts/detail";
    }

    // ===== API接口 =====

    /**
     * 获取告警统计（JSON）
     */
    @GetMapping("/api/statistics")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(alertService.getAlertStatistics());
    }

    /**
     * 获取告警趋势（JSON）
     */
    @GetMapping("/api/trend")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    public ResponseEntity<Map<String, Long>> getTrend(@RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(alertService.getAlertTrend(days));
    }

    /**
     * 获取所有告警（JSON）
     */
    @GetMapping("/api/all")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    public ResponseEntity<Page<AlertRecord>> getAllAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(alertService.getAllAlerts(page, size));
    }

    /**
     * 获取待处理告警（JSON）
     */
    @GetMapping("/api/pending")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    public ResponseEntity<List<AlertRecord>> getPendingAlerts() {
        return ResponseEntity.ok(alertService.getPendingAlerts());
    }

    /**
     * 获取高优先级告警（JSON）
     */
    @GetMapping("/api/high-priority")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    public ResponseEntity<List<AlertRecord>> getHighPriorityAlerts() {
        return ResponseEntity.ok(alertService.getHighPriorityAlerts());
    }

    /**
     * 获取告警规则（JSON）
     */
    @GetMapping("/api/rules")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    public ResponseEntity<List<AlertRule>> getRules() {
        return ResponseEntity.ok(alertService.getAllRules());
    }

    // ===== 写入接口 =====

    /**
     * 创建告警规则
     */
    @PostMapping("/api/rules")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_system:monitor:manage')")
    public ResponseEntity<AlertRule> createRule(@RequestBody AlertRule rule) {
        AlertRule created = alertService.createRule(rule);
        return ResponseEntity.ok(created);
    }

    /**
     * 更新告警规则
     */
    @PutMapping("/api/rules/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_system:monitor:manage')")
    public ResponseEntity<AlertRule> updateRule(@PathVariable Long id, @RequestBody AlertRule rule) {
        rule.setId(id);
        AlertRule updated = alertService.updateRule(rule);
        return ResponseEntity.ok(updated);
    }

    /**
     * 删除告警规则
     */
    @DeleteMapping("/api/rules/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_system:monitor:manage')")
    public ResponseEntity<Map<String, String>> deleteRule(@PathVariable Long id) {
        alertService.deleteRule(id);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Rule deleted"));
    }

    /**
     * 启用/禁用规则
     */
    @PostMapping("/api/rules/{id}/toggle")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_system:monitor:manage')")
    public ResponseEntity<Map<String, String>> toggleRule(@PathVariable Long id, @RequestParam boolean enabled) {
        alertService.toggleRule(id, enabled);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Rule " + (enabled ? "enabled" : "disabled")));
    }

    /**
     * 确认告警
     */
    @PostMapping("/api/{id}/acknowledge")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_system:monitor:manage')")
    public ResponseEntity<Map<String, String>> acknowledgeAlert(@PathVariable Long id, Authentication authentication) {
        String username = authentication.getName();
        alertService.acknowledgeAlert(id, username);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Alert acknowledged"));
    }

    /**
     * 开始处理告警
     */
    @PostMapping("/api/{id}/progress")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_system:monitor:manage')")
    public ResponseEntity<Map<String, String>> startProgress(@PathVariable Long id, Authentication authentication) {
        String username = authentication.getName();
        alertService.startProgress(id, username);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Alert in progress"));
    }

    /**
     * 解决告警
     */
    @PostMapping("/api/{id}/resolve")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_system:monitor:manage')")
    public ResponseEntity<Map<String, String>> resolveAlert(@PathVariable Long id, @RequestParam String resolution, Authentication authentication) {
        String username = authentication.getName();
        alertService.resolveAlert(id, username, resolution);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Alert resolved"));
    }

    /**
     * 忽略告警
     */
    @PostMapping("/api/{id}/ignore")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_system:monitor:manage')")
    public ResponseEntity<Map<String, String>> ignoreAlert(@PathVariable Long id) {
        alertService.ignoreAlert(id, "system");
        return ResponseEntity.ok(Map.of("status", "success", "message", "Alert ignored"));
    }

    /**
     * 批量确认所有待处理告警
     */
    @PostMapping("/api/acknowledge-all")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_system:monitor:manage')")
    public ResponseEntity<Map<String, Object>> acknowledgeAll(Authentication authentication) {
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        Long userId = user != null ? user.getId() : 0L;

        int count = alertService.acknowledgeAllPending(String.valueOf(userId));
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "已批量确认 " + count + " 个告警",
            "count", count
        ));
    }

    /**
     * 导出告警记录为CSV
     */
    @GetMapping("/api/export")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    public ResponseEntity<String> exportAlerts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(defaultValue = "30") int days) {

        String csv = alertService.exportAlertsToCsv(status, priority, days);

        return ResponseEntity.ok()
            .header("Content-Type", "text/csv; charset=UTF-8")
            .header("Content-Disposition", "attachment; filename=alerts_export.csv")
            .body(csv);
    }
}
