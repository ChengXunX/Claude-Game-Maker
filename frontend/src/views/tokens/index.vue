<template>
  <div class="tokens-page">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-primary-light-9)">
            <el-icon :size="24" color="var(--el-color-primary)"><Key /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.total || 0 }}</div>
            <div class="stat-label">Token 总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-success-light-9)">
            <el-icon :size="24" color="var(--el-color-success)"><CircleCheck /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.active || 0 }}</div>
            <div class="stat-label">启用中</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-warning-light-9)">
            <el-icon :size="24" color="var(--el-color-warning)"><Connection /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.assigned || 0 }}</div>
            <div class="stat-label">已绑定</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-info-light-9)">
            <el-icon :size="24" color="var(--el-color-info)"><Coin /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ formatTokens(totalTokensUsed) }}</div>
            <div class="stat-label">总消耗 Token</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>Token 管理</span>
          <el-button type="primary" @click="openCreateDialog">
            <el-icon><Plus /></el-icon> 创建 Token
          </el-button>
        </div>
      </template>

      <el-table :data="tokens" v-loading="loading" stripe>
        <el-table-column prop="name" label="名称" width="140" />
        <el-table-column label="API Key" width="180">
          <template #default="{ row }">
            <span class="api-key">{{ row.maskedApiKey || '••••••••' }}</span>
            <el-button size="small" text @click="handleCopyKey(row)" class="copy-btn">
              <el-icon><CopyDocument /></el-icon>
            </el-button>
          </template>
        </el-table-column>
        <el-table-column prop="apiUrl" label="API URL" min-width="160" show-overflow-tooltip />
        <el-table-column prop="model" label="模型" width="130" />
        <el-table-column label="上下文窗口" width="100">
          <template #default="{ row }">
            {{ formatContextWindow(row.contextWindow) }}
          </template>
        </el-table-column>
        <el-table-column label="适用角色" min-width="150">
          <template #default="{ row }">
            <template v-if="row.agentTags">
              <el-tag v-for="tag in parseTags(row.agentTags)" :key="tag" size="small" class="tag-item">
                {{ getRoleLabel(tag) }}
              </el-tag>
            </template>
            <span v-else class="text-muted">通用</span>
          </template>
        </el-table-column>
        <el-table-column label="优先级" width="70">
          <template #default="{ row }">
            <el-tag :type="getPriorityType(row.priority)" size="small">{{ row.priority || 10 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="70">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
              {{ row.status === 'ACTIVE' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="绑定 Agent" width="130">
          <template #default="{ row }">
            <span v-if="row.assignedAgentName">{{ row.assignedAgentName }}</span>
            <span v-else class="text-muted">未绑定</span>
          </template>
        </el-table-column>
        <el-table-column label="用量" width="100">
          <template #default="{ row }">
            <span v-if="row.totalTokensUsed > 0">{{ formatTokens(row.totalTokensUsed) }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="openEditDialog(row)">编辑</el-button>
            <el-button v-if="!row.assignedAgentId" type="success" size="small" text @click="openAssignDialog(row)">分配</el-button>
            <el-button v-else type="warning" size="small" text @click="handleUnassign(row)">解绑</el-button>
            <el-button type="danger" size="small" text @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tokens.length === 0" description="暂无 Token">
        <el-button type="primary" @click="openCreateDialog">创建 Token</el-button>
      </el-empty>
    </el-card>

    <!-- 创建/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑 Token' : '创建 Token'" width="600px">
      <!-- 平台预设 -->
      <div v-if="!isEdit" class="platform-presets">
        <div class="preset-label">快速选择平台</div>
        <div class="preset-buttons">
          <el-button v-for="p in platforms" :key="p.key" size="small"
            :type="activePlatform === p.key ? 'primary' : 'default'"
            @click="applyPreset(p.key)">
            {{ p.icon }} {{ p.name }}
          </el-button>
        </div>
        <el-divider />
      </div>

      <el-form :model="tokenForm" label-width="120px">
        <el-form-item label="名称" required>
          <el-input v-model="tokenForm.name" placeholder="如：Claude Sonnet Token" />
        </el-form-item>
        <el-form-item label="API Key" required>
          <el-input v-model="tokenForm.apiKey" type="password" show-password placeholder="sk-..." />
          <div v-if="isEdit && apiKeySet" class="form-tip" style="color: #67c23a;">已配置（留空则不修改）</div>
        </el-form-item>
        <el-form-item label="API URL">
          <el-input v-model="tokenForm.apiUrl" placeholder="https://api.anthropic.com" />
        </el-form-item>
        <el-form-item label="模型">
          <el-select v-model="tokenForm.model" filterable allow-create placeholder="选择或输入模型" style="width: 100%">
            <el-option v-for="m in currentModels" :key="m.value" :label="m.label" :value="m.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="最大 Token">
          <el-input-number v-model="tokenForm.maxTokens" :min="100" :max="200000" :step="1024" />
        </el-form-item>
        <el-form-item label="上下文窗口">
          <el-select v-model="tokenForm.contextWindow" style="width: 100%">
            <el-option :value="100000" label="100K（标准）" />
            <el-option :value="200000" label="200K（默认）" />
            <el-option :value="500000" label="500K（大上下文）" />
            <el-option :value="1000000" label="1M（长上下文）" />
          </el-select>
          <div class="form-tip">上下文窗口越大，Agent 记忆能力越强，但 Token 消耗更快</div>
        </el-form-item>
        <el-form-item label="适用角色">
          <el-select v-model="tokenForm.agentTagsArray" multiple placeholder="留空表示通用" style="width: 100%">
            <el-option v-for="r in roleOptions" :key="r.value" :label="r.label" :value="r.value" />
          </el-select>
          <div class="form-tip">不选角色则为通用 Token，任何 Agent 都可使用</div>
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="tokenForm.priority" :min="1" :max="100" />
          <div class="form-tip">数值越小优先级越高，自动分配时优先使用</div>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="tokenForm.description" type="textarea" :rows="2" placeholder="备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="success" @click="handleTestConnection" :loading="testing">测试连通性</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">{{ isEdit ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>

    <!-- 分配对话框 -->
    <el-dialog v-model="assignDialogVisible" title="分配 Token 给 Agent" width="500px">
      <el-form :model="assignForm" label-width="120px">
        <el-form-item label="Token">
          <el-input :value="assignForm.tokenName" disabled />
        </el-form-item>
        <el-form-item label="选择 Agent" required>
          <el-select v-model="assignForm.agentId" placeholder="选择 Agent" style="width: 100%" filterable>
            <el-option v-for="agent in availableAgents" :key="agent.id"
              :label="`${agent.name} (${getRoleLabel(agent.role)})`" :value="agent.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="生效方式">
          <el-radio-group v-model="assignForm.activation">
            <el-radio value="immediate">
              <div>
                <div>立即生效</div>
                <div class="radio-desc">重启 Agent 进程，立即使用新的 API 配置</div>
              </div>
            </el-radio>
            <el-radio value="pending">
              <div>
                <div>等待任务完成</div>
                <div class="radio-desc">Agent 完成当前任务后自动应用新配置</div>
              </div>
            </el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAssignSubmit" :loading="assigning">确认分配</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { tokenApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Key, CircleCheck, Connection, Coin, Plus, CopyDocument } from '@element-plus/icons-vue'

const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const tokens = ref([])
const stats = ref({ total: 0, active: 0, assigned: 0 })
const availableAgents = ref([])

// 对话框
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const apiKeySet = ref(false)
const activePlatform = ref('')

const tokenForm = ref({
  name: '', apiKey: '', apiUrl: '', model: '', maxTokens: 4096, contextWindow: 200000,
  agentTagsArray: [], priority: 10, description: ''
})

// 分配对话框
const assignDialogVisible = ref(false)
const assigning = ref(false)
const assignForm = ref({ tokenId: null, tokenName: '', agentId: '', activation: 'immediate' })

const totalTokensUsed = computed(() => tokens.value.reduce((sum, t) => sum + (t.totalTokensUsed || 0), 0))

const platforms = [
  { key: 'anthropic', name: 'Anthropic', icon: '✦' },
  { key: 'openai', name: 'OpenAI', icon: '⚡' },
  { key: 'mimo', name: '小米 MiMo', icon: '🤖' },
  { key: 'qwen', name: '阿里云通义', icon: '☁' },
  { key: 'tencent', name: '腾讯云', icon: '🐧' },
  { key: 'volcengine', name: '火山引擎', icon: '🌋' },
  { key: 'baidu', name: '百度智能云', icon: '🔍' },
  { key: 'stepfun', name: '阶跃星辰', icon: '🚀' },
  { key: 'zhipu', name: '智谱 AI', icon: '💬' },
  { key: 'minimax', name: 'MiniMax', icon: '🧠' },
  { key: 'moonshot', name: '月之暗面 Kimi', icon: '🌙' },
  { key: 'xfyun', name: '科大讯飞', icon: '🎤' },
  { key: 'openrouter', name: 'OpenRouter', icon: '🔀' }
]

const PLATFORM_PRESETS = {
  anthropic: {
    url: 'https://api.anthropic.com',
    models: [
      { value: 'claude-sonnet-4-20250514', label: 'Claude Sonnet 4' },
      { value: 'claude-haiku-4-5-20251001', label: 'Claude Haiku 4.5' },
      { value: 'claude-opus-4-20250918', label: 'Claude Opus 4' }
    ]
  },
  openai: {
    url: 'https://api.openai.com/v1',
    models: [
      { value: 'gpt-5.5', label: 'GPT-5.5' },
      { value: 'gpt-4o', label: 'GPT-4o' },
      { value: 'gpt-4o-mini', label: 'GPT-4o Mini' },
      { value: 'o1-preview', label: 'o1-preview' }
    ]
  },
  mimo: {
    url: 'https://token-plan-cn.xiaomimimo.com/anthropic',
    models: [
      { value: 'mimo-v2.5-pro', label: 'MiMo V2.5 Pro' },
      { value: 'mimo-v2.5', label: 'MiMo V2.5' }
    ]
  },
  qwen: {
    url: 'https://token-plan.ap-southeast-1.maas.aliyuncs.com/compatible-mode/v1',
    models: [
      { value: 'qwen3.7-max', label: 'Qwen3.7 Max' },
      { value: 'qwen3.6-plus', label: 'Qwen3.6 Plus' },
      { value: 'qwen3.6-flash', label: 'Qwen3.6 Flash' }
    ]
  },
  tencent: {
    url: 'https://api.lkeap.cloud.tencent.com/plan/v3',
    models: [
      { value: 'hy3-preview', label: 'Hy3 Preview' },
      { value: 'deepseek-v4', label: 'DeepSeek V4' },
      { value: 'glm-5.1', label: 'GLM-5.1' },
      { value: 'kimi-k2.6', label: 'Kimi K2.6' },
      { value: 'minimax-m2.5', label: 'MiniMax M2.5' }
    ]
  },
  volcengine: {
    url: 'https://ark.cn-beijing.volces.com/api/v3',
    models: [
      { value: 'doubao-seed-2.0-pro', label: 'Doubao Seed 2.0 Pro' },
      { value: 'doubao-seed-code-preview', label: 'Doubao Seed Code' },
      { value: 'kimi-k2.5', label: 'Kimi K2.5' },
      { value: 'glm-4.7', label: 'GLM-4.7' },
      { value: 'deepseek-v3.2', label: 'DeepSeek V3.2' }
    ]
  },
  baidu: {
    url: 'https://qianfan.baidubce.com/v2',
    models: [
      { value: 'wenxin-5.1', label: '文心 5.1' },
      { value: 'deepseek-v4', label: 'DeepSeek V4' },
      { value: 'glm-5.1', label: 'GLM-5.1' },
      { value: 'minimax-m2.5', label: 'MiniMax M2.5' }
    ]
  },
  stepfun: {
    url: 'https://api.stepfun.com/step_plan/v1',
    models: [
      { value: 'step-3.7-flash', label: 'Step 3.7 Flash' },
      { value: 'step-3.5-flash', label: 'Step 3.5 Flash' },
      { value: 'step-deepresearch', label: 'Step DeepResearch' }
    ]
  },
  zhipu: {
    url: 'https://open.bigmodel.cn/api/paas/v4',
    models: [
      { value: 'glm-5.1', label: 'GLM-5.1' },
      { value: 'glm-5', label: 'GLM-5' },
      { value: 'glm-4.7', label: 'GLM-4.7' }
    ]
  },
  minimax: {
    url: 'https://api.minimaxi.com/v1',
    models: [
      { value: 'minimax-m3', label: 'MiniMax M3' },
      { value: 'minimax-m2.7', label: 'MiniMax M2.7' }
    ]
  },
  moonshot: {
    url: 'https://api.moonshot.cn/v1',
    models: [
      { value: 'kimi-k2.6', label: 'Kimi K2.6' }
    ]
  },
  xfyun: {
    url: 'https://spark-api-open.xf-yun.cn/x2/',
    models: [
      { value: 'x2-flash', label: '星火 X2 Flash' }
    ]
  },
  openrouter: {
    url: 'https://openrouter.ai/api/v1',
    models: [
      { value: 'owl-alpha', label: 'Owl Alpha' },
      { value: 'gpt-5.5', label: 'GPT-5.5' },
      { value: 'qwen-3.6', label: 'Qwen 3.6' },
      { value: 'deepseek-v4', label: 'DeepSeek V4' }
    ]
  }
}

const roleOptions = [
  { value: 'producer', label: '制作人' },
  { value: 'server-dev', label: '服务端开发' },
  { value: 'client-dev', label: '客户端开发' },
  { value: 'ui-dev', label: 'UI设计' },
  { value: 'system-planner', label: '系统策划' },
  { value: 'numerical-planner', label: '数值策划' },
  { value: 'tester', label: '测试工程师' },
  { value: 'git-commit', label: 'Git专员' },
  { value: 'security-expert', label: '安全工程师' },
  { value: 'data-analyst', label: '数据分析师' },
  { value: 'ai-engineer', label: 'AI工程师' },
  { value: 'devops', label: '运维工程师' },
  { value: 'audio-dev', label: '音频设计师' },
  { value: 'narrative-planner', label: '剧情策划' },
  { value: 'level-design', label: '关卡设计师' },
  { value: 'performance-engineer', label: '性能优化' }
]

const currentModels = computed(() => {
  if (activePlatform.value && PLATFORM_PRESETS[activePlatform.value]) {
    return PLATFORM_PRESETS[activePlatform.value].models
  }
  return PLATFORM_PRESETS.anthropic.models
})

const getRoleLabel = (role) => {
  const found = roleOptions.find(r => r.value === role)
  return found ? found.label : role
}

const getPriorityType = (p) => {
  if (!p || p >= 10) return 'info'
  if (p <= 3) return 'danger'
  if (p <= 6) return 'warning'
  return 'success'
}

const parseTags = (tags) => tags ? tags.split(',').map(t => t.trim()).filter(t => t) : []

const formatTokens = (n) => {
  if (n >= 1000000) return (n / 1000000).toFixed(1) + 'M'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'K'
  return String(n)
}

const formatContextWindow = (n) => {
  if (!n) return '200K'
  if (n >= 1000000) return (n / 1000000).toFixed(0) + 'M'
  if (n >= 1000) return (n / 1000).toFixed(0) + 'K'
  return String(n)
}

const applyPreset = (key) => {
  const preset = PLATFORM_PRESETS[key]
  if (!preset) return
  activePlatform.value = key
  tokenForm.value.apiUrl = preset.url
  if (preset.models.length > 0) tokenForm.value.model = preset.models[0].value
}

const loadData = async () => {
  loading.value = true
  try {
    const [tokenList, statsData, agents] = await Promise.all([
      tokenApi.getAll(),
      tokenApi.getStats().catch(() => ({ total: 0, active: 0, assigned: 0 })),
      tokenApi.getAgents().catch(() => [])
    ])
    tokens.value = tokenList || []
    stats.value = statsData || {}
    availableAgents.value = agents || []
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const openCreateDialog = () => {
  isEdit.value = false
  editingId.value = null
  apiKeySet.value = false
  activePlatform.value = ''
  tokenForm.value = {
    name: '', apiKey: '', apiUrl: '', model: '', maxTokens: 4096, contextWindow: 200000,
    agentTagsArray: [], priority: 10, description: ''
  }
  dialogVisible.value = true
}

const openEditDialog = (token) => {
  isEdit.value = true
  editingId.value = token.id
  apiKeySet.value = !!(token.apiKey && token.apiKey.length > 0)
  activePlatform.value = ''
  tokenForm.value = {
    name: token.name || '',
    apiKey: '',
    apiUrl: token.apiUrl || '',
    model: token.model || '',
    maxTokens: token.maxTokens || 4096,
    contextWindow: token.contextWindow || 200000,
    agentTagsArray: parseTags(token.agentTags),
    priority: token.priority || 10,
    description: token.description || ''
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!tokenForm.value.name) { ElMessage.warning('请输入名称'); return }
  if (!isEdit.value && !tokenForm.value.apiKey) { ElMessage.warning('请输入 API Key'); return }

  saving.value = true
  try {
    const data = {
      ...tokenForm.value,
      agentTags: tokenForm.value.agentTagsArray.join(',')
    }
    delete data.agentTagsArray

    if (isEdit.value) {
      if (!data.apiKey) delete data.apiKey
      await tokenApi.update(editingId.value, data)
      ElMessage.success('Token 已更新')
    } else {
      await tokenApi.create(data)
      ElMessage.success('Token 创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error((isEdit.value ? '更新' : '创建') + '失败')
  } finally {
    saving.value = false
  }
}

const handleTestConnection = async () => {
  if (!tokenForm.value.apiKey) { ElMessage.warning('请先输入 API Key'); return }
  testing.value = true
  try {
    const result = await tokenApi.testConnection({
      apiKey: tokenForm.value.apiKey,
      apiUrl: tokenForm.value.apiUrl,
      model: tokenForm.value.model
    })
    if (result.success) {
      ElMessage.success(result.message || '连接测试成功')
    } else {
      ElMessage.warning(result.message || '连接测试失败')
    }
  } catch (e) {
    ElMessage.error('测试请求失败')
  } finally {
    testing.value = false
  }
}

const openAssignDialog = (token) => {
  assignForm.value = {
    tokenId: token.id,
    tokenName: token.name,
    agentId: token.assignedAgentId || '',
    activation: 'immediate'
  }
  assignDialogVisible.value = true
}

const handleAssignSubmit = async () => {
  if (!assignForm.value.agentId) { ElMessage.warning('请选择 Agent'); return }
  assigning.value = true
  try {
    await tokenApi.assign(assignForm.value.tokenId, assignForm.value.agentId, assignForm.value.activation)
    ElMessage.success('Token 已分配')
    assignDialogVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error('分配失败')
  } finally {
    assigning.value = false
  }
}

const handleUnassign = async (token) => {
  try {
    await ElMessageBox.confirm(`确定要解绑 Token "${token.name}" 吗？`, '确认', { type: 'warning' })
    await tokenApi.unassign(token.id)
    ElMessage.success('已解绑')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

const handleCopyKey = async (token) => {
  try {
    await navigator.clipboard.writeText(token.apiKey || token.maskedApiKey)
    ElMessage.success('已复制')
  } catch { ElMessage.error('复制失败') }
}

const handleDelete = async (token) => {
  try {
    await ElMessageBox.confirm(`确定要删除 Token "${token.name}" 吗？`, '删除确认', { type: 'warning' })
    await tokenApi.delete(token.id)
    ElMessage.success('已删除')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

onMounted(() => loadData())
</script>

<style scoped>
.tokens-page { padding: 20px; }
.stat-cards { margin-bottom: 16px; }
.stat-card :deep(.el-card__body) { display: flex; align-items: center; gap: 16px; padding: 20px; min-height: 80px; }
.stat-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.stat-value { font-size: 28px; font-weight: bold; color: var(--el-text-color-primary); line-height: 1.2; }
.stat-label { font-size: 13px; color: var(--el-text-color-secondary); margin-top: 4px; }
.card-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; }
.api-key { font-family: monospace; font-size: 12px; }
.copy-btn { margin-left: 4px; }
.tag-item { margin-right: 4px; margin-bottom: 4px; }
.text-muted { color: #909399; font-size: 12px; }
.form-tip { font-size: 12px; color: #909399; margin-top: 4px; }
.radio-desc { font-size: 12px; color: #909399; margin-top: 2px; }
:deep(.el-radio) { display: flex; align-items: flex-start; margin-bottom: 12px; height: auto; }
:deep(.el-radio__label) { white-space: normal; }
.platform-presets { margin-bottom: 8px; }
.preset-label { font-size: 13px; color: var(--el-text-color-secondary); margin-bottom: 8px; }
.preset-buttons { display: flex; flex-wrap: wrap; gap: 8px; }
@media (max-width: 767px) {
  .tokens-page { padding: 12px; }
  .stat-card :deep(.el-card__body) { padding: 12px; }
  .stat-value { font-size: 20px; }
  .card-header { flex-direction: column; align-items: flex-start; }
}
</style>
