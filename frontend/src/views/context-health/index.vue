<template>
  <div class="context-health-page">
    <!-- 顶部操作栏 -->
    <el-card class="action-card">
      <div class="action-header">
        <div class="action-left">
          <el-button type="primary" @click="loadHealthStatus" :loading="loading">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
          <el-button @click="checkAllAgents" :loading="checkingAll">
            <el-icon><CircleCheck /></el-icon> 全部检查
          </el-button>
          <el-button type="warning" @click="recoverAllUnhealthy" :loading="recoveringAll" :disabled="unhealthyCount === 0">
            <el-icon><MagicStick /></el-icon> 批量恢复 ({{ unhealthyCount }})
          </el-button>
          <el-divider direction="vertical" />
          <el-switch
            v-model="autoRefresh"
            active-text="自动刷新"
            inactive-text=""
            @change="toggleAutoRefresh"
          />
          <el-select v-model="refreshInterval" size="small" style="width: 100px" @change="updateRefreshInterval">
            <el-option label="10秒" :value="10000" />
            <el-option label="30秒" :value="30000" />
            <el-option label="1分钟" :value="60000" />
            <el-option label="5分钟" :value="300000" />
          </el-select>
        </div>
        <div class="action-right">
          <el-tag type="info">上次刷新: {{ lastRefreshTime }}</el-tag>
        </div>
      </div>
    </el-card>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :xs="12" :sm="6" :md="4">
        <el-card shadow="hover" class="stat-card" @click="filterByStatus('all')">
          <div class="stat-item" :class="{ active: statusFilter === 'all' }">
            <div class="stat-value">{{ summary.total || 0 }}</div>
            <div class="stat-label">总 Agent</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6" :md="4">
        <el-card shadow="hover" class="stat-card" @click="filterByStatus('healthy')">
          <div class="stat-item" :class="{ active: statusFilter === 'healthy' }">
            <div class="stat-value success">{{ summary.healthy || 0 }}</div>
            <div class="stat-label">健康</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6" :md="4">
        <el-card shadow="hover" class="stat-card" @click="filterByStatus('warning')">
          <div class="stat-item" :class="{ active: statusFilter === 'warning' }">
            <div class="stat-value warning">{{ summary.warning || 0 }}</div>
            <div class="stat-label">警告</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6" :md="4">
        <el-card shadow="hover" class="stat-card" @click="filterByStatus('error')">
          <div class="stat-item" :class="{ active: statusFilter === 'error' }">
            <div class="stat-value danger">{{ summary.error || 0 }}</div>
            <div class="stat-label">错误</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6" :md="4">
        <el-card shadow="hover" class="stat-card" @click="filterByStatus('critical')">
          <div class="stat-item" :class="{ active: statusFilter === 'critical' }">
            <div class="stat-value critical">{{ summary.critical || 0 }}</div>
            <div class="stat-label">严重</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6" :md="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-value info">{{ summary.lastCheck ? '已检查' : '未检查' }}</div>
            <div class="stat-label">检查状态</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 健康状态列表 -->
    <el-card>
      <template #header>
        <div class="card-header">
          <span>Agent 上下文状态</span>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索 Agent ID..."
            clearable
            style="width: 200px"
            size="small"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
      </template>

      <el-table :data="filteredHealthStatuses" v-loading="loading" stripe highlight-current-row>
        <el-table-column prop="agentId" label="Agent ID" width="180" fixed>
          <template #default="{ row }">
            <div class="agent-id-cell">
              <el-icon :size="16" :color="getAgentIconColor(row)">
                <component :is="getAgentIcon(row)" />
              </el-icon>
              <code>{{ row.agentId }}</code>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row)" size="small" effect="dark">
              {{ getStatusText(row) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="严重程度" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="!row.healthy" :type="getSeverityType(row.severity)" size="small">
              {{ row.severity }}
            </el-tag>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>

        <el-table-column prop="issue" label="问题描述" min-width="250" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.issue" class="issue-text">{{ row.issue }}</span>
            <span v-else class="text-muted">无异常</span>
          </template>
        </el-table-column>

        <el-table-column label="上次检查" width="170">
          <template #default="{ row }">
            <div class="time-cell">
              <el-icon><Clock /></el-icon>
              <span>{{ formatTime(row.lastCheck) }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="上次恢复" width="170">
          <template #default="{ row }">
            <div class="time-cell" v-if="row.lastRecovery">
              <el-icon><RefreshRight /></el-icon>
              <span>{{ formatTime(row.lastRecovery) }}</span>
            </div>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-tooltip content="手动检查健康状态" placement="top">
                <el-button
                  type="primary"
                  size="small"
                  text
                  @click="checkSingleAgent(row)"
                  :loading="row._checking"
                >
                  <el-icon><CircleCheck /></el-icon> 检查
                </el-button>
              </el-tooltip>

              <el-tooltip content="恢复上下文（轻度恢复）" placement="top">
                <el-button
                  v-if="!row.healthy"
                  type="warning"
                  size="small"
                  text
                  @click="handleRecover(row)"
                  :loading="row._recovering"
                >
                  <el-icon><MagicStick /></el-icon> 恢复
                </el-button>
              </el-tooltip>

              <el-tooltip content="重建上下文（彻底重建）" placement="top">
                <el-button
                  v-if="!row.healthy"
                  type="danger"
                  size="small"
                  text
                  @click="handleRebuild(row)"
                  :loading="row._rebuilding"
                >
                  <el-icon><RefreshRight /></el-icon> 重建
                </el-button>
              </el-tooltip>

              <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, row)">
                <el-button type="info" size="small" text>
                  <el-icon><MoreFilled /></el-icon> 更多
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="detail">
                      <el-icon><View /></el-icon> 查看详情
                    </el-dropdown-item>
                    <el-dropdown-item command="logs">
                      <el-icon><Document /></el-icon> 查看日志
                    </el-dropdown-item>
                    <el-dropdown-item command="reset" divided>
                      <el-icon><RefreshLeft /></el-icon> 重置恢复计数
                    </el-dropdown-item>
                    <el-dropdown-item command="stop" divided>
                      <el-icon><SwitchButton /></el-icon> 停止 Agent
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="filteredHealthStatuses.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无 Agent 数据">
          <el-button type="primary" @click="loadHealthStatus">刷新数据</el-button>
        </el-empty>
      </div>
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="Agent 详情" width="600px">
      <div v-if="currentAgent" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="Agent ID">{{ currentAgent.agentId }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentAgent)">{{ getStatusText(currentAgent) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="严重程度" v-if="!currentAgent.healthy">
            <el-tag :type="getSeverityType(currentAgent.severity)">{{ currentAgent.severity }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="问题描述" :span="2">
            {{ currentAgent.issue || '无异常' }}
          </el-descriptions-item>
          <el-descriptions-item label="上次检查">{{ formatTime(currentAgent.lastCheck) }}</el-descriptions-item>
          <el-descriptions-item label="上次恢复">{{ formatTime(currentAgent.lastRecovery) || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="detail-actions" v-if="!currentAgent.healthy">
          <h4>恢复操作</h4>
          <el-space>
            <el-button type="warning" @click="handleRecover(currentAgent)" :loading="currentAgent._recovering">
              <el-icon><MagicStick /></el-icon> 恢复上下文
            </el-button>
            <el-button type="danger" @click="handleRebuild(currentAgent)" :loading="currentAgent._rebuilding">
              <el-icon><RefreshRight /></el-icon> 重建上下文
            </el-button>
          </el-space>
          <p class="tip">恢复：尝试修复当前上下文问题<br/>重建：销毁并重新创建上下文（更彻底但耗时更长）</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 上下文监控页面
 * 监控 Agent 上下文健康状态，提供丰富的操作功能
 *
 * 功能：
 * - 实时健康状态监控
 * - 手动检查/恢复/重建上下文
 * - 批量操作
 * - 自动刷新
 * - 详情查看
 */
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh, CircleCheck, MagicStick, Search, Clock, RefreshRight,
  MoreFilled, View, Document, RefreshLeft, SwitchButton,
  User, Cpu, Monitor, Setting
} from '@element-plus/icons-vue'
import api from '@/api'

const loading = ref(false)
const checkingAll = ref(false)
const recoveringAll = ref(false)
const summary = ref({})
const healthStatuses = ref([])
const searchKeyword = ref('')
const statusFilter = ref('all')
const autoRefresh = ref(true)
const refreshInterval = ref(30000)
const lastRefreshTime = ref('-')
const detailDialogVisible = ref(false)
const currentAgent = ref(null)

let refreshTimer = null

// 计算属性：不健康的 Agent 数量
const unhealthyCount = computed(() => {
  return healthStatuses.value.filter(s => !s.healthy).length
})

// 计算属性：过滤后的状态列表
const filteredHealthStatuses = computed(() => {
  let list = healthStatuses.value

  // 按状态过滤
  if (statusFilter.value !== 'all') {
    list = list.filter(s => {
      if (statusFilter.value === 'healthy') return s.healthy
      if (statusFilter.value === 'warning') return !s.healthy && s.severity === 'WARNING'
      if (statusFilter.value === 'error') return !s.healthy && s.severity === 'ERROR'
      if (statusFilter.value === 'critical') return !s.healthy && s.severity === 'CRITICAL'
      return true
    })
  }

  // 按关键词搜索
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    list = list.filter(s => s.agentId.toLowerCase().includes(keyword))
  }

  return list
})

// 格式化时间
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

// 获取状态类型
const getStatusType = (row) => {
  if (row.healthy) return 'success'
  const severityMap = {
    'WARNING': 'warning',
    'ERROR': 'danger',
    'CRITICAL': 'danger'
  }
  return severityMap[row.severity] || 'info'
}

// 获取状态文本
const getStatusText = (row) => {
  if (row.healthy) return '健康'
  const severityMap = {
    'WARNING': '警告',
    'ERROR': '错误',
    'CRITICAL': '严重'
  }
  return severityMap[row.severity] || '异常'
}

// 获取严重程度类型
const getSeverityType = (severity) => {
  const map = {
    'WARNING': 'warning',
    'ERROR': 'danger',
    'CRITICAL': 'danger'
  }
  return map[severity] || 'info'
}

// 获取 Agent 图标颜色
const getAgentIconColor = (row) => {
  if (row.healthy) return '#67c23a'
  const map = {
    'WARNING': '#e6a23c',
    'ERROR': '#f56c6c',
    'CRITICAL': '#f56c6c'
  }
  return map[row.severity] || '#909399'
}

// 获取 Agent 图标
const getAgentIcon = (row) => {
  if (row.agentId.includes('producer')) return User
  if (row.agentId.includes('dev')) return Cpu
  if (row.agentId.includes('planner')) return Setting
  return Monitor
}

// 过滤状态
const filterByStatus = (status) => {
  statusFilter.value = statusFilter.value === status ? 'all' : status
}

// 加载健康状态
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
      ...status,
      _checking: false,
      _recovering: false,
      _rebuilding: false
    }))
    lastRefreshTime.value = new Date().toLocaleTimeString('zh-CN')
  } catch (error) {
    ElMessage.error('加载健康状态失败')
  } finally {
    loading.value = false
  }
}

