package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.PerformanceMetric;
import com.chengxun.gamemaker.web.repository.PerformanceMetricRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 性能监控服务
 * 负责收集、统计和分析系统及Agent的性能指标
 *
 * 主要功能：
 * - 记录性能指标数据
 * - 计算统计信息（平均值、最大值、最小值）
 * - 生成性能报告
 * - 监控系统资源
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
@Transactional
public class PerformanceMonitorService {

    private static final Logger log = LoggerFactory.getLogger(PerformanceMonitorService.class);

    @Autowired
    private PerformanceMetricRepository metricRepository;

    @Autowired
    private AlertService alertService;

    // ===== 指标记录 =====

    /**
     * 记录Agent响应时间
     * @param agentId Agent ID
     * @param agentName Agent名称
     * @param responseTimeMs 响应时间（毫秒）
     */
    public void recordAgentResponseTime(String agentId, String agentName, long responseTimeMs) {
        PerformanceMetric metric = new PerformanceMetric();
        metric.setMetricName("agent_response_time");
        metric.setMetricType(PerformanceMetric.MetricType.AGENT_RESPONSE_TIME.name());
        metric.setValue((double) responseTimeMs);
        metric.setUnit("ms");
        metric.setAgentId(agentId);
        metric.setAgentName(agentName);
        metric.setCreatedAt(LocalDateTime.now());

        metricRepository.save(metric);

        // 检查是否触发告警
        alertService.checkAndTrigger("AGENT_RESPONSE_TIME", responseTimeMs, agentId, agentName, null);

        log.debug("Recorded agent response time: {} = {}ms", agentId, responseTimeMs);
    }

    /**
     * 记录API调用
     * @param apiPath API路径
     * @param responseTimeMs 响应时间（毫秒）
     * @param success 是否成功
     */
    public void recordApiCall(String apiPath, long responseTimeMs, boolean success) {
        // 记录响应时间
        PerformanceMetric responseMetric = new PerformanceMetric();
        responseMetric.setMetricName("api_response_time");
        responseMetric.setMetricType(PerformanceMetric.MetricType.API_RESPONSE_TIME.name());
        responseMetric.setValue((double) responseTimeMs);
        responseMetric.setUnit("ms");
        responseMetric.setApiPath(apiPath);
        responseMetric.setCreatedAt(LocalDateTime.now());
        metricRepository.save(responseMetric);

        // 记录调用次数
        PerformanceMetric callMetric = new PerformanceMetric();
        callMetric.setMetricName("api_call_count");
        callMetric.setMetricType(PerformanceMetric.MetricType.API_CALL_COUNT.name());
        callMetric.setValue(1.0);
        callMetric.setUnit("count");
        callMetric.setApiPath(apiPath);
        callMetric.setTags("{\"success\": " + success + "}");
        callMetric.setCreatedAt(LocalDateTime.now());
        metricRepository.save(callMetric);

        // 检查是否触发告警
        alertService.checkAndTrigger("API_RESPONSE_TIME", responseTimeMs, null, null, null);

        log.debug("Recorded API call: {} {}ms success={}", apiPath, responseTimeMs, success);
    }

    /**
     * 记录Token消耗
     * @param agentId Agent ID
     * @param agentName Agent名称
     * @param tokenCount Token数量
     */
    public void recordTokenUsage(String agentId, String agentName, int tokenCount) {
        PerformanceMetric metric = new PerformanceMetric();
        metric.setMetricName("token_usage");
        metric.setMetricType(PerformanceMetric.MetricType.TOKEN_USAGE.name());
        metric.setValue((double) tokenCount);
        metric.setUnit("tokens");
        metric.setAgentId(agentId);
        metric.setAgentName(agentName);
        metric.setCreatedAt(LocalDateTime.now());

        metricRepository.save(metric);

        // 检查是否触发告警
        alertService.checkAndTrigger("TOKEN_USAGE", tokenCount, agentId, agentName, null);

        log.debug("Recorded token usage: {} = {} tokens", agentId, tokenCount);
    }

    /**
     * 记录任务执行时间
     * @param agentId Agent ID
     * @param agentName Agent名称
     * @param taskId 任务ID
     * @param durationMs 执行时间（毫秒）
     * @param success 是否成功
     */
    public void recordTaskDuration(String agentId, String agentName, String taskId, long durationMs, boolean success) {
        PerformanceMetric metric = new PerformanceMetric();
        metric.setMetricName("task_duration");
        metric.setMetricType(PerformanceMetric.MetricType.TASK_DURATION.name());
        metric.setValue((double) durationMs);
        metric.setUnit("ms");
        metric.setAgentId(agentId);
        metric.setAgentName(agentName);
        metric.setTags("{\"taskId\": \"" + taskId + "\", \"success\": " + success + "}");
        metric.setCreatedAt(LocalDateTime.now());

        metricRepository.save(metric);

        log.debug("Recorded task duration: {} task={} {}ms success={}", agentId, taskId, durationMs, success);
    }

