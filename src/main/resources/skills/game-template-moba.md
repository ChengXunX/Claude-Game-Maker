---
name: MOBA游戏开发模板
description: MOBA游戏开发模板，适用于多人在线战术竞技类游戏
trigger: MOBA,多人对战,战术竞技,英雄对战,推塔游戏
examples: 英雄联盟|王者荣耀|DOTA2|风暴英雄|300英雄
---

# MOBA游戏开发模板

## 游戏设计核心原则

### 核心循环（每局 15-30 分钟）
```
选择英雄 → 对线发育 → 团战 → 推塔 → 推基地 → 胜利
```
- **团队协作**：5 人配合
- **策略深度**：英雄选择、出装、战术
- **操作技巧**：技能释放、走位

### 玩家心理学
- **"团队胜利"的成就感**：和队友一起获胜
- **"个人发挥"的满足感**：Carry 全场的爽感
- **"策略博弈"的乐趣**：英雄选择、出装策略
- **"操作高光"的快感**：极限操作的成就感

### MOBA 设计要点
```
MOBA核心：
1. 英雄平衡：每个英雄都要有优缺点
2. 节奏控制：前期、中期、后期都有事做
3. 团队配合：单打独斗赢不了
4. 视野控制：眼位、草丛、地图信息
```

## 核心系统设计

### 1. 英雄系统
```javascript
class Hero {
  constructor(config) {
    this.id = config.id;
    this.name = config.name;
    this.role = config.role; // tank, mage, assassin, marksman, support
    this.level = 1;
    this.exp = 0;
    this.gold = 500;
    
    this.stats = {
      hp: config.hp,
      maxHp: config.hp,
      mp: config.mp,
      maxMp: config.mp,
      attack: config.attack,
      defense: config.defense,
      speed: config.speed,
      attackRange: config.attackRange
    };
    
    this.skills = config.skills;
    this.items = [];
    this.position = { x: 0, y: 0 };
    this.alive = true;
  }
  
  levelUp() {
    this.level++;
    this.exp -= this.getExpToNextLevel();
    
    // 提升属性
    this.stats.maxHp += 50;
    this.stats.hp = this.stats.maxHp;
    this.stats.attack += 3;
    this.stats.defense += 2;
    
    // 技能点
    this.skillPoints++;
  }
  
  buyItem(item) {
    if (this.gold < item.cost) return false;
    
    this.gold -= item.cost;
    this.items.push(item);
    this.applyItemStats(item);
    
    return true;
  }
  
  die() {
    this.alive = false;
    this.respawnTimer = 10 + this.level * 2;
  }
}

const HEROES = {
  warrior: { name: '战士', role: 'tank', hp: 600, mp: 200, attack: 50, defense: 30, speed: 300 },
  mage: { name: '法师', role: 'mage', hp: 400, mp: 400, attack: 30, defense: 15, speed: 280 },
  assassin: { name: '刺客', role: 'assassin', hp: 450, mp: 250, attack: 60, defense: 20, speed: 350 },
  marksman: { name: '射手', role: 'marksman', hp: 350, mp: 200, attack: 55, defense: 10, speed: 290 },
  support: { name: '辅助', role: 'support', hp: 500, mp: 350, attack: 25, defense: 25, speed: 310 }
};
```

### 2. 小兵系统
```javascript
class Minion {
  constructor(config) {
    this.type = config.type; // melee, caster, cannon
    this.hp = config.hp;
    this.attack = config.attack;
    this.defense = config.defense;
    this.speed = config.speed;
    this.gold = config.gold;
    this.exp = config.exp;
    this.position = { ...config.position };
    this.target = null;
  }
  
  update(delta) {
    // 寻找目标
    if (!this.target || !this.target.alive) {
      this.findTarget();
    }
    
    // 移动到目标
    if (this.target) {
      const dist = this.getDistance(this.position, this.target.position);
      
      if (dist > this.attackRange) {
        this.moveToward(this.target.position, delta);
      } else {
        this.attack(this.target);
      }
    }
  }
  
  findTarget() {
    // 优先攻击敌方小兵
    const enemyMinions = this.getEnemyMinions();
    if (enemyMinions.length > 0) {
      this.target = enemyMinions[0];
      return;
    }
    
    // 然后攻击敌方英雄
    const enemyHeroes = this.getEnemyHeroes();
    if (enemyHeroes.length > 0) {
      this.target = enemyHeroes[0];
    }
  }
}
```

