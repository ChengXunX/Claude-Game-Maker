---
name: 回合制RPG游戏开发模板
description: 回合制RPG游戏开发模板，适用于回合制战斗、策略RPG类游戏
trigger: 回合制RPG,回合制游戏,turn-based RPG,策略RPG,日式RPG
examples: 最终幻想|勇者斗恶龙|女神异闻录|八方旅人|宝可梦|Pokémon
---

# 回合制RPG游戏开发模板

## 游戏设计核心原则

### 核心循环（每场战斗 1-5 分钟）
```
遭遇敌人 → 选择行动 → 执行行动 → 敌人行动 → 判定结果 → 获得奖励
```
- **策略深度**：每个行动都有意义
- **成长感**：角色变强的过程
- **收集乐趣**：收集技能、装备、怪物

### 玩家心理学
- **"策略决策"的满足感**：选择最优行动的成就感
- **"角色成长"的期待感**：升级、学新技能的期待
- **"收集满足"的快感**：收集全图鉴的成就感
- **"BOSS 战"的紧张感**：挑战强敌的刺激感

### 战斗系统设计要点
```
回合制战斗核心：
1. 速度决定行动顺序
2. 每个行动有明确效果
3. 弱点系统增加策略性
4. 状态效果增加深度
5. 战斗不能太长（普通战 30 秒，BOSS 战 2-3 分钟）
```

## 核心系统设计

### 1. 战斗系统
```javascript
class BattleSystem {
  constructor() {
    this.allies = [];
    this.enemies = [];
    this.turnOrder = [];
    this.currentTurnIndex = 0;
  }
  
  startBattle(allies, enemies) {
    this.allies = allies;
    this.enemies = enemies;
    this.calculateTurnOrder();
    this.nextTurn();
  }
  
  calculateTurnOrder() {
    const allUnits = [...this.allies, ...this.enemies];
    this.turnOrder = allUnits.sort((a, b) => b.speed - a.speed);
    this.currentTurnIndex = 0;
  }
  
  nextTurn() {
    if (this.checkBattleEnd()) {
      this.endBattle();
      return;
    }
    
    const current = this.turnOrder[this.currentTurnIndex];
    
    if (!current.isAlive()) {
      this.currentTurnIndex = (this.currentTurnIndex + 1) % this.turnOrder.length;
      this.nextTurn();
      return;
    }
    
    if (this.allies.includes(current)) {
      this.showPlayerMenu(current);
    } else {
      this.executeEnemyAI(current);
    }
  }
  
  executeAction(action, user, target) {
    switch (action.type) {
      case 'attack':
        this.attack(user, target);
        break;
      case 'skill':
        this.useSkill(user, action.skill, target);
        break;
      case 'item':
        this.useItem(user, action.item, target);
        break;
      case 'defend':
        this.defend(user);
        break;
    }
    
    this.endTurn();
  }
  
  attack(attacker, defender) {
    const damage = this.calculateDamage(attacker, defender);
    defender.hp -= damage;
    this.showDamageEffect(defender, damage);
    
    if (defender.hp <= 0) {
      this.handleDeath(defender);
    }
  }
  
  calculateDamage(attacker, defender) {
    const baseDamage = attacker.attack - defender.defense / 2;
    const randomFactor = 0.9 + Math.random() * 0.2;
    return Math.max(1, Math.floor(baseDamage * randomFactor));
  }
}
```

### 2. 角色系统
```javascript
class Character {
  constructor(config) {
    this.name = config.name;
    this.level = 1;
    this.exp = 0;
    this.stats = {
      hp: config.hp,
      maxHp: config.hp,
      mp: config.mp,
      maxMp: config.mp,
      attack: config.attack,
      defense: config.defense,
      speed: config.speed,
      magic: config.magic
    };
    this.skills = [];
    this.equipment = {};
    this.statusEffects = [];
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
    
    // 提升属性
    this.stats.maxHp += 10;
    this.stats.hp = this.stats.maxHp;
    this.stats.maxMp += 5;
    this.stats.mp = this.stats.maxMp;
    this.stats.attack += 2;
    this.stats.defense += 1;
    this.stats.speed += 1;
    this.stats.magic += 2;
    
    // 学习新技能
    this.checkNewSkills();
  }
  
  useSkill(skillIndex, target) {
    const skill = this.skills[skillIndex];
    if (!skill) return false;
    
    if (this.stats.mp < skill.mpCost) return false;
    
    this.stats.mp -= skill.mpCost;
    return skill.execute(this, target);
  }
}
```

### 3. 技能系统
```javascript
const SKILLS = {
  fireball: {
    name: '火球术',
    mpCost: 10,
    type: 'magic',
    target: 'enemy',
    power: 50,
    element: 'fire',
    execute: (user, target) => {
      const damage = user.stats.magic * 2 + 50;
      target.takeDamage(damage);
      return { damage };
    }
  },
  heal: {
    name: '治疗',
    mpCost: 8,
    type: 'magic',
    target: 'ally',
    execute: (user, target) => {
      const heal = user.stats.magic * 1.5;
      target.heal(heal);
      return { heal };
    }
  },
  powerUp: {
    name: '力量增强',
    mpCost: 5,
    type: 'buff',
    target: 'self',
    execute: (user, target) => {
      user.addStatusEffect({ type: 'attack', value: 20, duration: 3 });
      return { buff: 'attack' };
    }
  }
};
```

### 4. 弱点系统
```javascript
const WEAKNESSES = {
  fire: { weak: 'ice', strong: 'water' },
  ice: { weak: 'fire', strong: 'earth' },
  water: { weak: 'lightning', strong: 'fire' },
  lightning: { weak: 'water', strong: 'ice' },
  earth: { weak: 'water', strong: 'lightning' }
};

function getWeaknessMultiplier(attackElement, defenderElement) {
  const weakness = WEAKNESSES[defenderElement];
  if (!weakness) return 1;
  
  if (attackElement === weakness.weak) return 2;
  if (attackElement === weakness.strong) return 0.5;
  return 1;
}
```

## 迭代策略

### 第一版：基础战斗
- 基础攻击
- 1 种敌人
- 简单 UI
- 经验值系统

### 第二版：技能系统
- 3 种技能
- MP 系统
- 多种敌人
- 升级系统

### 第三版：装备系统
- 装备穿戴
- 属性加成
- 多种装备
- 背包系统

### 第四版：深度玩法
- 弱点系统
- 状态效果
- BOSS 战
- 剧情系统

### 第五版：收集系统
- 怪物图鉴
- 技能学习
- 成就系统
- 排行榜

## 常见错误

1. **战斗太慢**：回合制战斗要有速度感
2. **没有策略性**：不能无脑攻击，要有弱点系统
3. **升级太慢**：要让玩家频繁升级
4. **技能没有差异**：每个技能要有独特效果
5. **BOSS 太难**：BOSS 要有挑战性，但不能让人放弃
