import { createRouter, createWebHistory } from 'vue-router'

/**
 * 路由配置
 *
 * 菜单分组：
 * - 工作台：仪表盘、AI助手
 * - Agent 管理：Agent列表、招聘、干预、健康、绩效、调度
 * - 项目管理：项目列表、代码审查、Git仓库、流水线、工作流
 * - 运维中心：监控、告警、资源用量
 * - 通知中心：通知管理、通知模板
 * - 系统管理：用户、角色、日志、配置、技能、Token、设备、钉钉
 *
 * 权限控制：
 * - meta.permission: 控制菜单显示的权限（不设置则所有用户可见）
 * - meta.hidden: 控制菜单是否隐藏（详情页等不需要在菜单中显示）
 * - meta.group: 菜单分组名称
 */
const routes = [
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      // ===== 工作台 =====
      {
        path: '',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '仪表盘', icon: 'Odometer', group: '工作台' }
      },
      {
        path: 'ai-assistant',
        name: 'AiAssistant',
        component: () => import('@/views/ai-assistant/index.vue'),
        meta: { title: 'AI助手', icon: 'ChatDotRound', group: '工作台' }
      },
      {
        path: 'search',
        name: 'Search',
        component: () => import('@/views/search/index.vue'),
        meta: { title: '全局搜索', icon: 'Search', hidden: true }
      },

      // ===== Agent 管理 =====
      {
        path: 'agents',
        name: 'Agents',
        component: () => import('@/views/agents/index.vue'),
        meta: { title: 'Agent列表', icon: 'UserFilled', group: 'Agent管理', permission: 'agents:view' }
      },
      {
        path: 'agents/:projectId/:agentRole',
        name: 'AgentDetail',
        component: () => import('@/views/agents/detail.vue'),
        meta: { title: 'Agent详情', hidden: true }
      },
      {
        path: 'recruitment',
        name: 'Recruitment',
        component: () => import('@/views/recruitment/index.vue'),
        meta: { title: 'Agent招聘', icon: 'Avatar', group: 'Agent管理', permission: 'agents:manage' }
      },
      {
        path: 'recruitment/approval',
        name: 'RecruitmentApproval',
        component: () => import('@/views/recruitment/approval.vue'),
        meta: { title: '招聘审批', hidden: true }
      },
      {
        path: 'interventions',
        name: 'Interventions',
        component: () => import('@/views/interventions/index.vue'),
        meta: { title: 'Agent干预', icon: 'Warning', group: 'Agent管理', permission: 'agents:manage' }
      },
      {
        path: 'health',
        name: 'Health',
        component: () => import('@/views/health/index.vue'),
        meta: { title: 'Agent健康', icon: 'CircleCheck', group: 'Agent管理', permission: 'agents:view' }
      },
      {
        path: 'game-verify',
        name: 'GameVerify',
        component: () => import('@/views/game-verify/index.vue'),
        meta: { title: '游戏验证', icon: 'Monitor', group: '项目管理', permission: 'projects:view' }
      },

      {
        path: 'performance',
        name: 'Performance',
        component: () => import('@/views/performance/index.vue'),
        meta: { title: 'Agent绩效', icon: 'TrendCharts', group: 'Agent管理', permission: 'agents:view' }
      },
      {
        path: 'performance/:agentId',
        name: 'PerformanceDetail',
        component: () => import('@/views/performance/detail.vue'),
        meta: { title: '绩效详情', hidden: true }
      },
      {
        path: 'scheduler',
        name: 'Scheduler',
        component: () => import('@/views/scheduler/index.vue'),
        meta: { title: '智能调度中心', icon: 'Cpu', group: 'Agent管理', permission: 'agents:manage' }
      },
      {
        path: 'approvals',
        name: 'Approvals',
        component: () => import('@/views/approvals/index.vue'),
        meta: { title: '审批管理', icon: 'Stamp', group: '系统管理', permission: 'agents:manage' }
      },
      {
        path: 'capabilities',
        name: 'Capabilities',
        component: () => import('@/views/capabilities/index.vue'),
        meta: { title: '能力管理', icon: 'Cpu', group: '项目管理', permission: 'agents:view' }
      },
      {
        path: 'mcp',
        name: 'MCP',
        component: () => import('@/views/mcp/index.vue'),
        meta: { title: 'MCP管理', icon: 'Connection', group: '集成配置', permission: 'agents:manage' }
      },
      {
        path: 'context-health',
        name: 'ContextHealth',
        component: () => import('@/views/context-health/index.vue'),
        meta: { title: '上下文监控', icon: 'Monitor', group: 'Agent管理', permission: 'agents:view' }
      },

      // ===== 项目管理 =====
      {
        path: 'projects',
        name: 'Projects',
        component: () => import('@/views/projects/index.vue'),
        meta: { title: '项目列表', icon: 'Folder', group: '项目管理', permission: 'projects:view' }
      },
      {
        path: 'projects/:projectId/supervision',
        name: 'SupervisionReport',
        component: () => import('@/views/projects/supervision.vue'),
        meta: { title: '督查报告', hidden: true }
      },
      {
        path: 'game-templates',
        name: 'GameTemplates',
        component: () => import('@/views/game-templates/index.vue'),
        meta: { title: '游戏模板', icon: 'Grid', group: '项目管理' }
      },
      {
        path: 'knowledge-base',
        name: 'KnowledgeBase',
        component: () => import('@/views/knowledge-base/index.vue'),
        meta: { title: '知识库', icon: 'Collection', group: '项目管理' }
      },
      {
        path: 'knowledge-evolution',
        name: 'KnowledgeEvolution',
        component: () => import('@/views/knowledge-evolution/index.vue'),
        meta: { title: '知识进化', icon: 'MagicStick', group: '项目管理', permission: 'projects:manage' }
      },
      {
        path: 'knowledge-graph',
        name: 'KnowledgeGraph',
        component: () => import('@/views/knowledge-graph/index.vue'),
        meta: { title: '知识图谱', icon: 'Share', group: '项目管理', permission: 'knowledge:graph' }
      },
      {
        path: 'quality-prediction',
        name: 'QualityPrediction',
        component: () => import('@/views/quality-prediction/index.vue'),
        meta: { title: '质量预测', icon: 'TrendCharts', group: '项目管理', permission: 'quality:view' }
      },
      {
        path: 'iteration-adapt',
        name: 'IterationAdapt',
        component: () => import('@/views/iteration-adapt/index.vue'),
        meta: { title: '迭代适应', icon: 'MagicStick', group: '项目管理', permission: 'iteration:view' }
      },
      {
        path: 'multi-turn',
        name: 'MultiTurn',
        component: () => import('@/views/multi-turn/index.vue'),
        meta: { title: '多轮推理', icon: 'Connection', group: 'Agent管理', permission: 'reasoning:view' }
      },
      {
        path: 'code/:projectId',
        name: 'CodeBrowser',
        component: () => import('@/views/code/index.vue'),
        meta: { title: '代码浏览', hidden: true }
      },
      {
        path: 'code/:projectId/view',
        name: 'CodeViewer',
        component: () => import('@/views/code/viewer.vue'),
        meta: { title: '代码查看', hidden: true }
      },
      {
        path: 'reviews',
        name: 'Reviews',
        component: () => import('@/views/reviews/index.vue'),
        meta: { title: '代码审查', icon: 'Document', group: '项目管理', permission: 'code:review' }
      },
      {
        path: 'reviews/:id',
        name: 'ReviewDetail',
        component: () => import('@/views/reviews/detail.vue'),
        meta: { title: '审查详情', hidden: true }
      },
      {
        path: 'git',
        name: 'Git',
        component: () => import('@/views/git/index.vue'),
        meta: { title: 'Git仓库', icon: 'Connection', group: '项目管理', permission: 'projects:view' }
      },
      {
        path: 'git/:id',
        name: 'GitDetail',
        component: () => import('@/views/git/detail.vue'),
        meta: { title: '仓库详情', hidden: true }
      },
      {
        path: 'pipeline',
        name: 'Pipeline',
        component: () => import('@/views/pipeline/index.vue'),
        meta: { title: 'CICD流水线', icon: 'SetUp', group: '项目管理', permission: 'pipeline:view' }
      },
      {
        path: 'pipeline/create',
        name: 'PipelineCreate',
        component: () => import('@/views/pipeline/create.vue'),
        meta: { title: '创建流水线', hidden: true }
      },
      {
        path: 'pipeline/:id',
        name: 'PipelineDetail',
        component: () => import('@/views/pipeline/detail.vue'),
        meta: { title: '流水线详情', hidden: true }
      },
      {
        path: 'workflow',
        name: 'Workflow',
        component: () => import('@/views/workflow/index.vue'),
        meta: { title: '工作流', icon: 'Share', group: '项目管理', permission: 'workflow:view' }
      },
      {
        path: 'code-quality/:projectId',
        name: 'CodeQuality',
        component: () => import('@/views/code-quality/index.vue'),
        meta: { title: '代码质量', hidden: true }
      },

      // ===== 运维中心 =====
      {
        path: 'monitoring',
        name: 'Monitoring',
        component: () => import('@/views/monitoring/index.vue'),
        meta: { title: '监控中心', icon: 'Monitor', group: '运维中心', permission: 'system:monitor' }
      },
      {
        path: 'alerts',
        name: 'Alerts',
        component: () => import('@/views/alerts/index.vue'),
        meta: { title: '告警中心', icon: 'Bell', group: '运维中心', permission: 'system:monitor' }
      },
      {
        path: 'resources',
        name: 'Resources',
        component: () => import('@/views/resources/index.vue'),
        meta: { title: '资源用量', icon: 'Coin', group: '运维中心', permission: 'system:monitor' }
      },
      {
        path: 'diagnostic',
        name: 'Diagnostic',
        component: () => import('@/views/diagnostic/index.vue'),
        meta: { title: '系统自检', icon: 'FirstAidKit', group: '运维中心', permission: 'system:monitor' }
      },

      // ===== 通知中心 =====
      {
        path: 'notifications',
        name: 'Notifications',
        component: () => import('@/views/notifications/index.vue'),
        meta: { title: '我的通知', icon: 'Message', group: '通知中心' }
      },
      {
        path: 'notification-templates',
        name: 'NotificationTemplates',
        component: () => import('@/views/notification-templates/index.vue'),
        meta: { title: '通知模板', icon: 'Files', group: '通知中心', permission: 'notification:manage' }
      },
      {
        path: 'notification-templates/create',
        name: 'TemplateCreate',
        component: () => import('@/views/notification-templates/form.vue'),
        meta: { title: '创建模板', hidden: true }
      },
      {
        path: 'notification-templates/:id',
        name: 'TemplateEdit',
        component: () => import('@/views/notification-templates/form.vue'),
        meta: { title: '编辑模板', hidden: true }
      },
      {
        path: 'notification-preferences',
        name: 'NotificationPreferences',
        component: () => import('@/views/notification-preferences/index.vue'),
        meta: { title: '通知偏好', icon: 'Bell', group: '通知中心' }
      },

      // ===== 团队管理 =====
      {
        path: 'admin/team',
        name: 'TeamManagement',
        meta: { title: '团队管理', icon: 'User', group: '系统管理' },
        children: [
          {
            path: 'users',
            name: 'Users',
            component: () => import('@/views/admin/users.vue'),
            meta: { title: '用户管理', permission: 'users:manage' }
          },
          {
            path: 'roles',
            name: 'Roles',
            component: () => import('@/views/admin/roles.vue'),
            meta: { title: '角色管理', permission: 'roles:manage' }
          },
          {
            path: 'pending',
            name: 'PendingUsers',
            component: () => import('@/views/admin/pending.vue'),
            meta: { title: '待审批用户', permission: 'users:manage' }
          },
          {
            path: 'permissions',
            name: 'Permissions',
            component: () => import('@/views/permissions/index.vue'),
            meta: { title: '权限管理', permission: 'admin:manage' }
          }
        ]
      },

      // ===== 运维工具 =====
      {
        path: 'admin/ops',
        name: 'OpsTools',
        meta: { title: '运维工具', icon: 'Tools', group: '系统管理' },
        children: [
          {
            path: 'logs',
            name: 'Logs',
            component: () => import('@/views/admin/logs.vue'),
            meta: { title: '操作日志', permission: 'logs:view' }
          },
          {
            path: 'logs/:id',
            name: 'LogDetail',
            component: () => import('@/views/admin/log-detail.vue'),
            meta: { title: '日志详情', hidden: true }
          },
          {
            path: 'agent-logs',
            name: 'AgentLogs',
            component: () => import('@/views/agent-logs/index.vue'),
            meta: { title: 'Agent日志', permission: 'logs:view' }
          },
          {
            path: 'terminal',
            name: 'Terminal',
            component: () => import('@/views/terminal/index.vue'),
            meta: { title: '系统终端', permission: 'system:monitor' }
          }
        ]
      },

      // ===== 系统管理 =====
      {
        path: 'admin/system',
        name: 'SystemSettings',
        meta: { title: '系统管理', icon: 'Setting', group: '系统管理' },
        children: [
          {
            path: 'configs',
            name: 'Configs',
            component: () => import('@/views/admin/configs.vue'),
            meta: { title: '配置中心' }
          },
          {
            path: 'settings',
            name: 'Settings',
            component: () => import('@/views/admin/settings.vue'),
            meta: { title: '系统设置' }
          },
          {
            path: 'constants',
            name: 'Constants',
            component: () => import('@/views/constants/index.vue'),
            meta: { title: '系统常量', permission: 'system:view' }
          },
          {
            path: 'files',
            name: 'Files',
            component: () => import('@/views/files/index.vue'),
            meta: { title: '文件管理', permission: 'agents:view' }
          },
          {
            path: 'api-docs',
            name: 'ApiDocs',
            component: () => import('@/views/api-docs/index.vue'),
            meta: { title: 'API文档' }
          }
        ]
      },

      // ===== 集成配置 =====
      {
        path: 'admin/integration',
        name: 'Integration',
        meta: { title: '集成配置', icon: 'Connection', group: '系统管理' },
        children: [
          {
            path: 'skills',
            name: 'Skills',
            component: () => import('@/views/skills/index.vue'),
            meta: { title: '技能管理', permission: 'skills:view' }
          },
          {
            path: 'skills/:id',
            name: 'SkillDetail',
            component: () => import('@/views/skills/detail.vue'),
            meta: { title: '技能详情', hidden: true }
          },
          {
            path: 'tokens',
            name: 'Tokens',
            component: () => import('@/views/tokens/index.vue'),
            meta: { title: 'Token管理' }
          },
          {
            path: 'tokens/create',
            name: 'TokenCreate',
            component: () => import('@/views/tokens/form.vue'),
            meta: { title: '创建Token', hidden: true }
          },
          {
            path: 'devices',
            name: 'Devices',
            component: () => import('@/views/devices/index.vue'),
            meta: { title: '设备信任' }
          },
          {
            path: 'dingtalk',
            name: 'DingTalk',
            component: () => import('@/views/dingtalk/index.vue'),
            meta: { title: '钉钉配置' }
          },
          {
            path: 'feishu',
            name: 'Feishu',
            component: () => import('@/views/feishu/index.vue'),
            meta: { title: '飞书配置' }
          }
        ]
      },

      // ===== 用户中心（隐藏菜单） =====
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人资料', hidden: true }
      },
      {
        path: 'profile/password',
        name: 'ChangePassword',
        component: () => import('@/views/profile/password.vue'),
        meta: { title: '修改密码', hidden: true }
      }
    ]
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login.vue'),
    meta: { title: '登录', hidden: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/register.vue'),
    meta: { title: '注册', hidden: true }
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('@/views/forgot-password.vue'),
    meta: { title: '忘记密码', hidden: true }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/404.vue'),
    meta: { title: '404', hidden: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - ChengXun Game Maker` : 'ChengXun Game Maker'

  // 检查系统是否已安装（首次访问时）
  const installStatus = sessionStorage.getItem('system_installed')
  if (installStatus === null && to.path !== '/install') {
    try {
      const resp = await fetch('/api/install/status')
      const data = await resp.json()
      sessionStorage.setItem('system_installed', data.installed ? 'true' : 'false')
      if (!data.installed) {
        next('/install')
        return
      }
    } catch (e) {
      // 接口异常时放行
    }
  } else if (installStatus === 'false' && to.path !== '/install') {
    next('/install')
    return
  }

  // 检查是否需要登录
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && to.path !== '/register' && to.path !== '/forgot-password' && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
