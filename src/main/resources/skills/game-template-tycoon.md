---
name: 经营模拟游戏开发模板
description: 经营模拟游戏开发模板，适用于主题公园、餐厅、医院等经营类游戏
trigger: 经营游戏,模拟经营,tycoon game,主题公园,餐厅经营,医院经营
examples: 过山车大亨|城市天际线|模拟医院|美食家餐厅|江南百景图|Two Point Hospital
---

# 经营模拟游戏开发模板

## 游戏设计核心原则

### 核心循环（每 5-15 分钟一轮）
```
建造设施 → 吸引顾客 → 赚取收入 → 升级设施 → 扩大规模
```
- **经营策略**：选择建造什么、在哪里建造
- **资源管理**：资金、人力、空间的平衡
- **成长感**：从小店到大企业的成长

### 玩家心理学
- **"从零到一"的成就感**：看着自己的企业成长
- **"优化策略"的乐趣**：找到最优的经营方式
- **"满足需求"的责任感**：满足顾客/患者的需求
- **"解锁新内容"的期待感**：新设施、新功能

### 经济系统设计
```
收入 = 顾客数 × 客单价 × 满意度
支出 = 员工工资 + 设施维护 + 租金
利润 = 收入 - 支出

关键：让玩家每 5-15 分钟就能做一次有意义的升级
```

## 核心系统设计

### 1. 设施系统
```javascript
class Facility {
  constructor(config) {
    this.id = config.id;
    this.type = config.type;
    this.name = config.name;
    this.size = config.size;
    this.cost = config.cost;
    this.capacity = config.capacity;
    this.level = 1;
    this.x = 0;
    this.y = 0;
  }
  
  getIncome() {
    return this.baseIncome * this.level;
  }
  
  getMaintenanceCost() {
    return this.baseMaintenance * this.level;
  }
  
  upgrade() {
    const cost = this.getUpgradeCost();
    if (this.funds >= cost) {
      this.funds -= cost;
      this.level++;
      return true;
    }
    return false;
  }
}

const FACILITIES = {
  // 主题公园
  rollerCoaster: { name: '过山车', capacity: 20, income: 100, maintenance: 20 },
  ferrisWheel: { name: '摩天轮', capacity: 30, income: 80, maintenance: 15 },
  foodStand: { name: '小吃摊', capacity: 10, income: 50, maintenance: 5 },
  
  // 餐厅
  kitchen: { name: '厨房', capacity: 5, income: 200, maintenance: 30 },
  diningArea: { name: '用餐区', capacity: 20, income: 0, maintenance: 10 },
  
  // 医院
  reception: { name: '前台', capacity: 5, income: 0, maintenance: 5 },
  doctorOffice: { name: '诊室', capacity: 1, income: 100, maintenance: 20 },
  pharmacy: { name: '药房', capacity: 3, income: 50, maintenance: 10 }
};
```

### 2. 顾客/患者系统
```javascript
class Customer {
  constructor(config) {
    this.id = config.id;
    this.type = config.type;
    this.needs = config.needs;
    this.patience = config.patience;
    this.satisfaction = 50;
    this.x = 0;
    this.y = 0;
  }
  
  update(delta) {
    // 等待降低耐心
    if (this.waiting) {
      this.patience -= 0.01 * delta;
    }
    
    // 满意度影响消费
    if (this.satisfaction >= 80) {
      this.tip = 0.2; // 20% 小费
    } else if (this.satisfaction >= 60) {
      this.tip = 0.1;
    } else {
      this.tip = 0;
    }
    
    // 耐心为 0 时离开
    if (this.patience <= 0) {
      this.leave();
    }
  }
  
  serve(facility) {
    this.satisfaction = Math.min(100, this.satisfaction + 20);
    this.waiting = false;
  }
  
  leave() {
    if (this.satisfaction < 50) {
      // 差评
      this.reputation -= 1;
    }
  }
}
```

### 3. 员工系统
```javascript
class Employee {
  constructor(config) {
    this.id = config.id;
    this.name = config.name;
    this.role = config.role;
    this.skill = config.skill;
    this.salary = config.salary;
    this.morale = 100;
    this.x = 0;
    this.y = 0;
  }
  
  work(delta) {
    // 工作降低士气
    this.morale -= 0.005 * delta;
    
    // 技能影响效率
    const efficiency = this.skill / 100;
    return efficiency;
  }
  
  rest() {
    this.morale = Math.min(100, this.morale + 10);
  }
  
  paySalary() {
    return this.salary;
  }
}

const EMPLOYEE_ROLES = {
  // 主题公园
  operator: { name: '操作员', salary: 50, skill: 80 },
  cleaner: { name: '清洁工', salary: 30, skill: 70 },
  
  // 餐厅
  chef: { name: '厨师', salary: 80, skill: 85 },
  waiter: { name: '服务员', salary: 40, skill: 75 },
  
  // 医院
  doctor: { name: '医生', salary: 100, skill: 90 },
  nurse: { name: '护士', salary: 60, skill: 80 }
};
```

### 4. 声望系统
```javascript
class ReputationSystem {
  constructor() {
    this.reputation = 50; // 0-100
    this.reviews = [];
  }
  
  addReview(review) {
    this.reviews.push(review);
    this.recalculate();
  }
  
  recalculate() {
    if (this.reviews.length === 0) return;
    
    const total = this.reviews.reduce((sum, r) => sum + r.rating, 0);
    this.reputation = Math.floor(total / this.reviews.length);
  }
  
  getCustomerMultiplier() {
    // 声望影响客流量
    return 0.5 + (this.reputation / 100) * 1.5;
  }
}
```

## 迭代策略

### 第一版：基础经营
- 1 种设施
- 基础经济系统
- 简单 UI
- 计时系统

### 第二版：多种设施
- 5 种设施
- 设施升级
- 顾客系统
- 员工系统

### 第三版：深度经营
- 10 种设施
- 声望系统
- 任务系统
- 成就系统

### 第四版：扩展玩法
- 20 种设施
- 天气系统
- 特殊事件
- 排行榜

### 第五版：变现
- 看广告获得加速
- 内购系统
- 社交分享
- 推送通知

## 常见错误

1. **收入太低**：玩家要能频繁升级，否则会失去兴趣
2. **设施没有差异**：每种设施要有独特特点
3. **没有反馈**：顾客要有表情和对话
4. **没有目标**：要有任务引导玩家
5. **经济不平衡**：收入和支出要平衡
