package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.engine.ClaudeCliEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Claude AI服务
 * 提供AI对话能力，用于提示词优化等功能
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class ClaudeAiService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeAiService.class);

    @Autowired(required = false)
    private ClaudeCliEngine cliEngine;

    /**
     * 发送消息给AI并获取响应
     *
     * @param prompt 提示词
     * @return AI响应
     */
    public String sendMessage(String prompt) {
        if (cliEngine == null) {
            log.warn("Claude CLI Engine not available");
            return "AI服务不可用";
        }

        try {
            // 使用CLI Engine发送消息
            // 注意：这里使用系统配置的默认API Key
            String response = cliEngine.sendMessage(
                "system-optimizer",  // agentId
                null,                // sessionId
                prompt,
                null,                // workDir
                null,                // apiKey (使用默认)
                null,                // apiUrl (使用默认)
                null,                // model (使用默认)
                null                 // mcpConfig
            );

            return response;
        } catch (Exception e) {
            log.error("AI调用失败: {}", e.getMessage());
            throw new RuntimeException("AI调用失败: " + e.getMessage());
        }
    }
}
