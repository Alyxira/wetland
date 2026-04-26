<template>
  <div class="chart-container">
    <div class="card-header">
      <h3 class="card-title">湿地生态系统服务功能评分</h3>
      <span class="card-subtitle">ECOSYSTEM SERVICES</span>
    </div>
    <div ref="chartRef" class="chart-body"></div>
    <div class="card-footer">
      <a href="https://iga.cas.cn" target="_blank" class="source-link">
        数据来源：中科院东北地理所、国家林草局
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
const fallbackSection = getFallbackScreenDataset().ecosystemServices

const initChart = (section = fallbackSection) => {
  if (!chartRef.value) return
  
  chartInstance = echarts.init(chartRef.value)
  
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
                <div style="font-size: 14px; margin-top: 4px;">评分：${params.value} 分</div>`
      }
    },
    radar: {
      center: ['50%', '58%'],
      radius: '42%',
      indicator: section.indicators,
      axisName: {
        color: '#2a4a3a',
        fontSize: 9,
        fontWeight: 500
      },
      splitArea: {
        areaStyle: {
          color: ['rgba(58, 106, 90, 0.03)', 'rgba(58, 106, 90, 0.06)', 'rgba(58, 106, 90, 0.09)', 'rgba(58, 106, 90, 0.12)']
        }
      },
      axisLine: {
        lineStyle: {
          color: 'rgba(58, 106, 90, 0.25)'
        }
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(58, 106, 90, 0.2)'
        }
      }
    },
    series: [
      {
        name: '服务功能评分',
        type: 'radar',
        data: [
          {
            value: section.values,
            name: '服务功能评分',
            areaStyle: {
              color: 'rgba(74, 154, 138, 0.35)'
            },
            lineStyle: {
              color: '#4a9a8a',
              width: 3
            },
            itemStyle: {
              color: '#4a9a8a',
              borderColor: '#2a6a5a',
              borderWidth: 2
            }
          }
        ]
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
    initChart(dataset.ecosystemServices)
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
  padding-top: 4px;
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
