import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { projectApi } from '@/api'

/**
 * 项目状态管理
 * 存储当前选中的项目，所有管理页面通过此 store 获取项目上下文
 */
export const useProjectStore = defineStore('project', () => {
  /** 项目列表 */
  const projects = ref([])

  /** 当前选中的项目 ID */
  const currentProjectId = ref(localStorage.getItem('currentProjectId') || '')

  /** 当前选中的项目对象 */
  const currentProject = computed(() => {
    return projects.value.find(p => p.id === currentProjectId.value) || null
  })

  /** 是否已选择项目 */
  const hasProject = computed(() => !!currentProjectId.value)

  /** 加载项目列表 */
  async function loadProjects() {
    try {
      const data = await projectApi.getAll()
      projects.value = data || []
    } catch (e) {
      console.error('Failed to load projects:', e)
    }
  }

  /** 切换当前项目 */
  function setCurrentProject(projectId) {
    currentProjectId.value = projectId || ''
    if (projectId) {
      localStorage.setItem('currentProjectId', projectId)
    } else {
      localStorage.removeItem('currentProjectId')
    }
  }

  /** 清除选择 */
  function clearProject() {
    currentProjectId.value = ''
    localStorage.removeItem('currentProjectId')
  }

  return {
    projects,
    currentProjectId,
    currentProject,
    hasProject,
    loadProjects,
    setCurrentProject,
    clearProject
  }
})
