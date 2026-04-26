<template>
  <section class="historical-panel">
    <div class="panel-header">
      <div>
        <p class="eyebrow">Historical Water Quality</p>
        <h3>水质历史趋势分析</h3>
        <p class="subtitle">
          {{ wetlandName ? `当前湿地：${wetlandName}` : '请先在地图左侧选择湿地' }}
        </p>
      </div>
      <button class="refresh-btn" @click="loadHistory" :disabled="!wetlandName || loading">
        {{ loading ? '加载中...' : '刷新数据' }}
      </button>
    </div>

    <div class="metric-strip" v-if="rows.length > 0">
      <div class="metric-card chla">
        <span>最新叶绿素a</span>
        <strong>{{ formatNumber(latestRow?.chla) }}</strong>
        <small>mg/m³</small>
      </div>
      <div class="metric-card tss">
        <span>最新悬浮物</span>
        <strong>{{ formatNumber(latestRow?.tss) }}</strong>
        <small>mg/L</small>
      </div>
      <div class="metric-card neutral">
        <span>历史记录数</span>
        <strong>{{ rows.length }}</strong>
        <small>条</small>
      </div>
    </div>

    <div class="chart-card" v-if="rows.length > 0">
      <div ref="chartRef" class="trend-chart"></div>
    </div>

    <div class="empty-state" v-else-if="!loading">
      <p v-if="wetlandName">当前湿地暂无历史水质数据，请在数据库 historical_data 表中填写。</p>
      <p v-else>选择湿地后，这里会自动展示全部历史叶绿素a和悬浮物浓度。</p>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { analyticsApi } from '../api'

const props = defineProps({
  wetlandName: {
    type: String,
    default: ''
  }
})

const chartRef = ref(null)
const rows = ref([])
const loading = ref(false)
let chart = null

const latestRow = computed(() => rows.value[rows.value.length - 1] || null)

onMounted(() => {
  loadHistory()
  window.addEventListener('resize', resizeChart)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeChart)
  if (chart) {
    chart.dispose()
    chart = null
  }
})

watch(
  () => props.wetlandName,
  () => {
    loadHistory()
  }
)

async function loadHistory() {
  if (!props.wetlandName) {
    rows.value = []
    renderChart()
    return
  }

  loading.value = true
  try {
    const response = await analyticsApi.getWetlandHistory(props.wetlandName)
    rows.value = normalizeRows(response.data?.rows || [])
    await nextTick()
    renderChart()
  } catch (error) {
    rows.value = []
    renderChart()
  } finally {
    loading.value = false
  }
}

function normalizeRows(rawRows) {
  return [...rawRows].sort((a, b) => new Date(a.date) - new Date(b.date))
}

function ensureChart() {
  if (!chartRef.value) return null
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }
  return chart
}

function renderChart() {
  nextTick(() => {
    const instance = ensureChart()
    if (!instance) return

    if (rows.value.length === 0) {
      instance.clear()
      return
    }

    const dates = rows.value.map((row) => formatDate(row.date))
    const chlaValues = rows.value.map((row) => toNumber(row.chla))
    const tssValues = rows.value.map((row) => toNumber(row.tss))

    instance.setOption(
      {
        color: ['#0f8b8d', '#e07a5f'],
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(15, 23, 42, 0.92)',
          borderWidth: 0,
          textStyle: { color: '#fff' },
          valueFormatter: (value) => formatNumber(value)
        },
        legend: {
          top: 0,
          right: 8,
          data: ['叶绿素a', '悬浮物']
        },
        grid: {
          top: 52,
          left: 48,
          right: 54,
          bottom: 44,
          containLabel: true
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: dates,
          axisLine: { lineStyle: { color: '#cbd5e1' } },
          axisLabel: { color: '#64748b' }
        },
        yAxis: [
          {
            type: 'value',
            name: '叶绿素a mg/m³',
            axisLine: { show: true, lineStyle: { color: '#0f8b8d' } },
            splitLine: { lineStyle: { color: '#edf2f7' } },
            axisLabel: { color: '#64748b' }
          },
          {
            type: 'value',
            name: '悬浮物 mg/L',
            axisLine: { show: true, lineStyle: { color: '#e07a5f' } },
            splitLine: { show: false },
            axisLabel: { color: '#64748b' }
          }
        ],
        dataZoom: [
          {
            type: 'inside',
            start: 0,
            end: 100
          }
        ],
        series: [
          {
            name: '叶绿素a',
            type: 'line',
            smooth: true,
            symbol: 'circle',
            symbolSize: 8,
            lineStyle: { width: 3 },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(15, 139, 141, 0.22)' },
                { offset: 1, color: 'rgba(15, 139, 141, 0.02)' }
              ])
            },
            data: chlaValues
          },
          {
            name: '悬浮物',
            type: 'line',
            yAxisIndex: 1,
            smooth: true,
            symbol: 'diamond',
            symbolSize: 8,
            lineStyle: { width: 3 },
            data: tssValues
          }
        ]
      },
      true
    )
  })
}

