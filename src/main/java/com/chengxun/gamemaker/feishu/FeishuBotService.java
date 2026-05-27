package com.chengxun.gamemaker.feishu;

import com.chengxun.gamemaker.config.AppConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class FeishuBotService {

    private static final Logger log = LoggerFactory.getLogger(FeishuBotService.class);
    private static final String FEISHU_API_BASE = "https://open.feishu.cn/open-apis";

    private final AppConfig appConfig;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String accessToken;
    private long tokenExpireTime;

    // 接收消息的 chat_id（需要从飞书事件中获取或配置）
    private String defaultChatId;

    public FeishuBotService(AppConfig appConfig) {
        this.appConfig = appConfig;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
        this.defaultChatId = appConfig.getFeishu().getChatId();
    }

    public boolean isEnabled() {
        return appConfig.getFeishu().isEnabled()
            && appConfig.getFeishu().getAppId() != null
            && !appConfig.getFeishu().getAppId().isEmpty();
    }

    // ===== 消息发送 =====

    public void sendMessage(String content) {
        sendMessage(defaultChatId, content);
    }

    public void sendMessage(String chatId, String content) {
        if (!isEnabled()) {
            log.warn("Feishu bot not enabled, message: {}", content);
            return;
        }

        if (chatId == null || chatId.isEmpty()) {
            log.warn("No chat_id configured, using webhook fallback");
            sendWebhookMessage(content);
            return;
        }

        try {
            String token = getAccessToken();
            if (token == null) {
                log.error("Failed to get Feishu access token");
                return;
            }

            String url = FEISHU_API_BASE + "/im/v1/messages?receive_id_type=chat_id";

            Map<String, Object> body = new HashMap<>();
            body.put("receive_id", chatId);
            body.put("msg_type", "text");

            Map<String, String> textContent = new HashMap<>();
            textContent.put("text", content);
            body.put("content", objectMapper.writeValueAsString(textContent));

            Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .post(RequestBody.create(
                    objectMapper.writeValueAsString(body),
                    MediaType.parse("application/json")))
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.body() != null) {
                    String responseBody = response.body().string();
                    JsonNode node = objectMapper.readTree(responseBody);
                    if (node.has("code") && node.get("code").asInt() == 0) {
                        log.info("Message sent to Feishu chat: {}", chatId);
                    } else {
                        log.error("Failed to send message: {}", responseBody);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to send Feishu message", e);
        }
    }

    public void sendCardMessage(String chatId, String title, String content) {
        if (!isEnabled()) {
            log.warn("Feishu bot not enabled");
            return;
        }

        try {
            String token = getAccessToken();
            if (token == null) return;

            String url = FEISHU_API_BASE + "/im/v1/messages?receive_id_type=chat_id";

            Map<String, Object> card = new HashMap<>();
            card.put("header", Map.of("title", Map.of("tag", "plain_text", "content", title)));

            Map<String, Object> element = new HashMap<>();
            element.put("tag", "markdown");
            element.put("content", content);

            card.put("elements", new Object[]{element});

            Map<String, Object> body = new HashMap<>();
            body.put("receive_id", chatId);
            body.put("msg_type", "interactive");
            body.put("content", objectMapper.writeValueAsString(card));

            Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .post(RequestBody.create(
                    objectMapper.writeValueAsString(body),
                    MediaType.parse("application/json")))
                .build();

            httpClient.newCall(request).execute();
            log.info("Card message sent to Feishu chat: {}", chatId);
        } catch (Exception e) {
            log.error("Failed to send Feishu card message", e);
        }
    }

    // ===== 审批请求 =====

    public void sendApprovalRequest(String content) {
        String title = "📋 审批请求";
        String cardContent = content + "\n\n请回复 **approve** 或 **reject**";

        if (defaultChatId != null) {
            sendCardMessage(defaultChatId, title, cardContent);
        } else {
            sendWebhookMessage(title + "\n\n" + cardContent);
        }
    }

    // ===== 通知消息 =====

    public void sendNotification(String title, String content) {
        if (defaultChatId != null) {
            sendCardMessage(defaultChatId, "🔔 " + title, content);
        } else {
            sendWebhookMessage("🔔 " + title + "\n\n" + content);
        }
    }

    public void sendErrorNotification(String agentId, String error) {
        sendNotification("错误通知",
            String.format("**Agent**: %s\n**错误**: %s", agentId, error));
    }

    public void sendTaskCompleteNotification(String agentId, String taskTitle, String result) {
        String summary = result.length() > 200 ? result.substring(0, 200) + "..." : result;
        sendNotification("任务完成",
            String.format("**Agent**: %s\n**任务**: %s\n**结果**: %s",
                agentId, taskTitle, summary));
    }

    public void sendProjectStatusNotification(String projectName, String status) {
        sendNotification("项目状态",
            String.format("**项目**: %s\n**状态**: %s", projectName, status));
    }

    // ===== Webhook 消息 =====

    public void sendWebhookMessage(String content) {
        String webhookUrl = appConfig.getFeishu().getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.warn("Feishu webhook not configured");
            return;
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("msg_type", "text");
            body.put("content", Map.of("text", content));

            Request request = new Request.Builder()
                .url(webhookUrl)
                .post(RequestBody.create(
                    objectMapper.writeValueAsString(body),
                    MediaType.parse("application/json")))
                .build();

            httpClient.newCall(request).execute();
            log.info("Webhook message sent");
        } catch (IOException e) {
            log.error("Failed to send webhook message", e);
        }
    }

    // ===== Access Token =====

    public String getAccessToken() {
        if (accessToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return accessToken;
        }

        try {
            String url = FEISHU_API_BASE + "/auth/v3/tenant_access_token/internal";

            Map<String, String> body = new HashMap<>();
            body.put("app_id", appConfig.getFeishu().getAppId());
            body.put("app_secret", appConfig.getFeishu().getAppSecret());

            Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(
                    objectMapper.writeValueAsString(body),
                    MediaType.parse("application/json")))
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.body() != null) {
                    String responseBody = response.body().string();
                    JsonNode node = objectMapper.readTree(responseBody);

                    if (node.has("tenant_access_token")) {
                        accessToken = node.get("tenant_access_token").asText();
                        int expire = node.has("expire") ? node.get("expire").asInt() : 7200;
                        tokenExpireTime = System.currentTimeMillis() + (expire - 300) * 1000;
                        return accessToken;
                    }
                }
            }
        } catch (IOException e) {
            log.error("Failed to get Feishu access token", e);
        }

        return null;
    }

    // ===== Chat ID 管理 =====

    public void setDefaultChatId(String chatId) {
        this.defaultChatId = chatId;
        log.info("Default Feishu chat ID set to: {}", chatId);
    }

    public String getDefaultChatId() {
        return defaultChatId;
    }

    // ===== 获取群列表 =====

    public void loadChatList() {
        if (!isEnabled()) return;

        try {
            String token = getAccessToken();
            if (token == null) return;

            String url = FEISHU_API_BASE + "/im/v1/chats?page_size=20";

            Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .get()
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.body() != null) {
                    String responseBody = response.body().string();
                    JsonNode node = objectMapper.readTree(responseBody);

                    if (node.has("data") && node.get("data").has("items")) {
                        JsonNode items = node.get("data").get("items");
                        for (JsonNode item : items) {
                            String chatId = item.get("chat_id").asText();
                            String name = item.has("name") ? item.get("name").asText() : "Unknown";
                            log.info("Found Feishu chat: {} ({})", name, chatId);

                            // 自动设置第一个群为默认群
                            if (defaultChatId == null) {
                                defaultChatId = chatId;
                                log.info("Set default chat: {} ({})", name, chatId);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to load Feishu chat list", e);
        }
    }
}
