---
name: 消息队列
description: 消息队列设计和使用
trigger: 消息队列,MQ,RabbitMQ,Kafka,异步处理
examples: 消息发送|异步处理|队列设计|消息消费
---

# 消息队列技能

## 使用场景

| 场景 | 说明 |
|------|------|
| 异步处理 | 耗时操作异步执行 |
| 系统解耦 | 降低系统间依赖 |
| 流量削峰 | 应对突发流量 |
| 日志收集 | 集中处理日志 |

## RabbitMQ使用

### 发送消息
```java
@Service
public class MessageProducer {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    public void sendOrderMessage(Order order) {
        rabbitTemplate.convertAndSend(
            "order.exchange",
            "order.create",
            order
        );
    }
}
```

### 接收消息
```java
@Component
public class MessageConsumer {
    
    @RabbitListener(queues = "order.queue")
    public void handleOrder(Order order) {
        // 处理订单
        log.info("收到订单: {}", order.getId());
    }
}
```

### 配置
```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

## 异步处理

```java
@Service
public class AsyncTaskService {
    
    @Async
    public CompletableFuture<String> asyncTask() {
        // 异步执行
        return CompletableFuture.completedFuture("完成");
    }
}
```

## 消息可靠性

### 发送确认
```java
rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
    if (!ack) {
        log.error("消息发送失败: {}", cause);
    }
});
```

### 消费确认
```java
@RabbitListener(queues = "order.queue")
public void handleOrder(Message message, Channel channel) throws IOException {
    try {
        // 处理消息
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    } catch (Exception e) {
        channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
    }
}
```

## 最佳实践

```
[消息队列检查]
□ 消息体大小合理
□ 设置过期时间
□ 处理消费失败
□ 避免消息堆积
□ 监控队列状态
```
