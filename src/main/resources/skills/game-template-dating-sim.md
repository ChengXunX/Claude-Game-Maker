---
name: 养成游戏开发模板
description: 养成游戏开发模板，适用于恋爱模拟、宠物养成、角色养成类游戏
trigger: 养成游戏,恋爱游戏,dating sim,宠物养成,角色养成,恋爱模拟
examples: 恋与制作人|奇迹暖暖|旅行青蛙|猫咪后院|美少女梦工厂|LovePlus
---

# 养成游戏开发模板

## 游戏设计核心原则

### 核心循环（每天/每周期）
```
安排日程 → 执行活动 → 提升属性 → 触发事件 → 好感度变化
```
- **养成感**：看着角色成长的满足感
- **收集感**：收集不同结局、服装、事件
- **代入感**：觉得自己就是主角

### 玩家心理学
- **"养成满足"的快乐**：看着角色变强/变美
- **"攻略成功"的成就感**：成功攻略角色的满足
- **"收集完整"的欲望**：想收集所有结局
- **"情感投入"的代入感**：对角色产生感情

### 养成设计要点
```
养成核心：
1. 日程安排：合理安排时间
2. 属性提升：多种属性成长
3. 事件触发：特定条件触发事件
4. 多结局：不同选择不同结局
```

## 核心系统设计

### 1. 日程系统
```javascript
class ScheduleSystem {
  constructor() {
    this.day = 1;
    this.maxDays = 365;
    this.timeSlots = ['morning', 'afternoon', 'evening'];
    this.schedule = {};
  }
  
 安排活动(timeSlot, activity) {
    if (!this.timeSlots.includes(timeSlot)) return false;
    
    this.schedule[this.day] = this.schedule[this.day] || {};
    this.schedule[this.day][timeSlot] = activity;
    
    return true;
  }
  
  advanceDay() {
    // 执行今天的活动
    const todaySchedule = this.schedule[this.day];
    if (todaySchedule) {
      for (const [timeSlot, activity] of Object.entries(todaySchedule)) {
        this.executeActivity(activity);
      }
    }
    
    this.day++;
    
    // 检查是否结束
    if (this.day > this.maxDays) {
      this.endGame();
    }
  }
  
  executeActivity(activity) {
    // 执行活动，提升属性
    for (const [stat, value] of Object.entries(activity.effects)) {
      this.character.stats[stat] += value;
    }
    
    // 检查事件触发
    this.checkEvents();
  }
}

const ACTIVITIES = {
  study: { name: '学习', effects: { intelligence: 2, stress: 1 } },
  exercise: { name: '运动', effects: { fitness: 2, stress: -1 } },
  work: { name: '打工', effects: { money: 100, stress: 2 } },
  rest: { name: '休息', effects: { stress: -3 } },
  social: { name: '社交', effects: { charm: 1, social: 2 } }
};
```

### 2. 属性系统
```javascript
class StatsSystem {
  constructor() {
    this.stats = {
      intelligence: 10, // 智力
      fitness: 10, // 体力
      charm: 10, // 魅力
      social: 10, // 社交
      stress: 0, // 压力
      money: 0 // 金钱
    };
    
    this.maxStats = {
      intelligence: 100,
      fitness: 100,
      charm: 100,
      social: 100,
      stress: 100,
      money: 99999
    };
  }
  
  addStat(stat, amount) {
    this.stats[stat] = Math.max(0, Math.min(this.maxStats[stat], this.stats[stat] + amount));
  }
  
  getStatLevel(stat) {
    const value = this.stats[stat];
    if (value >= 80) return 'S';
    if (value >= 60) return 'A';
    if (value >= 40) return 'B';
    if (value >= 20) return 'C';
    return 'D';
  }
  
  checkStress() {
    if (this.stats.stress >= 80) {
      this.applyStressPenalty();
    }
  }
}
```

