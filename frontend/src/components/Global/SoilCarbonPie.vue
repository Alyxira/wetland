<template>
  <div class="chart-container">
    <div class="card-header">
      <h3 class="card-title">沼泽湿地土壤有机碳储量分布</h3>
      <span class="card-subtitle">SOIL ORGANIC CARBON</span>
    </div>
    <div class="chart-body chart-body--split">
      <div class="soil-visual">
        <div ref="chartRef" class="soil-chart"></div>
      </div>
      <div class="legend-panel">
        <div
          v-for="item in sectionData.items"
          :key="item.name"
          class="legend-row"
        >
          <span class="legend-swatch" :style="{ background: getItemColor(item.name) }"></span>
          <span class="legend-name">{{ item.name }}</span>
          <span class="legend-value">{{ item.value }}亿吨</span>
        </div>
      </div>
    </div>
    <div class="card-footer">
      <a href="https://iga.cas.cn" target="_blank" class="source-link">
        数据来源：中科院东北地理所《中国湿地研究报告》
      </a>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { getFallbackScreenDataset, loadScreenDataset } from './screenData'

const chartRef = ref(null)
let chartInstance = null
const fallbackSection = getFallbackScreenDataset().soilCarbon
const sectionData = ref(fallbackSection)

const initChart = (section = fallbackSection) => {
  if (!chartRef.value) return
  
  chartInstance = echarts.init(chartRef.value)
  
  const total = section.total
  
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
                <div style="font-size: 14px; margin-top: 4px;">碳储量：${params.value} 亿吨</div>
                <div style="font-size: 13px; margin-top: 4px; color: rgba(42, 74, 58, 0.7);">占比：${params.percent}%</div>`
      }
    },
    legend: {
      show: false
    },
    series: [
      {
        name: '碳储量分布',
        type: 'pie',
        radius: ['38%', '70%'],
        center: ['50%', '52%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 8,
          borderColor: '#f0ece7',
          borderWidth: 3
        },
        label: {
          show: true,
          position: 'center',
          formatter: () => {
            return `{total|${total}}`
          },
          rich: {
            total: {
              fontSize: 26,
              fontFamily: 'Georgia, serif',
              fontWeight: 400,
              color: '#2a4a3a',
              lineHeight: 34
            },
          }
        },
        emphasis: {
          label: {
            show: true
          }
        },
        labelLine: {
          show: false
        },
        data: section.items.map((item, index) => ({
          ...item,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 1, 1, index === 0
              ? [{ offset: 0, color: '#5a9a8a' }, { offset: 1, color: '#3a6a5a' }]
              : index === 1
                ? [{ offset: 0, color: '#4a8a7a' }, { offset: 1, color: '#2a5a4a' }]
                : [{ offset: 0, color: '#6aaaaa' }, { offset: 1, color: '#4a7a6a' }])
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
    sectionData.value = dataset.soilCarbon || fallbackSection
    initChart(dataset.soilCarbon)
    window.addEventListener('resize', handleResize)
  })
})

onUnmounted(() => {
  if (chartInstance) {
    chartInstance.dispose()
  }
  window.removeEventListener('resize', handleResize)
})

const getItemColor = (name) => {
  if (name === '青藏高原') return 'linear-gradient(135deg, #5a9a8a, #3a6a5a)'
  if (name === '东北地区') return 'linear-gradient(135deg, #4a8a7a, #2a5a4a)'
  return 'linear-gradient(135deg, #6aaaaa, #4a7a6a)'
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
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  align-items: center;
}

.soil-visual {
  min-width: 0;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.soil-chart {
  width: 100%;
  height: 100%;
  min-height: 118px;
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
  font-size: 8px;
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
