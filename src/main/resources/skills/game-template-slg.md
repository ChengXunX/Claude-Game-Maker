---
name: game-template-slg
description: SLG策略游戏模板 - 提供完整的SLG游戏项目骨架
category: game-template
triggerPattern: SLG, simulation, 模拟, 策略, 4X, civilization, 文明, empire, 帝国, war, 战争
---

# SLG 策略游戏模板

## 概述

完整的 SLG（Simulation/Strategy）策略游戏模板，基于 Phaser 3 引擎。包含：
- 大地图系统（六边形网格、迷雾、视野）
- 城市系统（建造、升级、资源产出）
- 科技树系统（研究、解锁）
- 军事系统（兵种、编队、战斗）
- 外交系统（联盟、贸易、宣战）
- 回合制系统（阶段管理）

## 项目结构

```
game-slg/
├── index.html
├── package.json
├── src/
│   ├── main.js
│   ├── config.js
│   ├── scenes/
│   │   ├── BootScene.js
│   │   ├── MenuScene.js
│   │   ├── WorldScene.js        # 大地图场景
│   │   ├── CityScene.js         # 城市场景
│   │   ├── BattleScene.js       # 战斗场景
│   │   └── TechScene.js         # 科技树场景
│   ├── systems/
│   │   ├── HexMap.js            # 六边形地图
│   │   ├── CityManager.js       # 城市管理
│   │   ├── TechTree.js          # 科技树
│   │   ├── ArmyManager.js       # 军事管理
│   │   ├── DiplomacyManager.js  # 外交管理
│   │   ├── TurnManager.js       # 回合管理
│   │   └── ResourceManager.js   # 资源管理
│   ├── entities/
│   │   ├── City.js              # 城市
│   │   ├── Unit.js              # 军事单位
│   │   ├── Building.js          # 建筑
│   │   └── Technology.js        # 科技
│   ├── data/
│   │   ├── buildings.js         # 建筑数据
│   │   ├── technologies.js      # 科技数据
│   │   ├── units.js             # 兵种数据
│   │   └── civilizations.js     # 文明数据
│   └── ui/
│       ├── HUD.js
│       ├── CityPanel.js
│       ├── TechPanel.js
│       └── DiplomacyPanel.js
└── assets/
```

## 核心代码

### 1. 六边形地图 (HexMap.js)

```javascript
export class HexMap {
  constructor(rows, cols, hexSize) {
    this.rows = rows
    this.cols = cols
    this.hexSize = hexSize
    this.tiles = []
    this.fogOfWar = []
    this.init()
  }

  init() {
    for (let r = 0; r < this.rows; r++) {
      this.tiles[r] = []
      this.fogOfWar[r] = []
      for (let c = 0; c < this.cols; c++) {
        this.tiles[r][c] = this.generateTile(r, c)
        this.fogOfWar[r][c] = true // 未探索
      }
    }
  }

  generateTile(row, col) {
    const types = ['plain', 'forest', 'mountain', 'water', 'desert']
    const weights = [40, 25, 15, 10, 10]
    const type = this.weightedRandom(types, weights)

    return {
      row, col, type,
      terrain: TERRAIN_DATA[type],
      owner: null,
      improvement: null,
      resource: this.generateResource(type)
    }
  }

  hexToPixel(row, col) {
    const x = this.hexSize * (Math.sqrt(3) * col + (Math.sqrt(3) / 2) * (row % 2))
    const y = this.hexSize * (1.5 * row)
    return { x, y }
  }

  getNeighbors(row, col) {
    const dirs = row % 2 === 0
      ? [[0,1],[0,-1],[-1,0],[-1,1],[1,0],[1,1]]
      : [[0,1],[0,-1],[-1,-1],[-1,0],[1,-1],[1,0]]
    return dirs
      .map(([dr, dc]) => [row + dr, col + dc])
      .filter(([r, c]) => r >= 0 && r < this.rows && c >= 0 && c < this.cols)
      .map(([r, c]) => this.tiles[r][c])
  }

  revealArea(centerRow, centerCol, radius) {
    for (let r = centerRow - radius; r <= centerRow + radius; r++) {
      for (let c = centerCol - radius; c <= centerCol + radius; c++) {
        if (r >= 0 && r < this.rows && c >= 0 && c < this.cols) {
          const dist = this.hexDistance(centerRow, centerCol, r, c)
          if (dist <= radius) {
            this.fogOfWar[r][c] = false
          }
        }
      }
    }
  }
}
```

### 2. 城市系统 (City.js)

