<template>
  <div class="projects-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>项目管理</span>
          <div class="header-actions">
            <el-button @click="handleImport" v-permission="'projects:manage'">
              <el-icon><Upload /></el-icon> 导入项目
            </el-button>
            <el-button type="primary" @click="handleCreate" v-permission="'projects:manage'">
              <el-icon><Plus /></el-icon> 创建项目
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="projects" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="150" show-overflow-tooltip />
        <el-table-column prop="name" label="项目名称" width="150" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="workDir" label="工作目录" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ACTIVE' ? '活跃' : row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Agent" width="80">
          <template #default="{ row }">
            {{ row.agentIds ? row.agentIds.length : 0 }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="handleViewCode(row)">代码</el-button>
            <el-button type="success" size="small" text @click="handleViewAgents(row)">Agent</el-button>
            <el-button type="warning" size="small" text @click="handleEdit(row)" v-permission="'projects:manage'">编辑</el-button>
            <el-button type="danger" size="small" text @click="handleDelete(row)" v-permission="'projects:manage'">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑/创建项目对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑项目' : '创建项目'"
      width="600px"
    >
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="项目名称" required>
          <el-input v-model="editForm.name" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" placeholder="请输入项目描述" />
        </el-form-item>
        <el-form-item label="工作目录" v-if="!isEdit" required>
          <el-input v-model="editForm.workDir" placeholder="请输入工作目录路径" />
        </el-form-item>
        <el-form-item label="模板" v-if="!isEdit">
          <el-select v-model="editForm.templateId" placeholder="选择游戏模板（可选）" clearable style="width: 100%">
            <el-option v-for="t in templates" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>

        <el-divider v-if="!isEdit" content-position="left">项目目标（可选）</el-divider>

        <el-form-item label="项目目标" v-if="!isEdit">
          <el-input v-model="editForm.goal" type="textarea" :rows="2" placeholder="描述项目目标，如：开发一个休闲益智类H5小游戏" />
        </el-form-item>
        <el-form-item label="目标类型" v-if="!isEdit && editForm.goal">
          <el-select v-model="editForm.goalType" placeholder="选择目标类型" style="width: 100%">
            <el-option label="游戏开发" value="GAME_DEVELOPMENT" />
            <el-option label="功能开发" value="FEATURE" />
            <el-option label="Bug修复" value="BUG_FIX" />
            <el-option label="重构优化" value="REFACTOR" />
            <el-option label="自定义" value="CUSTOM" />
          </el-select>
        </el-form-item>

        <el-divider v-if="!isEdit" content-position="left">API配置（可选）</el-divider>

        <el-form-item label="API Key" v-if="!isEdit">
          <el-input v-model="editForm.apiKey" placeholder="全局API Key（可选，后续可为各Agent单独配置）" show-password />
        </el-form-item>
        <el-form-item label="API地址" v-if="!isEdit">
          <el-input v-model="editForm.apiUrl" placeholder="API地址，如：https://api.anthropic.com" />
        </el-form-item>
        <el-form-item label="模型" v-if="!isEdit">
          <el-input v-model="editForm.model" placeholder="模型名称，如：claude-sonnet-4-20250514" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 导入项目对话框 -->
    <el-dialog
      v-model="importDialogVisible"
      title="导入项目"
      width="600px"
    >
      <el-alert type="info" :closable="false" class="mb-4">
        <template #title>
          <div>
            <p>导入已有项目目录到系统中。</p>
            <p>系统会自动扫描目录结构，创建项目配置，并关联相关Agent。</p>
          </div>
        </template>
      </el-alert>

      <el-form :model="importForm" label-width="100px">
        <el-form-item label="项目名称" required>
          <el-input v-model="importForm.name" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="importForm.description" type="textarea" :rows="2" placeholder="请输入项目描述" />
        </el-form-item>
        <el-form-item label="工作目录" required>
          <el-input v-model="importForm.workDir" placeholder="请输入已存在的项目目录路径">
            <template #append>
              <el-button @click="checkDirectory">检查</el-button>
            </template>
          </el-input>
          <div v-if="directoryCheckResult" class="directory-check" :class="directoryCheckResult.valid ? 'success' : 'error'">
            {{ directoryCheckResult.message }}
          </div>
        </el-form-item>
        <el-form-item label="模板">
          <el-select v-model="importForm.templateId" placeholder="选择游戏模板（可选）" clearable style="width: 100%">
            <el-option v-for="t in templates" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>

        <el-divider content-position="left">项目目标（可选）</el-divider>

        <el-form-item label="项目目标">
          <el-input v-model="importForm.goal" type="textarea" :rows="2" placeholder="描述项目目标" />
        </el-form-item>
        <el-form-item label="目标类型" v-if="importForm.goal">
          <el-select v-model="importForm.goalType" placeholder="选择目标类型" style="width: 100%">
            <el-option label="游戏开发" value="GAME_DEVELOPMENT" />
            <el-option label="功能开发" value="FEATURE" />
            <el-option label="Bug修复" value="BUG_FIX" />
            <el-option label="重构优化" value="REFACTOR" />
            <el-option label="自定义" value="CUSTOM" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleImportSubmit" :loading="importing">导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 项目管理页面
 * 管理游戏项目
 *
 * 操作维度：项目级
 * 权限要求：projects:view
 */
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { projectApi, gameTemplateApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const projects = ref([])
const templates = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)

// 导入相关状态
const importDialogVisible = ref(false)
const importing = ref(false)
const directoryCheckResult = ref(null)

const editForm = ref({
  name: '',
  description: '',
  workDir: '',
  templateId: '',
  goal: '',
  goalType: 'GAME_DEVELOPMENT',
  apiKey: '',
  apiUrl: '',
  model: ''
})

const importForm = ref({
  name: '',
  description: '',
  workDir: '',
  templateId: '',
  goal: '',
  goalType: 'GAME_DEVELOPMENT'
})

/** 加载项目列表 */
const loadProjects = async () => {
  loading.value = true
  try {
    const data = await projectApi.getAll()
    projects.value = data || []
  } catch (error) {
    ElMessage.error('加载项目列表失败')
  } finally {
    loading.value = false
  }
}

/** 加载模板列表 */
const loadTemplates = async () => {
  try {
    const data = await gameTemplateApi.getAll()
    templates.value = data || []
  } catch (error) {
    console.error('加载模板失败:', error)
  }
}

/** 创建项目 */
const handleCreate = () => {
  isEdit.value = false
  editingId.value = null
  editForm.value = {
    name: '',
    description: '',
    workDir: '',
    templateId: '',
    goal: '',
    goalType: 'GAME_DEVELOPMENT',
    apiKey: '',
    apiUrl: '',
    model: ''
  }
  dialogVisible.value = true
}

/** 查看代码 */
const handleViewCode = (project) => {
  router.push(`/code/${project.id}`)
}

/** 查看Agent - 跳转到Agent列表页 */
const handleViewAgents = (project) => {
  router.push({ path: '/agents', query: { projectId: project.id } })
}

/** 编辑项目 */
const handleEdit = (project) => {
  isEdit.value = true
  editingId.value = project.id
  editForm.value = {
    name: project.name || '',
    description: project.description || '',
    workDir: project.workDir || '',
    templateId: project.templateId || ''
  }
  dialogVisible.value = true
}

/** 保存项目 */
const handleSave = async () => {
  if (!editForm.value.name) {
    ElMessage.warning('请输入项目名称')
    return
  }

  saving.value = true
  try {
    if (isEdit.value) {
      // 编辑模式 - 目前后端不支持直接编辑，提示用户
      ElMessage.info('项目信息修改功能暂不支持，如需修改请删除后重新创建')
    } else {
      // 创建模式
      if (!editForm.value.workDir) {
        ElMessage.warning('请输入工作目录')
        return
      }
      await projectApi.create(editForm.value)
      ElMessage.success('项目创建成功')
      dialogVisible.value = false
      loadProjects()
    }
  } catch (error) {
    ElMessage.error(isEdit.value ? '编辑失败' : '创建失败')
  } finally {
    saving.value = false
  }
}

/** 删除项目 */
const handleDelete = async (project) => {
  try {
    await ElMessageBox.confirm(`确定要删除项目 "${project.name}" 吗？此操作不可恢复！`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await projectApi.remove(project.id)
    ElMessage.success('项目已删除')
    loadProjects()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

/** 打开导入对话框 */
const handleImport = () => {
  importForm.value = {
    name: '',
    description: '',
    workDir: '',
    templateId: '',
    goal: '',
    goalType: 'GAME_DEVELOPMENT'
  }
  directoryCheckResult.value = null
  importDialogVisible.value = true
}

/** 检查目录 */
const checkDirectory = async () => {
  if (!importForm.value.workDir) {
    ElMessage.warning('请先输入工作目录路径')
    return
  }

  try {
    const result = await projectApi.checkDirectory(importForm.value.workDir)
    directoryCheckResult.value = result
  } catch (error) {
    directoryCheckResult.value = {
      valid: false,
      message: '检查失败: ' + (error.message || '未知错误')
    }
  }
}

/** 提交导入 */
const handleImportSubmit = async () => {
  if (!importForm.value.name) {
    ElMessage.warning('请输入项目名称')
    return
  }
  if (!importForm.value.workDir) {
    ElMessage.warning('请输入工作目录路径')
    return
  }

  importing.value = true
  try {
    await projectApi.import(importForm.value)
    ElMessage.success('项目导入成功')
    importDialogVisible.value = false
    loadProjects()
  } catch (error) {
    ElMessage.error('导入失败: ' + (error.message || '未知错误'))
  } finally {
    importing.value = false
  }
}

onMounted(() => {
  loadProjects()
  loadTemplates()
})
</script>

<style scoped>
.projects-page {
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

.mb-4 {
  margin-bottom: 16px;
}

.directory-check {
  margin-top: 8px;
  font-size: 13px;
}

.directory-check.success {
  color: var(--el-color-success);
}

.directory-check.error {
  color: var(--el-color-danger);
}

/* 手机端 */
@media (max-width: 767px) {
  .projects-page {
    padding: 12px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
  }

  .header-actions .el-button {
    flex: 1;
  }
}
</style>
