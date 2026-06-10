---
name: 肉鸽游戏开发模板
description: 肉鸽游戏开发模板，适用于Roguelike、Roguelite、地牢探索类游戏
trigger: roguelike, 肉鸽, roguelite, 随机, 地牢, dungeon, 永久死亡, permadeath, Slay the Spire
examples: Slay the Spire|Hades|Dead Cells|Enter the Gungeon|The Binding of Isaac
---

# 肉鸽游戏开发模板

## 游戏设计核心原则

### 核心循环（每局 20-60 分钟）
```
进入地牢 → 探索房间 → 战斗 → 获得奖励 → 下一层 → 死亡 → 永久解锁 → 再来一局
```
- **随机性**：每局都不一样，地图、敌人、道具都是随机的
- **永久死亡**：死亡后重新开始，但保留永久解锁内容
- **Build 构建**：通过道具组合创造独特玩法

### 玩家心理学
- **"这次运气好"**：随机道具让玩家觉得"这局很强"
- **"差一点就过"**：死亡后觉得"下次能行"
- **"发现了新组合"**：道具组合产生意外效果的惊喜感
- **"永久成长"**：死亡不是白费，解锁了新内容

### 随机性设计要点
```
随机性要"可控的随机"：
1. 敌人随机，但难度曲线固定
2. 道具随机，但保证每局都有足够的道具
3. 地图随机，但保证路径可达
4. BOSS 固定，但技能组合随机
```

## 核心系统设计

### 1. 随机地图生成
```javascript
class DungeonGenerator {
  constructor(width, height, numRooms) {
    this.width = width;
    this.height = height;
    this.numRooms = numRooms;
    this.rooms = [];
    this.corridors = [];
  }
  
  generate() {
    // 1. 随机生成房间
    for (let i = 0; i < this.numRooms; i++) {
      const room = this.createRandomRoom();
      if (!this.overlapsExisting(room)) {
        this.rooms.push(room);
      }
    }
    
    // 2. 连接房间（最小生成树）
    this.connectRooms();
    
    // 3. 放置特殊房间
    this.placeSpecialRooms();
    
    // 4. 生成地图数据
    return this.buildMap();
  }
  
  createRandomRoom() {
    const width = Phaser.Math.Between(4, 8);
    const height = Phaser.Math.Between(4, 8);
    const x = Phaser.Math.Between(1, this.width - width - 1);
    const y = Phaser.Math.Between(1, this.height - height - 1);
    return { x, y, width, height, center: { x: x + width/2, y: y + height/2 } };
  }
  
  connectRooms() {
    // 使用最小生成树连接所有房间
    const edges = [];
    for (let i = 0; i < this.rooms.length; i++) {
      for (let j = i + 1; j < this.rooms.length; j++) {
        const dist = Phaser.Math.Distance.BetweenPoints(
          this.rooms[i].center, this.rooms[j].center
        );
        edges.push({ from: i, to: j, weight: dist });
      }
    }
    edges.sort((a, b) => a.weight - b.weight);
    
    // Kruskal 算法
    const parent = this.rooms.map((_, i) => i);
    for (const edge of edges) {
      if (this.find(parent, edge.from) !== this.find(parent, edge.to)) {
        this.union(parent, edge.from, edge.to);
        this.corridors.push(edge);
      }
    }
  }
  
  placeSpecialRooms() {
    // 起始房间
    this.rooms[0].type = 'start';
    // BOSS 房间（最远的房间）
    const farthest = this.findFarthestRoom(0);
    this.rooms[farthest].type = 'boss';
    // 商店（随机位置）
    const shop = Phaser.Math.Between(1, this.rooms.length - 2);
    this.rooms[shop].type = 'shop';
    // 宝箱房
    const treasure = Phaser.Math.Between(1, this.rooms.length - 2);
    this.rooms[treasure].type = 'treasure';
  }
}
```

### 2. 道具系统（核心乐趣）
```javascript
const ITEMS = {
  // 攻击类
  fireSword: { name: '烈焰剑', type: 'attack', damage: 1.5, effect: 'burn' },
  iceStaff: { name: '冰霜法杖', type: 'attack', damage: 1.2, effect: 'slow' },
  
  // 防御类
  ironShield: { name: '铁盾', type: 'defense', armor: 20, effect: 'block' },
  healingPotion: { name: '治疗药水', type: 'defense', heal: 50 },
  
  // 辅助类
  speedBoots: { name: '疾风靴', type: 'utility', speed: 1.3 },
  magnetGloves: { name: '磁力手套', type: 'utility', pickupRange: 2 },
  
  // 组合类（需要特定道具组合）
  fireShield: { 
    name: '烈焰盾', 
    type: 'combo', 
    requires: ['ironShield', 'fireSword'],
    effect: 'burnEnemiesOnBlock' 
  }
};

class ItemSystem {
  constructor() {
    this.items = [];
    this.comboEffects = [];
  }
  
  addItem(item) {
    this.items.push(item);
    this.checkCombos();
  }
  
  checkCombos() {
    for (const combo of Object.values(ITEMS)) {
      if (combo.requires && this.hasAllItems(combo.requires)) {
        this.activateCombo(combo);
      }
    }
  }
  
  getDamageMultiplier() {
    return this.items.reduce((mult, item) => {
      return mult * (item.damage || 1);
    }, 1);
  }
}
```

### 3. 永久死亡与永久解锁
```javascript
class ProgressionSystem {
  constructor() {
    this.unlockedCharacters = ['warrior'];
    this.unlockedItems = [];
    this.achievements = [];
    this.totalRuns = 0;
    this.bestFloor = 0;
  }
  
  onDeath(floor, score) {
    this.totalRuns++;
    this.bestFloor = Math.max(this.bestFloor, floor);
    
    // 检查解锁条件
    if (floor >= 5 && !this.unlockedCharacters.includes('mage')) {
      this.unlockCharacter('mage');
    }
    if (score >= 1000 && !this.unlockedItems.includes('fireSword')) {
      this.unlockItem('fireSword');
    }
    
    // 保存进度
    this.save();
  }
  
  unlockCharacter(name) {
    this.unlockedCharacters.push(name);
    this.showUnlockNotification(`解锁新角色: ${name}`);
  }
  
  unlockItem(name) {
    this.unlockedItems.push(name);
    this.showUnlockNotification(`解锁新道具: ${ITEMS[name].name}`);
  }
}
```

## 迭代策略

### 第一版：核心循环
- 随机地图生成
- 玩家移动和攻击
- 1 种敌人
- 1 层地牢
- 永久死亡

### 第二版：战斗系统
- 添加 3 种武器
- 添加 3 种敌人
- 添加 BOSS
- 添加 5 层地牢

### 第三版：道具系统
- 添加 20 种道具
- 道具效果叠加
- 道具组合系统
- 商店系统

### 第四版：永久成长
- 永久解锁系统
- 多角色系统
- 成就系统
- 统计系统

### 第五版：打磨
- 平衡道具效果
- 优化地图生成
- 添加音效和音乐
- 添加剧情

## 常见错误

1. **随机性太强**：完全随机会让玩家觉得不公平，需要"可控的随机"
2. **死亡太惩罚**：永久死亡要有"下次能更好"的感觉，不能白费
3. **道具没有差异**：每个道具必须有独特效果，不能只是数值差异
4. **地图太单调**：随机地图要有变化，不能都是同样的房间
5. **BOSS 太难**：BOSS 是高潮，但不能难到让人放弃
