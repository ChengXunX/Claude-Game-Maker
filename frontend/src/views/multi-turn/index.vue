<template>
  <div class="multi-turn-page">
    <el-row :gutter="16" class="stat-cards">
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #409eff"><el-icon :size="24"><Connection /></el-icon></div>
          <div class="stat-info"><div class="stat-value">{{ stats.total || 0 }}</div><div class="stat-label">推理总数</div></div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #67c23a"><el-icon :size="24"><CircleCheck /></el-icon></div>
          <div class="stat-info"><div class="stat-value">{{ stats.passed || 0 }}</div><div class="stat-label">通过数</div></div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #e6a23c"><el-icon :size="24"><TrendCharts /></el-icon></div>
          <div class="stat-info"><div class="stat-value">{{ stats.passRate || 0 }}%</div><div class="stat-label">通过率</div></div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #909399"><el-icon :size="24"><Timer /></el-icon></div>
          <div class="stat-info"><div class="stat-value">{{ stats.avgTurns || 0 }}</div><div class="stat-label">平均轮次</div></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 实时进度卡片 -->
    <el-card v-if="activeRecord" class="section-card" style="border-left: 3px solid #409eff">
      <template #header>
        <div class="section-header">
          <div class="header-left">
            <el-icon :size="20" color="#409EFF"><Loading v-if="activeRecord.running" /><CircleCheck v-else /></el-icon>
            <span>推理进行中</span>
            <el-tag size="small" type="info">轮次 {{ activeRecord.turnNumber }}/{{ activeRecord.maxTurns }}</el-tag>
          </div>
        </div>
      </template>
      <div class="flow-steps">
        <div v-for="(step, i) in flowSteps" :key="step.name" class="flow-step" :class="{ active: isStepActive(i), done: isStepDone(i) }">
          <div class="step-icon" :style="{ background: isStepDone(i) ? '#67c23a' : step.color }">
            <el-icon :size="20"><component :is="step.icon" /></el-icon>
          </div>
          <div class="step-info">
            <div class="step-name">{{ step.name }}</div>
            <div class="step-desc">{{ step.desc }}</div>
          </div>
          <el-icon v-if="i < flowSteps.length - 1" class="step-arrow"><ArrowRight /></el-icon>
        </div>
      </div>
      <div v-if="activeRecord.thinkResult || activeRecord.planResult" style="margin-top: 12px">
        <el-collapse>
          <el-collapse-item v-if="activeRecord.thinkResult" title="Think 分析结果" name="think">
            <MarkdownRenderer :content="activeRecord.thinkResult" />
          </el-collapse-item>
          <el-collapse-item v-if="activeRecord.planResult" title="Plan 执行计划" name="plan">
            <MarkdownRenderer :content="activeRecord.planResult" />
          </el-collapse-item>
          <el-collapse-item v-if="activeRecord.actResult" title="Act 执行结果" name="act">
            <MarkdownRenderer :content="activeRecord.actResult" />
          </el-collapse-item>
          <el-collapse-item v-if="activeRecord.verifyResult" title="Verify 验证结果" name="verify">
            <MarkdownRenderer :content="activeRecord.verifyResult" />
          </el-collapse-item>
        </el-collapse>
      </div>
    </el-card>

    <!-- 流程说明 + 触发 -->
    <el-card class="section-card">
      <template #header>
        <div class="section-header">
          <div class="header-left">
            <el-icon :size="20" color="#409EFF"><Connection /></el-icon>
            <span>多轮推理引擎</span>
            <el-tag type="info" size="small" style="margin-left: 8px">Think → Plan → Act → Verify</el-tag>
          </div>
          <div class="header-actions">
            <el-select v-model="selectedProject" placeholder="选择项目" style="width: 200px" @change="loadData">
              <el-option v-for="p in projectList" :key="p.id" :label="p.name" :value="p.id" />
            </el-select>
            <el-button type="primary" @click="showTriggerDialog = true" :disabled="!selectedProject || !!activeRecord">
              <el-icon><VideoPlay /></el-icon> 触发推理
            </el-button>
          </div>
        </div>
      </template>

      <!-- 推理历史 -->
      <div v-if="history.length > 0">
        <div class="table-title">推理历史</div>
        <el-table :data="history" stripe highlight-current-row>
          <el-table-column label="任务" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <div class="task-cell">
                <span class="task-id">{{ row.taskId || '-' }}</span>
                <span class="task-desc">{{ truncate(row.taskDescription, 60) }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="轮次" width="100" align="center">
            <template #default="{ row }">
              <el-progress :percentage="(row.turnNumber / row.maxTurns) * 100" :stroke-width="6" :show-text="false" style="width: 50px; display: inline-block; vertical-align: middle" />
              <span style="margin-left: 6px; font-size: 13px">{{ row.turnNumber }}/{{ row.maxTurns }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" size="small" effect="plain">{{ getStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="验证" width="80" align="center">
            <template #default="{ row }">
              <el-icon v-if="row.verifyPassed === true" color="#67c23a" :size="18"><CircleCheck /></el-icon>
              <el-icon v-else-if="row.verifyPassed === false" color="#f56c6c" :size="18"><CircleClose /></el-icon>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
          <el-table-column label="耗时" width="90" align="center">
            <template #default="{ row }">{{ formatDuration(row.durationMs) }}</template>
          </el-table-column>
          <el-table-column prop="createdAt" label="时间" width="160" />
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="showDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <el-empty v-else-if="!loading && selectedProject" description="暂无推理记录，点击上方按钮触发" />
    </el-card>

    <!-- 触发对话框 -->
    <el-dialog v-model="showTriggerDialog" title="触发多轮推理" width="520px" :close-on-click-modal="false">
      <el-form :model="triggerForm" label-width="80px">
        <el-form-item label="Agent">
          <el-select v-model="triggerForm.agentId" placeholder="选择 Agent" style="width: 100%" filterable allow-create>
            <el-option v-for="a in agentList" :key="a.id" :label="`${a.name} (${a.role})`" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务">
          <el-input v-model="triggerForm.taskDescription" type="textarea" :rows="5" placeholder="描述需要推理的任务，例如：&#10;实现一个背包系统，支持物品分类、堆叠、拖拽排序" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showTriggerDialog = false">取消</el-button>
        <el-button type="primary" @click="handleTrigger" :loading="triggering">
          <el-icon><VideoPlay /></el-icon> 开始推理
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog v-model="showDetailDialog" title="推理详情" width="680px">
      <div v-if="detailRecord.taskDescription" class="detail-task">
        <strong>任务：</strong>{{ detailRecord.taskDescription }}
      </div>
      <el-divider />
      <el-timeline>
        <el-timeline-item v-for="step in detailSteps" :key="step.name" :color="step.color" :hollow="!step.content">
          <div class="detail-step-title">{{ step.name }}</div>
          <MarkdownRenderer v-if="step.content" :content="step.content" />
          <div v-else class="text-muted">无</div>
        </el-timeline-item>
      </el-timeline>
      <div class="detail-footer">
        <el-tag :type="getStatusType(detailRecord.status)" size="large">{{ getStatusLabel(detailRecord.status) }}</el-tag>
        <span class="detail-meta">轮次: {{ detailRecord.turnNumber }}/{{ detailRecord.maxTurns }} | 耗时: {{ formatDuration(detailRecord.durationMs) }}</span>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { multiTurnApi, projectApi, agentApi } from '@/api'
import { ElMessage } from 'element-plus'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'

const loading = ref(false)
const triggering = ref(false)
const selectedProject = ref('')
const projectList = ref([])
const agentList = ref([])
const stats = ref({})
const history = ref([])
const showTriggerDialog = ref(false)
const showDetailDialog = ref(false)
const detailRecord = ref({})
const activeRecord = ref(null)
const triggerForm = ref({ agentId: '', taskDescription: '' })
let pollTimer = null

const flowSteps = [
  { name: 'Think', desc: '分析任务需求和约束', color: '#409eff', icon: 'ChatDotRound' },
  { name: 'Plan', desc: '制定执行计划和步骤', color: '#e6a23c', icon: 'Document' },
  { name: 'Act', desc: '执行计划中的步骤', color: '#67c23a', icon: 'VideoPlay' },
  { name: 'Verify', desc: '验证执行结果', color: '#f56c6c', icon: 'CircleCheck' }
]

const statusToStep = { THINKING: 0, PLANNING: 1, EXECUTING: 2, VERIFYING: 3, PASSED: 4, FAILED: 4, MAX_TURNS: 4 }
const isStepActive = (i) => activeRecord.value && statusToStep[activeRecord.value.status] === i
const isStepDone = (i) => activeRecord.value && statusToStep[activeRecord.value.status] > i

const detailSteps = computed(() => [
  { name: 'Think - 分析', color: '#409eff', content: detailRecord.value.thinkResult },
  { name: 'Plan - 计划', color: '#e6a23c', content: detailRecord.value.planResult },
  { name: 'Act - 执行', color: '#67c23a', content: detailRecord.value.actResult },
  { name: 'Verify - 验证', color: '#f56c6c', content: detailRecord.value.verifyResult }
])

onMounted(async () => {
  try { projectList.value = (await projectApi.getAll()) || [] } catch {}
})

onUnmounted(() => { stopPolling() })

const loadData = async () => {
  if (!selectedProject.value) return
  loading.value = true
  try {
    stats.value = (await multiTurnApi.getStats(selectedProject.value)) || {}
    history.value = (await multiTurnApi.getHistory(selectedProject.value)) || []
    const agents = (await agentApi.getAll()) || []
    const prefix = selectedProject.value + ':'
    agentList.value = agents.filter(a => a.id && a.id.includes(prefix))
  } catch {} finally { loading.value = false }
}

const handleTrigger = async () => {
  if (!triggerForm.value.agentId || !triggerForm.value.taskDescription) { ElMessage.warning('请填写完整'); return }
  triggering.value = true
  try {
    const result = await multiTurnApi.reason({
      agentId: triggerForm.value.agentId,
      projectId: selectedProject.value,
      taskDescription: triggerForm.value.taskDescription
    })
    ElMessage.success('推理已启动，后台执行中')
    showTriggerDialog.value = false
    triggerForm.value = { agentId: '', taskDescription: '' }
    // 开始轮询状态
    startPolling(result.id)
  } catch { ElMessage.error('启动推理失败') } finally { triggering.value = false }
}

const startPolling = (recordId) => {
  stopPolling()
  pollStatus(recordId)
  pollTimer = setInterval(() => pollStatus(recordId), 3000)
}

const stopPolling = () => {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
}

const pollStatus = async (recordId) => {
  try {
    const data = await multiTurnApi.getStatus(recordId)
    if (!data) return
    activeRecord.value = data
    // 推理完成
    if (!data.running && ['PASSED', 'FAILED', 'MAX_TURNS'].includes(data.status)) {
      stopPolling()
      activeRecord.value = null
      await loadData()
      ElMessage.success(`推理完成: ${getStatusLabel(data.status)}`)
    }
  } catch { stopPolling() }
}

const showDetail = (row) => { detailRecord.value = row; showDetailDialog.value = true }
const getStatusType = (s) => ({ PASSED: 'success', FAILED: 'danger', MAX_TURNS: 'warning' }[s] || 'info')
const getStatusLabel = (s) => ({ THINKING: '思考中', PLANNING: '规划中', EXECUTING: '执行中', VERIFYING: '验证中', PASSED: '通过', FAILED: '失败', MAX_TURNS: '达上限' }[s] || s)
const formatDuration = (ms) => ms != null ? (ms > 60000 ? `${(ms / 60000).toFixed(1)}m` : `${(ms / 1000).toFixed(1)}s`) : '-'
const truncate = (s, n) => s && s.length > n ? s.slice(0, n) + '...' : s || ''
</script>

<style scoped>
.stat-cards { margin-bottom: 16px; }
.stat-card .el-card__body { display: flex; align-items: center; width: 100%; }
.stat-icon { width: 48px; height: 48px; border-radius: 8px; display: flex; align-items: center; justify-content: center; margin-right: 12px; }
.stat-info { flex: 1; }
.stat-value { font-size: 24px; font-weight: bold; }
.stat-label { font-size: 12px; color: #909399; }
.section-card { margin-bottom: 16px; }
.section-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px; }
.header-left { display: flex; align-items: center; gap: 8px; font-weight: bold; }
.header-actions { display: flex; gap: 10px; align-items: center; }
.flow-steps { display: flex; align-items: center; justify-content: center; gap: 0; padding: 20px 0; }
.flow-step { display: flex; align-items: center; gap: 10px; opacity: 0.5; transition: all 0.3s; }
.flow-step.active { opacity: 1; }
.flow-step.done { opacity: 0.8; }
.step-icon { width: 44px; height: 44px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #fff; transition: transform 0.2s; }
.flow-step.active .step-icon { transform: scale(1.15); box-shadow: 0 0 12px rgba(0,0,0,0.2); }
.flow-step.done .step-icon { transform: scale(1.05); }
.step-info { min-width: 80px; }
.step-name { font-weight: bold; font-size: 14px; }
.step-desc { font-size: 11px; color: #909399; }
.step-arrow { color: #c0c4cc; margin: 0 12px; font-size: 18px; }
.table-title { font-weight: bold; margin-bottom: 10px; }
.task-cell { display: flex; flex-direction: column; }
.task-id { font-size: 12px; color: #909399; }
.task-desc { font-size: 13px; }
.text-muted { color: #c0c4cc; }
.detail-task { padding: 10px; background: #f5f7fa; border-radius: 4px; font-size: 13px; }
.detail-step-title { font-weight: bold; font-size: 14px; margin-bottom: 4px; }
.detail-footer { display: flex; align-items: center; gap: 16px; margin-top: 16px; }
.detail-meta { font-size: 13px; color: #909399; }
</style>
