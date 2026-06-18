<template>
  <div class="knowledge-base-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>游戏开发知识库</span>
          <el-button @click="loadAll" :loading="loading">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </template>

      <!-- 统计卡片 -->
      <el-row :gutter="16" class="stat-cards">
        <el-col :span="6">
          <el-card shadow="hover" @click="activeTab = 'usageRecords'" class="stat-card-clickable">
            <div class="stat-item">
              <div class="stat-value">{{ stats.totalUsageRecords || 0 }}</div>
              <div class="stat-label">使用记录</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" @click="activeTab = 'solutions'" class="stat-card-clickable">
            <div class="stat-item">
              <div class="stat-value">{{ stats.totalSolutions || 0 }}</div>
              <div class="stat-label">解决方案</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" @click="activeTab = 'bestPractices'" class="stat-card-clickable">
            <div class="stat-item">
              <div class="stat-value">{{ stats.totalBestPractices || 0 }}</div>
              <div class="stat-label">最佳实践</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" @click="activeTab = 'templateStats'" class="stat-card-clickable">
            <div class="stat-item">
              <div class="stat-value">{{ stats.templatesTracked || 0 }}</div>
              <div class="stat-label">跟踪模板</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 标签页 -->
      <el-tabs v-model="activeTab">
        <!-- 使用记录 -->
        <el-tab-pane label="使用记录" name="usageRecords">
          <el-table :data="usageRecords" v-loading="loading" stripe>
            <el-table-column label="类型" width="120">
              <template #default="{ row }">
                <el-tag :type="getUsageTypeTag(row.type)" size="small">
                  {{ getUsageTypeLabel(row.type) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="resourceName" label="资源名称" width="150" show-overflow-tooltip />
            <el-table-column label="描述" min-width="250" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="clickable-text" @click="showContentDetail('使用描述', row.description)">
                  {{ row.description || '-' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="结果" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.success ? 'success' : 'danger'" size="small">
                  {{ row.success ? '成功' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="使用时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.usedAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" text @click="showUsageDetail(row)">
                  详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="usageRecords.length === 0" description="暂无使用记录" />
        </el-tab-pane>

        <!-- 解决方案 -->
        <el-tab-pane label="解决方案" name="solutions">
          <!-- 分类筛选 -->
          <div class="category-filter">
            <el-radio-group v-model="activeCategory" size="small">
              <el-radio-button label="">全部</el-radio-button>
              <el-radio-button v-for="cat in solutionCategories" :key="cat.key" :label="cat.key">
                {{ cat.icon }} {{ cat.label }}
              </el-radio-button>
            </el-radio-group>
          </div>
          <div v-for="solution in filteredSolutions" :key="solution.id" class="solution-card">
            <el-card shadow="hover">
              <template #header>
                <div class="solution-header">
                  <div class="solution-title">
                    <el-tag size="small" type="warning">{{ solution.problemType }}</el-tag>
                    <span class="title-text">{{ solution.problemDescription }}</span>
                  </div>
                  <span class="solution-time">{{ formatTime(solution.createdAt) }}</span>
                </div>
              </template>
              <div class="solution-content" @click="showContentDetail('解决方案', solution.solution)">
                <MarkdownRenderer :content="solution.solution" />
              </div>
            </el-card>
          </div>
          <el-empty v-if="solutions.length === 0" description="暂无解决方案" />
        </el-tab-pane>

        <!-- 最佳实践 -->
        <el-tab-pane label="最佳实践" name="bestPractices">
          <div v-for="practice in bestPractices" :key="practice.id" class="practice-card">
            <el-card shadow="hover">
              <template #header>
                <div class="practice-header">
                  <div class="practice-title">
                    <el-tag size="small">{{ practice.category }}</el-tag>
                    <span class="title-text">{{ practice.title }}</span>
                  </div>
                  <span class="practice-time">{{ formatTime(practice.createdAt) }}</span>
                </div>
              </template>
              <div class="practice-content" @click="showContentDetail('最佳实践 - ' + practice.title, practice.content)">
                <MarkdownRenderer :content="practice.content" />
              </div>
            </el-card>
          </div>
          <el-empty v-if="bestPractices.length === 0" description="暂无最佳实践" />
        </el-tab-pane>

        <!-- 模板统计 -->
        <el-tab-pane label="模板统计" name="templateStats">
          <el-table :data="templateStatsList" v-loading="loading" stripe>
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
          <el-empty v-if="templateStatsList.length === 0" description="暂无模板统计" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 内容详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" :title="detailTitle" width="700px" top="5vh">
      <div class="detail-content">
        <MarkdownRenderer :content="detailContent" />
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 使用记录详情弹窗 -->
    <el-dialog v-model="usageDetailVisible" title="使用记录详情" width="600px">
      <el-descriptions :column="1" border v-if="currentUsage">
        <el-descriptions-item label="记录 ID">{{ currentUsage.recordId }}</el-descriptions-item>
        <el-descriptions-item label="模板 ID">{{ currentUsage.templateId }}</el-descriptions-item>
        <el-descriptions-item label="游戏描述">
          <MarkdownRenderer :content="currentUsage.gameDescription || '无'" />
        </el-descriptions-item>
        <el-descriptions-item label="执行结果">
          <el-tag :type="currentUsage.success ? 'success' : 'danger'" size="small">
            {{ currentUsage.success ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="耗时">{{ formatDuration(currentUsage.durationMs) }}</el-descriptions-item>
        <el-descriptions-item label="使用时间">{{ formatTime(currentUsage.usedAt) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="usageDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 知识库页面
 * 展示游戏开发知识库统计、使用记录、解决方案和最佳实践
 *
 * @author chengxun
 * @since 1.0.0
 */
import { ref, computed, onMounted } from 'vue'
import { knowledgeBaseApi } from '@/api'
import { ElMessage } from 'element-plus'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'

const loading = ref(false)
const activeTab = ref('usageRecords')
const activeCategory = ref('')
const stats = ref({})
const usageRecords = ref([])
const solutions = ref([])
const bestPractices = ref([])
const templateStatsMap = ref({})

// 知识分类定义
const solutionCategories = [
  { key: 'psychology', label: '玩家心理学', icon: '🧠' },
  { key: 'game_feel', label: '游戏感觉', icon: '✨' },
  { key: 'game_design', label: '游戏设计', icon: '🎮' },
  { key: 'retention', label: '留存设计', icon: '🔄' },
  { key: 'randomness', label: '随机与概率', icon: '🎲' },
  { key: 'level_design', label: '关卡设计', icon: '🗺️' },
  { key: 'economy', label: '经济系统', icon: '💰' },
  { key: 'performance', label: '性能优化', icon: '⚡' },
  { key: 'architecture', label: '架构设计', icon: '🏗️' },
  { key: 'frontend', label: '前端技术', icon: '🖥️' },
  { key: 'tech_stack', label: '技术栈', icon: '📚' },
  { key: 'bug_pattern', label: 'Bug模式', icon: '🐛' },
  { key: 'player_feedback', label: '玩家反馈', icon: '💬' }
]

// 详情弹窗
const detailDialogVisible = ref(false)
const detailTitle = ref('')
const detailContent = ref('')

// 使用记录详情弹窗
const usageDetailVisible = ref(false)
const currentUsage = ref(null)

/** 模板统计列表（从map转换为数组） */
const templateStatsList = computed(() => {
  return Object.entries(templateStatsMap.value).map(([templateId, stats]) => ({
    templateId,
    ...stats
  }))
})

/** 按分类筛选的解决方案 */
const filteredSolutions = computed(() => {
  if (!activeCategory.value) return solutions.value
  return solutions.value.filter(s => s.problemType === activeCategory.value)
})

/** 获取成功率颜色 */
const getSuccessRateColor = (rate) => {
  if (rate >= 80) return '#67c23a'
  if (rate >= 60) return '#e6a23c'
  return '#f56c6c'
}

/** 获取使用记录类型标签 */
const getUsageTypeTag = (type) => {
  const map = {
    'TEMPLATE': 'primary',
    'SOLUTION': 'success',
    'BEST_PRACTICE': 'warning',
    'KNOWLEDGE_EXTRACTION': 'info',
    'EVOLUTION': 'danger',
    'GAME_LEARNING': ''
  }
  return map[type] || 'info'
}

/** 获取使用记录类型文本 */
const getUsageTypeLabel = (type) => {
  const map = {
    'TEMPLATE': '模板',
    'SOLUTION': '方案',
    'BEST_PRACTICE': '实践',
    'KNOWLEDGE_EXTRACTION': '提取',
    'EVOLUTION': '进化',
    'GAME_LEARNING': '学习'
  }
  return map[type] || type
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

/** 显示内容详情 */
const showContentDetail = (title, content) => {
  detailTitle.value = title
  detailContent.value = content || '暂无内容'
  detailDialogVisible.value = true
}

/** 显示使用记录详情 */
const showUsageDetail = (record) => {
  currentUsage.value = record
  usageDetailVisible.value = true
}

/** 加载统计数据 */
const loadStats = async () => {
  try {
    const data = await knowledgeBaseApi.getStats()
    stats.value = data || {}
  } catch (error) {
    console.error('加载统计数据失败', error)
  }
}

/** 加载使用记录 */
const loadUsageRecords = async () => {
  try {
    const data = await knowledgeBaseApi.getUsageRecords()
    usageRecords.value = data || []
  } catch (error) {
    console.error('加载使用记录失败', error)
  }
}

/** 加载解决方案 */
const loadSolutions = async () => {
  try {
    const data = await knowledgeBaseApi.getSolutionsList()
    solutions.value = data || []
  } catch (error) {
    console.error('加载解决方案失败', error)
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

/** 加载所有数据 */
const loadAll = async () => {
  loading.value = true
  try {
    await Promise.all([
      loadStats(),
      loadUsageRecords(),
      loadSolutions(),
      loadBestPractices()
    ])
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadAll()
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

.stat-card-clickable {
  cursor: pointer;
  transition: all 0.3s;
}

.stat-card-clickable:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
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

.solution-card,
.practice-card {
  margin-bottom: 12px;
}

.solution-header,
.practice-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.solution-title,
.practice-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-text {
  font-weight: 600;
}

.solution-time,
.practice-time {
  font-size: 12px;
  color: #999;
}

.solution-content,
.practice-content {
  font-size: 14px;
  line-height: 1.6;
  cursor: pointer;
  padding: 8px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.solution-content:hover,
.practice-content:hover {
  background-color: #f5f7fa;
}

.clickable-text {
  cursor: pointer;
  color: #409eff;
}

.clickable-text:hover {
  text-decoration: underline;
}

.detail-content {
  max-height: 60vh;
  overflow-y: auto;
  padding: 16px;
  background-color: #f5f7fa;
  border-radius: 8px;
}

.category-filter {
  margin-bottom: 16px;
  padding: 8px 0;
  border-bottom: 1px solid #ebeef5;
}

.category-filter .el-radio-button__inner {
  font-size: 12px;
  padding: 6px 12px;
}
</style>
