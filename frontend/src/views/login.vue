<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <h1>ChengXun Game Maker</h1>
        <p>AI驱动的游戏开发自动化管理系统</p>
      </div>

      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="rules"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <div class="login-options">
            <el-checkbox v-model="loginForm.remember">记住我</el-checkbox>
            <el-link type="primary" @click="router.push('/forgot-password')">忘记密码？</el-link>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-btn"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <span>还没有账号？</span>
        <router-link to="/register" class="register-link">注册账号</router-link>
        <el-divider direction="vertical" />
        <el-link type="info" @click="contactAdmin">联系管理员</el-link>
      </div>
    </div>

    <!-- 版权和备案信息 -->
    <div class="copyright-footer">
      <span>©{{ currentYear }} {{ systemName }} 版权所有</span>
      <template v-if="icpFilingNumber">
        <span class="copyright-divider">|</span>
        <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer" class="icp-link">
          {{ icpFilingNumber }}
        </a>
      </template>
    </div>

    <!-- 设备验证对话框 -->
    <el-dialog
      v-model="showDeviceVerify"
      title="设备验证"
      width="420px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
    >
      <el-alert type="warning" :closable="false" class="mb-4">
        <template #title>
          <div>
            <p><strong>检测到陌生设备登录</strong></p>
            <p>设备: {{ deviceName }}</p>
            <p>为了您的账号安全，请进行验证。</p>
          </div>
        </template>
      </el-alert>

      <el-form label-width="80px">
        <el-form-item label="验证码">
          <div class="verify-code-row">
            <el-input
              v-model="verifyCode"
              placeholder="请输入邮箱验证码"
              size="large"
            />
            <el-button
              type="primary"
              :disabled="codeSent"
              @click="sendVerifyCode"
              style="margin-left: 8px"
            >
              {{ codeSent ? `${countdown}s` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="信任设备">
          <el-checkbox v-model="trustDevice">
            信任此设备，下次登录不再验证
          </el-checkbox>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="cancelDeviceVerify">取消</el-button>
        <el-button type="primary" @click="handleVerifyDevice" :loading="verifyLoading">
          验证
        </el-button>
      </template>
    </el-dialog>

    <!-- 联系管理员图片弹窗 -->
    <el-dialog
      v-model="showContactImage"
      title="联系管理员"
      width="360px"
    >
      <div style="text-align: center">
        <img :src="contactImageUrl" alt="联系管理员" style="max-width: 100%; border-radius: 8px" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import api, { publicApi } from '@/api'

const router = useRouter()
const userStore = useUserStore()

const loginFormRef = ref(null)
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: '',
  remember: false
})

// 网站公开信息（版权和备案）
const systemName = ref('ChengXun Game Maker')
const icpFilingNumber = ref('')
const currentYear = new Date().getFullYear()

/** 加载网站公开信息 */
const loadSiteInfo = async () => {
  try {
    const data = await publicApi.getSiteInfo()
    if (data.systemName) systemName.value = data.systemName
    if (data.icpFilingNumber) icpFilingNumber.value = data.icpFilingNumber
  } catch (error) {
    console.error('加载网站信息失败:', error)
  }
}

// 联系管理员
const showContactImage = ref(false)
const contactImageUrl = ref('')

const contactAdmin = async () => {
  try {
    const data = await api.get('/auth/admin-contact')
    if (!data.success) {
      ElMessage.warning(data.message || '管理员未配置联系方式')
      return
    }

    const type = data.type
    // 自定义配置使用 data.value，管理员邮箱回退使用 data.email
    const value = data.value || data.email

    if (!value) {
      ElMessage.warning('管理员未配置联系方式')
      return
    }

    switch (type) {
      case 'link':
        // 链接：新窗口打开
        window.open(value.startsWith('http') ? value : `https://${value}`, '_blank')
        break
      case 'email':
        // 邮箱：弹窗确认后打开邮件客户端
        ElMessageBox.alert(
          `请联系系统管理员申请账号：\n\n邮箱：${value}\n\n点击"确定"将打开邮件客户端。`,
          '联系管理员',
          {
            confirmButtonText: '确定',
            cancelButtonText: '关闭',
            showCancelButton: true,
            type: 'info'
          }
        ).then(() => {
          window.location.href = `mailto:${value}?subject=${encodeURIComponent('ChengXun Game Maker - 账号申请')}&body=${encodeURIComponent('您好，我需要申请一个系统账号。')}`
        }).catch(() => {})
        break
      case 'image':
        // 图片：弹窗展示
        contactImageUrl.value = value
        showContactImage.value = true
        break
      default:
        ElMessage.warning('未知的联系方式类型')
    }
  } catch (error) {
    ElMessage.warning('无法获取管理员联系方式，请稍后重试')
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在3到20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 50, message: '密码长度在6到50个字符', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true

      try {
        const result = await userStore.login({
          username: loginForm.username,
          password: loginForm.password
        })

        // 检查是否需要设备验证
        if (result.needDeviceVerify) {
          // 保存验证专用 token（不是登录 token）
          verifyToken.value = result.verifyToken
          deviceName.value = result.deviceName || '未知设备'
          showDeviceVerify.value = true
          ElMessage.warning('检测到陌生设备，请进行验证')
        } else {
          ElMessage.success('登录成功')
          router.push('/')
        }
      } catch (error) {
        // 拦截器已处理 401/403 错误提示，此处仅补充其他错误
        const status = error.response?.status
        if (status !== 401 && status !== 403) {
          ElMessage.error(error.response?.data?.message || '登录失败')
        }
      } finally {
        loading.value = false
      }
    }
  })
}

