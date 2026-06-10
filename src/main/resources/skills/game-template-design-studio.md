---
name: 游戏设计工作室游戏开发模板
description: 游戏设计工作室游戏开发模板，适用于游戏编辑器、创作工具类游戏
trigger: design, 设计, editor, 编辑器, creative, 创作, sandbox, 沙盒, build, 建造, maker, 制作
examples: Mario Maker|LittleBigPlanet|Roblox|Dreams|Core
---

# 游戏设计工作室游戏开发模板

## 游戏设计核心原则

### 核心循环（持续进行）
```
设计关卡 → 测试游玩 → 调整优化 → 发布分享 → 获得反馈
```
- **创作自由**：玩家可以自由创作
- **即时反馈**：设计后立刻能玩
- **分享展示**：分享自己的作品

### 玩家心理学
- **"创作满足"的成就感**：创作自己的游戏
- **"即时测试"的乐趣**：设计后立刻能玩
- **"分享炫耀"的欲望**：展示自己的作品
- **"学习成长"的过程**：从新手到大师

### 设计工作室设计要点
```
设计工作室核心：
1. 可视化编辑：拖拽式操作
2. 即时预览：设计后立刻能玩
3. 丰富素材：提供足够素材
4. 分享系统：能分享作品
```

## 核心系统设计

### 1. 可视化编辑器
```javascript
class VisualEditor {
  constructor() {
    this.grid = new Grid(100, 100);
    this.selectedTool = 'select';
    this.selectedAsset = null;
    this.history = [];
    this.undoStack = [];
  }
  
  selectTool(tool) {
    this.selectedTool = tool;
  }
  
  selectAsset(asset) {
    this.selectedAsset = asset;
  }
  
  handleClick(x, y) {
    switch (this.selectedTool) {
      case 'select':
        this.selectObject(x, y);
        break;
      case 'place':
        this.placeObject(x, y, this.selectedAsset);
        break;
      case 'delete':
        this.deleteObject(x, y);
        break;
      case 'paint':
        this.paintTerrain(x, y);
        break;
    }
  }
  
  placeObject(x, y, asset) {
    const object = {
      id: this.generateId(),
      asset: asset,
      position: { x, y },
      properties: asset.defaultProperties
    };
    
    this.grid.set(x, y, object);
    this.history.push({ type: 'place', object });
    this.undoStack = [];
    
    this.render();
  }
  
  deleteObject(x, y) {
    const object = this.grid.get(x, y);
    if (object) {
      this.grid.remove(x, y);
      this.history.push({ type: 'delete', object });
      this.undoStack = [];
      
      this.render();
    }
  }
  
  undo() {
    if (this.history.length === 0) return;
    
    const action = this.history.pop();
    this.undoStack.push(action);
    
    switch (action.type) {
      case 'place':
        this.grid.remove(action.object.position.x, action.object.position.y);
        break;
      case 'delete':
        this.grid.set(action.object.position.x, action.object.position.y, action.object);
        break;
    }
    
    this.render();
  }
  
  redo() {
    if (this.undoStack.length === 0) return;
    
    const action = this.undoStack.pop();
    this.history.push(action);
    
    switch (action.type) {
      case 'place':
        this.grid.set(action.object.position.x, action.object.position.y, action.object);
        break;
      case 'delete':
        this.grid.remove(action.object.position.x, action.object.position.y);
        break;
    }
    
    this.render();
  }
}
```

### 2. 素材系统
```javascript
class AssetSystem {
  constructor() {
    this.assets = {};
    this.categories = ['terrain', 'objects', 'characters', 'effects', 'ui'];
  }
  
  addAsset(config) {
    this.assets[config.id] = {
      id: config.id,
      name: config.name,
      category: config.category,
      type: config.type, // sprite, tilemap, prefab
      url: config.url,
      properties: config.properties,
      defaultProperties: config.defaultProperties
    };
  }
  
  getAssetsByCategory(category) {
    return Object.values(this.assets).filter(a => a.category === category);
  }
  
  getAsset(assetId) {
    return this.assets[assetId];
  }
  
  searchAssets(query) {
    const lowerQuery = query.toLowerCase();
    return Object.values(this.assets).filter(a => 
      a.name.toLowerCase().includes(lowerQuery)
    );
  }
}

const DEFAULT_ASSETS = [
  { id: 'grass', name: '草地', category: 'terrain', type: 'tile' },
  { id: 'stone', name: '石头', category: 'terrain', type: 'tile' },
  { id: 'water', name: '水', category: 'terrain', type: 'tile' },
  { id: 'tree', name: '树', category: 'objects', type: 'sprite' },
  { id: 'house', name: '房子', category: 'objects', type: 'sprite' },
  { id: 'coin', name: '金币', category: 'objects', type: 'sprite' },
  { id: 'player', name: '玩家', category: 'characters', type: 'prefab' },
  { id: 'enemy', name: '敌人', category: 'characters', type: 'prefab' }
];
```

