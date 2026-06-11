package com.chengxun.gamemaker.agent;

import com.chengxun.gamemaker.engine.ClaudeCliEngine;
import com.chengxun.gamemaker.engine.MessageBus;
import com.chengxun.gamemaker.manager.*;
import com.chengxun.gamemaker.model.*;
import com.chengxun.gamemaker.service.*;
import com.chengxun.gamemaker.web.entity.AgentCapability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 验证督查Agent
 * 项目级默认Agent，负责项目的约束检查、验证和督查工作
 *
 * 主要职责：
 * 一、验证职能：
 * - 项目结构验证：检查项目目录结构完整性
 * - 代码质量检查：代码规范、最佳实践检查
 * - 设计审查：游戏设计文档的合理性和完整性
 * - 里程碑验证：验证里程碑的交付物是否满足验收标准
 * - 约束规则检查：检查项目是否遵守预定义的约束规则
 *
 * 二、督查职能：
 * - 任务进度监控：监控各Agent的任务完成情况
 * - 逾期预警：任务超时或进度滞后时发出预警
 * - 工作质量监督：监督Agent的工作质量
 * - 督促通知：督促Agent按时完成任务
 * - 工作日志审查：审查Agent的工作日志
 * - 绩效数据收集：为绩效评估提供数据支持
 *
 * 工作特点：
 * - 独立运行，不依赖ProducerAgent的指令
 * - 事件驱动，订阅关键事件（代码提交、里程碑变更等）
 * - 定期巡检，主动发现问题
 * - 与ProducerAgent协作，上报严重问题
 * - 主动督促，确保项目进度
 *
 * @author chengxun
 * @since 1.0.0
 */
public class VerificationAgent extends BaseAgent {

    private static final Logger log = LoggerFactory.getLogger(VerificationAgent.class);

    /** 游戏运行时验证服务 */
    private GameRuntimeVerifier gameRuntimeVerifier;

    /** 游戏设计审查服务 */
    private GameDesignReviewService gameDesignReviewService;

    /** 事件总线 */
    private EventBus eventBus;

    /** 项目看板 */
    private ProjectBoard projectBoard;

    /** 通知服务 */
    private com.chengxun.gamemaker.web.service.NotificationService notificationService;

    /** Agent管理器，用于获取其他Agent信息 */
    private AgentManager agentManager;

    /** 版本迭代服务 */
    private VersionIterationService versionIterationService;

    /** 上次督查时间 */
    private LocalDateTime lastSupervisionTime;

    /** 任务超时记录：taskId -> 超时开始时间 */
    private final Map<String, LocalDateTime> overdueTasks = new LinkedHashMap<>();

    /** Agent工作状态记录：agentId -> 状态信息 */
    private final Map<String, AgentWorkStatus> agentWorkStatuses = new LinkedHashMap<>();

    /** 督查问题队列 */
    private final Queue<SupervisionIssue> supervisionIssues = new LinkedList<>();

    /** 版本迭代监督记录：iterationId -> IterationSupervision */
    private final Map<String, IterationSupervision> iterationRecords = new LinkedHashMap<>();

    /** 版本迭代质量趋势 */
    private final IterationQualityTrend qualityTrend = new IterationQualityTrend();

    /** 上次迭代监督时间 */
    private LocalDateTime lastIterationSupervisionTime;

    /** 迭代周期阈值（小时），超过此值视为迭代周期过长 */
    private long iterationCycleThresholdHours = 72; // 3天，可通过配置修改

    /** 迭代分数阈值，低于此值视为质量不达标 */
    private int iterationScoreThreshold = 6;

    /** 回滚率阈值（百分比） */
    private int rollbackRateThreshold = 30;

    /** 系统配置服务 */
    private com.chengxun.gamemaker.web.service.SystemConfigService systemConfigService;

    /** 任务重平衡服务 */
    private com.chengxun.gamemaker.service.TaskRebalanceService taskRebalanceService;

    /** 协作效率度量服务 */
    private com.chengxun.gamemaker.service.CollaborationMetricsService collaborationMetricsService;

    /** 设置任务重平衡服务 */
    public void setTaskRebalanceService(com.chengxun.gamemaker.service.TaskRebalanceService service) {
        this.taskRebalanceService = service;
    }

    /** 设置协作效率度量服务 */
    public void setCollaborationMetricsService(com.chengxun.gamemaker.service.CollaborationMetricsService service) {
        this.collaborationMetricsService = service;
    }

    /**
     * Agent工作状态
     */
    public static class AgentWorkStatus {
        private final String agentId;
        private final String agentRole;
        private String status; // IDLE, WORKING, BLOCKED, OVERDUE
        private LocalDateTime lastActiveTime;
        private int pendingTaskCount;
        private int completedTaskCount;
        private int overdueTaskCount;
        private String currentTask;

        public AgentWorkStatus(String agentId, String agentRole) {
            this.agentId = agentId;
            this.agentRole = agentRole;
            this.status = "IDLE";
            this.lastActiveTime = LocalDateTime.now();
        }

        // Getters and Setters
        public String getAgentId() { return agentId; }
        public String getAgentRole() { return agentRole; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getLastActiveTime() { return lastActiveTime; }
        public void setLastActiveTime(LocalDateTime lastActiveTime) { this.lastActiveTime = lastActiveTime; }
        public int getPendingTaskCount() { return pendingTaskCount; }
        public void setPendingTaskCount(int count) { this.pendingTaskCount = count; }
        public int getCompletedTaskCount() { return completedTaskCount; }
        public void setCompletedTaskCount(int count) { this.completedTaskCount = count; }
        public int getOverdueTaskCount() { return overdueTaskCount; }
        public void setOverdueTaskCount(int count) { this.overdueTaskCount = count; }
        public String getCurrentTask() { return currentTask; }
        public void setCurrentTask(String task) { this.currentTask = task; }
    }

    /**
     * 版本迭代监督记录
     */
    public static class IterationSupervision {
        private final String iterationId;
        private final String version;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String result; // COMPLETED, ITERATED, IMPROVED, ROLLBACK
        private int score;
        private int milestoneCompleted;
        private int milestoneTotal;
        private long durationHours;
        private List<String> issues = new ArrayList<>();

        public IterationSupervision(String iterationId, String version) {
            this.iterationId = iterationId;
            this.version = version;
            this.startTime = LocalDateTime.now();
        }

        // Getters and Setters
        public String getIterationId() { return iterationId; }
        public String getVersion() { return version; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime time) { this.startTime = time; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime time) { this.endTime = time; }
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }
        public int getMilestoneCompleted() { return milestoneCompleted; }
        public void setMilestoneCompleted(int count) { this.milestoneCompleted = count; }
        public int getMilestoneTotal() { return milestoneTotal; }
        public void setMilestoneTotal(int count) { this.milestoneTotal = count; }
        public long getDurationHours() { return durationHours; }
        public void setDurationHours(long hours) { this.durationHours = hours; }
        public List<String> getIssues() { return issues; }
        public void addIssue(String issue) { this.issues.add(issue); }
    }

    /**
     * 版本迭代质量趋势
     */
    public static class IterationQualityTrend {
        private final List<Integer> scores = new ArrayList<>();
        private final List<String> versions = new ArrayList<>();
        private final List<String> results = new ArrayList<>();
        private int totalIterations = 0;
        int passedIterations = 0;
        int rollbackCount = 0;

        public void addIteration(String version, int score, String result) {
            versions.add(version);
            scores.add(score);
            results.add(result);
            totalIterations++;
            if ("COMPLETED".equals(result) || "ITERATED".equals(result)) {
                passedIterations++;
            }
            if ("ROLLBACK".equals(result)) {
                rollbackCount++;
            }
        }

