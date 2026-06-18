<template>
  <div class="skills-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>技能管理</span>
          <div class="header-actions">
            <el-button type="info" @click="handleDiscoverSkills" v-permission="'skills:view'">
              <el-icon><Search /></el-icon> 扫描项目Skill
            </el-button>
            <el-button @click="handleExport" v-permission="'skills:manage'" :disabled="selectedSkills.length === 0">
              <el-icon><Download /></el-icon> 导出{{ selectedSkills.length > 0 ? `(${selectedSkills.length})` : '' }}
            </el-button>
            <el-button @click="handleImport" v-permission="'skills:manage'">
              <el-icon><Upload /></el-icon> 导入
            </el-button>
            <el-button type="success" @click="handleAIGenerate" v-permission="'skills:manage'">
              <el-icon><MagicStick /></el-icon> AI 生成
            </el-button>
            <el-button type="primary" @click="handleCreate" v-permission="'skills:manage'">
              <el-icon><Plus /></el-icon> 手动创建
            </el-button>
          </div>
        </div>
      </template>

      <!-- 统计卡片 -->
      <el-row :gutter="16" class="stat-cards">
        <el-col :xs="12" :sm="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value">{{ skills.length }}</div>
            <div class="stat-label">技能总数</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value primary">{{ globalSkillsCount }}</div>
            <div class="stat-label">全局技能</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value success">{{ projectSkillsCount }}</div>
            <div class="stat-label">项目技能</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value warning">{{ categoriesCount }}</div>
            <div class="stat-label">技能分类</div>
          </el-card>
        </el-col>
      </el-row>

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
          placeholder="搜索技能名称、描述、触发词..."
          clearable
          class="filter-item"
          :prefix-icon="Search"
        />
        <el-select v-model="filterCategory" placeholder="技能分类" clearable class="filter-item" style="width: 150px">
          <el-option label="全部分类" value="" />
          <el-option v-for="cat in categories" :key="cat" :label="cat" :value="cat" />
        </el-select>
        <el-select v-model="filterType" placeholder="技能类型" clearable class="filter-item" style="width: 120px">
          <el-option label="全部" value="" />
          <el-option label="全局技能" value="global" />
          <el-option label="项目技能" value="project" />
        </el-select>
        <el-radio-group v-model="viewMode" class="view-toggle">
          <el-radio-button value="card">
            <el-icon><Grid /></el-icon>
          </el-radio-button>
          <el-radio-button value="table">
            <el-icon><List /></el-icon>
          </el-radio-button>
        </el-radio-group>
      </div>

      <!-- 卡片视图 -->
      <div v-if="viewMode === 'card'" class="skills-grid" v-loading="loading">
        <el-card
          v-for="skill in filteredSkills"
          :key="skill.id"
          class="skill-card"
          shadow="hover"
          @click="handleViewDetail(skill)"
        >
          <div class="skill-card-header">
            <el-tag :type="getCategoryType(skill.category)" size="small" effect="plain">
              {{ skill.category || '未分类' }}
            </el-tag>
            <el-checkbox
              v-model="skill._selected"
              @click.stop
              @change="handleSelectSkill(skill)"
            />
          </div>
          <h4 class="skill-name">{{ skill.name }}</h4>
          <p class="skill-desc">{{ skill.description || '暂无描述' }}</p>
          <div class="skill-meta">
            <span v-if="skill.triggerPattern" class="meta-item">
              <el-icon><Promotion /></el-icon>
              {{ truncateText(skill.triggerPattern, 20) }}
            </span>
            <span class="meta-item">
              <el-icon><Folder /></el-icon>
              {{ skill.projectId ? '项目' : '全局' }}
            </span>
          </div>
          <div class="skill-actions">
            <el-button type="primary" size="small" text @click.stop="handleEdit(skill)">
              编辑
            </el-button>
            <el-button type="danger" size="small" text @click.stop="handleDelete(skill)">
              删除
            </el-button>
          </div>
        </el-card>
        <el-empty v-if="!loading && filteredSkills.length === 0" description="暂无技能" />
      </div>

      <!-- 列表视图 -->
      <div v-else>
        <el-table
          :data="filteredSkills"
          v-loading="loading"
          stripe
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="45" />
          <el-table-column prop="name" label="名称" width="150">
            <template #default="{ row }">
              <el-link type="primary" @click="handleViewDetail(row)">{{ row.name }}</el-link>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
          <el-table-column prop="category" label="分类" width="100">
            <template #default="{ row }">
              <el-tag :type="getCategoryType(row.category)" size="small">{{ row.category || '未分类' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="triggerPattern" label="触发词" width="150" show-overflow-tooltip />
          <el-table-column label="类型" width="80">
            <template #default="{ row }">
              <el-tag :type="row.projectId ? 'warning' : 'success'" size="small" effect="plain">
                {{ row.projectId ? '项目' : '全局' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" size="small" text @click="handleViewDetail(row)">详情</el-button>
              <el-button type="danger" size="small" text @click="handleDelete(row)" v-permission="'skills:manage'">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- 技能详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :title="currentSkill?.name || '技能详情'"
      size="500px"
      direction="rtl"
    >
      <template v-if="currentSkill">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="技能ID">{{ currentSkill.id }}</el-descriptions-item>
          <el-descriptions-item label="名称">{{ currentSkill.name }}</el-descriptions-item>
          <el-descriptions-item label="分类">
            <el-tag :type="getCategoryType(currentSkill.category)" size="small">
              {{ currentSkill.category || '未分类' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="类型">
            <el-tag :type="currentSkill.projectId ? 'warning' : 'success'" size="small">
              {{ currentSkill.projectId ? '项目技能' : '全局技能' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="触发词">{{ currentSkill.triggerPattern || '无' }}</el-descriptions-item>
          <el-descriptions-item label="描述">{{ currentSkill.description || '暂无描述' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider>技能内容</el-divider>
        <div class="skill-content-preview">
          <MarkdownRenderer :content="currentSkill.content || '暂无内容'" />
        </div>

        <div class="drawer-actions">
          <el-button type="primary" @click="handleEdit(currentSkill)" v-permission="'skills:manage'">
            <el-icon><Edit /></el-icon> 编辑
          </el-button>
          <el-button @click="handleCopyContent(currentSkill)">
            <el-icon><CopyDocument /></el-icon> 复制内容
          </el-button>
          <el-button type="danger" @click="handleDelete(currentSkill)" v-permission="'skills:manage'">
            <el-icon><Delete /></el-icon> 删除
          </el-button>
        </div>
      </template>
    </el-drawer>

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
        <el-button type="success" @click="generateSkill" :loading="generating" v-if="!generatedSkill">
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

    <!-- 手动创建/编辑技能对话框 -->
    <el-dialog v-model="createDialogVisible" :title="isEdit ? '编辑技能' : '手动创建技能'" width="600px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="技能名称" prop="name">
          <el-input v-model="createForm.name" placeholder="如：代码审查" />
        </el-form-item>
        <el-form-item label="技能分类">
          <el-select v-model="createForm.category" placeholder="选择分类" filterable allow-create>
            <el-option label="自定义" value="custom" />
            <el-option label="代码生成" value="code-gen" />
            <el-option label="文档处理" value="document" />
            <el-option label="数据分析" value="data" />
            <el-option label="测试" value="testing" />
            <el-option label="部署" value="deploy" />
          </el-select>
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
        <el-button type="primary" @click="handleCreateSubmit" :loading="creating">
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 导入对话框 -->
    <el-dialog v-model="importDialogVisible" title="导入技能" width="500px">
      <el-alert type="info" :closable="false" class="mb-4">
        <p style="margin: 0">支持 JSON 格式的技能文件导入。可以是单个技能或技能数组。</p>
      </el-alert>
      <el-upload
        ref="uploadRef"
        :auto-upload="false"
        :limit="1"
        accept=".json"
        :on-change="handleFileChange"
        :on-exceed="() => ElMessage.warning('只能上传一个文件')"
      >
        <template #trigger>
          <el-button type="primary">选择文件</el-button>
        </template>
        <template #tip>
          <div class="el-upload__tip">只能上传 JSON 文件</div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleImportSubmit" :loading="importing" :disabled="!importFile">
          导入
        </el-button>
      </template>
    </el-dialog>

    <!-- Skill 发现对话框 -->
    <el-dialog v-model="discoverDialogVisible" title="项目 Skill 扫描结果" width="700px">
      <el-table :data="discoveredSkills" stripe v-loading="discoverLoading" empty-text="未发现文件系统 Skill">
        <el-table-column prop="name" label="名称" width="180" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="filePath" label="文件路径" show-overflow-tooltip />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleImportDiscovered(row)">导入</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="discoverDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 技能管理页面
 * 管理 Agent 的技能定义，支持 AI 辅助生成
 *
 * 优化内容：
 * - 卡片/列表视图切换
 * - 统计概览
 * - 技能详情预览（侧边抽屉）
 * - 批量选择和导出
 * - 技能导入
 * - 编辑功能
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { skillApi, skillDiscoveryApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  MagicStick, Check, Refresh, Search, Grid, List,
  Edit, Delete, CopyDocument, Download, Upload,
  Promotion, Folder
} from '@element-plus/icons-vue'
import ProjectSelector from '@/components/ProjectSelector.vue'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'

const router = useRouter()

const loading = ref(false)
const skills = ref([])
const selectedProjectId = ref('')
const searchKeyword = ref('')
const filterCategory = ref('')
const filterType = ref('')
const viewMode = ref('card') // card | table

// 统计
const globalSkillsCount = computed(() => skills.value.filter(s => !s.projectId).length)
const projectSkillsCount = computed(() => skills.value.filter(s => s.projectId).length)
const categoriesCount = computed(() => new Set(skills.value.map(s => s.category).filter(Boolean)).size)
const categories = computed(() => [...new Set(skills.value.map(s => s.category).filter(Boolean))].sort())

// 批量选择
const selectedSkills = ref([])

// 详情抽屉
const drawerVisible = ref(false)
const currentSkill = ref(null)

// AI 生成
const aiDialogVisible = ref(false)
const generating = ref(false)
const saving = ref(false)
const aiForm = ref({ description: '', category: 'custom' })
const generatedSkill = ref(null)

// 手动创建/编辑
const createDialogVisible = ref(false)
const createFormRef = ref(null)
const creating = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const createForm = ref({ name: '', description: '', triggerPattern: '', content: '', category: 'custom' })
const createRules = {
  name: [{ required: true, message: '请输入技能名称', trigger: 'blur' }],
  content: [{ required: true, message: '请输入技能内容', trigger: 'blur' }]
}

// 导入
const importDialogVisible = ref(false)
const importing = ref(false)
const importFile = ref(null)
const uploadRef = ref(null)

/** 筛选后的技能 */
const filteredSkills = computed(() => {
  let result = skills.value
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(s =>
      s.name?.toLowerCase().includes(keyword) ||
      s.description?.toLowerCase().includes(keyword) ||
      s.id?.toLowerCase().includes(keyword) ||
      s.triggerPattern?.toLowerCase().includes(keyword)
    )
  }
  if (filterCategory.value) {
    result = result.filter(s => s.category === filterCategory.value)
  }
  if (filterType.value === 'global') {
    result = result.filter(s => !s.projectId)
  } else if (filterType.value === 'project') {
    result = result.filter(s => s.projectId)
  }
  return result
})

/** 截断文本 */
const truncateText = (text, maxLen) => {
  if (!text) return ''
  return text.length > maxLen ? text.substring(0, maxLen) + '...' : text
}

/** 获取分类标签类型 */
const getCategoryType = (category) => {
  const typeMap = {
    'code-gen': 'primary',
    'document': 'success',
    'data': 'warning',
    'testing': 'danger',
    'deploy': 'info',
    'custom': ''
  }
  return typeMap[category] || ''
}

/** 加载技能列表 */
const loadSkills = async () => {
  loading.value = true
  try {
    const params = {}
    if (selectedProjectId.value) {
      params.projectId = selectedProjectId.value
    }
    const data = await skillApi.getAll(params)
    skills.value = (data || []).map(s => ({ ...s, _selected: false }))
  } catch (error) {
    console.error('加载技能列表失败:', error)
    skills.value = []
  } finally {
    loading.value = false
  }
}

/** 选择技能 */
const handleSelectSkill = (skill) => {
  if (skill._selected) {
    selectedSkills.value.push(skill)
  } else {
    selectedSkills.value = selectedSkills.value.filter(s => s.id !== skill.id)
  }
}

/** 表格多选变化 */
const handleSelectionChange = (selection) => {
  selectedSkills.value = selection
  // 更新卡片视图的选中状态
  skills.value.forEach(s => {
    s._selected = selection.some(sel => sel.id === s.id)
  })
}

/** 查看技能详情 */
const handleViewDetail = (skill) => {
  currentSkill.value = skill
  drawerVisible.value = true
}

/** 打开手动创建对话框 */
const handleCreate = () => {
  isEdit.value = false
  editingId.value = null
  createForm.value = { name: '', description: '', triggerPattern: '', content: '', category: 'custom' }
  createDialogVisible.value = true
}

// ===== Skill 发现 =====
const discoverDialogVisible = ref(false)
const discoverLoading = ref(false)
const discoveredSkills = ref([])

const handleDiscoverSkills = async () => {
  if (!selectedProjectId.value) {
    ElMessage.warning('请先选择项目')
    return
  }
  discoverLoading.value = true
  discoverDialogVisible.value = true
  discoveredSkills.value = []
  try {
    const res = await skillDiscoveryApi.discover(selectedProjectId.value)
    if (res.data?.success) {
      discoveredSkills.value = res.data.skills || []
      ElMessage.success(`发现 ${discoveredSkills.value.length} 个 Skill`)
    }
  } catch (e) {
    ElMessage.error('扫描失败')
  } finally {
    discoverLoading.value = false
  }
}

/** 导入发现的 Skill 为正式技能 */
const handleImportDiscovered = async (skill) => {
  try {
    await skillApi.create({
      name: skill.name,
      description: skill.description || `从文件 ${skill.filePath} 导入`,
      content: skill.content || '',
      category: skill.category || 'custom',
      triggerPattern: skill.triggerPattern || ''
    })
    ElMessage.success(`技能 "${skill.name}" 导入成功`)
    loadSkills()
  } catch (e) {
    ElMessage.error('导入失败')
  }
}

/** 打开编辑对话框 */
const handleEdit = (skill) => {
  isEdit.value = true
  editingId.value = skill.id
  createForm.value = {
    name: skill.name || '',
    description: skill.description || '',
    triggerPattern: skill.triggerPattern || '',
    content: skill.content || '',
    category: skill.category || 'custom'
  }
  createDialogVisible.value = true
  drawerVisible.value = false
}

/** 打开 AI 生成对话框 */
const handleAIGenerate = () => {
  aiForm.value = { description: '', category: 'custom' }
  generatedSkill.value = null
  aiDialogVisible.value = true
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
    loadSkills()
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

/** 提交手动创建/编辑 */
const handleCreateSubmit = async () => {
  try {
    await createFormRef.value.validate()
    creating.value = true
    if (isEdit.value) {
      await skillApi.update(editingId.value, createForm.value)
      ElMessage.success('技能更新成功')
    } else {
      await skillApi.create(createForm.value)
      ElMessage.success('技能创建成功')
    }
    createDialogVisible.value = false
    loadSkills()
  } catch (error) {
    if (error !== false) {
      ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
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
    drawerVisible.value = false
    loadSkills()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

/** 复制技能内容 */
const handleCopyContent = (skill) => {
  if (skill.content) {
    navigator.clipboard.writeText(skill.content)
    ElMessage.success('内容已复制到剪贴板')
  }
}

/** 导出技能 */
const handleExport = () => {
  const data = selectedSkills.value.length > 0
    ? selectedSkills.value.map(s => ({
        name: s.name,
        description: s.description,
        category: s.category,
        triggerPattern: s.triggerPattern,
        content: s.content
      }))
    : skills.value

  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `skills-export-${new Date().toISOString().slice(0, 10)}.json`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success(`已导出 ${data.length} 个技能`)
}

/** 打开导入对话框 */
const handleImport = () => {
  importFile.value = null
  importDialogVisible.value = true
}

/** 文件变化 */
const handleFileChange = (file) => {
  importFile.value = file.raw
}

/** 提交导入 */
const handleImportSubmit = async () => {
  if (!importFile.value) {
    ElMessage.warning('请选择文件')
    return
  }

  importing.value = true
  try {
    const text = await importFile.value.text()
    const data = JSON.parse(text)
    const skillsToImport = Array.isArray(data) ? data : [data]

    let successCount = 0
    for (const skill of skillsToImport) {
      if (skill.name && skill.content) {
        await skillApi.create(skill)
        successCount++
      }
    }

    ElMessage.success(`成功导入 ${successCount} 个技能`)
    importDialogVisible.value = false
    loadSkills()
  } catch (error) {
    ElMessage.error('导入失败：' + (error.message || '文件格式错误'))
  } finally {
    importing.value = false
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
  flex-wrap: wrap;
}

/* 统计卡片 */
.stat-cards {
  margin-bottom: 16px;
}

.stat-card {
  text-align: center;
  cursor: default;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: var(--el-text-color-primary);
}

.stat-value.primary {
  color: var(--el-color-primary);
}

.stat-value.success {
  color: var(--el-color-success);
}

.stat-value.warning {
  color: var(--el-color-warning);
}

.stat-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

/* 筛选区 */
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}

.filter-item {
  flex: 1;
  min-width: 150px;
}

.view-toggle {
  flex-shrink: 0;
}

/* 卡片视图 */
.skills-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  min-height: 200px;
}

.skill-card {
  cursor: pointer;
  transition: all 0.3s;
}

.skill-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--el-box-shadow-light);
}

.skill-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.skill-name {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.skill-desc {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.skill-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 12px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.skill-actions {
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid var(--el-border-color-lighter);
  padding-top: 12px;
}

/* 详情抽屉 */
.skill-content-preview {
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  padding: 16px;
  margin-top: 16px;
  max-height: 400px;
  overflow-y: auto;
}

.drawer-actions {
  display: flex;
  gap: 8px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}

/* 其他 */
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

/* 响应式 */
@media (max-width: 767px) {
  .skills-page {
    padding: 12px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
  }

  .header-actions .el-button {
    flex: 1;
  }

  .filter-bar {
    flex-direction: column;
  }

  .filter-item {
    width: 100%;
  }

  .skills-grid {
    grid-template-columns: 1fr;
  }

  :deep(.el-dialog),
  :deep(.el-drawer) {
    width: 90% !important;
  }
}
</style>
