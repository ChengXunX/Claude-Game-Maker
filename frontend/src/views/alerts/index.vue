<template>
  <div class="alerts-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>告警中心</span>
          <el-button @click="loadAlerts" :loading="loading">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </template>

      <!-- 筛选 -->
      <div class="filter-bar">
        <el-select v-model="filterStatus" placeholder="状态" clearable style="width: 120px">
          <el-option label="待处理" value="PENDING" />
          <el-option label="已确认" value="ACKNOWLEDGED" />
          <el-option label="已解决" value="RESOLVED" />
        </el-select>
        <el-select v-model="filterPriority" placeholder="优先级" clearable style="width: 120px">
          <el-option label="紧急" value="CRITICAL" />
          <el-option label="高" value="HIGH" />
          <el-option label="中" value="MEDIUM" />
          <el-option label="低" value="LOW" />
        </el-select>
        <el-button type="primary" @click="loadAlerts">查询</el-button>
      </div>

      <!-- 告警列表 -->
      <el-table :data="alerts" v-loading="loading" stripe>
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="优先级" width="80">
          <template #default="{ row }">
            <el-tag :type="getPriorityType(row.priority)" size="small">
              {{ getPriorityLabel(row.priority) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="agentId" label="Agent" width="120" show-overflow-tooltip />
        <el-table-column label="触发时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.triggeredAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING'"
              type="warning"
              size="small"
              text
              @click="handleAcknowledge(row)"
            >
              确认
            </el-button>
            <el-button
              v-if="row.status !== 'RESOLVED'"
              type="success"
              size="small"
              text
              @click="handleResolve(row)"
            >
              解决
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
/**
 * 告警中心页面
 * 查看和管理告警
 *
 * 操作维度：系统级
 * 权限要求：system:monitor
 */
import { ref, onMounted } from 'vue'
import { alertApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const alerts = ref([])
const filterStatus = ref('')
const filterPriority = ref('')

/** 获取优先级类型 */
const getPriorityType = (priority) => {
  const typeMap = { 'CRITICAL': 'danger', 'HIGH': 'warning', 'MEDIUM': 'info', 'LOW': 'success' }
  return typeMap[priority] || 'info'
}

/** 获取优先级标签 */
const getPriorityLabel = (priority) => {
  const labelMap = { 'CRITICAL': '紧急', 'HIGH': '高', 'MEDIUM': '中', 'LOW': '低' }
  return labelMap[priority] || priority
}

/** 获取状态类型 */
const getStatusType = (status) => {
  const typeMap = { 'PENDING': 'warning', 'ACKNOWLEDGED': 'info', 'RESOLVED': 'success' }
  return typeMap[status] || 'info'
}

/** 获取状态标签 */
const getStatusLabel = (status) => {
  const labelMap = { 'PENDING': '待处理', 'ACKNOWLEDGED': '已确认', 'RESOLVED': '已解决' }
  return labelMap[status] || status
}

/** 格式化时间 */
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

/** 加载告警 */
const loadAlerts = async () => {
  loading.value = true
  try {
    const params = {}
    if (filterStatus.value) params.status = filterStatus.value
    if (filterPriority.value) params.priority = filterPriority.value

    const data = await alertApi.getAlerts(params)
    // 后端返回分页对象，需要提取content数组
    alerts.value = data?.content || (Array.isArray(data) ? data : [])
  } catch (error) {
    ElMessage.error('加载告警失败')
  } finally {
    loading.value = false
  }
}

/** 确认告警 */
const handleAcknowledge = async (alert) => {
  try {
    await alertApi.acknowledgeAlert(alert.id)
    ElMessage.success('告警已确认')
    loadAlerts()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

/** 解决告警 */
const handleResolve = async (alert) => {
  try {
    const { value: resolution } = await ElMessageBox.prompt('请输入解决方案：', '解决告警', {
      confirmButtonText: '解决',
      cancelButtonText: '取消',
      inputPlaceholder: '解决方案'
    })

    await alertApi.resolveAlert(alert.id, { resolution })
    ElMessage.success('告警已解决')
    loadAlerts()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

onMounted(() => {
  loadAlerts()
})
</script>

<style scoped>
.alerts-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
</style>
