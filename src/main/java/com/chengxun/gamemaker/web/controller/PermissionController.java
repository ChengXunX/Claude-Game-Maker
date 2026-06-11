package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.dto.ErrorResponse;
import com.chengxun.gamemaker.web.entity.PermissionDefinition;
import com.chengxun.gamemaker.web.entity.PermissionRequest;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.entity.UserPermission;
import com.chengxun.gamemaker.web.service.PermissionService;
import com.chengxun.gamemaker.web.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 权限管理控制器
 * 提供权限申请、审批和管理功能
 *
 * @author chengxun
 * @since 2.0.0
 */
@Controller
@RequestMapping({"/permissions", "/api/permissions"})
public class PermissionController {

    private static final Logger log = LoggerFactory.getLogger(PermissionController.class);

    private final PermissionService permissionService;
    private final UserService userService;

    public PermissionController(PermissionService permissionService, UserService userService) {
        this.permissionService = permissionService;
        this.userService = userService;
    }

    // ===== 页面 =====

    /**
     * 权限申请页面（普通用户）
     */
    @GetMapping("/request")
    @PreAuthorize("isAuthenticated()")
    public String requestPage(Model model, Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        model.addAttribute("username", authentication.getName());
        model.addAttribute("userId", user != null ? user.getId() : null);
        model.addAttribute("availablePermissions", permissionService.getAvailablePermissions());
        model.addAttribute("userPermissions", permissionService.getUserExtraPermissionIds(user != null ? user.getId() : 0L));
        model.addAttribute("pendingRequests", user != null ?
            permissionService.getUserRequests(user.getId(), PageRequest.of(0, 50)) : Page.empty());
        return "permissions/request";
    }

