package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.PerformanceMetric;
import com.chengxun.gamemaker.web.repository.PerformanceMetricRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 监控指标收集服务
 * 负责收集和存储系统监控指标
 *
 * 主要功能：
 * - 收集Agent性能指标
 * - 收集系统资源指标
 * - 收集API调用指标
 * - 指标聚合和统计
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
@Transactional
public class MetricsCollectorService {

    private static final Logger log = LoggerFactory.getLogger(MetricsCollectorService.class);

    @Autowired
    private PerformanceMetricRepository metricRepository;

    /** API调用计数器 */
    private final Map<String, AtomicLong> apiCallCounters = new ConcurrentHashMap<>();

    /** API响应时间 */
    private final Map<String, List<Long>> apiResponseTimes = new ConcurrentHashMap<>();

    /** 用于同步 apiResponseTimes 操作的锁对象 */
    private final Object apiResponseTimesLock = new Object();

    /** Agent任务计数器 */
    private final Map<String, AtomicLong> agentTaskCounters = new ConcurrentHashMap<>();

    /** 错误计数器 */
    private final Map<String, AtomicLong> errorCounters = new ConcurrentHashMap<>();

    /**
     * 记录API调用
     *
     * @param endpoint API端点
     * @param responseTimeMs 响应时间（毫秒）
     * @param success 是否成功
     */
    public void recordApiCall(String endpoint, long responseTimeMs, boolean success) {
        // 增加调用计数
        apiCallCounters.computeIfAbsent(endpoint, k -> new AtomicLong(0)).incrementAndGet();

        // 记录响应时间（使用同步块保护 ArrayList 的并发访问）
        synchronized (apiResponseTimesLock) {
            apiResponseTimes.computeIfAbsent(endpoint, k -> new ArrayList<>()).add(responseTimeMs);
        }

        // 如果失败，增加错误计数
        if (!success) {
            errorCounters.computeIfAbsent(endpoint, k -> new AtomicLong(0)).incrementAndGet();
        }
    }

    /**
     * 记录Agent任务
     *
     * @param agentId Agent ID
     * @param success 是否成功
     */
    public void recordAgentTask(String agentId, boolean success) {
        agentTaskCounters.computeIfAbsent(agentId, k -> new AtomicLong(0)).incrementAndGet();

        if (!success) {
            errorCounters.computeIfAbsent("agent:" + agentId, k -> new AtomicLong(0)).incrementAndGet();
        }
    }

    /**
     * 获取API调用统计
     *
     * @return 统计数据
     */
    public Map<String, Object> getApiCallStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 总调用次数
        long totalCalls = apiCallCounters.values().stream()
            .mapToLong(AtomicLong::get)
            .sum();
        stats.put("totalCalls", totalCalls);

        // 各端点调用次数
        Map<String, Long> endpointCalls = new HashMap<>();
        apiCallCounters.forEach((key, value) -> endpointCalls.put(key, value.get()));
        stats.put("endpointCalls", endpointCalls);

        // 平均响应时间
        Map<String, Double> avgResponseTimes = new HashMap<>();
        apiResponseTimes.forEach((key, times) -> {
            if (!times.isEmpty()) {
                double avg = times.stream().mapToLong(Long::longValue).average().orElse(0);
                avgResponseTimes.put(key, Math.round(avg * 100.0) / 100.0);
            }
        });
        stats.put("avgResponseTimes", avgResponseTimes);

        // 错误率
        Map<String, Double> errorRates = new HashMap<>();
        apiCallCounters.forEach((endpoint, count) -> {
            long errors = errorCounters.getOrDefault(endpoint, new AtomicLong(0)).get();
            double rate = count.get() > 0 ? (double) errors / count.get() * 100 : 0;
            errorRates.put(endpoint, Math.round(rate * 100.0) / 100.0);
        });
        stats.put("errorRates", errorRates);