function resizeChart() {
  if (chart) {
    chart.resize()
  }
}

function formatDate(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleDateString('zh-CN')
}

function formatNumber(value) {
  if (value === null || value === undefined || value === '') return '-'
  const number = Number(value)
  if (Number.isNaN(number)) return value
  return number.toFixed(2)
}

function toNumber(value) {
  const number = Number(value)
  return Number.isNaN(number) ? null : number
}
</script>

<style scoped>
.historical-panel {
  position: relative;
  overflow: hidden;
  border-radius: 0;
  padding: 0;
  background:
    radial-gradient(circle at top left, rgba(var(--theme-accent-rgb, 45, 89, 103), 0.12), transparent 32%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.76) 0%, rgba(248, 246, 240, 0.86) 100%);
  border: 1px solid rgba(var(--theme-accent-rgb, 45, 89, 103), 0.12);
  box-shadow: 0 18px 45px rgba(var(--theme-accent-rgb, 45, 89, 103), 0.08);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 18px;
  padding: 22px 22px 0;
}

.eyebrow {
  margin: 0 0 6px;
  color: var(--theme-text-secondary, rgba(20, 42, 49, 0.78));
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.panel-header h3 {
  margin: 0;
  color: var(--theme-accent-strong, #173843);
  font-size: 30px;
  font-family: 'Cormorant Garamond', 'Songti SC', serif;
}

.subtitle {
  margin: 8px 0 0;
  color: var(--theme-text-secondary, rgba(20, 42, 49, 0.78));
  font-size: 14px;
}

.refresh-btn {
  border: 1px solid rgba(var(--theme-accent-rgb, 45, 89, 103), 0.18);
  border-radius: 999px;
  padding: 10px 16px;
  background: rgba(var(--theme-accent-rgb, 45, 89, 103), 0.08);
  color: var(--theme-accent-strong, #173843);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 8px 18px rgba(var(--theme-accent-rgb, 45, 89, 103), 0.12);
}

.refresh-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.metric-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(160px, 1fr));
  gap: 12px;
  margin-bottom: 14px;
  padding: 0 22px;
}

.metric-card {
  border: 1px solid rgba(var(--theme-accent-rgb, 45, 89, 103), 0.12);
  border-radius: 0;
  padding: 14px;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(8px);
}

.metric-card span,
.metric-card small {
  display: block;
  color: var(--theme-text-secondary, rgba(20, 42, 49, 0.78));
  font-size: 12px;
}

.metric-card strong {
  display: block;
  margin: 6px 0 2px;
  color: var(--theme-accent-strong, #173843);
  font-size: 30px;
  line-height: 1;
}

.metric-card.chla {
  box-shadow: inset 4px 0 0 var(--theme-accent, #2d5967);
}

.metric-card.tss {
  box-shadow: inset 4px 0 0 rgba(var(--theme-accent-rgb, 45, 89, 103), 0.6);
}

.metric-card.neutral {
  box-shadow: inset 4px 0 0 rgba(20, 42, 49, 0.35);
}

.chart-card {
  border-top: 1px solid rgba(var(--theme-accent-rgb, 45, 89, 103), 0.1);
  border-radius: 0;
  padding: 12px;
  background: rgba(255, 255, 255, 0.66);
}

.trend-chart {
  width: 100%;
  height: 380px;
}

.empty-state {
  padding: 40px 12px;
  text-align: center;
  color: var(--theme-text-secondary, rgba(20, 42, 49, 0.78));
  border: 1px dashed rgba(var(--theme-accent-rgb, 45, 89, 103), 0.22);
  border-radius: 0;
  background: rgba(255, 255, 255, 0.72);
  margin: 0 22px 22px;
}

@media (max-width: 900px) {
  .panel-header {
    flex-direction: column;
  }

  .metric-strip {
    grid-template-columns: 1fr;
  }

  .trend-chart {
    height: 320px;
  }
}
</style>


