<template>
  <div class="knowledge-base-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>游戏开发知识库</span>
          <el-button @click="loadStats" :loading="loading">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </template>

      <!-- 统计卡片 -->
      <el-row :gutter="16" class="stat-cards">
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value">{{ stats.totalUsageRecords || 0 }}</div>
              <div class="stat-label">使用记录</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value">{{ stats.totalSolutions || 0 }}</div>
              <div class="stat-label">解决方案</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value">{{ stats.totalBestPractices || 0 }}</div>
              <div class="stat-label">最佳实践</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value">{{ stats.templatesTracked || 0 }}</div>
              <div class="stat-label">跟踪模板</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 标签页 -->
      <el-tabs v-model="activeTab">
        <!-- 模板统计 -->
        <el-tab-pane label="模板统计" name="templateStats">
          <el-table :data="templateStats" v-loading="loading" stripe>
            <el-table-column prop="templateId" label="模板 ID" width="150" />
            <el-table-column label="成功率" width="120">
              <template #default="{ row }">
                <el-progress :percentage="Math.round(row.successRate)" :color="getSuccessRateColor(row.successRate)" />
              </template>
            </el-table-column>
            <el-table-column label="平均耗时" width="120">
              <template #default="{ row }">
                {{ formatDuration(row.avgDurationMs) }}
              </template>
            </el-table-column>
            <el-table-column prop="totalUsage" label="使用次数" width="100" />
            <el-table-column label="最后使用" width="180">
              <template #default="{ row }">
                {{ formatTime(row.lastUsed) }}
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 最佳实践 -->
        <el-tab-pane label="最佳实践" name="bestPractices">
          <div v-for="practice in bestPractices" :key="practice.id" class="practice-card">
            <el-card shadow="hover">
              <template #header>
                <div class="practice-header">
                  <span class="practice-title">{{ practice.title }}</span>
                  <el-tag size="small">{{ practice.category }}</el-tag>
                </div>
              </template>
              <div class="practice-content">{{ practice.content }}</div>
              <div class="practice-time">{{ formatTime(practice.createdAt) }}</div>
            </el-card>
          </div>
          <el-empty v-if="bestPractices.length === 0" description="暂无最佳实践" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
/**
 * 知识库页面
 * 展示游戏开发知识库统计和最佳实践
 *
 * @author chengxun
 * @since 1.0.0
 */
import { ref, onMounted } from 'vue'
import { knowledgeBaseApi } from '@/api'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const activeTab = ref('templateStats')
const stats = ref({})
const templateStats = ref([])
const bestPractices = ref([])

/** 获取成功率颜色 */
const getSuccessRateColor = (rate) => {
  if (rate >= 80) return '#67c23a'
  if (rate >= 60) return '#e6a23c'
  return '#f56c6c'
}

/** 格式化时长 */
const formatDuration = (ms) => {
  if (!ms) return '-'
  if (ms < 1000) return ms + 'ms'
  return (ms / 1000).toFixed(1) + 's'
}

/** 格式化时间 */
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

/** 加载统计数据 */
const loadStats = async () => {
  loading.value = true
  try {
    const data = await knowledgeBaseApi.getStats()
    stats.value = data || {}
  } catch (error) {
    ElMessage.error('加载统计数据失败')
  } finally {
    loading.value = false
  }
}

/** 加载最佳实践 */
const loadBestPractices = async () => {
  try {
    const data = await knowledgeBaseApi.getBestPractices()
    bestPractices.value = data || []
  } catch (error) {
    console.error('加载最佳实践失败', error)
  }
}

onMounted(() => {
  loadStats()
  loadBestPractices()
})
</script>

<style scoped>
.knowledge-base-page {
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

.stat-label {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.practice-card {
  margin-bottom: 12px;
}

.practice-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.practice-title {
  font-weight: 600;
}

.practice-content {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}

.practice-time {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}
</style>
