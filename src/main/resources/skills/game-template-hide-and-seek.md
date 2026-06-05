---
name: game-template-hide-and-seek
description: 躲猫猫游戏模板 - 提供完整的躲猫猫游戏项目骨架
category: game-template
triggerPattern: hide and seek, 躲猫猫, 捉迷藏, 捕捉, 追逐, chase, seeker, hider
---

# 躲猫猫游戏模板

## 概述

多人躲猫猫游戏模板，基于 Phaser 3 + Socket.IO。包含：
- 角色系统（躲藏者、寻找者）
- 地图系统（多房间、障碍物、躲藏点）
- 视野系统（视野范围、遮挡检测）
- 道具系统（隐身、加速、陷阱）
- 计时系统（回合时间、倒计时）
- 多人实时对战

## 项目结构

```
game-hide-seek/
├── index.html
├── package.json
├── server/
│   └── index.js              # Socket.IO 服务端
├── src/
│   ├── main.js
│   ├── config.js
│   ├── scenes/
│   │   ├── BootScene.js
│   │   ├── MenuScene.js
│   │   ├── LobbyScene.js     # 大厅
│   │   ├── GameScene.js      # 游戏场景
│   │   └── ResultScene.js    # 结果场景
│   ├── entities/
│   │   ├── Player.js          # 玩家基类
│   │   ├── Hider.js           # 躲藏者
│   │   ├── Seeker.js          # 寻找者
│   │   └── Prop.js            # 道具
│   ├── systems/
│   │   ├── VisionSystem.js    # 视野系统
│   │   ├── HideSystem.js      # 躲藏系统
│   │   ├── PropSystem.js      # 道具系统
│   │   └── TimerSystem.js     # 计时系统
│   ├── map/
│   │   ├── GameMap.js         # 地图管理
│   │   └── MapObjects.js      # 地图物件
│   └── ui/
│       ├── HUD.js
│       ├── MiniMap.js
│       └── ChatBox.js
└── assets/
```

## 核心代码

### 1. 游戏配置 (config.js)

```javascript
export const GAME_CONFIG = {
  width: 800,
  height: 600,
  roundTime: 180,        // 回合时间（秒）
  seekerDelay: 10,       // 寻找者出发延迟（秒）
  visionRange: 150,      // 视野范围
  seekerVisionRange: 200,
  playerSpeed: 150,
  seekerSpeed: 180,
  maxPlayers: 8,
  minPlayers: 2,
  props: {
    speedBoost: { duration: 5, multiplier: 1.5 },
    invisibility: { duration: 8 },
    trap: { duration: 30, slowMultiplier: 0.5 }
  }
}
```

### 2. 躲藏者 (Hider.js)

```javascript
export class Hider extends Phaser.Physics.Arcade.Sprite {
  constructor(scene, x, y, playerId, playerName) {
    super(scene, x, y, 'hider')
    this.playerId = playerId
    this.playerName = playerName
    this.isHiding = false
    this.hideSpot = null
    this.isInvisible = false
    this.speed = GAME_CONFIG.playerSpeed
    this.health = 1
    this.props = []
    this.score = 0
  }

  update(cursors) {
    if (this.isHiding) return

    let vx = 0, vy = 0
    if (cursors.left.isDown) vx = -this.speed
    if (cursors.right.isDown) vx = this.speed
    if (cursors.up.isDown) vy = -this.speed
    if (cursors.down.isDown) vy = this.speed
    this.setVelocity(vx, vy)

    // 检查是否可以躲藏
    this.checkHideSpot()
  }

  checkHideSpot() {
    const hideSpots = this.scene.hideSpots.getChildren()
    for (const spot of hideSpots) {
      if (this.scene.physics.overlap(this, spot)) {
        this.canHide = true
        this.currentHideSpot = spot
        return
      }
    }
    this.canHide = false
    this.currentHideSpot = null
  }

  hide() {
    if (!this.canHide || !this.currentHideSpot) return false

    this.isHiding = true
    this.hideSpot = this.currentHideSpot
    this.setVelocity(0, 0)
    this.setVisible(false)
    this.body.enable = false

    return true
  }

  unhide() {
    if (!this.isHiding) return

    this.isHiding = false
    this.hideSpot = null
    this.setVisible(true)
    this.body.enable = true
  }

  useProp(propType) {
    const prop = this.props.find(p => p.type === propType && p.count > 0)
    if (!prop) return false

    prop.count--

    switch (propType) {
      case 'speedBoost':
        this.applySpeedBoost()
        break
      case 'invisibility':
        this.applyInvisibility()
        break
      case 'trap':
        this.placeTrap()
        break
    }

    return true
  }

  applySpeedBoost() {
    const originalSpeed = this.speed
    this.speed *= GAME_CONFIG.props.speedBoost.multiplier
    this.scene.time.delayedCall(GAME_CONFIG.props.speedBoost.duration * 1000, () => {
      this.speed = originalSpeed
    })
  }

  applyInvisibility() {
    this.isInvisible = true
    this.setAlpha(0.3)
    this.scene.time.delayedCall(GAME_CONFIG.props.invisibility.duration * 1000, () => {
      this.isInvisible = false
      this.setAlpha(1)
    })
  }

  placeTrap() {
    const trap = this.scene.traps.create(this.x, this.y, 'trap')
    trap.owner = this.playerId
    trap.lifespan = GAME_CONFIG.props.trap.duration * 1000
  }

  caught() {
    this.health = 0
    this.setTint(0xff0000)
    this.setVelocity(0, 0)
    this.scene.events.emit('hiderCaught', { playerId: this.playerId })
  }
}
```

