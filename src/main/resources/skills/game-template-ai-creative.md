---
name: AI创意工坊游戏开发模板
description: AI创意工坊游戏开发模板，适用于AI绘画、AI音乐、AI故事生成类游戏
trigger: AI创作, 绘画, 音乐, 故事, creative, art, music, story, 生成, AI绘画
examples: AI绘画|AI音乐|AI故事|Midjourney|Suno|ChatGPT
---

# AI 创意工坊游戏开发模板

## 游戏设计核心原则

### 核心循环（每 5-15 分钟一轮）
```
输入创意 → AI 生成 → 调整优化 → 保存分享 → 收集展示
```
- **创意表达**：玩家可以自由创作
- **AI 辅助**：AI 帮助实现创意
- **分享展示**：展示自己的作品

### 玩家心理学
- **"创意实现"的满足感**：把想法变成现实
- **"AI 惊喜"的意外感**：AI 生成的结果超出预期
- **"分享炫耀"的欲望**：展示自己的作品
- **"收集完整"的成就感**：收集不同风格的作品

### AI 创意设计要点
```
AI 创意核心：
1. 简单输入：简单的描述就能生成
2. 多样风格：支持多种风格
3. 可调参数：可以调整生成结果
4. 快速生成：几秒钟就能出结果
```

## 核心系统设计

### 1. AI 绘画系统
```javascript
class AIPaintingSystem {
  constructor() {
    this.apiEndpoint = 'https://api.example.com/generate';
    this.styles = ['写实', '动漫', '油画', '水彩', '像素'];
  }
  
  async generate(prompt, style) {
    const response = await fetch(this.apiEndpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ prompt, style })
    });
    
    const result = await response.json();
    return result.imageUrl;
  }
  
  async refine(imageId, adjustments) {
    // 调整生成结果
    const response = await fetch(`${this.apiEndpoint}/refine`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ imageId, adjustments })
    });
    
    const result = await response.json();
    return result.imageUrl;
  }
  
  getStyles() {
    return this.styles;
  }
}
```

### 2. AI 音乐系统
```javascript
class AIMusicSystem {
  constructor() {
    this.apiEndpoint = 'https://api.example.com/music';
    this.genres = ['流行', '摇滚', '古典', '电子', '爵士'];
    this.moods = ['快乐', '悲伤', '紧张', '平静', '激昂'];
  }
  
  async generate(genre, mood, duration) {
    const response = await fetch(this.apiEndpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ genre, mood, duration })
    });
    
    const result = await response.json();
    return result.audioUrl;
  }
  
  async remix(audioId, adjustments) {
    const response = await fetch(`${this.apiEndpoint}/remix`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ audioId, adjustments })
    });
    
    const result = await response.json();
    return result.audioUrl;
  }
  
  getGenres() {
    return this.genres;
  }
  
  getMoods() {
    return this.moods;
  }
}
```

### 3. AI 故事系统
```javascript
class AIStorySystem {
  constructor() {
    this.apiEndpoint = 'https://api.example.com/story';
    this.genres = ['奇幻', '科幻', '悬疑', '爱情', '冒险'];
  }
  
  async generate(genre, theme, length) {
    const response = await fetch(this.apiEndpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ genre, theme, length })
    });
    
    const result = await response.json();
    return result.story;
  }
  
  async continue(storyId, direction) {
    const response = await fetch(`${this.apiEndpoint}/continue`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ storyId, direction })
    });
    
    const result = await response.json();
    return result.story;
  }
  
  async generateEnding(storyId) {
    const response = await fetch(`${this.apiEndpoint}/ending`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ storyId })
    });
    
    const result = await response.json();
    return result.ending;
  }
  
  getGenres() {
    return this.genres;
  }
}
```

### 4. 作品管理系统
```javascript
class GallerySystem {
  constructor() {
    this.works = [];
    this.collections = [];
  }
  
  addWork(work) {
    this.works.push({
      id: this.generateId(),
      type: work.type, // painting, music, story
      title: work.title,
      content: work.content,
      creator: work.creator,
      createdAt: Date.now(),
      likes: 0,
      views: 0
    });
  }
  
  likeWork(workId) {
    const work = this.works.find(w => w.id === workId);
    if (work) {
      work.likes++;
    }
  }
  
  viewWork(workId) {
    const work = this.works.find(w => w.id === workId);
    if (work) {
      work.views++;
    }
  }
  
  getPopularWorks(limit = 10) {
    return this.works
      .sort((a, b) => b.likes - a.likes)
      .slice(0, limit);
  }
  
  createCollection(name) {
    this.collections.push({
      id: this.generateId(),
      name,
      works: []
    });
  }
  
  addToCollection(collectionId, workId) {
    const collection = this.collections.find(c => c.id === collectionId);
    if (collection) {
      collection.works.push(workId);
    }
  }
}
```

### 5. 分享系统
```javascript
class ShareSystem {
  constructor() {
    this.platforms = ['微信', '微博', 'QQ', '保存本地'];
  }
  
  async share(workId, platform) {
    const work = this.getWork(workId);
    
    switch (platform) {
      case '微信':
        return this.shareToWechat(work);
      case '微博':
        return this.shareToWeibo(work);
      case 'QQ':
        return this.shareToQQ(work);
      case '保存本地':
        return this.saveToLocal(work);
    }
  }
  
  async shareToWechat(work) {
    // 分享到微信
    const shareData = {
      title: work.title,
      text: `我用 AI 创作了"${work.title}"，快来看看吧！`,
      url: `https://example.com/work/${work.id}`
    };
    
    if (navigator.share) {
      await navigator.share(shareData);
    }
  }
  
  async saveToLocal(work) {
    // 保存到本地
    const blob = await this.generateBlob(work);
    const url = URL.createObjectURL(blob);
    
    const a = document.createElement('a');
    a.href = url;
    a.download = `${work.title}.${work.type === 'music' ? 'mp3' : 'png'}`;
    a.click();
    
    URL.revokeObjectURL(url);
  }
}
```

## 迭代策略

### 第一版：基础创作
- AI 绘画
- 简单输入
- 基础 UI
- 本地保存

### 第二版：多样风格
- 多种风格
- 参数调整
- 作品管理
- 分享功能

### 第三版：音乐创作
- AI 音乐
- 多种曲风
- 混音功能
- 作品展示

### 第四版：故事创作
- AI 故事
- 多种题材
- 分支剧情
- 作品分享

### 第五版：社区功能
- 作品社区
- 点赞评论
- 排行榜
- 创作挑战

## 常见错误

1. **生成太慢**：要在几秒内出结果
2. **结果太差**：AI 生成质量要高
3. **不可调**：要能调整生成结果
4. **没有分享**：要能分享作品
5. **没有社区**：要有作品展示社区
