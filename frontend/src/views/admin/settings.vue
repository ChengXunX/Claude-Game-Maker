<template>
  <div class="settings-page">
    <el-card>
      <template #header>
        <span>系统设置</span>
      </template>

      <el-tabs v-model="activeTab">
        <!-- 基本设置 -->
        <el-tab-pane label="基本设置" name="basic">
          <el-form :model="basicForm" label-width="150px" style="max-width: 600px">
            <el-form-item label="系统名称">
              <el-input v-model="basicForm.systemName" placeholder="ChengXun Game Maker" />
            </el-form-item>
            <el-form-item label="联系方式类型">
              <el-radio-group v-model="basicForm.contactType">
                <el-radio value="link">链接</el-radio>
                <el-radio value="email">邮箱</el-radio>
                <el-radio value="image">图片</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item v-if="basicForm.contactType === 'link'" label="联系链接">
              <el-input v-model="basicForm.contactValue" placeholder="https://example.com/contact" />
              <div class="form-tip">点击后将在新窗口打开该链接</div>
            </el-form-item>
            <el-form-item v-if="basicForm.contactType === 'email'" label="联系邮箱">
              <el-input v-model="basicForm.contactValue" placeholder="admin@example.com" />
              <div class="form-tip">点击后将打开邮件客户端</div>
            </el-form-item>
            <el-form-item v-if="basicForm.contactType === 'image'" label="图片链接">
              <el-input v-model="basicForm.contactValue" placeholder="https://example.com/qr-code.png" />
              <div class="form-tip">点击后将以弹窗形式展示图片（如微信二维码）</div>
            </el-form-item>
            <el-form-item label="ICP备案号">
              <el-input v-model="basicForm.icpFilingNumber" placeholder="陕ICP备2026014383号" />
              <div class="form-tip">显示在网站底部的ICP备案号，留空则不显示。点击可跳转至工信部备案查询系统</div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveBasic" :loading="saving">保存</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 安全设置 -->
        <el-tab-pane label="安全设置" name="security">
          <el-form :model="securityForm" label-width="150px" style="max-width: 600px">
            <el-form-item label="设备信任启用">
              <el-switch v-model="securityForm.deviceTrustEnabled" />
            </el-form-item>
            <el-form-item label="信任天数">
              <el-input-number v-model="securityForm.deviceTrustDays" :min="1" :max="365" />
            </el-form-item>
            <el-divider content-position="left">Token 分配策略</el-divider>
            <el-form-item label="分配策略">
              <el-radio-group v-model="securityForm.tokenStrategy" class="strategy-radio-group">
                <el-radio value="system">系统自动分配</el-radio>
                <el-radio value="producer">制作人分配</el-radio>
              </el-radio-group>
              <div class="form-tip">
                系统自动：新建 Agent 时按优先级+使用量自动分配 Token；制作人分配：由制作人手动分配（能力注入模式）
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveSecurity" :loading="saving">保存</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- AI 模型设置 -->
        <el-tab-pane label="AI 模型设置" name="claude">
          <!-- 平台预设按钮 -->
          <el-form-item label="快速选择平台">
            <el-space wrap>
              <el-button
                v-for="p in platforms"
                :key="p.key"
                :type="activePlatform === p.key ? 'primary' : 'default'"
                @click="applyPreset(p.key)"
                size="small"
              >
                {{ p.icon }} {{ p.name }}
              </el-button>
            </el-space>
          </el-form-item>

          <el-divider />

          <el-form :model="claudeForm" label-width="150px" style="max-width: 600px">
            <el-form-item label="API Key">
              <el-input v-model="claudeForm.apiKey" type="password" show-password placeholder="请输入 API Key" />
              <div v-if="apiKeySet" class="form-tip" style="color: #67c23a;">
                API Key 已配置（留空则不修改）
              </div>
            </el-form-item>
            <el-form-item label="API URL">
              <el-input v-model="claudeForm.apiUrl" placeholder="https://api.anthropic.com" />
            </el-form-item>
            <el-form-item label="模型">
              <el-select
                v-model="claudeForm.model"
                filterable
                allow-create
                placeholder="选择或输入模型名称"
                style="width: 100%"
              >
                <el-option
                  v-for="m in currentModels"
                  :key="m.value"
                  :label="m.label"
                  :value="m.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="长上下文模式">
              <el-switch v-model="longContext" @change="onLongContextChange" />
              <span class="ml-2 text-muted" style="margin-left: 10px; color: #909399; font-size: 12px;">
                开启后模型名称追加 [1m] 参数
              </span>
            </el-form-item>
            <el-form-item label="最大 Token">
              <el-input-number v-model="claudeForm.maxTokens" :min="100" :max="200000" :step="1024" />
              <div class="form-tip" style="margin-top: 4px; font-size: 12px; color: #909399;">
                建议值：4096（默认）、8192、16384、32768、65536
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveClaude" :loading="saving">保存</el-button>
              <el-button type="success" @click="testConnection" :loading="testing">测试连接</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 邮件配置 -->
        <el-tab-pane label="邮件配置" name="email">
          <el-form :model="emailForm" label-width="150px" style="max-width: 600px">
            <el-form-item label="启用邮件">
              <el-switch v-model="emailForm.emailEnabled" />
              <div class="form-tip">开启后系统可通过邮件发送通知和验证码</div>
            </el-form-item>
            <template v-if="emailForm.emailEnabled">
              <el-divider content-position="left">SMTP 服务器配置</el-divider>
              <el-form-item label="SMTP 服务器">
                <el-input v-model="emailForm.smtpHost" placeholder="smtp.qq.com" />
              </el-form-item>
              <el-form-item label="SMTP 端口">
                <el-input-number v-model="emailForm.smtpPort" :min="1" :max="65535" />
                <div class="form-tip">常用端口：587（TLS）、465（SSL）、25（不加密）</div>
              </el-form-item>
              <el-form-item label="SMTP 登录账号">
                <el-input v-model="emailForm.smtpUsername" placeholder="your-email@qq.com" />
                <div class="form-tip">用于 SMTP 认证的邮箱地址</div>
              </el-form-item>
              <el-form-item label="密码/授权码">
                <el-input v-model="emailForm.smtpPassword" type="password" show-password placeholder="SMTP 密码或授权码" />
                <div class="form-tip">QQ邮箱请使用授权码，不是登录密码</div>
              </el-form-item>

              <el-divider content-position="left">发件人配置</el-divider>
              <el-form-item label="发件人地址">
                <el-input v-model="emailForm.emailFrom" placeholder="your-email@qq.com" />
                <div class="form-tip">收件人看到的发件人邮箱，留空则使用登录账号</div>
              </el-form-item>
              <el-form-item label="发件人名称">
                <el-input v-model="emailForm.senderName" placeholder="如：系统管理员、ChengXun Game Maker" />
                <div class="form-tip">收件人看到的发件人名称，如"系统管理员 &lt;noreply@example.com&gt;"</div>
              </el-form-item>
              <el-form-item label="代理邮箱（回复地址）">
                <el-input v-model="emailForm.replyTo" placeholder="admin@example.com（可选）" />
                <div class="form-tip">收件人点击"回复"时使用的邮箱地址，留空则使用发件人地址</div>
              </el-form-item>
            </template>
            <el-form-item>
              <el-button type="primary" @click="saveEmail" :loading="saving">保存</el-button>
              <el-button v-if="emailForm.emailEnabled" type="success" @click="testEmail" :loading="testingEmail">测试连接并发送</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 督查规则 -->
        <el-tab-pane label="督查规则" name="supervision">
          <el-form :model="supervisionForm" label-width="160px" style="max-width: 600px">
            <el-form-item label="迭代超时阈值">
              <el-input-number v-model="supervisionForm.iterationTimeoutHours" :min="12" :max="336" :step="12" />
              <span style="margin-left: 8px; color: #909399;">小时</span>
            </el-form-item>
            <el-form-item label="任务逾期阈值">
              <el-input-number v-model="supervisionForm.taskOverdueThresholdHours" :min="1" :max="168" :step="1" />
              <span style="margin-left: 8px; color: #909399;">小时</span>
            </el-form-item>
            <el-form-item label="质量评分阈值">
              <el-input-number v-model="supervisionForm.qualityScoreThreshold" :min="1" :max="10" :step="1" />
              <span style="margin-left: 8px; color: #909399;">分（低于此值预警）</span>
            </el-form-item>
            <el-form-item label="回滚率阈值">
              <el-input-number v-model="supervisionForm.rollbackRateThreshold" :min="5" :max="100" :step="5" />
              <span style="margin-left: 8px; color: #909399;">%（超过此值预警）</span>
            </el-form-item>
            <el-form-item label="Agent空闲阈值">
              <el-input-number v-model="supervisionForm.agentIdleThresholdHours" :min="1" :max="48" :step="1" />
              <span style="margin-left: 8px; color: #909399;">小时</span>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveSupervision" :loading="saving">保存</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
