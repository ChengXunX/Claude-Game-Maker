<template>
  <div class="tool-use-block" :class="{ expanded: isExpanded, success: isSuccess, error: isError }">
    <div class="tool-header" @click="toggleExpand">
      <div class="tool-icon">
        <el-icon :size="16"><SetUp /></el-icon>
      </div>
      <span class="tool-name">{{ toolName }}</span>
      <el-tag v-if="status" :type="statusType" size="small" class="tool-status">
        {{ statusText }}
      </el-tag>
      <el-icon class="expand-icon" :class="{ rotated: isExpanded }">
        <ArrowDown />
      </el-icon>
    </div>
    <transition name="slide">
      <div v-show="isExpanded" class="tool-content">
        <!-- 工具输入 -->
        <div v-if="input && Object.keys(input).length > 0" class="tool-section">
          <div class="section-label">
            <el-icon><Document /></el-icon>
            <span>输入参数</span>
          </div>
          <div class="tool-input">
            <div v-for="(value, key) in input" :key="key" class="input-item">
              <span class="input-key">{{ key }}:</span>
              <span class="input-value">{{ formatValue(value) }}</span>
            </div>
          </div>
        </div>

        <!-- 工具结果 -->
        <div v-if="result" class="tool-section">
          <div class="section-label">
            <el-icon><Finished /></el-icon>
            <span>执行结果</span>
          </div>
          <div class="tool-result">
            <pre>{{ result }}</pre>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="tool-actions">
          <el-button size="small" @click.stop="copyInput">
            <el-icon><CopyDocument /></el-icon>
            复制输入
          </el-button>
          <el-button v-if="result" size="small" @click.stop="copyResult">
            <el-icon><CopyDocument /></el-icon>
            复制结果
          </el-button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  toolName: {
    type: String,
    required: true
  },
  input: {
    type: Object,
    default: () => ({})
  },
  result: {
    type: String,
    default: null
  },
  status: {
    type: String,
    default: null // 'running', 'success', 'error'
  },
  defaultExpanded: {
    type: Boolean,
    default: false
  }
})

const isExpanded = ref(props.defaultExpanded)

const isSuccess = computed(() => props.status === 'success')
const isError = computed(() => props.status === 'error')

const statusType = computed(() => {
  switch (props.status) {
    case 'running': return 'warning'
    case 'success': return 'success'
    case 'error': return 'danger'
    default: return 'info'
  }
})

const statusText = computed(() => {
  switch (props.status) {
    case 'running': return '执行中'
    case 'success': return '成功'
    case 'error': return '失败'
    default: return ''
  }
})

const toggleExpand = () => {
  isExpanded.value = !isExpanded.value
}

const formatValue = (value) => {
  if (typeof value === 'object') {
    return JSON.stringify(value, null, 2)
  }
  return String(value)
}

const copyInput = async () => {
  try {
    await navigator.clipboard.writeText(JSON.stringify(props.input, null, 2))
    ElMessage.success('输入参数已复制')
  } catch {
    ElMessage.error('复制失败')
  }
}

const copyResult = async () => {
  try {
    await navigator.clipboard.writeText(props.result)
    ElMessage.success('执行结果已复制')
  } catch {
    ElMessage.error('复制失败')
  }
}
</script>

<style scoped>
.tool-use-block {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  margin: 8px 0;
  overflow: hidden;
  background: var(--el-fill-color-lighter);
  transition: border-color 0.3s;
}

.tool-use-block.success {
  border-color: var(--el-color-success-light-5);
}

.tool-use-block.error {
  border-color: var(--el-color-danger-light-5);
}

.tool-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  cursor: pointer;
  user-select: none;
  transition: background-color 0.2s;
}

.tool-header:hover {
  background: var(--el-fill-color-light);
}

.tool-icon {
  color: var(--el-color-warning);
  display: flex;
  align-items: center;
}

.tool-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  font-family: monospace;
}

.tool-status {
  margin-left: auto;
}

.expand-icon {
  margin-left: 8px;
  transition: transform 0.3s;
  color: var(--el-text-color-secondary);
}

.expand-icon.rotated {
  transform: rotate(180deg);
}

.tool-content {
  padding: 0 14px 14px;
}

.tool-section {
  margin-bottom: 12px;
}

.section-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 500;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
}

.tool-input {
  background: var(--el-bg-color);
  border-radius: 6px;
  padding: 12px;
  border: 1px solid var(--el-border-color-extra-light);
}

.input-item {
  display: flex;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 13px;
}

.input-item:last-child {
  margin-bottom: 0;
}

.input-key {
  color: var(--el-color-primary);
  font-weight: 500;
  font-family: monospace;
  min-width: 100px;
}

.input-value {
  color: var(--el-text-color-regular);
  word-break: break-all;
}

.tool-result {
  background: var(--el-bg-color);
  border-radius: 6px;
  padding: 12px;
  border: 1px solid var(--el-border-color-extra-light);
}

.tool-result pre {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--el-text-color-regular);
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 300px;
  overflow-y: auto;
}

.tool-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.slide-enter-active,
.slide-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}

.slide-enter-from,
.slide-leave-to {
  max-height: 0;
  opacity: 0;
}
</style>
