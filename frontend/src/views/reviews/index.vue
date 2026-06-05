<template>
  <div class="reviews-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>代码审查</span>
          <el-button type="primary" @click="handleSubmitReview" v-permission="'code:review'">
            <el-icon><Plus /></el-icon> 提交审查
          </el-button>
        </div>
      </template>

      <!-- 统计卡片 -->
      <el-row :gutter="16" class="stat-cards">
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value">{{ stats.totalReviews || 0 }}</div>
              <div class="stat-label">总审查数</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value pending">{{ stats.pendingReviews || 0 }}</div>
              <div class="stat-label">待审查</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value approved">{{ stats.approvedReviews || 0 }}</div>
              <div class="stat-label">已通过</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value rejected">{{ stats.rejectedReviews || 0 }}</div>
              <div class="stat-label">已拒绝</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 审查列表 -->
      <el-table :data="reviews" v-loading="loading" stripe>
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="agentName" label="提交者" width="120" />
        <el-table-column prop="projectName" label="项目" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="80">
          <template #default="{ row }">
            <span class="score">{{ row.score || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="handleViewDetail(row)">
              详情
            </el-button>
            <el-button
              v-if="row.status === 'PENDING'"
              type="success"
              size="small"
              text
              @click="handleReview(row)"
              v-permission="'code:review'"
            >
              审查
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 审查对话框 -->
    <el-dialog v-model="reviewDialogVisible" title="代码审查" width="600px">
      <el-form ref="reviewFormRef" :model="reviewForm" :rules="reviewRules" label-width="100px">
        <el-form-item label="审查结果" prop="approved">
          <el-radio-group v-model="reviewForm.approved">
            <el-radio :value="true">通过</el-radio>
            <el-radio :value="false">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="评分" prop="score">
          <el-rate v-model="reviewForm.score" :max="10" show-score />
        </el-form-item>
        <el-form-item label="评论" prop="comment">
          <el-input v-model="reviewForm.comment" type="textarea" :rows="4" placeholder="审查意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleReviewSubmit" :loading="submitting">提交</el-button>
      </template>
    </el-dialog>

    <!-- 提交审查对话框 -->
    <el-dialog v-model="submitDialogVisible" title="提交代码审查" width="600px">
      <el-form ref="submitFormRef" :model="submitForm" :rules="submitRules" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="submitForm.title" placeholder="审查标题" />
        </el-form-item>
        <el-form-item label="项目" prop="projectId">
          <ProjectSelector v-model="submitForm.projectId" placeholder="选择项目" width="100%" />
        </el-form-item>
        <el-form-item label="分支" prop="branch">
          <el-input v-model="submitForm.branch" placeholder="分支名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="submitForm.description" type="textarea" :rows="4" placeholder="审查描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submitDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitCreate" :loading="submitting">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 代码审查页面
 * 查看和执行代码审查
 *
 * 操作维度：项目级
 * 权限要求：code:review
 */
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { reviewApi } from '@/api'
import { ElMessage } from 'element-plus'
import ProjectSelector from '@/components/ProjectSelector.vue'

const router = useRouter()

const loading = ref(false)
const reviews = ref([])
const stats = ref({})

/** 审查对话框 */
const reviewDialogVisible = ref(false)
const reviewFormRef = ref(null)
const submitting = ref(false)
const currentReview = ref(null)
const reviewForm = ref({
  approved: true,
  score: 8,
  comment: ''
})
const reviewRules = {
  approved: [{ required: true, message: '请选择审查结果', trigger: 'change' }],
  comment: [{ required: true, message: '请输入评论', trigger: 'blur' }]
}

/** 提交审查对话框 */
const submitDialogVisible = ref(false)
const submitFormRef = ref(null)
const submitForm = ref({
  title: '',
  projectId: '',
  branch: '',
  description: ''
})
const submitRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  projectId: [{ required: true, message: '请输入项目ID', trigger: 'blur' }],
  branch: [{ required: true, message: '请输入分支', trigger: 'blur' }]
}

/** 获取状态标签类型 */
const getStatusType = (status) => {
  const typeMap = {
    'PENDING': 'warning',
    'APPROVED': 'success',
    'REJECTED': 'danger',
    'REVIEWING': 'info'
  }
  return typeMap[status] || 'info'
}

/** 获取状态标签文本 */
const getStatusLabel = (status) => {
  const labelMap = {
    'PENDING': '待审查',
    'APPROVED': '已通过',
    'REJECTED': '已拒绝',
    'REVIEWING': '审查中'
  }
  return labelMap[status] || status
}

/** 格式化时间 */
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

/** 加载审查列表 */
const loadReviews = async () => {
  loading.value = true
  try {
    const [reviewsData, statsData] = await Promise.all([
      reviewApi.getAll(),
      reviewApi.getStats()
    ])
    reviews.value = reviewsData || []
    stats.value = statsData || {}
  } catch (error) {
    ElMessage.error('加载审查列表失败')
  } finally {
    loading.value = false
  }
}

/** 提交审查 */
/** 打开提交审查对话框 */
const handleSubmitReview = () => {
  submitForm.value = {
    title: '',
    projectId: '',
    branch: '',
    description: ''
  }
  submitDialogVisible.value = true
}

/** 提交审查 */
const handleSubmitCreate = async () => {
  try {
    await submitFormRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    await reviewApi.submit(submitForm.value)
    ElMessage.success('审查提交成功')
    submitDialogVisible.value = false
    loadReviews()
  } catch (error) {
    ElMessage.error('提交失败: ' + (error.message || '未知错误'))
  } finally {
    submitting.value = false
  }
}

/** 查看详情 */
const handleViewDetail = (review) => {
  router.push(`/reviews/${review.id}`)
}

/** 打开审查对话框 */
const handleReview = (review) => {
  currentReview.value = review
  reviewForm.value = {
    approved: true,
    score: 8,
    comment: ''
  }
  reviewDialogVisible.value = true
}

/** 提交审查结果 */
const handleReviewSubmit = async () => {
  try {
    await reviewFormRef.value.validate()
    submitting.value = true

    await reviewApi.review(currentReview.value.id, reviewForm.value)
    ElMessage.success('审查已完成')
    reviewDialogVisible.value = false
    loadReviews()
  } catch (error) {
    if (error !== false) {
      ElMessage.error('提交失败')
    }
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadReviews()
})
</script>

<style scoped>
.reviews-page {
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

.pending {
  color: #e6a23c;
}

.approved {
  color: #67c23a;
}

.rejected {
  color: #f56c6c;
}

.stat-label {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.score {
  font-weight: bold;
  color: #409eff;
}
</style>