    /**
     * 记录错误
     * @param agentId Agent ID（可选）
     * @param errorType 错误类型
     * @param errorMessage 错误信息
     */
    public void recordError(String agentId, String errorType, String errorMessage) {
        PerformanceMetric metric = new PerformanceMetric();
        metric.setMetricName("error_count");
        metric.setMetricType(PerformanceMetric.MetricType.ERROR_RATE.name());
        metric.setValue(1.0);
        metric.setUnit("count");
        metric.setAgentId(agentId);
        metric.setTags("{\"errorType\": \"" + errorType + "\", \"message\": \"" + errorMessage + "\"}");
        metric.setCreatedAt(LocalDateTime.now());

        metricRepository.save(metric);

        // 检查错误率是否触发告警
        checkErrorRate();

        log.warn("Recorded error: type={} agent={}", errorType, agentId);
    }

    /**
     * 记录自定义指标
     * @param name 指标名称
     * @param value 指标值
     * @param unit 单位
     * @param tags 标签
     */
    public void recordCustomMetric(String name, double value, String unit, String tags) {
        PerformanceMetric metric = new PerformanceMetric();
        metric.setMetricName(name);
        metric.setMetricType(PerformanceMetric.MetricType.CUSTOM.name());
        metric.setValue(value);
        metric.setUnit(unit);
        metric.setTags(tags);
        metric.setCreatedAt(LocalDateTime.now());

        metricRepository.save(metric);

        log.debug("Recorded custom metric: {} = {} {}", name, value, unit);
    }

    // ===== 统计查询 =====

    /**
     * 获取指标统计信息
     * @param metricName 指标名称
     * @param hours 查询时间范围（小时）
     * @return 统计信息Map
     */
    public Map<String, Object> getMetricStatistics(String metricName, int hours) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusHours(hours);

        Map<String, Object> stats = new HashMap<>();

        Double avg = metricRepository.calculateAverage(metricName, start, end);
        Double max = metricRepository.calculateMax(metricName, start, end);
        Double min = metricRepository.calculateMin(metricName, start, end);

        stats.put("average", avg != null ? Math.round(avg * 100.0) / 100.0 : 0);
        stats.put("max", max != null ? Math.round(max * 100.0) / 100.0 : 0);
        stats.put("min", min != null ? Math.round(min * 100.0) / 100.0 : 0);
        stats.put("metricName", metricName);
        stats.put("hours", hours);

