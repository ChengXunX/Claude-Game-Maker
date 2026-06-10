<template>
  <div class="approvals-page">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-warning-light-9)">
            <el-icon :size="24" color="var(--el-color-warning)"><Clock /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value warning">{{ stats.pending || 0 }}</div>
            <div class="stat-label">待审批</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-success-light-9)">
            <el-icon :size="24" color="var(--el-color-success)"><CircleCheck /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value success">{{ stats.approved || 0 }}</div>
            <div class="stat-label">已通过</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-danger-light-9)">
            <el-icon :size="24" color="var(--el-color-danger)"><CircleClose /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value danger">{{ stats.rejected || 0 }}</div>
            <div class="stat-label">已拒绝</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-info-light-9)">
            <el-icon :size="24" color="var(--el-color-info)"><RemoveFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value info">{{ stats.expired || 0 }}</div>
            <div class="stat-label">已过期</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>审批管理</span>
          <div class="header-actions">
            <el-select v-model="filterType" placeholder="请求类型" clearable style="width: 140px">
              <el-option label="全部类型" value="" />
              <el-option label="招聘 Agent" value="CREATE_AGENT" />
              <el-option label="删除 Agent" value="DELETE_AGENT" />
              <el-option label="解雇 Agent" value="DISMISS_AGENT" />
              <el-option label="分配 API" value="ASSIGN_API" />
              <el-option label="修改配置" value="CHANGE_CONFIG" />
              <el-option label="战略决策" value="STRATEGIC_DECISION" />
              <el-option label="项目交付" value="DELIVERY" />
            </el-select>
            <el-select v-model="filterStatus" placeholder="审批状态" clearable style="width: 120px">
              <el-option label="全部状态" value="" />
              <el-option label="待审批" value="PENDING" />
              <el-option label="已通过" value="APPROVED" />
              <el-option label="已拒绝" value="REJECTED" />
              <el-option label="已过期" value="EXPIRED" />
            </el-select>
            <el-button @click="loadApprovals" :loading="loading">
              <el-icon><Refresh /></el-icon> 刷新
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="filteredApprovals" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.requestType)" size="small">
              {{ getTypeLabel(row.requestType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="requesterName" label="发起者" width="120" />
        <el-table-column prop="projectId" label="项目" width="120" show-overflow-tooltip />
        <el-table-column label="优先级" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="getPriorityType(row.priority)" size="small" effect="plain">
              {{ row.priority || '-' }}
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
        <el-table-column label="审批意见" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.approvalComment">{{ row.approvalComment }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="发起时间" width="170">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button type="success" size="small" @click="handleApprove(row)">
                通过
              </el-button>
              <el-button type="danger" size="small" @click="handleReject(row)">
                拒绝
              </el-button>
            </template>
            <template v-else-if="row.status === 'REJECTED'">
              <el-button type="warning" size="small" text @click="handleViewReason(row)">
                查看原因
              </el-button>
            </template>
            <span v-else class="text-muted">已处理</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 审批详情抽屉 -->
    <el-drawer v-model="drawerVisible" title="审批详情" size="500px" destroy-on-close>
      <template v-if="selectedApproval">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="请求ID">{{ selectedApproval.id }}</el-descriptions-item>
          <el-descriptions-item label="请求类型">
            <el-tag :type="getTypeTagType(selectedApproval.requestType)" size="small">
              {{ getTypeLabel(selectedApproval.requestType) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="发起者">{{ selectedApproval.requesterName || selectedApproval.requesterId }}</el-descriptions-item>
          <el-descriptions-item label="项目">{{ selectedApproval.projectId }}</el-descriptions-item>
          <el-descriptions-item label="描述">{{ selectedApproval.description || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(selectedApproval.status)" size="small">
              {{ getStatusLabel(selectedApproval.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="审批者">{{ selectedApproval.approverName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审批意见">{{ selectedApproval.approvalComment || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发起时间">{{ formatTime(selectedApproval.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="审批时间">{{ formatTime(selectedApproval.approvedAt) }}</el-descriptions-item>
        </el-descriptions>

        <!-- 请求数据 -->
        <el-card class="mt-4" v-if="selectedApproval.requestData">
          <template #header>请求数据</template>
          <pre class="code-block">{{ formatJson(selectedApproval.requestData) }}</pre>
        </el-card>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
/**
 * 统一审批管理页面
 * 管理所有类型的审批请求：招聘、删除、配置变更等
 */
import { ref, computed, onMounted } from 'vue'
import { approvalApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, CircleCheck, CircleClose, RemoveFilled, Refresh } from '@element-plus/icons-vue'

const loading = ref(false)
const approvals = ref([])
const stats = ref({})
const filterType = ref('')
const filterStatus = ref('')
const drawerVisible = ref(false)
const selectedApproval = ref(null)

const filteredApprovals = computed(() => {
  let result = approvals.value
  if (filterType.value) {
    result = result.filter(a => a.requestType === filterType.value)
  }
  if (filterStatus.value) {
    result = result.filter(a => a.status === filterStatus.value)
  }
  return result
})

const getTypeLabel = (type) => {
  const map = {
    'CREATE_AGENT': '招聘 Agent',
    'DELETE_AGENT': '删除 Agent',
    'DISMISS_AGENT': '解雇 Agent',
    'ASSIGN_API': '分配 API',
    'CHANGE_CONFIG': '修改配置',
    'STRATEGIC_DECISION': '战略决策',
    'DELIVERY': '项目交付'
  }
  return map[type] || type
}

const getTypeTagType = (type) => {
  const map = {
    'CREATE_AGENT': 'primary',
    'DELETE_AGENT': 'danger',
    'DISMISS_AGENT': 'danger',
    'ASSIGN_API': 'warning',
    'CHANGE_CONFIG': 'info',
    'STRATEGIC_DECISION': 'danger',
    'DELIVERY': 'success'
  }
  return map[type] || ''
}

const getStatusType = (status) => {
  const map = { 'PENDING': 'warning', 'APPROVED': 'success', 'REJECTED': 'danger', 'EXPIRED': 'info', 'CANCELLED': 'info' }
  return map[status] || 'info'
}

const getStatusLabel = (status) => {
  const map = { 'PENDING': '待审批', 'APPROVED': '已通过', 'REJECTED': '已拒绝', 'EXPIRED': '已过期', 'CANCELLED': '已取消' }
  return map[status] || status
}

const getPriorityType = (priority) => {
  if (!priority) return 'info'
  if (priority <= 2) return 'danger'
  if (priority <= 5) return 'warning'
  return 'info'
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

const formatJson = (str) => {
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch {
    return str
  }
}

const loadApprovals = async () => {
  loading.value = true
  try {
    const allData = await approvalApi.getAll()
    approvals.value = allData || []
    // 从全量数据计算统计
    const list = allData || []
    stats.value = {
      pending: list.filter(a => a.status === 'PENDING').length,
      approved: list.filter(a => a.status === 'APPROVED').length,
      rejected: list.filter(a => a.status === 'REJECTED').length,
      expired: list.filter(a => a.status === 'EXPIRED' || a.status === 'CANCELLED').length
    }
  } catch (error) {
    ElMessage.error('加载审批数据失败')
  } finally {
    loading.value = false
  }
}

const handleApprove = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要通过这个 ${getTypeLabel(row.requestType)} 请求吗？`,
      '确认通过',
      { confirmButtonText: '通过', cancelButtonText: '取消', type: 'success' }
    )
    await approvalApi.approve(row.id, { approved: true, comment: '' })
    ElMessage.success('审批已通过')
    loadApprovals()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const handleReject = async (row) => {
  try {
    const { value: reason } = await ElMessageBox.prompt(
      '请输入拒绝原因（制作人将根据原因进行优化）',
      '拒绝审批',
      {
        confirmButtonText: '确定拒绝',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: '请详细说明拒绝原因，以便制作人理解并优化...',
        inputValidator: (val) => val && val.trim() ? true : '请输入拒绝原因'
      }
    )

    await approvalApi.approve(row.id, { approved: false, comment: reason })
    ElMessage.success('审批已拒绝，制作人将收到反馈')
    loadApprovals()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const handleViewReason = (row) => {
  selectedApproval.value = row
  drawerVisible.value = true
}

onMounted(() => {
  loadApprovals()
})
</script>

<style scoped>
.approvals-page {
  padding: 20px;
}

.stat-cards {
  margin-bottom: 16px;
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
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  line-height: 1.2;
}

.stat-value.warning { color: var(--el-color-warning); }
.stat-value.success { color: var(--el-color-success); }
.stat-value.danger { color: var(--el-color-danger); }
.stat-value.info { color: var(--el-color-info); }

.stat-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
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
  align-items: center;
}

.mt-4 {
  margin-top: 16px;
}

.text-muted {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.code-block {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  font-size: 12px;
  font-family: monospace;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 300px;
  overflow-y: auto;
  margin: 0;
}
</style>
