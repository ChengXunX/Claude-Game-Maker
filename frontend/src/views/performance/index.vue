<template>
  <div class="performance-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>Agent 绩效</span>
          <div class="header-actions">
            <ProjectSelector
              v-model="selectedProjectId"
              placeholder="选择项目"
              width="200px"
              size="default"
              @change="handleProjectChange"
            />
            <el-button @click="loadPerformance" :loading="loading">
              <el-icon><Refresh /></el-icon> 刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- 绩效统计 -->
      <el-row :gutter="16" class="stat-cards">
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value">{{ stats.totalAgents || 0 }}</div>
              <div class="stat-label">Agent 总数</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value grade-s">{{ stats.gradeS || 0 }}</div>
              <div class="stat-label">S 级</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value grade-a">{{ stats.gradeA || 0 }}</div>
              <div class="stat-label">A 级</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value grade-b">{{ stats.gradeB || 0 }}</div>
              <div class="stat-label">B 级及以下</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 绩效列表 -->
      <el-table :data="performances" v-loading="loading" stripe>
        <el-table-column prop="agentName" label="Agent 名称" width="150" />
        <el-table-column prop="agentRole" label="角色" width="120" />
        <el-table-column label="完成率" width="100">
          <template #default="{ row }">
            <el-progress
              :percentage="Math.round((row.completionRate || 0) * 100)"
              :color="getRateColor(row.completionRate)"
              :stroke-width="8"
              :show-text="false"
              style="width: 60px; display: inline-block"
            />
            <span style="margin-left: 4px">{{ Math.round((row.completionRate || 0) * 100) }}%</span>
          </template>
        </el-table-column>
        <el-table-column label="效率" width="100">
          <template #default="{ row }">
            {{ Math.round((row.efficiency || 0) * 100) }}%
          </template>
        </el-table-column>
        <el-table-column label="质量" width="100">
          <template #default="{ row }">
            {{ Math.round((row.qualityScore || 0) * 100) }}%
          </template>
        </el-table-column>
        <el-table-column label="综合评分" width="100">
          <template #default="{ row }">
            <span class="score">{{ (row.overallScore || 0).toFixed(1) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="等级" width="80">
          <template #default="{ row }">
            <el-tag :type="getGradeType(row.grade)" size="small" effect="dark">
              {{ row.grade || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="完成任务" width="100">
          <template #default="{ row }">
            {{ row.completedTasks || 0 }} / {{ row.totalTasks || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="handleViewDetail(row)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
/**
 * Agent 绩效页面
 * 查看 Agent 绩效指标和等级评估
 *
 * 操作维度：系统级/项目级
 * 权限要求：agents:view
 */
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { performanceApi } from '@/api'
import { ElMessage } from 'element-plus'
import ProjectSelector from '@/components/ProjectSelector.vue'

const router = useRouter()

const loading = ref(false)
const performances = ref([])
const stats = ref({})
const selectedProjectId = ref(localStorage.getItem('selectedProjectId') || '')

/** 项目切换 */
const handleProjectChange = (projectId) => {
  loadPerformance()
}

/** 获取比率颜色 */
const getRateColor = (rate) => {
  if (rate >= 0.8) return '#67c23a'
  if (rate >= 0.6) return '#e6a23c'
  return '#f56c6c'
}

/** 获取等级标签类型 */
const getGradeType = (grade) => {
  const typeMap = {
    'S': 'danger',
    'A': 'warning',
    'B': 'success',
    'C': 'info',
    'D': ''
  }
  return typeMap[grade] || 'info'
}

/** 加载绩效数据 */
const loadPerformance = async () => {
  loading.value = true
  try {
    const [perfData, statsData] = await Promise.all([
      performanceApi.getAll(),
      performanceApi.getStats()
    ])
    performances.value = perfData || []
    stats.value = statsData || {}
  } catch (error) {
    ElMessage.error('加载绩效数据失败')
  } finally {
    loading.value = false
  }
}

/** 查看详情 */
const handleViewDetail = (performance) => {
  router.push(`/performance/${performance.agentId}`)
}

onMounted(() => {
  loadPerformance()
})
</script>

<style scoped>
.performance-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-cards {
  margin-bottom: 20px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}

.grade-s {
  color: #f56c6c;
}

.grade-a {
  color: #e6a23c;
}

.grade-b {
  color: #67c23a;
}

.stat-label {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.score {
  font-weight: bold;
  color: #409eff;
}
</style>
