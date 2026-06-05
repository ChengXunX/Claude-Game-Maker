<template>
  <div class="password-page">
    <el-card>
      <template #header>
        <span>修改密码</span>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        style="max-width: 500px"
      >
        <el-form-item label="当前密码" prop="currentPassword">
          <el-input
            v-model="form.currentPassword"
            type="password"
            show-password
            placeholder="请输入当前密码"
          />
        </el-form-item>

        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="form.newPassword"
            type="password"
            show-password
            placeholder="请输入新密码"
          />
          <div class="password-strength" v-if="form.newPassword">
            <span>密码强度：</span>
            <el-tag :type="strengthType" size="small">{{ strengthLabel }}</el-tag>
          </div>
        </el-form-item>

        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入新密码"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="loading">
            修改密码
          </el-button>
          <el-button @click="router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
/**
 * 修改密码页面
 * 用户修改自己的登录密码
 *
 * 操作维度：用户级
 * 权限要求：登录用户即可
 * 密码规则：至少8位，包含大小写字母和数字
 */
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)

/** 表单数据 */
const form = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

/** 密码强度计算 */
const passwordStrength = computed(() => {
  const pwd = form.value.newPassword
  if (!pwd) return 0
  let score = 0
  if (pwd.length >= 8) score++
  if (pwd.length >= 12) score++
  if (/[A-Z]/.test(pwd)) score++
  if (/[a-z]/.test(pwd)) score++
  if (/[0-9]/.test(pwd)) score++
  if (/[^A-Za-z0-9]/.test(pwd)) score++
  return score
})

const strengthType = computed(() => {
  if (passwordStrength.value <= 2) return 'danger'
  if (passwordStrength.value <= 4) return 'warning'
  return 'success'
})

const strengthLabel = computed(() => {
  if (passwordStrength.value <= 2) return '弱'
  if (passwordStrength.value <= 4) return '中'
  return '强'
})

/** 确认密码校验 */
const validateConfirm = (rule, value, callback) => {
  if (value !== form.value.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

/** 表单验证规则 */
const rules = {
  currentPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '密码长度不能少于8个字符', trigger: 'blur' },
    { pattern: /[A-Z]/, message: '密码必须包含至少一个大写字母', trigger: 'blur' },
    { pattern: /[a-z]/, message: '密码必须包含至少一个小写字母', trigger: 'blur' },
    { pattern: /[0-9]/, message: '密码必须包含至少一个数字', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

/** 提交修改 */
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    loading.value = true

    await userStore.changePassword({
      currentPassword: form.value.currentPassword,
      newPassword: form.value.newPassword
    })

    ElMessage.success('密码修改成功，请重新登录')
    await userStore.logout()
    router.push('/login')
  } catch (error) {
    if (error !== false) {
      ElMessage.error(error?.response?.data?.message || '密码修改失败')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.password-page {
  padding: 20px;
  max-width: 800px;
}

.password-strength {
  margin-top: 8px;
  font-size: 12px;
  color: #666;
}
</style>
