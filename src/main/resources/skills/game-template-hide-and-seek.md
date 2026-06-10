---
name: 躲猫猫游戏开发模板
description: 躲猫猫游戏开发模板，适用于躲藏、寻找、追逐类游戏
trigger: hide and seek, 躲猫猫, 捉迷藏, 捕捉, 追逐, chase, seeker, hider
examples: 躲猫猫|捉迷藏|Prop Hunt|Among Us|Dead by Daylight
---

# 躲猫猫游戏开发模板

## 游戏设计核心原则

### 核心循环（每局 5-15 分钟）
```
选择阵营 → 躲藏/寻找 → 追逐/躲藏 → 被抓/找到 → 结算
```
- **紧张感**：随时可能被发现
- **策略性**：选择躲藏位置
- **刺激感**：追逐的紧张感

### 玩家心理学
- **"躲藏成功"的成就感**：成功躲藏的满足感
- **"找到猎物"的快感**：找到躲藏者的爽感
- **"追逐刺激"的紧张感**：追逐的紧张感
- **"团队合作"的乐趣**：和队友配合

### 躲猫猫设计要点
```
躲猫猫核心：
1. 阵营平衡：躲藏者和寻找者要平衡
2. 地图设计：要有足够躲藏点
3. 视野系统：寻找者有视野限制
4. 时间限制：不能无限躲藏
```

## 核心系统设计

### 1. 阵营系统
```javascript
class TeamSystem {
  constructor() {
    this.hiders = [];
    this.seekers = [];
    this.maxHiders = 4;
    this.maxSeekers = 1;
  }
  
  joinTeam(player, team) {
    if (team === 'hider' && this.hiders.length < this.maxHiders) {
      this.hiders.push(player);
      player.team = 'hider';
      return true;
    }
    
    if (team === 'seeker' && this.seekers.length < this.maxSeekers) {
      this.seekers.push(player);
      player.team = 'seeker';
      return true;
    }
    
    return false;
  }
  
  getTeamCount() {
    return {
      hiders: this.hiders.length,
      seekers: this.seekers.length
    };
  }
  
  checkWinCondition() {
    // 所有躲藏者都被找到
    if (this.hiders.every(h => h.caught)) {
      return 'seekers';
    }
    
    // 时间到
    if (this.time <= 0) {
      return 'hiders';
    }
    
    return null;
  }
}
```

### 2. 躲藏系统
```javascript
class HideSystem {
  constructor() {
    this.hideSpots = [];
    this.occupiedSpots = [];
  }
  
  addHideSpot(config) {
    this.hideSpots.push({
      id: config.id,
      position: config.position,
      type: config.type, // bush, box, closet, etc.
      capacity: config.capacity || 1,
      occupants: []
    });
  }
  
  hide(player, spotId) {
    const spot = this.hideSpots.find(s => s.id === spotId);
    if (!spot) return false;
    
    if (spot.occupants.length >= spot.capacity) return false;
    
    spot.occupants.push(player);
    player.hidingSpot = spot;
    player.visible = false;
    
    return true;
  }
  
  unhide(player) {
    if (!player.hidingSpot) return false;
    
    const spot = player.hidingSpot;
    spot.occupants = spot.occupants.filter(p => p !== player);
    player.hidingSpot = null;
    player.visible = true;
    
    return true;
  }
  
  isSpotOccupied(spotId) {
    const spot = this.hideSpots.find(s => s.id === spotId);
    return spot && spot.occupants.length > 0;
  }
}
```

