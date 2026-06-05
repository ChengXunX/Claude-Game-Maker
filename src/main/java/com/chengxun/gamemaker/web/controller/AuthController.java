package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.dto.RegisterRequest;
import com.chengxun.gamemaker.web.entity.SystemConfig;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.repository.SystemConfigRepository;
import com.chengxun.gamemaker.web.service.CaptchaService;
import com.chengxun.gamemaker.web.service.EmailService;
import com.chengxun.gamemaker.web.service.OperationLogService;
import com.chengxun.gamemaker.web.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * 处理用户登录、注册、登出等认证相关请求
 *
 * 主要功能：
 * - 用户登录页面和登录处理
 * - 用户注册页面和注册处理
 * - 密码修改
 * - 访问拒绝页面
 *
 * @author chengxun
 * @since 1.0.0
 */
@Controller
public class AuthController {

    private final UserService userService;
    private final CaptchaService captchaService;
    private final EmailService emailService;
    private final OperationLogService logService;
    private final SystemConfigRepository configRepository;

    public AuthController(UserService userService, CaptchaService captchaService,
                          EmailService emailService, OperationLogService logService,
                          SystemConfigRepository configRepository) {
        this.userService = userService;
        this.captchaService = captchaService;
        this.emailService = emailService;
        this.logService = logService;
        this.configRepository = configRepository;
    }

    /**
     * 登录页面
     *
     * @param model 模型对象
     * @param device_verified 设备验证标记
     * @return 登录页面视图名
     */
    @GetMapping("/login")
    public String loginPage(Model model, @RequestParam(required = false) String device_verified) {
        String captchaId = captchaService.generateCaptcha();
        String captchaImage = captchaService.generateCaptchaImageBase64(captchaId);
        model.addAttribute("captchaId", captchaId);
        model.addAttribute("captchaImage", captchaImage);

        if ("true".equals(device_verified)) {
            model.addAttribute("success", "设备验证成功，请输入密码完成登录");
        }

        return "login";
    }

