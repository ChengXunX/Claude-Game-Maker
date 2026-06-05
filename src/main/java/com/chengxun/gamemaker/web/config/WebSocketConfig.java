package com.chengxun.gamemaker.web.config;

import com.chengxun.gamemaker.web.websocket.NotificationWebSocketHandler;
import com.chengxun.gamemaker.web.websocket.TerminalWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置
 * 注册终端和通知 WebSocket 处理器
 *
 * @author chengxun
 * @since 1.0.0
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final TerminalWebSocketHandler terminalHandler;
    private final NotificationWebSocketHandler notificationHandler;

    public WebSocketConfig(TerminalWebSocketHandler terminalHandler,
                           NotificationWebSocketHandler notificationHandler) {
        this.terminalHandler = terminalHandler;
        this.notificationHandler = notificationHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(terminalHandler, "/ws/terminal")
                .setAllowedOrigins("*");

        registry.addHandler(notificationHandler, "/ws/notifications")
                .setAllowedOrigins("*");
    }
}