### 3. 逻辑编辑器
```javascript
class LogicEditor {
  constructor() {
    this.triggers = [];
    this.actions = [];
    this.connections = [];
  }
  
  addTrigger(config) {
    this.triggers.push({
      id: this.generateId(),
      type: config.type, // collision, timer, input, event
      condition: config.condition,
      position: config.position
    });
  }
  
  addAction(config) {
    this.actions.push({
      id: this.generateId(),
      type: config.type, // move, spawn, destroy, change, play
      parameters: config.parameters,
      position: config.position
    });
  }
  
  connect(triggerId, actionId) {
    this.connections.push({
      trigger: triggerId,
      action: actionId
    });
  }
  
  execute() {
    // 执行所有触发器
    for (const trigger of this.triggers) {
      if (this.checkTrigger(trigger)) {
        // 执行连接的动作
        const connectedActions = this.connections
          .filter(c => c.trigger === trigger.id)
          .map(c => this.actions.find(a => a.id === c.action));
        
        for (const action of connectedActions) {
          this.executeAction(action);
        }
      }
    }
  }
  
  checkTrigger(trigger) {
    switch (trigger.type) {
      case 'collision':
        return this.checkCollision(trigger.condition);
      case 'timer':
        return this.checkTimer(trigger.condition);
      case 'input':
        return this.checkInput(trigger.condition);
      case 'event':
        return this.checkEvent(trigger.condition);
      default:
        return false;
    }
  }
  
  executeAction(action) {
    switch (action.type) {
      case 'move':
        this.moveObject(action.parameters);
        break;
      case 'spawn':
        this.spawnObject(action.parameters);
        break;
      case 'destroy':
        this.destroyObject(action.parameters);
        break;
      case 'change':
        this.changeProperty(action.parameters);
        break;
      case 'play':
        this.playSound(action.parameters);
        break;
    }
  }
}
```

### 4. 测试系统
```javascript
class TestSystem {
  constructor() {
    this.isTesting = false;
    this.testState = {};
  }
  
  startTest() {
    this.isTesting = true;
    this.testState = {
      score: 0,
      lives: 3,
      timer: 0
    };
    
    // 初始化游戏世界
    this.initGameWorld();
    
    // 开始游戏循环
    this.startGameLoop();
  }
  
  stopTest() {
    this.isTesting = false;
    this.stopGameLoop();
  }
  
  initGameWorld() {
    // 从编辑器状态初始化游戏世界
    const editorState = this.editor.getState();
    
    for (const object of editorState.objects) {
      this.gameWorld.addObject(object);
    }
    
    // 设置玩家
    this.gameWorld.setPlayer(editorState.player);
  }
  
  update(delta) {
    if (!this.isTesting) return;
    
    this.testState.timer += delta;
    
    // 更新游戏世界
    this.gameWorld.update(delta);
    
    // 检查胜负条件
    this.checkWinLose();
  }
  
  checkWinLose() {
    // 检查胜利条件
    if (this.gameWorld.checkWinCondition()) {
      this.showWinScreen();
      this.stopTest();
    }
    
    // 检查失败条件
    if (this.gameWorld.checkLoseCondition()) {
      this.showLoseScreen();
      this.stopTest();
    }
  }
}
```

### 5. 发布系统
```javascript
class PublishSystem {
  constructor() {
    this.publishedLevels = [];
  }
  
  async publishLevel(levelData) {
    // 验证关卡
    if (!this.validateLevel(levelData)) {
      return { success: false, error: '关卡验证失败' };
    }
    
    // 上传关卡
    const response = await fetch('/api/levels', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(levelData)
    });
    
    const result = await response.json();
    
    if (result.success) {
      this.publishedLevels.push(result.level);
      return { success: true, level: result.level };
    }
    
    return { success: false, error: result.error };
  }
  
  validateLevel(levelData) {
    // 验证关卡是否有玩家起点
    if (!levelData.playerStart) return false;
    
    // 验证关卡是否有终点或目标
    if (!levelData.goal && !levelData.winCondition) return false;
    
    // 验证关卡大小
    if (levelData.objects.length < 5) return false;
    
    return true;
  }
  
  async getPublishedLevels(page = 1, limit = 20) {
    const response = await fetch(`/api/levels?page=${page}&limit=${limit}`);
    return await response.json();
  }
  
  async playLevel(levelId) {
    const response = await fetch(`/api/levels/${levelId}`);
    const level = await response.json();
    
    this.testSystem.loadLevel(level);
    this.testSystem.startTest();
  }
}
```

## 迭代策略

### 第一版：基础编辑器
- 简单编辑器
- 基础素材
- 拖拽放置
- 本地保存

### 第二版：素材系统
- 多种素材
- 素材分类
- 搜索功能
- 自定义素材

### 第三版：逻辑编辑器
- 触发器系统
- 动作系统
- 可视化连接
- 测试功能

### 第四版：发布系统
- 发布关卡
- 关卡浏览
- 点赞评论
- 排行榜

### 第五版：社区功能
- 关卡分享
- 创作比赛
- 社区互动
- 排行榜

## 常见错误

1. **编辑太复杂**：要简单易用
2. **素材太少**：要有足够素材
3. **不能测试**：要能即时测试
4. **不能分享**：要能分享作品
5. **没有教程**：要有新手教程
