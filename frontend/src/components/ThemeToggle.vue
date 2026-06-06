<template>
  <el-dropdown @command="handleCommand" trigger="click">
    <el-button :icon="currentIcon" circle />
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="light" :class="{ active: mode === 'light' }">
          <el-icon><Sunny /></el-icon>
          <span>亮色模式</span>
        </el-dropdown-item>
        <el-dropdown-item command="dark" :class="{ active: mode === 'dark' }">
          <el-icon><Moon /></el-icon>
          <span>暗色模式</span>
        </el-dropdown-item>
        <el-dropdown-item command="auto" :class="{ active: mode === 'auto' }">
          <el-icon><Monitor /></el-icon>
          <span>跟随系统</span>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
/**
 * 主题切换组件
 * 支持亮色/暗色/跟随系统三种模式
 */
import { computed } from 'vue'
import { Sunny, Moon, Monitor } from '@element-plus/icons-vue'
import { useThemeStore } from '@/stores/theme'

const themeStore = useThemeStore()

const mode = computed(() => themeStore.mode)
const actualTheme = computed(() => themeStore.actualTheme)

const currentIcon = computed(() => {
  if (mode.value === 'auto') {
    return Monitor
  }
  return actualTheme.value === 'dark' ? Moon : Sunny
})

const handleCommand = (command) => {
  themeStore.setMode(command)
}
</script>

<style scoped>
.el-dropdown-menu .active {
  color: var(--el-color-primary);
  background-color: var(--el-color-primary-light-9);
}
</style>
