---
name: 剧情推进游戏开发模板
description: 剧情推进游戏开发模板，适用于视觉小说、文字冒险、剧情向游戏
trigger: story, 剧情, narrative, 叙事, branch, 分支, ending, 结局, visual novel, 视觉小说
examples: 极乐迪斯科|底特律变人|生命奇异|428|逆转裁判|Steins;Gate
---

# 剧情推进游戏开发模板

## 游戏设计核心原则

### 核心循环（每章 30-60 分钟）
```
阅读剧情 → 做出选择 → 看到后果 → 解锁新剧情 → 做出更多选择
```
- **代入感**：玩家觉得自己就是主角
- **选择感**：每个选择都有意义
- **探索欲**：想看所有结局

### 玩家心理学
- **"蝴蝶效应"的震撼**：小选择导致大后果
- **"多结局"的探索欲**：想看所有可能的结局
- **"角色代入"的情感**：对角色产生感情
- **"剧情反转"的惊喜**：出乎意料的剧情发展

### 剧情设计要点
```
剧情核心：
1. 选择有意义：每个选择都会影响剧情
2. 角色立体：角色有性格、有成长
3. 伏笔回收：前面埋的伏笔后面要回收
4. 多条线路：不同的选择走不同的路
```

## 核心系统设计

### 1. 剧情引擎
```javascript
class StoryEngine {
  constructor() {
    this.currentScene = null;
    this.flags = {};
    this.relationships = {};
    this.history = [];
  }
  
  loadScene(sceneId) {
    const scene = SCENES[sceneId];
    if (!scene) return false;
    
    this.currentScene = scene;
    this.history.push(sceneId);
    
    // 检查条件
    if (scene.condition && !scene.condition(this.flags)) {
      return this.loadScene(scene.alternative);
    }
    
    // 显示场景
    this.displayScene(scene);
    
    return true;
  }
  
  makeChoice(choiceId) {
    const choice = this.currentScene.choices.find(c => c.id === choiceId);
    if (!choice) return false;
    
    // 设置标志
    if (choice.setFlags) {
      Object.assign(this.flags, choice.setFlags);
    }
    
    // 改变关系
    if (choice.relationshipChange) {
      for (const [char, change] of Object.entries(choice.relationshipChange)) {
        this.relationships[char] = (this.relationships[char] || 0) + change;
      }
    }
    
    // 跳转到下一个场景
    return this.loadScene(choice.nextScene);
  }
  
  setFlag(key, value) {
    this.flags[key] = value;
  }
  
  getFlag(key) {
    return this.flags[key];
  }
}

const SCENES = {
  start: {
    text: '你醒来发现自己在一个陌生的房间里...',
    choices: [
      { id: 'explore', text: '探索房间', nextScene: 'explore_room', setFlags: { explored: true } },
      { id: 'wait', text: '等待有人来', nextScene: 'wait_for_help' }
    ]
  },
  explore_room: {
    condition: (flags) => flags.explored,
    text: '你在房间里发现了一把钥匙...',
    choices: [
      { id: 'take', text: '拿起钥匙', nextScene: 'take_key', setFlags: { hasKey: true } },
      { id: 'leave', text: '放下钥匙', nextScene: 'leave_key' }
    ]
  }
};
```

### 2. 伏笔系统
```javascript
class ForeshadowSystem {
  constructor() {
    this.foreshadows = [];
    this.revealed = [];
  }
  
  addForeshadow(config) {
    this.foreshadows.push({
      id: config.id,
      hint: config.hint,
      revealScene: config.revealScene,
      revealed: false
    });
  }
  
  checkReveal(sceneId) {
    for (const foreshadow of this.foreshadows) {
      if (!foreshadow.revealed && foreshadow.revealScene === sceneId) {
        this.revealForeshadow(foreshadow);
      }
    }
  }
  
  revealForeshadow(foreshadow) {
    foreshadow.revealed = true;
    this.revealed.push(foreshadow);
    
    // 显示伏笔回收提示
    this.showMessage(`伏笔回收: ${foreshadow.hint}`);
  }
}

const FORESHADOWS = [
  { id: 'mysterious_letter', hint: '你之前看到的神秘信件...', revealScene: 'chapter3_reveal' },
  { id: 'strange_sound', hint: '你之前听到的奇怪声音...', revealScene: 'chapter5_reveal' }
];
```

