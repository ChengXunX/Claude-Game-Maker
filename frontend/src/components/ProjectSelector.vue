<template>
  <div class="project-selector">
    <el-select
      v-model="selectedId"
      :placeholder="placeholder"
      clearable
      filterable
      :size="size"
      @change="handleChange"
      :style="{ width: width }"
    >
      <el-option
        v-for="project in projects"
        :key="project.id"
        :label="project.name"
        :value="project.id"
      >
        <div class="project-option">
          <span class="project-name">{{ project.name }}</span>
          <span class="project-desc">{{ project.description || '无描述' }}</span>
        </div>
      </el-option>
    </el-select>
    <slot name="extra"></slot>
  </div>
</template>

<script setup>
/**
 * 项目选择器组件
 * 可复用的项目下拉选择器，支持本地存储和权限过滤
 *
 * @example
 * <ProjectSelector v-model="projectId" @change="handleProjectChange" />
 */
import { ref, computed, onMounted, watch } from 'vue'
import { projectApi } from '@/api'

const props = defineProps({
  /** 当前选中的项目 ID */
  modelValue: {
    type: String,
    default: ''
  },
  /** 占位符文本 */
  placeholder: {
    type: String,
    default: '请选择项目'
  },
  /** 组件大小 */
  size: {
    type: String,
    default: 'default'
  },
  /** 组件宽度 */
  width: {
    type: String,
    default: '300px'
  },
  /** 是否自动加载项目列表 */
  autoLoad: {
    type: Boolean,
    default: true
  },
  /** 是否保存选择到本地存储 */
  saveToLocal: {
    type: Boolean,
    default: true
  },
  /** 本地存储的 key */
  localKey: {
    type: String,
    default: 'selectedProjectId'
  }
})

const emit = defineEmits(['update:modelValue', 'change', 'load'])

/** 项目列表 */
const projects = ref([])

/** 选中的项目 ID */
const selectedId = ref(props.modelValue || (props.saveToLocal ? localStorage.getItem(props.localKey) : ''))

/** 加载项目列表 */
const loadProjects = async () => {
  try {
    const data = await projectApi.getAll()
    projects.value = data || []

    // 验证缓存的项目 ID 是否有效
    if (selectedId.value) {
      const exists = projects.value.some(p => p.id === selectedId.value)
      if (!exists) {
        selectedId.value = ''
        if (props.saveToLocal) {
          localStorage.removeItem(props.localKey)
        }
        emit('update:modelValue', '')
        emit('change', '')
      }
    }

    emit('load', projects.value)
  } catch (error) {
    console.error('加载项目列表失败:', error)
  }
}

/** 处理选择变化 */
const handleChange = (value) => {
  selectedId.value = value || ''
  emit('update:modelValue', value || '')
  emit('change', value || '')

  if (props.saveToLocal) {
    if (value) {
      localStorage.setItem(props.localKey, value)
    } else {
      localStorage.removeItem(props.localKey)
    }
  }
}

/** 获取当前选中的项目 */
const getSelectedProject = () => {
  return projects.value.find(p => p.id === selectedId.value) || null
}

/** 刷新项目列表 */
const refresh = () => {
  loadProjects()
}

// 监听外部值变化
watch(() => props.modelValue, (newVal) => {
  if (newVal !== selectedId.value) {
    selectedId.value = newVal || ''
  }
})

// 暴露方法给父组件
defineExpose({
  loadProjects,
  refresh,
  getSelectedProject,
  projects
})

onMounted(() => {
  if (props.autoLoad) {
    loadProjects()
  }
})
</script>

<style scoped>
.project-selector {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.project-option {
  display: flex;
  flex-direction: column;
  padding: 4px 0;
}

.project-name {
  font-weight: 500;
}

.project-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}
</style>
