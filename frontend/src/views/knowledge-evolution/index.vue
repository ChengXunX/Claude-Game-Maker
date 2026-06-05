<template>
  <div class="knowledge-evolution-page">
    <!-- 顶部概览 -->
    <el-row :gutter="16" class="overview-cards">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #409eff">
            <el-icon :size="28"><Document /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalDocuments || 0 }}</div>
            <div class="stat-label">知识文档</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #67c23a">
            <el-icon :size="28"><Tools /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalSkills || 0 }}</div>
            <div class="stat-label">Agent 技能</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #e6a23c">
            <el-icon :size="28"><TrendCharts /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.evolutionCount || 0 }}</div>
            <div class="stat-label">进化次数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #909399">
            <el-icon :size="28"><Timer /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.lastEvolution || '从未' }}</div>
            <div class="stat-label">上次进化</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 核心功能区 -->
    <el-card class="section-card">
      <template #header>
        <div class="section-header">
          <div>
            <el-icon :size="20" color="#409EFF"><MagicStick /></el-icon>
            <span>知识进化引擎</span>
          </div>
          <el-tag type="success" v-if="stats.autoEvolutionEnabled">自动进化已开启</el-tag>
          <el-tag type="info" v-else>自动进化已关闭</el-tag>
        </div>
      </template>

      <el-alert type="info" :closable="false" class="mb-4" show-icon>
        <template #title>什么是知识进化？</template>
        <template #default>
          <p style="margin: 0">知识进化是 Agent 的核心能力之一。系统会自动从 Agent 的工作成果中学习，不断优化知识库，让 Agent 在后续任务中表现更好。</p>
        </template>
      </el-alert>

      <el-row :gutter="16">
        <!-- 自动进化 -->
        <el-col :span="8">
          <el-card shadow="hover" class="feature-card">
            <div class="feature-icon">
              <el-icon :size="40" color="#409EFF"><Refresh /></el-icon>
            </div>
            <h3>整理知识库</h3>
            <p class="feature-desc">自动整理和去重知识库中的文档，提取关键信息，建立知识索引。让 Agent 能够更快地找到相关知识。</p>
            <ul class="feature-list">
              <li>自动提取文档中的关键知识点</li>
              <li>建立知识之间的关联关系</li>
              <li>去除重复和过时的知识</li>
            </ul>
            <el-button type="primary" @click="handleOrganize" :loading="organizing" class="feature-btn">
              <el-icon><Refresh /></el-icon> 立即整理
            </el-button>
          </el-card>
        </el-col>

        <!-- 自进化 -->
        <el-col :span="8">
          <el-card shadow="hover" class="feature-card">
            <div class="feature-icon">
              <el-icon :size="40" color="#67c23a"><MagicStick /></el-icon>
            </div>
            <h3>触发自进化</h3>
            <p class="feature-desc">让 Agent 从过去的工作经验中学习，总结最佳实践，优化工作流程。这是 Agent "变聪明" 的核心功能。</p>
            <ul class="feature-list">
              <li>分析历史任务的成功/失败模式</li>
              <li>总结最佳实践和常见问题</li>
              <li>优化 Agent 的决策策略</li>
            </ul>
            <el-button type="success" @click="handleEvolve" :loading="evolving" class="feature-btn">
              <el-icon><MagicStick /></el-icon> 开始进化
            </el-button>
          </el-card>
        </el-col>

        <!-- 学习记录 -->
        <el-col :span="8">
          <el-card shadow="hover" class="feature-card">
            <div class="feature-icon">
              <el-icon :size="40" color="#e6a23c"><DataAnalysis /></el-icon>
            </div>
            <h3>学习记录</h3>
            <p class="feature-desc">查看 Agent 的学习历史，了解每次进化学到了什么，知识库是如何变化的。</p>
            <div class="learning-stats">
              <div class="learning-stat">
                <span class="ls-value">{{ learningRecords.length }}</span>
                <span class="ls-label">总学习次数</span>
              </div>
              <div class="learning-stat">
                <span class="ls-value">{{ recentLearnings }}</span>
                <span class="ls-label">近7天学习</span>
              </div>
            </div>
            <el-button type="warning" @click="showLearningDialog = true" class="feature-btn">
              <el-icon><View /></el-icon> 查看记录
            </el-button>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <!-- 手动操作区 -->
    <el-card class="section-card">
      <template #header>
        <div class="section-header">
          <div>
            <el-icon :size="20" color="#E6A23C"><Edit /></el-icon>
            <span>手动学习</span>
          </div>
          <span class="section-desc">手动触发特定的学习任务</span>
        </div>
      </template>

      <el-row :gutter="16">
        <!-- 处理 Agent 文档 -->
        <el-col :span="12">
          <el-card shadow="never" class="manual-card">
            <template #header>
              <div class="manual-header">
                <el-icon color="#409EFF"><Document /></el-icon>
                <span>从文档学习</span>
              </div>
            </template>
            <p class="manual-desc">让 Agent 学习一篇文档。可以选择已有项目文档，或手动粘贴内容。</p>
            <el-form :model="docForm" label-width="80px">
              <el-form-item label="Agent">
                <el-select v-model="docForm.agentId" placeholder="选择 Agent" style="width: 100%">
                  <el-option v-for="a in agents" :key="a.id" :label="`${a.name} (${a.role})`" :value="a.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="文档来源">
                <el-button type="primary" text @click="handleOpenDocSelect">
                  <el-icon><FolderOpened /></el-icon> 从项目选择文档
                </el-button>
                <span v-if="docForm.documentPath" class="selected-file">已选: {{ docForm.documentPath }}</span>
              </el-form-item>
              <el-form-item label="文档路径" v-if="!docForm.documentPath">
                <el-input v-model="docForm.documentPath" placeholder="如：/docs/api-design.md" />
              </el-form-item>
              <el-form-item label="文档内容">
                <el-input v-model="docForm.documentContent" type="textarea" :rows="4" placeholder="粘贴文档内容，或从项目选择文档自动填充" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleProcessDoc" :loading="processingDoc">
                  <el-icon><Upload /></el-icon> 让 Agent 学习
                </el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </el-col>

        <!-- 从游戏生成中学习 -->
        <el-col :span="12">
          <el-card shadow="never" class="manual-card">
            <template #header>
              <div class="manual-header">
                <el-icon color="#67C23A"><Monitor /></el-icon>
                <span>从游戏项目学习</span>
              </div>
            </template>
            <p class="manual-desc">让 Agent 从一个游戏项目的开发过程中学习。可以选择已有项目或手动输入项目信息。</p>
            <el-form :model="gameForm" label-width="80px">
              <el-form-item label="学习方式">
                <el-radio-group v-model="gameForm.learnMode" @change="handleLearnModeChange">
                  <el-radio value="existing">已有项目</el-radio>
                  <el-radio value="manual">手动输入</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="选择项目" v-if="gameForm.learnMode === 'existing'">
                <el-select v-model="gameForm.projectId" placeholder="选择已有项目" style="width: 100%" @change="handleProjectSelect">
                  <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="游戏描述" v-if="gameForm.learnMode === 'manual'">
                <el-input v-model="gameForm.gameDescription" type="textarea" :rows="2" placeholder="如：一个植物大战僵尸类的塔防游戏" />
              </el-form-item>
              <el-form-item label="模板">
                <el-select v-model="gameForm.templateId" placeholder="选择使用的模板" style="width: 100%">
                  <el-option v-for="t in templates" :key="t.id" :label="t.name" :value="t.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="项目结果">
                <el-radio-group v-model="gameForm.success">
                  <el-radio :value="true">成功</el-radio>
                  <el-radio :value="false">失败</el-radio>
                </el-radio-group>
                <div class="form-tip">成功和失败的项目都有学习价值</div>
              </el-form-item>
              <el-form-item>
                <el-button type="success" @click="handleLearnGame" :loading="learningGame">
                  <el-icon><MagicStick /></el-icon> 从项目学习
                </el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <!-- 文档选择对话框 -->
    <el-dialog v-model="showDocSelectDialog" title="选择项目文档" width="600px">
      <div class="doc-select-tip">选择一个项目，然后选择项目中的文档进行学习</div>
      <el-form label-width="80px" class="mb-4">
        <el-form-item label="项目">
          <el-select v-model="docSelectProjectId" placeholder="选择项目" style="width: 100%" @change="loadProjectFiles">
            <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <el-table :data="projectFiles" v-loading="loadingFiles" stripe @row-click="handleFileSelect" highlight-current-row>
        <el-table-column prop="name" label="文件名" />
        <el-table-column prop="path" label="路径" show-overflow-tooltip />
        <el-table-column label="大小" width="100">
          <template #default="{ row }">{{ formatSize(row.size) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loadingFiles && projectFiles.length === 0" description="该项目暂无可学习的文档" />
    </el-dialog>

    <!-- 学习记录对话框 -->
    <el-dialog v-model="showLearningDialog" title="Agent 学习记录" width="700px">
      <el-timeline v-if="learningRecords.length > 0">
        <el-timeline-item
          v-for="record in learningRecords"
          :key="record.id"
          :timestamp="record.time"
          :type="record.type === 'success' ? 'success' : record.type === 'error' ? 'danger' : 'primary'"
        >
          <el-card shadow="never">
            <h4>{{ record.title }}</h4>
            <p>{{ record.description }}</p>
            <div class="record-meta">
              <el-tag size="small" :type="record.source === 'auto' ? 'info' : 'warning'">
                {{ record.source === 'auto' ? '自动' : '手动' }}
              </el-tag>
              <span class="record-agent">Agent: {{ record.agentId }}</span>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无学习记录" />
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 知识进化管理页面
 * 管理 Agent 的自学习和自进化功能
 *
 * 知识进化是 Agent 的核心能力：
 * 1. 自动学习：从文档、技能、游戏项目中提取知识
 * 2. 自我优化：分析历史任务，总结最佳实践
 * 3. 持续进化：定期自动优化知识库
 */
