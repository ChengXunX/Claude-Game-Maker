---
name: 益智游戏开发模板
description: 益智游戏开发模板，适用于解谜、逻辑、益智类游戏
trigger: puzzle, 益智, 解谜, 逻辑, 数独, 推箱子, 华容道
examples: 数独|推箱子|华容道|2048|俄罗斯方块|Baba Is You|The Witness
---

# 益智游戏开发模板

## 游戏设计核心原则

### 核心循环（每关 1-10 分钟）
```
观察谜题 → 思考解法 → 尝试操作 → 验证结果 → 解开/重试
```
- **逻辑性**：谜题有唯一解或多种解法
- **渐进性**：从简单到复杂
- **满足感**：解开谜题的成就感

### 玩家心理学
- **"恍然大悟"的快感**：突然理解解法的瞬间
- **"挑战成功"的成就感**：解开难题的满足感
- **"逻辑推理"的乐趣**：思考过程本身就是乐趣
- **"收集完美"的欲望**：全关卡三星通关

### 谜题设计要点
```
谜题设计原则：
1. 规则简单：3 秒理解规则
2. 解法明确：玩家知道目标是什么
3. 难度渐进：从简单到复杂
4. 无歧义：只有一种正确理解
5. 可验证：玩家知道是否正确
```

## 核心系统设计

### 1. 谜题数据结构
```javascript
class Puzzle {
  constructor(config) {
    this.id = config.id;
    this.type = config.type;
    this.grid = config.grid; // 网格数据
    this.solution = config.solution; // 解法
    this.moves = 0;
    this.maxMoves = config.maxMoves;
    this.stars = 0;
  }
  
  checkSolution() {
    // 检查当前状态是否匹配解法
    return JSON.stringify(this.grid) === JSON.stringify(this.solution);
  }
  
  getStars() {
    if (!this.checkSolution()) return 0;
    if (this.moves <= this.maxMoves * 0.5) return 3;
    if (this.moves <= this.maxMoves * 0.75) return 2;
    return 1;
  }
}
```

### 2. 推箱子系统
```javascript
class Sokoban {
  constructor(level) {
    this.grid = level.grid;
    this.player = level.player;
    this.boxes = level.boxes;
    this.goals = level.goals;
    this.moves = 0;
  }
  
  move(dx, dy) {
    const newX = this.player.x + dx;
    const newY = this.player.y + dy;
    
    // 检查是否可以移动
    if (!this.canMove(newX, newY)) return false;
    
    // 检查是否有箱子
    const box = this.getBoxAt(newX, newY);
    if (box) {
      // 推箱子
      const boxNewX = newX + dx;
      const boxNewY = newY + dy;
      
      if (!this.canMoveBox(box, boxNewX, boxNewY)) return false;
      
      box.x = boxNewX;
      box.y = boxNewY;
    }
    
    // 移动玩家
    this.player.x = newX;
    this.player.y = newY;
    this.moves++;
    
    return true;
  }
  
  canMove(x, y) {
    // 检查是否在网格内
    if (x < 0 || x >= this.grid[0].length || y < 0 || y >= this.grid.length) return false;
    // 检查是否是墙
    if (this.grid[y][x] === '#') return false;
    return true;
  }
  
  canMoveBox(box, x, y) {
    if (!this.canMove(x, y)) return false;
    // 检查是否有其他箱子
    if (this.getBoxAt(x, y)) return false;
    return true;
  }
  
  isComplete() {
    // 检查所有箱子是否在目标位置
    return this.boxes.every(box => 
      this.goals.some(goal => goal.x === box.x && goal.y === box.y)
    );
  }
}
```

