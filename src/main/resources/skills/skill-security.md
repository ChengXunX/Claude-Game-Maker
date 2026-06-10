---
name: 安全编码
description: 安全编码实践，防止常见安全漏洞
trigger: 安全编码,安全漏洞,XSS,SQL注入,CSRF,安全防护
examples: 安全检查|漏洞修复|安全编码|防护措施
---

# 安全编码技能

## OWASP Top 10

### 1. 注入防护
```java
// SQL注入防护 - 使用参数化查询
String sql = "SELECT * FROM users WHERE id = ?";
PreparedStatement ps = conn.prepareStatement(sql);
ps.setLong(1, userId);

// MyBatis防护
@Select("SELECT * FROM users WHERE id = #{userId}")
User findById(Long userId);
```

### 2. XSS防护
```javascript
// 输出编码
function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

// 使用安全的API
element.textContent = userInput; // 安全
element.innerHTML = escapeHtml(userInput); // 需要编码
```

### 3. CSRF防护
```java
// Spring Security CSRF
@Override
protected void configure(HttpSecurity http) throws Exception {
    http.csrf()
        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
}
```

## 游戏安全

### 1. 输入验证
```javascript
// 验证玩家输入
function validatePlayerInput(input) {
  // 检查类型
  if (typeof input !== 'object') return false;
  
  // 检查必填字段
  if (!input.action || !input.target) return false;
  
  // 检查值范围
  if (input.x < 0 || input.x > mapWidth) return false;
  if (input.y < 0 || input.y > mapHeight) return false;
  
  return true;
}
```

### 2. 服务端验证
```java
// 关键逻辑服务端验证
public ActionResult processAction(PlayerAction action) {
  // 验证玩家状态
  if (!player.isAlive()) {
    return ActionResult.fail("玩家已死亡");
  }
  
  // 验证操作合法性
  if (!isValidAction(action)) {
    return ActionResult.fail("非法操作");
  }
  
  // 验证冷却时间
  if (!isCooldownReady(action)) {
    return ActionResult.fail("技能冷却中");
  }
  
  // 执行操作
  return executeAction(action);
}
```

### 3. 数据加密
```java
// 敏感数据加密
public class DataEncryption {
  private static final String SECRET_KEY = "your-secret-key";
  
  public static String encrypt(String data) {
    // 使用AES加密
    SecretKeySpec key = new SecretKeySpec(
      SECRET_KEY.getBytes(), "AES");
    Cipher cipher = Cipher.getInstance("AES");
    cipher.init(Cipher.ENCRYPT_MODE, key);
    byte[] encrypted = cipher.doFinal(data.getBytes());
    return Base64.getEncoder().encodeToString(encrypted);
  }
  
  public static String decrypt(String encryptedData) {
    // 解密逻辑
  }
}
```

### 4. 防作弊
```javascript
// 客户端防作弊
class AntiCheat {
  constructor() {
    this.checks = [];
  }
  
  addCheck(check) {
    this.checks.push(check);
  }
  
  validate() {
    for (const check of this.checks) {
      if (!check()) {
        this.reportCheat();
        return false;
      }
    }
    return true;
  }
  
  reportCheat() {
    // 上报作弊行为
    fetch('/api/cheat/report', {
      method: 'POST',
      body: JSON.stringify({
        playerId: this.playerId,
        type: 'speed_hack',
        timestamp: Date.now()
      })
    });
  }
}

// 速度检测
const speedCheck = () => {
  const now = Date.now();
  const elapsed = now - this.lastCheck;
  const distance = this.calculateDistance();
  const speed = distance / elapsed;
  
  return speed <= this.maxSpeed;
};
```

### 5. 速率限制
```java
// API速率限制
@RestController
public class GameController {
  
  @PostMapping("/api/action")
  @RateLimiter(name = "gameAction", fallbackMethod = "fallback")
  public ResponseEntity<ActionResult> processAction(
      @RequestBody PlayerAction action) {
    return ResponseEntity.ok(gameService.processAction(action));
  }
  
  public ResponseEntity<ActionResult> fallback(PlayerAction action, Exception e) {
    return ResponseEntity.status(429)
      .body(ActionResult.fail("操作过于频繁"));
  }
}
```

### 6. 日志安全
```java
// 安全日志记录
public class SecureLogger {
  private static final Logger log = LoggerFactory.getLogger(SecureLogger.class);
  
  public static void logAction(String playerId, String action, Map<String, Object> details) {
    // 脱敏敏感信息
    Map<String, Object> safeDetails = sanitizeDetails(details);
    
    log.info("Player action: playerId={}, action={}, details={}", 
      playerId, action, safeDetails);
  }
  
  private static Map<String, Object> sanitizeDetails(Map<String, Object> details) {
    Map<String, Object> sanitized = new HashMap<>();
    for (Map.Entry<String, Object> entry : details.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      
      // 脱敏密码、token等
      if (key.contains("password") || key.contains("token")) {
        sanitized.put(key, "***");
      } else {
        sanitized.put(key, value);
      }
    }
    return sanitized;
  }
}
```

## 安全检查清单

### 1. 输入验证
```
□ 验证所有用户输入
□ 使用白名单验证
□ 限制输入长度
□ 过滤特殊字符
```

### 2. 输出编码
```
□ HTML输出编码
□ JavaScript输出编码
□ URL输出编码
□ SQL参数化查询
```

### 3. 认证授权
```
□ 使用安全的认证方式
□ 实施最小权限原则
□ 保护敏感操作
□ 记录安全日志
```

### 4. 数据保护
```
□ 加密敏感数据
□ 保护密钥安全
□ 安全存储凭证
□ 定期更换密钥
```

## 常见错误

1. **不验证输入**：要验证所有用户输入
2. **不编码输出**：要编码所有输出
3. **硬编码密钥**：要安全存储密钥
4. **不记录日志**：要记录安全日志
5. **不更新依赖**：要定期更新依赖
