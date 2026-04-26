<template>
  <div class="chart-container">
    <div class="card-header">
      <h3 class="card-title">湿地保护体系层级结构</h3>
      <span class="card-subtitle">PROTECTION SYSTEM</span>
    </div>
    <div class="chart-body chart-body--split">
      <div class="sunburst-visual">
        <div ref="chartRef" class="sunburst-chart"></div>
        <div class="sunburst-center-copy">
          <div class="sunburst-center-label">总量</div>
          <div class="sunburst-center-value">{{ centerTotal }}处</div>
        </div>
      </div>
      <div class="legend-panel">
        <div
          v-for="item in sectionData.outerData"
          :key="item.name"
          class="legend-row"
        >
          <span class="legend-swatch" :style="{ background: getLegendColor(item.level) }"></span>
          <span class="legend-name">{{ item.name }}</span>
          <span class="legend-value">{{ item.value }}处</span>
        </div>
      </div>
    </div>
    <div class="card-footer">
      <a href="https://www.forestry.gov.cn" target="_blank" class="source-link">
        数据来源：国家林草局
      </a>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { getFallbackScreenDataset, loadScreenDataset } from './screenData'

const chartRef = ref(null)
let chartInstance = null
const fallbackSection = getFallbackScreenDataset().protectionSystem
const sectionData = ref(fallbackSection)
const centerTotal = computed(() =>
  (sectionData.value?.outerData || []).reduce((sum, item) => sum + Number(item.value || 0), 0)
)

const initChart = (section = fallbackSection) => {
  if (!chartRef.value) return
  
  chartInstance = echarts.init(chartRef.value)
  
  const innerData = section.innerData
  const outerData = section.outerData
  
  const getColorByLevelAndName = (level, name) => {
    const colors = {
      '国际': ['#5a9a8a', '#4a8a7a'],
      '国家': ['#4a8a7a', '#3a6a5a'],
      '省级': ['#6aaaaa', '#5a8a7a']
    }
    return new echarts.graphic.LinearGradient(0, 0, 1, 1, [
      { offset: 0, color: colors[level]?.[0] || '#5a9a8a' },
      { offset: 1, color: colors[level]?.[1] || '#3a6a5a' }
    ])
  }

  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(240, 236, 231, 0.98)',
      borderColor: 'rgba(58, 106, 90, 0.3)',
      borderWidth: 1,
      confine: false,
      renderMode: 'html',
      appendToBody: true,
      textStyle: {
        color: '#2a4a3a',
        fontSize: 14
      },
      formatter: (params) => {
        return `<div style="font-size: 16px; font-weight: 600;">${params.name}</div>
                <div style="font-size: 14px; margin-top: 4px;">${params.value} 处</div>`
      }
    },
    series: [
      {
        name: '层面',
        type: 'pie',
        radius: ['0%', '32%'],
        center: ['50%', '54%'],
        zlevel: 1,
        label: {
          show: false,
          position: 'inside'
        },
        data: innerData,
        itemStyle: {
          borderRadius: 6
        },
        color: ['#3a6a5a', '#3a5a6a', '#4a7a6a']
      },
      {
        name: '类型',
        type: 'pie',
        radius: ['40%', '68%'],
        center: ['50%', '54%'],
        zlevel: 2,
        label: {
          show: false,
          position: 'outside',
          formatter: '{b}\n{c}处',
          fontSize: 9,
          color: '#2a4a3a',
          backgroundColor: 'rgba(240, 236, 231, 0.85)',
          padding: [3, 5],
          borderRadius: 4,
          lineHeight: 12
        },
        labelLine: {
          show: false,
          length: 6,
          length2: 8,
          smooth: 0.2,
          lineStyle: {
            color: 'rgba(58, 106, 90, 0.5)'
          }
        },
        itemStyle: {
          borderRadius: 6,
          borderColor: '#f0ece7',
          borderWidth: 2
        },
        data: outerData.map((d) => ({
          ...d,
          itemStyle: {
            color: getColorByLevelAndName(d.level, d.name)
          }
        }))
      }
    ]
  }
  
  chartInstance.setOption(option)
}

const handleResize = () => {
  if (chartInstance) {
    chartInstance.resize()
  }
}

onMounted(() => {
  loadScreenDataset().then((dataset) => {
    sectionData.value = dataset.protectionSystem || fallbackSection
    initChart(dataset.protectionSystem)
    window.addEventListener('resize', handleResize)
  })
})

onUnmounted(() => {
  if (chartInstance) {
    chartInstance.dispose()
  }
  window.removeEventListener('resize', handleResize)
})

const getLegendColor = (level) => {
  if (level === '国际') return 'linear-gradient(135deg, #5a9a8a, #4a8a7a)'
  if (level === '国家') return 'linear-gradient(135deg, #4a8a7a, #3a6a5a)'
  return 'linear-gradient(135deg, #6aaaaa, #5a8a7a)'
}
</script>

<style scoped>
.chart-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 14px 16px 10px;
  box-sizing: border-box;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-rendering: optimizeLegibility;
}

.card-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  column-gap: 8px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(58, 106, 90, 0.12);
  flex-shrink: 0;
}

.card-title {
  font-family: 'Georgia', 'Times New Roman', serif;
  font-size: 22px;
  font-weight: 400;
  color: #2a4a3a;
  margin: 0;
  letter-spacing: 1px;
  line-height: 1.18;
}

.card-subtitle {
  font-family: 'Georgia', serif;
  font-size: 10px;
  color: rgba(42, 74, 58, 0.5);
  letter-spacing: 1px;
  text-align: right;
  line-height: 1.2;
}

.chart-body {
  flex: 1;
  min-height: 0;
  padding: 4px 0 8px;
}

.chart-body--split {
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  gap: 10px;
  align-items: center;
}

.sunburst-visual {
  min-width: 0;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.sunburst-chart {
  width: 100%;
  height: 100%;
  min-height: 118px;
}

.sunburst-center-copy {
  position: absolute;
  left: 50%;
  top: 52%;
  transform: translate(-50%, -50%);
  display: grid;
  justify-items: center;
  gap: 2px;
  pointer-events: none;
}

.sunburst-center-label {
  font-size: 10px;
  line-height: 1;
  font-weight: 600;
  color: #f4f7f5;
}

.sunburst-center-value {
  font-size: 11px;
  line-height: 1;
  font-weight: 600;
  color: #f4f7f5;
}

.legend-panel {
  display: grid;
  gap: 10px;
  align-content: center;
}

.legend-row {
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
}

.legend-swatch {
  width: 18px;
  height: 12px;
  border-radius: 4px;
}

.legend-name {
  font-size: 10px;
  line-height: 1.2;
  color: #2a4a3a;
}

.legend-value {
  font-size: 10px;
  line-height: 1.2;
  color: rgba(42, 74, 58, 0.7);
}

.card-footer {
  padding-top: 6px;
  border-top: 1px solid rgba(58, 106, 90, 0.12);
  display: flex;
  justify-content: center;
  flex-shrink: 0;
}

.source-link {
  font-family: 'Georgia', serif;
  font-size: 9px;
  color: rgba(42, 74, 58, 0.55);
  text-decoration: none;
  letter-spacing: 0;
  line-height: 1.1;
  text-align: center;
  transition: color 0.3s ease;
}

.source-link:hover {
  color: #3a6a5a;
  text-decoration: underline;
}
</style>
