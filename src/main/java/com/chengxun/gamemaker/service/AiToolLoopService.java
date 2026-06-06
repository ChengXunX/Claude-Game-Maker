package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.engine.StreamingClaudeCliEngine;
import com.chengxun.gamemaker.model.AiTool;
import com.chengxun.gamemaker.model.StreamEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 工具执行循环服务
 * 实现 AI 多步工具调用链：AI 输出 tool_call → 后端执行 → 结果回传给 AI → AI 继续推理
 *
 * 安全保障：
 * - 最大迭代轮次限制（防止无限循环）
 * - 前端断开检测（通过 AtomicBoolean 标记）
 * - 单轮超时保护（5 分钟）
 * - 工具执行权限检查
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class AiToolLoopService {

    private static final Logger log = LoggerFactory.getLogger(AiToolLoopService.class);

    /** 最大工具调用轮次，防止无限循环 */
    private static final int MAX_TOOL_ITERATIONS = 8;

    /** 单轮 AI 响应超时（秒） */
    private static final int ITERATION_TIMEOUT_SECONDS = 300;

    /** 工具调用正则：匹配 ```tool_call\n{...}\n``` */
    private static final Pattern TOOL_CALL_PATTERN =
        Pattern.compile("```tool_call\\s*\\n(\\{.*?\\})\\s*\\n```", Pattern.DOTALL);

    private final StreamingClaudeCliEngine streamingEngine;
    private final AiToolExecutor toolExecutor;
    private final AiToolRegistry toolRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiToolLoopService(StreamingClaudeCliEngine streamingEngine,
                             AiToolExecutor toolExecutor,
                             AiToolRegistry toolRegistry) {
        this.streamingEngine = streamingEngine;
        this.toolExecutor = toolExecutor;
        this.toolRegistry = toolRegistry;
    }

    /**
     * 执行带工具循环的 AI 对话（同步阻塞）
     *
     * 流程：
     * 1. 发送消息给 AI（同步等待完成）
     * 2. 检测是否包含 tool_call
     * 3. 如果有：执行工具 → 将结果作为新消息发送给 AI → 回到步骤 1
     * 4. 如果没有：输出最终文本，结束
     *
     * @param agentKey Agent 标识
     * @param sessionId 会话ID（用于 --resume 恢复上下文，首次调用传 null）
     * @param message 消息内容
     * @param username 用户名
     * @param userToken 用户的 JWT Token，用于后端 API 调用时的身份传递
     * @param userPermissions 用户权限
     * @param isCancelled 取消标记
     * @param onEvent 事件回调
     * @return 会话ID（用于后续调用恢复上下文）
     */
    public String executeWithToolLoop(String agentKey, String sessionId, String message, String username,
                                     String userToken, Set<String> userPermissions, AtomicBoolean isCancelled,
                                     Consumer<StreamEvent> onEvent) {
        log.info("开始工具循环 - agent: {}, sessionId: {}, 用户: {}", agentKey, sessionId, username);

        String currentMessage = message;
        String currentSessionId = sessionId;
        int iteration = 0;

        while (iteration < MAX_TOOL_ITERATIONS) {
            if (isCancelled.get()) {
                log.info("前端已断开，终止工具循环 - agent: {}, 轮次: {}", agentKey, iteration);
                return currentSessionId;
            }

            iteration++;
            log.info("工具循环轮次 {}/{} - agent: {}, sessionId: {}", iteration, MAX_TOOL_ITERATIONS, agentKey, currentSessionId);

            // 本轮收集器
            StringBuilder textOutput = new StringBuilder();
            StringBuilder thinkingOutput = new StringBuilder();
            StringBuilder finalResultContent = new StringBuilder();
            List<ToolCallInfo> toolCalls = new ArrayList<>();
            AtomicBoolean hasError = new AtomicBoolean(false);
            AtomicBoolean nativeToolUseSeen = new AtomicBoolean(false);
            // 收集 Claude 原生工具事件（tool_use / tool_result），由主循环统一转发给前端
            List<Object> nativeToolEvents = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(1);

            // 调用 AI 引擎（异步，通过 latch 等待完成）
            // 工具循环内部不使用 --resume，通过消息内容传递上下文
            streamingEngine.sendMessageStreaming(
                agentKey,
                null,
                currentMessage,
                null, null, null, null,
                userToken,
                event -> handleIterationEvent(event, textOutput, thinkingOutput, finalResultContent, toolCalls,
                                               hasError, isCancelled, nativeToolUseSeen, nativeToolEvents, onEvent, latch)
            );

            // 阻塞等待 AI 响应完成
            try {
                boolean completed = latch.await(ITERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!completed) {
                    log.warn("AI 响应超时 ({}秒) - agent: {}, 轮次: {}", ITERATION_TIMEOUT_SECONDS, agentKey, iteration);
                    onEvent.accept(StreamEvent.error("AI 响应超时，请重试"));
                    return currentSessionId;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("工具循环被中断 - agent: {}", agentKey);
                return currentSessionId;
            }

            // 检查是否出错
            if (hasError.get()) {
                log.warn("AI 引擎出错，终止循环 - agent: {}, 轮次: {}", agentKey, iteration);
                return currentSessionId;
            }

            // 检查前端是否已断开
            if (isCancelled.get()) {
                log.info("前端已断开，终止工具循环 - agent: {}, 轮次: {}", agentKey, iteration);
                return currentSessionId;
            }

            // 捕获引擎返回的 sessionId（用于后续恢复上下文）
            String engineSessionId = streamingEngine.getSessionId(agentKey);
            if (engineSessionId != null) {
                currentSessionId = engineSessionId;
            }

            // 如果有原生工具调用（Claude CLI 自己执行的工具），直接返回最终结果
            if (nativeToolUseSeen.get()) {
                String resultText = finalResultContent.toString().trim();
                if (resultText.isEmpty()) {
                    resultText = textOutput.toString().trim();
                }
                if (!resultText.isEmpty()) {
                    onEvent.accept(StreamEvent.complete(resultText));
                } else {
                    onEvent.accept(StreamEvent.complete("工具执行完成"));
                }
                log.info("工具循环结束（原生工具调用）- agent: {}, 轮次: {}, sessionId: {}", agentKey, iteration, currentSessionId);
                return currentSessionId;
            }

            // 如果没有工具调用，输出最终结果并结束
            if (toolCalls.isEmpty()) {
                // 优先使用引擎返回的最终结果，其次使用累积的文本
                String resultText = finalResultContent.toString().trim();
                if (resultText.isEmpty()) {
                    resultText = textOutput.toString().trim();
                }
                if (!resultText.isEmpty()) {
                    onEvent.accept(StreamEvent.complete(resultText));
                } else {
                    onEvent.accept(StreamEvent.complete("AI 未返回有效内容"));
                }
                log.info("工具循环结束（无工具调用）- agent: {}, 轮次: {}, sessionId: {}", agentKey, iteration, currentSessionId);
                return currentSessionId;
            }

            // 有工具调用：执行工具并收集结果
            log.info("检测到 {} 个工具调用 - agent: {}, 轮次: {}", toolCalls.size(), agentKey, iteration);

            // 发送工具调用前的文本
            String textBeforeTools = extractTextBeforeToolCall(textOutput.toString());
            if (!textBeforeTools.isEmpty()) {
                onEvent.accept(StreamEvent.text(textBeforeTools));
            }

            StringBuilder toolResults = new StringBuilder();
            toolResults.append("以下是工具执行结果，请根据结果继续回复用户：\n\n");

            // 标记是否有权限不足的情况
            boolean hasPermissionError = false;

            for (ToolCallInfo toolCall : toolCalls) {
                if (isCancelled.get()) {
                    log.info("前端已断开，终止工具循环 - agent: {}, 轮次: {}", agentKey, iteration);
                    return currentSessionId;
                }

                // 通知前端工具开始执行
                onEvent.accept(StreamEvent.toolUse(toolCall.name, toolCall.params));

                // 检查权限
                AiTool tool = toolRegistry.getTool(toolCall.name);
                if (tool != null && tool.getPermission() != null
                    && !userPermissions.contains(tool.getPermission())) {
                    String errorResult = "权限不足: 需要 " + tool.getPermission();
                    toolResults.append("### 工具: ").append(toolCall.name).append("\n");
                    toolResults.append("结果: ").append(errorResult).append("\n\n");
                    onEvent.accept(StreamEvent.toolResult(toolCall.name, errorResult));
                    hasPermissionError = true;
                    // 权限不足时直接返回错误，不继续执行其他工具
                    onEvent.accept(StreamEvent.complete("权限不足，无法执行操作: " + toolCall.name));
                    return currentSessionId;
                }

                // 通知前端工具正在执行
                onEvent.accept(StreamEvent.toolExecuting(toolCall.name, "正在执行 " + getToolDisplayName(toolCall.name) + "..."));

                // 执行工具
                long startTime = System.currentTimeMillis();
                Map<String, Object> result;
                try {
                    result = toolExecutor.executeTool(toolCall.name, toolCall.params, username, userToken);
                } catch (Exception e) {
                    log.error("工具执行异常: {}", toolCall.name, e);
                    result = Map.of("success", false, "error", "执行异常: " + e.getMessage());
                }
                long duration = System.currentTimeMillis() - startTime;

                String resultJson;
                try {
                    resultJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
                } catch (Exception e) {
                    resultJson = result.toString();
                }

                toolResults.append("### 工具: ").append(toolCall.name).append("\n");
                toolResults.append("耗时: ").append(duration).append("ms\n");
                toolResults.append("结果:\n```json\n").append(resultJson).append("\n```\n\n");

                // 通知前端工具执行完成
                boolean success = result.containsKey("success") ? (boolean) result.get("success") : true;
                String statusMsg = success ? "执行成功" : "执行失败: " + result.getOrDefault("error", "未知错误");
                onEvent.accept(StreamEvent.toolResult(toolCall.name, resultJson));
                log.info("工具执行完成: {} ({}ms, success={}) - 轮次: {}", toolCall.name, duration, success, iteration);
            }

            // 如果有权限不足的情况，直接返回，不继续循环
            if (hasPermissionError) {
                return currentSessionId;
            }

            // 构建下一轮消息
            currentMessage = toolResults.toString();
        }

        log.warn("达到最大工具调用轮次 ({}) - agent: {}", MAX_TOOL_ITERATIONS, agentKey);
        onEvent.accept(StreamEvent.complete("已达到最大工具调用轮次，请简化问题后重试。"));
        return currentSessionId;
    }

    /**
     * 处理单轮迭代中的流式事件
     * 收集数据并实时转发给前端显示
     *
     * 当 Claude CLI 使用原生工具执行时，tool_use 和 tool_result 事件
     * 会实时转发给前端，确保工具调用过程可见
     */
    private void handleIterationEvent(StreamEvent event,
                                       StringBuilder textOutput,
                                       StringBuilder thinkingOutput,
                                       StringBuilder finalResultContent,
                                       List<ToolCallInfo> toolCalls,
                                       AtomicBoolean hasError,
                                       AtomicBoolean isCancelled,
                                       AtomicBoolean nativeToolUseSeen,
                                       List<Object> nativeToolEvents,
                                       Consumer<StreamEvent> onEvent,
                                       CountDownLatch latch) {
        if (isCancelled.get()) {
            latch.countDown();
            return;
        }

        switch (event.getType()) {
            case "thinking":
                thinkingOutput.append(event.getContent());
                // 实时转发思考过程给前端
                onEvent.accept(event);
                break;

            case "text":
                textOutput.append(event.getContent());
                // 实时转发文本给前端
                onEvent.accept(event);
                break;

            case "tool_use":
                // Claude 原生工具调用：标记并实时转发给前端
                nativeToolUseSeen.set(true);
                nativeToolEvents.add(event);
                // 实时发送 tool_use 事件给前端（显示"准备中"）
                onEvent.accept(StreamEvent.toolUse(event.getToolName(), event.getToolInput()));
                // 立即发送 tool_executing 事件（显示"执行中"）
                onEvent.accept(StreamEvent.toolExecuting(event.getToolName(),
                    "正在执行 " + getToolDisplayName(event.getToolName()) + "..."));
                log.info("实时转发工具调用: {}", event.getToolName());
                break;

            case "tool_result":
                // Claude 原生工具结果：实时转发给前端
                nativeToolEvents.add(event);
                // 实时发送 tool_result 事件给前端（显示"成功"/"失败"）
                onEvent.accept(StreamEvent.toolResult(event.getToolName(), event.getToolResult()));
                log.info("实时转发工具结果: {}", event.getToolName());
                break;

            case "error":
                hasError.set(true);
                onEvent.accept(event);
                latch.countDown();
                break;

            case "complete":
                // 捕获引擎返回的最终结果内容
                if (event.getContent() != null && !event.getContent().isEmpty()) {
                    finalResultContent.append(event.getContent());
                }
                // 如果有原生工具调用，说明 Claude CLI 已自行执行工具，跳过文本解析
                if (!nativeToolUseSeen.get()) {
                    String fullText = textOutput.toString();
                    parseToolCalls(fullText, toolCalls);
                }
                latch.countDown();
                break;

            default:
                break;
        }
    }

    /**
     * 从 AI 输出文本中解析工具调用
     */
    private void parseToolCalls(String text, List<ToolCallInfo> toolCalls) {
        Matcher matcher = TOOL_CALL_PATTERN.matcher(text);
        while (matcher.find()) {
            try {
                String json = matcher.group(1);
                @SuppressWarnings("unchecked")
                Map<String, Object> call = objectMapper.readValue(json, Map.class);
                String toolName = (String) call.get("tool");
                @SuppressWarnings("unchecked")
                Map<String, Object> params = (Map<String, Object>) call.getOrDefault("params", Map.of());

                if (toolName != null && !toolName.isEmpty()) {
                    toolCalls.add(new ToolCallInfo(toolName, params));
                    log.debug("解析到工具调用: {} - 参数: {}", toolName, params);
                }
            } catch (Exception e) {
                log.warn("解析工具调用失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 提取工具调用之前的文本
     */
    private String extractTextBeforeToolCall(String text) {
        Matcher matcher = TOOL_CALL_PATTERN.matcher(text);
        if (matcher.find()) {
            return text.substring(0, matcher.start()).trim();
        }
        return text.trim();
    }

    /**
     * 工具调用信息
     */
    private static class ToolCallInfo {
        final String name;
        final Map<String, Object> params;

        ToolCallInfo(String name, Map<String, Object> params) {
            this.name = name;
            this.params = params;
        }
    }

    /**
     * 获取工具的中文显示名称
     */
    private String getToolDisplayName(String toolName) {
        return switch (toolName) {
            case "list_agents" -> "查询Agent列表";
            case "send_agent_task" -> "发送Agent任务";
            case "intervene_agent" -> "干预Agent";
            case "pause_agent" -> "暂停Agent";
            case "resume_agent" -> "恢复Agent";
            case "get_agent_health" -> "查询Agent健康状态";
            case "get_agent_logs" -> "查询Agent日志";
            case "list_projects" -> "查询项目列表";
            case "create_project" -> "创建项目";
            case "create_project_from_template" -> "从模板创建项目";
            case "list_workflow_templates" -> "查询工作流模板";
            case "create_workflow_template" -> "创建工作流模板";
            case "delete_workflow_template" -> "删除工作流模板";
            case "start_workflow" -> "启动工作流";
            case "list_workflow_instances" -> "查询工作流实例";
            case "cancel_workflow" -> "取消工作流";
            case "pause_workflow" -> "暂停工作流";
            case "resume_workflow" -> "恢复工作流";
            case "list_game_templates" -> "查询游戏模板";
            case "create_game_template" -> "创建游戏模板";
            case "delete_game_template" -> "删除游戏模板";
            case "list_skills" -> "查询技能列表";
            case "create_skill" -> "创建技能";
            case "list_mcp_servers" -> "查询MCP服务器";
            case "add_mcp_server" -> "添加MCP服务器";
            case "test_mcp_server" -> "测试MCP服务器";
            case "list_alerts" -> "查询告警";
            case "acknowledge_alert" -> "确认告警";
            case "resolve_alert" -> "解决告警";
            case "list_notifications" -> "查询通知";
            case "mark_notification_read" -> "标记通知已读";
            case "mark_all_notifications_read" -> "全部标记已读";
            case "get_resource_usage" -> "查询资源使用";
            case "get_operation_logs" -> "查询操作日志";
            case "list_configs" -> "查询系统配置";
            case "update_config" -> "更新系统配置";
            case "list_tokens" -> "查询Token列表";
            case "list_users" -> "查询用户列表";
            case "list_roles" -> "查询角色列表";
            case "list_pipelines" -> "查询流水线";
            case "trigger_pipeline" -> "触发流水线";
            case "list_git_repos" -> "查询Git仓库";
            case "list_reviews" -> "查询代码审查";
            case "list_capabilities" -> "查询能力列表";
            case "create_capability" -> "创建能力";
            case "get_system_info" -> "查询系统信息";
            case "get_diagnostic" -> "系统自检";
            default -> toolName;
        };
    }
}