/**
 * 系统设置页面
 * 管理系统配置，支持 AI 平台预设快速填充和长上下文模式
 *
 * 操作维度：系统级
 * 权限要求：系统管理员
 */
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api, { configApi, emailApi } from '@/api'

const route = useRoute()
const activeTab = ref(route.query.tab || 'basic')
const activePlatform = ref('')
const longContext = ref(false)
const saving = ref(false)
const testing = ref(false)
const testingEmail = ref(false)
const apiKeySet = ref(false)

/** 基本设置表单 */
const basicForm = ref({
  systemName: 'ChengXun Game Maker',
  contactType: 'link',
  contactValue: '',
  icpFilingNumber: ''
})

/** 安全设置表单 */
const securityForm = ref({
  deviceTrustEnabled: true,
  deviceTrustDays: 30,
  tokenStrategy: 'system'
})

/** Claude 设置表单 */
const claudeForm = ref({
  apiKey: '',
  apiUrl: 'https://api.anthropic.com',
  model: '',
  maxTokens: 4096
})

/** 邮件设置表单 */
const emailForm = ref({
  emailEnabled: false,
  smtpHost: '',
  smtpPort: 587,
  smtpUsername: '',
  smtpPassword: '',
  emailFrom: '',
  senderName: '',
  replyTo: ''
})

