package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.config.AppConfig;
import com.chengxun.gamemaker.engine.ClaudeCliEngine;
import com.chengxun.gamemaker.engine.StreamingClaudeCliEngine;
import com.chengxun.gamemaker.model.StreamEvent;
import com.chengxun.gamemaker.service.ProjectDiscussionService;
import com.chengxun.gamemaker.web.entity.ProjectDiscussion;
import com.chengxun.gamemaker.web.entity.ProjectDiscussionMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 项目讨论控制器
 * 提供项目讨论会话、消息、会议纪要生成等API
 *
 * @author chengxun
 * @since 2.0.0
 */
@RestController
@RequestMapping("/api/project-discussions")
public class ProjectDiscussionController {

    private static final Logger log = LoggerFactory.getLogger(ProjectDiscussionController.class);

    @Autowired
    private ProjectDiscussionService discussionService;

    @Autowired
    private StreamingClaudeCliEngine streamingEngine;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private com.chengxun.gamemaker.web.service.UserService userService;

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * 创建项目讨论会话
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public Map<String, Object> createDiscussion(@RequestBody Map<String, Object> body,
                                                 Authentication auth) {
        String projectId = (String) body.get("projectId");
        String title = (String) body.get("title");
        String username = auth.getName();

        // 获取用户ID
        Long userId = 0L;
        try {
            var user = userService.getUserByUsername(username);
            if (user != null) userId = user.getId();
        } catch (Exception e) {
            log.warn("无法获取用户ID: {}", e.getMessage());
        }

        ProjectDiscussion discussion = discussionService.createDiscussion(projectId, userId, username, title);
        return Map.of("success", true, "discussion", discussion);
    }

    /**
     * 获取项目的讨论列表
     */
    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public Map<String, Object> listDiscussions(@PathVariable String projectId) {
        List<ProjectDiscussion> discussions = discussionService.getDiscussionsByProject(projectId);
        return Map.of("success", true, "discussions", discussions, "total", discussions.size());
    }

    /**
     * 获取讨论详情（含消息）
     */
    @GetMapping("/{discussionId}")
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public Map<String, Object> getDiscussion(@PathVariable Long discussionId) {
        ProjectDiscussion discussion = discussionService.getDiscussion(discussionId);
        if (discussion == null) {
            return Map.of("success", false, "error", "讨论不存在");
        }
        List<ProjectDiscussionMessage> messages = discussionService.getMessages(discussionId);
        return Map.of("success", true, "discussion", discussion, "messages", messages);
    }

    /**
     * 删除讨论会话
     */
    @DeleteMapping("/{discussionId}")
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public Map<String, Object> deleteDiscussion(@PathVariable Long discussionId) {
        discussionService.deleteDiscussion(discussionId);
        return Map.of("success", true, "message", "讨论已删除");
    }

    /**
     * 流式发送消息（SSE）
     * 用户发送消息后，AI实时流式回复
     */
    @GetMapping(value = "/{discussionId}/stream/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAsk(@PathVariable Long discussionId,
                                @RequestParam String message,
                                Authentication auth) {
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        // 验证讨论存在
        ProjectDiscussion discussion = discussionService.getDiscussion(discussionId);
        if (discussion == null || !"ACTIVE".equals(discussion.getStatus())) {
            try {
                emitter.send(SseEmitter.event().name("error").data("讨论不存在或已结束"));
                emitter.complete();
            } catch (IOException e) { /* ignore */ }
            return emitter;
        }

        // 保存用户消息
        String username = auth.getName();
        discussionService.addUserMessage(discussionId, message, username);

        // 构建包含项目上下文的系统提示
        String systemPrompt = buildDiscussionSystemPrompt(discussion);

        // 获取历史消息构建上下文
        List<ProjectDiscussionMessage> history = discussionService.getMessages(discussionId);
        StringBuilder fullPrompt = new StringBuilder();
        fullPrompt.append(systemPrompt).append("\n\n");
        fullPrompt.append("## 对话历史\n\n");
        for (ProjectDiscussionMessage msg : history) {
            if ("user".equals(msg.getRole())) {
                fullPrompt.append("**用户**: ").append(msg.getContent()).append("\n\n");
            } else if ("assistant".equals(msg.getRole())) {
                fullPrompt.append("**AI**: ").append(msg.getContent()).append("\n\n");
            }
        }
        fullPrompt.append("**用户**: ").append(message).append("\n\n");
        fullPrompt.append("请回复用户的问题或建议。回复要简洁专业，聚焦项目方向讨论。");

        // 异步流式调用AI
        executorService.submit(() -> {
            StringBuilder responseBuffer = new StringBuilder();
            try {
                streamingEngine.sendMessageStreaming(
                    "discussion-" + discussionId,
                    null,
                    fullPrompt.toString(),
                    null,
                    appConfig.getApiKey(),
                    appConfig.getApiUrl(),
                    appConfig.getModel(),
                    null,
                    event -> handleStreamEvent(emitter, event, responseBuffer, discussionId)
                );
            } catch (Exception e) {
                log.error("流式对话失败: discussionId={}", discussionId, e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("AI响应失败: " + e.getMessage()));
                } catch (IOException ex) { /* ignore */ }
            } finally {
                // 保存AI回复
                if (responseBuffer.length() > 0) {
                    discussionService.addAssistantMessage(discussionId, responseBuffer.toString());
                }
                try {
                    emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                } catch (IOException e) { /* ignore */ }
                emitter.complete();
            }
        });

