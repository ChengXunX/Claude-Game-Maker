package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.engine.StreamingClaudeCliEngine;
import com.chengxun.gamemaker.model.StreamEvent;
import com.chengxun.gamemaker.service.AiAssistantService;
import com.chengxun.gamemaker.service.AiToolRegistry;
import com.chengxun.gamemaker.service.AiToolExecutor;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.RoleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 流式AI助手控制器
 * 使用SSE（Server-Sent Events）实时推送思考过程、工具调用、任务等
 *
 * 主要功能：
 * - 流式推送思考过程（thinking）
 * - 流式推送文本响应（text）
 * - 推送工具调用事件（tool_use）
 * - 推送任务事件（task）
 *
 * @author chengxun
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/ai-assistant")
@Tag(name = "流式AI助手", description = "流式AI助手接口，支持实时思考过程展示")
public class StreamingAiAssistantController {

    private static final Logger log = LoggerFactory.getLogger(StreamingAiAssistantController.class);

    @Autowired
    private StreamingClaudeCliEngine streamingEngine;

    @Autowired
    private AiAssistantService aiAssistantService;

    @Autowired
    private com.chengxun.gamemaker.manager.AgentManager agentManager;

    @Autowired
    private com.chengxun.gamemaker.manager.ProjectManager projectManager;

    @Autowired
    private AiToolRegistry toolRegistry;

    @Autowired
    private AiToolExecutor toolExecutor;

    @Autowired
    private RoleService roleService;

