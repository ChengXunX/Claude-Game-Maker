---
name: 格斗游戏开发模板
description: 格斗游戏开发模板，适用于对战格斗、动作对战类游戏
trigger: 格斗游戏,对战游戏,fighting game,街霸,拳皇,动作对战
examples: 街头霸王|拳皇|铁拳|真人快打|龙珠斗士Z|Street Fighter
---

# 格斗游戏开发模板

## 游戏设计核心原则

### 核心循环（每局 1-3 分钟）
```
选择角色 → 对战 → 使用招式 → 防御/闪避 → 击败对手
```
- **操作感**：精确控制角色
- **连招感**：连续攻击的爽感
- **策略性**：预判对手行动

### 玩家心理学
- **"连招爽感"**：打出华丽连招的满足感
- **"以弱胜强"**：用技巧战胜强敌
- **"角色精通"**：精通一个角色的成长感
- **"对战刺激"**：和真人对战的紧张感

### 格斗设计要点
```
格斗核心：
1. 操作灵敏：按键立刻响应
2. 判定准确：攻击判定要准确
3. 平衡性：每个角色都要有优缺点
4. 连招系统：普通攻击可以连招
```

## 核心系统设计

### 1. 角色系统
```javascript
class Fighter {
  constructor(config) {
    this.name = config.name;
    this.hp = config.hp || 1000;
    this.maxHp = this.hp;
    this.position = { x: 0, y: 0 };
    this.velocity = { x: 0, y: 0 };
    this.state = 'idle'; // idle, attacking, blocking, hit, knocked
    this.facing = 1; // 1 = right, -1 = left
    
    this.moves = config.moves;
    this.currentMove = null;
    this.hitboxes = [];
  }
  
  update(delta) {
    // 更新位置
    this.position.x += this.velocity.x * delta;
    this.position.y += this.velocity.y * delta;
    
    // 重力
    if (this.position.y > 0) {
      this.velocity.y -= 980 * delta;
    } else {
      this.position.y = 0;
      this.velocity.y = 0;
    }
    
    // 更新招式
    if (this.currentMove) {
      this.currentMove.update(delta);
      if (this.currentMove.isFinished()) {
        this.currentMove = null;
        this.state = 'idle';
      }
    }
  }
  
  attack(moveName) {
    if (this.state !== 'idle') return false;
    
    const move = this.moves[moveName];
    if (!move) return false;
    
    this.currentMove = move;
    this.state = 'attacking';
    this.hitboxes = move.getHitboxes();
    
    return true;
  }
  
  block() {
    if (this.state !== 'idle') return false;
    this.state = 'blocking';
    return true;
  }
  
  takeDamage(damage) {
    if (this.state === 'blocking') {
      damage *= 0.1; // 防御时只受 10% 伤害
    }
    
    this.hp -= damage;
    this.state = 'hit';
    
    if (this.hp <= 0) {
      this.ko();
    }
  }
}
```

### 2. 招式系统
```javascript
class Move {
  constructor(config) {
    this.name = config.name;
    this.startup = config.startup; // 启动帧数
    this.active = config.active; // 活跃帧数
    this.recovery = config.recovery; // 恢复帧数
    this.damage = config.damage;
    this.hitboxes = config.hitboxes;
    this.frame = 0;
  }
  
  update(delta) {
    this.frame++;
  }
  
  getHitboxes() {
    if (this.frame >= this.startup && this.frame < this.startup + this.active) {
      return this.hitboxes;
    }
    return [];
  }
  
  isFinished() {
    return this.frame >= this.startup + this.active + this.recovery;
  }
  
  canCancel() {
    return this.frame >= this.startup + this.active;
  }
}

const MOVES = {
  lightPunch: { name: '轻拳', startup: 3, active: 3, recovery: 5, damage: 30 },
  heavyPunch: { name: '重拳', startup: 5, active: 4, recovery: 10, damage: 70 },
  lightKick: { name: '轻脚', startup: 4, active: 3, recovery: 6, damage: 35 },
  heavyKick: { name: '重脚', startup: 6, active: 5, recovery: 12, damage: 80 },
  special: { name: '必杀技', startup: 8, active: 6, recovery: 15, damage: 150 }
};
```

### 3. 连招系统
```javascript
class ComboSystem {
  constructor() {
    this.hits = 0;
    this.damage = 0;
    this.window = 30; // 连招窗口帧数
    this.lastHitFrame = 0;
  }
  
  registerHit(damage) {
    this.hits++;
    this.damage += damage;
    this.lastHitFrame = 0;
    
    // 连招伤害递减
    const scaling = Math.max(0.1, 1 - (this.hits - 1) * 0.1);
    return Math.floor(damage * scaling);
  }
  
  update(delta) {
    this.lastHitFrame++;
    
    if (this.lastHitFrame > this.window) {
      this.reset();
    }
  }
  
  reset() {
    this.hits = 0;
    this.damage = 0;
  }
  
  isCombo() {
    return this.hits > 1;
  }
}
```

### 4. 判定系统
```javascript
class HitDetection {
  checkHit(attacker, defender) {
    const attackerHitboxes = attacker.getHitboxes();
    const defenderHurtboxes = defender.getHurtboxes();
    
    for (const hitbox of attackerHitboxes) {
      for (const hurtbox of defenderHurtboxes) {
        if (this.intersects(hitbox, hurtbox)) {
          return {
            hit: true,
            damage: hitbox.damage,
            position: this.getHitPosition(hitbox, hurtbox)
          };
        }
      }
    }
    
    return { hit: false };
  }
  
  intersects(box1, box2) {
    return box1.x < box2.x + box2.width &&
           box1.x + box1.width > box2.x &&
           box1.y < box2.y + box2.height &&
           box1.y + box1.height > box2.y;
  }
}
```

### 5. 输入系统
```javascript
class InputSystem {
  constructor() {
    this.buffer = [];
    this.bufferSize = 10;
  }
  
  addInput(input) {
    this.buffer.push({ ...input, frame: this.currentFrame });
    
    if (this.buffer.length > this.bufferSize) {
      this.buffer.shift();
    }
    
    this.checkSpecialMoves();
  }
  
  checkSpecialMoves() {
    // 检查必杀技指令
    const commands = [
      { name: 'fireball', sequence: ['down', 'down-forward', 'forward', 'punch'] },
      { name: 'shoryuken', sequence: ['forward', 'down', 'down-forward', 'punch'] }
    ];
    
    for (const command of commands) {
      if (this.matchCommand(command.sequence)) {
        this.executeSpecialMove(command.name);
      }
    }
  }
  
  matchCommand(sequence) {
    const recentInputs = this.buffer.slice(-sequence.length);
    return recentInputs.every((input, i) => input.direction === sequence[i]);
  }
}
```

## 迭代策略

### 第一版：基础对战
- 1 个角色
- 基础攻击
- 简单 AI
- 基础 UI

### 第二版：招式系统
- 多种招式
- 必杀技
- 连招系统
- 防御系统

### 第三版：多角色
- 3 个角色
- 角色选择
- 角色平衡
- 特殊效果

### 第四版：深度玩法
- 5 个角色
- 故事模式
- 挑战模式
- 成就系统

### 第五版：多人对战
- 在线对战
- 排位系统
- 回放系统
- 排行榜

## 常见错误

1. **操作不灵敏**：按键必须立刻响应
2. **判定不准确**：攻击判定要准确
3. **不平衡**：某些角色太强
4. **没有连招**：连招是格斗游戏的核心
5. **没有反馈**：击中要有视觉和音效反馈
