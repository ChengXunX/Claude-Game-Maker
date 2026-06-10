---
name: 生存游戏开发模板
description: 生存游戏开发模板，适用于生存建造、末日生存、荒野求生类游戏
trigger: 生存游戏,survival game,末日生存,荒野求生,生存建造,饥荒
examples: 饥荒|方舟生存进化|森林|绿色地狱|英灵神殿|Don't Starve
---

# 生存游戏开发模板

## 游戏设计核心原则

### 核心循环（持续进行）
```
探索 → 收集资源 → 制作工具 → 建造基地 → 对抗威胁 → 升级装备
```
- **生存压力**：饥饿、口渴、温度持续下降
- **探索奖励**：新区域有更好资源
- **建造成就感**：从露天到有家的安全感

### 玩家心理学
- **"活下去"的紧迫感**：资源不断减少的焦虑
- **"建造家园"的安全感**：从露天到有庇护所
- **"探索未知"的好奇心**：新区域、新资源、新危险
- **"从弱到强"的成长感**：从赤手空拳到全副武装

### 生存压力设计
```
生存属性（持续下降）：
- 饥饿值：每 60 秒下降 1，降到 0 开始扣血
- 口渴值：每 45 秒下降 1，降到 0 开始扣血
- 体温：环境温度影响，过冷/过热扣血
- 理智值：夜晚/怪物附近下降，影响视野

关键：让玩家有压力，但不至于绝望
```

## 核心系统设计

### 1. 生存属性系统
```javascript
class SurvivalStats {
  constructor() {
    this.health = 100;
    this.hunger = 100;
    this.thirst = 100;
    this.stamina = 100;
    this.temperature = 37; // 体温
    this.sanity = 100;
  }
  
  update(delta) {
    // 饥饿下降
    this.hunger -= 0.016 * delta; // 每秒下降 0.016，约 60 秒下降 1
    
    // 口渴下降
    this.thirst -= 0.022 * delta; // 每秒下降 0.022，约 45 秒下降 1
    
    // 饥饿/口渴为 0 时扣血
    if (this.hunger <= 0) {
      this.health -= 0.05 * delta;
    }
    if (this.thirst <= 0) {
      this.health -= 0.05 * delta;
    }
    
    // 体温影响
    if (this.temperature < 35 || this.temperature > 39) {
      this.health -= 0.03 * delta;
    }
    
    // 理智值影响
    if (this.sanity < 30) {
      // 低理智时视野变暗，出现幻觉
      this.applySanityEffects();
    }
    
    // 限制范围
    this.health = Math.max(0, Math.min(100, this.health));
    this.hunger = Math.max(0, Math.min(100, this.hunger));
    this.thirst = Math.max(0, Math.min(100, this.thirst));
    this.stamina = Math.max(0, Math.min(100, this.stamina));
    this.sanity = Math.max(0, Math.min(100, this.sanity));
  }
  
  eat(food) {
    this.hunger = Math.min(100, this.hunger + food.hungerRestore);
    if (food.thirstEffect) {
      this.thirst = Math.min(100, this.thirst + food.thirstEffect);
    }
  }
  
  drink(water) {
    this.thirst = Math.min(100, this.thirst + water.thirstRestore);
  }
  
  isAlive() {
    return this.health > 0;
  }
}
```

### 2. 资源收集系统
```javascript
class ResourceSystem {
  constructor() {
    this.inventory = {};
    this.maxSlots = 20;
  }
  
  collect(resource) {
    if (this.getUsedSlots() >= this.maxSlots) {
      return false; // 背包已满
    }
    
    if (!this.inventory[resource.id]) {
      this.inventory[resource.id] = 0;
    }
    this.inventory[resource.id] += resource.amount;
    
    return true;
  }
  
  hasItem(itemId, amount = 1) {
    return (this.inventory[itemId] || 0) >= amount;
  }
  
  useItem(itemId, amount = 1) {
    if (!this.hasItem(itemId, amount)) return false;
    this.inventory[itemId] -= amount;
    if (this.inventory[itemId] <= 0) {
      delete this.inventory[itemId];
    }
    return true;
  }
  
  getUsedSlots() {
    return Object.keys(this.inventory).length;
  }
}

const RESOURCES = {
  wood: { name: '木材', stackSize: 64 },
  stone: { name: '石头', stackSize: 64 },
  fiber: { name: '纤维', stackSize: 64 },
  meat: { name: '生肉', stackSize: 20, perishable: true },
  water: { name: '水', stackSize: 10 }
};
```

