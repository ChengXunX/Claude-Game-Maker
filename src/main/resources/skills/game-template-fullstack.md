---
name: 全栈游戏开发模板
description: 全栈游戏开发模板，适用于需要后端服务的多人在线游戏
trigger: fullstack, 全栈, multiplayer, 多人, online, 在线, server, 后端
examples: 多人在线游戏|MMO|实时对战|联机游戏
---

# 全栈游戏开发模板

## 游戏设计核心原则

### 核心循环（持续进行）
```
登录 → 匹配 → 游戏 → 结算 → 保存 → 继续
```
- **实时性**：多人实时交互
- **持久性**：数据保存到服务器
- **社交性**：和其他玩家互动

### 玩家心理学
- **"多人对战"的刺激感**：和真人对战
- **"数据持久"的安全感**：数据不会丢失
- **"社交互动"的乐趣**：和其他玩家交流
- -"排名竞争"的欲望**：提高排名

### 全栈设计要点
```
全栈核心：
1. 实时通信：WebSocket 低延迟
2. 数据持久：服务器保存数据
3. 状态同步：多人状态一致
4. 安全防护：防止作弊
```

## 核心系统设计

### 1. 服务器架构
```javascript
class GameServer {
  constructor() {
    this.io = require('socket.io')(server);
    this.rooms = new Map();
    this.players = new Map();
  }
  
  start() {
    this.io.on('connection', (socket) => {
      console.log('Player connected:', socket.id);
      
      // 玩家认证
      socket.on('authenticate', (data) => this.authenticate(socket, data));
      
      // 加入房间
      socket.on('join_room', (roomId) => this.joinRoom(socket, roomId));
      
      // 游戏操作
      socket.on('game_action', (action) => this.handleAction(socket, action));
      
      // 断开连接
      socket.on('disconnect', () => this.handleDisconnect(socket));
    });
  }
  
  authenticate(socket, data) {
    // 验证 token
    const player = this.verifyToken(data.token);
    
    if (player) {
      this.players.set(socket.id, player);
      socket.emit('authenticated', { success: true, player });
    } else {
      socket.emit('authenticated', { success: false, error: '认证失败' });
    }
  }
  
  joinRoom(socket, roomId) {
    const room = this.rooms.get(roomId);
    
    if (room && room.players.length < room.maxPlayers) {
      room.players.push(socket.id);
      socket.join(roomId);
      
      // 通知房间内其他玩家
      socket.to(roomId).emit('player_joined', {
        playerId: socket.id,
        player: this.players.get(socket.id)
      });
      
      // 发送房间状态
      socket.emit('room_state', room.getState());
    }
  }
  
  handleAction(socket, action) {
    const player = this.players.get(socket.id);
    const room = this.getPlayerRoom(socket.id);
    
    if (player && room) {
      // 验证操作
      if (this.validateAction(action, player)) {
        // 执行操作
        const result = room.executeAction(action, player);
        
        // 广播结果
        this.io.to(room.id).emit('action_result', result);
        
        // 检查游戏结束
        if (room.checkGameOver()) {
          this.endGame(room);
        }
      }
    }
  }
}
```

### 2. 房间系统
```javascript
class GameRoom {
  constructor(config) {
    this.id = config.id;
    this.name = config.name;
    this.maxPlayers = config.maxPlayers;
    this.players = [];
    this.state = 'waiting'; // waiting, playing, finished
    this.gameState = {};
  }
  
  addPlayer(playerId) {
    if (this.players.length < this.maxPlayers) {
      this.players.push(playerId);
      
      if (this.players.length >= this.minPlayers) {
        this.state = 'ready';
      }
      
      return true;
    }
    
    return false;
  }
  
  removePlayer(playerId) {
    this.players = this.players.filter(p => p !== playerId);
    
    if (this.players.length === 0) {
      this.state = 'empty';
    }
  }
  
  startGame() {
    if (this.state !== 'ready') return false;
    
    this.state = 'playing';
    this.gameState = this.initGameState();
    
    return true;
  }
  
  executeAction(action, player) {
    // 执行游戏操作
    switch (action.type) {
      case 'move':
        return this.handleMove(action, player);
      case 'attack':
        return this.handleAttack(action, player);
      case 'use_item':
        return this.handleUseItem(action, player);
      default:
        return { success: false, error: '未知操作' };
    }
  }
  
  checkGameOver() {
    // 检查游戏结束条件
    return this.gameState.gameOver;
  }
  
  getState() {
    return {
      id: this.id,
      name: this.name,
      players: this.players,
      state: this.state,
      gameState: this.gameState
    };
  }
}
```

