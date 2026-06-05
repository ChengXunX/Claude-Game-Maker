---
name: game-template-town-sim
description: 小镇模拟经营游戏模板 - 建造房屋、居民管理、资源收集、装饰美化
category: game-template
triggerPattern: town, 小镇, sim, 模拟, village, 村庄, build, 建造, decoration, 装饰
---

# 小镇模拟经营游戏模板

## 概述

小镇模拟经营游戏模板，包含：
- **建造系统**：多种建筑类型、自由摆放
- **居民系统**：居民需求、幸福感、工作分配
- **资源系统**：金币、木材、石材、食物
- **装饰系统**：美化小镇、提升幸福度
- **任务系统**：主线/支线任务
- **天气系统**：影响产出和心情

## 核心代码

### 1. 建筑系统 (BuildingSystem.js)

```javascript
export const BUILDING_TYPES = {
  house: {
    name: '民居',
    cost: { gold: 100, wood: 50 },
    capacity: 4,
    happiness: 5,
    buildTime: 60,
    size: { width: 2, height: 2 }
  },
  farm: {
    name: '农田',
    cost: { gold: 50, wood: 20 },
    production: { food: 10 },
    workers: 2,
    buildTime: 30,
    size: { width: 3, height: 3 }
  },
  workshop: {
    name: '工坊',
    cost: { gold: 200, wood: 100, stone: 50 },
    production: { wood: 5, stone: 3 },
    workers: 3,
    buildTime: 120,
    size: { width: 2, height: 2 }
  },
  market: {
    name: '集市',
    cost: { gold: 500, wood: 200, stone: 100 },
    income: 50,
    happiness: 10,
    buildTime: 300,
    size: { width: 3, height: 3 }
  },
  park: {
    name: '公园',
    cost: { gold: 150, wood: 30 },
    happiness: 20,
    buildTime: 60,
    size: { width: 2, height: 2 }
  }
}

export class Building {
  constructor(type, x, y) {
    this.id = Date.now() + Math.random()
    this.type = type
    this.x = x
    this.y = y
    this.config = BUILDING_TYPES[type]
    this.level = 1
    this.workers = []
    this.lastProduction = Date.now()
  }

  getProduction() {
    if (!this.config.production) return {}
    const multiplier = 1 + (this.level - 1) * 0.2
    const production = {}
    Object.entries(this.config.production).forEach(([resource, amount]) => {
      production[resource] = Math.floor(amount * multiplier)
    })
    return production
  }

  getUpgradeCost() {
    const cost = {}
    Object.entries(this.config.cost).forEach(([resource, amount]) => {
      cost[resource] = Math.floor(amount * Math.pow(1.5, this.level))
    })
    return cost
  }

  upgrade() {
    this.level++
    return this.level
  }

  assignWorker(worker) {
    if (!this.config.workers) return false
    if (this.workers.length >= this.config.workers) return false
    this.workers.push(worker)
    return true
  }
}
```

### 2. 居民系统 (ResidentSystem.js)

```javascript
export class Resident {
  constructor(name) {
    this.id = Date.now() + Math.random()
    this.name = name
    this.happiness = 50
    this.needs = {
      food: 100,
      shelter: 100,
      entertainment: 50
    }
    this.workplace = null
    this.home = null
  }

  update(town) {
    // 更新需求
    this.needs.food -= 1
    this.needs.entertainment -= 0.5

    // 检查是否有住所
    this.needs.shelter = this.home ? 100 : 0

    // 计算幸福感
    this.happiness = Math.floor(
      (this.needs.food + this.needs.shelter + this.needs.entertainment) / 3
    )

    // 检查需求
    if (this.needs.food <= 0) {
      this.happiness -= 20
    }

    return this.happiness
  }

  assignWork(building) {
    this.workplace = building
    building.assignWorker(this)
  }

  assignHome(building) {
    this.home = building
  }
}

export class ResidentManager {
  constructor() {
    this.residents = []
    this.maxResidents = 0
  }

  addResident(name) {
    if (this.residents.length >= this.maxResidents) return null
    const resident = new Resident(name)
    this.residents.push(resident)
    return resident
  }

  removeResident(id) {
    this.residents = this.residents.filter(r => r.id !== id)
  }

  getAverageHappiness() {
    if (this.residents.length === 0) return 0
    const total = this.residents.reduce((sum, r) => sum + r.happiness, 0)
    return Math.floor(total / this.residents.length)
  }
}
```

### 3. 资源系统 (ResourceSystem.js)

```javascript
export class ResourceSystem {
  constructor() {
    this.resources = {
      gold: 500,
      wood: 200,
      stone: 100,
      food: 100
    }
  }

  add(resource, amount) {
    this.resources[resource] = (this.resources[resource] || 0) + amount
  }

  remove(resource, amount) {
    if (!this.canAfford(resource, amount)) return false
    this.resources[resource] -= amount
    return true
  }

  canAfford(cost) {
    return Object.entries(cost).every(([resource, amount]) =>
      (this.resources[resource] || 0) >= amount
    )
  }

  spend(cost) {
    if (!this.canAfford(cost)) return false
    Object.entries(cost).forEach(([resource, amount]) => {
      this.resources[resource] -= amount
    })
    return true
  }

  collect(buildings) {
    buildings.forEach(building => {
      const production = building.getProduction()
      Object.entries(production).forEach(([resource, amount]) => {
        this.add(resource, amount)
      })
    })
  }
}
```

## 游戏玩法

1. **建造建筑**：花费资源建造各种建筑
2. **招募居民**：居民入住小镇
3. **分配工作**：让居民在建筑中工作
4. **收集资源**：定期收集产出的资源
5. **装饰小镇**：添加装饰提升幸福度
6. **完成任务**：完成任务获得奖励

## 扩展点

- 添加新建筑：在 `BUILDING_TYPES` 中定义
- 添加新居民类型：在 `ResidentConfig` 中定义
- 添加新资源：在 `ResourceSystem` 中扩展
- 添加天气系统：创建 `WeatherSystem`
- 添加成就系统：创建 `AchievementManager`
