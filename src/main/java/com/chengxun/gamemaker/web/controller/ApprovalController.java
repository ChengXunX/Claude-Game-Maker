package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.entity.ApprovalRequest;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.ApprovalService;
import com.chengxun.gamemaker.web.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 审批管理 API 控制器
 *
 * @author chengxun
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/approvals")
@Tag(name = "审批管理", description = "审批请求相关接口")
public class ApprovalController {

    private static final Logger log = LoggerFactory.getLogger(ApprovalController.class);

    private final ApprovalService approvalService;
    private final UserService userService;

    public ApprovalController(ApprovalService approvalService, UserService userService) {
        this.approvalService = approvalService;
        this.userService = userService;
    }

    /**
     * 获取所有待审批请求
     */
    @GetMapping("/pending")
    @Operation(summary = "获取待审批请求")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERM_approval:manage')")
    public ResponseEntity<List<ApprovalRequest>> getPendingRequests() {
        return ResponseEntity.ok(approvalService.getAllPendingRequests());
    }

    /**
     * 获取项目的待审批请求
     */
    @GetMapping("/project/{projectId}")
    @Operation(summary = "获取项目的待审批请求")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERM_approval:manage')")
    public ResponseEntity<List<ApprovalRequest>> getProjectPendingRequests(@PathVariable String projectId) {
        return ResponseEntity.ok(approvalService.getPendingRequests(projectId));
    }

    /**
     * 审批请求
     */
    @PostMapping("/{requestId}/approve")
    @Operation(summary = "审批请求")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERM_approval:manage')")
    public ResponseEntity<Map<String, Object>> approveRequest(
            @PathVariable Long requestId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {

        User user = userService.getUserByUsername(authentication.getName());
        boolean approved = (Boolean) request.getOrDefault("approved", false);
        String comment = (String) request.getOrDefault("comment", "");

        try {
            ApprovalRequest approvalRequest = approvalService.approve(
                requestId,
                user.getId().toString(),
                user.getNickname() != null ? user.getNickname() : user.getUsername(),
                approved,
                comment
            );

            String action = approved ? "批准" : "拒绝";
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "请求已" + action,
                "status", approvalRequest.getStatus().name()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取待审批请求数量
     */
    @GetMapping("/count")
    @Operation(summary = "获取待审批请求数量")
    public ResponseEntity<Map<String, Long>> getPendingCount() {
        return ResponseEntity.ok(Map.of("count", approvalService.getPendingCount()));
    }

    /**
     * 获取项目的待审批请求数量
     */
    @GetMapping("/count/project/{projectId}")
    @Operation(summary = "获取项目的待审批请求数量")
    public ResponseEntity<Map<String, Long>> getProjectPendingCount(@PathVariable String projectId) {
        return ResponseEntity.ok(Map.of("count", approvalService.getPendingCountByProject(projectId)));
    }
}
