---
name: 体育游戏开发模板
description: 体育游戏开发模板，适用于足球、篮球、网球等体育类游戏
trigger: 体育游戏,sports game,足球,篮球,网球,棒球,排球
examples: FIFA|NBA2K|实况足球|马里奥赛车|马里奥网球
---

# 体育游戏开发模板

## 游戏设计核心原则

### 核心循环（每局 5-15 分钟）
```
开球 → 进攻/防守 → 得分 → 换边 → 继续 → 结束
```
- **真实感**：模拟真实运动
- **操作感**：精确控制球员
- **竞技性**：和对手竞争

### 玩家心理学
- **"进球快感"**：得分的满足感
- **"团队配合"**：和队友配合的乐趣
- **"技巧展示"**：炫技的成就感
- **"胜负欲"**：想赢的欲望

### 体育游戏设计要点
```
体育核心：
1. 物理真实：球的物理要真实
2. 操作简单：基本操作要简单
3. 技巧深度：高级技巧要难掌握
4. 节奏明快：不能太拖沓
```

## 核心系统设计

### 1. 球员系统
```javascript
class Player {
  constructor(config) {
    this.name = config.name;
    this.position = config.position;
    this.stats = {
      speed: config.speed,
      shooting: config.shooting,
      passing: config.passing,
      defense: config.defense,
      dribbling: config.dribbling,
      stamina: config.stamina
    };
    this.stamina = 100;
    this.hasBall = false;
  }
  
  update(delta) {
    // 体力消耗
    if (this.isRunning) {
      this.stamina -= 0.1 * delta;
    } else {
      this.stamina = Math.min(100, this.stamina + 0.05 * delta);
    }
    
    // 移动
    this.position.x += this.velocity.x * delta;
    this.position.y += this.velocity.y * delta;
  }
  
  shoot(target) {
    if (!this.hasBall) return false;
    
    const accuracy = this.stats.shooting / 100;
    const power = this.stats.shooting * 0.8;
    
    return {
      power,
      accuracy,
      direction: this.getDirection(target)
    };
  }
  
  pass(target) {
    if (!this.hasBall) return false;
    
    const accuracy = this.stats.passing / 100;
    const power = this.getDistance(this.position, target.position) * 0.5;
    
    return {
      power,
      accuracy,
      target
    };
  }
}
```

### 2. 球物理系统
```javascript
class BallPhysics {
  constructor() {
    this.position = { x: 0, y: 0, z: 0 };
    this.velocity = { x: 0, y: 0, z: 0 };
    this.gravity = -9.8;
    this.friction = 0.98;
    this.bounciness = 0.7;
  }
  
  update(delta) {
    // 重力
    this.velocity.z += this.gravity * delta;
    
    // 更新位置
    this.position.x += this.velocity.x * delta;
    this.position.y += this.velocity.y * delta;
    this.position.z += this.velocity.z * delta;
    
    // 地面碰撞
    if (this.position.z <= 0) {
      this.position.z = 0;
      this.velocity.z *= -this.bounciness;
      this.velocity.x *= this.friction;
      this.velocity.y *= this.friction;
    }
  }
  
  kick(power, direction) {
    this.velocity.x = Math.cos(direction) * power;
    this.velocity.y = Math.sin(direction) * power;
    this.velocity.z = power * 0.3; // 略微向上
  }
}
```

### 3. AI 系统
```javascript
class SportsAI {
  constructor(team) {
    this.team = team;
    this.state = 'idle';
  }
  
  update(delta) {
    const ball = this.game.ball;
    const hasBall = this.team.hasBall();
    
    if (hasBall) {
      this.attackBehavior(delta);
    } else {
      this.defenseBehavior(delta);
    }
  }
  
  attackBehavior(delta) {
    // 进攻行为
    const ballCarrier = this.team.getBallCarrier();
    
    if (this.canShoot(ballCarrier)) {
      // 射门
      ballCarrier.shoot(this.game.goal);
    } else if (this.canPass(ballCarrier)) {
      // 传球
      const target = this.getBestPassTarget(ballCarrier);
      ballCarrier.pass(target);
    } else {
      // 带球前进
      this.dribbleForward(ballCarrier, delta);
    }
  }
  
  defenseBehavior(delta) {
    // 防守行为
    const ball = this.game.ball;
    
    // 追球
    for (const player of this.team.players) {
      const dist = this.getDistance(player.position, ball.position);
      if (dist < 100) {
        player.moveToward(ball.position, delta);
      }
    }
  }
}
```

### 4. 比赛系统
```javascript
class Match {
  constructor(team1, team2) {
    this.team1 = team1;
    this.team2 = team2;
    this.score = { team1: 0, team2: 0 };
    this.time = 0;
    this.maxTime = 90 * 60; // 90 分钟
    this.state = 'playing';
  }
  
  update(delta) {
    if (this.state !== 'playing') return;
    
    this.time += delta;
    
    // 更新球队
    this.team1.update(delta);
    this.team2.update(delta);
    
    // 更新球
    this.ball.update(delta);
    
    // 检查进球
    this.checkGoal();
    
    // 检查比赛结束
    if (this.time >= this.maxTime) {
      this.endMatch();
    }
  }
  
  checkGoal() {
    const ball = this.ball;
    
    // 检查是否进球
    if (this.isGoal(ball.position, this.team1.goal)) {
      this.score.team2++;
      this.resetPositions();
    } else if (this.isGoal(ball.position, this.team2.goal)) {
      this.score.team1++;
      this.resetPositions();
    }
  }
  
  endMatch() {
    this.state = 'ended';
    this.winner = this.score.team1 > this.score.team2 ? this.team1 : this.team2;
  }
}
```

### 5. 控制系统
```javascript
class ControlSystem {
  constructor() {
    this.selectedPlayer = null;
    this.controls = {
      move: { up: 'W', down: 'S', left: 'A', right: 'D' },
      pass: 'SPACE',
      shoot: 'SHIFT',
      tackle: 'E',
      sprint: 'CTRL'
    };
  }
  
  update(delta) {
    // 移动
    const moveDir = this.getMoveDirection();
    if (moveDir) {
      this.selectedPlayer.move(moveDir, delta);
    }
    
    // 传球
    if (this.isKeyPressed(this.controls.pass)) {
      this.pass();
    }
    
    // 射门
    if (this.isKeyPressed(this.controls.shoot)) {
      this.shoot();
    }
    
    // 切换球员
    if (this.isKeyPressed('TAB')) {
      this.switchPlayer();
    }
  }
  
  pass() {
    const target = this.findNearestTeammate();
    if (target) {
      this.selectedPlayer.pass(target);
    }
  }
  
  shoot() {
    const goal = this.getOpponentGoal();
    this.selectedPlayer.shoot(goal);
  }
}
```

## 迭代策略

### 第一版：基础运动
- 球员移动
- 球物理
- 简单 AI
- 基础 UI

### 第二版：比赛系统
- 计分系统
- 时间系统
- 犯规系统
- 换人系统

### 第三版：深度玩法
- 多种战术
- 球员技能
- 天气系统
- 观众系统

### 第四版：职业模式
- 球队管理
- 转会系统
- 训练系统
- 赛季系统

### 第五版：多人对战
- 在线对战
- 排位系统
- 回放系统
- 排行榜

## 常见错误

1. **物理太假**：球的物理要真实
2. **操作太难**：基本操作要简单
3. **AI 太蠢**：AI 要有基本的战术
4. **节奏太慢**：比赛节奏要快
5. **没有反馈**：进球要有庆祝效果
