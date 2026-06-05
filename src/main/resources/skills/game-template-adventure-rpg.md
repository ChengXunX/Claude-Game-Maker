---
name: 动作RPG游戏开发模板
description: 动作RPG游戏开发模板，适用于ARPG、暗黑类、动作角色扮演类游戏
trigger: 动作RPG,ARPG,暗黑破坏神,动作角色扮演,刷装备游戏
examples: 暗黑破坏神|流放之路|恐怖黎明|火炬之光|原神
---

# 动作RPG游戏开发模板

## 核心系统设计

### 1. 角色属性
```
Character {
  level: 等级
  experience: 经验值
  stats: {
    strength: 力量(影响物理攻击)
    dexterity: 敏捷(影响暴击、闪避)
    intelligence: 智力(影响魔法攻击)
    vitality: 体力(影响生命值)
  }
  derived: {
    hp: 生命值 = base + vitality * 10
    mp: 魔法值 = base + intelligence * 5
    attack: 攻击力 = weapon + strength * 2
    defense: 防御力 = armor + vitality
  }
}
```

### 2. 装备系统
| 部位 | 属性 |
|------|------|
| 武器 | 攻击力、攻速、特效 |
| 头盔 | 防御力、抗性 |
| 铠甲 | 防御力、生命加成 |
| 靴子 | 防御力、移速 |
| 饰品 | 特殊属性 |

### 3. 技能树系统
```javascript
const skillTree = {
  'warrior': {
    'melee': [
      { id: 'slash', name: '斩击', level: 1, damage: 20 },
      { id: 'cleave', name: '横扫', level: 5, damage: 35, aoe: true },
      { id: 'whirlwind', name: '旋风斩', level: 10, damage: 50, aoe: true }
    ],
    'defense': [
      { id: 'block', name: '格挡', level: 1, blockChance: 0.2 },
      { id: 'ironSkin', name: '铁皮', level: 8, defenseBonus: 50 }
    ]
  }
}
```

## 关键技术实现

### 伤害计算
```javascript
function calculateDamage(attacker, defender, skill) {
  const baseDamage = attacker.attack * skill.multiplier;
  const defense = defender.defense * 0.5;
  const critChance = attacker.critRate;
  const isCrit = Math.random() < critChance;
  
  let damage = Math.max(1, baseDamage - defense);
  if (isCrit) damage *= 1.5;
  
  return Math.floor(damage);
}
```

### 随机装备生成
```javascript
function generateEquipment(level, rarity) {
  const base = getBaseEquipment(level);
  const affixCount = rarityToAffixCount(rarity);
  
  const affixes = [];
  for (let i = 0; i < affixCount; i++) {
    affixes.push(randomAffix(base.type));
  }
  
  return { ...base, affixes, rarity };
}
```

## 装备稀有度

| 稀有度 | 颜色 | 词缀数 |
|--------|------|--------|
| 普通 | 白色 | 0 |
| 魔法 | 蓝色 | 1-2 |
| 稀有 | 黄色 | 3-4 |
| 暗金 | 橙色 | 固定属性 |
| 传说 | 绿色 | 5-6 |

## End Game内容

- 秘境/地图系统
- 赛季系统
- 排行榜
- 公会系统
- 交易市场
