<template>
  <el-drawer
    v-model="visible"
    :title="`项目讨论 - ${projectName}`"
    size="450px"
    :before-close="handleClose"
    class="project-chat-drawer"
  >
    <!-- 主列表视图：Tabs -->
    <div v-if="!currentDiscussion" class="drawer-body">
      <el-tabs v-model="activeTab" class="drawer-tabs">
        <!-- Tab 1: 讨论列表 -->
        <el-tab-pane label="讨论" name="discussions">
          <el-button type="primary" @click="createDiscussion" style="width: 100%; margin-bottom: 12px">
            <el-icon><Plus /></el-icon> 发起新讨论
          </el-button>
          <div v-if="discussions.length === 0" class="empty-tip">
            暂无讨论记录，点击上方按钮发起新讨论
          </div>
          <div
            v-for="d in discussions"
            :key="d.id"
            class="discussion-item"
          >
            <div class="discussion-title" @click="openDiscussion(d)">
              {{ d.title }}
              <el-icon v-if="d.status === 'MINUTES_GENERATED'" class="minutes-icon" color="#67c23a">
                <Document />
              </el-icon>
            </div>
            <div class="discussion-meta">
              <span>{{ d.username }} · {{ formatTime(d.createdAt) }}</span>
              <div class="discussion-actions">
                <el-tag :type="getStatusType(d.status)" size="small">{{ getStatusText(d.status) }}</el-tag>
                <el-button
                  v-if="d.status === 'ACTIVE'"
                  type="warning"
                  text
                  size="small"
                  @click.stop="closeDiscussion(d)"
                >结束</el-button>
                <el-button
                  type="danger"
                  text
                  size="small"
                  @click.stop="deleteDiscussion(d)"
                >删除</el-button>
              </div>
            </div>
            <div v-if="d.meetingMinutes" class="discussion-minutes-preview" @click="openDiscussion(d)">
              {{ truncateText(d.meetingMinutes, 60) }}
            </div>
          </div>
        </el-tab-pane>

        <!-- Tab 2: 会议纪要历史 -->
        <el-tab-pane label="会议纪要" name="minutes">
          <div v-if="minutesList.length === 0" class="empty-tip">
            暂无会议纪要，发起讨论后可生成纪要
          </div>
          <div
            v-for="m in minutesList"
            :key="m.id"
            class="minutes-card"
          >
            <div class="minutes-card-header" @click="toggleMinutes(m.id)">
              <div class="minutes-card-title">
                <el-icon><Document /></el-icon>
                {{ m.title || '会议纪要' }}
              </div>
              <div class="minutes-card-meta">
                <span>{{ formatTime(m.createdAt) }}</span>
                <el-tag v-if="m.syncedToProducer" type="success" size="small">已同步</el-tag>
                <el-tag v-else type="warning" size="small">待同步</el-tag>
                <el-icon :class="{ 'rotate-180': expandedMinutes[m.id] }"><ArrowDown /></el-icon>
              </div>
            </div>
            <el-collapse-transition>
              <div v-if="expandedMinutes[m.id]" class="minutes-card-content">
                <MarkdownRenderer :content="m.minutes" />
              </div>
            </el-collapse-transition>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 对话界面 -->
    <div v-else class="chat-area">
      <!-- 顶部栏 -->
      <div class="chat-header">
        <el-button text size="small" @click="backToList">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
        <span class="chat-title">{{ currentDiscussion.title }}</span>
        <el-tag :type="getStatusType(currentDiscussion.status)" size="small">
          {{ getStatusText(currentDiscussion.status) }}
        </el-tag>
      </div>

      <!-- 消息列表 -->
      <div class="messages-container" ref="messagesRef">
        <div
          v-for="msg in messages"
          :key="msg.id"
          :class="['message-item', msg.role]"
        >
          <div class="message-role">
            {{ msg.role === 'user' ? (msg.sender || '用户') : msg.role === 'system' ? '系统' : 'AI' }}
          </div>
          <div class="message-content">
            <MarkdownRenderer :content="msg.content" />
          </div>
        </div>

        <!-- AI 正在回复 -->
        <div v-if="streaming" class="message-item assistant">
          <div class="message-role">AI</div>
          <div class="message-content">
            <!-- 思考过程（可折叠） -->
            <div v-if="streamThinking" class="thinking-block">
              <div class="thinking-header" @click="showThinking = !showThinking">
                <el-icon class="thinking-arrow" :class="{ expanded: showThinking }"><ArrowRight /></el-icon>
                <span>思考过程</span>
              </div>
              <el-collapse-transition>
                <div v-if="showThinking" class="thinking-body">{{ streamThinking }}</div>
              </el-collapse-transition>
            </div>
            <MarkdownRenderer :content="streamContent" />
            <span class="typing-cursor">|</span>
          </div>
        </div>
      </div>

      <!-- 输入区 -->
      <div class="input-area" v-if="currentDiscussion.status === 'ACTIVE'">
        <el-input
          v-model="inputMessage"
          type="textarea"
          :rows="2"
          placeholder="输入你的想法..."
          @keydown.enter.ctrl="sendMessage"
          :disabled="streaming"
        />
        <div class="input-actions">
          <span class="input-tip">Ctrl+Enter 发送</span>
          <el-button type="primary" size="small" @click="sendMessage" :loading="streaming">
            发送
          </el-button>
        </div>
      </div>

      <!-- 会议纪要区（进行中或已结束的讨论都可以生成纪要） -->
      <div class="minutes-area" v-if="(currentDiscussion.status === 'ACTIVE' || currentDiscussion.status === 'CLOSED') && messages.length > 0">
        <el-button
          type="warning"
          @click="generateMinutes"
          :loading="generatingMinutes"
          style="width: 100%"
        >
          <el-icon><Document /></el-icon> 生成会议纪要（同步给制作人）
        </el-button>
      </div>

      <!-- 已生成的会议纪要 -->
      <div v-if="currentDiscussion.status === 'MINUTES_GENERATED' && currentDiscussion.meetingMinutes" class="minutes-display">
        <el-divider />
        <div class="minutes-title">
          会议纪要
          <el-tag type="success" size="small" style="margin-left: 8px">已同步给制作人</el-tag>
        </div>
        <div class="minutes-content">
          <MarkdownRenderer :content="currentDiscussion.meetingMinutes" />
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, reactive, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, ArrowLeft, ArrowDown, ArrowRight, Document } from '@element-plus/icons-vue'
import api from '@/api'
import { useUserStore } from '@/stores/user'
import MarkdownRenderer from './MarkdownRenderer.vue'

