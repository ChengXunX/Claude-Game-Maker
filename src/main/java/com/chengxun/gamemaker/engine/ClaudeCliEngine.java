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
    
    public String sendMessage(String agentId, String sessionId, String message, 
                              String workDir, String apiKey, String apiUrl, String model) {
        ProcessInfo processInfo = getOrCreateProcess(agentId, sessionId, workDir, apiKey, apiUrl, model);
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
        ProcessInfo existing = processes.get(agentId);
        if (existing != null && existing.isAlive()) {
            return existing;
        }
        
        try {
            return createProcess(agentId, sessionId, workDir, apiKey, apiUrl, model);
        } catch (Exception e) {
            log.error("Failed to create Claude CLI process for agent: {}", agentId, e);
            return null;
        }
    }
    
    private ProcessInfo createProcess(String agentId, String sessionId,
                                      String workDir, String apiKey, String apiUrl, String model) throws IOException {
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
        command.add("--dangerously-skip-permissions");
        
        if (model != null && !model.isEmpty()) {
            command.add("--model");
            command.add(model);
        }
        
        if (sessionId != null && !sessionId.isEmpty()) {
            command.add("--resume");
            command.add(sessionId);
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
            processes.remove(agentId);
            
            String result = output.toString().trim();
            if (result.isEmpty() && stderrOutput.length() > 0) {
                return "Error: " + stderrOutput.toString().trim();
            }
            
            return result.isEmpty() ? "No response from Claude" : result;
            
        } catch (Exception e) {
            log.error("Error executing command for agent: {}", agentId, e);
            processes.remove(agentId);
            return "Error: " + e.getMessage();
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
