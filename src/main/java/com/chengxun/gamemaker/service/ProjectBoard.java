package com.chengxun.gamemaker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 项目看板
 * 提供项目全局状态的共享视图，所有 Agent 可以看到：
 * - 各 Agent 的当前状态和任务
 * - 任务卡片（待办、进行中、已完成）
 * - 阻塞项和风险
 * - 共享上下文信息
 *
 * @author chengxun
 * @since 2.0.0
 */
@Component
public class ProjectBoard {

    private static final Logger log = LoggerFactory.getLogger(ProjectBoard.class);

    /** 项目看板数据：projectId -> BoardData */
    private final Map<String, BoardData> boards = new ConcurrentHashMap<>();

    /**
     * 看板数据
     */
    public static class BoardData {
        public String projectId;
        public Map<String, AgentStatus> agentStatuses = new ConcurrentHashMap<>();
        public List<TaskCard> taskCards = Collections.synchronizedList(new ArrayList<>());
        public List<Blocker> blockers = Collections.synchronizedList(new ArrayList<>());
        public Map<String, Object> sharedContext = new ConcurrentHashMap<>();
        public LocalDateTime lastUpdated = LocalDateTime.now();
    }

    /**
     * Agent 状态
     */
    public static class AgentStatus {
        String agentId;
        String agentRole;
        String agentName;
        String status;  // IDLE, WORKING, BLOCKED, STOPPED
        String currentTask;
        LocalDateTime lastActiveAt;
        Map<String, Object> metadata = new HashMap<>();

        public AgentStatus(String agentId, String agentRole, String agentName) {
            this.agentId = agentId;
            this.agentRole = agentRole;
            this.agentName = agentName;
            this.status = "IDLE";
            this.lastActiveAt = LocalDateTime.now();
        }
    }

    /**
     * 任务卡片
     */
    public static class TaskCard {
        String taskId;
        String title;
        String description;
        String assignedAgentId;
        String assignedRole;
        String status;  // TODO, IN_PROGRESS, DONE, BLOCKED
        String priority;  // HIGH, MEDIUM, LOW
        LocalDateTime createdAt;
        LocalDateTime completedAt;
        String result;
        Map<String, Object> metadata = new HashMap<>();

        public TaskCard(String taskId, String title, String assignedRole) {
            this.taskId = taskId;
            this.title = title;
            this.assignedRole = assignedRole;
            this.status = "TODO";
            this.priority = "MEDIUM";
            this.createdAt = LocalDateTime.now();
        }
    }

    /**
     * 阻塞项
     */
    public static class Blocker {
        String blockerId;
        String description;
        String affectedAgentId;
        String affectedTaskId;
        String severity;  // HIGH, MEDIUM, LOW
        boolean resolved;
        LocalDateTime createdAt;
        LocalDateTime resolvedAt;

        public Blocker(String description, String affectedAgentId, String severity) {
            this.blockerId = UUID.randomUUID().toString();
            this.description = description;
            this.affectedAgentId = affectedAgentId;
            this.severity = severity;
            this.resolved = false;
            this.createdAt = LocalDateTime.now();
        }
    }

    /**
     * 获取或创建项目看板
     */
    public BoardData getOrCreateBoard(String projectId) {
        return boards.computeIfAbsent(projectId, k -> {
            BoardData board = new BoardData();
            board.projectId = k;
            return board;
        });
    }

    /**
     * 更新 Agent 状态
     */
    public void updateAgentStatus(String projectId, String agentId, String agentRole,
                                   String agentName, String status, String currentTask) {
        BoardData board = getOrCreateBoard(projectId);
        AgentStatus agentStatus = board.agentStatuses.computeIfAbsent(
            agentId, k -> new AgentStatus(agentId, agentRole, agentName));
        agentStatus.status = status;
        agentStatus.currentTask = currentTask;
        agentStatus.lastActiveAt = LocalDateTime.now();
        board.lastUpdated = LocalDateTime.now();
    }

    /**
     * 添加任务卡片
     */
    public void addTaskCard(String projectId, TaskCard taskCard) {
        BoardData board = getOrCreateBoard(projectId);
        board.taskCards.add(taskCard);
        board.lastUpdated = LocalDateTime.now();
    }

    /**
     * 更新任务卡片状态
     */
    public void updateTaskStatus(String projectId, String taskId, String status, String result) {
        BoardData board = getOrCreateBoard(projectId);
        for (TaskCard card : board.taskCards) {
            if (taskId.equals(card.taskId)) {
                card.status = status;
                if (result != null) {
                    card.result = result;
                }
                if ("DONE".equals(status)) {
                    card.completedAt = LocalDateTime.now();
                }
                board.lastUpdated = LocalDateTime.now();
                break;
            }
        }
    }

    /**
     * 添加阻塞项
     */
    public void addBlocker(String projectId, String description, String affectedAgentId, String severity) {
        BoardData board = getOrCreateBoard(projectId);
        Blocker blocker = new Blocker(description, affectedAgentId, severity);
        board.blockers.add(blocker);
        board.lastUpdated = LocalDateTime.now();
        log.warn("Blocker added to project {}: {}", projectId, description);
    }

