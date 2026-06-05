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
     * @param userPermissions 用户权限
     * @param isCancelled 取消标记
     * @param onEvent 事件回调
     * @return 会话ID（用于后续调用恢复上下文）
     */
    public String executeWithToolLoop(String agentKey, String sessionId, String message, String username,
                                     Set<String> userPermissions, AtomicBoolean isCancelled,
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
            CountDownLatch latch = new CountDownLatch(1);

            // 调用 AI 引擎（异步，通过 latch 等待完成）
            // 工具循环内部不使用 --resume，通过消息内容传递上下文
            streamingEngine.sendMessageStreaming(
                agentKey,
                null,
                currentMessage,
                null, null, null, null,
                event -> handleIterationEvent(event, textOutput, thinkingOutput, finalResultContent, toolCalls,
                                               hasError, isCancelled, nativeToolUseSeen, onEvent, latch)
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
                    continue;
                }

                // 执行工具
                long startTime = System.currentTimeMillis();
                Map<String, Object> result;
                try {
                    result = toolExecutor.executeTool(toolCall.name, toolCall.params, username);
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

                onEvent.accept(StreamEvent.toolResult(toolCall.name, resultJson));
                log.info("工具执行完成: {} ({}ms) - 轮次: {}", toolCall.name, duration, iteration);
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
     */
    private void handleIterationEvent(StreamEvent event,
                                       StringBuilder textOutput,
                                       StringBuilder thinkingOutput,
                                       StringBuilder finalResultContent,
                                       List<ToolCallInfo> toolCalls,
                                       AtomicBoolean hasError,
                                       AtomicBoolean isCancelled,
                                       AtomicBoolean nativeToolUseSeen,
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
                // Claude 原生工具调用，标记并转发给前端显示
                nativeToolUseSeen.set(true);
                onEvent.accept(event);
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
}
