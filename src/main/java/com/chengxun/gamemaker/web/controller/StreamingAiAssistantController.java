package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.engine.StreamingClaudeCliEngine;
import com.chengxun.gamemaker.model.StreamEvent;
import com.chengxun.gamemaker.service.AiAssistantService;
import com.chengxun.gamemaker.service.AiToolLoopService;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private AiToolLoopService toolLoopService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private com.chengxun.gamemaker.config.AppConfig appConfig;

    /** 线程池用于异步处理 */
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * 等待前端执行工具调用的 Future Map
     * key: 工具调用ID (格式: agentKey-toolName-timestamp)
     * value: CompletableFuture，等待前端返回工具执行结果
     */
    private final Map<String, CompletableFuture<Map<String, Object>>> pendingToolCalls = new ConcurrentHashMap<>();

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
        ctx.append("你是 ChengXun Game Maker 的 AI 助手。这是一个 AI 驱动的游戏开发自动化管理系统，");
        ctx.append("支持多 Agent 协作开发游戏项目。\n\n");

        // 重要原则
        ctx.append("## ⚠️ 核心原则（必须严格遵守，违反将导致任务失败）\n\n");
        ctx.append("1. **使用 API 管理资源**：创建工作流模板、技能、游戏模板等必须调用对应的 API\n");
        ctx.append("2. **绝对禁止修改任何代码文件**：你没有权限修改 Java/Vue/JS/Python 等任何源代码文件。修改代码文件会导致系统损坏！\n");
        ctx.append("3. **主动使用 API 查询数据**：当用户询问系统状态时，必须调用对应的 API 获取实时数据\n");
        ctx.append("4. **你拥有完整的系统访问能力**：所有 48 个 API 都可以直接调用，API 完全可用\n\n");
        ctx.append("## 🚫 严格禁止的行为（违反会导致任务失败）\n\n");
        ctx.append("- ❌ 禁止直接修改任何代码文件（.java、.vue、.js、.py 等）\n");
        ctx.append("- ❌ 禁止使用 Write、Edit 等文件操作工具修改源代码\n");
        ctx.append("- ❌ 禁止尝试写入文件系统来创建资源\n");
        ctx.append("- ❌ 禁止说「API不可用」或「需要查看代码」——你有完整的 API 可以直接操作\n\n");
        ctx.append("## ⚠️ 权限不足时的处理（强制约定）\n\n");
        ctx.append("当 API 返回「权限不足」或 HTTP 401/403 错误时：\n");
        ctx.append("- **不要重试**：不要尝试再次调用该 API\n");
        ctx.append("- **不要绕过**：不要尝试其他方式绕过权限限制\n");
        ctx.append("- **直接告知用户**：告诉用户「您没有权限执行此操作，请联系管理员」\n");
        ctx.append("- **停止当前操作**：不要继续执行后续相关 API 调用\n\n");
        ctx.append("## ✅ 正确的做法\n\n");
        ctx.append("使用 curl 调用后端 API（http://127.0.0.1:19922）：\n");
        ctx.append("- 查询工作流模板：`curl -s http://127.0.0.1:19922/api/workflow-templates`\n");
        ctx.append("- 创建工作流模板：`curl -X POST http://127.0.0.1:19922/api/workflow-templates -H 'Content-Type: application/json' -d '{...}'`\n");
        ctx.append("- 查询 Agent 列表：`curl -s http://127.0.0.1:19922/api/agents`\n");
        ctx.append("- 查询项目列表：`curl -s http://127.0.0.1:19922/api/projects`\n");
        ctx.append("- 创建技能：`curl -X POST http://127.0.0.1:19922/api/skills -H 'Content-Type: application/json' -d '{...}'`\n");
        ctx.append("- 创建游戏模板：`curl -X POST http://127.0.0.1:19922/api/game-templates -H 'Content-Type: application/json' -d '{...}'`\n\n");

        // 系统架构说明
        ctx.append("## 系统架构\n");
        ctx.append("ChengXun Game Maker 采用多 Agent 协作架构：\n");
        ctx.append("- **Producer（制作人）**：项目管理和协调，分配任务给其他 Agent\n");
        ctx.append("- **ServerDev（服务端开发）**：后端逻辑实现\n");
        ctx.append("- **ClientDev（客户端开发）**：前端逻辑实现\n");
        ctx.append("- **SystemPlanner（系统策划）**：系统设计和文档\n");
        ctx.append("- **NumericalPlanner（数值策划）**：数值平衡设计\n");
        ctx.append("- **GitCommit（Git专员）**：版本管理\n\n");

        ctx.append("核心流程：项目创建 → 工作流模板选择 → Agent 自动协作 → 代码审查 → 部署\n\n");

        // 系统能力说明
        ctx.append("## 你的能力（通过 API 实现）\n\n");
        ctx.append("你拥有完整的系统访问能力，所有 API 都可以直接调用，**不需要查看代码文件**。\n");
        ctx.append("当用户询问系统状态、数据、配置等信息时，直接调用对应 API 获取实时数据。\n\n");
        ctx.append("### 项目管理\n");
        ctx.append("- 查询项目：`curl -s http://127.0.0.1:19922/api/projects`\n");
        ctx.append("- 创建项目：`curl -X POST http://127.0.0.1:19922/api/projects -H 'Content-Type: application/json' -d '{...}'`\n\n");
        ctx.append("### 工作流管理\n");
        ctx.append("- 查询工作流模板：`curl -s http://127.0.0.1:19922/api/workflow-templates`\n");
        ctx.append("- 创建工作流模板：`curl -X POST http://127.0.0.1:19922/api/workflow-templates -H 'Content-Type: application/json' -d '{...}'`\n");
        ctx.append("- 删除工作流模板：`curl -X DELETE http://127.0.0.1:19922/api/workflow-templates/{id}`\n");
        ctx.append("- 启动工作流：`curl -X POST http://127.0.0.1:19922/api/workflows/start -H 'Content-Type: application/json' -d '{...}'`\n");
        ctx.append("- 查询工作流实例：`curl -s http://127.0.0.1:19922/api/workflows/instances`\n\n");
        ctx.append("### Agent 管理\n");
        ctx.append("- 查询 Agent 列表：`curl -s http://127.0.0.1:19922/api/agents`\n");
        ctx.append("- 发送 Agent 任务：`curl -X POST http://127.0.0.1:19922/api/agents/{id}/task -H 'Content-Type: application/json' -d '{...}'`\n");
        ctx.append("- 干预 Agent：`curl -X POST http://127.0.0.1:19922/api/agents/{id}/intervene -H 'Content-Type: application/json' -d '{...}'`\n");
        ctx.append("- 暂停 Agent：`curl -X POST http://127.0.0.1:19922/api/agents/{id}/pause`\n");
        ctx.append("- 恢复 Agent：`curl -X POST http://127.0.0.1:19922/api/agents/{id}/resume`\n");
        ctx.append("- `get_agent_health`：查看 Agent 健康状态\n");
        ctx.append("- `get_agent_logs`：查看 Agent 运行日志\n\n");
        ctx.append("### 其他工具\n");
        ctx.append("- 游戏模板：`list_game_templates` / `create_game_template` / `delete_game_template`\n");
        ctx.append("- 技能管理：`list_skills` / `create_skill`\n");
        ctx.append("- 系统监控：`list_alerts` / `acknowledge_alert` / `resolve_alert`\n");
        ctx.append("- 资源监控：`get_resource_usage` / `get_system_info` / `get_diagnostic`\n");
        ctx.append("- 配置管理：`list_configs` / `update_config`\n");
        ctx.append("- Token管理：`list_tokens`\n");
        ctx.append("- 用户管理：`list_users` / `list_roles`\n");
        ctx.append("- CI/CD：`list_pipelines` / `trigger_pipeline`\n");
        ctx.append("- 代码审查：`list_reviews`\n");
        ctx.append("- 通用API调用：`call_api`（可调用任意系统API端点）\n\n");

        // 当前系统状态
        try {
            var agents = agentManager.getAllAgents();
            ctx.append("## 当前系统状态\n");
            ctx.append("- Agent 总数: ").append(agents.size()).append("\n");
            ctx.append("- 运行中: ").append(agents.stream().filter(a -> a.isAlive()).count()).append("\n");
            ctx.append("- 忙碌: ").append(agents.stream().filter(a -> a.isBusy()).count()).append("\n");
            for (var agent : agents) {
                ctx.append("  - ").append(agent.getName()).append(" (").append(agent.getRole()).append(")");
                ctx.append(" 状态:").append(agent.isAlive() ? "运行中" : "已停止");
                ctx.append(agent.isBusy() ? " [忙碌]" : "").append("\n");
            }
            ctx.append("\n");

            var projects = projectManager.getAllProjects();
            ctx.append("- 项目总数: ").append(projects.size()).append("\n");
            for (var project : projects) {
                ctx.append("  - ").append(project.getName()).append(" (").append(project.getId()).append(")");
                ctx.append(" 状态:").append(project.getStatus()).append("\n");
            }
            ctx.append("\n");
        } catch (Exception e) {
            // 忽略错误
        }

        // 添加工具说明（根据用户权限过滤）
        Set<String> permissions = getUserPermissions(authentication);
        ctx.append(toolRegistry.generateToolDescriptions(permissions));

        ctx.append("## 工具调用规则（必须严格遵守）\n");
        ctx.append("1. **必须使用以下格式调用工具**（不要使用任何其他格式）：\n");
        ctx.append("```tool_call\n{\"tool\": \"工具名\", \"params\": {\"参数名\": \"参数值\"}}\n```\n");
        ctx.append("2. **示例 - 创建工作流模板**：\n");
        ctx.append("```tool_call\n{\"tool\": \"create_workflow_template\", \"params\": {\"id\": \"my-workflow\", \"name\": \"我的工作流\", \"description\": \"描述\"}}\n```\n");
        ctx.append("3. **示例 - 查询工作流模板**：\n");
        ctx.append("```tool_call\n{\"tool\": \"list_workflow_templates\", \"params\": {}}\n```\n");
        ctx.append("4. 你可以在一次回复中调用多个工具（每个用单独的 tool_call 块）\n");
        ctx.append("5. 工具执行结果会自动返回，你可以根据结果继续回复用户\n");
        ctx.append("6. 如果工具执行失败，分析错误原因并尝试修复或告知用户\n");
        ctx.append("7. **绝对不要尝试修改代码文件来创建资源**，必须使用对应的工具\n\n");

        ctx.append("## 回复要求\n");
        ctx.append("- 使用中文回复\n");
        ctx.append("- 如果用户需要执行操作（如创建工作流、添加模板等），**必须使用 tool_call 调用对应工具**\n");
        ctx.append("- 如果用户需要数据（如工作流列表、Agent状态、项目信息等），**必须使用工具获取实时数据**\n");
        ctx.append("- **绝对不要说API不可访问或需要查看代码**——你有完整的工具可以直接查询系统数据\n");
        ctx.append("- **绝对不要尝试修改代码文件**——所有资源管理都通过API工具完成\n");
        ctx.append("- 工具执行完成后，告知用户执行结果\n");
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
                                 @RequestParam(required = false) String sessionId,
                                 Authentication authentication,
                                 jakarta.servlet.http.HttpServletRequest request,
                                 jakarta.servlet.http.HttpServletResponse response) {
        log.info("流式问答请求 - 用户: {}, sessionId: {}, 问题: {}", authentication.getName(), sessionId,
            question.length() > 50 ? question.substring(0, 50) + "..." : question);

        // 禁用Nginx缓冲，确保SSE事件实时推送
        response.setHeader("X-Accel-Buffering", "no");
        // 禁用浏览器缓存
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("Connection", "keep-alive");

        // 创建SSE发射器，超时时间5分钟
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        String userId = authentication.getName();

        // 获取用户的 JWT Token，用于后端 API 调用时的身份传递
        String userToken = null;
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            userToken = authHeader.substring(7);
        } else {
            // 尝试从 URL 参数获取 token（支持 SSE）
            userToken = request.getParameter("token");
        }
        final String finalUserToken = userToken;

        // 使用sessionId作为会话标识，确保同一会话的上下文连贯
        String agentKey = sessionId != null ? "ai-assistant-" + sessionId : "ai-assistant-" + userId;

        // 从数据库获取该会话的 Claude sessionId（用于 --resume 恢复上下文）
        String claudeSessionId = null;
        boolean needsContextSummary = false;
        String sessionSummary = null;
        if (sessionId != null) {
            var resumeInfo = aiAssistantService.getClaudeSessionId(sessionId);
            claudeSessionId = resumeInfo.getSessionId();
            needsContextSummary = resumeInfo.isNeedsReset();
            sessionSummary = resumeInfo.getSummary();
            if (claudeSessionId != null) {
                log.info("恢复会话上下文 - sessionId: {}, claudeSessionId: {}", sessionId, claudeSessionId);
            }
            if (needsContextSummary) {
                log.info("会话上下文已重置，将注入摘要 - sessionId: {}", sessionId);
            }
        }

        // 构建带系统上下文的消息（包含工具说明）
        // 恢复会话时不需要重新发送系统上下文（已在 Claude 会话中）
        // 但 session 重置时需要注入摘要让 AI 知道之前在做什么
        String enrichedQuestion;
        if (claudeSessionId != null) {
            // 正常恢复，不发送系统上下文
            enrichedQuestion = question;
            log.info("恢复会话，跳过系统上下文 - sessionId: {}", sessionId);
        } else if (needsContextSummary && sessionSummary != null) {
            // session 重置，发送系统上下文 + 会话摘要
            String systemContext = buildSystemContext(userId, authentication);
            enrichedQuestion = systemContext + sessionSummary + "\n\n用户问题：" + question;
            log.info("session 重置，注入摘要 - sessionId: {}", sessionId);
        } else {
            // 全新会话，发送完整系统上下文
            String systemContext = buildSystemContext(userId, authentication);
            enrichedQuestion = systemContext + "用户问题：" + question;
        }

        // 前端断开标记：当 SSE 连接关闭时设置为 true，终止工具循环
        AtomicBoolean isCancelled = new AtomicBoolean(false);

        // 获取用户权限
        Set<String> permissions = getUserPermissions(authentication);

        // 使用 final 变量以便在 lambda 中引用
        final String finalClaudeSessionId = claudeSessionId;

        // 异步处理
        executorService.submit(() -> {
            try {
                // 发送开始事件
                sendEvent(emitter, "start", new StartEvent("processing", question));

                // 使用工具循环服务（支持多步工具调用）
                String returnedSessionId = toolLoopService.executeWithToolLoop(
                    agentKey,
                    finalClaudeSessionId,
                    enrichedQuestion,
                    userId,
                    finalUserToken,
                    permissions,
                    isCancelled,
                    event -> {
                        try {
                            handleStreamEvent(emitter, event, userId, question, sessionId);
                        } catch (Exception e) {
                            // 发送事件失败时标记取消
                            isCancelled.set(true);
                        }
                    }
                );

                // 保存 Claude sessionId 到数据库，用于后续对话恢复上下文
                if (returnedSessionId != null && sessionId != null) {
                    aiAssistantService.saveClaudeSessionId(sessionId, returnedSessionId);
                    // 保存当前使用的模型
                    aiAssistantService.setSessionModel(sessionId, appConfig.getModel());
                    // 累加上下文大小，用于判断何时需要重置
                    aiAssistantService.addContextSize(sessionId, enrichedQuestion.length());
                    log.info("保存会话上下文 - sessionId: {}, claudeSessionId: {}, contextSize: {}, model: {}",
                        sessionId, returnedSessionId, enrichedQuestion.length(), appConfig.getModel());
                }

            } catch (Exception e) {
                log.error("流式问答处理失败", e);
                isCancelled.set(true);
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
            log.warn("SSE超时 - 用户: {}", userId);
            isCancelled.set(true);
            completedEmitters.add(emitter);
            emitter.complete();
        });

        emitter.onCompletion(() -> {
            log.debug("SSE完成 - 用户: {}", userId);
            isCancelled.set(true);
            completedEmitters.add(emitter);
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
                                          Authentication authentication,
                                          jakarta.servlet.http.HttpServletResponse response) {
        log.info("流式Agent消息 - Agent: {}, 用户: {}", agentId, authentication.getName());

        // 禁用Nginx缓冲，确保SSE事件实时推送
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache, no-transform");

        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        executorService.submit(() -> {
            try {
                sendEvent(emitter, "start", new StartEvent("processing", message));

                streamingEngine.sendMessageStreaming(
                    agentId,
                    null,
                    message,
                    null, null, null, null, null,
                    event -> handleStreamEvent(emitter, event, null, null, null)
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

    /**
     * 接收前端返回的工具执行结果
     * 前端执行工具调用后，通过此端点返回结果给后端
     *
     * @param toolCallId 工具调用ID
     * @param result 工具执行结果
     * @return 操作状态
     */
    @PostMapping("/tool-result/{toolCallId}")
    @Operation(summary = "接收工具执行结果", description = "前端执行工具后返回结果")
    @PreAuthorize("hasAuthority('PERM_ai:use')")
    public Map<String, Object> receiveToolResult(@PathVariable String toolCallId,
                                                  @RequestBody Map<String, Object> result) {
        log.info("收到前端工具执行结果 - toolCallId: {}, success: {}", toolCallId, result.get("success"));

        CompletableFuture<Map<String, Object>> future = pendingToolCalls.remove(toolCallId);
        if (future != null) {
            future.complete(result);
            return Map.of("success", true, "message", "结果已接收");
        } else {
            log.warn("未找到等待中的工具调用 - toolCallId: {}", toolCallId);
            return Map.of("success", false, "message", "工具调用已超时或不存在");
        }
    }

    /**
     * 获取等待中的工具调用 Future
     * 供 AiToolLoopService 使用
     */
    public CompletableFuture<Map<String, Object>> getOrCreateToolCallFuture(String toolCallId) {
        return pendingToolCalls.computeIfAbsent(toolCallId, k -> new CompletableFuture<>());
    }

    /**
     * 移除等待中的工具调用
     */
    public void removeToolCallFuture(String toolCallId) {
        pendingToolCalls.remove(toolCallId);
    }

    /**
     * 处理流式事件
     */
    private void handleStreamEvent(SseEmitter emitter, StreamEvent event,
                                   String userId, String question, String chatSessionId) {
        try {
            switch (event.getType()) {
                case "thinking":
                    sendEvent(emitter, "thinking", new ThinkingEvent(event.getContent()));
                    break;

                case "text":
                    // 直接转发文本给前端（tool_call 格式由 AiToolLoopService 处理）
                    sendEvent(emitter, "text", new TextEvent(event.getContent()));
                    break;

                case "tool_use":
                    sendEvent(emitter, "tool_use", new ToolUseEvent(
                        event.getToolName(),
                        event.getToolInput()
                    ));
                    break;

                case "tool_executing":
                    sendEvent(emitter, "tool_executing", new ToolExecutingEvent(
                        event.getToolName(),
                        event.getContent()
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
                    // 更新会话主题摘要（用于 session 重置后恢复上下文）
                    if (chatSessionId != null) {
                        aiAssistantService.updateSessionTopic(chatSessionId, question, event.getContent());
                    }
                    sendEvent(emitter, "complete", new CompleteEvent(event.getContent()));
                    try {
                        emitter.complete();
                    } catch (IllegalStateException ex) {
                        log.debug("SSE发射器已完成，忽略complete: {}", ex.getMessage());
                    }
                    completedEmitters.add(emitter);
                    break;

                case "error":
                    sendEvent(emitter, "error", new ErrorEvent(event.getErrorMessage()));
                    try {
                        emitter.completeWithError(new RuntimeException(event.getErrorMessage()));
                    } catch (IllegalStateException ex) {
                        log.debug("SSE发射器已完成，忽略error: {}", ex.getMessage());
                    }
                    completedEmitters.add(emitter);
                    break;

                default:
                    log.debug("Unknown event type: {}", event.getType());
            }
        } catch (IOException e) {
            if (!completedEmitters.contains(emitter)) {
                log.warn("发送事件失败 (客户端可能已断开): {}", event.getType());
            }
        }
    }

    /** 已完成的发射器标记，避免重复发送事件 */
    private final java.util.Set<SseEmitter> completedEmitters =
        java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    /**
     * 发送SSE事件
     * 如果发射器已完成（客户端断开），静默忽略而非抛出异常
     */
    private void sendEvent(SseEmitter emitter, String eventName, Object data) throws IOException {
        if (completedEmitters.contains(emitter)) {
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                .name(eventName)
                .data(data));
        } catch (IllegalStateException e) {
            // 发射器已完成，标记并忽略
            completedEmitters.add(emitter);
            log.debug("SSE发射器已完成，忽略事件: {}", eventName);
        }
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

    /** 工具执行中事件 */
    private static class ToolExecutingEvent {
        public final String toolName;
        public final String content;
        public final long timestamp;

        ToolExecutingEvent(String toolName, String content) {
            this.toolName = toolName;
            this.content = content;
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
