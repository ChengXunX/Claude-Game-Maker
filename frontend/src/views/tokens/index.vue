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
            <div class="stat-label">使用中</div>
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
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getResourceTypeTag(row.resourceType || 'TEXT')" size="small">
              {{ getResourceTypeLabel(row.resourceType || 'TEXT') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="上下文窗口" width="100">
          <template #default="{ row }">
            {{ formatContextWindow(row.contextWindow) }}
          </template>
        </el-table-column>
        <el-table-column label="最大Token" width="100">
          <template #default="{ row }">
            {{ formatContextWindow(row.maxTokens) }}
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
        <el-table-column label="用途" width="90">
          <template #default="{ row }">
            <el-tag :type="getPurposeType(row.purpose)" size="small">
              {{ getPurposeLabel(row.purpose) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="配额" width="200">
          <template #default="{ row }">
            <div v-if="row.quotaType === 'UNLIMITED'" class="quota-info">
              <el-tag type="success" size="small">无限制</el-tag>
            </div>
            <div v-else class="quota-info">
              <div class="quota-bar">
                <el-progress
                  :percentage="getQuotaPercent(row)"
                  :color="getQuotaColor(row)"
                  :stroke-width="8"
                  :show-text="false"
                />
              </div>
              <span class="quota-text">
                {{ row.quotaType === 'SLIDING_WINDOW' ? '窗口' : '总量' }}:
                {{ formatTokens(getQuotaRemaining(row)) }} / {{ formatTokens(row.quotaTotal) }}
              </span>
              <span v-if="row.windowSeconds > 0" class="quota-window">
                ({{ Math.floor(row.windowSeconds / 3600) }}h窗口)
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="并发" width="80">
          <template #default="{ row }">
            <span v-if="row.maxConcurrentAgents > 0" :class="getConcurrentClass(row)">
              {{ row._currentConcurrent || 0 }}/{{ row.maxConcurrentAgents }}
            </span>
            <span v-else class="text-muted">不限</span>
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
            <el-button type="success" size="small" text @click="openAssignDialog(row)">应用到Agent</el-button>
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
        <el-form-item label="用途">
          <el-radio-group v-model="tokenForm.purpose">
            <el-radio value="AGENT">Agent 专用</el-radio>
            <el-radio value="AI_ASSISTANT">AI 助手专用</el-radio>
            <el-radio value="SHARED">共享</el-radio>
          </el-radio-group>
          <div class="form-tip">Agent 专用 Token 不会分配给 AI 助手，AI 助手专用 Token 不会分配给 Agent</div>
        </el-form-item>
        <el-form-item label="提供商类型">
          <el-select v-model="tokenForm.providerType" style="width: 100%" @change="onProviderChange">
            <el-option value="ANTHROPIC" label="Anthropic Claude" />
            <el-option value="OPENAI_COMPATIBLE" label="OpenAI 兼容（DeepSeek/MiniMax/智谱等）" />
            <el-option value="SUNO" label="Suno 音乐生成" />
            <el-option value="ELEVENLABS" label="ElevenLabs 音效生成" />
            <el-option value="DALL_E" label="DALL-E 图片生成" />
            <el-option value="STABILITY" label="Stability AI (Stable Diffusion)" />
            <el-option value="ZHIPU_IMAGE" label="智谱图片生成" />
            <el-option value="CUSTOM_RESOURCE" label="自定义资源 API" />
          </el-select>
          <div class="form-tip">选择 Token 对应的 AI 服务提供商</div>
        </el-form-item>
        <el-form-item label="内容类型">
          <el-radio-group v-model="tokenForm.resourceType">
            <el-radio value="TEXT">文本/代码</el-radio>
            <el-radio value="AUDIO">音频</el-radio>
            <el-radio value="IMAGE">图片</el-radio>
            <el-radio value="MULTIMODAL">多模态</el-radio>
          </el-radio-group>
          <div class="form-tip">文本/代码用于代码类 Agent，音频/图片用于资源类 Agent（audio-dev、tech-artist、ui-dev）</div>
        </el-form-item>
        <el-divider content-position="left">配额设置</el-divider>
        <el-form-item label="配额类型">
          <el-select v-model="tokenForm.quotaType" style="width: 100%">
            <el-option value="UNLIMITED" label="无限制" />
            <el-option value="TOTAL" label="总量配额" />
            <el-option value="SLIDING_WINDOW" label="滑动窗口" />
          </el-select>
          <div class="form-tip">
            <span v-if="tokenForm.quotaType === 'UNLIMITED'">不限制使用量，适合无配额限制的平台</span>
            <span v-else-if="tokenForm.quotaType === 'TOTAL'">设置总配额量，用完即止（如月度配额）</span>
            <span v-else>滑动窗口内限额，窗口过后自动恢复（如火山引擎5小时窗口）</span>
          </div>
        </el-form-item>
        <el-form-item v-if="tokenForm.quotaType !== 'UNLIMITED'" label="配额总量">
          <el-input-number v-model="tokenForm.quotaTotal" :min="1000" :step="10000" style="width: 100%" />
          <div class="form-tip">Token 数量上限</div>
        </el-form-item>
        <el-form-item v-if="tokenForm.quotaType === 'SLIDING_WINDOW'" label="窗口时长">
          <el-input-number v-model="tokenForm.windowHours" :min="1" :max="168" :step="1" />
          <span style="margin-left: 8px">小时</span>
          <div class="form-tip">滑动窗口时长，窗口内的使用量不超过配额总量</div>
        </el-form-item>
        <el-form-item label="最大并发">
          <el-input-number v-model="tokenForm.maxConcurrentAgents" :min="0" :max="20" />
          <span style="margin-left: 8px">个 Agent（0=不限）</span>
          <div class="form-tip">同时使用该 Token 的最大 Agent 数量，防止并发过高导致卡顿</div>
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

    <!-- 应用到 Agent 对话框（池化模式） -->
    <el-dialog v-model="assignDialogVisible" title="应用 Token 到 Agent" width="500px">
      <el-alert type="info" :closable="false" style="margin-bottom: 16px">
        池化模式：Token 可以同时被多个 Agent 使用，不做排他绑定
      </el-alert>
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
        <el-button type="primary" @click="handleAssignSubmit" :loading="assigning">确认应用</el-button>
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
  agentTagsArray: [], priority: 10, description: '', purpose: 'AGENT',
  providerType: 'ANTHROPIC', resourceType: 'TEXT',
  quotaType: 'UNLIMITED', quotaTotal: 0, windowHours: 5, maxConcurrentAgents: 0
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

const getResourceTypeLabel = (type) => ({
  TEXT: '文本', AUDIO: '音频', IMAGE: '图片', MULTIMODAL: '多模态'
}[type] || type)

const getResourceTypeTag = (type) => ({
  TEXT: '', AUDIO: 'warning', IMAGE: 'success', MULTIMODAL: 'info'
}[type] || 'info')

const getPriorityType = (p) => {
  if (!p || p >= 10) return 'info'
  if (p <= 3) return 'danger'
  if (p <= 6) return 'warning'
  return 'success'
}

const getPurposeLabel = (purpose) => {
  const map = { 'AGENT': 'Agent', 'AI_ASSISTANT': 'AI助手', 'SHARED': '共享' }
  return map[purpose] || 'Agent'
}

const getPurposeType = (purpose) => {
  const map = { 'AGENT': 'primary', 'AI_ASSISTANT': 'warning', 'SHARED': 'info' }
  return map[purpose] || 'primary'
}

const parseTags = (tags) => tags ? tags.split(',').map(t => t.trim()).filter(t => t) : []

const formatTokens = (n) => {
  if (n === -1 || n === null || n === undefined) return '-'
  if (n >= 1000000) return (n / 1000000).toFixed(1) + 'M'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'K'
  return String(n)
}

/** 获取 Token 余量百分比（0-100） */
const getQuotaPercent = (row) => {
  if (!row.quotaTotal || row.quotaTotal <= 0) return 100
  const remaining = getQuotaRemaining(row)
  return Math.min(100, Math.round(remaining / row.quotaTotal * 100))
}

/** 获取 Token 剩余量 */
const getQuotaRemaining = (row) => {
  if (row.quotaType === 'UNLIMITED') return -1
  if (!row.quotaTotal) return 0
  if (row.quotaType === 'TOTAL') return Math.max(0, row.quotaTotal - (row.totalTokensUsed || 0))
  // SLIDING_WINDOW: 简化展示，实际余量由后端计算
  return row.quotaTotal
}

/** 获取余量进度条颜色 */
const getQuotaColor = (row) => {
  const percent = getQuotaPercent(row)
  if (percent > 50) return '#67c23a'
  if (percent > 20) return '#e6a23c'
  return '#f56c6c'
}

/** 获取并发状态样式 */
const getConcurrentClass = (row) => {
  if (!row.maxConcurrentAgents) return ''
  const current = row._currentConcurrent || 0
  if (current >= row.maxConcurrentAgents) return 'text-danger'
  if (current >= row.maxConcurrentAgents * 0.8) return 'text-warning'
  return ''
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
    agentTagsArray: [], priority: 10, description: '', purpose: 'AGENT',
    providerType: 'ANTHROPIC', resourceType: 'TEXT',
    quotaType: 'UNLIMITED', quotaTotal: 0, windowHours: 5, maxConcurrentAgents: 0
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
    description: token.description || '',
    purpose: token.purpose || 'AGENT',
    providerType: token.providerType || 'ANTHROPIC',
    resourceType: token.resourceType || 'TEXT',
    quotaType: token.quotaType || 'UNLIMITED',
    quotaTotal: token.quotaTotal || 0,
    windowHours: token.windowSeconds ? Math.floor(token.windowSeconds / 3600) : 5,
    maxConcurrentAgents: token.maxConcurrentAgents || 0
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
      agentTags: tokenForm.value.agentTagsArray.join(','),
      windowSeconds: (tokenForm.value.windowHours || 0) * 3600
    }
    delete data.agentTagsArray
    delete data.windowHours

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

// 提供商变更时自动设置内容类型和默认值
const onProviderChange = (provider) => {
  const presets = {
    ANTHROPIC: { resourceType: 'TEXT', apiUrl: 'https://api.anthropic.com', model: 'claude-sonnet-4-20250514' },
    OPENAI_COMPATIBLE: { resourceType: 'TEXT', apiUrl: '', model: '' },
    SUNO: { resourceType: 'AUDIO', apiUrl: 'https://api.suno.ai', model: '' },
    ELEVENLABS: { resourceType: 'AUDIO', apiUrl: 'https://api.elevenlabs.io', model: 'eleven_multilingual_v2' },
    DALL_E: { resourceType: 'IMAGE', apiUrl: 'https://api.openai.com', model: 'dall-e-3' },
    STABILITY: { resourceType: 'IMAGE', apiUrl: 'https://api.stability.ai', model: 'stable-diffusion-xl-1024-v1-0' },
    ZHIPU_IMAGE: { resourceType: 'IMAGE', apiUrl: 'https://open.bigmodel.cn', model: 'cogview-3' },
    CUSTOM_RESOURCE: { resourceType: 'IMAGE' }
  }
  const preset = presets[provider]
  if (preset) {
    // 只在内容类型还是默认值 TEXT 时才自动设置，用户已手动选择的不覆盖
    if (tokenForm.value.resourceType === 'TEXT' || !tokenForm.value.resourceType) {
      tokenForm.value.resourceType = preset.resourceType
    }
    if (preset.apiUrl && !tokenForm.value.apiUrl) tokenForm.value.apiUrl = preset.apiUrl
    if (preset.model && !tokenForm.value.model) tokenForm.value.model = preset.model
  }
}

const openAssignDialog = (token) => {
  assignForm.value = {
    tokenId: token.id,
    tokenName: token.name,
    agentId: '',
    activation: 'immediate'
  }
  assignDialogVisible.value = true
}

const handleAssignSubmit = async () => {
  if (!assignForm.value.agentId) { ElMessage.warning('请选择 Agent'); return }
  assigning.value = true
  try {
    await tokenApi.assign(assignForm.value.tokenId, assignForm.value.agentId, assignForm.value.activation)
    ElMessage.success('Token 已应用到 Agent')
    assignDialogVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error('操作失败')
  } finally {
    assigning.value = false
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
.quota-info { display: flex; flex-direction: column; gap: 4px; }
.quota-bar { width: 100%; }
.quota-bar :deep(.el-progress-bar__outer) { border-radius: 4px; }
.quota-bar :deep(.el-progress-bar__inner) { border-radius: 4px; }
.quota-text { font-size: 11px; color: #606266; }
.quota-window { font-size: 11px; color: #909399; }
.text-danger { color: #f56c6c; font-weight: bold; }
.text-warning { color: #e6a23c; }
</style>