### 3. 数独系统
```javascript
class Sudoku {
  constructor(puzzle) {
    this.grid = puzzle.map(row => [...row]);
    this.original = puzzle.map(row => [...row]);
  }
  
  setCell(x, y, value) {
    // 不能修改原始数字
    if (this.original[y][x] !== 0) return false;
    
    // 检查是否合法
    if (!this.isValid(x, y, value)) return false;
    
    this.grid[y][x] = value;
    return true;
  }
  
  isValid(x, y, value) {
    // 检查行
    for (let i = 0; i < 9; i++) {
      if (i !== x && this.grid[y][i] === value) return false;
    }
    
    // 检查列
    for (let i = 0; i < 9; i++) {
      if (i !== y && this.grid[i][x] === value) return false;
    }
    
    // 检查 3x3 宫格
    const boxX = Math.floor(x / 3) * 3;
    const boxY = Math.floor(y / 3) * 3;
    for (let dy = 0; dy < 3; dy++) {
      for (let dx = 0; dx < 3; dx++) {
        const nx = boxX + dx;
        const ny = boxY + dy;
        if (nx !== x && ny !== y && this.grid[ny][nx] === value) return false;
      }
    }
    
    return true;
  }
  
  isComplete() {
    return this.grid.every(row => row.every(cell => cell !== 0));
  }
}
```

### 4. 2048 系统
```javascript
class Game2048 {
  constructor() {
    this.grid = Array(4).fill(null).map(() => Array(4).fill(0));
    this.score = 0;
    this.addRandomTile();
    this.addRandomTile();
  }
  
  move(direction) {
    let moved = false;
    
    switch (direction) {
      case 'up':
        moved = this.moveUp();
        break;
      case 'down':
        moved = this.moveDown();
        break;
      case 'left':
        moved = this.moveLeft();
        break;
      case 'right':
        moved = this.moveRight();
        break;
    }
    
    if (moved) {
      this.addRandomTile();
    }
    
    return moved;
  }
  
  moveLeft() {
    let moved = false;
    for (let y = 0; y < 4; y++) {
      for (let x = 1; x < 4; x++) {
        if (this.grid[y][x] !== 0) {
          let newX = x;
          while (newX > 0 && this.grid[y][newX - 1] === 0) {
            newX--;
          }
          if (newX !== x) {
            this.grid[y][newX] = this.grid[y][x];
            this.grid[y][x] = 0;
            moved = true;
          }
          if (newX > 0 && this.grid[y][newX - 1] === this.grid[y][newX]) {
            this.grid[y][newX - 1] *= 2;
            this.score += this.grid[y][newX - 1];
            this.grid[y][newX] = 0;
            moved = true;
          }
        }
      }
    }
    return moved;
  }
  
  addRandomTile() {
    const empty = [];
    for (let y = 0; y < 4; y++) {
      for (let x = 0; x < 4; x++) {
        if (this.grid[y][x] === 0) {
          empty.push({ x, y });
        }
      }
    }
    
    if (empty.length === 0) return;
    
    const { x, y } = empty[Math.floor(Math.random() * empty.length)];
    this.grid[y][x] = Math.random() < 0.9 ? 2 : 4;
  }
  
  isGameOver() {
    // 检查是否还有空格
    for (let y = 0; y < 4; y++) {
      for (let x = 0; x < 4; x++) {
        if (this.grid[y][x] === 0) return false;
      }
    }
    
    // 检查是否还能合并
    for (let y = 0; y < 4; y++) {
      for (let x = 0; x < 3; x++) {
        if (this.grid[y][x] === this.grid[y][x + 1]) return false;
      }
    }
    for (let x = 0; x < 4; x++) {
      for (let y = 0; y < 3; y++) {
        if (this.grid[y][x] === this.grid[y + 1][x]) return false;
      }
    }
    
    return true;
  }
}
```

## 迭代策略

### 第一版：核心谜题
- 1 种谜题类型
- 10 个关卡
- 基础 UI
- 步数统计

### 第二版：多种谜题
- 3 种谜题类型
- 30 个关卡
- 星级评价
- 提示系统

### 第三版：内容扩展
- 5 种谜题类型
- 100 个关卡
- 成就系统
- 排行榜

### 第四版：深度玩法
- 关卡编辑器
- 关卡分享
- 每日挑战
- 特殊事件

### 第五版：变现
- 看广告获得提示
- 内购系统
- 社交分享
- 推送通知

## 常见错误

1. **规则太复杂**：益智游戏规则要简单
2. **难度跳跃**：难度要渐进，不能突然变难
3. **没有提示**：玩家卡住时要有提示
4. **没有撤销**：误操作要能撤销
5. **没有反馈**：操作要有视觉和音效反馈
