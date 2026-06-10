package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.web.entity.ApiToken;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.ApiTokenService;
import com.chengxun.gamemaker.web.service.OperationLogService;
import com.chengxun.gamemaker.web.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Token 管理 REST API 控制器
 * 为 Vue 前端提供 JSON 接口
 *
 * @author chengxun
 * @since 1.0.0
 */
@Tag(name = "Token管理", description = "API Token 管理接口")
@RestController
@RequestMapping("/api/tokens")
public class TokenApiController {

    private static final Logger log = LoggerFactory.getLogger(TokenApiController.class);

    private final ApiTokenService tokenService;
    private final AgentManager agentManager;
    private final UserService userService;
    private final OperationLogService logService;

    public TokenApiController(ApiTokenService tokenService, AgentManager agentManager,
                              UserService userService, OperationLogService logService) {
        this.tokenService = tokenService;
        this.agentManager = agentManager;
        this.userService = userService;
        this.logService = logService;
    }

    @GetMapping
    @Operation(summary = "获取Token列表")
    @PreAuthorize("hasAnyAuthority('PERM_agents:manage', 'PERM_agents:task')")
    public ResponseEntity<List<ApiToken>> getAll() {
        return ResponseEntity.ok(tokenService.getAllTokens());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取Token详情")
    @PreAuthorize("hasAnyAuthority('PERM_agents:manage', 'PERM_agents:task')")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        ApiToken token = tokenService.getTokenById(id);
        if (token == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(token);
    }

    @PostMapping
    @Operation(summary = "创建Token")
    @PreAuthorize("hasAuthority('PERM_agents:manage')")
    public ResponseEntity<?> create(@RequestBody Map<String, Object> request, Authentication authentication) {
        String name = (String) request.get("name");
        String apiKey = (String) request.get("apiKey");
        String apiUrl = (String) request.get("apiUrl");
        String model = (String) request.get("model");
        Integer maxTokens = request.get("maxTokens") != null ?
            Integer.parseInt(String.valueOf(request.get("maxTokens"))) : 4096;
        Integer contextWindow = request.get("contextWindow") != null ?
            Integer.parseInt(String.valueOf(request.get("contextWindow"))) : 200000;
        String description = (String) request.get("description");
        String agentTags = (String) request.get("agentTags");
        Integer priority = request.get("priority") != null ?
            Integer.parseInt(String.valueOf(request.get("priority"))) : 10;

        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Token 名称不能为空"));
        }
        if (apiKey == null || apiKey.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "API Key 不能为空"));
        }

        try {
            String username = authentication.getName();
            Long userId = getUserId(authentication);
            ApiToken token = tokenService.createToken(name, apiKey, apiUrl, model, maxTokens, contextWindow, description, username);
            token.setUserId(userId);
            token.setAgentTags(agentTags);
            token.setPriority(priority);
            tokenService.saveToken(token);

            logService.log(userId, username, "CREATE_TOKEN", name, "Created API token (contextWindow=" + contextWindow + ")", null);
            return ResponseEntity.ok(Map.of("success", true, "message", "Token 创建成功", "token", token));
        } catch (Exception e) {
            log.error("Failed to create token", e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "创建失败: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新Token")
    @PreAuthorize("hasAuthority('PERM_agents:manage')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> request,
                                    Authentication authentication) {
        try {
            ApiToken token = tokenService.getTokenById(id);
            if (token == null) return ResponseEntity.notFound().build();

            String name = (String) request.getOrDefault("name", token.getName());
            String apiKey = (String) request.get("apiKey");
            String apiUrl = (String) request.getOrDefault("apiUrl", token.getApiUrl());
            String model = (String) request.getOrDefault("model", token.getModel());
            Integer maxTokens = request.get("maxTokens") != null ?
                Integer.parseInt(String.valueOf(request.get("maxTokens"))) : token.getMaxTokens();
            Integer contextWindow = request.get("contextWindow") != null ?
                Integer.parseInt(String.valueOf(request.get("contextWindow"))) : token.getContextWindow();
            String description = (String) request.getOrDefault("description", token.getDescription());
            String agentTags = (String) request.get("agentTags");
            Integer priority = request.get("priority") != null ?
                Integer.parseInt(String.valueOf(request.get("priority"))) : token.getPriority();
            String status = (String) request.get("status");

            tokenService.updateToken(id, name, apiKey, apiUrl, model, maxTokens, contextWindow, description);
            ApiToken updated = tokenService.getTokenById(id);
            if (agentTags != null) updated.setAgentTags(agentTags);
            if (priority != null) updated.setPriority(priority);
            if (status != null) {
                try { updated.setStatus(ApiToken.TokenStatus.valueOf(status)); } catch (IllegalArgumentException ignored) {}
            }
            tokenService.saveToken(updated);

            logService.log(getUserId(authentication), authentication.getName(), "UPDATE_TOKEN", name, "Updated API token (contextWindow=" + contextWindow + ")", null);
            return ResponseEntity.ok(Map.of("success", true, "message", "Token 已更新", "token", updated));
        } catch (Exception e) {
            log.error("Failed to update token {}", id, e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "更新失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除Token")
    @PreAuthorize("hasAuthority('PERM_agents:manage')")
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication authentication) {
        try {
            ApiToken token = tokenService.getTokenById(id);
            String tokenName = token != null ? token.getName() : "Unknown";
            tokenService.deleteToken(id);
            logService.log(getUserId(authentication), authentication.getName(), "DELETE_TOKEN", tokenName, "Deleted API token", null);
            return ResponseEntity.ok(Map.of("success", true, "message", "Token 已删除"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "删除失败: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "分配Token给Agent")
    @PreAuthorize("hasAuthority('PERM_agents:manage')")
    public ResponseEntity<?> assign(@PathVariable Long id, @RequestBody Map<String, String> request,
                                    Authentication authentication) {
        String agentId = request.get("agentId");
        String activation = request.getOrDefault("activation", "immediate");

        if (agentId == null || agentId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Agent ID 不能为空"));
        }

        try {
            ApiToken token = tokenService.assignToken(id, agentId, activation);
            String activationLabel = "pending".equals(activation) ? "等待任务完成" : "立即";
            logService.log(getUserId(authentication), authentication.getName(), "ASSIGN_TOKEN",
                token.getName(), "Assigned to agent: " + agentId + " (" + activationLabel + ")", null);
            return ResponseEntity.ok(Map.of("success", true, "message", "Token 已分配（" + activationLabel + "生效）"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "分配失败: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/unassign")
    @Operation(summary = "取消Token分配")
    @PreAuthorize("hasAuthority('PERM_agents:manage')")
    public ResponseEntity<?> unassign(@PathVariable Long id, Authentication authentication) {
        try {
            ApiToken token = tokenService.unassignToken(id);
            logService.log(getUserId(authentication), authentication.getName(), "UNASSIGN_TOKEN",
                token.getName(), "Unassigned from agent", null);
            return ResponseEntity.ok(Map.of("success", true, "message", "Token 已取消分配"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "操作失败: " + e.getMessage()));
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "获取Token统计")
    @PreAuthorize("hasAnyAuthority('PERM_agents:manage', 'PERM_agents:task')")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", tokenService.getAllTokens().size());
        stats.put("active", tokenService.getActiveTokenCount());
        stats.put("assigned", tokenService.getAssignedTokenCount());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/agents")
    @Operation(summary = "获取可分配的Agent列表")
    @PreAuthorize("hasAnyAuthority('PERM_agents:manage', 'PERM_agents:task')")
    public ResponseEntity<List<Map<String, String>>> getAvailableAgents() {
        List<Map<String, String>> agents = agentManager.getAllAgents().stream()
            .map(agent -> {
                Map<String, String> info = new HashMap<>();
                info.put("id", agent.getId());
                info.put("name", agent.getName());
                info.put("role", agent.getDefinition().getRole());
                info.put("project", agent.getDefinition().getProjectId());
                return info;
            })
            .toList();
        return ResponseEntity.ok(agents);
    }

    private Long getUserId(Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        return user != null ? user.getId() : null;
    }
}
