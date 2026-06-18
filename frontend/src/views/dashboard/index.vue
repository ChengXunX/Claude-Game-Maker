<template>
  <div class="dashboard">
    <!-- 顶部快捷操作栏 -->
    <div class="quick-actions">
      <el-button type="primary" @click="$router.push('/projects')">
        <el-icon><Plus /></el-icon> 创建项目
      </el-button>
      <el-button @click="$router.push('/agents')">
        <el-icon><User /></el-icon> 管理 Agent
      </el-button>
      <el-button @click="$router.push('/scheduler')">
        <el-icon><Calendar /></el-icon> 调度中心
      </el-button>
      <el-button @click="$router.push('/monitoring')">
        <el-icon><Monitor /></el-icon> 监控中心
      </el-button>
      <el-button @click="$router.push('/ai-assistant')">
        <el-icon><ChatDotRound /></el-icon> AI 助手
      </el-button>
    </div>

    <!-- KPI 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :xs="12" :sm="12" :md="6" :lg="4">
        <el-card class="stat-card" shadow="hover" @click="$router.push('/agents')">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
              <el-icon :size="28"><UserFilled /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.agentCount }}</div>
              <div class="stat-label">Agent 总数</div>
            </div>
          </div>
          <div class="stat-extra">
            <span class="stat-running">{{ stats.runningAgents }} 运行中</span>
            <span class="stat-busy">{{ stats.busyAgents }} 忙碌</span>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="12" :sm="12" :md="6" :lg="4">
        <el-card class="stat-card" shadow="hover" @click="$router.push('/projects')">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)">
              <el-icon :size="28"><FolderOpened /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.projectCount }}</div>
              <div class="stat-label">项目总数</div>
            </div>
          </div>
          <div class="stat-extra">
            <span class="stat-running">{{ stats.activeProjects }} 活跃</span>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="12" :sm="12" :md="6" :lg="4">
        <el-card class="stat-card" shadow="hover" @click="$router.push('/approvals')">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #fa709a 0%, #fee140 100%)">
              <el-icon :size="28"><Bell /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.pendingApprovals }}</div>
              <div class="stat-label">待审批</div>
            </div>
          </div>
          <div class="stat-extra">
            <span class="stat-muted">&nbsp;</span>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="12" :sm="12" :md="6" :lg="4">
        <el-card class="stat-card" shadow="hover" @click="$router.push('/notifications')">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)">
              <el-icon :size="28"><Warning /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.unresolvedAlerts }}</div>
              <div class="stat-label">未解决告警</div>
            </div>
          </div>
          <div class="stat-extra">
            <span class="stat-muted">&nbsp;</span>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="12" :sm="12" :md="6" :lg="4">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)">
              <el-icon :size="28"><Finished /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.completedTasks }}</div>
              <div class="stat-label">已完成任务</div>
            </div>
          </div>
          <div class="stat-extra">
            <span class="stat-muted">&nbsp;</span>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="12" :sm="12" :md="6" :lg="4">
        <el-card class="stat-card" shadow="hover" @click="$router.push('/health')">
          <div class="stat-content">
            <div class="stat-icon" :style="{ background: systemHealthIcon === 'CircleCheck' ? 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)' : 'linear-gradient(135deg, #f5576c 0%, #ff6b6b 100%)' }">
              <el-icon :size="28">
                <CircleCheck v-if="systemHealthIcon === 'CircleCheck'" />
                <Warning v-else />
              </el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value" :class="{ 'text-danger': systemHealthIcon !== 'CircleCheck' }">{{ stats.systemHealth }}</div>
              <div class="stat-label">系统状态</div>
            </div>
          </div>
          <div class="stat-extra">
            <span class="stat-muted">&nbsp;</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 主要内容区 -->
    <el-row :gutter="16" class="content-row">
      <!-- Agent 状态列表 -->
      <el-col :xs="24" :sm="24" :md="14" :lg="14">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>Agent 状态</span>
              <el-button type="primary" size="small" @click="refreshData">
                <el-icon><Refresh /></el-icon> 刷新
              </el-button>
            </div>
          </template>

          <el-table :data="agents" style="width: 100%" size="small">
            <el-table-column prop="name" label="名称" min-width="80" show-overflow-tooltip />
            <el-table-column prop="role" label="角色" min-width="80" show-overflow-tooltip />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.alive ? 'success' : 'danger'" size="small">
                  {{ row.alive ? '运行中' : '已停止' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="忙碌" width="60">
              <template #default="{ row }">
                <el-tag :type="row.busy ? 'warning' : 'info'" size="small">
                  {{ row.busy ? '是' : '否' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="taskCount" label="任务" width="60" />
            <el-table-column label="操作" width="120" fixed="right">
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

      <!-- 右侧面板 -->
      <el-col :xs="24" :sm="24" :md="10" :lg="10">
        <!-- 待审批事项 -->
        <el-card class="mb-16">
          <template #header>
            <div class="card-header">
              <span>待审批事项</span>
              <el-badge :value="pendingApprovals.length" :max="99" v-if="pendingApprovals.length > 0">
                <el-button size="small" link @click="$router.push('/approvals')">查看全部</el-button>
              </el-badge>
            </div>
          </template>

          <div v-if="pendingApprovals.length === 0" class="empty-text">
            暂无待审批事项
          </div>

          <div v-else class="approval-list">
            <div
              v-for="item in pendingApprovals.slice(0, 5)"
              :key="item.id"
              class="approval-item"
              @click="$router.push('/approvals')"
            >
              <div class="approval-type">
                <el-tag size="small" :type="getApprovalType(item.type)">{{ item.type }}</el-tag>
              </div>
              <div class="approval-desc">{{ item.description || item.title }}</div>
              <div class="approval-time">{{ formatTime(item.createdAt) }}</div>
            </div>
          </div>
        </el-card>

        <!-- 最近告警 -->
        <el-card class="mb-16">
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

        <!-- 最近活动 -->
        <el-card>
          <template #header>
            <span>最近活动</span>
          </template>

          <div v-if="recentLogs.length === 0" class="empty-text">
            暂无活动记录
          </div>

          <div v-else class="log-list">
            <div
              v-for="log in recentLogs.slice(0, 8)"
              :key="log.id"
              class="log-item"
            >
              <div class="log-action">{{ log.action }}</div>
              <div class="log-detail">{{ log.detail || log.username }}</div>
              <div class="log-time">{{ formatTime(log.createdAt) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { agentApi, alertApi, approvalApi, logApi, healthApi, projectApi } from '@/api'

const router = useRouter()

// 统计数据
const stats = ref({
  agentCount: 0,
  runningAgents: 0,
  busyAgents: 0,
  projectCount: 0,
  activeProjects: 0,
  pendingApprovals: 0,
  unresolvedAlerts: 0,
  completedTasks: 0,
  systemHealth: '正常'
})

// Agent列表
const agents = ref([])

// 最近告警
const recentAlerts = ref([])

// 待审批事项
const pendingApprovals = ref([])

// 最近活动日志
const recentLogs = ref([])

// 系统健康状态图标
const systemHealthIcon = computed(() => {
  return stats.value.systemHealth === '正常' ? 'CircleCheck' : 'Warning'
})

// 获取Agent列表
const fetchAgents = async () => {
  try {
    const data = await agentApi.getAll()
    agents.value = data || []
    stats.value.agentCount = data.length
    stats.value.runningAgents = data.filter(a => a.alive).length
    stats.value.busyAgents = data.filter(a => a.busy).length
  } catch (error) {
    console.error('获取Agent列表失败:', error)
  }
}

// 获取项目列表
const fetchProjects = async () => {
  try {
    const data = await projectApi.getAll()
    const projects = data || []
    stats.value.projectCount = projects.length
    stats.value.activeProjects = projects.filter(p => p.status === 'ACTIVE').length
    // 从所有项目的里程碑任务中统计已完成任务数
    let completed = 0
    for (const p of projects) {
      if (p.milestones) {
        for (const m of p.milestones) {
          if (m.tasks) {
            completed += m.tasks.filter(t => t.status === 'COMPLETED').length
          }
        }
      }
    }
    stats.value.completedTasks = completed
  } catch (error) {
    console.error('获取项目列表失败:', error)
  }
}

// 获取告警列表
const fetchAlerts = async () => {
  try {
    const data = await alertApi.getAlerts({ page: 0, size: 5 })
    recentAlerts.value = data.content || data || []
    stats.value.unresolvedAlerts = recentAlerts.value.filter(a => a.status !== 'RESOLVED').length
  } catch (error) {
    console.error('获取告警列表失败:', error)
  }
}

// 获取待审批事项
const fetchApprovals = async () => {
  try {
    const data = await approvalApi.getPending()
    pendingApprovals.value = data || []
    stats.value.pendingApprovals = pendingApprovals.value.length
  } catch (error) {
    console.error('获取待审批事项失败:', error)
  }
}

// 获取最近活动日志
const fetchLogs = async () => {
  try {
    const data = await logApi.getAll({ page: 0, size: 10 })
    recentLogs.value = data.content || data || []
  } catch (error) {
    console.error('获取活动日志失败:', error)
  }
}

// 获取系统健康状态
const fetchHealth = async () => {
  try {
    const data = await healthApi.getStats()
    if (data) {
      stats.value.systemHealth = data.status === 'UP' ? '正常' : '异常'
    }
  } catch (error) {
    // 静默处理
  }
}

// 刷新数据
const refreshData = () => {
  fetchAgents()
  fetchProjects()
  fetchAlerts()
  fetchApprovals()
  fetchLogs()
  fetchHealth()
  ElMessage.success('数据已刷新')
}

// 查看Agent详情
const viewAgent = (agent) => {
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
  const types = { CRITICAL: 'danger', HIGH: 'warning', MEDIUM: '', LOW: 'info' }
  return types[priority] || 'info'
}

// 获取审批类型样式
const getApprovalType = (type) => {
  const types = { dismiss: 'danger', email_change: 'warning', recruitment: 'primary' }
  return types[type] || 'info'
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

.quick-actions {
  margin-bottom: 16px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
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
  gap: 12px;
}

.stat-icon {
  width: 48px;
  height: 48px;
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
  font-size: 24px;
  font-weight: bold;
  color: #333;
}

.stat-label {
  font-size: 12px;
  color: #666;
  margin-top: 2px;
}

.stat-extra {
  margin-top: 8px;
  font-size: 12px;
  color: #999;
  display: flex;
  gap: 8px;
}

.stat-running { color: #67c23a; }
.stat-busy { color: #e6a23c; }
.stat-muted { color: transparent; }
.text-danger { color: #f56c6c; }

.content-row {
  margin-top: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.mb-16 { margin-bottom: 16px; }

.empty-text {
  text-align: center;
  color: #999;
  padding: 30px 0;
}

/* 审批列表 */
.approval-list { max-height: 200px; overflow-y: auto; }
.approval-item {
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
}
.approval-item:hover { background: #f5f7fa; }
.approval-item:last-child { border-bottom: none; }
.approval-type { margin-bottom: 4px; }
.approval-desc { font-size: 13px; color: #333; }
.approval-time { font-size: 11px; color: #999; margin-top: 2px; }

/* 告警列表 */
.alert-list { max-height: 250px; overflow-y: auto; }
.alert-item { padding: 10px 0; border-bottom: 1px solid #f0f0f0; }
.alert-item:last-child { border-bottom: none; }
.alert-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.alert-time { font-size: 11px; color: #999; }
.alert-title { font-size: 13px; color: #333; word-break: break-word; }

/* 日志列表 */
.log-list { max-height: 250px; overflow-y: auto; }
.log-item { padding: 6px 0; border-bottom: 1px solid #f5f5f5; }
.log-item:last-child { border-bottom: none; }
.log-action { font-size: 13px; color: #333; font-weight: 500; }
.log-detail { font-size: 12px; color: #666; }
.log-time { font-size: 11px; color: #999; }

@media (max-width: 767px) {
  .stat-icon { width: 40px; height: 40px; }
  .stat-value { font-size: 18px; }
  .quick-actions { gap: 4px; }
}
</style>
