package com.chengxun.gamemaker.config;

import com.chengxun.gamemaker.model.AgentMessage;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka消息队列配置类
 * 配置Kafka生产者、消费者和Topic
 *
 * 主要功能：
 * - 配置Kafka连接参数
 * - 创建必要的Topic
 * - 配置生产者和消费者工厂
 * - 支持JSON序列化
 *
 * 配置方式：
 * - 通过 app.messaging.type=kafka 启用
 * - 默认使用内存消息总线
 *
 * @author chengxun
 * @since 1.0.0
 */
@Configuration
@EnableKafka
@ConditionalOnProperty(name = "app.messaging.type", havingValue = "kafka")
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    /** Agent间消息Topic */
    public static final String AGENT_MESSAGES_TOPIC = "game-maker.agent.messages";

    /** Agent任务Topic */
    public static final String AGENT_TASKS_TOPIC = "game-maker.agent.tasks";

    /** Agent事件Topic */
    public static final String AGENT_EVENTS_TOPIC = "game-maker.agent.events";

    /** 系统告警Topic */
    public static final String SYSTEM_ALERTS_TOPIC = "game-maker.system.alerts";

    /**
     * Kafka AdminClient配置
     * 用于创建和管理Topic
     *
     * @return KafkaAdmin实例
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        log.info("Kafka Admin配置完成，服务器: {}", bootstrapServers);
        return new KafkaAdmin(configs);
    }

    /**
     * 创建Agent消息Topic
     *
     * @return NewTopic实例
     */
    @Bean
    public NewTopic agentMessagesTopic() {
        return new NewTopic(AGENT_MESSAGES_TOPIC, 3, (short) 1);
    }

    /**
     * 创建Agent任务Topic
     *
     * @return NewTopic实例
     */
    @Bean
    public NewTopic agentTasksTopic() {
        return new NewTopic(AGENT_TASKS_TOPIC, 3, (short) 1);
    }

    /**
     * 创建Agent事件Topic
     *
     * @return NewTopic实例
     */
    @Bean
    public NewTopic agentEventsTopic() {
        return new NewTopic(AGENT_EVENTS_TOPIC, 3, (short) 1);
    }

    /**
     * 创建系统告警Topic
     *
     * @return NewTopic实例
     */
    @Bean
    public NewTopic systemAlertsTopic() {
        return new NewTopic(SYSTEM_ALERTS_TOPIC, 3, (short) 1);
    }

    /**
     * 配置生产者工厂
     * 使用JSON序列化器
     *
     * @return ProducerFactory实例
     */
    @Bean
    public ProducerFactory<String, AgentMessage> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);

        log.info("Kafka生产者工厂配置完成");
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * 配置KafkaTemplate
     * 用于发送消息
     *
     * @return KafkaTemplate实例
     */
    @Bean
    public KafkaTemplate<String, AgentMessage> kafkaTemplate() {
        KafkaTemplate<String, AgentMessage> template = new KafkaTemplate<>(producerFactory());
        template.setDefaultTopic(AGENT_MESSAGES_TOPIC);
        log.info("KafkaTemplate配置完成");
        return template;
    }

    /**
     * 配置消费者工厂
     * 使用JSON反序列化器
     *
     * @return ConsumerFactory实例
     */
    @Bean
    public ConsumerFactory<String, AgentMessage> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, "game-maker-consumer");
        configProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
            org.apache.kafka.common.serialization.StringDeserializer.class);
        configProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            org.springframework.kafka.support.serializer.JsonDeserializer.class);
        configProps.put(org.springframework.kafka.support.serializer.JsonDeserializer.TRUSTED_PACKAGES, "com.chengxun.gamemaker.*");
        configProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        configProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);

        log.info("Kafka消费者工厂配置完成");
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    /**
     * 配置Kafka监听器容器工厂
     * 用于并发消费消息
     *
     * @return ConcurrentKafkaListenerContainerFactory实例
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AgentMessage> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, AgentMessage> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);  // 并发消费者数量
        factory.getContainerProperties().setPollTimeout(3000);

        log.info("Kafka监听器容器工厂配置完成");
        return factory;
    }
}
