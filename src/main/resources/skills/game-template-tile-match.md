---
name: 堆叠三消游戏开发模板
description: 堆叠三消游戏开发模板，适用于羊了个羊、麻将消除类游戏
trigger: tile, 三消, 堆叠, match, 羊了个羊, mahjong, 麻将, 消除, puzzle, 益智
examples: 羊了个羊|Mahjong|麻将消除|Tile Match|3tiles
---

# 堆叠三消游戏开发模板

## 游戏设计核心原则

### 核心循环（每局 5-15 分钟）
```
观察牌面 → 选择牌面 → 放入暂存槽 → 三消消除 → 清空牌面
```
- **策略性**：选择哪张牌很重要
- **紧张感**：暂存槽快满的紧张感
- **成就感**：消除的爽感

### 玩家心理学
- **"差一点就成功"**：总是差一点，让人重试
- **"全屏消除"的爽感**：一次消除大量牌面
- **"策略思考"的乐趣**：思考最优选择
- **"挑战极限"的欲望**：挑战更高难度

### 堆叠三消设计要点
```
堆叠三消核心：
1. 多层堆叠：牌面堆叠在多层
2. 暂存槽：最多 7 张牌暂存
3. 三消消除：3 张相同牌面消除
4. 道具系统：洗牌、撤回、移出
```

## 核心系统设计

### 1. 牌面系统
```javascript
class Tile {
  constructor(config) {
    this.id = config.id;
    this.type = config.type; // 图案类型
    this.layer = config.layer; // 所在层
    this.position = config.position; // 位置
    this.covered = false; // 是否被覆盖
    this.selected = false;
  }
  
  isClickable() {
    // 只有没有被覆盖的牌才能点击
    return !this.covered;
  }
  
  select() {
    if (!this.isClickable()) return false;
    this.selected = true;
    return true;
  }
}

const TILE_TYPES = [
  { id: 'bamboo', name: '竹子', icon: '🎋' },
  { id: 'flower', name: '花', icon: '🌸' },
  { id: 'bird', name: '鸟', icon: '🐦' },
  { id: 'fish', name: '鱼', icon: '🐟' },
  { id: 'moon', name: '月亮', icon: '🌙' },
  { id: 'sun', name: '太阳', icon: '☀️' },
  { id: 'star', name: '星星', icon: '⭐' },
  { id: 'cloud', name: '云', icon: '☁️' }
];
```

### 2. 棋盘系统
```javascript
class Board {
  constructor(config) {
    this.width = config.width;
    this.height = config.height;
    this.layers = config.layers;
    this.tiles = [];
    
    this.generateTiles();
    this.calculateCoverage();
  }
  
  generateTiles() {
    // 生成牌面（每种 3 张，保证能全部消除）
    const tilesPerType = 3;
    const totalTiles = TILE_TYPES.length * tilesPerType;
    
    for (const type of TILE_TYPES) {
      for (let i = 0; i < tilesPerType; i++) {
        this.tiles.push(new Tile({
          id: `${type.id}_${i}`,
          type: type.id,
          layer: this.getRandomLayer(),
          position: this.getRandomPosition()
        }));
      }
    }
  }
  
  calculateCoverage() {
    // 计算每张牌是否被覆盖
    for (const tile of this.tiles) {
      tile.covered = this.isCovered(tile);
    }
  }
  
  isCovered(tile) {
    // 检查是否有更高层的牌覆盖此牌
    for (const other of this.tiles) {
      if (other === tile) continue;
      if (other.layer <= tile.layer) continue;
      
      if (this.overlaps(tile.position, other.position)) {
        return true;
      }
    }
    
    return false;
  }
  
  overlaps(pos1, pos2) {
    const size = 40; // 牌面大小
    return Math.abs(pos1.x - pos2.x) < size && Math.abs(pos1.y - pos2.y) < size;
  }
  
  removeTile(tile) {
    this.tiles = this.tiles.filter(t => t !== tile);
    this.calculateCoverage();
  }
}
```

