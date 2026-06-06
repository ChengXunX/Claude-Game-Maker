---
name: game-template-coop-adventure
description: 双人合作闯关游戏模板 - 支持双人本地/联机合作闯关
category: game-template
triggerPattern: coop, 合作, adventure, 冒险, 闯关, 双人, two player, puzzle, platform, 合作闯关
---

# 双人合作闯关游戏模板

## 概述

完整的双人合作闯关游戏模板，类似《双人成行》(It Takes Two)、《传送门骑士》(Portal Knights)。支持：
- **双人本地**：同屏分屏或共享屏幕
- **双人联机**：WebSocket 实时通信
- **合作机制**：互补能力、协作解谜、互相帮助
- **关卡系统**：多种关卡类型、递增难度
- **角色系统**：不同角色不同能力
- **道具系统**：共享道具、特殊道具

## 项目结构

```
game-coop-adventure/
├── index.html
├── package.json
├── server/
│   └── index.js              # 联机服务端
├── src/
│   ├── main.js
│   ├── config.js
│   ├── scenes/
│   │   ├── BootScene.js
│   │   ├── MenuScene.js
│   │   ├── LobbyScene.js     # 联机大厅
│   │   ├── GameScene.js      # 游戏场景
│   │   ├── ResultScene.js    # 结果场景
│   │   └── CutsceneScene.js  # 过场动画
│   ├── entities/
│   │   ├── Player.js          # 玩家基类
│   │   ├── PlayerA.js         # 玩家A（力量型）
│   │   ├── PlayerB.js         # 玩家B（敏捷型）
│   │   ├── Obstacle.js        # 障碍物
│   │   ├── Switch.js          # 开关
│   │   ├── Platform.js        # 平台
│   │   ├── Door.js            # 门
│   │   ├── Collectible.js     # 收集品
│   │   └── NPC.js             # NPC
│   ├── systems/
│   │   ├── PhysicsSystem.js   # 物理系统
│   │   ├── PuzzleSystem.js    # 谜题系统
│   │   ├── CheckpointSystem.js # 检查点系统
│   │   ├── DialogSystem.js    # 对话系统
│   │   └── CameraSystem.js    # 相机系统
│   ├── network/
│   │   ├── SocketManager.js   # Socket 管理
│   │   └── StateSync.js       # 状态同步
│   ├── data/
│   │   ├── levels.js          # 关卡数据
│   │   ├── puzzles.js         # 谜题数据
│   │   └── dialogs.js         # 对话数据
│   └── ui/
│       ├── HUD.js
│       ├── DialogBox.js
│       ├── PuzzleHint.js
│       └── MiniMap.js
└── assets/
```

## 目录配置

| 目录路径 | 用途 | 可访问角色 | 说明 |
|---------|------|-----------|------|
| /src | 游戏源代码 | client-dev | 游戏主程序、场景、实体 |
| /src/scenes | 游戏场景 | client-dev | 各个游戏场景的实现 |
| /src/entities | 游戏实体 | client-dev | 玩家、障碍物、NPC等实体类 |
| /src/systems | 游戏系统 | client-dev | 物理、谜题、检查点等系统 |
| /src/network | 网络模块 | client-dev | Socket管理、状态同步 |
| /src/data | 数据配置 | client-dev, system-planner | 关卡、谜题、对话数据 |
| /src/ui | UI组件 | client-dev, ui-dev | HUD、对话框、提示等UI |
| /server | 联机服务端 | server-dev | 联机服务端代码 |
| /assets | 资源文件 | ui-dev | 图片、音频资源 |
| /config | 配置文件 | | 游戏配置（所有角色可访问） |
| /docs | 文档 | system-planner | 需求文档、设计文档 |

## 核心代码

### 1. 游戏配置 (config.js)

```javascript
export const GAME_CONFIG = {
  width: 1280,
  height: 720,
  splitScreen: false,          // 是否分屏
  tileSize: 32,
  playerSpeed: 200,
  jumpForce: -400,
  gravity: 800,
  maxHealth: 100,
  respawnDelay: 3000,
  checkpointRadius: 50,
  interactionRadius: 60,
  camera: {
    followBoth: true,          // 相机跟随两个玩家
    smoothing: 0.1,
    minZoom: 0.5,
    maxZoom: 1.5
  },
  roles: {
    A: {
      name: '力量型',
      abilities: ['push', 'break', 'carry'],
      speed: 180,
      jumpForce: -380
    },
    B: {
      name: '敏捷型',
      abilities: ['dash', 'glide', 'fit'],
      speed: 220,
      jumpForce: -420
    }
  }
}
```

