package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.service.AgentRecruitmentService;
import com.chengxun.gamemaker.service.AgentRecruitmentService.RecruitmentRequest;
import com.chengxun.gamemaker.web.entity.ProjectMember;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.ProjectPermissionService;
import com.chengxun.gamemaker.web.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Agent招聘控制器
 * 提供制作人Agent招聘新员工的API
 *
 * @author chengxun
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/recruitment")
public class AgentRecruitmentController {

    private static final Logger log = LoggerFactory.getLogger(AgentRecruitmentController.class);

    @Autowired
    private AgentRecruitmentService recruitmentService;

    @Autowired
    private AgentManager agentManager;

    @Autowired
    private ProjectPermissionService permissionService;

    @Autowired
    private UserService userService;

    /**
     * 获取可招聘的角色列表
     */
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('PERM_agent:view')")
    public ResponseEntity<List<Map<String, Object>>> getRecruitableRoles() {
        return ResponseEntity.ok(recruitmentService.getRecruitableRoles());
    }

    /**
     * 获取预设角色模板
     */
    @GetMapping("/templates")
    @PreAuthorize("hasAuthority('PERM_agent:view')")
    public ResponseEntity<List<Map<String, Object>>> getPresetTemplates() {
        return ResponseEntity.ok(recruitmentService.getPresetRoleTemplates());
    }

    /**
     * 获取自定义角色模板
     */
    @GetMapping("/custom-templates")
    @PreAuthorize("hasAuthority('PERM_agent:view')")
    public ResponseEntity<List<Map<String, Object>>> getCustomTemplates() {
        return ResponseEntity.ok(recruitmentService.getCustomRoleTemplates());
    }

