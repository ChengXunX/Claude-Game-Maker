package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.service.WorkflowEngine;
import com.chengxun.gamemaker.web.service.WorkflowEngine.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 工作流控制器
 * 提供工作流管理的REST API
 *
 * 主要功能：
 * - 启动工作流
 * - 查询工作流状态
 * - 暂停/恢复/取消工作流
 *
 * @author chengxun
 * @since 1.0.0
 */
@Tag(name = "工作流", description = "Agent协作工作流管理API")
@RestController
@RequestMapping("/api/workflow")
public class WorkflowController {

    @Autowired
    private WorkflowEngine workflowEngine;

    /**
     * 启动工作流
     */
    @Operation(summary = "启动工作流", description = "启动一个新的工作流实例")
    @PostMapping("/start")
    @PreAuthorize("hasAuthority('PERM_workflow:manage')")
    public ResponseEntity<Map<String, Object>> startWorkflow(@RequestBody Map<String, Object> request) {
        String templateId = (String) request.get("templateId");
        String projectId = (String) request.get("projectId");
        Map<String, String> parameters = (Map<String, String>) request.get("parameters");

        WorkflowInstance instance = workflowEngine.startWorkflow(templateId, projectId, parameters);

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "instanceId", instance.getId(),
            "message", "工作流已启动"
        ));
    }

    /**
     * 获取工作流实例状态
     */
    @Operation(summary = "获取工作流状态", description = "获取工作流实例的当前状态")
    @GetMapping("/{instanceId}")
    @PreAuthorize("hasAuthority('PERM_workflow:view')")
    public ResponseEntity<WorkflowInstance> getWorkflowInstance(@PathVariable String instanceId) {
        WorkflowInstance instance = workflowEngine.getInstance(instanceId);
        if (instance == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(instance);
    }

    /**
     * 获取所有运行中的工作流
     */
    @Operation(summary = "获取运行中的工作流", description = "获取所有运行中的工作流实例")
    @GetMapping("/running")
    @PreAuthorize("hasAuthority('PERM_workflow:view')")
    public ResponseEntity<List<WorkflowInstance>> getRunningWorkflows() {
        return ResponseEntity.ok(workflowEngine.getRunningInstances());
    }

    /**
     * 暂停工作流
     */
    @Operation(summary = "暂停工作流", description = "暂停指定的工作流实例")
    @PostMapping("/{instanceId}/pause")
    @PreAuthorize("hasAuthority('PERM_workflow:manage')")
    public ResponseEntity<Map<String, Object>> pauseWorkflow(@PathVariable String instanceId) {
        workflowEngine.pauseWorkflow(instanceId);
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "工作流已暂停"
        ));
    }

    /**
     * 恢复工作流
     */
    @Operation(summary = "恢复工作流", description = "恢复暂停的工作流实例")
    @PostMapping("/{instanceId}/resume")
    @PreAuthorize("hasAuthority('PERM_workflow:manage')")
    public ResponseEntity<Map<String, Object>> resumeWorkflow(@PathVariable String instanceId) {
        workflowEngine.resumeWorkflow(instanceId);
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "工作流已恢复"
        ));
    }

    /**
     * 取消工作流
     */
    @Operation(summary = "取消工作流", description = "取消指定的工作流实例")
    @PostMapping("/{instanceId}/cancel")
    @PreAuthorize("hasAuthority('PERM_workflow:manage')")
    public ResponseEntity<Map<String, Object>> cancelWorkflow(@PathVariable String instanceId) {
        workflowEngine.cancelWorkflow(instanceId);
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "工作流已取消"
        ));
    }
}
