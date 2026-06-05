import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import { permissionDirective } from './utils/permission'
import { useUserStore } from './stores/user'
import './style.css'

/**
 * 应用入口
 *
 * 初始化顺序：
 * 1. 创建 Vue 应用和 Pinia 实例
 * 2. 注册全局组件、指令、插件
 * 3. 配置全局错误处理
 * 4. 初始化用户状态（处理页面刷新场景）
 * 5. 挂载应用
 */
const app = createApp(App)
const pinia = createPinia()

// 注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 注册权限指令 v-permission
app.directive('permission', permissionDirective)

/**
 * 全局错误处理器
 * 捕获 Vue 组件中的未处理错误
 */
app.config.errorHandler = (err, instance, info) => {
  console.error('Vue 全局错误:', err)
  console.error('错误信息:', info)

  // 过滤掉一些不需要提示的错误
  const ignoreErrors = [
    'ResizeObserver loop',
    'Script error',
    'Network Error'
  ]

  const errorMessage = err?.message || String(err)
  const shouldIgnore = ignoreErrors.some(ignore => errorMessage.includes(ignore))

  if (!shouldIgnore) {
    ElementPlus.ElMessage.error({
      message: `页面错误: ${errorMessage.substring(0, 100)}`,
      duration: 5000,
      showClose: true
    })
  }
}

/**
 * 全局未处理的 Promise 拒绝
 */
window.addEventListener('unhandledrejection', (event) => {
  console.error('未处理的 Promise 拒绝:', event.reason)

  // 过滤掉 API 错误（已经在拦截器中处理）
  if (event.reason?.response) {
    return
  }

  const message = event.reason?.message || '操作失败'
  ElementPlus.ElMessage.error({
    message: message.substring(0, 100),
    duration: 3000
  })
})

/**
 * 全局 JS 错误
 */
window.onerror = (message, source, lineno, colno, error) => {
  console.error('全局 JS 错误:', { message, source, lineno, colno, error })

  // 过滤掉一些常见无害错误
  if (message?.includes('ResizeObserver') || message?.includes('Script error')) {
    return
  }

  ElementPlus.ElMessage.error({
    message: '页面发生错误，请刷新重试',
    duration: 5000,
    showClose: true
  })
}

// 安装插件
app.use(pinia)
app.use(router)
app.use(ElementPlus)

/**
 * 初始化用户状态
 * 页面刷新后，Pinia 状态会丢失，需要从 localStorage 恢复 token 并重新获取用户信息
 */
const userStore = useUserStore()
userStore.init().finally(() => {
  // 无论初始化成功与否，都挂载应用
  app.mount('#app')
})
