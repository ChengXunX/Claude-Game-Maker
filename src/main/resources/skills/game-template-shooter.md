---
name: 射击游戏开发模板
description: 射击游戏开发模板，适用于俯视角射击、弹幕、双摇杆射击类游戏
trigger: shooter, FPS, 射击, gun, 枪战, bullet, 子弹, 弹幕, 双摇杆
examples: Vampire Survivors|Enter the Gungeon|Geometry Wars|Bullet Hell|Robotron
---

# 射击游戏开发模板

## 游戏设计核心原则

### 核心循环（每局 5-20 分钟）
```
移动躲避 → 瞄准射击 → 消灭敌人 → 拾取奖励 → 遇到更强敌人
```
- **射击手感**：子弹必须有重量感，击中必须有反馈
- **弹幕美感**：子弹轨迹要有视觉美感
- **紧张感**：敌人越来越多，空间越来越小

### 玩家心理学
- **掌控感**：精确控制角色移动和射击方向
- **爽感**：一梭子消灭一群敌人的满足感
- **紧张感**：弹幕中穿梭的刺激感
- **成长感**：武器越来越强，敌人越来越难

### 武器设计要点
```
每种武器要有独特手感：
- 手枪：精准、单发、适合远距离
- 霰弹枪：散射、伤害高、适合近距离
- 机枪：连射、弹幕、适合压制
- 激光：穿透、持续、适合直线敌人
- 火箭筒：范围爆炸、伤害高、适合群敌
```

## 核心系统设计

### 1. 武器系统
```javascript
const WEAPONS = {
  pistol: {
    name: '手枪',
    damage: 10,
    fireRate: 300, // 毫秒
    bulletSpeed: 500,
    spread: 0, // 散射角度
    bulletsPerShot: 1,
    penetration: 0, // 穿透数
    explosive: false
  },
  shotgun: {
    name: '霰弹枪',
    damage: 8,
    fireRate: 800,
    bulletSpeed: 400,
    spread: 30, // 30度散射
    bulletsPerShot: 5,
    penetration: 0,
    explosive: false
  },
  machinegun: {
    name: '机枪',
    damage: 5,
    fireRate: 100,
    bulletSpeed: 600,
    spread: 5,
    bulletsPerShot: 1,
    penetration: 0,
    explosive: false
  },
  laser: {
    name: '激光',
    damage: 3, // 每帧伤害
    fireRate: 0, // 持续射击
    bulletSpeed: 0, // 即时命中
    spread: 0,
    bulletsPerShot: 1,
    penetration: 999, // 无限穿透
    explosive: false
  },
  rocket: {
    name: '火箭筒',
    damage: 50,
    fireRate: 1500,
    bulletSpeed: 300,
    spread: 0,
    bulletsPerShot: 1,
    penetration: 0,
    explosive: true,
    explosionRadius: 80
  }
};
```

### 2. 子弹系统（对象池）
```javascript
class BulletPool {
  constructor(scene, maxSize = 500) {
    this.scene = scene;
    this.pool = [];
    this.active = [];
    this.maxSize = maxSize;
    
    // 预创建子弹
    for (let i = 0; i < maxSize; i++) {
      const bullet = scene.add.sprite(0, 0, 'bullet');
      bullet.setVisible(false);
      bullet.setActive(false);
      this.pool.push(bullet);
    }
  }
  
  fire(x, y, angle, config) {
    const bullet = this.pool.pop();
    if (!bullet) return null;
    
    bullet.setPosition(x, y);
    bullet.setRotation(angle);
    bullet.setVisible(true);
    bullet.setActive(true);
    
    // 设置物理属性
    bullet.damage = config.damage;
    bullet.speed = config.bulletSpeed;
    bullet.penetration = config.penetration;
    bullet.explosive = config.explosive;
    bullet.explosionRadius = config.explosionRadius;
    
    // 设置速度
    this.scene.physics.velocityFromRotation(
      angle, config.bulletSpeed, bullet.body.velocity
    );
    
    this.active.push(bullet);
    return bullet;
  }
  
  update() {
    // 回收超出屏幕的子弹
    for (let i = this.active.length - 1; i >= 0; i--) {
      const bullet = this.active[i];
      if (!this.scene.cameras.main.getBounds().contains(bullet.x, bullet.y)) {
        this.release(bullet);
        this.active.splice(i, 1);
      }
    }
  }
  
  release(bullet) {
    bullet.setVisible(false);
    bullet.setActive(false);
    bullet.body.velocity.setTo(0, 0);
    this.pool.push(bullet);
  }
}
```

