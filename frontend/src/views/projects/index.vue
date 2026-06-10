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
            <el-tag :type="getProjectStatusType(project.status)" size="small">
              {{ getProjectStatusText(project.status) }}
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
              <el-tag :type="getProjectStatusType(row.status)" size="small">
                {{ getProjectStatusText(row.status) }}
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
            <el-tag :type="getProjectStatusType(currentProject.status)" size="small">
              {{ getProjectStatusText(currentProject.status) }}
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
            <!-- 验证信息 -->
            <div v-if="milestone.verificationCriteria && milestone.verificationCriteria.length > 0" class="verification-section">
              <div class="verification-title">验证标准：</div>
              <ul class="verification-list">
                <li v-for="(criteria, idx) in milestone.verificationCriteria" :key="idx">{{ criteria }}</li>
              </ul>
            </div>
            <div v-if="milestone.verificationResult" class="verification-result">
              <el-tag :type="milestone.verificationResult.includes('通过') ? 'success' : 'warning'" size="small">
                {{ milestone.verificationResult }}
              </el-tag>
            </div>
            <!-- 人工验证按钮（未完成的里程碑都可以验证） -->
            <div v-if="milestone.status !== 'COMPLETED'" class="milestone-actions">
              <el-button size="small" type="success" @click="handleVerifyMilestone(milestone, true)">
                <el-icon><CircleCheck /></el-icon> 验证通过
              </el-button>
              <el-button size="small" type="warning" @click="handleVerifyMilestone(milestone, false)">
                <el-icon><CircleClose /></el-icon> 验证未通过
              </el-button>
              <el-button size="small" type="primary" @click="handleInterveneMilestone(milestone)">
                <el-icon><Warning /></el-icon> 发送干预
              </el-button>
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

        <!-- 项目概况 -->
        <el-divider>
          <div class="divider-title">
            <span>项目概况</span>
            <el-tag :type="currentProject.running ? 'success' : 'info'" size="small">
              {{ currentProject.running ? '运行中' : '已停止' }}
            </el-tag>
          </div>
        </el-divider>
        <div v-if="currentProject.projectOverview" class="overview-content">
          <MarkdownRenderer :content="currentProject.projectOverview" />
        </div>
        <el-empty v-else description="暂无项目概况" :image-size="60">
          <div class="overview-tip">里程碑完成后将自动更新项目概况</div>
        </el-empty>

        <!-- 项目规则 -->
        <el-divider>
          <div class="divider-title">
            <span>项目规则</span>
            <el-button size="small" text @click="showRulesDialog">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
          </div>
        </el-divider>
        <div v-if="projectRules" class="rules-content">
          <MarkdownRenderer :content="projectRules" />
        </div>
        <el-empty v-else description="暂未设置项目规则" :image-size="60">
          <el-button size="small" @click="showRulesDialog">设置规则</el-button>
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
          <div v-if="editForm.templateId" class="form-tip" style="color: #67c23a;">已自动填充模板推荐的 Agent</div>
        </el-form-item>

        <el-divider v-if="!isEdit" content-position="left">
          默认 Agent（{{ editForm.agents.length + 1 }} 个，制作人固定 + {{ editForm.agents.length }} 个可选）
        </el-divider>

        <!-- 制作人（固定，不可取消） -->
        <div v-if="!isEdit" class="agent-select-grid" style="margin-bottom: 12px;">
          <div class="agent-select-item active fixed-agent">
            <div class="agent-select-icon">🎬</div>
            <div class="agent-select-info">
              <div class="agent-select-name">制作人 <el-tag size="small" type="danger">必须</el-tag></div>
              <div class="agent-select-desc">协调团队、分配任务、审查工作</div>
            </div>
            <el-icon class="agent-check"><CircleCheck /></el-icon>
          </div>
        </div>

        <div v-if="!isEdit" class="agent-select-grid">
          <div
            v-for="role in availableRoles"
            :key="role.role"
            class="agent-select-item"
            :class="{ active: editForm.agents.includes(role.role) }"
            @click="toggleAgent(role.role)"
          >
            <div class="agent-select-icon">{{ role.icon }}</div>
            <div class="agent-select-info">
              <div class="agent-select-name">{{ role.name }}</div>
              <div class="agent-select-desc">{{ role.description }}</div>
            </div>
            <el-icon v-if="editForm.agents.includes(role.role)" class="agent-check"><CircleCheck /></el-icon>
          </div>
        </div>

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

    <!-- 规则编辑对话框 -->
    <el-dialog
      v-model="rulesDialogVisible"
      title="编辑项目规则"
      width="600px"
    >
      <el-alert type="info" :closable="false" class="mb-4">
        <template #title>
          <div>
            <p>项目规则将通知给所有团队成员，帮助他们理解项目规范。</p>
            <p>建议包含：代码规范、目录结构、运行部署流程、协作规范等。</p>
          </div>
        </template>
      </el-alert>

      <el-form :model="rulesForm" label-width="0">
        <el-form-item>
          <MarkdownEditor v-model="rulesForm.rules" :rows="15" placeholder="请输入项目规则..." />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="rulesDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveRules" :loading="savingRules">保存</el-button>
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
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { projectApi, gameTemplateApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search, Grid, List, Folder, CircleCheck, User, Aim,
  MoreFilled, Document, Edit, Delete, Box, Connection,
  FolderOpened, Upload, Plus, Clock, Warning, CircleClose
} from '@element-plus/icons-vue'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'

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
const projectRules = ref('')

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

