package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.OperationLogService;
import com.chengxun.gamemaker.web.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final OperationLogService logService;

    public AdminController(UserService userService, OperationLogService logService) {
        this.userService = userService;
        this.logService = logService;
    }

    @GetMapping("/users")
    public String listUsers(Model model, Authentication authentication) {
        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);
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

    @GetMapping("/logs")
    public String operationLogs(Model model, Authentication authentication) {
        model.addAttribute("logs", logService.getRecentLogs());
        model.addAttribute("username", authentication.getName());
        return "admin/logs";
    }

    private Long getUserId(Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        return user != null ? user.getId() : null;
    }
}
