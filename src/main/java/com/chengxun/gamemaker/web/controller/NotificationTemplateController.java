package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.entity.NotificationTemplate;
import com.chengxun.gamemaker.web.entity.NotificationTemplate.Category;
import com.chengxun.gamemaker.web.entity.NotificationTemplate.Channel;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.exception.BusinessException;
import com.chengxun.gamemaker.web.service.NotificationTemplateService;
import com.chengxun.gamemaker.web.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 通知模板控制器
 * 提供通知模板管理的REST API
 *
 * 权限要求：
 * - 查看模板：notification:view
 * - 管理模板：notification:manage
 *
 * @author chengxun
 * @since 1.0.0
 */
@Tag(name = "通知模板", description = "通知模板管理API")
@RestController
@RequestMapping("/api/notification-templates")
public class NotificationTemplateController {

    private static final Logger log = LoggerFactory.getLogger(NotificationTemplateController.class);

    private final NotificationTemplateService templateService;
    private final UserService userService;

    public NotificationTemplateController(NotificationTemplateService templateService,
                                           UserService userService) {
        this.templateService = templateService;
        this.userService = userService;
    }

    /**
     * 获取所有模板
     */
    @Operation(summary = "获取所有模板", description = "获取所有通知模板列表")
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_notification:manage')")
    public ResponseEntity<List<NotificationTemplate>> getAllTemplates() {
        return ResponseEntity.ok(templateService.getAllTemplates());
    }

    /**
     * 获取启用的模板
     */
    @Operation(summary = "获取启用的模板", description = "获取所有启用的通知模板")
    @GetMapping("/enabled")
    @PreAuthorize("hasAuthority('PERM_notification:manage')")
    public ResponseEntity<List<NotificationTemplate>> getEnabledTemplates() {
        return ResponseEntity.ok(templateService.getEnabledTemplates());
    }

    /**
     * 根据ID获取模板
     */
    @Operation(summary = "获取模板详情", description = "根据ID获取通知模板详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_notification:manage')")
    public ResponseEntity<NotificationTemplate> getTemplate(@PathVariable Long id) {
        NotificationTemplate template = templateService.getTemplate(id);
        if (template == null) {
            throw BusinessException.notFound("模板不存在");
        }
        return ResponseEntity.ok(template);
    }

    /**
     * 根据编码获取模板
     */
    @Operation(summary = "根据编码获取模板", description = "根据模板编码获取通知模板")
    @GetMapping("/code/{templateCode}")
    @PreAuthorize("hasAuthority('PERM_notification:manage')")
    public ResponseEntity<NotificationTemplate> getTemplateByCode(@PathVariable String templateCode) {
        NotificationTemplate template = templateService.getTemplateByCode(templateCode);
        if (template == null) {
            throw BusinessException.notFound("模板不存在: " + templateCode);
        }
        return ResponseEntity.ok(template);
    }

    /**
     * 根据渠道获取模板
     */
    @Operation(summary = "根据渠道获取模板", description = "根据通知渠道获取启用的模板")
    @GetMapping("/channel/{channel}")
    @PreAuthorize("hasAuthority('PERM_notification:manage')")
    public ResponseEntity<List<NotificationTemplate>> getTemplatesByChannel(@PathVariable Channel channel) {
        return ResponseEntity.ok(templateService.getTemplatesByChannel(channel));
    }

    /**
     * 根据分类获取模板
     */
    @Operation(summary = "根据分类获取模板", description = "根据模板分类获取启用的模板")
    @GetMapping("/category/{category}")
    @PreAuthorize("hasAuthority('PERM_notification:manage')")
    public ResponseEntity<List<NotificationTemplate>> getTemplatesByCategory(@PathVariable Category category) {
        return ResponseEntity.ok(templateService.getTemplatesByCategory(category));
    }

