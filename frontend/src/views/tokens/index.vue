<template>
  <div class="tokens-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>Token 管理</span>
          <el-button type="primary" @click="router.push('/tokens/create')">
            <el-icon><Plus /></el-icon> 创建 Token
          </el-button>
        </div>
      </template>

      <el-table :data="tokens" v-loading="loading" stripe>
        <el-table-column prop="name" label="名称" width="150" />
        <el-table-column label="API Key" width="200">
          <template #default="{ row }">
            <span class="api-key">{{ row.maskedApiKey || '••••••••' }}</span>
            <el-button size="small" text @click="handleCopyKey(row)" class="copy-btn">
              <el-icon><CopyDocument /></el-icon>
            </el-button>
          </template>
        </el-table-column>
        <el-table-column prop="apiUrl" label="API URL" min-width="180" show-overflow-tooltip />
        <el-table-column prop="model" label="模型" width="120" />
        <el-table-column label="适用角色" min-width="150">
          <template #default="{ row }">
            <template v-if="row.agentTags">
              <el-tag
                v-for="tag in parseTags(row.agentTags)"
                :key="tag"
                size="small"
                class="tag-item"
              >
                {{ getRoleLabel(tag) }}
              </el-tag>
            </template>
            <span v-else class="text-muted">通用</span>
          </template>
        </el-table-column>
        <el-table-column label="优先级" width="80">
          <template #default="{ row }">
            <el-tag :type="getPriorityType(row.priority)" size="small">
              {{ row.priority || 10 }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
              {{ row.status === 'ACTIVE' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="绑定 Agent" width="120">
          <template #default="{ row }">
            <span v-if="row.assignedAgentName">{{ row.assignedAgentName }}</span>
            <span v-else class="text-muted">未绑定</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="handleEdit(row)">编辑</el-button>
            <el-button type="success" size="small" text @click="handleAssign(row)">分配</el-button>
            <el-button type="danger" size="small" text @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tokens.length === 0" description="暂无 Token" />
    </el-card>

    <!-- 分配对话框 -->
    <el-dialog v-model="assignDialogVisible" title="分配 Token 给 Agent" width="500px">
      <el-form :model="assignForm" label-width="120px">
        <el-form-item label="Token">
          <el-input :value="assignForm.tokenName" disabled />
        </el-form-item>
        <el-form-item label="Agent ID" required>
          <el-input v-model="assignForm.agentId" placeholder="输入 Agent ID" />
        </el-form-item>
        <el-form-item label="生效方式">
          <el-radio-group v-model="assignForm.activation">
            <el-radio value="immediate">
              <div>
                <div>立即生效</div>
                <div class="radio-desc">重启 Agent 进程，立即使用新的 API 配置</div>
              </div>
            </el-radio>
            <el-radio value="pending">
              <div>
                <div>等待任务完成</div>
                <div class="radio-desc">Agent 完成当前任务后自动应用新配置</div>
              </div>
            </el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAssignSubmit" :loading="assigning">确认分配</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * Token 管理页面
 * 管理 API Token，用于 Claude CLI 调用
 *
 * 操作维度：系统级
 * 权限要求：系统管理员
 */
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { tokenApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

const loading = ref(false)
const tokens = ref([])

/** 分配对话框 */
const assignDialogVisible = ref(false)
const assigning = ref(false)
const assignForm = ref({
  tokenId: null,
  tokenName: '',
  agentId: '',
  activation: 'immediate'
})

/** 格式化时间 */
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

/** 解析标签 */
const parseTags = (tags) => {
  if (!tags) return []
  return tags.split(',').map(t => t.trim()).filter(t => t)
}

/** 获取角色标签 */
const getRoleLabel = (role) => {
  const labelMap = {
    'server-dev': '服务端',
    'client-dev': '客户端',
    'ui-dev': 'UI开发',
    'system-planner': '系统策划',
    'numerical-planner': '数值策划',
    'git-commit': 'Git专员',
    'producer': '制作人'
  }
  return labelMap[role] || role
}

/** 获取优先级类型 */
const getPriorityType = (priority) => {
  if (!priority || priority >= 10) return 'info'
  if (priority <= 3) return 'danger'
  if (priority <= 6) return 'warning'
  return 'success'
}

/** 加载 Token 列表 */
const loadTokens = async () => {
  loading.value = true
  try {
    const data = await tokenApi.getAll()
    tokens.value = data || []
  } catch (error) {
    ElMessage.error('加载 Token 列表失败')
  } finally {
    loading.value = false
  }
}

/** 复制 API Key */
const handleCopyKey = async (token) => {
  try {
    await navigator.clipboard.writeText(token.apiKey || token.maskedApiKey)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败')
  }
}

/** 编辑 Token */
const handleEdit = (token) => {
  router.push(`/tokens/create?id=${token.id}`)
}

/** 打开分配对话框 */
const handleAssign = (token) => {
  assignForm.value = {
    tokenId: token.id,
    tokenName: token.name,
    agentId: token.assignedAgentId || '',
    activation: 'immediate'
  }
  assignDialogVisible.value = true
}

/** 提交分配 */
const handleAssignSubmit = async () => {
  if (!assignForm.value.agentId) {
    ElMessage.warning('请输入 Agent ID')
    return
  }

  assigning.value = true
  try {
    await tokenApi.assign(
      assignForm.value.tokenId,
      assignForm.value.agentId,
      assignForm.value.activation
    )
    const activationLabel = assignForm.value.activation === 'pending' ? '等待任务完成' : '立即'
    ElMessage.success(`Token 已分配（${activationLabel}生效）`)
    assignDialogVisible.value = false
    loadTokens()
  } catch (error) {
    ElMessage.error('分配失败')
  } finally {
    assigning.value = false
  }
}

/** 切换状态 */
const handleToggleStatus = async (token) => {
  const newStatus = token.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  const action = newStatus === 'ACTIVE' ? '启用' : '禁用'

  try {
    await ElMessageBox.confirm(
      `确定要${action} Token "${token.name}" 吗？`,
      '确认操作',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )

    await tokenApi.update(token.id, { status: newStatus })
    ElMessage.success(`Token 已${action}`)
    loadTokens()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

/** 删除 Token */
const handleDelete = async (token) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除 Token "${token.name}" 吗？删除后无法恢复。`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )

    await tokenApi.delete(token.id)
    ElMessage.success('Token 已删除')
    loadTokens()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadTokens()
})
</script>

<style scoped>
.tokens-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.api-key {
  font-family: monospace;
  font-size: 12px;
  word-break: break-all;
}

.copy-btn {
  margin-left: 4px;
}

.tag-item {
  margin-right: 4px;
  margin-bottom: 4px;
}

.text-muted {
  color: #909399;
  font-size: 12px;
}

.radio-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

:deep(.el-radio) {
  display: flex;
  align-items: flex-start;
  margin-bottom: 12px;
  height: auto;
}

:deep(.el-radio__label) {
  white-space: normal;
}

/* 手机端 */
@media (max-width: 767px) {
  .tokens-page {
    padding: 12px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .api-key {
    font-size: 10px;
  }
}
</style>
