package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.config.AppConfig;
import com.chengxun.gamemaker.engine.ClaudeCliEngine;
import com.chengxun.gamemaker.web.entity.WorkflowApprovalEntity;
import com.chengxun.gamemaker.web.entity.WorkflowAuditLogEntity;
import com.chengxun.gamemaker.web.entity.WorkflowInstanceEntity;
import com.chengxun.gamemaker.web.entity.WorkflowStepExecutionEntity;
import com.chengxun.gamemaker.web.repository.WorkflowInstanceRepository;
import com.chengxun.gamemaker.web.repository.WorkflowStepExecutionRepository;
import com.chengxun.gamemaker.web.service.AgentMatchStrategy;
import com.chengxun.gamemaker.web.service.WorkflowEngine;
import com.chengxun.gamemaker.web.service.WorkflowEngine.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

    @Autowired
    private WorkflowInstanceRepository instanceRepository;

    @Autowired
    private WorkflowStepExecutionRepository stepExecutionRepository;

    @Autowired
    private AgentMatchStrategy agentMatchStrategy;

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
                if (stepData.containsKey("parallel") && stepData.get("parallel") != null) {
                    step.setParallel((Boolean) stepData.get("parallel"));
                }
                if (stepData.containsKey("requiresApproval") && stepData.get("requiresApproval") != null) {
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
                "设计原则：\n" +
                "- 测试步骤的taskDescription应说明：如测试失败需通知开发Agent修复后重新测试\n" +
                "- 部署步骤建议设置requiresApproval为true\n" +
                "- 可并行的开发步骤建议设置parallel为true\n\n" +
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
                    if (stepData.containsKey("requiresApproval") && stepData.get("requiresApproval") != null) {
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

    // ===== 实例管理（数据库持久化） =====

    /**
     * 获取所有工作流实例（从数据库）
     */
    @Operation(summary = "获取所有实例", description = "从数据库获取所有工作流实例")
    @GetMapping("/instances")
    @PreAuthorize("hasAuthority('PERM_workflow:view')")
    public ResponseEntity<List<WorkflowInstanceEntity>> getAllInstances() {
        return ResponseEntity.ok(instanceRepository.findAll());
    }

    /**
     * 按项目获取工作流实例
     */
    @Operation(summary = "按项目获取实例", description = "获取指定项目的工作流实例")
    @GetMapping("/instances/project/{projectId}")
    @PreAuthorize("hasAuthority('PERM_workflow:view')")
    public ResponseEntity<List<WorkflowInstanceEntity>> getInstancesByProject(@PathVariable String projectId) {
        return ResponseEntity.ok(instanceRepository.findByProjectIdOrderByCreatedAtDesc(projectId));
    }

    /**
     * 获取实例详情（从数据库）
     */
    @Operation(summary = "获取实例详情", description = "从数据库获取工作流实例详情")
    @GetMapping("/instances/{instanceId}/detail")
    @PreAuthorize("hasAuthority('PERM_workflow:view')")
    public ResponseEntity<Map<String, Object>> getInstanceDetail(@PathVariable String instanceId) {
        return instanceRepository.findById(instanceId)
            .map(instance -> {
                List<WorkflowStepExecutionEntity> steps = stepExecutionRepository.findByInstanceIdOrderByCreatedAtAsc(instanceId);
                List<WorkflowApprovalEntity> approvals = workflowEngine.getApprovals(instanceId);
                List<WorkflowAuditLogEntity> auditLogs = workflowEngine.getAuditLogs(instanceId);
                return ResponseEntity.ok(Map.<String, Object>of(
                    "instance", instance,
                    "steps", steps,
                    "approvals", approvals,
                    "auditLogs", auditLogs
                ));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 获取实例的步骤执行详情
     */
    @Operation(summary = "获取步骤执行详情", description = "获取工作流实例的步骤执行记录")
    @GetMapping("/instances/{instanceId}/steps")
    @PreAuthorize("hasAuthority('PERM_workflow:view')")
    public ResponseEntity<List<WorkflowStepExecutionEntity>> getStepExecutions(@PathVariable String instanceId) {
        return ResponseEntity.ok(stepExecutionRepository.findByInstanceIdOrderByCreatedAtAsc(instanceId));
    }

    // ===== 审批管理 =====

    /**
     * 审批通过
     */
    @Operation(summary = "审批通过", description = "审批通过指定的工作流步骤")
    @PostMapping("/instances/{instanceId}/steps/{stepId}/approve")
    @PreAuthorize("hasAuthority('PERM_workflow:manage')")
    public ResponseEntity<Map<String, Object>> approveStep(
            @PathVariable String instanceId,
            @PathVariable String stepId,
            @RequestBody(required = false) Map<String, String> request,
            Authentication authentication) {
        String comment = request != null ? request.get("comment") : null;
        String username = authentication.getName();

        boolean result = workflowEngine.approveStep(instanceId, stepId, null, username, comment);
        if (result) {
            return ResponseEntity.ok(Map.of("status", "success", "message", "审批通过"));
        }
        return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "审批失败，可能已审批或不存在"));
    }

    /**
     * 审批拒绝
     */
    @Operation(summary = "审批拒绝", description = "拒绝指定的工作流步骤")
    @PostMapping("/instances/{instanceId}/steps/{stepId}/reject")
    @PreAuthorize("hasAuthority('PERM_workflow:manage')")
    public ResponseEntity<Map<String, Object>> rejectStep(
            @PathVariable String instanceId,
            @PathVariable String stepId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        String comment = request.get("comment");
        String username = authentication.getName();

        boolean result = workflowEngine.rejectStep(instanceId, stepId, null, username, comment);
        if (result) {
            return ResponseEntity.ok(Map.of("status", "success", "message", "已拒绝"));
        }
        return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "拒绝失败，可能已审批或不存在"));
    }

    /**
     * 获取待审批列表
     */
    @Operation(summary = "获取待审批列表", description = "获取所有待审批的工作流步骤")
    @GetMapping("/approvals/pending")
    @PreAuthorize("hasAuthority('PERM_workflow:view')")
    public ResponseEntity<List<WorkflowApprovalEntity>> getPendingApprovals() {
        return ResponseEntity.ok(workflowEngine.getPendingApprovals());
    }

    // ===== 审计日志 =====

    /**
     * 获取实例的审计日志
     */
    @Operation(summary = "获取审计日志", description = "获取工作流实例的审计日志")
    @GetMapping("/instances/{instanceId}/audit-logs")
    @PreAuthorize("hasAuthority('PERM_workflow:view')")
    public ResponseEntity<List<WorkflowAuditLogEntity>> getAuditLogs(@PathVariable String instanceId) {
        return ResponseEntity.ok(workflowEngine.getAuditLogs(instanceId));
    }

    // ===== Agent匹配 =====

    /**
     * 获取Agent匹配评分
     * 查看指定角色的Agent综合评分，用于调试和监控
     */
    @Operation(summary = "获取Agent评分", description = "获取指定角色的Agent综合评分")
    @GetMapping("/agent-scores")
    @PreAuthorize("hasAuthority('PERM_workflow:view')")
    public ResponseEntity<List<Map<String, Object>>> getAgentScores(
            @RequestParam String role,
            @RequestParam(required = false) String projectId) {
        return ResponseEntity.ok(agentMatchStrategy.getAgentScores(role, projectId));
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

        if (templateId == null || templateId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "请选择工作流模板"
            ));
        }
        if (projectId == null || projectId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "请选择所属项目"
            ));
        }

        try {
            WorkflowInstance instance = workflowEngine.startWorkflow(templateId, projectId, parameters);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "instanceId", instance.getId(),
                "message", "工作流已启动"
            ));
        } catch (Exception e) {
            log.error("启动工作流失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "启动工作流失败: " + e.getMessage()
            ));
        }
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
