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

        <!-- 推理深度列 -->
        <el-table-column label="推理深度" width="200">
          <template #default="{ row }">
            <div class="reasoning-depth-cell">
              <el-select
                :model-value="row.reasoningDepth || 3"
                size="small"
                style="width: 140px"
                @change="(val) => handleReasoningDepthChange(row, val)"
                :disabled="!hasPermission('agents:manage')"
              >
                <el-option
                  v-for="option in reasoningDepthOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                >
                  <div class="depth-option">
                    <span>{{ option.label }}</span>
                    <span class="depth-desc">{{ option.description }}</span>
                  </div>
                </el-option>
              </el-select>
              <el-tooltip :content="getDepthTooltip(row.reasoningDepth)" placement="top">
                <el-icon class="depth-info"><InfoFilled /></el-icon>
              </el-tooltip>
            </div>
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

    <!-- 推理深度说明对话框 -->
    <el-dialog v-model="depthHelpVisible" title="推理深度说明" width="500px">
      <div class="depth-help-content">
        <p>推理深度决定了 Agent 在处理任务时的思考深度和详细程度。深度越高，Agent 的回答越全面详细，但响应时间也会更长。</p>
        <el-table :data="reasoningDepthOptions" size="small" border>
          <el-table-column prop="label" label="级别" width="120" />
          <el-table-column prop="description" label="说明" />
        </el-table>
        <div class="depth-help-tip">
          <el-icon><InfoFilled /></el-icon>
          <span>建议根据任务复杂度选择合适的推理深度。简单任务使用"快速"或"标准"，复杂任务使用"深入"或"全面"。</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * Agent 管理页面
 * 先选择项目，再管理该项目下的 Agent
 *
 * 功能：
 * - 查看 Agent 列表和状态
 * - 启动/停止/重启 Agent
 * - 设置推理深度（全局生效）
 * - 查看 Agent 详情
 *
 * 操作维度：项目级
 * 权限要求：agents:view
 */
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { agentApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Folder, InfoFilled } from '@element-plus/icons-vue'
import ProjectSelector from '@/components/ProjectSelector.vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const agents = ref([])
const selectedProjectId = ref(route.query.projectId || localStorage.getItem('selectedProjectId') || '')
const projects = ref([])
const depthHelpVisible = ref(false)

// 推理深度选项
const reasoningDepthOptions = [
  { value: 1, label: '快速', description: '简洁直接，关注关键要点' },
  { value: 2, label: '标准', description: '清晰合理，必要分析' },
  { value: 3, label: '深入', description: '多角度分析，详细推理' },
  { value: 4, label: '全面', description: '全面深入，考虑边界情况' },
  { value: 5, label: '极致', description: '穷举可能性，最优方案' }
]

const selectedProject = computed(() => {
  return projects.value.find(p => p.id === selectedProjectId.value) || null
})

// 检查权限
const hasPermission = (permission) => {
  return userStore.isAdmin() || userStore.hasPermission(permission)
}

// 获取深度提示
const getDepthTooltip = (depth) => {
  const option = reasoningDepthOptions.find(o => o.value === depth)
  return option ? `${option.label}: ${option.description}` : '点击查看详情'
}

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

/** 设置推理深度 */
const handleReasoningDepthChange = async (agent, newDepth) => {
  try {
    const result = await agentApi.setReasoningDepth(selectedProjectId.value, agent.role, newDepth)
    if (result.success) {
      ElMessage.success(result.message || '推理深度已更新')
      // 更新本地数据
      agent.reasoningDepth = newDepth
    } else {
      ElMessage.error(result.message || '设置失败')
    }
  } catch (error) {
    ElMessage.error('设置推理深度失败')
    // 恢复原值
    loadAgents()
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

.reasoning-depth-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.depth-info {
  color: var(--el-text-color-secondary);
  cursor: pointer;
}

.depth-info:hover {
  color: var(--el-color-primary);
}

.depth-option {
  display: flex;
  justify-content: space-between;
  width: 100%;
}

.depth-desc {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.depth-help-content p {
  margin-bottom: 16px;
  line-height: 1.6;
}

.depth-help-tip {
  margin-top: 16px;
  padding: 12px;
  background: var(--el-color-primary-light-9);
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--el-color-primary);
  font-size: 13px;
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
