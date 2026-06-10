<template>
  <div class="template-form-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ isEdit ? '编辑模板' : '创建模板' }}</span>
          <el-button @click="router.push('/notification-templates')">返回列表</el-button>
        </div>
      </template>

      <!-- 预设模板 -->
      <div v-if="!isEdit" class="preset-section">
        <div class="preset-label">快速填充预设：</div>
        <div class="preset-buttons">
          <el-button v-for="p in presets" :key="p.key" size="small" @click="applyPreset(p)">
            {{ p.label }}
          </el-button>
        </div>
        <el-divider />
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" v-loading="loading">
        <el-form-item label="模板编码" prop="templateCode">
          <el-input v-model="form.templateCode" placeholder="如：ALERT_EMAIL_001" :disabled="isEdit" />
          <div class="form-tip">唯一标识，创建后不可修改</div>
        </el-form-item>

        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="form.templateName" placeholder="如：告警邮件通知模板" />
        </el-form-item>

        <el-form-item label="通知渠道" prop="channel">
          <el-select v-model="form.channel" placeholder="请选择通知渠道">
            <el-option label="邮件" value="EMAIL" />
            <el-option label="飞书" value="FEISHU" />
            <el-option label="钉钉" value="DINGTALK" />
            <el-option label="站内信" value="SYSTEM" />
          </el-select>
        </el-form-item>

        <el-form-item label="模板分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择模板分类">
            <el-option label="告警通知" value="ALERT" />
            <el-option label="任务通知" value="TASK" />
            <el-option label="Agent通知" value="AGENT" />
            <el-option label="系统通知" value="SYSTEM" />
          </el-select>
        </el-form-item>

        <el-form-item label="主题模板" prop="subject" v-if="form.channel !== 'SYSTEM'">
          <el-input v-model="form.subject" placeholder="支持变量替换，如：[${priority}] ${title}" />
          <div class="form-tip">邮件主题或飞书/钉钉消息标题</div>
        </el-form-item>

        <el-form-item label="内容模板" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="10" placeholder="支持HTML和变量替换" />
          <div class="form-tip">
            支持变量替换，格式：${variableName}
            <el-button type="primary" link @click="showVariables = true">查看可用变量</el-button>
          </div>
        </el-form-item>

        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="模板用途说明" />
        </el-form-item>

        <el-form-item label="启用状态">
          <el-switch v-model="form.enabled" active-text="启用" inactive-text="禁用" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            {{ isEdit ? '保存修改' : '创建模板' }}
          </el-button>
          <el-button @click="handlePreview" :disabled="!form.content">预览效果</el-button>
          <el-button @click="router.push('/notification-templates')">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 变量说明对话框 -->
    <el-dialog v-model="showVariables" title="可用变量说明" width="700px">
      <el-tabs>
        <el-tab-pane label="通用变量">
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="${title}">消息标题</el-descriptions-item>
            <el-descriptions-item label="${content}">消息内容</el-descriptions-item>
            <el-descriptions-item label="${time}">当前时间（yyyy-MM-dd HH:mm:ss）</el-descriptions-item>
            <el-descriptions-item label="${date}">当前日期（yyyy-MM-dd）</el-descriptions-item>
            <el-descriptions-item label="${systemName}">系统名称</el-descriptions-item>
            <el-descriptions-item label="${userName}">接收用户名称</el-descriptions-item>
            <el-descriptions-item label="${userEmail}">接收用户邮箱</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="告警相关">
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="${priority}">优先级（英文：HIGH/MEDIUM/LOW）</el-descriptions-item>
            <el-descriptions-item label="${priorityDesc}">优先级描述（中文：高/中/低）</el-descriptions-item>
            <el-descriptions-item label="${ruleName}">告警规则名称</el-descriptions-item>
            <el-descriptions-item label="${ruleId}">告警规则 ID</el-descriptions-item>
            <el-descriptions-item label="${metric}">监控指标名称</el-descriptions-item>
            <el-descriptions-item label="${triggerValue}">触发值</el-descriptions-item>
            <el-descriptions-item label="${thresholdValue}">阈值</el-descriptions-item>
            <el-descriptions-item label="${alertType}">告警类型（触发/恢复）</el-descriptions-item>
            <el-descriptions-item label="${alertDuration}">告警持续时间</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="Agent 相关">
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="${agentId}">Agent ID</el-descriptions-item>
            <el-descriptions-item label="${agentName}">Agent 名称</el-descriptions-item>
            <el-descriptions-item label="${agentRole}">Agent 角色</el-descriptions-item>
            <el-descriptions-item label="${agentStatus}">Agent 状态</el-descriptions-item>
            <el-descriptions-item label="${agentProject}">Agent 所属项目</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="任务相关">
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="${taskTitle}">任务标题</el-descriptions-item>
            <el-descriptions-item label="${taskId}">任务 ID</el-descriptions-item>
            <el-descriptions-item label="${taskResult}">任务结果</el-descriptions-item>
            <el-descriptions-item label="${taskStatus}">任务状态</el-descriptions-item>
            <el-descriptions-item label="${taskDuration}">任务耗时</el-descriptions-item>
            <el-descriptions-item label="${milestoneName}">里程碑名称</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="项目相关">
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="${projectId}">项目 ID</el-descriptions-item>
            <el-descriptions-item label="${projectName}">项目名称</el-descriptions-item>
            <el-descriptions-item label="${projectStatus}">项目状态</el-descriptions-item>
            <el-descriptions-item label="${projectGoal}">项目目标</el-descriptions-item>
            <el-descriptions-item label="${projectProgress}">项目进度</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="审批相关">
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="${approvalId}">审批 ID</el-descriptions-item>
            <el-descriptions-item label="${approvalType}">审批类型</el-descriptions-item>
            <el-descriptions-item label="${approvalStatus}">审批状态</el-descriptions-item>
            <el-descriptions-item label="${approvalReason}">审批原因</el-descriptions-item>
            <el-descriptions-item label="${applicantName}">申请人名称</el-descriptions-item>
            <el-descriptions-item label="${approverName}">审批人名称</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
      </el-tabs>
      <div class="variable-tip">
        <el-icon><InfoFilled /></el-icon>
        <span>变量格式：${variableName}，在模板中使用变量会自动替换为实际值</span>
      </div>
    </el-dialog>

    <!-- 预览对话框 -->
    <el-dialog v-model="showPreview" title="模板预览" width="700px">
      <div class="preview-container">
        <div class="preview-item" v-if="previewResult.subject">
          <h4>主题：</h4>
          <div class="preview-content">{{ previewResult.subject }}</div>
        </div>
        <div class="preview-item">
          <h4>内容：</h4>
          <div class="preview-content" v-html="previewResult.content"></div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 通知模板表单页面
 * 支持创建和编辑通知模板
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { templateApi } from '@/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const showVariables = ref(false)
const showPreview = ref(false)
const previewResult = ref({ subject: '', content: '' })

