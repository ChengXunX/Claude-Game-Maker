---
name: AI对弈棋局游戏开发模板
description: AI对弈棋局游戏开发模板，适用于AI对手的棋类游戏
trigger: AI对弈, 棋类, chess, 围棋, 象棋, 五子棋, AI对手, 对战
examples: 象棋|围棋|五子棋|国际象棋|Chess.com|Lichess
---

# AI 对弈棋局游戏开发模板

## 游戏设计核心原则

### 核心循环（每局 5-30 分钟）
```
选择难度 → 开始对弈 → 思考落子 → AI 回应 → 判定胜负
```
- **策略深度**：每一步都有意义
- **AI 挑战**：不同难度的 AI 对手
- **学习成长**：从新手到高手

### 玩家心理学
- **"以智取胜"的成就感**：用策略战胜 AI
- **"学习进步"的成长感**：从输到赢的过程
- **"复盘分析"的乐趣**：分析对局找出失误
- **"挑战极限"的欲望**：挑战更高难度 AI

### AI 设计要点
```
AI 核心：
1. 多难度级别：新手到大师
2. 自适应难度：根据玩家水平调整
3. 思考时间：模拟人类思考
4. 开局库：常见开局模式
```

## 核心系统设计

### 1. AI 系统
```javascript
class ChessAI {
  constructor(difficulty) {
    this.difficulty = difficulty; // 1-10
    this.maxDepth = difficulty;
    this.openingBook = {};
    this.evaluationCache = {};
  }
  
  getBestMove(game) {
    // 检查开局库
    const openingMove = this.checkOpeningBook(game);
    if (openingMove) return openingMove;
    
    // 搜索最佳走法
    const moves = game.getValidMoves();
    let bestMove = null;
    let bestScore = -Infinity;
    
    for (const move of moves) {
      game.makeMove(move);
      const score = this.minimax(game, this.maxDepth, -Infinity, Infinity, false);
      game.undoMove(move);
      
      if (score > bestScore) {
        bestScore = score;
        bestMove = move;
      }
    }
    
    return bestMove;
  }
  
  minimax(game, depth, alpha, beta, isMaximizing) {
    // 检查缓存
    const cacheKey = game.getBoardHash();
    if (this.evaluationCache[cacheKey]) {
      return this.evaluationCache[cacheKey];
    }
    
    if (depth === 0 || game.isGameOver()) {
      const score = this.evaluate(game);
      this.evaluationCache[cacheKey] = score;
      return score;
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
    
    // 子力价值
    score += this.evaluateMaterial(game);
    
    // 位置价值
    score += this.evaluatePosition(game);
    
    // 机动性
    score += this.evaluateMobility(game);
    
    return score;
  }
}
```

### 2. 难度自适应系统
```javascript
class AdaptiveDifficulty {
  constructor() {
    this.playerRating = 1000;
    this.gameHistory = [];
  }
  
  adjustDifficulty(result) {
    this.gameHistory.push(result);
    
    // 根据最近 10 局调整难度
    const recentGames = this.gameHistory.slice(-10);
    const winRate = recentGames.filter(r => r === 'win').length / recentGames.length;
    
    if (winRate > 0.7) {
      // 玩家太强，增加难度
      this.playerRating += 50;
    } else if (winRate < 0.3) {
      // 玩家太弱，降低难度
      this.playerRating -= 50;
    }
    
    return this.getDifficultyForRating();
  }
  
  getDifficultyForRating() {
    // 根据评分返回难度
    if (this.playerRating < 800) return 1;
    if (this.playerRating < 1000) return 2;
    if (this.playerRating < 1200) return 3;
    if (this.playerRating < 1400) return 4;
    if (this.playerRating < 1600) return 5;
    if (this.playerRating < 1800) return 6;
    if (this.playerRating < 2000) return 7;
    if (this.playerRating < 2200) return 8;
    if (this.playerRating < 2400) return 9;
    return 10;
  }
}
```

