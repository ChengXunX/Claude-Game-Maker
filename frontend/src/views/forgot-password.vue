<template>
  <div class="forgot-container">
    <div class="forgot-card">
      <div class="forgot-header">
        <h1>忘记密码</h1>
        <p>输入您的用户名，系统将发送重置密码邮件</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="forgot-form"
        @submit.prevent="handleSubmit"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="submit-btn"
            @click="handleSubmit"
          >
            发送重置码
          </el-button>
        </el-form-item>
      </el-form>

      <div class="forgot-footer">
        <el-link type="primary" @click="router.push('/login')">返回登录</el-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Message } from '@element-plus/icons-vue'
import api from '@/api'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const step = ref(1)

const form = reactive({
  username: '',
  email: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      if (step.value === 1) {
        // 查询用户邮箱
        const data = await api.post('/auth/check-user', { username: form.username })
        if (data.hasEmail) {
          // 有邮箱，直接发送重置码（不需要用户再输入邮箱）
          await api.post('/auth/forgot-password', {
            username: form.username,
            email: ''  // 后端会使用用户的真实邮箱
          })
          ElMessage.success('重置码已发送到您的邮箱：' + data.email)
          setTimeout(() => router.push('/login'), 3000)
        } else {
          ElMessage.warning('该用户未配置邮箱，请联系管理员')
        }
      }
    } catch (error) {
      ElMessage.error(error.response?.data?.message || '操作失败')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.forgot-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.forgot-card {
  width: 100%;
  max-width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.forgot-header {
  text-align: center;
  margin-bottom: 30px;
}

.forgot-header h1 {
  font-size: 24px;
  color: #333;
  margin-bottom: 8px;
}

.forgot-header p {
  font-size: 14px;
  color: #666;
}

.forgot-form {
  margin-top: 20px;
}

.submit-btn {
  width: 100%;
}

.forgot-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #666;
}
</style>
