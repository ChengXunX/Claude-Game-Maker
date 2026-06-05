<template>
  <div class="log-detail-page">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>日志详情</span>
          <el-button @click="router.back()">返回</el-button>
        </div>
      </template>

      <el-descriptions :column="2" border v-if="log">
        <el-descriptions-item label="日志ID">{{ log.id }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ formatTime(log.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="操作用户">{{ log.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ log.ipAddress || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">
          <el-tag :type="getActionType(log.action)">{{ log.actionDescription || log.action }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作状态">
          <el-tag :type="log.status === 'SUCCESS' ? 'success' : 'danger'">
            {{ log.status === 'SUCCESS' ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="目标类型">{{ log.targetType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="目标名称">{{ log.targetName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Agent ID">{{ log.agentId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="项目ID">{{ log.projectId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ log.durationMs ? log.durationMs + 'ms' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作级别">{{ log.level || '-' }}</el-descriptions-item>
        <el-descriptions-item label="详情" :span="2">{{ log.detail || '-' }}</el-descriptions-item>
        <el-descriptions-item label="错误信息" :span="2" v-if="log.errorMessage">
          <span class="error-text">{{ log.errorMessage }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 请求参数 -->
      <el-card class="mt-4" v-if="log?.requestParams">
        <template #header>请求参数</template>
        <pre class="code-block">{{ formatJson(log.requestParams) }}</pre>
      </el-card>

      <!-- 响应数据 -->
      <el-card class="mt-4" v-if="log?.responseData">
        <template #header>响应数据</template>
        <pre class="code-block">{{ formatJson(log.responseData) }}</pre>
      </el-card>
    </el-card>
  </div>
</template>

<script setup>
/**
 * 日志详情页面
 * 查看单条操作日志的完整信息
 *
 * 操作维度：系统级
 * 权限要求：logs:view
 */
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { logApi } from '@/api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const log = ref(null)

/** 格式化时间 */
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

/** 获取操作类型标签颜色 */
const getActionType = (action) => {
  const typeMap = {
    'LOGIN': 'info',
    'LOGOUT': 'info',
    'CREATE': 'success',
    'UPDATE': 'warning',
    'DELETE': 'danger',
    'APPROVE': 'success'
  }
  return typeMap[action] || 'info'
}

/** 格式化 JSON */
const formatJson = (str) => {
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch {
    return str
  }
}

/** 加载日志详情 */
const loadLog = async () => {
  const id = route.params.id
  if (!id) return

  loading.value = true
  try {
    const data = await logApi.getById(id)
    log.value = data
  } catch (error) {
    ElMessage.error('加载日志详情失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadLog()
})
</script>

<style scoped>
.log-detail-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.mt-4 {
  margin-top: 16px;
}

.error-text {
  color: #f56c6c;
}

.code-block {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  font-size: 12px;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
