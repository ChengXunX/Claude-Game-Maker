---
name: 螺丝解谜游戏开发模板
description: 螺丝解谜游戏开发模板，适用于物理解谜、拆卸、分类类游戏
trigger: screw, 螺丝, bolt, 解谜, puzzle, 拆卸, 分类, 物理
examples: 螺丝解谜|拆卸游戏|分类游戏|物理益智
---

# 螺丝解谜游戏开发模板

## 游戏设计核心原则

### 核心循环（每关 1-5 分钟）
```
观察结构 → 选择螺丝 → 拆卸 → 分类收集 → 完成
```
- **物理解谜**：物理引擎驱动
- **策略性**：选择拆卸顺序
- **满足感**：成功拆卸的满足感

### 玩家心理学
- **"拆卸快感"**：拆东西的满足感
- **"分类整理"的乐趣**：整理的快感
- **"物理解谜"的挑战**：思考拆卸顺序
- **"完成成就"的满足感**：完成关卡的成就感

### 螺丝解谜设计要点
```
螺丝解谜核心：
1. 物理真实：物理引擎要真实
2. 顺序重要：拆卸顺序很重要
3. 分类收集：按颜色/类型分类
4. 难度渐进：从简单到复杂
```

## 核心系统设计

### 1. 螺丝系统
```javascript
class Screw {
  constructor(config) {
    this.id = config.id;
    this.type = config.type; // phillips, flat, hex, torx
    this.color = config.color;
    this.size = config.size;
    this.position = config.position;
    this.rotation = 0;
    this.tightness = config.tightness || 100;
    this.removed = false;
  }
  
  unscrew(tool) {
    if (this.removed) return false;
    
    // 检查工具是否匹配
    if (tool.type !== this.type) {
      return { success: false, error: '工具不匹配' };
    }
    
    // 检查工具尺寸
    if (tool.size !== this.size) {
      return { success: false, error: '工具尺寸不匹配' };
    }
    
    // 旋转螺丝
    this.rotation += 10;
    this.tightness -= 10;
    
    // 检查是否完全松开
    if (this.tightness <= 0) {
      this.removed = true;
      return { success: true, removed: true };
    }
    
    return { success: true, removed: false };
  }
}

const SCREW_TYPES = {
  phillips: { name: '十字', icon: '✚' },
  flat: { name: '一字', icon: '━' },
  hex: { name: '六角', icon: '⬡' },
  torx: { name: '梅花', icon: '✦' }
};
```

### 2. 工具系统
```javascript
class ToolSystem {
  constructor() {
    this.tools = [];
    this.selectedTool = null;
  }
  
  addTool(config) {
    this.tools.push({
      id: config.id,
      type: config.type,
      size: config.size,
      name: config.name
    });
  }
  
  selectTool(toolId) {
    this.selectedTool = this.tools.find(t => t.id === toolId);
  }
  
  useTool(screw) {
    if (!this.selectedTool) return { success: false, error: '未选择工具' };
    
    return screw.unscrew(this.selectedTool);
  }
}

const TOOLS = [
  { id: 'phillips_s', name: '十字螺丝刀(小)', type: 'phillips', size: 'small' },
  { id: 'phillips_m', name: '十字螺丝刀(中)', type: 'phillips', size: 'medium' },
  { id: 'phillips_l', name: '十字螺丝刀(大)', type: 'phillips', size: 'large' },
  { id: 'flat_s', name: '一字螺丝刀(小)', type: 'flat', size: 'small' },
  { id: 'hex_s', name: '六角扳手(小)', type: 'hex', size: 'small' },
  { id: 'torx_s', name: '梅花螺丝刀(小)', type: 'torx', size: 'small' }
];
```

### 3. 物理系统
```javascript
class PhysicsSystem {
  constructor() {
    this.gravity = 9.8;
    this.objects = [];
  }
  
  addObject(object) {
    this.objects.push({
      ...object,
      velocity: { x: 0, y: 0 },
      angularVelocity: 0,
      grounded: false
    });
  }
  
  update(delta) {
    for (const object of this.objects) {
      if (object.removed) continue;
      
      // 重力
      if (!object.grounded) {
        object.velocity.y += this.gravity * delta;
      }
      
      // 更新位置
      object.position.x += object.velocity.x * delta;
      object.position.y += object.velocity.y * delta;
      
      // 碰撞检测
      this.checkCollisions(object);
    }
  }
  
  checkCollisions(object) {
    for (const other of this.objects) {
      if (other === object || other.removed) continue;
      
      if (this.intersects(object, other)) {
        this.resolveCollision(object, other);
      }
    }
  }
  
  intersects(a, b) {
    // 简单的 AABB 碰撞检测
    return a.position.x < b.position.x + b.width &&
           a.position.x + a.width > b.position.x &&
           a.position.y < b.position.y + b.height &&
           a.position.y + a.height > b.position.y;
  }
  
  resolveCollision(a, b) {
    // 简单的碰撞响应
    const overlapX = Math.min(a.position.x + a.width - b.position.x, b.position.x + b.width - a.position.x);
    const overlapY = Math.min(a.position.y + a.height - b.position.y, b.position.y + b.height - a.position.y);
    
    if (overlapX < overlapY) {
      a.position.x += overlapX / 2;
      b.position.x -= overlapX / 2;
      a.velocity.x *= -0.5;
      b.velocity.x *= -0.5;
    } else {
      a.position.y += overlapY / 2;
      b.position.y -= overlapY / 2;
      a.velocity.y *= -0.5;
      b.velocity.y *= -0.5;
    }
  }
}
```

