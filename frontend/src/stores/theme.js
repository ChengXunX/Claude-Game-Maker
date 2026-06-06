import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

/**
 * 主题状态管理
 * 支持亮色/暗色/跟随系统三种模式
 */
export const useThemeStore = defineStore('theme', () => {
  // 主题模式: 'light' | 'dark' | 'auto'
  const mode = ref(localStorage.getItem('theme-mode') || 'light')

  // 实际应用的主题（auto 模式下根据系统偏好计算）
  const actualTheme = ref('light')

  // 系统是否偏好暗色
  const systemDark = ref(false)

  /**
   * 初始化主题
   * 监听系统主题变化，应用初始主题
   */
  function init() {
    // 检测系统主题偏好
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    systemDark.value = mediaQuery.matches

    // 监听系统主题变化
    mediaQuery.addEventListener('change', (e) => {
      systemDark.value = e.matches
      if (mode.value === 'auto') {
        applyTheme(e.matches ? 'dark' : 'light')
      }
    })

    // 应用初始主题
    updateActualTheme()
  }

  /**
   * 更新实际主题
   */
  function updateActualTheme() {
    if (mode.value === 'auto') {
      actualTheme.value = systemDark.value ? 'dark' : 'light'
    } else {
      actualTheme.value = mode.value
    }
    applyTheme(actualTheme.value)
  }

  /**
   * 应用主题到 DOM
   * @param {'light' | 'dark'} theme
   */
  function applyTheme(theme) {
    const html = document.documentElement

    if (theme === 'dark') {
      html.classList.add('dark')
      html.setAttribute('data-theme', 'dark')
    } else {
      html.classList.remove('dark')
      html.setAttribute('data-theme', 'light')
    }

    // 更新 meta theme-color
    const metaTheme = document.querySelector('meta[name="theme-color"]')
    if (metaTheme) {
      metaTheme.setAttribute('content', theme === 'dark' ? '#1d1e1f' : '#ffffff')
    }
  }

  /**
   * 设置主题模式
   * @param {'light' | 'dark' | 'auto'} newMode
   */
  function setMode(newMode) {
    mode.value = newMode
    localStorage.setItem('theme-mode', newMode)
    updateActualTheme()
  }

  /**
   * 切换主题（亮色 <-> 暗色）
   */
  function toggle() {
    const newMode = actualTheme.value === 'dark' ? 'light' : 'dark'
    setMode(newMode)
  }

  // 监听 mode 变化
  watch(mode, () => {
    updateActualTheme()
  })

  return {
    mode,
    actualTheme,
    systemDark,
    init,
    setMode,
    toggle
  }
})
