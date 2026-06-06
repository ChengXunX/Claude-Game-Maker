---
name: game-template-platformer
description: 平台跳跃游戏模板 - 提供完整的平台跳跃游戏项目骨架
category: game-template
triggerPattern: platformer, platform game, 跳跃, 横版, side-scrolling
---

# 平台跳跃游戏模板

## 概述

这是一个完整的平台跳跃游戏模板，基于 Phaser 3 引擎。包含：
- 角色控制（移动、跳跃、冲刺）
- 关卡系统（多关卡、检查点）
- 敌人系统（AI 行为、碰撞检测）
- UI 系统（生命值、分数、菜单）
- 音效系统

## 项目结构

```
game-project/
├── index.html          # 入口页面
├── package.json        # 依赖配置
├── vite.config.js      # 构建配置
├── src/
│   ├── main.js         # 游戏入口
│   ├── config.js       # 游戏配置
│   ├── scenes/
│   │   ├── BootScene.js    # 启动场景
│   │   ├── MenuScene.js    # 菜单场景
│   │   ├── GameScene.js    # 游戏场景
│   │   └── GameOverScene.js # 结束场景
│   ├── sprites/
│   │   ├── Player.js       # 玩家角色
│   │   ├── Enemy.js        # 敌人基类
│   │   └── Platform.js     # 平台
│   ├── objects/
│   │   ├── Coin.js         # 金币
│   │   ├── PowerUp.js      # 道具
│   │   └── Checkpoint.js   # 检查点
│   ├── ui/
│   │   ├── HUD.js          # 游戏内UI
│   │   └── Menu.js         # 菜单UI
│   └── utils/
│       ├── LevelManager.js # 关卡管理
│       └── SoundManager.js # 音效管理
└── assets/
    ├── images/             # 图片资源
    ├── sounds/             # 音效资源
    └── levels/             # 关卡数据
```

## 目录配置

| 目录路径 | 用途 | 可访问角色 | 说明 |
|---------|------|-----------|------|
| /src | 游戏源代码 | client-dev | 游戏主程序、场景 |
| /src/scenes | 游戏场景 | client-dev | 各个游戏场景的实现 |
| /src/sprites | 游戏精灵 | client-dev | 玩家、敌人、平台等 |
| /src/objects | 游戏对象 | client-dev | 金币、道具、检查点等 |
| /src/ui | UI组件 | client-dev, ui-dev | HUD、菜单等UI |
| /src/utils | 工具类 | client-dev | 关卡管理、音效管理等 |
| /assets | 资源文件 | ui-dev | 图片、音频、关卡文件 |
| /assets/levels | 关卡数据 | client-dev, system-planner | 关卡设计数据 |
| /config | 配置文件 | | 游戏配置（所有角色可访问） |
| /docs | 文档 | system-planner | 需求文档、设计文档 |

## 核心代码模板

### 1. 游戏配置 (config.js)

```javascript
export const GAME_CONFIG = {
  width: 800,
  height: 600,
  backgroundColor: '#2c3e50',
  physics: {
    default: 'arcade',
    arcade: {
      gravity: { y: 300 },
      debug: false
    }
  },
  player: {
    speed: 160,
    jumpForce: -330,
    dashSpeed: 300,
    maxHealth: 3,
    invincibleTime: 1500
  },
  enemies: {
    speed: 80,
    damage: 1
  }
}
```

### 2. 玩家角色 (Player.js)

```javascript
export class Player extends Phaser.Physics.Arcade.Sprite {
  constructor(scene, x, y) {
    super(scene, x, y, 'player')
    scene.add.existing(this)
    scene.physics.add.existing(this)

    this.setCollideWorldBounds(true)
    this.setBounce(0.1)

    this.health = GAME_CONFIG.player.maxHealth
    this.isInvincible = false
    this.canDash = true
    this.isDashing = false
  }

  update(cursors) {
    if (this.isDashing) return

    // 水平移动
    if (cursors.left.isDown) {
      this.setVelocityX(-GAME_CONFIG.player.speed)
      this.setFlipX(true)
    } else if (cursors.right.isDown) {
      this.setVelocityX(GAME_CONFIG.player.speed)
      this.setFlipX(false)
    } else {
      this.setVelocityX(0)
    }

    // 跳跃
    if (cursors.up.isDown && this.body.touching.down) {
      this.setVelocityY(GAME_CONFIG.player.jumpForce)
    }

    // 动画
    this.updateAnimation()
  }

  dash() {
    if (!this.canDash) return
    this.canDash = false
    this.isDashing = true

    const direction = this.flipX ? -1 : 1
    this.setVelocityX(GAME_CONFIG.player.dashSpeed * direction)

    this.scene.time.delayedCall(200, () => {
      this.isDashing = false
    })

    this.scene.time.delayedCall(1000, () => {
      this.canDash = true
    })
  }

  takeDamage(amount) {
    if (this.isInvincible) return

    this.health -= amount
    this.isInvincible = true

    // 闪烁效果
    this.scene.tweens.add({
      targets: this,
      alpha: 0.5,
      duration: 100,
      yoyo: true,
      repeat: 5
    })

    this.scene.time.delayedCall(GAME_CONFIG.player.invincibleTime, () => {
      this.isInvincible = false
      this.setAlpha(1)
    })

    if (this.health <= 0) {
      this.die()
    }
  }

  die() {
    this.setTint(0xff0000)
    this.setVelocity(0, -200)
    this.body.enable = false

    this.scene.time.delayedCall(1000, () => {
      this.scene.scene.start('GameOverScene', { score: this.scene.score })
    })
  }

  updateAnimation() {
    if (this.body.touching.down) {
      if (this.body.velocity.x !== 0) {
        this.play('run', true)
      } else {
        this.play('idle', true)
      }
    } else {
      this.play('jump', true)
    }
  }
}
```

