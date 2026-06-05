<template>
  <div class="code-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon :size="24" color="#409EFF"><FolderOpened /></el-icon>
            <span>代码浏览</span>
          </div>
          <div class="header-actions">
            <el-button size="small" @click="handleDownload" :disabled="!currentFile">
              <el-icon><Download /></el-icon> 下载
            </el-button>
            <el-button @click="router.back()">
              <el-icon><Back /></el-icon> 返回
            </el-button>
          </div>
        </div>
      </template>

      <div class="resizable-container" ref="containerRef">
        <!-- 文件树 -->
        <div class="file-tree-panel" :style="{ width: leftPanelWidth + 'px' }">
          <el-card shadow="never" class="file-tree-card">
            <template #header>
              <div class="tree-header">
                <span>文件列表</span>
                <el-button size="small" text @click="loadFileTree">
                  <el-icon><Refresh /></el-icon>
                </el-button>
              </div>
            </template>
            <el-input
              v-model="filterText"
              placeholder="搜索文件"
              size="small"
              clearable
              class="filter-input"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-tree
              ref="treeRef"
              :data="fileTree"
              :props="treeProps"
              node-key="path"
              highlight-current
              :filter-node-method="filterNode"
              @node-click="handleFileClick"
              v-loading="loadingTree"
              default-expand-all
            >
              <template #default="{ node, data }">
                <span class="tree-node" :title="data.path">
                  <el-icon v-if="data.isDirectory"><Folder /></el-icon>
                  <el-icon v-else-if="isImage(data.extension)"><Picture /></el-icon>
                  <el-icon v-else><Document /></el-icon>
                  <span class="node-label">{{ data.name }}</span>
                  <span v-if="!data.isDirectory && data.size" class="node-size">
                    {{ formatSize(data.size) }}
                  </span>
                </span>
              </template>
            </el-tree>
          </el-card>
        </div>

        <!-- 拖动分割线 -->
        <div
          class="resize-handle"
          @mousedown="startResize"
          :class="{ 'is-resizing': isResizing }"
        >
          <div class="resize-line"></div>
        </div>

        <!-- 文件内容 -->
        <div class="file-content-panel" :style="{ width: rightPanelWidth + 'px' }">
          <el-card shadow="never" class="file-content-card">
            <template #header>
              <div class="content-header">
                <div class="file-path" :title="currentFile">
                  <el-icon><Document /></el-icon>
                  <span class="file-name">{{ displayFileName }}</span>
                  <span v-if="currentFilePath" class="file-directory">{{ currentFilePath }}</span>
                  <el-tag v-if="currentLanguage" size="small" type="info" class="language-tag">
                    {{ currentLanguage }}
                  </el-tag>
                </div>
                <div class="file-actions" v-if="currentFile">
                  <el-button size="small" @click="toggleMarkdownView" v-if="isMarkdown">
                    <el-icon><View /></el-icon> {{ showMarkdownSource ? '预览' : '源码' }}
                  </el-button>
                  <el-button size="small" @click="handleFormatJson" v-if="isJsonFile">
                    <el-icon><Operation /></el-icon> 格式化
                  </el-button>
                  <el-button size="small" @click="handleCopy">
                    <el-icon><CopyDocument /></el-icon> 复制
                  </el-button>
                </div>
              </div>
            </template>

            <!-- 代码内容 -->
            <div v-if="fileContent" class="file-content" v-loading="loadingContent">
              <!-- Markdown渲染视图 -->
              <div v-if="isMarkdown && !showMarkdownSource" class="markdown-body" v-html="renderedMarkdown"></div>

              <!-- 代码视图 -->
              <div v-else class="code-wrapper">
                <div class="line-numbers">
                  <div v-for="n in lineCount" :key="n" class="line-number">{{ n }}</div>
                </div>
                <pre class="code-block"><code :class="'language-' + currentLanguage" v-html="highlightedCode"></code></pre>
              </div>
            </div>

            <!-- 空状态 -->
            <el-empty v-else-if="!loadingContent" description="请选择文件查看内容">
              <template #image>
                <el-icon :size="64" color="#c0c4cc"><Document /></el-icon>
              </template>
            </el-empty>
          </el-card>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
/**
 * 代码浏览页面
 * 浏览项目代码文件，支持语法高亮和下载
 *
 * 主要功能：
 * - 文件树浏览
 * - 代码语法高亮
 * - JSON格式化
 * - 文件下载
 * - 可拖动调整面板大小
 *
 * 操作维度：项目级
 * 权限要求：projects:view
 */
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import hljs from 'highlight.js'