    /** 线程池用于异步处理 */
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * 获取用户权限列表
     */
    private Set<String> getUserPermissions(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());
    }

    /**
     * 构建系统上下文提示
     * 根据当前用户和系统状态生成上下文信息
     */
    private String buildSystemContext(String username, Authentication authentication) {
        StringBuilder ctx = new StringBuilder();
        ctx.append("你是 ChengXun Game Maker 的 AI 助手。这是一个 AI 驱动的游戏开发自动化管理系统。\n\n");

        // 系统能力说明
        ctx.append("## 你的能力\n");
        ctx.append("1. **项目管理**：查看、创建、管理游戏项目\n");
        ctx.append("2. **Agent 管理**：查看 Agent 状态、启动/停止 Agent、发送任务\n");
        ctx.append("3. **技能管理**：查看、创建、生成 Agent 技能\n");
        ctx.append("4. **MCP 服务管理**：查看、添加、测试 MCP 服务器\n");
        ctx.append("5. **游戏模板**：查看游戏模板、从模板创建项目\n");
        ctx.append("6. **能力管理**：查看、创建 Agent 能力\n");
        ctx.append("7. **监控告警**：查看系统监控数据和告警\n");
        ctx.append("8. **配置管理**：查看和修改系统配置\n");
        ctx.append("9. **数据分析**：分析 Agent 绩效、资源使用等\n\n");

        // 当前系统状态
        try {
            var agents = agentManager.getAllAgents();
            ctx.append("## 当前系统状态\n");
            ctx.append("- Agent 总数: ").append(agents.size()).append("\n");
            ctx.append("- 运行中: ").append(agents.stream().filter(a -> a.isAlive()).count()).append("\n");
            ctx.append("- 忙碌: ").append(agents.stream().filter(a -> a.isBusy()).count()).append("\n\n");

            var projects = projectManager.getAllProjects();
            ctx.append("- 项目总数: ").append(projects.size()).append("\n");
            for (var project : projects) {
                ctx.append("  - ").append(project.getName()).append(" (").append(project.getId()).append(")\n");
            }
            ctx.append("\n");
        } catch (Exception e) {
            // 忽略错误
        }

        // 添加工具说明（根据用户权限过滤）
        Set<String> permissions = getUserPermissions(authentication);
        ctx.append(toolRegistry.generateToolDescriptions(permissions));

        ctx.append("## 工具调用格式\n");
        ctx.append("当需要执行操作时，使用以下格式调用工具：\n");
        ctx.append("```tool_call\n{\"tool\": \"工具名\", \"params\": {\"参数名\": \"参数值\"}}\n```\n\n");
        ctx.append("工具调用结果会自动返回，你可以根据结果继续回复用户。\n\n");

        ctx.append("## 回复要求\n");
        ctx.append("- 使用中文回复\n");
        ctx.append("- 如果用户需要执行操作，使用工具调用而不是手动说明\n");
        ctx.append("- 如果用户需要数据，使用工具获取并分析\n");
        ctx.append("- 保持专业但友好的语气\n\n");

        return ctx.toString();
    }

    /**
     * 应用关闭时关闭线程池
     */
    @PreDestroy
    public void destroy() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Streaming AI assistant executor service shut down");
    }

    /**
     * 流式问答接口
     * 使用SSE实时推送思考过程、工具调用、文本响应
     *
     * @param question 问题内容
     * @param authentication 认证信息
     * @return SSE发射器
     */
    @GetMapping(value = "/stream/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式问答", description = "流式推送思考过程和响应内容")
    @PreAuthorize("hasAuthority('PERM_ai:use')")
    public SseEmitter streamAsk(@RequestParam String question,
                                 Authentication authentication) {
        log.info("流式问答请求 - 用户: {}, 问题: {}", authentication.getName(),
            question.length() > 50 ? question.substring(0, 50) + "..." : question);

        // 创建SSE发射器，超时时间5分钟
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        String userId = authentication.getName();

        // 构建带系统上下文的消息（包含工具说明）
        String systemContext = buildSystemContext(userId, authentication);
        String enrichedQuestion = systemContext + "用户问题：" + question;

        // 异步处理
        executorService.submit(() -> {
            try {
                // 发送开始事件
                sendEvent(emitter, "start", new StartEvent("processing", question));

                // 调用流式引擎
                streamingEngine.sendMessageStreaming(
                    "ai-assistant-" + userId,
                    null,
                    enrichedQuestion,
                    null, null, null, null,
                    event -> handleStreamEvent(emitter, event, userId, question)
                );

            } catch (Exception e) {
                log.error("流式问答处理失败", e);
                try {
                    sendEvent(emitter, "error", new ErrorEvent("处理失败: " + e.getMessage()));
                    emitter.completeWithError(e);
                } catch (IOException ex) {
                    log.error("发送错误事件失败", ex);
                }
            }
        });

        // 设置超时和完成回调
        emitter.onTimeout(() -> {
            log.warn("SSE超时");
            emitter.complete();
        });

        emitter.onCompletion(() -> {
            log.info("SSE完成");
        });

        return emitter;
    }

    /**
     * 流式发送消息给Agent
     *
     * @param agentId Agent ID
     * @param message 消息内容
     * @param authentication 认证信息
     * @return SSE发射器
     */
    @GetMapping(value = "/stream/agent/{agentId}/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式发送Agent消息", description = "流式推送Agent响应")
    @PreAuthorize("hasAuthority('PERM_agent:manage')")
    public SseEmitter streamAgentMessage(@PathVariable String agentId,
                                          @RequestParam String message,
                                          Authentication authentication) {
        log.info("流式Agent消息 - Agent: {}, 用户: {}", agentId, authentication.getName());

        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        executorService.submit(() -> {
            try {
                sendEvent(emitter, "start", new StartEvent("processing", message));

                streamingEngine.sendMessageStreaming(
                    agentId,
                    null,
                    message,
                    null, null, null, null,
                    event -> handleStreamEvent(emitter, event, null, null)
                );

            } catch (Exception e) {
                log.error("流式Agent消息处理失败", e);
                try {
                    sendEvent(emitter, "error", new ErrorEvent("处理失败: " + e.getMessage()));
                    emitter.completeWithError(e);
                } catch (IOException ex) {
                    // ignore
                }
            }
        });

        emitter.onTimeout(() -> emitter.complete());
        return emitter;
    }

    /** 工具调用正则表达式 */
    private static final Pattern TOOL_CALL_PATTERN = Pattern.compile("```tool_call\\s*\\n(\\{.*?\\})\\s*\\n```", Pattern.DOTALL);

    /** 缓存的文本内容，用于检测工具调用 */
    private final Map<String, StringBuilder> textBuffers = new HashMap<>();

    /**
     * 处理流式事件
     */
    private void handleStreamEvent(SseEmitter emitter, StreamEvent event,
                                   String userId, String question) {
        try {
            switch (event.getType()) {
                case "thinking":
                    sendEvent(emitter, "thinking", new ThinkingEvent(event.getContent()));
                    break;

                case "text":
                    String textContent = event.getContent();
                    // 检查是否包含工具调用
                    if (textContent != null && textContent.contains("```tool_call")) {
                        // 缓存文本，等待完整工具调用
                        textBuffers.computeIfAbsent(userId, k -> new StringBuilder()).append(textContent);
                        String buffered = textBuffers.get(userId).toString();

                        // 检查是否有完整的工具调用块
                        Matcher matcher = TOOL_CALL_PATTERN.matcher(buffered);
                        if (matcher.find()) {
                            String toolCallJson = matcher.group(1);
                            // 提取工具调用前的文本
                            String beforeText = buffered.substring(0, matcher.start());
                            if (!beforeText.trim().isEmpty()) {
                                sendEvent(emitter, "text", new TextEvent(beforeText));
                            }

                            // 发送工具调用到前端执行
                            sendToolCallToFrontend(emitter, toolCallJson);

                            // 清理缓冲区
                            textBuffers.remove(userId);
                        }
                        // 如果没有完整块，继续等待
                    } else {
                        sendEvent(emitter, "text", new TextEvent(textContent));
                    }
                    break;

                case "tool_use":
                    sendEvent(emitter, "tool_use", new ToolUseEvent(
                        event.getToolName(),
                        event.getToolInput()
                    ));
                    break;

                case "tool_result":
                    sendEvent(emitter, "tool_result", new ToolResultEvent(
                        event.getToolName(),
                        event.getToolResult()
                    ));
                    break;

                case "task":
                    sendEvent(emitter, "task", new TaskEvent(
                        event.getTaskId(),
                        event.getTaskTitle(),
                        event.getTaskStatus()
                    ));
                    break;

                case "complete":
                    // 保存对话历史
                    if (userId != null && question != null) {
                        aiAssistantService.saveConversation(userId, question, event.getContent(), null);
                    }
                    sendEvent(emitter, "complete", new CompleteEvent(event.getContent()));
                    emitter.complete();
                    break;

                case "error":
                    sendEvent(emitter, "error", new ErrorEvent(event.getErrorMessage()));
                    emitter.completeWithError(new RuntimeException(event.getErrorMessage()));
                    break;

                default:
                    log.debug("Unknown event type: {}", event.getType());
            }
        } catch (IOException e) {
            log.error("发送事件失败: {}", event.getType(), e);
        }
    }

    /**
     * 发送工具调用到前端（前端执行）
     */
    @SuppressWarnings("unchecked")
    private void sendToolCallToFrontend(SseEmitter emitter, String toolCallJson) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> toolCall = mapper.readValue(toolCallJson, Map.class);
            String toolName = (String) toolCall.get("tool");
            Map<String, Object> params = (Map<String, Object>) toolCall.getOrDefault("params", Map.of());

            log.info("发送工具调用到前端: {}", toolName);

            // 发送工具调用事件到前端，让前端执行
            sendEvent(emitter, "tool_call", new ToolCallEvent(toolName, mapper.writeValueAsString(params), toolCallJson));

        } catch (Exception e) {
            log.error("发送工具调用失败", e);
            try {
                sendEvent(emitter, "error", new ErrorEvent("工具调用发送失败: " + e.getMessage()));
            } catch (IOException ex) {
                log.error("发送错误事件失败", ex);
            }
        }
    }

    /**
     * 发送SSE事件
     */
    private void sendEvent(SseEmitter emitter, String eventName, Object data) throws IOException {
        emitter.send(SseEmitter.event()
            .name(eventName)
            .data(data));
    }

    // ===== 事件数据类 =====

    /** 开始事件 */
    private static class StartEvent {
        public final String status;
        public final String question;
        public final long timestamp;

        StartEvent(String status, String question) {
            this.status = status;
            this.question = question;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /** 思考事件 */
    private static class ThinkingEvent {
        public final String content;
        public final long timestamp;

        ThinkingEvent(String content) {
            this.content = content;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /** 文本事件 */
    private static class TextEvent {
        public final String content;
        public final long timestamp;

        TextEvent(String content) {
            this.content = content;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /** 工具调用事件 */
    private static class ToolUseEvent {
        public final String toolName;
        public final Object input;
        public final long timestamp;

        ToolUseEvent(String toolName, Object input) {
            this.toolName = toolName;
            this.input = input;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /** 工具调用事件（前端执行） */
    private static class ToolCallEvent {
        public final String toolName;
        public final String params;
        public final String rawJson;
        public final long timestamp;

        ToolCallEvent(String toolName, String params, String rawJson) {
            this.toolName = toolName;
            this.params = params;
            this.rawJson = rawJson;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /** 工具结果事件 */
    private static class ToolResultEvent {
        public final String toolName;
        public final String result;
        public final long timestamp;

        ToolResultEvent(String toolName, String result) {
            this.toolName = toolName;
            this.result = result;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /** 任务事件 */
    private static class TaskEvent {
        public final String taskId;
        public final String title;
        public final String status;
        public final long timestamp;

        TaskEvent(String taskId, String title, String status) {
            this.taskId = taskId;
            this.title = title;
            this.status = status;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /** 完成事件 */
    private static class CompleteEvent {
        public final String content;
        public final long timestamp;

        CompleteEvent(String content) {
            this.content = content;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /** 错误事件 */
    private static class ErrorEvent {
        public final String message;
        public final long timestamp;

        ErrorEvent(String message) {
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
