---
name: 动作RPG游戏开发模板
description: 动作RPG游戏开发模板，适用于ARPG、暗黑类、动作角色扮演类游戏
trigger: 动作RPG,ARPG,暗黑破坏神,动作角色扮演,刷装备游戏
examples: 暗黑破坏神|流放之路|恐怖黎明|火炬之光|原神|Diablo
---

# 动作RPG游戏开发模板

## 游戏设计核心原则

### 核心循环（每 5-15 分钟一轮）
```
探索地图 → 击杀怪物 → 获得装备 → 升级角色 → 挑战更强怪物
```
- **爽快战斗**：击杀怪物的爽感
- **装备驱动**：刷装备是核心乐趣
- **成长感**：从弱到强的渐进

### 玩家心理学
- **"刷装备"的快感**：掉落稀有装备的惊喜
- **"Build 构建"的乐趣**：搭配装备和技能的策略
- **"一打多"的爽感**：击杀大量怪物的满足感
- **"挑战极限"的欲望**：挑战更高难度

### 战斗设计要点
```
动作RPG战斗核心：
1. 操作简单：点击移动，点击攻击
2. 反馈强烈：击杀有爆炸效果、掉落物品
3. 技能多样：多种技能组合
4. 难度渐进：从简单到地狱难度
```

## 核心系统设计

### 1. 角色属性系统
```javascript
class Character {
  constructor(config) {
    this.level = 1;
    this.exp = 0;
    this.stats = {
      strength: config.strength || 10,
      dexterity: config.dexterity || 10,
      intelligence: config.intelligence || 10,
      vitality: config.vitality || 10
    };
    this.derived = this.calculateDerived();
  }
  
  calculateDerived() {
    return {
      maxHp: this.stats.vitality * 10,
      maxMp: this.stats.intelligence * 5,
      attack: this.stats.strength * 2,
      defense: this.stats.dexterity * 1,
      speed: this.stats.dexterity * 0.5
    };
  }
  
  gainExp(amount) {
    this.exp += amount;
    while (this.exp >= this.getExpToNextLevel()) {
      this.levelUp();
    }
  }
  
  levelUp() {
    this.level++;
    this.exp -= this.getExpToNextLevel();
    
    // 分配属性点
    this.showAttributePointDialog();
  }
}
```

### 2. 装备系统
```javascript
class EquipmentSystem {
  constructor() {
    this.slots = {
      weapon: null,
      armor: null,
      helmet: null,
      boots: null,
      gloves: null,
      ring1: null,
      ring2: null,
      amulet: null
    };
  }
  
  equip(item) {
    const slot = item.slot;
    const oldItem = this.slots[slot];
    
    if (oldItem) {
      this.unequip(slot);
    }
    
    this.slots[slot] = item;
    this.applyStats(item);
  }
  
  unequip(slot) {
    const item = this.slots[slot];
    if (item) {
      this.removeStats(item);
      this.slots[slot] = null;
    }
  }
  
  applyStats(item) {
    for (const [stat, value] of Object.entries(item.stats)) {
      this.character.stats[stat] += value;
    }
    this.character.recalculate();
  }
}

const ITEM_RARITIES = {
  common: { name: '普通', color: '#ffffff', statMultiplier: 1 },
  uncommon: { name: '优秀', color: '#00ff00', statMultiplier: 1.2 },
  rare: { name: '稀有', color: '#0088ff', statMultiplier: 1.5 },
  epic: { name: '史诗', color: '#cc00ff', statMultiplier: 2 },
  legendary: { name: '传说', color: '#ff8800', statMultiplier: 3 }
};
```

### 3. 技能系统
```javascript
class SkillSystem {
  constructor() {
    this.skills = [];
    this.skillPoints = 0;
  }
  
  learnSkill(skillId) {
    const skill = SKILLS[skillId];
    if (!skill) return false;
    
    if (this.skillPoints < skill.cost) return false;
    
    this.skills.push({ ...skill, level: 1 });
    this.skillPoints -= skill.cost;
    
    return true;
  }
  
  upgradeSkill(skillId) {
    const skill = this.skills.find(s => s.id === skillId);
    if (!skill) return false;
    
    if (this.skillPoints < 1) return false;
    if (skill.level >= skill.maxLevel) return false;
    
    skill.level++;
    this.skillPoints--;
    
    return true;
  }
  
  useSkill(skillId, target) {
    const skill = this.skills.find(s => s.id === skillId);
    if (!skill) return false;
    
    if (this.character.mp < skill.mpCost) return false;
    
    this.character.mp -= skill.mpCost;
    return skill.execute(this.character, target, skill.level);
  }
}

const SKILLS = {
  whirlwind: {
    name: '旋风斩',
    description: '对周围敌人造成伤害',
    mpCost: 20,
    maxLevel: 10,
    execute: (user, target, level) => {
      const damage = user.attack * (1 + level * 0.2);
      // 对周围所有敌人造成伤害
      return { damage, aoe: true };
    }
  },
  fireball: {
    name: '火球术',
    description: '发射火球造成范围伤害',
    mpCost: 30,
    maxLevel: 10,
    execute: (user, target, level) => {
      const damage = user.intelligence * (2 + level * 0.3);
      return { damage, aoe: true, element: 'fire' };
    }
  }
};
```

### 4. 掉落系统
```javascript
class LootSystem {
  constructor() {
    this.lootTables = {};
  }
  
  generateLoot(monster) {
    const loot = [];
    const table = this.lootTables[monster.type];
    
    if (!table) return loot;
    
    for (const drop of table.drops) {
      if (Math.random() < drop.chance) {
        const item = this.generateItem(drop.itemLevel, drop.rarity);
        loot.push(item);
      }
    }
    
    return loot;
  }
  
  generateItem(level, rarity) {
    const baseItem = this.getRandomBaseItem(level);
    const rarityConfig = ITEM_RARITIES[rarity];
    
    return {
      ...baseItem,
      rarity,
      stats: this.generateStats(baseItem, level, rarityConfig.statMultiplier)
    };
  }
  
  generateStats(baseItem, level, multiplier) {
    const stats = {};
    for (const [stat, range] of Object.entries(baseItem.possibleStats)) {
      const base = range.min + Math.random() * (range.max - range.min);
      stats[stat] = Math.floor(base * level * multiplier);
    }
    return stats;
  }
}
```

## 迭代策略

### 第一版：基础战斗
- 基础移动和攻击
- 1 种怪物
- 简单掉落
- 基础 UI

### 第二版：装备系统
- 5 种装备
- 装备穿戴
- 属性加成
- 掉落系统

### 第三版：技能系统
- 5 种技能
- 技能升级
- 技能组合
- MP 系统

### 第四版：深度玩法
- 多种怪物
- BOSS 战
- 多难度
- 成就系统

### 第五版：多人联机
- 多人合作
- 交易系统
- 排行榜
- 赛季系统

## 常见错误

1. **战斗太无聊**：要有爽快感，不能只是数值对比
2. **装备没有差异**：每件装备要有独特属性
3. **掉落太少**：要让玩家频繁获得装备
4. **技能没有组合**：技能之间要有协同效应
5. **难度太陡**：难度要渐进，不能突然变难
