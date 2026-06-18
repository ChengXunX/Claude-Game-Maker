<template>
  <div class="supervision-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <el-button text @click="router.back()">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
        <h2 class="page-title">督查报告</h2>
        <el-tag v-if="project" type="info" size="small">{{ project.name }}</el-tag>
      </div>
      <div class="header-actions">
        <el-button size="small" @click="handleExportReport">
          <el-icon><Download /></el-icon> 导出报告
        </el-button>
        <el-button size="small" @click="router.push('/admin/system/settings?tab=supervision')">
          <el-icon><Setting /></el-icon> 督查规则
        </el-button>
        <el-button size="small" @click="loadAllData" :loading="loading">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="5" animated />
    </div>

    <template v-else-if="supervisionReport">
      <!-- 督查概览统计 -->
      <el-row :gutter="16" class="supervision-stats">
        <el-col :xs="12" :sm="8" :md="4">
          <el-card shadow="hover" class="stat-card-mini">
            <div class="stat-mini" :class="supervisionReport.overdueTasks > 0 ? 'stat-danger' : 'stat-success'">
              <div class="stat-mini-value">{{ supervisionReport.overdueTasks }}</div>
              <div class="stat-mini-label">逾期任务</div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="8" :md="4">
          <el-card shadow="hover" class="stat-card-mini">
            <div class="stat-mini">
              <div class="stat-mini-value">{{ supervisionReport.totalTasks }}</div>
              <div class="stat-mini-label">总任务</div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="8" :md="4">
          <el-card shadow="hover" class="stat-card-mini">
            <div class="stat-mini">
              <div class="stat-mini-value">{{ supervisionReport.completedTasks }}</div>
              <div class="stat-mini-label">已完成</div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="8" :md="4">
          <el-card shadow="hover" class="stat-card-mini">
            <div class="stat-mini">
              <div class="stat-mini-value">{{ supervisionReport.taskCompletionRate?.toFixed(1) }}%</div>
              <div class="stat-mini-label">完成率</div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="8" :md="4">
          <el-card shadow="hover" class="stat-card-mini">
            <div class="stat-mini">
              <div class="stat-mini-value">{{ supervisionReport.completedMilestones }}/{{ supervisionReport.totalMilestones }}</div>
              <div class="stat-mini-label">里程碑</div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="8" :md="4">
          <el-card shadow="hover" class="stat-card-mini">
            <div class="stat-mini">
              <div class="stat-mini-value">{{ supervisionReport.versionCount }}</div>
              <div class="stat-mini-label">迭代次数</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- Tab 切换 -->
      <el-card class="content-card">
        <el-tabs v-model="activeTab" class="supervision-tabs">
          <!-- 甘特图视图 -->
          <el-tab-pane label="甘特图" name="gantt">
            <div class="gantt-view">
              <div v-for="(milestone, mi) in supervisionReport.milestones" :key="milestone.id" class="gantt-milestone">
                <div class="gantt-milestone-header">
                  <el-tag :type="getMilestoneTagType(milestone.status)" size="small">{{ getMilestoneStatusText(milestone.status) }}</el-tag>
                  <span class="gantt-milestone-title">{{ milestone.title }}</span>
                  <span class="gantt-milestone-role">{{ milestone.assignedRole }}</span>
                  <el-progress :percentage="milestone.progress || 0" :stroke-width="6" style="width: 120px;" />
                </div>
                <div class="gantt-tasks">
                  <div v-for="task in milestone.tasks" :key="task.id" class="gantt-task-row">
                    <div class="gantt-task-label">
                      <el-icon :size="12" :style="{ color: task.status === 'COMPLETED' ? '#67c23a' : '#909399' }">
                        <CircleCheck v-if="task.status === 'COMPLETED'" />
                        <Clock v-else />
                      </el-icon>
                      <span :class="{ 'text-done': task.status === 'COMPLETED' }">{{ task.title }}</span>
                    </div>
                    <div class="gantt-task-bar-area">
                      <div
                        class="gantt-task-bar"
                        :style="{
                          width: getGanttBarWidth(task) + '%',
                          backgroundColor: getGanttBarColor(task)
                        }"
                      >
                        <span class="gantt-bar-text">{{ task.estimatedHours }}h</span>
                      </div>
                    </div>
                    <div class="gantt-task-status">
                      <el-tag :type="getTaskStatusType(task.status)" size="small">{{ getTaskStatusText(task.status) }}</el-tag>
                    </div>
                  </div>
                </div>
              </div>
              <el-empty v-if="!supervisionReport.milestones?.length" description="暂无里程碑数据" />
            </div>
          </el-tab-pane>

          <!-- Agent效率分析 -->
          <el-tab-pane label="Agent效率" name="efficiency">
            <div v-if="supervisionReport.agentEfficiency?.length > 0">
              <el-row :gutter="16">
                <el-col :xs="24" :sm="12" :lg="8" v-for="agent in supervisionReport.agentEfficiency" :key="agent.role">
                  <el-card shadow="hover" class="efficiency-card">
                    <div class="efficiency-header">
                      <span class="efficiency-role">{{ agent.role }}</span>
                      <el-tag :type="agent.overdueTasks > 0 ? 'danger' : 'success'" size="small">
                        {{ agent.overdueTasks > 0 ? agent.overdueTasks + '个逾期' : '正常' }}
                      </el-tag>
                    </div>
                    <el-row :gutter="12">
                      <el-col :span="12">
                        <el-statistic title="总任务" :value="agent.totalTasks" />
                      </el-col>
                      <el-col :span="12">
                        <el-statistic title="已完成" :value="agent.completedTasks" />
                      </el-col>
                    </el-row>
                    <el-row :gutter="12" style="margin-top: 12px;">
                      <el-col :span="12">
                        <el-statistic title="完成率" :value="agent.completionRate" :precision="1" suffix="%" />
                      </el-col>
                      <el-col :span="12">
                        <el-statistic title="逾期" :value="agent.overdueTasks" />
                      </el-col>
                    </el-row>
                    <el-progress :percentage="agent.completionRate" :status="agent.completionRate >= 80 ? 'success' : agent.completionRate >= 50 ? '' : 'exception'" style="margin-top: 12px;" />
                  </el-card>
                </el-col>
              </el-row>
            </div>
            <el-empty v-else description="暂无效率数据" :image-size="60" />
          </el-tab-pane>

          <!-- 协作效率 -->
          <el-tab-pane label="协作效率" name="collaboration">
            <div v-if="collaborationMetrics">
              <el-row :gutter="16" style="margin-bottom: 16px;">
                <el-col :span="6">
                  <el-statistic title="总任务数" :value="collaborationMetrics.totalTasks" />
                </el-col>
                <el-col :span="6">
                  <el-statistic title="已完成" :value="collaborationMetrics.completedTasks" />
                </el-col>
                <el-col :span="6">
                  <el-statistic title="返工任务" :value="collaborationMetrics.reworkedTasks" />
                </el-col>
                <el-col :span="6">
                  <el-statistic title="返工率" :value="collaborationMetrics.reworkRate" :precision="1" suffix="%" />
                </el-col>
              </el-row>
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-card shadow="hover" class="metric-card">
                    <div class="metric-title">返工率</div>
                    <el-progress :percentage="collaborationMetrics.reworkRate" :precision="1"
                      :status="collaborationMetrics.reworkRate > 20 ? 'exception' : collaborationMetrics.reworkRate < 5 ? 'success' : ''" />
                    <div class="metric-hint">
                      {{ collaborationMetrics.reworkRate > 20 ? '返工率偏高，建议检查质量标准' : collaborationMetrics.reworkRate < 5 ? '返工率良好' : '返工率正常' }}
                    </div>
                  </el-card>
                </el-col>
                <el-col :span="12">
                  <el-card shadow="hover" class="metric-card">
                    <div class="metric-title">预估工时</div>
                    <div class="metric-value">{{ collaborationMetrics.totalEstimatedHours }} 小时</div>
                    <div class="metric-hint">所有任务的预估工时总和</div>
                  </el-card>
                </el-col>
              </el-row>
            </div>
            <el-empty v-else description="暂无协作效率数据" :image-size="60" />
          </el-tab-pane>

          <!-- 迭代趋势 -->
          <el-tab-pane label="迭代趋势" name="trends">
            <div v-if="iterationTrends">
              <div v-if="iterationTrends.versionTrend?.length > 0" class="trend-chart-section">
                <div class="section-title">版本评分趋势</div>
                <div class="trend-bars">
                  <div v-for="v in iterationTrends.versionTrend" :key="v.version" class="trend-bar-item">
                    <div class="trend-bar-wrapper">
                      <div class="trend-bar" :style="{ height: (v.score * 10) + '%', backgroundColor: getScoreColor(v.score) }">
                        <span class="trend-bar-value">{{ v.score }}</span>
                      </div>
                    </div>
                    <div class="trend-bar-label">{{ v.version }}</div>
                  </div>
                </div>
              </div>
              <div v-if="iterationEfficiency" class="efficiency-summary" style="margin-top: 24px;">
                <el-row :gutter="16">
                  <el-col :span="8">
                    <el-statistic title="平均迭代周期" :value="iterationEfficiency.averageCycleHours" :precision="0" suffix="小时" />
                  </el-col>
                  <el-col :span="8">
                    <el-statistic title="预估总工时" :value="iterationEfficiency.totalEstimatedHours" suffix="小时" />
                  </el-col>
                  <el-col :span="8">
                    <el-statistic title="实际总工时" :value="iterationEfficiency.totalActualHours" suffix="小时" />
                  </el-col>
                </el-row>
              </div>
            </div>
            <el-empty v-else description="暂无趋势数据" :image-size="60" />
          </el-tab-pane>

          <!-- 逾期任务 -->
          <el-tab-pane name="overdue">
            <template #label>
              逾期任务
              <el-badge v-if="supervisionReport.overdueTasks > 0" :value="supervisionReport.overdueTasks" :max="99" class="overdue-badge" />
            </template>
            <div v-if="supervisionReport.overdueTasks > 0">
              <el-table :data="overdueTasks" stripe>
                <el-table-column label="任务" prop="title" min-width="200" />
                <el-table-column label="里程碑" prop="milestoneTitle" width="150" />
                <el-table-column label="负责角色" prop="assignedRole" width="120" />
                <el-table-column label="预估工时" width="100">
                  <template #default="{ row }">{{ row.estimatedHours }}h</template>
                </el-table-column>
              </el-table>
            </div>
            <el-empty v-else description="暂无逾期任务" :image-size="60" />
          </el-tab-pane>

          <!-- 玩家体验 -->
          <el-tab-pane label="玩家体验" name="playerExperience">
            <div v-if="playerExperience">
              <div class="fun-score-header">
                <div class="fun-score-circle" :class="getFunScoreClass(playerExperience.overallScore)">
                  <span class="fun-score-value">{{ playerExperience.overallScore }}</span>
                  <span class="fun-score-label">趣味度</span>
                </div>
                <div class="fun-score-status">
                  {{ playerExperience.overallScore >= 70 ? '游戏体验良好' : playerExperience.overallScore >= 50 ? '有改进空间' : '需要重点优化' }}
                </div>
                <el-tag v-if="playerExperience.dataSource" size="small" :type="playerExperience.dataSource === 'AI_DEEP' ? 'success' : playerExperience.dataSource === 'CODE_FEATURE' ? 'warning' : 'info'" style="margin-top: 4px;">
                  {{ playerExperience.dataSource === 'AI_DEEP' ? 'AI深度分析' : playerExperience.dataSource === 'CODE_FEATURE' ? '代码特征' : '关键词估算' }}
                </el-tag>
              </div>
              <el-row :gutter="16" style="margin-top: 16px;">
                <el-col :xs="8" :sm="4" v-for="(score, key) in playerExperience.dimensionScores" :key="key">
                  <div class="dimension-score">
                    <el-progress type="circle" :percentage="score" :width="80"
                      :color="getDimensionColor(score)" :stroke-width="8" />
                    <div class="dimension-label">{{ getDimensionName(key) }}</div>
                  </div>
                </el-col>
              </el-row>
              <el-card shadow="hover" style="margin-top: 16px;">
                <div class="metric-title">功能完整度</div>
                <el-progress :percentage="playerExperience.featureCompleteness" :precision="1"
                  :status="playerExperience.featureCompleteness >= 80 ? 'success' : ''" />
                <div class="metric-hint">已完成 {{ playerExperience.completedFeatures }}/{{ playerExperience.totalFeatures }} 个功能</div>
              </el-card>
            </div>
            <el-empty v-else description="暂无玩家体验数据" :image-size="60" />
          </el-tab-pane>

          <!-- 风险预测 -->
          <el-tab-pane label="风险预测" name="riskPrediction">
            <div v-if="riskPrediction">
              <el-row :gutter="16" style="margin-bottom: 16px;">
                <el-col :span="8">
                  <el-statistic title="风险数量" :value="riskPrediction.riskCount" />
                </el-col>
                <el-col :span="8">
                  <el-tag :type="riskPrediction.hasCritical ? 'danger' : riskPrediction.riskCount > 0 ? 'warning' : 'success'" size="large">
                    {{ riskPrediction.hasCritical ? '严重' : riskPrediction.riskCount > 0 ? '警告' : '安全' }}
                  </el-tag>
                </el-col>
              </el-row>
              <div v-if="riskPrediction.risks?.length > 0">
                <el-card v-for="(risk, idx) in riskPrediction.risks" :key="idx" shadow="hover" class="risk-card">
                  <div class="risk-header">
                    <el-tag :type="getRiskSeverityType(risk.severity)" size="small">{{ risk.severity }}</el-tag>
                    <span class="risk-type">{{ risk.type }}</span>
                  </div>
                  <div class="risk-description">{{ risk.description }}</div>
                  <div class="risk-suggestion">
                    <el-icon color="#409EFF"><InfoFilled /></el-icon>
                    <span>{{ risk.suggestion }}</span>
                  </div>
                </el-card>
              </div>
              <el-empty v-else description="当前无显著风险" :image-size="60" />
            </div>
            <el-empty v-else description="暂无风险数据" :image-size="60" />
          </el-tab-pane>

          <!-- 质量基准 -->
          <el-tab-pane label="质量基准" name="qualityBenchmark">
            <div v-if="qualityBenchmark">
              <el-card shadow="hover" style="margin-bottom: 16px;">
                <div class="benchmark-header">
                  <span class="benchmark-title">功能完整度</span>
                  <el-tag :type="qualityBenchmark.completeness >= 70 ? 'success' : qualityBenchmark.completeness >= 40 ? '' : 'warning'">
                    {{ qualityBenchmark.developmentStatus }}
                  </el-tag>
                </div>
                <el-progress :percentage="qualityBenchmark.completeness" :precision="1" :stroke-width="12"
                  :status="qualityBenchmark.completeness >= 80 ? 'success' : ''" />
                <div class="metric-hint">{{ qualityBenchmark.completedMilestones }}/{{ qualityBenchmark.totalMilestones }} 里程碑已完成</div>
              </el-card>
              <div v-if="qualityBenchmark.checklist?.length > 0">
                <div class="section-title">发布前Checklist</div>
                <div v-for="(item, idx) in qualityBenchmark.checklist" :key="idx" class="checklist-item">
                  <el-checkbox disabled />
                  <span>{{ item.item }}</span>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无基准数据" :image-size="60" />
          </el-tab-pane>

          <!-- 迭代回顾 -->
          <el-tab-pane label="迭代回顾" name="retrospective">
            <div v-if="iterationStats.totalIterations > 0">
              <el-row :gutter="16" class="retro-stats">
                <el-col :span="6">
                  <el-statistic title="迭代次数" :value="iterationStats.totalIterations" suffix="次" />
                </el-col>
                <el-col :span="6">
                  <el-statistic title="通过次数" :value="iterationStats.passedIterations" suffix="次" />
                </el-col>
                <el-col :span="6">
                  <el-statistic title="通过率" :value="iterationStats.passRate" :precision="1" suffix="%" />
                </el-col>
                <el-col :span="6">
                  <el-statistic title="平均评分" :value="iterationStats.averageScore" :precision="1" suffix="/10" />
                </el-col>
              </el-row>
            </div>
            <el-empty v-else description="暂无迭代数据" :image-size="60" />
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </template>

    <el-empty v-else description="暂无督查数据" :image-size="80" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { projectApi } from '@/api'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft, Download, Setting, Refresh, CircleCheck, Clock, InfoFilled
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const project = ref(null)
const activeTab = ref('gantt')

