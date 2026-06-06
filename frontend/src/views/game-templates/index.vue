<template>
  <div class="game-templates-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>游戏模板库</span>
          <div class="header-actions">
            <el-button type="primary" @click="handleCreate" v-permission="'projects:manage'">
              <el-icon><Plus /></el-icon> 新增模板
            </el-button>
            <el-button @click="loadTemplates" :loading="loading">
              <el-icon><Refresh /></el-icon> 刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- 搜索和筛选 -->
      <div class="filter-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索游戏模板..."
          clearable
          style="width: 300px"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <!-- 模板列表 -->
      <el-row :gutter="16">
        <el-col :span="8" v-for="template in filteredTemplates" :key="template.id">
          <el-card class="template-card" shadow="hover">
            <template #header>
              <div class="template-header">
                <span class="template-name">{{ template.name }}</span>
                <el-tag size="small">{{ template.id }}</el-tag>
              </div>
            </template>

            <div class="template-content">
              <p class="template-desc">{{ template.description }}</p>

              <div class="template-keywords">
                <el-tag
                  v-for="keyword in template.keywords.slice(0, 5)"
                  :key="keyword"
                  size="small"
                  type="info"
                  class="keyword-tag"
                >
                  {{ keyword }}
                </el-tag>
              </div>
            </div>

            <div class="template-actions">
              <el-button type="primary" size="small" @click="handleUseTemplate(template)">
                使用模板
              </el-button>
              <el-button size="small" @click="handlePreview(template)">
                预览
              </el-button>
              <el-button type="warning" size="small" @click="handleEdit(template)" v-permission="'projects:manage'">
                编辑
              </el-button>
              <el-button type="danger" size="small" @click="handleDelete(template)" v-permission="'projects:manage'">
                删除
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-empty v-if="!loading && filteredTemplates.length === 0" description="暂无游戏模板" />
    </el-card>

    <!-- 新增/编辑模板对话框 -->
    <el-dialog v-model="formDialogVisible" :title="isEdit ? '编辑模板' : '新增模板'" width="700px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="模板ID" prop="id">
          <el-input v-model="form.id" placeholder="如：my-game" :disabled="isEdit" />
          <div class="form-tip">唯一标识，只能使用小写字母、数字和连字符</div>
        </el-form-item>
        <el-form-item label="模板名称" prop="name">
          <el-input v-model="form.name" placeholder="如：我的游戏模板" />
        </el-form-item>
        <el-form-item label="模板描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="描述模板的类型和特点" />
        </el-form-item>
        <el-form-item label="技能名称" prop="skillName">
          <el-input v-model="form.skillName" placeholder="如：game-template-my-game" />
          <div class="form-tip">对应的技能文件名称（不含.md后缀）</div>
        </el-form-item>
        <el-form-item label="关键词" prop="keywords">
          <el-select
            v-model="form.keywords"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="输入关键词后回车"
            style="width: 100%"
          >
            <el-option v-for="kw in commonKeywords" :key="kw" :label="kw" :value="kw" />
          </el-select>
        </el-form-item>
        <el-form-item label="模板内容">
          <MarkdownEditor v-model="form.content" :rows="10" placeholder="模板的详细内容（Markdown格式）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ isEdit ? '保存修改' : '创建模板' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 模板预览对话框 -->
    <el-dialog v-model="previewDialogVisible" :title="previewTemplate?.name" width="600px">
      <div v-if="previewTemplate" class="preview-content">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="模板ID">{{ previewTemplate.id }}</el-descriptions-item>
          <el-descriptions-item label="模板名称">{{ previewTemplate.name }}</el-descriptions-item>
          <el-descriptions-item label="描述">{{ previewTemplate.description }}</el-descriptions-item>
          <el-descriptions-item label="技能名称">{{ previewTemplate.skillName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="关键词">
            <el-tag v-for="keyword in previewTemplate.keywords" :key="keyword" size="small" class="keyword-tag">
              {{ keyword }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <div v-if="previewTemplate.content" class="preview-markdown">
          <h4>模板内容：</h4>
          <!-- 使用 MarkdownRenderer 组件渲染模板内容（只读展示） -->
          <div class="markdown-preview-container">
            <MarkdownRenderer :content="previewTemplate.content" />
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="previewDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleUseTemplate(previewTemplate)">使用此模板</el-button>
      </template>
    </el-dialog>

    <!-- 智能匹配对话框 -->
    <el-dialog v-model="matchDialogVisible" title="智能匹配游戏模板" width="600px">
      <el-form label-width="100px">
        <el-form-item label="游戏描述">
          <el-input
            v-model="gameDescription"
            type="textarea"
            :rows="4"
            placeholder="描述你想要创建的游戏，例如：做一个塔防游戏，猫咪防御入侵者"
          />
        </el-form-item>
      </el-form>

      <div v-if="matchedTemplates.length > 0" class="matched-templates">
        <h4>匹配结果：</h4>
        <div v-for="template in matchedTemplates" :key="template.id" class="matched-item">
          <el-tag :type="template.id === bestMatch?.id ? 'success' : 'info'" size="small">
            {{ template.name }}
          </el-tag>
          <span class="match-desc">{{ template.description }}</span>
        </div>
      </div>

      <template #footer>
        <el-button @click="matchDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleMatch" :loading="matching">
          智能匹配
        </el-button>
        <el-button type="success" v-if="bestMatch" @click="handleUseTemplate(bestMatch)">
          使用最佳匹配
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 游戏模板库页面
 * 展示和管理游戏模板
 *
 * 功能：
 * - 浏览游戏模板
 * - 搜索模板
 * - 智能匹配模板
 * - 使用模板创建游戏
 * - 新增/编辑/删除模板（需要 projects:manage 权限）
 *
 * 组件复用说明：
 * - MarkdownEditor: 可编辑的 Markdown 编辑器（支持编辑/预览/分屏模式）
 *   位置: @/components/MarkdownEditor.vue
 *   用途: 编辑模板内容
 * - MarkdownRenderer: 只读 Markdown 渲染器（纯展示，无编辑功能）
 *   位置: @/components/MarkdownRenderer.vue
 *   用途: 预览模板内容
 *
 * @author chengxun
 * @since 1.0.0
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { gameTemplateApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

/**
 * Markdown 组件
 * - MarkdownEditor: 编辑模式，用于创建/编辑模板内容
 * - MarkdownRenderer: 只读模式，用于预览模板内容
 */
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'

const router = useRouter()

const loading = ref(false)
const templates = ref([])
const searchKeyword = ref('')

/** 表单对话框 */
const formDialogVisible = ref(false)
const formRef = ref(null)
const submitting = ref(false)
const isEdit = ref(false)
const form = ref({
  id: '',
  name: '',
  description: '',
  skillName: '',
  keywords: [],
  content: ''
})
const formRules = {
  id: [
    { required: true, message: '请输入模板ID', trigger: 'blur' },
    { pattern: /^[a-z0-9-]+$/, message: '只能使用小写字母、数字和连字符', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入模板名称', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入模板描述', trigger: 'blur' }
  ]
}

/** 常用关键词 */
const commonKeywords = [
  'RPG', 'FPS', 'RTS', 'SLG', 'MMORPG',
  '平台跳跃', '塔防', '射击', '赛车', '益智',
  '策略', '冒险', '动作', '模拟', '沙盒',
  '多人', '单机', '联机', '合作', '对战',
  '2D', '3D', '像素', '卡通', '写实'
]

/** 智能匹配 */
const matchDialogVisible = ref(false)
const matching = ref(false)
const gameDescription = ref('')
const matchedTemplates = ref([])
const bestMatch = ref(null)

/** 模板预览 */
const previewDialogVisible = ref(false)
const previewTemplate = ref(null)

/** 筛选后的模板 */
const filteredTemplates = computed(() => {
  if (!searchKeyword.value) return templates.value

  const keyword = searchKeyword.value.toLowerCase()
  return templates.value.filter(t =>
    t.name.toLowerCase().includes(keyword) ||
    t.description.toLowerCase().includes(keyword) ||
    t.keywords.some(k => k.toLowerCase().includes(keyword))
  )
})

/** 加载模板列表 */
const loadTemplates = async () => {
  loading.value = true
  try {
    const data = await gameTemplateApi.getAll()
    templates.value = data || []
  } catch (error) {
    ElMessage.error('加载模板失败')
  } finally {
    loading.value = false
  }
}

/** 使用模板 */
const handleUseTemplate = (template) => {
  ElMessage.success(`已选择模板: ${template.name}`)
  matchDialogVisible.value = false
  previewDialogVisible.value = false
  // 跳转到创建游戏页面，携带模板信息
  router.push({
    path: '/projects',
    query: { template: template.id }
  })
}

/** 预览模板 */
const handlePreview = (template) => {
  previewTemplate.value = template
  previewDialogVisible.value = true
}

/** 新增模板 */
const handleCreate = () => {
  isEdit.value = false
  form.value = {
    id: '',
    name: '',
    description: '',
    skillName: '',
    keywords: [],
    content: ''
  }
  formDialogVisible.value = true
}

/** 编辑模板 */
const handleEdit = (template) => {
  isEdit.value = true
  form.value = {
    id: template.id,
    name: template.name,
    description: template.description,
    skillName: template.skillName || '',
    keywords: [...(template.keywords || [])],
    content: template.content || ''
  }
  formDialogVisible.value = true
}

/** 删除模板 */
const handleDelete = async (template) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除模板 "${template.name}" 吗？`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )

    await gameTemplateApi.delete(template.id)
    ElMessage.success('模板删除成功')
    loadTemplates()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败：' + (error.message || '未知错误'))
    }
  }
}

/** 提交表单 */
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true

    if (isEdit.value) {
      await gameTemplateApi.update(form.value.id, form.value)
      ElMessage.success('模板更新成功')
    } else {
      await gameTemplateApi.create(form.value)
      ElMessage.success('模板创建成功')
    }

    formDialogVisible.value = false
    loadTemplates()
  } catch (error) {
    if (error !== false) {
      ElMessage.error('保存失败：' + (error.message || '未知错误'))
    }
  } finally {
    submitting.value = false
  }
}

/** 智能匹配 */
const handleMatch = async () => {
  if (!gameDescription.value.trim()) {
    ElMessage.warning('请输入游戏描述')
    return
  }

  matching.value = true
  try {
    const data = await gameTemplateApi.match(gameDescription.value)
    matchedTemplates.value = data || []
    bestMatch.value = data && data.length > 0 ? data[0] : null

    if (matchedTemplates.value.length === 0) {
      ElMessage.info('未找到匹配的模板')
    }
  } catch (error) {
    ElMessage.error('匹配失败')
  } finally {
    matching.value = false
  }
}

onMounted(() => {
  loadTemplates()
})
</script>

<style scoped>
.game-templates-page {
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
}

.filter-bar {
  margin-bottom: 20px;
}

.template-card {
  margin-bottom: 16px;
}

.template-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.template-name {
  font-weight: 600;
}

.template-content {
  min-height: 100px;
}

.template-desc {
  font-size: 13px;
  color: #666;
  margin-bottom: 12px;
  line-height: 1.5;
}

.template-keywords {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.keyword-tag {
  font-size: 11px;
}

.template-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.form-tip {
  color: #909399;
  font-size: 12px;
  margin-top: 5px;
}

.matched-templates {
  margin-top: 16px;
}

.matched-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
}

.match-desc {
  font-size: 13px;
  color: #666;
}

.preview-content {
  padding: 10px 0;
}

.preview-content .keyword-tag {
  margin-right: 4px;
  margin-bottom: 4px;
}

.preview-markdown {
  margin-top: 16px;
}

.preview-markdown h4 {
  margin-bottom: 8px;
}

/**
 * Markdown 预览容器样式
 * 为 MarkdownRenderer 组件提供容器样式
 */
.markdown-preview-container {
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  padding: 16px;
  max-height: 400px;
  overflow-y: auto;
}
</style>
