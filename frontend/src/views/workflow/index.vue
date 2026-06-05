<template>
  <div class="workflow-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>工作流管理</span>
          <el-button type="primary" @click="handleStartWorkflow" v-permission="'workflow:manage'">
            <el-icon><VideoPlay /></el-icon> 启动工作流
          </el-button>
        </div>
      </template>

      <!-- 标签页 -->
      <el-tabs v-model="activeTab">
        <!-- 工作流模板 -->
        <el-tab-pane label="工作流模板" name="templates">
          <el-table :data="templates" v-loading="loading" stripe>
            <el-table-column prop="id" label="模板 ID" width="150" show-overflow-tooltip />
            <el-table-column prop="name" label="名称" width="150" />
            <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
            <el-table-column label="步骤数" width="80">
              <template #default="{ row }">
                {{ (row.steps || []).length }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" text @click="handleStartWithTemplate(row)" v-permission="'workflow:manage'">
                  启动
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 运行中的工作流 -->
        <el-tab-pane label="运行中的工作流" name="instances">
          <el-table :data="instances" v-loading="loading" stripe>
            <el-table-column prop="id" label="实例 ID" width="150" show-overflow-tooltip />
            <el-table-column prop="templateId" label="模板 ID" width="120" />
            <el-table-column prop="projectId" label="项目 ID" width="120" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" size="small">
                  {{ getStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="进度" width="150">
              <template #default="{ row }">
                <el-progress :percentage="getProgress(row)" :stroke-width="8" />
              </template>
            </el-table-column>
            <el-table-column label="创建时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button type="warning" size="small" text @click="handlePause(row)" v-if="row.status === 'RUNNING'" v-permission="'workflow:manage'">
                  暂停
                </el-button>
                <el-button type="success" size="small" text @click="handleResume(row)" v-if="row.status === 'PAUSED'" v-permission="'workflow:manage'">
                  恢复
                </el-button>
                <el-button type="danger" size="small" text @click="handleCancel(row)" v-if="['RUNNING', 'PAUSED'].includes(row.status)" v-permission="'workflow:manage'">
                  取消
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 启动工作流对话框 -->
    <el-dialog v-model="startDialogVisible" title="启动工作流" width="500px">
      <el-form ref="startFormRef" :model="startForm" :rules="startRules" label-width="100px">
        <el-form-item label="选择模板" prop="templateId">
          <el-select v-model="startForm.templateId" placeholder="选择工作流模板">
            <el-option v-for="t in templates" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目 ID" prop="projectId">
          <el-input v-model="startForm.projectId" placeholder="关联的项目 ID" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="startDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleStartSubmit" :loading="submitting">启动</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 工作流页面
 * 管理工作流模板和实例
 *
 * 操作维度：系统级
 * 权限要求：workflow:view / workflow:manage
 */
import { ref, onMounted } from 'vue'
import { workflowApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const activeTab = ref('templates')
const templates = ref([])
const instances = ref([])

/** 启动对话框 */
const startDialogVisible = ref(false)
const startFormRef = ref(null)
const submitting = ref(false)
const startForm = ref({
  templateId: '',
  projectId: ''
})
const startRules = {
  templateId: [{ required: true, message: '请选择模板', trigger: 'change' }],
  projectId: [{ required: true, message: '请输入项目 ID', trigger: 'blur' }]
}

/** 获取状态标签类型 */
const getStatusType = (status) => {
  const typeMap = {
    'CREATED': 'info',
    'RUNNING': 'warning',
    'PAUSED': 'info',
    'COMPLETED': 'success',
    'FAILED': 'danger',
    'CANCELLED': 'info'
  }
  return typeMap[status] || 'info'
}

/** 获取状态标签文本 */
const getStatusLabel = (status) => {
  const labelMap = {
    'CREATED': '已创建',
    'RUNNING': '运行中',
    'PAUSED': '已暂停',
    'COMPLETED': '已完成',
    'FAILED': '失败',
    'CANCELLED': '已取消'
  }
  return labelMap[status] || status
}

/** 获取进度 */
const getProgress = (instance) => {
  if (!instance.stepExecutions) return 0
  const total = Object.keys(instance.stepExecutions).length
  if (total === 0) return 0
  const completed = Object.values(instance.stepExecutions).filter(s => s.status === 'COMPLETED').length
  return Math.round((completed / total) * 100)
}

/** 格式化时间 */
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

/** 加载数据 */
const loadData = async () => {
  loading.value = true
  try {
    const [templatesData, instancesData] = await Promise.all([
      workflowApi.getTemplates(),
      workflowApi.getInstances()
    ])
    templates.value = templatesData || []
    instances.value = instancesData || []
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

/** 打开启动对话框 */
const handleStartWorkflow = () => {
  startForm.value = { templateId: '', projectId: '' }
  startDialogVisible.value = true
}

/** 使用模板启动 */
const handleStartWithTemplate = (template) => {
  startForm.value = { templateId: template.id, projectId: '' }
  startDialogVisible.value = true
}

/** 提交启动 */
const handleStartSubmit = async () => {
  try {
    await startFormRef.value.validate()
    submitting.value = true

    await workflowApi.start(startForm.value)
    ElMessage.success('工作流已启动')
    startDialogVisible.value = false
    loadData()
  } catch (error) {
    if (error !== false) {
      ElMessage.error('启动失败')
    }
  } finally {
    submitting.value = false
  }
}

/** 暂停 */
const handlePause = async (instance) => {
  try {
    await workflowApi.pause(instance.id)
    ElMessage.success('工作流已暂停')
    loadData()
  } catch (error) {
    ElMessage.error('暂停失败')
  }
}

/** 恢复 */
const handleResume = async (instance) => {
  try {
    await workflowApi.resume(instance.id)
    ElMessage.success('工作流已恢复')
    loadData()
  } catch (error) {
    ElMessage.error('恢复失败')
  }
}

/** 取消 */
const handleCancel = async (instance) => {
  try {
    await ElMessageBox.confirm('确定要取消此工作流吗？', '取消确认', {
      confirmButtonText: '取消工作流',
      cancelButtonText: '返回',
      type: 'warning'
    })

    await workflowApi.cancel(instance.id)
    ElMessage.success('工作流已取消')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('取消失败')
    }
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.workflow-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
