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
          // 优先显示后端返回的具体错误原因（如账号被禁用、审核未通过等）
          ElMessage.error(errorMsg || '登录已过期，请重新登录')
          localStorage.removeItem('token')
          router.push('/login')
          break
        case 403:
          // 账号状态异常（禁用/拒绝/待审核），显示具体原因并跳转登录页
          if (data?.message && ['账号已被禁用', '账号审核未通过', '账号正在审核中', '账号状态异常'].some(k => data.message.includes(k))) {
            ElMessage.error(data.message)
            localStorage.removeItem('token')
            router.push('/login')
          } else {
            ElMessage.error(errorMsg || '没有权限访问该资源')
          }
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
  changePassword: (data) => api.post('/v1/auth/change-password', data),
  updateProfile: (data) => api.put('/v1/auth/profile', data),
  requestEmailChange: (data) => api.post('/v1/auth/request-email-change', data),
  // 邮箱换绑相关
  checkEmailChangeCondition: () => api.get('/v1/auth/email-change-check'),
  sendCurrentEmailCode: () => api.post('/v1/auth/send-current-email-code'),
  sendNewEmailCode: (data) => api.post('/v1/auth/send-new-email-code', data),
  changeEmail: (data) => api.post('/v1/auth/change-email', data)
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
  query: (projectId, agentRole, data) => api.post(`/agents/project/${projectId}/${agentRole}/query`, data),
  getReasoningDepth: (projectId, agentRole) => api.get(`/agents/project/${projectId}/${agentRole}/reasoning-depth`),
  setReasoningDepth: (projectId, agentRole, depth) => api.put(`/agents/project/${projectId}/${agentRole}/reasoning-depth`, { reasoningDepth: depth }),
  setThinkingMode: (projectId, agentRole, thinkingMode) => api.put(`/agents/project/${projectId}/${agentRole}/thinking-mode`, { thinkingMode }),
  getThinkingMode: (projectId, agentRole) => api.get(`/agents/project/${projectId}/${agentRole}/thinking-mode`)
}

export const projectApi = {
  getAll: () => api.get('/projects/api/all'),
  getById: (id) => api.get(`/projects/api/${id}`),
  create: (data) => api.post('/projects/api/create', data),
  import: (data) => api.post('/projects/api/import', data),
  remove: (id) => api.post(`/projects/api/${id}/remove`),
  archive: (id) => api.post(`/projects/api/${id}/archive`),
  refresh: (id) => api.post(`/projects/api/${id}/refresh`),
  setRules: (id, data) => api.post(`/projects/${id}/rules`, data),
  setGoal: (id, data) => api.post(`/projects/${id}/goal`, data),
  checkDirectory: (workDir) => api.get('/projects/api/check-directory', { params: { workDir } }),
  getMilestones: (id) => api.get(`/projects/api/${id}/milestones`),
  getDirectories: (id) => api.get(`/projects/api/${id}/directories`),
  addDirectory: (id, data) => api.post(`/projects/api/${id}/directories`, data),
  removeDirectory: (id, dirPath) => api.delete(`/projects/api/${id}/directories/${encodeURIComponent(dirPath)}`),
  verifyMilestone: (projectId, milestoneId, data) => api.post(`/projects/api/${projectId}/milestones/${milestoneId}/verify`, data),
  getVerificationDoc: (id) => api.get(`/projects/api/${id}/verification-doc`),
  // 版本迭代相关
  getIterationStats: (id) => api.get(`/projects/api/${id}/iteration-stats`),
  getIterationRecords: (id) => api.get(`/projects/api/${id}/iteration-records`),
  rollbackVersion: (id, targetVersion) => api.post(`/projects/api/${id}/rollback`, { targetVersion }),
  getRollbackableVersions: (id) => api.get(`/projects/api/${id}/rollbackable-versions`),
  getVersionComparison: (id, version1, version2) => api.get(`/projects/api/${id}/version-comparison`, { params: { version1, version2 } }),
  exportIterationReport: (id) => api.get(`/projects/api/${id}/export-iteration-report`, { responseType: 'blob' }),
  getIterationTemplates: () => api.get('/projects/api/iteration-templates'),
  // 管理员版本迭代指令
  getVersionInstruction: (id) => api.get(`/projects/api/${id}/version-instruction`),
  saveVersionInstruction: (id, instruction) => api.post(`/projects/api/${id}/version-instruction`, { instruction }),
  // 数据清理
  cleanData: (id) => api.post(`/projects/api/${id}/clean-data`),
  resetMilestones: (id) => api.post(`/projects/api/${id}/reset-milestones`)
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
  resolveAlert: (id, data) => api.post(`/alerts/${id}/resolve`, data),
  batchAcknowledge: () => api.post('/alerts/batch-acknowledge'),
  getStats: () => api.get('/alerts/stats')
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
  assign: (id, agentId, activation) => api.post(`/tokens/${id}/assign`, { agentId, activation: activation || 'immediate' }),
  getQuota: (id) => api.get(`/tokens/${id}/quota`),
  getStats: () => api.get('/tokens/stats'),
  getAgents: () => api.get('/tokens/agents'),
  testConnection: (data) => api.post('/configs/test-ai-connection', data, { timeout: 30000 })
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
  refreshCache: () => api.post('/configs/refresh-cache'),
  testAiConnection: (data) => api.post('/configs/test-ai-connection', data, { timeout: 30000 }),
  reveal: (id) => api.get(`/configs/${id}/reveal`)
}