### 3. 暂存槽系统
```javascript
class SlotSystem {
  constructor(maxSize = 7) {
    this.slots = [];
    this.maxSize = maxSize;
  }
  
  addTile(tile) {
    if (this.slots.length >= this.maxSize) {
      return false; // 暂存槽已满
    }
    
    // 检查是否能三消
    const sameType = this.slots.filter(t => t.type === tile.type);
    
    if (sameType.length >= 2) {
      // 三消！
      this.removeSameType(tile.type);
      return 'match';
    }
    
    this.slots.push(tile);
    return 'added';
  }
  
  removeSameType(type) {
    this.slots = this.slots.filter(t => t.type !== type);
  }
  
  isFull() {
    return this.slots.length >= this.maxSize;
  }
  
  isEmpty() {
    return this.slots.length === 0;
  }
  
  checkMatch() {
    // 检查是否有三消
    const typeCounts = {};
    for (const tile of this.slots) {
      typeCounts[tile.type] = (typeCounts[tile.type] || 0) + 1;
    }
    
    for (const [type, count] of Object.entries(typeCounts)) {
      if (count >= 3) {
        return type;
      }
    }
    
    return null;
  }
}
```

### 4. 道具系统
```javascript
class PowerUpSystem {
  constructor() {
    this.powerups = {
      shuffle: { name: '洗牌', count: 3 },
      undo: { name: '撤回', count: 3 },
      remove: { name: '移出', count: 3 }
    };
  }
  
  useShuffle(board) {
    if (this.powerups.shuffle.count <= 0) return false;
    
    // 随机打乱牌面位置
    const positions = board.tiles.map(t => t.position);
    for (let i = positions.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [positions[i], positions[j]] = [positions[j], positions[i]];
    }
    
    board.tiles.forEach((tile, i) => {
      tile.position = positions[i];
    });
    
    board.calculateCoverage();
    this.powerups.shuffle.count--;
    
    return true;
  }
  
  useUndo(slotSystem, history) {
    if (this.powerups.undo.count <= 0) return false;
    if (history.length === 0) return false;
    
    const lastAction = history.pop();
    slotSystem.slots.pop();
    
    this.powerups.undo.count--;
    
    return true;
  }
  
  useRemove(slotSystem, tileType) {
    if (this.powerups.remove.count <= 0) return false;
    
    slotSystem.slots = slotSystem.slots.filter(t => t.type !== tileType);
    this.powerups.remove.count--;
    
    return true;
  }
}
```

### 5. 关卡系统
```javascript
class LevelSystem {
  constructor() {
    this.currentLevel = 1;
    this.levels = LEVELS;
  }
  
  getLevelConfig() {
    return this.levels[this.currentLevel - 1];
  }
  
  checkWin(board, slotSystem) {
    return board.tiles.length === 0 && slotSystem.isEmpty();
  }
  
  checkLose(slotSystem) {
    return slotSystem.isFull();
  }
}

const LEVELS = [
  { level: 1, tileTypes: 4, tilesPerType: 3, layers: 1, name: '入门' },
  { level: 2, tileTypes: 6, tilesPerType: 3, layers: 2, name: '简单' },
  { level: 3, tileTypes: 8, tilesPerType: 3, layers: 3, name: '普通' },
  { level: 4, tileTypes: 10, tilesPerType: 3, layers: 4, name: '困难' },
  { level: 5, tileTypes: 12, tilesPerType: 3, layers: 5, name: '地狱' }
];
```

## 迭代策略

### 第一版：基础消除
- 单层牌面
- 三消消除
- 暂存槽
- 简单 UI

### 第二版：多层堆叠
- 多层牌面
- 覆盖判定
- 关卡系统
- 计分系统

### 第三版：道具系统
- 洗牌道具
- 撤回道具
- 移出道具
- 道具商店

### 第四版：深度玩法
- 20 个关卡
- 特殊牌面
- 成就系统
- 排行榜

### 第五版：变现
- 看广告获得道具
- 内购系统
- 社交分享
- 推送通知

## 常见错误

1. **牌面太多**：牌面太多会看不清
2. **暂存槽太小**：暂存槽要够用
3. **没有道具**：卡住时要有道具帮助
4. **难度太陡**：难度要渐进
5. **没有反馈**：消除要有视觉和音效反馈
