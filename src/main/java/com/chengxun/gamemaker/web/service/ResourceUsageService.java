package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.TokenUsage;
import com.chengxun.gamemaker.web.repository.TokenUsageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 资源使用统计服务
 * 负责Token消耗、API配额等资源的统计和管理
 *
 * 主要功能：
 * - 记录Token使用量
 * - 统计每日/每月消耗
 * - 生成资源使用报告
 * - 配额告警
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
@Transactional
public class ResourceUsageService {

    private static final Logger log = LoggerFactory.getLogger(ResourceUsageService.class);

    @Autowired
    private TokenUsageRepository tokenUsageRepository;

    @Autowired
    private AlertService alertService;

    // ===== Token使用记录 =====

    /**
     * 记录Token使用
     * @param agentId Agent ID
     * @param agentName Agent名称
     * @param inputTokens 输入Token数
     * @param outputTokens 输出Token数
     * @param userId 用户ID（可选）
     * @param projectId 项目ID（可选）
     */
    public void recordTokenUsage(String agentId, String agentName, long inputTokens, long outputTokens,
                                  Long userId, String projectId) {
        LocalDate today = LocalDate.now();

        // 查找或创建今日记录
        TokenUsage usage = tokenUsageRepository.findByUsageDateAndAgentId(today, agentId)
            .orElseGet(() -> {
                TokenUsage newUsage = new TokenUsage();
                newUsage.setUsageDate(today);
                newUsage.setAgentId(agentId);
                newUsage.setAgentName(agentName);
                newUsage.setUserId(userId);
                newUsage.setProjectId(projectId);
                return newUsage;
            });

        usage.addUsage(inputTokens, outputTokens);
        tokenUsageRepository.save(usage);

        log.debug("Recorded token usage for agent {}: input={}, output={}", agentId, inputTokens, outputTokens);

        // 检查是否触发配额告警
        checkTokenQuota(usage);
    }

    /**
     * 检查Token配额
     */
    private void checkTokenQuota(TokenUsage usage) {
        // 获取本月累计使用量
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate monthEnd = LocalDate.now();
        Long monthlyTotal = tokenUsageRepository.sumTotalTokensByDateRange(monthStart, monthEnd);

        if (monthlyTotal != null) {
            // 假设月度配额为100万tokens
            long monthlyQuota = 1000000L;
            double usagePercent = (double) monthlyTotal / monthlyQuota * 100;

            if (usagePercent >= 90) {
                alertService.checkAndTrigger("TOKEN_USAGE", usagePercent, usage.getAgentId(), usage.getAgentName(), null);
            }
        }
    }

    // ===== 统计查询 =====

    /**
     * 获取今日Token使用统计
     */
    public Map<String, Object> getTodayStats() {
        LocalDate today = LocalDate.now();
        return getDateStats(today);
    }

    /**
     * 获取指定日期的Token使用统计
     */
    public Map<String, Object> getDateStats(LocalDate date) {
        Map<String, Object> stats = new HashMap<>();

        List<TokenUsage> dayUsages = tokenUsageRepository.findByUsageDateBetweenOrderByUsageDateDesc(date, date);

        long totalTokens = dayUsages.stream().mapToLong(TokenUsage::getTotalTokens).sum();
        long inputTokens = dayUsages.stream().mapToLong(TokenUsage::getInputTokens).sum();
        long outputTokens = dayUsages.stream().mapToLong(TokenUsage::getOutputTokens).sum();
        int totalCalls = dayUsages.stream().mapToInt(TokenUsage::getCallCount).sum();
        double totalCost = dayUsages.stream().mapToDouble(TokenUsage::getEstimatedCost).sum();

        stats.put("date", date);
        stats.put("totalTokens", totalTokens);
        stats.put("inputTokens", inputTokens);
        stats.put("outputTokens", outputTokens);
        stats.put("totalCalls", totalCalls);
        stats.put("totalCost", Math.round(totalCost * 100.0) / 100.0);
        stats.put("agentCount", dayUsages.size());

        return stats;
    }

