package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.web.entity.IterationAdaptRecord;
import com.chengxun.gamemaker.web.repository.IterationAdaptRecordRepository;
import com.chengxun.gamemaker.web.service.SystemConstantService;
import com.chengxun.gamemaker.config.SystemConstants;
import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.model.GameProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 迭代适应服务
 * 根据项目阶段（早期/中期/后期）动态调整迭代策略
 *
 * 阶段划分：
 * - EARLY（v1-v3）：快速验证核心玩法，低阈值（5分），全量迭代
 * - MID（v4-v7）：功能完善和优化，标准阈值（7分），自适应迭代
 * - LATE（v8+）：稳定性和质量，高阈值（8分），增量迭代
 *
 * @author chengxun
 * @since 3.0.0
 */
@Service
@Transactional
public class IterationAdaptService {

    private static final Logger log = LoggerFactory.getLogger(IterationAdaptService.class);

    @Autowired
    private IterationAdaptRecordRepository adaptRecordRepository;

    @Autowired
    private ProjectManager projectManager;

    @Autowired(required = false)
    private SystemConstantService constantService;

    /**
     * 判断项目阶段
     */
    public IterationAdaptRecord.ProjectPhase determineProjectPhase(String projectId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) return IterationAdaptRecord.ProjectPhase.EARLY;

        int versionCount = project.getVersionCount();
        double completionRate = calcMilestoneCompletionRate(project);

        if (versionCount <= 3) {
            return IterationAdaptRecord.ProjectPhase.EARLY;
        } else if (versionCount <= 7) {
            return IterationAdaptRecord.ProjectPhase.MID;
        } else {
            return IterationAdaptRecord.ProjectPhase.LATE;
        }
    }

    /**
     * 获取推荐的迭代策略
     */
    public Map<String, Object> getRecommendation(String projectId) {
        Map<String, Object> result = new HashMap<>();
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            result.put("error", "项目不存在");
            return result;
        }

        IterationAdaptRecord.ProjectPhase phase = determineProjectPhase(projectId);
        String currentStrategy = getIterationStrategy();
        int currentPassScore = getVersionPassScore();

        String recommendedStrategy;
        int recommendedPassScore;
        String reason;

        switch (phase) {
            case EARLY:
                recommendedStrategy = "full";
                recommendedPassScore = 5;
                reason = "早期阶段：快速验证核心玩法，使用全量迭代和低阈值";
                break;
            case MID:
                recommendedStrategy = "adaptive";
                recommendedPassScore = 7;
                reason = "中期阶段：功能完善，使用自适应迭代和标准阈值";
                break;
            case LATE:
                recommendedStrategy = "incremental";
                recommendedPassScore = 8;
                reason = "后期阶段：稳定性优先，使用增量迭代和高阈值";
                break;
            default:
                recommendedStrategy = "adaptive";
                recommendedPassScore = 7;
                reason = "默认策略";
        }

        result.put("projectId", projectId);
        result.put("projectName", project.getName());
        result.put("version", project.getVersion());
        result.put("versionCount", project.getVersionCount());
        result.put("phase", phase.name());
        result.put("currentStrategy", currentStrategy);
        result.put("currentPassScore", currentPassScore);
        result.put("recommendedStrategy", recommendedStrategy);
        result.put("recommendedPassScore", recommendedPassScore);
        result.put("reason", reason);
        result.put("needsChange", !currentStrategy.equals(recommendedStrategy) || currentPassScore != recommendedPassScore);

        return result;
    }

    /**
     * 应用推荐的迭代策略
     */
    public IterationAdaptRecord applyRecommendation(String projectId) {
        Map<String, Object> recommendation = getRecommendation(projectId);
        if (Boolean.FALSE.equals(recommendation.get("needsChange"))) {
            log.info("项目 {} 当前策略已是最优，无需调整", projectId);
            return null;
        }

        String oldStrategy = (String) recommendation.get("currentStrategy");
        String newStrategy = (String) recommendation.get("recommendedStrategy");
        int oldPassScore = (int) recommendation.get("currentPassScore");
        int newPassScore = (int) recommendation.get("recommendedPassScore");
        IterationAdaptRecord.ProjectPhase phase = IterationAdaptRecord.ProjectPhase.valueOf((String) recommendation.get("phase"));
        String reason = (String) recommendation.get("reason");
        GameProject project = projectManager.getProject(projectId);

        // 更新系统常量
        if (constantService != null) {
            try {
                constantService.update(SystemConstants.VERSION_ITERATION_STRATEGY, newStrategy);
                constantService.update(SystemConstants.VERSION_PASS_SCORE, String.valueOf(newPassScore));
            } catch (Exception e) {
                log.warn("更新系统常量失败: {}", e.getMessage());
            }
        }

        // 保存记录
        IterationAdaptRecord record = new IterationAdaptRecord();
        record.setProjectId(projectId);
        record.setPhase(phase);
        record.setVersion(project != null ? project.getVersion() : "?");
        record.setIterationCount(project != null ? project.getVersionCount() : 0);
        record.setOldStrategy(oldStrategy);
        record.setNewStrategy(newStrategy);
        record.setOldPassScore(oldPassScore);
        record.setNewPassScore(newPassScore);
        record.setReason(reason);
        record.setApplied(true);

        IterationAdaptRecord saved = adaptRecordRepository.save(record);
        log.info("迭代策略已调整: project={}, {} -> {}, passScore {} -> {}",
            projectId, oldStrategy, newStrategy, oldPassScore, newPassScore);
        return saved;
    }

    /**
     * 获取调整历史
     */
    public List<IterationAdaptRecord> getHistory(String projectId) {
        return adaptRecordRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    private String getIterationStrategy() {
        if (constantService != null) {
            return constantService.getString(SystemConstants.VERSION_ITERATION_STRATEGY, "adaptive");
        }
        return "adaptive";
    }

    private int getVersionPassScore() {
        if (constantService != null) {
            return constantService.getInt(SystemConstants.VERSION_PASS_SCORE, 7);
        }
        return 7;
    }

    private double calcMilestoneCompletionRate(GameProject project) {
        if (project.getMilestones() == null || project.getMilestones().isEmpty()) return 0;
        long completed = project.getMilestones().stream()
            .filter(m -> m.getStatus() == GameProject.MilestoneStatus.COMPLETED).count();
        return (double) completed / project.getMilestones().size() * 100;
    }
}
