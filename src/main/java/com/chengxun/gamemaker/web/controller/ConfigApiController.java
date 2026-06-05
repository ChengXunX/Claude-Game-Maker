package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.config.AppConfig;
import com.chengxun.gamemaker.web.entity.SystemConfig;
import com.chengxun.gamemaker.web.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 配置中心 API 控制器
 * 提供系统配置的 CRUD 接口
 *
 * 操作维度：系统级
 * 权限要求：系统管理员
 *
 * @author chengxun
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/configs")
@Tag(name = "配置中心", description = "系统配置相关接口")
public class ConfigApiController {

    private static final Logger log = LoggerFactory.getLogger(ConfigApiController.class);

    /** 敏感配置关键词模式：配置键中包含这些关键词的值需要脱敏 */
    private static final Pattern SENSITIVE_KEY_PATTERN = Pattern.compile(
        "(?i)(\\.key$|\\.secret$|\\.password$|\\.token$|credential|\\.auth\\.)");

    /** 敏感配置键集合（精确匹配） */
    private static final Set<String> SENSITIVE_KEYS = Set.of(
        "claude.api.key",
        "feishu.app.secret",
        "dingtalk.secret"
    );

    private final SystemConfigService configService;
    private final AppConfig appConfig;

    public ConfigApiController(SystemConfigService configService, AppConfig appConfig) {
        this.configService = configService;
        this.appConfig = appConfig;
    }

    /**
     * 获取配置列表
     *
     * @param group 配置分组（可选）
     * @return 配置列表
     */
    @GetMapping
    @Operation(summary = "获取配置列表")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SystemConfig>> list(
            @RequestParam(required = false) String group) {

        List<SystemConfig> configs;
        if (group != null && !group.isEmpty()) {
            configs = configService.getConfigsByGroup(group);
        } else {
            configs = configService.getAllConfigs();
        }

        // 对敏感配置值进行脱敏处理
        configs.forEach(this::maskSensitiveValue);

        return ResponseEntity.ok(configs);
    }

    /**
     * 获取配置的原始值（解密/脱敏前）
     * 仅限管理员使用，用于编辑敏感配置时获取完整值
     *
     * @param id 配置ID
     * @return 配置原始值
     */
    @GetMapping("/{id}/reveal")
    @Operation(summary = "获取配置原始值", description = "获取敏感配置的完整值（仅管理员）")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> reveal(@PathVariable Long id) {
        SystemConfig config = configService.getAllConfigs().stream()
            .filter(c -> c.getId().equals(id))
            .findFirst()
            .orElse(null);

        if (config == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of(
            "configKey", config.getConfigKey(),
            "configValue", config.getConfigValue() != null ? config.getConfigValue() : ""
        ));
    }

    /**
     * 按分组获取配置
     *
     * @param group 配置分组
     * @return 配置列表
     */
    @GetMapping("/group/{group}")
    @Operation(summary = "按分组获取配置")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SystemConfig>> getByGroup(@PathVariable String group) {
        List<SystemConfig> configs = configService.getConfigsByGroup(group);
        configs.forEach(this::maskSensitiveValue);
        return ResponseEntity.ok(configs);
    }

    /**
     * 获取所有配置分组
     *
     * @return 分组列表
     */
    @GetMapping("/groups")
    @Operation(summary = "获取所有分组")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<String>> getGroups() {
        return ResponseEntity.ok(configService.getAllGroups());
    }