### 3. 敌人基类 (Enemy.js)

```javascript
export class Enemy extends Phaser.Physics.Arcade.Sprite {
  constructor(scene, x, y, texture) {
    super(scene, x, y, texture)
    scene.add.existing(this)
    scene.physics.add.existing(this)

    this.setCollideWorldBounds(true)
    this.health = 1
    this.damage = GAME_CONFIG.enemies.damage
    this.speed = GAME_CONFIG.enemies.speed

    this.direction = 1
    this.patrolDistance = 200
    this.startX = x
  }

  update() {
    this.patrol()
  }

  patrol() {
    this.setVelocityX(this.speed * this.direction)

    if (Math.abs(this.x - this.startX) > this.patrolDistance) {
      this.direction *= -1
      this.setFlipX(this.direction < 0)
    }
  }

  takeDamage(amount) {
    this.health -= amount
    if (this.health <= 0) {
      this.die()
    }
  }

  die() {
    // 死亡动画
    this.scene.tweens.add({
      targets: this,
      alpha: 0,
      y: this.y + 50,
      duration: 300,
      onComplete: () => {
        this.destroy()
      }
    })

    // 掉落金币
    this.dropLoot()
  }

  dropLoot() {
    if (Math.random() < 0.3) {
      const coin = new Coin(this.scene, this.x, this.y)
      this.scene.coins.add(coin)
    }
  }
}
```

### 4. 游戏场景 (GameScene.js)

```javascript
export class GameScene extends Phaser.Scene {
  constructor() {
    super('GameScene')
  }

  init(data) {
    this.level = data.level || 1
    this.score = data.score || 0
  }

  create() {
    // 创建背景
    this.createBackground()

    // 创建地图
    this.createLevel()

    // 创建玩家
    this.player = new Player(this, 100, 450)

    // 创建敌人
    this.enemies = this.physics.add.group()
    this.createEnemies()

    // 创建金币
    this.coins = this.physics.add.group()
    this.createCoins()

    // 设置碰撞
    this.setupCollisions()

    // 创建UI
    this.hud = new HUD(this)

    // 输入控制
    this.cursors = this.input.keyboard.createCursorKeys()
    this.spaceKey = this.input.keyboard.addKey('SPACE')
    this.dashKey = this.input.keyboard.addKey('SHIFT')

    // 分数
    this.score = 0
  }

  update() {
    this.player.update(this.cursors)

    // 冲刺
    if (Phaser.Input.Keyboard.JustDown(this.dashKey)) {
      this.player.dash()
    }

    // 更新敌人
    this.enemies.children.iterate(enemy => {
      enemy.update()
    })

    // 更新UI
    this.hud.update(this.player.health, this.score)

    // 检查关卡完成
    this.checkLevelComplete()
  }

  createBackground() {
    // 视差背景
    this.bg1 = this.add.tileSprite(0, 0, 800, 600, 'bg1')
      .setOrigin(0, 0)
      .setScrollFactor(0)
  }

  createLevel() {
    // 从JSON加载关卡数据
    const levelData = this.cache.json.get(`level${this.level}`)

    this.platforms = this.physics.add.staticGroup()
    levelData.platforms.forEach(p => {
      this.platforms.create(p.x, p.y, 'platform')
    })
  }

  createEnemies() {
    const levelData = this.cache.json.get(`level${this.level}`)
    levelData.enemies.forEach(e => {
      const enemy = new Enemy(this, e.x, e.y, e.type)
      this.enemies.add(enemy)
    })
  }

  createCoins() {
    const levelData = this.cache.json.get(`level${this.level}`)
    levelData.coins.forEach(c => {
      const coin = new Coin(this, c.x, c.y)
      this.coins.add(coin)
    })
  }

  setupCollisions() {
    // 玩家与平台
    this.physics.add.collider(this.player, this.platforms)

    // 敌人与平台
    this.physics.add.collider(this.enemies, this.platforms)

    // 玩家与金币
    this.physics.add.overlap(this.player, this.coins, this.collectCoin, null, this)

    // 玩家与敌人
    this.physics.add.overlap(this.player, this.enemies, this.hitEnemy, null, this)
  }

  collectCoin(player, coin) {
    coin.collect()
    this.score += 10
  }

  hitEnemy(player, enemy) {
    if (player.body.velocity.y > 0 && player.y < enemy.y) {
      // 踩踏敌人
      enemy.takeDamage(1)
      player.setVelocityY(-200)
      this.score += 20
    } else {
      // 被敌人碰到
      player.takeDamage(enemy.damage)
    }
  }

  checkLevelComplete() {
    if (this.player.x > 750) {
      this.scene.start('GameScene', {
        level: this.level + 1,
        score: this.score
      })
    }
  }
}
```

## 使用方法

1. 使用此模板创建新项目
2. 替换 `assets/images/` 中的图片资源
3. 修改 `assets/levels/` 中的关卡数据
4. 调整 `config.js` 中的游戏参数
5. 运行 `npm run dev` 预览游戏

## 扩展点

- 添加新敌人类型：继承 `Enemy` 类
- 添加新道具：继承 `PowerUp` 类
- 添加新关卡：在 `assets/levels/` 中添加 JSON 文件
- 添加音效：在 `assets/sounds/` 中添加音频文件
