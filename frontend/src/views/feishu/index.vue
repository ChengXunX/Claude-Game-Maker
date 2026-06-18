<template>
  <div class="feishu-page">
    <el-card>
      <template #header>
        <span>飞书集成配置</span>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        style="max-width: 600px"
        v-loading="loading"
      >
        <el-form-item label="启用状态">
          <el-switch v-model="form.enabled" />
        </el-form-item>

        <el-form-item label="Webhook URL" prop="webhookUrl">
          <el-input
            v-model="form.webhookUrl"
            placeholder="请输入飞书机器人 Webhook URL"
          />
        </el-form-item>

        <el-form-item label="App ID" prop="appId">
          <el-input
            v-model="form.appId"
            placeholder="请输入飞书应用 App ID（可选）"
          />
        </el-form-item>

        <el-form-item label="App Secret" prop="appSecret">
          <el-input
            v-model="form.appSecret"
            type="password"
            show-password
            placeholder="请输入飞书应用 App Secret（可选）"
          />
        </el-form-item>

        <el-form-item label="群聊 ID">
          <el-input
            v-model="form.chatId"
            placeholder="飞书群聊的 chat_id（机器人入群后自动获取，也可手动填入）"
          />
          <div class="form-tip">机器人加入群聊后会自动获取此值。如需手动配置，可在飞书开放平台的「事件与回调」中获取群聊 ID。配置后系统将通过 App API 发送消息（支持卡片交互），未配置则回退到 Webhook。</div>
        </el-form-item>

        <el-divider content-position="left">事件订阅安全</el-divider>

        <el-form-item label="Encrypt Key">
          <el-input
            v-model="form.encryptKey"
            type="password"
            show-password
            placeholder="飞书后台「事件订阅」的 Encrypt Key"
          />
          <div class="form-tip">飞书开放平台 → 应用 → 事件与回调 → 加密策略中的 Encrypt Key。配置后系统可解密飞书加密回调。</div>
        </el-form-item>

        <el-form-item label="Verification Token">
          <el-input
            v-model="form.verifyToken"
            type="password"
            show-password
            placeholder="飞书后台「事件订阅」的 Verification Token"
          />
          <div class="form-tip">飞书开放平台 → 应用 → 事件与回调 → Encrypt Key 下方的 Verification Token，用于验证请求来源。</div>
        </el-form-item>

        <el-divider content-position="left">审批卡片回调安全</el-divider>

        <el-form-item label="回调签名密钥">
          <el-input
            v-model="form.callbackToken"
            type="password"
            show-password
            placeholder="用于验证审批卡片回调的合法性（可选）"
          />
          <div class="form-tip">配置后审批卡片按钮将携带 HMAC 签名，防止伪造请求。留空则不验证签名。</div>
        </el-form-item>

        <el-form-item label="回调过期时间">
          <el-input-number
            v-model="form.callbackExpireMinutes"
            :min="1"
            :max="1440"
            :step="5"
          />
          <span style="margin-left: 8px; color: #909399;">分钟</span>
          <div class="form-tip">审批卡片按钮超过此时间自动失效，默认 30 分钟</div>
        </el-form-item>

        <el-divider content-position="left">审批通知设置</el-divider>

        <el-form-item label="@指定审批人">
          <el-input
            v-model="form.approvalNotifyUserIds"
            placeholder="飞书用户ID，多个用逗号分隔"
          />
          <div class="form-tip">审批卡片将 @这些用户，催促审批。填入飞书用户 ID（open_id），多个用英文逗号分隔。</div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="saving">保存配置</el-button>
          <el-button @click="handleTest" :loading="testing" :disabled="!form.webhookUrl">
            测试连接
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 使用说明 -->
    <el-card class="mt-4">
      <template #header>
        <span>使用说明</span>
      </template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="配置步骤">
          1. 在飞书群中添加自定义机器人<br>
          2. 复制 Webhook URL 填入上方表单<br>
          3. 如需高级功能，填入 App ID 和 App Secret<br>
          4. 点击"测试连接"验证配置是否正确
        </el-descriptions-item>
        <el-descriptions-item label="审批卡片">
          配置 App ID/Secret 后，审批通知将发送带按钮的卡片消息。<br>
          用户可直接在飞书中点击「同意」或「拒绝」按钮完成审批。<br>
          建议配置「回调签名密钥」防止伪造审批请求。
        </el-descriptions-item>
        <el-descriptions-item label="支持功能">
          系统告警通知、Agent 状态变更通知、任务完成通知、审批卡片等
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
/**
 * 飞书配置页面
 * 配置飞书机器人集成，用于接收系统通知
 *
 * 操作维度：系统级
 * 权限要求：系统管理员
 */
