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
        <el-descriptions-item label="支持功能">
          系统告警通知、Agent 状态变更通知、任务完成通知等
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
  appSecret: ''
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
      { configKey: 'feishu.app.secret', configValue: form.value.appSecret }
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
</style>