        return stats;
    }

    /**
     * 获取Agent性能统计
     * @param agentId Agent ID
     * @param hours 查询时间范围（小时）
     * @return 统计信息Map
     */
    public Map<String, Object> getAgentPerformanceStats(String agentId, int hours) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusHours(hours);

        Map<String, Object> stats = new HashMap<>();

        // 响应时间统计
        List<PerformanceMetric> responseTimes = metricRepository.findByAgentIdAndTimeRange(agentId, start, end);
        responseTimes = responseTimes.stream()
            .filter(m -> "agent_response_time".equals(m.getMetricName()))
            .collect(Collectors.toList());

        if (!responseTimes.isEmpty()) {
            double avgResponseTime = responseTimes.stream()
                .mapToDouble(PerformanceMetric::getValue)
                .average()
                .orElse(0);
            double maxResponseTime = responseTimes.stream()
                .mapToDouble(PerformanceMetric::getValue)
                .max()
                .orElse(0);
            double minResponseTime = responseTimes.stream()
                .mapToDouble(PerformanceMetric::getValue)
                .min()
                .orElse(0);

            stats.put("avgResponseTime", Math.round(avgResponseTime * 100.0) / 100.0);
            stats.put("maxResponseTime", Math.round(maxResponseTime * 100.0) / 100.0);
            stats.put("minResponseTime", Math.round(minResponseTime * 100.0) / 100.0);
            stats.put("callCount", responseTimes.size());
        } else {
            stats.put("avgResponseTime", 0);
            stats.put("maxResponseTime", 0);
            stats.put("minResponseTime", 0);
            stats.put("callCount", 0);
        }

        // Token消耗统计
        List<PerformanceMetric> tokenUsage = metricRepository.findByAgentIdAndTimeRange(agentId, start, end);
        tokenUsage = tokenUsage.stream()
            .filter(m -> "token_usage".equals(m.getMetricName()))
            .collect(Collectors.toList());

        long totalTokens = tokenUsage.stream()
            .mapToLong(m -> m.getValue().longValue())
            .sum();
        stats.put("totalTokens", totalTokens);

        stats.put("agentId", agentId);
        stats.put("hours", hours);

        return stats;
    }

    /**
     * 获取API性能统计
     * @param hours 查询时间范围（小时）
     * @return API统计列表
     */
    public List<Map<String, Object>> getApiPerformanceStats(int hours) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusHours(hours);

        List<PerformanceMetric> apiMetrics = metricRepository.findByMetricNameAndTimeRange("api_response_time", start, end);

        // 按API路径分组
        Map<String, List<PerformanceMetric>> groupedByPath = apiMetrics.stream()
            .collect(Collectors.groupingBy(m -> m.getApiPath() != null ? m.getApiPath() : "unknown"));

        List<Map<String, Object>> stats = new ArrayList<>();

        for (Map.Entry<String, List<PerformanceMetric>> entry : groupedByPath.entrySet()) {
            Map<String, Object> apiStat = new HashMap<>();
            List<PerformanceMetric> metrics = entry.getValue();

            double avgResponseTime = metrics.stream()
                .mapToDouble(PerformanceMetric::getValue)
                .average()
                .orElse(0);

            apiStat.put("apiPath", entry.getKey());
            apiStat.put("avgResponseTime", Math.round(avgResponseTime * 100.0) / 100.0);
            apiStat.put("callCount", metrics.size());

            stats.add(apiStat);
        }

        // 按平均响应时间降序排序
        stats.sort((a, b) -> Double.compare(
            (Double) b.get("avgResponseTime"),
            (Double) a.get("avgResponseTime")
        ));

        return stats;
    }

    /**
     * 获取性能趋势（按小时）
     * @param metricName 指标名称
     * @param hours 查询时间范围（小时）
     * @return 趋势数据Map
     */
    public Map<String, Double> getMetricTrend(String metricName, int hours) {
        Map<String, Double> trend = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = hours - 1; i >= 0; i--) {
            LocalDateTime hourStart = now.minusHours(i).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime hourEnd = hourStart.plusHours(1);

            Double avg = metricRepository.calculateAverage(metricName, hourStart, hourEnd);
            String hourKey = hourStart.getHour() + ":00";
            trend.put(hourKey, avg != null ? Math.round(avg * 100.0) / 100.0 : 0);
        }

        return trend;
    }

    // ===== 系统监控 =====

    /**
     * 收集系统资源使用情况
     * 每分钟执行一次
     */
    @Scheduled(fixedRate = 60000)
    public void collectSystemMetrics() {
        try {
            // 内存使用
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            double heapUsagePercent = (double) heapUsed / heapMax * 100;

            recordCustomMetric("system_memory_usage", heapUsagePercent, "%",
                "{\"heapUsed\": " + heapUsed + ", \"heapMax\": " + heapMax + "}");

            // 检查内存使用告警
            alertService.checkAndTrigger("SYSTEM_RESOURCE", heapUsagePercent, null, null, null);

            // JVM运行时间
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
            long uptime = runtimeBean.getUptime() / 1000; // 转换为秒
            recordCustomMetric("system_uptime", (double) uptime, "seconds", null);

            log.debug("System metrics collected: heap={}%, uptime={}s",
                String.format("%.1f", heapUsagePercent), uptime);

        } catch (Exception e) {
            log.error("Failed to collect system metrics", e);
        }
    }

    /**
     * 检查错误率
     */
    private void checkErrorRate() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        // 获取最近1小时的API调用次数
        List<PerformanceMetric> apiCalls = metricRepository.findByMetricNameAndTimeRange("api_call_count", oneHourAgo, LocalDateTime.now());
        long totalCalls = apiCalls.size();

        if (totalCalls > 0) {
            // 获取失败的调用次数
            long failedCalls = apiCalls.stream()
                .filter(m -> m.getTags() != null && m.getTags().contains("\"success\": false"))
                .count();

            double errorRate = (double) failedCalls / totalCalls * 100;

            // 检查错误率告警
            alertService.checkAndTrigger("TASK_FAILURE_RATE", errorRate, null, null, null);
        }
    }

    // ===== 数据清理 =====

    /**
     * 清理过期的性能指标数据
     * 保留最近30天的数据，每批删除5000条避免长事务
     * 每天凌晨3点执行
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupOldMetrics() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        int totalDeleted = 0;
        int batchSize = 5000;

        while (true) {
            int deleted = metricRepository.deleteByCreatedAtBefore(thirtyDaysAgo, batchSize);
            totalDeleted += deleted;
            if (deleted < batchSize) break;
        }

        if (totalDeleted > 0) {
            log.info("Cleaned up {} performance metrics older than {}", totalDeleted, thirtyDaysAgo);
        }
    }
}
