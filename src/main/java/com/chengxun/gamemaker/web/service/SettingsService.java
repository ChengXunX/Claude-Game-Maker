package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.config.AppConfig;
import com.chengxun.gamemaker.config.MailConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

@Service
@Transactional
public class SettingsService {

    private static final Logger log = LoggerFactory.getLogger(SettingsService.class);
    private static final String SETTINGS_FILE = "data/settings.properties";

    private final AppConfig appConfig;
    private final SystemConfigService configService;
    private final JavaMailSender mailSender;
    private final MailConfig mailConfig;

    public SettingsService(AppConfig appConfig, SystemConfigService configService,
                           JavaMailSender mailSender, MailConfig mailConfig) {
        this.appConfig = appConfig;
        this.configService = configService;
        this.mailSender = mailSender;
        this.mailConfig = mailConfig;
        loadSettings();
    }

    public void loadSettings() {
        // 优先从数据库加载配置（安装向导保存的位置）
        loadFromDatabase();

        // 然后从文件加载（如果有文件配置，会覆盖数据库配置）
        Path path = Path.of(SETTINGS_FILE);
        if (!Files.exists(path)) {
            return;
        }

        try (var inputStream = Files.newInputStream(path)) {
            Properties props = new Properties();
            props.load(inputStream);

            // Claude API
            if (props.containsKey("claude.api-key")) {
                appConfig.getClaude().setApiKey(props.getProperty("claude.api-key"));
            }
            if (props.containsKey("claude.api-url")) {
                appConfig.getClaude().setApiUrl(props.getProperty("claude.api-url"));
            }
            if (props.containsKey("claude.model")) {
                appConfig.getClaude().setModel(props.getProperty("claude.model"));
            }
            if (props.containsKey("claude.max-tokens")) {
                try {
                    appConfig.getClaude().setMaxTokens(Integer.parseInt(props.getProperty("claude.max-tokens")));
                } catch (NumberFormatException e) {
                    log.warn("Invalid claude.max-tokens value: {}", props.getProperty("claude.max-tokens"));
                }
            }

            // Security
            if (props.containsKey("security.device-trust-enabled")) {
                appConfig.getSecurity().setDeviceTrustEnabled(Boolean.parseBoolean(props.getProperty("security.device-trust-enabled")));
            }
            if (props.containsKey("security.device-trust-days")) {
                try {
                    appConfig.getSecurity().setDeviceTrustDays(Integer.parseInt(props.getProperty("security.device-trust-days")));
                } catch (NumberFormatException e) {
                    log.warn("Invalid security.device-trust-days value: {}", props.getProperty("security.device-trust-days"));
                }
            }

            // Feishu
            if (props.containsKey("feishu.enabled")) {
                appConfig.getFeishu().setEnabled(Boolean.parseBoolean(props.getProperty("feishu.enabled")));
            }
            if (props.containsKey("feishu.app-id")) {
                appConfig.getFeishu().setAppId(props.getProperty("feishu.app-id"));
            }
            if (props.containsKey("feishu.app-secret")) {
                appConfig.getFeishu().setAppSecret(props.getProperty("feishu.app-secret"));
            }
            if (props.containsKey("feishu.webhook-url")) {
                appConfig.getFeishu().setWebhookUrl(props.getProperty("feishu.webhook-url"));
            }
            if (props.containsKey("feishu.chat-id")) {
                appConfig.getFeishu().setChatId(props.getProperty("feishu.chat-id"));
            }
            if (props.containsKey("feishu.verify-token")) {
                appConfig.getFeishu().setVerifyToken(props.getProperty("feishu.verify-token"));
            }
            if (props.containsKey("feishu.encrypt-key")) {
                appConfig.getFeishu().setEncryptKey(props.getProperty("feishu.encrypt-key"));
            }

            // DingTalk
            if (props.containsKey("dingtalk.enabled")) {
                appConfig.getDingtalk().setEnabled(Boolean.parseBoolean(props.getProperty("dingtalk.enabled")));
            }
            if (props.containsKey("dingtalk.webhook-url")) {
                appConfig.getDingtalk().setWebhookUrl(props.getProperty("dingtalk.webhook-url"));
            }
            if (props.containsKey("dingtalk.secret")) {
                appConfig.getDingtalk().setSecret(props.getProperty("dingtalk.secret"));
            }

            // Email
            if (props.containsKey("app.email.enabled")) {
                appConfig.getEmail().setEnabled(Boolean.parseBoolean(props.getProperty("app.email.enabled")));
            }
            if (props.containsKey("spring.mail.host")) {
                appConfig.getEmail().setSmtpHost(props.getProperty("spring.mail.host"));
            }
            if (props.containsKey("spring.mail.port")) {
                try {
                    appConfig.getEmail().setSmtpPort(Integer.parseInt(props.getProperty("spring.mail.port")));
                } catch (NumberFormatException e) {
                    log.warn("Invalid spring.mail.port value: {}", props.getProperty("spring.mail.port"));
                }
            }
            if (props.containsKey("spring.mail.username")) {
                appConfig.getEmail().setSmtpUsername(props.getProperty("spring.mail.username"));
            }
            if (props.containsKey("spring.mail.password")) {
                appConfig.getEmail().setSmtpPassword(props.getProperty("spring.mail.password"));
            }
            if (props.containsKey("spring.mail.from")) {
                appConfig.getEmail().setEmailFrom(props.getProperty("spring.mail.from"));
            }

            log.info("Settings loaded from: {}", SETTINGS_FILE);
        } catch (IOException e) {
            log.error("Failed to load settings", e);
        }

        // 配置加载完成后刷新邮件发送器
        mailConfig.refreshMailSender(mailSender);
    }

