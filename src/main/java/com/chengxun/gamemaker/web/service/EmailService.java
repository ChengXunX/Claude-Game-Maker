package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.config.AppConfig;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 邮件服务
 * 负责发送各种类型的邮件，支持 HTML 格式和发件人名字
 *
 * 主要功能：
 * - 发送验证码邮件
 * - 发送通用邮件（支持 HTML）
 * - 发送模板渲染后的邮件
 * - 支持发件人名字和代理邮箱
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final int EXPIRE_MINUTES = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JavaMailSender mailSender;
    private final AppConfig appConfig;
    private final NotificationTemplateService templateService;

    // email -> {code, expireTime}
    private final ConcurrentHashMap<String, VerificationEntry> verificationStore = new ConcurrentHashMap<>();

    public EmailService(JavaMailSender mailSender, AppConfig appConfig, NotificationTemplateService templateService) {
        this.mailSender = mailSender;
        this.appConfig = appConfig;
        this.templateService = templateService;
    }

    public String generateVerificationCode(String email) {
        String code = String.format("%06d", RANDOM.nextInt(1000000));
        long expireAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(EXPIRE_MINUTES);
        verificationStore.put(email.toLowerCase(), new VerificationEntry(code, expireAt));
        return code;
    }

    public void sendVerificationEmail(String toEmail, String code) {
        if (!isEmailEnabled() || getEmailFrom() == null || getEmailFrom().isEmpty()) {
            log.warn("Email not configured, verification code for {}: {}", toEmail, code);
            return;
        }

        try {
            // 使用模板服务渲染内容
            Map<String, String> variables = new HashMap<>();
            variables.put("code", code);
            variables.put("expireMinutes", String.valueOf(EXPIRE_MINUTES));

            String subject;
            String content;

            try {
                Map<String, String> rendered = templateService.renderTemplate("VERIFICATION_EMAIL", variables);
                subject = rendered.get("subject");
                content = rendered.get("content");
            } catch (Exception e) {
                // 模板不存在时使用默认内容
                log.warn("验证码模板不存在，使用默认内容: {}", e.getMessage());
                subject = "ChengXun Game Maker - 邮箱验证码";
                content = String.format(
                    "<div style='font-family: Arial, sans-serif; padding: 20px;'>" +
                    "<h2>邮箱验证码</h2>" +
                    "<p>您的邮箱验证码是：</p>" +
                    "<div style='background: #f5f5f5; padding: 15px; text-align: center; font-size: 24px; font-weight: bold; letter-spacing: 5px; margin: 20px 0;'>%s</div>" +
                    "<p>验证码 <b>%d</b> 分钟内有效。</p>" +
                    "<p style='color: #999; font-size: 12px;'>如果这不是您的操作，请忽略此邮件。</p>" +
                    "</div>",
                    code, EXPIRE_MINUTES);
            }

            // 使用 MimeMessage 发送 HTML 邮件
            sendHtmlEmail(toEmail, subject, content);
            log.info("Verification email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", toEmail, e);
            // 邮件发送失败时不抛出异常，允许降级
        }
    }

    public boolean verifyCode(String email, String inputCode) {
        if (email == null || inputCode == null) return false;

        VerificationEntry entry = verificationStore.get(email.toLowerCase());
        if (entry == null) return false;

        if (entry.expireAt < System.currentTimeMillis()) {
            verificationStore.remove(email.toLowerCase());
            return false;
        }

        if (entry.code.equals(inputCode.trim())) {
            verificationStore.remove(email.toLowerCase());
            return true;
        }

        return false;
    }

    /**
     * 发送通用邮件（支持 HTML）
     * 用于告警通知、系统通知等场景
     *
     * @param toEmail 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容（支持 HTML）
     * @throws RuntimeException 当邮件发送失败时抛出
     */
    public void sendGeneralEmail(String toEmail, String subject, String content) {
        if (!isEmailEnabled()) {
            throw new RuntimeException("邮件服务未启用");
        }

        String fromEmail = getEmailFrom();
        if (fromEmail == null || fromEmail.isEmpty()) {
            throw new RuntimeException("发件人邮箱未配置");
        }

        try {
            // 检测内容是否包含 HTML 标签
            boolean isHtml = content != null && (content.contains("<div") || content.contains("<h") ||
                content.contains("<p") || content.contains("<table") || content.contains("<span"));

            if (isHtml) {
                // 发送 HTML 邮件
                sendHtmlEmail(toEmail, subject, content);
            } else {
                // 发送纯文本邮件
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(getFromAddress());
                message.setTo(toEmail);
                message.setSubject(subject);
                message.setText(content);
                mailSender.send(message);
            }
            log.info("General email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send general email to: {}", toEmail, e);
            throw new RuntimeException("邮件发送失败: " + e.getMessage());
        }
    }

    /**
     * 发送 HTML 邮件
     *
     * @param toEmail 收件人邮箱
     * @param subject 邮件主题
     * @param htmlContent HTML 内容
     * @throws MessagingException 当邮件发送失败时抛出
     */
    private void sendHtmlEmail(String toEmail, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

        helper.setFrom(getFromAddress());
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(htmlContent, true); // true 表示 HTML 内容

        mailSender.send(message);
    }

    /**
     * 获取发件人地址（支持发件人名字）
     * 格式: "发件人名字 <邮箱地址>" 或 "邮箱地址"
     *
     * @return 发件人地址
     */
    private String getFromAddress() {
        String email = getEmailFrom();
        String name = getSenderName();

        if (name != null && !name.isEmpty()) {
            return String.format("%s <%s>", name, email);
        }
        return email;
    }

    /**
     * 获取发件人名字
     *
     * @return 发件人名字，未配置返回 null
     */
    public String getSenderName() {
        return appConfig.getEmail().getSenderName();
    }

    /**
     * 获取代理邮箱（用于发送）
     *
     * @return 代理邮箱，未配置返回 null
     */
    public String getProxyEmail() {
        return appConfig.getEmail().getProxyEmail();
    }

    public boolean isEmailEnabled() {
        return appConfig.getEmail().isEnabled();
    }

    public String getSmtpHost() {
        return appConfig.getEmail().getSmtpHost();
    }

    public int getSmtpPort() {
        return appConfig.getEmail().getSmtpPort();
    }

    public String getSmtpUsername() {
        return appConfig.getEmail().getSmtpUsername();
    }

    public String getEmailFrom() {
        String from = appConfig.getEmail().getEmailFrom();
        // 如果 from 不是有效的邮箱地址，则使用 smtpUsername
        if (from == null || from.isEmpty() || !from.contains("@")) {
            return appConfig.getEmail().getSmtpUsername();
        }
        return from;
    }

    private static class VerificationEntry {
        final String code;
        final long expireAt;

        VerificationEntry(String code, long expireAt) {
            this.code = code;
            this.expireAt = expireAt;
        }
    }
}
