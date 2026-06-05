package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.DeviceTrustService;
import com.chengxun.gamemaker.web.service.EmailService;
import com.chengxun.gamemaker.web.service.OperationLogService;
import com.chengxun.gamemaker.web.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/device")
public class DeviceVerifyController {

    private static final Logger log = LoggerFactory.getLogger(DeviceVerifyController.class);
    private static final String SESSION_PENDING_USER = "pending_device_verify_user";

    private final DeviceTrustService deviceTrustService;
    private final EmailService emailService;
    private final UserService userService;
    private final OperationLogService logService;

    public DeviceVerifyController(DeviceTrustService deviceTrustService, EmailService emailService,
                                  UserService userService, OperationLogService logService) {
        this.deviceTrustService = deviceTrustService;
        this.emailService = emailService;
        this.userService = userService;
        this.logService = logService;
    }

    @GetMapping("/verify")
    public String verifyPage(Model model, HttpServletRequest request, HttpSession session) {
        String username = (String) session.getAttribute(SESSION_PENDING_USER);
        if (username == null) {
            return "redirect:/login";
        }

        User user = userService.getUserByUsername(username);
        if (user == null) {
            session.removeAttribute(SESSION_PENDING_USER);
            return "redirect:/login";
        }

        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            model.addAttribute("error", "您的账号未设置邮箱，无法进行设备验证。请联系管理员。");
            return "device-verify";
        }

        model.addAttribute("email", maskEmail(user.getEmail()));
        model.addAttribute("emailEnabled", emailService.isEmailEnabled());
        model.addAttribute("trustDays", deviceTrustService.getTrustDays());

        // 自动生成发送验证码
        if (emailService.isEmailEnabled()) {
            String code = emailService.generateVerificationCode(user.getEmail());
            emailService.sendVerificationEmail(user.getEmail(), code);
            model.addAttribute("codeSent", true);
        }

        return "device-verify";
    }

    @PostMapping("/verify")
    public String verify(@RequestParam String verifyCode,
                         HttpSession session,
                         HttpServletRequest request,
                         RedirectAttributes redirectAttributes) {
        String username = (String) session.getAttribute(SESSION_PENDING_USER);
        if (username == null) {
            return "redirect:/login";
        }

        User user = userService.getUserByUsername(username);
        if (user == null) {
            session.removeAttribute(SESSION_PENDING_USER);
            return "redirect:/login";
        }

        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "账号未设置邮箱");
            return "redirect:/device/verify";
        }

        if (!emailService.verifyCode(user.getEmail(), verifyCode)) {
            redirectAttributes.addFlashAttribute("error", "验证码错误或已过期");
            return "redirect:/device/verify";
        }

        // 信任设备
        deviceTrustService.trustDevice(user.getId(), request);

        // 记录日志
        String deviceName = deviceTrustService.parseDeviceName(request);
        logService.log(user.getId(), "TRUST_DEVICE", deviceName, "信任设备: " + deviceName, deviceTrustService.getClientIp(request));

        // 将用户标记为已验证，允许登录
        session.setAttribute("device_verified_user", username);
        session.removeAttribute(SESSION_PENDING_USER);

        log.info("Device verified for user: {}, device: {}", username, deviceName);
        return "redirect:/login?device_verified=true";
    }

    @PostMapping("/send-code")
    @ResponseBody
    public Map<String, Object> sendCode(HttpSession session) {
        Map<String, Object> result = new HashMap<>();

        String username = (String) session.getAttribute(SESSION_PENDING_USER);
        if (username == null) {
            result.put("success", false);
            result.put("message", "会话已过期，请重新登录");
            return result;
        }

        User user = userService.getUserByUsername(username);
        if (user == null || user.getEmail() == null || user.getEmail().isEmpty()) {
            result.put("success", false);
            result.put("message", "无法发送验证码");
            return result;
        }

        if (!emailService.isEmailEnabled()) {
            result.put("success", false);
            result.put("message", "邮件服务未启用");
            return result;
        }

        try {
            String code = emailService.generateVerificationCode(user.getEmail());
            emailService.sendVerificationEmail(user.getEmail(), code);
            result.put("success", true);
            result.put("message", "验证码已发送");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "发送失败: " + e.getMessage());
        }

        return result;
    }

    public static void setPendingUser(HttpSession session, String username) {
        session.setAttribute(SESSION_PENDING_USER, username);
    }

    public static String getPendingUser(HttpSession session) {
        return (String) session.getAttribute(SESSION_PENDING_USER);
    }

    public static void clearPendingUser(HttpSession session) {
        session.removeAttribute(SESSION_PENDING_USER);
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) {
            return "***" + email.substring(atIndex);
        }
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }
}
