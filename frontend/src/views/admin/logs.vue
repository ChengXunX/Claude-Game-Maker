<template>
  <div class="logs-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>操作日志</span>
          <el-button type="success" size="small" @click="handleExport">
            <el-icon><Download /></el-icon> 导出
          </el-button>
        </div>
      </template>

      <!-- 搜索筛选区 -->
      <div class="filter-bar">
        <el-input
          v-model="filters.keyword"
          placeholder="搜索关键词（用户名/目标/详情）"
          clearable
          style="width: 220px"
          @keyup.enter="loadLogs"
        />
        <el-select v-model="filters.action" placeholder="操作类型" clearable style="width: 150px" filterable allow-create>
          <el-option-group label="用户操作">
            <el-option label="用户登录" value="USER_LOGIN" />
            <el-option label="用户登出" value="USER_LOGOUT" />
          </el-option-group>
          <el-option-group label="写操作">
            <el-option label="创建（所有）" value="CREATE" />
            <el-option label="更新（所有）" value="UPDATE" />
            <el-option label="删除（所有）" value="DELETE" />
          </el-option-group>
          <el-option-group label="具体操作">
            <el-option label="创建项目" value="CREATE_PROJECT" />
            <el-option label="更新项目" value="UPDATE_PROJECT" />
            <el-option label="删除项目" value="DELETE_PROJECT" />
            <el-option label="创建Agent" value="CREATE_AGENT" />
            <el-option label="更新Agent" value="UPDATE_AGENT" />
            <el-option label="删除Agent" value="DELETE_AGENT" />
            <el-option label="创建用户" value="CREATE_USER" />
            <el-option label="更新用户" value="UPDATE_USER" />
            <el-option label="删除用户" value="DELETE_USER" />
            <el-option label="更新配置" value="UPDATE_CONFIG" />
          </el-option-group>
        </el-select>
        <el-select v-model="filters.targetType" placeholder="目标类型" clearable style="width: 130px">
          <el-option label="项目" value="PROJECT" />
          <el-option label="Agent" value="AGENT" />
          <el-option label="用户" value="USER" />
          <el-option label="角色" value="ROLE" />
          <el-option label="Token" value="TOKEN" />
          <el-option label="技能" value="SKILL" />
          <el-option label="配置" value="CONFIG" />
          <el-option label="工作流" value="WORKFLOW" />
          <el-option label="系统" value="SYSTEM" />
        </el-select>
        <el-select v-model="filters.projectId" placeholder="关联项目" clearable style="width: 160px">
          <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
        <el-select v-model="filters.status" placeholder="状态" clearable style="width: 100px">
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAILURE" />
        </el-select>
        <el-date-picker
          v-model="filters.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 280px"
        />
        <el-button type="primary" @click="loadLogs">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <!-- 日志表格 -->
      <el-table
        :data="logs"
        v-loading="loading"
        stripe
        border
        :header-cell-style="{ cursor: 'pointer' }"
        @row-click="handleRowClick"
      >
        <el-table-column prop="createdAt" label="时间" width="170" resizable>
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户" width="110" resizable />
        <el-table-column prop="action" label="操作" width="140" resizable>
          <template #default="{ row }">
            <el-tag :type="getActionType(row.action)" size="small">
              {{ row.actionDescription || formatAction(row.action) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetType" label="目标类型" width="100" resizable>
          <template #default="{ row }">
            <el-tag v-if="row.targetType" type="info" size="small" effect="plain">{{ formatTargetType(row.targetType) }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="targetName" label="目标" min-width="160" resizable show-overflow-tooltip>
          <template #default="{ row }">
            {{ formatTarget(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="detail" label="操作内容" min-width="200" resizable show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="IP地址" width="130" resizable />
        <el-table-column label="状态" width="80" resizable>
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
              {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="80" resizable>
          <template #default="{ row }">
            {{ row.durationMs ? row.durationMs + 'ms' : '-' }}
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
    <el-drawer v-model="drawerVisible" title="日志详情" size="550px" destroy-on-close>
      <template v-if="selectedLog">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="日志ID">{{ selectedLog.id }}</el-descriptions-item>
          <el-descriptions-item label="操作时间">{{ formatTime(selectedLog.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="操作用户">{{ selectedLog.username || '-' }}</el-descriptions-item>
          <el-descriptions-item label="IP地址">{{ selectedLog.ipAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="操作类型">
            <el-tag :type="getActionType(selectedLog.action)" size="small">
              {{ selectedLog.actionDescription || formatAction(selectedLog.action) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="操作状态">
            <el-tag :type="selectedLog.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
              {{ selectedLog.status === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="目标类型">{{ formatTargetType(selectedLog.targetType) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="目标名称">{{ formatTarget(selectedLog) }}</el-descriptions-item>
          <el-descriptions-item label="Agent ID">{{ selectedLog.agentId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="项目ID">{{ selectedLog.projectId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="耗时">{{ selectedLog.durationMs ? selectedLog.durationMs + 'ms' : '-' }}</el-descriptions-item>
          <el-descriptions-item label="操作级别">{{ selectedLog.level || '-' }}</el-descriptions-item>
          <el-descriptions-item label="操作内容" :span="1">{{ selectedLog.detail || '-' }}</el-descriptions-item>
          <el-descriptions-item label="错误信息" :span="1" v-if="selectedLog.errorMessage">
            <span class="error-text">{{ selectedLog.errorMessage }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 请求参数 -->
        <el-card class="mt-4" v-if="selectedLog.requestParams">
          <template #header>请求参数</template>
          <pre class="code-block">{{ formatJson(selectedLog.requestParams) }}</pre>
        </el-card>

        <!-- 响应数据 -->
        <el-card class="mt-4" v-if="selectedLog.responseData">
          <template #header>响应数据</template>
          <pre class="code-block">{{ formatJson(selectedLog.responseData) }}</pre>
        </el-card>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
/**
 * 操作日志页面
 * 查看系统操作日志记录，支持多维度筛选和详情查看
 *
 * 操作维度：系统级
 * 权限要求：logs:view
 */
import { ref, onMounted } from 'vue'
import { logApi, projectApi } from '@/api'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const logs = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const projects = ref([])

/** 详情抽屉 */
const drawerVisible = ref(false)
const selectedLog = ref(null)

/** 筛选条件 */
const filters = ref({
  keyword: '',
  action: '',
  targetType: '',
  projectId: '',
  status: '',
  dateRange: null
})

/** 格式化时间 */
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

/** 格式化操作类型 */
const formatAction = (action) => {
  if (!action) return '-'
  const map = {
    'USER_LOGIN': '用户登录',
    'USER_LOGOUT': '用户登出',
    'USER_REGISTER': '用户注册',
    'USER_CREATE': '创建用户',
    'USER_UPDATE': '更新用户',
    'USER_DELETE': '删除用户',
    'AGENT_CREATE': '创建Agent',
    'AGENT_UPDATE': '更新Agent',
    'AGENT_DELETE': '删除Agent',
    'AGENT_START': '启动Agent',
    'AGENT_STOP': '停止Agent',
    'AGENT_RESTART': '重启Agent',
    'PROJECT_CREATE': '创建项目',
    'PROJECT_UPDATE': '更新项目',
    'PROJECT_DELETE': '删除项目',
    'TOKEN_CREATE': '创建Token',
    'TOKEN_UPDATE': '更新Token',
    'TOKEN_DELETE': '删除Token',
    'CONFIG_UPDATE': '更新配置',
    'ROLE_CREATE': '创建角色',
    'ROLE_UPDATE': '更新角色',
    'ROLE_DELETE': '删除角色',
    'WORKFLOW_CREATE': '创建工作流',
    'WORKFLOW_UPDATE': '更新工作流',
    'WORKFLOW_DELETE': '删除工作流'
  }
  // 尝试从映射中查找
  if (map[action]) return map[action]
  // 从 action 名称推导：CREATE_xxx, UPDATE_xxx, DELETE_xxx
  if (action.startsWith('CREATE_')) return '创建' + formatTargetType(action.replace('CREATE_', ''))
  if (action.startsWith('UPDATE_')) return '更新' + formatTargetType(action.replace('UPDATE_', ''))
  if (action.startsWith('DELETE_')) return '删除' + formatTargetType(action.replace('DELETE_', ''))
  return action
}

/** 格式化目标类型 */
const formatTargetType = (type) => {
  if (!type) return ''
  const map = {
    'PROJECT': '项目',
    'AGENT': 'Agent',
    'USER': '用户',
    'ROLE': '角色',
    'TOKEN': 'Token',
    'SKILL': '技能',
    'CONFIG': '配置',
    'WORKFLOW': '工作流',
    'INTERVENTION': '干预',
    'ALERT': '告警',
    'NOTIFICATION': '通知',
    'SEARCH': '搜索',
    'LOG': '日志',
    'SYSTEM': '系统'
  }
  return map[type] || type
}

/** 格式化目标显示 */
const formatTarget = (row) => {
  if (row.targetName && row.targetName !== row.targetId) {
    return row.targetName
  }
  if (row.targetName) {
    // 如果 targetName 是纯数字（ID），尝试结合 targetType 显示
    if (/^\d+$/.test(row.targetName)) {
      const type = formatTargetType(row.targetType)
      return type ? type + ' #' + row.targetName : '#' + row.targetName
    }
    return row.targetName
  }
  if (row.targetId) {
    if (/^\d+$/.test(row.targetId)) {
      const type = formatTargetType(row.targetType)
      return type ? type + ' #' + row.targetId : '#' + row.targetId
    }
    return row.targetId
  }
  return '-'
}

/** 获取操作类型标签颜色 */
const getActionType = (action) => {
  if (!action) return 'info'
  if (action.includes('LOGIN') || action.includes('LOGOUT')) return 'info'
  if (action.includes('CREATE') || action.includes('START') || action.includes('APPROVE') || action.includes('REGISTER')) return 'success'
  if (action.includes('UPDATE') || action.includes('RESTART')) return 'warning'
  if (action.includes('DELETE') || action.includes('STOP') || action.includes('REJECT') || action.includes('FAIL')) return 'danger'
  return 'info'
}

/** 格式化 JSON */
const formatJson = (str) => {
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch {
    return str
  }
}

/** 加载日志列表 */
const loadLogs = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value,
      keyword: filters.value.keyword || undefined,
      action: filters.value.action || undefined,
      targetType: filters.value.targetType || undefined,
      projectId: filters.value.projectId || undefined,
      status: filters.value.status || undefined
    }

    if (filters.value.dateRange) {
      params.startDate = filters.value.dateRange[0]?.toISOString()
      params.endDate = filters.value.dateRange[1]?.toISOString()
    }

    const data = await logApi.getAll(params)
    logs.value = data?.content || data || []
    total.value = data?.totalElements || logs.value.length
  } catch (error) {
    ElMessage.error('加载日志失败')
  } finally {
    loading.value = false
  }
}

/** 加载项目列表 */
const loadProjects = async () => {
  try {
    const data = await projectApi.getAll()
    projects.value = Array.isArray(data) ? data : (data?.content || [])
  } catch {
    // ignore
  }
}

/** 重置筛选 */
const handleReset = () => {
  filters.value = {
    keyword: '',
    action: '',
    targetType: '',
    projectId: '',
    status: '',
    dateRange: null
  }
  currentPage.value = 1
  loadLogs()
}

/** 点击行查看详情 */
const handleRowClick = (row) => {
  showDetail(row)
}

/** 显示详情抽屉 */
const showDetail = async (row) => {
  try {
    const data = await logApi.getById(row.id)
    selectedLog.value = data
    drawerVisible.value = true
  } catch {
    selectedLog.value = row
    drawerVisible.value = true
  }
}

/** 导出日志 */
const handleExport = async () => {
  try {
    const params = {
      keyword: filters.value.keyword || undefined,
      action: filters.value.action || undefined,
      targetType: filters.value.targetType || undefined,
      projectId: filters.value.projectId || undefined
    }

    const blob = await logApi.export(params)
    const url = window.URL.createObjectURL(new Blob([blob]))
    const link = document.createElement('a')
    link.href = url
    link.download = `操作日志_${new Date().toISOString().slice(0, 10)}.csv`
    link.click()
    window.URL.revokeObjectURL(url)

    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error('导出失败')
  }
}

onMounted(() => {
  loadLogs()
  loadProjects()
})
</script>

<style scoped>
.logs-page {
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
  flex-wrap: wrap;
  align-items: center;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.mt-4 {
  margin-top: 16px;
}

.error-text {
  color: #f56c6c;
}

.code-block {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  font-size: 12px;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 300px;
  overflow-y: auto;
}
</style>
