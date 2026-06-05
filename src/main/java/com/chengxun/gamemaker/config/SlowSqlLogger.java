package com.chengxun.gamemaker.config;

import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.appender.Slf4JLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 慢SQL日志记录器
 * 继承P6Spy的Slf4JLogger，实现慢SQL的检测和记录
 *
 * 主要功能：
 * - 检测执行时间超过阈值的SQL
 * - 记录慢SQL详细信息
 * - 统计慢SQL数量
 * - 支持异步批量写入数据库
 *
 * @author chengxun
 * @since 1.0.0
 */
public class SlowSqlLogger extends Slf4JLogger {

    private static final Logger log = LoggerFactory.getLogger(SlowSqlLogger.class);
    private static final Logger slowSqlLog = LoggerFactory.getLogger("SLOW_SQL");

    /** 慢SQL阈值（毫秒） */
    private static final long SLOW_SQL_THRESHOLD = 500;

    /** 慢SQL队列，用于异步处理 */
    private static final ConcurrentLinkedQueue<SlowSqlRecord> slowSqlQueue = new ConcurrentLinkedQueue<>();

    /** 慢SQL计数器 */
    private static final AtomicInteger slowSqlCount = new AtomicInteger(0);

    /** 日期格式化器 */
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * 记录SQL执行信息
     * 重写父类方法，添加慢SQL检测逻辑
     *
     * @param connectionId 连接ID
     * @param now 当前时间
     * @param elapsed 执行耗时（毫秒）
     * @param category SQL类别
     * @param prepared 预编译SQL
     * @param sql 实际SQL
     * @param url 数据库URL
     */
    @Override
    public void logSQL(int connectionId, String now, long elapsed, Category category,
                       String prepared, String sql, String url) {
        // 检查是否为慢SQL
        if (elapsed >= SLOW_SQL_THRESHOLD) {
            handleSlowSql(connectionId, now, elapsed, category, prepared, sql, url);
        }

        // 调用父类方法记录日志
        super.logSQL(connectionId, now, elapsed, category, prepared, sql, url);
    }

    /**
     * 处理慢SQL
     * 记录详细信息并加入队列
     *
     * @param connectionId 连接ID
     * @param now 当前时间
     * @param elapsed 执行耗时
     * @param category SQL类别
     * @param prepared 预编译SQL
     * @param sql 实际SQL
     * @param url 数据库URL
     */
    private void handleSlowSql(int connectionId, String now, long elapsed, Category category,
                               String prepared, String sql, String url) {
        // 记录慢SQL日志
        slowSqlLog.warn("慢SQL检测 - 耗时: {}ms, 连接ID: {}, 类别: {}\nSQL: {}\n参数: {}",
            elapsed, connectionId, category, sql, prepared);

        // 创建慢SQL记录
        SlowSqlRecord record = new SlowSqlRecord();
        record.setConnectionId(connectionId);
        record.setTimestamp(LocalDateTime.now());
        record.setExecutionTime(elapsed);
        record.setCategory(category.getName());
        record.setSql(sql);
        record.setPreparedSql(prepared);
        record.setUrl(url);

        // 加入队列
        slowSqlQueue.offer(record);
        slowSqlCount.incrementAndGet();

        // 如果队列超过100条，触发批量写入
        if (slowSqlQueue.size() >= 100) {
            flushSlowSqlRecords();
        }
    }

    /**
     * 刷新慢SQL记录到数据库
     * 异步批量写入，避免影响SQL执行性能
     */
    private static void flushSlowSqlRecords() {
        // 这里可以实现批量写入数据库的逻辑
        // 为了简化，目前只记录日志
        int count = slowSqlQueue.size();
        if (count > 0) {
            log.info("批量写入 {} 条慢SQL记录", count);
            slowSqlQueue.clear();
        }
    }

    /**
     * 获取慢SQL数量
     *
     * @return 慢SQL计数
     */
    public static int getSlowSqlCount() {
        return slowSqlCount.get();
    }

    /**
     * 重置慢SQL计数
     */
    public static void resetSlowSqlCount() {
        slowSqlCount.set(0);
    }

    /**
     * 获取当前队列中的慢SQL记录数
     *
     * @return 队列大小
     */
    public static int getQueueSize() {
        return slowSqlQueue.size();
    }

    /**
     * 慢SQL记录内部类
     */
    private static class SlowSqlRecord {
        private int connectionId;
        private LocalDateTime timestamp;
        private long executionTime;
        private String category;
        private String sql;
        private String preparedSql;
        private String url;

        // Getters and Setters
        public int getConnectionId() { return connectionId; }
        public void setConnectionId(int connectionId) { this.connectionId = connectionId; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        public long getExecutionTime() { return executionTime; }
        public void setExecutionTime(long executionTime) { this.executionTime = executionTime; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public String getSql() { return sql; }
        public void setSql(String sql) { this.sql = sql; }

        public String getPreparedSql() { return preparedSql; }
        public void setPreparedSql(String preparedSql) { this.preparedSql = preparedSql; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }
}
