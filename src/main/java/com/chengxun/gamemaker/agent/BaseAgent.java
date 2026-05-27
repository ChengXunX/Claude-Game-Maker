package com.chengxun.gamemaker.agent;

import com.chengxun.gamemaker.engine.ClaudeCliEngine;
import com.chengxun.gamemaker.engine.MessageBus;
import com.chengxun.gamemaker.manager.ContextManager;
import com.chengxun.gamemaker.manager.MemoryManager;
import com.chengxun.gamemaker.model.AgentDefinition;
import com.chengxun.gamemaker.model.AgentMessage;
import com.chengxun.gamemaker.model.TaskAssignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class BaseAgent implements Agent {
    
    protected static final Logger log = LoggerFactory.getLogger(BaseAgent.class);
    
    protected final AgentDefinition definition;
    protected final ClaudeCliEngine cliEngine;
    protected final MessageBus messageBus;
    protected final ContextManager contextManager;
    protected final MemoryManager memoryManager;
    
    protected final ConcurrentLinkedQueue<AgentMessage> pendingMessages = new ConcurrentLinkedQueue<>();
    protected final List<TaskAssignment> tasks = new ArrayList<>();
    
    protected volatile boolean busy = false;
    protected volatile boolean running = false;
    
    protected BaseAgent(AgentDefinition definition, 
                       ClaudeCliEngine cliEngine,
                       MessageBus messageBus,
                       ContextManager contextManager,
                       MemoryManager memoryManager) {
        this.definition = definition;
        this.cliEngine = cliEngine;
        this.messageBus = messageBus;
        this.contextManager = contextManager;
        this.memoryManager = memoryManager;
    }
    
    @Override
    public String getId() {
        return definition.getId();
    }
    
    @Override
    public String getName() {
        return definition.getName();
    }
    
    @Override
    public String getRole() {
        return definition.getRole();
    }
    
    @Override
    public AgentDefinition getDefinition() {
        return definition;
    }
    
    @Override
    public void initialize() {
        log.info("Initializing agent: {} ({})", getName(), getId());
        loadContext();
        loadMemory("default");
        
        messageBus.registerAgent(this);
        
        log.info("Agent initialized: {}", getName());
    }
    
    @Override
    public void start() {
        if (running) {
            log.warn("Agent {} is already running", getName());
            return;
        }
        running = true;
        log.info("Agent started: {}", getName());
    }
    
    @Override
    public void stop() {
        running = false;
        log.info("Agent stopped: {}", getName());
    }
    
    @Override
    public boolean isBusy() {
        return busy;
    }
    
    @Override
    public boolean isAlive() {
        return running;
    }
    
    @Override
    public void work() {
        if (!running || busy) {
            return;
        }
        
        busy = true;
        try {
            processPendingMessages();
            doWork();
        } catch (Exception e) {
            log.error("Error in agent work: {}", getName(), e);
        } finally {
            busy = false;
        }
    }
    
    protected abstract void doWork();
    
    protected void processPendingMessages() {
        AgentMessage message;
        while ((message = pendingMessages.poll()) != null) {
            try {
                handleMessage(message);
            } catch (Exception e) {
                log.error("Error processing message for agent {}: {}", getName(), e.getMessage());
            }
        }
    }
    
    protected abstract void handleMessage(AgentMessage message);
    
    @Override
    public String sendMessage(String message) {
        if (!running) {
            return "Agent is not running";
        }
        
        busy = true;
        try {
            String sessionId = definition.getSessionId();
            String response = cliEngine.sendMessage(
                getId(), 
                sessionId, 
                message,
                definition.getWorkDir(),
                definition.getApiKey(),
                definition.getApiUrl(),
                definition.getModel()
            );
            
            String newSessionId = cliEngine.getSessionId(getId());
            if (newSessionId != null && !newSessionId.equals(sessionId)) {
                definition.setSessionId(newSessionId);
                contextManager.saveSessionId(getId(), newSessionId);
            }
            
            return response;
        } finally {
            busy = false;
        }
    }
    
    @Override
    public void receiveMessage(AgentMessage message) {
        pendingMessages.offer(message);
        log.debug("Agent {} received message from {}", getName(), message.getFromAgentId());
    }
    
    @Override
    public List<AgentMessage> getPendingMessages() {
        return new ArrayList<>(pendingMessages);
    }
    
    @Override
    public void sendMessage(AgentMessage message) {
        message.setFromAgentId(getId());
        messageBus.send(message);
    }
    
    @Override
    public void assignTask(TaskAssignment task) {
        tasks.add(task);
        log.info("Task assigned to {}: {}", getName(), task.getTitle());
    }
    
    @Override
    public List<TaskAssignment> getTasks() {
        return new ArrayList<>(tasks);
    }
    
    @Override
    public void reportProgress(String taskId, String progress) {
        for (TaskAssignment task : tasks) {
            if (task.getId().equals(taskId)) {
                AgentMessage report = AgentMessage.createReport(getId(), 
                    String.format("Task [%s] progress: %s", task.getTitle(), progress));
                report.setToAgentId(task.getAssignerId());
                sendMessage(report);
                break;
            }
        }
    }
    
    @Override
    public void saveContext() {
        contextManager.saveContext(getId(), definition);
    }
    
    @Override
    public void loadContext() {
        AgentDefinition saved = contextManager.loadContext(getId());
        if (saved != null) {
            definition.setSessionId(saved.getSessionId());
            definition.setWorkDir(saved.getWorkDir());
        }
    }
    
    @Override
    public void saveMemory(String key, String value) {
        memoryManager.saveMemory(getId(), key, value);
    }
    
    @Override
    public String loadMemory(String key) {
        return memoryManager.loadMemory(getId(), key);
    }
}
