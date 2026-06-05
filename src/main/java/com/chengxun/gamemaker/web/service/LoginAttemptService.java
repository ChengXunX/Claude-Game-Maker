package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.config.SystemConstants;
import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 登录尝试限制服务
 * 防止暴力破解攻击，记录和限制登录尝试次数
 *
 * 主要功能：
 * - 记录每个用户的登录失败次数
 * - 达到最大尝试次数后锁定账户
 * - 支持锁定时间自动解锁
 *
 * 配置项（通过 SystemConfigService 动态读取）：
 * - security.max-login-attempts: 最大登录尝试次数
 * - security.session-timeout-minutes: 会话超时时间（用于锁定时长）
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class LoginAttemptService {

    /** 默认最大登录尝试次数（配置不存在时使用） */
    private static final int DEFAULT_MAX_ATTEMPTS = 5;

    /** 默认锁定时间（毫秒）：30分钟 */
    private static final long DEFAULT_LOCKOUT_DURATION_MS = 30 * 60 * 1000;

    /** 配置服务 */
    private final SystemConfigService configService;

    /** 登录尝试记录，key为用户名 */
    private final ConcurrentHashMap<String, AttemptInfo> attempts = new ConcurrentHashMap<>();

    public LoginAttemptService(SystemConfigService configService) {
        this.configService = configService;
    }

    /**
     * 获取最大登录尝试次数（从配置读取）
     */
    private int getMaxAttempts() {
        return configService.getInt(SystemConstants.SECURITY_MAX_LOGIN_ATTEMPTS, DEFAULT_MAX_ATTEMPTS);
    }

    /**
     * 获取锁定时长（从配置读取，单位分钟转毫秒）
     */
    private long getLockoutDurationMs() {
        // 从 session-timeout 配置推导锁定时长，或者使用默认值
        int sessionTimeoutMinutes = configService.getInt(SystemConstants.SECURITY_SESSION_TIMEOUT, 30);
        return sessionTimeoutMinutes * 60 * 1000L;
    }

    /**
     * 记录登录失败尝试
     *
     * @param username 用户名
     */
    public void recordFailedAttempt(String username) {
        attempts.compute(username, (key, existing) -> {
            if (existing == null) {
                // 首次失败，创建新记录
                AttemptInfo info = new AttemptInfo();
                info.incrementAttempts();
                return info;
            }
            // 如果已锁定且未过期，不更新
            if (existing.isLocked() && !existing.isLockoutExpired()) {
                return existing;
            }
            // 如果锁定已过期，重置计数器
            if (existing.isLocked() && existing.isLockoutExpired()) {
                AttemptInfo info = new AttemptInfo();
                info.incrementAttempts();
                return info;
            }
            existing.incrementAttempts();
            return existing;
        });
    }

    /**
     * 记录登录成功，清除失败记录
     *
     * @param username 用户名
     */
    public void recordSuccessfulLogin(String username) {
        attempts.remove(username);
    }

    /**
     * 检查用户是否被锁定
     *
     * @param username 用户名
     * @return 如果被锁定返回 true
     */
    public boolean isLocked(String username) {
        AttemptInfo info = attempts.get(username);
        if (info == null) {
            return false;
        }
        // 检查锁定是否已过期
        if (info.isLocked() && info.isLockoutExpired()) {
            attempts.remove(username);
            return false;
        }
        return info.isLocked();
    }

    /**
     * 获取剩余锁定时间（毫秒）
     *
     * @param username 用户名
     * @return 剩余锁定时间，如果未锁定返回 0
     */
    public long getRemainingLockoutTime(String username) {
        AttemptInfo info = attempts.get(username);
        if (info == null || !info.isLocked()) {
            return 0;
        }
        long remaining = getLockoutDurationMs() - (System.currentTimeMillis() - info.getLockoutTime());
        return Math.max(0, remaining);
    }

    /**
     * 手动解锁用户账户
     *
     * @param username 用户名
     */
    public void unlock(String username) {
        attempts.remove(username);
    }

    /**
     * 内部类：登录尝试信息
     * 注意：需要通过外部类引用获取配置值
     */
    private class AttemptInfo {
        private final AtomicInteger attempts = new AtomicInteger(0);
        private volatile long lockoutTime = 0;

        public void incrementAttempts() {
            int current = attempts.incrementAndGet();
            if (current >= getMaxAttempts()) {
                lockoutTime = System.currentTimeMillis();
            }
        }

        public boolean isLocked() {
            return attempts.get() >= getMaxAttempts();
        }

        public boolean isLockoutExpired() {
            return System.currentTimeMillis() - lockoutTime > getLockoutDurationMs();
        }

        public long getLockoutTime() {
            return lockoutTime;
        }
    }
}