// 配置marked
marked.setOptions({
  breaks: true,
  gfm: true
})
import { codeBrowserApi } from '@/api'
import {
  FolderOpened, Back, Refresh, Search, Folder, Document, Picture,
  CopyDocument, Download, Operation, View
} from '@element-plus/icons-vue'

// 导入highlight.js样式
import 'highlight.js/styles/github-dark.css'

const route = useRoute()
const router = useRouter()
const treeRef = ref(null)
const containerRef = ref(null)

const loadingTree = ref(false)
const loadingContent = ref(false)
const fileTree = ref([])
const fileContent = ref('')
const currentFile = ref('')
const currentLanguage = ref('')
const filterText = ref('')
const showMarkdownSource = ref(false)

// 拖动调整相关
const leftPanelWidth = ref(280)
const rightPanelWidth = ref(calcRightWidth())
const isResizing = ref(false)
const startX = ref(0)
const startLeftWidth = ref(0)

const treeProps = {
  children: 'children',
  label: 'name'
}

// 计算右侧宽度
function calcRightWidth() {
  const containerWidth = typeof window !== 'undefined' ? window.innerWidth - 100 : 1000
  return containerWidth - 280 - 10
}

// 显示的文件名（仅文件名）
const displayFileName = computed(() => {
  if (!currentFile.value) return ''
  const parts = currentFile.value.split('/')
  return parts[parts.length - 1]
})

// 文件目录路径
const currentFilePath = computed(() => {
  if (!currentFile.value) return ''
  const parts = currentFile.value.split('/')
  if (parts.length <= 1) return ''
  return parts.slice(0, -1).join('/') + '/'
})

// 是否是JSON文件
const isJsonFile = computed(() => {
  if (!currentFile.value) return false
  return currentFile.value.endsWith('.json')
})

// 是否是Markdown文件
const isMarkdown = computed(() => {
  if (!currentFile.value) return false
  const name = currentFile.value.toLowerCase()
  return name.endsWith('.md') || name.endsWith('.markdown') || currentLanguage.value === 'markdown'
})

// 渲染Markdown
const renderedMarkdown = computed(() => {
  if (!fileContent.value) return ''
  try {
    return marked(fileContent.value)
  } catch (e) {
    return `<pre>${escapeHtml(fileContent.value)}</pre>`
  }
})

// 切换Markdown视图
const toggleMarkdownView = () => {
  showMarkdownSource.value = !showMarkdownSource.value
}

// 计算行数
const lineCount = computed(() => {
  if (!fileContent.value) return 0
  return fileContent.value.split('\n').length
})

// 语法高亮后的代码
const highlightedCode = computed(() => {
  if (!fileContent.value) return ''
  try {
    if (currentLanguage.value && hljs.getLanguage(currentLanguage.value)) {
      return hljs.highlight(fileContent.value, { language: currentLanguage.value }).value
    }
    return hljs.highlightAuto(fileContent.value).value
  } catch (e) {
    return escapeHtml(fileContent.value)
  }
})