### 3. 开局库系统
```javascript
class OpeningBook {
  constructor() {
    this.book = {};
    this.loadBook();
  }
  
  loadBook() {
    // 五子棋开局
    this.book['gomoku'] = {
      'center': { move: { x: 7, y: 7 }, weight: 10 },
      'diagonal': { move: { x: 8, y: 8 }, weight: 8 },
      'adjacent': { move: { x: 7, y: 8 }, weight: 6 }
    };
    
    // 象棋开局
    this.book['chess'] = {
      '中炮': { moves: ['炮二平五'], weight: 10 },
      '飞相': { moves: ['相三进五'], weight: 8 },
      '起马': { moves: ['马二进三'], weight: 9 }
    };
  }
  
  getMove(gameType, position) {
    const book = this.book[gameType];
    if (!book) return null;
    
    // 根据权重随机选择
    const entries = Object.values(book);
    const totalWeight = entries.reduce((sum, e) => sum + e.weight, 0);
    let random = Math.random() * totalWeight;
    
    for (const entry of entries) {
      random -= entry.weight;
      if (random <= 0) {
        return entry.move;
      }
    }
    
    return entries[0].move;
  }
}
```

### 4. 复盘系统
```javascript
class ReplaySystem {
  constructor() {
    this.moves = [];
    this.currentMove = 0;
  }
  
  recordMove(move) {
    this.moves.push(move);
  }
  
  goBack() {
    if (this.currentMove > 0) {
      this.currentMove--;
      return this.moves[this.currentMove];
    }
    return null;
  }
  
  goForward() {
    if (this.currentMove < this.moves.length - 1) {
      this.currentMove++;
      return this.moves[this.currentMove];
    }
    return null;
  }
  
  goToStart() {
    this.currentMove = 0;
    return this.moves[0];
  }
  
  goToEnd() {
    this.currentMove = this.moves.length - 1;
    return this.moves[this.currentMove];
  }
  
  analyze() {
    // 分析对局
    const analysis = {
      totalMoves: this.moves.length,
      averageThinkTime: this.calculateAverageThinkTime(),
      mistakes: this.findMistakes(),
      bestMoves: this.findBestMoves()
    };
    
    return analysis;
  }
}
```

### 5. 教学系统
```javascript
class TutorialSystem {
  constructor() {
    this.lessons = [];
    this.currentLesson = 0;
  }
  
  addLesson(config) {
    this.lessons.push({
      id: config.id,
      title: config.title,
      description: config.description,
      steps: config.steps,
      completed: false
    });
  }
  
  startLesson(lessonId) {
    const lesson = this.lessons.find(l => l.id === lessonId);
    if (!lesson) return false;
    
    this.currentLesson = lesson;
    this.currentStep = 0;
    
    this.showStep(lesson.steps[0]);
    
    return true;
  }
  
  nextStep() {
    this.currentStep++;
    
    if (this.currentStep >= this.currentLesson.steps.length) {
      this.completeLesson();
      return;
    }
    
    this.showStep(this.currentLesson.steps[this.currentStep]);
  }
  
  completeLesson() {
    this.currentLesson.completed = true;
    this.showCompletionMessage();
  }
}

const LESSONS = [
  {
    id: 'basic_moves',
    title: '基本走法',
    description: '学习棋子的基本走法',
    steps: [
      { text: '点击棋子选中', highlight: 'piece' },
      { text: '点击目标位置落子', highlight: 'target' },
      { text: '很好！你学会了基本走法', type: 'complete' }
    ]
  }
];
```

## 迭代策略

### 第一版：基础对弈
- 1 种棋类
- 基础 AI
- 简单 UI
- 基础规则

### 第二版：AI 系统
- 多难度 AI
- 开局库
- 复盘系统
- 计时系统

### 第三版：多种棋类
- 3 种棋类
- 规则说明
- 教学系统
- 成就系统

### 第四版：深度功能
- 自适应难度
- 棋谱分享
- 在线对弈
- 排位系统

### 第五版：社交功能
- 好友系统
- 观战系统
- 直播功能
- 赛事系统

## 常见错误

1. **AI 太弱**：AI 要有挑战性
2. **AI 太强**：新手要有获胜机会
3. **没有教学**：新手要能学会规则
4. **没有复盘**：要能回顾对局
5. **没有难度选择**：要有多种难度
