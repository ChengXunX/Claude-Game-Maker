# 平台跳跃游戏模板

纯 HTML5 + JavaScript 实现的经典平台跳跃游戏，无任何第三方依赖。

## 游戏特性

- **角色控制**：左右移动 + 跳跃
- **平台碰撞**：真实的物理碰撞检测
- **金币收集**：收集所有金币即可通关
- **重力物理**：模拟真实的重力效果
- **分数系统**：收集金币获得分数

## 操作方式

| 按键 | 功能 |
|------|------|
| ← → 或 A D | 左右移动 |
| ↑ 或 W 或 空格 | 跳跃 |
| R | 重新开始 |

## 文件结构

```
html5-platformer/
├── index.html          # 主页面
├── css/
│   └── style.css       # 样式表
├── js/
│   ├── config.js       # 配置常量
│   ├── player.js       # 玩家类
│   ├── level.js        # 关卡类
│   └── game.js         # 游戏主类
└── README.md           # 说明文档
```

## 快速开始

直接在浏览器中打开 `index.html` 即可开始游戏。

## 技术实现

- **渲染**：Canvas 2D API
- **游戏循环**：requestAnimationFrame
- **碰撞检测**：AABB矩形碰撞
- **物理引擎**：自实现重力和跳跃

## 自定义配置

修改 `js/config.js` 中的常量可以调整游戏参数：

```javascript
const GRAVITY = 0.5;        // 重力大小
const JUMP_FORCE = -12;     // 跳跃力度
const PLAYER_SPEED = 5;     // 移动速度
const COIN_SCORE = 10;      // 每个金币分数
```

## 浏览器兼容性

支持所有现代浏览器：
- Chrome 60+
- Firefox 55+
- Safari 11+
- Edge 79+
