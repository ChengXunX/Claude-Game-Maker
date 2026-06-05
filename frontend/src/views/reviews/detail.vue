<template>
  <div class="review-detail-page">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>审查详情</span>
          <el-button @click="router.back()">返回</el-button>
        </div>
      </template>

      <template v-if="review">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="标题" :span="2">{{ review.title }}</el-descriptions-item>
          <el-descriptions-item label="提交者">{{ review.agentName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="项目">{{ review.projectName || review.projectId }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(review.status)" size="small">
              {{ getStatusLabel(review.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="评分">
            <span class="score">{{ review.score || '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ formatTime(review.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="审查时间">{{ formatTime(review.reviewedAt) }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ review.description || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 审查结果 -->
        <div v-if="review.comment" class="review-comment">
          <h4>审查意见</h4>
          <div class="comment-content">{{ review.comment }}</div>
        </div>

        <!-- 操作按钮 -->
        <div v-if="review.status === 'PENDING'" class="actions">
          <el-button type="success" @click="handleApprove">通过</el-button>
          <el-button type="danger" @click="handleReject">拒绝</el-button>
        </div>
      </template>

      <el-empty v-else-if="!loading" description="审查不存在" />
    </el-card>
  </div>
</template>

<script setup>
/**
 * 审查详情页面
 * 查看代码审查详情
 */
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { reviewApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const review = ref(null)

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

/** 加载审查详情 */
const loadReview = async () => {
  const id = route.params.id
  if (!id) return

  loading.value = true
  try {
    const data = await reviewApi.getById(id)
    review.value = data
  } catch (error) {
    ElMessage.error('加载审查详情失败')
  } finally {
    loading.value = false
  }
}

/** 通过审查 */
const handleApprove = async () => {
  try {
    const { value: comment } = await ElMessageBox.prompt('请输入审查意见', '通过审查', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputValue: '代码质量良好，审核通过'
    })

    await reviewApi.review(route.params.id, { approved: true, score: 9, comment })
    ElMessage.success('审查已通过')
    loadReview()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

/** 拒绝审查 */
const handleReject = async () => {
  try {
    const { value: comment } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝审查', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputType: 'textarea'
    })

    if (!comment) {
      ElMessage.warning('请输入拒绝原因')
      return
    }

    await reviewApi.review(route.params.id, { approved: false, score: 3, comment })
    ElMessage.success('审查已拒绝')
    loadReview()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

onMounted(() => {
  loadReview()
})
</script>

<style scoped>
.review-detail-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.score {
  font-size: 18px;
  font-weight: bold;
  color: var(--el-color-primary);
}

.review-comment {
  margin-top: 24px;
}

.review-comment h4 {
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--el-border-color);
}

.comment-content {
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  line-height: 1.6;
}

.actions {
  margin-top: 24px;
  display: flex;
  gap: 12px;
}
</style>
