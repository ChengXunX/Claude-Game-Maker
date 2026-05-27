package com.chengxun.gamemaker.manager;

import com.chengxun.gamemaker.config.AppConfig;
import com.chengxun.gamemaker.model.AgentContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class ContextManager {

    private static final Logger log = LoggerFactory.getLogger(ContextManager.class);
    private static final DateTimeFormatter SNAPSHOT_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final AppConfig appConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ContextManager(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    // ===== AgentContext 主文件 =====

    public void saveContext(AgentContext context) {
        Path contextPath = getContextPath(context.getAgentId());
        try {
            Files.createDirectories(contextPath.getParent());
            context.touch();
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(contextPath.toFile(), context);
            log.debug("Context saved for agent: {}", context.getAgentId());
        } catch (IOException e) {
            log.error("Failed to save context for agent: {}", context.getAgentId(), e);
        }
    }

    public AgentContext loadContext(String agentId) {
        Path contextPath = getContextPath(agentId);
        if (!Files.exists(contextPath)) {
            return null;
        }
        try {
            return objectMapper.readValue(contextPath.toFile(), AgentContext.class);
        } catch (IOException e) {
            log.error("Failed to load context for agent: {}", agentId, e);
            return null;
        }
    }

    public AgentContext getOrCreateContext(String agentId) {
        AgentContext ctx = loadContext(agentId);
        if (ctx == null) {
            ctx = AgentContext.builder()
                .agentId(agentId)
                .build();
            saveContext(ctx);
        }
        return ctx;
    }

    public void updateSessionId(String agentId, String sessionId) {
        AgentContext ctx = getOrCreateContext(agentId);
        ctx.setSessionId(sessionId);
        saveContext(ctx);
    }

    // ===== 对话历史 =====

    public void saveConversation(String agentId, String conversationId, List<ConversationMessage> messages) {
        Path convPath = getConversationPath(agentId, conversationId);
        try {
            Files.createDirectories(convPath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(convPath.toFile(), messages);
            log.debug("Conversation saved for agent: {}, conv: {}", agentId, conversationId);
        } catch (IOException e) {
            log.error("Failed to save conversation for agent: {}", agentId, e);
        }
    }

    public List<ConversationMessage> loadConversation(String agentId, String conversationId) {
        Path convPath = getConversationPath(agentId, conversationId);
        if (!Files.exists(convPath)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(convPath.toFile(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, ConversationMessage.class));
        } catch (IOException e) {
            log.error("Failed to load conversation for agent: {}", agentId, e);
            return new ArrayList<>();
        }
    }

    public List<String> listConversations(String agentId) {
        Path convDir = getConversationsDir(agentId);
        if (!Files.exists(convDir)) {
            return new ArrayList<>();
        }
        try {
            return Files.list(convDir)
                .filter(p -> p.toString().endsWith(".json"))
                .map(p -> p.getFileName().toString().replace(".json", ""))
                .sorted(Comparator.reverseOrder())
                .toList();
        } catch (IOException e) {
            log.error("Failed to list conversations for agent: {}", agentId, e);
            return new ArrayList<>();
        }
    }

    public List<ConversationMessage> getRecentMessages(String agentId, int limit) {
        List<String> convIds = listConversations(agentId);
        List<ConversationMessage> result = new ArrayList<>();
        for (String convId : convIds) {
            List<ConversationMessage> msgs = loadConversation(agentId, convId);
            result.addAll(msgs);
            if (result.size() >= limit) break;
        }
        if (result.size() > limit) {
            return result.subList(result.size() - limit, result.size());
        }
        return result;
    }

    // ===== 上下文快照 =====

    public void createSnapshot(String agentId, AgentContext context, List<ConversationMessage> recentMessages) {
        String timestamp = LocalDateTime.now().format(SNAPSHOT_FORMAT);
        Path snapshotPath = getSnapshotPath(agentId, timestamp);
        try {
            Files.createDirectories(snapshotPath.getParent());
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("context", context);
            snapshot.put("recentMessages", recentMessages);
            snapshot.put("createdAt", LocalDateTime.now().toString());
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(snapshotPath.toFile(), snapshot);
            log.info("Snapshot created for agent: {} at {}", agentId, timestamp);
        } catch (IOException e) {
            log.error("Failed to create snapshot for agent: {}", agentId, e);
        }
    }

    public Map<String, Object> loadLatestSnapshot(String agentId) {
        Path snapshotDir = getSnapshotsDir(agentId);
        if (!Files.exists(snapshotDir)) {
            return null;
        }
        try {
            Optional<Path> latest = Files.list(snapshotDir)
                .filter(p -> p.toString().endsWith(".json"))
                .sorted(Comparator.reverseOrder())
                .findFirst();
            if (latest.isPresent()) {
                return objectMapper.readValue(latest.get().toFile(), Map.class);
            }
        } catch (IOException e) {
            log.error("Failed to load snapshot for agent: {}", agentId, e);
        }
        return null;
    }

    public void cleanupOldSnapshots(String agentId, int keepCount) {
        Path snapshotDir = getSnapshotsDir(agentId);
        if (!Files.exists(snapshotDir)) return;
        try {
            List<Path> snapshots = Files.list(snapshotDir)
                .filter(p -> p.toString().endsWith(".json"))
                .sorted(Comparator.reverseOrder())
                .toList();
            for (int i = keepCount; i < snapshots.size(); i++) {
                Files.deleteIfExists(snapshots.get(i));
            }
        } catch (IOException e) {
            log.error("Failed to cleanup snapshots for agent: {}", agentId, e);
        }
    }

    // ===== 路径方法 =====

    private Path getContextPath(String agentId) {
        return Path.of(appConfig.getContextsDir(), agentId, "context.json");
    }

    private Path getConversationsDir(String agentId) {
        return Path.of(appConfig.getContextsDir(), agentId, "conversations");
    }

    private Path getConversationPath(String agentId, String conversationId) {
        return Path.of(appConfig.getContextsDir(), agentId, "conversations", conversationId + ".json");
    }

    private Path getSnapshotsDir(String agentId) {
        return Path.of(appConfig.getContextsDir(), agentId, "snapshots");
    }

    private Path getSnapshotPath(String agentId, String timestamp) {
        return Path.of(appConfig.getContextsDir(), agentId, "snapshots", timestamp + ".json");
    }

    // ===== 内部类 =====

    public static class ConversationMessage {
        private String role;    // "user" | "assistant" | "system"
        private String content;
        private String timestamp;
        private String taskId;

        public ConversationMessage() {}
        public ConversationMessage(String role, String content) {
            this.role = role;
            this.content = content;
            this.timestamp = LocalDateTime.now().toString();
        }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
    }
}
