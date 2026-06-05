package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.AgentPerformance;
import com.chengxun.gamemaker.web.repository.AgentPerformanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent性能评估服务
 * 负责Agent性能数据的收集、计算和分析
 *
 * 主要功能：
 * - 记录任务执行结果
 * - 计算性能指标
 * - 生成评估报告
 * - 提供智能推荐
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
@Transactional
public class AgentPerformanceService {

    private static final Logger log = LoggerFactory.getLogger(AgentPerformanceService.class);

    @Autowired
    private AgentPerformanceRepository performanceRepository;

    /**
     * 获取或创建Agent性能记录
     * @param agentId Agent ID
     * @param agentName Agent名称
     * @param agentRole Agent角色
     * @return 性能记录
     */
    public AgentPerformance getOrCreatePerformance(String agentId, String agentName, String agentRole) {
        return performanceRepository.findByAgentId(agentId)
            .orElseGet(() -> {
                AgentPerformance performance = new AgentPerformance(agentId, agentName, agentRole);
                log.info("Created new performance record for agent: {} ({})", agentName, agentId);
                return performanceRepository.save(performance);
            });
    }

    /**
     * 记录任务完成
     * @param agentId Agent ID
     * @param agentName Agent名称
     * @param agentRole Agent角色
     * @param durationMs 任务耗时（毫秒）
     * @param qualityScore 质量评分（0-100，可选）
     */
    public void recordTaskCompletion(String agentId, String agentName, String agentRole,
                                     long durationMs, Double qualityScore) {
        AgentPerformance performance = getOrCreatePerformance(agentId, agentName, agentRole);
        performance.updateTaskStats(true, durationMs, qualityScore);
        performance.markTaskCompleted();
        performanceRepository.save(performance);

        log.info("Recorded task completion for agent {}: duration={}ms, quality={}",
            agentId, durationMs, qualityScore);
    }

    /**
     * 记录任务失败
     * @param agentId Agent ID
     * @param agentName Agent名称
     * @param agentRole Agent角色
     * @param durationMs 任务耗时（毫秒）
     */
    public void recordTaskFailure(String agentId, String agentName, String agentRole, long durationMs) {
        AgentPerformance performance = getOrCreatePerformance(agentId, agentName, agentRole);
        performance.updateTaskStats(false, durationMs, null);
        performance.markTaskCompleted();
        performanceRepository.save(performance);

        log.info("Recorded task failure for agent {}: duration={}ms", agentId, durationMs);
    }

    /**
     * 记录任务开始
     * @param agentId Agent ID
     * @param agentName Agent名称
     * @param agentRole Agent角色
     */
    public void recordTaskStart(String agentId, String agentName, String agentRole) {
        AgentPerformance performance = getOrCreatePerformance(agentId, agentName, agentRole);
        performance.markTaskStarted();
        performanceRepository.save(performance);

        log.debug("Recorded task start for agent {}", agentId);
    }

    /**
     * 更新Agent负载
     * @param agentId Agent ID
     * @param load 负载值（0-100）
     */
    public void updateAgentLoad(String agentId, int load) {
        performanceRepository.findByAgentId(agentId).ifPresent(performance -> {
            performance.updateLoad(load);
            performanceRepository.save(performance);
            log.debug("Updated load for agent {}: {}%", agentId, load);
        });
    }

    /**
     * 获取Agent性能详情
     * @param agentId Agent ID
     * @return 性能记录
     */
    public AgentPerformance getPerformance(String agentId) {
        return performanceRepository.findByAgentId(agentId).orElse(null);
    }

    /**
     * 获取所有Agent性能记录（按综合评分排序）
     * @return 性能记录列表
     */
    public List<AgentPerformance> getAllPerformances() {
        return performanceRepository.findAllByOrderByOverallScoreDesc();
    }

    /**
     * 按角色获取Agent性能记录
     * @param role Agent角色
     * @return 性能记录列表
     */
    public List<AgentPerformance> getPerformancesByRole(String role) {
        return performanceRepository.findByAgentRole(role);
    }

    /**
     * 获取项目的 Agent 性能记录（按综合评分排序）
     * @param projectId 项目 ID
     * @return 性能记录列表
     */
    public List<AgentPerformance> getProjectPerformances(String projectId) {
        return performanceRepository.findByProjectIdOrderByOverallScoreDesc(projectId);
    }

    /**
     * 获取项目的最佳 Agent
     * @param projectId 项目 ID
     * @param limit 限制数量
     * @return 性能记录列表
     */
    public List<AgentPerformance> getProjectTopAgents(String projectId, int limit) {
        return performanceRepository.findByProjectIdOrderByOverallScoreDesc(projectId)
            .stream()
            .limit(limit)
            .toList();
    }

