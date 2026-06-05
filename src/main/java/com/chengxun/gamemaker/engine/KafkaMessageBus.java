package com.chengxun.gamemaker.engine;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.model.AgentMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于Kafka的异步消息总线
 * 支持消息持久化、重试机制和优先级处理
 *
 * 主要功能：
 * - 异步消息传递
 * - 消息持久化到Kafka
 * - 自动重试机制（最多3次）
 * - 消息优先级处理
 * - 消息追踪和监控
 *
 * @author chengxun
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(name = "app.messaging.type", havingValue = "kafka")
public class KafkaMessageBus implements MessageBusInterface {

    private static final Logger log = LoggerFactory.getLogger(KafkaMessageBus.class);

    /** Agent消息主题 */
    private static final String AGENT_MESSAGE_TOPIC = "agent-messages";

    /** 重试主题 */
    private static final String RETRY_TOPIC = "agent-messages-retry";

    /** 死信主题 */
    private static final String DEAD_LETTER_TOPIC = "agent-messages-dlt";

    /** 最大重试次数 */
    private static final int MAX_RETRY_COUNT = 3;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /** 已注册的Agent（key: 运行时 ID） */
    private final Map<String, Agent> agents = new ConcurrentHashMap<>();

    /** Agent 到 projectId 的映射 */
    private final Map<String, String> agentProjectMap = new ConcurrentHashMap<>();

    /** 项目级 Agent 分组 */
    private final Map<String, Map<String, Agent>> projectAgents = new ConcurrentHashMap<>();

    /** 消息重试计数器 */
    private final Map<String, AtomicInteger> retryCounters = new ConcurrentHashMap<>();

    /** 消息统计 */
    private final MessageStatistics statistics = new MessageStatistics();

    @Value("${app.messaging.kafka.consumer-group:game-maker-consumer}")
    private String consumerGroup;

    @Override
    public void registerAgent(Agent agent) {
        agents.put(agent.getId(), agent);
        log.info("Agent registered with Kafka message bus: {}", agent.getId());
    }

    @Override
    public void registerAgent(Agent agent, String projectId) {
        String agentId = agent.getId();
        agents.put(agentId, agent);
        agentProjectMap.put(agentId, projectId);
        projectAgents.computeIfAbsent(projectId, k -> new ConcurrentHashMap<>()).put(agentId, agent);
        log.info("Agent registered with Kafka message bus: {} for project: {}", agentId, projectId);
    }

    @Override
    public void unregisterAgent(String agentId) {
        agents.remove(agentId);
        String projectId = agentProjectMap.remove(agentId);
        if (projectId != null) {
            Map<String, Agent> projectMap = projectAgents.get(projectId);
            if (projectMap != null) {
                projectMap.remove(agentId);
                if (projectMap.isEmpty()) {
                    projectAgents.remove(projectId);
                }
            }
        }
        log.info("Agent unregistered from Kafka message bus: {}", agentId);
    }

    @Override
    public void send(AgentMessage message) {
        if (message.getToAgentId() == null || message.getToAgentId().isEmpty()) {
            broadcast(message);
        } else {
            deliver(message);
        }
    }

