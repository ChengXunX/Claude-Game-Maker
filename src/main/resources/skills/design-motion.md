---
name: 动效设计
description: Vue3页面的微动效和过渡设计，包括页面切换、列表动画、交互反馈、数据更新动效
trigger: 动效,动画,transition,animation,过渡效果,微交互,motion
examples: 给这个列表添加入场动画|优化页面切换效果|按钮点击反馈
---

# 动效设计技能（Vue 3）

> 为 Vue 3 + Element Plus 项目添加有意义的微动效
> 原则：动效服务于体验，不是装饰

## 0. 核心原则

### 做
- 动效持续时间：150-300ms（快速反馈）
- 使用 `transform` 和 `opacity`（GPU 加速）
- 缓出曲线：`cubic-bezier(0.16, 1, 0.3, 1)`（弹性出）
- 列表项交错入场：`delay: index * 50ms`
- 状态变化要有过渡（不要瞬间切换）

### 不做
- 超过 500ms 的动画（除了 loading）
- 使用 `top`/`left`/`width`/`height`（触发重排）
- 无意义的装饰动画
- 阻塞用户操作的动画

## 1. Vue Transition 组件

### 基础用法
```vue
<template>
  <Transition name="fade">
    <div v-if="show">内容</div>
  </Transition>
</template>

<style>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
```

### 常用动画
```vue
<!-- 淡入淡出 -->
<Transition name="fade">...</Transition>

<!-- 滑入滑出 -->
<Transition name="slide">...</Transition>

<!-- 缩放 -->
<Transition name="scale">...</Transition>

<!-- 弹跳 -->
<Transition name="bounce">...</Transition>
```

## 2. 列表动画

### 列表项入场动画
```vue
<template>
  <TransitionGroup name="list" tag="ul">
    <li v-for="(item, index) in items" 
        :key="item.id"
        :style="{ animationDelay: index * 50 + 'ms' }">
      {{ item.name }}
    </li>
  </TransitionGroup>
</template>

<style>
.list-enter-active {
  animation: slideIn 0.3s ease forwards;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
```

### 列表项删除动画
```vue
<template>
  <TransitionGroup name="list" tag="ul">
    <li v-for="item in items" :key="item.id">
      {{ item.name }}
      <button @click="remove(item)">删除</button>
    </li>
  </TransitionGroup>
</template>

<style>
.list-leave-active {
  animation: slideOut 0.3s ease forwards;
}

@keyframes slideOut {
  to {
    opacity: 0;
    transform: translateX(100px);
  }
}
</style>
```

## 3. 交互反馈

### 按钮点击反馈
```vue
<template>
  <button class="btn" @click="handleClick">
    点击
  </button>
</template>

<style>
.btn {
  transition: transform 0.1s ease;
}

.btn:active {
  transform: scale(0.95);
}
</style>
```

### 悬停效果
```vue
<template>
  <div class="card" @mouseenter="hover = true" @mouseleave="hover = false">
    内容
  </div>
</template>

<style>
.card {
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}
</style>
```

## 4. 数据更新动效

### 数字滚动
```vue
<template>
  <span class="number">{{ displayValue }}</span>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps(['value'])
const displayValue = ref(0)

watch(() => props.value, (newVal) => {
  const start = displayValue.value
  const end = newVal
  const duration = 500
  const startTime = Date.now()
  
  function update() {
    const elapsed = Date.now() - startTime
    const progress = Math.min(elapsed / duration, 1)
    displayValue.value = Math.floor(start + (end - start) * progress)
    
    if (progress < 1) {
      requestAnimationFrame(update)
    }
  }
  
  update()
})
</script>
```

### 进度条动画
```vue
<template>
  <div class="progress-bar">
    <div class="progress-fill" :style="{ width: progress + '%' }"></div>
  </div>
</template>

<style>
.progress-bar {
  height: 8px;
  background: #eee;
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #409eff;
  transition: width 0.5s ease;
  border-radius: 4px;
}
</style>
```

## 5. 页面切换动画

### 路由切换
```vue
<template>
  <router-view v-slot="{ Component }">
    <Transition name="page" mode="out-in">
      <component :is="Component" />
    </Transition>
  </router-view>
</template>

<style>
.page-enter-active,
.page-leave-active {
  transition: opacity 0.3s ease;
}

.page-enter-from,
.page-leave-to {
  opacity: 0;
}
</style>
```

## 6. Loading动画

### 旋转动画
```vue
<template>
  <div class="loading">
    <div class="spinner"></div>
  </div>
</template>

<style>
.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #409eff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>
```

## 常见错误

1. **动画太慢**：要控制在300ms内
2. **性能问题**：要使用transform和opacity
3. **无意义动画**：动效要服务于体验
4. **阻塞操作**：动画不能阻塞用户操作
5. **不一致**：要保持动效风格一致
