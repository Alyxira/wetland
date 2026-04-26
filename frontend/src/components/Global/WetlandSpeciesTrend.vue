<template>
  <div class="chart-container">
    <div class="card-header">
      <h3 class="card-title">濒危物种种群恢复趋势</h3>
      <span class="card-subtitle">ENDANGERED SPECIES RECOVERY</span>
    </div>
    <div ref="chartRef" class="chart-body"></div>
    <div class="card-footer">
      <a href="https://www.forestry.gov.cn" target="_blank" class="source-link">
        数据来源：国家林草局全国越冬水鸟同步调查
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
const fallbackSection = getFallbackScreenDataset().endangeredSpecies

const initChart = (section = fallbackSection) => {
  if (!chartRef.value) return
  
  chartInstance = echarts.init(chartRef.value)
  
  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
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
        let result = `<div style="font-size: 16px; font-weight: 600; margin-bottom: 6px;">${params[0].axisValue} 年</div>`
        params.forEach(item => {
          result += `<div style="display: flex; align-items: center; margin: 6px 0;">
            <span style="display: inline-block; width: 10px; height: 10px; background: ${item.color}; border-radius: 50%; margin-right: 8px;"></span>
            <span style="font-size: 13px;">${item.seriesName}：${item.value.toLocaleString()} 只</span>
          </div>`
        })
        return result
      }
    },
    legend: {
      data: section.series.map((item) => item.name),
      top: '1%',
      left: 'center',
      textStyle: {
        color: '#2a4a3a',
        fontSize: 9
      },
      itemWidth: 16,
      itemHeight: 10,
      itemGap: 12
    },
    grid: {
      left: '14%',
      right: '8%',
      top: '24%',
      bottom: '14%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: section.years,
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
        fontSize: 10,
        margin: 12
      }
    },
    yAxis: {
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
        color: '#2a4a3a',
        fontSize: 10
      }
    },
    series: section.series.map((item, index) => ({
        name: item.name,
        type: 'line',
        data: item.values,
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        lineStyle: {
          color: ['#4a9a8a', '#5abaaa', '#6acaaa'][index] || '#4a9a8a',
          width: 4
        },
        itemStyle: {
          color: ['#4a9a8a', '#5abaaa', '#6acaaa'][index] || '#4a9a8a',
          borderWidth: 2,
          borderColor: ['#2a6a5a', '#3a7a6a', '#4a8a7a'][index] || '#2a6a5a'
        },
        emphasis: {
          itemStyle: {
            color: ['#6abaaa', '#7abaaa', '#8adaaa'][index] || '#6abaaa',
            borderWidth: 3
          }
        }
      }))
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
    initChart(dataset.endangeredSpecies)
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
  padding-top: 2px;
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
