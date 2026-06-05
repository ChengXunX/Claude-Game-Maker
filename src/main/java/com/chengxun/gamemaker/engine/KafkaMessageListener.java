package com.chengxun.gamemaker.engine;

import com.chengxun.gamemaker.config.KafkaConfig;
import com.chengxun.gamemaker.model.AgentMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka消息监听器
 * 监听系统级Topic，处理Agent事件和系统告警
 *
 * 主要功能：
 * - 监听Agent事件Topic
 * - 监听系统告警Topic
 * - 处理系统级消息
 *
 * @author chengxun
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(name = "app.messaging.type", havingValue = "kafka")
public class KafkaMessageListener {

    private static final Logger log = LoggerFactory.getLogger(KafkaMessageListener.class);

    /**
     * 监听Agent事件Topic
     * 处理Agent启动、停止、错误等事件
     *
     * @param message 事件消息
     */
    @KafkaListener(topics = KafkaConfig.AGENT_EVENTS_TOPIC, groupId = "game-maker-events-consumer")
    public void consumeAgentEvent(AgentMessage message) {
        try {
            log.info("收到Agent事件 - 类型: {}, 来源: {}, 内容: {}",
                message.getType(), message.getFromAgentId(), message.getContent());

            // 处理不同类型的Agent事件
            switch (message.getType()) {
                case REPORT:
                    handleAgentReport(message);
                    break;
                case SYSTEM:
                    handleAgentSystemEvent(message);
                    break;
                default:
                    log.debug("未处理的事件类型: {}", message.getType());
            }
        } catch (Exception e) {
            log.error("处理Agent事件异常: {}", e.getMessage());
        }
    }

    /**
     * 监听系统告警Topic
     * 处理系统级告警消息
     *
     * @param message 告警消息
     */
    @KafkaListener(topics = KafkaConfig.SYSTEM_ALERTS_TOPIC, groupId = "game-maker-alerts-consumer")
    public void consumeSystemAlert(AgentMessage message) {
        try {
            log.warn("收到系统告警 - 来源: {}, 内容: {}",
                message.getFromAgentId(), message.getContent());

            // 处理系统告警
            handleSystemAlert(message);
        } catch (Exception e) {
            log.error("处理系统告警异常: {}", e.getMessage());
        }
    }

    /**
     * 处理Agent报告
     *
     * @param message 报告消息
     */
    private void handleAgentReport(AgentMessage message) {
        // 记录Agent报告
        log.info("处理Agent报告 - Agent: {}, 报告: {}",
            message.getFromAgentId(), message.getContent());

        // 可以在这里添加报告持久化逻辑
    }

    /**
     * 处理Agent系统事件
     *
     * @param message 系统事件消息
     */
    private void handleAgentSystemEvent(AgentMessage message) {
        // 记录Agent系统事件
        log.info("处理Agent系统事件 - Agent: {}, 事件: {}",
            message.getFromAgentId(), message.getContent());

        // 可以在这里添加系统事件处理逻辑
    }

    /**
     * 处理系统告警
     *
     * @param message 告警消息
     */
    private void handleSystemAlert(AgentMessage message) {
        // 记录系统告警
        log.warn("处理系统告警 - 来源: {}, 告警: {}",
            message.getFromAgentId(), message.getContent());

        // 可以在这里添加告警通知逻辑
    }
}
