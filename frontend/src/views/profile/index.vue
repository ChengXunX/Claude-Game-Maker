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

    <!-- 飞书 AI 助手绑定 -->
    <el-card class="mt-4">
      <template #header>
        <span>飞书 AI 助手</span>
      </template>
      <div class="feishu-bind-section">
        <p class="bind-desc">绑定飞书账号后，可在飞书中直接与 AI 助手对话</p>
        <!-- 已绑定状态 -->
        <div v-if="feishuBound" class="bind-status-bound">
          <el-alert type="success" :closable="false" show-icon>
            <template #title>
              <span>已绑定飞书账号</span>
            </template>
            <template #default>
              <span>绑定时间：{{ feishuBindTime || '未知' }}</span>
            </template>
          </el-alert>
          <el-button type="danger" size="small" @click="handleUnbindFeishu" style="margin-top: 8px;">
            解绑
          </el-button>
        </div>
        <!-- 未绑定状态 -->
        <div v-else>
          <div v-if="feishuBindCode" class="bind-code-display">
            <el-alert type="success" :closable="false" show-icon>
              <template #title>
                <span>绑定验证码：<strong class="code-text">{{ feishuBindCode }}</strong></span>
              </template>
              <template #default>
                <span>在飞书中发送「绑定 {{ feishuBindCode }}」即可完成绑定（30分钟内有效）</span>
              </template>
            </el-alert>
          </div>
          <el-button type="primary" @click="handleGenerateBindCode" :loading="generatingCode">
            <el-icon><Link /></el-icon> 生成绑定验证码
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 飞书对话记录 -->
    <el-card class="mt-4" v-if="feishuBound">
      <template #header>
        <div class="card-header">
          <span>飞书对话记录</span>
          <el-button size="small" @click="loadFeishuSessions" :loading="loadingFeishuSessions">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </template>
      <div v-if="feishuSessions.length === 0" class="empty-feishu">
        <el-empty description="暂无飞书对话记录" :image-size="60" />
      </div>
      <div v-else class="feishu-sessions">
        <div v-for="session in feishuSessions" :key="session.id" class="feishu-session-item">
          <div class="session-header" @click="toggleFeishuSession(session.id)">
            <div class="session-info">
              <el-icon><ChatDotRound /></el-icon>
              <span class="session-title">{{ session.title }}</span>
              <el-tag size="small" type="info">{{ session.messageCount || 0 }} 条消息</el-tag>
            </div>
            <div class="session-actions">
              <el-button size="small" type="warning" text @click.stop="handleClearFeishuSession(session.id)">
                清空上下文
              </el-button>
              <el-icon :class="{ 'rotate-180': expandedSessions.includes(session.id) }">
                <ArrowDown />
              </el-icon>
            </div>
          </div>
          <div v-if="expandedSessions.includes(session.id)" class="session-messages">
            <div v-if="loadingMessages[session.id]" class="loading-messages">
              <el-skeleton :rows="3" animated />
            </div>
            <div v-else class="message-list">
              <div v-for="msg in (feishuMessages[session.id] || [])" :key="msg.id"
                   :class="['message-item', msg.role]">
                <div class="message-role">{{ msg.role === 'user' ? '👤 我' : '🤖 AI' }}</div>
                <div v-if="msg.role === 'assistant'" class="message-content markdown-body" v-html="renderMarkdown(msg.content)"></div>
                <div v-else class="message-content">{{ msg.content }}</div>
                <div class="message-time">{{ formatTime(msg.createdAt) }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
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
import { authApi, feishuApi, chatSessionApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, ChatDotRound, ArrowDown } from '@element-plus/icons-vue'
import EmailChangeDialog from '@/components/EmailChangeDialog.vue'
import { marked } from 'marked'

/** 渲染 Markdown 内容 */
const renderMarkdown = (content) => {
  if (!content) return ''
  try {
    return marked(content, { breaks: true, gfm: true })
  } catch (e) {
    return content
  }
}

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

// 飞书绑定
const feishuBindCode = ref('')
const feishuBound = ref(false)
const feishuBindTime = ref('')
const generatingCode = ref(false)

// 飞书对话记录
const feishuSessions = ref([])
const feishuMessages = ref({})
const expandedSessions = ref([])
const loadingFeishuSessions = ref(false)
const loadingMessages = ref({})

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

/** 生成飞书绑定验证码 */
const handleGenerateBindCode = async () => {
  generatingCode.value = true
  try {
    const res = await feishuApi.generateBindCode()
    if (res.success) {
      feishuBindCode.value = res.code
      ElMessage.success('验证码已生成')
    } else {
      ElMessage.error(res.message || '生成失败')
    }
  } catch (e) {
    ElMessage.error('生成验证码失败')
  } finally {
    generatingCode.value = false
  }
}

/** 加载飞书绑定状态 */
const loadFeishuBindStatus = async () => {
  try {
    const res = await feishuApi.getBindStatus()
    if (res.success && res.bound) {
      feishuBound.value = true
      feishuBindTime.value = res.bindTime || ''
    } else {
      feishuBound.value = false
    }
  } catch (e) {
    // 忽略加载失败
  }
}

/** 解绑飞书 */
const handleUnbindFeishu = async () => {
  try {
    await ElMessageBox.confirm('确定要解绑飞书账号吗？解绑后将无法在飞书中使用 AI 助手。', '确认解绑', {
      confirmButtonText: '解绑',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await feishuApi.unbind()
    if (res.success) {
      feishuBound.value = false
      feishuBindCode.value = ''
      ElMessage.success('已解绑')
    } else {
      ElMessage.error(res.message || '解绑失败')
    }
  } catch (e) {
    // 用户取消
  }
}

/** 加载飞书会话列表 */
const loadFeishuSessions = async () => {
  loadingFeishuSessions.value = true
  try {
    const res = await chatSessionApi.getFeishuSessions()
    feishuSessions.value = res || []
  } catch (e) {
    console.error('加载飞书会话失败', e)
  } finally {
    loadingFeishuSessions.value = false
  }
}

/** 展开/折叠飞书会话 */
const toggleFeishuSession = async (sessionId) => {
  const idx = expandedSessions.value.indexOf(sessionId)
  if (idx >= 0) {
    expandedSessions.value.splice(idx, 1)
    return
  }
  expandedSessions.value.push(sessionId)
  // 加载消息
  if (!feishuMessages.value[sessionId]) {
    loadingMessages.value[sessionId] = true
    try {
      const res = await chatSessionApi.getFeishuSession(sessionId)
      feishuMessages.value[sessionId] = res?.messages || []
    } catch (e) {
      console.error('加载飞书消息失败', e)
      feishuMessages.value[sessionId] = []
    } finally {
      loadingMessages.value[sessionId] = false
    }
  }
}

/** 清空飞书会话上下文 */
const handleClearFeishuSession = async (sessionId) => {
  try {
    await ElMessageBox.confirm('确定要清空该飞书对话的上下文吗？清空后 AI 将忘记之前的对话历史。', '清空上下文', {
      confirmButtonText: '清空',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await chatSessionApi.clearFeishuSession(sessionId)
    if (res.success) {
      feishuMessages.value[sessionId] = []
      ElMessage.success('上下文已清空')
      loadFeishuSessions()
    } else {
      ElMessage.error(res.message || '清空失败')
    }
  } catch (e) {
    // 用户取消
  }
}

/** 格式化时间 */
const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return d.toLocaleString('zh-CN')
}

onMounted(async () => {
  // 先从后端刷新用户数据，确保显示最新信息（如审批通过后的邮箱）
  try {
    await userStore.getUserInfo()
  } catch (e) {
    // 忽略刷新失败，使用缓存数据
  }
  loadUserInfo()
  loadFeishuBindStatus().then(() => {
    if (feishuBound.value) {
      loadFeishuSessions()
    }
  })
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

.feishu-bind-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bind-desc {
  color: #606266;
  font-size: 14px;
  margin: 0;
}

.bind-code-display {
  margin: 8px 0;
}

.code-text {
  font-size: 18px;
  letter-spacing: 3px;
  color: #409eff;
}

/* 飞书对话记录 */
.empty-feishu {
  padding: 20px 0;
}

.feishu-sessions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.feishu-session-item {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.session-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  background: #fafafa;
  transition: background 0.2s;
}

.session-header:hover {
  background: #f0f2f5;
}

.session-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.session-title {
  font-weight: 500;
  color: #303133;
}

.session-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rotate-180 {
  transform: rotate(180deg);
}

.session-messages {
  border-top: 1px solid #e4e7ed;
  padding: 16px;
  max-height: 500px;
  overflow-y: auto;
}

.loading-messages {
  padding: 16px;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px;
  border-radius: 8px;
  max-width: 85%;
}

.message-item.user {
  align-self: flex-end;
  background: #ecf5ff;
  border: 1px solid #b3d8ff;
}

.message-item.assistant {
  align-self: flex-start;
  background: #f4f4f5;
  border: 1px solid #e9e9eb;
}

.message-role {
  font-size: 12px;
  color: #909399;
  font-weight: 500;
}

.message-content {
  font-size: 14px;
  color: #303133;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-time {
  font-size: 11px;
  color: #c0c4cc;
  align-self: flex-end;
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

  /* Markdown 渲染样式 */
  .markdown-body {
    font-size: 14px;
    line-height: 1.6;
    word-wrap: break-word;

    p { margin: 0 0 8px 0; }
    h1, h2, h3, h4, h5, h6 { margin: 12px 0 8px 0; font-weight: 600; }
    ul, ol { padding-left: 20px; margin: 4px 0; }
    li { margin: 2px 0; }
    code { background: #f5f7fa; padding: 2px 6px; border-radius: 3px; font-size: 13px; }
    pre { background: #f5f7fa; padding: 12px; border-radius: 4px; overflow-x: auto; }
    pre code { background: none; padding: 0; }
    blockquote { border-left: 4px solid #dcdfe6; padding-left: 12px; color: #909399; margin: 8px 0; }
    table { border-collapse: collapse; width: 100%; margin: 8px 0; }
    th, td { border: 1px solid #ebeef5; padding: 8px 12px; text-align: left; }
    th { background: #f5f7fa; }
    a { color: #409eff; text-decoration: none; }
    img { max-width: 100%; }
  }
}
</style>
