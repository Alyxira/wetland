<template>
  <div class="chart-container">
    <div class="card-header">
      <h3 class="card-title">湿地物种多样性统计</h3>
      <span class="card-subtitle">WETLAND BIODIVERSITY</span>
    </div>
    <div ref="chartRef" class="chart-body"></div>
    <div class="card-footer">
      <a href="https://www.forestry.gov.cn" target="_blank" class="source-link">
        数据来源：国家林草局湿地资源调查成果
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
const fallbackSection = getFallbackScreenDataset().biodiversity

const initChart = (section = fallbackSection) => {
  if (!chartRef.value) return
  
  chartInstance = echarts.init(chartRef.value)
  
  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
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
        const item = params[0]
        let detail = ''
        detail = section.items.find((entry) => entry.name === item.name)?.detail
          ? `<div style="margin-top: 4px; font-size: 12px; color: rgba(42, 74, 58, 0.7);">${section.items.find((entry) => entry.name === item.name)?.detail}</div>`
          : ''
        return `<div style="font-size: 16px; font-weight: 600;">${item.name}</div>
                <div style="font-size: 14px; margin-top: 4px;">总数：${item.value.toLocaleString()} 种</div>
                ${detail}`
      }
    },
    grid: {
      left: '5%',
      right: '12%',
      top: '8%',
      bottom: '8%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      axisLine: {
        lineStyle: {
          color: 'rgba(58, 106, 90, 0.3)'
        }
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(58, 106, 90, 0.12)'
        }
      },
      axisLabel: {
        color: 'rgba(42, 74, 58, 0.7)',
        fontSize: 12
      }
    },
    yAxis: {
      type: 'category',
      data: section.items.map((item) => item.name),
      axisLine: {
        lineStyle: {
          color: 'rgba(58, 106, 90, 0.3)'
        }
      },
      axisTick: {
        show: false
      },
      axisLabel: {
        color: '#2a4a3a',
        fontSize: 12,
        fontWeight: 500
      }
    },
    series: [
      {
        name: '物种数量',
        type: 'bar',
        data: section.items.map((item, index) => ({
          value: item.value,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, index === 0
              ? [{ offset: 0, color: '#3a6a5a' }, { offset: 1, color: '#5a9a8a' }]
              : index === 1
                ? [{ offset: 0, color: '#4a7a6a' }, { offset: 1, color: '#6aaaaa' }]
                : [{ offset: 0, color: '#5a8a7a' }, { offset: 1, color: '#7ababa' }]),
            borderRadius: [0, 6, 6, 0]
          }
        })),
        barWidth: 18,
        label: {
          show: true,
          position: 'right',
          color: '#2a4a3a',
          fontSize: 11,
          fontWeight: 500,
          formatter: '{c} 种'
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
    initChart(dataset.biodiversity)
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