// 检查单个 Agent
const checkSingleAgent = async (row) => {
  row._checking = true
  try {
    await api.post(`/capabilities/health/${row.agentId}/check`)
    ElMessage.success(`${row.agentId} 检查完成`)
    await loadHealthStatus()
  } catch (error) {
    ElMessage.error('检查失败')
  } finally {
    row._checking = false
  }
}

// 检查所有 Agent
const checkAllAgents = async () => {
  checkingAll.value = true
  try {
    await api.post('/capabilities/health/check-all')
    ElMessage.success('全部检查完成')
    await loadHealthStatus()
  } catch (error) {
    ElMessage.error('检查失败')
  } finally {
    checkingAll.value = false
  }
}

// 恢复单个 Agent
const handleRecover = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要恢复 Agent "${row.agentId}" 的上下文吗？`,
      '恢复确认',
      { confirmButtonText: '恢复', cancelButtonText: '取消', type: 'warning' }
    )
    row._recovering = true
    const result = await api.post(`/capabilities/health/${row.agentId}/recover`)
    if (result.success) {
      ElMessage.success('恢复已触发')
      await loadHealthStatus()
    } else {
      ElMessage.error(result.message || '恢复失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('恢复失败')
    }
  } finally {
    row._recovering = false
  }
}

// 重建上下文
const handleRebuild = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要重建 Agent "${row.agentId}" 的上下文吗？这将销毁当前上下文并重新创建。`,
      '重建确认',
      { confirmButtonText: '重建', cancelButtonText: '取消', type: 'warning' }
    )
    row._rebuilding = true
    const result = await api.post(`/capabilities/health/${row.agentId}/rebuild`)
    if (result.success) {
      ElMessage.success('重建已触发')
      await loadHealthStatus()
    } else {
      ElMessage.error(result.message || '重建失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('重建失败')
    }
  } finally {
    row._rebuilding = false
  }
}

