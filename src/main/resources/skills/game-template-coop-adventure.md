---
name: 双人合作闯关游戏开发模板
description: 双人合作闯关游戏开发模板，适用于双人合作、协作解谜类游戏
trigger: coop, 合作, adventure, 冒险, 闯关, 双人, two player, 合作闯关
examples: 双人成行|传送门骑士|胡闹厨房|Overcooked|It Takes Two
---

# 双人合作闯关游戏开发模板

## 游戏设计核心原则

### 核心循环（每关 5-15 分钟）
```
了解目标 → 分工合作 → 互相帮助 → 克服障碍 → 通关
```
- **合作性**：必须两个人配合才能通关
- **互补性**：两个人有不同能力
- **沟通性**：需要沟通才能成功

### 玩家心理学
- **"合作成功"的成就感**：和朋友一起通关的满足感
- **"互相帮助"的温暖感**：帮助朋友的快乐
- **"默契配合"的快感**：完美配合的爽感
- **"搞笑失败"的乐趣**：失败也很有趣

### 合作设计要点
```
合作核心：
1. 互补能力：两个人有不同能力
2. 必须配合：单人无法通关
3. 沟通重要：需要沟通才能成功
4. 容错设计：失败不会太沮丧
```

## 核心系统设计

### 1. 双人控制系统
```javascript
class CoopControlSystem {
  constructor() {
    this.player1 = {
      controls: { up: 'W', down: 'S', left: 'A', right: 'D', action: 'E' },
      position: { x: 0, y: 0 },
      abilities: ['push', 'pull']
    };
    
    this.player2 = {
      controls: { up: 'ArrowUp', down: 'ArrowDown', left: 'ArrowLeft', right: 'ArrowRight', action: 'Space' },
      position: { x: 100, y: 0 },
      abilities: ['jump', 'glide']
    };
  }
  
  update(delta) {
    this.updatePlayer(this.player1, delta);
    this.updatePlayer(this.player2, delta);
    this.checkInteractions();
  }
  
  updatePlayer(player, delta) {
    const moveDir = this.getMoveDirection(player.controls);
    if (moveDir) {
      player.position.x += moveDir.x * player.speed * delta;
      player.position.y += moveDir.y * player.speed * delta;
    }
    
    if (this.isKeyPressed(player.controls.action)) {
      this.performAction(player);
    }
  }
  
  checkInteractions() {
    // 检查玩家之间的互动
    const dist = this.getDistance(this.player1.position, this.player2.position);
    
    if (dist < 50) {
      // 近距离互动
      this.showInteractionPrompt();
    }
  }
}
```

### 2. 互补能力系统
```javascript
class AbilitySystem {
  constructor() {
    this.player1Abilities = {
      push: { name: '推动', description: '推动重物' },
      pull: { name: '拉动', description: '拉动重物' },
      carry: { name: '搬运', description: '搬运物品' }
    };
    
    this.player2Abilities = {
      jump: { name: '跳跃', description: '高跳' },
      glide: { name: '滑翔', description: '空中滑翔' },
      fly: { name: '飞行', description: '短暂飞行' }
    };
  }
  
  useAbility(player, abilityName) {
    const abilities = player === 1 ? this.player1Abilities : this.player2Abilities;
    const ability = abilities[abilityName];
    
    if (!ability) return false;
    
    // 执行能力
    ability.execute();
    
    return true;
  }
}
```