    /**
     * 解除阻塞
     */
    public void resolveBlocker(String projectId, String blockerId) {
        BoardData board = getOrCreateBoard(projectId);
        for (Blocker blocker : board.blockers) {
            if (blockerId.equals(blocker.blockerId)) {
                blocker.resolved = true;
                blocker.resolvedAt = LocalDateTime.now();
                board.lastUpdated = LocalDateTime.now();
                log.info("Blocker resolved in project {}: {}", projectId, blocker.description);
                break;
            }
        }
    }

    /**
     * 记录代码变更
     * Agent 完成代码修改后调用，通知其他 Agent
     *
     * @param projectId 项目 ID
     * @param agentId 修改者 Agent ID
     * @param changedFiles 变更的文件列表
     * @param summary 变更摘要
     */
    public void recordCodeChange(String projectId, String agentId, List<String> changedFiles, String summary) {
        BoardData board = getOrCreateBoard(projectId);

        // 记录到共享上下文
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recentChanges = (List<Map<String, Object>>) board.sharedContext.get("recent_code_changes");
        if (recentChanges == null) {
            recentChanges = Collections.synchronizedList(new ArrayList<>());
            board.sharedContext.put("recent_code_changes", recentChanges);
        }

        Map<String, Object> change = new HashMap<>();
        change.put("agentId", agentId);
        change.put("files", changedFiles);
        change.put("summary", summary);
        change.put("timestamp", LocalDateTime.now().toString());

        recentChanges.add(change);
        // 只保留最近 20 条变更记录
        while (recentChanges.size() > 20) {
            recentChanges.remove(recentChanges.size() - 1);
        }

        board.lastUpdated = LocalDateTime.now();
        log.info("Code change recorded for project {}: {} files changed by {}", projectId, changedFiles.size(), agentId);
    }

    /**
     * 获取最近的代码变更记录
     *
     * @param projectId 项目 ID
     * @param limit 最大返回条数
     * @return 最近的代码变更列表
     */
    public List<Map<String, Object>> getRecentCodeChanges(String projectId, int limit) {
        BoardData board = boards.get(projectId);
        if (board == null) return Collections.emptyList();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recentChanges = (List<Map<String, Object>>) board.sharedContext.get("recent_code_changes");
        if (recentChanges == null) return Collections.emptyList();

        return recentChanges.stream().limit(limit).toList();
    }

    /**
     * 设置共享上下文
     */
    public void setSharedContext(String projectId, String key, Object value) {
        BoardData board = getOrCreateBoard(projectId);
        board.sharedContext.put(key, value);
        board.lastUpdated = LocalDateTime.now();
    }

    /**
     * 获取共享上下文
     */
    public Object getSharedContext(String projectId, String key) {
        BoardData board = getOrCreateBoard(projectId);
        return board.sharedContext.get(key);
    }

    /**
     * 获取项目看板（供 API 使用）
     */
    public BoardData getBoard(String projectId) {
        return boards.get(projectId);
    }

    /**
     * 获取所有项目看板
     */
    public Map<String, BoardData> getAllBoards() {
        return new HashMap<>(boards);
    }

