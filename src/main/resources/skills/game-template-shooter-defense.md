---
name: game-template-shooter-defense
description: 射击防御游戏模板 - 射击+塔防混合、怪物波次、武器升级、防御工事
category: game-template
triggerPattern: shooter, defense, 射击, 防御, zombie, 僵尸, monster, 怪物, wave, 波次
---

# 射击防御游戏模板

## 概述

射击+塔防混合游戏模板，包含：
- **射击系统**：多种武器、手动瞄准射击
- **波次系统**：递增难度的怪物波次
- **防御工事**：建造墙壁、炮塔
- **武器升级**：升级武器伤害、射速
- **技能系统**：主动/被动技能
- **Boss 战**：每 10 波 Boss 出现

## 核心代码

### 1. 武器系统 (WeaponSystem.js)

```javascript
export const WEAPONS = {
  pistol: {
    name: '手枪',
    damage: 10,
    fireRate: 300,
    spread: 0.05,
    ammo: Infinity,
    reloadTime: 1000,
    upgrades: { damage: [12, 15, 20], fireRate: [280, 250, 200] }
  },
  shotgun: {
    name: '霰弹枪',
    damage: 8,
    fireRate: 800,
    spread: 0.3,
    pellets: 5,
    ammo: 30,
    reloadTime: 2000,
    upgrades: { damage: [10, 12, 15], pellets: [6, 7, 8] }
  },
  rifle: {
    name: '步枪',
    damage: 15,
    fireRate: 100,
    spread: 0.02,
    ammo: 120,
    reloadTime: 1500,
    upgrades: { damage: [18, 22, 28], fireRate: [90, 80, 60] }
  },
  laser: {
    name: '激光枪',
    damage: 25,
    fireRate: 500,
    spread: 0,
    ammo: 50,
    reloadTime: 3000,
    upgrades: { damage: [30, 38, 50] }
  }
}

export class Weapon {
  constructor(type) {
    this.type = type
    this.config = { ...WEAPONS[type] }
    this.level = 1
    this.currentAmmo = this.config.ammo
    this.lastFired = 0
    this.isReloading = false
  }

  fire(angle) {
    if (this.isReloading) return null
    if (this.currentAmmo <= 0) {
      this.reload()
      return null
    }

    const now = Date.now()
    if (now - this.lastFired < this.config.fireRate) return null

    this.lastFired = now
    if (this.config.ammo !== Infinity) this.currentAmmo--

    return this.createProjectile(angle)
  }

  createProjectile(angle) {
    const spread = (Math.random() - 0.5) * this.config.spread
    return {
      angle: angle + spread,
      damage: this.config.damage,
      speed: 500,
      type: this.type
    }
  }

  reload() {
    this.isReloading = true
    setTimeout(() => {
      this.currentAmmo = this.config.ammo
      this.isReloading = false
    }, this.config.reloadTime)
  }

  upgrade() {
    this.level++
    const upgrades = this.config.upgrades
    if (upgrades.damage && upgrades.damage[this.level - 2]) {
      this.config.damage = upgrades.damage[this.level - 2]
    }
  }
}
```

### 2. 怪物系统 (MonsterSystem.js)

```javascript
export const MONSTER_TYPES = {
  basic: {
    name: '普通怪物',
    health: 50,
    damage: 10,
    speed: 100,
    reward: 10,
    color: '#FF0000'
  },
  fast: {
    name: '快速怪物',
    health: 30,
    damage: 5,
    speed: 200,
    reward: 15,
    color: '#FFFF00'
  },
  tank: {
    name: '坦克怪物',
    health: 200,
    damage: 20,
    speed: 50,
    reward: 30,
    color: '#0000FF'
  },
  boss: {
    name: 'Boss怪物',
    health: 1000,
    damage: 50,
    speed: 30,
    reward: 100,
    color: '#FF00FF'
  }
}

export class WaveGenerator {
  constructor() {
    this.waveNumber = 0
  }

  generateWave() {
    this.waveNumber++
    const monsters = []

    if (this.waveNumber % 10 === 0) {
      // Boss 波次
      monsters.push(this.createMonster('boss', 1 + this.waveNumber / 10))
    } else {
      // 普通波次
      const count = 5 + this.waveNumber * 2
      for (let i = 0; i < count; i++) {
        const type = this.getRandomType()
        monsters.push(this.createMonster(type, this.waveNumber))
      }
    }

    return monsters
  }

  getRandomType() {
    const rand = Math.random()
    if (rand < 0.5) return 'basic'
    if (rand < 0.8) return 'fast'
    return 'tank'
  }

  createMonster(type, wave) {
    const config = MONSTER_TYPES[type]
    const scale = 1 + (wave - 1) * 0.1
    return {
      type,
      health: Math.floor(config.health * scale),
      maxHealth: Math.floor(config.health * scale),
      damage: Math.floor(config.damage * scale),
      speed: config.speed,
      reward: Math.floor(config.reward * scale),
      color: config.color
    }
  }
}
```

### 3. 防御工事 (DefenseSystem.js)

```javascript
export const DEFENSE_TYPES = {
  wall: {
    name: '墙壁',
    health: 500,
    cost: 50,
    size: { width: 40, height: 40 }
  },
  turret: {
    name: '炮塔',
    health: 200,
    cost: 200,
    damage: 10,
    fireRate: 1000,
    range: 200,
    size: { width: 30, height: 30 }
  },
  mine: {
    name: '地雷',
    health: 50,
    cost: 30,
    damage: 100,
    triggerRadius: 30,
    size: { width: 20, height: 20 }
  }
}

export class Defense {
  constructor(type, x, y) {
    this.type = type
    this.x = x
    this.y = y
    this.config = { ...DEFENSE_TYPES[type] }
    this.health = this.config.health
    this.lastFired = 0
  }

  update(monsters) {
    if (this.type === 'turret') {
      this.fireAtNearest(monsters)
    } else if (this.type === 'mine') {
      this.checkTrigger(monsters)
    }
  }

  fireAtNearest(monsters) {
    const now = Date.now()
    if (now - this.lastFired < this.config.fireRate) return

    let nearest = null
    let minDist = this.config.range

    for (const monster of monsters) {
      const dist = Math.hypot(monster.x - this.x, monster.y - this.y)
      if (dist < minDist) {
        minDist = dist
        nearest = monster
      }
    }

    if (nearest) {
      this.lastFired = now
      return { target: nearest, damage: this.config.damage }
    }
    return null
  }

  checkTrigger(monsters) {
    for (const monster of monsters) {
      const dist = Math.hypot(monster.x - this.x, monster.y - this.y)
      if (dist < this.config.triggerRadius) {
        monster.health -= this.config.damage
        this.health = 0
        return true
      }
    }
    return false
  }
}
```

## 游戏玩法

1. **手动射击**：鼠标瞄准，点击射击
2. **建造防御**：花费金币建造墙壁、炮塔
3. **抵御波次**：一波波怪物进攻
4. **升级武器**：用金币升级武器属性
5. **Boss 战**：每 10 波出现强力 Boss

## 扩展点

- 添加新武器：在 `WEAPONS` 中定义
- 添加新怪物：在 `MONSTER_TYPES` 中定义
- 添加新防御工事：在 `DEFENSE_TYPES` 中定义
- 添加技能系统：创建 `SkillManager`
- 添加成就系统：创建 `AchievementManager`
