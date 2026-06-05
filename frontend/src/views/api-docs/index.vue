<template>
  <div class="api-docs-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>API文档</span>
          <el-button type="primary" @click="openSwagger">
            <el-icon><Link /></el-icon> 打开Swagger UI
          </el-button>
        </div>
      </template>

      <div class="api-info">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="API基础路径">/api</el-descriptions-item>
          <el-descriptions-item label="认证方式">JWT Token</el-descriptions-item>
          <el-descriptions-item label="Swagger UI">
            <el-link type="primary" @click="openSwagger">/swagger-ui.html</el-link>
          </el-descriptions-item>
          <el-descriptions-item label="OpenAPI文档">
            <el-link type="primary" @click="openApiDocs">/v3/api-docs</el-link>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <el-divider />

      <h3>常用API列表</h3>
      <el-table :data="apiList" stripe>
        <el-table-column prop="module" label="模块" width="150" />
        <el-table-column prop="path" label="路径" min-width="250">
          <template #default="{ row }">
            <code>{{ row.path }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="method" label="方法" width="100">
          <template #default="{ row }">
            <el-tag :type="getMethodType(row.method)" size="small">{{ row.method }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column prop="permission" label="权限" width="150" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
/**
 * API文档页面
 * 查看系统API文档
 *
 * 权限要求：登录用户
 */
import { ref } from 'vue'

const apiList = ref([
  { module: '认证', path: '/api/v1/auth/login', method: 'POST', description: '用户登录', permission: '公开' },
  { module: '认证', path: '/api/v1/auth/me', method: 'GET', description: '获取当前用户', permission: '登录' },
  { module: 'Agent', path: '/api/agents/project/{projectId}', method: 'GET', description: '获取项目Agent列表', permission: 'agents:view' },
  { module: 'Agent', path: '/api/agents/project/{id}/{role}/task', method: 'POST', description: '发送任务', permission: 'agents:task' },
  { module: '项目', path: '/api/projects', method: 'GET', description: '获取项目列表', permission: 'projects:view' },
  { module: '项目', path: '/api/projects', method: 'POST', description: '创建项目', permission: 'projects:manage' },
  { module: '技能', path: '/api/skills', method: 'GET', description: '获取技能列表', permission: 'skills:view' },
  { module: '能力', path: '/api/capabilities', method: 'GET', description: '获取能力列表', permission: 'agents:view' },
  { module: '通知', path: '/api/notifications', method: 'GET', description: '获取通知列表', permission: '登录' },
  { module: '日志', path: '/api/logs', method: 'GET', description: '获取操作日志', permission: 'logs:view' },
  { module: '流水线', path: '/api/pipelines/list', method: 'GET', description: '获取流水线列表', permission: 'pipeline:view' },
  { module: 'Git', path: '/api/git/project/{id}', method: 'GET', description: '获取Git仓库', permission: 'projects:view' },
  { module: '监控', path: '/api/monitoring/overview', method: 'GET', description: '获取监控概览', permission: 'system:monitor' },
  { module: '告警', path: '/api/alerts/statistics', method: 'GET', description: '获取告警统计', permission: 'system:monitor' },
  { module: 'AI助手', path: '/api/ai-assistant/ask', method: 'POST', description: 'AI问答', permission: 'ai:use' },
  { module: '知识库', path: '/api/knowledge-base/stats', method: 'GET', description: '获取知识库统计', permission: '登录' },
  { module: 'Token', path: '/api/tokens', method: 'GET', description: '获取Token列表', permission: 'tokens:view' },
  { module: '用户', path: '/api/admin/users', method: 'GET', description: '获取用户列表', permission: 'users:manage' },
  { module: '角色', path: '/api/admin/roles', method: 'GET', description: '获取角色列表', permission: 'roles:manage' },
])

const getMethodType = (method) => {
  const map = { GET: 'success', POST: 'primary', PUT: 'warning', DELETE: 'danger' }
  return map[method] || 'info'
}

const openSwagger = () => {
  window.open('/swagger-ui.html', '_blank')
}

const openApiDocs = () => {
  window.open('/v3/api-docs', '_blank')
}
</script>

<style scoped>
.api-docs-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.api-info {
  margin-bottom: 20px;
}

h3 {
  margin-bottom: 16px;
  color: var(--el-text-color-primary);
}

code {
  background: var(--el-fill-color-light);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
  color: var(--el-text-color-primary);
}

:deep(.el-descriptions__label) {
  color: var(--el-text-color-primary);
}

:deep(.el-descriptions__content) {
  color: var(--el-text-color-regular);
}

:deep(.el-table) {
  color: var(--el-text-color-primary);
}

:deep(.el-table th) {
  color: var(--el-text-color-primary);
  background-color: var(--el-fill-color-light);
}

:deep(.el-table td) {
  color: var(--el-text-color-regular);
}

:deep(.el-link) {
  color: var(--el-color-primary);
}
</style>
