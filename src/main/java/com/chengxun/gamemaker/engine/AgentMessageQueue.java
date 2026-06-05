package com.chengxun.gamemaker.engine;

import com.chengxun.gamemaker.config.SystemConstants;
import com.chengxun.gamemaker.model.AgentMessage;
import com.chengxun.gamemaker.web.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Agent消息队列
 * 提供AGENT之间的异步消息通信机制，支持：
 * - 消息优先级
 * - 消息持久化（内存）
 * - 消息消费回调
 * - 消息重试
 *
 * 配置项（通过 SystemConfigService 动态读取）：
 * - agent.message-queue-size: 消息队列最大容量
 * - agent.max-retry-count: 消息最大重试次数
 * - agent.history-size: 历史记录保留数量
 *
 * @author chengxun
 * @since 1.0.0
 */
@Component
public class AgentMessageQueue {

    private static final Logger log = LoggerFactory.getLogger(AgentMessageQueue.class);

    /** 默认消息队列大小（配置不存在时使用） */
    private static final int DEFAULT_QUEUE_SIZE = 1000;

    /** 默认最大重试次数（配置不存在时使用） */
    private static final int DEFAULT_RETRY_COUNT = 3;

    /** 默认历史记录大小（配置不存在时使用） */
    private static final int DEFAULT_HISTORY_SIZE = 2000;

    /** 消息队列：按目标Agent分组 */
    private final ConcurrentHashMap<String, PriorityBlockingQueue<AgentMessage>> messageQueues = new ConcurrentHashMap<>();

    /** 消息处理器：按Agent注册 */
    private final ConcurrentHashMap<String, Consumer<AgentMessage>> messageHandlers = new ConcurrentHashMap<>();

    /** 消息历史：用于审计和重试 */
    private final ConcurrentHashMap<String, AgentMessage> messageHistory = new ConcurrentHashMap<>();