    /**
     * 获取日期范围内的Token使用统计
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 统计信息
     */
    public Map<String, Object> getDateRangeStats(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> stats = new HashMap<>();

        Long totalTokens = tokenUsageRepository.sumTotalTokensByDateRange(startDate, endDate);
        Double totalCost = tokenUsageRepository.sumCostByDateRange(startDate, endDate);

        stats.put("startDate", startDate);
        stats.put("endDate", endDate);
        stats.put("totalTokens", totalTokens != null ? totalTokens : 0);
        stats.put("totalCost", totalCost != null ? Math.round(totalCost * 100.0) / 100.0 : 0);

        // 按日统计
        List<Object[]> dailyStats = tokenUsageRepository.sumByDateRange(startDate, endDate);
        List<Map<String, Object>> dailyList = new ArrayList<>();
        for (Object[] row : dailyStats) {
            Map<String, Object> dayStat = new HashMap<>();
            dayStat.put("date", row[0]);
            dayStat.put("tokens", row[1]);
            dayStat.put("cost", row[2]);
            dailyList.add(dayStat);
        }
        stats.put("dailyStats", dailyList);

        // 按Agent统计
        List<Object[]> agentStats = tokenUsageRepository.sumByAgentAndDateRange(startDate, endDate);
        List<Map<String, Object>> agentList = new ArrayList<>();
        for (Object[] row : agentStats) {
            Map<String, Object> agentStat = new HashMap<>();
            agentStat.put("agentId", row[0]);
            agentStat.put("agentName", row[1]);
            agentStat.put("tokens", row[2]);
            agentStat.put("cost", row[3]);
            agentList.add(agentStat);
        }
        stats.put("agentStats", agentList);

        return stats;
    }

    /**
     * 获取最近7天的Token使用趋势
     */
    public Map<String, Object> getWeeklyTrend() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);
        return getDateRangeStats(startDate, endDate);
    }

    /**
     * 获取最近30天的Token使用趋势
     */
    public Map<String, Object> getMonthlyTrend() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(29);
        return getDateRangeStats(startDate, endDate);
    }

    /**
     * 获取Agent Token使用排名
     * @param days 查询天数
     * @return 排名列表
     */
    public List<Map<String, Object>> getAgentRanking(int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        List<Object[]> agentStats = tokenUsageRepository.sumByAgentAndDateRange(startDate, endDate);

        return agentStats.stream()
            .map(row -> {
                Map<String, Object> agent = new HashMap<>();
                agent.put("agentId", row[0]);
                agent.put("agentName", row[1]);
                agent.put("tokens", row[2]);
                agent.put("cost", row[3]);
                return agent;
            })
            .collect(Collectors.toList());
    }

    /**
     * 获取月度使用统计
     * @return 月度统计
     */
    public Map<String, Object> getMonthlyStats() {
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate monthEnd = LocalDate.now();

        Map<String, Object> stats = getDateRangeStats(monthStart, monthEnd);

        // 添加月度配额信息
        long monthlyQuota = 1000000L; // 假设100万tokens配额
        Long totalTokens = (Long) stats.get("totalTokens");
        double usagePercent = (double) (totalTokens != null ? totalTokens : 0) / monthlyQuota * 100;

        stats.put("monthlyQuota", monthlyQuota);
        stats.put("usagePercent", Math.round(usagePercent * 100.0) / 100.0);
        stats.put("remaining", monthlyQuota - (totalTokens != null ? totalTokens : 0));

        return stats;
    }

    /**
     * 获取Token使用趋势（按日）
     * @param days 天数
     * @return 趋势数据
     */
    public Map<String, Long> getTokenTrend(int days) {
        Map<String, Long> trend = new LinkedHashMap<>();
        LocalDate endDate = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = endDate.minusDays(i);
            List<TokenUsage> dayUsages = tokenUsageRepository.findByUsageDateBetweenOrderByUsageDateDesc(date, date);
            long totalTokens = dayUsages.stream().mapToLong(TokenUsage::getTotalTokens).sum();
            trend.put(date.toString(), totalTokens);
        }

        return trend;
    }
}
