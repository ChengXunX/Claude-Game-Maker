package com.chengxun.gamemaker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "game-maker")
public class AppConfig {
    private String dataDir = "data";
    private String projectsDir = "data/projects";

    /** @deprecated 上下文现在跟随项目存储，此字段仅保留兼容性 */
    @Deprecated
    private String contextsDir = "data/contexts";

    /** @deprecated 记忆现在跟随项目存储，此字段仅保留兼容性 */
    @Deprecated
    private String memoryDir = "data/memory";

    private String agentsDir = "src/main/resources/agents";
    private String templatesDir = "data/templates";
    private String defaultWorkDir = "data/projects/default";

    private ClaudeConfig claude = new ClaudeConfig();
    private FeishuConfig feishu = new FeishuConfig();
    private DingTalkConfig dingtalk = new DingTalkConfig();
    private SchedulerConfig scheduler = new SchedulerConfig();
    private SkillsConfig skills = new SkillsConfig();
    private ContextConfig context = new ContextConfig();
    private SecurityConfig security = new SecurityConfig();
    private EmailConfig email = new EmailConfig();

    // Getters and Setters
    public String getDataDir() { return dataDir; }
    public void setDataDir(String dataDir) { this.dataDir = dataDir; }

    public String getProjectsDir() { return projectsDir; }
    public void setProjectsDir(String projectsDir) { this.projectsDir = projectsDir; }

    /** @deprecated 上下文现在跟随项目存储 */
    @Deprecated
    public String getContextsDir() { return contextsDir; }
    @Deprecated
    public void setContextsDir(String contextsDir) { this.contextsDir = contextsDir; }

    /** @deprecated 记忆现在跟随项目存储 */
    @Deprecated
    public String getMemoryDir() { return memoryDir; }
    @Deprecated
    public void setMemoryDir(String memoryDir) { this.memoryDir = memoryDir; }

    public String getAgentsDir() { return agentsDir; }
    public void setAgentsDir(String agentsDir) { this.agentsDir = agentsDir; }

    public String getTemplatesDir() { return templatesDir; }
    public void setTemplatesDir(String templatesDir) { this.templatesDir = templatesDir; }

    public String getDefaultWorkDir() { return defaultWorkDir; }
    public void setDefaultWorkDir(String defaultWorkDir) { this.defaultWorkDir = defaultWorkDir; }
    
    public ClaudeConfig getClaude() { return claude; }
    public void setClaude(ClaudeConfig claude) { this.claude = claude; }
    
    public FeishuConfig getFeishu() { return feishu; }
    public void setFeishu(FeishuConfig feishu) { this.feishu = feishu; }

    public DingTalkConfig getDingtalk() { return dingtalk; }
    public void setDingtalk(DingTalkConfig dingtalk) { this.dingtalk = dingtalk; }

    public SchedulerConfig getScheduler() { return scheduler; }
    public void setScheduler(SchedulerConfig scheduler) { this.scheduler = scheduler; }

    public SkillsConfig getSkills() { return skills; }
    public void setSkills(SkillsConfig skills) { this.skills = skills; }

    public ContextConfig getContext() { return context; }
    public void setContext(ContextConfig context) { this.context = context; }

    public SecurityConfig getSecurity() { return security; }
    public void setSecurity(SecurityConfig security) { this.security = security; }

    public EmailConfig getEmail() { return email; }
    public void setEmail(EmailConfig email) { this.email = email; }
    
    // Convenience methods
    public String getApiKey() { return claude.getApiKey(); }
    public String getApiUrl() { return claude.getApiUrl(); }
    public String getModel() { return claude.getModel(); }
    public String getInstallPath() { return claude.getInstallPath(); }
    
    public static class ClaudeConfig {
        private String apiKey;
        private String apiUrl = "https://api.anthropic.com";
        private String model = "claude-sonnet-4-20250514";
        private String installPath = "/usr/bin/claude";
        private int maxTokens = 4096;
        
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        
        public String getApiUrl() { return apiUrl; }
        public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
        
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        
        public String getInstallPath() { return installPath; }
        public void setInstallPath(String installPath) { this.installPath = installPath; }
        
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    }
    
    public static class FeishuConfig {
        private String appId;
        private String appSecret;
        private String webhookUrl;
        private String chatId;       // 默认接收消息的群 ID
        private String verifyToken;  // 事件订阅验证 token
        private String encryptKey;   // 事件订阅加密 key
        private boolean enabled = false;

        /** 审批卡片回调签名密钥（可选，不配置则跳过签名验证） */
        private String callbackToken;
        /** 审批卡片回调过期时间（分钟），默认30分钟 */
        private int callbackExpireMinutes = 30;
        /** 审批卡片 @指定人 的飞书用户ID列表，逗号分隔 */
        private String approvalNotifyUserIds;

        public String getAppId() { return appId; }
        public void setAppId(String appId) { this.appId = appId; }

        public String getAppSecret() { return appSecret; }
        public void setAppSecret(String appSecret) { this.appSecret = appSecret; }

        public String getWebhookUrl() { return webhookUrl; }
        public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }

        public String getChatId() { return chatId; }
        public void setChatId(String chatId) { this.chatId = chatId; }

        public String getVerifyToken() { return verifyToken; }
        public void setVerifyToken(String verifyToken) { this.verifyToken = verifyToken; }