const isEdit = computed(() => !!route.params.id)

const presets = [
  { key: 'alert', label: '告警通知', templateCode: 'ALERT_CUSTOM', channel: 'SYSTEM', category: 'ALERT',
    subject: '[${priorityDesc}] 告警: ${ruleName}',
    content: '监控告警已触发\n\n规则：${ruleName}\n级别：${priorityDesc}\n当前值：${triggerValue}\n阈值：${thresholdValue}\n时间：${time}' },
  { key: 'task', label: '任务通知', templateCode: 'TASK_CUSTOM', channel: 'SYSTEM', category: 'TASK',
    subject: '任务通知: ${taskTitle}',
    content: '任务：${taskTitle}\n结果：${taskResult}\n时间：${time}' },
  { key: 'agent', label: 'Agent通知', templateCode: 'AGENT_CUSTOM', channel: 'SYSTEM', category: 'AGENT',
    subject: 'Agent通知: ${agentName}',
    content: 'Agent：${agentName}\n${content}\n时间：${time}' },
  { key: 'approval', label: '审批通知', templateCode: 'APPROVAL_CUSTOM', channel: 'SYSTEM', category: 'SYSTEM',
    subject: '审批通知: ${title}',
    content: '${content}\n时间：${time}' },
  { key: 'system', label: '系统通知', templateCode: 'SYSTEM_CUSTOM', channel: 'SYSTEM', category: 'SYSTEM',
    subject: '系统通知: ${title}',
    content: '${content}\n时间：${time}' }
]

const applyPreset = (preset) => {
  form.value.templateCode = preset.templateCode
  form.value.channel = preset.channel
  form.value.category = preset.category
  form.value.subject = preset.subject
  form.value.content = preset.content
  form.value.templateName = preset.label
}

