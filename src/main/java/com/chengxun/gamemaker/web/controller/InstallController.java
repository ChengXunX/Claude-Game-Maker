package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.service.InstallService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 安装向导 API 控制器
 *
 * 安装流程：
 * 1. GET  /api/install/status — 检查安装状态
 * 2. POST /api/install/test-db — 测试数据库连接
 * 3. POST /api/install/init-db — 初始化数据库表
 * 4. POST /api/install/save-system — 保存系统配置
 * 5. POST /api/install/test-claude — 测试 Claude API
 * 6. POST /api/install/save-claude — 保存 Claude 配置
 * 7. POST /api/install/test-email — 测试邮件连接
 * 8. POST /api/install/save-email — 保存邮件配置
 * 9. POST /api/install/test-feishu — 测试飞书连接
 * 10. POST /api/install/save-feishu — 保存飞书配置
 * 11. POST /api/install/test-dingtalk — 测试钉钉连接
 * 12. POST /api/install/save-dingtalk — 保存钉钉配置
 * 13. POST /api/install/test-redis — 测试 Redis 连接
 * 14. POST /api/install/save-redis — 保存 Redis 配置
 * 15. POST /api/install/create-admin — 创建管理员账号
 * 16. POST /api/install/complete — 完成安装
 *
 * @author chengxun
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/install")
public class InstallController {

    private static final Logger log = LoggerFactory.getLogger(InstallController.class);

    private final InstallService installService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public InstallController(InstallService installService) {
        this.installService = installService;
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        return ResponseEntity.ok(installService.getInstallStatus());
    }

    // ===== 数据库 =====

    @PostMapping("/test-db")
    public ResponseEntity<?> testDatabase() {
        if (installService.isInstalled()) return gone();
        return ResponseEntity.ok(installService.testDatabaseConnection());
    }

    @PostMapping("/test-mysql-db")
    public ResponseEntity<?> testMysqlDatabase(@RequestBody Map<String, Object> request) {
        if (installService.isInstalled()) return gone();
        String host = (String) request.getOrDefault("host", "localhost");
        int port = request.get("port") != null ? ((Number) request.get("port")).intValue() : 3306;
        String database = (String) request.getOrDefault("database", "game_maker");
        String username = (String) request.getOrDefault("username", "root");
        String password = (String) request.getOrDefault("password", "");
        return ResponseEntity.ok(installService.testMysqlConnection(host, port, database, username, password));
    }

    @PostMapping("/save-mysql")
    public ResponseEntity<?> saveMysql(@RequestBody Map<String, Object> request) {
        if (installService.isInstalled()) return gone();
        String host = (String) request.getOrDefault("host", "localhost");
        int port = request.get("port") != null ? ((Number) request.get("port")).intValue() : 3306;
        String database = (String) request.getOrDefault("database", "game_maker");
        String username = (String) request.getOrDefault("username", "root");
        String password = (String) request.getOrDefault("password", "");
        installService.saveMysqlConfig(host, port, database, username, password);
        return ResponseEntity.ok(Map.of("success", true, "message", "MySQL 配置已保存"));
    }

    @PostMapping("/check-tables")
    public ResponseEntity<?> checkTables() {
        if (installService.isInstalled()) return gone();
        return ResponseEntity.ok(installService.checkDatabaseTables());
    }

    @PostMapping("/init-db")
    public ResponseEntity<?> initDatabase() {
        if (installService.isInstalled()) return gone();
        return ResponseEntity.ok(installService.initializeDatabase());
    }

