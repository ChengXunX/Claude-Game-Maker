<template>
  <div class="terminal-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>系统终端</span>
          <div>
            <el-tag :type="connected ? 'success' : 'danger'" size="small">
              {{ connected ? '已连接' : '未连接' }}
            </el-tag>
            <el-button v-if="!connected" type="primary" size="small" @click="connect">
              连接
            </el-button>
            <el-button v-else type="danger" size="small" @click="disconnect">
              断开
            </el-button>
          </div>
        </div>
      </template>

      <!-- 终端输出区域 -->
      <div class="terminal-output" ref="outputRef">
        <div v-for="(line, index) in outputLines" :key="index" class="terminal-line">
          <span v-html="line"></span>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="terminal-input">
        <el-input
          v-model="inputCommand"
          placeholder="输入命令..."
          @keyup.enter="sendCommand"
          :disabled="!connected"
        >
          <template #prefix>
            <span class="prompt">$</span>
          </template>
          <template #append>
            <el-button @click="sendCommand" :disabled="!connected">
              发送
            </el-button>
          </template>
        </el-input>
      </div>

      <!-- 快捷命令 -->
      <div class="quick-commands">
        <el-button size="small" @click="quickCommand('ls -la')">ls -la</el-button>
        <el-button size="small" @click="quickCommand('pwd')">pwd</el-button>
        <el-button size="small" @click="quickCommand('ps aux')">ps aux</el-button>
        <el-button size="small" @click="quickCommand('df -h')">df -h</el-button>
        <el-button size="small" @click="quickCommand('free -m')">free -m</el-button>
        <el-button size="small" @click="quickCommand('uname -a')">uname -a</el-button>
        <el-button size="small" @click="quickCommand('status')">系统状态</el-button>
        <el-button size="small" @click="quickCommand('help')">帮助</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
/**
 * 系统终端页面
 * 提供 Web 终端功能，支持实时命令执行和输出
 *
 * 功能：
 * - WebSocket 实时通信
 * - 命令历史记录
 * - 快捷命令
 * - ANSI 颜色支持
 *
 * 权限要求：系统管理员
 */
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()

const connected = ref(false)
const inputCommand = ref('')
const outputLines = ref([])
const outputRef = ref(null)
const commandHistory = ref([])
const historyIndex = ref(-1)

let ws = null

/** 连接终端 */
const connect = () => {
  const token = userStore.token
  if (!token) {
    ElMessage.error('请先登录')
    return
  }

  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const host = window.location.host
  const wsUrl = `${protocol}//${host}/ws/terminal?token=${token}`

  ws = new WebSocket(wsUrl)

  ws.onopen = () => {
    connected.value = true
    ElMessage.success('终端已连接')
  }

  ws.onmessage = (event) => {
    const data = event.data
    // 处理 ANSI 颜色代码
    const formatted = formatAnsi(data)
    outputLines.value.push(formatted)
    scrollToBottom()
  }

  ws.onclose = (event) => {
    connected.value = false
    if (event.code !== 1000) {
      ElMessage.warning('终端连接已断开')
    }
  }

  ws.onerror = () => {
    connected.value = false
    ElMessage.error('终端连接失败')
  }
}

/** 断开连接 */
const disconnect = () => {
  if (ws) {
    ws.close(1000)
    ws = null
  }
  connected.value = false
}

/** 发送命令 */
const sendCommand = () => {
  const command = inputCommand.value.trim()
  if (!command || !connected.value) return

  ws.send(command)
  commandHistory.value.unshift(command)
  if (commandHistory.value.length > 100) {
    commandHistory.value.pop()
  }
  historyIndex.value = -1
  inputCommand.value = ''
}

/** 快捷命令 */
const quickCommand = (command) => {
  inputCommand.value = command
  sendCommand()
}

/** 滚动到底部 */
const scrollToBottom = () => {
  nextTick(() => {
    if (outputRef.value) {
      outputRef.value.scrollTop = outputRef.value.scrollHeight
    }
  })
}

/** 格式化 ANSI 颜色代码 */
const formatAnsi = (text) => {
  return text
    .replace(/\033\[1;31m(.*?)\033\[0m/g, '<span style="color: #ff6b6b">$1</span>')
    .replace(/\033\[1;32m(.*?)\033\[0m/g, '<span style="color: #51cf66">$1</span>')
    .replace(/\033\[1;33m(.*?)\033\[0m/g, '<span style="color: #ffd43b">$1</span>')
    .replace(/\033\[1;34m(.*?)\033\[0m/g, '<span style="color: #339af0">$1</span>')
    .replace(/\033\[1;35m(.*?)\033\[0m/g, '<span style="color: #cc5de8">$1</span>')
    .replace(/\033\[1;36m(.*?)\033\[0m/g, '<span style="color: #22b8cf">$1</span>')
    .replace(/\033\[2J\033\[H/g, '') // 清屏
    .replace(/\033\[[0-9;]*m/g, '') // 清理其他ANSI代码
}

onMounted(() => {
  // 自动连接
  connect()
})

onUnmounted(() => {
  disconnect()
})
</script>

<style scoped>
.terminal-page {
  padding: 20px;
  height: calc(100vh - 100px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.terminal-output {
  background: #1e1e1e;
  color: #d4d4d4;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.5;
  padding: 16px;
  min-height: 400px;
  max-height: 60vh;
  overflow-y: auto;
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-all;
}

.terminal-line {
  margin-bottom: 2px;
}

.terminal-input {
  margin-top: 16px;
}

.prompt {
  font-weight: bold;
  color: #51cf66;
}

.quick-commands {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

:deep(.el-input-group__append) {
  background-color: #409eff;
  border-color: #409eff;
  color: #fff;
}

:deep(.el-input-group__append:hover) {
  background-color: #66b1ff;
  border-color: #66b1ff;
}
</style>
