<template>
  <div class="interventions-page">
    <!-- 统计概览 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-primary-light-9)">
            <el-icon :size="24" color="var(--el-color-primary)"><Warning /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ interventions.length }}</div>
            <div class="stat-label">干预总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-success-light-9)">
            <el-icon :size="24" color="var(--el-color-success)"><CircleCheck /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ activeCount }}</div>
            <div class="stat-label">进行中</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-warning-light-9)">
            <el-icon :size="24" color="var(--el-color-warning)"><Promotion /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ instructionCount }}</div>
            <div class="stat-label">指令干预</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-danger-light-9)">
            <el-icon :size="24" color="var(--el-color-danger)"><EditPen /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ overrideCount }}</div>
            <div class="stat-label">决策覆盖</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>Agent 干预</span>
          <div class="header-actions">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索干预记录..."
              clearable
              style="width: 200px"
              :prefix-icon="Search"
            />
            <el-button type="warning" @click="handleVersionIteration" v-permission="'agents:manage'">
              <el-icon><Promotion /></el-icon> 版本迭代
            </el-button>
            <el-button type="primary" @click="handleNewIntervention" v-permission="'agents:manage'">
              <el-icon><Warning /></el-icon> 发起干预
            </el-button>
          </div>
        </div>
      </template>

      <!-- 筛选区 -->
      <div class="filter-bar">
        <ProjectSelector
          v-model="selectedProjectId"
          placeholder="选择项目"
          width="200px"
          size="default"
          @change="handleProjectChange"
        />
        <el-select v-model="filterType" placeholder="干预类型" clearable style="width: 150px">
          <el-option label="全部" value="" />
          <el-option label="指令干预" value="INSTRUCTION" />
          <el-option label="决策覆盖" value="DECISION_OVERRIDE" />
          <el-option label="方向调整" value="DIRECTION_CHANGE" />
          <el-option label="暂停" value="PAUSE" />
          <el-option label="恢复" value="RESUME" />
          <el-option label="版本迭代" value="VERSION_ITERATION" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="状态" clearable style="width: 120px">
          <el-option label="全部" value="" />
          <el-option label="待处理" value="PENDING" />
          <el-option label="已确认" value="ACKNOWLEDGED" />
          <el-option label="执行中" value="EXECUTING" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已拒绝" value="REJECTED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
      </div>

      <!-- 干预记录列表 -->
      <el-table :data="filteredInterventions" v-loading="loading" stripe @row-click="handleViewDetail">
        <el-table-column prop="interventionNo" label="编号" width="150" show-overflow-tooltip />
        <el-table-column prop="agentName" label="Agent" width="120">
          <template #default="{ row }">
            <span>{{ row.agentName || row.agentId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="干预类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getInterventionType(row.interventionType)" size="small">
              {{ getInterventionLabel(row.interventionType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="instruction" label="指令内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="username" label="操作人" width="100" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small" effect="plain">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click.stop="handleViewDetail(row)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && filteredInterventions.length === 0" description="暂无干预记录" />
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :title="currentIntervention?.interventionNo || '干预详情'"
      size="450px"
      direction="rtl"
    >
      <template v-if="currentIntervention">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="干预编号">{{ currentIntervention.interventionNo }}</el-descriptions-item>
          <el-descriptions-item label="Agent">
            {{ currentIntervention.agentName || currentIntervention.agentId }}
          </el-descriptions-item>
          <el-descriptions-item label="干预类型">
            <el-tag :type="getInterventionType(currentIntervention.interventionType)" size="small">
              {{ getInterventionLabel(currentIntervention.interventionType) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusTagType(currentIntervention.status)" size="small">
              {{ getStatusLabel(currentIntervention.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="操作人">{{ currentIntervention.username || '-' }}</el-descriptions-item>
          <el-descriptions-item label="操作时间">{{ formatTime(currentIntervention.createdAt) }}</el-descriptions-item>
        </el-descriptions>

        <el-divider>干预内容</el-divider>
        <div class="intervention-content">
          <div v-if="currentIntervention.instruction" class="content-section">
            <h4>指令内容</h4>
            <p>{{ currentIntervention.instruction }}</p>
          </div>
          <div v-if="currentIntervention.newDecision" class="content-section">
            <h4>新决策</h4>
            <p>{{ currentIntervention.newDecision }}</p>
          </div>
          <div v-if="currentIntervention.reason" class="content-section">
            <h4>干预原因</h4>
            <p>{{ currentIntervention.reason }}</p>
          </div>
        </div>

        <el-divider>干预效果</el-divider>
        <div class="intervention-effect">
          <el-alert
            v-if="currentIntervention.status === 'PENDING'"
            type="warning"
            :closable="false"
            show-icon
          >
            <template #title>待处理</template>
            <template #default>干预指令已发送，等待 Agent 确认执行</template>
          </el-alert>
          <el-alert
            v-else-if="currentIntervention.status === 'EXECUTING'"
            type="warning"
            :closable="false"
            show-icon
          >
            <template #title>执行中</template>
            <template #default>Agent 正在执行该干预指令</template>
          </el-alert>
          <el-alert
            v-else-if="currentIntervention.status === 'COMPLETED'"
            type="success"
            :closable="false"
            show-icon
          >
            <template #title>已完成</template>
            <template #default>干预指令已执行完成</template>
          </el-alert>
          <!-- 执行结果详情 -->
          <div v-if="currentIntervention.executionResult" class="execution-result">
            <h4>执行结果</h4>
            <div class="result-content">{{ currentIntervention.executionResult }}</div>
          </div>
          <el-alert
            v-else-if="currentIntervention.status === 'REJECTED'"
            type="danger"
            :closable="false"
            show-icon
          >
            <template #title>已拒绝</template>
            <template #default>Agent 拒绝了该干预指令</template>
          </el-alert>
          <el-alert
            v-else
            type="info"
            :closable="false"
            show-icon
          >
            <template #title>{{ getStatusLabel(currentIntervention.status) }}</template>
            <template #default>该干预指令当前状态为{{ getStatusLabel(currentIntervention.status) }}</template>
          </el-alert>
        </div>
      </template>
    </el-drawer>

    <!-- 新增干预对话框 -->
    <el-dialog v-model="dialogVisible" title="发起干预" width="550px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="目标 Agent" prop="agentId">
          <el-select
            v-model="form.agentId"
            placeholder="选择 Agent"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="agent in availableAgents"
              :key="agent.id"
              :label="`${agent.name} (${agent.role})`"
              :value="agent.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="干预类型" prop="interventionType">
          <el-radio-group v-model="form.interventionType">
            <el-radio-button value="INSTRUCTION">指令干预</el-radio-button>
            <el-radio-button value="DECISION_OVERRIDE">决策覆盖</el-radio-button>
            <el-radio-button value="DIRECTION_CHANGE">方向调整</el-radio-button>
            <el-radio-button value="PAUSE">暂停</el-radio-button>
            <el-radio-button value="RESUME">恢复</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="指令内容" prop="instruction" v-if="form.interventionType === 'INSTRUCTION'">
          <el-input
            v-model="form.instruction"
            type="textarea"
            :rows="4"
            placeholder="输入给 Agent 的指令，例如：请优先处理登录模块的 bug"
          />
        </el-form-item>
        <el-form-item label="新方向" prop="instruction" v-if="form.interventionType === 'DIRECTION_CHANGE'">
          <el-input
            v-model="form.instruction"
            type="textarea"
            :rows="4"
            placeholder="输入新的工作方向，例如：暂停美术资源制作，优先完成核心玩法"
          />
        </el-form-item>
        <el-form-item label="新决策" prop="newDecision" v-if="form.interventionType === 'DECISION_OVERRIDE'">
          <el-input
            v-model="form.newDecision"
            type="textarea"
            :rows="4"
            placeholder="覆盖的决策内容"
          />
        </el-form-item>
        <el-form-item label="干预原因" prop="reason">
          <el-input
            v-model="form.reason"
            type="textarea"
            :rows="2"
            placeholder="说明干预的原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">提交干预</el-button>
      </template>
    </el-dialog>

    <!-- 版本迭代对话框 -->
    <el-dialog v-model="iterationDialogVisible" title="版本迭代" width="600px">
      <el-form ref="iterationFormRef" :model="iterationForm" :rules="iterationRules" label-width="100px">
        <el-form-item label="目标项目" prop="projectId">
          <el-input :value="selectedProjectId" disabled />
        </el-form-item>
        <el-form-item label="迭代需求" prop="requirements">
          <el-input
            v-model="iterationForm.requirements"
            type="textarea"
            :rows="6"
            placeholder="请详细描述版本迭代的需求，例如：&#10;- 添加新的游戏关卡&#10;- 优化UI动画效果&#10;- 修复已知bug&#10;- 添加新的游戏功能"
          />
        </el-form-item>
        <el-form-item label="目标版本">
          <el-input v-model="iterationForm.version" placeholder="例如：v1.1.0（可选）" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="iterationForm.priority" placeholder="选择优先级" style="width: 100%">
            <el-option label="高" value="HIGH" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="低" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker
            v-model="iterationForm.deadline"
            type="datetime"
            placeholder="选择截止时间（可选）"
            style="width: 100%"
          />
        </el-form-item>
        <el-alert
          title="版本迭代说明"
          type="info"
          description="发起版本迭代后，制作人会分析需求，重新制定迭代计划，并启动新的开发流程。已完成的里程碑将被保留，新的迭代将基于当前版本进行。"
          show-icon
          :closable="false"
          style="margin-bottom: 16px"
        />
      </el-form>
      <template #footer>
        <el-button @click="iterationDialogVisible = false">取消</el-button>
        <el-button type="warning" @click="handleIterationSubmit" :loading="iterationSubmitting">
          <el-icon><Promotion /></el-icon> 发起迭代
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * Agent 干预页面
 * 人工干预 Agent 的决策和行为
 *
 * 功能：
 * - 统计概览（总数、生效中、指令干预、决策覆盖）
 * - 干预记录列表（支持搜索和筛选）
 * - 干预详情（侧边抽屉）
 * - 发起干预（支持选择 Agent）
 * - 干预效果展示
 *
 * 操作维度：项目级
 * 权限要求：agents:manage
 */
import { ref, computed, onMounted } from 'vue'
import { interventionApi, agentApi } from '@/api'
import { ElMessage } from 'element-plus'
import { Warning, CircleCheck, Promotion, EditPen, Search } from '@element-plus/icons-vue'
import ProjectSelector from '@/components/ProjectSelector.vue'

const loading = ref(false)
const interventions = ref([])
const selectedProjectId = ref(localStorage.getItem('selectedProjectId') || '')
const filterType = ref('')
const filterStatus = ref('')
const searchKeyword = ref('')
const availableAgents = ref([])

// 详情抽屉
const drawerVisible = ref(false)
const currentIntervention = ref(null)

/** 对话框 */
const dialogVisible = ref(false)
const formRef = ref(null)
const submitting = ref(false)
const form = ref({
  agentId: '',
  interventionType: 'INSTRUCTION',
  instruction: '',
  newDecision: '',
  reason: ''
})
const rules = {
  agentId: [{ required: true, message: '请选择 Agent', trigger: 'change' }],
  interventionType: [{ required: true, message: '请选择干预类型', trigger: 'change' }],
  reason: [{ required: true, message: '请输入干预原因', trigger: 'blur' }]
}

/** 版本迭代对话框 */
const iterationDialogVisible = ref(false)
const iterationFormRef = ref(null)
const iterationSubmitting = ref(false)
const iterationForm = ref({
  requirements: '',
  version: '',
  priority: 'MEDIUM',
  deadline: null
})
const iterationRules = {
  requirements: [{ required: true, message: '请输入迭代需求', trigger: 'blur' }]
}

// 统计
const activeCount = computed(() => interventions.value.filter(i => i.status === 'PENDING' || i.status === 'ACKNOWLEDGED' || i.status === 'EXECUTING').length)
const instructionCount = computed(() => interventions.value.filter(i => i.interventionType === 'INSTRUCTION').length)
const overrideCount = computed(() => interventions.value.filter(i => i.interventionType === 'DECISION_OVERRIDE').length)

// 筛选后的干预记录
const filteredInterventions = computed(() => {
  let result = interventions.value

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(i =>
      i.interventionNo?.toLowerCase().includes(keyword) ||
      i.agentName?.toLowerCase().includes(keyword) ||
      i.instruction?.toLowerCase().includes(keyword) ||
      i.reason?.toLowerCase().includes(keyword)
    )
  }

  if (filterType.value) {
    result = result.filter(i => i.interventionType === filterType.value)
  }

  if (filterStatus.value) {
    result = result.filter(i => i.status === filterStatus.value)
  }

  return result
})

/** 获取干预类型标签颜色 */
const getInterventionType = (type) => {
  const typeMap = {
    'INSTRUCTION': 'warning',
    'DECISION_OVERRIDE': 'danger',
    'DIRECTION_CHANGE': 'primary',
    'PAUSE': 'info',
    'RESUME': 'success',
    'PRIORITY_CHANGE': 'warning',
    'TASK_CANCEL': 'danger',
    'TASK_REASSIGN': 'primary',
    'URGENT_INSTRUCTION': 'danger',
    'VERSION_ITERATION': 'warning',
    'OTHER': 'info'
  }
  return typeMap[type] || 'info'
}

/** 获取干预类型文本 */
const getInterventionLabel = (type) => {
  const labelMap = {
    'INSTRUCTION': '指令',
    'DECISION_OVERRIDE': '决策覆盖',
    'DIRECTION_CHANGE': '方向调整',
    'PAUSE': '暂停',
    'RESUME': '恢复',
    'PRIORITY_CHANGE': '优先级调整',
    'TASK_CANCEL': '任务取消',
    'TASK_REASSIGN': '任务重分配',
    'URGENT_INSTRUCTION': '紧急指令',
    'VERSION_ITERATION': '版本迭代',
    'OTHER': '其他'
  }
  return labelMap[type] || type
}

/** 获取状态标签颜色 */
const getStatusTagType = (status) => {
  const map = {
    'PENDING': 'warning',
    'ACKNOWLEDGED': 'primary',
    'EXECUTING': 'warning',
    'COMPLETED': 'success',
    'REJECTED': 'danger',
    'CANCELLED': 'info'
  }
  return map[status] || 'info'
}

/** 获取状态文本 */
const getStatusLabel = (status) => {
  const map = {
    'PENDING': '待处理',
    'ACKNOWLEDGED': '已确认',
    'EXECUTING': '执行中',
    'COMPLETED': '已完成',
    'REJECTED': '已拒绝',
    'CANCELLED': '已取消'
  }
  return map[status] || status
}

/** 项目切换 */
const handleProjectChange = (projectId) => {
  if (projectId) {
    loadInterventions()
    loadAgents()
  } else {
    interventions.value = []
    availableAgents.value = []
  }
}

/** 格式化时间 */
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

/** 加载干预记录 */
const loadInterventions = async () => {
  loading.value = true
  try {
    const params = {}
    if (selectedProjectId.value) params.projectId = selectedProjectId.value
    if (filterType.value) params.type = filterType.value

    const data = await interventionApi.getAll(params)
    // 后端返回分页对象，提取 content 数组
    interventions.value = Array.isArray(data) ? data : (data?.content || [])
  } catch (error) {
    let errorMessage = '加载干预记录失败'
    if (error.response && error.response.data) {
      errorMessage = error.response.data.message || error.response.data.error || errorMessage
    }
    ElMessage.error(errorMessage)
  } finally {
    loading.value = false
  }
}

/** 加载 Agent 列表 */
const loadAgents = async () => {
  if (!selectedProjectId.value) return
  try {
    const data = await agentApi.getByProject(selectedProjectId.value)
    availableAgents.value = data || []
  } catch (error) {
    console.error('加载 Agent 列表失败:', error)
  }
}

/** 查看详情 */
const handleViewDetail = (intervention) => {
  currentIntervention.value = intervention
  drawerVisible.value = true
}

/** 打开新增对话框 */
const handleNewIntervention = () => {
  form.value = {
    agentId: '',
    interventionType: 'INSTRUCTION',
    instruction: '',
    newDecision: '',
    reason: ''
  }
  dialogVisible.value = true
}

/** 提交干预 */
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true

    const data = {
      agentId: form.value.agentId,
      interventionType: form.value.interventionType,
      reason: form.value.reason
    }

    if (form.value.interventionType === 'INSTRUCTION') {
      data.instruction = form.value.instruction
      await interventionApi.sendInstruction(data)
    } else if (form.value.interventionType === 'DECISION_OVERRIDE') {
      data.newDecision = form.value.newDecision
      await interventionApi.overrideDecision(data)
    } else if (form.value.interventionType === 'DIRECTION_CHANGE') {
      data.newDirection = form.value.instruction
      await interventionApi.changeDirection(data)
    } else if (form.value.interventionType === 'PAUSE') {
      await interventionApi.pauseAgent(form.value.agentId, form.value.reason)
    } else if (form.value.interventionType === 'RESUME') {
      await interventionApi.resumeAgent(form.value.agentId, form.value.reason)
    }

    ElMessage.success('干预指令已发送')
    dialogVisible.value = false
    loadInterventions()
  } catch (error) {
    if (error !== false) {
      let errorMessage = '提交失败'
      if (error.response && error.response.data) {
        errorMessage = error.response.data.message || error.response.data.error || errorMessage
      } else if (error.message) {
        errorMessage = error.message
      }
      ElMessage.error(errorMessage)
    }
  } finally {
    submitting.value = false
  }
}

/** 打开版本迭代对话框 */
const handleVersionIteration = () => {
  if (!selectedProjectId.value) {
    ElMessage.warning('请先选择项目')
    return
  }
  iterationForm.value = {
    requirements: '',
    version: '',
    priority: 'MEDIUM',
    deadline: null
  }
  iterationDialogVisible.value = true
}

/** 提交版本迭代 */
const handleIterationSubmit = async () => {
  try {
    await iterationFormRef.value.validate()
    iterationSubmitting.value = true

    const data = {
      projectId: selectedProjectId.value,
      requirements: iterationForm.value.requirements,
      version: iterationForm.value.version || undefined,
      priority: iterationForm.value.priority,
      deadline: iterationForm.value.deadline ? new Date(iterationForm.value.deadline).toISOString() : undefined
    }

    await interventionApi.startVersionIteration(data)

    ElMessage.success('版本迭代已发起，制作人将分析需求并启动迭代')
    iterationDialogVisible.value = false
    loadInterventions()
  } catch (error) {
    if (error !== false) {
      let errorMessage = '发起版本迭代失败'
      if (error.response && error.response.data) {
        errorMessage = error.response.data.message || error.response.data.error || errorMessage
      } else if (error.message) {
        errorMessage = error.message
      }
      ElMessage.error(errorMessage)
    }
  } finally {
    iterationSubmitting.value = false
  }
}

onMounted(() => {
  loadInterventions()
  if (selectedProjectId.value) {
    loadAgents()
  }
})
</script>

<style scoped>
.interventions-page {
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

/* 卡片头部 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

/* 筛选区 */
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

/* 详情内容 */
.intervention-content {
  margin-top: 16px;
}

.content-section {
  margin-bottom: 16px;
}

.content-section h4 {
  margin: 0 0 8px;
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.content-section p {
  margin: 0;
  padding: 12px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  line-height: 1.6;
}

.intervention-effect {
  margin-top: 16px;
}

/* 执行结果 */
.execution-result {
  margin-top: 12px;
  padding: 12px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
}

.execution-result h4 {
  margin: 0 0 8px;
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.result-content {
  font-size: 13px;
  line-height: 1.6;
  color: var(--el-text-color-regular);
  white-space: pre-wrap;
  word-break: break-word;
}

/* 响应式 */
@media (max-width: 767px) {
  .interventions-page {
    padding: 12px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
  }

  .filter-bar {
    flex-direction: column;
  }

  :deep(.el-dialog),
  :deep(.el-drawer) {
    width: 90% !important;
  }
}
</style>
