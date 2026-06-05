package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.dto.UserDTO;
import com.chengxun.gamemaker.web.entity.Role;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.OperationLogService;
import com.chengxun.gamemaker.web.service.RoleService;
import com.chengxun.gamemaker.web.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 管理后台 REST API 控制器
 * 提供用户管理、角色管理等 REST 接口
 *
 * @author chengxun
 * @since 2.0.0
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('PERM_admin:manage')")
public class AdminApiController {

    private static final Logger log = LoggerFactory.getLogger(AdminApiController.class);

    private final UserService userService;
    private final RoleService roleService;
    private final OperationLogService logService;

    public AdminApiController(UserService userService, RoleService roleService,
                              OperationLogService logService) {
        this.userService = userService;
        this.roleService = roleService;
        this.logService = logService;
    }

    // ===== 用户管理 =====

    /**
     * 获取所有用户列表
     * 返回UserDTO，隐藏密码等敏感信息
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> listUsers() {
        List<UserDTO> users = userService.getAllUsers().stream()
            .map(UserDTO::fromEntity)
            .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    /**
     * 获取待审批用户列表
     */
    @GetMapping("/users/pending")
    @Operation(summary = "获取待审批用户")
    @PreAuthorize("hasAuthority('PERM_users:manage')")
    public ResponseEntity<List<UserDTO>> listPendingUsers() {
        List<UserDTO> users = userService.getPendingUsers().stream()
            .map(UserDTO::fromEntity)
            .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    /**
     * 获取单个用户
     * 返回UserDTO，隐藏密码等敏感信息
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(UserDTO.fromEntity(user));
    }

    /**
     * 创建用户
     */
    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody Map<String, Object> request,
                                         Authentication authentication) {
        try {
            String username = (String) request.get("username");
            String password = (String) request.get("password");
            String email = (String) request.get("email");
            String nickname = (String) request.get("nickname");
            Long roleId = request.get("roleId") != null ? ((Number) request.get("roleId")).longValue() : null;

            User user = userService.createUser(username, password, email, nickname, roleId);
            logService.log(getUserId(authentication), "CREATE_USER", username, "Created user", null);
            return ResponseEntity.ok(Map.of("success", true, "message", "用户创建成功", "user", user));
        } catch (Exception e) {
            log.error("Failed to create user", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 更新用户
     */
    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id,
                                         @RequestBody Map<String, Object> request,
                                         Authentication authentication) {
        try {
            User user = userService.updateUser(id, request);
            logService.log(getUserId(authentication), "UPDATE_USER", user.getUsername(), "Updated user", null);
            return ResponseEntity.ok(Map.of("success", true, "message", "用户更新成功", "user", user));
        } catch (Exception e) {
            log.error("Failed to update user", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id,
                                         Authentication authentication) {
        try {
            User user = userService.getUserById(id);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            userService.deleteUser(id);
            logService.log(getUserId(authentication), "DELETE_USER", user.getUsername(), "Deleted user", null);
            return ResponseEntity.ok(Map.of("success", true, "message", "用户已删除"));
        } catch (Exception e) {
            log.error("Failed to delete user", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 批准用户
     */
    @PostMapping("/users/{id}/approve")
    public ResponseEntity<?> approveUser(@PathVariable Long id,
                                          Authentication authentication) {
        try {
            User user = userService.approveUser(id);
            logService.log(getUserId(authentication), "APPROVE_USER", user.getUsername(), "Approved user", null);
            return ResponseEntity.ok(Map.of("success", true, "message", "用户已批准"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 拒绝用户
     */
    @PostMapping("/users/{id}/reject")
    public ResponseEntity<?> rejectUser(@PathVariable Long id,
                                         Authentication authentication) {
        try {
            User user = userService.rejectUser(id);
            logService.log(getUserId(authentication), "REJECT_USER", user.getUsername(), "Rejected user", null);
            return ResponseEntity.ok(Map.of("success", true, "message", "用户已拒绝"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 禁用用户
     */
    @PostMapping("/users/{id}/disable")
    public ResponseEntity<?> disableUser(@PathVariable Long id,
                                          Authentication authentication) {
        try {
            User user = userService.disableUser(id);
            logService.log(getUserId(authentication), "DISABLE_USER", user.getUsername(), "Disabled user", null);
            return ResponseEntity.ok(Map.of("success", true, "message", "用户已禁用"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 更新用户角色
     * 禁止修改超级管理员（ID=1）的角色
     */
    @PostMapping("/users/{userId}/role")
    public ResponseEntity<?> updateUserRole(@PathVariable Long userId,
                                             @RequestBody Map<String, Long> request,
                                             Authentication authentication) {
        try {
            // 禁止修改超级管理员角色
            if (userId == 1L) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "超级管理员角色不允许修改"));
            }

            Long roleId = request.get("roleId");
            if (roleId == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "角色ID不能为空"));
            }
            User user = userService.updateUserRole(userId, roleId);
            logService.log(getUserId(authentication), "UPDATE_ROLE", user.getUsername(),
                "Role updated to " + user.getRole().getDisplayName(), null);
            return ResponseEntity.ok(Map.of("success", true, "message", "角色已更新"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ===== 角色管理 =====

    /**
     * 获取所有角色
     */
    @GetMapping("/roles")
    public ResponseEntity<List<Role>> listRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    /**
     * 获取单个角色
     */
    @GetMapping("/roles/{id}")
    public ResponseEntity<Role> getRole(@PathVariable Long id) {
        Role role = roleService.getRoleById(id);
        if (role == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(role);
    }

    /**
     * 创建角色
     */
    @PostMapping("/roles")
    public ResponseEntity<?> createRole(@RequestBody Map<String, Object> request,
                                         Authentication authentication) {
        try {
            String name = (String) request.get("name");
            String displayName = (String) request.get("displayName");
            String description = (String) request.get("description");
            @SuppressWarnings("unchecked")
            List<String> permissionsList = (List<String>) request.get("permissions");
            Set<String> permissions = permissionsList != null ? new HashSet<>(permissionsList) : new HashSet<>();

            Role role = roleService.createRole(name, displayName, description, permissions);
            logService.log(getUserId(authentication), "CREATE_ROLE", name, "Created role", null);
            return ResponseEntity.ok(Map.of("success", true, "message", "角色创建成功", "role", role));
        } catch (Exception e) {
            log.error("Failed to create role", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 更新角色
     */
    @PutMapping("/roles/{id}")
    public ResponseEntity<?> updateRole(@PathVariable Long id,
                                         @RequestBody Map<String, Object> request,
                                         Authentication authentication) {
        try {
            String displayName = (String) request.get("displayName");
            String description = (String) request.get("description");
            @SuppressWarnings("unchecked")
            List<String> permissionsList = (List<String>) request.get("permissions");
            Set<String> permissions = permissionsList != null ? new HashSet<>(permissionsList) : new HashSet<>();

            Role role = roleService.updateRole(id, displayName, description, permissions);
            logService.log(getUserId(authentication), "UPDATE_ROLE", role.getName(), "Updated role", null);
            return ResponseEntity.ok(Map.of("success", true, "message", "角色更新成功", "role", role));
        } catch (Exception e) {
            log.error("Failed to update role", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/roles/{id}")
    public ResponseEntity<?> deleteRole(@PathVariable Long id,
                                         Authentication authentication) {
        try {
            Role role = roleService.getRoleById(id);
            if (role == null) {
                return ResponseEntity.notFound().build();
            }
            if (role.isSystem()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "系统内置角色不可删除"));
            }
            roleService.deleteRole(id);
            logService.log(getUserId(authentication), "DELETE_ROLE", role.getName(), "Deleted role", null);
            return ResponseEntity.ok(Map.of("success", true, "message", "角色已删除"));
        } catch (Exception e) {
            log.error("Failed to delete role", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    private Long getUserId(Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        return user != null ? user.getId() : null;
    }
}