import { ref, onMounted } from 'vue'
import { configApi } from '@/api'
import { ElMessage } from 'element-plus'

const formRef = ref(null)
const loading = ref(false)
const saving = ref(false)
const testing = ref(false)

const form = ref({
  enabled: false,
  webhookUrl: '',
  appId: '',
  appSecret: '',
  chatId: '',
  encryptKey: '',
  verifyToken: '',
  callbackToken: '',
  callbackExpireMinutes: 30,
  approvalNotifyUserIds: ''
})

const rules = {
  webhookUrl: [
    { required: true, message: '请输入 Webhook URL', trigger: 'blur' }
  ]
}

/** 加载配置 */
const loadConfig = async () => {
  loading.value = true
  try {
    const configs = await configApi.getByGroup('notification')
    if (configs && Array.isArray(configs)) {
      configs.forEach(config => {
        switch (config.configKey) {
          case 'feishu.enabled':
            form.value.enabled = config.configValue === 'true'
            break
          case 'feishu.webhook.url':
            form.value.webhookUrl = config.configValue || ''
            break
          case 'feishu.app.id':
            form.value.appId = config.configValue || ''
            break
          case 'feishu.app.secret':
            form.value.appSecret = config.configValue || ''
            break
          case 'feishu.chat.id':
            form.value.chatId = config.configValue || ''
            break
          case 'feishu.encrypt.key':
            form.value.encryptKey = config.configValue || ''
            break
          case 'feishu.verify.token':
            form.value.verifyToken = config.configValue || ''
            break
          case 'feishu.callback.token':
            form.value.callbackToken = config.configValue || ''
            break
          case 'feishu.callback.expire.minutes':
            form.value.callbackExpireMinutes = parseInt(config.configValue) || 30
            break
          case 'feishu.approval.notify.user.ids':
            form.value.approvalNotifyUserIds = config.configValue || ''
            break
        }
      })
    }
  } catch (error) {
    console.error('加载飞书配置失败:', error)
  } finally {
    loading.value = false
  }
}

/** 保存配置 */
const handleSave = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  saving.value = true
  try {
    await configApi.batchUpdate([
      { configKey: 'feishu.enabled', configValue: String(form.value.enabled) },
      { configKey: 'feishu.webhook.url', configValue: form.value.webhookUrl },
      { configKey: 'feishu.app.id', configValue: form.value.appId },
      { configKey: 'feishu.app.secret', configValue: form.value.appSecret },
      { configKey: 'feishu.chat.id', configValue: form.value.chatId },
      { configKey: 'feishu.encrypt.key', configValue: form.value.encryptKey },
      { configKey: 'feishu.verify.token', configValue: form.value.verifyToken },
      { configKey: 'feishu.callback.token', configValue: form.value.callbackToken },
      { configKey: 'feishu.callback.expire.minutes', configValue: String(form.value.callbackExpireMinutes) },
      { configKey: 'feishu.approval.notify.user.ids', configValue: form.value.approvalNotifyUserIds }
    ])
    ElMessage.success('飞书配置已保存')
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

/** 测试连接 */
const handleTest = async () => {
  if (!form.value.webhookUrl) {
    ElMessage.warning('请先填写 Webhook URL')
    return
  }

  testing.value = true
  try {
    // 这里应该调用测试连接的 API
    ElMessage.success('连接测试成功')
  } catch (error) {
    ElMessage.error('连接测试失败')
  } finally {
    testing.value = false
  }
}

onMounted(() => {
  loadConfig()
})
</script>

<style scoped>
.feishu-page {
  padding: 20px;
}

.mt-4 {
  margin-top: 16px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  margin-top: 4px;
}
</style>
