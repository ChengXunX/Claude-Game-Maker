---
name: 放置挂机游戏开发模板
description: 放置挂机游戏开发模板，适用于点击器、放置类、挂机类游戏
trigger: 放置游戏,挂机游戏,idle game,clicker,点击游戏
examples: 点击英雄|无尽的饼干|旅行青蛙|最强蜗牛|想不想修真
---

# 放置挂机游戏开发模板

## 核心设计原则

### 1. 离线收益系统
- 核心循环：产出 → 积累 → 升级 → 更多产出
- 离线时间计算：记录最后在线时间，计算离线收益
- 离线收益上限：防止过度积累

### 2. 数值膨胀设计
```
收益 = 基础值 × 等级系数 × 加成倍率
等级系数 = 1.15^level (指数增长)
```

### 3. 多层循环系统
| 循环层 | 说明 | 重置奖励 |
|--------|------|----------|
| 第1层 | 主要资源 | 无 |
| 第2层 | 转生/重生 | 永久加成 |
| 第3层 | 超越/飞升 | 解锁新系统 |

## 关键系统实现

### 离线收益计算
```javascript
function calculateOfflineEarnings(lastOnline, now, baseRate) {
  const offlineSeconds = Math.floor((now - lastOnline) / 1000);
  const maxOfflineHours = 24;
  const cappedSeconds = Math.min(offlineSeconds, maxOfflineHours * 3600);
  return baseRate * cappedSeconds * 0.5; // 离线效率50%
}
```

### 数值格式化
```javascript
function formatNumber(num) {
  const suffixes = ['', 'K', 'M', 'B', 'T', 'Qa', 'Qi', 'Sx', 'Sp', 'Oc'];
  const tier = Math.floor(Math.log10(Math.abs(num)) / 3);
  if (tier === 0) return num.toString();
  const suffix = suffixes[tier] || `e${tier * 3}`;
  const scale = Math.pow(10, tier * 3);
  return (num / scale).toFixed(1) + suffix;
}
```

## 成瘾性设计

- 每日登录奖励
- 成就系统
- 限时活动
- 收集图鉴
- 排行榜竞争

## 变现设计

| 类型 | 说明 |
|------|------|
| 广告激励 | 看广告获得加速 |
| 月卡 | 每日领取奖励 |
| 礼包 | 限时折扣礼包 |
| VIP | 多级VIP特权 |
