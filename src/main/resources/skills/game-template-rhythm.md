---
name: 音乐节奏游戏开发模板
description: 音乐节奏游戏开发模板，适用于音游、节奏类游戏
trigger: 音乐游戏,节奏游戏,rhythm game,音游,跳舞机
examples: Cytus|Arcaea|OSU!|太鼓达人|节奏大师
---

# 音乐节奏游戏开发模板

## 核心系统设计

### 1. 谱面系统
```
Beatmap {
  songId: 歌曲ID
  difficulty: 难度(Easy/Normal/Hard/Expert)
  bpm: 节拍数
  notes: [
    { time: 1.234, type: 'tap', position: {x, y} },
    { time: 2.345, type: 'hold', duration: 0.5 },
    { time: 3.456, type: 'slide', path: [...] }
  ]
}
```

### 2. 判定系统
| 判定 | 时间窗口 | 分数 |
|------|----------|------|
| Perfect | ±30ms | 100% |
| Great | ±60ms | 80% |
| Good | ±100ms | 50% |
| Miss | >100ms | 0% |

### 3. 连击系统
- Combo加成：连击数越高，分数加成越大
- 连击中断：Miss时重置连击
- 全连奖励：Full Combo额外加分

## 技术实现

### 音频同步
```javascript
class AudioEngine {
  constructor() {
    this.audioContext = new AudioContext();
    this.startTime = 0;
  }
  
  play() {
    this.startTime = this.audioContext.currentTime;
  }
  
  getCurrentTime() {
    return this.audioContext.currentTime - this.startTime;
  }
}
```

### 输入延迟补偿
```javascript
function judgeNote(noteTime, inputTime, offset) {
  const diff = Math.abs(inputTime - noteTime - offset);
  if (diff < 0.03) return 'PERFECT';
  if (diff < 0.06) return 'GREAT';
  if (diff < 0.10) return 'GOOD';
  return 'MISS';
}
```

## 谱面编辑器

- 可视化谱面编辑
- 自动对齐节拍
- 导入/导出格式
- 试玩功能

## 视觉效果

- 音符特效
- 背景动画
- 判定特效
- 连击特效
