<template>
  <div class="performance-detail-page">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>绩效详情</span>
          <el-button @click="router.back()">返回</el-button>
        </div>
      </template>

      <template v-if="performance">
        <!-- Agent信息 -->
        <el-descriptions :column="2" border>
          <el-descriptions-item label="Agent ID">{{ performance.agentId }}</el-descriptions-item>
          <el-descriptions-item label="Agent名称">{{ performance.agentName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="角色">{{ performance.role || '-' }}</el-descriptions-item>
          <el-descriptions-item label="项目">{{ performance.projectId || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 绩效指标 -->
        <div class="metrics-section">
          <h4>绩效指标</h4>
          <el-row :gutter="16">
            <el-col :span="6">
              <el-card shadow="hover">
                <div class="metric-item">
                  <div class="metric-value">{{ performance.totalTasks || 0 }}</div>
                  <div class="metric-label">总任务数</div>
                </div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="hover">
                <div class="metric-item">
                  <div class="metric-value success">{{ performance.completedTasks || 0 }}</div>
                  <div class="metric-label">完成任务</div>
                </div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="hover">
                <div class="metric-item">
                  <div class="metric-value warning">{{ performance.failedTasks || 0 }}</div>
                  <div class="metric-label">失败任务</div>
                </div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="hover">
                <div class="metric-item">
                  <div class="metric-value primary">{{ performance.avgResponseTime || 0 }}ms</div>
                  <div class="metric-label">平均响应时间</div>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>

        <!-- 绩效评分 -->
        <div class="score-section">
          <h4>绩效评分</h4>
          <div class="score-display">
            <el-progress
              type="dashboard"
              :percentage="performance.score || 0"
              :color="getScoreColor(performance.score)"
            />
            <div class="score-label">
              <el-tag :type="getScoreType(performance.score)" size="large">
                {{ getScoreLabel(performance.score) }}
              </el-tag>
            </div>
          </div>
        </div>

        <!-- 优化建议 -->
        <div v-if="recommendations.length > 0" class="recommendations-section">
          <h4>优化建议</h4>
          <el-timeline>
            <el-timeline-item
              v-for="(rec, index) in recommendations"
              :key="index"
              :type="rec.priority === 'HIGH' ? 'danger' : rec.priority === 'MEDIUM' ? 'warning' : 'info'"
            >
              <h5>{{ rec.title }}</h5>
              <p>{{ rec.description }}</p>
            </el-timeline-item>
          </el-timeline>
        </div>
      </template>

      <el-empty v-else-if="!loading" description="绩效数据不存在" />
    </el-card>
  </div>
</template>

<script setup>
/**
 * 绩效详情页面
 * 查看Agent绩效详情和优化建议
 */
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { performanceApi } from '@/api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const performance = ref(null)
const recommendations = ref([])

/** 获取分数颜色 */
const getScoreColor = (score) => {
  if (score >= 80) return '#67c23a'
  if (score >= 60) return '#e6a23c'
  return '#f56c6c'
}

/** 获取分数类型 */
const getScoreType = (score) => {
  if (score >= 80) return 'success'
  if (score >= 60) return 'warning'
  return 'danger'
}

/** 获取分数标签 */
const getScoreLabel = (score) => {
  if (score >= 80) return '优秀'
  if (score >= 60) return '良好'
  return '需改进'
}

/** 加载绩效详情 */
const loadPerformance = async () => {
  const agentId = route.params.agentId
  if (!agentId) return

  loading.value = true
  try {
    const [perfData, recData] = await Promise.all([
      performanceApi.getByAgent(agentId),
      performanceApi.getRecommendations(agentId)
    ])
    performance.value = perfData
    recommendations.value = recData || []
  } catch (error) {
    ElMessage.error('加载绩效详情失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadPerformance()
})
</script>

<style scoped>
.performance-detail-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.metrics-section,
.score-section,
.recommendations-section {
  margin-top: 24px;
}

.metrics-section h4,
.score-section h4,
.recommendations-section h4 {
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--el-border-color);
}

.metric-item {
  text-align: center;
  padding: 16px;
}

.metric-value {
  font-size: 28px;
  font-weight: bold;
  color: var(--el-text-color-primary);
}

.metric-value.success {
  color: var(--el-color-success);
}

.metric-value.warning {
  color: var(--el-color-warning);
}

.metric-value.primary {
  color: var(--el-color-primary);
}

.metric-label {
  margin-top: 8px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.score-display {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}
</style>
