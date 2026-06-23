package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.web.entity.PerformanceReview;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.chengxun.gamemaker.web.entity.VersionEvaluationEntity;
import com.chengxun.gamemaker.web.repository.VersionEvaluationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 版本评估服务
 * 在每个版本（迭代周期）执行过程中，动态评估：
 * 1. 每个 Agent 的积极性和效率
 * 2. 人手缺失与冗余
 * 3. 版本完成后的综合评估报告
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class VersionEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(VersionEvaluationService.class);

    @Autowired
    private AgentManager agentManager;

    @Autowired(required = false)
    private PerformanceManagementService performanceManagementService;

    @Autowired(required = false)
    private GoalService goalService;

    @Autowired(required = false)
    private ProjectBoard projectBoard;

    /** 版本评估持久化仓库 */
    @Autowired(required = false)
    private VersionEvaluationRepository evaluationRepository;

    /** JSON 序列化工具 */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 版本评估结果
     */
    public static class VersionEvaluation {
        private String projectId;
        private String milestoneId;
        private String milestoneTitle;
        private LocalDateTime evaluatedAt;

        /** Agent 绩效评估 */
        private Map<String, AgentEvaluation> agentEvaluations = new LinkedHashMap<>();

        /** 缺失的角色 */
        private List<String> missingRoles = new ArrayList<>();

        /** 冗余的角色 */
        private List<String> redundantRoles = new ArrayList<>();

        /** 版本效率评分 (0-100) */
        private int efficiencyScore;

        /** 版本质量评分 (0-100) */
        private int qualityScore;

        /** 综合评分 (0-100) */
        private int overallScore;

        /** 建议列表 */
        private List<String> recommendations = new ArrayList<>();

        public VersionEvaluation(String projectId, String milestoneId, String milestoneTitle) {
            this.projectId = projectId;
            this.milestoneId = milestoneId;
            this.milestoneTitle = milestoneTitle;
            this.evaluatedAt = LocalDateTime.now();
        }

        // Getters and Setters
        public String getProjectId() { return projectId; }
        public String getMilestoneId() { return milestoneId; }
        public String getMilestoneTitle() { return milestoneTitle; }
        public LocalDateTime getEvaluatedAt() { return evaluatedAt; }
        public Map<String, AgentEvaluation> getAgentEvaluations() { return agentEvaluations; }
        public List<String> getMissingRoles() { return missingRoles; }
        public List<String> getRedundantRoles() { return redundantRoles; }
        public int getEfficiencyScore() { return efficiencyScore; }
        public int getQualityScore() { return qualityScore; }
        public int getOverallScore() { return overallScore; }
        public List<String> getRecommendations() { return recommendations; }
    }

    /**
     * Agent 评估
     */
    public static class AgentEvaluation {
        private String agentId;
        private String agentName;
        private String agentRole;

        /** 任务完成率 (0-100) */
        private int taskCompletionRate;

        /** 响应速度评分 (0-100) */
        private int responsivenessScore;

        /** 工作质量评分 (0-100) */
        private int qualityScore;

        /** 协作评分 (0-100) */
        private int collaborationScore;

        /** 综合评分 (0-100) */
        private int overallScore;

        /** 积极性：HIGH, MEDIUM, LOW */
        private String enthusiasm;

        /** 状态：ACTIVE, IDLE, BLOCKED, UNRESPONSIVE */
        private String status;

        /** 建议 */
        private String recommendation;

        public AgentEvaluation(String agentId, String agentName, String agentRole) {
            this.agentId = agentId;
            this.agentName = agentName;
            this.agentRole = agentRole;
        }

        // Getters and Setters
        public String getAgentId() { return agentId; }
        public String getAgentName() { return agentName; }
        public String getAgentRole() { return agentRole; }
        public int getTaskCompletionRate() { return taskCompletionRate; }
        public void setTaskCompletionRate(int rate) { this.taskCompletionRate = rate; }
        public int getResponsivenessScore() { return responsivenessScore; }
        public void setResponsivenessScore(int score) { this.responsivenessScore = score; }
        public int getQualityScore() { return qualityScore; }
        public void setQualityScore(int score) { this.qualityScore = score; }
        public int getCollaborationScore() { return collaborationScore; }
        public void setCollaborationScore(int score) { this.collaborationScore = score; }
        public int getOverallScore() { return overallScore; }
        public void setOverallScore(int score) { this.overallScore = score; }
        public String getEnthusiasm() { return enthusiasm; }
        public void setEnthusiasm(String enthusiasm) { this.enthusiasm = enthusiasm; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getRecommendation() { return recommendation; }
        public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    }

    /**
     * 评估当前版本
     *
     * @param projectId 项目 ID
     * @param milestoneId 里程碑 ID
     * @return 版本评估结果
     */
    public VersionEvaluation evaluateVersion(String projectId, String milestoneId) {
        if (goalService == null) {
            log.warn("GoalService 未注入，无法评估版本");
            return null;
        }

        GameProject.GoalMilestone milestone = goalService.getMilestones(projectId).stream()
            .filter(m -> milestoneId.equals(m.getId()))
            .findFirst()
            .orElse(null);

        if (milestone == null) {
            log.warn("里程碑不存在: {}", milestoneId);
            return null;
        }

        VersionEvaluation evaluation = new VersionEvaluation(projectId, milestoneId, milestone.getTitle());

        // 1. 评估每个 Agent
        evaluateAgents(projectId, milestone, evaluation);

        // 2. 检测人手缺失与冗余
        detectStaffingIssues(projectId, milestone, evaluation);

        // 3. 计算版本效率和质量评分
        calculateVersionScores(milestone, evaluation);

        // 4. 生成建议
        generateRecommendations(evaluation);

        log.info("版本评估完成: 项目={}, 里程碑={}, 综合评分={}",
            projectId, milestone.getTitle(), evaluation.getOverallScore());

        // 持久化评估结果
        persistEvaluation(evaluation);

        return evaluation;
    }

    /**
     * 持久化评估结果到数据库
     */
    private void persistEvaluation(VersionEvaluation evaluation) {
        if (evaluationRepository == null) return;

        try {
            VersionEvaluationEntity entity = new VersionEvaluationEntity();
            entity.setProjectId(evaluation.getProjectId());
            entity.setMilestoneId(evaluation.getMilestoneId());
            entity.setMilestoneTitle(evaluation.getMilestoneTitle());
            entity.setEvaluatedAt(evaluation.getEvaluatedAt());
            entity.setEfficiencyScore(evaluation.getEfficiencyScore());
            entity.setQualityScore(evaluation.getQualityScore());
            entity.setOverallScore(evaluation.getOverallScore());
            entity.setMissingRoles(objectMapper.writeValueAsString(evaluation.getMissingRoles()));
            entity.setRedundantRoles(objectMapper.writeValueAsString(evaluation.getRedundantRoles()));
            entity.setRecommendations(objectMapper.writeValueAsString(evaluation.getRecommendations()));
            entity.setAgentEvaluationsJson(objectMapper.writeValueAsString(evaluation.getAgentEvaluations()));

            evaluationRepository.save(entity);
            log.debug("版本评估已持久化: 项目={}, 里程碑={}", evaluation.getProjectId(), evaluation.getMilestoneId());
        } catch (Exception e) {
            log.error("持久化版本评估失败", e);
        }
    }

    /**
     * 获取项目的历史评估记录
     *
     * @param projectId 项目 ID
     * @return 评估记录列表
     */
    public List<VersionEvaluationEntity> getEvaluationHistory(String projectId) {
        if (evaluationRepository == null) return Collections.emptyList();
        return evaluationRepository.findByProjectIdOrderByEvaluatedAtDesc(projectId);
    }

    /**
     * 获取项目的最新评估记录
     *
     * @param projectId 项目 ID
     * @return 最新评估记录
     */
    public VersionEvaluationEntity getLatestEvaluation(String projectId) {
        if (evaluationRepository == null) return null;
        return evaluationRepository.findFirstByProjectIdOrderByEvaluatedAtDesc(projectId);
    }

    /**
     * 评估每个 Agent 的表现
     */
    private void evaluateAgents(String projectId, GameProject.GoalMilestone milestone,
                                 VersionEvaluation evaluation) {
        List<Agent> agents = agentManager.getAgentsByProject(projectId);

        for (Agent agent : agents) {
            if ("producer".equals(agent.getRole())) continue;

            AgentEvaluation agentEval = new AgentEvaluation(
                agent.getId(), agent.getName(), agent.getRole());

            // 获取该 Agent 在此里程碑中的任务
            List<GameProject.MilestoneTask> agentTasks = milestone.getTasksByRole(agent.getRole());

            // 计算任务完成率
            if (!agentTasks.isEmpty()) {
                long completed = agentTasks.stream()
                    .filter(t -> t.getStatus() == GameProject.MilestoneStatus.COMPLETED)
                    .count();
                agentEval.setTaskCompletionRate((int) (completed * 100 / agentTasks.size()));
            }

            // 评估响应速度（基于Agent是否活跃）
            if (agent.isAlive()) {
                if (agent.isBusy()) {
                    agentEval.setResponsivenessScore(90);
                    agentEval.setStatus("ACTIVE");
                    agentEval.setEnthusiasm("HIGH");
                } else {
                    agentEval.setResponsivenessScore(70);
                    agentEval.setStatus("IDLE");
                    agentEval.setEnthusiasm("MEDIUM");
                }
            } else {
                agentEval.setResponsivenessScore(30);
                agentEval.setStatus("UNRESPONSIVE");
                agentEval.setEnthusiasm("LOW");
            }

            // 从绩效管理系统获取历史评分
            if (performanceManagementService != null) {
                List<PerformanceReview> reviews = performanceManagementService.getAgentReviews(agent.getId());
                if (!reviews.isEmpty()) {
                    PerformanceReview latest = reviews.get(0);
                    agentEval.setQualityScore(latest.getQualityScore() != null ? latest.getQualityScore() : 70);
                    agentEval.setCollaborationScore(latest.getCollaborationScore() != null ? latest.getCollaborationScore() : 70);
                } else {
                    agentEval.setQualityScore(70);
                    agentEval.setCollaborationScore(70);
                }
            } else {
                agentEval.setQualityScore(70);
                agentEval.setCollaborationScore(70);
            }

            // 计算综合评分
            int overall = (agentEval.getTaskCompletionRate() * 3 +
                          agentEval.getResponsivenessScore() * 2 +
                          agentEval.getQualityScore() * 3 +
                          agentEval.getCollaborationScore() * 2) / 10;
            agentEval.setOverallScore(overall);

            // 生成建议
            if (overall < 50) {
                agentEval.setRecommendation("表现不佳，建议关注或替换");
            } else if (overall < 70) {
                agentEval.setRecommendation("表现一般，需要改进");
            } else {
                agentEval.setRecommendation("表现良好");
            }

            evaluation.getAgentEvaluations().put(agent.getId(), agentEval);
        }
    }

    /**
     * 检测人手缺失与冗余
     */
    private void detectStaffingIssues(String projectId, GameProject.GoalMilestone milestone,
                                       VersionEvaluation evaluation) {
        // 获取里程碑需要的角色
        Set<String> requiredRoles = milestone.getInvolvedRoles();

        // 获取现有的 Agent 角色
        List<Agent> agents = agentManager.getAgentsByProject(projectId);
        Set<String> existingRoles = agents.stream()
            .map(Agent::getRole)
            .filter(r -> !"producer".equals(r))
            .collect(Collectors.toSet());

        // 检测缺失的角色
        for (String role : requiredRoles) {
            if (!existingRoles.contains(role)) {
                evaluation.getMissingRoles().add(role);
            }
        }

        // 检测冗余的角色（有Agent但里程碑不需要）
        for (String role : existingRoles) {
            if (!requiredRoles.contains(role)) {
                // 检查该角色是否有未完成的任务
                boolean hasUnfinishedTasks = milestone.getTasks().stream()
                    .anyMatch(t -> role.equals(t.getAssignedRole()) &&
                        t.getStatus() != GameProject.MilestoneStatus.COMPLETED);
                if (!hasUnfinishedTasks) {
                    evaluation.getRedundantRoles().add(role);
                }
            }
        }
    }

    /**
     * 计算版本效率和质量评分
     */
    private void calculateVersionScores(GameProject.GoalMilestone milestone,
                                         VersionEvaluation evaluation) {
        // 效率评分：基于任务完成率
        long totalTasks = milestone.getTasks().size();
        long completedTasks = milestone.getTasks().stream()
            .filter(t -> t.getStatus() == GameProject.MilestoneStatus.COMPLETED)
            .count();

        if (totalTasks > 0) {
            evaluation.efficiencyScore = (int) (completedTasks * 100 / totalTasks);
        } else {
            // 无任务时，根据里程碑状态给默认分
            evaluation.efficiencyScore = milestone.getStatus() == GameProject.MilestoneStatus.COMPLETED ? 70 : 30;
            log.info("里程碑 [{}] 无任务，使用默认效率分: {}", milestone.getTitle(), evaluation.efficiencyScore);
        }

        // 质量评分：基于 Agent 质量评分的平均值
        if (!evaluation.getAgentEvaluations().isEmpty()) {
            int totalQuality = evaluation.getAgentEvaluations().values().stream()
                .mapToInt(AgentEvaluation::getQualityScore)
                .sum();
            evaluation.qualityScore = totalQuality / evaluation.getAgentEvaluations().size();
        } else {
            // 无Agent评估时，根据里程碑完成状态给默认分
            evaluation.qualityScore = milestone.getStatus() == GameProject.MilestoneStatus.COMPLETED ? 60 : 40;
            log.info("里程碑 [{}] 无Agent评估数据，使用默认质量分: {}", milestone.getTitle(), evaluation.qualityScore);
        }

        // 综合评分：确保最低分不为0
        evaluation.overallScore = Math.max(1, (evaluation.efficiencyScore * 4 + evaluation.qualityScore * 6) / 10);
    }

    /**
     * 生成建议
     */
    private void generateRecommendations(VersionEvaluation evaluation) {
        // 基于缺失角色生成建议
        if (!evaluation.getMissingRoles().isEmpty()) {
            evaluation.getRecommendations().add(
                "缺少角色: " + String.join(", ", evaluation.getMissingRoles()) + "，建议招聘");
        }

        // 基于冗余角色生成建议
        if (!evaluation.getRedundantRoles().isEmpty()) {
            evaluation.getRecommendations().add(
                "冗余角色: " + String.join(", ", evaluation.getRedundantRoles()) + "，可考虑调整");
        }

        // 基于效率评分生成建议
        if (evaluation.getEfficiencyScore() < 50) {
            evaluation.getRecommendations().add("版本效率较低，建议检查任务分配和进度推进");
        }

        // 基于质量评分生成建议
        if (evaluation.getQualityScore() < 60) {
            evaluation.getRecommendations().add("版本质量较低，建议加强质量检查和代码审查");
        }

        // 基于 Agent 表现生成建议
        long lowPerformers = evaluation.getAgentEvaluations().values().stream()
            .filter(a -> a.getOverallScore() < 50)
            .count();
        if (lowPerformers > 0) {
            evaluation.getRecommendations().add(
                lowPerformers + " 个 Agent 表现不佳，建议关注或调整");
        }
    }

    /**
     * 生成版本评估报告（文本格式）
     *
     * @param evaluation 版本评估结果
     * @return 格式化的报告
     */
    public String generateReport(VersionEvaluation evaluation) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 版本评估报告\n\n");

        sb.append("### 基本信息\n");
        sb.append("- 项目: ").append(evaluation.getProjectId()).append("\n");
        sb.append("- 里程碑: ").append(evaluation.getMilestoneTitle()).append("\n");
        sb.append("- 评估时间: ").append(evaluation.getEvaluatedAt()).append("\n\n");

        sb.append("### 评分概览\n");
        sb.append("- 效率评分: ").append(evaluation.getEfficiencyScore()).append("/100\n");
        sb.append("- 质量评分: ").append(evaluation.getQualityScore()).append("/100\n");
        sb.append("- 综合评分: ").append(evaluation.getOverallScore()).append("/100\n\n");

        sb.append("### Agent 表现\n");
        for (AgentEvaluation agentEval : evaluation.getAgentEvaluations().values()) {
            sb.append(String.format("- **%s** (%s): 综合=%d, 任务完成=%d%%, 响应=%d, 质量=%d, 协作=%d [%s]\n",
                agentEval.getAgentName(), agentEval.getAgentRole(),
                agentEval.getOverallScore(), agentEval.getTaskCompletionRate(),
                agentEval.getResponsivenessScore(), agentEval.getQualityScore(),
                agentEval.getCollaborationScore(), agentEval.getEnthusiasm()));
        }
        sb.append("\n");

        if (!evaluation.getMissingRoles().isEmpty()) {
            sb.append("### ⚠️ 缺失角色\n");
            for (String role : evaluation.getMissingRoles()) {
                sb.append("- ").append(role).append("\n");
            }
            sb.append("\n");
        }

        if (!evaluation.getRedundantRoles().isEmpty()) {
            sb.append("### 📋 冗余角色\n");
            for (String role : evaluation.getRedundantRoles()) {
                sb.append("- ").append(role).append("\n");
            }
            sb.append("\n");
        }

        if (!evaluation.getRecommendations().isEmpty()) {
            sb.append("### 💡 建议\n");
            for (String rec : evaluation.getRecommendations()) {
                sb.append("- ").append(rec).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
