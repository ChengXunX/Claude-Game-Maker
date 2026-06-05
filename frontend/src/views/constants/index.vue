<template>
  <div class="constants-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>系统常量</span>
          <div class="header-actions">
            <el-button @click="loadConstants" :loading="loading">
              <el-icon><Refresh /></el-icon> 刷新
            </el-button>
            <el-button @click="handleReload" v-permission="'system:manage'">
              <el-icon><RefreshRight /></el-icon> 重载缓存
            </el-button>
            <el-button @click="handleResetAll" type="danger" v-permission="'system:manage'">
              <el-icon><Delete /></el-icon> 重置全部
            </el-button>
          </div>
        </div>
      </template>

      <!-- 分组筛选 -->
      <div class="filter-bar">
        <el-select v-model="filterGroup" placeholder="选择分组" clearable @change="loadConstants">
          <el-option label="全部分组" value="" />
          <el-option v-for="group in groups" :key="group" :label="group" :value="group" />
        </el-select>
      </div>

      <!-- 常量列表 -->
      <el-table :data="constants" v-loading="loading" stripe>
        <el-table-column prop="constantKey" label="配置键" min-width="200" />
        <el-table-column prop="displayName" label="显示名称" width="150" />
        <el-table-column prop="description" label="描述" width="200" show-overflow-tooltip />
        <el-table-column prop="groupName" label="分组" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.groupName || '未分组' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="值" min-width="200">
          <template #default="{ row }">
            <el-input
              v-if="row.editing"
              v-model="row.editValue"
              size="small"
              @keyup.enter="handleSave(row)"
              @blur="handleSave(row)"
            />
            <span v-else @click="handleEdit(row)" class="editable-value">
              {{ row.value || '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="handleEdit(row)" v-permission="'system:manage'">
              编辑
            </el-button>
            <el-button type="warning" size="small" text @click="handleReset(row)" v-permission="'system:manage'">
              重置
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
/**
 * 系统常量页面
 * 管理系统配置常量
 *
 * 权限要求：system:view, system:manage
 */
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'

const loading = ref(false)
const constants = ref([])
const groups = ref([])
const filterGroup = ref('')

const loadConstants = async () => {
  loading.value = true
  try {
    const url = filterGroup.value
      ? `/constants/api/group/${filterGroup.value}`
      : '/constants/api/all'
    const data = await api.get(url) || []
    constants.value = data.map(item => ({ ...item, editing: false, editValue: '' }))

    // 加载分组列表
    groups.value = await api.get('/constants/api/groups') || []
  } catch (error) {
    ElMessage.error('加载系统常量失败')
  } finally {
    loading.value = false
  }
}

const handleEdit = (row) => {
  row.editValue = row.value || ''
  row.editing = true
}

const handleSave = async (row) => {
  try {
    await api.post('/constants/api/update', { key: row.constantKey, value: row.editValue })
    row.value = row.editValue
    row.editing = false
    ElMessage.success('更新成功')
  } catch (error) {
    ElMessage.error('更新失败')
  }
}

const handleReset = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要重置 "${row.constantKey}" 吗？`, '重置确认')
    await api.post(`/constants/api/reset/${row.constantKey}`)
    ElMessage.success('重置成功')
    loadConstants()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('重置失败')
    }
  }
}

const handleReload = async () => {
  try {
    await api.post('/constants/api/reload')
    ElMessage.success('缓存已重载')
    loadConstants()
  } catch (error) {
    ElMessage.error('重载失败')
  }
}

const handleResetAll = async () => {
  try {
    await ElMessageBox.confirm('确定要重置所有系统常量吗？此操作不可恢复！', '重置确认', {
      type: 'warning'
    })
    await api.post('/constants/api/reset-all')
    ElMessage.success('所有常量已重置')
    loadConstants()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('重置失败')
    }
  }
}

onMounted(() => {
  loadConstants()
})
</script>

<style scoped>
.constants-page {
  padding: 20px;
}

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
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.editable-value {
  cursor: pointer;
  color: var(--el-color-primary);
}

.editable-value:hover {
  text-decoration: underline;
}
</style>