// 督查数据
const supervisionReport = ref(null)
const iterationTrends = ref(null)
const iterationEfficiency = ref(null)
const collaborationMetrics = ref(null)
const playerExperience = ref(null)
const riskPrediction = ref(null)
const qualityBenchmark = ref(null)
const iterationStats = ref({ totalIterations: 0, passedIterations: 0, passRate: 0, averageScore: 0 })

const projectId = computed(() => route.params.projectId)

// 逾期任务列表
const overdueTasks = computed(() => {
  if (!supervisionReport.value?.milestones) return []
  const tasks = []
  for (const m of supervisionReport.value.milestones) {
    for (const t of m.tasks || []) {
      if (t.status === 'IN_PROGRESS' && isTaskOverdue(t)) {
        tasks.push({ ...t, milestoneTitle: m.title })
      }
    }
  }
  return tasks
})

const isTaskOverdue = (task) => {
  if (!task.startedAt || !task.estimatedHours || task.estimatedHours <= 0) return false
  const startedAt = new Date(task.startedAt)
  const expectedCompletion = new Date(startedAt.getTime() + task.estimatedHours * 60 * 60 * 1000)
  return new Date() > expectedCompletion
}

const getGanttBarWidth = (task) => {
  if (!task.estimatedHours) return 10
  return Math.min(100, Math.max(10, task.estimatedHours * 5))
}

