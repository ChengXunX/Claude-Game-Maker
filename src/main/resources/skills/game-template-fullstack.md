---
name: game-template-fullstack
description: 全栈游戏模板 - 包含前端游戏和后端服务的完整项目骨架
category: game-template
triggerPattern: fullstack, 全栈, multiplayer, 多人, online, 在线, server, 后端
---

# 全栈游戏模板

## 概述

这是一个完整的全栈游戏模板，包含：
- **前端**：Phaser 3 游戏引擎 + Vue 3 管理界面
- **后端**：Node.js + Express + Socket.IO
- **数据库**：MongoDB / PostgreSQL
- **实时通信**：WebSocket 双向通信
- **用户系统**：注册、登录、排行榜
- **部署**：Docker + Nginx

## 项目结构

```
game-fullstack/
├── README.md
├── docker-compose.yml
├── .env.example
│
├── client/                    # 前端游戏客户端
│   ├── index.html
│   ├── package.json
│   ├── vite.config.js
│   ├── src/
│   │   ├── main.js
│   │   ├── config.js
│   │   ├── scenes/
│   │   │   ├── BootScene.js
│   │   │   ├── MenuScene.js
│   │   │   ├── GameScene.js
│   │   │   └── LobbyScene.js
│   │   ├── network/
│   │   │   ├── SocketManager.js    # WebSocket 管理
│   │   │   └── ApiClient.js        # REST API 客户端
│   │   ├── entities/
│   │   │   ├── Player.js
│   │   │   └── RemotePlayer.js     # 远程玩家
│   │   └── ui/
│   │       ├── HUD.js
│   │       ├── ChatBox.js
│   │       └── Leaderboard.js
│   └── assets/
│
├── server/                    # 后端服务
│   ├── package.json
│   ├── src/
│   │   ├── index.js           # 服务入口
│   │   ├── config.js          # 配置
│   │   ├── routes/
│   │   │   ├── auth.js        # 认证路由
│   │   │   ├── game.js        # 游戏路由
│   │   │   └── leaderboard.js # 排行榜路由
│   │   ├── models/
│   │   │   ├── User.js        # 用户模型
│   │   │   ├── GameRoom.js    # 游戏房间
│   │   │   └── Score.js       # 分数记录
│   │   ├── services/
│   │   │   ├── AuthService.js
│   │   │   ├── GameService.js
│   │   │   └── MatchService.js
│   │   ├── socket/
│   │   │   ├── SocketHandler.js
│   │   │   └── GameRoom.js
│   │   └── middleware/
│   │       ├── auth.js
│   │       └── rateLimit.js
│   └── .env
│
└── database/                  # 数据库脚本
    ├── init.sql
    └── seed.sql
```

## 目录配置

| 目录路径 | 用途 | 可访问角色 | 说明 |
|---------|------|-----------|------|
| /client | 前端游戏客户端 | client-dev, ui-dev | 游戏主程序、场景、实体、UI |
| /server | 后端服务 | server-dev | API接口、业务逻辑、数据库模型 |
| /database | 数据库脚本 | server-dev | 数据库初始化、种子数据 |
| /config | 配置文件 | | 游戏配置、环境变量（所有角色可访问） |
| /docs | 文档 | system-planner | 需求文档、设计文档、API文档 |
| /proto | 接口定义 | client-dev, server-dev | 前后端接口协议定义 |

## 核心代码模板

### 1. 后端入口 (server/src/index.js)

```javascript
const express = require('express')
const http = require('http')
const { Server } = require('socket.io')
const cors = require('cors')
const mongoose = require('mongoose')
require('dotenv').config()

const authRoutes = require('./routes/auth')
const gameRoutes = require('./routes/game')
const leaderboardRoutes = require('./routes/leaderboard')
const { setupSocketHandlers } = require('./socket/SocketHandler')

const app = express()
const server = http.createServer(app)
const io = new Server(server, {
  cors: {
    origin: process.env.CLIENT_URL || 'http://localhost:5173',
    methods: ['GET', 'POST']
  }
})

// 中间件
app.use(cors())
app.use(express.json())

// 路由
app.use('/api/auth', authRoutes)
app.use('/api/game', gameRoutes)
app.use('/api/leaderboard', leaderboardRoutes)

// WebSocket 处理
setupSocketHandlers(io)

// 数据库连接
mongoose.connect(process.env.MONGODB_URI || 'mongodb://localhost:27017/game')
  .then(() => console.log('MongoDB 连接成功'))
  .catch(err => console.error('MongoDB 连接失败:', err))

// 启动服务器
const PORT = process.env.PORT || 3000
server.listen(PORT, () => {
  console.log(`服务器运行在端口 ${PORT}`)
})
```

