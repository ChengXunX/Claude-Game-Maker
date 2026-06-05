package com.chengxun.gamemaker.web.util;

import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.exception.BusinessException;
import com.chengxun.gamemaker.web.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全工具类
 * 提供用户认证和授权相关的通用方法
 *
 * 主要功能：
 * - 获取当前登录用户
 * - 获取当前用户名
 * - 验证用户权限
 *
 * 使用示例：
 * ```java
 * // 获取当前用户名
 * String username = SecurityUtil.getCurrentUsername();
 *
 * // 获取当前用户对象
 * User user = SecurityUtil.getCurrentUser(userService);
 *
 * // 获取当前用户ID
 * Long userId = SecurityUtil.getCurrentUserId(userService);
 * ```
 *
 * @author chengxun
 * @since 1.0.0
 */
public final class SecurityUtil {

    private SecurityUtil() {
        // 工具类不允许实例化
    }

    /**
     * 获取当前认证对象
     *
     * @return 当前认证对象，如果未登录返回null
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 获取当前用户名
     *
     * @return 当前用户名，如果未登录返回null
     */
    public static String getCurrentUsername() {
        Authentication authentication = getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }

    /**
     * 获取当前用户对象
     *
     * @param userService 用户服务
     * @return 当前用户对象
     * @throws BusinessException 当用户不存在时抛出
     */
    public static User getCurrentUser(UserService userService) {
        String username = getCurrentUsername();
        if (username == null) {
            throw BusinessException.forbidden("未登录");
        }

        User user = userService.getUserByUsername(username);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        return user;
    }

    /**
     * 获取当前用户ID
     *
     * @param userService 用户服务
     * @return 当前用户ID
     * @throws BusinessException 当用户不存在时抛出
     */
    public static Long getCurrentUserId(UserService userService) {
        return getCurrentUser(userService).getId();
    }

    /**
     * 检查当前用户是否已认证
     *
     * @return 是否已认证
     */
    public static boolean isAuthenticated() {
        Authentication authentication = getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }

    /**
     * 检查当前用户是否拥有指定权限
     *
     * @param permission 权限标识
     * @return 是否拥有权限
     */
    public static boolean hasPermission(String permission) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("PERM_" + permission));
    }

    /**
     * 检查当前用户是否拥有指定角色
     *
     * @param role 角色名称
     * @return 是否拥有角色
     */
    public static boolean hasRole(String role) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

    /**
     * 要求当前用户必须拥有指定权限，否则抛出异常
     *
     * @param permission 权限标识
     * @throws BusinessException 当用户没有权限时抛出
     */
    public static void requirePermission(String permission) {
        if (!hasPermission(permission)) {
            throw BusinessException.forbidden("权限不足: " + permission);
        }
    }

    /**
     * 要求当前用户必须拥有指定角色，否则抛出异常
     *
     * @param role 角色名称
     * @throws BusinessException 当用户没有角色时抛出
     */
    public static void requireRole(String role) {
        if (!hasRole(role)) {
            throw BusinessException.forbidden("角色不足: " + role);
        }
    }
}
