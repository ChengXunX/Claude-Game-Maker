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
        <el-select v-model="filterAgent" placeholder="选择Agent" clearable @change="loadFiles">
          <el-option label="全部Agent" value="" />
          <el-option v-for="agent in agents" :key="agent" :label="agent" :value="agent" />
        </el-select>
      </div>

      <!-- 文件列表 -->
      <el-table :data="files" v-loading="loading" stripe>
        <el-table-column prop="fileName" label="文件名" min-width="200" show-overflow-tooltip />
        <el-table-column prop="agentId" label="Agent" width="120" />
        <el-table-column prop="fileSize" label="大小" width="100">
          <template #default="{ row }">
            {{ formatSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="mimeType" label="类型" width="150" />
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
            <el-button type="danger" size="small" text @click="handleDelete(row)" v-permission="'agents:manage'">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 上传对话框 -->
    <el-dialog v-model="uploadDialogVisible" title="上传文件" width="500px">
      <el-form label-width="100px">
        <el-form-item label="Agent">
          <el-select v-model="uploadForm.agentId" placeholder="选择Agent">
            <el-option v-for="agent in agents" :key="agent" :label="agent" :value="agent" />
          </el-select>
        </el-form-item>
        <el-form-item label="文件">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
          >
            <el-button type="primary">选择文件</el-button>
          </el-upload>
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
 * 管理Agent的文件
 *
 * 权限要求：agents:view, agents:manage
 */
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'

const loading = ref(false)
const files = ref([])
const agents = ref([])
const filterAgent = ref('')
const uploadDialogVisible = ref(false)
const uploading = ref(false)
const uploadRef = ref(null)
const selectedFile = ref(null)
const uploadForm = ref({
  agentId: ''
})

const formatSize = (bytes) => {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

const loadFiles = async () => {
  loading.value = true
  try {
    const url = filterAgent.value ? `/files/agent/${filterAgent.value}` : '/files/project/all'
    const data = await api.get(url)
    files.value = data || []
  } catch (error) {
    ElMessage.error('加载文件列表失败')
  } finally {
    loading.value = false
  }
}

const handleUpload = () => {
  uploadForm.value = { agentId: '' }
  selectedFile.value = null
  uploadDialogVisible.value = true
}

const handleFileChange = (file) => {
  selectedFile.value = file.raw
}

const handleUploadSubmit = async () => {
  if (!selectedFile.value || !uploadForm.value.agentId) {
    ElMessage.warning('请选择Agent和文件')
    return
  }

  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    formData.append('agentId', uploadForm.value.agentId)

    await api.post('/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    ElMessage.success('上传成功')
    uploadDialogVisible.value = false
    loadFiles()
  } catch (error) {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
}

const handleDownload = (file) => {
  window.open(`/api/files/${file.id}/download`)
}

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
}
</style>
