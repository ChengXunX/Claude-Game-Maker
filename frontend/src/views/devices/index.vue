<template>
  <div class="devices-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>可信设备管理</span>
          <el-button type="danger" size="small" @click="handleRemoveAll" :disabled="devices.length === 0">
            移除所有设备
          </el-button>
        </div>
      </template>

      <el-table :data="devices" v-loading="loading" stripe>
        <el-table-column label="设备信息" min-width="200">
          <template #default="{ row }">
            <div>
              <el-icon><Cellphone /></el-icon>
              <span style="margin-left: 8px">{{ row.deviceName || '未知设备' }}</span>
            </div>
            <div class="device-detail">{{ row.userAgent }}</div>
          </template>
        </el-table-column>
        <el-table-column label="IP地址" width="150">
          <template #default="{ row }">
            {{ row.ipAddress || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="最后使用" width="180">
          <template #default="{ row }">
            {{ formatTime(row.lastUsedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="信任状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getTrustStatus(row).type" size="small">
              {{ getTrustStatus(row).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" size="small" text @click="handleRemove(row)">
              移除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && devices.length === 0" description="暂无可信设备" />
    </el-card>
  </div>
</template>

<script setup>
/**
 * 设备信任页面
 * 管理用户的可信设备列表
 *
 * 操作维度：用户级
 * 权限要求：登录用户即可
 */
import { ref, onMounted } from 'vue'
import { deviceApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const devices = ref([])

/** 格式化时间 */
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

/** 获取信任状态 */
const getTrustStatus = (row) => {
  if (!row.trustedAt) return { type: 'info', label: '未知' }
  if (row.expiresAt && new Date(row.expiresAt) < new Date()) return { type: 'warning', label: '已过期' }
  return { type: 'success', label: '可信' }
}

/** 加载设备列表 */
const loadDevices = async () => {
  loading.value = true
  try {
    const data = await deviceApi.getAll()
    devices.value = data || []
  } catch (error) {
    ElMessage.error('加载设备列表失败')
  } finally {
    loading.value = false
  }
}

/** 移除单个设备 */
const handleRemove = async (device) => {
  try {
    await ElMessageBox.confirm(
      '确定要移除此设备的信任关系吗？移除后该设备需要重新验证。',
      '移除确认',
      { confirmButtonText: '移除', cancelButtonText: '取消', type: 'warning' }
    )

    await deviceApi.remove(device.id)
    ElMessage.success('设备已移除')
    loadDevices()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('移除失败')
    }
  }
}

/** 移除所有设备 */
const handleRemoveAll = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要移除所有可信设备吗？移除后所有设备需要重新验证。',
      '移除确认',
      { confirmButtonText: '全部移除', cancelButtonText: '取消', type: 'warning' }
    )

    await deviceApi.removeAll()
    ElMessage.success('所有设备已移除')
    loadDevices()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('移除失败')
    }
  }
}

onMounted(() => {
  loadDevices()
})
</script>

<style scoped>
.devices-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.device-detail {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
</style>
