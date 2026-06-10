---
name: 经营养成游戏开发模板
description: 经营养成游戏开发模板，适用于店铺经营、商业模拟、养成类游戏
trigger: idle, 放置, tycoon, 经营, 养成, 数值, 店铺, 商业, 经理, manager
examples: 开罗游戏|模拟城市|大富翁|商业大亨|Idle Miner
---

# 经营养成游戏开发模板

## 游戏设计核心原则

### 核心循环（每 5-15 分钟一轮）
```
经营店铺 → 赚取金币 → 升级店铺 → 解锁新店铺 → 扩大规模
```
- **成长感**：从一个小摊位到商业帝国
- **策略感**：选择升级哪个店铺、雇佣哪个员工
- **自动化**：后期可以挂机自动赚钱

### 玩家心理学
- **"从零到一"的成就感**：看着自己的商业帝国成长
- **"优化策略"的乐趣**：找到最优的升级路径
- **"解锁新内容"的期待**：新店铺、新员工、新功能
- **"数值增长"的快感**：收入从 100 到 100 万

### 经济系统设计
```
收入 = 店铺基础收入 × 等级倍数 × 员工加成 × 全局加成
升级费用 = 基础费用 × 1.12^当前等级
员工费用 = 基础费用 × 1.2^当前等级

关键：让玩家每 5-15 分钟就能做一次有意义的升级
```

## 核心系统设计

### 1. 店铺系统
```javascript
class Shop {
  constructor(config) {
    this.id = config.id;
    this.name = config.name;
    this.baseIncome = config.baseIncome;
    this.level = 1;
    this.baseCost = config.baseCost;
    this.employees = [];
    this.lastCollectTime = Date.now();
  }
  
  getIncome() {
    let income = this.baseIncome * this.level;
    
    // 员工加成
    for (const employee of this.employees) {
      income *= employee.getBonus();
    }
    
    return income;
  }
  
  getUpgradeCost() {
    return Math.floor(this.baseCost * Math.pow(1.12, this.level));
  }
  
  upgrade() {
    const cost = this.getUpgradeCost();
    if (this.currency >= cost) {
      this.currency -= cost;
      this.level++;
      return true;
    }
    return false;
  }
  
  collectEarnings() {
    const now = Date.now();
    const elapsed = (now - this.lastCollectTime) / 1000;
    const earnings = this.getIncome() * elapsed;
    this.lastCollectTime = now;
    return Math.floor(earnings);
  }
}

const SHOPS = [
  { id: 'lemonade', name: '柠檬水摊', baseIncome: 1, baseCost: 10 },
  { id: 'bakery', name: '面包店', baseIncome: 5, baseCost: 100 },
  { id: 'restaurant', name: '餐厅', baseIncome: 25, baseCost: 1000 },
  { id: 'hotel', name: '酒店', baseIncome: 100, baseCost: 10000 },
  { id: 'mall', name: '购物中心', baseIncome: 500, baseCost: 100000 }
];
```

### 2. 员工系统
```javascript
class Employee {
  constructor(config) {
    this.id = config.id;
    this.name = config.name;
    this.role = config.role;
    this.level = 1;
    this.baseBonus = config.baseBonus;
    this.salary = config.salary;
  }
  
  getBonus() {
    return 1 + (this.baseBonus * this.level / 100);
  }
  
  upgrade() {
    this.level++;
    this.salary = Math.floor(this.salary * 1.1);
  }
}

const EMPLOYEES = [
  { id: 'cashier', name: '收银员', role: 'income', baseBonus: 10, salary: 5 },
  { id: 'manager', name: '经理', role: 'speed', baseBonus: 20, salary: 20 },
  { id: 'accountant', name: '会计', role: 'cost', baseBonus: 15, salary: 15 }
];
```

### 3. 离线收益系统
```javascript
class OfflineEarnings {
  calculate(shops, lastOnlineTime) {
    const now = Date.now();
    const offlineHours = (now - lastOnlineTime) / (1000 * 60 * 60);
    const maxHours = 24;
    const effectiveHours = Math.min(offlineHours, maxHours);
    
    let totalEarnings = 0;
    for (const shop of shops) {
      totalEarnings += shop.getIncome() * effectiveHours * 3600 * 0.5;
    }
    
    return Math.floor(totalEarnings);
  }
}
```

### 4. 成就系统
```javascript
const ACHIEVEMENTS = [
  { id: 'first_shop', name: '第一桶金', condition: (state) => state.totalEarnings >= 100 },
  { id: 'shop_5', name: '连锁经营', condition: (state) => state.shopCount >= 5 },
  { id: 'millionaire', name: '百万富翁', condition: (state) => state.totalEarnings >= 1000000 },
  { id: 'employees_10', name: '团队领袖', condition: (state) => state.employeeCount >= 10 }
];
```

## 迭代策略

### 第一版：基础经营
- 1 种店铺
- 升级系统
- 手动收集收益
- 基础 UI

### 第二版：多店铺
- 5 种店铺
- 店铺解锁
- 自动收集
- 成就系统

### 第三版：员工系统
- 员工招聘
- 员工升级
- 员工技能
- 离线收益

### 第四版：深度玩法
- 20 种店铺
- 特殊事件
- 每日任务
- 排行榜

### 第五版：变现
- 看广告获得加速
- 内购系统
- 社交分享
- 推送通知

## 常见错误

1. **升级太慢**：玩家要能频繁做有意义的决策
2. **店铺没有差异**：每种店铺要有独特特点
3. **离线收益太少**：玩家回来时要有惊喜
4. **没有自动化**：后期要能挂机，否则太累
5. **数值不平衡**：某些升级太强或太弱