const getGanttBarColor = (task) => {
  if (task.status === 'COMPLETED') return '#67c23a'
  if (task.status === 'IN_PROGRESS' && isTaskOverdue(task)) return '#f56c6c'
  if (task.status === 'IN_PROGRESS') return '#409eff'
  return '#c0c4cc'
}

const getMilestoneTagType = (status) => {
  const typeMap = { 'PENDING': 'info', 'IN_PROGRESS': 'primary', 'COMPLETED': 'success', 'BLOCKED': 'warning' }
  return typeMap[status] || 'info'
}

const getMilestoneStatusText = (status) => {
  const textMap = { 'PENDING': '待开始', 'IN_PROGRESS': '进行中', 'COMPLETED': '已完成', 'BLOCKED': '已阻塞' }
  return textMap[status] || status
}

const getTaskStatusType = (status) => {
  const typeMap = { 'PENDING': 'info', 'IN_PROGRESS': 'primary', 'COMPLETED': 'success', 'BLOCKED': 'warning' }
  return typeMap[status] || 'info'
}

const getTaskStatusText = (status) => {
  const textMap = { 'PENDING': '待开始', 'IN_PROGRESS': '进行中', 'COMPLETED': '已完成', 'BLOCKED': '已阻塞' }
  return textMap[status] || status
}

