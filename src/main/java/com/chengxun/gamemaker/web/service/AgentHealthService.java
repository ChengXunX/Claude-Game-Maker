package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.agent.BaseAgent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.web.entity.AgentHealth;
import com.chengxun.gamemaker.web.repository.AgentHealthRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent健康检查服务
 * 负责Agent健康指标收集、监控和自动恢复
 *
 * 主要功能：
 * - 定期收集Agent健康指标
 * - 检测不健康的Agent
 * - 自动重启失败的Agent
 * - 生成健康报告
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
@Transactional
public class AgentHealthService {

    private static final Logger log = LoggerFactory.getLogger(AgentHealthService.class);

    /** 最大连续错误次数，超过后自动重启 */
    private static final int MAX_CONSECUTIVE_ERRORS = 10;

    /** 响应时间阈值（毫秒），超过后标记为警告 */
    private static final long RESPONSE_TIME_WARNING_THRESHOLD = 3000;

    /** 响应时间阈值（毫秒），超过后标记为不健康 */
    private static final long RESPONSE_TIME_UNHEALTHY_THRESHOLD = 5000;

    @Autowired
    private AgentHealthRepository healthRepository;

    @Autowired
    private AgentManager agentManager;

    /** Agent启动时间 */
    private final Map<String, Long> agentStartTimes = new ConcurrentHashMap<>();

    /** Agent请求记录 */
    private final Map<String, List<RequestRecord>> requestRecords = new ConcurrentHashMap<>();

    /**
     * 记录Agent请求
     *
     * @param agentId Agent ID
     * @param responseTimeMs 响应时间（毫秒）
     * @param success 是否成功
     * @param errorMessage 错误信息（如果失败）
     */
    public void recordRequest(String agentId, long responseTimeMs, boolean success, String errorMessage) {
        // 获取或创建健康记录
        AgentHealth health = getOrCreateHealth(agentId);

        // 记录请求
        health.recordRequest(responseTimeMs, success);
        if (!success && errorMessage != null) {
            health.setLastErrorMessage(errorMessage);
        }

        // 保存记录
        healthRepository.save(health);

        // 记录到内存（使用 CopyOnWriteArrayList 保证线程安全）
        requestRecords.computeIfAbsent(agentId, k -> new java.util.concurrent.CopyOnWriteArrayList<>())
            .add(new RequestRecord(responseTimeMs, success, System.currentTimeMillis()));

        // 清理旧记录
        cleanOldRecords(agentId);

        log.debug("Request recorded for agent {}: {}ms, success={}", agentId, responseTimeMs, success);
    }

    /**
     * 记录任务完成
     *
     * @param agentId Agent ID
     * @param success 是否成功
     */
    public void recordTaskCompletion(String agentId, boolean success) {
        AgentHealth health = getOrCreateHealth(agentId);
        health.recordTaskCompletion(success);
        healthRepository.save(health);
    }

    /**
     * 获取Agent健康状态
     *
     * @param agentId Agent ID
     * @return 健康记录
     */
    public AgentHealth getAgentHealth(String agentId) {
        return healthRepository.findFirstByAgentIdOrderByCheckTimeDesc(agentId)
            .orElse(null);
    }

    /**
     * 获取所有Agent的健康状态
     *
     * @return 健康记录列表
     */
    public List<AgentHealth> getAllAgentHealth() {
        List<AgentHealth> result = new ArrayList<>();
        List<Agent> agents = agentManager.getAllAgents();

        for (Agent agent : agents) {
            AgentHealth health = getAgentHealth(agent.getId());
            if (health != null) {
                result.add(health);
            }
        }

        return result;
    }

    /**
     * 获取项目的所有 Agent 健康状态
     *
     * @param projectId 项目 ID
     * @return 健康记录列表
     */
    public List<AgentHealth> getProjectAgentHealth(String projectId) {
        List<AgentHealth> result = new ArrayList<>();
        List<Agent> agents = agentManager.getAgentsByProject(projectId);

        for (Agent agent : agents) {
            AgentHealth health = getAgentHealth(agent.getId());
            if (health != null) {
                result.add(health);
            }
        }

        return result;
    }

    /**
     * 获取项目的健康统计
     *
     * @param projectId 项目 ID
     * @return 统计数据
     */
    public Map<String, Object> getProjectHealthStatistics(String projectId) {
        Map<String, Object> stats = new HashMap<>();

        List<Object[]> statusCounts = healthRepository.countByHealthStatusAndProjectId(projectId);
        Map<String, Long> statusMap = new HashMap<>();
        for (Object[] row : statusCounts) {
            statusMap.put(((AgentHealth.HealthStatus) row[0]).name(), (Long) row[1]);
        }
        stats.put("statusCounts", statusMap);

        long totalAgents = agentManager.getAgentsByProject(projectId).size();
        long healthyAgents = statusMap.getOrDefault("HEALTHY", 0L);
        double healthRate = totalAgents > 0 ? (double) healthyAgents / totalAgents * 100 : 0;
        stats.put("healthRate", Math.round(healthRate * 100.0) / 100.0);
        stats.put("totalAgents", totalAgents);
        stats.put("projectId", projectId);

        return stats;
    }

    /**
     * 获取健康统计
     *
     * @return 统计数据
     */
    public Map<String, Object> getHealthStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 各状态统计
        List<Object[]> statusCounts = healthRepository.countByHealthStatus();
        Map<String, Long> statusMap = new HashMap<>();
        for (Object[] row : statusCounts) {
            statusMap.put(((AgentHealth.HealthStatus) row[0]).name(), (Long) row[1]);
        }
        stats.put("statusCounts", statusMap);

