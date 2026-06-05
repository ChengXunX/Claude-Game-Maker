---
name: game-template-tower-defense
description: 塔防游戏模板 - 提供完整的塔防游戏项目骨架
category: game-template
triggerPattern: tower defense, 塔防, defense, TD
---

# 塔防游戏模板

## 概述

这是一个完整的塔防游戏模板，基于 Phaser 3 引擎。包含：
- 波次系统（敌人波次生成）
- 塔防系统（多种塔类型、升级）
- 路径系统（敌人寻路）
- 经济系统（金币、建造、升级）
- UI 系统（生命值、金币、波次信息）

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
│   │   └── GameOverScene.js
│   ├── towers/
│   │   ├── Tower.js          # 塔基类
│   │   ├── ArrowTower.js     # 箭塔
│   │   ├── MagicTower.js     # 魔法塔
│   │   └── CannonTower.js    # 炮塔
│   ├── enemies/
│   │   ├── Enemy.js          # 敌人基类
│   │   ├── BasicEnemy.js     # 基础敌人
│   │   ├── FastEnemy.js      # 快速敌人
│   │   └── BossEnemy.js      # Boss敌人
│   ├── systems/
│   │   ├── WaveManager.js    # 波次管理
│   │   ├── PathManager.js    # 路径管理
│   │   └── EconomyManager.js # 经济管理
│   └── ui/
│       ├── HUD.js
│       └── TowerMenu.js
└── assets/
    ├── images/
    ├── sounds/
    └── levels/
```

## 核心代码模板

### 1. 塔基类 (Tower.js)

```javascript
export class Tower extends Phaser.GameObjects.Container {
  constructor(scene, x, y, config) {
    super(scene, x, y)

    this.config = config
    this.level = 1
    this.range = config.range
    this.damage = config.damage
    this.fireRate = config.fireRate
    this.lastFired = 0
    this.target = null

    // 塔的图像
    this.image = scene.add.image(0, 0, config.texture)
    this.add(this.image)

    // 范围指示器
    this.rangeCircle = scene.add.circle(0, 0, this.range, 0x00ff00, 0.1)
    this.add(this.rangeCircle)
    this.rangeCircle.setVisible(false)

    scene.add.existing(this)
  }

  update(time) {
    this.findTarget()
    if (this.target && time > this.lastFired + this.fireRate) {
      this.fire()
      this.lastFired = time
    }
  }

  findTarget() {
    this.target = null
    let closestDist = this.range

    this.scene.enemies.children.iterate(enemy => {
      if (!enemy || !enemy.active) return
      const dist = Phaser.Math.Distance.Between(this.x, this.y, enemy.x, enemy.y)
      if (dist < closestDist) {
        closestDist = dist
        this.target = enemy
      }
    })
  }

  fire() {
    if (!this.target) return

    const projectile = this.scene.physics.add.image(this.x, this.y, this.config.projectileTexture)
    this.scene.physics.moveTo(projectile, this.target.x, this.target.y, 300)

    this.scene.physics.add.overlap(projectile, this.target, () => {
      this.target.takeDamage(this.damage)
      projectile.destroy()
    })

    // 5秒后自动销毁子弹
    this.scene.time.delayedCall(5000, () => {
      if (projectile.active) projectile.destroy()
    })
  }

  upgrade() {
    this.level++
    this.damage *= 1.5
    this.range *= 1.1
    this.fireRate *= 0.9
    this.rangeCircle.setRadius(this.range)
  }

  showRange() {
    this.rangeCircle.setVisible(true)
  }

  hideRange() {
    this.rangeCircle.setVisible(false)
  }
}
```

### 2. 波次管理器 (WaveManager.js)

```javascript
export class WaveManager {
  constructor(scene) {
    this.scene = scene
    this.currentWave = 0
    this.enemiesRemaining = 0
    this.isWaveActive = false
    this.waves = this.generateWaves()
  }

  generateWaves() {
    const waves = []
    for (let i = 1; i <= 20; i++) {
      waves.push({
        enemies: this.generateWaveEnemies(i),
        reward: i * 50
      })
    }
    return waves
  }

  generateWaveEnemies(waveNum) {
    const enemies = []
    const count = 5 + waveNum * 2

    for (let i = 0; i < count; i++) {
      const type = waveNum % 5 === 0 ? 'boss' : (waveNum > 10 && Math.random() < 0.3 ? 'fast' : 'basic')
      enemies.push({
        type,
        delay: i * 1000
      })
    }

    return enemies
  }

