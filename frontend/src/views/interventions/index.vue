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
            <div class="stat-label">生效中</div>
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
          <el-option label="决策覆盖" value="OVERRIDE" />
          <el-option label="暂停" value="PAUSE" />
          <el-option label="恢复" value="RESUME" />
          <el-option label="重定向" value="REDIRECT" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="状态" clearable style="width: 120px">
          <el-option label="全部" value="" />
          <el-option label="生效中" value="ACTIVE" />
          <el-option label="已过期" value="EXPIRED" />
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
        <el-table-column prop="userName" label="操作人" width="100" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small" effect="plain">
              {{ row.status === 'ACTIVE' ? '生效' : '已过期' }}
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
            <el-tag :type="currentIntervention.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ currentIntervention.status === 'ACTIVE' ? '生效中' : '已过期' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="操作人">{{ currentIntervention.userName || '-' }}</el-descriptions-item>
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
            v-if="currentIntervention.status === 'ACTIVE'"
            type="success"
            :closable="false"
            show-icon
          >
            <template #title>干预生效中</template>
            <template #default>该干预指令正在对 Agent 生效</template>
          </el-alert>
          <el-alert
            v-else
            type="info"
            :closable="false"
            show-icon
          >
            <template #title>干预已过期</template>
            <template #default>该干预指令已不再生效</template>
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
            <el-radio-button value="OVERRIDE">决策覆盖</el-radio-button>
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
        <el-form-item label="新决策" prop="newDecision" v-if="form.interventionType === 'OVERRIDE'">
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

// 统计
const activeCount = computed(() => interventions.value.filter(i => i.status === 'ACTIVE').length)
const instructionCount = computed(() => interventions.value.filter(i => i.interventionType === 'INSTRUCTION').length)
const overrideCount = computed(() => interventions.value.filter(i => i.interventionType === 'OVERRIDE').length)

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

/** 获取干预类型标签 */
const getInterventionType = (type) => {
  const typeMap = {
    'INSTRUCTION': 'warning',
    'OVERRIDE': 'danger',
    'PAUSE': 'info',
    'RESUME': 'success',
    'REDIRECT': 'primary'
  }
  return typeMap[type] || 'info'
}

/** 获取干预类型文本 */
const getInterventionLabel = (type) => {
  const labelMap = {
    'INSTRUCTION': '指令',
    'OVERRIDE': '覆盖',
    'PAUSE': '暂停',
    'RESUME': '恢复',
    'REDIRECT': '重定向'
  }
  return labelMap[type] || type
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
    interventions.value = data || []
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
    } else if (form.value.interventionType === 'OVERRIDE') {
      data.newDecision = form.value.newDecision
      await interventionApi.overrideDecision(data)
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
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
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
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: var(--el-text-color-primary);
}

.stat-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
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