```javascript
export class City {
  constructor(config) {
    this.id = config.id
    this.name = config.name
    this.row = config.row
    this.col = config.col
    this.owner = config.owner
    this.population = 1
    this.buildings = []
    this.production = null
    this.storage = { food: 0, wood: 0, stone: 0, gold: 0 }
    this.yields = { food: 2, wood: 1, stone: 1, gold: 1 }
  }

  // 每回合产出
  processTurn() {
    // 计算产出
    const yields = this.calculateYields()

    // 增加存储
    Object.keys(yields).forEach(res => {
      this.storage[res] += yields[res]
    })

    // 人口增长
    this.checkPopulationGrowth()

    // 生产进度
    if (this.production) {
      this.production.progress += yields.production
      if (this.production.progress >= this.production.cost) {
        this.completeProduction()
      }
    }
  }

  calculateYields() {
    let yields = { ...this.yields }

    // 建筑加成
    this.buildings.forEach(b => {
      if (b.yields) {
        Object.keys(b.yields).forEach(res => {
          yields[res] = (yields[res] || 0) + b.yields[res]
        })
      }
    })

    // 人口加成
    yields.food += this.population * 2
    yields.production = this.population * 1.5

    return yields
  }

  checkPopulationGrowth() {
    if (this.storage.food >= this.getFoodRequired()) {
      this.population++
      this.storage.food -= this.getFoodRequired()
    }
  }

  getFoodRequired() {
    return Math.floor(10 * Math.pow(1.5, this.population - 1))
  }

  build(buildingDef) {
    if (this.canBuild(buildingDef)) {
      this.buildings.push({ ...buildingDef })
      return true
    }
    return false
  }

  canBuild(buildingDef) {
    return !this.buildings.some(b => b.id === buildingDef.id)
      && this.storage.wood >= (buildingDef.cost?.wood || 0)
      && this.storage.stone >= (buildingDef.cost?.stone || 0)
  }
}
```

### 3. 科技树 (TechTree.js)

```javascript
export class TechTree {
  constructor() {
    this.technologies = new Map()
    this.researched = new Set()
    this.currentResearch = null
    this.researchProgress = 0
    this.init()
  }

  init() {
    // 古代科技
    this.addTech('agriculture', '农业', 20, [], { food: 2 })
    this.addTech('mining', '采矿', 20, [], { stone: 2 })
    this.addTech('woodcutting', '伐木', 20, [], { wood: 2 })
    this.addTech('archery', '弓箭', 30, ['woodcutting'], { attack: 2 })

    // 中世纪科技
    this.addTech('iron_working', '冶铁', 50, ['mining'], { attack: 3 })
    this.addTech('construction', '建筑', 50, ['mining', 'woodcutting'], { defense: 3 })
    this.addTech('horseback_riding', '骑术', 40, ['agriculture'], { speed: 2 })
    this.addTech('mathematics', '数学', 60, ['agriculture', 'mining'], { production: 2 })

    // 近代科技
    this.addTech('gunpowder', '火药', 80, ['iron_working', 'mathematics'], { attack: 5 })
    this.addTech('printing', '印刷术', 70, ['mathematics'], { research: 2 })
    this.addTech('navigation', '航海', 80, ['mathematics', 'construction'], { vision: 3 })
  }

  addTech(id, name, cost, prerequisites, effects) {
    this.technologies.set(id, { id, name, cost, prerequisites, effects })
  }

  canResearch(techId) {
    const tech = this.technologies.get(techId)
    if (!tech) return false
    if (this.researched.has(techId)) return false
    return tech.prerequisites.every(p => this.researched.has(p))
  }

  startResearch(techId) {
    if (!this.canResearch(techId)) return false
    this.currentResearch = techId
    this.researchProgress = 0
    return true
  }

  addResearchPoints(points) {
    if (!this.currentResearch) return

    this.researchProgress += points
    const tech = this.technologies.get(this.currentResearch)

    if (this.researchProgress >= tech.cost) {
      this.researched.add(this.currentResearch)
      this.currentResearch = null
      this.researchProgress = 0
      return tech
    }
    return null
  }

  getResearchProgress() {
    if (!this.currentResearch) return 0
    const tech = this.technologies.get(this.currentResearch)
    return Math.min(100, (this.researchProgress / tech.cost) * 100)
  }
}
```

### 4. 回合管理 (TurnManager.js)

```javascript
export class TurnManager {
  constructor() {
    this.currentTurn = 1
    this.currentPhase = 'player' // player, ai, production, combat
    this.players = []
    this.currentPlayerIndex = 0
    this.onTurnEnd = null
    this.onPhaseChange = null
  }

  addPlayer(player) {
    this.players.push(player)
  }

  nextPhase() {
    const phases = ['player', 'production', 'combat', 'ai']
    const currentIndex = phases.indexOf(this.currentPhase)
    const nextIndex = (currentIndex + 1) % phases.length

    if (nextIndex === 0) {
      this.nextTurn()
    }

    this.currentPhase = phases[nextIndex]
    this.onPhaseChange?.(this.currentPhase)
  }

  nextTurn() {
    this.currentTurn++

    // 处理每个玩家的回合
    this.players.forEach(player => {
      player.processTurn(this.currentTurn)
    })

    this.onTurnEnd?.(this.currentTurn)
  }

  getCurrentPlayer() {
    return this.players[this.currentPlayerIndex]
  }

  endTurn() {
    this.nextPhase()
  }
}
```

## 使用方法

1. 使用此模板创建新项目
2. 在 `data/` 中定义建筑、科技、兵种数据
3. 在 `assets/` 中添加地图和单位图片
4. 运行 `npm run dev` 预览游戏

## 扩展点

- 添加新文明：在 `data/civilizations.js` 中定义
- 添加新科技：在 `data/technologies.js` 中定义
- 添加新建筑：在 `data/buildings.js` 中定义
- 添加新兵种：在 `data/units.js` 中定义
- 添加 AI 对手：创建 `AIManager`
- 添加多人模式：集成 WebSocket
