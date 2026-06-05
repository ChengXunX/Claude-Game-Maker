<template>
  <div class="pending-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>待审批用户</span>
          <el-badge :value="pendingUsers.length" :hidden="pendingUsers.length === 0">
            <el-tag type="warning">待审批</el-tag>
          </el-badge>
        </div>
      </template>

      <el-table :data="pendingUsers" v-loading="loading" stripe>
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="nickname" label="昵称" width="150" />
        <el-table-column prop="email" label="邮箱" width="200" />
        <el-table-column label="注册时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="success" size="small" @click="handleApprove(row)">
              通过
            </el-button>
            <el-button type="danger" size="small" @click="handleReject(row)">
              拒绝
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && pendingUsers.length === 0" description="暂无待审批用户" />
    </el-card>
  </div>
</template>

<script setup>
/**
 * 待审批用户页面
 * 管理员审批新注册的用户
 *
 * 操作维度：系统级
 * 权限要求：users:manage
 */
import { ref, onMounted } from 'vue'
import { userApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const pendingUsers = ref([])

/** 格式化时间 */
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

/** 加载待审批用户 */
const loadPendingUsers = async () => {
  loading.value = true
  try {
    const data = await userApi.getPending()
    pendingUsers.value = data || []
  } catch (error) {
    ElMessage.error('加载待审批用户失败')
  } finally {
    loading.value = false
  }
}

/** 审批通过 */
const handleApprove = async (user) => {
  try {
    await ElMessageBox.confirm(
      `确定通过用户 "${user.username}" 的注册申请吗？`,
      '审批确认',
      { confirmButtonText: '通过', cancelButtonText: '取消', type: 'info' }
    )

    await userApi.approve(user.id)
    ElMessage.success(`用户 "${user.username}" 已通过审批`)
    loadPendingUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('审批失败')
    }
  }
}

/** 审批拒绝 */
const handleReject = async (user) => {
  try {
    const { value: reason } = await ElMessageBox.prompt(
      `请输入拒绝用户 "${user.username}" 的原因：`,
      '审批拒绝',
      {
        confirmButtonText: '拒绝',
        cancelButtonText: '取消',
        type: 'warning',
        inputPlaceholder: '拒绝原因（可选）'
      }
    )

    await userApi.reject(user.id, { reason })
    ElMessage.success(`用户 "${user.username}" 已被拒绝`)
    loadPendingUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

onMounted(() => {
  loadPendingUsers()
})
</script>

<style scoped>
.pending-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
