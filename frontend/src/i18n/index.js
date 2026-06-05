/**
 * 国际化配置模块
 * 提供多语言支持框架
 *
 * 支持语言：
 * - zh-CN: 简体中文（默认）
 * - en-US: English
 *
 * @author chengxun
 * @since 1.0.0
 */

import { ref, computed } from 'vue'
import zhCN from './zh-CN'
import enUS from './en-US'

/** 语言包映射 */
const messages = {
  'zh-CN': zhCN,
  'en-US': enUS
}

/** 当前语言 */
const currentLocale = ref(localStorage.getItem('locale') || 'zh-CN')

/**
 * 获取当前语言的翻译文本
 * @param {string} key - 翻译键，支持点号分隔的嵌套键（如 'menu.dashboard'）
 * @param {Object} params - 插值参数（如 { name: 'Admin' }）
 * @returns {string} 翻译后的文本
 */
export function t(key, params = {}) {
  const locale = currentLocale.value
  const messages = getMessages(locale)

  let value = getNestedValue(messages, key)

  if (value === undefined) {
    // 回退到中文
    value = getNestedValue(zhCN, key)
  }

  if (value === undefined) {
    return key
  }

  // 插值替换
  if (typeof value === 'string' && Object.keys(params).length > 0) {
    return value.replace(/\{(\w+)\}/g, (match, key) => {
      return params[key] !== undefined ? params[key] : match
    })
  }

  return value
}

/**
 * 切换语言
 * @param {string} locale - 语言代码
 */
export function setLocale(locale) {
  if (messages[locale]) {
    currentLocale.value = locale
    localStorage.setItem('locale', locale)
  }
}

/**
 * 获取当前语言
 * @returns {string} 语言代码
 */
export function getLocale() {
  return currentLocale.value
}

/**
 * 获取可用语言列表
 * @returns {Array} 语言列表
 */
export function getAvailableLocales() {
  return [
    { code: 'zh-CN', name: '简体中文' },
    { code: 'en-US', name: 'English' }
  ]
}

/**
 * 获取语言包
 * @param {string} locale - 语言代码
 * @returns {Object} 语言包
 */
function getMessages(locale) {
  return messages[locale] || messages['zh-CN']
}

/**
 * 获取嵌套对象的值
 * @param {Object} obj - 对象
 * @param {string} key - 点号分隔的键
 * @returns {*} 值
 */
function getNestedValue(obj, key) {
  return key.split('.').reduce((o, k) => o?.[k], obj)
}

/**
 * Vue 插件安装函数
 * @param {App} app - Vue 应用实例
 */
export function install(app) {
  app.config.globalProperties.$t = t
  app.provide('t', t)
}

export default {
  install,
  t,
  setLocale,
  getLocale,
  getAvailableLocales
}
