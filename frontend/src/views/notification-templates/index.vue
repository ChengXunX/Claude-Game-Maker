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
        <el-table-column prop="templateCode" label="模板编码" width="180" show-overflow-tooltip />
        <el-table-column prop="templateName" label="模板名称" width="150" />
        <el-table-column label="渠道" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="channelTagType(row.channel)">{{ channelLabel(row.channel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="80">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ categoryLabel(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="subject" label="标题模板" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
              {{ row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="内置" width="60" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.systemBuiltin" size="small" type="warning">是</el-tag>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="handleEdit(row)">编辑</el-button>
            <el-button type="success" size="small" text @click="handleTest(row)" :loading="row.testing">
              测试
            </el-button>
            <el-button v-if="!row.systemBuiltin" type="danger" size="small" text @click="handleDelete(row)" v-permission="'notification:manage'">
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { templateApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const templates = ref([])

const channelLabel = (ch) => ({ EMAIL: '邮件', FEISHU: '飞书', DINGTALK: '钉钉', SYSTEM: '站内' }[ch] || ch)
const channelTagType = (ch) => ({ EMAIL: '', FEISHU: 'success', DINGTALK: 'warning', SYSTEM: 'info' }[ch] || '')
const categoryLabel = (cat) => ({ ALERT: '告警', TASK: '任务', AGENT: 'Agent', SYSTEM: '系统' }[cat] || cat)

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

const handleCreate = () => router.push('/notification-templates/create')
const handleEdit = (t) => router.push(`/notification-templates/${t.id}`)

const handleDelete = async (t) => {
  try {
    await ElMessageBox.confirm(`确定要删除模板 "${t.templateName}" 吗？`, '删除确认', { type: 'warning' })
    await templateApi.delete(t.id)
    ElMessage.success('模板已删除')
    loadTemplates()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

const handleTest = async (t) => {
  try {
    await ElMessageBox.confirm(
      `确定要测试发送模板 "${t.templateName}" 吗？\n\n渠道: ${channelLabel(t.channel)}${t.channel === 'EMAIL' ? '\n将发送到您的邮箱' : ''}`,
      '测试发送确认',
      { type: 'info', confirmButtonText: '发送测试', cancelButtonText: '取消' }
    )

    // 设置测试中状态
    t.testing = true

    const result = await templateApi.testSend(t.id)

    if (result.status === 'success') {
      ElMessage.success(result.message || '测试通知已发送')
    } else {
      ElMessage.warning(result.message || '测试发送完成，请检查配置')
    }
  } catch (error) {
    if (error !== 'cancel') {
      const message = error.response?.data?.message || error.message || '测试发送失败'
      ElMessage.error(message)
    }
  } finally {
    t.testing = false
  }
}

onMounted(() => loadTemplates())
</script>

<style scoped>
.templates-page { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.text-muted { color: #909399; font-size: 12px; }
</style>
