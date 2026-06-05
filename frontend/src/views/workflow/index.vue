<template>
  <div class="workflow-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>工作流管理</span>
          <div class="header-actions">
            <el-button type="warning" @click="showAiGenerate" v-permission="'workflow:manage'">
              <el-icon><MagicStick /></el-icon> AI 生成模板
            </el-button>
            <el-button type="success" @click="showCreateTemplate" v-permission="'workflow:manage'">
              <el-icon><Plus /></el-icon> 手动创建
            </el-button>
            <el-button type="primary" @click="handleStartWorkflow" v-permission="'workflow:manage'">
              <el-icon><VideoPlay /></el-icon> 启动工作流
            </el-button>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <!-- 工作流模板 -->
        <el-tab-pane label="工作流模板" name="templates">
          <div class="template-grid">
            <el-card
              v-for="template in templates"
              :key="template.id"
              class="template-card"
              shadow="hover"
            >
              <div class="template-header">
                <h3>{{ template.name }}</h3>
                <el-button
                  type="danger"
                  size="small"
                  text
                  @click="handleDeleteTemplate(template)"
                  v-permission="'workflow:manage'"
                >
                  删除
                </el-button>
              </div>
              <p class="template-desc">{{ template.description || '无描述' }}</p>
              <div class="template-steps">
                <div class="steps-label">流程步骤：</div>
                <div class="steps-flow">
                  <template v-for="(step, idx) in template.steps" :key="step.id">
                    <div class="step-node">
                      <el-tag :type="getStepTagType(step.agentRole)" size="small">
                        {{ step.name }}
                      </el-tag>
                      <span class="step-role">{{ getRoleLabel(step.agentRole) }}</span>
                    </div>
                    <el-icon v-if="idx < template.steps.length - 1" class="step-arrow"><Right /></el-icon>
                  </template>
                </div>
              </div>
              <div class="template-footer">
                <el-tag size="small" type="info">{{ (template.steps || []).length }} 步</el-tag>
                <el-button
                  type="primary"
                  size="small"
                  @click="handleStartWithTemplate(template)"
                  v-permission="'workflow:manage'"
                >
                  启动
                </el-button>
              </div>
            </el-card>
          </div>
          <el-empty v-if="!loading && templates.length === 0" description="暂无工作流模板" />
        </el-tab-pane>

        <!-- 运行中的工作流 -->
        <el-tab-pane label="运行中的工作流" name="instances">
          <el-table :data="instances" v-loading="loading" stripe>
            <el-table-column prop="id" label="实例 ID" width="120" show-overflow-tooltip />
            <el-table-column prop="templateId" label="模板" width="150">
              <template #default="{ row }">
                {{ getTemplateName(row.templateId) }}
              </template>
            </el-table-column>
            <el-table-column prop="projectId" label="项目" width="150" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" size="small">
                  {{ getStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="进度" width="180">
              <template #default="{ row }">
                <el-progress :percentage="getProgress(row)" :stroke-width="8" />
              </template>
            </el-table-column>
            <el-table-column label="创建时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button type="warning" size="small" text @click="handlePause(row)" v-if="row.status === 'RUNNING'" v-permission="'workflow:manage'">
                  暂停
                </el-button>
                <el-button type="success" size="small" text @click="handleResume(row)" v-if="row.status === 'PAUSED'" v-permission="'workflow:manage'">
                  恢复
                </el-button>
                <el-button type="danger" size="small" text @click="handleCancel(row)" v-if="['RUNNING', 'PAUSED'].includes(row.status)" v-permission="'workflow:manage'">
                  取消
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loading && instances.length === 0" description="暂无运行中的工作流" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 启动工作流对话框 -->
    <el-dialog v-model="startDialogVisible" title="启动工作流" width="500px">
      <el-form ref="startFormRef" :model="startForm" :rules="startRules" label-width="100px">
        <el-form-item label="选择模板" prop="templateId">
          <el-select v-model="startForm.templateId" placeholder="选择工作流模板" style="width: 100%">
            <el-option v-for="t in templates" :key="t.id" :label="t.name" :value="t.id">
              <div>
                <div>{{ t.name }}</div>
                <div style="font-size: 12px; color: #909399">{{ t.description }}</div>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="所属项目" prop="projectId">
          <ProjectSelector v-model="startForm.projectId" placeholder="选择项目" width="100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="startDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleStartSubmit" :loading="submitting">启动</el-button>
      </template>
    </el-dialog>

    <!-- 创建模板对话框 -->
    <el-dialog v-model="createDialogVisible" title="创建工作流模板" width="700px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="模板名称" prop="name">
          <el-input v-model="createForm.name" placeholder="如：自定义开发流程" />
        </el-form-item>
        <el-form-item label="模板描述" prop="description">
          <el-input v-model="createForm.description" type="textarea" :rows="2" placeholder="描述模板用途" />
        </el-form-item>
        <el-form-item label="流程步骤">
          <div class="steps-editor">
            <div v-for="(step, idx) in createForm.steps" :key="idx" class="step-edit-row">
              <el-input v-model="step.name" placeholder="步骤名称" style="width: 140px" />
              <el-select v-model="step.agentRole" placeholder="执行角色" style="width: 140px">
                <el-option v-for="role in agentRoles" :key="role.value" :label="role.label" :value="role.value" />
              </el-select>
              <el-input v-model="step.taskDescription" placeholder="任务描述" style="flex: 1" />
              <el-button type="danger" text @click="removeStep(idx)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <el-button type="primary" text @click="addStep" style="margin-top: 8px">
              <el-icon><Plus /></el-icon> 添加步骤
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateSubmit" :loading="submitting">创建</el-button>
      </template>
    </el-dialog>

    <!-- AI 生成模板对话框 -->
    <el-dialog v-model="aiDialogVisible" title="AI 生成工作流模板" width="650px">
      <div class="ai-generate-section">
        <p class="ai-hint">描述你想要的工作流，AI 将自动生成模板。支持中文描述，越详细生成的模板越精准。</p>
        <el-input
          v-model="aiDescription"
          type="textarea"
          :rows="4"
          placeholder="例如：我要开发一个 H5 小游戏，需要策划、前端开发、测试、部署上线的完整流程"
        />
        <div class="ai-examples">
          <span class="examples-label">快速填入：</span>
          <el-tag
            v-for="ex in aiExamples"
            :key="ex"
            class="example-tag"
            @click="aiDescription = ex"
            effect="plain"
          >
            {{ ex }}
          </el-tag>
        </div>
      </div>

      <!-- 生成结果预览 -->
      <div v-if="aiResult" class="ai-preview">
        <el-divider />
        <h4>生成结果预览</h4>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="模板名称">{{ aiResult.name }}</el-descriptions-item>
          <el-descriptions-item label="描述">{{ aiResult.description }}</el-descriptions-item>
        </el-descriptions>
        <div class="preview-steps">
          <div v-for="(step, idx) in aiResult.steps" :key="step.id" class="preview-step">
            <el-tag :type="getStepTagType(step.agentRole)" size="small">{{ step.name }}</el-tag>
            <span class="step-detail">{{ getRoleLabel(step.agentRole) }} - {{ step.taskDescription }}</span>
            <el-tag v-if="step.requiresApproval" type="warning" size="small" effect="plain">需审批</el-tag>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="aiDialogVisible = false">取消</el-button>
        <el-button v-if="aiResult" @click="aiResult = null; aiGenerating = false">重新生成</el-button>
        <el-button
          v-if="!aiResult"
          type="warning"
          @click="handleAiGenerate"
          :loading="aiGenerating"
        >
          {{ aiGenerating ? 'AI 生成中...' : '开始生成' }}
        </el-button>
        <el-button
          v-if="aiResult"
          type="primary"
          @click="handleAiSave"
          :loading="submitting"
        >
          保存模板
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { workflowApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import ProjectSelector from '@/components/ProjectSelector.vue'

const loading = ref(false)
const submitting = ref(false)
const activeTab = ref('templates')
const templates = ref([])
const instances = ref([])

// AI 生成
const aiDialogVisible = ref(false)
const aiDescription = ref('')
const aiGenerating = ref(false)
const aiResult = ref(null)
const aiExamples = [
  '开发一个 H5 小游戏的完整流程',
  '需要策划、服务端开发、客户端开发、测试、部署的标准流程',
  '快速原型验证，策划到开发到测试',
  '代码审查和质量检查流程'
]

const agentRoles = [
  { value: 'system-planner', label: '系统策划' },
  { value: 'numerical-planner', label: '数值策划' },
  { value: 'server-dev', label: '服务端开发' },
  { value: 'client-dev', label: '客户端开发' },
  { value: 'ui-dev', label: 'UI开发' },
  { value: 'tester', label: '测试' },
  { value: 'git-commit', label: 'Git专员' },
  { value: 'producer', label: '制作人' }
]

const roleLabelMap = {
  'system-planner': '系统策划',
  'numerical-planner': '数值策划',
  'server-dev': '服务端',
  'client-dev': '客户端',
  'ui-dev': 'UI开发',
  'tester': '测试',
  'git-commit': 'Git',
  'producer': '制作人'
}

const getRoleLabel = (role) => roleLabelMap[role] || role

const getStepTagType = (role) => {
  const map = {
    'system-planner': 'warning',
    'numerical-planner': 'warning',
    'server-dev': '',
    'client-dev': 'success',
    'ui-dev': 'success',
    'tester': 'danger',
    'git-commit': 'info',
    'producer': 'warning'
  }
  return map[role] || 'info'
}

const getTemplateName = (id) => {
  const t = templates.value.find(t => t.id === id)
  return t ? t.name : id
}

// 启动对话框
const startDialogVisible = ref(false)
const startFormRef = ref(null)
const startForm = ref({ templateId: '', projectId: '' })
const startRules = {
  templateId: [{ required: true, message: '请选择模板', trigger: 'change' }],
  projectId: [{ required: true, message: '请选择项目', trigger: 'change' }]
}

// 创建模板对话框
const createDialogVisible = ref(false)
const createFormRef = ref(null)
const createForm = ref({
  name: '',
  description: '',
  steps: [{ name: '', agentRole: '', taskDescription: '' }]
})
const createRules = {
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }]
}

const addStep = () => {
  createForm.value.steps.push({ name: '', agentRole: '', taskDescription: '' })
}

const removeStep = (idx) => {
  createForm.value.steps.splice(idx, 1)
}

const showCreateTemplate = () => {
  createForm.value = {
    name: '',
    description: '',
    steps: [{ name: '', agentRole: '', taskDescription: '' }]
  }
  createDialogVisible.value = true
}

const getStatusType = (status) => {
  return { CREATED: 'info', RUNNING: 'warning', PAUSED: 'info', COMPLETED: 'success', FAILED: 'danger', CANCELLED: 'info' }[status] || 'info'
}

const getStatusLabel = (status) => {
  return { CREATED: '已创建', RUNNING: '运行中', PAUSED: '已暂停', COMPLETED: '已完成', FAILED: '失败', CANCELLED: '已取消' }[status] || status
}

const getProgress = (instance) => {
  if (!instance.stepExecutions) return 0
  const total = Object.keys(instance.stepExecutions).length
  if (total === 0) return 0
  const completed = Object.values(instance.stepExecutions).filter(s => s.status === 'COMPLETED').length
  return Math.round((completed / total) * 100)
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

const loadData = async () => {
  loading.value = true
  try {
    const [templatesData, instancesData] = await Promise.all([
      workflowApi.getTemplates(),
      workflowApi.getInstances()
    ])
    templates.value = templatesData || []
    instances.value = instancesData || []
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleStartWorkflow = () => {
  startForm.value = { templateId: '', projectId: '' }
  startDialogVisible.value = true
}

const handleStartWithTemplate = (template) => {
  startForm.value = { templateId: template.id, projectId: '' }
  startDialogVisible.value = true
}

const handleStartSubmit = async () => {
  try {
    await startFormRef.value.validate()
  } catch { return }
  submitting.value = true
  try {
    await workflowApi.start(startForm.value)
    ElMessage.success('工作流已启动')
    startDialogVisible.value = false
    activeTab.value = 'instances'
    loadData()
  } catch (error) {
    ElMessage.error('启动失败')
  } finally {
    submitting.value = false
  }
}

const handleCreateSubmit = async () => {
  try {
    await createFormRef.value.validate()
  } catch { return }
  submitting.value = true
  try {
    const id = 'custom-' + Date.now()
    const steps = createForm.value.steps.map((s, idx) => ({
      id: 'step-' + (idx + 1),
      name: s.name,
      agentRole: s.agentRole,
      taskDescription: s.taskDescription,
      dependencies: idx > 0 ? ['step-' + idx] : []
    }))
    await workflowApi.createTemplate({
      id,
      name: createForm.value.name,
      description: createForm.value.description,
      steps
    })
    ElMessage.success('模板已创建')
    createDialogVisible.value = false
    loadData()
  } catch (error) {
    ElMessage.error('创建失败')
  } finally {
    submitting.value = false
  }
}

const handleDeleteTemplate = async (template) => {
  try {
    await ElMessageBox.confirm(`确定删除模板 "${template.name}" 吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await workflowApi.deleteTemplate(template.id)
    ElMessage.success('模板已删除')
    loadData()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

const handlePause = async (instance) => {
  try {
    await workflowApi.pause(instance.id)
    ElMessage.success('工作流已暂停')
    loadData()
  } catch { ElMessage.error('暂停失败') }
}

const handleResume = async (instance) => {
  try {
    await workflowApi.resume(instance.id)
    ElMessage.success('工作流已恢复')
    loadData()
  } catch { ElMessage.error('恢复失败') }
}

const handleCancel = async (instance) => {
  try {
    await ElMessageBox.confirm('确定要取消此工作流吗？', '取消确认', {
      confirmButtonText: '取消工作流',
      cancelButtonText: '返回',
      type: 'warning'
    })
    await workflowApi.cancel(instance.id)
    ElMessage.success('工作流已取消')
    loadData()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('取消失败')
  }
}

const showAiGenerate = () => {
  aiDescription.value = ''
  aiResult.value = null
  aiGenerating.value = false
  aiDialogVisible.value = true
}

const handleAiGenerate = async () => {
  if (!aiDescription.value.trim()) {
    ElMessage.warning('请输入模板描述')
    return
  }
  aiGenerating.value = true
  aiResult.value = null
  try {
    const resp = await workflowApi.generateTemplate({ description: aiDescription.value })
    if (resp.status === 'success' && resp.template) {
      aiResult.value = resp.template
      ElMessage.success('模板生成成功，请确认后保存')
    } else {
      ElMessage.error(resp.message || '生成失败，请重试')
    }
  } catch (error) {
    ElMessage.error('AI 生成失败，请重试')
  } finally {
    aiGenerating.value = false
  }
}

const handleAiSave = async () => {
  if (!aiResult.value) return
  submitting.value = true
  try {
    const template = aiResult.value
    // 如果模板还没有注册（是纯预览），需要调用创建接口
    await workflowApi.createTemplate({
      id: template.id || ('ai-' + Date.now()),
      name: template.name,
      description: template.description,
      steps: template.steps || []
    })
    ElMessage.success('模板已保存')
    aiDialogVisible.value = false
    loadData()
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => { loadData() })
</script>

<style scoped>
.workflow-page {
  padding: 20px;
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

.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
}

.template-card {
  cursor: default;
}

.template-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.template-header h3 {
  margin: 0;
  font-size: 16px;
}

.template-desc {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin: 8px 0 12px;
}

.template-steps {
  margin-bottom: 12px;
}

.steps-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 6px;
}

.steps-flow {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.step-node {
  display: flex;
  align-items: center;
  gap: 4px;
}

.step-role {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

.step-arrow {
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.template-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid var(--el-border-color-lighter);
  padding-top: 12px;
}

.steps-editor {
  width: 100%;
}

.step-edit-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}

.ai-generate-section {
  margin-bottom: 16px;
}

.ai-hint {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin-bottom: 12px;
}

.ai-examples {
  margin-top: 12px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.examples-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.example-tag {
  cursor: pointer;
  font-size: 12px;
}

.example-tag:hover {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary);
}

.ai-preview {
  margin-top: 8px;
}

.ai-preview h4 {
  margin: 0 0 12px;
  font-size: 14px;
  color: var(--el-text-color-primary);
}

.preview-steps {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.preview-step {
  display: flex;
  align-items: center;
  gap: 8px;
}

.step-detail {
  font-size: 13px;
  color: var(--el-text-color-regular);
}
</style>
