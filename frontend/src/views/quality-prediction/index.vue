<template>
  <div class="quality-prediction-page">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" :style="{ background: getProbabilityColor(prediction.passProbability) }">
            <el-icon :size="24"><TrendCharts /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ prediction.passProbability || 0 }}%</div>
            <div class="stat-label">通过概率</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" :style="{ background: getRiskColor(prediction.riskLevel) }">
            <el-icon :size="24"><Warning /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ prediction.riskLevel || '-' }}</div>
            <div class="stat-label">风险等级</div>
          </div>
        </el-card>
      </el-col>
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
          <div class="stat-icon" style="background: #909399">
            <el-icon :size="24"><Timer /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ history.length }}</div>
            <div class="stat-label">预测次数</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 操作区 -->
    <el-card class="section-card">
      <template #header>
        <div class="section-header">
          <span>质量预测</span>
          <div>
            <el-select v-model="selectedProject" placeholder="选择项目" style="width: 200px; margin-right: 10px" @change="loadPrediction">
              <el-option v-for="p in projectList" :key="p.id" :label="p.name" :value="p.id" />
            </el-select>
            <el-button type="primary" @click="handlePredict" :loading="predicting">
              <el-icon><TrendCharts /></el-icon> 执行预测
            </el-button>
          </div>
        </div>
      </template>

      <el-row :gutter="20">
        <!-- 通过率仪表盘 -->
        <el-col :span="8">
          <div class="probability-circle" :style="{ borderColor: getProbabilityColor(prediction.passProbability) }">
            <div class="probability-value">{{ prediction.passProbability || 0 }}%</div>
            <div class="probability-label">通过概率</div>
          </div>
          <div style="text-align: center; margin-top: 10px">
            <el-tag :type="getRiskTagType(prediction.riskLevel)" size="large">{{ prediction.riskLevel || '-' }}</el-tag>
          </div>
        </el-col>

        <!-- 风险因素 -->
        <el-col :span="8">
          <h4>风险因素</h4>
          <div v-if="riskFactors.length > 0">
            <div v-for="(factor, i) in riskFactors" :key="i" class="risk-item">
              <el-icon color="#f56c6c"><Warning /></el-icon>
              <span>{{ factor }}</span>
            </div>
          </div>
          <el-empty v-else description="无风险因素" :image-size="60" />
        </el-col>

        <!-- 改进建议 -->
        <el-col :span="8">
          <h4>改进建议</h4>
          <div v-if="suggestions.length > 0">
            <div v-for="(sug, i) in suggestions" :key="i" class="suggestion-item">
              <el-icon color="#67c23a"><CircleCheck /></el-icon>
              <span>{{ sug }}</span>
            </div>
          </div>
          <el-empty v-else description="无改进建议" :image-size="60" />
        </el-col>
      </el-row>

      <!-- 因子详情 -->
      <el-divider />
      <h4>预测因子详情</h4>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="历史通过率">{{ formatPercent(factorsDetail.historicalPassRate) }}</el-descriptions-item>
        <el-descriptions-item label="里程碑完成率">{{ formatPercent(factorsDetail.milestoneCompletionRate) }}</el-descriptions-item>
        <el-descriptions-item label="验证失败次数">{{ factorsDetail.verificationFailCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="Agent错误率">{{ formatPercent(factorsDetail.avgAgentErrorRate) }}</el-descriptions-item>
        <el-descriptions-item label="迭代次数">{{ factorsDetail.iterationCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="版本">{{ prediction.version || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 预测历史 -->
    <el-card v-if="history.length > 0" class="section-card">
      <template #header><span>预测历史</span></template>
      <el-table :data="history" stripe>
        <el-table-column prop="version" label="版本" width="100" />
        <el-table-column label="通过概率" width="120">
          <template #default="{ row }">
            <el-progress :percentage="row.passProbability" :color="getProbabilityColor(row.passProbability)" :stroke-width="8" />
          </template>
        </el-table-column>
        <el-table-column prop="riskLevel" label="风险等级" width="100">
          <template #default="{ row }">
            <el-tag :type="getRiskTagType(row.riskLevel)" size="small">{{ row.riskLevel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="预测时间" width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { qualityPredictionApi, projectApi } from '@/api'
import { ElMessage } from 'element-plus'

const predicting = ref(false)
const selectedProject = ref('')
const projectList = ref([])
const prediction = ref({})
const riskFactors = ref([])
const suggestions = ref([])
const factorsDetail = ref({})
const history = ref([])

onMounted(async () => {
  try {
    const res = await projectApi.getAll()
    projectList.value = res || []
  } catch (e) { console.error(e) }
})

const loadPrediction = async () => {
  if (!selectedProject.value) return
  try {
    const res = await qualityPredictionApi.getLatest(selectedProject.value)
    prediction.value = res || {}
    riskFactors.value = parseJson(res.riskFactors)
    suggestions.value = parseJson(res.improvementSuggestions)
    factorsDetail.value = parseJson(res.factorsDetail)
    history.value = await qualityPredictionApi.getHistory(selectedProject.value) || []
  } catch (e) {
    console.error(e)
  }
}

const handlePredict = async () => {
  if (!selectedProject.value) {
    ElMessage.warning('请先选择项目')
    return
  }
  predicting.value = true
  try {
    const res = await qualityPredictionApi.predict(selectedProject.value)
    prediction.value = res
    riskFactors.value = parseJson(res.riskFactors)
    suggestions.value = parseJson(res.improvementSuggestions)
    factorsDetail.value = parseJson(res.factorsDetail)
    history.value = await qualityPredictionApi.getHistory(selectedProject.value) || []
    ElMessage.success('预测完成')
  } catch (e) {
    ElMessage.error('预测失败')
  } finally {
    predicting.value = false
  }
}

const parseJson = (str) => {
  if (!str) return []
  try { return typeof str === 'string' ? JSON.parse(str) : str } catch { return [] }
}

const getProbabilityColor = (p) => {
  if (p >= 80) return '#67c23a'
  if (p >= 60) return '#e6a23c'
  if (p >= 40) return '#f56c6c'
  return '#909399'
}

const getRiskColor = (level) => {
  const map = { LOW: '#67c23a', MEDIUM: '#e6a23c', HIGH: '#f56c6c', CRITICAL: '#f56c6c' }
  return map[level] || '#909399'
}

const getRiskTagType = (level) => {
  const map = { LOW: 'success', MEDIUM: 'warning', HIGH: 'danger', CRITICAL: 'danger' }
  return map[level] || 'info'
}

const formatPercent = (v) => v != null ? `${Math.round(v * 10) / 10}%` : '-'
</script>

<style scoped>
.stat-cards { margin-bottom: 16px; }
.stat-card .el-card__body { display: flex; align-items: center; width: 100%; }
.stat-icon { width: 48px; height: 48px; border-radius: 8px; display: flex; align-items: center; justify-content: center; margin-right: 12px; }
.stat-info { flex: 1; }
.stat-value { font-size: 24px; font-weight: bold; }
.stat-label { font-size: 12px; color: #909399; }
.section-card { margin-bottom: 16px; }
.section-header { display: flex; justify-content: space-between; align-items: center; }
.probability-circle { width: 160px; height: 160px; border-radius: 50%; border: 8px solid; display: flex; flex-direction: column; align-items: center; justify-content: center; margin: 0 auto; }
.probability-value { font-size: 36px; font-weight: bold; }
.probability-label { font-size: 14px; color: #909399; }
.risk-item, .suggestion-item { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; font-size: 14px; }
</style>
