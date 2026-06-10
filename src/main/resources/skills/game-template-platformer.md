---
name: 平台跳跃游戏开发模板
description: 平台跳跃游戏开发模板，适用于横版动作类游戏
trigger: platformer, platform game, 跳跃, 横版, side-scrolling, 马里奥
examples: Super Mario|Celeste|Hollow Knight|Ori|Dead Cells
---

# 平台跳跃游戏开发模板

## 游戏设计核心原则

### 核心循环（每关 2-5 分钟）
```
观察地形 → 跳跃/移动 → 收集物品 → 躲避/消灭敌人 → 到达终点
```
- 跳跃必须**手感好**（响应快、空中可微调、落地缓冲）
- 死亡必须**公平**（玩家觉得是自己的错，不是游戏的错）
- 检查点必须**频繁**（每 30-60 秒一个检查点）

### 玩家心理学
- **掌控感**：玩家觉得"我能精确控制角色"
- **探索欲**：隐藏区域、秘密通道激发好奇心
- **成长感**：新能力解锁后可以到达之前到不了的地方
- **挑战欲**：高难度关卡激发"再试一次"的冲动

### 手感优化（最重要！）
```
跳跃手感公式：
1. 按住跳跃键时间越长，跳得越高（变量跳跃）
2. 空中可以微调水平方向（空中控制）
3. 下落速度比上升速度快（重力感）
4. 落地前 0.1 秒按跳跃键可以"土狼跳"（宽容窗口）
5. 离开平台后 0.1 秒内仍可跳跃（边缘跳跃）
```

## 核心系统设计

### 1. 玩家控制（手感核心）
```javascript
class Player extends Phaser.Physics.Arcade.Sprite {
  constructor(scene, x, y) {
    super(scene, x, y, 'player');
    scene.add.existing(this);
    scene.physics.add.existing(this);
    
    // 物理参数（调整这些改变手感）
    this.setCollideWorldBounds(true);
    this.setBounce(0);
    this.setDragX(1000); // 地面摩擦
    this.setMaxVelocity(300, 600);
    
    // 状态
    this.health = 3;
    this.isInvincible = false;
    this.canDoubleJump = false;
    this.hasDoubleJumped = false;
    this.coyoteTime = 0; // 土狼时间
    this.jumpBufferTime = 0; // 跳跃缓冲
  }

  update(cursors) {
    // 水平移动
    if (cursors.left.isDown) {
      this.setVelocityX(-200);
      this.setFlipX(true);
    } else if (cursors.right.isDown) {
      this.setVelocityX(200);
      this.setFlipX(false);
    } else {
      this.setVelocityX(0);
    }
    
    // 土狼时间（离开平台后短暂时间内仍可跳跃）
    if (this.body.touching.down) {
      this.coyoteTime = 100; // 100ms 宽容窗口
    } else {
      this.coyoteTime = Math.max(0, this.coyoteTime - 16);
    }
    
    // 跳跃缓冲（提前按跳跃键）
    if (cursors.up.isDown) {
      this.jumpBufferTime = 100;
    } else {
      this.jumpBufferTime = Math.max(0, this.jumpBufferTime - 16);
    }
    
    // 跳跃逻辑
    if (this.jumpBufferTime > 0 && this.coyoteTime > 0) {
      this.setVelocityY(-400); // 跳跃力度
      this.coyoteTime = 0;
      this.jumpBufferTime = 0;
      this.hasDoubleJumped = false;
    }
    
    // 变量跳跃（松开跳跃键减速）
    if (!cursors.up.isDown && this.body.velocity.y < -100) {
      this.setVelocityY(this.body.velocity.y * 0.85);
    }
    
    // 二段跳
    if (this.canDoubleJump && !this.body.touching.down && 
        Phaser.Input.Keyboard.JustDown(cursors.up) && !this.hasDoubleJumped) {
      this.setVelocityY(-350);
      this.hasDoubleJumped = true;
    }
    
    // 重力加速度（下落更快）
    if (this.body.velocity.y > 0) {
      this.body.setGravityY(300);
    } else {
      this.body.setGravityY(0);
    }
  }
}
```

### 2. 关卡设计原则
```
教程区: 教基本操作（移动、跳跃、攻击）
安全区: 放松，收集物品
挑战区: 需要精确操作
高潮区: 关卡最难的部分
奖励区: 隐藏区域，高价值物品
```

### 3. 敌人设计
| 敌人类型 | 行为 | 击杀方式 | 出现场景 |
|----------|------|----------|----------|
| 巡逻兵 | 左右移动 | 踩踏/攻击 | 基础关卡 |
| 飞行兵 | 上下移动 | 攻击 | 进阶关卡 |
| 射手兵 | 固定位置射击 | 躲避+攻击 | 挑战关卡 |
| BOSS | 多阶段攻击 | 找弱点 | 关卡结尾 |