    /**
     * 从数据库加载配置
     * 安装向导和系统设置将配置保存到 system_configs 表
     */
    private void loadFromDatabase() {
        try {
            // Claude API 配置
            String apiKey = configService.getString("claude.api.key", null);
            String apiUrl = configService.getString("claude.api.url", null);
            String model = configService.getString("claude.model", null);

            if (apiKey != null && !apiKey.isEmpty()) {
                appConfig.getClaude().setApiKey(apiKey);
                log.info("Loaded Claude API key from database");
            }
            if (apiUrl != null && !apiUrl.isEmpty()) {
                appConfig.getClaude().setApiUrl(apiUrl);
                log.info("Loaded Claude API URL from database: {}", apiUrl);
            }
            if (model != null && !model.isEmpty()) {
                appConfig.getClaude().setModel(model);
                log.info("Loaded Claude model from database: {}", model);
            }

            // 邮件配置
            String emailEnabled = configService.getString("email.enabled", null);
            if (emailEnabled != null) {
                appConfig.getEmail().setEnabled(Boolean.parseBoolean(emailEnabled));
                log.info("Loaded email enabled from database: {}", emailEnabled);
            }

            String smtpHost = configService.getString("email.smtp.host", null);
            if (smtpHost != null && !smtpHost.isEmpty()) {
                appConfig.getEmail().setSmtpHost(smtpHost);
                log.info("Loaded email smtp host from database: {}", smtpHost);
            }

            String smtpPort = configService.getString("email.smtp.port", null);
            if (smtpPort != null && !smtpPort.isEmpty()) {
                try {
                    appConfig.getEmail().setSmtpPort(Integer.parseInt(smtpPort));
                } catch (NumberFormatException e) {
                    log.warn("Invalid email smtp port value in database: {}", smtpPort);
                }
            }

            String smtpUsername = configService.getString("email.smtp.username", null);
            if (smtpUsername != null && !smtpUsername.isEmpty()) {
                appConfig.getEmail().setSmtpUsername(smtpUsername);
                log.info("Loaded email smtp username from database");
            }

            String smtpPassword = configService.getString("email.smtp.password", null);
            if (smtpPassword != null && !smtpPassword.isEmpty()) {
                appConfig.getEmail().setSmtpPassword(smtpPassword);
                log.info("Loaded email smtp password from database");
            }

            String emailFrom = configService.getString("email.from", null);
            if (emailFrom != null && !emailFrom.isEmpty()) {
                appConfig.getEmail().setEmailFrom(emailFrom);
                log.info("Loaded email from address from database: {}", emailFrom);
            }

            String senderName = configService.getString("email.sender.name", null);
            if (senderName != null && !senderName.isEmpty()) {
                appConfig.getEmail().setSenderName(senderName);
                log.info("Loaded email sender name from database: {}", senderName);
            }

            String proxyEmail = configService.getString("email.proxy.email", null);
            if (proxyEmail != null && !proxyEmail.isEmpty()) {
                appConfig.getEmail().setProxyEmail(proxyEmail);
                log.info("Loaded email proxy email from database: {}", proxyEmail);
            }

            log.info("Settings loaded from database");
        } catch (Exception e) {
            log.warn("Failed to load settings from database, will use defaults: {}", e.getMessage());
        }
    }

    public void saveClaudeSettings(String apiKey, String apiUrl, String model, int maxTokens) {
        Properties props = loadProperties();

        if (apiKey != null && !apiKey.isBlank()) {
            props.setProperty("claude.api-key", apiKey);
            appConfig.getClaude().setApiKey(apiKey);
        }
        if (apiUrl != null && !apiUrl.isBlank()) {
            props.setProperty("claude.api-url", apiUrl);
            appConfig.getClaude().setApiUrl(apiUrl);
        }
        if (model != null && !model.isBlank()) {
            props.setProperty("claude.model", model);
            appConfig.getClaude().setModel(model);
        }
        props.setProperty("claude.max-tokens", String.valueOf(maxTokens));
        appConfig.getClaude().setMaxTokens(maxTokens);

        saveProperties(props);
        log.info("Claude settings saved");
    }

    public void saveSecuritySettings(boolean deviceTrustEnabled, int deviceTrustDays) {
        Properties props = loadProperties();

        props.setProperty("security.device-trust-enabled", String.valueOf(deviceTrustEnabled));
        appConfig.getSecurity().setDeviceTrustEnabled(deviceTrustEnabled);

        props.setProperty("security.device-trust-days", String.valueOf(deviceTrustDays));
        appConfig.getSecurity().setDeviceTrustDays(deviceTrustDays);

        saveProperties(props);
        log.info("Security settings saved: deviceTrustEnabled={}, deviceTrustDays={}", deviceTrustEnabled, deviceTrustDays);
    }

