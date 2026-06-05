---
name: 卡牌游戏开发模板
description: 卡牌游戏开发的最佳实践和模板，适用于TCG、DBG等卡牌类游戏
trigger: 卡牌游戏,card game,TCG,DBG,集换式卡牌
examples: 炉石传说|杀戮尖塔|游戏王|万智牌
---

# 卡牌游戏开发模板

## 核心系统设计

### 1. 卡牌系统
```
Card {
  id: 唯一标识
  name: 卡牌名称
  type: 类型(随从/法术/装备)
  cost: 费用
  attack: 攻击力(随从)
  health: 生命值(随从)
  effects: 效果列表
  rarity: 稀有度
}
```

### 2. 回合系统
- 回合开始：抽牌、重置资源
- 回合中：出牌、攻击、结束
- 回合结束：触发结束效果

### 3. 战斗系统
- 伤害计算：攻击 - 护甲 = 实际伤害
- 效果叠加：Buff/Debuff管理
- 触发机制：战吼、亡语、光环

## 关键技术实现

### 卡牌效果系统
```javascript
// 效果触发器
const triggers = {
  ON_PLAY: 'onPlay',      // 打出时
  ON_DEATH: 'onDeath',    // 死亡时
  ON_ATTACK: 'onAttack',  // 攻击时
  ON_DAMAGE: 'onDamage',  // 受伤时
  END_TURN: 'endTurn'     // 回合结束
}
```

### AI对手设计
- 基于规则的决策树
- 评估函数：场面价值、手牌价值、血量价值
- 蒙特卡洛树搜索(高级)

## 数据存储

### 玩家数据
- 卡牌收藏
- 套牌配置
- 排位分数
- 成就进度

## 平衡性设计

- 每周数据统计
- 胜率监控
- 卡牌使用率分析
- 定期平衡补丁