// ===== 邮件配置 API =====
export const emailApi = {
  getSettings: () => api.get('/admin/api/settings/email'),
  saveSettings: (data) => api.post('/admin/api/settings/email', data),
  testConnection: (data) => api.post('/admin/api/settings/email/test', data, { timeout: 15000 }),
  sendTestEmail: (toEmail, config) => api.post('/admin/api/settings/email/send-test', { toEmail, ...config }, { timeout: 15000 })
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
  getRoleDetail: (roleId) => api.get(`/recruitment/roles/${roleId}`),
  updateRolePrompt: (roleId, data) => api.put(`/recruitment/roles/${roleId}/prompt`, data),
  resetRolePrompt: (roleId) => api.post(`/recruitment/roles/${roleId}/reset-prompt`),
  generateRolePrompt: (roleId, data) => api.post(`/recruitment/roles/${roleId}/generate-prompt`, data),
  evolveRolePrompt: (roleId) => api.post(`/recruitment/roles/${roleId}/evolve`),
  getEvolutionMeta: (roleId) => api.get(`/recruitment/roles/${roleId}/evolution-meta`),
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
  getAgentInterventions: (agentId) => api.get(`/interventions/agent/${agentId}`),
  startVersionIteration: (data) => api.post('/interventions/version-iteration', data)
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
  getAllReviews: () => api.get('/performance-management/reviews/producer/all'),
  getReviewsByAgent: (agentId) => api.get(`/performance-management/reviews/agent/${agentId}`),
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
  // 模板管理
  getTemplates: () => api.get('/workflow/templates'),
  createTemplate: (data) => api.post('/workflow/templates', data),
  deleteTemplate: (id) => api.delete(`/workflow/templates/${id}`),
  generateTemplate: (data) => api.post('/workflow/templates/generate', data),

  // 实例管理
  getInstances: (params) => api.get('/workflow/running', { params }),
  getAllInstances: () => api.get('/workflow/instances'),
  getInstancesByProject: (projectId) => api.get(`/workflow/instances/project/${projectId}`),
  getInstanceDetail: (id) => api.get(`/workflow/instances/${id}/detail`),
  getStepExecutions: (id) => api.get(`/workflow/instances/${id}/steps`),
  start: (data) => api.post('/workflow/start', data),
  cancel: (id) => api.post(`/workflow/${id}/cancel`),
  pause: (id) => api.post(`/workflow/${id}/pause`),
  resume: (id) => api.post(`/workflow/${id}/resume`),
  getStatus: (id) => api.get(`/workflow/${id}`),

  // 审批管理
  getPendingApprovals: () => api.get('/workflow/approvals/pending'),
  approveStep: (instanceId, stepId, data) => api.post(`/workflow/instances/${instanceId}/steps/${stepId}/approve`, data),
  rejectStep: (instanceId, stepId, data) => api.post(`/workflow/instances/${instanceId}/steps/${stepId}/reject`, data),

  // 审计日志
  getAuditLogs: (instanceId) => api.get(`/workflow/instances/${instanceId}/audit-logs`),

  // Agent评分
  getAgentScores: (role, projectId) => api.get('/workflow/agent-scores', { params: { role, projectId } })
}

