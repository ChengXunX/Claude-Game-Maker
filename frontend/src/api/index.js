import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// 创建axios实例
const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    // 从localStorage获取token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response

      // 统一错误消息提取（兼容新旧格式）
      const errorMsg = data?.message || data?.error || '请求失败'

      switch (status) {
        case 401:
          ElMessage.error('登录已过期，请重新登录')
          localStorage.removeItem('token')
          router.push('/login')
          break
        case 403:
          ElMessage.error(errorMsg || '没有权限访问该资源')
          break
        case 404:
          ElMessage.error(errorMsg || '请求的资源不存在')
          break
        case 410:
          // 系统已安装等特殊状态，不显示错误
          break
        case 500:
          ElMessage.error(errorMsg || '服务器内部错误')
          break
        default:
          ElMessage.error(errorMsg)
      }
    } else if (error.request) {
      ElMessage.error('网络连接失败，请检查网络')
    } else {
      ElMessage.error('请求配置错误')
    }

    return Promise.reject(error)
  }
)

export default api

// API接口定义
// 注意：baseURL已经是'/api'，所以这里不需要再加'/api'前缀
export const authApi = {
  login: (data) => api.post('/v1/auth/login', data),
  logout: () => api.post('/v1/auth/logout'),
  getCurrentUser: () => api.get('/v1/auth/me'),
  changePassword: (data) => api.post('/v1/auth/change-password', data)
}

export const agentApi = {
  getAll: () => api.get('/agents'),
  getByProject: (projectId) => api.get(`/agents/project/${projectId}`),
  getById: (projectId, agentRole) => api.get(`/agents/project/${projectId}/${agentRole}`),
  getStatus: (projectId, agentRole) => api.get(`/agents/project/${projectId}/${agentRole}`),
  start: (projectId, agentRole) => api.post(`/agents/project/${projectId}/${agentRole}/start`),
  stop: (projectId, agentRole) => api.post(`/agents/project/${projectId}/${agentRole}/stop`),
  restart: (projectId, agentRole) => api.post(`/agents/project/${projectId}/${agentRole}/restart`),
  sendTask: (projectId, agentRole, data) => api.post(`/agents/project/${projectId}/${agentRole}/task`, data),
  query: (projectId, agentRole, data) => api.post(`/agents/project/${projectId}/${agentRole}/query`, data)
}

export const projectApi = {
  getAll: () => api.get('/projects/api/all'),
  getById: (id) => api.get(`/projects/api/${id}`),
  create: (data) => api.post('/projects/api/create', data),
  import: (data) => api.post('/projects/api/import', data),
  remove: (id) => api.post(`/projects/api/${id}/remove`),
  refresh: (id) => api.post(`/projects/api/${id}/refresh`),
  setRules: (id, data) => api.post(`/projects/${id}/rules`, data),
  setGoal: (id, data) => api.post(`/projects/${id}/goal`, data),
  checkDirectory: (workDir) => api.get('/projects/api/check-directory', { params: { workDir } })
}

// ===== 游戏模板管理 API =====
export const gameTemplateMgmtApi = {
  getAll: () => api.get('/templates'),
  getById: (id) => api.get(`/templates/${id}`),
  createProjectWithTemplate: (data) => api.post('/templates/create-project', data),
  createCustomTemplate: (data) => api.post('/templates/create', data),
  refresh: () => api.post('/templates/refresh')
}

export const alertApi = {
  getRules: () => api.get('/alerts/rules'),
  createRule: (data) => api.post('/alerts/rules', data),
  updateRule: (id, data) => api.put(`/alerts/rules/${id}`, data),
  deleteRule: (id) => api.delete(`/alerts/rules/${id}`),
  getAlerts: (params) => api.get('/alerts', { params }),
  acknowledgeAlert: (id) => api.post(`/alerts/${id}/acknowledge`),
  resolveAlert: (id, data) => api.post(`/alerts/${id}/resolve`, data)
}

export const monitorApi = {
  getMetrics: () => api.get('/system/info'),
  getAgentMetrics: (agentId) => api.get(`/system/info`),
  getSystemHealth: () => api.get('/system/info')
}

