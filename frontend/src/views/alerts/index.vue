<template>
  <div class="alerts-page">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-value text-warning">{{ stats.pending || 0 }}</div>
            <div class="stat-label">待处理</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-value text-info">{{ stats.acknowledged || 0 }}</div>
            <div class="stat-label">已确认</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-value text-success">{{ stats.resolved || 0 }}</div>
            <div class="stat-label">已解决</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-value text-danger">{{ stats.critical || 0 }}</div>
            <div class="stat-label">紧急告警</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>告警中心</span>
          <div class="header-actions">
            <el-button @click="handleBatchAcknowledge" :loading="batchLoading" v-if="stats.pending > 0">
              <el-icon><Check /></el-icon> 全部确认
            </el-button>
            <el-button @click="loadAlerts" :loading="loading">
              <el-icon><Refresh /></el-icon> 刷新
            </el-button>
          </div>
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
      <el-table :data="alerts" v-loading="loading" stripe @row-click="handleViewDetail">
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="alert-title-cell">
              <el-icon :class="getPriorityIconClass(row.priority)">
                <Warning v-if="row.priority === 'CRITICAL' || row.priority === 'HIGH'" />
                <InfoFilled v-else />
              </el-icon>
              <span>{{ row.title }}</span>
            </div>
          </template>
        </el-table-column>
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
        <el-table-column label="Agent" width="120" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.agentName || row.agentId || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="触发值" width="100">
          <template #default="{ row }">
            <span v-if="row.triggerValue != null" class="trigger-value">
              {{ formatValue(row.triggerValue) }}
            </span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="阈值" width="100">
          <template #default="{ row }">
            <span v-if="row.thresholdValue != null" class="threshold-value">
              {{ formatValue(row.thresholdValue) }}
            </span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="触发时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING'"
              type="warning"
              size="small"
              text
              @click.stop="handleAcknowledge(row)"
            >
              确认
            </el-button>
            <el-button
              v-if="row.status !== 'RESOLVED'"
              type="success"
              size="small"
              text
              @click.stop="handleResolve(row)"
            >
              解决
            </el-button>
            <el-button
              type="primary"
              size="small"
              text
              @click.stop="handleViewDetail(row)"
            >
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <el-empty v-if="!loading && alerts.length === 0" description="暂无告警记录">
        <template #image>
          <el-icon :size="60" color="#67c23a"><CircleCheck /></el-icon>
        </template>
        <div class="empty-text">系统运行正常，暂无告警</div>
      </el-empty>
    </el-card>

    <!-- 告警详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="告警详情"
      width="700px"
    >
      <template v-if="currentAlert">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="告警ID">{{ currentAlert.id }}</el-descriptions-item>
          <el-descriptions-item label="规则名称">{{ currentAlert.ruleName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="优先级" :span="1">
            <el-tag :type="getPriorityType(currentAlert.priority)" size="small">
              {{ getPriorityLabel(currentAlert.priority) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态" :span="1">
            <el-tag :type="getStatusType(currentAlert.status)" size="small">
              {{ getStatusLabel(currentAlert.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="监控指标">{{ currentAlert.metric || '-' }}</el-descriptions-item>
          <el-descriptions-item label="Agent">{{ currentAlert.agentName || currentAlert.agentId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="触发值">
            <span class="trigger-value">{{ formatValue(currentAlert.triggerValue) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="阈值">
            <span class="threshold-value">{{ formatValue(currentAlert.thresholdValue) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="触发时间" :span="2">{{ formatTime(currentAlert.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="标题" :span="2">{{ currentAlert.title }}</el-descriptions-item>
        </el-descriptions>

        <!-- 告警详情 -->
        <div class="alert-detail-section">
          <h4>告警详情</h4>
          <div class="detail-content" v-if="currentAlert.detail">
            <pre>{{ currentAlert.detail }}</pre>
          </div>
          <div v-else class="no-detail">暂无详细信息</div>
        </div>

        <!-- 解决信息 -->
        <div v-if="currentAlert.resolution" class="alert-resolution-section">
          <h4>解决方案</h4>
          <div class="resolution-content">
            <p>{{ currentAlert.resolution }}</p>
            <div class="resolution-meta">
              <span>解决人: {{ currentAlert.resolvedBy || '-' }}</span>
              <span>解决时间: {{ formatTime(currentAlert.resolvedAt) }}</span>
            </div>
          </div>
        </div>
      </template>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button
          v-if="currentAlert?.status === 'PENDING'"
          type="warning"
          @click="handleAcknowledge(currentAlert)"
        >
          确认
        </el-button>
        <el-button
          v-if="currentAlert?.status !== 'RESOLVED'"
          type="success"
          @click="handleResolve(currentAlert)"
        >
          解决
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 告警中心页面
 * 查看和管理告警
 *
 * 优化内容：
 * - 统计概览卡片
 * - 告警详情展示
 * - 批量操作
 * - 空状态优化
 *
 * @author chengxun
 * @since 1.0.0
 */
import { ref, onMounted } from 'vue'
import { alertApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Warning, InfoFilled, CircleCheck, Check, Refresh } from '@element-plus/icons-vue'

const loading = ref(false)
const batchLoading = ref(false)
const alerts = ref([])
const filterStatus = ref('')
const filterPriority = ref('')

// 统计数据
const stats = ref({
  pending: 0,
  acknowledged: 0,
  resolved: 0,
  critical: 0
})

// 详情对话框
const detailDialogVisible = ref(false)
const currentAlert = ref(null)

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

/** 获取优先级图标样式 */
const getPriorityIconClass = (priority) => {
  const classMap = {
    'CRITICAL': 'priority-critical',
    'HIGH': 'priority-high',
    'MEDIUM': 'priority-medium',
    'LOW': 'priority-low'
  }
  return classMap[priority] || ''
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

/** 格式化数值 */
const formatValue = (value) => {
  if (value == null) return '-'
  if (value >= 10000) return (value / 1000).toFixed(1) + 'K'
  if (value >= 1000) return (value / 1000).toFixed(2) + 'K'
  return value.toFixed(1)
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

    // 更新统计
    updateStats()
  } catch (error) {
    ElMessage.error('加载告警失败')
  } finally {
    loading.value = false
  }
}

/** 更新统计 */
const updateStats = () => {
  const allAlerts = alerts.value
  stats.value = {
    pending: allAlerts.filter(a => a.status === 'PENDING').length,
    acknowledged: allAlerts.filter(a => a.status === 'ACKNOWLEDGED').length,
    resolved: allAlerts.filter(a => a.status === 'RESOLVED').length,
    critical: allAlerts.filter(a => a.priority === 'CRITICAL').length
  }
}

/** 查看告警详情 */
const handleViewDetail = (alert) => {
  currentAlert.value = alert
  detailDialogVisible.value = true
}

/** 确认告警 */
const handleAcknowledge = async (alert) => {
  try {
    await alertApi.acknowledgeAlert(alert.id)
    ElMessage.success('告警已确认')
    detailDialogVisible.value = false
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
      inputPlaceholder: '解决方案',
      inputType: 'textarea'
    })

    await alertApi.resolveAlert(alert.id, { resolution })
    ElMessage.success('告警已解决')
    detailDialogVisible.value = false
    loadAlerts()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

/** 批量确认所有待处理告警 */
const handleBatchAcknowledge = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要确认所有 ${stats.value.pending} 条待处理告警吗？`,
      '批量确认',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
    )

    batchLoading.value = true
    const result = await alertApi.batchAcknowledge()
    ElMessage.success(`已确认 ${result || stats.value.pending} 条告警`)
    loadAlerts()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量确认失败')
    }
  } finally {
    batchLoading.value = false
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

.stat-cards {
  margin-bottom: 16px;
}

.stat-card {
  cursor: default;
}

.stat-card :deep(.el-card__body) {
  padding: 16px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

.text-warning { color: var(--el-color-warning); }
.text-info { color: var(--el-color-info); }
.text-success { color: var(--el-color-success); }
.text-danger { color: var(--el-color-danger); }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.alert-title-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.priority-critical { color: var(--el-color-danger); }
.priority-high { color: var(--el-color-warning); }
.priority-medium { color: var(--el-color-info); }
.priority-low { color: var(--el-color-success); }

.trigger-value {
  color: var(--el-color-danger);
  font-weight: 500;
}

.threshold-value {
  color: var(--el-color-info);
}

.text-muted {
  color: var(--el-text-color-placeholder);
}

.empty-text {
  color: var(--el-text-color-secondary);
  margin-top: 8px;
}

/* 详情对话框 */
.alert-detail-section,
.alert-resolution-section {
  margin-top: 16px;
}

.alert-detail-section h4,
.alert-resolution-section h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: var(--el-text-color-primary);
}

.detail-content {
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
  padding: 12px;
  max-height: 200px;
  overflow-y: auto;
}

.detail-content pre {
  margin: 0;
  font-family: monospace;
  font-size: 12px;
  color: var(--el-text-color-regular);
  white-space: pre-wrap;
  word-break: break-word;
}

.no-detail {
  color: var(--el-text-color-placeholder);
  font-size: 13px;
}

.resolution-content {
  background: var(--el-color-success-light-9);
  border-radius: 6px;
  padding: 12px;
}

.resolution-content p {
  margin: 0 0 8px 0;
  color: var(--el-text-color-regular);
}

.resolution-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

@media (max-width: 767px) {
  .stat-value {
    font-size: 20px;
  }

  .filter-bar {
    flex-direction: column;
  }

  .header-actions {
    flex-direction: column;
    width: 100%;
  }
}
</style>
