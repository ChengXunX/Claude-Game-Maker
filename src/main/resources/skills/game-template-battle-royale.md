---
name: 大逃杀游戏开发模板
description: 大逃杀游戏开发模板，适用于吃鸡类、生存竞技类游戏
trigger: 大逃杀,吃鸡,battle royale,生存竞技,百人对战
examples: PUBG|Fortnite|Apex Legends|和平精英|荒野行动
---

# 大逃杀游戏开发模板

## 游戏设计核心原则

### 核心循环（每局 15-30 分钟）
```
跳伞降落 → 搜集装备 → 遭遇战斗 → 毒圈缩小 → 决战 → 胜利/失败
```
- **紧张感**：随时可能被敌人击杀
- **随机性**：每局都不一样
- **策略性**：选择跳伞点、移动路线、战斗时机

### 玩家心理学
- **"吃鸡"的成就感**：百人中胜出的满足感
- **"落地成盒"的挫败感**：早期死亡的挫败感（但会立刻重玩）
- **"搜集装备"的乐趣**：找到好装备的惊喜
- **"决赛圈"的紧张感**：最后几人的紧张对决

### 毒圈设计要点
```
毒圈机制：
1. 每 2-3 分钟缩小一次
2. 缩小前有 30 秒预警
3. 毒圈伤害逐渐增加
4. 最终圈很小，强制战斗
```

## 核心系统设计

### 1. 匹配系统
```javascript
class MatchmakingSystem {
  constructor() {
    this.queue = [];
    this.matchSize = 100;
  }
  
  addToQueue(player) {
    this.queue.push(player);
    
    if (this.queue.length >= this.matchSize) {
      this.createMatch();
    }
  }
  
  createMatch() {
    const players = this.queue.splice(0, this.matchSize);
    
    const match = {
      id: this.generateId(),
      players: players,
      state: 'waiting',
      map: this.selectMap(),
      startTime: Date.now()
    };
    
    return match;
  }
}
```

### 2. 跳伞系统
```javascript
class ParachuteSystem {
  constructor(player) {
    this.player = player;
    this.isFalling = false;
    this.speed = 0;
    this.direction = { x: 0, y: 0 };
  }
  
  jump(planePosition) {
    this.player.position = { ...planePosition };
    this.isFalling = true;
    this.speed = 50;
  }
  
  update(delta) {
    if (!this.isFalling) return;
    
    // 水平移动
    this.player.position.x += this.direction.x * this.speed * delta;
    this.player.position.y += this.direction.y * this.speed * delta;
    
    // 垂直下落
    this.player.position.z -= this.speed * delta;
    
    // 开伞
    if (this.player.position.z <= 100) {
      this.openParachute();
    }
    
    // 着陆
    if (this.player.position.z <= 0) {
      this.land();
    }
  }
  
  openParachute() {
    this.speed = 20;
    this.player.hasParachute = true;
  }
  
  land() {
    this.isFalling = false;
    this.player.position.z = 0;
    this.player.state = 'playing';
  }
}
```

### 3. 搜集系统
```javascript
class LootSystem {
  constructor() {
    this.items = [];
  }
  
  spawnLoot(map) {
    // 在地图上随机生成物品
    for (const spawnPoint of map.lootSpawns) {
      const item = this.generateRandomItem(spawnPoint.tier);
      this.items.push({
        ...item,
        position: spawnPoint.position
      });
    }
  }
  
  generateRandomItem(tier) {
    const items = LOOT_TABLES[tier];
    const weights = items.map(i => i.weight);
    const totalWeight = weights.reduce((a, b) => a + b, 0);
    let random = Math.random() * totalWeight;
    
    for (let i = 0; i < items.length; i++) {
      random -= items[i].weight;
      if (random <= 0) {
        return items[i].item;
      }
    }
    
    return items[0].item;
  }
  
  pickup(player, itemId) {
    const item = this.items.find(i => i.id === itemId);
    if (!item) return false;
    
    const dist = this.getDistance(player.position, item.position);
    if (dist > 2) return false;
    
    player.inventory.add(item);
    this.items = this.items.filter(i => i.id !== itemId);
    
    return true;
  }
}

const LOOT_TABLES = {
  high: [
    { item: { type: 'weapon', name: 'AWM', damage: 120 }, weight: 5 },
    { item: { type: 'armor', name: '三级甲', defense: 50 }, weight: 10 },
    { item: { type: 'helmet', name: '三级头', defense: 50 }, weight: 10 }
  ],
  medium: [
    { item: { type: 'weapon', name: 'M416', damage: 40 }, weight: 20 },
    { item: { type: 'armor', name: '二级甲', defense: 30 }, weight: 20 },
    { item: { type: 'heal', name: '急救包', heal: 75 }, weight: 30 }
  ],
  low: [
    { item: { type: 'weapon', name: '手枪', damage: 20 }, weight: 40 },
    { item: { type: 'heal', name: '绷带', heal: 10 }, weight: 60 }
  ]
};
```

