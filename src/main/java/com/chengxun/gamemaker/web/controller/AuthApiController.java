package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.dto.LoginRequest;
import com.chengxun.gamemaker.web.dto.ChangePasswordRequest;
import com.chengxun.gamemaker.web.entity.DeviceTrust;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.CaptchaService;
import com.chengxun.gamemaker.web.service.DeviceTrustService;
import com.chengxun.gamemaker.web.service.EmailService;
import com.chengxun.gamemaker.web.service.UserService;
import com.chengxun.gamemaker.web.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证API控制器
 * 为Vue前端提供REST API认证接口
 *
 * 主要功能：
 * - 用户登录（返回JSON）
 * - 用户登出
 * - 获取当前用户信息
 * - 修改密码
 *
 * @author chengxun
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "认证API", description = "用户认证相关接口")
public class AuthApiController {

    private static final Logger log = LoggerFactory.getLogger(AuthApiController.class);

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final DeviceTrustService deviceTrustService;
    private final CaptchaService captchaService;
    private final EmailService emailService;

    public AuthApiController(AuthenticationManager authenticationManager, UserService userService,
                            JwtUtils jwtUtils, DeviceTrustService deviceTrustService,
                            CaptchaService captchaService, EmailService emailService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtUtils = jwtUtils;
        this.deviceTrustService = deviceTrustService;
        this.captchaService = captchaService;
        this.emailService = emailService;
    }

