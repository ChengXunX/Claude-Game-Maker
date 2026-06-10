---
name: 放置点击游戏开发模板
description: 放置点击游戏开发模板，适用于点击器、放置类、挂机类游戏
trigger: idle, clicker, 放置, 点击, 挂机, 点击器, idle game
examples: Cookie Clicker|Clicker Heroes|Adventure Capitalist|Egg Inc|Idle Miner
---

# 放置点击游戏开发模板

## 游戏设计核心原则

### 核心循环（持续进行）
```
点击 → 赚钱 → 升级 → 自动赚钱 → 解锁新内容 → 更多升级
```
- **即时反馈**：每次点击都有数字弹出
- **指数增长**：数字从 1 到 100 万到 10 亿
- **离线收益**：关掉游戏也能赚钱

### 玩家心理学
- **"数字变大"快感**：看着数字增长本身就是乐趣
- **"差一点就升级"**：总是差一点，让人继续玩
- **"解锁新内容"**：新功能、新升级激发好奇心
- **"离线收益"**：回来时看到大量收益的惊喜

### 数值设计要点
```
指数增长公式：
收益 = 基础收益 × 升级倍数 × 全局倍数
升级费用 = 基础费用 × 1.15^当前等级

关键：让玩家每 10-30 秒就能买一次升级
```

## 核心系统设计

### 1. 点击系统
```javascript
class ClickSystem {
  constructor() {
    this.baseClickValue = 1;
    this.clickMultiplier = 1;
    this.totalClicks = 0;
  }
  
  click() {
    const value = this.baseClickValue * this.clickMultiplier;
    this.totalClicks++;
    this.addCurrency(value);
    this.showClickEffect(value);
    this.checkAchievements();
  }
  
  showClickEffect(value) {
    const text = this.scene.add.text(
      this.scene.input.x, this.scene.input.y,
      `+${this.formatNumber(value)}`,
      { fontSize: '20px', color: '#ffff00' }
    );
    
    this.scene.tweens.add({
      targets: text,
      y: text.y - 50,
      alpha: 0,
      duration: 1000,
      onComplete: () => text.destroy()
    });
  }
}
```

### 2. 升级系统
```javascript
class UpgradeSystem {
  constructor() {
    this.upgrades = {};
  }
  
  getCost(upgradeId) {
    const upgrade = this.upgrades[upgradeId];
    return Math.floor(upgrade.baseCost * Math.pow(1.15, upgrade.level));
  }
  
  getEffect(upgradeId) {
    const upgrade = this.upgrades[upgradeId];
    return upgrade.baseEffect * upgrade.level;
  }
  
  purchase(upgradeId) {
    const cost = this.getCost(upgradeId);
    if (this.currency >= cost) {
      this.currency -= cost;
      this.upgrades[upgradeId].level++;
      this.recalculateIncome();
      return true;
    }
    return false;
  }
}

const UPGRADES = [
  { id: 'clickPower', name: '点击力量', cost: 10, effect: 1, type: 'click' },
  { id: 'autoClicker', name: '自动点击器', cost: 100, effect: 1, type: 'auto' },
  { id: 'factory', name: '工厂', cost: 1000, effect: 10, type: 'auto' },
  { id: 'bank', name: '银行', cost: 10000, effect: 100, type: 'auto' },
  { id: 'temple', name: '神殿', cost: 100000, effect: 1000, type: 'auto' }
];
```

### 3. 离线收益系统
```javascript
class OfflineSystem {
  constructor() {
    this.lastOnlineTime = Date.now();
    this.offlineMultiplier = 0.5;
  }
  
  calculateOfflineEarnings() {
    const now = Date.now();
    const offlineSeconds = (now - this.lastOnlineTime) / 1000;
    const offlineMinutes = offlineSeconds / 60;
    const maxOfflineMinutes = 24 * 60;
    const effectiveMinutes = Math.min(offlineMinutes, maxOfflineMinutes);
    
    const earningsPerMinute = this.getIncomePerSecond() * 60;
    return Math.floor(earningsPerMinute * effectiveMinutes * this.offlineMultiplier);
  }
  
  onGameLoad() {
    const earnings = this.calculateOfflineEarnings();
    if (earnings > 0) {
      this.showOfflineEarnings(earnings);
      this.addCurrency(earnings);
    }
    this.lastOnlineTime = Date.now();
  }
}
```

### 4. 数字格式化
```javascript
function formatNumber(num) {
  if (num < 1000) return Math.floor(num).toString();
  if (num < 1000000) return (num / 1000).toFixed(1) + 'K';
  if (num < 1000000000) return (num / 1000000).toFixed(1) + 'M';
  if (num < 1000000000000) return (num / 1000000000).toFixed(1) + 'B';
  if (num < 1000000000000000) return (num / 1000000000000).toFixed(1) + 'T';
  return num.toExponential(2);
}
```

## 迭代策略

### 第一版：核心点击
- 点击赚钱
- 1 种升级
- 数字显示
- 基础 UI

### 第二版：升级系统
- 5 种升级
- 自动赚钱
- 升级动画
- 成就系统

### 第三版：离线系统
- 离线收益
- 离线通知
- 数据保存
- 统计系统

### 第四版：深度玩法
- 20 种升级
- 特殊事件
- 每日挑战
- 排行榜

### 第五版：变现
- 看广告获得加速
- 内购系统
- 社交分享
- 推送通知

## 常见错误

1. **增长太慢**：玩家要能看到明显增长，否则会失去兴趣
2. **升级太贵**：每 10-30 秒要能买一次升级
3. **没有离线收益**：玩家回来时要有惊喜
4. **数字太大看不懂**：要格式化大数字（K/M/B/T）
5. **没有反馈**：每次点击都要有视觉和音效反馈
