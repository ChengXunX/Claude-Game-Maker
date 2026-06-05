---
name: game-template-puzzle
description: 益智游戏模板 - 提供完整的益智游戏项目骨架
category: game-template
triggerPattern: puzzle, match-3, 益智, 消除, 三消
---

# 益智游戏模板

## 概述

这是一个完整的三消益智游戏模板，基于 Phaser 3 引擎。包含：
- 棋盘系统（8x8 网格）
- 宝石系统（多种宝石类型）
- 消除系统（三消、四消、五消）
- 特殊宝石（炸弹、闪电、彩虹）
- 关卡系统（目标、步数限制）
- 分数系统（连击加成）

## 项目结构

```
game-project/
├── index.html
├── package.json
├── vite.config.js
├── src/
│   ├── main.js
│   ├── config.js
│   ├── scenes/
│   │   ├── BootScene.js
│   │   ├── MenuScene.js
│   │   ├── GameScene.js
│   │   └── LevelSelectScene.js
│   ├── objects/
│   │   ├── Gem.js            # 宝石类
│   │   ├── Board.js          # 棋盘类
│   │   └── SpecialGem.js     # 特殊宝石
│   ├── systems/
│   │   ├── MatchSystem.js    # 匹配系统
│   │   ├── CascadeSystem.js  # 连锁系统
│   │   └── ScoreSystem.js    # 分数系统
│   ├── data/
│   │   └── levels.js         # 关卡数据
│   └── ui/
│       ├── HUD.js
│       └── LevelComplete.js
└── assets/
    ├── images/
    └── sounds/
```

## 核心代码模板

### 1. 棋盘系统 (Board.js)

