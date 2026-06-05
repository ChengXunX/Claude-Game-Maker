package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.entity.Pipeline;
import com.chengxun.gamemaker.web.entity.PipelineStage;
import com.chengxun.gamemaker.web.service.PipelineService;
import com.chengxun.gamemaker.web.service.UserService;
import com.chengxun.gamemaker.web.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * CICD流水线控制器
 * 提供流水线管理的REST API
 *
 * 主要功能：
 * - 创建流水线
 * - 触发执行
 * - 查询状态
 * - 取消执行
 *
 * @author chengxun
 * @since 1.0.0
 */
@Tag(name = "CICD流水线", description = "CICD流水线管理API")
@RestController
@RequestMapping("/api/pipelines")
public class PipelineController {

    private static final Logger log = LoggerFactory.getLogger(PipelineController.class);

    @Autowired
    private PipelineService pipelineService;

    @Autowired
    private UserService userService;

    /**
     * 创建流水线
     */
    @Operation(summary = "创建流水线", description = "创建新的CICD流水线")
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_pipeline:manage')")
    public ResponseEntity<Map<String, Object>> createPipeline(@RequestBody Map<String, String> request,
                                                               Authentication authentication) {
        String name = request.get("name");
        String description = request.get("description");
        String projectId = request.get("projectId");
        String pipelineType = request.getOrDefault("pipelineType", "FULL");
        String config = request.get("config");

        if (name == null || name.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "流水线名称不能为空"
            ));
        }

        if (projectId == null || projectId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "项目ID不能为空"
            ));
        }

        Pipeline pipeline = pipelineService.createPipeline(name, description, projectId, pipelineType, config);

        log.info("Pipeline created: {} by {}", pipeline.getPipelineNo(), authentication.getName());

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "pipelineId", pipeline.getId(),
            "pipelineNo", pipeline.getPipelineNo(),
            "message", "流水线创建成功"
        ));
    }

    /**
     * 触发流水线执行
     */
    @Operation(summary = "触发执行", description = "手动触发流水线执行")
    @PostMapping("/{pipelineId}/trigger")
    @PreAuthorize("hasAuthority('PERM_pipeline:execute')")
    public ResponseEntity<Map<String, Object>> triggerPipeline(@PathVariable Long pipelineId,
                                                                Authentication authentication) {
        Long userId = SecurityUtil.getCurrentUserId(userService);
        String username = authentication.getName();

        Pipeline pipeline = pipelineService.triggerPipeline(pipelineId, userId, username, "MANUAL");

        log.info("Pipeline triggered: {} by {}", pipeline.getPipelineNo(), username);

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "pipelineNo", pipeline.getPipelineNo(),
            "message", "流水线已触发执行"
        ));
    }

    /**
     * 取消流水线执行
     */
    @Operation(summary = "取消执行", description = "取消正在执行的流水线")
    @PostMapping("/{pipelineId}/cancel")
    @PreAuthorize("hasAuthority('PERM_pipeline:intervene')")
    public ResponseEntity<Map<String, Object>> cancelPipeline(@PathVariable Long pipelineId) {
        Pipeline pipeline = pipelineService.cancelPipeline(pipelineId);

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "pipelineNo", pipeline.getPipelineNo(),
            "message", "流水线已取消"
        ));
    }

    /**
     * 暂停流水线
     */
    @Operation(summary = "暂停流水线", description = "暂停正在执行的流水线")
    @PostMapping("/{pipelineId}/pause")
    @PreAuthorize("hasAuthority('PERM_pipeline:intervene')")
    public ResponseEntity<Map<String, Object>> pausePipeline(@PathVariable Long pipelineId) {
        Pipeline pipeline = pipelineService.pausePipeline(pipelineId);

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "pipelineNo", pipeline.getPipelineNo(),
            "message", "流水线已暂停"
        ));
    }

    /**
     * 恢复流水线
     */
    @Operation(summary = "恢复流水线", description = "恢复暂停的流水线")
    @PostMapping("/{pipelineId}/resume")
    @PreAuthorize("hasAuthority('PERM_pipeline:intervene')")
    public ResponseEntity<Map<String, Object>> resumePipeline(@PathVariable Long pipelineId) {
        Pipeline pipeline = pipelineService.resumePipeline(pipelineId);

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "pipelineNo", pipeline.getPipelineNo(),
            "message", "流水线已恢复"
        ));
    }

    /**
     * 提交审批请求
     */
    @Operation(summary = "提交审批", description = "提交流水线审批请求")
    @PostMapping("/{pipelineId}/submit-approval")
    @PreAuthorize("hasAuthority('PERM_pipeline:execute')")
    public ResponseEntity<Map<String, Object>> submitApproval(@PathVariable Long pipelineId) {
        Pipeline pipeline = pipelineService.submitApproval(pipelineId);

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "pipelineNo", pipeline.getPipelineNo(),
            "approvalStatus", pipeline.getApprovalStatusDescription(),
            "message", "审批请求已提交"
        ));
    }

    /**
     * 审批流水线
     */
    @Operation(summary = "审批流水线", description = "审批流水线（批准或拒绝）")
    @PostMapping("/{pipelineId}/approve")
    @PreAuthorize("hasAuthority('PERM_pipeline:approve')")
    public ResponseEntity<Map<String, Object>> approvePipeline(
            @PathVariable Long pipelineId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        Long userId = SecurityUtil.getCurrentUserId(userService);
        String username = authentication.getName();
        boolean approved = (Boolean) request.getOrDefault("approved", true);
        String comment = (String) request.getOrDefault("comment", "");

        Pipeline pipeline = pipelineService.approvePipeline(pipelineId, userId, username, approved, comment);

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "pipelineNo", pipeline.getPipelineNo(),
            "approvalStatus", pipeline.getApprovalStatusDescription(),
            "message", approved ? "流水线已批准" : "流水线已拒绝"
        ));
    }

    /**
     * 获取流水线详情
     */
    @Operation(summary = "获取详情", description = "获取流水线详细信息")
    @GetMapping("/{pipelineId}")
    @PreAuthorize("hasAuthority('PERM_pipeline:view')")
    public ResponseEntity<Pipeline> getPipeline(@PathVariable Long pipelineId) {
        Pipeline pipeline = pipelineService.getPipeline(pipelineId);
        if (pipeline == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(pipeline);
    }

    /**
     * 获取流水线阶段
     */
    @Operation(summary = "获取阶段", description = "获取流水线的所有阶段")
    @GetMapping("/{pipelineId}/stages")
    @PreAuthorize("hasAuthority('PERM_pipeline:view')")
    public ResponseEntity<List<PipelineStage>> getPipelineStages(@PathVariable Long pipelineId) {
        return ResponseEntity.ok(pipelineService.getPipelineStages(pipelineId));
    }

    /**
     * 获取项目的所有流水线
     */
    @Operation(summary = "获取项目流水线", description = "获取指定项目的所有流水线")
    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAuthority('PERM_pipeline:view')")
    public ResponseEntity<List<Pipeline>> getProjectPipelines(@PathVariable String projectId) {
        return ResponseEntity.ok(pipelineService.getProjectPipelines(projectId));
    }

    /**
     * 获取所有流水线
     */
    @Operation(summary = "获取所有流水线", description = "获取所有流水线列表")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('PERM_pipeline:view')")
    public ResponseEntity<List<Pipeline>> getAllPipelines() {
        return ResponseEntity.ok(pipelineService.getAllPipelines());
    }

    /**
     * 获取正在运行的流水线
     */
    @Operation(summary = "获取运行中的流水线", description = "获取所有正在执行的流水线")
    @GetMapping("/running")
    @PreAuthorize("hasAuthority('PERM_pipeline:view')")
    public ResponseEntity<List<Pipeline>> getRunningPipelines() {
        return ResponseEntity.ok(pipelineService.getRunningPipelines());
    }

    /**
     * 获取流水线统计
     */
    @Operation(summary = "获取统计", description = "获取流水线统计数据")
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('PERM_pipeline:view')")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(pipelineService.getPipelineStatistics());
    }
}
