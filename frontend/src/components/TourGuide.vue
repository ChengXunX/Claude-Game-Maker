<template>
  <div class="tour-guide" v-if="visible">
    <div class="tour-overlay" @click="handleSkip"></div>
    <div class="tour-tooltip" :style="tooltipStyle">
      <div class="tour-header">
        <span class="tour-step">步骤 {{ currentStep + 1 }} / {{ steps.length }}</span>
        <el-button type="text" @click="handleSkip">跳过</el-button>
      </div>
      <div class="tour-content">
        <h3>{{ steps[currentStep].title }}</h3>
        <p>{{ steps[currentStep].description }}</p>
      </div>
      <div class="tour-footer">
        <el-button v-if="currentStep > 0" @click="handlePrev">上一步</el-button>
        <el-button type="primary" @click="handleNext">
          {{ currentStep === steps.length - 1 ? '完成' : '下一步' }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 新手引导组件
 * 首次登录时的步骤引导
 *
 * 功能：
 * - 步骤引导（仪表盘、Agent、项目、AI 助手）
 * - 引导完成标记
 * - 重新触发引导
 */
import { ref, computed, onMounted } from 'vue'

const props = defineProps({
  steps: {
    type: Array,
    default: () => [
      {
        title: '欢迎使用 Game Maker',
        description: '这是一个 AI 驱动的游戏开发自动化管理系统。让我们快速了解主要功能。',
        target: null
      },
      {
        title: '仪表盘',
        description: '仪表盘是您的工作起点，可以查看系统概览、Agent 状态和最近活动。',
        target: '.dashboard-link'
      },
      {
        title: 'Agent 管理',
        description: '在这里管理您的 AI Agent，包括启动、停止、发送任务等。',
        target: '.agents-link'
      },
      {
        title: '项目管理',
        description: '管理游戏项目，配置项目规则和工作目录。',
        target: '.projects-link'
      },
      {
        title: 'AI 助手',
        description: '与 AI 助手对话，获取开发建议和帮助。',
        target: '.ai-assistant-link'
      }
    ]
  }
})

const emit = defineEmits(['complete', 'skip'])

const visible = ref(false)
const currentStep = ref(0)

/** 工具提示位置 */
const tooltipStyle = computed(() => {
  const step = props.steps[currentStep.value]
  if (!step?.target) {
    return {
      top: '50%',
      left: '50%',
      transform: 'translate(-50%, -50%)'
    }
  }

  const target = document.querySelector(step.target)
  if (!target) {
    return {
      top: '50%',
      left: '50%',
      transform: 'translate(-50%, -50%)'
    }
  }

  const rect = target.getBoundingClientRect()
  return {
    top: `${rect.bottom + 10}px`,
    left: `${rect.left}px`
  }
})

/** 检查是否需要显示引导 */
const checkShowGuide = () => {
  const guided = localStorage.getItem('tour_completed')
  if (!guided) {
    visible.value = true
  }
}

/** 下一步 */
const handleNext = () => {
  if (currentStep.value < props.steps.length - 1) {
    currentStep.value++
  } else {
    handleComplete()
  }
}

/** 上一步 */
const handlePrev = () => {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

/** 完成引导 */
const handleComplete = () => {
  visible.value = false
  localStorage.setItem('tour_completed', 'true')
  emit('complete')
}

/** 跳过引导 */
const handleSkip = () => {
  visible.value = false
  localStorage.setItem('tour_completed', 'true')
  emit('skip')
}

/** 重新开始引导 */
const restart = () => {
  currentStep.value = 0
  visible.value = true
}

onMounted(() => {
  checkShowGuide()
})

defineExpose({
  restart
})
</script>

<style scoped>
.tour-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 9998;
}

.tour-tooltip {
  position: fixed;
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 9999;
  max-width: 400px;
  min-width: 300px;
}

.tour-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.tour-step {
  font-size: 12px;
  color: #999;
}

.tour-content h3 {
  margin: 0 0 8px;
  font-size: 16px;
}

.tour-content p {
  margin: 0;
  color: #666;
  font-size: 14px;
  line-height: 1.5;
}

.tour-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}
</style>
