package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.service.AgentCapabilityService;
import com.chengxun.gamemaker.service.GameProjectTemplateService;
import com.chengxun.gamemaker.service.GameProjectTemplateService.GameType;
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
 * Agent能力管理控制器
 *
 * 权限模型：
 * - Agent 运行时 ID 格式: projectId:role
 * - 操作 Agent 时需要校验用户对该项目的权限
 *
 * @author chengxun
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/agent-capabilities")
@PreAuthorize("hasAnyAuthority('PERM_agents:view', 'PERM_agents:manage', 'PERM_admin:manage')")
public class AgentCapabilityController {

    private static final Logger log = LoggerFactory.getLogger(AgentCapabilityController.class);

    @Autowired
    private AgentCapabilityService capabilityService;

    @Autowired
    private GameProjectTemplateService templateService;

    @Autowired
    private ProjectPermissionService permissionService;

    @Autowired
    private UserService userService;

    @Autowired
    private AgentManager agentManager;

    // ===== 标签管理 =====

    @GetMapping("/{agentId}/tags")
    @PreAuthorize("hasAuthority('PERM_agent:view')")
    public ResponseEntity<Map<String, String>> getAgentTags(@PathVariable String agentId,
                                                             Authentication authentication) {
        if (!checkAgentAccess(agentId, authentication, ProjectMember.ProjectRole.VIEWER)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(capabilityService.getAgentTags(agentId));
    }

    @PostMapping("/{agentId}/tags")
    @PreAuthorize("hasAuthority('PERM_agent:manage')")
    public ResponseEntity<Map<String, String>> setAgentTag(@PathVariable String agentId,
                                                            @RequestParam String key,
                                                            @RequestParam String value,
                                                            Authentication authentication) {
        if (!checkAgentAccess(agentId, authentication, ProjectMember.ProjectRole.DEVELOPER)) {
            return ResponseEntity.status(403).build();
        }
        capabilityService.setAgentTag(agentId, key, value);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Tag set"));
    }

    @DeleteMapping("/{agentId}/tags/{key}")
    @PreAuthorize("hasAuthority('PERM_agent:manage')")
    public ResponseEntity<Map<String, String>> removeAgentTag(@PathVariable String agentId,
                                                               @PathVariable String key,
                                                               Authentication authentication) {
        if (!checkAgentAccess(agentId, authentication, ProjectMember.ProjectRole.DEVELOPER)) {
            return ResponseEntity.status(403).build();
        }
        capabilityService.removeAgentTag(agentId, key);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Tag removed"));
    }

    // ===== 能力管理 =====

    @GetMapping("/{agentId}/capabilities")
    @PreAuthorize("hasAuthority('PERM_agent:view')")
    public ResponseEntity<Set<String>> getAgentCapabilities(@PathVariable String agentId,
                                                             Authentication authentication) {
        if (!checkAgentAccess(agentId, authentication, ProjectMember.ProjectRole.VIEWER)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(capabilityService.getAgentCapabilities(agentId));
    }

    @PostMapping("/{agentId}/capabilities")
    @PreAuthorize("hasAuthority('PERM_agent:manage')")
    public ResponseEntity<Map<String, String>> addCapability(@PathVariable String agentId,
                                                              @RequestParam String capability,
                                                              Authentication authentication) {
        if (!checkAgentAccess(agentId, authentication, ProjectMember.ProjectRole.MANAGER)) {
            return ResponseEntity.status(403).build();
        }
        capabilityService.addCapability(agentId, capability);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Capability added"));
    }

    @PostMapping("/find-by-capabilities")
    @PreAuthorize("hasAuthority('PERM_agent:view')")
    public ResponseEntity<List<Map<String, Object>>> findByCapabilities(
            @RequestBody Set<String> capabilities,
            @RequestParam(defaultValue = "") Set<String> excludeAgentIds) {
        List<Agent> agents = capabilityService.findAgentsByCapabilities(capabilities, excludeAgentIds);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Agent agent : agents) {
            Map<String, Object> info = new HashMap<>();
            info.put("id", agent.getId());
            info.put("name", agent.getName());
            info.put("role", agent.getRole());
            info.put("capabilities", agent.getDefinition().getCapabilities());
            result.add(info);
        }
        return ResponseEntity.ok(result);
    }

    // ===== 上下文管理 =====

    @GetMapping("/{agentId}/context-usage")
    @PreAuthorize("hasAuthority('PERM_agent:view')")
    public ResponseEntity<Map<String, Object>> getContextUsage(@PathVariable String agentId,
                                                                Authentication authentication) {
        if (!checkAgentAccess(agentId, authentication, ProjectMember.ProjectRole.VIEWER)) {
            return ResponseEntity.status(403).build();
        }
        Map<String, Object> usage = capabilityService.getContextUsage(agentId);
        if (usage.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(usage);
    }

    @PostMapping("/{agentId}/context-usage")
    @PreAuthorize("hasAuthority('PERM_agent:manage')")
    public ResponseEntity<Map<String, String>> updateContextUsage(@PathVariable String agentId,
                                                                   @RequestParam int usage,
                                                                   Authentication authentication) {
        if (!checkAgentAccess(agentId, authentication, ProjectMember.ProjectRole.MANAGER)) {
            return ResponseEntity.status(403).build();
        }
        capabilityService.updateContextUsage(agentId, usage);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Context usage updated"));
    }

    // ===== API能力 =====

    @GetMapping("/{agentId}/api-capabilities")
    @PreAuthorize("hasAuthority('PERM_agent:view')")
    public ResponseEntity<Map<String, Object>> getApiCapabilities(@PathVariable String agentId,
                                                                   Authentication authentication) {
        if (!checkAgentAccess(agentId, authentication, ProjectMember.ProjectRole.VIEWER)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(capabilityService.getApiCapabilities(agentId));
    }

    @GetMapping("/{agentId}/supports/{feature}")
    @PreAuthorize("hasAuthority('PERM_agent:view')")
    public ResponseEntity<Map<String, Object>> supportsFeature(@PathVariable String agentId,
                                                                @PathVariable String feature,
                                                                Authentication authentication) {
        if (!checkAgentAccess(agentId, authentication, ProjectMember.ProjectRole.VIEWER)) {
            return ResponseEntity.status(403).build();
        }
        boolean supported = capabilityService.supportsFeature(agentId, feature);
        return ResponseEntity.ok(Map.of("agentId", agentId, "feature", feature, "supported", supported));
    }

    // ===== 智能分配 =====

    @PostMapping("/recommend")
    @PreAuthorize("hasAuthority('PERM_agent:view')")
    public ResponseEntity<List<Map<String, Object>>> recommendAgents(
            @RequestParam String taskDescription,
            @RequestBody(required = false) Set<String> requiredCapabilities,
            @RequestParam(required = false) String requiredFileType,
            @RequestParam(defaultValue = "100") int maxLoad) {
        List<Agent> agents = capabilityService.recommendAgentsForTask(
            taskDescription, requiredCapabilities, requiredFileType, maxLoad);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Agent agent : agents) {
            Map<String, Object> info = new HashMap<>();
            info.put("id", agent.getId());
            info.put("name", agent.getName());
            info.put("role", agent.getRole());
            info.put("capabilities", agent.getDefinition().getCapabilities());
            result.add(info);
        }
        return ResponseEntity.ok(result);
    }

    // ===== 统计 =====

    /**
     * F06: 支持按项目筛选
     */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('PERM_agent:view')")
    public ResponseEntity<List<Map<String, Object>>> getAllCapabilities(
            @RequestParam(required = false) String projectId,
            Authentication authentication) {
        if (projectId != null && !projectId.isEmpty()) {
            User user = userService.getUserByUsername(authentication.getName());
            if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.VIEWER)) {
                return ResponseEntity.status(403).build();
            }
            // 返回项目内 Agent 的能力
            List<Map<String, Object>> result = new ArrayList<>();
            for (Agent agent : agentManager.getAgentsByProject(projectId)) {
                Map<String, Object> info = new HashMap<>();
                info.put("id", agent.getId());
                info.put("name", agent.getName());
                info.put("role", agent.getRole());
                info.put("capabilities", agent.getDefinition().getCapabilities());
                result.add(info);
            }
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.ok(capabilityService.getAllAgentsCapabilities());
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('PERM_agent:view')")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(capabilityService.getCapabilityStatistics());
    }

    // ===== 游戏项目模板 =====

    @GetMapping("/game-types")
    @PreAuthorize("hasAuthority('PERM_agent:view')")
    public ResponseEntity<List<Map<String, String>>> getSupportedGameTypes() {
        return ResponseEntity.ok(templateService.getSupportedGameTypes());
    }

    @GetMapping("/game-types/{gameType}/agents")
    @PreAuthorize("hasAuthority('PERM_agent:view')")
    public ResponseEntity<List<Map<String, Object>>> getDefaultAgents(
            @PathVariable GameType gameType,
            @RequestParam String projectWorkDir,
            @RequestParam(defaultValue = "false") boolean supportsImageGeneration) {
        List<com.chengxun.gamemaker.model.AgentDefinition> agents =
            templateService.getDefaultAgentConfigs(gameType, projectWorkDir, supportsImageGeneration);

        List<Map<String, Object>> result = new ArrayList<>();
        for (com.chengxun.gamemaker.model.AgentDefinition def : agents) {
            Map<String, Object> info = new HashMap<>();
            info.put("id", def.getId());
            info.put("name", def.getName());
            info.put("role", def.getRole());
            info.put("description", def.getDescription());
            info.put("capabilities", def.getCapabilities());
            result.add(info);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/game-types/{gameType}/directory-structure")
    @PreAuthorize("hasAuthority('PERM_agent:view')")
    public ResponseEntity<Map<String, List<String>>> getDirectoryStructure(@PathVariable GameType gameType) {
        return ResponseEntity.ok(templateService.getProjectDirectoryStructure(gameType));
    }

    @GetMapping("/game-types/{gameType}/tech-stack")
    @PreAuthorize("hasAuthority('PERM_agent:view')")
    public ResponseEntity<Map<String, Object>> getTechStack(@PathVariable GameType gameType) {
        return ResponseEntity.ok(templateService.getTechStackRecommendation(gameType));
    }

    // ===== 辅助方法 =====

    /**
     * 检查用户是否有权限操作指定 Agent
     * 从 agentId（运行时 ID: projectId:role）中提取 projectId 并校验权限
     */
    private boolean checkAgentAccess(String agentId, Authentication authentication,
                                      ProjectMember.ProjectRole requiredRole) {
        User user = userService.getUserByUsername(authentication.getName());
        if (user == null) return false;
        if (user.isAdmin()) return true;

        // 从运行时 ID 提取 projectId
        String projectId = extractProjectId(agentId);
        if (projectId == null) return true; // 无法确定项目，允许访问（向后兼容）

        return permissionService.hasProjectAccess(user, projectId, requiredRole);
    }

    /**
     * 从运行时 ID 中提取项目 ID
     * 运行时 ID 格式: projectId:agentRole
     */
    private String extractProjectId(String runtimeId) {
        if (runtimeId == null) return null;
        int lastColon = runtimeId.lastIndexOf(':');
        if (lastColon > 0) {
            return runtimeId.substring(0, lastColon);
        }
        return null;
    }
}
