package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.config.AppConfig;
import com.chengxun.gamemaker.config.MailConfig;
import com.chengxun.gamemaker.web.entity.SystemConfig;
import com.chengxun.gamemaker.web.repository.SystemConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统配置服务
 * 管理可动态配置的系统参数，支持缓存和热更新
 */
@Service
@Transactional
public class SystemConfigService {

    private static final Logger log = LoggerFactory.getLogger(SystemConfigService.class);

    /** 配置缓存，避免频繁数据库查询 */
    private final ConcurrentHashMap<String, SystemConfig> configCache = new ConcurrentHashMap<>();

    private final SystemConfigRepository configRepository;
    private final AppConfig appConfig;
    private final MailConfig mailConfig;
    private final JavaMailSender javaMailSender;

    public SystemConfigService(SystemConfigRepository configRepository, AppConfig appConfig,
                               MailConfig mailConfig, JavaMailSender javaMailSender) {
        this.configRepository = configRepository;
        this.appConfig = appConfig;
        this.mailConfig = mailConfig;
        this.javaMailSender = javaMailSender;
    }

    /**
     * 应用启动时初始化默认配置
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initDefaultConfigs() {
        // 安全配置
        initConfig("security.password.min-length", "8", "密码最小长度", SystemConfig.GROUP_SECURITY, "number", true);
        initConfig("security.password.require-uppercase", "true", "密码是否需要大写字母", SystemConfig.GROUP_SECURITY, "boolean", true);
        initConfig("security.password.require-lowercase", "true", "密码是否需要小写字母", SystemConfig.GROUP_SECURITY, "boolean", true);
        initConfig("security.password.require-digit", "true", "密码是否需要数字", SystemConfig.GROUP_SECURITY, "boolean", true);
        initConfig("security.password.require-special", "true", "密码是否需要特殊字符", SystemConfig.GROUP_SECURITY, "boolean", true);
        initConfig("security.session.timeout-minutes", "30", "会话超时时间（分钟）", SystemConfig.GROUP_SECURITY, "number", true);
        initConfig("security.session.max-concurrent", "1", "最大并发会话数", SystemConfig.GROUP_SECURITY, "number", true);
        initConfig("security.login.max-attempts", "5", "最大登录尝试次数", SystemConfig.GROUP_SECURITY, "number", true);
        initConfig("security.login.lockout-minutes", "15", "登录锁定时间（分钟）", SystemConfig.GROUP_SECURITY, "number", true);

        // 用户配置
        initConfig("user.default.role", "USER", "新注册用户默认角色（USER/READONLY/ADMIN等）", SystemConfig.GROUP_SYSTEM, "string", true);

        // Agent配置
        initConfig("agent.task.max-retry", "3", "任务最大重试次数", SystemConfig.GROUP_AGENT, "number", true);
        initConfig("agent.task.retry-delay-ms", "5000", "任务重试延迟（毫秒）", SystemConfig.GROUP_AGENT, "number", true);
        initConfig("agent.task.max-queue-size", "100", "任务队列最大长度", SystemConfig.GROUP_AGENT, "number", true);
        initConfig("agent.message.max-size", "10000", "消息最大长度", SystemConfig.GROUP_AGENT, "number", true);

        // 邮件配置
        initConfig("email.verification.expire-minutes", "10", "验证码过期时间（分钟）", SystemConfig.GROUP_EMAIL, "number", true);
        initConfig("email.verification.code-length", "6", "验证码长度", SystemConfig.GROUP_EMAIL, "number", true);

        // 通知配置
        initConfig("notification.enabled.channels", "feishu,email", "启用的通知渠道（逗号分隔）", SystemConfig.GROUP_NOTIFICATION, "string", true);

        // 系统配置
        initConfig("system.pagination.default-size", "20", "默认分页大小", SystemConfig.GROUP_SYSTEM, "number", true);
        initConfig("system.pagination.max-size", "100", "最大分页大小", SystemConfig.GROUP_SYSTEM, "number", true);

        // 安全配置 - 设备信任
        initConfig("security.device.trust.enabled", "false", "是否启用设备信任（陌生设备二次验证）", SystemConfig.GROUP_SECURITY, "boolean", true);
        initConfig("security.device.trust.days", "7", "设备信任有效期（天）", SystemConfig.GROUP_SECURITY, "number", true);

        // 工作流审批配置
        initConfig("workflow.approval.importance-threshold", "MEDIUM", "审批重要程度阈值（LOW/MEDIUM/HIGH/CRITICAL），低于此级别的步骤免审批", SystemConfig.GROUP_SYSTEM, "string", true);

        // 加载所有配置到缓存
        loadAllToCache();

        // 将数据库中保存的配置同步到内存 AppConfig，确保重启后配置不丢失
        syncConfigsToAppConfig();

        log.info("System configs initialized, total: {}", configCache.size());
    }

    /**
     * 初始化配置（如果不存在）
     */
    private void initConfig(String key, String value, String description, String group, String valueType, boolean builtin) {
        if (!configRepository.existsByConfigKey(key)) {
            SystemConfig config = new SystemConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setDescription(description);
            config.setGroup(group);
            config.setValueType(valueType);
            config.setSystemBuiltin(builtin);
            configRepository.save(config);
        }
    }

