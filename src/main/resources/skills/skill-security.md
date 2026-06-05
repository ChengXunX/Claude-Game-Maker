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
User findById(@Param("userId") Long userId);
```

### 2. XSS防护
```java
// 输出编码
String safe = HtmlUtils.htmlEscape(userInput);

// Content Security Policy
response.setHeader("Content-Security-Policy", "default-src 'self'");
```

### 3. CSRF防护
```java
// Spring Security自动防护
@EnableWebSecurity
public class SecurityConfig {
    // CSRF默认启用
}
```

## 密码安全

### 加密存储
```java
// BCrypt加密
PasswordEncoder encoder = new BCryptPasswordEncoder();
String hash = encoder.encode(password);
boolean matches = encoder.matches(rawPassword, hash);
```

### 密码策略
- 最少8位
- 包含大小写字母
- 包含数字
- 包含特殊字符

## 认证授权

### JWT安全
```java
// 生成Token
String token = Jwts.builder()
    .setSubject(username)
    .setExpiration(new Date(System.currentTimeMillis() + 3600000))
    .signWith(SignatureAlgorithm.HS256, secret)
    .compact();

// 验证Token
Claims claims = Jwts.parser()
    .setSigningKey(secret)
    .parseClaimsJws(token)
    .getBody();
```

## 输入验证

```java
// 参数验证
public class UserRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度3-50")
    private String username;
    
    @Email(message = "邮箱格式不正确")
    private String email;
}
```

## 安全检查清单

```
[安全检查]
□ 输入验证完整
□ SQL参数化查询
□ XSS输出编码
□ CSRF防护启用
□ 密码加密存储
□ 敏感数据脱敏
□ 日志不含敏感信息
□ HTTPS启用
□ 权限控制正确
```
