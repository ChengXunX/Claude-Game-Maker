/**
 * 权限工具模块
 * 提供权限检查的工具函数和指令
 *
 * 权限分级：
 * - 系统级：需要管理员或特定角色权限（如 users:manage, roles:manage）
 * - 项目级：需要项目相关权限（如 projects:view, projects:edit）
 * - 用户级：用户自己的数据（如个人资料、修改密码）
 *
 * @author chengxun
 * @since 1.0.0
 */

import { useUserStore } from '@/stores/user'

/**
 * 检查是否拥有指定权限
 * @param {string} permission - 权限标识，如 'agents:manage'
 * @returns {boolean} 是否拥有权限
 */
export function hasPermission(permission) {
  const userStore = useUserStore()
  return userStore.hasPermission(permission)
}

/**
 * 检查是否拥有任意一个权限
 * @param {string[]} permissions - 权限标识数组
 * @returns {boolean} 是否拥有任意一个权限
 */
export function hasAnyPermission(permissions) {
  const userStore = useUserStore()
  return permissions.some(p => userStore.hasPermission(p))
}

/**
 * 检查是否拥有所有权限
 * @param {string[]} permissions - 权限标识数组
 * @returns {boolean} 是否拥有所有权限
 */
export function hasAllPermissions(permissions) {
  const userStore = useUserStore()
  return permissions.every(p => userStore.hasPermission(p))
}

/**
 * 检查是否为管理员
 * @returns {boolean} 是否为管理员
 */
export function isAdmin() {
  const userStore = useUserStore()
  return userStore.isAdmin()
}

/**
 * 权限指令 - v-permission
 * 用法：v-permission="'agents:manage'" 或 v-permission="['agents:manage', 'agents:view']"
 */
export const permissionDirective = {
  mounted(el, binding) {
    const { value } = binding
    const userStore = useUserStore()

    if (value) {
      let hasPerm = false
      if (Array.isArray(value)) {
        hasPerm = value.some(p => userStore.hasPermission(p))
      } else {
        hasPerm = userStore.hasPermission(value)
      }

      if (!hasPerm) {
        el.parentNode?.removeChild(el)
      }
    }
  }
}

/**
 * 权限常量定义
 * 与后端 RoleService 中的 PERM_* 常量对应
 */
export const PERMISSIONS = {
  // 仪表盘
  DASHBOARD_VIEW: 'dashboard:view',

  // Agent 管理
  AGENTS_VIEW: 'agents:view',
  AGENTS_MANAGE: 'agents:manage',
  AGENTS_TASK: 'agents:task',

  // 项目管理
  PROJECTS_VIEW: 'projects:view',
  PROJECTS_MANAGE: 'projects:manage',
  PROJECTS_EDIT: 'projects:edit',

  // 技能管理
  SKILLS_VIEW: 'skills:view',
  SKILLS_MANAGE: 'skills:manage',

  // 用户管理
  USERS_VIEW: 'users:view',
  USERS_MANAGE: 'users:manage',

  // 角色管理
  ROLES_MANAGE: 'roles:manage',

  // 日志
  LOGS_VIEW: 'logs:view',

  // CICD 流水线
  PIPELINE_VIEW: 'pipeline:view',
  PIPELINE_CREATE: 'pipeline:create',
  PIPELINE_MANAGE: 'pipeline:manage',
  PIPELINE_EXECUTE: 'pipeline:execute',
  PIPELINE_APPROVE: 'pipeline:approve',
  PIPELINE_INTERVENE: 'pipeline:intervene',

  // 监控
  MONITOR_VIEW: 'system:monitor',
  MONITOR_MANAGE: 'system:monitor:manage',

  // 工作流
  WORKFLOW_VIEW: 'workflow:view',
  WORKFLOW_MANAGE: 'workflow:manage',

  // 代码审查
  CODE_REVIEW: 'code:review',

  // 通知管理
  NOTIFICATION_MANAGE: 'notification:manage',

  // AI 助手
  AI_USE: 'ai:use',

  // 终端
  TERMINAL_USE: 'terminal:use',

  // 能力管理（新增）
  CAPABILITIES_VIEW: 'capabilities:view',
  CAPABILITIES_MANAGE: 'capabilities:manage',

  // MCP管理（新增）
  MCP_VIEW: 'mcp:view',
  MCP_MANAGE: 'mcp:manage',

  // 文件管理（新增）
  FILES_VIEW: 'files:view',
  FILES_MANAGE: 'files:manage',

  // 系统常量（新增）
  CONSTANTS_VIEW: 'constants:view',
  CONSTANTS_MANAGE: 'constants:manage',

  // 权限管理（新增）
  PERMISSIONS_VIEW: 'permissions:view',
  PERMISSIONS_MANAGE: 'permissions:manage',

  // 上下文监控（新增）
  CONTEXT_MONITOR: 'context:monitor',

  // API文档（新增）
  API_VIEW: 'api:view',

  // 通知偏好（新增）
  NOTIFICATION_PREFERENCES: 'notification:preferences',

  // 系统管理
  SYSTEM_VIEW: 'system:view',
  SYSTEM_MANAGE: 'system:manage',
  ADMIN_MANAGE: 'admin:manage',

  // 知识库
  KNOWLEDGE_MANAGE: 'knowledge:manage',

  // Token管理
  TOKENS_VIEW: 'tokens:view',
  TOKENS_MANAGE: 'tokens:manage',

  // Agent查看
  AGENT_VIEW: 'agent:view',
  AGENT_MANAGE: 'agent:manage',

  // 游戏验证（G8 新增视觉验证权限）
  GAME_VERIFY: 'game:verify',
  GAME_VERIFY_VIEW: 'game:verify:view',
  GAME_VISUAL_VIEW: 'game:visual:view',
  GAME_PREVIEW: 'game:preview',

  // 所有权限
  ALL: '*'
}
