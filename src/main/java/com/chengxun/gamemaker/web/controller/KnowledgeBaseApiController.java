package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.service.GameKnowledgeBase;
import com.chengxun.gamemaker.service.GameKnowledgeBase.BestPractice;
import com.chengxun.gamemaker.service.GameKnowledgeBase.Solution;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 知识库 API 控制器
 * 提供游戏开发知识库的查询接口
 *
 * @author chengxun
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/knowledge-base")
@Tag(name = "知识库", description = "游戏开发知识库接口")
public class KnowledgeBaseApiController {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseApiController.class);

    private final GameKnowledgeBase knowledgeBase;

    public KnowledgeBaseApiController(GameKnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    /**
     * 获取知识库统计
     */
    @GetMapping("/stats")
    @Operation(summary = "获取知识库统计")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(knowledgeBase.getKnowledgeBaseStats());
    }

    /**
     * 获取模板使用统计
     */
    @GetMapping("/template-stats/{templateId}")
    @Operation(summary = "获取模板使用统计")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getTemplateStats(@PathVariable String templateId) {
        return ResponseEntity.ok(knowledgeBase.getTemplateStats(templateId));
    }

    /**
     * 获取使用记录列表
     * 返回所有类型的使用记录（模板、解决方案、最佳实践、知识提取、进化等）
     */
    @GetMapping("/usage-records")
    @Operation(summary = "获取使用记录列表")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<GameKnowledgeBase.UsageRecord>> getUsageRecords() {
        return ResponseEntity.ok(knowledgeBase.getAllGeneralUsageRecords());
    }

    /**
     * 获取解决方案列表
     */
    @GetMapping("/solutions")
    @Operation(summary = "获取解决方案列表")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<GameKnowledgeBase.Solution>> getSolutionsList() {
        return ResponseEntity.ok(knowledgeBase.getAllSolutionsList());
    }

    /**
     * 获取问题解决方案
     */
    @GetMapping("/solutions/{problemType}")
    @Operation(summary = "获取问题解决方案")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Solution>> getSolutions(@PathVariable String problemType) {
        return ResponseEntity.ok(knowledgeBase.getSolutions(problemType));
    }

    /**
     * 获取最佳实践
     */
    @GetMapping("/best-practices")
    @Operation(summary = "获取最佳实践")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BestPractice>> getBestPractices(
            @RequestParam(required = false) String category) {
        if (category != null && !category.isEmpty()) {
            return ResponseEntity.ok(knowledgeBase.getBestPractices(category));
        }
        return ResponseEntity.ok(knowledgeBase.getAllBestPractices());
    }

    /**
     * 记录模板使用
     */
    @PostMapping("/record-usage")
    @Operation(summary = "记录模板使用")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> recordUsage(@RequestBody Map<String, Object> data) {
        String templateId = (String) data.get("templateId");
        String gameDescription = (String) data.get("gameDescription");
        Boolean success = (Boolean) data.get("success");
        Long durationMs = ((Number) data.get("durationMs")).longValue();

        knowledgeBase.recordTemplateUsage(templateId, gameDescription, success, durationMs);
        return ResponseEntity.ok().build();
    }

    /**
     * 记录问题解决方案
     */
    @PostMapping("/record-solution")
    @Operation(summary = "记录问题解决方案")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> recordSolution(@RequestBody Map<String, String> data) {
        knowledgeBase.recordSolution(
            data.get("problemType"),
            data.get("problemDescription"),
            data.get("solution")
        );
        return ResponseEntity.ok().build();
    }

    /**
     * 记录最佳实践
     */
    @PostMapping("/record-best-practice")
    @Operation(summary = "记录最佳实践")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> recordBestPractice(@RequestBody Map<String, String> data) {
        knowledgeBase.recordBestPractice(
            data.get("category"),
            data.get("title"),
            data.get("content")
        );
        return ResponseEntity.ok().build();
    }
}
