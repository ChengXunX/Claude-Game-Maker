<template>
  <div class="projects-page">
    <!-- 统计概览 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-primary-light-9)">
            <el-icon :size="24" color="var(--el-color-primary)"><Folder /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ projects.length }}</div>
            <div class="stat-label">项目总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-success-light-9)">
            <el-icon :size="24" color="var(--el-color-success)"><CircleCheck /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ activeProjectsCount }}</div>
            <div class="stat-label">活跃项目</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-warning-light-9)">
            <el-icon :size="24" color="var(--el-color-warning)"><User /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ totalAgentsCount }}</div>
            <div class="stat-label">Agent 总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-danger-light-9)">
            <el-icon :size="24" color="var(--el-color-danger)"><Aim /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ projectsWithGoalCount }}</div>
            <div class="stat-label">有目标的项目</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>项目管理</span>
          <div class="header-actions">
            <el-button @click="handleImport" v-permission="'projects:manage'">
              <el-icon><Upload /></el-icon> 导入项目
            </el-button>
            <el-button type="primary" @click="handleCreate" v-permission="'projects:manage'">
              <el-icon><Plus /></el-icon> 创建项目
            </el-button>
          </div>
        </div>
      </template>

      <!-- 筛选区 -->
      <div class="filter-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索项目名称、描述..."
          clearable
          class="filter-item"
          :prefix-icon="Search"
        />
        <el-select v-model="filterStatus" placeholder="项目状态" clearable style="width: 120px">
          <el-option label="全部" value="" />
          <el-option label="活跃" value="ACTIVE" />
          <el-option label="归档" value="ARCHIVED" />
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
      <div v-if="viewMode === 'card'" class="projects-grid" v-loading="loading">
        <el-card
          v-for="project in filteredProjects"
          :key="project.id"
          class="project-card"
          shadow="hover"
          @click="handleViewDetail(project)"
        >
          <div class="project-card-header">
            <el-tag :type="project.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ project.status === 'ACTIVE' ? '活跃' : project.status }}
            </el-tag>
            <el-dropdown @command="(cmd) => handleCommand(cmd, project)" @click.stop>
              <el-icon class="more-btn"><MoreFilled /></el-icon>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="code">
                    <el-icon><Document /></el-icon> 查看代码
                  </el-dropdown-item>
                  <el-dropdown-item command="agents">
                    <el-icon><User /></el-icon> 管理 Agent
                  </el-dropdown-item>
                  <el-dropdown-item command="goal" divided>
                    <el-icon><Aim /></el-icon> 设置目标
                  </el-dropdown-item>
                  <el-dropdown-item command="edit" v-permission="'projects:manage'">
                    <el-icon><Edit /></el-icon> 编辑
                  </el-dropdown-item>
                  <el-dropdown-item command="archive" v-permission="'projects:manage'" divided>
                    <el-icon><Box /></el-icon> 归档
                  </el-dropdown-item>
                  <el-dropdown-item command="delete" v-permission="'projects:manage'" divided>
                    <el-icon><Delete /></el-icon> 删除
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>

          <h3 class="project-name">{{ project.name }}</h3>
          <p class="project-desc">{{ project.description || '暂无描述' }}</p>

          <div class="project-meta">
            <div class="meta-item">
              <el-icon><FolderOpened /></el-icon>
              <span class="meta-text" :title="project.workDir">{{ truncatePath(project.workDir) }}</span>
            </div>
            <div class="meta-item">
              <el-icon><User /></el-icon>
              <span>{{ project.agentIds?.length || 0 }} 个 Agent</span>
            </div>
          </div>

          <!-- 目标进度 -->
          <div v-if="project.goal" class="project-goal">
            <div class="goal-header">
              <el-icon><Aim /></el-icon>
              <span class="goal-text">{{ truncateText(project.goal, 30) }}</span>
            </div>
            <el-progress
              :percentage="project.goalProgress || 0"
              :status="getGoalStatus(project.goalStatus)"
              :stroke-width="6"
            />
          </div>

          <div class="project-card-footer">
            <el-button type="primary" size="small" text @click.stop="handleViewCode(project)">
              代码
            </el-button>
            <el-button type="success" size="small" text @click.stop="handleViewAgents(project)">
              Agent
            </el-button>
            <el-button type="warning" size="small" text @click.stop="handleViewWorkflow(project)">
              工作流
            </el-button>
          </div>
        </el-card>

        <el-empty v-if="!loading && filteredProjects.length === 0" description="暂无项目">
          <el-button type="primary" @click="handleCreate">创建项目</el-button>
        </el-empty>
      </div>

      <!-- 列表视图 -->
      <div v-else>
        <el-table :data="filteredProjects" v-loading="loading" stripe>
          <el-table-column prop="name" label="项目名称" width="150">
            <template #default="{ row }">
              <el-link type="primary" @click="handleViewDetail(row)">{{ row.name }}</el-link>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
          <el-table-column label="工作目录" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="path-text">{{ truncatePath(row.workDir) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
                {{ row.status === 'ACTIVE' ? '活跃' : row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Agent" width="80" align="center">
            <template #default="{ row }">
              <el-badge :value="row.agentIds?.length || 0" :max="99" type="primary">
                <el-icon><User /></el-icon>
              </el-badge>
            </template>
          </el-table-column>
          <el-table-column label="目标进度" width="150">
            <template #default="{ row }">
              <div v-if="row.goal">
                <el-progress
                  :percentage="row.goalProgress || 0"
                  :status="getGoalStatus(row.goalStatus)"
                  :stroke-width="6"
                />
              </div>
              <span v-else class="no-goal">未设置</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="250" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" size="small" text @click="handleViewCode(row)">代码</el-button>
              <el-button type="success" size="small" text @click="handleViewAgents(row)">Agent</el-button>
              <el-button type="warning" size="small" text @click="handleEdit(row)" v-permission="'projects:manage'">编辑</el-button>
              <el-button type="danger" size="small" text @click="handleDelete(row)" v-permission="'projects:manage'">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- 项目详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :title="currentProject?.name || '项目详情'"
      size="500px"
      direction="rtl"
    >
      <template v-if="currentProject">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="项目ID">{{ currentProject.id }}</el-descriptions-item>
          <el-descriptions-item label="名称">{{ currentProject.name }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="currentProject.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ currentProject.status === 'ACTIVE' ? '活跃' : currentProject.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="描述">{{ currentProject.description || '暂无描述' }}</el-descriptions-item>
          <el-descriptions-item label="工作目录">
            <el-text class="path-text" truncated>{{ currentProject.workDir }}</el-text>
          </el-descriptions-item>
          <el-descriptions-item label="Agent 数量">
            {{ currentProject.agentIds?.length || 0 }} 个
          </el-descriptions-item>
        </el-descriptions>

        <!-- 目标信息 -->
        <el-divider>项目目标</el-divider>
        <template v-if="currentProject.goal">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="目标">{{ currentProject.goal }}</el-descriptions-item>
            <el-descriptions-item label="类型">{{ currentProject.goalType || '未设置' }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="getGoalTagType(currentProject.goalStatus)" size="small">
                {{ getGoalStatusText(currentProject.goalStatus) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="进度">
              <el-progress :percentage="currentProject.goalProgress || 0" />
            </el-descriptions-item>
            <el-descriptions-item v-if="currentProject.goalDeadline" label="截止时间">
              {{ currentProject.goalDeadline }}
            </el-descriptions-item>
          </el-descriptions>
        </template>
        <el-empty v-else description="未设置目标" :image-size="60">
          <el-button type="primary" size="small" @click="handleSetGoal(currentProject)">设置目标</el-button>
        </el-empty>

        <div class="drawer-actions">
          <el-button type="primary" @click="handleViewCode(currentProject)">
            <el-icon><Document /></el-icon> 查看代码
          </el-button>
          <el-button type="success" @click="handleViewAgents(currentProject)">
            <el-icon><User /></el-icon> 管理 Agent
          </el-button>
          <el-button @click="handleViewWorkflow(currentProject)">
            <el-icon><Connection /></el-icon> 工作流
          </el-button>
        </div>

        <!-- 里程碑列表 -->
        <el-divider>项目里程碑</el-divider>
        <div v-if="milestones.length > 0" class="milestones-list">
          <div v-for="(milestone, index) in milestones" :key="milestone.id" class="milestone-item">
            <div class="milestone-header">
              <div class="milestone-order">{{ index + 1 }}</div>
              <div class="milestone-info">
                <div class="milestone-title">{{ milestone.title }}</div>
                <div class="milestone-role">
                  <el-tag size="small" type="info">{{ milestone.assignedAgentRole }}</el-tag>
                </div>
              </div>
              <el-tag :type="getMilestoneTagType(milestone.status)" size="small">
                {{ getMilestoneStatusText(milestone.status) }}
              </el-tag>
            </div>
            <div v-if="milestone.description" class="milestone-desc">{{ milestone.description }}</div>
            <el-progress
              :percentage="milestone.progress || 0"
              :status="milestone.status === 'COMPLETED' ? 'success' : ''"
              :stroke-width="4"
              class="milestone-progress"
            />
            <!-- 任务列表 -->
            <div v-if="milestone.tasks && milestone.tasks.length > 0" class="milestone-tasks">
              <div v-for="task in milestone.tasks" :key="task.id" class="task-item">
                <el-icon :class="task.status === 'COMPLETED' ? 'task-done' : 'task-pending'">
                  <CircleCheck v-if="task.status === 'COMPLETED'" />
                  <Clock v-else />
                </el-icon>
                <span :class="{ 'task-done-text': task.status === 'COMPLETED' }">{{ task.description }}</span>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无里程碑" :image-size="60" />

        <!-- 目录配置 -->
        <el-divider>
          <div class="divider-title">
            <span>目录配置</span>
            <el-button size="small" text @click="showDirectoryDialog">
              <el-icon><Edit /></el-icon> 管理
            </el-button>
          </div>
        </el-divider>
        <div v-if="directoryConfigs.length > 0" class="directory-list">
          <div v-for="dir in directoryConfigs" :key="dir.path" class="directory-item">
            <div class="dir-path">
              <el-icon><FolderOpened /></el-icon>
              <span>{{ dir.path }}</span>
            </div>
            <div class="dir-desc">{{ dir.description }}</div>
            <div v-if="dir.notes" class="dir-notes">{{ dir.notes }}</div>
          </div>
        </div>
        <el-empty v-else description="暂未配置目录结构" :image-size="60">
          <el-button size="small" @click="showDirectoryDialog">配置目录</el-button>
        </el-empty>
      </template>
    </el-drawer>

    <!-- 编辑/创建项目对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑项目' : '创建项目'"
      width="600px"
    >
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="项目名称" required>
          <el-input v-model="editForm.name" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" placeholder="请输入项目描述" />
        </el-form-item>
        <el-form-item label="工作目录" v-if="!isEdit" required>
          <el-input v-model="editForm.workDir" placeholder="请输入工作目录路径" />
        </el-form-item>
        <el-form-item label="模板" v-if="!isEdit">
          <el-select v-model="editForm.templateId" placeholder="选择游戏模板（可选）" clearable style="width: 100%">
            <el-option v-for="t in templates" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>

        <el-divider v-if="!isEdit" content-position="left">项目目标（可选）</el-divider>

        <el-form-item label="项目目标" v-if="!isEdit">
          <el-input v-model="editForm.goal" type="textarea" :rows="2" placeholder="描述项目目标，如：开发一个休闲益智类H5小游戏" />
        </el-form-item>
        <el-form-item label="目标类型" v-if="!isEdit && editForm.goal">
          <el-select v-model="editForm.goalType" placeholder="选择目标类型" style="width: 100%">
            <el-option label="游戏开发" value="GAME_DEVELOPMENT" />
            <el-option label="功能开发" value="FEATURE" />
            <el-option label="Bug修复" value="BUG_FIX" />
            <el-option label="重构优化" value="REFACTOR" />
            <el-option label="自定义" value="CUSTOM" />
          </el-select>
        </el-form-item>

        <el-divider v-if="!isEdit" content-position="left">API配置（可选）</el-divider>

        <el-form-item label="API Key" v-if="!isEdit">
          <el-input v-model="editForm.apiKey" placeholder="全局API Key（可选，后续可为各Agent单独配置）" show-password />
        </el-form-item>
        <el-form-item label="API地址" v-if="!isEdit">
          <el-input v-model="editForm.apiUrl" placeholder="API地址，如：https://api.anthropic.com" />
        </el-form-item>
        <el-form-item label="模型" v-if="!isEdit">
          <el-input v-model="editForm.model" placeholder="模型名称，如：claude-sonnet-4-20250514" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 设置目标对话框 -->
    <el-dialog v-model="goalDialogVisible" title="设置项目目标" width="500px">
      <el-form :model="goalForm" label-width="100px">
        <el-form-item label="项目目标" required>
          <el-input
            v-model="goalForm.goal"
            type="textarea"
            :rows="3"
            placeholder="描述项目目标，如：开发一个休闲益智类H5小游戏"
          />
        </el-form-item>
        <el-form-item label="目标类型">
          <el-select v-model="goalForm.goalType" placeholder="选择目标类型" style="width: 100%">
            <el-option label="游戏开发" value="GAME_DEVELOPMENT" />
            <el-option label="功能开发" value="FEATURE" />
            <el-option label="Bug修复" value="BUG_FIX" />
            <el-option label="重构优化" value="REFACTOR" />
            <el-option label="自定义" value="CUSTOM" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker
            v-model="goalForm.deadline"
            type="datetime"
            placeholder="选择截止时间（可选）"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="goalDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveGoal" :loading="savingGoal">保存</el-button>
      </template>
    </el-dialog>

    <!-- 导入项目对话框 -->
    <el-dialog
      v-model="importDialogVisible"
      title="导入项目"
      width="600px"
    >
      <el-alert type="info" :closable="false" class="mb-4">
        <template #title>
          <div>
            <p>导入已有项目目录到系统中。</p>
            <p>系统会自动扫描目录结构，创建项目配置，并关联相关Agent。</p>
          </div>
        </template>
      </el-alert>

      <el-form :model="importForm" label-width="100px">
        <el-form-item label="项目名称" required>
          <el-input v-model="importForm.name" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="importForm.description" type="textarea" :rows="2" placeholder="请输入项目描述" />
        </el-form-item>
        <el-form-item label="工作目录" required>
          <el-input v-model="importForm.workDir" placeholder="请输入已存在的项目目录路径">
            <template #append>
              <el-button @click="checkDirectory">检查</el-button>
            </template>
          </el-input>
          <div v-if="directoryCheckResult" class="directory-check" :class="directoryCheckResult.valid ? 'success' : 'error'">
            {{ directoryCheckResult.message }}
          </div>
        </el-form-item>
        <el-form-item label="模板">
          <el-select v-model="importForm.templateId" placeholder="选择游戏模板（可选）" clearable style="width: 100%">
            <el-option v-for="t in templates" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>

        <el-divider content-position="left">项目目标（可选）</el-divider>

        <el-form-item label="项目目标">
          <el-input v-model="importForm.goal" type="textarea" :rows="2" placeholder="描述项目目标" />
        </el-form-item>
        <el-form-item label="目标类型" v-if="importForm.goal">
          <el-select v-model="importForm.goalType" placeholder="选择目标类型" style="width: 100%">
            <el-option label="游戏开发" value="GAME_DEVELOPMENT" />
            <el-option label="功能开发" value="FEATURE" />
            <el-option label="Bug修复" value="BUG_FIX" />
            <el-option label="重构优化" value="REFACTOR" />
            <el-option label="自定义" value="CUSTOM" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleImportSubmit" :loading="importing">导入</el-button>
      </template>
    </el-dialog>

    <!-- 目录配置对话框 -->
    <el-dialog
      v-model="dirDialogVisible"
      title="目录配置管理"
      width="600px"
    >
      <el-alert type="info" :closable="false" class="mb-4">
        <template #title>
          <div>
            <p>配置项目目录结构，帮助 Agent 理解项目布局。</p>
            <p>配置后，制作人在分配任务时会自动下发目录信息。</p>
          </div>
        </template>
      </el-alert>

      <!-- 已配置的目录列表 -->
      <div v-if="directoryConfigs.length > 0" class="dir-config-list">
        <div v-for="(dir, index) in directoryConfigs" :key="index" class="dir-config-item">
          <div class="dir-config-info">
            <div class="dir-config-path">{{ dir.path }}</div>
            <div class="dir-config-desc">{{ dir.description }}</div>
            <div v-if="dir.notes" class="dir-config-notes">{{ dir.notes }}</div>
          </div>
          <el-button type="danger" size="small" text @click="handleRemoveDir(dir.path)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>
      <el-empty v-else description="暂未配置目录" :image-size="40" />

      <el-divider content-position="left">添加目录配置</el-divider>

      <el-form :model="dirForm" label-width="80px">
        <el-form-item label="目录路径" required>
          <el-input v-model="dirForm.path" placeholder="如：/server、/client、/config" />
        </el-form-item>
        <el-form-item label="用途描述" required>
          <el-input v-model="dirForm.description" placeholder="如：服务端代码、前端客户端、配置文件" />
        </el-form-item>
        <el-form-item label="补充说明">
          <el-input v-model="dirForm.notes" type="textarea" :rows="2" placeholder="如：可访问的角色、注意事项等（可选）" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dirDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleAddDir" :loading="savingDir">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 项目管理页面
 * 管理游戏项目
 *
 * 优化内容：
 * - 统计概览卡片
 * - 卡片/列表视图切换
 * - 搜索和筛选
 * - 项目详情抽屉
 * - 目标进度展示
 * - 快捷操作菜单
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { projectApi, gameTemplateApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search, Grid, List, Folder, CircleCheck, User, Aim,
  MoreFilled, Document, Edit, Delete, Box, Connection,
  FolderOpened, Upload, Plus, Clock
} from '@element-plus/icons-vue'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const projects = ref([])
const templates = ref([])

// 视图模式
const viewMode = ref('card') // card | table
const searchKeyword = ref('')
const filterStatus = ref('')

// 详情抽屉
const drawerVisible = ref(false)
const currentProject = ref(null)
const milestones = ref([])
const directoryConfigs = ref([])

// 编辑/创建
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)

// 目标设置
const goalDialogVisible = ref(false)
const savingGoal = ref(false)
const goalProjectId = ref(null)
const goalForm = ref({
  goal: '',
  goalType: 'GAME_DEVELOPMENT',
  deadline: null
})

// 导入
const importDialogVisible = ref(false)
const importing = ref(false)
const directoryCheckResult = ref(null)

// 目录配置
const dirDialogVisible = ref(false)
const savingDir = ref(false)
const dirForm = ref({
  path: '',
  description: '',
  notes: ''
})

const editForm = ref({
  name: '',
  description: '',
  workDir: '',
  templateId: '',
  goal: '',
  goalType: 'GAME_DEVELOPMENT',
  apiKey: '',
  apiUrl: '',
  model: ''
})

const importForm = ref({
  name: '',
  description: '',
  workDir: '',
  templateId: '',
  goal: '',
  goalType: 'GAME_DEVELOPMENT'
})

// 统计
const activeProjectsCount = computed(() => projects.value.filter(p => p.status === 'ACTIVE').length)
const totalAgentsCount = computed(() => projects.value.reduce((sum, p) => sum + (p.agentIds?.length || 0), 0))
const projectsWithGoalCount = computed(() => projects.value.filter(p => p.goal).length)

/** 筛选后的项目 */
const filteredProjects = computed(() => {
  let result = projects.value
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(p =>
      p.name?.toLowerCase().includes(keyword) ||
      p.description?.toLowerCase().includes(keyword) ||
      p.id?.toLowerCase().includes(keyword)
    )
  }
  if (filterStatus.value) {
    result = result.filter(p => p.status === filterStatus.value)
  }
  return result
})

/** 截断文本 */
const truncateText = (text, maxLen) => {
  if (!text) return ''
  return text.length > maxLen ? text.substring(0, maxLen) + '...' : text
}

/** 截断路径 */
const truncatePath = (path) => {
  if (!path) return '-'
  const parts = path.split('/')
  if (parts.length > 3) {
    return '.../' + parts.slice(-2).join('/')
  }
  return path
}

/** 获取目标状态 */
const getGoalStatus = (status) => {
  if (status === 'COMPLETED') return 'success'
  if (status === 'IN_PROGRESS') return ''
  if (status === 'PAUSED') return 'warning'
  return ''
}

/** 获取目标标签类型 */
const getGoalTagType = (status) => {
  const typeMap = {
    'NOT_STARTED': 'info',
    'IN_PROGRESS': 'primary',
    'COMPLETED': 'success',
    'PAUSED': 'warning',
    'REVIEW': 'warning'
  }
  return typeMap[status] || 'info'
}

/** 获取目标状态文本 */
const getGoalStatusText = (status) => {
  const textMap = {
    'NOT_STARTED': '未开始',
    'IN_PROGRESS': '进行中',
    'COMPLETED': '已完成',
    'PAUSED': '已暂停',
    'REVIEW': '审查中'
  }
  return textMap[status] || status
}

/** 加载项目列表 */
const loadProjects = async () => {
  loading.value = true
  try {
    const data = await projectApi.getAll()
    projects.value = data || []
  } catch (error) {
    ElMessage.error('加载项目列表失败')
  } finally {
    loading.value = false
  }
}

/** 加载模板列表 */
const loadTemplates = async () => {
  try {
    const data = await gameTemplateApi.getAll()
    templates.value = data || []
  } catch (error) {
    console.error('加载模板失败:', error)
  }
}

/** 创建项目 */
const handleCreate = () => {
  isEdit.value = false
  editingId.value = null
  editForm.value = {
    name: '',
    description: '',
    workDir: '',
    templateId: '',
    goal: '',
    goalType: 'GAME_DEVELOPMENT',
    apiKey: '',
    apiUrl: '',
    model: ''
  }
  dialogVisible.value = true
}

/** 查看项目详情 */
const handleViewDetail = async (project) => {
  currentProject.value = project
  drawerVisible.value = true
  // 加载里程碑和目录配置
  await loadMilestones(project.id)
  await loadDirectoryConfigs(project.id)
}

/** 加载里程碑列表 */
const loadMilestones = async (projectId) => {
  try {
    const data = await projectApi.getMilestones(projectId)
    milestones.value = data || []
  } catch (error) {
    console.error('加载里程碑失败:', error)
    milestones.value = []
  }
}

/** 加载目录配置 */
const loadDirectoryConfigs = async (projectId) => {
  try {
    const data = await projectApi.getDirectories(projectId)
    directoryConfigs.value = data ? Object.values(data) : []
  } catch (error) {
    console.error('加载目录配置失败:', error)
    directoryConfigs.value = []
  }
}

/** 获取里程碑状态标签类型 */
const getMilestoneTagType = (status) => {
  const typeMap = {
    'PENDING': 'info',
    'IN_PROGRESS': 'primary',
    'COMPLETED': 'success',
    'BLOCKED': 'warning'
  }
  return typeMap[status] || 'info'
}

/** 获取里程碑状态文本 */
const getMilestoneStatusText = (status) => {
  const textMap = {
    'PENDING': '待开始',
    'IN_PROGRESS': '进行中',
    'COMPLETED': '已完成',
    'BLOCKED': '已阻塞'
  }
  return textMap[status] || status
}

/** 显示目录配置对话框 */
const showDirectoryDialog = () => {
  dirForm.value = { path: '', description: '', notes: '' }
  dirDialogVisible.value = true
}

/** 添加目录配置 */
const handleAddDir = async () => {
  if (!dirForm.value.path) {
    ElMessage.warning('请输入目录路径')
    return
  }
  if (!dirForm.value.description) {
    ElMessage.warning('请输入用途描述')
    return
  }

  savingDir.value = true
  try {
    await projectApi.addDirectory(currentProject.value.id, dirForm.value)
    ElMessage.success('目录配置已添加')
    dirForm.value = { path: '', description: '', notes: '' }
    await loadDirectoryConfigs(currentProject.value.id)
  } catch (error) {
    ElMessage.error('添加失败: ' + (error.message || '未知错误'))
  } finally {
    savingDir.value = false
  }
}

/** 删除目录配置 */
const handleRemoveDir = async (dirPath) => {
  try {
    await ElMessageBox.confirm(`确定要删除目录配置 "${dirPath}" 吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await projectApi.removeDirectory(currentProject.value.id, dirPath)
    ElMessage.success('目录配置已删除')
    await loadDirectoryConfigs(currentProject.value.id)
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

/** 查看代码 */
const handleViewCode = (project) => {
  router.push(`/code/${project.id}`)
  drawerVisible.value = false
}

/** 查看Agent - 跳转到Agent列表页 */
const handleViewAgents = (project) => {
  router.push({ path: '/agents', query: { projectId: project.id } })
  drawerVisible.value = false
}

/** 查看工作流 */
const handleViewWorkflow = (project) => {
  router.push({ path: '/workflow', query: { projectId: project.id } })
  drawerVisible.value = false
}

/** 设置目标 */
const handleSetGoal = (project) => {
  goalProjectId.value = project.id
  goalForm.value = {
    goal: project.goal || '',
    goalType: project.goalType || 'GAME_DEVELOPMENT',
    deadline: project.goalDeadline ? new Date(project.goalDeadline) : null
  }
  goalDialogVisible.value = true
  drawerVisible.value = false
}

/** 保存目标 */
const handleSaveGoal = async () => {
  if (!goalForm.value.goal) {
    ElMessage.warning('请输入项目目标')
    return
  }
  savingGoal.value = true
  try {
    await projectApi.setGoal(goalProjectId.value, goalForm.value)
    ElMessage.success('目标设置成功')
    goalDialogVisible.value = false
    loadProjects()
  } catch (error) {
    ElMessage.error('设置目标失败')
  } finally {
    savingGoal.value = false
  }
}

/** 编辑项目 */
const handleEdit = (project) => {
  isEdit.value = true
  editingId.value = project.id
  editForm.value = {
    name: project.name || '',
    description: project.description || '',
    workDir: project.workDir || '',
    templateId: project.templateId || ''
  }
  dialogVisible.value = true
  drawerVisible.value = false
}

/** 处理下拉菜单命令 */
const handleCommand = (command, project) => {
  switch (command) {
    case 'code':
      handleViewCode(project)
      break
    case 'agents':
      handleViewAgents(project)
      break
    case 'goal':
      handleSetGoal(project)
      break
    case 'edit':
      handleEdit(project)
      break
    case 'archive':
      handleArchive(project)
      break
    case 'delete':
      handleDelete(project)
      break
  }
}

/** 归档项目 */
const handleArchive = async (project) => {
  try {
    await ElMessageBox.confirm(`确定要归档项目 "${project.name}" 吗？归档后项目将变为只读状态。`, '归档确认', {
      confirmButtonText: '归档',
      cancelButtonText: '取消',
      type: 'warning'
    })
    // TODO: 调用归档 API
    ElMessage.success('项目已归档')
    loadProjects()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('归档失败')
    }
  }
}

/** 保存项目 */
const handleSave = async () => {
  if (!editForm.value.name) {
    ElMessage.warning('请输入项目名称')
    return
  }

  saving.value = true
  try {
    if (isEdit.value) {
      // 编辑模式 - 目前后端不支持直接编辑，提示用户
      ElMessage.info('项目信息修改功能暂不支持，如需修改请删除后重新创建')
    } else {
      // 创建模式
      if (!editForm.value.workDir) {
        ElMessage.warning('请输入工作目录')
        return
      }
      await projectApi.create(editForm.value)
      ElMessage.success('项目创建成功')
      dialogVisible.value = false
      loadProjects()
    }
  } catch (error) {
    ElMessage.error(isEdit.value ? '编辑失败' : '创建失败')
  } finally {
    saving.value = false
  }
}

/** 删除项目 */
const handleDelete = async (project) => {
  try {
    await ElMessageBox.confirm(`确定要删除项目 "${project.name}" 吗？此操作不可恢复！`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await projectApi.remove(project.id)
    ElMessage.success('项目已删除')
    drawerVisible.value = false
    loadProjects()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

/** 打开导入对话框 */
const handleImport = () => {
  importForm.value = {
    name: '',
    description: '',
    workDir: '',
    templateId: '',
    goal: '',
    goalType: 'GAME_DEVELOPMENT'
  }
  directoryCheckResult.value = null
  importDialogVisible.value = true
}

/** 检查目录 */
const checkDirectory = async () => {
  if (!importForm.value.workDir) {
    ElMessage.warning('请先输入工作目录路径')
    return
  }

  try {
    const result = await projectApi.checkDirectory(importForm.value.workDir)
    directoryCheckResult.value = result
  } catch (error) {
    directoryCheckResult.value = {
      valid: false,
      message: '检查失败: ' + (error.message || '未知错误')
    }
  }
}

/** 提交导入 */
const handleImportSubmit = async () => {
  if (!importForm.value.name) {
    ElMessage.warning('请输入项目名称')
    return
  }
  if (!importForm.value.workDir) {
    ElMessage.warning('请输入工作目录路径')
    return
  }

  importing.value = true
  try {
    await projectApi.import(importForm.value)
    ElMessage.success('项目导入成功')
    importDialogVisible.value = false
    loadProjects()
  } catch (error) {
    ElMessage.error('导入失败: ' + (error.message || '未知错误'))
  } finally {
    importing.value = false
  }
}

onMounted(() => {
  loadProjects()
  loadTemplates()
})
</script>

<style scoped>
.projects-page {
  padding: 20px;
}

/* 统计卡片 */
.stat-cards {
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
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
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

/* 卡片头部 */
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
  min-width: 200px;
}

.view-toggle {
  flex-shrink: 0;
}

/* 卡片视图 */
.projects-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
  min-height: 200px;
}

.project-card {
  cursor: pointer;
  transition: all 0.3s;
}

.project-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--el-box-shadow-light);
}

.project-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.more-btn {
  cursor: pointer;
  color: var(--el-text-color-secondary);
  font-size: 18px;
}

.more-btn:hover {
  color: var(--el-color-primary);
}

.project-name {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.project-desc {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.project-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.meta-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 目标进度 */
.project-goal {
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
}

.goal-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.goal-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 卡片底部 */
.project-card-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  border-top: 1px solid var(--el-border-color-lighter);
  padding-top: 12px;
}

/* 列表视图 */
.path-text {
  font-family: monospace;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.no-goal {
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

/* 详情抽屉 */
.drawer-actions {
  display: flex;
  gap: 8px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}

/* 导入对话框 */
.mb-4 {
  margin-bottom: 16px;
}

.directory-check {
  margin-top: 8px;
  font-size: 13px;
}

.directory-check.success {
  color: var(--el-color-success);
}

.directory-check.error {
  color: var(--el-color-danger);
}

/* 里程碑列表 */
.milestones-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.milestone-item {
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  padding: 12px;
}

.milestone-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.milestone-order {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--el-color-primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
  flex-shrink: 0;
}

.milestone-info {
  flex: 1;
}

.milestone-title {
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.milestone-role {
  margin-top: 4px;
}

.milestone-desc {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
  padding-left: 40px;
}

.milestone-progress {
  margin-bottom: 8px;
}

.milestone-tasks {
  margin-top: 8px;
  padding-left: 40px;
}

.task-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.task-done {
  color: var(--el-color-success);
}

.task-pending {
  color: var(--el-text-color-placeholder);
}

.task-done-text {
  text-decoration: line-through;
  color: var(--el-text-color-placeholder);
}

/* 目录配置列表 */
.directory-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.directory-item {
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
  padding: 10px 12px;
}

.dir-path {
  display: flex;
  align-items: center;
  gap: 6px;
  font-family: monospace;
  font-size: 13px;
  font-weight: 500;
  color: var(--el-color-primary);
}

.dir-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
  padding-left: 22px;
}

.dir-notes {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  margin-top: 2px;
  padding-left: 22px;
}

/* 目录配置对话框 */
.divider-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dir-config-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.dir-config-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
  padding: 10px 12px;
}

.dir-config-info {
  flex: 1;
}

.dir-config-path {
  font-family: monospace;
  font-size: 13px;
  font-weight: 500;
  color: var(--el-color-primary);
}

.dir-config-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}

.dir-config-notes {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  margin-top: 2px;
}

/* 响应式 */
@media (max-width: 767px) {
  .projects-page {
    padding: 12px;
  }

  .stat-cards {
    margin-bottom: 12px;
  }

  .stat-card {
    padding: 12px;
  }

  .stat-value {
    font-size: 20px;
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

  .projects-grid {
    grid-template-columns: 1fr;
  }

  :deep(.el-dialog),
  :deep(.el-drawer) {
    width: 90% !important;
  }
}
</style>