import { ref, computed, onMounted } from 'vue'
import { knowledgeEvolutionApi, knowledgeBaseApi, agentApi, gameTemplateApi, projectApi, codeBrowserApi } from '@/api'
import { ElMessage } from 'element-plus'
import {
  Document, Tools, TrendCharts, Timer, Refresh, MagicStick,
  DataAnalysis, View, Edit, Upload, Monitor, FolderOpened
} from '@element-plus/icons-vue'

const organizing = ref(false)
const evolving = ref(false)
const processingDoc = ref(false)
const learningGame = ref(false)
const showLearningDialog = ref(false)
const showDocSelectDialog = ref(false)
const loadingFiles = ref(false)

const stats = ref({
  totalDocuments: 0,
  totalSkills: 0,
  evolutionCount: 0,
  lastEvolution: '从未',
  autoEvolutionEnabled: true
})

const agents = ref([])
const templates = ref([])
const projects = ref([])
const learningRecords = ref([])
const projectFiles = ref([])
const docSelectProjectId = ref('')

const recentLearnings = computed(() => {
  const weekAgo = Date.now() - 7 * 24 * 60 * 60 * 1000
  return learningRecords.value.filter(r => new Date(r.time).getTime() > weekAgo).length
})

/** 文档处理表单 */
const docForm = ref({
  agentId: '',
  documentPath: '',
  documentContent: ''
})

