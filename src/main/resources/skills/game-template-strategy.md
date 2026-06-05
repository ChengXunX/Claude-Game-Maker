---
name: game-template-strategy
description: 策略游戏模板 - 提供完整的策略游戏项目骨架
category: game-template
triggerPattern: strategy, RTS, 策略, 战争, army, 军队, empire, 帝国
---

# 策略游戏模板

## 概述

回合制策略游戏模板，基于 Phaser 3 引擎。包含：
- 地图系统（六边形网格）
- 单位系统（多种单位类型）
- 资源系统（金币、木材、矿石）
- 建筑系统（建造、升级）
- 战斗系统（回合制战斗）
- AI 对手

## 核心代码

### 六边形网格

```javascript
export class HexGrid {
  constructor(scene, rows, cols, hexSize) {
    this.scene = scene
    this.rows = rows
    this.cols = cols
    this.hexSize = hexSize
    this.grid = []

    this.init()
  }

  init() {
    for (let row = 0; row < this.rows; row++) {
      this.grid[row] = []
      for (let col = 0; col < this.cols; col++) {
        const { x, y } = this.hexToPixel(row, col)
        this.grid[row][col] = {
          row, col, x, y,
          terrain: 'grass',
          unit: null,
          building: null
        }
      }
    }
  }

  hexToPixel(row, col) {
    const x = this.hexSize * (Math.sqrt(3) * col + (Math.sqrt(3) / 2) * (row % 2))
    const y = this.hexSize * (1.5 * row)
    return { x, y }
  }

  getNeighbors(row, col) {
    const directions = [
      [[0, 1], [0, -1], [-1, 0], [-1, 1], [1, 0], [1, 1]],
      [[0, 1], [0, -1], [-1, -1], [-1, 0], [1, -1], [1, 0]]
    ]
    const parity = row % 2
    return directions[parity]
      .map(([dr, dc]) => [row + dr, col + dc])
      .filter(([r, c]) => r >= 0 && r < this.rows && c >= 0 && c < this.cols)
      .map(([r, c]) => this.grid[r][c])
  }
}
```

### 单位系统

```javascript
export class Unit {
  constructor(config) {
    this.type = config.type
    this.name = config.name
    this.health = config.health
    this.maxHealth = config.health
    this.attack = config.attack
    this.defense = config.defense
    this.movement = config.movement
    this.range = config.range
    this.row = 0
    this.col = 0
    this.owner = null
    this.hasMoved = false
    this.hasAttacked = false
  }

  canMoveTo(targetRow, targetCol, grid) {
    const distance = this.getDistance(this.row, this.col, targetRow, targetCol)
    return distance <= this.movement && !grid[targetRow][targetCol].unit
  }

  canAttack(targetRow, targetCol, grid) {
    const distance = this.getDistance(this.row, this.col, targetRow, targetCol)
    return distance <= this.range && grid[targetRow][targetCol].unit
  }

  getDistance(row1, col1, row2, col2) {
    // 六边形距离计算
    const dx = col2 - col1
    const dy = row2 - row1
    return Math.max(Math.abs(dx), Math.abs(dy), Math.abs(dx + dy))
  }

  takeDamage(amount) {
    const actualDamage = Math.max(1, amount - this.defense)
    this.health -= actualDamage
    return actualDamage
  }

  isAlive() {
    return this.health > 0
  }
}
```

### 资源系统

```javascript
export class ResourceManager {
  constructor() {
    this.resources = {
      gold: 100,
      wood: 50,
      ore: 30
    }
  }

  canAfford(cost) {
    return Object.entries(cost).every(([resource, amount]) =>
      this.resources[resource] >= amount
    )
  }

  spend(cost) {
    if (!this.canAfford(cost)) return false
    Object.entries(cost).forEach(([resource, amount]) => {
      this.resources[resource] -= amount
    })
    return true
  }

  earn(income) {
    Object.entries(income).forEach(([resource, amount]) => {
      this.resources[resource] += amount
    })
  }
}
```

## 使用方法

1. 使用此模板创建新项目
2. 在 `data/units.js` 中定义单位类型
3. 在 `data/buildings.js` 中定义建筑类型
4. 运行 `npm run dev` 预览游戏
