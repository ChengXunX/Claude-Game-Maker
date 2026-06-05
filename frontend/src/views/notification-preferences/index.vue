<template>
  <div class="notification-preferences-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>通知偏好设置</span>
          <div class="header-actions">
            <el-button @click="handleReset" :loading="resetting">
              <el-icon><RefreshLeft /></el-icon> 恢复默认
            </el-button>
            <el-button type="primary" @click="handleSave" :loading="saving">
              <el-icon><Check /></el-icon> 保存设置
            </el-button>
          </div>
        </div>
      </template>

      <div v-loading="loading">
        <!-- 渠道配置状态 -->
        <el-alert type="info" :closable="false" class="mb-4">
          <template #title>
            <div class="channel-status">
              <span>通知渠道状态：</span>
              <el-tag :type="emailConfigured ? 'success' : 'info'" size="small">
                邮件 {{ emailConfigured ? '已配置' : '未配置' }}
              </el-tag>
              <el-tag :type="feishuConfigured ? 'success' : 'info'" size="small">
                飞书 {{ feishuConfigured ? '已配置' : '未配置' }}
              </el-tag>
              <el-tag :type="dingtalkConfigured ? 'success' : 'info'" size="small">
                钉钉 {{ dingtalkConfigured ? '已配置' : '未配置' }}
              </el-tag>
            </div>
          </template>
        </el-alert>

        <!-- 通知偏好列表 -->
        <el-table :data="preferences" stripe>
          <el-table-column prop="templateName" label="通知类型" min-width="200" />
          <el-table-column prop="channel" label="渠道" width="100">
            <template #default="{ row }">
              <el-tag size="small">{{ row.channel }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="启用" width="100" align="center">
            <template #default="{ row }">
              <el-switch v-model="row.enabled" />
            </template>
          </el-table-column>
          <el-table-column label="免打扰" width="100" align="center">
            <template #default="{ row }">
              <el-switch v-model="row.dndExempt" />
            </template>
          </el-table-column>
        </el-table>

        <!-- 免打扰设置 -->
        <el-divider content-position="left">免打扰时段</el-divider>
        <el-form :inline="true" class="dnd-form">
          <el-form-item label="启用免打扰">
            <el-switch v-model="dndEnabled" />
          </el-form-item>
          <el-form-item label="开始时间" v-if="dndEnabled">
            <el-time-picker v-model="dndStart" format="HH:mm" placeholder="开始时间" />
          </el-form-item>
          <el-form-item label="结束时间" v-if="dndEnabled">
            <el-time-picker v-model="dndEnd" format="HH:mm" placeholder="结束时间" />
          </el-form-item>
        </el-form>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'

const loading = ref(false)
const saving = ref(false)
const resetting = ref(false)
const preferences = ref([])
const emailConfigured = ref(false)
const feishuConfigured = ref(false)
const dingtalkConfigured = ref(false)
const dndEnabled = ref(false)
const dndStart = ref(null)
const dndEnd = ref(null)

const loadPreferences = async () => {
  loading.value = true
  try {
    const data = await api.get('/notification-preferences/api/list')
    preferences.value = data.preferences || []
    emailConfigured.value = data.emailConfigured || false
    feishuConfigured.value = data.feishuConfigured || false
    dingtalkConfigured.value = data.dingtalkConfigured || false
    dndEnabled.value = data.dndEnabled || false
    dndStart.value = data.dndStart ? new Date(`2000-01-01T${data.dndStart}`) : null
    dndEnd.value = data.dndEnd ? new Date(`2000-01-01T${data.dndEnd}`) : null
  } catch (error) {
    ElMessage.error('加载通知偏好失败')
  } finally {
    loading.value = false
  }
}

const handleSave = async () => {
  saving.value = true
  try {
    await api.post('/notification-preferences/api/batch-update', {
      preferences: preferences.value,
      dndEnabled: dndEnabled.value,
      dndStart: dndStart.value ? `${dndStart.value.getHours().toString().padStart(2, '0')}:${dndStart.value.getMinutes().toString().padStart(2, '0')}` : null,
      dndEnd: dndEnd.value ? `${dndEnd.value.getHours().toString().padStart(2, '0')}:${dndEnd.value.getMinutes().toString().padStart(2, '0')}` : null
    })
    ElMessage.success('保存成功')
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const handleReset = async () => {
  try {
    await ElMessageBox.confirm('确定要恢复默认设置吗？', '恢复确认')
    resetting.value = true
    await api.post('/notification-preferences/api/reset')
    ElMessage.success('已恢复默认设置')
    loadPreferences()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('恢复失败')
  } finally {
    resetting.value = false
  }
}

onMounted(() => {
  loadPreferences()
})
</script>

<style scoped>
.notification-preferences-page {
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

.channel-status {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mb-4 {
  margin-bottom: 16px;
}

.dnd-form {
  margin-top: 16px;
}
</style>
