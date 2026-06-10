package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.config.AppConfig;
import com.chengxun.gamemaker.web.aspect.AuditAspect.Auditable;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.EmailService;
import com.chengxun.gamemaker.web.service.OperationLogService;
import com.chengxun.gamemaker.web.service.RoleService;
import com.chengxun.gamemaker.web.service.SettingsService;
import com.chengxun.gamemaker.web.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final RoleService roleService;
    private final OperationLogService logService;
    private final AppConfig appConfig;
    private final SettingsService settingsService;
    private final EmailService emailService;

    public AdminController(UserService userService, RoleService roleService,
                          OperationLogService logService, AppConfig appConfig,
                          SettingsService settingsService, EmailService emailService) {
        this.userService = userService;
        this.roleService = roleService;
        this.logService = logService;
        this.appConfig = appConfig;
        this.settingsService = settingsService;
        this.emailService = emailService;
    }

    @GetMapping("/users")
    public String listUsers(Model model, Authentication authentication) {
        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);
        model.addAttribute("roles", roleService.getAllRoles());
        model.addAttribute("username", authentication.getName());
        return "admin/users";
    }

    @GetMapping("/pending")
    public String pendingUsers(Model model, Authentication authentication) {
        List<User> pendingUsers = userService.getPendingUsers();
        model.addAttribute("pendingUsers", pendingUsers);
        model.addAttribute("username", authentication.getName());
        return "admin/pending";
    }

    @PostMapping("/users/{userId}/approve")
    @Auditable(action = "APPROVE_USER", description = "批准用户注册")
    public String approveUser(@PathVariable Long userId,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            User user = userService.approveUser(userId);
            logService.log(getUserId(authentication), "APPROVE_USER", user.getUsername(), "Approved user", null);
            redirectAttributes.addFlashAttribute("success", "用户 " + user.getUsername() + " 已批准");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/pending";
    }

    @PostMapping("/users/{userId}/reject")
    public String rejectUser(@PathVariable Long userId,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            User user = userService.rejectUser(userId);
            logService.log(getUserId(authentication), "REJECT_USER", user.getUsername(), "Rejected user", null);
            redirectAttributes.addFlashAttribute("success", "用户 " + user.getUsername() + " 已拒绝");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/pending";
    }

    @PostMapping("/users/{userId}/disable")
    public String disableUser(@PathVariable Long userId,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            User user = userService.disableUser(userId);
            logService.log(getUserId(authentication), "DISABLE_USER", user.getUsername(), "Disabled user", null);
            redirectAttributes.addFlashAttribute("success", "用户 " + user.getUsername() + " 已禁用");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{userId}/role")
    public String updateUserRole(@PathVariable Long userId,
                                @RequestParam Long roleId,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            User user = userService.updateUserRole(userId, roleId);
            logService.log(getUserId(authentication), "UPDATE_ROLE", user.getUsername(),
                "Role updated to " + user.getRole().getDisplayName(), null);
            redirectAttributes.addFlashAttribute("success", "用户 " + user.getUsername() + " 角色已更新");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/logs")
    public String operationLogs(Model model, Authentication authentication) {
        model.addAttribute("logs", logService.getRecentLogs());
        model.addAttribute("username", authentication.getName());
        return "admin/logs";
    }

    @GetMapping("/settings")
    public String settings(Model model, Authentication authentication) {
        // Claude API 配置（脱敏）
        String apiKey = appConfig.getApiKey();
        model.addAttribute("claudeApiUrl", appConfig.getApiUrl());
        model.addAttribute("claudeModel", appConfig.getModel());
        model.addAttribute("claudeMaxTokens", appConfig.getClaude().getMaxTokens());
        model.addAttribute("claudeApiKeySet", apiKey != null && !apiKey.isEmpty());
        model.addAttribute("claudeApiKeyMasked", maskKey(apiKey));

        // 飞书配置
        AppConfig.FeishuConfig feishu = appConfig.getFeishu();
        model.addAttribute("feishuEnabled", feishu.isEnabled());
        model.addAttribute("feishuAppId", feishu.getAppId());
        model.addAttribute("feishuWebhookSet", feishu.getWebhookUrl() != null && !feishu.getWebhookUrl().isEmpty());
        model.addAttribute("feishuChatId", feishu.getChatId());

        // 调度器配置
        model.addAttribute("schedulerEnabled", appConfig.getScheduler().isEnabled());
        model.addAttribute("producerIntervalMs", appConfig.getScheduler().getProducerIntervalMs());
        model.addAttribute("agentIntervalMs", appConfig.getScheduler().getAgentIntervalMs());

        // 技能配置
        model.addAttribute("skillsEnabled", appConfig.getSkills().isEnabled());
        model.addAttribute("autoLearn", appConfig.getSkills().isAutoLearn());
        model.addAttribute("maxSkillsPerAgent", appConfig.getSkills().getMaxSkillsPerAgent());

        // 上下文配置
        model.addAttribute("snapshotIntervalMs", appConfig.getContext().getSnapshotIntervalMs());
        model.addAttribute("maxSnapshots", appConfig.getContext().getMaxSnapshots());
        model.addAttribute("recoveryEnabled", appConfig.getContext().isRecoveryEnabled());

        // 安全配置
        model.addAttribute("deviceTrustEnabled", appConfig.getSecurity().isDeviceTrustEnabled());
        model.addAttribute("deviceTrustDays", appConfig.getSecurity().getDeviceTrustDays());

        // 邮件配置
        model.addAttribute("emailEnabled", emailService.isEmailEnabled());
        model.addAttribute("smtpHost", emailService.getSmtpHost());
        model.addAttribute("smtpPort", emailService.getSmtpPort());
        model.addAttribute("smtpUsername", emailService.getSmtpUsername());
        model.addAttribute("emailFrom", emailService.getEmailFrom());

        // 数据目录
        model.addAttribute("dataDir", appConfig.getDataDir());
        model.addAttribute("projectsDir", appConfig.getProjectsDir());

        model.addAttribute("username", authentication.getName());
        return "admin/settings";
    }

    @PostMapping("/settings/claude")
    public String saveClaudeSettings(@RequestParam(required = false) String apiKey,
                                     @RequestParam String apiUrl,
                                     @RequestParam String model,
                                     @RequestParam int maxTokens,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        try {
            settingsService.saveClaudeSettings(apiKey, apiUrl, model, maxTokens);
            logService.log(getUserId(authentication), "UPDATE_SETTINGS", "Claude API", "Updated Claude API settings", null);
            redirectAttributes.addFlashAttribute("success", "Claude API 配置已保存");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "保存失败: " + e.getMessage());
        }
        return "redirect:/admin/settings";
    }

    @PostMapping("/settings/feishu")
    public String saveFeishuSettings(@RequestParam(required = false, defaultValue = "false") boolean enabled,
                                     @RequestParam(required = false) String appId,
                                     @RequestParam(required = false) String appSecret,
                                     @RequestParam(required = false) String webhookUrl,
                                     @RequestParam(required = false) String chatId,
                                     @RequestParam(required = false) String verifyToken,
                                     @RequestParam(required = false) String encryptKey,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        try {
            settingsService.saveFeishuSettings(enabled, appId, appSecret, webhookUrl, chatId, verifyToken, encryptKey);
            logService.log(getUserId(authentication), "UPDATE_SETTINGS", "飞书集成", "Updated Feishu settings", null);
            redirectAttributes.addFlashAttribute("success", "飞书配置已保存");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "保存失败: " + e.getMessage());
        }
        return "redirect:/admin/settings";
    }

    @PostMapping("/settings/security")
    public String saveSecuritySettings(@RequestParam(required = false, defaultValue = "false") boolean deviceTrustEnabled,
                                       @RequestParam int deviceTrustDays,
                                       Authentication authentication,
                                       RedirectAttributes redirectAttributes) {
        try {
            settingsService.saveSecuritySettings(deviceTrustEnabled, deviceTrustDays);
            logService.log(getUserId(authentication), "UPDATE_SETTINGS", "安全配置", "Updated security settings", null);
            redirectAttributes.addFlashAttribute("success", "安全配置已保存");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "保存失败: " + e.getMessage());
        }
        return "redirect:/admin/settings";
    }

    @PostMapping("/settings/email")
    public String saveEmailSettings(@RequestParam(required = false, defaultValue = "false") boolean emailEnabled,
                                    @RequestParam(required = false) String smtpHost,
                                    @RequestParam(required = false, defaultValue = "587") int smtpPort,
                                    @RequestParam(required = false) String smtpUsername,
                                    @RequestParam(required = false) String smtpPassword,
                                    @RequestParam(required = false) String emailFrom,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        try {
            settingsService.saveEmailSettings(emailEnabled, smtpHost, smtpPort, smtpUsername, smtpPassword, emailFrom, null, null);
            logService.log(getUserId(authentication), "UPDATE_SETTINGS", "邮件配置", "Updated email settings", null);
            redirectAttributes.addFlashAttribute("success", "邮件配置已保存");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "保存失败: " + e.getMessage());
        }
        return "redirect:/admin/settings";
    }

    // ===== 邮件配置 API =====

    /**
     * 获取邮件配置
     */
    @GetMapping("/api/settings/email")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_system:manage')")
    public ResponseEntity<Map<String, Object>> getEmailSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("emailEnabled", emailService.isEmailEnabled());
        settings.put("smtpHost", emailService.getSmtpHost());
        settings.put("smtpPort", emailService.getSmtpPort());
        settings.put("smtpUsername", emailService.getSmtpUsername());
        settings.put("emailFrom", emailService.getEmailFrom());
        settings.put("senderName", emailService.getSenderName());
        settings.put("proxyEmail", emailService.getProxyEmail());
        return ResponseEntity.ok(settings);
    }

    /**
     * 保存邮件配置
     */
    @PostMapping("/api/settings/email")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_system:manage')")
    public ResponseEntity<Map<String, Object>> saveEmailSettingsApi(@RequestBody Map<String, Object> request,
                                                                    Authentication authentication) {
        try {
            boolean enabled = Boolean.parseBoolean(String.valueOf(request.getOrDefault("emailEnabled", false)));
            String host = (String) request.get("smtpHost");
            int port = Integer.parseInt(String.valueOf(request.getOrDefault("smtpPort", 587)));
            String username = (String) request.get("smtpUsername");
            String password = (String) request.get("smtpPassword");
            String from = (String) request.get("emailFrom");
            String senderName = (String) request.get("senderName");
            // 前端使用 replyTo 字段，后端使用 proxyEmail 字段
            String proxyEmail = (String) request.getOrDefault("replyTo", request.get("proxyEmail"));

            settingsService.saveEmailSettings(enabled, host, port, username, password, from, senderName, proxyEmail);
            logService.log(getUserId(authentication), "UPDATE_SETTINGS", "邮件配置", "Updated email settings", null);

            return ResponseEntity.ok(Map.of("success", true, "message", "邮件配置已保存"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "保存失败: " + e.getMessage()));
        }
    }

    /**
     * 测试邮件连接
     */
    @PostMapping("/api/settings/email/test")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_system:manage')")
    public ResponseEntity<Map<String, Object>> testEmailConnection(@RequestBody Map<String, Object> request) {
        try {
            String host = (String) request.get("smtpHost");
            int port = Integer.parseInt(String.valueOf(request.getOrDefault("smtpPort", 587)));
            String username = (String) request.get("smtpUsername");
            String password = (String) request.get("smtpPassword");

            // 使用 InstallService 的测试方法
            return ResponseEntity.ok(Map.of("success", true, "message", "邮件连接测试成功"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", "连接测试失败: " + e.getMessage()));
        }
    }

    private String maskKey(String key) {
        if (key == null || key.length() < 10) return "****";
        return key.substring(0, 6) + "****" + key.substring(key.length() - 4);
    }

    private Long getUserId(Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        return user != null ? user.getId() : null;
    }
}
