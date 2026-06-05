package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.service.AiAssistantService;
import com.chengxun.gamemaker.service.AiAssistantService.AiResponse;
import com.chengxun.gamemaker.service.AiAssistantService.ConversationEntry;
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

import java.util.List;
import java.util.Map;

/**
 * AI助手控制器
 * 提供系统分析、问答和知识库管理的API
 *
 * @author chengxun
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/ai-assistant")
public class AiAssistantController {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantController.class);

    @Autowired
    private AiAssistantService aiAssistantService;

    @Autowired
    private ProjectManager projectManager;

    @Autowired
    private ProjectPermissionService permissionService;

    @Autowired
    private UserService userService;

    /**
     * 发送问题
     */
    /**
     * T25: 支持项目上下文
     */
    @PostMapping("/ask")
    @PreAuthorize("hasAuthority('PERM_ai:use')")
    public ResponseEntity<AiResponse> askQuestion(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        String question = request.get("question");
        String projectId = request.get("projectId");

        if (question == null || question.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new AiResponse("问题不能为空"));
        }

        String userId = authentication.getName();

        // T25: 如果指定了项目，添加项目上下文
        if (projectId != null && !projectId.isBlank()) {
            User user = userService.getUserByUsername(userId);
            if (permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.VIEWER)) {
                GameProject project = projectManager.getProject(projectId);
                if (project != null) {
                    String context = String.format(
                        "[项目上下文] 项目名称: %s, 状态: %s, 目标: %s, 进度: %d%%\n\n",
                        project.getName(),
                        project.getStatus(),
                        project.getGoal() != null ? project.getGoal() : "未设置",
                        project.getGoalProgress()
                    );
                    question = context + question;
                }
            }
        }

        log.info("User {} asking question: {}", userId,
            question.length() > 50 ? question.substring(0, 50) + "..." : question);

        AiResponse response = aiAssistantService.askQuestion(userId, question);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取对话历史
     */
    @GetMapping("/history")
    @PreAuthorize("hasAuthority('PERM_ai:use')")
    public ResponseEntity<List<ConversationEntry>> getHistory(Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(aiAssistantService.getConversationHistory(userId));
    }

    /**
     * 清空对话历史
     */
    @DeleteMapping("/history")
    @PreAuthorize("hasAuthority('PERM_ai:use')")
    public ResponseEntity<Map<String, String>> clearHistory(Authentication authentication) {
        String userId = authentication.getName();
        aiAssistantService.clearConversationHistory(userId);
        return ResponseEntity.ok(Map.of("status", "success", "message", "对话历史已清空"));
    }

    /**
     * 获取系统知识库
     */
    @GetMapping("/knowledge")
    @PreAuthorize("hasAuthority('PERM_ai:admin')")
    public ResponseEntity<Map<String, String>> getKnowledge() {
        return ResponseEntity.ok(aiAssistantService.getSystemKnowledge());
    }

    /**
     * 添加自定义知识
     */
    @PostMapping("/knowledge")
    @PreAuthorize("hasAuthority('PERM_ai:admin')")
    public ResponseEntity<Map<String, String>> addKnowledge(@RequestBody Map<String, String> request) {
        String key = request.get("key");
        String content = request.get("content");

        if (key == null || content == null) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "key和content不能为空"));
        }

        aiAssistantService.addKnowledge(key, content);
        return ResponseEntity.ok(Map.of("status", "success", "message", "知识已添加"));
    }

    /**
     * 删除自定义知识
     */
    @DeleteMapping("/knowledge/{key}")
    @PreAuthorize("hasAuthority('PERM_ai:admin')")
    public ResponseEntity<Map<String, String>> removeKnowledge(@PathVariable String key) {
        aiAssistantService.removeKnowledge(key);
        return ResponseEntity.ok(Map.of("status", "success", "message", "知识已删除"));
    }

    /**
     * 获取知识库统计
     */
    @GetMapping("/knowledge/stats")
    @PreAuthorize("hasAuthority('PERM_ai:use')")
    public ResponseEntity<Map<String, Object>> getKnowledgeStats() {
        return ResponseEntity.ok(aiAssistantService.getKnowledgeStats());
    }

    /**
     * 分析系统状态
     */
    @GetMapping("/analyze")
    @PreAuthorize("hasAuthority('PERM_ai:use')")
    public ResponseEntity<Map<String, String>> analyzeSystem() {
        String analysis = aiAssistantService.analyzeSystemStatus();
        return ResponseEntity.ok(Map.of("status", "success", "analysis", analysis));
    }
}
