package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.entity.AgentHealth;
import com.chengxun.gamemaker.web.service.AgentHealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Agent健康检查控制器
 * 提供Agent健康状态查询和管理的REST API
 *
 * 主要功能：
 * - 查询Agent健康状态
 * - 获取健康统计
 * - 手动重启Agent
 * - 获取健康历史
 *
 * @author chengxun
 * @since 1.0.0
 */
@Tag(name = "Agent健康检查", description = "Agent健康状态监控和管理API")
@RestController
@RequestMapping("/api/health")
public class AgentHealthController {

    private static final Logger log = LoggerFactory.getLogger(AgentHealthController.class);

    @Autowired
    private AgentHealthService healthService;

    /**
     * 获取Agent健康状态
     *
     * @param agentId Agent ID
     * @return 健康记录
     */
    @Operation(summary = "获取Agent健康状态", description = "根据Agent ID获取健康状态")
    @GetMapping("/agent/{agentId}")
    @PreAuthorize("hasAuthority('PERM_agent:view')")
    public ResponseEntity<AgentHealth> getAgentHealth(@PathVariable String agentId) {
        AgentHealth health = healthService.getAgentHealth(agentId);
        if (health == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(health);
    }

    /**
     * 获取所有Agent的健康状态
     *
     * @return 健康记录列表
     */
    @Operation(summary = "获取所有Agent健康状态", description = "获取所有Agent的健康状态列表")
    @GetMapping("/agents")
    @PreAuthorize("hasAuthority('PERM_agent:view')")
    public ResponseEntity<List<AgentHealth>> getAllAgentHealth() {
        return ResponseEntity.ok(healthService.getAllAgentHealth());
    }

    /**
     * 获取健康统计
     *
     * @return 统计数据
     */
    @Operation(summary = "获取健康统计", description = "获取Agent健康状态的统计数据")
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('PERM_agent:view')")
    public ResponseEntity<Map<String, Object>> getHealthStatistics() {
        return ResponseEntity.ok(healthService.getHealthStatistics());
    }

    /**
     * 重启Agent
     *
     * @param agentId Agent ID
     * @return 操作结果
     */
    @Operation(summary = "重启Agent", description = "手动重启指定的Agent")
    @PostMapping("/agent/{agentId}/restart")
    @PreAuthorize("hasAuthority('PERM_agent:manage')")
    public ResponseEntity<Map<String, Object>> restartAgent(@PathVariable String agentId) {
        log.info("Manual restart requested for agent: {}", agentId);

        try {
            healthService.restartAgent(agentId);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Agent已重启"
            ));
        } catch (Exception e) {
            log.error("Failed to restart agent {}: {}", agentId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "重启失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取Agent健康历史
     *
     * @param agentId Agent ID
     * @param hours 查询最近N小时的数据
     * @return 健康记录列表
     */
    @Operation(summary = "获取Agent健康历史", description = "获取Agent的健康历史记录")
    @GetMapping("/agent/{agentId}/history")
    @PreAuthorize("hasAuthority('PERM_agent:view')")
    public ResponseEntity<List<AgentHealth>> getAgentHealthHistory(
            @PathVariable String agentId,
            @RequestParam(defaultValue = "24") int hours) {
        List<AgentHealth> history = healthService.getAgentHealthHistory(agentId, hours);
        return ResponseEntity.ok(history);
    }
}
