package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.entity.DismissalRequest;
import com.chengxun.gamemaker.web.entity.PerformanceReview;
import com.chengxun.gamemaker.web.entity.ProducerReplacement;
import com.chengxun.gamemaker.service.PerformanceManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 绩效管理控制器
 * 提供绩效打分、警告、解雇申请的API
 *
 * @author chengxun
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/performance-management")
public class PerformanceManagementController {

    private static final Logger log = LoggerFactory.getLogger(PerformanceManagementController.class);

    @Autowired
    private PerformanceManagementService performanceService;

    // ===== 绩效打分 =====

    /**
     * 制作人提交绩效评审
     */
    @PostMapping("/reviews/submit")
    @PreAuthorize("hasAuthority('PERM_agents:manage')")
    public ResponseEntity<Map<String, Object>> submitReview(@RequestBody Map<String, Object> request) {
        try {
            String producerId = (String) request.get("producerId");
            String agentId = (String) request.get("agentId");
            String projectId = (String) request.get("projectId");
            String reviewPeriod = (String) request.get("reviewPeriod");
            Integer qualityScore = (Integer) request.get("qualityScore");
            Integer efficiencyScore = (Integer) request.get("efficiencyScore");
            Integer collaborationScore = (Integer) request.get("collaborationScore");
            Integer innovationScore = (Integer) request.get("innovationScore");
            String strengths = (String) request.get("strengths");
            String improvements = (String) request.get("improvements");
            String comments = (String) request.get("comments");
            String highlights = (String) request.get("highlights");

            PerformanceReview review = performanceService.submitReview(
                producerId, agentId, projectId, reviewPeriod,
                qualityScore, efficiencyScore, collaborationScore, innovationScore,
                strengths, improvements, comments, highlights
            );

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("reviewId", review.getId());
            result.put("reviewNo", review.getReviewNo());
            result.put("overallScore", review.getOverallScore());
            result.put("grade", review.getGrade());
            result.put("isWarning", review.getIsWarning());
            result.put("message", "绩效评审已提交");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Submit review failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 制作人发出警告
     */
    @PostMapping("/warnings/issue")
    @PreAuthorize("hasAuthority('PERM_agents:manage')")
    public ResponseEntity<Map<String, String>> issueWarning(@RequestBody Map<String, String> request) {
        try {
            String producerId = request.get("producerId");
            String agentId = request.get("agentId");
            String reason = request.get("reason");

            performanceService.issueWarning(producerId, agentId, reason);

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "警告已发出"
            ));
        } catch (Exception e) {
            log.error("Issue warning failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    // ===== 解雇申请 =====

    /**
     * 制作人发起解雇申请
     */
    @PostMapping("/dismissals/submit")
    @PreAuthorize("hasAuthority('PERM_agents:manage')")
    public ResponseEntity<Map<String, Object>> submitDismissalRequest(@RequestBody Map<String, String> request) {
        try {
            String producerId = request.get("producerId");
            String agentId = request.get("agentId");
            String reasonType = request.get("reasonType");
            String reason = request.get("reason");

            DismissalRequest dismissalRequest = performanceService.submitDismissalRequest(
                producerId, agentId, reasonType, reason
            );

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("requestId", dismissalRequest.getId());
            result.put("requestNo", dismissalRequest.getRequestNo());
            result.put("message", "解雇申请已提交，等待管理员审批");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Submit dismissal request failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 管理员批准解雇
     */
    @PostMapping("/dismissals/{requestId}/approve")
    @PreAuthorize("hasAuthority('PERM_admin:manage')")
    public ResponseEntity<Map<String, Object>> approveDismissal(
            @PathVariable Long requestId,
            @RequestParam String comment,
            Authentication authentication) {
        try {
            Long adminId = 1L;
            String adminName = authentication.getName();

            DismissalRequest request = performanceService.approveDismissal(requestId, adminId, adminName, comment);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("requestNo", request.getRequestNo());
            result.put("message", "解雇申请已批准");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Approve dismissal failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 管理员驳回解雇
     */
    @PostMapping("/dismissals/{requestId}/reject")
    @PreAuthorize("hasAuthority('PERM_admin:manage')")
    public ResponseEntity<Map<String, Object>> rejectDismissal(
            @PathVariable Long requestId,
            @RequestParam String reason,
            Authentication authentication) {
        try {
            Long adminId = 1L;
            String adminName = authentication.getName();

            DismissalRequest request = performanceService.rejectDismissal(requestId, adminId, adminName, reason);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("requestNo", request.getRequestNo());
            result.put("message", "解雇申请已驳回");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Reject dismissal failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 执行解雇（管理员批准后）
     */
    @PostMapping("/dismissals/{requestId}/execute")
    @PreAuthorize("hasAuthority('PERM_admin:manage')")
    public ResponseEntity<Map<String, Object>> executeDismissal(
            @PathVariable Long requestId,
            Authentication authentication) {
        try {
            String executedBy = authentication.getName();

            performanceService.executeDismissal(requestId, executedBy);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "解雇已执行");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Execute dismissal failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    // ===== 查询接口 =====

    /**
     * 获取Agent的绩效评审历史
     */
    @GetMapping("/reviews/agent/{agentId}")
    @PreAuthorize("hasAuthority('PERM_agents:view')")
    public ResponseEntity<List<PerformanceReview>> getAgentReviews(@PathVariable String agentId) {
        return ResponseEntity.ok(performanceService.getAgentReviews(agentId));
    }

    /**
     * 获取制作人的评审记录
     */
    @GetMapping("/reviews/producer/{producerId}")
    @PreAuthorize("hasAuthority('PERM_agents:view')")
    public ResponseEntity<List<PerformanceReview>> getProducerReviews(@PathVariable String producerId) {
        return ResponseEntity.ok(performanceService.getProducerReviews(producerId));
    }

    /**
     * 获取所有评审记录
     */
    @GetMapping("/reviews/producer/all")
    @PreAuthorize("hasAuthority('PERM_agents:view')")
    public ResponseEntity<List<PerformanceReview>> getAllReviews() {
        return ResponseEntity.ok(performanceService.getAllReviews());
    }

    /**
     * 获取待审批的解雇申请
     */
    @GetMapping("/dismissals/pending")
    @PreAuthorize("hasAuthority('PERM_admin:manage')")
    public ResponseEntity<List<DismissalRequest>> getPendingDismissals() {
        return ResponseEntity.ok(performanceService.getPendingDismissalRequests());
    }

    /**
     * 获取所有解雇申请
     */
    @GetMapping("/dismissals/all")
    @PreAuthorize("hasAuthority('PERM_admin:manage')")
    public ResponseEntity<List<DismissalRequest>> getAllDismissals() {
        return ResponseEntity.ok(performanceService.getAllDismissalRequests());
    }

    /**
     * 获取解雇申请详情
     */
    @GetMapping("/dismissals/{requestId}")
    @PreAuthorize("hasAuthority('PERM_agents:view')")
    public ResponseEntity<DismissalRequest> getDismissalRequest(@PathVariable Long requestId) {
        DismissalRequest request = performanceService.getDismissalRequest(requestId);
        if (request == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(request);
    }

    /**
     * 获取绩效统计
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('PERM_agents:view')")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(performanceService.getPerformanceStatistics());
    }

    /**
     * 获取Agent的警告次数
     */
    @GetMapping("/warnings/count/{agentId}")
    @PreAuthorize("hasAuthority('PERM_agents:view')")
    public ResponseEntity<Map<String, Object>> getWarningCount(@PathVariable String agentId) {
        Long count = performanceService.getWarningCount(agentId);
        return ResponseEntity.ok(Map.of(
            "agentId", agentId,
            "warningCount", count
        ));
    }

    // ===== 管理员解雇制作人（更换制作人） =====

    /**
     * 管理员解雇制作人（更换制作人）
     */
    @PostMapping("/producer/replace")
    @PreAuthorize("hasAuthority('PERM_admin:manage')")
    public ResponseEntity<Map<String, Object>> replaceProducer(@RequestBody Map<String, String> request,
                                                                Authentication authentication) {
        try {
            Long adminId = 1L; // 简化处理
            String adminName = authentication.getName();
            String producerId = request.get("producerId");
            String projectId = request.get("projectId");
            String reasonType = request.get("reasonType");
            String reason = request.get("reason");
            String newGuidelines = request.get("newGuidelines");

            ProducerReplacement replacement = performanceService.replaceProducer(
                adminId, adminName, producerId, projectId, reasonType, reason, newGuidelines
            );

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("replacementNo", replacement.getReplacementNo());
            result.put("oldProducerId", replacement.getOldProducerId());
            result.put("newProducerId", replacement.getNewProducerId());
            result.put("message", "制作人已更换，新制作人已创建");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Replace producer failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取制作人更换历史
     */
    @GetMapping("/producer/replacements/{projectId}")
    @PreAuthorize("hasAuthority('PERM_agents:view')")
    public ResponseEntity<List<ProducerReplacement>> getProducerReplacements(@PathVariable String projectId) {
        return ResponseEntity.ok(performanceService.getProducerReplacementHistory(projectId));
    }

    /**
     * 获取所有制作人更换记录
     */
    @GetMapping("/producer/replacements")
    @PreAuthorize("hasAuthority('PERM_admin:manage')")
    public ResponseEntity<List<ProducerReplacement>> getAllProducerReplacements() {
        return ResponseEntity.ok(performanceService.getAllProducerReplacements());
    }
}
