package com.chengxun.gamemaker.feishu;

import com.chengxun.gamemaker.config.AppConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
public class FeishuBotService {
    
    private static final Logger log = LoggerFactory.getLogger(FeishuBotService.class);
    
    private final AppConfig appConfig;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private String accessToken;
    private long tokenExpireTime;
    
    public FeishuBotService(AppConfig appConfig) {
        this.appConfig = appConfig;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
    }
    
    public boolean isEnabled() {
        return appConfig.getFeishu().isEnabled() 
            && appConfig.getFeishu().getAppId() != null 
            && !appConfig.getFeishu().getAppId().isEmpty();
    }
    
    public void sendMessage(String content) {
        if (!isEnabled()) {
            log.warn("Feishu bot not enabled, message: {}", content);
            return;
        }
        
        try {
            String token = getAccessToken();
            if (token == null) {
                log.error("Failed to get Feishu access token");
                return;
            }
            
            log.info("Sending Feishu message: {}", content);
            
        } catch (Exception e) {
            log.error("Failed to send Feishu message", e);
        }
    }
    
    public void sendApprovalRequest(String content) {
        if (!isEnabled()) {
            log.warn("Feishu bot not enabled, approval request: {}", content);
            return;
        }
        
        String approvalMessage = String.format(
            "📋 **审批请求**\n\n%s\n\n请回复 'approve' 或 'reject'", 
            content
        );
        
        sendMessage(approvalMessage);
    }
    
    private String getAccessToken() {
        if (accessToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return accessToken;
        }
        
        try {
            String url = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal";
            
            String body = String.format(
                "{\"app_id\":\"%s\",\"app_secret\":\"%s\"}",
                appConfig.getFeishu().getAppId(),
                appConfig.getFeishu().getAppSecret()
            );
            
            Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(body, MediaType.parse("application/json")))
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
    
    public void sendWebhookMessage(String content) {
        String webhookUrl = appConfig.getFeishu().getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.warn("Feishu webhook not configured");
            return;
        }
        
        try {
            String body = String.format(
                "{\"msg_type\":\"text\",\"content\":{\"text\":\"%s\"}}",
                content.replace("\"", "\\\"")
            );
            
            Request request = new Request.Builder()
                .url(webhookUrl)
                .post(RequestBody.create(body, MediaType.parse("application/json")))
                .build();
            
            httpClient.newCall(request).execute();
            log.info("Webhook message sent");
            
        } catch (IOException e) {
            log.error("Failed to send webhook message", e);
        }
    }
}
