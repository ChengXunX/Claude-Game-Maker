<template>
  <div class="streaming-chat">
    <!-- 消息列表 -->
    <div class="messages-container" ref="messagesRef">
      <div v-for="(message, index) in messages" :key="index" class="message-item" :id="'msg-' + index">
        <!-- 用户消息 -->
        <div v-if="message.role === 'user'" class="message message-user">
          <div class="message-content">{{ message.content }}</div>
          <div class="message-meta">
            <span class="message-time">{{ formatTime(message.timestamp) }}</span>
          </div>
        </div>

        <!-- 助手消息 -->
        <div v-else-if="message.role === 'assistant'" class="message message-assistant">
          <!-- 思考过程 -->
          <ThinkingBlock
            v-if="message.thinking"
            :content="message.thinking"
            :duration="message.thinkingDuration"
          />

          <!-- 工具调用列表 -->
          <div v-if="message.toolUses && message.toolUses.length > 0" class="tool-uses">
            <ToolUseBlock
              v-for="(tool, toolIndex) in message.toolUses"
              :key="toolIndex"
              :tool-name="tool.toolName"
              :input="tool.input"
              :result="tool.result"
              :status="tool.status"
            />
          </div>

          <!-- 任务列表 -->
          <div v-if="message.tasks && message.tasks.length > 0" class="tasks">
            <TaskBlock
              v-for="(task, taskIndex) in message.tasks"
              :key="taskIndex"
              :task-id="task.taskId"
              :title="task.title"
              :status="task.status"
            />
          </div>

          <!-- 文本内容 - Markdown渲染 -->
          <div v-if="message.content" class="message-content markdown-body" v-html="renderMarkdown(message.content)"></div>

          <!-- 消息元信息 -->
          <div class="message-meta">
            <span v-if="message.thinkingDuration" class="thinking-time">
              思考 {{ formatDuration(message.thinkingDuration) }}
            </span>
            <span class="message-time">{{ formatTime(message.timestamp) }}</span>
          </div>

          <!-- 加载状态 -->
          <div v-if="message.loading" class="loading-indicator">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>思考中...</span>
          </div>
        </div>
      </div>

      <!-- 流式响应中的实时内容 -->
      <div v-if="streamingMessage" class="message message-assistant streaming">
        <!-- 实时思考过程 -->
        <ThinkingBlock
          v-if="streamingThinking"
          :content="streamingThinking"
          :auto-expand="true"
        />

        <!-- 实时工具调用 -->
        <div v-if="streamingToolUses.length > 0" class="tool-uses">
          <ToolUseBlock
            v-for="(tool, index) in streamingToolUses"
            :key="index"
            :tool-name="tool.toolName"
            :input="tool.input"
            :status="tool.status"
            :default-expanded="true"
          />
        </div>

        <!-- 实时任务 -->
        <div v-if="streamingTasks.length > 0" class="tasks">
          <TaskBlock
            v-for="(task, index) in streamingTasks"
            :key="index"
            :task-id="task.taskId"
            :title="task.title"
            :status="task.status"
          />
        </div>

        <!-- 实时文本 - Markdown渲染 -->
        <div v-if="streamingText" class="message-content markdown-body">
          <div v-html="renderMarkdown(streamingText)"></div>
          <span class="cursor">|</span>
        </div>
      </div>
    </div>

    <!-- 右侧节点链条导航 -->
    <div
      class="message-nav"
      :class="{ collapsed: navCollapsed }"
      v-if="messages.length > 2"
      @mouseenter="navCollapsed = false"
      @mouseleave="navCollapsed = true"
    >
      <div class="nav-title">
        <el-icon><ChatDotRound /></el-icon>
        <span v-show="!navCollapsed">消息导航</span>
      </div>
      <div
        v-for="(msg, index) in messages"
        :key="index"
        v-show="msg.role === 'user'"
        class="nav-item"
        :class="{ active: currentNavIndex === index }"
        @click="scrollToMessage(index)"
        :title="navCollapsed ? msg.content.substring(0, 30) : ''"
      >
        <el-icon><ChatDotRound /></el-icon>
        <span class="nav-text" v-show="!navCollapsed">{{ msg.content.substring(0, 20) }}{{ msg.content.length > 20 ? '...' : '' }}</span>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="input-area">
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="3"
        placeholder="输入问题... (Enter发送, Shift+Enter换行)"
        @keydown="handleKeydown"
        :disabled="isLoading"
      />
      <div class="input-actions">
        <el-button
          type="primary"
          :loading="isLoading"
          @click="sendMessage"
        >
          {{ isLoading ? '处理中...' : '发送' }}
        </el-button>
        <el-button @click="clearChat">清空对话</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, nextTick, onMounted, watch, defineExpose } from 'vue'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import api from '@/api'