// 可选的 Agent 角色列表（制作人是固定的，不在此列表中）
const availableRoles = [
  { role: 'system-planner', name: '系统策划', description: '游戏系统设计、玩法策划', icon: '📋' },
  { role: 'server-dev', name: '服务端开发', description: '后端逻辑、API接口、数据库', icon: '⚙️' },
  { role: 'client-dev', name: '客户端开发', description: '前端逻辑、交互实现', icon: '💻' },
  { role: 'ui-dev', name: 'UI设计', description: '界面设计、图标制作、视觉效果', icon: '🎨' },
  { role: 'numerical-planner', name: '数值策划', description: '数值平衡、经济系统', icon: '📊' },
  { role: 'tester', name: '测试工程师', description: '功能测试、性能测试、Bug报告', icon: '🧪' },
  { role: 'git-commit', name: 'Git专员', description: '代码提交、分支管理、版本控制', icon: '📦' },
  { role: 'security-expert', name: '安全工程师', description: '安全审计、漏洞检测', icon: '🛡️' },
  { role: 'data-analyst', name: '数据分析师', description: '玩家行为分析、留存分析', icon: '📈' },
  { role: 'ai-engineer', name: 'AI工程师', description: 'NPC行为AI、寻路算法', icon: '🤖' },
  { role: 'devops', name: '运维工程师', description: 'CI/CD、服务器部署、监控', icon: '🚀' },
  { role: 'audio-dev', name: '音频设计师', description: '音效设计、背景音乐', icon: '🎵' },
  { role: 'narrative-planner', name: '剧情策划', description: '世界观构建、剧情设计', icon: '📖' },
  { role: 'level-design', name: '关卡设计师', description: '关卡流程、地图布局', icon: '🗺️' },
  { role: 'performance-engineer', name: '性能优化', description: '性能分析、瓶颈定位', icon: '⚡' }
]

// 模板对应的默认 Agent 配置（前端预设，制作人已固定，此处只列可选角色）
const templateAgentPresets = {
  'h5-casual': ['server-dev', 'client-dev', 'system-planner', 'ui-dev'],
  'rpg-server': ['server-dev', 'system-planner', 'numerical-planner', 'tester'],
  'unity-mobile': ['client-dev', 'ui-dev', 'system-planner', 'tester']
}

