<template>
  <div class="iteration-adapt-page">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #409eff">
            <el-icon :size="24"><Folder /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ projectList.length }}</div>
            <div class="stat-label">项目数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" :style="{ background: getPhaseColor(recommendation.phase) }">
            <el-icon :size="24"><MagicStick /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ getPhaseLabel(recommendation.phase) }}</div>
            <div class="stat-label">当前阶段</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #e6a23c">
            <el-icon :size="24"><Setting /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ recommendation.currentStrategy || '-' }}</div>
            <div class="stat-label">当前策略</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #909399">
            <el-icon :size="24"><Timer /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ history.length }}</div>
            <div class="stat-label">调整次数</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 推荐策略 -->
    <el-card class="section-card">
      <template #header>
        <div class="section-header">
          <span>迭代适应策略</span>
          <div>
            <el-select v-model="selectedProject" placeholder="选择项目" style="width: 200px; margin-right: 10px" @change="loadRecommendation">
              <el-option v-for="p in projectList" :key="p.id" :label="p.name" :value="p.id" />
            </el-select>
            <el-button v-if="recommendation.needsChange" type="primary" @click="handleApply" :loading="applying">
              <el-icon><Check /></el-icon> 应用建议
            </el-button>
          </div>
        </div>
      </template>

      <el-row :gutter="20" v-if="recommendation.projectId">
        <el-col :span="12">
          <el-descriptions title="项目信息" :column="1" border>
            <el-descriptions-item label="项目名称">{{ recommendation.projectName }}</el-descriptions-item>
            <el-descriptions-item label="当前版本">{{ recommendation.version }}</el-descriptions-item>
            <el-descriptions-item label="迭代次数">{{ recommendation.versionCount }}</el-descriptions-item>
            <el-descriptions-item label="项目阶段">
              <el-tag :color="getPhaseColor(recommendation.phase)" effect="dark">{{ getPhaseLabel(recommendation.phase) }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-col>
        <el-col :span="12">
          <el-descriptions title="策略对比" :column="1" border>
            <el-descriptions-item label="当前策略">
              <el-tag>{{ recommendation.currentStrategy }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="当前阈值">{{ recommendation.currentPassScore }} 分</el-descriptions-item>
            <el-descriptions-item label="推荐策略">
              <el-tag :type="recommendation.needsChange ? 'warning' : 'success'">{{ recommendation.recommendedStrategy }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="推荐阈值">{{ recommendation.recommendedPassScore }} 分</el-descriptions-item>
          </el-descriptions>
        </el-col>
      </el-row>

      <div v-if="recommendation.reason" class="reason-box">
        <el-icon><InfoFilled /></el-icon>
        <span>{{ recommendation.reason }}</span>
      </div>

      <el-empty v-if="!recommendation.projectId && !loading" description="请选择项目查看推荐策略" />
    </el-card>

    <!-- 阶段说明 -->
    <el-card class="section-card">
      <template #header><span>阶段说明</span></template>
      <el-timeline>
        <el-timeline-item v-for="phase in phases" :key="phase.name" :color="phase.color" :hollow="recommendation.phase !== phase.name">
          <h4>{{ phase.label }}（{{ phase.range }}）</h4>
          <p>{{ phase.description }}</p>
          <p>策略: <el-tag size="small">{{ phase.strategy }}</el-tag> | 阈值: {{ phase.passScore }} 分</p>
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <!-- 调整历史 -->
    <el-card v-if="history.length > 0" class="section-card">
      <template #header><span>调整历史</span></template>
      <el-table :data="history" stripe>
        <el-table-column prop="phase" label="阶段" width="100">
          <template #default="{ row }">
            <el-tag :color="getPhaseColor(row.phase)" effect="dark" size="small">{{ getPhaseLabel(row.phase) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="oldStrategy" label="旧策略" width="120" />
        <el-table-column prop="newStrategy" label="新策略" width="120" />
        <el-table-column label="阈值变化" width="120">
          <template #default="{ row }">{{ row.oldPassScore }} → {{ row.newPassScore }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="时间" width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { iterationAdaptApi, projectApi } from '@/api'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const applying = ref(false)
const selectedProject = ref('')
const projectList = ref([])
const recommendation = ref({})
const history = ref([])

const phases = [
  { name: 'EARLY', label: '早期', range: 'v1-v3', color: '#67c23a', strategy: 'full', passScore: 5, description: '快速验证核心玩法，使用全量迭代和低阈值，允许快速试错' },
  { name: 'MID', label: '中期', range: 'v4-v7', color: '#e6a23c', strategy: 'adaptive', passScore: 7, description: '功能完善和优化，使用自适应迭代和标准阈值' },
  { name: 'LATE', label: '后期', range: 'v8+', color: '#f56c6c', strategy: 'incremental', passScore: 8, description: '稳定性优先，使用增量迭代和高阈值，减少大改动' }
]

onMounted(async () => {
  try {
    const res = await projectApi.getAll()
    projectList.value = res || []
  } catch (e) { console.error(e) }
})

const loadRecommendation = async () => {
  if (!selectedProject.value) return
  loading.value = true
  try {
    recommendation.value = await iterationAdaptApi.getRecommendation(selectedProject.value) || {}
    history.value = await iterationAdaptApi.getHistory(selectedProject.value) || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleApply = async () => {
  applying.value = true
  try {
    await iterationAdaptApi.apply(selectedProject.value)
    ElMessage.success('策略已应用')
    await loadRecommendation()
  } catch (e) {
    ElMessage.error('应用失败')
  } finally {
    applying.value = false
  }
}

const getPhaseColor = (phase) => ({ EARLY: '#67c23a', MID: '#e6a23c', LATE: '#f56c6c' }[phase] || '#909399')
const getPhaseLabel = (phase) => ({ EARLY: '早期', MID: '中期', LATE: '后期' }[phase] || phase)
</script>

<style scoped>
.stat-cards { margin-bottom: 16px; }
.stat-card .el-card__body { display: flex; align-items: center; width: 100%; }
.stat-icon { width: 48px; height: 48px; border-radius: 8px; display: flex; align-items: center; justify-content: center; margin-right: 12px; }
.stat-info { flex: 1; }
.stat-value { font-size: 20px; font-weight: bold; }
.stat-label { font-size: 12px; color: #909399; }
.section-card { margin-bottom: 16px; }
.section-header { display: flex; justify-content: space-between; align-items: center; }
.reason-box { margin-top: 16px; padding: 12px; background: #f0f9ff; border-radius: 4px; display: flex; align-items: center; gap: 8px; color: #409eff; }
</style>
