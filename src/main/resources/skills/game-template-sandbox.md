---
name: 沙盒游戏开发模板
description: 沙盒游戏开发模板，适用于Minecraft类开放世界沙盒游戏
trigger: 沙盒游戏,sandbox game,开放世界,建造游戏,生存游戏,我的世界
examples: Minecraft|泰拉瑞亚|方舟生存进化|饥荒|英灵神殿
---

# 沙盒游戏开发模板

## 游戏设计核心原则

### 核心循环（持续进行）
```
探索 → 收集资源 → 制作工具 → 建造基地 → 发现新区域 → 更好的资源
```
- **自由度**：玩家可以做任何想做的事
- **创造力**：建造任何想象的东西
- **探索欲**：发现新地形、新生物

### 玩家心理学
- **"创造世界"的满足感**：建造自己的世界
- **"探索未知"的好奇心**：发现新地形、新资源
- **"从无到有"的成就感**：从零开始建造
- **"分享作品"的欲望**：展示自己的建造

### 沙盒设计要点
```
沙盒核心：
1. 自由建造：任何地方都能建造
2. 丰富资源：多种资源收集
3. 探索世界：大型开放世界
4. 生存挑战：饥饿、怪物、环境
```

## 核心系统设计

### 1. 方块系统
```javascript
class Block {
  constructor(config) {
    this.id = config.id;
    this.type = config.type;
    this.name = config.name;
    this.hardness = config.hardness;
    this.transparent = config.transparent || false;
    this.textures = config.textures; // 6 个面的纹理
  }
  
  canBreak(tool) {
    return true;
  }
  
  getBreakTime(tool) {
    if (tool && tool.canBreak(this.type)) {
      return this.hardness / tool.speed;
    }
    return this.hardness * 1.5;
  }
}

const BLOCKS = {
  dirt: { name: '泥土', hardness: 0.5, type: 'natural' },
  stone: { name: '石头', hardness: 1.5, type: 'natural' },
  wood: { name: '木头', hardness: 2, type: 'natural' },
  iron: { name: '铁矿', hardness: 3, type: 'ore' },
  diamond: { name: '钻石', hardness: 5, type: 'ore' },
  grass: { name: '草方块', hardness: 0.6, type: 'natural' }
};
```

### 2. 世界生成系统
```javascript
class WorldGenerator {
  constructor(seed) {
    this.seed = seed;
    this.noise = new SimplexNoise(seed);
  }
  
  generateChunk(chunkX, chunkZ) {
    const chunk = new Chunk(chunkX, chunkZ);
    
    for (let x = 0; x < 16; x++) {
      for (let z = 0; z < 16; z++) {
        const worldX = chunkX * 16 + x;
        const worldZ = chunkZ * 16 + z;
        
        // 使用噪声生成地形高度
        const height = this.getHeight(worldX, worldZ);
        
        // 生成地形
        for (let y = 0; y < height; y++) {
          if (y === 0) {
            chunk.setBlock(x, y, z, 'bedrock');
          } else if (y < height - 4) {
            chunk.setBlock(x, y, z, 'stone');
          } else if (y < height) {
            chunk.setBlock(x, y, z, 'dirt');
          } else {
            chunk.setBlock(x, y, z, 'grass');
          }
        }
        
        // 生成树木
        if (Math.random() < 0.02 && height > 50) {
          this.generateTree(chunk, x, height, z);
        }
      }
    }
    
    return chunk;
  }
  
  getHeight(x, z) {
    const scale = 0.01;
    const height = this.noise.noise2D(x * scale, z * scale);
    return Math.floor(50 + height * 30);
  }
}
```

