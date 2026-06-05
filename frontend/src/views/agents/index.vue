<template>
  <div class="agents-page">
    <!-- 项目选择区 -->
    <el-card class="project-selector-card">
      <div class="project-selector-wrapper">
        <span class="selector-label">选择项目：</span>
        <ProjectSelector
          v-model="selectedProjectId"
          @change="handleProjectChange"
          width="300px"
        />
        <el-tag v-if="selectedProject" type="info" size="small" class="project-tag">
          工作目录: {{ selectedProject.workDir }}
        </el-tag>
      </div>
    </el-card>

    <!-- Agent 列表 -->
    <el-card v-if="selectedProjectId">
      <template #header>
        <div class="card-header">
          <span>{{ selectedProject?.name }} - Agent 列表</span>
          <el-button @click="loadAgents" :loading="loading">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </template>

      <el-table :data="agents" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="150" show-overflow-tooltip />
        <el-table-column prop="name" label="名称" width="120" />
        <el-table-column prop="role" label="角色" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ row.role || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.alive ? 'success' : 'info'" size="small">
              {{ row.alive ? '运行中' : '已停止' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="忙碌" width="80">
          <template #default="{ row }">
            <el-tag :type="row.busy ? 'warning' : 'success'" size="small">
              {{ row.busy ? '忙碌' : '空闲' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="handleViewDetail(row)">详情</el-button>
            <el-button
              :type="row.alive ? 'warning' : 'success'"
              size="small"
              text
              @click="handleToggle(row)"
              v-permission="'agents:manage'"
            >
              {{ row.alive ? '停止' : '启动' }}
            </el-button>
            <el-button type="info" size="small" text @click="handleRestart(row)" v-permission="'agents:manage'">重启</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 未选择项目时的提示 -->
    <el-card v-else class="empty-card">
      <el-empty description="请先选择一个项目">
        <template #image>
          <el-icon :size="64" color="#c0c4cc"><Folder /></el-icon>
        </template>
      </el-empty>
    </el-card>
  </div>
</template>

<script setup>
/**
 * Agent 管理页面
 * 先选择项目，再管理该项目下的 Agent
 *
 * 操作维度：项目级
 * 权限要求：agents:view
 */
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { agentApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Folder } from '@element-plus/icons-vue'
import ProjectSelector from '@/components/ProjectSelector.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const agents = ref([])
const selectedProjectId = ref(route.query.projectId || localStorage.getItem('selectedProjectId') || '')
const projects = ref([])

const selectedProject = computed(() => {
  return projects.value.find(p => p.id === selectedProjectId.value) || null
})

/** 项目切换 */
const handleProjectChange = (projectId) => {
  if (projectId) {
    loadAgents()
  } else {
    agents.value = []
  }
}

/** 加载 Agent 列表 */
const loadAgents = async () => {
  if (!selectedProjectId.value) return

  loading.value = true
  try {
    const data = await agentApi.getByProject(selectedProjectId.value)
    agents.value = data || []
  } catch (error) {
    ElMessage.error('加载 Agent 列表失败')
  } finally {
    loading.value = false
  }
}

/** 查看详情 */
const handleViewDetail = (agent) => {
  router.push(`/agents/${selectedProjectId.value}/${agent.role}`)
}

/** 启动/停止 */
const handleToggle = async (agent) => {
  const action = agent.alive ? '停止' : '启动'
  try {
    await ElMessageBox.confirm(`确定要${action} Agent "${agent.name}" 吗？`, '确认操作', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    if (agent.alive) {
      await agentApi.stop(selectedProjectId.value, agent.role)
    } else {
      await agentApi.start(selectedProjectId.value, agent.role)
    }

    ElMessage.success(`Agent 已${action}`)
    loadAgents()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(`${action}失败`)
    }
  }
}

/** 重启 */
const handleRestart = async (agent) => {
  try {
    await ElMessageBox.confirm(`确定要重启 Agent "${agent.name}" 吗？`, '确认操作', {
      confirmButtonText: '重启',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await agentApi.restart(selectedProjectId.value, agent.role)

    ElMessage.success('Agent 已重启')
    loadAgents()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('重启失败')
    }
  }
}

onMounted(() => {
  if (selectedProjectId.value) {
    loadAgents()
  }
})
</script>

<style scoped>
.agents-page {
  padding: 20px;
}

.project-selector-card {
  margin-bottom: 16px;
}

.project-selector-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.selector-label {
  font-weight: 500;
  color: #606266;
}

.project-tag {
  margin-left: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.empty-card {
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 手机端 */
@media (max-width: 767px) {
  .agents-page {
    padding: 12px;
  }

  .project-selector-wrapper {
    flex-direction: column;
    align-items: flex-start;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
