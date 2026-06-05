---
name: game-template-roguelike
description: 肉鸽游戏模板 - 随机地图、永久死亡、道具组合、技能构建
category: game-template
triggerPattern: roguelike, 肉鸽, roguelite, 随机, 地牢, dungeon, 永久死亡, permadeath
---

# 肉鸽游戏模板

## 概述

完整的肉鸽（Roguelike）游戏模板，包含：
- **随机地图**：程序生成的地牢关卡
- **永久死亡**：死亡后重新开始
- **道具系统**：随机掉落、效果叠加
- **技能构建**：多种技能组合
- **进度系统**：永久解锁内容
- **Boss 系统**：每层 Boss 战

## 核心代码

### 1. 地图生成器 (MapGenerator.js)

```javascript
export class MapGenerator {
  constructor(width, height) {
    this.width = width
    this.height = height
    this.grid = []
    this.rooms = []
    this.corridors = []
  }

  generate() {
    // 初始化网格
    this.initGrid()

    // 生成房间
    this.generateRooms()

    // 连接房间
    this.connectRooms()

    // 放置道具和敌人
    this.populate()

    return {
      grid: this.grid,
      rooms: this.rooms,
      spawn: this.getSpawnPoint(),
      exit: this.getExitPoint()
    }
  }

  initGrid() {
    for (let y = 0; y < this.height; y++) {
      this.grid[y] = []
      for (let x = 0; x < this.width; x++) {
        this.grid[y][x] = { type: 'wall', x, y }
      }
    }
  }

  generateRooms() {
    const roomCount = 5 + Math.floor(Math.random() * 5)
    for (let i = 0; i < roomCount; i++) {
      const room = this.createRoom()
      if (room) this.rooms.push(room)
    }
  }

  createRoom() {
    const width = 4 + Math.floor(Math.random() * 6)
    const height = 4 + Math.floor(Math.random() * 6)
    const x = 1 + Math.floor(Math.random() * (this.width - width - 2))
    const y = 1 + Math.floor(Math.random() * (this.height - height - 2))

    const room = { x, y, width, height }

    // 检查重叠
    for (const existing of this.rooms) {
      if (this.overlaps(room, existing)) return null
    }

    // 挖空房间
    for (let ry = y; ry < y + height; ry++) {
      for (let rx = x; rx < x + width; rx++) {
        this.grid[ry][rx].type = 'floor'
      }
    }

    return room
  }

  connectRooms() {
    for (let i = 1; i < this.rooms.length; i++) {
      const from = this.getCenter(this.rooms[i - 1])
      const to = this.getCenter(this.rooms[i])
      this.createCorridor(from, to)
    }
  }

  createCorridor(from, to) {
    let x = from.x
    let y = from.y

    while (x !== to.x || y !== to.y) {
      if (x < to.x) x++
      else if (x > to.x) x--
      else if (y < to.y) y++
      else if (y > to.y) y--

      this.grid[y][x].type = 'floor'
    }
  }

  populate() {
    // 放置宝箱
    this.rooms.forEach((room, i) => {
      if (i > 0 && Math.random() < 0.6) {
        const pos = this.getRandomFloor(room)
        this.grid[pos.y][pos.x].type = 'chest'
      }
    })

    // 放置敌人
    this.rooms.forEach((room, i) => {
      if (i > 0) {
        const count = 1 + Math.floor(Math.random() * 3)
        for (let j = 0; j < count; j++) {
          const pos = this.getRandomFloor(room)
          this.grid[pos.y][pos.x].type = 'enemy'
        }
      }
    })
  }

  getSpawnPoint() {
    return this.getCenter(this.rooms[0])
  }

  getExitPoint() {
    const lastRoom = this.rooms[this.rooms.length - 1]
    return this.getCenter(lastRoom)
  }

  getCenter(room) {
    return {
      x: Math.floor(room.x + room.width / 2),
      y: Math.floor(room.y + room.height / 2)
    }
  }
}
```

### 2. 道具系统 (ItemSystem.js)

