<template>
  <div class="markdown-editor">
    <div class="editor-toolbar">
      <el-tabs v-model="activeTab" type="card" size="small">
        <el-tab-pane label="编辑" name="edit" />
        <el-tab-pane label="预览" name="preview" />
        <el-tab-pane label="分屏" name="split" />
      </el-tabs>
    </div>
    <div class="editor-content" :class="{ 'split-mode': activeTab === 'split' }">
      <!-- 编辑模式 -->
      <div v-show="activeTab === 'edit' || activeTab === 'split'" class="edit-area">
        <el-input
          v-model="content"
          type="textarea"
          :rows="rows"
          :placeholder="placeholder"
          @input="handleInput"
          resize="vertical"
        />
      </div>
      <!-- 预览模式 -->
      <div v-show="activeTab === 'preview' || activeTab === 'split'" class="preview-area markdown-body" v-html="renderedContent"></div>
    </div>
  </div>
</template>

<script setup>
/**
 * Markdown编辑器组件
 * 支持实时预览、分屏模式
 */
import { ref, computed, watch } from 'vue'
import { marked } from 'marked'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  rows: {
    type: Number,
    default: 10
  },
  placeholder: {
    type: String,
    default: '请输入Markdown格式的内容...'
  }
})

const emit = defineEmits(['update:modelValue'])

const activeTab = ref('edit')
const content = ref(props.modelValue)

// 监听外部值变化
watch(() => props.modelValue, (newVal) => {
  content.value = newVal
})

// 渲染Markdown
const renderedContent = computed(() => {
  if (!content.value) return '<p class="text-muted">暂无内容</p>'
  try {
    return marked(content.value, {
      breaks: true,
      gfm: true
    })
  } catch (e) {
    return content.value
  }
})

// 输入事件
const handleInput = (val) => {
  emit('update:modelValue', val)
}
</script>

<style scoped>
.markdown-editor {
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  overflow: hidden;
}

.editor-toolbar {
  padding: 8px 12px;
  background: var(--el-fill-color-lighter);
  border-bottom: 1px solid var(--el-border-color);
}

.editor-toolbar :deep(.el-tabs__header) {
  margin: 0;
}

.editor-toolbar :deep(.el-tabs__item) {
  height: 28px;
  line-height: 28px;
  font-size: 12px;
}

.editor-content {
  display: flex;
  min-height: 200px;
}

.editor-content.split-mode {
  gap: 0;
}

.edit-area {
  flex: 1;
  display: flex;
}

.edit-area :deep(.el-textarea) {
  height: 100%;
}

.edit-area :deep(.el-textarea__inner) {
  height: 100%;
  border: none;
  border-radius: 0;
  resize: none;
  font-family: 'Courier New', Consolas, Monaco, monospace;
  font-size: 14px;
  line-height: 1.6;
}

.preview-area {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  background: var(--el-bg-color);
  max-height: 500px;
}

.split-mode .edit-area,
.split-mode .preview-area {
  flex: 1;
  min-width: 0;
}

.split-mode .edit-area {
  border-right: 1px solid var(--el-border-color);
}

/* Markdown样式 */
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
