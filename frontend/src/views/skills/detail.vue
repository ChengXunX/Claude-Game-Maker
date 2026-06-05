<template>
  <div class="skill-detail-page">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>技能详情</span>
          <el-button @click="router.back()">返回</el-button>
        </div>
      </template>

      <template v-if="skill">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="ID">{{ skill.id }}</el-descriptions-item>
          <el-descriptions-item label="名称">{{ skill.name }}</el-descriptions-item>
          <el-descriptions-item label="分类">
            <el-tag size="small">{{ skill.category || '未分类' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="触发词">{{ skill.triggerPattern || '-' }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ skill.description || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 技能内容 - Markdown渲染 -->
        <div v-if="skill.content" class="skill-content-section">
          <h4>技能内容</h4>
          <div class="markdown-body" v-html="renderedContent"></div>
        </div>
      </template>

      <el-empty v-else-if="!loading" description="技能不存在" />
    </el-card>
  </div>
</template>

<script setup>
/**
 * 技能详情页面
 * 查看技能详细信息，支持Markdown渲染
 */
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { skillApi } from '@/api'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const skill = ref(null)

// 渲染Markdown内容
const renderedContent = computed(() => {
  if (!skill.value?.content) return ''
  try {
    return marked(skill.value.content, {
      breaks: true,
      gfm: true
    })
  } catch (e) {
    return skill.value.content
  }
})

/** 加载技能详情 */
const loadSkill = async () => {
  const id = route.params.id
  if (!id) return

  loading.value = true
  try {
    const data = await skillApi.getById(id)
    skill.value = data
  } catch (error) {
    ElMessage.error('加载技能详情失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadSkill()
})
</script>

<style scoped>
.skill-detail-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.skill-content-section {
  margin-top: 24px;
}

.skill-content-section h4 {
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--el-border-color);
}

/* Markdown样式 */
.markdown-body {
  padding: 20px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  line-height: 1.6;
}

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
</style>
