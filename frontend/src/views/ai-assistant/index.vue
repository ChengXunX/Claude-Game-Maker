<template>
  <div class="ai-assistant-page">
    <!-- 左侧会话列表 -->
    <div class="session-sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <el-button type="primary" class="new-chat-btn" @click="createNewSession" v-show="!sidebarCollapsed">
          <el-icon><Plus /></el-icon>
          <span>新对话</span>
        </el-button>
        <el-button type="primary" circle @click="createNewSession" v-show="sidebarCollapsed" class="new-chat-btn-circle">
          <el-icon><Plus /></el-icon>
        </el-button>
        <el-button text @click="sidebarCollapsed = !sidebarCollapsed" class="collapse-btn">
          <el-icon><Fold v-if="!sidebarCollapsed" /><Expand v-else /></el-icon>
        </el-button>
      </div>

      <div class="session-list" v-show="!sidebarCollapsed">
        <div
          v-for="session in sessions"
          :key="session.id"
          class="session-item"
          :class="{ active: currentSessionId === session.id }"
          @click="switchSession(session.id)"
        >
          <div class="session-title">
            <el-icon><ChatDotRound /></el-icon>
            <span class="title-text">{{ session.title }}</span>
          </div>
          <div class="session-actions">
            <el-dropdown trigger="click" @command="(cmd) => handleSessionCommand(cmd, session)">
              <el-icon class="more-btn"><MoreFilled /></el-icon>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="rename">
                    <el-icon><Edit /></el-icon> 重命名
                  </el-dropdown-item>
                  <el-dropdown-item command="delete" divided>
                    <el-icon><Delete /></el-icon> 删除
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
        <el-empty v-if="sessions.length === 0" description="暂无会话" :image-size="60" />
      </div>
    </div>

    <!-- 右侧聊天区域 -->
    <div class="chat-area">
      <el-card class="chat-card" shadow="never">
        <template #header>
          <div class="card-header">
            <div class="header-left">
              <el-icon :size="24" color="#409EFF"><ChatDotRound /></el-icon>
              <h2>{{ currentSession?.title || 'AI 助手' }}</h2>
            </div>
            <div class="header-right">
              <el-tag type="success" effect="dark" size="small">在线</el-tag>
            </div>
          </div>
        </template>

        <StreamingChat
          ref="chatRef"
          :api-endpoint="chatApiEndpoint"
          :session-id="currentSessionId"
          :initial-messages="currentMessages"
          @send="handleSendMessage"
          @clear="handleClearChat"
          @save-message="saveAssistantMessage"
          @tool-result="handleToolResult"
        />
      </el-card>
    </div>

    <!-- 重命名对话框 -->
    <el-dialog v-model="renameDialogVisible" title="重命名会话" width="400px">
      <el-input v-model="newTitle" placeholder="请输入新标题" @keyup.enter="confirmRename" />
      <template #footer>
        <el-button @click="renameDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmRename">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Fold, Expand, ChatDotRound, MoreFilled, Edit, Delete } from '@element-plus/icons-vue'
import StreamingChat from '@/components/streaming/StreamingChat.vue'
import api from '@/api'

const chatRef = ref(null)
const sidebarCollapsed = ref(false)
const sessions = ref([])
const currentSessionId = ref(null)
const currentMessages = ref([])
const renameDialogVisible = ref(false)
const newTitle = ref('')
const renameSessionId = ref(null)

// 当前会话
const currentSession = computed(() => {
  return sessions.value.find(s => s.id === currentSessionId.value)
})

// API 端点
const chatApiEndpoint = computed(() => {
  return `/api/v1/ai-assistant/stream/ask`
})

// 加载会话列表
const loadSessions = async () => {
  try {
    const data = await api.get('/chat/sessions')
    sessions.value = data || []
  } catch (error) {
    console.error('加载会话列表失败:', error)
  }
}

// 创建新会话
const createNewSession = async (silent = false) => {
  try {
    const data = await api.post('/chat/sessions', { title: '新对话' })
    sessions.value.unshift(data)
    // 静默模式下不重置消息，避免清空StreamingChat中用户刚发送的消息
    if (silent) {
      currentSessionId.value = data.id
    } else {
      currentSessionId.value = data.id
      currentMessages.value = []
      // 强制清空聊天组件内部消息
      if (chatRef.value) {
        chatRef.value.clearChat()
      }
      ElMessage.success('新会话已创建')
    }
    return data
  } catch (error) {
    ElMessage.error('创建会话失败')
    return null
  }
}

// 切换会话
const switchSession = async (sessionId) => {
  if (currentSessionId.value === sessionId) return
  currentSessionId.value = sessionId
  // 先清空聊天组件，避免显示旧会话内容
  if (chatRef.value) {
    chatRef.value.clearChat()
  }
  await loadSessionMessages(sessionId)
}

// 加载会话消息
const loadSessionMessages = async (sessionId) => {
  try {
    const data = await api.get(`/chat/sessions/${sessionId}`)
    currentMessages.value = data.messages || []
  } catch (error) {
    console.error('加载会话消息失败:', error)
    currentMessages.value = []
  }
}

