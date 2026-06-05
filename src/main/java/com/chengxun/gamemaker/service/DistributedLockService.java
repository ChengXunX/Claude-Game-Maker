package com.chengxun.gamemaker.service;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁服务
 * 基于 Redisson 实现，用于防止并发操作冲突
 *
 * 使用场景：
 * - Agent 创建时防止重复创建
 * - 任务分配时防止重复分配
 * - Token 绑定时防止并发冲突
 * - 配置更新时防止并发冲突
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class DistributedLockService {

    private static final Logger log = LoggerFactory.getLogger(DistributedLockService.class);

    /** 默认等待时间（秒） */
    private static final long DEFAULT_WAIT_SECONDS = 10;

    /** 默认持有时间（秒） */
    private static final long DEFAULT_HOLD_SECONDS = 30;

    @Autowired(required = false)
    private RedissonClient redissonClient;

    /**
     * 获取锁并执行操作
     *
     * @param lockKey 锁的 key
     * @param action  要执行的操作
     * @param waitSeconds 等待获取锁的最大时间（秒）
     * @return 是否成功获取锁并执行
     */
    public boolean executeWithLock(String lockKey, Runnable action, long waitSeconds) {
        if (redissonClient == null) {
            // Redis 不可用时直接执行（单机模式降级）
            action.run();
            return true;
        }

        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(waitSeconds, DEFAULT_HOLD_SECONDS, TimeUnit.SECONDS)) {
                try {
                    action.run();
                    return true;
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            }
            log.warn("Failed to acquire lock: {}", lockKey);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Lock acquisition interrupted: {}", lockKey);
            return false;
        }
    }

    /**
     * 获取锁并执行操作（默认等待时间）
     */
    public boolean executeWithLock(String lockKey, Runnable action) {
        return executeWithLock(lockKey, action, DEFAULT_WAIT_SECONDS);
    }

    /**
     * 获取锁并返回结果
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> action, long waitSeconds) {
        if (redissonClient == null) {
            return action.get();
        }

        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(waitSeconds, DEFAULT_HOLD_SECONDS, TimeUnit.SECONDS)) {
                try {
                    return action.get();
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            }
            log.warn("Failed to acquire lock: {}", lockKey);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * 检查锁是否被持有
     */
    public boolean isLocked(String lockKey) {
        if (redissonClient == null) return false;
        RLock lock = redissonClient.getLock(lockKey);
        return lock.isLocked();
    }
}
