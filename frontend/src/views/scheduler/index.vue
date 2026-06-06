<template>
  <div class="scheduler-page">
    <!-- 统计概览 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-primary-light-9)">
            <el-icon :size="24" color="var(--el-color-primary)"><User /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ status.totalAgents || 0 }}</div>
            <div class="stat-label">Agent 总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-success-light-9)">
            <el-icon :size="24" color="var(--el-color-success)"><VideoPlay /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ status.runningAgents || 0 }}</div>
            <div class="stat-label">运行中</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-warning-light-9)">
            <el-icon :size="24" color="var(--el-color-warning)"><Loading /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ status.busyAgents || 0 }}</div>
            <div class="stat-label">忙碌中</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-info-light-9)">
            <el-icon :size="24" color="var(--el-color-info)"><Coffee /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ status.idleAgents || 0 }}</div>
            <div class="stat-label">空闲中</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 任务队列 -->
    <el-card>
      <template #header>
        <div class="card-header">
          <span>任务队列</span>
          <div class="header-actions">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索任务..."
              clearable
              style="width: 200px"
              :prefix-icon="Search"
            />
            <el-select v-model="filterStatus" placeholder="任务状态" clearable style="width: 120px">
              <el-option label="全部" value="" />
              <el-option label="待处理" value="PENDING" />
              <el-option label="处理中" value="PROCESSING" />
              <el-option label="已完成" value="COMPLETED" />
              <el-option label="失败" value="FAILED" />
            </el-select>
            <el-button type="primary" @click="handleTrigger" :loading="triggering" v-permission="'agents:manage'">
              <el-icon><VideoPlay /></el-icon> 手动触发
            </el-button>
            <el-button @click="loadStatus" :loading="loading">
              <el-icon><Refresh /></el-icon> 刷新
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="filteredTaskQueue" v-loading="loading" stripe @row-click="handleViewTask">
        <el-table-column prop="taskId" label="任务 ID" width="150" show-overflow-tooltip />
        <el-table-column prop="agentId" label="Agent" width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.agentName || row.agentId }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="任务标题" min-width="150" show-overflow-tooltip />
        <el-table-column label="优先级" width="80">
          <template #default="{ row }">
            <el-tag :type="getPriorityType(row.priority)" size="small">
              {{ row.priority || 'NORMAL' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click.stop="handleViewTask(row)">详情</el-button>
            <el-button
              v-if="row.status === 'PENDING' || row.status === 'PROCESSING'"
              type="danger"
              size="small"
              text
              @click.stop="handleCancelTask(row)"
              v-permission="'agents:manage'"
            >
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && filteredTaskQueue.length === 0" description="暂无待处理任务" />
    </el-card>

    <!-- 调度配置 -->
    <el-card class="mt-4">
      <template #header>
        <div class="card-header">
          <span>调度配置</span>
          <el-tag type="info" size="small">上次调度: {{ formatTime(status.lastScheduleTime) }}</el-tag>
        </div>
      </template>
      <el-form :model="config" label-width="150px" style="max-width: 600px">
        <el-form-item label="调度间隔（秒）">
          <el-input-number v-model="config.intervalSeconds" :min="10" :max="3600" />
          <span class="form-tip">Agent 检查任务的间隔时间</span>
        </el-form-item>
        <el-form-item label="最大并发任务数">
          <el-input-number v-model="config.maxConcurrentTasks" :min="1" :max="100" />
          <span class="form-tip">同时处理的最大任务数量</span>
        </el-form-item>
        <el-form-item label="任务超时（分钟）">
          <el-input-number v-model="config.taskTimeoutMinutes" :min="1" :max="1440" />
          <span class="form-tip">任务执行超时时间</span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSaveConfig" :loading="savingConfig">保存配置</el-button>
          <el-button @click="handleResetConfig">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 任务详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :title="currentTask?.title || '任务详情'"
      size="450px"
      direction="rtl"
    >
      <template v-if="currentTask">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="任务 ID">{{ currentTask.taskId }}</el-descriptions-item>
          <el-descriptions-item label="Agent">
            {{ currentTask.agentName || currentTask.agentId }}
          </el-descriptions-item>
          <el-descriptions-item label="任务标题">{{ currentTask.title }}</el-descriptions-item>
          <el-descriptions-item label="优先级">
            <el-tag :type="getPriorityType(currentTask.priority)" size="small">
              {{ currentTask.priority || 'NORMAL' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentTask.status)" size="small">
              {{ getStatusLabel(currentTask.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(currentTask.createdAt) }}</el-descriptions-item>
          <el-descriptions-item v-if="currentTask.startedAt" label="开始时间">{{ formatTime(currentTask.startedAt) }}</el-descriptions-item>
          <el-descriptions-item v-if="currentTask.completedAt" label="完成时间">{{ formatTime(currentTask.completedAt) }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="currentTask.description" class="task-description">
          <el-divider>任务描述</el-divider>
          <p>{{ currentTask.description }}</p>
        </div>

        <div v-if="currentTask.result" class="task-result">
          <el-divider>执行结果</el-divider>
          <p>{{ currentTask.result }}</p>
        </div>

        <div v-if="currentTask.error" class="task-error">
          <el-divider>错误信息</el-divider>
          <el-alert type="error" :closable="false">
            {{ currentTask.error }}
          </el-alert>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
/**
 * Agent 调度页面
 * 查看和管理 Agent 调度状态
 *
 * 功能：
 * - 统计概览（总数、运行中、忙碌、空闲）
 * - 任务队列列表（支持搜索和筛选）
 * - 任务详情（侧边抽屉）
 * - 取消任务
 * - 调度配置
 * - 手动触发调度
 *
 * 操作维度：系统级
 * 权限要求：agents:manage
 */
import { ref, computed, onMounted } from 'vue'
import { schedulerApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, User, VideoPlay, Loading, Coffee, Refresh } from '@element-plus/icons-vue'

const loading = ref(false)
const triggering = ref(false)
const savingConfig = ref(false)
const status = ref({})
const taskQueue = ref([])
const config = ref({
  intervalSeconds: 60,
  maxConcurrentTasks: 10,
  taskTimeoutMinutes: 30
})

// 搜索和筛选
const searchKeyword = ref('')
const filterStatus = ref('')

// 详情抽屉
const drawerVisible = ref(false)
const currentTask = ref(null)

// 筛选后的任务队列
const filteredTaskQueue = computed(() => {
  let result = taskQueue.value

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(t =>
      t.taskId?.toLowerCase().includes(keyword) ||
      t.title?.toLowerCase().includes(keyword) ||
      t.agentId?.toLowerCase().includes(keyword)
    )
  }

  if (filterStatus.value) {
    result = result.filter(t => t.status === filterStatus.value)
  }

  return result
})

/** 获取优先级标签类型 */
const getPriorityType = (priority) => {
  const typeMap = {
    'HIGH': 'danger',
    'NORMAL': 'info',
    'LOW': 'success'
  }
  return typeMap[priority] || 'info'
}

/** 获取状态标签类型 */
const getStatusType = (status) => {
  const typeMap = {
    'PENDING': 'info',
    'PROCESSING': 'warning',
    'COMPLETED': 'success',
    'FAILED': 'danger',
    'CANCELLED': 'info'
  }
  return typeMap[status] || 'info'
}

/** 获取状态标签文本 */
const getStatusLabel = (status) => {
  const labelMap = {
    'PENDING': '待处理',
    'PROCESSING': '处理中',
    'COMPLETED': '已完成',
    'FAILED': '失败',
    'CANCELLED': '已取消'
  }
  return labelMap[status] || status
}

/** 格式化时间 */
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

/** 加载调度状态 */
const loadStatus = async () => {
  loading.value = true
  try {
    const [statusData, queueData, configData] = await Promise.all([
      schedulerApi.getStatus(),
      schedulerApi.getTaskQueue(),
      schedulerApi.getConfig()
    ])
    status.value = statusData || {}
    taskQueue.value = queueData || []
    if (configData) {
      config.value = { ...config.value, ...configData }
    }
  } catch (error) {
    ElMessage.error('加载调度状态失败')
  } finally {
    loading.value = false
  }
}

/** 手动触发调度 */
const handleTrigger = async () => {
  triggering.value = true
  try {
    await schedulerApi.triggerSchedule()
    ElMessage.success('调度已触发')
    loadStatus()
  } catch (error) {
    ElMessage.error('触发失败')
  } finally {
    triggering.value = false
  }
}

/** 查看任务详情 */
const handleViewTask = (task) => {
  currentTask.value = task
  drawerVisible.value = true
}

/** 取消任务 */
const handleCancelTask = async (task) => {
  try {
    await ElMessageBox.confirm('确定要取消该任务吗？', '取消任务', {
      confirmButtonText: '取消任务',
      cancelButtonText: '返回',
      type: 'warning'
    })

    await schedulerApi.cancelTask(task.taskId)
    ElMessage.success('任务已取消')
    loadStatus()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('取消任务失败')
    }
  }
}

/** 保存配置 */
const handleSaveConfig = async () => {
  savingConfig.value = true
  try {
    await schedulerApi.updateConfig(config.value)
    ElMessage.success('配置已保存')
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    savingConfig.value = false
  }
}

/** 重置配置 */
const handleResetConfig = () => {
  config.value = {
    intervalSeconds: 60,
    maxConcurrentTasks: 10,
    taskTimeoutMinutes: 30
  }
  ElMessage.info('配置已重置，请点击保存')
}

onMounted(() => {
  loadStatus()
})
</script>

<style scoped>
.scheduler-page {
  padding: 20px;
}

/* 统计卡片 */
.stat-cards {
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
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
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: var(--el-text-color-primary);
}

.stat-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
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

/* 表单提示 */
.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

/* 任务详情 */
.task-description,
.task-result,
.task-error {
  margin-top: 16px;
}

.task-description p,
.task-result p {
  margin: 0;
  padding: 12px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  line-height: 1.6;
}

.mt-4 {
  margin-top: 16px;
}

/* 响应式 */
@media (max-width: 767px) {
  .scheduler-page {
    padding: 12px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
  }

  :deep(.el-dialog),
  :deep(.el-drawer) {
    width: 90% !important;
  }
}
</style>
