<template>
  <div class="code-quality-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>代码质量报告</span>
          <el-button @click="router.back()">返回</el-button>
        </div>
      </template>

      <!-- 质量评分 -->
      <div class="quality-score" v-if="report">
        <div class="score-circle" :style="{ borderColor: scoreColor }">
          <div class="score-value" :style="{ color: scoreColor }">{{ report.score || 0 }}</div>
          <div class="score-label">质量评分</div>
        </div>
      </div>

      <!-- 问题统计 -->
      <el-row :gutter="16" class="stat-cards" v-if="report">
        <el-col :xs="12" :sm="12" :md="6" :lg="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value error">{{ report.errorCount || 0 }}</div>
              <div class="stat-label">错误</div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="12" :md="6" :lg="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value warning">{{ report.warningCount || 0 }}</div>
              <div class="stat-label">警告</div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="12" :md="6" :lg="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value info">{{ report.infoCount || 0 }}</div>
              <div class="stat-label">提示</div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="12" :md="6" :lg="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value">{{ report.fileCount || 0 }}</div>
              <div class="stat-label">文件数</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 问题列表 -->
      <h3 class="section-title">问题列表</h3>
      <el-table :data="issues" v-loading="loading" stripe>
        <el-table-column label="级别" width="80">
          <template #default="{ row }">
            <el-tag :type="getIssueType(row.severity)" size="small">
              {{ row.severity }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="filePath" label="文件" min-width="200" show-overflow-tooltip />
        <el-table-column prop="line" label="行号" width="80" />
        <el-table-column prop="message" label="问题描述" min-width="250" show-overflow-tooltip />
        <el-table-column prop="rule" label="规则" width="150" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
/**
 * 代码质量页面
 * 查看项目代码质量报告
 *
 * 操作维度：项目级
 * 权限要求：code:review
 */
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { codeQualityApi } from '@/api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const report = ref(null)
const issues = ref([])

/** 评分颜色 */
const scoreColor = computed(() => {
  const score = report.value?.score || 0
  if (score >= 80) return '#67c23a'
  if (score >= 60) return '#e6a23c'
  return '#f56c6c'
})

/** 获取问题类型 */
const getIssueType = (severity) => {
  const typeMap = {
    'ERROR': 'danger',
    'WARNING': 'warning',
    'INFO': 'info'
  }
  return typeMap[severity] || 'info'
}

/** 加载报告 */
const loadReport = async () => {
  const projectId = route.params.projectId
  if (!projectId) return

  loading.value = true
  try {
    const [reportData, issuesData] = await Promise.all([
      codeQualityApi.getReport(projectId),
      codeQualityApi.getIssues(projectId)
    ])
    report.value = reportData
    issues.value = issuesData || []
  } catch (error) {
    ElMessage.error('加载代码质量报告失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadReport()
})
</script>

<style scoped>
.code-quality-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.quality-score {
  display: flex;
  justify-content: center;
  margin: 20px 0;
}

.score-circle {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  border: 4px solid;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.score-value {
  font-size: 36px;
  font-weight: bold;
}

.score-label {
  font-size: 12px;
  color: #999;
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
}

.error {
  color: #f56c6c;
}

.warning {
  color: #e6a23c;
}

.info {
  color: #909399;
}

.stat-label {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.section-title {
  margin: 16px 0 12px;
  font-size: 16px;
}

/* 手机端 */
@media (max-width: 767px) {
  .code-quality-page {
    padding: 12px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .score-circle {
    width: 90px;
    height: 90px;
  }

  .score-value {
    font-size: 28px;
  }

  .stat-value {
    font-size: 20px;
  }

  .stat-item {
    padding: 4px;
  }
}
</style>
