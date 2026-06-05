<template>
  <div class="scheduler-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>Agent 调度</span>
          <div>
            <el-button type="primary" @click="handleTrigger" :loading="triggering" v-permission="'agents:manage'">
              <el-icon><VideoPlay /></el-icon> 手动触发调度
            </el-button>
            <el-button @click="loadStatus" :loading="loading">
              <el-icon><Refresh /></el-icon> 刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- 调度状态总览 -->
      <el-row :gutter="16" class="stat-cards">
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value">{{ status.totalAgents || 0 }}</div>
              <div class="stat-label">Agent 总数</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value running">{{ status.runningAgents || 0 }}</div>
              <div class="stat-label">运行中</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value idle">{{ status.idleAgents || 0 }}</div>
              <div class="stat-label">空闲</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value busy">{{ status.busyAgents || 0 }}</div>
              <div class="stat-label">忙碌</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 任务队列 -->
      <h3 class="section-title">任务队列</h3>
      <el-table :data="taskQueue" v-loading="loading" stripe>
        <el-table-column prop="taskId" label="任务 ID" width="150" show-overflow-tooltip />
        <el-table-column prop="agentId" label="Agent ID" width="120" show-overflow-tooltip />
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
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && taskQueue.length === 0" description="暂无待处理任务" />
    </el-card>

    <!-- 调度配置 -->
    <el-card class="mt-4">
      <template #header>
        <span>调度配置</span>
      </template>
      <el-form :model="config" label-width="150px" style="max-width: 600px">
        <el-form-item label="调度间隔（秒）">
          <el-input-number v-model="config.intervalSeconds" :min="10" :max="3600" />
        </el-form-item>
        <el-form-item label="最大并发任务数">
          <el-input-number v-model="config.maxConcurrentTasks" :min="1" :max="100" />
        </el-form-item>
        <el-form-item label="任务超时（分钟）">
          <el-input-number v-model="config.taskTimeoutMinutes" :min="1" :max="1440" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSaveConfig" :loading="savingConfig">保存配置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
/**
 * Agent 调度页面
 * 查看和管理 Agent 调度状态
 *
 * 操作维度：系统级
 * 权限要求：agents:manage
 */
import { ref, onMounted } from 'vue'
import { schedulerApi } from '@/api'
import { ElMessage } from 'element-plus'

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
    'FAILED': 'danger'
  }
  return typeMap[status] || 'info'
}

/** 获取状态标签文本 */
const getStatusLabel = (status) => {
  const labelMap = {
    'PENDING': '待处理',
    'PROCESSING': '处理中',
    'COMPLETED': '已完成',
    'FAILED': '失败'
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

onMounted(() => {
  loadStatus()
})
</script>

<style scoped>
.scheduler-page {
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
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}

.running {
  color: #67c23a;
}

.idle {
  color: #909399;
}

.busy {
  color: #e6a23c;
}

.stat-label {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.section-title {
  margin: 16px 0 12px;
  font-size: 16px;
}

.mt-4 {
  margin-top: 16px;
}
</style>
