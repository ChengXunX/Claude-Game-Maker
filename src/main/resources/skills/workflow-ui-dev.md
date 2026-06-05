---
name: UI开发工作流技能
description: UI开发Agent的专业工作流程技能，优化前端和界面开发效率
trigger: UI开发,ui-dev,前端开发,界面设计,视觉开发
examples: 组件开发|页面布局|样式优化|响应式设计|动画效果
---

# UI开发工作流技能

## 核心职责

作为UI开发，你负责：
1. 界面设计与实现
2. 组件开发与维护
3. 用户体验优化
4. 视觉效果实现

## 设计工作流程

### 1. 设计评审流程
```
[设计评审清单]
□ 理解设计需求
□ 确认交互规范
□ 评估技术可行性
□ 识别实现难点
□ 制定开发计划
```

### 2. 组件开发规范
```markdown
## 组件设计模板

### 组件名称: [ComponentName]
- **用途**: [组件用途说明]
- **父组件**: [父组件名称]
- **子组件**: [子组件列表]

### Props定义
| 属性名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| prop1 | String | '' | 属性说明 |
| prop2 | Number | 0 | 属性说明 |
| prop3 | Boolean | false | 属性说明 |

### Events定义
| 事件名 | 参数 | 说明 |
|--------|------|------|
| change | value | 值变化时触发 |
| click | event | 点击时触发 |

### 使用示例
```vue
<template>
  <ComponentName
    prop1="value"
    :prop2="123"
    :prop3="true"
    @change="handleChange"
  />
</template>
```
```

### 3. 样式规范
```css
/* 命名规范：BEM */
.block {}
.block__element {}
.block--modifier {}

/* 颜色变量 */
:root {
  --primary-color: #409EFF;
  --success-color: #67C23A;
  --warning-color: #E6A23C;
  --danger-color: #F56C6C;
  --info-color: #909399;
}

/* 间距系统 */
--spacing-xs: 4px;
--spacing-sm: 8px;
--spacing-md: 16px;
--spacing-lg: 24px;
--spacing-xl: 32px;
```

## 响应式设计

### 断点定义
| 断点 | 宽度 | 设备 |
|------|------|------|
| xs | < 576px | 手机 |
| sm | ≥ 576px | 大手机/小平板 |
| md | ≥ 768px | 平板 |
| lg | ≥ 992px | 小桌面 |
| xl | ≥ 1200px | 桌面 |
| xxl | ≥ 1600px | 大桌面 |

### 响应式工具类
```css
/* 显示隐藏 */
.hidden-xs { display: block; }
.visible-xs { display: none; }

@media (max-width: 575px) {
  .hidden-xs { display: none; }
  .visible-xs { display: block; }
}
```

## 动画规范

### 过渡动画
```css
/* 基础过渡 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
```

### 性能优化
- 使用 transform 代替 top/left
- 使用 opacity 代替 visibility
- 避免触发重排的属性
- 使用 will-change 提示浏览器

## 可访问性规范

### 键盘导航
- Tab 键顺序合理
- Enter/Space 触发操作
- Escape 关闭弹窗

### 屏幕阅读器
- 语义化HTML标签
- aria-* 属性
- alt 文本描述

## 性能检查清单

```
[性能检查清单]
□ 图片懒加载
□ 组件按需加载
□ CSS/JS压缩
□ 缓存策略
□ 减少重绘重排
□ 虚拟滚动（长列表）
```
