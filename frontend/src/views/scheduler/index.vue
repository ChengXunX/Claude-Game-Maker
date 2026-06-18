<template>
  <div class="scheduler-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h2>智能调度中心</h2>
        <span class="subtitle">企业级多Agent协作调度与质量管理</span>
      </div>
      <div class="header-right">
        <el-button @click="loadStatus" :loading="loading">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
      </div>
    </div>

    <!-- 统计概览 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-primary-light-9)">
            <el-icon :size="24" color="var(--el-color-primary)"><User /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ status.totalAgents || 0 }}</div>
            <div class="stat-label">Agent 总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-success-light-9)">
            <el-icon :size="24" color="var(--el-color-success)"><VideoPlay /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ status.runningAgents || 0 }}</div>
            <div class="stat-label">运行中</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-warning-light-9)">
            <el-icon :size="24" color="var(--el-color-warning)"><Loading /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ status.busyAgents || 0 }}</div>
            <div class="stat-label">忙碌中</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: var(--el-color-info-light-9)">
            <el-icon :size="24" color="var(--el-color-info)"><Coffee /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ status.idleAgents || 0 }}</div>
            <div class="stat-label">空闲中</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 主要内容区 - Tab 布局 -->
    <el-tabs v-model="activeTab" type="border-card" class="mt-4">
      <!-- 基础调度 -->
      <el-tab-pane label="调度监控" name="monitor">
        <!-- 制作人状态 -->
        <el-card class="mb-4" v-for="producer in producerStatusList" :key="producer.projectId">
      <template #header>
        <div class="card-header">
          <span>制作人监控 — {{ producer.projectId }}</span>
          <div>
            <el-tag :type="producer.alive ? 'success' : 'danger'" size="small">
              {{ producer.alive ? '运行中' : '已停止' }}
            </el-tag>
            <el-tag :type="producer.busy ? 'warning' : 'info'" size="small" class="ml-2">
              {{ producer.busy ? '忙碌' : '空闲' }}
            </el-tag>
          </div>
        </div>
      </template>

      <el-row :gutter="16">
        <el-col :span="6">
          <el-statistic title="工作周期" :value="producer.workCycleCount || 0" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="分配中的任务" :value="producer.assignedTaskCount || 0" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="审批等待">
            <template #default>
              <el-tag :type="producer.pendingApproval ? 'danger' : 'success'" size="small">
                {{ producer.pendingApproval ? '等待中' : '无' }}
              </el-tag>
            </template>
          </el-statistic>
        </el-col>
        <el-col :span="6">
          <el-statistic title="活跃工作流">
            <template #default>
              <el-tag :type="producer.activeWorkflow ? 'primary' : 'info'" size="small">
                {{ producer.activeWorkflow ? '运行中' : '无' }}
              </el-tag>
            </template>
          </el-statistic>
        </el-col>
      </el-row>

      <!-- 任务追踪表 -->
      <div v-if="producer.assignedTasks && producer.assignedTasks.length > 0" class="mt-4">
        <h4>任务追踪</h4>
        <el-table :data="producer.assignedTasks" size="small" stripe>
          <el-table-column prop="agentName" label="Agent" width="120">
            <template #default="{ row }">
              {{ row.agentName || row.agentId }}
            </template>
          </el-table-column>
          <el-table-column prop="agentRole" label="角色" width="100" />
          <el-table-column prop="taskTitle" label="任务" min-width="150" show-overflow-tooltip />
          <el-table-column label="已耗时" width="100">
            <template #default="{ row }">
              <el-tag :type="row.elapsedMinutes > 30 ? 'danger' : row.elapsedMinutes > 10 ? 'warning' : 'success'" size="small">
                {{ row.elapsedMinutes }} 分钟
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Agent 状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.agentBusy ? 'warning' : 'success'" size="small">
                {{ row.agentBusy ? '忙碌' : '空闲' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <el-empty v-else description="暂无分配中的任务" :image-size="60" />
    </el-card>

    <!-- 制作人决策历史 -->
    <el-card class="mt-4" v-if="producerDecisions.length > 0">
      <template #header>
        <div class="card-header">
          <span>制作人最近决策</span>
          <el-tag type="info" size="small">最近 {{ producerDecisions.length }} 条</el-tag>
        </div>
      </template>
      <el-timeline>
        <el-timeline-item
          v-for="(decision, index) in producerDecisions"
          :key="index"
          :timestamp="formatTime(decision.createdAt)"
          placement="top"
          :type="index === 0 ? 'primary' : ''"
        >
          <el-card shadow="never" class="decision-card">
            <div class="decision-summary">{{ decision.summary }}</div>
            <el-collapse v-if="decision.decision">
              <el-collapse-item title="查看详细决策内容">
                <pre class="decision-detail">{{ truncateText(decision.decision, 1000) }}</pre>
              </el-collapse-item>
            </el-collapse>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <!-- 任务队列 -->
    <el-card class="mt-4">
      <template #header>
        <div class="card-header">
          <span>任务队列</span>
          <div class="header-actions">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索任务..."
              clearable
              style="width: 200px"
              :prefix-icon="Search"
            />
            <el-select v-model="filterStatus" placeholder="任务状态" clearable style="width: 120px">
              <el-option label="全部" value="" />
              <el-option label="待处理" value="PENDING" />
              <el-option label="处理中" value="PROCESSING" />
              <el-option label="执行中" value="RUNNING" />
              <el-option label="已完成" value="COMPLETED" />
              <el-option label="已阻塞" value="BLOCKED" />
              <el-option label="失败" value="FAILED" />
            </el-select>
            <el-button type="primary" @click="handleTrigger" :loading="triggering" v-permission="'agents:manage'">
              <el-icon><VideoPlay /></el-icon> 手动触发
            </el-button>
            <el-button @click="loadStatus" :loading="loading">
              <el-icon><Refresh /></el-icon> 刷新
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="filteredTaskQueue" v-loading="loading" stripe @row-click="handleViewTask">
        <el-table-column prop="taskId" label="任务 ID" width="150" show-overflow-tooltip />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.type === 'MILESTONE' ? 'danger' : row.type === 'TASK' ? 'warning' : 'info'" size="small">
              {{ row.type === 'MILESTONE' ? '里程碑' : row.type === 'TASK' ? '子任务' : '运行时' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="agentId" label="Agent" width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.agentName || row.agentId }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="任务标题" min-width="150" show-overflow-tooltip />
        <el-table-column label="优先级" width="80">
          <template #default="{ row }">
            <el-tag :type="getPriorityType(row.priority)" size="small">
              {{ row.priority || 'NORMAL' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click.stop="handleViewTask(row)">详情</el-button>
            <el-button
              v-if="row.status !== 'COMPLETED' && row.status !== 'CANCELLED'"
              type="danger"
              size="small"
              text
              @click.stop="handleCancelTask(row)"
              v-permission="'agents:manage'"
            >
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && filteredTaskQueue.length === 0" description="暂无待处理任务" />
    </el-card>

    <!-- 调度配置 -->
    <el-card class="mt-4">
      <template #header>
        <div class="card-header">
          <span>调度配置</span>
          <el-tag type="info" size="small">上次调度: {{ formatTime(status.lastScheduleTime) }}</el-tag>
        </div>
      </template>
      <el-form :model="config" label-width="150px" style="max-width: 600px">
        <el-form-item label="调度间隔（秒）">
          <el-input-number v-model="config.intervalSeconds" :min="10" :max="3600" />
          <span class="form-tip">Agent 检查任务的间隔时间</span>
        </el-form-item>
        <el-form-item label="最大并发任务数">
          <el-input-number v-model="config.maxConcurrentTasks" :min="1" :max="100" />
          <span class="form-tip">同时处理的最大任务数量</span>
        </el-form-item>
        <el-form-item label="任务超时（分钟）">
          <el-input-number v-model="config.taskTimeoutMinutes" :min="1" :max="1440" />
          <span class="form-tip">任务执行超时时间</span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSaveConfig" :loading="savingConfig">保存配置</el-button>
          <el-button @click="handleResetConfig">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    </el-tab-pane>

    <!-- 智能调度 -->
      <el-tab-pane label="智能调度" name="intelligent">
        <div class="section-header">
          <h3>Agent 综合评分</h3>
          <el-tag :type="getOverallLoadType()">整体负载: {{ getOverallLoadStatus() }}</el-tag>
        </div>

        <el-alert type="info" :closable="false" class="mb-4">
          综合评估体系：绩效评分(25) + 任务完成率(20) + 工作质量(15) + 协作能力(10) + 角色需求(15) + 活跃程度(10) + 历史贡献(5) = 100分 | 低于30分解雇，低于45分警告
        </el-alert>

        <el-table :data="agentEvaluations" stripe v-loading="loadingEvaluations">
          <el-table-column prop="agentName" label="Agent" width="140" show-overflow-tooltip>
            <template #default="{ row }">
              <div>
                <div style="font-weight: 500">{{ row.agentName }}</div>
                <div style="font-size: 12px; color: var(--el-text-color-secondary)">{{ row.role }}</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="综合评分" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getScoreTagType(row.totalScore)" size="small" effect="dark">
                {{ Math.round(row.totalScore) }}分
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="绩效" width="70" align="center">
            <template #default="{ row }">
              <span :style="{ color: row.performanceScore >= 70 ? '#67c23a' : row.performanceScore >= 50 ? '#e6a23c' : '#f56c6c' }">
                {{ Math.round(row.performanceScore) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="负载" width="80" align="center">
            <template #default="{ row }">
              <el-progress
                :percentage="row.loadScore"
                :status="getLoadProgressStatus(row.loadScore)"
                :stroke-width="8"
                :show-text="false"
              />
            </template>
          </el-table-column>
          <el-table-column label="解雇风险" width="110" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.dismissalStatus === 'EXECUTED'" type="info" size="small" effect="dark">
                已解雇
              </el-tag>
              <el-tag v-else-if="row.dismissalStatus === 'PENDING'" type="danger" size="small" effect="dark">
                待审批
              </el-tag>
              <el-tag v-else-if="row.dismissalRisk > 0" type="danger" size="small">
                高风险
              </el-tag>
              <el-tag v-else type="success" size="small">
                正常
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="历史表现" width="80" align="center">
            <template #default="{ row }">
              {{ Math.round(row.historyScore) }}%
            </template>
          </el-table-column>
          <el-table-column label="评估结论" width="120">
            <template #default="{ row }">
              <el-tag :type="getRecommendationType(row.recommendation)" size="small">
                {{ getRecommendationText(row.recommendation) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="调度优先级" width="100">
            <template #default="{ row }">
              <el-tag :type="getPriorityTypeByEval(row)" size="small">
                {{ getPriorityTextByEval(row) }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>

        <div class="section-header mt-4">
          <h3>调度统计</h3>
        </div>
        <el-row :gutter="16">
          <el-col :span="6">
            <el-statistic title="总调度次数" :value="scheduleStats.totalScheduled || 0" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="成功率">
              <template #default>
                <span :style="{ color: scheduleStats.successRate >= 80 ? '#67c23a' : '#e6a23c' }">
                  {{ formatPercent(scheduleStats.successRate) }}%
                </span>
              </template>
            </el-statistic>
          </el-col>
          <el-col :span="6">
            <el-statistic title="平均耗时" :value="formatDuration(scheduleStats.avgDurationMs)" suffix="秒" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="解雇预警">
              <template #default>
                <span :style="{ color: dismissalInfo.pendingCount > 0 ? '#f56c6c' : '#67c23a' }">
                  {{ dismissalInfo.pendingCount || 0 }} 个待审批
                </span>
              </template>
            </el-statistic>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- 协作管理 -->
      <el-tab-pane label="协作管理" name="collaboration">
        <div class="section-header">
          <h3>协作会话</h3>
          <div style="display: flex; align-items: center; gap: 12px;">
            <el-select v-model="selectedCollabProject" placeholder="选择项目" clearable style="width: 200px" @change="loadCollaborationData">
              <el-option v-for="p in projects" :key="p.id" :label="p.name || p.id" :value="p.id" />
            </el-select>
            <el-tag type="info">完成率: {{ formatPercent(collaborationStats.successRate) }}%</el-tag>
          </div>
        </div>

        <el-table :data="collaborationSessions" stripe v-loading="loadingCollaboration">
          <el-table-column prop="sessionId" label="会话ID" min-width="200" show-overflow-tooltip />
          <el-table-column prop="projectId" label="项目" width="150" />
          <el-table-column label="参与者" width="200">
            <template #default="{ row }">
              <el-tag v-for="agent in row.participantAgentIds?.slice(0, 3)" :key="agent" size="small" class="mr-1">
                {{ getAgentShortName(agent) }}
              </el-tag>
              <el-tag v-if="row.participantAgentIds?.length > 3" size="small" type="info">
                +{{ row.participantAgentIds.length - 3 }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="getCollaborationStatusType(row.status)" size="small">
                {{ getCollaborationStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="步骤" width="100" align="center">
            <template #default="{ row }">
              {{ getCompletedSteps(row) }}/{{ row.steps?.length || 0 }}
            </template>
          </el-table-column>
          <el-table-column label="开始时间" width="170">
            <template #default="{ row }">
              {{ formatTime(row.startTime) }}
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-if="collaborationSessions.length === 0" description="暂无协作会话" />
      </el-tab-pane>

      <!-- 质量门禁 -->
      <el-tab-pane label="质量门禁" name="quality">
        <div class="section-header">
          <h3>质量门禁配置</h3>
          <div style="display: flex; align-items: center; gap: 12px;">
            <el-select v-model="selectedQualityProject" placeholder="选择项目" clearable style="width: 200px" @change="loadQualityData">
              <el-option v-for="p in projects" :key="p.id" :label="p.name || p.id" :value="p.id" />
            </el-select>
            <el-button type="primary" @click="showAssessDialog" :disabled="!selectedQualityProject">
              <el-icon><DataAnalysis /></el-icon> 执行质量评估
            </el-button>
          </div>
        </div>

        <el-table :data="qualityGates" stripe>
          <el-table-column prop="gateId" label="门禁ID" width="120" />
          <el-table-column prop="name" label="名称" width="120" />
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
          <el-table-column label="级别" width="80" align="center">
            <template #default="{ row }">
              <el-tag type="info" size="small">L{{ row.level }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="最低分" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.minScore >= 70 ? 'success' : 'warning'" size="small">
                {{ row.minScore }}分
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="阻塞" width="80" align="center">
            <template #default="{ row }">
              <el-icon v-if="row.blocking" style="color: #f56c6c"><Warning /></el-icon>
              <el-icon v-else style="color: #67c23a"><CircleCheck /></el-icon>
            </template>
          </el-table-column>
          <el-table-column label="检查项" min-width="200">
            <template #default="{ row }">
              <el-tag v-for="item in row.checkItems?.slice(0, 3)" :key="item" size="small" class="mr-1">
                {{ item }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>

        <!-- 质量评估结果 -->
        <div v-if="assessmentResult" class="mt-4">
          <el-divider>评估结果</el-divider>
          <div class="assessment-overview">
            <div class="overall-score">
              <div class="score-circle" :class="getScoreClass(assessmentResult.overallScore)">
                {{ assessmentResult.overallScore }}
              </div>
              <div class="score-label">综合评分</div>
            </div>
            <div class="quality-level">
              <el-tag :type="getQualityLevelType(assessmentResult.qualityLevel)" size="large">
                {{ getQualityLevelText(assessmentResult.qualityLevel) }}
              </el-tag>
              <el-tag :type="assessmentResult.passed ? 'success' : 'danger'" size="large" class="ml-2">
                {{ assessmentResult.passed ? '通过' : '未通过' }}
              </el-tag>
            </div>
          </div>

          <div class="gate-results mt-4">
            <div v-for="(result, gateId) in assessmentResult.gateResults" :key="gateId" class="gate-result-item">
              <div class="gate-header">
                <span class="gate-name">{{ result.gateName }}</span>
                <el-tag :type="result.passed ? 'success' : 'danger'" size="small">
                  {{ result.passed ? '通过' : '未通过' }}
                </el-tag>
              </div>
              <el-progress
                :percentage="result.score"
                :status="result.passed ? 'success' : 'exception'"
                :stroke-width="8"
              />
              <div class="gate-summary">{{ result.summary }}</div>
            </div>
          </div>
        </div>

        <!-- 质量评估历史 -->
        <div v-if="assessmentHistory.length > 0" class="mt-4">
          <el-divider>评估历史</el-divider>
          <el-table :data="assessmentHistory" stripe size="small">
            <el-table-column label="评估时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.assessedAt) }}
              </template>
            </el-table-column>
            <el-table-column label="综合评分" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="getScoreTagType(row.overallScore)" size="small" effect="dark">
                  {{ row.overallScore }}分
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="质量等级" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="getQualityLevelType(row.qualityLevel)" size="small">
                  {{ getQualityLevelText(row.qualityLevel) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="结果" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.passed ? 'success' : 'danger'" size="small">
                  {{ row.passed ? '通过' : '未通过' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 任务门禁 -->
      <el-tab-pane label="任务门禁" name="taskgate">
        <div class="section-header" style="margin-bottom: 16px; display: flex; align-items: center; gap: 12px; flex-wrap: wrap;">
          <span style="font-weight: 600;">任务门禁检查</span>
          <el-select v-model="gateAgentId" placeholder="选择 Agent" style="width: 200px" size="small">
            <el-option v-for="a in agentOptions" :key="a.id" :label="`${a.name} (${a.role})`" :value="a.id" />
          </el-select>
          <el-select v-model="gateProjectId" placeholder="选择项目" style="width: 200px" size="small" clearable>
            <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
          <el-button type="primary" size="small" @click="handleCheckTaskGate" :loading="gateLoading">
            <el-icon><Finished /></el-icon> 检查门禁
          </el-button>
        </div>
        <el-descriptions :column="2" border v-if="gateResult">
          <el-descriptions-item label="检查结果">
            <el-tag :type="gateResult.passed ? 'success' : 'danger'" size="large">
              {{ gateResult.passed ? '通过' : '拦截' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="消息">{{ gateResult.message }}</el-descriptions-item>
          <el-descriptions-item label="Agent" v-if="gateAgentId">{{ gateAgentId }}</el-descriptions-item>
          <el-descriptions-item label="项目" v-if="gateProjectId">{{ gateProjectId }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="gateResult?.pendingItems?.length" style="margin-top: 12px;">
          <el-divider content-position="left">待处理项</el-divider>
          <el-space wrap>
            <el-tag v-for="item in gateResult.pendingItems" :key="item" type="warning">{{ item }}</el-tag>
          </el-space>
        </div>
        <el-empty v-if="!gateResult" description="选择 Agent 和项目后执行门禁检查" />
      </el-tab-pane>
    </el-tabs>

    <!-- 质量评估对话框 -->
    <el-dialog v-model="assessDialogVisible" title="执行质量评估" width="500px">
      <el-form :model="assessForm" label-width="100px">
        <el-form-item label="项目">
          <el-select v-model="assessForm.projectId" placeholder="选择项目" style="width: 100%">
            <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assessDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="executeAssessment" :loading="assessing">
          开始评估
        </el-button>
      </template>
    </el-dialog>

    <!-- 任务详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :title="currentTask?.title || '任务详情'"
      size="450px"
      direction="rtl"
    >
      <template v-if="currentTask">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="任务 ID">{{ currentTask.taskId }}</el-descriptions-item>
          <el-descriptions-item label="类型">
            <el-tag :type="currentTask.type === 'MILESTONE' ? 'danger' : currentTask.type === 'TASK' ? 'warning' : 'info'" size="small">
              {{ currentTask.type === 'MILESTONE' ? '里程碑' : currentTask.type === 'TASK' ? '子任务' : 'Agent 运行时' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Agent">
            {{ currentTask.agentName || currentTask.agentId }}
          </el-descriptions-item>
          <el-descriptions-item v-if="currentTask.projectName" label="项目">
            {{ currentTask.projectName }}
          </el-descriptions-item>
          <el-descriptions-item label="任务标题">{{ currentTask.title }}</el-descriptions-item>
          <el-descriptions-item v-if="currentTask.parentMilestone" label="所属里程碑">
            {{ currentTask.parentMilestone }}
          </el-descriptions-item>
          <el-descriptions-item label="优先级">
            <el-tag :type="getPriorityType(currentTask.priority)" size="small">
              {{ currentTask.priority || 'NORMAL' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentTask.status)" size="small">
              {{ getStatusLabel(currentTask.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="currentTask.pendingMessages" label="待处理消息">
            {{ currentTask.pendingMessages }} 条
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(currentTask.createdAt) }}</el-descriptions-item>
          <el-descriptions-item v-if="currentTask.startedAt" label="开始时间">{{ formatTime(currentTask.startedAt) }}</el-descriptions-item>
          <el-descriptions-item v-if="currentTask.completedAt" label="完成时间">{{ formatTime(currentTask.completedAt) }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="currentTask.description" class="task-description">
          <el-divider>任务描述</el-divider>
          <p>{{ currentTask.description }}</p>
        </div>

        <div v-if="currentTask.result" class="task-result">
          <el-divider>执行结果</el-divider>
          <p>{{ currentTask.result }}</p>
        </div>

        <div v-if="currentTask.error" class="task-error">
          <el-divider>错误信息</el-divider>
          <el-alert type="error" :closable="false">
            {{ currentTask.error }}
          </el-alert>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
/**
 * Agent 调度页面
 * 查看和管理 Agent 调度状态
 *
 * 功能：
 * - 统计概览（总数、运行中、忙碌、空闲）
 * - 任务队列列表（支持搜索和筛选）
 * - 任务详情（侧边抽屉）
 * - 取消任务
 * - 调度配置
 * - 手动触发调度
 *
 * 操作维度：系统级
 * 权限要求：agents:manage
 */
import { ref, computed, onMounted } from 'vue'
import { schedulerApi, projectApi, taskGateApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, User, VideoPlay, Loading, Coffee, Refresh, DataAnalysis, Warning, CircleCheck, Connection } from '@element-plus/icons-vue'

const loading = ref(false)
const triggering = ref(false)
const savingConfig = ref(false)
const status = ref({})
const taskQueue = ref([])
const producerStatusList = ref([])
const producerDecisions = ref([])
const config = ref({
  intervalSeconds: 60,
  maxConcurrentTasks: 10,
  taskTimeoutMinutes: 30
})

// Tab 切换
const activeTab = ref('monitor')

// 任务门禁
const gateLoading = ref(false)
const gateResult = ref(null)
const gateAgentId = ref('')
const gateProjectId = ref('')

// 从状态中提取 Agent 选项
const agentOptions = computed(() => {
  return (status.value.agents || []).map(a => ({
    id: a.agentId || a.id,
    name: a.name || a.agentId || a.id,
    role: a.role || ''
  }))
})

// 搜索和筛选
const searchKeyword = ref('')
const filterStatus = ref('')

// 详情抽屉
const drawerVisible = ref(false)
const currentTask = ref(null)

// 智能调度相关
const scheduleStats = ref({})
const agentLoadList = ref([])
const agentEvaluations = ref([])
const loadingEvaluations = ref(false)
const dismissalInfo = ref({ pendingCount: 0, approvedCount: 0, riskPenalties: {} })

// 协作管理相关
const collaborationStats = ref({})
const collaborationSessions = ref([])
const selectedCollabProject = ref('')
const loadingCollaboration = ref(false)

// 质量门禁相关
const qualityGates = ref([])
const assessDialogVisible = ref(false)
const assessing = ref(false)
const assessForm = ref({ projectId: '' })
const assessmentResult = ref(null)
const assessmentHistory = ref([])
const projects = ref([])
const selectedQualityProject = ref('')

// 筛选后的任务队列
const filteredTaskQueue = computed(() => {
  let result = taskQueue.value

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(t =>
      t.taskId?.toLowerCase().includes(keyword) ||
      t.title?.toLowerCase().includes(keyword) ||
      t.agentId?.toLowerCase().includes(keyword)
    )
  }

  if (filterStatus.value) {
    result = result.filter(t => t.status === filterStatus.value)
  }

  return result
})

/** 获取优先级标签类型 */
const getPriorityType = (priority) => {
  const typeMap = {
    'HIGH': 'danger',
    'NORMAL': 'info',
    'LOW': 'success'
  }
  return typeMap[priority] || 'info'
}

/** 获取状态标签类型 */
const getStatusType = (status) => {
  const typeMap = {
    'PENDING': 'info',
    'PROCESSING': 'warning',
    'RUNNING': 'warning',
    'COMPLETED': 'success',
    'FAILED': 'danger',
    'CANCELLED': 'info',
    'BLOCKED': 'danger',
    'IDLE': 'info'
  }
  return typeMap[status] || 'info'
}

/** 获取状态标签文本 */
const getStatusLabel = (status) => {
  const labelMap = {
    'PENDING': '待处理',
    'PROCESSING': '处理中',
    'RUNNING': '执行中',
    'COMPLETED': '已完成',
    'FAILED': '失败',
    'CANCELLED': '已取消',
    'BLOCKED': '已阻塞',
    'IDLE': '空闲'
  }
  return labelMap[status] || status
}

/** 格式化时间 */
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

/** 截断文本 */
const truncateText = (text, maxLen) => {
  if (!text) return ''
  return text.length > maxLen ? text.substring(0, maxLen) + '...' : text
}

/** 加载调度状态 */
const loadStatus = async () => {
  loading.value = true
  try {
    const [statusData, queueData, configData, producerData, decisionsData] = await Promise.all([
      schedulerApi.getStatus(),
      schedulerApi.getTaskQueue(),
      schedulerApi.getConfig(),
      schedulerApi.getProducerStatus().catch(() => []),
      schedulerApi.getProducerDecisions(10).catch(() => [])
    ])
    status.value = statusData || {}
    taskQueue.value = queueData || []
    producerStatusList.value = producerData || []
    producerDecisions.value = decisionsData || []
    if (configData) {
      config.value = { ...config.value, ...configData }
    }

    // 加载智能调度数据
    await loadIntelligentData()
  } catch (error) {
    ElMessage.error('加载调度状态失败')
  } finally {
    loading.value = false
  }
}

// 加载智能调度数据
const loadIntelligentData = async () => {
  try {
    // 加载调度统计
    const statsData = await schedulerApi.getStats().catch(() => ({}))
    scheduleStats.value = statsData.data || statsData || {}

    // 解雇风险统计
    const dismissalData = scheduleStats.value.dismissalStats || {}
    dismissalInfo.value = {
      pendingCount: dismissalData.pendingCount || 0,
      approvedCount: dismissalData.approvedCount || 0,
      riskPenalties: dismissalData.riskPenalties || {}
    }

    // 转换 Agent 负载为列表，同时获取绩效评分和解雇风险
    const loads = scheduleStats.value.agentLoads || {}
    const performanceScores = scheduleStats.value.agentPerformanceScores || {}
    const riskPenalties = dismissalInfo.value.riskPenalties
    agentLoadList.value = Object.entries(loads).map(([agentId, loadScore]) => ({
      agentId,
      loadScore: Math.round(loadScore),
      performanceScore: performanceScores[agentId] || 70, // 默认绩效评分
      activeTasks: 0,
      completedTasks: 0,
      failedTasks: 0,
      dismissalRisk: riskPenalties[agentId] || 0,
      hasWarning: (riskPenalties[agentId] || 0) > 0
    }))

    // 加载协作统计
    const collabData = await schedulerApi.getCollaborationStats().catch(() => ({}))
    collaborationStats.value = collabData.data || collabData || {}

    // 加载质量门禁配置
    const gatesData = await schedulerApi.getQualityGateConfigs().catch(() => ({}))
    const configs = gatesData.data || gatesData || {}
    qualityGates.value = Object.values(configs)

    // 加载项目列表
    const projectsData = await projectApi.getAll().catch(() => [])
    projects.value = projectsData.data || projectsData || []

    // 设置默认选中项目并加载对应数据
    if (projects.value.length > 0) {
      const defaultProjectId = projects.value[0].id
      selectedQualityProject.value = defaultProjectId
      selectedCollabProject.value = defaultProjectId

      // 加载 Agent 综合评估数据
      loadingEvaluations.value = true
      try {
        const evalData = await schedulerApi.getAgentEvaluations(defaultProjectId).catch(() => [])
        agentEvaluations.value = evalData.data || evalData || []
      } catch (e) {
        console.error('加载 Agent 评估数据失败:', e)
      } finally {
        loadingEvaluations.value = false
      }

      // 加载质量评估历史
      try {
        const historyData = await schedulerApi.getQualityAssessmentHistory(defaultProjectId).catch(() => [])
        assessmentHistory.value = historyData.data || historyData || []
      } catch (e) {
        console.error('加载质量评估历史失败:', e)
      }

      // 加载最新质量评估
      try {
        const latestData = await schedulerApi.getLatestQualityAssessment(defaultProjectId).catch(() => null)
        assessmentResult.value = latestData?.data || latestData || null
      } catch (e) {
        console.error('加载最新质量评估失败:', e)
      }

      // 加载协作会话数据
      loadCollaborationData()
    }
  } catch (e) {
    console.error('加载智能调度数据失败:', e)
  }
}

/** 手动触发调度 */
const handleTrigger = async () => {
  triggering.value = true
  try {
    await schedulerApi.triggerSchedule()
    ElMessage.success('调度已触发')
    loadStatus()
  } catch (error) {
    ElMessage.error('触发失败')
  } finally {
    triggering.value = false
  }
}

/** 查看任务详情 */
const handleViewTask = (task) => {
  currentTask.value = task
  drawerVisible.value = true
}

/** 取消任务 */
const handleCancelTask = async (task) => {
  try {
    await ElMessageBox.confirm('确定要取消该任务吗？', '取消任务', {
      confirmButtonText: '取消任务',
      cancelButtonText: '返回',
      type: 'warning'
    })

    await schedulerApi.cancelTask(task.taskId)
    ElMessage.success('任务已取消')
    loadStatus()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('取消任务失败')
    }
  }
}

/** 保存配置 */
const handleSaveConfig = async () => {
  savingConfig.value = true
  try {
    await schedulerApi.updateConfig(config.value)
    ElMessage.success('配置已保存')
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    savingConfig.value = false
  }
}

/** 重置配置 */
const handleResetConfig = () => {
  config.value = {
    intervalSeconds: 60,
    maxConcurrentTasks: 10,
    taskTimeoutMinutes: 30
  }
  ElMessage.info('配置已重置，请点击保存')
}

// ===== 智能调度相关方法 =====

// 格式化百分比
const formatPercent = (value) => {
  if (value == null) return '0'
  return Math.round(value)
}

// 格式化时长
const formatDuration = (ms) => {
  if (!ms) return '0'
  return Math.round(ms / 1000)
}

// 评分标签类型
const getScoreTagType = (score) => {
  if (score >= 85) return 'success'
  if (score >= 70) return 'primary'
  if (score >= 60) return 'warning'
  return 'danger'
}

// 调度优先级（基于综合评分）
const getPriorityTypeByScore = (agent) => {
  // 有解雇风险的 Agent 标记为危险
  if (agent.dismissalRisk > 0) return 'danger'
  const score = agent.performanceScore || 70
  if (score >= 85) return 'success'
  if (score >= 70) return 'primary'
  if (score >= 60) return 'warning'
  return 'danger'
}

const getPriorityTextByScore = (agent) => {
  // 有解雇风险的 Agent 直接标记为不可调度
  if (agent.dismissalRisk > 0) return '降权调度'
  const score = agent.performanceScore || 70
  if (score >= 85) return '高优先'
  if (score >= 70) return '中优先'
  if (score >= 60) return '低优先'
  return '待观察'
}

// 评估结论相关
const getRecommendationType = (recommendation) => {
  const map = {
    'DISMISS': 'danger',
    'WARN': 'warning',
    'OBSERVE': 'info',
    'KEEP': 'success'
  }
  return map[recommendation] || 'info'
}

const getRecommendationText = (recommendation) => {
  const map = {
    'DISMISS': '建议解雇',
    'WARN': '需警告',
    'OBSERVE': '观察中',
    'KEEP': '表现良好'
  }
  return map[recommendation] || recommendation
}

// 基于综合评估的调度优先级
const getPriorityTypeByEval = (evalData) => {
  if (evalData.recommendation === 'DISMISS' || evalData.dismissalStatus === 'EXECUTED') return 'info'
  if (evalData.recommendation === 'WARN' || evalData.dismissalStatus === 'PENDING') return 'danger'
  if (evalData.totalScore >= 70) return 'success'
  if (evalData.totalScore >= 50) return 'primary'
  return 'warning'
}

const getPriorityTextByEval = (evalData) => {
  if (evalData.dismissalStatus === 'EXECUTED') return '已解雇'
  if (evalData.dismissalStatus === 'PENDING') return '待审批'
  if (evalData.recommendation === 'DISMISS') return '建议解雇'
  if (evalData.recommendation === 'WARN') return '降权调度'
  if (evalData.totalScore >= 70) return '高优先'
  if (evalData.totalScore >= 50) return '中优先'
  return '低优先'
}

// Agent 负载相关
const getOverallLoadType = () => {
  const list = agentEvaluations.value.length > 0 ? agentEvaluations.value : agentLoadList.value
  const avgLoad = list.length > 0
    ? list.reduce((sum, a) => sum + (a.loadScore || 0), 0) / list.length
    : 0
  if (avgLoad < 30) return 'success'
  if (avgLoad < 70) return 'warning'
  return 'danger'
}

const getOverallLoadStatus = () => {
  const list = agentEvaluations.value.length > 0 ? agentEvaluations.value : agentLoadList.value
  const avgLoad = list.length > 0
    ? list.reduce((sum, a) => sum + (a.loadScore || 0), 0) / list.length
    : 0
  if (avgLoad < 30) return '低'
  if (avgLoad < 70) return '中'
  return '高'
}

const getLoadProgressStatus = (score) => {
  if (score < 30) return 'success'
  if (score < 70) return ''
  return 'exception'
}

const getAgentLoadStatusType = (agent) => {
  if (agent.loadScore > 70) return 'danger'
  if (agent.loadScore > 30) return 'warning'
  return 'success'
}

const getAgentLoadStatusText = (agent) => {
  if (agent.loadScore > 70) return '高负载'
  if (agent.loadScore > 30) return '中等'
  return '空闲'
}

// 协作管理相关
const getAgentShortName = (agentId) => {
  if (!agentId) return ''
  const parts = agentId.split(':')
  return parts[parts.length - 1] || agentId
}

const getCollaborationStatusType = (status) => {
  const map = {
    'INITIATED': 'info',
    'IN_PROGRESS': 'warning',
    'WAITING_INPUT': 'warning',
    'COMPLETED': 'success',
    'FAILED': 'danger',
    'CANCELLED': 'info'
  }
  return map[status] || 'info'
}

const getCollaborationStatusText = (status) => {
  const map = {
    'INITIATED': '已发起',
    'IN_PROGRESS': '进行中',
    'WAITING_INPUT': '等待输入',
    'COMPLETED': '已完成',
    'FAILED': '失败',
    'CANCELLED': '已取消'
  }
  return map[status] || status
}

const getCompletedSteps = (session) => {
  if (!session.steps) return 0
  return session.steps.filter(s => s.status === 'COMPLETED').length
}

// 质量门禁相关
const showAssessDialog = () => {
  assessForm.value.projectId = selectedQualityProject.value || projects.value[0]?.id || ''
  assessDialogVisible.value = true
}

/** 加载协作管理数据 */
const loadCollaborationData = async () => {
  if (!selectedCollabProject.value) {
    collaborationSessions.value = []
    collaborationStats.value = {}
    return
  }
  loadingCollaboration.value = true
  try {
    const [sessionsData, statsData] = await Promise.all([
      schedulerApi.getProjectCollaborations(selectedCollabProject.value).catch(() => []),
      schedulerApi.getCollaborationStats().catch(() => ({}))
    ])
    collaborationSessions.value = sessionsData.data || sessionsData || []
    collaborationStats.value = statsData.data || statsData || {}
  } catch (e) {
    console.error('加载协作数据失败:', e)
  } finally {
    loadingCollaboration.value = false
  }
}

/** 加载质量门禁数据 */
const loadQualityData = async () => {
  if (!selectedQualityProject.value) {
    assessmentHistory.value = []
    assessmentResult.value = null
    return
  }
  try {
    const [historyData, latestData] = await Promise.all([
      schedulerApi.getQualityAssessmentHistory(selectedQualityProject.value).catch(() => []),
      schedulerApi.getLatestQualityAssessment(selectedQualityProject.value).catch(() => null)
    ])
    assessmentHistory.value = historyData.data || historyData || []
    assessmentResult.value = latestData?.data || latestData || null
  } catch (e) {
    console.error('加载质量门禁数据失败:', e)
  }
}

const executeAssessment = async () => {
  if (!assessForm.value.projectId) {
    ElMessage.warning('请选择项目')
    return
  }

  assessing.value = true
  try {
    const project = projects.value.find(p => p.id === assessForm.value.projectId)
    const data = await schedulerApi.assessQuality(assessForm.value.projectId, {
      projectDir: project?.workDir || '',
      projectName: project?.name || '',
      projectGoal: project?.goal || ''
    })

    assessmentResult.value = data.data || data
    assessDialogVisible.value = false
    ElMessage.success('质量评估完成')
  } catch (e) {
    ElMessage.error('质量评估失败: ' + (e.message || '未知错误'))
  } finally {
    assessing.value = false
  }
}

const getScoreClass = (score) => {
  if (score >= 90) return 'score-excellent'
  if (score >= 75) return 'score-good'
  if (score >= 60) return 'score-fair'
  return 'score-poor'
}

const getQualityLevelType = (level) => {
  const map = {
    'EXCELLENT': 'success',
    'GOOD': 'success',
    'ACCEPTABLE': 'warning',
    'POOR': 'danger',
    'CRITICAL': 'danger'
  }
  return map[level] || 'info'
}

const getQualityLevelText = (level) => {
  const map = {
    'EXCELLENT': '优秀',
    'GOOD': '良好',
    'ACCEPTABLE': '可接受',
    'POOR': '较差',
    'CRITICAL': '严重不足'
  }
  return map[level] || level
}

/** 检查任务门禁 */
const handleCheckTaskGate = async () => {
  if (!gateAgentId.value) {
    ElMessage.warning('请先选择 Agent')
    return
  }
  gateLoading.value = true
  gateResult.value = null
  try {
    const res = await taskGateApi.check({
      projectId: gateProjectId.value || '',
      agentId: gateAgentId.value,
      isSubAgent: false,
      currentGateCount: 0
    })
    gateResult.value = res.data
    if (res.data?.passed) {
      ElMessage.success('门禁检查通过')
    }
  } catch (e) {
    ElMessage.error('门禁检查失败')
  } finally {
    gateLoading.value = false
  }
}

onMounted(() => {
  loadStatus()
})
</script>

<style scoped>
.scheduler-page {
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
  align-items: center;
  flex-wrap: wrap;
}

/* 表单提示 */
.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

/* 页面头部 */
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

/* 区块头部 */
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header h3 {
  margin: 0;
}

.mr-1 {
  margin-right: 4px;
}

.mb-4 {
  margin-bottom: 16px;
}

.mt-4 {
  margin-top: 16px;
}

/* 质量评估结果 */
.assessment-overview {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 20px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
}

.overall-score {
  text-align: center;
}

.score-circle {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: bold;
  color: white;
}

.score-excellent {
  background: linear-gradient(135deg, #67c23a, #4caf50);
}

.score-good {
  background: linear-gradient(135deg, #409eff, #2196f3);
}

.score-fair {
  background: linear-gradient(135deg, #e6a23c, #ff9800);
}

.score-poor {
  background: linear-gradient(135deg, #f56c6c, #f44336);
}

.score-label {
  margin-top: 8px;
  color: var(--el-text-color-secondary);
}

.quality-level {
  display: flex;
  align-items: center;
}

.ml-2 {
  margin-left: 8px;
}

.gate-results {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.gate-result-item {
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
}

.gate-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.gate-name {
  font-weight: 500;
}

.gate-summary {
  margin-top: 8px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

/* 任务详情 */
.task-description,
.task-result,
.task-error {
  margin-top: 16px;
}

.task-description p,
.task-result p {
  margin: 0;
  padding: 12px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  line-height: 1.6;
}

.mt-4 {
  margin-top: 16px;
}

/* 响应式 */
@media (max-width: 767px) {
  .scheduler-page {
    padding: 12px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
  }

  :deep(.el-dialog),
  :deep(.el-drawer) {
    width: 90% !important;
  }
}

/* 制作人决策卡片 */
.decision-card {
  margin-bottom: 0;
}

.decision-card :deep(.el-card__body) {
  padding: 12px;
}

.decision-summary {
  font-weight: 500;
  font-size: 14px;
  color: var(--el-text-color-primary);
  line-height: 1.6;
}

.decision-detail {
  font-family: 'Courier New', Consolas, Monaco, monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-wrap: break-word;
  background: var(--el-fill-color-light);
  padding: 12px;
  border-radius: 4px;
  margin: 8px 0 0 0;
  max-height: 400px;
  overflow-y: auto;
}

:deep(.el-timeline-item__content) {
  width: 100%;
}

:deep(.el-collapse-item__header) {
  font-size: 13px;
  height: 32px;
  line-height: 32px;
}

.mt-4 {
  margin-top: 16px;
}

.ml-2 {
  margin-left: 8px;
}
</style>
