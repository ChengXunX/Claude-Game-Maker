---
name: game-debug
description: 游戏调试技能 - 自动检测和修复游戏常见问题
category: game-debug
triggerPattern: debug, fix, 修复, 调试, error, bug
---

# 游戏调试技能

## 概述

这是一个游戏调试技能，能够自动检测和修复游戏开发中的常见问题。包括：
- 运行时错误检测
- 碰撞检测问题
- 性能问题诊断
- 资源加载问题
- 逻辑错误修复

## 常见问题及修复方案

### 1. 物理引擎问题

**问题**: 角色穿墙或卡墙

**原因**: 碰撞体设置不当或物理引擎配置问题

**修复方案**:
```javascript
// 确保碰撞体大小正确
this.body.setSize(width * 0.8, height * 0.9)
this.body.setOffset(width * 0.1, height * 0.1)

// 确保碰撞检测开启
this.body.checkCollision.up = true
this.body.checkCollision.down = true
this.body.checkCollision.left = true
this.body.checkCollision.right = true

// 使用 collideWorldBounds
this.setCollideWorldBounds(true)
```

### 2. 资源加载问题

**问题**: 图片或音效加载失败

**原因**: 路径错误或资源未预加载

**修复方案**:
```javascript
// 确保在 preload 中加载所有资源
preload() {
  this.load.image('player', 'assets/images/player.png')
  this.load.audio('jump', 'assets/sounds/jump.mp3')

  // 添加加载错误处理
  this.load.on('loaderror', (file) => {
    console.error('资源加载失败:', file.src)
  })
}

// 使用回调确认加载完成
create() {
  // 资源已加载完成，可以安全使用
}
```

### 3. 内存泄漏问题

**问题**: 游戏运行越来越卡

**原因**: 对象未正确销毁

**修复方案**:
```javascript
// 销毁不需要的对象
enemy.destroy()

// 清除事件监听器
this.events.off('eventName')

// 使用对象池
this.enemyPool = this.physics.add.group({
  classType: Enemy,
  maxSize: 20,
  runChildUpdate: true
})

// 从池中获取
const enemy = this.enemyPool.get(x, y)

// 回收到池
this.enemyPool.killAndHide(enemy)
```

### 4. 碰撞检测问题

**问题**: 碰撞检测不准确

**原因**: 碰撞体类型不匹配

**修复方案**:
```javascript
// 使用正确的碰撞体类型
// 静态碰撞体（不移动）
this.platforms = this.physics.add.staticGroup()

// 动态碰撞体（会移动）
this.enemies = this.physics.add.group()

// 设置碰撞
this.physics.add.collider(player, platforms)  // 碰撞
this.physics.add.overlap(player, coins, collectCoin)  // 重叠检测
```

### 5. 动画问题

**问题**: 动画不播放或卡顿

**原因**: 动画配置错误或帧率问题

**修复方案**:
```javascript
// 正确创建动画
this.anims.create({
  key: 'walk',
  frames: this.anims.generateFrameNumbers('player', { start: 0, end: 3 }),
  frameRate: 10,
  repeat: -1
})

// 播放动画
this.play('walk', true)

// 检查动画是否存在
if (this.anims.exists('walk')) {
  this.play('walk')
}
```

### 6. 输入处理问题

**问题**: 按键无响应或冲突

**原因**: 输入系统配置错误

**修复方案**:
```javascript
// 正确创建按键
this.cursors = this.input.keyboard.createCursorKeys()
this.spaceKey = this.input.keyboard.addKey('SPACE')

// 使用 JustDown 防止重复触发
if (Phaser.Input.Keyboard.JustDown(this.spaceKey)) {
  this.jump()
}

// 禁用默认浏览器行为
this.input.keyboard.addCapture(['SPACE', 'UP', 'DOWN'])
```

### 7. 性能优化

**问题**: 游戏帧率低

**优化方案**:
```javascript
// 1. 使用对象池
this.bulletPool = this.physics.add.group({
  classType: Bullet,
  maxSize: 100,
  runChildUpdate: false
})

// 2. 减少碰撞检测范围
this.physics.world.setBounds(0, 0, width, height)

// 3. 使用纹理图集
this.load.atlas('sprites', 'assets/sprites.png', 'assets/sprites.json')

// 4. 限制更新频率
if (this.timer % 2 === 0) {
  this.updateEnemyAI()
}

// 5. 使用 camera culling
this.cameras.main.setBounds(0, 0, worldWidth, worldHeight)
```

### 8. 游戏状态管理

**问题**: 状态混乱或丢失

**修复方案**:
```javascript
// 使用场景管理状态
this.scene.start('GameScene', {
  level: this.level,
  score: this.score,
  lives: this.lives
})

// 暂停和恢复
this.scene.pause('GameScene')
this.scene.resume('GameScene')

// 场景间传递数据
this.scene.launch('PauseMenu', { gameScene: this })
```

## 调试流程

1. **收集信息**: 获取错误日志和复现步骤
2. **定位问题**: 分析代码找出问题根源
3. **制定修复方案**: 根据问题类型选择修复方案
4. **实施修复**: 修改代码并测试
5. **验证修复**: 确认问题已解决且无新问题

## 自动调试命令

```bash
# 运行游戏并捕获错误
npm run dev 2>&1 | tee game.log

# 分析错误日志
grep -i "error\|exception\|failed" game.log

# 检查资源文件
find assets/ -type f -name "*.png" -o -name "*.jpg" -o -name "*.mp3" | wc -l

# 检查代码质量
npx eslint src/ --ext .js
```

## 最佳实践

1. **错误处理**: 始终添加 try-catch 和错误回调
2. **资源管理**: 使用对象池和资源预加载
3. **性能监控**: 定期检查帧率和内存使用
4. **代码审查**: 定期审查代码质量
5. **测试覆盖**: 编写单元测试和集成测试
