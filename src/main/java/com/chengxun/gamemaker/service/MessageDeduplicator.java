package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.config.SystemConstants;
import com.chengxun.gamemaker.model.AgentMessage;
import com.chengxun.gamemaker.web.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息去重合并器
 * 在指定时间窗口内合并相同来源和类型的连续消息，减少 AI 调用次数
 *
 * 工作原理：
 * - 维护每个 Agent 的消息窗口
 * - 窗口内相同 fromAgentId + type 的消息合并为一条
 * - 合并后的内容用分隔符拼接
 * - 窗口过期后自动 flush
 *
 * @author chengxun
 * @since 2.0.0
 */
@Component
public class MessageDeduplicator {

    private static final Logger log = LoggerFactory.getLogger(MessageDeduplicator.class);

    @Autowired
    private SystemConfigService configService;

    /**
     * 每个 Agent 的消息合并窗口
     * key: toAgentId
     * value: 合并窗口
     */
    private final ConcurrentHashMap<String, MergeWindow> windows = new ConcurrentHashMap<>();

    /**
     * 合并窗口
     */
    private static class MergeWindow {
        /** 窗口开始时间 */
        long windowStart;
        /** 来源 Agent ID */
        String fromAgentId;
        /** 消息类型 */
        AgentMessage.MessageType type;
        /** 合并后的内容列表 */
        final List<String> contents = new ArrayList<>();
        /** 原始消息列表（用于保留元数据） */
        final List<AgentMessage> messages = new ArrayList<>();

        MergeWindow(AgentMessage firstMessage) {
            this.windowStart = System.currentTimeMillis();
            this.fromAgentId = firstMessage.getFromAgentId();
            this.type = firstMessage.getType();
            this.contents.add(firstMessage.getContent());
            this.messages.add(firstMessage);
        }

        /** 检查消息是否可以合并到此窗口 */
        boolean canMerge(AgentMessage msg) {
            long windowMs = 30000; // 默认30秒，实际从配置读取
            return System.currentTimeMillis() - windowStart < windowMs
                && fromAgentId.equals(msg.getFromAgentId())
                && type == msg.getType();
        }

        /** 合并消息 */
        void merge(AgentMessage msg) {
            contents.add(msg.getContent());
            messages.add(msg);
        }

        /** 获取合并后的消息 */
        AgentMessage getMergedMessage() {
            if (messages.isEmpty()) return null;
            if (messages.size() == 1) return messages.get(0);

            // 合并内容
            StringBuilder merged = new StringBuilder();
            merged.append(String.format("[合并了 %d 条消息]\n\n", messages.size()));
            for (int i = 0; i < contents.size(); i++) {
                merged.append(String.format("--- 消息 %d ---\n", i + 1));
                merged.append(contents.get(i)).append("\n\n");
            }

            // 基于第一条消息创建合并消息
            AgentMessage first = messages.get(0);
            return AgentMessage.builder()
                .fromAgentId(first.getFromAgentId())
                .toAgentId(first.getToAgentId())
                .type(first.getType())
                .content(merged.toString().trim())
                .build();
        }
    }

    /**
     * 处理消息去重合并
     * 如果消息在窗口内且来源/类型相同，合并后返回 null
     * 如果窗口过期或来源/类型不同，返回合并后的消息并开启新窗口
     *
     * @param message 新到达的消息
     * @return 合并后的消息（应处理），null（已合并到窗口，暂不处理）
     */
    public AgentMessage process(AgentMessage message) {
        String toAgentId = message.getToAgentId();
        long windowMs = configService.getInt(SystemConstants.AGENT_MESSAGE_DEDUP_WINDOW_SECONDS, 30) * 1000L;

        MergeWindow existing = windows.get(toAgentId);

        if (existing != null && existing.canMerge(message)) {
            // 在窗口内，合并
            existing.merge(message);
            log.debug("Merged message from {} to {} (window size: {})",
                message.getFromAgentId(), toAgentId, existing.contents.size());
            return null;
        }

        // 窗口过期或来源/类型不同，flush 旧窗口
        AgentMessage result = null;
        if (existing != null) {
            result = existing.getMergedMessage();
            if (result != null && existing.messages.size() > 1) {
                log.info("Flushed merge window for agent {}: {} messages merged",
                    toAgentId, existing.messages.size());
            }
        }

        // 开启新窗口
        MergeWindow newWindow = new MergeWindow(message);
        newWindow.windowStart = System.currentTimeMillis(); // 使用实际配置的窗口时间
        windows.put(toAgentId, newWindow);

        return result;
    }

    /**
     * 强制 flush 指定 Agent 的合并窗口
     *
     * @param agentId Agent ID
     * @return 合并后的消息，窗口为空返回 null
     */
    public AgentMessage flush(String agentId) {
        MergeWindow window = windows.remove(agentId);
        if (window == null) return null;
        return window.getMergedMessage();
    }

    /**
     * flush 所有窗口
     *
     * @return 所有待处理的合并消息
     */
    public List<AgentMessage> flushAll() {
        List<AgentMessage> result = new ArrayList<>();
        for (String agentId : windows.keySet()) {
            AgentMessage msg = flush(agentId);
            if (msg != null) {
                result.add(msg);
            }
        }
        return result;
    }
}
