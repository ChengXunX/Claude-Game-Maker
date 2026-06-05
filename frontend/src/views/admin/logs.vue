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
          placeholder="搜索关键词"
          clearable
          style="width: 200px"
          @keyup.enter="loadLogs"
        />
        <el-select v-model="filters.action" placeholder="操作类型" clearable style="width: 150px">
          <el-option label="登录" value="LOGIN" />
          <el-option label="登出" value="LOGOUT" />
          <el-option label="创建" value="CREATE" />
          <el-option label="更新" value="UPDATE" />
          <el-option label="删除" value="DELETE" />
          <el-option label="审批" value="APPROVE" />
        </el-select>
        <el-date-picker
          v-model="filters.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 300px"
        />
        <el-button type="primary" @click="loadLogs">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <!-- 日志表格 -->
      <el-table :data="logs" v-loading="loading" stripe @row-click="handleRowClick">
        <el-table-column prop="createdAt" label="时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户" width="120" />
        <el-table-column prop="action" label="操作" width="120">
          <template #default="{ row }">
            <el-tag :type="getActionType(row.action)" size="small">
              {{ row.actionDescription || row.action }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetName" label="目标" width="150" />
        <el-table-column prop="detail" label="详情" min-width="200" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="IP地址" width="130" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
              {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @size-change="loadLogs"
          @current-change="loadLogs"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
/**
 * 操作日志页面
 * 查看系统操作日志记录
 *
 * 操作维度：系统级
 * 权限要求：logs:view
 */
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { logApi } from '@/api'
import { ElMessage } from 'element-plus'

const router = useRouter()

const loading = ref(false)
const logs = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

/** 筛选条件 */
const filters = ref({
  keyword: '',
  action: '',
  dateRange: null
})

/** 格式化时间 */
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

/** 获取操作类型标签颜色 */
const getActionType = (action) => {
  const typeMap = {
    'LOGIN': 'info',
    'LOGOUT': 'info',
    'CREATE': 'success',
    'UPDATE': 'warning',
    'DELETE': 'danger',
    'APPROVE': 'success'
  }
  return typeMap[action] || 'info'
}

/** 加载日志列表 */
const loadLogs = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value,
      keyword: filters.value.keyword || undefined,
      action: filters.value.action || undefined
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

/** 重置筛选 */
const handleReset = () => {
  filters.value = {
    keyword: '',
    action: '',
    dateRange: null
  }
  currentPage.value = 1
  loadLogs()
}

/** 点击行查看详情 */
const handleRowClick = (row) => {
  router.push(`/admin/logs/${row.id}`)
}

/** 导出日志 */
const handleExport = async () => {
  try {
    const params = {
      keyword: filters.value.keyword || undefined,
      action: filters.value.action || undefined
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
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