### 4. 能力解锁
| 能力 | 解锁时机 | 作用 |
|------|----------|------|
| 基础跳跃 | 游戏开始 | 跨越障碍 |
| 二段跳 | 第 3 关 | 到达更高平台 |
| 冲刺 | 第 5 关 | 快速移动/跨越大坑 |
| 墙跳 | 第 8 关 | 攀爬墙壁 |
| 下砸 | 第 10 关 | 破坏地面障碍 |

## 关键技术实现

### 碰撞检测
```javascript
setupCollisions() {
  // 玩家与平台
  this.physics.add.collider(this.player, this.platforms);
  
  // 敌人与平台
  this.physics.add.collider(this.enemies, this.platforms);
  
  // 玩家与金币
  this.physics.add.overlap(this.player, this.coins, this.collectCoin, null, this);
  
  // 玩家与敌人（区分踩踏和碰撞）
  this.physics.add.overlap(this.player, this.enemies, this.hitEnemy, null, this);
}

hitEnemy(player, enemy) {
  if (player.body.velocity.y > 0 && player.y < enemy.y - 10) {
    // 踩踏：玩家在敌人上方且向下移动
    enemy.takeDamage(1);
    player.setVelocityY(-300); // 弹起
    this.score += 100;
    this.showScorePopup(enemy.x, enemy.y, '+100');
  } else if (!player.isInvincible) {
    // 被撞：受伤
    player.takeDamage(1);
  }
}
```

### 视差背景
```javascript
createParallaxBackground() {
  // 多层背景，越远的层移动越慢
  this.bg1 = this.add.tileSprite(0, 0, 800, 600, 'bg_sky')
    .setOrigin(0, 0).setScrollFactor(0);
  this.bg2 = this.add.tileSprite(0, 0, 800, 600, 'bg_mountains')
    .setOrigin(0, 0).setScrollFactor(0);
  this.bg3 = this.add.tileSprite(0, 0, 800, 600, 'bg_trees')
    .setOrigin(0, 0).setScrollFactor(0);
}

update() {
  // 背景跟随相机移动，但速度不同
  this.bg1.tilePositionX = this.cameras.main.scrollX * 0.1;
  this.bg2.tilePositionX = this.cameras.main.scrollX * 0.3;
  this.bg3.tilePositionX = this.cameras.main.scrollX * 0.5;
}
```

### 检查点系统
```javascript
class Checkpoint extends Phaser.Physics.Arcade.Sprite {
  constructor(scene, x, y) {
    super(scene, x, y, 'checkpoint');
    scene.add.existing(this);
    scene.physics.add.existing(this, true); // 静态
    
    this.activated = false;
  }
  
  activate(player) {
    if (this.activated) return;
    
    this.activated = true;
    this.play('checkpoint_activate');
    
    // 保存检查点位置
    player.checkpoint = { x: this.x, y: this.y };
    
    // 显示提示
    this.scene.showMessage('检查点已激活！');
  }
}
```

## 关卡设计

### 关卡节奏
```
第1关: 教学关 - 只有平台和金币，教基本操作
第2关: 简单关 - 引入敌人，教踩踏
第3关: 跳跃关 - 需要精确跳跃，解锁二段跳
第4关: 收集关 - 收集所有星星才能过关
第5关: 速度关 - 限时通过，解锁冲刺
第6关: 探索关 - 大地图，隐藏通道
第7关: 战斗关 - 大量敌人
第8关: 攀爬关 - 垂直关卡，解锁墙跳
第9关: 综合关 - 结合所有机制
第10关: BOSS关 - BOSS 战
```

### 隐藏要素
- 隐藏通道（墙壁可以穿过）
- 隐藏金币（需要特定能力才能到达）
- 秘密关卡（收集所有星星解锁）
- 成就系统（挑战性目标）

## 迭代策略

### 第一版：最小可玩版本
- 基础移动和跳跃
- 1 个关卡
- 平台和金币
- 简单 UI

### 第二版：核心手感
- 优化跳跃手感（变量跳跃、土狼跳）
- 添加敌人和踩踏
- 添加 3 个关卡
- 添加音效

### 第三版：能力系统
- 添加二段跳
- 添加冲刺
- 添加 5 个关卡
- 添加检查点

### 第四版：内容扩展
- 添加 BOSS
- 添加隐藏区域
- 添加 10 个关卡
- 添加成就系统

### 第五版：打磨
- 优化手感细节
- 添加粒子效果
- 优化关卡设计
- 添加音乐

## 常见错误

1. **手感差**：跳跃必须响应快、可微调、有宽容窗口
2. **死亡太频繁**：检查点要频繁，死亡惩罚要轻
3. **关卡太长**：每关 2-5 分钟，太长会疲惫
4. **没有教程**：必须渐进式教玩家新机制
5. **视觉反馈弱**：跳跃、落地、受伤都要有动画和音效
