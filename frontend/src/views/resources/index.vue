<template>
  <div class="resources-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>资源用量</span>
          <el-button @click="loadStats" :loading="loading">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </template>

      <!-- 统计卡片 -->
      <el-row :gutter="16" class="stat-cards">
        <el-col :xs="12" :sm="12" :md="6" :lg="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value">{{ formatNumber(stats.totalTokens) }}</div>
              <div class="stat-label">总 Token 用量</div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="12" :md="6" :lg="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value">${{ (stats.totalCost || 0).toFixed(2) }}</div>
              <div class="stat-label">总费用</div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="12" :md="6" :lg="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value">{{ formatNumber(stats.monthlyTokens) }}</div>
              <div class="stat-label">本月用量</div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="12" :md="6" :lg="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value">${{ (stats.monthlyCost || 0).toFixed(2) }}</div>
              <div class="stat-label">本月费用</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 配额信息 -->
      <el-card class="quota-card" v-if="quota">
        <template #header>
          <span>月度配额</span>
        </template>
        <div class="quota-info">
          <div class="quota-label">已使用：{{ quota.used || 0 }} / {{ quota.limit || '无限制' }}</div>
          <el-progress
            :percentage="quotaPercentage"
            :color="quotaColor"
            style="width: 100%"
          />
        </div>
      </el-card>

      <!-- Agent 使用排名 -->
      <h3 class="section-title">Agent 使用排名</h3>
      <el-table :data="agentRanking" v-loading="loading" stripe>
        <el-table-column type="index" label="排名" width="60" />
        <el-table-column label="Agent 名称" width="150">
          <template #default="{ row }">
            {{ row.agentName || row.agentId || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="角色" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.agentRole" size="small">{{ row.agentRole }}</el-tag>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="Token 用量" width="120">
          <template #default="{ row }">
            <span class="token-value">{{ formatNumber(row.tokens || row.totalTokens || 0) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="费用" width="100">
          <template #default="{ row }">
            <span class="cost-value">${{ (row.cost || row.totalCost || 0).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="使用占比" min-width="200">
          <template #default="{ row }">
            <el-progress
              :percentage="getUsagePercentage(row)"
              :stroke-width="10"
              :show-text="false"
              style="width: 150px; display: inline-block"
            />
            <span style="margin-left: 8px">{{ getUsagePercentage(row) }}%</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
/**
 * 资源用量页面
 * 查看 Token 使用量和费用统计
 *
 * 操作维度：系统级
 * 权限要求：系统管理员
 */
import { ref, computed, onMounted } from 'vue'
import { resourceApi } from '@/api'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const stats = ref({})
const quota = ref(null)
const agentRanking = ref([])

/** 配额百分比 */
const quotaPercentage = computed(() => {
  if (!quota.value || !quota.value.limit) return 0
  return Math.min(100, Math.round((quota.value.used / quota.value.limit) * 100))
})

/** 配额颜色 */
const quotaColor = computed(() => {
  const pct = quotaPercentage.value
  if (pct >= 90) return '#f56c6c'
  if (pct >= 70) return '#e6a23c'
  return '#67c23a'
})

/** 格式化数字 */
const formatNumber = (num) => {
  if (!num || num === 0) return '0'
  if (num >= 1000000000) return (num / 1000000000).toFixed(2) + 'B'
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'K'
  return num.toLocaleString()
}

/** 获取使用百分比（使用预计算的值） */
const getUsagePercentage = (agent) => {
  return agent.usagePercent || 0
}

/** 加载统计数据 */
const loadStats = async () => {
  loading.value = true
  try {
    // 并行加载今日和月度数据
    const [todayData, monthlyData, rankingData] = await Promise.all([
      resourceApi.getToday(),
      resourceApi.getMonthly(),
      resourceApi.getAgentUsage()
    ])

    // 合并今日和月度数据
    stats.value = {
      ...(todayData || {}),
      monthlyTokens: monthlyData?.totalTokens || 0,
      monthlyCost: monthlyData?.totalCost || 0
    }

    // 加载 Agent 排名（按30天统计）
    const totalAgentTokens = (rankingData || []).reduce((sum, a) => sum + (a.tokens || a.totalTokens || 0), 0)
    agentRanking.value = (rankingData || [])
      .map(a => ({
        ...a,
        usagePercent: totalAgentTokens > 0 ? Math.round(((a.tokens || a.totalTokens || 0) / totalAgentTokens) * 100) : 0
      }))
      .sort((a, b) => ((b.tokens || b.totalTokens || 0) - (a.tokens || a.totalTokens || 0)))
  } catch (error) {
    console.error('加载统计数据失败:', error)
    ElMessage.error('加载统计数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadStats()
})
</script>

<style scoped>
.resources-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.stat-cards {
  margin-bottom: 20px;
}

.stat-item {
  text-align: center;
  padding: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
  word-break: break-all;
}

.stat-label {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.quota-card {
  margin-bottom: 20px;
}

.quota-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.quota-label {
  font-size: 14px;
}

.section-title {
  margin: 16px 0 12px;
  font-size: 16px;
}

.token-value {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-weight: 600;
  color: #409eff;
}

.cost-value {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-weight: 600;
  color: #67c23a;
}

.text-muted {
  color: #909399;
  font-size: 12px;
}

/* 手机端 */
@media (max-width: 767px) {
  .resources-page {
    padding: 12px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .stat-value {
    font-size: 18px;
  }

  .stat-item {
    padding: 4px;
  }
}
</style>