```javascript
export class Board {
  constructor(scene, x, y, rows, cols) {
    this.scene = scene
    this.x = x
    this.y = y
    this.rows = rows
    this.cols = cols
    this.cellSize = 64
    this.grid = []
    this.selectedGem = null
    this.isProcessing = false

    this.init()
  }

  init() {
    // 创建网格
    for (let row = 0; row < this.rows; row++) {
      this.grid[row] = []
      for (let col = 0; col < this.cols; col++) {
        this.grid[row][col] = null
      }
    }

    // 填充宝石
    this.fillBoard()

    // 设置输入
    this.setupInput()
  }

  fillBoard() {
    for (let row = 0; row < this.rows; row++) {
      for (let col = 0; col < this.cols; col++) {
        if (!this.grid[row][col]) {
          this.createGemAt(row, col)
        }
      }
    }

    // 确保没有初始匹配
    while (this.findMatches().length > 0) {
      this.shuffleBoard()
    }
  }

  createGemAt(row, col, type = null) {
    if (!type) {
      type = this.getRandomGemType()
    }

    const x = this.x + col * this.cellSize + this.cellSize / 2
    const y = this.y + row * this.cellSize + this.cellSize / 2

    const gem = new Gem(this.scene, x, y, type)
    gem.setGridPosition(row, col)

    this.grid[row][col] = gem
    return gem
  }

  getRandomGemType() {
    const types = ['red', 'blue', 'green', 'yellow', 'purple', 'orange']
    return types[Math.floor(Math.random() * types.length)]
  }

  setupInput() {
    this.scene.input.on('pointerdown', this.onPointerDown, this)
    this.scene.input.on('pointermove', this.onPointerMove, this)
    this.scene.input.on('pointerup', this.onPointerUp, this)
  }

  onPointerDown(pointer) {
    if (this.isProcessing) return

    const col = Math.floor((pointer.x - this.x) / this.cellSize)
    const row = Math.floor((pointer.y - this.y) / this.cellSize)

    if (row >= 0 && row < this.rows && col >= 0 && col < this.cols) {
      this.selectedGem = this.grid[row][col]
      if (this.selectedGem) {
        this.selectedGem.select()
      }
    }
  }

  onPointerMove(pointer) {
    if (!this.selectedGem || this.isProcessing) return

    const col = Math.floor((pointer.x - this.x) / this.cellSize)
    const row = Math.floor((pointer.y - this.y) / this.cellSize)

    if (row >= 0 && row < this.rows && col >= 0 && col < this.cols) {
      const targetGem = this.grid[row][col]
      if (targetGem && targetGem !== this.selectedGem) {
        this.trySwap(this.selectedGem, targetGem)
      }
    }
  }

  onPointerUp() {
    if (this.selectedGem) {
      this.selectedGem.deselect()
      this.selectedGem = null
    }
  }

  async trySwap(gem1, gem2) {
    this.isProcessing = true
    this.selectedGem = null

    // 执行交换
    await this.swapGems(gem1, gem2)

    // 检查匹配
    const matches = this.findMatches()
    if (matches.length > 0) {
      await this.processMatches(matches)
    } else {
      // 没有匹配，交换回来
      await this.swapGems(gem1, gem2)
    }

    this.isProcessing = false
  }

  async swapGems(gem1, gem2) {
    const row1 = gem1.row
    const col1 = gem1.col
    const row2 = gem2.row
    const col2 = gem2.col

    // 更新网格
    this.grid[row1][col1] = gem2
    this.grid[row2][col2] = gem1

    // 更新宝石位置
    gem1.setGridPosition(row2, col2)
    gem2.setGridPosition(row1, col1)

    // 动画
    await this.animateSwap(gem1, gem2)
  }

  findMatches() {
    const matches = []

    // 横向匹配
    for (let row = 0; row < this.rows; row++) {
      for (let col = 0; col < this.cols - 2; col++) {
        const gem1 = this.grid[row][col]
        const gem2 = this.grid[row][col + 1]
        const gem3 = this.grid[row][col + 2]

        if (gem1 && gem2 && gem3 && gem1.type === gem2.type && gem2.type === gem3.type) {
          matches.push([gem1, gem2, gem3])
        }
      }
    }

    // 纵向匹配
    for (let col = 0; col < this.cols; col++) {
      for (let row = 0; row < this.rows - 2; row++) {
        const gem1 = this.grid[row][col]
        const gem2 = this.grid[row + 1][col]
        const gem3 = this.grid[row + 2][col]

        if (gem1 && gem2 && gem3 && gem1.type === gem2.type && gem2.type === gem3.type) {
          matches.push([gem1, gem2, gem3])
        }
      }
    }

    return matches
  }

  async processMatches(matches) {
    // 计算分数
    let totalScore = 0
    matches.forEach(match => {
      totalScore += match.length * 10
      // 四消、五消加分
      if (match.length > 3) {
        totalScore += (match.length - 3) * 20
      }
    })

    // 移除匹配的宝石
    matches.forEach(match => {
      match.forEach(gem => {
        this.grid[gem.row][gem.col] = null
        gem.destroy()
      })
    })

    // 更新分数
    this.scene.updateScore(totalScore)

    // 下落
    await this.dropGems()

    // 填充新宝石
    this.fillBoard()

    // 检查新的匹配（连锁）
    const newMatches = this.findMatches()
    if (newMatches.length > 0) {
      await this.processMatches(newMatches)
    }
  }

  async dropGems() {
    for (let col = 0; col < this.cols; col++) {
      let emptyRow = this.rows - 1

      for (let row = this.rows - 1; row >= 0; row--) {
        if (this.grid[row][col]) {
          if (row !== emptyRow) {
            const gem = this.grid[row][col]
            this.grid[emptyRow][col] = gem
            this.grid[row][col] = null
            gem.setGridPosition(emptyRow, col)
            await this.animateDrop(gem)
          }
          emptyRow--
        }
      }
    }
  }

  shuffleBoard() {
    // 收集所有宝石
    const gems = []
    for (let row = 0; row < this.rows; row++) {
      for (let col = 0; col < this.cols; col++) {
        if (this.grid[row][col]) {
          gems.push(this.grid[row][col])
        }
      }
    }

    // 打乱顺序
    for (let i = gems.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [gems[i], gems[j]] = [gems[j], gems[i]]
    }

    // 重新放置
    let index = 0
    for (let row = 0; row < this.rows; row++) {
      for (let col = 0; col < this.cols; col++) {
        this.grid[row][col] = gems[index]
        gems[index].setGridPosition(row, col)
        index++
      }
    }
  }
}
```

