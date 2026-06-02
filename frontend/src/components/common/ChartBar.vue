<script setup>
import { computed, ref, watch, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { TooltipComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([BarChart, TooltipComponent, GridComponent, CanvasRenderer])

const props = defineProps({
  data: { type: Array, default: () => [] },
  color: { type: String, default: '#b65f3b' },
  horizontal: { type: Boolean, default: true },
  height: { type: String, default: '320px' },
  barWidth: { type: Number, default: 18 },
  unit: { type: String, default: '' }
})

const chartRef = ref(null)
let chart = null
let resizeObserver = null

const hasData = computed(() => props.data.some((d) => Number(d.value || 0) > 0))

function buildOption() {
  const sorted = [...props.data].sort((a, b) => Number(a.value || 0) - Number(b.value || 0))
  const names = sorted.map((d) => d.name)
  const values = sorted.map((d) => Number(d.value || 0))

  const categoryAxis = {
    type: 'category',
    data: names,
    axisLabel: { color: '#73665b', fontSize: 12 },
    axisLine: { show: false },
    axisTick: { show: false }
  }
  const valueAxis = {
    type: 'value',
    axisLabel: { color: '#a09286', fontSize: 11 },
    splitLine: { lineStyle: { color: 'rgba(88,62,43,0.06)' } }
  }

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const p = params[0]
        return `${p.name}: ${p.value}${props.unit}`
      },
      backgroundColor: 'rgba(255,250,243,0.96)',
      borderColor: 'rgba(88,62,43,0.12)',
      textStyle: { color: '#241914', fontSize: 13 }
    },
    grid: {
      left: props.horizontal ? '28%' : '12%',
      right: '8%',
      top: '8%',
      bottom: '8%',
      containLabel: false
    },
    xAxis: props.horizontal ? valueAxis : categoryAxis,
    yAxis: props.horizontal ? categoryAxis : valueAxis,
    series: [
      {
        type: 'bar',
        data: values,
        barWidth: props.barWidth,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(props.horizontal ? 0 : 0, props.horizontal ? 0 : 1, 1, 0, [
            { offset: 0, color: props.color },
            { offset: 1, color: '#d57a4a' }
          ]),
          borderRadius: props.horizontal ? [0, 6, 6, 0] : [6, 6, 0, 0]
        },
        emphasis: {
          itemStyle: { shadowBlur: 8, shadowColor: 'rgba(88,62,43,0.16)' }
        }
      }
    ]
  }
}

function initChart() {
  if (!chartRef.value) return
  chart = echarts.init(chartRef.value)
  chart.setOption(buildOption())
}

function updateChart() {
  if (chart) {
    chart.setOption(buildOption(), true)
  }
}

watch(() => props.data, updateChart, { deep: true })

onMounted(() => {
  initChart()
  resizeObserver = new ResizeObserver(() => chart?.resize())
  if (chartRef.value) resizeObserver.observe(chartRef.value)
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  chart?.dispose()
})
</script>

<template>
  <div v-if="hasData" ref="chartRef" :style="{ width: '100%', height }" />
  <div v-else class="chart-empty">
    <span>暂无数据</span>
  </div>
</template>

<style scoped>
.chart-empty {
  min-height: 160px;
  display: grid;
  place-items: center;
  border: 1px dashed rgba(88, 62, 43, 0.18);
  border-radius: 16px;
  color: #a09286;
  background: rgba(255, 255, 255, 0.52);
  font-size: 14px;
}
</style>
