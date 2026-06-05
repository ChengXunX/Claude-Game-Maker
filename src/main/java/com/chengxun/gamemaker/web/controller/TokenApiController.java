package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.entity.ApiToken;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.ApiTokenService;
import com.chengxun.gamemaker.web.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Token管理 REST API 控制器
 * 提供API Token的CRUD和管理接口
 *
 * 权限要求：
 * - 查看Token：tokens:view
 * - 管理Token：tokens:manage
 *
 * @author chengxun
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/tokens")
@Tag(name = "Token管理", description = "API Token管理API")
public class TokenApiController {

    private static final Logger log = LoggerFactory.getLogger(TokenApiController.class);

    @Autowired
    private ApiTokenService tokenService;

    @Autowired
    private UserService userService;

    /**
     * 获取所有Token
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_tokens:view')")
    @Operation(summary = "获取所有Token")
    public ResponseEntity<List<ApiToken>> getAllTokens() {
        return ResponseEntity.ok(tokenService.getAllTokens());
    }

    /**
     * 获取Token详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_tokens:view')")
    @Operation(summary = "获取Token详情")
    public ResponseEntity<?> getToken(@PathVariable Long id) {
        ApiToken token = tokenService.getTokenById(id);
        if (token == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(token);
    }

    /**
     * 创建Token
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_tokens:manage')")
    @Operation(summary = "创建Token")
    public ResponseEntity<?> createToken(@RequestBody Map<String, Object> request, Authentication authentication) {
        try {
            String tokenName = (String) request.get("tokenName");
            String apiKey = (String) request.get("apiKey");
            String apiUrl = (String) request.get("apiUrl");
            String model = (String) request.get("model");
            Integer maxTokens = request.get("maxTokens") != null ? ((Number) request.get("maxTokens")).intValue() : null;
            String description = (String) request.get("description");
            String createdBy = authentication.getName();

            ApiToken created = tokenService.createToken(tokenName, apiKey, apiUrl, model, maxTokens, description, createdBy);
            log.info("Token创建成功: {} - {}", created.getId(), tokenName);
            return ResponseEntity.ok(Map.of("success", true, "token", created));
        } catch (Exception e) {
            log.error("创建Token失败", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 更新Token
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_tokens:manage')")
    @Operation(summary = "更新Token")
    public ResponseEntity<?> updateToken(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            String tokenName = (String) request.get("tokenName");
            String apiKey = (String) request.get("apiKey");
            String apiUrl = (String) request.get("apiUrl");
            String model = (String) request.get("model");
            Integer maxTokens = request.get("maxTokens") != null ? ((Number) request.get("maxTokens")).intValue() : null;
            String description = (String) request.get("description");

            ApiToken updated = tokenService.updateToken(id, tokenName, apiKey, apiUrl, model, maxTokens, description);
            log.info("Token更新成功: {}", id);
            return ResponseEntity.ok(Map.of("success", true, "token", updated));
        } catch (Exception e) {
            log.error("更新Token失败", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 删除Token
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_tokens:manage')")
    @Operation(summary = "删除Token")
    public ResponseEntity<?> deleteToken(@PathVariable Long id) {
        try {
            tokenService.deleteToken(id);
            log.info("Token删除成功: {}", id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Token已删除"));
        } catch (Exception e) {
            log.error("删除Token失败", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 分配Token给Agent
     */
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAuthority('PERM_tokens:manage')")
    @Operation(summary = "分配Token给Agent")
    public ResponseEntity<?> assignToken(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String agentId = request.get("agentId");
            tokenService.assignToken(id, agentId);
            log.info("Token分配成功: {} -> {}", id, agentId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Token已分配"));
        } catch (Exception e) {
            log.error("分配Token失败", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 取消Token分配
     */
    @PostMapping("/{id}/unassign")
    @PreAuthorize("hasAuthority('PERM_tokens:manage')")
    @Operation(summary = "取消Token分配")
    public ResponseEntity<?> unassignToken(@PathVariable Long id) {
        try {
            tokenService.unassignToken(id);
            log.info("Token取消分配成功: {}", id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Token已取消分配"));
        } catch (Exception e) {
            log.error("取消分配Token失败", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 获取Token统计
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('PERM_tokens:view')")
    @Operation(summary = "获取Token统计")
    public ResponseEntity<Map<String, Object>> getTokenStats() {
        Map<String, Object> stats = new HashMap<>();
        List<ApiToken> tokens = tokenService.getAllTokens();
        stats.put("total", tokens.size());
        stats.put("active", tokens.stream().filter(t -> "ACTIVE".equals(t.getStatus())).count());
        stats.put("assigned", tokens.stream().filter(t -> t.getAssignedAgentId() != null).count());
        return ResponseEntity.ok(stats);
    }
}
