---
name: 派对合作游戏开发模板
description: 派对合作游戏开发模板，适用于胡闹厨房、多人合作类游戏
trigger: party, 派对, coop, 合作, overcooked, 胡闹厨房, multiplayer, 联机, cook, 烹饪
examples: 胡闹厨房|Overcooked|Moving Out|Tools Up|Unrailed
---

# 派对合作游戏开发模板

## 游戏设计核心原则

### 核心循环（每关 3-5 分钟）
```
接单 → 分工合作 → 完成任务 → 得分 → 下一关
```
- **合作性**：必须多人配合
- **紧张感**：时间紧迫
- **欢乐感**：失败也很有趣

### 玩家心理学
- **"合作成功"的成就感**：和朋友一起通关
- **"手忙脚乱"的欢乐感**：混乱中的乐趣
- **"分工配合"的默契感**：完美配合的爽感
- **"搞笑失败"的乐趣**：失败也很有趣

### 派对游戏设计要点
```
派对核心：
1. 简单操作：任何人都能玩
2. 必须合作：单人无法完成
3. 时间紧迫：增加紧张感
4. 欢乐氛围：失败也要有趣
```

## 核心系统设计

### 1. 订单系统
```javascript
class OrderSystem {
  constructor() {
    this.orders = [];
    this.maxOrders = 5;
    this.orderTimer = 0;
    this.orderInterval = 10; // 每 10 秒一个新订单
  }
  
  update(delta) {
    this.orderTimer += delta;
    
    if (this.orderTimer >= this.orderInterval && this.orders.length < this.maxOrders) {
      this.generateOrder();
      this.orderTimer = 0;
    }
    
    // 检查订单超时
    for (const order of this.orders) {
      order.timer -= delta;
      if (order.timer <= 0) {
        this.failOrder(order);
      }
    }
  }
  
  generateOrder() {
    const recipes = Object.values(RECIPES);
    const recipe = recipes[Math.floor(Math.random() * recipes.length)];
    
    this.orders.push({
      id: this.generateId(),
      recipe: recipe,
      timer: recipe.timeLimit,
      status: 'waiting'
    });
  }
  
  completeOrder(orderId, dish) {
    const order = this.orders.find(o => o.id === orderId);
    if (!order) return false;
    
    if (this.matchRecipe(order.recipe, dish)) {
      order.status = 'completed';
      this.addScore(order.recipe.score);
      this.removeOrder(orderId);
      return true;
    }
    
    return false;
  }
}

const RECIPES = {
  burger: { name: '汉堡', ingredients: ['bun', 'meat', 'lettuce'], timeLimit: 30, score: 100 },
  pizza: { name: '披萨', ingredients: ['dough', 'cheese', 'tomato'], timeLimit: 45, score: 150 },
  salad: { name: '沙拉', ingredients: ['lettuce', 'tomato', 'cucumber'], timeLimit: 20, score: 80 }
};
```

### 2. 厨房系统
```javascript
class KitchenSystem {
  constructor() {
    this.stations = [];
    this.items = [];
  }
  
  addStation(config) {
    this.stations.push({
      id: config.id,
      type: config.type, // cutting, cooking, plating, washing
      position: config.position,
      occupied: false,
      currentItem: null
    });
  }
  
  placeItem(stationId, item) {
    const station = this.stations.find(s => s.id === stationId);
    if (!station || station.occupied) return false;
    
    station.currentItem = item;
    station.occupied = true;
    
    return true;
  }
  
  processItem(stationId) {
    const station = this.stations.find(s => s.id === stationId);
    if (!station || !station.occupied) return null;
    
    const item = station.currentItem;
    
    switch (station.type) {
      case 'cutting':
        item.state = 'cut';
        break;
      case 'cooking':
        item.state = 'cooked';
        break;
      case 'plating':
        item.state = 'plated';
        break;
    }
    
    return item;
  }
}
```

### 3. 角色系统
```javascript
class ChefSystem {
  constructor() {
    this.chefs = [];
  }
  
  addChef(config) {
    this.chefs.push({
      id: config.id,
      name: config.name,
      position: config.position,
      speed: config.speed,
      carrying: null,
      action: null
    });
  }
  
  pickUp(chefId, itemId) {
    const chef = this.chefs.find(c => c.id === chefId);
    if (!chef || chef.carrying) return false;
    
    chef.carrying = itemId;
    return true;
  }
  
  putDown(chefId, stationId) {
    const chef = this.chefs.find(c => c.id === chefId);
    if (!chef || !chef.carrying) return false;
    
    const station = this.stations.find(s => s.id === stationId);
    if (!station) return false;
    
    this.placeItem(stationId, chef.carrying);
    chef.carrying = null;
    
    return true;
  }
  
  useStation(chefId, stationId) {
    const chef = this.chefs.find(c => c.id === chefId);
    if (!chef) return false;
    
    const station = this.stations.find(s => s.id === stationId);
    if (!station || station.occupied) return false;
    
    chef.action = 'using_station';
    this.processItem(stationId);
    
    return true;
  }
}
```

### 4. 评分系统
```javascript
class ScoringSystem {
  constructor() {
    this.score = 0;
    this.combo = 0;
    this.maxCombo = 0;
  }
  
  addScore(amount) {
    this.combo++;
    this.maxCombo = Math.max(this.maxCombo, this.combo);
    
    const bonus = Math.floor(amount * (1 + this.combo * 0.1));
    this.score += bonus;
    
    this.showScorePopup(bonus);
  }
  
  resetCombo() {
    this.combo = 0;
  }
  
  getStarRating() {
    const thresholds = this.getLevelThresholds();
    
    if (this.score >= thresholds.three) return 3;
    if (this.score >= thresholds.two) return 2;
    if (this.score >= thresholds.one) return 1;
    return 0;
  }
}
```

### 5. 关卡系统
```javascript
class LevelSystem {
  constructor() {
    this.currentLevel = 1;
    this.levels = LEVELS;
  }
  
  getLevelConfig() {
    return this.levels[this.currentLevel - 1];
  }
  
  checkLevelComplete() {
    const config = this.getLevelConfig();
    return this.score >= config.targetScore;
  }
  
  advanceLevel() {
    this.currentLevel++;
    this.loadLevel(this.getLevelConfig());
  }
}

const LEVELS = [
  { level: 1, name: '入门厨房', targetScore: 100, orderInterval: 15, maxOrders: 3 },
  { level: 2, name: '双人厨房', targetScore: 200, orderInterval: 12, maxOrders: 4 },
  { level: 3, name: '忙碌厨房', targetScore: 350, orderInterval: 10, maxOrders: 5 },
  { level: 4, name: '混乱厨房', targetScore: 500, orderInterval: 8, maxOrders: 6 },
  { level: 5, name: '地狱厨房', targetScore: 700, orderInterval: 6, maxOrders: 7 }
];
```

## 迭代策略

### 第一版：基础厨房
- 1 种菜谱
- 简单厨房
- 双人合作
- 基础 UI

### 第二版：订单系统
- 多种菜谱
- 订单系统
- 计时系统
- 计分系统

### 第三版：厨房扩展
- 多种厨房
- 特殊机制
- 道具系统
- 成就系统

### 第四版：深度玩法
- 10 个关卡
- 多种模式
- 排行榜
- 解锁系统

### 第五版：多人模式
- 4 人合作
- 在线联机
- 房间系统
- 社区功能

## 常见错误

1. **不需要合作**：必须多人配合才能完成
2. **操作太复杂**：操作要简单
3. **时间太紧**：时间要合理
4. **没有反馈**：要有视觉和音效反馈
5. **失败太沮丧**：失败要有趣
