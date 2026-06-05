<template>
  <div class="search-page">
    <el-card>
      <template #header>
        <span>全局搜索</span>
      </template>

      <!-- 搜索框 -->
      <div class="search-bar">
        <el-input
          v-model="keyword"
          placeholder="输入关键词搜索..."
          size="large"
          clearable
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
          <template #append>
            <el-button @click="handleSearch" :loading="loading">搜索</el-button>
          </template>
        </el-input>
      </div>

      <!-- 搜索建议 -->
      <div class="suggestions" v-if="suggestions.length > 0 && !hasSearched">
        <div class="suggestions-title">搜索建议</div>
        <el-tag
          v-for="suggestion in suggestions"
          :key="suggestion"
          class="suggestion-tag"
          @click="handleSuggestionClick(suggestion)"
        >
          {{ suggestion }}
        </el-tag>
      </div>

      <!-- 搜索结果 -->
      <div v-if="hasSearched" class="search-results">
        <div v-if="totalResults === 0" class="no-results">
          <el-empty description="未找到相关结果" />
        </div>

        <template v-else>
          <div class="results-summary">
            找到 {{ totalResults }} 条结果
          </div>

          <!-- Agent 结果 -->
          <div v-if="results.agents && results.agents.length > 0" class="result-section">
            <h3 class="section-title">
              <el-icon><UserFilled /></el-icon> Agent ({{ results.agents.length }})
            </h3>
            <div v-for="item in results.agents" :key="item.id" class="result-item" @click="router.push(item.link)">
              <div class="result-title">{{ item.title }}</div>
              <div class="result-subtitle">{{ item.subtitle }}</div>
            </div>
          </div>

          <!-- 告警结果 -->
          <div v-if="results.alerts && results.alerts.length > 0" class="result-section">
            <h3 class="section-title">
              <el-icon><Bell /></el-icon> 告警 ({{ results.alerts.length }})
            </h3>
            <div v-for="item in results.alerts" :key="item.id" class="result-item" @click="router.push(item.link)">
              <div class="result-title">{{ item.title }}</div>
              <div class="result-subtitle">{{ item.subtitle }}</div>
            </div>
          </div>

          <!-- 审查结果 -->
          <div v-if="results.reviews && results.reviews.length > 0" class="result-section">
            <h3 class="section-title">
              <el-icon><Document /></el-icon> 代码审查 ({{ results.reviews.length }})
            </h3>
            <div v-for="item in results.reviews" :key="item.id" class="result-item" @click="router.push(item.link)">
              <div class="result-title">{{ item.title }}</div>
              <div class="result-subtitle">{{ item.subtitle }}</div>
            </div>
          </div>

          <!-- 日志结果 -->
          <div v-if="results.logs && results.logs.length > 0" class="result-section">
            <h3 class="section-title">
              <el-icon><List /></el-icon> 操作日志 ({{ results.logs.length }})
            </h3>
            <div v-for="item in results.logs" :key="item.id" class="result-item" @click="router.push(item.link)">
              <div class="result-title">{{ item.title }}</div>
              <div class="result-subtitle">{{ item.subtitle }}</div>
            </div>
          </div>
        </template>
      </div>
    </el-card>
  </div>
</template>

<script setup>
/**
 * 全局搜索页面
 * 跨模块统一搜索
 *
 * 操作维度：系统级
 * 权限要求：登录用户即可
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { searchApi } from '@/api'
import { ElMessage } from 'element-plus'

const router = useRouter()

const loading = ref(false)
const keyword = ref('')
const hasSearched = ref(false)
const results = ref({})
const suggestions = ref([])

/** 总结果数 */
const totalResults = computed(() => {
  let count = 0
  if (results.value.agents) count += results.value.agents.length
  if (results.value.alerts) count += results.value.alerts.length
  if (results.value.reviews) count += results.value.reviews.length
  if (results.value.logs) count += results.value.logs.length
  return count
})

/** 加载搜索建议 */
const loadSuggestions = async () => {
  if (!keyword.value || keyword.value.length < 2) {
    suggestions.value = []
    return
  }

  try {
    const data = await searchApi.getSuggestions({ q: keyword.value })
    suggestions.value = data || []
  } catch {
    suggestions.value = []
  }
}

/** 搜索 */
const handleSearch = async () => {
  if (!keyword.value.trim()) return

  loading.value = true
  hasSearched.value = true
  try {
    const data = await searchApi.search({ q: keyword.value, limit: 10 })
    results.value = data || {}
  } catch (error) {
    ElMessage.error('搜索失败')
  } finally {
    loading.value = false
  }
}

/** 点击建议 */
const handleSuggestionClick = (suggestion) => {
  keyword.value = suggestion
  handleSearch()
}

/** 监听关键词变化，加载建议 */
watch(keyword, () => {
  if (!hasSearched.value) {
    loadSuggestions()
  }
})

onMounted(() => {
  // 检查 URL 参数
  const urlParams = new URLSearchParams(window.location.search)
  const q = urlParams.get('q')
  if (q) {
    keyword.value = q
    handleSearch()
  }
})
</script>

<style scoped>
.search-page {
  padding: 20px;
  max-width: 900px;
  margin: 0 auto;
}

.search-bar {
  margin-bottom: 20px;
}

.suggestions {
  margin-bottom: 20px;
}

.suggestions-title {
  font-size: 14px;
  color: #999;
  margin-bottom: 8px;
}

.suggestion-tag {
  margin-right: 8px;
  margin-bottom: 8px;
  cursor: pointer;
}

.suggestion-tag:hover {
  color: #409eff;
}

.results-summary {
  font-size: 14px;
  color: #999;
  margin-bottom: 16px;
}

.result-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 16px;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.result-item {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.result-item:hover {
  border-color: #409eff;
  background: #f5f7fa;
}

.result-title {
  font-weight: 500;
  margin-bottom: 4px;
}

.result-subtitle {
  font-size: 13px;
  color: #666;
}

.no-results {
  padding: 40px 0;
}
</style>
