---
name: 生存游戏开发模板
description: 生存游戏开发模板，适用于生存建造、末日生存、荒野求生类游戏
trigger: 生存游戏,survival game,末日生存,荒野求生,生存建造
examples: 饥荒|方舟生存进化|森林|绿色地狱|英灵神殿
---

# 生存游戏开发模板

## 核心系统设计

### 1. 生存属性
```
Player {
  health: 生命值(100)
  hunger: 饥饿值(100) - 持续下降
  thirst: 口渴值(100) - 持续下降
  stamina: 体力值(100) - 消耗后恢复
  temperature: 体温(36-37正常)
  sanity: 理智值(100)
}
```

### 2. 状态效果
| 状态 | 触发条件 | 效果 |
|------|----------|------|
| 饥饿 | hunger=0 | 持续掉血 |
| 脱水 | thirst=0 | 持续掉血 |
| 失温 | temperature<35 | 移速降低 |
| 中暑 | temperature>39 | 持续掉血 |

### 3. 制作系统
```javascript
const recipes = {
  'campfire': {
    materials: { wood: 5, stone: 3 },
    craftTime: 5,
    unlockLevel: 1
  },
  'stone_axe': {
    materials: { wood: 2, stone: 3, fiber: 1 },
    craftTime: 3,
    unlockLevel: 1
  }
}
```

## 关键技术实现

### 天气系统
```javascript
class WeatherSystem {
  constructor() {
    this.current = 'clear';
    this.temperature = 25;
    this.humidity = 0.5;
  }
  
  update(deltaTime) {
    // 天气变化逻辑
    // 温度随天气和时间变化
  }
}
```

### 资源生成
- 程序化地图生成
- 资源点分布
- 稀有资源刷新

## 建造系统

| 建筑 | 材料 | 功能 |
|------|------|------|
| 营火 | 木材×5 | 烹饪、取暖 |
| 木屋 | 木材×50 | 庇护所 |
| 工作台 | 木材×20 | 高级制作 |
| 围栏 | 木材×10 | 防御 |

## 敌人系统

- 野生动物(被动/主动)
- 敌对NPC
- Boss怪物
- 夜间怪物

## 多人联机

- 合作生存
- PVP服务器
- 部落系统
- 交易系统