    /**
     * 初始化数据库（SSE实时进度）
     * 返回 Server-Sent Events 流，实时推送执行进度
     */
    @GetMapping(value = "/init-db-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter initDatabaseStream() {
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时

        executor.submit(() -> {
            try {
                Map<String, Object> result = installService.initializeDatabaseWithProgress(
                    new InstallService.ProgressCallback() {
                        @Override
                        public void onProgress(int step, int total, String message) {
                            try {
                                emitter.send(SseEmitter.event()
                                    .name("progress")
                                    .data(Map.of(
                                        "step", step,
                                        "total", total,
                                        "message", message
                                    )));
                            } catch (Exception e) {
                                log.warn("SSE send error: {}", e.getMessage());
                            }
                        }
                    }
                );

                // 发送最终结果
                emitter.send(SseEmitter.event()
                    .name("complete")
                    .data(result));

                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    // ===== 系统配置 =====

    @PostMapping("/save-system")
    public ResponseEntity<?> saveSystem(@RequestBody Map<String, String> request) {
        if (installService.isInstalled()) return gone();
        installService.saveSystemSettings(request.get("systemName"), request.get("jwtSecret"), request.get("contactLink"));
        return ResponseEntity.ok(Map.of("success", true, "message", "系统配置已保存"));
    }

    // ===== Claude API =====

    @PostMapping("/test-claude")
    public ResponseEntity<?> testClaude(@RequestBody Map<String, String> request) {
        if (installService.isInstalled()) return gone();
        return ResponseEntity.ok(installService.testClaudeApiConnection(
            request.get("apiKey"), request.get("apiUrl"), request.get("model")));
    }

    @PostMapping("/save-claude")
    public ResponseEntity<?> saveClaude(@RequestBody Map<String, String> request) {
        if (installService.isInstalled()) return gone();
        installService.saveClaudeConfig(request.get("apiKey"), request.get("apiUrl"), request.get("model"), request.get("maxTokens"));
        return ResponseEntity.ok(Map.of("success", true, "message", "Claude 配置已保存"));
    }

    // ===== 邮件 =====

    @PostMapping("/test-email")
    public ResponseEntity<?> testEmail(@RequestBody Map<String, Object> request) {
        if (installService.isInstalled()) return gone();
        String host = (String) request.get("host");
        int port = request.get("port") != null ? ((Number) request.get("port")).intValue() : 587;
        String username = (String) request.get("username");
        String password = (String) request.get("password");
        return ResponseEntity.ok(installService.testEmailConnection(host, port, username, password));
    }

    @PostMapping("/save-email")
    public ResponseEntity<?> saveEmail(@RequestBody Map<String, Object> request) {
        if (installService.isInstalled()) return gone();
        boolean enabled = Boolean.parseBoolean(String.valueOf(request.getOrDefault("emailEnabled", true)));
        String host = (String) request.get("host");
        int port = request.get("port") != null ? ((Number) request.get("port")).intValue() : 587;
        String username = (String) request.get("username");
        String password = (String) request.get("password");
        String from = (String) request.get("from");
        String senderName = (String) request.get("senderName");
        String proxyEmail = (String) request.get("proxyEmail");
        installService.saveEmailConfig(enabled, host, port, username, password, from, senderName, proxyEmail);
        return ResponseEntity.ok(Map.of("success", true, "message", "邮件配置已保存"));
    }

    // ===== 飞书 =====

    @PostMapping("/test-feishu")
    public ResponseEntity<?> testFeishu(@RequestBody Map<String, String> request) {
        if (installService.isInstalled()) return gone();
        return ResponseEntity.ok(installService.testFeishuConnection(request.get("webhookUrl")));
    }

    @PostMapping("/save-feishu")
    public ResponseEntity<?> saveFeishu(@RequestBody Map<String, String> request) {
        if (installService.isInstalled()) return gone();
        installService.saveFeishuConfig(request.get("webhookUrl"), request.get("appId"), request.get("appSecret"));
        return ResponseEntity.ok(Map.of("success", true, "message", "飞书配置已保存"));
    }

    // ===== 钉钉 =====

    @PostMapping("/test-dingtalk")
    public ResponseEntity<?> testDingTalk(@RequestBody Map<String, String> request) {
        if (installService.isInstalled()) return gone();
        return ResponseEntity.ok(installService.testDingTalkConnection(
            request.get("webhookUrl"), request.get("secret")));
    }

    @PostMapping("/save-dingtalk")
    public ResponseEntity<?> saveDingTalk(@RequestBody Map<String, String> request) {
        if (installService.isInstalled()) return gone();
        installService.saveDingTalkConfig(request.get("webhookUrl"), request.get("secret"));
        return ResponseEntity.ok(Map.of("success", true, "message", "钉钉配置已保存"));
    }

    // ===== Redis =====

    @PostMapping("/test-redis")
    public ResponseEntity<?> testRedis(@RequestBody Map<String, Object> request) {
        if (installService.isInstalled()) return gone();
        String host = (String) request.getOrDefault("host", "localhost");
        int port = request.get("port") != null ? ((Number) request.get("port")).intValue() : 6379;
        String password = (String) request.get("password");
        return ResponseEntity.ok(installService.testRedisConnection(host, port, password));
    }

    @PostMapping("/save-redis")
    public ResponseEntity<?> saveRedis(@RequestBody Map<String, Object> request) {
        if (installService.isInstalled()) return gone();
        String host = (String) request.getOrDefault("host", "localhost");
        int port = request.get("port") != null ? ((Number) request.get("port")).intValue() : 6379;
        String password = (String) request.get("password");
        int database = request.get("database") != null ? ((Number) request.get("database")).intValue() : 0;
        installService.saveRedisConfig(host, port, password, database);
        return ResponseEntity.ok(Map.of("success", true, "message", "Redis 配置已保存"));
    }

    // ===== 管理员 =====

    @PostMapping("/create-admin")
    public ResponseEntity<?> createAdmin(@RequestBody Map<String, String> request) {
        if (installService.isInstalled()) return gone();
        return ResponseEntity.ok(installService.createAdminAccount(
            request.get("username"), request.get("password"), request.get("email")));
    }

    // ===== 完成 =====

    @PostMapping("/complete")
    public ResponseEntity<?> complete() {
        if (installService.isInstalled()) return gone();
        return ResponseEntity.ok(installService.completeInstallation());
    }

    @PostMapping("/reset")
    public ResponseEntity<?> reset() {
        installService.resetInstallation();
        return ResponseEntity.ok(Map.of("success", true, "message", "安装状态已重置"));
    }

    /**
     * 重置数据库（测试用）
     * 清空所有表数据，但保留表结构
     */
    @PostMapping("/reset-database")
    public ResponseEntity<?> resetDatabase() {
        if (installService.isInstalled()) return gone();
        Map<String, Object> result = installService.resetDatabase();
        return ResponseEntity.ok(result);
    }

    private ResponseEntity<?> gone() {
        return ResponseEntity.status(410).body(com.chengxun.gamemaker.web.dto.ErrorResponse.gone("系统已安装"));
    }
}
