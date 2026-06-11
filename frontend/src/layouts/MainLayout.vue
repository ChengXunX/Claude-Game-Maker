<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="aside">
      <div class="logo">
        <el-icon :size="24"><Monitor /></el-icon>
        <span v-show="!isCollapse">Game Maker</span>
      </div>

      <el-menu
        :default-active="currentRoute"
        :default-openeds="defaultOpeneds"
        :collapse="isCollapse"
        :router="true"
        background-color="#001529"
        text-color="#ffffffa6"
        active-text-color="#1890ff"
      >
        <template v-for="group in menuGroups" :key="group.name">
          <!-- 有分组名称的菜单组：包裹为可折叠的 sub-menu -->
          <el-sub-menu v-if="group.name" :index="'group:' + group.name">
            <template #title>
              <span class="group-title-text">{{ group.name }}</span>
            </template>

            <template v-for="route in group.items" :key="route.path">
              <!-- 单级菜单 -->
              <el-menu-item v-if="!route.children" :index="'/' + route.path">
                <el-icon><component :is="route.meta?.icon" /></el-icon>
                <template #title>{{ route.meta?.title }}</template>
              </el-menu-item>

              <!-- 多级菜单（父子菜单） -->
              <el-sub-menu v-else :index="'/' + route.path">
                <template #title>
                  <el-icon><component :is="route.meta?.icon" /></el-icon>
                  <span>{{ route.meta?.title }}</span>
                </template>
                <el-menu-item
                  v-for="child in route.children"
                  :key="child.path"
                  :index="'/' + route.path + '/' + child.path"
                >
                  {{ child.meta?.title }}
                </el-menu-item>
              </el-sub-menu>
            </template>
          </el-sub-menu>

          <!-- 无分组名称的菜单项：直接显示 -->
          <template v-else>
            <template v-for="route in group.items" :key="route.path">
              <el-menu-item v-if="!route.children" :index="'/' + route.path">
                <el-icon><component :is="route.meta?.icon" /></el-icon>
                <template #title>{{ route.meta?.title }}</template>
              </el-menu-item>
              <el-sub-menu v-else :index="'/' + route.path">
                <template #title>
                  <el-icon><component :is="route.meta?.icon" /></el-icon>
                  <span>{{ route.meta?.title }}</span>
                </template>
                <el-menu-item
                  v-for="child in route.children"
                  :key="child.path"
                  :index="'/' + route.path + '/' + child.path"
                >
                  {{ child.meta?.title }}
                </el-menu-item>
              </el-sub-menu>
            </template>
          </template>
        </template>
      </el-menu>
    </el-aside>

    <!-- 主内容区 -->
    <el-container>
      <!-- 顶部导航栏 -->
      <el-header class="header">
        <div class="header-left">
          <el-icon
            class="collapse-btn"
            @click="isCollapse = !isCollapse"
          >
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>

          <!-- 面包屑导航 -->
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
              {{ item.meta?.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <!-- 主题切换 -->
          <ThemeToggle />

          <!-- 全局搜索 -->
          <el-icon :size="20" class="header-icon" @click="router.push('/search')">
            <Search />
          </el-icon>

          <!-- 通知铃铛 -->
          <el-badge :value="notificationCount" :max="99" :hidden="notificationCount === 0" class="notification-badge" @click="router.push('/notifications')">
            <el-icon :size="20" class="header-icon">
              <Bell />
            </el-icon>
          </el-badge>

          <!-- 用户头像和下拉菜单 -->
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="32" :src="userAvatar">
                {{ username?.charAt(0)?.toUpperCase() }}
              </el-avatar>
              <span class="username">{{ username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon> 个人中心
                </el-dropdown-item>
                <el-dropdown-item command="password">
                  <el-icon><Lock /></el-icon> 修改密码
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 页面内容 -->
      <el-main class="main">
        <router-view v-slot="{ Component, route }">
          <transition name="fade" mode="out-in">
            <keep-alive :max="10">
              <component :is="Component" :key="route.path" />
            </keep-alive>
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
/**
 * 主布局组件
 * 包含侧边栏菜单、顶部导航栏和主内容区
 *
 * 功能：
 * - 菜单权限过滤：根据用户权限显示/隐藏菜单项
 * - 父子菜单分组：按功能模块分组显示
 * - 菜单折叠/展开
 * - 面包屑导航
 * - 用户信息和快捷操作
 */
import { ref, computed, onMounted, watch, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { notificationApi } from '@/api'
import ThemeToggle from '@/components/ThemeToggle.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()

const isCollapse = ref(false)
const notificationCount = ref(0)
let notificationTimer = null

/** 加载未读通知数量 */
const loadNotificationCount = async () => {
  try {
    const data = await notificationApi.getUnreadCount()
    notificationCount.value = data?.count || 0
  } catch (error) {
    console.error('加载通知数量失败:', error)
  }
}

/**
 * 全局路由变化监听
 * 路由切换时恢复 body 状态，防止 el-dialog 残留的滚动锁定
 */
watch(() => route.path, (newPath, oldPath) => {
  if (newPath !== oldPath) {
    // 恢复 body 滚动（el-dialog 打开时会锁定 body）
    document.body.style.overflow = ''
    document.body.classList.remove('el-popup-parent--hidden')
  }
})

/** 用户信息 */
const username = computed(() => userStore.username || 'Admin')
const userAvatar = computed(() => userStore.avatar || '')

/** 当前路由路径 */
const currentRoute = computed(() => route.path)

/** 默认展开的菜单：自动展开当前路由所属的分组 */
const defaultOpeneds = computed(() => {
  const opened = []

  // 展开当前路由所属的顶级分组
  for (const group of menuGroups.value) {
    if (group.name) {
      const hasActive = group.items.some(item => {
        // 有子菜单的路由：检查子菜单路径
        if (item.children) {
          return item.children.some(child => {
            const childPath = '/' + item.path + '/' + child.path
            return route.path === childPath || route.path.startsWith(childPath + '/')
          })
        }
        // 单级菜单：检查路径
        const itemPath = '/' + item.path
        return route.path === itemPath || route.path.startsWith(itemPath + '/')
      })
      if (hasActive) {
        opened.push('group:' + group.name)
      }
    }
  }

  // 展开当前路由所属的子菜单（admin/system 等）
  for (const group of menuGroups.value) {
    for (const item of group.items) {
      if (item.children) {
        const hasActiveChild = item.children.some(child => {
          const childPath = '/' + item.path + '/' + child.path
          return route.path === childPath || route.path.startsWith(childPath + '/')
        })
        if (hasActiveChild) {
          opened.push('/' + item.path)
        }
      }
    }
  }

  return opened
})

/** 面包屑导航 */
const breadcrumbs = computed(() => {
  return route.matched.filter(item => item.meta?.title && item.path !== '/')
})

/**
 * 检查路由是否有权限显示
 * @param {Object} routeItem - 路由配置
 * @returns {boolean} 是否有权限
 */
const hasRoutePermission = (routeItem) => {
  // 没有设置权限要求的路由，所有用户可见
  if (!routeItem.meta?.permission) return true

  // 管理员拥有所有权限
  if (userStore.isAdmin()) return true

  // 检查具体权限
  return userStore.hasPermission(routeItem.meta.permission)
}

/**
 * 过滤菜单路由（根据权限和 hidden 标记）
 */
const filteredRoutes = computed(() => {
  const mainRoute = router.options.routes.find(r => r.path === '/')
  if (!mainRoute?.children) return []

  return mainRoute.children.filter(r => {
    // 隐藏的路由不显示
    if (r.meta?.hidden) return false

    // 有子菜单的路由：检查是否有任何子菜单可见
    if (r.children) {
      const visibleChildren = r.children.filter(child => {
        if (child.meta?.hidden) return false
        return hasRoutePermission(child)
      })
      return visibleChildren.length > 0
    }

    // 单级菜单：检查权限
    return hasRoutePermission(r)
  })
})

/**
 * 按分组组织菜单
 * 结构：[{ name: '工作台', items: [...] }, { name: 'Agent管理', items: [...] }, ...]
 */
const menuGroups = computed(() => {
  const groups = []
  const groupMap = new Map()

  filteredRoutes.value.forEach(r => {
    const groupName = r.meta?.group || ''

    if (!groupMap.has(groupName)) {
      groupMap.set(groupName, [])
    }

    // 如果有子菜单，过滤有权限的子菜单
    if (r.children) {
      const visibleChildren = r.children.filter(child => {
        if (child.meta?.hidden) return false
        return hasRoutePermission(child)
      })

      if (visibleChildren.length > 0) {
        groupMap.get(groupName).push({
          ...r,
          children: visibleChildren
        })
      }
    } else {
      groupMap.get(groupName).push(r)
    }
  })

  // 转换为数组，保持顺序
  const groupOrder = ['工作台', 'Agent管理', '项目管理', '运维中心', '通知中心', '系统管理', '集成配置', '']

  groupOrder.forEach(name => {
    if (groupMap.has(name) && groupMap.get(name).length > 0) {
      groups.push({
        name,
        items: groupMap.get(name)
      })
    }
  })

  return groups
})

/** 处理下拉菜单命令 */
const handleCommand = async (command) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'password':
      router.push('/profile/password')
      break
    case 'logout':
      try {
        await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await userStore.logout()
        router.push('/login')
        ElMessage.success('已退出登录')
      } catch {
        // 取消操作
      }
      break
  }
}

/** 初始化 */
onMounted(async () => {
  // 初始化主题
  themeStore.init()

  // 如果已登录但没有用户信息，重新获取
  if (userStore.isLoggedIn && !userStore.userInfo) {
    try {
      await userStore.getUserInfo()
    } catch (error) {
      console.error('获取用户信息失败:', error)
    }
  }

  // 加载通知数量
  if (userStore.isLoggedIn) {
    loadNotificationCount()
    // 每60秒刷新一次通知数量
    notificationTimer = setInterval(loadNotificationCount, 60000)
  }
})

/** 清理 */
onUnmounted(() => {
  if (notificationTimer) {
    clearInterval(notificationTimer)
  }
})
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.aside {
  background-color: #001529;
  transition: width 0.3s;
  overflow: hidden;
  overflow-y: auto;
}

html.dark .aside {
  background-color: #0d0d0d;
}

.aside::-webkit-scrollbar {
  width: 0;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid #ffffff1a;
  flex-shrink: 0;
}

.menu-group-title {
  padding: 16px 16px 4px;
  font-size: 12px;
  color: #ffffff73;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.group-title-text {
  font-size: 13px;
  font-weight: 500;
  color: #ffffffb3;
  letter-spacing: 0.5px;
}

.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  transition: background-color 0.3s;
}

html.dark .header {
  background: #1d1e1f;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.3);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: #333;
  transition: color 0.3s;
}

html.dark .collapse-btn {
  color: #e5eaf3;
}

.collapse-btn:hover {
  color: #1890ff;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.header-icon {
  color: #666;
  cursor: pointer;
  transition: color 0.3s;
}

html.dark .header-icon {
  color: #a3a6ad;
}

.header-icon:hover {
  color: #1890ff;
}

.notification-badge {
  cursor: pointer;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.username {
  color: #333;
  font-size: 14px;
  transition: color 0.3s;
}

html.dark .username {
  color: #e5eaf3;
}

.main {
  background-color: #f0f2f5;
  padding: 20px;
  transition: background-color 0.3s;
}

html.dark .main {
  background-color: #141414;
}

/* 页面切换动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
