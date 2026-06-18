package com.chengxun.gamemaker.engine;

import com.chengxun.gamemaker.config.AppConfig;
import com.chengxun.gamemaker.web.service.ResourceUsageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ClaudeCliEngine {

    private static final Logger log = LoggerFactory.getLogger(ClaudeCliEngine.class);

    private final AppConfig appConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired(required = false)
    private ResourceUsageService resourceUsageService;

    private final Map<String, ProcessInfo> processes = new ConcurrentHashMap<>();
    private final Map<String, String> sessionIds = new ConcurrentHashMap<>();

    /**
     * AI 调用结果（含精确 Token 计量）
     * 用于从 Claude CLI stream-json 输出中提取真实 token 使用量
     */
    public static class AiCallResult {
        /** 响应文本 */
        public final String response;
        /** 输入 Token 数（从 API 返回的真实值） */
        public final long inputTokens;
        /** 输出 Token 数（从 API 返回的真实值） */
        public final long outputTokens;
        /** 调用耗时（毫秒） */
        public final long durationMs;
        /** 是否有精确 token 数据 */
        public final boolean hasPreciseTokens;

        public AiCallResult(String response, long inputTokens, long outputTokens, long durationMs, boolean hasPreciseTokens) {
            this.response = response;
            this.inputTokens = inputTokens;
            this.outputTokens = outputTokens;
            this.durationMs = durationMs;
            this.hasPreciseTokens = hasPreciseTokens;
        }

        /** 从估算值创建（fallback） */
        public static AiCallResult fromEstimate(String response, int inputChars, int outputChars) {
            long estInput = Math.max(1, inputChars / 3);
            long estOutput = Math.max(1, outputChars / 3);
            return new AiCallResult(response, estInput, estOutput, 0, false);
        }
    }

    public ClaudeCliEngine(AppConfig appConfig) {
        this.appConfig = appConfig;
    }
    
    @PostConstruct
    public void init() {
        log.info("ClaudeCliEngine initialized, CLI path: {}", appConfig.getClaude().getInstallPath());
    }
    
    @PreDestroy
    public void destroy() {
        log.info("Destroying all Claude CLI processes...");
        processes.values().forEach(ProcessInfo::destroy);
        processes.clear();
    }

    /**
     * 销毁指定 Agent 的 CLI 进程
     * 当 Agent 的 API 配置变更时调用，下次发送消息时会创建新进程
     *
     * @param agentId Agent ID
     */
    public void destroyProcess(String agentId) {
        ProcessInfo processInfo = processes.remove(agentId);
        if (processInfo != null) {
            processInfo.destroy();
            log.info("Destroyed Claude CLI process for agent: {}", agentId);
        }
    }

    /**
     * 检查指定 Agent 的 CLI 进程是否存活
     *
     * @param agentId Agent ID
     * @return 进程是否存活
     */
    public boolean isProcessAlive(String agentId) {
        ProcessInfo processInfo = processes.get(agentId);
        return processInfo != null && processInfo.isAlive();
    }

    public String sendMessage(String agentId, String sessionId, String message,
                              String workDir, String apiKey, String apiUrl, String model) {
        return sendMessage(agentId, sessionId, message, workDir, apiKey, apiUrl, model, null);
    }

    /**
     * 发送消息到 Claude CLI（带 MCP 配置）
     *
     * @param agentId    Agent ID
     * @param sessionId  会话 ID
     * @param message    消息内容
     * @param workDir    工作目录
     * @param apiKey     API Key
     * @param apiUrl     API URL
     * @param model      模型
     * @param mcpConfig  MCP 配置 JSON（可选）
     * @return Claude 的响应
     */
    public String sendMessage(String agentId, String sessionId, String message,
                              String workDir, String apiKey, String apiUrl, String model,
                              String mcpConfig) {
        return sendMessage(agentId, sessionId, message, workDir, apiKey, apiUrl, model, mcpConfig, "high");
    }

    /**
     * 发送消息（带 effort 参数）
     */
    public String sendMessage(String agentId, String sessionId, String message,
                              String workDir, String apiKey, String apiUrl, String model,
                              String mcpConfig, String effort) {
        AiCallResult result = sendMessageWithTokenUsage(agentId, sessionId, message, workDir, apiKey, apiUrl, model, mcpConfig, effort);
        return result.response;
    }

    /**
     * 发送消息并返回精确的 Token 使用量
     */
    public AiCallResult sendMessageWithTokenUsage(String agentId, String sessionId, String message,
                                                   String workDir, String apiKey, String apiUrl, String model,
                                                   String mcpConfig) {
        return sendMessageWithTokenUsage(agentId, sessionId, message, workDir, apiKey, apiUrl, model, mcpConfig, "high");
    }

    /**
     * 发送消息并返回精确的 Token 使用量（带 effort 参数）
     *
     * @param agentId   Agent ID
     * @param sessionId 会话 ID
     * @param message   消息内容
     * @param workDir   工作目录
     * @param apiKey    API Key
     * @param apiUrl    API URL
     * @param model     模型
     * @param mcpConfig MCP 配置 JSON（可选）
     * @param effort    推理努力级别: low, medium, high, xhigh, max
     * @return AiCallResult 包含响应文本和精确 token 数据
     */
    public AiCallResult sendMessageWithTokenUsage(String agentId, String sessionId, String message,
                                                   String workDir, String apiKey, String apiUrl, String model,
                                                   String mcpConfig, String effort) {
        ProcessInfo processInfo = getOrCreateProcess(agentId, sessionId, workDir, apiKey, apiUrl, model, mcpConfig, effort);
        if (processInfo == null) {
            return AiCallResult.fromEstimate("Failed to create Claude CLI process", 0, 0);
        }

        return executeCommandWithTokenUsage(processInfo, message, agentId);
    }

    public String getSessionId(String agentId) {
        return sessionIds.get(agentId);
    }

    private ProcessInfo getOrCreateProcess(String agentId, String sessionId,
                                           String workDir, String apiKey, String apiUrl, String model,
                                           String mcpConfig, String effort) {
        ProcessInfo existing = processes.get(agentId);
        if (existing != null && existing.isAlive()) {
            return existing;
        }

        try {
            return createProcess(agentId, sessionId, workDir, apiKey, apiUrl, model, mcpConfig, effort);
        } catch (Exception e) {
            log.error("Failed to create Claude CLI process for agent: {}", agentId, e);
            return null;
        }
    }

    private ProcessInfo createProcess(String agentId, String sessionId,
                                      String workDir, String apiKey, String apiUrl, String model,
                                      String mcpConfig, String effort) throws IOException {
        String installPath = appConfig.getClaude().getInstallPath();

        // API 配置：优先使用传入值（Token 分配），否则 fallback 到全局配置
        // 必须显式设置，防止继承系统环境变量导致 Token 分配无效
        String keySource = "system-env";

        if (apiKey != null && !apiKey.isEmpty()) {
            keySource = "token-assigned";
        } else if (appConfig.getApiKey() != null && !appConfig.getApiKey().isEmpty()) {
            apiKey = appConfig.getApiKey();
            keySource = "global-config";
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

        java.util.List<String> command = new java.util.ArrayList<>();

        // Claude CLI 禁止 root 使用 --dangerously-skip-permissions
        // 检测 root 时以项目目录所有者身份运行
        boolean isRoot = "root".equals(System.getProperty("user.name"));

        if (isRoot) {
            // 获取项目目录的所有者作为运行用户（支持任意部署用户）
            String runUser = System.getenv("RUN_USER");
            if (runUser == null || runUser.isEmpty() || "root".equals(runUser)) {
                try {
                    java.nio.file.Path projectDir = java.nio.file.Paths.get(System.getProperty("user.dir"));
                    // 优先使用 Files.getOwner()，更兼容各种文件系统
                    java.nio.file.attribute.UserPrincipal owner = java.nio.file.Files.getOwner(projectDir);
                    if (owner != null) {
                        runUser = owner.getName();
                        log.debug("Detected project directory owner via getOwner(): {}", runUser);
                    } else {
                        // fallback: 尝试 unix:ownerName 属性
                        runUser = (String) java.nio.file.Files.getAttribute(projectDir, "unix:ownerName");
                        log.debug("Detected project directory owner via getAttribute(): {}", runUser);
                    }
                } catch (Exception e) {
                    log.warn("Failed to detect project directory owner, falling back to chengxun: {}", e.getMessage());
                    runUser = "chengxun";
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
            // 推理努力级别：将 Agent 推理深度映射到 CLI effort 参数
            if (effort != null && !effort.isEmpty()) {
                cmd.append(" --effort ").append(effort);
            }
            if (sessionId != null && !sessionId.isEmpty()) {
                cmd.append(" --resume ").append(sessionId);
            }
            if (mcpConfig != null && !mcpConfig.isEmpty()) {
                try {
                    java.nio.file.Path mcpFile = java.nio.file.Path.of(
                        System.getProperty("java.io.tmpdir"),
                        "mcp-" + agentId.replaceAll("[^a-zA-Z0-9-]", "_") + ".json"
                    );
                    java.nio.file.Files.writeString(mcpFile, mcpConfig);
                    cmd.append(" --mcp-config ").append(mcpFile.toAbsolutePath());
                    log.info("MCP config injected for agent: {}", agentId);
                } catch (Exception e) {
                    log.warn("Failed to write MCP config for agent {}: {}", agentId, e.getMessage());
                }
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

            // 推理努力级别：将 Agent 推理深度映射到 CLI effort 参数
            if (effort != null && !effort.isEmpty()) {
                command.add("--effort");
                command.add(effort);
            }

            if (sessionId != null && !sessionId.isEmpty()) {
                command.add("--resume");
                command.add(sessionId);
            }

            // 注入 MCP 配置
            if (mcpConfig != null && !mcpConfig.isEmpty()) {
                try {
                    java.nio.file.Path mcpFile = java.nio.file.Path.of(
                        System.getProperty("java.io.tmpdir"),
                        "mcp-" + agentId.replaceAll("[^a-zA-Z0-9-]", "_") + ".json"
                    );
                    java.nio.file.Files.writeString(mcpFile, mcpConfig);
                    command.add("--mcp-config");
                    command.add(mcpFile.toString());
                    log.info("MCP config injected for agent: {}", agentId);
                } catch (Exception e) {
                    log.warn("Failed to write MCP config for agent {}: {}", agentId, e.getMessage());
                }
            }

            pb.command(command);

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

        log.info("Creating Claude CLI process for agent: {}, keySource: {}, model: {}", agentId, keySource, model);

        Process process = pb.start();
        ProcessInfo processInfo = new ProcessInfo(process);
        processes.put(agentId, processInfo);

        return processInfo;
    }
    
    private String executeCommand(ProcessInfo processInfo, String message, String agentId) {
        try {
            OutputStream stdin = processInfo.process.getOutputStream();
            stdin.write((message + "\n").getBytes());
            stdin.flush();
            // 关闭 stdin 以通知进程输入结束（--print 模式需要）
            stdin.close();
            
            StringBuilder output = new StringBuilder();
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
            
            BufferedReader stdout = new BufferedReader(
                new InputStreamReader(processInfo.process.getInputStream()));
            String line;
            
            while ((line = stdout.readLine()) != null) {
                if (line.isEmpty()) continue;
                
                try {
                    JsonNode node = objectMapper.readTree(line);
                    String type = node.has("type") ? node.get("type").asText() : "";
                    
                    if ("assistant".equals(type)) {
                        JsonNode messageNode = node.get("message");
                        if (messageNode != null) {
                            JsonNode contentNode = messageNode.get("content");
                            if (contentNode != null && contentNode.isArray()) {
                                for (JsonNode item : contentNode) {
                                    if ("text".equals(item.get("type").asText())) {
                                        output.append(item.get("text").asText());
                                    }
                                }
                            }
                        }
                    } else if ("result".equals(type)) {
                        JsonNode sessionNode = node.get("session_id");
                        if (sessionNode != null) {
                            sessionIds.put(agentId, sessionNode.asText());
                        }
                        
                        JsonNode resultContent = node.get("content");
                        if (resultContent != null && resultContent.isArray()) {
                            for (JsonNode item : resultContent) {
                                if (item.has("type") && "text".equals(item.get("type").asText())) {
                                    output.append(item.get("text").asText());
                                }
                            }
                        }
                        
                        break;
                    }
                } catch (Exception e) {
                    output.append(line).append("\n");
                }
            }
            
            stderrThread.join(10000);

            int exitCode = processInfo.process.waitFor();
            String result = output.toString().trim();
            String stderr = stderrOutput.toString().trim();

            if (result.isEmpty()) {
                log.warn("Claude CLI returned empty result [{}]: exitCode={}, stderr={}", agentId, exitCode, stderr.isEmpty() ? "(empty)" : stderr);
                if (!stderr.isEmpty()) {
                    return "Error: " + stderr;
                }
                return "No response from Claude (exitCode=" + exitCode + ", agentId=" + agentId + "). Check logs for details.";
            }

            // 记录Token使用量（估算）
            if (resourceUsageService != null && !result.isEmpty() && !result.startsWith("Error:")) {
                try {
                    // 估算token使用量：输入约等于消息长度/4，输出约等于响应长度/4
                    long inputTokens = Math.max(1, message.length() / 4);
                    long outputTokens = Math.max(1, result.length() / 4);
                    // 从agentId中提取projectId（格式：projectId:role）
                    String projectId = agentId.contains(":") ? agentId.split(":")[0] : null;
                    resourceUsageService.recordTokenUsage(agentId, agentId, inputTokens, outputTokens, null, projectId);
                    log.debug("Token usage recorded for agent {}: input={}, output={}", agentId, inputTokens, outputTokens);
                } catch (Exception e) {
                    log.debug("Failed to record token usage: {}", e.getMessage());
                }
            }

            return result.isEmpty() ? "No response from Claude (agentId=" + agentId + ")" : result;

        } catch (Exception e) {
            log.error("Error executing command for agent: {}", agentId, e);
            return "Error: " + e.getMessage();
        } finally {
            // 确保进程被销毁，防止泄漏
            processes.remove(agentId);
            processInfo.destroy();
        }
    }

    /**
     * 执行命令并返回精确 Token 使用量
     * 解析 Claude CLI stream-json 输出中的 usage 字段
     *
     * Claude CLI result 类型输出格式：
     * {"type":"result","session_id":"...","content":[...],"usage":{"input_tokens":N,"output_tokens":N},"cost_usd":N,"duration_ms":N}
     */
    private AiCallResult executeCommandWithTokenUsage(ProcessInfo processInfo, String message, String agentId) {
        long startTime = System.currentTimeMillis();
        try {
            OutputStream stdin = processInfo.process.getOutputStream();
            stdin.write((message + "\n").getBytes());
            stdin.flush();
            stdin.close();

            StringBuilder output = new StringBuilder();
            StringBuilder stderrOutput = new StringBuilder();
            long inputTokens = 0;
            long outputTokens = 0;
            boolean hasPreciseTokens = false;

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

            BufferedReader stdout = new BufferedReader(
                new InputStreamReader(processInfo.process.getInputStream()));
            String line;

            while ((line = stdout.readLine()) != null) {
                if (line.isEmpty()) continue;

                try {
                    JsonNode node = objectMapper.readTree(line);
                    String type = node.has("type") ? node.get("type").asText() : "";

                    if ("assistant".equals(type)) {
                        JsonNode messageNode = node.get("message");
                        if (messageNode != null) {
                            JsonNode contentNode = messageNode.get("content");
                            if (contentNode != null && contentNode.isArray()) {
                                for (JsonNode item : contentNode) {
                                    if ("text".equals(item.get("type").asText())) {
                                        output.append(item.get("text").asText());
                                    }
                                }
                            }
                        }
                    } else if ("result".equals(type)) {
                        JsonNode sessionNode = node.get("session_id");
                        if (sessionNode != null) {
                            sessionIds.put(agentId, sessionNode.asText());
                        }

                        JsonNode resultContent = node.get("content");
                        if (resultContent != null && resultContent.isArray()) {
                            for (JsonNode item : resultContent) {
                                if (item.has("type") && "text".equals(item.get("type").asText())) {
                                    output.append(item.get("text").asText());
                                }
                            }
                        }

                        // 解析真实 token 使用量
                        JsonNode usageNode = node.get("usage");
                        if (usageNode != null) {
                            if (usageNode.has("input_tokens")) {
                                inputTokens = usageNode.get("input_tokens").asLong();
                            }
                            if (usageNode.has("output_tokens")) {
                                outputTokens = usageNode.get("output_tokens").asLong();
                            }
                            hasPreciseTokens = inputTokens > 0 || outputTokens > 0;
                            if (hasPreciseTokens) {
                                log.info("Precise token usage for agent {}: input={}, output={}",
                                    agentId, inputTokens, outputTokens);
                            }
                        }

                        break;
                    }
                } catch (Exception e) {
                    output.append(line).append("\n");
                }
            }

            stderrThread.join(10000);

            int exitCode = processInfo.process.waitFor();
            long duration = System.currentTimeMillis() - startTime;
            String result = output.toString().trim();
            String stderr = stderrOutput.toString().trim();

            if (result.isEmpty()) {
                log.warn("Claude CLI returned empty result (with token tracking) [{}]: exitCode={}, stderr={}", agentId, exitCode, stderr.isEmpty() ? "(empty)" : stderr);
                if (!stderr.isEmpty()) {
                    result = "Error: " + stderr;
                } else {
                    result = "No response from Claude (exitCode=" + exitCode + ", agentId=" + agentId + "). Check logs for details.";
                }
            }

            // 如果没有精确 token 数据，使用估算值
            if (!hasPreciseTokens && !result.isEmpty() && !result.startsWith("Error:")) {
                inputTokens = Math.max(1, message.length() / 3);
                outputTokens = Math.max(1, result.length() / 3);
                log.debug("Estimated token usage for agent {}: input={}, output={}", agentId, inputTokens, outputTokens);
            }

            // 记录到资源使用服务
            if (resourceUsageService != null && !result.isEmpty() && !result.startsWith("Error:")) {
                try {
                    String projectId = agentId.contains(":") ? agentId.split(":")[0] : null;
                    resourceUsageService.recordTokenUsage(agentId, agentId, inputTokens, outputTokens, null, projectId);
                } catch (Exception e) {
                    log.debug("Failed to record token usage: {}", e.getMessage());
                }
            }

            return new AiCallResult(
                result.isEmpty() ? "No response from Claude" : result,
                inputTokens, outputTokens, duration, hasPreciseTokens
            );

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Error executing command for agent: {}", agentId, e);
            return new AiCallResult("Error: " + e.getMessage(), 0, 0, duration, false);
        } finally {
            processes.remove(agentId);
            processInfo.destroy();
        }
    }
    
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
