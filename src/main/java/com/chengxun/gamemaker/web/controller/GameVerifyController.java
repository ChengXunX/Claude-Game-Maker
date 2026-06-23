package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.service.BuildStrategyRegistry;
import com.chengxun.gamemaker.service.GameAnalysisTaskService;
import com.chengxun.gamemaker.service.GameDesignReviewService;
import com.chengxun.gamemaker.service.GameRuntimeVerifier;
import com.chengxun.gamemaker.web.constants.PermissionConstants;
import com.chengxun.gamemaker.web.entity.DesignReviewRecord;
import com.chengxun.gamemaker.web.entity.GameVerifyResult;
import com.chengxun.gamemaker.web.repository.DesignReviewRecordRepository;
import com.chengxun.gamemaker.web.repository.GameVerifyResultRepository;
import com.chengxun.gamemaker.web.util.ApiResponseUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * 游戏验证控制器
 * 提供游戏项目的验证、预览和设计审查功能
 *
 * 主要功能：
 * - 结构验证：验证项目目录结构完整性（结果持久化到数据库）
 * - 完整验证：触发结构+构建+运行+质量分析的完整验证流程
 * - 游戏预览：启动/停止游戏预览服务
 * - 深度分析：AI 分析游戏质量（支持后台异步执行）
 * - 设计审查：AI 审查游戏设计文档
 * - 任务管理：查询分析任务状态和结果
 *
 * @author chengxun
 * @since 2.0.0
 */
@RestController
@RequestMapping("/api/game-verify")
@Tag(name = "游戏验证", description = "游戏项目验证、预览和设计审查")
@PreAuthorize("hasAnyAuthority('PERM_" + PermissionConstants.GAME_VERIFY + "', 'PERM_" + PermissionConstants.GAME_VERIFY_VIEW + "', 'PERM_" + PermissionConstants.GAME_PREVIEW + "', 'PERM_projects:view', 'PERM_admin:manage', '*')")
public class GameVerifyController {

    private static final Logger log = LoggerFactory.getLogger(GameVerifyController.class);

    @Autowired(required = false)
    private GameRuntimeVerifier gameRuntimeVerifier;

    @Autowired(required = false)
    private BuildStrategyRegistry buildStrategyRegistry;

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

    @Autowired
    private com.chengxun.gamemaker.web.repository.DesignReviewRecordRepository designReviewRecordRepository;

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
     * 触发完整验证
     * 对游戏项目执行结构+构建+运行+质量分析的完整验证流程
     *
     * @param projectId 项目ID
     * @param projectDir 项目目录路径
     * @return 完整验证结果
     */
    @Operation(summary = "触发完整验证", description = "对游戏项目执行结构+构建+运行+质量分析的完整验证")
    @PostMapping("/projects/{projectId}/verify")
    @PreAuthorize("hasAnyAuthority('PERM_" + PermissionConstants.GAME_VERIFY + "', 'PERM_projects:manage', 'PERM_admin:manage', '*')")
    public ResponseEntity<?> triggerFullVerify(@PathVariable String projectId,
                                                @RequestParam(required = false) String projectDir) {
        log.info("触发完整游戏验证: projectId={}, projectDir={}", projectId, projectDir);
        try {
            if (gameRuntimeVerifier == null) {
                return ApiResponseUtil.error("验证服务不可用");
            }

            // 如果未指定目录，从项目信息获取
            if (projectDir == null || projectDir.isEmpty()) {
                GameProject project = projectManager.getProject(projectId);
                if (project != null) {
                    projectDir = project.getWorkDir();
                }
            }

            if (projectDir == null || projectDir.isEmpty()) {
                return ApiResponseUtil.error("项目目录未指定");
            }

            GameRuntimeVerifier.FullVerifyResult result = gameRuntimeVerifier.verifyFull(projectDir, null, null);
            return ApiResponseUtil.success("完整验证完成", result);
        } catch (Exception e) {
            log.error("完整验证失败: projectId={}", projectId, e);
            return ApiResponseUtil.error("验证失败: " + e.getMessage());
        }
    }

