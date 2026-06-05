<template>
  <div class="code-viewer-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="file-info">
            <el-icon><Document /></el-icon>
            <span>{{ fileName }}</span>
            <el-tag v-if="language" size="small" type="info">{{ language }}</el-tag>
            <el-tag v-if="isMarkdown" size="small" type="success">Markdown</el-tag>
          </div>
          <div class="actions">
            <el-button size="small" @click="toggleView" v-if="isMarkdown">
              <el-icon><View /></el-icon> {{ showSource ? '预览' : '源码' }}
            </el-button>
            <el-button size="small" @click="handleCopy">
              <el-icon><CopyDocument /></el-icon> 复制
            </el-button>
            <el-button size="small" @click="handleDownload">
              <el-icon><Download /></el-icon> 下载
            </el-button>
            <el-button @click="router.back()">返回</el-button>
          </div>
        </div>
      </template>

      <div v-loading="loading" class="code-content">
        <!-- Markdown 渲染视图 -->
        <div v-if="isMarkdown && !showSource" class="markdown-body" v-html="renderedMarkdown"></div>

        <!-- 代码视图 -->
        <div v-else-if="content" class="code-wrapper">
          <div class="line-numbers">
            <div v-for="n in lineCount" :key="n" class="line-number">{{ n }}</div>
          </div>
          <pre class="code-block"><code v-html="highlightedCode"></code></pre>
        </div>

        <el-empty v-else-if="!loading" description="无法加载文件内容" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
/**
 * 代码查看页面
 * 支持代码语法高亮和Markdown渲染
 */
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { codeBrowserApi } from '@/api'
import { ElMessage } from 'element-plus'
import { View } from '@element-plus/icons-vue'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'
import { marked } from 'marked'

// 配置marked
marked.setOptions({
  breaks: true,
  gfm: true
})

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const content = ref('')
const language = ref('')
const showSource = ref(false)

const fileName = computed(() => {
  const path = route.query.path || ''
  return path.split('/').pop() || '未知文件'
})

/** 判断是否为Markdown文件 */
const isMarkdown = computed(() => {
  const name = fileName.value.toLowerCase()
  return name.endsWith('.md') || name.endsWith('.markdown') || language.value === 'markdown'
})

/** 计算行数 */
const lineCount = computed(() => {
  if (!content.value) return 0
  return content.value.split('\n').length
})

/** 渲染Markdown */
const renderedMarkdown = computed(() => {
  if (!content.value) return ''
  try {
    return marked(content.value)
  } catch (e) {
    return `<pre>${escapeHtml(content.value)}</pre>`
  }
})

/** 语法高亮 */
const highlightedCode = computed(() => {
  if (!content.value) return ''
  try {
    if (language.value && hljs.getLanguage(language.value)) {
      return hljs.highlight(content.value, { language: language.value }).value
    }
    return hljs.highlightAuto(content.value).value
  } catch (e) {
    return escapeHtml(content.value)
  }
})

/** HTML转义 */
const escapeHtml = (str) => {
  return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

/** 切换视图 */
const toggleView = () => {
  showSource.value = !showSource.value
}

/** 加载文件内容 */
const loadContent = async () => {
  const projectId = route.query.projectId
  const path = route.query.path

  if (!projectId || !path) {
    ElMessage.warning('缺少必要参数')
    return
  }

  loading.value = true
  try {
    const result = await codeBrowserApi.getFileContent(projectId, path)
    if (result.success) {
      // 统一换行符为 \n
      content.value = (result.content || '').replace(/\r\n/g, '\n').replace(/\r/g, '\n')
      language.value = result.language || ''
    } else {
      ElMessage.error(result.message || '加载文件失败')
    }
  } catch (error) {
    ElMessage.error('加载文件内容失败')
  } finally {
    loading.value = false
  }
}

/** 复制内容 */
const handleCopy = async () => {
  try {
    await navigator.clipboard.writeText(content.value)
    ElMessage.success('已复制到剪贴板')
  } catch (error) {
    ElMessage.error('复制失败')
  }
}

/** 下载文件 */
const handleDownload = () => {
  const blob = new Blob([content.value], { type: 'text/plain' })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName.value
  link.click()
  window.URL.revokeObjectURL(url)
}

onMounted(() => {
  loadContent()
})
</script>

<style scoped>
.code-viewer-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 500;
}

.actions {
  display: flex;
  gap: 8px;
}

.code-content {
  min-height: 400px;
}

/* Markdown渲染样式 */
.markdown-body {
  padding: 24px;
  background: #fff;
  border-radius: 8px;
  line-height: 1.8;
}

.markdown-body :deep(h1) {
  font-size: 2em;
  border-bottom: 1px solid #eaecef;
  padding-bottom: 0.3em;
  margin-bottom: 16px;
}

.markdown-body :deep(h2) {
  font-size: 1.5em;
  border-bottom: 1px solid #eaecef;
  padding-bottom: 0.3em;
  margin-bottom: 16px;
}

.markdown-body :deep(h3) {
  font-size: 1.25em;
  margin-bottom: 16px;
}

.markdown-body :deep(p) {
  margin-bottom: 16px;
}

.markdown-body :deep(code) {
  background: #f6f8fa;
  padding: 0.2em 0.4em;
  border-radius: 3px;
  font-size: 85%;
}

.markdown-body :deep(pre) {
  background: #f6f8fa;
  padding: 16px;
  border-radius: 6px;
  overflow-x: auto;
  margin-bottom: 16px;
}

.markdown-body :deep(pre code) {
  background: transparent;
  padding: 0;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 2em;
  margin-bottom: 16px;
}

.markdown-body :deep(li) {
  margin-bottom: 4px;
}

.markdown-body :deep(blockquote) {
  border-left: 4px solid #dfe2e5;
  padding: 0 16px;
  color: #6a737d;
  margin-bottom: 16px;
}

.markdown-body :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin-bottom: 16px;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid #dfe2e5;
  padding: 6px 13px;
}

.markdown-body :deep(th) {
  background: #f6f8fa;
}

.markdown-body :deep(img) {
  max-width: 100%;
}

/* 代码视图样式 */
.code-wrapper {
  display: flex;
  background: #1e1e1e;
  border-radius: 8px;
  overflow: auto;
}

.line-numbers {
  padding: 16px 12px;
  background: #2d2d2d;
  color: #858585;
  text-align: right;
  user-select: none;
  font-family: 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.6;
  border-right: 1px solid #404040;
}

.line-number {
  padding: 0 8px;
}

.code-block {
  flex: 1;
  margin: 0;
  padding: 16px;
  font-family: 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.6;
  overflow-x: auto;
  white-space: pre;
  word-wrap: normal;
}

.code-block code {
  background: transparent;
  padding: 0;
  white-space: inherit;
  word-wrap: inherit;
}
</style>
