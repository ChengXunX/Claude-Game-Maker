package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.Role;
import com.chengxun.gamemaker.web.entity.SystemConfig;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.repository.RoleRepository;
import com.chengxun.gamemaker.web.repository.SystemConfigRepository;
import com.chengxun.gamemaker.web.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 安装向导服务
 * 处理系统首次安装的配置和初始化
 *
 * 配置持久化策略：
 * - 所有配置保存到 system_configs 表
 * - 启动时从数据库加载，覆盖 application.yml 默认值
 * - 重启后配置依然生效
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class InstallService {

    private static final Logger log = LoggerFactory.getLogger(InstallService.class);

    /** 安装状态文件 */
    private static final String INSTALL_MARKER = "data/.installed";

    private final DataSource dataSource;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SystemConfigRepository configRepository;

    public InstallService(DataSource dataSource, PasswordEncoder passwordEncoder,
                          UserRepository userRepository, RoleRepository roleRepository,
                          SystemConfigRepository configRepository) {
        this.dataSource = dataSource;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.configRepository = configRepository;
    }

    // ===== 安装状态 =====

    public boolean isInstalled() {
        return Files.exists(Path.of(INSTALL_MARKER));
    }

    public Map<String, Object> getInstallStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("installed", isInstalled());
        if (isInstalled()) {
            try {
                String content = Files.readString(Path.of(INSTALL_MARKER));
                status.put("installedAt", content.trim());
            } catch (IOException e) {
                status.put("installedAt", "unknown");
            }
        }
        return status;
    }

    // ===== 数据库 =====

    public Map<String, Object> testDatabaseConnection() {
        Map<String, Object> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            result.put("success", true);
            result.put("database", meta.getDatabaseProductName());
            result.put("version", meta.getDatabaseProductVersion());
            result.put("url", meta.getURL());
            result.put("driver", meta.getDriverName());
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    /**
     * 测试 MySQL 数据库连接
     */
    public Map<String, Object> testMysqlConnection(String host, int port, String database, String username, String password) {
        Map<String, Object> result = new HashMap<>();
        String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true", host, port, database);
        try (Connection conn = java.sql.DriverManager.getConnection(url, username, password)) {
            DatabaseMetaData meta = conn.getMetaData();
            result.put("success", true);
            result.put("database", meta.getDatabaseProductName());
            result.put("version", meta.getDatabaseProductVersion());
            result.put("url", url);
            result.put("message", "MySQL 连接成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "连接失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 保存 MySQL 配置到 application.yml 或环境变量
     */
    public void saveMysqlConfig(String host, int port, String database, String username, String password) {
        saveConfig("mysql.host", host, "MySQL 主机", "database");
        saveConfig("mysql.port", String.valueOf(port), "MySQL 端口", "database");
        saveConfig("mysql.database", database, "MySQL 数据库名", "database");
        saveConfig("mysql.username", username, "MySQL 用户名", "database");
        if (password != null && !password.isEmpty()) {
            saveConfig("mysql.password", password, "MySQL 密码", "database");
        }
    }

    public Map<String, Object> checkDatabaseTables() {
        Map<String, Object> result = new HashMap<>();
        List<String> existingTables = new ArrayList<>();
        List<String> missingTables = new ArrayList<>();

        List<String> requiredTables = List.of(
            "users", "roles", "role_permissions", "system_configs",
            "agent_presets", "project_members", "api_tokens",
            "notification_templates", "alert_rules"
        );

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, null, "%", new String[]{"TABLE"})) {
                Set<String> tables = new HashSet<>();
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME").toLowerCase());
                }
                for (String table : requiredTables) {
                    if (tables.contains(table.toLowerCase())) {
                        existingTables.add(table);
                    } else {
                        missingTables.add(table);
                    }
                }
            }
            result.put("success", true);
            result.put("existingTables", existingTables);
            result.put("missingTables", missingTables);
            result.put("totalRequired", requiredTables.size());
            result.put("totalExisting", existingTables.size());
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> initializeDatabase() {
        Map<String, Object> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            Path mysqlScript = Path.of("sql/sql_create_mysql.sql");
            if (Files.exists(mysqlScript)) {
                String sql = Files.readString(mysqlScript);
                String[] statements = sql.split(";");
                int executed = 0;
                int errors = 0;
                for (String statement : statements) {
                    String trimmed = statement.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                        try {
                            stmt.execute(trimmed);
                            executed++;
                        } catch (Exception e) {
                            if (!e.getMessage().contains("already exists")) {
                                errors++;
                                log.warn("SQL execution warning: {}", e.getMessage());
                            }
                        }
                    }
                }
                result.put("success", true);
                result.put("executedStatements", executed);
                result.put("errors", errors);
            } else {
                result.put("success", false);
                result.put("error", "SQL script not found: sql/sql_create_mysql.sql");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    /**
     * 初始化数据库（带实时进度回调）
     * 执行 create 和 init_data 两个脚本，通过回调通知进度
     *
     * @param progressCallback 进度回调，参数为 (当前步骤, 总步骤, 消息)
     * @return 执行结果
     */
    @FunctionalInterface
    public interface ProgressCallback {
        void onProgress(int step, int total, String message);
    }

    public Map<String, Object> initializeDatabaseWithProgress(ProgressCallback progressCallback) {
        Map<String, Object> result = new HashMap<>();
        int totalExecuted = 0;
        int totalErrors = 0;

        // 要执行的脚本列表
        List<String> scripts = List.of("sql/sql_create_mysql.sql", "sql/sql_init_data_mysql.sql");
        int totalScripts = scripts.size();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            for (int s = 0; s < totalScripts; s++) {
                String scriptPath = scripts.get(s);
                Path script = Path.of(scriptPath);

                if (!Files.exists(script)) {
                    progressCallback.onProgress(s + 1, totalScripts, "跳过: " + scriptPath + " (文件不存在)");
                    continue;
                }

                String sql = Files.readString(script);
                String[] statements = sql.split(";");
                int scriptTotal = 0;
                // 先统计有效语句数
                for (String st : statements) {
                    String trimmed = st.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                        scriptTotal++;
                    }
                }

                progressCallback.onProgress(s + 1, totalScripts,
                    "开始执行: " + scriptPath + " (共 " + scriptTotal + " 条语句)");

                int scriptExecuted = 0;
                int scriptErrors = 0;

                for (String statement : statements) {
                    String trimmed = statement.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                        try {
                            stmt.execute(trimmed);
                            scriptExecuted++;
                            totalExecuted++;

                            // 每10条语句报告一次进度
                            if (scriptExecuted % 10 == 0 || scriptExecuted == scriptTotal) {
                                progressCallback.onProgress(s + 1, totalScripts,
                                    String.format("[%d/%d] 已执行 %d/%d 条语句",
                                        s + 1, totalScripts, scriptExecuted, scriptTotal));
                            }
                        } catch (Exception e) {
                            if (e.getMessage().contains("already exists") ||
                                e.getMessage().contains("Duplicate")) {
                                scriptExecuted++;
                                totalExecuted++;
                            } else {
                                scriptErrors++;
                                totalErrors++;
                                log.warn("SQL execution warning in {}: {}", scriptPath, e.getMessage());
                            }
                        }
                    }
                }

                progressCallback.onProgress(s + 1, totalScripts,
                    String.format("完成: %s (执行 %d 条, 错误 %d 条)",
                        scriptPath, scriptExecuted, scriptErrors));
            }

            result.put("success", true);
            result.put("executedStatements", totalExecuted);
            result.put("errors", totalErrors);
            result.put("scripts", totalScripts);
            progressCallback.onProgress(totalScripts, totalScripts,
                String.format("全部完成！共执行 %d 条语句, %d 个错误", totalExecuted, totalErrors));

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            progressCallback.onProgress(0, 0, "错误: " + e.getMessage());
        }
        return result;
    }

    // ===== 连接测试 =====

    /**
     * 测试 Redis 连接
     */
    public Map<String, Object> testRedisConnection(String host, int port, String password) {
        Map<String, Object> result = new HashMap<>();
        try (Socket socket = new Socket(host, port)) {
            result.put("success", true);
            result.put("host", host);
            result.put("port", port);
            result.put("message", "Redis 端口可达");
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "无法连接到 " + host + ":" + port + " - " + e.getMessage());
        }
        return result;
    }

    /**
     * 测试邮件 SMTP 连接
     */
    public Map<String, Object> testEmailConnection(String host, int port, String username, String password) {
        Map<String, Object> result = new HashMap<>();
        try (Socket socket = new Socket(host, port)) {
            result.put("success", true);
            result.put("host", host);
            result.put("port", port);
            result.put("message", "SMTP 服务器可达");
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "无法连接到 SMTP 服务器 " + host + ":" + port + " - " + e.getMessage());
        }
        return result;
    }

    /**
     * 测试飞书 Webhook 连接
     */
    public Map<String, Object> testFeishuConnection(String webhookUrl) {
        Map<String, Object> result = new HashMap<>();
        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"msg_type\":\"text\",\"content\":{\"text\":\"连接测试\"}}"))
                .timeout(Duration.ofSeconds(10))
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                result.put("success", true);
                result.put("message", "飞书 Webhook 连接成功");
            } else {
                result.put("success", false);
                result.put("error", "飞书返回状态码: " + response.statusCode() + " - " + response.body());
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "飞书连接失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 测试钉钉 Webhook 连接
     */
    public Map<String, Object> testDingTalkConnection(String webhookUrl, String secret) {
        Map<String, Object> result = new HashMap<>();
        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"msgtype\":\"text\",\"text\":{\"content\":\"连接测试\"}}"))
                .timeout(Duration.ofSeconds(10))
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                result.put("success", true);
                result.put("message", "钉钉 Webhook 连接成功");
            } else {
                result.put("success", false);
                result.put("error", "钉钉返回状态码: " + response.statusCode() + " - " + response.body());
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "钉钉连接失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 测试 Claude API 连接
     */
    public Map<String, Object> testClaudeApiConnection(String apiKey, String apiUrl) {
        return testClaudeApiConnection(apiKey, apiUrl, null);
    }

    /**
     * 测试 Claude API 连接（支持自定义模型）
     */
    public Map<String, Object> testClaudeApiConnection(String apiKey, String apiUrl, String model) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (apiUrl == null || apiUrl.isEmpty()) {
                apiUrl = "https://api.anthropic.com";
            }
            // 确保URL不以/结尾
            if (apiUrl.endsWith("/")) {
                apiUrl = apiUrl.substring(0, apiUrl.length() - 1);
            }

            // 使用用户选择的模型，或默认模型
            if (model == null || model.isEmpty()) {
                model = "claude-haiku-4-5-20251001";
            }

            // 移除长上下文模式后缀 [1m]，因为这是CLI参数，不是API模型名称的一部分
            String apiModel = model.replaceAll("\\[1m\\]$", "");

            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

            // 使用JSON库正确转义模型名称，避免特殊字符问题
            String escapedModel = apiModel.replace("\\", "\\\\").replace("\"", "\\\"");
            String requestBody = "{\"model\":\"" + escapedModel + "\",\"max_tokens\":10,\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}]}";

            // 构造完整的API端点URL
            String endpoint = apiUrl;
            if (!endpoint.endsWith("/")) {
                endpoint += "/";
            }
            endpoint += "v1/messages";

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(30))
                .build();

            log.info("测试 API 连接: URL={}, Model={} (原始: {})", endpoint, apiModel, model);

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("API 响应: Status={}, Body={}", response.statusCode(), response.body());

            if (response.statusCode() == 200) {
                result.put("success", true);
                result.put("message", "Claude API 连接成功");
                result.put("model", model);
            } else if (response.statusCode() == 401) {
                result.put("success", false);
                result.put("error", "API Key 无效");
            } else if (response.statusCode() == 400) {
                // 显示 API 返回的实际错误信息
                String errorMsg = "API 返回错误: " + response.body();
                result.put("success", false);
                result.put("error", errorMsg);
            } else if (response.statusCode() == 403) {
                result.put("success", false);
                result.put("error", "API Key 权限不足或已过期");
            } else if (response.statusCode() == 429) {
                result.put("success", false);
                result.put("error", "API 请求频率超限，请稍后再试");
            } else {
                result.put("success", false);
                result.put("error", "API 返回状态码: " + response.statusCode() + ", 响应: " + response.body());
            }
        } catch (java.net.ConnectException e) {
            result.put("success", false);
            result.put("error", "无法连接到 API 服务器，请检查 URL 是否正确");
        } catch (java.net.UnknownHostException e) {
            result.put("success", false);
            result.put("error", "无法解析 API 服务器地址，请检查 URL");
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "Claude API 连接失败: " + e.getMessage());
        }
        return result;
    }

    // ===== 配置保存（持久化到数据库） =====

    /**
     * 保存配置到数据库
     * 配置持久化到 system_configs 表，重启后依然生效
     */
    public void saveConfig(String key, String value, String description, String group) {
        SystemConfig config = configRepository.findByConfigKey(key).orElse(null);
        if (config != null) {
            config.setConfigValue(value);
        } else {
            config = new SystemConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setDescription(description);
            config.setGroup(group);
            config.setValueType("string");
            config.setSystemBuiltin(false);
        }
        configRepository.save(config);
        log.info("Config saved: {} = {}", key, maskSensitive(key, value));
    }

    /**
     * 批量保存配置
     */
    public void saveConfigs(Map<String, String> configs, String group) {
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            saveConfig(entry.getKey(), entry.getValue(), "", group);
        }
    }

    /**
     * 保存系统基础配置
     *
     * @param systemName 系统名称
     * @param jwtSecret JWT密钥
     * @param contactLink 联系方式值（链接/邮箱/图片URL）
     * @param contactType 联系方式类型（link/email/image）
     */
    public void saveSystemSettings(String systemName, String jwtSecret, String contactLink, String contactType) {
        if (systemName != null && !systemName.isEmpty()) {
            saveConfig("system.name", systemName, "系统名称", "system");
        }
        if (jwtSecret != null && !jwtSecret.isEmpty()) {
            saveConfig("security.jwt.secret", jwtSecret, "JWT 密钥", "security");
        }
        if (contactType != null && !contactType.isEmpty()) {
            saveConfig("system.contact.type", contactType, "联系方式类型", "system");
        }
        if (contactLink != null) {
            saveConfig("system.contact.link", contactLink, "联系方式", "system");
        }
    }

    /**
     * 保存 Claude API 配置
     */
    public void saveClaudeConfig(String apiKey, String apiUrl, String model, String maxTokens) {
        if (apiKey != null) saveConfig("claude.api.key", apiKey, "Claude API Key", "agent");
        if (apiUrl != null) saveConfig("claude.api.url", apiUrl, "Claude API URL", "agent");
        if (model != null) saveConfig("claude.model", model, "Claude 模型", "agent");
        if (maxTokens != null && !maxTokens.isEmpty()) saveConfig("claude.max.tokens", maxTokens, "Claude 最大 Token", "agent");
    }

    /**
     * 保存邮件配置
     */
    public void saveEmailConfig(boolean enabled, String host, int port, String username,
                                String password, String from, String senderName, String proxyEmail) {
        saveConfig("email.enabled", String.valueOf(enabled), "邮件通知开关", "email");
        saveConfig("email.smtp.host", host, "SMTP 服务器", "email");
        saveConfig("email.smtp.port", String.valueOf(port), "SMTP 端口", "email");
        saveConfig("email.smtp.username", username, "SMTP 用户名", "email");
        saveConfig("email.smtp.password", password, "SMTP 密码", "email");
        if (from != null) saveConfig("email.from", from, "发件人地址", "email");
        if (senderName != null) saveConfig("email.sender.name", senderName, "发件人名称", "email");
        if (proxyEmail != null) saveConfig("email.proxy.email", proxyEmail, "代理邮箱", "email");
    }

    /**
     * 保存飞书配置
     */
    public void saveFeishuConfig(String webhookUrl, String appId, String appSecret) {
        if (webhookUrl != null) saveConfig("feishu.webhook.url", webhookUrl, "飞书 Webhook URL", "notification");
        if (appId != null) saveConfig("feishu.app.id", appId, "飞书 App ID", "notification");
        if (appSecret != null) saveConfig("feishu.app.secret", appSecret, "飞书 App Secret", "notification");
        saveConfig("feishu.enabled", "true", "飞书通知开关", "notification");
    }

    /**
     * 保存钉钉配置
     */
    public void saveDingTalkConfig(String webhookUrl, String secret) {
        if (webhookUrl != null) saveConfig("dingtalk.webhook.url", webhookUrl, "钉钉 Webhook URL", "notification");
        if (secret != null) saveConfig("dingtalk.secret", secret, "钉钉加签密钥", "notification");
        saveConfig("dingtalk.enabled", "true", "钉钉通知开关", "notification");
    }

    /**
     * 保存 Redis 配置
     */
    public void saveRedisConfig(String host, int port, String password, int database) {
        saveConfig("redis.host", host, "Redis 主机", "system");
        saveConfig("redis.port", String.valueOf(port), "Redis 端口", "system");
        if (password != null && !password.isEmpty()) {
            saveConfig("redis.password", password, "Redis 密码", "system");
        }
        saveConfig("redis.database", String.valueOf(database), "Redis 数据库", "system");
    }

    // ===== 管理员创建 =====

    public Map<String, Object> createAdminAccount(String username, String password, String email) {
        Map<String, Object> result = new HashMap<>();

        if (username == null || username.length() < 3) {
            result.put("success", false);
            result.put("error", "用户名至少3个字符");
            return result;
        }
        if (password == null || password.length() < 8) {
            result.put("success", false);
            result.put("error", "密码至少8个字符");
            return result;
        }

        try {
            if (userRepository.existsByUsername(username)) {
                result.put("success", false);
                result.put("error", "用户名已存在");
                return result;
            }

            Role adminRole = roleRepository.findByName("ADMIN").orElse(null);
            if (adminRole == null) {
                adminRole = new Role();
                adminRole.setName("ADMIN");
                adminRole.setDisplayName("管理员");
                adminRole.setDescription("系统管理员，拥有所有权限");
                adminRole.setSystem(true);
                adminRole = roleRepository.save(adminRole);
            }

            // 确保管理员拥有所有权限（完整60个）
            Set<String> allPermissions = Set.of(
                "agents:view", "agents:manage", "agents:task",
                "skills:view", "skills:manage",
                "tokens:view", "tokens:manage",
                "projects:view", "projects:manage", "projects:edit",
                "system:monitor", "system:manage", "system:view", "system:config", "system:config:manage", "system:monitor:manage",
                "ai:use", "ai:admin",
                "approval:view", "approval:manage",
                "notification:view", "notification:manage", "notifications:manage",
                "admin:manage", "roles:manage",
                "users:view", "users:manage", "logs:view", "log:view",
                "code:review",
                "pipeline:view", "pipeline:manage", "pipeline:create", "pipeline:execute", "pipeline:approve", "pipeline:intervene",
                "workflow:view", "workflow:manage",
                "dashboard:view", "terminal:use",
                "knowledge:manage",
                "agent:view", "agent:manage", "agent:config", "agent:optimize",
                "version:manage", "data:view",
                "PERM_capabilities:view", "PERM_capabilities:manage",
                "PERM_mcp:view", "PERM_mcp:manage",
                "PERM_files:view", "PERM_files:manage",
                "PERM_constants:view", "PERM_constants:manage",
                "PERM_permissions:view", "PERM_permissions:manage",
                "PERM_api:view", "PERM_notification:preferences", "PERM_context:monitor"
            );
            if (!adminRole.getPermissions().containsAll(allPermissions)) {
                adminRole.setPermissions(new HashSet<>(allPermissions));
                adminRole = roleRepository.save(adminRole);
            }

            User admin = new User();
            admin.setUsername(username);
            admin.setPassword(passwordEncoder.encode(password));
            admin.setEmail(email);
            admin.setNickname("管理员");
            admin.setRole(adminRole);
            admin.setStatus(User.UserStatus.APPROVED);
            admin.setMustChangePassword(false);
            userRepository.save(admin);

            result.put("success", true);
            result.put("message", "管理员账号创建成功");
            result.put("username", username);
            log.info("Admin account created: {}", username);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "创建管理员失败: " + e.getMessage());
            log.error("Failed to create admin account", e);
        }
        return result;
    }

    // ===== 安装完成 =====

    public Map<String, Object> completeInstallation() {
        Map<String, Object> result = new HashMap<>();
        try {
            Files.createDirectories(Path.of("data"));
            Files.writeString(Path.of(INSTALL_MARKER),
                "Installed at: " + LocalDateTime.now() + "\n");
            result.put("success", true);
            result.put("message", "安装完成");
            log.info("Installation completed");
        } catch (IOException e) {
            result.put("success", false);
            result.put("error", "创建安装标记失败: " + e.getMessage());
        }
        return result;
    }

    public void resetInstallation() {
        try {
            Files.deleteIfExists(Path.of(INSTALL_MARKER));
            log.info("Installation reset");
        } catch (IOException e) {
            log.error("Failed to reset installation", e);
        }
    }

    /**
     * 重置数据库（测试用）
     * 清空所有业务表数据，但保留表结构
     */
    public Map<String, Object> resetDatabase() {
        Map<String, Object> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // 禁用外键检查
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

            // 获取所有表名
            List<String> tables = new ArrayList<>();
            try (ResultSet rs = conn.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    // 跳过系统表
                    if (!tableName.equals("flyway_schema_history")) {
                        tables.add(tableName);
                    }
                }
            }

            // 清空每个表
            int cleared = 0;
            for (String table : tables) {
                try {
                    stmt.execute("TRUNCATE TABLE " + table);
                    cleared++;
                } catch (Exception e) {
                    log.warn("Failed to truncate table {}: {}", table, e.getMessage());
                }
            }

            // 启用外键检查
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");

            // 删除安装标记
            resetInstallation();

            result.put("success", true);
            result.put("message", "数据库已重置");
            result.put("tablesCleared", cleared);
            result.put("totalTables", tables.size());
            log.info("Database reset: {} tables cleared", cleared);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "重置数据库失败: " + e.getMessage());
            log.error("Failed to reset database", e);
        }
        return result;
    }

    // ===== 工具方法 =====

    /**
     * 敏感信息脱敏
     */
    private String maskSensitive(String key, String value) {
        if (value == null) return "null";
        if (key.contains("password") || key.contains("secret") || key.contains("key")) {
            if (value.length() <= 8) return "****";
            return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
        }
        return value;
    }
}
