package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.entity.ProjectAgentConfig;
import com.chengxun.gamemaker.web.service.ProjectAgentConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 项目级Agent配置控制器
 * 管理项目中Agent的自定义配置，支持AI优化提示词
 *
 * @author chengxun
 * @since 2.0.0
 */
@Tag(name = "项目Agent配置", description = "项目级Agent配置管理API")
@RestController
@RequestMapping("/api/projects/{projectId}/agents")
public class ProjectAgentConfigController {

    private static final Logger log = LoggerFactory.getLogger(ProjectAgentConfigController.class);

    private final ProjectAgentConfigService configService;

    public ProjectAgentConfigController(ProjectAgentConfigService configService) {
        this.configService = configService;
    }

    /**
     * 获取项目的所有Agent配置
     */
    @Operation(summary = "获取项目Agent配置列表", description = "获取项目中所有Agent的自定义配置")
    @GetMapping("/configs")
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public ResponseEntity<List<ProjectAgentConfig>> getProjectConfigs(@PathVariable String projectId) {
        return ResponseEntity.ok(configService.getProjectConfigs(projectId));
    }

    /**
     * 获取指定角色的Agent配置
     */
    @Operation(summary = "获取Agent配置", description = "获取项目中指定角色Agent的配置")
    @GetMapping("/{agentRole}/config")
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public ResponseEntity<ProjectAgentConfig> getAgentConfig(
            @PathVariable String projectId,
            @PathVariable String agentRole) {
        ProjectAgentConfig config = configService.getConfig(projectId, agentRole);
        // 没有配置时返回200+null，而非404
        return ResponseEntity.ok(config);
    }

    /**
     * 保存Agent配置
     */
    @Operation(summary = "保存Agent配置", description = "保存或更新项目中指定角色Agent的配置")
    @PostMapping("/{agentRole}/config")
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public ResponseEntity<ProjectAgentConfig> saveAgentConfig(
            @PathVariable String projectId,
            @PathVariable String agentRole,
            @RequestBody ProjectAgentConfig config) {
        return ResponseEntity.ok(configService.saveConfig(projectId, agentRole, config));
    }

    /**
     * 获取Agent的完整提示词
     */
    @Operation(summary = "获取Agent提示词", description = "获取项目中指定角色Agent的完整提示词（默认+自定义）")
    @GetMapping("/{agentRole}/prompt")
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public ResponseEntity<Map<String, String>> getAgentPrompt(
            @PathVariable String projectId,
            @PathVariable String agentRole) {
        String prompt = configService.getFullPrompt(projectId, agentRole);
        return ResponseEntity.ok(Map.of(
            "agentRole", agentRole,
            "projectId", projectId,
            "prompt", prompt != null ? prompt : ""
        ));
    }

    /**
     * AI优化Agent提示词
     */
    @Operation(summary = "优化Agent提示词", description = "使用AI分析项目需求，优化Agent的提示词配置")
    @PostMapping("/{agentRole}/optimize")
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public ResponseEntity<Map<String, Object>> optimizeAgentPrompt(
            @PathVariable String projectId,
            @PathVariable String agentRole,
            @RequestBody(required = false) Map<String, String> body) {
        String direction = body != null ? body.get("direction") : null;
        Map<String, Object> result = configService.optimizeAgentPrompt(projectId, agentRole, direction);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取Agent的职责权重
     */
    @Operation(summary = "获取Agent职责权重", description = "获取项目中指定角色Agent的职责权重")
    @GetMapping("/{agentRole}/weights")
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public ResponseEntity<Map<String, Double>> getAgentWeights(
            @PathVariable String projectId,
            @PathVariable String agentRole) {
        return ResponseEntity.ok(configService.getResponsibilityWeights(projectId, agentRole));
    }

    /**
     * 获取Agent的绩效评分权重
     */
    @Operation(summary = "获取Agent绩效评分权重", description = "获取项目中指定角色Agent的绩效评分权重（质量/效率/协作/创新）")
    @GetMapping("/{agentRole}/performance-weights")
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public ResponseEntity<Map<String, Double>> getPerformanceWeights(
            @PathVariable String projectId,
            @PathVariable String agentRole) {
        return ResponseEntity.ok(configService.getPerformanceWeights(projectId, agentRole));
    }

    /**
     * 保存Agent的绩效评分权重
     */
    @Operation(summary = "保存Agent绩效评分权重", description = "保存项目中指定角色Agent的绩效评分权重")
    @PutMapping("/{agentRole}/performance-weights")
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public ResponseEntity<Map<String, Object>> savePerformanceWeights(
            @PathVariable String projectId,
            @PathVariable String agentRole,
            @RequestBody Map<String, Double> weights) {
        try {
            configService.savePerformanceWeights(projectId, agentRole, weights);
            return ResponseEntity.ok(Map.of("success", true, "message", "绩效评分权重已保存"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
