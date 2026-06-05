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
            <el-button type="primary" @click="showVisualDesigner" v-permission="'workflow:manage'">
              <el-icon><Share /></el-icon> 可视化设计
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

        <!-- 工作流实例 -->
        <el-tab-pane label="工作流实例" name="instances">
          <div class="instance-toolbar">
            <el-radio-group v-model="instanceFilter" @click="loadInstances">
              <el-radio-button label="all">全部</el-radio-button>
              <el-radio-button label="running">运行中</el-radio-button>
              <el-radio-button label="completed">已完成</el-radio-button>
              <el-radio-button label="failed">失败</el-radio-button>
            </el-radio-group>
            <el-button @click="loadInstances" :icon="Refresh">刷新</el-button>
          </div>
          <el-table :data="filteredInstances" v-loading="loading" stripe @row-click="showInstanceDetail">
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
            <el-table-column label="创建时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="完成时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.completedAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" text @click.stop="showInstanceDetail(row)">
                  详情
                </el-button>
                <el-button type="warning" size="small" text @click.stop="handlePause(row)" v-if="row.status === 'RUNNING'" v-permission="'workflow:manage'">
                  暂停
                </el-button>
                <el-button type="success" size="small" text @click.stop="handleResume(row)" v-if="row.status === 'PAUSED'" v-permission="'workflow:manage'">
                  恢复
                </el-button>
                <el-button type="danger" size="small" text @click.stop="handleCancel(row)" v-if="['RUNNING', 'PAUSED'].includes(row.status)" v-permission="'workflow:manage'">
                  取消
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loading && filteredInstances.length === 0" description="暂无工作流实例" />
        </el-tab-pane>

        <!-- 待审批 -->
        <el-tab-pane label="待审批" name="approvals">
          <el-table :data="pendingApprovals" v-loading="loading" stripe>
            <el-table-column prop="instanceId" label="实例 ID" width="120" show-overflow-tooltip />
            <el-table-column prop="stepId" label="步骤" width="120" />
            <el-table-column label="请求时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.requestedAt) }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'PENDING' ? 'warning' : row.status === 'APPROVED' ? 'success' : 'danger'" size="small">
                  {{ row.status === 'PENDING' ? '待审批' : row.status === 'APPROVED' ? '已通过' : '已拒绝' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <template v-if="row.status === 'PENDING'">
                  <el-button type="success" size="small" @click="handleApprove(row)" v-permission="'workflow:manage'">
                    通过
                  </el-button>
                  <el-button type="danger" size="small" @click="handleReject(row)" v-permission="'workflow:manage'">
                    拒绝
                  </el-button>
                </template>
                <template v-else>
                  <span class="approval-result">
                    {{ row.approverName }} {{ row.status === 'APPROVED' ? '通过' : '拒绝' }}
                    <span v-if="row.comment">: {{ row.comment }}</span>
                  </span>
                </template>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loading && pendingApprovals.length === 0" description="暂无待审批项" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 实例详情对话框 -->
    <el-dialog v-model="instanceDetailVisible" title="工作流实例详情" width="900px" top="5vh">
      <template v-if="currentInstance">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="实例 ID">{{ currentInstance.instance?.id }}</el-descriptions-item>
          <el-descriptions-item label="模板">{{ getTemplateName(currentInstance.instance?.templateId) }}</el-descriptions-item>
          <el-descriptions-item label="项目">{{ currentInstance.instance?.projectId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentInstance.instance?.status)" size="small">
              {{ getStatusLabel(currentInstance.instance?.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(currentInstance.instance?.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ formatTime(currentInstance.instance?.completedAt) }}</el-descriptions-item>
          <el-descriptions-item label="错误信息" :span="2" v-if="currentInstance.instance?.errorMessage">
            <span class="error-text">{{ currentInstance.instance.errorMessage }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <el-divider />

        <!-- 步骤执行详情 -->
        <h4>步骤执行详情</h4>
        <el-table :data="currentInstance.steps || []" stripe size="small">
          <el-table-column prop="stepId" label="步骤 ID" width="120" />
          <el-table-column prop="agentRole" label="角色" width="120">
            <template #default="{ row }">
              {{ getRoleLabel(row.agentRole) }}
            </template>
          </el-table-column>
          <el-table-column prop="agentId" label="Agent" width="150" show-overflow-tooltip />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="getStepStatusType(row.status)" size="small">
                {{ getStepStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="重试" width="80">
            <template #default="{ row }">
              {{ row.retryCount || 0 }}/{{ row.maxRetries || 3 }}
            </template>
          </el-table-column>
          <el-table-column label="开始时间" width="160">
            <template #default="{ row }">
              {{ formatTime(row.startedAt) }}
            </template>
          </el-table-column>
          <el-table-column label="完成时间" width="160">
            <template #default="{ row }">
              {{ formatTime(row.completedAt) }}
            </template>
          </el-table-column>
          <el-table-column label="错误" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.errorMessage" class="error-text">{{ row.errorMessage }}</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
        </el-table>

        <el-divider />

        <!-- 审计日志 -->
        <h4>审计日志</h4>
        <el-timeline>
          <el-timeline-item
            v-for="log in (currentInstance.auditLogs || []).slice(0, 20)"
            :key="log.id"
            :timestamp="formatTime(log.createdAt)"
            :type="getAuditLogType(log.action)"
            placement="top"
          >
            <div class="audit-log-item">
              <el-tag :type="getAuditLogType(log.action)" size="small" effect="plain">
                {{ getAuditLogLabel(log.action) }}
              </el-tag>
              <span v-if="log.stepId" class="audit-step">步骤: {{ log.stepId }}</span>
              <span class="audit-actor">{{ log.actorType }}: {{ log.actorName || log.actorId }}</span>
            </div>
          </el-timeline-item>
        </el-timeline>
      </template>
    </el-dialog>

    <!-- 可视化设计器对话框 -->
    <el-dialog v-model="designerVisible" title="可视化工作流设计器" width="95%" top="3vh" fullscreen>
      <div class="visual-designer">
        <div class="designer-toolbar">
          <el-input v-model="designerForm.name" placeholder="模板名称" style="width: 200px" />
          <el-input v-model="designerForm.description" placeholder="模板描述" style="width: 300px" />
          <el-button type="primary" @click="addDesignerStep">添加步骤</el-button>
          <el-button type="success" @click="saveDesignedTemplate" :loading="submitting">保存模板</el-button>
        </div>

        <div class="designer-canvas">
          <!-- 可视化流程图 -->
          <div class="flow-canvas">
            <div v-for="(step, idx) in designerForm.steps" :key="idx" class="flow-step-wrapper">
              <div class="flow-step" :class="{ 'is-parallel': step.parallel, 'needs-approval': step.requiresApproval }">
                <div class="step-header">
                  <el-input v-model="step.name" placeholder="步骤名称" size="small" style="width: 120px" />
                  <el-button type="danger" text size="small" @click="removeDesignerStep(idx)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
                <el-select v-model="step.agentRole" placeholder="选择角色" size="small" style="width: 100%">
                  <el-option v-for="role in agentRoles" :key="role.value" :label="role.label" :value="role.value" />
                </el-select>
                <el-input v-model="step.taskDescription" placeholder="任务描述" type="textarea" :rows="2" size="small" />
                <div class="step-options">
                  <el-checkbox v-model="step.parallel">并行执行</el-checkbox>
                  <el-checkbox v-model="step.requiresApproval">需要审批</el-checkbox>
                </div>
                <div class="step-dependencies">
                  <span class="dep-label">依赖：</span>
                  <el-select v-model="step.dependencies" multiple placeholder="选择依赖步骤" size="small" style="flex: 1">
                    <el-option
                      v-for="(s, i) in designerForm.steps.filter((_, j) => j < idx)"
                      :key="s.id || i"
                      :label="s.name || '步骤 ' + (i + 1)"
                      :value="s.id || 'step-' + (i + 1)"
                    />
                  </el-select>
                </div>
              </div>
              <div v-if="idx < designerForm.steps.length - 1" class="flow-arrow">
                <el-icon><Bottom /></el-icon>
              </div>
            </div>
          </div>

          <!-- JSON预览 -->
          <div class="json-preview">
            <h4>JSON 预览</h4>
            <pre>{{ JSON.stringify(designerForm, null, 2) }}</pre>
          </div>
        </div>
      </div>
    </el-dialog>

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

    <!-- 审批对话框 -->
    <el-dialog v-model="approvalDialogVisible" :title="approvalAction === 'approve' ? '审批通过' : '审批拒绝'" width="400px">
      <el-form label-width="80px">
        <el-form-item label="实例 ID">
          <span>{{ currentApproval?.instanceId }}</span>
        </el-form-item>
        <el-form-item label="步骤">
          <span>{{ currentApproval?.stepId }}</span>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="approvalComment" type="textarea" :rows="3" placeholder="请输入审批意见（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approvalDialogVisible = false">取消</el-button>
        <el-button :type="approvalAction === 'approve' ? 'success' : 'danger'" @click="submitApproval" :loading="submitting">
          {{ approvalAction === 'approve' ? '确认通过' : '确认拒绝' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { workflowApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Bottom } from '@element-plus/icons-vue'
import ProjectSelector from '@/components/ProjectSelector.vue'

const loading = ref(false)
const submitting = ref(false)
const activeTab = ref('templates')
const templates = ref([])
const instances = ref([])
const pendingApprovals = ref([])
const instanceFilter = ref('all')

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

// 可视化设计器
const designerVisible = ref(false)
const designerForm = ref({ name: '', description: '', steps: [] })

// 实例详情
const instanceDetailVisible = ref(false)
const currentInstance = ref(null)

// 审批
const approvalDialogVisible = ref(false)
const approvalAction = ref('approve')
const currentApproval = ref(null)
const approvalComment = ref('')

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

const filteredInstances = computed(() => {
  if (instanceFilter.value === 'all') return instances.value
  const statusMap = { running: 'RUNNING', completed: 'COMPLETED', failed: 'FAILED' }
  return instances.value.filter(i => i.status === statusMap[instanceFilter.value])
})

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

const getStepStatusType = (status) => {
  return { PENDING: 'info', WAITING_DEPENDENCIES: 'warning', READY: 'info', RUNNING: 'warning', COMPLETED: 'success', FAILED: 'danger', SKIPPED: 'info' }[status] || 'info'
}

const getStepStatusLabel = (status) => {
  return { PENDING: '待执行', WAITING_DEPENDENCIES: '等待依赖', READY: '就绪', RUNNING: '运行中', COMPLETED: '已完成', FAILED: '失败', SKIPPED: '已跳过' }[status] || status
}

const getAuditLogType = (action) => {
  if (action?.includes('COMPLETED') || action?.includes('APPROVED')) return 'success'
  if (action?.includes('FAILED') || action?.includes('REJECTED')) return 'danger'
  if (action?.includes('STARTED') || action?.includes('REQUESTED')) return 'warning'
  return 'info'
}

const getAuditLogLabel = (action) => {
  const map = {
    'INSTANCE_CREATED': '实例创建',
    'INSTANCE_STARTED': '实例启动',
    'INSTANCE_PAUSED': '实例暂停',
    'INSTANCE_RESUMED': '实例恢复',
    'INSTANCE_COMPLETED': '实例完成',
    'INSTANCE_FAILED': '实例失败',
    'INSTANCE_CANCELLED': '实例取消',
    'STEP_STARTED': '步骤开始',
    'STEP_COMPLETED': '步骤完成',
    'STEP_FAILED': '步骤失败',
    'STEP_RETRIED': '步骤重试',
    'STEP_TIMEOUT': '步骤超时',
    'APPROVAL_REQUESTED': '审批请求',
    'APPROVAL_APPROVED': '审批通过',
    'APPROVAL_REJECTED': '审批拒绝'
  }
  return map[action] || action
}

const formatTime = (time) => {
  if (!time) return '-'
  if (Array.isArray(time)) {
    // LocalDateTime数组格式 [year, month, day, hour, minute, second]
    const [y, m, d, h, min, s] = time
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')} ${String(h).padStart(2, '0')}:${String(min).padStart(2, '0')}:${String(s || 0).padStart(2, '0')}`
  }
  return new Date(time).toLocaleString('zh-CN')
}

const loadData = async () => {
  loading.value = true
  try {
    const [templatesData, instancesData, approvalsData] = await Promise.all([
      workflowApi.getTemplates(),
      workflowApi.getAllInstances(),
      workflowApi.getPendingApprovals().catch(() => [])
    ])
    templates.value = templatesData || []
    instances.value = instancesData || []
    pendingApprovals.value = approvalsData || []
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const loadInstances = async () => {
  try {
    const data = await workflowApi.getAllInstances()
    instances.value = data || []
  } catch { /* ignore */ }
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

// 实例详情
const showInstanceDetail = async (row) => {
  try {
    const detail = await workflowApi.getInstanceDetail(row.id)
    currentInstance.value = detail
    instanceDetailVisible.value = true
  } catch {
    ElMessage.error('加载实例详情失败')
  }
}

// 可视化设计器
const showVisualDesigner = () => {
  designerForm.value = {
    name: '',
    description: '',
    steps: [
      { id: 'step-1', name: '步骤1', agentRole: 'system-planner', taskDescription: '', dependencies: [], parallel: false, requiresApproval: false }
    ]
  }
  designerVisible.value = true
}

const addDesignerStep = () => {
  const idx = designerForm.value.steps.length + 1
  designerForm.value.steps.push({
    id: 'step-' + idx,
    name: '步骤' + idx,
    agentRole: 'server-dev',
    taskDescription: '',
    dependencies: idx > 1 ? ['step-' + (idx - 1)] : [],
    parallel: false,
    requiresApproval: false
  })
}

const removeDesignerStep = (idx) => {
  designerForm.value.steps.splice(idx, 1)
  // 更新依赖引用
  designerForm.value.steps.forEach((step, i) => {
    step.id = 'step-' + (i + 1)
    if (i === 0) {
      step.dependencies = []
    } else if (step.dependencies.length === 0) {
      step.dependencies = ['step-' + i]
    }
  })
}

const saveDesignedTemplate = async () => {
  if (!designerForm.value.name) {
    ElMessage.warning('请输入模板名称')
    return
  }
  submitting.value = true
  try {
    const id = 'visual-' + Date.now()
    await workflowApi.createTemplate({
      id,
      name: designerForm.value.name,
      description: designerForm.value.description,
      steps: designerForm.value.steps
    })
    ElMessage.success('模板已保存')
    designerVisible.value = false
    loadData()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    submitting.value = false
  }
}

// 审批
const handleApprove = (approval) => {
  currentApproval.value = approval
  approvalAction.value = 'approve'
  approvalComment.value = ''
  approvalDialogVisible.value = true
}

const handleReject = (approval) => {
  currentApproval.value = approval
  approvalAction.value = 'reject'
  approvalComment.value = ''
  approvalDialogVisible.value = true
}

const submitApproval = async () => {
  submitting.value = true
  try {
    if (approvalAction.value === 'approve') {
      await workflowApi.approveStep(currentApproval.value.instanceId, currentApproval.value.stepId, {
        comment: approvalComment.value
      })
      ElMessage.success('审批通过')
    } else {
      await workflowApi.rejectStep(currentApproval.value.instanceId, currentApproval.value.stepId, {
        comment: approvalComment.value
      })
      ElMessage.success('已拒绝')
    }
    approvalDialogVisible.value = false
    loadData()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
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

.instance-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.approval-result {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.error-text {
  color: var(--el-color-danger);
  font-size: 12px;
}

.audit-log-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.audit-step {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.audit-actor {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

/* 可视化设计器 */
.visual-designer {
  height: calc(100vh - 150px);
  display: flex;
  flex-direction: column;
}

.designer-toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
}

.designer-canvas {
  flex: 1;
  display: flex;
  gap: 20px;
  overflow: auto;
}

.flow-canvas {
  flex: 1;
  padding: 20px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  overflow-y: auto;
}

.flow-step-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.flow-step {
  width: 350px;
  padding: 16px;
  background: white;
  border: 2px solid var(--el-border-color);
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.flow-step.is-parallel {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.flow-step.needs-approval {
  border-color: var(--el-color-warning);
  background: var(--el-color-warning-light-9);
}

.step-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.step-options {
  display: flex;
  gap: 16px;
  margin-top: 8px;
}

.step-dependencies {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}

.dep-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}

.flow-arrow {
  padding: 8px 0;
  color: var(--el-text-color-secondary);
  font-size: 20px;
}

.json-preview {
  width: 350px;
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  overflow: auto;
}

.json-preview h4 {
  margin: 0 0 12px;
  font-size: 14px;
}

.json-preview pre {
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
}

/* AI 生成 */
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
