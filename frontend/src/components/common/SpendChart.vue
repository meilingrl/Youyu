<script setup>
import { computed, ref, shallowRef, watch } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { BarChart, PieChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([CanvasRenderer, BarChart, PieChart, GridComponent, TooltipComponent, LegendComponent])

const props = defineProps({
  mode: {
    type: String,
    default: 'monthly',
    validator: (v) => ['monthly', 'yearly', 'category'].includes(v)
  },
  monthlyData: {
    type: Array,
    default: () => []
  },
  yearlyData: {
    type: Object,
    default: () => ({})
  },
  categoryData: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const WARM_COLORS = [
  '#c26b44', '#d78b55', '#c47a2c', '#e4a55a',
  '#b65f3b', '#d57a4a', '#8a6953', '#b87972'
]

const monthlyOption = computed(() => {
  const data = props.monthlyData
  if (!data.length) return null

  return {
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const p = params[0]
        return `${p.name}<br/>支出: ¥${Number(p.value).toFixed(2)}`
      }
    },
    grid: { left: 56, right: 16, top: 16, bottom: 32 },
    xAxis: {
      type: 'category',
      data: data.map((d) => d.label),
      axisLabel: { fontSize: 11, color: '#73665b' },
      axisLine: { lineStyle: { color: 'rgba(88,62,43,0.15)' } },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        fontSize: 11,
        color: '#a09286',
        formatter: (v) => (v >= 1000 ? `${(v / 1000).toFixed(0)}k` : v)
      },
      splitLine: { lineStyle: { color: 'rgba(88,62,43,0.08)' } }
    },
    series: [{
      type: 'bar',
      data: data.map((d) => d.amount),
      barMaxWidth: 32,
      itemStyle: {
        borderRadius: [6, 6, 0, 0],
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: '#c26b44' },
            { offset: 1, color: '#d78b55' }
          ]
        }
      },
      emphasis: {
        itemStyle: { color: '#b65f3b' }
      }
    }]
  }
})

const categoryOption = computed(() => {
  const data = props.categoryData
  if (!data.length) return null

  return {
    tooltip: {
      trigger: 'item',
      formatter: (p) => `${p.name}: ¥${Number(p.value).toFixed(2)} (${p.percent}%)`
    },
    legend: {
      orient: 'vertical',
      right: 8,
      top: 'center',
      textStyle: { fontSize: 12, color: '#73665b' },
      itemWidth: 12,
      itemHeight: 12,
      itemGap: 10
    },
    series: [{
      type: 'pie',
      radius: ['42%', '70%'],
      center: ['35%', '50%'],
      avoidLabelOverlap: true,
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 13, fontWeight: 'bold' }
      },
      data: data.map((d, i) => ({
        name: d.label,
        value: d.amount,
        itemStyle: { color: WARM_COLORS[i % WARM_COLORS.length] }
      }))
    }]
  }
})

const hasData = computed(() => {
  if (props.mode === 'monthly') return props.monthlyData.length > 0
  if (props.mode === 'yearly') return props.yearlyData.totalSpend > 0
  if (props.mode === 'category') return props.categoryData.length > 0
  return false
})
</script>

<template>
  <div class="spend-chart" :class="`spend-chart--${mode}`">
    <div v-if="loading" class="spend-chart__loading">
      <span class="spend-chart__spinner" />
      <span>加载中…</span>
    </div>

    <template v-else-if="hasData">
      <v-chart
        v-if="mode === 'monthly' && monthlyOption"
        class="spend-chart__canvas"
        :option="monthlyOption"
        autoresize
      />

      <div v-else-if="mode === 'yearly'" class="spend-chart__yearly">
        <div class="yearly-stat yearly-stat--primary">
          <strong>¥{{ Number(yearlyData.totalSpend || 0).toFixed(2) }}</strong>
          <span>年度总支出</span>
        </div>
        <div class="yearly-stat">
          <strong>¥{{ Number(yearlyData.avgMonthly || 0).toFixed(2) }}</strong>
          <span>月均支出</span>
        </div>
        <div class="yearly-stat">
          <strong>{{ yearlyData.orderCount || 0 }} 笔</strong>
          <span>订单总数</span>
        </div>
        <div class="yearly-stat">
          <strong>{{ yearlyData.itemCount || 0 }} 件</strong>
          <span>购买商品</span>
        </div>
        <div v-if="yearlyData.peakMonth" class="yearly-stat yearly-stat--accent">
          <strong>{{ yearlyData.peakMonth }}</strong>
          <span>消费高峰月</span>
        </div>
        <div v-if="yearlyData.avgPerOrder > 0" class="yearly-stat">
          <strong>¥{{ Number(yearlyData.avgPerOrder || 0).toFixed(2) }}</strong>
          <span>笔均消费</span>
        </div>
      </div>

      <v-chart
        v-else-if="mode === 'category' && categoryOption"
        class="spend-chart__canvas"
        :option="categoryOption"
        autoresize
      />
    </template>

    <div v-else class="spend-chart__empty">
      <span>暂无支出数据</span>
    </div>
  </div>
</template>

<style scoped>
.spend-chart {
  min-height: 200px;
  display: flex;
  flex-direction: column;
}

.spend-chart__canvas {
  width: 100%;
  height: 260px;
}

.spend-chart__loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 200px;
  color: var(--cm-text-secondary);
  font-size: 14px;
}

.spend-chart__spinner {
  width: 18px;
  height: 18px;
  border: 2px solid var(--cm-border-strong);
  border-top-color: var(--cm-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.spend-chart__empty {
  min-height: 200px;
  display: grid;
  place-items: center;
  border: 1px dashed var(--cm-border-strong);
  border-radius: var(--cm-radius-md);
  background: rgba(255, 255, 255, 0.52);
  color: var(--cm-text-secondary);
  font-size: 14px;
}

.spend-chart__yearly {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.yearly-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 20px 12px;
  border-radius: var(--cm-radius-sm);
  background: var(--cm-surface-muted);
  text-align: center;
}

.yearly-stat strong {
  font-size: 20px;
  color: var(--cm-text);
}

.yearly-stat span {
  font-size: 12px;
  color: var(--cm-text-secondary);
}

.yearly-stat--primary {
  background: var(--cm-primary-soft);
}

.yearly-stat--primary strong {
  color: var(--cm-primary);
  font-size: 24px;
}

.yearly-stat--accent {
  background: var(--cm-accent-soft);
}

.yearly-stat--accent strong {
  color: var(--cm-accent);
}

@media (max-width: 640px) {
  .spend-chart__yearly {
    grid-template-columns: repeat(2, 1fr);
  }

  .spend-chart__canvas {
    height: 220px;
  }
}
</style>
