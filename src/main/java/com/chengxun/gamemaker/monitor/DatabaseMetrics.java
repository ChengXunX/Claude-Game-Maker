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
 * 数据库监控指标收集器
 * 收集数据库相关的性能指标
 *
 * 主要功能：
 * - 记录SQL执行耗时
 * - 统计慢SQL数量
 * - 监控数据库连接池状态
 * - 统计查询成功/失败次数
 *
 * 指标列表：
 * - db.query.duration: 查询耗时
 * - db.query.total: 查询总数
 * - db.slow.query.total: 慢查询总数
 * - db.pool.active: 活跃连接数
 * - db.pool.idle: 空闲连接数
 * - db.pool.total: 总连接数
 *
 * @author chengxun
 * @since 1.0.0
 */
@Component
public class DatabaseMetrics {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMetrics.class);

    private final MeterRegistry meterRegistry;

    /** 查询耗时计时器 */
    private final Timer queryTimer;

    /** 查询计数器缓存 */
    private final ConcurrentHashMap<String, Counter> queryCounters = new ConcurrentHashMap<>();

    /** 慢查询计数器 */
    private final Counter slowQueryCounter;

    /** 慢SQL阈值（毫秒） */
    private static final long SLOW_QUERY_THRESHOLD = 500;

    public DatabaseMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // 初始化查询耗时计时器
        this.queryTimer = Timer.builder("db.query.duration")
            .description("数据库查询耗时")
            .register(meterRegistry);

        // 初始化慢查询计数器
        this.slowQueryCounter = Counter.builder("db.slow.query.total")
            .description("慢查询总数")
            .register(meterRegistry);

        log.info("数据库监控指标收集器初始化完成");
    }

    /**
     * 记录查询耗时
     *
     * @param queryType 查询类型（SELECT, INSERT, UPDATE, DELETE）
     * @param table 表名
     * @param duration 耗时（毫秒）
     * @param success 是否成功
     */
    public void recordQuery(String queryType, String table, long duration, boolean success) {
        try {
            // 记录耗时
            queryTimer.record(duration, TimeUnit.MILLISECONDS);

            // 检查是否为慢查询
            if (duration >= SLOW_QUERY_THRESHOLD) {
                slowQueryCounter.increment();
                log.warn("慢SQL检测 - 类型: {}, 表: {}, 耗时: {}ms", queryType, table, duration);
            }

            // 记录查询计数
            String counterKey = queryType + "." + table + "." + (success ? "success" : "failure");
            Counter counter = queryCounters.computeIfAbsent(counterKey,
                key -> Counter.builder("db.query.total")
                    .description("数据库查询总数")
                    .tag("type", queryType)
                    .tag("table", table)
                    .tag("success", String.valueOf(success))
                    .register(meterRegistry));

            counter.increment();

            log.debug("数据库查询指标已记录 - 类型: {}, 表: {}, 耗时: {}ms, 成功: {}",
                queryType, table, duration, success);
        } catch (Exception e) {
            log.error("记录数据库查询指标异常: {}", e.getMessage());
        }
    }

    /**
     * 记录连接池状态
     *
     * @param active 活跃连接数
     * @param idle 空闲连接数
     * @param total 总连接数
     */
    public void recordConnectionPool(int active, int idle, int total) {
        try {
            meterRegistry.gauge("db.pool.active", active);
            meterRegistry.gauge("db.pool.idle", idle);
            meterRegistry.gauge("db.pool.total", total);

            log.debug("数据库连接池指标已记录 - 活跃: {}, 空闲: {}, 总数: {}", active, idle, total);
        } catch (Exception e) {
            log.error("记录数据库连接池指标异常: {}", e.getMessage());
        }
    }

    /**
     * 记录连接池使用率
     *
     * @param usageRate 使用率（百分比）
     */
    public void recordConnectionPoolUsage(double usageRate) {
        try {
            meterRegistry.gauge("db.pool.usage", usageRate);

            // 检查连接池使用率是否过高
            if (usageRate > 90) {
                log.warn("数据库连接池使用率过高: {}%", usageRate);
            }

            log.debug("数据库连接池使用率已记录: {}%", usageRate);
        } catch (Exception e) {
            log.error("记录数据库连接池使用率异常: {}", e.getMessage());
        }
    }

    /**
     * 记录事务统计
     *
     * @param success 是否成功
     * @param duration 耗时（毫秒）
     */
    public void recordTransaction(boolean success, long duration) {
        try {
            String counterKey = "transaction." + (success ? "success" : "failure");
            Counter counter = queryCounters.computeIfAbsent(counterKey,
                key -> Counter.builder("db.transaction.total")
                    .description("事务总数")
                    .tag("success", String.valueOf(success))
                    .register(meterRegistry));

            counter.increment();

            log.debug("数据库事务指标已记录 - 成功: {}, 耗时: {}ms", success, duration);
        } catch (Exception e) {
            log.error("记录数据库事务指标异常: {}", e.getMessage());
        }
    }

    /**
     * 获取慢查询计数
     *
     * @return 慢查询计数
     */
    public double getSlowQueryCount() {
        return slowQueryCounter.count();
    }

    /**
     * 重置慢查询计数
     */
    public void resetSlowQueryCount() {
        // Micrometer Counter 不支持重置，需要创建新的计数器
        log.info("慢查询计数重置（注意：Micrometer Counter不支持重置，需要重启应用）");
    }
}