const getScoreColor = (score) => {
  if (score >= 8) return '#67c23a'
  if (score >= 6) return '#e6a23c'
  return '#f56c6c'
}

const getFunScoreClass = (score) => {
  if (score >= 70) return 'fun-score-good'
  if (score >= 50) return 'fun-score-medium'
  return 'fun-score-low'
}

const getDimensionColor = (score) => {
  if (score >= 70) return '#67c23a'
  if (score >= 50) return '#e6a23c'
  return '#f56c6c'
}

const getDimensionName = (key) => {
  const names = { coreLoop: '核心循环', challenge: '挑战感', reward: '奖励反馈', progression: '进度感', novelty: '新颖度' }
  return names[key] || key
}

const getRiskSeverityType = (severity) => {
  const types = { CRITICAL: 'danger', HIGH: 'warning', MEDIUM: '', LOW: 'info' }
  return types[severity] || ''
}

const loadAllData = async () => {
  loading.value = true
  try {
    const id = projectId.value
    const authHeader = { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    const safeFetch = (url) => fetch(url, { headers: authHeader }).then(r => r.ok ? r.json() : null).catch(() => null)

    // 加载项目信息
    const projectData = await projectApi.getById(id).catch(() => null)
    if (projectData) project.value = projectData

    // 加载迭代统计
    const statsData = await projectApi.getIterationStats(id).catch(() => null)
    if (statsData) iterationStats.value = statsData

    // 并行加载督查数据
    const [reportRes, trendsRes, efficiencyRes, collaborationRes, experienceRes, riskRes, benchmarkRes] = await Promise.all([
      safeFetch(`/api/projects/api/${id}/supervision-report`),
      safeFetch(`/api/projects/api/${id}/supervision-trends`),
      safeFetch(`/api/projects/api/${id}/iteration-efficiency`),
      safeFetch(`/api/projects/api/${id}/collaboration-metrics`),
      safeFetch(`/api/projects/api/${id}/player-experience`),
      safeFetch(`/api/projects/api/${id}/risk-prediction`),
      safeFetch(`/api/projects/api/${id}/quality-benchmark`)
    ])

    if (reportRes) supervisionReport.value = reportRes
    if (trendsRes) iterationTrends.value = trendsRes
    if (efficiencyRes) iterationEfficiency.value = efficiencyRes
    if (collaborationRes) collaborationMetrics.value = collaborationRes
    if (experienceRes) playerExperience.value = experienceRes
    if (riskRes) riskPrediction.value = riskRes
    if (benchmarkRes) qualityBenchmark.value = benchmarkRes
  } catch (error) {
    console.error('加载督查数据失败:', error)
  } finally {
    loading.value = false
  }
}

const handleExportReport = async () => {
  try {
    const response = await fetch(`/api/projects/${projectId.value}/export-iteration-text`, {
      headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    })
    if (response.ok) {
      const result = await response.json()
      const blob = new Blob([result.content], { type: 'text/markdown;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = result.fileName
      a.click()
      URL.revokeObjectURL(url)
      ElMessage.success('报告已导出')
    }
  } catch (error) {
    ElMessage.error('导出失败')
  }
}

onMounted(() => {
  if (projectId.value) {
    loadAllData()
  }
})
</script>

<style scoped>
.supervision-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.loading-container {
  padding: 40px;
}

.supervision-stats {
  margin-bottom: 20px;
}

.stat-card-mini {
  text-align: center;
}

.stat-card-mini :deep(.el-card__body) {
  padding: 16px;
}

.stat-mini {
  text-align: center;
}

.stat-mini-value {
  font-size: 28px;
  font-weight: bold;
  color: var(--el-text-color-primary);
  line-height: 1.2;
}

.stat-mini-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

.stat-danger .stat-mini-value {
  color: var(--el-color-danger);
}

.stat-success .stat-mini-value {
  color: var(--el-color-success);
}

.content-card {
  min-height: 400px;
}

/* 甘特图 */
.gantt-milestone {
  margin-bottom: 20px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 16px;
}

.gantt-milestone-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.gantt-milestone-title {
  font-weight: 600;
  flex: 1;
}

.gantt-milestone-role {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.gantt-task-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 0;
  border-bottom: 1px solid var(--el-border-color-extra-light);
}

.gantt-task-label {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 180px;
  font-size: 13px;
}

.gantt-task-bar-area {
  flex: 1;
  height: 20px;
  background: var(--el-fill-color-lighter);
  border-radius: 4px;
  position: relative;
  overflow: hidden;
}

.gantt-task-bar {
  height: 100%;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 30px;
  transition: width 0.3s;
}

.gantt-bar-text {
  font-size: 11px;
  color: white;
  font-weight: 500;
}

.gantt-task-status {
  min-width: 70px;
  text-align: right;
}

.text-done {
  text-decoration: line-through;
  color: var(--el-text-color-placeholder);
}

/* Agent效率 */
.efficiency-card {
  margin-bottom: 16px;
}

.efficiency-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.efficiency-role {
  font-weight: 600;
  font-size: 15px;
}

/* 指标卡片 */
.metric-card {
  text-align: center;
}

.metric-title {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
}

.metric-value {
  font-size: 24px;
  font-weight: bold;
  color: var(--el-text-color-primary);
}

.metric-hint {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  margin-top: 8px;
}

/* 趋势图 */
.trend-chart-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
  color: var(--el-text-color-primary);
}

.trend-bars {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  height: 200px;
  padding: 0 8px;
}

.trend-bar-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
}

.trend-bar-wrapper {
  flex: 1;
  width: 100%;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.trend-bar {
  width: 80%;
  max-width: 60px;
  border-radius: 4px 4px 0 0;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 4px;
  min-height: 20px;
  transition: height 0.3s;
}

.trend-bar-value {
  font-size: 11px;
  color: white;
  font-weight: 600;
}

.trend-bar-label {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
  text-align: center;
}

/* 玩家体验 */
.fun-score-header {
  display: flex;
  align-items: center;
  gap: 20px;
}

.fun-score-circle {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.fun-score-good { background: linear-gradient(135deg, #67c23a, #95d475); }
.fun-score-medium { background: linear-gradient(135deg, #e6a23c, #eebe77); }
.fun-score-low { background: linear-gradient(135deg, #f56c6c, #fab6b6); }

.fun-score-value {
  font-size: 28px;
  font-weight: bold;
  color: white;
}

.fun-score-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
}

.fun-score-status {
  font-size: 16px;
  color: var(--el-text-color-primary);
}

.dimension-score {
  text-align: center;
  margin-bottom: 16px;
}

.dimension-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 8px;
}

/* 风险卡片 */
.risk-card {
  margin-bottom: 12px;
}

.risk-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.risk-type {
  font-weight: 600;
}

.risk-description {
  color: var(--el-text-color-regular);
  margin-bottom: 8px;
  line-height: 1.6;
}

.risk-suggestion {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  color: var(--el-color-primary);
  font-size: 13px;
}

/* 质量基准 */
.benchmark-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.benchmark-title {
  font-size: 15px;
  font-weight: 600;
}

.checklist-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid var(--el-border-color-extra-light);
}

/* 迭代回顾 */
.retro-stats {
  margin-bottom: 24px;
}

.overdue-badge {
  margin-left: 4px;
}

/* 响应式 */
@media (max-width: 767px) {
  .supervision-page {
    padding: 12px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .gantt-task-row {
    flex-wrap: wrap;
  }

  .gantt-task-label {
    min-width: auto;
  }
}
</style>
