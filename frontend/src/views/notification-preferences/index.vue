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
        <!-- 渠道状态 -->
        <el-alert type="info" :closable="false" class="mb-4">
          <template #title>
            <div class="channel-status">
              <span>通知渠道状态：</span>
              <el-tag :type="channelStatus.IN_APP ? 'success' : 'info'" size="small">站内信</el-tag>
              <el-tag :type="channelStatus.EMAIL ? 'success' : 'info'" size="small">
                邮件 {{ channelStatus.EMAIL ? '已配置' : '未配置' }}
              </el-tag>
              <el-tag :type="channelStatus.FEISHU ? 'success' : 'info'" size="small">
                飞书 {{ channelStatus.FEISHU ? '已配置' : '未配置' }}
              </el-tag>
              <el-tag :type="channelStatus.DINGTALK ? 'success' : 'info'" size="small">
                钉钉 {{ channelStatus.DINGTALK ? '已配置' : '未配置' }}
              </el-tag>
            </div>
          </template>
        </el-alert>

        <!-- 通知偏好矩阵 -->
        <template v-for="(types, category) in groupedTypes" :key="category">
          <h4 class="category-title">{{ category }}</h4>
          <el-table :data="types" stripe size="small" class="pref-table">
            <el-table-column prop="label" label="通知类型" min-width="160">
              <template #default="{ row }">
                <div>
                  <div class="type-label">{{ row.label }}</div>
                  <div class="type-desc">{{ row.description }}</div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="站内信" width="80" align="center">
              <template #default="{ row }">
                <el-switch v-model="prefMatrix[row.key].IN_APP" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="邮件" width="80" align="center">
              <template #default="{ row }">
                <el-switch v-model="prefMatrix[row.key].EMAIL" size="small"
                  :disabled="!channelStatus.EMAIL" />
              </template>
            </el-table-column>
            <el-table-column label="飞书" width="80" align="center">
              <template #default="{ row }">
                <el-switch v-model="prefMatrix[row.key].FEISHU" size="small"
                  :disabled="!channelStatus.FEISHU" />
              </template>
            </el-table-column>
            <el-table-column label="钉钉" width="80" align="center">
              <template #default="{ row }">
                <el-switch v-model="prefMatrix[row.key].DINGTALK" size="small"
                  :disabled="!channelStatus.DINGTALK" />
              </template>
            </el-table-column>
          </el-table>
        </template>

        <!-- 免打扰设置 -->
        <el-divider content-position="left">免打扰时段</el-divider>
        <el-form :inline="true" class="dnd-form">
          <el-form-item label="启用免打扰">
            <el-switch v-model="dndEnabled" />
          </el-form-item>
          <el-form-item label="开始时间" v-if="dndEnabled">
            <el-time-picker v-model="dndStart" format="HH:mm" placeholder="开始时间" style="width: 120px" />
          </el-form-item>
          <el-form-item label="结束时间" v-if="dndEnabled">
            <el-time-picker v-model="dndEnd" format="HH:mm" placeholder="结束时间" style="width: 120px" />
          </el-form-item>
        </el-form>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'

const loading = ref(false)
const saving = ref(false)
const resetting = ref(false)
const groupedTypes = ref({})
const prefMatrix = reactive({})
const channelStatus = reactive({ IN_APP: true, EMAIL: false, FEISHU: false, DINGTALK: false })
const dndEnabled = ref(false)
const dndStart = ref(null)
const dndEnd = ref(null)

const CHANNELS = ['IN_APP', 'EMAIL', 'FEISHU', 'DINGTALK']

const loadPreferences = async () => {
  loading.value = true
  try {
    const data = await api.get('/notification-preferences/api/list')
    const visibleTypes = data.visibleTypes || []
    const userPrefs = data.preferences || []

    // 渠道状态
    channelStatus.IN_APP = true
    channelStatus.EMAIL = data.emailConfigured || false
    channelStatus.FEISHU = data.feishuConfigured || false
    channelStatus.DINGTALK = data.dingtalkConfigured || false

    // 按分类分组
    const grouped = {}
    for (const type of visibleTypes) {
      if (!grouped[type.category]) grouped[type.category] = []
      grouped[type.category].push(type)
    }
    groupedTypes.value = grouped

    // 初始化偏好矩阵
    for (const type of visibleTypes) {
      prefMatrix[type.key] = {}
      for (const ch of CHANNELS) {
        prefMatrix[type.key][ch] = false
      }
    }

    // 填入用户已有偏好
    for (const pref of userPrefs) {
      if (prefMatrix[pref.notificationType]) {
        prefMatrix[pref.notificationType][pref.channel] = pref.enabled
      }
    }

    // 免打扰
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
    // 构建偏好列表
    const preferences = []
    for (const [typeKey, channels] of Object.entries(prefMatrix)) {
      for (const [ch, enabled] of Object.entries(channels)) {
        preferences.push({ notificationType: typeKey, channel: ch, enabled })
      }
    }

    await api.post('/notification-preferences/api/batch-update', {
      preferences,
      dndEnabled: dndEnabled.value,
      dndStart: dndStart.value ? formatTime(dndStart.value) : null,
      dndEnd: dndEnd.value ? formatTime(dndEnd.value) : null
    })
    ElMessage.success('保存成功')
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const formatTime = (date) => {
  return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
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

onMounted(() => loadPreferences())
</script>

<style scoped>
.notification-preferences-page { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; }
.header-actions { display: flex; gap: 8px; }
.channel-status { display: flex; align-items: center; gap: 8px; }
.mb-4 { margin-bottom: 16px; }
.category-title { margin: 16px 0 8px; color: var(--el-text-color-primary); font-size: 15px; }
.pref-table { margin-bottom: 8px; }
.type-label { font-size: 13px; }
.type-desc { font-size: 11px; color: #909399; margin-top: 2px; }
.dnd-form { margin-top: 16px; }
</style>
