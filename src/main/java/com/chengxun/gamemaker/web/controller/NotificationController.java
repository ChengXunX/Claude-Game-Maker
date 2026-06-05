package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.entity.Notification;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.NotificationService;
import com.chengxun.gamemaker.web.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知控制器
 * 提供通知页面
 */
@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    /**
     * 通知列表页面
     */
    @GetMapping
    public String listNotifications(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size,
                                    Model model, Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (user == null) {
            return "redirect:/";
        }

        Page<Notification> notificationPage = notificationService.getUserNotifications(user.getId(), page, size);

        model.addAttribute("notifications", notificationPage.getContent());
        model.addAttribute("notificationPage", notificationPage);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user.getId()));
        model.addAttribute("username", authentication.getName());
        return "notifications";
    }

    /**
     * 获取未读通知数量（API）
     */
    @GetMapping("/unread-count")
    @ResponseBody
    public Map<String, Object> getUnreadCount(Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        Map<String, Object> result = new HashMap<>();
        result.put("count", user != null ? notificationService.getUnreadCount(user.getId()) : 0);
        return result;
    }

    /**
     * 获取未读通知列表（API）
     */
    @GetMapping("/unread")
    @ResponseBody
    public List<Notification> getUnreadNotifications(Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (user == null) {
            return List.of();
        }
        return notificationService.getUnreadNotifications(user.getId());
    }

    /**
     * 标记通知为已读（API）
     */
    @PostMapping("/{id}/read")
    @ResponseBody
    public Map<String, Object> markAsRead(@PathVariable Long id, Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        Map<String, Object> result = new HashMap<>();
        try {
            if (user != null) {
                notificationService.markAsRead(id, user.getId());
                result.put("success", true);
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 标记所有通知为已读（API）
     */
    @PostMapping("/read-all")
    @ResponseBody
    public Map<String, Object> markAllAsRead(Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        Map<String, Object> result = new HashMap<>();
        try {
            if (user != null) {
                int count = notificationService.markAllAsRead(user.getId());
                result.put("success", true);
                result.put("count", count);
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 删除通知（API）
     */
    @DeleteMapping("/{id}")
    @ResponseBody
    public Map<String, Object> deleteNotification(@PathVariable Long id, Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        Map<String, Object> result = new HashMap<>();
        try {
            if (user != null) {
                notificationService.deleteNotification(id, user.getId());
                result.put("success", true);
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