        // 需要重启的Agent
        List<AgentHealth> needsRestart = healthRepository.findAgentsNeedingRestart();
        stats.put("needsRestartCount", needsRestart.size());
        stats.put("needsRestartAgents", needsRestart.stream()
            .map(AgentHealth::getAgentId)
            .toList());

        // 响应缓慢的Agent
        List<AgentHealth> slowAgents = healthRepository.findSlowAgents();
        stats.put("slowAgentCount", slowAgents.size());
        stats.put("slowAgents", slowAgents.stream()
            .map(AgentHealth::getAgentId)
            .toList());

        // 总体健康率
        long totalAgents = agentManager.getAllAgents().size();
        long healthyAgents = statusMap.getOrDefault("HEALTHY", 0L);
        double healthRate = totalAgents > 0 ? (double) healthyAgents / totalAgents * 100 : 0;
        stats.put("healthRate", Math.round(healthRate * 100.0) / 100.0);
        stats.put("totalAgents", totalAgents);

        return stats;
    }

    /**
     * 检查并自动重启不健康的Agent
     * 每5分钟执行一次
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void checkAndRestartUnhealthyAgents() {
        log.info("Checking for unhealthy agents...");

        List<AgentHealth> needsRestart = healthRepository.findAgentsNeedingRestart();
        for (AgentHealth health : needsRestart) {
            try {
                log.warn("Agent {} needs restart (consecutive errors: {})",
                    health.getAgentId(), health.getConsecutiveErrors());
                restartAgent(health.getAgentId());
            } catch (Exception e) {
                log.error("Failed to restart agent {}: {}", health.getAgentId(), e.getMessage());
            }
        }
    }

    /**
     * 定期收集Agent健康指标
     * 每分钟执行一次
     */
    @Scheduled(fixedRate = 60000) // 1分钟
    public void collectHealthMetrics() {
        log.debug("Collecting agent health metrics...");

        List<Agent> agents = agentManager.getAllAgents();
        for (Agent agent : agents) {
            try {
                collectAgentMetrics(agent);
            } catch (Exception e) {
                log.error("Failed to collect metrics for agent {}: {}", agent.getId(), e.getMessage());
            }
        }
    }

    /**
     * 获取Agent健康历史
     *
     * @param agentId Agent ID
     * @param hours 查询最近N小时的数据
     * @return 健康记录列表
     */
    public List<AgentHealth> getAgentHealthHistory(String agentId, int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return healthRepository.findAgentHealthHistory(agentId, since);
    }

    /**
     * 重启Agent
     *
     * @param agentId Agent ID
     */
    public void restartAgent(String agentId) {
        Agent agent = agentManager.getAgent(agentId);
        if (agent == null) {
            log.warn("Agent not found for restart: {}", agentId);
            return;
        }

        log.info("Restarting agent: {}", agentId);

        try {
            // 停止Agent
            agent.stop();

            // 等待一段时间
            Thread.sleep(1000);

            // 重新启动Agent
            agent.start();

            // 记录重启
            AgentHealth health = getOrCreateHealth(agentId);
            health.setConsecutiveErrors(0);
            health.setHealthStatus(AgentHealth.HealthStatus.HEALTHY);
            healthRepository.save(health);

            // 更新启动时间
            agentStartTimes.put(agentId, System.currentTimeMillis());

            log.info("Agent restarted successfully: {}", agentId);

        } catch (Exception e) {
            log.error("Failed to restart agent {}: {}", agentId, e.getMessage());
            throw new RuntimeException("重启Agent失败: " + e.getMessage(), e);
        }
    }

    /**
     * 收集单个Agent的指标
     */
    private void collectAgentMetrics(Agent agent) {
        AgentHealth health = getOrCreateHealth(agent.getId());

        // 更新基本信息
        health.setAgentName(agent.getName());
        health.setAgentRole(agent.getRole());

        // 设置项目 ID
        if (agent instanceof BaseAgent baseAgent && baseAgent.getProjectId() != null) {
            health.setProjectId(baseAgent.getProjectId());
        }

        // 更新状态
        if (!agent.isAlive()) {
            health.setHealthStatus(AgentHealth.HealthStatus.OFFLINE);
        }

        // 计算运行时间
        Long startTime = agentStartTimes.get(agent.getId());
        if (startTime != null) {
            health.setUptimeSeconds((System.currentTimeMillis() - startTime) / 1000);
        }

        // 更新最后活动时间
        health.setLastActivityTime(LocalDateTime.now());

        // 保存
        healthRepository.save(health);
    }

    /**
     * 获取或创建健康记录
     */
    private AgentHealth getOrCreateHealth(String agentId) {
        return healthRepository.findFirstByAgentIdOrderByCheckTimeDesc(agentId)
            .orElseGet(() -> {
                AgentHealth health = new AgentHealth();
                health.setAgentId(agentId);
                health.setHealthStatus(AgentHealth.HealthStatus.HEALTHY);
                health.setCheckTime(LocalDateTime.now());

                Agent agent = agentManager.getAgent(agentId);
                if (agent != null) {
                    health.setAgentName(agent.getName());
                    health.setAgentRole(agent.getRole());
                }

                return health;
            });
    }

    /**
     * 清理旧的请求记录
     */
    private void cleanOldRecords(String agentId) {
        List<RequestRecord> records = requestRecords.get(agentId);
        if (records != null) {
            long oneHourAgo = System.currentTimeMillis() - 3600000;
            records.removeIf(r -> r.timestamp < oneHourAgo);
        }
    }

    /**
     * 请求记录内部类
     */
    private static class RequestRecord {
        final long responseTimeMs;
        final boolean success;
        final long timestamp;

        RequestRecord(long responseTimeMs, boolean success, long timestamp) {
            this.responseTimeMs = responseTimeMs;
            this.success = success;
            this.timestamp = timestamp;
        }
    }
}
