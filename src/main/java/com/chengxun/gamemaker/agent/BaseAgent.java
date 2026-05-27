package com.chengxun.gamemaker.agent;

import com.chengxun.gamemaker.engine.ClaudeCliEngine;
import com.chengxun.gamemaker.engine.MessageBus;
import com.chengxun.gamemaker.manager.ContextManager;
import com.chengxun.gamemaker.manager.MemoryManager;
import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.manager.SkillManager;
import com.chengxun.gamemaker.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class BaseAgent implements Agent {

    protected static final Logger log = LoggerFactory.getLogger(BaseAgent.class);

    protected final AgentDefinition definition;
    protected final ClaudeCliEngine cliEngine;
    protected final MessageBus messageBus;
    protected final ContextManager contextManager;
    protected final MemoryManager memoryManager;
    protected final SkillManager skillManager;
    protected final ProjectManager projectManager;

    protected AgentContext agentContext;
    protected GameProject currentProject;
    protected String currentConversationId;

    protected final ConcurrentLinkedQueue<AgentMessage> pendingMessages = new ConcurrentLinkedQueue<>();
    protected final List<TaskAssignment> tasks = new ArrayList<>();

    protected volatile boolean busy = false;
    protected volatile boolean running = false;

    protected BaseAgent(AgentDefinition definition,
                       ClaudeCliEngine cliEngine,
                       MessageBus messageBus,
                       ContextManager contextManager,
                       MemoryManager memoryManager,
                       SkillManager skillManager,
                       ProjectManager projectManager) {
        this.definition = definition;
        this.cliEngine = cliEngine;
        this.messageBus = messageBus;
        this.contextManager = contextManager;
        this.memoryManager = memoryManager;
        this.skillManager = skillManager;
        this.projectManager = projectManager;
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

    public GameProject getCurrentProject() {
        return currentProject;
    }

    public AgentContext getAgentContext() {
        return agentContext;
    }

    public String getCurrentConversationId() {
        return currentConversationId;
    }

    @Override
    public void initialize() {
        log.info("Initializing agent: {} ({})", getName(), getId());

        // 初始化项目
        initProject();

        // 加载或创建上下文
        agentContext = contextManager.getOrCreateContext(getId(), currentProject);
        if (agentContext.getSessionId() != null) {
            definition.setSessionId(agentContext.getSessionId());
        }
        if (agentContext.getWorkDir() != null) {
            definition.setWorkDir(agentContext.getWorkDir());
        }

        // 初始化记忆分类
        initMemoryCategories();

        // 加载默认记忆
        loadDefaultMemory();

        // 加载项目级别 SKILL
        loadProjectSkills();

        // 注册到消息总线
        messageBus.registerAgent(this);

        // 开始新的对话
        startNewConversation();

        log.info("Agent initialized: {} for project: {}", getName(),
            currentProject != null ? currentProject.getName() : "global");
    }

    protected void initProject() {
        String workDir = definition.getWorkDir();
        if (workDir != null && !workDir.isEmpty()) {
            currentProject = projectManager.getOrCreateProject(workDir);
            currentProject.addAgent(getId());
            projectManager.saveProjectConfig(currentProject);
        }
    }

    protected void initMemoryCategories() {
        String agentId = getId();
        if (currentProject != null) {
            // 项目级别记忆目录
            String projectMemoryDir = currentProject.getMemoryDir() + "/" + agentId;
            memoryManager.createCategory(projectMemoryDir, "skills");
            memoryManager.createCategory(projectMemoryDir, "knowledge");
            memoryManager.createCategory(projectMemoryDir, "experiences");
            memoryManager.createCategory(projectMemoryDir, "preferences");
            memoryManager.createCategory(projectMemoryDir, "general");
        }
        // 全局记忆目录
        memoryManager.createCategory(agentId, "skills");
        memoryManager.createCategory(agentId, "knowledge");
        memoryManager.createCategory(agentId, "experiences");
        memoryManager.createCategory(agentId, "preferences");
        memoryManager.createCategory(agentId, "general");
    }

    protected void loadDefaultMemory() {
        // 加载 AGENTS.md 文件内容
        String agentsFile = definition.getAgentsFile();
        if (agentsFile != null && !agentsFile.isEmpty()) {
            try {
                java.nio.file.Path path = java.nio.file.Path.of(agentsFile);
                if (java.nio.file.Files.exists(path)) {
                    String content = java.nio.file.Files.readString(path);
                    saveKnowledge("agents_file", content);
                }
            } catch (Exception e) {
                log.warn("Failed to load agents file for {}: {}", getId(), e.getMessage());
            }
        }

        // 加载项目规则
        if (currentProject != null) {
            String rules = projectManager.loadProjectRules(currentProject.getId());
            if (rules != null) {
                saveKnowledge("project_rules", rules);
            }
        }
    }

    protected void loadProjectSkills() {
        if (currentProject != null) {
            skillManager.loadProjectSkills(currentProject.getId(), currentProject.getSkillsDir());
        }
    }

    protected void startNewConversation() {
        currentConversationId = UUID.randomUUID().toString();
        log.debug("New conversation started: {}", currentConversationId);
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
        saveContext();
        createSnapshot();
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
            saveConversationMessage("user", message);

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
                agentContext.setSessionId(newSessionId);
                contextManager.updateSessionId(getId(), currentProject, newSessionId);
            }

            saveConversationMessage("assistant", response);

            agentContext.addWorkingMemory("last_message", message.length() > 100 ? message.substring(0, 100) : message);
            agentContext.addWorkingMemory("last_response", response.length() > 100 ? response.substring(0, 100) : response);

            return response;
        } finally {
            busy = false;
        }
    }

    protected void saveConversationMessage(String role, String content) {
        ContextManager.ConversationMessage msg = new ContextManager.ConversationMessage(role, content);
        msg.setTaskId(agentContext.getCurrentTaskId());

        List<ContextManager.ConversationMessage> messages = contextManager.loadConversation(getId(), currentProject, currentConversationId);
        messages.add(msg);
        contextManager.saveConversation(getId(), currentProject, currentConversationId, messages);
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
        agentContext.setCurrentTaskId(task.getId());
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

    // ===== 上下文管理增强 =====

    @Override
    public void saveContext() {
        agentContext.setWorkDir(definition.getWorkDir());
        agentContext.setSessionId(definition.getSessionId());
        contextManager.saveContext(agentContext, currentProject);
    }

    @Override
    public void loadContext() {
        agentContext = contextManager.loadContext(getId(), currentProject);
        if (agentContext != null) {
            definition.setSessionId(agentContext.getSessionId());
            definition.setWorkDir(agentContext.getWorkDir());
        }
    }

    protected void createSnapshot() {
        List<ContextManager.ConversationMessage> recentMessages = contextManager.getRecentMessages(getId(), currentProject, 20);
        contextManager.createSnapshot(getId(), currentProject, agentContext, recentMessages);
        contextManager.cleanupOldSnapshots(getId(), currentProject, 5);
    }

    protected void recoverContext() {
        log.info("Recovering context for agent: {} in project: {}", getId(),
            currentProject != null ? currentProject.getName() : "global");

        Map<String, Object> snapshot = contextManager.loadLatestSnapshot(getId(), currentProject);
        if (snapshot != null) {
            log.info("Found snapshot, recovering...");

            if (snapshot.containsKey("context")) {
                agentContext.addWorkingMemory("recovery_time", java.time.LocalDateTime.now().toString());
                agentContext.addWorkingMemory("recovery_source", "snapshot");
            }

            if (snapshot.containsKey("recentMessages")) {
                String summary = buildConversationSummary(snapshot);
                agentContext.addWorkingMemory("conversation_summary", summary);
            }
        }

        Map<String, String> knowledge = loadAllKnowledge();
        Map<String, String> experiences = loadAllExperiences();

        String recoveryPrompt = buildRecoveryPrompt(knowledge, experiences);
        if (!recoveryPrompt.isEmpty()) {
            log.info("Sending recovery prompt to rebuild context");
            sendMessage(recoveryPrompt);
        }
    }

    @SuppressWarnings("unchecked")
    protected String buildConversationSummary(Map<String, Object> snapshot) {
        Object messagesObj = snapshot.get("recentMessages");
        if (messagesObj instanceof List<?> messages) {
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (Object msgObj : messages) {
                if (msgObj instanceof Map<?, ?> msg && count < 5) {
                    String role = (String) msg.get("role");
                    String content = (String) msg.get("content");
                    if (content != null && content.length() > 100) {
                        content = content.substring(0, 100) + "...";
                    }
                    sb.append(String.format("- [%s]: %s\n", role, content));
                    count++;
                }
            }
            return sb.toString();
        }
        return "";
    }

    protected String buildRecoveryPrompt(Map<String, String> knowledge, Map<String, String> experiences) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 上下文恢复\n\n");
        sb.append("由于会话中断，需要恢复工作上下文。\n\n");

        if (!knowledge.isEmpty()) {
            sb.append("### 已有知识\n");
            knowledge.forEach((key, value) -> {
                String summary = value.length() > 200 ? value.substring(0, 200) + "..." : value;
                sb.append(String.format("- **%s**: %s\n", key, summary));
            });
            sb.append("\n");
        }

        if (!experiences.isEmpty()) {
            sb.append("### 已有经验\n");
            experiences.forEach((key, value) -> {
                String summary = value.length() > 200 ? value.substring(0, 200) + "..." : value;
                sb.append(String.format("- **%s**: %s\n", key, summary));
            });
            sb.append("\n");
        }

        sb.append("请基于以上信息恢复工作上下文，并确认你已准备好继续工作。");
        return sb.toString();
    }

    // ===== 记忆管理增强 =====

    @Override
    public void saveMemory(String key, String value) {
        memoryManager.saveMemory(getId(), "general", key, value);
        if (currentProject != null) {
            memoryManager.saveMemory(currentProject.getMemoryDir() + "/" + getId(), "general", key, value);
        }
    }

    @Override
    public String loadMemory(String key) {
        // 先尝试加载项目级别
        if (currentProject != null) {
            String value = memoryManager.loadMemory(currentProject.getMemoryDir() + "/" + getId(), "general", key);
            if (value != null) return value;
        }
        return memoryManager.loadMemory(getId(), "general", key);
    }

    public void saveKnowledge(String key, String value) {
        memoryManager.saveMemory(getId(), "knowledge", key, value);
        if (currentProject != null) {
            memoryManager.saveMemory(currentProject.getMemoryDir() + "/" + getId(), "knowledge", key, value);
        }
    }

    public String loadKnowledge(String key) {
        if (currentProject != null) {
            String value = memoryManager.loadMemory(currentProject.getMemoryDir() + "/" + getId(), "knowledge", key);
            if (value != null) return value;
        }
        return memoryManager.loadMemory(getId(), "knowledge", key);
    }

    public void saveExperience(String key, String value) {
        memoryManager.saveMemory(getId(), "experiences", key, value);
        if (currentProject != null) {
            memoryManager.saveMemory(currentProject.getMemoryDir() + "/" + getId(), "experiences", key, value);
        }
        agentContext.addLearnedPattern(key);
    }

    public String loadExperience(String key) {
        if (currentProject != null) {
            String value = memoryManager.loadMemory(currentProject.getMemoryDir() + "/" + getId(), "experiences", key);
            if (value != null) return value;
        }
        return memoryManager.loadMemory(getId(), "experiences", key);
    }

    public void saveSkill(String key, String value) {
        memoryManager.saveMemory(getId(), "skills", key, value);
        if (currentProject != null) {
            memoryManager.saveMemory(currentProject.getMemoryDir() + "/" + getId(), "skills", key, value);
        }
    }

    public String loadSkill(String key) {
        if (currentProject != null) {
            String value = memoryManager.loadMemory(currentProject.getMemoryDir() + "/" + getId(), "skills", key);
            if (value != null) return value;
        }
        return memoryManager.loadMemory(getId(), "skills", key);
    }

    protected Map<String, String> loadAllKnowledge() {
        Map<String, String> result = new HashMap<>();
        result.putAll(memoryManager.getCategoryMemory(getId(), "knowledge"));
        if (currentProject != null) {
            result.putAll(memoryManager.getCategoryMemory(currentProject.getMemoryDir() + "/" + getId(), "knowledge"));
        }
        return result;
    }

    protected Map<String, String> loadAllExperiences() {
        Map<String, String> result = new HashMap<>();
        result.putAll(memoryManager.getCategoryMemory(getId(), "experiences"));
        if (currentProject != null) {
            result.putAll(memoryManager.getCategoryMemory(currentProject.getMemoryDir() + "/" + getId(), "experiences"));
        }
        return result;
    }

    // ===== SKILL 相关 =====

    protected String buildSkillPrompt(String taskDescription) {
        String projectId = currentProject != null ? currentProject.getId() : null;
        List<Skill> matchedSkills = skillManager.matchSkills(taskDescription, projectId);
        if (matchedSkills.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("\n## 可用技能\n\n");
        sb.append("以下是与当前任务相关的技能，请参考使用：\n\n");

        for (Skill skill : matchedSkills) {
            sb.append(skill.toPromptSection()).append("\n");
            skillManager.recordSkillUsage(skill.getId(), projectId);
        }

        return sb.toString();
    }

    protected void tryLearnSkill(String taskDescription, String result) {
        if (currentProject == null) return;

        String summaryPrompt = String.format(
            "请分析以下任务执行过程，如果发现可复用的模式，请提取为技能。\n\n" +
            "任务描述：%s\n\n" +
            "执行结果：%s\n\n" +
            "如果这是一个可复用的技能，请用以下格式输出：\n" +
            "SKILL_NAME: 技能名称\n" +
            "SKILL_DESC: 技能描述\n" +
            "SKILL_TRIGGER: 触发关键词（逗号分隔）\n" +
            "SKILL_PROMPT: 技能的prompt模板\n\n" +
            "如果没有发现可复用模式，请输出：NO_SKILL",
            taskDescription, result.length() > 500 ? result.substring(0, 500) + "..." : result
        );

        String response = sendMessage(summaryPrompt);
        if (response != null && !response.contains("NO_SKILL")) {
            parseAndSaveLearnedSkill(response);
        }
    }

    protected void parseAndSaveLearnedSkill(String response) {
        try {
            String name = extractField(response, "SKILL_NAME");
            String desc = extractField(response, "SKILL_DESC");
            String trigger = extractField(response, "SKILL_TRIGGER");
            String prompt = extractField(response, "SKILL_PROMPT");

            if (name != null && prompt != null && currentProject != null) {
                Skill skill = Skill.builder()
                    .id("learned-" + System.currentTimeMillis())
                    .name(name)
                    .description(desc != null ? desc : name)
                    .category("learned")
                    .triggerPattern(trigger != null ? trigger : name)
                    .prompt(prompt)
                    .build();

                skillManager.saveLearnedSkill(skill, getId(), currentProject.getId(), currentProject.getSkillsDir());
                log.info("Learned new skill: {} for project: {}", skill.getId(), currentProject.getId());
            }
        } catch (Exception e) {
            log.debug("Failed to parse learned skill: {}", e.getMessage());
        }
    }

    private String extractField(String text, String fieldName) {
        String pattern = fieldName + ":";
        int start = text.indexOf(pattern);
        if (start == -1) return null;
        start += pattern.length();
        int end = text.indexOf("\n", start);
        if (end == -1) end = text.length();
        return text.substring(start, end).trim();
    }
}
