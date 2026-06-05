---
name: game-template-racing
description: 赛车游戏模板 - 提供完整的赛车游戏项目骨架
category: game-template
triggerPattern: racing, race, 赛车, car, 汽车, drift, 漂移
---

# 赛车游戏模板

## 概述

俯视角赛车游戏模板，基于 Phaser 3 引擎。包含：
- 车辆物理（加速、刹车、漂移）
- 赛道系统（弯道、直道、障碍物）
- AI 对手（路径追踪）
- 计时系统（圈速、排名）
- 道具系统（加速、护盾）

## 核心代码

### 车辆控制

```javascript
export class Car extends Phaser.Physics.Arcade.Sprite {
  constructor(scene, x, y, config) {
    super(scene, x, y, config.texture)
    this.speed = 0
    this.maxSpeed = config.maxSpeed || 300
    this.acceleration = config.acceleration || 200
    this.brakeForce = config.brakeForce || 300
    this.turnSpeed = config.turnSpeed || 150
    this.drag = config.drag || 0.98
    this.driftFactor = config.driftFactor || 0.95
    this.isDrifting = false
  }

  update(cursors) {
    // 加速
    if (cursors.up.isDown) {
      this.speed = Math.min(this.speed + this.acceleration * 0.016, this.maxSpeed)
    }
    // 刹车
    if (cursors.down.isDown) {
      this.speed = Math.max(this.speed - this.brakeForce * 0.016, -this.maxSpeed * 0.3)
    }
    // 漂移
    this.isDrifting = cursors.shift.isDown
    const turnMultiplier = this.isDrifting ? 1.5 : 1

    // 转向
    if (cursors.left.isDown) {
      this.angle -= this.turnSpeed * 0.016 * turnMultiplier
    }
    if (cursors.right.isDown) {
      this.angle += this.turnSpeed * 0.016 * turnMultiplier
    }

    // 应用摩擦力
    this.speed *= this.isDrifting ? this.driftFactor : this.drag

    // 更新位置
    const rad = Phaser.Math.DegToRad(this.angle)
    this.body.setVelocity(
      Math.cos(rad) * this.speed,
      Math.sin(rad) * this.speed
    )
  }
}
```

### AI 对手

```javascript
export class AICar extends Car {
  constructor(scene, x, y, config, waypoints) {
    super(scene, x, y, config)
    this.waypoints = waypoints
    this.currentWaypoint = 0
    this.speed = config.maxSpeed * 0.8
  }

  update() {
    const target = this.waypoints[this.currentWaypoint]
    const angle = Phaser.Math.Angle.Between(this.x, this.y, target.x, target.y)
    const distance = Phaser.Math.Distance.Between(this.x, this.y, target.x, target.y)

    // 转向目标
    const targetAngle = Phaser.Math.RadToDeg(angle)
    const diff = targetAngle - this.angle
    this.angle += diff * 0.1

    // 移动
    const rad = Phaser.Math.DegToRad(this.angle)
    this.body.setVelocity(
      Math.cos(rad) * this.speed,
      Math.sin(rad) * this.speed
    )

    // 到达路点后切换下一个
    if (distance < 50) {
      this.currentWaypoint = (this.currentWaypoint + 1) % this.waypoints.length
    }
  }
}
```

## 使用方法

1. 使用此模板创建新项目
2. 在 `assets/tracks/` 中添加赛道数据
3. 在 `config.js` 中调整车辆参数
4. 运行 `npm run dev` 预览游戏