### 3. 防御塔系统
```javascript
class Tower {
  constructor(config) {
    this.team = config.team;
    this.hp = config.hp;
    this.maxHp = config.hp;
    this.attack = config.attack;
    this.defense = config.defense;
    this.attackRange = config.attackRange;
    this.target = null;
    this.position = { ...config.position };
  }
  
  update(delta) {
    // 寻找目标
    if (!this.target || !this.target.alive) {
      this.findTarget();
    }
    
    // 攻击目标
    if (this.target) {
      this.attack(this.target);
    }
  }
  
  findTarget() {
    const enemies = this.getEnemiesInRange();
    
    // 优先攻击正在攻击己方英雄的敌人
    const attackers = enemies.filter(e => e.target && e.target.team === this.team);
    if (attackers.length > 0) {
      this.target = attackers[0];
      return;
    }
    
    // 然后攻击小兵
    const minions = enemies.filter(e => e.type === 'minion');
    if (minions.length > 0) {
      this.target = minions[0];
      return;
    }
    
    // 最后攻击英雄
    const heroes = enemies.filter(e => e.type === 'hero');
    if (heroes.length > 0) {
      this.target = heroes[0];
    }
  }
}
```

### 4. 经济系统
```javascript
class EconomySystem {
  constructor() {
    this.goldPerSecond = 2;
    this.goldPerKill = 300;
    this.goldPerAssist = 150;
    this.goldPerMinion = 20;
  }
  
  update(delta) {
    // 每秒获得金币
    this.hero.gold += this.goldPerSecond * delta;
  }
  
  onKill(killer, victim) {
    // 击杀获得金币
    killer.gold += this.goldPerKill;
    
    // 助攻获得金币
    const assistants = this.getAssistants(killer, victim);
    for (const assistant of assistants) {
      assistant.gold += this.goldPerAssist;
    }
  }
  
  onMinionKill(killer, minion) {
    // 补兵获得金币
    killer.gold += this.goldPerMinion;
    killer.exp += minion.exp;
  }
}
```

### 5. 地图系统
```javascript
class GameMap {
  constructor() {
    this.width = 15000;
    this.height = 15000;
    this.lanes = {
      top: this.generateLane('top'),
      mid: this.generateLane('mid'),
      bot: this.generateLane('bot')
    };
    this.jungle = this.generateJungle();
    this.towers = this.generateTowers();
  }
  
  generateLane(type) {
    // 生成兵线路径
    const points = [];
    // ... 路径点生成
    return points;
  }
  
  generateJungle() {
    // 生成野区
    return {
      monsters: [
        { type: 'blueBuff', position: { x: 3000, y: 7500 }, respawn: 300 },
        { type: 'redBuff', position: { x: 12000, y: 7500 }, respawn: 300 },
        { type: 'dragon', position: { x: 7500, y: 3000 }, respawn: 600 },
        { type: 'baron', position: { x: 7500, y: 12000 }, respawn: 1200 }
      ]
    };
  }
}
```

## 迭代策略

### 第一版：基础对战
- 1 个英雄
- 基础小兵
- 1 座塔
- 简单 AI

### 第二版：英雄系统
- 3 个英雄
- 技能系统
- 等级系统
- 装备系统

### 第三版：地图系统
- 3 条兵线
- 野区系统
- 防御塔
- 基地

### 第四版：团队系统
- 5v5 对战
- 英雄选择
- 召唤师技能
- 符文系统

### 第五版：竞技化
- 排位系统
- 赛季系统
- 观战系统
- 回放系统

## 常见错误

1. **英雄不平衡**：某些英雄太强或太弱
2. **节奏太慢**：前期太无聊
3. **没有团战**：团战是 MOBA 的核心
4. **网络延迟**：延迟会毁掉体验
5. **挂机问题**：要有惩罚机制
