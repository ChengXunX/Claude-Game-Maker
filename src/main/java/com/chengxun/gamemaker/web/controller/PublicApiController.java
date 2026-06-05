package com.chengxun.gamemaker.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 公开 API 控制器
 * 提供不需要认证的公开接口，如健康检查、系统状态等
 *
 * @author chengxun
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/public")
@Tag(name = "公开API", description = "不需要认证的公开接口")
public class PublicApiController {

    /**
     * 健康检查接口
     * 用于负载均衡器、监控系统检测服务是否可用
     *
     * @return 健康状态信息
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "返回服务健康状态，用于监控和负载均衡")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "ChengXun Game Maker");
        response.put("version", "1.0.0");
        return ResponseEntity.ok(response);
    }

    /**
     * 系统信息接口
     * 返回系统基本信息，不包含敏感数据
     *
     * @return 系统信息
     */
    @GetMapping("/info")
    @Operation(summary = "系统信息", description = "返回系统基本信息")
    public ResponseEntity<Map<String, Object>> systemInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "ChengXun Game Maker");
        info.put("description", "AI-driven game development automation system");
        info.put("version", "1.0.0");
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("osName", System.getProperty("os.name"));
        info.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        info.put("maxMemory", Runtime.getRuntime().maxMemory() / 1024 / 1024 + " MB");
        return ResponseEntity.ok(info);
    }
}
