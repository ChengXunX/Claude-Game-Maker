---
name: game-template-party-coop
description: 派对合作游戏模板 - 类似胡闹厨房的多人合作游戏，支持多设备联机
category: game-template
triggerPattern: party, 派对, coop, 合作, overcooked, 胡闹厨房, multiplayer, 联机, cook, 烹饪, kitchen, 厨房
---

# 派对合作游戏模板（胡闹厨房风格）

## 概述

完整的多人合作派对游戏模板，类似《胡闹厨房》(Overcooked)。支持：
- **多人联机**：WebSocket 实时通信，支持 2-4 人
- **订单系统**：动态生成订单，限时完成
- **厨房系统**：切菜、烹饪、装盘、上菜流程
- **协作机制**：玩家分工合作，传递食材
- **关卡系统**：多种厨房布局，递增难度
- **计分系统**：小费、连击、评价

## 项目结构

```
game-party-coop/
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
│   │   ├── LobbyScene.js     # 大厅（创建/加入房间）
│   │   ├── GameScene.js      # 游戏场景
│   │   └── ResultScene.js    # 结果场景
│   ├── entities/
│   │   ├── Player.js          # 玩家角色
│   │   ├── Kitchen.js         # 厨房
│   │   ├── Station.js         # 工作台（切菜台、灶台等）
│   │   ├── Ingredient.js      # 食材
│   │   ├── Dish.js            # 菜品
│   │   └── Order.js           # 订单
│   ├── systems/
│   │   ├── OrderSystem.js     # 订单系统
│   │   ├── CookingSystem.js   # 烹饪系统
│   │   ├── ScoreSystem.js     # 计分系统
│   │   ├── TimerSystem.js     # 计时系统
│   │   └── SyncSystem.js      # 同步系统
│   ├── network/
│   │   ├── SocketManager.js   # Socket 管理
│   │   └── StateSync.js       # 状态同步
│   ├── data/
│   │   ├── recipes.js         # 食谱数据
│   │   ├── levels.js          # 关卡数据
│   │   └── ingredients.js     # 食材数据
│   └── ui/
│       ├── HUD.js
│       ├── OrderBoard.js      # 订单板
│       ├── ScoreBoard.js      # 计分板
│       └── MiniMap.js         # 小地图
└── assets/
```

## 核心代码

### 1. 游戏配置 (config.js)

```javascript
export const GAME_CONFIG = {
  width: 1200,
  height: 800,
  maxPlayers: 4,
  roundTime: 180,           // 回合时间（秒）
  orderTimeout: 60,         // 订单超时（秒）
  maxActiveOrders: 5,       // 最大活跃订单数
  tileSize: 48,             // 地图格子大小
  playerSpeed: 200,         // 玩家移动速度
  carryCapacity: 1,         // 携带食材数量
  chopTime: 3000,           // 切菜时间（毫秒）
  cookTime: 5000,           // 烹饪时间（毫秒）
  washTime: 2000,           // 洗碗时间（毫秒）
  scorePerDish: 50,         // 每道菜基础分
  comboBonus: 1.5,          // 连击加成倍数
  tipMultiplier: 0.1,       // 小费倍数（每剩余秒）
  levels: [
    { id: 1, name: '新手厨房', players: 2, orders: 5 },
    { id: 2, name: '双人厨房', players: 2, orders: 8 },
    { id: 3, name: '团队厨房', players: 4, orders: 12 },
    { id: 4, name: '混乱厨房', players: 4, orders: 15, obstacles: true },
    { id: 5, name: '地狱厨房', players: 4, orders: 20, obstacles: true, moving: true }
  ]
}
```

### 2. 食谱数据 (recipes.js)

