---
name: SLG策略游戏开发模板
description: SLG策略游戏开发模板，适用于4X策略、文明类、帝国建设类游戏
trigger: SLG, simulation, 模拟, 策略, 4X, civilization, 文明, empire, 帝国, war, 战争
examples: 文明|全面战争|钢铁雄心|欧陆风云|星际争霸|Age of Empires
---

# SLG 策略游戏开发模板

## 游戏设计核心原则

### 核心循环（每回合 5-15 分钟）
```
收集资源 → 建造/研究 → 移动单位 → 外交/战争 → 结算 → 下一回合
```
- **长期规划**：要考虑多个回合后的结果
- **策略深度**：每个决策都有影响
- **帝国成长**：从小国到大帝国

### 玩家心理学
- **"运筹帷幄"的掌控感**：指挥帝国的满足感
- **"以弱胜强"的成就感**：用策略战胜强敌
- **"科技领先"的优势感**：科技领先的快感
- **"统一世界"的终极目标**：征服世界的欲望

### SLG 设计要点
```
SLG核心：
1. 资源管理：有限资源的最优分配
2. 科技树：解锁新能力的期待感
3. 外交系统：联盟、贸易、战争
4. 多种胜利条件：军事、科技、文化、经济
```

## 核心系统设计

### 1. 城市系统
```javascript
class City {
  constructor(config) {
    this.name = config.name;
    this.position = config.position;
    this.population = 1;
    this.buildings = [];
    this.production = null;
    this.resources = { food: 0, production: 0, gold: 0, science: 0 };
  }
  
  update(delta) {
    // 收集资源
    this.collectResources(delta);
    
    // 人口增长
    this.checkGrowth();
    
    // 生产进度
    if (this.production) {
      this.production.progress += this.resources.production * delta;
      if (this.production.progress >= this.production.cost) {
        this.completeProduction();
      }
    }
  }
  
  collectResources(delta) {
    const terrain = this.getTerrain();
    
    this.resources.food = terrain.food * this.population;
    this.resources.production = terrain.production * this.population;
    this.resources.gold = terrain.gold * this.population;
    this.resources.science = terrain.science * this.population;
    
    // 建筑加成
    for (const building of this.buildings) {
      this.resources.food += building.foodBonus;
      this.resources.production += building.productionBonus;
      this.resources.gold += building.goldBonus;
      this.resources.science += building.scienceBonus;
    }
  }
  
  checkGrowth() {
    if (this.resources.food >= this.getGrowthCost()) {
      this.population++;
      this.resources.food -= this.getGrowthCost();
    }
  }
}

const BUILDINGS = {
  granary: { name: '粮仓', cost: 60, foodBonus: 2 },
  workshop: { name: '工坊', cost: 80, productionBonus: 2 },
  market: { name: '市场', cost: 100, goldBonus: 3 },
  library: { name: '图书馆', cost: 80, scienceBonus: 2 },
  barracks: { name: '兵营', cost: 100, unlocks: 'warrior' }
};
```

### 2. 科技树系统
```javascript
class TechTree {
  constructor() {
    this.researched = [];
    this.currentResearch = null;
    this.researchProgress = 0;
  }
  
  startResearch(techId) {
    const tech = TECHS[techId];
    if (!tech) return false;
    
    // 检查前置科技
    for (const prereq of tech.prerequisites) {
      if (!this.researched.includes(prereq)) return false;
    }
    
    this.currentResearch = techId;
    this.researchProgress = 0;
    
    return true;
  }
  
  update(delta, sciencePoints) {
    if (!this.currentResearch) return;
    
    this.researchProgress += sciencePoints * delta;
    
    const tech = TECHS[this.currentResearch];
    if (this.researchProgress >= tech.cost) {
      this.completeResearch();
    }
  }
  
  completeResearch() {
    this.researched.push(this.currentResearch);
    
    // 解锁新内容
    const tech = TECHS[this.currentResearch];
    for (const unlock of tech.unlocks) {
      this.unlock(unlock);
    }
    
    this.currentResearch = null;
  }
}

const TECHS = {
  agriculture: { name: '农业', cost: 20, prerequisites: [], unlocks: ['granary'] },
  mining: { name: '采矿', cost: 30, prerequisites: [], unlocks: ['mine'] },
  pottery: { name: '陶器', cost: 25, prerequisites: ['agriculture'], unlocks: ['granary'] },
  bronzeWorking: { name: '青铜', cost: 50, prerequisites: ['mining'], unlocks: ['warrior'] },
  ironWorking: { name: '铁器', cost: 100, prerequisites: ['bronzeWorking'], unlocks: ['swordsman'] },
  mathematics: { name: '数学', cost: 80, prerequisites: ['pottery'], unlocks: ['library'] }
};
```

