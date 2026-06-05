package com.chengxun.gamemaker.web.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通知 WebSocket 处理器
 * 实时推送通知、审批请求、Agent 状态变更等
 *
 * @author chengxun
 * @since 2.0.0
 */
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 用户会话映射：userId -> WebSocketSession */
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = extractUserId(session);
        if (userId != null) {
            sessions.put(userId, session);
            log.info("Notification WebSocket connected: userId={}", userId);

            // 发送连接成功消息
            sendMessage(session, Map.of(
                "type", "connected",
                "message", "通知服务已连接",
                "timestamp", LocalDateTime.now().toString()
            ));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = extractUserId(session);
        if (userId != null) {
            sessions.remove(userId);
            log.info("Notification WebSocket disconnected: userId={}", userId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 客户端心跳
        try {
            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            if ("ping".equals(payload.get("type"))) {
                sendMessage(session, Map.of("type", "pong", "timestamp", LocalDateTime.now().toString()));
            }
        } catch (Exception e) {
            log.warn("Failed to handle WebSocket message: {}", e.getMessage());
        }
    }

    /**
     * 向指定用户发送通知
     */
    public void sendToUser(String userId, Map<String, Object> notification) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            sendMessage(session, notification);
        }
    }

    /**
     * 广播通知给所有在线用户
     */
    public void broadcast(Map<String, Object> notification) {
        sessions.values().forEach(session -> {
            if (session.isOpen()) {
                sendMessage(session, notification);
            }
        });
    }

    /**
     * 发送审批通知
     */
    public void sendApprovalNotification(String userId, Long approvalId, String description) {
        sendToUser(userId, Map.of(
            "type", "approval",
            "approvalId", approvalId,
            "description", description,
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * 发送 Agent 状态变更通知
     */
    public void sendAgentStatusChange(String userId, String agentId, String status) {
        sendToUser(userId, Map.of(
            "type", "agent_status",
            "agentId", agentId,
            "status", status,
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * 获取在线用户数
     */
    public int getOnlineCount() {
        return (int) sessions.values().stream().filter(WebSocketSession::isOpen).count();
    }

    private void sendMessage(WebSocketSession session, Map<String, Object> data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.warn("Failed to send WebSocket message: {}", e.getMessage());
        }
    }

    private String extractUserId(WebSocketSession session) {
        // 从 session attributes 或 URL 参数中提取 userId
        Object userId = session.getAttributes().get("userId");
        if (userId != null) return userId.toString();

        // 从 URL 参数提取
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=");
                if (kv.length == 2 && "userId".equals(kv[0])) {
                    return kv[1];
                }
            }
        }
        return null;
    }
}
