<template>
  <div class="pipeline-create-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ isEdit ? '编辑流水线' : '创建流水线' }}</span>
          <el-button @click="router.back()">返回</el-button>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="流水线名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入流水线名称" />
        </el-form-item>
        <el-form-item label="项目ID" prop="projectId">
          <el-input v-model="form.projectId" placeholder="请输入项目ID" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入流水线描述" />
        </el-form-item>
        <el-form-item label="触发方式">
          <el-select v-model="form.triggerType" placeholder="选择触发方式">
            <el-option label="手动触发" value="MANUAL" />
            <el-option label="定时触发" value="SCHEDULE" />
            <el-option label="Git推送触发" value="GIT_PUSH" />
          </el-select>
        </el-form-item>
        <el-form-item label="阶段配置">
          <div class="stages-config">
            <div v-for="(stage, index) in form.stages" :key="index" class="stage-item">
              <el-input v-model="stage.name" placeholder="阶段名称" style="width: 200px" />
              <el-select v-model="stage.type" placeholder="阶段类型" style="width: 150px">
                <el-option label="构建" value="BUILD" />
                <el-option label="测试" value="TEST" />
                <el-option label="部署" value="DEPLOY" />
                <el-option label="审批" value="APPROVAL" />
              </el-select>
              <el-input v-model="stage.command" placeholder="执行命令" style="flex: 1" />
              <el-button type="danger" @click="removeStage(index)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <el-button type="primary" @click="addStage">
              <el-icon><Plus /></el-icon> 添加阶段
            </el-button>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="router.back()">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ isEdit ? '保存修改' : '创建流水线' }}
        </el-button>
      </template>
    </el-card>
  </div>
</template>

<script setup>
/**
 * 流水线创建/编辑页面
 */
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { pipelineApi } from '@/api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const formRef = ref(null)
const submitting = ref(false)
const isEdit = computed(() => !!route.query.id)

const form = ref({
  name: '',
  projectId: '',
  description: '',
  triggerType: 'MANUAL',
  stages: [
    { name: '构建', type: 'BUILD', command: '' },
    { name: '测试', type: 'TEST', command: '' }
  ]
})

const rules = {
  name: [{ required: true, message: '请输入流水线名称', trigger: 'blur' }],
  projectId: [{ required: true, message: '请输入项目ID', trigger: 'blur' }]
}

/** 添加阶段 */
const addStage = () => {
  form.value.stages.push({ name: '', type: 'BUILD', command: '' })
}

/** 移除阶段 */
const removeStage = (index) => {
  form.value.stages.splice(index, 1)
}

/** 加载流水线数据（编辑模式） */
const loadPipeline = async () => {
  if (!route.query.id) return

  try {
    const [pipelineData, stagesData] = await Promise.all([
      pipelineApi.getById(route.query.id),
      pipelineApi.getStages(route.query.id)
    ])

    form.value = {
      name: pipelineData.name || '',
      projectId: pipelineData.projectId || '',
      description: pipelineData.description || '',
      triggerType: pipelineData.triggerType || 'MANUAL',
      stages: stagesData?.length > 0 ? stagesData : [{ name: '构建', type: 'BUILD', command: '' }]
    }
  } catch (error) {
    ElMessage.error('加载流水线数据失败')
  }
}

/** 提交表单 */
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    if (isEdit.value) {
      await pipelineApi.update(route.query.id, form.value)
      ElMessage.success('流水线更新成功')
    } else {
      await pipelineApi.create(form.value)
      ElMessage.success('流水线创建成功')
    }
    router.push('/pipeline')
  } catch (error) {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadPipeline()
})
</script>

<style scoped>
.pipeline-create-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stages-config {
  width: 100%;
}

.stage-item {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
  align-items: center;
}
</style>
