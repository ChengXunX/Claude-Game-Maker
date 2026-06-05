package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.service.ResourceUsageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

/**
 * 资源用量 REST API 控制器
 * 提供资源使用统计和监控接口
 *
 * 权限要求：
 * - 查看资源用量：system:monitor
 *
 * @author chengxun
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/resources")
@Tag(name = "资源用量", description = "资源使用统计API")
public class ResourceUsageApiController {

    private static final Logger log = LoggerFactory.getLogger(ResourceUsageApiController.class);

    @Autowired
    private ResourceUsageService resourceUsageService;

    /**
     * 获取今日资源统计
     */
    @GetMapping("/today")
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    @Operation(summary = "获取今日资源统计")
    public ResponseEntity<Map<String, Object>> getTodayStats() {
        return ResponseEntity.ok(resourceUsageService.getTodayStats());
    }

    /**
     * 获取指定日期资源统计
     */
    @GetMapping("/date/{date}")
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    @Operation(summary = "获取指定日期资源统计")
    public ResponseEntity<Map<String, Object>> getDateStats(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return ResponseEntity.ok(resourceUsageService.getDateStats(date));
    }

    /**
     * 获取日期范围资源统计
     */
    @GetMapping("/range")
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    @Operation(summary = "获取日期范围资源统计")
    public ResponseEntity<Map<String, Object>> getRangeStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        return ResponseEntity.ok(resourceUsageService.getDateRangeStats(start, end));
    }

    /**
     * 获取周统计
     */
    @GetMapping("/weekly")
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    @Operation(summary = "获取周统计")
    public ResponseEntity<Map<String, Object>> getWeeklyStats() {
        return ResponseEntity.ok(resourceUsageService.getWeeklyTrend());
    }

    /**
     * 获取月统计
     */
    @GetMapping("/monthly")
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    @Operation(summary = "获取月统计")
    public ResponseEntity<Map<String, Object>> getMonthlyStats() {
        return ResponseEntity.ok(resourceUsageService.getMonthlyStats());
    }

    /**
     * 获取Agent资源用量
     */
    @GetMapping("/agents")
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    @Operation(summary = "获取Agent资源用量")
    public ResponseEntity<List<Map<String, Object>>> getAgentUsage() {
        return ResponseEntity.ok(resourceUsageService.getAgentRanking(30));
    }

    /**
     * 获取指定Agent资源用量
     */
    @GetMapping("/agent/{agentId}")
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    @Operation(summary = "获取指定Agent资源用量")
    public ResponseEntity<?> getAgentUsage(@PathVariable String agentId) {
        // 获取Agent排名列表，然后查找指定Agent
        List<Map<String, Object>> rankings = resourceUsageService.getAgentRanking(30);
        for (Map<String, Object> ranking : rankings) {
            if (agentId.equals(ranking.get("agentId"))) {
                return ResponseEntity.ok(ranking);
            }
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 获取资源趋势
     */
    @GetMapping("/trend")
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    @Operation(summary = "获取资源趋势")
    public ResponseEntity<Map<String, Long>> getResourceTrend(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(resourceUsageService.getTokenTrend(days));
    }

    /**
     * 获取资源配额信息
     */
    @GetMapping("/quota")
    @PreAuthorize("hasAuthority('PERM_system:monitor')")
    @Operation(summary = "获取资源配额")
    public ResponseEntity<Map<String, Object>> getQuota() {
        Map<String, Object> quota = new java.util.HashMap<>();
        quota.put("dailyTokenLimit", 1000000);
        quota.put("dailyTokenUsed", resourceUsageService.getTodayStats().getOrDefault("totalTokens", 0L));
        quota.put("monthlyTokenLimit", 30000000);
        quota.put("agentLimit", 50);
        quota.put("activeAgents", 0);
        return ResponseEntity.ok(quota);
    }
}
