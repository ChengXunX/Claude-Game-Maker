package com.chengxun.gamemaker.model;

import java.time.LocalDateTime;
import java.util.*;

public class AgentContext {
    private String agentId;
    private String sessionId;
    private String workDir;
    private String currentTaskId;
    private String projectSummary;
    private String apiKey;
    private String apiUrl;
    private String model;
    private List<WorkingMemoryItem> workingMemory = new ArrayList<>();
    private List<String> learnedPatterns = new ArrayList<>();
    private List<ApiConfigRecord> apiHistory = new ArrayList<>();
    private LocalDateTime lastActiveTime;
    private LocalDateTime createdAt;

    public AgentContext() {
        this.createdAt = LocalDateTime.now();
        this.lastActiveTime = LocalDateTime.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final AgentContext ctx = new AgentContext();

        public Builder agentId(String agentId) { ctx.agentId = agentId; return this; }
        public Builder sessionId(String sessionId) { ctx.sessionId = sessionId; return this; }
        public Builder workDir(String workDir) { ctx.workDir = workDir; return this; }
        public Builder currentTaskId(String currentTaskId) { ctx.currentTaskId = currentTaskId; return this; }
        public Builder projectSummary(String projectSummary) { ctx.projectSummary = projectSummary; return this; }
        public Builder lastActiveTime(LocalDateTime lastActiveTime) { ctx.lastActiveTime = lastActiveTime; return this; }
        public AgentContext build() { return ctx; }
    }

    public void addWorkingMemory(String key, String value) {
        workingMemory.removeIf(item -> item.getKey().equals(key));
        workingMemory.add(new WorkingMemoryItem(key, value, LocalDateTime.now()));
        if (workingMemory.size() > 50) {
            workingMemory.remove(0);
        }
    }

    public void addLearnedPattern(String pattern) {
        if (!learnedPatterns.contains(pattern)) {
            learnedPatterns.add(pattern);
        }
    }

    public void addApiConfig(String apiUrl, String model) {
        apiHistory.add(new ApiConfigRecord(apiUrl, model, LocalDateTime.now()));
    }

    public String getWorkingMemory(String key) {
        return workingMemory.stream()
            .filter(item -> item.getKey().equals(key))
            .findFirst()
            .map(WorkingMemoryItem::getValue)
            .orElse(null);
    }

    /**
     * 获取所有工作记忆（key-value 格式）
     *
     * @return 工作记忆的 Map 视图
     */
    public Map<String, String> getAllWorkingMemory() {
        Map<String, String> result = new LinkedHashMap<>();
        for (WorkingMemoryItem item : workingMemory) {
            result.put(item.getKey(), item.getValue());
        }
        return result;
    }

    /**
     * 移除指定 key 的工作记忆
     *
     * @param key 要移除的记忆 key
     */
    public void removeWorkingMemory(String key) {
        workingMemory.removeIf(item -> item.getKey().equals(key));
    }

    public void touch() {
        this.lastActiveTime = LocalDateTime.now();
    }

    // Getters and Setters
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getWorkDir() { return workDir; }
    public void setWorkDir(String workDir) { this.workDir = workDir; }

    public String getCurrentTaskId() { return currentTaskId; }
    public void setCurrentTaskId(String currentTaskId) { this.currentTaskId = currentTaskId; }

    public String getProjectSummary() { return projectSummary; }
    public void setProjectSummary(String projectSummary) { this.projectSummary = projectSummary; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public List<WorkingMemoryItem> getWorkingMemory() { return workingMemory; }
    public void setWorkingMemory(List<WorkingMemoryItem> workingMemory) { this.workingMemory = workingMemory; }

    public List<String> getLearnedPatterns() { return learnedPatterns; }
    public void setLearnedPatterns(List<String> learnedPatterns) { this.learnedPatterns = learnedPatterns; }

    public List<ApiConfigRecord> getApiHistory() { return apiHistory; }
    public void setApiHistory(List<ApiConfigRecord> apiHistory) { this.apiHistory = apiHistory; }

    public LocalDateTime getLastActiveTime() { return lastActiveTime; }
    public void setLastActiveTime(LocalDateTime lastActiveTime) { this.lastActiveTime = lastActiveTime; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static class WorkingMemoryItem {
        private String key;
        private String value;
        private LocalDateTime updatedAt;

        public WorkingMemoryItem() {}
        public WorkingMemoryItem(String key, String value, LocalDateTime updatedAt) {
            this.key = key;
            this.value = value;
            this.updatedAt = updatedAt;
        }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    public static class ApiConfigRecord {
        private String apiUrl;
        private String model;
        private LocalDateTime usedAt;

        public ApiConfigRecord() {}
        public ApiConfigRecord(String apiUrl, String model, LocalDateTime usedAt) {
            this.apiUrl = apiUrl;
            this.model = model;
            this.usedAt = usedAt;
        }

        public String getApiUrl() { return apiUrl; }
        public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public LocalDateTime getUsedAt() { return usedAt; }
        public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }
    }
}
