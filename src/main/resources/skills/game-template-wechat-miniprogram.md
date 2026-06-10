---
name: 微信小程序游戏开发模板
description: 微信小程序游戏开发模板，适用于微信小游戏、小程序游戏
trigger: wechat, miniprogram, 微信, 小程序, wx, 微信游戏, mobile, 手机
examples: 跳一跳|羊了个羊|欢乐斗地主|开心消消乐|贪吃蛇大作战
---

# 微信小程序游戏开发模板

## 游戏设计核心原则

### 核心循环（每局 30 秒 - 5 分钟）
```
开始 → 操作 → 反馈 → 结束 → 分享 → 再来一局
```
- **即时开始**：打开就能玩，不需要加载
- **社交分享**：分享给好友/群
- **碎片时间**：适合等车、排队时玩

### 玩家心理学
- **"社交攀比"**：和好友比分数
- **"分享炫耀"**：分享高分到朋友圈
- **"再来一局"**：短局制让人不断重玩
- **"解锁成就"**：成就系统激发收集欲

### 微信小游戏特点
```
1. 包体限制：首包 4MB，总包 8MB
2. 性能限制：低端机要流畅
3. 社交特性：好友排行、群排行
4. 分享机制：分享卡片、复活分享
5. 广告变现：激励视频、插屏广告
```

## 核心系统设计

### 1. 微信登录系统
```javascript
class WxLogin {
  static login() {
    return new Promise((resolve, reject) => {
      wx.login({
        success: (res) => {
          if (res.code) {
            // 发送 code 到后端换取 openid
            wx.request({
              url: 'https://api.example.com/login',
              method: 'POST',
              data: { code: res.code },
              success: (response) => {
                resolve(response.data);
              },
              fail: reject
            });
          } else {
            reject(new Error('登录失败'));
          }
        },
        fail: reject
      });
    });
  }
  
  static getUserInfo() {
    return new Promise((resolve, reject) => {
      wx.getUserInfo({
        success: (res) => {
          resolve(res.userInfo);
        },
        fail: reject
      });
    });
  }
}
```

### 2. 微信分享系统
```javascript
class WxShare {
  static share(score, title) {
    // 设置分享内容
    wx.showShareMenu({
      withShareTicket: true,
      menus: ['shareAppMessage', 'shareTimeline']
    });
    
    // 监听分享事件
    wx.onShareAppMessage(() => {
      return {
        title: title || `我在游戏中获得了${score}分，你能超过我吗？`,
        imageUrl: 'https://example.com/share.png',
        query: `score=${score}`
      };
    });
  }
  
  static shareToGroup(score) {
    // 分享到群
    wx.shareAppMessage({
      title: `我在游戏中获得了${score}分，你能超过我吗？`,
      imageUrl: 'https://example.com/share.png',
      success: (res) => {
        if (res.shareTickets && res.shareTickets.length > 0) {
          // 获取群排行数据
          this.getGroupRanking(res.shareTickets[0]);
        }
      }
    });
  }
  
  static getGroupRanking(shareTicket) {
    wx.getGroupCloudStorage({
      shareTicket: shareTicket,
      keyList: ['score'],
      success: (res) => {
        // 处理群排行数据
        const ranking = res.data.map(item => ({
          nickname: item.nickname,
          avatar: item.avatarUrl,
          score: item.KVDataList[0]?.value || 0
        }));
        ranking.sort((a, b) => b.score - a.score);
        return ranking;
      }
    });
  }
}
```

### 3. 微信排行榜
```javascript
class WxRanking {
  static setScore(score) {
    wx.setUserCloudStorage({
      KVDataList: [
        { key: 'score', value: JSON.stringify({ score, timestamp: Date.now() }) }
      ],
      success: () => {
        console.log('分数上传成功');
      }
    });
  }
  
  static getFriendRanking() {
    return new Promise((resolve, reject) => {
      wx.getFriendCloudStorage({
        keyList: ['score'],
        success: (res) => {
          const ranking = res.data.map(item => ({
            nickname: item.nickname,
            avatar: item.avatarUrl,
            score: item.KVDataList[0]?.value ? JSON.parse(item.KVDataList[0].value).score : 0
          }));
          ranking.sort((a, b) => b.score - a.score);
          resolve(ranking);
        },
        fail: reject
      });
    });
  }
}
```

### 4. 微信广告系统
```javascript
class WxAds {
  static showRewardedVideo(adUnitId) {
    return new Promise((resolve, reject) => {
      const rewardedAd = wx.createRewardedVideoAd({ adUnitId });
      
      rewardedAd.onClose((res) => {
        if (res && res.isEnded) {
          // 用户看完广告
          resolve(true);
        } else {
          // 用户中途关闭
          resolve(false);
        }
      });
      
      rewardedAd.onError((err) => {
        reject(err);
      });
      
      rewardedAd.show();
    });
  }
  
  static showInterstitial(adUnitId) {
    const interstitialAd = wx.createInterstitialAd({ adUnitId });
    interstitialAd.show().catch((err) => {
      console.error('插屏广告显示失败', err);
    });
  }
}
```

### 5. 数据存储
```javascript
class WxStorage {
  static save(key, value) {
    wx.setStorageSync(key, JSON.stringify(value));
  }
  
  static load(key) {
    try {
      const value = wx.getStorageSync(key);
      return value ? JSON.parse(value) : null;
    } catch (e) {
      return null;
    }
  }
  
  static saveHighScore(score) {
    const highScore = this.load('highScore') || 0;
    if (score > highScore) {
      this.save('highScore', score);
      WxRanking.setScore(score);
    }
  }
}
```

## 迭代策略

### 第一版：核心玩法
- 基础游戏机制
- 简单计分
- 本地存储
- 基础 UI

### 第二版：社交功能
- 微信登录
- 好友排行
- 分享功能
- 群排行

### 第三版：变现系统
- 激励视频广告
- 插屏广告
- 复活看广告
- 道具看广告

### 第四版：深度玩法
- 成就系统
- 每日挑战
- 特殊事件
- 排行榜赛季

### 第五版：优化
- 性能优化
- 包体优化
- 低端机适配
- 用户体验优化

## 常见错误

1. **包体太大**：首包 4MB 限制，要压缩资源
2. **性能太差**：低端机要流畅，要优化渲染
3. **没有分享**：微信游戏的核心是社交分享
4. **没有排行**：好友排行是核心驱动力
5. **广告太多**：广告要适度，不能影响体验
