<template>
  <div class="api-docs-page">
    <!-- 顶部信息 -->
    <el-card class="info-card">
      <div class="info-header">
        <div class="info-left">
          <h2>{{ apiInfo.title }}</h2>
          <p class="api-desc">{{ apiInfo.description }}</p>
        </div>
        <div class="info-right">
          <el-tag type="success">v{{ apiInfo.version }}</el-tag>
          <el-tag>基础路径: {{ basePath }}</el-tag>
          <el-tag type="info">认证: JWT Token</el-tag>
          <el-button type="primary" size="small" @click="openSwaggerUI">
            <el-icon><Link /></el-icon>
            Swagger UI
          </el-button>
        </div>
      </div>
    </el-card>

    <el-row :gutter="16" class="main-content">
      <!-- 左侧标签导航 -->
      <el-col :span="5">
        <el-card class="nav-card">
          <template #header>
            <span>API 分类</span>
          </template>
          <div class="nav-list">
            <div
              v-for="tag in tags"
              :key="tag.name"
              class="nav-item"
              :class="{ active: activeTag === tag.name }"
              @click="scrollToTag(tag.name)"
            >
              <span class="nav-name">{{ tag.name }}</span>
              <el-badge :value="getTagCount(tag.name)" :max="99" type="primary" />
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧 API 列表 -->
      <el-col :span="19">
        <!-- 搜索栏 -->
        <el-card class="search-card">
          <el-input
            v-model="searchText"
            placeholder="搜索 API 路径、描述..."
            clearable
            prefix-icon="Search"
            size="large"
          />
          <div class="search-stats">
            共 {{ totalApis }} 个接口，{{ tags.length }} 个分类
            <span v-if="searchText">，搜索到 {{ filteredPaths.length }} 个结果</span>
          </div>
        </el-card>

        <!-- API 分组展示 -->
        <div v-loading="loading">
          <template v-for="tag in tags" :key="tag.name">
            <el-card
              v-if="getTagPaths(tag.name).length > 0"
              :id="'tag-' + tag.name"
              class="api-group"
            >
              <template #header>
                <div class="group-header">
                  <span class="group-title">{{ tag.name }}</span>
                  <span class="group-desc">{{ tag.description }}</span>
                  <el-tag size="small" type="info">{{ getTagPaths(tag.name).length }} 个接口</el-tag>
                </div>
              </template>

              <div
                v-for="api in getTagPaths(tag.name)"
                :key="api.path + api.method"
                class="api-item"
                :class="{ expanded: expandedApi === api.path + api.method }"
              >
                <div class="api-summary" @click="toggleApi(api)">
                  <el-tag
                    :type="getMethodType(api.method)"
                    size="small"
                    class="method-tag"
                    effect="dark"
                  >
                    {{ api.method }}
                  </el-tag>
                  <code class="api-path">{{ api.path }}</code>
                  <span class="api-desc">{{ api.summary }}</span>
                  <el-icon class="expand-icon"><ArrowDown v-if="expandedApi !== api.path + api.method" /><ArrowUp v-else /></el-icon>
                </div>

                <!-- 详情展开 -->
                <div v-if="expandedApi === api.path + api.method" class="api-detail">
                  <p v-if="api.description" class="detail-desc">{{ api.description }}</p>

                  <!-- 参数列表 -->
                  <div v-if="api.parameters && api.parameters.length > 0" class="detail-section">
                    <h4>请求参数</h4>
                    <el-table :data="api.parameters" size="small" border stripe>
                      <el-table-column prop="name" label="参数名" width="180" />
                      <el-table-column prop="in" label="位置" width="100">
                        <template #default="{ row }">
                          <el-tag size="small" type="info">{{ row.in }}</el-tag>
                        </template>
                      </el-table-column>
                      <el-table-column label="类型" width="120">
                        <template #default="{ row }">
                          {{ getParamType(row) }}
                        </template>
                      </el-table-column>
                      <el-table-column prop="required" label="必填" width="80">
                        <template #default="{ row }">
                          <el-tag :type="row.required ? 'danger' : 'info'" size="small">
                            {{ row.required ? '是' : '否' }}
                          </el-tag>
                        </template>
                      </el-table-column>
                      <el-table-column prop="description" label="说明" min-width="200" />
                    </el-table>
                  </div>

                  <!-- 请求体 -->
                  <div v-if="api.requestBody" class="detail-section">
                    <h4>请求体</h4>
                    <div class="schema-block">
                      <pre>{{ formatSchema(api.requestBody) }}</pre>
                    </div>
                  </div>

                  <!-- 响应 -->
                  <div v-if="api.responses" class="detail-section">
                    <h4>响应</h4>
                    <div v-for="(resp, code) in api.responses" :key="code" class="response-item">
                      <el-tag :type="getResponseType(code)" size="small" class="response-code">{{ code }}</el-tag>
                      <span class="response-desc">{{ resp.description }}</span>
                    </div>
                  </div>

                  <!-- 权限标签 -->
                  <div v-if="api.security" class="detail-section">
                    <h4>权限</h4>
                    <div class="security-tags">
                      <el-tag v-for="s in api.security" :key="s" size="small" type="warning">
                        {{ s }}
                      </el-tag>
                    </div>
                  </div>
                </div>
              </div>
            </el-card>
          </template>

          <el-empty v-if="!loading && filteredPaths.length === 0" description="未找到匹配的 API" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Link } from '@element-plus/icons-vue'