// ===== 审批管理 API =====
export const approvalApi = {
  getAll: () => api.get('/approvals/all'),
  getPending: () => api.get('/approvals/pending'),
  getByProject: (projectId) => api.get(`/approvals/project/${projectId}`),
  approve: (requestId, data) => api.post(`/approvals/${requestId}/approve`, data),
  getCount: () => api.get('/approvals/count'),
  getProjectCount: (projectId) => api.get(`/approvals/count/project/${projectId}`)
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
  preview: (id, data) => api.post(`/notification-templates/${id}/preview`, data),
  testSend: (id) => api.post(`/notification-templates/${id}/test`),
  testSendEmail: (id, toEmail) => api.post(`/notification-templates/${id}/test-email`, { toEmail })
}

// ===== Agent 调度 API =====
export const schedulerApi = {
  getStatus: () => api.get('/agent-scheduler/status'),
  getTaskQueue: () => api.get('/agent-scheduler/tasks'),
  triggerSchedule: () => api.post('/agent-scheduler/trigger'),
  getConfig: () => api.get('/agent-scheduler/config'),
  updateConfig: (data) => api.put('/agent-scheduler/config', data),
  cancelTask: (taskId) => api.post(`/agent-scheduler/tasks/${taskId}/cancel`),
  getProducerStatus: () => api.get('/agent-scheduler/producer-status'),
  getProducerDecisions: (limit) => api.get('/agent-scheduler/producer-decisions', { params: { limit: limit || 10 } }),
  // 智能调度统计
  getStats: () => api.get('/scheduler/stats'),
  // 协作统计
  getCollaborationStats: () => api.get('/scheduler/collaboration/stats'),
  // 获取项目协作会话
  getProjectCollaborations: (projectId) => api.get(`/scheduler/collaboration/project/${projectId}`),
  // 获取协作会话详情
  getCollaborationSession: (sessionId) => api.get(`/scheduler/collaboration/session/${sessionId}`),
  // 获取质量门禁配置
  getQualityGateConfigs: () => api.get('/scheduler/quality-gates'),
  // 执行质量评估
  assessQuality: (projectId, data) => api.post(`/scheduler/quality-gates/assess/${projectId}`, data),
  // 更新质量门禁配置
  updateQualityGate: (gateId, data) => api.put(`/scheduler/quality-gates/${gateId}`, data),
  // 获取 Agent 综合评估
  getAgentEvaluations: (projectId) => api.get(`/scheduler/evaluations/${projectId}`),
  // 获取质量评估历史
  getQualityAssessmentHistory: (projectId) => api.get(`/scheduler/quality-gates/history/${projectId}`),
  // 获取最新质量评估
  getLatestQualityAssessment: (projectId) => api.get(`/scheduler/quality-gates/latest/${projectId}`)
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
  getSolutionsList: () => api.get('/knowledge-base/solutions'),
  getBestPractices: (category) => api.get('/knowledge-base/best-practices', { params: { category } }),
  getUsageRecords: () => api.get('/knowledge-base/usage-records'),
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
  evolve: () => api.post('/knowledge-evolution/evolve'),
  getStats: () => api.get('/knowledge-evolution/stats'),
  getLearnedPatterns: () => api.get('/knowledge-evolution/learned-patterns'),
  getLearnedSkills: () => api.get('/knowledge-evolution/learned-skills')
}