```javascript
export const RECIPES = {
  salad: {
    name: '沙拉',
    ingredients: ['lettuce', 'tomato'],
    steps: ['chop', 'chop', 'plate'],
    time: 30,
    score: 30,
    difficulty: 1
  },
  burger: {
    name: '汉堡',
    ingredients: ['bun', 'patty', 'lettuce'],
    steps: ['cook', 'chop', 'assemble'],
    time: 45,
    score: 50,
    difficulty: 2
  },
  pizza: {
    name: '披萨',
    ingredients: ['dough', 'cheese', 'tomato'],
    steps: ['roll', 'assemble', 'cook'],
    time: 60,
    score: 70,
    difficulty: 3
  },
  sushi: {
    name: '寿司',
    ingredients: ['rice', 'fish', 'seaweed'],
    steps: ['cook', 'slice', 'roll'],
    time: 50,
    score: 60,
    difficulty: 2
  },
  steak: {
    name: '牛排',
    ingredients: ['beef', 'potato'],
    steps: ['cook', 'cook', 'plate'],
    time: 40,
    score: 80,
    difficulty: 3
  },
  pasta: {
    name: '意面',
    ingredients: ['pasta', 'tomato', 'cheese'],
    steps: ['cook', 'cook', 'plate'],
    time: 55,
    score: 65,
    difficulty: 2
  }
}
```

### 3. 玩家角色 (Player.js)

```javascript
export class Player extends Phaser.Physics.Arcade.Sprite {
  constructor(scene, x, y, playerData) {
    super(scene, x, y, 'player')
    this.playerId = playerData.id
    this.playerName = playerData.name
    this.playerIndex = playerData.index
    this.speed = GAME_CONFIG.playerSpeed
    this.carrying = null          // 携带的食材/菜品
    this.currentStation = null    // 当前交互的工作台
    this.isChopping = false
    this.isCooking = false
    this.score = 0
    this.dishesServed = 0

    // 玩家颜色
    this.colors = ['#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4']
    this.setTint(Phaser.Display.Color.HexStringToColor(this.colors[playerIndex]).color)
  }

  update(cursors) {
    if (this.isChopping || this.isCooking) return

    let vx = 0, vy = 0
    if (cursors.left.isDown) vx = -this.speed
    if (cursors.right.isDown) vx = this.speed
    if (cursors.up.isDown) vy = -this.speed
    if (cursors.down.isDown) vy = this.speed
    this.setVelocity(vx, vy)

    // 更新动画
    this.updateAnimation(vx, vy)
  }

  updateAnimation(vx, vy) {
    if (vx === 0 && vy === 0) {
      this.play('idle', true)
    } else if (Math.abs(vx) > Math.abs(vy)) {
      this.play(vx > 0 ? 'walk-right' : 'walk-left', true)
    } else {
      this.play(vy > 0 ? 'walk-down' : 'walk-up', true)
    }
  }

  // 交互：拿起/放下
  interact() {
    if (this.carrying) {
      this.tryPlace()
    } else {
      this.tryPickup()
    }
  }

  tryPickup() {
    if (!this.currentStation) return false

    const item = this.currentStation.takeItem()
    if (item) {
      this.carrying = item
      this.updateCarryDisplay()
      return true
    }
    return false
  }

  tryPlace() {
    if (!this.currentStation) return false

    const placed = this.currentStation.placeItem(this.carrying)
    if (placed) {
      this.carrying = null
      this.updateCarryDisplay()

      // 触发工作台处理
      this.currentStation.process()
      return true
    }
    return false
  }

  updateCarryDisplay() {
    // 更新头顶显示的食材图标
    if (this.carrySprite) this.carrySprite.destroy()

    if (this.carrying) {
      this.carrySprite = this.scene.add.image(0, -20, this.carrying.type)
      this.add(this.carrySprite)
    }
  }

  startChopping(station) {
    if (this.isChopping) return
    this.isChopping = true
    this.setVelocity(0, 0)

    // 进度条
    this.progressBar = this.scene.add.graphics()
    this.updateProgress(0)

    const chopInterval = setInterval(() => {
      this.chopProgress = (this.chopProgress || 0) + 10
      this.updateProgress(this.chopProgress)

      if (this.chopProgress >= 100) {
        clearInterval(chopInterval)
        this.finishChopping(station)
      }
    }, GAME_CONFIG.chopTime / 10)
  }

  finishChopping(station) {
    this.isChopping = false
    this.chopProgress = 0
    if (this.progressBar) this.progressBar.destroy()

    station.completeChop()
  }
}
```

### 4. 工作台 (Station.js)