        return stats;
    }

    /**
     * 获取Agent任务统计
     *
     * @return 统计数据
     */
    public Map<String, Object> getAgentTaskStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 各Agent任务数
        Map<String, Long> agentTasks = new HashMap<>();
        agentTaskCounters.forEach((key, value) -> agentTasks.put(key, value.get()));
        stats.put("agentTasks", agentTasks);

        // 总任务数
        long totalTasks = agentTaskCounters.values().stream()
            .mapToLong(AtomicLong::get)
            .sum();
        stats.put("totalTasks", totalTasks);

        return stats;
    }

    /**
     * 获取错误统计
     *
     * @return 统计数据
     */
    public Map<String, Object> getErrorStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 各类型错误数
        Map<String, Long> errorCounts = new HashMap<>();
        errorCounters.forEach((key, value) -> errorCounts.put(key, value.get()));
        stats.put("errorCounts", errorCounts);

        // 总错误数
        long totalErrors = errorCounters.values().stream()
            .mapToLong(AtomicLong::get)
            .sum();
        stats.put("totalErrors", totalErrors);

        return stats;
    }

    /**
     * 定期保存指标到数据库
     * 每5分钟执行一次
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void saveMetricsToDatabase() {
        log.info("Saving metrics to database...");

        try {
            // 保存API调用指标
            saveApiCallMetrics();

            // 保存Agent任务指标
            saveAgentTaskMetrics();

            // 保存错误指标
            saveErrorMetrics();

            log.info("Metrics saved successfully");

        } catch (Exception e) {
            log.error("Failed to save metrics: {}", e.getMessage());
        }
    }

    /**
     * 保存API调用指标
     */
    private void saveApiCallMetrics() {
        apiCallCounters.forEach((endpoint, count) -> {
            PerformanceMetric metric = new PerformanceMetric();
            metric.setMetricName("api.calls");
            metric.setValue((double) count.get());
            metric.setTags("{\"endpoint\":\"" + endpoint + "\"}");
            metric.setCreatedAt(LocalDateTime.now());
            metricRepository.save(metric);
        });
    }

    /**
     * 保存Agent任务指标
     */
    private void saveAgentTaskMetrics() {
        agentTaskCounters.forEach((agentId, count) -> {
            PerformanceMetric metric = new PerformanceMetric();
            metric.setMetricName("agent.tasks");
            metric.setValue((double) count.get());
            metric.setTags("{\"agentId\":\"" + agentId + "\"}");
            metric.setCreatedAt(LocalDateTime.now());
            metricRepository.save(metric);
        });
    }

    /**
     * 保存错误指标
     */
    private void saveErrorMetrics() {
        errorCounters.forEach((type, count) -> {
            PerformanceMetric metric = new PerformanceMetric();
            metric.setMetricName("errors");
            metric.setValue((double) count.get());
            metric.setTags("{\"type\":\"" + type + "\"}");
            metric.setCreatedAt(LocalDateTime.now());
            metricRepository.save(metric);
        });
    }

    /**
     * 定期清理过长的响应时间列表
     * 每小时执行一次
     */
    @Scheduled(fixedRate = 3600000)
    public void cleanupResponseTimes() {
        synchronized (apiResponseTimesLock) {
            int maxSize = 10000; // 每个端点最多保留10000条记录
            apiResponseTimes.forEach((endpoint, times) -> {
                if (times.size() > maxSize) {
                    // 只保留最新的记录
                    List<Long> trimmed = new ArrayList<>(times.subList(times.size() - maxSize, times.size()));
                    times.clear();
                    times.addAll(trimmed);
                }
            });
        }
        log.debug("Cleaned up response time records");
    }

    /**
     * 重置计数器
     */
    public void resetCounters() {
        apiCallCounters.clear();
        synchronized (apiResponseTimesLock) {
            apiResponseTimes.clear();
        }
        agentTaskCounters.clear();
        errorCounters.clear();
        log.info("All counters reset");
    }
}
