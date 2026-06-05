package com.chengxun.gamemaker.engine;

import com.chengxun.gamemaker.config.AppConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    
    private final Map<String, ProcessInfo> processes = new ConcurrentHashMap<>();
    private final Map<String, String> sessionIds = new ConcurrentHashMap<>();
    
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
        ProcessInfo processInfo = getOrCreateProcess(agentId, sessionId, workDir, apiKey, apiUrl, model, mcpConfig);
        if (processInfo == null) {
            return "Failed to create Claude CLI process";
        }

        return executeCommand(processInfo, message, agentId);
    }
    
    public String getSessionId(String agentId) {
        return sessionIds.get(agentId);
    }
    
    private ProcessInfo getOrCreateProcess(String agentId, String sessionId,
                                           String workDir, String apiKey, String apiUrl, String model) {
        return getOrCreateProcess(agentId, sessionId, workDir, apiKey, apiUrl, model, null);
    }

    private ProcessInfo getOrCreateProcess(String agentId, String sessionId,
                                           String workDir, String apiKey, String apiUrl, String model,
                                           String mcpConfig) {
        ProcessInfo existing = processes.get(agentId);
        if (existing != null && existing.isAlive()) {
            return existing;
        }

        try {
            return createProcess(agentId, sessionId, workDir, apiKey, apiUrl, model, mcpConfig);
        } catch (Exception e) {
            log.error("Failed to create Claude CLI process for agent: {}", agentId, e);
            return null;
        }
    }
    
    private ProcessInfo createProcess(String agentId, String sessionId,
                                      String workDir, String apiKey, String apiUrl, String model) throws IOException {
        return createProcess(agentId, sessionId, workDir, apiKey, apiUrl, model, null);
    }

    private ProcessInfo createProcess(String agentId, String sessionId,
                                      String workDir, String apiKey, String apiUrl, String model,
                                      String mcpConfig) throws IOException {
        String installPath = appConfig.getClaude().getInstallPath();

        ProcessBuilder pb = new ProcessBuilder();
        pb.redirectErrorStream(false);

        if (workDir != null && !workDir.isEmpty()) {
            pb.directory(new File(workDir));
        }

        java.util.List<String> command = new java.util.ArrayList<>();
        command.add(installPath);
        command.add("--print");
        command.add("--input-format");
        command.add("text");
        command.add("--output-format");
        command.add("stream-json");
        command.add("--verbose");

        if (model != null && !model.isEmpty()) {
            command.add("--model");
            command.add(model);
        }

        if (sessionId != null && !sessionId.isEmpty()) {
            command.add("--resume");
            command.add(sessionId);
        }

        // 注入 MCP 配置
        if (mcpConfig != null && !mcpConfig.isEmpty()) {
            try {
                // 写入临时文件
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
        
        Map<String, String> env = pb.environment();
        if (apiKey != null && !apiKey.isEmpty()) {
            env.put("ANTHROPIC_API_KEY", apiKey);
        }
        if (apiUrl != null && !apiUrl.isEmpty()) {
            env.put("ANTHROPIC_BASE_URL", apiUrl);
        }
        
        log.info("Creating Claude CLI process for agent: {}, command: {}", agentId, command);
        
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
            
            stderrThread.join(3000);

            String result = output.toString().trim();
            if (result.isEmpty() && stderrOutput.length() > 0) {
                return "Error: " + stderrOutput.toString().trim();
            }

            return result.isEmpty() ? "No response from Claude" : result;

        } catch (Exception e) {
            log.error("Error executing command for agent: {}", agentId, e);
            return "Error: " + e.getMessage();
        } finally {
            // 确保进程被销毁，防止泄漏
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
