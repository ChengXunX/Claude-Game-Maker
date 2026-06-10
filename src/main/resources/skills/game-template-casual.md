---
name: 休闲游戏开发模板
description: 休闲游戏开发模板，适用于简单易上手的休闲小游戏
trigger: 休闲游戏,casual game,简单游戏,小游戏,弹球,打砖块,Flappy Bird,2048
examples: Flappy Bird|2048|打砖块|贪吃蛇|俄罗斯方块|羊了个羊
---

# 休闲游戏开发模板

## 游戏设计核心原则

### 核心循环（每局 10-60 秒）
```
开始 → 操作 → 反馈 → 失败/成功 → 再来一局
```
- **3 秒理解规则**：不需要教程，看一眼就知道怎么玩
- **单手操作**：点击/滑动即可，适合碎片时间
- **即时死亡**：失败立刻重来，没有loading

### 玩家心理学
- **"再来一局"冲动**：失败后觉得"这次我能做得更好"
- **渐进难度**：从简单到困难，让玩家有成长感
- **社交分享**：高分截图分享，激发竞争欲
- **收集解锁**：新皮肤、新角色激发收集欲

### 成瘾性设计公式
```
1. 每局很短（10-60秒），降低"再玩一局"的心理门槛
2. 失败后立刻重来，不要有loading screen
3. 显示"最高分"，激发超越欲望
4. 偶尔给"差一点就成功"的感觉
5. 每隔几局给一个小奖励（新皮肤/新道具）
```

## 核心系统设计

### 1. 游戏状态机
```javascript
class GameStateMachine {
  constructor() {
    this.states = {
      MENU: { enter: this.enterMenu, update: null, exit: null },
      PLAYING: { enter: this.enterPlaying, update: this.updatePlaying, exit: null },
      GAME_OVER: { enter: this.enterGameOver, update: null, exit: null }
    };
    this.currentState = null;
  }
  
  changeState(newState) {
    if (this.currentState) {
      this.states[this.currentState].exit?.();
    }
    this.currentState = newState;
    this.states[newState].enter?.();
  }
  
  enterMenu() {
    // 显示菜单，隐藏游戏元素
    this.showMenu();
    this.hideGame();
  }
  
  enterPlaying() {
    // 重置游戏状态，开始游戏
    this.resetGame();
    this.hideMenu();
    this.showGame();
    this.score = 0;
  }
  
  enterGameOver() {
    // 显示分数，显示"再来一局"按钮
    this.showGameOver();
    this.saveHighScore();
  }
}
```

### 2. 分数系统
```javascript
class ScoreSystem {
  constructor() {
    this.score = 0;
    this.highScore = localStorage.getItem('highScore') || 0;
    this.combo = 0;
  }
  
  addScore(amount) {
    this.combo++;
    const bonus = Math.floor(amount * (1 + this.combo * 0.1));
    this.score += bonus;
    this.showScorePopup(bonus);
    this.checkAchievements();
  }
  
  resetCombo() {
    this.combo = 0;
  }
  
  saveHighScore() {
    if (this.score > this.highScore) {
      this.highScore = this.score;
      localStorage.setItem('highScore', this.highScore);
      this.showNewHighScore();
    }
  }
}
```

### 3. 难度渐进
```javascript
class DifficultyManager {
  constructor() {
    this.level = 1;
    this.score = 0;
    this.thresholds = [100, 300, 600, 1000, 1500, 2100, 2800, 3600, 4500, 5500];
  }
  
  update(score) {
    this.score = score;
    const newLevel = this.thresholds.findIndex(t => score < t) + 1;
    if (newLevel > this.level) {
      this.level = newLevel;
      this.onLevelUp();
    }
  }
  
  getSpeed() {
    return 1 + (this.level - 1) * 0.15; // 每级速度+15%
  }
  
  getSpawnRate() {
    return Math.max(500, 2000 - (this.level - 1) * 150); // 每级减少150ms
  }
  
  onLevelUp() {
    // 显示升级提示
    this.showLevelUpEffect();
    // 播放升级音效
    this.playLevelUpSound();
  }
}
```

## 关键技术实现

### 对象池（性能关键）
```javascript
class ObjectPool {
  constructor(createFn, resetFn, initialSize = 20) {
    this.createFn = createFn;
    this.resetFn = resetFn;
    this.pool = [];
    
    // 预创建对象
    for (let i = 0; i < initialSize; i++) {
      this.pool.push(createFn());
    }
  }
  
  get() {
    if (this.pool.length > 0) {
      return this.pool.pop();
    }
    return this.createFn();
  }
  
  release(obj) {
    this.resetFn(obj);
    this.pool.push(obj);
  }
}
```

### 屏幕适配
```javascript
function resizeGame() {
  const canvas = document.querySelector('canvas');
  const windowWidth = window.innerWidth;
  const windowHeight = window.innerHeight;
  const gameRatio = GAME_WIDTH / GAME_HEIGHT;
  const windowRatio = windowWidth / windowHeight;
  
  if (windowRatio < gameRatio) {
    canvas.style.width = windowWidth + 'px';
    canvas.style.height = (windowWidth / gameRatio) + 'px';
  } else {
    canvas.style.height = windowHeight + 'px';
    canvas.style.width = (windowHeight * gameRatio) + 'px';
  }
}
```

## 迭代策略

### 第一版：核心玩法验证
- 最基本的游戏机制
- 单一操作方式
- 简单计分
- 测试"好不好玩"

### 第二版：手感优化
- 优化操作响应
- 添加音效和震动
- 优化失败反馈
- 添加"再来一局"

### 第三版：内容扩展
- 添加难度渐进
- 添加成就系统
- 添加新皮肤/角色
- 添加排行榜

### 第四版：变现和传播
- 添加分享功能
- 添加广告（看广告复活）
- 添加每日挑战
- 添加社交功能

### 第五版：打磨
- 优化视觉效果
- 优化性能
- 添加新手引导
- 平衡难度曲线

## 常见错误

1. **太复杂**：休闲游戏必须简单，3秒看不懂就失败了
2. **没有"再来一局"冲动**：失败后要让玩家觉得"差一点就成功"
3. **loading太多**：休闲游戏要快，不要有loading screen
4. **没有最高分**：最高分是休闲游戏的核心驱动力
5. **操作不灵敏**：点击/滑动必须立刻响应，延迟会毁掉体验
