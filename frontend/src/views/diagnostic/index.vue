<template>
  <div class="diagnostic-page">
    <!-- 顶部控制栏 -->
    <div class="control-bar">
      <div class="control-left">
        <span class="page-title">系统自检</span>
        <el-tag :type="overallStatusType" effect="dark" size="large">
          {{ overallStatusText }}
        </el-tag>
      </div>
      <div class="control-right">
        <el-switch v-model="autoRefresh" active-text="自动刷新" inactive-text="" style="margin-right: 16px" />
        <el-button type="primary" @click="runDiagnostic" :loading="running">
          <el-icon><Refresh /></el-icon> 执行自检
        </el-button>
      </div>
    </div>

    <!-- 运行时间 -->
    <div class="uptime-bar" v-if="result?.checks?.cpu">
      <el-icon><Timer /></el-icon>
      <span>系统运行时间：{{ result.checks.cpu.uptimeFormatted || '-' }}</span>
      <span class="timestamp">最近检查：{{ formatTime(result.timestamp) }}</span>
    </div>

    <!-- 指标卡片网格 -->
    <div class="metrics-grid" v-if="result?.checks">
      <!-- CPU 卡片 -->
      <div class="metric-card" :class="{ expanded: expandedCard === 'cpu' }" @click="toggleCard('cpu')">
        <div class="card-main">
          <div class="card-icon cpu">
            <el-icon :size="24"><Cpu /></el-icon>
          </div>
          <div class="card-info">
            <div class="card-title">CPU 使用率</div>
            <div class="card-value">{{ result.checks.cpu?.systemCpuPercent || 0 }}%</div>
          </div>
          <el-progress
            type="dashboard"
            :percentage="result.checks.cpu?.systemCpuPercent || 0"
            :color="getCpuColor(result.checks.cpu?.systemCpuPercent)"
            :width="60"
            :stroke-width="8"
          />
        </div>
        <div class="card-details" v-if="expandedCard === 'cpu'">
          <div class="detail-row">
            <span>进程 CPU</span>
            <span>{{ result.checks.cpu?.processCpuPercent || 0 }}%</span>
          </div>
          <div class="detail-row">
            <span>系统负载</span>
            <span>{{ result.checks.cpu?.systemLoadAverage || '-' }}</span>
          </div>
          <div class="detail-row">
            <span>CPU 核心</span>
            <span>{{ result.checks.cpu?.availableProcessors || '-' }}</span>
          </div>
          <el-button type="primary" size="small" @click.stop="openDetail('cpu')" class="detail-btn">
            <el-icon><View /></el-icon> 查看详情
          </el-button>
        </div>
      </div>

      <!-- 内存卡片 -->
      <div class="metric-card" :class="{ expanded: expandedCard === 'memory' }" @click="toggleCard('memory')">
        <div class="card-main">
          <div class="card-icon memory">
            <el-icon :size="24"><Coin /></el-icon>
          </div>
          <div class="card-info">
            <div class="card-title">内存使用</div>
            <div class="card-value">{{ result.checks.memory?.usagePercent || 0 }}%</div>
          </div>
          <el-progress
            type="dashboard"
            :percentage="result.checks.memory?.usagePercent || 0"
            :color="getMemoryColor(result.checks.memory?.usagePercent)"
            :width="60"
            :stroke-width="8"
          />
        </div>
        <div class="card-details" v-if="expandedCard === 'memory'">
          <div class="detail-row">
            <span>已用内存</span>
            <span>{{ result.checks.memory?.usedMemoryMB || 0 }}MB</span>
          </div>
          <div class="detail-row">
            <span>最大内存</span>
            <span>{{ result.checks.memory?.maxMemoryMB || 0 }}MB</span>
          </div>
          <div class="detail-row">
            <span>堆内存</span>
            <span>{{ result.checks.memory?.heapUsedMB || 0 }}MB / {{ result.checks.memory?.heapMaxMB || 0 }}MB</span>
          </div>
          <el-progress :percentage="result.checks.memory?.usagePercent || 0" :color="getMemoryColor(result.checks.memory?.usagePercent)" :stroke-width="10" />
          <el-button type="primary" size="small" @click.stop="openDetail('memory')" class="detail-btn">
            <el-icon><View /></el-icon> 查看详情
          </el-button>
        </div>
      </div>

      <!-- 磁盘卡片 -->
      <div class="metric-card" :class="{ expanded: expandedCard === 'disk' }" @click="toggleCard('disk')">
        <div class="card-main">
          <div class="card-icon disk">
            <el-icon :size="24"><Box /></el-icon>
          </div>
          <div class="card-info">
            <div class="card-title">磁盘空间</div>
            <div class="card-value">{{ result.checks.disk?.usagePercent || 0 }}%</div>
          </div>
          <el-progress
            type="dashboard"
            :percentage="result.checks.disk?.usagePercent || 0"
            :color="getDiskColor(result.checks.disk?.usagePercent)"
            :width="60"
            :stroke-width="8"
          />
        </div>
        <div class="card-details" v-if="expandedCard === 'disk'">
          <div class="detail-row">
            <span>可用空间</span>
            <span>{{ result.checks.disk?.freeSpaceGB || 0 }}GB</span>
          </div>
          <div class="detail-row">
            <span>总空间</span>
            <span>{{ result.checks.disk?.totalSpaceGB || 0 }}GB</span>
          </div>
          <el-progress :percentage="result.checks.disk?.usagePercent || 0" :color="getDiskColor(result.checks.disk?.usagePercent)" :stroke-width="10" />
          <el-button type="primary" size="small" @click.stop="openDetail('disk')" class="detail-btn">
            <el-icon><View /></el-icon> 查看详情
          </el-button>
        </div>
      </div>

      <!-- 线程卡片 -->
      <div class="metric-card" :class="{ expanded: expandedCard === 'threads' }" @click="toggleCard('threads')">
        <div class="card-main">
          <div class="card-icon threads">
            <el-icon :size="24"><Connection /></el-icon>
          </div>
          <div class="card-info">
            <div class="card-title">活跃线程</div>
            <div class="card-value">{{ result.checks.threads?.threadCount || 0 }}</div>
          </div>
          <div class="card-badge">
            <span>峰值 {{ result.checks.threads?.peakThreadCount || 0 }}</span>
          </div>
        </div>
        <div class="card-details" v-if="expandedCard === 'threads'">
          <div class="detail-row">
            <span>守护线程</span>
            <span>{{ result.checks.threads?.daemonThreadCount || 0 }}</span>
          </div>
          <div class="detail-row">
            <span>总启动线程</span>
            <span>{{ result.checks.threads?.totalStartedThreadCount || 0 }}</span>
          </div>
          <el-button type="primary" size="small" @click.stop="openDetail('threads')" class="detail-btn">
            <el-icon><View /></el-icon> 查看详情
          </el-button>
        </div>
      </div>

      <!-- 数据库卡片 -->
      <div class="metric-card" :class="{ expanded: expandedCard === 'database' }" @click="toggleCard('database')">
        <div class="card-main">
          <div class="card-icon database">
            <el-icon :size="24"><Coin /></el-icon>
          </div>
          <div class="card-info">
            <div class="card-title">数据库</div>
            <div class="card-value">{{ result.checks.database?.database || '-' }}</div>
          </div>
          <el-tag :type="result.checks.database?.status === 'UP' ? 'success' : 'danger'" size="large">
            {{ result.checks.database?.status }}
          </el-tag>
        </div>
        <div class="card-details" v-if="expandedCard === 'database'">
          <div class="detail-row">
            <span>版本</span>
            <span>{{ result.checks.database?.version || '-' }}</span>
          </div>
          <div class="detail-row">
            <span>连接测试</span>
            <el-tag :type="result.checks.database?.queryTest === 'PASS' ? 'success' : 'danger'" size="small">
              {{ result.checks.database?.queryTest }}
            </el-tag>
          </div>
        </div>
      </div>

      <!-- Agent 卡片 -->
      <div class="metric-card" :class="{ expanded: expandedCard === 'agents' }" @click="toggleCard('agents')">
        <div class="card-main">
          <div class="card-icon agents">
            <el-icon :size="24"><UserFilled /></el-icon>
          </div>
          <div class="card-info">
            <div class="card-title">Agent</div>
            <div class="card-value">{{ result.checks.agents?.aliveAgents || 0 }} / {{ result.checks.agents?.totalAgents || 0 }}</div>
          </div>
          <div class="card-badge">
            <span>运行中</span>
          </div>
        </div>
        <div class="card-details" v-if="expandedCard === 'agents'">
          <div class="detail-row">
            <span>忙碌</span>
            <el-tag type="warning" size="small">{{ result.checks.agents?.busyAgents || 0 }}</el-tag>
          </div>
          <div class="detail-row">
            <span>空闲</span>
            <el-tag type="success" size="small">{{ result.checks.agents?.idleAgents || 0 }}</el-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- 无数据提示 -->
    <el-empty v-if="!result && !loading" description="暂无自检数据，点击执行自检" />
  </div>

  <!-- CPU 详情对话框 -->
  <el-dialog v-model="cpuDialogVisible" title="CPU 详细信息" width="900px">
    <div v-if="cpuDetails" class="detail-content">
      <el-descriptions :column="2" border class="mb-4">
        <el-descriptions-item label="操作系统">{{ cpuDetails.osName }} {{ cpuDetails.osArch }}</el-descriptions-item>
        <el-descriptions-item label="系统版本">{{ cpuDetails.osVersion }}</el-descriptions-item>
        <el-descriptions-item label="CPU 核心">{{ cpuDetails.availableProcessors }}</el-descriptions-item>
        <el-descriptions-item label="系统负载">{{ cpuDetails.systemLoadAverage }}</el-descriptions-item>
        <el-descriptions-item label="系统 CPU">
          <el-progress :percentage="cpuDetails.systemCpuLoad" :color="getCpuColor(cpuDetails.systemCpuLoad)" :stroke-width="10" style="width: 120px; display: inline-block" />
          <span style="margin-left: 4px">{{ cpuDetails.systemCpuLoad }}%</span>
        </el-descriptions-item>
        <el-descriptions-item label="进程 CPU">
          <el-progress :percentage="cpuDetails.processCpuLoad" :color="getCpuColor(cpuDetails.processCpuLoad)" :stroke-width="10" style="width: 120px; display: inline-block" />
          <span style="margin-left: 4px">{{ cpuDetails.processCpuLoad }}%</span>
        </el-descriptions-item>
        <el-descriptions-item label="进程 PID">{{ cpuDetails.pid }}</el-descriptions-item>
        <el-descriptions-item label="CPU 时间">{{ cpuDetails.processCpuTime }}ms</el-descriptions-item>
        <el-descriptions-item label="运行时间">{{ cpuDetails.uptimeFormatted }}</el-descriptions-item>
        <el-descriptions-item label="虚拟内存">{{ cpuDetails.committedVirtualMemory }}MB</el-descriptions-item>
      </el-descriptions>

      <div class="detail-section" v-if="cpuDetails.env">
        <h4>系统环境</h4>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="Java 版本">{{ cpuDetails.javaVersion }}</el-descriptions-item>
          <el-descriptions-item label="JVM">{{ cpuDetails.jvmName }} {{ cpuDetails.jvmVersion }}</el-descriptions-item>
          <el-descriptions-item label="运行用户">{{ cpuDetails.env.user }}</el-descriptions-item>
          <el-descriptions-item label="工作目录">{{ cpuDetails.env.userDir }}</el-descriptions-item>
          <el-descriptions-item label="临时目录">{{ cpuDetails.env.tempDir }}</el-descriptions-item>
          <el-descriptions-item label="文件编码">{{ cpuDetails.env.fileEncoding }}</el-descriptions-item>
          <el-descriptions-item label="物理内存">{{ cpuDetails.env.freePhysicalMemoryMB }}MB / {{ cpuDetails.env.availableMemoryMB }}MB</el-descriptions-item>
          <el-descriptions-item label="交换空间">{{ cpuDetails.env.freeSwapSpaceMB }}MB / {{ cpuDetails.env.totalSwapSpaceMB }}MB</el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="detail-section" v-if="cpuDetails.topCpuThreads?.length">
        <h4>CPU 占用 TOP {{ cpuDetails.topCpuThreads.length }} 线程</h4>
        <el-table :data="cpuDetails.topCpuThreads" stripe size="small" max-height="300">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="name" label="线程名" min-width="180" show-overflow-tooltip />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStateType(row.state)" size="small">{{ row.state }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="CPU 耗时" width="100">
            <template #default="{ row }">{{ row.cpuTimeMs }}ms</template>
          </el-table-column>
          <el-table-column label="栈顶方法" min-width="250" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="stack-trace">{{ row.stackTop || '-' }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="detail-section" v-if="cpuDetails.jvmArgs?.length">
        <h4>JVM 启动参数</h4>
        <div class="param-list">
          <div v-for="(arg, i) in cpuDetails.jvmArgs" :key="i" class="param-item">{{ arg }}</div>
        </div>
      </div>
    </div>
  </el-dialog>

  <!-- 内存详情对话框 -->
  <el-dialog v-model="memoryDialogVisible" title="内存详细信息" width="800px">
    <div v-if="memoryDetails" class="detail-content">
      <el-descriptions :column="2" border class="mb-4">
        <el-descriptions-item label="堆内存已用">{{ memoryDetails.heap?.used }}MB</el-descriptions-item>
        <el-descriptions-item label="堆内存已分配">{{ memoryDetails.heap?.committed }}MB</el-descriptions-item>
        <el-descriptions-item label="堆内存最大">{{ memoryDetails.heap?.max }}MB</el-descriptions-item>
        <el-descriptions-item label="非堆内存已用">{{ memoryDetails.nonHeap?.used }}MB</el-descriptions-item>
      </el-descriptions>
      <div class="detail-section" v-if="memoryDetails.pools?.length">
        <h4>内存池详情</h4>
        <el-table :data="memoryDetails.pools" stripe size="small">
          <el-table-column prop="name" label="名称" min-width="200" show-overflow-tooltip />
          <el-table-column prop="type" label="类型" width="80" />
          <el-table-column label="已用" width="80">
            <template #default="{ row }">{{ row.used }}MB</template>
          </el-table-column>
          <el-table-column label="已分配" width="80">
            <template #default="{ row }">{{ row.committed }}MB</template>
          </el-table-column>
          <el-table-column label="最大" width="80">
            <template #default="{ row }">{{ row.max > 0 ? row.max + 'MB' : '-' }}</template>
          </el-table-column>
          <el-table-column label="使用率" width="100">
            <template #default="{ row }">
              <el-progress v-if="row.usagePercent != null" :percentage="row.usagePercent" :stroke-width="6" :show-text="false" style="width: 60px; display: inline-block" />
              <span style="margin-left: 4px">{{ row.usagePercent != null ? row.usagePercent + '%' : '-' }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="detail-section" v-if="memoryDetails.gc?.length">
        <h4>GC 信息</h4>
        <el-table :data="memoryDetails.gc" stripe size="small">
          <el-table-column prop="name" label="GC 名称" min-width="200" />
          <el-table-column prop="collectionCount" label="回收次数" width="100" />
          <el-table-column label="回收耗时" width="120">
            <template #default="{ row }">{{ row.collectionTime }}ms</template>
          </el-table-column>
          <el-table-column label="内存池" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">{{ row.memoryPoolNames?.join(', ') }}</template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </el-dialog>

  <!-- 线程详情对话框 -->
  <el-dialog v-model="threadDialogVisible" title="线程详细信息" width="900px">
    <div v-if="threadDetails" class="detail-content">
      <el-descriptions :column="4" border class="mb-4">
        <el-descriptions-item label="总线程">{{ threadDetails.threadCount }}</el-descriptions-item>
        <el-descriptions-item label="守护线程">{{ threadDetails.daemonThreadCount }}</el-descriptions-item>
        <el-descriptions-item label="峰值线程">{{ threadDetails.peakThreadCount }}</el-descriptions-item>
        <el-descriptions-item label="已启动线程">{{ threadDetails.totalStartedThreadCount }}</el-descriptions-item>
      </el-descriptions>
      <div class="detail-section" v-if="threadDetails.stateDistribution">
        <h4>线程状态分布</h4>
        <div class="state-tags">
          <el-tag v-for="(count, state) in threadDetails.stateDistribution" :key="state" :type="getStateType(state)" class="state-tag">
            {{ state }}: {{ count }}
          </el-tag>
        </div>
      </div>
      <div class="detail-section" v-if="threadDetails.threads?.length">
        <h4>线程列表（前 {{ threadDetails.totalShown }} 个）</h4>
        <el-table :data="threadDetails.threads" stripe size="small" max-height="400">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="name" label="名称" min-width="200" show-overflow-tooltip />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="getStateType(row.state)" size="small">{{ row.state }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="守护" width="60">
            <template #default="{ row }">{{ row.daemon ? '是' : '否' }}</template>
          </el-table-column>
          <el-table-column label="CPU 时间" width="90">
            <template #default="{ row }">{{ row.cpuTime }}ms</template>
          </el-table-column>
          <el-table-column label="堆栈" min-width="300" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="stack-trace">{{ row.stackTrace?.[0] || '-' }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </el-dialog>

  <!-- 磁盘详情对话框 -->
  <el-dialog v-model="diskDialogVisible" title="磁盘详细信息" width="700px">
    <div v-if="diskDetails" class="detail-content">
      <el-descriptions :column="2" border class="mb-4">
        <el-descriptions-item label="工作目录">{{ diskDetails.workDir }}</el-descriptions-item>
        <el-descriptions-item label="目录可用">{{ diskDetails.workDirFreeGB }}GB</el-descriptions-item>
      </el-descriptions>
      <div class="detail-section" v-if="diskDetails.drives?.length">
        <h4>磁盘分区</h4>
        <el-table :data="diskDetails.drives" stripe size="small">
          <el-table-column prop="path" label="挂载点" width="100" />
          <el-table-column label="总空间" width="100">
            <template #default="{ row }">{{ row.totalGB }}GB</template>
          </el-table-column>
          <el-table-column label="可用" width="100">
            <template #default="{ row }">{{ row.freeGB }}GB</template>
          </el-table-column>
          <el-table-column label="使用率" min-width="200">
            <template #default="{ row }">
              <el-progress :percentage="row.usagePercent" :color="getDiskColor(row.usagePercent)" :stroke-width="10" />
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
/**
 * 系统自检页面
 * 实时显示系统健康状态和诊断信息
 *
 * 功能：
 * - 实时 CPU、内存、磁盘、线程指标
 * - 可点击卡片展开详情
 * - 自动刷新（每 10 秒）
 * - 运行时间显示
 *
 * 操作维度：系统级
 * 权限要求：系统管理员
 */
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { diagnosticApi } from '@/api'
import { ElMessage } from 'element-plus'

const route = useRoute()

const loading = ref(false)
const running = ref(false)
const result = ref(null)
const expandedCard = ref(null)
const autoRefresh = ref(true)
let refreshTimer = null

// 详情对话框
const cpuDialogVisible = ref(false)
const memoryDialogVisible = ref(false)
const threadDialogVisible = ref(false)
const diskDialogVisible = ref(false)

// 详情数据
const cpuDetails = ref(null)
const memoryDetails = ref(null)
const threadDetails = ref(null)
const diskDetails = ref(null)

/** 总体状态文本 */
const overallStatusText = computed(() => {
  if (!result.value) return '未检测'
  return result.value.overallStatus === 'UP' ? '系统正常' : '系统异常'
})

/** 总体状态类型 */
const overallStatusType = computed(() => {
  if (!result.value) return 'info'
  return result.value.overallStatus === 'UP' ? 'success' : 'warning'
})

/** 切换卡片展开 */
const toggleCard = (card) => {
  expandedCard.value = expandedCard.value === card ? null : card
}

/** 打开详情对话框 */
const openDetail = async (type) => {
  try {
    switch (type) {
      case 'cpu':
        cpuDetails.value = await diagnosticApi.getCpuDetails()
        cpuDialogVisible.value = true
        break
      case 'memory':
        memoryDetails.value = await diagnosticApi.getMemoryDetails()
        memoryDialogVisible.value = true
        break
      case 'threads':
        threadDetails.value = await diagnosticApi.getThreadDetails()
        threadDialogVisible.value = true
        break
      case 'disk':
        diskDetails.value = await diagnosticApi.getDiskDetails()
        diskDialogVisible.value = true
        break
    }
  } catch (error) {
    ElMessage.error('获取详情失败')
  }
}

/** 获取线程状态类型 */
const getStateType = (state) => {
  const typeMap = {
    'RUNNABLE': 'success',
    'BLOCKED': 'danger',
    'WAITING': 'warning',
    'TIMED_WAITING': 'info',
    'NEW': '',
    'TERMINATED': 'info'
  }
  return typeMap[state] || ''
}

/** 获取 CPU 颜色 */
const getCpuColor = (percent) => {
  if (percent < 50) return '#67c23a'
  if (percent < 80) return '#e6a23c'
  return '#f56c6c'
}

/** 获取内存颜色 */
const getMemoryColor = (percent) => {
  if (percent < 70) return '#67c23a'
  if (percent < 90) return '#e6a23c'
  return '#f56c6c'
}

/** 获取磁盘颜色 */
const getDiskColor = (percent) => {
  if (percent < 70) return '#67c23a'
  if (percent < 90) return '#e6a23c'
  return '#f56c6c'
}

/** 格式化时间 */
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

/** 执行自检 */
const runDiagnostic = async () => {
  running.value = true
  try {
    const data = await diagnosticApi.run()
    result.value = data
    ElMessage.success('自检完成')
  } catch (error) {
    ElMessage.error('自检失败')
  } finally {
    running.value = false
  }
}

/** 加载结果 */
const loadResult = async () => {
  loading.value = true
  try {
    const data = await diagnosticApi.getResult()
    result.value = data
  } catch (error) {
    console.error('加载结果失败', error)
  } finally {
    loading.value = false
  }
}

/** 启动自动刷新 */
const startAutoRefresh = () => {
  stopAutoRefresh()
  refreshTimer = setInterval(() => {
    if (autoRefresh.value) {
      loadResult()
    }
  }, 10000) // 每 10 秒刷新
}

/** 停止自动刷新 */
const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

/** 监听自动刷新开关 */
watch(autoRefresh, (val) => {
  if (val) {
    startAutoRefresh()
  } else {
    stopAutoRefresh()
  }
})

/** 路由变化时关闭所有弹窗，防止 keep-alive 下弹窗 DOM 残留导致页面空白 */
watch(() => route.path, () => {
  cpuDialogVisible.value = false
  memoryDialogVisible.value = false
  threadDialogVisible.value = false
  diskDialogVisible.value = false
})

onMounted(() => {
  loadResult()
  startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style scoped>
.diagnostic-page {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

.control-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.control-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
}

.control-right {
  display: flex;
  align-items: center;
}

.uptime-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 16px;
  font-size: 14px;
  color: #666;
}

.uptime-bar .timestamp {
  margin-left: auto;
  font-size: 12px;
  color: #999;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.metric-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.metric-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.metric-card.expanded {
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);
}

.card-main {
  display: flex;
  align-items: center;
  gap: 16px;
}

.card-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.card-icon.cpu { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
.card-icon.memory { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
.card-icon.disk { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); }
.card-icon.threads { background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); }
.card-icon.database { background: linear-gradient(135deg, #fa709a 0%, #fee140 100%); }
.card-icon.agents { background: linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%); }

.card-info {
  flex: 1;
}

.card-title {
  font-size: 13px;
  color: #999;
  margin-bottom: 4px;
}

.card-value {
  font-size: 24px;
  font-weight: 600;
  color: #333;
}

.card-badge {
  text-align: center;
}

.card-badge span {
  font-size: 12px;
  color: #999;
}

.card-details {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #eee;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  font-size: 13px;
}

.detail-row span:first-child {
  color: #666;
}

.detail-row span:last-child {
  font-weight: 500;
}

.detail-btn {
  margin-top: 12px;
  width: 100%;
}

.detail-content {
  max-height: 600px;
  overflow-y: auto;
}

.detail-section {
  margin-top: 20px;
}

.detail-section h4 {
  margin: 0 0 12px;
  font-size: 14px;
  color: var(--el-text-color-primary);
}

.param-list {
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  padding: 12px;
  max-height: 200px;
  overflow-y: auto;
}

.param-item {
  font-family: 'Courier New', monospace;
  font-size: 12px;
  color: var(--el-text-color-regular);
  padding: 2px 0;
  word-break: break-all;
}

.state-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.state-tag {
  font-size: 13px;
}

.stack-trace {
  font-family: 'Courier New', monospace;
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

.mb-4 {
  margin-bottom: 16px;
}
</style>
