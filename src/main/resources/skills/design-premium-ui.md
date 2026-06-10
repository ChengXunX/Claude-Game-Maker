---
name: 高端UI设计
description: 创建高端质感的Vue3+Element Plus界面，禁止AI通用设计模板，强调设计系统、排版层次、微动效
trigger: 高端UI,premium,设计优化,界面美化,UI升级,好看的设计
examples: 设计一个高端的登录页面|优化这个页面的视觉效果|让界面更有质感
---

# 高端 UI 设计技能（Vue 3 + Element Plus）

> 适用于：登录页、着陆页、个人中心、设置页等需要视觉品质的页面
> 不适用于：数据表格密集的管理后台、纯功能性的表单页

## 0. 设计意图推断

在动手之前，先推断用户想要什么：

1. **页面类型** - 登录/注册、着陆页、个人中心、设置页、空状态页
2. **情绪词** - "高端"、"简洁"、"科技感"、"游戏风"、"专业"、"温馨"
3. **目标用户** - 游戏开发者、项目经理、管理者

输出一行设计判断：**"设计意图：为\<用户>设计的\<页面类型>，风格偏向\<方向>"**

## 1. 绝对禁止项

以下任何一条出现，设计即为失败：

- **禁止字体**：直接使用浏览器默认字体，不声明 font-family
- **禁止配色**：大面积使用 Element Plus 默认蓝色 `#409EFF` 作为背景
- **禁止布局**：所有内容居中、三列等分卡片、无层次的平铺
- **禁止阴影**：使用默认的 `el-card` 阴影不做任何定制
- **禁止圆角**：所有元素都是相同的 `border-radius: 4px`
- **禁止留白**：内容挤在一起，没有呼吸感
- **禁止渐变**：AI 常用的紫蓝渐变背景（#667eea → #764ba2）
- **禁止动画**：无意义的无限循环动画、loading 旋转装饰

## 2. 色彩系统

### 深色主题（推荐用于游戏开发平台）
```
背景层：#0a0a0f（主背景）→ #12121a（卡片）→ #1a1a2e（悬浮态）
文字层：#e8e8e8（主文字）→ #a0a0a8（次要文字）→ #6b6b78（占位符）
强调色：选一个，只用一个
  - 科技蓝：#3b82f6（适合工具类）
  - 翡翠绿：#10b981（适合状态类）
  - 琥珀金：#f59e0b（适合游戏类）
分割线：rgba(255,255,255,0.06)
```

### 浅色主题
```
背景层：#fafafa（主背景）→ #ffffff（卡片）→ #f5f5f5（次级区域）
文字层：#111111（主文字）→ #666666（次要文字）→ #999999（占位符）
强调色：同上，但饱和度降低 10%
分割线：rgba(0,0,0,0.06)
```

## 3. 排版层次

### 字体选择
```css
/* 主字体 - 界面文字 */
font-family: 'Inter', 'PingFang SC', 'Microsoft YaHei', sans-serif;

/* 等宽字体 - 代码、数据、ID */
font-family: 'JetBrains Mono', 'SF Mono', 'Fira Code', monospace;

/* 标题字体 - 大标题（可选） */
font-family: 'Plus Jakarta Sans', 'Noto Sans SC', sans-serif;
```

### 层次规范
```
页面标题：28-32px / font-weight: 700 / letter-spacing: -0.02em
区块标题：18-20px / font-weight: 600
正文内容：14px / font-weight: 400 / line-height: 1.6
辅助文字：12-13px / font-weight: 400 / color: 次要文字色
数据数字：使用等宽字体 / font-variant-numeric: tabular-nums
```

## 4. 空间节奏

### 间距系统（8px 基准）
```
紧凑间距：8px（图标与文字、同组元素）
标准间距：16px（卡片内边距、表单项间距）
宽松间距：24-32px（区块之间、卡片与卡片）
呼吸间距：48-64px（页面大区块分隔）
```

### 容器约束
```
最大内容宽度：1200px（管理后台）/ 1440px（着陆页）
表单最大宽度：480-600px
卡片最小高度：不强制等高，允许内容决定高度
```

## 5. 组件定制规范

### 按钮
```css
/* 主按钮 - 实心 */
background: 强调色;
border: none;
border-radius: 8px;
padding: 10px 24px;
font-weight: 500;
transition: all 0.2s ease;
/* hover: 亮度提升 10%，微弱上移 transform: translateY(-1px) */

/* 次按钮 - 幽灵 */
background: transparent;
border: 1px solid 分割线;
color: 主文字色;
/* hover: 背景变为 0.03 透明度的强调色 */
```

### 卡片
```css
background: 卡片背景色;
border: 1px solid 分割线;
border-radius: 12px;  /* 外层大圆角 */
padding: 24px;
/* 内部元素用 8px 圆角 */
/* 不使用 box-shadow，用 border 表达层次 */
```

### 表单输入
```css
background: 输入框背景（比卡片深/浅 0.02）;
border: 1px solid 分割线;
border-radius: 8px;
padding: 10px 14px;
/* focus: border-color 变为强调色，添加 0.15 透明度的强调色外发光 */
```

## 6. 微动效

### 原则
- 只用 `transform` 和 `opacity`，不用 `top/left/width/height`
- 持续时间：150-300ms
- 缓动函数：`cubic-bezier(0.16, 1, 0.3, 1)`（弹性出）或 `ease-out`
- 不做无限循环动画（除了 loading）

### 入场动画
```css
/* 元素进入视口 */
.fade-up-enter {
  opacity: 0;
  transform: translateY(16px);
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}
/* 列表项交错延迟：animation-delay: calc(var(--index) * 60ms) */
```

### 交互反馈
```css
/* 卡片悬浮 */
transition: transform 0.2s ease, border-color 0.2s ease;
/* hover: translateY(-2px) + border-color 变为强调色 0.3 透明度 */

/* 按钮点击 */
/* active: transform: scale(0.98) */
```

## 7. 空状态设计

不要用 Element Plus 默认的空状态图。为不同场景设计有意义的空状态：

- **无项目**：展示一个"创建第一个项目"的引导卡片，带游戏手柄图标
- **无 Agent**：展示招聘 Agent 的快捷入口
- **无数据**：用轻量的图标 + 一行说明文字 + 一个操作按钮

## 8. 响应式

```
移动端（<768px）：
  - 单列布局
  - 卡片全宽
  - 侧边栏折叠为汉堡菜单
  - 表格改为卡片列表

平板（768-1024px）：
  - 两列布局
  - 侧边栏可折叠

桌面（>1024px）：
  - 完整布局
  - 侧边栏固定
```

## 9. 常见错误

1. **使用默认样式**：要自定义Element Plus样式
2. **配色不当**：要使用统一的色彩系统
3. **层次不清**：要建立清晰的视觉层次
4. **间距混乱**：要使用统一的间距系统
5. **动效过度**：要适度使用动效