    public void saveFeishuSettings(boolean enabled, String appId, String appSecret,
                                   String webhookUrl, String chatId,
                                   String verifyToken, String encryptKey) {
        Properties props = loadProperties();

        props.setProperty("feishu.enabled", String.valueOf(enabled));
        appConfig.getFeishu().setEnabled(enabled);

        if (appId != null) {
            props.setProperty("feishu.app-id", appId);
            appConfig.getFeishu().setAppId(appId);
        }
        if (appSecret != null && !appSecret.isBlank()) {
            props.setProperty("feishu.app-secret", appSecret);
            appConfig.getFeishu().setAppSecret(appSecret);
        }
        if (webhookUrl != null) {
            props.setProperty("feishu.webhook-url", webhookUrl);
            appConfig.getFeishu().setWebhookUrl(webhookUrl);
        }
        if (chatId != null) {
            props.setProperty("feishu.chat-id", chatId);
            appConfig.getFeishu().setChatId(chatId);
        }
        if (verifyToken != null) {
            props.setProperty("feishu.verify-token", verifyToken);
            appConfig.getFeishu().setVerifyToken(verifyToken);
        }
        if (encryptKey != null) {
            props.setProperty("feishu.encrypt-key", encryptKey);
            appConfig.getFeishu().setEncryptKey(encryptKey);
        }

        saveProperties(props);
        log.info("Feishu settings saved");
    }

    /**
     * 保存邮件配置
     * 使用 SystemConfigService 保存到数据库，确保重启后配置不丢失
     */
    public void saveEmailSettings(boolean enabled, String smtpHost, int smtpPort,
                                   String smtpUsername, String smtpPassword, String emailFrom,
                                   String senderName, String proxyEmail) {
        // 保存到数据库（SystemConfigService）
        configService.setConfig("email.enabled", String.valueOf(enabled));
        appConfig.getEmail().setEnabled(enabled);

        if (smtpHost != null && !smtpHost.isBlank()) {
            configService.setConfig("email.smtp.host", smtpHost);
            appConfig.getEmail().setSmtpHost(smtpHost);
        }
        configService.setConfig("email.smtp.port", String.valueOf(smtpPort));
        appConfig.getEmail().setSmtpPort(smtpPort);

        if (smtpUsername != null && !smtpUsername.isBlank()) {
            configService.setConfig("email.smtp.username", smtpUsername);
            appConfig.getEmail().setSmtpUsername(smtpUsername);
        }
        if (smtpPassword != null && !smtpPassword.isBlank()) {
            configService.setConfig("email.smtp.password", smtpPassword);
            appConfig.getEmail().setSmtpPassword(smtpPassword);
        }
        if (emailFrom != null && !emailFrom.isBlank()) {
            configService.setConfig("email.from", emailFrom);
            appConfig.getEmail().setEmailFrom(emailFrom);
        }

        // 保存发件人名字
        if (senderName != null) {
            configService.setConfig("email.sender.name", senderName);
            appConfig.getEmail().setSenderName(senderName);
        }

        // 保存代理邮箱
        if (proxyEmail != null) {
            configService.setConfig("email.proxy.email", proxyEmail);
            appConfig.getEmail().setProxyEmail(proxyEmail);
        }

        // 刷新 JavaMailSender 配置，使新设置立即生效
        mailConfig.refreshMailSender(mailSender);

        log.info("Email settings saved to database: enabled={}, host={}, senderName={}", enabled, smtpHost, senderName);
    }

    public void saveDingTalkSettings(String webhookUrl, String secret, boolean enabled) {
        Properties props = loadProperties();

        props.setProperty("dingtalk.enabled", String.valueOf(enabled));
        appConfig.getDingtalk().setEnabled(enabled);

        if (webhookUrl != null) {
            props.setProperty("dingtalk.webhook-url", webhookUrl);
            appConfig.getDingtalk().setWebhookUrl(webhookUrl);
        }
        if (secret != null) {
            props.setProperty("dingtalk.secret", secret);
            appConfig.getDingtalk().setSecret(secret);
        }

        saveProperties(props);
        log.info("DingTalk settings saved");
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        Path path = Path.of(SETTINGS_FILE);
        if (Files.exists(path)) {
            try (var inputStream = Files.newInputStream(path)) {
                props.load(inputStream);
            } catch (IOException e) {
                log.error("Failed to load properties", e);
            }
        }
        return props;
    }

    private void saveProperties(Properties props) {
        try {
            Path dir = Path.of("data");
            Files.createDirectories(dir);
            try (var outputStream = Files.newOutputStream(Path.of(SETTINGS_FILE))) {
                props.store(outputStream, "ChengXun Game Maker Settings");
            }
        } catch (IOException e) {
            log.error("Failed to save settings", e);
        }
    }
}
