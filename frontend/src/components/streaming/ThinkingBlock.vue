<template>
  <div class="thinking-block" :class="{ expanded: isExpanded }">
    <div class="thinking-header" @click="toggleExpand">
      <div class="thinking-icon">
        <el-icon :size="16"><Cpu /></el-icon>
      </div>
      <span class="thinking-label">思考过程</span>
      <el-icon class="expand-icon" :class="{ rotated: isExpanded }">
        <ArrowDown />
      </el-icon>
      <span class="thinking-duration" v-if="duration">
        {{ formatDuration(duration) }}
      </span>
    </div>
    <transition name="slide">
      <div v-show="isExpanded" class="thinking-content">
        <div class="thinking-text" ref="contentRef">
          <slot>{{ content }}</slot>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'

const props = defineProps({
  content: {
    type: String,
    default: ''
  },
  duration: {
    type: Number,
    default: null
  },
  autoExpand: {
    type: Boolean,
    default: false
  }
})

const isExpanded = ref(props.autoExpand)
const contentRef = ref(null)

const toggleExpand = () => {
  isExpanded.value = !isExpanded.value
}

const formatDuration = (ms) => {
  if (ms < 1000) return `${ms}ms`
  return `${(ms / 1000).toFixed(1)}s`
}

// 自动滚动到底部
watch(() => props.content, () => {
  if (contentRef.value && isExpanded.value) {
    contentRef.value.scrollTop = contentRef.value.scrollHeight
  }
})

onMounted(() => {
  if (props.autoExpand) {
    isExpanded.value = true
  }
})
</script>

<style scoped>
.thinking-block {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  margin: 8px 0;
  overflow: hidden;
  background: var(--el-fill-color-lighter);
}

.thinking-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  cursor: pointer;
  user-select: none;
  transition: background-color 0.2s;
}

.thinking-header:hover {
  background: var(--el-fill-color-light);
}

.thinking-icon {
  color: var(--el-color-primary);
  display: flex;
  align-items: center;
}

.thinking-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--el-text-color-regular);
}

.expand-icon {
  margin-left: auto;
  transition: transform 0.3s;
  color: var(--el-text-color-secondary);
}

.expand-icon.rotated {
  transform: rotate(180deg);
}

.thinking-duration {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-left: 8px;
}

.thinking-content {
  padding: 0 14px 14px;
}

.thinking-text {
  font-size: 13px;
  line-height: 1.6;
  color: var(--el-text-color-regular);
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 400px;
  overflow-y: auto;
  padding: 12px;
  background: var(--el-bg-color);
  border-radius: 6px;
  border: 1px solid var(--el-border-color-extra-light);
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
  padding-top: 0;
  padding-bottom: 0;
}
</style>