    /** 线程池：用于异步消息处理 */
    private final ExecutorService executorService = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("agent-msg-processor-" + t.getId());
        return t;
    });

    /** 配置服务 */
    private final SystemConfigService configService;

    public AgentMessageQueue(SystemConfigService configService) {
        this.configService = configService;
    }

    /**
     * 获取最大队列大小（从配置读取）
     */
    private int getMaxQueueSize() {
        return configService.getInt(SystemConstants.AGENT_MESSAGE_QUEUE_SIZE, DEFAULT_QUEUE_SIZE);
    }

    /**
     * 获取最大重试次数（从配置读取）
     */
    private int getMaxRetryCount() {
        return configService.getInt(SystemConstants.AGENT_MAX_RETRY_COUNT, DEFAULT_RETRY_COUNT);
    }

    /**
     * 获取历史记录最大大小（从配置读取）
     */
    private int getMaxHistorySize() {
        return configService.getInt(SystemConstants.AGENT_HISTORY_SIZE, DEFAULT_HISTORY_SIZE);
    }

    /**
     * 注册Agent的消息处理器
     */
    public void registerHandler(String agentId, Consumer<AgentMessage> handler) {
        messageHandlers.put(agentId, handler);
        messageQueues.putIfAbsent(agentId, new PriorityBlockingQueue<>(11,
            (a, b) -> Integer.compare(b.getPriority(), a.getPriority()))); // 优先级高的排在前面
        log.info("Message handler registered for agent: {}", agentId);
    }

    /**
     * 注销Agent的消息处理器
     */
    public void unregisterHandler(String agentId) {
        messageHandlers.remove(agentId);
        messageQueues.remove(agentId);
        log.info("Message handler unregistered for agent: {}", agentId);
    }

    /**
     * 发送消息到指定Agent
     */
    public void send(AgentMessage message) {
        if (message.getToAgentId() == null) {
            log.warn("Cannot send message without target agent ID");
            return;
        }

        // 设置消息ID
        if (message.getId() == null) {
            message.setId(java.util.UUID.randomUUID().toString());
        }

        // 设置时间戳
        if (message.getTimestamp() == null) {
            message.setTimestamp(java.time.LocalDateTime.now());
            message.setTimestampMs(System.currentTimeMillis());
        }

        // 添加到目标队列
        PriorityBlockingQueue<AgentMessage> queue = messageQueues.computeIfAbsent(
            message.getToAgentId(),
            k -> new PriorityBlockingQueue<>(11, (a, b) -> Integer.compare(b.getPriority(), a.getPriority()))
        );

        int maxSize = getMaxQueueSize();
        if (queue.size() >= maxSize) {
            log.warn("Message queue for agent {} is full (size={}/{}), dropping oldest message",
                message.getToAgentId(), queue.size(), maxSize);
            queue.poll(); // 移除最旧的消息
        }

        queue.offer(message);
        messageHistory.put(message.getId(), message);

        log.debug("Message sent from {} to {}: {}", message.getFromAgentId(), message.getToAgentId(), message.getType());

        // 异步处理消息
        processMessageAsync(message.getToAgentId());
    }

    /**
     * 广播消息给所有Agent
     */
    public void broadcast(AgentMessage message) {
        messageHandlers.keySet().forEach(agentId -> {
            if (!agentId.equals(message.getFromAgentId())) { // 不发送给自己
                AgentMessage copy = copyMessage(message);
                copy.setToAgentId(agentId);
                send(copy);
            }
        });
    }

    /**
     * 异步处理消息
     */
    private void processMessageAsync(String agentId) {
        Consumer<AgentMessage> handler = messageHandlers.get(agentId);
        if (handler == null) {
            log.debug("No handler registered for agent: {}, message will be queued", agentId);
            return;
        }

        executorService.submit(() -> {
            PriorityBlockingQueue<AgentMessage> queue = messageQueues.get(agentId);
            if (queue == null) return;

            AgentMessage message;
            while ((message = queue.poll()) != null) {
                try {
                    handler.accept(message);
                    message.setStatus(AgentMessage.MessageStatus.PROCESSED);
                    log.debug("Message processed: {}", message.getId());
                } catch (Exception e) {
                    log.error("Error processing message {} for agent {}: {}",
                        message.getId(), agentId, e.getMessage());
                    handleMessageError(message, e);
                }
            }
        });
    }

    /**
     * 处理消息错误（支持重试）
     */
    private void handleMessageError(AgentMessage message, Exception error) {
        int retryCount = message.getRetryCount() != null ? message.getRetryCount() : 0;
        int maxRetry = getMaxRetryCount();

        if (retryCount < maxRetry) {
            message.setRetryCount(retryCount + 1);
            message.setStatus(AgentMessage.MessageStatus.RETRYING);
            log.info("Retrying message {} (attempt {}/{})", message.getId(), retryCount + 1, maxRetry);

            // 延迟重试
            executorService.submit(() -> {
                try {
                    Thread.sleep(1000 * retryCount); // 递增延迟
                    send(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        } else {
            message.setStatus(AgentMessage.MessageStatus.FAILED);
            message.setError(error.getMessage());
            log.error("Message {} failed after {} retries", message.getId(), maxRetry);
        }
    }

    /**
     * 获取Agent的待处理消息数量
     */
    public int getPendingMessageCount(String agentId) {
        PriorityBlockingQueue<AgentMessage> queue = messageQueues.get(agentId);
        return queue != null ? queue.size() : 0;
    }

    /**
     * 获取消息历史
     */
    public AgentMessage getMessageById(String messageId) {
        return messageHistory.get(messageId);
    }

    /**
     * 清空Agent的消息队列
     */
    public void clearQueue(String agentId) {
        PriorityBlockingQueue<AgentMessage> queue = messageQueues.get(agentId);
        if (queue != null) {
            queue.clear();
        }
    }

    /**
     * 复制消息（用于广播）
     */
    private AgentMessage copyMessage(AgentMessage original) {
        return AgentMessage.builder()
            .fromAgentId(original.getFromAgentId())
            .type(original.getType())
            .content(original.getContent())
            .priority(original.getPriority())
            .build();
    }

    /**
     * 定期清理过期的消息历史，防止内存无限增长
     * 每10分钟执行一次，保留最近的MAX_HISTORY_SIZE条记录
     */
    @Scheduled(fixedRate = 600000)
    public void cleanupMessageHistory() {
        int maxSize = getMaxHistorySize();
        if (messageHistory.size() > maxSize) {
            int toRemove = messageHistory.size() - maxSize;
            messageHistory.entrySet().stream()
                .sorted((a, b) -> {
                    long timeA = a.getValue().getTimestampMs() != null ? a.getValue().getTimestampMs() : 0;
                    long timeB = b.getValue().getTimestampMs() != null ? b.getValue().getTimestampMs() : 0;
                    return Long.compare(timeA, timeB);
                })
                .limit(toRemove)
                .forEach(entry -> messageHistory.remove(entry.getKey()));
            log.info("Cleaned up {} old message history entries", toRemove);
        }
    }

    /**
     * 关闭消息队列
     */
    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Message queue shutdown");
    }
}
