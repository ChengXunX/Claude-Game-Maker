<template>
  <div class="markdown-renderer markdown-body" v-html="renderedContent"></div>
</template>

<script setup>
/**
 * Markdown 渲染器组件
 * 只读展示 Markdown 内容
 */
import { computed } from 'vue'
import { marked } from 'marked'

const props = defineProps({
  content: {
    type: String,
    default: ''
  }
})

// 渲染 Markdown
const renderedContent = computed(() => {
  if (!props.content) return '<p class="text-muted">暂无内容</p>'
  try {
    return marked(props.content, {
      breaks: true,
      gfm: true
    })
  } catch (e) {
    return props.content
  }
})
</script>

<style scoped>
.markdown-renderer {
  font-size: 14px;
  line-height: 1.6;
  color: var(--el-text-color-primary);
}

/* Markdown 样式 */
.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4),
.markdown-body :deep(h5),
.markdown-body :deep(h6) {
  margin-top: 16px;
  margin-bottom: 8px;
  font-weight: 600;
  line-height: 1.25;
}

.markdown-body :deep(h1) {
  font-size: 1.5em;
  padding-bottom: 0.3em;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.markdown-body :deep(h2) {
  font-size: 1.3em;
  padding-bottom: 0.3em;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.markdown-body :deep(h3) {
  font-size: 1.1em;
}

.markdown-body :deep(p) {
  margin-top: 0;
  margin-bottom: 10px;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 2em;
  margin-top: 0;
  margin-bottom: 10px;
}

.markdown-body :deep(li) {
  margin-top: 0.25em;
}

.markdown-body :deep(blockquote) {
  padding: 0 1em;
  color: var(--el-text-color-secondary);
  border-left: 0.25em solid var(--el-border-color);
  margin-top: 0;
  margin-bottom: 10px;
}

.markdown-body :deep(pre) {
  background: var(--el-fill-color-light);
  padding: 16px;
  border-radius: 6px;
  overflow-x: auto;
  margin-top: 0;
  margin-bottom: 10px;
}

.markdown-body :deep(code) {
  background: var(--el-fill-color-light);
  padding: 0.2em 0.4em;
  border-radius: 3px;
  font-size: 85%;
  font-family: 'Courier New', monospace;
}

.markdown-body :deep(pre code) {
  background: transparent;
  padding: 0;
  font-size: 100%;
}

.markdown-body :deep(table) {
  border-collapse: collapse;
  border-spacing: 0;
  margin-top: 0;
  margin-bottom: 10px;
  width: 100%;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  padding: 6px 13px;
  border: 1px solid var(--el-border-color);
}

.markdown-body :deep(th) {
  font-weight: 600;
  background: var(--el-fill-color-lighter);
}

.markdown-body :deep(hr) {
  height: 0.25em;
  padding: 0;
  margin: 24px 0;
  background-color: var(--el-border-color);
  border: 0;
}

.markdown-body :deep(img) {
  max-width: 100%;
  height: auto;
}

.markdown-body :deep(a) {
  color: var(--el-color-primary);
  text-decoration: none;
}

.markdown-body :deep(a:hover) {
  text-decoration: underline;
}

.text-muted {
  color: var(--el-text-color-secondary);
  font-style: italic;
}
</style>
