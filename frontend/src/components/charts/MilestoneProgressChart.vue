<template>
  <div class="milestone-chart">
    <div ref="chartRef" :style="{ width: '100%', height: height + 'px' }"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, onUnmounted } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  milestones: { type: Array, default: () => [] },
  height: { type: Number, default: 300 }
})

const chartRef = ref(null)
let chart = null

const renderChart = () => {
  if (!chartRef.value || !props.milestones.length) return

  if (chart) {
    chart.dispose()
  }

  chart = echarts.init(chartRef.value)

  const names = props.milestones.map((m, i) => m.title || `里程碑 ${i + 1}`)
  const progress = props.milestones.map(m => m.progress || 0)
  const colors = props.milestones.map(m => {
    if (m.status === 'COMPLETED') return '#67c23a'
    if (m.status === 'IN_PROGRESS' || m.status === 'EXECUTING') return '#409eff'
    if (m.status === 'BLOCKED') return '#f56c6c'
    return '#909399'
  })

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const idx = params[0].dataIndex
        const m = props.milestones[idx]
        return `<strong>${m.title || '里程碑 ' + (idx + 1)}</strong><br/>
                状态: ${m.status || '-'}<br/>
                进度: ${m.progress || 0}%<br/>
                ${m.description ? '描述: ' + m.description : ''}`
      }
    },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: names,
      axisLabel: {
        rotate: names.length > 5 ? 30 : 0,
        fontSize: 11,
        color: '#666'
      }
    },
    yAxis: {
      type: 'value',
      max: 100,
      axisLabel: { formatter: '{value}%', color: '#666' },
      splitLine: { lineStyle: { type: 'dashed', color: '#eee' } }
    },
    series: [{
      type: 'bar',
      data: progress.map((v, i) => ({
        value: v,
        itemStyle: { color: colors[i], borderRadius: [4, 4, 0, 0] }
      })),
      barWidth: '40%',
      label: {
        show: true,
        position: 'top',
        formatter: '{c}%',
        fontSize: 11,
        color: '#333'
      }
    }]
  }

  chart.setOption(option)
}

const handleResize = () => {
  chart?.resize()
}

watch(() => props.milestones, renderChart, { deep: true })

onMounted(() => {
  renderChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
})
</script>

<style scoped>
.milestone-chart {
  width: 100%;
}
</style>