### 3. 物品系统
```javascript
class Item {
  constructor(config) {
    this.id = config.id;
    this.name = config.name;
    this.type = config.type; // block, tool, weapon, food
    this.maxStackSize = config.maxStackSize || 64;
    this.durability = config.durability;
    this.damage = config.damage;
    this.speed = config.speed;
  }
}

const ITEMS = {
  woodenPickaxe: { name: '木镐', type: 'tool', durability: 60, speed: 2, canBreak: ['stone', 'ore'] },
  stonePickaxe: { name: '石镐', type: 'tool', durability: 132, speed: 4, canBreak: ['stone', 'ore', 'iron'] },
  ironPickaxe: { name: '铁镐', type: 'tool', durability: 251, speed: 6, canBreak: ['stone', 'ore', 'iron', 'diamond'] },
  diamondPickaxe: { name: '钻石镐', type: 'tool', durability: 1562, speed: 8, canBreak: ['stone', 'ore', 'iron', 'diamond', 'obsidian'] },
  woodenSword: { name: '木剑', type: 'weapon', damage: 4 },
  stoneSword: { name: '石剑', type: 'weapon', damage: 5 },
  ironSword: { name: '铁剑', type: 'weapon', damage: 6 },
  diamondSword: { name: '钻石剑', type: 'weapon', damage: 7 }
};
```

### 4. 合成系统
```javascript
class CraftingSystem {
  constructor() {
    this.recipes = {};
  }
  
  addRecipe(recipe) {
    this.recipes[recipe.id] = recipe;
  }
  
  canCraft(recipeId, inventory) {
    const recipe = this.recipes[recipeId];
    if (!recipe) return false;
    
    for (const [itemId, amount] of Object.entries(recipe.ingredients)) {
      if (inventory.getItemCount(itemId) < amount) return false;
    }
    
    return true;
  }
  
  craft(recipeId, inventory) {
    if (!this.canCraft(recipeId, inventory)) return false;
    
    const recipe = this.recipes[recipeId];
    
    // 消耗材料
    for (const [itemId, amount] of Object.entries(recipe.ingredients)) {
      inventory.removeItem(itemId, amount);
    }
    
    // 获得物品
    inventory.addItem(recipe.result, recipe.resultAmount || 1);
    
    return true;
  }
}

const RECIPES = [
  { id: 'woodenPickaxe', ingredients: { wood: 3, stick: 2 }, result: 'woodenPickaxe' },
  { id: 'stonePickaxe', ingredients: { stone: 3, stick: 2 }, result: 'stonePickaxe' },
  { id: 'ironPickaxe', ingredients: { iron: 3, stick: 2 }, result: 'ironPickaxe' },
  { id: 'diamondPickaxe', ingredients: { diamond: 3, stick: 2 }, result: 'diamondPickaxe' }
];
```

### 5. 生物系统
```javascript
class Mob {
  constructor(config) {
    this.id = config.id;
    this.type = config.type; // passive, hostile, neutral
    this.hp = config.hp;
    this.attack = config.attack;
    this.speed = config.speed;
    this.drops = config.drops;
    this.ai = config.ai;
  }
  
  update(delta) {
    this.ai.update(this, delta);
  }
  
  die() {
    // 掉落物品
    for (const drop of this.drops) {
      if (Math.random() < drop.chance) {
        this.world.dropItem(this.position, drop.item, drop.amount);
      }
    }
  }
}

const MOBS = {
  zombie: { type: 'hostile', hp: 20, attack: 3, speed: 0.23, drops: [{ item: 'rottenFlesh', chance: 1 }] },
  skeleton: { type: 'hostile', hp: 20, attack: 4, speed: 0.25, drops: [{ item: 'bone', chance: 1 }] },
  cow: { type: 'passive', hp: 10, attack: 0, speed: 0.2, drops: [{ item: 'beef', chance: 1 }] },
  pig: { type: 'passive', hp: 10, attack: 0, speed: 0.25, drops: [{ item: 'pork', chance: 1 }] }
};
```

## 迭代策略

### 第一版：基础世界
- 方块放置/破坏
- 基础世界生成
- 简单物品
- 基础 UI

### 第二版：生存系统
- 饥饿系统
- 生命系统
- 敌对生物
- 战斗系统

### 第三版：合成系统
- 合成台
- 多种配方
- 工具系统
- 装备系统

### 第四版：深度玩法
- 多种生物群落
- 洞穴系统
- 矿石系统
- 附魔系统

### 第五版：多人联机
- 多人合作
- 服务器系统
- 创造模式
- 社区分享

## 常见错误

1. **世界太小**：开放世界要足够大
2. **资源太少**：要让玩家有东西收集
3. **合成太复杂**：合成要简单直观
4. **没有教程**：新手要能学会玩
5. **性能问题**：大型世界要优化性能
