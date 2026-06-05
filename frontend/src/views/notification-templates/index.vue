<template>
  <div class="templates-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>通知模板</span>
          <el-button type="primary" @click="handleCreate" v-permission="'notification:manage'">
            <el-icon><Plus /></el-icon> 创建模板
          </el-button>
        </div>
      </template>

      <el-table :data="templates" v-loading="loading" stripe>
        <el-table-column prop="templateCode" label="模板编码" width="150" show-overflow-tooltip />
        <el-table-column prop="templateName" label="模板名称" width="150" />
        <el-table-column prop="channel" label="通知渠道" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.channel || 'SYSTEM' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="titleTemplate" label="标题模板" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
              {{ row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" text @click="handleDelete(row)" v-permission="'notification:manage'">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && templates.length === 0" description="暂无通知模板" />
    </el-card>
  </div>
</template>

<script setup>
/**
 * 通知模板页面
 * 管理通知模板
 *
 * 操作维度：系统级
 * 权限要求：系统管理员
 */
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { templateApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

const loading = ref(false)
const templates = ref([])

/** 加载模板列表 */
const loadTemplates = async () => {
  loading.value = true
  try {
    const data = await templateApi.getAll()
    templates.value = data || []
  } catch (error) {
    ElMessage.error('加载模板列表失败')
  } finally {
    loading.value = false
  }
}

/** 创建模板 */
const handleCreate = () => {
  router.push('/notification-templates/create')
}

/** 编辑模板 */
const handleEdit = (template) => {
  router.push(`/notification-templates/${template.id}`)
}

/** 删除模板 */
const handleDelete = async (template) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除模板 "${template.templateName}" 吗？`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )

    await templateApi.delete(template.id)
    ElMessage.success('模板已删除')
    loadTemplates()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadTemplates()
})
</script>

<style scoped>
.templates-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
