package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.service.GameAnalysisTaskService;
import com.chengxun.gamemaker.service.GameDesignReviewService;
import com.chengxun.gamemaker.service.GameRuntimeVerifier;
import com.chengxun.gamemaker.web.entity.GameVerifyResult;
import com.chengxun.gamemaker.web.repository.GameVerifyResultRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 游戏运行时验证控制器
 * 提供游戏项目的运行时验证API，支持按项目查询验证状态和手动触发验证
 *
 * 主要功能：
 * - 结构验证：验证项目目录结构完整性（结果持久化到数据库）
 * - 深度分析：AI 分析游戏质量（支持后台异步执行）
 * - 任务管理：查询分析任务状态和结果
 *
 * @author chengxun
 * @since 2.0.0
 */
@RestController
@RequestMapping("/api/game-verify")
@PreAuthorize("hasAnyAuthority('PERM_projects:view', 'PERM_admin:manage')")
public class GameVerifyController {

    private static final Logger log = LoggerFactory.getLogger(GameVerifyController.class);

    @Autowired(required = false)
    private GameRuntimeVerifier gameRuntimeVerifier;

    @Autowired
    private ProjectManager projectManager;

    @Autowired(required = false)
    private GameAnalysisTaskService analysisTaskService;

    @Autowired
    private GameVerifyResultRepository verifyResultRepository;

    @Autowired(required = false)
    private com.chengxun.gamemaker.service.VerificationFeedbackService verificationFeedbackService;

