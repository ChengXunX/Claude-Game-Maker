---
name: game-template-tile-match
description: 羊了个羊风格三消堆叠益智游戏模板 - 多层堆叠、三消匹配、高难度挑战
category: game-template
triggerPattern: tile, 三消, 堆叠, match, 羊了个羊, mahjong, 麻将, 消除, puzzle, 益智
---

# 羊了个羊风格三消堆叠游戏模板

## 概述

完整的三消堆叠益智游戏模板，类似《羊了个羊》。支持：
- **多层堆叠**：牌面堆叠在多层，需要逐层消除
- **三消匹配**：选中3张相同牌面消除
- **暂存槽**：最多7张牌暂存
- **道具系统**：撤回、洗牌、移出
- **关卡系统**：递增难度，高挑战性
- **排行榜**：微信好友排行

## 项目结构

```
game-tile-match/
├── index.html
├── package.json
├── src/
│   ├── main.js
│   ├── config.js
│   ├── scenes/
│   │   ├── BootScene.js
│   │   ├── MenuScene.js
│   │   ├── GameScene.js
│   │   └── ResultScene.js
│   ├── entities/
│   │   ├── Tile.js             # 牌面
│   │   ├── TileStack.js        # 牌面堆叠
│   │   ├── Slot.js             # 暂存槽
│   │   └── Prop.js             # 道具
│   ├── systems/
│   │   ├── MatchSystem.js      # 匹配系统
│   │   ├── ShuffleSystem.js    # 洗牌系统
│   │   ├── UndoSystem.js       # 撤回系统
│   │   ├── LevelSystem.js      # 关卡系统
│   │   └── ScoreSystem.js      # 计分系统
│   ├── data/
│   │   ├── levels.js           # 关卡数据
│   │   └── tileTypes.js        # 牌面类型
│   └── ui/
│       ├── HUD.js
│       ├── SlotBar.js
│       └── PropsBar.js
└── assets/
```

## 核心代码

### 1. 游戏配置 (config.js)

```javascript
export const GAME_CONFIG = {
  width: 400,
  height: 700,
  slotCapacity: 7,           // 暂存槽容量
  matchCount: 3,             // 匹配数量
  tileWidth: 50,
  tileHeight: 60,
  tileGap: 2,
  maxLayers: 5,              // 最大堆叠层数
  props: {
    undo: { maxUses: 3 },    // 撤回道具
    shuffle: { maxUses: 1 }, // 洗牌道具
    remove: { maxUses: 1 }   // 移出道具
  }
}
```

### 2. 牌面类型 (tileTypes.js)

```javascript
export const TILE_TYPES = [
  { id: 'bamboo', name: '竹子', emoji: '🎋' },
  { id: 'flower', name: '花朵', emoji: '🌸' },
  { id: 'fruit', name: '水果', emoji: '🍎' },
  { id: 'animal', name: '动物', emoji: '🐑' },
  { id: 'star', name: '星星', emoji: '⭐' },
  { id: 'moon', name: '月亮', emoji: '🌙' },
  { id: 'sun', name: '太阳', emoji: '☀️' },
  { id: 'cloud', name: '云朵', emoji: '☁️' },
  { id: 'heart', name: '爱心', emoji: '❤️' },
  { id: 'diamond', name: '钻石', emoji: '💎' },
  { id: 'crown', name: '皇冠', emoji: '👑' },
  { id: 'rocket', name: '火箭', emoji: '🚀' }
]
```

### 3. 牌面 (Tile.js)

```javascript
export class Tile extends Phaser.GameObjects.Container {
  constructor(scene, x, y, tileType, layer) {
    super(scene, x, y)
    this.tileType = tileType
    this.layer = layer
    this.isBlocked = false
    this.isSelected = false
    this.isRemoved = false

    // 牌面背景
    this.bg = scene.add.rectangle(0, 0, GAME_CONFIG.tileWidth, GAME_CONFIG.tileHeight,
      this.getLayerColor(layer))
    this.bg.setStrokeStyle(2, 0x333333)
    this.add(this.bg)

    // 牌面图案
    this.emoji = scene.add.text(0, 0, tileType.emoji, {
      fontSize: '24px'
    })
    this.emoji.setOrigin(0.5)
    this.add(this.emoji)

    // 牌面名称
    this.label = scene.add.text(0, 20, tileType.name, {
      fontSize: '10px',
      color: '#666'
    })
    this.label.setOrigin(0.5)
    this.add(this.label)

    // 设置交互
    this.setSize(GAME_CONFIG.tileWidth, GAME_CONFIG.tileHeight)
    this.setInteractive()
    this.on('pointerdown', () => this.onClick())

    scene.add.existing(this)
  }

  getLayerColor(layer) {
    const colors = [0xFFFFFF, 0xE8F5E9, 0xE3F2FD, 0xFFF3E0, 0xFCE4EC]
    return colors[layer % colors.length]
  }

  onClick() {
    if (this.isBlocked || this.isRemoved) return
    this.scene.onTileClick(this)
  }

  select() {
    this.isSelected = true
    this.bg.setStrokeStyle(3, 0x4CAF50)
    this.setScale(1.1)
  }

  deselect() {
    this.isSelected = false
    this.bg.setStrokeStyle(2, 0x333333)
    this.setScale(1)
  }

  block() {
    this.isBlocked = true
    this.setAlpha(0.6)
  }

  unblock() {
    this.isBlocked = false
    this.setAlpha(1)
  }

  remove() {
    this.isRemoved = true
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

### 4. 暂存槽 (Slot.js)

```javascript
export class SlotBar extends Phaser.GameObjects.Container {
  constructor(scene, x, y) {
    super(scene, x, y)
    this.slots = []
    this.capacity = GAME_CONFIG.slotCapacity

    // 创建槽位
    for (let i = 0; i < this.capacity; i++) {
      const slot = scene.add.rectangle(
        i * (GAME_CONFIG.tileWidth + 8), 0,
        GAME_CONFIG.tileWidth, GAME_CONFIG.tileHeight,
        0xF5F5F5
      )
      slot.setStrokeStyle(1, 0xCCCCCC)
      this.add(slot)
      this.slots.push({ rect: slot, tile: null })
    }

    scene.add.existing(this)
  }

