package com.chengxun.gamemaker.web.service;

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
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class LoginAttemptService {

    /** 最大登录尝试次数 */
    private static final int MAX_ATTEMPTS = 5;

    /** 锁定时间（毫秒）：30分钟 */
    private static final long LOCKOUT_DURATION_MS = 30 * 60 * 1000;

    /** 登录尝试记录，key为用户名 */
    private final ConcurrentHashMap<String, AttemptInfo> attempts = new ConcurrentHashMap<>();

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
        long remaining = LOCKOUT_DURATION_MS - (System.currentTimeMillis() - info.getLockoutTime());
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
     */
    private static class AttemptInfo {
        private final AtomicInteger attempts = new AtomicInteger(0);
        private volatile long lockoutTime = 0;

        public void incrementAttempts() {
            int current = attempts.incrementAndGet();
            if (current >= MAX_ATTEMPTS) {
                lockoutTime = System.currentTimeMillis();
            }
        }

        public boolean isLocked() {
            return attempts.get() >= MAX_ATTEMPTS;
        }

        public boolean isLockoutExpired() {
            return System.currentTimeMillis() - lockoutTime > LOCKOUT_DURATION_MS;
        }

        public long getLockoutTime() {
            return lockoutTime;
        }
    }
}
