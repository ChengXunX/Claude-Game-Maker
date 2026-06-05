package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.service.CodeQualityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 代码质量检查控制器
 * 提供代码质量检查的REST API
 *
 * 主要功能：
 * - 代码风格检查
 * - 安全漏洞扫描
 * - 代码复杂度分析
 *
 * @author chengxun
 * @since 1.0.0
 */
@Tag(name = "代码质量", description = "代码质量检查API")
@RestController
@RequestMapping("/api/code-quality")
public class CodeQualityController {

    @Autowired
    private CodeQualityService codeQualityService;

    /**
     * 综合代码质量检查
     */
    @Operation(summary = "综合检查", description = "对代码进行综合质量检查")
    @PostMapping("/check")
    @PreAuthorize("hasAuthority('PERM_code:review')")
    public ResponseEntity<Map<String, Object>> checkCode(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        String language = request.getOrDefault("language", "java");

        if (code == null || code.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "代码内容不能为空"
            ));
        }

        Map<String, Object> results = codeQualityService.comprehensiveCheck(code, language);

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "results", results
        ));
    }

    /**
     * 代码风格检查
     */
    @Operation(summary = "风格检查", description = "检查代码风格")
    @PostMapping("/style")
    @PreAuthorize("hasAuthority('PERM_code:review')")
    public ResponseEntity<Map<String, Object>> checkStyle(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        String language = request.getOrDefault("language", "java");

        CodeQualityService.QualityCheckResult result = codeQualityService.checkCodeStyle(code, language);

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "result", result
        ));
    }

    /**
     * 安全漏洞检查
     */
    @Operation(summary = "安全检查", description = "检查安全漏洞")
    @PostMapping("/security")
    @PreAuthorize("hasAuthority('PERM_code:review')")
    public ResponseEntity<Map<String, Object>> checkSecurity(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        String language = request.getOrDefault("language", "java");

        CodeQualityService.QualityCheckResult result = codeQualityService.checkSecurityVulnerabilities(code, language);

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "result", result
        ));
    }

    /**
     * 代码复杂度检查
     */
    @Operation(summary = "复杂度检查", description = "检查代码复杂度")
    @PostMapping("/complexity")
    @PreAuthorize("hasAuthority('PERM_code:review')")
    public ResponseEntity<Map<String, Object>> checkComplexity(@RequestBody Map<String, String> request) {
        String code = request.get("code");

        CodeQualityService.QualityCheckResult result = codeQualityService.checkComplexity(code);

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "result", result
        ));
    }

    /**
     * 获取项目的代码质量报告
     */
    @Operation(summary = "获取质量报告", description = "获取项目的代码质量检查报告")
    @GetMapping("/{projectId}")
    @PreAuthorize("hasAuthority('PERM_code:review')")
    public ResponseEntity<Map<String, Object>> getReport(@PathVariable String projectId) {
        Map<String, Object> report = codeQualityService.getProjectReport(projectId);
        return ResponseEntity.ok(report);
    }

    /**
     * 获取项目的代码质量问题列表
     */
    @Operation(summary = "获取问题列表", description = "获取项目的代码质量问题")
    @GetMapping("/{projectId}/issues")
    @PreAuthorize("hasAuthority('PERM_code:review')")
    public ResponseEntity<List<Map<String, Object>>> getIssues(@PathVariable String projectId,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "50") int size) {
        List<Map<String, Object>> issues = codeQualityService.getProjectIssues(projectId, page, size);
        return ResponseEntity.ok(issues);
    }

    /**
     * 获取项目的代码质量趋势
     */
    @Operation(summary = "获取质量趋势", description = "获取项目的代码质量变化趋势")
    @GetMapping("/{projectId}/trend")
    @PreAuthorize("hasAuthority('PERM_code:review')")
    public ResponseEntity<List<Map<String, Object>>> getTrend(@PathVariable String projectId) {
        List<Map<String, Object>> trend = codeQualityService.getProjectTrend(projectId);
        return ResponseEntity.ok(trend);
    }
}