    @Autowired(required = false)
    private com.chengxun.gamemaker.service.GameDesignReviewService gameDesignReviewService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 对指定项目执行运行时验证
     *
     * @param projectId 项目ID
     * @return 验证结果
     */
    @PostMapping("/{projectId}/verify")
    public ResponseEntity<Map<String, Object>> verifyProject(@PathVariable String projectId) {
        if (gameRuntimeVerifier == null) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "error", "验证服务未启用"
            ));
        }

        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "error", "项目不存在: " + projectId
            ));
        }

        String workDir = project.getWorkDir();
        if (workDir == null || workDir.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "error", "项目未设置工作目录"
            ));
        }

        log.info("手动触发项目验证: projectId={}, workDir={}", projectId, workDir);

        GameRuntimeVerifier.VerifyResult result = gameRuntimeVerifier.verify(workDir);

        // 保存到数据库
        GameVerifyResult entity = new GameVerifyResult();
        entity.setProjectId(projectId);
        entity.setProjectName(project.getName());
        entity.setSuccess(result.isSuccess());
        entity.setMessage(result.getMessage());
        entity.setError(result.getError());
        entity.setVerifyType("QUICK");
        entity.setVerifiedAt(LocalDateTime.now());

        // 转换 warnings 为 JSON
        if (result.getWarnings() != null && !result.getWarnings().isEmpty()) {
            try {
                entity.setWarningsJson(objectMapper.writeValueAsString(result.getWarnings()));
            } catch (JsonProcessingException e) {
                log.warn("序列化 warnings 失败: {}", e.getMessage());
            }
        }

        verifyResultRepository.save(entity);

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("message", result.getMessage());
        response.put("error", result.getError());
        response.put("warnings", result.getWarnings());
        response.put("hasWarnings", result.hasWarnings());
        response.put("timestamp", entity.getVerifiedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        response.put("projectId", projectId);
        response.put("projectName", project.getName());

        return ResponseEntity.ok(response);
    }

    /**
     * 获取项目的最近验证结果
     *
     * @param projectId 项目ID
     * @return 最近验证结果（无结果时返回空状态）
     */
    @GetMapping("/{projectId}/status")
    public ResponseEntity<Map<String, Object>> getVerifyStatus(@PathVariable String projectId) {
        Optional<GameVerifyResult> optional = verifyResultRepository
            .findFirstByProjectIdAndVerifyTypeOrderByVerifiedAtDesc(projectId, "QUICK");

        if (optional.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "hasResult", false,
                "projectId", projectId,
                "message", "尚未进行验证"
            ));
        }

        GameVerifyResult entity = optional.get();
        List<String> warnings = parseJsonList(entity.getWarningsJson());

        Map<String, Object> response = new HashMap<>();
        response.put("hasResult", true);
        response.put("success", entity.isSuccess());
        response.put("message", entity.getMessage());
        response.put("error", entity.getError());
        response.put("warnings", warnings);
        response.put("hasWarnings", warnings != null && !warnings.isEmpty());
        response.put("timestamp", entity.getVerifiedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        response.put("projectId", projectId);

        return ResponseEntity.ok(response);
    }

    /**
     * 批量获取多个项目的验证状态
     *
     * @param projectIds 项目ID列表
     * @return 各项目的验证状态
     */
    @PostMapping("/batch-status")
    public ResponseEntity<Map<String, Object>> batchVerifyStatus(@RequestBody List<String> projectIds) {
        Map<String, Object> results = new HashMap<>();

        // 批量查询
        List<GameVerifyResult> allResults = verifyResultRepository.findByProjectIdIn(projectIds);

        // 按 projectId 分组，取每个项目的最新结果
        Map<String, GameVerifyResult> latestByProject = new HashMap<>();
        for (GameVerifyResult r : allResults) {
            if ("QUICK".equals(r.getVerifyType())) {
                latestByProject.putIfAbsent(r.getProjectId(), r);
            }
        }

        for (String projectId : projectIds) {
            GameVerifyResult entity = latestByProject.get(projectId);
            Map<String, Object> status = new HashMap<>();

            if (entity != null) {
                status.put("hasResult", true);
                status.put("success", entity.isSuccess());
                status.put("message", entity.getMessage());
                status.put("error", entity.getError());
                status.put("timestamp", entity.getVerifiedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            } else {
                status.put("hasResult", false);
            }

            results.put(projectId, status);
        }

        return ResponseEntity.ok(results);
    }

    /**
     * 清除项目的验证结果
     */
    @DeleteMapping("/{projectId}/cache")
    public ResponseEntity<Map<String, Object>> clearCache(@PathVariable String projectId) {
        verifyResultRepository.deleteByProjectId(projectId);
        return ResponseEntity.ok(Map.of("success", true, "message", "验证结果已清除"));
    }

    /**
     * 解析 JSON 字符串为 List
     */
    private List<String> parseJsonList(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            log.warn("解析 JSON 列表失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * AI 深度分析游戏质量（后台异步执行）
     * 提交后台分析任务，不阻塞请求，分析完成后发送通知
     *
     * @param projectId 项目ID
     * @param authentication 当前认证用户
     * @return 任务信息
     */
    @PostMapping("/{projectId}/analyze")
    public ResponseEntity<Map<String, Object>> analyzeQuality(@PathVariable String projectId,
                                                                Authentication authentication) {
        if (gameRuntimeVerifier == null) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "error", "验证服务未启用"
            ));
        }

        if (analysisTaskService == null) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "error", "分析任务服务未启用"
            ));
        }

        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "error", "项目不存在: " + projectId
            ));
        }

        String workDir = project.getWorkDir();
        if (workDir == null || workDir.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "error", "项目未设置工作目录"
            ));
        }

        // 检查是否有正在进行的分析任务
        GameAnalysisTaskService.AnalysisTask latestTask = analysisTaskService.getLatestTask(projectId);
        if (latestTask != null &&
            (latestTask.getStatus() == GameAnalysisTaskService.TaskStatus.PENDING ||
             latestTask.getStatus() == GameAnalysisTaskService.TaskStatus.RUNNING)) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "分析任务已在进行中",
                "taskId", latestTask.getTaskId(),
                "status", latestTask.getStatus().name(),
                "progress", latestTask.getProgress()
            ));
        }

        // 获取请求者信息
        String requestedBy = "system";
        if (authentication != null) {
            requestedBy = authentication.getName();
        }

        log.info("提交后台分析任务: projectId={}, workDir={}", projectId, workDir);

        // 提交后台分析任务
        String taskId = analysisTaskService.submitTask(
            projectId, project.getName(), workDir, project.getGoal(), requestedBy);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "分析任务已提交，将在后台执行",
            "taskId", taskId,
            "status", "PENDING",
            "progress", 0
        ));
    }

    /**
     * 获取分析任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态和结果
     */
    @GetMapping("/task/{taskId}")
    public ResponseEntity<Map<String, Object>> getTaskStatus(@PathVariable String taskId) {
        if (analysisTaskService == null) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "error", "分析任务服务未启用"
            ));
        }

        GameAnalysisTaskService.AnalysisTask task = analysisTaskService.getTask(taskId);
        if (task == null) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "error", "任务不存在: " + taskId
            ));
        }

        Map<String, Object> response = new HashMap<>(task.toMap());
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取项目最新的分析任务状态
     *
     * @param projectId 项目ID
     * @return 任务状态和结果
     */
    @GetMapping("/{projectId}/analyze/status")
    public ResponseEntity<Map<String, Object>> getLatestAnalysisStatus(@PathVariable String projectId) {
        if (analysisTaskService == null) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "error", "分析任务服务未启用"
            ));
        }

        GameAnalysisTaskService.AnalysisTask task = analysisTaskService.getLatestTask(projectId);
        if (task == null) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "hasTask", false,
                "message", "暂无分析任务"
            ));
        }

        Map<String, Object> response = new HashMap<>(task.toMap());
        response.put("success", true);
        response.put("hasTask", true);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取项目所有分析任务历史
     *
     * @param projectId 项目ID
     * @return 任务列表
     */
    @GetMapping("/{projectId}/analyze/history")
    public ResponseEntity<Map<String, Object>> getAnalysisHistory(@PathVariable String projectId) {
        if (analysisTaskService == null) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "error", "分析任务服务未启用"
            ));
        }

        List<GameAnalysisTaskService.AnalysisTask> tasks = analysisTaskService.getProjectTasks(projectId);

        List<Map<String, Object>> taskList = new ArrayList<>();
        for (GameAnalysisTaskService.AnalysisTask task : tasks) {
            taskList.add(task.toMap());
        }

        return ResponseEntity.ok(Map.of(
            "success", true,
            "projectId", projectId,
            "tasks", taskList,
            "total", taskList.size()
        ));
    }

    /**
     * 游戏设计审查
     * 对项目的设计文档进行 AI 审查，检查核心循环、有趣度、差异化等
     *
     * @param projectId 项目ID
     * @return 审查结果
     */
    @PostMapping("/{projectId}/design-review")
    public ResponseEntity<Map<String, Object>> reviewDesign(@PathVariable String projectId) {
        if (gameDesignReviewService == null) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "error", "设计审查服务未启用"
            ));
        }

        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "error", "项目不存在: " + projectId
            ));
        }

        String goal = project.getGoal();
        if (goal == null || goal.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "error", "项目未设置目标描述"
            ));
        }

        // 收集项目中的设计文档
        String designDocuments = collectDesignDocuments(project);

        log.info("触发游戏设计审查: projectId={}", projectId);

        GameDesignReviewService.DesignReviewResult result =
            gameDesignReviewService.reviewDesign(goal, designDocuments, null);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("reviewed", result.reviewed);
        response.put("passed", result.passed);
        response.put("score", result.score);
        response.put("issues", result.issues.stream().map(issue -> {
            Map<String, String> issueMap = new HashMap<>();
            issueMap.put("severity", issue.severity);
            issueMap.put("description", issue.description);
            issueMap.put("suggestion", issue.suggestion);
            return issueMap;
        }).toList());
        response.put("strengths", result.strengths);
        response.put("summary", result.summary);
        response.put("report", result.toReport());

        return ResponseEntity.ok(response);
    }

    /**
     * 收集项目中的设计文档内容
     */
    private String collectDesignDocuments(GameProject project) {
        StringBuilder docs = new StringBuilder();
        String workDir = project.getWorkDir();

        if (workDir != null && !workDir.isEmpty()) {
            java.io.File dir = new java.io.File(workDir);
            if (dir.exists() && dir.isDirectory()) {
                // 查找 .md 和 .txt 文件作为设计文档
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

        // 如果没有找到设计文档，使用项目目标作为设计输入
        if (docs.isEmpty() && project.getGoal() != null) {
            docs.append("项目目标：\n").append(project.getGoal());
        }

        return docs.toString();
    }

    /**
     * 缓存的验证结果
     */
    private static class CachedVerifyResult {
        final boolean success;
        final String message;
        final String error;
        final List<String> warnings;
        final long timestamp;

        CachedVerifyResult(boolean success, String message, String error, List<String> warnings, long timestamp) {
            this.success = success;
            this.message = message;
            this.error = error;
            this.warnings = warnings;
            this.timestamp = timestamp;
        }
    }
}