### 2. 玩家基类 (Player.js)

```javascript
export class Player extends Phaser.Physics.Arcade.Sprite {
  constructor(scene, x, y, role) {
    super(scene, x, y, `player-${role}`)
    this.role = role
    this.config = GAME_CONFIG.roles[role]
    this.speed = this.config.speed
    this.jumpForce = this.config.jumpForce
    this.health = GAME_CONFIG.maxHealth
    this.isDead = false
    this.canInteract = false
    this.currentInteractable = null
    this.checkpoint = { x, y }
    this.score = 0
    this.items = []

    scene.add.existing(this)
    scene.physics.add.existing(this)
    this.setCollideWorldBounds(true)
    this.setBounce(0.1)
  }

  update(cursors, keys) {
    if (this.isDead) return

    // 移动
    let vx = 0
    if (cursors.left.isDown) vx = -this.speed
    if (cursors.right.isDown) vx = this.speed
    this.setVelocityX(vx)

    // 跳跃
    if (cursors.up.isDown && this.body.touching.down) {
      this.setVelocityY(this.jumpForce)
    }

    // 特殊能力
    this.updateAbility(keys)

    // 动画
    this.updateAnimation(vx)
  }

  updateAbility(keys) {
    // 由子类实现
  }

  die() {
    if (this.isDead) return
    this.isDead = true
    this.setTint(0xff0000)
    this.setVelocity(0, -300)
    this.body.enable = false

    setTimeout(() => this.respawn(), GAME_CONFIG.respawnDelay)
  }

  respawn() {
    this.isDead = false
    this.clearTint()
    this.body.enable = true
    this.setPosition(this.checkpoint.x, this.checkpoint.y)
    this.health = GAME_CONFIG.maxHealth
  }

  saveCheckpoint(x, y) {
    this.checkpoint = { x, y }
  }
}
```

### 3. 力量型玩家 (PlayerA.js)

```javascript
export class PlayerA extends Player {
  constructor(scene, x, y) {
    super(scene, x, y, 'A')
    this.pushPower = 300
    this.canBreak = true
    this.carrying = null
  }

  updateAbility(keys) {
    // 推动能力
    if (keys.space.isDown) {
      this.push()
    }

    // 搬运能力
    if (keys.e.isDown) {
      this.carry()
    }
  }

  push() {
    // 检测前方是否有可推动的物体
    const direction = this.flipX ? -1 : 1
    const objects = this.scene.physics.overlapRect(
      this.x + (direction * 30), this.y - 20, 40, 40
    )

    objects.forEach(obj => {
      if (obj.gameObject?.canBePushed) {
        obj.gameObject.setVelocityX(this.pushPower * direction)
      }
    })
  }

  breakObject() {
    // 检测前方是否有可破坏的物体
    const direction = this.flipX ? -1 : 1
    const objects = this.scene.physics.overlapRect(
      this.x + (direction * 30), this.y - 20, 40, 40
    )

    objects.forEach(obj => {
      if (obj.gameObject?.canBeBroken) {
        obj.gameObject.destroy()
      }
    })
  }

  carry() {
    if (this.carrying) {
      // 放下
      this.carrying.setPosition(this.x, this.y - 30)
      this.carrying.body.enable = true
      this.carrying = null
    } else {
      // 捡起
      const direction = this.flipX ? -1 : 1
      const objects = this.scene.physics.overlapRect(
        this.x + (direction * 20), this.y - 30, 40, 40
      )

      for (const obj of objects) {
        if (obj.gameObject?.canBeCarried) {
          this.carrying = obj.gameObject
          this.carrying.body.enable = false
          break
        }
      }
    }
  }
}
```

### 4. 敏捷型玩家 (PlayerB.js)