/** 督查规则表单 */
const supervisionForm = ref({
  iterationTimeoutHours: 168,
  taskOverdueThresholdHours: 24,
  qualityScoreThreshold: 6,
  rollbackRateThreshold: 30,
  agentIdleThresholdHours: 4
})

/** 平台列表 */
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

/** 平台预设配置 */
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

/** 当前平台的模型列表 */
const currentModels = computed(() => {
  if (activePlatform.value && PLATFORM_PRESETS[activePlatform.value]) {
    return PLATFORM_PRESETS[activePlatform.value].models
  }
  return PLATFORM_PRESETS.anthropic.models
})

/**
 * 应用平台预设
 */
function applyPreset(platform) {
  const preset = PLATFORM_PRESETS[platform]
  if (!preset) return

  activePlatform.value = platform
  claudeForm.value.apiUrl = preset.url

  // 默认选中第一个模型
  if (preset.models.length > 0) {
    claudeForm.value.model = preset.models[0].value
  }

  // 重置长上下文开关
  longContext.value = false
}

/**
 * 长上下文开关变化
 */
function onLongContextChange() {
  const baseModel = claudeForm.value.model.replace(/\[1m\]$/, '')
  if (longContext.value && baseModel) {
    claudeForm.value.model = baseModel + '[1m]'
  } else {
    claudeForm.value.model = baseModel
  }
}

