---
name: 缓存策略
description: 缓存设计和使用最佳实践
trigger: 缓存,cache,Redis,缓存策略,性能优化
examples: 缓存设计|Redis使用|缓存优化|缓存穿透
---

# 缓存策略技能

## 缓存层次

| 层次 | 技术 | 场景 |
|------|------|------|
| 浏览器缓存 | HTTP Cache | 静态资源 |
| CDN缓存 | CDN | 全球加速 |
| 应用缓存 | Redis/Memcached | 热点数据 |
| 数据库缓存 | Query Cache | 查询结果 |

## 缓存模式

### Cache-Aside
```java
public User getUser(Long id) {
    // 1. 先查缓存
    String key = "user:" + id;
    User user = redis.get(key);
    
    // 2. 缓存命中
    if (user != null) {
        return user;
    }
    
    // 3. 缓存未命中，查数据库
    user = userRepository.findById(id);
    
    // 4. 写入缓存
    if (user != null) {
        redis.set(key, user, 30, TimeUnit.MINUTES);
    }
    
    return user;
}
```

### Write-Through
```java
public void updateUser(User user) {
    // 1. 更新数据库
    userRepository.save(user);
    
    // 2. 更新缓存
    redis.set("user:" + user.getId(), user, 30, TimeUnit.MINUTES);
}
```

## 缓存问题

### 缓存穿透
```java
// 问题：查询不存在的数据
// 解决1：缓存空值
if (user == null) {
    redis.set(key, NULL_VALUE, 1, TimeUnit.MINUTES);
}

// 解决2：布隆过滤器
if (!bloomFilter.mightContain(id)) {
    return null;
}
```

### 缓存击穿
```java
// 问题：热点key过期
// 解决：互斥锁
public User getUser(Long id) {
    String key = "user:" + id;
    User user = redis.get(key);
    
    if (user == null) {
        String lockKey = "lock:" + key;
        if (redis.setnx(lockKey, "1", 10, TimeUnit.SECONDS)) {
            try {
                user = userRepository.findById(id);
                redis.set(key, user, 30, TimeUnit.MINUTES);
            } finally {
                redis.del(lockKey);
            }
        } else {
            Thread.sleep(50);
            return getUser(id);
        }
    }
    return user;
}
```

### 缓存雪崩
```java
// 问题：大量key同时过期
// 解决：随机过期时间
int randomTTL = 30 * 60 + new Random().nextInt(10 * 60);
redis.set(key, value, randomTTL, TimeUnit.SECONDS);
```

## Spring Cache

```java
@Cacheable(value = "users", key = "#id")
public User getUser(Long id) {
    return userRepository.findById(id);
}

@CachePut(value = "users", key = "#user.id")
public User updateUser(User user) {
    return userRepository.save(user);
}

@CacheEvict(value = "users", key = "#id")
public void deleteUser(Long id) {
    userRepository.deleteById(id);
}
```

## 检查清单

```
[缓存检查]
□ 缓存key设计合理
□ 过期时间设置合适
□ 缓存穿透已处理
□ 缓存击穿已处理
□ 缓存雪崩已处理
□ 缓存与数据库一致性
```
