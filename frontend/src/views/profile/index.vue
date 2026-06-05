<template>
  <div class="profile-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>个人资料</span>
          <el-button type="primary" @click="handleEdit" v-if="!isEditing">
            <el-icon><Edit /></el-icon> 编辑
          </el-button>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        :disabled="!isEditing"
      >
        <el-form-item label="用户名">
          <el-input v-model="form.username" disabled />
        </el-form-item>

        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>

        <el-form-item label="头像URL" prop="avatar">
          <el-input v-model="form.avatar" placeholder="请输入头像URL" />
        </el-form-item>

        <el-form-item label="角色">
          <el-tag :type="roleTagType">{{ form.roleName }}</el-tag>
        </el-form-item>

        <el-form-item label="状态">
          <el-tag type="success">正常</el-tag>
        </el-form-item>

        <el-form-item v-if="isEditing">
          <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
          <el-button @click="handleCancel">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 修改密码入口 -->
    <el-card class="mt-4">
      <template #header>
        <span>账户安全</span>
      </template>
      <el-button type="warning" @click="router.push('/profile/password')">
        <el-icon><Lock /></el-icon> 修改密码
      </el-button>
    </el-card>
  </div>
</template>

<script setup>
/**
 * 个人资料页面
 * 用户可以查看和编辑自己的基本信息
 *
 * 操作维度：用户级（只能编辑自己的资料）
 * 权限要求：登录用户即可
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const isEditing = ref(false)
const saving = ref(false)

/** 表单数据 */
const form = ref({
  username: '',
  nickname: '',
  email: '',
  avatar: '',
  roleName: ''
})

/** 表单验证规则 */
const rules = {
  nickname: [
    { max: 50, message: '昵称长度不能超过50个字符', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

/** 角色标签类型 */
const roleTagType = computed(() => {
  const role = form.value.roleName
  if (role === 'ADMIN' || role === '超级管理员') return 'danger'
  if (role === 'PROJECT_MANAGER' || role === '项目经理') return 'warning'
  return 'info'
})

/** 加载用户信息 */
const loadUserInfo = () => {
  const user = userStore.userInfo
  if (user) {
    form.value = {
      username: user.username || '',
      nickname: user.nickname || '',
      email: user.email || '',
      avatar: user.avatar || '',
      roleName: user.role?.displayName || user.role?.name || ''
    }
  }
}

/** 进入编辑模式 */
const handleEdit = () => {
  isEditing.value = true
}

/** 取消编辑 */
const handleCancel = () => {
  isEditing.value = false
  loadUserInfo()
}

/** 保存资料 */
const handleSave = async () => {
  try {
    await formRef.value.validate()
    saving.value = true

    await authApi.updateProfile({
      nickname: form.value.nickname,
      email: form.value.email,
      avatar: form.value.avatar
    })

    await userStore.getUserInfo()
    loadUserInfo()

    isEditing.value = false
    ElMessage.success('资料更新成功')
  } catch (error) {
    if (error !== false) {
      ElMessage.error('保存失败')
    }
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.profile-page {
  padding: 20px;
  max-width: 800px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.mt-4 {
  margin-top: 16px;
}

/* 手机端 */
@media (max-width: 767px) {
  .profile-page {
    padding: 12px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  /* 表单标签宽度调整 */
  :deep(.el-form-item__label) {
    float: none;
    display: block;
    text-align: left;
    padding: 0 0 8px 0;
  }

  :deep(.el-form-item__content) {
    margin-left: 0 !important;
  }
}
</style>
