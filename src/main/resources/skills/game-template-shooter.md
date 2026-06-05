---
name: game-template-shooter
description: 射击游戏模板 - 提供完整的射击游戏项目骨架
category: game-template
triggerPattern: shooter, FPS, 射击, gun, 枪战, bullet, 子弹
---

# 射击游戏模板

## 概述

俯视角射击游戏模板，基于 Phaser 3 引擎。包含：
- 玩家控制（移动、瞄准、射击）
- 武器系统（多种武器、切换）
- 敌人系统（AI 寻路、射击）
- 子弹系统（碰撞检测）
- 道具系统（血包、弹药）
- 关卡系统

## 核心代码

### 武器配置

```javascript
export const WEAPONS = {
  pistol: {
    name: '手枪',
    damage: 10,
    fireRate: 300,
    bulletSpeed: 500,
    spread: 0.05,
    ammo: Infinity,
    texture: 'bullet-pistol'
  },
  shotgun: {
    name: '霰弹枪',
    damage: 8,
    fireRate: 800,
    bulletSpeed: 400,
    spread: 0.3,
    pellets: 5,
    ammo: 30,
    texture: 'bullet-shotgun'
  },
  rifle: {
    name: '步枪',
    damage: 15,
    fireRate: 100,
    bulletSpeed: 600,
    spread: 0.02,
    ammo: 120,
    texture: 'bullet-rifle'
  }
}
```

### 玩家控制

```javascript
export class Player extends Phaser.Physics.Arcade.Sprite {
  constructor(scene, x, y) {
    super(scene, x, y, 'player')
    this.speed = 200
    this.health = 100
    this.currentWeapon = 'pistol'
    this.ammo = {}
    this.lastFired = 0
  }

  update(cursors, pointer) {
    // 移动
    let vx = 0, vy = 0
    if (cursors.left.isDown) vx = -this.speed
    if (cursors.right.isDown) vx = this.speed
    if (cursors.up.isDown) vy = -this.speed
    if (cursors.down.isDown) vy = this.speed
    this.setVelocity(vx, vy)

    // 瞄准鼠标
    const angle = Phaser.Math.Angle.Between(this.x, this.y, pointer.x, pointer.y)
    this.setRotation(angle)

    // 射击
    if (pointer.isDown) {
      this.shoot(angle)
    }
  }

  shoot(angle) {
    const weapon = WEAPONS[this.currentWeapon]
    const now = Date.now()
    if (now - this.lastFired < weapon.fireRate) return
    this.lastFired = now

    const pellets = weapon.pellets || 1
    for (let i = 0; i < pellets; i++) {
      const spread = (Math.random() - 0.5) * weapon.spread
      const bulletAngle = angle + spread
      const bullet = new Bullet(this.scene, this.x, this.y, bulletAngle, weapon)
      this.scene.bullets.add(bullet)
    }
  }
}
```

### 子弹类

```javascript
export class Bullet extends Phaser.Physics.Arcade.Image {
  constructor(scene, x, y, angle, weapon) {
    super(scene, x, y, weapon.texture)
    this.speed = weapon.bulletSpeed
    this.damage = weapon.damage

    scene.physics.velocityFromRotation(angle, this.speed, this.body)
    this.setRotation(angle)

    scene.time.delayedCall(2000, () => this.destroy())
  }
}
```

## 使用方法

1. 使用此模板创建新项目
2. 替换 `assets/images/` 中的图片资源
3. 在 `config.js` 中调整武器参数
4. 运行 `npm run dev` 预览游戏
