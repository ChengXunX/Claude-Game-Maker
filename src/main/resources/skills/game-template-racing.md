---
name: 赛车游戏开发模板
description: 赛车游戏开发模板，适用于俯视角赛车、竞速类游戏
trigger: racing, race, 赛车, car, 汽车, drift, 漂移, 竞速
examples: Mario Kart|Micro Machines|Racing Rivals|CSR Racing|Need for Speed
---

# 赛车游戏开发模板

## 游戏设计核心原则

### 核心循环（每局 1-3 分钟）
```
起步 → 加速 → 过弯 → 漂移 → 道具 → 冲刺 → 终点
```
- **速度感**：让玩家感觉"快"
- **操控感**：精确控制车辆
- **竞争感**：和对手争夺第一

### 玩家心理学
- **"速度与激情"**：高速行驶的刺激感
- **"完美过弯"的成就感**：漂移过弯的爽感
- **"反超对手"的快感**：从最后一名追到第一名
- **"道具战"的随机乐趣**：被道具击中的意外感

### 操控设计要点
```
赛车操控核心：
1. 加速要线性，不能突然快
2. 刹车要有效，不能刹不住
3. 转向要灵敏，但不能过度
4. 漂移要有奖励（加速/积分）
5. 碰撞要有惩罚（减速）
```

## 核心系统设计

### 1. 车辆物理
```javascript
class Car {
  constructor(config) {
    this.x = config.x;
    this.y = config.y;
    this.angle = config.angle || 0;
    this.speed = 0;
    this.maxSpeed = config.maxSpeed || 300;
    this.acceleration = config.acceleration || 200;
    this.brakeForce = config.brakeForce || 300;
    this.turnSpeed = config.turnSpeed || 3;
    this.friction = 0.98;
    this.drifting = false;
    this.driftAngle = 0;
  }
  
  update(cursors, delta) {
    // 加速
    if (cursors.up.isDown) {
      this.speed = Math.min(this.speed + this.acceleration * delta, this.maxSpeed);
    }
    
    // 刹车
    if (cursors.down.isDown) {
      this.speed = Math.max(this.speed - this.brakeForce * delta, -this.maxSpeed * 0.3);
    }
    
    // 转向
    if (cursors.left.isDown) {
      this.angle -= this.turnSpeed * delta * (this.speed / this.maxSpeed);
    }
    if (cursors.right.isDown) {
      this.angle += this.turnSpeed * delta * (this.speed / this.maxSpeed);
    }
    
    // 漂移
    if (this.drifting) {
      this.driftAngle *= 0.95;
      this.angle += this.driftAngle * delta;
    }
    
    // 摩擦力
    this.speed *= this.friction;
    
    // 更新位置
    this.x += Math.cos(this.angle) * this.speed * delta;
    this.y += Math.sin(this.angle) * this.speed * delta;
  }
  
  startDrift() {
    this.drifting = true;
    this.driftAngle = (this.speed > 0 ? 1 : -1) * 0.5;
  }
  
  endDrift() {
    this.drifting = false;
    this.driftAngle = 0;
  }
}
```

### 2. 赛道系统
```javascript
class Track {
  constructor(config) {
    this.points = config.points; // 赛道控制点
    this.width = config.width || 100;
    this.checkpoints = config.checkpoints;
  }
  
  isOnTrack(x, y) {
    // 检查点是否在赛道上
    for (let i = 0; i < this.points.length - 1; i++) {
      const p1 = this.points[i];
      const p2 = this.points[i + 1];
      const dist = this.distanceToSegment(x, y, p1.x, p1.y, p2.x, p2.y);
      if (dist < this.width / 2) return true;
    }
    return false;
  }
  
  distanceToSegment(px, py, x1, y1, x2, y2) {
    const dx = x2 - x1;
    const dy = y2 - y1;
    const t = Math.max(0, Math.min(1, ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy)));
    const nearX = x1 + t * dx;
    const nearY = y1 + t * dy;
    return Math.sqrt((px - nearX) ** 2 + (py - nearY) ** 2);
  }
}
```

### 3. AI 对手
```javascript
class AIDriver {
  constructor(car, track) {
    this.car = car;
    this.track = track;
    this.currentCheckpoint = 0;
    this.skill = 0.8; // AI 技能等级 0-1
  }
  
  update() {
    const target = this.track.checkpoints[this.currentCheckpoint];
    const angle = Math.atan2(target.y - this.car.y, target.x - this.car.x);
    const angleDiff = this.normalizeAngle(angle - this.car.angle);
    
    // 转向
    if (angleDiff > 0.1) {
      this.car.angle += this.car.turnSpeed * 0.016 * this.skill;
    } else if (angleDiff < -0.1) {
      this.car.angle -= this.car.turnSpeed * 0.016 * this.skill;
    }
    
    // 加速
    if (Math.abs(angleDiff) < 0.5) {
      this.car.speed = Math.min(
        this.car.speed + this.car.acceleration * 0.016 * this.skill,
        this.car.maxSpeed * this.skill
      );
    } else {
      this.car.speed *= 0.95;
    }
    
    // 检查是否到达检查点
    const dist = Phaser.Math.Distance.Between(this.car.x, this.car.y, target.x, target.y);
    if (dist < 50) {
      this.currentCheckpoint = (this.currentCheckpoint + 1) % this.track.checkpoints.length;
    }
  }
  
  normalizeAngle(angle) {
    while (angle > Math.PI) angle -= Math.PI * 2;
    while (angle < -Math.PI) angle += Math.PI * 2;
    return angle;
  }
}
```

### 4. 道具系统
```javascript
const POWERUPS = {
  boost: { name: '加速', duration: 2, effect: (car) => car.speed *= 1.5 },
  shield: { name: '护盾', duration: 3, effect: (car) => car.invulnerable = true },
  missile: { name: '导弹', effect: (car, target) => target.speed = 0 },
  oil: { name: '油渍', effect: (car) => car.drifting = true }
};
```

## 迭代策略

### 第一版：基础赛车
- 车辆物理
- 简单赛道
- 计时系统
- 基础 UI

### 第二版：AI 对手
- AI 赛车
- 排名系统
- 多条赛道
- 碰撞检测

### 第三版：道具系统
- 4 种道具
- 道具箱
- 道具动画
- 音效

### 第四版：深度玩法
- 车辆升级
- 车辆解锁
- 成就系统
- 排行榜

### 第五版：多人对战
- 多人联机
- 房间系统
- 聊天系统
- 赛季系统

## 常见错误

1. **操控太差**：赛车游戏的核心是操控，必须灵敏
2. **速度感不够**：要用视觉效果（模糊、缩放）增强速度感
3. **赛道太简单**：要有弯道、捷径、障碍物
4. **AI 太蠢**：AI 要有挑战性，但不能太强
5. **没有漂移**：漂移是赛车游戏的核心乐趣
