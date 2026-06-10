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
          <div v-if="emailChangePending" class="email-pending">
            <el-tag type="warning" size="small">审批中</el-tag>
            <span class="pending-email">{{ pendingNewEmail }}</span>
            <el-button type="info" size="small" text @click="handleCancelEmailChange">取消申请</el-button>
          </div>
          <el-input v-else v-model="form.email" placeholder="请输入邮箱" />
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

    <!-- 邮箱换绑验证码弹窗 -->
    <EmailChangeDialog
      v-model="showEmailChangeDialog"
      :current-email="originalEmail"
      @success="handleEmailChangeSuccess"
    />
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
import { ElMessage, ElMessageBox } from 'element-plus'
import EmailChangeDialog from '@/components/EmailChangeDialog.vue'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const isEditing = ref(false)
const saving = ref(false)

// 邮箱变更审批
const emailChangePending = ref(false)
const pendingNewEmail = ref('')
const originalEmail = ref('')

// 邮箱换绑验证码弹窗
const showEmailChangeDialog = ref(false)

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
    originalEmail.value = user.email || ''
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

    // 检查邮箱是否变更
    const emailChanged = form.value.email !== originalEmail.value && form.value.email !== ''

    if (emailChanged) {
      // 邮箱变更：检查是否可以走验证码换绑流程
      try {
        const checkRes = await authApi.checkEmailChangeCondition()

        if (checkRes.success && checkRes.canUseVerification) {
          // 可以走验证码换绑流程
          // 先保存其他字段
          await authApi.updateProfile({
            nickname: form.value.nickname,
            avatar: form.value.avatar
          })
          await userStore.getUserInfo()

          // 弹出验证码换绑弹窗
          showEmailChangeDialog.value = true
          isEditing.value = false
          return
        } else {
          // 走审批流程
          await ElMessageBox.confirm(
            '当前邮箱不可用，邮箱变更需要管理员审批，审批通过后新邮箱才会生效。是否继续？',
            '邮箱变更确认',
            { confirmButtonText: '提交审批', cancelButtonText: '取消', type: 'warning' }
          )

          await authApi.requestEmailChange({
            newEmail: form.value.email,
            reason: '用户主动变更邮箱'
          })

          emailChangePending.value = true
          pendingNewEmail.value = form.value.email
          form.value.email = originalEmail.value // 恢复原邮箱显示
          isEditing.value = false
          ElMessage.success('邮箱变更审批已提交，等待管理员审批')
          return
        }
      } catch (e) {
        if (e === 'cancel') return
        throw e
      }
    }

    // 非邮箱变更，直接保存
    await authApi.updateProfile({
      nickname: form.value.nickname,
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

/** 取消邮箱变更申请 */
const handleCancelEmailChange = () => {
  emailChangePending.value = false
  pendingNewEmail.value = ''
  ElMessage.info('邮箱变更申请已取消')
}

/** 邮箱换绑成功回调 */
const handleEmailChangeSuccess = async () => {
  await userStore.getUserInfo()
  loadUserInfo()
  ElMessage.success('邮箱更换成功')
}

onMounted(async () => {
  // 先从后端刷新用户数据，确保显示最新信息（如审批通过后的邮箱）
  try {
    await userStore.getUserInfo()
  } catch (e) {
    // 忽略刷新失败，使用缓存数据
  }
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

  /* 邮箱审批中状态 */
  .email-pending {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 12px;
    background: #fdf6ec;
    border-radius: 4px;
    border: 1px solid #e6a23c;
  }

  .pending-email {
    font-weight: 500;
    color: #606266;
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
