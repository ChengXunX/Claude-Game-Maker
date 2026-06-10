---
name: 恐怖游戏开发模板
description: 恐怖游戏开发模板，适用于生存恐怖、心理恐怖、解谜恐怖类游戏
trigger: 恐怖游戏,horror game,生存恐怖,心理恐怖,恐怖解谜
examples: 生化危机|寂静岭|逃生|港诡实录|纸嫁衣|Phasmophobia
---

# 恐怖游戏开发模板

## 游戏设计核心原则

### 核心循环（每 10-30 分钟一章）
```
探索环境 → 收集线索 → 解开谜题 → 遭遇恐怖 → 逃出生天
```
- **恐惧氛围**：让玩家感到不安
- **紧张节奏**：松紧交替的节奏
- **沉浸感**：让玩家忘记这是游戏

### 玩家心理学
- **"未知恐惧"**：不知道什么在等着你
- **"孤立无援"**：独自面对危险
- **"心跳加速"**：紧张时的生理反应
- **"劫后余生"**：逃脱后的放松感

### 恐怖设计要点
```
恐怖氛围营造：
1. 视觉：昏暗灯光、狭窄视野、血腥场景
2. 听觉：环境音效、心跳声、突然的声响
3. 心理：未知恐惧、孤立感、压迫感
4. 节奏：松紧交替，不能一直吓人
```

## 核心系统设计

### 1. 恐惧值系统
```javascript
class FearSystem {
  constructor() {
    this.fearLevel = 0; // 0-100
    this.maxFear = 100;
    this.fearDecayRate = 0.5; // 每秒下降
    this.fearThresholds = {
      calm: 30,
      nervous: 60,
      panic: 80
    };
  }
  
  addFear(amount) {
    this.fearLevel = Math.min(this.maxFear, this.fearLevel + amount);
    this.applyFearEffects();
  }
  
  update(delta) {
    // 恐惧值自然下降
    this.fearLevel = Math.max(0, this.fearLevel - this.fearDecayRate * delta);
    this.applyFearEffects();
  }
  
  applyFearEffects() {
    if (this.fearLevel >= this.fearThresholds.panic) {
      // 恐慌状态：视野变窄、心跳加速、移动变慢
      this.applyPanicEffects();
    } else if (this.fearLevel >= this.fearThresholds.nervous) {
      // 紧张状态：轻微视野变化
      this.applyNervousEffects();
    } else {
      // 冷静状态
      this.applyCalmEffects();
    }
  }
  
  applyPanicEffects() {
    // 视野变窄
    this.camera.setZoom(0.8);
    // 心跳音效
    this.audio.playHeartbeat('fast');
    // 移动变慢
    this.player.speed *= 0.7;
  }
}
```

### 2. 视野系统
```javascript
class VisionSystem {
  constructor() {
    this.fov = 90; // 视野角度
    this.maxDistance = 500; // 最大视野距离
    this.lightLevel = 1; // 光照等级 0-1
  }
  
  update(delta) {
    // 根据恐惧值调整视野
    if (this.fearSystem.fearLevel > 60) {
      this.fov = Math.max(45, this.fov - 0.5 * delta);
    } else {
      this.fov = Math.min(90, this.fov + 0.2 * delta);
    }
    
    // 根据环境调整光照
    this.updateLightLevel();
  }
  
  isVisible(target) {
    const distance = this.getDistance(this.player, target);
    const angle = this.getAngle(this.player, target);
    
    // 检查距离
    if (distance > this.maxDistance) return false;
    
    // 检查角度
    if (Math.abs(angle) > this.fov / 2) return false;
    
    // 检查光照
    if (this.lightLevel < 0.3) {
      // 黑暗中只能看到很近的东西
      return distance < 100;
    }
    
    return true;
  }
}
```

