package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.config.SystemConstants;
import com.chengxun.gamemaker.engine.ClaudeCliEngine;
import com.chengxun.gamemaker.manager.ContextManager;
import com.chengxun.gamemaker.manager.MemoryManager;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.web.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dream 知识提取服务
 * 从近期会话中自动提取持久知识，存入记忆系统
 *
 * 主要功能：
 * - 扫描近期会话轨迹
 * - 提取关键决策、技术方案、经验教训
 * - 去重：与已有知识比对
 * - 保存到 Agent 的知识库
 *
 * 灵感来源：知识自动提取与学习机制
 *
 * @author chengxun
 * @since 3.0.0
 */
@Service
public class DreamService {

    private static final Logger log = LoggerFactory.getLogger(DreamService.class);

    @Autowired
    private ContextManager contextManager;

    @Autowired
    private MemoryManager memoryManager;

    @Autowired
    private ClaudeCliEngine cliEngine;

    @Autowired
    private SystemConfigService configService;

    @Autowired(required = false)
    private KnowledgeEvolutionService knowledgeEvolutionService;

    @Autowired
    private GameKnowledgeBase knowledgeBase;

    /**
     * 执行 Dream 知识提取
     *
     * @param project 项目
     * @param agentId Agent ID
     * @return 提取结果摘要
     */
    public DreamResult dream(GameProject project, String agentId) {
        if (project == null) {
            return DreamResult.failure("项目为空");
        }

        log.info("开始 Dream 知识提取: agent={}, project={}", agentId, project.getId());

        // 1. 加载近期对话
        List<ContextManager.ConversationMessage> recentMessages =
            contextManager.getRecentMessages(agentId, project, 100);

        // 如果没有近期对话，尝试从 Agent 记忆中提取
        if (recentMessages.isEmpty()) {
            log.info("没有近期对话，尝试从 Agent 记忆中提取知识: agent={}", agentId);
            List<KnowledgeItem> memoryItems = extractFromMemory(project, agentId);
            if (memoryItems.isEmpty()) {
                return DreamResult.failure("没有近期对话，也没有可提取的记忆");
            }
            // 去重并保存
            List<KnowledgeItem> newItems = deduplicate(project, agentId, memoryItems);
            int savedCount = 0;
            for (KnowledgeItem item : newItems) {
                String key = item.category() + "_" + System.currentTimeMillis();
                memoryManager.saveMemory(project, agentId, item.category(), key, item.content());
                savedCount++;
            }
            DreamResult result = new DreamResult(true, savedCount, memoryItems.size(), newItems);
            log.info("Dream 从记忆完成: agent={}, project={}, 提取={}, 保存={}",
                agentId, project.getId(), memoryItems.size(), savedCount);
            if (knowledgeEvolutionService != null && savedCount > 0) {
                knowledgeEvolutionService.recordDreamExtraction(agentId, project.getId(), savedCount);
            }
            knowledgeBase.recordKnowledgeExtraction(agentId, project.getId(), memoryItems.size(), savedCount);
            return result;
        }

        // 2. 提取知识项
        List<KnowledgeItem> extractedItems = extractKnowledge(recentMessages);

        // 3. 去重：与已有知识比对
        List<KnowledgeItem> newItems = deduplicate(project, agentId, extractedItems);

        // 4. 限制数量
        int maxItems = configService.getInt("dream.max-extract-items", 20);
        if (newItems.size() > maxItems) {
            newItems = newItems.subList(0, maxItems);
        }

        // 5. 保存到知识库
        int savedCount = 0;
        for (KnowledgeItem item : newItems) {
            String key = item.category() + "_" + System.currentTimeMillis();
            memoryManager.saveMemory(project, agentId, item.category(), key, item.content());
            savedCount++;
        }

        DreamResult result = new DreamResult(true, savedCount, extractedItems.size(), newItems);
        log.info("Dream 完成: agent={}, project={}, 提取={}, 保存={}",
            agentId, project.getId(), extractedItems.size(), savedCount);

        // 联动知识进化服务，更新文档计数
        if (knowledgeEvolutionService != null && savedCount > 0) {
            knowledgeEvolutionService.recordDreamExtraction(agentId, project.getId(), savedCount);
        }

        // 记录知识提取使用事件
        knowledgeBase.recordKnowledgeExtraction(agentId, project.getId(), extractedItems.size(), savedCount);

        return result;
    }