```javascript
export class PlayerB extends Player {
  constructor(scene, x, y) {
    super(scene, x, y, 'B')
    this.canDash = true
    this.isDashing = false
    this.canGlide = true
    this.isGliding = false
  }

  updateAbility(keys) {
    // 冲刺
    if (keys.shift.isDown && this.canDash) {
      this.dash()
    }

    // 滑翔
    if (keys.space.isDown && !this.body.touching.down && this.canGlide) {
      this.glide()
    } else {
      this.isGliding = false
    }
  }

  dash() {
    if (!this.canDash) return
    this.canDash = false
    this.isDashing = true

    const direction = this.flipX ? -1 : 1
    this.setVelocityX(this.speed * 3 * direction)

    setTimeout(() => {
      this.isDashing = false
    }, 200)

    setTimeout(() => {
      this.canDash = true
    }, 2000)
  }

  glide() {
    this.isGliding = true
    this.setVelocityY(Math.max(this.body.velocity.y, -50))
  }

  fitSmallSpace() {
    // 缩小碰撞体以通过狭窄通道
    this.body.setSize(16, 24)
    this.body.setOffset(8, 8)
  }

  restoreSize() {
    this.body.setSize(24, 32)
    this.body.setOffset(4, 0)
  }
}
```

### 5. 关卡数据 (levels.js)

