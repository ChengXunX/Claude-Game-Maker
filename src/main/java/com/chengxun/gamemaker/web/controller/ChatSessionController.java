package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.entity.ChatMessage;
import com.chengxun.gamemaker.web.entity.ChatSession;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.repository.ChatMessageRepository;
import com.chengxun.gamemaker.web.repository.ChatSessionRepository;
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
 * AI助手会话管理控制器
 * 提供会话的CRUD操作
 *
 * @author chengxun
 * @since 2.0.0
 */
@RestController
@RequestMapping("/api/chat/sessions")
@Tag(name = "AI助手会话", description = "AI助手会话管理接口")
@PreAuthorize("hasAuthority('PERM_ai:use')")
public class ChatSessionController {

    private static final Logger log = LoggerFactory.getLogger(ChatSessionController.class);

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final UserService userService;

    public ChatSessionController(ChatSessionRepository sessionRepository,
                                  ChatMessageRepository messageRepository,
                                  UserService userService) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.userService = userService;
    }

    /**
     * 获取用户的会话列表
     */
    @GetMapping
    @Operation(summary = "获取会话列表")
    public ResponseEntity<List<ChatSession>> listSessions(Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        List<ChatSession> sessions = sessionRepository.findByUserIdOrderByUpdatedAtDesc(user.getId());
        return ResponseEntity.ok(sessions);
    }

    /**
     * 创建新会话
     */
    @PostMapping
    @Operation(summary = "创建新会话")
    public ResponseEntity<ChatSession> createSession(@RequestBody(required = false) Map<String, String> request,
                                                      Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        ChatSession session = new ChatSession();
        session.setUserId(user.getId());
        if (request != null && request.containsKey("title")) {
            session.setTitle(request.get("title"));
        }

        ChatSession saved = sessionRepository.save(session);
        log.info("用户 {} 创建新会话: {}", user.getUsername(), saved.getId());
        return ResponseEntity.ok(saved);
    }

    /**
     * 获取会话详情（包含消息）
     */
    @GetMapping("/{sessionId}")
    @Operation(summary = "获取会话详情")
    public ResponseEntity<?> getSession(@PathVariable Long sessionId,
                                         Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        ChatSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null || !session.getUserId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }

        List<ChatMessage> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        // 返回包含消息的Map，避免JsonIgnore影响
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("id", session.getId());
        result.put("userId", session.getUserId());
        result.put("title", session.getTitle());
        result.put("createdAt", session.getCreatedAt());
        result.put("updatedAt", session.getUpdatedAt());
        result.put("messages", messages);
        return ResponseEntity.ok(result);
    }

    /**
     * 更新会话标题
     */
    @PutMapping("/{sessionId}")
    @Operation(summary = "更新会话标题")
    public ResponseEntity<?> updateSession(@PathVariable Long sessionId,
                                            @RequestBody Map<String, String> request,
                                            Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        ChatSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null || !session.getUserId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }

        if (request.containsKey("title")) {
            session.setTitle(request.get("title"));
        }

        ChatSession saved = sessionRepository.save(session);
        return ResponseEntity.ok(saved);
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/{sessionId}")
    @Operation(summary = "删除会话")
    public ResponseEntity<?> deleteSession(@PathVariable Long sessionId,
                                            Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        ChatSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null || !session.getUserId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }

        sessionRepository.delete(session);
        log.info("用户 {} 删除会话: {}", user.getUsername(), sessionId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * 添加消息到会话
     */
    @PostMapping("/{sessionId}/messages")
    @Operation(summary = "添加消息")
    public ResponseEntity<ChatMessage> addMessage(@PathVariable Long sessionId,
                                                    @RequestBody Map<String, String> request,
                                                    Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        ChatSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null || !session.getUserId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }

        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setRole(request.getOrDefault("role", "user"));
        message.setContent(request.get("content"));
        message.setThinking(request.get("thinking"));
        message.setToolUses(request.get("toolUses"));
        message.setTasks(request.get("tasks"));

        ChatMessage saved = messageRepository.save(message);

        // 更新会话时间
        session.preUpdate();
        sessionRepository.save(session);

        // 如果是用户的第一条消息，自动生成标题
        if ("user".equals(message.getRole()) && message.getContent() != null) {
            long messageCount = messageRepository.countBySessionId(sessionId);
            if (messageCount == 1) {
                String title = message.getContent().length() > 30
                    ? message.getContent().substring(0, 30) + "..."
                    : message.getContent();
                session.setTitle(title);
                sessionRepository.save(session);
            }
        }

        return ResponseEntity.ok(saved);
    }

    /**
     * 获取会话消息列表
     */
    @GetMapping("/{sessionId}/messages")
    @Operation(summary = "获取消息列表")
    public ResponseEntity<List<ChatMessage>> listMessages(@PathVariable Long sessionId,
                                                           Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        ChatSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null || !session.getUserId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }

        List<ChatMessage> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        return ResponseEntity.ok(messages);
    }
}
