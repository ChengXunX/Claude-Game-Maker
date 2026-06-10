package com.chengxun.gamemaker.engine;

import com.chengxun.gamemaker.config.AppConfig;
import com.chengxun.gamemaker.model.StreamEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 流式Claude CLI引擎
 * 支持实时推送思考过程、工具调用、任务等
 *
 * 主要功能：
 * - 流式读取Claude CLI输出
 * - 解析thinking、tool_use、tool_result、task等事件
 * - 通过回调函数实时推送事件
 *
 * Claude CLI stream-json输出格式：
 * - type: "content_block_start" - 内容块开始
 * - type: "content_block_delta" - 内容块增量
 * - type: "content_block_stop" - 内容块结束
 * - type: "message_start" - 消息开始
 * - type: "message_delta" - 消息增量
 * - type: "message_stop" - 消息结束
 *
 * @author chengxun
 * @since 1.0.0
 */
@Component
public class StreamingClaudeCliEngine {

    private static final Logger log = LoggerFactory.getLogger(StreamingClaudeCliEngine.class);

    private final AppConfig appConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, ProcessInfo> processes = new ConcurrentHashMap<>();
    private final Map<String, String> sessionIds = new ConcurrentHashMap<>();

    public StreamingClaudeCliEngine(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    /**
     * 流式发送消息
     * 通过回调函数实时推送事件
     *
     * @param agentId Agent ID
     * @param sessionId 会话ID
     * @param message 消息内容
     * @param workDir 工作目录
     * @param apiKey API密钥
     * @param apiUrl API地址
     * @param model 模型名称
     * @param userToken 用户 JWT Token，用于 MCP 工具鉴权
     * @param onEvent 事件回调
     */
    public void sendMessageStreaming(String agentId, String sessionId, String message,
                                     String workDir, String apiKey, String apiUrl, String model,
                                     String userToken, Consumer<StreamEvent> onEvent) {
        ProcessInfo processInfo = getOrCreateProcess(agentId, sessionId, workDir, apiKey, apiUrl, model, userToken);
        if (processInfo == null) {
            onEvent.accept(StreamEvent.error("Failed to create Claude CLI process"));
            return;
        }

        // 异步执行命令
        Thread streamThread = new Thread(() -> {
            executeCommandStreaming(processInfo, message, agentId, onEvent);
        });
        streamThread.setDaemon(true);
        streamThread.setName("claude-stream-" + agentId);
        streamThread.start();
    }

    /**
     * 获取或创建进程
     * --print 模式是一次性进程，每次都需要创建新进程
     */
    private ProcessInfo getOrCreateProcess(String agentId, String sessionId,
                                           String workDir, String apiKey, String apiUrl, String model,
                                           String userToken) {
        // 清理旧进程（--print 模式下进程用完即销毁）
        ProcessInfo existing = processes.remove(agentId);
        if (existing != null) {
            existing.destroy();
        }

        try {
            return createProcess(agentId, sessionId, workDir, apiKey, apiUrl, model, userToken);
        } catch (Exception e) {
            log.error("Failed to create Claude CLI process for agent: {}", agentId, e);
            return null;
        }
    }

    /**
     * 创建Claude CLI进程
     */
    private ProcessInfo createProcess(String agentId, String sessionId,
                                      String workDir, String apiKey, String apiUrl, String model,
                                      String userToken) throws IOException {
        String installPath = appConfig.getClaude().getInstallPath();

        // 使用默认配置
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = appConfig.getApiKey();
        }
        if (apiUrl == null || apiUrl.isEmpty()) {
            apiUrl = appConfig.getApiUrl();
        }
        if (model == null || model.isEmpty()) {
            model = appConfig.getModel();
        }

        ProcessBuilder pb = new ProcessBuilder();
        pb.redirectErrorStream(false);

        if (workDir != null && !workDir.isEmpty()) {
            pb.directory(new File(workDir));
        }

        List<String> command = new ArrayList<>();

        // Claude CLI 禁止 root 使用 --dangerously-skip-permissions
        // 检测 root 时以项目目录所有者身份运行
        boolean isRoot = "root".equals(System.getProperty("user.name"));

        // 生成 MCP 配置文件
        File mcpConfigFile = null;
        if (userToken != null && !userToken.isEmpty()) {
            mcpConfigFile = generateMcpConfig(userToken);
            if (mcpConfigFile != null) {
                log.info("MCP config generated for agent: {}, token: {}...", agentId, userToken.substring(0, Math.min(20, userToken.length())));
            }
        }

        if (isRoot) {
            // 获取项目目录的所有者作为运行用户（支持任意部署用户）
            String runUser = System.getenv("RUN_USER");
            if (runUser == null || runUser.isEmpty() || "root".equals(runUser)) {
                try {
                    java.nio.file.Path projectDir = java.nio.file.Paths.get(System.getProperty("user.dir"));
                    runUser = (String) java.nio.file.Files.getAttribute(projectDir, "unix:ownerName");
                } catch (Exception e) {
                    runUser = "nobody";
                }
            }
            log.info("Running as root, dropping to user: {}", runUser);
            command.add("su");
            command.add("-s");
            command.add("/bin/bash");
            command.add(runUser);
            command.add("-c");

            StringBuilder cmd = new StringBuilder();
            // 注入环境变量
            if (apiKey != null && !apiKey.isEmpty()) {
                cmd.append("ANTHROPIC_API_KEY='").append(apiKey.replace("'", "'\\''")).append("' ");
            }
            if (apiUrl != null && !apiUrl.isEmpty()) {
                cmd.append("ANTHROPIC_BASE_URL='").append(apiUrl.replace("'", "'\\''")).append("' ");
            }
            cmd.append(installPath);
            cmd.append(" --print");
            cmd.append(" --input-format text");
            cmd.append(" --output-format stream-json");
            cmd.append(" --verbose");
            cmd.append(" --dangerously-skip-permissions");
            if (model != null && !model.isEmpty()) {
                cmd.append(" --model ").append(model);
            }
            if (sessionId != null && !sessionId.isEmpty()) {
                cmd.append(" --resume ").append(sessionId);
            }
            if (mcpConfigFile != null) {
                cmd.append(" --mcp-config ").append(mcpConfigFile.getAbsolutePath());
            }
            command.add(cmd.toString());
        } else {
            command.add(installPath);
            command.add("--print");
            command.add("--input-format");
            command.add("text");
            command.add("--output-format");
            command.add("stream-json");
            command.add("--verbose");
            command.add("--dangerously-skip-permissions");
            if (model != null && !model.isEmpty()) {
                command.add("--model");
                command.add(model);
            }
            if (sessionId != null && !sessionId.isEmpty()) {
                command.add("--resume");
                command.add(sessionId);
            }
            if (mcpConfigFile != null) {
                command.add("--mcp-config");
                command.add(mcpConfigFile.getAbsolutePath());
            }

            // 非 root 模式：通过 ProcessBuilder 设置环境变量
            Map<String, String> env = pb.environment();
            if (apiKey != null && !apiKey.isEmpty()) {
                env.put("ANTHROPIC_API_KEY", apiKey);
            } else {
                env.remove("ANTHROPIC_API_KEY");
            }
            if (apiUrl != null && !apiUrl.isEmpty()) {
                env.put("ANTHROPIC_BASE_URL", apiUrl);
            }
        }

        pb.command(command);

        log.info("Creating streaming Claude CLI process for agent: {}, model: {}", agentId, model);

        Process process = pb.start();
        ProcessInfo processInfo = new ProcessInfo(process);
        processes.put(agentId, processInfo);

        return processInfo;
    }