### 3. 视野系统
```javascript
class VisionSystem {
  constructor() {
    this.seekerVisionRange = 300;
    this.hiderVisionRange = 200;
    this.fieldOfView = 90; // 度
  }
  
  canSee(seeker, target) {
    const distance = this.getDistance(seeker.position, target.position);
    
    // 距离检查
    if (distance > this.seekerVisionRange) return false;
    
    // 角度检查
    const angle = this.getAngle(seeker.position, seeker.facing, target.position);
    if (Math.abs(angle) > this.fieldOfView / 2) return false;
    
    // 遮挡检查
    if (this.isObstructed(seeker.position, target.position)) return false;
    
    // 躲藏检查
    if (target.hidingSpot && !this.canSeeThroughSpot(target.hidingSpot)) {
      return false;
    }
    
    return true;
  }
  
  isObstructed(from, to) {
    // 射线检测是否有障碍物
    const obstacles = this.getObstacles();
    
    for (const obstacle of obstacles) {
      if (this.lineIntersectsRect(from, to, obstacle)) {
        return true;
      }
    }
    
    return false;
  }
  
  canSeeThroughSpot(spot) {
    // 某些躲藏点可以看到
    return spot.type === 'bush'; // 灌木丛可以看到轮廓
  }
}
```

### 4. 追逐系统
```javascript
class ChaseSystem {
  constructor() {
    this.chaseSpeed = 1.5; // 追逐速度倍数
    this.escapeDistance = 500;
  }
  
  startChase(seeker, hider) {
    seeker.chasing = hider;
    hider.beingChased = true;
    
    // 加速
    seeker.speed *= this.chaseSpeed;
  }
  
  endChase(seeker, hider) {
    seeker.chasing = null;
    hider.beingChased = false;
    
    // 恢复速度
    seeker.speed /= this.chaseSpeed;
  }
  
  update(delta) {
    for (const seeker of this.seekers) {
      if (seeker.chasing) {
        const distance = this.getDistance(seeker.position, seeker.chasing.position);
        
        if (distance > this.escapeDistance) {
          this.endChase(seeker, seeker.chasing);
        } else if (distance < 30) {
          this.catchHider(seeker, seeker.chasing);
        }
      }
    }
  }
  
  catchHider(seeker, hider) {
    hider.caught = true;
    hider.visible = true;
    
    this.endChase(seeker, hider);
    
    // 显示捕获效果
    this.showCatchEffect(seeker, hider);
  }
}
```

### 5. 道具系统
```javascript
class PowerUpSystem {
  constructor() {
    this.powerups = [];
  }
  
  spawnPowerUp(config) {
    this.powerups.push({
      id: config.id,
      type: config.type,
      position: config.position,
      collected: false
    });
  }
  
  collectPowerUp(player, powerupId) {
    const powerup = this.powerups.find(p => p.id === powerupId);
    if (!powerup || powerup.collected) return false;
    
    powerup.collected = true;
    
    switch (powerup.type) {
      case 'speed':
        player.speed *= 1.5;
        setTimeout(() => player.speed /= 1.5, 5000);
        break;
      case 'invisible':
        player.visible = false;
        setTimeout(() => player.visible = true, 3000);
        break;
      case 'reveal':
        // 显示所有躲藏者位置
        this.revealAllHiders();
        break;
    }
    
    return true;
  }
}

const POWERUPS = {
  speed: { name: '加速', duration: 5000 },
  invisible: { name: '隐身', duration: 3000 },
  reveal: { name: '透视', duration: 0 }
};
```

## 迭代策略

### 第一版：基础躲猫猫
- 2 个阵营
- 简单地图
- 基础躲藏
- 基础 UI

### 第二版：视野系统
- 视野系统
- 遮挡检测
- 多个躲藏点
- 计时系统

### 第三版：道具系统
- 道具系统
- 多种道具
- 道具效果
- 道具刷新

### 第四版：深度玩法
- 多种地图
- 多种模式
- 成就系统
- 排行榜

### 第五版：多人对战
- 在线对战
- 房间系统
- 聊天系统
- 社区功能

## 常见错误

1. **阵营不平衡**：躲藏者和寻找者要平衡
2. **地图太小**：要有足够躲藏空间
3. **视野太好**：寻找者视野不能太好
4. **没有时间限制**：不能无限躲藏
5. **没有道具**：要有道具增加乐趣
