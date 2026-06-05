<template>
  <div class="pipeline-detail-page">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>流水线详情</span>
          <el-button @click="router.back()">返回</el-button>
        </div>
      </template>

      <template v-if="pipeline">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="名称">{{ pipeline.name }}</el-descriptions-item>
          <el-descriptions-item label="项目">{{ pipeline.projectId }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(pipeline.status)" size="small">
              {{ getStatusLabel(pipeline.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="触发方式">{{ pipeline.triggerType || '手动' }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ pipeline.description || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(pipeline.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="最后运行">{{ formatTime(pipeline.lastRunAt) }}</el-descriptions-item>
        </el-descriptions>

        <!-- 操作按钮 -->
        <div class="actions">
          <el-button type="primary" @click="handleTrigger" :disabled="pipeline.status === 'RUNNING'">
            <el-icon><VideoPlay /></el-icon> 触发执行
          </el-button>
          <el-button type="warning" @click="handleEdit">
            <el-icon><Edit /></el-icon> 编辑
          </el-button>
          <el-button type="danger" @click="handleDelete">
            <el-icon><Delete /></el-icon> 删除
          </el-button>
        </div>

        <!-- 阶段列表 -->
        <div class="stages-section">
          <h4>执行阶段</h4>
          <el-timeline>
            <el-timeline-item
              v-for="stage in stages"
              :key="stage.id"
              :type="getStageType(stage.status)"
              :timestamp="formatTime(stage.startedAt)"
              placement="top"
            >
              <el-card shadow="hover">
                <h5>{{ stage.name }}</h5>
                <p class="stage-status">
                  <el-tag :type="getStageType(stage.status)" size="small">
                    {{ stage.status || '等待中' }}
                  </el-tag>
                </p>
                <p v-if="stage.duration" class="stage-duration">
                  耗时: {{ stage.duration }}ms
                </p>
              </el-card>
            </el-timeline-item>
          </el-timeline>
        </div>
      </template>

      <el-empty v-else-if="!loading" description="流水线不存在" />
    </el-card>
  </div>
</template>

<script setup>
/**
 * 流水线详情页面
 * 查看流水线信息和执行阶段
 */
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { pipelineApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const pipeline = ref(null)
const stages = ref([])

/** 获取状态标签类型 */
const getStatusType = (status) => {
  const typeMap = {
    'ACTIVE': 'success',
    'RUNNING': 'warning',
    'SUCCESS': 'success',
    'FAILED': 'danger',
    'PENDING': 'info'
  }
  return typeMap[status] || 'info'
}

/** 获取状态标签文本 */
const getStatusLabel = (status) => {
  const labelMap = {
    'ACTIVE': '活跃',
    'RUNNING': '运行中',
    'SUCCESS': '成功',
    'FAILED': '失败',
    'PENDING': '等待中'
  }
  return labelMap[status] || status
}

/** 获取阶段类型 */
const getStageType = (status) => {
  const typeMap = {
    'SUCCESS': 'success',
    'RUNNING': 'warning',
    'FAILED': 'danger',
    'PENDING': 'info'
  }
  return typeMap[status] || 'info'
}

/** 格式化时间 */
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

/** 加载流水线详情 */
const loadPipeline = async () => {
  const id = route.params.id
  if (!id) return

  loading.value = true
  try {
    const [pipelineData, stagesData] = await Promise.all([
      pipelineApi.getById(id),
      pipelineApi.getStages(id)
    ])
    pipeline.value = pipelineData
    stages.value = stagesData || []
  } catch (error) {
    ElMessage.error('加载流水线详情失败')
  } finally {
    loading.value = false
  }
}

/** 触发执行 */
const handleTrigger = async () => {
  try {
    await ElMessageBox.confirm('确定要触发流水线执行吗？', '确认操作')
    await pipelineApi.trigger(route.params.id)
    ElMessage.success('流水线已触发')
    loadPipeline()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('触发失败')
    }
  }
}

/** 编辑 */
const handleEdit = () => {
  router.push(`/pipeline/create?id=${route.params.id}`)
}

/** 删除 */
const handleDelete = async () => {
  try {
    await ElMessageBox.confirm('确定要删除此流水线吗？此操作不可恢复！', '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await pipelineApi.delete(route.params.id)
    ElMessage.success('流水线已删除')
    router.push('/pipeline')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadPipeline()
})
</script>

<style scoped>
.pipeline-detail-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.actions {
  margin-top: 24px;
  display: flex;
  gap: 12px;
}

.stages-section {
  margin-top: 24px;
}

.stages-section h4 {
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--el-border-color);
}

.stage-status {
  margin: 8px 0;
}

.stage-duration {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
</style>
