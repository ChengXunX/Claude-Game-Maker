package com.chengxun.gamemaker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 系统保护服务
 * 保护核心系统功能不被误删除或修改
 *
 * 受保护的功能：
 * - AI系统助手
 * - 用户认证
 * - 角色管理
 * - 系统配置
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class SystemProtectionService {

    private static final Logger log = LoggerFactory.getLogger(SystemProtectionService.class);

    /** 受保护的功能列表 */
    private static final Set<String> PROTECTED_FEATURES = Set.of(
        "ai-assistant",           // AI系统助手
        "user-authentication",    // 用户认证
        "role-management",        // 角色管理
        "system-config",          // 系统配置
        "audit-log",              // 审计日志
        "backup-restore",         // 备份恢复
        "producer-agent"          // 制作人Agent（核心）
    );

    /** 受保护的核心角色（不可删除） */
    private static final Set<String> PROTECTED_ROLES = Set.of(
        // 注意：系统预设角色也可以被解雇，但需要管理员审批
        // 只有制作人角色在当前项目中不可被解雇（除非管理员批准）
    );

    /** 受保护的权限列表 */
    private static final Set<String> PROTECTED_PERMISSIONS = Set.of(
        "PERM_ai:use",            // AI助手使用权限
        "PERM_ai:admin",          // AI助手管理权限
        "PERM_system:manage",     // 系统管理权限
        "PERM_role:manage",       // 角色管理权限
        "PERM_user:manage"        // 用户管理权限
    );

    /** 受保护的页面路径 */
    private static final Set<String> PROTECTED_PAGES = Set.of(
        "/ai-assistant",
        "/admin/users",
        "/admin/settings",
        "/roles"
    );

    /** 受保护的API路径 */
    private static final Set<String> PROTECTED_APIS = Set.of(
        "/api/ai-assistant",
        "/api/system",
        "/api/roles"
    );

    /**
     * 检查功能是否受保护
     */
    public boolean isFeatureProtected(String featureId) {
        return PROTECTED_FEATURES.contains(featureId);
    }

    /**
     * 检查角色是否受保护（不可删除）
     */
    public boolean isRoleProtected(String role) {
        return PROTECTED_ROLES.contains(role);
    }

    /**
     * 检查权限是否受保护
     */
    public boolean isPermissionProtected(String permission) {
        return PROTECTED_PERMISSIONS.contains(permission);
    }

    /**
     * 检查页面是否受保护
     */
    public boolean isPageProtected(String path) {
        return PROTECTED_PAGES.contains(path);
    }

    /**
     * 检查API是否受保护
     */
    public boolean isApiProtected(String path) {
        return PROTECTED_APIS.stream().anyMatch(path::startsWith);
    }

    /**
     * 验证删除操作
     * @param targetType 删除目标类型
     * @param targetId 删除目标ID
     * @return 是否允许删除
     */
    public boolean validateDeleteOperation(String targetType, String targetId) {
        // 检查是否是受保护的功能
        if ("feature".equals(targetType) && isFeatureProtected(targetId)) {
            log.warn("Attempt to delete protected feature: {}", targetId);
            return false;
        }

        // 检查是否是受保护的权限
        if ("permission".equals(targetType) && isPermissionProtected(targetId)) {
            log.warn("Attempt to delete protected permission: {}", targetId);
            return false;
        }

        return true;
    }

    /**
     * 验证修改操作
     * @param targetType 修改目标类型
     * @param targetId 修改目标ID
     * @return 是否允许修改
     */
    public boolean validateModifyOperation(String targetType, String targetId) {
        // 受保护的功能只能由管理员修改
        if ("feature".equals(targetType) && isFeatureProtected(targetId)) {
            log.info("Protected feature modification requires admin: {}", targetId);
            return true; // 允许修改，但需要管理员权限
        }

        return true;
    }

    /**
     * 获取受保护的功能列表
     */
    public Set<String> getProtectedFeatures() {
        return new HashSet<>(PROTECTED_FEATURES);
    }

    /**
     * 获取受保护的权限列表
     */
    public Set<String> getProtectedPermissions() {
        return new HashSet<>(PROTECTED_PERMISSIONS);
    }

    /**
     * 获取保护状态报告
     */
    public Map<String, Object> getProtectionStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("protectedFeatures", PROTECTED_FEATURES.size());
        status.put("protectedPermissions", PROTECTED_PERMISSIONS.size());
        status.put("protectedPages", PROTECTED_PAGES.size());
        status.put("protectedApis", PROTECTED_APIS.size());
        status.put("features", PROTECTED_FEATURES);
        status.put("permissions", PROTECTED_PERMISSIONS);
        return status;
    }

    /**
     * 记录保护事件
     */
    public void logProtectionEvent(String eventType, String target, String user) {
        log.warn("Protection event: {} - {} by {}", eventType, target, user);
    }
}
