package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.entity.KnowledgeBase;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.AiKnowledgeBaseService;
import com.chengxun.gamemaker.web.service.PromptSecurityService;
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
 * AI 知识库 API 控制器
 *
 * @author chengxun
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/knowledge")
@Tag(name = "AI 知识库", description = "知识库管理接口")
public class KnowledgeBaseController {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseController.class);

    private final AiKnowledgeBaseService knowledgeService;
    private final PromptSecurityService securityService;
    private final UserService userService;

    public KnowledgeBaseController(AiKnowledgeBaseService knowledgeService,
                                   PromptSecurityService securityService,
                                   UserService userService) {
        this.knowledgeService = knowledgeService;
        this.securityService = securityService;
        this.userService = userService;
    }

    /**
     * 获取知识列表
     */
    @GetMapping
    @Operation(summary = "获取知识列表")
    public ResponseEntity<List<KnowledgeBase>> getKnowledge(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String projectId,
            Authentication authentication) {

        User user = userService.getUserByUsername(authentication.getName());
        List<KnowledgeBase> knowledge = knowledgeService.getKnowledge(category, projectId, user);
        return ResponseEntity.ok(knowledge);
    }

    /**
     * 获取提示词模板
     */
    @GetMapping("/prompt/{templateKey}")
    @Operation(summary = "获取提示词模板")
    public ResponseEntity<Map<String, String>> getPromptTemplate(
            @PathVariable String templateKey,
            @RequestParam(required = false) String projectId,
            Authentication authentication) {

        User user = userService.getUserByUsername(authentication.getName());
        String prompt = knowledgeService.getPromptTemplate(templateKey, projectId, user, null);
        return ResponseEntity.ok(Map.of("template", prompt));
    }

    /**
     * 构建安全的提示词
     */
    @PostMapping("/build-prompt")
    @Operation(summary = "构建安全的提示词")
    public ResponseEntity<Map<String, String>> buildSecurePrompt(
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        User user = userService.getUserByUsername(authentication.getName());
        String task = request.get("task");
        String projectId = request.get("projectId");
        String agentRole = request.get("agentRole");

        // 安全检查
        PromptSecurityService.SecurityCheckResult checkResult = securityService.checkSecurity(task);
        if (!checkResult.isSafe()) {
            log.warn("Security check failed for user {}: {}", user.getUsername(), checkResult.getIssueSummary());
            return ResponseEntity.badRequest().body(Map.of(
                "error", "输入内容未通过安全检查",
                "issues", checkResult.getIssueSummary()
            ));
        }

        String prompt = knowledgeService.buildSecurePrompt(task, projectId, user, agentRole);
        return ResponseEntity.ok(Map.of("prompt", prompt));
    }

    /**
     * 添加知识
     */
    @PostMapping
    @Operation(summary = "添加知识")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERM_knowledge:manage')")
    public ResponseEntity<KnowledgeBase> addKnowledge(@RequestBody KnowledgeBase knowledge,
                                                      Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        knowledge.setCreatedBy(user.getUsername());

        // 安全检查内容
        if (securityService.detectSensitiveInfo(knowledge.getContent())) {
            return ResponseEntity.badRequest().build();
        }

        KnowledgeBase saved = knowledgeService.addKnowledge(knowledge);
        return ResponseEntity.ok(saved);
    }

    /**
     * 更新知识
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新知识")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERM_knowledge:manage')")
    public ResponseEntity<KnowledgeBase> updateKnowledge(@PathVariable Long id,
                                                         @RequestBody KnowledgeBase knowledge) {
        // 安全检查内容
        if (securityService.detectSensitiveInfo(knowledge.getContent())) {
            return ResponseEntity.badRequest().build();
        }

        KnowledgeBase updated = knowledgeService.updateKnowledge(id, knowledge);
        return ResponseEntity.ok(updated);
    }

    /**
     * 删除知识
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERM_knowledge:manage')")
    public ResponseEntity<Void> deleteKnowledge(@PathVariable Long id) {
        knowledgeService.deleteKnowledge(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取知识统计
     */
    @GetMapping("/stats")
    @Operation(summary = "获取知识统计")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(knowledgeService.getStats());
    }

    /**
     * 安全检查
     */
    @PostMapping("/check-security")
    @Operation(summary = "安全检查")
    public ResponseEntity<PromptSecurityService.SecurityCheckResult> checkSecurity(
            @RequestBody Map<String, String> request) {
        String content = request.get("content");
        PromptSecurityService.SecurityCheckResult result = securityService.checkSecurity(content);
        return ResponseEntity.ok(result);
    }
}
