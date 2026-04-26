<template>
  <div class="rose-card">
    <div class="card-header">
      <h3 class="card-title">湿地类型构成</h3>
      <span class="card-subtitle">WETLAND CLASSIFICATION</span>
    </div>
    <div class="card-body">
      <div ref="chartRef" class="rose-chart"></div>
    </div>
    <div class="card-footer">
      <span class="source-label" @click="openSource">数据来源：国家林业和草原局</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { getFallbackScreenDataset, loadScreenDataset } from './screenData'

const chartRef = ref(null)
let chartInstance = null
const fallbackSection = getFallbackScreenDataset().wetlandTypes

const initChart = (section = fallbackSection) => {
  if (!chartRef.value) return
  chartInstance = echarts.init(chartRef.value)

  const data = section.items

  const option = {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(35, 55, 50, 0.92)',
      borderColor: 'rgba(74, 122, 106, 0.3)',
      borderWidth: 1,
      padding: [20, 30],
      confine: false,
      renderMode: 'html',
      appendToBody: true,
      textStyle: {
        color: '#d8e4e0',
        fontSize: 14,
        fontFamily: 'Georgia, serif'
      },
      formatter: (params) => {
        const d = data.find(item => item.name === params.name)
        const total = d ? (d.category === '天然湿地' ? 2594 : 4000) : 0
        const pct = total ? ((params.value / total) * 100).toFixed(1) : 0
        return `<strong style="font-size:18px;letter-spacing:1px;">${params.name}</strong><br/>
                <span style="color:#9ab8b0;font-size:13px;">面积：${params.value} 万公顷</span><br/>
                <span style="color:#9ab8b0;font-size:13px;">占比：${pct}%</span><br/>
                <span style="color:#7abaaa;font-size:11px;">${d?.category || ''}</span>`
      }
    },
    series: [{
      type: 'pie',
      radius: ['18%', '60%'],
      center: ['50%', '50%'],
      roseType: 'area',
      itemStyle: {
        borderRadius: 6,
        borderColor: 'rgba(220, 235, 230, 0.25)',
        borderWidth: 2
      },
      label: {
        show: true,
        position: 'outside',
        formatter: (params) => {
          return `${params.value}万\n${params.name}`
        },
        fontSize: 12,
        fontFamily: 'Georgia, serif',
        color: '#2a4a3a',
        lineHeight: 16,
        fontWeight: 400
      },
      labelLine: {
        length: 10,
        length2: 12,
        smooth: true,
        lineStyle: {
          color: 'rgba(58, 106, 90, 0.35)',
          width: 2
        }
      },
      emphasis: {
        itemStyle: {
          shadowBlur: 40,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.15)'
        },
        label: {
          fontSize: 14,
          fontWeight: 400
        }
      },
      data: data.map((d, i) => ({
        ...d,
        itemStyle: {
          color: d.category === '天然湿地'
            ? new echarts.graphic.LinearGradient(0, 0, 1, 1, [
                { offset: 0, color: '#4a7a6a' },
                { offset: 1, color: '#3a6a5a' }
              ])
            : new echarts.graphic.LinearGradient(0, 0, 1, 1, [
                { offset: 0, color: '#5a8a7a' },
                { offset: 1, color: '#4a7a6a' }
              ])
        }
      })),
      animationDuration: 1800,
      animationEasing: 'cubicOut'
    }]
  }

  chartInstance.setOption(option)
}

const handleResize = () => {
  chartInstance?.resize()
}

const openSource = () => {
  window.open('https://www.forestry.gov.cn', '_blank')
}

onMounted(() => {
  loadScreenDataset().then((dataset) => {
    initChart(dataset.wetlandTypes)
    window.addEventListener('resize', handleResize)
  })
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
})
</script>

<style scoped>
.rose-card {
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
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 0;
}

.rose-chart {
  width: 100%;
  height: 100%;
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
  font-size: 10px;
  color: rgba(42, 74, 58, 0.55);
  letter-spacing: 0;
  line-height: 1.35;
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