### 2. 认证路由 (server/src/routes/auth.js)

```javascript
const express = require('express')
const router = express.Router()
const jwt = require('jsonwebtoken')
const bcrypt = require('bcryptjs')
const User = require('../models/User')

// 注册
router.post('/register', async (req, res) => {
  try {
    const { username, password, email } = req.body

    // 检查用户是否存在
    const existingUser = await User.findOne({ username })
    if (existingUser) {
      return res.status(400).json({ error: '用户名已存在' })
    }

    // 创建用户
    const hashedPassword = await bcrypt.hash(password, 10)
    const user = new User({
      username,
      password: hashedPassword,
      email
    })
    await user.save()

    // 生成 Token
    const token = jwt.sign(
      { userId: user._id, username: user.username },
      process.env.JWT_SECRET || 'secret',
      { expiresIn: '7d' }
    )

    res.json({ token, user: { id: user._id, username: user.username } })
  } catch (error) {
    res.status(500).json({ error: '注册失败' })
  }
})

// 登录
router.post('/login', async (req, res) => {
  try {
    const { username, password } = req.body

    // 查找用户
    const user = await User.findOne({ username })
    if (!user) {
      return res.status(401).json({ error: '用户名或密码错误' })
    }

    // 验证密码
    const validPassword = await bcrypt.compare(password, user.password)
    if (!validPassword) {
      return res.status(401).json({ error: '用户名或密码错误' })
    }

    // 生成 Token
    const token = jwt.sign(
      { userId: user._id, username: user.username },
      process.env.JWT_SECRET || 'secret',
      { expiresIn: '7d' }
    )

    res.json({ token, user: { id: user._id, username: user.username } })
  } catch (error) {
    res.status(500).json({ error: '登录失败' })
  }
})

module.exports = router
```

### 3. WebSocket 处理 (server/src/socket/SocketHandler.js)

```javascript
const GameRoom = require('./GameRoom')

const rooms = new Map()
const players = new Map()

function setupSocketHandlers(io) {
  io.on('connection', (socket) => {
    console.log(`玩家连接: ${socket.id}`)

    // 加入房间
    socket.on('joinRoom', (data) => {
      const { roomId, player } = data

      let room = rooms.get(roomId)
      if (!room) {
        room = new GameRoom(roomId)
        rooms.set(roomId, room)
      }

      room.addPlayer(socket.id, player)
      players.set(socket.id, { roomId, player })

      socket.join(roomId)
      io.to(roomId).emit('playerJoined', { playerId: socket.id, player })
      io.to(roomId).emit('roomState', room.getState())
    })

    // 玩家移动
    socket.on('playerMove', (data) => {
      const playerData = players.get(socket.id)
      if (!playerData) return

      const room = rooms.get(playerData.roomId)
      if (room) {
        room.updatePlayerPosition(socket.id, data.position)
        socket.to(playerData.roomId).emit('playerMoved', {
          playerId: socket.id,
          position: data.position
        })
      }
    })

    // 玩家动作
    socket.on('playerAction', (data) => {
      const playerData = players.get(socket.id)
      if (!playerData) return

      const room = rooms.get(playerData.roomId)
      if (room) {
        room.handlePlayerAction(socket.id, data.action)
        io.to(playerData.roomId).emit('actionPerformed', {
          playerId: socket.id,
          action: data.action
        })
      }
    })

    // 发送消息
    socket.on('sendMessage', (data) => {
      const playerData = players.get(socket.id)
      if (!playerData) return

      io.to(playerData.roomId).emit('messageReceived', {
        playerId: socket.id,
        playerName: playerData.player.name,
        message: data.message,
        timestamp: Date.now()
      })
    })

    // 离开房间
    socket.on('leaveRoom', () => {
      handlePlayerLeave(socket, io)
    })

    // 断开连接
    socket.on('disconnect', () => {
      handlePlayerLeave(socket, io)
      console.log(`玩家断开: ${socket.id}`)
    })
  })
}

function handlePlayerLeave(socket, io) {
  const playerData = players.get(socket.id)
  if (!playerData) return

  const room = rooms.get(playerData.roomId)
  if (room) {
    room.removePlayer(socket.id)
    io.to(playerData.roomId).emit('playerLeft', { playerId: socket.id })

    // 如果房间为空，删除房间
    if (room.isEmpty()) {
      rooms.delete(playerData.roomId)
    }
  }

  players.delete(socket.id)
}

module.exports = { setupSocketHandlers }
```

