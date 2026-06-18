package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.web.entity.QualityPrediction;
import com.chengxun.gamemaker.web.entity.VersionIterationRecord;
import com.chengxun.gamemaker.web.repository.QualityPredictionRepository;
import com.chengxun.gamemaker.web.repository.VersionIterationRecordRepository;
import com.chengxun.gamemaker.web.service.AgentHealthService;
import com.chengxun.gamemaker.web.entity.AgentHealth;
import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.model.GameProject.GoalMilestone;
import com.chengxun.gamemaker.model.GameProject.MilestoneStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 质量预测服务
 * 基于历史数据和当前项目状态预测版本验收通过率
 *
 * 预测因子：
 * 1. 历史通过率 — 过去版本的验收通过率
 * 2. 里程碑完成率 — 当前版本的里程碑完成比例
 * 3. 验证失败次数 — 里程碑验证失败的累计次数
 * 4. Agent 平均错误率 — 项目内 Agent 的平均错误率
 * 5. 迭代次数 — 当前是第几次迭代（过多可能说明质量不稳定）
 *
 * @author chengxun
 * @since 3.0.0
 */
@Service
@Transactional
public class QualityPredictionService {

    private static final Logger log = LoggerFactory.getLogger(QualityPredictionService.class);

    @Autowired
    private QualityPredictionRepository predictionRepository;

    @Autowired(required = false)
    private VersionIterationRecordRepository iterationRecordRepository;

    @Autowired
    private ProjectManager projectManager;

    @Autowired(required = false)
    private AgentHealthService agentHealthService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 预测当前版本的通过概率
     */
    public QualityPrediction predict(String projectId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            QualityPrediction pred = new QualityPrediction();
            pred.setProjectId(projectId);
            pred.setPassProbability(0);
            pred.setRiskLevel("CRITICAL");
            pred.setRiskFactors("[\"项目不存在\"]");
            return pred;
        }

        // 计算各因子
        double historicalPassRate = calcHistoricalPassRate(projectId);
        double milestoneCompletionRate = calcMilestoneCompletionRate(project);
        int verificationFailCount = calcVerificationFailCount(project);
        double avgAgentErrorRate = calcAvgAgentErrorRate(projectId);
        int iterationCount = project.getVersionCount();

        // 加权计算通过概率
        // 权重：历史通过率30% + 里程碑完成率30% + 验证失败20% + Agent错误率10% + 迭代次数10%
        double score = 0;
        score += historicalPassRate * 0.30;
        score += milestoneCompletionRate * 0.30;
        score += Math.max(0, 100 - verificationFailCount * 10) * 0.20;  // 每次失败扣10分
        score += Math.max(0, 100 - avgAgentErrorRate) * 0.10;  // 错误率越高扣分越多
        score += Math.max(0, 100 - Math.max(0, iterationCount - 5) * 10) * 0.10;  // 超过5次迭代开始扣分

        int passProbability = Math.min(100, Math.max(0, (int) Math.round(score)));

        // 确定风险等级
        String riskLevel;
        if (passProbability >= 80) riskLevel = "LOW";
        else if (passProbability >= 60) riskLevel = "MEDIUM";
        else if (passProbability >= 40) riskLevel = "HIGH";
        else riskLevel = "CRITICAL";

        // 生成风险因素和改进建议
        List<String> riskFactors = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        if (historicalPassRate < 50) {
            riskFactors.add("历史通过率较低 (" + String.format("%.0f%%", historicalPassRate) + ")");
            suggestions.add("回顾历史失败原因，总结经验教训");
        }
        if (milestoneCompletionRate < 80) {
            riskFactors.add("里程碑完成率不足 (" + String.format("%.0f%%", milestoneCompletionRate) + ")");
            suggestions.add("集中力量完成剩余里程碑");
        }
        if (verificationFailCount > 3) {
            riskFactors.add("验证失败次数过多 (" + verificationFailCount + "次)");
            suggestions.add("分析验证失败原因，改进实现质量");
        }
        if (avgAgentErrorRate > 20) {
            riskFactors.add("Agent错误率偏高 (" + String.format("%.1f%%", avgAgentErrorRate) + ")");
            suggestions.add("检查Agent配置，排查错误根因");
        }
        if (iterationCount > 7) {
            riskFactors.add("迭代次数过多 (" + iterationCount + "次)，质量可能不稳定");
            suggestions.add("考虑收敛功能，聚焦核心质量");
        }

