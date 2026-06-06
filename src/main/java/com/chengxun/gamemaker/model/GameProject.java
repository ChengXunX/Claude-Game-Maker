package com.chengxun.gamemaker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GameProject {
    private String id;
    private String name;
    private String description;
    private String workDir;              // 项目工作目录
    private String projectConfigDir;     // .game-maker 目录
    private String templateId;           // 使用的模板ID
    private ProjectStatus status;
    private List<String> agentIds = new ArrayList<>();
    private Map<String, String> metadata = new HashMap<>();
    private LocalDateTime createdAt;
    private LocalDateTime lastActiveAt;

    /** 项目版本号 */
    private String version = "1.0.0";

    /** 版本历史列表 */
    private List<VersionHistory> versionHistory = new ArrayList<>();

    // ===== 项目目标 =====

    /** 项目目标描述 */
    private String goal;

    /** 目标类型 */
    private GoalType goalType;

    /** 目标状态 */
    private GoalStatus goalStatus = GoalStatus.NOT_STARTED;

    /** 目标进度（0-100） */
    private int goalProgress = 0;

    /** 目标截止时间（可选） */
    private LocalDateTime goalDeadline;

    /** 里程碑列表 */
    private List<GoalMilestone> milestones = new ArrayList<>();

    /** 目录配置（目录路径 -> 目录配置），用于告诉 Agent 项目目录结构和用途 */
    private Map<String, DirectoryConfig> directoryConfigs = new HashMap<>();

    /**
     * 目标类型枚举
     */
    public enum GoalType {
        /** 游戏开发 */
        GAME_DEVELOPMENT,
        /** Bug 修复 */
        BUG_FIX,
        /** 功能开发 */
        FEATURE,
        /** 重构 */
        REFACTOR,
        /** 自定义 */
        CUSTOM
    }

    /**
     * 目标状态枚举
     */
    public enum GoalStatus {
        /** 未开始 */
        NOT_STARTED,
        /** 分解中（Producer 正在分析目标生成里程碑） */
        DECOMPOSING,
        /** 进行中 */
        IN_PROGRESS,
        /** 审查中（目标完成，等待验收） */
        REVIEW,
        /** 已完成 */
        COMPLETED,
        /** 已暂停 */
        PAUSED
    }

    public enum ProjectStatus {
        CREATED, ACTIVE, PAUSED, COMPLETED, ARCHIVED
    }

    /**
     * 版本历史记录
     */
    public static class VersionHistory {
        private String version;
        private String description;
        private String createdBy;
        private LocalDateTime createdAt;
        private Map<String, String> changes = new HashMap<>();

        public VersionHistory() {}

        public VersionHistory(String version, String description, String createdBy) {
            this.version = version;
            this.description = description;
            this.createdBy = createdBy;
            this.createdAt = LocalDateTime.now();
        }

        // Getters and Setters
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getCreatedBy() { return createdBy; }
        public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public Map<String, String> getChanges() { return changes; }
        public void setChanges(Map<String, String> changes) { this.changes = changes; }
    }

    /**
     * 里程碑
     * 项目目标的分解单元，由 Producer 根据目标生成
     */
    public static class GoalMilestone {
        /** 里程碑 ID */
        private String id;
        /** 里程碑标题 */
        private String title;
        /** 里程碑描述 */
        private String description;
        /** 里程碑状态 */
        private MilestoneStatus status = MilestoneStatus.PENDING;
        /** 分配给哪个角色的 Agent */
        private String assignedAgentRole;
        /** 分配给哪个具体的 Agent 运行时 ID */
        private String assignedAgentId;
        /** 具体任务列表 */
        private List<MilestoneTask> tasks = new ArrayList<>();
        /** 进度（0-100） */
        private int progress = 0;
        /** 执行顺序 */
        private int order;
        /** 依赖的前置里程碑 ID 列表 */
        private List<String> dependencies = new ArrayList<>();

        /** 该里程碑可访问的目录列表（相对于项目根目录） */
        private List<String> accessibleDirs = new ArrayList<>();

        public GoalMilestone() {}

        public GoalMilestone(String id, String title, String assignedAgentRole, int order) {
            this.id = id;
            this.title = title;
            this.assignedAgentRole = assignedAgentRole;
            this.order = order;
        }

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public MilestoneStatus getStatus() { return status; }
        public void setStatus(MilestoneStatus status) { this.status = status; }
        public String getAssignedAgentRole() { return assignedAgentRole; }
        public void setAssignedAgentRole(String role) { this.assignedAgentRole = role; }
        public String getAssignedAgentId() { return assignedAgentId; }
        public void setAssignedAgentId(String agentId) { this.assignedAgentId = agentId; }
        public List<MilestoneTask> getTasks() { return tasks; }
        public void setTasks(List<MilestoneTask> tasks) { this.tasks = tasks; }
        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = progress; }
        public int getOrder() { return order; }
        public void setOrder(int order) { this.order = order; }
        public List<String> getDependencies() { return dependencies; }
        public void setDependencies(List<String> dependencies) { this.dependencies = dependencies; }

        public List<String> getAccessibleDirs() { return accessibleDirs; }
        public void setAccessibleDirs(List<String> accessibleDirs) { this.accessibleDirs = accessibleDirs != null ? accessibleDirs : new ArrayList<>(); }

        public void addTask(MilestoneTask task) { this.tasks.add(task); }

        /**
         * 检查是否所有前置依赖都已完成
         */
        public boolean areDependenciesMet(List<GoalMilestone> allMilestones) {
            if (dependencies.isEmpty()) return true;
            for (String depId : dependencies) {
                boolean met = allMilestones.stream()
                    .anyMatch(m -> m.getId().equals(depId) && m.getStatus() == MilestoneStatus.COMPLETED);
                if (!met) return false;
            }
            return true;
        }
    }

    /**
     * 里程碑状态枚举
     */
    public enum MilestoneStatus {
        /** 待开始 */
        PENDING,
        /** 进行中 */
        IN_PROGRESS,
        /** 已完成 */
        COMPLETED,
        /** 被阻塞（依赖未完成） */
        BLOCKED
    }

    /**
     * 目录配置
     * 定义项目中各目录的用途，供 Agent 参考
     */
    public static class DirectoryConfig {
        /** 目录路径（相对于项目根目录，如 /server、/client） */
        private String path;
        /** 目录用途描述 */
        private String description;
        /** 补充说明（如可访问的角色、注意事项等） */
        private String notes;

        public DirectoryConfig() {}

        public DirectoryConfig(String path, String description) {
            this.path = path;
            this.description = description;
        }

        public DirectoryConfig(String path, String description, String notes) {
            this.path = path;
            this.description = description;
            this.notes = notes;
        }

        // Getters and Setters
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    /**
     * 里程碑任务
     * 里程碑的具体执行单元
     */
    public static class MilestoneTask {
        /** 任务 ID */
        private String id;
        /** 任务描述 */
        private String description;
        /** 任务状态 */
        private MilestoneStatus status = MilestoneStatus.PENDING;
        /** 执行结果 */
        private String result;

        public MilestoneTask() {}

        public MilestoneTask(String id, String description) {
            this.id = id;
            this.description = description;
        }

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public MilestoneStatus getStatus() { return status; }
        public void setStatus(MilestoneStatus status) { this.status = status; }
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
    }

    public GameProject() {
        this.createdAt = LocalDateTime.now();
        this.lastActiveAt = LocalDateTime.now();
        this.status = ProjectStatus.CREATED;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final GameProject project = new GameProject();

        public Builder id(String id) { project.id = id; return this; }
        public Builder name(String name) { project.name = name; return this; }
        public Builder description(String description) { project.description = description; return this; }
        public Builder workDir(String workDir) { project.workDir = workDir; return this; }
        public Builder templateId(String templateId) { project.templateId = templateId; return this; }
        public Builder status(ProjectStatus status) { project.status = status; return this; }
        public Builder goal(String goal) { project.goal = goal; return this; }
        public Builder goalType(GoalType goalType) { project.goalType = goalType; return this; }
        public Builder goalDeadline(LocalDateTime deadline) { project.goalDeadline = deadline; return this; }
        public GameProject build() {
            if (project.workDir != null) {
                project.projectConfigDir = project.workDir + "/.game-maker";
            }
            return project;
        }
    }

    public void addAgent(String agentId) {
        if (!agentIds.contains(agentId)) {
            agentIds.add(agentId);
        }
    }

    public void removeAgent(String agentId) {
        agentIds.remove(agentId);
    }

    public void touch() {
        this.lastActiveAt = LocalDateTime.now();
    }

    public String getSkillsDir() {
        return projectConfigDir + "/skills";
    }

    public String getMemoryDir() {
        return projectConfigDir + "/memory";
    }

    public String getContextsDir() {
        return projectConfigDir + "/contexts";
    }

    public String getProjectConfigFile() {
        return projectConfigDir + "/project.json";
    }

    public String getProjectRulesFile() {
        return projectConfigDir + "/rules.md";
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getWorkDir() { return workDir; }
    public void setWorkDir(String workDir) { this.workDir = workDir; }

    public String getProjectConfigDir() { return projectConfigDir; }
    public void setProjectConfigDir(String projectConfigDir) { this.projectConfigDir = projectConfigDir; }

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }

    public ProjectStatus getStatus() { return status; }
    public void setStatus(ProjectStatus status) { this.status = status; }

    public List<String> getAgentIds() { return agentIds; }
    public void setAgentIds(List<String> agentIds) { this.agentIds = agentIds; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(LocalDateTime lastActiveAt) { this.lastActiveAt = lastActiveAt; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public List<VersionHistory> getVersionHistory() { return versionHistory; }
    public void setVersionHistory(List<VersionHistory> versionHistory) { this.versionHistory = versionHistory; }

    // ===== 目标相关 Getters and Setters =====

    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }

    public GoalType getGoalType() { return goalType; }
    public void setGoalType(GoalType goalType) { this.goalType = goalType; }

    public GoalStatus getGoalStatus() { return goalStatus; }
    public void setGoalStatus(GoalStatus goalStatus) { this.goalStatus = goalStatus; }

    public int getGoalProgress() { return goalProgress; }
    public void setGoalProgress(int goalProgress) { this.goalProgress = goalProgress; }

    public LocalDateTime getGoalDeadline() { return goalDeadline; }
    public void setGoalDeadline(LocalDateTime goalDeadline) { this.goalDeadline = goalDeadline; }

    public List<GoalMilestone> getMilestones() { return milestones; }
    public void setMilestones(List<GoalMilestone> milestones) { this.milestones = milestones; }

    // ===== 目录配置相关 =====

    public Map<String, DirectoryConfig> getDirectoryConfigs() { return directoryConfigs; }
    public void setDirectoryConfigs(Map<String, DirectoryConfig> directoryConfigs) {
        this.directoryConfigs = directoryConfigs != null ? directoryConfigs : new HashMap<>();
    }

    /**
     * 添加目录配置
     *
     * @param config 目录配置
     */
    public void addDirectoryConfig(DirectoryConfig config) {
        if (config != null && config.getPath() != null) {
            this.directoryConfigs.put(config.getPath(), config);
        }
    }

    /**
     * 删除目录配置
     *
     * @param path 目录路径
     */
    public void removeDirectoryConfig(String path) {
        this.directoryConfigs.remove(path);
    }

    /**
     * 获取目录配置的文本描述
     * 用于在 prompt 中告知 Agent 项目目录结构
     *
     * @return 格式化的目录配置文本
     */
    public String getDirectoryConfigText() {
        if (directoryConfigs.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (DirectoryConfig config : directoryConfigs.values()) {
            sb.append("- ").append(config.getPath());
            sb.append(": ").append(config.getDescription());
            if (config.getNotes() != null && !config.getNotes().isEmpty()) {
                sb.append("（").append(config.getNotes()).append("）");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * 是否有目标
     */
    public boolean hasGoal() {
        return goal != null && !goal.isEmpty();
    }

    /**
     * 目标是否正在进行
     */
    public boolean isGoalActive() {
        return goalStatus == GoalStatus.IN_PROGRESS || goalStatus == GoalStatus.DECOMPOSING;
    }

    /**
     * 添加里程碑
     */
    public void addMilestone(GoalMilestone milestone) {
        this.milestones.add(milestone);
    }

    /**
     * 获取下一个待执行的里程碑（按顺序，依赖已满足）
     */
    public GoalMilestone getNextMilestone() {
        return milestones.stream()
            .filter(m -> m.getStatus() == MilestoneStatus.PENDING && m.areDependenciesMet(milestones))
            .min((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
            .orElse(null);
    }

    /**
     * 更新目标总进度（根据里程碑进度自动计算）
     */
    public void recalculateGoalProgress() {
        if (milestones.isEmpty()) {
            this.goalProgress = 0;
            return;
        }
        int total = milestones.stream().mapToInt(GoalMilestone::getProgress).sum();
        this.goalProgress = total / milestones.size();
    }

    /**
     * Q03: 获取目标模型视图（轻量抽取，不改变现有字段）
     */
    public ProjectGoal toGoalModel() {
        ProjectGoal model = new ProjectGoal(goal, goalType);
        model.setGoalStatus(goalStatus);
        model.setProgress(goalProgress);
        model.setDeadline(goalDeadline);
        model.setMilestones(new ArrayList<>(milestones));
        return model;
    }

    /**
     * 检查目标是否全部完成
     */
    public boolean isGoalCompleted() {
        return !milestones.isEmpty() &&
            milestones.stream().allMatch(m -> m.getStatus() == MilestoneStatus.COMPLETED);
    }

    /**
     * 创建新版本
     */
    public void createVersion(String newVersion, String description, String createdBy) {
        VersionHistory history = new VersionHistory(this.version, description, createdBy);
        versionHistory.add(history);
        this.version = newVersion;
        this.touch();
    }

    /**
     * 获取最新版本历史
     */
    public VersionHistory getLatestVersionHistory() {
        if (versionHistory.isEmpty()) {
            return null;
        }
        return versionHistory.get(versionHistory.size() - 1);
    }

    /**
     * 获取版本数量
     */
    public int getVersionCount() {
        return versionHistory.size();
    }
}
