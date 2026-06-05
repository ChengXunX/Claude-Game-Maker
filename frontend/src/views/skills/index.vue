<template>
  <div class="skills-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>技能管理</span>
          <div class="header-actions">
            <el-button type="success" @click="handleAIGenerate" v-permission="'skills:manage'">
              <el-icon><MagicStick /></el-icon> AI 生成技能
            </el-button>
            <el-button type="primary" @click="handleCreate" v-permission="'skills:manage'">
              <el-icon><Plus /></el-icon> 手动创建
            </el-button>
          </div>
        </div>
      </template>

      <!-- 筛选区 -->
      <div class="filter-bar">
        <ProjectSelector
          v-model="selectedProjectId"
          placeholder="选择项目（查看项目技能）"
          width="200px"
          size="default"
          :save-local="false"
          @change="loadSkills"
        />
        <el-input
          v-model="searchKeyword"
          placeholder="搜索技能"
          clearable
          class="filter-item"
        />
        <el-select v-model="filterType" placeholder="技能类型" clearable class="filter-item">
          <el-option label="全部" value="" />
          <el-option label="全局技能" value="global" />
          <el-option label="项目技能" value="project" />
        </el-select>
      </div>

      <!-- 技能列表 -->
      <el-table :data="filteredSkills" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="150" show-overflow-tooltip />
        <el-table-column prop="name" label="名称" width="150" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.category || '未分类' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="triggerPattern" label="触发词" width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="handleView(row)">查看</el-button>
            <el-button type="danger" size="small" text @click="handleDelete(row)" v-permission="'skills:manage'">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && filteredSkills.length === 0" description="暂无技能" />
    </el-card>

    <!-- AI 生成技能对话框 -->
    <el-dialog v-model="aiDialogVisible" title="AI 生成技能" width="650px">
      <el-alert type="info" :closable="false" class="mb-4">
        <p style="margin: 0">描述你想要的技能功能，AI 会自动生成技能定义。你可以修改生成结果后再保存。</p>
      </el-alert>

      <el-form label-width="100px">
        <el-form-item label="技能描述" required>
          <el-input
            v-model="aiForm.description"
            type="textarea"
            :rows="4"
            placeholder="例如：当用户需要生成数据库表结构时，自动根据需求描述创建 SQL 建表语句，包含字段定义、索引和约束"
          />
        </el-form-item>
        <el-form-item label="技能分类">
          <el-select v-model="aiForm.category" placeholder="选择分类">
            <el-option label="自定义" value="custom" />
            <el-option label="代码生成" value="code-gen" />
            <el-option label="文档处理" value="document" />
            <el-option label="数据分析" value="data" />
            <el-option label="测试" value="testing" />
            <el-option label="部署" value="deploy" />
          </el-select>
        </el-form-item>
      </el-form>

      <!-- 生成结果预览 -->
      <div v-if="generatedSkill" class="generated-preview">
        <el-divider>生成结果预览</el-divider>
        <el-form label-width="100px">
          <el-form-item label="技能名称">
            <el-input v-model="generatedSkill.name" />
          </el-form-item>
          <el-form-item label="技能描述">
            <el-input v-model="generatedSkill.description" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item label="触发词">
            <el-input v-model="generatedSkill.triggerPattern" placeholder="用逗号分隔" />
          </el-form-item>
          <el-form-item label="技能内容">
            <MarkdownEditor v-model="generatedSkill.content" :rows="12" />
          </el-form-item>
          <el-form-item label="优化建议" v-if="generatedSkill.suggestions?.length">
            <ul class="suggestions-list">
              <li v-for="(s, i) in generatedSkill.suggestions" :key="i">{{ s }}</li>
            </ul>
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="aiDialogVisible = false">取消</el-button>
        <el-button type="success" @click="handleAIGenerate" :loading="generating" v-if="!generatedSkill">
          <el-icon><MagicStick /></el-icon> 生成
        </el-button>
        <el-button type="primary" @click="handleAISave" :loading="saving" v-if="generatedSkill">
          <el-icon><Check /></el-icon> 保存技能
        </el-button>
        <el-button @click="handleAIRegenerate" v-if="generatedSkill">
          <el-icon><Refresh /></el-icon> 重新生成
        </el-button>
      </template>
    </el-dialog>

    <!-- 手动创建技能对话框 -->
    <el-dialog v-model="createDialogVisible" title="手动创建技能" width="600px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="技能名称" prop="name">
          <el-input v-model="createForm.name" placeholder="如：代码审查" />
        </el-form-item>
        <el-form-item label="技能描述" prop="description">
          <el-input v-model="createForm.description" type="textarea" :rows="3" placeholder="描述技能的功能和用途" />
        </el-form-item>
        <el-form-item label="触发词">
          <el-input v-model="createForm.triggerPattern" placeholder="用逗号分隔，如：审查,review,check" />
        </el-form-item>
        <el-form-item label="技能内容" prop="content">
          <MarkdownEditor v-model="createForm.content" :rows="10" placeholder="Markdown 格式的技能定义" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateSubmit" :loading="creating">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 技能管理页面
 * 管理 Agent 的技能定义，支持 AI 辅助生成
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { skillApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MagicStick, Check, Refresh } from '@element-plus/icons-vue'
import ProjectSelector from '@/components/ProjectSelector.vue'
import MarkdownEditor from '@/components/MarkdownEditor.vue'

