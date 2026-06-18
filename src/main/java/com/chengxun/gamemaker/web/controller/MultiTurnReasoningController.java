package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.service.MultiTurnReasoningService;
import com.chengxun.gamemaker.web.entity.MultiTurnRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 多轮推理控制器
 * 提供多轮推理的触发、状态查询和统计接口
 *
 * @author chengxun
 * @since 3.0.0
 */
@RestController
@RequestMapping("/api/multi-turn")
@Tag(name = "多轮推理", description = "多轮推理管理接口")
public class MultiTurnReasoningController {

    @Autowired
    private MultiTurnReasoningService reasoningService;

    /**
     * 触发多轮推理（异步）
     * 立即返回记录ID，后台执行推理，前端通过 getStatus 轮询进度
     */
    @PostMapping("/reason")
    @Operation(summary = "触发多轮推理", description = "异步执行 Think→Plan→Act→Verify 循环，返回记录ID供轮询")
    @PreAuthorize("hasAnyAuthority('PERM_reasoning:manage', 'PERM_agents:manage', 'PERM_admin:manage', '*')")
    public ResponseEntity<Map<String, Object>> reason(@RequestBody Map<String, String> request) {
        String agentId = request.get("agentId");
        String projectId = request.get("projectId");
        String taskId = request.get("taskId");
        String taskDescription = request.get("taskDescription");

        MultiTurnRecord record = reasoningService.reason(agentId, projectId, taskId, taskDescription);

        Map<String, Object> result = new HashMap<>();
        result.put("id", record.getId());
        result.put("status", record.getStatus().name());
        result.put("message", "推理已启动，请通过 /api/multi-turn/status/" + record.getId() + " 查询进度");
        return ResponseEntity.ok(result);
    }

    /**
     * 查询推理状态（前端轮询）
     */
    @GetMapping("/status/{recordId}")
    @Operation(summary = "查询推理状态")
    @PreAuthorize("hasAnyAuthority('PERM_reasoning:view', 'PERM_agents:view', 'PERM_admin:manage', '*')")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable Long recordId) {
        MultiTurnRecord record = reasoningService.getStatus(recordId);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", record.getId());
        result.put("status", record.getStatus().name());
        result.put("turnNumber", record.getTurnNumber());
        result.put("maxTurns", record.getMaxTurns());
        result.put("thinkResult", record.getThinkResult());
        result.put("planResult", record.getPlanResult());
        result.put("actResult", record.getActResult());
        result.put("verifyResult", record.getVerifyResult());
        result.put("verifyPassed", record.getVerifyPassed());
        result.put("durationMs", record.getDurationMs());
        result.put("running", reasoningService.isRunning(recordId));
        result.put("createdAt", record.getCreatedAt());
        result.put("updatedAt", record.getUpdatedAt());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats/{projectId}")
    @Operation(summary = "获取推理统计")
    @PreAuthorize("hasAnyAuthority('PERM_reasoning:view', 'PERM_agents:view', 'PERM_admin:manage', '*')")
    public ResponseEntity<Map<String, Object>> getStats(@PathVariable String projectId) {
        return ResponseEntity.ok(reasoningService.getStats(projectId));
    }

    @GetMapping("/history/{projectId}")
    @Operation(summary = "获取推理历史")
    @PreAuthorize("hasAnyAuthority('PERM_reasoning:view', 'PERM_agents:view', 'PERM_admin:manage', '*')")
    public ResponseEntity<List<MultiTurnRecord>> getHistory(@PathVariable String projectId) {
        return ResponseEntity.ok(reasoningService.getHistory(projectId));
    }
}
