---
name: 模拟赛车游戏开发模板
description: 模拟赛车游戏开发模板，适用于真实赛车模拟、驾驶模拟类游戏
trigger: 赛车模拟,驾驶模拟,racing sim,模拟驾驶,赛车游戏
examples: 极限竞速|GT赛车|尘埃拉力赛|欧洲卡车模拟|神力科莎
---

# 模拟赛车游戏开发模板

## 核心系统设计

### 1. 车辆物理
```
Vehicle {
  mass: 质量(kg)
  engine: {
    power: 功率(HP)
    torque: 扭矩(Nm)
    rpm: 转速
    gearRatios: [3.5, 2.5, 1.8, 1.3, 1.0, 0.8]
  }
  tires: {
    grip: 抓地力
    wear: 磨损度
    temperature: 温度
  }
  suspension: 悬挂参数
}
```

### 2. 轮胎模型
```javascript
function calculateTireForce(slipRatio, normalLoad, tireTemp) {
  const peakGrip = 1.0;
  const tempFactor = Math.max(0, 1 - Math.abs(tireTemp - 80) / 50);
  
  // Pacejka魔术公式简化版
  const force = normalLoad * peakGrip * Math.sin(1.9 * Math.atan(slipRatio * 10)) * tempFactor;
  return force;
}
```

### 3. 空气动力学
| 参数 | 影响 |
|------|------|
| 下压力 | 高速过弯稳定性 |
| 风阻 | 极速影响 |
| 升力 | 高速稳定性 |

## 关键技术实现

### 物理引擎
```javascript
class CarPhysics {
  update(dt, inputs) {
    // 1. 计算引擎力
    const engineForce = this.calculateEngineForce(inputs.throttle);
    
    // 2. 计算轮胎力
    const tireForce = this.calculateTireForces(inputs.steer);
    
    // 3. 计算空气动力
    const aeroForce = this.calculateAeroForce();
    
    // 4. 更新速度和位置
    this.velocity += (engineForce + tireForce + aeroForce) / this.mass * dt;
    this.position += this.velocity * dt;
  }
}
```

### 碰撞系统
- 车辆碰撞
- 赛道边界
- 损坏系统

## 游戏模式

| 模式 | 说明 |
|------|------|
| 职业模式 | 从新手到冠军 |
| 计时赛 | 刷新最快圈速 |
| 在线对战 | 多人竞赛 |
| 自由驾驶 | 练习赛道 |

## 车辆调校

- 胎压调整
- 悬挂高度
- 齿比设置
- 下压力大小
- 刹车平衡
