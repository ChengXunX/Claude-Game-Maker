<template>
  <div class="roles-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>角色管理</span>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon> 创建角色
          </el-button>
        </div>
      </template>

      <el-table :data="roles" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="角色标识" width="150" />
        <el-table-column prop="displayName" label="显示名称" width="150" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="内置角色" width="80">
          <template #default="{ row }">
            <el-tag :type="row.system ? 'danger' : 'info'" size="small">
              {{ row.system ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="权限数" width="80">
          <template #default="{ row }">
            {{ row.permissions?.length || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="handleEdit(row)" :disabled="row.system">编辑</el-button>
            <el-button type="danger" size="small" text @click="handleDelete(row)" :disabled="row.system">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑角色对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑角色' : '创建角色'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="角色标识" prop="name">
          <el-input v-model="form.name" placeholder="如：DEVELOPER" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="显示名称" prop="displayName">
          <el-input v-model="form.displayName" placeholder="如：开发者" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="角色描述" />
        </el-form-item>
        <el-form-item label="权限">
          <div class="permission-tree-container">
            <el-tree
              ref="permTreeRef"
              :data="permissionTree"
              :props="{ children: 'children', label: 'label' }"
              show-checkbox
              node-key="id"
              :default-checked-keys="form.permissions"
              @check="handlePermCheck"
            >
              <template #default="{ node, data }">
                <div class="perm-node">
                  <span>{{ node.label }}</span>
                  <span v-if="data.description" class="perm-desc">{{ data.description }}</span>
                </div>
              </template>
            </el-tree>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
/**
 * 角色管理页面
 * 管理系统角色和权限
 *
 * 操作维度：系统级
 * 权限要求：roles:manage
 */
import { ref, onMounted, nextTick } from 'vue'
import { roleApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const roles = ref([])

/** 对话框 */
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const formRef = ref(null)
const permTreeRef = ref(null)

const form = ref({
  name: '',
  displayName: '',
  description: '',
  permissions: []
})

const rules = {
  name: [{ required: true, message: '请输入角色标识', trigger: 'blur' }],
  displayName: [{ required: true, message: '请输入显示名称', trigger: 'blur' }]
}

/** 权限树形结构 */
const permissionTree = [
  {
    id: 'agent_group',
    label: 'Agent 管理',
    children: [
      { id: 'agents:view', label: '查看 Agent', description: '查看 Agent 列表和状态' },
      { id: 'agents:manage', label: '管理 Agent', description: '启动、停止、重启 Agent' },
      { id: 'agents:task', label: '发送任务', description: '向 Agent 发送任务和指令' }
    ]
  },
  {
    id: 'project_group',
    label: '项目管理',
    children: [
      { id: 'projects:view', label: '查看项目', description: '查看项目列表和详情' },
      { id: 'projects:manage', label: '管理项目', description: '创建、编辑、删除项目' }
    ]
  },
  {
    id: 'skill_group',
    label: '技能管理',
    children: [
      { id: 'skills:view', label: '查看技能', description: '查看技能列表和详情' },
      { id: 'skills:manage', label: '管理技能', description: '创建、编辑、删除技能' }
    ]
  },
  {
    id: 'token_group',
    label: 'Token 管理',
    children: [
      { id: 'tokens:view', label: '查看 Token', description: '查看 Token 列表和用量' },
      { id: 'tokens:manage', label: '管理 Token', description: '创建、编辑、删除 Token' }
    ]
  },
  {
    id: 'ai_group',
    label: 'AI 助手',
    children: [
      { id: 'ai:use', label: '使用 AI', description: '使用 AI 助手功能' },
      { id: 'ai:admin', label: 'AI 管理', description: '管理 AI 配置和知识库' }
    ]
  },
  {
    id: 'system_group',
    label: '系统管理',
    children: [
      { id: 'system:monitor', label: '系统监控', description: '查看系统监控数据' },
      { id: 'system:manage', label: '系统管理', description: '管理系统配置和设置' },
      { id: 'admin:manage', label: '管理后台', description: '访问管理后台功能' },
      { id: 'roles:manage', label: '角色管理', description: '创建、编辑、删除角色' },
      { id: 'users:manage', label: '用户管理', description: '创建、编辑、删除用户' },
      { id: 'logs:view', label: '查看日志', description: '查看操作日志' }
    ]
  },
  {
    id: 'workflow_group',
    label: '工作流',
    children: [
      { id: 'workflow:view', label: '查看工作流', description: '查看工作流状态' },
      { id: 'pipeline:view', label: '查看流水线', description: '查看 CI/CD 流水线' },
      { id: 'approval:view', label: '查看审批', description: '查看审批记录' },
      { id: 'approval:manage', label: '管理审批', description: '处理审批请求' }
    ]
  },
  {
    id: 'other_group',
    label: '其他功能',
    children: [
      { id: 'notification:view', label: '查看通知', description: '查看系统通知' },
      { id: 'notification:manage', label: '管理通知', description: '管理系统通知' },
      { id: 'tokens:view', label: '查看 Token', description: '查看 Token 列表和用量' },
      { id: 'approval:view', label: '查看审批', description: '查看审批记录' },
      { id: 'system:view', label: '查看系统', description: '查看系统信息' },
      { id: 'code:review', label: '代码审查', description: '进行代码审查' }
    ]
  }
]

/** 加载角色列表 */
const loadRoles = async () => {
  loading.value = true
  try {
    const data = await roleApi.getAll()
    roles.value = data || []
  } catch (error) {
    ElMessage.error('加载角色列表失败')
  } finally {
    loading.value = false
  }
}

/** 处理权限选择变化 */
const handlePermCheck = () => {
  if (permTreeRef.value) {
    const checkedKeys = permTreeRef.value.getCheckedKeys(true)
    form.value.permissions = checkedKeys
  }
}

/** 创建角色 */
const handleCreate = () => {
  isEdit.value = false
  editId.value = null
  form.value = { name: '', displayName: '', description: '', permissions: [] }
  dialogVisible.value = true
  // 重置树选择
  nextTick(() => {
    if (permTreeRef.value) {
      permTreeRef.value.setCheckedKeys([])
    }
  })
}

/** 编辑角色 */
const handleEdit = (role) => {
  isEdit.value = true
  editId.value = role.id
  form.value = {
    name: role.name,
    displayName: role.displayName,
    description: role.description,
    permissions: [...(role.permissions || [])]
  }
  dialogVisible.value = true
  // 设置树选择状态
  nextTick(() => {
    if (permTreeRef.value) {
      permTreeRef.value.setCheckedKeys(role.permissions || [])
    }
  })
}

/** 保存角色 */
const handleSave = async () => {
  try {
    await formRef.value.validate()

    if (isEdit.value) {
      await roleApi.update(editId.value, form.value)
      ElMessage.success('角色更新成功')
    } else {
      await roleApi.create(form.value)
      ElMessage.success('角色创建成功')
    }

    dialogVisible.value = false
    loadRoles()
  } catch (error) {
    if (error !== false) {
      ElMessage.error('保存失败')
    }
  }
}

/** 删除角色 */
const handleDelete = async (role) => {
  try {
    await ElMessageBox.confirm(`确定要删除角色 "${role.displayName}" 吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await roleApi.delete(role.id)
    ElMessage.success('角色已删除')
    loadRoles()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadRoles()
})
</script>

<style scoped>
.roles-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.permission-tree-container {
  width: 100%;
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 10px;
}

.perm-node {
  display: flex;
  align-items: center;
  gap: 10px;
}

.perm-desc {
  color: #909399;
  font-size: 12px;
}
</style>
