<template>
  <div class="monitoring-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>监控中心</span>
          <el-button @click="loadMetrics" :loading="loading">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </template>

      <!-- 指标卡片 -->
      <el-row :gutter="16" class="metric-cards">
        <el-col :xs="12" :sm="12" :md="6" :lg="6">
          <el-card shadow="hover">
            <div class="metric-item">
              <div class="metric-value">{{ metrics.totalAgents || 0 }}</div>
              <div class="metric-label">Agent 总数</div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="12" :md="6" :lg="6">
          <el-card shadow="hover">
            <div class="metric-item">
              <div class="metric-value alive">{{ metrics.aliveAgents || 0 }}</div>
              <div class="metric-label">运行中</div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="12" :md="6" :lg="6">
          <el-card shadow="hover">
            <div class="metric-item">
              <div class="metric-value busy">{{ metrics.busyAgents || 0 }}</div>
              <div class="metric-label">忙碌</div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="12" :md="6" :lg="6">
          <el-card shadow="hover">
            <div class="metric-item">
              <div class="metric-value">{{ metrics.totalProjects || 0 }}</div>
              <div class="metric-label">项目数量</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 系统信息 -->
      <el-row :gutter="16" class="mt-4">
        <el-col :span="12">
          <el-card>
            <template #header>
              <span>JVM 信息</span>
            </template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="Java 版本">{{ systemInfo.javaVersion || '-' }}</el-descriptions-item>
              <el-descriptions-item label="CPU 核心">{{ systemInfo.availableProcessors || '-' }}</el-descriptions-item>
              <el-descriptions-item label="最大内存">{{ systemInfo.maxMemory || '-' }}</el-descriptions-item>
              <el-descriptions-item label="已分配内存">{{ systemInfo.totalMemory || '-' }}</el-descriptions-item>
              <el-descriptions-item label="已使用内存">{{ systemInfo.usedMemory || '-' }}</el-descriptions-item>
              <el-descriptions-item label="可用内存">{{ systemInfo.freeMemory || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card>
            <template #header>
              <span>系统环境</span>
            </template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="操作系统">{{ systemInfo.osName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="系统架构">{{ systemInfo.osArch || '-' }}</el-descriptions-item>
              <el-descriptions-item label="系统版本">{{ systemInfo.osVersion || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
/**
 * 监控中心页面
 * 显示系统监控指标
 *
 * 操作维度：系统级
 * 权限要求：system:monitor
 */
import { ref, onMounted } from 'vue'
import { monitorApi } from '@/api'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const metrics = ref({})
const systemInfo = ref({})

/** 加载指标 */
const loadMetrics = async () => {
  loading.value = true
  try {
    const data = await monitorApi.getMetrics()
    if (data) {
      // 映射 Agent 统计
      const agents = data.agents || {}
      metrics.value = {
        totalAgents: agents.total || 0,
        aliveAgents: agents.alive || 0,
        busyAgents: agents.busy || 0,
        totalTasks: agents.total || 0
      }
      // 映射系统信息
      const jvm = data.jvm || {}
      systemInfo.value = {
        javaVersion: jvm.javaVersion || '-',
        osName: data.osName || `${data.osArch || ''} ${data.osName || ''}`.trim() || '-',
        availableProcessors: jvm.availableProcessors || '-',
        maxMemory: jvm.maxMemory || '-',
        totalMemory: jvm.totalMemory || '-',
        freeMemory: jvm.freeMemory || '-'
      }
    }
  } catch (error) {
    ElMessage.error('加载监控数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadMetrics()
})
</script>

<style scoped>
.monitoring-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.metric-cards {
  margin-bottom: 16px;
}

.metric-item {
  text-align: center;
  padding: 8px;
}

.metric-value {
  font-size: 28px;
  font-weight: bold;
  color: #409eff;
}

.alive {
  color: #67c23a;
}

.busy {
  color: #e6a23c;
}

.metric-label {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.mt-4 {
  margin-top: 16px;
}

/* 手机端 */
@media (max-width: 767px) {
  .monitoring-page {
    padding: 12px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .metric-value {
    font-size: 22px;
  }

  .metric-item {
    padding: 4px;
  }

  /* 描述列表响应式 */
  :deep(.el-descriptions) {
    font-size: 13px;
  }
}
</style>