```javascript
export const LEVELS = [
  {
    id: 1,
    name: '新手教程',
    description: '学习基本操作',
    width: 2000,
    height: 800,
    background: 'forest',
    spawn: { A: { x: 100, y: 600 }, B: { x: 200, y: 600 } },
    goal: { x: 1900, y: 600 },
    platforms: [
      { x: 100, y: 700, width: 400, height: 32 },
      { x: 600, y: 650, width: 200, height: 32 },
      { x: 900, y: 600, width: 200, height: 32 },
      { x: 1200, y: 550, width: 300, height: 32 },
      { x: 1600, y: 700, width: 400, height: 32 }
    ],
    obstacles: [],
    puzzles: [],
    collectibles: [
      { x: 650, y: 620, type: 'star' },
      { x: 950, y: 570, type: 'star' },
      { x: 1300, y: 520, type: 'gem' }
    ],
    npcs: []
  },
  {
    id: 2,
    name: '协作桥梁',
    description: '一人按下开关，另一人通过',
    width: 3000,
    height: 1000,
    background: 'cave',
    spawn: { A: { x: 100, y: 800 }, B: { x: 200, y: 800 } },
    goal: { x: 2900, y: 600 },
    platforms: [
      { x: 100, y: 900, width: 500, height: 32 },
      { x: 700, y: 850, width: 200, height: 32 },
      { x: 1000, y: 800, width: 200, height: 32 },
      { x: 1400, y: 700, width: 300, height: 32 },
      { x: 2000, y: 600, width: 400, height: 32 },
      { x: 2500, y: 700, width: 500, height: 32 }
    ],
    obstacles: [
      { x: 1200, y: 800, type: 'gap', width: 200 }
    ],
    puzzles: [
      {
        type: 'switch-door',
        switch: { x: 1450, y: 670 },
        door: { x: 1800, y: 600, width: 32, height: 100 }
      }
    ],
    collectibles: [],
    npcs: []
  },
  {
    id: 3,
    name: '力量与敏捷',
    description: '利用各自能力通过障碍',
    width: 4000,
    height: 1200,
    background: 'mountain',
    spawn: { A: { x: 100, y: 1000 }, B: { x: 200, y: 1000 } },
    goal: { x: 3800, y: 400 },
    platforms: [
      { x: 100, y: 1100, width: 600, height: 32 },
      { x: 800, y: 1050, width: 200, height: 32 },
      { x: 1100, y: 1000, width: 200, height: 32 },
      { x: 1400, y: 900, width: 300, height: 32 },
      { x: 1800, y: 800, width: 200, height: 32 },
      { x: 2100, y: 700, width: 300, height: 32 },
      { x: 2500, y: 600, width: 200, height: 32 },
      { x: 2800, y: 500, width: 300, height: 32 },
      { x: 3200, y: 450, width: 200, height: 32 },
      { x: 3500, y: 500, width: 500, height: 32 }
    ],
    obstacles: [
      { x: 1500, y: 900, type: 'breakable', width: 50, height: 50 },
      { x: 2300, y: 700, type: 'pushable', width: 60, height: 60 },
      { x: 2600, y: 600, type: 'narrow', width: 40, height: 80 }
    ],
    puzzles: [
      {
        type: 'carry-stack',
        items: [
          { x: 1850, y: 770, type: 'box' },
          { x: 1900, y: 770, type: 'box' }
        ],
        target: { x: 1900, y: 750 }
      }
    ],
    collectibles: [
      { x: 1200, y: 970, type: 'star' },
      { x: 2200, y: 670, type: 'star' },
      { x: 3300, y: 420, type: 'gem' }
    ],
    npcs: []
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
    this.isOnline = data.isOnline || false
    this.roomId = data.roomId
  }

  create() {
    // 加载关卡
    this.level = LEVELS.find(l => l.id === this.levelId)
    if (!this.level) {
      this.scene.start('MenuScene')
      return
    }

    // 设置世界边界
    this.physics.world.setBounds(0, 0, this.level.width, this.level.height)

    // 创建背景
    this.createBackground()

    // 创建平台
    this.platforms = this.physics.add.staticGroup()
    this.createPlatforms()

    // 创建障碍物
    this.createObstacles()

    // 创建谜题
    this.createPuzzles()

    // 创建收集品
    this.collectibles = this.physics.add.group()
    this.createCollectibles()

    // 创建玩家
    this.createPlayers()

    // 设置相机
    this.setupCamera()

    // 设置碰撞
    this.setupCollisions()

    // 创建 UI
    this.hud = new HUD(this)

    // 设置输入
    this.setupInput()

    // 联机同步
    if (this.isOnline) {
      this.setupNetworkSync()
    }
  }

  createPlatforms() {
    this.level.platforms.forEach(p => {
      const platform = this.platforms.create(p.x + p.width/2, p.y + p.height/2, 'platform')
      platform.setDisplaySize(p.width, p.height)
      platform.refreshBody()
    })
  }

  createPlayers() {
    const spawn = this.level.spawn

    // 创建玩家A（力量型）
    this.playerA = new PlayerA(this, spawn.A.x, spawn.A.y)
    this.physics.add.collider(this.playerA, this.platforms)

    // 创建玩家B（敏捷型）
    this.playerB = new PlayerB(this, spawn.B.x, spawn.B.y)
    this.physics.add.collider(this.playerB, this.platforms)

    // 设置输入
    this.cursorsA = {
      left: this.input.keyboard.addKey('A'),
      right: this.input.keyboard.addKey('D'),
      up: this.input.keyboard.addKey('W'),
      down: this.input.keyboard.addKey('S')
    }
    this.keysA = {
      space: this.input.keyboard.addKey('F'),
      e: this.input.keyboard.addKey('G')
    }

    this.cursorsB = this.input.keyboard.createCursorKeys()
    this.keysB = {
      space: this.input.keyboard.addKey('SPACE'),
      shift: this.input.keyboard.addKey('SHIFT'),
      e: this.input.keyboard.addKey('E')
    }
  }

  setupCamera() {
    if (GAME_CONFIG.camera.followBoth) {
      // 相机跟随两个玩家中间
      this.cameraTarget = this.add.container(0, 0)
      this.cameraTarget.setAlpha(0)
      this.cameras.main.startFollow(this.cameraTarget, true,
        GAME_CONFIG.camera.smoothing, GAME_CONFIG.camera.smoothing)
    } else {
      this.cameras.main.startFollow(this.playerA, true)
    }
    this.cameras.main.setBounds(0, 0, this.level.width, this.level.height)
  }

  setupCollisions() {
    // 玩家与平台
    this.physics.add.collider(this.playerA, this.platforms)
    this.physics.add.collider(this.playerB, this.platforms)

    // 玩家与收集品
    this.physics.add.overlap(this.playerA, this.collectibles, this.collectItem, null, this)
    this.physics.add.overlap(this.playerB, this.collectibles, this.collectItem, null, this)

    // 玩家与检查点
    this.physics.add.overlap(this.playerA, this.checkpoints, this.saveCheckpoint, null, this)
    this.physics.add.overlap(this.playerB, this.checkpoints, this.saveCheckpoint, null, this)

    // 玩家与目标
    this.physics.add.overlap(this.playerA, this.goal, this.reachGoal, null, this)
    this.physics.add.overlap(this.playerB, this.goal, this.reachGoal, null, this)
  }

  update() {
    // 更新玩家
    this.playerA.update(this.cursorsA, this.keysA)
    this.playerB.update(this.cursorsB, this.keysB)

    // 更新相机位置
    if (GAME_CONFIG.camera.followBoth && this.cameraTarget) {
      const midX = (this.playerA.x + this.playerB.x) / 2
      const midY = (this.playerA.y + this.playerB.y) / 2
      this.cameraTarget.setPosition(midX, midY)
    }

    // 更新 HUD
    this.hud.update()
  }

  collectItem(player, item) {
    item.collect()
    player.score += item.value || 10
  }

  saveCheckpoint(player, checkpoint) {
    player.saveCheckpoint(checkpoint.x, checkpoint.y)
    checkpoint.activate()
  }

  reachGoal(player) {
    // 两个玩家都到达目标才算完成
    const distA = Phaser.Math.Distance.Between(
      this.playerA.x, this.playerA.y, this.level.goal.x, this.level.goal.y)
    const distB = Phaser.Math.Distance.Between(
      this.playerB.x, this.playerB.y, this.level.goal.x, this.level.goal.y)

    if (distA < 50 && distB < 50) {
      this.levelComplete()
    }
  }

  levelComplete() {
    const score = this.playerA.score + this.playerB.score
    this.scene.start('ResultScene', {
      levelId: this.levelId,
      score: score,
      completed: true
    })
  }
}
```

