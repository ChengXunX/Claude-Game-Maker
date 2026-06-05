package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.entity.SystemConfig;
import com.chengxun.gamemaker.web.service.ConfigCenterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 配置中心控制器
 * 提供系统配置管理的REST API
 *
 * 主要功能：
 * - 配置的增删改查
 * - 配置热更新
 * - 配置统计
 *
 * @author chengxun
 * @since 1.0.0
 */
@Tag(name = "配置中心", description = "系统配置管理API")
@RestController
@RequestMapping("/api/config")
public class ConfigCenterController {

    private static final Logger log = LoggerFactory.getLogger(ConfigCenterController.class);

    @Autowired
    private ConfigCenterService configService;

    /**
     * 获取配置值
     *
     * @param key 配置键
     * @return 配置值
     */
    @Operation(summary = "获取配置", description = "根据配置键获取配置值")
    @GetMapping("/{key}")
    @PreAuthorize("hasAuthority('PERM_system:config')")
    public ResponseEntity<Map<String, Object>> getConfig(@PathVariable String key) {
        String value = configService.getConfig(key);
        if (value == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
            "key", key,
            "value", value
        ));
    }

    /**
     * 设置配置值
     *
     * @param key 配置键
     * @param request 请求体，包含value、description、group
     * @return 操作结果
     */
    @Operation(summary = "设置配置", description = "设置或更新配置值")
    @PutMapping("/{key}")
    @PreAuthorize("hasAuthority('PERM_system:config:manage')")
    public ResponseEntity<Map<String, Object>> setConfig(@PathVariable String key,
                                                          @RequestBody Map<String, String> request) {
        String value = request.get("value");
        String description = request.get("description");
        String group = request.get("group");

        if (value == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "配置值不能为空"
            ));
        }

        configService.setConfig(key, value, description, group);

        log.info("Config set: {} = {}", key, value);

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "配置已更新"
        ));
    }

    /**
     * 删除配置
     *
     * @param key 配置键
     * @return 操作结果
     */
    @Operation(summary = "删除配置", description = "删除指定配置")
    @DeleteMapping("/{key}")
    @PreAuthorize("hasAuthority('PERM_system:config:manage')")
    public ResponseEntity<Map<String, Object>> deleteConfig(@PathVariable String key) {
        configService.deleteConfig(key);

        log.info("Config deleted: {}", key);

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "配置已删除"
        ));
    }

    /**
     * 获取所有配置
     *
     * @return 配置列表
     */
    @Operation(summary = "获取所有配置", description = "获取所有系统配置")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('PERM_system:config')")
    public ResponseEntity<List<SystemConfig>> getAllConfigs() {
        return ResponseEntity.ok(configService.getAllConfigs());
    }

    /**
     * 根据分组获取配置
     *
     * @param group 配置分组
     * @return 配置列表
     */
    @Operation(summary = "按分组获取配置", description = "根据分组获取配置列表")
    @GetMapping("/group/{group}")
    @PreAuthorize("hasAuthority('PERM_system:config')")
    public ResponseEntity<List<SystemConfig>> getConfigsByGroup(@PathVariable String group) {
        return ResponseEntity.ok(configService.getConfigsByGroup(group));
    }

    /**
     * 搜索配置
     *
     * @param keyword 关键词
     * @return 配置列表
     */
    @Operation(summary = "搜索配置", description = "根据关键词搜索配置")
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('PERM_system:config')")
    public ResponseEntity<List<SystemConfig>> searchConfigs(@RequestParam String keyword) {
        return ResponseEntity.ok(configService.searchConfigs(keyword));
    }

    /**
     * 批量更新配置
     *
     * @param configs 配置映射
     * @return 操作结果
     */
    @Operation(summary = "批量更新配置", description = "批量更新多个配置")
    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('PERM_system:config:manage')")
    public ResponseEntity<Map<String, Object>> batchUpdateConfigs(@RequestBody Map<String, String> configs) {
        configService.batchUpdateConfigs(configs);

        log.info("Batch updated {} configs", configs.size());

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "批量更新成功",
            "count", configs.size()
        ));
    }

    /**
     * 获取配置统计
     *
     * @return 统计数据
     */
    @Operation(summary = "获取配置统计", description = "获取配置的统计数据")
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('PERM_system:config')")
    public ResponseEntity<Map<String, Object>> getConfigStatistics() {
        return ResponseEntity.ok(configService.getConfigStatistics());
    }

    /**
     * 刷新配置缓存
     *
     * @return 操作结果
     */
    @Operation(summary = "刷新缓存", description = "刷新配置缓存")
    @PostMapping("/refresh")
    @PreAuthorize("hasAuthority('PERM_system:config:manage')")
    public ResponseEntity<Map<String, Object>> refreshCache() {
        configService.refreshCache();

        log.info("Config cache refreshed");

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "缓存已刷新"
        ));
    }
}