// 批量恢复不健康的 Agent
const recoverAllUnhealthy = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要恢复所有 ${unhealthyCount.value} 个不健康的 Agent 吗？`,
      '批量恢复确认',
      { confirmButtonText: '全部恢复', cancelButtonText: '取消', type: 'warning' }
    )
    recoveringAll.value = true
    const unhealthyAgents = healthStatuses.value.filter(s => !s.healthy)
    let successCount = 0
    for (const agent of unhealthyAgents) {
      try {
        await api.post(`/capabilities/health/${agent.agentId}/recover`)
        successCount++
      } catch (e) {
        // 忽略单个失败
      }
    }
    ElMessage.success(`已恢复 ${successCount}/${unhealthyAgents.length} 个 Agent`)
    await loadHealthStatus()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量恢复失败')
    }
  } finally {
    recoveringAll.value = false
  }
}

// 处理更多操作命令
const handleCommand = async (command, row) => {
  switch (command) {
    case 'detail':
      currentAgent.value = row
      detailDialogVisible.value = true
      break
    case 'logs':
      // 跳转到 Agent 日志页面
      window.open(`/admin/agent-logs?agentId=${row.agentId}`, '_blank')
      break
    case 'reset':
      await resetRecoveryCount(row)
      break
    case 'stop':
      await stopAgent(row)
      break
  }
}

// 重置恢复计数
const resetRecoveryCount = async (row) => {
  try {
    await api.post(`/capabilities/health/${row.agentId}/reset-recovery`)
    ElMessage.success('恢复计数已重置')
    await loadHealthStatus()
  } catch (error) {
    ElMessage.error('重置失败')
  }
}

// 停止 Agent
const stopAgent = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要停止 Agent "${row.agentId}" 吗？停止后需要手动启动。`,
      '停止确认',
      { confirmButtonText: '停止', cancelButtonText: '取消', type: 'danger' }
    )
    await api.post(`/agents/${row.agentId}/stop`)
    ElMessage.success('Agent 已停止')
    await loadHealthStatus()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('停止失败')
    }
  }
}