// 设备验证相关状态
const showDeviceVerify = ref(false)
const verifyToken = ref('')  // 验证专用 token（不是登录 token）
const deviceName = ref('')
const verifyCode = ref('')
const trustDevice = ref(true)
const verifyLoading = ref(false)
const codeSent = ref(false)
const countdown = ref(60)

// 发送验证码
const sendVerifyCode = async () => {
  try {
    await api.post('/v1/auth/send-verify-code', { verifyToken: verifyToken.value })
    ElMessage.success('验证码已发送到您的邮箱')
    codeSent.value = true

    // 倒计时
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
        codeSent.value = false
        countdown.value = 60
      }
    }, 1000)
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '发送验证码失败')
  }
}

// 验证设备
const handleVerifyDevice = async () => {
  if (!verifyCode.value) {
    ElMessage.warning('请输入验证码')
    return
  }

  verifyLoading.value = true
  try {
    const result = await api.post('/v1/auth/verify-device', {
      verifyToken: verifyToken.value,
      verifyCode: verifyCode.value,
      trustDevice: trustDevice.value.toString()
    })

    ElMessage.success('验证成功')
    showDeviceVerify.value = false

    // 验证成功后，后端返回真正的登录 token
    localStorage.setItem('token', result.token)
    router.push('/')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '验证失败')
  } finally {
    verifyLoading.value = false
  }
}

// 取消设备验证（清除验证 token，回到登录页）
const cancelDeviceVerify = () => {
  showDeviceVerify.value = false
  verifyToken.value = ''
  verifyCode.value = ''
  ElMessage.info('已取消验证，请重新登录')
}

onMounted(() => {
  loadSiteInfo()
})
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-card {
  width: 100%;
  max-width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h1 {
  font-size: 24px;
  color: #333;
  margin-bottom: 8px;
}

.login-header p {
  font-size: 14px;
  color: #666;
}

.login-form {
  margin-top: 20px;
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.login-btn {
  width: 100%;
}

.login-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #666;
}

.register-link {
  color: #409eff;
  text-decoration: none;
  cursor: pointer;
}

.register-link:hover {
  text-decoration: underline;
}

.divider {
  margin: 0 8px;
  color: #ccc;
}

.mb-4 {
  margin-bottom: 16px;
}

.verify-code-row {
  display: flex;
  width: 100%;
}

.verify-code-row .el-input {
  flex: 1;
}

/* 手机端 */
@media (max-width: 767px) {
  .login-card {
    padding: 24px;
  }

  .login-header h1 {
    font-size: 20px;
  }

  .login-header p {
    font-size: 12px;
  }
}

/* 版权和备案信息 */
.copyright-footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  text-align: center;
  padding: 16px 20px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  z-index: 1;
}

.copyright-divider {
  margin: 0 8px;
  color: rgba(255, 255, 255, 0.4);
}

.icp-link {
  color: rgba(255, 255, 255, 0.7);
  text-decoration: none;
  transition: color 0.3s;
}

.icp-link:hover {
  color: #fff;
  text-decoration: underline;
}

/* 小手机端 */
@media (max-width: 374px) {
  .login-card {
    padding: 20px;
  }

  .login-header {
    margin-bottom: 20px;
  }

  .login-header h1 {
    font-size: 18px;
  }

  .copyright-footer {
    font-size: 11px;
    padding: 12px 16px;
  }
}
</style>