const userStore = useUserStore()

const props = defineProps({
  modelValue: Boolean,
  projectId: String,
  projectName: String
})

const emit = defineEmits(['update:modelValue'])

const visible = ref(false)
const activeTab = ref('discussions')
const discussions = ref([])
const minutesList = ref([])
const currentDiscussion = ref(null)
const messages = ref([])
const inputMessage = ref('')
const streaming = ref(false)
const streamContent = ref('')
const streamThinking = ref('')
const showThinking = ref(false)
const generatingMinutes = ref(false)
const messagesRef = ref(null)
const expandedMinutes = reactive({})

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val && props.projectId) {
    loadDiscussions()
    loadMinutes()
  }
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

const loadDiscussions = async () => {
  try {
    const res = await api.get(`/project-discussions/project/${props.projectId}`)
    if (res.success) {
      discussions.value = res.discussions || []
    }
  } catch (e) {
    console.error('加载讨论列表失败', e)
  }
}

const loadMinutes = async () => {
  try {
    const res = await api.get(`/project-discussions/project/${props.projectId}/minutes`)
    if (res.success) {
      minutesList.value = res.minutes || []
    }
  } catch (e) {
    console.error('加载会议纪要失败', e)
  }
}

const createDiscussion = async () => {
  try {
    const res = await api.post('/project-discussions', {
      projectId: props.projectId,
      title: `项目讨论 - ${new Date().toLocaleString('zh-CN')}`
    })
    if (res.success) {
      discussions.value.unshift(res.discussion)
      openDiscussion(res.discussion)
    }
  } catch (e) {
    ElMessage.error('创建讨论失败')
  }
}

const openDiscussion = async (discussion) => {
  currentDiscussion.value = discussion
  try {
    const res = await api.get(`/project-discussions/${discussion.id}`)
    if (res.success) {
      messages.value = res.messages || []
      currentDiscussion.value = res.discussion
      scrollToBottom()
    }
  } catch (e) {
    ElMessage.error('加载对话失败')
  }
}