### 3. 敌人 AI
```javascript
class EnemyAI {
  constructor(enemy, player) {
    this.enemy = enemy;
    this.player = player;
    this.state = 'CHASE'; // CHASE, ATTACK, FLEE
    this.attackRange = 200;
    this.fleeRange = 100;
  }
  
  update() {
    const dist = Phaser.Math.Distance.Between(
      this.enemy.x, this.enemy.y,
      this.player.x, this.player.y
    );
    
    if (dist < this.fleeRange) {
      this.state = 'FLEE';
    } else if (dist < this.attackRange) {
      this.state = 'ATTACK';
    } else {
      this.state = 'CHASE';
    }
    
    switch (this.state) {
      case 'CHASE':
        this.chasePlayer();
        break;
      case 'ATTACK':
        this.attackPlayer();
        break;
      case 'FLEE':
        this.fleeFromPlayer();
        break;
    }
  }
  
  chasePlayer() {
    const angle = Phaser.Math.Angle.Between(
      this.enemy.x, this.enemy.y,
      this.player.x, this.player.y
    );
    this.enemy.body.velocity.x = Math.cos(angle) * this.enemy.speed;
    this.enemy.body.velocity.y = Math.sin(angle) * this.enemy.speed;
  }
  
  attackPlayer() {
    // 面向玩家
    this.enemy.rotation = Phaser.Math.Angle.Between(
      this.enemy.x, this.enemy.y,
      this.player.x, this.player.y
    );
    // 射击
    if (this.enemy.canFire()) {
      this.enemy.fire();
    }
  }
  
  fleeFromPlayer() {
    const angle = Phaser.Math.Angle.Between(
      this.player.x, this.player.y,
      this.enemy.x, this.enemy.y
    );
    this.enemy.body.velocity.x = Math.cos(angle) * this.enemy.speed;
    this.enemy.body.velocity.y = Math.sin(angle) * this.enemy.speed;
  }
}
```

### 4. 敌人波次系统
```javascript
const WAVES = [
  { enemies: [{ type: 'basic', count: 5 }], delay: 1000 },
  { enemies: [{ type: 'basic', count: 8 }, { type: 'fast', count: 2 }], delay: 800 },
  { enemies: [{ type: 'basic', count: 10 }, { type: 'fast', count: 5 }], delay: 600 },
  { enemies: [{ type: 'basic', count: 15 }, { type: 'tank', count: 2 }], delay: 500 },
  { enemies: [{ type: 'boss', count: 1 }], delay: 0 }
];
```

## 迭代策略

### 第一版：核心射击
- 玩家移动和射击
- 1 种武器（手枪）
- 1 种敌人（基础兵）
- 基础碰撞检测

### 第二版：武器系统
- 添加 3 种武器（霰弹枪、机枪、火箭筒）
- 武器切换
- 子弹特效
- 敌人死亡动画

### 第三版：敌人多样性
- 添加 5 种敌人类型
- 敌人 AI（追击、攻击、逃跑）
- 波次系统
- BOSS 战

### 第四版：成长系统
- 经验值和升级
- 武器升级
- 道具掉落
- 永久死亡（可选）

### 第五版：打磨
- 优化手感
- 添加粒子效果
- 添加音效和音乐
- 平衡难度

## 常见错误

1. **子弹太慢**：射击游戏的子弹要快，让人感觉有力量
2. **没有击中反馈**：击中敌人必须有视觉+音效反馈
3. **敌人太弱**：没有挑战感，玩家会无聊
4. **武器没有差异**：每种武器必须有独特手感
5. **屏幕太乱**：敌人和子弹太多会看不清，需要视觉层次