const editForm = ref({
  name: '',
  description: '',
  workDir: '',
  templateId: '',
  agents: ['system-planner'],
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

// 监听模板选择，自动更新 Agent 列表
watch(() => editForm.value.templateId, (newTemplateId) => {
  if (isEdit.value) return
  if (newTemplateId && templateAgentPresets[newTemplateId]) {
    editForm.value.agents = [...templateAgentPresets[newTemplateId]]
  } else if (!newTemplateId) {
    // 清空模板时恢复默认
    editForm.value.agents = ['system-planner']
  }
})

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
/** 切换 Agent 选择 */
const toggleAgent = (role) => {
  const idx = editForm.value.agents.indexOf(role)
  if (idx >= 0) {
    // 至少保留一个 Agent
    if (editForm.value.agents.length > 1) {
      editForm.value.agents.splice(idx, 1)
    }
  } else {
    editForm.value.agents.push(role)
  }
}

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

/** 获取项目状态文本 */
const getProjectStatusText = (status) => {
  const map = { 'ACTIVE': '活跃', 'PAUSED': '暂停', 'COMPLETED': '已完成', 'ARCHIVED': '已归档', 'CREATED': '已创建' }
  return map[status] || status
}

/** 获取项目状态标签类型 */
const getProjectStatusType = (status) => {
  if (status === 'ACTIVE') return 'success'
  if (status === 'COMPLETED') return 'success'
  if (status === 'ARCHIVED') return 'info'
  if (status === 'PAUSED') return 'warning'
  return 'info'
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
    agents: ['system-planner'],
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
  // 加载里程碑、目录配置和项目规则
  await loadMilestones(project.id)
  await loadDirectoryConfigs(project.id)
  await loadProjectRules(project.id)
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

/** 加载项目规则 */
const loadProjectRules = async (projectId) => {
  try {
    const data = await projectApi.getVerificationDoc(projectId)
    projectRules.value = data?.rules || ''
  } catch (error) {
    console.error('加载项目规则失败:', error)
    projectRules.value = ''
  }
}

/** 显示规则编辑对话框 */
/** 规则编辑对话框 */
const rulesDialogVisible = ref(false)
const rulesForm = ref({ rules: '' })
const savingRules = ref(false)

const showRulesDialog = () => {
  rulesForm.value.rules = projectRules.value || ''
  rulesDialogVisible.value = true
}

const handleSaveRules = async () => {
  savingRules.value = true
  try {
    await projectApi.setRules(currentProject.value.id, { rules: rulesForm.value.rules })
    ElMessage.success('项目规则已保存')
    projectRules.value = rulesForm.value.rules
    rulesDialogVisible.value = false
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    savingRules.value = false
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

/** 人工验证里程碑 */
const handleVerifyMilestone = async (milestone, passed) => {
  try {
    const { value: comment } = await ElMessageBox.prompt(
      `请确认验证 ${passed ? '通过' : '未通过'} 里程碑 "${milestone.title}"`,
      passed ? '验证通过' : '验证未通过',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入验证备注（可选）',
        inputType: 'textarea'
      }
    )

    const result = await projectApi.verifyMilestone(currentProject.value.id, milestone.id, {
      passed,
      comment: comment || ''
    })

    if (result.success) {
      ElMessage.success(result.message)
      // 刷新里程碑列表
      loadMilestones(currentProject.value.id)
    } else {
      ElMessage.error(result.message || '验证失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('验证操作失败')
    }
  }
}

/** 对里程碑发送干预 */
const handleInterveneMilestone = (milestone) => {
  // 跳转到干预页面，传递项目和里程碑信息
  const projectId = currentProject.value.id
  const agentId = `${projectId}:${milestone.assignedAgentRole}`
  router.push({
    path: '/interventions',
    query: {
      agentId,
      milestoneId: milestone.id,
      milestoneTitle: milestone.title
    }
  })
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
    await ElMessageBox.confirm(`确定要归档项目 "${project.name}" 吗？归档后项目将变为只读状态，所有 Agent 将被停止。`, '归档确认', {
      confirmButtonText: '归档',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const result = await projectApi.archive(project.id)
    if (result.success) {
      ElMessage.success('项目已归档')
      loadProjects()
    } else {
      ElMessage.error(result.message || '归档失败')
    }
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
  cursor: default;
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  min-height: 80px;
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
  min-width: 0;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: var(--el-text-color-primary);
  line-height: 1.2;
  white-space: nowrap;
}

.stat-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
  white-space: nowrap;
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

/* Agent 选择网格 */
.agent-select-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 10px;
  max-height: 320px;
  overflow-y: auto;
  padding: 4px;
}

.agent-select-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 2px solid var(--el-border-color-lighter);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.agent-select-item:hover {
  border-color: var(--el-color-primary-light-5);
  background: var(--el-color-primary-light-9);
}

.agent-select-item.active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.agent-select-item.fixed-agent {
  cursor: default;
  opacity: 0.85;
  border-style: dashed;
}

.agent-select-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.agent-select-info {
  flex: 1;
  min-width: 0;
}

.agent-select-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.agent-select-desc {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.agent-check {
  color: var(--el-color-primary);
  font-size: 18px;
  flex-shrink: 0;
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

/* 验证部分样式 */
.verification-section {
  margin-top: 8px;
  padding-left: 40px;
}

.verification-title {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
}

.verification-list {
  margin: 0;
  padding-left: 16px;
  font-size: 12px;
  color: var(--el-text-color-regular);
}

.verification-list li {
  margin-bottom: 2px;
}

.verification-result {
  margin-top: 8px;
  padding-left: 40px;
}

.milestone-actions {
  margin-top: 12px;
  padding-left: 40px;
  display: flex;
  gap: 8px;
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

/* 项目概况样式 */
.overview-content {
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
  padding: 12px;
  max-height: 400px;
  overflow-y: auto;
}

.overview-text {
  font-family: monospace;
  font-size: 12px;
  color: var(--el-text-color-regular);
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
}

.overview-tip {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  margin-top: 8px;
}

/* 项目规则样式 */
.rules-content {
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
  padding: 12px;
  max-height: 300px;
  overflow-y: auto;
}

.rules-text {
  font-family: monospace;
  font-size: 12px;
  color: var(--el-text-color-regular);
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
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