    /**
     * 更新配置
     *
     * @param id 配置ID
     * @param data 配置数据
     * @return 操作结果
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新配置")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, String> data) {
        try {
            String value = data.get("configValue");
            if (value == null) {
                return ResponseEntity.badRequest().body(com.chengxun.gamemaker.web.dto.ErrorResponse.badRequest("配置值不能为空"));
            }

            // 查找配置
            SystemConfig config = configService.getAllConfigs().stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElse(null);

            if (config == null) {
                return ResponseEntity.notFound().build();
            }

            configService.setConfig(config.getConfigKey(), value);
            // 同步更新内存中的 AppConfig
            syncAppConfig(config.getConfigKey(), value);

            log.info("配置更新成功: {} = {}", config.getConfigKey(), value);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("更新配置失败: {}", id, e);
            return ResponseEntity.internalServerError().body(com.chengxun.gamemaker.web.dto.ErrorResponse.internal("更新失败: " + e.getMessage()));
        }
    }

    /**
     * 创建配置
     *
     * @param config 配置数据
     * @return 创建的配置
     */
    @PostMapping
    @Operation(summary = "创建配置")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@RequestBody SystemConfig config) {
        try {
            if (config.getConfigKey() == null || config.getConfigKey().isEmpty()) {
                return ResponseEntity.badRequest().body(com.chengxun.gamemaker.web.dto.ErrorResponse.badRequest("配置键不能为空"));
            }

            SystemConfig created = configService.createConfig(config);

            log.info("配置创建成功: {}", config.getConfigKey());
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            log.error("创建配置失败", e);
            return ResponseEntity.internalServerError().body(com.chengxun.gamemaker.web.dto.ErrorResponse.internal("创建失败: " + e.getMessage()));
        }
    }

    /**
     * 删除配置
     *
     * @param id 配置ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除配置")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            configService.deleteConfig(id);
            log.info("配置删除成功: {}", id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            log.error("删除配置失败: {}", id, e);
            return ResponseEntity.badRequest().body(com.chengxun.gamemaker.web.dto.ErrorResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 刷新配置缓存
     *
     * @return 操作结果
     */
    @PostMapping("/refresh-cache")
    @Operation(summary = "刷新缓存")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> refreshCache() {
        configService.refreshCache();
        log.info("配置缓存已刷新");
        return ResponseEntity.ok().build();
    }

    /**
     * 测试 AI 模型连接
     * 向 AI API 发送一个最小请求，验证 API Key 和 URL 是否有效
     *
     * @param request 包含 apiKey、apiUrl、model 的配置
     * @return 测试结果
     */
    @PostMapping("/test-ai-connection")
    @Operation(summary = "测试AI模型连接", description = "验证 API Key 和 API URL 是否可用")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> testAiConnection(@RequestBody Map<String, String> request) {
        String apiKey = request.getOrDefault("apiKey", "");
        String apiUrl = request.getOrDefault("apiUrl", "https://api.anthropic.com");
        String model = request.getOrDefault("model", "claude-sonnet-4-20250514");

        // 清理模型名称中的长上下文标记 [1m]
        if (model != null && model.contains("[1m]")) {
            model = model.replace("[1m]", "").trim();
        }

        // 如果请求中没有 apiKey，尝试从内存配置获取
        if (apiKey.isEmpty()) {
            apiKey = appConfig.getApiKey();
        }
        if (apiKey == null || apiKey.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "API Key 未配置"
            ));
        }

        try {
            // 构造最小请求体（messages API）
            String requestBody = String.format(
                "{\"model\":\"%s\",\"max_tokens\":1,\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}]}",
                model
            );

            // 确保 URL 以 /v1/messages 结尾
            String endpoint = apiUrl;
            if (endpoint.endsWith("/")) {
                endpoint = endpoint.substring(0, endpoint.length() - 1);
            }
            if (!endpoint.endsWith("/v1/messages")) {
                endpoint += "/v1/messages";
            }

            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(15))
                .build();

            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "连接成功，API Key 有效"
                ));
            } else if (response.statusCode() == 401) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "API Key 无效（HTTP 401）"
                ));
            } else if (response.statusCode() == 403) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "API Key 无权限（HTTP 403）"
                ));
            } else if (response.statusCode() == 404) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "API URL 不正确，接口不存在（HTTP 404）"
                ));
            } else {
                // 返回 API 的错误信息
                String body = response.body();
                String detail = body.length() > 200 ? body.substring(0, 200) : body;
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", String.format("连接失败（HTTP %d）: %s", response.statusCode(), detail)
                ));
            }

        } catch (java.net.ConnectException e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "无法连接到 API 服务器，请检查 API URL"
            ));
        } catch (java.net.UnknownHostException e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "无法解析 API 地址，请检查 API URL"
            ));
        } catch (Exception e) {
            log.error("测试 AI 连接失败", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "连接测试失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 批量更新配置
     *
     * @param updates 配置更新列表，每项包含 configKey 和 configValue
     * @return 操作结果
     */
    @PutMapping("/batch")
    @Operation(summary = "批量更新配置")
    @PreAuthorize("hasRole('ADMIN')")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> batchUpdate(@RequestBody List<Map<String, String>> updates) {
        try {
            int count = 0;
            for (Map<String, String> item : updates) {
                String key = item.get("configKey");
                String value = item.get("configValue");
                if (key != null && value != null) {
                    configService.setConfig(key, value);
                    count++;
                    // 同步更新内存中的 AppConfig，使 Claude CLI 等组件立即生效
                    syncAppConfig(key, value);
                }
            }
            log.info("批量更新配置成功: {} 项", count);
            return ResponseEntity.ok(Map.of("success", true, "updated", count));
        } catch (Exception e) {
            log.error("批量更新配置失败", e);
            return ResponseEntity.internalServerError().body(com.chengxun.gamemaker.web.dto.ErrorResponse.internal("批量更新失败: " + e.getMessage()));
        }
    }

    /**
     * 同步配置变更到内存中的 AppConfig
     * 使 Claude CLI 等组件无需重启即可使用新配置
     */
    private void syncAppConfig(String key, String value) {
        switch (key) {
            case "claude.api.key":
                appConfig.getClaude().setApiKey(value);
                log.info("AppConfig 已同步: claude.api.key");
                break;
            case "claude.api.url":
                appConfig.getClaude().setApiUrl(value);
                log.info("AppConfig 已同步: claude.api.url");
                break;
            case "claude.model":
                appConfig.getClaude().setModel(value);
                log.info("AppConfig 已同步: claude.model");
                break;
            case "claude.max.tokens":
                try {
                    appConfig.getClaude().setMaxTokens(Integer.parseInt(value));
                    log.info("AppConfig 已同步: claude.max.tokens");
                } catch (NumberFormatException e) {
                    log.warn("Invalid max tokens value: {}", value);
                }
                break;
            // 安全配置同步（前端使用点分隔，后端 AppConfig 使用连字符）
            case "security.device.trust.enabled":
            case "security.device-trust-enabled":
                appConfig.getSecurity().setDeviceTrustEnabled(Boolean.parseBoolean(value));
                log.info("AppConfig 已同步: deviceTrustEnabled = {}", value);
                break;
            case "security.device.trust.days":
            case "security.device-trust-days":
                try {
                    appConfig.getSecurity().setDeviceTrustDays(Integer.parseInt(value));
                    log.info("AppConfig 已同步: deviceTrustDays = {}", value);
                } catch (NumberFormatException e) {
                    log.warn("Invalid device trust days value: {}", value);
                }
                break;
            default:
                break;
        }
    }

    /**
     * 判断配置键是否为敏感类型
     * 通过精确匹配和关键词匹配两种方式判断
     *
     * @param configKey 配置键
     * @return 是否为敏感配置
     */
    private boolean isSensitiveKey(String configKey) {
        if (configKey == null) return false;
        // 精确匹配
        if (SENSITIVE_KEYS.contains(configKey)) return true;
        // 关键词匹配
        return SENSITIVE_KEY_PATTERN.matcher(configKey).find();
    }

    /**
     * 对敏感配置值进行脱敏处理
     * 保留前 3 位和后 4 位，中间用 * 替代
     * 值长度不足 8 位时全部用 * 替代
     *
     * @param config 配置对象（直接修改其 configValue）
     */
    private void maskSensitiveValue(SystemConfig config) {
        String value = config.getConfigValue();
        if (value == null || value.isEmpty()) return;
        if (!isSensitiveKey(config.getConfigKey())) return;

        int len = value.length();
        if (len <= 8) {
            // 短值全部脱敏
            config.setConfigValue("*".repeat(len));
        } else {
            // 保留前3后4，中间用*替代
            String prefix = value.substring(0, 3);
            String suffix = value.substring(len - 4);
            config.setConfigValue(prefix + "*".repeat(len - 7) + suffix);
        }
    }
}
