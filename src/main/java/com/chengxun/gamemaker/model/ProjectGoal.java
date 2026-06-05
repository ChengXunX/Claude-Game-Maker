package com.chengxun.gamemaker.model;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 项目目标模型
 * 从 GameProject 中抽取的目标相关字段，降低 GameProject 复杂度
 *
 * @author chengxun
 * @since 1.0.0
 */
public class ProjectGoal {

    /** 目标描述 */
    private String goal;

    /** 目标类型 */
    private GameProject.GoalType goalType;

    /** 目标状态 */
    private GameProject.GoalStatus goalStatus = GameProject.GoalStatus.NOT_STARTED;

    /** 目标进度（0-100） */
    private int progress = 0;

    /** 截止时间 */
    private LocalDateTime deadline;

    /** 里程碑列表 */
    private List<GameProject.GoalMilestone> milestones = new ArrayList<>();

    public ProjectGoal() {}

    public ProjectGoal(String goal, GameProject.GoalType goalType) {
        this.goal = goal;
        this.goalType = goalType;
    }

    // Getters and Setters
    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }

    public GameProject.GoalType getGoalType() { return goalType; }
    public void setGoalType(GameProject.GoalType goalType) { this.goalType = goalType; }

    public GameProject.GoalStatus getGoalStatus() { return goalStatus; }
    public void setGoalStatus(GameProject.GoalStatus goalStatus) { this.goalStatus = goalStatus; }

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }

    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }

    public List<GameProject.GoalMilestone> getMilestones() { return milestones; }
    public void setMilestones(List<GameProject.GoalMilestone> milestones) { this.milestones = milestones; }

    public boolean hasGoal() {
        return goal != null && !goal.isEmpty();
    }

    public boolean isActive() {
        return goalStatus == GameProject.GoalStatus.IN_PROGRESS
            || goalStatus == GameProject.GoalStatus.DECOMPOSING;
    }

    public boolean isCompleted() {
        return !milestones.isEmpty()
            && milestones.stream().allMatch(m -> m.getStatus() == GameProject.MilestoneStatus.COMPLETED);
    }

    public void addMilestone(GameProject.GoalMilestone milestone) {
        this.milestones.add(milestone);
    }

    public GameProject.GoalMilestone getNextMilestone() {
        return milestones.stream()
            .filter(m -> m.getStatus() == GameProject.MilestoneStatus.PENDING
                && m.areDependenciesMet(milestones))
            .min(Comparator.comparingInt(GameProject.GoalMilestone::getOrder))
            .orElse(null);
    }

    public void recalculateProgress() {
        if (milestones.isEmpty()) {
            this.progress = 0;
            return;
        }
        int total = milestones.stream().mapToInt(GameProject.GoalMilestone::getProgress).sum();
        this.progress = total / milestones.size();
    }
}
