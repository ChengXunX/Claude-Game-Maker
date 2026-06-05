package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.config.AppConfig;
import com.chengxun.gamemaker.engine.ClaudeCliEngine;
import com.chengxun.gamemaker.manager.MemoryManager;
import com.chengxun.gamemaker.model.GameProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * AI助手服务
 * 提供系统分析、问答和知识库自进化功能
 *
 * 角色定位：系统和项目的分析员
 * 主要职责：
 * - 回答系统相关问题
 * - 提供优化建议
 * - 分析项目状态
 * - 知识库自进化
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class AiAssistantService {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantService.class);

    @Autowired
    private ClaudeCliEngine cliEngine;

    @Autowired
    private MemoryManager memoryManager;

    @Autowired
    private AppConfig appConfig;

    /** 系统知识库 */
    private final Map<String, String> systemKnowledge = new ConcurrentHashMap<>();

    /** 对话历史 */
    private final Map<String, List<ConversationEntry>> conversationHistory = new ConcurrentHashMap<>();

    /** Claude CLI 会话ID映射：chatSessionId -> claudeSessionId */
    private final Map<String, String> claudeSessionIdMap = new ConcurrentHashMap<>();

    /** 会话累计消息大小（字节），用于判断是否需要重置上下文 */
    private final Map<String, Long> sessionContextSize = new ConcurrentHashMap<>();

    /** 会话当前使用的模型名称 */
    private final Map<String, String> sessionModelMap = new ConcurrentHashMap<>();

    /** 会话主题摘要：chatSessionId -> 摘要文本 */
    private final Map<String, String> sessionTopicMap = new ConcurrentHashMap<>();

    /** 默认上下文大小阈值（100KB） */
    private static final long DEFAULT_CONTEXT_SIZE_THRESHOLD = 100 * 1024;

    /** 1M 上下文模型的阈值（800KB，留 20% 余量） */
    private static final long LONG_CONTEXT_SIZE_THRESHOLD = 800 * 1024;

    /** AI助手的固定会话ID */
    private static final String ASSISTANT_SESSION_ID = "system-assistant";

    /**
     * 对话条目
     */
    public static class ConversationEntry {
        private String role; // "user" or "assistant"
        private String content;
        private String thinking; // 思考过程
        private long timestamp;

        public ConversationEntry(String role, String content, String thinking) {
            this.role = role;
            this.content = content;
            this.thinking = thinking;
            this.timestamp = System.currentTimeMillis();
        }

        // Getters
        public String getRole() { return role; }
        public String getContent() { return content; }
        public String getThinking() { return thinking; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * AI响应结果
     */
    public static class AiResponse {
        private String answer;
        private String thinking;
        private boolean success;
        private String error;

        public AiResponse(String answer, String thinking) {
            this.answer = answer;
            this.thinking = thinking;
            this.success = true;
        }

        public AiResponse(String error) {
            this.error = error;
            this.success = false;
        }

        // Getters
        public String getAnswer() { return answer; }
        public String getThinking() { return thinking; }
        public boolean isSuccess() { return success; }
        public String getError() { return error; }
    }

    /**
     * 初始化系统知识库
     */
    public void initSystemKnowledge() {
        // 系统架构知识
        systemKnowledge.put("system_architecture", """
            ChengXun Game Maker 是一个AI驱动的游戏开发自动化管理系统。

            核心架构：
            - 多Agent协作：支持策划、服务端、客户端、UI、测试等角色
            - 消息队列：Agent间通过消息总线通信
            - 任务调度：智能任务分配和负载均衡
            - 上下文管理：支持上下文压缩和恢复
            - 知识库：支持技能、记忆、经验的持久化

            技术栈：
            - 后端：Spring Boot 3.2, Spring Data JPA, Spring Security
            - 数据库：MySQL (生产), H2 (测试)
            - 前端：Thymeleaf, Bootstrap 5, Chart.js
            - API文档：SpringDoc OpenAPI
            """);

        // Agent系统知识
        systemKnowledge.put("agent_system", """
            Agent系统是核心功能，支持多种角色的AI Agent：

            角色类型：
            - Producer (制作人)：项目管理和协调
            - ServerDev (服务端开发)：后端逻辑实现
            - ClientDev (客户端开发)：前端逻辑实现
            - UiDev (UI设计)：界面设计和实现
            - SystemPlanner (系统策划)：系统设计和文档
            - NumericalPlanner (数值策划)：数值平衡设计
            - GitCommit (Git专员)：版本管理
            - Tester (测试)：功能测试

            能力系统：
            - 自定义标签：灵活分类和筛选
            - 能力声明：精确的能力匹配
            - 上下文管理：智能压缩和恢复
            - API能力：支持/不支持功能检测
            """);

        // 项目管理知识
        systemKnowledge.put("project_management", """
            项目管理功能：

            Git仓库管理：
            - 支持多仓库：策划、服务端、客户端独立管理
            - 目录分离：通过相对路径管理
            - Agent分配：每个仓库分配对应Agent

            代码审查：
            - 自动审查：代码规范、安全性检查
            - 人工审查：针对特定仓库
            - 审查评分：0-100分制

            性能监控：
            - Agent响应时间
            - API调用统计
            - Token消耗统计
            - 系统资源监控
            """);

        log.info("System knowledge initialized with {} entries", systemKnowledge.size());
    }

    /**
     * 发送问题并获取回答
     *
     * @param userId 用户ID
     * @param question 问题
     * @return AI响应
     */
    public AiResponse askQuestion(String userId, String question) {
        try {
            // 构建包含系统知识的提示
            String systemPrompt = buildSystemPrompt();
            String fullPrompt = systemPrompt + "\n\n用户问题：" + question;

            // 调用AI引擎
            String response = cliEngine.sendMessage(
                ASSISTANT_SESSION_ID,
                null,
                fullPrompt,
                null,
                appConfig.getApiKey(),
                appConfig.getApiUrl(),
                appConfig.getModel()
            );

            // 解析响应，分离思考过程和答案
            String thinking = extractThinking(response);
            String answer = extractAnswer(response);

            // 保存到对话历史
            saveConversation(userId, question, answer, thinking);

            // 知识库自进化：从对话中学习
            learnFromConversation(question, answer);

            return new AiResponse(answer, thinking);

        } catch (Exception e) {
            log.error("Failed to process question", e);
            return new AiResponse("抱歉，处理问题时出现错误：" + e.getMessage());
        }
    }

    /**
     * 构建系统提示
     */
    private String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("## 角色定义\n\n");
        sb.append("你是ChengXun Game Maker系统的AI助手，角色是**系统和项目的分析员**。\n\n");
        sb.append("### 主要职责\n");
        sb.append("1. 回答系统相关问题\n");
        sb.append("2. 提供系统优化建议\n");
        sb.append("3. 分析项目状态和性能\n");
        sb.append("4. 协助故障排查\n");
        sb.append("5. 解释系统功能和配置\n\n");

        sb.append("### 回答要求\n");
        sb.append("- 使用中文回答\n");
        sb.append("- 保持专业但友好的语气\n");
        sb.append("- 提供具体可行的建议\n");
        sb.append("- 必要时提供代码示例\n");
        sb.append("- 对于不确定的问题，坦诚说明\n\n");

        sb.append("### 思考过程\n");
        sb.append("请在回答前，先在<thinking>标签中展示你的思考过程，");
        sb.append("然后在<answer>标签中给出最终答案。\n\n");

        // 添加系统知识
        sb.append("## 系统知识库\n\n");
        for (Map.Entry<String, String> entry : systemKnowledge.entrySet()) {
            sb.append("### ").append(entry.getKey()).append("\n");
            sb.append(entry.getValue()).append("\n\n");
        }

        return sb.toString();
    }

    /**
     * 提取思考过程
     */
    private String extractThinking(String response) {
        int start = response.indexOf("<thinking>");
        int end = response.indexOf("</thinking>");
        if (start != -1 && end != -1) {
            return response.substring(start + 10, end).trim();
        }
        return null;
    }

    /**
     * 提取答案
     */
    private String extractAnswer(String response) {
        int start = response.indexOf("<answer>");
        int end = response.indexOf("</answer>");
        if (start != -1 && end != -1) {
            return response.substring(start + 8, end).trim();
        }
        // 如果没有标签，返回整个响应
        return response.replace("<thinking>", "")
                      .replace("</thinking>", "")
                      .trim();
    }

    /**
     * 保存对话历史
     */
    public void saveConversation(String userId, String question, String answer, String thinking) {
        List<ConversationEntry> history = conversationHistory.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());

        synchronized (history) {
            history.add(new ConversationEntry("user", question, null));
            history.add(new ConversationEntry("assistant", answer, thinking));

            // 只保留最近50条对话
            while (history.size() > 100) {
                history.remove(0);
            }
        }
    }

    /** 学习知识的最大保留数量 */
    private static final int MAX_LEARNED_KNOWLEDGE = 100;

    /**
     * 知识库自进化：从对话中学习
     */
    private void learnFromConversation(String question, String answer) {
        // 识别有价值的信息
        if (isValuableKnowledge(question, answer)) {
            String key = "learned_" + System.currentTimeMillis();
            String value = "Q: " + question + "\nA: " + answer;
            systemKnowledge.put(key, value);
            log.info("Learned new knowledge from conversation");

            // 清理过多的学习知识，保留最新的
            cleanupLearnedKnowledge();
        }
    }

    /**
     * 清理过多的学习知识，防止内存泄漏
     */
    private void cleanupLearnedKnowledge() {
        long learnedCount = systemKnowledge.keySet().stream()
            .filter(k -> k.startsWith("learned_"))
            .count();

        if (learnedCount > MAX_LEARNED_KNOWLEDGE) {
            // 删除最早的学习知识
            systemKnowledge.keySet().stream()
                .filter(k -> k.startsWith("learned_"))
                .sorted()
                .limit(learnedCount - MAX_LEARNED_KNOWLEDGE)
                .toList()
                .forEach(systemKnowledge::remove);
        }
    }

    /**
     * 判断是否是有价值的知识
     */
    private boolean isValuableKnowledge(String question, String answer) {
        // 包含解决方案、最佳实践、配置建议等
        String lowerAnswer = answer.toLowerCase();
        return lowerAnswer.contains("建议") ||
               lowerAnswer.contains("解决方案") ||
               lowerAnswer.contains("最佳实践") ||
               lowerAnswer.contains("配置") ||
               lowerAnswer.contains("优化");
    }

    /**
     * 获取对话历史
     */
    public List<ConversationEntry> getConversationHistory(String userId) {
        return conversationHistory.getOrDefault(userId, new ArrayList<>());
    }

    /**
     * 清空对话历史
     */
    public void clearConversationHistory(String userId) {
        conversationHistory.remove(userId);
    }

    /**
     * 获取 Claude CLI 会话ID
     * 用于 --resume 恢复上下文
     * 当上下文累计大小超过模型对应的阈值时返回 null（强制重新开始，避免响应变慢）
     *
     * @param chatSessionId 聊天会话ID
     * @return 会话恢复信息，包含 sessionId 和是否需要注入摘要
     */
    public SessionResumeInfo getClaudeSessionId(String chatSessionId) {
        Long contextSize = sessionContextSize.get(chatSessionId);
        String model = sessionModelMap.get(chatSessionId);
        long threshold = getContextThreshold(model);

        if (contextSize != null && contextSize > threshold) {
            log.info("会话上下文过大 ({}KB > {}KB)，重置 sessionId - chatSessionId: {}, model: {}",
                contextSize / 1024, threshold / 1024, chatSessionId, model);

            // 生成会话摘要
            String summary = generateSessionSummary(chatSessionId);

            // 重置 session
            claudeSessionIdMap.remove(chatSessionId);
            sessionContextSize.put(chatSessionId, 0L);

            return new SessionResumeInfo(null, true, summary);
        }

        String claudeSessionId = claudeSessionIdMap.get(chatSessionId);
        return new SessionResumeInfo(claudeSessionId, false, null);
    }

    /**
     * 根据模型名称获取上下文大小阈值
     *
     * @param model 模型名称
     * @return 阈值（字节）
     */
    private long getContextThreshold(String model) {
        if (model == null) {
            return DEFAULT_CONTEXT_SIZE_THRESHOLD;
        }
        // 带 [1m] 标记的模型有 1M 上下文窗口
        if (model.contains("[1m]")) {
            return LONG_CONTEXT_SIZE_THRESHOLD;
        }
        // 其他模型使用默认阈值
        return DEFAULT_CONTEXT_SIZE_THRESHOLD;
    }

    /**
     * 保存 Claude CLI 会话ID
     * 用于后续对话恢复上下文
     *
     * @param chatSessionId 聊天会话ID
     * @param claudeSessionId Claude CLI 会话ID
     */
    public void saveClaudeSessionId(String chatSessionId, String claudeSessionId) {
        claudeSessionIdMap.put(chatSessionId, claudeSessionId);
        log.debug("保存 Claude sessionId: {} -> {}", chatSessionId, claudeSessionId);
    }

    /**
     * 设置会话使用的模型
     *
     * @param chatSessionId 聊天会话ID
     * @param model 模型名称
     */
    public void setSessionModel(String chatSessionId, String model) {
        sessionModelMap.put(chatSessionId, model);
    }

    /**
     * 累加会话上下文大小
     * 用于判断何时需要重置上下文
     *
     * @param chatSessionId 聊天会话ID
     * @param messageSize 本次消息大小（字节）
     */
    public void addContextSize(String chatSessionId, long messageSize) {
        sessionContextSize.merge(chatSessionId, messageSize, Long::sum);
    }

    /**
     * 更新会话主题摘要
     * 从对话内容中提取关键信息
     *
     * @param chatSessionId 聊天会话ID
     * @param userMessage 用户消息
     * @param assistantReply 助手回复
     */
    public void updateSessionTopic(String chatSessionId, String userMessage, String assistantReply) {
        // 提取用户意图和关键信息
        StringBuilder topic = new StringBuilder();

        // 保留之前的主题信息（如果有）
        String existingTopic = sessionTopicMap.get(chatSessionId);
        if (existingTopic != null && !existingTopic.isEmpty()) {
            topic.append(existingTopic).append("\n---\n");
        }

        // 提取用户消息的关键信息（截取前200字）
        if (userMessage != null && !userMessage.isEmpty()) {
            String userSummary = userMessage.length() > 200 ? userMessage.substring(0, 200) + "..." : userMessage;
            topic.append("用户: ").append(userSummary).append("\n");
        }

        // 提取助手回复的关键信息（截取前200字）
        if (assistantReply != null && !assistantReply.isEmpty()) {
            String replySummary = assistantReply.length() > 200 ? assistantReply.substring(0, 200) + "..." : assistantReply;
            topic.append("助手: ").append(replySummary).append("\n");
        }

        // 只保留最近 10 轮对话的摘要
        String topicStr = topic.toString();
        String[] lines = topicStr.split("\n---\n");
        if (lines.length > 10) {
            StringBuilder trimmed = new StringBuilder();
            for (int i = lines.length - 10; i < lines.length; i++) {
                if (i > lines.length - 10) trimmed.append("\n---\n");
                trimmed.append(lines[i]);
            }
            topicStr = trimmed.toString();
        }

        sessionTopicMap.put(chatSessionId, topicStr);
    }

    /**
     * 生成会话摘要
     * 用于 session 重置后注入新会话，让 AI 知道之前在做什么
     *
     * @param chatSessionId 聊天会话ID
     * @return 会话摘要文本
     */
    private String generateSessionSummary(String chatSessionId) {
        String topic = sessionTopicMap.get(chatSessionId);
        if (topic == null || topic.isEmpty()) {
            return null;
        }

        StringBuilder summary = new StringBuilder();
        summary.append("## 会话上下文恢复\n\n");
        summary.append("这是一个之前的会话，由于上下文过长已自动重置。以下是之前的对话摘要：\n\n");
        summary.append(topic);
        summary.append("\n\n请基于以上上下文继续与用户对话，保持工作连续性。");

        return summary.toString();
    }

    /**
     * 会话恢复信息
     */
    public static class SessionResumeInfo {
        private final String sessionId;
        private final boolean needsReset;
        private final String summary;

        public SessionResumeInfo(String sessionId, boolean needsReset, String summary) {
            this.sessionId = sessionId;
            this.needsReset = needsReset;
            this.summary = summary;
        }

        public String getSessionId() { return sessionId; }
        public boolean isNeedsReset() { return needsReset; }
        public String getSummary() { return summary; }
    }

    /**
     * 清除 Claude CLI 会话ID 和上下文大小
     * 会话删除时调用
     *
     * @param chatSessionId 聊天会话ID
     */
    public void clearClaudeSessionId(String chatSessionId) {
        claudeSessionIdMap.remove(chatSessionId);
        sessionContextSize.remove(chatSessionId);
        sessionModelMap.remove(chatSessionId);
        sessionTopicMap.remove(chatSessionId);
    }

    /**
     * 获取系统知识库
     */
    public Map<String, String> getSystemKnowledge() {
        return new HashMap<>(systemKnowledge);
    }

    /**
     * 添加自定义知识
     */
    public void addKnowledge(String key, String content) {
        systemKnowledge.put(key, content);
        log.info("Added custom knowledge: {}", key);
    }

    /**
     * 删除自定义知识
     */
    public void removeKnowledge(String key) {
        systemKnowledge.remove(key);
        log.info("Removed knowledge: {}", key);
    }

    /**
     * 获取知识库统计
     */
    public Map<String, Object> getKnowledgeStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEntries", systemKnowledge.size());
        stats.put("learnedEntries", systemKnowledge.keySet().stream()
            .filter(k -> k.startsWith("learned_"))
            .count());
        stats.put("systemEntries", systemKnowledge.keySet().stream()
            .filter(k -> !k.startsWith("learned_"))
            .count());
        return stats;
    }

    /**
     * 分析系统状态
     */
    public String analyzeSystemStatus() {
        StringBuilder analysis = new StringBuilder();
        analysis.append("## 系统状态分析\n\n");

        // 知识库统计
        Map<String, Object> stats = getKnowledgeStats();
        analysis.append("### 知识库\n");
        analysis.append("- 总条目：").append(stats.get("totalEntries")).append("\n");
        analysis.append("- 系统知识：").append(stats.get("systemEntries")).append("\n");
        analysis.append("- 学习知识：").append(stats.get("learnedEntries")).append("\n\n");

        // 对话统计
        int totalConversations = conversationHistory.values().stream()
            .mapToInt(List::size)
            .sum();
        analysis.append("### 对话统计\n");
        analysis.append("- 活跃用户：").append(conversationHistory.size()).append("\n");
        analysis.append("- 总对话数：").append(totalConversations / 2).append("\n\n");

        analysis.append("### 建议\n");
        analysis.append("- 定期清理过期对话历史\n");
        analysis.append("- 关注学习知识的质量\n");
        analysis.append("- 及时更新系统知识库\n");

        return analysis.toString();
    }

    /**
     * 根据项目团队配置生成优化建议
     *
     * @param projectId 项目ID
     * @param teamConfig 团队配置
     * @return 优化建议
     */
    public String generateTeamOptimizationAdvice(String projectId, Map<String, Object> teamConfig) {
        StringBuilder advice = new StringBuilder();
        advice.append("## 团队配置优化建议\n\n");

        // 分析团队规模
        int teamSize = teamConfig.containsKey("teamSize") ? (int) teamConfig.get("teamSize") : 0;
        String projectType = (String) teamConfig.getOrDefault("projectType", "unknown");

        advice.append("### 项目信息\n");
        advice.append("- 项目ID：").append(projectId).append("\n");
        advice.append("- 项目类型：").append(projectType).append("\n");
        advice.append("- 团队规模：").append(teamSize).append(" 人\n\n");

        // 根据项目类型给出建议
        advice.append("### 团队配置建议\n");
        switch (projectType.toLowerCase()) {
            case "h5-game":
            case "casual-game":
                advice.append("休闲H5游戏推荐配置：\n");
                advice.append("- 制作人 x 1\n");
                advice.append("- 前端开发 x 1-2\n");
                advice.append("- 策划 x 1\n");
                advice.append("- 美术（可选）x 1\n\n");
                if (teamSize > 4) {
                    advice.append("⚠️ 团队规模偏大，建议精简\n");
                }
                break;
            case "rpg-game":
            case "slg-game":
                advice.append("RPG/SLG游戏推荐配置：\n");
                advice.append("- 制作人 x 1\n");
                advice.append("- 前端开发 x 2-3\n");
                advice.append("- 后端开发 x 1-2\n");
                advice.append("- 策划 x 2\n");
                advice.append("- 美术 x 2\n");
                advice.append("- 测试 x 1\n\n");
                if (teamSize < 6) {
                    advice.append("⚠️ 团队规模偏小，可能影响开发效率\n");
                }
                break;
            default:
                advice.append("通用游戏推荐配置：\n");
                advice.append("- 制作人 x 1\n");
                advice.append("- 开发 x 2-3\n");
                advice.append("- 策划 x 1\n");
                advice.append("- 测试 x 1\n\n");
        }

        // 根据历史数据给出建议
        advice.append("### 历史经验\n");
        advice.append("基于知识库中的项目经验：\n");

        // 从知识库中查找类似项目的经验
        String similarExperience = findSimilarProjectExperience(projectType);
        if (similarExperience != null) {
            advice.append(similarExperience).append("\n");
        } else {
            advice.append("- 暂无类似项目经验\n");
        }

        // 优化建议
        advice.append("\n### 优化建议\n");
        advice.append("1. 根据项目阶段动态调整团队规模\n");
        advice.append("2. 原型阶段精简团队，快速验证\n");
        advice.append("3. 开发阶段按需扩充，确保进度\n");
        advice.append("4. 测试阶段增加测试人员\n");
        advice.append("5. 运维阶段保持核心团队\n");

        return advice.toString();
    }

    /**
     * 从知识库中查找类似项目的经验
     */
    private String findSimilarProjectExperience(String projectType) {
        // 从学习的知识中查找
        for (Map.Entry<String, String> entry : systemKnowledge.entrySet()) {
            if (entry.getKey().startsWith("learned_") &&
                entry.getValue().contains(projectType)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 根据项目阶段生成全局优化建议
     *
     * @param projectId 项目ID
     * @param currentPhase 当前阶段
     * @return 优化建议
     */
    public String generateGlobalOptimizationAdvice(String projectId, String currentPhase) {
        StringBuilder advice = new StringBuilder();
        advice.append("## 全局优化建议\n\n");

        advice.append("### 项目阶段：").append(currentPhase).append("\n\n");

        // 根据阶段给出建议
        switch (currentPhase.toLowerCase()) {
            case "concept":
                advice.append("#### 概念阶段优化\n");
                advice.append("1. 快速验证核心玩法\n");
                advice.append("2. 制作可玩原型\n");
                advice.append("3. 收集目标用户反馈\n");
                advice.append("4. 确定技术可行性\n\n");
                break;
            case "prototype":
                advice.append("#### 原型阶段优化\n");
                advice.append("1. 专注核心功能\n");
                advice.append("2. 快速迭代\n");
                advice.append("3. 测试市场反应\n");
                advice.append("4. 验证商业模式\n\n");
                break;
            case "development":
                advice.append("#### 开发阶段优化\n");
                advice.append("1. 制定详细计划\n");
                advice.append("2. 分配充足资源\n");
                advice.append("3. 定期代码审查\n");
                advice.append("4. 持续集成测试\n\n");
                break;
            case "testing":
                advice.append("#### 测试阶段优化\n");
                advice.append("1. 全面功能测试\n");
                advice.append("2. 性能压力测试\n");
                advice.append("3. 兼容性测试\n");
                advice.append("4. 用户体验测试\n\n");
                break;
            case "release":
                advice.append("#### 发布阶段优化\n");
                advice.append("1. 制定发布计划\n");
                advice.append("2. 准备回滚方案\n");
                advice.append("3. 监控系统状态\n");
                advice.append("4. 收集用户反馈\n\n");
                break;
            default:
                advice.append("#### 通用优化建议\n");
                advice.append("1. 保持团队沟通顺畅\n");
                advice.append("2. 定期回顾和改进\n");
                advice.append("3. 关注技术债务\n");
                advice.append("4. 持续学习和改进\n\n");
        }

        // 从知识库中获取相关最佳实践
        advice.append("### 相关最佳实践\n");
        String bestPractice = getBestPracticeForPhase(currentPhase);
        if (bestPractice != null) {
            advice.append(bestPractice).append("\n");
        } else {
            advice.append("- 暂无特定阶段的最佳实践\n");
        }

        return advice.toString();
    }

    /**
     * 获取特定阶段的最佳实践
     */
    private String getBestPracticeForPhase(String phase) {
        String key = "best_practice_" + phase.toLowerCase();
        return systemKnowledge.get(key);
    }

    /**
     * 更新项目特定的知识
     *
     * @param projectId 项目ID
     * @param key 知识键
     * @param content 知识内容
     */
    public void updateProjectKnowledge(String projectId, String key, String content) {
        String fullKey = "project_" + projectId + "_" + key;
        systemKnowledge.put(fullKey, content);
        log.info("Updated project knowledge: {}", fullKey);
    }

    /**
     * 获取项目特定的知识
     *
     * @param projectId 项目ID
     * @param key 知识键
     * @return 知识内容
     */
    public String getProjectKnowledge(String projectId, String key) {
        String fullKey = "project_" + projectId + "_" + key;
        return systemKnowledge.get(fullKey);
    }

    /**
     * 分析项目团队效能
     *
     * @param projectId 项目ID
     * @param teamMetrics 团队指标
     * @return 分析结果
     */
    public String analyzeTeamPerformance(String projectId, Map<String, Object> teamMetrics) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("## 团队效能分析\n\n");

        // 提取指标
        int completedTasks = teamMetrics.containsKey("completedTasks") ? (int) teamMetrics.get("completedTasks") : 0;
        int totalTasks = teamMetrics.containsKey("totalTasks") ? (int) teamMetrics.get("totalTasks") : 0;
        double avgResponseTime = teamMetrics.containsKey("avgResponseTime") ? (double) teamMetrics.get("avgResponseTime") : 0;
        int activeAgents = teamMetrics.containsKey("activeAgents") ? (int) teamMetrics.get("activeAgents") : 0;

        analysis.append("### 关键指标\n");
        analysis.append("- 完成任务：").append(completedTasks).append("/").append(totalTasks).append("\n");
        analysis.append("- 完成率：").append(totalTasks > 0 ? (completedTasks * 100 / totalTasks) : 0).append("%\n");
        analysis.append("- 平均响应时间：").append(String.format("%.1f", avgResponseTime)).append(" 秒\n");
        analysis.append("- 活跃 Agent：").append(activeAgents).append("\n\n");

        // 效能评估
        analysis.append("### 效能评估\n");
        double completionRate = totalTasks > 0 ? (double) completedTasks / totalTasks : 0;
        if (completionRate >= 0.8) {
            analysis.append("✅ 任务完成率良好\n");
        } else if (completionRate >= 0.5) {
            analysis.append("⚠️ 任务完成率一般，建议检查瓶颈\n");
        } else {
            analysis.append("❌ 任务完成率较低，需要重点关注\n");
        }

        if (avgResponseTime <= 30) {
            analysis.append("✅ 响应速度良好\n");
        } else if (avgResponseTime <= 60) {
            analysis.append("⚠️ 响应速度一般，可能影响效率\n");
        } else {
            analysis.append("❌ 响应速度较慢，建议优化\n");
        }

        // 优化建议
        analysis.append("\n### 优化建议\n");
        if (completionRate < 0.8) {
            analysis.append("1. 检查任务分配是否合理\n");
            analysis.append("2. 确认 Agent 能力是否匹配\n");
            analysis.append("3. 优化工作流程\n");
        }
        if (avgResponseTime > 30) {
            analysis.append("4. 检查 API 配置和网络状况\n");
            analysis.append("5. 考虑增加 Agent 数量\n");
        }
        if (activeAgents < 3) {
            analysis.append("6. 考虑扩充团队规模\n");
        }

        return analysis.toString();
    }
}