    /**
     * 流式执行命令
     * 实时解析JSON输出并回调
     */
    private void executeCommandStreaming(ProcessInfo processInfo, String message, String agentId,
                                         Consumer<StreamEvent> onEvent) {
        try {
            OutputStream stdin = processInfo.process.getOutputStream();
            stdin.write((message + "\n").getBytes());
            stdin.flush();
            // 关闭 stdin 以通知进程输入结束（--print 模式需要）
            stdin.close();

            StringBuilder finalResult = new StringBuilder();

            // 读取stderr（后台线程）
            StringBuilder stderrOutput = new StringBuilder();
            Thread stderrThread = new Thread(() -> {
                try {
                    BufferedReader stderr = new BufferedReader(
                        new InputStreamReader(processInfo.process.getErrorStream()));
                    String line;
                    while ((line = stderr.readLine()) != null) {
                        stderrOutput.append(line).append("\n");
                        log.debug("Claude stderr [{}]: {}", agentId, line);
                    }
                } catch (Exception e) {
                    log.debug("Stderr reader error: {}", e.getMessage());
                }
            });
            stderrThread.setDaemon(true);
            stderrThread.start();

            // 读取stdout（流式解析）
            BufferedReader stdout = new BufferedReader(
                new InputStreamReader(processInfo.process.getInputStream()));
            String line;

            // 当前正在构建的内容块
            String currentBlockType = null;
            StringBuilder currentBlockContent = new StringBuilder();
            String currentToolName = null;
            Map<String, Object> currentToolInput = null;

            while ((line = stdout.readLine()) != null) {
                if (line.isEmpty()) continue;

                try {
                    JsonNode node = objectMapper.readTree(line);
                    String type = node.has("type") ? node.get("type").asText() : "";

                    switch (type) {
                        case "system":
                            // 系统初始化消息，忽略
                            log.debug("System init: {}", node);
                            break;

                        case "assistant":
                            // 助手消息 - Claude CLI格式
                            JsonNode assistantMessage = node.get("message");
                            if (assistantMessage != null && assistantMessage.has("content")) {
                                JsonNode contentArray = assistantMessage.get("content");
                                if (contentArray.isArray()) {
                                    for (JsonNode contentItem : contentArray) {
                                        String contentType = contentItem.has("type") ? contentItem.get("type").asText() : "";
                                        if ("thinking".equals(contentType)) {
                                            String thinking = contentItem.has("thinking") ? contentItem.get("thinking").asText() : "";
                                            if (!thinking.isEmpty()) {
                                                onEvent.accept(StreamEvent.thinking(thinking));
                                            }
                                        } else if ("text".equals(contentType)) {
                                            String text = contentItem.has("text") ? contentItem.get("text").asText() : "";
                                            if (!text.isEmpty()) {
                                                onEvent.accept(StreamEvent.text(text));
                                                finalResult.append(text);
                                            }
                                        } else if ("tool_use".equals(contentType)) {
                                            String toolName = contentItem.has("name") ? contentItem.get("name").asText() : "";
                                            Map<String, Object> toolInput = new HashMap<>();
                                            if (contentItem.has("input")) {
                                                JsonNode inputNode = contentItem.get("input");
                                                if (inputNode.isObject()) {
                                                    inputNode.fields().forEachRemaining(entry -> {
                                                        toolInput.put(entry.getKey(), entry.getValue().asText());
                                                    });
                                                }
                                            }
                                            onEvent.accept(StreamEvent.toolUse(toolName, toolInput));
                                        }
                                    }
                                }
                            }
                            break;

                        case "result":
                            // 最终结果
                            String sessionNode = node.has("session_id") ? node.get("session_id").asText() : null;
                            if (sessionNode != null) {
                                sessionIds.put(agentId, sessionNode);
                            }

                            String resultContent = extractResultContent(node);
                            if (resultContent != null && !resultContent.isEmpty()) {
                                finalResult.setLength(0);
                                finalResult.append(resultContent);
                            }
                            break;

                        case "error":
                            // 错误
                            String errorMsg = node.has("error") ? node.get("error").asText() : "Unknown error";
                            onEvent.accept(StreamEvent.error(errorMsg));
                            break;

                        default:
                            log.debug("Unknown type: {}", type);
                    }
                } catch (Exception e) {
                    // 非JSON行，可能是普通文本
                    log.debug("Non-JSON line: {}", line);
                }
            }

            stderrThread.join(3000);

            // 完成
            String result = finalResult.toString().trim();
            if (result.isEmpty() && stderrOutput.length() > 0) {
                onEvent.accept(StreamEvent.error(stderrOutput.toString().trim()));
            } else {
                onEvent.accept(StreamEvent.complete(result.isEmpty() ? "No response from Claude" : result));
            }

        } catch (Exception e) {
            log.error("Error executing streaming command for agent: {}", agentId, e);
            onEvent.accept(StreamEvent.error("Error: " + e.getMessage()));
        } finally {
            // 确保进程被销毁，防止泄漏
            processes.remove(agentId);
            processInfo.destroy();
        }
    }

