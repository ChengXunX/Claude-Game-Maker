package com.chengxun.gamemaker.agent;

import com.chengxun.gamemaker.model.AgentDefinition;
import com.chengxun.gamemaker.model.AgentMessage;
import com.chengxun.gamemaker.model.TaskAssignment;

import java.util.List;

public interface Agent {
    String getId();
    String getName();
    String getRole();
    AgentDefinition getDefinition();
    
    // Lifecycle
    void initialize();
    void start();
    void stop();
    boolean isBusy();
    boolean isAlive();
    
    // Work
    void work();                    // Main work loop (called by scheduler)
    String sendMessage(String message);  // Send message to Claude CLI
    
    // Communication
    void receiveMessage(AgentMessage message);
    List<AgentMessage> getPendingMessages();
    void sendMessage(AgentMessage message);
    
    // Task management
    void assignTask(TaskAssignment task);
    List<TaskAssignment> getTasks();
    void reportProgress(String taskId, String progress);
    
    // Context and memory
    void saveContext();
    void loadContext();
    void saveMemory(String key, String value);
    String loadMemory(String key);
}
