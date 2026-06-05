<template>
  <div class="git-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>Git 仓库管理</span>
          <el-button type="primary" @click="handleAddRepo" v-permission="'projects:manage'">
            <el-icon><Plus /></el-icon> 添加仓库
          </el-button>
        </div>
      </template>

      <el-table :data="repositories" v-loading="loading" stripe>
        <el-table-column prop="name" label="仓库名称" width="150" />
        <el-table-column prop="url" label="仓库地址" min-width="250" show-overflow-tooltip />
        <el-table-column prop="branch" label="默认分支" width="120" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.repositoryType || 'LOCAL' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最后同步" width="180">
          <template #default="{ row }">
            {{ formatTime(row.lastSyncAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="handleViewDetail(row)">详情</el-button>
            <el-button type="success" size="small" text @click="handlePull(row)">Pull</el-button>
            <el-button type="warning" size="small" text @click="handlePush(row)">Push</el-button>
            <el-button type="danger" size="small" text @click="handleDelete(row)" v-permission="'projects:manage'">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && repositories.length === 0" description="暂无 Git 仓库" />
    </el-card>

    <!-- 添加仓库对话框 -->
    <el-dialog v-model="addDialogVisible" title="添加 Git 仓库" width="500px">
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="100px">
        <el-form-item label="仓库名称" prop="name">
          <el-input v-model="addForm.name" placeholder="如：game-server" />
        </el-form-item>
        <el-form-item label="仓库地址" prop="url">
          <el-input v-model="addForm.url" placeholder="https://github.com/xxx/xxx.git" />
        </el-form-item>
        <el-form-item label="默认分支" prop="branch">
          <el-input v-model="addForm.branch" placeholder="main" />
        </el-form-item>
        <el-form-item label="项目 ID" prop="projectId">
          <el-input v-model="addForm.projectId" placeholder="关联的项目 ID" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAddSubmit" :loading="submitting">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * Git 仓库页面
 * 管理 Git 仓库
 *
 * 操作维度：项目级
 * 权限要求：projects:view / projects:manage
 */
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { gitApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

const loading = ref(false)
const repositories = ref([])

/** 添加仓库对话框 */
const addDialogVisible = ref(false)
const addFormRef = ref(null)
const submitting = ref(false)
const addForm = ref({
  name: '',
  url: '',
  branch: 'main',
  projectId: ''
})
const addRules = {
  name: [{ required: true, message: '请输入仓库名称', trigger: 'blur' }],
  url: [{ required: true, message: '请输入仓库地址', trigger: 'blur' }]
}

/** 格式化时间 */
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

/** 加载仓库列表 */
const loadRepositories = async () => {
  loading.value = true
  try {
    const data = await gitApi.getRepositories()
    repositories.value = data || []
  } catch (error) {
    ElMessage.error('加载仓库列表失败')
  } finally {
    loading.value = false
  }
}

/** 查看详情 */
const handleViewDetail = (repo) => {
  router.push(`/git/${repo.id}`)
}

/** Pull */
const handlePull = async (repo) => {
  try {
    await gitApi.pull(repo.id)
    ElMessage.success('Pull 成功')
    loadRepositories()
  } catch (error) {
    ElMessage.error('Pull 失败')
  }
}

/** Push */
const handlePush = async (repo) => {
  try {
    await gitApi.push(repo.id)
    ElMessage.success('Push 成功')
  } catch (error) {
    ElMessage.error('Push 失败')
  }
}

/** 删除仓库 */
const handleDelete = async (repo) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除仓库 "${repo.name}" 吗？`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )

    await gitApi.deleteRepository(repo.id)
    ElMessage.success('仓库已删除')
    loadRepositories()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

/** 打开添加对话框 */
const handleAddRepo = () => {
  addForm.value = { name: '', url: '', branch: 'main', projectId: '' }
  addDialogVisible.value = true
}

/** 提交添加 */
const handleAddSubmit = async () => {
  try {
    await addFormRef.value.validate()
    submitting.value = true

    await gitApi.addRepository(addForm.value)
    ElMessage.success('仓库已添加')
    addDialogVisible.value = false
    loadRepositories()
  } catch (error) {
    if (error !== false) {
      ElMessage.error('添加失败')
    }
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadRepositories()
})
</script>

<style scoped>
.git-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