        // Getters
        public List<Integer> getScores() { return scores; }
        public List<String> getVersions() { return versions; }
        public List<String> getResults() { return results; }
        public int getTotalIterations() { return totalIterations; }
        public int getPassedIterations() { return passedIterations; }
        public int getRollbackCount() { return rollbackCount; }
        public double getPassRate() { return totalIterations > 0 ? (double) passedIterations / totalIterations * 100 : 0; }
        public double getAverageScore() { return scores.isEmpty() ? 0 : scores.stream().mapToInt(Integer::intValue).average().orElse(0); }

        /**
         * 检查质量趋势是否下降
         */
        public boolean isQualityDeclining() {
            if (scores.size() < 3) return false;
            int size = scores.size();
            // 检查最近3次迭代的分数是否连续下降
            return scores.get(size-1) < scores.get(size-2) && scores.get(size-2) < scores.get(size-3);
        }

        /**
         * 检查回滚率是否过高
         */
        public boolean isRollbackRateHigh(int threshold) {
            return totalIterations > 0 && (double) rollbackCount / totalIterations * 100 > threshold;
        }
    }

    /**
     * 督查问题
     */
    public static class SupervisionIssue {
        private final String type; // TASK_OVERDUE, AGENT_IDLE, QUALITY_ISSUE, PROGRESS_DELAY, ITERATION_ISSUE
        private final String severity; // CRITICAL, HIGH, MEDIUM, LOW
        private final String targetAgentId;
        private final String description;
        private final String suggestion;
        private final LocalDateTime detectedAt;

        public SupervisionIssue(String type, String severity, String targetAgentId, String description, String suggestion) {
            this.type = type;
            this.severity = severity;
            this.targetAgentId = targetAgentId;
            this.description = description;
            this.suggestion = suggestion;
            this.detectedAt = LocalDateTime.now();
        }

        // Getters
        public String getType() { return type; }
        public String getSeverity() { return severity; }
        public String getTargetAgentId() { return targetAgentId; }
        public String getDescription() { return description; }
        public String getSuggestion() { return suggestion; }
        public LocalDateTime getDetectedAt() { return detectedAt; }
    }

    /** 上次完整验证时间 */
    private LocalDateTime lastFullVerificationTime;

    /** 上次增量验证时间 */
    private LocalDateTime lastIncrementalVerificationTime;

    /** 验证结果缓存：验证类型 -> 验证结果 */
    private final Map<String, VerificationResult> verificationResults = new LinkedHashMap<>();

    /** 待上报的问题队列 */
    private final Queue<VerificationIssue> pendingIssues = new LinkedList<>();

    /**
     * 验证结果
     */
    public static class VerificationResult {
        private final String type;
        private final boolean passed;
        private final String message;
        private final List<String> warnings;
        private final LocalDateTime verifiedAt;

        public VerificationResult(String type, boolean passed, String message, List<String> warnings) {
            this.type = type;
            this.passed = passed;
            this.message = message;
            this.warnings = warnings != null ? warnings : new ArrayList<>();
            this.verifiedAt = LocalDateTime.now();
        }

        // Getters
        public String getType() { return type; }
        public boolean isPassed() { return passed; }
        public String getMessage() { return message; }
        public List<String> getWarnings() { return warnings; }
        public LocalDateTime getVerifiedAt() { return verifiedAt; }
    }

    /**
     * 验证问题
     */
    public static class VerificationIssue {
        private final String severity; // CRITICAL, HIGH, MEDIUM, LOW
        private final String category;
        private final String description;
        private final String suggestion;
        private final LocalDateTime detectedAt;

        public VerificationIssue(String severity, String category, String description, String suggestion) {
            this.severity = severity;
            this.category = category;
            this.description = description;
            this.suggestion = suggestion;
            this.detectedAt = LocalDateTime.now();
        }

        // Getters
        public String getSeverity() { return severity; }
        public String getCategory() { return category; }
        public String getDescription() { return description; }
        public String getSuggestion() { return suggestion; }
        public LocalDateTime getDetectedAt() { return detectedAt; }
    }

    public VerificationAgent(AgentDefinition definition,
                            ClaudeCliEngine cliEngine,
                            MessageBus messageBus,
                            ContextManager contextManager,
                            MemoryManager memoryManager,
                            SkillManager skillManager,
                            ProjectManager projectManager) {
        super(definition, cliEngine, messageBus, contextManager, memoryManager, skillManager, projectManager);
    }

    @Override
    protected void doWork() {
        log.info("VerificationAgent starting work for project: {}", currentProject != null ? currentProject.getName() : "unknown");

        if (currentProject == null) {
            log.warn("No current project, skipping verification work");
            return;
        }

        try {
            // ===== 第一部分：验证工作 =====

            // 执行完整验证
            performFullVerification();

            // 处理待上报的问题
            processPendingIssues();

            // 生成验证报告
            generateVerificationReport();

            // ===== 第二部分：督查工作 =====

            // 监控Agent工作状态
            monitorAgentWorkStatus();

            // 检查任务逾期
            checkOverdueTasks();

            // ===== 第三部分：版本迭代监督 =====

            // 监督版本迭代
            superviseIteration();

            // 分析迭代瓶颈
            analyzeIterationBottlenecks();

            // 沉淀迭代经验到知识库
            saveIterationExperience();

            // 处理督查问题
            processSupervisionIssues();

            // 【新增】监控Agent负载均衡
            monitorAgentLoadBalance();

            // 生成督查报告
            generateSupervisionReport();

        } catch (Exception e) {
            log.error("Verification/Supervision work failed: {}", e.getMessage(), e);
        }
    }

    @Override
    protected void handleMessage(AgentMessage message) {
        log.info("VerificationAgent received message: {}", message.getType());

        // 处理验证相关消息
        switch (message.getType()) {
            case TASK -> {
                // 收到任务消息，执行验证
                performFullVerification();
            }
            case COMMAND -> {
                // 收到命令消息
                String content = message.getContent();
                if (content != null && content.contains("verify_milestone")) {
                    // 提取里程碑ID并验证
                    log.info("收到里程碑验证命令");
                }
            }
            default -> log.debug("忽略未处理的消息类型: {}", message.getType());
        }
    }

    // ===== 服务注入 =====

    public void setGameRuntimeVerifier(GameRuntimeVerifier gameRuntimeVerifier) {
        this.gameRuntimeVerifier = gameRuntimeVerifier;
    }

    public void setGameDesignReviewService(GameDesignReviewService gameDesignReviewService) {
        this.gameDesignReviewService = gameDesignReviewService;
    }

    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void setProjectBoard(ProjectBoard projectBoard) {
        this.projectBoard = projectBoard;
    }