```javascript
export const ITEM_RARITIES = [
  { id: 'common', name: '普通', color: '#AAAAAA', dropRate: 0.5 },
  { id: 'uncommon', name: '优秀', color: '#55FF55', dropRate: 0.3 },
  { id: 'rare', name: '稀有', color: '#5555FF', dropRate: 0.15 },
  { id: 'epic', name: '史诗', color: '#AA55AA', dropRate: 0.04 },
  { id: 'legendary', name: '传说', color: '#FFAA00', dropRate: 0.01 }
]

export const ITEM_TYPES = {
  weapon: {
    sword: { name: '剑', baseDamage: 10 },
    staff: { name: '法杖', baseDamage: 8, magicBonus: 5 },
    bow: { name: '弓', baseDamage: 12, range: 5 }
  },
  armor: {
    light: { name: '轻甲', defense: 5, speed: 0 },
    medium: { name: '中甲', defense: 10, speed: -5 },
    heavy: { name: '重甲', defense: 15, speed: -10 }
  },
  potion: {
    health: { name: '生命药水', effect: 'heal', value: 50 },
    mana: { name: '魔力药水', effect: 'mana', value: 30 },
    strength: { name: '力量药水', effect: 'buff', stat: 'attack', value: 10, duration: 60 }
  }
}

export class Item {
  constructor(type, subtype, rarity) {
    this.id = Date.now() + Math.random()
    this.type = type
    this.subtype = subtype
    this.rarity = rarity
    this.name = `${rarity.name} ${ITEM_TYPES[type][subtype].name}`
    this.stats = this.calculateStats()
  }

  calculateStats() {
    const base = ITEM_TYPES[this.type][this.subtype]
    const multiplier = this.getRarityMultiplier()
    const stats = {}

    Object.entries(base).forEach(([key, value]) => {
      if (typeof value === 'number') {
        stats[key] = Math.floor(value * multiplier)
      }
    })

    return stats
  }

  getRarityMultiplier() {
    const multipliers = { common: 1, uncommon: 1.2, rare: 1.5, epic: 2, legendary: 3 }
    return multipliers[this.rarity.id] || 1
  }
}

export function generateRandomItem() {
  const rarity = weightedRandom(ITEM_RARITIES)
  const types = Object.keys(ITEM_TYPES)
  const type = types[Math.floor(Math.random() * types.length)]
  const subtypes = Object.keys(ITEM_TYPES[type])
  const subtype = subtypes[Math.floor(Math.random() * subtypes.length)]

  return new Item(type, subtype, rarity)
}
```

### 3. 进度系统 (ProgressionSystem.js)

```javascript
export class ProgressionSystem {
  constructor() {
    this.meta = {
      totalRuns: 0,
      bestFloor: 0,
      totalKills: 0,
      totalGold: 0,
      unlockedCharacters: ['warrior'],
      unlockedItems: [],
      upgrades: {
        healthBonus: 0,
        damageBonus: 0,
        defenseBonus: 0,
        goldBonus: 0,
        luckBonus: 0
      }
    }
  }

  endRun(stats) {
    this.meta.totalRuns++
    this.meta.bestFloor = Math.max(this.meta.bestFloor, stats.floor)
    this.meta.totalKills += stats.kills
    this.meta.totalGold += stats.gold

    // 计算永久货币
    const permanentCurrency = Math.floor(stats.floor * 10 + stats.kills * 2)
    this.meta.permanentCurrency = (this.meta.permanentCurrency || 0) + permanentCurrency

    // 检查解锁
    this.checkUnlocks(stats)

    return permanentCurrency
  }

  checkUnlocks(stats) {
    // 解锁新角色
    if (stats.floor >= 5 && !this.meta.unlockedCharacters.includes('mage')) {
      this.meta.unlockedCharacters.push('mage')
    }
    if (stats.floor >= 10 && !this.meta.unlockedCharacters.includes('rogue')) {
      this.meta.unlockedCharacters.push('rogue')
    }

    // 解锁新道具
    if (stats.kills >= 100 && !this.meta.unlockedItems.includes('legendary_sword')) {
      this.meta.unlockedItems.push('legendary_sword')
    }
  }

  upgrade(upgradeId) {
    const cost = this.getUpgradeCost(upgradeId)
    if (this.meta.permanentCurrency < cost) return false

    this.meta.permanentCurrency -= cost
    this.meta.upgrades[upgradeId]++
    return true
  }

  getUpgradeCost(upgradeId) {
    const level = this.meta.upgrades[upgradeId] || 0
    return Math.floor(100 * Math.pow(1.5, level))
  }

  getPlayerBonuses() {
    return {
      maxHealth: this.meta.upgrades.healthBonus * 10,
      attack: this.meta.upgrades.damageBonus * 2,
      defense: this.meta.upgrades.defenseBonus * 2,
      goldMultiplier: 1 + (this.meta.upgrades.goldBonus * 0.1),
      luck: this.meta.upgrades.luckBonus * 5
    }
  }
}
```

## 游戏玩法

1. **探索地牢**：在随机生成的地牢中探索
2. **战斗敌人**：击败怪物获得经验和战利品
3. **收集道具**：拾取武器、防具、药水
4. **永久死亡**：死亡后本轮结束，但保留永久进度
5. **永久升级**：用获得的货币永久提升属性
6. **挑战 Boss**：每层末尾的 Boss 战

## 扩展点

- 添加新角色：在 `CharacterConfig` 中定义
- 添加新敌人：在 `EnemyConfig` 中定义
- 添加新道具：在 `ItemType` 中定义
- 添加新地图风格：在 `MapGenerator` 中扩展
- 添加成就系统：创建 `AchievementManager`