    /**
     * 注册页面
     *
     * @param model 模型对象
     * @return 注册页面视图名
     */
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        String captchaId = captchaService.generateCaptcha();
        String captchaImage = captchaService.generateCaptchaImageBase64(captchaId);
        model.addAttribute("captchaId", captchaId);
        model.addAttribute("captchaImage", captchaImage);
        model.addAttribute("emailEnabled", emailService.isEmailEnabled());
        return "register";
    }

    /**
     * 处理用户注册
     *
     * @param request 注册请求，使用 @Valid 进行参数验证
     * @param bindingResult 验证结果
     * @param captchaId 验证码ID
     * @param captchaCode 验证码
     * @param emailCode 邮箱验证码（可选）
     * @param redirectAttributes 重定向属性
     * @return 重定向到登录页面或返回注册页面
     */
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegisterRequest request,
                           BindingResult bindingResult,
                           @RequestParam String captchaId,
                           @RequestParam String captchaCode,
                           @RequestParam(required = false) String emailCode,
                           RedirectAttributes redirectAttributes) {
        // 检查验证错误
        if (bindingResult.hasErrors()) {
            String error = bindingResult.getFieldErrors().stream()
                .map(e -> e.getDefaultMessage())
                .findFirst()
                .orElse("参数验证失败");
            redirectAttributes.addFlashAttribute("error", error);
            return "redirect:/register";
        }

        // 验证验证码
        if (!captchaService.verifyCaptcha(captchaId, captchaCode)) {
            redirectAttributes.addFlashAttribute("error", "验证码错误或已过期");
            return "redirect:/register";
        }

        // 验证密码确认
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            redirectAttributes.addFlashAttribute("error", "两次输入的密码不一致");
            return "redirect:/register";
        }

        // 邮箱验证（如果启用了邮件服务且提供了邮箱）
        if (emailService.isEmailEnabled() && request.getEmail() != null && !request.getEmail().isBlank()) {
            if (emailCode == null || emailCode.isBlank()) {
                redirectAttributes.addFlashAttribute("error", "请输入邮箱验证码");
                return "redirect:/register";
            }
            if (!emailService.verifyCode(request.getEmail(), emailCode)) {
                redirectAttributes.addFlashAttribute("error", "邮箱验证码错误或已过期");
                return "redirect:/register";
            }
        }

        try {
            userService.register(request.getUsername(), request.getPassword(), request.getEmail(), request.getNickname());

            // T01: 记录注册事件，管理员可在操作日志中看到
            logService.log(null, "USER_REGISTER", request.getUsername(),
                "新用户注册，等待审核: " + request.getUsername(), null);

            redirectAttributes.addFlashAttribute("success", "注册成功，请等待管理员审核");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        }
    }

    /**
     * 访问拒绝页面
     *
     * @return 403错误页面视图名
     */
    @GetMapping("/403")
    public String accessDenied() {
        return "403";
    }

    /**
     * 检查用户是否存在并返回邮箱（脱敏）
     */
    @PostMapping("/api/auth/check-user")
    @ResponseBody
    public ResponseEntity<?> checkUser(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        if (username == null || username.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "用户名不能为空"));
        }

        User user = userService.getUserByUsername(username);
        if (user == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "用户不存在"));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            // 脱敏邮箱: test@example.com -> t***@example.com
            String email = user.getEmail();
            int atIndex = email.indexOf('@');
            if (atIndex > 1) {
                result.put("email", email.charAt(0) + "***" + email.substring(atIndex));
            } else {
                result.put("email", "***" + email);
            }
            result.put("hasEmail", true);
        } else {
            result.put("hasEmail", false);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 发送重置密码邮件
     */
    @PostMapping("/api/auth/forgot-password")
    @ResponseBody
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String email = request.get("email");

        if (username == null || username.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "用户名不能为空"));
        }

        User user = userService.getUserByUsername(username);
        if (user == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "用户不存在"));
        }

        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            return ResponseEntity.ok(Map.of("success", false, "message", "该用户未配置邮箱"));
        }

        // 生成6位重置码（有效期30分钟）
        String resetCode = String.format("%06d", new java.util.Random().nextInt(1000000));

        // 发送重置邮件
        try {
            String subject = "ChengXun Game Maker - 密码重置";
            String content = String.format(
                "您好 %s，\n\n您请求了密码重置，您的重置码是：%s\n\n重置码有效期30分钟。如非本人操作，请忽略此邮件。",
                user.getNickname() != null ? user.getNickname() : username, resetCode
            );

            emailService.sendGeneralEmail(user.getEmail(), subject, content);

            logService.log(user.getId(), "PASSWORD_RESET_REQUEST", username,
                "用户请求重置密码，邮件已发送至: " + user.getEmail(), null);

            return ResponseEntity.ok(Map.of("success", true, "message", "重置码已发送到您的邮箱"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", "邮件发送失败: " + e.getMessage()));
        }
    }

    /**
     * 获取管理员联系信息
     * 优先返回自定义联系链接，其次返回管理员邮箱
     */
    @GetMapping("/api/auth/admin-contact")
    @ResponseBody
    public ResponseEntity<?> getAdminContact() {
        Map<String, Object> result = new HashMap<>();

        // 优先检查自定义联系链接
        String contactLink = configRepository.findByConfigKey("system.contact.link")
            .map(SystemConfig::getConfigValue)
            .orElse(null);
        if (contactLink != null && !contactLink.isEmpty()) {
            result.put("success", true);
            result.put("link", contactLink);
            result.put("type", "link");
            return ResponseEntity.ok(result);
        }

        // 其次返回管理员邮箱
        User admin = userService.getFirstAdmin();
        if (admin != null && admin.getEmail() != null && !admin.getEmail().isEmpty()) {
            result.put("success", true);
            result.put("email", admin.getEmail());
            result.put("username", admin.getUsername());
            result.put("type", "email");
        } else {
            result.put("success", false);
            result.put("message", "管理员未配置联系方式");
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 获取验证码
     * 用于前端注册页面获取验证码图片
     *
     * @return 验证码ID和图片
     */
    @GetMapping("/api/auth/captcha")
    @ResponseBody
    public ResponseEntity<?> getCaptcha() {
        Map<String, Object> result = new HashMap<>();

        try {
            String captchaId = captchaService.generateCaptcha();
            String captchaImage = captchaService.generateCaptchaImageBase64(captchaId);

            result.put("captchaId", captchaId);
            result.put("captchaImage", captchaImage);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "生成验证码失败");
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 获取注册配置
     * 用于前端注册页面判断是否需要邮箱验证
     *
     * @return 注册配置信息
     */
    @GetMapping("/api/auth/register-config")
    @ResponseBody
    public ResponseEntity<?> getRegisterConfig() {
        Map<String, Object> result = new HashMap<>();

        result.put("emailEnabled", emailService.isEmailEnabled());

        return ResponseEntity.ok(result);
    }

    /**
     * 用户注册API
     * 处理前端Vue注册请求
     *
     * @param request 注册请求数据
     * @return 注册结果
     */
    @PostMapping("/api/auth/register")
    @ResponseBody
    public ResponseEntity<?> registerApi(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();

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
                result.put("success", false);
                result.put("message", "验证码错误或已过期");
                return ResponseEntity.badRequest().body(result);
            }

            // 验证密码确认
            if (!password.equals(confirmPassword)) {
                result.put("success", false);
                result.put("message", "两次输入的密码不一致");
                return ResponseEntity.badRequest().body(result);
            }

            // 邮箱验证（如果启用了邮件服务且提供了邮箱）
            if (emailService.isEmailEnabled() && email != null && !email.isEmpty()) {
                String emailCode = request.get("emailCode");
                if (emailCode == null || emailCode.isEmpty()) {
                    result.put("success", false);
                    result.put("message", "请输入邮箱验证码");
                    return ResponseEntity.badRequest().body(result);
                }
                if (!emailService.verifyCode(email, emailCode)) {
                    result.put("success", false);
                    result.put("message", "邮箱验证码错误或已过期");
                    return ResponseEntity.badRequest().body(result);
                }
            }

            // 注册用户
            userService.register(username, password, email, nickname);

            // 记录注册事件
            logService.log(null, "USER_REGISTER", username,
                "新用户注册，等待审核: " + username, null);

            result.put("success", true);
            result.put("message", "注册成功，请等待管理员审核");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
}