/** 游戏学习表单 */
const gameForm = ref({
  learnMode: 'existing',
  projectId: '',
  gameDescription: '',
  templateId: '',
  success: true
})

/** 切换学习方式 */
const handleLearnModeChange = () => {
  gameForm.value.projectId = ''
  gameForm.value.gameDescription = ''
}

/** 选择已有项目 */
const handleProjectSelect = (projectId) => {
  const project = projects.value.find(p => p.id === projectId)
  if (project) {
    gameForm.value.gameDescription = project.description || project.name
  }
}

/** 打开文档选择对话框 */
const handleOpenDocSelect = () => {
  docSelectProjectId.value = ''
  projectFiles.value = []
  showDocSelectDialog.value = true
}

/** 加载项目文件列表 */
const loadProjectFiles = async (projectId) => {
  if (!projectId) return
  loadingFiles.value = true
  try {
    const files = await codeBrowserApi.getFileTree(projectId)
    // 过滤出文档类型的文件
    projectFiles.value = flattenFiles(files || []).filter(f =>
      !f.isDirectory && isDocument(f.name)
    )
  } catch (error) {
    console.error('加载文件列表失败:', error)
    projectFiles.value = []
  } finally {
    loadingFiles.value = false
  }
}

/** 递归展平文件树 */
const flattenFiles = (tree, prefix = '') => {
  const result = []
  for (const item of tree) {
    const path = prefix ? `${prefix}/${item.name}` : item.name
    result.push({ ...item, path })
    if (item.children) {
      result.push(...flattenFiles(item.children, path))
    }
  }
  return result
}

