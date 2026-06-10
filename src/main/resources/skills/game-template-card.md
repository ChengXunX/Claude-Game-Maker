---
name: 卡牌游戏开发模板
description: 卡牌游戏开发模板，适用于TCG、DBG、卡牌策略类游戏
trigger: 卡牌游戏,card game,TCG,DBG,集换式卡牌,卡牌构筑
examples: 炉石传说|杀戮尖塔|游戏王|万智牌|Slay the Spire
---

# 卡牌游戏开发模板

## 游戏设计核心原则

### 核心循环（每局 10-30 分钟）
```
抽牌 → 选择出牌 → 结算效果 → 回合结束 → 对手回合 → 抽牌
```
- **策略深度**：每张牌都有意义，没有"废牌"
- **随机与控制**：抽牌是随机的，但出牌是策略的
- **组合乐趣**：牌之间有协同效应

### 玩家心理学
- **"抽到关键牌"**：抽到想要的牌的爽感
- **"完美 combo"**：多张牌组合产生强大效果的满足感
- **"预测对手"**：猜对手下一步并反制的策略感
- **"构筑乐趣"：组建卡组的过程本身就是乐趣

### 卡牌设计原则
```
每张牌要有"身份"：
1. 费用：决定什么时候能出
2. 效果：决定出牌的价值
3. 时机：决定什么时候出最好
4. 目标：决定对谁出
```

## 核心系统设计

### 1. 卡牌数据结构
```javascript
class Card {
  constructor(config) {
    this.id = config.id;
    this.name = config.name;
    this.type = config.type; // 'minion', 'spell', 'weapon'
    this.cost = config.cost;
    this.attack = config.attack; // 随从/武器
    this.health = config.health; // 随从
    this.durability = config.durability; // 武器
    this.effects = config.effects || [];
    this.rarity = config.rarity; // 'common', 'rare', 'epic', 'legendary'
    this.description = config.description;
  }
  
  // 卡牌效果
  executeEffect(context) {
    for (const effect of this.effects) {
      effect.execute(context);
    }
  }
}

// 卡牌效果系统
const EFFECTS = {
  dealDamage: {
    execute: (context, amount) => {
      context.target.takeDamage(amount);
    }
  },
  drawCards: {
    execute: (context, amount) => {
      context.player.drawCards(amount);
    }
  },
  heal: {
    execute: (context, amount) => {
      context.target.heal(amount);
    }
  },
  summon: {
    execute: (context, minionId) => {
      context.player.summonMinion(minionId);
    }
  }
};
```

### 2. 回合系统
```javascript
class TurnManager {
  constructor(player1, player2) {
    this.players = [player1, player2];
    this.currentPlayerIndex = 0;
    this.turnNumber = 0;
  }
  
  startTurn() {
    this.turnNumber++;
    const player = this.currentPlayer;
    
    // 增加法力水晶
    player.maxMana = Math.min(10, player.maxMana + 1);
    player.mana = player.maxMana;
    
    // 抽牌
    player.drawCard();
    
    // 重置随从攻击次数
    player.resetMinions();
    
    // 触发回合开始效果
    player.triggerTurnStartEffects();
  }
  
  endTurn() {
    const player = this.currentPlayer;
    
    // 触发回合结束效果
    player.triggerTurnEndEffects();
    
    // 切换玩家
    this.currentPlayerIndex = (this.currentPlayerIndex + 1) % 2;
    
    // 开始对手回合
    this.startTurn();
  }
  
  get currentPlayer() {
    return this.players[this.currentPlayerIndex];
  }
}
```

### 3. 随从战斗系统
```javascript
class MinionCombat {
  attack(attacker, defender) {
    // 随从攻击随从
    defender.takeDamage(attacker.attack);
    attacker.takeDamage(defender.attack);
    
    // 检查死亡
    if (defender.health <= 0) {
      defender.die();
    }
    if (attacker.health <= 0) {
      attacker.die();
    }
    
    // 触发攻击后效果
    attacker.triggerAfterAttack(defender);
  }
  
  attackHero(attacker, hero) {
    // 随从攻击英雄
    hero.takeDamage(attacker.attack);
    
    // 触发攻击后效果
    attacker.triggerAfterAttack(hero);
  }
}
```

### 4. 卡组构筑系统
```javascript
class DeckBuilder {
  constructor() {
    this.cards = [];
    this.maxCards = 30;
    this.maxCopies = 2; // 每张牌最多2张
  }
  
  addCard(card) {
    if (this.cards.length >= this.maxCards) {
      return false; // 卡组已满
    }
    
    const copies = this.cards.filter(c => c.id === card.id).length;
    if (copies >= this.maxCopies) {
      return false; // 已达最大数量
    }
    
    this.cards.push(card);
    return true;
  }
  
  removeCard(cardId) {
    const index = this.cards.findIndex(c => c.id === cardId);
    if (index !== -1) {
      this.cards.splice(index, 1);
      return true;
    }
    return false;
  }
  
  validate() {
    // 检查卡组是否合法
    return this.cards.length === this.maxCards;
  }
}
```

## 卡牌数据库示例

```javascript
const CARD_DATABASE = [
  // 基础卡
  { id: 'fireball', name: '火球术', type: 'spell', cost: 4, 
    effects: [{ type: 'dealDamage', amount: 6 }], rarity: 'common' },
  
  { id: 'wolf', name: '灰狼', type: 'minion', cost: 3, attack: 3, health: 3,
    effects: [], rarity: 'common' },
  
  // 稀有卡
  { id: 'heal', name: '治疗之触', type: 'spell', cost: 3,
    effects: [{ type: 'heal', amount: 8 }], rarity: 'rare' },
  
  // 史诗卡
  { id: 'dragon', name: '巨龙', type: 'minion', cost: 8, attack: 8, health: 8,
    effects: [{ type: 'dealDamage', amount: 3, target: 'all_enemies' }], rarity: 'epic' },
  
  // 传说卡
  { id: 'deathwing', name: '死亡之翼', type: 'minion', cost: 10, attack: 12, health: 12,
    effects: [{ type: 'destroyAll', target: 'all_minions' }], rarity: 'legendary' }
];
```

## 迭代策略

### 第一版：基础对战
- 基础卡牌系统
- 回合制对战
- 10 张基础卡
- AI 对手

### 第二版：卡牌效果
- 添加卡牌效果系统
- 添加 30 张卡
- 添加法力水晶系统
- 添加随从战斗

### 第三版：卡组构筑
- 卡组编辑器
- 添加 50 张卡
- 添加卡牌稀有度
- 添加抽卡动画

### 第四版：深度玩法
- 添加 100 张卡
- 添加连击系统
- 添加卡牌组合
- 添加 PVP 对战

### 第五版：打磨
- 平衡卡牌效果
- 添加卡牌特效
- 添加音效和音乐
- 添加剧情模式

## 常见错误

1. **卡牌不平衡**：某些卡太强或太弱，需要大量测试
2. **随机性太强**：抽牌是随机的，但出牌要有策略
3. **卡牌没有差异**：每张牌要有独特效果，不能只是数值差异
4. **回合太长**：每个回合要有时间限制，否则会拖沓
5. **没有反馈**：出牌要有视觉和音效反馈