### 3. 角色关系系统
```javascript
class RelationshipSystem {
  constructor() {
    this.characters = {};
  }
  
  addCharacter(config) {
    this.characters[config.id] = {
      name: config.name,
      trust: 0,
      affection: 0,
      flags: {}
    };
  }
  
  changeTrust(characterId, amount) {
    const char = this.characters[characterId];
    if (!char) return;
    
    char.trust = Math.max(-100, Math.min(100, char.trust + amount));
    this.checkRelationshipChange(characterId);
  }
  
  changeAffection(characterId, amount) {
    const char = this.characters[characterId];
    if (!char) return;
    
    char.affection = Math.max(-100, Math.min(100, char.affection + amount));
    this.checkRelationshipChange(characterId);
  }
  
  checkRelationshipChange(characterId) {
    const char = this.characters[characterId];
    
    // 检查关系变化
    if (char.trust >= 50 && !char.flags.highTrust) {
      char.flags.highTrust = true;
      this.onRelationshipChange(characterId, 'highTrust');
    }
    
    if (char.affection >= 50 && !char.flags.highAffection) {
      char.flags.highAffection = true;
      this.onRelationshipChange(characterId, 'highAffection');
    }
  }
  
  getRelationship(characterId) {
    const char = this.characters[characterId];
    if (!char) return null;
    
    return {
      trust: char.trust,
      affection: char.affection,
      level: this.getRelationshipLevel(char)
    };
  }
  
  getRelationshipLevel(char) {
    const total = char.trust + char.affection;
    if (total >= 80) return 'soulmate';
    if (total >= 50) return 'close';
    if (total >= 20) return 'friend';
    if (total >= 0) return 'acquaintance';
    return 'stranger';
  }
}
```

### 4. 存档系统
```javascript
class SaveSystem {
  constructor() {
    this.slots = 3;
  }
  
  save(slot, gameState) {
    const saveData = {
      timestamp: Date.now(),
      scene: gameState.currentScene,
      flags: gameState.flags,
      relationships: gameState.relationships,
      history: gameState.history
    };
    
    localStorage.setItem(`save_${slot}`, JSON.stringify(saveData));
  }
  
  load(slot) {
    const data = localStorage.getItem(`save_${slot}`);
    if (!data) return null;
    
    return JSON.parse(data);
  }
  
  getSaveInfo(slot) {
    const data = this.load(slot);
    if (!data) return null;
    
    return {
      timestamp: new Date(data.timestamp).toLocaleString(),
      scene: data.scene,
      playTime: data.playTime
    };
  }
}
```

### 5. 文本显示系统
```javascript
class TextDisplay {
  constructor() {
    this.textQueue = [];
    this.currentText = '';
    this.charIndex = 0;
    this.speed = 30; // 每个字符的毫秒数
  }
  
  showText(text, speaker) {
    this.textQueue.push({ text, speaker });
    if (!this.isDisplaying) {
      this.displayNext();
    }
  }
  
  displayNext() {
    if (this.textQueue.length === 0) {
      this.isDisplaying = false;
      return;
    }
    
    const { text, speaker } = this.textQueue.shift();
    this.currentText = text;
    this.speaker = speaker;
    this.charIndex = 0;
    this.isDisplaying = true;
    
    this.typeWriter();
  }
  
  typeWriter() {
    if (this.charIndex >= this.currentText.length) {
      this.isDisplaying = false;
      return;
    }
    
    this.charIndex++;
    this.displayText(this.currentText.substring(0, this.charIndex));
    
    setTimeout(() => this.typeWriter(), this.speed);
  }
  
  skip() {
    this.charIndex = this.currentText.length;
    this.displayText(this.currentText);
    this.isDisplaying = false;
  }
}
```

## 迭代策略

### 第一版：基础剧情
- 简单剧情
- 2 个选择
- 基础 UI
- 文字显示

### 第二版：分支剧情
- 多条剧情线
- 选择系统
- 存档系统
- 角色立绘

### 第三版：关系系统
- 角色关系
- 好感度系统
- 多结局
- 伏笔系统

### 第四版：深度玩法
- 10 个结局
- 成就系统
- 图鉴系统
- 音乐系统

### 第五版：变现
- 看广告解锁章节
- 内购系统
- 社交分享
- 推送通知

## 常见错误

1. **选择没有意义**：每个选择都要影响剧情
2. **剧情太平淡**：要有起伏和反转
3. **角色太扁平**：角色要有性格和成长
4. **伏笔没回收**：埋的伏笔后面要回收
5. **没有存档**：长剧情要有存档功能
