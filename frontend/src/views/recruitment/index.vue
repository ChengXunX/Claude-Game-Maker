<template>
  <div class="recruitment-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>Agent 招聘</span>
          <el-button type="primary" @click="handleRecruit" v-permission="'agents:manage'">
            <el-icon><UserPlus /></el-icon> 发起招聘
          </el-button>
        </div>
      </template>

      <!-- 统计卡片 -->
      <el-row :gutter="16" class="stat-cards">
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value">{{ stats.totalAgents || 0 }}</div>
              <div class="stat-label">当前 Agent 数</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value">{{ stats.pendingRequests || 0 }}</div>
              <div class="stat-label">待审批申请</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value">{{ stats.approvedRequests || 0 }}</div>
              <div class="stat-label">已通过申请</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value">{{ stats.rejectedRequests || 0 }}</div>
              <div class="stat-label">已拒绝申请</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 可招聘角色列表 -->
      <h3 class="section-title">可招聘角色</h3>
      <el-table :data="roles" v-loading="loading" stripe>
        <el-table-column prop="role" label="角色标识" width="140" />
        <el-table-column prop="name" label="名称" width="120" />
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.preset ? 'primary' : 'success'" size="small">
              {{ row.preset ? '预设' : '自定义' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="能力" min-width="150">
          <template #default="{ row }">
            <el-tag v-for="cap in (row.capabilities || []).slice(0, 2)" :key="cap" size="small" class="cap-tag">
              {{ cap }}
            </el-tag>
            <el-tag v-if="(row.capabilities || []).length > 2" size="small" type="info">
              +{{ row.capabilities.length - 2 }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleApply(row)" v-permission="'agents:manage'">
              招聘
            </el-button>
            <el-button type="info" size="small" text @click="handleViewRoleDetail(row)">
              角色详情
            </el-button>
            <el-button v-if="!row.preset" type="warning" size="small" text @click="handleEditRole(row)" v-permission="'agents:manage'">
              编辑
            </el-button>
            <el-button v-if="!row.preset" type="danger" size="small" text @click="handleDeleteRole(row)" v-permission="'agents:manage'">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="add-role-section">
        <el-button type="success" @click="handleAddRole" v-permission="'agents:manage'">
          <el-icon><Plus /></el-icon> 添加自定义角色
        </el-button>
        <el-button type="primary" @click="handleDirectCustomRecruit" v-permission="'agents:manage'">
          <el-icon><UserFilled /></el-icon> 直接招聘自定义角色
        </el-button>
      </div>
    </el-card>

    <!-- 审批列表 -->
    <el-card class="mt-4">
      <template #header>
        <div class="card-header">
          <span>审批申请</span>
          <el-button type="primary" text @click="router.push('/interventions')">查看全部审批</el-button>
        </div>
      </template>

      <el-table :data="approvalRequests" v-loading="approvalLoading" stripe>
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getApprovalTypeTag(row.requestType)" size="small">
              {{ getApprovalTypeLabel(row.requestType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getApprovalStatusTag(row.status)" size="small">
              {{ getApprovalStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requesterId" label="发起者" width="120" show-overflow-tooltip />
        <el-table-column label="时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="router.push('/interventions')">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!approvalLoading && approvalRequests.length === 0" description="暂无审批申请" :image-size="60" />
    </el-card>

    <!-- 招聘申请对话框 -->
    <el-dialog v-model="applyDialogVisible" title="发起招聘申请" width="500px">
      <el-form ref="applyFormRef" :model="applyForm" :rules="applyRules" label-width="100px">
        <el-form-item label="角色">
          <el-input :value="applyForm.roleName" disabled />
        </el-form-item>
        <el-form-item label="Agent 名称" prop="agentName">
          <el-input v-model="applyForm.agentName" placeholder="为新 Agent 取个名字" />
        </el-form-item>
        <el-form-item label="工作目录" prop="workDir">
          <el-input v-model="applyForm.workDir" placeholder="Agent 的工作目录路径" />
        </el-form-item>
        <el-form-item label="备注" prop="reason">
          <el-input v-model="applyForm.reason" type="textarea" :rows="3" placeholder="招聘原因（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleApplySubmit" :loading="submitting">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 自定义角色对话框 -->
    <el-dialog v-model="roleDialogVisible" :title="isEditRole ? '编辑角色' : '添加自定义角色'" width="600px">
      <el-form ref="roleFormRef" :model="roleForm" :rules="roleRules" label-width="100px">
        <el-form-item label="角色标识" prop="role">
          <el-input v-model="roleForm.role" placeholder="如：audio-dev" :disabled="isEditRole" />
          <div class="form-tip">唯一标识，只能使用小写字母、数字和连字符</div>
        </el-form-item>
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="roleForm.name" placeholder="如：音频开发" />
        </el-form-item>
        <el-form-item label="角色描述" prop="description">
          <el-input v-model="roleForm.description" type="textarea" :rows="3" placeholder="角色职责描述" />
        </el-form-item>
        <el-form-item label="默认能力">
          <el-select v-model="roleForm.capabilities" multiple filterable allow-create placeholder="输入能力名称后回车">
            <el-option v-for="cap in commonCapabilities" :key="cap" :label="cap" :value="cap" />
          </el-select>
        </el-form-item>
        <el-form-item label="文件类型">
          <el-select v-model="roleForm.fileTypes" multiple filterable allow-create placeholder="输入文件类型后回车">
            <el-option v-for="ft in commonFileTypes" :key="ft" :label="ft" :value="ft" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRoleSubmit" :loading="submitting">
          {{ isEditRole ? '保存修改' : '创建角色' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 直接招聘自定义角色对话框 -->
    <el-dialog v-model="customRecruitDialogVisible" title="直接招聘自定义角色" width="650px">
      <el-form ref="customRecruitFormRef" :model="customRecruitForm" :rules="customRecruitRules" label-width="100px">
        <el-alert type="info" :closable="false" style="margin-bottom: 16px;">
          制作人可以根据项目需求直接招聘任意角色，系统会自动创建角色模板。
        </el-alert>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="角色标识" prop="role">
              <el-input v-model="customRecruitForm.role" placeholder="如：audio-dev" />
              <div class="form-tip">唯一标识，只能使用小写字母、数字和连字符</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色名称" prop="roleName">
              <el-input v-model="customRecruitForm.roleName" placeholder="如：音频开发" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="Agent 名称" prop="name">
          <el-input v-model="customRecruitForm.name" placeholder="为新 Agent 取个名字" />
        </el-form-item>
        <el-form-item label="角色描述" prop="description">
          <el-input v-model="customRecruitForm.description" type="textarea" :rows="2" placeholder="角色职责描述" />
        </el-form-item>
        <el-form-item label="工作目录" prop="workDir">
          <el-input v-model="customRecruitForm.workDir" placeholder="Agent 的工作目录路径" />
        </el-form-item>
        <el-form-item label="能力标签">
          <el-select v-model="customRecruitForm.capabilities" multiple filterable allow-create placeholder="输入能力名称后回车">
            <el-option v-for="cap in commonCapabilities" :key="cap" :label="cap" :value="cap" />
          </el-select>
        </el-form-item>
        <el-form-item label="文件类型">
          <el-select v-model="customRecruitForm.fileTypes" multiple filterable allow-create placeholder="输入文件类型后回车">
            <el-option v-for="ft in commonFileTypes" :key="ft" :label="ft" :value="ft" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="customRecruitDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCustomRecruitSubmit" :loading="submitting">
          立即招聘
        </el-button>
      </template>
    </el-dialog>

    <!-- 角色详情弹窗 -->
    <el-dialog v-model="roleDetailVisible" :title="`角色详情 — ${roleDetailData.name || roleDetailData.role}`" width="750px" top="5vh">
      <template v-if="roleDetailData.role">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="角色标识">{{ roleDetailData.role }}</el-descriptions-item>
          <el-descriptions-item label="名称">{{ roleDetailData.name }}</el-descriptions-item>
          <el-descriptions-item label="通知目标">{{ roleDetailData.notifyTargets || 'producer' }}</el-descriptions-item>
          <el-descriptions-item label="审查者">{{ roleDetailData.reviewer || '无' }}</el-descriptions-item>
          <el-descriptions-item label="数据来源">
            <el-tag :type="roleDetailData.source === 'database' ? 'success' : 'info'" size="small">
              {{ roleDetailData.source === 'database' ? '数据库（已优化）' : '文件（默认）' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="版本">
            <el-tag v-if="evoMeta.version > 0" type="warning" size="small">
              v{{ evoMeta.version }}
              <span v-if="evoMeta.source"> · {{ { manual: '人工', ai: 'AI', evolution: '自进化' }[evoMeta.source] || evoMeta.source }}</span>
            </el-tag>
            <el-tag v-else type="info" size="small">v0（初始版本）</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="上次进化" :span="2" v-if="evoMeta.lastEvolutionAt">
            {{ new Date(evoMeta.lastEvolutionAt).toLocaleString('zh-CN') }}
          </el-descriptions-item>
        </el-descriptions>

        <el-divider>角色提示词</el-divider>
        <div class="role-prompt-viewer">
          <pre>{{ roleDetailData.prompt || '暂无提示词' }}</pre>
        </div>

        <div class="role-detail-actions" v-permission="'agents:manage'">
          <el-button type="primary" @click="handleEvolvePrompt" :loading="evolvingPrompt">
            <el-icon><MagicStick /></el-icon> AI 进化
          </el-button>
          <el-button type="warning" @click="handleEditPrompt">
            <el-icon><Edit /></el-icon> 编辑提示词
          </el-button>
          <el-button @click="handleResetPrompt" :loading="resettingPrompt">
            <el-icon><RefreshLeft /></el-icon> 重置为默认
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 编辑提示词弹窗 -->
    <el-dialog v-model="editPromptVisible" title="编辑角色提示词" width="800px" top="3vh">
      <el-form label-width="80px">
        <el-form-item label="通知目标">
          <el-input v-model="editPromptForm.notifyTargets" placeholder="如：producer,git-commit" />
        </el-form-item>
        <el-form-item label="审查者">
          <el-input v-model="editPromptForm.reviewer" placeholder="如：git-commit" />
        </el-form-item>
        <el-form-item label="提示词">
          <MarkdownEditor v-model="editPromptForm.prompt" :rows="20" placeholder="角色系统提示词" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editPromptVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSavePrompt" :loading="savingPrompt">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * Agent 招聘页面
 * 制作人 Agent 招聘新 Agent（员工）
 *
 * 操作维度：系统级
 * 权限要求：agents:manage
 */
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { recruitmentApi, agentApi, approvalApi } from '@/api'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as Icons from '@element-plus/icons-vue'

const { UserPlus, Plus, UserFilled } = Icons

const router = useRouter()
const loading = ref(false)
const roles = ref([])
const stats = ref({})

// 角色详情
const roleDetailVisible = ref(false)
const roleDetailData = ref({})

// 编辑提示词
const editPromptVisible = ref(false)
const editPromptForm = ref({ prompt: '', notifyTargets: '', reviewer: '' })
const savingPrompt = ref(false)
const resettingPrompt = ref(false)
const evolvingPrompt = ref(false)
const evoMeta = ref({})

// 审批数据
const approvalLoading = ref(false)
const approvalRequests = ref([])

/** 申请对话框 */
const applyDialogVisible = ref(false)
const applyFormRef = ref(null)
const submitting = ref(false)
const applyForm = ref({
  role: '',
  roleName: '',
  agentName: '',
  workDir: '',
  reason: ''
})
const applyRules = {
  agentName: [{ required: true, message: '请输入 Agent 名称', trigger: 'blur' }],
  workDir: [{ required: true, message: '请输入工作目录', trigger: 'blur' }]
}

/** 自定义角色对话框 */
const roleDialogVisible = ref(false)
const roleFormRef = ref(null)
const isEditRole = ref(false)
const roleForm = ref({
  role: '',
  name: '',
  description: '',
  capabilities: [],
  fileTypes: []
})
const roleRules = {
  role: [
    { required: true, message: '请输入角色标识', trigger: 'blur' },
    { pattern: /^[a-z0-9-]+$/, message: '只能使用小写字母、数字和连字符', trigger: 'blur' }
  ],
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

/** 直接招聘自定义角色对话框 */
const customRecruitDialogVisible = ref(false)
const customRecruitFormRef = ref(null)
const customRecruitForm = ref({
  role: '',
  roleName: '',
  name: '',
  description: '',
  workDir: '',
  capabilities: [],
  fileTypes: []
})
const customRecruitRules = {
  role: [
    { required: true, message: '请输入角色标识', trigger: 'blur' },
    { pattern: /^[a-z0-9-]+$/, message: '只能使用小写字母、数字和连字符', trigger: 'blur' }
  ],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  name: [{ required: true, message: '请输入 Agent 名称', trigger: 'blur' }],
  workDir: [{ required: true, message: '请输入工作目录', trigger: 'blur' }]
}

/** 常用能力列表 */
const commonCapabilities = [
  'backend_development', 'frontend_development', 'api_design', 'database_design',
  'game_design', 'system_design', 'documentation', 'testing', 'bug_reporting',
  'ui_design', 'css_styling', 'responsive_design', 'performance_optimization',
  'version_control', 'git_operations', 'numerical_design', 'balance_tuning',
  'audio_design', '3d_modeling', 'animation', 'shader_programming'
]

/** 常用文件类型 */
const commonFileTypes = [
  'java', 'py', 'go', 'js', 'ts', 'html', 'css', 'lua', 'sql',
  'json', 'yaml', 'xml', 'md', 'txt', 'xlsx', 'csv',
  'png', 'jpg', 'svg', 'mp3', 'wav', 'fbx', 'obj'
]

/** 加载可招聘角色 */
const loadRoles = async () => {
  loading.value = true
  try {
    const data = await recruitmentApi.getRoles()
    roles.value = data || []
  } catch (error) {
    ElMessage.error('加载角色列表失败')
  } finally {
    loading.value = false
  }
}

/** 加载统计 */
const loadStats = async () => {
  try {
    const data = await recruitmentApi.getStats()
    stats.value = data || {}
  } catch (error) {
    // 忽略
  }
}

/** 加载审批列表 */
const loadApprovals = async () => {
  approvalLoading.value = true
  try {
    const data = await approvalApi.getAll()
    approvalRequests.value = (data || []).slice(0, 10) // 最多显示10条
    // 更新统计中的审批数据
    const pending = (data || []).filter(r => r.status === 'PENDING').length
    const approved = (data || []).filter(r => r.status === 'APPROVED').length
    const rejected = (data || []).filter(r => r.status === 'REJECTED').length
    stats.value.pendingRequests = pending
    stats.value.approvedRequests = approved
    stats.value.rejectedRequests = rejected
  } catch (error) {
    // 忽略
  } finally {
    approvalLoading.value = false
  }
}

/** 审批类型标签 */
const getApprovalTypeTag = (type) => {
  const map = { 'CREATE_AGENT': 'primary', 'DELETE_AGENT': 'danger', 'DISMISS_AGENT': 'danger', 'ASSIGN_API': 'warning', 'EMAIL_CHANGE': 'info' }
  return map[type] || 'info'
}

const getApprovalTypeLabel = (type) => {
  const map = { 'CREATE_AGENT': '招聘', 'DELETE_AGENT': '删除', 'DISMISS_AGENT': '解雇', 'ASSIGN_API': 'API分配', 'EMAIL_CHANGE': '邮箱变更' }
  return map[type] || type
}

const getApprovalStatusTag = (status) => {
  const map = { 'PENDING': 'warning', 'APPROVED': 'success', 'REJECTED': 'danger', 'EXPIRED': 'info' }
  return map[status] || 'info'
}

const getApprovalStatusLabel = (status) => {
  const map = { 'PENDING': '待审批', 'APPROVED': '已通过', 'REJECTED': '已拒绝', 'EXPIRED': '已过期' }
  return map[status] || status
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

/** 点击招聘按钮 */
const handleRecruit = () => {
  // 默认选择第一个角色
  if (roles.value.length > 0) {
    handleApply(roles.value[0])
  }
}

/** 选择角色招聘 */
const handleApply = (role) => {
  applyForm.value = {
    role: role.role,
    roleName: role.name,
    agentName: '',
    workDir: '',
    reason: ''
  }
  applyDialogVisible.value = true
}

/** 提交申请 */
const handleApplySubmit = async () => {
  try {
    await applyFormRef.value.validate()
    submitting.value = true

    await recruitmentApi.createRequest({
      role: applyForm.value.role,
      agentName: applyForm.value.agentName,
      workDir: applyForm.value.workDir,
      reason: applyForm.value.reason
    })

    ElMessage.success('招聘申请已提交')
    applyDialogVisible.value = false
    loadStats()
  } catch (error) {
    if (error !== false) {
      ElMessage.error('提交失败')
    }
  } finally {
    submitting.value = false
  }
}

/** 添加自定义角色 */
const handleAddRole = () => {
  isEditRole.value = false
  roleForm.value = {
    role: '',
    name: '',
    description: '',
    capabilities: [],
    fileTypes: []
  }
  roleDialogVisible.value = true
}

/** 编辑自定义角色 */
const handleEditRole = (role) => {
  isEditRole.value = true
  roleForm.value = {
    role: role.role,
    name: role.name,
    description: role.description,
    capabilities: [...(role.capabilities || [])],
    fileTypes: [...(role.fileTypes || [])]
  }
  roleDialogVisible.value = true
}

/** 删除自定义角色 */
const handleDeleteRole = async (role) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除角色 "${role.name}" 吗？`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )

    await recruitmentApi.deleteCustomTemplate(role.role)
    ElMessage.success('角色已删除')
    loadRoles()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

/** 提交角色表单 */
const handleRoleSubmit = async () => {
  try {
    await roleFormRef.value.validate()
    submitting.value = true

    if (isEditRole.value) {
      await recruitmentApi.updateCustomTemplate(roleForm.value.role, roleForm.value)
      ElMessage.success('角色更新成功')
    } else {
      await recruitmentApi.createCustomTemplate(roleForm.value)
      ElMessage.success('角色创建成功')
    }

    roleDialogVisible.value = false
    loadRoles()
  } catch (error) {
    if (error !== false) {
      ElMessage.error('保存失败：' + (error.message || '未知错误'))
    }
  } finally {
    submitting.value = false
  }
}

/** 查看角色详情 */
const handleViewRoleDetail = async (role) => {
  try {
    const [detail, meta] = await Promise.all([
      recruitmentApi.getRoleDetail(role.role),
      recruitmentApi.getEvolutionMeta(role.role).catch(() => ({}))
    ])
    roleDetailData.value = { ...detail, source: detail.source || 'file' }
    evoMeta.value = meta || {}
    roleDetailVisible.value = true
  } catch {
    ElMessage.error('加载角色详情失败')
  }
}

/** AI 进化提示词 */
const handleEvolvePrompt = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要让 AI 进化「${roleDetailData.value.name}」的角色提示词吗？AI 将分析当前提示词并生成更优版本。`,
      'AI 进化确认',
      { confirmButtonText: '开始进化', cancelButtonText: '取消', type: 'info' }
    )
    evolvingPrompt.value = true
    const result = await recruitmentApi.evolveRolePrompt(roleDetailData.value.role)
    if (result.success) {
      ElMessage.success(result.message)
      // 刷新详情
      const [detail, meta] = await Promise.all([
        recruitmentApi.getRoleDetail(roleDetailData.value.role),
        recruitmentApi.getEvolutionMeta(roleDetailData.value.role).catch(() => ({}))
      ])
      roleDetailData.value = detail
      evoMeta.value = meta || {}
    } else {
      ElMessage.error(result.message)
    }
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('进化失败')
  } finally {
    evolvingPrompt.value = false
  }
}

/** 编辑提示词 */
const handleEditPrompt = () => {
  editPromptForm.value = {
    prompt: roleDetailData.value.prompt || '',
    notifyTargets: roleDetailData.value.notifyTargets || 'producer',
    reviewer: roleDetailData.value.reviewer || ''
  }
  editPromptVisible.value = true
}

/** 保存提示词 */
const handleSavePrompt = async () => {
  savingPrompt.value = true
  try {
    await recruitmentApi.updateRolePrompt(roleDetailData.value.role, editPromptForm.value)
    ElMessage.success('提示词已保存')
    editPromptVisible.value = false
    // 刷新详情
    const [detail, meta] = await Promise.all([
      recruitmentApi.getRoleDetail(roleDetailData.value.role),
      recruitmentApi.getEvolutionMeta(roleDetailData.value.role).catch(() => ({}))
    ])
    roleDetailData.value = detail
    evoMeta.value = meta || {}
  } catch {
    ElMessage.error('保存失败')
  } finally {
    savingPrompt.value = false
  }
}

/** 重置提示词 */
const handleResetPrompt = async () => {
  try {
    await ElMessageBox.confirm('确定重置为文件默认版本？数据库中的自定义版本将被清除。', '确认重置', {
      confirmButtonText: '确定重置',
      cancelButtonText: '取消',
      type: 'warning'
    })
    resettingPrompt.value = true
    const result = await recruitmentApi.resetRolePrompt(roleDetailData.value.role)
    if (result.success) {
      ElMessage.success('已重置为默认版本')
      const detail = await recruitmentApi.getRoleDetail(roleDetailData.value.role)
      roleDetailData.value = detail
    } else {
      ElMessage.error(result.message)
    }
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('重置失败')
  } finally {
    resettingPrompt.value = false
  }
}

/** 打开直接招聘自定义角色对话框 */
const handleDirectCustomRecruit = () => {
  customRecruitForm.value = {
    role: '',
    roleName: '',
    name: '',
    description: '',
    workDir: '',
    capabilities: [],
    fileTypes: []
  }
  customRecruitDialogVisible.value = true
}

/** 提交直接招聘自定义角色 */
const handleCustomRecruitSubmit = async () => {
  try {
    await customRecruitFormRef.value.validate()
    submitting.value = true

    // 从已有 Agent 列表中查找制作人
    let producerId = null
    try {
      const agents = await agentApi.getAll()
      const producer = (agents || []).find(a => a.role === 'producer')
      if (producer) {
        producerId = producer.id
      }
    } catch (e) {
      console.warn('获取 Agent 列表失败:', e)
    }

    if (!producerId) {
      ElMessage.error('未找到可用的制作人 Agent，请先启动项目')
      return
    }

    const result = await recruitmentApi.recruitCustom(producerId, customRecruitForm.value)

    ElMessage.success(`自定义角色 "${customRecruitForm.value.roleName}" 招聘成功！Agent: ${result.agentName}`)
    customRecruitDialogVisible.value = false
    loadRoles()
    loadStats()
  } catch (error) {
    if (error !== false) {
      ElMessage.error('招聘失败：' + (error.message || '未知错误'))
    }
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadRoles()
  loadStats()
  loadApprovals()
})
</script>

<style scoped>
.recruitment-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-cards {
  margin-bottom: 24px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}

.stat-label {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.section-title {
  margin: 16px 0 12px;
  font-size: 16px;
}

.cap-tag {
  margin-right: 4px;
  margin-bottom: 4px;
}

.add-role-section {
  margin-top: 20px;
  text-align: center;
}

.mt-4 {
  margin-top: 16px;
}

.form-tip {
  color: #909399;
  font-size: 12px;
  margin-top: 5px;
}

/* 角色提示词查看器 */
.role-prompt-viewer {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 16px;
  max-height: 50vh;
  overflow-y: auto;
}

.role-prompt-viewer pre {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: 'Courier New', monospace;
}

.role-detail-actions {
  margin-top: 16px;
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}
</style>
