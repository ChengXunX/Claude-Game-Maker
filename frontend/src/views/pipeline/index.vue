<template>
  <div class="pipeline-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>CICD 流水线</span>
          <el-button type="primary" @click="handleCreate" v-permission="'pipeline:create'">
            <el-icon><Plus /></el-icon> 创建流水线
          </el-button>
        </div>
      </template>

      <el-table :data="pipelines" v-loading="loading" stripe>
        <el-table-column prop="name" label="流水线名称" width="150" />
        <el-table-column prop="projectName" label="项目" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="阶段" width="200">
          <template #default="{ row }">
            <el-steps :active="getActiveStage(row)" finish-status="success" simple style="font-size: 12px">
              <el-step v-for="stage in (row.stages || [])" :key="stage.id" :title="stage.name" />
            </el-steps>
          </template>
        </el-table-column>
        <el-table-column label="最后执行" width="180">
          <template #default="{ row }">
            {{ formatTime(row.lastExecutedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="handleViewDetail(row)">详情</el-button>
            <el-button type="success" size="small" text @click="handleExecute(row)" v-permission="'pipeline:execute'">
              执行
            </el-button>
            <el-button type="danger" size="small" text @click="handleDelete(row)" v-permission="'pipeline:manage'">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && pipelines.length === 0" description="暂无流水线" />
    </el-card>
  </div>
</template>

<script setup>
/**
 * CICD 流水线页面
 * 管理 CICD 流水线
 *
 * 操作维度：项目级
 * 权限要求：pipeline:view / pipeline:manage
 */
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { pipelineApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

const loading = ref(false)
const pipelines = ref([])

/** 获取状态标签类型 */
const getStatusType = (status) => {
  const typeMap = {
    'IDLE': 'info',
    'RUNNING': 'warning',
    'SUCCESS': 'success',
    'FAILED': 'danger',
    'PENDING_APPROVAL': 'warning'
  }
  return typeMap[status] || 'info'
}

/** 获取状态标签文本 */
const getStatusLabel = (status) => {
  const labelMap = {
    'IDLE': '空闲',
    'RUNNING': '运行中',
    'SUCCESS': '成功',
    'FAILED': '失败',
    'PENDING_APPROVAL': '待审批'
  }
  return labelMap[status] || status
}

/** 获取当前活跃阶段 */
const getActiveStage = (pipeline) => {
  if (!pipeline.stages) return 0
  const idx = pipeline.stages.findIndex(s => s.status === 'RUNNING')
  return idx >= 0 ? idx : pipeline.stages.length
}

/** 格式化时间 */
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

/** 加载流水线列表 */
const loadPipelines = async () => {
  loading.value = true
  try {
    const data = await pipelineApi.getAll()
    pipelines.value = data || []
  } catch (error) {
    ElMessage.error('加载流水线列表失败')
  } finally {
    loading.value = false
  }
}

/** 创建流水线 */
const handleCreate = () => {
  router.push('/pipeline/create')
}

/** 查看详情 */
const handleViewDetail = (pipeline) => {
  router.push(`/pipeline/${pipeline.id}`)
}

/** 执行流水线 */
const handleExecute = async (pipeline) => {
  try {
    await ElMessageBox.confirm(
      `确定要执行流水线 "${pipeline.name}" 吗？`,
      '执行确认',
      { confirmButtonText: '执行', cancelButtonText: '取消', type: 'info' }
    )

    await pipelineApi.execute(pipeline.id)
    ElMessage.success('流水线已开始执行')
    loadPipelines()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('执行失败')
    }
  }
}

/** 删除流水线 */
const handleDelete = async (pipeline) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除流水线 "${pipeline.name}" 吗？`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )

    await pipelineApi.delete(pipeline.id)
    ElMessage.success('流水线已删除')
    loadPipelines()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadPipelines()
})
</script>

<style scoped>
.pipeline-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