### 4. 游戏房间 (server/src/socket/GameRoom.js)

```javascript
class GameRoom {
  constructor(id) {
    this.id = id
    this.players = new Map()
    this.state = {
      status: 'waiting', // waiting, playing, finished
      startedAt: null,
      scores: {}
    }
    this.maxPlayers = 4
    this.createdAt = Date.now()
  }

  addPlayer(socketId, player) {
    if (this.players.size >= this.maxPlayers) {
      throw new Error('房间已满')
    }
    this.players.set(socketId, {
      ...player,
      socketId,
      joinedAt: Date.now()
    })
    this.state.scores[socketId] = 0
  }

  removePlayer(socketId) {
    this.players.delete(socketId)
    delete this.state.scores[socketId]
  }

  updatePlayerPosition(socketId, position) {
    const player = this.players.get(socketId)
    if (player) {
      player.position = position
    }
  }

  handlePlayerAction(socketId, action) {
    // 处理游戏动作
    console.log(`玩家 ${socketId} 执行动作: ${action.type}`)
  }

  updateScore(socketId, score) {
    this.state.scores[socketId] = (this.state.scores[socketId] || 0) + score
  }

  getState() {
    return {
      id: this.id,
      status: this.state.status,
      players: Array.from(this.players.values()),
      scores: this.state.scores,
      maxPlayers: this.maxPlayers
    }
  }

  isEmpty() {
    return this.players.size === 0
  }

  isFull() {
    return this.players.size >= this.maxPlayers
  }
}

module.exports = GameRoom
```

### 5. 前端 WebSocket 管理 (client/src/network/SocketManager.js)

```javascript
import { io } from 'socket.io-client'

class SocketManager {
  constructor() {
    this.socket = null
    this.listeners = new Map()
  }

  connect(url) {
    this.socket = io(url, {
      autoConnect: true,
      reconnection: true,
      reconnectionAttempts: 5,
      reconnectionDelay: 1000
    })

    this.socket.on('connect', () => {
      console.log('WebSocket 连接成功')
    })

    this.socket.on('disconnect', () => {
      console.log('WebSocket 断开连接')
    })

    this.socket.on('connect_error', (error) => {
      console.error('WebSocket 连接错误:', error)
    })

    return this
  }

  joinRoom(roomId, player) {
    this.socket.emit('joinRoom', { roomId, player })
  }

  leaveRoom() {
    this.socket.emit('leaveRoom')
  }

  sendMove(position) {
    this.socket.emit('playerMove', { position })
  }

  sendAction(action) {
    this.socket.emit('playerAction', { action })
  }

  sendMessage(message) {
    this.socket.emit('sendMessage', { message })
  }

  onPlayerJoined(callback) {
    this.socket.on('playerJoined', callback)
  }

  onPlayerLeft(callback) {
    this.socket.on('playerLeft', callback)
  }

  onPlayerMoved(callback) {
    this.socket.on('playerMoved', callback)
  }

  onActionPerformed(callback) {
    this.socket.on('actionPerformed', callback)
  }

  onMessageReceived(callback) {
    this.socket.on('messageReceived', callback)
  }

  onRoomState(callback) {
    this.socket.on('roomState', callback)
  }

  disconnect() {
    if (this.socket) {
      this.socket.disconnect()
    }
  }
}

export default new SocketManager()
```

### 6. Docker Compose 配置

```yaml
version: '3.8'

services:
  client:
    build: ./client
    ports:
      - "5173:5173"
    depends_on:
      - server

  server:
    build: ./server
    ports:
      - "3000:3000"
    environment:
      - NODE_ENV=production
      - MONGODB_URI=mongodb://mongo:27017/game
      - JWT_SECRET=${JWT_SECRET}
    depends_on:
      - mongo

  mongo:
    image: mongo:6
    ports:
      - "27017:27017"
    volumes:
      - mongo-data:/data/db

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
    depends_on:
      - client
      - server

volumes:
  mongo-data:
```

## 使用方法

1. 复制模板到新目录
2. 配置 `.env` 文件
3. 运行 `docker-compose up`
4. 访问 `http://localhost`

## 扩展点

- 添加新游戏类型：在 `GameService.js` 中实现
- 添加新排行榜：在 `leaderboard.js` 路由中实现
- 添加好友系统：创建 `FriendService`
- 添加聊天系统：扩展 `SocketHandler`
- 添加存档系统：创建 `SaveService`