### 7. Socket.IO 服务端 (server/index.js)

```javascript
const io = require('socket.io')(3000, {
  cors: { origin: '*' }
})

const rooms = new Map()

io.on('connection', (socket) => {
  socket.on('createRoom', (data) => {
    const room = {
      id: generateRoomId(),
      host: socket.id,
      players: [{ id: socket.id, name: data.playerName, role: 'A' }],
      levelId: data.levelId || 1,
      state: 'waiting'
    }
    rooms.set(room.id, room)
    socket.join(room.id)
    socket.roomId = room.id
    socket.emit('roomCreated', room)
  })

  socket.on('joinRoom', (data) => {
    const room = rooms.get(data.roomId)
    if (!room || room.players.length >= 2) {
      socket.emit('error', { message: '无法加入' })
      return
    }

    room.players.push({ id: socket.id, name: data.playerName, role: 'B' })
    socket.join(room.id)
    socket.roomId = room.id
    io.to(room.id).emit('playerJoined', room)
  })

  socket.on('startGame', () => {
    const room = rooms.get(socket.roomId)
    if (room && room.players.length === 2) {
      room.state = 'playing'
      io.to(room.id).emit('gameStart', {
        roomId: room.id,
        levelId: room.levelId,
        players: room.players
      })
    }
  })

  socket.on('playerMove', (data) => {
    socket.to(socket.roomId).emit('playerMoved', {
      playerId: socket.id,
      ...data
    })
  })

  socket.on('playerAction', (data) => {
    socket.to(socket.roomId).emit('playerAction', {
      playerId: socket.id,
      ...data
    })
  })

  socket.on('puzzleSolved', (data) => {
    io.to(socket.roomId).emit('puzzleSolved', data)
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

function generateRoomId() {
  return Math.random().toString(36).substr(2, 6).toUpperCase()
}
```

## 游戏玩法

1. **选择模式**：本地双人 或 联机双人
2. **选择关卡**：选择要挑战的关卡
3. **合作闯关**：
   - 玩家A（力量型）：推动重物、破坏障碍、搬运物品
   - 玩家B（敏捷型）：冲刺、滑翔、穿过狭窄空间
4. **解谜**：利用各自能力协作解开谜题
5. **收集**：收集星星和宝石获得额外分数
6. **到达终点**：两人都到达终点门完成关卡

## 操作说明

| 操作 | 玩家A | 玩家B |
|------|-------|-------|
| 移动 | A/D | 左/右箭头 |
| 跳跃 | W | 上箭头 |
| 能力1 | F（推动） | 空格（滑翔） |
| 能力2 | G（搬运） | Shift（冲刺） |
| 交互 | - | E |

## 使用方法

1. 启动服务端 `node server/index.js`（联机模式）
2. 启动客户端 `npm run dev`
3. 选择本地或联机模式
4. 开始游戏

## 扩展点

- 添加新关卡：在 `levels.js` 中定义
- 添加新谜题类型：在 `puzzles.js` 中定义
- 添加新角色能力：继承 `Player` 类
- 添加过场动画：创建 `CutsceneScene`
- 添加成就系统：创建 `AchievementManager`
