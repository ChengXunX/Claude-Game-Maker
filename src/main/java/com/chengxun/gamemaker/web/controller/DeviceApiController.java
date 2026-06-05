package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.entity.DeviceTrust;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.DeviceTrustService;
import com.chengxun.gamemaker.web.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 设备信任 API 控制器
 * 提供可信设备的管理接口
 *
 * 操作维度：用户级
 * 权限要求：登录用户即可
 *
 * @author chengxun
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/devices")
@Tag(name = "设备信任", description = "可信设备相关接口")
public class DeviceApiController {

    private static final Logger log = LoggerFactory.getLogger(DeviceApiController.class);

    private final DeviceTrustService deviceTrustService;
    private final UserService userService;

    public DeviceApiController(DeviceTrustService deviceTrustService,
                               UserService userService) {
        this.deviceTrustService = deviceTrustService;
        this.userService = userService;
    }

    /**
     * 获取当前用户的可信设备列表
     *
     * @param authentication 认证信息
     * @return 设备列表
     */
    @GetMapping
    @Operation(summary = "获取可信设备列表")
    public ResponseEntity<List<DeviceTrust>> list(Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        List<DeviceTrust> devices = deviceTrustService.getTrustedDevices(user.getId());
        return ResponseEntity.ok(devices);
    }

    /**
     * 移除指定设备
     *
     * @param id 设备ID
     * @param authentication 认证信息
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "移除设备")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> remove(@PathVariable Long id, Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            deviceTrustService.removeDevice(user.getId(), id);
            log.info("设备已移除: {}", id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("移除设备失败: {}", id, e);
            return ResponseEntity.internalServerError().body(com.chengxun.gamemaker.web.dto.ErrorResponse.internal("移除失败"));
        }
    }

    /**
     * 移除当前用户的所有可信设备
     *
     * @param authentication 认证信息
     * @return 操作结果
     */
    @DeleteMapping
    @Operation(summary = "移除所有设备")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> removeAll(Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            deviceTrustService.removeAllDevices(user.getId());
            log.info("用户 {} 移除了所有可信设备", user.getUsername());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("移除所有设备失败", e);
            return ResponseEntity.internalServerError().body(com.chengxun.gamemaker.web.dto.ErrorResponse.internal("移除失败"));
        }
    }
}
