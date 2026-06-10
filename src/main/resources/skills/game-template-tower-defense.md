---
name: 塔防游戏开发模板
description: 塔防游戏开发模板，适用于策略防御类游戏
trigger: tower defense, 塔防, defense, TD, 策略防御
examples: Kingdom Rush|Bloons TD|植物大战僵尸|Arknights|Random Dice
---

# 塔防游戏开发模板

## 游戏设计核心原则

### 核心循环（每波 30-60 秒）
```
观察敌人路线 → 选择建塔位置 → 等待敌人 → 升级/调整 → 下一波
```
- 建塔必须有**即时反馈**（建塔动画 + 音效）
- 敌人被消灭时要有**爽感**（爆炸效果 + 金币飞出）
- 每波结束给玩家**喘息时间**（5-10 秒准备期）

### 玩家心理学
- **策略感**：玩家觉得"我的布局很聪明"
- **成长感**：塔从 Lv1 升到 Lv5 的视觉变化
- **紧张感**：敌人快到终点时的紧迫感
- **成就感**：完美通关（不漏一个敌人）的满足感

### 难度曲线设计
```
波次1-3:   教程波，只有普通敌人，教建塔和升级
波次4-10:  入门波，引入新敌人类型，教针对性建塔
波次11-20: 进阶波，敌人数量增加，需要优化布局
波次21-30: 挑战波，精英敌人+BOSS，需要策略配合
波次31+:   无尽模式，测试玩家极限
```

## 核心系统设计

### 1. 塔类型设计
| 塔类型 | 攻击方式 | 优势 | 劣势 | 建造费用 |
|--------|----------|------|------|----------|
| 箭塔 | 单体远程 | 攻速快 | 伤害低 | 100 |
| 炮塔 | 范围爆炸 | 伤害高 | 攻速慢 | 200 |
| 冰塔 | 减速 | 控制 | 无伤害 | 150 |
| 电塔 | 链式攻击 | 多目标 | 伤害递减 | 250 |
| 毒塔 | 持续伤害 | DOT | 单体 | 180 |

### 2. 敌人类型设计
| 敌人类型 | 特点 | 弱点 | 出现波次 |
|----------|------|------|----------|
| 普通兵 | 速度慢，血量低 | 无 | 第1波 |
| 快速兵 | 速度快，血量低 | 冰塔 | 第4波 |
| 重甲兵 | 速度慢，血量高 | 炮塔 | 第8波 |
| 飞行兵 | 无视地形 | 箭塔 | 第12波 |
| BOSS | 血量极高，有技能 | 集火 | 每10波 |

### 3. 升级系统
```
塔 Lv1 → Lv2: 伤害+30%, 费用 50% 建造费
塔 Lv2 → Lv3: 伤害+30%, 攻速+20%, 费用 75% 建造费
塔 Lv3 → Lv4: 伤害+30%, 攻速+20%, 范围+20%, 费用 100% 建造费
塔 Lv4 → Lv5: 解锁特殊技能, 费用 150% 建造费
```

### 4. 经济系统
- 击杀敌人获得金币
- 每波结束获得基础金币
- 卖塔返还 70% 费用
- 利息系统：每波结束时金币的 5% 作为利息（鼓励存钱）

## 关键技术实现

### 寻路系统（A* 算法）
```javascript
class Pathfinder {
  findPath(start, end, grid) {
    const openSet = [start];
    const cameFrom = {};
    const gScore = {};
    const fScore = {};
    
    gScore[this.key(start)] = 0;
    fScore[this.key(start)] = this.heuristic(start, end);
    
    while (openSet.length > 0) {
      // 找 fScore 最小的节点
      const current = openSet.reduce((a, b) => 
        (fScore[this.key(a)] || Infinity) < (fScore[this.key(b)] || Infinity) ? a : b
      );
      
      if (current.x === end.x && current.y === end.y) {
        return this.reconstructPath(cameFrom, current);
      }
      
      openSet.splice(openSet.indexOf(current), 1);
      
      // 检查四个方向的邻居
      for (const neighbor of this.getNeighbors(current, grid)) {
        const tentativeG = gScore[this.key(current)] + 1;
        
        if (tentativeG < (gScore[this.key(neighbor)] || Infinity)) {
          cameFrom[this.key(neighbor)] = current;
          gScore[this.key(neighbor)] = tentativeG;
          fScore[this.key(neighbor)] = tentativeG + this.heuristic(neighbor, end);
          
          if (!openSet.find(n => n.x === neighbor.x && n.y === neighbor.y)) {
            openSet.push(neighbor);
          }
        }
      }
    }
    
    return null; // 无路径
  }
  
  heuristic(a, b) {
    return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
  }
}
```

### 波次系统
```javascript
class WaveManager {
  constructor() {
    this.waves = [];
    this.currentWave = 0;
    this.enemiesAlive = 0;
  }
  
  addWave(waveConfig) {
    this.waves.push(waveConfig);
  }
  
  startNextWave() {
    if (this.currentWave >= this.waves.length) {
      this.onAllWavesComplete();
      return;
    }
    
    const wave = this.waves[this.currentWave];
    this.spawnEnemies(wave);
    this.currentWave++;
  }
  
  spawnEnemies(wave) {
    let delay = 0;
    
    for (const enemyGroup of wave.enemies) {
      for (let i = 0; i < enemyGroup.count; i++) {
        this.scene.time.delayedCall(delay, () => {
          const enemy = this.createEnemy(enemyGroup.type);
          this.scene.enemies.add(enemy);
          this.enemiesAlive++;
        });
        delay += enemyGroup.interval;
      }
    }
  }
  
  onEnemyKilled() {
    this.enemiesAlive--;
    if (this.enemiesAlive <= 0) {
      this.onWaveComplete();
    }
  }
}
```

## 关卡设计

### 地图类型
| 地图类型 | 特点 | 难度 |
|----------|------|------|
| 单路线 | 一条固定路线 | 简单 |
| 双路线 | 两条路线分流 | 中等 |
| 多路线 | 三条以上路线 | 困难 |
| 开放地图 | 敌人自由寻路 | 极难 |

### 星级评价
| 星级 | 条件 |
|------|------|
| ★ | 通关 |
| ★★ | 不漏超过 5 个敌人 |
| ★★★ | 完美通关（不漏敌人） |

## 迭代策略

### 第一版：最小可玩版本
- 1 种塔（箭塔）
- 1 种敌人（普通兵）
- 单路线地图
- 3 波敌人
- 基础建塔和攻击

### 第二版：核心玩法
- 添加 3 种塔（炮塔、冰塔、电塔）
- 添加 3 种敌人（快速兵、重甲兵、飞行兵）
- 添加升级系统
- 添加 10 波敌人

### 第三版：内容扩展
- 添加 BOSS 系统
- 添加 3 张地图
- 添加星级评价
- 添加道具系统

### 第四版：深度玩法
- 添加无尽模式
- 添加成就系统
- 添加排行榜
- 添加每日挑战

### 第五版：打磨
- 优化平衡性
- 添加特效和音效
- 优化 UI 体验
- 添加新手引导

## 常见错误

1. **寻路太简单**：敌人走直线没有策略感，用 A* 算法
2. **塔没有视觉反馈**：攻击时必须有弹道/特效
3. **经济太紧**：让玩家有钱建塔，否则会无聊
4. **敌人太弱**：没有挑战感，玩家会睡着
5. **没有 BOSS**：BOSS 是塔防游戏的高潮，必须有
