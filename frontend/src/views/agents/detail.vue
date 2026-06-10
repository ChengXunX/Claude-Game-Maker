<template>
  <div class="agent-detail-page">
    <!-- 基本信息 -->
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span>Agent 详情</span>
            <el-tag v-if="agent" :type="agent.alive ? 'success' : 'info'" size="small" class="ml-2">
              {{ agent.alive ? '运行中' : '已停止' }}
            </el-tag>
            <el-tag v-if="agent?.busy" type="warning" size="small" class="ml-2">忙碌</el-tag>
          </div>
          <el-button @click="router.back()">返回</el-button>
        </div>
      </template>

      <el-descriptions :column="2" border v-if="agent">
        <el-descriptions-item label="Agent ID">{{ agent.id }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ agent.name }}</el-descriptions-item>
        <el-descriptions-item label="角色">
          <el-tag :type="getRoleTagType(agent.role)" size="small">{{ getRoleLabel(agent.role) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="项目 ID">{{ projectId }}</el-descriptions-item>
        <el-descriptions-item label="推理深度">{{ getDepthLabel(agent.reasoningDepth) }}</el-descriptions-item>
        <el-descriptions-item label="工作目录">{{ agent.workDir || '-' }}</el-descriptions-item>
        <el-descriptions-item label="任务数">{{ agent.taskCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="待处理消息">{{ agent.pendingMessages || 0 }}</el-descriptions-item>
      </el-descriptions>

      <!-- 操作按钮 -->
      <div class="actions" v-if="agent">
        <el-button type="primary" @click="handleSendTask">发送任务</el-button>
        <el-button
          :type="agent.alive ? 'warning' : 'success'"
          @click="handleToggle"
        >
          {{ agent.alive ? '停止' : '启动' }}
        </el-button>
        <el-button type="info" @click="handleRestart">重启</el-button>
      </div>
    </el-card>

    <!-- 当前任务 -->
    <el-card class="mt-4" v-if="agent?.currentTask">
      <template #header>
        <span>当前任务</span>
      </template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="任务标题">{{ agent.currentTask.title }}</el-descriptions-item>
        <el-descriptions-item label="任务描述">{{ agent.currentTask.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag type="warning" size="small">执行中</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 任务列表 -->
    <el-card class="mt-4" v-if="agent?.tasks?.length > 0">
      <template #header>
        <span>任务列表 ({{ agent.tasks.length }})</span>
      </template>
      <el-table :data="agent.tasks" stripe size="small">
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getTaskStatusType(row.status)" size="small">
              {{ getTaskStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优先级" width="80">
          <template #default="{ row }">
            <el-tag :type="getPriorityType(row.priority)" size="small" effect="plain">
              {{ row.priority }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">
            {{ row.createdAt ? new Date(row.createdAt).toLocaleString('zh-CN') : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="完成时间" width="160">
          <template #default="{ row }">
            {{ row.completedAt ? new Date(row.completedAt).toLocaleString('zh-CN') : '-' }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 角色提示词 -->
    <el-card class="mt-4">
      <template #header>
        <div class="card-header">
          <span>角色提示词</span>
          <div class="header-actions">
            <el-button type="primary" text size="small" @click="showOptimizeDialog = true" :loading="optimizing">
              <el-icon><MagicStick /></el-icon> AI优化
            </el-button>
            <el-button type="primary" text size="small" @click="showFullPrompt = true" v-if="rolePrompt">
              查看完整
            </el-button>
          </div>
        </div>
      </template>
      <div v-if="rolePrompt" class="prompt-preview">
        <MarkdownRenderer :content="rolePrompt.substring(0, 500) + (rolePrompt.length > 500 ? '...' : '')" />
      </div>
      <el-empty v-else description="暂无角色提示词" :image-size="60" />
    </el-card>

    <!-- 项目级Agent配置 -->
    <el-card class="mt-4">
      <template #header>
        <div class="card-header">
          <span>项目级配置</span>
          <el-button type="primary" text size="small" @click="showConfigDialog = true">
            编辑配置
          </el-button>
        </div>
      </template>
      <el-descriptions :column="1" border v-if="projectConfig">
        <el-descriptions-item label="自定义系统提示词">
          <div class="config-content">{{ projectConfig.customSystemPrompt || '未配置' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="自定义能力提示词">
          <div class="config-content">{{ projectConfig.customCapabilityPrompt || '未配置' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="项目上下文">
          <div class="config-content">{{ projectConfig.projectContext || '未配置' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="版本">
          v{{ projectConfig.version || 1 }}
        </el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="暂无项目级配置" :image-size="60" />
    </el-card>

    <!-- 职责权重 -->
    <el-card class="mt-4">
      <template #header>
        <span>职责权重</span>
      </template>
      <div v-if="responsibilityWeights" class="weights-chart">
        <div v-for="(weight, name) in responsibilityWeights" :key="name" class="weight-item">
          <div class="weight-label">{{ name }}</div>
          <el-progress
            :percentage="Math.round(weight * 100)"
            :stroke-width="20"
            :text-inside="true"
          />
        </div>
      </div>
      <el-empty v-else description="暂无职责权重配置" :image-size="60" />
    </el-card>

    <!-- 绩效评分权重 -->
    <el-card class="mt-4">
      <template #header>
        <div class="card-header">
          <span>绩效评分权重</span>
          <el-button size="small" type="primary" text @click="showPerformanceWeightsDialog = true">
            <el-icon><Edit /></el-icon> 编辑
          </el-button>
        </div>
      </template>
      <div v-if="performanceWeights" class="performance-weights">
        <div class="weight-item">
          <div class="weight-label">质量</div>
          <el-progress :percentage="Math.round((performanceWeights.quality || 1) / 2 * 100)" :stroke-width="20" :text-inside="true" :format="() => (performanceWeights.quality || 1).toFixed(1)" />
          <div class="weight-desc">任务完成质量、代码质量</div>
        </div>
        <div class="weight-item">
          <div class="weight-label">效率</div>
          <el-progress :percentage="Math.round((performanceWeights.efficiency || 1) / 2 * 100)" :stroke-width="20" :text-inside="true" :format="() => (performanceWeights.efficiency || 1).toFixed(1)" />
          <div class="weight-desc">任务完成速度、响应时间</div>
        </div>
        <div class="weight-item">
          <div class="weight-label">协作</div>
          <el-progress :percentage="Math.round((performanceWeights.collaboration || 1) / 2 * 100)" :stroke-width="20" :text-inside="true" :format="() => (performanceWeights.collaboration || 1).toFixed(1)" />
          <div class="weight-desc">团队沟通、配合度</div>
        </div>
        <div class="weight-item">
          <div class="weight-label">创新</div>
          <el-progress :percentage="Math.round((performanceWeights.innovation || 1) / 2 * 100)" :stroke-width="20" :text-inside="true" :format="() => (performanceWeights.innovation || 1).toFixed(1)" />
          <div class="weight-desc">创新思维、问题解决</div>
        </div>
      </div>
      <el-empty v-else description="使用默认权重" :image-size="60" />
    </el-card>

    <!-- 绩效权重编辑弹窗 -->
    <el-dialog v-model="showPerformanceWeightsDialog" title="绩效评分权重配置" width="500px">
      <el-alert type="info" :closable="false" class="mb-4">
        <template #title>
          <p>配置 <strong>{{ agent?.name }}</strong> 的绩效评分权重。权重越大，该维度在综合评分中占比越高。</p>
        </template>
      </el-alert>
      <el-form :model="performanceWeightsForm" label-width="80px">
        <el-form-item label="质量">
          <el-slider v-model="performanceWeightsForm.quality" :min="0.5" :max="2.0" :step="0.1" show-input :format-tooltip="v => v.toFixed(1)" />
        </el-form-item>
        <el-form-item label="效率">
          <el-slider v-model="performanceWeightsForm.efficiency" :min="0.5" :max="2.0" :step="0.1" show-input :format-tooltip="v => v.toFixed(1)" />
        </el-form-item>
        <el-form-item label="协作">
          <el-slider v-model="performanceWeightsForm.collaboration" :min="0.5" :max="2.0" :step="0.1" show-input :format-tooltip="v => v.toFixed(1)" />
        </el-form-item>
        <el-form-item label="创新">
          <el-slider v-model="performanceWeightsForm.innovation" :min="0.5" :max="2.0" :step="0.1" show-input :format-tooltip="v => v.toFixed(1)" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPerformanceWeights">恢复默认</el-button>
        <el-button @click="showPerformanceWeightsDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSavePerformanceWeights" :loading="savingPerformanceWeights">保存</el-button>
      </template>
    </el-dialog>

    <!-- AI优化提示词对话框 -->
    <el-dialog v-model="showOptimizeDialog" title="AI 优化提示词" width="550px">
      <el-alert type="info" :closable="false" class="mb-4">
        <template #title>
          <p style="margin: 0">AI 将分析当前提示词并给出优化建议。你可以指定优化方向，也可以留空让 AI 自动分析。</p>
        </template>
      </el-alert>
      <el-form label-width="80px">
        <el-form-item label="优化方向">
          <el-input
            v-model="optimizeDirection"
            type="textarea"
            :rows="3"
            placeholder="可选，例如：增强协作协议、补充错误处理、优化输出格式、增加质量检查清单..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showOptimizeDialog = false">取消</el-button>
        <el-button type="primary" @click="handleOptimizePrompt" :loading="optimizing">
          <el-icon><MagicStick /></el-icon> 开始优化
        </el-button>
      </template>
    </el-dialog>

    <!-- 完整提示词弹窗 -->
    <el-dialog v-model="showFullPrompt" title="角色提示词" width="700px" top="5vh">
      <div class="full-prompt">
        <MarkdownRenderer :content="rolePrompt" />
      </div>
      <template #footer>
        <el-button @click="showFullPrompt = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 配置编辑弹窗 -->
    <el-dialog v-model="showConfigDialog" title="项目级Agent配置" width="800px" top="5vh">
      <el-form :model="configForm" label-width="120px">
        <el-form-item label="自定义系统提示词">
          <MarkdownEditor v-model="configForm.customSystemPrompt" :rows="4" placeholder="输入项目特定的系统提示词，追加到默认提示词之后..." />
        </el-form-item>
        <el-form-item label="自定义能力提示词">
          <MarkdownEditor v-model="configForm.customCapabilityPrompt" :rows="4" placeholder="描述该Agent在项目中的具体职责..." />
        </el-form-item>
        <el-form-item label="项目上下文">
          <MarkdownEditor v-model="configForm.projectContext" :rows="4" placeholder="输入项目特定的上下文信息..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showConfigDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveConfig" :loading="savingConfig">保存</el-button>
      </template>
    </el-dialog>

    <!-- 优化结果弹窗 -->
    <el-dialog v-model="showOptimizeResult" title="AI优化建议" width="800px" top="5vh">
      <div v-if="optimizeResult" class="optimize-result">
        <el-alert
          v-if="optimizeResult.success"
          title="优化完成"
          type="success"
          :description="optimizeResult.message"
          show-icon
          :closable="false"
          style="margin-bottom: 16px"
        />
        <el-alert
          v-else
          title="优化失败"
          type="error"
          :description="optimizeResult.message"
          show-icon
          :closable="false"
          style="margin-bottom: 16px"
        />
        <div v-if="optimizeResult.suggestions" class="suggestions">
          <h4>优化建议：</h4>
          <pre>{{ optimizeResult.fullResponse }}</pre>
        </div>
      </div>
      <template #footer>
        <el-button @click="showOptimizeResult = false">关闭</el-button>
        <el-button type="primary" @click="handleApplyOptimization" v-if="optimizeResult?.success">
          应用优化
        </el-button>
      </template>
    </el-dialog>

    <!-- 发送任务弹窗 -->
    <el-dialog v-model="taskDialogVisible" title="发送任务" width="500px">
      <el-form :model="taskForm" label-width="80px">
        <el-form-item label="Agent">
          <el-input :model-value="agent?.name" disabled />
        </el-form-item>
        <el-form-item label="任务内容" required>
          <el-input v-model="taskForm.content" type="textarea" :rows="4" placeholder="请输入任务内容..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="taskDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitTask" :loading="sendingTask">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { agentApi, recruitmentApi, projectAgentConfigApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MagicStick, Edit } from '@element-plus/icons-vue'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const agent = ref(null)
const rolePrompt = ref('')
const showFullPrompt = ref(false)
const taskDialogVisible = ref(false)
const sendingTask = ref(false)
const taskForm = ref({ content: '' })
const projectId = ref(route.params.projectId || '')
const agentRole = ref(route.params.agentRole || '')

// 项目级配置相关
const projectConfig = ref(null)
const responsibilityWeights = ref(null)
const showConfigDialog = ref(false)
const savingConfig = ref(false)
const configForm = ref({
  customSystemPrompt: '',
  customCapabilityPrompt: '',
  projectContext: ''
})

// 绩效评分权重相关
const performanceWeights = ref(null)
const showPerformanceWeightsDialog = ref(false)
const savingPerformanceWeights = ref(false)
const performanceWeightsForm = ref({
  quality: 1.0,
  efficiency: 1.0,
  collaboration: 1.0,
  innovation: 1.0
})

// AI优化相关
const optimizing = ref(false)
const showOptimizeResult = ref(false)
const optimizeResult = ref(null)
const showOptimizeDialog = ref(false)
const optimizeDirection = ref('')

const getRoleTagType = (role) => {
  const map = { 'producer': 'danger', 'server-dev': 'primary', 'client-dev': 'success', 'ui-dev': 'warning', 'system-planner': 'info', 'numerical-planner': 'info', 'tester': '', 'git-commit': 'info' }
  return map[role] || ''
}

const getRoleLabel = (role) => {
  const map = { 'producer': '制作人', 'server-dev': '服务端', 'client-dev': '客户端', 'ui-dev': 'UI设计', 'system-planner': '系统策划', 'numerical-planner': '数值策划', 'tester': '测试', 'git-commit': 'Git专员' }
  return map[role] || role
}

const getDepthLabel = (depth) => {
  const map = { 1: '快速', 2: '标准', 3: '深度', 4: '专家', 5: '极致' }
  return map[depth || 3] || '标准'
}

const getTaskStatusType = (status) => {
  const map = { 'PENDING': 'info', 'IN_PROGRESS': 'warning', 'COMPLETED': 'success', 'FAILED': 'danger' }
  return map[status] || ''
}

const getTaskStatusLabel = (status) => {
  const map = { 'PENDING': '待执行', 'IN_PROGRESS': '执行中', 'COMPLETED': '已完成', 'FAILED': '失败' }
  return map[status] || status
}

const getPriorityType = (priority) => {
  const map = { 'HIGH': 'danger', 'MEDIUM': 'warning', 'LOW': 'info' }
  return map[priority] || ''
}

const loadAgent = async () => {
  if (!projectId.value || !agentRole.value) return
  loading.value = true
  try {
    const data = await agentApi.getById(projectId.value, agentRole.value)
    agent.value = data
    // 加载角色提示词
    try {
      const detail = await recruitmentApi.getRoleDetail(agentRole.value)
      rolePrompt.value = detail?.prompt || ''
    } catch { /* 忽略 */ }

    // 加载项目级配置
    loadProjectConfig()

    // 加载职责权重
    loadResponsibilityWeights()

    // 加载绩效评分权重
    loadPerformanceWeights()
  } catch {
    ElMessage.error('加载 Agent 详情失败')
  } finally {
    loading.value = false
  }
}

/** 加载项目级配置 */
const loadProjectConfig = async () => {
  try {
    const data = await projectAgentConfigApi.getConfig(projectId.value, agentRole.value)
    projectConfig.value = data
    configForm.value = {
      customSystemPrompt: data?.customSystemPrompt || '',
      customCapabilityPrompt: data?.customCapabilityPrompt || '',
      projectContext: data?.projectContext || ''
    }
  } catch { /* 忽略 */ }
}

/** 加载职责权重 */
const loadResponsibilityWeights = async () => {
  try {
    const data = await projectAgentConfigApi.getWeights(projectId.value, agentRole.value)
    responsibilityWeights.value = data
  } catch { /* 忽略 */ }
}

/** 加载绩效评分权重 */
const loadPerformanceWeights = async () => {
  try {
    const data = await projectAgentConfigApi.getPerformanceWeights(projectId.value, agentRole.value)
    performanceWeights.value = data
    performanceWeightsForm.value = {
      quality: data?.quality || 1.0,
      efficiency: data?.efficiency || 1.0,
      collaboration: data?.collaboration || 1.0,
      innovation: data?.innovation || 1.0
    }
  } catch { /* 忽略 */ }
}

/** 恢复默认绩效权重 */
const resetPerformanceWeights = async () => {
  try {
    const data = await projectAgentConfigApi.getPerformanceWeights(projectId.value, agentRole.value + '?default=true')
    performanceWeightsForm.value = {
      quality: data?.quality || 1.0,
      efficiency: data?.efficiency || 1.0,
      collaboration: data?.collaboration || 1.0,
      innovation: data?.innovation || 1.0
    }
  } catch {
    performanceWeightsForm.value = { quality: 1.0, efficiency: 1.0, collaboration: 1.0, innovation: 1.0 }
  }
}

/** 保存绩效评分权重 */
const handleSavePerformanceWeights = async () => {
  savingPerformanceWeights.value = true
  try {
    await projectAgentConfigApi.savePerformanceWeights(projectId.value, agentRole.value, performanceWeightsForm.value)
    ElMessage.success('绩效评分权重已保存')
    showPerformanceWeightsDialog.value = false
    loadPerformanceWeights()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    savingPerformanceWeights.value = false
  }
}

/** 保存项目级配置 */
const handleSaveConfig = async () => {
  savingConfig.value = true
  try {
    await projectAgentConfigApi.saveConfig(projectId.value, agentRole.value, configForm.value)
    ElMessage.success('配置已保存')
    showConfigDialog.value = false
    loadProjectConfig()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    savingConfig.value = false
  }
}

/** AI优化提示词 */
const handleOptimizePrompt = async () => {
  optimizing.value = true
  showOptimizeDialog.value = false
  try {
    const params = {}
    if (optimizeDirection.value.trim()) {
      params.direction = optimizeDirection.value.trim()
    }
    const result = await projectAgentConfigApi.optimizePrompt(projectId.value, agentRole.value, params)
    optimizeResult.value = result
    showOptimizeResult.value = true
  } catch {
    ElMessage.error('AI优化失败')
  } finally {
    optimizing.value = false
  }
}

/** 应用优化结果 */
const handleApplyOptimization = async () => {
  if (!optimizeResult.value?.suggestions) return

  configForm.value = {
    customSystemPrompt: optimizeResult.value.suggestions.systemPrompt || configForm.value.customSystemPrompt,
    customCapabilityPrompt: optimizeResult.value.suggestions.capabilityPrompt || configForm.value.customCapabilityPrompt,
    projectContext: optimizeResult.value.suggestions.projectContext || configForm.value.projectContext
  }

  showOptimizeResult.value = false
  showConfigDialog.value = true
}

const handleToggle = async () => {
  const action = agent.value.alive ? '停止' : '启动'
  try {
    await ElMessageBox.confirm(`确定要${action} Agent 吗？`, '确认操作')
    if (agent.value.alive) {
      await agentApi.stop(projectId.value, agentRole.value)
    } else {
      await agentApi.start(projectId.value, agentRole.value)
    }
    ElMessage.success(`Agent 已${action}`)
    loadAgent()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(`${action}失败`)
  }
}

const handleRestart = async () => {
  try {
    await ElMessageBox.confirm('确定要重启 Agent 吗？', '确认操作')
    await agentApi.restart(projectId.value, agentRole.value)
    ElMessage.success('Agent 已重启')
    loadAgent()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('重启失败')
  }
}

const handleSendTask = () => {
  taskForm.value = { content: '' }
  taskDialogVisible.value = true
}

const handleSubmitTask = async () => {
  if (!taskForm.value.content.trim()) {
    ElMessage.warning('请输入任务内容')
    return
  }
  sendingTask.value = true
  try {
    await agentApi.sendTask(projectId.value, agentRole.value, taskForm.value.content)
    ElMessage.success('任务已发送')
    taskDialogVisible.value = false
    loadAgent()
  } catch {
    ElMessage.error('发送失败')
  } finally {
    sendingTask.value = false
  }
}

onMounted(() => { loadAgent() })
</script>

<style scoped>
.agent-detail-page { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-left { display: flex; align-items: center; gap: 8px; }
.header-actions { display: flex; gap: 8px; }
.actions { margin-top: 20px; display: flex; gap: 12px; }
.mt-4 { margin-top: 16px; }
.prompt-preview { background: #f5f7fa; border-radius: 8px; padding: 12px; font-size: 13px; line-height: 1.6; color: #606266; white-space: pre-wrap; }
.full-prompt { font-size: 13px; line-height: 1.8; white-space: pre-wrap; max-height: 65vh; overflow-y: auto; background: #f5f7fa; border-radius: 8px; padding: 16px; }
.config-content { font-size: 13px; line-height: 1.6; white-space: pre-wrap; max-height: 200px; overflow-y: auto; }
.weights-chart { display: flex; flex-direction: column; gap: 12px; }
.weight-item { display: flex; align-items: center; gap: 12px; }
.weight-label { width: 100px; font-size: 14px; color: #606266; }
.optimize-result { max-height: 60vh; overflow-y: auto; }
.optimize-result pre { background: #f5f7fa; border-radius: 8px; padding: 16px; font-size: 13px; line-height: 1.6; white-space: pre-wrap; }
.optimize-result h4 { margin: 16px 0 8px; color: #303133; }
</style>
