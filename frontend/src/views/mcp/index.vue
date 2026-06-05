<template>
  <div class="mcp-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>MCP服务器管理</span>
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

      <!-- 服务器列表 -->
      <el-table :data="servers" v-loading="loading" stripe>
        <el-table-column prop="name" label="名称" width="150" />
        <el-table-column prop="command" label="命令" min-width="200" show-overflow-tooltip />
        <el-table-column prop="projectId" label="项目" width="120" />
        <el-table-column prop="enabled" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
              {{ row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="handleToggle(row)" v-permission="'agents:manage'">
              {{ row.enabled ? '禁用' : '启用' }}
            </el-button>
            <el-button type="info" size="small" text @click="handleTest(row)">
              测试
            </el-button>
            <el-button type="danger" size="small" text @click="handleDelete(row)" v-permission="'agents:manage'">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建对话框 -->
    <el-dialog v-model="dialogVisible" title="添加MCP服务器" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="服务器名称" prop="name">
          <el-input v-model="form.name" placeholder="如：filesystem" />
        </el-form-item>
        <el-form-item label="命令" prop="command">
          <el-input v-model="form.command" placeholder="如：npx -y @modelcontextprotocol/server-filesystem" />
        </el-form-item>
        <el-form-item label="参数">
          <el-input v-model="form.args" placeholder="参数，用逗号分隔" />
        </el-form-item>
        <el-form-item label="项目ID">
          <el-input v-model="form.projectId" placeholder="可选，绑定到特定项目" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * MCP服务器管理页面
 * 管理Model Context Protocol服务器
 */
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'

const loading = ref(false)
const servers = ref([])
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const form = ref({
  name: '',
  command: '',
  args: '',
  projectId: ''
})
const rules = {
  name: [{ required: true, message: '请输入服务器名称', trigger: 'blur' }],
  command: [{ required: true, message: '请输入命令', trigger: 'blur' }]
}

const loadServers = async () => {
  loading.value = true
  try {
    const data = await api.get('/mcp/api/servers')
    servers.value = data || []
  } catch (error) {
    ElMessage.error('加载MCP服务器列表失败')
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  form.value = { name: '', command: '', args: '', projectId: '' }
  dialogVisible.value = true
}

const handleToggle = async (server) => {
  try {
    await api.post(`/mcp/api/servers/${server.id}/toggle`)
    ElMessage.success('状态已切换')
    loadServers()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const handleTest = async (server) => {
  try {
    const result = await api.post(`/mcp/api/servers/${server.id}/test`)
    // API返回服务器对象，通过connected和lastTestResult判断结果
    if (result.connected) {
      ElMessage.success(result.lastTestResult || '连接测试成功')
    } else {
      ElMessage.error(result.lastTestResult || '连接测试失败')
    }
    // 刷新服务器列表以更新状态
    loadServers()
  } catch (error) {
    ElMessage.error('测试失败')
  }
}

const handleDelete = async (server) => {
  try {
    await ElMessageBox.confirm(`确定要删除MCP服务器 "${server.name}" 吗？`, '删除确认')
    await api.delete(`/mcp/api/servers/${server.id}`)
    ElMessage.success('删除成功')
    loadServers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    await api.post('/mcp/api/servers', form.value)
    ElMessage.success('添加成功')
    dialogVisible.value = false
    loadServers()
  } catch (error) {
    ElMessage.error('添加失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadServers()
})
</script>

<style scoped>
.mcp-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.header-actions {
  display: flex;
  gap: 8px;
}
</style>