export const userApi = {
  getAll: () => api.get('/admin/users'),
  getById: (id) => api.get(`/admin/users/${id}`),
  getPending: () => api.get('/admin/users/pending'),
  create: (data) => api.post('/admin/users', data),
  update: (id, data) => api.put(`/admin/users/${id}`, data),
  delete: (id) => api.delete(`/admin/users/${id}`),
  approve: (id) => api.post(`/admin/users/${id}/approve`),
  reject: (id, data) => api.post(`/admin/users/${id}/reject`, data),
  disable: (id) => api.post(`/admin/users/${id}/disable`),
  updateRole: (userId, roleId) => api.post(`/admin/users/${userId}/role`, { roleId })
}

export const roleApi = {
  getAll: () => api.get('/admin/roles'),
  getById: (id) => api.get(`/admin/roles/${id}`),
  create: (data) => api.post('/admin/roles', data),
  update: (id, data) => api.put(`/admin/roles/${id}`, data),
  delete: (id) => api.delete(`/admin/roles/${id}`)
}

// ===== Token管理 API =====
export const tokenApi = {
  getAll: () => api.get('/tokens'),
  getById: (id) => api.get(`/tokens/${id}`),
  create: (data) => api.post('/tokens', data),
  update: (id, data) => api.put(`/tokens/${id}`, data),
  delete: (id) => api.delete(`/tokens/${id}`),
  assign: (id, agentId) => api.post(`/tokens/${id}/assign`, { agentId }),
  unassign: (id) => api.post(`/tokens/${id}/unassign`),
  getStats: () => api.get('/tokens/stats')
}

// ===== 技能管理 API =====
export const skillApi = {
  getAll: (params) => api.get('/skills', { params }),
  getById: (id) => api.get(`/skills/${id}`),
  create: (data) => api.post('/skills', data),
  update: (id, data) => api.put(`/skills/${id}`, data),
  delete: (id) => api.delete(`/skills/${id}`),
  getByProject: (projectId) => api.get(`/skills/project/${projectId}`),
  generate: (data) => api.post('/skills/generate', data)
}

// ===== 通知管理 API =====
export const notificationApi = {
  getAll: (params) => api.get('/notifications', { params }),
  getById: (id) => api.get(`/notifications/${id}`),
  markAsRead: (id) => api.put(`/notifications/${id}/read`),
  markAllAsRead: () => api.put('/notifications/read-all'),
  delete: (id) => api.delete(`/notifications/${id}`),
  getUnreadCount: () => api.get('/notifications/unread-count')
}

// ===== 操作日志 API =====
export const logApi = {
  getAll: (params) => api.get('/logs', { params }),
  getById: (id) => api.get(`/logs/${id}`),
  export: (params) => api.get('/logs/export', { params, responseType: 'blob' })
}

// ===== 配置中心 API =====
export const configApi = {
  getAll: (params) => api.get('/configs', { params }),
  getByGroup: (group) => api.get(`/configs/group/${group}`),
  getGroups: () => api.get('/configs/groups'),
  update: (id, data) => api.put(`/configs/${id}`, data),
  batchUpdate: (data) => api.put('/configs/batch', data),
  create: (data) => api.post('/configs', data),
  delete: (id) => api.delete(`/configs/${id}`),
  refreshCache: () => api.post('/configs/refresh-cache')
}

// ===== 设备信任 API =====
export const deviceApi = {
  getAll: () => api.get('/devices'),
  remove: (id) => api.delete(`/devices/${id}`),
  removeAll: () => api.delete('/devices')
}

// ===== 钉钉配置 API =====
export const dingtalkApi = {
  getConfig: () => api.get('/dingtalk/config'),
  saveConfig: (data) => api.put('/dingtalk/config', data),
  testConnection: () => api.post('/dingtalk/test')
}

// ===== Agent 招聘 API =====
export const recruitmentApi = {
  getRoles: () => api.get('/recruitment/roles'),
  getTemplates: () => api.get('/recruitment/templates'),
  getCustomTemplates: () => api.get('/recruitment/custom-templates'),
  createCustomTemplate: (data) => api.post('/recruitment/custom-templates', data),
  updateCustomTemplate: (role, data) => api.put(`/recruitment/custom-templates/${role}`, data),
  deleteCustomTemplate: (role) => api.delete(`/recruitment/custom-templates/${role}`),
  recruit: (producerId, data) => api.post(`/recruitment/recruit?producerId=${producerId}`, data),
  recruitFull: (producerId, data) => api.post(`/recruitment/recruit-full?producerId=${producerId}`, data),
  recruitCustom: (producerId, data) => api.post(`/recruitment/recruit-custom?producerId=${producerId}`, data),
  createRequest: (data) => api.post('/recruitment/requests', data),
  getRequests: (params) => api.get('/recruitment/requests', { params }),
  approveRequest: (id) => api.put(`/recruitment/requests/${id}/approve`),
  rejectRequest: (id, data) => api.put(`/recruitment/requests/${id}/reject`, data),
  getStats: () => api.get('/recruitment/stats')
}

