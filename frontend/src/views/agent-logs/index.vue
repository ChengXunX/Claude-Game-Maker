<template>
  <div class="agent-logs-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>Agent 日志</span>
          <div class="header-actions">
            <el-button @click="loadLogs" :loading="loading">
              <el-icon><Refresh /></el-icon> 刷新
            </el-button>
            <el-button @click="handleExport">
              <el-icon><Download /></el-icon> 导出
            </el-button>
          </div>
        </div>
      </template>

      <!-- 筛选条件 -->
      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item label="Agent">
          <el-select v-model="filters.agentId" placeholder="选择Agent" clearable filterable style="width: 200px">
            <el-option label="全部Agent" value="" />
            <el-option v-for="agent in agents" :key="agent.id" :label="agent.name || agent.id" :value="agent.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作">
          <el-select v-model="filters.action" placeholder="选择操作" clearable filterable style="width: 160px">
            <el-option label="全部操作" value="" />
            <el-option-group label="任务">
              <el-option label="接收任务" value="TASK_RECEIVED" />
              <el-option label="开始任务" value="TASK_STARTED" />
              <el-option label="完成任务" value="TASK_COMPLETED" />
              <el-option label="任务失败" value="TASK_FAILED" />
            </el-option-group>
            <el-option-group label="AI">
              <el-option label="调用AI" value="AI_CALL" />
              <el-option label="AI响应" value="AI_RESPONSE" />
            </el-option-group>
            <el-option-group label="文件">
              <el-option label="读取文件" value="FILE_READ" />
              <el-option label="写入文件" value="FILE_WRITE" />
              <el-option label="执行命令" value="COMMAND_EXEC" />
            </el-option-group>
            <el-option-group label="通信">
              <el-option label="发送消息" value="MESSAGE_SENT" />
              <el-option label="接收消息" value="MESSAGE_RECEIVED" />
            </el-option-group>
            <el-option-group label="生命周期">
              <el-option label="Agent启动" value="AGENT_STARTED" />
              <el-option label="Agent停止" value="AGENT_STOPPED" />
              <el-option label="Agent错误" value="AGENT_ERROR" />
            </el-option-group>
            <el-option-group label="其他">
              <el-option label="决策" value="DECISION" />
              <el-option label="系统事件" value="SYSTEM" />
            </el-option-group>
          </el-select>
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="filters.level" placeholder="选择级别" clearable style="width: 120px">
            <el-option label="全部级别" value="" />
            <el-option label="DEBUG" value="DEBUG" />
            <el-option label="INFO" value="INFO" />
            <el-option label="WARN" value="WARN" />
            <el-option label="ERROR" value="ERROR" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="filters.dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 340px"
          />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" placeholder="搜索摘要/详情/决策" clearable style="width: 220px"
            @keyup.enter="loadLogs" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadLogs">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 日志列表 -->
      <el-table :data="logs" v-loading="loading" stripe @row-click="showDetail" style="cursor: pointer">
        <el-table-column prop="createdAt" label="时间" width="170" sortable>
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="agentName" label="Agent" width="120" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.agentName || row.agentId || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="action" label="操作" width="130">
          <template #default="{ row }">
            <el-tag :type="getActionType(row.action)" size="small">{{ formatAction(row.action) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="级别" width="80">
          <template #default="{ row }">
            <el-tag :type="getLevelType(row.level)" size="small">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="summary" label="摘要" min-width="250" show-overflow-tooltip />
        <el-table-column prop="taskId" label="任务ID" width="130" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.taskId" class="task-id">{{ row.taskId }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="durationMs" label="耗时" width="90">
          <template #default="{ row }">
            <span v-if="row.durationMs">{{ formatDuration(row.durationMs) }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click.stop="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[20, 50, 100, 200]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadLogs"
          @current-change="loadLogs"
        />
      </div>
    </el-card>

    <!-- 日志详情抽屉 -->
    <el-drawer v-model="drawerVisible" title="日志详情" size="700px" destroy-on-close>
      <template v-if="selectedLog">
        <!-- 基本信息 -->
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="日志ID">{{ selectedLog.id }}</el-descriptions-item>
          <el-descriptions-item label="时间">{{ formatTime(selectedLog.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="Agent 名称">{{ selectedLog.agentName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="Agent ID">
            <el-tag size="small" type="info">{{ selectedLog.agentId }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="操作类型">
            <el-tag :type="getActionType(selectedLog.action)" size="small">
              {{ formatAction(selectedLog.action) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="级别">
            <el-tag :type="getLevelType(selectedLog.level)" size="small">{{ selectedLog.level }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="项目ID">{{ selectedLog.projectId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="任务ID">
            <span v-if="selectedLog.taskId" class="task-id">{{ selectedLog.taskId }}</span>
            <span v-else class="text-muted">-</span>
          </el-descriptions-item>
          <el-descriptions-item label="耗时" :span="2">
            {{ selectedLog.durationMs ? formatDuration(selectedLog.durationMs) : '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 摘要 -->
        <el-card class="mt-4" v-if="selectedLog.summary">
          <template #header>
            <span>摘要</span>
          </template>
          <div class="summary-content">{{ selectedLog.summary }}</div>
        </el-card>

        <!-- 详情内容 -->
        <el-card class="mt-4" v-if="selectedLog.detail">
          <template #header>
            <span>详情内容</span>
          </template>
          <pre class="code-block">{{ selectedLog.detail }}</pre>
        </el-card>

        <!-- 决策 / AI 输出 -->
        <el-card class="mt-4" v-if="selectedLog.decision">
          <template #header>
            <span>决策 / AI 输出</span>
          </template>
          <pre class="code-block">{{ selectedLog.decision }}</pre>
        </el-card>

        <!-- 无额外数据提示 -->
        <el-empty v-if="!selectedLog.summary && !selectedLog.detail && !selectedLog.decision"
          description="暂无详细数据" :image-size="60" />
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
/**
 * Agent 日志页面
 * 查看 Agent 的详细操作日志，支持多维度筛选和详情查看
 */
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '@/api'

const loading = ref(false)
const logs = ref([])
const agents = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(50)

// 详情抽屉
const drawerVisible = ref(false)
const selectedLog = ref(null)

const filters = ref({
  agentId: '',
  action: '',
  level: '',
  keyword: '',
  dateRange: null
})

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

const formatDuration = (ms) => {
  if (!ms) return '-'
  if (ms < 1000) return ms + 'ms'
  if (ms < 60000) return (ms / 1000).toFixed(1) + 's'
  return (ms / 60000).toFixed(1) + 'min'
}

const formatAction = (action) => {
  if (!action) return '-'
  const map = {
    'TASK_RECEIVED': '接收任务',
    'TASK_STARTED': '开始任务',
    'TASK_COMPLETED': '完成任务',
    'TASK_FAILED': '任务失败',
    'AI_CALL': '调用AI',
    'AI_RESPONSE': 'AI响应',
    'COMMAND_EXEC': '执行命令',
    'FILE_READ': '读取文件',
    'FILE_WRITE': '写入文件',
    'DECISION': '决策',
    'MESSAGE_SENT': '发送消息',
    'MESSAGE_RECEIVED': '接收消息',
    'AGENT_STARTED': '启动',
    'AGENT_STOPPED': '停止',
    'AGENT_ERROR': '错误',
    'SYSTEM': '系统'
  }
  return map[action] || action
}

const getActionType = (action) => {
  if (!action) return 'info'
  if (action.includes('STARTED') || action.includes('COMPLETED') || action.includes('RECEIVED')) return 'success'
  if (action.includes('FAILED') || action.includes('ERROR')) return 'danger'
  if (action.includes('CALL') || action.includes('RESPONSE')) return 'primary'
  if (action.includes('FILE') || action.includes('COMMAND')) return 'warning'
  if (action.includes('MESSAGE')) return 'info'
  return 'info'
}

const getLevelType = (level) => {
  const map = { DEBUG: 'info', INFO: '', WARN: 'warning', ERROR: 'danger' }
  return map[level] || 'info'
}

const loadLogs = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value
    }
    if (filters.value.agentId) params.agentId = filters.value.agentId
    if (filters.value.action) params.action = filters.value.action
    if (filters.value.level) params.level = filters.value.level
    if (filters.value.keyword) params.keyword = filters.value.keyword
    if (filters.value.dateRange && filters.value.dateRange[0]) {
      params.startTime = filters.value.dateRange[0]
      params.endTime = filters.value.dateRange[1]
    }

    const data = await api.get('/admin/agent-logs/api/list', { params })
    logs.value = data.content || []
    total.value = data.totalElements || 0
  } catch (error) {
    ElMessage.error('加载日志失败')
  } finally {
    loading.value = false
  }
}

const loadAgents = async () => {
  try {
    const data = await api.get('/agents')
    agents.value = data || []
  } catch (error) {
    console.error('加载Agent列表失败')
  }
}

const handleReset = () => {
  filters.value = { agentId: '', action: '', level: '', keyword: '', dateRange: null }
  currentPage.value = 1
  loadLogs()
}

const showDetail = (row) => {
  selectedLog.value = row
  drawerVisible.value = true
}

const handleExport = () => {
  const params = new URLSearchParams()
  if (filters.value.agentId) params.append('agentId', filters.value.agentId)
  if (filters.value.action) params.append('action', filters.value.action)
  if (filters.value.level) params.append('level', filters.value.level)
  if (filters.value.keyword) params.append('keyword', filters.value.keyword)
  if (filters.value.dateRange && filters.value.dateRange[0]) {
    params.append('startTime', filters.value.dateRange[0])
    params.append('endTime', filters.value.dateRange[1])
  }

  const token = localStorage.getItem('token')
  window.open(`/admin/agent-logs/export?${params.toString()}&token=${token}`)
}

onMounted(() => {
  loadAgents()
  loadLogs()
})
</script>

<style scoped>
.agent-logs-page {
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
  gap: 8px;
}

.filter-form {
  margin-bottom: 16px;
}

.filter-form :deep(.el-form-item) {
  margin-bottom: 12px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.mt-4 {
  margin-top: 16px;
}

.task-id {
  font-family: monospace;
  font-size: 12px;
  color: #606266;
}

.text-muted {
  color: #909399;
  font-size: 12px;
}

.code-block {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  font-size: 12px;
  font-family: 'JetBrains Mono', 'SF Mono', monospace;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 400px;
  overflow-y: auto;
  margin: 0;
}

.summary-content {
  font-size: 14px;
  line-height: 1.6;
  color: #303133;
  word-break: break-all;
  white-space: pre-wrap;
}
</style>
