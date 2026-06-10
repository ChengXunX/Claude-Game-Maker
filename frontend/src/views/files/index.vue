<template>
  <div class="files-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>文件管理</span>
          <el-button type="primary" @click="handleUpload" v-permission="'agents:manage'">
            <el-icon><Upload /></el-icon> 上传文件
          </el-button>
        </div>
      </template>

      <!-- 筛选区 -->
      <div class="filter-bar">
        <el-select v-model="filterAgent" placeholder="选择Agent" clearable @change="handleFilterChange">
          <el-option label="全部Agent" value="" />
          <el-option v-for="agentId in agentIds" :key="agentId" :label="agentId" :value="agentId" />
        </el-select>
        <el-input
          v-model="searchKeyword"
          placeholder="搜索文件名"
          clearable
          style="width: 200px"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <!-- 文件列表 -->
      <el-table :data="files" v-loading="loading" stripe empty-text="暂无文件">
        <el-table-column prop="fileName" label="文件名" min-width="200" show-overflow-tooltip />
        <el-table-column prop="agentId" label="Agent" width="150" show-overflow-tooltip />
        <el-table-column prop="projectId" label="项目" width="120" show-overflow-tooltip />
        <el-table-column prop="fileSize" label="大小" width="100">
          <template #default="{ row }">
            {{ formatSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="mimeType" label="类型" width="150" show-overflow-tooltip />
        <el-table-column prop="source" label="来源" width="80">
          <template #default="{ row }">
            <el-tag :type="row.source === 'USER_UPLOAD' ? 'primary' : 'info'" size="small">
              {{ getSourceLabel(row.source) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="上传时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="handleDownload(row)">
              下载
            </el-button>
            <el-button type="danger" size="small" text @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @size-change="loadFiles"
          @current-change="loadFiles"
        />
      </div>
    </el-card>

    <!-- 上传对话框 -->
    <el-dialog v-model="uploadDialogVisible" title="上传文件" width="500px">
      <el-form label-width="100px">
        <el-form-item label="Agent" required>
          <el-select v-model="uploadForm.agentId" placeholder="选择Agent">
            <el-option v-for="agentId in agentIds" :key="agentId" :label="agentId" :value="agentId" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目ID">
          <el-input v-model="uploadForm.projectId" placeholder="关联的项目ID（可选）" />
        </el-form-item>
        <el-form-item label="文件" required>
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="() => selectedFile = null"
          >
            <el-button type="primary">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">单个文件最大 50MB</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="uploadForm.remark" placeholder="文件备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUploadSubmit" :loading="uploading">上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 文件管理页面
 * 管理所有 Agent 的文件，支持按 Agent 筛选、搜索、分页
 *
 * 权限要求：agents:view
 */
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'

const loading = ref(false)
const files = ref([])
const agentIds = ref([])
const filterAgent = ref('')
const searchKeyword = ref('')
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const uploadDialogVisible = ref(false)
const uploading = ref(false)
const uploadRef = ref(null)
const selectedFile = ref(null)
const uploadForm = ref({
  agentId: '',
  projectId: '',
  remark: ''
})

const formatSize = (bytes) => {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

/** 获取文件来源标签 */
const getSourceLabel = (source) => {
  const map = {
    'USER_UPLOAD': '上传',
    'AGENT_GENERATED': '生成',
    'MCP': 'MCP',
    'AGENT_TRANSFER': '传输'
  }
  return map[source] || source || '-'
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

/** 加载 Agent ID 列表（用于筛选下拉框） */
const loadAgentIds = async () => {
  try {
    const data = await api.get('/files/agents')
    agentIds.value = data || []
  } catch (error) {
    // 静默失败
  }
}

/** 加载文件列表 */
const loadFiles = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value
    }

    let data
    if (filterAgent.value) {
      data = await api.get(`/files/agent/${filterAgent.value}`, { params })
    } else {
      data = await api.get('/files/all', { params })
    }

    files.value = data?.content || []
    total.value = data?.totalElements || 0
  } catch (error) {
    ElMessage.error('加载文件列表失败')
  } finally {
    loading.value = false
  }
}

/** 搜索文件（支持全局搜索和按Agent搜索） */
const handleSearch = async () => {
  if (!searchKeyword.value.trim()) {
    loadFiles()
    return
  }

  loading.value = true
  try {
    const params = {
      keyword: searchKeyword.value.trim(),
      page: 0,
      size: pageSize.value
    }
    let data
    if (filterAgent.value) {
      data = await api.get(`/files/search/${filterAgent.value}`, { params })
    } else {
      data = await api.get('/files/search', { params })
    }
    files.value = data?.content || []
    total.value = data?.totalElements || 0
    currentPage.value = 1
  } catch (error) {
    ElMessage.error('搜索失败')
  } finally {
    loading.value = false
  }
}

/** 筛选变更 */
const handleFilterChange = () => {
  currentPage.value = 1
  searchKeyword.value = ''
  loadFiles()
}

/** 重置筛选 */
const handleReset = () => {
  filterAgent.value = ''
  searchKeyword.value = ''
  currentPage.value = 1
  loadFiles()
}

/** 打开上传对话框 */
const handleUpload = () => {
  uploadForm.value = { agentId: '', projectId: '', remark: '' }
  selectedFile.value = null
  uploadDialogVisible.value = true
}

/** 文件选择 */
const handleFileChange = (file) => {
  selectedFile.value = file.raw
}

/** 提交上传 */
const handleUploadSubmit = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请选择文件')
    return
  }
  if (!uploadForm.value.agentId) {
    ElMessage.warning('请选择 Agent')
    return
  }

  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    formData.append('agentId', uploadForm.value.agentId)
    if (uploadForm.value.projectId) {
      formData.append('projectId', uploadForm.value.projectId)
    }
    if (uploadForm.value.remark) {
      formData.append('remark', uploadForm.value.remark)
    }

    await api.post('/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    ElMessage.success('上传成功')
    uploadDialogVisible.value = false
    loadFiles()
    loadAgentIds()
  } catch (error) {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
}

/** 下载文件 */
const handleDownload = (file) => {
  window.open(`/api/files/${file.id}/download`)
}

/** 删除文件 */
const handleDelete = async (file) => {
  try {
    await ElMessageBox.confirm(`确定要删除文件 "${file.fileName}" 吗？`, '删除确认')
    await api.delete(`/files/${file.id}`)
    ElMessage.success('删除成功')
    loadFiles()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadAgentIds()
  loadFiles()
})
</script>

<style scoped>
.files-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
