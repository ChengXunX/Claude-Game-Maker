package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.model.GameProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 协作效率度量服务
 * 量化Agent间的协作效率，发现瓶颈和改进点
 *
 * 度量维度：
 * 1. 交接延迟 — 任务分配到开始执行的平均时间
 * 2. 返工率 — 验证失败导致返工的比例
 * 3. 阻塞时长 — 任务被阻塞的累计时间
 * 4. 知识传递率 — Agent间知识共享的频率
 * 5. 协作瓶颈 — 哪个Agent最常成为瓶颈
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class CollaborationMetricsService {

    private static final Logger log = LoggerFactory.getLogger(CollaborationMetricsService.class);

    /** 任务分配记录：taskId -> TaskRecord */
    private final Map<String, TaskRecord> taskRecords = new ConcurrentHashMap<>();

    /** 知识传递记录 */
    private final List<KnowledgeTransfer> knowledgeTransfers = Collections.synchronizedList(new ArrayList<>());

    /** Agent瓶颈计数：agentId -> 被标记为瓶颈的次数 */
    private final Map<String, Integer> bottleneckCounts = new ConcurrentHashMap<>();

    /**
     * 任务协作记录
     */
    public static class TaskRecord {
        private final String taskId;
        private final String milestoneId;
        private final String assignerId;
        private final String assigneeId;
        private final String taskTitle;
        private LocalDateTime assignedAt;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private int reworkCount;
        private long blockedMinutes;
        private String result; // SUCCESS, FAILED, REWORKED

        public TaskRecord(String taskId, String milestoneId, String assignerId, String assigneeId, String taskTitle) {
            this.taskId = taskId;
            this.milestoneId = milestoneId;
            this.assignerId = assignerId;
            this.assigneeId = assigneeId;
            this.taskTitle = taskTitle;
            this.assignedAt = LocalDateTime.now();
            this.reworkCount = 0;
            this.blockedMinutes = 0;
        }

        // Getters and Setters
        public String getTaskId() { return taskId; }
        public String getMilestoneId() { return milestoneId; }
        public String getAssignerId() { return assignerId; }
        public String getAssigneeId() { return assigneeId; }
        public String getTaskTitle() { return taskTitle; }
        public LocalDateTime getAssignedAt() { return assignedAt; }
        public LocalDateTime getStartedAt() { return startedAt; }
        public void setStartedAt(LocalDateTime time) { this.startedAt = time; }
        public LocalDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(LocalDateTime time) { this.completedAt = time; }
        public int getReworkCount() { return reworkCount; }
        public void incrementRework() { this.reworkCount++; }
        public long getBlockedMinutes() { return blockedMinutes; }
        public void addBlockedMinutes(long minutes) { this.blockedMinutes += minutes; }
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }

        /**
         * 获取交接延迟（分钟）：分配到开始执行的时间差
         */
        public long getHandoffDelayMinutes() {
            if (assignedAt == null || startedAt == null) return -1;
            return ChronoUnit.MINUTES.between(assignedAt, startedAt);
        }

        /**
         * 获取执行时长（分钟）
         */
        public long getExecutionMinutes() {
            if (startedAt == null || completedAt == null) return -1;
            return ChronoUnit.MINUTES.between(startedAt, completedAt);
        }
    }

    /**
     * 知识传递记录
     */
    public static class KnowledgeTransfer {
        private final String fromAgentId;
        private final String toAgentId;
        private final String knowledgeType;
        private final LocalDateTime timestamp;

        public KnowledgeTransfer(String fromAgentId, String toAgentId, String knowledgeType) {
            this.fromAgentId = fromAgentId;
            this.toAgentId = toAgentId;
            this.knowledgeType = knowledgeType;
            this.timestamp = LocalDateTime.now();
        }

        public String getFromAgentId() { return fromAgentId; }
        public String getToAgentId() { return toAgentId; }
        public String getKnowledgeType() { return knowledgeType; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    /**
     * 协作效率指标
     */
    public static class CollaborationMetrics {
        private double avgHandoffDelayMinutes;
        private double reworkRate;
        private long totalBlockedMinutes;
        private int knowledgeTransferCount;
        private String bottleneckAgentId;
        private String bottleneckReason;
        private int totalTasks;
        private int completedTasks;
        private int failedTasks;

        // Getters and Setters
        public double getAvgHandoffDelayMinutes() { return avgHandoffDelayMinutes; }
        public void setAvgHandoffDelayMinutes(double val) { this.avgHandoffDelayMinutes = val; }
        public double getReworkRate() { return reworkRate; }
        public void setReworkRate(double val) { this.reworkRate = val; }
        public long getTotalBlockedMinutes() { return totalBlockedMinutes; }
        public void setTotalBlockedMinutes(long val) { this.totalBlockedMinutes = val; }
        public int getKnowledgeTransferCount() { return knowledgeTransferCount; }
        public void setKnowledgeTransferCount(int val) { this.knowledgeTransferCount = val; }
        public String getBottleneckAgentId() { return bottleneckAgentId; }
        public void setBottleneckAgentId(String val) { this.bottleneckAgentId = val; }
        public String getBottleneckReason() { return bottleneckReason; }
        public void setBottleneckReason(String val) { this.bottleneckReason = val; }
        public int getTotalTasks() { return totalTasks; }
        public void setTotalTasks(int val) { this.totalTasks = val; }
        public int getCompletedTasks() { return completedTasks; }
        public void setCompletedTasks(int val) { this.completedTasks = val; }
        public int getFailedTasks() { return failedTasks; }
        public void setFailedTasks(int val) { this.failedTasks = val; }

        /**
         * 格式化为可读文本
         */
        public String toReadableText() {
            StringBuilder sb = new StringBuilder();
            sb.append("## 协作效率指标\n\n");
            sb.append(String.format("- 总任务数: %d | 已完成: %d | 失败: %d\n", totalTasks, completedTasks, failedTasks));
            sb.append(String.format("- 平均交接延迟: %.1f 分钟\n", avgHandoffDelayMinutes));
            sb.append(String.format("- 返工率: %.1f%%\n", reworkRate * 100));
            sb.append(String.format("- 总阻塞时长: %d 分钟\n", totalBlockedMinutes));
            sb.append(String.format("- 知识传递次数: %d\n", knowledgeTransferCount));

            if (bottleneckAgentId != null) {
                sb.append(String.format("- 协作瓶颈: %s（%s）\n", bottleneckAgentId, bottleneckReason));
            }

            // 效率评级
            sb.append("\n### 效率评级\n");
            double efficiencyScore = calculateEfficiencyScore();
            sb.append(String.format("- 综合效率分: **%.0f/100**\n", efficiencyScore));
            if (efficiencyScore >= 80) {
                sb.append("- 评级: 优秀 — 团队协作流畅\n");
            } else if (efficiencyScore >= 60) {
                sb.append("- 评级: 良好 — 有改进空间\n");
            } else if (efficiencyScore >= 40) {
                sb.append("- 评级: 一般 — 需要关注协作问题\n");
            } else {
                sb.append("- 评级: 较差 — 协作效率需要重点改进\n");
            }

            return sb.toString();
        }

        /**
         * 计算综合效率分
         */
        private double calculateEfficiencyScore() {
            double score = 100;

            // 交接延迟扣分（每分钟延迟扣0.5分，最多扣20分）
            score -= Math.min(20, avgHandoffDelayMinutes * 0.5);

            // 返工率扣分（每1%返工率扣1分，最多扣30分）
            score -= Math.min(30, reworkRate * 100);

            // 阻塞时长扣分（每10分钟阻塞扣1分，最多扣20分）
            score -= Math.min(20, totalBlockedMinutes / 10.0);

            // 任务完成率加分
            if (totalTasks > 0) {
                double completionRate = (double) completedTasks / totalTasks;
                score = score * (0.5 + 0.5 * completionRate);
            }

            return Math.max(0, Math.min(100, score));
        }
    }

    /**
     * 记录任务分配
     */
    public void recordTaskAssignment(String taskId, String milestoneId, String assignerId, String assigneeId, String taskTitle) {
        taskRecords.put(taskId, new TaskRecord(taskId, milestoneId, assignerId, assigneeId, taskTitle));
        log.debug("任务分配已记录: {} -> {}", assignerId, assigneeId);
    }

    /**
     * 记录任务开始执行
     */
    public void recordTaskStarted(String taskId) {
        TaskRecord record = taskRecords.get(taskId);
        if (record != null) {
            record.setStartedAt(LocalDateTime.now());
        }
    }

    /**
     * 记录任务完成
     */
    public void recordTaskCompleted(String taskId, boolean success) {
        TaskRecord record = taskRecords.get(taskId);
        if (record != null) {
            record.setCompletedAt(LocalDateTime.now());
            record.setResult(success ? "SUCCESS" : "FAILED");
        }
    }

    /**
     * 记录任务返工
     */
    public void recordTaskRework(String taskId) {
        TaskRecord record = taskRecords.get(taskId);
        if (record != null) {
            record.incrementRework();
            record.setResult("REWORKED");
        }
    }

    /**
     * 记录任务阻塞
     */
    public void recordTaskBlocked(String taskId, long blockedMinutes) {
        TaskRecord record = taskRecords.get(taskId);
        if (record != null) {
            record.addBlockedMinutes(blockedMinutes);
        }
    }

    /**
     * 记录知识传递
     */
    public void recordKnowledgeTransfer(String fromAgentId, String toAgentId, String knowledgeType) {
        knowledgeTransfers.add(new KnowledgeTransfer(fromAgentId, toAgentId, knowledgeType));
        log.debug("知识传递已记录: {} -> {}", fromAgentId, toAgentId);
    }

    /**
     * 标记Agent为瓶颈
     */
    public void markBottleneck(String agentId, String reason) {
        bottleneckCounts.merge(agentId, 1, Integer::sum);
        log.info("标记瓶颈Agent: {} (原因: {})", agentId, reason);
    }

    /**
     * 计算项目的协作效率指标
     *
     * @param projectId 项目ID
     * @return 协作效率指标
     */
    public CollaborationMetrics calculateMetrics(String projectId) {
        CollaborationMetrics metrics = new CollaborationMetrics();

        // 筛选该项目的任务记录
        List<TaskRecord> projectTasks = taskRecords.values().stream()
            .filter(r -> projectId == null || (r.getMilestoneId() != null && r.getMilestoneId().contains(projectId)))
            .toList();

        metrics.setTotalTasks(projectTasks.size());

        // 交接延迟
        OptionalDouble avgHandoff = projectTasks.stream()
            .filter(r -> r.getHandoffDelayMinutes() >= 0)
            .mapToLong(TaskRecord::getHandoffDelayMinutes)
            .average();
        metrics.setAvgHandoffDelayMinutes(avgHandoff.orElse(0));

        // 返工率
        long reworkedTasks = projectTasks.stream()
            .filter(r -> r.getReworkCount() > 0)
            .count();
        metrics.setReworkRate(projectTasks.isEmpty() ? 0 : (double) reworkedTasks / projectTasks.size());

        // 阻塞时长
        long totalBlocked = projectTasks.stream()
            .mapToLong(TaskRecord::getBlockedMinutes)
            .sum();
        metrics.setTotalBlockedMinutes(totalBlocked);

        // 完成/失败统计
        long completed = projectTasks.stream()
            .filter(r -> "SUCCESS".equals(r.getResult()))
            .count();
        long failed = projectTasks.stream()
            .filter(r -> "FAILED".equals(r.getResult()))
            .count();
        metrics.setCompletedTasks((int) completed);
        metrics.setFailedTasks((int) failed);

        // 知识传递
        metrics.setKnowledgeTransferCount(knowledgeTransfers.size());

        // 瓶颈Agent
        bottleneckCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .ifPresent(entry -> {
                metrics.setBottleneckAgentId(entry.getKey());
                metrics.setBottleneckReason("被标记为瓶颈 " + entry.getValue() + " 次");
            });

        return metrics;
    }

    /**
     * 获取协作瓶颈图（文本格式）
     * 识别哪个Agent最常成为瓶颈
     */
    public String getBottleneckReport() {
        if (bottleneckCounts.isEmpty()) {
            return "暂无瓶颈数据";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("### 协作瓶颈分析\n\n");

        bottleneckCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .forEach(entry -> {
                sb.append(String.format("- %s: 被标记 %d 次\n", entry.getKey(), entry.getValue()));
            });

        return sb.toString();
    }

    /**
     * 清理过期数据（保留最近7天）
     */
    public void cleanup() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        taskRecords.entrySet().removeIf(e -> {
            TaskRecord r = e.getValue();
            return r.getAssignedAt() != null && r.getAssignedAt().isBefore(threshold);
        });
        knowledgeTransfers.removeIf(t -> t.getTimestamp().isBefore(threshold));
    }
}