// ===== Agent 干预 API =====
export const interventionApi = {
  getAll: (params) => api.get('/interventions/all', { params }),
  getById: (id) => api.get(`/interventions/${id}`),
  sendIntervention: (data) => api.post('/interventions/send', data),
  sendInstruction: (data) => api.post('/interventions/instruction', data),
  overrideDecision: (data) => api.post('/interventions/override-decision', data),
  changeDirection: (data) => api.post('/interventions/change-direction', data),
  pauseAgent: (agentId, reason) => api.post(`/interventions/pause/${agentId}`, null, { params: { reason } }),
  resumeAgent: (agentId, reason) => api.post(`/interventions/resume/${agentId}`, null, { params: { reason } }),
  sendUrgentInstruction: (data) => api.post('/interventions/urgent', data),
  acknowledge: (id, comment) => api.post(`/interventions/${id}/acknowledge`, { comment }),
  execute: (id, comment) => api.post(`/interventions/${id}/execute`, { comment }),
  reject: (id, comment) => api.post(`/interventions/${id}/reject`, { comment }),
  cancel: (id, comment) => api.post(`/interventions/${id}/cancel`, { comment }),
  getStats: () => api.get('/interventions/stats'),
  getPending: () => api.get('/interventions/pending'),
  getAgentInterventions: (agentId) => api.get(`/interventions/agent/${agentId}`)
}

// ===== Agent 健康 API =====
export const healthApi = {
  getAll: () => api.get('/health/agents'),
  getByAgent: (agentId) => api.get(`/health/agent/${agentId}`),
  getHistory: (agentId, params) => api.get(`/health/agent/${agentId}/history`, { params }),
  getStats: () => api.get('/health/stats'),
  restartAgent: (agentId) => api.post(`/health/agent/${agentId}/restart`)
}

// ===== Agent 绩效 API =====
export const performanceApi = {
  getAll: () => api.get('/performance/api/all'),
  getByAgent: (agentId) => api.get(`/performance/api/agent/${agentId}`),
  getRecommendations: (agentId) => api.get(`/performance/api/recommend/${agentId}`),
  getStats: () => api.get('/performance/api/summary')
}

// ===== 绩效管理 API =====
export const performanceMgmtApi = {
  getReviews: (params) => api.get('/performance-management/reviews/agent/me', { params }),
  createReview: (data) => api.post('/performance-management/reviews/submit', data),
  getDismissals: (params) => api.get('/performance-management/dismissals/all', { params }),
  getPendingDismissals: () => api.get('/performance-management/dismissals/pending'),
  approveDismissal: (id) => api.post(`/performance-management/dismissals/${id}/approve`),
  rejectDismissal: (id, data) => api.post(`/performance-management/dismissals/${id}/reject`, data),
  getStats: () => api.get('/performance-management/stats')
}

// ===== 代码审查 API =====
export const reviewApi = {
  getAll: (params) => api.get('/reviews/api/all', { params }),
  getById: (id) => api.get(`/reviews/${id}`),
  submit: (data) => api.post('/reviews/api/submit', data),
  review: (id, data) => api.post(`/reviews/api/${id}/review`, data),
  getStats: () => api.get('/reviews/api/statistics'),
  getPending: () => api.get('/reviews/api/pending')
}

// ===== Git 仓库 API =====
export const gitApi = {
  getRepositories: (projectId) => api.get(`/git/project/${projectId || 'all'}`),
  getById: (id) => api.get(`/git/repository/${id}`),
  addRepository: (data) => api.post('/git/repository', data),
  deleteRepository: (id) => api.delete(`/git/repository/${id}`),
  getBranches: (id) => api.get(`/git/repository/${id}/branches`),
  getCommits: (id, params) => api.get(`/git/repository/${id}/commits`, { params }),
  pull: (id) => api.post(`/git/repository/${id}/pull`),
  push: (id) => api.post(`/git/repository/${id}/push`)
}

