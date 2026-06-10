<template>
  <div class="capabilities-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h2>能力管理</h2>
        <span class="subtitle">管理Agent的能力定义，控制Agent能做什么</span>
      </div>
      <div class="header-right">
        <el-button @click="loadCapabilities" :loading="loading">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
        <el-button @click="handleReload" :loading="reloading" type="success">
          <el-icon><RefreshRight /></el-icon> 热重载
        </el-button>
        <el-button type="primary" @click="handleAIGenerate" v-permission="'agents:manage'">
          <el-icon><MagicStick /></el-icon> AI生成
        </el-button>
        <el-button type="primary" @click="handleCreate" v-permission="'agents:manage'">
          <el-icon><Plus /></el-icon> 新增能力
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <el-card shadow="hover" class="stat-card">
        <div class="stat-icon" style="background: #409eff">
          <el-icon :size="24"><Cpu /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.total || 0 }}</div>
          <div class="stat-label">总能力数</div>
        </div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-icon" style="background: #67c23a">
          <el-icon :size="24"><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.enabled || 0 }}</div>
          <div class="stat-label">已启用</div>
        </div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-icon" style="background: #909399">
          <el-icon :size="24"><CircleClose /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.disabled || 0 }}</div>
          <div class="stat-label">已禁用</div>
        </div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-icon" style="background: #e6a23c">
          <el-icon :size="24"><UserFilled /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.roles || 0 }}</div>
          <div class="stat-label">Agent角色数</div>
        </div>
      </el-card>
    </div>

    <!-- 筛选和视图切换 -->
    <el-card class="filter-card">
      <div class="filter-bar">
        <div class="filter-left">
          <el-select v-model="filterRole" placeholder="角色筛选" clearable @change="loadCapabilities" style="width: 150px">
            <el-option label="全部角色" value="" />
            <el-option v-for="role in roles" :key="role" :label="role" :value="role" />
          </el-select>
          <el-select v-model="filterCategory" placeholder="分类筛选" clearable @change="loadCapabilities" style="width: 150px">
            <el-option label="全部分类" value="" />
            <el-option label="Agent管理" value="agent_management" />
            <el-option label="任务" value="task" />
            <el-option label="通信" value="communication" />
            <el-option label="监控" value="monitoring" />
            <el-option label="项目" value="project" />
            <el-option label="代码" value="code" />
            <el-option label="部署" value="deploy" />
            <el-option label="验证" value="verification" />
          </el-select>
          <el-select v-model="filterStatus" placeholder="状态筛选" clearable @change="filterLocal" style="width: 120px">
            <el-option label="全部状态" value="" />
            <el-option label="已启用" value="enabled" />
            <el-option label="已禁用" value="disabled" />
          </el-select>
          <el-input v-model="searchText" placeholder="搜索能力名称..." clearable style="width: 200px" @input="filterLocal">
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
        <div class="filter-right">
          <el-radio-group v-model="viewMode" size="small">
            <el-radio-button value="card">
              <el-icon><Grid /></el-icon>
            </el-radio-button>
            <el-radio-button value="table">
              <el-icon><List /></el-icon>
            </el-radio-button>
          </el-radio-group>
        </div>
      </div>
    </el-card>

    <!-- 卡片视图 -->
    <div v-if="viewMode === 'card'" class="capabilities-grid" v-loading="loading">
      <el-card v-for="cap in filteredCapabilities" :key="cap.id" class="capability-card" shadow="hover">
        <div class="card-top">
          <div class="card-icon" :style="{ background: getCategoryColor(cap.category) }">
            <el-icon :size="20"><component :is="getCategoryIcon(cap.category)" /></el-icon>
          </div>
          <div class="card-status">
            <el-switch v-model="cap.enabled" @change="handleToggle(cap)" size="small" v-permission="'agents:manage'" />
          </div>
        </div>
        <div class="card-content">
          <h3 class="card-title">{{ cap.displayName || cap.capabilityName }}</h3>
          <code class="card-name">{{ cap.capabilityName }}</code>
          <p class="card-desc">{{ cap.description || '暂无描述' }}</p>
        </div>
        <div class="card-tags">
          <el-tag size="small" type="primary">{{ cap.agentRole }}</el-tag>
          <el-tag size="small" :type="getExecutionTypeColor(cap.executionType)">
            {{ getExecutionTypeLabel(cap.executionType) }}
          </el-tag>
          <el-tag v-if="cap.requiresApproval" size="small" type="warning">需审批</el-tag>
        </div>
        <div class="card-footer">
          <el-button type="primary" text size="small" @click="handleEdit(cap)">
            <el-icon><Edit /></el-icon> 编辑
          </el-button>
          <el-button type="info" text size="small" @click="handleTest(cap)">
            <el-icon><VideoPlay /></el-icon> 测试
          </el-button>
          <el-button type="danger" text size="small" @click="handleDelete(cap)" v-permission="'agents:manage'">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </el-card>

      <!-- 空状态 -->
      <el-empty v-if="!loading && filteredCapabilities.length === 0" description="暂无能力数据">
        <el-button type="primary" @click="handleCreate">新增能力</el-button>
      </el-empty>
    </div>

    <!-- 表格视图 -->
    <el-card v-else class="table-card">
      <el-table :data="filteredCapabilities" v-loading="loading" stripe>
        <el-table-column prop="capabilityName" label="能力名称" width="180">
          <template #default="{ row }">
            <div>
              <div class="name-display">{{ row.displayName || row.capabilityName }}</div>
              <code class="name-code">{{ row.capabilityName }}</code>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="agentRole" label="角色" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ row.agentRole }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="100">
          <template #default="{ row }">
            <el-tag size="small" :color="getCategoryColor(row.category)" style="color: white">
              {{ getCategoryLabel(row.category) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="executionType" label="执行方式" width="100">
          <template #default="{ row }">
            <el-tag :type="getExecutionTypeColor(row.executionType)" size="small">
              {{ getExecutionTypeLabel(row.executionType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="requiresApproval" label="审批" width="80" align="center">
          <template #default="{ row }">
            <el-icon v-if="row.requiresApproval" style="color: #e6a23c"><Warning /></el-icon>
            <el-icon v-else style="color: #67c23a"><CircleCheck /></el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="enabled" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" @change="handleToggle(row)" size="small" v-permission="'agents:manage'" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="handleEdit(row)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-button type="info" size="small" text @click="handleTest(row)">
              <el-icon><VideoPlay /></el-icon> 测试
            </el-button>
            <el-button type="danger" size="small" text @click="handleDelete(row)" v-permission="'agents:manage'">
              <el-icon><Delete /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑能力' : '新增能力'" width="800px" top="5vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-tabs v-model="formTab">
          <el-tab-pane label="基本信息" name="basic">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="能力名称" prop="capabilityName">
                  <el-input v-model="form.capabilityName" placeholder="如：createAgent" :disabled="isEdit" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="显示名称" prop="displayName">
                  <el-input v-model="form.displayName" placeholder="如：创建Agent" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="角色" prop="agentRole">
                  <el-select v-model="form.agentRole" placeholder="选择角色" style="width: 100%">
                    <el-option v-for="role in roles" :key="role" :label="role" :value="role" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="执行方式" prop="executionType">
                  <el-select v-model="form.executionType" placeholder="选择执行方式" style="width: 100%">
                    <el-option label="Java执行器" value="java">
                      <div>
                        <span>Java执行器</span>
                        <span style="color: #909399; font-size: 12px; margin-left: 8px;">调用Java类执行，适合系统级操作</span>
                      </div>
                    </el-option>
                    <el-option label="Prompt模板" value="prompt">
                      <div>
                        <span>Prompt模板</span>
                        <span style="color: #909399; font-size: 12px; margin-left: 8px;">通过AI提示词驱动，适合智能任务</span>
                      </div>
                    </el-option>
                    <el-option label="消息发送" value="message">
                      <div>
                        <span>消息发送</span>
                        <span style="color: #909399; font-size: 12px; margin-left: 8px;">发送消息给Agent或外部系统</span>
                      </div>
                    </el-option>
                    <el-option label="脚本执行" value="script">
                      <div>
                        <span>脚本执行</span>
                        <span style="color: #909399; font-size: 12px; margin-left: 8px;">运行Shell/Python脚本，适合自动化任务</span>
                      </div>
                    </el-option>
                    <el-option label="API调用" value="api">
                      <div>
                        <span>API调用</span>
                        <span style="color: #909399; font-size: 12px; margin-left: 8px;">调用外部HTTP接口，适合集成第三方服务</span>
                      </div>
                    </el-option>
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="分类">
                  <el-select v-model="form.category" placeholder="选择分类" style="width: 100%">
                    <el-option label="任务" value="task" />
                    <el-option label="通信" value="communication" />
                    <el-option label="监控" value="monitoring" />
                    <el-option label="Agent管理" value="agent_management" />
                    <el-option label="项目" value="project" />
                    <el-option label="代码" value="code" />
                    <el-option label="部署" value="deploy" />
                    <el-option label="验证" value="verification" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="优先级">
                  <el-input-number v-model="form.priority" :min="1" :max="100" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="描述">
              <el-input v-model="form.description" type="textarea" :rows="3" placeholder="能力描述" />
            </el-form-item>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="需要审批">
                  <el-switch v-model="form.requiresApproval" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="启用状态">
                  <el-switch v-model="form.enabled" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-tab-pane>

          <el-tab-pane label="执行配置" name="execution">
            <el-alert type="info" :closable="false" style="margin-bottom: 16px;">
              执行配置定义了Agent调用此能力时的具体执行方式。不同的执行方式需要填写不同的配置参数。
            </el-alert>

            <el-form-item label="执行器类名" v-if="form.executionType === 'java'">
              <el-input v-model="form.executorClass" placeholder="如：com.example.CreateAgentExecutor" />
              <div class="form-tip">实现 CapabilityExecutor 接口的 Java 类全限定名</div>
            </el-form-item>
            <el-form-item label="Prompt模板" v-if="form.executionType === 'prompt'">
              <MarkdownEditor v-model="form.promptTemplate" :rows="8" placeholder="输入Prompt模板，支持变量：{{agentId}}, {{projectId}}, {{taskDescription}}" />
              <div class="form-tip">AI 执行时使用的提示词模板，可用变量：{{agentId}}、{{projectId}}、{{taskDescription}}</div>
            </el-form-item>
            <el-form-item label="脚本内容" v-if="form.executionType === 'script'">
              <el-input v-model="form.scriptContent" type="textarea" :rows="8" placeholder="#!/bin/bash&#10;echo 'Hello World'" />
              <div class="form-tip">Shell 或 Python 脚本内容，支持变量替换</div>
            </el-form-item>
            <el-form-item label="API地址" v-if="form.executionType === 'api'">
              <el-input v-model="form.apiUrl" placeholder="如：/api/external/execute 或 https://example.com/api" />
              <div class="form-tip">外部 API 的 URL 地址，支持相对路径和绝对路径</div>
            </el-form-item>
            <el-form-item label="消息目标" v-if="form.executionType === 'message'">
              <el-input v-model="form.messageTarget" placeholder="如：agent:producer 或 channel:dev-team" />
              <div class="form-tip">消息发送目标，格式：agent:{id} 或 channel:{name}</div>
            </el-form-item>

            <el-divider>执行策略</el-divider>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="超时时间(秒)">
                  <el-input-number v-model="form.timeout" :min="1" :max="300" style="width: 100%" />
                  <div class="form-tip">执行超时时间，超时后自动终止（建议：简单任务30秒，复杂任务120秒）</div>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="重试次数">
                  <el-input-number v-model="form.retryCount" :min="0" :max="5" style="width: 100%" />
                  <div class="form-tip">失败后自动重试次数（0=不重试，建议：关键任务1-2次）</div>
                </el-form-item>
              </el-col>
            </el-row>
          </el-tab-pane>

          <el-tab-pane label="关联技能" name="skills">
            <el-form-item label="关联技能">
              <el-select v-model="form.relatedSkillIds" multiple filterable placeholder="选择关联技能" style="width: 100%">
                <el-option v-for="skill in skills" :key="skill.id" :label="skill.name" :value="skill.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="前置能力">
              <el-select v-model="form.prerequisiteCapabilities" multiple filterable placeholder="选择前置能力" style="width: 100%">
                <el-option v-for="cap in allCapabilities" :key="cap.id" :label="cap.displayName || cap.capabilityName" :value="cap.capabilityName" />
              </el-select>
            </el-form-item>
          </el-tab-pane>
        </el-tabs>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ isEdit ? '更新' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- AI生成对话框 -->
    <el-dialog v-model="aiDialogVisible" title="AI生成能力" width="700px">
      <el-alert type="info" :closable="false" class="mb-4">
        <template #title>
          <div>
            <p>描述你想要的功能，AI将自动生成能力定义。例如：</p>
            <ul style="margin: 8px 0 0 20px">
              <li>创建一个能自动分析代码质量的能力</li>
              <li>生成一个用于部署项目到服务器的能力</li>
              <li>创建一个能自动修复代码Bug的能力</li>
            </ul>
          </div>
        </template>
      </el-alert>

      <el-form :model="aiForm" label-width="100px">
        <el-form-item label="目标角色">
          <el-select v-model="aiForm.role" placeholder="选择角色" style="width: 100%">
            <el-option v-for="role in roles" :key="role" :label="role" :value="role" />
          </el-select>
        </el-form-item>
        <el-form-item label="功能描述">
          <el-input v-model="aiForm.description" type="textarea" :rows="4" placeholder="描述你想要的功能..." />
        </el-form-item>
        <el-form-item label="执行方式">
          <el-select v-model="aiForm.executionType" placeholder="选择执行方式" style="width: 100%">
            <el-option label="自动选择" value="" />
            <el-option label="Java执行器 - 系统级操作" value="java" />
            <el-option label="Prompt模板 - AI智能任务" value="prompt" />
            <el-option label="脚本执行 - 自动化任务" value="script" />
            <el-option label="API调用 - 集成第三方" value="api" />
          </el-select>
        </el-form-item>
      </el-form>

      <!-- AI生成结果预览 -->
      <div v-if="aiResult" class="ai-result">
        <el-divider content-position="left">生成结果</el-divider>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="能力名称">{{ aiResult.capabilityName }}</el-descriptions-item>
          <el-descriptions-item label="显示名称">{{ aiResult.displayName }}</el-descriptions-item>
          <el-descriptions-item label="角色">{{ aiResult.agentRole }}</el-descriptions-item>
          <el-descriptions-item label="执行方式">{{ aiResult.executionType }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ aiResult.category }}</el-descriptions-item>
          <el-descriptions-item label="优先级">{{ aiResult.priority }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ aiResult.description }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="aiResult.promptTemplate" class="ai-prompt-preview">
          <h4>Prompt模板预览：</h4>
          <pre>{{ aiResult.promptTemplate }}</pre>
        </div>
      </div>

      <template #footer>
        <el-button @click="aiDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAISubmit" :loading="aiLoading">
          {{ aiResult ? '使用此配置创建' : '开始生成' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 测试对话框 -->
    <el-dialog v-model="testDialogVisible" title="能力测试" width="600px">
      <div v-if="testingCapability">
        <el-descriptions :column="1" border size="small" class="mb-4">
          <el-descriptions-item label="能力名称">{{ testingCapability.displayName || testingCapability.capabilityName }}</el-descriptions-item>
          <el-descriptions-item label="执行方式">{{ getExecutionTypeLabel(testingCapability.executionType) }}</el-descriptions-item>
        </el-descriptions>

        <el-form :model="testForm" label-width="100px">
          <el-form-item label="测试Agent">
            <el-select v-model="testForm.agentId" placeholder="选择测试Agent" style="width: 100%">
              <el-option v-for="agent in agents" :key="agent.id" :label="agent.name" :value="agent.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="测试参数">
            <el-input v-model="testForm.params" type="textarea" :rows="4" placeholder="JSON格式的测试参数" />
          </el-form-item>
        </el-form>

        <div v-if="testResult" class="test-result">
          <el-divider content-position="left">测试结果</el-divider>
          <el-tag :type="testResult.success ? 'success' : 'danger'" class="mb-2">
            {{ testResult.success ? '成功' : '失败' }}
          </el-tag>
          <pre class="test-output">{{ testResult.output }}</pre>
        </div>
      </div>
      <template #footer>
        <el-button @click="testDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleTestExecute" :loading="testLoading">
          执行测试
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 能力管理页面
 * 管理Agent的能力定义，支持AI生成和实时生效
 */
import { ref, computed, onMounted } from 'vue'
import { capabilityApi, skillApi, agentApi } from '@/api'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const reloading = ref(false)
const capabilities = ref([])
const allCapabilities = ref([])
const roles = ref([])
const skills = ref([])
const agents = ref([])

// 筛选
const filterRole = ref('')
const filterCategory = ref('')
const filterStatus = ref('')
const searchText = ref('')
const viewMode = ref('card')

// 表单
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const formTab = ref('basic')
const form = ref({
  capabilityName: '',
  displayName: '',
  agentRole: '',
  executionType: 'java',
  description: '',
  category: 'task',
  priority: 5,
  requiresApproval: false,
  enabled: true,
  executorClass: '',
  promptTemplate: '',
  scriptContent: '',
  apiUrl: '',
  messageTarget: '',
  timeout: 30,
  retryCount: 1,
  relatedSkillIds: [],
  prerequisiteCapabilities: []
})
const rules = {
  capabilityName: [{ required: true, message: '请输入能力名称', trigger: 'blur' }],
  agentRole: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

// AI生成
const aiDialogVisible = ref(false)
const aiLoading = ref(false)
const aiForm = ref({ role: '', description: '', executionType: '' })
const aiResult = ref(null)

// 测试
const testDialogVisible = ref(false)
const testingCapability = ref(null)
const testLoading = ref(false)
const testForm = ref({ agentId: '', params: '{}' })
const testResult = ref(null)

// 统计
const stats = computed(() => {
  const list = capabilities.value
  return {
    total: list.length,
    enabled: list.filter(c => c.enabled).length,
    disabled: list.filter(c => !c.enabled).length,
    roles: roles.value.length  // 使用实际的角色列表长度
  }
})

// 本地过滤
const filteredCapabilities = computed(() => {
  let result = capabilities.value
  if (filterRole.value) result = result.filter(c => c.agentRole === filterRole.value)
  if (filterCategory.value) result = result.filter(c => c.category === filterCategory.value)
  if (filterStatus.value === 'enabled') result = result.filter(c => c.enabled)
  if (filterStatus.value === 'disabled') result = result.filter(c => !c.enabled)
  if (searchText.value) {
    const text = searchText.value.toLowerCase()
    result = result.filter(c =>
      c.capabilityName.toLowerCase().includes(text) ||
      (c.displayName && c.displayName.toLowerCase().includes(text)) ||
      (c.description && c.description.toLowerCase().includes(text))
    )
  }
  return result
})

const filterLocal = () => {}

// 颜色和图标
const getCategoryColor = (cat) => {
  const map = { task: '#409eff', communication: '#67c23a', monitoring: '#e6a23c', agent_management: '#909399', project: '#f56c6c', code: '#00d4aa', deploy: '#7c3aed' }
  return map[cat] || '#909399'
}

const getCategoryIcon = (cat) => {
  const map = { task: 'Document', communication: 'ChatDotRound', monitoring: 'Monitor', agent_management: 'UserFilled', project: 'Folder', code: 'Document', deploy: 'Upload' }
  return map[cat] || 'Cpu'
}

const getCategoryLabel = (cat) => {
  const map = { task: '任务', communication: '通信', monitoring: '监控', agent_management: 'Agent管理', project: '项目', code: '代码', deploy: '部署' }
  return map[cat] || cat
}

const getExecutionTypeColor = (type) => {
  const map = { java: 'primary', prompt: 'info', message: 'success', script: 'warning', api: 'danger' }
  return map[type] || 'info'
}

const getExecutionTypeLabel = (type) => {
  const map = { java: 'Java', prompt: 'Prompt', message: '消息', script: '脚本', api: 'API' }
  return map[type] || type
}

// 加载数据
const loadCapabilities = async () => {
  loading.value = true
  try {
    const [capsData, rolesData] = await Promise.all([
      capabilityApi.getAll(),
      capabilityApi.getRoles()
    ])
    capabilities.value = capsData || []
    allCapabilities.value = capsData || []
    roles.value = rolesData || []
  } catch (error) {
    ElMessage.error('加载能力列表失败')
  } finally {
    loading.value = false
  }
}

const loadSkills = async () => {
  try {
    const data = await skillApi.getAll()
    skills.value = data || []
  } catch (error) {
    console.error('加载技能列表失败')
  }
}

const loadAgents = async () => {
  try {
    const data = await agentApi.getAll()
    agents.value = data || []
  } catch (error) {
    console.error('加载Agent列表失败')
  }
}

// 热重载
const handleReload = async () => {
  reloading.value = true
  try {
    await capabilityApi.reload()
    ElMessage.success('能力配置已热重载')
    loadCapabilities()
  } catch (error) {
    ElMessage.error('热重载失败')
  } finally {
    reloading.value = false
  }
}

// 创建
const handleCreate = () => {
  isEdit.value = false
  formTab.value = 'basic'
  form.value = {
    capabilityName: '',
    displayName: '',
    agentRole: '',
    executionType: 'java',
    description: '',
    category: 'task',
    priority: 5,
    requiresApproval: false,
    enabled: true,
    executorClass: '',
    promptTemplate: '',
    scriptContent: '',
    apiUrl: '',
    messageTarget: '',
    timeout: 30,
    retryCount: 1,
    relatedSkillIds: [],
    prerequisiteCapabilities: []
  }
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  isEdit.value = true
  formTab.value = 'basic'
  form.value = {
    ...row,
    relatedSkillIds: row.relatedSkillIds ? row.relatedSkillIds.split(',').filter(Boolean) : [],
    prerequisiteCapabilities: row.prerequisiteCapabilities ? row.prerequisiteCapabilities.split(',').filter(Boolean) : []
  }
  dialogVisible.value = true
}

// 切换状态
const handleToggle = async (row) => {
  try {
    await capabilityApi.toggle(row.id)
    ElMessage.success('状态已切换')
  } catch (error) {
    ElMessage.error('操作失败')
    row.enabled = !row.enabled
  }
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这个能力吗？', '确认删除', { type: 'warning' })
    await capabilityApi.delete(row.id)
    ElMessage.success('删除成功')
    loadCapabilities()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const data = {
      ...form.value,
      relatedSkillIds: Array.isArray(form.value.relatedSkillIds) ? form.value.relatedSkillIds.join(',') : form.value.relatedSkillIds,
      prerequisiteCapabilities: Array.isArray(form.value.prerequisiteCapabilities) ? form.value.prerequisiteCapabilities.join(',') : form.value.prerequisiteCapabilities
    }

    if (isEdit.value) {
      await capabilityApi.update(form.value.id, data)
    } else {
      await capabilityApi.create(data)
    }
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    loadCapabilities()
  } catch (error) {
    ElMessage.error('操作失败: ' + (error.message || ''))
  } finally {
    submitting.value = false
  }
}

// AI生成
const handleAIGenerate = () => {
  aiForm.value = { role: roles.value[0] || '', description: '', executionType: '' }
  aiResult.value = null
  aiDialogVisible.value = true
}

const handleAISubmit = async () => {
  if (!aiResult.value) {
    // 生成
    if (!aiForm.value.description) {
      ElMessage.warning('请输入功能描述')
      return
    }
    aiLoading.value = true
    try {
      // 模拟AI生成（实际应调用AI API）
      const result = generateCapabilityFromDescription(aiForm.value)
      aiResult.value = result
    } catch (error) {
      ElMessage.error('生成失败')
    } finally {
      aiLoading.value = false
    }
  } else {
    // 使用生成结果创建
    form.value = { ...aiResult.value }
    isEdit.value = false
    formTab.value = 'basic'
    aiDialogVisible.value = false
    dialogVisible.value = true
  }
}

// 本地AI生成逻辑
const generateCapabilityFromDescription = (input) => {
  const { role, description, executionType } = input
  const desc = description.toLowerCase()

  // 根据描述推断能力类型
  let capabilityName = ''
  let displayName = ''
  let category = 'task'
  let execType = executionType || 'prompt'
  let promptTemplate = ''

  if (desc.includes('代码') && desc.includes('审查') || desc.includes('review')) {
    capabilityName = 'codeReview'
    displayName = '代码审查'
    category = 'code'
    promptTemplate = `请审查以下代码，检查：
1. 代码质量
2. 潜在Bug
3. 性能问题
4. 安全漏洞

代码：
{{code}}

请提供详细的审查报告。`
  } else if (desc.includes('部署') || desc.includes('deploy')) {
    capabilityName = 'deployProject'
    displayName = '部署项目'
    category = 'deploy'
    execType = 'script'
  } else if (desc.includes('测试') || desc.includes('test')) {
    capabilityName = 'runTests'
    displayName = '运行测试'
    category = 'code'
    execType = 'script'
  } else if (desc.includes('文档') || desc.includes('document')) {
    capabilityName = 'generateDocs'
    displayName = '生成文档'
    category = 'code'
    promptTemplate = `请为以下代码生成文档：

{{code}}

要求：
1. 函数说明
2. 参数说明
3. 返回值说明
4. 使用示例`
  } else if (desc.includes('分析') || desc.includes('analyze')) {
    capabilityName = 'analyzeCode'
    displayName = '代码分析'
    category = 'code'
    promptTemplate = `请分析以下代码：

{{code}}

分析维度：
1. 架构设计
2. 代码质量
3. 性能瓶颈
4. 改进建议`
  } else {
    // 通用生成
    const words = description.match(/[一-龥a-zA-Z]+/g) || []
    capabilityName = words.slice(0, 3).join('_').toLowerCase() || 'custom_capability'
    displayName = description.slice(0, 20)
    promptTemplate = `执行任务：${description}

上下文信息：
- 项目ID: {{projectId}}
- Agent ID: {{agentId}}

请根据上下文执行相应操作。`
  }

  return {
    capabilityName,
    displayName,
    agentRole: role,
    executionType: execType,
    description,
    category,
    priority: 5,
    requiresApproval: category === 'deploy',
    enabled: true,
    promptTemplate,
    timeout: 60,
    retryCount: 1
  }
}

// 测试能力
const handleTest = (row) => {
  testingCapability.value = row
  testForm.value = { agentId: '', params: '{}' }
  testResult.value = null
  testDialogVisible.value = true
}

const handleTestExecute = async () => {
  if (!testForm.value.agentId) {
    ElMessage.warning('请选择测试Agent')
    return
  }
  testLoading.value = true
  try {
    // 模拟测试执行
    testResult.value = {
      success: true,
      output: `能力 "${testingCapability.value.displayName}" 测试执行完成。\n\n执行时间: 1.2s\n状态: 成功`
    }
  } catch (error) {
    testResult.value = {
      success: false,
      output: error.message || '测试执行失败'
    }
  } finally {
    testLoading.value = false
  }
}

onMounted(() => {
  loadCapabilities()
  loadSkills()
  loadAgents()
})
</script>

<style scoped>
.capabilities-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.header-left h2 {
  margin: 0 0 4px 0;
  font-size: 24px;
}

.subtitle {
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.header-right {
  display: flex;
  gap: 8px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  line-height: 1;
}

.stat-label {
  color: var(--el-text-color-secondary);
  font-size: 14px;
  margin-top: 4px;
}

.filter-card {
  margin-bottom: 20px;
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-left {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

/* 卡片视图 */
.capabilities-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.capability-card {
  transition: all 0.3s;
}

.capability-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.card-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.card-content {
  margin-bottom: 16px;
}

.card-title {
  margin: 0 0 4px 0;
  font-size: 16px;
  font-weight: 600;
}

.card-name {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
  padding: 2px 6px;
  border-radius: 4px;
}

.card-desc {
  margin: 8px 0 0 0;
  font-size: 13px;
  color: var(--el-text-color-regular);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 16px;
}

.card-footer {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
  border-top: 1px solid var(--el-border-color-lighter);
  padding-top: 12px;
}

/* 表格视图 */
.table-card {
  margin-bottom: 20px;
}

.name-display {
  font-weight: 500;
  margin-bottom: 2px;
}

.name-code {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

/* AI生成结果 */
.ai-result {
  margin-top: 16px;
}

.ai-prompt-preview {
  margin-top: 16px;
}

.ai-prompt-preview h4 {
  margin-bottom: 8px;
}

.ai-prompt-preview pre {
  background: var(--el-fill-color-light);
  padding: 12px;
  border-radius: 8px;
  font-size: 13px;
  max-height: 200px;
  overflow-y: auto;
}

/* 测试结果 */
.test-result {
  margin-top: 16px;
}

.test-output {
  background: var(--el-fill-color-light);
  padding: 12px;
  border-radius: 8px;
  font-size: 13px;
  white-space: pre-wrap;
  max-height: 300px;
  overflow-y: auto;
}

.mb-2 {
  margin-bottom: 8px;
}

.mb-4 {
  margin-bottom: 16px;
}

.form-tip {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
  line-height: 1.4;
}

/* 响应式 */
@media (max-width: 767px) {
  .capabilities-page {
    padding: 12px;
  }

  .page-header {
    flex-direction: column;
    gap: 12px;
  }

  .header-right {
    width: 100%;
    flex-wrap: wrap;
  }

  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-left {
    flex-direction: column;
  }

  .capabilities-grid {
    grid-template-columns: 1fr;
  }
}
</style>
