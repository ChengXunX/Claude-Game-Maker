<template>
  <div class="game-verify-page">
    <!-- 统计概览 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-success-light-9)">
            <el-icon :size="24" color="var(--el-color-success)"><CircleCheck /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value success">{{ stats.passedCount }}</div>
            <div class="stat-label">验证通过</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-danger-light-9)">
            <el-icon :size="24" color="var(--el-color-danger)"><CircleClose /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value danger">{{ stats.failedCount }}</div>
            <div class="stat-label">验证失败</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-warning-light-9)">
            <el-icon :size="24" color="var(--el-color-warning)"><WarningFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value warning">{{ stats.warningCount }}</div>
            <div class="stat-label">有警告</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-info-light-9)">
            <el-icon :size="24" color="var(--el-color-info)"><QuestionFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value info">{{ stats.notVerifiedCount }}</div>
            <div class="stat-label">未验证</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 项目列表 -->
    <el-card>
      <template #header>
        <div class="card-header">
          <span>游戏项目运行时验证</span>
          <div class="header-actions">
            <el-button :icon="Refresh" @click="loadProjects" :loading="loading">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table :data="projects" v-loading="loading" stripe>
        <el-table-column prop="name" label="项目名称" min-width="150" />
        <el-table-column prop="goal" label="项目目标" min-width="200" show-overflow-tooltip />
        <el-table-column label="项目类型" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="getProjectTypeTag(row.goal)">
              {{ getProjectTypeLabel(row.goal) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="验证状态" width="120">
          <template #default="{ row }">
            <template v-if="verifyResults[row.id]">
              <el-tag v-if="verifyResults[row.id].success" type="success" size="small">
                通过
              </el-tag>
              <el-tag v-else type="danger" size="small">
                失败
              </el-tag>
            </template>
            <el-tag v-else type="info" size="small">未验证</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="验证详情" min-width="250">
          <template #default="{ row }">
            <template v-if="verifyResults[row.id]">
              <div v-if="verifyResults[row.id].success" class="verify-detail success">
                <el-icon><CircleCheck /></el-icon>
                {{ verifyResults[row.id].message }}
              </div>
              <div v-else class="verify-detail error">
                <el-icon><CircleClose /></el-icon>
                {{ verifyResults[row.id].error }}
              </div>
              <!-- 警告列表 -->
              <div v-if="verifyResults[row.id].warnings && verifyResults[row.id].warnings.length > 0"
                   class="verify-warnings">
                <div v-for="(warn, idx) in verifyResults[row.id].warnings" :key="idx" class="warning-item">
                  <el-icon><WarningFilled /></el-icon> {{ warn }}
                </div>
              </div>
            </template>
            <span v-else class="text-muted">点击验证按钮执行运行时检查</span>
          </template>
        </el-table-column>
        <el-table-column label="深度分析" width="150">
          <template #default="{ row }">
            <template v-if="analysisResults[row.id]">
              <el-tag
                :type="getScoreTagType(analysisResults[row.id].overallScore)"
                size="small"
                class="clickable-tag"
                @click="showAnalysisResult(row.id)"
              >
                {{ analysisResults[row.id].overallScore }}分
              </el-tag>
            </template>
            <template v-else-if="runningTasks[row.id]">
              <el-tag type="warning" size="small">分析中...</el-tag>
            </template>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="设计审查" width="120">
          <template #default="{ row }">
            <template v-if="designReviewResults[row.id]">
              <el-tag
                :type="designReviewResults[row.id].passed ? 'success' : 'danger'"
                size="small"
                class="clickable-tag"
                @click="showDesignReviewResult(row.id)"
              >
                {{ designReviewResults[row.id].score }}分
              </el-tag>
            </template>
            <template v-else-if="designReviewing[row.id]">
              <el-tag type="warning" size="small">审查中...</el-tag>
            </template>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="运行状态" width="140">
          <template #default="{ row }">
            <div v-if="row.running" class="running-status">
              <el-tag type="success" size="small">运行中</el-tag>
              <el-button v-if="getPreviewUrl(row)" type="primary" link size="small" @click="openProjectPreview(row)">
                预览
              </el-button>
            </div>
            <el-tag v-else type="info" size="small">未运行</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="验证时间" width="170">
          <template #default="{ row }">
            <span v-if="verifyResults[row.id]">
              {{ formatTime(verifyResults[row.id].timestamp) }}
            </span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              :loading="verifying[row.id]"
              @click="handleVerify(row)"
            >
              {{ verifying[row.id] ? '验证中...' : '验证' }}
            </el-button>
            <el-button
              type="success"
              size="small"
              :loading="analyzing[row.id]"
              :disabled="isAnalyzing(row.id)"
              @click="handleAnalyze(row)"
            >
              {{ getAnalyzeButtonText(row.id) }}
            </el-button>
            <el-button
              type="warning"
              size="small"
              :loading="designReviewing[row.id]"
              @click="handleDesignReview(row)"
            >
              设计审查
            </el-button>
            <el-button
              v-if="analysisResults[row.id]"
              type="info"
              size="small"
              @click="showAnalysisHistory(row.id)"
            >
              历史
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 验证结果详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="验证详情" width="600px">
      <template v-if="selectedResult">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="项目名称">{{ selectedResult.projectName }}</el-descriptions-item>
          <el-descriptions-item label="验证状态">
            <el-tag :type="selectedResult.success ? 'success' : 'danger'">
              {{ selectedResult.success ? '通过' : '失败' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="验证信息">
            {{ selectedResult.message || selectedResult.error }}
          </el-descriptions-item>
          <el-descriptions-item label="验证时间">
            {{ formatTime(selectedResult.timestamp) }}
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="selectedResult.warnings && selectedResult.warnings.length > 0" style="margin-top: 16px;">
          <h4>警告信息</h4>
          <el-alert
            v-for="(warn, idx) in selectedResult.warnings"
            :key="idx"
            :title="warn"
            type="warning"
            :closable="false"
            show-icon
            style="margin-bottom: 8px;"
          />
        </div>
      </template>
    </el-dialog>

    <!-- AI 深度分析结果弹窗 -->
    <el-dialog
      v-model="analysisDialogVisible"
      title="AI 深度分析"
      width="800px"
      top="5vh"
      @close="handleAnalysisDialogClose"
    >
      <template v-if="analysisResult">
        <!-- 评分概览 -->
        <div class="analysis-overview">
          <div class="overall-score">
            <div class="score-circle" :class="getScoreClass(analysisResult.overallScore)">
              {{ analysisResult.overallScore }}
            </div>
            <div class="score-label">综合评分</div>
          </div>
          <div class="summary-text">{{ analysisResult.summary }}</div>
        </div>

        <!-- 分项评分 -->
        <el-divider>分项评分</el-divider>
        <div class="score-items">
          <div class="score-item">
            <span class="score-item-label">可运行性</span>
            <el-progress :percentage="analysisResult.runnableScore" :status="getProgressStatus(analysisResult.runnableScore)" />
            <span class="score-item-value">{{ analysisResult.runnableScore }}/100</span>
          </div>
          <div class="score-item">
            <span class="score-item-label">可玩性</span>
            <el-progress :percentage="analysisResult.playableScore" :status="getProgressStatus(analysisResult.playableScore)" />
            <span class="score-item-value">{{ analysisResult.playableScore }}/100</span>
          </div>
          <div class="score-item">
            <span class="score-item-label">玩法完整性</span>
            <el-progress :percentage="analysisResult.completenessScore" :status="getProgressStatus(analysisResult.completenessScore)" />
            <span class="score-item-value">{{ analysisResult.completenessScore }}/100</span>
          </div>
          <div class="score-item">
            <span class="score-item-label">UI/UX 质量</span>
            <el-progress :percentage="analysisResult.uiuxScore" :status="getProgressStatus(analysisResult.uiuxScore)" />
            <span class="score-item-value">{{ analysisResult.uiuxScore }}/100</span>
          </div>
          <div class="score-item">
            <span class="score-item-label">代码质量</span>
            <el-progress :percentage="analysisResult.codeQualityScore" :status="getProgressStatus(analysisResult.codeQualityScore)" />
            <span class="score-item-value">{{ analysisResult.codeQualityScore }}/100</span>
          </div>
        </div>

        <!-- 详细分析（Markdown 渲染） -->
        <el-divider>详细分析报告</el-divider>
        <div class="analysis-report">
          <MarkdownRenderer :content="formatAnalysisReport(analysisResult)" />
        </div>
      </template>
      <template v-else-if="analysisLoading">
        <div class="analysis-loading">
          <el-icon :size="40" class="is-loading"><Loading /></el-icon>
          <p>AI 正在后台深度分析游戏质量...</p>
          <p class="loading-tip">分析正在后台进行，您可以关闭此页面，分析完成后会收到通知</p>
          <div v-if="currentTaskId" class="task-info">
            <el-tag type="info" size="small">任务ID: {{ currentTaskId }}</el-tag>
            <el-tag v-if="currentTaskStatus" size="small" style="margin-left: 8px">
              状态: {{ currentTaskStatus }}
            </el-tag>
          </div>
        </div>
      </template>
      <template v-else>
        <div class="analysis-loading">
          <p>等待分析任务提交...</p>
        </div>
      </template>
    </el-dialog>

    <!-- 分析历史记录弹窗 -->
    <el-dialog
      v-model="historyDialogVisible"
      title="深度分析历史"
      width="800px"
      top="5vh"
    >
      <el-table :data="analysisHistory" stripe>
        <el-table-column label="评分" width="100">
          <template #default="{ row }">
            <el-tag :type="getScoreTagType(row.overallScore)" size="small">
              {{ row.overallScore }}分
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="summary" label="摘要" min-width="300" show-overflow-tooltip />
        <el-table-column label="分析时间" width="170">
          <template #default="{ row }">
            {{ formatTime(row.completedAt || row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="viewHistoryDetail(row)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 设计审查结果弹窗 -->
    <el-dialog
      v-model="designReviewDialogVisible"
      title="游戏设计审查报告"
      width="800px"
      top="5vh"
    >
      <template v-if="designReviewResult">
        <!-- 审查概览 -->
        <div class="analysis-overview">
          <div class="overall-score">
            <div class="score-circle" :class="getScoreClass(designReviewResult.score)">
              {{ designReviewResult.score }}
            </div>
            <div class="score-label">设计评分</div>
          </div>
          <div class="summary-text">
            <el-tag :type="designReviewResult.passed ? 'success' : 'danger'" size="large" style="margin-bottom: 8px;">
              {{ designReviewResult.passed ? '审查通过' : '需改进' }}
            </el-tag>
            <p>{{ designReviewResult.summary }}</p>
          </div>
        </div>

        <!-- 设计亮点 -->
        <div v-if="designReviewResult.strengths && designReviewResult.strengths.length > 0" style="margin-top: 16px;">
          <el-divider>设计亮点</el-divider>
          <ul class="analysis-list strengths">
            <li v-for="(s, idx) in designReviewResult.strengths" :key="idx">{{ s }}</li>
          </ul>
        </div>

        <!-- 发现的问题 -->
        <div v-if="designReviewResult.issues && designReviewResult.issues.length > 0" style="margin-top: 16px;">
          <el-divider>发现的问题</el-divider>
          <div v-for="(issue, idx) in designReviewResult.issues" :key="idx" class="design-issue">
            <el-tag
              :type="issue.severity === 'HIGH' ? 'danger' : issue.severity === 'MEDIUM' ? 'warning' : 'info'"
              size="small"
              style="margin-right: 8px;"
            >
              {{ issue.severity }}
            </el-tag>
            <span class="issue-desc">{{ issue.description }}</span>
            <div v-if="issue.suggestion" class="issue-suggestion">
              <el-icon><Promotion /></el-icon> 建议: {{ issue.suggestion }}
            </div>
          </div>
        </div>

        <!-- 完整报告 -->
        <div v-if="designReviewResult.report" style="margin-top: 16px;">
          <el-divider>完整报告</el-divider>
          <div class="analysis-report">
            <MarkdownRenderer :content="designReviewResult.report" />
          </div>
        </div>
      </template>
      <template v-else-if="designReviewLoading">
        <div class="analysis-loading">
          <el-icon :size="40" class="is-loading"><Loading /></el-icon>
          <p>AI 正在审查游戏设计...</p>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, CircleCheck, CircleClose, WarningFilled, QuestionFilled, Loading, Promotion } from '@element-plus/icons-vue'
import { gameVerifyApi, projectApi } from '@/api'

import MarkdownRenderer from '@/components/MarkdownRenderer.vue'

const loading = ref(false)
const projects = ref([])
const verifyResults = reactive({})
const verifying = reactive({})
const analyzing = reactive({})
const detailDialogVisible = ref(false)
const selectedResult = ref(null)

// AI 深度分析
const analysisDialogVisible = ref(false)
const analysisResult = ref(null)
const analysisLoading = ref(false)

// 记录正在进行的分析任务 { projectId: taskId }
const runningTasks = reactive({})

// 记录每个项目的最新分析结果 { projectId: result }
const analysisResults = reactive({})

// 分析历史
const historyDialogVisible = ref(false)
const analysisHistory = ref([])

// 设计审查
const designReviewDialogVisible = ref(false)
const designReviewResult = ref(null)
const designReviewLoading = ref(false)
const designReviewing = reactive({})
const designReviewResults = reactive({})

// 检查项目是否正在分析中
const isAnalyzing = (projectId) => {
  return !!runningTasks[projectId] || analyzing[projectId]
}

// 获取分析按钮文本
const getAnalyzeButtonText = (projectId) => {
  if (analyzing[projectId]) return '提交中...'
  if (runningTasks[projectId]) return '分析中...'
  return '深度分析'
}

// 统计数据
const stats = computed(() => {
  const results = Object.values(verifyResults)
  return {
    passedCount: results.filter(r => r.success).length,
    failedCount: results.filter(r => !r.success).length,
    warningCount: results.filter(r => r.warnings && r.warnings.length > 0).length,
    notVerifiedCount: projects.value.length - results.length
  }
})

// 加载项目列表
const loadProjects = async () => {
  loading.value = true
  try {
    const res = await projectApi.getAll()
    projects.value = res.data || res || []

    // 批量获取验证状态
    if (projects.value.length > 0) {
      const ids = projects.value.map(p => p.id)
      try {
        const statusRes = await gameVerifyApi.batchStatus(ids)
        const statusData = statusRes.data || statusRes || {}
        Object.keys(statusData).forEach(projectId => {
          if (statusData[projectId].hasResult) {
            verifyResults[projectId] = statusData[projectId]
          }
        })
      } catch (e) {
        // 批量查询失败不影响页面展示
      }

      // 检查每个项目是否有正在进行的分析任务，并加载最新分析结果
      for (const project of projects.value) {
        try {
          const taskRes = await gameVerifyApi.getAnalysisStatus(project.id)
          const taskData = taskRes.data || taskRes
          if (taskData.hasTask) {
            if (taskData.status === 'PENDING' || taskData.status === 'RUNNING') {
              runningTasks[project.id] = taskData.taskId
              // 启动轮询
              startTaskPolling(project.id, taskData.taskId)
            } else if (taskData.status === 'COMPLETED' && taskData.result) {
              // 保存最新的分析结果
              analysisResults[project.id] = taskData.result
            }
          }
        } catch (e) {
          // 查询失败不影响页面
        }
      }
    }
  } catch (e) {
    ElMessage.error('加载项目列表失败: ' + (e.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// 执行验证
const handleVerify = async (project) => {
  verifying[project.id] = true
  try {
    const res = await gameVerifyApi.verify(project.id)
    const data = res.data || res

    verifyResults[project.id] = {
      success: data.success,
      message: data.message,
      error: data.error,
      warnings: data.warnings || [],
      timestamp: data.timestamp || Date.now()
    }

    if (data.success) {
      ElMessage.success(`[${project.name}] 验证通过`)
    } else {
      ElMessage.warning(`[${project.name}] 验证失败: ${data.error}`)
    }
  } catch (e) {
    ElMessage.error(`验证请求失败: ${e.message || '未知错误'}`)
  } finally {
    verifying[project.id] = false
  }
}

// 当前分析任务信息
const currentTaskId = ref(null)
const currentTaskStatus = ref(null)
const currentProjectId = ref(null)
const pollingTimers = reactive({})

// 执行深度分析（后台异步任务）
const handleAnalyze = async (project) => {
  // 检查是否已有任务在进行中
  if (runningTasks[project.id]) {
    ElMessage.warning('该项目正在分析中，请等待完成')
    // 打开弹窗查看进度
    currentTaskId.value = runningTasks[project.id]
    currentProjectId.value = project.id
    analysisResult.value = null
    analysisLoading.value = true
    analysisDialogVisible.value = true
    startTaskPolling(project.id, runningTasks[project.id])
    return
  }

  analyzing[project.id] = true
  analysisResult.value = null
  analysisLoading.value = true
  analysisDialogVisible.value = true
  currentTaskId.value = null
  currentTaskStatus.value = null
  currentProjectId.value = project.id

  try {
    const data = await gameVerifyApi.analyze(project.id)

    if (data.success) {
      if (data.taskId) {
        // 后台任务已提交
        currentTaskId.value = data.taskId
        currentTaskStatus.value = data.status
        runningTasks[project.id] = data.taskId
        ElMessage.success('分析任务已提交，正在后台执行...')

        // 开始轮询任务状态
        startTaskPolling(project.id, data.taskId)
      } else if (data.message && data.message.includes('已在进行中')) {
        // 已有任务在进行中
        currentTaskId.value = data.taskId
        runningTasks[project.id] = data.taskId
        startTaskPolling(project.id, data.taskId)
      }
    } else {
      ElMessage.warning(`分析失败: ${data.error}`)
      analysisDialogVisible.value = false
    }
  } catch (e) {
    ElMessage.error(`分析请求失败: ${e.message || '未知错误'}`)
    analysisDialogVisible.value = false
  } finally {
    analyzing[project.id] = false
  }
}

// 开始轮询任务状态
const startTaskPolling = (projectId, taskId) => {
  // 清除之前的轮询
  if (pollingTimers[projectId]) {
    clearInterval(pollingTimers[projectId])
  }

  // 立即查询一次
  checkTaskStatus(projectId, taskId)

  // 每 3 秒轮询一次
  pollingTimers[projectId] = setInterval(async () => {
    await checkTaskStatus(projectId, taskId)
  }, 3000)
}

// 检查任务状态
const checkTaskStatus = async (projectId, taskId) => {
  try {
    const data = await gameVerifyApi.getTaskStatus(taskId)

    if (data.success) {
      currentTaskStatus.value = data.status

      if (data.status === 'COMPLETED') {
        // 分析完成 - 保存结果并自动打开弹窗显示
        analysisResult.value = data.result
        analysisResults[projectId] = data.result  // 保存到项目分析结果缓存
        analysisLoading.value = false
        analysisDialogVisible.value = true  // 确保弹窗打开
        currentProjectId.value = projectId
        stopTaskPolling(projectId)
        delete runningTasks[projectId]
        ElMessage.success('分析完成！')
      } else if (data.status === 'FAILED') {
        // 分析失败
        analysisLoading.value = false
        analysisDialogVisible.value = true  // 打开弹窗显示错误
        stopTaskPolling(projectId)
        delete runningTasks[projectId]
        ElMessage.error(`分析失败: ${data.errorMessage || '未知错误'}`)
      } else {
        // 仍在进行中，更新进度
        analysisLoading.value = true
      }
    }
  } catch (e) {
    console.error('查询任务状态失败:', e)
  }
}

// 停止轮询
const stopTaskPolling = (projectId) => {
  if (pollingTimers[projectId]) {
    clearInterval(pollingTimers[projectId])
    delete pollingTimers[projectId]
  }
}

// 停止所有轮询
const stopAllPolling = () => {
  Object.keys(pollingTimers).forEach(projectId => {
    clearInterval(pollingTimers[projectId])
  })
  Object.keys(pollingTimers).forEach(key => delete pollingTimers[key])
}

// 关闭分析弹窗时停止轮询
const handleAnalysisDialogClose = () => {
  if (currentProjectId.value) {
    stopTaskPolling(currentProjectId.value)
  }
  analysisDialogVisible.value = false
}

// 格式化分析报告为 Markdown
const formatAnalysisReport = (result) => {
  if (!result) return ''

  let md = '## 分析报告\n\n'

  // 优点
  if (result.strengths && result.strengths.length > 0) {
    md += '### ✅ 优点\n\n'
    result.strengths.forEach(s => {
      md += `- ${s}\n`
    })
    md += '\n'
  }

  // 问题
  if (result.issues && result.issues.length > 0) {
    md += '### ❌ 问题\n\n'
    result.issues.forEach(issue => {
      md += `- ${issue}\n`
    })
    md += '\n'
  }

  // 改进建议
  if (result.suggestions && result.suggestions.length > 0) {
    md += '### 💡 改进建议\n\n'
    result.suggestions.forEach(sug => {
      md += `- ${sug}\n`
    })
    md += '\n'
  }

  return md
}

// 评分等级样式
const getScoreClass = (score) => {
  if (score >= 80) return 'score-excellent'
  if (score >= 60) return 'score-good'
  if (score >= 40) return 'score-fair'
  return 'score-poor'
}

// 评分标签类型
const getScoreTagType = (score) => {
  if (score >= 80) return 'success'
  if (score >= 60) return 'warning'
  return 'danger'
}

// 显示分析结果（从缓存中）
const showAnalysisResult = (projectId) => {
  if (analysisResults[projectId]) {
    analysisResult.value = analysisResults[projectId]
    analysisDialogVisible.value = true
  }
}

// 显示分析历史
const showAnalysisHistory = async (projectId) => {
  try {
    const data = await gameVerifyApi.getAnalysisHistory(projectId)
    if (data.success && data.tasks) {
      analysisHistory.value = data.tasks
      historyDialogVisible.value = true
    }
  } catch (e) {
    ElMessage.error('获取分析历史失败')
  }
}

// 查看历史详情
const viewHistoryDetail = (task) => {
  if (task.result) {
    analysisResult.value = task.result
    historyDialogVisible.value = false
    analysisDialogVisible.value = true
  }
}

// 执行设计审查
const handleDesignReview = async (project) => {
  designReviewing[project.id] = true
  designReviewResult.value = null
  designReviewLoading.value = true
  designReviewDialogVisible.value = true

  try {
    const data = await gameVerifyApi.designReview(project.id)

    if (data.success && data.reviewed) {
      designReviewResult.value = data
      designReviewResults[project.id] = data
      ElMessage.success(`[${project.name}] 设计审查完成`)
    } else {
      ElMessage.warning(data.error || '设计审查未返回结果')
      designReviewDialogVisible.value = false
    }
  } catch (e) {
    ElMessage.error(`设计审查失败: ${e.message || '未知错误'}`)
    designReviewDialogVisible.value = false
  } finally {
    designReviewing[project.id] = false
    designReviewLoading.value = false
  }
}

// 显示设计审查结果（从缓存中）
const showDesignReviewResult = (projectId) => {
  if (designReviewResults[projectId]) {
    designReviewResult.value = designReviewResults[projectId]
    designReviewDialogVisible.value = true
  }
}

// 进度条状态
const getProgressStatus = (score) => {
  if (score >= 80) return 'success'
  if (score >= 60) return ''
  if (score >= 40) return 'warning'
  return 'exception'
}

// 格式化时间
const formatTime = (timestamp) => {
  if (!timestamp) return '-'
  return new Date(timestamp).toLocaleString('zh-CN')
}

// 项目类型判断
const getProjectTypeLabel = (goal) => {
  if (!goal) return '未知'
  const lower = goal.toLowerCase()
  if (lower.includes('三消') || lower.includes('match') || lower.includes('消除')) return '三消游戏'
  if (lower.includes('rpg') || lower.includes('角色')) return 'RPG'
  if (lower.includes('塔防') || lower.includes('tower')) return '塔防'
  if (lower.includes('射击') || lower.includes('shooter')) return '射击'
  if (lower.includes('赛车') || lower.includes('racing')) return '赛车'
  if (lower.includes('策略') || lower.includes('strategy')) return '策略'
  return '自定义'
}

const getProjectTypeTag = (goal) => {
  if (!goal) return 'info'
  const lower = goal.toLowerCase()
  if (lower.includes('三消') || lower.includes('match')) return 'success'
  if (lower.includes('rpg')) return 'warning'
  if (lower.includes('塔防')) return 'danger'
  return 'info'
}

// 从项目概况中提取预览 URL
const getPreviewUrl = (project) => {
  const overview = project.projectOverview
  if (!overview) return null
  const match = overview.match(/预览地址[:：]\s*(https?:\/\/[^\s\n]+)/)
  return match ? match[1] : null
}

const openProjectPreview = (project) => {
  const url = getPreviewUrl(project)
  if (url) window.open(url, '_blank')
}

onMounted(() => {
  loadProjects()
})

onUnmounted(() => {
  // 清理所有轮询定时器
  stopAllPolling()
})
</script>

<style scoped>
.game-verify-page {
  padding: 20px;
}

/* 统计卡片 */
.stat-cards {
  margin-bottom: 16px;
}

.stat-card {
  cursor: default;
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
  min-width: 0;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: var(--el-text-color-primary);
  line-height: 1.2;
  white-space: nowrap;
}

.stat-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
  white-space: nowrap;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.verify-detail {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.verify-detail.success {
  color: var(--el-color-success);
}

.verify-detail.error {
  color: var(--el-color-danger);
}

.verify-warnings {
  margin-top: 6px;
}

.warning-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
}
.running-status {
  display: flex;
  align-items: center;
  gap: 6px;
}
.warning-text {
  color: var(--el-color-warning);
  margin-bottom: 2px;
}

.text-muted {
  color: var(--el-text-color-placeholder);
  font-size: 13px;
}

/* AI 深度分析 */
.analysis-overview {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
}

.overall-score {
  text-align: center;
  flex-shrink: 0;
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

.score-excellent { background: var(--el-color-success); }
.score-good { background: var(--el-color-primary); }
.score-fair { background: var(--el-color-warning); }
.score-poor { background: var(--el-color-danger); }

.score-label {
  margin-top: 8px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.summary-text {
  flex: 1;
  font-size: 14px;
  line-height: 1.6;
  color: var(--el-text-color-regular);
}

.score-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.score-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.score-item-label {
  width: 90px;
  font-size: 13px;
  color: var(--el-text-color-regular);
  flex-shrink: 0;
}

.score-item :deep(.el-progress) {
  flex: 1;
}

.score-item-value {
  width: 60px;
  text-align: right;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  flex-shrink: 0;
}

.analysis-list {
  margin: 0;
  padding-left: 20px;
  font-size: 13px;
  line-height: 1.8;
}

.analysis-list li {
  margin-bottom: 4px;
}

.analysis-list.strengths li::marker {
  color: var(--el-color-success);
}

.analysis-list.issues li::marker {
  color: var(--el-color-danger);
}

.analysis-list.suggestions li::marker {
  color: var(--el-color-primary);
}

.analysis-loading {
  text-align: center;
  padding: 40px 0;
  color: var(--el-text-color-secondary);
}

.analysis-loading p {
  margin-top: 16px;
  font-size: 14px;
}

.loading-tip {
  font-size: 12px !important;
  color: var(--el-text-color-placeholder) !important;
}

.task-info {
  margin-top: 16px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.analysis-report {
  max-height: 400px;
  overflow-y: auto;
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
}

.clickable-tag {
  cursor: pointer;
}

.clickable-tag:hover {
  opacity: 0.8;
}

.design-issue {
  padding: 12px;
  margin-bottom: 8px;
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
  border-left: 3px solid var(--el-color-warning);
}

.design-issue .issue-desc {
  font-size: 14px;
  color: var(--el-text-color-primary);
}

.design-issue .issue-suggestion {
  margin-top: 8px;
  font-size: 13px;
  color: var(--el-color-primary);
  display: flex;
  align-items: flex-start;
  gap: 4px;
}
</style>
