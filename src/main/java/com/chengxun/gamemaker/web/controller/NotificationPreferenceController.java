package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.entity.NotificationPreference;
import com.chengxun.gamemaker.web.entity.NotificationPreference.Channel;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.NotificationPreferenceService;
import com.chengxun.gamemaker.web.service.NotificationPreferenceService.NotificationType;
import com.chengxun.gamemaker.web.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 通知偏好控制器
 *
 * @author chengxun
 * @since 2.0.0
 */
@Controller
@RequestMapping({"/notification-preferences", "/api/notification-preferences"})
public class NotificationPreferenceController {

    private static final Logger log = LoggerFactory.getLogger(NotificationPreferenceController.class);

    private final NotificationPreferenceService preferenceService;
    private final UserService userService;

    public NotificationPreferenceController(NotificationPreferenceService preferenceService,
                                             UserService userService) {
        this.preferenceService = preferenceService;
        this.userService = userService;
    }

    // ===== 页面 =====

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String preferencesPage(Model model, Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        Long userId = user != null ? user.getId() : 0L;

        // 获取用户权限
        Set<String> permissions = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());

        // 获取可见的通知类型
        List<NotificationType> visibleTypes = preferenceService.getVisibleTypes(permissions);

        // 按分类分组
        Map<String, List<NotificationType>> groupedTypes = visibleTypes.stream()
            .collect(Collectors.groupingBy(NotificationType::getCategory,
                LinkedHashMap::new, Collectors.toList()));

        // 获取用户当前偏好
        List<NotificationPreference> userPrefs = preferenceService.getUserPreferences(userId);
        Map<String, Map<String, Boolean>> prefMap = new HashMap<>();
        for (NotificationPreference pref : userPrefs) {
            prefMap.computeIfAbsent(pref.getNotificationType(), k -> new HashMap<>())
                .put(pref.getChannel().name(), pref.isEnabled());
        }

        model.addAttribute("username", authentication.getName());
        model.addAttribute("groupedTypes", groupedTypes);
        model.addAttribute("channels", preferenceService.getAvailableChannels());
        model.addAttribute("userPreferences", prefMap);
        model.addAttribute("channelLabels", Map.of(
            "IN_APP", "站内信",
            "EMAIL", "邮件",
            "DINGTALK", "钉钉",
            "FEISHU", "飞书",
            "WEBHOOK", "Webhook"
        ));
        model.addAttribute("channelIcons", Map.of(
            "IN_APP", "bi-bell",
            "EMAIL", "bi-envelope",
            "DINGTALK", "bi-chat-dots",
            "FEISHU", "bi-messenger",
            "WEBHOOK", "bi-globe"
        ));

        return "notification-preferences";
    }

    /**
     * 获取用户通知偏好（JSON API）
     */
    @GetMapping("/api/list")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getPreferencesApi(Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            Long userId = user != null ? user.getId() : 0L;

            Set<String> permissions = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

            List<NotificationType> visibleTypes = preferenceService.getVisibleTypes(permissions);
            List<NotificationPreference> userPrefs = preferenceService.getUserPreferences(userId);

            Map<String, Object> result = new HashMap<>();
            result.put("preferences", userPrefs);
            result.put("visibleTypes", visibleTypes);
            result.put("emailConfigured", false);
            result.put("feishuConfigured", false);
            result.put("dingtalkConfigured", false);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ===== API =====

    /**
     * 更新单个偏好
     */
    @PostMapping("/api/update")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updatePreference(@RequestBody Map<String, Object> body,
                                               Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
            }

            String notificationType = (String) body.get("notificationType");
            Channel channel = Channel.valueOf((String) body.get("channel"));
            boolean enabled = (Boolean) body.get("enabled");

            preferenceService.updatePreference(user.getId(), notificationType, channel, enabled);

            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 批量更新偏好
     */
    @PostMapping("/api/batch-update")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> batchUpdate(@RequestBody Map<String, Object> body,
                                          Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> preferences = (List<Map<String, Object>>) body.get("preferences");
            preferenceService.batchUpdate(user.getId(), preferences);

            return ResponseEntity.ok(Map.of("success", true, "message", "通知偏好已保存"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 重置为默认值
     */
    @PostMapping("/api/reset")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> resetToDefaults(Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
            }

            preferenceService.resetToDefaults(user.getId());
            return ResponseEntity.ok(Map.of("success", true, "message", "已重置为默认设置"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
