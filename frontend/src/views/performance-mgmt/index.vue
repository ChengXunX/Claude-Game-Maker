<template>
  <div class="performance-mgmt-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>绩效管理</span>
          <el-button type="primary" @click="handleCreateReview" v-permission="'agents:manage'">
            <el-icon><Plus /></el-icon> 发起评估
          </el-button>
        </div>
      </template>

      <!-- 标签页 -->
      <el-tabs v-model="activeTab" @tab-click="loadData">
        <!-- 绩效评估 -->
        <el-tab-pane label="绩效评估" name="reviews">
          <el-table :data="reviews" v-loading="loading" stripe>
            <el-table-column prop="reviewNo" label="评估编号" width="150" show-overflow-tooltip />
            <el-table-column prop="agentName" label="Agent 名称" width="120" />
            <el-table-column prop="agentRole" label="角色" width="100" />
            <el-table-column label="评估周期" width="200">
              <template #default="{ row }">
                {{ formatDate(row.startDate) }} ~ {{ formatDate(row.endDate) }}
              </template>
            </el-table-column>
            <el-table-column label="评分" width="100">
              <template #default="{ row }">
                <span class="score">{{ row.score || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="等级" width="80">
              <template #default="{ row }">
                <el-tag :type="getGradeType(row.grade)" size="small" effect="dark">
                  {{ row.grade || '-' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getReviewStatusType(row.status)" size="small">
                  {{ getReviewStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.createdAt) }}
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 离职申请 -->
        <el-tab-pane label="离职申请" name="dismissals">
          <el-table :data="dismissals" v-loading="loading" stripe>
            <el-table-column prop="dismissalNo" label="申请编号" width="150" show-overflow-tooltip />
            <el-table-column prop="agentName" label="Agent 名称" width="120" />
            <el-table-column prop="agentRole" label="角色" width="100" />
            <el-table-column prop="reason" label="离职原因" min-width="200" show-overflow-tooltip />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getDismissalStatusType(row.status)" size="small">
                  {{ getDismissalStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <template v-if="row.status === 'PENDING'">
                  <el-button type="success" size="small" text @click="handleApproveDismissal(row)" v-permission="'agents:manage'">
                    通过
                  </el-button>
                  <el-button type="danger" size="small" text @click="handleRejectDismissal(row)" v-permission="'agents:manage'">
                    拒绝
                  </el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 发起评估对话框 -->
    <el-dialog v-model="reviewDialogVisible" title="发起绩效评估" width="600px">
      <el-form ref="reviewFormRef" :model="reviewForm" :rules="reviewRules" label-width="100px">
        <el-form-item label="Agent" prop="agentId">
          <el-select v-model="reviewForm.agentId" placeholder="选择Agent" filterable>
            <el-option v-for="agent in agents" :key="agent.id" :label="`${agent.name} (${agent.role})`" :value="agent.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="评估周期" prop="period">
          <el-date-picker
            v-model="reviewForm.period"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
          />
        </el-form-item>
        <el-form-item label="评分" prop="score">
          <el-slider v-model="reviewForm.score" :min="0" :max="100" :step="5" show-input />
        </el-form-item>
        <el-form-item label="评语">
          <el-input v-model="reviewForm.comment" type="textarea" :rows="4" placeholder="请输入评估评语" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitReview" :loading="submitting">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 绩效管理页面
 * 管理 Agent 绩效评估和离职申请
 *
 * 操作维度：系统级
 * 权限要求：系统管理员
 */
import { ref, onMounted } from 'vue'
import { performanceMgmtApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'

const loading = ref(false)
const activeTab = ref('reviews')
const reviews = ref([])
const dismissals = ref([])
const agents = ref([])

const reviewDialogVisible = ref(false)
const reviewFormRef = ref(null)
const submitting = ref(false)
const reviewForm = ref({
  agentId: '',
  period: [],
  score: 80,
  comment: ''
})
const reviewRules = {
  agentId: [{ required: true, message: '请选择Agent', trigger: 'change' }],
  period: [{ required: true, message: '请选择评估周期', trigger: 'change' }],
  score: [{ required: true, message: '请设置评分', trigger: 'change' }]
}

/** 获取等级标签类型 */
const getGradeType = (grade) => {
  const typeMap = { 'S': 'danger', 'A': 'warning', 'B': 'success', 'C': 'info', 'D': '' }
  return typeMap[grade] || 'info'
}

/** 获取评估状态标签类型 */
const getReviewStatusType = (status) => {
  const typeMap = { 'PENDING': 'info', 'IN_PROGRESS': 'warning', 'COMPLETED': 'success' }
  return typeMap[status] || 'info'
}

/** 获取评估状态标签文本 */
const getReviewStatusLabel = (status) => {
  const labelMap = { 'PENDING': '待评估', 'IN_PROGRESS': '评估中', 'COMPLETED': '已完成' }
  return labelMap[status] || status
}

/** 获取离职状态标签类型 */
const getDismissalStatusType = (status) => {
  const typeMap = { 'PENDING': 'warning', 'APPROVED': 'success', 'REJECTED': 'danger' }
  return typeMap[status] || 'info'
}

/** 获取离职状态标签文本 */
const getDismissalStatusLabel = (status) => {
  const labelMap = { 'PENDING': '待审批', 'APPROVED': '已通过', 'REJECTED': '已拒绝' }
  return labelMap[status] || status
}

/** 格式化日期 */
const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('zh-CN')
}

/** 格式化时间 */
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

/** 加载数据 */
const loadData = async () => {
  loading.value = true
  try {
    const [reviewsData, dismissalsData] = await Promise.all([
      performanceMgmtApi.getReviews(),
      performanceMgmtApi.getDismissals()
    ])
    reviews.value = reviewsData || []
    dismissals.value = dismissalsData || []
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

/** 发起评估 */
const handleCreateReview = async () => {
  reviewForm.value = {
    agentId: '',
    period: [],
    score: 80,
    comment: ''
  }
  reviewDialogVisible.value = true

  // 加载Agent列表
  try {
    const data = await api.get('/agents')
    agents.value = data || []
  } catch (error) {
    console.error('加载Agent列表失败')
  }
}

/** 提交评估 */
const handleSubmitReview = async () => {
  try {
    await reviewFormRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const data = {
      agentId: reviewForm.value.agentId,
      startDate: reviewForm.value.period[0],
      endDate: reviewForm.value.period[1],
      score: reviewForm.value.score,
      comment: reviewForm.value.comment
    }
    await performanceMgmtApi.createReview(data)
    ElMessage.success('评估已发起')
    reviewDialogVisible.value = false
    loadData()
  } catch (error) {
    ElMessage.error('发起失败')
  } finally {
    submitting.value = false
  }
}

/** 通过离职 */
const handleApproveDismissal = async (dismissal) => {
  try {
    await ElMessageBox.confirm(
      `确定通过 Agent "${dismissal.agentName}" 的离职申请吗？`,
      '审批确认',
      { confirmButtonText: '通过', cancelButtonText: '取消', type: 'warning' }
    )

    await performanceMgmtApi.approveDismissal(dismissal.id)
    ElMessage.success('已通过')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

/** 拒绝离职 */
const handleRejectDismissal = async (dismissal) => {
  try {
    const { value: reason } = await ElMessageBox.prompt(
      `请输入拒绝 Agent "${dismissal.agentName}" 离职的原因：`,
      '审批拒绝',
      {
        confirmButtonText: '拒绝',
        cancelButtonText: '取消',
        type: 'warning',
        inputPlaceholder: '拒绝原因（可选）'
      }
    )

    await performanceMgmtApi.rejectDismissal(dismissal.id, { reason })
    ElMessage.success('已拒绝')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.performance-mgmt-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.score {
  font-weight: bold;
  color: #409eff;
}
</style>
