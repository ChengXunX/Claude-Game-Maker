package com.chengxun.gamemaker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "game-maker")
public class AppConfig {
    private String dataDir = "data";
    private String projectsDir = "data/projects";
    private String contextsDir = "data/contexts";
    private String memoryDir = "data/memory";
    private String agentsDir = "src/main/resources/agents";
    private String defaultWorkDir = ".";
    
    private ClaudeConfig claude = new ClaudeConfig();
    private FeishuConfig feishu = new FeishuConfig();
    private SchedulerConfig scheduler = new SchedulerConfig();
    private SkillsConfig skills = new SkillsConfig();
    private ContextConfig context = new ContextConfig();
    
    // Getters and Setters
    public String getDataDir() { return dataDir; }
    public void setDataDir(String dataDir) { this.dataDir = dataDir; }
    
    public String getProjectsDir() { return projectsDir; }
    public void setProjectsDir(String projectsDir) { this.projectsDir = projectsDir; }
    
    public String getContextsDir() { return contextsDir; }
    public void setContextsDir(String contextsDir) { this.contextsDir = contextsDir; }
    
    public String getMemoryDir() { return memoryDir; }
    public void setMemoryDir(String memoryDir) { this.memoryDir = memoryDir; }
    
    public String getAgentsDir() { return agentsDir; }
    public void setAgentsDir(String agentsDir) { this.agentsDir = agentsDir; }
    
    public String getDefaultWorkDir() { return defaultWorkDir; }
    public void setDefaultWorkDir(String defaultWorkDir) { this.defaultWorkDir = defaultWorkDir; }
    
    public ClaudeConfig getClaude() { return claude; }
    public void setClaude(ClaudeConfig claude) { this.claude = claude; }
    
    public FeishuConfig getFeishu() { return feishu; }
    public void setFeishu(FeishuConfig feishu) { this.feishu = feishu; }
    
    public SchedulerConfig getScheduler() { return scheduler; }
    public void setScheduler(SchedulerConfig scheduler) { this.scheduler = scheduler; }

    public SkillsConfig getSkills() { return skills; }
    public void setSkills(SkillsConfig skills) { this.skills = skills; }

    public ContextConfig getContext() { return context; }
    public void setContext(ContextConfig context) { this.context = context; }
    
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
}