    /**
     * 加载所有配置到缓存
     */
    private void loadAllToCache() {
        configCache.clear();
        configRepository.findAll().forEach(config -> configCache.put(config.getConfigKey(), config));
    }

    /**
     * 将数据库中保存的配置同步到内存中的 AppConfig
     * 确保应用重启后，通过 UI 保存的配置（如 API Key、安全设置）仍然生效
     */
    private void syncConfigsToAppConfig() {
        // 同步 Claude 配置
        String apiKey = getString("claude.api.key", null);
        if (apiKey != null && !apiKey.isEmpty()) {
            appConfig.getClaude().setApiKey(apiKey);
            log.info("启动同步: claude.api.key 已加载");
        }

        String apiUrl = getString("claude.api.url", null);
        if (apiUrl != null && !apiUrl.isEmpty()) {
            appConfig.getClaude().setApiUrl(apiUrl);
            log.info("启动同步: claude.api.url = {}", apiUrl);
        }

        String model = getString("claude.model", null);
        if (model != null && !model.isEmpty()) {
            appConfig.getClaude().setModel(model);
            log.info("启动同步: claude.model = {}", model);
        }

        String maxTokens = getString("claude.max.tokens", null);
        if (maxTokens != null && !maxTokens.isEmpty()) {
            try {
                appConfig.getClaude().setMaxTokens(Integer.parseInt(maxTokens));
                log.info("启动同步: claude.max.tokens = {}", maxTokens);
            } catch (NumberFormatException e) {
                log.warn("Invalid max tokens value in DB: {}", maxTokens);
            }
        }

        // 同步安全配置（支持点分隔和连字符两种 key 格式）
        String deviceTrustEnabled = getString("security.device.trust.enabled", null);
        if (deviceTrustEnabled == null) {
            deviceTrustEnabled = getString("security.device-trust-enabled", null);
        }
        if (deviceTrustEnabled != null) {
            appConfig.getSecurity().setDeviceTrustEnabled(Boolean.parseBoolean(deviceTrustEnabled));
            log.info("启动同步: deviceTrustEnabled = {}", deviceTrustEnabled);
        }

        String deviceTrustDays = getString("security.device.trust.days", null);
        if (deviceTrustDays == null) {
            deviceTrustDays = getString("security.device-trust-days", null);
        }
        if (deviceTrustDays != null) {
            try {
                appConfig.getSecurity().setDeviceTrustDays(Integer.parseInt(deviceTrustDays));
                log.info("启动同步: deviceTrustDays = {}", deviceTrustDays);
            } catch (NumberFormatException e) {
                log.warn("Invalid device trust days value in DB: {}", deviceTrustDays);
            }
        }

        // 同步邮件配置
        String emailEnabled = getString("email.enabled", null);
        if (emailEnabled != null) {
            appConfig.getEmail().setEnabled(Boolean.parseBoolean(emailEnabled));
            log.info("启动同步: email.enabled = {}", emailEnabled);
        }

        String smtpHost = getString("email.smtp.host", null);
        if (smtpHost != null && !smtpHost.isEmpty()) {
            appConfig.getEmail().setSmtpHost(smtpHost);
            log.info("启动同步: email.smtp.host = {}", smtpHost);
        }

        String smtpPort = getString("email.smtp.port", null);
        if (smtpPort != null && !smtpPort.isEmpty()) {
            try {
                appConfig.getEmail().setSmtpPort(Integer.parseInt(smtpPort));
                log.info("启动同步: email.smtp.port = {}", smtpPort);
            } catch (NumberFormatException e) {
                log.warn("Invalid email smtp port value in DB: {}", smtpPort);
            }
        }

        String smtpUsername = getString("email.smtp.username", null);
        if (smtpUsername != null && !smtpUsername.isEmpty()) {
            appConfig.getEmail().setSmtpUsername(smtpUsername);
            log.info("启动同步: email.smtp.username 已加载");
        }

        String smtpPassword = getString("email.smtp.password", null);
        if (smtpPassword != null && !smtpPassword.isEmpty()) {
            appConfig.getEmail().setSmtpPassword(smtpPassword);
            log.info("启动同步: email.smtp.password 已加载");
        }

        String emailFrom = getString("email.from", null);
        if (emailFrom != null && !emailFrom.isEmpty()) {
            appConfig.getEmail().setEmailFrom(emailFrom);
            log.info("启动同步: email.from = {}", emailFrom);
        }

        String senderName = getString("email.sender.name", null);
        if (senderName != null && !senderName.isEmpty()) {
            appConfig.getEmail().setSenderName(senderName);
            log.info("启动同步: email.sender.name = {}", senderName);
        }

        String proxyEmail = getString("email.proxy.email", null);
        if (proxyEmail != null && !proxyEmail.isEmpty()) {
            appConfig.getEmail().setProxyEmail(proxyEmail);
            log.info("启动同步: email.proxy.email = {}", proxyEmail);
        }

        // 刷新 JavaMailSender，使数据库配置生效
        mailConfig.refreshMailSender(javaMailSender);
        log.info("JavaMailSender 已刷新，使用数据库配置");

        // 同步飞书配置
        String feishuEnabled = getString("feishu.enabled", null);
        if (feishuEnabled != null) {
            appConfig.getFeishu().setEnabled(Boolean.parseBoolean(feishuEnabled));
            log.info("启动同步: feishu.enabled = {}", feishuEnabled);
        }
        String feishuAppId = getString("feishu.app.id", null);
        if (feishuAppId != null && !feishuAppId.isEmpty()) {
            appConfig.getFeishu().setAppId(feishuAppId);
            log.info("启动同步: feishu.app.id 已加载");
        }
        String feishuAppSecret = getString("feishu.app.secret", null);
        if (feishuAppSecret != null && !feishuAppSecret.isEmpty()) {
            appConfig.getFeishu().setAppSecret(feishuAppSecret);
        }
        String feishuWebhookUrl = getString("feishu.webhook.url", null);
        if (feishuWebhookUrl != null && !feishuWebhookUrl.isEmpty()) {
            appConfig.getFeishu().setWebhookUrl(feishuWebhookUrl);
            log.info("启动同步: feishu.webhook.url 已加载");
        }
        String feishuChatId = getString("feishu.chat.id", null);
        if (feishuChatId != null && !feishuChatId.isEmpty()) {
            appConfig.getFeishu().setChatId(feishuChatId);
        }
        String feishuEncryptKey = getString("feishu.encrypt.key", null);
        if (feishuEncryptKey != null && !feishuEncryptKey.isEmpty()) {
            appConfig.getFeishu().setEncryptKey(feishuEncryptKey);
            log.info("启动同步: feishu.encrypt.key 已加载");
        }
        String feishuVerifyToken = getString("feishu.verify.token", null);
        if (feishuVerifyToken != null && !feishuVerifyToken.isEmpty()) {
            appConfig.getFeishu().setVerifyToken(feishuVerifyToken);
            log.info("启动同步: feishu.verify.token 已加载");
        }
        String feishuCallbackToken = getString("feishu.callback.token", null);
        if (feishuCallbackToken != null && !feishuCallbackToken.isEmpty()) {
            appConfig.getFeishu().setCallbackToken(feishuCallbackToken);
            log.info("启动同步: feishu.callback.token 已加载");
        }
        String feishuCallbackExpire = getString("feishu.callback.expire.minutes", null);
        if (feishuCallbackExpire != null && !feishuCallbackExpire.isEmpty()) {
            try {
                appConfig.getFeishu().setCallbackExpireMinutes(Integer.parseInt(feishuCallbackExpire));
            } catch (NumberFormatException e) {
                log.warn("飞书回调过期时间配置格式错误: {}", feishuCallbackExpire);
            }
        }
        String feishuApprovalNotifyUserIds = getString("feishu.approval.notify.user.ids", null);
        if (feishuApprovalNotifyUserIds != null && !feishuApprovalNotifyUserIds.isEmpty()) {
            appConfig.getFeishu().setApprovalNotifyUserIds(feishuApprovalNotifyUserIds);
            log.info("启动同步: feishu.approval.notify.user.ids 已加载");
        }

        // 同步钉钉配置
        String dingtalkEnabled = getString("dingtalk.enabled", null);
        if (dingtalkEnabled != null) {
            appConfig.getDingtalk().setEnabled(Boolean.parseBoolean(dingtalkEnabled));
            log.info("启动同步: dingtalk.enabled = {}", dingtalkEnabled);
        }
        String dingtalkWebhookUrl = getString("dingtalk.webhook.url", null);
        if (dingtalkWebhookUrl != null && !dingtalkWebhookUrl.isEmpty()) {
            appConfig.getDingtalk().setWebhookUrl(dingtalkWebhookUrl);
            log.info("启动同步: dingtalk.webhook.url 已加载");
        }
        String dingtalkSecret = getString("dingtalk.secret", null);
        if (dingtalkSecret != null && !dingtalkSecret.isEmpty()) {
            appConfig.getDingtalk().setSecret(dingtalkSecret);
        }
    }

