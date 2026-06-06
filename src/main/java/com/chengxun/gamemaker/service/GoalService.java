package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.model.GameProject.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 目标管理服务
 * 管理项目目标的生命周期：创建、分解、进度更新、完成检测
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class GoalService {

    private static final Logger log = LoggerFactory.getLogger(GoalService.class);

    private final ProjectManager projectManager;

    public GoalService(ProjectManager projectManager) {
        this.projectManager = projectManager;
    }

    /**
     * 为项目创建目标
     *
     * @param projectId 项目 ID
     * @param goal 目标描述
     * @param goalType 目标类型
     * @param deadline 截止时间（可选）
     * @return 更新后的项目
     */
    public GameProject createGoal(String projectId, String goal, GoalType goalType, LocalDateTime deadline) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            throw new IllegalArgumentException("项目不存在: " + projectId);
        }

        project.setGoal(goal);
        project.setGoalType(goalType);
        project.setGoalStatus(GoalStatus.NOT_STARTED);
        project.setGoalProgress(0);
        project.setGoalDeadline(deadline);
        project.setMilestones(new ArrayList<>());
        project.touch();

        projectManager.saveProjectConfig(project);
        log.info("Goal created for project {}: {}", projectId, goal);
        return project;
    }

    /**
     * 添加里程碑
     */
    public GoalMilestone addMilestone(String projectId, String title, String description,
                                       String assignedAgentRole, int order) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) throw new IllegalArgumentException("项目不存在: " + projectId);

        GoalMilestone milestone = new GoalMilestone(
            UUID.randomUUID().toString(),
            title,
            assignedAgentRole,
            order
        );
        milestone.setDescription(description);

        project.addMilestone(milestone);
        project.touch();
        projectManager.saveProjectConfig(project);

        log.info("Milestone added to project {}: {} -> {}", projectId, milestone.getId(), title);
        return milestone;
    }

    /**
     * 添加里程碑（带依赖）
     */
    public GoalMilestone addMilestone(String projectId, String title, String description,
                                       String assignedAgentRole, int order, List<String> dependencies) {
        GoalMilestone milestone = addMilestone(projectId, title, description, assignedAgentRole, order);
        milestone.setDependencies(dependencies);
        projectManager.saveProjectConfig(projectManager.getProject(projectId));
        return milestone;
    }

    /**
     * 为里程碑添加任务
     */
    public MilestoneTask addTask(String projectId, String milestoneId, String taskDescription) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) throw new IllegalArgumentException("项目不存在: " + projectId);

        GoalMilestone milestone = project.getMilestones().stream()
            .filter(m -> m.getId().equals(milestoneId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("里程碑不存在: " + milestoneId));

        MilestoneTask task = new MilestoneTask(
            UUID.randomUUID().toString(),
            taskDescription
        );
        milestone.addTask(task);
        project.touch();
        projectManager.saveProjectConfig(project);

        log.info("Task added to milestone {}: {}", milestoneId, taskDescription);
        return task;
    }

    /**
     * 更新里程碑进度
     */
    public void updateMilestoneProgress(String projectId, String milestoneId, int progress) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) return;

        GoalMilestone milestone = project.getMilestones().stream()
            .filter(m -> m.getId().equals(milestoneId))
            .findFirst()
            .orElse(null);

        if (milestone == null) return;

        milestone.setProgress(Math.max(0, Math.min(100, progress)));

        // 自动更新里程碑状态
        if (progress >= 100) {
            milestone.setStatus(MilestoneStatus.COMPLETED);
        } else if (progress > 0) {
            milestone.setStatus(MilestoneStatus.IN_PROGRESS);
        }

        // 重新计算项目目标总进度
        project.recalculateGoalProgress();

        // 检查目标是否完成
        if (project.isGoalCompleted()) {
            project.setGoalStatus(GoalStatus.COMPLETED);
            project.setStatus(GameProject.ProjectStatus.COMPLETED);
            log.info("Goal completed for project {}!", projectId);
        } else if (project.getGoalStatus() == GoalStatus.NOT_STARTED) {
            project.setGoalStatus(GoalStatus.IN_PROGRESS);
        }

        project.touch();
        projectManager.saveProjectConfig(project);
        log.info("Milestone progress updated: {} -> {}%", milestoneId, progress);
    }

    /**
     * 更新任务状态
     */
    public void updateTaskStatus(String projectId, String milestoneId, String taskId,
                                  MilestoneStatus status, String result) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) return;

        GoalMilestone milestone = project.getMilestones().stream()
            .filter(m -> m.getId().equals(milestoneId))
            .findFirst()
            .orElse(null);

        if (milestone == null) return;

        MilestoneTask task = milestone.getTasks().stream()
            .filter(t -> t.getId().equals(taskId))
            .findFirst()
            .orElse(null);

        if (task == null) return;

        task.setStatus(status);
        if (result != null) task.setResult(result);

        // 重新计算里程碑进度
        if (!milestone.getTasks().isEmpty()) {
            long completed = milestone.getTasks().stream()
                .filter(t -> t.getStatus() == MilestoneStatus.COMPLETED)
                .count();
            int progress = (int) (completed * 100 / milestone.getTasks().size());
            milestone.setProgress(progress);
        }

        project.recalculateGoalProgress();
        project.touch();
        projectManager.saveProjectConfig(project);
    }

    /**
     * 暂停目标
     */
    public void pauseGoal(String projectId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null || !project.hasGoal()) return;

        project.setGoalStatus(GoalStatus.PAUSED);
        project.touch();
        projectManager.saveProjectConfig(project);
        log.info("Goal paused for project: {}", projectId);
    }

    /**
     * 恢复目标
     */
    public void resumeGoal(String projectId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null || !project.hasGoal()) return;

        if (project.getGoalStatus() == GoalStatus.PAUSED) {
            project.setGoalStatus(GoalStatus.IN_PROGRESS);
            project.touch();
            projectManager.saveProjectConfig(project);
            log.info("Goal resumed for project: {}", projectId);
        }
    }

    /**
     * 获取项目目标信息
     */
    public GameProject getGoalInfo(String projectId) {
        return projectManager.getProject(projectId);
    }

    /**
     * 获取项目的所有里程碑
     *
     * @param projectId 项目 ID
     * @return 里程碑列表，如果项目不存在返回空列表
     */
    public List<GoalMilestone> getMilestones(String projectId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null || project.getMilestones() == null) {
            return new ArrayList<>();
        }
        return project.getMilestones();
    }

    /**
     * 获取下一个可执行的里程碑
     */
    public GoalMilestone getNextExecutableMilestone(String projectId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null || !project.hasGoal()) return null;
        return project.getNextMilestone();
    }

    /**
     * 检查目标是否应该进入审查状态
     */
    public boolean shouldEnterReview(String projectId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null || !project.hasGoal()) return false;
        return project.isGoalCompleted() && project.getGoalStatus() != GoalStatus.COMPLETED;
    }

    /**
     * T17: 定期检查目标截止时间（每小时）
     * 超期目标记录日志
     */
    @Scheduled(fixedRate = 3600000, initialDelay = 60000)
    public void checkGoalDeadlines() {
        for (GameProject project : projectManager.getAllProjects()) {
            if (project.getGoalDeadline() == null) continue;
            if (!project.isGoalActive()) continue;

            if (LocalDateTime.now().isAfter(project.getGoalDeadline())) {
                log.warn("项目 [{}] 目标已超期！截止时间: {}, 当前进度: {}%",
                    project.getName(), project.getGoalDeadline(), project.getGoalProgress());
            }
        }
    }
}
