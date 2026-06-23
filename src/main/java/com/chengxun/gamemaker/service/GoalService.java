package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.model.GameProject.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    /** 里程碑状态更新冷却时间（秒）- 防止短时间内重复更新 */
    private static final int MILESTONE_UPDATE_COOLDOWN_SECONDS = 60;

    /** 里程碑状态更新时间记录，key = projectId:milestoneId */
    private final Map<String, Long> milestoneUpdateCooldowns = new ConcurrentHashMap<>();

    private final ProjectManager projectManager;
    private final AgentManager agentManager;

    public GoalService(ProjectManager projectManager, @Lazy AgentManager agentManager) {
        this.projectManager = projectManager;
        this.agentManager = agentManager;
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

        // 去重检查：如果已存在同名里程碑，返回现有的
        GoalMilestone existing = project.getMilestones().stream()
            .filter(m -> m.getTitle() != null && m.getTitle().equals(title))
            .findFirst()
            .orElse(null);
        if (existing != null) {
            log.info("里程碑已存在，跳过创建: {} -> {}", existing.getId(), title);
            return existing;
        }

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
        return addTask(projectId, milestoneId, taskDescription, null, null, null);
    }

    /**
     * 为里程碑添加任务（完整参数）
     *
     * @param projectId 项目 ID
     * @param milestoneIdOrTitle 里程碑 ID 或标题
     * @param title 任务标题
     * @param description 任务描述（可选）
     * @param assignedRole 负责角色（可选）
     * @param priority 优先级（可选：HIGH, MEDIUM, LOW）
     * @return 创建的任务
     */
    public MilestoneTask addTask(String projectId, String milestoneIdOrTitle, String title,
                                  String description, String assignedRole, String priority) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) throw new IllegalArgumentException("项目不存在: " + projectId);

        GoalMilestone milestone = findMilestone(project, milestoneIdOrTitle);
        if (milestone == null) throw new IllegalArgumentException("里程碑不存在: " + milestoneIdOrTitle);

        MilestoneTask task = new MilestoneTask(UUID.randomUUID().toString(), title);
        if (description != null && !description.isEmpty()) task.setDescription(description);
        if (assignedRole != null && !assignedRole.isEmpty()) task.setAssignedRole(assignedRole);
        if (priority != null && !priority.isEmpty()) task.setPriority(priority);

        milestone.addTask(task);
        project.touch();
        projectManager.saveProjectConfig(project);

        log.info("Task added to milestone [{}]: {} (role={}, priority={})",
            milestone.getTitle(), title, assignedRole, priority);
        return task;
    }

    /**
     * 更新任务信息
     *
     * @param projectId 项目 ID
     * @param milestoneIdOrTitle 里程碑 ID 或标题
     * @param taskId 任务 ID
     * @param title 新标题（可选）
     * @param description 新描述（可选）
     * @param assignedRole 新负责角色（可选）
     * @param priority 新优先级（可选）
     * @return 是否更新成功
     */
    public boolean updateTask(String projectId, String milestoneIdOrTitle, String taskId,
                               String title, String description, String assignedRole, String priority) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) return false;

        GoalMilestone milestone = findMilestone(project, milestoneIdOrTitle);
        if (milestone == null) return false;

        MilestoneTask task = milestone.getTasks().stream()
            .filter(t -> taskId.equals(t.getId()))
            .findFirst().orElse(null);
        if (task == null) return false;

        if (title != null && !title.isEmpty()) task.setTitle(title);
        if (description != null && !description.isEmpty()) task.setDescription(description);
        if (assignedRole != null && !assignedRole.isEmpty()) task.setAssignedRole(assignedRole);
        if (priority != null && !priority.isEmpty()) task.setPriority(priority);

        project.touch();
        projectManager.saveProjectConfig(project);
        log.info("Task updated: {} in milestone [{}]", task.getTitle(), milestone.getTitle());
        return true;
    }

    /**
     * 更新里程碑进度
     *
     * 【重要】当进度为100%时，必须验证里程碑是否真正完成
     * 不能仅仅因为Agent报告"完成"就标记为完成
     */
    public void updateMilestoneProgress(String projectId, String milestoneId, int progress) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) return;

        GoalMilestone milestone = project.getMilestones().stream()
            .filter(m -> m.getId().equals(milestoneId))
            .findFirst()
            .orElse(null);

        if (milestone == null) return;

        // 【重要】当进度为100%时，检查是否有验证结果
        if (progress >= 100) {
            // 如果没有验证结果，不允许直接设置为100%
            if (milestone.getVerificationResult() == null || milestone.getVerificationResult().isEmpty()) {
                log.warn("里程碑 [{}] 尝试设置为100%但没有验证结果，拒绝操作", milestone.getTitle());
                return;
            }

            // 检查验证结果是否包含"通过"关键词
            if (!milestone.getVerificationResult().contains("通过") && !milestone.getVerificationResult().contains("success")) {
                log.warn("里程碑 [{}] 尝试设置为100%但验证结果不是通过状态，拒绝操作", milestone.getTitle());
                return;
            }
        }

        milestone.setProgress(Math.max(0, Math.min(100, progress)));

        // 自动更新里程碑状态
        if (progress >= 100) {
            milestone.setStatus(MilestoneStatus.COMPLETED);
            // 【修复】同步将所有未完成任务标记为 COMPLETED，确保任务状态一致
            if (milestone.getTasks() != null) {
                for (MilestoneTask task : milestone.getTasks()) {
                    if (task.getStatus() != MilestoneStatus.COMPLETED) {
                        task.setStatus(MilestoneStatus.COMPLETED);
                        task.setResult("里程碑进度100%，自动完成");
                        if (task.getCompletedAt() == null) {
                            task.setCompletedAt(LocalDateTime.now());
                        }
                    }
                }
            }
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
     * 从能力系统调用的里程碑进度更新方法
     * 不检查验证结果，允许能力系统直接更新进度
     *
     * @param projectId 项目 ID
     * @param milestoneId 里程碑 ID
     * @param progress 进度 (0-100)
     * @return 是否更新成功
     */
    public boolean updateMilestoneProgressFromCapability(String projectId, String milestoneIdOrTitle, int progress) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            log.warn("updateMilestoneProgressFromCapability: 项目不存在 {}", projectId);
            return false;
        }

        // 先按 ID 查找，再按标题查找，最后按序号查找
        GoalMilestone milestone = findMilestone(project, milestoneIdOrTitle);

        if (milestone == null) {
            log.warn("updateMilestoneProgressFromCapability: 里程碑不存在 '{}', 项目: {}, 可用里程碑: {}",
                milestoneIdOrTitle, projectId,
                project.getMilestones().stream().map(m -> m.getTitle() + "(ID:" + m.getId() + ")").toList());
            return false;
        }

        // 防抖：同一里程碑在冷却时间内不能重复更新进度
        String cooldownKey = projectId + ":" + milestone.getId() + ":progress";
        Long lastUpdate = milestoneUpdateCooldowns.get(cooldownKey);
        long now = System.currentTimeMillis();
        if (lastUpdate != null && (now - lastUpdate) < MILESTONE_UPDATE_COOLDOWN_SECONDS * 1000L) {
            long remaining = (MILESTONE_UPDATE_COOLDOWN_SECONDS * 1000L - (now - lastUpdate)) / 1000;
            log.warn("里程碑 [{}] 进度更新被拒绝：冷却中，还需等待 {} 秒", milestone.getTitle(), remaining);
            return false;
        }

        milestone.setProgress(Math.max(0, Math.min(100, progress)));

        // 自动更新里程碑状态
        if (progress >= 100) {
            milestone.setStatus(GameProject.MilestoneStatus.COMPLETED);
            if (milestone.getVerificationResult() == null || milestone.getVerificationResult().isEmpty()) {
                milestone.setVerificationResult("能力系统更新进度至100%");
                milestone.setLastVerificationTime(java.time.LocalDateTime.now().toString());
            }
            // 同步将所有未完成任务标记为 COMPLETED
            for (GameProject.MilestoneTask task : milestone.getTasks()) {
                if (task.getStatus() != GameProject.MilestoneStatus.COMPLETED) {
                    task.setStatus(GameProject.MilestoneStatus.COMPLETED);
                    task.setResult("里程碑进度100%，自动完成");
                    if (task.getCompletedAt() == null) {
                        task.setCompletedAt(java.time.LocalDateTime.now());
                    }
                }
            }
            log.info("里程碑 [{}] 进度100%，已标记为 COMPLETED 并同步任务状态", milestone.getTitle());
        } else if (progress > 0) {
            milestone.setStatus(GameProject.MilestoneStatus.IN_PROGRESS);
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

        // 记录冷却时间
        milestoneUpdateCooldowns.put(cooldownKey, now);

        log.info("里程碑进度已更新: {} (ID: {}) -> {}%", milestone.getTitle(), milestone.getId(), progress);
        return true;
    }

    /**
     * 智能查找里程碑
     * 支持多种匹配方式：ID、标题、序号（如 milestone_1、里程碑1）
     *
     * @param project 项目
     * @param idOrTitle 里程碑 ID、标题或序号
     * @return 匹配的里程碑，未找到返回 null
     */
    private GoalMilestone findMilestone(GameProject project, String idOrTitle) {
        if (idOrTitle == null || idOrTitle.isEmpty()) return null;
        java.util.List<GoalMilestone> milestones = project.getMilestones();
        if (milestones == null || milestones.isEmpty()) return null;

        // 1. 按精确 ID 匹配
        GoalMilestone found = milestones.stream()
            .filter(m -> idOrTitle.equals(m.getId()))
            .findFirst().orElse(null);
        if (found != null) return found;

        // 2. 按标题精确匹配
        found = milestones.stream()
            .filter(m -> idOrTitle.equals(m.getTitle()))
            .findFirst().orElse(null);
        if (found != null) return found;

        // 3. 按标题模糊匹配（包含）
        found = milestones.stream()
            .filter(m -> m.getTitle() != null && m.getTitle().contains(idOrTitle))
            .findFirst().orElse(null);
        if (found != null) return found;

        // 4. 按序号匹配（如 "milestone_1" → 第1个里程碑，"里程碑2" → 第2个）
        try {
            String numStr = idOrTitle.replaceAll("[^0-9]", "");
            if (!numStr.isEmpty()) {
                int index = Integer.parseInt(numStr) - 1; // 转为 0-based
                if (index >= 0 && index < milestones.size()) {
                    return milestones.get(index);
                }
            }
        } catch (NumberFormatException ignored) {}

        return null;
    }

    /**
     * 根据任务 ID 查找所属的里程碑 ID
     *
     * @param projectId 项目 ID
     * @param taskId 任务 ID
     * @return 里程碑 ID，未找到返回 null
     */
    public String findMilestoneIdByTaskId(String projectId, String taskId) {
        if (projectId == null || taskId == null) return null;

        GameProject project = projectManager.getProject(projectId);
        if (project == null || project.getMilestones() == null) return null;

        for (GoalMilestone milestone : project.getMilestones()) {
            if (milestone.getTasks() != null) {
                for (GameProject.MilestoneTask task : milestone.getTasks()) {
                    if (taskId.equals(task.getId())) {
                        return milestone.getId();
                    }
                }
            }
        }
        return null;
    }

    /**
     * 从能力系统调用的里程碑状态更新方法
     *
     * @param projectId 项目 ID
     * @param milestoneId 里程碑 ID
     * @param status 状态字符串
     * @return 是否更新成功
     */
    public boolean updateMilestoneStatusFromCapability(String projectId, String milestoneIdOrTitle, String status) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            log.warn("updateMilestoneStatusFromCapability: 项目不存在 {}", projectId);
            return false;
        }

        GoalMilestone milestone = findMilestone(project, milestoneIdOrTitle);

        if (milestone == null) {
            log.warn("updateMilestoneStatusFromCapability: 里程碑不存在 '{}', 项目: {}, 可用里程碑: {}",
                milestoneIdOrTitle, projectId,
                project.getMilestones().stream().map(GameProject.GoalMilestone::getTitle).toList());
            return false;
        }

        // 防抖：同一里程碑在冷却时间内不能重复更新状态
        String cooldownKey = projectId + ":" + milestone.getId();
        Long lastUpdate = milestoneUpdateCooldowns.get(cooldownKey);
        long now = System.currentTimeMillis();
        if (lastUpdate != null && (now - lastUpdate) < MILESTONE_UPDATE_COOLDOWN_SECONDS * 1000L) {
            long remaining = (MILESTONE_UPDATE_COOLDOWN_SECONDS * 1000L - (now - lastUpdate)) / 1000;
            log.warn("里程碑 [{}] 状态更新被拒绝：冷却中，还需等待 {} 秒", milestone.getTitle(), remaining);
            return false;
        }

        try {
            MilestoneStatus newStatus = MilestoneStatus.valueOf(status.toUpperCase());
            MilestoneStatus oldStatus = milestone.getStatus();

            // 防护：已完成的里程碑不能重复标记
            if (oldStatus == MilestoneStatus.COMPLETED && newStatus == MilestoneStatus.COMPLETED) {
                log.debug("里程碑 {} 已经是 COMPLETED 状态，跳过重复更新", milestone.getTitle());
                return true;
            }

            // 防护：标记为 COMPLETED 必须有验证通过的证据
            if (newStatus == MilestoneStatus.COMPLETED) {
                String verifyResult = milestone.getVerificationResult();
                boolean hasValidVerification = verifyResult != null
                    && !verifyResult.isEmpty()
                    && !verifyResult.contains("能力系统更新状态为已完成")
                    && !verifyResult.contains("绕过验证");

                if (!hasValidVerification) {
                    // 【修复】检查是否所有任务都已完成
                    long completedTasks = milestone.getTasks().stream()
                        .filter(t -> t.getStatus() == MilestoneStatus.COMPLETED)
                        .count();
                    boolean allTasksCompleted = completedTasks == milestone.getTasks().size() && completedTasks > 0;

                    if (!allTasksCompleted && milestone.getProgress() < 80) {
                        log.warn("里程碑 [{}] 无法标记为 COMPLETED: 进度仅 {}%，且无有效验证结果。请先完成验证。",
                            milestone.getTitle(), milestone.getProgress());
                        return false;
                    }
                    log.info("里程碑 [{}] 进度 {}%，无验证结果但进度足够，允许标记完成",
                        milestone.getTitle(), milestone.getProgress());
                }

                // 【修复】标记里程碑完成时，自动将所有未完成的任务标记为完成
                for (GameProject.MilestoneTask task : milestone.getTasks()) {
                    if (task.getStatus() != MilestoneStatus.COMPLETED) {
                        task.setStatus(MilestoneStatus.COMPLETED);
                        task.setCompletedAt(java.time.LocalDateTime.now());
                        if (task.getResult() == null || task.getResult().isEmpty()) {
                            task.setResult("里程碑完成时自动标记");
                        }
                        log.info("任务 [{}] 已自动标记为 COMPLETED（里程碑完成）", task.getTitle());
                    }
                }
            }

            milestone.setStatus(newStatus);

            // 根据状态更新进度
            if (newStatus == MilestoneStatus.COMPLETED) {
                milestone.setProgress(100);
                // 设置验证结果（仅在没有有效验证结果时）
                if (milestone.getVerificationResult() == null || milestone.getVerificationResult().isEmpty()) {
                    milestone.setVerificationResult("能力系统更新状态为已完成");
                    milestone.setLastVerificationTime(java.time.LocalDateTime.now().toString());
                }
            } else if (newStatus == MilestoneStatus.IN_PROGRESS && milestone.getProgress() == 0) {
                milestone.setProgress(10);
            }

            // 重新计算项目目标总进度
            project.recalculateGoalProgress();

            project.touch();
            projectManager.saveProjectConfig(project);

            // 记录冷却时间
            milestoneUpdateCooldowns.put(cooldownKey, now);

            log.info("里程碑状态已更新: {} (ID: {}) {} -> {}", milestone.getTitle(), milestone.getId(), oldStatus, newStatus);
            return true;
        } catch (IllegalArgumentException e) {
            log.warn("无效的里程碑状态: {}", status);
            return false;
        }
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
            .filter(t -> t.getId() != null && t.getId().equals(taskId))
            .findFirst()
            .orElse(null);

        if (task == null) return;

        // 记录时间戳
        if (status == MilestoneStatus.IN_PROGRESS && task.getStartedAt() == null) {
            task.setStartedAt(LocalDateTime.now());
        } else if (status == MilestoneStatus.COMPLETED) {
            task.setCompletedAt(LocalDateTime.now());
        }

        task.setStatus(status);
        if (result != null) task.setResult(result);

        // 使用加权进度计算
        recalculateMilestoneProgress(milestone);

        project.recalculateGoalProgress();
        project.touch();
        projectManager.saveProjectConfig(project);
    }

    /**
     * 重新计算里程碑进度（公共方法）
     * 供 AgentScheduler 等外部服务调用
     *
     * @param projectId 项目 ID
     * @param milestoneId 里程碑 ID
     */
    public void recalculateMilestoneProgress(String projectId, String milestoneId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) return;

        GoalMilestone milestone = project.getMilestones().stream()
            .filter(m -> m.getId().equals(milestoneId))
            .findFirst()
            .orElse(null);

        if (milestone == null) return;

        recalculateMilestoneProgress(milestone);
        project.recalculateGoalProgress();
        project.touch();
        projectManager.saveProjectConfig(project);
    }

    /**
     * 重新计算里程碑进度（智能加权平均）
     *
     * 优化点：
     * 1. 考虑任务持续时间 - 长时间进行中的任务给予更低的进度
     * 2. 考虑任务优先级 - 高优先级任务权重更高
     * 3. 进度平滑 - 避免进度剧烈波动
     */
    private void recalculateMilestoneProgress(GameProject.GoalMilestone milestone) {
        if (milestone.getTasks().isEmpty()) {
            milestone.setProgress(0);
            return;
        }

        int totalWeight = 0;
        int weightedProgress = 0;
        int completedCount = 0;
        int inProgressCount = 0;

        for (GameProject.MilestoneTask task : milestone.getTasks()) {
            int weight = task.getWeight();
            totalWeight += weight;

            if (task.getStatus() == GameProject.MilestoneStatus.COMPLETED) {
                weightedProgress += weight * 100;
                completedCount++;
            } else if (task.getStatus() == GameProject.MilestoneStatus.IN_PROGRESS) {
                inProgressCount++;

                // 根据任务持续时间调整进度
                int taskProgress = 50; // 默认进行中=50%
                if (task.getStartedAt() != null) {
                    long minutesElapsed = java.time.Duration.between(task.getStartedAt(), java.time.LocalDateTime.now()).toMinutes();

                    // 超过30分钟的进行中任务，进度递减
                    if (minutesElapsed > 30) {
                        taskProgress = Math.max(20, 50 - (int)((minutesElapsed - 30) / 10));
                    }
                }

                weightedProgress += weight * taskProgress;
            }
            // PENDING 任务贡献 0 进度
        }

        int progress = totalWeight > 0 ? weightedProgress / totalWeight : 0;

        // 进度平滑：避免进度下降（除非任务被重置）
        if (progress < milestone.getProgress() && milestone.getProgress() > 0) {
            // 检查是否有任务被重置
            boolean hasResetTask = milestone.getTasks().stream()
                .anyMatch(t -> t.getStatus() == GameProject.MilestoneStatus.PENDING && t.getStartedAt() != null);

            if (!hasResetTask) {
                // 没有任务被重置，保持当前进度
                progress = milestone.getProgress();
            }
        }

        milestone.setProgress(Math.min(100, progress));

        // 脏数据修复：如果里程碑是 COMPLETED 但有未完成任务，回退为 IN_PROGRESS
        if (milestone.getStatus() == GameProject.MilestoneStatus.COMPLETED) {
            if (completedCount < milestone.getTasks().size()) {
                milestone.setStatus(GameProject.MilestoneStatus.IN_PROGRESS);
                log.warn("里程碑 [{}] 状态不一致：标记COMPLETED但有{}个未完成任务，已回退为IN_PROGRESS",
                    milestone.getTitle(), milestone.getTasks().size() - completedCount);
            }
        }

        // 自动标记里程碑完成：所有任务完成 OR 进度达到 100%
        if (milestone.getStatus() != GameProject.MilestoneStatus.COMPLETED) {
            if (completedCount == milestone.getTasks().size() && completedCount > 0) {
                milestone.setStatus(GameProject.MilestoneStatus.COMPLETED);
                log.info("里程碑 [{}] 所有任务完成，自动标记为 COMPLETED", milestone.getTitle());
            } else if (progress >= 100) {
                milestone.setStatus(GameProject.MilestoneStatus.COMPLETED);
                log.info("里程碑 [{}] 进度达到 100%，自动标记为 COMPLETED", milestone.getTitle());
            }
        }
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
     * 确认目标完成，执行结束逻辑
     * 包含：停止所有 Agent、归档项目、记录完成时间
     *
     * @param projectId 项目 ID
     * @return 操作是否成功
     */
    public boolean completeGoal(String projectId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null || !project.hasGoal()) return false;

        if (project.getGoalStatus() != GoalStatus.REVIEW) {
            log.warn("Cannot complete goal: current status is {}", project.getGoalStatus());
            return false;
        }

        // 1. 更新目标状态为已完成
        project.setGoalStatus(GoalStatus.COMPLETED);
        project.setGoalProgress(100);
        project.setStatus(GameProject.ProjectStatus.COMPLETED);
        project.touch();
        projectManager.saveProjectConfig(project);

        // 2. 停止项目的所有 Agent，不再消耗 Token
        try {
            agentManager.removeProjectAgents(projectId);
            log.info("All agents stopped for completed project: {}", projectId);
        } catch (Exception e) {
            log.warn("Failed to stop agents for project {}: {}", projectId, e.getMessage());
        }

        log.info("Goal completed for project {} ({}): agents stopped, project archived",
            project.getName(), projectId);
        return true;
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
     * 人工验证里程碑
     * 允许项目管理手动验证里程碑的完成情况
     *
     * @param projectId 项目 ID
     * @param milestoneId 里程碑 ID
     * @param passed 验证是否通过
     * @param comment 验证备注
     * @param verifiedBy 验证人
     * @return 操作是否成功
     */
    public boolean verifyMilestoneManually(String projectId, String milestoneId, boolean passed,
                                            String comment, String verifiedBy) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) return false;

        GoalMilestone milestone = project.getMilestones().stream()
            .filter(m -> m.getId().equals(milestoneId))
            .findFirst()
            .orElse(null);

        if (milestone == null) {
            log.warn("verifyMilestoneManually: 里程碑不存在 {}", milestoneId);
            return false;
        }

        // 允许验证的状态：IN_PROGRESS, PENDING, BLOCKED
        // COMPLETED 不需要再验证
        if (milestone.getStatus() == MilestoneStatus.COMPLETED) {
            log.warn("里程碑 [{}] 已完成，无需重复验证", milestone.getTitle());
            return false;
        }

        // 构建验证结果
        String verificationResult = String.format("人工验证 %s: %s (验证人: %s)",
            passed ? "通过" : "未通过",
            comment != null ? comment : "无备注",
            verifiedBy);

        milestone.setVerificationResult(verificationResult);
        milestone.setLastVerificationTime(LocalDateTime.now().toString());

        if (passed) {
            // 验证通过，标记为完成
            milestone.setStatus(MilestoneStatus.COMPLETED);
            milestone.setProgress(100);

            // 同时完成所有未完成的子任务
            if (milestone.getTasks() != null) {
                for (MilestoneTask task : milestone.getTasks()) {
                    if (task.getStatus() != MilestoneStatus.COMPLETED) {
                        task.setStatus(MilestoneStatus.COMPLETED);
                        task.setResult("人工验证通过，自动完成");
                        if (task.getCompletedAt() == null) {
                            task.setCompletedAt(LocalDateTime.now());
                        }
                    }
                }
            }

            log.info("里程碑 [{}] 人工验证通过，已标记为完成", milestone.getTitle());
        } else {
            // 验证未通过，记录失败次数
            milestone.setVerificationFailCount(milestone.getVerificationFailCount() + 1);
            log.info("里程碑 [{}] 人工验证未通过，失败次数: {}", milestone.getTitle(), milestone.getVerificationFailCount());
        }

        // 重新计算项目目标总进度
        project.recalculateGoalProgress();

        // 检查目标是否完成
        if (project.isGoalCompleted()) {
            project.setGoalStatus(GoalStatus.REVIEW);
            log.info("项目 [{}] 所有里程碑已完成，进入审查状态", project.getName());
        }

        project.touch();
        projectManager.saveProjectConfig(project);

        return true;
    }

    /**
     * 跳过里程碑（干预专用）
     * 允许管理员通过干预命令强制完成卡住的里程碑，无需验证结果
     *
     * @param projectId 项目 ID
     * @param milestoneId 里程碑 ID
     * @param reason 跳过原因
     * @return 操作是否成功
     */
    public boolean skipMilestone(String projectId, String milestoneId, String reason) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) return false;

        GoalMilestone milestone = project.getMilestones().stream()
            .filter(m -> m.getId().equals(milestoneId))
            .findFirst()
            .orElse(null);

        if (milestone == null) return false;

        // 已完成的里程碑无需跳过
        if (milestone.getStatus() == MilestoneStatus.COMPLETED) {
            log.warn("里程碑 [{}] 已完成，无需跳过", milestone.getTitle());
            return false;
        }

        // 设置验证结果（绕过验证检查）
        milestone.setVerificationResult("干预跳过: " + (reason != null ? reason : "管理员手动跳过"));
        milestone.setLastVerificationTime(LocalDateTime.now().toString());

        // 强制标记为完成
        milestone.setStatus(MilestoneStatus.COMPLETED);
        milestone.setProgress(100);

        // 同时完成所有未完成的子任务
        for (GameProject.MilestoneTask task : milestone.getTasks()) {
            if (task.getStatus() != MilestoneStatus.COMPLETED) {
                task.setStatus(MilestoneStatus.COMPLETED);
                task.setResult("干预跳过");
                if (task.getCompletedAt() == null) {
                    task.setCompletedAt(LocalDateTime.now());
                }
            }
        }

        // 重新计算项目目标总进度
        project.recalculateGoalProgress();

        // 检查目标是否完成
        if (project.isGoalCompleted()) {
            project.setGoalStatus(GoalStatus.REVIEW);
            log.info("项目 [{}] 所有里程碑完成，进入评审阶段", projectId);
        } else if (project.getGoalStatus() == GoalStatus.NOT_STARTED) {
            project.setGoalStatus(GoalStatus.IN_PROGRESS);
        }

        project.touch();
        projectManager.saveProjectConfig(project);

        log.info("里程碑 [{}] 已被干预跳过，原因: {}", milestone.getTitle(), reason);
        return true;
    }

    /**
     * 强制推进里程碑进度（干预专用）
     * 允许管理员通过干预命令强制推进卡住的里程碑进度
     *
     * @param projectId 项目 ID
     * @param milestoneId 里程碑 ID
     * @param targetProgress 目标进度 (0-100)
     * @param reason 推进原因
     * @return 操作是否成功
     */
    public boolean forceMilestoneProgress(String projectId, String milestoneId, int targetProgress, String reason) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) return false;

        GoalMilestone milestone = project.getMilestones().stream()
            .filter(m -> m.getId().equals(milestoneId))
            .findFirst()
            .orElse(null);

        if (milestone == null) return false;

        // 已完成的里程碑无需推进
        if (milestone.getStatus() == MilestoneStatus.COMPLETED) {
            log.warn("里程碑 [{}] 已完成，无需推进", milestone.getTitle());
            return false;
        }

        int newProgress = Math.max(0, Math.min(100, targetProgress));
        milestone.setProgress(newProgress);

        // 更新状态
        if (newProgress >= 100) {
            milestone.setVerificationResult("干预强制完成: " + (reason != null ? reason : ""));
            milestone.setLastVerificationTime(LocalDateTime.now().toString());
            milestone.setStatus(MilestoneStatus.COMPLETED);
        } else if (newProgress > 0) {
            milestone.setStatus(MilestoneStatus.IN_PROGRESS);
        }

        // 重新计算项目目标总进度
        project.recalculateGoalProgress();
        project.touch();
        projectManager.saveProjectConfig(project);

        log.info("里程碑 [{}] 进度被干预推进至 {}%，原因: {}", milestone.getTitle(), newProgress, reason);
        return true;
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
