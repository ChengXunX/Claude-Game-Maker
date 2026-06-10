package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.web.entity.GameAnalysisTask;
import com.chengxun.gamemaker.web.repository.GameAnalysisTaskRepository;
import com.chengxun.gamemaker.web.service.NotificationService;
import com.chengxun.gamemaker.web.service.NotificationTemplateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 游戏分析任务服务
 * 管理后台异步分析任务，支持任务状态查询和完成通知
 *
 * 主要功能：
 * - 异步执行游戏质量深度分析
 * - 任务状态管理和查询
 * - 分析完成后发送通知
 * - 结果持久化到数据库，支持重启后恢复历史
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class GameAnalysisTaskService {

    private static final Logger log = LoggerFactory.getLogger(GameAnalysisTaskService.class);

    @Autowired
    private GameRuntimeVerifier gameRuntimeVerifier;

    @Autowired(required = false)
    private NotificationService notificationService;

    @Autowired(required = false)
    private NotificationTemplateService templateService;

    @Autowired
    private GameAnalysisTaskRepository taskRepository;

    @Autowired(required = false)
    private VerificationFeedbackService verificationFeedbackService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 内存缓存（用于正在执行的任务）
     * key: taskId
     * value: 任务信息
     */
    private final ConcurrentHashMap<String, AnalysisTask> runningTasks = new ConcurrentHashMap<>();

    /**
     * 分析任务状态
     */
    public enum TaskStatus {
        PENDING,    // 等待执行
        RUNNING,    // 执行中
        COMPLETED,  // 已完成
        FAILED      // 失败
    }

    /**
     * 分析任务信息（内存模型）
     */
    public static class AnalysisTask {
        private final String taskId;
        private final String projectId;
        private final String projectName;
        private final String projectDir;
        private final String projectGoal;
        private final String requestedBy;
        private final LocalDateTime createdAt;

        private volatile TaskStatus status;
        private volatile LocalDateTime startedAt;
        private volatile LocalDateTime completedAt;
        private volatile GameRuntimeVerifier.QualityAnalysisResult result;
        private volatile String errorMessage;
        private volatile int progress; // 0-100

        public AnalysisTask(String taskId, String projectId, String projectName,
                           String projectDir, String projectGoal, String requestedBy) {
            this.taskId = taskId;
            this.projectId = projectId;
            this.projectName = projectName;
            this.projectDir = projectDir;
            this.projectGoal = projectGoal;
            this.requestedBy = requestedBy;
            this.createdAt = LocalDateTime.now();
            this.status = TaskStatus.PENDING;
            this.progress = 0;
        }

        // Getters and Setters
        public String getTaskId() { return taskId; }
        public String getProjectId() { return projectId; }
        public String getProjectName() { return projectName; }
        public String getProjectDir() { return projectDir; }
        public String getProjectGoal() { return projectGoal; }
        public String getRequestedBy() { return requestedBy; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public TaskStatus getStatus() { return status; }
        public void setStatus(TaskStatus status) { this.status = status; }
        public LocalDateTime getStartedAt() { return startedAt; }
        public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
        public LocalDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
        public GameRuntimeVerifier.QualityAnalysisResult getResult() { return result; }
        public void setResult(GameRuntimeVerifier.QualityAnalysisResult result) { this.result = result; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = progress; }

        /**
         * 转换为 Map（用于 API 返回）
         */
        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("taskId", taskId);
            map.put("projectId", projectId);
            map.put("projectName", projectName);
            map.put("status", status.name());
            map.put("progress", progress);
            map.put("createdAt", createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            if (startedAt != null) {
                map.put("startedAt", startedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            if (completedAt != null) {
                map.put("completedAt", completedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            if (result != null) {
                map.put("result", resultToMap(result));
            }
            if (errorMessage != null) {
                map.put("errorMessage", errorMessage);
            }
            map.put("requestedBy", requestedBy);
            return map;
        }

        private Map<String, Object> resultToMap(GameRuntimeVerifier.QualityAnalysisResult r) {
            Map<String, Object> resultMap = new LinkedHashMap<>();
            resultMap.put("success", r.isSuccess());
            resultMap.put("overallScore", r.getOverallScore());
            resultMap.put("runnableScore", r.getRunnableScore());
            resultMap.put("playableScore", r.getPlayableScore());
            resultMap.put("completenessScore", r.getCompletenessScore());
            resultMap.put("uiuxScore", r.getUiuxScore());
            resultMap.put("codeQualityScore", r.getCodeQualityScore());
            resultMap.put("summary", r.getSummary());
            resultMap.put("strengths", r.getStrengths());
            resultMap.put("issues", r.getIssues());
            resultMap.put("suggestions", r.getSuggestions());
            return resultMap;
        }
    }

    /**
     * 提交分析任务
     *
     * @param projectId 项目 ID
     * @param projectName 项目名称
     * @param projectDir 项目目录
     * @param projectGoal 项目目标
     * @param requestedBy 请求者
     * @return 任务 ID
     */
    public String submitTask(String projectId, String projectName, String projectDir,
                            String projectGoal, String requestedBy) {
        String taskId = "analysis-" + projectId + "-" + System.currentTimeMillis();

        // 创建内存任务对象
        AnalysisTask task = new AnalysisTask(taskId, projectId, projectName,
                                            projectDir, projectGoal, requestedBy);

        // 保存到数据库
        GameAnalysisTask entity = new GameAnalysisTask();
        entity.setTaskId(taskId);
        entity.setProjectId(projectId);
        entity.setProjectName(projectName);
        entity.setProjectDir(projectDir);
        entity.setProjectGoal(projectGoal);
        entity.setRequestedBy(requestedBy);
        entity.setStatus("PENDING");
        entity.setProgress(0);
        entity.setCreatedAt(LocalDateTime.now());
        taskRepository.save(entity);

        // 缓存到内存
        runningTasks.put(taskId, task);

        log.info("分析任务已提交: taskId={}, project={}", taskId, projectName);

        // 异步执行任务
        executeTaskAsync(task);

        return taskId;
    }

    /**
     * 异步执行分析任务
     */
    @Async
    protected void executeTaskAsync(AnalysisTask task) {
        log.info("开始执行分析任务: taskId={}, project={}", task.getTaskId(), task.getProjectName());

        task.setStatus(TaskStatus.RUNNING);
        task.setStartedAt(LocalDateTime.now());
        task.setProgress(10);

        // 更新数据库状态
        updateTaskInDb(task);

        try {
            // 执行深度分析
            task.setProgress(30);
            updateTaskInDb(task);

            GameRuntimeVerifier.QualityAnalysisResult result =
                gameRuntimeVerifier.analyzeQuality(task.getProjectDir(), task.getProjectName(), task.getProjectGoal());

            task.setProgress(90);
            task.setResult(result);
            task.setStatus(TaskStatus.COMPLETED);
            task.setCompletedAt(LocalDateTime.now());
            task.setProgress(100);

            // 更新数据库
            updateTaskInDb(task);

            log.info("分析任务完成: taskId={}, score={}", task.getTaskId(), result.getOverallScore());

            // 将验证结果反馈给 Agent（生成修复任务）
            if (verificationFeedbackService != null) {
                try {
                    verificationFeedbackService.processVerificationResult(task.getProjectId(), result);
                    log.info("验证反馈已处理: projectId={}", task.getProjectId());
                } catch (Exception e) {
                    log.error("验证反馈处理失败: {}", e.getMessage(), e);
                }
            }

            // 发送完成通知
            sendCompletionNotification(task);

        } catch (Exception e) {
            log.error("分析任务失败: taskId={}", task.getTaskId(), e);
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage(e.getMessage());
            task.setCompletedAt(LocalDateTime.now());

            // 更新数据库
            updateTaskInDb(task);

            // 发送失败通知
            sendFailureNotification(task);
        } finally {
            // 执行完成后从内存缓存移除
            runningTasks.remove(task.getTaskId());
        }
    }

    /**
     * 更新数据库中的任务状态
     */
    private void updateTaskInDb(AnalysisTask task) {
        try {
            Optional<GameAnalysisTask> optional = taskRepository.findById(task.getTaskId());
            if (optional.isPresent()) {
                GameAnalysisTask entity = optional.get();
                entity.setStatus(task.getStatus().name());
                entity.setProgress(task.getProgress());
                entity.setStartedAt(task.getStartedAt());
                entity.setCompletedAt(task.getCompletedAt());
                entity.setErrorMessage(task.getErrorMessage());

                // 保存分析结果
                if (task.getResult() != null) {
                    GameRuntimeVerifier.QualityAnalysisResult r = task.getResult();
                    entity.setOverallScore(r.getOverallScore());
                    entity.setRunnableScore(r.getRunnableScore());
                    entity.setPlayableScore(r.getPlayableScore());
                    entity.setCompletenessScore(r.getCompletenessScore());
                    entity.setUiuxScore(r.getUiuxScore());
                    entity.setCodeQualityScore(r.getCodeQualityScore());
                    entity.setSummary(r.getSummary());

                    try {
                        if (r.getStrengths() != null) {
                            entity.setStrengthsJson(objectMapper.writeValueAsString(r.getStrengths()));
                        }
                        if (r.getIssues() != null) {
                            entity.setIssuesJson(objectMapper.writeValueAsString(r.getIssues()));
                        }
                        if (r.getSuggestions() != null) {
                            entity.setSuggestionsJson(objectMapper.writeValueAsString(r.getSuggestions()));
                        }
                    } catch (JsonProcessingException e) {
                        log.warn("序列化分析结果失败: {}", e.getMessage());
                    }
                }

                taskRepository.save(entity);
            }
        } catch (Exception e) {
            log.error("更新数据库任务状态失败: taskId={}", task.getTaskId(), e);
        }
    }

    /**
     * 发送完成通知
     */
    private void sendCompletionNotification(AnalysisTask task) {
        if (notificationService == null) {
            log.warn("通知服务未注入，跳过发送分析完成通知");
            return;
        }

        try {
            GameRuntimeVerifier.QualityAnalysisResult result = task.getResult();
            int score = result != null ? result.getOverallScore() : 0;
            String status = score >= 80 ? "优秀" : (score >= 60 ? "良好" : "需改进");

            Map<String, String> variables = new HashMap<>();
            variables.put("projectName", task.getProjectName());
            variables.put("score", String.valueOf(score));
            variables.put("status", status);
            variables.put("summary", result != null && result.getSummary() != null ?
                result.getSummary() : "无");
            variables.put("taskId", task.getTaskId());
            variables.put("requestedBy", task.getRequestedBy());

            log.info("准备发送分析完成通知: taskId={}, project={}, score={}", task.getTaskId(), task.getProjectName(), score);

            // 使用通知模板发送
            String templateCode = "GAME_ANALYSIS_COMPLETED";
            notificationService.notifyAdmins("GAME_ANALYSIS", templateCode, variables,
                com.chengxun.gamemaker.web.entity.Notification.NotificationType.INFO);

            log.info("分析完成通知已发送: taskId={}", task.getTaskId());
        } catch (Exception e) {
            log.error("发送分析完成通知失败: taskId={}, error={}", task.getTaskId(), e.getMessage(), e);
        }
    }

    /**
     * 发送失败通知
     */
    private void sendFailureNotification(AnalysisTask task) {
        if (notificationService == null) return;

        try {
            Map<String, String> variables = new HashMap<>();
            variables.put("projectName", task.getProjectName());
            variables.put("errorMessage", task.getErrorMessage() != null ? task.getErrorMessage() : "未知错误");

            notificationService.notifyAdmins("GAME_ANALYSIS", "GAME_ANALYSIS_FAILED", variables,
                com.chengxun.gamemaker.web.entity.Notification.NotificationType.WARNING);

            log.info("分析失败通知已发送: taskId={}", task.getTaskId());
        } catch (Exception e) {
            log.warn("发送分析失败通知失败: {}", e.getMessage());
        }
    }

    /**
     * 获取任务状态
     * 优先从内存缓存获取（正在执行的任务），否则从数据库获取
     *
     * @param taskId 任务 ID
     * @return 任务信息，不存在返回 null
     */
    public AnalysisTask getTask(String taskId) {
        // 先从内存缓存获取
        AnalysisTask cached = runningTasks.get(taskId);
        if (cached != null) return cached;

        // 从数据库获取
        Optional<GameAnalysisTask> optional = taskRepository.findById(taskId);
        if (optional.isPresent()) {
            return convertToAnalysisTask(optional.get());
        }
        return null;
    }

    /**
     * 获取项目最近的分析任务
     *
     * @param projectId 项目 ID
     * @return 任务信息，不存在返回 null
     */
    public AnalysisTask getLatestTask(String projectId) {
        // 先检查内存缓存
        for (AnalysisTask task : runningTasks.values()) {
            if (projectId.equals(task.getProjectId())) {
                return task;
            }
        }

        // 从数据库获取
        Optional<GameAnalysisTask> optional = taskRepository.findFirstByProjectIdOrderByCreatedAtDesc(projectId);
        if (optional.isPresent()) {
            return convertToAnalysisTask(optional.get());
        }
        return null;
    }

    /**
     * 获取项目所有分析任务
     *
     * @param projectId 项目 ID
     * @return 任务列表（按时间倒序）
     */
    public List<AnalysisTask> getProjectTasks(String projectId) {
        List<GameAnalysisTask> entities = taskRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        List<AnalysisTask> tasks = new ArrayList<>();
        for (GameAnalysisTask entity : entities) {
            tasks.add(convertToAnalysisTask(entity));
        }
        return tasks;
    }

    /**
     * 获取所有任务
     *
     * @return 任务列表（按时间倒序）
     */
    public List<AnalysisTask> getAllTasks() {
        List<GameAnalysisTask> entities = taskRepository.findAll();
        List<AnalysisTask> tasks = new ArrayList<>();
        for (GameAnalysisTask entity : entities) {
            tasks.add(convertToAnalysisTask(entity));
        }
        tasks.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return tasks;
    }

    /**
     * 将数据库实体转换为内存任务对象
     */
    private AnalysisTask convertToAnalysisTask(GameAnalysisTask entity) {
        AnalysisTask task = new AnalysisTask(
            entity.getTaskId(),
            entity.getProjectId(),
            entity.getProjectName(),
            entity.getProjectDir(),
            entity.getProjectGoal(),
            entity.getRequestedBy()
        );

        // 设置状态
        try {
            task.setStatus(TaskStatus.valueOf(entity.getStatus()));
        } catch (Exception e) {
            task.setStatus(TaskStatus.PENDING);
        }

        task.setStartedAt(entity.getStartedAt());
        task.setCompletedAt(entity.getCompletedAt());
        task.setErrorMessage(entity.getErrorMessage());
        task.setProgress(entity.getProgress());

        // 如果有分析结果，构建 QualityAnalysisResult
        if (entity.getOverallScore() != null) {
            GameRuntimeVerifier.QualityAnalysisResult result = new GameRuntimeVerifier.QualityAnalysisResult();
            result.setSuccess(entity.isSuccess());
            result.setOverallScore(entity.getOverallScore());
            result.setRunnableScore(entity.getRunnableScore() != null ? entity.getRunnableScore() : 0);
            result.setPlayableScore(entity.getPlayableScore() != null ? entity.getPlayableScore() : 0);
            result.setCompletenessScore(entity.getCompletenessScore() != null ? entity.getCompletenessScore() : 0);
            result.setUiuxScore(entity.getUiuxScore() != null ? entity.getUiuxScore() : 0);
            result.setCodeQualityScore(entity.getCodeQualityScore() != null ? entity.getCodeQualityScore() : 0);
            result.setSummary(entity.getSummary());

            // 解析 JSON 列表
            try {
                if (entity.getStrengthsJson() != null && !entity.getStrengthsJson().isEmpty()) {
                    result.setStrengths(objectMapper.readValue(entity.getStrengthsJson(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)));
                }
                if (entity.getIssuesJson() != null && !entity.getIssuesJson().isEmpty()) {
                    result.setIssues(objectMapper.readValue(entity.getIssuesJson(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)));
                }
                if (entity.getSuggestionsJson() != null && !entity.getSuggestionsJson().isEmpty()) {
                    result.setSuggestions(objectMapper.readValue(entity.getSuggestionsJson(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)));
                }
            } catch (JsonProcessingException e) {
                log.warn("解析 JSON 列表失败: {}", e.getMessage());
            }

            task.setResult(result);
        }

        return task;
    }

    /**
     * 清理过期任务（保留最近 100 个）
     */
    public void cleanupOldTasks() {
        long count = taskRepository.count();
        if (count <= 100) return;

        // 删除较旧的任务
        List<GameAnalysisTask> allTasks = taskRepository.findAll();
        allTasks.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        for (int i = 100; i < allTasks.size(); i++) {
            taskRepository.delete(allTasks.get(i));
        }

        log.info("清理过期分析任务完成，保留 100 个，删除 {} 个", count - 100);
    }
}
