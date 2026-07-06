package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.engine.ClaudeCliEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Claude AI服务
 * 提供AI对话能力，用于提示词优化、截图视觉分析等功能
 *
 * <p>支持的能力：</p>
 * <ul>
 *   <li>{@link #sendMessage(String)} - 纯文本对话</li>
 *   <li>{@link #sendMessageWithImages(String, List)} - 多模态对话（带图片附件）</li>
 * </ul>
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class ClaudeAiService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeAiService.class);

    /** 单张图片 base64 编码最大字节数（避免 prompt 过大） */
    private static final int MAX_IMAGE_BASE64_BYTES = 4 * 1024 * 1024; // 4MB

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

    /**
     * 发送带图片附件的消息给AI（多模态对话）
     *
     * <p>将图片文件编码为 base64 并附加到 prompt 中，用于截图视觉分析等场景。</p>
     *
     * <p>实现策略：</p>
     * <ul>
     *   <li>图片大小 &lt; 4MB：内联 base64 到 prompt 中</li>
     *   <li>图片过大：跳过该张图片，在 prompt 中标注路径</li>
     * </ul>
     *
     * @param prompt 文本提示词
     * @param imagePaths 图片文件绝对路径列表
     * @return AI 响应
     */
    public String sendMessageWithImages(String prompt, List<String> imagePaths) {
        if (imagePaths == null || imagePaths.isEmpty()) {
            return sendMessage(prompt);
        }

        // 过滤出有效图片
        List<String> validPaths = imagePaths.stream()
            .filter(p -> p != null && !p.isBlank() && new File(p).exists())
            .collect(Collectors.toList());

        if (validPaths.isEmpty()) {
            log.warn("sendMessageWithImages: 没有有效的图片路径");
            return sendMessage(prompt);
        }

        // 构建多模态 prompt
        StringBuilder multimodalPrompt = new StringBuilder(prompt);
        multimodalPrompt.append("\n\n## 附件图片\n");

        int embeddedCount = 0;
        int skippedCount = 0;
        for (String path : validPaths) {
            File f = new File(path);
            if (f.length() > MAX_IMAGE_BASE64_BYTES) {
                multimodalPrompt.append("- [跳过-过大] ").append(path)
                    .append(" (").append(f.length() / 1024 / 1024).append("MB)\n");
                skippedCount++;
                continue;
            }

            try {
                byte[] bytes = Files.readAllBytes(Paths.get(path));
                String base64 = Base64.getEncoder().encodeToString(bytes);
                multimodalPrompt.append("- [内联图片] 文件: ").append(path)
                    .append(", base64长度: ").append(base64.length())
                    .append(" 字符, data:image/png;base64,").append(base64).append("\n");
                embeddedCount++;
            } catch (Exception e) {
                log.warn("读取图片失败: {}, 错误: {}", path, e.getMessage());
                multimodalPrompt.append("- [读取失败] ").append(path).append("\n");
                skippedCount++;
            }
        }

        multimodalPrompt.append("\n（已嵌入 ").append(embeddedCount).append(" 张图片，跳过 ")
            .append(skippedCount).append(" 张）\n");

        log.info("多模态对话: 嵌入{}张图片, 跳过{}张, 总prompt长度={}",
            embeddedCount, skippedCount, multimodalPrompt.length());

        return sendMessage(multimodalPrompt.toString());
    }
}
