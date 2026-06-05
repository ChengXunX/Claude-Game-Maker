<template>
  <div class="dashboard">
    <h2>仪表盘</h2>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :xs="12" :sm="12" :md="6" :lg="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: #1890ff">
              <el-icon :size="32"><UserFilled /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.agentCount }}</div>
              <div class="stat-label">Agent数量</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="12" :sm="12" :md="6" :lg="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: #52c41a">
              <el-icon :size="32"><Folder /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.projectCount }}</div>
              <div class="stat-label">项目数量</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="12" :sm="12" :md="6" :lg="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: #faad14">
              <el-icon :size="32"><Bell /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.alertCount }}</div>
              <div class="stat-label">待处理告警</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="12" :sm="12" :md="6" :lg="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: #722ed1">
              <el-icon :size="32"><Finished /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.taskCount }}</div>
              <div class="stat-label">完成任务</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Agent状态列表 -->
    <el-row :gutter="16" class="content-row">
      <el-col :xs="24" :sm="24" :md="16" :lg="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>Agent状态</span>
              <el-button type="primary" size="small" @click="refreshData">
                <el-icon><Refresh /></el-icon> 刷新
              </el-button>
            </div>
          </template>

          <el-table :data="agents" style="width: 100%">
            <el-table-column prop="name" label="名称" min-width="100" show-overflow-tooltip />
            <el-table-column prop="role" label="角色" min-width="100" show-overflow-tooltip />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.alive ? 'success' : 'danger'">
                  {{ row.alive ? '运行中' : '已停止' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="忙碌" width="80">
              <template #default="{ row }">
                <el-tag :type="row.busy ? 'warning' : 'info'">
                  {{ row.busy ? '是' : '否' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="taskCount" label="任务数" width="80" />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" link @click="viewAgent(row)">
                  查看
                </el-button>
                <el-button
                  :type="row.alive ? 'danger' : 'success'"
                  size="small"
                  link
                  @click="toggleAgent(row)"
                >
                  {{ row.alive ? '停止' : '启动' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :md="8" :lg="8">
        <el-card>
          <template #header>
            <span>最近告警</span>
          </template>

          <div v-if="recentAlerts.length === 0" class="empty-text">
            暂无告警
          </div>

          <div v-else class="alert-list">
            <div
              v-for="alert in recentAlerts"
              :key="alert.id"
              class="alert-item"
            >
              <div class="alert-header">
                <el-tag :type="getAlertType(alert.priority)" size="small">
                  {{ alert.priority }}
                </el-tag>
                <span class="alert-time">{{ formatTime(alert.createdAt) }}</span>
              </div>
              <div class="alert-title">{{ alert.title }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { agentApi, alertApi } from '@/api'

const router = useRouter()

// 统计数据
const stats = ref({
  agentCount: 0,
  projectCount: 0,
  alertCount: 0,
  taskCount: 0
})

// Agent列表
const agents = ref([])

// 最近告警
const recentAlerts = ref([])

// 获取Agent列表
const fetchAgents = async () => {
  try {
    const data = await agentApi.getAll()
    agents.value = data
    stats.value.agentCount = data.length
  } catch (error) {
    console.error('获取Agent列表失败:', error)
  }
}

// 获取告警列表
const fetchAlerts = async () => {
  try {
    const data = await alertApi.getAlerts({ page: 0, size: 5 })
    recentAlerts.value = data.content || []
    stats.value.alertCount = data.totalElements || 0
  } catch (error) {
    console.error('获取告警列表失败:', error)
  }
}

// 刷新数据
const refreshData = () => {
  fetchAgents()
  fetchAlerts()
  ElMessage.success('数据已刷新')
}

// 查看Agent详情
const viewAgent = (agent) => {
  // agent.id 格式为 projectId:agentRole，需要拆分
  const lastColon = agent.id.lastIndexOf(':')
  if (lastColon > 0) {
    const projectId = agent.id.substring(0, lastColon)
    const agentRole = agent.id.substring(lastColon + 1)
    router.push(`/agents/${projectId}/${agentRole}`)
  } else {
    router.push('/agents')
  }
}

// 切换Agent状态
const toggleAgent = async (agent) => {
  try {
    if (agent.alive) {
      await agentApi.stop(agent.id)
      ElMessage.success(`Agent ${agent.name} 已停止`)
    } else {
      await agentApi.start(agent.id)
      ElMessage.success(`Agent ${agent.name} 已启动`)
    }
    refreshData()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 获取告警类型
const getAlertType = (priority) => {
  const types = {
    CRITICAL: 'danger',
    HIGH: 'warning',
    MEDIUM: '',
    LOW: 'info'
  }
  return types[priority] || 'info'
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN')
}

// 组件挂载时获取数据
onMounted(() => {
  refreshData()
})
</script>

<style scoped>
.dashboard {
  padding: 0;
}

.stats-row {
  margin-bottom: 16px;
}

.stat-card {
  cursor: pointer;
  margin-bottom: 12px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.stat-info {
  min-width: 0;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #333;
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-top: 4px;
}

.content-row {
  margin-top: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.empty-text {
  text-align: center;
  color: #999;
  padding: 40px 0;
}

.alert-list {
  max-height: 400px;
  overflow-y: auto;
}

.alert-item {
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.alert-item:last-child {
  border-bottom: none;
}

.alert-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  flex-wrap: wrap;
  gap: 4px;
}

.alert-time {
  font-size: 12px;
  color: #999;
}

.alert-title {
  font-size: 14px;
  color: #333;
  word-break: break-word;
}

/* 平板端 */
@media (max-width: 991px) {
  .stat-icon {
    width: 48px;
    height: 48px;
  }

  .stat-value {
    font-size: 22px;
  }

  .stat-label {
    font-size: 12px;
  }
}

/* 手机端 */
@media (max-width: 767px) {
  .stat-content {
    flex-direction: column;
    text-align: center;
    gap: 8px;
  }

  .stat-icon {
    width: 40px;
    height: 40px;
  }

  .stat-icon .el-icon {
    font-size: 24px !important;
  }

  .stat-value {
    font-size: 20px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