const backToList = () => {
  if (activeEventSource) {
    activeEventSource.close()
    activeEventSource = null
  }
  currentDiscussion.value = null
  messages.value = []
  streamContent.value = ''
  streamThinking.value = ''
  streaming.value = false
  loadDiscussions()
  loadMinutes()
}

let activeEventSource = null

const sendMessage = async () => {
  if (!inputMessage.value.trim() || streaming.value) return

  const userMsg = inputMessage.value.trim()
  inputMessage.value = ''

  messages.value.push({
    id: Date.now(),
    role: 'user',
    content: userMsg,
    sender: userStore.username || '用户'
  })
  scrollToBottom()

  streaming.value = true
  streamContent.value = ''
  streamThinking.value = ''
  showThinking.value = false

  try {
    // 使用 EventSource（和AI助手一致），token通过URL参数传递
    const token = localStorage.getItem('token') || ''
    const url = `/api/project-discussions/${currentDiscussion.value.id}/stream/ask?message=${encodeURIComponent(userMsg)}&token=${encodeURIComponent(token)}`
    const eventSource = new EventSource(url)
    activeEventSource = eventSource

    eventSource.addEventListener('thinking', (e) => {
      try {
        const data = JSON.parse(e.data)
        streamThinking.value += data.content || ''
        scrollToBottom()
      } catch {}
    })

    eventSource.addEventListener('text', (e) => {
      try {
        const data = JSON.parse(e.data)
        streamContent.value += data.content || ''
        scrollToBottom()
      } catch {}
    })

    eventSource.addEventListener('done', () => {
      if (streamContent.value) {
        messages.value.push({
          id: Date.now(),
          role: 'assistant',
          content: streamContent.value
        })
      }
      eventSource.close()
      activeEventSource = null
      streaming.value = false
      streamContent.value = ''
      streamThinking.value = ''
      scrollToBottom()
    })

    eventSource.addEventListener('error', (e) => {
      try {
        const data = JSON.parse(e.data)
        ElMessage.error('AI回复失败: ' + (data.message || data))
      } catch {}
      eventSource.close()
      activeEventSource = null
      streaming.value = false
      streamContent.value = ''
      streamThinking.value = ''
      scrollToBottom()
    })

    eventSource.onerror = () => {
      if (streaming.value) {
        if (streamContent.value) {
          messages.value.push({
            id: Date.now(),
            role: 'assistant',
            content: streamContent.value
          })
        }
        eventSource.close()
        activeEventSource = null
        streaming.value = false
        streamContent.value = ''
        streamThinking.value = ''
        scrollToBottom()
      }
    }
  } catch (e) {
    ElMessage.error('AI回复失败: ' + e.message)
    streaming.value = false
    streamContent.value = ''
    streamThinking.value = ''
    scrollToBottom()
  }
}

const generateMinutes = async () => {
  generatingMinutes.value = true
  try {
    const res = await api.post(`/project-discussions/${currentDiscussion.value.id}/generate-minutes`)
    if (res.success) {
      ElMessage.success('会议纪要已生成并同步给制作人')
      const detail = await api.get(`/project-discussions/${currentDiscussion.value.id}`)
      if (detail.success) {
        currentDiscussion.value = detail.discussion
      }
      messages.value.push({
        id: Date.now(),
        role: 'system',
        content: '会议纪要已生成，将自动同步给项目制作人。'
      })
      loadMinutes()
    } else {
      ElMessage.error(res.error || '生成失败')
    }
  } catch (e) {
    ElMessage.error('生成会议纪要失败')
  } finally {
    generatingMinutes.value = false
  }
}

