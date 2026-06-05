---
name: 格斗游戏开发模板
description: 格斗游戏开发模板，适用于对战格斗、动作对战类游戏
trigger: 格斗游戏,对战游戏,fighting game,街霸,拳皇,动作对战
examples: 街头霸王|拳皇|铁拳|真人快打|龙珠斗士Z
---

# 格斗游戏开发模板

## 核心系统设计

### 1. 角色系统
```
Fighter {
  id: 角色ID
  name: 角色名
  health: 生命值(1000)
  moves: {
    punch: { damage: 10, startup: 3, recovery: 5 },
    kick: { damage: 12, startup: 4, recovery: 6 },
    special: { damage: 25, startup: 8, recovery: 12 }
  }
  hitbox: 碰撞箱
  hurtbox: 受击箱
}
```

### 2. 帧数据系统
| 阶段 | 说明 | 帧数 |
|------|------|------|
| 启动帧 | 出招前摇 | 3-10 |
| 活跃帧 | 判定存在 | 2-5 |
| 恢复帧 | 出招后摇 | 5-15 |

### 3. 连招系统
```javascript
const comboSystem = {
  chain: ['LP', 'MP', 'HP'],  // 轻中重拳链
  cancel: ['normal', 'special', 'super'],  // 取消层级
  juggle: { maxHits: 3, gravity: 0.5 }  // 浮空连招
}
```

## 关键技术实现

### 碰撞检测
```javascript
function checkHit(attacker, defender) {
  const attackBox = attacker.getHitbox();
  const defendBox = defender.getHurtbox();
  
  return attackBox.intersects(defendBox);
}
```

### 输入缓冲
```javascript
class InputBuffer {
  constructor(bufferSize = 10) {
    this.buffer = [];
    this.size = bufferSize;
  }
  
  addInput(input) {
    this.buffer.push({ ...input, time: Date.now() });
    if (this.buffer.length > this.size) {
      this.buffer.shift();
    }
  }
  
  checkMotion(motionSequence) {
    // 检查搓招指令
  }
}
```

## 网络对战

- 帧同步方案
- 输入延迟补偿
- 回滚网络代码
- 匹配系统(ELO)

## 游戏模式

| 模式 | 说明 |
|------|------|
| 街机模式 | 连续挑战AI |
| 对战模式 | 本地/在线PVP |
| 练习模式 | 连招练习 |
| 训练模式 | 帧数据查看 |
