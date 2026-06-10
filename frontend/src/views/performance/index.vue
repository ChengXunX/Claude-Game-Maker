<template>
  <div class="performance-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>Agent 绩效管理</span>
          <div class="header-actions">
            <ProjectSelector
              v-model="selectedProjectId"
              placeholder="选择项目"
              width="200px"
              size="default"
              @change="handleProjectChange"
            />
            <el-button type="primary" @click="handleCreateReview" v-permission="'agents:manage'">
              <el-icon><Plus /></el-icon> 发起评估
            </el-button>
            <el-button @click="loadData" :loading="loading">
              <el-icon><Refresh /></el-icon> 刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- 绩效统计 -->
      <el-row :gutter="16" class="stat-cards">
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value">{{ stats.totalReviews || 0 }}</div>
              <div class="stat-label">评审总数</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value grade-a">{{ stats.averageScore || 0 }}</div>
              <div class="stat-label">平均评分</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value grade-s">{{ stats.warningCount || 0 }}</div>
              <div class="stat-label">警告次数</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value grade-b">{{ stats.agentCount || 0 }}</div>
              <div class="stat-label">已评估 Agent</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 标签页 -->
      <el-tabs v-model="activeTab" @tab-click="loadData">
        <!-- 绩效评估 -->
        <el-tab-pane label="绩效评估" name="reviews">
          <!-- 筛选条件 -->
          <el-row :gutter="16" class="filter-row">
            <el-col :span="8">
              <el-select v-model="filterAgentId" placeholder="按 Agent 筛选" clearable filterable @change="loadReviews">
                <el-option v-for="agent in agents" :key="agent.id" :label="`${agent.name} (${agent.role})`" :value="agent.id" />
              </el-select>
            </el-col>
          </el-row>

          <el-table :data="reviews" v-loading="loading" stripe>
            <el-table-column prop="agentName" label="Agent 名称" width="120" />
            <el-table-column prop="agentRole" label="角色" width="100" />
            <el-table-column prop="reviewPeriod" label="评审周期" width="120" />
            <el-table-column label="综合评分" width="100">
              <template #default="{ row }">
                <span class="score" :class="getScoreClass(row.overallScore)">{{ row.overallScore || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="等级" width="80">
              <template #default="{ row }">
                <el-tag :type="getGradeType(row.grade)" size="small" effect="dark">
                  {{ row.grade || '-' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="维度评分" min-width="200">
              <template #default="{ row }">
                <div class="dimension-scores">
                  <span title="质量">质{{ row.qualityScore || '-' }}</span>
                  <span title="效率">效{{ row.efficiencyScore || '-' }}</span>
                  <span title="协作">协{{ row.collaborationScore || '-' }}</span>
                  <span title="创新">创{{ row.innovationScore || '-' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.isWarning ? 'danger' : 'success'" size="small">
                  {{ row.isWarning ? '警告' : '正常' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="producerName" label="评审人" width="120">
              <template #default="{ row }">
                {{ row.producerName || 'AI 自动评估' }}
              </template>
            </el-table-column>
            <el-table-column label="创建时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" text @click="handleViewDetail(row)">
                  详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 解雇申请 -->
        <el-tab-pane label="解雇申请" name="dismissals">
          <el-table :data="dismissals" v-loading="loading" stripe>
            <el-table-column prop="requestNo" label="申请编号" width="150" show-overflow-tooltip />
            <el-table-column prop="agentName" label="Agent 名称" width="120" />
            <el-table-column prop="agentRole" label="角色" width="100" />
            <el-table-column prop="reasonTypeDescription" label="原因类型" width="120" />
            <el-table-column prop="reason" label="原因" min-width="200" show-overflow-tooltip />
            <el-table-column prop="warningCount" label="警告次数" width="100" />
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
                  <el-button type="success" size="small" text @click="handleApproveDismissal(row)" v-permission="'admin:manage'">
                    通过
                  </el-button>
                  <el-button type="danger" size="small" text @click="handleRejectDismissal(row)" v-permission="'admin:manage'">
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
    <el-dialog v-model="reviewDialogVisible" title="发起绩效评估" width="700px">
      <el-form ref="reviewFormRef" :model="reviewForm" :rules="reviewRules" label-width="100px">
        <el-form-item label="Agent" prop="agentId">
          <el-select v-model="reviewForm.agentId" placeholder="选择Agent" filterable>
            <el-option v-for="agent in agents" :key="agent.id" :label="`${agent.name} (${agent.role})`" :value="agent.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="评审周期" prop="reviewPeriod">
          <el-date-picker
            v-model="reviewForm.reviewPeriod"
            type="month"
            placeholder="选择月份"
            value-format="YYYY-MM"
          />
        </el-form-item>

        <el-divider content-position="left">维度评分</el-divider>

        <el-form-item label="任务质量" prop="qualityScore">
          <el-slider v-model="reviewForm.qualityScore" :min="0" :max="100" :step="5" show-input :marks="scoreMarks" />
        </el-form-item>
        <el-form-item label="工作效率" prop="efficiencyScore">
          <el-slider v-model="reviewForm.efficiencyScore" :min="0" :max="100" :step="5" show-input :marks="scoreMarks" />
        </el-form-item>
        <el-form-item label="协作能力" prop="collaborationScore">
          <el-slider v-model="reviewForm.collaborationScore" :min="0" :max="100" :step="5" show-input :marks="scoreMarks" />
        </el-form-item>
        <el-form-item label="创新能力" prop="innovationScore">
          <el-slider v-model="reviewForm.innovationScore" :min="0" :max="100" :step="5" show-input :marks="scoreMarks" />
        </el-form-item>

        <el-divider content-position="left">评价内容</el-divider>

        <el-form-item label="优点">
          <el-input v-model="reviewForm.strengths" type="textarea" :rows="2" placeholder="该 Agent 的优点和亮点" />
        </el-form-item>
        <el-form-item label="待改进">
          <el-input v-model="reviewForm.improvements" type="textarea" :rows="2" placeholder="需要改进的地方" />
        </el-form-item>
        <el-form-item label="综合评价">
          <el-input v-model="reviewForm.comments" type="textarea" :rows="3" placeholder="综合评价和建议" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitReview" :loading="submitting">提交评估</el-button>
      </template>
    </el-dialog>

    <!-- 评审详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="评审详情" width="700px">
      <template v-if="selectedReview">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="评审编号">{{ selectedReview.reviewNo }}</el-descriptions-item>
          <el-descriptions-item label="评审周期">{{ selectedReview.reviewPeriod }}</el-descriptions-item>
          <el-descriptions-item label="Agent">{{ selectedReview.agentName }} ({{ selectedReview.agentRole }})</el-descriptions-item>
          <el-descriptions-item label="评审人">{{ selectedReview.producerName || 'AI 自动评估' }}</el-descriptions-item>
          <el-descriptions-item label="综合评分">
            <span class="score" :class="getScoreClass(selectedReview.overallScore)">{{ selectedReview.overallScore }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="等级">
            <el-tag :type="getGradeType(selectedReview.grade)" effect="dark">{{ selectedReview.grade }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">维度评分</el-divider>
        <el-row :gutter="16">
          <el-col :span="6" v-for="dim in dimensionData" :key="dim.label">
            <div class="dimension-card">
              <div class="dimension-value" :class="getScoreClass(dim.value)">{{ dim.value }}</div>
              <div class="dimension-label">{{ dim.label }}</div>
              <el-progress
                :percentage="dim.value"
                :color="getProgressColor(dim.value)"
                :show-text="false"
                :stroke-width="6"
              />
            </div>
          </el-col>
        </el-row>

        <template v-if="selectedReview.strengths || selectedReview.improvements || selectedReview.comments || selectedReview.highlights">
          <el-divider content-position="left">评价内容</el-divider>
          <div v-if="selectedReview.strengths" class="review-section">
            <h4>优点</h4>
            <p>{{ selectedReview.strengths }}</p>
          </div>
          <div v-if="selectedReview.improvements" class="review-section">
            <h4>待改进</h4>
            <p>{{ selectedReview.improvements }}</p>
          </div>
          <div v-if="selectedReview.highlights" class="review-section">
            <h4>工作亮点</h4>
            <p>{{ selectedReview.highlights }}</p>
          </div>
          <div v-if="selectedReview.comments" class="review-section">
            <h4>综合评价</h4>
            <p>{{ selectedReview.comments }}</p>
          </div>
        </template>

        <template v-if="selectedReview.isWarning">
          <el-divider content-position="left">警告信息</el-divider>
          <el-alert type="error" :closable="false">
            {{ selectedReview.warningReason }}
          </el-alert>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * Agent 绩效管理页面（合并版）
 * 查看和管理 Agent 绩效评审记录，支持手动和 AI 自动评估
 *
 * 功能：
 * - 绩效统计概览
 * - 评审列表（支持按 Agent 筛选）
 * - 解雇申请管理
 * - 发起手动评估
 * - 查看评审详情
 *
 * 操作维度：系统级/项目级
 * 权限要求：agents:view（查看）、agents:manage（管理）
 */
import { ref, computed, onMounted } from 'vue'
import { performanceMgmtApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'
import ProjectSelector from '@/components/ProjectSelector.vue'

const loading = ref(false)
const activeTab = ref('reviews')
const reviews = ref([])
const dismissals = ref([])
const agents = ref([])
const stats = ref({})
const selectedProjectId = ref(localStorage.getItem('selectedProjectId') || '')
const filterAgentId = ref('')

// 评估表单
const reviewDialogVisible = ref(false)
const reviewFormRef = ref(null)
const submitting = ref(false)
const reviewForm = ref({
  agentId: '',
  reviewPeriod: '',
  qualityScore: 80,
  efficiencyScore: 80,
  collaborationScore: 80,
  innovationScore: 80,
  strengths: '',
  improvements: '',
  comments: ''
})
const reviewRules = {
  agentId: [{ required: true, message: '请选择Agent', trigger: 'change' }],
  reviewPeriod: [{ required: true, message: '请选择评审周期', trigger: 'change' }],
  qualityScore: [{ required: true, message: '请设置质量评分', trigger: 'change' }],
  efficiencyScore: [{ required: true, message: '请设置效率评分', trigger: 'change' }],
  collaborationScore: [{ required: true, message: '请设置协作评分', trigger: 'change' }],
  innovationScore: [{ required: true, message: '请设置创新评分', trigger: 'change' }]
}

const scoreMarks = {
  0: '0',
  50: '50',
  60: '及格',
  80: '良好',
  100: '优秀'
}

// 评审详情
const detailDialogVisible = ref(false)
const selectedReview = ref(null)

const dimensionData = computed(() => {
  if (!selectedReview.value) return []
  return [
    { label: '任务质量', value: selectedReview.value.qualityScore || 0 },
    { label: '工作效率', value: selectedReview.value.efficiencyScore || 0 },
    { label: '协作能力', value: selectedReview.value.collaborationScore || 0 },
    { label: '创新能力', value: selectedReview.value.innovationScore || 0 }
  ]
})

/** 项目切换 */
const handleProjectChange = () => {
  loadData()
}

/** 获取分数样式 */
const getScoreClass = (score) => {
  if (score >= 90) return 'score-excellent'
  if (score >= 80) return 'score-good'
  if (score >= 60) return 'score-pass'
  return 'score-fail'
}

/** 获取等级标签类型 */
const getGradeType = (grade) => {
  const typeMap = { 'A': 'danger', 'B': 'warning', 'C': 'success', 'D': 'info', 'F': '' }
  return typeMap[grade] || 'info'
}

/** 获取进度条颜色 */
const getProgressColor = (score) => {
  if (score >= 90) return '#f56c6c'
  if (score >= 80) return '#e6a23c'
  if (score >= 60) return '#67c23a'
  return '#909399'
}

/** 获取解雇状态标签类型 */
const getDismissalStatusType = (status) => {
  const typeMap = { 'PENDING': 'warning', 'APPROVED': 'success', 'REJECTED': 'danger', 'EXECUTED': 'info' }
  return typeMap[status] || 'info'
}

/** 获取解雇状态标签文本 */
const getDismissalStatusLabel = (status) => {
  const labelMap = { 'PENDING': '待审批', 'APPROVED': '已通过', 'REJECTED': '已拒绝', 'EXECUTED': '已执行' }
  return labelMap[status] || status
}

/** 格式化时间 */
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

/** 加载所有数据 */
const loadData = async () => {
  loading.value = true
  try {
    await Promise.all([
      loadReviews(),
      loadAgents()
    ])
  } catch (error) {
    console.error('加载数据失败', error)
  } finally {
    loading.value = false
  }
}

/** 加载评审记录 */
const loadReviews = async () => {
  try {
    let url = '/performance-management/reviews/producer/all'
    if (filterAgentId.value) {
      url = `/performance-management/reviews/agent/${filterAgentId.value}`
    }
    const data = await api.get(url)
    reviews.value = data || []
    calculateStats()
  } catch (error) {
    console.error('加载评审数据失败', error)
  }
}

/** 加载解雇申请 */
const loadDismissals = async () => {
  try {
    const data = await performanceMgmtApi.getDismissals()
    dismissals.value = data || []
  } catch (error) {
    console.error('加载解雇数据失败', error)
  }
}

/** 加载 Agent 列表 */
const loadAgents = async () => {
  if (agents.value.length > 0) return
  try {
    const data = await api.get('/agents')
    agents.value = data || []
  } catch (error) {
    console.error('加载Agent列表失败', error)
  }
}

/** 计算统计数据 */
const calculateStats = () => {
  const reviewList = reviews.value
  if (!reviewList || reviewList.length === 0) {
    stats.value = { totalReviews: 0, averageScore: 0, warningCount: 0, agentCount: 0 }
    return
  }

  const totalReviews = reviewList.length
  const totalScore = reviewList.reduce((sum, r) => sum + (r.overallScore || 0), 0)
  const averageScore = Math.round(totalScore / totalReviews)
  const warningCount = reviewList.filter(r => r.isWarning).length
  const agentIds = new Set(reviewList.map(r => r.agentId))

  stats.value = {
    totalReviews,
    averageScore,
    warningCount,
    agentCount: agentIds.size
  }
}

/** 发起评估 */
const handleCreateReview = async () => {
  reviewForm.value = {
    agentId: '',
    reviewPeriod: '',
    qualityScore: 80,
    efficiencyScore: 80,
    collaborationScore: 80,
    innovationScore: 80,
    strengths: '',
    improvements: '',
    comments: ''
  }
  reviewDialogVisible.value = true
  await loadAgents()
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
      reviewPeriod: reviewForm.value.reviewPeriod,
      qualityScore: reviewForm.value.qualityScore,
      efficiencyScore: reviewForm.value.efficiencyScore,
      collaborationScore: reviewForm.value.collaborationScore,
      innovationScore: reviewForm.value.innovationScore,
      strengths: reviewForm.value.strengths,
      improvements: reviewForm.value.improvements,
      comments: reviewForm.value.comments
    }
    await performanceMgmtApi.createReview(data)
    ElMessage.success('评估已提交')
    reviewDialogVisible.value = false
    loadReviews()
  } catch (error) {
    ElMessage.error('提交失败')
  } finally {
    submitting.value = false
  }
}

/** 查看详情 */
const handleViewDetail = (review) => {
  selectedReview.value = review
  detailDialogVisible.value = true
}

/** 通过解雇 */
const handleApproveDismissal = async (dismissal) => {
  try {
    await ElMessageBox.confirm(
      `确定通过 Agent "${dismissal.agentName}" 的解雇申请吗？`,
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

/** 拒绝解雇 */
const handleRejectDismissal = async (dismissal) => {
  try {
    const { value: reason } = await ElMessageBox.prompt(
      `请输入拒绝 Agent "${dismissal.agentName}" 解雇的原因：`,
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
.performance-page {
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
  gap: 12px;
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

.grade-s {
  color: #f56c6c;
}

.grade-a {
  color: #e6a23c;
}

.grade-b {
  color: #67c23a;
}

.stat-label {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.filter-row {
  margin-bottom: 16px;
}

.score {
  font-weight: bold;
  font-size: 16px;
}

.score-excellent {
  color: #f56c6c;
}

.score-good {
  color: #e6a23c;
}

.score-pass {
  color: #67c23a;
}

.score-fail {
  color: #909399;
}

.dimension-scores {
  display: flex;
  gap: 8px;
  font-size: 12px;
  color: #606266;
}

.dimension-scores span {
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 4px;
}

.dimension-card {
  text-align: center;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.dimension-value {
  font-size: 28px;
  font-weight: bold;
}

.dimension-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  margin-bottom: 8px;
}

.review-section {
  margin-bottom: 16px;
}

.review-section h4 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 14px;
}

.review-section p {
  margin: 0;
  color: #606266;
  line-height: 1.6;
}
</style>