/** 保存基本设置 */
const saveBasic = async () => {
  saving.value = true
  try {
    await configApi.batchUpdate([
      { configKey: 'system.name', configValue: basicForm.value.systemName },
      { configKey: 'system.contact.type', configValue: basicForm.value.contactType },
      { configKey: 'system.contact.link', configValue: basicForm.value.contactValue }
    ])
    // ICP备案号存储在 system_constants 表中，需单独保存
    await api.post('/constants/api/update', { key: 'site.icp-filing-number', value: basicForm.value.icpFilingNumber })
    // 同时同步系统名称到 system_constants，供登录页等公开页面使用
    await api.post('/constants/api/update', { key: 'site.name', value: basicForm.value.systemName })
    ElMessage.success('基本设置已保存')
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

/** 保存安全设置 */
const saveSecurity = async () => {
  saving.value = true
  try {
    await configApi.batchUpdate([
      { configKey: 'security.device.trust.enabled', configValue: String(securityForm.value.deviceTrustEnabled) },
      { configKey: 'security.device.trust.days', configValue: String(securityForm.value.deviceTrustDays) },
      { configKey: 'token.allocation.strategy', configValue: securityForm.value.tokenStrategy }
    ])
    ElMessage.success('安全设置已保存')
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

/** 保存 Claude 设置 */
const saveClaude = async () => {
  saving.value = true
  try {
    const updates = [
      { configKey: 'claude.api.url', configValue: claudeForm.value.apiUrl },
      { configKey: 'claude.model', configValue: claudeForm.value.model },
      { configKey: 'claude.max.tokens', configValue: String(claudeForm.value.maxTokens) }
    ]
    // API Key 只在用户输入了新值时才更新
    if (claudeForm.value.apiKey && claudeForm.value.apiKey.trim()) {
      updates.push({ configKey: 'claude.api.key', configValue: claudeForm.value.apiKey.trim() })
    }
    await configApi.batchUpdate(updates)
    ElMessage.success('AI 模型设置已保存')
    // 保存后重新加载，确认数据已持久化
    await loadSettings()
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

/** 保存邮件配置 */
const saveEmail = async () => {
  saving.value = true
  try {
    await emailApi.saveSettings(emailForm.value)
    ElMessage.success('邮件配置已保存')
  } catch (error) {
    ElMessage.error('保存失败: ' + (error.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

/** 保存督查规则（使用已有的督查规则接口） */
const saveSupervision = async () => {
  saving.value = true
  try {
    await api.post('/projects/api/supervision-rules', supervisionForm.value)
    ElMessage.success('督查规则已保存')
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

/** 测试邮件连接 */
const testEmail = async () => {
  try {
    // 先测试连接（使用当前填写的数据）
    testingEmail.value = true
    const connResult = await emailApi.testConnection(emailForm.value)
    if (!connResult.success) {
      ElMessage.warning(connResult.message || '连接测试失败')
      return
    }

    // 连接成功后，询问是否发送测试邮件
    const { value: toEmail } = await ElMessageBox.prompt(
      '邮件连接测试成功！是否发送测试邮件到指定邮箱？',
      '发送测试邮件',
      {
        confirmButtonText: '发送测试',
        cancelButtonText: '跳过',
        inputPlaceholder: '请输入收件邮箱地址',
        inputPattern: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/,
        inputErrorMessage: '请输入有效的邮箱地址'
      }
    )

    // 发送测试邮件（使用当前填写的配置）
    testingEmail.value = false
    testingEmail.value = true
    const sendResult = await emailApi.sendTestEmail(toEmail, emailForm.value)
    if (sendResult.success) {
      ElMessage.success(sendResult.message || '测试邮件已发送')
    } else {
      ElMessage.warning(sendResult.message || '发送测试邮件失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('测试失败: ' + (error.message || '未知错误'))
    }
  } finally {
    testingEmail.value = false
  }
}

/** 测试 AI 模型连接 */
const testConnection = async () => {
  testing.value = true
  try {
    const result = await configApi.testAiConnection({
      apiKey: claudeForm.value.apiKey || '',
      apiUrl: claudeForm.value.apiUrl,
      model: claudeForm.value.model
    })
    if (result.success) {
      ElMessage.success(result.message)
    } else {
      ElMessage.warning(result.message)
    }
  } catch (error) {
    ElMessage.error('测试请求失败: ' + (error.message || '未知错误'))
  } finally {
    testing.value = false
  }
}

/** 加载设置 */
const loadSettings = async () => {
  try {
    // 加载邮件配置
    try {
      const emailSettings = await emailApi.getSettings()
      if (emailSettings) {
        emailForm.value.emailEnabled = emailSettings.emailEnabled ?? false
        emailForm.value.smtpHost = emailSettings.smtpHost || ''
        emailForm.value.smtpPort = emailSettings.smtpPort || 587
        emailForm.value.smtpUsername = emailSettings.smtpUsername || ''
        emailForm.value.emailFrom = emailSettings.emailFrom || ''
        emailForm.value.senderName = emailSettings.senderName || ''
        emailForm.value.replyTo = emailSettings.proxyEmail || ''
      }
    } catch (e) {
      // 邮件配置加载失败不影响其他设置
    }

    // 加载ICP备案号（存储在 system_constants 表中）
    try {
      const icpData = await api.get('/constants/api/all')
      if (icpData && Array.isArray(icpData)) {
        const icp = icpData.find(c => c.constantKey === 'site.icp-filing-number')
        if (icp) basicForm.value.icpFilingNumber = icp.value || ''
      }
    } catch (e) {
      // ICP备案号加载失败不影响其他设置
    }

    const configs = await configApi.getAll()
    if (configs && Array.isArray(configs)) {
      configs.forEach(config => {
        switch (config.configKey) {
          case 'system.name':
            basicForm.value.systemName = config.configValue || 'ChengXun Game Maker'
            break
          case 'system.contact.type':
            basicForm.value.contactType = config.configValue || 'link'
            break
          case 'system.contact.link':
            basicForm.value.contactValue = config.configValue || ''
            break
          case 'claude.api.key':
            // 不覆盖用户正在输入的值
            apiKeySet.value = !!(config.configValue && config.configValue.trim())
            break
          case 'claude.api.url':
            claudeForm.value.apiUrl = config.configValue || 'https://api.anthropic.com'
            break
          case 'claude.model':
            claudeForm.value.model = config.configValue || ''
            longContext.value = config.configValue?.includes('[1m]') || false
            break
          case 'claude.max.tokens':
            claudeForm.value.maxTokens = parseInt(config.configValue) || 4096
            break
          case 'security.device.trust.enabled':
            securityForm.value.deviceTrustEnabled = config.configValue !== 'false'
            break
          case 'security.device.trust.days':
            securityForm.value.deviceTrustDays = parseInt(config.configValue) || 30
            break
          case 'token.allocation.strategy':
            securityForm.value.tokenStrategy = config.configValue || 'system'
            break
        }
      })

      // 加载督查规则（使用专用接口）
      try {
        const rules = await api.get('/projects/api/supervision-rules')
        if (rules) {
          supervisionForm.value.iterationTimeoutHours = rules.iterationTimeoutHours ?? 168
          supervisionForm.value.taskOverdueThresholdHours = rules.taskOverdueThresholdHours ?? 24
          supervisionForm.value.qualityScoreThreshold = rules.qualityScoreThreshold ?? 6
          supervisionForm.value.rollbackRateThreshold = rules.rollbackRateThreshold ?? 30
          supervisionForm.value.agentIdleThresholdHours = rules.agentIdleThresholdHours ?? 4
        }
      } catch (e) {
        // 督查规则加载失败不影响其他设置
      }
      // 自动匹配当前平台
      detectPlatform()
    }
  } catch (error) {
    console.error('加载设置失败:', error)
  }
}

/** 根据当前 API URL 自动匹配平台 */
function detectPlatform() {
  const url = claudeForm.value.apiUrl
  if (!url) return
  for (const [key, preset] of Object.entries(PLATFORM_PRESETS)) {
    if (url === preset.url) {
      activePlatform.value = key
      return
    }
  }
  activePlatform.value = ''
}

onMounted(() => {
  loadSettings()
})
</script>

<style scoped>
.settings-page {
  padding: 20px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
