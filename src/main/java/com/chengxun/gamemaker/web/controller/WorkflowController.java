package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.config.AppConfig;
import com.chengxun.gamemaker.engine.ClaudeCliEngine;
import com.chengxun.gamemaker.web.service.WorkflowEngine;
import com.chengxun.gamemaker.web.service.WorkflowEngine.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    private static final Logger log = LoggerFactory.getLogger(WorkflowController.class);

    @Autowired
    private WorkflowEngine workflowEngine;

    @Autowired
    private ClaudeCliEngine cliEngine;

    @Autowired
    private AppConfig appConfig;

    /**
     * 获取所有工作流模板
     */
    @Operation(summary = "获取模板列表", description = "获取所有可用的工作流模板")
    @GetMapping("/templates")
    @PreAuthorize("hasAuthority('PERM_workflow:view')")
    public ResponseEntity<List<WorkflowEngine.WorkflowTemplate>> getTemplates() {
        return ResponseEntity.ok(workflowEngine.getAllTemplates());
    }

    /**
     * 创建自定义工作流模板
     */
    @Operation(summary = "创建模板", description = "创建自定义工作流模板")
    @PostMapping("/templates")
    @PreAuthorize("hasAuthority('PERM_workflow:manage')")
    public ResponseEntity<Map<String, Object>> createTemplate(@RequestBody Map<String, Object> request) {
        String id = (String) request.get("id");
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        List<Map<String, Object>> stepsData = (List<Map<String, Object>>) request.get("steps");

        List<WorkflowEngine.WorkflowStep> steps = new java.util.ArrayList<>();
        if (stepsData != null) {
            for (Map<String, Object> stepData : stepsData) {
                String stepId = (String) stepData.get("id");
                String stepName = (String) stepData.get("name");
                String agentRole = (String) stepData.get("agentRole");
                String taskDesc = (String) stepData.get("taskDescription");
                WorkflowEngine.WorkflowStep step = new WorkflowEngine.WorkflowStep(stepId, stepName, agentRole, taskDesc);
                List<String> deps = (List<String>) stepData.get("dependencies");
                if (deps != null) {
                    for (String dep : deps) {
                        step.addDependency(dep);
                    }
                }
                if (stepData.containsKey("parallel")) {
                    step.setParallel((Boolean) stepData.get("parallel"));
                }
                if (stepData.containsKey("requiresApproval")) {
                    step.setRequiresApproval((Boolean) stepData.get("requiresApproval"));
                }
                steps.add(step);
            }
        }

        WorkflowEngine.WorkflowTemplate template = workflowEngine.createTemplate(id, name, description, steps);
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "templateId", template.getId(),
            "message", "模板已创建"
        ));
    }

    /**
     * 删除工作流模板
     */
    @Operation(summary = "删除模板", description = "删除指定的工作流模板")
    @DeleteMapping("/templates/{templateId}")
    @PreAuthorize("hasAuthority('PERM_workflow:manage')")
    public ResponseEntity<Map<String, Object>> deleteTemplate(@PathVariable String templateId) {
        boolean deleted = workflowEngine.deleteTemplate(templateId);
        if (deleted) {
            return ResponseEntity.ok(Map.of("status", "success", "message", "模板已删除"));
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * AI 生成工作流模板
     * 根据用户的自然语言描述，AI 自动生成工作流模板
     */
    @Operation(summary = "AI生成模板", description = "根据描述自动生成工作流模板")
    @PostMapping("/templates/generate")
    @PreAuthorize("hasAuthority('PERM_workflow:manage')")
    public ResponseEntity<Map<String, Object>> generateTemplate(@RequestBody Map<String, String> request) {
        String description = request.get("description");
        if (description == null || description.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "请提供模板描述"));
        }

        try {
            String prompt = "你是一个游戏开发工作流设计专家。根据用户描述，生成一个工作流模板。\n\n" +
                "可用的Agent角色：\n" +
                "- system-planner: 系统策划，负责需求分析和架构设计\n" +
                "- numerical-planner: 数值策划，负责游戏数值设计\n" +
                "- server-dev: 服务端开发，负责后端逻辑和API\n" +
                "- client-dev: 客户端开发，负责前端界面和交互\n" +
                "- ui-dev: UI开发，负责界面设计和实现\n" +
                "- tester: 测试，负责功能和性能测试\n" +
                "- git-commit: Git专员，负责代码管理和部署\n" +
                "- producer: 制作人，负责统筹管理\n\n" +
                "请严格按以下JSON格式返回，不要包含其他内容：\n" +
                "{\"name\":\"模板名称\",\"description\":\"模板描述\",\"steps\":[{\"id\":\"step-1\",\"name\":\"步骤名称\",\"agentRole\":\"角色\",\"taskDescription\":\"任务描述\",\"dependencies\":[],\"requiresApproval\":false}]}\n\n" +
                "注意：\n" +
                "- steps 数组中的步骤按顺序排列\n" +
                "- dependencies 是依赖的步骤id数组，第一个步骤通常为空\n" +
                "- requiresApproval 表示是否需要人工审批，部署类步骤建议设为true\n" +
                "- 请根据描述的复杂度决定步骤数量\n\n" +
                "用户描述：" + description;

            String response = cliEngine.sendMessage(
                "workflow-template-gen-" + UUID.randomUUID().toString().substring(0, 8),
                null,
                prompt,
                null,
                appConfig.getApiKey(),
                appConfig.getApiUrl(),
                appConfig.getModel()
            );

            // 提取JSON部分
            String json = extractJson(response);
            if (json == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "AI 未能生成有效的模板格式，请重试"
                ));
            }

            // 解析并注册模板
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> templateData = mapper.readValue(json, Map.class);

            String templateId = "ai-" + UUID.randomUUID().toString().substring(0, 8);
            String name = (String) templateData.get("name");
            String desc = (String) templateData.get("description");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> stepsData = (List<Map<String, Object>>) templateData.get("steps");
            List<WorkflowStep> steps = new ArrayList<>();
            if (stepsData != null) {
                for (Map<String, Object> stepData : stepsData) {
                    String stepId = (String) stepData.getOrDefault("id", "step-" + (steps.size() + 1));
                    String stepName = (String) stepData.get("name");
                    String agentRole = (String) stepData.get("agentRole");
                    String taskDesc = (String) stepData.get("taskDescription");
                    WorkflowStep step = new WorkflowStep(stepId, stepName, agentRole, taskDesc);
                    @SuppressWarnings("unchecked")
                    List<String> deps = (List<String>) stepData.get("dependencies");
                    if (deps != null) {
                        for (String dep : deps) {
                            step.addDependency(dep);
                        }
                    }
                    if (stepData.containsKey("requiresApproval")) {
                        step.setRequiresApproval((Boolean) stepData.get("requiresApproval"));
                    }
                    steps.add(step);
                }
            }

            WorkflowTemplate template = workflowEngine.createTemplate(templateId, name, desc, steps);

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "模板已生成",
                "template", template
            ));

        } catch (Exception e) {
            log.error("Failed to generate workflow template", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", "生成模板失败：" + e.getMessage()
            ));
        }
    }

    /**
     * 从AI响应中提取JSON内容
     */
    private String extractJson(String response) {
        if (response == null) return null;
        // 尝试找到JSON块
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return null;
    }

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