### 3. 制作系统
```javascript
class CraftingSystem {
  constructor() {
    this.recipes = {};
  }
  
  addRecipe(recipe) {
    this.recipes[recipe.id] = recipe;
  }
  
  canCraft(recipeId, inventory) {
    const recipe = this.recipes[recipeId];
    if (!recipe) return false;
    
    for (const [itemId, amount] of Object.entries(recipe.ingredients)) {
      if (!inventory.hasItem(itemId, amount)) return false;
    }
    
    return true;
  }
  
  craft(recipeId, inventory) {
    if (!this.canCraft(recipeId, inventory)) return false;
    
    const recipe = this.recipes[recipeId];
    
    // 消耗材料
    for (const [itemId, amount] of Object.entries(recipe.ingredients)) {
      inventory.useItem(itemId, amount);
    }
    
    // 获得物品
    inventory.collect({ id: recipe.result, amount: recipe.resultAmount || 1 });
    
    return true;
  }
}

const RECIPES = [
  { id: 'axe', result: 'axe', ingredients: { wood: 3, stone: 2 } },
  { id: 'pickaxe', result: 'pickaxe', ingredients: { wood: 3, stone: 3 } },
  { id: 'campfire', result: 'campfire', ingredients: { wood: 5, stone: 3 } },
  { id: 'shelter', result: 'shelter', ingredients: { wood: 20, stone: 10, fiber: 10 } }
];
```

### 4. 建造系统
```javascript
class BuildingSystem {
  constructor() {
    this.buildings = [];
    this.grid = new Grid(100, 100);
  }
  
  placeBuilding(buildingId, x, y) {
    const building = BUILDINGS[buildingId];
    if (!building) return false;
    
    // 检查位置是否可用
    if (!this.canPlace(x, y, building.size)) return false;
    
    // 检查材料
    if (!this.hasMaterials(building)) return false;
    
    // 放置建筑
    this.buildings.push({
      id: buildingId,
      x, y,
      ...building
    });
    
    return true;
  }
  
  canPlace(x, y, size) {
    // 检查位置是否被占用
    for (let dx = 0; dx < size.width; dx++) {
      for (let dy = 0; dy < size.height; dy++) {
        if (this.grid.get(x + dx, y + dy)) return false;
      }
    }
    return true;
  }
}

const BUILDINGS = {
  campfire: { name: '营火', size: { width: 1, height: 1 }, effect: 'warmth' },
  shelter: { name: '庇护所', size: { width: 3, height: 3 }, effect: 'protection' },
  workbench: { name: '工作台', size: { width: 2, height: 1 }, effect: 'crafting' },
  storage: { name: '储物箱', size: { width: 2, height: 1 }, effect: 'storage' }
};
```

### 5. 昼夜系统
```javascript
class DayNightSystem {
  constructor() {
    this.time = 0; // 0-24 小时
    this.dayLength = 600; // 10 分钟一天
    this.speed = 24 / this.dayLength; // 每秒多少小时
  }
  
  update(delta) {
    this.time += this.speed * delta;
    if (this.time >= 24) this.time -= 24;
  }
  
  isDay() {
    return this.time >= 6 && this.time < 18;
  }
  
  isNight() {
    return !this.isDay();
  }
  
  getLightLevel() {
    if (this.time >= 6 && this.time < 8) {
      // 日出
      return (this.time - 6) / 2;
    } else if (this.time >= 8 && this.time < 16) {
      // 白天
      return 1;
    } else if (this.time >= 16 && this.time < 18) {
      // 日落
      return 1 - (this.time - 16) / 2;
    } else {
      // 夜晚
      return 0.1;
    }
  }
}
```

## 迭代策略

### 第一版：基础生存
- 生存属性（饥饿、口渴）
- 资源收集（木材、石头）
- 基础制作（工具）
- 简单建造（营火）

### 第二版：建造系统
- 多种建筑
- 建筑效果
- 背包系统
- 制作系统

### 第三版：危险系统
- 敌人（怪物）
- 战斗系统
- 死亡惩罚
- 天气系统

### 第四版：深度玩法
- 多种生物群落
- 季节系统
- 农业系统
- 驯服系统

### 第五版：多人联机
- 多人合作
- 基地共享
- 交易系统
- PVP 模式

## 常见错误

1. **生存压力太大**：玩家要有喘息空间，不能一直焦虑
2. **资源太稀缺**：让玩家有探索的动力，但不能饿死
3. **建造太复杂**：建造要简单直观，不能太繁琐
4. **没有教程**：生存游戏机制多，必须有引导
5. **死亡太惩罚**：死亡要有损失，但不能让玩家放弃
