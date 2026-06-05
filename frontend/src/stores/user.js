import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api'

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(null)
  const permissions = ref([])

  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value?.username || '')
  const avatar = computed(() => userInfo.value?.avatar || '')
  const userId = computed(() => userInfo.value?.id)
  const userRole = computed(() => userInfo.value?.role?.name || '')

  // 登录
  async function login(loginData) {
    try {
      const response = await authApi.login(loginData)

      // 如果需要设备验证，不自动设置token
      if (response.needDeviceVerify) {
        return response
      }

      // 正常登录，设置token
      token.value = response.token
      localStorage.setItem('token', response.token)

      // 获取用户信息
      await getUserInfo()

      return response
    } catch (error) {
      throw error
    }
  }

  // 获取用户信息
  async function getUserInfo() {
    try {
      const response = await authApi.getCurrentUser()
      // 后端返回格式: { success: true, user: {...} }
      const userData = response.user || response
      userInfo.value = userData

      // 解析权限（后端返回的是 Set<String>，可能是数组或对象数组）
      if (userData.role?.permissions) {
        if (Array.isArray(userData.role.permissions)) {
          // 检查是字符串数组还是对象数组
          if (userData.role.permissions.length > 0 && typeof userData.role.permissions[0] === 'string') {
            permissions.value = userData.role.permissions
          } else {
            permissions.value = userData.role.permissions.map(p => p.permission || p)
          }
        }
      }

      return userData
    } catch (error) {
      throw error
    }
  }

  // 登出
  async function logout() {
    try {
      await authApi.logout()
    } catch (error) {
      console.error('登出失败:', error)
    } finally {
      // 清除本地状态
      token.value = ''
      userInfo.value = null
      permissions.value = []
      localStorage.removeItem('token')
    }
  }

  // 检查权限
  function hasPermission(permission) {
    return permissions.value.includes(permission)
  }

  // 检查是否为管理员
  function isAdmin() {
    return userRole.value === 'ADMIN'
  }

  // 修改密码
  async function changePassword(data) {
    try {
      await authApi.changePassword(data)
    } catch (error) {
      throw error
    }
  }

  // 初始化（如果已有token则获取用户信息）
  async function init() {
    if (token.value) {
      try {
        await getUserInfo()
      } catch (error) {
        // token无效，清除
        token.value = ''
        localStorage.removeItem('token')
      }
    }
  }

  return {
    // 状态
    token,
    userInfo,
    permissions,

    // 计算属性
    isLoggedIn,
    username,
    avatar,
    userId,
    userRole,

    // 方法
    login,
    getUserInfo,
    logout,
    hasPermission,
    isAdmin,
    changePassword,
    init
  }
})
