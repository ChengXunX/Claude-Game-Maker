<template>
  <div class="burndown-chart">
    <div ref="chartRef" :style="{ width: '100%', height: height + 'px' }"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, onUnmounted } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  dates: { type: Array, default: () => [] },
  remaining: { type: Array, default: () => [] },
  ideal: { type: Array, default: () => [] },
  height: { type: Number, default: 300 }
})

const chartRef = ref(null)
let chart = null

const renderChart = () => {
  if (!chartRef.value) return

  if (chart) {
    chart.dispose()
  }

  chart = echarts.init(chartRef.value)

  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        let html = `<strong>${params[0].axisValue}</strong><br/>`
        params.forEach(p => {
          html += `${p.marker} ${p.seriesName}: ${p.value} 个任务<br/>`
        })
        return html
      }
    },
    legend: { data: ['实际剩余', '理想进度'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
    xAxis: {
      type: 'category',
      data: props.dates,
      axisLabel: { fontSize: 11, color: '#666' }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#666' },
      splitLine: { lineStyle: { type: 'dashed', color: '#eee' } }
    },
    series: [
      {
        name: '实际剩余',
        type: 'line',
        data: props.remaining,
        smooth: true,
        lineStyle: { width: 3, color: '#409eff' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64,158,255,0.3)' },
            { offset: 1, color: 'rgba(64,158,255,0.05)' }
          ])
        },
        symbol: 'circle',
        symbolSize: 6
      },
      {
        name: '理想进度',
        type: 'line',
        data: props.ideal,
        lineStyle: { width: 2, type: 'dashed', color: '#909399' },
        symbol: 'none',
        smooth: true
      }
    ]
  }

  chart.setOption(option)
}

const handleResize = () => {
  chart?.resize()
}

watch(() => [props.dates, props.remaining, props.ideal], renderChart, { deep: true })

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
.burndown-chart {
  width: 100%;
}
</style>