        public String getEncryptKey() { return encryptKey; }
        public void setEncryptKey(String encryptKey) { this.encryptKey = encryptKey; }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getCallbackToken() { return callbackToken; }
        public void setCallbackToken(String callbackToken) { this.callbackToken = callbackToken; }

        public int getCallbackExpireMinutes() { return callbackExpireMinutes; }
        public void setCallbackExpireMinutes(int callbackExpireMinutes) { this.callbackExpireMinutes = callbackExpireMinutes; }

        public String getApprovalNotifyUserIds() { return approvalNotifyUserIds; }
        public void setApprovalNotifyUserIds(String approvalNotifyUserIds) { this.approvalNotifyUserIds = approvalNotifyUserIds; }
    }

    /**
     * 钉钉机器人配置
     *
     * 配置说明：
     * - webhook-url: 钉钉机器人Webhook地址（必填）
     * - secret: 加签密钥（可选，安全设置中的"加签"选项）
     * - enabled: 是否启用
     */
    public static class DingTalkConfig {
        /** Webhook地址 */
        private String webhookUrl;
        /** 加签密钥（可选） */
        private String secret;
        /** 是否启用 */
        private boolean enabled = false;

        public String getWebhookUrl() { return webhookUrl; }
        public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
    
    public static class SchedulerConfig {
        private long producerIntervalMs = 300000;
        private long agentIntervalMs = 600000;
        private boolean enabled = true;
        
        public long getProducerIntervalMs() { return producerIntervalMs; }
        public void setProducerIntervalMs(long producerIntervalMs) { this.producerIntervalMs = producerIntervalMs; }
        
        public long getAgentIntervalMs() { return agentIntervalMs; }
        public void setAgentIntervalMs(long agentIntervalMs) { this.agentIntervalMs = agentIntervalMs; }
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class SkillsConfig {
        private boolean enabled = true;
        private boolean autoLearn = true;
        private int maxSkillsPerAgent = 50;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public boolean isAutoLearn() { return autoLearn; }
        public void setAutoLearn(boolean autoLearn) { this.autoLearn = autoLearn; }

        public int getMaxSkillsPerAgent() { return maxSkillsPerAgent; }
        public void setMaxSkillsPerAgent(int maxSkillsPerAgent) { this.maxSkillsPerAgent = maxSkillsPerAgent; }
    }

    public static class ContextConfig {
        private long snapshotIntervalMs = 3600000;
        private int maxSnapshots = 5;
        private boolean recoveryEnabled = true;

        public long getSnapshotIntervalMs() { return snapshotIntervalMs; }
        public void setSnapshotIntervalMs(long snapshotIntervalMs) { this.snapshotIntervalMs = snapshotIntervalMs; }

        public int getMaxSnapshots() { return maxSnapshots; }
        public void setMaxSnapshots(int maxSnapshots) { this.maxSnapshots = maxSnapshots; }

        public boolean isRecoveryEnabled() { return recoveryEnabled; }
        public void setRecoveryEnabled(boolean recoveryEnabled) { this.recoveryEnabled = recoveryEnabled; }
    }

    public static class SecurityConfig {
        private int deviceTrustDays = 7;
        private boolean deviceTrustEnabled = true;

        public int getDeviceTrustDays() { return deviceTrustDays; }
        public void setDeviceTrustDays(int deviceTrustDays) { this.deviceTrustDays = deviceTrustDays; }

        public boolean isDeviceTrustEnabled() { return deviceTrustEnabled; }
        public void setDeviceTrustEnabled(boolean deviceTrustEnabled) { this.deviceTrustEnabled = deviceTrustEnabled; }
    }

    /**
     * 邮件配置
     */
    public static class EmailConfig {
        /** 是否启用邮件 */
        private boolean enabled = false;
        /** SMTP 服务器地址 */
        private String smtpHost = "";
        /** SMTP 端口 */
        private int smtpPort = 587;
        /** SMTP 用户名 */
        private String smtpUsername = "";
        /** SMTP 密码 */
        private String smtpPassword = "";
        /** 发件人地址 */
        private String emailFrom = "";
        /** 发件人名字（显示在邮件发件人字段） */
        private String senderName = "";
        /** 代理邮箱（用于发送，可选） */
        private String proxyEmail = "";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getSmtpHost() { return smtpHost; }
        public void setSmtpHost(String smtpHost) { this.smtpHost = smtpHost; }

        public int getSmtpPort() { return smtpPort; }
        public void setSmtpPort(int smtpPort) { this.smtpPort = smtpPort; }

        public String getSmtpUsername() { return smtpUsername; }
        public void setSmtpUsername(String smtpUsername) { this.smtpUsername = smtpUsername; }

        public String getSmtpPassword() { return smtpPassword; }
        public void setSmtpPassword(String smtpPassword) { this.smtpPassword = smtpPassword; }

        public String getEmailFrom() { return emailFrom; }
        public void setEmailFrom(String emailFrom) { this.emailFrom = emailFrom; }

        public String getSenderName() { return senderName; }
        public void setSenderName(String senderName) { this.senderName = senderName; }

        public String getProxyEmail() { return proxyEmail; }
        public void setProxyEmail(String proxyEmail) { this.proxyEmail = proxyEmail; }
    }
}