/** 判断是否是文档文件 */
const isDocument = (name) => {
  const docExts = ['.md', '.txt', '.doc', '.docx', '.pdf', '.json', '.yaml', '.yml', '.xml', '.html']
  const lower = name.toLowerCase()
  return docExts.some(ext => lower.endsWith(ext))
}

/** 选择文件 */
const handleFileSelect = async (file) => {
  try {
    const result = await codeBrowserApi.getFileContent(docSelectProjectId.value, file.path)
    if (result.success) {
      docForm.value.documentPath = file.path
      docForm.value.documentContent = result.content
      showDocSelectDialog.value = false
      ElMessage.success(`已加载文件: ${file.name}`)
    }
  } catch (error) {
    ElMessage.error('加载文件内容失败')
  }
}

/** 格式化文件大小 */
const formatSize = (bytes) => {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

/** 加载数据 */
const loadData = async () => {
  try {
    const kbStats = await knowledgeBaseApi.getStats()
    if (kbStats) {
      stats.value.totalDocuments = kbStats.totalDocuments || 0
      stats.value.totalSkills = kbStats.totalSkills || 0
    }
  } catch (error) {
    console.error('加载知识库统计失败:', error)
  }

  try {
    const agentData = await agentApi.getAll()
    agents.value = agentData || []
  } catch (error) {
    console.error('加载 Agent 列表失败:', error)
  }

  try {
    const templateData = await gameTemplateApi.getAll()
    templates.value = templateData || []
  } catch (error) {
    console.error('加载模板列表失败:', error)
  }

  try {
    const projectData = await projectApi.getAll()
    projects.value = projectData || []
  } catch (error) {
    console.error('加载项目列表失败:', error)
  }
}

/** 整理知识库 */
const handleOrganize = async () => {
  organizing.value = true
  try {
    const result = await knowledgeEvolutionApi.organize()
    ElMessage.success('知识库整理完成！已重新建立知识索引。')
    addLearningRecord('知识库整理', '完成了知识库的整理和索引重建', 'auto', 'system')
    loadData()
  } catch (error) {
    ElMessage.error('整理失败：' + (error.message || '未知错误'))
  } finally {
    organizing.value = false
  }
}

/** 触发自进化 */
const handleEvolve = async () => {
  evolving.value = true
  try {
    const result = await knowledgeEvolutionApi.evolve()
    ElMessage.success('自进化完成！Agent 已从历史经验中学习。')
    addLearningRecord('自进化', 'Agent 分析了历史任务并优化了知识库', 'auto', 'system')
    stats.value.evolutionCount = (stats.value.evolutionCount || 0) + 1
    stats.value.lastEvolution = '刚刚'
    loadData()
  } catch (error) {
    ElMessage.error('自进化失败：' + (error.message || '未知错误'))
  } finally {
    evolving.value = false
  }
}

/** 处理文档 */
const handleProcessDoc = async () => {
  if (!docForm.value.agentId || !docForm.value.documentContent) {
    ElMessage.warning('请选择 Agent 并填写文档内容')
    return
  }

  processingDoc.value = true
  try {
    await knowledgeEvolutionApi.processDocument(docForm.value)
    ElMessage.success('文档学习完成！知识已加入知识库。')
    addLearningRecord(
      '文档学习',
      `Agent 从文档 "${docForm.value.documentPath || '未命名'}" 中提取了知识`,
      'manual',
      docForm.value.agentId
    )
    docForm.value = { agentId: '', documentPath: '', documentContent: '' }
    loadData()
  } catch (error) {
    ElMessage.error('学习失败：' + (error.message || '未知错误'))
  } finally {
    processingDoc.value = false
  }
}

/** 从游戏生成中学习 */
const handleLearnGame = async () => {
  if (!gameForm.value.gameDescription || !gameForm.value.templateId) {
    ElMessage.warning('请填写游戏描述并选择模板')
    return
  }

  learningGame.value = true
  try {
    await knowledgeEvolutionApi.learnFromGame(gameForm.value)
    ElMessage.success('项目学习完成！已总结最佳实践。')
    addLearningRecord(
      '项目学习',
      `从${gameForm.value.success ? '成功' : '失败'}的项目中学习了经验`,
      'manual',
      'system'
    )
    gameForm.value = { gameDescription: '', templateId: '', success: true }
    loadData()
  } catch (error) {
    ElMessage.error('学习失败：' + (error.message || '未知错误'))
  } finally {
    learningGame.value = false
  }
}

/** 添加学习记录 */
const addLearningRecord = (title, description, source, agentId) => {
  learningRecords.value.unshift({
    id: Date.now(),
    title,
    description,
    source,
    agentId,
    time: new Date().toLocaleString('zh-CN'),
    type: 'success'
  })
  // 只保留最近50条
  if (learningRecords.value.length > 50) {
    learningRecords.value = learningRecords.value.slice(0, 50)
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.knowledge-evolution-page {
  padding: 20px;
}

.overview-cards {
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 0;
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  width: 100%;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: var(--el-text-color-primary);
}

.stat-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

.section-card {
  margin-bottom: 16px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-header > div {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 500;
}

.section-desc {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.feature-card {
  text-align: center;
  height: 100%;
}

.feature-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 20px;
}

.feature-icon {
  margin-bottom: 16px;
}

.feature-card h3 {
  margin: 0 0 12px;
  font-size: 18px;
}

.feature-desc {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.6;
  margin: 0 0 16px;
  text-align: left;
}

.feature-list {
  text-align: left;
  font-size: 13px;
  color: var(--el-text-color-regular);
  padding-left: 20px;
  margin: 0 0 16px;
  line-height: 1.8;
}

.feature-btn {
  margin-top: auto;
  width: 100%;
}

.manual-card {
  height: 100%;
}

.manual-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.manual-desc {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.6;
  margin: 0 0 16px;
}

.form-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

.learning-stats {
  display: flex;
  justify-content: center;
  gap: 32px;
  margin: 16px 0;
}

.learning-stat {
  text-align: center;
}

.ls-value {
  display: block;
  font-size: 24px;
  font-weight: bold;
  color: var(--el-color-warning);
}

.ls-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.record-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}

.record-agent {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.mb-4 {
  margin-bottom: 16px;
}

.selected-file {
  margin-left: 12px;
  color: var(--el-color-success);
  font-size: 13px;
}

.doc-select-tip {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin-bottom: 12px;
}

/* 手机端适配 */
@media (max-width: 767px) {
  .knowledge-evolution-page {
    padding: 12px;
  }

  .overview-cards .el-col {
    margin-bottom: 8px;
  }

  .feature-card {
    margin-bottom: 16px;
  }
}
</style>