// ===== AI助手会话 API =====
export const chatSessionApi = {
  getAll: () => api.get('/chat/sessions'),
  getById: (id) => api.get(`/chat/sessions/${id}`),
  create: (data) => api.post('/chat/sessions', data),
  update: (id, data) => api.put(`/chat/sessions/${id}`, data),
  delete: (id) => api.delete(`/chat/sessions/${id}`),
  getMessages: (sessionId) => api.get(`/chat/sessions/${sessionId}/messages`),
  addMessage: (sessionId, data) => api.post(`/chat/sessions/${sessionId}/messages`, data),
  // 飞书会话相关
  getFeishuSessions: () => api.get('/chat/sessions/feishu'),
  getFeishuSession: (id) => api.get(`/chat/sessions/feishu/${id}`),
  clearFeishuSession: (id) => api.delete(`/chat/sessions/feishu/${id}/clear`)
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

// ===== 游戏运行时验证 API =====
export const gameVerifyApi = {
  // 触发项目验证
  verify: (projectId) => api.post(`/game-verify/${projectId}/verify`),
  // AI 深度分析游戏质量（后台异步执行，设置较长超时防止误报）
  analyze: (projectId) => api.post(`/game-verify/${projectId}/analyze`, null, { timeout: 60000 }),
  // 游戏设计审查
  designReview: (projectId) => api.post(`/game-verify/${projectId}/design-review`, null, { timeout: 60000 }),
  // 获取分析任务状态（轮询接口，设置较长超时）
  getTaskStatus: (taskId) => api.get(`/game-verify/task/${taskId}`, { timeout: 30000 }),
  // 获取项目最新分析状态
  getAnalysisStatus: (projectId) => api.get(`/game-verify/${projectId}/analyze/status`, { timeout: 15000 }),
  // 获取项目分析历史
  getAnalysisHistory: (projectId) => api.get(`/game-verify/${projectId}/analyze/history`, { timeout: 15000 }),
  // 获取项目验证状态
  getStatus: (projectId) => api.get(`/game-verify/${projectId}/status`),
  // 批量获取验证状态
  batchStatus: (projectIds) => api.post('/game-verify/batch-status', projectIds),
  // 清除验证缓存
  clearCache: (projectId) => api.delete(`/game-verify/${projectId}/cache`),
  // 触发完整验证（结构+构建+运行+质量）
  triggerVerify: (projectId, projectDir) => api.post(`/game-verify/projects/${projectId}/verify`, null, { params: { projectDir } }),
  // 获取最近验证结果
  getLatestVerify: (projectId) => api.get(`/game-verify/projects/${projectId}/verify/latest`),
  // 启动游戏预览
  startPreview: (projectId, projectDir, port) => api.post(`/game-verify/projects/${projectId}/preview/start`, null, { params: { projectDir, port } }),
  // 停止游戏预览
  stopPreview: (projectId) => api.post(`/game-verify/projects/${projectId}/preview/stop`)
}

// ===== 项目Agent配置 API =====
export const projectAgentConfigApi = {
  getConfigs: (projectId) => api.get(`/projects/${projectId}/agents/configs`),
  getConfig: (projectId, agentRole) => api.get(`/projects/${projectId}/agents/${agentRole}/config`),
  saveConfig: (projectId, agentRole, data) => api.post(`/projects/${projectId}/agents/${agentRole}/config`, data),
  getPrompt: (projectId, agentRole) => api.get(`/projects/${projectId}/agents/${agentRole}/prompt`),
  optimizePrompt: (projectId, agentRole, params) => api.post(`/projects/${projectId}/agents/${agentRole}/optimize`, params || {}),
  getWeights: (projectId, agentRole) => api.get(`/projects/${projectId}/agents/${agentRole}/weights`),
  getPerformanceWeights: (projectId, agentRole) => api.get(`/projects/${projectId}/agents/${agentRole}/performance-weights`),
  savePerformanceWeights: (projectId, agentRole, weights) => api.put(`/projects/${projectId}/agents/${agentRole}/performance-weights`, weights)
}

// ===== Agent 工具 API =====

// 检查点管理
export const checkpointApi = {
  list: (projectId, agentId) => api.get(`/agent-tools/checkpoints/${projectId}/${agentId}`),
  get: (projectId, agentId, timestamp) => api.get(`/agent-tools/checkpoints/${projectId}/${agentId}/${timestamp}`),
  create: (data) => api.post('/agent-tools/checkpoints', data),
  restore: (projectId, agentId, timestamp) => api.post(`/agent-tools/checkpoints/${projectId}/${agentId}/${timestamp}/restore`),
  delete: (projectId, agentId, timestamp) => api.delete(`/agent-tools/checkpoints/${projectId}/${agentId}/${timestamp}`)
}

// 记忆全文搜索
export const memorySearchApi = {
  search: (projectId, agentId, query) => api.get('/agent-tools/memory/search', { params: { projectId, agentId, query } }),
  searchGlobal: (agentId, query) => api.get('/agent-tools/memory/search/global', { params: { agentId, query } }),
  rebuildIndex: (data) => api.post('/agent-tools/memory/rebuild-index', data)
}

// Dream 知识提取
export const dreamApi = {
  trigger: (data) => api.post('/agent-tools/dream/trigger', data)
}

// 裁判评估
export const goalJudgeApi = {
  evaluate: (data) => api.post('/agent-tools/goal-judge/evaluate', data)
}

// 子代理管理
export const subAgentApi = {
  spawn: (data) => api.post('/agent-tools/sub-agents/spawn', data),
  list: (parentAgentId) => api.get(`/agent-tools/sub-agents/${parentAgentId}`),
  terminate: (subAgentId) => api.post(`/agent-tools/sub-agents/${subAgentId}/terminate`),
  stats: () => api.get('/agent-tools/sub-agents/stats')
}

// Skill 文件发现
export const skillDiscoveryApi = {
  discover: (projectId) => api.get(`/agent-tools/skills/discover/${projectId}`)
}

// Distill 工作流发现
export const distillApi = {
  trigger: (data) => api.post('/agent-tools/distill/trigger', data)
}

// 快照回滚
export const snapshotApi = {
  list: (projectId, agentId) => api.get(`/agent-tools/snapshots/${projectId}/${agentId}`),
  create: (data) => api.post('/agent-tools/snapshots', data),
  restore: (projectId, agentId, snapshotId) => api.post(`/agent-tools/snapshots/${projectId}/${agentId}/${snapshotId}/restore`),
  undo: (projectId, agentId) => api.post(`/agent-tools/snapshots/${projectId}/${agentId}/undo`),
  delete: (projectId, agentId, snapshotId) => api.delete(`/agent-tools/snapshots/${projectId}/${agentId}/${snapshotId}`)
}

// 任务门禁
export const taskGateApi = {
  check: (data) => api.post('/agent-tools/task-gate/check', data)
}

// 预算上下文注入
export const budgetedContextApi = {
  get: (data) => api.post('/agent-tools/context/budgeted', data)
}

// Agent 工具权限
export const toolPermissionApi = {
  get: (agentId) => api.get(`/agent-tools/tool-permissions/${agentId}`),
  set: (agentId, data) => api.post(`/agent-tools/tool-permissions/${agentId}`, data)
}

// 会话分叉
export const sessionForkApi = {
  create: (data) => api.post('/agent-tools/forks', data),
  list: (parentAgentId) => api.get(`/agent-tools/forks/${parentAgentId}`),
  merge: (forkId, strategy) => api.post(`/agent-tools/forks/${forkId}/merge?strategy=${strategy || 'merge'}`),
  discard: (forkId) => api.post(`/agent-tools/forks/${forkId}/discard`)
}

/** 飞书集成 API */
export const feishuApi = {
  /** 生成飞书绑定验证码 */
  generateBindCode: () => api.post('/feishu/bind-code'),
  /** 查询飞书绑定状态 */
  getBindStatus: () => api.get('/feishu/bind-status'),
  /** 解绑飞书 */
  unbind: () => api.post('/feishu/unbind')
}


// ===== 知识图谱 API =====
export const knowledgeGraphApi = {
  getGraph: (projectId) => api.get(`/knowledge-graph/${projectId}`),
  getNodes: (projectId) => api.get(`/knowledge-graph/${projectId}/nodes`),
  getEdges: (projectId) => api.get(`/knowledge-graph/${projectId}/edges`),
  getNeighbors: (projectId, nodeId) => api.get(`/knowledge-graph/${projectId}/neighbors/${nodeId}`),
  search: (projectId, keyword) => api.get(`/knowledge-graph/${projectId}/search`, { params: { keyword } }),
  build: (projectId) => api.post(`/knowledge-graph/${projectId}/build`),
  getStats: (projectId) => api.get(`/knowledge-graph/${projectId}/stats`)
}

// ===== 质量预测 API =====
export const qualityPredictionApi = {
  getLatest: (projectId) => api.get(`/quality-prediction/${projectId}`),
  predict: (projectId) => api.post(`/quality-prediction/${projectId}/predict`),
  getHistory: (projectId) => api.get(`/quality-prediction/${projectId}/history`)
}

// ===== 迭代适应 API =====
export const iterationAdaptApi = {
  getRecommendation: (projectId) => api.get(`/iteration-adapt/recommendation/${projectId}`),
  apply: (projectId) => api.post(`/iteration-adapt/apply/${projectId}`),
  getHistory: (projectId) => api.get(`/iteration-adapt/history/${projectId}`)
}

// ===== 多轮推理 API =====
export const multiTurnApi = {
  reason: (data) => api.post('/multi-turn/reason', data),
  getStatus: (recordId) => api.get(`/multi-turn/status/${recordId}`),
  getStats: (projectId) => api.get(`/multi-turn/stats/${projectId}`),
  getHistory: (projectId) => api.get(`/multi-turn/history/${projectId}`)
}

