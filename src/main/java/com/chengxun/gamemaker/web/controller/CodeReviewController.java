package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.entity.CodeReview;
import com.chengxun.gamemaker.web.service.CodeReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 代码审查控制器
 * 提供代码审查的管理接口
 *
 * @author chengxun
 * @since 1.0.0
 */
@Controller
@RequestMapping({"/reviews", "/api/reviews"})
public class CodeReviewController {

    private static final Logger log = LoggerFactory.getLogger(CodeReviewController.class);

    @Autowired
    private CodeReviewService reviewService;

    // ===== 页面路由 =====

    /**
     * 代码审查主页
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_code:review')")
    public String reviewDashboard(Model model) {
        log.info("Loading code review dashboard");

        // 获取统计信息
        Map<String, Object> statistics = reviewService.getReviewStatistics();
        model.addAttribute("statistics", statistics);

        // 获取待审查记录
        List<CodeReview> pendingReviews = reviewService.getPendingReviews();
        model.addAttribute("pendingReviews", pendingReviews);

        // 获取最近审查记录
        Page<CodeReview> recentReviews = reviewService.getAllReviews(0, 20);
        model.addAttribute("recentReviews", recentReviews.getContent());

        return "reviews/dashboard";
    }

    /**
     * 审查详情页
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_code:review')")
    public String reviewDetail(@PathVariable Long id, Model model) {
        CodeReview review = reviewService.getReview(id);
        model.addAttribute("review", review);
        return "reviews/detail";
    }

    // ===== API接口 =====

    /**
     * 获取审查统计（JSON）
     */
    @GetMapping("/api/statistics")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_code:review')")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(reviewService.getReviewStatistics());
    }

    /**
     * 获取所有审查记录（JSON）
     */
    @GetMapping("/api/all")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_code:review')")
    public ResponseEntity<Page<CodeReview>> getAllReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reviewService.getAllReviews(page, size));
    }

    /**
     * 获取待审查记录（JSON）
     */
    @GetMapping("/api/pending")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_code:review')")
    public ResponseEntity<List<CodeReview>> getPendingReviews() {
        return ResponseEntity.ok(reviewService.getPendingReviews());
    }

    /**
     * 获取Agent审查记录（JSON）
     */
    @GetMapping("/api/agent/{agentId}")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_code:review')")
    public ResponseEntity<List<CodeReview>> getAgentReviews(@PathVariable String agentId) {
        return ResponseEntity.ok(reviewService.getAgentReviews(agentId));
    }

    /**
     * 提交代码审查（JSON）
     */
    @PostMapping("/api/submit")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_code:review')")
    public ResponseEntity<CodeReview> submitReview(@RequestBody Map<String, String> request) {
        CodeReview review = reviewService.submitReview(
            request.get("agentId"),
            request.get("agentName"),
            request.get("projectId"),
            request.get("title"),
            request.get("description"),
            request.get("branch"),
            request.get("commitHash"),
            request.get("changedFiles"),
            request.get("diffContent")
        );
        return ResponseEntity.ok(review);
    }

    /**
     * 执行人工审查（JSON）
     */
    @PostMapping("/api/{id}/review")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_code:review')")
    public ResponseEntity<Map<String, String>> performReview(
            @PathVariable Long id,
            @RequestParam String reviewer,
            @RequestParam boolean approved,
            @RequestParam(required = false) Integer score,
            @RequestParam(required = false) String comment) {
        reviewService.performManualReview(id, reviewer, approved, score, comment);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Review completed"));
    }

    /**
     * 删除审查记录（JSON）
     */
    @DeleteMapping("/api/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_code:review')")
    public ResponseEntity<Map<String, String>> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Review deleted"));
    }
}
