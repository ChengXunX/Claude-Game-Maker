<template>
  <div class="health-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>Agent 健康监控</span>
          <div class="header-actions">
            <ProjectSelector
              v-model="selectedProjectId"
              placeholder="选择项目"
              width="200px"
              size="default"
              @change="handleProjectChange"
            />
            <el-button @click="loadHealth" :loading="loading">
              <el-icon><Refresh /></el-icon> 刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- 健康统计 -->
      <el-row :gutter="16" class="stat-cards">
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-item">
              <div class="stat-value" :style="{ color: '#67c23a' }">{{ stats.healthyCount || 0 }}</div>
              <div class="stat-label">健康</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-item">
              <div class="stat-value" :style="{ color: '#e6a23c' }">{{ stats.warningCount || 0 }}</div>
              <div class="stat-label">警告</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-item">
              <div class="stat-value" :style="{ color: '#f56c6c' }">{{ stats.unhealthyCount || 0 }}</div>
              <div class="stat-label">不健康</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-item">
              <div class="stat-value" :style="{ color: '#909399' }">{{ stats.offlineCount || 0 }}</div>
              <div class="stat-label">离线</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 健康率 -->
      <div class="health-rate">
        <span class="rate-label">总体健康率：</span>
        <el-progress
          :percentage="stats.healthRate || 0"
          :color="healthRateColor"
          style="width: 300px"
        />
      </div>

      <!-- Agent 健康列表 -->
      <el-table :data="agents" v-loading="loading" stripe class="mt-4">
        <el-table-column prop="agentId" label="Agent ID" width="150" show-overflow-tooltip />
        <el-table-column prop="agentName" label="名称" width="120" />
        <el-table-column prop="agentRole" label="角色" width="120" />
        <el-table-column label="健康状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.healthStatus)" size="small">
              {{ getStatusLabel(row.healthStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="连续错误" width="100">
          <template #default="{ row }">
            <span :class="{ 'error-count': row.consecutiveErrors > 0 }">
              {{ row.consecutiveErrors || 0 }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="平均响应时间" width="120">
          <template #default="{ row }">
            {{ row.avgResponseTime ? row.avgResponseTime + 'ms' : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="运行时间" width="120">
          <template #default="{ row }">
            {{ formatUptime(row.uptimeSeconds) }}
          </template>
        </el-table-column>
        <el-table-column label="最后活动" width="180">
          <template #default="{ row }">
            {{ formatTime(row.lastActivityTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              type="warning"
              size="small"
              text
              @click="handleRestart(row)"
              v-permission="'agents:manage'"
            >
              重启
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
/**
 * Agent 健康监控页面
 * 查看 Agent 健康状态、响应时间、错误统计
 *
 * 操作维度：系统级/项目级
 * 权限要求：agents:view
 */
import { ref, computed, onMounted } from 'vue'
import { healthApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import ProjectSelector from '@/components/ProjectSelector.vue'

const loading = ref(false)
const agents = ref([])
const stats = ref({})
const selectedProjectId = ref(localStorage.getItem('selectedProjectId') || '')

/** 项目切换 */
const handleProjectChange = (projectId) => {
  loadHealth()
}

/** 健康率颜色 */
const healthRateColor = computed(() => {
  const rate = stats.value.healthRate || 0
  if (rate >= 80) return '#67c23a'
  if (rate >= 60) return '#e6a23c'
  return '#f56c6c'
})

/** 获取状态标签类型 */
const getStatusType = (status) => {
  const typeMap = {
    'HEALTHY': 'success',
    'WARNING': 'warning',
    'UNHEALTHY': 'danger',
    'OFFLINE': 'info'
  }
  return typeMap[status] || 'info'
}

/** 获取状态标签文本 */
const getStatusLabel = (status) => {
  const labelMap = {
    'HEALTHY': '健康',
    'WARNING': '警告',
    'UNHEALTHY': '不健康',
    'OFFLINE': '离线'
  }
  return labelMap[status] || status
}

/** 格式化运行时间 */
const formatUptime = (seconds) => {
  if (!seconds) return '-'
  if (seconds < 60) return `${seconds}秒`
  if (seconds < 3600) return `${Math.floor(seconds / 60)}分钟`
  if (seconds < 86400) return `${Math.floor(seconds / 3600)}小时`
  return `${Math.floor(seconds / 86400)}天`
}

/** 格式化时间 */
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

/** 加载健康数据 */
const loadHealth = async () => {
  loading.value = true
  try {
    const [healthData, statsData] = await Promise.all([
      healthApi.getAll(),
      healthApi.getStats()
    ])
    agents.value = healthData || []
    stats.value = statsData || {}
  } catch (error) {
    ElMessage.error('加载健康数据失败')
  } finally {
    loading.value = false
  }
}

/** 重启 Agent */
const handleRestart = async (agent) => {
  try {
    await ElMessageBox.confirm(
      `确定要重启 Agent "${agent.agentName}" 吗？`,
      '重启确认',
      { confirmButtonText: '重启', cancelButtonText: '取消', type: 'warning' }
    )

    await healthApi.restartAgent(agent.agentId)
    ElMessage.success('Agent 重启命令已发送')
    loadHealth()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('重启失败')
    }
  }
}

onMounted(() => {
  loadHealth()
})
</script>

<style scoped>
.health-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-cards {
  margin-bottom: 20px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
}

.stat-label {
  font-size: 13px;
  color: #999;
  margin-top: 4px;
}

.health-rate {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.rate-label {
  font-weight: 500;
}

.error-count {
  color: #f56c6c;
  font-weight: bold;
}

.mt-4 {
  margin-top: 16px;
}
</style>
