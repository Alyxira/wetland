<template>
  <div class="water-quality-result" v-if="result">
    <div class="result-header">
      <h3>水质反演结果</h3>
      <div class="header-info">
        <span class="task-id">任务ID: {{ result.taskId }}</span>
        <span class="time">{{ formatDateTime(result.acquiredAt) }}</span>
      </div>
    </div>

    <div class="result-meta">
      <div class="meta-item">
        <span class="label">影像ID</span>
        <span class="value">{{ result.imageId }}</span>
      </div>
      <div class="meta-item">
        <span class="label">区域ID</span>
        <span class="value">{{ result.regionId }}</span>
      </div>
      <div class="meta-item">
        <span class="label">获取时间</span>
        <span class="value">{{ formatDateTime(result.acquiredAt) }}</span>
      </div>
    </div>

    <div class="metrics-section" v-if="result.metrics">
      <h4>水质参数统计</h4>
      <div class="metrics-grid">
        <div class="metric-card" v-if="result.metrics.CHLA">
          <div class="metric-header">
            <span class="metric-name">叶绿素a (CHLA)</span>
            <span class="unit">{{ result.metrics.CHLA.unit || 'mg/m³' }}</span>
          </div>
          <div class="metric-value">{{ formatNumber(result.metrics.CHLA.mean) }}</div>
          <div class="metric-stats">
            <div class="stat-item">
              <span class="stat-label">最小值</span>
              <span class="stat-value">{{ formatNumber(result.metrics.CHLA.min) }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">最大值</span>
              <span class="stat-value">{{ formatNumber(result.metrics.CHLA.max) }}</span>
            </div>
          </div>
          <div class="metric-bar chla">
            <div class="bar-fill" :style="{ width: getBarWidth(result.metrics.CHLA.mean, 50) }"></div>
          </div>
        </div>

        <div class="metric-card" v-if="result.metrics.TSS">
          <div class="metric-header">
            <span class="metric-name">悬浮物 (TSS)</span>
            <span class="unit">{{ result.metrics.TSS.unit || 'mg/L' }}</span>
          </div>
          <div class="metric-value">{{ formatNumber(result.metrics.TSS.mean) }}</div>
          <div class="metric-stats">
            <div class="stat-item">
              <span class="stat-label">最小值</span>
              <span class="stat-value">{{ formatNumber(result.metrics.TSS.min) }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">最大值</span>
              <span class="stat-value">{{ formatNumber(result.metrics.TSS.max) }}</span>
            </div>
          </div>
          <div class="metric-bar tss">
            <div class="bar-fill" :style="{ width: getBarWidth(result.metrics.TSS.mean, 150) }"></div>
          </div>
        </div>
      </div>
    </div>

    <div class="raster-section" v-if="visibleRasterEntries.length > 0">
      <h4>结果栅格数据</h4>
      <div class="raster-list">
        <div
          v-for="(entry, index) in visibleRasterEntries"
          :key="`${entry.parameter}-${index}`"
          class="raster-item"
        >
          <span class="raster-name">{{ getParameterLabel(entry.parameter) }}</span>
          <a :href="entry.url" target="_blank" class="raster-link">查看栅格</a>
        </div>
      </div>
    </div>

    <div class="no-data" v-if="!result.metrics || Object.keys(result.metrics).length === 0">
      <p>暂无反演结果数据</p>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { resolveAssetUrl } from '../api'

const props = defineProps({
  result: {
    type: Object,
    default: null
  }
})

const visibleRasterEntries = computed(() => {
  const urls = props.result?.resultRasterUrls || {}
  const visibleParameters = new Set(['CHLA', 'TSS'])
  return Object.entries(urls)
    .filter(([parameter]) => visibleParameters.has(parameter))
    .map(([parameter, url]) => ({ parameter, url: resolveAssetUrl(url) }))
})

function formatDateTime(dateStr) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

function formatNumber(num) {
  if (num === null || num === undefined) return '-'
  return typeof num === 'number' ? num.toFixed(2) : num
}

function getBarWidth(value, maxValue) {
  if (value === null || value === undefined) return '0%'
  const percentage = Math.min((value / maxValue) * 100, 100)
  return percentage + '%'
}

function getParameterLabel(parameter) {
  const labels = {
    CHLA: '叶绿素a',
    TSS: '悬浮物'
  }
  return labels[parameter] || parameter
}
</script>

<style scoped>
.water-quality-result {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.result-header h3 {
  margin: 0;
  color: #333;
  font-size: 18px;
}

.header-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 5px;
}

.task-id {
  font-size: 12px;
  color: #666;
  font-family: monospace;
}

.time {
  font-size: 13px;
  color: #999;
}

.result-meta {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 6px;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.meta-item .label {
  font-size: 12px;
  color: #999;
}

.meta-item .value {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.metrics-section h4,
.raster-section h4 {
  margin: 0 0 15px 0;
  color: #333;
  font-size: 15px;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 15px;
}

.metric-card {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 15px;
  border: 1px solid #e9ecef;
}

.metric-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.metric-name {
  font-weight: 600;
  color: #333;
  font-size: 14px;
}

.unit {
  font-size: 11px;
  color: #999;
  background: #e9ecef;
  padding: 2px 6px;
  border-radius: 3px;
}

.metric-value {
  font-size: 32px;
  font-weight: 700;
  color: #3388ff;
  margin-bottom: 15px;
}

.metric-stats {
  display: flex;
  gap: 20px;
  margin-bottom: 15px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-label {
  font-size: 11px;
  color: #999;
}

.stat-value {
  font-size: 13px;
  font-weight: 500;
  color: #666;
}

.metric-bar {
  height: 6px;
  background: #e9ecef;
  border-radius: 3px;
  overflow: hidden;
}

.metric-bar .bar-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.5s ease;
}

.metric-bar.chla .bar-fill {
  background: linear-gradient(90deg, #3388ff, #66aaff);
}

.metric-bar.tss .bar-fill {
  background: linear-gradient(90deg, #28a745, #5cb85c);
}

.raster-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.raster-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.raster-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 15px;
  background: #f8f9fa;
  border-radius: 6px;
  border: 1px solid #e9ecef;
}

.raster-name {
  font-weight: 500;
  color: #333;
}

.raster-link {
  color: #3388ff;
  text-decoration: none;
  font-size: 13px;
  padding: 4px 12px;
  background: white;
  border: 1px solid #3388ff;
  border-radius: 4px;
  transition: all 0.2s;
}

.raster-link:hover {
  background: #3388ff;
  color: white;
}

.no-data {
  text-align: center;
  padding: 40px;
  color: #999;
}

@media (max-width: 768px) {
  .result-meta {
    flex-direction: column;
    gap: 10px;
  }
}
</style>

