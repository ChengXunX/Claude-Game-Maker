<template>
  <div class="permissions-page">
    <el-tabs v-model="activeTab" @tab-click="handleTabClick">
      <!-- 权限申请 -->
      <el-tab-pane label="权限申请" name="request">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>我的权限申请</span>
              <el-button type="primary" @click="handleRequestPermission">
                <el-icon><Plus /></el-icon> 申请权限
              </el-button>
            </div>
          </template>

          <el-table :data="myRequests" v-loading="loading" stripe>
            <el-table-column prop="permission" label="权限" width="200" />
            <el-table-column prop="reason" label="申请原因" min-width="200" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" size="small">
                  {{ getStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="申请时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === 'PENDING'"
                  type="danger"
                  size="small"
                  text
                  @click="handleCancel(row)"
                >
                  撤销
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 权限审批（管理员） -->
      <el-tab-pane label="权限审批" name="approval" v-if="isAdmin">
        <el-card>
          <template #header>
            <span>待审批权限申请</span>
          </template>

          <el-table :data="pendingRequests" v-loading="loading" stripe>
            <el-table-column prop="username" label="用户" width="120" />
            <el-table-column prop="permission" label="权限" width="200" />
            <el-table-column prop="reason" label="申请原因" min-width="200" />
            <el-table-column prop="createdAt" label="申请时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button type="success" size="small" @click="handleApprove(row)">
                  批准
                </el-button>
                <el-button type="danger" size="small" @click="handleReject(row)">
                  拒绝
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 用户权限管理（管理员） -->
      <el-tab-pane label="用户权限" name="users" v-if="isAdmin">
        <el-card>
          <template #header>
            <span>用户权限管理</span>
          </template>

          <el-table :data="users" v-loading="loading" stripe>
            <el-table-column prop="username" label="用户名" width="120" />
            <el-table-column prop="nickname" label="昵称" width="120" />
            <el-table-column prop="role" label="角色" width="100">
              <template #default="{ row }">
                <el-tag size="small">{{ row.role?.displayName || row.role?.name || '-' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="额外权限" min-width="200">
              <template #default="{ row }">
                <el-tag v-for="perm in row.extraPermissions" :key="perm" size="small" class="perm-tag">
                  {{ perm }}
                </el-tag>
                <span v-if="!row.extraPermissions?.length" class="text-muted">无</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button type="primary" size="small" text @click="handleManageUser(row)">
                  管理权限
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 权限定义（管理员） -->
      <el-tab-pane label="权限定义" name="definitions" v-if="isAdmin">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>权限定义</span>
              <el-button type="primary" @click="handleCreateDefinition">
                <el-icon><Plus /></el-icon> 新增权限
              </el-button>
            </div>
          </template>

          <el-table :data="definitions" v-loading="loading" stripe>
            <el-table-column prop="permission" label="权限标识" width="200" />
            <el-table-column prop="name" label="名称" width="150" />
            <el-table-column prop="description" label="描述" min-width="200" />
            <el-table-column prop="category" label="分类" width="100" />
            <el-table-column prop="enabled" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
                  {{ row.enabled ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button type="warning" size="small" text @click="handleEditDefinition(row)">
                  编辑
                </el-button>
                <el-button type="primary" size="small" text @click="handleToggleDefinition(row)">
                  {{ row.enabled ? '禁用' : '启用' }}
                </el-button>
                <el-button type="danger" size="small" text @click="handleDeleteDefinition(row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 申请权限对话框 -->
    <el-dialog v-model="requestDialogVisible" title="申请权限" width="500px">
      <el-form :model="requestForm" label-width="100px">
        <el-form-item label="权限">
          <el-select v-model="requestForm.permission" placeholder="选择权限" filterable>
            <el-option v-for="perm in availablePermissions" :key="perm.permission" :label="perm.name" :value="perm.permission" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请原因">
          <el-input v-model="requestForm.reason" type="textarea" :rows="3" placeholder="请说明申请原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="requestDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitRequest">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 用户权限管理对话框 -->
    <el-dialog v-model="userPermDialogVisible" title="用户权限管理" width="600px">
      <el-descriptions :column="2" border class="mb-4">
        <el-descriptions-item label="用户名">{{ currentUser?.username }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ currentUser?.role?.displayName || currentUser?.role?.name || '-' }}</el-descriptions-item>
      </el-descriptions>

      <h4>额外权限</h4>
      <div class="user-perm-list">
        <el-checkbox-group v-model="userPermissions">
          <div v-for="perm in allPermissions" :key="perm.permission" class="perm-checkbox">
            <el-checkbox :value="perm.permission">
              {{ perm.name }}
              <span class="perm-desc">({{ perm.description }})</span>
            </el-checkbox>
          </div>
        </el-checkbox-group>
      </div>

      <template #footer>
        <el-button @click="userPermDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveUserPermissions" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 创建权限定义对话框 -->
    <el-dialog v-model="defDialogVisible" :title="isEditDef ? '编辑权限' : '新增权限'" width="500px">
      <el-form ref="defFormRef" :model="defForm" :rules="defRules" label-width="100px">
        <el-form-item label="权限标识" prop="permission">
          <el-input v-model="defForm.permission" placeholder="如：projects:manage" :disabled="isEditDef" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="defForm.name" placeholder="如：项目管理" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="defForm.description" type="textarea" :rows="2" placeholder="权限描述" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="defForm.category" placeholder="选择分类">
            <el-option label="Agent" value="Agent" />
            <el-option label="项目" value="项目" />
            <el-option label="系统" value="系统" />
            <el-option label="管理" value="管理" />
            <el-option label="通知" value="通知" />
            <el-option label="AI" value="AI" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="defForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="defDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitDefinition" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 权限管理页面
 * 权限申请、审批、用户权限管理、权限定义
 *
 * 权限要求：权限申请需要登录，审批和管理需要admin:manage
 */
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.isAdmin())

const activeTab = ref('request')
const loading = ref(false)
const myRequests = ref([])
const pendingRequests = ref([])
const users = ref([])
const definitions = ref([])
const availablePermissions = ref([])

const requestDialogVisible = ref(false)
const requestForm = ref({
  permission: '',
  reason: ''
})

const userPermDialogVisible = ref(false)
const currentUser = ref(null)
const userPermissions = ref([])
const allPermissions = ref([])

const defDialogVisible = ref(false)
const isEditDef = ref(false)
const defFormRef = ref(null)
const saving = ref(false)
const defForm = ref({
  permission: '',
  name: '',
  description: '',
  category: '',
  enabled: true
})
const defRules = {
  permission: [{ required: true, message: '请输入权限标识', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

const getStatusType = (status) => {
  const map = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }
  return map[status] || 'info'
}

const getStatusLabel = (status) => {
  const map = { PENDING: '待审批', APPROVED: '已批准', REJECTED: '已拒绝' }
  return map[status] || status
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

const handleTabClick = () => {
  loadData()
}

const loadData = async () => {
  loading.value = true
  try {
    if (activeTab.value === 'request') {
      const data = await api.get('/permissions/api/user/' + userStore.userId)
      myRequests.value = data || []
    } else if (activeTab.value === 'approval') {
      const pendingData = await api.get('/permissions/api/pending-count')
      pendingRequests.value = pendingData.requests || []
    } else if (activeTab.value === 'users') {
      const data = await api.get('/permissions/admin/users')
      users.value = data || []
    } else if (activeTab.value === 'definitions') {
      const data = await api.get('/permissions/api/definitions')
      definitions.value = data || []
    }
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const loadAvailablePermissions = async () => {
  try {
    const data = await api.get('/permissions/api/available')
    availablePermissions.value = data || []
  } catch (error) {
    console.error('加载可用权限失败')
  }
}

const handleRequestPermission = () => {
  requestForm.value = { permission: '', reason: '' }
  requestDialogVisible.value = true
  loadAvailablePermissions()
}

const handleSubmitRequest = async () => {
  if (!requestForm.value.permission || !requestForm.value.reason) {
    ElMessage.warning('请填写完整信息')
    return
  }

  try {
    await api.post('/permissions/api/request', requestForm.value)
    ElMessage.success('申请已提交')
    requestDialogVisible.value = false
    loadData()
  } catch (error) {
    ElMessage.error('提交失败')
  }
}

const handleCancel = async (request) => {
  try {
    await ElMessageBox.confirm('确定要撤销此申请吗？', '撤销确认')
    await api.post(`/permissions/api/request/${request.id}/cancel`)
    ElMessage.success('已撤销')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('撤销失败')
    }
  }
}

const handleApprove = async (request) => {
  try {
    await api.post(`/permissions/api/approve/${request.id}`)
    ElMessage.success('已批准')
    loadData()
  } catch (error) {
    ElMessage.error('批准失败')
  }
}

const handleReject = async (request) => {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝申请')
    await api.post(`/permissions/api/reject/${request.id}`, { reason })
    ElMessage.success('已拒绝')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('拒绝失败')
    }
  }
}

const handleManageUser = async (user) => {
  currentUser.value = user
  userPermissions.value = user.extraPermissions ? [...user.extraPermissions] : []
  userPermDialogVisible.value = true

  // 加载所有权限定义
  try {
    const data = await api.get('/permissions/api/definitions')
    allPermissions.value = data || []
  } catch (error) {
    console.error('加载权限定义失败')
  }
}

const handleSaveUserPermissions = async () => {
  saving.value = true
  try {
    // 先撤销所有额外权限
    const currentPerms = currentUser.value.extraPermissions || []
    for (const perm of currentPerms) {
      if (!userPermissions.value.includes(perm)) {
        await api.post('/permissions/api/revoke', { userId: currentUser.value.id, permission: perm })
      }
    }

    // 授予新权限
    for (const perm of userPermissions.value) {
      if (!currentPerms.includes(perm)) {
        await api.post('/permissions/api/grant', { userId: currentUser.value.id, permission: perm })
      }
    }

    ElMessage.success('权限更新成功')
    userPermDialogVisible.value = false
    loadData()
  } catch (error) {
    ElMessage.error('更新失败')
  } finally {
    saving.value = false
  }
}

const handleCreateDefinition = () => {
  isEditDef.value = false
  defForm.value = {
    permission: '',
    name: '',
    description: '',
    category: '',
    enabled: true
  }
  defDialogVisible.value = true
}

const handleEditDefinition = (definition) => {
  isEditDef.value = true
  defForm.value = { ...definition }
  defDialogVisible.value = true
}

const handleSubmitDefinition = async () => {
  try {
    await defFormRef.value.validate()
  } catch (e) {
    return
  }

  saving.value = true
  try {
    const url = isEditDef.value
      ? `/permissions/api/definitions/${defForm.value.id}`
      : '/permissions/api/definitions'

    if (isEditDef.value) {
      await api.put(url, defForm.value)
    } else {
      await api.post(url, defForm.value)
    }
    ElMessage.success(isEditDef.value ? '更新成功' : '创建成功')
    defDialogVisible.value = false
    loadData()
  } catch (error) {
    ElMessage.error('操作失败')
  } finally {
    saving.value = false
  }
}

const handleToggleDefinition = async (definition) => {
  try {
    await api.post(`/permissions/api/definitions/${definition.id}/toggle`)
    ElMessage.success('状态已切换')
    loadData()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const handleDeleteDefinition = async (definition) => {
  try {
    await ElMessageBox.confirm(`确定要删除权限 "${definition.name}" 吗？`, '删除确认')
    await api.delete(`/permissions/api/definitions/${definition.id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.permissions-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.perm-tag {
  margin-right: 4px;
  margin-bottom: 4px;
}

.text-muted {
  color: var(--el-text-color-secondary);
}
</style>
