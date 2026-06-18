<template>
  <div class="mcp-page">
    <!-- 模板快速安装 -->
    <el-card class="section-card" v-if="templates.length > 0">
      <template #header>
        <div class="card-header">
          <span>快速安装模板</span>
          <el-input v-model="templateSearch" placeholder="搜索模板..." clearable style="width: 200px;" size="small">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </div>
      </template>
      <el-tabs v-model="activeCategory" type="card">
        <el-tab-pane v-for="cat in visibleCategories" :key="cat.key" :label="cat.label" :name="cat.key">
          <div class="template-grid">
            <el-card v-for="t in getTemplatesByCategory(cat.key)" :key="t.id" shadow="hover" class="template-item" @click="handleInstallTemplate(t)">
              <div class="template-name">{{ t.name }}</div>
              <div class="template-desc">{{ t.description || t.templateKey }}</div>
              <div class="template-tags">
                <el-tag v-if="t.transportType === 'STDIO'" size="small" type="info">STDIO</el-tag>
                <el-tag v-else size="small" type="success">HTTP</el-tag>
                <el-tag v-if="t.authMode === 'header'" size="small" type="warning">Header 认证</el-tag>
                <el-tag v-else-if="t.authMode === 'body'" size="small" type="warning">Body 认证</el-tag>
              </div>
            </el-card>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 服务器列表 -->
    <el-card class="section-card">
      <template #header>
        <div class="card-header">
          <span>MCP 服务器</span>
          <div class="header-actions">
            <el-button @click="loadServers" :loading="loading">
              <el-icon><Refresh /></el-icon> 刷新
            </el-button>
            <el-button type="primary" @click="handleCreate" v-permission="'agents:manage'">
              <el-icon><Plus /></el-icon> 添加服务器
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="servers" v-loading="loading" stripe>
        <el-table-column prop="name" label="名称" width="150" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTransportTag(row.transportType)" size="small">{{ getTransportLabel(row.transportType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="120">
          <template #default="{ row }">
            <span>{{ getCategoryLabel(row.category) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="地址" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.transportType === 'STDIO' ? row.command : row.url }}
          </template>
        </el-table-column>
        <el-table-column label="工具" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.toolCount > 0" size="small" type="success">{{ row.toolCount }}</el-tag>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
              {{ row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="handleEdit(row)" v-permission="'agents:manage'">编辑</el-button>
            <el-button type="success" size="small" text @click="handleDiscoverTools(row)" v-permission="'agents:manage'">发现工具</el-button>
            <el-button type="info" size="small" text @click="handleViewTools(row)">工具列表</el-button>
            <el-button type="warning" size="small" text @click="handleToggle(row)" v-permission="'agents:manage'">
              {{ row.enabled ? '禁用' : '启用' }}
            </el-button>
            <el-button type="danger" size="small" text @click="handleDelete(row)" v-permission="'agents:manage'">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 模板安装配置对话框 -->
    <el-dialog v-model="installDialogVisible" :title="'安装 - ' + (installingTemplate?.name || '')" width="550px" :close-on-click-modal="false">
      <div v-if="installingTemplate" class="install-form">
        <div class="install-desc">{{ installingTemplate.description }}</div>
        <el-divider />
        <!-- STDIO 模式：环境变量输入 -->
        <template v-if="installingTemplate.transportType === 'STDIO' && installingTemplate.env">
          <div v-for="param in parseTemplateParams(installingTemplate)" :key="param.key" class="param-item">
            <label class="param-label">{{ param.label || param.key }}</label>
            <el-input v-model="installParams[param.key]" :placeholder="param.placeholder || ''" :type="param.key.includes('KEY') || param.key.includes('SECRET') || param.key.includes('TOKEN') || param.key.includes('PASSWORD') ? 'password' : 'text'" show-password />
          </div>
        </template>
        <!-- HTTP 模式：根据 requiredParams 生成输入框 -->
        <template v-else-if="installingTemplate.requiredParams">
          <div v-for="param in parseRequiredParams(installingTemplate)" :key="param.key" class="param-item">
            <label class="param-label">{{ param.label || param.key }}</label>
            <el-input v-model="installParams[param.key]" :placeholder="param.placeholder || ''" :type="isSecretParam(param.key) ? 'password' : 'text'" show-password />
          </div>
        </template>
        <div v-else class="no-params">此模板无需额外配置，点击安装即可。</div>
      </div>
      <template #footer>
        <el-button @click="installDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmInstall" :loading="installing">安装</el-button>
      </template>
    </el-dialog>

    <!-- 创建/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑 MCP 服务器' : '添加 MCP 服务器'" width="650px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="服务器名称" prop="name">
          <el-input v-model="form.name" placeholder="如：image-generator" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" placeholder="服务器用途描述" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" placeholder="选择分类" clearable>
            <el-option v-for="cat in categories" :key="cat.key" :label="cat.icon + ' ' + cat.label" :value="cat.key" />
          </el-select>
        </el-form-item>
        <el-form-item label="传输方式" prop="transportType">
          <el-radio-group v-model="form.transportType">
            <el-radio value="STDIO">STDIO（本地进程）</el-radio>
            <el-radio value="SSE">SSE（HTTP 长连接）</el-radio>
            <el-radio value="STREAMABLE_HTTP">HTTP（标准请求）</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- STDIO 模式 -->
        <template v-if="form.transportType === 'STDIO'">
          <el-form-item label="命令" prop="command">
            <el-input v-model="form.command" placeholder="如：npx, node, python" />
          </el-form-item>
          <el-form-item label="参数">
            <el-input v-model="form.argsText" type="textarea" :rows="2" placeholder='JSON 数组格式，如：["-y", "@modelcontextprotocol/server-filesystem"]' />
          </el-form-item>
          <el-form-item label="环境变量">
            <el-input v-model="form.envText" type="textarea" :rows="2" placeholder='JSON 格式，如：{"API_KEY": "sk-xxx"}' />
          </el-form-item>
        </template>

        <!-- SSE/HTTP 模式 -->
        <template v-if="form.transportType === 'SSE' || form.transportType === 'STREAMABLE_HTTP'">
          <el-form-item label="URL" prop="url">
            <el-input v-model="form.url" placeholder="如：https://api.example.com/mcp" />
          </el-form-item>
          <el-form-item label="认证模式">
            <el-radio-group v-model="form.authMode">
              <el-radio value="header">请求头认证</el-radio>
              <el-radio value="body">请求体认证</el-radio>
              <el-radio value="env">无特殊认证</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="form.authMode === 'header'" label="认证头名">
            <el-input v-model="form.authHeaderName" placeholder="Authorization" />
          </el-form-item>
          <el-form-item label="请求头">
            <el-input v-model="form.headersText" type="textarea" :rows="2" placeholder='JSON 格式，如：{"Authorization": "Bearer sk-xxx"}' />
          </el-form-item>
        </template>

        <el-form-item label="绑定项目">
          <el-input v-model="form.projectId" placeholder="可选，留空表示全局" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">{{ isEdit ? '保存' : '添加' }}</el-button>
      </template>
    </el-dialog>

    <!-- 工具列表对话框 -->
    <el-dialog v-model="toolsDialogVisible" :title="toolsDialogTitle + ' - 工具列表'" width="900px">
      <el-table :data="currentTools" stripe>
        <el-table-column prop="toolName" label="工具名" width="160" />
        <el-table-column prop="displayName" label="显示名" width="100" />
        <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
        <el-table-column label="已预配参数" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.defaultParams" style="font-size:12px;color:#67c23a;">{{ truncateText(row.defaultParams, 60) }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="参数 Schema" width="100">
          <template #default="{ row }">
            <el-button v-if="row.inputSchema" size="small" text @click="showToolSchema(row)">查看</el-button>
            <span v-else class="text-muted">无</span>
          </template>
        </el-table-column>
        <el-table-column label="默认参数" width="100">
          <template #default="{ row }">
            <el-button size="small" text @click="editDefaultParams(row)">配置</el-button>
          </template>
        </el-table-column>
        <el-table-column label="AI 提示" width="100">
          <template #default="{ row }">
            <el-button size="small" text @click="editParamHints(row)">编辑</el-button>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="70">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" size="small" @change="handleToggleTool(row)" />
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="currentTools.length === 0" description="暂无工具，请先点击「发现工具」" />
    </el-dialog>

    <!-- 参数 Schema 对话框 -->
    <el-dialog v-model="schemaDialogVisible" title="工具参数 Schema" width="500px">
      <pre class="schema-pre">{{ currentSchema }}</pre>
    </el-dialog>

    <!-- 默认参数编辑对话框 -->
    <el-dialog v-model="paramsDialogVisible" :title="'配置默认参数 - ' + editingToolName" width="550px">
      <div class="params-tip">
        <p>默认参数在 Agent 调用工具时自动注入，Agent 传入的同名参数会覆盖默认值。</p>
        <p>常见参数：<code>apiKey</code>、<code>model</code>、<code>api_url</code></p>
      </div>
      <el-input v-model="editingParams" type="textarea" :rows="8" placeholder='JSON 格式，如：{"model": "image-01", "width": 1024}' />
      <template #footer>
        <el-button @click="paramsDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDefaultParams">保存</el-button>
      </template>
    </el-dialog>

    <!-- AI 参数提示编辑对话框 -->
    <el-dialog v-model="hintsDialogVisible" :title="'AI 填写指导 - ' + editingToolName" width="600px">
      <div class="params-tip">
        <p>为 AI 提供每个参数的填写指导，帮助 AI 理解如何生成正确的参数值。</p>
        <p>格式：JSON 对象，key 为参数名，value 为填写说明和示例。</p>
      </div>
      <el-input v-model="editingHints" type="textarea" :rows="10" placeholder='JSON 格式，如：
{
  "prompt": "描述要生成的内容，如：a cute cartoon cat playing with yarn",
  "style": "风格描述，如：digital art, watercolor, realistic"
}' />
      <template #footer>
        <el-button @click="hintsDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveParamHints">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import api from '@/api'

const loading = ref(false)
const servers = ref([])
const templates = ref([])
const templateSearch = ref('')
const activeCategory = ref('resource-image')

// 分类定义
const categories = [
  { key: 'resource-image', label: '图片生成', icon: '🎨' },
  { key: 'resource-audio', label: '音频生成', icon: '🎵' },
  { key: 'resource-video', label: '视频生成', icon: '🎬' },
  { key: 'resource-3d', label: '3D 生成', icon: '🧊' },
  { key: 'gamedev', label: '游戏开发', icon: '🎮' },
  { key: 'dev', label: '开发工具', icon: '🔧' },
  { key: 'data', label: '数据库', icon: '💾' },
  { key: 'collaboration', label: '协作', icon: '🤝' },
  { key: 'monitoring', label: '监控', icon: '📊' }
]

const getCategoryLabel = (cat) => {
  const found = categories.find(c => c.key === cat)
  return found ? found.icon + ' ' + found.label : cat || '未分类'
}

// 按搜索过滤后的可见分类
const visibleCategories = computed(() => {
  return categories.filter(cat => {
    const catTemplates = getTemplatesByCategory(cat.key)
    return catTemplates.length > 0
  })
})

const getTemplatesByCategory = (cat) => {
  let list = templates.value.filter(t => (t.category || 'other') === cat)
  if (templateSearch.value) {
    const q = templateSearch.value.toLowerCase()
    list = list.filter(t => t.name.toLowerCase().includes(q) || (t.description || '').toLowerCase().includes(q))
  }
  return list
}

// 安装对话框
const installDialogVisible = ref(false)
const installingTemplate = ref(null)
const installParams = ref({})
const installing = ref(false)

// 编辑对话框
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const submitting = ref(false)
const formRef = ref(null)

const toolsDialogVisible = ref(false)
const toolsDialogTitle = ref('')
const currentTools = ref([])
const currentServerId = ref(null)

const schemaDialogVisible = ref(false)
const currentSchema = ref('')

const paramsDialogVisible = ref(false)
const editingToolId = ref(null)
const editingToolName = ref('')
const editingParams = ref('')

const hintsDialogVisible = ref(false)
const editingHints = ref('')

const form = ref({
  name: '', description: '', transportType: 'STDIO',
  command: '', argsText: '', envText: '',
  url: '', headersText: '', projectId: '',
  authMode: 'header', authHeaderName: 'Authorization',
  category: ''
})

const rules = {
  name: [{ required: true, message: '请输入服务器名称', trigger: 'blur' }],
  transportType: [{ required: true, message: '请选择传输方式', trigger: 'change' }],
  command: [{ required: true, message: '请输入命令', trigger: 'blur' }],
  url: [{ required: true, message: '请输入 URL', trigger: 'blur' }]
}

const getTransportLabel = (t) => ({ STDIO: 'STDIO', SSE: 'SSE', STREAMABLE_HTTP: 'HTTP' }[t] || t)
const getTransportTag = (t) => ({ STDIO: '', SSE: 'warning', STREAMABLE_HTTP: 'success' }[t] || 'info')

const isSecretParam = (key) => /key|secret|token|password/i.test(key)

const truncateText = (text, max) => {
  if (!text) return ''
  return text.length > max ? text.substring(0, max) + '...' : text
}

/** 从模板 env JSON 中解析出 ${VAR} 占位符对应的参数 */
const parseTemplateParams = (template) => {
  const params = []
  const envStr = template.env || ''
  const regex = /\$\{(\w+)\}/g
  let match
  while ((match = regex.exec(envStr)) !== null) {
    const key = match[1]
    params.push({ key, label: key.replace(/_/g, ' '), placeholder: '' })
  }
  return params
}

/** 解析 requiredParams JSON */
const parseRequiredParams = (template) => {
  if (!template.requiredParams) return []
  try {
    return JSON.parse(template.requiredParams)
  } catch { return [] }
}

onMounted(() => {
  loadServers()
  loadTemplates()
})

const loadServers = async () => {
  loading.value = true
  try {
    servers.value = (await api.get('/mcp/api/servers')) || []
  } catch { ElMessage.error('加载失败') } finally { loading.value = false }
}

const loadTemplates = async () => {
  try {
    templates.value = (await api.get('/mcp/api/templates')) || []
  } catch { /* ignore */ }
}

/** 点击模板卡片 → 弹出安装配置对话框 */
const handleInstallTemplate = (template) => {
  installingTemplate.value = template
  installParams.value = {}

  // 预填默认值
  const params = template.transportType === 'STDIO' ? parseTemplateParams(template) : parseRequiredParams(template)
  for (const p of params) {
    if (p.defaultValue) {
      installParams.value[p.key] = p.defaultValue
    }
  }

  installDialogVisible.value = true
}

/** 确认安装 */
const confirmInstall = async () => {
  installing.value = true
  try {
    await api.post('/mcp/api/servers/install', {
      templateKey: installingTemplate.value.templateKey,
      projectId: null,
      envVars: installParams.value
    })
    ElMessage.success('安装成功')
    installDialogVisible.value = false
    loadServers()
  } catch { ElMessage.error('安装失败') } finally { installing.value = false }
}

const handleCreate = () => {
  isEdit.value = false
  editingId.value = null
  form.value = { name: '', description: '', transportType: 'STDIO', command: '', argsText: '', envText: '', url: '', headersText: '', projectId: '', authMode: 'header', authHeaderName: 'Authorization', category: '' }
  dialogVisible.value = true
}

const handleEdit = (server) => {
  isEdit.value = true
  editingId.value = server.id
  form.value = {
    name: server.name || '',
    description: server.description || '',
    transportType: server.transportType || 'STDIO',
    command: server.command || '',
    argsText: server.args || '',
    envText: server.env || '',
    url: server.url || '',
    headersText: server.headers || '',
    projectId: server.projectId || '',
    authMode: server.authMode || 'header',
    authHeaderName: server.authHeaderName || 'Authorization',
    category: server.category || ''
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try { await formRef.value.validate() } catch { return }
  submitting.value = true
  try {
    const data = {
      name: form.value.name,
      description: form.value.description,
      transportType: form.value.transportType,
      command: form.value.command,
      args: form.value.argsText || null,
      env: form.value.envText || null,
      url: form.value.url || null,
      headers: form.value.headersText || null,
      projectId: form.value.projectId || null,
      authMode: form.value.authMode || 'env',
      authHeaderName: form.value.authHeaderName || 'Authorization',
      category: form.value.category || null,
      enabled: true
    }
    if (isEdit.value) {
      await api.put(`/mcp/api/servers/${editingId.value}`, data)
      ElMessage.success('已保存')
    } else {
      await api.post('/mcp/api/servers', data)
      ElMessage.success('已添加')
    }
    dialogVisible.value = false
    loadServers()
  } catch { ElMessage.error('操作失败') } finally { submitting.value = false }
}

const handleToggle = async (server) => {
  try {
    await api.post(`/mcp/api/servers/${server.id}/toggle`)
    ElMessage.success('状态已切换')
    loadServers()
  } catch { ElMessage.error('操作失败') }
}

const handleDiscoverTools = async (server) => {
  try {
    const result = await api.post(`/mcp/api/servers/${server.id}/test`)
    if (result.connected) {
      ElMessage.success(`连接成功，发现 ${result.toolCount || 0} 个工具`)
    } else {
      ElMessage.warning(result.lastTestResult || '连接失败')
    }
    loadServers()
  } catch { ElMessage.error('测试失败') }
}

const handleViewTools = async (server) => {
  currentServerId.value = server.id
  toolsDialogTitle.value = server.name
  try {
    currentTools.value = (await api.get(`/mcp/api/servers/${server.id}/tools`)) || []
    toolsDialogVisible.value = true
  } catch { ElMessage.error('加载工具列表失败') }
}

const handleToggleTool = async (tool) => {
  try {
    await api.post(`/mcp/api/tools/${tool.id}/toggle`)
  } catch { ElMessage.error('切换失败') }
}

const showToolSchema = (tool) => {
  try {
    currentSchema.value = JSON.stringify(JSON.parse(tool.inputSchema), null, 2)
  } catch {
    currentSchema.value = tool.inputSchema
  }
  schemaDialogVisible.value = true
}

const editDefaultParams = (tool) => {
  editingToolId.value = tool.id
  editingToolName.value = tool.toolName
  try {
    editingParams.value = tool.defaultParams ? JSON.stringify(JSON.parse(tool.defaultParams), null, 2) : ''
  } catch {
    editingParams.value = tool.defaultParams || ''
  }
  paramsDialogVisible.value = true
}

const saveDefaultParams = async () => {
  try {
    if (editingParams.value.trim()) {
      JSON.parse(editingParams.value)
    }
    await api.put(`/mcp/api/tools/${editingToolId.value}`, { defaultParams: editingParams.value || null })
    ElMessage.success('已保存')
    paramsDialogVisible.value = false
    if (currentServerId.value) {
      currentTools.value = (await api.get(`/mcp/api/servers/${currentServerId.value}/tools`)) || []
    }
  } catch (e) {
    if (e instanceof SyntaxError) {
      ElMessage.error('JSON 格式错误')
    } else {
      ElMessage.error('保存失败')
    }
  }
}

const editParamHints = (tool) => {
  editingToolId.value = tool.id
  editingToolName.value = tool.toolName
  try {
    editingHints.value = tool.paramHints ? JSON.stringify(JSON.parse(tool.paramHints), null, 2) : ''
  } catch {
    editingHints.value = tool.paramHints || ''
  }
  hintsDialogVisible.value = true
}

const saveParamHints = async () => {
  try {
    if (editingHints.value.trim()) {
      JSON.parse(editingHints.value)
    }
    await api.put(`/mcp/api/tools/${editingToolId.value}`, { paramHints: editingHints.value || null })
    ElMessage.success('已保存')
    hintsDialogVisible.value = false
    if (currentServerId.value) {
      currentTools.value = (await api.get(`/mcp/api/servers/${currentServerId.value}/tools`)) || []
    }
  } catch (e) {
    if (e instanceof SyntaxError) {
      ElMessage.error('JSON 格式错误')
    } else {
      ElMessage.error('保存失败')
    }
  }
}

const handleDelete = async (server) => {
  try {
    await ElMessageBox.confirm(`确定删除 "${server.name}"？`, '删除确认')
    await api.delete(`/mcp/api/servers/${server.id}`)
    ElMessage.success('已删除')
    loadServers()
  } catch { /* cancelled */ }
}
</script>

<style scoped>
.mcp-page { padding: 20px; background: var(--el-bg-color-page); min-height: calc(100vh - 60px); }
.section-card { margin-bottom: 16px; border-radius: 8px; }
.section-card :deep(.el-card__header) { padding: 16px 20px; border-bottom: 1px solid var(--el-border-color-lighter); }
.card-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; }
.card-header span { font-size: 16px; font-weight: 600; color: var(--el-text-color-primary); }
.header-actions { display: flex; gap: 8px; }
.template-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 12px; }
.template-item { cursor: pointer; transition: all 0.2s; border: 1px solid var(--el-border-color-lighter); border-radius: 8px; }
.template-item:hover { border-color: var(--el-color-primary); box-shadow: 0 2px 12px rgba(0,0,0,0.08); transform: translateY(-2px); }
.template-name { font-weight: 600; margin-bottom: 6px; color: var(--el-text-color-primary); font-size: 14px; }
.template-desc { font-size: 12px; color: var(--el-text-color-secondary); margin-bottom: 10px; line-height: 1.5; min-height: 36px; }
.template-tags { display: flex; gap: 6px; flex-wrap: wrap; }
.text-muted { color: var(--el-text-color-secondary); }

/* 安装对话框 */
.install-form {}
.install-desc { color: var(--el-text-color-secondary); font-size: 14px; margin-bottom: 8px; }
.param-item { margin-bottom: 16px; }
.param-label { display: block; font-size: 13px; font-weight: 500; margin-bottom: 4px; color: var(--el-text-color-regular); }
.no-params { color: var(--el-text-color-secondary); text-align: center; padding: 20px 0; }

.schema-pre { background: var(--el-fill-color-light); padding: 16px; border-radius: 6px; overflow: auto; max-height: 400px; font-size: 13px; font-family: 'Courier New', monospace; }
.params-tip { margin-bottom: 12px; padding: 12px 16px; background: var(--el-color-primary-light-9); border-radius: 6px; font-size: 13px; color: var(--el-text-color-regular); border-left: 3px solid var(--el-color-primary); }
.params-tip p { margin: 0 0 6px 0; }
.params-tip p:last-child { margin-bottom: 0; }
.params-tip code { background: var(--el-fill-color); padding: 1px 4px; border-radius: 3px; font-size: 12px; color: var(--el-color-primary); }

/* 表格样式优化 */
:deep(.el-table) { border-radius: 8px; overflow: hidden; }
:deep(.el-table th) { background: var(--el-fill-color-lighter) !important; font-weight: 600; }
:deep(.el-tabs__item) { font-weight: 500; }
:deep(.el-tabs__header) { margin-bottom: 16px; }
</style>
