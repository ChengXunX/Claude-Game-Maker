<template>
  <div class="configs-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>配置中心</span>
          <div>
            <el-button type="primary" size="small" @click="handleCreate">
              <el-icon><Plus /></el-icon> 新增配置
            </el-button>
            <el-button size="small" @click="handleRefreshCache">
              <el-icon><Refresh /></el-icon> 刷新缓存
            </el-button>
          </div>
        </div>
      </template>

      <!-- 筛选区 -->
      <div class="filter-bar">
        <el-select v-model="selectedGroup" placeholder="选择分组" clearable style="width: 150px" @change="loadConfigs">
          <el-option v-for="group in groups" :key="group" :label="group" :value="group" />
        </el-select>
        <el-input
          v-model="searchKeyword"
          placeholder="搜索配置"
          clearable
          style="width: 200px"
          @input="handleSearch"
        />
      </div>

      <!-- 配置表格 -->
      <el-table :data="filteredConfigs" v-loading="loading" stripe>
        <el-table-column prop="configKey" label="配置键" min-width="200" show-overflow-tooltip />
        <el-table-column prop="configValue" label="配置值" min-width="200">
          <template #default="{ row }">
            <template v-if="!row._editing">
              <el-icon v-if="isMasked(row.configValue)" style="color: #e6a23c; margin-right: 4px; vertical-align: middle;"><Lock /></el-icon>
              <span>{{ row.configValue }}</span>
            </template>
            <el-input v-else v-model="row._newValue" size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
        <el-table-column prop="group" label="分组" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.group }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="valueType" label="类型" width="80" />
        <el-table-column label="系统内置" width="80">
          <template #default="{ row }">
            <el-tag :type="row.systemBuiltin ? 'danger' : 'info'" size="small">
              {{ row.systemBuiltin ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <template v-if="!row._editing">
              <el-button type="primary" size="small" text @click="handleEdit(row)">编辑</el-button>
              <el-button type="danger" size="small" text @click="handleDelete(row)" :disabled="row.systemBuiltin">删除</el-button>
            </template>
            <template v-else>
              <el-button type="success" size="small" text @click="handleSave(row)">保存</el-button>
              <el-button size="small" text @click="handleCancelEdit(row)">取消</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增配置对话框 -->
    <el-dialog v-model="createDialogVisible" title="新增配置" width="500px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="配置键" prop="configKey">
          <el-input v-model="createForm.configKey" placeholder="如: system.pagination.default-size" />
        </el-form-item>
        <el-form-item label="配置值" prop="configValue">
          <el-input v-model="createForm.configValue" placeholder="请输入配置值" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="createForm.description" placeholder="请输入配置描述" />
        </el-form-item>
        <el-form-item label="分组" prop="group">
          <el-select v-model="createForm.group" placeholder="选择分组">
            <el-option v-for="group in groups" :key="group" :label="group" :value="group" />
            <el-option label="自定义" value="custom" />
          </el-select>
        </el-form-item>
        <el-form-item label="值类型" prop="valueType">
          <el-select v-model="createForm.valueType">
            <el-option label="字符串" value="string" />
            <el-option label="数字" value="number" />
            <el-option label="布尔" value="boolean" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateSubmit" :loading="creating">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 配置中心页面
 * 管理系统配置参数，支持热更新
 *
 * 操作维度：系统级
 * 权限要求：系统管理员
 */
import { ref, computed, onMounted } from 'vue'
import { configApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Lock } from '@element-plus/icons-vue'

const loading = ref(false)
const configs = ref([])
const groups = ref([])
const selectedGroup = ref('')
const searchKeyword = ref('')

/** 新增对话框 */
const createDialogVisible = ref(false)
const createFormRef = ref(null)
const creating = ref(false)
const createForm = ref({
  configKey: '',
  configValue: '',
  description: '',
  group: '',
  valueType: 'string'
})
const createRules = {
  configKey: [{ required: true, message: '请输入配置键', trigger: 'blur' }],
  configValue: [{ required: true, message: '请输入配置值', trigger: 'blur' }],
  group: [{ required: true, message: '请选择分组', trigger: 'change' }],
  valueType: [{ required: true, message: '请选择值类型', trigger: 'change' }]
}

/** 筛选后的配置 */
const filteredConfigs = computed(() => {
  if (!searchKeyword.value) return configs.value
  const keyword = searchKeyword.value.toLowerCase()
  return configs.value.filter(c =>
    c.configKey.toLowerCase().includes(keyword) ||
    (c.description && c.description.toLowerCase().includes(keyword))
  )
})

/** 加载配置列表 */
const loadConfigs = async () => {
  loading.value = true
  try {
    const params = selectedGroup.value ? { group: selectedGroup.value } : {}
    const data = await configApi.getAll(params)
    configs.value = (data || []).map(c => ({ ...c, _editing: false, _newValue: '' }))
  } catch (error) {
    ElMessage.error('加载配置失败')
  } finally {
    loading.value = false
  }
}

/** 加载分组列表 */
const loadGroups = async () => {
  try {
    const data = await configApi.getGroups()
    groups.value = data || []
  } catch (error) {
    // 忽略
  }
}

/** 搜索 */
const handleSearch = () => {
  // 使用 computed 属性自动筛选
}

/** 刷新缓存 */
const handleRefreshCache = async () => {
  try {
    await configApi.refreshCache()
    ElMessage.success('缓存已刷新')
  } catch (error) {
    ElMessage.error('刷新缓存失败')
  }
}

/** 判断是否为敏感配置（含 * 号表示已脱敏） */
const isMasked = (value) => value && value.includes('*')

/** 编辑配置 */
const handleEdit = async (row) => {
  row._editing = true
  // 如果值已脱敏，先获取原始值
  if (isMasked(row.configValue)) {
    try {
      const data = await configApi.reveal(row.id)
      row._newValue = data.configValue
    } catch {
      row._newValue = row.configValue
    }
  } else {
    row._newValue = row.configValue
  }
}

/** 取消编辑 */
const handleCancelEdit = (row) => {
  row._editing = false
  row._newValue = ''
}

/** 保存编辑 */
const handleSave = async (row) => {
  try {
    await configApi.update(row.id, { configValue: row._newValue })
    row.configValue = row._newValue
    row._editing = false
    ElMessage.success('配置已更新')
  } catch (error) {
    ElMessage.error('更新失败')
  }
}

/** 删除配置 */
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除配置 "${row.configKey}" 吗？`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )

    await configApi.delete(row.id)
    ElMessage.success('配置已删除')
    loadConfigs()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

/** 打开新增对话框 */
const handleCreate = () => {
  createForm.value = {
    configKey: '',
    configValue: '',
    description: '',
    group: groups.value[0] || '',
    valueType: 'string'
  }
  createDialogVisible.value = true
}

/** 提交新增 */
const handleCreateSubmit = async () => {
  try {
    await createFormRef.value.validate()
    creating.value = true

    await configApi.create(createForm.value)
    ElMessage.success('配置已创建')
    createDialogVisible.value = false
    loadConfigs()
  } catch (error) {
    if (error !== false) {
      ElMessage.error('创建失败')
    }
  } finally {
    creating.value = false
  }
}

onMounted(() => {
  loadConfigs()
  loadGroups()
})
</script>

<style scoped>
.configs-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
</style>
