<template>
  <div class="users-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon> 创建用户
          </el-button>
        </div>
      </template>

      <el-table :data="users" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" width="120">
          <template #default="{ row }">
            <span>{{ row.username }}</span>
            <el-tag v-if="isDefaultUser(row)" type="danger" size="small" class="ms-1">默认</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="email" label="邮箱" width="180" />
        <el-table-column label="角色" width="120">
          <template #default="{ row }">
            <el-tag :type="getRoleType(row.role?.name || row.roleName)" size="small">
              {{ getRoleDisplayName(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <template v-if="isDefaultUser(row)">
              <el-tooltip content="默认用户不可操作" placement="top">
                <span class="text-muted small">系统账号</span>
              </el-tooltip>
            </template>
            <template v-else>
              <el-button type="primary" size="small" text @click="handleEditRole(row)">角色</el-button>
              <el-button
                :type="row.status === 'APPROVED' ? 'warning' : 'success'"
                size="small"
                text
                @click="handleToggleStatus(row)"
              >
                {{ row.status === 'APPROVED' ? '禁用' : '启用' }}
              </el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 修改角色对话框 -->
    <el-dialog v-model="roleDialogVisible" title="修改用户角色" width="400px">
      <el-form label-width="80px">
        <el-form-item label="用户">
          <el-input :value="currentUser?.username" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="selectedRoleId" placeholder="选择角色">
            <el-option v-for="role in roles" :key="role.id" :label="role.displayName || role.name" :value="role.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveRole">保存</el-button>
      </template>
    </el-dialog>

    <!-- 创建用户对话框 -->
    <el-dialog v-model="createDialogVisible" title="创建用户" width="500px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="createForm.username" placeholder="3-50个字符，字母数字下划线" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="createForm.password" type="password" placeholder="至少8位，包含大小写字母和数字" show-password>
            <template #append>
              <el-button @click="generatePassword" title="生成随机密码">
                <el-icon><Key /></el-icon> 生成
              </el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="createForm.confirmPassword" type="password" placeholder="再次输入密码" show-password />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="createForm.email" placeholder="用户邮箱（可选）" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="createForm.nickname" placeholder="用户昵称（可选）" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="createForm.roleId" placeholder="选择角色">
            <el-option v-for="role in roles" :key="role.id" :label="role.displayName || role.name" :value="role.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 用户管理页面
 * 管理系统用户
 *
 * 操作维度：系统级
 * 权限要求：users:manage
 */
import { ref, reactive, onMounted } from 'vue'
import { userApi, roleApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Key } from '@element-plus/icons-vue'

const loading = ref(false)
const users = ref([])
const roles = ref([])

// 默认用户名列表（系统内置账号）
const DEFAULT_USERS = ['admin']

/** 角色对话框 */
const roleDialogVisible = ref(false)
const currentUser = ref(null)
const selectedRoleId = ref(null)

/** 创建用户对话框 */
const createDialogVisible = ref(false)
const createFormRef = ref(null)
const createForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  nickname: '',
  roleId: null
})

const createRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度在3-50个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码长度不能少于8位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== createForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

/** 判断是否是默认用户 */
const isDefaultUser = (user) => {
  return DEFAULT_USERS.includes(user.username?.toLowerCase())
}

/** 获取角色显示名称 */
const getRoleDisplayName = (user) => {
  // 优先使用 role 对象
  if (user.role?.displayName) return user.role.displayName
  if (user.role?.name) return user.role.name
  // 兼容 roleName 字段
  if (user.roleName) return user.roleName
  return '-'
}

/** 获取角色类型 */
const getRoleType = (roleName) => {
  const typeMap = {
    'ADMIN': 'danger',
    'PROJECT_MANAGER': 'warning',
    'DEVELOPER': 'success',
    'USER': 'info'
  }
  return typeMap[roleName] || 'info'
}

/** 获取状态类型 */
const getStatusType = (status) => {
  const typeMap = {
    'APPROVED': 'success',
    'PENDING': 'warning',
    'REJECTED': 'danger',
    'DISABLED': 'info'
  }
  return typeMap[status] || 'info'
}

/** 获取状态标签 */
const getStatusLabel = (status) => {
  const labelMap = {
    'APPROVED': '正常',
    'PENDING': '待审批',
    'REJECTED': '已拒绝',
    'DISABLED': '已禁用'
  }
  return labelMap[status] || status
}

/** 格式化时间 */
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

/** 加载用户列表 */
const loadUsers = async () => {
  loading.value = true
  try {
    const data = await userApi.getAll()
    users.value = data || []
  } catch (error) {
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

/** 加载角色列表 */
const loadRoles = async () => {
  try {
    const data = await roleApi.getAll()
    roles.value = data || []
  } catch (error) {
    console.error('加载角色列表失败', error)
  }
}

/** 打开修改角色对话框 */
const handleEditRole = (user) => {
  currentUser.value = user
  selectedRoleId.value = user.role?.id || null
  roleDialogVisible.value = true
}

/** 保存角色 */
const handleSaveRole = async () => {
  if (!selectedRoleId.value) {
    ElMessage.warning('请选择角色')
    return
  }

  try {
    await userApi.updateRole(currentUser.value.id, selectedRoleId.value)
    ElMessage.success('角色修改成功')
    roleDialogVisible.value = false
    loadUsers()
  } catch (error) {
    ElMessage.error('修改失败')
  }
}

/** 切换用户状态 */
const handleToggleStatus = async (user) => {
  const newStatus = user.status === 'APPROVED' ? 'DISABLED' : 'APPROVED'
  const action = newStatus === 'APPROVED' ? '启用' : '禁用'

  try {
    await ElMessageBox.confirm(`确定要${action}用户 "${user.username}" 吗？`, '确认操作', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    if (newStatus === 'APPROVED') {
      await userApi.approve(user.id)
    } else {
      await userApi.disable(user.id)
    }

    ElMessage.success(`用户已${action}`)
    loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

/** 生成随机密码 */
const generatePassword = () => {
  const length = 16
  const uppercase = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'
  const lowercase = 'abcdefghijklmnopqrstuvwxyz'
  const numbers = '0123456789'
  const special = '!@#$%^&*'
  const allChars = uppercase + lowercase + numbers + special

  // 确保包含各种字符
  let password = ''
  password += uppercase[Math.floor(Math.random() * uppercase.length)]
  password += lowercase[Math.floor(Math.random() * lowercase.length)]
  password += numbers[Math.floor(Math.random() * numbers.length)]
  password += special[Math.floor(Math.random() * special.length)]

  // 填充剩余长度
  for (let i = password.length; i < length; i++) {
    password += allChars[Math.floor(Math.random() * allChars.length)]
  }

  // 打乱顺序
  password = password.split('').sort(() => Math.random() - 0.5).join('')

  createForm.password = password
  createForm.confirmPassword = password
  ElMessage.success('已生成随机密码，请妥善保存')
}

/** 打开创建用户对话框 */
const handleCreate = () => {
  createForm.username = ''
  createForm.password = ''
  createForm.confirmPassword = ''
  createForm.email = ''
  createForm.nickname = ''
  createForm.roleId = null
  createDialogVisible.value = true
}

/** 保存创建用户 */
const handleSaveCreate = async () => {
  try {
    await createFormRef.value.validate()
  } catch {
    return
  }

  try {
    await userApi.create({
      username: createForm.username,
      password: createForm.password,
      email: createForm.email,
      nickname: createForm.nickname,
      roleId: createForm.roleId
    })
    ElMessage.success('用户创建成功')
    createDialogVisible.value = false
    loadUsers()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '创建失败')
  }
}

onMounted(() => {
  loadUsers()
  loadRoles()
})
</script>

<style scoped>
.users-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

/* 手机端 */
@media (max-width: 767px) {
  .users-page {
    padding: 12px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  /* 对话框响应式 */
  :deep(.el-dialog) {
    width: 90% !important;
    margin: 0 auto;
  }
}
</style>
