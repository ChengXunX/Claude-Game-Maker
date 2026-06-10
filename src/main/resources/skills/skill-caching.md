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

### 1. Cache-Aside模式
```
读取流程：
1. 先查缓存
2. 缓存命中，直接返回
3. 缓存未命中，查数据库
4. 将数据写入缓存
5. 返回数据

写入流程：
1. 更新数据库
2. 删除缓存
```

### 2. Write-Through模式
```
写入流程：
1. 写入缓存
2. 缓存同步写入数据库
3. 返回成功
```

### 3. Write-Behind模式
```
写入流程：
1. 写入缓存
2. 返回成功
3. 缓存异步写入数据库
```

## 缓存问题

### 1. 缓存穿透
```
问题：查询不存在的数据，每次都查数据库
解决方案：
- 布隆过滤器
- 缓存空值
- 参数校验
```

### 2. 缓存击穿
```
问题：热点数据过期，大量请求打到数据库
解决方案：
- 互斥锁
- 永不过期
- 后台更新
```

### 3. 缓存雪崩
```
问题：大量缓存同时过期，数据库压力骤增
解决方案：
- 随机过期时间
- 多级缓存
- 限流降级
```

## 游戏缓存设计

### 1. 玩家数据缓存
```java
// 玩家基础信息缓存
@Cacheable(value = "player", key = "#playerId")
public Player getPlayer(Long playerId) {
    return playerRepository.findById(playerId).orElse(null);
}

// 玩家背包缓存
@Cacheable(value = "inventory", key = "#playerId")
public Inventory getInventory(Long playerId) {
    return inventoryRepository.findByPlayerId(playerId);
}
```

### 2. 游戏配置缓存
```java
// 游戏配置缓存
@Cacheable(value = "gameConfig", key = "#configKey")
public GameConfig getConfig(String configKey) {
    return configRepository.findByKey(configKey);
}
```

### 3. 排行榜缓存
```java
// 排行榜缓存
@Cacheable(value = "leaderboard", key = "#type")
public List<LeaderboardEntry> getLeaderboard(String type) {
    return leaderboardRepository.findByType(type);
}
```

## Redis使用

### 1. 数据结构
```
- String：简单键值
- Hash：对象属性
- List：列表数据
- Set：集合数据
- Sorted Set：有序集合
```

### 2. 过期策略
```
- TTL：设置过期时间
- LRU：最近最少使用
- LFU：最不经常使用
- Random：随机淘汰
```

### 3. 持久化
```
- RDB：定期快照
- AOF：追加写入
- 混合持久化：RDB + AOF
```

## 缓存优化

### 1. 缓存预热
```java
// 系统启动时预热缓存
@PostConstruct
public void warmUpCache() {
    // 加载热点数据
    List<Player> hotPlayers = playerRepository.findHotPlayers();
    for (Player player : hotPlayers) {
        cacheManager.getCache("player").put(player.getId(), player);
    }
}
```

### 2. 缓存更新
```java
// 更新缓存
@CachePut(value = "player", key = "#player.id")
public Player updatePlayer(Player player) {
    return playerRepository.save(player);
}

// 删除缓存
@CacheEvict(value = "player", key = "#playerId")
public void deletePlayer(Long playerId) {
    playerRepository.deleteById(playerId);
}
```

### 3. 缓存监控
```java
// 缓存命中率监控
public class CacheMonitor {
    private long hits = 0;
    private long misses = 0;
    
    public void recordHit() {
        hits++;
    }
    
    public void recordMiss() {
        misses++;
    }
    
    public double getHitRate() {
        return (double) hits / (hits + misses);
    }
}
```

## 常见错误

1. **缓存一致性**：要保证缓存和数据库一致
2. **缓存过期**：要设置合理的过期时间
3. **缓存穿透**：要处理缓存穿透问题
4. **缓存雪崩**：要防止缓存雪崩
5. **缓存监控**：要监控缓存使用情况
