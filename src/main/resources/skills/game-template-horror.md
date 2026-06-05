---
name: 恐怖游戏开发模板
description: 恐怖游戏开发模板，适用于生存恐怖、心理恐怖、解谜恐怖类游戏
trigger: 恐怖游戏,horror game,生存恐怖,心理恐怖,恐怖解谜
examples: 生化危机|寂静岭|逃生|港诡实录|纸嫁衣
---

# 恐怖游戏开发模板

## 核心设计原则

### 1. 恐惧氛围营造
- **视觉**: 昏暗灯光、狭窄视野、突然惊吓
- **听觉**: 环境音效、心跳声、脚步回响
- **心理**: 未知恐惧、孤立感、压迫感

### 2. 资源管理
```
Player {
  health: 生命值
  sanity: 理智值
  battery: 手电电量
  items: 道具栏(有限)
}
```

### 3. AI敌人系统
- 巡逻路线
- 声音感知
- 视线检测
- 追击/搜索行为

## 关键技术实现

### 光照系统
```javascript
// 动态手电筒
class Flashlight {
  constructor() {
    this.battery = 100;
    this.consumption = 0.1; // 每秒消耗
    this.range = 10;
    this.angle = 30;
  }
  
  update(deltaTime) {
    if (this.isOn) {
      this.battery -= this.consumption * deltaTime;
    }
  }
}
```

### 声音系统
```javascript
// 3D音效
class AudioSystem {
  play3D(sound, position, listener) {
    const distance = position.distanceTo(listener);
    const volume = 1 / (1 + distance * 0.1);
    // 立体声计算
  }
}
```

### 惊吓系统
- Jump Scare触发条件
- 渐进式紧张
- 随机事件系统

## 恐惧设计技巧

| 技巧 | 说明 |
|------|------|
| 限制视野 | 手电筒、黑暗区域 |
| 资源稀缺 | 弹药、电池有限 |
| 不可战胜 | 逃跑而非战斗 |
| 环境叙事 | 文件、录音、痕迹 |
| 心理暗示 | 幻觉、记忆扭曲 |

## 关卡设计

- 安全屋设计
- 渐进式难度
- 多路线选择
- 隐藏收集品
