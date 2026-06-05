---
name: 养成游戏开发模板
description: 养成游戏开发模板，适用于恋爱模拟、宠物养成、角色养成类游戏
trigger: 养成游戏,恋爱游戏,dating sim,宠物养成,角色养成
examples: 恋与制作人|奇迹暖暖|旅行青蛙|猫咪后院|美少女梦工厂
---

# 养成游戏开发模板

## 核心系统设计

### 1. 好感度系统
```
Character {
  name: 角色名
  affection: 好感度(0-1000)
  mood: 心情状态
  gifts: 喜好/厌恶物品列表
  events: 已触发事件列表
}
```

### 2. 时间系统
| 时段 | 可执行操作 |
|------|------------|
| 早晨 | 问候、送早餐 |
| 中午 | 约会、聊天 |
| 晚上 | 送礼物、告白 |
| 周末 | 特殊约会 |

### 3. 选择分支
```javascript
const dialogueTree = {
  'start': {
    text: '今天想做什么？',
    choices: [
      { text: '去看电影', next: 'movie', affection: +10 },
      { text: '去逛街', next: 'shopping', affection: +5 },
      { text: '待在家里', next: 'home', affection: -5 }
    ]
  }
}
```

## 关键系统实现

### 送礼系统
```javascript
function giveGift(character, item) {
  let affectionChange = 0;
  
  if (character.loves.includes(item.id)) {
    affectionChange = 50;
  } else if (character.likes.includes(item.id)) {
    affectionChange = 20;
  } else if (character.hates.includes(item.id)) {
    affectionChange = -30;
  }
  
  character.affection += affectionChange;
  return affectionChange;
}
```

### 事件触发
```javascript
function checkEventTrigger(character, conditions) {
  return conditions.every(condition => {
    switch(condition.type) {
      case 'affection':
        return character.affection >= condition.value;
      case 'event':
        return character.events.includes(condition.eventId);
      case 'time':
        return gameTime.day >= condition.day;
    }
  });
}
```

## 结局系统

| 好感度 | 结局 |
|--------|------|
| 900+ | 完美结局 |
| 700-899 | 普通结局 |
| 500-699 | 友情结局 |
| <500 | 普通结局 |

## 变现设计

- 角色解锁
- 服装道具
- 剧情章节
- 特殊事件