const router = useRouter()

const loading = ref(false)
const skills = ref([])
const selectedProjectId = ref('')
const searchKeyword = ref('')
const filterType = ref('')

// AI 生成
const aiDialogVisible = ref(false)
const generating = ref(false)
const saving = ref(false)
const aiForm = ref({ description: '', category: 'custom' })
const generatedSkill = ref(null)

// 手动创建
const createDialogVisible = ref(false)
const createFormRef = ref(null)
const creating = ref(false)
const createForm = ref({ name: '', description: '', triggerPattern: '', content: '' })
const createRules = {
  name: [{ required: true, message: '请输入技能名称', trigger: 'blur' }],
  content: [{ required: true, message: '请输入技能内容', trigger: 'blur' }]
}

/** 筛选后的技能 */
const filteredSkills = computed(() => {
  let result = skills.value
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(s =>
      s.name?.toLowerCase().includes(keyword) ||
      s.description?.toLowerCase().includes(keyword) ||
      s.id?.toLowerCase().includes(keyword)
    )
  }
  if (filterType.value === 'global') {
    result = result.filter(s => !s.projectId)
  } else if (filterType.value === 'project') {
    result = result.filter(s => s.projectId)
  }
  return result
})

/** 加载技能列表 */
const loadSkills = async () => {
  loading.value = true
  try {
    const params = {}
    if (selectedProjectId.value) {
      params.projectId = selectedProjectId.value
    }
    const data = await skillApi.getAll(params)
    skills.value = data || []
  } catch (error) {
    console.error('加载技能列表失败:', error)
    skills.value = []
  } finally {
    loading.value = false
  }
}

/** 查看技能详情 */
const handleView = (skill) => {
  router.push(`/skills/${skill.id}`)
}

/** 打开手动创建对话框 */
const handleCreate = () => {
  createForm.value = { name: '', description: '', triggerPattern: '', content: '' }
  createDialogVisible.value = true
}

/** 打开 AI 生成对话框 */
const handleAIGenerate = () => {
  if (!aiForm.value.description) {
    aiDialogVisible.value = true
    return
  }
  generateSkill()
}

/** AI 生成技能 */
const generateSkill = async () => {
  if (!aiForm.value.description) {
    ElMessage.warning('请填写技能描述')
    return
  }
  generating.value = true
  try {
    const result = await skillApi.generate(aiForm.value)
    generatedSkill.value = result
    ElMessage.success('技能定义已生成，请检查后保存')
  } catch (error) {
    ElMessage.error('生成失败：' + (error.message || '未知错误'))
  } finally {
    generating.value = false
  }
}

/** 重新生成 */
const handleAIRegenerate = () => {
  generatedSkill.value = null
  generateSkill()
}

/** 保存 AI 生成的技能 */
const handleAISave = async () => {
  if (!generatedSkill.value?.name || !generatedSkill.value?.content) {
    ElMessage.warning('技能名称和内容不能为空')
    return
  }
  saving.value = true
  try {
    await skillApi.create(generatedSkill.value)
    ElMessage.success('技能创建成功')
    aiDialogVisible.value = false
    generatedSkill.value = null
    aiForm.value = { description: '', category: 'custom' }
    loadSkills()
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

/** 提交手动创建 */
const handleCreateSubmit = async () => {
  try {
    await createFormRef.value.validate()
    creating.value = true
    await skillApi.create(createForm.value)
    ElMessage.success('技能创建成功')
    createDialogVisible.value = false
    loadSkills()
  } catch (error) {
    if (error !== false) {
      ElMessage.error('创建失败')
    }
  } finally {
    creating.value = false
  }
}

/** 删除技能 */
const handleDelete = async (skill) => {
  try {
    await ElMessageBox.confirm(`确定要删除技能 "${skill.name}" 吗？`, '删除确认', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    })
    await skillApi.delete(skill.id)
    ElMessage.success('技能已删除')
    loadSkills()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadSkills()
})
</script>

<style scoped>
.skills-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.filter-item {
  flex: 1;
  min-width: 150px;
}

.mb-4 {
  margin-bottom: 16px;
}

.generated-preview {
  margin-top: 8px;
}

.suggestions-list {
  margin: 0;
  padding-left: 20px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.suggestions-list li {
  margin-bottom: 4px;
}

@media (max-width: 767px) {
  .skills-page {
    padding: 12px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .filter-bar {
    flex-direction: column;
  }

  .filter-item {
    width: 100%;
  }

  :deep(.el-dialog) {
    width: 90% !important;
  }
}
</style>