// HTML转义
const escapeHtml = (str) => {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

// 过滤节点
const filterNode = (value, data) => {
  if (!value) return true
  return data.name.toLowerCase().includes(value.toLowerCase())
}

// 监听搜索文本变化
watch(filterText, (val) => {
  treeRef.value?.filter(val)
})

// 判断是否是图片
const isImage = (ext) => {
  return ['png', 'jpg', 'jpeg', 'gif', 'svg', 'ico', 'webp'].includes(ext)
}

// 格式化文件大小
const formatSize = (bytes) => {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

// 开始拖动调整大小
const startResize = (e) => {
  isResizing.value = true
  startX.value = e.clientX
  startLeftWidth.value = leftPanelWidth.value
  document.addEventListener('mousemove', handleResize)
  document.addEventListener('mouseup', stopResize)
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
}

// 处理拖动
const handleResize = (e) => {
  if (!isResizing.value) return
  const diff = e.clientX - startX.value
  const newLeftWidth = startLeftWidth.value + diff
  // 限制最小和最大宽度
  if (newLeftWidth >= 200 && newLeftWidth <= 600) {
    leftPanelWidth.value = newLeftWidth
    rightPanelWidth.value = calcRightWidth() - newLeftWidth + 280
  }
}

// 停止拖动
const stopResize = () => {
  isResizing.value = false
  document.removeEventListener('mousemove', handleResize)
  document.removeEventListener('mouseup', stopResize)
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
}

// 窗口大小变化时重新计算
const handleWindowResize = () => {
  rightPanelWidth.value = calcRightWidth() - leftPanelWidth.value + 280
}

/** 加载文件树 */
const loadFileTree = async () => {
  const projectId = route.params.projectId
  if (!projectId) {
    ElMessage.warning('请先选择项目')
    return
  }

  loadingTree.value = true
  try {
    const data = await codeBrowserApi.getFileTree(projectId)
    fileTree.value = data || []
  } catch (error) {
    console.error('加载文件树失败:', error)
    ElMessage.error('加载文件树失败')
  } finally {
    loadingTree.value = false
  }
}

/** 点击文件 */
const handleFileClick = async (data) => {
  if (data.isDirectory) return

  const projectId = route.params.projectId
  currentFile.value = data.path
  currentLanguage.value = ''
  fileContent.value = ''
  loadingContent.value = true

  try {
    const result = await codeBrowserApi.getFileContent(projectId, data.path)
    if (result.success) {
      // 统一换行符为 \n
      fileContent.value = (result.content || '').replace(/\r\n/g, '\n').replace(/\r/g, '\n')
      currentLanguage.value = result.language || ''
    } else {
      ElMessage.error(result.message || '加载文件内容失败')
    }
  } catch (error) {
    console.error('加载文件内容失败:', error)
    ElMessage.error('加载文件内容失败')
  } finally {
    loadingContent.value = false
  }
}

/** 格式化JSON */
const handleFormatJson = () => {
  try {
    const parsed = JSON.parse(fileContent.value)
    fileContent.value = JSON.stringify(parsed, null, 2)
    ElMessage.success('JSON格式化完成')
  } catch (error) {
    ElMessage.error('JSON格式化失败：无效的JSON格式')
  }
}

/** 复制内容 */
const handleCopy = async () => {
  try {
    await navigator.clipboard.writeText(fileContent.value)
    ElMessage.success('已复制到剪贴板')
  } catch (error) {
    // 降级方案
    const textarea = document.createElement('textarea')
    textarea.value = fileContent.value
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    ElMessage.success('已复制到剪贴板')
  }
}

/** 下载文件 */
const handleDownload = async () => {
  const projectId = route.params.projectId
  if (!projectId || !currentFile.value) return

  try {
    const blob = await codeBrowserApi.downloadFile(projectId, currentFile.value)
    const downloadUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = displayFileName.value
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(downloadUrl)
    ElMessage.success('文件下载成功')
  } catch (error) {
    console.error('下载文件失败:', error)
    ElMessage.error('下载文件失败')
  }
}

onMounted(() => {
  loadFileTree()
  window.addEventListener('resize', handleWindowResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleWindowResize)
  document.removeEventListener('mousemove', handleResize)
  document.removeEventListener('mouseup', stopResize)
})
</script>

<style scoped>
.code-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}

/* 可调整大小的容器 */
.resizable-container {
  display: flex;
  gap: 0;
  overflow: hidden;
}

.file-tree-panel {
  flex-shrink: 0;
  min-width: 200px;
  max-width: 600px;
}

.file-tree-card {
  height: calc(100vh - 200px);
  overflow-y: auto;
}

.tree-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: var(--el-text-color-primary);
}

.filter-input {
  margin-bottom: 12px;
}

/* 拖动分割线 */
.resize-handle {
  width: 10px;
  cursor: col-resize;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-bg-color);
  transition: background-color 0.2s;
  flex-shrink: 0;
}

.resize-handle:hover,
.resize-handle.is-resizing {
  background: var(--el-color-primary-light-9);
}

.resize-line {
  width: 2px;
  height: 40px;
  background: var(--el-border-color);
  border-radius: 1px;
  transition: background-color 0.2s;
}

.resize-handle:hover .resize-line,
.resize-handle.is-resizing .resize-line {
  background: var(--el-color-primary);
  height: 60px;
}

.file-content-panel {
  flex-shrink: 0;
  min-width: 400px;
}

