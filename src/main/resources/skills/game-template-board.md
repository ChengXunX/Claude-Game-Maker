---
name: 棋牌游戏开发模板
description: 棋牌游戏开发模板，适用于象棋、围棋、扑克等棋牌类游戏
trigger: 棋牌游戏,board game,象棋,围棋,扑克,麻将,五子棋,斗地主
examples: 象棋|围棋|五子棋|斗地主|德州扑克|麻将|国际象棋
---

# 棋牌游戏开发模板

## 游戏设计核心原则

### 核心循环（每局 5-30 分钟）
```
了解规则 → 思考策略 → 落子/出牌 → 对手回合 → 判定胜负
```
- **策略深度**：每一步都有意义
- **公平性**：双方机会均等
- **社交性**：和真人对弈的乐趣

### 玩家心理学
- **"以智取胜"的成就感**：用策略战胜对手
- **"学习进步"的成长感**：从新手到高手
- **"复盘分析"的乐趣**：分析对局找出失误
- **"排名上升"的欲望**：提高段位的追求

### 棋牌设计要点
```
棋牌核心：
1. 规则简单：5 分钟学会规则
2. 策略深邃：一辈子学不完
3. 公平对弈：双方机会均等
4. 社交对弈：和真人玩更有趣
```

## 核心系统设计

### 1. 棋盘系统
```javascript
class Board {
  constructor(size) {
    this.size = size;
    this.grid = Array(size).fill(null).map(() => Array(size).fill(null));
  }
  
  placePiece(x, y, piece) {
    if (!this.isValidPosition(x, y)) return false;
    if (this.grid[y][x] !== null) return false;
    
    this.grid[y][x] = piece;
    return true;
  }
  
  removePiece(x, y) {
    if (!this.isValidPosition(x, y)) return null;
    
    const piece = this.grid[y][x];
    this.grid[y][x] = null;
    return piece;
  }
  
  getPiece(x, y) {
    if (!this.isValidPosition(x, y)) return null;
    return this.grid[y][x];
  }
  
  isValidPosition(x, y) {
    return x >= 0 && x < this.size && y >= 0 && y < this.size;
  }
  
  isEmpty(x, y) {
    return this.getPiece(x, y) === null;
  }
}
```

### 2. 五子棋系统
```javascript
class Gomoku {
  constructor() {
    this.board = new Board(15);
    this.currentPlayer = 'black';
    this.moves = [];
  }
  
  placePiece(x, y) {
    if (!this.board.isEmpty(x, y)) return false;
    
    this.board.placePiece(x, y, this.currentPlayer);
    this.moves.push({ x, y, player: this.currentPlayer });
    
    // 检查胜负
    if (this.checkWin(x, y)) {
      this.winner = this.currentPlayer;
      return true;
    }
    
    // 切换玩家
    this.currentPlayer = this.currentPlayer === 'black' ? 'white' : 'black';
    
    return true;
  }
  
  checkWin(x, y) {
    const piece = this.board.getPiece(x, y);
    const directions = [
      [1, 0], [0, 1], [1, 1], [1, -1]
    ];
    
    for (const [dx, dy] of directions) {
      let count = 1;
      
      // 正方向
      for (let i = 1; i < 5; i++) {
        const nx = x + dx * i;
        const ny = y + dy * i;
        if (this.board.getPiece(nx, ny) === piece) {
          count++;
        } else {
          break;
        }
      }
      
      // 反方向
      for (let i = 1; i < 5; i++) {
        const nx = x - dx * i;
        const ny = y - dy * i;
        if (this.board.getPiece(nx, ny) === piece) {
          count++;
        } else {
          break;
        }
      }
      
      if (count >= 5) return true;
    }
    
    return false;
  }
}
```

