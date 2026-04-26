<template>
  <div class="wetland-card">
    <div class="card-header">
      <h3 class="card-title">湿地概况</h3>
      <span class="card-subtitle">WETLAND OVERVIEW</span>
    </div>
    <div class="card-body">
      <div class="overview-hero">
        <div ref="chartRef" class="overview-chart"></div>
        <div class="hero-caption">湿地保护率目标</div>
      </div>
    </div>
    <div class="overview-stats">
      <div class="stat-block">
        <div class="stat-value">{{ sectionData.totalArea }}</div>
        <div class="stat-unit">{{ sectionData.totalAreaUnit || '万公顷' }}</div>
        <div class="stat-label">湿地总面积</div>
      </div>
      <div class="stat-block">
        <div class="stat-value stat-value--compact">{{ sectionData.nationalRank }}</div>
        <div class="stat-unit stat-unit--compact">{{ sectionData.globalRank }}</div>
        <div class="stat-label">全球排名</div>
      </div>
    </div>
    <div class="card-footer">
      <span class="source-label" @click="openSource">数据来源：国家林业和草原局、全国湿地保护规划（2022—2030 年）</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getFallbackScreenDataset, loadScreenDataset } from './screenData'

const chartRef = ref(null)
let chartInstance = null
const fallbackSection = getFallbackScreenDataset().overview
const sectionData = ref(fallbackSection)

const initChart = (section = fallbackSection) => {
  if (!chartRef.value) return
  chartInstance = echarts.init(chartRef.value, null, { renderer: 'canvas' })

  const option = {
    series: [
      {
        type: 'gauge',
        center: ['50%', '52%'],
        radius: '76%',
        startAngle: 200,
        endAngle: -20,
        min: 0,
        max: 100,
        progress: {
          show: true,
          width: 14,
          roundCap: true,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: '#4a7a6a' },
              { offset: 1, color: '#3a6a5a' }
            ])
          }
        },
        axisLine: {
          lineStyle: {
            width: 14,
            color: [[1, 'rgba(58, 106, 90, 0.15)']]
          }
        },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { show: false },
        pointer: { show: false },
        detail: {
          offsetCenter: [0, '4%'],
          formatter: '{value}%',
          fontSize: 18,
          fontFamily: 'Georgia, serif',
          color: '#2a4a3a',
          fontWeight: 600
        },
        title: {
          show: false,
          offsetCenter: [0, '20%']
        },
        data: [{ value: section.protectionRate, name: '湿地保护率目标' }],
        animationDuration: 2000,
        animationEasing: 'cubicOut'
      }
    ]
  }

  chartInstance.setOption(option)
}

const handleResize = () => {
  chartInstance?.resize()
}

const openSource = () => {
  window.open('https://www.forestry.gov.cn', '_blank')
}

onMounted(async () => {
  await nextTick()
  const dataset = await loadScreenDataset()
  sectionData.value = dataset.overview || fallbackSection
  initChart(dataset.overview)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
})
</script>

<style scoped>
.wetland-card {
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

.card-body {
  flex: 0 0 48%;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 0;
  padding: 8px 0 2px;
}

.overview-hero {
  width: 100%;
  height: 100%;
  display: grid;
  justify-items: center;
  align-content: center;
  gap: 6px;
}

.overview-chart {
  width: 120px;
  height: 92px;
}

.hero-caption {
  font-family: 'Georgia', serif;
  font-size: 10px;
  line-height: 1.1;
  color: rgba(42, 74, 58, 0.62);
  text-align: center;
}

.overview-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  align-items: start;
  padding: 0 4px 6px;
  flex-shrink: 0;
}

.stat-block {
  text-align: center;
}

.stat-value {
  font-family: 'Georgia', 'Times New Roman', serif;
  font-size: 15px;
  line-height: 1.1;
  color: #2a4a3a;
}

.stat-value--compact {
  font-size: 13px;
}

.stat-unit {
  margin-top: 2px;
  font-family: 'Georgia', serif;
  font-size: 9px;
  line-height: 1.2;
  color: rgba(42, 74, 58, 0.68);
}

.stat-unit--compact {
  font-size: 9px;
}

.stat-label {
  margin-top: 3px;
  font-family: 'Georgia', serif;
  font-size: 9px;
  line-height: 1.15;
  color: rgba(42, 74, 58, 0.56);
}

.card-footer {
  padding-top: 6px;
  border-top: 1px solid rgba(58, 106, 90, 0.12);
  display: flex;
  justify-content: center;
  flex-shrink: 0;
}

.source-label {
  font-family: 'Georgia', serif;
  font-size: 8px;
  color: rgba(42, 74, 58, 0.55);
  letter-spacing: 0;
  line-height: 1.1;
  text-align: center;
  cursor: pointer;
  transition: color 0.2s;
  text-decoration: underline;
  text-underline-offset: 5px;
  text-decoration-color: rgba(42, 74, 58, 0.2);
}

.source-label:hover {
  color: #3a5a4a;
  text-decoration-color: rgba(42, 74, 58, 0.4);
}
</style>
