package com.chengxun.gamemaker.model;

public class AgentDefinition {
    private String id;
    private String name;
    private String role;
    private String description;
    private String agentsFile;
    private String apiKey;
    private String apiUrl;
    private String model;
    private String sessionId;
    private String workDir;
    private AgentStatus status;
    private boolean parent;
    private String parentId;
    
    public enum AgentStatus {
        IDLE, WORKING, WAITING, ERROR, STOPPED
    }
    
    public AgentDefinition() {}
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private final AgentDefinition def = new AgentDefinition();
        
        public Builder id(String id) { def.id = id; return this; }
        public Builder name(String name) { def.name = name; return this; }
        public Builder role(String role) { def.role = role; return this; }
        public Builder description(String description) { def.description = description; return this; }
        public Builder agentsFile(String agentsFile) { def.agentsFile = agentsFile; return this; }
        public Builder apiKey(String apiKey) { def.apiKey = apiKey; return this; }
        public Builder apiUrl(String apiUrl) { def.apiUrl = apiUrl; return this; }
        public Builder model(String model) { def.model = model; return this; }
        public Builder sessionId(String sessionId) { def.sessionId = sessionId; return this; }
        public Builder workDir(String workDir) { def.workDir = workDir; return this; }
        public Builder status(AgentStatus status) { def.status = status; return this; }
        public Builder parent(boolean parent) { def.parent = parent; return this; }
        public Builder parentId(String parentId) { def.parentId = parentId; return this; }
        
        public AgentDefinition build() { return def; }
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getAgentsFile() { return agentsFile; }
    public void setAgentsFile(String agentsFile) { this.agentsFile = agentsFile; }
    
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    
    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
    
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getWorkDir() { return workDir; }
    public void setWorkDir(String workDir) { this.workDir = workDir; }
    
    public AgentStatus getStatus() { return status; }
    public void setStatus(AgentStatus status) { this.status = status; }
    
    public boolean isParent() { return parent; }
    public void setParent(boolean parent) { this.parent = parent; }
    
    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
}
