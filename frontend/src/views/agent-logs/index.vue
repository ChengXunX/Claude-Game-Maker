<template>
  <div class="agent-logs-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>Agent日志</span>
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
          <el-select v-model="filters.agentId" placeholder="选择Agent" clearable style="width: 200px">
            <el-option label="全部Agent" value="" />
            <el-option v-for="agent in agents" :key="agent.id" :label="agent.name || agent.id" :value="agent.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作">
          <el-select v-model="filters.action" placeholder="选择操作" clearable style="width: 150px">
            <el-option label="全部操作" value="" />
            <el-option label="启动" value="START" />
            <el-option label="停止" value="STOP" />
            <el-option label="任务" value="TASK" />
            <el-option label="错误" value="ERROR" />
            <el-option label="查询" value="QUERY" />
          </el-select>
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="filters.level" placeholder="选择级别" clearable style="width: 120px">
            <el-option label="全部级别" value="" />
            <el-option label="INFO" value="INFO" />
            <el-option label="WARN" value="WARN" />
            <el-option label="ERROR" value="ERROR" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" placeholder="搜索关键词" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadLogs">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 日志列表 -->
      <el-table :data="logs" v-loading="loading" stripe>
        <el-table-column prop="createdAt" label="时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="agentId" label="Agent ID" width="200" show-overflow-tooltip />
        <el-table-column prop="agentName" label="Agent名称" width="120" />
        <el-table-column prop="action" label="操作" width="100">
          <template #default="{ row }">
            <el-tag :type="getActionType(row.action)" size="small">{{ row.action }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="级别" width="80">
          <template #default="{ row }">
            <el-tag :type="getLevelType(row.level)" size="small">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="summary" label="摘要" min-width="200" show-overflow-tooltip />
        <el-table-column prop="detail" label="详情" min-width="200" show-overflow-tooltip />
      </el-table>

      <!-- 分页 -->
      <div class="pagination" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadLogs"
          @current-change="loadLogs"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
/**
 * Agent日志页面
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

const filters = ref({
  agentId: '',
  action: '',
  level: '',
  keyword: ''
})

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

const getActionType = (action) => {
  const map = { START: 'success', STOP: 'danger', TASK: 'primary', ERROR: 'danger', QUERY: 'info' }
  return map[action] || 'info'
}

const getLevelType = (level) => {
  const map = { INFO: 'info', WARN: 'warning', ERROR: 'danger' }
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
  filters.value = { agentId: '', action: '', level: '', keyword: '' }
  currentPage.value = 1
  loadLogs()
}

const handleExport = () => {
  const params = new URLSearchParams()
  if (filters.value.agentId) params.append('agentId', filters.value.agentId)
  if (filters.value.action) params.append('action', filters.value.action)
  if (filters.value.level) params.append('level', filters.value.level)
  if (filters.value.keyword) params.append('keyword', filters.value.keyword)

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

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