    /**
     * 从对话中提取知识项
     */
    private List<KnowledgeItem> extractKnowledge(List<ContextManager.ConversationMessage> messages) {
        List<KnowledgeItem> items = new ArrayList<>();

        for (ContextManager.ConversationMessage msg : messages) {
            String content = msg.getContent();
            if (content == null || content.length() < 20) continue;

            // 提取关键决策
            if (containsAny(content, DECISION_KEYWORDS)) {
                String summary = summarize(content, 150);
                items.add(new KnowledgeItem("knowledge", "决策: " + summary, content));
            }

            // 提取技术方案
            if (containsAny(content, TECH_KEYWORDS)) {
                String summary = summarize(content, 150);
                items.add(new KnowledgeItem("knowledge", "方案: " + summary, content));
            }

            // 提取经验教训
            if (containsAny(content, LESSON_KEYWORDS)) {
                String summary = summarize(content, 150);
                items.add(new KnowledgeItem("experiences", "经验: " + summary, content));
            }

            // 提取待办事项
            if (containsAny(content, TODO_KEYWORDS)) {
                String summary = summarize(content, 100);
                items.add(new KnowledgeItem("general", "待办: " + summary, content));
            }
        }

        return items;
    }

    /**
     * 从 Agent 记忆中提取知识项
     * 当没有近期对话时，从已有的记忆文件中提取可复用的知识
     */
    private List<KnowledgeItem> extractFromMemory(GameProject project, String agentId) {
        List<KnowledgeItem> items = new ArrayList<>();
        try {
            Map<String, String> memories = memoryManager.getAllMemory(project, agentId);
            if (memories == null || memories.isEmpty()) return items;

            for (Map.Entry<String, String> entry : memories.entrySet()) {
                String key = entry.getKey();
                String content = entry.getValue();
                if (content == null || content.length() < 20) continue;

                // 从记忆中提取有价值的内容
                if (key.contains("experience") || key.contains("lesson") || key.contains("decision")) {
                    String summary = summarize(content, 150);
                    items.add(new KnowledgeItem("experiences", "记忆经验: " + summary, content));
                } else if (key.contains("solution") || key.contains("pattern")) {
                    String summary = summarize(content, 150);
                    items.add(new KnowledgeItem("knowledge", "记忆方案: " + summary, content));
                } else if (content.length() > 50) {
                    // 通用记忆，长度足够则提取
                    String summary = summarize(content, 100);
                    items.add(new KnowledgeItem("general", "记忆: " + summary, content));
                }
            }
        } catch (Exception e) {
            log.warn("从记忆提取知识失败: {}", e.getMessage());
        }
        return items;
    }

    /**
     * 去重：过滤已有的知识
     */
    private List<KnowledgeItem> deduplicate(GameProject project, String agentId, List<KnowledgeItem> items) {
        // 加载已有知识
        Map<String, String> existingKnowledge = memoryManager.getAllMemory(project, agentId);

        // 简单去重：检查标题是否已存在
        Set<String> existingTitles = existingKnowledge.values().stream()
            .map(v -> v.length() > 50 ? v.substring(0, 50) : v)
            .collect(Collectors.toSet());

        return items.stream()
            .filter(item -> {
                String titlePrefix = item.title().length() > 50 ? item.title().substring(0, 50) : item.title();
                return !existingTitles.contains(titlePrefix);
            })
            .collect(Collectors.toList());
    }

    private boolean containsAny(String text, String[] keywords) {
        String lower = text.toLowerCase();
        for (String keyword : keywords) {
            if (lower.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String summarize(String content, int maxLength) {
        if (content.length() <= maxLength) {
            return content;
        }
        // 尝试在句号处截断
        int periodIndex = content.indexOf('。', maxLength / 2);
        if (periodIndex > 0 && periodIndex < maxLength) {
            return content.substring(0, periodIndex + 1);
        }
        return content.substring(0, maxLength) + "...";
    }

    // ===== 关键词定义 =====

    private static final String[] DECISION_KEYWORDS = {
        "决定", "选择", "确认", "采用", "放弃", "方案", "使用", "切换",
        "agree", "decide", "confirm", "choose", "最终决定", "经讨论"
    };

    private static final String[] TECH_KEYWORDS = {
        "架构", "设计", "模式", "接口", "重构", "优化", "缓存", "数据库",
        "architecture", "design", "pattern", "refactor", "performance"
    };

    private static final String[] LESSON_KEYWORDS = {
        "踩坑", "教训", "注意", "避免", "坑", "问题", "修复", "fix",
        "bug", "error", "异常", "崩溃", "crash", "排查"
    };

    private static final String[] TODO_KEYWORDS = {
        "TODO", "FIXME", "待完成", "待处理", "待办", "下一步", "计划",
        "todo", "fixme", "待实现", "待开发", "待测试"
    };

    // ===== 内部类 =====

    /**
     * 知识项
     */
    public record KnowledgeItem(String category, String title, String content) {}

    /**
     * Dream 结果
     */
    public record DreamResult(boolean success, int savedCount, int extractedCount, List<KnowledgeItem> items) {
        static DreamResult failure(String reason) {
            return new DreamResult(false, 0, 0, Collections.emptyList());
        }
    }
}