const loading = ref(false)
const searchText = ref('')
const activeTag = ref('')
const expandedApi = ref('')
const apiSpec = ref(null)

const apiInfo = computed(() => apiSpec.value?.info || { title: 'API 文档', version: '1.0.0', description: '' })
const basePath = computed(() => apiSpec.value?.servers?.[0]?.url || '/api')
const tags = computed(() => apiSpec.value?.tags || [])

const totalApis = computed(() => {
  if (!apiSpec.value?.paths) return 0
  let count = 0
  for (const path of Object.values(apiSpec.value.paths)) {
    count += Object.keys(path).filter(m => ['get','post','put','delete','patch'].includes(m)).length
  }
  return count
})

const allPaths = computed(() => {
  if (!apiSpec.value?.paths) return []
  const result = []
  for (const [path, methods] of Object.entries(apiSpec.value.paths)) {
    for (const [method, detail] of Object.entries(methods)) {
      if (!['get','post','put','delete','patch'].includes(method)) continue
      result.push({
        path,
        method: method.toUpperCase(),
        summary: detail.summary || '',
        description: detail.description || '',
        tags: detail.tags || [],
        parameters: detail.parameters || [],
        requestBody: detail.requestBody || null,
        responses: detail.responses || {},
        security: detail.security?.map(s => Object.keys(s)).flat() || null
      })
    }
  }
  return result
})

const filteredPaths = computed(() => {
  let list = allPaths.value
  if (searchText.value) {
    const q = searchText.value.toLowerCase()
    list = list.filter(api =>
      api.path.toLowerCase().includes(q) ||
      api.summary.toLowerCase().includes(q) ||
      api.description.toLowerCase().includes(q) ||
      api.method.toLowerCase().includes(q)
    )
  }
  return list
})

const getTagPaths = (tagName) => {
  return filteredPaths.value.filter(api => api.tags.includes(tagName))
}

const getTagCount = (tagName) => {
  return getTagPaths(tagName).length
}

const getMethodType = (method) => {
  return { GET: 'success', POST: '', PUT: 'warning', DELETE: 'danger', PATCH: 'warning' }[method] || 'info'
}

const getParamType = (param) => {
  if (param.schema) {
    return param.schema.type || 'object'
  }
  return param.type || '-'
}

