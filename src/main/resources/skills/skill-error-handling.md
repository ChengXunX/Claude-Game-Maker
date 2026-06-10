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
    │   └── ArrayIndexOutOfBoundsException
    └── 受检异常
        ├── IOException
        └── SQLException
```

### JavaScript异常体系
```
Error
├── TypeError
├── ReferenceError
├── SyntaxError
├── RangeError
└── URIError
```

## 异常处理原则

### 1. 早失败，快失败
```java
// 好的做法：尽早检查参数
public void process(int value) {
  if (value < 0) {
    throw new IllegalArgumentException("Value must be positive");
  }
  // 处理逻辑
}

// 不好的做法：延迟检查
public void process(int value) {
  // 很多逻辑...
  if (value < 0) {
    // 已经做了很多工作
  }
}
```

### 2. 不要忽略异常
```java
// 不好的做法
try {
  // 逻辑
} catch (Exception e) {
  // 忽略异常
}

// 好的做法
try {
  // 逻辑
} catch (Exception e) {
  log.error("操作失败", e);
  throw new BusinessException("操作失败", e);
}
```

### 3. 使用具体的异常类型
```java
// 不好的做法
try {
  // 逻辑
} catch (Exception e) {
  // 太宽泛
}

// 好的做法
try {
  // 逻辑
} catch (IOException e) {
  // 处理IO异常
} catch (SQLException e) {
  // 处理SQL异常
}
```

## 游戏异常处理

### 1. 游戏错误边界
```javascript
class GameErrorBoundary {
  constructor() {
    this.errors = [];
  }
  
  wrap(fn) {
    return (...args) => {
      try {
        return fn(...args);
      } catch (error) {
        this.handleError(error);
        return null;
      }
    };
  }
  
  handleError(error) {
    this.errors.push({
      message: error.message,
      stack: error.stack,
      timestamp: Date.now()
    });
    
    // 显示友好的错误提示
    this.showErrorMessage(error);
    
    // 上报错误
    this.reportError(error);
  }
  
  showErrorMessage(error) {
    // 不要显示技术性错误信息
    const friendlyMessage = this.getFriendlyMessage(error);
    showToast(friendlyMessage);
  }
  
  getFriendlyMessage(error) {
    const messages = {
      'NetworkError': '网络连接失败，请检查网络',
      'TimeoutError': '请求超时，请稍后重试',
      'ValidationError': '输入数据有误，请检查'
    };
    return messages[error.name] || '发生了一个错误';
  }
}
```

### 2. 资源加载错误处理
```javascript
class ResourceManager {
  constructor() {
    this.loaded = new Map();
    this.failed = new Set();
  }
  
  async load(key, url) {
    try {
      const response = await fetch(url);
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      const data = await response.blob();
      this.loaded.set(key, data);
      return data;
    } catch (error) {
      this.failed.add(key);
      console.error(`Failed to load ${key}:`, error);
      return this.getFallback(key);
    }
  }
  
  getFallback(key) {
    // 返回默认资源
    return this.loaded.get('default');
  }
}
```

### 3. 网络错误处理
```javascript
class NetworkManager {
  constructor() {
    this.retryCount = 3;
    this.retryDelay = 1000;
  }
  
  async request(url, options = {}) {
    let lastError;
    
    for (let i = 0; i < this.retryCount; i++) {
      try {
        const response = await fetch(url, options);
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`);
        }
        return await response.json();
      } catch (error) {
        lastError = error;
        
        if (i < this.retryCount - 1) {
          await this.delay(this.retryDelay * (i + 1));
        }
      }
    }
    
    throw lastError;
  }
  
  delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }
}
```

### 4. 游戏状态恢复
```javascript
class GameStateManager {
  constructor() {
    this.state = {};
    this.checkpoints = [];
  }
  
  saveCheckpoint() {
    this.checkpoints.push(JSON.parse(JSON.stringify(this.state)));
  }
  
  restoreCheckpoint() {
    if (this.checkpoints.length > 0) {
      this.state = this.checkpoints.pop();
      return true;
    }
    return false;
  }
  
  safeExecute(fn) {
    try {
      return fn();
    } catch (error) {
      console.error('Game error:', error);
      
      // 尝试恢复到上一个检查点
      if (this.restoreCheckpoint()) {
        showToast('发生错误，已恢复到上一个存档点');
      } else {
        showToast('发生严重错误，需要重新开始');
      }
      
      return null;
    }
  }
}
```

## 异常处理最佳实践

### 1. 日志记录
```java
// 记录详细的错误信息
try {
  // 逻辑
} catch (Exception e) {
  log.error("操作失败: userId={}, action={}", userId, action, e);
  throw new BusinessException("操作失败", e);
}
```

### 2. 错误上报
```javascript
// 上报错误到监控系统
function reportError(error, context) {
  const errorReport = {
    message: error.message,
    stack: error.stack,
    context: context,
    timestamp: Date.now(),
    userAgent: navigator.userAgent
  };
  
  fetch('/api/errors', {
    method: 'POST',
    body: JSON.stringify(errorReport)
  });
}
```

### 3. 用户友好提示
```javascript
// 显示用户友好的错误信息
function showError(error) {
  const messages = {
    'INVALID_CREDENTIALS': '用户名或密码错误',
    'ACCOUNT_LOCKED': '账户已被锁定，请联系管理员',
    'NETWORK_ERROR': '网络连接失败，请检查网络',
    'SERVER_ERROR': '服务器错误，请稍后重试'
  };
  
  const message = messages[error.code] || '发生了一个错误';
  showToast(message);
}
```

## 常见错误

1. **忽略异常**：不要忽略异常
2. **异常太宽泛**：要使用具体的异常类型
3. **不记录日志**：要记录详细的错误日志
4. **不恢复状态**：要实现状态恢复机制
5. **显示技术错误**：要显示用户友好的错误信息
