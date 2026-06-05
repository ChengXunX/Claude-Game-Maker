package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.web.entity.RecruitmentRequest;
import com.chengxun.gamemaker.service.RecruitmentApprovalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 招聘审批控制器
 * 提供招聘申请和审批的API
 *
 * @author chengxun
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/recruitment-approval")
public class RecruitmentApprovalController {

    private static final Logger log = LoggerFactory.getLogger(RecruitmentApprovalController.class);

    @Autowired
    private RecruitmentApprovalService approvalService;

    @Autowired
    private com.chengxun.gamemaker.web.service.UserService userService;

    /**
     * 制作人提交招聘申请
     */
    @PostMapping("/submit")
    @PreAuthorize("hasAuthority('PERM_agent:manage')")
    public ResponseEntity<Map<String, Object>> submitRequest(@RequestBody Map<String, String> request) {
        try {
            String producerId = request.get("producerId");
            String role = request.get("role");
            String employeeName = request.get("employeeName");
            String description = request.get("description");
            String capabilities = request.get("capabilities");
            String workDir = request.get("workDir");
            String reason = request.get("reason");

            RecruitmentRequest recruitmentRequest = approvalService.submitRequest(
                producerId, role, employeeName, description, capabilities, workDir, reason);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("requestId", recruitmentRequest.getId());
            result.put("requestNo", recruitmentRequest.getRequestNo());
            result.put("message", "招聘申请已提交，等待管理员审批");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Submit request failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 制作人提交完整招聘申请
     */
    @PostMapping("/submit-full")
    @PreAuthorize("hasAuthority('PERM_agent:manage')")
    public ResponseEntity<Map<String, Object>> submitFullRequest(@RequestBody Map<String, Object> request) {
        try {
            String producerId = (String) request.get("producerId");
            String role = (String) request.get("role");
            String roleNameDisplay = (String) request.get("roleNameDisplay");
            String employeeName = (String) request.get("employeeName");
            String description = (String) request.get("description");
            String capabilities = (String) request.get("capabilities");
            String supportedFileTypes = (String) request.get("supportedFileTypes");
            String workDir = (String) request.get("workDir");
            String reason = (String) request.get("reason");

            RecruitmentRequest recruitmentRequest = approvalService.submitFullRequest(
                producerId, role, roleNameDisplay, employeeName, description,
                capabilities, supportedFileTypes, workDir, reason);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("requestId", recruitmentRequest.getId());
            result.put("requestNo", recruitmentRequest.getRequestNo());
            result.put("message", "招聘申请已提交，等待管理员审批");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Submit full request failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 管理员批准申请
     */
    @PostMapping("/{requestId}/approve")
    @PreAuthorize("hasAuthority('PERM_admin:manage')")
    public ResponseEntity<Map<String, Object>> approveRequest(
            @PathVariable Long requestId,
            @RequestParam String comment,
            Authentication authentication) {
        try {
            com.chengxun.gamemaker.web.entity.User adminUser = userService.getUserByUsername(authentication.getName());
            Long adminId = adminUser != null ? adminUser.getId() : 0L;
            String adminName = authentication.getName();

            RecruitmentRequest request = approvalService.approveRequest(requestId, adminId, adminName, comment);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("requestNo", request.getRequestNo());
            result.put("message", "申请已批准，通知制作人执行招聘");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Approve request failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 管理员驳回申请
     */
    @PostMapping("/{requestId}/reject")
    @PreAuthorize("hasAuthority('PERM_admin:manage')")
    public ResponseEntity<Map<String, Object>> rejectRequest(
            @PathVariable Long requestId,
            @RequestParam String reason,
            Authentication authentication) {
        try {
            com.chengxun.gamemaker.web.entity.User adminUser = userService.getUserByUsername(authentication.getName());
            Long adminId = adminUser != null ? adminUser.getId() : 0L;
            String adminName = authentication.getName();

            RecruitmentRequest request = approvalService.rejectRequest(requestId, adminId, adminName, reason);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("requestNo", request.getRequestNo());
            result.put("rejectionReason", request.getRejectionReason());
            result.put("message", "申请已驳回，通知制作人");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Reject request failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 制作人执行招聘（申请批准后）
     */
    @PostMapping("/{requestId}/execute")
    @PreAuthorize("hasAuthority('PERM_agent:manage')")
    public ResponseEntity<Map<String, Object>> executeRecruitment(
            @PathVariable Long requestId,
            @RequestParam String producerId) {
        try {
            Agent agent = approvalService.executeRecruitment(requestId, producerId);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("agentId", agent.getId());
            result.put("agentName", agent.getName());
            result.put("role", agent.getRole());
            result.put("message", "招聘执行成功");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Execute recruitment failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 制作人取消申请
     */
    @PostMapping("/{requestId}/cancel")
    @PreAuthorize("hasAuthority('PERM_agent:manage')")
    public ResponseEntity<Map<String, Object>> cancelRequest(
            @PathVariable Long requestId,
            @RequestParam String producerId) {
        try {
            RecruitmentRequest request = approvalService.cancelRequest(requestId, producerId);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("requestNo", request.getRequestNo());
            result.put("message", "申请已取消");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Cancel request failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 制作人重新申请（被驳回后）
     */
    @PostMapping("/{requestId}/revise")
    @PreAuthorize("hasAuthority('PERM_agent:manage')")
    public ResponseEntity<Map<String, Object>> reviseRequest(
            @PathVariable Long requestId,
            @RequestParam String producerId,
            @RequestParam String newReason) {
        try {
            RecruitmentRequest newRequest = approvalService.reviseRequest(requestId, producerId, newReason);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("newRequestId", newRequest.getId());
            result.put("newRequestNo", newRequest.getRequestNo());
            result.put("message", "重新申请已提交，等待管理员审批");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Revise request failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取申请详情
     */
    @GetMapping("/{requestId}")
    @PreAuthorize("hasAuthority('PERM_agent:view')")
    public ResponseEntity<RecruitmentRequest> getRequest(@PathVariable Long requestId) {
        RecruitmentRequest request = approvalService.getRequest(requestId);
        if (request == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(request);
    }

    /**
     * 获取待审批的申请列表
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('PERM_admin:manage')")
    public ResponseEntity<List<RecruitmentRequest>> getPendingRequests() {
        return ResponseEntity.ok(approvalService.getPendingRequests());
    }

    /**
     * 获取制作人的申请列表
     */
    @GetMapping("/producer/{producerId}")
    @PreAuthorize("hasAuthority('PERM_agent:view')")
    public ResponseEntity<List<RecruitmentRequest>> getProducerRequests(@PathVariable String producerId) {
        return ResponseEntity.ok(approvalService.getProducerRequests(producerId));
    }

    /**
     * 获取所有申请
     */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('PERM_admin:manage')")
    public ResponseEntity<List<RecruitmentRequest>> getAllRequests() {
        return ResponseEntity.ok(approvalService.getAllRequests());
    }

    /**
     * 获取申请统计
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('PERM_agent:view')")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(approvalService.getRequestStatistics());
    }
}
