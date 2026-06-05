---
name: MOBA游戏开发模板
description: MOBA游戏开发模板，适用于多人在线战术竞技类游戏
trigger: MOBA,多人对战,战术竞技,英雄对战,推塔游戏
examples: 英雄联盟|王者荣耀|DOTA2|风暴英雄|300英雄
---

# MOBA游戏开发模板

## 核心系统设计

### 1. 英雄系统
```
Hero {
  id: 英雄ID
  name: 英雄名
  role: 定位(坦克/战士/刺客/法师/射手/辅助)
  baseStats: { hp, mp, attack, defense, speed }
  skills: [Q, W, E, R]  // 4个技能
  passive: 被动技能
}
```

### 2. 经济系统
| 来源 | 金币 |
|------|------|
| 小兵击杀 | 20-30 |
| 英雄击杀 | 200-500 |
| 助攻 | 100-150 |
| 防御塔 | 150 |
| 野怪 | 50-100 |

### 3. 装备系统
```javascript
const itemTree = {
  '长剑': { cost: 350, stats: { attack: 10 } },
  '暴风大剑': { cost: 1300, stats: { attack: 40 }, recipe: ['长剑', '长剑'] },
  '无尽之刃': { cost: 3400, stats: { attack: 80, crit: 0.25 }, recipe: ['暴风大剑', '灵巧披风'] }
}
```

## 关键技术实现

### 技能系统
```javascript
class Skill {
  constructor(config) {
    this.damage = config.damage;
    this.cooldown = config.cooldown;
    this.range = config.range;
    this.aoe = config.aoe;
  }
  
  cast(caster, target) {
    if (this.onCooldown) return false;
    
    const damage = this.calculateDamage(caster, target);
    target.takeDamage(damage);
    
    this.startCooldown();
    return true;
  }
}
```

### 寻路系统
- A*寻路算法
- 动态避障
- 视野系统

## 匹配系统

| 段位 | 匹配规则 |
|------|----------|
| 青铜-白银 | 相邻段位 |
| 黄金-铂金 | 同段位 |
| 钻石+ | 严格匹配 |

## 游戏模式

- 排位赛
- 匹配赛
- 人机对战
- 自定义房间
- 无限火力(娱乐模式)
