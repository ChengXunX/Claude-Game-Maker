<template>
  <div class="dingtalk-page">
    <el-card>
      <template #header>
        <span>钉钉集成配置</span>
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
            placeholder="请输入钉钉机器人 Webhook URL"
          />
        </el-form-item>

        <el-form-item label="签名密钥" prop="secret">
          <el-input
            v-model="form.secret"
            type="password"
            show-password
            placeholder="请输入钉钉机器人签名密钥（可选）"
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
          1. 在钉钉群中添加自定义机器人<br>
          2. 复制 Webhook URL 填入上方表单<br>
          3. 如需签名验证，填入签名密钥<br>
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
 * 钉钉配置页面
 * 配置钉钉机器人集成，用于接收系统通知
 *
 * 操作维度：系统级
 * 权限要求：系统管理员
 */
import { ref, onMounted } from 'vue'
import { dingtalkApi } from '@/api'
import { ElMessage } from 'element-plus'

const formRef = ref(null)
const loading = ref(false)
const saving = ref(false)
const testing = ref(false)

/** 表单数据 */
const form = ref({
  enabled: false,
  webhookUrl: '',
  secret: ''
})

/** 表单验证规则 */
const rules = {
  webhookUrl: [
    { required: true, message: '请输入 Webhook URL', trigger: 'blur' },
    { pattern: /^https?:\/\/.+/, message: '请输入有效的 URL', trigger: 'blur' }
  ]
}

/** 加载配置 */
const loadConfig = async () => {
  loading.value = true
  try {
    const data = await dingtalkApi.getConfig()
    if (data) {
      form.value = {
        enabled: data.enabled || false,
        webhookUrl: data.webhookUrl || '',
        secret: data.secret || ''
      }
    }
  } catch (error) {
    // 首次访问可能没有配置，忽略错误
  } finally {
    loading.value = false
  }
}

/** 保存配置 */
const handleSave = async () => {
  try {
    await formRef.value.validate()
    saving.value = true

    await dingtalkApi.saveConfig(form.value)
    ElMessage.success('配置保存成功')
  } catch (error) {
    if (error !== false) {
      ElMessage.error('保存失败')
    }
  } finally {
    saving.value = false
  }
}

/** 测试连接 */
const handleTest = async () => {
  testing.value = true
  try {
    await dingtalkApi.testConnection()
    ElMessage.success('连接测试成功')
  } catch (error) {
    ElMessage.error('连接测试失败，请检查配置')
  } finally {
    testing.value = false
  }
}

onMounted(() => {
  loadConfig()
})
</script>

<style scoped>
.dingtalk-page {
  padding: 20px;
  max-width: 900px;
}

.mt-4 {
  margin-top: 16px;
}
</style>