### 4. 毒圈系统
```javascript
class SafeZoneSystem {
  constructor(mapSize) {
    this.mapSize = mapSize;
    this.currentZone = { x: mapSize/2, y: mapSize/2, radius: mapSize/2 };
    this.nextZone = null;
    this.phase = 0;
    this.phases = [
      { delay: 120, shrinkTime: 60, radiusMultiplier: 0.7, damage: 1 },
      { delay: 90, shrinkTime: 45, radiusMultiplier: 0.6, damage: 2 },
      { delay: 60, shrinkTime: 30, radiusMultiplier: 0.5, damage: 5 },
      { delay: 30, shrinkTime: 20, radiusMultiplier: 0.3, damage: 10 },
      { delay: 10, shrinkTime: 10, radiusMultiplier: 0, damage: 20 }
    ];
  }
  
  update(delta) {
    if (this.nextZone) {
      this.shrinkZone(delta);
    } else {
      this.checkPhaseTimer(delta);
    }
  }
  
  checkPhaseTimer(delta) {
    const phase = this.phases[this.phase];
    if (!phase) return;
    
    phase.timer -= delta;
    
    if (phase.timer <= 0) {
      this.startNextPhase();
    }
  }
  
  startNextPhase() {
    const phase = this.phases[this.phase];
    
    // 生成下一个安全区
    this.nextZone = {
      x: this.currentZone.x + (Math.random() - 0.5) * this.currentZone.radius * 0.5,
      y: this.currentZone.y + (Math.random() - 0.5) * this.currentZone.radius * 0.5,
      radius: this.currentZone.radius * phase.radiusMultiplier
    };
    
    this.phase++;
  }
  
  isInSafeZone(x, y) {
    const dist = Math.sqrt((x - this.currentZone.x) ** 2 + (y - this.currentZone.y) ** 2);
    return dist <= this.currentZone.radius;
  }
  
  getDamage() {
    return this.phases[this.phase - 1]?.damage || 0;
  }
}
```

### 5. 战斗系统
```javascript
class CombatSystem {
  constructor() {
    this.projectiles = [];
  }
  
  shoot(player, weapon, target) {
    // 检查弹药
    if (weapon.ammo <= 0) return false;
    
    weapon.ammo--;
    
    // 计算命中
    const hitChance = this.calculateHitChance(player, weapon, target);
    const hit = Math.random() < hitChance;
    
    if (hit) {
      const damage = this.calculateDamage(weapon, target);
      target.hp -= damage;
      
      if (target.hp <= 0) {
        this.handleKill(player, target);
      }
    }
    
    return hit;
  }
  
  calculateHitChance(player, weapon, target) {
    const distance = this.getDistance(player.position, target.position);
    const baseAccuracy = weapon.accuracy;
    const distancePenalty = distance / weapon.range;
    
    return Math.max(0.1, baseAccuracy - distancePenalty);
  }
  
  calculateDamage(weapon, target) {
    const baseDamage = weapon.damage;
    const armorReduction = target.armor ? target.armor.defense : 0;
    
    return Math.max(1, baseDamage - armorReduction);
  }
  
  handleKill(killer, victim) {
    killer.kills++;
    killer.items.push(...victim.items);
    
    // 显示击杀提示
    this.showKillFeed(killer, victim);
  }
}
```

## 迭代策略

### 第一版：基础对战
- 跳伞系统
- 搜集系统
- 基础战斗
- 毒圈系统

### 第二版：武器系统
- 多种武器
- 武器配件
- 弹药系统
- 投掷物

### 第三版：载具系统
- 多种载具
- 载具战斗
- 载具损坏
- 燃料系统

### 第四版：社交功能
- 组队系统
- 语音聊天
- 观战系统
- 回放系统

### 第五段：竞技化
- 排位系统
- 赛季系统
- 电竞模式
- 排行榜

## 常见错误

1. **节奏太慢**：前期搜集太久会无聊
2. **毒圈太痛**：毒圈伤害要适度
3. **武器不平衡**：某些武器太强或太弱
4. **外挂问题**：要有反作弊系统
5. **服务器延迟**：网络延迟会毁掉体验
