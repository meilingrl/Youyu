<script setup>
import { computed, ref, watch, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts/core'
import { PieChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([PieChart, TooltipComponent, LegendComponent, CanvasRenderer])

const props = defineProps({
  data: { type: Array, default: () => [] },
  colors: { type: Array, default: () => ['#b65f3b', '#c47a2c', '#d57a4a', '#69795f', '#b87972', '#e4a173', '#8a6953', '#a95031'] },
  innerRadius: { type: String, default: '42%' },
  outerRadius: { type: String, default: '72%' },
  height: { type: String, default: '320px' }
})

const chartRef = ref(null)
let chart = null
let resizeObserver = null

const hasData = computed(() => props.data.some((d) => Number(d.value || 0) > 0))

function buildOption() {
  return {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)',
      backgroundColor: 'rgba(255,250,243,0.96)',
      borderColor: 'rgba(88,62,43,0.12)',
      textStyle: { color: '#241914', fontSize: 13 }
    },
    legend: {
      orient: 'horizontal',
      bottom: 0,
      textStyle: { color: '#73665b', fontSize: 12 },
      itemWidth: 12,
      itemHeight: 12,
      itemGap: 14
    },
    color: props.colors,
    series: [
      {
        type: 'pie',
        radius: [props.innerRadius, props.outerRadius],
        center: ['50%', '45%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fffaf3', borderWidth: 2 },
        label: { show: false },
        emphasis: {
          label: { show: true, fontSize: 14, fontWeight: 'bold', color: '#241914' },
          itemStyle: { shadowBlur: 12, shadowColor: 'rgba(88,62,43,0.18)' }
        },
        data: props.data.map((d) => ({ name: d.name, value: Number(d.value || 0) }))
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