// ===== CICD 流水线 API =====
export const pipelineApi = {
  getAll: (params) => api.get('/pipelines/list', { params }),
  getProjectPipelines: (projectId) => api.get(`/pipelines/project/${projectId}`),
  getById: (id) => api.get(`/pipelines/${id}`),
  getStages: (id) => api.get(`/pipelines/${id}/stages`),
  create: (data) => api.post('/pipelines', data),
  update: (id, data) => api.put(`/pipelines/${id}`, data),
  delete: (id) => api.delete(`/pipelines/${id}`),
  trigger: (id) => api.post(`/pipelines/${id}/trigger`),
  approve: (id, data) => api.post(`/pipelines/${id}/approve`, data),
  getStats: () => api.get('/pipelines/stats')
}

// ===== 工作流 API =====
export const workflowApi = {
  getTemplates: () => api.get('/workflow/running'),
  getInstances: (params) => api.get('/workflow/running', { params }),
  start: (data) => api.post('/workflow/start', data),
  cancel: (id) => api.post(`/workflow/${id}/cancel`),
  pause: (id) => api.post(`/workflow/${id}/pause`),
  resume: (id) => api.post(`/workflow/${id}/resume`),
  getStatus: (id) => api.get(`/workflow/${id}`)
}

// ===== 全局搜索 API =====
export const searchApi = {
  search: (params) => api.get('/search/api', { params }),
  getSuggestions: (params) => api.get('/search/api/suggestions', { params })
}

// ===== 资源用量 API =====
export const resourceApi = {
  getStats: () => api.get('/resources/today'),
  getQuota: () => api.get('/resources/quota'),
  getUsage: () => api.get('/resources/agents'),
  getToday: () => api.get('/resources/today'),
  getByDate: (date) => api.get(`/resources/date/${date}`),
  getRange: (start, end) => api.get('/resources/range', { params: { start, end } }),
  getWeekly: () => api.get('/resources/weekly'),
  getMonthly: () => api.get('/resources/monthly'),
  getAgentUsage: () => api.get('/resources/agents'),
  getByAgent: (agentId) => api.get(`/resources/agent/${agentId}`),
  getTrend: (days) => api.get('/resources/trend', { params: { days } })
}

// ===== 通知模板 API =====
export const templateApi = {
  getAll: (params) => api.get('/notification-templates', { params }),
  getById: (id) => api.get(`/notification-templates/${id}`),
  getByCode: (code) => api.get(`/notification-templates/code/${code}`),
  create: (data) => api.post('/notification-templates', data),
  update: (id, data) => api.put(`/notification-templates/${id}`, data),
  delete: (id) => api.delete(`/notification-templates/${id}`),
  preview: (id, data) => api.post(`/notification-templates/${id}/preview`, data)
}

// ===== Agent 调度 API =====
export const schedulerApi = {
  getStatus: () => api.get('/agent-scheduler/status'),
  getTaskQueue: () => api.get('/agent-scheduler/tasks'),
  triggerSchedule: () => api.post('/agent-scheduler/trigger'),
  getConfig: () => api.get('/agent-scheduler/config'),
  updateConfig: (data) => api.put('/agent-scheduler/config', data)
}

// ===== 代码质量 API =====
export const codeQualityApi = {
  getReport: (projectId) => api.get(`/code-quality/${projectId}`),
  getIssues: (projectId, params) => api.get(`/code-quality/${projectId}/issues`, { params }),
  getTrend: (projectId) => api.get(`/code-quality/${projectId}/trend`)
}

// ===== 代码浏览器 API =====
export const codeBrowserApi = {
  getFileTree: (projectId, path = '') => api.get(`/code-browser/tree/${projectId}`, { params: { path } }),
  getFileContent: (projectId, path) => api.get(`/code-browser/content/${projectId}`, { params: { path } }),
  downloadFile: (projectId, path) => api.get(`/code-browser/download/${projectId}`, {
    params: { path },
    responseType: 'blob'
  })
}