### 3. 寻找者 (Seeker.js)

```javascript
export class Seeker extends Phaser.Physics.Arcade.Sprite {
  constructor(scene, x, y, playerId, playerName) {
    super(scene, x, y, 'seeker')
    this.playerId = playerId
    this.playerName = playerName
    this.speed = GAME_CONFIG.seekerSpeed
    this.visionRange = GAME_CONFIG.seekerVisionRange
    this.caughtCount = 0
    this.cooldown = 0
  }

  update(cursors) {
    let vx = 0, vy = 0
    if (cursors.left.isDown) vx = -this.speed
    if (cursors.right.isDown) vx = this.speed
    if (cursors.up.isDown) vy = -this.speed
    if (cursors.down.isDown) vy = this.speed
    this.setVelocity(vx, vy)

    // 更新冷却
    if (this.cooldown > 0) {
      this.cooldown -= 16 // 假设60fps
    }

    // 检测附近的躲藏者
    this.detectHiders()
  }

  detectHiders() {
    const hiders = this.scene.hiders.getChildren()
    for (const hider of hiders) {
      if (hider.isHiding || hider.isInvisible || hider.health <= 0) continue

      const distance = Phaser.Math.Distance.Between(this.x, this.y, hider.x, hider.y)
      if (distance <= this.visionRange) {
        // 检查是否有遮挡
        if (!this.isBlocked(hider)) {
          this.scene.events.emit('hiderDetected', {
            seekerId: this.playerId,
            hiderId: hider.playerId
          })
        }
      }
    }
  }

  isBlocked(target) {
    // 简化的遮挡检测
    const obstacles = this.scene.obstacles.getChildren()
    for (const obstacle of obstacles) {
      const line = new Phaser.Geom.Line(this.x, this.y, target.x, target.y)
      const rect = obstacle.getBounds()
      if (Phaser.Geom.Intersects.LineToRectangle(line, rect)) {
        return true
      }
    }
    return false
  }

  catch(hider) {
    if (this.cooldown > 0) return false
    if (hider.isHiding || hider.isInvisible) return false

    const distance = Phaser.Math.Distance.Between(this.x, this.y, hider.x, hider.y)
    if (distance > 50) return false

    hider.caught()
    this.caughtCount++
    this.cooldown = 2000 // 2秒冷却

    return true
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
    this.roomId = data.roomId
    this.players = data.players
    this.isSeeker = data.isSeeker
  }

  create() {
    // 创建地图
    this.gameMap = new GameMap(this)
    this.gameMap.loadMap('level1')

    // 创建玩家
    this.createPlayers()

    // 创建道具
    this.propSystem = new PropSystem(this)
    this.propSystem.spawnProps()

    // 创建视野系统
    this.visionSystem = new VisionSystem(this)

    // 创建计时系统
    this.timerSystem = new TimerSystem(this, GAME_CONFIG.roundTime)
    this.timerSystem.onTimeUp(() => this.endRound())
    this.timerSystem.start()

    // 设置碰撞
    this.setupCollisions()

    // 输入
    this.cursors = this.input.keyboard.createCursorKeys()
    this.spaceKey = this.input.keyboard.addKey('SPACE')
    this.eKey = this.input.keyboard.addKey('E')

    // UI
    this.hud = new HUD(this)
  }

  update() {
    this.localPlayer?.update(this.cursors)
    this.visionSystem.update()
    this.timerSystem.update()
    this.hud.update()
  }

  createPlayers() {
    this.hiders = this.add.group()
    this.seekers = this.add.group()

    this.players.forEach(playerData => {
      const spawnPoint = this.gameMap.getSpawnPoint(playerData.role)

      if (playerData.role === 'seeker') {
        const seeker = new Seeker(this, spawnPoint.x, spawnPoint.y, playerData.id, playerData.name)
        this.seekers.add(seeker)
        if (playerData.isLocal) this.localPlayer = seeker
      } else {
        const hider = new Hider(this, spawnPoint.x, spawnPoint.y, playerData.id, playerData.name)
        this.hiders.add(hider)
        if (playerData.isLocal) this.localPlayer = hider
      }
    })
  }

  setupCollisions() {
    // 玩家与障碍物
    this.physics.add.collider(this.hiders, this.obstacles)
    this.physics.add.collider(this.seekers, this.obstacles)

    // 寻找者与躲藏者
    this.physics.add.overlap(this.seekers, this.hiders, (seeker, hider) => {
      seeker.catch(hider)
    })

    // 躲藏者与道具
    this.physics.add.overlap(this.hiders, this.props, (hider, prop) => {
      hider.pickupProp(prop)
      prop.destroy()
    })
  }

  endRound() {
    const hidersAlive = this.hiders.getChildren().filter(h => h.health > 0).length
    const seekersWon = hidersAlive === 0

    this.scene.start('ResultScene', {
      hidersAlive,
      seekersWon,
      caughtCount: this.seekers.getChildren()[0]?.caughtCount || 0
    })
  }
}
```