        if (riskFactors.isEmpty()) {
            riskFactors.add("当前无明显风险因素");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("保持当前节奏，继续推进");
        }

        // 构建因子详情
        Map<String, Object> factorsDetail = new LinkedHashMap<>();
        factorsDetail.put("historicalPassRate", Math.round(historicalPassRate * 10) / 10.0);
        factorsDetail.put("milestoneCompletionRate", Math.round(milestoneCompletionRate * 10) / 10.0);
        factorsDetail.put("verificationFailCount", verificationFailCount);
        factorsDetail.put("avgAgentErrorRate", Math.round(avgAgentErrorRate * 10) / 10.0);
        factorsDetail.put("iterationCount", iterationCount);

        // 保存预测记录
        QualityPrediction prediction = new QualityPrediction();
        prediction.setProjectId(projectId);
        prediction.setProjectName(project.getName());
        prediction.setVersion(project.getVersion());
        prediction.setPassProbability(passProbability);
        prediction.setRiskLevel(riskLevel);
        prediction.setHistoricalPassRate(historicalPassRate);
        prediction.setMilestoneCompletionRate(milestoneCompletionRate);
        prediction.setVerificationFailCount(verificationFailCount);
        prediction.setAvgAgentErrorRate(avgAgentErrorRate);
        try {
            prediction.setRiskFactors(objectMapper.writeValueAsString(riskFactors));
            prediction.setImprovementSuggestions(objectMapper.writeValueAsString(suggestions));
            prediction.setFactorsDetail(objectMapper.writeValueAsString(factorsDetail));
        } catch (Exception e) {
            prediction.setRiskFactors("[]");
            prediction.setImprovementSuggestions("[]");
        }

        QualityPrediction saved = predictionRepository.save(prediction);
        log.info("质量预测完成: project={}, probability={}, risk={}", projectId, passProbability, riskLevel);
        return saved;
    }

    /**
     * 获取预测历史
     */
    public List<QualityPrediction> getHistory(String projectId) {
        return predictionRepository.findHistoryByProject(projectId);
    }

    /**
     * 获取最新预测
     */
    public QualityPrediction getLatest(String projectId) {
        return predictionRepository.findFirstByProjectIdOrderByCreatedAtDesc(projectId).orElse(null);
    }

    private double calcHistoricalPassRate(String projectId) {
        if (iterationRecordRepository == null) return 50.0; // 默认50%
        try {
            long total = iterationRecordRepository.countByProjectId(projectId);
            if (total == 0) return 50.0;
            long passed = iterationRecordRepository.countByProjectIdAndPassed(projectId, true);
            return (double) passed / total * 100;
        } catch (Exception e) {
            return 50.0;
        }
    }

    private double calcMilestoneCompletionRate(GameProject project) {
        List<GoalMilestone> milestones = project.getMilestones();
        if (milestones == null || milestones.isEmpty()) return 0;
        long completed = milestones.stream().filter(m -> m.getStatus() == MilestoneStatus.COMPLETED).count();
        return (double) completed / milestones.size() * 100;
    }

    private int calcVerificationFailCount(GameProject project) {
        int total = 0;
        if (project.getMilestones() != null) {
            for (GoalMilestone m : project.getMilestones()) {
                if (m.getVerificationFailCount() > 0) total += m.getVerificationFailCount();
            }
        }
        return total;
    }

    private double calcAvgAgentErrorRate(String projectId) {
        if (agentHealthService == null) return 0;
        try {
            List<AgentHealth> healthList = agentHealthService.getProjectAgentHealth(projectId);
            if (healthList.isEmpty()) return 0;
            double totalRate = healthList.stream()
                .mapToDouble(h -> h.getErrorRate() != null ? h.getErrorRate() : 0)
                .sum();
            return totalRate / healthList.size();
        } catch (Exception e) {
            return 0;
        }
    }
}