```javascript
export class Station extends Phaser.GameObjects.Container {
  constructor(scene, x, y, type) {
    super(scene, x, y)
    this.stationType = type
    this.items = []
    this.processing = false
    this.processProgress = 0

    // 工作台图像
    this.image = scene.add.image(0, 0, `station-${type}`)
    this.add(this.image)

    // 交互提示
    this.interactHint = scene.add.text(0, -30, '', {
      fontSize: '12px',
      backgroundColor: '#00000088',
      color: '#fff',
      padding: { x: 4, y: 2 }
    })
    this.add(this.interactHint)
    this.interactHint.setVisible(false)

    scene.add.existing(this)
  }

  canAccept(item) {
    switch (this.stationType) {
      case 'chop':
        return item.needsChop && !this.processing
      case 'stove':
        return item.needsCook && !this.processing
      case 'plate':
        return item.readyToPlate
      case 'sink':
        return item.needsWash
      case 'trash':
        return true
      case 'counter':
        return this.items.length < 3
      default:
        return false
    }
  }

  placeItem(item) {
    if (!this.canAccept(item)) return false

    this.items.push(item)
    this.updateDisplay()
    return true
  }

  takeItem() {
    if (this.items.length === 0) return null
    if (this.processing) return null

    const item = this.items.pop()
    this.updateDisplay()
    return item
  }

  process() {
    if (this.processing || this.items.length === 0) return

    const item = this.items[this.items.length - 1]

    switch (this.stationType) {
      case 'chop':
        this.startChopping(item)
        break
      case 'stove':
        this.startCooking(item)
        break
      case 'plate':
        this.plateDish(item)
        break
    }
  }

  startChopping(item) {
    this.processing = true
    this.processProgress = 0

    // 播放切菜动画
    this.playChopAnimation()

    const interval = setInterval(() => {
      this.processProgress += 10
      this.updateProgressDisplay(this.processProgress)

      if (this.processProgress >= 100) {
        clearInterval(interval)
        item.chopped = true
        item.needsChop = false
        this.processing = false
        this.updateDisplay()
      }
    }, GAME_CONFIG.chopTime / 10)
  }

  startCooking(item) {
    this.processing = true
    this.processProgress = 0

    const interval = setInterval(() => {
      this.processProgress += 10
      this.updateProgressDisplay(this.processProgress)

      if (this.processProgress >= 100) {
        clearInterval(interval)
        item.cooked = true
        item.needsCook = false
        this.processing = false
        this.updateDisplay()
      }
    }, GAME_CONFIG.cookTime / 10)
  }
}
```

### 5. 订单系统 (OrderSystem.js)

```javascript
export class OrderSystem {
  constructor(scene) {
    this.scene = scene
    this.orders = []
    this.completedOrders = 0
    this.failedOrders = 0
    this.maxOrders = GAME_CONFIG.maxActiveOrders
    this.comboCount = 0
  }

  generateOrder() {
    if (this.orders.length >= this.maxOrders) return null

    const recipes = Object.keys(RECIPES)
    const recipeKey = recipes[Math.floor(Math.random() * recipes.length)]
    const recipe = RECIPES[recipeKey]

    const order = {
      id: `order-${Date.now()}-${Math.random().toString(36).substr(2, 5)}`,
      recipe: recipeKey,
      recipeName: recipe.name,
      ingredients: [...recipe.ingredients],
      status: 'pending',
      createdAt: Date.now(),
      timeout: GAME_CONFIG.orderTimeout * 1000,
      score: recipe.score
    }

    this.orders.push(order)
    this.scene.events.emit('newOrder', order)

    return order
  }

  completeOrder(orderId, servedDish) {
    const order = this.orders.find(o => o.id === orderId)
    if (!order) return null

    // 验证菜品
    if (!this.validateDish(order, servedDish)) {
      return { success: false, reason: 'wrong_dish' }
    }

    // 计算分数
    const elapsed = Date.now() - order.createdAt
    const remaining = Math.max(0, order.timeout - elapsed)
    const tip = Math.floor(remaining * GAME_CONFIG.tipMultiplier)

    // 连击加成
    this.comboCount++
    const comboMultiplier = Math.min(3, 1 + (this.comboCount - 1) * 0.5)

    const score = Math.floor((order.score + tip) * comboMultiplier)

    // 更新状态
    order.status = 'completed'
    order.completedAt = Date.now()
    order.scoreEarned = score

    this.completedOrders++

    this.scene.events.emit('orderCompleted', {
      order,
      score,
      combo: this.comboCount,
      tip
    })

    // 清理已完成订单
    this.orders = this.orders.filter(o => o.id !== orderId)

    return { success: true, score, combo: this.comboCount }
  }

  validateDish(order, dish) {
    if (!dish) return false
    const recipe = RECIPES[order.recipe]
    if (!recipe) return false

    // 检查食材是否匹配
    const required = [...recipe.ingredients].sort()
    const provided = [...dish.ingredients].sort()

    if (required.length !== provided.length) return false
    return required.every((ing, i) => ing === provided[i])
  }

  checkTimeouts() {
    const now = Date.now()
    const expired = this.orders.filter(o =>
      o.status === 'pending' && (now - o.createdAt) > o.timeout
    )

    expired.forEach(order => {
      order.status = 'failed'
      this.failedOrders++
      this.comboCount = 0 // 断连击
      this.scene.events.emit('orderExpired', order)
    })

    this.orders = this.orders.filter(o => o.status !== 'failed')
  }

  getActiveOrders() {
    return this.orders.filter(o => o.status === 'pending')
  }
}
```

