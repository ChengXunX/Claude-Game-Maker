/**
 * 快捷键工具模块
 * 提供常用操作的键盘快捷键支持
 *
 * 快捷键列表：
 * - Ctrl+K / Cmd+K - 全局搜索
 * - Ctrl+/ / Cmd+/ - 快捷键帮助
 * - Esc - 关闭对话框/返回
 *
 * @author chengxun
 * @since 1.0.0
 */

import { ref } from 'vue'
import { useRouter } from 'vue-router'

/** 快捷键帮助对话框可见性 */
export const showShortcutHelp = ref(false)

/** 已注册的快捷键 */
const shortcuts = ref([])

/**
 * 注册快捷键
 * @param {string} key - 按键（如 'k', '/', 'Escape'）
 * @param {Object} options - 选项
 * @param {boolean} options.ctrl - 是否需要 Ctrl/Cmd
 * @param {boolean} options.shift - 是否需要 Shift
 * @param {boolean} options.alt - 是否需要 Alt
 * @param {Function} options.action - 执行的动作
 * @param {string} options.description - 快捷键描述
 */
export function registerShortcut(key, options) {
  shortcuts.value.push({ key, ...options })
}

/**
 * 初始化快捷键监听
 * @param {Router} router - Vue Router 实例
 */
export function initShortcuts(router) {
  // 注册默认快捷键
  registerShortcut('k', {
    ctrl: true,
    action: () => {
      router.push('/search')
    },
    description: '全局搜索'
  })

  registerShortcut('/', {
    ctrl: true,
    action: () => {
      showShortcutHelp.value = !showShortcutHelp.value
    },
    description: '显示/隐藏快捷键帮助'
  })

  registerShortcut('Escape', {
    action: () => {
      showShortcutHelp.value = false
    },
    description: '关闭弹窗'
  })

  // 监听键盘事件
  document.addEventListener('keydown', handleKeydown)
}

/**
 * 处理键盘事件
 * @param {KeyboardEvent} event - 键盘事件
 */
function handleKeydown(event) {
  const isMac = navigator.platform.toUpperCase().indexOf('MAC') >= 0
  const ctrlKey = isMac ? event.metaKey : event.ctrlKey

  for (const shortcut of shortcuts.value) {
    const keyMatch = event.key.toLowerCase() === shortcut.key.toLowerCase()
    const ctrlMatch = shortcut.ctrl ? ctrlKey : !ctrlKey
    const shiftMatch = shortcut.shift ? event.shiftKey : !event.shiftKey
    const altMatch = shortcut.alt ? event.altKey : !event.altKey

    if (keyMatch && ctrlMatch && shiftMatch && altMatch) {
      event.preventDefault()
      shortcut.action()
      break
    }
  }
}

/**
 * 清理快捷键监听
 */
export function destroyShortcuts() {
  document.removeEventListener('keydown', handleKeydown)
  shortcuts.value = []
}

/**
 * 获取所有注册的快捷键
 * @returns {Array} 快捷键列表
 */
export function getShortcuts() {
  return shortcuts.value.map(s => ({
    key: s.key,
    ctrl: s.ctrl,
    shift: s.shift,
    alt: s.alt,
    description: s.description,
    label: formatShortcutLabel(s)
  }))
}

/**
 * 格式化快捷键标签
 * @param {Object} shortcut - 快捷键配置
 * @returns {string} 格式化后的标签
 */
function formatShortcutLabel(shortcut) {
  const isMac = navigator.platform.toUpperCase().indexOf('MAC') >= 0
  const parts = []

  if (shortcut.ctrl) {
    parts.push(isMac ? '⌘' : 'Ctrl')
  }
  if (shortcut.shift) {
    parts.push(isMac ? '⇧' : 'Shift')
  }
  if (shortcut.alt) {
    parts.push(isMac ? '⌥' : 'Alt')
  }

  const keyMap = {
    'k': 'K',
    '/': '/',
    'Escape': 'Esc'
  }
  parts.push(keyMap[shortcut.key] || shortcut.key)

  return parts.join(isMac ? '' : '+')
}
