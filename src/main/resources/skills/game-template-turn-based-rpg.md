---
name: 回合制RPG游戏开发模板
description: 回合制RPG游戏开发模板，适用于回合制战斗、策略RPG类游戏
trigger: 回合制RPG,回合制游戏,turn-based RPG,策略RPG,日式RPG
examples: 最终幻想|勇者斗恶龙|女神异闻录|八方旅人|宝可梦
---

# 回合制RPG游戏开发模板

## 核心系统设计

### 1. 战斗系统
```
BattleState {
  allies: [角色1, 角色2, 角色3, 角色4]
  enemies: [怪物1,怪物2,怪物3]
  turnOrder: 速度排序
  currentTurn: 当前行动角色
}
```

### 2. 行动系统
| 行动 | 说明 |
|------|------|
| 攻击 | 普通攻击 |
| 技能 | 消耗MP的技能 |
| 道具 | 使用背包物品 |
| 防御 | 减少受到伤害 |
| 逃跑 | 逃离战斗 |

### 3. 属性克制
```javascript
const elementSystem = {
  fire: { strong: 'ice', weak: 'water' },
  ice: { strong: 'fire', weak: 'lightning' },
  water: { strong: 'fire', weak: 'ice' },
  lightning: { strong: 'ice', weak: 'earth' },
  earth: { strong: 'lightning', weak: 'water' }
};
```

## 关键技术实现

### 回合排序
```javascript
function calculateTurnOrder(characters) {
  return characters.sort((a, b) => {
    // 速度决定行动顺序
    const speedA = a.stats.speed + Math.random() * 10;
    const speedB = b.stats.speed + Math.random() * 10;
    return speedB - speedA;
  });
}
```

### 伤害计算
```javascript
function calculateDamage(attacker, defender, skill) {
  let damage;
  
  if (skill.type === 'physical') {
    damage = attacker.stats.attack * skill.power / defender.stats.defense;
  } else if (skill.type === 'magical') {
    damage = attacker.stats.magic * skill.power / (defender.stats.magicDef * 0.5);
  }
  
  // 属性克制
  if (isEffective(skill.element, defender.element)) {
    damage *= 1.5;
  }
  
  // 暴击
  if (Math.random() < attacker.stats.critRate) {
    damage *= 1.5;
  }
  
  return Math.floor(damage);
}
```

## 遇敌系统

| 类型 | 概率 | 说明 |
|------|------|------|
| 普通 | 70% | 常规怪物 |
| 精英 | 25% | 强化怪物 |
| Boss | 5% | BOSS战 |

## 经验与升级

```javascript
function calculateExpGain(enemy) {
  const baseExp = enemy.level * 10;
  const levelDiff = enemy.level - player.level;
  
  if (levelDiff > 5) return baseExp * 1.5;
  if (levelDiff < -5) return baseExp * 0.5;
  return baseExp;
}
```

## 队伍系统

- 角色切换
- 队伍阵型
- 连携技能
- 友好度系统
