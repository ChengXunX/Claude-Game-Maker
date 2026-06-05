---
name: 异常处理
description: 异常处理最佳实践和错误处理策略
trigger: 异常处理,错误处理,exception,try catch,容错
examples: 异常捕获|错误处理|容错设计|降级策略
---

# 异常处理技能

## 异常分类

### Java异常体系
```
Throwable
├── Error (系统错误，不应捕获)
│   ├── OutOfMemoryError
│   └── StackOverflowError
└── Exception
    ├── RuntimeException (非受检异常)
    │   ├── NullPointerException
    │   ├── IllegalArgumentException
    │   └── IndexOutOfBoundsException
    └── 受检异常
        ├── IOException
        └── SQLException
```

## 处理原则

### 1. 不要捕获Exception
```java
// 差
try {
    // ...
} catch (Exception e) {
    // 太宽泛
}

// 好
try {
    // ...
} catch (IOException e) {
    // 处理IO异常
} catch (SQLException e) {
    // 处理SQL异常
}
```

### 2. 不要忽略异常
```java
// 差
try {
    // ...
} catch (Exception e) {
    // 什么都不做
}

// 好
try {
    // ...
} catch (Exception e) {
    log.error("操作失败", e);
    throw new ServiceException("操作失败", e);
}
```

### 3. 使用自定义异常
```java
public class BusinessException extends RuntimeException {
    private final String code;
    
    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }
    
    public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
```

## 全局异常处理

### Spring Boot
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusiness(BusinessException e) {
        return ResponseEntity.badRequest()
            .body(Map.of("success", false, "code", e.getCode(), "message", e.getMessage()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception e) {
        log.error("系统异常", e);
        return ResponseEntity.internalServerError()
            .body(Map.of("success", false, "message", "系统内部错误"));
    }
}
```

## 重试机制

```java
@Retryable(value = {RemoteException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
public String callRemote() {
    // 远程调用
}
```

## 降级策略

```java
@HystrixCommand(fallbackMethod = "fallback")
public String getData() {
    return remoteService.getData();
}

public String fallback() {
    return "默认数据";
}
```

## 检查清单

```
[异常处理检查]
□ 异常类型具体明确
□ 异常信息清晰有用
□ 异常被正确记录
□ 资源被正确释放
□ 有降级/兜底方案
□ 不吞掉异常
```
