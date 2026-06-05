package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.entity.SystemConfig;
import com.chengxun.gamemaker.web.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    private final SystemConfigService configService;

    public ConfigApiController(SystemConfigService configService) {
        this.configService = configService;
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

        return ResponseEntity.ok(configs);
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
        return ResponseEntity.ok(configService.getConfigsByGroup(group));
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
                }
            }
            log.info("批量更新配置成功: {} 项", count);
            return ResponseEntity.ok(Map.of("success", true, "updated", count));
        } catch (Exception e) {
            log.error("批量更新配置失败", e);
            return ResponseEntity.internalServerError().body(com.chengxun.gamemaker.web.dto.ErrorResponse.internal("批量更新失败: " + e.getMessage()));
        }
    }
}
