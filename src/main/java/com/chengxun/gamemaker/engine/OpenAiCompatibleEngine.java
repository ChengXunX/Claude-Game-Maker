package com.chengxun.gamemaker.engine;

import com.chengxun.gamemaker.config.AppConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * OpenAI 兼容 API 引擎
 * 用于调用 MiniMax、DeepSeek、智谱、月之暗面等 OpenAI 兼容格式的 API
 *
 * 与 ClaudeCliEngine 的区别：
 * - ClaudeCliEngine 通过 Claude CLI 子进程调用 Anthropic 格式 API
 * - 本引擎直接通过 HTTP 调用 OpenAI 兼容格式 API（/v1/chat/completions）
 *
 * @author chengxun
 * @since 1.0.0
 */
@Component
public class OpenAiCompatibleEngine {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleEngine.class);

    private final AppConfig appConfig;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 已知的 OpenAI 兼容 API 提供商域名 */
    private static final Set<String> OPENAI_COMPATIBLE_HOSTS = Set.of(
        "api.minimaxi.com",     // MiniMax
        "api.deepseek.com",     // DeepSeek
        "open.bigmodel.cn",     // 智谱
        "api.moonshot.cn",      // 月之暗面 Kimi
        "api.stepfun.com",      // 阶跃星辰
        "api.xfyun.cn",        // 科大讯飞
        "openrouter.ai",        // OpenRouter
        "api.volcengine.com",   // 火山引擎
        "ark.cn-beijing.volces.com", // 火山引擎 Ark
        "qianfan.baidubce.com", // 百度千帆
        "api.hunyuan.cloud.tencent.com" // 腾讯混元
    );

    public OpenAiCompatibleEngine(AppConfig appConfig) {
        this.appConfig = appConfig;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS) // 长文本生成需要较长时间
            .build();
    }

    /**
     * 判断指定的 API URL 是否为 OpenAI 兼容格式
     *
     * @param apiUrl API 地址
     * @return true 表示应该使用本引擎而非 Claude CLI
     */
    public static boolean isOpenAiCompatible(String apiUrl) {
        if (apiUrl == null || apiUrl.isEmpty()) return false;
        String lower = apiUrl.toLowerCase();
        for (String host : OPENAI_COMPATIBLE_HOSTS) {
            if (lower.contains(host)) return true;
        }
        return false;
    }

    /**
     * 发送消息（使用配置中的默认 maxTokens）
     */
    public ClaudeCliEngine.AiCallResult sendMessage(String agentId, String message,
                                                      String apiKey, String apiUrl, String model) {
        return sendMessage(agentId, message, apiKey, apiUrl, model, appConfig.getClaude().getMaxTokens());
    }

    /**
     * 发送消息并返回结果（含 Token 计量）
     *
     * @param agentId   Agent ID
     * @param message   消息内容
     * @param apiKey    API Key
     * @param apiUrl    API 地址
     * @param model     模型名称
     * @param maxTokens 最大输出 Token 数
     * @return AiCallResult 包含响应文本和 token 数据
     */
    public ClaudeCliEngine.AiCallResult sendMessage(String agentId, String message,
                                                      String apiKey, String apiUrl, String model,
                                                      int maxTokens) {
        long startTime = System.currentTimeMillis();

        try {
            // 构建请求体（OpenAI Chat Completions 格式）
            Map<String, Object> request = new HashMap<>();
            request.put("model", model);
            request.put("messages", List.of(
                Map.of("role", "user", "content", message)
            ));
            request.put("max_tokens", maxTokens);

            String requestJson = objectMapper.writeValueAsString(request);

            // 构建 URL（确保以 /v1/chat/completions 结尾）
            String url = buildChatCompletionsUrl(apiUrl);

            log.info("OpenAI compatible API call: agent={}, model={}, url={}", agentId, model, url);
            log.debug("Request body length: {}", requestJson.length());

            Request httpRequest = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestJson, MediaType.parse("application/json")))
                .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                long duration = System.currentTimeMillis() - startTime;

                if (response.body() == null) {
                    log.error("Empty response from OpenAI compatible API for agent: {}", agentId);
                    return ClaudeCliEngine.AiCallResult.fromEstimate("API Error: Empty response", 0, 0);
                }

                String responseBody = response.body().string();

                if (!response.isSuccessful()) {
                    log.error("OpenAI compatible API error for agent {}: HTTP {} - {}", agentId, response.code(), responseBody);
                    // 提取错误信息
                    String errorMsg = extractErrorMessage(responseBody, response.code());
                    return ClaudeCliEngine.AiCallResult.fromEstimate("API Error: " + errorMsg, 0, 0);
                }

                // 解析响应
                JsonNode root = objectMapper.readTree(responseBody);

                // 提取文本内容
                String content = "";
                JsonNode choices = root.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    JsonNode messageNode = choices.get(0).path("message");
                    content = messageNode.path("content").asText("");
                }

                // 提取 Token 使用量
                long inputTokens = 0;
                long outputTokens = 0;
                JsonNode usage = root.path("usage");
                if (!usage.isMissingNode()) {
                    inputTokens = usage.path("prompt_tokens").asLong(0);
                    outputTokens = usage.path("completion_tokens").asLong(0);
                }

                log.info("OpenAI compatible API response: agent={}, input={}, output={}, duration={}ms",
                    agentId, inputTokens, outputTokens, duration);

                if (inputTokens > 0 || outputTokens > 0) {
                    return new ClaudeCliEngine.AiCallResult(content, inputTokens, outputTokens, duration, true);
                } else {
                    return ClaudeCliEngine.AiCallResult.fromEstimate(content,
                        (int) estimateTokens(message), (int) estimateTokens(content));
                }
            }
        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("OpenAI compatible API call failed for agent {}: {}", agentId, e.getMessage(), e);
            return ClaudeCliEngine.AiCallResult.fromEstimate("API Error: " + e.getMessage(), 0, 0);
        }
    }

    /**
     * 构建 Chat Completions URL
     * 确保 URL 指向 /v1/chat/completions 端点
     */
    private String buildChatCompletionsUrl(String apiUrl) {
        if (apiUrl == null) apiUrl = "";

        // 移除尾部斜杠
        String url = apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;

        // 修复误配置的 URL：OpenAI 兼容 API 不应包含 /anthropic 路径前缀
        // 例如 https://api.minimaxi.com/anthropic/v1 -> https://api.minimaxi.com/v1
        //      https://api.minimaxi.com/anthropic -> https://api.minimaxi.com
        if (url.contains("/anthropic/")) {
            url = url.replace("/anthropic/", "/");
            log.debug("Stripped '/anthropic' from OpenAI compatible API URL: {} -> {}", apiUrl, url);
        } else if (url.endsWith("/anthropic")) {
            url = url.substring(0, url.length() - "/anthropic".length());
            log.debug("Stripped '/anthropic' from OpenAI compatible API URL: {} -> {}", apiUrl, url);
        }

        // 如果已经包含完整路径，直接使用
        if (url.endsWith("/chat/completions")) return url;
        if (url.endsWith("/v1/chat/completions")) return url;

        // 如果以 /v1 结尾，追加 /chat/completions
        if (url.endsWith("/v1")) return url + "/chat/completions";

        // 默认追加 /v1/chat/completions
        return url + "/v1/chat/completions";
    }

    /**
     * 从错误响应中提取可读的错误信息
     */
    private String extractErrorMessage(String responseBody, int httpCode) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            // OpenAI 格式: { "error": { "message": "...", "type": "..." } }
            JsonNode error = root.path("error");
            if (!error.isMissingNode()) {
                String msg = error.path("message").asText("");
                String type = error.path("type").asText("");
                if (!msg.isEmpty()) return type.isEmpty() ? msg : type + ": " + msg;
            }

            // MiniMax 格式: { "type": "error", "error": { "type": "...", "message": "..." } }
            JsonNode typeNode = root.path("type");
            if ("error".equals(typeNode.asText(""))) {
                JsonNode innerError = root.path("error");
                String msg = innerError.path("message").asText("");
                if (!msg.isEmpty()) return msg;
            }

            // 通用格式: { "message": "..." }
            String msg = root.path("message").asText("");
            if (!msg.isEmpty()) return msg;
        } catch (Exception ignored) {}

        // 截取前200字符避免日志过长
        return responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody;
    }

    /**
     * 估算 Token 数量（简单启发式）
     * 中文约 1.5 字/token，英文约 4 字符/token
     */
    private long estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        int chineseChars = 0;
        int otherChars = 0;
        for (char c : text.toCharArray()) {
            if (c > 0x4E00 && c < 0x9FFF) {
                chineseChars++;
            } else {
                otherChars++;
            }
        }
        return (long) (chineseChars * 1.5 + otherChars * 0.25);
    }
}