    /**
     * 创建模板
     */
    @Operation(summary = "创建模板", description = "创建新的通知模板")
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_notification:manage')")
    public ResponseEntity<NotificationTemplate> createTemplate(@RequestBody NotificationTemplate template) {
        NotificationTemplate created = templateService.createTemplate(template);
        log.info("Template created: {} by {}", created.getTemplateCode(), "admin");
        return ResponseEntity.ok(created);
    }

    /**
     * 更新模板
     */
    @Operation(summary = "更新模板", description = "更新通知模板")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_notification:manage')")
    public ResponseEntity<NotificationTemplate> updateTemplate(@PathVariable Long id,
                                                               @RequestBody NotificationTemplate template) {
        template.setId(id);
        NotificationTemplate updated = templateService.updateTemplate(template);
        log.info("Template updated: {} by {}", updated.getTemplateCode(), "admin");
        return ResponseEntity.ok(updated);
    }

    /**
     * 删除模板
     */
    @Operation(summary = "删除模板", description = "删除通知模板（系统内置模板不可删除）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_notification:manage')")
    public ResponseEntity<Map<String, String>> deleteTemplate(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        log.info("Template deleted: {} by {}", id, "admin");
        return ResponseEntity.ok(Map.of("status", "success", "message", "模板已删除"));
    }

    /**
     * 切换模板启用状态
     */
    @Operation(summary = "切换启用状态", description = "启用或禁用通知模板")
    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('PERM_notification:manage')")
    public ResponseEntity<Map<String, String>> toggleTemplate(@PathVariable Long id,
                                                               @RequestParam boolean enabled) {
        templateService.toggleTemplate(id, enabled);
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", enabled ? "模板已启用" : "模板已禁用"
        ));
    }

    /**
     * 预览模板
     */
    @Operation(summary = "预览模板", description = "使用提供的变量预览模板渲染结果")
    @PostMapping("/{id}/preview")
    @PreAuthorize("hasAuthority('PERM_notification:manage')")
    public ResponseEntity<Map<String, String>> previewTemplate(@PathVariable Long id,
                                                                @RequestBody Map<String, String> variables) {
        Map<String, String> result = templateService.previewTemplate(id, variables);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取变量说明
     */
    @Operation(summary = "获取变量说明", description = "获取所有可用变量的说明")
    @GetMapping("/variables")
    @PreAuthorize("hasAuthority('PERM_notification:manage')")
    public ResponseEntity<Map<String, String>> getVariableDescriptions() {
        return ResponseEntity.ok(templateService.getVariableDescriptions());
    }

    /**
     * 测试发送通知模板
     * 使用系统配置发送真实的测试通知
     * 邮件类型会发送给当前登录用户的邮箱
     */
    @Operation(summary = "测试发送", description = "使用系统配置发送测试通知")
    @PostMapping("/{id}/test")
    @PreAuthorize("hasAuthority('PERM_notification:manage')")
    public ResponseEntity<Map<String, Object>> testSendTemplate(@PathVariable Long id,
                                                                 Authentication authentication) {
        try {
            // 获取当前用户的邮箱
            String userEmail = null;
            if (authentication != null) {
                User user = userService.getUserByUsername(authentication.getName());
                if (user != null) {
                    userEmail = user.getEmail();
                }
            }

            Map<String, Object> result = templateService.testSendTemplate(id, userEmail);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("测试发送通知失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 测试发送邮件模板到指定邮箱
     */
    @Operation(summary = "测试发送邮件", description = "发送测试邮件到指定邮箱")
    @PostMapping("/{id}/test-email")
    @PreAuthorize("hasAuthority('PERM_notification:manage')")
    public ResponseEntity<Map<String, Object>> testSendEmail(@PathVariable Long id,
                                                              @RequestBody Map<String, String> request) {
        try {
            String toEmail = request.get("toEmail");
            if (toEmail == null || toEmail.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "请提供收件邮箱地址"
                ));
            }

            Map<String, Object> result = templateService.testSendEmailTemplate(id, toEmail);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("测试发送邮件失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
}
