package com.chengxun.gamemaker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "game-maker.agents")
public class AgentConfig {
    private Map<String, AgentDefinition> definitions = new HashMap<>();
    
    public Map<String, AgentDefinition> getDefinitions() { return definitions; }
    public void setDefinitions(Map<String, AgentDefinition> definitions) { this.definitions = definitions; }
    
    public static class AgentDefinition {
        private String name;
        private String role;
        private String description;
        private String agentsFile;
        private String apiKey;
        private String apiUrl;
        private String model;
        private boolean autoStart = false;
        
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
        
        public boolean isAutoStart() { return autoStart; }
        public void setAutoStart(boolean autoStart) { this.autoStart = autoStart; }
    }
}