.file-content-card {
  height: calc(100vh - 200px);
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.file-path {
  display: flex;
  align-items: center;
  gap: 8px;
  overflow: hidden;
  flex: 1;
  min-width: 0;
  color: var(--el-text-color-primary);
}

.file-name {
  font-weight: 600;
  white-space: nowrap;
  color: var(--el-text-color-primary);
}

.file-directory {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.language-tag {
  margin-left: 8px;
  flex-shrink: 0;
}

.file-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  overflow: hidden;
  padding: 2px 0;
  color: var(--el-text-color-primary);
}

.node-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: var(--el-text-color-primary);
}

.node-size {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  margin-left: auto;
  flex-shrink: 0;
}

.file-content {
  height: calc(100vh - 280px);
  overflow: auto;
}

.code-wrapper {
  display: flex;
  min-height: 100%;
}

.line-numbers {
  padding: 20px 16px;
  background: #f5f7fa;
  color: #909399;
  text-align: right;
  user-select: none;
  border-right: 1px solid var(--el-border-color);
  font-family: 'Courier New', Consolas, Monaco, monospace;
  font-size: 14px;
  line-height: 1.6;
  flex-shrink: 0;
}

.line-number {
  padding: 0 8px;
}

.code-block {
  flex: 1;
  margin: 0;
  padding: 20px;
  background: #ffffff;
  overflow-x: auto;
  font-family: 'Courier New', Consolas, Monaco, monospace;
  font-size: 14px;
  line-height: 1.6;
  tab-size: 4;
  white-space: pre;
  word-wrap: normal;
  color: #303133;
}

.code-block code {
  background: transparent;
  padding: 0;
  font-size: inherit;
  white-space: inherit;
  word-wrap: inherit;
  color: inherit;
}

/* Markdown渲染样式 */
.markdown-body {
  padding: 24px;
  background: var(--el-bg-color);
  border-radius: 4px;
  line-height: 1.8;
  overflow: auto;
  max-height: calc(100vh - 280px);
  color: var(--el-text-color-primary);
}

.markdown-body :deep(h1) {
  font-size: 2em;
  border-bottom: 1px solid var(--el-border-color-lighter);
  padding-bottom: 0.3em;
  margin-bottom: 16px;
  color: var(--el-text-color-primary);
}

.markdown-body :deep(h2) {
  font-size: 1.5em;
  border-bottom: 1px solid var(--el-border-color-lighter);
  padding-bottom: 0.3em;
  margin-bottom: 16px;
  color: var(--el-text-color-primary);
}

.markdown-body :deep(h3) {
  font-size: 1.25em;
  margin-bottom: 16px;
  color: var(--el-text-color-primary);
}

.markdown-body :deep(p) {
  margin-bottom: 16px;
  color: var(--el-text-color-primary);
}

.markdown-body :deep(code) {
  background: #f5f7fa;
  padding: 0.2em 0.4em;
  border-radius: 3px;
  font-size: 85%;
  color: #303133;
}

.markdown-body :deep(pre) {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 6px;
  overflow-x: auto;
  margin-bottom: 16px;
}

.markdown-body :deep(pre code) {
  background: transparent;
  padding: 0;
  color: #303133;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 2em;
  margin-bottom: 16px;
  color: var(--el-text-color-primary);
}

.markdown-body :deep(li) {
  margin-bottom: 4px;
  color: var(--el-text-color-primary);
}

.markdown-body :deep(blockquote) {
  border-left: 4px solid var(--el-color-primary);
  padding: 0 16px;
  color: var(--el-text-color-secondary);
  margin-bottom: 16px;
}

.markdown-body :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin-bottom: 16px;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid var(--el-border-color);
  padding: 8px 12px;
  color: var(--el-text-color-primary);
}

.markdown-body :deep(th) {
  background: #f5f7fa;
}

.markdown-body :deep(img) {
  max-width: 100%;
}

.markdown-body :deep(a) {
  color: var(--el-color-primary);
  text-decoration: none;
}

.markdown-body :deep(a:hover) {
  text-decoration: underline;
}

/* 响应式 */
@media (max-width: 768px) {
  .resizable-container {
    flex-direction: column;
  }

  .file-tree-panel,
  .file-content-panel {
    width: 100% !important;
    min-width: unset;
  }

  .resize-handle {
    width: 100%;
    height: 10px;
    cursor: row-resize;
  }

  .resize-line {
    width: 40px;
    height: 2px;
  }

  .file-tree-card,
  .file-content-card {
    height: auto;
    max-height: 40vh;
  }

  .file-content {
    height: 50vh;
  }
}
</style>
