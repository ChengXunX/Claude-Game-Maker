package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.agent.BaseAgent;
import com.chengxun.gamemaker.config.SystemConstants;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.model.GameProject.GoalMilestone;
import com.chengxun.gamemaker.model.GameProject.MilestoneStatus;
import com.chengxun.gamemaker.model.GameProject.VersionHistory;
import com.chengxun.gamemaker.web.entity.VersionEvaluationDimension;
import com.chengxun.gamemaker.web.entity.VersionIterationRecord;
import com.chengxun.gamemaker.web.repository.VersionEvaluationDimensionRepository;
import com.chengxun.gamemaker.web.repository.VersionIterationRecordRepository;
import com.chengxun.gamemaker.web.service.AlertService;
import com.chengxun.gamemaker.web.service.SystemConstantService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 版本迭代服务
 * 管理游戏项目的版本迭代生命周期，支持多版本循环开发
 *
 * 核心功能：
 * - 版本验收：评估当前版本质量，决定是否通过
 * - 版本规划：判断是否需要下一版本迭代
 * - 版本升级：创建新版本并重置里程碑
 * - 停止项目：目标完成时停止所有项目内Agent
 *
 * 设计理念：
 * 里程碑完成 ≠ 目标完成
 * 里程碑全部完成 → 版本验收 → 记录版本历史 → AI评估是否需要下一版本
 *   → 需要：创建新版本里程碑 → 继续迭代
 *   → 不需要：标记目标完成 → 停止所有Agent
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class VersionIterationService {

    private static final Logger log = LoggerFactory.getLogger(VersionIterationService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ProjectManager projectManager;

    @Autowired
    private AgentManager agentManager;

    @Autowired(required = false)
    private SystemConstantService constantService;

    @Autowired(required = false)
    private VersionIterationRecordRepository iterationRecordRepository;

    @Autowired(required = false)
    private VersionEvaluationDimensionRepository dimensionRepository;

    @Autowired(required = false)
    private AlertService alertService;

    @Autowired(required = false)
    private com.chengxun.gamemaker.feishu.FeishuBotService feishuService;

    @Autowired(required = false)
    private com.chengxun.gamemaker.engine.ClaudeCliEngine claudeEngine;

    @Autowired(required = false)
    private com.chengxun.gamemaker.web.service.ClaudeAiService aiService;

    /**
     * 版本迭代开始时间缓存
     * key: projectId, value: 当前版本迭代开始时间
     */
    private final java.util.concurrent.ConcurrentHashMap<String, java.time.LocalDateTime> iterationStartTimes = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 定时检查迭代超时
     * 每小时检查一次
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void checkIterationTimeout() {
        int timeoutHours = getIterationTimeoutHours();
        if (timeoutHours <= 0) return;

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.Duration timeout = java.time.Duration.ofHours(timeoutHours);

        for (var entry : iterationStartTimes.entrySet()) {
            String projectId = entry.getKey();
            java.time.LocalDateTime startTime = entry.getValue();
            java.time.Duration elapsed = java.time.Duration.between(startTime, now);

            if (elapsed.compareTo(timeout) > 0) {
                // 超时告警
                sendTimeoutAlert(projectId, elapsed.toHours());
            }
        }
    }

    /**
     * 记录迭代开始时间
     */
    private void recordIterationStart(String projectId) {
        iterationStartTimes.put(projectId, java.time.LocalDateTime.now());
    }

    /**
     * 清除迭代开始时间
     */
    private void clearIterationStart(String projectId) {
        iterationStartTimes.remove(projectId);
    }

    /**
     * 发送超时告警
     */
    private void sendTimeoutAlert(String projectId, long elapsedHours) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) return;

        String message = String.format("项目 [%s] 版本迭代已超时 %d 小时（当前版本: %s）",
            project.getName(), elapsedHours, project.getVersion());

        log.warn("版本迭代超时: {}", message);

        // 通过告警系统发送
        if (alertService != null) {
            try {
                com.chengxun.gamemaker.web.entity.AlertRecord alert = new com.chengxun.gamemaker.web.entity.AlertRecord();
                alert.setRuleName("版本迭代超时");
                alert.setPriority("HIGH");
                alert.setTitle("版本迭代超时告警");
                alert.setDetail(message);
                alert.setMetric("VERSION_ITERATION_TIMEOUT");
                alert.setProjectId(projectId);
                alert.setStatus("PENDING");
                alert.setCreatedAt(java.time.LocalDateTime.now());
                alert.setUpdatedAt(java.time.LocalDateTime.now());
                alertService.saveAlert(alert);
            } catch (Exception e) {
                log.warn("发送超时告警失败: {}", e.getMessage());
            }
        }

        // 通过飞书发送
        if (feishuService != null && feishuService.isEnabled()) {
            try {
                feishuService.sendMessage("⚠️ " + message);
            } catch (Exception e) {
                log.debug("飞书发送超时告警失败: {}", e.getMessage());
            }
        }
    }

    private int getIterationTimeoutHours() {
        if (constantService != null) {
            return constantService.getInt(SystemConstants.VERSION_ITERATION_TIMEOUT_HOURS, 72);
        }
        return 72;
    }

    /**
     * 获取版本验收通过分数
     */
    private int getVersionPassScore() {
        if (constantService != null) {
            return constantService.getInt(SystemConstants.VERSION_PASS_SCORE, 7);
        }
        return 7;
    }

    /**
     * 获取最大版本迭代次数
     */
    private int getMaxVersionIterations() {
        if (constantService != null) {
            return constantService.getInt(SystemConstants.VERSION_MAX_ITERATIONS, 10);
        }
        return 10;
    }

    /**
     * 获取最小版本迭代次数
     */
    private int getMinVersionIterations() {
        if (constantService != null) {
            return constantService.getInt(SystemConstants.VERSION_MIN_ITERATIONS, 1);
        }
        return 1;
    }

    /**
     * 获取迭代策略
     * incremental: 增量迭代 - 只重置未完成的里程碑
     * full: 全量迭代 - 重置所有里程碑
     * adaptive: 自适应迭代 - 根据评估结果决定重置范围
     */
    private String getIterationStrategy() {
        if (constantService != null) {
            return constantService.getString(SystemConstants.VERSION_ITERATION_STRATEGY, "adaptive");
        }
        return "adaptive";
    }

    /**
     * 版本评估结果
     */
    public static class VersionEvaluationResult {
        /** 评估分数（1-10） */
        private int score;
        /** 是否通过验收 */
        private boolean passed;
        /** 评估详情 */
        private String details;
        /** 优点列表 */
        private List<String> strengths = new ArrayList<>();
        /** 待改进列表 */
        private List<String> improvements = new ArrayList<>();
        /** 建议（通过时：是否需要下一版本；不通过时：如何改进） */
        private String recommendation;

        // Getters and Setters
        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }
        public boolean isPassed() { return passed; }
        public void setPassed(boolean passed) { this.passed = passed; }
        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }
        public List<String> getStrengths() { return strengths; }
        public void setStrengths(List<String> strengths) { this.strengths = strengths; }
        public List<String> getImprovements() { return improvements; }
        public void setImprovements(List<String> improvements) { this.improvements = improvements; }
        public String getRecommendation() { return recommendation; }
        public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    }

    /**
     * 下一版本规划结果
     */
    public static class NextVersionPlan {
        /** 是否需要下一版本 */
        private boolean needNextVersion;
        /** 下一版本号 */
        private String nextVersion;
        /** 下一版本目标 */
        private String nextGoal;
        /** 下一版本里程碑列表 */
        private List<String> nextMilestones = new ArrayList<>();
        /** 规划理由 */
        private String reason;

        // Getters and Setters
        public boolean isNeedNextVersion() { return needNextVersion; }
        public void setNeedNextVersion(boolean needNextVersion) { this.needNextVersion = needNextVersion; }
        public String getNextVersion() { return nextVersion; }
        public void setNextVersion(String nextVersion) { this.nextVersion = nextVersion; }
        public String getNextGoal() { return nextGoal; }
        public void setNextGoal(String nextGoal) { this.nextGoal = nextGoal; }
        public List<String> getNextMilestones() { return nextMilestones; }
        public void setNextMilestones(List<String> nextMilestones) { this.nextMilestones = nextMilestones; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    /**
     * 检查当前版本是否已完成（所有里程碑完成）
     *
     * @param projectId 项目ID
     * @return true 如果所有里程碑都已完成
     */
    public boolean isCurrentVersionCompleted(String projectId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) return false;

        List<GoalMilestone> milestones = project.getMilestones();
        if (milestones.isEmpty()) return false;

        return milestones.stream().allMatch(m -> m.getStatus() == MilestoneStatus.COMPLETED);
    }

    /**
     * 评估当前版本质量
     * 使用AI分析版本完成情况，给出1-10分的评分
     *
     * @param projectId 项目ID
     * @return 版本评估结果
     */
    public VersionEvaluationResult evaluateVersion(String projectId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            VersionEvaluationResult result = new VersionEvaluationResult();
            result.setScore(0);
            result.setDetails("项目不存在");
            return result;
        }

        // 构建评估上下文
        StringBuilder context = new StringBuilder();
        context.append("## 项目信息\n");
        context.append("- 项目名称: ").append(project.getName()).append("\n");
        context.append("- 项目目标: ").append(project.getGoal()).append("\n");
        context.append("- 当前版本: ").append(project.getVersion()).append("\n\n");

        context.append("## 里程碑完成情况\n");
        for (GoalMilestone milestone : project.getMilestones()) {
            context.append("- ").append(milestone.getTitle())
                   .append(" [").append(milestone.getStatus()).append("]\n");
            if (milestone.getDescription() != null) {
                context.append("  描述: ").append(milestone.getDescription()).append("\n");
            }
        }

        // 使用AI评估
        String prompt = buildEvaluationPrompt(context.toString());
        String response = callAiForEvaluation(prompt, project);

        // 解析评估结果
        return parseEvaluationResponse(response);
    }

    /**
     * 规划下一版本
     * 使用AI判断是否需要下一版本，以及下一版本的内容
     *
     * @param projectId 项目ID
     * @param currentEvaluation 当前版本评估结果
     * @return 下一版本规划结果
     */
    public NextVersionPlan planNextVersion(String projectId, VersionEvaluationResult currentEvaluation) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            NextVersionPlan plan = new NextVersionPlan();
            plan.setNeedNextVersion(false);
            plan.setReason("项目不存在");
            return plan;
        }

        // 检查是否已达最大迭代次数
        int maxIterations = getMaxVersionIterations();
        if (project.getVersionCount() >= maxIterations) {
            NextVersionPlan plan = new NextVersionPlan();
            plan.setNeedNextVersion(false);
            plan.setReason("已达最大版本迭代次数(" + maxIterations + ")，建议结束项目");
            return plan;
        }

        // 检查是否达到最小迭代次数
        int minIterations = getMinVersionIterations();
        if (project.getVersionCount() < minIterations) {
            // 未达到最小迭代次数，强制继续迭代
            NextVersionPlan plan = new NextVersionPlan();
            plan.setNeedNextVersion(true);
            plan.setNextVersion(incrementVersion(project.getVersion()));
            plan.setReason("未达到最小迭代次数(" + minIterations + ")，继续迭代");
            return plan;
        }

        // 构建规划上下文
        StringBuilder context = new StringBuilder();
        context.append("## 项目信息\n");
        context.append("- 项目名称: ").append(project.getName()).append("\n");
        context.append("- 项目目标: ").append(project.getGoal()).append("\n");
        context.append("- 当前版本: ").append(project.getVersion()).append("\n");
        context.append("- 已迭代版本数: ").append(project.getVersionCount()).append("\n\n");

        context.append("## 当前版本评估\n");
        context.append("- 评分: ").append(currentEvaluation.getScore()).append("/10\n");
        context.append("- 详情: ").append(currentEvaluation.getDetails()).append("\n");
        if (currentEvaluation.getImprovements() != null && !currentEvaluation.getImprovements().isEmpty()) {
            context.append("- 待改进: ").append(String.join(", ", currentEvaluation.getImprovements())).append("\n");
        }

        // 使用AI规划
        String prompt = buildPlanningPrompt(context.toString());
        String response = callAiForPlanning(prompt);

        // 解析规划结果
        return parsePlanningResponse(response, project);
    }

    /**
     * 执行版本升级
     * 创建新版本历史，重置里程碑状态，清理工作流状态
     *
     * @param projectId 项目ID
     * @param plan 下一版本规划
     * @return 是否成功
     */
    public boolean upgradeVersion(String projectId, NextVersionPlan plan) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) return false;

        // 记录当前版本历史
        String oldVersion = project.getVersion();
        String newVersion = plan.getNextVersion();
        project.createVersion(newVersion, "版本迭代: " + plan.getReason(), "ProducerAgent");

        // 根据迭代策略重置里程碑
        String strategy = getIterationStrategy();
        resetMilestonesByStrategy(project, strategy, plan);

        // 更新目标
        if (plan.getNextGoal() != null && !plan.getNextGoal().isEmpty()) {
            project.setGoal(plan.getNextGoal());
        }

        // 保存项目
        projectManager.saveProjectConfig(project);

        // 通知项目内所有Agent版本升级
        notifyVersionUpgrade(projectId, oldVersion, newVersion, plan);

        log.info("版本升级完成: {} -> {} (策略: {}) for project {}", oldVersion, newVersion, strategy, projectId);
        return true;
    }

    /**
     * 根据迭代策略重置里程碑
     *
     * @param project 项目
     * @param strategy 迭代策略
     * @param plan 版本规划
     */
    private void resetMilestonesByStrategy(GameProject project, String strategy, NextVersionPlan plan) {
        List<GoalMilestone> milestones = project.getMilestones();

        switch (strategy) {
            case "incremental" -> {
                // 增量迭代：只重置有问题的里程碑（如质量不达标的）
                log.info("增量迭代策略：只重置需要改进的里程碑");
                for (GoalMilestone milestone : milestones) {
                    // 如果有验证失败记录或任务完成质量差，重置该里程碑
                    if (milestone.getVerificationFailCount() > 0 ||
                        (milestone.getTasks() != null && hasIncompleteTasks(milestone))) {
                        resetMilestone(milestone);
                    }
                }
            }
            case "full" -> {
                // 全量迭代：重置所有里程碑
                log.info("全量迭代策略：重置所有里程碑");
                for (GoalMilestone milestone : milestones) {
                    resetMilestone(milestone);
                }
            }
            case "adaptive" -> {
                // 自适应迭代：根据评估分数决定重置范围
                log.info("自适应迭代策略：根据评估结果决定重置范围");
                for (GoalMilestone milestone : milestones) {
                    // 如果里程碑有验证失败或任务未完成，重置
                    if (milestone.getVerificationFailCount() > 0 ||
                        milestone.getStatus() != MilestoneStatus.COMPLETED ||
                        hasIncompleteTasks(milestone)) {
                        resetMilestone(milestone);
                    }
                }
            }
            default -> {
                // 默认：重置所有里程碑
                log.info("默认策略：重置所有里程碑");
                for (GoalMilestone milestone : milestones) {
                    resetMilestone(milestone);
                }
            }
        }
    }

    /**
     * 重置单个里程碑
     */
    private void resetMilestone(GoalMilestone milestone) {
        milestone.setStatus(MilestoneStatus.PENDING);
        milestone.setProgress(0);
        milestone.setVerificationResult(null);
        milestone.setVerificationFailCount(0);
        milestone.setLastVerificationTime(null);
        if (milestone.getTasks() != null) {
            milestone.getTasks().forEach(task -> {
                task.setStatus(MilestoneStatus.PENDING);
                task.setResult(null);
            });
        }
    }

    /**
     * 检查里程碑是否有未完成的任务
     */
    private boolean hasIncompleteTasks(GoalMilestone milestone) {
        if (milestone.getTasks() == null || milestone.getTasks().isEmpty()) {
            return false;
        }
        return milestone.getTasks().stream()
            .anyMatch(task -> task.getStatus() != MilestoneStatus.COMPLETED);
    }

    /**
     * 通知项目内所有Agent版本升级
     */
    private void notifyVersionUpgrade(String projectId, String oldVersion, String newVersion, NextVersionPlan plan) {
        List<Agent> agents = agentManager.getAgentsByProject(projectId);
        String message = String.format(
            "版本升级通知：项目已从 %s 升级到 %s\n原因：%s\n下一版本目标：%s",
            oldVersion, newVersion, plan.getReason(),
            plan.getNextGoal() != null ? plan.getNextGoal() : "继续迭代"
        );

        for (Agent agent : agents) {
            if (agent.isAlive() && agent instanceof BaseAgent baseAgent) {
                try {
                    baseAgent.saveKnowledge("version_upgrade_" + newVersion, message);
                } catch (Exception e) {
                    log.debug("通知Agent版本升级失败: {}", agent.getId());
                }
            }
        }
    }

    /**
     * 停止项目内所有Agent
     * 目标完成时调用，停止制作人和其他所有Agent
     *
     * @param projectId 项目ID
     * @return 停止的Agent数量
     */
    public int stopAllProjectAgents(String projectId) {
        if (projectId == null) return 0;

        List<Agent> agents = agentManager.getAgentsByProject(projectId);
        int stoppedCount = 0;

        for (Agent agent : agents) {
            if (agent.isAlive()) {
                agent.stop();
                stoppedCount++;
                log.info("已停止Agent: {} ({})", agent.getName(), agent.getRole());
            }
        }

        log.info("项目 {} 已停止 {} 个Agent", projectId, stoppedCount);
        return stoppedCount;
    }

    /**
     * 获取项目的版本迭代记录
     *
     * @param projectId 项目ID
     * @return 版本迭代记录列表
     */
    public List<VersionIterationRecord> getIterationRecords(String projectId) {
        if (iterationRecordRepository == null) {
            return Collections.emptyList();
        }
        return iterationRecordRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    /**
     * 获取项目的版本迭代统计
     *
     * @param projectId 项目ID
     * @return 统计信息（迭代次数、平均分、完成次数等）
     */
    public Map<String, Object> getIterationStats(String projectId) {
        Map<String, Object> stats = new HashMap<>();
        if (iterationRecordRepository == null) {
            return stats;
        }

        long totalCount = iterationRecordRepository.countByProjectId(projectId);
        long passedCount = iterationRecordRepository.countByProjectIdAndPassed(projectId, true);
        Double avgScore = iterationRecordRepository.getAverageScoreByProjectId(projectId);

        stats.put("totalIterations", totalCount);
        stats.put("passedIterations", passedCount);
        stats.put("passRate", totalCount > 0 ? (double) passedCount / totalCount * 100 : 0);
        stats.put("averageScore", avgScore != null ? Math.round(avgScore * 10) / 10.0 : 0);

        return stats;
    }

    /**
     * 版本对比
     * 对比两个版本的里程碑差异和评估变化
     *
     * @param projectId 项目ID
     * @param version1 版本1
     * @param version2 版本2
     * @return 对比结果
     */
    public Map<String, Object> compareVersions(String projectId, String version1, String version2) {
        Map<String, Object> comparison = new HashMap<>();
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            comparison.put("error", "项目不存在");
            return comparison;
        }

        // 获取两个版本的迭代记录
        VersionIterationRecord record1 = null;
        VersionIterationRecord record2 = null;
        if (iterationRecordRepository != null) {
            record1 = iterationRecordRepository.findByProjectIdAndVersion(projectId, version1).orElse(null);
            record2 = iterationRecordRepository.findByProjectIdAndVersion(projectId, version2).orElse(null);
        }

        comparison.put("version1", version1);
        comparison.put("version2", version2);

        // 评估对比
        if (record1 != null && record2 != null) {
            Map<String, Object> evaluationComparison = new HashMap<>();
            evaluationComparison.put("score1", record1.getEvaluationScore());
            evaluationComparison.put("score2", record2.getEvaluationScore());
            evaluationComparison.put("scoreDiff", record2.getEvaluationScore() - record1.getEvaluationScore());
            evaluationComparison.put("passed1", record1.isPassed());
            evaluationComparison.put("passed2", record2.isPassed());
            comparison.put("evaluation", evaluationComparison);
        }

        // 里程碑对比
        List<GoalMilestone> milestones = project.getMilestones();
        Map<String, Object> milestoneComparison = new HashMap<>();
        milestoneComparison.put("total", milestones.size());
        milestoneComparison.put("completed", milestones.stream()
            .filter(m -> m.getStatus() == MilestoneStatus.COMPLETED).count());
        comparison.put("milestones", milestoneComparison);

        // 版本历史信息
        List<VersionHistory> history = project.getVersionHistory();
        VersionHistory history1 = history.stream()
            .filter(h -> version1.equals(h.getVersion())).findFirst().orElse(null);
        VersionHistory history2 = history.stream()
            .filter(h -> version2.equals(h.getVersion())).findFirst().orElse(null);

        if (history1 != null) {
            comparison.put("description1", history1.getDescription());
            comparison.put("createdAt1", history1.getCreatedAt());
        }
        if (history2 != null) {
            comparison.put("description2", history2.getDescription());
            comparison.put("createdAt2", history2.getCreatedAt());
        }

        return comparison;
    }

    /**
     * 导出迭代报告
     *
     * @param projectId 项目ID
     * @return Excel文件字节数组
     */
    public byte[] exportIterationReport(String projectId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }

        List<VersionIterationRecord> records = getIterationRecords(projectId);
        Map<String, Object> stats = getIterationStats(projectId);

        // 使用简单的CSV格式（实际项目中可以使用Apache POI生成Excel）
        StringBuilder csv = new StringBuilder();
        csv.append("﻿"); // BOM for UTF-8

        // 标题行
        csv.append("版本迭代报告 - ").append(project.getName()).append("\n\n");

        // 统计信息
        csv.append("迭代统计\n");
        csv.append("总迭代次数,").append(stats.get("totalIterations")).append("\n");
        csv.append("通过次数,").append(stats.get("passedIterations")).append("\n");
        csv.append("通过率,").append(String.format("%.1f%%", stats.get("passRate"))).append("\n");
        csv.append("平均评分,").append(stats.get("averageScore")).append("\n\n");

        // 迭代记录
        csv.append("版本,评估分数,是否通过,结果,里程碑完成,创建时间\n");
        for (VersionIterationRecord record : records) {
            csv.append(record.getVersion()).append(",");
            csv.append(record.getEvaluationScore()).append(",");
            csv.append(record.isPassed() ? "是" : "否").append(",");
            csv.append(getRecordResultText(record.getResult())).append(",");
            csv.append(record.getCompletedMilestones()).append("/").append(record.getTotalMilestones()).append(",");
            csv.append(record.getCreatedAt() != null ? record.getCreatedAt().toString() : "").append("\n");
        }

        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String getRecordResultText(String result) {
        if (result == null) return "未知";
        return switch (result) {
            case "COMPLETED" -> "目标完成";
            case "ITERATED" -> "继续迭代";
            case "IMPROVED" -> "需要改进";
            case "ROLLBACK" -> "版本回滚";
            default -> result;
        };
    }

    /**
     * 获取迭代模板列表
     *
     * @return 模板列表
     */
    public List<Map<String, Object>> getIterationTemplates() {
        List<Map<String, Object>> templates = new ArrayList<>();

        Map<String, Object> mvpTemplate = new HashMap<>();
        mvpTemplate.put("id", "mvp");
        mvpTemplate.put("name", "MVP迭代");
        mvpTemplate.put("description", "最小可行产品迭代，快速验证核心玩法");
        mvpTemplate.put("defaultMilestones", List.of("核心玩法实现", "基础UI完成", "可玩版本发布"));
        templates.add(mvpTemplate);

        Map<String, Object> optimizeTemplate = new HashMap<>();
        optimizeTemplate.put("id", "optimize");
        optimizeTemplate.put("name", "优化迭代");
        optimizeTemplate.put("description", "针对性能、体验、UI的优化迭代");
        optimizeTemplate.put("defaultMilestones", List.of("性能优化", "UI美化", "体验改进", "Bug修复"));
        templates.add(optimizeTemplate);

        Map<String, Object> releaseTemplate = new HashMap<>();
        releaseTemplate.put("id", "release");
        releaseTemplate.put("name", "发布迭代");
        releaseTemplate.put("description", "正式发布前的准备迭代");
        releaseTemplate.put("defaultMilestones", List.of("功能冻结", "全面测试", "文档完善", "正式发布"));
        templates.add(releaseTemplate);

        return templates;
    }

    /**
     * 版本回滚
     * 回滚到上一个版本，恢复里程碑状态
     *
     * @param projectId 项目ID
     * @param targetVersion 目标版本号（null则回滚到上一个版本）
     * @return 回滚结果信息
     */
    public String rollbackVersion(String projectId, String targetVersion) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return "项目不存在";
        }

        List<VersionHistory> history = project.getVersionHistory();
        if (history == null || history.isEmpty()) {
            return "没有版本历史，无法回滚";
        }

        String currentVersion = project.getVersion();
        VersionHistory targetHistory = null;

        if (targetVersion == null || targetVersion.isEmpty()) {
            // 回滚到上一个版本
            targetHistory = history.get(history.size() - 1);
        } else {
            // 回滚到指定版本
            targetHistory = history.stream()
                .filter(h -> targetVersion.equals(h.getVersion()))
                .findFirst()
                .orElse(null);
        }

        if (targetHistory == null) {
            return "目标版本不存在: " + targetVersion;
        }

        if (targetHistory.getVersion().equals(currentVersion)) {
            return "当前已经是目标版本: " + currentVersion;
        }

        // 执行回滚
        String oldVersion = currentVersion;
        String newVersion = targetHistory.getVersion();

        // 恢复版本号
        project.setVersion(newVersion);

        // 移除目标版本之后的所有历史
        int targetIndex = -1;
        for (int i = 0; i < history.size(); i++) {
            if (newVersion.equals(history.get(i).getVersion())) {
                targetIndex = i;
                break;
            }
        }
        if (targetIndex >= 0 && targetIndex < history.size() - 1) {
            // 保留目标版本及之前的记录
            List<VersionHistory> newHistory = new ArrayList<>(history.subList(0, targetIndex + 1));
            project.setVersionHistory(newHistory);
        }

        // 重置里程碑状态
        for (GoalMilestone milestone : project.getMilestones()) {
            milestone.setStatus(MilestoneStatus.PENDING);
            milestone.setProgress(0);
            milestone.setVerificationResult(null);
            if (milestone.getTasks() != null) {
                milestone.getTasks().forEach(task -> {
                    task.setStatus(MilestoneStatus.PENDING);
                    task.setResult(null);
                });
            }
        }

        // 保存项目
        projectManager.saveProjectConfig(project);

        // 记录回滚操作
        saveRollbackRecord(projectId, oldVersion, newVersion);

        // 分析回滚原因并保存到知识库
        analyzeRollbackAndSaveLessons(project, oldVersion, newVersion);

        log.info("版本回滚完成: {} -> {} for project {}", oldVersion, newVersion, projectId);
        return String.format("版本回滚成功: %s -> %s", oldVersion, newVersion);
    }

    /**
     * 分析回滚原因并保存经验教训到知识库
     * 这是迭代反馈闭环的核心：回滚 → 分析 → 沉淀 → 下次避免
     */
    private void analyzeRollbackAndSaveLessons(GameProject project, String fromVersion, String toVersion) {
        try {
            StringBuilder lessons = new StringBuilder();
            lessons.append(String.format("版本 %s 回滚到 %s 的经验教训：\n\n", fromVersion, toVersion));

            // 分析里程碑完成情况
            List<GoalMilestone> milestones = project.getMilestones();
            int totalTasks = 0;
            int completedTasks = 0;
            int failedTasks = 0;
            List<String> failedTaskNames = new ArrayList<>();

            for (GoalMilestone m : milestones) {
                if (m.getTasks() == null) continue;
                for (var task : m.getTasks()) {
                    totalTasks++;
                    if (task.getStatus() == MilestoneStatus.COMPLETED) {
                        completedTasks++;
                    } else if (task.getResult() != null && task.getResult().contains("失败")) {
                        failedTasks++;
                        failedTaskNames.add(task.getTitle());
                    }
                }
            }

            lessons.append("### 任务完成情况\n");
            lessons.append(String.format("- 总任务数: %d\n", totalTasks));
            lessons.append(String.format("- 已完成: %d\n", completedTasks));
            lessons.append(String.format("- 失败: %d\n", failedTasks));

            if (!failedTaskNames.isEmpty()) {
                lessons.append("\n### 失败任务\n");
                for (String name : failedTaskNames) {
                    lessons.append("- ").append(name).append("\n");
                }
            }

            // 分析验证失败情况
            lessons.append("\n### 验证失败分析\n");
            for (GoalMilestone m : milestones) {
                if (m.getVerificationFailCount() > 0) {
                    lessons.append(String.format("- 里程碑 '%s': 验证失败 %d 次\n",
                        m.getTitle(), m.getVerificationFailCount()));
                }
            }

            lessons.append("\n### 改进建议\n");
            lessons.append("1. 下次分解任务时，确保每个任务有明确的验收标准\n");
            lessons.append("2. 对于验证失败的任务，增加更详细的验证规则\n");
            lessons.append("3. 考虑将大任务拆分为更小的可验证单元\n");

            // 保存到知识库
            Agent producer = agentManager.getAgentsByProject(project.getId()).stream()
                .filter(a -> "producer".equals(a.getRole()))
                .findFirst().orElse(null);

            if (producer instanceof BaseAgent baseAgent) {
                baseAgent.saveKnowledge("rollback_lesson_" + fromVersion, lessons.toString());
                log.info("回滚经验教训已保存到知识库: {}", fromVersion);
            }
        } catch (Exception e) {
            log.warn("分析回滚原因失败: {}", e.getMessage());
        }
    }

    /**
     * 保存回滚记录
     */
    private void saveRollbackRecord(String projectId, String fromVersion, String toVersion) {
        if (iterationRecordRepository == null) {
            return;
        }

        try {
            GameProject project = projectManager.getProject(projectId);
            if (project == null) return;

            VersionIterationRecord record = new VersionIterationRecord(projectId, toVersion);
            record.setEvaluationScore(0);
            record.setPassed(false);
            record.setEvaluationDetails(String.format("版本回滚: %s -> %s", fromVersion, toVersion));
            record.setRecommendation("版本回滚操作");
            record.setNeedNextVersion(false);
            record.setResult("ROLLBACK");
            record.setTotalMilestones(project.getMilestones().size());
            record.setCompletedMilestones(0);

            iterationRecordRepository.save(record);
        } catch (Exception e) {
            log.warn("保存回滚记录失败: {}", e.getMessage());
        }
    }

    /**
     * 获取可回滚的版本列表
     *
     * @param projectId 项目ID
     * @return 版本历史列表
     */
    public List<Map<String, Object>> getRollbackableVersions(String projectId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return Collections.emptyList();
        }

        List<VersionHistory> history = project.getVersionHistory();
        if (history == null || history.isEmpty()) {
            return Collections.emptyList();
        }

        String currentVersion = project.getVersion();
        List<Map<String, Object>> versions = new ArrayList<>();

        for (VersionHistory h : history) {
            Map<String, Object> versionInfo = new HashMap<>();
            versionInfo.put("version", h.getVersion());
            versionInfo.put("description", h.getDescription());
            versionInfo.put("createdAt", h.getCreatedAt());
            versionInfo.put("isCurrent", h.getVersion().equals(currentVersion));
            versions.add(versionInfo);
        }

        return versions;
    }

    /**
     * 检查是否需要版本迭代
     * 综合判断当前版本是否已完成，是否需要进入下一版本
     *
     * @param projectId 项目ID
     * @return 0=未完成, 1=需要迭代, 2=目标完成
     */
    public int checkVersionIteration(String projectId) {
        // 检查当前版本是否完成
        if (!isCurrentVersionCompleted(projectId)) {
            // 记录迭代开始时间（如果还没有记录）
            iterationStartTimes.putIfAbsent(projectId, java.time.LocalDateTime.now());
            return 0; // 未完成
        }

        // 评估当前版本
        VersionEvaluationResult evaluation = evaluateVersion(projectId);
        log.info("版本评估结果: score={}, passed={}", evaluation.getScore(), evaluation.isPassed());

        if (!evaluation.isPassed()) {
            // 版本未通过验收，需要改进
            saveIterationRecord(projectId, evaluation, null, "IMPROVED");
            return 0;
        }

        // 版本通过验收，规划下一版本
        NextVersionPlan plan = planNextVersion(projectId, evaluation);

        if (plan.isNeedNextVersion()) {
            // 需要下一版本迭代
            upgradeVersion(projectId, plan);
            saveIterationRecord(projectId, evaluation, plan, "ITERATED");
            // 记录新版本迭代开始时间
            recordIterationStart(projectId);
            return 1; // 需要迭代
        } else {
            // 目标完成
            saveIterationRecord(projectId, evaluation, plan, "COMPLETED");
            // 清除迭代开始时间
            clearIterationStart(projectId);
            return 2; // 目标完成
        }
    }

    /**
     * 保存版本迭代记录
     */
    private void saveIterationRecord(String projectId, VersionEvaluationResult evaluation,
                                      NextVersionPlan plan, String result) {
        if (iterationRecordRepository == null) {
            log.debug("VersionIterationRecordRepository 未注入，跳过保存迭代记录");
            return;
        }

        try {
            GameProject project = projectManager.getProject(projectId);
            if (project == null) return;

            VersionIterationRecord record = new VersionIterationRecord(projectId, project.getVersion());
            record.setEvaluationScore(evaluation.getScore());
            record.setPassed(evaluation.isPassed());
            record.setEvaluationDetails(evaluation.getDetails());
            record.setRecommendation(evaluation.getRecommendation());
            record.setResult(result);

            // 保存优点和待改进
            if (evaluation.getStrengths() != null) {
                record.setStrengths(toJsonString(evaluation.getStrengths()));
            }
            if (evaluation.getImprovements() != null) {
                record.setImprovements(toJsonString(evaluation.getImprovements()));
            }

            // 保存里程碑统计
            List<GoalMilestone> milestones = project.getMilestones();
            record.setTotalMilestones(milestones.size());
            record.setCompletedMilestones((int) milestones.stream()
                .filter(m -> m.getStatus() == MilestoneStatus.COMPLETED).count());

            // 保存规划结果
            if (plan != null) {
                record.setNeedNextVersion(plan.isNeedNextVersion());
                record.setNextVersion(plan.getNextVersion());
                record.setNextGoal(plan.getNextGoal());
                record.setPlanReason(plan.getReason());
                if (plan.getNextMilestones() != null) {
                    record.setNextMilestones(toJsonString(plan.getNextMilestones()));
                }
            } else {
                record.setNeedNextVersion(false);
            }

            iterationRecordRepository.save(record);
            log.info("版本迭代记录已保存: project={}, version={}, result={}", projectId, project.getVersion(), result);
        } catch (Exception e) {
            log.warn("保存版本迭代记录失败: {}", e.getMessage());
        }
    }

    private String toJsonString(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    /**
     * 构建评估Prompt
     * 使用可配置的评估维度
     */
    private String buildEvaluationPrompt(String context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个游戏项目评估专家。请评估以下游戏项目的当前版本完成质量。\n\n");
        prompt.append(context).append("\n");
        prompt.append("## 评估要求\n");
        prompt.append("请从以下维度评估（1-10分）：\n");

        // 获取启用的评估维度
        List<VersionEvaluationDimension> dimensions = getEnabledDimensions();
        if (dimensions.isEmpty()) {
            // 默认维度
            prompt.append("1. 功能完整性：核心功能是否完整实现\n");
            prompt.append("2. 代码质量：代码结构、可维护性\n");
            prompt.append("3. 游戏体验：可玩性、流畅度\n");
            prompt.append("4. 目标达成度：是否达到项目目标\n");
        } else {
            int i = 1;
            for (VersionEvaluationDimension dim : dimensions) {
                prompt.append(i++).append(". ").append(dim.getDisplayName());
                if (dim.getDescription() != null) {
                    prompt.append("：").append(dim.getDescription());
                }
                prompt.append("（权重：").append(dim.getWeight()).append("%）\n");
                if (dim.getCriteria() != null) {
                    prompt.append("   评分标准：").append(dim.getCriteria()).append("\n");
                }
            }
        }

        prompt.append("\n## 输出格式（JSON）\n");
        prompt.append("{\n");
        prompt.append("  \"score\": 7,\n");
        prompt.append("  \"details\": \"评估详情...\",\n");
        prompt.append("  \"strengths\": [\"优点1\", \"优点2\"],\n");
        prompt.append("  \"improvements\": [\"改进1\", \"改进2\"],\n");
        prompt.append("  \"recommendation\": \"建议...\"\n");
        prompt.append("}");

        return prompt.toString();
    }

    /**
     * 获取启用的评估维度
     */
    private List<VersionEvaluationDimension> getEnabledDimensions() {
        if (dimensionRepository == null) {
            return Collections.emptyList();
        }
        try {
            return dimensionRepository.findByEnabledTrueOrderByDisplayOrderAsc();
        } catch (Exception e) {
            log.debug("获取评估维度失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 构建规划Prompt
     */
    private String buildPlanningPrompt(String context) {
        return "你是一个游戏项目规划专家。请根据以下信息判断是否需要下一版本迭代。\n\n" +
               context + "\n" +
               "## 规划要求\n" +
               "1. 如果当前版本质量优秀（8分以上）且目标基本达成，可以结束项目\n" +
               "2. 如果有明显的改进空间或未完成的重要功能，建议迭代\n" +
               "3. 版本号格式：X.Y.Z（主版本.次版本.修订号）\n\n" +
               "## 输出格式（JSON）\n" +
               "{\n" +
               "  \"needNextVersion\": true,\n" +
               "  \"nextVersion\": \"1.1.0\",\n" +
               "  \"nextGoal\": \"下一版本目标...\",\n" +
               "  \"nextMilestones\": [\"里程碑1\", \"里程碑2\"],\n" +
               "  \"reason\": \"规划理由...\"\n" +
               "}";
    }

    /**
     * 调用AI进行评估
     * AI服务不可用时，基于里程碑完成质量进行启发式评估
     */
    private String callAiForEvaluation(String prompt, GameProject project) {
        if (aiService != null) {
            try {
                return aiService.sendMessage(prompt);
            } catch (Exception e) {
                log.warn("AI评估调用失败，使用启发式评估: {}", e.getMessage());
            }
        }
        // AI服务不可用时，基于实际数据进行启发式评估
        return buildHeuristicEvaluation(project);
    }

    /**
     * 启发式评估：基于里程碑完成质量计算分数
     * 当AI服务不可用时的降级策略
     */
    private String buildHeuristicEvaluation(GameProject project) {
        List<GoalMilestone> milestones = project.getMilestones();
        int totalMilestones = milestones.size();
        int completedMilestones = (int) milestones.stream()
            .filter(m -> m.getStatus() == MilestoneStatus.COMPLETED).count();
        int milestonesWithTasks = (int) milestones.stream()
            .filter(m -> m.getTasks() != null && !m.getTasks().isEmpty()).count();
        int milestonesWithCriteria = (int) milestones.stream()
            .filter(m -> m.getVerificationCriteria() != null && !m.getVerificationCriteria().isEmpty()).count();

        // 基础分：完成比例 * 6
        double baseScore = totalMilestones > 0 ? (double) completedMilestones / totalMilestones * 6 : 0;
        // 任务分解加分：有任务的里程碑比例 * 2
        double taskScore = totalMilestones > 0 ? (double) milestonesWithTasks / totalMilestones * 2 : 0;
        // 验证标准加分：有验证标准的里程碑比例 * 2
        double criteriaScore = totalMilestones > 0 ? (double) milestonesWithCriteria / totalMilestones * 2 : 0;

        int score = Math.min(10, Math.max(1, (int) Math.round(baseScore + taskScore + criteriaScore)));

        List<String> strengths = new ArrayList<>();
        List<String> improvements = new ArrayList<>();

        if (completedMilestones == totalMilestones) {
            strengths.add("所有里程碑已完成");
        }
        if (milestonesWithTasks > totalMilestones / 2) {
            strengths.add("大部分里程碑有详细任务分解");
        }
        if (milestonesWithCriteria < totalMilestones) {
            improvements.add("部分里程碑缺少验证标准");
        }
        if (milestonesWithTasks < totalMilestones) {
            improvements.add("部分里程碑缺少任务分解");
        }

        String details = String.format("启发式评估：完成 %d/%d 里程碑，%d 个有任务分解，%d 个有验证标准",
            completedMilestones, totalMilestones, milestonesWithTasks, milestonesWithCriteria);

        return String.format(
            "{\"score\": %d, \"details\": \"%s\", \"strengths\": %s, \"improvements\": %s, \"recommendation\": \"%s\"}",
            score, details, toJsonArray(strengths), toJsonArray(improvements),
            score >= getVersionPassScore() ? "建议进行下一版本迭代" : "需要继续改进"
        );
    }

    private String toJsonArray(List<String> list) {
        return "[" + list.stream()
            .map(s -> "\"" + s.replace("\"", "\\\"") + "\"")
            .collect(Collectors.joining(", ")) + "]";
    }

    /**
     * 调用AI进行规划
     */
    private String callAiForPlanning(String prompt) {
        if (aiService != null) {
            try {
                return aiService.sendMessage(prompt);
            } catch (Exception e) {
                log.warn("AI规划调用失败，使用默认结果: {}", e.getMessage());
            }
        }
        // AI服务不可用时返回默认结果
        return "{\"needNextVersion\": true, \"nextVersion\": \"1.1.0\", " +
               "\"nextGoal\": \"优化游戏体验和UI\", " +
               "\"nextMilestones\": [\"UI优化\", \"性能优化\", \"新增功能\"], " +
               "\"reason\": \"当前版本基础功能完整，但有改进空间（AI服务不可用，使用默认规划）\"}";
    }

    /**
     * 解析评估响应
     */
    private VersionEvaluationResult parseEvaluationResponse(String response) {
        VersionEvaluationResult result = new VersionEvaluationResult();
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(response);

            int score = node.has("score") ? node.get("score").asInt() : 7;
            result.setScore(score);
            result.setPassed(score >= getVersionPassScore());
            result.setDetails(node.has("details") ? node.get("details").asText() : "");
            result.setRecommendation(node.has("recommendation") ? node.get("recommendation").asText() : "");

            if (node.has("strengths")) {
                List<String> strengths = new ArrayList<>();
                node.get("strengths").forEach(s -> strengths.add(s.asText()));
                result.setStrengths(strengths);
            }

            if (node.has("improvements")) {
                List<String> improvements = new ArrayList<>();
                node.get("improvements").forEach(s -> improvements.add(s.asText()));
                result.setImprovements(improvements);
            }
        } catch (Exception e) {
            log.warn("解析评估响应失败: {}", e.getMessage());
            result.setScore(7);
            result.setPassed(7 >= getVersionPassScore());
            result.setDetails("评估解析失败，默认通过");
        }
        return result;
    }

    /**
     * 解析规划响应
     */
    private NextVersionPlan parsePlanningResponse(String response, GameProject project) {
        NextVersionPlan plan = new NextVersionPlan();
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(response);

            plan.setNeedNextVersion(node.has("needNextVersion") && node.get("needNextVersion").asBoolean());
            plan.setNextVersion(node.has("nextVersion") ? node.get("nextVersion").asText() : incrementVersion(project.getVersion()));
            plan.setNextGoal(node.has("nextGoal") ? node.get("nextGoal").asText() : null);
            plan.setReason(node.has("reason") ? node.get("reason").asText() : "");

            if (node.has("nextMilestones")) {
                List<String> milestones = new ArrayList<>();
                node.get("nextMilestones").forEach(m -> milestones.add(m.asText()));
                plan.setNextMilestones(milestones);
            }
        } catch (Exception e) {
            log.warn("解析规划响应失败: {}", e.getMessage());
            // 默认需要下一版本
            plan.setNeedNextVersion(true);
            plan.setNextVersion(incrementVersion(project.getVersion()));
            plan.setReason("规划解析失败，默认继续迭代");
        }
        return plan;
    }

    /**
     * 自动递增版本号
     * 1.0.0 -> 1.1.0 -> 1.2.0 -> ... -> 2.0.0
     */
    private String incrementVersion(String currentVersion) {
        if (currentVersion == null || currentVersion.isEmpty()) {
            return "1.1.0";
        }
        try {
            String[] parts = currentVersion.replace("v", "").split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

            // 递增次版本号
            minor++;

            return major + "." + minor + "." + patch;
        } catch (Exception e) {
            log.warn("版本号解析失败: {}", currentVersion);
            return "1.1.0";
        }
    }
}
