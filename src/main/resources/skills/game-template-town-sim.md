---
name: 小镇模拟经营游戏开发模板
description: 小镇模拟经营游戏开发模板，适用于建造、经营、装饰类游戏
trigger: town, 小镇, sim, 模拟, village, 村庄, build, 建造, decoration, 装饰
examples: 模拟城市|动物森友会|星露谷物语|Township|Hay Day
---

# 小镇模拟经营游戏开发模板

## 游戏设计核心原则

### 核心循环（每 5-15 分钟一轮）
```
收集资源 → 建造建筑 → 满足居民需求 → 获得收益 → 扩大城镇
```
- **建造自由**：玩家可以自由摆放建筑
- **居民反馈**：居民会表达需求和满意度
- **成长感**：从荒地到繁华小镇

### 玩家心理学
- **"创造世界"的满足感**：建造自己的小镇
- **"照顾居民"的责任感**：满足居民需求
- **"美化环境"的成就感**：装饰让小镇更漂亮
- **"解锁新内容"的期待感**：新建筑、新装饰

### 居民需求设计
```
居民需求层次：
1. 基础需求：食物、住房、安全
2. 社交需求：公园、广场、娱乐
3. 发展需求：学校、医院、工作
4. 美化需求：装饰、绿化、美化

满足需求 → 幸福度提升 → 更多居民 → 更多收益
```

## 核心系统设计

### 1. 建筑系统
```javascript
class Building {
  constructor(config) {
    this.id = config.id;
    this.type = config.type;
    this.name = config.name;
    this.size = config.size; // { width, height }
    this.cost = config.cost;
    this.buildTime = config.buildTime;
    this.x = 0;
    this.y = 0;
    this.level = 1;
    this.production = config.production;
  }
  
  canUpgrade() {
    return this.level < this.maxLevel;
  }
  
  getUpgradeCost() {
    return Math.floor(this.cost * Math.pow(1.5, this.level));
  }
  
  upgrade() {
    if (!this.canUpgrade()) return false;
    this.level++;
    return true;
  }
  
  getProduction() {
    return this.production * this.level;
  }
}

const BUILDINGS = {
  house: { name: '房屋', size: { width: 2, height: 2 }, cost: 100, population: 4 },
  farm: { name: '农场', size: { width: 3, height: 3 }, cost: 200, production: 10 },
  shop: { name: '商店', size: { width: 2, height: 2 }, cost: 300, income: 50 },
  park: { name: '公园', size: { width: 2, height: 2 }, cost: 150, happiness: 10 },
  school: { name: '学校', size: { width: 3, height: 3 }, cost: 500, education: 20 }
};
```

### 2. 居民系统
```javascript
class Resident {
  constructor(config) {
    this.id = config.id;
    this.name = config.name;
    this.home = null;
    this.work = null;
    this.happiness = 50;
    this.needs = {
      food: 100,
      shelter: 100,
      entertainment: 50,
      education: 50,
      health: 50
    };
  }
  
  update(delta) {
    // 需求下降
    this.needs.food -= 0.01 * delta;
    this.needs.entertainment -= 0.005 * delta;
    
    // 计算幸福度
    this.happiness = this.calculateHappiness();
    
    // 检查是否离开
    if (this.happiness < 20) {
      this.considerLeaving();
    }
  }
  
  calculateHappiness() {
    const weights = { food: 0.3, shelter: 0.25, entertainment: 0.2, education: 0.15, health: 0.1 };
    let total = 0;
    for (const [need, value] of Object.entries(this.needs)) {
      total += value * weights[need];
    }
    return Math.max(0, Math.min(100, total));
  }
}
```

### 3. 资源系统
```javascript
class ResourceManager {
  constructor() {
    this.resources = {
      gold: 500,
      wood: 100,
      stone: 50,
      food: 200
    };
    this.production = {
      gold: 0,
      wood: 0,
      stone: 0,
      food: 0
    };
  }
  
  update(delta) {
    // 收集产出
    for (const [resource, amount] of Object.entries(this.production)) {
      this.resources[resource] += amount * delta / 60; // 每分钟产出
    }
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
  
  collect(resource, amount) {
    this.resources[resource] += amount;
  }
}
```

### 4. 装饰系统
```javascript
class DecorationSystem {
  constructor() {
    this.decorations = [];
  }
  
  placeDecoration(decorationId, x, y) {
    const decoration = DECORATIONS[decorationId];
    if (!decoration) return false;
    
    this.decorations.push({
      id: decorationId,
      x, y,
      ...decoration
    });
    
    return true;
  }
  
  getHappinessBonus(x, y, radius) {
    let bonus = 0;
    for (const decoration of this.decorations) {
      const dist = Math.sqrt((decoration.x - x) ** 2 + (decoration.y - y) ** 2);
      if (dist <= radius) {
        bonus += decoration.happiness;
      }
    }
    return bonus;
  }
}

const DECORATIONS = {
  tree: { name: '树木', cost: 50, happiness: 2, radius: 3 },
  fountain: { name: '喷泉', cost: 200, happiness: 5, radius: 5 },
  statue: { name: '雕像', cost: 500, happiness: 8, radius: 4 },
  bench: { name: '长椅', cost: 30, happiness: 1, radius: 2 }
};
```

### 5. 任务系统
```javascript
class QuestSystem {
  constructor() {
    this.quests = [];
    this.completed = [];
  }
  
  addQuest(quest) {
    this.quests.push(quest);
  }
  
  checkCompletion(questId) {
    const quest = this.quests.find(q => q.id === questId);
    if (!quest) return false;
    
    return quest.objectives.every(obj => obj.completed);
  }
  
  completeQuest(questId) {
    const index = this.quests.findIndex(q => q.id === questId);
    if (index === -1) return false;
    
    const quest = this.quests[index];
    this.completed.push(quest);
    this.quests.splice(index, 1);
    
    // 发放奖励
    for (const [resource, amount] of Object.entries(quest.rewards)) {
      this.resourceManager.collect(resource, amount);
    }
    
    return true;
  }
}

const QUESTS = [
  { id: 'first_house', name: '第一栋房子', objectives: [{ type: 'build', building: 'house', count: 1 }], rewards: { gold: 100 } },
  { id: 'population_10', name: '人口增长', objectives: [{ type: 'population', count: 10 }], rewards: { gold: 200 } },
  { id: 'happiness_80', name: '幸福小镇', objectives: [{ type: 'happiness', value: 80 }], rewards: { gold: 500 } }
];
```

## 迭代策略

### 第一版：基础建造
- 1 种建筑（房屋）
- 资源收集
- 基础居民
- 简单 UI

### 第二版：多种建筑
- 5 种建筑
- 建筑升级
- 居民需求
- 任务系统

### 第三版：装饰系统
- 10 种装饰
- 幸福度系统
- 成就系统
- 排行榜

### 第四版：深度玩法
- 20 种建筑
- 天气系统
- 季节系统
- 特殊事件

### 第五版：社交功能
- 好友访问
- 交易系统
- 社交分享
- 赛季系统

## 常见错误

1. **建造太贵**：玩家要能频繁建造，否则会失去兴趣
2. **居民没有反馈**：居民要有表情和对话
3. **没有目标**：要有任务引导玩家
4. **装饰没有效果**：装饰要影响幸福度
5. **没有成长感**：要让玩家看到小镇的变化