### 5. Socket.IO 服务端 (server/index.js)

```javascript
const io = require('socket.io')(3000, {
  cors: { origin: '*' }
})

const rooms = new Map()

io.on('connection', (socket) => {
  console.log(`Player connected: ${socket.id}`)

  socket.on('joinRoom', (data) => {
    const { roomId, playerName } = data
    let room = rooms.get(roomId)

    if (!room) {
      room = { id: roomId, players: [], state: 'waiting' }
      rooms.set(roomId, room)
    }

    const player = {
      id: socket.id,
      name: playerName,
      role: room.players.length === 0 ? 'seeker' : 'hider',
      isReady: false
    }

    room.players.push(player)
    socket.join(roomId)
    socket.roomId = roomId

    io.to(roomId).emit('playerJoined', player)
    io.to(roomId).emit('roomState', room)
  })

  socket.on('playerMove', (data) => {
    socket.to(socket.roomId).emit('playerMoved', {
      playerId: socket.id,
      x: data.x,
      y: data.y
    })
  })

  socket.on('playerHide', () => {
    socket.to(socket.roomId).emit('playerHidden', { playerId: socket.id })
  })

  socket.on('playerCaught', (data) => {
    io.to(socket.roomId).emit('hiderCaught', {
      hiderId: data.hiderId,
      seekerId: socket.id
    })
  })

  socket.on('disconnect', () => {
    const room = rooms.get(socket.roomId)
    if (room) {
      room.players = room.players.filter(p => p.id !== socket.id)
      io.to(socket.roomId).emit('playerLeft', { playerId: socket.id })
      if (room.players.length === 0) rooms.delete(socket.roomId)
    }
  })
})
```

## 使用方法

1. 使用此模板创建新项目
2. 启动服务端 `node server/index.js`
3. 启动客户端 `npm run dev`
4. 多个浏览器访问开始游戏

## 扩展点

- 添加新地图：在 `assets/maps/` 中添加地图数据
- 添加新道具：在 `PropSystem.js` 中实现
- 添加新角色技能：继承 `Player` 类
- 添加排行榜：创建 `LeaderboardManager`