// 处理会话命令
const handleSessionCommand = (command, session) => {
  if (command === 'rename') {
    renameSessionId.value = session.id
    newTitle.value = session.title
    renameDialogVisible.value = true
  } else if (command === 'delete') {
    deleteSession(session)
  }
}

// 确认重命名
const confirmRename = async () => {
  if (!newTitle.value.trim()) {
    ElMessage.warning('请输入标题')
    return
  }

  try {
    await api.put(`/chat/sessions/${renameSessionId.value}`, { title: newTitle.value })
    const session = sessions.value.find(s => s.id === renameSessionId.value)
    if (session) {
      session.title = newTitle.value
    }
    renameDialogVisible.value = false
    ElMessage.success('重命名成功')
  } catch (error) {
    ElMessage.error('重命名失败')
  }
}

// 删除会话
const deleteSession = async (session) => {
  try {
    await ElMessageBox.confirm(`确定要删除会话"${session.title}"吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await api.delete(`/chat/sessions/${session.id}`)
    sessions.value = sessions.value.filter(s => s.id !== session.id)

    if (currentSessionId.value === session.id) {
      if (sessions.value.length > 0) {
        switchSession(sessions.value[0].id)
      } else {
        currentSessionId.value = null
        currentMessages.value = []
      }
    }

    ElMessage.success('会话已删除')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 发送消息
const handleSendMessage = async (message) => {
  // 如果没有当前会话，先创建一个（静默模式，不弹窗）
  if (!currentSessionId.value) {
    const session = await createNewSession(true)
    if (!session) return
  }

  // 保存用户消息
  try {
    await api.post(`/chat/sessions/${currentSessionId.value}/messages`, {
      role: 'user',
      content: message
    })
  } catch (error) {
    console.error('保存消息失败:', error)
  }

  // 更新会话列表顺序
  const session = sessions.value.find(s => s.id === currentSessionId.value)
  if (session) {
    session.updatedAt = new Date().toISOString()
    sessions.value.sort((a, b) => new Date(b.updatedAt) - new Date(a.updatedAt))
  }
}

// 清空对话
const handleClearChat = () => {
  currentMessages.value = []
}

// 保存助手回复
const saveAssistantMessage = async (content, thinking, toolUses, tasks) => {
  if (!currentSessionId.value) return

  try {
    await api.post(`/chat/sessions/${currentSessionId.value}/messages`, {
      role: 'assistant',
      content,
      thinking,
      toolUses: toolUses ? JSON.stringify(toolUses) : null,
      tasks: tasks ? JSON.stringify(tasks) : null
    })
  } catch (error) {
    console.error('保存助手消息失败:', error)
  }
}

// 处理工具执行结果
const handleToolResult = async (resultText) => {
  // 将工具结果作为新的用户消息发送给AI继续对话
  if (chatRef.value) {
    chatRef.value.addToolResult(resultText)
  }
}

// 暴露方法给子组件
defineExpose({
  saveAssistantMessage
})

onMounted(() => {
  loadSessions()
})
</script>

<style scoped>
.ai-assistant-page {
  display: flex;
  height: calc(100vh - 120px);
  gap: 0;
  background: var(--el-bg-color);
  border-radius: 8px;
  overflow: hidden;
}

/* 侧边栏 */
.session-sidebar {
  width: 280px;
  background: var(--el-fill-color-light);
  border-right: 1px solid var(--el-border-color-light);
  display: flex;
  flex-direction: column;
  transition: width 0.3s;
}

.session-sidebar.collapsed {
  width: 60px;
}

.sidebar-header {
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  border-bottom: 1px solid var(--el-border-color-light);
}

.session-sidebar.collapsed .sidebar-header {
  padding: 16px 8px;
  flex-direction: column;
}

.new-chat-btn {
  flex: 1;
}

.new-chat-btn-circle {
  width: 36px;
  height: 36px;
  padding: 0;
  min-height: 36px;
}

.collapse-btn {
  padding: 8px;
  flex-shrink: 0;
}

.session-sidebar.collapsed .collapse-btn {
  margin-top: 8px;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 4px;
}

.session-item:hover {
  background: var(--el-fill-color);
}

.session-item.active {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.session-title {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.title-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}

.session-actions {
  opacity: 0;
  transition: opacity 0.2s;
}

.session-item:hover .session-actions {
  opacity: 1;
}

.more-btn {
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
}

.more-btn:hover {
  background: var(--el-fill-color-dark);
}

/* 聊天区域 */
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  border: none;
}

.chat-card :deep(.el-card__body) {
  flex: 1;
  padding: 0;
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-left h2 {
  margin: 0;
  font-size: 16px;
  color: var(--el-text-color-primary);
}

/* 响应式 */
@media (max-width: 768px) {
  .session-sidebar {
    width: 0;
    position: absolute;
    z-index: 100;
    height: 100%;
  }

  .session-sidebar:not(.collapsed) {
    width: 280px;
    box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
  }
}
</style>
