<template>
  <div class="notifications-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>通知管理</span>
          <div class="header-actions">
            <el-button size="small" type="danger" plain @click="handleCleanup">清理无效</el-button>
            <el-badge :value="unreadCount" :hidden="unreadCount === 0">
              <el-button size="small" @click="handleMarkAllRead">全部已读</el-button>
            </el-badge>
          </div>
        </div>
      </template>

      <!-- 筛选区 -->
      <div class="filter-bar">
        <el-radio-group v-model="filterRead" @change="loadNotifications">
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="unread">未读</el-radio-button>
          <el-radio-button label="read">已读</el-radio-button>
        </el-radio-group>
      </div>

      <!-- 通知列表 -->
      <div class="notification-list" v-loading="loading">
        <div
          v-for="notification in notifications"
          :key="notification.id"
          class="notification-item"
          :class="{ unread: !notification.read }"
          @click="handleView(notification)"
        >
          <div class="notification-icon">
            <el-icon :size="20" :color="getIconColor(notification.type)">
              <component :is="getIcon(notification.type)" />
            </el-icon>
          </div>
          <div class="notification-content">
            <div class="notification-title">
              {{ notification.title }}
              <el-icon v-if="getLink(notification)" class="link-icon"><Link /></el-icon>
            </div>
            <div class="notification-detail" v-if="notification.content && notification.content !== '-'">{{ notification.content }}</div>
            <div class="notification-time">{{ formatTime(notification.createdAt) }}</div>
          </div>
          <div class="notification-actions">
            <el-button
              v-if="!notification.read"
              type="primary"
              size="small"
              text
              @click.stop="handleMarkRead(notification)"
            >
              标记已读
            </el-button>
            <el-button
              type="danger"
              size="small"
              text
              @click.stop="handleDelete(notification)"
            >
              删除
            </el-button>
          </div>
        </div>

        <el-empty v-if="!loading && notifications.length === 0" description="暂无通知" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
/**
 * 通知管理页面
 * 查看和管理用户通知
 *
 * 操作维度：用户级（每个用户看到自己的通知）
 * 权限要求：登录用户即可
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api, { notificationApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Link } from '@element-plus/icons-vue'

const router = useRouter()
const loading = ref(false)
const notifications = ref([])
const unreadCount = ref(0)
const filterRead = ref('')

/** 通知类型对应的跳转路径 */
const LINK_MAP = {
  'TASK': '/agents',
  'AGENT': '/agents',
  'SYSTEM': '/admin/settings',
  'INFO': null,
  'WARNING': null,
  'ERROR': null,
  'SUCCESS': null
}

/** 获取通知图标 */
const getIcon = (type) => {
  const iconMap = {
    'info': 'InfoFilled',
    'warning': 'WarningFilled',
    'error': 'CircleCloseFilled',
    'success': 'SuccessFilled',
    'task': 'Finished',
    'agent': 'UserFilled',
    'system': 'Setting'
  }
  return iconMap[type?.toLowerCase()] || 'Bell'
}

/** 获取图标颜色 */
const getIconColor = (type) => {
  const colorMap = {
    'info': '#409eff',
    'warning': '#e6a23c',
    'error': '#f56c6c',
    'success': '#67c23a',
    'task': '#409eff',
    'agent': '#67c23a',
    'system': '#909399'
  }
  return colorMap[type?.toLowerCase()] || '#909399'
}

/** 获取通知跳转链接 */
const getLink = (notification) => {
  // 优先使用通知自带的链接
  if (notification.link) return notification.link
  // 根据通知类型返回默认链接
  if (notification.referenceType && LINK_MAP[notification.referenceType]) {
    return LINK_MAP[notification.referenceType]
  }
  if (notification.type && LINK_MAP[notification.type]) {
    return LINK_MAP[notification.type]
  }
  return null
}

/** 格式化时间 */
const formatTime = (time) => {
  if (!time) return '-'
  const date = new Date(time)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return date.toLocaleString('zh-CN')
}

/** 加载通知列表 */
const loadNotifications = async () => {
  loading.value = true
  try {
    const params = {}
    if (filterRead.value === 'unread') params.read = false
    if (filterRead.value === 'read') params.read = true

    const data = await notificationApi.getAll(params)
    // 后端返回分页对象，需要提取content数组
    notifications.value = data?.content || (Array.isArray(data) ? data : [])

    // 更新未读数
    const countData = await notificationApi.getUnreadCount()
    unreadCount.value = countData?.count || 0
  } catch (error) {
    ElMessage.error('加载通知失败')
  } finally {
    loading.value = false
  }
}

/** 查看通知详情 */
const handleView = async (notification) => {
  if (!notification.read) {
    await handleMarkRead(notification)
  }
  // 跳转到关联页面
  const link = getLink(notification)
  if (link) {
    router.push(link)
  }
}

/** 标记已读 */
const handleMarkRead = async (notification) => {
  try {
    await notificationApi.markAsRead(notification.id)
    notification.read = true
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

/** 全部标记已读 */
const handleMarkAllRead = async () => {
  try {
    await notificationApi.markAllAsRead()
    notifications.value.forEach(n => n.read = true)
    unreadCount.value = 0
    ElMessage.success('已全部标记为已读')
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

/** 删除通知 */
const handleDelete = async (notification) => {
  try {
    await ElMessageBox.confirm('确定要删除此通知吗？', '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await notificationApi.delete(notification.id)
    ElMessage.success('通知已删除')
    loadNotifications()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

/** 清理无效通知 */
const handleCleanup = async () => {
  try {
    await ElMessageBox.confirm('确定要清理所有无效通知吗？（标题或内容为空的通知）', '清理确认', {
      confirmButtonText: '清理',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const data = await api.delete('/notifications/cleanup')
    ElMessage.success(`已清理 ${data.deleted || 0} 条无效通知`)
    loadNotifications()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('清理失败')
    }
  }
}

onMounted(() => {
  loadNotifications()
})
</script>

<style scoped>
.notifications-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-bar {
  margin-bottom: 16px;
  overflow-x: auto;
}

.notification-list {
  min-height: 200px;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  padding: 16px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: background-color 0.2s;
}

.notification-item:hover {
  background-color: #f5f7fa;
}

.notification-item.unread {
  background-color: #ecf5ff;
}

.notification-icon {
  margin-right: 12px;
  margin-top: 2px;
  flex-shrink: 0;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-title {
  font-weight: 500;
  margin-bottom: 4px;
  word-break: break-word;
  display: flex;
  align-items: center;
  gap: 4px;
}

.link-icon {
  color: #409eff;
  font-size: 14px;
}

.notification-detail {
  font-size: 13px;
  color: #666;
  margin-bottom: 4px;
  word-break: break-word;
}

.notification-time {
  font-size: 12px;
  color: #999;
}

.notification-actions {
  margin-left: 12px;
  flex-shrink: 0;
}

/* 手机端 */
@media (max-width: 767px) {
  .notifications-page {
    padding: 12px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .notification-item {
    flex-wrap: wrap;
    padding: 12px;
  }

  .notification-icon {
    margin-right: 8px;
  }

  .notification-actions {
    width: 100%;
    margin-left: 0;
    margin-top: 8px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
