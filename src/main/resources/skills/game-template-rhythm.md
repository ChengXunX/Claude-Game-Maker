---
name: 音乐节奏游戏开发模板
description: 音乐节奏游戏开发模板，适用于音游、节奏类游戏
trigger: 音乐游戏,节奏游戏,rhythm game,音游,跳舞机,节奏大师
examples: Cytus|Arcaea|OSU!|太鼓达人|节奏大师|Phigros
---

# 音乐节奏游戏开发模板

## 游戏设计核心原则

### 核心循环（每首歌 1-5 分钟）
```
听音乐 → 看音符 → 跟着节奏点击 → 得分 → 评级
```
- **节奏感**：点击必须和音乐节拍同步
- **反馈感**：每次点击都有视觉+音效反馈
- **挑战性**：从简单到困难，让玩家有成长感

### 玩家心理学
- **"完美同步"的爽感**：和音乐完美同步的满足感
- **"全连击"的成就感**：一首歌全连的成就感
- **"挑战高难度"的欲望**：从 Easy 到 Expert 的成长
- **"收集歌曲"的乐趣**：解锁新歌曲的期待感

### 音符设计要点
```
音符类型：
1. Tap（点击）：在指定时间点击
2. Hold（长按）：按住指定时长
3. Slide（滑动）：在指定方向滑动
4. Flick（轻弹）：快速滑动

判定等级：
- Perfect（完美）：±30ms
- Great（优秀）：±60ms
- Good（良好）：±100ms
- Miss（错过）：>100ms
```

## 核心系统设计

### 1. 谱面系统
```javascript
class Beatmap {
  constructor(config) {
    this.songId = config.songId;
    this.difficulty = config.difficulty;
    this.bpm = config.bpm;
    this.notes = config.notes;
    this.duration = config.duration;
  }
  
  getNotesInWindow(currentTime, windowMs) {
    return this.notes.filter(note => {
      const diff = Math.abs(note.time - currentTime);
      return diff <= windowMs / 1000;
    });
  }
}

// 谱面数据格式
const BEATMAP = {
  songId: 'song001',
  difficulty: 'Hard',
  bpm: 120,
  duration: 180,
  notes: [
    { time: 1.234, type: 'tap', position: { x: 100, y: 200 } },
    { time: 2.345, type: 'hold', position: { x: 200, y: 200 }, duration: 0.5 },
    { time: 3.456, type: 'slide', position: { x: 300, y: 200 }, direction: 'left' },
    { time: 4.567, type: 'flick', position: { x: 400, y: 200 }, direction: 'up' }
  ]
};
```

### 2. 判定系统
```javascript
class JudgmentSystem {
  constructor() {
    this.windows = {
      perfect: 30,  // ±30ms
      great: 60,    // ±60ms
      good: 100,    // ±100ms
      miss: 150     // >100ms
    };
  }
  
  judge(noteTime, inputTime) {
    const diff = Math.abs(noteTime - inputTime) * 1000; // 转换为毫秒
    
    if (diff <= this.windows.perfect) return 'perfect';
    if (diff <= this.windows.great) return 'great';
    if (diff <= this.windows.good) return 'good';
    return 'miss';
  }
  
  getScore(judgment) {
    const scores = { perfect: 1000, great: 500, good: 100, miss: 0 };
    return scores[judgment];
  }
  
  getComboMultiplier(combo) {
    if (combo >= 100) return 4;
    if (combo >= 50) return 3;
    if (combo >= 20) return 2;
    if (combo >= 10) return 1.5;
    return 1;
  }
}
```

### 3. 音频同步系统
```javascript
class AudioSync {
  constructor() {
    this.audioContext = new AudioContext();
    this.startTime = 0;
    this.offset = 0; // 音频延迟补偿
  }
  
  play(audioBuffer) {
    this.source = this.audioContext.createBufferSource();
    this.source.buffer = audioBuffer;
    this.source.connect(this.audioContext.destination);
    this.source.start(0);
    this.startTime = this.audioContext.currentTime;
  }
  
  getCurrentTime() {
    return this.audioContext.currentTime - this.startTime - this.offset;
  }
  
  // 延迟校准
  calibrate(offsetMs) {
    this.offset = offsetMs / 1000;
  }
}
```

### 4. 评级系统
```javascript
class RatingSystem {
  constructor() {
    this.ratings = {
      SSS: { min: 990000, color: '#ff00ff' },
      SS: { min: 980000, color: '#ff00ff' },
      S: { min: 950000, color: '#ffff00' },
      A: { min: 900000, color: '#00ff00' },
      B: { min: 800000, color: '#00ffff' },
      C: { min: 700000, color: '#ffffff' },
      D: { min: 0, color: '#888888' }
    };
  }
  
  getRating(score, maxScore) {
    const ratio = (score / maxScore) * 1000000;
    
    for (const [rating, config] of Object.entries(this.ratings)) {
      if (ratio >= config.min) {
        return { rating, color: config.color };
      }
    }
    
    return { rating: 'D', color: '#888888' };
  }
}
```

### 5. 连击系统
```javascript
class ComboSystem {
  constructor() {
    this.combo = 0;
    this.maxCombo = 0;
  }
  
  hit() {
    this.combo++;
    this.maxCombo = Math.max(this.maxCombo, this.combo);
    this.showComboEffect();
  }
  
  miss() {
    this.combo = 0;
    this.showComboBreak();
  }
  
  showComboEffect() {
    if (this.combo >= 10 && this.combo % 10 === 0) {
      // 每 10 连击显示特效
      this.showSpecialEffect(this.combo);
    }
  }
}
```

## 迭代策略

### 第一版：基础音游
- 1 首歌曲
- Tap 音符
- 基础判定
- 基础 UI

### 第二版：多种音符
- 添加 Hold、Slide、Flick
- 3 首歌曲
- 连击系统
- 评级系统

### 第三版：内容扩展
- 10 首歌曲
- 多难度等级
- 成就系统
- 排行榜

### 第四版：深度玩法
- 30 首歌曲
- 自定义谱面
- 谱面分享
- 每日挑战

### 第五版：社交功能
- 多人对战
- 好友系统
- 社交分享
- 赛季系统

## 常见错误

1. **判定太严**：Perfect 窗口太小会让玩家沮丧
2. **音画不同步**：音频和视觉必须同步，否则无法玩
3. **没有延迟校准**：不同设备有不同的延迟，必须支持校准
4. **谱面太难**：Easy 难度要让新手能玩
5. **没有反馈**：每次点击都要有视觉+音效反馈
