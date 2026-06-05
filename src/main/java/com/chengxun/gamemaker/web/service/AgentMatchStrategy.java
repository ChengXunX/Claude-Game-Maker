package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.web.entity.AgentPerformance;
import com.chengxun.gamemaker.web.repository.AgentPerformanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent匹配策略服务
 * 综合考虑能力匹配、负载均衡、历史表现三个维度，为工作流步骤选择最优Agent
 *
 * 评分公式：
 * 综合分 = 能力匹配(40%) + 负载均衡(30%) + 历史表现(30%)
 *
 * - 能力匹配：Agent角色与步骤要求的匹配度，完全匹配100分，不匹配0分
 * - 负载均衡：当前任务数越少分越高，空闲100分，满载0分
 * - 历史表现：从agent_performance表查询，综合评分直接使用
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class AgentMatchStrategy {

    private static final Logger log = LoggerFactory.getLogger(AgentMatchStrategy.class);

    /** 能力匹配权重 */
    private static final double CAPABILITY_WEIGHT = 0.4;

    /** 负载均衡权重 */
    private static final double LOAD_WEIGHT = 0.3;

    /** 历史表现权重 */
    private static final double PERFORMANCE_WEIGHT = 0.3;

    /** Agent最大并发任务数 */
    private static final int MAX_TASKS_PER_AGENT = 5;

    @Autowired
    private AgentManager agentManager;

    @Autowired
    private AgentPerformanceRepository performanceRepository;

    /**
     * 为指定步骤选择最优Agent
     *
     * @param requiredRole 步骤要求的Agent角色
     * @param projectId 项目ID（用于限定项目内Agent）
     * @return 最优Agent，如果没有可用Agent返回null
     */
    public Agent selectBestAgent(String requiredRole, String projectId) {
        List<Agent> candidates = getCandidates(requiredRole, projectId);
        if (candidates.isEmpty()) {
            log.warn("没有可用的Agent - 角色: {}, 项目: {}", requiredRole, projectId);
            return null;
        }

        // 获取所有候选Agent的性能数据
        Map<String, AgentPerformance> perfMap = getPerformanceMap(candidates);

        // 计算每个候选Agent的综合评分
        Agent bestAgent = null;
        double bestScore = -1;

        for (Agent agent : candidates) {
            double score = calculateScore(agent, requiredRole, perfMap);
            log.debug("Agent评分 - {}: {:.2f}", agent.getId(), score);
            if (score > bestScore) {
                bestScore = score;
                bestAgent = agent;
            }
        }

        if (bestAgent != null) {
            log.info("最优Agent选择 - 角色: {}, Agent: {}, 评分: {:.2f}", requiredRole, bestAgent.getId(), bestScore);
        }
        return bestAgent;
    }

    /**
     * 获取候选Agent列表（角色匹配且存活且未满载）
     */
    private List<Agent> getCandidates(String requiredRole, String projectId) {
        List<Agent> agents;
        if (projectId != null && !projectId.isEmpty()) {
            agents = agentManager.getAgentsByProject(projectId);
        } else {
            agents = agentManager.getAllAgents();
        }

        return agents.stream()
            .filter(a -> requiredRole.equals(a.getRole()))
            .filter(Agent::isAlive)
            .filter(a -> !isFullLoad(a))
            .collect(Collectors.toList());
    }

    /**
     * 检查Agent是否满载
     */
    private boolean isFullLoad(Agent agent) {
        List<?> tasks = agent.getTasks();
        return tasks != null && tasks.size() >= MAX_TASKS_PER_AGENT;
    }

    /**
     * 获取Agent性能数据Map
     */
    private Map<String, AgentPerformance> getPerformanceMap(List<Agent> agents) {
        Map<String, AgentPerformance> perfMap = new HashMap<>();
        for (Agent agent : agents) {
            performanceRepository.findByAgentId(agent.getId())
                .ifPresent(perf -> perfMap.put(agent.getId(), perf));
        }
        return perfMap;
    }

    /**
     * 计算Agent综合评分
     *
     * @param agent Agent实例
     * @param requiredRole 要求的角色
     * @param perfMap 性能数据Map
     * @return 综合评分（0-100）
     */
    private double calculateScore(Agent agent, String requiredRole, Map<String, AgentPerformance> perfMap) {
        // 1. 能力匹配分（角色完全匹配100分）
        double capabilityScore = requiredRole.equals(agent.getRole()) ? 100.0 : 0.0;

        // 2. 负载均衡分（当前任务越少分越高）
        double loadScore = calculateLoadScore(agent);

        // 3. 历史表现分
        double perfScore = calculatePerformanceScore(agent.getId(), perfMap);

        return capabilityScore * CAPABILITY_WEIGHT
             + loadScore * LOAD_WEIGHT
             + perfScore * PERFORMANCE_WEIGHT;
    }

    /**
     * 计算负载均衡分
     * 空闲=100分，满载=0分，线性递减
     */
    private double calculateLoadScore(Agent agent) {
        List<?> tasks = agent.getTasks();
        int currentTasks = tasks != null ? tasks.size() : 0;
        return Math.max(0, 100.0 * (1.0 - (double) currentTasks / MAX_TASKS_PER_AGENT));
    }

    /**
     * 计算历史表现分
     * 如果没有历史数据，返回默认50分
     */
    private double calculatePerformanceScore(String agentId, Map<String, AgentPerformance> perfMap) {
        AgentPerformance perf = perfMap.get(agentId);
        if (perf == null || perf.getTotalTasks() == 0) {
            return 50.0; // 新Agent默认中等分
        }
        return perf.getOverallScore();
    }

    /**
     * 获取Agent评分详情（用于调试和展示）
     *
     * @param requiredRole 步骤要求的Agent角色
     * @param projectId 项目ID
     * @return 按评分排序的Agent评分列表
     */
    public List<Map<String, Object>> getAgentScores(String requiredRole, String projectId) {
        List<Agent> candidates = getCandidates(requiredRole, projectId);
        Map<String, AgentPerformance> perfMap = getPerformanceMap(candidates);

        List<Map<String, Object>> scores = new ArrayList<>();
        for (Agent agent : candidates) {
            double capabilityScore = requiredRole.equals(agent.getRole()) ? 100.0 : 0.0;
            double loadScore = calculateLoadScore(agent);
            double perfScore = calculatePerformanceScore(agent.getId(), perfMap);
            double totalScore = capabilityScore * CAPABILITY_WEIGHT
                              + loadScore * LOAD_WEIGHT
                              + perfScore * PERFORMANCE_WEIGHT;

            Map<String, Object> info = new LinkedHashMap<>();
            info.put("agentId", agent.getId());
            info.put("agentName", agent.getName());
            info.put("role", agent.getRole());
            info.put("capabilityScore", Math.round(capabilityScore * 100.0) / 100.0);
            info.put("loadScore", Math.round(loadScore * 100.0) / 100.0);
            info.put("performanceScore", Math.round(perfScore * 100.0) / 100.0);
            info.put("totalScore", Math.round(totalScore * 100.0) / 100.0);
            info.put("currentTasks", agent.getTasks() != null ? agent.getTasks().size() : 0);
            info.put("alive", agent.isAlive());
            info.put("busy", agent.isBusy());
            scores.add(info);
        }

        // 按总分降序排序
        scores.sort((a, b) -> Double.compare((double) b.get("totalScore"), (double) a.get("totalScore")));
        return scores;
    }
}
