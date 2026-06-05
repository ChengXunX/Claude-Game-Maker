<template>
  <div class="recruitment-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>Agent 招聘</span>
          <el-button type="primary" @click="handleRecruit" v-permission="'agents:manage'">
            <el-icon><UserPlus /></el-icon> 发起招聘
          </el-button>
        </div>
      </template>

      <!-- 统计卡片 -->
      <el-row :gutter="16" class="stat-cards">
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value">{{ stats.totalAgents || 0 }}</div>
              <div class="stat-label">当前 Agent 数</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value">{{ stats.pendingRequests || 0 }}</div>
              <div class="stat-label">待审批申请</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value">{{ stats.approvedRequests || 0 }}</div>
              <div class="stat-label">已通过申请</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-value">{{ stats.rejectedRequests || 0 }}</div>
              <div class="stat-label">已拒绝申请</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 可招聘角色列表 -->
      <h3 class="section-title">可招聘角色</h3>
      <el-table :data="roles" v-loading="loading" stripe>
        <el-table-column prop="role" label="角色" width="150" />
        <el-table-column prop="name" label="名称" width="150" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.preset ? 'primary' : 'success'" size="small">
              {{ row.preset ? '预设' : '自定义' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="能力" min-width="200">
          <template #default="{ row }">
            <el-tag v-for="cap in (row.capabilities || []).slice(0, 3)" :key="cap" size="small" class="cap-tag">
              {{ cap }}
            </el-tag>
            <el-tag v-if="(row.capabilities || []).length > 3" size="small" type="info">
              +{{ row.capabilities.length - 3 }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleApply(row)" v-permission="'agents:manage'">
              招聘
            </el-button>
            <el-button v-if="!row.preset" type="warning" size="small" @click="handleEditRole(row)" v-permission="'agents:manage'">
              编辑
            </el-button>
            <el-button v-if="!row.preset" type="danger" size="small" @click="handleDeleteRole(row)" v-permission="'agents:manage'">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="add-role-section">
        <el-button type="success" @click="handleAddRole" v-permission="'agents:manage'">
          <el-icon><Plus /></el-icon> 添加自定义角色
        </el-button>
        <el-button type="primary" @click="handleDirectCustomRecruit" v-permission="'agents:manage'">
          <el-icon><UserFilled /></el-icon> 直接招聘自定义角色
        </el-button>
      </div>
    </el-card>

    <!-- 招聘申请对话框 -->
    <el-dialog v-model="applyDialogVisible" title="发起招聘申请" width="500px">
      <el-form ref="applyFormRef" :model="applyForm" :rules="applyRules" label-width="100px">
        <el-form-item label="角色">
          <el-input :value="applyForm.roleName" disabled />
        </el-form-item>
        <el-form-item label="Agent 名称" prop="agentName">
          <el-input v-model="applyForm.agentName" placeholder="为新 Agent 取个名字" />
        </el-form-item>
        <el-form-item label="工作目录" prop="workDir">
          <el-input v-model="applyForm.workDir" placeholder="Agent 的工作目录路径" />
        </el-form-item>
        <el-form-item label="备注" prop="reason">
          <el-input v-model="applyForm.reason" type="textarea" :rows="3" placeholder="招聘原因（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleApplySubmit" :loading="submitting">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 自定义角色对话框 -->
    <el-dialog v-model="roleDialogVisible" :title="isEditRole ? '编辑角色' : '添加自定义角色'" width="600px">
      <el-form ref="roleFormRef" :model="roleForm" :rules="roleRules" label-width="100px">
        <el-form-item label="角色标识" prop="role">
          <el-input v-model="roleForm.role" placeholder="如：audio-dev" :disabled="isEditRole" />
          <div class="form-tip">唯一标识，只能使用小写字母、数字和连字符</div>
        </el-form-item>
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="roleForm.name" placeholder="如：音频开发" />
        </el-form-item>
        <el-form-item label="角色描述" prop="description">
          <el-input v-model="roleForm.description" type="textarea" :rows="3" placeholder="角色职责描述" />
        </el-form-item>
        <el-form-item label="默认能力">
          <el-select v-model="roleForm.capabilities" multiple filterable allow-create placeholder="输入能力名称后回车">
            <el-option v-for="cap in commonCapabilities" :key="cap" :label="cap" :value="cap" />
          </el-select>
        </el-form-item>
        <el-form-item label="文件类型">
          <el-select v-model="roleForm.fileTypes" multiple filterable allow-create placeholder="输入文件类型后回车">
            <el-option v-for="ft in commonFileTypes" :key="ft" :label="ft" :value="ft" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRoleSubmit" :loading="submitting">
          {{ isEditRole ? '保存修改' : '创建角色' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 直接招聘自定义角色对话框 -->
    <el-dialog v-model="customRecruitDialogVisible" title="直接招聘自定义角色" width="650px">
      <el-form ref="customRecruitFormRef" :model="customRecruitForm" :rules="customRecruitRules" label-width="100px">
        <el-alert type="info" :closable="false" style="margin-bottom: 16px;">
          制作人可以根据项目需求直接招聘任意角色，系统会自动创建角色模板。
        </el-alert>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="角色标识" prop="role">
              <el-input v-model="customRecruitForm.role" placeholder="如：audio-dev" />
              <div class="form-tip">唯一标识，只能使用小写字母、数字和连字符</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色名称" prop="roleName">
              <el-input v-model="customRecruitForm.roleName" placeholder="如：音频开发" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="Agent 名称" prop="name">
          <el-input v-model="customRecruitForm.name" placeholder="为新 Agent 取个名字" />
        </el-form-item>
        <el-form-item label="角色描述" prop="description">
          <el-input v-model="customRecruitForm.description" type="textarea" :rows="2" placeholder="角色职责描述" />
        </el-form-item>
        <el-form-item label="工作目录" prop="workDir">
          <el-input v-model="customRecruitForm.workDir" placeholder="Agent 的工作目录路径" />
        </el-form-item>
        <el-form-item label="能力标签">
          <el-select v-model="customRecruitForm.capabilities" multiple filterable allow-create placeholder="输入能力名称后回车">
            <el-option v-for="cap in commonCapabilities" :key="cap" :label="cap" :value="cap" />
          </el-select>
        </el-form-item>
        <el-form-item label="文件类型">
          <el-select v-model="customRecruitForm.fileTypes" multiple filterable allow-create placeholder="输入文件类型后回车">
            <el-option v-for="ft in commonFileTypes" :key="ft" :label="ft" :value="ft" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="customRecruitDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCustomRecruitSubmit" :loading="submitting">
          立即招聘
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * Agent 招聘页面
 * 制作人 Agent 招聘新 Agent（员工）
 *
 * 操作维度：系统级
 * 权限要求：agents:manage
 */
import { ref, onMounted } from 'vue'
import { recruitmentApi, agentApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as Icons from '@element-plus/icons-vue'

const { UserPlus, Plus, UserFilled } = Icons

const loading = ref(false)
const roles = ref([])
const stats = ref({})

/** 申请对话框 */
const applyDialogVisible = ref(false)
const applyFormRef = ref(null)
const submitting = ref(false)
const applyForm = ref({
  role: '',
  roleName: '',
  agentName: '',
  workDir: '',
  reason: ''
})
const applyRules = {
  agentName: [{ required: true, message: '请输入 Agent 名称', trigger: 'blur' }],
  workDir: [{ required: true, message: '请输入工作目录', trigger: 'blur' }]
}

/** 自定义角色对话框 */
const roleDialogVisible = ref(false)
const roleFormRef = ref(null)
const isEditRole = ref(false)
const roleForm = ref({
  role: '',
  name: '',
  description: '',
  capabilities: [],
  fileTypes: []
})
const roleRules = {
  role: [
    { required: true, message: '请输入角色标识', trigger: 'blur' },
    { pattern: /^[a-z0-9-]+$/, message: '只能使用小写字母、数字和连字符', trigger: 'blur' }
  ],
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

/** 直接招聘自定义角色对话框 */
const customRecruitDialogVisible = ref(false)
const customRecruitFormRef = ref(null)
const customRecruitForm = ref({
  role: '',
  roleName: '',
  name: '',
  description: '',
  workDir: '',
  capabilities: [],
  fileTypes: []
})
const customRecruitRules = {
  role: [
    { required: true, message: '请输入角色标识', trigger: 'blur' },
    { pattern: /^[a-z0-9-]+$/, message: '只能使用小写字母、数字和连字符', trigger: 'blur' }
  ],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  name: [{ required: true, message: '请输入 Agent 名称', trigger: 'blur' }],
  workDir: [{ required: true, message: '请输入工作目录', trigger: 'blur' }]
}

/** 常用能力列表 */
const commonCapabilities = [
  'backend_development', 'frontend_development', 'api_design', 'database_design',
  'game_design', 'system_design', 'documentation', 'testing', 'bug_reporting',
  'ui_design', 'css_styling', 'responsive_design', 'performance_optimization',
  'version_control', 'git_operations', 'numerical_design', 'balance_tuning',
  'audio_design', '3d_modeling', 'animation', 'shader_programming'
]

/** 常用文件类型 */
const commonFileTypes = [
  'java', 'py', 'go', 'js', 'ts', 'html', 'css', 'lua', 'sql',
  'json', 'yaml', 'xml', 'md', 'txt', 'xlsx', 'csv',
  'png', 'jpg', 'svg', 'mp3', 'wav', 'fbx', 'obj'
]

/** 加载可招聘角色 */
const loadRoles = async () => {
  loading.value = true
  try {
    const data = await recruitmentApi.getRoles()
    roles.value = data || []
  } catch (error) {
    ElMessage.error('加载角色列表失败')
  } finally {
    loading.value = false
  }
}

/** 加载统计 */
const loadStats = async () => {
  try {
    const data = await recruitmentApi.getStats()
    stats.value = data || {}
  } catch (error) {
    // 忽略
  }
}

/** 点击招聘按钮 */
const handleRecruit = () => {
  // 默认选择第一个角色
  if (roles.value.length > 0) {
    handleApply(roles.value[0])
  }
}

/** 选择角色招聘 */
const handleApply = (role) => {
  applyForm.value = {
    role: role.role,
    roleName: role.name,
    agentName: '',
    workDir: '',
    reason: ''
  }
  applyDialogVisible.value = true
}

/** 提交申请 */
const handleApplySubmit = async () => {
  try {
    await applyFormRef.value.validate()
    submitting.value = true

    await recruitmentApi.createRequest({
      role: applyForm.value.role,
      agentName: applyForm.value.agentName,
      workDir: applyForm.value.workDir,
      reason: applyForm.value.reason
    })

    ElMessage.success('招聘申请已提交')
    applyDialogVisible.value = false
    loadStats()
  } catch (error) {
    if (error !== false) {
      ElMessage.error('提交失败')
    }
  } finally {
    submitting.value = false
  }
}

/** 添加自定义角色 */
const handleAddRole = () => {
  isEditRole.value = false
  roleForm.value = {
    role: '',
    name: '',
    description: '',
    capabilities: [],
    fileTypes: []
  }
  roleDialogVisible.value = true
}

/** 编辑自定义角色 */
const handleEditRole = (role) => {
  isEditRole.value = true
  roleForm.value = {
    role: role.role,
    name: role.name,
    description: role.description,
    capabilities: [...(role.capabilities || [])],
    fileTypes: [...(role.fileTypes || [])]
  }
  roleDialogVisible.value = true
}

/** 删除自定义角色 */
const handleDeleteRole = async (role) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除角色 "${role.name}" 吗？`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )

    await recruitmentApi.deleteCustomTemplate(role.role)
    ElMessage.success('角色已删除')
    loadRoles()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

/** 提交角色表单 */
const handleRoleSubmit = async () => {
  try {
    await roleFormRef.value.validate()
    submitting.value = true

    if (isEditRole.value) {
      await recruitmentApi.updateCustomTemplate(roleForm.value.role, roleForm.value)
      ElMessage.success('角色更新成功')
    } else {
      await recruitmentApi.createCustomTemplate(roleForm.value)
      ElMessage.success('角色创建成功')
    }

    roleDialogVisible.value = false
    loadRoles()
  } catch (error) {
    if (error !== false) {
      ElMessage.error('保存失败：' + (error.message || '未知错误'))
    }
  } finally {
    submitting.value = false
  }
}

/** 打开直接招聘自定义角色对话框 */
const handleDirectCustomRecruit = () => {
  customRecruitForm.value = {
    role: '',
    roleName: '',
    name: '',
    description: '',
    workDir: '',
    capabilities: [],
    fileTypes: []
  }
  customRecruitDialogVisible.value = true
}

/** 提交直接招聘自定义角色 */
const handleCustomRecruitSubmit = async () => {
  try {
    await customRecruitFormRef.value.validate()
    submitting.value = true

    // 获取第一个制作人Agent的ID作为producerId
    // 实际应该从当前项目中获取
    const producerId = 'producer' // TODO: 从实际项目中获取

    const result = await recruitmentApi.recruitCustom(producerId, customRecruitForm.value)

    ElMessage.success(`自定义角色 "${customRecruitForm.value.roleName}" 招聘成功！Agent: ${result.agentName}`)
    customRecruitDialogVisible.value = false
    loadRoles()
    loadStats()
  } catch (error) {
    if (error !== false) {
      ElMessage.error('招聘失败：' + (error.message || '未知错误'))
    }
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadRoles()
  loadStats()
})
</script>

<style scoped>
.recruitment-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-cards {
  margin-bottom: 24px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}

.stat-label {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.section-title {
  margin: 16px 0 12px;
  font-size: 16px;
}

.cap-tag {
  margin-right: 4px;
  margin-bottom: 4px;
}

.add-role-section {
  margin-top: 20px;
  text-align: center;
}

.form-tip {
  color: #909399;
  font-size: 12px;
  margin-top: 5px;
}
</style>