    /**
     * 获取配置值（字符串）— 全局配置
     */
    public String getString(String key, String defaultValue) {
        SystemConfig config = configCache.get(key);
        if (config != null && config.getConfigValue() != null) {
            return config.getConfigValue();
        }
        return defaultValue;
    }

    /**
     * 获取配置值（字符串）— 支持项目级覆盖
     * 先查项目配置，没有则回退到全局配置
     *
     * @param key 配置键
     * @param projectId 项目 ID（null 则查全局）
     * @param defaultValue 默认值
     * @return 配置值
     */
    public String getString(String key, String projectId, String defaultValue) {
        if (projectId != null) {
            // 先查项目级配置
            Optional<SystemConfig> projectConfig = configRepository.findByConfigKeyAndProjectId(key, projectId);
            if (projectConfig.isPresent() && projectConfig.get().getConfigValue() != null) {
                return projectConfig.get().getConfigValue();
            }
        }
        // 回退到全局配置
        return getString(key, defaultValue);
    }

    /**
     * 获取配置值（整数）— 全局配置
     */
    public int getInt(String key, int defaultValue) {
        SystemConfig config = configCache.get(key);
        if (config != null) {
            return config.getIntValue(defaultValue);
        }
        return defaultValue;
    }

    /**
     * 获取配置值（整数）— 支持项目级覆盖
     */
    public int getInt(String key, String projectId, int defaultValue) {
        String value = getString(key, projectId, null);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * 获取配置值（长整数）— 全局配置
     */
    public long getLong(String key, long defaultValue) {
        SystemConfig config = configCache.get(key);
        if (config != null) {
            return config.getLongValue(defaultValue);
        }
        return defaultValue;
    }

    /**
     * 获取配置值（布尔）— 全局配置
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        SystemConfig config = configCache.get(key);
        if (config != null) {
            return config.getBooleanValue(defaultValue);
        }
        return defaultValue;
    }

    /**
     * 获取配置值（布尔）— 支持项目级覆盖
     */
    public boolean getBoolean(String key, String projectId, boolean defaultValue) {
        String value = getString(key, projectId, null);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }

    /**
     * 设置全局配置值（不存在则自动创建）
     */
    public void setConfig(String key, String value) {
        SystemConfig config = configRepository.findByConfigKeyAndProjectIdIsNull(key).orElse(null);
        if (config != null) {
            config.setConfigValue(value);
            configRepository.save(config);
            configCache.put(key, config);
            log.info("Config updated: {} = {}", key, value);
        } else {
            // 配置不存在时自动创建
            config = new SystemConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setGroup(extractGroup(key));
            config.setValueType("string");
            configRepository.save(config);
            configCache.put(key, config);
            log.info("Config created: {} = {}", key, value);
        }
    }

    /**
     * 从配置键中提取分组名
     * 例如 "claude.api.key" -> "claude"
     */
    private String extractGroup(String key) {
        if (key == null || !key.contains(".")) return "custom";
        return key.substring(0, key.indexOf('.'));
    }

    /**
     * 设置项目级配置值（覆盖全局配置）
     *
     * @param key 配置键
     * @param projectId 项目 ID
     * @param value 配置值
     */
    public void setProjectConfig(String key, String projectId, String value) {
        SystemConfig config = configRepository.findByConfigKeyAndProjectId(key, projectId).orElse(null);
        if (config != null) {
            config.setConfigValue(value);
        } else {
            // 创建项目级配置
            SystemConfig globalConfig = configRepository.findByConfigKeyAndProjectIdIsNull(key).orElse(null);
            config = new SystemConfig();
            config.setConfigKey(key);
            config.setProjectId(projectId);
            config.setConfigValue(value);
            config.setGroup(globalConfig != null ? globalConfig.getGroup() : "custom");
            config.setValueType(globalConfig != null ? globalConfig.getValueType() : "string");
            config.setDescription(globalConfig != null ? "项目级覆盖: " + globalConfig.getDescription() : "项目级配置");
        }
        configRepository.save(config);
        log.info("Project config updated: {} = {} (project: {})", key, value, projectId);
    }

    /**
     * 获取项目的所有配置（包括全局和项目级）
     */
    public List<SystemConfig> getProjectConfigs(String projectId) {
        List<SystemConfig> globalConfigs = configRepository.findByGroupAndProjectIdIsNull("all");
        List<SystemConfig> projectConfigs = configRepository.findByProjectId(projectId);
        // 合并，项目级覆盖全局
        // 简化实现：返回所有配置
        return configRepository.findAll();
    }

    /**
     * 获取所有配置
     */
    public List<SystemConfig> getAllConfigs() {
        return configRepository.findAll();
    }

    /**
     * 根据分组获取全局配置
     */
    public List<SystemConfig> getConfigsByGroup(String group) {
        return configRepository.findByGroupAndProjectIdIsNull(group);
    }

    /**
     * 根据分组获取项目配置（全局 + 项目级覆盖）
     */
    public List<SystemConfig> getConfigsByGroup(String group, String projectId) {
        if (projectId == null) {
            return getConfigsByGroup(group);
        }
        return configRepository.findByGroupAndProjectIdOrGroupAndProjectIdIsNull(group, projectId, group);
    }

    /**
     * 获取所有分组
     */
    public List<String> getAllGroups() {
        return configRepository.findAll().stream()
            .map(SystemConfig::getGroup)
            .distinct()
            .sorted()
            .toList();
    }

    /**
     * 创建新配置
     */
    public SystemConfig createConfig(SystemConfig config) {
        SystemConfig saved = configRepository.save(config);
        configCache.put(saved.getConfigKey(), saved);
        return saved;
    }

    /**
     * 删除配置（仅非系统内置）
     */
    public void deleteConfig(Long id) {
        SystemConfig config = configRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("配置不存在"));

        if (config.isSystemBuiltin()) {
            throw new RuntimeException("系统内置配置不可删除");
        }

        configRepository.delete(config);
        configCache.remove(config.getConfigKey());
        log.info("Config deleted: {}", config.getConfigKey());
    }

    /**
     * 刷新缓存
     */
    public void refreshCache() {
        loadAllToCache();
        log.info("Config cache refreshed");
    }
}
