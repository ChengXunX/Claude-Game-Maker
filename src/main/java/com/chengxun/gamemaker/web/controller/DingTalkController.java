package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.config.AppConfig;
import com.chengxun.gamemaker.dingtalk.DingTalkService;
import com.chengxun.gamemaker.web.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

/**
 * 钉钉机器人控制器
 * 提供钉钉机器人配置管理和测试功能
 *
 * @author chengxun
 * @since 1.0.0
 */
@Tag(name = "钉钉机器人", description = "钉钉机器人配置管理API")
@Controller
@RequestMapping("/dingtalk")
public class DingTalkController {

    private static final Logger log = LoggerFactory.getLogger(DingTalkController.class);

    private final DingTalkService dingTalkService;
    private final AppConfig appConfig;
    private final SettingsService settingsService;

    public DingTalkController(DingTalkService dingTalkService, AppConfig appConfig, SettingsService settingsService) {
        this.dingTalkService = dingTalkService;
        this.appConfig = appConfig;
        this.settingsService = settingsService;
    }

    // ===== 页面路由 =====

    /**
     * 钉钉配置页面
     */
    @GetMapping("/config")
    @PreAuthorize("hasAuthority('PERM_system:manage')")
    public String configPage(Model model) {
        AppConfig.DingTalkConfig config = appConfig.getDingtalk();
        model.addAttribute("config", config);
        model.addAttribute("configInfo", dingTalkService.getConfigInfo());
        return "dingtalk/config";
    }

    // ===== API接口 =====

    /**
     * 获取钉钉配置
     */
    @Operation(summary = "获取钉钉配置", description = "获取钉钉机器人配置信息")
    @GetMapping("/api/config")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_system:view')")
    public ResponseEntity<Map<String, Object>> getConfig() {
        return ResponseEntity.ok(dingTalkService.getConfigInfo());
    }

    /**
     * 更新钉钉配置
     */
    @Operation(summary = "更新钉钉配置", description = "更新钉钉机器人配置")
    @PostMapping("/api/config")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_system:manage')")
    public ResponseEntity<Map<String, String>> updateConfig(@RequestBody Map<String, Object> request) {
        try {
            AppConfig.DingTalkConfig config = appConfig.getDingtalk();

            if (request.containsKey("webhookUrl")) {
                config.setWebhookUrl((String) request.get("webhookUrl"));
            }
            if (request.containsKey("secret")) {
                config.setSecret((String) request.get("secret"));
            }
            if (request.containsKey("enabled")) {
                config.setEnabled((Boolean) request.get("enabled"));
            }

            log.info("DingTalk config updated");
            return ResponseEntity.ok(Map.of("status", "success", "message", "配置已更新"));
        } catch (Exception e) {
            log.error("Failed to update DingTalk config", e);
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * 保存配置（表单提交）
     */
    @PostMapping("/save")
    @PreAuthorize("hasAuthority('PERM_system:manage')")
    public String saveConfig(@RequestParam String webhookUrl,
                              @RequestParam(required = false) String secret,
                              @RequestParam(required = false, defaultValue = "false") boolean enabled,
                              RedirectAttributes redirectAttributes) {
        try {
            settingsService.saveDingTalkSettings(webhookUrl, secret, enabled);

            log.info("DingTalk config saved");
            redirectAttributes.addFlashAttribute("success", "钉钉配置已保存");
        } catch (Exception e) {
            log.error("Failed to save DingTalk config", e);
            redirectAttributes.addFlashAttribute("error", "保存失败: " + e.getMessage());
        }
        return "redirect:/dingtalk/config";
    }

    /**
     * 测试钉钉连接
     */
    @Operation(summary = "测试连接", description = "测试钉钉机器人连接")
    @PostMapping("/api/test")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_system:manage')")
    public ResponseEntity<Map<String, Object>> testConnection() {
        Map<String, Object> result = dingTalkService.testConnection();
        return ResponseEntity.ok(result);
    }

    /**
     * 测试连接（表单提交）
     */
    @PostMapping("/test")
    @PreAuthorize("hasAuthority('PERM_system:manage')")
    public String testConnectionForm(RedirectAttributes redirectAttributes) {
        Map<String, Object> result = dingTalkService.testConnection();
        if ((Boolean) result.get("success")) {
            redirectAttributes.addFlashAttribute("success", result.get("message"));
        } else {
            redirectAttributes.addFlashAttribute("error", result.get("message"));
        }
        return "redirect:/dingtalk/config";
    }
}
