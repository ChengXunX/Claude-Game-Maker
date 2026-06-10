package com.chengxun.gamemaker.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * 邮件配置类
 * 创建自定义 JavaMailSender Bean，从 AppConfig 读取 SMTP 配置
 * 覆盖 Spring Boot 自动配置，确保后台设置的邮件配置生效
 */
@Configuration
public class MailConfig {

    private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

    private final AppConfig appConfig;

    public MailConfig(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    /**
     * 创建 JavaMailSender Bean
     * 从 AppConfig.EmailConfig 读取 SMTP 配置
     */
    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        configureSender(sender);
        return sender;
    }

    /**
     * 配置邮件发送器
     */
    private void configureSender(JavaMailSenderImpl sender) {
        AppConfig.EmailConfig emailConfig = appConfig.getEmail();

        String host = emailConfig.getSmtpHost();
        if (host != null && !host.isEmpty()) {
            sender.setHost(host);
        }

        sender.setPort(emailConfig.getSmtpPort());

        String username = emailConfig.getSmtpUsername();
        if (username != null && !username.isEmpty()) {
            sender.setUsername(username);
        }

        String password = emailConfig.getSmtpPassword();
        if (password != null && !password.isEmpty()) {
            sender.setPassword(password);
        }

        // 根据端口自动设置协议属性
        java.util.Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        int port = emailConfig.getSmtpPort();
        if (port == 465) {
            // SSL 端口
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.starttls.enable", "false");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.ssl.checkserveridentity", "false");
        } else if (port == 587) {
            // STARTTLS 端口
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.ssl.enable", "false");
        } else {
            // 其他端口：根据是否为SSL端口自动判断
            if (port == 465) {
                props.put("mail.smtp.ssl.enable", "true");
            }
        }

        log.info("JavaMailSender configured: host={}, port={}, user={}", host, port, username);
    }

    /**
     * 刷新邮件发送器配置
     * 当用户通过后台修改邮件设置后调用
     */
    public void refreshMailSender(JavaMailSender mailSender) {
        if (mailSender instanceof JavaMailSenderImpl impl) {
            configureSender(impl);
            log.info("JavaMailSender configuration refreshed");
        }
    }
}