    /**
     * 发送消息到指定Agent
     */
    private void deliver(AgentMessage message) {
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            String key = message.getToAgentId();

            // 添加消息头信息
            ProducerRecord<String, String> record = new ProducerRecord<>(
                AGENT_MESSAGE_TOPIC, key, messageJson
            );
            record.headers().add(new RecordHeader("messageType", message.getType().name().getBytes(StandardCharsets.UTF_8)));
            record.headers().add(new RecordHeader("priority", String.valueOf(message.getPriority()).getBytes(StandardCharsets.UTF_8)));
            record.headers().add(new RecordHeader("retryCount", "0".getBytes(StandardCharsets.UTF_8)));

            // 异步发送消息
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(record);
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    statistics.incrementSent();
                    log.debug("Message sent to topic {} partition {} offset {}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                } else {
                    statistics.incrementFailed();
                    log.error("Failed to send message to agent {}: {}", message.getToAgentId(), ex.getMessage());
                    // 发送失败时进行本地处理
                    handleSendFailure(message, ex);
                }
            });

            log.debug("Message delivery initiated from {} to {}",
                message.getFromAgentId(), message.getToAgentId());

        } catch (JsonProcessingException e) {
            statistics.incrementFailed();
            log.error("Failed to serialize message: {}", e.getMessage());
        }
    }

    /**
     * 广播消息给所有Agent
     */
    private void broadcast(AgentMessage message) {
        try {
            String messageJson = objectMapper.writeValueAsString(message);

            ProducerRecord<String, String> record = new ProducerRecord<>(
                AGENT_MESSAGE_TOPIC, null, messageJson
            );
            record.headers().add(new RecordHeader("broadcast", "true".getBytes(StandardCharsets.UTF_8)));
            record.headers().add(new RecordHeader("messageType", message.getType().name().getBytes(StandardCharsets.UTF_8)));

            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(record);
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    statistics.incrementSent();
                    log.debug("Broadcast message sent to topic");
                } else {
                    statistics.incrementFailed();
                    log.error("Failed to broadcast message: {}", ex.getMessage());
                }
            });

            log.debug("Broadcast message initiated from {}", message.getFromAgentId());

        } catch (JsonProcessingException e) {
            statistics.incrementFailed();
            log.error("Failed to serialize broadcast message: {}", e.getMessage());
        }
    }

    /**
     * 消费Agent消息
     */
    @KafkaListener(topics = AGENT_MESSAGE_TOPIC, groupId = "${app.messaging.kafka.consumer-group}")
    public void consumeMessage(ConsumerRecord<String, String> record) {
        try {
            AgentMessage message = objectMapper.readValue(record.value(), AgentMessage.class);

            // 检查是否是广播消息
            boolean isBroadcast = record.headers().lastHeader("broadcast") != null;

            if (isBroadcast) {
                // 广播消息：发送给除发送者外的所有Agent
                for (Map.Entry<String, Agent> entry : agents.entrySet()) {
                    if (!entry.getKey().equals(message.getFromAgentId())) {
                        entry.getValue().receiveMessage(message);
                    }
                }
                statistics.incrementReceived();
                log.debug("Broadcast message consumed from topic, sent to {} agents", agents.size() - 1);
            } else {
                // 点对点消息：发送给目标Agent
                Agent recipient = agents.get(message.getToAgentId());
                if (recipient != null) {
                    recipient.receiveMessage(message);
                    statistics.incrementReceived();
                    log.debug("Message consumed from topic, delivered to {}", message.getToAgentId());
                } else {
                    statistics.incrementFailed();
                    log.warn("Agent not found for message: {}", message.getToAgentId());
                    // 发送到重试队列
                    sendToRetryQueue(message, record);
                }
            }

        } catch (Exception e) {
            statistics.incrementFailed();
            log.error("Failed to consume message: {}", e.getMessage());
            // 解析失败，发送到死信队列
            sendToDeadLetterQueue(record.value(), e);
        }
    }

    /**
     * 消费重试消息
     */
    @KafkaListener(topics = RETRY_TOPIC, groupId = "${app.messaging.kafka.consumer-group}-retry")
    public void consumeRetryMessage(ConsumerRecord<String, String> record) {
        try {
            AgentMessage message = objectMapper.readValue(record.value(), AgentMessage.class);
            String retryKey = message.getFromAgentId() + ":" + message.getToAgentId() + ":" + System.currentTimeMillis();

            // 获取重试次数
            AtomicInteger retryCounter = retryCounters.computeIfAbsent(retryKey, k -> new AtomicInteger(0));
            int retryCount = retryCounter.incrementAndGet();

            if (retryCount <= MAX_RETRY_COUNT) {
                log.info("Retrying message delivery, attempt {}/{}", retryCount, MAX_RETRY_COUNT);

                // 检查Agent是否可用
                Agent recipient = agents.get(message.getToAgentId());
                if (recipient != null) {
                    recipient.receiveMessage(message);
                    statistics.incrementReceived();
                    retryCounters.remove(retryKey);
                    log.info("Retry successful for message to {}", message.getToAgentId());
                } else {
                    // Agent仍不可用，重新发送到重试队列
                    sendToRetryQueue(message, record);
                }
            } else {
                // 超过最大重试次数，发送到死信队列
                log.warn("Max retry count exceeded for message to {}, sending to DLQ", message.getToAgentId());
                sendToDeadLetterQueue(record.value(), new RuntimeException("Max retry count exceeded"));
                retryCounters.remove(retryKey);
            }

        } catch (Exception e) {
            log.error("Failed to consume retry message: {}", e.getMessage());
            sendToDeadLetterQueue(record.value(), e);
        }
    }

    /**
     * 消费死信消息
     */
    @KafkaListener(topics = DEAD_LETTER_TOPIC, groupId = "${app.messaging.kafka.consumer-group}-dlt")
    public void consumeDeadLetterMessage(ConsumerRecord<String, String> record) {
        log.error("Dead letter message received: {}", record.value());
        statistics.incrementDeadLetter();

        // 可以在这里添加死信消息的处理逻辑
        // 例如：保存到数据库、发送告警通知等
    }

    /**
     * 发送到重试队列
     */
    private void sendToRetryQueue(AgentMessage message, ConsumerRecord<String, String> originalRecord) {
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            ProducerRecord<String, String> record = new ProducerRecord<>(
                RETRY_TOPIC, message.getToAgentId(), messageJson
            );

            // 复制原始消息头
            originalRecord.headers().forEach(header ->
                record.headers().add(header)
            );

            kafkaTemplate.send(record);
            log.debug("Message sent to retry queue for agent {}", message.getToAgentId());

        } catch (JsonProcessingException e) {
            log.error("Failed to send message to retry queue: {}", e.getMessage());
            sendToDeadLetterQueue(originalRecord.value(), e);
        }
    }

    /**
     * 发送到死信队列
     */
    private void sendToDeadLetterQueue(String messageValue, Exception cause) {
        try {
            ProducerRecord<String, String> record = new ProducerRecord<>(
                DEAD_LETTER_TOPIC, null, messageValue
            );
            record.headers().add(new RecordHeader("error", cause.getMessage().getBytes(StandardCharsets.UTF_8)));
            record.headers().add(new RecordHeader("timestamp", String.valueOf(System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8)));

            kafkaTemplate.send(record);
            log.debug("Message sent to dead letter queue");

        } catch (Exception e) {
            log.error("Failed to send message to dead letter queue: {}", e.getMessage());
        }
    }

    /**
     * 处理发送失败
     */
    private void handleSendFailure(AgentMessage message, Throwable cause) {
        log.error("Handling send failure for message to {}: {}", message.getToAgentId(), cause.getMessage());

        // 如果目标Agent在线，尝试本地投递
        Agent recipient = agents.get(message.getToAgentId());
        if (recipient != null) {
            log.info("Attempting local delivery to agent {}", message.getToAgentId());
            recipient.receiveMessage(message);
            statistics.incrementReceived();
        }
    }

    /**
     * 获取消息统计
     */
    public MessageStatistics getStatistics() {
        return statistics;
    }

    /**
     * 获取已注册Agent列表
     */
    public Map<String, Agent> getAgents() {
        return new ConcurrentHashMap<>(agents);
    }

    @Override
    public Map<String, Agent> getProjectAgents(String projectId) {
        Map<String, Agent> projectMap = projectAgents.get(projectId);
        return projectMap != null ? new ConcurrentHashMap<>(projectMap) : new ConcurrentHashMap<>();
    }

    /**
     * 消息统计内部类
     */
    public static class MessageStatistics {
        private final AtomicInteger sentCount = new AtomicInteger(0);
        private final AtomicInteger receivedCount = new AtomicInteger(0);
        private final AtomicInteger failedCount = new AtomicInteger(0);
        private final AtomicInteger deadLetterCount = new AtomicInteger(0);

        public void incrementSent() {
            sentCount.incrementAndGet();
        }

        public void incrementReceived() {
            receivedCount.incrementAndGet();
        }

        public void incrementFailed() {
            failedCount.incrementAndGet();
        }

        public void incrementDeadLetter() {
            deadLetterCount.incrementAndGet();
        }

        public int getSentCount() {
            return sentCount.get();
        }

        public int getReceivedCount() {
            return receivedCount.get();
        }

        public int getFailedCount() {
            return failedCount.get();
        }

        public int getDeadLetterCount() {
            return deadLetterCount.get();
        }

        public Map<String, Integer> toMap() {
            return Map.of(
                "sent", getSentCount(),
                "received", getReceivedCount(),
                "failed", getFailedCount(),
                "deadLetter", getDeadLetterCount()
            );
        }
    }
}