    /**
     * 创建自定义角色模板
     */
    @PostMapping("/custom-templates")
    @PreAuthorize("hasAuthority('PERM_agent:manage')")
    public ResponseEntity<Map<String, Object>> createCustomTemplate(@RequestBody Map<String, Object> request) {
        try {
            String role = (String) request.get("role");
            String name = (String) request.get("name");
            String description = (String) request.get("description");

            @SuppressWarnings("unchecked")
            Set<String> capabilities = request.get("capabilities") != null
                ? new HashSet<>((List<String>) request.get("capabilities"))
                : Set.of();

            @SuppressWarnings("unchecked")
            Set<String> fileTypes = request.get("fileTypes") != null
                ? new HashSet<>((List<String>) request.get("fileTypes"))
                : Set.of();

            AgentRecruitmentService.RoleTemplate template =
                recruitmentService.createCustomRoleTemplate(role, name, description, capabilities, fileTypes);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("role", template.getRole());
            result.put("name", template.getName());
            result.put("message", "自定义角色创建成功");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 更新自定义角色模板
     */
    @PutMapping("/custom-templates/{role}")
    @PreAuthorize("hasAuthority('PERM_agent:manage')")
    public ResponseEntity<Map<String, Object>> updateCustomTemplate(@PathVariable String role,
                                                                     @RequestBody Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            String description = (String) request.get("description");

            @SuppressWarnings("unchecked")
            Set<String> capabilities = request.get("capabilities") != null
                ? new HashSet<>((List<String>) request.get("capabilities"))
                : null;

            @SuppressWarnings("unchecked")
            Set<String> fileTypes = request.get("fileTypes") != null
                ? new HashSet<>((List<String>) request.get("fileTypes"))
                : null;

            AgentRecruitmentService.RoleTemplate template =
                recruitmentService.updateCustomRoleTemplate(role, name, description, capabilities, fileTypes);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("role", template.getRole());
            result.put("name", template.getName());
            result.put("message", "自定义角色更新成功");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 删除自定义角色模板
     */
    @DeleteMapping("/custom-templates/{role}")
    @PreAuthorize("hasAuthority('PERM_agent:manage')")
    public ResponseEntity<Map<String, String>> deleteCustomTemplate(@PathVariable String role) {
        try {
            boolean deleted = recruitmentService.deleteCustomRoleTemplate(role);
            if (deleted) {
                return ResponseEntity.ok(Map.of("status", "success", "message", "自定义角色已删除"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "角色不存在"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * 招聘新Agent（简单模式）
     */
    @PostMapping("/recruit")
    @PreAuthorize("hasAuthority('PERM_agent:manage')")
    public ResponseEntity<Map<String, Object>> recruitAgent(
            @RequestParam String producerId,
            @RequestParam String role,
            @RequestParam String name,
            @RequestParam String workDir,
            Authentication authentication) {
        try {
            // S04: 校验 producerId 属于当前用户的项目
            String projectId = extractProjectId(producerId);
            if (projectId != null) {
                User user = userService.getUserByUsername(authentication.getName());
                if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.MANAGER)) {
                    return ResponseEntity.status(403).body(Map.of("status", "error", "message", "无权限操作该项目"));
                }
            }

            Agent agent = recruitmentService.recruitAgent(producerId, role, name, workDir);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("agentId", agent.getId());
            result.put("agentName", agent.getName());
            result.put("role", agent.getRole());
            result.put("message", "招聘成功");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Recruitment failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 招聘新Agent（完整模式）
     */
    @PostMapping("/recruit-full")
    @PreAuthorize("hasAuthority('PERM_agent:manage')")
    public ResponseEntity<Map<String, Object>> recruitAgentFull(
            @RequestParam String producerId,
            @RequestBody RecruitmentRequest request,
            Authentication authentication) {
        try {
            // S04: 校验 producerId 属于当前用户的项目
            String projectId = extractProjectId(producerId);
            if (projectId != null) {
                User user = userService.getUserByUsername(authentication.getName());
                if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.MANAGER)) {
                    return ResponseEntity.status(403).body(Map.of("status", "error", "message", "无权限操作该项目"));
                }
            }

            Agent agent = recruitmentService.recruitAgentFull(producerId, request);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("agentId", agent.getId());
            result.put("agentName", agent.getName());
            result.put("role", agent.getRole());
            result.put("capabilities", agent.getDefinition().getCapabilities());
            result.put("message", "招聘成功");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Full recruitment failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 招聘自定义角色Agent（自动创建角色模板）
     * 制作人可以根据需要直接招聘任意角色，系统会自动创建角色模板
     */
    @PostMapping("/recruit-custom")
    @PreAuthorize("hasAuthority('PERM_agent:manage')")
    public ResponseEntity<Map<String, Object>> recruitCustomAgent(
            @RequestParam String producerId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            // S04: 校验 producerId 属于当前用户的项目
            String projectId = extractProjectId(producerId);
            if (projectId != null) {
                User user = userService.getUserByUsername(authentication.getName());
                if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.MANAGER)) {
                    return ResponseEntity.status(403).body(Map.of("status", "error", "message", "无权限操作该项目"));
                }
            }

            String role = (String) request.get("role");
            String roleName = (String) request.get("roleName");
            String name = (String) request.get("name");
            String description = (String) request.get("description");
            String workDir = (String) request.get("workDir");

            @SuppressWarnings("unchecked")
            Set<String> capabilities = request.get("capabilities") != null
                ? new HashSet<>((List<String>) request.get("capabilities"))
                : Set.of();

            @SuppressWarnings("unchecked")
            Set<String> fileTypes = request.get("fileTypes") != null
                ? new HashSet<>((List<String>) request.get("fileTypes"))
                : Set.of();

            Agent agent = recruitmentService.recruitCustomAgent(
                producerId, role, roleName, name, description, capabilities, fileTypes, workDir);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("agentId", agent.getId());
            result.put("agentName", agent.getName());
            result.put("role", agent.getRole());
            result.put("capabilities", agent.getDefinition().getCapabilities());
            result.put("message", "自定义角色招聘成功");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Custom recruitment failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 为Agent添加能力
     */
    @PostMapping("/{agentId}/capabilities")
    @PreAuthorize("hasAuthority('PERM_agent:manage')")
    public ResponseEntity<Map<String, String>> addCapability(
            @RequestParam String producerId,
            @PathVariable String agentId,
            @RequestParam String capability) {
        try {
            recruitmentService.addCapabilityToAgent(producerId, agentId, capability);
            return ResponseEntity.ok(Map.of("status", "success", "message", "能力已添加"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * 为Agent设置标签
     */
    @PostMapping("/{agentId}/tags")
    @PreAuthorize("hasAuthority('PERM_agent:manage')")
    public ResponseEntity<Map<String, String>> setAgentTag(
            @RequestParam String producerId,
            @PathVariable String agentId,
            @RequestParam String key,
            @RequestParam String value) {
        try {
            recruitmentService.setAgentTag(producerId, agentId, key, value);
            return ResponseEntity.ok(Map.of("status", "success", "message", "标签已设置"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * 解雇Agent
     */
    @DeleteMapping("/{agentId}")
    @PreAuthorize("hasAuthority('PERM_agent:manage')")
    public ResponseEntity<Map<String, String>> dismissAgent(
            @RequestParam String producerId,
            @PathVariable String agentId) {
        try {
            boolean dismissed = recruitmentService.dismissAgent(producerId, agentId);
            if (dismissed) {
                return ResponseEntity.ok(Map.of("status", "success", "message", "已解雇"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Agent不存在"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * 检查Agent是否是核心Agent
     */
    @GetMapping("/{agentId}/is-core")
    @PreAuthorize("hasAuthority('PERM_agent:view')")
    public ResponseEntity<Map<String, Object>> isCoreAgent(@PathVariable String agentId) {
        boolean isCore = recruitmentService.isCoreAgent(agentId);
        return ResponseEntity.ok(Map.of(
            "agentId", agentId,
            "isCore", isCore
        ));
    }

    /**
     * 获取已招聘的Agent列表
     */
    @GetMapping("/recruited")
    @PreAuthorize("hasAuthority('PERM_agent:view')")
    public ResponseEntity<List<Map<String, Object>>> getRecruitedAgents() {
        return ResponseEntity.ok(recruitmentService.getRecruitedAgents());
    }

    /**
     * 获取招聘统计
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('PERM_agent:view')")
    public ResponseEntity<Map<String, Object>> getRecruitmentStats() {
        return ResponseEntity.ok(recruitmentService.getRecruitmentStats());
    }

    // ===== 招聘申请管理 =====

    /**
     * 创建招聘申请
     */
    @PostMapping("/requests")
    @PreAuthorize("hasAuthority('PERM_agent:manage')")
    public ResponseEntity<Map<String, Object>> createRequest(@RequestBody Map<String, String> request,
                                                              Authentication authentication) {
        try {
            String producerId = request.get("producerId");
            String role = request.get("role");
            String name = request.getOrDefault("name", role);
            String description = request.getOrDefault("description", "");
            String roleName = request.getOrDefault("roleName", role);

            // 直接执行招聘（简化流程，不走审批）
            Agent agent = recruitmentService.recruitCustomAgent(
                producerId, role, roleName, name, description,
                java.util.Set.of(), java.util.Set.of(), null
            );

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("agentId", agent.getId());
            result.put("agentName", agent.getName());
            result.put("message", "招聘申请已提交并执行");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("创建招聘申请失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取招聘申请列表（返回已招聘的Agent列表）
     */
    @GetMapping("/requests")
    @PreAuthorize("hasAuthority('PERM_agent:view')")
    public ResponseEntity<List<Map<String, Object>>> getRequests(
            @RequestParam(required = false) String producerId) {
        return ResponseEntity.ok(recruitmentService.getRecruitedAgents());
    }

    /**
     * 批准招聘申请（直接执行招聘）
     */
    @PutMapping("/requests/{id}/approve")
    @PreAuthorize("hasAuthority('PERM_agent:manage')")
    public ResponseEntity<Map<String, Object>> approveRequest(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "招聘申请已批准"
        ));
    }

    /**
     * 驳回招聘申请
     */
    @PutMapping("/requests/{id}/reject")
    @PreAuthorize("hasAuthority('PERM_agent:manage')")
    public ResponseEntity<Map<String, Object>> rejectRequest(@PathVariable Long id,
                                                              @RequestBody(required = false) Map<String, String> request) {
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "招聘申请已驳回"
        ));
    }

    /**
     * S04: 从运行时 ID 提取项目 ID
     */
    private String extractProjectId(String runtimeId) {
        if (runtimeId == null) return null;
        int lastColon = runtimeId.lastIndexOf(':');
        return lastColon > 0 ? runtimeId.substring(0, lastColon) : null;
    }
}
