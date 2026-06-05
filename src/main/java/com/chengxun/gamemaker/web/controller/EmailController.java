package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.service.EmailService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/email")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/send-code")
    @ResponseBody
    public Map<String, Object> sendVerificationCode(@RequestParam String email) {
        Map<String, Object> result = new HashMap<>();

        if (email == null || email.isBlank() || !email.contains("@")) {
            result.put("success", false);
            result.put("message", "请输入有效的邮箱地址");
            return result;
        }

        if (!emailService.isEmailEnabled()) {
            result.put("success", false);
            result.put("message", "邮件服务未启用，请联系管理员");
            return result;
        }

        try {
            String code = emailService.generateVerificationCode(email);
            emailService.sendVerificationEmail(email, code);
            result.put("success", true);
            result.put("message", "验证码已发送到 " + email);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "发送失败: " + e.getMessage());
        }

        return result;
    }
}
