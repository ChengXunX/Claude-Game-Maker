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

### 1. 连接配置
```java
@Configuration
public class RabbitConfig {
    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("guest");
        return factory;
    }
}
```

### 2. 消息发送
```java
@Service
public class MessageSender {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    public void send(String exchange, String routingKey, Object message) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }
}
```

### 3. 消息接收
```java
@Component
public class MessageReceiver {
    @RabbitListener(queues = "game.queue")
    public void receive(Message message) {
        System.out.println("Received: " + message);
    }
}
```

## 游戏消息队列

### 1. 游戏事件队列
```java
// 游戏事件
public class GameEvent {
    private String eventType;
    private String playerId;
    private Map<String, Object> data;
    private LocalDateTime timestamp;
}

// 事件发送
@Service
public class GameEventSender {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    public void sendGameEvent(GameEvent event) {
        rabbitTemplate.convertAndSend(
            "game.exchange",
            "game.event." + event.getEventType(),
            event
        );
    }
}

// 事件接收
@Component
public class GameEventReceiver {
    @RabbitListener(queues = "game.event.queue")
    public void receiveGameEvent(GameEvent event) {
        // 处理游戏事件
    }
}
```

### 2. 异步任务队列
```java
// 异步任务
public class AsyncTask {
    private String taskType;
    private String taskId;
    private Map<String, Object> params;
}

// 任务发送
@Service
public class AsyncTaskSender {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    public void sendTask(AsyncTask task) {
        rabbitTemplate.convertAndSend(
            "task.exchange",
            "task." + task.getTaskType(),
            task
        );
    }
}

// 任务处理
@Component
public class AsyncTaskReceiver {
    @RabbitListener(queues = "task.queue")
    public void processTask(AsyncTask task) {
        // 处理异步任务
    }
}
```

### 3. 通知队列
```java
// 通知消息
public class Notification {
    private String userId;
    private String type;
    private String content;
    private LocalDateTime sendTime;
}

// 通知发送
@Service
public class NotificationSender {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    public void sendNotification(Notification notification) {
        rabbitTemplate.convertAndSend(
            "notification.exchange",
            "notification." + notification.getType(),
            notification
        );
    }
}

// 通知处理
@Component
public class NotificationReceiver {
    @RabbitListener(queues = "notification.queue")
    public void processNotification(Notification notification) {
        // 发送通知
    }
}
```

## Kafka使用

### 1. 配置
```java
@Configuration
public class KafkaConfig {
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }
    
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

### 2. 消息发送
```java
@Service
public class KafkaSender {
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    public void send(String topic, String message) {
        kafkaTemplate.send(topic, message);
    }
}
```

### 3. 消息接收
```java
@Component
public class KafkaReceiver {
    @KafkaListener(topics = "game-topic", groupId = "game-group")
    public void receive(String message) {
        System.out.println("Received: " + message);
    }
}
```

## 最佳实践

### 1. 消息可靠性
```
- 消息持久化
- 消费确认
- 重试机制
- 死信队列
```

### 2. 性能优化
```
- 批量发送
- 异步发送
- 消息压缩
- 分区策略
```

### 3. 监控告警
```
- 队列长度监控
- 消费延迟监控
- 错误率监控
- 告警通知
```

## 常见错误

1. **消息丢失**：要保证消息可靠性
2. **消息重复**：要处理幂等性
3. **消息顺序**：要注意消息顺序
4. **队列积压**：要监控队列长度
5. **连接问题**：要处理连接异常