    /**
     * 获取或创建项目级 Agent 性能记录
     */
    public AgentPerformance getOrCreatePerformance(String projectId, String agentId, String agentName, String agentRole) {
        if (projectId != null) {
            return performanceRepository.findByProjectIdAndAgentId(projectId, agentId)
                .orElseGet(() -> {
                    AgentPerformance performance = new AgentPerformance(projectId, agentId, agentName, agentRole);
                    log.info("Created new performance record for agent: {} in project: {}", agentId, projectId);
                    return performanceRepository.save(performance);
                });
        }
        return getOrCreatePerformance(agentId, agentName, agentRole);
    }

    /**
     * 获取最佳Agent（按综合评分）
     * @param limit 限制数量
     * @return 性能记录列表
     */
    public List<AgentPerformance> getTopAgents(int limit) {
        return performanceRepository.findAllByOrderByOverallScoreDesc()
            .stream()
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * 获取最可靠Agent（按可靠性评分）
     * @param limit 限制数量
     * @return 性能记录列表
     */
    public List<AgentPerformance> getMostReliableAgents(int limit) {
        return performanceRepository.findAllByOrderByReliabilityScoreDesc()
            .stream()
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * 获取最高效Agent（按效率评分）
     * @param limit 限制数量
     * @return 性能记录列表
     */
    public List<AgentPerformance> getMostEfficientAgents(int limit) {
        return performanceRepository.findAllByOrderByEfficiencyScoreDesc()
            .stream()
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * 获取可用Agent（负载低且评分高）
     * @param maxLoad 最大负载（默认70）
     * @param minScore 最小评分（默认60）
     * @return 性能记录列表
     */
    public List<AgentPerformance> getAvailableAgents(Integer maxLoad, Double minScore) {
        int load = maxLoad != null ? maxLoad : 70;
        double score = minScore != null ? minScore : 60.0;
        return performanceRepository.findAvailableAgents(load, score);
    }

    /**
     * 根据角色推荐最佳Agent
     * 用于智能任务分配
     * @param role Agent角色
     * @param maxLoad 最大负载
     * @return 推荐的Agent列表
     */
    public List<AgentPerformance> recommendAgentsByRole(String role, Integer maxLoad) {
        int load = maxLoad != null ? maxLoad : 80;
        return performanceRepository.findBestAgentsByRole(role, load);
    }

    /**
     * 智能任务分配推荐
     * 根据任务类型、复杂度和Agent能力进行推荐
     * @param taskType 任务类型
     * @param complexity 复杂度（1-5）
     * @return 推荐的Agent列表
     */
    public List<AgentPerformance> recommendAgentsForTask(String taskType, int complexity) {
        // 根据任务类型确定所需角色
        String requiredRole = determineRoleForTask(taskType);

        // 根据复杂度确定最低评分要求
        double minScore = switch (complexity) {
            case 1 -> 50.0;  // 简单任务
            case 2 -> 60.0;  // 一般任务
            case 3 -> 70.0;  // 中等任务
            case 4 -> 80.0;  // 复杂任务
            case 5 -> 85.0;  // 非常复杂任务
            default -> 60.0;
        };

        // 根据复杂度确定最大负载要求
        int maxLoad = switch (complexity) {
            case 1 -> 90;  // 简单任务可以分配给高负载Agent
            case 2 -> 80;
            case 3 -> 70;
            case 4 -> 60;
            case 5 -> 50;  // 复杂任务需要低负载Agent
            default -> 70;
        };

        List<AgentPerformance> candidates;

        if (requiredRole != null) {
            // 优先分配给指定角色的Agent
            candidates = performanceRepository.findBestAgentsByRole(requiredRole, maxLoad);

            // 如果指定角色没有可用Agent，考虑其他角色
            if (candidates.isEmpty()) {
                candidates = performanceRepository.findAvailableAgents(maxLoad, minScore);
            }
        } else {
            // 没有特定角色要求，选择最佳可用Agent
            candidates = performanceRepository.findAvailableAgents(maxLoad, minScore);
        }

        // 过滤出评分满足要求的Agent
        return candidates.stream()
            .filter(p -> p.getOverallScore() >= minScore)
            .collect(Collectors.toList());
    }

    /**
     * 根据任务类型确定所需角色
     */
    private String determineRoleForTask(String taskType) {
        if (taskType == null) return null;

        return switch (taskType.toLowerCase()) {
            case "server-dev", "backend", "api", "database" -> "server-dev";
            case "system-planner", "architecture", "design" -> "system-planner";
            case "numerical-planner", "balance", "数值" -> "numerical-planner";
            case "git-commit", "version", "deploy" -> "git-commit";
            case "producer", "management", "coordination" -> "producer";
            default -> null;
        };
    }

    /**
     * 获取性能统计摘要
     * @return 统计信息Map
     */
    public Map<String, Object> getPerformanceSummary() {
        Map<String, Object> summary = new HashMap<>();

        List<AgentPerformance> allPerformances = performanceRepository.findAll();

        // 总体统计
        summary.put("totalAgents", allPerformances.size());
        summary.put("totalTasks", allPerformances.stream().mapToInt(AgentPerformance::getTotalTasks).sum());
        summary.put("completedTasks", allPerformances.stream().mapToInt(AgentPerformance::getCompletedTasks).sum());
        summary.put("failedTasks", allPerformances.stream().mapToInt(AgentPerformance::getFailedTasks).sum());

        // 平均指标
        double avgScore = allPerformances.stream()
            .mapToDouble(AgentPerformance::getOverallScore)
            .average()
            .orElse(0.0);
        summary.put("avgOverallScore", Math.round(avgScore * 100.0) / 100.0);

        double avgCompletionRate = allPerformances.stream()
            .mapToDouble(AgentPerformance::getCompletionRate)
            .average()
            .orElse(0.0);
        summary.put("avgCompletionRate", Math.round(avgCompletionRate * 100.0) / 100.0);

        // 等级分布
        Map<String, Long> gradeDistribution = allPerformances.stream()
            .collect(Collectors.groupingBy(
                AgentPerformance::getPerformanceGrade,
                Collectors.counting()
            ));
        summary.put("gradeDistribution", gradeDistribution);

        // 角色统计
        Map<String, Long> roleDistribution = allPerformances.stream()
            .collect(Collectors.groupingBy(
                AgentPerformance::getAgentRole,
                Collectors.counting()
            ));
        summary.put("roleDistribution", roleDistribution);

        // 负载统计
        int highLoadCount = (int) allPerformances.stream()
            .filter(p -> p.getCurrentLoad() >= 80)
            .count();
        summary.put("highLoadAgents", highLoadCount);

        // 需要关注的Agent
        List<Map<String, Object>> attentionNeeded = allPerformances.stream()
            .filter(p -> p.getOverallScore() < 60 || p.getCurrentLoad() > 80)
            .map(p -> {
                Map<String, Object> agentInfo = new HashMap<>();
                agentInfo.put("agentId", p.getAgentId());
                agentInfo.put("agentName", p.getAgentName());
                agentInfo.put("overallScore", p.getOverallScore());
                agentInfo.put("currentLoad", p.getCurrentLoad());
                agentInfo.put("status", p.getStatusDescription());
                return agentInfo;
            })
            .collect(Collectors.toList());
        summary.put("attentionNeeded", attentionNeeded);

        return summary;
    }

    /**
     * 获取角色性能统计
     * @return 角色统计Map
     */
    public Map<String, Map<String, Object>> getRolePerformanceStats() {
        Map<String, Map<String, Object>> roleStats = new HashMap<>();

        List<Object[]> roleCounts = performanceRepository.countByRole();
        for (Object[] row : roleCounts) {
            String role = (String) row[0];
            Long count = (Long) row[1];

            List<AgentPerformance> rolePerformances = performanceRepository.findByAgentRole(role);

            Map<String, Object> stats = new HashMap<>();
            stats.put("agentCount", count);
            stats.put("avgScore", rolePerformances.stream()
                .mapToDouble(AgentPerformance::getOverallScore)
                .average()
                .orElse(0.0));
            stats.put("avgCompletionRate", rolePerformances.stream()
                .mapToDouble(AgentPerformance::getCompletionRate)
                .average()
                .orElse(0.0));
            stats.put("totalTasks", rolePerformances.stream()
                .mapToInt(AgentPerformance::getTotalTasks)
                .sum());

            roleStats.put(role, stats);
        }

        return roleStats;
    }

    /**
     * 触发性能评估更新
     * 重新计算所有Agent的综合评分
     */
    public void triggerEvaluation() {
        List<AgentPerformance> allPerformances = performanceRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (AgentPerformance performance : allPerformances) {
            performance.recalculateScores();
            performance.setLastEvaluatedAt(now);
            performanceRepository.save(performance);
        }

        log.info("Triggered performance evaluation for {} agents", allPerformances.size());
    }

    /**
     * 删除性能记录
     * @param agentId Agent ID
     */
    public void deletePerformance(String agentId) {
        performanceRepository.findByAgentId(agentId).ifPresent(performance -> {
            performanceRepository.delete(performance);
            log.info("Deleted performance record for agent: {}", agentId);
        });
    }

    /**
     * 重置Agent性能数据
     * @param agentId Agent ID
     */
    public void resetPerformance(String agentId) {
        performanceRepository.findByAgentId(agentId).ifPresent(performance -> {
            performance.setTotalTasks(0);
            performance.setCompletedTasks(0);
            performance.setFailedTasks(0);
            performance.setInProgressTasks(0);
            performance.setAvgCompletionTimeMs(0L);
            performance.setMinCompletionTimeMs(0L);
            performance.setMaxCompletionTimeMs(0L);
            performance.setAvgQualityScore(0.0);
            performance.setMaxQualityScore(0.0);
            performance.setMinQualityScore(0.0);
            performance.setCurrentLoad(0);
            performance.setAvgLoad(0.0);
            performance.setOverallScore(0.0);
            performance.setReliabilityScore(0.0);
            performance.setEfficiencyScore(0.0);
            performance.setUpdatedAt(LocalDateTime.now());

            performanceRepository.save(performance);
            log.info("Reset performance data for agent: {}", agentId);
        });
    }
}
