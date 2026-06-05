<template>
  <div class="git-detail-page">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>仓库详情</span>
          <el-button @click="router.back()">返回</el-button>
        </div>
      </template>

      <template v-if="repository">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="仓库名称">{{ repository.name }}</el-descriptions-item>
          <el-descriptions-item label="项目ID">{{ repository.projectId }}</el-descriptions-item>
          <el-descriptions-item label="分支">{{ repository.branch || 'main' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="repository.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ repository.status || '未知' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="远程地址" :span="2">
            <el-text type="primary">{{ repository.remoteUrl || '-' }}</el-text>
          </el-descriptions-item>
          <el-descriptions-item label="本地路径" :span="2">
            <el-text type="info">{{ repository.localPath || '-' }}</el-text>
          </el-descriptions-item>
          <el-descriptions-item label="最后同步">{{ formatTime(repository.lastSyncAt) }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(repository.createdAt) }}</el-descriptions-item>
        </el-descriptions>

        <!-- 操作按钮 -->
        <div class="actions">
          <el-button type="primary" @click="handlePull">
            <el-icon><Download /></el-icon> 拉取
          </el-button>
          <el-button type="success" @click="handlePush">
            <el-icon><Upload /></el-icon> 推送
          </el-button>
        </div>

        <!-- 提交记录 -->
        <div class="commits-section">
          <h4>提交记录</h4>
          <el-table :data="commits" v-loading="loadingCommits" stripe>
            <el-table-column prop="hash" label="哈希" width="100">
              <template #default="{ row }">
                <el-text type="primary" class="commit-hash">{{ row.hash?.substring(0, 7) }}</el-text>
              </template>
            </el-table-column>
            <el-table-column prop="message" label="提交信息" min-width="300" show-overflow-tooltip />
            <el-table-column prop="author" label="作者" width="120" />
            <el-table-column prop="date" label="时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.date) }}
              </template>
            </el-table-column>
          </el-table>
        </div>
      </template>

      <el-empty v-else-if="!loading" description="仓库不存在" />
    </el-card>
  </div>
</template>

<script setup>
/**
 * Git仓库详情页面
 * 查看仓库信息和提交记录
 */
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { gitApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const loadingCommits = ref(false)
const repository = ref(null)
const commits = ref([])

/** 格式化时间 */
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

/** 加载仓库详情 */
const loadRepository = async () => {
  const id = route.params.id
  if (!id) return

  loading.value = true
  try {
    const data = await gitApi.getById(id)
    repository.value = data
    loadCommits()
  } catch (error) {
    ElMessage.error('加载仓库详情失败')
  } finally {
    loading.value = false
  }
}

/** 加载提交记录 */
const loadCommits = async () => {
  const id = route.params.id
  if (!id) return

  loadingCommits.value = true
  try {
    const data = await gitApi.getCommits(id)
    commits.value = data || []
  } catch (error) {
    console.error('加载提交记录失败:', error)
  } finally {
    loadingCommits.value = false
  }
}

/** 拉取代码 */
const handlePull = async () => {
  try {
    await ElMessageBox.confirm('确定要拉取最新代码吗？', '确认操作')
    await gitApi.pull(route.params.id)
    ElMessage.success('拉取成功')
    loadRepository()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('拉取失败')
    }
  }
}

/** 推送代码 */
const handlePush = async () => {
  try {
    await ElMessageBox.confirm('确定要推送代码吗？', '确认操作')
    await gitApi.push(route.params.id)
    ElMessage.success('推送成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('推送失败')
    }
  }
}

onMounted(() => {
  loadRepository()
})
</script>

<style scoped>
.git-detail-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.actions {
  margin-top: 24px;
  display: flex;
  gap: 12px;
}

.commits-section {
  margin-top: 24px;
}

.commits-section h4 {
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--el-border-color);
}

.commit-hash {
  font-family: monospace;
  cursor: pointer;
}
</style>