### 3. 跳吓系统
```javascript
class JumpScareSystem {
  constructor() {
    this.triggers = [];
    this.cooldown = 0;
  }
  
  addTrigger(config) {
    this.triggers.push({
      id: config.id,
      position: config.position,
      condition: config.condition,
      animation: config.animation,
      sound: config.sound,
      fearAmount: config.fearAmount || 30,
      triggered: false
    });
  }
  
  checkTriggers(playerPosition) {
    if (this.cooldown > 0) return;
    
    for (const trigger of this.triggers) {
      if (trigger.triggered) continue;
      
      const distance = this.getDistance(playerPosition, trigger.position);
      
      if (distance < 50 && trigger.condition()) {
        this.executeJumpScare(trigger);
        trigger.triggered = true;
        this.cooldown = 5; // 5 秒冷却
      }
    }
  }
  
  executeJumpScare(trigger) {
    // 播放跳吓动画
    this.playAnimation(trigger.animation);
    
    // 播放跳吓音效
    this.playSound(trigger.sound);
    
    // 增加恐惧值
    this.fearSystem.addFear(trigger.fearAmount);
    
    // 屏幕震动
    this.camera.shake(0.02, 500);
  }
}
```

### 4. 解谜系统
```javascript
class PuzzleSystem {
  constructor() {
    this.puzzles = [];
    this.solvedPuzzles = [];
  }
  
  addPuzzle(config) {
    this.puzzles.push({
      id: config.id,
      type: config.type,
      clues: config.clues,
      solution: config.solution,
      reward: config.reward,
      solved: false
    });
  }
  
  checkSolution(puzzleId, answer) {
    const puzzle = this.puzzles.find(p => p.id === puzzleId);
    if (!puzzle) return false;
    
    if (answer === puzzle.solution) {
      puzzle.solved = true;
      this.solvedPuzzles.push(puzzle);
      this.giveReward(puzzle.reward);
      return true;
    }
    
    return false;
  }
  
  giveReward(reward) {
    switch (reward.type) {
      case 'key':
        this.inventory.addItem('key', reward.id);
        this.showMessage(`获得钥匙: ${reward.name}`);
        break;
      case 'document':
        this.showDocument(reward.content);
        break;
      case 'access':
        this.unlockArea(reward.area);
        break;
    }
  }
}
```

### 5. 音效系统
```javascript
class HorrorAudioSystem {
  constructor() {
    this.ambientSounds = [];
    this.heartbeat = null;
    this.stingerSounds = [];
  }
  
  update(delta) {
    // 根据恐惧值调整音效
    if (this.fearSystem.fearLevel > 60) {
      this.playHeartbeat('fast');
      this.playAmbient('tense');
    } else if (this.fearSystem.fearLevel > 30) {
      this.playHeartbeat('normal');
      this.playAmbient('uneasy');
    } else {
      this.stopHeartbeat();
      this.playAmbient('calm');
    }
  }
  
  playStinger(soundId) {
    // 播放惊吓音效
    const sound = this.stingerSounds.find(s => s.id === soundId);
    if (sound) {
      this.audio.play(sound.file, { volume: 0.8 });
    }
  }
  
  playHeartbeat(type) {
    if (this.heartbeat) {
      this.heartbeat.stop();
    }
    
    const file = type === 'fast' ? 'heartbeat_fast.mp3' : 'heartbeat_normal.mp3';
    this.heartbeat = this.audio.play(file, { loop: true, volume: 0.5 });
  }
}
```

## 迭代策略

### 第一版：基础恐怖
- 简单场景
- 基础音效
- 1 个跳吓
- 简单解谜

### 第二版：氛围营造
- 恐惧值系统
- 视野系统
- 环境音效
- 多个跳吓

### 第三版：解谜系统
- 多种谜题
- 线索系统
- 道具系统
- 剧情推进

### 第四版：深度恐怖
- AI 敌人
- 多种恐怖元素
- 多结局
- 成就系统

### 第五版：多人恐怖
- 多人合作
- 语音聊天
- 排行榜
- 社区功能

## 常见错误

1. **恐怖疲劳**：不能一直吓人，要有节奏
2. **音效太吵**：音效要适度，不能让人不适
3. **画面太暗**：要能看到路，不能完全看不见
4. **没有引导**：要有线索引导玩家
5. **跳吓太廉价**：跳吓要有铺垫，不能太突然
