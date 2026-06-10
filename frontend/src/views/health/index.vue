<template>
  <div class="health-page">
    <!-- 统计概览 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-success-light-9)">
            <el-icon :size="24" color="var(--el-color-success)"><CircleCheck /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value success">{{ stats.healthyCount || 0 }}</div>
            <div class="stat-label">健康</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-warning-light-9)">
            <el-icon :size="24" color="var(--el-color-warning)"><WarningFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value warning">{{ stats.warningCount || 0 }}</div>
            <div class="stat-label">警告</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-danger-light-9)">
            <el-icon :size="24" color="var(--el-color-danger)"><CircleClose /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value danger">{{ stats.unhealthyCount || 0 }}</div>
            <div class="stat-label">不健康</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-info-light-9)">
            <el-icon :size="24" color="var(--el-color-info)"><RemoveFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value info">{{ stats.offlineCount || 0 }}</div>
            <div class="stat-label">离线</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 健康率 -->
    <el-card class="health-rate-card">
      <div class="health-rate">
        <div class="rate-info">
          <span class="rate-label">总体健康率</span>
          <span class="rate-value">{{ stats.healthRate || 0 }}%</span>
        </div>
        <el-progress
          :percentage="stats.healthRate || 0"
          :color="healthRateColor"
          :stroke-width="20"
          style="flex: 1"
        />
        <el-tag :type="healthRateType" size="large">{{ healthRateLabel }}</el-tag>
      </div>
    </el-card>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>Agent 健康监控</span>
          <div class="header-actions">
            <el-switch
              v-model="autoRefresh"
              active-text="自动刷新"
              inactive-text=""
              @change="handleAutoRefreshChange"
            />
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

      <!-- 搜索和筛选 -->
      <div class="filter-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索 Agent..."
          clearable
          style="width: 200px"
          :prefix-icon="Search"
        />
        <el-select v-model="filterStatus" placeholder="健康状态" clearable style="width: 120px">
          <el-option label="全部" value="" />
          <el-option label="健康" value="HEALTHY" />
          <el-option label="警告" value="WARNING" />
          <el-option label="不健康" value="UNHEALTHY" />
          <el-option label="离线" value="OFFLINE" />
        </el-select>
        <el-button
          v-if="selectedAgents.length > 0"
          type="warning"
          size="small"
          @click="handleBatchRestart"
          v-permission="'agents:manage'"
        >
          批量重启 ({{ selectedAgents.length }})
        </el-button>
      </div>

      <!-- Agent 健康列表 -->
      <el-table
        :data="filteredAgents"
        v-loading="loading"
        stripe
        @selection-change="handleSelectionChange"
        @row-click="handleViewDetail"
        :row-class-name="getRowClassName"
      >
        <el-table-column type="selection" width="45" />
        <el-table-column prop="agentName" label="Agent" width="120">
          <template #default="{ row }">
            <span class="agent-name">{{ row.agentName || row.agentId }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="agentRole" label="角色" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="getRoleTagType(row.agentRole)">{{ getRoleLabel(row.agentRole) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="健康状态" width="100">
          <template #default="{ row }">
            <div class="health-status">
              <span class="status-dot" :class="getStatusClass(row.healthStatus)"></span>
              <el-tag :type="getStatusType(row.healthStatus)" size="small">
                {{ getStatusLabel(row.healthStatus) }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="错误率" width="80" align="center">
          <template #default="{ row }">
            <span :class="{ 'error-count': (row.errorRate || 0) > 10 }">
              {{ row.errorRate ? row.errorRate.toFixed(1) + '%' : '0%' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="平均响应" width="100" align="center">
          <template #default="{ row }">
            <span :class="{ 'slow-response': (row.avgResponseTimeMs || row.avgResponseTime) > 5000 }">
              {{ (row.avgResponseTimeMs || row.avgResponseTime) ? (row.avgResponseTimeMs || row.avgResponseTime) + 'ms' : '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="告警原因" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.healthStatus === 'WARNING' || row.healthStatus === 'UNHEALTHY'" class="warning-reason">
              {{ getWarningReason(row) }}
            </span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="运行时间" width="100">
          <template #default="{ row }">
            {{ formatUptime(row.uptimeSeconds) }}
          </template>
        </el-table-column>
        <el-table-column label="最后活动" width="160">
          <template #default="{ row }">
            {{ formatTime(row.lastActivityTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click.stop="handleViewDetail(row)">详情</el-button>
            <el-button
              type="warning"
              size="small"
              text
              @click.stop="handleRestart(row)"
              v-permission="'agents:manage'"
            >
              重启
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 健康详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :title="currentAgent?.agentName || '健康详情'"
      size="450px"
      direction="rtl"
    >
      <template v-if="currentAgent">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="Agent ID">{{ currentAgent.agentId }}</el-descriptions-item>
          <el-descriptions-item label="名称">{{ currentAgent.agentName }}</el-descriptions-item>
          <el-descriptions-item label="角色">
            <el-tag :type="getRoleTagType(currentAgent.agentRole)" size="small">
              {{ getRoleLabel(currentAgent.agentRole) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="健康状态">
            <el-tag :type="getStatusType(currentAgent.healthStatus)" size="small">
              {{ getStatusLabel(currentAgent.healthStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="连续错误">
            <span :class="{ 'error-count': currentAgent.consecutiveErrors > 0 }">
              {{ currentAgent.consecutiveErrors || 0 }} 次
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="平均响应时间">
            {{ (currentAgent.avgResponseTimeMs || currentAgent.avgResponseTime) ? (currentAgent.avgResponseTimeMs || currentAgent.avgResponseTime) + 'ms' : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="运行时间">
            {{ formatUptime(currentAgent.uptimeSeconds) }}
          </el-descriptions-item>
          <el-descriptions-item label="最后活动">
            {{ formatTime(currentAgent.lastActivityTime) }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 健康指标 -->
        <el-divider>健康指标</el-divider>
        <div class="health-metrics">
          <div class="metric-item">
            <span class="metric-label">响应时间</span>
            <el-progress
              :percentage="getResponseTimePercent(currentAgent.avgResponseTimeMs || currentAgent.avgResponseTime)"
              :color="getResponseTimeColor(currentAgent.avgResponseTimeMs || currentAgent.avgResponseTime)"
            />
          </div>
          <div class="metric-item">
            <span class="metric-label">错误率</span>
            <el-progress
              :percentage="getErrorRatePercent(currentAgent.errorRate || 0)"
              :color="getErrorRateColor(currentAgent.errorRate || 0)"
            />
          </div>
        </div>

        <!-- 告警历史 -->
        <el-divider>告警历史</el-divider>
        <div class="alert-history">
          <el-timeline v-if="currentAgent.alerts?.length">
            <el-timeline-item
              v-for="alert in currentAgent.alerts"
              :key="alert.id"
              :timestamp="formatTime(alert.createdAt)"
              :type="alert.level === 'ERROR' ? 'danger' : 'warning'"
            >
              {{ alert.message }}
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无告警记录" :image-size="60" />
        </div>

        <div class="drawer-actions">
          <el-button type="warning" @click="handleRestart(currentAgent)" v-permission="'agents:manage'">
            <el-icon><RefreshRight /></el-icon> 重启 Agent
          </el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
/**
 * Agent 健康监控页面
 * 查看 Agent 健康状态、响应时间、错误统计
 *
 * 功能：
 * - 统计概览（健康、警告、不健康、离线）
 * - 健康率展示
 * - Agent 健康列表（支持搜索和筛选）
 * - 健康详情（侧边抽屉）
 * - 健康指标可视化
 * - 告警历史
 * - 自动刷新
 * - 批量重启
 *
 * 组件复用说明：
 * - ProjectSelector: 项目选择器组件
 *   位置: @/components/ProjectSelector.vue
 *   用途: 选择要监控的项目
 *
 * 操作维度：系统级/项目级
 * 权限要求：agents:view
 */
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { healthApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleCheck, WarningFilled, CircleClose, RemoveFilled,
  Search, Refresh, RefreshRight
} from '@element-plus/icons-vue'
import ProjectSelector from '@/components/ProjectSelector.vue'

const loading = ref(false)
const agents = ref([])
const stats = ref({})
const selectedProjectId = ref(localStorage.getItem('selectedProjectId') || '')

// 搜索和筛选
const searchKeyword = ref('')
const filterStatus = ref('')
const selectedAgents = ref([])

// 详情抽屉
const drawerVisible = ref(false)
const currentAgent = ref(null)

// 自动刷新
const autoRefresh = ref(false)
let refreshTimer = null

// 筛选后的 Agent
const filteredAgents = computed(() => {
  let result = agents.value

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(a =>
      a.agentName?.toLowerCase().includes(keyword) ||
      a.agentId?.toLowerCase().includes(keyword) ||
      a.agentRole?.toLowerCase().includes(keyword)
    )
  }

  if (filterStatus.value) {
    result = result.filter(a => a.healthStatus === filterStatus.value)
  }

  return result
})

/** 健康率颜色 */
const healthRateColor = computed(() => {
  const rate = stats.value.healthRate || 0
  if (rate >= 80) return '#67c23a'
  if (rate >= 60) return '#e6a23c'
  return '#f56c6c'
})

/** 健康率类型 */
const healthRateType = computed(() => {
  const rate = stats.value.healthRate || 0
  if (rate >= 80) return 'success'
  if (rate >= 60) return 'warning'
  return 'danger'
})

/** 健康率标签 */
const healthRateLabel = computed(() => {
  const rate = stats.value.healthRate || 0
  if (rate >= 90) return '优秀'
  if (rate >= 80) return '良好'
  if (rate >= 60) return '一般'
  return '警告'
})

/** 获取角色标签类型 */
const getRoleTagType = (role) => {
  const typeMap = {
    'producer': 'danger',
    'server-dev': 'primary',
    'client-dev': 'success',
    'ui-dev': 'warning',
    'system-planner': 'info',
    'numerical-planner': 'info',
    'tester': '',
    'git-commit': 'info',
    'security-expert': 'danger',
    'data-analyst': 'success',
    'tech-artist': 'warning',
    'product-manager': 'primary',
    'localization': '',
    'ai-engineer': 'primary',
    'performance-engineer': 'warning',
    'audio-dev': 'success',
    'narrative-planner': 'info',
    'level-design': 'warning',
    'devops': 'primary'
  }
  return typeMap[role] || ''
}

/** 获取角色标签文本 */
const getRoleLabel = (role) => {
  const labelMap = {
    'producer': '制作人',
    'server-dev': '服务端',
    'client-dev': '客户端',
    'ui-dev': 'UI设计',
    'system-planner': '系统策划',
    'numerical-planner': '数值策划',
    'tester': '测试',
    'git-commit': 'Git专员',
    'security-expert': '安全工程师',
    'data-analyst': '数据分析师',
    'tech-artist': '技术美术',
    'product-manager': '产品经理',
    'localization': '本地化',
    'ai-engineer': 'AI工程师',
    'performance-engineer': '性能优化',
    'audio-dev': '音频设计',
    'narrative-planner': '剧情策划',
    'level-design': '关卡设计',
    'devops': '运维工程师'
  }
  return labelMap[role] || role
}

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

/** 获取状态样式类 */
const getStatusClass = (status) => {
  return {
    'status-healthy': status === 'HEALTHY',
    'status-warning': status === 'WARNING',
    'status-unhealthy': status === 'UNHEALTHY',
    'status-offline': status === 'OFFLINE'
  }
}

/** 获取行样式 */
const getRowClassName = ({ row }) => {
  if (row.healthStatus === 'UNHEALTHY') return 'unhealthy-row'
  if (row.healthStatus === 'WARNING') return 'warning-row'
  return ''
}

/** 获取响应时间百分比 */
const getResponseTimePercent = (time) => {
  if (!time) return 0
  // 5秒为100%
  return Math.min(100, (time / 5000) * 100)
}

/** 获取响应时间颜色 */
const getResponseTimeColor = (time) => {
  if (!time) return '#67c23a'
  if (time < 1000) return '#67c23a'
  if (time < 3000) return '#e6a23c'
  return '#f56c6c'
}

/** 获取错误率百分比（直接使用 errorRate 百分比值） */
const getErrorRatePercent = (rate) => {
  if (!rate) return 0
  return Math.min(100, Math.round(rate))
}

/** 获取错误率颜色 */
const getErrorRateColor = (rate) => {
  if (!rate) return '#67c23a'
  if (rate < 10) return '#67c23a'
  if (rate < 30) return '#e6a23c'
  return '#f56c6c'
}

/** 获取告警原因 */
const getWarningReason = (agent) => {
  const reasons = []
  if (agent.consecutiveErrors >= 3) reasons.push(`连续 ${agent.consecutiveErrors} 次错误`)
  if (agent.errorRate > 20) reasons.push(`错误率 ${agent.errorRate.toFixed(1)}%`)
  if ((agent.avgResponseTimeMs || agent.avgResponseTime) > 5000) reasons.push('响应缓慢')
  if (agent.lastErrorMessage) reasons.push(agent.lastErrorMessage.substring(0, 50))
  return reasons.length > 0 ? reasons.join('；') : '需关注'
}

/** 项目切换 */
const handleProjectChange = (projectId) => {
  loadHealth()
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
    const params = {}
    if (selectedProjectId.value) {
      params.projectId = selectedProjectId.value
    }

    const [healthData, statsData] = await Promise.all([
      healthApi.getAll(params),
      healthApi.getStats(params)
    ])

    // 按项目筛选数据
    let filteredAgents = healthData || []
    if (selectedProjectId.value) {
      filteredAgents = filteredAgents.filter(agent =>
        agent.projectId === selectedProjectId.value
      )
    }

    agents.value = filteredAgents

    // 重新计算统计数据（基于筛选后的数据）
    const filteredStats = {
      healthyCount: filteredAgents.filter(a => a.healthStatus === 'HEALTHY').length,
      warningCount: filteredAgents.filter(a => a.healthStatus === 'WARNING').length,
      unhealthyCount: filteredAgents.filter(a => a.healthStatus === 'UNHEALTHY').length,
      offlineCount: filteredAgents.filter(a => a.healthStatus === 'OFFLINE').length,
      totalAgents: filteredAgents.length
    }
    filteredStats.healthRate = filteredStats.totalAgents > 0
      ? Math.round((filteredStats.healthyCount / filteredStats.totalAgents) * 100 * 100) / 100
      : 0

    stats.value = filteredStats
  } catch (error) {
    ElMessage.error('加载健康数据失败')
  } finally {
    loading.value = false
  }
}

/** 表格多选变化 */
const handleSelectionChange = (selection) => {
  selectedAgents.value = selection
}

/** 查看详情 */
const handleViewDetail = (agent) => {
  currentAgent.value = agent
  drawerVisible.value = true
}

/** 重启 Agent */
const handleRestart = async (agent) => {
  try {
    await ElMessageBox.confirm(
      `确定要重启 Agent "${agent.agentName || agent.agentId}" 吗？`,
      '重启确认',
      { confirmButtonText: '重启', cancelButtonText: '取消', type: 'warning' }
    )

    await healthApi.restartAgent(agent.agentId)
    ElMessage.success('Agent 重启命令已发送')
    drawerVisible.value = false
    loadHealth()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('重启失败')
    }
  }
}

/** 批量重启 */
const handleBatchRestart = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要重启选中的 ${selectedAgents.value.length} 个 Agent 吗？`,
      '批量重启',
      { confirmButtonText: '重启', cancelButtonText: '取消', type: 'warning' }
    )

    for (const agent of selectedAgents.value) {
      await healthApi.restartAgent(agent.agentId)
    }

    ElMessage.success('批量重启命令已发送')
    loadHealth()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量重启失败')
    }
  }
}

/** 自动刷新切换 */
const handleAutoRefreshChange = (val) => {
  if (val) {
    refreshTimer = setInterval(loadHealth, 10000) // 每10秒刷新
    ElMessage.success('已开启自动刷新（每10秒）')
  } else {
    if (refreshTimer) {
      clearInterval(refreshTimer)
      refreshTimer = null
    }
    ElMessage.info('已关闭自动刷新')
  }
}

onMounted(() => {
  loadHealth()
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})
</script>

<style scoped>
.health-page {
  padding: 20px;
}

/* 统计卡片 */
.stat-cards {
  margin-bottom: 16px;
}

.stat-card {
  cursor: default;
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  min-height: 80px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: var(--el-text-color-primary);
  line-height: 1.2;
  white-space: nowrap;
}

.stat-value.success { color: var(--el-color-success); }
.stat-value.warning { color: var(--el-color-warning); }
.stat-value.danger { color: var(--el-color-danger); }
.stat-value.info { color: var(--el-color-info); }

.stat-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
  white-space: nowrap;
}

/* 健康率 */
.health-rate-card {
  margin-bottom: 16px;
}

.health-rate {
  display: flex;
  align-items: center;
  gap: 20px;
}

.rate-info {
  display: flex;
  flex-direction: column;
  min-width: 100px;
}

.rate-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.rate-value {
  font-size: 28px;
  font-weight: bold;
  color: var(--el-text-color-primary);
}

/* 卡片头部 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

/* 筛选区 */
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}

/* 健康状态 */
.health-status {
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-dot.status-healthy { background: var(--el-color-success); }
.status-dot.status-warning { background: var(--el-color-warning); }
.status-dot.status-unhealthy { background: var(--el-color-danger); }
.status-dot.status-offline { background: var(--el-color-info); }

/* 错误计数 */
.error-count {
  color: var(--el-color-danger);
  font-weight: bold;
}

/* 慢响应 */
.slow-response {
  color: var(--el-color-warning);
}

/* 告警原因 */
.warning-reason {
  color: var(--el-color-danger);
  font-size: 12px;
}

.text-muted {
  color: var(--el-text-color-placeholder);
}

/* 行样式 */
:deep(.unhealthy-row) {
  background-color: var(--el-color-danger-light-9) !important;
}

:deep(.warning-row) {
  background-color: var(--el-color-warning-light-9) !important;
}

/* 健康指标 */
.health-metrics {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.metric-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.metric-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

/* 告警历史 */
.alert-history {
  max-height: 300px;
  overflow-y: auto;
}

/* 抽屉操作 */
.drawer-actions {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}

/* 响应式 */
@media (max-width: 767px) {
  .health-page {
    padding: 12px;
  }

  .health-rate {
    flex-direction: column;
    align-items: flex-start;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
  }

  .filter-bar {
    flex-direction: column;
  }

  :deep(.el-dialog),
  :deep(.el-drawer) {
    width: 90% !important;
  }
}
</style>