// ===== 系统自检 API =====
export const diagnosticApi = {
  run: () => api.post('/diagnostic/run'),
  getResult: () => api.get('/diagnostic/result'),
  quickCheck: () => api.get('/diagnostic/quick'),
  getCpuDetails: () => api.get('/diagnostic/details/cpu'),
  getMemoryDetails: () => api.get('/diagnostic/details/memory'),
  getThreadDetails: () => api.get('/diagnostic/details/threads'),
  getDiskDetails: () => api.get('/diagnostic/details/disk')
}

// ===== 游戏模板 API =====
export const gameTemplateApi = {
  getAll: () => api.get('/game-templates'),
  getById: (id) => api.get(`/game-templates/${id}`),
  match: (description) => api.get('/game-templates/match', { params: { description } }),
  getBestMatch: (description) => api.get('/game-templates/best-match', { params: { description } }),
  create: (data) => api.post('/game-templates', data),
  update: (id, data) => api.put(`/game-templates/${id}`, data),
  delete: (id) => api.delete(`/game-templates/${id}`)
}

// ===== 知识库 API =====
export const knowledgeBaseApi = {
  getStats: () => api.get('/knowledge-base/stats'),
  getTemplateStats: (templateId) => api.get(`/knowledge-base/template-stats/${templateId}`),
  getSolutions: (problemType) => api.get(`/knowledge-base/solutions/${problemType}`),
  getBestPractices: (category) => api.get('/knowledge-base/best-practices', { params: { category } }),
  recordUsage: (data) => api.post('/knowledge-base/record-usage', data),
  recordSolution: (data) => api.post('/knowledge-base/record-solution', data),
  recordBestPractice: (data) => api.post('/knowledge-base/record-best-practice', data)
}

// ===== 知识进化 API =====
export const knowledgeEvolutionApi = {
  processDocument: (data) => api.post('/knowledge-evolution/process-document', data),
  processSkill: (data) => api.post('/knowledge-evolution/process-skill', data),
  learnFromGame: (data) => api.post('/knowledge-evolution/learn-from-game', data),
  extractFromMemory: (data) => api.post('/knowledge-evolution/extract-from-memory', data),
  organize: () => api.post('/knowledge-evolution/organize'),
  evolve: () => api.post('/knowledge-evolution/evolve')
}

// ===== AI助手会话 API =====
export const chatSessionApi = {
  getAll: () => api.get('/chat/sessions'),
  getById: (id) => api.get(`/chat/sessions/${id}`),
  create: (data) => api.post('/chat/sessions', data),
  update: (id, data) => api.put(`/chat/sessions/${id}`, data),
  delete: (id) => api.delete(`/chat/sessions/${id}`),
  getMessages: (sessionId) => api.get(`/chat/sessions/${sessionId}/messages`),
  addMessage: (sessionId, data) => api.post(`/chat/sessions/${sessionId}/messages`, data)
}

// ===== 能力管理 API =====
export const capabilityApi = {
  getAll: () => api.get('/capabilities'),
  getByRole: (agentRole) => api.get(`/capabilities/role/${agentRole}`),
  getByRoleAndProject: (agentRole, projectId) => api.get(`/capabilities/role/${agentRole}/project/${projectId}`),
  create: (data) => api.post('/capabilities', data),
  update: (id, data) => api.put(`/capabilities/${id}`, data),
  delete: (id) => api.delete(`/capabilities/${id}`),
  toggle: (id) => api.post(`/capabilities/${id}/toggle`),
  reload: () => api.post('/capabilities/reload'),
  getByCategory: (category) => api.get(`/capabilities/category/${category}`),
  getByProject: (projectId) => api.get(`/capabilities/project/${projectId}`),
  getRoles: () => api.get('/capabilities/roles'),
  getInvocationLogs: (params) => api.get('/capabilities/invocations', { params }),
  getInvocationLogsByAgent: (agentId, params) => api.get(`/capabilities/invocations/agent/${agentId}`, { params }),
  getInvocationLogsByProject: (projectId, params) => api.get(`/capabilities/invocations/project/${projectId}`, { params })
}

// ===== 上下文健康 API =====
export const contextHealthApi = {
  getAll: () => api.get('/capabilities/health'),
  getSummary: () => api.get('/capabilities/health/summary'),
  recover: (agentId) => api.post(`/capabilities/health/${agentId}/recover`)
}