  addTile(tile) {
    if (this.isFull()) return false

    // 找到第一个空槽位
    const emptySlot = this.slots.find(s => s.tile === null)
    if (!emptySlot) return false

    // 移动牌面到槽位
    emptySlot.tile = tile
    tile.setPosition(emptySlot.rect.x, emptySlot.rect.y)
    tile.setInteractive(false) // 槽位中的牌不可直接点击

    // 检查是否可以消除
    this.checkMatch()

    return true
  }

  checkMatch() {
    // 统计各类型牌面数量
    const typeCount = {}
    this.slots.forEach(slot => {
      if (slot.tile) {
        const type = slot.tile.tileType.id
        typeCount[type] = (typeCount[type] || 0) + 1
      }
    })

    // 检查是否有3张相同的
    for (const [type, count] of Object.entries(typeCount)) {
      if (count >= 3) {
        this.eliminate(type)
        return true
      }
    }

    // 检查是否满了
    if (this.isFull()) {
      this.scene.gameOver(false)
    }

    return false
  }

  eliminate(type) {
    // 移除3张相同牌面
    let removed = 0
    this.slots.forEach(slot => {
      if (slot.tile && slot.tile.tileType.id === type && removed < 3) {
        slot.tile.remove()
        slot.tile = null
        removed++
      }
    })

    // 压缩槽位
    this.compact()

    // 更新分数
    this.scene.addScore(30)
  }

  compact() {
    // 将所有牌面移到前面
    const tiles = this.slots.filter(s => s.tile).map(s => s.tile)
    this.slots.forEach((slot, i) => {
      if (i < tiles.length) {
        slot.tile = tiles[i]
        slot.tile.setPosition(slot.rect.x, slot.rect.y)
      } else {
        slot.tile = null
      }
    })
  }

  isFull() {
    return this.slots.every(s => s.tile !== null)
  }

  isEmpty() {
    return this.slots.every(s => s.tile === null)
  }

  getLastTile() {
    for (let i = this.slots.length - 1; i >= 0; i--) {
      if (this.slots[i].tile) return this.slots[i].tile
    }
    return null
  }
}
```

### 5. 关卡数据 (levels.js)

```javascript
export const LEVELS = [
  {
    id: 1,
    name: '新手入门',
    difficulty: 1,
    tileCount: 30,
    typeCount: 5,
    layers: 2,
    layout: 'simple'
  },
  {
    id: 2,
    name: '小试牛刀',
    difficulty: 2,
    tileCount: 45,
    typeCount: 6,
    layers: 3,
    layout: 'pyramid'
  },
  {
    id: 3,
    name: '渐入佳境',
    difficulty: 3,
    tileCount: 60,
    typeCount: 7,
    layers: 3,
    layout: 'cross'
  },
  {
    id: 4,
    name: '挑战开始',
    difficulty: 4,
    tileCount: 75,
    typeCount: 8,
    layers: 4,
    layout: 'diamond'
  },
  {
    id: 5,
    name: '地狱难度',
    difficulty: 5,
    tileCount: 90,
    typeCount: 10,
    layers: 5,
    layout: 'complex'
  }
]
```

### 6. 游戏场景 (GameScene.js)

```javascript
export class GameScene extends Phaser.Scene {
  constructor() {
    super('GameScene')
  }

  init(data) {
    this.levelId = data.levelId || 1
    this.level = LEVELS.find(l => l.id === this.levelId)
    this.score = 0
    this.undoStack = []
    this.props = { undo: 3, shuffle: 1, remove: 1 }
  }