### 3. 状态同步系统
```javascript
class StateSync {
  constructor() {
    this.tickRate = 20; // 每秒 20 次
    this.lastTick = Date.now();
  }
  
  update(delta) {
    const now = Date.now();
    
    if (now - this.lastTick >= 1000 / this.tickRate) {
      this.syncState();
      this.lastTick = now;
    }
  }
  
  syncState() {
    const state = this.game.getState();
    
    // 广播给所有玩家
    this.io.to(this.room.id).emit('state_update', {
      timestamp: Date.now(),
      state: state
    });
  }
  
  interpolateState(lastState, currentState, alpha) {
    // 状态插值，使动画平滑
    const interpolated = {};
    
    for (const key in currentState) {
      if (typeof currentState[key] === 'number') {
        interpolated[key] = lastState[key] + (currentState[key] - lastState[key]) * alpha;
      } else {
        interpolated[key] = currentState[key];
      }
    }
    
    return interpolated;
  }
}
```

### 4. 数据持久化系统
```javascript
class DataPersistence {
  constructor() {
    this.db = require('./database');
  }
  
  async savePlayerData(playerId, data) {
    await this.db.players.updateOne(
      { _id: playerId },
      { $set: data },
      { upsert: true }
    );
  }
  
  async loadPlayerData(playerId) {
    return await this.db.players.findOne({ _id: playerId });
  }
  
  async saveGameResult(gameId, result) {
    await this.db.games.updateOne(
      { _id: gameId },
      { $set: result },
      { upsert: true }
    );
  }
  
  async getLeaderboard(limit = 10) {
    return await this.db.players
      .find({})
      .sort({ score: -1 })
      .limit(limit)
      .toArray();
  }
  
  async getPlayerStats(playerId) {
    const stats = await this.db.games.aggregate([
      { $match: { playerId } },
      { $group: {
        _id: null,
        totalGames: { $sum: 1 },
        wins: { $sum: { $cond: ['$won', 1, 0] } },
        losses: { $sum: { $cond: ['$won', 0, 1] } },
        totalScore: { $sum: '$score' }
      }}
    ]).toArray();
    
    return stats[0] || { totalGames: 0, wins: 0, losses: 0, totalScore: 0 };
  }
}
```

### 5. 匹配系统
```javascript
class MatchmakingSystem {
  constructor() {
    this.queue = [];
    this.matchSize = 2;
  }
  
  addToQueue(player) {
    this.queue.push({
      playerId: player.id,
      rating: player.rating,
      joinedAt: Date.now()
    });
    
    this.tryMatch();
  }
  
  removeFromQueue(playerId) {
    this.queue = this.queue.filter(p => p.playerId !== playerId);
  }
  
  tryMatch() {
    if (this.queue.length >= this.matchSize) {
      // 按评分排序
      this.queue.sort((a, b) => a.rating - b.rating);
      
      // 找到评分相近的玩家
      const match = this.findMatch();
      
      if (match) {
        this.createMatch(match);
      }
    }
  }
  
  findMatch() {
    // 找到评分最接近的玩家
    for (let i = 0; i < this.queue.length - 1; i++) {
      const diff = Math.abs(this.queue[i].rating - this.queue[i + 1].rating);
      
      if (diff <= 200) { // 评分差不超过 200
        return [this.queue[i], this.queue[i + 1]];
      }
    }
    
    return null;
  }
  
  createMatch(players) {
    // 创建房间
    const room = this.createRoom();
    
    // 移出队列
    for (const player of players) {
      this.removeFromQueue(player.playerId);
      room.addPlayer(player.playerId);
    }
    
    // 开始游戏
    room.startGame();
    
    return room;
  }
}
```

## 迭代策略

### 第一版：基础联机
- 简单服务器
- 基础联机
- 状态同步
- 简单 UI

### 第二版：房间系统
- 房间创建
- 房间加入
- 房间列表
- 房间聊天

### 第三版：匹配系统
- 自动匹配
- 评分系统
- 匹配队列
- 匹配历史

### 第四版：数据持久化
- 玩家数据保存
- 游戏记录
- 排行榜
- 成就系统

### 第五版：安全防护
- 反作弊系统
- 服务器验证
- 数据加密
- DDoS 防护

## 常见错误

1. **延迟太高**：要优化网络延迟
2. **状态不同步**：要保证状态一致
3. **没有持久化**：数据要保存到服务器
4. **没有反作弊**：要有反作弊系统
5. **没有匹配**：要有匹配系统