        return emitter;
    }

    /**
     * 处理流式事件
     */
    private void handleStreamEvent(SseEmitter emitter, StreamEvent event,
                                    StringBuilder responseBuffer, Long discussionId) {
        try {
            switch (event.getType()) {
                case "thinking":
                    emitter.send(SseEmitter.event().name("thinking").data(event.getContent()));
                    break;
                case "text":
                    responseBuffer.append(event.getContent());
                    emitter.send(SseEmitter.event().name("text").data(event.getContent()));
                    break;
                case "tool_use":
                    emitter.send(SseEmitter.event().name("tool_use").data(event.getContent()));
                    break;
                case "error":
                    emitter.send(SseEmitter.event().name("error").data(event.getContent()));
                    break;
            }
        } catch (IOException e) {
            log.debug("SSE发送失败（客户端可能已断开）: {}", e.getMessage());
        }
    }

    /**
     * 生成会议纪要
     */
    @PostMapping("/{discussionId}/generate-minutes")
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public Map<String, Object> generateMeetingMinutes(@PathVariable Long discussionId) {
        try {
            String minutes = discussionService.generateMeetingMinutes(discussionId);
            return Map.of("success", true, "minutes", minutes, "message", "会议纪要已生成并同步给制作人");
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * 获取项目的会议纪要列表
     */
    @GetMapping("/project/{projectId}/minutes")
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public Map<String, Object> getMinutes(@PathVariable String projectId) {
        List<ProjectDiscussion> discussions = discussionService.getMinutesByProject(projectId);
        List<Map<String, Object>> minutesList = new ArrayList<>();
        for (ProjectDiscussion d : discussions) {
            minutesList.add(Map.of(
                "id", d.getId(),
                "title", d.getTitle() != null ? d.getTitle() : "",
                "minutes", d.getMeetingMinutes() != null ? d.getMeetingMinutes() : "",
                "syncedToProducer", d.isSyncedToProducer(),
                "createdAt", d.getCreatedAt() != null ? d.getCreatedAt().toString() : ""
            ));
        }
        return Map.of("success", true, "minutes", minutesList, "total", minutesList.size());
    }

    /**
     * 构建项目讨论的系统提示
     * 包含项目上下文信息，让AI能够针对性地讨论项目方向
     */
    private String buildDiscussionSystemPrompt(ProjectDiscussion discussion) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是 ChengXun Game Maker 的项目分析顾问。\n\n");
        sb.append("## 你的角色\n");
        sb.append("你正在与用户讨论一个游戏项目的方向。你的职责是：\n");
        sb.append("1. 分析用户提出的项目方向建议\n");
        sb.append("2. 提供专业的游戏设计和开发建议\n");
        sb.append("3. 指出潜在的风险和挑战\n");
        sb.append("4. 帮助用户理清思路，做出更好的决策\n\n");

        sb.append("## 项目信息\n");
        sb.append("- 项目ID: ").append(discussion.getProjectId()).append("\n");
        sb.append("- 发起人: ").append(discussion.getUsername()).append("\n\n");

        sb.append("## 回复原则\n");
        sb.append("- 聚焦项目方向讨论，不做无关闲聊\n");
        sb.append("- 建议具体可行，避免空泛\n");
        sb.append("- 如果用户的想法有风险，坦诚指出并给出替代方案\n");
        sb.append("- 回复简洁专业，控制在300字以内\n\n");

        sb.append("## 关于会议纪要\n");
        sb.append("当用户结束讨论后，可以点击\"生成会议纪要\"按钮。");
        sb.append("纪要会自动同步给项目制作人Agent，作为项目方向调整的依据。");
        sb.append("因此你的建议应该具有可执行性，制作人能够据此采取行动。\n");

        return sb.toString();
    }
}
