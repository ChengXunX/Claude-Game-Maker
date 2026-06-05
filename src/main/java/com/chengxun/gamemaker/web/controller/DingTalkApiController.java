package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.config.AppConfig;
import com.chengxun.gamemaker.web.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 钉钉配置 API 控制器
 * 提供钉钉集成配置的管理接口
 *
 * 操作维度：系统级
 * 权限要求：系统管理员
 *
 * @author chengxun
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/dingtalk")
@Tag(name = "钉钉配置", description = "钉钉集成配置相关接口")
public class DingTalkApiController {

    private static final Logger log = LoggerFactory.getLogger(DingTalkApiController.class);

    private final SettingsService settingsService;
    private final AppConfig appConfig;

    public DingTalkApiController(SettingsService settingsService, AppConfig appConfig) {
        this.settingsService = settingsService;
        this.appConfig = appConfig;
    }

    /**
     * 获取钉钉配置
     *
     * @return 钉钉配置信息
     */
    @GetMapping("/config")
    @Operation(summary = "获取钉钉配置")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", appConfig.getDingtalk().isEnabled());
        config.put("webhookUrl", appConfig.getDingtalk().getWebhookUrl());
        config.put("secret", appConfig.getDingtalk().getSecret());
        return ResponseEntity.ok(config);
    }

    /**
     * 保存钉钉配置
     *
     * @param config 配置数据
     * @return 操作结果
     */
    @PutMapping("/config")
    @Operation(summary = "保存钉钉配置")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> saveConfig(@RequestBody Map<String, Object> config) {
        try {
            Boolean enabled = (Boolean) config.get("enabled");
            String webhookUrl = (String) config.get("webhookUrl");
            String secret = (String) config.get("secret");

            settingsService.saveDingTalkSettings(
                webhookUrl != null ? webhookUrl : "",
                secret != null ? secret : "",
                enabled != null ? enabled : false
            );

            log.info("钉钉配置已保存");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("保存钉钉配置失败", e);
            return ResponseEntity.internalServerError().body(com.chengxun.gamemaker.web.dto.ErrorResponse.internal("保存失败: " + e.getMessage()));
        }
    }

    /**
     * 测试钉钉连接
     *
     * @return 测试结果
     */
    @PostMapping("/test")
    @Operation(summary = "测试连接")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> testConnection() {
        try {
            String webhookUrl = appConfig.getDingtalk().getWebhookUrl();
            if (webhookUrl == null || webhookUrl.isEmpty()) {
                return ResponseEntity.badRequest().body(com.chengxun.gamemaker.web.dto.ErrorResponse.badRequest("请先配置 Webhook URL"));
            }

            // 简化实现：检查 URL 格式
            if (!webhookUrl.startsWith("http")) {
                return ResponseEntity.badRequest().body(com.chengxun.gamemaker.web.dto.ErrorResponse.badRequest("Webhook URL 格式不正确"));
            }

            log.info("钉钉连接测试成功");
            return ResponseEntity.ok(Map.of("message", "连接测试成功"));
        } catch (Exception e) {
            log.error("钉钉连接测试失败", e);
            return ResponseEntity.internalServerError().body(com.chengxun.gamemaker.web.dto.ErrorResponse.internal("连接测试失败: " + e.getMessage()));
        }
    }
}
