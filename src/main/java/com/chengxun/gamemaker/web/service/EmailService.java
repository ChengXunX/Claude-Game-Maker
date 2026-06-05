package com.chengxun.gamemaker.web.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final int EXPIRE_MINUTES = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${spring.mail.host:}")
    private String smtpHost;

    @Value("${spring.mail.port:587}")
    private int smtpPort;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    // email -> {code, expireTime}
    private final ConcurrentHashMap<String, VerificationEntry> verificationStore = new ConcurrentHashMap<>();

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public String generateVerificationCode(String email) {
        String code = String.format("%06d", RANDOM.nextInt(1000000));
        long expireAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(EXPIRE_MINUTES);
        verificationStore.put(email.toLowerCase(), new VerificationEntry(code, expireAt));
        return code;
    }

    public void sendVerificationEmail(String toEmail, String code) {
        if (!emailEnabled || fromAddress == null || fromAddress.isEmpty()) {
            log.warn("Email not configured, verification code for {}: {}", toEmail, code);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("ChengXun Game Maker - 邮箱验证码");
            message.setText(String.format(
                "您好，\n\n您的邮箱验证码是：%s\n\n验证码 %d 分钟内有效。\n如果这不是您的操作，请忽略此邮件。\n\nChengXun Game Maker",
                code, EXPIRE_MINUTES));
            mailSender.send(message);
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
     * 发送通用邮件
     * 用于告警通知、系统通知等场景
     *
     * @param toEmail 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    public void sendGeneralEmail(String toEmail, String subject, String content) {
        if (!emailEnabled || fromAddress == null || fromAddress.isEmpty()) {
            log.warn("Email not configured, general email not sent to: {}", toEmail);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            log.info("General email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send general email to: {}", toEmail, e);
        }
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public int getSmtpPort() {
        return smtpPort;
    }

    public String getSmtpUsername() {
        return smtpUsername;
    }

    public String getEmailFrom() {
        return fromAddress;
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
