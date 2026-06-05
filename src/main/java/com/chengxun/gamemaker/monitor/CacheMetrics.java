package com.chengxun.gamemaker.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 缓存监控指标收集器
 * 收集缓存相关的性能指标
 *
 * 主要功能：
 * - 记录缓存命中/未命中次数
 * - 统计缓存操作耗时
 * - 监控缓存大小
 * - 计算缓存命中率
 *
 * 指标列表：
 * - cache.hit.total: 缓存命中总数
 * - cache.miss.total: 缓存未命中总数
 * - cache.hit.rate: 缓存命中率
 * - cache.operation.duration: 缓存操作耗时
 * - cache.size: 缓存大小
 *
 * @author chengxun
 * @since 1.0.0
 */
@Component
public class CacheMetrics {

    private static final Logger log = LoggerFactory.getLogger(CacheMetrics.class);

    private final MeterRegistry meterRegistry;

    /** 缓存命中计数器缓存 */
    private final ConcurrentHashMap<String, Counter> hitCounters = new ConcurrentHashMap<>();

    /** 缓存未命中计数器缓存 */
    private final ConcurrentHashMap<String, Counter> missCounters = new ConcurrentHashMap<>();

    /** 缓存操作耗时计时器缓存 */
    private final ConcurrentHashMap<String, Timer> operationTimers = new ConcurrentHashMap<>();

    /** 缓存命中计数（用于计算命中率） */
    private long totalHits = 0;

    /** 缓存未命中计数（用于计算命中率） */
    private long totalMisses = 0;

    public CacheMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        log.info("缓存监控指标收集器初始化完成");
    }

    /**
     * 记录缓存命中
     *
     * @param cacheName 缓存名称
     * @param key 缓存键
     */
    public void recordCacheHit(String cacheName, String key) {
        try {
            String counterKey = cacheName + ".hit";
            Counter counter = hitCounters.computeIfAbsent(counterKey,
                keyName -> Counter.builder("cache.hit.total")
                    .description("缓存命中总数")
                    .tag("cache", cacheName)
                    .register(meterRegistry));

            counter.increment();
            totalHits++;

            // 更新命中率
            updateHitRate(cacheName);

            log.debug("缓存命中已记录 - 缓存: {}, 键: {}", cacheName, key);
        } catch (Exception e) {
            log.error("记录缓存命中异常: {}", e.getMessage());
        }
    }

    /**
     * 记录缓存未命中
     *
     * @param cacheName 缓存名称
     * @param key 缓存键
     */
    public void recordCacheMiss(String cacheName, String key) {
        try {
            String counterKey = cacheName + ".miss";
            Counter counter = missCounters.computeIfAbsent(counterKey,
                keyName -> Counter.builder("cache.miss.total")
                    .description("缓存未命中总数")
                    .tag("cache", cacheName)
                    .register(meterRegistry));

            counter.increment();
            totalMisses++;

            // 更新命中率
            updateHitRate(cacheName);

            log.debug("缓存未命中已记录 - 缓存: {}, 键: {}", cacheName, key);
        } catch (Exception e) {
            log.error("记录缓存未命中异常: {}", e.getMessage());
        }
    }

    /**
     * 记录缓存操作耗时
     *
     * @param cacheName 缓存名称
     * @param operation 操作类型（GET, PUT, DELETE）
     * @param duration 耗时（毫秒）
     */
    public void recordCacheOperation(String cacheName, String operation, long duration) {
        try {
            String timerKey = cacheName + "." + operation;
            Timer timer = operationTimers.computeIfAbsent(timerKey,
                key -> Timer.builder("cache.operation.duration")
                    .description("缓存操作耗时")
                    .tag("cache", cacheName)
                    .tag("operation", operation)
                    .register(meterRegistry));

            timer.record(duration, TimeUnit.MILLISECONDS);

            log.debug("缓存操作耗时已记录 - 缓存: {}, 操作: {}, 耗时: {}ms",
                cacheName, operation, duration);
        } catch (Exception e) {
            log.error("记录缓存操作耗时异常: {}", e.getMessage());
        }
    }

    /**
     * 记录缓存大小
     *
     * @param cacheName 缓存名称
     * @param size 缓存大小
     */
    public void recordCacheSize(String cacheName, int size) {
        try {
            meterRegistry.gauge("cache.size",
                io.micrometer.core.instrument.Tags.of("cache", cacheName),
                size);

            log.debug("缓存大小已记录 - 缓存: {}, 大小: {}", cacheName, size);
        } catch (Exception e) {
            log.error("记录缓存大小异常: {}", e.getMessage());
        }
    }

    /**
     * 更新缓存命中率
     *
     * @param cacheName 缓存名称
     */
    private void updateHitRate(String cacheName) {
        try {
            long total = totalHits + totalMisses;
            if (total > 0) {
                double hitRate = (double) totalHits / total * 100;
                meterRegistry.gauge("cache.hit.rate",
                    io.micrometer.core.instrument.Tags.of("cache", cacheName),
                    hitRate);

                // 检查命中率是否过低
                if (hitRate < 50) {
                    log.warn("缓存命中率过低 - 缓存: {}, 命中率: {}%", cacheName, hitRate);
                }
            }
        } catch (Exception e) {
            log.error("更新缓存命中率异常: {}", e.getMessage());
        }
    }

    /**
     * 获取缓存命中率
     *
     * @return 命中率（百分比）
     */
    public double getHitRate() {
        long total = totalHits + totalMisses;
        if (total == 0) {
            return 0.0;
        }
        return (double) totalHits / total * 100;
    }

    /**
     * 获取总命中次数
     *
     * @return 命中次数
     */
    public long getTotalHits() {
        return totalHits;
    }

    /**
     * 获取总未命中次数
     *
     * @return 未命中次数
     */
    public long getTotalMisses() {
        return totalMisses;
    }

    /**
     * 重置统计
     */
    public void resetStatistics() {
        totalHits = 0;
        totalMisses = 0;
        log.info("缓存统计已重置");
    }
}