    public void setNotificationService(com.chengxun.gamemaker.web.service.NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void setAgentManager(AgentManager agentManager) {
        this.agentManager = agentManager;
    }

    public void setVersionIterationService(VersionIterationService versionIterationService) {
        this.versionIterationService = versionIterationService;
    }

    public void setSystemConfigService(com.chengxun.gamemaker.web.service.SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    // ===== 生命周期方法 =====

    @Override
    public void initialize() {
        super.initialize();
        log.info("VerificationAgent initializing for project: {}", getProjectId());

        // 订阅事件
        subscribeToEvents();

        log.info("VerificationAgent initialized for project: {}", getProjectId());
    }

    @Override
    public void start() {
        super.start();
        log.info("VerificationAgent started for project: {}", getProjectId());
    }

    @Override
    public void stop() {
        super.stop();
        log.info("VerificationAgent stopped for project: {}", getProjectId());
    }

    // ===== 事件订阅 =====

    /**
     * 订阅相关事件
     */
    private void subscribeToEvents() {
        if (eventBus == null) return;

        // 订阅代码提交事件
        eventBus.subscribe(getProjectId(), "CODE_COMMITTED", event -> {
            log.info("收到代码提交事件，触发增量验证");
            performIncrementalVerification();
        });

        // 订阅里程碑状态变更事件
        eventBus.subscribe(getProjectId(), "MILESTONE_STATUS_CHANGED", event -> {
            log.info("收到里程碑状态变更事件，触发里程碑验证");
            String milestoneId = (String) event.getData().get("milestoneId");
            if (milestoneId != null) {
                verifyMilestone(milestoneId);
            }
        });

        // 订阅Agent工作完成事件
        eventBus.subscribe(getProjectId(), "AGENT_WORK_COMPLETED", event -> {
            log.info("收到Agent工作完成事件，触发增量验证");
            performIncrementalVerification();
        });

        log.info("已订阅事件: CODE_COMMITTED, MILESTONE_STATUS_CHANGED, AGENT_WORK_COMPLETED");
    }

    // ===== 核心工作方法 =====

    /**
     * 执行完整验证
     * 包括结构验证、代码质量检查、设计审查等
     */
    public void performFullVerification() {
        log.info("开始完整验证: project={}", getProjectId());

        // 清空上次的验证结果
        verificationResults.clear();

        // 1. 项目结构验证
        verifyProjectStructure();

        // 2. 代码质量检查
        verifyCodeQuality();

        // 3. 设计审查
        reviewGameDesign();

        // 4. 约束规则检查
        checkConstraints();

        // 5. 验证所有里程碑
        verifyAllMilestones();

        lastFullVerificationTime = LocalDateTime.now();

        // 6. 发送验证摘要给ProducerAgent
        sendVerificationSummaryToProducer();

        // 7. 发送验证通知
        sendVerificationNotification();

        log.info("完整验证完成: project={}", getProjectId());
    }

    /**
     * 验证所有里程碑
     */
    private void verifyAllMilestones() {
        if (currentProject == null) return;

        List<GameProject.GoalMilestone> milestones = currentProject.getMilestones();
        if (milestones == null || milestones.isEmpty()) {
            log.info("项目没有里程碑，跳过里程碑验证");
            return;
        }

        for (GameProject.GoalMilestone milestone : milestones) {
            if (milestone.getStatus() == GameProject.MilestoneStatus.IN_PROGRESS ||
                milestone.getStatus() == GameProject.MilestoneStatus.COMPLETED) {
                verifyMilestone(milestone.getId());
            }
        }
    }

    /**
     * 发送验证摘要给ProducerAgent
     * 让制作人了解项目整体质量状况
     */
    private void sendVerificationSummaryToProducer() {
        String summary = getVerificationSummary();
        if (summary == null || summary.isEmpty()) return;

        StringBuilder content = new StringBuilder();
        content.append("【项目验证摘要】\n\n");
        content.append(summary).append("\n\n");

        // 添加问题汇总
        int criticalCount = 0;
        int highCount = 0;
        int mediumCount = 0;
        int lowCount = 0;

        for (VerificationResult result : verificationResults.values()) {
            if (!result.isPassed()) {
                // 根据验证类型判断严重级别
                if ("STRUCTURE".equals(result.getType())) {
                    criticalCount++;
                } else if ("CODE_QUALITY".equals(result.getType())) {
                    mediumCount++;
                } else if ("DESIGN_REVIEW".equals(result.getType())) {
                    highCount++;
                } else {
                    lowCount++;
                }
            }
        }

        if (criticalCount + highCount + mediumCount + lowCount > 0) {
            content.append("问题汇总:\n");
            if (criticalCount > 0) content.append("- CRITICAL: ").append(criticalCount).append("个\n");
            if (highCount > 0) content.append("- HIGH: ").append(highCount).append("个\n");
            if (mediumCount > 0) content.append("- MEDIUM: ").append(mediumCount).append("个\n");
            if (lowCount > 0) content.append("- LOW: ").append(lowCount).append("个\n");
        }

        // 发送给ProducerAgent
        AgentMessage message = AgentMessage.builder()
            .fromAgentId(getId())
            .toAgentId(getProjectId() + ":producer")
            .type(AgentMessage.MessageType.RESPONSE)
            .content(content.toString())
            .build();
        sendMessage(message);

        log.info("已发送验证摘要给ProducerAgent");
    }

    /**
     * 发送验证通知
     * 使用通知模板发送验证结果给项目成员
     */
    private void sendVerificationNotification() {
        if (notificationService == null || currentProject == null) return;

        try {
            // 计算验证结果
            int passed = 0;
            int failed = 0;
            for (VerificationResult result : verificationResults.values()) {
                if (result.isPassed()) {
                    passed++;
                } else {
                    failed++;
                }
            }

            String resultSummary = failed == 0 ? "全部通过" : String.format("%d项通过, %d项未通过", passed, failed);

            // 构建详情
            StringBuilder details = new StringBuilder();
            for (Map.Entry<String, VerificationResult> entry : verificationResults.entrySet()) {
                VerificationResult result = entry.getValue();
                String status = result.isPassed() ? "✓" : "✗";
                details.append(status).append(" ").append(entry.getKey()).append(": ").append(result.getMessage()).append("\n");
            }

            // 发送通知
            Map<String, String> variables = Map.of(
                "projectName", currentProject.getName(),
                "result", resultSummary,
                "details", details.toString(),
                "time", java.time.LocalDateTime.now().toString()
            );

            notificationService.sendNotificationByTemplate(
                null, // 发送给所有管理员
                "VERIFICATION_SYSTEM",
                variables,
                com.chengxun.gamemaker.web.entity.Notification.NotificationType.SYSTEM
            );

            log.info("已发送验证通知: project={}, result={}", currentProject.getName(), resultSummary);
        } catch (Exception e) {
            log.warn("发送验证通知失败: {}", e.getMessage());
        }
    }

    /**
     * 发送验证问题通知
     * 当发现严重问题时发送通知
     */
    private void sendVerificationIssueNotification(VerificationIssue issue) {
        if (notificationService == null || currentProject == null) return;

        try {
            Map<String, String> variables = Map.of(
                "projectName", currentProject.getName(),
                "severity", issue.getSeverity(),
                "category", issue.getCategory(),
                "description", issue.getDescription(),
                "suggestion", issue.getSuggestion(),
                "time", java.time.LocalDateTime.now().toString()
            );

            notificationService.sendNotificationByTemplate(
                null,
                "VERIFICATION_ISSUE_SYSTEM",
                variables,
                com.chengxun.gamemaker.web.entity.Notification.NotificationType.WARNING
            );

            log.info("已发送验证问题通知: severity={}, category={}", issue.getSeverity(), issue.getCategory());
        } catch (Exception e) {
            log.warn("发送验证问题通知失败: {}", e.getMessage());
        }
    }

    /**
     * 执行增量验证
     * 只验证变更部分，速度快
     */
    public void performIncrementalVerification() {
        log.info("开始增量验证: project={}", getProjectId());

        // 1. 项目结构验证（快速）
        verifyProjectStructure();

        // 2. 约束规则检查（快速）
        checkConstraints();

        lastIncrementalVerificationTime = LocalDateTime.now();
        log.info("增量验证完成: project={}", getProjectId());
    }

    // ===== 验证方法 =====

    /**
     * 验证项目结构完整性
     */
    private void verifyProjectStructure() {
        if (gameRuntimeVerifier == null) {
            log.warn("GameRuntimeVerifier未注入，跳过结构验证");
            return;
        }

        String workDir = currentProject.getWorkDir();
        if (workDir == null || workDir.isEmpty()) {
            addIssue("HIGH", "结构验证", "项目未设置工作目录", "请设置项目工作目录");
            verificationResults.put("STRUCTURE", new VerificationResult("STRUCTURE", false, "项目未设置工作目录", null));
            return;
        }

        GameRuntimeVerifier.VerifyResult result = gameRuntimeVerifier.verify(workDir);

        if (!result.isSuccess()) {
            addIssue("CRITICAL", "结构验证", result.getMessage(), "请检查项目目录结构");
        }

        if (result.hasWarnings()) {
            for (String warning : result.getWarnings()) {
                addIssue("LOW", "结构验证", warning, "建议优化项目结构");
            }
        }

        verificationResults.put("STRUCTURE", new VerificationResult(
            "STRUCTURE", result.isSuccess(), result.getMessage(), result.getWarnings()));

        log.info("结构验证完成: success={}, message={}", result.isSuccess(), result.getMessage());
    }

    /**
     * 验证代码质量
     * 检查代码文件的基本质量指标
     */
    private void verifyCodeQuality() {
        String workDir = currentProject.getWorkDir();
        if (workDir == null || workDir.isEmpty()) {
            return;
        }

        java.io.File dir = new java.io.File(workDir);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        List<String> warnings = new ArrayList<>();
        int totalFiles = 0;
        int largeFiles = 0;
        int emptyFiles = 0;

        // 扫描代码文件
        java.io.File[] files = dir.listFiles();
        if (files != null) {
            for (java.io.File file : files) {
                if (file.isFile() && isCodeFile(file.getName())) {
                    totalFiles++;

                    // 检查文件大小
                    long sizeKB = file.length() / 1024;
                    if (sizeKB > 1000) { // 超过1MB
                        largeFiles++;
                        warnings.add(String.format("文件 %s 过大 (%d KB)，建议拆分", file.getName(), sizeKB));
                    }

                    // 检查空文件
                    if (file.length() == 0) {
                        emptyFiles++;
                        warnings.add(String.format("文件 %s 为空", file.getName()));
                    }
                }
            }
        }

        boolean passed = largeFiles == 0 && emptyFiles == 0;
        String message = String.format("代码质量检查完成: %d个文件, %d个大文件, %d个空文件",
            totalFiles, largeFiles, emptyFiles);

        if (!passed) {
            addIssue("MEDIUM", "代码质量", message, "请处理大文件和空文件");
        }

        verificationResults.put("CODE_QUALITY", new VerificationResult("CODE_QUALITY", passed, message, warnings));
        log.info("代码质量检查完成: total={}, large={}, empty={}", totalFiles, largeFiles, emptyFiles);
    }

    /**
     * 审查游戏设计
     * 检查设计文档的合理性和完整性
     */
    private void reviewGameDesign() {
        if (gameDesignReviewService == null) {
            log.warn("GameDesignReviewService未注入，跳过设计审查");
            return;
        }

        String goal = currentProject.getGoal();
        if (goal == null || goal.isEmpty()) {
            verificationResults.put("DESIGN_REVIEW", new VerificationResult(
                "DESIGN_REVIEW", true, "项目未设置目标，跳过设计审查", null));
            return;
        }

        // 收集设计文档
        String designDocuments = collectDesignDocuments();

        GameDesignReviewService.DesignReviewResult result =
            gameDesignReviewService.reviewDesign(goal, designDocuments, null);

        if (!result.passed) {
            for (var issue : result.issues) {
                addIssue(issue.severity, "设计审查", issue.description, issue.suggestion);
            }
        }

        verificationResults.put("DESIGN_REVIEW", new VerificationResult(
            "DESIGN_REVIEW", result.passed,
            String.format("设计审查完成: 得分=%d, 问题=%d个", result.score, result.issues.size()),
            null));

        log.info("设计审查完成: passed={}, score={}, issues={}",
            result.passed, result.score, result.issues.size());
    }

    /**
     * 验证里程碑
     *
     * @param milestoneId 里程碑ID
     */
    public void verifyMilestone(String milestoneId) {
        log.info("验证里程碑: milestoneId={}", milestoneId);

        // 获取里程碑信息
        List<GameProject.GoalMilestone> milestones = currentProject.getMilestones();
        GameProject.GoalMilestone targetMilestone = null;

        for (GameProject.GoalMilestone m : milestones) {
            if (m.getId().equals(milestoneId)) {
                targetMilestone = m;
                break;
            }
        }

        if (targetMilestone == null) {
            log.warn("未找到里程碑: {}", milestoneId);
            return;
        }

        // 检查里程碑的验收标准
        List<String> criteria = targetMilestone.getVerificationCriteria();
        if (criteria == null || criteria.isEmpty()) {
            log.info("里程碑未设置验收标准，跳过验证: {}", milestoneId);
            return;
        }

        // 执行验证
        boolean passed = checkMilestoneCriteria(targetMilestone);

        verificationResults.put("MILESTONE_" + milestoneId, new VerificationResult(
            "MILESTONE", passed,
            String.format("里程碑 '%s' 验证%s", targetMilestone.getTitle(), passed ? "通过" : "未通过"),
            null));

        log.info("里程碑验证完成: milestoneId={}, passed={}", milestoneId, passed);
    }

    /**
     * 检查约束规则
     * 验证项目是否遵守预定义的约束规则
     */
    private void checkConstraints() {
        String workDir = currentProject.getWorkDir();
        if (workDir == null || workDir.isEmpty()) {
            return;
        }

        List<String> violations = new ArrayList<>();

        // 检查项目目录大小
        java.io.File dir = new java.io.File(workDir);
        if (dir.exists()) {
            long sizeMB = getDirectorySize(dir) / (1024 * 1024);
            if (sizeMB > 1000) { // 超过1GB
                violations.add(String.format("项目目录过大 (%d MB)，建议清理不必要的文件", sizeMB));
            }
        }

        // 检查是否有敏感文件
        checkSensitiveFiles(dir, violations);

        boolean passed = violations.isEmpty();
        String message = passed ? "约束规则检查通过" : "发现 " + violations.size() + " 个约束违规";

        if (!passed) {
            for (String violation : violations) {
                addIssue("MEDIUM", "约束规则", violation, "请处理约束违规");
            }
        }

        verificationResults.put("CONSTRAINTS", new VerificationResult("CONSTRAINTS", passed, message, violations));
        log.info("约束规则检查完成: passed={}, violations={}", passed, violations.size());
    }

    // ===== 督查方法 =====

    /**
     * 监控Agent工作状态
     * 收集各Agent的任务完成情况、活跃度等信息
     */
    private void monitorAgentWorkStatus() {
        if (agentManager == null || currentProject == null) return;

        String projectId = getProjectId();
        log.info("开始监控Agent工作状态: project={}", projectId);

        // 获取项目的所有Agent
        List<Agent> agents = agentManager.getAgentsByProject(projectId);
        if (agents == null || agents.isEmpty()) {
            log.info("项目没有Agent，跳过监控");
            return;
        }

        for (Agent agent : agents) {
            if (!(agent instanceof BaseAgent baseAgent)) continue;

            String agentId = agent.getId();
            String agentRole = baseAgent.getDefinition().getRole();

            // 跳过自己
            if ("verifier".equals(agentRole)) continue;

            AgentWorkStatus status = agentWorkStatuses.computeIfAbsent(
                agentId, k -> new AgentWorkStatus(agentId, agentRole));

            // 更新状态
            status.setStatus(agent.isAlive() ? "WORKING" : "IDLE");
            status.setLastActiveTime(LocalDateTime.now());

            // 获取任务信息
            List<TaskAssignment> tasks = baseAgent.getTasks();
            if (tasks != null) {
                int pending = 0;
                int completed = 0;
                for (TaskAssignment task : tasks) {
                    switch (task.getStatus()) {
                        case PENDING, IN_PROGRESS -> pending++;
                        case COMPLETED -> completed++;
                    }
                }
                status.setPendingTaskCount(pending);
                status.setCompletedTaskCount(completed);
            }

            log.debug("Agent状态: {} - {}", agentId, status.getStatus());
        }

        lastSupervisionTime = LocalDateTime.now();
        log.info("Agent工作状态监控完成: {} 个Agent", agents.size());
    }

    /**
     * 监控Agent负载均衡
     * 检测超载/空闲Agent，记录负载不均衡问题
     */
    private void monitorAgentLoadBalance() {
        if (taskRebalanceService == null || agentManager == null || currentProject == null) return;

        String projectId = getProjectId();
        List<Agent> agents = agentManager.getAgentsByProject(projectId);
        if (agents.isEmpty()) return;

        // 更新负载信息
        for (Agent agent : agents) {
            if ("producer".equals(agent.getRole()) || "verifier".equals(agent.getRole())) continue;
            int taskCount = agent.getTasks().size();
            taskRebalanceService.updateAgentLoad(agent, taskCount);
        }

        // 检查重平衡
        List<TaskRebalanceService.RebalanceAction> actions = taskRebalanceService.checkRebalance(agents);
        for (TaskRebalanceService.RebalanceAction action : actions) {
            String severity = action.getType() == TaskRebalanceService.RebalanceAction.Type.RESOLVE_CONFLICT ? "HIGH" : "MEDIUM";
            supervisionIssues.offer(new SupervisionIssue(
                "LOAD_IMBALANCE",
                severity,
                action.getFromAgentId() != null ? action.getFromAgentId() : action.getToAgentId(),
                action.toReadableText(),
                "建议制作人重新分配任务或调整团队配置"
            ));
        }

        log.debug("负载均衡监控完成: {} 个重平衡建议", actions.size());
    }

    /**
     * 检查任务逾期
     * 根据任务开始时间和预估工时检查是否超时
     */
    private void checkOverdueTasks() {
        if (currentProject == null) return;

        log.info("检查任务逾期: project={}", getProjectId());

        List<GameProject.GoalMilestone> milestones = currentProject.getMilestones();
        if (milestones == null || milestones.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now();

        for (GameProject.GoalMilestone milestone : milestones) {
            // 只检查进行中的里程碑
            if (milestone.getStatus() != GameProject.MilestoneStatus.IN_PROGRESS) continue;

            // 检查里程碑下的任务
            List<GameProject.MilestoneTask> tasks = milestone.getTasks();
            if (tasks == null) continue;

            for (GameProject.MilestoneTask task : tasks) {
                // 只检查进行中的任务
                if (task.getStatus() != GameProject.MilestoneStatus.IN_PROGRESS) continue;

                // 检查任务是否有开始时间和预估工时
                LocalDateTime startedAt = task.getStartedAt();
                int estimatedHours = task.getEstimatedHours();

                if (startedAt == null || estimatedHours <= 0) continue;

                // 计算预期完成时间
                LocalDateTime expectedCompletion = startedAt.plusHours(estimatedHours);

                // 计算超时时间
                long hoursOverdue = java.time.Duration.between(expectedCompletion, now).toHours();

                if (hoursOverdue > 0) {
                    // 已超时
                    String targetAgent = getProjectId() + ":" + task.getAssignedRole();
                    addSupervisionIssue("TASK_OVERDUE", hoursOverdue > 24 ? "CRITICAL" : "HIGH", targetAgent,
                        String.format("任务 '%s' 已超时 %d 小时 (里程碑: %s, 预估: %dh)", task.getTitle(), hoursOverdue, milestone.getTitle(), estimatedHours),
                        "请立即检查并推进进度");
                }
            }
        }

        log.info("任务逾期检查完成");
    }

    // ===== 版本迭代监督 =====

    /**
     * 监督版本迭代
     * 检查迭代质量、周期、目标达成率等
     */
    private void superviseIteration() {
        if (versionIterationService == null || currentProject == null) return;

        String projectId = getProjectId();
        log.info("开始版本迭代监督: project={}", projectId);

        try {
            // 加载可配置阈值
            loadSupervisionConfig();

            // 获取迭代统计
            Map<String, Object> stats = versionIterationService.getIterationStats(projectId);
            if (stats == null || stats.isEmpty()) {
                log.info("暂无迭代数据，跳过迭代监督");
                return;
            }

            // 更新质量趋势
            updateQualityTrend(projectId);

            // 检查迭代周期
            checkIterationCycle(projectId);

            // 检查迭代质量
            checkIterationQuality();

            // 检查回滚率
            checkRollbackRate();

            // 检查目标达成率
            checkGoalAchievement();

            lastIterationSupervisionTime = LocalDateTime.now();
            log.info("版本迭代监督完成");

        } catch (Exception e) {
            log.error("版本迭代监督失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 加载可配置的督查规则
     */
    private void loadSupervisionConfig() {
        if (systemConfigService == null) return;
        try {
            iterationCycleThresholdHours = systemConfigService.getInt(
                com.chengxun.gamemaker.config.SystemConstants.SUPERVISION_ITERATION_TIMEOUT_HOURS, 72);
            iterationScoreThreshold = systemConfigService.getInt(
                com.chengxun.gamemaker.config.SystemConstants.SUPERVISION_QUALITY_SCORE_THRESHOLD, 6);
            rollbackRateThreshold = systemConfigService.getInt(
                com.chengxun.gamemaker.config.SystemConstants.SUPERVISION_ROLLBACK_RATE_THRESHOLD, 30);
        } catch (Exception e) {
            log.debug("加载督查配置失败，使用默认值: {}", e.getMessage());
        }
    }

    /**
     * 更新质量趋势
     */
    private void updateQualityTrend(String projectId) {
        List<?> records = versionIterationService.getIterationRecords(projectId);
        if (records == null || records.isEmpty()) return;

        // 清空旧数据
        qualityTrend.getScores().clear();
        qualityTrend.getVersions().clear();
        qualityTrend.getResults().clear();

        // 解析迭代记录
        for (Object record : records) {
            if (record instanceof Map<?, ?> map) {
                String version = (String) map.get("version");
                Object scoreObj = map.get("evaluationScore");
                String result = (String) map.get("result");

                if (version != null && scoreObj != null && result != null) {
                    int score = scoreObj instanceof Number ? ((Number) scoreObj).intValue() : 0;
                    qualityTrend.addIteration(version, score, result);
                }
            }
        }

        log.info("质量趋势更新: {} 次迭代, 平均分 {:.1f}, 通过率 {:.1f}%",
            qualityTrend.getTotalIterations(), qualityTrend.getAverageScore(), qualityTrend.getPassRate());
    }

    /**
     * 检查迭代周期
     */
    private void checkIterationCycle(String projectId) {
        List<?> records = versionIterationService.getIterationRecords(projectId);
        if (records == null || records.size() < 2) return;

        // 获取最近两次迭代
        Object lastRecord = records.get(records.size() - 1);
        Object prevRecord = records.get(records.size() - 2);

        if (lastRecord instanceof Map<?, ?> lastMap && prevRecord instanceof Map<?, ?> prevMap) {
            String lastTimeStr = (String) lastMap.get("createdAt");
            String prevTimeStr = (String) prevMap.get("createdAt");

            if (lastTimeStr != null && prevTimeStr != null) {
                try {
                    LocalDateTime lastTime = LocalDateTime.parse(lastTimeStr.substring(0, 19));
                    LocalDateTime prevTime = LocalDateTime.parse(prevTimeStr.substring(0, 19));
                    long cycleHours = java.time.Duration.between(prevTime, lastTime).toHours();

                    if (cycleHours > iterationCycleThresholdHours) {
                        addSupervisionIssue("ITERATION_ISSUE", "MEDIUM", null,
                            String.format("迭代周期过长: %d 小时 (阈值: %d 小时)", cycleHours, iterationCycleThresholdHours),
                            "建议优化迭代流程，缩短迭代周期");
                    }
                } catch (Exception e) {
                    log.warn("解析迭代时间失败: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 检查迭代质量
     */
    private void checkIterationQuality() {
        if (qualityTrend.getScores().isEmpty()) return;

        // 检查最近一次迭代的分数
        int lastScore = qualityTrend.getScores().get(qualityTrend.getScores().size() - 1);
        if (lastScore < iterationScoreThreshold) {
            addSupervisionIssue("ITERATION_ISSUE", "HIGH", null,
                String.format("最近一次迭代评分不达标: %d/%d", lastScore, 10),
                "请分析原因并改进下一个迭代");
        }

        // 检查质量趋势是否下降
        if (qualityTrend.isQualityDeclining()) {
            addSupervisionIssue("ITERATION_ISSUE", "HIGH", null,
                "迭代质量连续下降，最近3次迭代评分持续走低",
                "请分析根本原因，调整开发策略");
        }
    }

    /**
     * 检查回滚率
     */
    private void checkRollbackRate() {
        if (qualityTrend.getTotalIterations() < 3) return;

        if (qualityTrend.isRollbackRateHigh(rollbackRateThreshold)) {
            addSupervisionIssue("ITERATION_ISSUE", "CRITICAL", null,
                String.format("回滚率过高: %d/%d (%.1f%%)",
                    qualityTrend.getRollbackCount(), qualityTrend.getTotalIterations(),
                    (double) qualityTrend.getRollbackCount() / qualityTrend.getTotalIterations() * 100),
                "请加强质量把控，减少回滚");
        }
    }

    /**
     * 检查目标达成率
     */
    private void checkGoalAchievement() {
        if (currentProject == null) return;

        // 检查项目目标进度
        int goalProgress = currentProject.getGoalProgress();
        String goalStatus = currentProject.getGoalStatus() != null ? currentProject.getGoalStatus().name() : "UNKNOWN";

        // 如果项目已进行多次迭代但目标进度仍然很低
        if (qualityTrend.getTotalIterations() > 5 && goalProgress < 30) {
            addSupervisionIssue("ITERATION_ISSUE", "HIGH", null,
                String.format("目标达成率偏低: %d%% (已迭代 %d 次)", goalProgress, qualityTrend.getTotalIterations()),
                "请重新评估目标可行性或调整策略");
        }
    }

    /**
     * 分析迭代瓶颈
     * 识别影响迭代效率的关键因素
     */
    private void analyzeIterationBottlenecks() {
        if (currentProject == null || qualityTrend.getTotalIterations() < 3) return;

        log.info("分析迭代瓶颈: project={}", getProjectId());

        // 分析里程碑完成情况
        List<GameProject.GoalMilestone> milestones = currentProject.getMilestones();
        if (milestones != null) {
            int blockedCount = 0;
            int delayedCount = 0;

            for (GameProject.GoalMilestone milestone : milestones) {
                if (milestone.getStatus() == GameProject.MilestoneStatus.IN_PROGRESS) {
                    // 检查是否有阻塞的任务
                    List<GameProject.MilestoneTask> tasks = milestone.getTasks();
                    if (tasks != null) {
                        for (GameProject.MilestoneTask task : tasks) {
                            if (task.getStatus() == GameProject.MilestoneStatus.IN_PROGRESS) {
                                LocalDateTime startedAt = task.getStartedAt();
                                int estimatedHours = task.getEstimatedHours();
                                if (startedAt != null && estimatedHours > 0) {
                                    long actualHours = java.time.Duration.between(startedAt, LocalDateTime.now()).toHours();
                                    if (actualHours > estimatedHours * 1.5) {
                                        delayedCount++;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (delayedCount > 0) {
                addSupervisionIssue("ITERATION_ISSUE", "MEDIUM", null,
                    String.format("发现 %d 个任务严重滞后", delayedCount),
                    "请分析滞后原因，是估时不准还是遇到技术难题");
            }
        }

        // 分析Agent工作负载均衡性
        if (!agentWorkStatuses.isEmpty()) {
            int overloadedCount = 0;
            int idleCount = 0;

            for (AgentWorkStatus status : agentWorkStatuses.values()) {
                if (status.getPendingTaskCount() > 5) {
                    overloadedCount++;
                } else if (status.getPendingTaskCount() == 0 && "WORKING".equals(status.getStatus())) {
                    idleCount++;
                }
            }

            if (overloadedCount > 0 && idleCount > 0) {
                addSupervisionIssue("ITERATION_ISSUE", "MEDIUM", null,
                    String.format("工作负载不均衡: %d个Agent过载, %d个Agent空闲", overloadedCount, idleCount),
                    "建议优化任务分配，平衡工作负载");
            }
        }
    }

    /**
     * 生成迭代改进建议
     * 基于历史数据生成针对性的改进建议
     */
    private String generateIterationImprovementSuggestions() {
        StringBuilder suggestions = new StringBuilder();

        if (qualityTrend.getTotalIterations() < 3) {
            suggestions.append("数据不足，暂无建议。请积累更多迭代数据后再分析。\n");
            return suggestions.toString();
        }

        suggestions.append("## 迭代改进建议\n\n");

        // 基于平均分的建议
        double avgScore = qualityTrend.getAverageScore();
        if (avgScore < 6) {
            suggestions.append("### 质量提升\n");
            suggestions.append("- 平均评分偏低 (").append(String.format("%.1f", avgScore)).append("/10)，建议：\n");
            suggestions.append("  - 加强需求评审，确保目标清晰\n");
            suggestions.append("  - 增加代码审查频率\n");
            suggestions.append("  - 引入自动化测试\n\n");
        }

        // 基于回滚率的建议
        double rollbackRate = qualityTrend.getTotalIterations() > 0 ?
            (double) qualityTrend.getRollbackCount() / qualityTrend.getTotalIterations() * 100 : 0;
        if (rollbackRate > 20) {
            suggestions.append("### 回滚优化\n");
            suggestions.append("- 回滚率偏高 (").append(String.format("%.1f", rollbackRate)).append("%)，建议：\n");
            suggestions.append("  - 加强迭代前的自测环节\n");
            suggestions.append("  - 建立更完善的验收标准\n");
            suggestions.append("  - 考虑灰度发布策略\n\n");
        }

        // 基于迭代周期的建议
        suggestions.append("### 流程优化\n");
        suggestions.append("- 建议每次迭代后进行回顾会议\n");
        suggestions.append("- 记录迭代中的问题和改进点\n");
        suggestions.append("- 将成功的经验沉淀到知识库\n");

        return suggestions.toString();
    }

    /**
     * 沉淀迭代经验到知识库
     */
    private void saveIterationExperience() {
        if (currentProject == null || qualityTrend.getTotalIterations() < 3) return;

        try {
            String experience = String.format(
                "迭代经验总结 (项目: %s)\n\n" +
                "总迭代次数: %d\n" +
                "平均评分: %.1f/10\n" +
                "通过率: %.1f%%\n" +
                "回滚次数: %d\n" +
                "质量趋势: %s\n\n" +
                "改进建议:\n%s",
                currentProject.getName(),
                qualityTrend.getTotalIterations(),
                qualityTrend.getAverageScore(),
                qualityTrend.getPassRate(),
                qualityTrend.getRollbackCount(),
                qualityTrend.isQualityDeclining() ? "下降" : "稳定",
                generateIterationImprovementSuggestions()
            );

            saveToKnowledgeBase("iteration_experience", experience);
            log.info("迭代经验已保存到知识库");
        } catch (Exception e) {
            log.warn("保存迭代经验失败: {}", e.getMessage());
        }
    }

    /**
     * 获取迭代监督摘要
     */
    public String getIterationSupervisionSummary() {
        if (qualityTrend.getTotalIterations() == 0) {
            return "暂无迭代数据";
        }

        return String.format("迭代监督: %d次迭代, 平均分%.1f, 通过率%.1f%%, 回滚%d次, 趋势%s",
            qualityTrend.getTotalIterations(),
            qualityTrend.getAverageScore(),
            qualityTrend.getPassRate(),
            qualityTrend.getRollbackCount(),
            qualityTrend.isQualityDeclining() ? "下降" : "稳定");
    }

    /**
     * 添加督查问题
     */
    private void addSupervisionIssue(String type, String severity, String targetAgentId, String description, String suggestion) {
        SupervisionIssue issue = new SupervisionIssue(type, severity, targetAgentId, description, suggestion);
        supervisionIssues.add(issue);
        log.info("发现督查问题: [{}] {} - {}", severity, type, description);
    }

    /**
     * 处理督查问题
     * 发送督促通知给相关Agent
     */
    private void processSupervisionIssues() {
        while (!supervisionIssues.isEmpty()) {
            SupervisionIssue issue = supervisionIssues.poll();

            // 发送督促通知
            sendSupervisionNotification(issue);

            // 如果是严重问题，通知ProducerAgent
            if ("CRITICAL".equals(issue.getSeverity()) || "HIGH".equals(issue.getSeverity())) {
                notifyProducerSupervision(issue);
            }
        }
    }

    /**
     * 发送督促通知
     */
    private void sendSupervisionNotification(SupervisionIssue issue) {
        if (issue.getTargetAgentId() == null) return;

        String content = String.format("【督查通知】\n\n类型: %s\n严重级别: %s\n问题: %s\n建议: %s",
            issue.getType(), issue.getSeverity(), issue.getDescription(), issue.getSuggestion());

        AgentMessage message = AgentMessage.builder()
            .fromAgentId(getId())
            .toAgentId(issue.getTargetAgentId())
            .type(AgentMessage.MessageType.SYSTEM)
            .content(content)
            .build();
        sendMessage(message);

        log.info("已发送督促通知: target={}, type={}", issue.getTargetAgentId(), issue.getType());
    }

    /**
     * 通知ProducerAgent督查问题
     */
    private void notifyProducerSupervision(SupervisionIssue issue) {
        String content = String.format("【督查报告】\n\n类型: %s\n严重级别: %s\n问题: %s\n建议: %s",
            issue.getType(), issue.getSeverity(), issue.getDescription(), issue.getSuggestion());

        AgentMessage message = AgentMessage.builder()
            .fromAgentId(getId())
            .toAgentId(getProjectId() + ":producer")
            .type(AgentMessage.MessageType.SYSTEM)
            .content(content)
            .build();
        sendMessage(message);

        log.info("已通知ProducerAgent督查问题: type={}", issue.getType());
    }

    /**
     * 生成督查报告
     */
    private void generateSupervisionReport() {
        StringBuilder report = new StringBuilder();
        report.append("# 项目督查报告\n\n");
        report.append("项目: ").append(currentProject != null ? currentProject.getName() : "未知").append("\n");
        report.append("督查时间: ").append(LocalDateTime.now()).append("\n\n");

        // Agent工作状态
        if (!agentWorkStatuses.isEmpty()) {
            report.append("## Agent工作状态\n\n");
            report.append("| Agent | 角色 | 状态 | 待处理任务 | 已完成任务 |\n");
            report.append("|-------|------|------|-----------|----------|\n");

            for (AgentWorkStatus status : agentWorkStatuses.values()) {
                report.append(String.format("| %s | %s | %s | %d | %d |\n",
                    status.getAgentId(), status.getAgentRole(), status.getStatus(),
                    status.getPendingTaskCount(), status.getCompletedTaskCount()));
            }

            report.append("\n## 督查统计\n\n");
            report.append("- 监控Agent数: ").append(agentWorkStatuses.size()).append("\n");
            report.append("- 活跃Agent: ").append(agentWorkStatuses.values().stream()
                .filter(s -> "WORKING".equals(s.getStatus())).count()).append("\n");
            report.append("- 空闲Agent: ").append(agentWorkStatuses.values().stream()
                .filter(s -> "IDLE".equals(s.getStatus())).count()).append("\n");
        }

        // 版本迭代监督
        if (qualityTrend.getTotalIterations() > 0) {
            report.append("\n## 版本迭代监督\n\n");
            report.append("- 迭代次数: ").append(qualityTrend.getTotalIterations()).append("\n");
            report.append("- 平均评分: ").append(String.format("%.1f", qualityTrend.getAverageScore())).append("/10\n");
            report.append("- 通过率: ").append(String.format("%.1f", qualityTrend.getPassRate())).append("%\n");
            report.append("- 回滚次数: ").append(qualityTrend.getRollbackCount()).append("\n");
            report.append("- 质量趋势: ").append(qualityTrend.isQualityDeclining() ? "⚠️ 下降" : "✅ 稳定").append("\n");
            report.append("- 回滚率: ").append(qualityTrend.isRollbackRateHigh(rollbackRateThreshold) ? "⚠️ 过高" : "✅ 正常").append("\n");

            // 最近几次迭代的分数趋势
            if (qualityTrend.getScores().size() > 1) {
                report.append("\n### 分数趋势\n\n");
                int showCount = Math.min(5, qualityTrend.getScores().size());
                for (int i = qualityTrend.getScores().size() - showCount; i < qualityTrend.getScores().size(); i++) {
                    report.append(String.format("- %s: %d/10 (%s)\n",
                        qualityTrend.getVersions().get(i),
                        qualityTrend.getScores().get(i),
                        qualityTrend.getResults().get(i)));
                }
            }

            // 添加迭代改进建议
            report.append("\n").append(generateIterationImprovementSuggestions());
        }

        // 【新增】协作效率指标
        if (collaborationMetricsService != null) {
            try {
                CollaborationMetricsService.CollaborationMetrics metrics =
                    collaborationMetricsService.calculateMetrics(getProjectId());
                if (metrics.getTotalTasks() > 0) {
                    report.append("\n## 协作效率\n\n");
                    report.append(String.format("- 总任务: %d | 完成: %d | 失败: %d\n",
                        metrics.getTotalTasks(), metrics.getCompletedTasks(), metrics.getFailedTasks()));
                    report.append(String.format("- 平均交接延迟: %.1f 分钟\n", metrics.getAvgHandoffDelayMinutes()));
                    report.append(String.format("- 返工率: %.1f%%\n", metrics.getReworkRate() * 100));
                    report.append(String.format("- 总阻塞时长: %d 分钟\n", metrics.getTotalBlockedMinutes()));
                    if (metrics.getBottleneckAgentId() != null) {
                        report.append(String.format("- 瓶颈Agent: %s（%s）\n",
                            metrics.getBottleneckAgentId(), metrics.getBottleneckReason()));
                    }
                }
            } catch (Exception e) {
                log.debug("协作效率计算失败: {}", e.getMessage());
            }
        }

        // 【新增】Agent负载均衡状态
        if (taskRebalanceService != null) {
            try {
                List<Agent> agents = agentManager.getAgentsByProject(getProjectId());
                if (!agents.isEmpty()) {
                    String loadSummary = taskRebalanceService.getLoadSummary(agents);
                    if (!loadSummary.isEmpty()) {
                        report.append("\n").append(loadSummary);
                    }
                }
            } catch (Exception e) {
                log.debug("负载均衡检查失败: {}", e.getMessage());
            }
        }

        // 保存报告到知识库
        saveToKnowledgeBase("supervision_report", report.toString());
        log.info("督查报告已生成");

        // 发送报告给ProducerAgent
        sendReportToProducer(report.toString());
    }

    /**
     * 发送报告给ProducerAgent
     */
    private void sendReportToProducer(String reportContent) {
        if (currentProject == null) return;

        // 只发送关键信息，不发送完整报告
        String summary = String.format("【督查报告摘要】\n\n项目: %s\n时间: %s\n\n%s",
            currentProject.getName(),
            LocalDateTime.now(),
            getSupervisionSummary());

        if (qualityTrend.getTotalIterations() > 0) {
            summary += "\n" + getIterationSupervisionSummary();
        }

        AgentMessage message = AgentMessage.builder()
            .fromAgentId(getId())
            .toAgentId(getProjectId() + ":producer")
            .type(AgentMessage.MessageType.RESPONSE)
            .content(summary)
            .build();
        sendMessage(message);

        log.info("已发送督查报告摘要给ProducerAgent");
    }

    /**
     * 获取督查摘要
     */
    public String getSupervisionSummary() {
        if (agentWorkStatuses.isEmpty()) {
            return "暂无督查数据";
        }

        int working = 0;
        int idle = 0;
        int totalPending = 0;
        int totalCompleted = 0;

        for (AgentWorkStatus status : agentWorkStatuses.values()) {
            if ("WORKING".equals(status.getStatus())) working++;
            if ("IDLE".equals(status.getStatus())) idle++;
            totalPending += status.getPendingTaskCount();
            totalCompleted += status.getCompletedTaskCount();
        }

        return String.format("督查摘要: %d个Agent, %d活跃/%d空闲, %d待处理/%d已完成任务",
            agentWorkStatuses.size(), working, idle, totalPending, totalCompleted);
    }

    // ===== 辅助方法 =====

    /**
     * 添加问题到待上报队列
     */
    private void addIssue(String severity, String category, String description, String suggestion) {
        VerificationIssue issue = new VerificationIssue(severity, category, description, suggestion);
        pendingIssues.add(issue);
        log.info("发现问题: [{}] {} - {}", severity, category, description);
    }

    /**
     * 处理待上报的问题
     * 将严重问题通知ProducerAgent
     */
    private void processPendingIssues() {
        while (!pendingIssues.isEmpty()) {
            VerificationIssue issue = pendingIssues.poll();

            // 只上报HIGH和CRITICAL级别问题
            if ("CRITICAL".equals(issue.getSeverity()) || "HIGH".equals(issue.getSeverity())) {
                notifyProducerAgent(issue);
                // 发送通知
                sendVerificationIssueNotification(issue);
            }
        }
    }

    /**
     * 通知ProducerAgent
     */
    private void notifyProducerAgent(VerificationIssue issue) {
        String content = String.format("【验证问题通知】\n\n严重级别: %s\n类别: %s\n问题: %s\n建议: %s",
            issue.getSeverity(), issue.getCategory(), issue.getDescription(), issue.getSuggestion());

        // 通过消息系统发送给ProducerAgent
        AgentMessage message = AgentMessage.builder()
            .fromAgentId(getId())
            .toAgentId(getProjectId() + ":producer")
            .type(AgentMessage.MessageType.SYSTEM)
            .content(content)
            .build();
        sendMessage(message);
        log.info("已通知ProducerAgent: {}", issue.getDescription());
    }

    /**
     * 生成验证报告
     */
    private void generateVerificationReport() {
        StringBuilder report = new StringBuilder();
        report.append("# 项目验证报告\n\n");
        report.append("项目: ").append(currentProject.getName()).append("\n");
        report.append("验证时间: ").append(LocalDateTime.now()).append("\n\n");

        report.append("## 验证结果汇总\n\n");

        int passed = 0;
        int failed = 0;

        for (Map.Entry<String, VerificationResult> entry : verificationResults.entrySet()) {
            VerificationResult result = entry.getValue();
            String status = result.isPassed() ? "✅ 通过" : "❌ 未通过";

            report.append("- ").append(entry.getKey()).append(": ").append(status).append("\n");
            report.append("  ").append(result.getMessage()).append("\n");

            if (result.isPassed()) {
                passed++;
            } else {
                failed++;
            }
        }

        report.append("\n## 统计\n\n");
        report.append("- 通过: ").append(passed).append("\n");
        report.append("- 未通过: ").append(failed).append("\n");
        report.append("- 通过率: ").append(passed + failed > 0 ? (passed * 100 / (passed + failed)) : 0).append("%\n");

        // 保存报告到知识库
        saveToKnowledgeBase("verification_report", report.toString());
        log.info("验证报告已生成");
    }

    /**
     * 收集设计文档
     */
    private String collectDesignDocuments() {
        StringBuilder docs = new StringBuilder();
        String workDir = currentProject.getWorkDir();

        if (workDir != null && !workDir.isEmpty()) {
            java.io.File dir = new java.io.File(workDir);
            if (dir.exists() && dir.isDirectory()) {
                java.io.File[] files = dir.listFiles((d, name) ->
                    name.endsWith(".md") || name.endsWith(".txt") ||
                    name.contains("design") || name.contains("plan") ||
                    name.contains("GDD"));
                if (files != null) {
                    for (java.io.File file : files) {
                        try {
                            String content = java.nio.file.Files.readString(file.toPath());
                            if (content.length() > 3000) {
                                content = content.substring(0, 3000) + "\n...(截断)";
                            }
                            docs.append("### ").append(file.getName()).append("\n");
                            docs.append(content).append("\n\n");
                        } catch (Exception e) {
                            // 忽略读取失败的文件
                        }
                    }
                }
            }
        }

        if (docs.isEmpty() && currentProject.getGoal() != null) {
            docs.append("项目目标：\n").append(currentProject.getGoal());
        }

        return docs.toString();
    }

    /**
     * 检查里程碑验收标准
     */
    private boolean checkMilestoneCriteria(GameProject.GoalMilestone milestone) {
        // 基本检查：里程碑必须有验收标准
        if (milestone.getVerificationCriteria() == null || milestone.getVerificationCriteria().isEmpty()) {
            return true;
        }

        // 检查项目目录中是否有相关产出
        String workDir = currentProject.getWorkDir();
        if (workDir == null || workDir.isEmpty()) {
            return false;
        }

        // 简单检查：项目目录非空且有代码文件
        java.io.File dir = new java.io.File(workDir);
        if (!dir.exists() || !dir.isDirectory()) {
            return false;
        }

        java.io.File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return false;
        }

        // 检查是否有代码文件
        boolean hasCodeFiles = false;
        for (java.io.File file : files) {
            if (file.isFile() && isCodeFile(file.getName())) {
                hasCodeFiles = true;
                break;
            }
        }

        return hasCodeFiles;
    }

    /**
     * 判断是否为代码文件
     */
    private boolean isCodeFile(String fileName) {
        String[] codeExtensions = {
            ".java", ".py", ".js", ".ts", ".jsx", ".tsx", ".vue", ".svelte",
            ".c", ".cpp", ".h", ".hpp", ".cs", ".go", ".rs", ".rb", ".php",
            ".swift", ".kt", ".dart", ".lua", ".html", ".css", ".scss"
        };

        String lowerName = fileName.toLowerCase();
        for (String ext : codeExtensions) {
            if (lowerName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查敏感文件
     */
    private void checkSensitiveFiles(java.io.File dir, List<String> violations) {
        if (dir == null || !dir.exists()) return;

        String[] sensitivePatterns = {".env", ".pem", ".key", ".secret", "password", "credentials"};

        java.io.File[] files = dir.listFiles();
        if (files != null) {
            for (java.io.File file : files) {
                if (file.isFile()) {
                    String name = file.getName().toLowerCase();
                    for (String pattern : sensitivePatterns) {
                        if (name.contains(pattern)) {
                            violations.add(String.format("发现敏感文件: %s，请确保不要提交到版本控制", file.getName()));
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * 获取目录大小
     */
    private long getDirectorySize(java.io.File dir) {
        long size = 0;
        if (dir.isDirectory()) {
            java.io.File[] files = dir.listFiles();
            if (files != null) {
                for (java.io.File file : files) {
                    if (file.isFile()) {
                        size += file.length();
                    } else if (file.isDirectory()) {
                        size += getDirectorySize(file);
                    }
                }
            }
        }
        return size;
    }

    /**
     * 保存到知识库
     */
    private void saveToKnowledgeBase(String key, String content) {
        try {
            if (memoryManager != null && currentProject != null) {
                memoryManager.saveMemory(currentProject, getId(), "verification", key, content);
            }
        } catch (Exception e) {
            log.warn("保存验证报告到知识库失败: {}", e.getMessage());
        }
    }

    // ===== Getter方法 =====

    /**
     * 获取验证结果
     */
    public Map<String, VerificationResult> getVerificationResults() {
        return Collections.unmodifiableMap(verificationResults);
    }

    /**
     * 获取上次完整验证时间
     */
    public LocalDateTime getLastFullVerificationTime() {
        return lastFullVerificationTime;
    }

    /**
     * 获取上次增量验证时间
     */
    public LocalDateTime getLastIncrementalVerificationTime() {
        return lastIncrementalVerificationTime;
    }

    /**
     * 获取验证摘要
     */
    public String getVerificationSummary() {
        int passed = 0;
        int failed = 0;

        for (VerificationResult result : verificationResults.values()) {
            if (result.isPassed()) {
                passed++;
            } else {
                failed++;
            }
        }

        return String.format("验证摘要: %d项通过, %d项未通过, 上次验证: %s",
            passed, failed,
            lastFullVerificationTime != null ? lastFullVerificationTime.toString() : "未验证");
    }
}
