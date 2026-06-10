package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.model.GameProject.*;
import com.chengxun.gamemaker.web.entity.DismissalRequest;
import com.chengxun.gamemaker.web.entity.PerformanceReview;
import com.chengxun.gamemaker.web.repository.DismissalRequestRepository;
import com.chengxun.gamemaker.web.repository.PerformanceReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 智能调度器
 * 基于 Agent 能力、负载、历史表现进行智能任务分配
 *
 * 核心能力：
 * 1. 能力匹配 - 根据任务特性匹配最合适的 Agent
 * 2. 负载均衡 - 避免单个 Agent 过载
 * 3. 优先级调度 - 重要任务优先处理
 * 4. 依赖管理 - 自动处理任务间依赖
 * 5. 故障转移 - Agent 故障时自动重新分配
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class IntelligentScheduler {

    private static final Logger log = LoggerFactory.getLogger(IntelligentScheduler.class);

    private final AgentManager agentManager;
    private final ProjectManager projectManager;
    private final KnowledgeEvolutionService knowledgeService;

    @Autowired(required = false)
    private PerformanceReviewRepository performanceReviewRepository;

    @Autowired(required = false)
    private DismissalRequestRepository dismissalRequestRepository;

    /** Agent 负载追踪 */
    private final ConcurrentHashMap<String, AgentLoad> agentLoadMap = new ConcurrentHashMap<>();

    /** 任务分配历史（用于学习） */
    private final List<AssignmentHistory> assignmentHistory = new ArrayList<>();

    /** Agent 能力评分（基于历史表现） */
    private final ConcurrentHashMap<String, Map<String, Double>> agentCapabilityScores = new ConcurrentHashMap<>();

    /** Agent 绩效评分缓存（从绩效系统同步） */
    private final ConcurrentHashMap<String, Double> agentPerformanceScores = new ConcurrentHashMap<>();

    public IntelligentScheduler(AgentManager agentManager, ProjectManager projectManager,
                                 KnowledgeEvolutionService knowledgeService) {
        this.agentManager = agentManager;
        this.projectManager = projectManager;
        this.knowledgeService = knowledgeService;
    }

    /**
     * Agent 负载信息
     */
    public static class AgentLoad {
        String agentId;
        int activeTasks;
        int completedTasks;
        int failedTasks;
        long totalBusyTimeMs;
        LocalDateTime lastActiveTime;
        double loadScore; // 0-100，越高越忙

        public AgentLoad(String agentId) {
            this.agentId = agentId;
            this.activeTasks = 0;
            this.completedTasks = 0;
            this.failedTasks = 0;
            this.totalBusyTimeMs = 0;
            this.lastActiveTime = LocalDateTime.now();
            this.loadScore = 0;
        }

        void updateLoadScore() {
            // 负载评分 = 活跃任务数 * 30 + 失败率 * 20 + 忙碌时间因子 * 50
            double failureRate = (completedTasks + failedTasks) > 0
                ? (double) failedTasks / (completedTasks + failedTasks) : 0;
            double busyFactor = Math.min(1.0, totalBusyTimeMs / 3600000.0); // 1小时为满

            this.loadScore = Math.min(100,
                activeTasks * 30 + failureRate * 20 + busyFactor * 50);
        }
    }

    /**
     * 任务分配历史
     */
    static class AssignmentHistory {
        String agentId;
        String milestoneId;
        String taskType;
        boolean success;
        long durationMs;
        LocalDateTime assignedAt;
        LocalDateTime completedAt;
    }

    /**
     * 智能调度结果
     */
    public static class ScheduleResult {
        boolean scheduled;
        String agentId;
        String reason;
        double confidence; // 0-1，调度置信度

        public ScheduleResult(boolean scheduled, String agentId, String reason, double confidence) {
            this.scheduled = scheduled;
            this.agentId = agentId;
            this.reason = reason;
            this.confidence = confidence;
        }

        public static ScheduleResult success(String agentId, String reason, double confidence) {
            return new ScheduleResult(true, agentId, reason, confidence);
        }

        public static ScheduleResult failure(String reason) {
            return new ScheduleResult(false, null, reason, 0);
        }
    }

    /**
     * 智能调度里程碑
     * 综合考虑 Agent 能力、负载、历史表现进行调度
     *
     * @param projectId 项目 ID
     * @param milestone 里程碑
     * @return 调度结果
     */
    public ScheduleResult scheduleMilestone(String projectId, GoalMilestone milestone) {
        String role = milestone.getAssignedAgentRole();

        // 1. 获取该角色的所有可用 Agent
        List<Agent> availableAgents = getAvailableAgents(projectId, role);
        if (availableAgents.isEmpty()) {
            return ScheduleResult.failure("没有可用的 " + role + " Agent");
        }

        // 2. 获取历史经验（如果有）
        String knowledgeQuery = milestone.getTitle() + " " + milestone.getDescription();
        String historicalAdvice = "";
        if (knowledgeService != null) {
            historicalAdvice = knowledgeService.queryRelevantKnowledge(knowledgeQuery, role);
        }

        // 3. 为每个 Agent 计算调度评分
        Map<Agent, Double> scores = new LinkedHashMap<>();
        for (Agent agent : availableAgents) {
            double score = calculateAgentScore(agent, milestone, historicalAdvice);
            scores.put(agent, score);
        }

        // 4. 选择评分最高的 Agent
        Map.Entry<Agent, Double> best = scores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .orElse(null);

        if (best == null || best.getValue() < 20) {
            return ScheduleResult.failure("没有合适的 Agent，最高评分: " +
                (best != null ? String.format("%.1f", best.getValue()) : "0"));
        }

        Agent selectedAgent = best.getKey();
        double confidence = best.getValue() / 100.0;

        log.info("智能调度: 里程碑 [{}] -> Agent {} (评分: {:.1f}, 置信度: {:.2f})",
            milestone.getTitle(), selectedAgent.getName(), best.getValue(), confidence);

        return ScheduleResult.success(selectedAgent.getId(),
            String.format("基于能力匹配(%.0f%%)和负载均衡(%.0f%%)",
                getCapabilityScore(selectedAgent, role),
                100 - getAgentLoad(selectedAgent.getId()).loadScore),
            confidence);
    }

    /**
     * 计算 Agent 对特定里程碑的调度评分
     *
     * 评分维度（权重总和 = 100%）：
     * 1. 能力匹配 (25%) - Agent 对该角色的胜任能力
     * 2. 绩效评分 (30%) - 制作人给出的历史绩效评分
     * 3. 负载均衡 (20%) - 当前负载状态
     * 4. 历史表现 (15%) - 任务完成率
     * 5. 可用性 (5%) - Agent 是否在线
     * 6. 解雇风险惩罚 (扣分项) - 连续低绩效、有待审批解雇申请时降权
     */
    private double calculateAgentScore(Agent agent, GoalMilestone milestone, String historicalAdvice) {
        double score = 0;

        // 1. 能力匹配评分 (25%)
        double capabilityScore = getCapabilityScore(agent, milestone.getAssignedAgentRole());
        score += capabilityScore * 0.25;

        // 2. 绩效评分 (30%) - 从绩效系统获取
        double performanceScore = getPerformanceScore(agent.getId());
        score += performanceScore * 0.30;

        // 3. 负载均衡评分 (20%)
        AgentLoad load = getAgentLoad(agent.getId());
        double loadScore = 100 - load.loadScore; // 负载越低评分越高
        score += loadScore * 0.20;

        // 4. 历史表现评分 (15%)
        double historyScore = getHistoryScore(agent.getId(), milestone.getAssignedAgentRole());
        score += historyScore * 0.15;

        // 5. 可用性评分 (5%)
        double availabilityScore = agent.isAlive() ? (agent.isBusy() ? 30 : 100) : 0;
        score += availabilityScore * 0.05;

        // 6. 解雇风险惩罚（扣分项）
        double dismissalPenalty = getDismissalRiskPenalty(agent.getId());
        score = Math.max(0, score - dismissalPenalty);

        log.debug("Agent {} 评分详情: 能力={:.1f}, 绩效={:.1f}, 负载={:.1f}, 历史={:.1f}, 可用={}, 解雇惩罚={}, 总分={:.1f}",
            agent.getName(), capabilityScore, performanceScore, loadScore, historyScore,
            availabilityScore, dismissalPenalty, score);

        return score;
    }

    /**
     * 获取 Agent 绩效评分
     * 从绩效系统获取最新绩效评分，如果无记录则返回默认值
     */
    private double getPerformanceScore(String agentId) {
        // 先检查缓存
        if (agentPerformanceScores.containsKey(agentId)) {
            return agentPerformanceScores.get(agentId);
        }

        // 从绩效系统获取
        if (performanceReviewRepository != null) {
            try {
                List<PerformanceReview> reviews = performanceReviewRepository.findByAgentIdOrderByCreatedAtDesc(agentId);
                if (!reviews.isEmpty()) {
                    // 取最近一次评审的综合评分
                    PerformanceReview latestReview = reviews.get(0);
                    Integer overallScore = latestReview.getOverallScore();
                    if (overallScore != null) {
                        double score = overallScore.doubleValue();
                        agentPerformanceScores.put(agentId, score);
                        log.debug("从绩效系统获取 Agent {} 评分: {}", agentId, score);
                        return score;
                    }
                }
            } catch (Exception e) {
                log.warn("获取 Agent {} 绩效评分失败: {}", agentId, e.getMessage());
            }
        }

        // 默认评分：70（中等偏上）
        double defaultScore = 70.0;
        agentPerformanceScores.put(agentId, defaultScore);
        return defaultScore;
    }

    /**
     * 同步绩效评分
     * 当绩效评审完成时调用，更新缓存中的评分
     */
    public void syncPerformanceScore(String agentId, double score) {
        agentPerformanceScores.put(agentId, score);
        log.info("同步 Agent {} 绩效评分: {}", agentId, score);
    }

    /**
     * 获取 Agent 绩效评分（公开方法，供前端展示）
     */
    public double getAgentPerformanceScore(String agentId) {
        return getPerformanceScore(agentId);
    }

    /**
     * 获取 Agent 能力评分
     */
    private double getCapabilityScore(Agent agent, String role) {
        Map<String, Double> scores = agentCapabilityScores.get(agent.getId());
        if (scores == null || scores.isEmpty()) {
            return 50; // 默认中等评分
        }
        return scores.getOrDefault(role, 50.0);
    }

    /**
     * 获取 Agent 历史表现评分
     */
    private double getHistoryScore(String agentId, String role) {
        long completed = assignmentHistory.stream()
            .filter(h -> h.agentId.equals(agentId) && h.success)
            .count();
        long total = assignmentHistory.stream()
            .filter(h -> h.agentId.equals(agentId))
            .count();

        if (total == 0) return 50; // 无历史记录，默认中等

        return (double) completed / total * 100;
    }

    /**
     * 获取 Agent 负载信息
     */
    private AgentLoad getAgentLoad(String agentId) {
        return agentLoadMap.computeIfAbsent(agentId, AgentLoad::new);
    }

    /**
     * 获取可用 Agent 列表
     * 过滤掉已死亡、有待解雇申请或已被解雇执行的 Agent
     */
    private List<Agent> getAvailableAgents(String projectId, String role) {
        // 先查找项目内的 Agent
        List<Agent> projectAgents = agentManager.getAgentsByProject(projectId).stream()
            .filter(a -> role.equals(a.getRole()))
            .filter(Agent::isAlive)
            .filter(a -> !isAgentDismissedOrPending(a.getId()))
            .collect(Collectors.toList());

        if (!projectAgents.isEmpty()) {
            return projectAgents;
        }

        // 如果项目内没有，查找全局 Agent
        return agentManager.getAllAgents().stream()
            .filter(a -> role.equals(a.getRole()))
            .filter(Agent::isAlive)
            .filter(a -> !isAgentDismissedOrPending(a.getId()))
            .collect(Collectors.toList());
    }

    /**
     * 检查 Agent 是否已被解雇或有待审批的解雇申请
     * 有解雇申请（PENDING/APPROVED）或已执行解雇的 Agent 不应被调度
     *
     * @param agentId Agent ID
     * @return true 表示该 Agent 不应被调度
     */
    private boolean isAgentDismissedOrPending(String agentId) {
        if (dismissalRequestRepository == null) return false;

        try {
            List<DismissalRequest> requests = dismissalRequestRepository.findByAgentIdOrderByCreatedAtDesc(agentId);
            if (requests.isEmpty()) return false;

            // 检查是否有未完结的解雇申请（PENDING 或 APPROVED 但未执行）
            for (DismissalRequest req : requests) {
                String status = req.getStatus();
                // PENDING: 待审批，不应调度
                // APPROVED: 已批准待执行，不应调度
                if ("PENDING".equals(status) || "APPROVED".equals(status)) {
                    log.info("Agent {} 有未完结的解雇申请 ({}), 跳过调度", agentId, status);
                    return true;
                }
                // EXECUTED: 已执行解雇，不应调度
                if ("EXECUTED".equals(status)) {
                    log.info("Agent {} 已被解雇, 跳过调度", agentId);
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("检查 Agent {} 解雇状态失败: {}", agentId, e.getMessage());
        }

        return false;
    }

    /**
     * 获取 Agent 解雇风险惩罚分数
     * 连续低绩效期数越多，惩罚越大
     *
     * @param agentId Agent ID
     * @return 惩罚分数 (0-50)，越高表示惩罚越重
     */
    private double getDismissalRiskPenalty(String agentId) {
        if (dismissalRequestRepository == null) return 0;

        try {
            List<DismissalRequest> requests = dismissalRequestRepository.findByAgentIdOrderByCreatedAtDesc(agentId);
            if (requests.isEmpty()) return 0;

            for (DismissalRequest req : requests) {
                // 有 PENDING 或 APPROVED 的解雇申请，施加惩罚
                String status = req.getStatus();
                if ("PENDING".equals(status) || "APPROVED".equals(status)) {
                    int consecutiveLowPeriods = req.getConsecutiveLowScorePeriods() != null
                        ? req.getConsecutiveLowScorePeriods() : 0;
                    int warningCount = req.getWarningCount() != null ? req.getWarningCount() : 0;

                    // 惩罚 = 连续低分期数 * 10 + 警告次数 * 5，上限 50
                    double penalty = Math.min(50, consecutiveLowPeriods * 10 + warningCount * 5);
                    if (penalty > 0) {
                        log.debug("Agent {} 解雇风险惩罚: {} (连续低分{}期, 警告{}次)",
                            agentId, penalty, consecutiveLowPeriods, warningCount);
                    }
                    return penalty;
                }
            }
        } catch (Exception e) {
            log.warn("获取 Agent {} 解雇风险惩罚失败: {}", agentId, e.getMessage());
        }

        return 0;
    }

    /**
     * 更新 Agent 负载
     */
    public void updateAgentLoad(String agentId, boolean busy) {
        AgentLoad load = getAgentLoad(agentId);
        if (busy) {
            load.activeTasks++;
        } else {
            load.activeTasks = Math.max(0, load.activeTasks - 1);
        }
        load.lastActiveTime = LocalDateTime.now();
        load.updateLoadScore();
    }

    /**
     * 记录任务完成
     */
    public void recordTaskCompletion(String agentId, String milestoneId, boolean success, long durationMs) {
        // 更新负载
        AgentLoad load = getAgentLoad(agentId);
        if (success) {
            load.completedTasks++;
        } else {
            load.failedTasks++;
        }
        load.totalBusyTimeMs += durationMs;
        load.updateLoadScore();

        // 记录历史
        AssignmentHistory history = new AssignmentHistory();
        history.agentId = agentId;
        history.milestoneId = milestoneId;
        history.success = success;
        history.durationMs = durationMs;
        history.assignedAt = LocalDateTime.now().minusMinutes(durationMs / 60000);
        history.completedAt = LocalDateTime.now();
        assignmentHistory.add(history);

        // 更新能力评分
        updateCapabilityScore(agentId, milestoneId, success);
    }

    /**
     * 更新 Agent 能力评分
     */
    private void updateCapabilityScore(String agentId, String milestoneId, boolean success) {
        // 简单的指数移动平均
        Map<String, Double> scores = agentCapabilityScores.computeIfAbsent(agentId, k -> new HashMap<>());

        // 这里简化处理，实际应该根据里程碑类型更新对应角色的评分
        String role = "default";
        double currentScore = scores.getOrDefault(role, 50.0);
        double newScore = success ? Math.min(100, currentScore + 5) : Math.max(0, currentScore - 10);
        scores.put(role, newScore);
    }

    /**
     * 获取调度统计信息
     */
    public Map<String, Object> getScheduleStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        // Agent 负载统计
        Map<String, Double> loadStats = new LinkedHashMap<>();
        agentLoadMap.forEach((id, load) -> loadStats.put(id, load.loadScore));
        stats.put("agentLoads", loadStats);

        // Agent 绩效评分统计
        Map<String, Double> performanceStats = new LinkedHashMap<>();
        agentPerformanceScores.forEach((id, score) -> performanceStats.put(id, score));
        stats.put("agentPerformanceScores", performanceStats);

        // Agent 解雇风险统计
        Map<String, Object> dismissalStats = new LinkedHashMap<>();
        if (dismissalRequestRepository != null) {
            try {
                List<DismissalRequest> pendingRequests = dismissalRequestRepository.findByStatusOrderByCreatedAtDesc("PENDING");
                List<DismissalRequest> approvedRequests = dismissalRequestRepository.findByStatusOrderByCreatedAtDesc("APPROVED");
                dismissalStats.put("pendingCount", pendingRequests.size());
                dismissalStats.put("approvedCount", approvedRequests.size());

                // 各 Agent 的解雇风险惩罚
                Map<String, Double> riskPenalties = new LinkedHashMap<>();
                for (DismissalRequest req : pendingRequests) {
                    riskPenalties.put(req.getAgentId(), getDismissalRiskPenalty(req.getAgentId()));
                }
                for (DismissalRequest req : approvedRequests) {
                    riskPenalties.putIfAbsent(req.getAgentId(), getDismissalRiskPenalty(req.getAgentId()));
                }
                dismissalStats.put("riskPenalties", riskPenalties);
            } catch (Exception e) {
                log.warn("获取解雇统计失败: {}", e.getMessage());
            }
        }
        stats.put("dismissalStats", dismissalStats);

        // 调度历史统计
        long totalScheduled = assignmentHistory.size();
        long successfulScheduled = assignmentHistory.stream().filter(h -> h.success).count();
        stats.put("totalScheduled", totalScheduled);
        stats.put("successRate", totalScheduled > 0 ? (double) successfulScheduled / totalScheduled * 100 : 0);

        // 平均调度时间
        OptionalDouble avgDuration = assignmentHistory.stream()
            .mapToLong(h -> h.durationMs)
            .average();
        stats.put("avgDurationMs", avgDuration.orElse(0));

        return stats;
    }

    /**
     * 获取所有 Agent 的综合评估数据
     * 供前端调度页面展示每个 Agent 的多维度评分
     *
     * @param projectId 项目 ID
     * @return Agent 综合评估列表
     */
    public List<Map<String, Object>> getAgentEvaluations(String projectId) {
        List<Map<String, Object>> evaluations = new ArrayList<>();

        List<Agent> agents = agentManager.getAgentsByProject(projectId);
        if (agents.isEmpty()) {
            agents = agentManager.getAllAgents();
        }

        for (Agent agent : agents) {
            if ("producer".equals(agent.getRole())) continue;

            Map<String, Object> eval = new LinkedHashMap<>();
            eval.put("agentId", agent.getId());
            eval.put("agentName", agent.getName());
            eval.put("role", agent.getRole());
            eval.put("alive", agent.isAlive());
            eval.put("busy", agent.isBusy());

            // 绩效评分
            double perfScore = getPerformanceScore(agent.getId());
            eval.put("performanceScore", perfScore);

            // 负载评分
            AgentLoad load = getAgentLoad(agent.getId());
            eval.put("loadScore", load.loadScore);
            eval.put("activeTasks", load.activeTasks);
            eval.put("completedTasks", load.completedTasks);
            eval.put("failedTasks", load.failedTasks);

            // 历史表现
            double historyScore = getHistoryScore(agent.getId(), agent.getRole());
            eval.put("historyScore", historyScore);

            // 解雇风险
            double riskPenalty = getDismissalRiskPenalty(agent.getId());
            eval.put("dismissalRisk", riskPenalty);

            // 解雇申请状态
            String dismissalStatus = getDismissalStatus(agent.getId());
            eval.put("dismissalStatus", dismissalStatus);

            // 综合评分（与 ProducerAgent 的 7 维度评估对齐的简化版）
            double capabilityScore = getCapabilityScore(agent, agent.getRole());
            double availabilityScore = agent.isAlive() ? (agent.isBusy() ? 30 : 100) : 0;

            double totalScore = capabilityScore * 0.25
                + perfScore * 0.30
                + (100 - load.loadScore) * 0.20
                + historyScore * 0.15
                + availabilityScore * 0.05
                - riskPenalty;
            totalScore = Math.max(0, Math.min(100, totalScore));
            eval.put("totalScore", totalScore);

            // 评估结论
            if (totalScore < 30 || "EXECUTED".equals(dismissalStatus)) {
                eval.put("recommendation", "DISMISS");
            } else if (totalScore < 45 || "PENDING".equals(dismissalStatus)) {
                eval.put("recommendation", "WARN");
            } else if (totalScore < 60) {
                eval.put("recommendation", "OBSERVE");
            } else {
                eval.put("recommendation", "KEEP");
            }

            evaluations.add(eval);
        }

        // 按综合评分排序
        evaluations.sort((a, b) -> Double.compare(
            (double) b.get("totalScore"), (double) a.get("totalScore")));

        return evaluations;
    }

    /**
     * 获取 Agent 的解雇申请状态
     *
     * @param agentId Agent ID
     * @return 状态字符串（NONE/PENDING/APPROVED/EXECUTED/REJECTED）
     */
    private String getDismissalStatus(String agentId) {
        if (dismissalRequestRepository == null) return "NONE";

        try {
            List<DismissalRequest> requests = dismissalRequestRepository.findByAgentIdOrderByCreatedAtDesc(agentId);
            if (requests.isEmpty()) return "NONE";

            // 返回最新的解雇申请状态
            for (DismissalRequest req : requests) {
                String status = req.getStatus();
                if ("PENDING".equals(status) || "APPROVED".equals(status) || "EXECUTED".equals(status)) {
                    return status;
                }
            }
        } catch (Exception e) {
            log.warn("获取 Agent {} 解雇状态失败: {}", agentId, e.getMessage());
        }

        return "NONE";
    }
}
