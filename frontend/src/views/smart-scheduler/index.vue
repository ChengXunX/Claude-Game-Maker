<template>
  <div class="smart-scheduler-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h2>智能调度中心</h2>
        <span class="subtitle">企业级多Agent协作调度与质量管理</span>
      </div>
      <div class="header-right">
        <el-button @click="loadAllData" :loading="loading">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #409eff">
            <el-icon :size="24"><Cpu /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ scheduleStats.totalScheduled || 0 }}</div>
            <div class="stat-label">总调度次数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #67c23a">
            <el-icon :size="24"><CircleCheck /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ formatPercent(scheduleStats.successRate) }}%</div>
            <div class="stat-label">调度成功率</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #e6a23c">
            <el-icon :size="24"><Connection /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ collaborationStats.totalSessions || 0 }}</div>
            <div class="stat-label">协作会话数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #f56c6c">
            <el-icon :size="24"><DataAnalysis /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ qualityGates.length }}</div>
            <div class="stat-label">质量门禁</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 主要内容区 -->
    <el-tabs v-model="activeTab" type="border-card">
      <!-- Agent 负载监控 -->
      <el-tab-pane label="Agent 负载" name="load">
        <div class="section-header">
          <h3>Agent 负载监控</h3>
          <el-tag :type="getOverallLoadType()">整体负载: {{ getOverallLoadStatus() }}</el-tag>
        </div>

        <el-table :data="agentLoadList" stripe>
          <el-table-column prop="agentId" label="Agent ID" min-width="200" show-overflow-tooltip />
          <el-table-column label="负载评分" width="150">
            <template #default="{ row }">
              <el-progress
                :percentage="row.loadScore"
                :status="getLoadProgressStatus(row.loadScore)"
                :stroke-width="10"
              />
            </template>
          </el-table-column>
          <el-table-column prop="activeTasks" label="活跃任务" width="100" align="center" />
          <el-table-column prop="completedTasks" label="已完成" width="100" align="center" />
          <el-table-column prop="failedTasks" label="失败" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.failedTasks > 0 ? 'danger' : 'success'" size="small">
                {{ row.failedTasks }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getAgentStatusType(row)" size="small">
                {{ getAgentStatusText(row) }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 协作会话 -->
      <el-tab-pane label="协作会话" name="collaboration">
        <div class="section-header">
          <h3>协作会话管理</h3>
          <el-tag type="info">完成率: {{ formatPercent(collaborationStats.successRate) }}%</el-tag>
        </div>

        <el-table :data="collaborationSessions" stripe>
          <el-table-column prop="sessionId" label="会话ID" min-width="200" show-overflow-tooltip />
          <el-table-column prop="projectId" label="项目" width="150" />
          <el-table-column label="参与者" width="200">
            <template #default="{ row }">
              <el-tag v-for="agent in row.participantAgentIds?.slice(0, 3)" :key="agent" size="small" class="mr-1">
                {{ getAgentName(agent) }}
              </el-tag>
              <el-tag v-if="row.participantAgentIds?.length > 3" size="small" type="info">
                +{{ row.participantAgentIds.length - 3 }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="getCollaborationStatusType(row.status)" size="small">
                {{ getCollaborationStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="步骤" width="100" align="center">
            <template #default="{ row }">
              {{ getCompletedSteps(row) }}/{{ row.steps?.length || 0 }}
            </template>
          </el-table-column>
          <el-table-column label="开始时间" width="170">
            <template #default="{ row }">
              {{ formatTime(row.startTime) }}
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 质量门禁 -->
      <el-tab-pane label="质量门禁" name="quality">
        <div class="section-header">
          <h3>质量门禁配置</h3>
          <el-button type="primary" @click="showAssessDialog">
            <el-icon><DataAnalysis /></el-icon> 执行质量评估
          </el-button>
        </div>

        <el-table :data="qualityGates" stripe>
          <el-table-column prop="gateId" label="门禁ID" width="120" />
          <el-table-column prop="name" label="名称" width="120" />
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
          <el-table-column label="级别" width="80" align="center">
            <template #default="{ row }">
              <el-tag type="info" size="small">L{{ row.level }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="最低分" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.minScore >= 70 ? 'success' : 'warning'" size="small">
                {{ row.minScore }}分
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="阻塞" width="80" align="center">
            <template #default="{ row }">
              <el-icon v-if="row.blocking" style="color: #f56c6c"><Warning /></el-icon>
              <el-icon v-else style="color: #67c23a"><CircleCheck /></el-icon>
            </template>
          </el-table-column>
          <el-table-column label="检查项" min-width="200">
            <template #default="{ row }">
              <el-tag v-for="item in row.checkItems?.slice(0, 3)" :key="item" size="small" class="mr-1">
                {{ item }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 质量评估结果 -->
      <el-tab-pane label="评估结果" name="assessment" v-if="assessmentResult">
        <div class="section-header">
          <h3>质量评估结果</h3>
          <el-tag :type="assessmentResult.passed ? 'success' : 'danger'" size="large">
            {{ assessmentResult.passed ? '通过' : '未通过' }}
          </el-tag>
        </div>

        <!-- 总分 -->
        <div class="assessment-overview">
          <div class="overall-score">
            <div class="score-circle" :class="getScoreClass(assessmentResult.overallScore)">
              {{ assessmentResult.overallScore }}
            </div>
            <div class="score-label">综合评分</div>
          </div>
          <div class="quality-level">
            <el-tag :type="getQualityLevelType(assessmentResult.qualityLevel)" size="large">
              {{ getQualityLevelText(assessmentResult.qualityLevel) }}
            </el-tag>
          </div>
        </div>

        <!-- 各级门禁结果 -->
        <el-divider>各级门禁检查结果</el-divider>
        <div class="gate-results">
          <div v-for="(result, gateId) in assessmentResult.gateResults" :key="gateId" class="gate-result-item">
            <div class="gate-header">
              <span class="gate-name">{{ result.gateName }}</span>
              <el-tag :type="result.passed ? 'success' : 'danger'" size="small">
                {{ result.passed ? '通过' : '未通过' }}
              </el-tag>
            </div>
            <el-progress
              :percentage="result.score"
              :status="result.passed ? 'success' : 'exception'"
              :stroke-width="8"
            />
            <div class="gate-summary">{{ result.summary }}</div>
          </div>
        </div>

        <!-- 阻塞项 -->
        <div v-if="assessmentResult.blockers?.length > 0" class="blockers-section">
          <el-divider>阻塞项</el-divider>
          <el-alert
            v-for="blocker in assessmentResult.blockers"
            :key="blocker"
            :title="blocker"
            type="error"
            :closable="false"
            show-icon
            class="mb-2"
          />
        </div>

        <!-- 改进建议 -->
        <div v-if="assessmentResult.recommendations?.length > 0" class="recommendations-section">
          <el-divider>改进建议</el-divider>
          <ul class="recommendations-list">
            <li v-for="(rec, idx) in assessmentResult.recommendations" :key="idx">{{ rec }}</li>
          </ul>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 质量评估对话框 -->
    <el-dialog v-model="assessDialogVisible" title="执行质量评估" width="500px">
      <el-form :model="assessForm" label-width="100px">
        <el-form-item label="项目">
          <el-select v-model="assessForm.projectId" placeholder="选择项目" style="width: 100%">
            <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assessDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="executeAssessment" :loading="assessing">
          开始评估
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Cpu, CircleCheck, Connection, DataAnalysis, Warning } from '@element-plus/icons-vue'
import { schedulerApi, projectApi } from '@/api'

const loading = ref(false)
const activeTab = ref('load')

// 调度统计
const scheduleStats = ref({})

// 协作统计
const collaborationStats = ref({})
const collaborationSessions = ref([])

// Agent 负载
const agentLoadList = ref([])

// 质量门禁
const qualityGates = ref([])

// 质量评估
const assessDialogVisible = ref(false)
const assessing = ref(false)
const assessForm = ref({ projectId: '' })
const assessmentResult = ref(null)
const projects = ref([])

// 加载所有数据
const loadAllData = async () => {
  loading.value = true
  try {
    await Promise.all([
      loadScheduleStats(),
      loadCollaborationStats(),
      loadQualityGates(),
      loadProjects()
    ])
  } catch (e) {
    console.error('加载数据失败:', e)
  } finally {
    loading.value = false
  }
}

// 加载调度统计
const loadScheduleStats = async () => {
  try {
    const data = await schedulerApi.getStats()
    scheduleStats.value = data.data || data || {}

    // 转换 Agent 负载为列表
    const loads = scheduleStats.value.agentLoads || {}
    agentLoadList.value = Object.entries(loads).map(([agentId, loadScore]) => ({
      agentId,
      loadScore: Math.round(loadScore),
      activeTasks: 0,
      completedTasks: 0,
      failedTasks: 0
    }))
  } catch (e) {
    console.error('加载调度统计失败:', e)
  }
}

// 加载协作统计
const loadCollaborationStats = async () => {
  try {
    const data = await schedulerApi.getCollaborationStats()
    collaborationStats.value = data.data || data || {}
  } catch (e) {
    console.error('加载协作统计失败:', e)
  }
}

// 加载质量门禁配置
const loadQualityGates = async () => {
  try {
    const data = await schedulerApi.getQualityGateConfigs()
    const configs = data.data || data || {}
    qualityGates.value = Object.values(configs)
  } catch (e) {
    console.error('加载质量门禁配置失败:', e)
  }
}

// 加载项目列表
const loadProjects = async () => {
  try {
    const res = await projectApi.getAll()
    projects.value = res.data || res || []
  } catch (e) {
    console.error('加载项目列表失败:', e)
  }
}

// 显示评估对话框
const showAssessDialog = () => {
  assessForm.value.projectId = projects.value[0]?.id || ''
  assessDialogVisible.value = true
}

// 执行质量评估
const executeAssessment = async () => {
  if (!assessForm.value.projectId) {
    ElMessage.warning('请选择项目')
    return
  }

  assessing.value = true
  try {
    const project = projects.value.find(p => p.id === assessForm.value.projectId)
    const data = await schedulerApi.assessQuality(assessForm.value.projectId, {
      projectDir: project?.workDir || '',
      projectName: project?.name || '',
      projectGoal: project?.goal || ''
    })

    assessmentResult.value = data.data || data
    activeTab.value = 'assessment'
    assessDialogVisible.value = false
    ElMessage.success('质量评估完成')
  } catch (e) {
    ElMessage.error('质量评估失败: ' + (e.message || '未知错误'))
  } finally {
    assessing.value = false
  }
}

// 格式化百分比
const formatPercent = (value) => {
  if (value == null) return '0'
  return Math.round(value)
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

// 获取 Agent 名称
const getAgentName = (agentId) => {
  if (!agentId) return ''
  const parts = agentId.split(':')
  return parts[parts.length - 1] || agentId
}

// 负载状态相关
const getOverallLoadType = () => {
  const avgLoad = agentLoadList.value.length > 0
    ? agentLoadList.value.reduce((sum, a) => sum + a.loadScore, 0) / agentLoadList.value.length
    : 0
  if (avgLoad < 30) return 'success'
  if (avgLoad < 70) return 'warning'
  return 'danger'
}

const getOverallLoadStatus = () => {
  const avgLoad = agentLoadList.value.length > 0
    ? agentLoadList.value.reduce((sum, a) => sum + a.loadScore, 0) / agentLoadList.value.length
    : 0
  if (avgLoad < 30) return '低'
  if (avgLoad < 70) return '中'
  return '高'
}

const getLoadProgressStatus = (score) => {
  if (score < 30) return 'success'
  if (score < 70) return ''
  return 'exception'
}

const getAgentStatusType = (agent) => {
  if (agent.loadScore > 70) return 'danger'
  if (agent.loadScore > 30) return 'warning'
  return 'success'
}

const getAgentStatusText = (agent) => {
  if (agent.loadScore > 70) return '高负载'
  if (agent.loadScore > 30) return '中等'
  return '空闲'
}

// 协作状态相关
const getCollaborationStatusType = (status) => {
  const map = {
    'INITIATED': 'info',
    'IN_PROGRESS': 'warning',
    'WAITING_INPUT': 'warning',
    'COMPLETED': 'success',
    'FAILED': 'danger',
    'CANCELLED': 'info'
  }
  return map[status] || 'info'
}

const getCollaborationStatusText = (status) => {
  const map = {
    'INITIATED': '已发起',
    'IN_PROGRESS': '进行中',
    'WAITING_INPUT': '等待输入',
    'COMPLETED': '已完成',
    'FAILED': '失败',
    'CANCELLED': '已取消'
  }
  return map[status] || status
}

const getCompletedSteps = (session) => {
  if (!session.steps) return 0
  return session.steps.filter(s => s.status === 'COMPLETED').length
}

// 质量评估相关
const getScoreClass = (score) => {
  if (score >= 90) return 'score-excellent'
  if (score >= 75) return 'score-good'
  if (score >= 60) return 'score-fair'
  return 'score-poor'
}

const getQualityLevelType = (level) => {
  const map = {
    'EXCELLENT': 'success',
    'GOOD': 'success',
    'ACCEPTABLE': 'warning',
    'POOR': 'danger',
    'CRITICAL': 'danger'
  }
  return map[level] || 'info'
}

const getQualityLevelText = (level) => {
  const map = {
    'EXCELLENT': '优秀',
    'GOOD': '良好',
    'ACCEPTABLE': '可接受',
    'POOR': '较差',
    'CRITICAL': '严重不足'
  }
  return map[level] || level
}

onMounted(() => {
  loadAllData()
})
</script>

<style scoped>
.smart-scheduler-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.header-left h2 {
  margin: 0 0 4px 0;
  font-size: 24px;
}

.subtitle {
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.stat-cards {
  margin-bottom: 20px;
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  line-height: 1;
}

.stat-label {
  color: var(--el-text-color-secondary);
  font-size: 14px;
  margin-top: 4px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header h3 {
  margin: 0;
}

.mr-1 {
  margin-right: 4px;
}

.mb-2 {
  margin-bottom: 8px;
}

/* 质量评估结果 */
.assessment-overview {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 20px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
}

.overall-score {
  text-align: center;
}

.score-circle {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: bold;
  color: white;
}

.score-excellent {
  background: linear-gradient(135deg, #67c23a, #4caf50);
}

.score-good {
  background: linear-gradient(135deg, #409eff, #2196f3);
}

.score-fair {
  background: linear-gradient(135deg, #e6a23c, #ff9800);
}

.score-poor {
  background: linear-gradient(135deg, #f56c6c, #f44336);
}

.score-label {
  margin-top: 8px;
  color: var(--el-text-color-secondary);
}

.gate-results {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.gate-result-item {
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
}

.gate-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.gate-name {
  font-weight: 500;
}

.gate-summary {
  margin-top: 8px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.blockers-section,
.recommendations-section {
  margin-top: 20px;
}

.recommendations-list {
  padding-left: 20px;
}

.recommendations-list li {
  margin-bottom: 8px;
  line-height: 1.5;
}
</style>
