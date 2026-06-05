package com.chengxun.gamemaker.dingtalk;

import com.chengxun.gamemaker.config.AppConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 钉钉机器人服务
 * 支持钉钉自定义机器人的各种消息类型
 *
 * 主要功能：
 * - 发送文本消息
 * - 发送Markdown消息
 * - 发送链接消息
 * - 发送ActionCard消息（单按钮/多按钮）
 * - 发送FeedCard消息
 * - 支持@指定人
 * - 支持加签安全验证
 *
 * 配置说明：
 * - webhook-url: 机器人Webhook地址
 * - secret: 加签密钥（可选）
 * - enabled: 是否启用
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class DingTalkService {

    private static final Logger log = LoggerFactory.getLogger(DingTalkService.class);
    private static final String DINGTALK_API_BASE = "https://oapi.dingtalk.com/robot/send";

    private final AppConfig appConfig;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DingTalkService(AppConfig appConfig) {
        this.appConfig = appConfig;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
        this.objectMapper = new ObjectMapper();
    }

    // ===== 状态检查 =====

    /**
     * 检查钉钉机器人是否启用
     *
     * @return 是否启用
     */
    public boolean isEnabled() {
        AppConfig.DingTalkConfig config = appConfig.getDingtalk();
        return config != null
            && config.isEnabled()
            && config.getWebhookUrl() != null
            && !config.getWebhookUrl().isEmpty();
    }

    /**
     * 获取配置信息
     *
     * @return 配置信息（脱敏）
     */
    public Map<String, Object> getConfigInfo() {
        Map<String, Object> info = new HashMap<>();
        AppConfig.DingTalkConfig config = appConfig.getDingtalk();
        if (config != null) {
            info.put("enabled", config.isEnabled());
            info.put("webhookUrl", maskUrl(config.getWebhookUrl()));
            info.put("hasSecret", config.getSecret() != null && !config.getSecret().isEmpty());
        }
        return info;
    }

    /**
     * URL脱敏处理
     */
    private String maskUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        // 只显示前20个字符和后10个字符
        if (url.length() > 30) {
            return url.substring(0, 20) + "..." + url.substring(url.length() - 10);
        }
        return url;
    }

    // ===== 消息发送 =====

    /**
     * 发送文本消息
     *
     * @param content 消息内容
     */
    public void sendText(String content) {
        sendText(content, null, false);
    }

    /**
     * 发送文本消息并@所有人
     *
     * @param content 消息内容
     * @param isAtAll 是否@所有人
     */
    public void sendText(String content, boolean isAtAll) {
        sendText(content, null, isAtAll);
    }

    /**
     * 发送文本消息并@指定人
     *
     * @param content 消息内容
     * @param atMobiles 要@的手机号列表
     * @param isAtAll 是否@所有人
     */
    public void sendText(String content, List<String> atMobiles, boolean isAtAll) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("msgtype", "text");
            body.put("text", Map.of("content", content));

            Map<String, Object> at = new HashMap<>();
            if (atMobiles != null && !atMobiles.isEmpty()) {
                at.put("atMobiles", atMobiles);
            }
            at.put("isAtAll", isAtAll);
            body.put("at", at);

            sendRequest(body);
            log.info("DingTalk text message sent");
        } catch (Exception e) {
            log.error("Failed to send DingTalk text message", e);
        }
    }

    /**
     * 发送Markdown消息
     *
     * @param title 标题
     * @param text Markdown内容
     */
    public void sendMarkdown(String title, String text) {
        sendMarkdown(title, text, null, false);
    }

    /**
     * 发送Markdown消息并@指定人
     *
     * @param title 标题
     * @param text Markdown内容
     * @param atMobiles 要@的手机号列表
     * @param isAtAll 是否@所有人
     */
    public void sendMarkdown(String title, String text, List<String> atMobiles, boolean isAtAll) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("msgtype", "markdown");
            body.put("markdown", Map.of(
                "title", title,
                "text", text
            ));

            Map<String, Object> at = new HashMap<>();
            if (atMobiles != null && !atMobiles.isEmpty()) {
                at.put("atMobiles", atMobiles);
            }
            at.put("isAtAll", isAtAll);
            body.put("at", at);

            sendRequest(body);
            log.info("DingTalk markdown message sent: {}", title);
        } catch (Exception e) {
            log.error("Failed to send DingTalk markdown message", e);
        }
    }

    /**
     * 发送链接消息
     *
     * @param title 标题
     * @param text 描述
     * @param messageUrl 跳转链接
     * @param picUrl 图片链接
     */
    public void sendLink(String title, String text, String messageUrl, String picUrl) {
        try {
            Map<String, Object> link = new HashMap<>();
            link.put("title", title);
            link.put("text", text);
            link.put("messageUrl", messageUrl);
            link.put("picUrl", picUrl);

            Map<String, Object> body = new HashMap<>();
            body.put("msgtype", "link");
            body.put("link", link);

            sendRequest(body);
            log.info("DingTalk link message sent: {}", title);
        } catch (Exception e) {
            log.error("Failed to send DingTalk link message", e);
        }
    }

    /**
     * 发送整体跳转ActionCard
     *
     * @param title 标题
     * @param text 内容（支持Markdown）
     * @param singleTitle 按钮标题
     * @param singleURL 按钮跳转链接
     */
    public void sendActionCard(String title, String text, String singleTitle, String singleURL) {
        try {
            Map<String, Object> actionCard = new HashMap<>();
            actionCard.put("title", title);
            actionCard.put("text", text);
            actionCard.put("singleTitle", singleTitle);
            actionCard.put("singleURL", singleURL);

            Map<String, Object> body = new HashMap<>();
            body.put("msgtype", "actionCard");
            body.put("actionCard", actionCard);

            sendRequest(body);
            log.info("DingTalk actionCard message sent: {}", title);
        } catch (Exception e) {
            log.error("Failed to send DingTalk actionCard message", e);
        }
    }

    /**
     * 发送独立跳转ActionCard（多按钮）
     *
     * @param title 标题
     * @param text 内容（支持Markdown）
     * @param buttons 按钮列表，每个按钮包含title和actionURL
     */
    public void sendActionCardMultiButton(String title, String text, List<Map<String, String>> buttons) {
        try {
            Map<String, Object> actionCard = new HashMap<>();
            actionCard.put("title", title);
            actionCard.put("text", text);
            actionCard.put("btnOrientation", "0"); // 0:按钮竖直排列，1:按钮横向排列
            actionCard.put("btns", buttons);

            Map<String, Object> body = new HashMap<>();
            body.put("msgtype", "actionCard");
            body.put("actionCard", actionCard);

            sendRequest(body);
            log.info("DingTalk multi-button actionCard message sent: {}", title);
        } catch (Exception e) {
            log.error("Failed to send DingTalk multi-button actionCard message", e);
        }
    }

    /**
     * 发送通知消息（通用方法）
     *
     * @param title 标题
     * @param content 内容
     */
    public void sendNotification(String title, String content) {
        if (!isEnabled()) {
            log.warn("DingTalk not enabled, notification: {}", title);
            return;
        }

        // 使用Markdown格式发送
        String markdownText = String.format("### %s\n\n%s", title, content);
        sendMarkdown(title, markdownText);
    }

    // ===== 审批相关 =====

    /**
     * 发送审批请求
     *
     * @param content 审批内容
     */
    public void sendApprovalRequest(String content) {
        if (!isEnabled()) {
            log.warn("DingTalk not enabled, approval request: {}", content);
            return;
        }

        String title = "审批请求";
        String text = String.format("### %s\n\n%s\n\n请回复 **approve** 或 **reject**", title, content);
        sendMarkdown(title, text);
    }

    // ===== 错误和告警通知 =====

    /**
     * 发送错误通知
     *
     * @param agentId Agent ID
     * @param error 错误信息
     */
    public void sendErrorNotification(String agentId, String error) {
        if (!isEnabled()) {
            log.warn("DingTalk not enabled, error notification: {}", agentId);
            return;
        }

        String title = "错误通知";
        String text = String.format("### %s\n\n**Agent**: %s\n**错误**: %s", title, agentId, error);
        sendMarkdown(title, text, null, true);
    }

    /**
     * 发送任务完成通知
     *
     * @param agentId Agent ID
     * @param taskTitle 任务标题
     * @param result 任务结果
     */
    public void sendTaskCompleteNotification(String agentId, String taskTitle, String result) {
        if (!isEnabled()) {
            log.warn("DingTalk not enabled, task complete notification: {}", agentId);
            return;
        }

        String summary = result.length() > 200 ? result.substring(0, 200) + "..." : result;
        String title = "任务完成";
        String text = String.format("### %s\n\n**Agent**: %s\n**任务**: %s\n**结果**: %s",
            title, agentId, taskTitle, summary);
        sendMarkdown(title, text);
    }

    /**
     * 发送项目状态通知
     *
     * @param projectName 项目名称
     * @param status 项目状态
     */
    public void sendProjectStatusNotification(String projectName, String status) {
        if (!isEnabled()) {
            log.warn("DingTalk not enabled, project status notification: {}", projectName);
            return;
        }

        String title = "项目状态";
        String text = String.format("### %s\n\n**项目**: %s\n**状态**: %s", title, projectName, status);
        sendMarkdown(title, text);
    }

    // ===== 内部方法 =====

    /**
     * 发送请求到钉钉API
     *
     * @param body 请求体
     * @throws IOException IO异常
     * @throws NoSuchAlgorithmException 签名算法异常
     * @throws InvalidKeyException 签名密钥异常
     */
    private void sendRequest(Map<String, Object> body) throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        if (!isEnabled()) {
            log.warn("DingTalk not enabled, message skipped");
            return;
        }

        String webhookUrl = buildWebhookUrl();
        String jsonBody = objectMapper.writeValueAsString(body);

        Request request = new Request.Builder()
            .url(webhookUrl)
            .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.body() != null) {
                String responseBody = response.body().string();
                log.debug("DingTalk API response: {}", responseBody);

                // 检查响应
                com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(responseBody);
                if (node.has("errcode") && node.get("errcode").asInt() != 0) {
                    log.error("DingTalk API error: {}", responseBody);
                }
            }
        }
    }

    /**
     * 构建Webhook URL（包含签名）
     *
     * @return 完整的Webhook URL
     * @throws NoSuchAlgorithmException 签名算法异常
     * @throws InvalidKeyException 签名密钥异常
     */
    private String buildWebhookUrl() throws NoSuchAlgorithmException, InvalidKeyException {
        AppConfig.DingTalkConfig config = appConfig.getDingtalk();
        String webhookUrl = config.getWebhookUrl();
        String secret = config.getSecret();

        if (secret == null || secret.isEmpty()) {
            return webhookUrl;
        }

        // 加签处理
        long timestamp = System.currentTimeMillis();
        String stringToSign = timestamp + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        String sign = URLEncoder.encode(Base64.getEncoder().encodeToString(signData), StandardCharsets.UTF_8);

        return webhookUrl + "&timestamp=" + timestamp + "&sign=" + sign;
    }

    /**
     * 测试钉钉连接
     *
     * @return 测试结果
     */
    public Map<String, Object> testConnection() {
        Map<String, Object> result = new HashMap<>();
        try {
            if (!isEnabled()) {
                result.put("success", false);
                result.put("message", "钉钉机器人未启用");
                return result;
            }

            sendText("ChengXun Game Maker 钉钉机器人连接测试成功！");
            result.put("success", true);
            result.put("message", "测试消息发送成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "测试失败: " + e.getMessage());
        }
        return result;
    }
}