### 6. 游戏场景 (GameScene.js)

```javascript
export class GameScene extends Phaser.Scene {
  constructor() {
    super('GameScene')
  }

  init(data) {
    this.roomId = data.roomId
    this.players = data.players
    this.levelId = data.levelId || 1
    this.level = GAME_CONFIG.levels.find(l => l.id === this.levelId)
  }

  create() {
    // 创建厨房地图
    this.createKitchen()

    // 创建玩家
    this.createPlayers()

    // 创建订单系统
    this.orderSystem = new OrderSystem(this)
    this.setupOrderGeneration()

    // 创建计分系统
    this.scoreSystem = new ScoreSystem(this)

    // 创建计时系统
    this.timerSystem = new TimerSystem(this, GAME_CONFIG.roundTime)
    this.timerSystem.onTimeUp(() => this.endRound())
    this.timerSystem.start()

    // 设置输入
    this.setupInput()

    // 设置碰撞
    this.setupCollisions()

    // 创建 UI
    this.hud = new HUD(this)
    this.orderBoard = new OrderBoard(this)
    this.scoreBoard = new ScoreBoard(this)

    // 网络同步
    this.setupNetworkSync()
  }

  createKitchen() {
    const levelData = this.getLevelData()

    // 创建地面
    this.add.tileSprite(0, 0, GAME_CONFIG.width, GAME_CONFIG.height, 'floor')

    // 创建墙壁
    this.walls = this.physics.add.staticGroup()
    levelData.walls.forEach(wall => {
      this.walls.create(wall.x, wall.y, 'wall')
    })

    // 创建工作台
    this.stations = []
    levelData.stations.forEach(stationData => {
      const station = new Station(this, stationData.x, stationData.y, stationData.type)
      this.stations.push(station)
    })

    // 创建上菜窗口
    this.servingWindow = new ServingWindow(this, levelData.servingWindow.x, levelData.servingWindow.y)

    // 创建障碍物（高级关卡）
    if (levelData.obstacles) {
      this.obstacles = this.physics.add.group()
      levelData.obstacles.forEach(obs => {
        const obstacle = this.physics.add.image(obs.x, obs.y, obs.type)
        obstacle.body.setImmovable(true)
        this.obstacles.add(obstacle)
      })
    }
  }

  createPlayers() {
    this.playerGroup = this.physics.add.group()
    const spawnPoints = this.getLevelData().spawnPoints

    this.players.forEach((playerData, index) => {
      const spawn = spawnPoints[index]
      const player = new Player(this, spawn.x, spawn.y, {
        ...playerData,
        index
      })

      this.playerGroup.add(player)

      if (playerData.isLocal) {
        this.localPlayer = player
      }
    })
  }

  setupInput() {
    // 键盘输入
    this.cursors = this.input.keyboard.createCursorKeys()
    this.spaceKey = this.input.keyboard.addKey('SPACE')
    this.eKey = this.input.keyboard.addKey('E')

    // 交互键
    this.eKey.on('down', () => {
      this.localPlayer?.interact()
    })

    // 丢弃键
    this.spaceKey.on('down', () => {
      this.localPlayer?.drop()
    })
  }

  setupNetworkSync() {
    // 同步玩家位置
    this.time.addEvent({
      delay: 50, // 20fps
      callback: () => {
        if (this.localPlayer) {
          SocketManager.sendMove({
            x: this.localPlayer.x,
            y: this.localPlayer.y,
            carrying: this.localPlayer.carrying?.type
          })
        }
      },
      loop: true
    })

    // 接收其他玩家位置
    SocketManager.onPlayerMoved((data) => {
      const player = this.findPlayer(data.playerId)
      if (player) {
        this.tweens.add({
          targets: player,
          x: data.x,
          y: data.y,
          duration: 50,
          ease: 'Linear'
        })
      }
    })
  }

  update() {
    this.localPlayer?.update(this.cursors)
    this.orderSystem?.checkTimeouts()
    this.hud?.update()
    this.orderBoard?.update()
  }

  endRound() {
    const stats = {
      completed: this.orderSystem.completedOrders,
      failed: this.orderSystem.failedOrders,
      totalScore: this.scoreSystem.totalScore,
      level: this.levelId
    }

    this.scene.start('ResultScene', stats)
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
  console.log(`Player connected: ${socket.id}`)

  socket.on('createRoom', (data) => {
    const room = {
      id: generateRoomId(),
      host: socket.id,
      players: [{ id: socket.id, name: data.playerName, ready: false }],
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
    if (!room) {
      socket.emit('error', { message: '房间不存在' })
      return
    }
    if (room.players.length >= 4) {
      socket.emit('error', { message: '房间已满' })
      return
    }

    room.players.push({ id: socket.id, name: data.playerName, ready: false })
    socket.join(room.id)
    socket.roomId = room.id
    io.to(room.id).emit('playerJoined', room)
  })

  socket.on('playerReady', () => {
    const room = rooms.get(socket.roomId)
    if (!room) return

    const player = room.players.find(p => p.id === socket.id)
    if (player) player.ready = true

    // 检查是否所有玩家准备就绪
    if (room.players.length >= 2 && room.players.every(p => p.ready)) {
      room.state = 'playing'
      io.to(room.id).emit('gameStart', { roomId: room.id, players: room.players })
    }
  })

  socket.on('playerMove', (data) => {
    socket.to(socket.roomId).emit('playerMoved', {
      playerId: socket.id,
      ...data
    })
  })

  socket.on('orderCompleted', (data) => {
    io.to(socket.roomId).emit('orderCompleted', {
      playerId: socket.id,
      ...data
    })
  })

  socket.on('dishServed', (data) => {
    io.to(socket.roomId).emit('dishServed', {
      playerId: socket.id,
      ...data
    })
  })

  socket.on('disconnect', () => {
    const room = rooms.get(socket.roomId)
    if (room) {
      room.players = room.players.filter(p => p.id !== socket.id)
      io.to(socket.roomId).emit('playerLeft', { playerId: socket.id })
      if (room.players.length === 0) {
        rooms.delete(socket.roomId)
      }
    }
  })
})

function generateRoomId() {
  return Math.random().toString(36).substr(2, 6).toUpperCase()
}
```

## 游戏玩法

1. **创建/加入房间**：玩家创建房间或输入房间号加入
2. **选择关卡**：房主选择厨房关卡
3. **游戏开始**：所有玩家准备后自动开始
4. **合作完成订单**：
   - 从冰箱取食材
   - 在切菜台切菜
   - 在灶台烹饪
   - 装盘后送到上菜窗口
5. **计分**：根据完成速度和连击计算分数
6. **结算**：时间结束后显示成绩

## 操作说明

| 按键 | 功能 |
|------|------|
| 方向键 | 移动 |
| E | 交互（拿起/放下/切菜） |
| 空格 | 丢弃食材 |

## 使用方法

1. 启动服务端 `node server/index.js`
2. 启动客户端 `npm run dev`
3. 多个浏览器/设备访问
4. 创建房间或加入房间
5. 开始游戏

## 扩展点

- 添加新食谱：在 `recipes.js` 中定义
- 添加新关卡：在 `levels.js` 中定义
- 添加新食材：在 `ingredients.js` 中定义
- 添加成就系统：创建 `AchievementManager`
- 添加排行榜：创建 `LeaderboardManager`
- 添加语音聊天：集成 WebRTC