    /**
     * 权限审批页面（管理员）
     */
    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('PERM_admin:manage')")
    public String adminPage(Model model, Authentication authentication) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("pendingRequests", permissionService.getPendingRequests());
        model.addAttribute("pendingCount", permissionService.getPendingCount());
        model.addAttribute("availablePermissions", permissionService.getAvailablePermissions());
        return "permissions/admin";
    }

    /**
     * 用户权限管理页面（管理员）
     */
    @GetMapping("/admin/users")
    @PreAuthorize("hasAuthority('PERM_admin:manage')")
    public String userPermissionsPage(Model model, Authentication authentication) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("availablePermissions", permissionService.getAvailablePermissions());
        return "permissions/users";
    }

    /**
     * 获取用户列表（API，用于前端）
     */
    @GetMapping("/api/admin/users")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_admin:manage')")
    public ResponseEntity<?> getUsersList() {
        try {
            var users = userService.getAllUsers();
            List<Map<String, Object>> list = new ArrayList<>();
            for (var user : users) {
                Map<String, Object> info = new HashMap<>();
                info.put("id", user.getId());
                info.put("username", user.getUsername());
                info.put("nickname", user.getNickname());
                info.put("email", user.getEmail());
                info.put("status", user.getStatus());
                if (user.getRole() != null) {
                    Map<String, Object> roleInfo = new HashMap<>();
                    roleInfo.put("id", user.getRole().getId());
                    roleInfo.put("name", user.getRole().getName());
                    roleInfo.put("displayName", user.getRole().getDisplayName());
                    info.put("role", roleInfo);
                }
                info.put("extraPermissions", permissionService.getUserExtraPermissionIds(user.getId()));
                list.add(info);
            }
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ===== API =====

    /**
     * 获取可申请的权限列表
     */
    @GetMapping("/api/available")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public Map<String, String> getAvailablePermissions() {
        return permissionService.getAvailablePermissions();
    }

    /**
     * 提交权限申请
     */
    @PostMapping("/api/request")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> submitRequest(@RequestBody Map<String, String> body, Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ErrorResponse.badRequest("用户不存在"));
            }

            String permission = body.get("permission");
            String reason = body.get("reason");

            if (permission == null || permission.isEmpty()) {
                return ResponseEntity.badRequest().body(ErrorResponse.badRequest("请选择要申请的权限"));
            }

            PermissionRequest request = permissionService.submitRequest(
                user.getId(), user.getUsername(), permission, reason);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "权限申请已提交，等待管理员审批",
                "requestId", request.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ErrorResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 取消申请
     */
    @PostMapping("/api/request/{id}/cancel")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> cancelRequest(@PathVariable Long id, Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ErrorResponse.badRequest("用户不存在"));
            }

            permissionService.cancelRequest(id, user.getId());
            return ResponseEntity.ok(Map.of("success", true, "message", "申请已取消"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ErrorResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 审批通过
     */
    @PostMapping("/api/approve/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_admin:manage')")
    public ResponseEntity<?> approve(@PathVariable Long id,
                                      @RequestBody(required = false) Map<String, String> body,
                                      Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            String comment = body != null ? body.get("comment") : null;

            PermissionRequest request = permissionService.approve(
                id, user.getId(), user.getUsername(), comment);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "已批准 " + request.getUsername() + " 的 " + request.getPermission() + " 权限申请"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ErrorResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 审批拒绝
     */
    @PostMapping("/api/reject/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_admin:manage')")
    public ResponseEntity<?> reject(@PathVariable Long id,
                                     @RequestBody Map<String, String> body,
                                     Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            String comment = body.get("comment");

            if (comment == null || comment.isEmpty()) {
                return ResponseEntity.badRequest().body(ErrorResponse.badRequest("请填写拒绝原因"));
            }

            PermissionRequest request = permissionService.reject(
                id, user.getId(), user.getUsername(), comment);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "已拒绝 " + request.getUsername() + " 的 " + request.getPermission() + " 权限申请"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ErrorResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 管理员直接授予权限
     */
    @PostMapping("/api/grant")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_admin:manage')")
    public ResponseEntity<?> grantPermission(@RequestBody Map<String, Object> body,
                                              Authentication authentication) {
        try {
            Object userIdObj = body.get("userId");
            if (userIdObj == null) {
                return ResponseEntity.badRequest().body(ErrorResponse.badRequest("userId 不能为空"));
            }
            Long userId = Long.valueOf(userIdObj.toString());

            String permission = (String) body.get("permission");
            if (permission == null || permission.isBlank()) {
                return ResponseEntity.badRequest().body(ErrorResponse.badRequest("权限标识不能为空"));
            }

            String reason = (String) body.get("reason");
            User admin = userService.getUserByUsername(authentication.getName());

            UserPermission perm = permissionService.grantPermission(
                userId, permission, admin.getUsername(), reason);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "权限已授予"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ErrorResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 管理员撤销权限
     */
    @PostMapping("/api/revoke")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_admin:manage')")
    public ResponseEntity<?> revokePermission(@RequestBody Map<String, Object> body) {
        try {
            Object userIdObj = body.get("userId");
            if (userIdObj == null) {
                return ResponseEntity.badRequest().body(ErrorResponse.badRequest("userId 不能为空"));
            }
            Long userId = Long.valueOf(userIdObj.toString());

            String permission = (String) body.get("permission");
            if (permission == null || permission.isBlank()) {
                return ResponseEntity.badRequest().body(ErrorResponse.badRequest("权限标识不能为空"));
            }

            permissionService.revokePermission(userId, permission);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "权限已撤销"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ErrorResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 获取用户的权限申请列表
     * 普通用户可以查看自己的申请，管理员可以查看所有用户的申请
     */
    @GetMapping("/api/user/{userId}")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserPermissionRequests(@PathVariable Long userId,
                                                        Authentication authentication) {
        try {
            User currentUser = userService.getUserByUsername(authentication.getName());
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
            }

            // 普通用户只能查看自己的申请
            if (!currentUser.isAdmin() && !currentUser.getId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "无权查看其他用户的权限申请"));
            }

            var requests = permissionService.getUserRequests(userId, PageRequest.of(0, 100));
            return ResponseEntity.ok(requests.getContent());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取待审批数量（供导航栏显示）
     */
    @GetMapping("/api/pending-count")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_admin:manage')")
    public Map<String, Object> getPendingCount() {
        return Map.of("count", permissionService.getPendingCount());
    }

    /**
     * 获取待审批的权限申请列表
     */
    @GetMapping("/api/pending")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_admin:manage')")
    public List<PermissionRequest> getPendingRequests() {
        return permissionService.getPendingRequests();
    }

    // ===== 权限定义管理 =====

    /**
     * 权限定义管理页面
     */
    @GetMapping("/admin/definitions")
    @PreAuthorize("hasAuthority('PERM_admin:manage')")
    public String definitionsPage(Model model, Authentication authentication) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("definitions", permissionService.getAllDefinitions());
        model.addAttribute("categories", permissionService.getCategories());
        return "permissions/definitions";
    }

    /**
     * 获取所有权限定义
     */
    @GetMapping("/api/definitions")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_admin:manage')")
    public List<PermissionDefinition> getDefinitions() {
        return permissionService.getAllDefinitions();
    }

    /**
     * 获取所有分类
     */
    @GetMapping("/api/categories")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_admin:manage')")
    public List<String> getCategories() {
        return permissionService.getCategories();
    }

    /**
     * 新增权限定义
     */
    @PostMapping("/api/definitions")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_admin:manage')")
    public ResponseEntity<?> createDefinition(@RequestBody PermissionDefinition definition) {
        try {
            String key = definition.getPermissionKey();
            // 校验格式：必须以 PERM_ 开头，只允许字母、数字、冒号、下划线
            if (key == null || !key.matches("^PERM_[a-zA-Z0-9:_]{2,80}$")) {
                return ResponseEntity.badRequest().body(ErrorResponse.badRequest(
                    "权限标识格式错误，必须以 PERM_ 开头，只允许字母、数字、冒号、下划线，长度 3-80"));
            }
            // 禁止使用 ROLE_ 前缀（防止越权）
            if (key.startsWith("ROLE_")) {
                return ResponseEntity.badRequest().body(ErrorResponse.badRequest("权限标识不能以 ROLE_ 开头"));
            }
            if (permissionService.getAllDefinitions().stream()
                .anyMatch(d -> d.getPermissionKey().equals(key))) {
                return ResponseEntity.badRequest().body(ErrorResponse.badRequest("权限标识已存在"));
            }
            definition.setSystem(false);
            definition.setEnabled(true);
            PermissionDefinition saved = permissionService.saveDefinition(definition);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ErrorResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 更新权限定义
     */
    @PutMapping("/api/definitions/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_admin:manage')")
    public ResponseEntity<?> updateDefinition(@PathVariable Long id,
                                               @RequestBody PermissionDefinition definition) {
        try {
            definition.setId(id);
            PermissionDefinition saved = permissionService.saveDefinition(definition);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ErrorResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 切换权限启用状态
     */
    @PostMapping("/api/definitions/{id}/toggle")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_admin:manage')")
    public ResponseEntity<?> toggleDefinition(@PathVariable Long id) {
        try {
            PermissionDefinition toggled = permissionService.toggleDefinition(id);
            return ResponseEntity.ok(toggled);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ErrorResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 删除权限定义
     */
    @DeleteMapping("/api/definitions/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_admin:manage')")
    public ResponseEntity<?> deleteDefinition(@PathVariable Long id) {
        try {
            permissionService.deleteDefinition(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ErrorResponse.badRequest(e.getMessage()));
        }
    }
}
