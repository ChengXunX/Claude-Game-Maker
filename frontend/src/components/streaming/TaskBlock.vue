<template>
  <div class="task-block" :class="statusClass">
    <div class="task-header">
      <div class="task-icon">
        <el-icon :size="16"><List /></el-icon>
      </div>
      <div class="task-info">
        <span class="task-title">{{ title || '任务' }}</span>
        <span v-if="taskId" class="task-id">{{ taskId }}</span>
      </div>
      <el-tag :type="statusType" size="small" class="task-status">
        {{ statusText }}
      </el-tag>
    </div>
    <div v-if="description" class="task-description">
      {{ description }}
    </div>
    <div v-if="showProgress" class="task-progress">
      <el-progress
        :percentage="progress"
        :status="progressStatus"
        :stroke-width="6"
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  taskId: {
    type: String,
    default: null
  },
  title: {
    type: String,
    default: null
  },
  description: {
    type: String,
    default: null
  },
  status: {
    type: String,
    default: 'pending' // 'pending', 'running', 'completed', 'failed'
  },
  progress: {
    type: Number,
    default: null
  }
})

const statusClass = computed(() => `status-${props.status}`)

const statusType = computed(() => {
  switch (props.status) {
    case 'pending': return 'info'
    case 'running': return 'warning'
    case 'completed': return 'success'
    case 'failed': return 'danger'
    default: return 'info'
  }
})

const statusText = computed(() => {
  switch (props.status) {
    case 'pending': return '待执行'
    case 'running': return '执行中'
    case 'completed': return '已完成'
    case 'failed': return '失败'
    default: return '未知'
  }
})

const showProgress = computed(() => props.progress !== null && props.progress >= 0)

const progressStatus = computed(() => {
  if (props.status === 'completed') return 'success'
  if (props.status === 'failed') return 'exception'
  return ''
})
</script>

<style scoped>
.task-block {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  margin: 8px 0;
  overflow: hidden;
  background: var(--el-fill-color-lighter);
  transition: all 0.3s;
}

.task-block.status-running {
  border-color: var(--el-color-warning-light-5);
  background: var(--el-color-warning-light-9);
}

.task-block.status-completed {
  border-color: var(--el-color-success-light-5);
  background: var(--el-color-success-light-9);
}

.task-block.status-failed {
  border-color: var(--el-color-danger-light-5);
  background: var(--el-color-danger-light-9);
}

.task-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
}

.task-icon {
  color: var(--el-color-primary);
  display: flex;
  align-items: center;
}

.task-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.task-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.task-id {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  font-family: monospace;
}

.task-description {
  padding: 0 14px 12px;
  font-size: 13px;
  color: var(--el-text-color-regular);
  line-height: 1.5;
}

.task-progress {
  padding: 0 14px 12px;
}
</style>