### 4. 收集系统
```javascript
class CollectionSystem {
  constructor() {
    this.bins = {};
    this.collected = {};
  }
  
  addBin(config) {
    this.bins[config.id] = {
      id: config.id,
      color: config.color,
      type: config.type,
      count: 0,
      maxCount: config.maxCount
    };
  }
  
  collectScrew(screw) {
    const binId = this.findMatchingBin(screw);
    
    if (!binId) return { success: false, error: '没有匹配的收集箱' };
    
    const bin = this.bins[binId];
    
    if (bin.count >= bin.maxCount) {
      return { success: false, error: '收集箱已满' };
    }
    
    bin.count++;
    this.collected[screw.id] = binId;
    
    return { success: true, binId };
  }
  
  findMatchingBin(screw) {
    for (const [binId, bin] of Object.entries(this.bins)) {
      if (bin.color === screw.color || bin.type === screw.type) {
        return binId;
      }
    }
    return null;
  }
  
  isComplete() {
    return Object.values(this.bins).every(bin => bin.count >= bin.maxCount);
  }
  
  getScore() {
    let score = 0;
    for (const bin of Object.values(this.bins)) {
      score += bin.count * 100;
    }
    return score;
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
  
  loadLevel(levelId) {
    const config = this.levels[levelId - 1];
    
    // 创建螺丝
    for (const screwConfig of config.screws) {
      this.screwSystem.addScrew(screwConfig);
    }
    
    // 创建工具
    for (const toolConfig of config.tools) {
      this.toolSystem.addTool(toolConfig);
    }
    
    // 创建收集箱
    for (const binConfig of config.bins) {
      this.collectionSystem.addBin(binConfig);
    }
  }
  
  checkLevelComplete() {
    return this.collectionSystem.isComplete();
  }
  
  advanceLevel() {
    this.currentLevel++;
    this.loadLevel(this.getLevelConfig());
  }
}

const LEVELS = [
  {
    level: 1,
    name: '入门',
    screws: [
      { id: 1, type: 'phillips', color: 'red', size: 'small', position: { x: 100, y: 200 } },
      { id: 2, type: 'phillips', color: 'red', size: 'small', position: { x: 200, y: 200 } },
      { id: 3, type: 'phillips', color: 'red', size: 'small', position: { x: 300, y: 200 } }
    ],
    tools: [{ id: 'phillips_s', type: 'phillips', size: 'small' }],
    bins: [{ id: 'red', color: 'red', maxCount: 3 }]
  },
  {
    level: 2,
    name: '双色',
    screws: [
      { id: 1, type: 'phillips', color: 'red', size: 'small', position: { x: 100, y: 200 } },
      { id: 2, type: 'phillips', color: 'blue', size: 'small', position: { x: 200, y: 200 } },
      { id: 3, type: 'phillips', color: 'red', size: 'small', position: { x: 300, y: 200 } },
      { id: 4, type: 'phillips', color: 'blue', size: 'small', position: { x: 400, y: 200 } }
    ],
    tools: [{ id: 'phillips_s', type: 'phillips', size: 'small' }],
    bins: [
      { id: 'red', color: 'red', maxCount: 2 },
      { id: 'blue', color: 'blue', maxCount: 2 }
    ]
  }
];
```

## 迭代策略

### 第一版：基础解谜
- 简单螺丝
- 基础工具
- 物理引擎
- 简单 UI

### 第二版：多种类型
- 多种螺丝类型
- 多种工具
- 颜色分类
- 计分系统

### 第三版：物理解谜
- 复杂结构
- 物理互动
- 多种收集箱
- 计时系统

### 第四版：深度玩法
- 20 个关卡
- 特殊螺丝
- 成就系统
- 排行榜

### 第五版：变现
- 看广告获得提示
- 内购系统
- 社交分享
- 推送通知

## 常见错误

1. **物理太假**：物理引擎要真实
2. **工具不匹配**：要提示工具不匹配
3. **没有反馈**：拆卸要有视觉和音效反馈
4. **难度太陡**：难度要渐进
5. **没有收集**：要有收集系统