    /**
     * 提取result内容
     */
    private String extractResultContent(JsonNode node) {
        if (node.has("content")) {
            JsonNode content = node.get("content");
            if (content.isArray()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode item : content) {
                    if (item.has("type") && "text".equals(item.get("type").asText())) {
                        sb.append(item.get("text").asText());
                    }
                }
                return sb.toString();
            }
        }
        return null;
    }

    /**
     * 获取会话ID
     */
    public String getSessionId(String agentId) {
        return sessionIds.get(agentId);
    }

    /**
     * 生成 MCP 配置文件
     * 创建临时 JSON 文件，配置 MCP 服务器和用户 Token
     *
     * @param userToken 用户 JWT Token
     * @return MCP 配置文件，失败返回 null
     */
    private File generateMcpConfig(String userToken) {
        try {
            // MCP 服务器脚本路径
            String mcpServerPath = System.getProperty("user.dir") + "/mcp-server/index.js";

            // 检查 MCP 服务器脚本是否存在
            File mcpServerFile = new File(mcpServerPath);
            if (!mcpServerFile.exists()) {
                log.warn("MCP 服务器脚本不存在: {}", mcpServerPath);
                return null;
            }

            // 生成 MCP 配置 JSON
            String config = String.format("""
                {
                  "mcpServers": {
                    "chengxun-game-maker": {
                      "command": "node",
                      "args": ["%s"],
                      "env": {
                        "USER_TOKEN": "%s"
                      }
                    }
                  }
                }
                """, mcpServerPath, userToken);

            // 写入临时文件
            File tempFile = File.createTempFile("mcp-config-", ".json");
            tempFile.deleteOnExit();
            java.nio.file.Files.write(tempFile.toPath(), config.getBytes());

            log.info("MCP 配置文件已生成: {}", tempFile.getAbsolutePath());
            return tempFile;
        } catch (Exception e) {
            log.error("生成 MCP 配置文件失败", e);
            return null;
        }
    }

    /**
     * 应用关闭时清理所有子进程
     */
    @PreDestroy
    public void destroy() {
        log.info("Destroying StreamingClaudeCliEngine, cleaning up {} processes", processes.size());
        for (Map.Entry<String, ProcessInfo> entry : processes.entrySet()) {
            try {
                entry.getValue().destroy();
                log.info("Destroyed process for agent: {}", entry.getKey());
            } catch (Exception e) {
                log.warn("Failed to destroy process for agent {}: {}", entry.getKey(), e.getMessage());
            }
        }
        processes.clear();
        sessionIds.clear();
    }

    /**
     * 进程信息内部类
     */
    private static class ProcessInfo {
        final Process process;
        final long createdAt;

        ProcessInfo(Process process) {
            this.process = process;
            this.createdAt = System.currentTimeMillis();
        }

        boolean isAlive() {
            return process.isAlive();
        }

        void destroy() {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }
}