const getResponseType = (code) => {
  if (code.startsWith('2')) return 'success'
  if (code.startsWith('4')) return 'warning'
  if (code.startsWith('5')) return 'danger'
  return 'info'
}

const formatSchema = (requestBody) => {
  try {
    const content = requestBody.content
    if (!content) return '无内容'
    const json = content['application/json']
    if (!json?.schema) return '无 Schema'
    return JSON.stringify(json.schema, null, 2)
  } catch {
    return '无法解析'
  }
}

const toggleApi = (api) => {
  const key = api.path + api.method
  expandedApi.value = expandedApi.value === key ? '' : key
}

const scrollToTag = (tagName) => {
  activeTag.value = tagName
  nextTick(() => {
    const el = document.getElementById('tag-' + tagName)
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  })
}

const loadApiDocs = async () => {
  loading.value = true
  try {
    const resp = await fetch('/v3/api-docs')
    if (!resp.ok) throw new Error('加载失败')
    apiSpec.value = await resp.json()
    if (tags.value.length > 0) {
      activeTag.value = tags.value[0].name
    }
  } catch (error) {
    console.error('加载 API 文档失败:', error)
    ElMessage.error('加载 API 文档失败')
  } finally {
    loading.value = false
  }
}

const openSwaggerUI = () => {
  window.open('/swagger-ui.html', '_blank')
}

onMounted(() => {
  loadApiDocs()
})
</script>

<style scoped>
.api-docs-page {
  padding: 20px;
}

.info-card {
  margin-bottom: 16px;
}

.info-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
}

.info-left h2 {
  margin: 0 0 8px 0;
  font-size: 20px;
}

.api-desc {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 14px;
  white-space: pre-line;
}

.info-right {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.main-content {
  min-height: calc(100vh - 280px);
}

.nav-card {
  position: sticky;
  top: 20px;
}

.nav-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: calc(100vh - 320px);
  overflow-y: auto;
}

.nav-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 13px;
}

.nav-item:hover {
  background: var(--el-fill-color-light);
}

.nav-item.active {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-weight: 500;
}

.search-card {
  margin-bottom: 16px;
}

.search-stats {
  margin-top: 8px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.api-group {
  margin-bottom: 16px;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.group-title {
  font-size: 16px;
  font-weight: 600;
}

.group-desc {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.api-item {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  margin-bottom: 8px;
  transition: all 0.2s;
}

.api-item:hover {
  border-color: var(--el-color-primary-light-5);
}

.api-item.expanded {
  border-color: var(--el-color-primary);
}

.api-summary {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
}

.method-tag {
  width: 60px;
  text-align: center;
  flex-shrink: 0;
}

.api-path {
  font-family: 'Courier New', monospace;
  font-size: 13px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  background: var(--el-fill-color-light);
  padding: 2px 8px;
  border-radius: 4px;
  flex-shrink: 0;
}

.api-summary .api-desc {
  flex: 1;
  color: var(--el-text-color-regular);
  font-size: 13px;
}

.expand-icon {
  color: var(--el-text-color-secondary);
  flex-shrink: 0;
  transition: transform 0.2s;
}

.api-detail {
  padding: 0 16px 16px;
  border-top: 1px solid var(--el-border-color-extra-light);
}

.detail-desc {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin: 12px 0;
}

.detail-section {
  margin-top: 16px;
}

.detail-section h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: var(--el-text-color-primary);
  font-weight: 600;
}

.schema-block {
  background: var(--el-fill-color-light);
  border-radius: 6px;
  padding: 12px;
  overflow-x: auto;
}

.schema-block pre {
  margin: 0;
  font-size: 12px;
  font-family: 'Courier New', monospace;
  white-space: pre-wrap;
  word-break: break-all;
  color: var(--el-text-color-primary);
}

.response-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.response-code {
  width: 50px;
  text-align: center;
}

.response-desc {
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.security-tags {
  display: flex;
  gap: 8px;
}
</style>