const form = ref({
  templateCode: '',
  templateName: '',
  channel: 'SYSTEM',
  category: 'SYSTEM',
  subject: '',
  content: '',
  description: '',
  enabled: true,
  systemBuiltin: false
})

const rules = {
  templateCode: [
    { required: true, message: '请输入模板编码', trigger: 'blur' },
    { pattern: /^[A-Z0-9_]+$/, message: '只能使用大写字母、数字和下划线', trigger: 'blur' }
  ],
  templateName: [
    { required: true, message: '请输入模板名称', trigger: 'blur' }
  ],
  channel: [
    { required: true, message: '请选择通知渠道', trigger: 'change' }
  ],
  category: [
    { required: true, message: '请选择模板分类', trigger: 'change' }
  ],
  content: [
    { required: true, message: '请输入内容模板', trigger: 'blur' }
  ]
}

/** 加载模板数据（编辑模式） */
const loadTemplate = async () => {
  if (!isEdit.value) return

  loading.value = true
  try {
    const data = await templateApi.getById(route.params.id)
    if (data) {
      form.value = {
        templateCode: data.templateCode,
        templateName: data.templateName,
        channel: data.channel,
        category: data.category,
        subject: data.subject || '',
        content: data.content,
        description: data.description || '',
        enabled: data.enabled,
        systemBuiltin: data.systemBuiltin
      }
    }
  } catch (error) {
    ElMessage.error('加载模板数据失败')
    router.push('/notification-templates')
  } finally {
    loading.value = false
  }
}

/** 提交表单 */
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true

    if (isEdit.value) {
      await templateApi.update(route.params.id, form.value)
      ElMessage.success('模板更新成功')
    } else {
      await templateApi.create(form.value)
      ElMessage.success('模板创建成功')
    }

    router.push('/notification-templates')
  } catch (error) {
    if (error !== false) {
      ElMessage.error('保存失败：' + (error.message || '未知错误'))
    }
  } finally {
    submitting.value = false
  }
}

/** 预览模板 */
const handlePreview = async () => {
  const sampleVariables = {
    title: '示例标题',
    content: '示例内容',
    time: new Date().toLocaleString('zh-CN'),
    priority: 'HIGH',
    priorityDesc: '高',
    ruleName: 'CPU使用率告警',
    metric: 'cpu_usage',
    triggerValue: '95%',
    thresholdValue: '80%',
    agentId: 'agent-001',
    agentName: '服务端开发Agent',
    taskTitle: '完成用户模块开发',
    taskResult: '成功',
    projectName: '示例项目',
    userName: '管理员',
    systemName: 'ChengXun Game Maker'
  }

  // 编辑模式调用后端API预览，创建模式本地预览（模板尚未保存）
  if (isEdit.value) {
    try {
      const result = await templateApi.preview(route.params.id, sampleVariables)
      previewResult.value = result
      showPreview.value = true
      return
    } catch (error) {
      // API失败时降级到本地预览
    }
  }

  // 本地预览：替换变量
  let content = form.value.content || ''
  let subject = form.value.subject || ''

  for (const [key, value] of Object.entries(sampleVariables)) {
    const escaped = key.replace(/\$/g, '\\$').replace(/\{/g, '\\{').replace(/\}/g, '\\}')
    content = content.replace(new RegExp(escaped, 'g'), value)
    subject = subject.replace(new RegExp(escaped, 'g'), value)
  }

  previewResult.value = { subject, content }
  showPreview.value = true
}

onMounted(() => {
  loadTemplate()
})
</script>

<style scoped>
.template-form-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.form-tip {
  color: #909399;
  font-size: 12px;
  margin-top: 5px;
}
.preset-section { margin-bottom: 8px; }
.preset-label { font-size: 13px; color: var(--el-text-color-secondary); margin-bottom: 8px; }
.preset-buttons { display: flex; flex-wrap: wrap; gap: 8px; }

.preview-container {
  padding: 20px;
}

.preview-item {
  margin-bottom: 20px;
}

.preview-item h4 {
  margin-bottom: 10px;
  color: #303133;
}

.preview-content {
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-all;
}

.variable-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding: 12px;
  background: var(--el-color-info-light-9);
  border-radius: 4px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

:deep(.el-descriptions__label) {
  font-family: monospace;
  font-size: 13px;
  color: var(--el-color-primary);
}

:deep(.el-tabs__content) {
  padding: 16px 0;
}
</style>
