package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.manager.ContextManager;
import com.chengxun.gamemaker.model.AgentContext;
import com.chengxun.gamemaker.model.GameProject;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话分叉服务
 * 支持会话分叉（fork），像 git branch 一样做探索性尝试
 *
 * 主要功能：
 * - fork：从当前会话创建分叉
 * - switch：切换到分叉会话
 * - merge：将分叉合并回主会话
 * - discard：丢弃分叉
 * - list：列出所有分叉
 *
 * 使用场景：
 * - 探索性尝试：不确定方案是否可行时，先 fork 一个会话尝试
 * - 并行方案对比：fork 多个会话尝试不同方案
 * - 安全实验：在分叉中实验，不影响主会话
 *
 * 灵感来源：会话分叉与合并机制
 *
 * @author chengxun
 * @since 3.0.0
 */
@Service
public class SessionForkService {

    private static final Logger log = LoggerFactory.getLogger(SessionForkService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Autowired
    private ContextManager contextManager;

    @Autowired
    private SnapshotService snapshotService;

    @Autowired
    private ObjectMapper objectMapper;

    /** 分叉注册表：key=forkId, value=分叉信息 */
    private final ConcurrentHashMap<String, ForkInfo> forkRegistry = new ConcurrentHashMap<>();

    /**
     * 创建会话分叉
     *
     * @param agentId 原始 Agent ID
     * @param project 项目
     * @param description 分叉描述
     * @return 分叉信息
     */
    public ForkInfo fork(String agentId, GameProject project, String description) {
        if (project == null) return null;

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String forkId = agentId + "-fork-" + timestamp;

        // 1. 保存当前会话的快照
        AgentContext currentContext = contextManager.loadContext(agentId, project);
        if (currentContext == null) {
            log.warn("无法分叉：当前上下文为空 agent={}", agentId);
            return null;
        }

        // 2. 创建分叉上下文（复制当前上下文）
        AgentContext forkContext = copyContext(currentContext, forkId);
        contextManager.saveContext(forkContext, project);

        // 3. 复制最近对话
        List<ContextManager.ConversationMessage> recentMessages =
            contextManager.getRecentMessages(agentId, project, 50);
        // 注意：这里只记录引用，实际复制需要 ContextManager 支持

        // 4. 注册分叉
        String forkAgentId = agentId + ":fork:" + forkId;
        ForkInfo forkInfo = new ForkInfo(
            forkId,
            forkAgentId,
            agentId,
            project.getId(),
            description != null ? description : "探索性分叉",
            ForkStatus.ACTIVE,
            LocalDateTime.now(),
            null,
            recentMessages.size()
        );
        forkRegistry.put(forkId, forkInfo);

        log.info("会话已分叉: agent={}, forkId={}, description={}", agentId, forkId, description);
        return forkInfo;
    }

    /**
     * 切换到分叉会话
     *
     * @param forkId 分叉 ID
     * @return 分叉的 Agent ID（用于后续操作）
     */
    public String switchToFork(String forkId) {
        ForkInfo info = forkRegistry.get(forkId);
        if (info == null || info.status() != ForkStatus.ACTIVE) {
            log.warn("分叉不存在或已结束: forkId={}", forkId);
            return null;
        }

        log.info("切换到分叉: forkId={}", forkId);
        return info.forkAgentId();
    }

    /**
     * 将分叉合并回主会话
     *
     * @param forkId 分叉 ID
     * @param strategy 合并策略（replace=替换, append=追加, merge=智能合并）
     * @return 是否合并成功
     */
    public boolean merge(String forkId, MergeStrategy strategy) {
        ForkInfo info = forkRegistry.get(forkId);
        if (info == null || info.status() != ForkStatus.ACTIVE) {
            return false;
        }

        try {
            switch (strategy) {
                case REPLACE -> {
                    // 用分叉的上下文替换主会话
                    AgentContext forkContext = contextManager.loadContext(info.forkAgentId(), null);
                    if (forkContext != null) {
                        AgentContext mainContext = copyContext(forkContext, info.parentAgentId());
                        contextManager.saveContext(mainContext, null);
                    }
                }
                case APPEND -> {
                    // 将分叉的对话追加到主会话
                    List<ContextManager.ConversationMessage> forkMessages =
                        contextManager.getRecentMessages(info.forkAgentId(), null, 100);
                    // 追加到主会话（需要 ContextManager 支持批量追加）
                }
                case MERGE -> {
                    // 智能合并：保留主会话状态，合并分叉的工作记忆
                    AgentContext forkContext = contextManager.loadContext(info.forkAgentId(), null);
                    AgentContext mainContext = contextManager.loadContext(info.parentAgentId(), null);
                    if (forkContext != null && mainContext != null) {
                        mergeWorkingMemory(mainContext, forkContext);
                        contextManager.saveContext(mainContext, null);
                    }
                }
            }

            // 更新分叉状态
            ForkInfo merged = new ForkInfo(
                info.forkId(), info.forkAgentId(), info.parentAgentId(), info.projectId(),
                info.description(), ForkStatus.MERGED, info.createdAt(),
                LocalDateTime.now(), info.messageCount()
            );
            forkRegistry.put(forkId, merged);

            log.info("分叉已合并: forkId={}, strategy={}", forkId, strategy);
            return true;
        } catch (Exception e) {
            log.error("合并分叉失败: forkId={}", forkId, e);
            return false;
        }
    }

    /**
     * 丢弃分叉
     *
     * @param forkId 分叉 ID
     * @return 是否丢弃成功
     */
    public boolean discard(String forkId) {
        ForkInfo info = forkRegistry.get(forkId);
        if (info == null) return false;

        ForkInfo discarded = new ForkInfo(
            info.forkId(), info.forkAgentId(), info.parentAgentId(), info.projectId(),
            info.description(), ForkStatus.DISCARDED, info.createdAt(),
            LocalDateTime.now(), info.messageCount()
        );
        forkRegistry.put(forkId, discarded);

        log.info("分叉已丢弃: forkId={}", forkId);
        return true;
    }

    /**
     * 列出 Agent 的所有分叉
     *
     * @param parentAgentId 父 Agent ID
     * @return 分叉列表
     */
    public List<ForkInfo> listForks(String parentAgentId) {
        return forkRegistry.values().stream()
            .filter(f -> f.parentAgentId().equals(parentAgentId))
            .sorted(Comparator.comparing(ForkInfo::createdAt).reversed())
            .toList();
    }

    /**
     * 获取活跃分叉
     *
     * @param parentAgentId 父 Agent ID
     * @return 活跃分叉列表
     */
    public List<ForkInfo> getActiveForks(String parentAgentId) {
        return forkRegistry.values().stream()
            .filter(f -> f.parentAgentId().equals(parentAgentId) && f.status() == ForkStatus.ACTIVE)
            .toList();
    }

    /**
     * 复制上下文
     */
    private AgentContext copyContext(AgentContext source, String newAgentId) {
        AgentContext copy = new AgentContext();
        copy.setAgentId(newAgentId);
        copy.setCurrentTaskId(source.getCurrentTaskId());

        // 复制工作记忆
        if (source.getWorkingMemory() != null) {
            for (AgentContext.WorkingMemoryItem item : source.getWorkingMemory()) {
                copy.addWorkingMemory(item.getKey(), item.getValue());
            }
        }

        return copy;
    }

    /**
     * 合并工作记忆
     */
    private void mergeWorkingMemory(AgentContext target, AgentContext source) {
        if (source.getWorkingMemory() == null) return;

        for (AgentContext.WorkingMemoryItem item : source.getWorkingMemory()) {
            // 分叉的记忆优先
            target.addWorkingMemory(item.getKey(), item.getValue());
        }
    }

    // ===== 内部类 =====

    /**
     * 分叉信息
     */
    public record ForkInfo(
        String forkId,
        String forkAgentId,
        String parentAgentId,
        String projectId,
        String description,
        ForkStatus status,
        LocalDateTime createdAt,
        LocalDateTime closedAt,
        int messageCount
    ) {}

    /**
     * 分叉状态
     */
    public enum ForkStatus {
        ACTIVE,     // 活跃中
        MERGED,     // 已合并
        DISCARDED   // 已丢弃
    }

    /**
     * 合并策略
     */
    public enum MergeStrategy {
        REPLACE,  // 替换主会话
        APPEND,   // 追加分叉对话
        MERGE     // 智能合并
    }
}
