<template>
  <el-dialog
    v-model="visible"
    title="更换邮箱"
    width="500px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <!-- 当前邮箱 -->
      <el-form-item label="当前邮箱">
        <el-input :model-value="currentEmail" disabled />
      </el-form-item>

      <!-- 当前邮箱验证码 -->
      <el-form-item label="当前验证码" prop="currentCode">
        <div class="code-input-group">
          <el-input
            v-model="form.currentCode"
            placeholder="请输入当前邮箱验证码"
            maxlength="6"
          />
          <el-button
            type="primary"
            :disabled="currentCodeCooldown > 0"
            @click="handleSendCurrentCode"
            :loading="sendingCurrentCode"
          >
            {{ currentCodeCooldown > 0 ? `${currentCodeCooldown}s` : '发送验证码' }}
          </el-button>
        </div>
      </el-form-item>

      <!-- 分隔线 -->
      <el-divider content-position="left">验证新邮箱</el-divider>

      <!-- 新邮箱 -->
      <el-form-item label="新邮箱" prop="newEmail">
        <el-input
          v-model="form.newEmail"
          placeholder="请输入新邮箱地址"
          @blur="checkNewEmail"
        />
      </el-form-item>

      <!-- 新邮箱验证码 -->
      <el-form-item label="新验证码" prop="newCode">
        <div class="code-input-group">
          <el-input
            v-model="form.newCode"
            placeholder="请输入新邮箱验证码"
            maxlength="6"
          />
          <el-button
            type="primary"
            :disabled="newCodeCooldown > 0 || !isNewEmailValid"
            @click="handleSendNewCode"
            :loading="sendingNewCode"
          >
            {{ newCodeCooldown > 0 ? `${newCodeCooldown}s` : '发送验证码' }}
          </el-button>
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button
        type="primary"
        @click="handleSubmit"
        :loading="submitting"
        :disabled="!isFormValid"
      >
        确认换绑
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
/**
 * 邮箱换绑验证码弹窗组件
 *
 * 功能：
 * 1. 向当前邮箱发送验证码，验证当前邮箱所有权
 * 2. 向新邮箱发送验证码，验证新邮箱所有权
 * 3. 双重验证通过后，直接更新邮箱绑定
 */
import { ref, computed, watch } from 'vue'
import { authApi } from '@/api'
import { ElMessage } from 'element-plus'

const props = defineProps({
  /** 是否显示弹窗 */
  modelValue: {
    type: Boolean,
    default: false
  },
  /** 当前绑定的邮箱 */
  currentEmail: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue', 'success'])

const formRef = ref(null)
const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

// 表单数据
const form = ref({
  currentCode: '',
  newEmail: '',
  newCode: ''
})

// 发送状态
const sendingCurrentCode = ref(false)
const sendingNewCode = ref(false)
const submitting = ref(false)

// 倒计时
const currentCodeCooldown = ref(0)
const newCodeCooldown = ref(0)

// 新邮箱是否有效（格式正确且与当前不同）
const isNewEmailValid = ref(false)

// 表单验证规则
const rules = {
  currentCode: [
    { required: true, message: '请输入当前邮箱验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' }
  ],
  newEmail: [
    { required: true, message: '请输入新邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  newCode: [
    { required: true, message: '请输入新邮箱验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' }
  ]
}

// 表单是否有效
const isFormValid = computed(() => {
  return form.value.currentCode.length === 6 &&
    form.value.newEmail &&
    isNewEmailValid.value &&
    form.value.newCode.length === 6
})

// 检查新邮箱是否有效
const checkNewEmail = () => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  isNewEmailValid.value = emailRegex.test(form.value.newEmail) &&
    form.value.newEmail !== props.currentEmail
}

// 启动倒计时
const startCooldown = (type) => {
  const cooldownRef = type === 'current' ? currentCodeCooldown : newCodeCooldown
  cooldownRef.value = 60
  const timer = setInterval(() => {
    cooldownRef.value--
    if (cooldownRef.value <= 0) {
      clearInterval(timer)
    }
  }, 1000)
}

// 发送当前邮箱验证码
const handleSendCurrentCode = async () => {
  sendingCurrentCode.value = true
  try {
    const res = await authApi.sendCurrentEmailCode()
    if (res.success) {
      ElMessage.success(res.message || '验证码已发送')
      startCooldown('current')
    } else {
      ElMessage.error(res.message || '发送失败')
    }
  } catch (e) {
    ElMessage.error('发送验证码失败')
  } finally {
    sendingCurrentCode.value = false
  }
}

// 发送新邮箱验证码
const handleSendNewCode = async () => {
  if (!isNewEmailValid.value) {
    ElMessage.warning('请输入有效的新邮箱地址')
    return
  }

  sendingNewCode.value = true
  try {
    const res = await authApi.sendNewEmailCode({ newEmail: form.value.newEmail })
    if (res.success) {
      ElMessage.success(res.message || '验证码已发送')
      startCooldown('new')
    } else {
      ElMessage.error(res.message || '发送失败')
    }
  } catch (e) {
    ElMessage.error('发送验证码失败')
  } finally {
    sendingNewCode.value = false
  }
}

// 提交换绑
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const res = await authApi.changeEmail({
      newEmail: form.value.newEmail,
      currentEmailCode: form.value.currentCode,
      newEmailCode: form.value.newCode
    })

    if (res.success) {
      ElMessage.success('邮箱更换成功')
      emit('success')
      handleClose()
    } else {
      ElMessage.error(res.message || '邮箱更换失败')
    }
  } catch (e) {
    ElMessage.error('邮箱更换失败')
  } finally {
    submitting.value = false
  }
}

// 关闭弹窗
const handleClose = () => {
  form.value = {
    currentCode: '',
    newEmail: '',
    newCode: ''
  }
  isNewEmailValid.value = false
  currentCodeCooldown.value = 0
  newCodeCooldown.value = 0
  visible.value = false
}

// 监听弹窗打开，重置状态
watch(visible, (val) => {
  if (val) {
    handleClose()
  }
})
</script>

<style scoped>
.code-input-group {
  display: flex;
  gap: 12px;
  width: 100%;
}

.code-input-group .el-input {
  flex: 1;
}

.code-input-group .el-button {
  white-space: nowrap;
  min-width: 120px;
}

:deep(.el-divider__text) {
  font-size: 13px;
  color: #909399;
}
</style>
