<template>
  <div class="agents-page">
    <!-- 项目选择区 -->
    <el-card class="project-selector-card">
      <div class="project-selector-wrapper">
        <span class="selector-label">选择项目：</span>
        <ProjectSelector
          v-model="selectedProjectId"
          @change="handleProjectChange"
          width="300px"
        />
        <el-tag v-if="selectedProject" type="info" size="small" class="project-tag">
          工作目录: {{ selectedProject.workDir }}
        </el-tag>
      </div>
    </el-card>

    <!-- 统计概览 -->
    <el-row :gutter="16" class="stat-cards" v-if="selectedProjectId">
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-primary-light-9)">
            <el-icon :size="24" color="var(--el-color-primary)"><User /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ agents.length }}</div>
            <div class="stat-label">Agent 总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-success-light-9)">
            <el-icon :size="24" color="var(--el-color-success)"><CircleCheck /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ runningCount }}</div>
            <div class="stat-label">运行中</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-warning-light-9)">
            <el-icon :size="24" color="var(--el-color-warning)"><Loading /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ busyCount }}</div>
            <div class="stat-label">忙碌中</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-info-light-9)">
            <el-icon :size="24" color="var(--el-color-info)"><Coffee /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ idleCount }}</div>
            <div class="stat-label">空闲中</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Agent 列表 -->
    <el-card v-if="selectedProjectId">
      <template #header>
        <div class="card-header">
          <span>{{ selectedProject?.name }} - Agent 列表</span>
          <div class="header-actions">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索 Agent..."
              clearable
              style="width: 200px"
              :prefix-icon="Search"
            />
            <el-radio-group v-model="viewMode" class="view-toggle">
              <el-radio-button value="table">
                <el-icon><List /></el-icon>
              </el-radio-button>
              <el-radio-button value="card">
                <el-icon><Grid /></el-icon>
              </el-radio-button>
            </el-radio-group>
            <el-button @click="loadAgents" :loading="loading">
              <el-icon><Refresh /></el-icon> 刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- 表格视图 -->
      <div v-if="viewMode === 'table'">
        <el-table :data="filteredAgents" v-loading="loading" stripe @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="45" />
          <el-table-column prop="name" label="名称" width="120">
            <template #default="{ row }">
              <el-link type="primary" @click="handleViewDetail(row)">{{ row.name }}</el-link>
            </template>
          </el-table-column>
          <el-table-column prop="role" label="角色" width="120">
            <template #default="{ row }">
              <el-tag size="small" :type="getRoleTagType(row.role)">{{ getRoleLabel(row.role) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.alive ? 'success' : 'info'" size="small">
                {{ row.alive ? '运行中' : '已停止' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="工作状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.busy ? 'warning' : 'success'" size="small" effect="plain">
                {{ row.busy ? '忙碌' : '空闲' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Token" width="180">
            <template #default="{ row }">
              <div v-if="row.tokenName" class="token-cell">
                <el-tag type="success" size="small">{{ row.tokenName }}</el-tag>
                <span class="token-model">{{ row.tokenModel || '' }}</span>
              </div>
              <span v-else class="text-muted">未分配</span>
            </template>
          </el-table-column>
          <el-table-column label="推理深度" width="200">
            <template #default="{ row }">
              <div class="reasoning-depth-cell">
                <el-select
                  :model-value="row.reasoningDepth || 3"
                  size="small"
                  style="width: 140px"
                  @change="(val) => handleReasoningDepthChange(row, val)"
                  :disabled="!hasPermission('agents:manage')"
                >
                  <el-option
                    v-for="option in reasoningDepthOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  >
                    <div class="depth-option">
                      <span>{{ option.label }}</span>
                      <span class="depth-desc">{{ option.description }}</span>
                    </div>
                  </el-option>
                </el-select>
                <el-tooltip :content="getDepthTooltip(row.reasoningDepth)" placement="top">
                  <el-icon class="depth-info"><InfoFilled /></el-icon>
                </el-tooltip>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="250" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" size="small" text @click="handleViewDetail(row)">详情</el-button>
              <el-button type="success" size="small" text @click="handleSendTask(row)">任务</el-button>
              <el-button
                :type="row.alive ? 'warning' : 'success'"
                size="small"
                text
                @click="handleToggle(row)"
                v-permission="'agents:manage'"
              >
                {{ row.alive ? '停止' : '启动' }}
              </el-button>
              <el-button type="info" size="small" text @click="handleRestart(row)" v-permission="'agents:manage'">重启</el-button>
              <el-button type="warning" size="small" text @click="handleEditToken(row)" v-permission="'agents:manage'">Token</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 卡片视图 -->
      <div v-else class="agents-grid">
        <el-card
          v-for="agent in filteredAgents"
          :key="agent.id"
          class="agent-card"
          shadow="hover"
          @click="handleViewDetail(agent)"
        >
          <div class="agent-card-header">
            <el-tag :type="agent.alive ? 'success' : 'info'" size="small">
              {{ agent.alive ? '运行中' : '已停止' }}
            </el-tag>
            <el-tag v-if="agent.busy" type="warning" size="small" effect="plain">忙碌</el-tag>
          </div>
          <div class="agent-card-body">
            <div class="agent-avatar">
              <el-icon :size="32"><User /></el-icon>
            </div>
            <h4 class="agent-name">{{ agent.name }}</h4>
            <el-tag :type="getRoleTagType(agent.role)" size="small">{{ getRoleLabel(agent.role) }}</el-tag>
          </div>
          <div class="agent-card-footer">
            <el-button type="primary" size="small" text @click.stop="handleSendTask(agent)">发送任务</el-button>
            <el-button
              :type="agent.alive ? 'warning' : 'success'"
              size="small"
              text
              @click.stop="handleToggle(agent)"
              v-permission="'agents:manage'"
            >
              {{ agent.alive ? '停止' : '启动' }}
            </el-button>
          </div>
        </el-card>
      </div>

      <!-- 批量操作 -->
      <div v-if="selectedAgents.length > 0" class="batch-actions">
        <span class="batch-info">已选择 {{ selectedAgents.length }} 个 Agent</span>
        <el-button type="warning" size="small" @click="handleBatchStop" v-permission="'agents:manage'">批量停止</el-button>
        <el-button type="success" size="small" @click="handleBatchStart" v-permission="'agents:manage'">批量启动</el-button>
      </div>
    </el-card>

    <!-- 未选择项目时的提示 -->
    <el-card v-else class="empty-card">
      <el-empty description="请先选择一个项目">
        <template #image>
          <el-icon :size="64" color="#c0c4cc"><Folder /></el-icon>
        </template>
      </el-empty>
    </el-card>

    <!-- Agent 详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :title="currentAgent?.name || 'Agent 详情'"
      size="550px"
      direction="rtl"
    >
      <template v-if="currentAgent">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="Agent ID">{{ currentAgent.id }}</el-descriptions-item>
          <el-descriptions-item label="名称">{{ currentAgent.name }}</el-descriptions-item>
          <el-descriptions-item label="角色">
            <el-tag :type="getRoleTagType(currentAgent.role)" size="small">{{ getRoleLabel(currentAgent.role) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="currentAgent.alive ? 'success' : 'info'" size="small">
              {{ currentAgent.alive ? '运行中' : '已停止' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="工作状态">
            <el-tag :type="currentAgent.busy ? 'warning' : 'success'" size="small">
              {{ currentAgent.busy ? '忙碌' : '空闲' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="推理深度">
            {{ getDepthLabel(currentAgent.reasoningDepth) }}
          </el-descriptions-item>
          <el-descriptions-item label="通知目标" v-if="roleDetail.notifyTargets">
            {{ roleDetail.notifyTargets }}
          </el-descriptions-item>
          <el-descriptions-item label="审查者" v-if="roleDetail.reviewer">
            {{ roleDetail.reviewer || '无' }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 角色提示词预览 -->
        <el-divider>角色提示词</el-divider>
        <div class="prompt-preview" v-if="roleDetail.prompt">
          <div class="prompt-content">{{ roleDetail.prompt.substring(0, 500) }}{{ roleDetail.prompt.length > 500 ? '...' : '' }}</div>
          <el-button type="primary" text size="small" @click="showFullPrompt = true">
            查看完整提示词
          </el-button>
        </div>
        <el-empty v-else description="暂无角色提示词" :image-size="60" />

        <el-divider>快捷操作</el-divider>
        <div class="drawer-actions">
          <el-button type="primary" @click="handleSendTask(currentAgent)">
            <el-icon><Promotion /></el-icon> 发送任务
          </el-button>
          <el-button @click="handleQuery(currentAgent)">
            <el-icon><ChatDotRound /></el-icon> 发送消息
          </el-button>
          <el-button type="success" @click="handleGoToDetail(currentAgent)">
            <el-icon><MagicStick /></el-icon> 提示词优化
          </el-button>
          <el-button
            :type="currentAgent.alive ? 'warning' : 'success'"
            @click="handleToggle(currentAgent)"
            v-permission="'agents:manage'"
          >
            <el-icon><VideoPlay /></el-icon> {{ currentAgent.alive ? '停止' : '启动' }}
          </el-button>
          <el-button type="info" @click="handleRestart(currentAgent)" v-permission="'agents:manage'">
            <el-icon><RefreshRight /></el-icon> 重启
          </el-button>
        </div>
      </template>
    </el-drawer>

    <!-- 完整提示词弹窗 -->
    <el-dialog v-model="showFullPrompt" title="角色提示词" width="700px" top="5vh">
      <div class="full-prompt-content">{{ roleDetail.prompt }}</div>
      <template #footer>
        <el-button @click="showFullPrompt = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 发送任务对话框 -->
    <el-dialog v-model="taskDialogVisible" title="发送任务" width="500px">
      <el-form :model="taskForm" label-width="80px">
        <el-form-item label="Agent">
          <el-input :model-value="taskForm.agentName" disabled />
        </el-form-item>
        <el-form-item label="任务内容" required>
          <MarkdownEditor v-model="taskForm.content" :rows="4" placeholder="请输入任务内容..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="taskDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitTask" :loading="sendingTask">发送</el-button>
      </template>
    </el-dialog>

    <!-- Token 编辑对话框 -->
    <el-dialog v-model="tokenDialogVisible" title="分配 Token" width="500px">
      <el-form :model="tokenForm" label-width="120px">
        <el-form-item label="Agent">
          <el-input :model-value="tokenForm.agentName" disabled />
        </el-form-item>
        <el-form-item label="当前 Token">
          <span v-if="tokenForm.currentTokenName" class="current-token">{{ tokenForm.currentTokenName }}</span>
          <span v-else class="text-muted">未分配</span>
        </el-form-item>
        <el-form-item label="选择 Token" required>
          <el-select v-model="tokenForm.tokenId" placeholder="选择 Token" style="width: 100%" filterable>
            <el-option
              v-for="token in availableTokens"
              :key="token.id"
              :label="`${token.name} (${token.model || '未设置'}) - ${token.maskedApiKey || '••••'}`"
              :value="token.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="生效方式">
          <el-radio-group v-model="tokenForm.activation">
            <el-radio value="immediate">立即生效</el-radio>
            <el-radio value="pending">等待任务完成</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleUnassignToken" type="danger" v-if="tokenForm.currentTokenName">解绑</el-button>
        <el-button @click="tokenDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAssignToken" :loading="assigningToken">确认分配</el-button>
      </template>
    </el-dialog>

    <!-- 推理深度说明对话框 -->
    <el-dialog v-model="depthHelpVisible" title="推理深度说明" width="500px">
      <div class="depth-help-content">
        <p>推理深度决定了 Agent 在处理任务时的思考深度和详细程度。深度越高，Agent 的回答越全面详细，但响应时间也会更长。</p>
        <el-table :data="reasoningDepthOptions" size="small" border>
          <el-table-column prop="label" label="级别" width="120" />
          <el-table-column prop="description" label="说明" />
        </el-table>
        <div class="depth-help-tip">
          <el-icon><InfoFilled /></el-icon>
          <span>建议根据任务复杂度选择合适的推理深度。简单任务使用"快速"或"标准"，复杂任务使用"深入"或"全面"。</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * Agent 管理页面
 * 先选择项目，再管理该项目下的 Agent
 *
 * 功能：
 * - 统计概览（总数、运行中、忙碌、空闲）
 * - 查看 Agent 列表和状态（表格/卡片视图）
 * - 启动/停止/重启 Agent
 * - 设置推理深度
 * - 查看 Agent 详情（侧边抽屉）
 * - 发送任务给 Agent
 * - 批量操作
 *
 * 组件复用说明：
 * - ProjectSelector: 项目选择器组件
 *   位置: @/components/ProjectSelector.vue
 *   用途: 选择要管理的项目
 *
 * 操作维度：项目级
 * 权限要求：agents:view
 */
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { agentApi, tokenApi, recruitmentApi } from '@/api'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Folder, InfoFilled, Search, List, Grid, User,
  CircleCheck, Loading, Coffee, Promotion,
  ChatDotRound, VideoPlay, RefreshRight, MagicStick
} from '@element-plus/icons-vue'
import ProjectSelector from '@/components/ProjectSelector.vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const agents = ref([])
const selectedProjectId = ref(route.query.projectId || localStorage.getItem('selectedProjectId') || '')
const projects = ref([])
const depthHelpVisible = ref(false)

// 视图模式
const viewMode = ref('table') // table | card
const searchKeyword = ref('')
const selectedAgents = ref([])

// 详情抽屉
const drawerVisible = ref(false)
const currentAgent = ref(null)
const roleDetail = ref({})
const showFullPrompt = ref(false)

// 发送任务
const taskDialogVisible = ref(false)
const sendingTask = ref(false)
const taskForm = ref({
  agentId: '',
  agentName: '',
  content: ''
})

// Token 编辑
const tokenDialogVisible = ref(false)
const assigningToken = ref(false)
const availableTokens = ref([])
const tokenForm = ref({
  agentId: '',
  agentName: '',
  currentTokenName: '',
  tokenId: null,
  activation: 'immediate'
})

// 推理深度选项
const reasoningDepthOptions = [
  { value: 1, label: '快速', description: '简洁直接，关注关键要点' },
  { value: 2, label: '标准', description: '清晰合理，必要分析' },
  { value: 3, label: '深入', description: '多角度分析，详细推理' },
  { value: 4, label: '全面', description: '全面深入，考虑边界情况' },
  { value: 5, label: '极致', description: '穷举可能性，最优方案' }
]

const selectedProject = computed(() => {
  return projects.value.find(p => p.id === selectedProjectId.value) || null
})

// 统计
const runningCount = computed(() => agents.value.filter(a => a.alive).length)
const busyCount = computed(() => agents.value.filter(a => a.busy).length)
const idleCount = computed(() => agents.value.filter(a => a.alive && !a.busy).length)

// 筛选后的 Agent
const filteredAgents = computed(() => {
  if (!searchKeyword.value) return agents.value
  const keyword = searchKeyword.value.toLowerCase()
  return agents.value.filter(a =>
    a.name?.toLowerCase().includes(keyword) ||
    a.role?.toLowerCase().includes(keyword) ||
    a.id?.toLowerCase().includes(keyword)
  )
})

// 检查权限
const hasPermission = (permission) => {
  return userStore.isAdmin() || userStore.hasPermission(permission)
}

// 获取角色标签类型
const getRoleTagType = (role) => {
  const typeMap = {
    'producer': 'danger',
    'server-dev': 'primary',
    'client-dev': 'success',
    'ui-dev': 'warning',
    'system-planner': 'info',
    'numerical-planner': 'info',
    'tester': '',
    'git-commit': 'info',
    'security-expert': 'danger',
    'data-analyst': 'success',
    'tech-artist': 'warning',
    'product-manager': 'primary',
    'localization': '',
    'ai-engineer': 'primary',
    'performance-engineer': 'warning',
    'audio-dev': 'success',
    'narrative-planner': 'info',
    'level-design': 'warning',
    'devops': 'primary'
  }
  return typeMap[role] || ''
}

// 获取角色标签文本
const getRoleLabel = (role) => {
  const labelMap = {
    'producer': '制作人',
    'server-dev': '服务端',
    'client-dev': '客户端',
    'ui-dev': 'UI设计',
    'system-planner': '系统策划',
    'numerical-planner': '数值策划',
    'tester': '测试',
    'git-commit': 'Git专员',
    'security-expert': '安全工程师',
    'data-analyst': '数据分析师',
    'tech-artist': '技术美术',
    'product-manager': '产品经理',
    'localization': '本地化',
    'ai-engineer': 'AI工程师',
    'performance-engineer': '性能优化',
    'audio-dev': '音频设计',
    'narrative-planner': '剧情策划',
    'level-design': '关卡设计',
    'devops': '运维工程师'
  }
  return labelMap[role] || role
}

// 获取深度提示
const getDepthTooltip = (depth) => {
  const option = reasoningDepthOptions.find(o => o.value === depth)
  return option ? `${option.label}: ${option.description}` : '点击查看详情'
}

// 获取深度标签
const getDepthLabel = (depth) => {
  const option = reasoningDepthOptions.find(o => o.value === depth)
  return option ? option.label : '未知'
}

/** 项目切换 */
const handleProjectChange = (projectId) => {
  if (projectId) {
    loadAgents()
  } else {
    agents.value = []
  }
}

/** 加载 Agent 列表 */
const loadAgents = async () => {
  if (!selectedProjectId.value) return

  loading.value = true
  try {
    const data = await agentApi.getByProject(selectedProjectId.value)
    agents.value = data || []
  } catch (error) {
    ElMessage.error('加载 Agent 列表失败')
  } finally {
    loading.value = false
  }
}

/** 表格多选变化 */
const handleSelectionChange = (selection) => {
  selectedAgents.value = selection
}

/** 查看详情 */
const handleViewDetail = async (agent) => {
  currentAgent.value = agent
  drawerVisible.value = true
  // 加载角色详情（提示词、通知目标、审查者）
  try {
    const detail = await recruitmentApi.getRoleDetail(agent.role)
    roleDetail.value = detail || {}
  } catch {
    roleDetail.value = {}
  }
}

/** 跳转到Agent详情页（提示词优化等功能） */
const handleGoToDetail = (agent) => {
  const projectId = agent.projectId || agent.id?.split(':')[0] || route.query.projectId
  const agentRole = agent.role || agent.id?.split(':')[1]
  if (projectId && agentRole) {
    drawerVisible.value = false
    router.push(`/agents/${projectId}/${agentRole}`)
  } else {
    ElMessage.warning('无法确定Agent的项目和角色信息')
  }
}

/** 发送任务 */
const handleSendTask = (agent) => {
  taskForm.value = {
    agentId: agent.id,
    agentName: agent.name,
    content: ''
  }
  taskDialogVisible.value = true
  drawerVisible.value = false
}

/** 提交任务 */
const handleSubmitTask = async () => {
  if (!taskForm.value.content) {
    ElMessage.warning('请输入任务内容')
    return
  }
  sendingTask.value = true
  try {
    await agentApi.sendTask(selectedProjectId.value, taskForm.value.agentId.split(':').pop(), {
      content: taskForm.value.content
    })
    ElMessage.success('任务已发送')
    taskDialogVisible.value = false
    loadAgents()
  } catch (error) {
    ElMessage.error('发送任务失败')
  } finally {
    sendingTask.value = false
  }
}

/** 发送消息 */
const handleQuery = (agent) => {
  taskForm.value = {
    agentId: agent.id,
    agentName: agent.name,
    content: ''
  }
  taskDialogVisible.value = true
  drawerVisible.value = false
}

/** 启动/停止 */
const handleToggle = async (agent) => {
  const action = agent.alive ? '停止' : '启动'
  try {
    await ElMessageBox.confirm(`确定要${action} Agent "${agent.name}" 吗？`, '确认操作', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    if (agent.alive) {
      await agentApi.stop(selectedProjectId.value, agent.role)
    } else {
      await agentApi.start(selectedProjectId.value, agent.role)
    }

    ElMessage.success(`Agent 已${action}`)
    drawerVisible.value = false
    loadAgents()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(`${action}失败`)
    }
  }
}

/** 重启 */
const handleRestart = async (agent) => {
  try {
    await ElMessageBox.confirm(`确定要重启 Agent "${agent.name}" 吗？`, '确认操作', {
      confirmButtonText: '重启',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await agentApi.restart(selectedProjectId.value, agent.role)

    ElMessage.success('Agent 已重启')
    drawerVisible.value = false
    loadAgents()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('重启失败')
    }
  }
}

/** 设置推理深度 */
const handleReasoningDepthChange = async (agent, newDepth) => {
  try {
    const result = await agentApi.setReasoningDepth(selectedProjectId.value, agent.role, newDepth)
    if (result.success) {
      ElMessage.success(result.message || '推理深度已更新')
      agent.reasoningDepth = newDepth
    } else {
      ElMessage.error(result.message || '设置失败')
    }
  } catch (error) {
    ElMessage.error('设置推理深度失败')
    loadAgents()
  }
}

/** 打开 Token 编辑对话框 */
const handleEditToken = async (agent) => {
  tokenForm.value = {
    agentId: agent.id,
    agentName: agent.name,
    currentTokenName: agent.tokenName || '',
    tokenId: agent.tokenId || null,
    activation: 'immediate'
  }
  // 加载可用 Token 列表
  try {
    const tokens = await tokenApi.getAll()
    availableTokens.value = (tokens || []).filter(t => t.status === 'ACTIVE')
  } catch {
    availableTokens.value = []
  }
  tokenDialogVisible.value = true
}

/** 提交 Token 分配 */
const handleAssignToken = async () => {
  if (!tokenForm.value.tokenId) {
    ElMessage.warning('请选择 Token')
    return
  }
  assigningToken.value = true
  try {
    await tokenApi.assign(tokenForm.value.tokenId, tokenForm.value.agentId, tokenForm.value.activation)
    ElMessage.success('Token 已分配')
    tokenDialogVisible.value = false
    loadAgents()
  } catch (error) {
    ElMessage.error('分配失败')
  } finally {
    assigningToken.value = false
  }
}

/** 解绑 Token */
const handleUnassignToken = async () => {
  if (!tokenForm.value.tokenId) return
  try {
    await ElMessageBox.confirm('确定要解绑该 Agent 的 Token 吗？', '确认', { type: 'warning' })
    await tokenApi.unassign(tokenForm.value.tokenId)
    ElMessage.success('Token 已解绑')
    tokenDialogVisible.value = false
    loadAgents()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('解绑失败')
  }
}

/** 批量停止 */
const handleBatchStop = async () => {
  try {
    await ElMessageBox.confirm(`确定要停止选中的 ${selectedAgents.value.length} 个 Agent 吗？`, '批量停止', {
      confirmButtonText: '停止',
      cancelButtonText: '取消',
      type: 'warning'
    })

    for (const agent of selectedAgents.value) {
      if (agent.alive) {
        await agentApi.stop(selectedProjectId.value, agent.role)
      }
    }

    ElMessage.success('批量停止完成')
    loadAgents()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量停止失败')
    }
  }
}

/** 批量启动 */
const handleBatchStart = async () => {
  try {
    await ElMessageBox.confirm(`确定要启动选中的 ${selectedAgents.value.length} 个 Agent 吗？`, '批量启动', {
      confirmButtonText: '启动',
      cancelButtonText: '取消',
      type: 'warning'
    })

    for (const agent of selectedAgents.value) {
      if (!agent.alive) {
        await agentApi.start(selectedProjectId.value, agent.role)
      }
    }

    ElMessage.success('批量启动完成')
    loadAgents()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量启动失败')
    }
  }
}

onMounted(() => {
  if (selectedProjectId.value) {
    loadAgents()
  }
})
</script>

<style scoped>
.agents-page {
  padding: 20px;
}

.project-selector-card {
  margin-bottom: 16px;
}

.project-selector-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.selector-label {
  font-weight: 500;
  color: #606266;
}

.project-tag {
  margin-left: 8px;
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

.view-toggle {
  flex-shrink: 0;
}

/* 卡片视图 */
.agents-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.agent-card {
  cursor: pointer;
  transition: all 0.3s;
}

.agent-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--el-box-shadow-light);
}

.agent-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.agent-card-body {
  text-align: center;
  padding: 12px 0;
}

.agent-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--el-color-primary-light-9);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 12px;
  color: var(--el-color-primary);
}

.agent-name {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
}

.agent-card-footer {
  display: flex;
  justify-content: center;
  gap: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
  padding-top: 12px;
}

/* 批量操作 */
.batch-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  padding: 12px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
}

.batch-info {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

/* 详情抽屉 */
.drawer-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

/* 角色提示词预览 */
.prompt-preview {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 12px;
}

.prompt-content {
  font-size: 12px;
  line-height: 1.6;
  color: #606266;
  white-space: pre-wrap;
  max-height: 200px;
  overflow-y: auto;
  margin-bottom: 8px;
}

.full-prompt-content {
  font-size: 13px;
  line-height: 1.8;
  color: #303133;
  white-space: pre-wrap;
  max-height: 65vh;
  overflow-y: auto;
  background: #f5f7fa;
  border-radius: 8px;
  padding: 16px;
}

/* 推理深度 */
.reasoning-depth-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.depth-info {
  color: var(--el-text-color-secondary);
  cursor: pointer;
}

.depth-info:hover {
  color: var(--el-color-primary);
}

.depth-option {
  display: flex;
  justify-content: space-between;
  width: 100%;
}

.depth-desc {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

/* 深度说明 */
.depth-help-content p {
  margin-bottom: 16px;
  line-height: 1.6;
}

.depth-help-tip {
  margin-top: 16px;
  padding: 12px;
  background: var(--el-color-primary-light-9);
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--el-color-primary);
  font-size: 13px;
}

.empty-card {
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Token 单元格 */
.token-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.token-model {
  font-size: 11px;
  color: #909399;
}

.text-muted {
  color: #909399;
  font-size: 12px;
}

.current-token {
  font-weight: 500;
  color: #67c23a;
}

/* 响应式 */
@media (max-width: 767px) {
  .agents-page {
    padding: 12px;
  }

  .project-selector-wrapper {
    flex-direction: column;
    align-items: flex-start;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .agents-grid {
    grid-template-columns: 1fr;
  }

  :deep(.el-dialog),
  :deep(.el-drawer) {
    width: 90% !important;
  }
}
</style>
