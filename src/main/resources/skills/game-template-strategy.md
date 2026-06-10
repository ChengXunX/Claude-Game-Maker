---
name: 策略游戏开发模板
description: 策略游戏开发模板，适用于回合制策略、战棋、战争类游戏
trigger: strategy, RTS, 策略, 战争, army, 军队, empire, 帝国, 战棋
examples: 文明|火焰纹章|三国志|英雄无敌|Advance Wars
---

# 策略游戏开发模板

## 游戏设计核心原则

### 核心循环（每回合 1-5 分钟）
```
收集资源 → 建造/训练 → 移动单位 → 战斗 → 结算 → 下一回合
```
- **策略深度**：每个决策都有影响
- **长期规划**：要考虑多个回合后的结果
- **资源管理**：有限资源下的最优分配

### 玩家心理学
- **"运筹帷幄"的掌控感**：指挥军队的满足感
- **"以少胜多"的成就感**：用策略战胜强敌
- **"发展帝国"的成长感**：从一个小村庄到大帝国
- **"探索世界"的好奇心**：发现新地图、新资源

### 策略设计要点
```
策略游戏核心：
1. 信息透明：玩家能看到所有信息
2. 决策有意义：每个选择都有后果
3. 多种胜利条件：军事、经济、科技、文化
4. 随机性可控：地图随机，但公平
```

## 核心系统设计

### 1. 六边形地图系统
```javascript
class HexGrid {
  constructor(width, height) {
    this.width = width;
    this.height = height;
    this.tiles = [];
    
    for (let y = 0; y < height; y++) {
      for (let x = 0; x < width; x++) {
        this.tiles.push({
          x, y,
          type: 'grass',
          resource: null,
          unit: null,
          building: null
        });
      }
    }
  }
  
  getNeighbors(x, y) {
    const neighbors = [];
    const offsets = [
      [1, 0], [-1, 0], [0, 1], [0, -1], [1, -1], [-1, 1]
    ];
    
    for (const [dx, dy] of offsets) {
      const nx = x + dx;
      const ny = y + dy;
      if (this.isValid(nx, ny)) {
        neighbors.push(this.getTile(nx, ny));
      }
    }
    
    return neighbors;
  }
  
  getDistance(x1, y1, x2, y2) {
    // 六边形距离公式
    const dx = Math.abs(x1 - x2);
    const dy = Math.abs(y1 - y2);
    return Math.max(dx, dy, dx + dy);
  }
  
  findPath(startX, startY, endX, endY) {
    // A* 寻路算法
    const openSet = [{ x: startX, y: startY, g: 0, h: 0, f: 0, parent: null }];
    const closedSet = new Set();
    
    while (openSet.length > 0) {
      const current = openSet.reduce((a, b) => a.f < b.f ? a : b);
      
      if (current.x === endX && current.y === endY) {
        return this.reconstructPath(current);
      }
      
      openSet.splice(openSet.indexOf(current), 1);
      closedSet.add(`${current.x},${current.y}`);
      
      for (const neighbor of this.getNeighbors(current.x, current.y)) {
        if (closedSet.has(`${neighbor.x},${neighbor.y}`)) continue;
        if (neighbor.unit) continue; // 有单位阻挡
        
        const g = current.g + 1;
        const h = this.getDistance(neighbor.x, neighbor.y, endX, endY);
        const f = g + h;
        
        const existing = openSet.find(n => n.x === neighbor.x && n.y === neighbor.y);
        if (existing && g >= existing.g) continue;
        
        openSet.push({ x: neighbor.x, y: neighbor.y, g, h, f, parent: current });
      }
    }
    
    return null; // 无路径
  }
}
```

### 2. 单位系统
```javascript
class Unit {
  constructor(config) {
    this.id = config.id;
    this.type = config.type;
    this.name = config.name;
    this.hp = config.hp;
    this.maxHp = config.maxHp;
    this.attack = config.attack;
    this.defense = config.defense;
    this.moveRange = config.moveRange;
    this.attackRange = config.attackRange;
    this.x = config.x;
    this.y = config.y;
    this.hasMoved = false;
    this.hasAttacked = false;
  }
  
  move(x, y) {
    this.x = x;
    this.y = y;
    this.hasMoved = true;
  }
  
  attack(target) {
    const damage = Math.max(1, this.attack - target.defense);
    target.hp -= damage;
    
    if (target.hp <= 0) {
      target.die();
    }
    
    this.hasAttacked = true;
    return damage;
  }
  
  die() {
    // 单位死亡
    this.hp = 0;
  }
}

const UNIT_TYPES = {
  warrior: { name: '战士', hp: 100, attack: 15, defense: 10, moveRange: 3, attackRange: 1 },
  archer: { name: '弓箭手', hp: 60, attack: 12, defense: 5, moveRange: 4, attackRange: 3 },
  cavalry: { name: '骑兵', hp: 80, attack: 18, defense: 8, moveRange: 5, attackRange: 1 },
  mage: { name: '法师', hp: 50, attack: 20, defense: 3, moveRange: 3, attackRange: 2 }
};
```

### 3. 资源系统
```javascript
class ResourceSystem {
  constructor() {
    this.resources = {
      gold: 100,
      wood: 50,
      stone: 30,
      food: 100
    };
  }
  
  canAfford(cost) {
    for (const [resource, amount] of Object.entries(cost)) {
      if (this.resources[resource] < amount) return false;
    }
    return true;
  }
  
  spend(cost) {
    if (!this.canAfford(cost)) return false;
    for (const [resource, amount] of Object.entries(cost)) {
      this.resources[resource] -= amount;
    }
    return true;
  }
  
  collect() {
    // 每回合收集资源
    this.resources.gold += 10;
    this.resources.wood += 5;
    this.resources.stone += 3;
    this.resources.food += 8;
  }
}
```

### 4. 战斗系统
```javascript
class CombatSystem {
  calculateDamage(attacker, defender, terrain) {
    const baseDamage = attacker.attack - defender.defense;
    const terrainBonus = terrain.defenseBonus || 0;
    const randomFactor = 0.8 + Math.random() * 0.4; // 80%-120%
    
    return Math.max(1, Math.floor((baseDamage - terrainBonus) * randomFactor));
  }
  
  resolveCombat(attacker, defender, terrain) {
    // 攻击者先攻击
    const damage1 = this.calculateDamage(attacker, defender, terrain);
    defender.hp -= damage1;
    
    // 防御者反击（如果还活着且在攻击范围内）
    if (defender.hp > 0 && this.isInRange(defender, attacker)) {
      const damage2 = this.calculateDamage(defender, attacker, terrain);
      attacker.hp -= damage2;
    }
    
    return { damage1, damage2: defender.hp > 0 ? damage2 : 0 };
  }
}
```

## 迭代策略

### 第一版：基础回合制
- 六边形地图
- 1 种单位
- 移动和攻击
- 基础 AI

### 第二版：资源系统
- 资源收集
- 建筑建造
- 单位训练
- 多种单位

### 第三版：战斗深度
- 地形效果
- 单位技能
- 战斗动画
- 多种地图

### 第四版：帝国发展
- 科技树
- 文化系统
- 外交系统
- 多种胜利条件

### 第五版：多人对战
- 多人联机
- 房间系统
- 聊天系统
- 排行榜

## 常见错误

1. **AI 太蠢**：AI 要有挑战性，但不能太强
2. **回合太长**：每个回合要有时间限制
3. **信息不透明**：玩家要能看到所有信息
4. **没有撤销**：误操作要能撤销
5. **平衡性差**：某些单位或策略太强
