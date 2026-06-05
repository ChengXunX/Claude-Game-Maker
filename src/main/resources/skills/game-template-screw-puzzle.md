---
name: game-template-screw-puzzle
description: 螺丝解谜游戏模板 - 拆卸螺丝、分类收集、物理解谜
category: game-template
triggerPattern: screw, 螺丝, bolt, 解谜, puzzle, 拆卸, 分类, 物理
---

# 螺丝解谜游戏模板

## 概述

螺丝解谜游戏模板，包含：
- **螺丝系统**：多种螺丝类型、颜色分类
- **拆卸系统**：点击拆卸、顺序解谜
- **收集系统**：按颜色分类收集
- **关卡系统**：递增难度
- **道具系统**：提示、重排、时间暂停

## 核心代码

### 1. 螺丝系统 (ScrewSystem.js)

```javascript
export const SCREW_TYPES = [
  { id: 'red', name: '红色', color: '#FF4444' },
  { id: 'blue', name: '蓝色', color: '#4444FF' },
  { id: 'green', name: '绿色', color: '#44FF44' },
  { id: 'yellow', name: '黄色', color: '#FFFF44' },
  { id: 'purple', name: '紫色', color: '#AA44AA' },
  { id: 'orange', name: '橙色', color: '#FFAA44' }
]

export class Screw {
  constructor(type, x, y, layer) {
    this.id = Date.now() + Math.random()
    this.type = type
    this.x = x
    this.y = y
    this.layer = layer
    this.isRemoved = false
    this.isBlocked = false
  }
}

export class ScrewBoard {
  constructor(levelConfig) {
    this.screws = []
    this.layers = levelConfig.layers || 3
    this.generate(levelConfig)
  }

  generate(config) {
    const { screwCount, typeCount } = config
    const types = SCREW_TYPES.slice(0, typeCount)

    // 生成螺丝（确保每种颜色有3个）
    const screwData = []
    for (let i = 0; i < screwCount; i += 3) {
      const type = types[i % types.length]
      screwData.push(type, type, type)
    }

    // 洗牌
    this.shuffleArray(screwData)

    // 放置螺丝
    const positions = this.generatePositions(config.layout, this.layers)
    positions.forEach((pos, i) => {
      if (i < screwData.length) {
        this.screws.push(new Screw(screwData[i], pos.x, pos.y, pos.layer))
      }
    })

    this.updateBlockedStatus()
  }

  generatePositions(layout, layers) {
    const positions = []
    const centerX = 200
    const centerY = 300

    for (let layer = 0; layer < layers; layer++) {
      const count = 4 + layer * 2
      for (let i = 0; i < count; i++) {
        const angle = (2 * Math.PI * i) / count
        const radius = 60 + layer * 50
        positions.push({
          x: centerX + Math.cos(angle) * radius,
          y: centerY + Math.sin(angle) * radius,
          layer
        })
      }
    }

    return positions
  }

  removeScrew(screwId) {
    const screw = this.screws.find(s => s.id === screwId)
    if (!screw || screw.isBlocked) return false

    screw.isRemoved = true
    this.updateBlockedStatus()
    return true
  }

  updateBlockedStatus() {
    this.screws.forEach(screw => {
      if (screw.isRemoved) return

      screw.isBlocked = this.screws.some(other => {
        if (other === screw || other.isRemoved) return false
        const dx = Math.abs(screw.x - other.x)
        const dy = Math.abs(screw.y - other.y)
        return dx < 30 && dy < 30 && other.layer > screw.layer
      })
    })
  }

  getRemainingCount() {
    return this.screws.filter(s => !s.isRemoved).length
  }

  shuffleArray(array) {
    for (let i = array.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [array[i], array[j]] = [array[j], array[i]]
    }
  }
}
```

### 2. 收集槽 (CollectSlot.js)

```javascript
export class CollectSlot {
  constructor(capacity) {
    this.capacity = capacity
    this.slots = []
  }

  addScrew(screw) {
    if (this.isFull()) return false
    this.slots.push(screw)
    this.checkMatch()
    return true
  }

  checkMatch() {
    const typeCount = {}
    this.slots.forEach(s => {
      typeCount[s.type.id] = (typeCount[s.type.id] || 0) + 1
    })

    for (const [typeId, count] of Object.entries(typeCount)) {
      if (count >= 3) {
        this.eliminate(typeId)
        return true
      }
    }

    return false
  }

  eliminate(typeId) {
    this.slots = this.slots.filter(s => s.type.id !== typeId)
  }

  isFull() {
    return this.slots.length >= this.capacity
  }
}
```

### 3. 关卡数据 (levels.js)

```javascript
export const SCREW_LEVELS = [
  {
    id: 1,
    name: '简单入门',
    screwCount: 12,
    typeCount: 2,
    layers: 2,
    layout: 'circle'
  },
  {
    id: 2,
    name: '小有挑战',
    screwCount: 18,
    typeCount: 3,
    layers: 3,
    layout: 'circle'
  },
  {
    id: 3,
    name: '渐入佳境',
    screwCount: 24,
    typeCount: 4,
    layers: 3,
    layout: 'hexagon'
  },
  {
    id: 4,
    name: '烧脑谜题',
    screwCount: 30,
    typeCount: 5,
    layers: 4,
    layout: 'hexagon'
  },
  {
    id: 5,
    name: '终极挑战',
    screwCount: 36,
    typeCount: 6,
    layers: 5,
    layout: 'complex'
  }
]
```

## 游戏玩法

1. **观察布局**：查看螺丝的位置和颜色
2. **拆卸螺丝**：点击未被遮挡的螺丝
3. **分类收集**：相同颜色的螺丝进入收集槽
4. **自动消除**：收集槽中3个相同颜色自动消除
5. **全部拆完**：所有螺丝拆完即胜利

## 扩展点

- 添加新螺丝类型：在 `SCREW_TYPES` 中定义
- 添加新关卡：在 `SCREW_LEVELS` 中定义
- 添加新道具：创建 `PropSystem`
- 添加成就系统：创建 `AchievementManager`
