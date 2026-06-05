<template>
  <div class="agent-detail-page">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>Agent 详情</span>
          <el-button @click="router.back()">返回</el-button>
        </div>
      </template>

      <el-descriptions :column="2" border v-if="agent">
        <el-descriptions-item label="ID">{{ agent.id }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ agent.name }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ agent.role || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="agent.alive ? 'success' : 'info'" size="small">
            {{ agent.alive ? '运行中' : '已停止' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="忙碌">
          <el-tag :type="agent.busy ? 'warning' : 'success'" size="small">
            {{ agent.busy ? '忙碌' : '空闲' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="项目 ID">{{ projectId }}</el-descriptions-item>
        <el-descriptions-item label="任务数">{{ agent.tasks || 0 }}</el-descriptions-item>
      </el-descriptions>

      <!-- 操作按钮 -->
      <div class="actions" v-if="agent">
        <el-button
          :type="agent.alive ? 'warning' : 'success'"
          @click="handleToggle"
        >
          {{ agent.alive ? '停止' : '启动' }}
        </el-button>
        <el-button type="info" @click="handleRestart">重启</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
/**
 * Agent 详情页面
 * 查看 Agent 详细信息
 *
 * 操作维度：项目级
 * 权限要求：agents:view
 */
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { agentApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const agent = ref(null)
const projectId = ref(route.params.projectId || '')
const agentRole = ref(route.params.agentRole || '')

/** 加载 Agent 详情 */
const loadAgent = async () => {
  if (!projectId.value || !agentRole.value) return

  loading.value = true
  try {
    const data = await agentApi.getById(projectId.value, agentRole.value)
    agent.value = data
  } catch (error) {
    ElMessage.error('加载 Agent 详情失败')
  } finally {
    loading.value = false
  }
}

/** 启动/停止 */
const handleToggle = async () => {
  const action = agent.value.alive ? '停止' : '启动'
  try {
    await ElMessageBox.confirm(`确定要${action} Agent 吗？`, '确认操作', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    if (agent.value.alive) {
      await agentApi.stop(projectId.value, agentRole.value)
    } else {
      await agentApi.start(projectId.value, agentRole.value)
    }

    ElMessage.success(`Agent 已${action}`)
    loadAgent()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(`${action}失败`)
    }
  }
}

/** 重启 */
const handleRestart = async () => {
  try {
    await ElMessageBox.confirm('确定要重启 Agent 吗？', '确认操作', {
      confirmButtonText: '重启',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await agentApi.restart(projectId.value, agentRole.value)

    ElMessage.success('Agent 已重启')
    loadAgent()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('重启失败')
    }
  }
}

onMounted(() => {
  loadAgent()
})
</script>

<style scoped>
.agent-detail-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.actions {
  margin-top: 20px;
  display: flex;
  gap: 12px;
}
</style>