### 3. 单位系统
```javascript
class Unit {
  constructor(config) {
    this.type = config.type;
    this.name = config.name;
    this.hp = config.hp;
    this.maxHp = config.hp;
    this.attack = config.attack;
    this.defense = config.defense;
    this.movement = config.movement;
    this.remainingMovement = config.movement;
    this.position = { ...config.position };
  }
  
  move(target) {
    const distance = this.getDistance(this.position, target);
    
    if (distance > this.remainingMovement) return false;
    
    this.position = { ...target };
    this.remainingMovement -= distance;
    
    return true;
  }
  
  attack(target) {
    const damage = this.calculateDamage(target);
    target.hp -= damage;
    
    if (target.hp <= 0) {
      this.captureTile(target.position);
    }
    
    return damage;
  }
  
  calculateDamage(target) {
    const baseDamage = this.attack - target.defense * 0.5;
    const randomFactor = 0.8 + Math.random() * 0.4;
    return Math.max(1, Math.floor(baseDamage * randomFactor));
  }
}

const UNITS = {
  warrior: { name: '战士', hp: 100, attack: 15, defense: 10, movement: 2, cost: 40 },
  archer: { name: '弓箭手', hp: 75, attack: 12, defense: 5, movement: 2, cost: 50, range: 2 },
  cavalry: { name: '骑兵', hp: 100, attack: 18, defense: 8, movement: 4, cost: 80 },
  swordsman: { name: '剑士', hp: 120, attack: 20, defense: 15, movement: 2, cost: 70 }
};
```

### 4. 外交系统
```javascript
class DiplomacySystem {
  constructor() {
    this.relations = {};
    this.treaties = {};
  }
  
  getRelation(civ1, civ2) {
    const key = this.getRelationKey(civ1, civ2);
    return this.relations[key] || 0;
  }
  
  changeRelation(civ1, civ2, amount) {
    const key = this.getRelationKey(civ1, civ2);
    this.relations[key] = Math.max(-100, Math.min(100, (this.relations[key] || 0) + amount));
  }
  
  proposeTreaty(civ1, civ2, type) {
    const relation = this.getRelation(civ1, civ2);
    
    switch (type) {
      case 'peace':
        if (relation >= -20) {
          this.signTreaty(civ1, civ2, 'peace');
          return true;
        }
        break;
      case 'alliance':
        if (relation >= 50) {
          this.signTreaty(civ1, civ2, 'alliance');
          return true;
        }
        break;
      case 'trade':
        if (relation >= 0) {
          this.signTreaty(civ1, civ2, 'trade');
          return true;
        }
        break;
    }
    
    return false;
  }
}
```

### 5. 迷雾系统
```javascript
class FogOfWar {
  constructor(mapWidth, mapHeight) {
    this.width = mapWidth;
    this.height = mapHeight;
    this.visibility = Array(mapHeight).fill(null).map(() => Array(mapWidth).fill(0));
  }
  
  updateVisibility(units) {
    // 重置视野
    for (let y = 0; y < this.height; y++) {
      for (let x = 0; x < this.width; x++) {
        this.visibility[y][x] = Math.max(0, this.visibility[y][x] - 1);
      }
    }
    
    // 更新单位视野
    for (const unit of units) {
      const range = unit.visionRange;
      
      for (let dy = -range; dy <= range; dy++) {
        for (let dx = -range; dx <= range; dx++) {
          const x = unit.position.x + dx;
          const y = unit.position.y + dy;
          
          if (x >= 0 && x < this.width && y >= 0 && y < this.height) {
            const dist = Math.sqrt(dx * dx + dy * dy);
            if (dist <= range) {
              this.visibility[y][x] = 2;
            }
          }
        }
      }
    }
  }
  
  isVisible(x, y) {
    return this.visibility[y][x] > 0;
  }
  
  isExplored(x, y) {
    return this.visibility[y][x] === 2;
  }
}
```

## 迭代策略

### 第一版：基础回合制
- 六边形地图
- 1 种城市
- 1 种单位
- 基础资源

### 第二版：科技系统
- 科技树
- 多种建筑
- 多种单位
- 基础 AI

### 第三版：外交系统
- 外交关系
- 条约系统
- 贸易系统
- 宣战系统

### 第四版：深度玩法
- 多种文明
- 特殊能力
- 奇观系统
- 多种胜利条件

### 第五版：多人对战
- 多人联机
- 房间系统
- 聊天系统
- 排行榜

## 常见错误

1. **回合太长**：每个回合要有时间限制
2. **AI 太蠢**：AI 要有基本的策略
3. **信息不透明**：玩家要能看到所有信息
4. **没有撤销**：误操作要能撤销
5. **平衡性差**：某些策略太强