const toggleMinutes = (id) => {
  expandedMinutes[id] = !expandedMinutes[id]
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

const handleClose = () => {
  if (activeEventSource) {
    activeEventSource.close()
    activeEventSource = null
  }
  visible.value = false
}

const closeDiscussion = async (discussion) => {
  try {
    const res = await api.post(`/project-discussions/${discussion.id}/close`)
    if (res.success) {
      discussion.status = 'CLOSED'
      ElMessage.success('讨论已结束')
    } else {
      ElMessage.error(res.error || '操作失败')
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const deleteDiscussion = async (discussion) => {
  try {
    const res = await api.delete(`/project-discussions/${discussion.id}`)
    if (res.success) {
      discussions.value = discussions.value.filter(d => d.id !== discussion.id)
      ElMessage.success('讨论已删除')
    } else {
      ElMessage.error(res.error || '删除失败')
    }
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

const getStatusType = (status) => {
  const map = { ACTIVE: 'primary', MINUTES_GENERATED: 'success', CLOSED: 'info', ARCHIVED: 'info' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { ACTIVE: '进行中', MINUTES_GENERATED: '已生成纪要', CLOSED: '已结束', ARCHIVED: '已归档' }
  return map[status] || status
}

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  return `${d.getMonth() + 1}/${d.getDate()} ${d.getHours()}:${String(d.getMinutes()).padStart(2, '0')}`
}

const truncateText = (text, len) => {
  if (!text) return ''
  return text.length > len ? text.slice(0, len) + '...' : text
}
</script>

<style scoped>
.project-chat-drawer :deep(.el-drawer__body) {
  padding: 0;
  display: flex;
  flex-direction: column;
}

.drawer-body {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.drawer-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.drawer-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 16px;
}

.drawer-tabs :deep(.el-tabs__content) {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.drawer-tabs :deep(.el-tab-pane) {
  min-height: 100%;
}

.empty-tip {
  text-align: center;
  color: #999;
  padding: 40px 0;
  font-size: 14px;
}

/* 讨论列表 */
.discussion-item {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: border-color 0.2s;
}

.discussion-item:hover {
  border-color: #409eff;
}

.discussion-title {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.minutes-icon {
  flex-shrink: 0;
}

.discussion-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #999;
}

.discussion-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.discussion-minutes-preview {
  margin-top: 6px;
  font-size: 12px;
  color: #67c23a;
  background: #f0f9eb;
  padding: 4px 8px;
  border-radius: 4px;
  line-height: 1.4;
}

/* 会议纪要卡片 */
.minutes-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 8px;
  overflow: hidden;
}

.minutes-card-header {
  padding: 12px;
  cursor: pointer;
  transition: background 0.2s;
}

.minutes-card-header:hover {
  background: #f5f7fa;
}

.minutes-card-title {
  font-size: 14px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.minutes-card-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #999;
}

.minutes-card-meta .el-icon {
  transition: transform 0.3s;
  margin-left: auto;
}

.rotate-180 {
  transform: rotate(180deg);
}

.minutes-card-content {
  padding: 0 12px 12px;
  font-size: 13px;
  line-height: 1.8;
  color: #333;
  border-top: 1px solid #ebeef5;
  padding-top: 12px;
  background: #fafafa;
}

/* 对话区 */
.chat-area {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-bottom: 1px solid #ebeef5;
  font-size: 13px;
}

.chat-title {
  flex: 1;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.message-item {
  margin-bottom: 16px;
}

.message-role {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.message-item.user .message-role { color: #409eff; }
.message-item.system .message-role { color: #e6a23c; }

.message-content {
  background: #f4f4f5;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.message-item.user .message-content { background: #ecf5ff; }

.message-item.system .message-content {
  background: #fdf6ec;
  font-size: 13px;
  color: #e6a23c;
}

.thinking-block {
  margin-bottom: 8px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  overflow: hidden;
}

.thinking-header {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px;
  background: #f5f7fa;
  cursor: pointer;
  font-size: 12px;
  color: #909399;
  user-select: none;
}

.thinking-header:hover {
  background: #ebeef5;
}

.thinking-arrow {
  transition: transform 0.2s;
  font-size: 12px;
}

.thinking-arrow.expanded {
  transform: rotate(90deg);
}

.thinking-body {
  padding: 8px 10px;
  font-size: 13px;
  color: #67c23a;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 200px;
  overflow-y: auto;
  background: #f0f9eb;
}

.typing-cursor {
  animation: blink 1s infinite;
  color: #409eff;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

.input-area {
  padding: 12px 16px;
  border-top: 1px solid #ebeef5;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.input-tip {
  font-size: 12px;
  color: #999;
}

.minutes-area {
  padding: 12px 16px;
  border-top: 1px solid #ebeef5;
}

.minutes-display {
  padding: 0 16px 16px;
}

.minutes-title {
  font-weight: 600;
  font-size: 15px;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
}

.minutes-content {
  font-size: 14px;
  line-height: 1.8;
  background: #f0f9eb;
  padding: 12px;
  border-radius: 8px;
  word-break: break-word;
}
</style>