    /**
     * 用户登录
     *
     * @param request 登录请求（用户名、密码）
     * @param httpRequest HTTP请求
     * @return 登录结果
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录认证")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request,
                                                      HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 认证
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            // 设置认证信息到SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 获取用户信息
            User user = userService.getUserByUsername(request.getUsername());

            // 设备信任检查
            boolean deviceTrusted = false;
            boolean needDeviceVerify = false;
            String deviceName = null;

            if (deviceTrustService.isDeviceTrustEnabled()) {
                DeviceTrustService.DeviceCheckResult checkResult = deviceTrustService.checkDevice(user.getId(), httpRequest);
                deviceTrusted = checkResult.isTrusted();
                needDeviceVerify = checkResult.isNeedVerify();
                deviceName = checkResult.getDeviceName();

                if (deviceTrusted) {
                    log.info("设备已信任: user={}, device={}", user.getUsername(), deviceName);
                } else if (needDeviceVerify) {
                    log.info("陌生设备需要验证: user={}, device={}", user.getUsername(), deviceName);
                }
            }

            // 设备验证未通过时，不返回 JWT token，防止绕过验证
            if (needDeviceVerify) {
                response.put("success", true);
                response.put("needDeviceVerify", true);
                response.put("deviceName", deviceName);
                // 返回临时验证 token（仅用于设备验证流程，不是登录 token）
                String verifyToken = jwtUtils.generateVerifyToken(user.getUsername(), user.getId());
                response.put("verifyToken", verifyToken);
                log.info("用户需要设备验证: {}, 不返回登录 token", request.getUsername());
                return ResponseEntity.ok(response);
            }

            // 设备已信任或功能未启用，正常登录
            String role = user.getRole() != null ? user.getRole().getName() : "USER";
            String jwtToken = jwtUtils.generateToken(user.getUsername(), user.getId(), role);

            response.put("success", true);
            response.put("message", "登录成功");
            response.put("token", jwtToken);
            response.put("user", buildUserInfo(user));
            response.put("deviceTrusted", deviceTrusted);

            log.info("用户登录成功: {}", request.getUsername());
            return ResponseEntity.ok(response);

        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            log.error("用户登录失败(密码错误): {}", request.getUsername());
            response.put("success", false);
            response.put("message", "用户名或密码错误");
            return ResponseEntity.status(401).body(response);
        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
            log.error("用户登录失败(用户不存在或未审核): {} - {}", request.getUsername(), e.getMessage());
            response.put("success", false);
            if (e.getMessage() != null && e.getMessage().contains("not approved")) {
                response.put("message", "账号正在审核中，请等待管理员审核");
            } else {
                response.put("message", "用户名或密码错误");
            }
            return ResponseEntity.status(401).body(response);
        } catch (Exception e) {
            log.error("用户登录失败: {} - {}", request.getUsername(), e.getMessage());
            response.put("success", false);
            response.put("message", "登录失败，请稍后重试");
            return ResponseEntity.status(401).body(response);
        }
    }

    /**
     * 验证设备（二次验证）
     * 陌生设备登录时需要验证，验证通过后才返回登录 token
     *
     * @param request 包含验证token和验证码的请求
     * @param httpRequest HTTP请求
     * @return 验证结果（成功时包含登录 token）
     */
    @PostMapping("/verify-device")
    @Operation(summary = "验证设备", description = "陌生设备二次验证，验证通过后返回登录 token")
    public ResponseEntity<Map<String, Object>> verifyDevice(@RequestBody Map<String, String> request,
                                                             HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();

        try {
            String verifyToken = request.get("verifyToken");
            String verifyCode = request.get("verifyCode");
            boolean trustDevice = Boolean.parseBoolean(request.getOrDefault("trustDevice", "false"));

            if (verifyToken == null || verifyCode == null) {
                response.put("success", false);
                response.put("message", "参数不完整");
                return ResponseEntity.badRequest().body(response);
            }

            // 从验证token获取用户信息（必须是验证专用token，不能用登录token）
            String username = jwtUtils.getUsernameFromVerifyToken(verifyToken);
            if (username == null) {
                response.put("success", false);
                response.put("message", "验证 token 无效或已过期，请重新登录");
                return ResponseEntity.status(401).body(response);
            }

            User user = userService.getUserByUsername(username);
            if (user == null) {
                response.put("success", false);
                response.put("message", "用户不存在");
                return ResponseEntity.status(401).body(response);
            }

            // 验证验证码（使用邮箱验证码）
            if (!emailService.verifyCode(user.getEmail(), verifyCode)) {
                response.put("success", false);
                response.put("message", "验证码错误或已过期");
                return ResponseEntity.badRequest().body(response);
            }

            // 验证通过，如果用户选择信任设备
            if (trustDevice) {
                deviceTrustService.trustDevice(user.getId(), httpRequest);
                log.info("设备已信任: user={}, device={}", username, deviceTrustService.parseDeviceName(httpRequest));
            }

            // 验证通过，生成真正的登录 token
            String role = user.getRole() != null ? user.getRole().getName() : "USER";
            String jwtToken = jwtUtils.generateToken(user.getUsername(), user.getId(), role);

            response.put("success", true);
            response.put("message", "验证成功");
            response.put("token", jwtToken);
            response.put("user", buildUserInfo(user));
            response.put("deviceTrusted", trustDevice);

            log.info("设备验证通过，用户登录成功: {}", username);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("设备验证失败: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "验证失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 发送设备验证邮件
     *
     * @param request 包含验证token的请求
     * @return 发送结果
     */
    @PostMapping("/send-verify-code")
    @Operation(summary = "发送设备验证码", description = "发送设备验证邮件")
    public ResponseEntity<Map<String, Object>> sendVerifyCode(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            String verifyToken = request.get("verifyToken");

            if (verifyToken == null) {
                response.put("success", false);
                response.put("message", "参数不完整");
                return ResponseEntity.badRequest().body(response);
            }

            // 从验证token获取用户信息（必须是验证专用token）
            String username = jwtUtils.getUsernameFromVerifyToken(verifyToken);
            if (username == null) {
                response.put("success", false);
                response.put("message", "验证 token 无效或已过期，请重新登录");
                return ResponseEntity.status(401).body(response);
            }
            User user = userService.getUserByUsername(username);

            if (user == null) {
                response.put("success", false);
                response.put("message", "用户不存在");
                return ResponseEntity.status(401).body(response);
            }

            if (user.getEmail() == null || user.getEmail().isEmpty()) {
                response.put("success", false);
                response.put("message", "用户未配置邮箱，无法发送验证码");
                return ResponseEntity.badRequest().body(response);
            }

            // 生成验证码并存储
            String verifyCode = emailService.generateVerificationCode(user.getEmail());

            // 发送验证码邮件
            emailService.sendVerificationEmail(user.getEmail(), verifyCode);

            response.put("success", true);
            response.put("message", "验证码已发送到您的邮箱");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("发送验证码失败: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "发送验证码失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 信任当前设备
     *
     * @param request 包含token的请求
     * @param httpRequest HTTP请求
     * @return 操作结果
     */
    @PostMapping("/trust-device")
    @Operation(summary = "信任设备", description = "信任当前设备")
    public ResponseEntity<Map<String, Object>> trustDevice(@RequestBody Map<String, String> request,
                                                            HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();

        try {
            String token = request.get("token");

            if (token == null) {
                response.put("success", false);
                response.put("message", "参数不完整");
                return ResponseEntity.badRequest().body(response);
            }

            // 从token获取用户信息
            String username = jwtUtils.getUsernameFromToken(token);
            User user = userService.getUserByUsername(username);

            if (user == null) {
                response.put("success", false);
                response.put("message", "用户不存在");
                return ResponseEntity.status(401).body(response);
            }

            // 信任设备
            DeviceTrust device = deviceTrustService.trustDevice(user.getId(), httpRequest);

            response.put("success", true);
            response.put("message", "设备已信任");
            response.put("deviceName", device.getDeviceName());
            response.put("expiresAt", device.getExpiresAt());

            log.info("设备已信任: user={}, device={}", username, device.getDeviceName());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("信任设备失败: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "操作失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 用户登出
     *
     * @param request HTTP请求
     * @return 登出结果
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "用户登出")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 清除 SecurityContext
            SecurityContextHolder.clearContext();

            response.put("success", true);
            response.put("message", "登出成功");

            log.info("用户登出成功");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("用户登出失败: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "登出失败");
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取验证码
     * 用于前端注册页面获取验证码图片
     *
     * @return 验证码ID和图片
     */
    @GetMapping("/captcha")
    @Operation(summary = "获取验证码", description = "获取注册验证码")
    public ResponseEntity<Map<String, Object>> getCaptcha() {
        Map<String, Object> response = new HashMap<>();

        try {
            String captchaId = captchaService.generateCaptcha();
            String captchaImage = captchaService.generateCaptchaImageBase64(captchaId);

            response.put("captchaId", captchaId);
            response.put("captchaImage", captchaImage);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("生成验证码失败: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "生成验证码失败");
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取注册配置
     * 用于前端注册页面判断是否需要邮箱验证
     *
     * @return 注册配置信息
     */
    @GetMapping("/register-config")
    @Operation(summary = "获取注册配置", description = "获取注册页面配置")
    public ResponseEntity<Map<String, Object>> getRegisterConfig() {
        Map<String, Object> response = new HashMap<>();

        response.put("emailEnabled", emailService.isEmailEnabled());

        return ResponseEntity.ok(response);
    }

    /**
     * 用户注册
     * 处理前端Vue注册请求
     *
     * @param request 注册请求
     * @return 注册结果
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "用户注册账号")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            String username = request.get("username");
            String password = request.get("password");
            String confirmPassword = request.get("confirmPassword");
            String email = request.get("email");
            String nickname = request.get("nickname");
            String captchaId = request.get("captchaId");
            String captchaCode = request.get("captchaCode");

            // 验证验证码
            if (captchaId == null || captchaCode == null ||
                !captchaService.verifyCaptcha(captchaId, captchaCode)) {
                response.put("success", false);
                response.put("message", "验证码错误或已过期");
                return ResponseEntity.badRequest().body(response);
            }

            // 验证密码确认
            if (!password.equals(confirmPassword)) {
                response.put("success", false);
                response.put("message", "两次输入的密码不一致");
                return ResponseEntity.badRequest().body(response);
            }

            // 邮箱验证（如果启用了邮件服务且提供了邮箱）
            if (emailService.isEmailEnabled() && email != null && !email.isEmpty()) {
                String emailCode = request.get("emailCode");
                if (emailCode == null || emailCode.isEmpty()) {
                    response.put("success", false);
                    response.put("message", "请输入邮箱验证码");
                    return ResponseEntity.badRequest().body(response);
                }
                if (!emailService.verifyCode(email, emailCode)) {
                    response.put("success", false);
                    response.put("message", "邮箱验证码错误或已过期");
                    return ResponseEntity.badRequest().body(response);
                }
            }

            // 注册用户
            userService.register(username, password, email, nickname);

            response.put("success", true);
            response.put("message", "注册成功，请等待管理员审核");

            log.info("新用户注册: {}", username);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("用户注册失败: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取当前用户信息
     *
     * @param authentication 认证信息
     * @return 用户信息
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前用户", description = "获取当前登录用户信息")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication == null || !authentication.isAuthenticated()) {
            response.put("success", false);
            response.put("message", "未登录");
            return ResponseEntity.status(401).body(response);
        }

        try {
            User user = userService.getUserByUsername(authentication.getName());
            if (user == null) {
                response.put("success", false);
                response.put("message", "用户不存在");
                return ResponseEntity.status(404).body(response);
            }

            response.put("success", true);
            response.put("user", buildUserInfo(user));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取用户信息失败: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "获取用户信息失败");
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 修改密码
     *
     * @param request 修改密码请求
     * @param authentication 认证信息
     * @return 修改结果
     */
    @PostMapping("/change-password")
    @Operation(summary = "修改密码", description = "修改当前用户密码")
    public ResponseEntity<Map<String, Object>> changePassword(@RequestBody ChangePasswordRequest request,
                                                               Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication == null || !authentication.isAuthenticated()) {
            response.put("success", false);
            response.put("message", "未登录");
            return ResponseEntity.status(401).body(response);
        }

        try {
            User user = userService.getUserByUsername(authentication.getName());
            if (user == null) {
                response.put("success", false);
                response.put("message", "用户不存在");
                return ResponseEntity.status(404).body(response);
            }

            // 修改密码（服务层会验证旧密码）
            userService.changePassword(user.getId(), request.getCurrentPassword(), request.getNewPassword());

            response.put("success", true);
            response.put("message", "密码修改成功");

            log.info("用户密码修改成功: {}", authentication.getName());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("修改密码失败: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "修改密码失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 构建用户信息Map
     *
     * @param user 用户实体
     * @return 用户信息Map
     */
    private Map<String, Object> buildUserInfo(User user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("email", user.getEmail());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("status", user.getStatus());
        userInfo.put("isAdmin", user.isAdmin());

        if (user.getRole() != null) {
            Map<String, Object> roleInfo = new HashMap<>();
            roleInfo.put("id", user.getRole().getId());
            roleInfo.put("name", user.getRole().getName());
            roleInfo.put("description", user.getRole().getDescription());
            roleInfo.put("permissions", user.getRole().getPermissions());
            userInfo.put("role", roleInfo);
        }

        return userInfo;
    }
}
