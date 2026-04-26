<template>
  <div class="chart-container">
    <div class="card-header">
      <h3 class="card-title">湿地保护修复成效</h3>
      <span class="card-subtitle">WETLAND PROTECTION RESULTS</span>
    </div>
    <div ref="chartRef" class="chart-body"></div>
    <div class="card-footer">
      <a href="https://www.forestry.gov.cn" target="_blank" class="source-link">
        数据来源：国家林草局 2026 年湿地保护成效发布
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
const fallbackSection = getFallbackScreenDataset().protectionResults

const initChart = (section = fallbackSection) => {
  if (!chartRef.value) return
  
  chartInstance = echarts.init(chartRef.value)
  const wrapCategoryLabel = (value) => {
    if (typeof value !== 'string') return value
    const labelMap = {
      '保护修复项目\n(个)': '保护修复',
      '新增修复湿地\n(万公顷)': '新增修复',
      '红树林营造\n(公顷)': '红树林',
      '互花米草防治\n(万公顷)': '互花米草',
      '湿地公园保护\n(万公顷)': '湿地公园'
    }
    if (labelMap[value]) return labelMap[value]
    return value.replace(/\n.*/g, '')
  }
  const colors = [
    ['#5a9a8a', '#3a6a5a'],
    ['#4a8a7a', '#2a5a4a'],
    ['#6aaaaa', '#4a7a6a'],
    ['#7ababa', '#5a8a7a'],
    ['#5abaaa', '#3a8a7a']
  ]
  const series = section.items.map((item, index) => ({
    name: item.name,
    type: 'bar',
    yAxisIndex: item.axisIndex || 0,
    data: section.categories.map((_, categoryIndex) => (
      categoryIndex === index
        ? {
            value: item.value,
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: colors[index]?.[0] || '#5a9a8a' },
                { offset: 1, color: colors[index]?.[1] || '#3a6a5a' }
              ])
            }
          }
        : null
    )),
    barWidth: 26,
    barGap: '20%'
  }))
  
  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow',
        animation: false
      },
      backgroundColor: 'rgba(240, 236, 231, 0.98)',
      borderColor: 'rgba(58, 106, 90, 0.3)',
      borderWidth: 1,
      padding: [16, 20],
      confine: false,
      transitionDuration: 0.1,
      renderMode: 'html',
      appendToBody: true,
        textStyle: {
          color: '#2a4a3a',
          fontSize: 14
        },
      formatter: (params) => {
        const item = params.find(p => p.value !== null)
        if (!item) return ''
        const unit = item.seriesName.includes('项目') ? '个' : 
                     item.seriesName.includes('红树林') ? '公顷' : '万公顷'
        return `<div style="font-size: 16px; font-weight: 600;">${item.seriesName}</div>
                <div style="font-size: 14px; margin-top: 4px;">${item.value.toLocaleString()} ${unit}</div>`
      }
    },
    legend: {
      show: false
    },
    grid: {
      left: '12%',
      right: '12%',
      top: '18%',
      bottom: '30%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: section.categories,
      axisLine: {
        lineStyle: {
          color: 'rgba(58, 106, 90, 0.3)'
        }
      },
      axisTick: {
        show: false
      },
      axisLabel: {
        color: 'rgba(42, 74, 58, 0.75)',
        fontSize: 9,
        interval: 0,
        lineHeight: 11,
        margin: 10,
        formatter: wrapCategoryLabel,
        align: 'center'
      }
    },
    yAxis: [
      {
        type: 'value',
        position: 'left',
        axisLine: {
          show: true,
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
          fontSize: 10,
          formatter: (value) => {
            if (value >= 10000) return (value / 10000) + '万'
            return value
          }
        }
      },
      {
        type: 'value',
        position: 'right',
        axisLine: {
          show: true,
          lineStyle: {
            color: 'rgba(58, 106, 90, 0.3)'
          }
        },
        splitLine: {
          show: false
        },
        axisLabel: {
          color: 'rgba(42, 74, 58, 0.7)',
          fontSize: 10
        }
      }
    ],
    series
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
    initChart(dataset.protectionResults)
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
  padding: 10px 0 2px;
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
