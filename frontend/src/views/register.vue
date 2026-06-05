<template>
  <div class="register-container">
    <div class="register-card">
      <div class="register-header">
        <h1>ChengXun Game Maker</h1>
        <p>注册新账号</p>
      </div>

      <el-form
        ref="registerFormRef"
        :model="registerForm"
        :rules="rules"
        class="register-form"
        @submit.prevent="handleRegister"
      >
        <el-form-item prop="username">
          <el-input
            v-model="registerForm.username"
            placeholder="用户名（3-50个字符）"
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item prop="nickname">
          <el-input
            v-model="registerForm.nickname"
            placeholder="昵称（可选）"
            size="large"
            :prefix-icon="UserFilled"
          />
        </el-form-item>

        <el-form-item prop="email">
          <el-input
            v-model="registerForm.email"
            placeholder="邮箱地址"
            size="large"
            :prefix-icon="Message"
          >
            <template #append>
              <el-button @click="sendEmailCode" :disabled="emailCodeSent" :loading="sendingCode">
                {{ emailCodeSent ? `${countdown}s后重试` : '发送验证码' }}
              </el-button>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item prop="emailCode" v-if="emailEnabled">
          <el-input
            v-model="registerForm.emailCode"
            placeholder="邮箱验证码"
            size="large"
            :prefix-icon="Key"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="密码（至少8位，包含大小写字母和数字）"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="确认密码"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item prop="captchaCode">
          <div class="captcha-row">
            <el-input
              v-model="registerForm.captchaCode"
              placeholder="验证码"
              size="large"
              :prefix-icon="Key"
            />
            <div class="captcha-image" @click="refreshCaptcha">
              <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
              <span v-else class="captcha-placeholder">点击刷新</span>
            </div>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="register-btn"
            @click="handleRegister"
          >
            {{ loading ? '注册中...' : '注册' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="register-footer">
        <span>已有账号？</span>
        <el-link type="primary" @click="router.push('/login')">返回登录</el-link>
      </div>

      <el-alert type="info" :closable="false" class="mt-4">
        <template #title>
          <span>注册后需要管理员审核才能登录，审核结果将通过邮件通知。</span>
        </template>
      </el-alert>
    </div>
  </div>
</template>

<script setup>
/**
 * 注册页面
 * 用户注册，需要管理员审批
 */
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, UserFilled, Lock, Message, Key } from '@element-plus/icons-vue'
import api from '@/api'

const router = useRouter()

const registerFormRef = ref(null)
const loading = ref(false)
const emailEnabled = ref(false)
const captchaId = ref('')
const captchaImage = ref('')
const emailCodeSent = ref(false)
const countdown = ref(60)
const sendingCode = ref(false)

const registerForm = reactive({
  username: '',
  nickname: '',
  email: '',
  emailCode: '',
  password: '',
  confirmPassword: '',
  captchaCode: ''
})

const validatePassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入密码'))
  } else if (value.length < 8) {
    callback(new Error('密码长度不能少于8位'))
  } else if (!/[A-Z]/.test(value)) {
    callback(new Error('密码必须包含大写字母'))
  } else if (!/[a-z]/.test(value)) {
    callback(new Error('密码必须包含小写字母'))
  } else if (!/[0-9]/.test(value)) {
    callback(new Error('密码必须包含数字'))
  } else {
    callback()
  }
}

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度在3到50个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  password: [
    { required: true, validator: validatePassword, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ],
  captchaCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ]
}

/** 刷新验证码 */
const refreshCaptcha = async () => {
  try {
    const data = await api.get('/auth/captcha')
    captchaId.value = data.captchaId
    captchaImage.value = data.captchaImage
  } catch (error) {
    console.error('获取验证码失败')
  }
}

/** 发送邮箱验证码 */
const sendEmailCode = async () => {
  if (!registerForm.email) {
    ElMessage.warning('请先输入邮箱地址')
    return
  }

  sendingCode.value = true
  try {
    await api.post('/email/send-code', { email: registerForm.email })
    ElMessage.success('验证码已发送到邮箱')
    emailCodeSent.value = true

    // 倒计时
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
        emailCodeSent.value = false
        countdown.value = 60
      }
    }, 1000)
  } catch (error) {
    ElMessage.error('发送验证码失败')
  } finally {
    sendingCode.value = false
  }
}

/** 处理注册 */
const handleRegister = async () => {
  if (!registerFormRef.value) return

  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true

      try {
        await api.post('/auth/register', {
          ...registerForm,
          captchaId: captchaId.value
        })

        ElMessage.success('注册成功，请等待管理员审核')
        router.push('/login')
      } catch (error) {
        ElMessage.error(error.response?.data?.message || '注册失败')
        refreshCaptcha()
      } finally {
        loading.value = false
      }
    }
  })
}

/** 加载配置 */
const loadConfig = async () => {
  try {
    const data = await api.get('/auth/register-config')
    emailEnabled.value = data.emailEnabled || false
  } catch (error) {
    console.error('加载注册配置失败')
  }
}

onMounted(() => {
  refreshCaptcha()
  loadConfig()
})
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  padding: 20px;
}

.register-card {
  width: 100%;
  max-width: 450px;
  background: var(--el-bg-color);
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.register-header {
  text-align: center;
  margin-bottom: 30px;
}

.register-header h1 {
  font-size: 24px;
  margin-bottom: 8px;
  color: var(--el-text-color-primary);
}

.register-header p {
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.register-form {
  margin-top: 20px;
}

.captcha-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.captcha-row .el-input {
  flex: 1;
}

.captcha-image {
  width: 120px;
  height: 40px;
  cursor: pointer;
  border-radius: 4px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-fill-color-light);
}

.captcha-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.captcha-placeholder {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.register-btn {
  width: 100%;
}

.register-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.mt-4 {
  margin-top: 16px;
}
</style>
