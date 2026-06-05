<template>
  <div class="approval-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>招聘审批</span>
          <el-button @click="router.back()">返回</el-button>
        </div>
      </template>

      <!-- 统计卡片 -->
      <el-row :gutter="16" class="stat-cards">
        <el-col :span="8">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value warning">{{ stats.pending || 0 }}</div>
              <div class="stat-label">待审批</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value success">{{ stats.approved || 0 }}</div>
              <div class="stat-label">已通过</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value danger">{{ stats.rejected || 0 }}</div>
              <div class="stat-label">已拒绝</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 审批列表 -->
      <el-table :data="requests" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="agentRole" label="Agent角色" width="120" />
        <el-table-column prop="agentName" label="Agent名称" width="150" />
        <el-table-column prop="projectName" label="项目" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requestedBy" label="申请人" width="100" />
        <el-table-column label="申请时间" width="180">
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
            <span v-else class="text-muted">已处理</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
/**
 * 招聘审批页面
 * 审批Agent招聘请求
 */
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { recruitmentApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

const loading = ref(false)
const requests = ref([])
const stats = ref({})

/** 获取状态标签类型 */
const getStatusType = (status) => {
  const typeMap = {
    'PENDING': 'warning',
    'APPROVED': 'success',
    'REJECTED': 'danger'
  }
  return typeMap[status] || 'info'
}

/** 获取状态标签文本 */
const getStatusLabel = (status) => {
  const labelMap = {
    'PENDING': '待审批',
    'APPROVED': '已通过',
    'REJECTED': '已拒绝'
  }
  return labelMap[status] || status
}

/** 格式化时间 */
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

/** 加载审批列表 */
const loadRequests = async () => {
  loading.value = true
  try {
    const [requestsData, statsData] = await Promise.all([
      recruitmentApi.getRequests(),
      recruitmentApi.getStats()
    ])
    requests.value = requestsData || []
    stats.value = statsData || {}
  } catch (error) {
    ElMessage.error('加载审批列表失败')
  } finally {
    loading.value = false
  }
}

/** 通过审批 */
const handleApprove = async (request) => {
  try {
    await ElMessageBox.confirm(`确定要通过 ${request.agentName || request.agentRole} 的招聘申请吗？`, '确认操作')
    await recruitmentApi.approveRequest(request.id)
    ElMessage.success('审批已通过')
    loadRequests()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

/** 拒绝审批 */
const handleReject = async (request) => {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝审批', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputType: 'textarea'
    })

    if (!reason) {
      ElMessage.warning('请输入拒绝原因')
      return
    }

    await recruitmentApi.rejectRequest(request.id, { reason })
    ElMessage.success('审批已拒绝')
    loadRequests()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

onMounted(() => {
  loadRequests()
})
</script>

<style scoped>
.approval-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-cards {
  margin-bottom: 24px;
}

.stat-item {
  text-align: center;
  padding: 16px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
}

.stat-value.warning {
  color: var(--el-color-warning);
}

.stat-value.success {
  color: var(--el-color-success);
}

.stat-value.danger {
  color: var(--el-color-danger);
}

.stat-label {
  margin-top: 8px;
  color: var(--el-text-color-secondary);
}

.text-muted {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
</style>