### 3. 扑克牌系统
```javascript
class Poker {
  constructor() {
    this.deck = this.createDeck();
    this.players = [];
    this.pot = 0;
    this.communityCards = [];
  }
  
  createDeck() {
    const suits = ['hearts', 'diamonds', 'clubs', 'spades'];
    const ranks = ['2', '3', '4', '5', '6', '7', '8', '9', '10', 'J', 'Q', 'K', 'A'];
    const deck = [];
    
    for (const suit of suits) {
      for (const rank of ranks) {
        deck.push({ suit, rank });
      }
    }
    
    return this.shuffle(deck);
  }
  
  shuffle(array) {
    for (let i = array.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [array[i], array[j]] = [array[j], array[i]];
    }
    return array;
  }
  
  deal(numCards) {
    return this.deck.splice(0, numCards);
  }
  
  evaluateHand(cards) {
    // 评估牌型
    const sorted = this.sortCards(cards);
    
    if (this.isRoyalFlush(sorted)) return { rank: 10, name: '皇家同花顺' };
    if (this.isStraightFlush(sorted)) return { rank: 9, name: '同花顺' };
    if (this.isFourOfAKind(sorted)) return { rank: 8, name: '四条' };
    if (this.isFullHouse(sorted)) return { rank: 7, name: '葫芦' };
    if (this.isFlush(sorted)) return { rank: 6, name: '同花' };
    if (this.isStraight(sorted)) return { rank: 5, name: '顺子' };
    if (this.isThreeOfAKind(sorted)) return { rank: 4, name: '三条' };
    if (this.isTwoPair(sorted)) return { rank: 3, name: '两对' };
    if (this.isOnePair(sorted)) return { rank: 2, name: '一对' };
    return { rank: 1, name: '高牌' };
  }
}
```

### 4. AI 对手
```javascript
class ChessAI {
  constructor(difficulty) {
    this.difficulty = difficulty; // 1-10
    this.maxDepth = difficulty;
  }
  
  getBestMove(game) {
    const moves = game.getValidMoves();
    let bestMove = null;
    let bestScore = -Infinity;
    
    for (const move of moves) {
      game.makeMove(move);
      const score = this.minimax(game, this.maxDepth - 1, -Infinity, Infinity, false);
      game.undoMove(move);
      
      if (score > bestScore) {
        bestScore = score;
        bestMove = move;
      }
    }
    
    return bestMove;
  }
  
  minimax(game, depth, alpha, beta, isMaximizing) {
    if (depth === 0 || game.isGameOver()) {
      return this.evaluate(game);
    }
    
    if (isMaximizing) {
      let maxScore = -Infinity;
      for (const move of game.getValidMoves()) {
        game.makeMove(move);
        const score = this.minimax(game, depth - 1, alpha, beta, false);
        game.undoMove(move);
        maxScore = Math.max(maxScore, score);
        alpha = Math.max(alpha, score);
        if (beta <= alpha) break;
      }
      return maxScore;
    } else {
      let minScore = Infinity;
      for (const move of game.getValidMoves()) {
        game.makeMove(move);
        const score = this.minimax(game, depth - 1, alpha, beta, true);
        game.undoMove(move);
        minScore = Math.min(minScore, score);
        beta = Math.min(beta, score);
        if (beta <= alpha) break;
      }
      return minScore;
    }
  }
  
  evaluate(game) {
    // 评估棋盘状态
    let score = 0;
    // ... 评估逻辑
    return score;
  }
}
```

## 迭代策略

### 第一版：基础对弈
- 1 种棋类
- 基础规则
- 双人对弈
- 简单 UI

### 第二版：AI 对手
- AI 对弈
- 多难度级别
- 悔棋功能
- 历史记录

### 第三版：多种棋类
- 3 种棋类
- 规则说明
- 教学模式
- 成就系统

### 第四版：在线对弈
- 在线匹配
- 好友对弈
- 聊天系统
- 排位系统

### 第五版：深度功能
- 复盘分析
- 棋谱分享
- 直播功能
- 赛事系统

## 常见错误

1. **AI 太弱**：AI 要有挑战性
2. **规则不完整**：要处理所有特殊规则
3. **没有悔棋**：误操作要能撤销
4. **没有历史记录**：要能查看对局历史
5. **没有教学**：新手要能学会规则
