package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.service.GameEvaluationService;
import com.chengxun.gamemaker.service.GameSandboxService;
import com.chengxun.gamemaker.service.ProjectImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * 游戏评估和导入 API 控制器
 *
 * @author chengxun
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/game-tools")
@Tag(name = "游戏工具", description = "游戏评估、沙盒、导入等工具接口")
public class GameEvaluationApiController {

    private static final Logger log = LoggerFactory.getLogger(GameEvaluationApiController.class);

    private final GameEvaluationService evaluationService;
    private final GameSandboxService sandboxService;
    private final ProjectImportService importService;

    public GameEvaluationApiController(GameEvaluationService evaluationService,
                                       GameSandboxService sandboxService,
                                       ProjectImportService importService) {
        this.evaluationService = evaluationService;
        this.sandboxService = sandboxService;
        this.importService = importService;
    }

    // ===== 游戏评估 =====

    /**
     * 评估游戏项目
     */
    @PostMapping("/evaluate")
    @Operation(summary = "评估游戏项目")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> evaluateProject(@RequestBody Map<String, String> data) {
        String projectId = data.get("projectId");
        String projectPath = data.get("projectPath");
        String userPrompt = data.get("userPrompt");

        GameEvaluationService.EvaluationResult result = evaluationService.evaluate(projectId, projectPath, userPrompt);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取评估结果
     */
    @GetMapping("/evaluate/{projectId}")
    @Operation(summary = "获取评估结果")
    public ResponseEntity<?> getEvaluationResult(@PathVariable String projectId) {
        GameEvaluationService.EvaluationResult result = evaluationService.getResult(projectId);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    // ===== 沙盒执行 =====

    /**
     * 在沙盒中执行命令
     */
    @PostMapping("/sandbox/execute")
    @Operation(summary = "沙盒执行命令")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> sandboxExecute(@RequestBody Map<String, Object> data) {
        String projectId = (String) data.get("projectId");
        String projectPath = (String) data.get("projectPath");
        String command = (String) data.get("command");
        int timeout = data.containsKey("timeout") ? (int) data.get("timeout") : 30;

        GameSandboxService.ExecutionResult result = sandboxService.execute(projectId, projectPath, command, timeout);
        return ResponseEntity.ok(result);
    }

    /**
     * 在沙盒中运行 npm install
     */
    @PostMapping("/sandbox/npm-install")
    @Operation(summary = "沙盒运行 npm install")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> sandboxNpmInstall(@RequestBody Map<String, String> data) {
        GameSandboxService.ExecutionResult result = sandboxService.npmInstall(data.get("projectId"), data.get("projectPath"));
        return ResponseEntity.ok(result);
    }

    /**
     * 在沙盒中运行 npm run build
     */
    @PostMapping("/sandbox/npm-build")
    @Operation(summary = "沙盒运行 npm build")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> sandboxNpmBuild(@RequestBody Map<String, String> data) {
        GameSandboxService.ExecutionResult result = sandboxService.npmRunBuild(data.get("projectId"), data.get("projectPath"));
        return ResponseEntity.ok(result);
    }

    /**
     * 获取沙盒执行结果
     */
    @GetMapping("/sandbox/result/{projectId}")
    @Operation(summary = "获取沙盒执行结果")
    public ResponseEntity<?> getSandboxResult(@PathVariable String projectId) {
        GameSandboxService.ExecutionResult result = sandboxService.getResult(projectId);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    // ===== 项目导入 =====

    /**
     * 从 ZIP 文件导入项目
     */
    @PostMapping("/import/zip")
    @Operation(summary = "从 ZIP 导入项目")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> importFromZip(@RequestParam("file") MultipartFile file,
                                           @RequestParam("name") String name,
                                           @RequestParam(value = "description", required = false) String description) {
        try {
            // 保存上传的文件到临时目录
            Path tempFile = Files.createTempFile("import-", ".zip");
            file.transferTo(tempFile.toFile());

            ProjectImportService.ImportResult result = importService.importFromZip(
                tempFile.toString(), name, description);

            // 清理临时文件
            Files.deleteIfExists(tempFile);

            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (IOException e) {
            log.error("导入失败", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "导入失败: " + e.getMessage()));
        }
    }

    /**
     * 从本地目录导入项目
     */
    @PostMapping("/import/directory")
    @Operation(summary = "从目录导入项目")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> importFromDirectory(@RequestBody Map<String, String> data) {
        ProjectImportService.ImportResult result = importService.importFromDirectory(
            data.get("sourcePath"), data.get("name"), data.get("description"));

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 分析项目结构
     */
    @PostMapping("/analyze")
    @Operation(summary = "分析项目结构")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> analyzeProject(@RequestBody Map<String, String> data) {
        ProjectImportService.ProjectAnalysis analysis = importService.analyzeProject(data.get("projectPath"));
        return ResponseEntity.ok(analysis);
    }
}