### 3. 合作谜题系统
```javascript
class CoopPuzzleSystem {
  constructor() {
    this.puzzles = [];
  }
  
  addPuzzle(config) {
    this.puzzles.push({
      id: config.id,
      type: config.type,
      requirements: config.requirements,
      solved: false
    });
  }
  
  checkPuzzle(puzzleId, player1State, player2State) {
    const puzzle = this.puzzles.find(p => p.id === puzzleId);
    if (!puzzle) return false;
    
    // 检查是否满足所有要求
    for (const req of puzzle.requirements) {
      if (!this.checkRequirement(req, player1State, player2State)) {
        return false;
      }
    }
    
    puzzle.solved = true;
    return true;
  }
  
  checkRequirement(req, player1State, player2State) {
    switch (req.type) {
      case 'player1_at':
        return this.isAtPosition(player1State, req.position);
      case 'player2_at':
        return this.isAtPosition(player2State, req.position);
      case 'both_at':
        return this.isAtPosition(player1State, req.position1) &&
               this.isAtPosition(player2State, req.position2);
      case 'player1_action':
        return player1State.lastAction === req.action;
      case 'player2_action':
        return player2State.lastAction === req.action;
      default:
        return false;
    }
  }
}

const PUZZLES = [
  {
    id: 'pressure_plate',
    type: 'coop',
    requirements: [
      { type: 'player1_at', position: { x: 100, y: 200 } },
      { type: 'player2_at', position: { x: 300, y: 200 } }
    ]
  },
  {
    id: 'throw_switch',
    type: 'sequential',
    requirements: [
      { type: 'player1_action', action: 'switch' },
      { type: 'player2_action', action: 'jump', delay: 2 }
    ]
  }
];
```

### 4. 通信系统
```javascript
class CommunicationSystem {
  constructor() {
    this.signals = {};
    this.quickMessages = [
      { id: 'help', text: '帮我！', icon: '🆘' },
      { id: 'come', text: '过来', icon: '👉' },
      { id: 'wait', text: '等等', icon: '✋' },
      { id: 'go', text: '走！', icon: '🏃' },
      { id: 'yes', text: '好的', icon: '👍' },
      { id: 'no', text: '不行', icon: '👎' }
    ];
  }
  
  sendSignal(fromPlayer, signalId) {
    const signal = this.quickMessages.find(s => s.id === signalId);
    if (!signal) return;
    
    this.signals[fromPlayer] = {
      ...signal,
      timestamp: Date.now()
    };
    
    this.showSignal(fromPlayer, signal);
  }
  
  showSignal(fromPlayer, signal) {
    // 在玩家头顶显示信号
    const player = fromPlayer === 1 ? this.player1 : this.player2;
    this.createFloatingText(player.position, signal.icon);
  }
}
```

### 5. 视角系统
```javascript
class CameraSystem {
  constructor() {
    this.mode = 'split'; // split, shared, follow
    this.splitScreen = true;
  }
  
  update(delta) {
    if (this.mode === 'split') {
      this.updateSplitScreen();
    } else if (this.mode === 'shared') {
      this.updateSharedScreen();
    }
  }
  
  updateSplitScreen() {
    // 分屏模式
    const midX = (this.player1.position.x + this.player2.position.x) / 2;
    
    this.camera1.setCenter(this.player1.position);
    this.camera2.setCenter(this.player2.position);
    
    // 分割线
    this.splitLine.setPosition(midX, 0);
  }
  
  updateSharedScreen() {
    // 共享屏幕模式
    const center = {
      x: (this.player1.position.x + this.player2.position.x) / 2,
      y: (this.player1.position.y + this.player2.position.y) / 2
    };
    
    const distance = this.getDistance(this.player1.position, this.player2.position);
    const zoom = Math.max(0.5, 1 - distance / 1000);
    
    this.camera.setCenter(center);
    this.camera.setZoom(zoom);
  }
}
```

## 迭代策略

### 第一版：基础合作
- 双人控制
- 简单关卡
- 基础物理
- 简单 UI

### 第二版：互补能力
- 不同能力
- 合作谜题
- 多个关卡
- 计分系统

### 第三版：深度合作
- 多种谜题
- 通信系统
- 成就系统
- 排行榜

### 第四版：内容扩展
- 20 个关卡
- 特殊机制
- 隐藏关卡
- 收集系统

### 第五版：多人模式
- 在线合作
- 房间系统
- 聊天系统
- 社区功能

## 常见错误

1. **不需要合作**：必须两个人配合才能通关
2. **能力太相似**：两个人要有不同能力
3. **没有沟通**：要有沟通系统
4. **失败太沮丧**：失败要有趣，不能太沮丧
5. **视角问题**：分屏或共享屏幕要处理好
