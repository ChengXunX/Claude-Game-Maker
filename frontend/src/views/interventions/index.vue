<template>
  <div class="interventions-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>Agent 干预</span>
          <el-button type="primary" @click="handleNewIntervention" v-permission="'agents:manage'">
            <el-icon><Warning /></el-icon> 发起干预
          </el-button>
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
          <el-option label="指令干预" value="INSTRUCTION" />
          <el-option label="决策覆盖" value="OVERRIDE" />
          <el-option label="暂停" value="PAUSE" />
          <el-option label="恢复" value="RESUME" />
          <el-option label="重定向" value="REDIRECT" />
        </el-select>
        <el-input
          v-model="filterAgentId"
          placeholder="Agent ID"
          clearable
          style="width: 200px"
        />
        <el-button type="primary" @click="loadInterventions">查询</el-button>
      </div>

      <!-- 干预记录列表 -->
      <el-table :data="interventions" v-loading="loading" stripe>
        <el-table-column prop="interventionNo" label="编号" width="150" show-overflow-tooltip />
        <el-table-column prop="agentId" label="Agent ID" width="120" show-overflow-tooltip />
        <el-table-column prop="agentName" label="Agent 名称" width="120" />
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
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ACTIVE' ? '生效' : '已过期' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="handleViewDetail(row)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && interventions.length === 0" description="暂无干预记录" />
    </el-card>

    <!-- 新增干预对话框 -->
    <el-dialog v-model="dialogVisible" title="发起干预" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="Agent ID" prop="agentId">
          <el-input v-model="form.agentId" placeholder="目标 Agent ID" />
        </el-form-item>
        <el-form-item label="干预类型" prop="interventionType">
          <el-select v-model="form.interventionType" placeholder="选择干预类型">
            <el-option label="指令干预" value="INSTRUCTION" />
            <el-option label="决策覆盖" value="OVERRIDE" />
            <el-option label="暂停" value="PAUSE" />
            <el-option label="恢复" value="RESUME" />
          </el-select>
        </el-form-item>
        <el-form-item label="指令内容" prop="instruction" v-if="form.interventionType === 'INSTRUCTION'">
          <el-input v-model="form.instruction" type="textarea" :rows="4" placeholder="输入给 Agent 的指令" />
        </el-form-item>
        <el-form-item label="新决策" prop="newDecision" v-if="form.interventionType === 'OVERRIDE'">
          <el-input v-model="form.newDecision" type="textarea" :rows="4" placeholder="覆盖的决策内容" />
        </el-form-item>
        <el-form-item label="原因" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="2" placeholder="干预原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * Agent 干预页面
 * 人工干预 Agent 的决策和行为
 *
 * 操作维度：项目级
 * 权限要求：agents:manage
 */
import { ref, onMounted } from 'vue'
import { interventionApi } from '@/api'
import { ElMessage } from 'element-plus'
import ProjectSelector from '@/components/ProjectSelector.vue'

const loading = ref(false)
const interventions = ref([])
const selectedProjectId = ref(localStorage.getItem('selectedProjectId') || '')
const filterType = ref('')
const filterAgentId = ref('')

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
  agentId: [{ required: true, message: '请输入 Agent ID', trigger: 'blur' }],
  interventionType: [{ required: true, message: '请选择干预类型', trigger: 'change' }],
  reason: [{ required: true, message: '请输入干预原因', trigger: 'blur' }]
}

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
  } else {
    interventions.value = []
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
    if (filterType.value) params.type = filterType.value
    if (filterAgentId.value) params.agentId = filterAgentId.value

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
      // 提取错误信息
      let errorMessage = '提交失败'
      if (error.response && error.response.data) {
        // 后端返回的错误信息
        errorMessage = error.response.data.message || error.response.data.error || errorMessage
      } else if (error.message) {
        // 前端或其他错误
        errorMessage = error.message
      }
      ElMessage.error(errorMessage)
    }
  } finally {
    submitting.value = false
  }
}

/** 查看详情 */
const handleViewDetail = (intervention) => {
  // 可以跳转到详情页或显示详情对话框
  ElMessage.info(`干预编号: ${intervention.interventionNo}`)
}

onMounted(() => {
  loadInterventions()
})
</script>

<style scoped>
.interventions-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
</style>