    /**
     * 获取最近验证结果
     * 获取项目最近一次验证的详细报告
     *
     * @param projectId 项目ID
     * @return 最近验证结果
     */
    @Operation(summary = "获取最近验证结果", description = "获取项目最近一次验证的详细报告")
    @GetMapping("/projects/{projectId}/verify/latest")
    @PreAuthorize("hasAnyAuthority('PERM_" + PermissionConstants.GAME_VERIFY_VIEW + "', 'PERM_projects:view', 'PERM_admin:manage', '*')")
    public ResponseEntity<?> getLatestVerify(@PathVariable String projectId) {
        Optional<GameVerifyResult> optional = verifyResultRepository
            .findFirstByProjectIdOrderByVerifiedAtDesc(projectId);

        if (optional.isEmpty()) {
            return ApiResponseUtil.success("暂无验证记录");
        }

        GameVerifyResult entity = optional.get();
        Map<String, Object> response = new HashMap<>();
        response.put("hasResult", true);
        response.put("success", entity.isSuccess());
        response.put("message", entity.getMessage());
        response.put("error", entity.getError());
        response.put("verifyType", entity.getVerifyType());
        response.put("timestamp", entity.getVerifiedAt() != null
            ? entity.getVerifiedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        response.put("projectId", projectId);

        return ApiResponseUtil.success("查询成功", response);
    }

    /**
     * 启动游戏预览
     * 启动游戏服务并返回预览URL
     *
     * @param projectId 项目ID
     * @param projectDir 项目目录路径
     * @param port 预览端口，默认18100
     * @return 预览URL或错误信息
     */
    @Operation(summary = "启动游戏预览", description = "启动游戏服务并返回预览URL")
    @PostMapping("/projects/{projectId}/preview/start")
    @PreAuthorize("hasAnyAuthority('PERM_" + PermissionConstants.GAME_PREVIEW + "', 'PERM_projects:manage', 'PERM_admin:manage', '*')")
    public ResponseEntity<?> startPreview(@PathVariable String projectId,
                                           @RequestParam(required = false) String projectDir,
                                           @RequestParam(defaultValue = "18100") int port) {
        log.info("启动游戏预览: projectId={}, projectDir={}, port={}", projectId, projectDir, port);
        try {
            if (buildStrategyRegistry == null) {
                return ApiResponseUtil.error("构建策略服务不可用");
            }

            // 如果未指定目录，从项目信息获取
            if (projectDir == null || projectDir.isEmpty()) {
                GameProject project = projectManager.getProject(projectId);
                if (project != null) {
                    projectDir = project.getWorkDir();
                }
            }

            if (projectDir == null || projectDir.isEmpty()) {
                return ApiResponseUtil.error("项目目录未指定");
            }

            BuildStrategyRegistry.GameProcess gp = buildStrategyRegistry.startGame(projectDir, port);

            // 等待游戏进程就绪，最多等待30秒
            boolean ready = false;
            for (int i = 0; i < 30; i++) {
                if (gp.isReady()) {
                    ready = true;
                    break;
                }
                Thread.sleep(1000);
            }

            if (ready) {
                String previewUrl = String.format("http://localhost:%d", port);
                log.info("游戏预览已启动: projectId={}, url={}", projectId, previewUrl);
                return ApiResponseUtil.success("游戏已启动",
                    Map.of("previewUrl", previewUrl, "port", port));
            } else {
                gp.stop();
                return ApiResponseUtil.error("游戏启动超时（30秒）");
            }
        } catch (Exception e) {
            log.error("启动预览失败: projectId={}", projectId, e);
            return ApiResponseUtil.error("启动失败: " + e.getMessage());
        }
    }

    /**
     * 停止游戏预览
     * 停止游戏预览服务
     *
     * @param projectId 项目ID
     * @return 操作结果
     */
    @Operation(summary = "停止游戏预览", description = "停止游戏预览服务")
    @PostMapping("/projects/{projectId}/preview/stop")
    @PreAuthorize("hasAnyAuthority('PERM_" + PermissionConstants.GAME_PREVIEW + "', 'PERM_projects:manage', 'PERM_admin:manage', '*')")
    public ResponseEntity<?> stopPreview(@PathVariable String projectId) {
        log.info("停止游戏预览: projectId={}", projectId);
        // TODO: 实现停止预览逻辑，需要维护活跃的GameProcess映射
        return ApiResponseUtil.success("预览已停止");
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

        // 保存审查记录到数据库
        try {
            DesignReviewRecord record = new DesignReviewRecord();
            record.setProjectId(projectId);
            record.setProjectName(project.getName());
            record.setScore(result.score);
            record.setPassed(result.passed);
            record.setSummary(result.summary);
            record.setStrengths(objectMapper.writeValueAsString(result.strengths));
            record.setIssues(objectMapper.writeValueAsString(result.issues.stream().map(issue -> {
                Map<String, String> issueMap = new HashMap<>();
                issueMap.put("severity", issue.severity);
                issueMap.put("description", issue.description);
                issueMap.put("suggestion", issue.suggestion);
                return issueMap;
            }).toList()));
            record.setReport(result.toReport());
            designReviewRecordRepository.save(record);
        } catch (Exception e) {
            log.warn("保存设计审查记录失败: {}", e.getMessage());
        }

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
     * 获取设计审查历史
     */
    @GetMapping("/{projectId}/design-review/history")
    @Operation(summary = "获取设计审查历史")
    public ResponseEntity<List<DesignReviewRecord>> getDesignReviewHistory(@PathVariable String projectId) {
        List<DesignReviewRecord> records = designReviewRecordRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        return ResponseEntity.ok(records);
    }

    /**
     * 收集项目中的设计文档内容
     * 从项目目录和里程碑产出中收集设计文档
     */
    private String collectDesignDocuments(GameProject project) {
        StringBuilder docs = new StringBuilder();
        String workDir = project.getWorkDir();

        // 1. 从工作目录扫描文档（包含子目录）
        if (workDir != null && !workDir.isEmpty()) {
            java.io.File dir = new java.io.File(workDir);
            if (dir.exists() && dir.isDirectory()) {
                scanDirectory(dir, docs, 0, 3); // 最多递归3层
            }
        }

        // 2. 从里程碑产出中收集设计信息
        if (project.getMilestones() != null) {
            for (var milestone : project.getMilestones()) {
                if (milestone.getDescription() != null && !milestone.getDescription().isEmpty()) {
                    docs.append("### 里程碑: ").append(milestone.getTitle()).append("\n");
                    docs.append(milestone.getDescription()).append("\n\n");
                }
                // 收集任务结果
                if (milestone.getTasks() != null) {
                    for (var task : milestone.getTasks()) {
                        if (task.getResult() != null && !task.getResult().isEmpty()
                            && task.getResult().length() > 50) {
                            docs.append("### 任务: ").append(task.getTitle()).append("\n");
                            String result = task.getResult();
                            if (result.length() > 2000) result = result.substring(0, 2000) + "\n...(截断)";
                            docs.append(result).append("\n\n");
                        }
                    }
                }
            }
        }

        // 3. 如果还是空的，使用项目目标
        if (docs.isEmpty() && project.getGoal() != null) {
            docs.append("项目目标：\n").append(project.getGoal());
        }

        return docs.toString();
    }

    /**
     * 递归扫描目录中的文档
     */
    private void scanDirectory(java.io.File dir, StringBuilder docs, int depth, int maxDepth) {
        if (depth > maxDepth) return;

        java.io.File[] files = dir.listFiles();
        if (files == null) return;

        for (java.io.File file : files) {
            if (file.isDirectory()) {
                // 跳过隐藏目录和构建目录
                if (!file.getName().startsWith(".") && !file.getName().equals("node_modules")
                    && !file.getName().equals("target") && !file.getName().equals("build")) {
                    scanDirectory(file, docs, depth + 1, maxDepth);
                }
            } else if (file.getName().endsWith(".md") || file.getName().endsWith(".txt") ||
                       file.getName().contains("design") || file.getName().contains("plan") ||
                       file.getName().contains("GDD") || file.getName().contains("README")) {
                if (file.length() > 100000) continue; // 跳过超大文件
                try {
                    String content = java.nio.file.Files.readString(file.toPath());
                    if (content.length() > 3000) {
                        content = content.substring(0, 3000) + "\n...(截断)";
                    }
                    if (content.length() > 50) { // 只包含有实际内容的文件
                        docs.append("### ").append(file.getName()).append("\n");
                        docs.append(content).append("\n\n");
                    }
                } catch (Exception e) {
                    // 忽略读取失败的文件
                }
            }
        }
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
