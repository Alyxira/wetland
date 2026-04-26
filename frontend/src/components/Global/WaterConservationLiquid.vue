<template>
  <div class="chart-container">
    <div class="card-header">
      <h3 class="card-title">湿地年涵养水源量</h3>
      <span class="card-subtitle">WATER CONSERVATION</span>
    </div>
    <div ref="chartRef" class="chart-body"></div>
    <div class="card-footer">
      <a href="https://www.forestry.gov.cn" target="_blank" class="source-link">
        数据来源：国家林草局
      </a>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import 'echarts-liquidfill'
import { getFallbackScreenDataset, loadScreenDataset } from './screenData'

const chartRef = ref(null)
let chartInstance = null
const fallbackSection = getFallbackScreenDataset().waterConservation

const initChart = (section = fallbackSection) => {
  if (!chartRef.value) return
  
  chartInstance = echarts.init(chartRef.value)
  
  const value = section.value
  const maxValue = section.maxValue
  const ratio = value / maxValue
  
  const option = {
    backgroundColor: 'transparent',
    series: [
      {
        type: 'liquidFill',
        radius: '70%',
        center: ['50%', '50%'],
        data: [
          {
            value: ratio,
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#6abaaa' },
                { offset: 1, color: '#3a6a5a' }
              ])
            }
          },
          {
            value: ratio - 0.05,
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#5a9a8a' },
                { offset: 1, color: '#2a5a4a' }
              ])
            }
          }
        ],
        backgroundStyle: {
          color: 'rgba(58, 106, 90, 0.1)',
          borderColor: 'rgba(58, 106, 90, 0.3)',
          borderWidth: 3
        },
        outline: {
          show: true,
          borderDistance: 8,
          itemStyle: {
            color: 'none',
            borderColor: 'rgba(58, 106, 90, 0.4)',
            borderWidth: 4
          }
        },
        label: {
          show: true,
          formatter: () => {
            return `{value|${value.toLocaleString()}}\n{unit|${section.unit || '亿立方米/年'}}`
          },
          rich: {
            value: {
              fontSize: 28,
              fontFamily: 'Georgia, serif',
              fontWeight: 400,
              color: '#2a4a3a',
              lineHeight: 70
            },
            unit: {
              fontSize: 13,
              fontFamily: 'Georgia, serif',
              color: 'rgba(42, 74, 58, 0.65)',
              lineHeight: 36
            }
          }
        },
        emphasis: {
          itemStyle: {
            opacity: 0.9
          }
        }
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
    initChart(dataset.waterConservation)
    window.addEventListener('resize', handleResize)
  })
})

onUnmounted(() => {
  if (chartInstance) {
    chartInstance.dispose()
  }
  window.removeEventListener('resize', handleResize)
})
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
  font-size: 10px;
  color: rgba(42, 74, 58, 0.55);
  text-decoration: none;
  letter-spacing: 0;
  line-height: 1.35;
  text-align: center;
  transition: color 0.3s ease;
}

.source-link:hover {
  color: #3a6a5a;
  text-decoration: underline;
}
</style>