### 2. 宝石类 (Gem.js)

```javascript
export class Gem extends Phaser.GameObjects.Container {
  constructor(scene, x, y, type) {
    super(scene, x, y)

    this.type = type
    this.row = 0
    this.col = 0
    this.isSelected = false

    // 宝石图像
    this.image = scene.add.image(0, 0, `gem-${type}`)
    this.add(this.image)

    // 选中效果
    this.selectionCircle = scene.add.circle(0, 0, 30, 0xffffff, 0.3)
    this.add(this.selectionCircle)
    this.selectionCircle.setVisible(false)

    scene.add.existing(this)
  }

  setGridPosition(row, col) {
    this.row = row
    this.col = col

    // 更新实际位置
    const x = this.scene.board.x + col * this.scene.board.cellSize + this.scene.board.cellSize / 2
    const y = this.scene.board.y + row * this.scene.board.cellSize + this.scene.board.cellSize / 2

    this.setPosition(x, y)
  }

  select() {
    this.isSelected = true
    this.selectionCircle.setVisible(true)

    // 放大效果
    this.scene.tweens.add({
      targets: this,
      scaleX: 1.2,
      scaleY: 1.2,
      duration: 100
    })
  }

  deselect() {
    this.isSelected = false
    this.selectionCircle.setVisible(false)

    // 恢复大小
    this.scene.tweens.add({
      targets: this,
      scaleX: 1,
      scaleY: 1,
      duration: 100
    })
  }

  async animateSwap(targetX, targetY) {
    return new Promise(resolve => {
      this.scene.tweens.add({
        targets: this,
        x: targetX,
        y: targetY,
        duration: 200,
        ease: 'Power2',
        onComplete: resolve
      })
    })
  }

  async animateDrop(targetY) {
    return new Promise(resolve => {
      this.scene.tweens.add({
        targets: this,
        y: targetY,
        duration: 300,
        ease: 'Bounce',
        onComplete: resolve
      })
    })
  }

  animateDestroy() {
    this.scene.tweens.add({
      targets: this,
      scaleX: 0,
      scaleY: 0,
      alpha: 0,
      duration: 200,
      onComplete: () => this.destroy()
    })
  }
}
```

### 3. 关卡数据 (levels.js)

```javascript
export const LEVELS = [
  {
    id: 1,
    name: '入门关卡',
    rows: 8,
    cols: 8,
    targetScore: 1000,
    maxMoves: 30,
    goals: [
      { type: 'red', count: 10 }
    ]
  },
  {
    id: 2,
    name: '双色挑战',
    rows: 8,
    cols: 8,
    targetScore: 2000,
    maxMoves: 25,
    goals: [
      { type: 'red', count: 15 },
      { type: 'blue', count: 15 }
    ]
  },
  {
    id: 3,
    name: '三色迷阵',
    rows: 8,
    cols: 8,
    targetScore: 3000,
    maxMoves: 20,
    goals: [
      { type: 'red', count: 20 },
      { type: 'blue', count: 20 },
      { type: 'green', count: 20 }
    ]
  }
]
```

## 使用方法

1. 使用此模板创建新项目
2. 在 `assets/images/` 中添加宝石图片
3. 在 `data/levels.js` 中定义关卡
4. 调整 `config.js` 中的游戏参数
5. 运行 `npm run dev` 预览游戏

## 扩展点

- 添加特殊宝石：在 `SpecialGem.js` 中实现
- 添加道具系统：创建 `PowerUpManager`
- 添加成就系统：创建 `AchievementManager`
- 添加排行榜：创建 `LeaderboardManager`