  startWave() {
    if (this.isWaveActive) return

    this.currentWave++
    this.isWaveActive = true
    const wave = this.waves[this.currentWave - 1]

    this.enemiesRemaining = wave.enemies.length

    wave.enemies.forEach(enemyConfig => {
      this.scene.time.delayedCall(enemyConfig.delay, () => {
        this.spawnEnemy(enemyConfig.type)
      })
    })

    this.scene.events.emit('waveStart', this.currentWave)
  }

  spawnEnemy(type) {
    const enemy = this.scene.createEnemy(type)
    this.scene.enemies.add(enemy)
  }

  enemyDefeated() {
    this.enemiesRemaining--
    if (this.enemiesRemaining <= 0) {
      this.isWaveActive = false
      this.scene.events.emit('waveComplete', this.currentWave)
    }
  }
}
```

### 3. 游戏场景 (GameScene.js)

```javascript
export class GameScene extends Phaser.Scene {
  constructor() {
    super('GameScene')
  }

  create() {
    // 创建地图
    this.createMap()

    // 创建路径
    this.pathManager = new PathManager(this)

    // 创建波次管理器
    this.waveManager = new WaveManager(this)

    // 创建经济管理器
    this.economyManager = new EconomyManager(this)

    // 创建敌人组
    this.enemies = this.physics.add.group()

    // 创建塔组
    this.towers = []

    // 设置碰撞
    this.setupCollisions()

    // 创建UI
    this.hud = new HUD(this)
    this.towerMenu = new TowerMenu(this)

    // 输入
    this.input.on('pointerdown', this.handleClick, this)
  }

  update(time) {
    // 更新塔
    this.towers.forEach(tower => tower.update(time))

    // 更新敌人
    this.enemies.children.iterate(enemy => {
      if (enemy && enemy.active) enemy.update()
    })
  }

  createMap() {
    // 绘制网格地图
    this.grid = []
    for (let y = 0; y < 10; y++) {
      this.grid[y] = []
      for (let x = 0; x < 15; x++) {
        this.grid[y][x] = { type: 'empty', x: x * 64, y: y * 64 }
      }
    }

    // 设置路径
    const path = [[0,4],[1,4],[2,4],[3,4],[4,4],[5,4],[5,3],[5,2],[6,2],[7,2],[8,2],[9,2],[9,3],[9,4],[10,4],[11,4],[12,4],[13,4],[14,4]]
    path.forEach(([x, y]) => {
      this.grid[y][x].type = 'path'
    })

    // 渲染地图
    this.renderMap()
  }

  handleClick(pointer) {
    const gridX = Math.floor(pointer.x / 64)
    const gridY = Math.floor(pointer.y / 64)

    if (this.grid[gridY][gridX].type === 'empty') {
      this.towerMenu.show(gridX, gridY)
    }
  }

  placeTower(gridX, gridY, towerType) {
    const x = gridX * 64 + 32
    const y = gridY * 64 + 32

    const config = TOWER_CONFIGS[towerType]
    if (this.economyManager.gold < config.cost) {
      ElMessage.error('金币不足')
      return
    }

    this.economyManager.spend(config.cost)
    const tower = new Tower(this, x, y, config)
    this.towers.push(tower)
    this.grid[gridY][gridX].type = 'tower'
  }

  createEnemy(type) {
    const config = ENEMY_CONFIGS[type]
    const startPoint = this.pathManager.getStartPoint()
    return new Enemy(this, startPoint.x, startPoint.y, config)
  }

  setupCollisions() {
    // 敌人到达终点
    this.physics.add.overlap(this.enemies, this.endZone, (enemy) => {
      this.economyManager.loseLife()
      enemy.destroy()
    })
  }
}
```

## 使用方法

1. 使用此模板创建新项目
2. 替换 `assets/images/` 中的图片资源
3. 修改 `config.js` 中的塔和敌人配置
4. 在 `assets/levels/` 中添加地图数据
5. 运行 `npm run dev` 预览游戏

## 扩展点

- 添加新塔类型：继承 `Tower` 类
- 添加新敌人类型：继承 `Enemy` 类
- 添加特殊技能：在 `GameScene` 中实现
- 添加成就系统：创建 `AchievementManager`
