---
name: 经营模拟游戏开发模板
description: 经营模拟游戏开发模板，适用于主题公园、餐厅、医院等经营类游戏
trigger: 经营游戏,模拟经营,tycoon game,主题公园,餐厅经营
examples: 过山车大亨|城市天际线|模拟医院|美食家餐厅|江南百景图
---

# 经营模拟游戏开发模板

## 核心系统设计

### 1. 经济系统
```
Business {
  funds: 资金
  income: 收入(每周期)
  expenses: 支出(每周期)
  reputation: 声望(影响客流量)
}
```

### 2. 客户系统
| 属性 | 说明 |
|------|------|
| 满意度 | 0-100，影响回头率 |
| 消费力 | 决定消费金额
| 等待耐心 | 排队容忍度 |
| 需求 | 想要的服务/商品 |

### 3. 员工系统
```javascript
const employee = {
  name: '员工名',
  skill: 50,  // 技能等级
  salary: 1000,  // 薪资
  morale: 80,  // 士气
  position: '厨师'  // 职位
}
```

## 关键技术实现

### 模拟循环
```javascript
function simulateDay(business) {
  // 1. 生成客户
  const customers = generateCustomers(business.reputation);
  
  // 2. 处理服务
  customers.forEach(customer => {
    const service = findService(customer.needs);
    if (service) {
      customer.satisfaction = calculateSatisfaction(service);
      business.income += customer.spend();
    }
  });
  
  // 3. 计算支出
  business.expenses = calculateExpenses(business.employees);
  
  // 4. 更新资金
  business.funds += business.income - business.expenses;
}
```

### 建筑系统
- 建筑放置
- 升级系统
- 布局优化
- 装饰加成

## 经营策略

| 策略 | 效果 |
|------|------|
| 提价 | 收入增加，客户减少 |
| 降价 | 客户增加，收入减少 |
| 装修 | 声望提升，成本增加 |
| 培训 | 员工效率提升 |

## 扩展系统

- 连锁经营
- 解锁新地图
- 特殊事件
- 季节活动
- 成就系统
