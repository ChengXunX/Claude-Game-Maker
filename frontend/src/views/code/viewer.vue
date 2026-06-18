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
        <MarkdownRenderer v-if="isMarkdown && !showSource" :content="content" />

        <!-- 代码视图 -->
        <div v-else-if="content" class="code-wrapper">
          <div class="line-numbers">
            <div v-for="n in lineCount" :key="n" class="line-number">{{ n }}</div>
          </div>
          <pre class="code-block"><code v-html="highlightedCode"></code></pre>
        </div>

        <!-- 图片预览 -->
        <div v-else-if="isImageFile && fileRawUrl" class="media-preview">
          <img :src="fileRawUrl" :alt="fileName" class="media-image" @error="handleMediaError" />
        </div>

        <!-- 视频播放 -->
        <div v-else-if="isVideoFile && fileRawUrl" class="media-preview">
          <video :src="fileRawUrl" controls class="media-video" @error="handleMediaError">
            您的浏览器不支持视频播放
          </video>
        </div>

        <!-- 音频播放 -->
        <div v-else-if="isAudioFile && fileRawUrl" class="media-preview audio-preview">
          <el-icon :size="64" color="#409EFF"><Headset /></el-icon>
          <p class="audio-filename">{{ fileName }}</p>
          <audio :src="fileRawUrl" controls class="media-audio" @error="handleMediaError">
            您的浏览器不支持音频播放
          </audio>
        </div>

        <!-- 媒体加载失败 -->
        <div v-else-if="(isImageFile || isVideoFile || isAudioFile) && mediaError" class="media-preview">
          <el-icon :size="64" color="#F56C6C"><WarningFilled /></el-icon>
          <p class="media-error-text">媒体文件加载失败</p>
          <p class="media-error-hint">可能原因：文件不存在、路径错误或格式不支持</p>
          <el-button size="small" @click="handleDownloadRaw">下载文件</el-button>
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
import { View, VideoPlay, Headset, WarningFilled } from '@element-plus/icons-vue'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import 'highlight.js/styles/github-dark.css'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const content = ref('')
const language = ref('')
const showSource = ref(false)
const mediaError = ref(false)

const fileName = computed(() => {
  const path = route.query.path || ''
  return path.split('/').pop() || '未知文件'
})

/** 判断是否为Markdown文件 */
const isMarkdown = computed(() => {
  const name = fileName.value.toLowerCase()
  return name.endsWith('.md') || name.endsWith('.markdown') || language.value === 'markdown'
})

// 媒体文件类型检测
const imageExtensions = ['png', 'jpg', 'jpeg', 'gif', 'svg', 'webp', 'bmp', 'ico', 'avif']
const videoExtensions = ['mp4', 'webm', 'ogg', 'ogv', 'avi', 'mov', 'mkv']
const audioExtensions = ['mp3', 'wav', 'oga', 'flac', 'aac', 'm4a']

const currentExtension = computed(() => {
  const name = fileName.value.toLowerCase()
  const parts = name.split('.')
  return parts.length > 1 ? parts[parts.length - 1] : ''
})

const isImageFile = computed(() => imageExtensions.includes(currentExtension.value))
const isVideoFile = computed(() => videoExtensions.includes(currentExtension.value))
const isAudioFile = computed(() => audioExtensions.includes(currentExtension.value))

// 文件原始内容URL
const fileRawUrl = computed(() => {
  const projectId = route.query.projectId
  const path = route.query.path
  if (!projectId || !path) return ''
  return `/api/code-browser/raw/${projectId}?path=${encodeURIComponent(path)}`
})

/** 计算行数 */
const lineCount = computed(() => {
  if (!content.value) return 0
  return content.value.split('\n').length
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

  // 媒体文件不需要加载文本内容
  const ext = path.split('.').pop()?.toLowerCase() || ''
  if (imageExtensions.includes(ext) || videoExtensions.includes(ext) || audioExtensions.includes(ext)) {
    loading.value = false
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

/** 媒体文件加载错误处理 */
const handleMediaError = (e) => {
  console.error('媒体文件加载失败:', e)
  mediaError.value = true
}

/** 下载原始媒体文件 */
const handleDownloadRaw = () => {
  if (fileRawUrl.value) {
    window.open(fileRawUrl.value, '_blank')
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

/* 媒体预览 */
.media-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  min-height: 400px;
}

.media-image {
  max-width: 100%;
  max-height: 70vh;
  object-fit: contain;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.media-video {
  max-width: 100%;
  max-height: 70vh;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.audio-preview {
  gap: 16px;
}

.audio-filename {
  color: var(--el-text-color-primary);
  font-size: 14px;
  margin: 0;
}

.media-audio {
  width: 400px;
  max-width: 100%;
}

.media-error-text {
  color: var(--el-color-danger);
  font-size: 16px;
  font-weight: 500;
  margin: 16px 0 8px;
}

.media-error-hint {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin: 0 0 16px;
}

/* 代码视图样式 */
.code-wrapper {
  display: flex;
  background: #f6f8fa;
  border-radius: 8px;
  overflow: auto;
}

.line-numbers {
  padding: 16px 12px;
  background: #f6f8fa;
  color: #6a737d;
  text-align: right;
  user-select: none;
  font-family: 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.6;
  border-right: 1px solid #e1e4e8;
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
  color: #24292e;
}

.code-block code {
  background: transparent;
  padding: 0;
  white-space: inherit;
  word-wrap: inherit;
  color: inherit;
}

/* 暗色主题下的代码样式 */
html.dark .code-wrapper {
  background: #282c34;
}

html.dark .line-numbers {
  background: #282c34;
  color: #636d83;
  border-right-color: #3e4451;
}

html.dark .code-block {
  color: #abb2bf;
}

html.dark .code-block code.hljs {
  background: transparent;
  color: #abb2bf;
}
</style>
