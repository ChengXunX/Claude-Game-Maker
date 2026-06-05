/**
 * 批量操作工具模块
 * 提供列表页面的批量操作支持
 *
 * 功能：
 * - 批量选择（复选框）
 * - 批量删除
 * - 批量状态变更
 * - 批量导出
 *
 * @author chengxun
 * @since 1.0.0
 */

import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

/**
 * 创建批量操作控制器
 * @param {Object} options - 配置选项
 * @param {Function} options.onDelete - 批量删除回调
 * @param {Function} options.onStatusChange - 批量状态变更回调
 * @param {Function} options.onExport - 批量导出回调
 * @returns {Object} 批量操作控制器
 */
export function useBatchOperations(options = {}) {
  /** 选中的行 */
  const selectedRows = ref([])

  /** 是否有选中项 */
  const hasSelection = computed(() => selectedRows.value.length > 0)

  /** 选中数量 */
  const selectionCount = computed(() => selectedRows.value.length)

  /** 全选/取消全选 */
  const toggleAll = (rows) => {
    if (selectedRows.value.length === rows.length) {
      selectedRows.value = []
    } else {
      selectedRows.value = [...rows]
    }
  }

  /** 选择变更 */
  const handleSelectionChange = (selection) => {
    selectedRows.value = selection
  }

  /** 清除选择 */
  const clearSelection = () => {
    selectedRows.value = []
  }

  /** 批量删除 */
  const batchDelete = async (confirmText = '确定要删除选中的项目吗？') => {
    if (!hasSelection.value) {
      ElMessage.warning('请先选择要删除的项目')
      return
    }

    try {
      await ElMessageBox.confirm(
        `${confirmText}（共 ${selectionCount.value} 项）`,
        '批量删除',
        { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
      )

      if (options.onDelete) {
        await options.onDelete(selectedRows.value)
      }

      ElMessage.success(`已删除 ${selectionCount.value} 项`)
      clearSelection()
    } catch (error) {
      if (error !== 'cancel') {
        ElMessage.error('批量删除失败')
      }
    }
  }

  /** 批量状态变更 */
  const batchStatusChange = async (status, statusLabel = '状态') => {
    if (!hasSelection.value) {
      ElMessage.warning('请先选择要操作的项目')
      return
    }

    try {
      await ElMessageBox.confirm(
        `确定要将选中的 ${selectionCount.value} 项${statusLabel}为 "${status}" 吗？`,
        '批量操作',
        { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' }
      )

      if (options.onStatusChange) {
        await options.onStatusChange(selectedRows.value, status)
      }

      ElMessage.success(`已更新 ${selectionCount.value} 项`)
      clearSelection()
    } catch (error) {
      if (error !== 'cancel') {
        ElMessage.error('批量操作失败')
      }
    }
  }

  /** 批量导出 */
  const batchExport = async (format = 'csv') => {
    if (!hasSelection.value) {
      ElMessage.warning('请先选择要导出的项目')
      return
    }

    try {
      if (options.onExport) {
        await options.onExport(selectedRows.value, format)
      }

      ElMessage.success(`已导出 ${selectionCount.value} 项`)
    } catch (error) {
      ElMessage.error('导出失败')
    }
  }

  return {
    selectedRows,
    hasSelection,
    selectionCount,
    toggleAll,
    handleSelectionChange,
    clearSelection,
    batchDelete,
    batchStatusChange,
    batchExport
  }
}
