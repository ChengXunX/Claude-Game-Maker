<template>
  <div class="token-form-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ isEdit ? '编辑 Token' : '创建 Token' }}</span>
          <el-button @click="router.back()">返回</el-button>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" style="max-width: 600px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="如：小米 mimo Token" />
        </el-form-item>
        <el-form-item label="API Key" prop="apiKey">
          <el-input v-model="form.apiKey" type="password" show-password placeholder="请输入 API Key" />
        </el-form-item>
        <el-form-item label="API URL" prop="apiUrl">
          <el-input v-model="form.apiUrl" placeholder="如：https://api.anthropic.com" />
        </el-form-item>
        <el-form-item label="模型" prop="model">
          <el-input v-model="form.model" placeholder="如：mimo-v2.5-pro" />
        </el-form-item>
        <el-form-item label="最大 Token" prop="maxTokens">
          <el-input-number v-model="form.maxTokens" :min="100" :max="100000" />
        </el-form-item>

        <!-- 新增：适用 Agent 角色 -->
        <el-form-item label="适用角色">
          <el-select
            v-model="selectedTags"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="选择或输入适用的 Agent 角色"
            style="width: 100%"
          >
            <el-option label="服务端开发 (server-dev)" value="server-dev" />
            <el-option label="客户端开发 (client-dev)" value="client-dev" />
            <el-option label="UI 开发 (ui-dev)" value="ui-dev" />
            <el-option label="系统策划 (system-planner)" value="system-planner" />
            <el-option label="数值策划 (numerical-planner)" value="numerical-planner" />
            <el-option label="Git 专员 (git-commit)" value="git-commit" />
            <el-option label="制作人 (producer)" value="producer" />
          </el-select>
          <div class="form-tip">留空表示适用于所有角色，制作人会根据角色自动分配</div>
        </el-form-item>

        <!-- 新增：优先级 -->
        <el-form-item label="优先级">
          <el-input-number v-model="form.priority" :min="1" :max="100" />
          <div class="form-tip">数值越小优先级越高，自动分配时优先选择</div>
        </el-form-item>

        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="Token 描述" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            {{ isEdit ? '保存' : '创建' }}
          </el-button>
          <el-button @click="router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
/**
 * Token 表单页面
 * 创建或编辑 API Token
 *
 * 操作维度：系统级
 * 权限要求：系统管理员
 */
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { tokenApi } from '@/api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const formRef = ref(null)
const submitting = ref(false)

/** 是否编辑模式 */
const isEdit = computed(() => !!route.query.id)

/** 选中的角色标签 */
const selectedTags = ref([])

/** 表单数据 */
const form = ref({
  name: '',
  apiKey: '',
  apiUrl: '',
  model: '',
  maxTokens: 4096,
  agentTags: '',
  priority: 10,
  description: ''
})

/** 表单验证规则 */
const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  apiKey: [{ required: true, message: '请输入 API Key', trigger: 'blur' }],
  apiUrl: [{ required: true, message: '请输入 API URL', trigger: 'blur' }]
}

/** 加载 Token 数据（编辑模式） */
const loadToken = async () => {
  if (!route.query.id) return

  try {
    const data = await tokenApi.getById(route.query.id)
    if (data) {
      form.value = {
        name: data.name || '',
        apiKey: data.apiKey || '',
        apiUrl: data.apiUrl || '',
        model: data.model || '',
        maxTokens: data.maxTokens || 4096,
        agentTags: data.agentTags || '',
        priority: data.priority || 10,
        description: data.description || ''
      }
      // 解析标签
      if (data.agentTags) {
        selectedTags.value = data.agentTags.split(',').map(t => t.trim()).filter(t => t)
      }
    }
  } catch (error) {
    ElMessage.error('加载 Token 失败')
  }
}

/** 提交表单 */
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true

    // 合并标签
    const submitData = {
      ...form.value,
      agentTags: selectedTags.value.join(',')
    }

    if (isEdit.value) {
      await tokenApi.update(route.query.id, submitData)
      ElMessage.success('Token 更新成功')
    } else {
      await tokenApi.create(submitData)
      ElMessage.success('Token 创建成功')
    }

    router.push('/tokens')
  } catch (error) {
    if (error !== false) {
      ElMessage.error('操作失败')
    }
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadToken()
})
</script>

<style scoped>
.token-form-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
