<template>
  <div class="monitoring-page">
    <!-- 顶部状态栏 -->
    <div class="status-bar">
      <div class="status-left">
        <span class="page-title">监控中心</span>
        <el-tag :type="systemHealthType" effect="dark" size="large">
          {{ systemHealthText }}
        </el-tag>
      </div>
      <div class="status-right">
        <el-switch v-model="autoRefresh" active-text="自动刷新" inactive-text="" style="margin-right: 16px" />
        <el-button @click="loadAll" :loading="loading">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
      </div>
    </div>

    <!-- KPI 概览卡片 -->
    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
          <el-icon :size="24"><UserFilled /></el-icon>
        </div>
        <div class="kpi-content">
          <div class="kpi-value">{{ agents.alive || 0 }}<span class="kpi-unit">/{{ agents.total || 0 }}</span></div>
          <div class="kpi-label">运行中 Agent</div>
        </div>
        <div class="kpi-trend" v-if="agents.busy > 0">
          <el-tag type="warning" size="small">{{ agents.busy }} 忙碌</el-tag>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)">
          <el-icon :size="24"><Coin /></el-icon>
        </div>
        <div class="kpi-content">
          <div class="kpi-value">{{ memory.usagePercent || 0 }}<span class="kpi-unit">%</span></div>
          <div class="kpi-label">内存使用率</div>
        </div>
        <div class="kpi-trend">
          <span class="kpi-detail">{{ memory.usedMB || 0 }}MB / {{ memory.maxMB || 0 }}MB</span>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)">
          <el-icon :size="24"><Box /></el-icon>
        </div>
        <div class="kpi-content">
          <div class="kpi-value">{{ disk.usagePercent || 0 }}<span class="kpi-unit">%</span></div>
          <div class="kpi-label">磁盘使用率</div>
        </div>
        <div class="kpi-trend">
          <span class="kpi-detail">可用 {{ disk.freeGB || 0 }}GB</span>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-icon" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)">
          <el-icon :size="24"><FolderOpened /></el-icon>
        </div>
        <div class="kpi-content">
          <div class="kpi-value">{{ projects.total || 0 }}</div>
          <div class="kpi-label">项目总数</div>
        </div>
        <div class="kpi-trend">
          <el-tag type="success" size="small" v-if="projects.active > 0">{{ projects.active }} 活跃</el-tag>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-icon" style="background: linear-gradient(135deg, #fa709a 0%, #fee140 100%)">
          <el-icon :size="24"><Warning /></el-icon>
        </div>
        <div class="kpi-content">
          <div class="kpi-value">{{ alerts.unresolved || 0 }}</div>
          <div class="kpi-label">未解决告警</div>
        </div>
        <div class="kpi-trend">
          <el-tag :type="alerts.unresolved > 0 ? 'danger' : 'success'" size="small">
            {{ alerts.unresolved > 0 ? '需要关注' : '全部正常' }}
          </el-tag>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-icon" style="background: linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)">
          <el-icon :size="24"><Connection /></el-icon>
        </div>
        <div class="kpi-content">
          <div class="kpi-value">{{ threads.count || 0 }}</div>
          <div class="kpi-label">活跃线程</div>
        </div>
        <div class="kpi-trend">
          <span class="kpi-detail">峰值 {{ threads.peak || 0 }}</span>
        </div>
      </div>
    </div>

    <!-- 主内容区：两列布局 -->
    <div class="main-grid">
      <!-- 左列：Agent 状态 + 最近事件 -->
      <div class="main-left">
        <!-- Agent 状态列表 -->
        <el-card class="section-card">
          <template #header>
            <div class="section-header">
              <span class="section-title">Agent 状态</span>
              <el-tag size="small">{{ agentList.length }} 个</el-tag>
            </div>
          </template>
          <div class="agent-list" v-if="agentList.length > 0">
            <div v-for="agent in agentList" :key="agent.id" class="agent-row">
              <div class="agent-status-dot" :class="agent.alive ? (agent.busy ? 'busy' : 'alive') : 'dead'" />
              <div class="agent-info">
                <div class="agent-name">{{ agent.name || agent.id }}</div>
                <div class="agent-role">{{ agent.role }}</div>
              </div>
              <div class="agent-tags">
                <el-tag v-if="agent.alive && agent.busy" type="warning" size="small">忙碌</el-tag>
                <el-tag v-else-if="agent.alive" type="success" size="small">运行中</el-tag>
                <el-tag v-else type="info" size="small">已停止</el-tag>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无 Agent" :image-size="60" />
        </el-card>

        <!-- 最近告警 -->
        <el-card class="section-card">
          <template #header>
            <div class="section-header">
              <span class="section-title">最近告警</span>
              <el-button size="small" text type="primary" @click="$router.push('/alerts')">查看全部</el-button>
            </div>
          </template>
          <div class="alert-list" v-if="recentAlerts.length > 0">
            <div v-for="alert in recentAlerts" :key="alert.id" class="alert-row">
              <el-icon :size="16" :class="'alert-icon-' + (alert.severity || 'warning')">
                <Warning />
              </el-icon>
              <div class="alert-content">
                <div class="alert-message">{{ alert.message || alert.title }}</div>
                <div class="alert-time">{{ formatTime(alert.createdAt) }}</div>
              </div>
              <el-tag :type="getAlertType(alert.severity)" size="small">{{ alert.severity || 'warning' }}</el-tag>
            </div>
          </div>
          <el-empty v-else description="暂无告警" :image-size="60" />
        </el-card>
      </div>

      <!-- 右列：系统信息 + 资源趋势 -->
      <div class="main-right">
        <!-- 系统信息 -->
        <el-card class="section-card">
          <template #header>
            <span class="section-title">系统信息</span>
          </template>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="Java 版本">{{ systemInfo.javaVersion || '-' }}</el-descriptions-item>
            <el-descriptions-item label="操作系统">{{ systemInfo.osName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="CPU 核心">{{ systemInfo.processors || '-' }}</el-descriptions-item>
            <el-descriptions-item label="系统负载">{{ systemInfo.loadAverage || '-' }}</el-descriptions-item>
            <el-descriptions-item label="运行时间">{{ systemInfo.uptime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="数据库">
              <el-tag :type="database.status === 'UP' ? 'success' : 'danger'" size="small">
                {{ database.status || '-' }} {{ database.version ? '(' + database.version + ')' : '' }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 内存使用详情 -->
        <el-card class="section-card">
          <template #header>
            <span class="section-title">内存使用</span>
          </template>
          <div class="memory-detail">
            <div class="memory-bar">
              <div class="memory-label">堆内存</div>
              <el-progress
                :percentage="memory.heapPercent || 0"
                :color="getMemoryColor(memory.heapPercent)"
                :stroke-width="16"
              />
              <div class="memory-value">{{ memory.heapUsed || 0 }}MB / {{ memory.heapMax || 0 }}MB</div>
            </div>
            <div class="memory-bar">
              <div class="memory-label">非堆内存</div>
              <el-progress
                :percentage="memory.nonHeapPercent || 0"
                :color="getMemoryColor(memory.nonHeapPercent)"
                :stroke-width="16"
              />
              <div class="memory-value">{{ memory.nonHeapUsed || 0 }}MB</div>
            </div>
            <div class="memory-bar">
              <div class="memory-label">系统内存</div>
              <el-progress
                :percentage="memory.usagePercent || 0"
                :color="getMemoryColor(memory.usagePercent)"
                :stroke-width="16"
              />
              <div class="memory-value">{{ memory.usedMB || 0 }}MB / {{ memory.maxMB || 0 }}MB</div>
            </div>
          </div>
        </el-card>

        <!-- 最近操作日志 -->
        <el-card class="section-card">
          <template #header>
            <div class="section-header">
              <span class="section-title">最近操作</span>
              <el-button size="small" text type="primary" @click="$router.push('/admin/logs')">查看全部</el-button>
            </div>
          </template>
          <div class="log-list" v-if="recentLogs.length > 0">
            <div v-for="log in recentLogs" :key="log.id" class="log-row">
              <div class="log-time">{{ formatTimeShort(log.createdAt) }}</div>
              <div class="log-action">
                <el-tag size="small" type="info">{{ log.action }}</el-tag>
              </div>
              <div class="log-detail">{{ log.target }}{{ log.detail ? ' - ' + log.detail : '' }}</div>
            </div>
          </div>
          <el-empty v-else description="暂无操作记录" :image-size="60" />
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, onDeactivated } from 'vue'
import { monitorApi, healthApi, alertApi, logApi } from '@/api'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const autoRefresh = ref(true)
let refreshTimer = null

// 数据
const agents = ref({ total: 0, alive: 0, busy: 0 })
const agentList = ref([])
const memory = ref({})
const disk = ref({})
const projects = ref({})
const alerts = ref({ unresolved: 0 })
const recentAlerts = ref([])
const threads = ref({})
const systemInfo = ref({})
const database = ref({})
const recentLogs = ref([])

// 健康状态
const systemHealthText = computed(() => {
  if (alerts.value.unresolved > 0) return '有告警'
  if (agents.value.alive === 0 && agents.value.total > 0) return 'Agent 全部停止'
  return '系统正常'
})

const systemHealthType = computed(() => {
  if (alerts.value.unresolved > 0) return 'warning'
  if (agents.value.alive === 0 && agents.value.total > 0) return 'danger'
  return 'success'
})

const getMemoryColor = (percent) => {
  if (percent < 70) return '#67c23a'
  if (percent < 90) return '#e6a23c'
  return '#f56c6c'
}

const getAlertType = (severity) => {
  const map = { critical: 'danger', high: 'danger', warning: 'warning', info: 'info' }
  return map[severity] || 'warning'
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

const formatTimeShort = (time) => {
  if (!time) return '-'
  const d = new Date(time)
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}

/** 加载所有数据 */
const loadAll = async () => {
  loading.value = true
  try {
    await Promise.all([
      loadSystemInfo(),
      loadAgents(),
      loadAlerts(),
      loadLogs()
    ])
  } catch (e) {
    console.error('加载监控数据失败', e)
  } finally {
    loading.value = false
  }
}

const loadSystemInfo = async () => {
  try {
    const data = await monitorApi.getMetrics()
    if (!data) return

    // Agent 统计
    const ag = data.agents || {}
    agents.value = { total: ag.total || 0, alive: ag.alive || 0, busy: ag.busy || 0 }

    // 内存
    const jvm = data.jvm || {}
    const heapUsed = jvm.heapUsedMB || 0
    const heapMax = jvm.heapMaxMB || 0
    const nonHeap = jvm.nonHeapUsedMB || 0
    const totalUsed = jvm.usedMemoryMB || 0
    const totalMax = jvm.maxMemoryMB || 0

    memory.value = {
      usagePercent: totalMax > 0 ? Math.round(totalUsed * 100 / totalMax) : 0,
      usedMB: totalUsed,
      maxMB: totalMax,
      heapPercent: heapMax > 0 ? Math.round(heapUsed * 100 / heapMax) : 0,
      heapUsed,
      heapMax,
      nonHeapUsed: nonHeap,
      nonHeapPercent: nonHeap > 0 ? Math.min(100, Math.round(nonHeap / 256 * 100)) : 0
    }

    // 磁盘
    const d = data.disk || {}
    disk.value = {
      usagePercent: d.usagePercent || 0,
      freeGB: d.freeGB || 0,
      totalGB: d.totalGB || 0
    }

    // 线程
    const t = data.threads || {}
    threads.value = { count: t.threadCount || 0, peak: t.peakThreadCount || 0 }

    // 项目
    const p = data.projects || {}
    projects.value = { total: p.total || 0, active: p.active || 0 }

    // 系统信息
    systemInfo.value = {
      javaVersion: jvm.javaVersion || data.javaVersion || '-',
      osName: data.osName || '-',
      processors: jvm.availableProcessors || data.availableProcessors || '-',
      loadAverage: data.loadAverage || '-',
      uptime: jvm.uptimeFormatted || '-'
    }

    // 数据库
    const db = data.database || {}
    database.value = { status: db.status || '-', version: db.version || '' }

    // Agent 列表
    if (data.agentList) {
      agentList.value = data.agentList
    }
  } catch (e) {
    console.error('加载系统信息失败', e)
  }
}

const loadAgents = async () => {
  try {
    const data = await healthApi.getAll()
    if (data && Array.isArray(data)) {
      agentList.value = data.map(h => ({
        id: h.agentId || h.id,
        name: h.agentName || h.name,
        role: h.agentRole || h.role,
        alive: h.alive !== false,
        busy: h.busy === true
      }))
    }
  } catch (e) {
    // fallback: agentList already loaded from systemInfo
  }
}

const loadAlerts = async () => {
  try {
    const data = await alertApi.getAlerts({ page: 0, size: 10 })
    if (data) {
      recentAlerts.value = data.content || data || []
      alerts.value.unresolved = recentAlerts.value.filter(a => a.status !== 'RESOLVED').length
    }
  } catch (e) {
    console.error('加载告警失败', e)
  }
}

const loadLogs = async () => {
  try {
    const data = await logApi.getAll({ page: 0, size: 10 })
    if (data) {
      recentLogs.value = data.content || data || []
    }
  } catch (e) {
    console.error('加载日志失败', e)
  }
}

const startAutoRefresh = () => {
  stopAutoRefresh()
  refreshTimer = setInterval(() => {
    if (autoRefresh.value) loadAll()
  }, 15000)
}

const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

onDeactivated(() => {
  stopAutoRefresh()
})

onMounted(() => {
  loadAll()
  startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style scoped>
.monitoring-page {
  padding: 20px;
  background: #f5f7fa;
  min-height: calc(100vh - 120px);
}

.status-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.status-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
}

.status-right {
  display: flex;
  align-items: center;
}

/* KPI 卡片 - 3列布局，大屏6个卡片分2行 */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.kpi-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: transform 0.2s, box-shadow 0.2s;
}

.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.kpi-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.kpi-content {
  flex: 1;
  min-width: 0;
}

.kpi-value {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a2e;
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
}

.kpi-unit {
  font-size: 14px;
  font-weight: 400;
  color: #999;
}

.kpi-label {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.kpi-trend {
  flex-shrink: 0;
}

.kpi-detail {
  font-size: 11px;
  color: #999;
}

/* 主内容区 */
.main-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.main-left, .main-right {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section-card {
  border: none;
}

.section-card :deep(.el-card__header) {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.section-card :deep(.el-card__body) {
  padding: 16px 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
}

/* Agent 列表 */
.agent-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 300px;
  overflow-y: auto;
}

.agent-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #fafafa;
  transition: background 0.2s;
}

.agent-row:hover {
  background: #f0f2f5;
}

.agent-status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.agent-status-dot.alive {
  background: #67c23a;
  box-shadow: 0 0 6px rgba(103, 194, 58, 0.4);
}

.agent-status-dot.busy {
  background: #e6a23c;
  box-shadow: 0 0 6px rgba(230, 162, 60, 0.4);
  animation: pulse 2s ease-in-out infinite;
}

.agent-status-dot.dead {
  background: #c0c4cc;
}

.agent-info {
  flex: 1;
  min-width: 0;
}

.agent-name {
  font-size: 13px;
  font-weight: 500;
  color: #333;
}

.agent-role {
  font-size: 11px;
  color: #999;
}

/* 告警列表 */
.alert-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 250px;
  overflow-y: auto;
}

.alert-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #fafafa;
}

.alert-icon-warning { color: #e6a23c; }
.alert-icon-critical { color: #f56c6c; }
.alert-icon-high { color: #f56c6c; }
.alert-icon-info { color: #909399; }

.alert-content {
  flex: 1;
  min-width: 0;
}

.alert-message {
  font-size: 13px;
  color: #333;
  line-height: 1.4;
}

.alert-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}

/* 内存详情 */
.memory-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.memory-bar {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.memory-label {
  font-size: 12px;
  color: #666;
  font-weight: 500;
}

.memory-value {
  font-size: 11px;
  color: #999;
  font-variant-numeric: tabular-nums;
}

/* 操作日志 */
.log-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 250px;
  overflow-y: auto;
}

.log-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 0;
  font-size: 12px;
  border-bottom: 1px solid #f5f5f5;
}

.log-row:last-child {
  border-bottom: none;
}

.log-time {
  color: #999;
  font-family: 'JetBrains Mono', 'SF Mono', monospace;
  font-variant-numeric: tabular-nums;
  flex-shrink: 0;
  width: 48px;
}

.log-action {
  flex-shrink: 0;
}

.log-detail {
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

/* 响应式 */
@media (max-width: 1024px) {
  .main-grid {
    grid-template-columns: 1fr;
  }

  .kpi-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  }
}

@media (max-width: 767px) {
  .monitoring-page {
    padding: 12px;
  }

  .status-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .kpi-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .kpi-card {
    padding: 14px;
  }

  .kpi-value {
    font-size: 22px;
  }
}
</style>
