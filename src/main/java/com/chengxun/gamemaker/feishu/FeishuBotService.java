package com.chengxun.gamemaker.feishu;

import com.chengxun.gamemaker.config.AppConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class FeishuBotService {

    private static final Logger log = LoggerFactory.getLogger(FeishuBotService.class);
    private static final String FEISHU_API_BASE = "https://open.feishu.cn/open-apis";

    private final AppConfig appConfig;
    private final com.chengxun.gamemaker.web.service.SystemConfigService configService;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String accessToken;
    private long tokenExpireTime;

    // 接收消息的 chat_id（需要从飞书事件中获取或配置）
    private String defaultChatId;

    public FeishuBotService(AppConfig appConfig, com.chengxun.gamemaker.web.service.SystemConfigService configService) {
        this.appConfig = appConfig;
        this.configService = configService;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
        this.defaultChatId = appConfig.getFeishu().getChatId();
    }

    public boolean isEnabled() {
        if (!appConfig.getFeishu().isEnabled()) {
            return false;
        }
        // 支持两种配置方式：App ID 或 Webhook URL
        boolean hasAppId = appConfig.getFeishu().getAppId() != null
            && !appConfig.getFeishu().getAppId().isEmpty();
        boolean hasWebhook = appConfig.getFeishu().getWebhookUrl() != null
            && !appConfig.getFeishu().getWebhookUrl().isEmpty();
        return hasAppId || hasWebhook;
    }

    // ===== 消息发送 =====

    /**
     * 发送纯文本消息
     */
    public void sendMessage(String content) {
        sendMessage(resolveChatId(), content);
    }

    public void sendMessage(String chatId, String content) {
        if (!isEnabled()) {
            log.warn("Feishu bot not enabled, message: {}", content);
            return;
        }

        // 自动升级为卡片消息
        sendCardMessage(chatId, "📢 通知", "blue", content);
    }

    /**
     * 发送卡片消息（单个 markdown 元素）
     */
    public void sendCardMessage(String chatId, String title, String content) {
        sendCardMessage(chatId, title, "blue", content);
    }

    /**
     * 发送卡片消息（指定颜色）
     *
     * @param chatId  群聊 ID
     * @param title   卡片标题
     * @param color   标题颜色：blue, green, red, orange, purple, indigo, turquoise, yellow, grey
     * @param content markdown 内容
     */
    public void sendCardMessage(String chatId, String title, String color, String content) {
        if (!isEnabled()) {
            log.warn("Feishu bot not enabled");
            return;
        }

        // 动态解析 chatId：优先用参数，其次用 AppConfig（数据库同步后的值），最后用内存缓存
        if (chatId == null || chatId.isEmpty()) {
            chatId = resolveChatId();
        }

        if (chatId == null || chatId.isEmpty()) {
            log.warn("No chat_id configured, using webhook fallback");
            sendWebhookCardMessage(title, color, content);
            return;
        }

        try {
            String token = getAccessToken();
            if (token == null) return;

            String url = FEISHU_API_BASE + "/im/v1/messages?receive_id_type=chat_id";

            Map<String, Object> card = buildCardJson(title, color, content);

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

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.body() != null) {
                    String responseBody = response.body().string();
                    JsonNode node = objectMapper.readTree(responseBody);
                    if (node.has("code") && node.get("code").asInt() == 0) {
                        log.info("Card message sent to Feishu chat: {}", chatId);
                    } else {
                        log.error("Failed to send card message: {}", responseBody);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to send Feishu card message", e);
        }
    }

    /**
     * 发送带备注的卡片消息（底部灰色小字）
     */
    public void sendCardMessageWithNote(String chatId, String title, String color, String content, String note) {
        if (!isEnabled()) return;

        if (chatId == null || chatId.isEmpty()) {
            sendWebhookCardMessage(title, color, content);
            return;
        }

        try {
            String token = getAccessToken();
            if (token == null) return;

            String url = FEISHU_API_BASE + "/im/v1/messages?receive_id_type=chat_id";

            Map<String, Object> card = buildCardJson(title, color, content);

            // 添加备注
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> elements = (java.util.List<Map<String, Object>>) card.get("elements");
            elements.add(Map.of(
                "tag", "note",
                "elements", new Object[]{Map.of("tag", "plain_text", "content", note)}
            ));

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
        } catch (Exception e) {
            log.error("Failed to send Feishu card message with note", e);
        }
    }

    /**
     * 构建飞书卡片 JSON
     */
    private Map<String, Object> buildCardJson(String title, String color, String content) {
        Map<String, Object> card = new HashMap<>();

        // 标题头
        Map<String, Object> header = new HashMap<>();
        Map<String, Object> titleObj = new HashMap<>();
        titleObj.put("tag", "plain_text");
        titleObj.put("content", title);
        header.put("title", titleObj);
        header.put("template", color != null ? color : "blue");
        card.put("header", header);

        // 内容元素列表
        java.util.List<Map<String, Object>> elements = new java.util.ArrayList<>();

        // 将内容按分隔线拆分为多个块，每块一个 markdown 元素
        // 避免单个 markdown 元素过长导致渲染问题
        String[] blocks = content.split("\n---\n");
        for (int i = 0; i < blocks.length; i++) {
            String block = blocks[i].trim();
            if (block.isEmpty()) continue;

            Map<String, Object> mdElement = new HashMap<>();
            mdElement.put("tag", "markdown");
            mdElement.put("content", block);
            elements.add(mdElement);

            // 块之间加分割线（最后一块不加）
            if (i < blocks.length - 1) {
                elements.add(Map.of("tag", "hr"));
            }
        }

        // 如果内容为空，添加占位
        if (elements.isEmpty()) {
            elements.add(Map.of("tag", "markdown", "content", "（无内容）"));
        }

        card.put("elements", elements);
        return card;
    }

    // ===== 审批请求 =====

    /**
     * 发送带审批按钮的卡片消息（无需 requestId）
     * 用于通知类审批模板，按钮触发后通过卡片回调处理
     *
     * @param chatId  群聊 ID
     * @param title   卡片标题
     * @param color   标题颜色
     * @param content markdown 内容
     * @param actionId 动作标识（用于回调时识别是哪个审批）
     */
    public void sendCardMessageWithApprovalButtons(String chatId, String title, String color, String content, String actionId) {
        if (!isEnabled()) {
            log.warn("Feishu bot not enabled");
            return;
        }

        if (chatId == null || chatId.isEmpty()) {
            chatId = resolveChatId();
        }

        if (chatId == null || chatId.isEmpty()) {
            log.warn("No chat_id configured for approval card");
            return;
        }

        try {
            String token = getAccessToken();
            if (token == null) return;

            String url = FEISHU_API_BASE + "/im/v1/messages?receive_id_type=chat_id";

            Map<String, Object> card = buildCardJsonWithApprovalButtons(title, color, content, actionId);

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

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.body() != null) {
                    String responseBody = response.body().string();
                    JsonNode node = objectMapper.readTree(responseBody);
                    if (node.has("code") && node.get("code").asInt() == 0) {
                        log.info("Approval card sent to Feishu chat: {}, actionId: {}", chatId, actionId);
                    } else {
                        log.error("Failed to send approval card: {}", responseBody);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to send Feishu approval card", e);
        }
    }

    /**
     * 构建带审批按钮的卡片 JSON（无需 requestId）
     * 底部包含「同意」和「拒绝」两个按钮
     */
    private Map<String, Object> buildCardJsonWithApprovalButtons(String title, String color, String content, String actionId) {
        Map<String, Object> card = new HashMap<>();

        // 标题头
        Map<String, Object> header = new HashMap<>();
        Map<String, Object> titleObj = new HashMap<>();
        titleObj.put("tag", "plain_text");
        titleObj.put("content", title);
        header.put("title", titleObj);
        header.put("template", color != null ? color : "orange");
        card.put("header", header);

        // 内容元素列表
        java.util.List<Map<String, Object>> elements = new java.util.ArrayList<>();

        // 将内容按分隔线拆分为多个块
        String[] blocks = content.split("\n---\n");
        for (int i = 0; i < blocks.length; i++) {
            String block = blocks[i].trim();
            if (block.isEmpty()) continue;

            Map<String, Object> mdElement = new HashMap<>();
            mdElement.put("tag", "markdown");
            mdElement.put("content", block);
            elements.add(mdElement);

            if (i < blocks.length - 1) {
                elements.add(Map.of("tag", "hr"));
            }
        }

        // 如果内容为空，添加占位
        if (elements.isEmpty()) {
            elements.add(Map.of("tag", "markdown", "content", "（无内容）"));
        }

        // 添加审批按钮组
        Map<String, Object> actionBlock = new HashMap<>();
        actionBlock.put("tag", "action");

        java.util.List<Map<String, Object>> actions = new java.util.ArrayList<>();

        // 同意按钮（绿色）
        Map<String, Object> approveValue = new HashMap<>();
        approveValue.put("action", "approve");
        approveValue.put("actionId", actionId != null ? actionId : "");
        // requestId 用于直接审批，actionId 作为数字时就是 requestId
        if (actionId != null) {
            try { approveValue.put("requestId", Long.parseLong(actionId)); } catch (NumberFormatException ignored) {}
        }
        Map<String, Object> approveBtn = new HashMap<>();
        approveBtn.put("tag", "button");
        approveBtn.put("text", Map.of("tag", "plain_text", "content", "✅ 同意"));
        approveBtn.put("type", "primary");
        approveBtn.put("value", approveValue);
        // 【新增】按钮点击后的 toast 提示
        approveBtn.put("toast", Map.of("type", "success", "content", "✅ 审批已通过"));
        actions.add(approveBtn);

        // 拒绝按钮（红色）
        Map<String, Object> rejectValue = new HashMap<>();
        rejectValue.put("action", "reject");
        rejectValue.put("actionId", actionId != null ? actionId : "");
        if (actionId != null) {
            try { rejectValue.put("requestId", Long.parseLong(actionId)); } catch (NumberFormatException ignored) {}
        }
        Map<String, Object> rejectBtn = new HashMap<>();
        rejectBtn.put("tag", "button");
        rejectBtn.put("text", Map.of("tag", "plain_text", "content", "❌ 拒绝"));
        rejectBtn.put("type", "danger");
        rejectBtn.put("value", rejectValue);
        // 【新增】按钮点击后的 toast 提示
        rejectBtn.put("toast", Map.of("type", "info", "content", "❌ 审批已拒绝"));
        actions.add(rejectBtn);

        actionBlock.put("actions", actions);
        elements.add(actionBlock);

        card.put("elements", elements);
        return card;
    }

    /**
     * 发送审批请求（纯文本，兼容旧逻辑）
     */
    public void sendApprovalRequest(String content) {
        String title = "📋 审批请求";
        String cardContent = content + "\n\n请回复 **approve** 或 **reject**";

        String cid = resolveChatId();
        if (cid != null) {
            sendCardMessage(cid, title, cardContent);
        } else {
            sendWebhookMessage(title + "\n\n" + cardContent);
        }
    }

    /**
     * 发送带按钮的审批卡片
     * 卡片底部显示「同意」和「拒绝」按钮，用户点击后触发回调
     *
     * @param chatId    群聊ID
     * @param title     卡片标题
     * @param content   markdown 内容
     * @param requestId 审批请求ID，用于回调时定位审批记录
     */
    public void sendApprovalCard(String chatId, String title, String content, Long requestId) {
        if (!isEnabled()) {
            log.warn("Feishu bot not enabled");
            return;
        }

        if (chatId == null || chatId.isEmpty()) {
            chatId = resolveChatId();
        }

        // 无 chat_id 时回退到 webhook
        if (chatId == null || chatId.isEmpty()) {
            log.info("No chat_id configured, using webhook for approval card");
            sendWebhookApprovalCard(title, content, requestId);
            return;
        }

        try {
            String token = getAccessToken();
            if (token == null) return;

            String url = FEISHU_API_BASE + "/im/v1/messages?receive_id_type=chat_id";

            Map<String, Object> card = buildApprovalCardJson(title, "orange", content, requestId);

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

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.body() != null) {
                    String responseBody = response.body().string();
                    JsonNode node = objectMapper.readTree(responseBody);
                    if (node.has("code") && node.get("code").asInt() == 0) {
                        log.info("Approval card sent to Feishu chat: {}, requestId: {}", chatId, requestId);
                    } else {
                        log.error("Failed to send approval card: {}", responseBody);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to send Feishu approval card", e);
        }
    }

    /**
     * 通过 Webhook 发送审批卡片
     */
    private void sendWebhookApprovalCard(String title, String content, Long requestId) {
        String webhookUrl = appConfig.getFeishu().getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.warn("Feishu webhook not configured for approval card");
            return;
        }

        try {
            Map<String, Object> card = buildApprovalCardJson(title, "orange", content, requestId);

            Map<String, Object> body = new HashMap<>();
            body.put("msg_type", "interactive");
            body.put("card", card);

            Request request = new Request.Builder()
                .url(webhookUrl)
                .post(RequestBody.create(
                    objectMapper.writeValueAsString(body),
                    MediaType.parse("application/json")))
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.body() != null) {
                    String responseBody = response.body().string();
                    log.info("Webhook approval card response: {}", responseBody);
                }
            }
            log.info("Webhook approval card sent for requestId: {}", requestId);
        } catch (Exception e) {
            log.error("Failed to send webhook approval card", e);
        }
    }

    /**
     * 构建带按钮的审批卡片 JSON
     * 底部包含「同意」和「拒绝」两个按钮
     * 按钮 value 中携带过期时间戳和 HMAC 签名，防止伪造和超期使用
     */
    private Map<String, Object> buildApprovalCardJson(String title, String color, String content, Long requestId) {
        Map<String, Object> card = new HashMap<>();

        // 标题头
        Map<String, Object> header = new HashMap<>();
        Map<String, Object> titleObj = new HashMap<>();
        titleObj.put("tag", "plain_text");
        titleObj.put("content", title);
        header.put("title", titleObj);
        header.put("template", color != null ? color : "orange");
        card.put("header", header);

        // 计算过期时间戳和签名
        int expireMinutes = appConfig.getFeishu().getCallbackExpireMinutes();
        long expireTime = System.currentTimeMillis() + expireMinutes * 60L * 1000L;
        String callbackToken = appConfig.getFeishu().getCallbackToken();
        String sig = signCallback(requestId, expireTime, callbackToken);

        // 构建按钮 value（包含 requestId、过期时间、签名）
        Map<String, Object> baseValue = new HashMap<>();
        baseValue.put("requestId", requestId);
        baseValue.put("expire", expireTime);
        if (sig != null) {
            baseValue.put("sig", sig);
        }

        // 内容元素列表
        java.util.List<Map<String, Object>> elements = new java.util.ArrayList<>();

        // 将内容按分隔线拆分为多个块
        String[] blocks = content.split("\n---\n");
        for (int i = 0; i < blocks.length; i++) {
            String block = blocks[i].trim();
            if (block.isEmpty()) continue;

            Map<String, Object> mdElement = new HashMap<>();
            mdElement.put("tag", "markdown");
            mdElement.put("content", block);
            elements.add(mdElement);

            if (i < blocks.length - 1) {
                elements.add(Map.of("tag", "hr"));
            }
        }

        // 添加 @指定人（从配置读取，多个用户ID逗号分隔）
        String notifyUserIds = appConfig.getFeishu().getApprovalNotifyUserIds();
        if (notifyUserIds != null && !notifyUserIds.isEmpty()) {
            StringBuilder atContent = new StringBuilder();
            for (String userId : notifyUserIds.split(",")) {
                String uid = userId.trim();
                if (!uid.isEmpty()) {
                    if (atContent.length() > 0) atContent.append(" ");
                    atContent.append("<at id=").append(uid).append("></at>");
                }
            }
            if (atContent.length() > 0) {
                Map<String, Object> atElement = new HashMap<>();
                atElement.put("tag", "markdown");
                atElement.put("content", "请审批: " + atContent);
                elements.add(atElement);
            }
        }

        // 添加审批按钮组
        Map<String, Object> actionBlock = new HashMap<>();
        actionBlock.put("tag", "action");

        java.util.List<Map<String, Object>> actions = new java.util.ArrayList<>();

        // 同意按钮（绿色）
        Map<String, Object> approveValue = new HashMap<>(baseValue);
        approveValue.put("action", "approve");
        Map<String, Object> approveBtn = new HashMap<>();
        approveBtn.put("tag", "button");
        approveBtn.put("text", Map.of("tag", "plain_text", "content", "✅ 同意"));
        approveBtn.put("type", "primary");
        approveBtn.put("value", approveValue);
        actions.add(approveBtn);

        // 拒绝按钮（红色）
        Map<String, Object> rejectValue = new HashMap<>(baseValue);
        rejectValue.put("action", "reject");
        Map<String, Object> rejectBtn = new HashMap<>();
        rejectBtn.put("tag", "button");
        rejectBtn.put("text", Map.of("tag", "plain_text", "content", "❌ 拒绝"));
        rejectBtn.put("type", "danger");
        rejectBtn.put("value", rejectValue);
        actions.add(rejectBtn);

        actionBlock.put("actions", actions);
        elements.add(actionBlock);

        // 底部备注（显示有效期）
        String expireDesc = expireMinutes >= 60
            ? (expireMinutes / 60) + "小时" + (expireMinutes % 60 > 0 ? (expireMinutes % 60) + "分钟" : "")
            : expireMinutes + "分钟";
        String noteText = "💡 点击按钮快速审批 | 有效期 " + expireDesc
            + (sig != null ? " | 已签名验证" : "");
        elements.add(Map.of(
            "tag", "note",
            "elements", new Object[]{Map.of("tag", "plain_text", "content", noteText)}
        ));

        card.put("elements", elements);
        return card;
    }

    /**
     * 生成审批回调签名
     * 使用 HMAC-SHA256 对 requestId:expire 进行签名
     *
     * @param requestId    审批请求ID
     * @param expireTime   过期时间戳（毫秒）
     * @param callbackToken 签名密钥（为 null 则跳过签名）
     * @return Base64 编码的签名，密钥为空时返回 null
     */
    private String signCallback(Long requestId, long expireTime, String callbackToken) {
        if (callbackToken == null || callbackToken.isEmpty()) {
            return null;
        }
        try {
            String data = requestId + ":" + expireTime;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(callbackToken.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signData = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signData);
        } catch (Exception e) {
            log.error("Failed to sign callback", e);
            return null;
        }
    }

    // ===== 通知消息 =====

    public void sendNotification(String title, String content) {
        String cid = resolveChatId();
        if (cid != null) {
            sendCardMessage(cid, "🔔 " + title, content);
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
        sendWebhookCardMessage("📢 通知", "blue", content);
    }

    /**
     * 通过 Webhook 发送卡片消息
     */
    public void sendWebhookCardMessage(String title, String color, String content) {
        String webhookUrl = appConfig.getFeishu().getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.warn("Feishu webhook not configured");
            return;
        }

        try {
            Map<String, Object> card = buildCardJson(title, color, content);

            Map<String, Object> body = new HashMap<>();
            body.put("msg_type", "interactive");
            body.put("card", card);

            Request request = new Request.Builder()
                .url(webhookUrl)
                .post(RequestBody.create(
                    objectMapper.writeValueAsString(body),
                    MediaType.parse("application/json")))
                .build();

            httpClient.newCall(request).execute();
            log.info("Webhook card message sent");
        } catch (IOException e) {
            log.error("Failed to send webhook card message", e);
        }
    }

    // ===== 回复消息（支持@mention） =====

    /**
     * 回复指定消息（带@mention）
     * 使用飞书回复 API，消息会以"回复 xxx"的形式展示
     *
     * @param messageId 要回复的消息ID
     * @param openId    要@的用户 open_id
     * @param title     卡片标题
     * @param color     卡片颜色
     * @param content   markdown 内容
     */
    public void sendReplyCardMessage(String messageId, String openId, String title, String color, String content) {
        if (!isEnabled()) {
            log.warn("Feishu bot not enabled");
            return;
        }

        if (messageId == null || messageId.isEmpty()) {
            log.warn("No message_id for reply");
            return;
        }

        try {
            String token = getAccessToken();
            if (token == null) return;

            String url = FEISHU_API_BASE + "/im/v1/messages/" + messageId + "/reply";

            // 在内容开头添加 @mention
            String mentionTag = (openId != null && !openId.isEmpty())
                ? "<at id=" + openId + "></at> "
                : "";
            String fullContent = mentionTag + content;

            Map<String, Object> card = buildCardJson(title, color, fullContent);

            Map<String, Object> body = new HashMap<>();
            body.put("msg_type", "interactive");
            body.put("content", objectMapper.writeValueAsString(card));

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
                        log.info("Reply card message sent: messageId={}, openId={}", messageId, openId);
                    } else {
                        log.error("Failed to send reply card message: {}", responseBody);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to send Feishu reply card message", e);
        }
    }

    /**
     * 发送带@mention的卡片消息（不回复特定消息）
     *
     * @param chatId  群聊ID
     * @param openId  要@的用户 open_id
     * @param title   卡片标题
     * @param color   卡片颜色
     * @param content markdown 内容
     */
    public void sendCardMessageWithMention(String chatId, String openId, String title, String color, String content) {
        if (!isEnabled()) {
            log.warn("Feishu bot not enabled");
            return;
        }

        // 在内容开头添加 @mention
        String mentionTag = (openId != null && !openId.isEmpty())
            ? "<at id=" + openId + "></at> "
            : "";
        String fullContent = mentionTag + content;

        sendCardMessage(chatId, title, color, fullContent);
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
        appConfig.getFeishu().setChatId(chatId);
        // 持久化到数据库，重启后自动恢复
        try {
            configService.setConfig("feishu.chat.id", chatId);
            log.info("Default Feishu chat ID set and persisted: {}", chatId);
        } catch (Exception e) {
            log.warn("Failed to persist Feishu chat ID: {}", e.getMessage());
        }
    }

    public String getDefaultChatId() {
        return resolveChatId();
    }

    /**
     * 动态获取有效的 chatId
     * 优先使用内存缓存，其次从 AppConfig 获取（数据库启动同步后的值）
     */
    private String resolveChatId() {
        if (defaultChatId != null && !defaultChatId.isEmpty()) return defaultChatId;
        String fromConfig = appConfig.getFeishu().getChatId();
        if (fromConfig != null && !fromConfig.isEmpty()) {
            // 回填缓存
            this.defaultChatId = fromConfig;
            return fromConfig;
        }
        return null;
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
                            if (resolveChatId() == null) {
                                this.defaultChatId = chatId;
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
