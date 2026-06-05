---
name: 大逃杀游戏开发模板
description: 大逃杀游戏开发模板，适用于吃鸡类、生存竞技类游戏
trigger: 大逃杀,吃鸡,battle royale,生存竞技,百人对战
examples: PUBG|Fortnite|Apex Legends|和平精英|荒野行动
---

# 大逃杀游戏开发模板

## 核心系统设计

### 1. 匹配系统
```
Match {
  id: 对局ID
  players: 100名玩家
  mode: 单人/双人/四人
  map: 地图ID
  state: 等待/进行中/结束
}
```

### 2. 缩圈系统
| 阶段 | 等待时间 | 缩圈时间 | 伤害 |
|------|----------|----------|------|
| 1 | 120s | 60s | 1/s |
| 2 | 90s | 45s | 2/s |
| 3 | 60s | 30s | 5/s |
| 4 | 30s | 20s | 10/s |

### 3. 物资系统
```javascript
const lootTable = {
  weapons: { rarity: [40, 30, 20, 8, 2] },  // 普通到传说
  armor: { level1: 40, level2: 30, level3: 15 },
  healing: { bandage: 50, medkit: 20, revive: 10 }
}
```

## 关键技术实现

### 物资生成
```javascript
function spawnLoot(container, lootTable) {
  const items = [];
  const itemCount = randomRange(2, 5);
  
  for (let i = 0; i < itemCount; i++) {
    const category = weightedRandom(lootTable);
    const item = generateItem(category);
    items.push(item);
  }
  
  return items;
}
```

### 击杀播报
```javascript
function broadcastKill(killer, victim, weapon) {
  const message = {
    type: 'kill',
    killer: killer.name,
    victim: victim.name,
    weapon: weapon.name,
    remaining: alivePlayers.length
  };
  
  broadcastToAll(message);
}
```

## 网络优化

- 状态同步优化
- 区域裁剪
- 延迟补偿
- 反作弊系统

## 游戏模式

| 模式 | 人数 | 说明 |
|------|------|------|
| 经典模式 | 100 | 标准大逃杀 |
| 快速模式 | 50 | 缩短时间 |
| 团队模式 | 4人组队 | 合作竞技 |
| 僵尸模式 | PVE+PVP | 加入AI敌人 |
