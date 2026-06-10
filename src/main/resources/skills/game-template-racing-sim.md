---
name: 模拟赛车游戏开发模板
description: 模拟赛车游戏开发模板，适用于真实赛车模拟、驾驶模拟类游戏
trigger: 赛车模拟,驾驶模拟,racing sim,模拟驾驶,赛车游戏,真实驾驶
examples: 极限竞速|GT赛车|尘埃拉力赛|欧洲卡车模拟|神力科莎
---

# 模拟赛车游戏开发模板

## 游戏设计核心原则

### 核心循环（每局 5-30 分钟）
```
选择车辆 → 调校设置 → 熟悉赛道 → 精确驾驶 → 刷新圈速
```
- **真实性**：车辆物理要真实
- **精确性**：每个操作都要有影响
- **成长性**：从新手到高手的渐进

### 玩家心理学
- **"掌控感"**：精确控制车辆的满足感
- **"突破自我"**：刷新圈速的成就感
- **"调校乐趣"**：优化车辆设置的策略感
- **"真实感"**：仿佛在开真车的沉浸感

### 物理模拟要点
```
真实赛车物理：
1. 引擎特性（扭矩曲线、转速限制）
2. 轮胎物理（抓地力、滑移率）
3. 空气动力学（下压力、阻力）
4. 悬挂系统（弹簧、阻尼）
5. 重量转移（加速/刹车/转弯时的重心变化）
```

## 核心系统设计

### 1. 车辆物理引擎
```javascript
class VehiclePhysics {
  constructor(config) {
    this.mass = config.mass; // kg
    this.engine = {
      power: config.power, // HP
      torque: config.torque, // Nm
      rpm: 0,
      maxRpm: config.maxRpm,
      gearRatios: config.gearRatios,
      currentGear: 0
    };
    this.wheels = {
      grip: config.grip,
      slipAngle: 0,
      slipRatio: 0
    };
    this.aero = {
      downforce: config.downforce,
      drag: config.drag
    };
  }
  
  update(inputs, delta) {
    // 引擎计算
    this.updateEngine(inputs.throttle, delta);
    
    // 轮胎力计算
    const wheelForce = this.calculateWheelForce(inputs);
    
    // 空气动力学
    const aeroForce = this.calculateAeroForce();
    
    // 重量转移
    this.calculateWeightTransfer(inputs);
    
    // 更新速度和位置
    this.updateVelocity(wheelForce, aeroForce, delta);
    this.updatePosition(delta);
  }
  
  updateEngine(throttle, delta) {
    // 扭矩曲线
    const rpmRatio = this.engine.rpm / this.engine.maxRpm;
    const torqueMultiplier = this.getTorqueCurve(rpmRatio);
    const torque = this.engine.torque * torqueMultiplier * throttle;
    
    // 更新转速
    this.engine.rpm += (torque / this.mass) * delta * 100;
    this.engine.rpm = Math.min(this.engine.rpm, this.engine.maxRpm);
    
    // 自动换挡
    if (this.engine.rpm > this.engine.maxRpm * 0.95) {
      this.shiftUp();
    } else if (this.engine.rpm < this.engine.maxRpm * 0.3) {
      this.shiftDown();
    }
  }
  
  calculateWheelForce(inputs) {
    const grip = this.wheels.grip;
    const slipAngle = this.wheels.slipAngle;
    const slipRatio = this.wheels.slipRatio;
    
    // 横向力（转向）
    const lateralForce = -slipAngle * grip * this.mass;
    
    // 纵向力（加速/刹车）
    const longitudinalForce = slipRatio * grip * this.mass;
    
    return { lateral: lateralForce, longitudinal: longitudinalForce };
  }
}
```

### 2. 赛道数据
```javascript
class TrackData {
  constructor(config) {
    this.name = config.name;
    this.length = config.length; // 米
    this.curves = config.curves; // 弯道数据
    this.elevation = config.elevation; // 坡度数据
    this.sectorCount = config.sectorCount;
  }
  
  getCurvatureAt(distance) {
    // 根据距离返回赛道曲率
    for (const curve of this.curves) {
      if (distance >= curve.start && distance <= curve.end) {
        return curve.curvature;
      }
    }
    return 0; // 直道
  }
  
  getElevationAt(distance) {
    // 根据距离返回坡度
    for (const elev of this.elevation) {
      if (distance >= elev.start && distance <= elev.end) {
        return elev.slope;
      }
    }
    return 0;
  }
}
```

### 3. 车辆调校系统
```javascript
class VehicleSetup {
  constructor() {
    this.settings = {
      tirePressure: { front: 2.0, rear: 2.0 }, // bar
      suspension: { front: 50, rear: 50 }, // 硬度
      gearRatios: [3.5, 2.5, 1.8, 1.3, 1.0, 0.8],
      downforce: { front: 50, rear: 50 }, // 下压力
      brakeBias: 0.6 // 制动力分配
    };
  }
  
  getEffect(setting, value) {
    // 返回设置对车辆性能的影响
    const effects = {
      tirePressure: {
        grip: -value * 0.1 + 0.2,
        wear: value * 0.05
      },
      suspension: {
        stability: value * 0.01,
        comfort: -value * 0.005
      },
      downforce: {
        grip: value * 0.01,
        topSpeed: -value * 0.005
      }
    };
    return effects[setting] || {};
  }
}
```

### 4. 圈速系统
```javascript
class LapTimer {
  constructor() {
    this.currentLap = 0;
    this.lapTimes = [];
    this.sectorTimes = [];
    this.lapStartTime = 0;
    this.sectorStartTime = 0;
  }
  
  startLap() {
    this.currentLap++;
    this.lapStartTime = Date.now();
    this.sectorStartTime = Date.now();
  }
  
  endSector(sector) {
    const now = Date.now();
    const sectorTime = now - this.sectorStartTime;
    this.sectorTimes.push({ lap: this.currentLap, sector, time: sectorTime });
    this.sectorStartTime = now;
    return sectorTime;
  }
  
  endLap() {
    const now = Date.now();
    const lapTime = now - this.lapStartTime;
    this.lapTimes.push({ lap: this.currentLap, time: lapTime });
    return lapTime;
  }
  
  getBestLap() {
    if (this.lapTimes.length === 0) return null;
    return this.lapTimes.reduce((best, lap) => 
      lap.time < best.time ? lap : best
    );
  }
}
```

## 迭代策略

### 第一版：基础物理
- 车辆物理引擎
- 简单赛道
- 圈速计时
- 基础 UI

### 第二版：真实物理
- 轮胎物理
- 空气动力学
- 重量转移
- 多条赛道

### 第三版：调校系统
- 车辆调校
- 调校效果
- 调校保存
- 调校分享

### 第四版：内容扩展
- 10 辆车
- 10 条赛道
- 赛事系统
- 成就系统

### 第五版：多人对战
- 多人联机
- 排位赛
- 赛季系统
- 排行榜

## 常见错误

1. **物理太假**：模拟赛车的核心是真实物理
2. **操控太难**：要有辅助系统帮助新手
3. **没有反馈**：车辆状态要可视化（轮胎温度、引擎转速）
4. **赛道太少**：需要多条赛道保持新鲜感
5. **没有调校**：调校是模拟赛车的核心乐趣
