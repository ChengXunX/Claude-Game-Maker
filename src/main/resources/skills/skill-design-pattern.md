---
name: 设计模式应用
description: 常用设计模式的应用场景和实现
trigger: 设计模式,design pattern,单例,工厂,观察者,策略模式
examples: 设计模式|模式应用|代码重构|架构设计
---

# 设计模式应用技能

## 创建型模式

### 单例模式
```java
public class DatabaseManager {
    private static volatile DatabaseManager instance;
    
    private DatabaseManager() {}
    
    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }
}
```

### 工厂模式
```java
public class NotificationFactory {
    public static Notification create(String type) {
        return switch (type) {
            case "email" -> new EmailNotification();
            case "sms" -> new SmsNotification();
            case "push" -> new PushNotification();
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        };
    }
}
```

## 结构型模式

### 适配器模式
```java
// 旧接口
public interface OldLogger {
    void logMessage(String msg);
}

// 新接口
public interface NewLogger {
    void info(String msg);
    void error(String msg);
}

// 适配器
public class LoggerAdapter implements NewLogger {
    private OldLogger oldLogger;
    
    public LoggerAdapter(OldLogger oldLogger) {
        this.oldLogger = oldLogger;
    }
    
    @Override
    public void info(String msg) {
        oldLogger.logMessage("[INFO] " + msg);
    }
    
    @Override
    public void error(String msg) {
        oldLogger.logMessage("[ERROR] " + msg);
    }
}
```

## 行为型模式

### 策略模式
```java
public interface PaymentStrategy {
    void pay(double amount);
}

public class CreditCardPayment implements PaymentStrategy {
    @Override
    public void pay(double amount) {
        System.out.println("Paid " + amount + " with credit card");
    }
}

public class AlipayPayment implements PaymentStrategy {
    @Override
    public void pay(double amount) {
        System.out.println("Paid " + amount + " with Alipay");
    }
}

public class PaymentContext {
    private PaymentStrategy strategy;
    
    public void setStrategy(PaymentStrategy strategy) {
        this.strategy = strategy;
    }
    
    public void executePayment(double amount) {
        strategy.pay(amount);
    }
}
```

### 观察者模式
```java
public interface EventListener {
    void onEvent(String event);
}

public class EventBus {
    private Map<String, List<Listener>> listeners = new HashMap<>();
    
    public void subscribe(String event, Listener listener) {
        listeners.computeIfAbsent(event, k -> new ArrayList<>()).add(listener);
    }
    
    public void publish(String event) {
        listeners.getOrDefault(event, List.of())
            .forEach(l -> l.onEvent(event));
    }
}
```

## 使用场景

| 模式 | 场景 |
|------|------|
| 单例 | 数据库连接、配置管理 |
| 工厂 | 创建不同类型的对象 |
| 策略 | 算法切换、支付方式 |
| 观察者 | 事件处理、消息通知 |
| 适配器 | 接口兼容、旧系统集成 |
