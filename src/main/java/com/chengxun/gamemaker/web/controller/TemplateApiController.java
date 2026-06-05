package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.service.TemplateService;
import com.chengxun.gamemaker.web.util.SecurityUtil;
import com.chengxun.gamemaker.web.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 模板管理 API 控制器
 * 提供游戏项目模板的查询和应用接口
 *
 * 主要功能：
 * - 获取模板列表
 * - 获取模板详情
 * - 基于模板创建项目
 * - 刷新模板缓存
 *
 * @author chengxun
 * @since 1.0.0
 */
@Tag(name = "模板管理", description = "游戏项目模板管理API")
@RestController
@RequestMapping("/api/templates")
public class TemplateApiController {

    private static final Logger log = LoggerFactory.getLogger(TemplateApiController.class);

    private final TemplateService templateService;
    private final ProjectManager projectManager;
    private final UserService userService;

    public TemplateApiController(TemplateService templateService,
                                  ProjectManager projectManager,
                                  UserService userService) {
        this.templateService = templateService;
        this.projectManager = projectManager;
        this.userService = userService;
    }

    /**
     * 获取所有模板列表
     *
     * @return 模板列表
     */
    @Operation(summary = "获取模板列表", description = "获取所有可用的游戏项目模板")
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public ResponseEntity<List<Map<String, Object>>> getTemplates() {
        List<Map<String, Object>> templates = templateService.getAllTemplates();
        return ResponseEntity.ok(templates);
    }

    /**
     * 获取模板详情
     *
     * @param templateId 模板ID
     * @return 模板详情
     */
    @Operation(summary = "获取模板详情", description = "根据模板ID获取模板详细信息")
    @GetMapping("/{templateId}")
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public ResponseEntity<Map<String, Object>> getTemplate(@PathVariable String templateId) {
        Map<String, Object> template = templateService.getTemplate(templateId);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(template);
    }

    /**
     * 基于模板创建项目
     *
     * @param request 创建请求，包含 name、description、workDir、templateId
     * @return 创建的项目
     */
    @Operation(summary = "基于模板创建项目", description = "使用指定模板创建新的游戏项目")
    @PostMapping("/create-project")
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public ResponseEntity<Map<String, Object>> createProjectWithTemplate(
            @RequestBody Map<String, String> request) {

        String name = request.get("name");
        String description = request.get("description");
        String workDir = request.get("workDir");
        String templateId = request.get("templateId");

        // 验证必填参数
        if (name == null || name.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "项目名称不能为空"
            ));
        }

        if (workDir == null || workDir.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "工作目录不能为空"
            ));
        }

        // 验证模板是否存在（如果指定了模板）
        if (templateId != null && !templateId.isEmpty()) {
            Map<String, Object> template = templateService.getTemplate(templateId);
            if (template == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "模板不存在: " + templateId
                ));
            }
        }

        try {
            // 创建项目
            GameProject project = projectManager.createProject(name, description, workDir, templateId);

            log.info("Project created with template: {} by user: {}",
                project.getId(), SecurityUtil.getCurrentUsername());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "项目创建成功",
                "projectId", project.getId(),
                "projectName", project.getName(),
                "templateId", templateId != null ? templateId : ""
            ));
        } catch (Exception e) {
            log.error("Failed to create project with template", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "项目创建失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 刷新模板缓存
     *
     * @return 操作结果
     */
    @Operation(summary = "刷新模板缓存", description = "重新加载所有模板")
    @PostMapping("/refresh")
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public ResponseEntity<Map<String, Object>> refreshTemplates() {
        templateService.refreshTemplates();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "模板缓存已刷新"
        ));
    }

    /**
     * 创建自定义模板
     *
     * @param request 创建请求，包含 baseTemplateId、newTemplateId、name、description
     * @return 操作结果
     */
    @Operation(summary = "创建自定义模板", description = "基于现有模板创建自定义模板")
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public ResponseEntity<Map<String, Object>> createCustomTemplate(
            @RequestBody Map<String, String> request) {

        String baseTemplateId = request.get("baseTemplateId");
        String newTemplateId = request.get("newTemplateId");
        String name = request.get("name");
        String description = request.get("description");

        // 验证必填参数
        if (baseTemplateId == null || baseTemplateId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "基础模板ID不能为空"
            ));
        }

        if (newTemplateId == null || newTemplateId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "新模板ID不能为空"
            ));
        }

        if (name == null || name.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "模板名称不能为空"
            ));
        }

        boolean created = templateService.createCustomTemplate(baseTemplateId, newTemplateId, name, description);
        if (created) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "自定义模板创建成功",
                "templateId", newTemplateId
            ));
        } else {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "自定义模板创建失败"
            ));
        }
    }
}