  create() {
    // 创建背景
    this.add.rectangle(200, 350, 400, 700, 0xFFF8E1)

    // 创建牌面
    this.tiles = []
    this.createTiles()

    // 创建暂存槽
    this.slotBar = new SlotBar(this, 50, 650)

    // 创建道具栏
    this.createPropsBar()

    // 创建 HUD
    this.createHUD()

    // 检查初始状态
    this.updateBlockedStatus()
  }

  createTiles() {
    // 根据关卡配置生成牌面
    const { tileCount, typeCount, layers, layout } = this.level
    const types = TILE_TYPES.slice(0, typeCount)

    // 生成牌面数据（确保能3消）
    const tileData = []
    for (let i = 0; i < tileCount; i += 3) {
      const type = types[i % types.length]
      tileData.push(type, type, type)
    }

    // 洗牌
    this.shuffleArray(tileData)

    // 根据布局生成位置
    const positions = this.generateLayout(layout, layers)

    // 创建牌面对象
    positions.forEach((pos, index) => {
      if (index < tileData.length) {
        const tile = new Tile(this, pos.x, pos.y, tileData[index], pos.layer)
        this.tiles.push(tile)
      }
    })
  }

  generateLayout(layout, layers) {
    const positions = []
    const centerX = 200
    const centerY = 300

    switch (layout) {
      case 'simple':
        // 简单网格
        for (let row = 0; row < 5; row++) {
          for (let col = 0; col < 6; col++) {
            positions.push({
              x: centerX - 150 + col * 55,
              y: centerY - 100 + row * 65,
              layer: 0
            })
          }
        }
        break

      case 'pyramid':
        // 金字塔形状
        for (let layer = 0; layer < layers; layer++) {
          const size = 5 - layer
          for (let row = 0; row < size; row++) {
            for (let col = 0; col < size; col++) {
              positions.push({
                x: centerX - (size * 27) + col * 55 + layer * 10,
                y: centerY - (size * 32) + row * 65 - layer * 20,
                layer: layer
              })
            }
          }
        }
        break

      case 'cross':
        // 十字形状
        for (let i = 0; i < 5; i++) {
          for (let j = 0; j < 3; j++) {
            positions.push({ x: centerX - 100 + i * 55, y: centerY - 50 + j * 65, layer: 0 })
            positions.push({ x: centerX - 50 + j * 55, y: centerY - 100 + i * 65, layer: 0 })
          }
        }
        break

      default:
        // 默认随机布局
        for (let i = 0; i < 30; i++) {
          positions.push({
            x: 50 + Math.random() * 300,
            y: 50 + Math.random() * 500,
            layer: Math.floor(Math.random() * layers)
          })
        }
    }

    return positions
  }

  onTileClick(tile) {
    // 移动到暂存槽
    const success = this.slotBar.addTile(tile)
    if (success) {
      this.undoStack.push(tile)
      this.updateBlockedStatus()
      this.checkWin()
    }
  }

  updateBlockedStatus() {
    // 更新牌面的遮挡状态
    this.tiles.forEach(tile => {
      if (tile.isRemoved) return

      const isCovered = this.tiles.some(other => {
        if (other === tile || other.isRemoved) return false
        const dx = Math.abs(tile.x - other.x)
        const dy = Math.abs(tile.y - other.y)
        return dx < 50 && dy < 60 && other.layer > tile.layer
      })

      if (isCovered) {
        tile.block()
      } else {
        tile.unblock()
      }
    })
  }

  checkWin() {
    const remaining = this.tiles.filter(t => !t.isRemoved).length
    if (remaining === 0) {
      this.gameOver(true)
    }
  }

  gameOver(win) {
    this.scene.start('ResultScene', {
      levelId: this.levelId,
      score: this.score,
      win: win
    })
  }

  addScore(points) {
    this.score += points
    this.updateHUD()
  }

  shuffleArray(array) {
    for (let i = array.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [array[i], array[j]] = [array[j], array[i]]
    }
  }
}
```

## 游戏玩法

1. **选择牌面**：点击未被遮挡的牌面移入暂存槽
2. **三消匹配**：暂存槽中3张相同牌面自动消除
3. **胜负判定**：
   - 胜利：所有牌面消除完毕
   - 失败：暂存槽满了但还有牌面剩余
4. **道具使用**：
   - 撤回：将暂存槽最后一张牌放回原位
   - 洗牌：打乱剩余牌面顺序
   - 移出：将暂存槽中一张牌移出游戏

## 使用方法

1. 使用此模板创建新项目
2. 在 `assets/images/` 中添加牌面图片
3. 调整 `config.js` 中的游戏参数
4. 运行 `npm run dev` 预览游戏

## 扩展点

- 添加新牌面类型：在 `tileTypes.js` 中定义
- 添加新关卡：在 `levels.js` 中定义
- 添加新布局：在 `generateLayout` 方法中实现
- 添加成就系统：创建 `AchievementManager`
- 添加排行榜：创建 `LeaderboardManager`
