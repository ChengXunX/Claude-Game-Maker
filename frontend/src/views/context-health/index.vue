<template>
  <div class="context-health-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>上下文监控</span>
          <el-button @click="loadHealthStatus" :loading="loading">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </template>

      <!-- 统计卡片 -->
      <el-row :gutter="16" class="stat-cards">
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value success">{{ summary.healthy || 0 }}</div>
              <div class="stat-label">健康</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value warning">{{ summary.warning || 0 }}</div>
              <div class="stat-label">警告</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value danger">{{ summary.error || 0 }}</div>
              <div class="stat-label">错误</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value">{{ summary.critical || 0 }}</div>
              <div class="stat-label">严重</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 健康状态列表 -->
      <el-table :data="healthStatuses" v-loading="loading" stripe>
        <el-table-column prop="agentId" label="Agent ID" width="200">
          <template #default="{ row }">
            <code>{{ row.agentId }}</code>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.healthy ? 'success' : 'danger'" size="small">
              {{ row.healthy ? '健康' : row.severity }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="issue" label="问题" min-width="200" />
        <el-table-column prop="lastCheck" label="上次检查" width="180">
          <template #default="{ row }">
            {{ formatTime(row.lastCheck) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="!row.healthy"
              type="warning"
              size="small"
              text
              @click="handleRecover(row)"
              v-permission="'agents:manage'"
            >
              恢复
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
/**
 * 上下文监控页面
 * 监控Agent上下文健康状态
 */
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'

const loading = ref(false)
const summary = ref({})
const healthStatuses = ref([])
let refreshTimer = null

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

const loadHealthStatus = async () => {
  loading.value = true
  try {
    const [summaryData, statusData] = await Promise.all([
      api.get('/capabilities/health/summary'),
      api.get('/capabilities/health')
    ])
    summary.value = summaryData || {}
    const statuses = statusData || {}
    healthStatuses.value = Object.entries(statuses).map(([id, status]) => ({
      agentId: id,
      ...status
    }))
  } catch (error) {
    ElMessage.error('加载健康状态失败')
  } finally {
    loading.value = false
  }
}

const handleRecover = async (status) => {
  try {
    await ElMessageBox.confirm(`确定要恢复 Agent "${status.agentId}" 吗？`, '恢复确认')
    const result = await api.post(`/capabilities/health/${status.agentId}/recover`)
    if (result.success) {
      ElMessage.success('恢复已触发')
      loadHealthStatus()
    } else {
      ElMessage.error('恢复失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('恢复失败')
    }
  }
}

onMounted(() => {
  loadHealthStatus()
  // 每30秒自动刷新
  refreshTimer = setInterval(loadHealthStatus, 30000)
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})
</script>

<style scoped>
.context-health-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.stat-cards {
  margin-bottom: 24px;
}

.stat-item {
  text-align: center;
  padding: 16px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
}

.stat-value.success {
  color: var(--el-color-success);
}

.stat-value.warning {
  color: var(--el-color-warning);
}

.stat-value.danger {
  color: var(--el-color-danger);
}

.stat-label {
  margin-top: 8px;
  color: var(--el-text-color-secondary);
}
</style>