// 切换自动刷新
const toggleAutoRefresh = (val) => {
  if (val) {
    startAutoRefresh()
  } else {
    stopAutoRefresh()
  }
}

// 更新刷新间隔
const updateRefreshInterval = () => {
  if (autoRefresh.value) {
    stopAutoRefresh()
    startAutoRefresh()
  }
}

// 开始自动刷新
const startAutoRefresh = () => {
  stopAutoRefresh()
  refreshTimer = setInterval(loadHealthStatus, refreshInterval.value)
}

// 停止自动刷新
const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

onMounted(() => {
  loadHealthStatus()
  if (autoRefresh.value) {
    startAutoRefresh()
  }
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style scoped>
.context-health-page {
  padding: 20px;
}

.action-card {
  margin-bottom: 16px;
}

.action-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.action-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.action-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stat-cards {
  margin-bottom: 16px;
}

.stat-card {
  cursor: pointer;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-item {
  text-align: center;
  padding: 16px;
  border-radius: 8px;
  transition: all 0.3s;
}

.stat-item.active {
  background: var(--el-color-primary-light-9);
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: var(--el-text-color-primary);
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

.stat-value.critical {
  color: #f56c6c;
  animation: pulse 2s infinite;
}

.stat-value.info {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.stat-label {
  margin-top: 8px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.agent-id-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.agent-id-cell code {
  font-size: 13px;
  background: var(--el-fill-color-light);
  padding: 2px 6px;
  border-radius: 4px;
  color: var(--el-text-color-primary);
}

.issue-text {
  color: var(--el-color-danger);
}

.text-muted {
  color: var(--el-text-color-placeholder);
}

.time-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 4px;
}

.empty-state {
  padding: 40px 0;
}

.detail-content {
  padding: 16px 0;
}

.detail-actions {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.detail-actions h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: var(--el-text-color-primary);
}

.detail-actions .tip {
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}

/* 响应式 */
@media (max-width: 768px) {
  .action-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .stat-value {
    font-size: 20px;
  }
}
</style>