    /**
     * 构建看板摘要（供 Agent 上下文使用）
     */
    public String buildBoardSummary(String projectId) {
        BoardData board = boards.get(projectId);
        if (board == null) return "暂无看板数据";

        StringBuilder sb = new StringBuilder();
        sb.append("## 项目看板\n\n");

        // Agent 状态
        sb.append("### 团队状态\n");
        for (AgentStatus status : board.agentStatuses.values()) {
            sb.append(String.format("- %s (%s): %s", status.agentName, status.agentRole, status.status));
            if (status.currentTask != null && !status.currentTask.isEmpty()) {
                sb.append(" - ").append(status.currentTask);
            }
            sb.append("\n");
        }
        sb.append("\n");

        // 任务统计
        long todoCount = board.taskCards.stream().filter(t -> "TODO".equals(t.status)).count();
        long inProgressCount = board.taskCards.stream().filter(t -> "IN_PROGRESS".equals(t.status)).count();
        long doneCount = board.taskCards.stream().filter(t -> "DONE".equals(t.status)).count();
        sb.append(String.format("### 任务统计\n- 待办: %d\n- 进行中: %d\n- 已完成: %d\n\n", todoCount, inProgressCount, doneCount));

        // 阻塞项
        List<Blocker> activeBlockers = board.blockers.stream()
            .filter(b -> !b.resolved)
            .toList();
        if (!activeBlockers.isEmpty()) {
            sb.append("### ⚠️ 阻塞项\n");
            for (Blocker blocker : activeBlockers) {
                sb.append(String.format("- [%s] %s (影响: %s)\n",
                    blocker.severity, blocker.description, blocker.affectedAgentId));
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 为指定 Agent 构建协作上下文
     * 包含团队状态、任务依赖、最近事件、最近完成任务等信息
     *
     * @param projectId 项目 ID
     * @param agentId Agent ID
     * @param eventBus 事件总线（用于获取最近事件）
     * @return 协作上下文文本
     */
    public String buildAgentContext(String projectId, String agentId, EventBus eventBus) {
        BoardData board = boards.get(projectId);
        if (board == null) return "";

        StringBuilder sb = new StringBuilder();

        // 1. 当前版本目标（从共享上下文获取）
        String currentGoal = (String) board.sharedContext.get("current_goal");
        if (currentGoal != null && !currentGoal.isEmpty()) {
            sb.append("## 当前版本目标\n\n");
            sb.append(currentGoal).append("\n\n");

            // 验证标准
            String verificationCriteria = (String) board.sharedContext.get("verification_criteria");
            if (verificationCriteria != null && !verificationCriteria.isEmpty()) {
                sb.append("**验证标准:** ").append(verificationCriteria).append("\n\n");
            }
        }

        // 2. 团队状态
        sb.append("## 团队状态\n");
        for (AgentStatus status : board.agentStatuses.values()) {
            if (agentId.equals(status.agentId)) {
                sb.append(String.format("- **%s (%s)**: %s [我]\n",
                    status.agentName, status.agentRole, status.status));
            } else {
                sb.append(String.format("- %s (%s): %s",
                    status.agentName, status.agentRole, status.status));
                if (status.currentTask != null && !status.currentTask.isEmpty()) {
                    sb.append(" - ").append(status.currentTask);
                }
                sb.append("\n");
            }
        }
        sb.append("\n");

        // 3. 我的任务
        List<TaskCard> myTasks = board.taskCards.stream()
            .filter(t -> agentId.equals(t.assignedAgentId))
            .toList();
        if (!myTasks.isEmpty()) {
            sb.append("### 我的任务\n");
            for (TaskCard task : myTasks) {
                sb.append(String.format("- [%s] %s", task.status, task.title));
                if (task.description != null && !task.description.isEmpty()) {
                    sb.append(": ").append(task.description.length() > 100 ?
                        task.description.substring(0, 100) + "..." : task.description);
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        // 4. 其他成员最近完成的任务（最近5条）
        List<TaskCard> recentDoneByOthers = board.taskCards.stream()
            .filter(t -> "DONE".equals(t.status))
            .filter(t -> !agentId.equals(t.assignedAgentId))
            .sorted((a, b) -> {
                if (a.completedAt == null) return 1;
                if (b.completedAt == null) return -1;
                return b.completedAt.compareTo(a.completedAt);
            })
            .limit(5)
            .toList();
        if (!recentDoneByOthers.isEmpty()) {
            sb.append("### 团队最近完成的任务\n");
            for (TaskCard task : recentDoneByOthers) {
                String assigneeName = task.assignedRole;
                AgentStatus assigneeStatus = board.agentStatuses.get(task.assignedAgentId);
                if (assigneeStatus != null) {
                    assigneeName = assigneeStatus.agentName;
                }
                sb.append(String.format("- %s 完成了: %s\n", assigneeName, task.title));
            }
            sb.append("\n");
        }

        // 5. 任务统计
        long todoCount = board.taskCards.stream().filter(t -> "TODO".equals(t.status)).count();
        long inProgressCount = board.taskCards.stream().filter(t -> "IN_PROGRESS".equals(t.status)).count();
        long doneCount = board.taskCards.stream().filter(t -> "DONE".equals(t.status)).count();
        sb.append(String.format("### 版本任务统计\n- 待办: %d\n- 进行中: %d\n- 已完成: %d\n\n",
            todoCount, inProgressCount, doneCount));

        // 6. 阻塞项和风险
        List<Blocker> activeBlockers = board.blockers.stream()
            .filter(b -> !b.resolved)
            .toList();
        if (!activeBlockers.isEmpty()) {
            sb.append("### ⚠️ 阻塞项和风险\n");
            for (Blocker blocker : activeBlockers) {
                sb.append(String.format("- [%s] %s (影响: %s)\n",
                    blocker.severity, blocker.description, blocker.affectedAgentId));
            }
            sb.append("\n");
        }

        // 7. 最近团队事件
        if (eventBus != null) {
            List<EventBus.ProjectEvent> recentEvents = eventBus.getRecentEvents(projectId, 10);
            if (!recentEvents.isEmpty()) {
                sb.append("### 最近团队动态\n");
                for (EventBus.ProjectEvent event : recentEvents) {
                    String sourceName = event.getSourceAgentId();
                    AgentStatus sourceStatus = board.agentStatuses.get(event.getSourceAgentId());
                    if (sourceStatus != null) {
                        sourceName = sourceStatus.agentName;
                    }
                    sb.append(String.format("- [%s] %s: %s\n",
                        event.getEventType(), sourceName,
                        event.getData().getOrDefault("summary", "")));
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }
}