### 3. 好感度系统
```javascript
class AffectionSystem {
  constructor() {
    this.characters = {};
  }
  
  addCharacter(config) {
    this.characters[config.id] = {
      name: config.name,
      affection: 0,
      maxAffection: 100,
      events: [],
      gifts: config.gifts || {}
    };
  }
  
  addAffection(characterId, amount) {
    const char = this.characters[characterId];
    if (!char) return;
    
    char.affection = Math.max(0, Math.min(char.maxAffection, char.affection + amount));
    
    // 检查好感度事件
    this.checkAffectionEvents(characterId);
  }
  
  giveGift(characterId, giftId) {
    const char = this.characters[characterId];
    if (!char) return false;
    
    const gift = char.gifts[giftId];
    if (!gift) return false;
    
    // 检查是否喜欢
    if (gift.liked) {
      this.addAffection(characterId, gift.affection);
      this.showMessage(`${char.name} 很喜欢这个礼物！`);
    } else {
      this.addAffection(characterId, -gift.affection / 2);
      this.showMessage(`${char.name} 不太喜欢这个礼物...`);
    }
    
    return true;
  }
  
  checkAffectionEvents(characterId) {
    const char = this.characters[characterId];
    
    // 检查好感度事件
    for (const event of char.events) {
      if (!event.triggered && char.affection >= event.requiredAffection) {
        this.triggerEvent(event);
        event.triggered = true;
      }
    }
  }
}
```

### 4. 事件系统
```javascript
class EventSystem {
  constructor() {
    this.events = [];
    this.triggeredEvents = [];
  }
  
  addEvent(config) {
    this.events.push({
      id: config.id,
      type: config.type,
      condition: config.condition,
      story: config.story,
      choices: config.choices,
      triggered: false
    });
  }
  
  checkEvents() {
    for (const event of this.events) {
      if (!event.triggered && event.condition(this.gameState)) {
        this.triggerEvent(event);
      }
    }
  }
  
  triggerEvent(event) {
    event.triggered = true;
    this.triggeredEvents.push(event);
    
    // 显示事件
    this.showEvent(event);
  }
  
  makeChoice(eventId, choiceId) {
    const event = this.events.find(e => e.id === eventId);
    if (!event) return false;
    
    const choice = event.choices.find(c => c.id === choiceId);
    if (!choice) return false;
    
    // 应用选择效果
    for (const [stat, value] of Object.entries(choice.effects)) {
      this.statsSystem.addStat(stat, value);
    }
    
    // 改变好感度
    if (choice.affectionChange) {
      for (const [charId, amount] of Object.entries(choice.affectionChange)) {
        this.affectionSystem.addAffection(charId, amount);
      }
    }
    
    return true;
  }
}
```

### 5. 结局系统
```javascript
class EndingSystem {
  constructor() {
    this.endings = [];
    this.unlockedEndings = [];
  }
  
  addEnding(config) {
    this.endings.push({
      id: config.id,
      name: config.name,
      condition: config.condition,
      story: config.story,
      unlocked: false
    });
  }
  
  checkEndings() {
    for (const ending of this.endings) {
      if (!ending.unlocked && ending.condition(this.gameState)) {
        this.unlockEnding(ending);
      }
    }
  }
  
  unlockEnding(ending) {
    ending.unlocked = true;
    this.unlockedEndings.push(ending);
    
    // 显示结局
    this.showEnding(ending);
  }
  
  getEndingProgress() {
    return {
      unlocked: this.unlockedEndings.length,
      total: this.endings.length,
      percentage: Math.floor(this.unlockedEndings.length / this.endings.length * 100)
    };
  }
}

const ENDINGS = [
  {
    id: 'good_ending',
    name: '完美结局',
    condition: (state) => state.affection >= 80 && state.stats.intelligence >= 60,
    story: '你们幸福地在一起了...'
  },
  {
    id: 'normal_ending',
    name: '普通结局',
    condition: (state) => state.affection >= 50,
    story: '你们成为了好朋友...'
  },
  {
    id: 'bad_ending',
    name: '坏结局',
    condition: (state) => state.affection < 20,
    story: '你们渐行渐远...'
  }
];
```

## 迭代策略

### 第一版：基础养成
- 日程系统
- 属性系统
- 简单事件
- 基础 UI

### 第二版：好感度系统
- 好感度系统
- 送礼系统
- 多个角色
- 事件系统

### 第三版：剧情系统
- 分支剧情
- 多结局
- 存档系统
- CG 收集

### 第四版：深度玩法
- 30 个结局
- 成就系统
- 图鉴系统
- 音乐系统

### 第五版：变现
- 看广告解锁剧情
- 内购系统
- 社交分享
- 推送通知

## 常见错误

1. **养成太无聊**：要有丰富的事件
2. **好感度太难涨**：好感度提升要合理
3. **结局太少**：要有足够多的结局
4. **没有代入感**：要让玩家有代入感
5. **没有收集欲**：要有收集系统