import { useUserStore } from '@/stores/user'
import ThinkingBlock from './ThinkingBlock.vue'
import ToolUseBlock from './ToolUseBlock.vue'
import TaskBlock from './TaskBlock.vue'

const userStore = useUserStore()

// 配置marked
marked.setOptions({
  breaks: true,
  gfm: true
})

const props = defineProps({
  apiEndpoint: {
    type: String,
    default: '/api/v1/ai-assistant/stream/ask'
  },
  sessionId: {
    type: [Number, String],
    default: null
  },
  initialMessages: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['send', 'clear', 'saveMessage', 'toolResult'])

const messagesRef = ref(null)
const inputText = ref('')
const isLoading = ref(false)
const currentNavIndex = ref(0)
const navCollapsed = ref(true) // 默认折叠

// 消息列表
const messages = reactive([])

// Markdown渲染
const renderMarkdown = (content) => {
  if (!content) return ''
  try {
    return marked(content)
  } catch (e) {
    return content
  }
}

// 格式化时间戳
const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  return `${hours}:${minutes}`
}

// 格式化持续时间
const formatDuration = (ms) => {
  if (!ms) return ''
  if (ms < 1000) return `${ms}ms`
  return `${(ms / 1000).toFixed(1)}s`
}

// 滚动到指定消息
const scrollToMessage = (index) => {
  currentNavIndex.value = index
  const element = document.getElementById('msg-' + index)
  if (element) {
    element.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

// 流式状态
const streamingMessage = ref(null)
const streamingThinking = ref('')
const streamingText = ref('')
const streamingToolUses = reactive([])
const streamingTasks = reactive([])

// 发送消息
const sendMessage = async () => {
  const question = inputText.value.trim()
  if (!question || isLoading.value) return

  // 通知父组件（用于保存消息到会话）
  emit('send', question)

  // 添加用户消息
  messages.push({
    role: 'user',
    content: question,
    timestamp: Date.now()
  })

  // 添加助手消息占位
  const assistantIndex = messages.length
  messages.push({
    role: 'assistant',
    content: '',
    thinking: '',
    thinkingDuration: null,
    toolUses: [],
    tasks: [],
    loading: true,
    timestamp: Date.now()
  })

  inputText.value = ''
  isLoading.value = true

  // 重置流式状态
  streamingThinking.value = ''
  streamingText.value = ''
  streamingToolUses.length = 0
  streamingTasks.length = 0

  // 滚动到底部
  await scrollToBottom()

  try {
    // 建立SSE连接（通过URL参数传递token，因为EventSource不支持自定义请求头）
    const token = localStorage.getItem('token') || ''
    const sessionParam = props.sessionId ? `&sessionId=${encodeURIComponent(props.sessionId)}` : ''
    const url = `${props.apiEndpoint}?question=${encodeURIComponent(question)}&token=${encodeURIComponent(token)}${sessionParam}`
    const eventSource = new EventSource(url)

    let thinkingStartTime = Date.now()

    // 监听开始事件
    eventSource.addEventListener('start', (e) => {
      console.log('Stream started:', JSON.parse(e.data))
    })

    // 监听思考事件
    eventSource.addEventListener('thinking', (e) => {
      const data = JSON.parse(e.data)
      streamingThinking.value += data.content
      messages[assistantIndex].thinking = streamingThinking.value
      scrollToBottom()
    })

    // 监听文本事件
    eventSource.addEventListener('text', (e) => {
      const data = JSON.parse(e.data)
      streamingText.value += data.content
      messages[assistantIndex].content = streamingText.value
      messages[assistantIndex].loading = false
      scrollToBottom()
    })

    // 监听工具调用事件（后端执行，前端展示）
    // 使用 nextToolIndex 跟踪下一个要完成的工具索引（后端保证 tool_use/tool_result 顺序一致）
    let nextToolIndex = 0

    eventSource.addEventListener('tool_use', (e) => {
      const data = JSON.parse(e.data)
      const tool = {
        toolName: data.toolName,
        input: data.input,
        result: null,
        status: 'running'
      }
      streamingToolUses.push(tool)
      // 直接修改 messages 中的引用，触发 Vue 响应式更新
      messages[assistantIndex].toolUses = [...streamingToolUses]
      scrollToBottom()
    })

    // 监听工具执行中事件
    eventSource.addEventListener('tool_executing', (e) => {
      const data = JSON.parse(e.data)
      // 更新对应工具的状态为"执行中"
      const tool = streamingToolUses.find(t => t.toolName === data.toolName && t.status === 'running')
      if (tool) {
        tool.status = 'executing'
        tool.executingMessage = data.content || '执行中...'
      }
      // 触发响应式更新
      messages[assistantIndex].toolUses = [...streamingToolUses]
      scrollToBottom()
    })

    // 监听工具结果事件（后端执行完成后）
    eventSource.addEventListener('tool_result', (e) => {
      const data = JSON.parse(e.data)
      // 按顺序匹配：后端保证 tool_use 和 tool_result 按相同顺序发送
      if (nextToolIndex < streamingToolUses.length) {
        streamingToolUses[nextToolIndex].result = data.result
        // 判断是否成功
        try {
          const resultObj = JSON.parse(data.result)
          streamingToolUses[nextToolIndex].status = resultObj.success !== false ? 'success' : 'error'
        } catch {
          streamingToolUses[nextToolIndex].status = 'success'
        }
        nextToolIndex++
      } else {
        // 兜底：按名称查找（兼容乱序情况）
        const tool = streamingToolUses.find(t => t.toolName === data.toolName && (t.status === 'running' || t.status === 'executing'))
        if (tool) {
          tool.result = data.result
          try {
            const resultObj = JSON.parse(data.result)
            tool.status = resultObj.success !== false ? 'success' : 'error'
          } catch {
            tool.status = 'success'
          }
        }
      }
      // 触发响应式更新
      messages[assistantIndex].toolUses = [...streamingToolUses]
      scrollToBottom()
    })

    // 监听任务事件
    eventSource.addEventListener('task', (e) => {
      const data = JSON.parse(e.data)
      const task = {
        taskId: data.taskId,
        title: data.title,
        status: data.status
      }
      streamingTasks.push(task)
      messages[assistantIndex].tasks = [...streamingTasks]
      scrollToBottom()
    })

    // 监听完成事件
    eventSource.addEventListener('complete', (e) => {
      const data = JSON.parse(e.data)

      // 更新最终内容
      if (data.content) {
        messages[assistantIndex].content = data.content
      }

      // 计算思考时长
      messages[assistantIndex].thinkingDuration = Date.now() - thinkingStartTime
      messages[assistantIndex].loading = false

      // 将所有仍在"执行中"的工具标记为"成功"（后端已完成执行）
      streamingToolUses.forEach(tool => {
        if (tool.status === 'running' || tool.status === 'executing') {
          tool.status = 'success'
        }
      })
      // 确保 messages 中的引用是最新的
      messages[assistantIndex].toolUses = [...streamingToolUses]

      // 保存助手消息（包含思考过程、工具调用、任务）
      emit('saveMessage',
        data.content,
        messages[assistantIndex].thinking,
        messages[assistantIndex].toolUses,
        messages[assistantIndex].tasks
      )

      // 清理流式状态（使用新数组，不影响已保存到 messages 的数据）
      streamingThinking.value = ''
      streamingText.value = ''
      streamingToolUses.splice(0, streamingToolUses.length)
      streamingTasks.splice(0, streamingTasks.length)

      eventSource.close()
      isLoading.value = false
      scrollToBottom()
    })

    // 监听错误事件
    eventSource.addEventListener('error', (e) => {
      const data = e.data ? JSON.parse(e.data) : { message: '连接错误' }

      messages[assistantIndex].content = `错误: ${data.message}`
      messages[assistantIndex].loading = false

      eventSource.close()
      isLoading.value = false
      ElMessage.error(data.message)
      scrollToBottom()
    })

    // 连接错误处理
    eventSource.onerror = () => {
      if (isLoading.value) {
        messages[assistantIndex].content = '连接中断，请重试'
        messages[assistantIndex].loading = false

        eventSource.close()
        isLoading.value = false
        ElMessage.error('连接中断')
        scrollToBottom()
      }
    }

  } catch (error) {
    console.error('发送消息失败:', error)
    messages[assistantIndex].content = `发送失败: ${error.message}`
    messages[assistantIndex].loading = false

    isLoading.value = false
    ElMessage.error('发送消息失败')
  }
}

// 处理键盘事件
const handleKeydown = (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

// 清空对话
const clearChat = () => {
  messages.length = 0
  streamingThinking.value = ''
  streamingText.value = ''
  streamingToolUses.length = 0
  streamingTasks.length = 0
  emit('clear')
}

// 滚动到底部
const scrollToBottom = async () => {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

// 监听初始消息变化（仅在切换会话时同步消息，不清空当前正在输入的消息）
watch(() => props.initialMessages, (newMessages) => {
  if (newMessages && newMessages.length > 0 && !isLoading.value) {
    messages.length = 0
    newMessages.forEach(msg => {
      // 解析工具调用和任务记录
      let toolUses = []
      let tasks = []
      try {
        if (msg.toolUses) toolUses = typeof msg.toolUses === 'string' ? JSON.parse(msg.toolUses) : msg.toolUses
      } catch (e) { /* ignore */ }
      try {
        if (msg.tasks) tasks = typeof msg.tasks === 'string' ? JSON.parse(msg.tasks) : msg.tasks
      } catch (e) { /* ignore */ }

      messages.push({
        role: msg.role,
        content: msg.content,
        thinking: msg.thinking,
        toolUses,
        tasks,
        loading: false
      })
    })
    scrollToBottom()
  } else {
    messages.length = 0
  }
}, { immediate: true })

/**
 * 添加工具执行结果
 * 将工具结果作为新消息发送给AI继续对话
 */
const addToolResult = (resultText) => {
  // 将工具结果作为用户消息发送
  sendMessage(resultText)
}

// 暴露方法给父组件
defineExpose({
  addToolResult,
  clearChat
})

onMounted(() => {
  // 可以在这里加载历史对话
})
</script>

<style scoped>
.streaming-chat {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--el-bg-color);
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.message-item {
  margin-bottom: 20px;
}

.message {
  max-width: 85%;
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.6;
}

.message-user {
  margin-left: auto;
  background: var(--el-color-primary);
  color: white;
  border-bottom-right-radius: 4px;
}

.message-assistant {
  margin-right: auto;
  background: var(--el-fill-color-light);
  border-bottom-left-radius: 4px;
}

.message-assistant.streaming {
  border: 1px solid var(--el-color-primary-light-5);
}

.message-content {
  font-size: 14px;
  word-break: break-word;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 6px;
  font-size: 11px;
  color: var(--el-text-color-placeholder);
}

.message-user .message-meta {
  justify-content: flex-end;
}

.thinking-time {
  color: var(--el-text-color-secondary);
}

.message-content :deep(pre) {
  background: var(--el-bg-color);
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 8px 0;
}

.message-content :deep(code) {
  font-family: 'Courier New', monospace;
  font-size: 13px;
}

.message-content :deep(strong) {
  font-weight: 600;
}

.tool-uses,
.tasks {
  margin: 12px 0;
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.loading-indicator .is-loading {
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.cursor {
  display: inline-block;
  animation: blink 1s infinite;
  color: var(--el-color-primary);
  font-weight: bold;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

.input-area {
  padding: 16px 20px;
  border-top: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color);
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 12px;
}

/* 节点链条导航 */
.message-nav {
  position: absolute;
  right: 16px;
  top: 80px;
  width: 180px;
  max-height: calc(100vh - 200px);
  overflow-y: auto;
  overflow-x: hidden;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  z-index: 10;
  transition: width 0.3s ease, padding 0.3s ease;
}

.message-nav.collapsed {
  width: 44px;
  padding: 12px 8px;
}

.nav-title {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 12px;
  color: var(--el-text-color-regular);
  white-space: nowrap;
}

.nav-item:hover {
  background: var(--el-fill-color-light);
}

.nav-item.active {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.message-nav.collapsed .nav-item {
  justify-content: center;
  padding: 8px 4px;
}

.nav-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

/* Markdown样式 */
.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4),
.markdown-body :deep(h5),
.markdown-body :deep(h6) {
  margin-top: 16px;
  margin-bottom: 8px;
  font-weight: 600;
  line-height: 1.25;
}

.markdown-body :deep(h1) {
  font-size: 1.5em;
  padding-bottom: 0.3em;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.markdown-body :deep(h2) {
  font-size: 1.3em;
  padding-bottom: 0.3em;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.markdown-body :deep(h3) {
  font-size: 1.1em;
}

.markdown-body :deep(p) {
  margin-top: 0;
  margin-bottom: 10px;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 2em;
  margin-top: 0;
  margin-bottom: 10px;
}

.markdown-body :deep(li) {
  margin-top: 0.25em;
}

.markdown-body :deep(blockquote) {
  padding: 0 1em;
  color: var(--el-text-color-secondary);
  border-left: 0.25em solid var(--el-border-color);
  margin-top: 0;
  margin-bottom: 10px;
}

.markdown-body :deep(pre) {
  background: var(--el-fill-color-light);
  padding: 16px;
  border-radius: 6px;
  overflow-x: auto;
  margin-top: 0;
  margin-bottom: 10px;
}

.markdown-body :deep(code) {
  background: var(--el-fill-color-light);
  padding: 0.2em 0.4em;
  border-radius: 3px;
  font-size: 85%;
  font-family: 'Courier New', monospace;
}

.markdown-body :deep(pre code) {
  background: transparent;
  padding: 0;
  font-size: 100%;
}

.markdown-body :deep(table) {
  border-collapse: collapse;
  border-spacing: 0;
  margin-top: 0;
  margin-bottom: 10px;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  padding: 6px 13px;
  border: 1px solid var(--el-border-color);
}

.markdown-body :deep(th) {
  font-weight: 600;
  background: var(--el-fill-color-lighter);
}

.markdown-body :deep(hr) {
  height: 0.25em;
  padding: 0;
  margin: 24px 0;
  background-color: var(--el-border-color);
  border: 0;
}

.markdown-body :deep(img) {
  max-width: 100%;
  height: auto;
}

.markdown-body :deep(a) {
  color: var(--el-color-primary);
  text-decoration: none;
}

.markdown-body :deep(a:hover) {
  text-decoration: underline;
}

/* 流式聊天容器 */
.streaming-chat {
  position: relative;
}
</style>
