package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.config.AppConfig;
import com.chengxun.gamemaker.web.dto.UserDTO;
import com.chengxun.gamemaker.web.entity.Role;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.OperationLogService;
import com.chengxun.gamemaker.web.service.RoleService;
import com.chengxun.gamemaker.web.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.chengxun.gamemaker.web.service.EmailService;
import com.chengxun.gamemaker.web.service.SettingsService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 管理后台 REST API 控制器
 * 提供用户管理、角色管理等 REST 接口
 *
 * @author chengxun
 * @since 2.0.0
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('PERM_admin:manage')")
public class AdminApiController {

    private static final Logger log = LoggerFactory.getLogger(AdminApiController.class);

    private final UserService userService;
    private final RoleService roleService;
    private final OperationLogService logService;
    private final EmailService emailService;
    private final SettingsService settingsService;
    private final AppConfig appConfig;

    public AdminApiController(UserService userService, RoleService roleService,
                              OperationLogService logService, EmailService emailService,
                              SettingsService settingsService, AppConfig appConfig) {
        this.userService = userService;
        this.roleService = roleService;
        this.logService = logService;
        this.emailService = emailService;
        this.settingsService = settingsService;
        this.appConfig = appConfig;
    }

    // ===== 用户管理 =====

    /**
     * 获取所有用户列表
     * 返回UserDTO，隐藏密码等敏感信息
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> listUsers() {
        List<UserDTO> users = userService.getAllUsers().stream()
            .map(UserDTO::fromEntity)
            .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    /**
     * 获取待审批用户列表
     */
    @GetMapping("/users/pending")
    @Operation(summary = "获取待审批用户")
    @PreAuthorize("hasAuthority('PERM_users:manage')")
    public ResponseEntity<List<UserDTO>> listPendingUsers() {
        List<UserDTO> users = userService.getPendingUsers().stream()
            .map(UserDTO::fromEntity)
            .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    /**
     * 获取单个用户
     * 返回UserDTO，隐藏密码等敏感信息
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(UserDTO.fromEntity(user));
    }

    /**
     * 创建用户
     */
    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody Map<String, Object> request,
                                         Authentication authentication) {
        try {
            String username = (String) request.get("username");
            String password = (String) request.get("password");
            String email = (String) request.get("email");
            String nickname = (String) request.get("nickname");
            Long roleId = request.get("roleId") != null ? ((Number) request.get("roleId")).longValue() : null;

            User user = userService.createUser(username, password, email, nickname, roleId);
            logService.log(getUserId(authentication), "CREATE_USER", username, "Created user", null);
            return ResponseEntity.ok(Map.of("success", true, "message", "用户创建成功", "user", user));
        } catch (Exception e) {
            log.error("Failed to create user", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 更新用户
     */
    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id,
                                         @RequestBody Map<String, Object> request,
                                         Authentication authentication) {
        try {
            User user = userService.updateUser(id, request);
            logService.log(getUserId(authentication), "UPDATE_USER", user.getUsername(), "Updated user", null);
            return ResponseEntity.ok(Map.of("success", true, "message", "用户更新成功", "user", user));
        } catch (Exception e) {
            log.error("Failed to update user", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id,
                                         Authentication authentication) {
        try {
            User user = userService.getUserById(id);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            userService.deleteUser(id);
            logService.log(getUserId(authentication), "DELETE_USER", user.getUsername(), "Deleted user", null);
            return ResponseEntity.ok(Map.of("success", true, "message", "用户已删除"));
        } catch (Exception e) {
            log.error("Failed to delete user", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 批准用户
     */
    @PostMapping("/users/{id}/approve")
    public ResponseEntity<?> approveUser(@PathVariable Long id,
                                          Authentication authentication) {
        try {
            User user = userService.approveUser(id);
            logService.log(getUserId(authentication), "APPROVE_USER", user.getUsername(), "Approved user", null);
            return ResponseEntity.ok(Map.of("success", true, "message", "用户已批准"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 拒绝用户
     */
    @PostMapping("/users/{id}/reject")
    public ResponseEntity<?> rejectUser(@PathVariable Long id,
                                         Authentication authentication) {
        try {
            User user = userService.rejectUser(id);
            logService.log(getUserId(authentication), "REJECT_USER", user.getUsername(), "Rejected user", null);
            return ResponseEntity.ok(Map.of("success", true, "message", "用户已拒绝"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 禁用用户
     */
    @PostMapping("/users/{id}/disable")
    public ResponseEntity<?> disableUser(@PathVariable Long id,
                                          Authentication authentication) {
        try {
            User user = userService.disableUser(id);
            logService.log(getUserId(authentication), "DISABLE_USER", user.getUsername(), "Disabled user", null);
            return ResponseEntity.ok(Map.of("success", true, "message", "用户已禁用"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 更新用户角色
     * 禁止修改超级管理员（ID=1）的角色
     */
    @PostMapping("/users/{userId}/role")
    public ResponseEntity<?> updateUserRole(@PathVariable Long userId,
                                             @RequestBody Map<String, Long> request,
                                             Authentication authentication) {
        try {
            // 禁止修改超级管理员角色
            if (userId == 1L) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "超级管理员角色不允许修改"));
            }

            Long roleId = request.get("roleId");
            if (roleId == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "角色ID不能为空"));
            }
            User user = userService.updateUserRole(userId, roleId);
            logService.log(getUserId(authentication), "UPDATE_ROLE", user.getUsername(),
                "Role updated to " + user.getRole().getDisplayName(), null);
            return ResponseEntity.ok(Map.of("success", true, "message", "角色已更新"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ===== 角色管理 =====

    /**
     * 获取所有角色
     */
    @GetMapping("/roles")
    public ResponseEntity<List<Role>> listRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    /**
     * 获取单个角色
     */
    @GetMapping("/roles/{id}")
    public ResponseEntity<Role> getRole(@PathVariable Long id) {
        Role role = roleService.getRoleById(id);
        if (role == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(role);
    }

    /**
     * 创建角色
     */
    @PostMapping("/roles")
    public ResponseEntity<?> createRole(@RequestBody Map<String, Object> request,
                                         Authentication authentication) {
        try {
            String name = (String) request.get("name");
            String displayName = (String) request.get("displayName");
            String description = (String) request.get("description");
            @SuppressWarnings("unchecked")
            List<String> permissionsList = (List<String>) request.get("permissions");
            Set<String> permissions = permissionsList != null ? new HashSet<>(permissionsList) : new HashSet<>();

            Role role = roleService.createRole(name, displayName, description, permissions);
            logService.log(getUserId(authentication), "CREATE_ROLE", name, "Created role", null);
            return ResponseEntity.ok(Map.of("success", true, "message", "角色创建成功", "role", role));
        } catch (Exception e) {
            log.error("Failed to create role", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 更新角色
     */
    @PutMapping("/roles/{id}")
    public ResponseEntity<?> updateRole(@PathVariable Long id,
                                         @RequestBody Map<String, Object> request,
                                         Authentication authentication) {
        try {
            String displayName = (String) request.get("displayName");
            String description = (String) request.get("description");
            @SuppressWarnings("unchecked")
            List<String> permissionsList = (List<String>) request.get("permissions");
            Set<String> permissions = permissionsList != null ? new HashSet<>(permissionsList) : new HashSet<>();

            Role role = roleService.updateRole(id, displayName, description, permissions);
            logService.log(getUserId(authentication), "UPDATE_ROLE", role.getName(), "Updated role", null);
            return ResponseEntity.ok(Map.of("success", true, "message", "角色更新成功", "role", role));
        } catch (Exception e) {
            log.error("Failed to update role", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/roles/{id}")
    public ResponseEntity<?> deleteRole(@PathVariable Long id,
                                         Authentication authentication) {
        try {
            Role role = roleService.getRoleById(id);
            if (role == null) {
                return ResponseEntity.notFound().build();
            }
            if (role.isSystem()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "系统内置角色不可删除"));
            }
            roleService.deleteRole(id);
            logService.log(getUserId(authentication), "DELETE_ROLE", role.getName(), "Deleted role", null);
            return ResponseEntity.ok(Map.of("success", true, "message", "角色已删除"));
        } catch (Exception e) {
            log.error("Failed to delete role", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }


    // ===== 邮件配置 API =====

    /**
     * 获取邮件配置
     */
    @GetMapping("/api/settings/email")
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
    public ResponseEntity<Map<String, Object>> saveEmailSettings(@RequestBody Map<String, Object> request,
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
     * 实际连接 SMTP 服务器验证配置是否正确
     */
    @PostMapping("/api/settings/email/test")
    public ResponseEntity<Map<String, Object>> testEmailConnection(@RequestBody Map<String, Object> request) {
        try {
            String host = (String) request.get("smtpHost");
            int port = Integer.parseInt(String.valueOf(request.getOrDefault("smtpPort", 587)));
            String username = (String) request.get("smtpUsername");
            String password = (String) request.get("smtpPassword");

            // 创建临时邮件发送器进行实际连接测试
            org.springframework.mail.javamail.JavaMailSenderImpl testSender =
                new org.springframework.mail.javamail.JavaMailSenderImpl();
            testSender.setHost(host);
            testSender.setPort(port);
            if (username != null && !username.isEmpty()) {
                testSender.setUsername(username);
            }
            if (password != null && !password.isEmpty()) {
                testSender.setPassword(password);
            }

            java.util.Properties props = testSender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.connectiontimeout", "5000");
            props.put("mail.smtp.timeout", "5000");

            if (port == 465) {
                props.put("mail.smtp.ssl.enable", "true");
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                props.put("mail.smtp.socketFactory.port", "465");
            } else if (port == 587) {
                props.put("mail.smtp.starttls.enable", "true");
            }

            // 实际连接测试
            testSender.testConnection();

            return ResponseEntity.ok(Map.of("success", true, "message", "邮件连接测试成功"));
        } catch (Exception e) {
            log.error("Email connection test failed", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "连接测试失败: " + e.getMessage()));
        }
    }

    /**
     * 发送测试邮件到指定邮箱
     * 支持使用当前填写的配置（可能还没保存）
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/api/settings/email/send-test")
    public ResponseEntity<Map<String, Object>> sendTestEmail(@RequestBody Map<String, Object> request) {
        try {
            String toEmail = (String) request.get("toEmail");
            if (toEmail == null || toEmail.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "请提供收件邮箱地址"
                ));
            }

            // 获取邮件配置（优先使用请求中的配置，否则使用当前保存的配置）
            String smtpHost = (String) request.get("smtpHost");
            String smtpPortStr = String.valueOf(request.getOrDefault("smtpPort", "587"));
            String smtpUsername = (String) request.get("smtpUsername");
            String smtpPassword = (String) request.get("smtpPassword");
            String emailFrom = (String) request.get("emailFrom");
            String senderName = (String) request.get("senderName");
            String replyTo = (String) request.get("replyTo");

            // 如果请求中没有配置，使用当前保存的配置
            if (smtpHost == null || smtpHost.isEmpty()) {
                smtpHost = emailService.getSmtpHost();
            }
            if (smtpUsername == null || smtpUsername.isEmpty()) {
                smtpUsername = emailService.getSmtpUsername();
            }
            if (smtpPassword == null || smtpPassword.isEmpty()) {
                smtpPassword = appConfig.getEmail().getSmtpPassword();
            }
            if (emailFrom == null || emailFrom.isEmpty()) {
                emailFrom = emailService.getEmailFrom();
            }

            int smtpPort = 587;
            try {
                smtpPort = Integer.parseInt(smtpPortStr);
            } catch (NumberFormatException e) {
                // 使用默认端口
            }

            // 创建临时邮件发送器
            org.springframework.mail.javamail.JavaMailSenderImpl testSender =
                new org.springframework.mail.javamail.JavaMailSenderImpl();
            testSender.setHost(smtpHost);
            testSender.setPort(smtpPort);
            if (smtpUsername != null && !smtpUsername.isEmpty()) {
                testSender.setUsername(smtpUsername);
            }
            if (smtpPassword != null && !smtpPassword.isEmpty()) {
                testSender.setPassword(smtpPassword);
            }

            // 配置邮件属性
            java.util.Properties props = testSender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.connectiontimeout", "10000");
            props.put("mail.smtp.timeout", "10000");

            if (smtpPort == 465) {
                props.put("mail.smtp.ssl.enable", "true");
                props.put("mail.smtp.starttls.enable", "false");
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                props.put("mail.smtp.socketFactory.port", "465");
            } else if (smtpPort == 587) {
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.ssl.enable", "false");
            }

            // 构建发件人地址（支持发件人名称）
            String fromAddress;
            if (senderName != null && !senderName.isEmpty()) {
                fromAddress = String.format("%s <%s>", senderName, emailFrom);
            } else {
                fromAddress = emailFrom;
            }

            // 发送测试邮件
            org.springframework.mail.SimpleMailMessage message = new org.springframework.mail.SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("ChengXun Game Maker - 邮件配置测试");

            String content = "这是一封测试邮件，用于验证邮件配置是否正确。\n\n" +
                "如果您收到此邮件，说明邮件服务配置成功！\n\n" +
                "配置信息：\n" +
                "- SMTP 服务器: " + smtpHost + ":" + smtpPort + "\n" +
                "- 登录账号: " + smtpUsername + "\n" +
                "- 发件人地址: " + emailFrom + "\n" +
                (senderName != null && !senderName.isEmpty() ? "- 发件人名称: " + senderName + "\n" : "") +
                (replyTo != null && !replyTo.isEmpty() ? "- 回复地址: " + replyTo + "\n" : "") +
                "\n发送时间: " + java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) +
                "\n\n--\nChengXun Game Maker";

            message.setText(content);

            // 设置回复地址
            if (replyTo != null && !replyTo.isEmpty()) {
                message.setReplyTo(replyTo);
            }

            testSender.send(message);
            log.info("Test email sent to: {} using config: {}:{}", toEmail, smtpHost, smtpPort);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "测试邮件已发送到 " + toEmail
            ));
        } catch (Exception e) {
            log.error("Send test email failed", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "发送测试邮件失败: " + e.getMessage()
            ));
        }
    }

    private Long getUserId(Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        return user != null ? user.getId() : null;
    }
}
