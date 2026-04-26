<template>
  <div :class="['timeline-container', { 'timeline-container--vertical': isVertical }]" @wheel.stop @touchmove.stop>
    <div class="timeline-header">
      <h3>时间轴</h3>
      <div class="current-time" v-if="currentTime">
        <span class="time-label">当前时间:</span>
        <span class="time-value">{{ formatYearSeason(currentTime) }}</span>
      </div>
    </div>

    <div class="timeline-wrapper">
      <div
        class="timeline-track"
        ref="timelineTrack"
        @wheel.prevent
        @touchmove.prevent
        @mousedown="startDrag"
        @touchstart="startDrag"
      >
        <div class="timeline-axis"></div>

        <div
          class="timeline-thumb"
          :style="isVertical ? { top: sliderPosition + '%', left: '50%' } : { left: sliderPosition + '%', top: '50%' }"
          @mousedown.stop="startDrag"
          @touchstart.stop="startDrag"
        >
          <span class="thumb-stick"></span>
          <span class="thumb-dot"></span>
        </div>
        
        <div 
          v-for="(item, index) in timelineItems" 
          :key="item.id"
          class="timeline-item"
          :class="{ 
            active: selectedIndex === index,
            hasWaterMask: item.hasWaterMask 
          }"
          :style="isVertical ? { top: getItemPosition(index) + '%', left: '50%' } : { left: getItemPosition(index) + '%' }"
          @click="selectItem(index)"
        >
          <div class="item-marker">
            <div class="marker-dot"></div>
            <div class="marker-pulse" v-if="selectedIndex === index"></div>
          </div>
          
          <div class="item-label">{{ formatYearSeason(item.acquisitionDate) }}</div>
        </div>

      </div>

      <div class="timeline-controls">
        <button @click="previousItem" :disabled="selectedIndex === 0" class="control-btn">
          上一个
        </button>
        <button @click="playTimeline" :class="{ playing: isPlaying }" class="control-btn play-btn">
          {{ isPlaying ? '暂停' : '播放' }}
        </button>
        <button @click="nextItem" :disabled="selectedIndex === timelineItems.length - 1" class="control-btn">
          下一个
        </button>
      </div>

      <div class="timeline-legend">
        <div class="legend-item">
          <span class="legend-dot active"></span>
          <span>当前选中</span>
        </div>
        <div class="legend-item">
          <span class="legend-dot has-mask"></span>
          <span>有水体掩膜</span>
        </div>
        <div class="legend-item">
          <span class="legend-dot normal"></span>
          <span>仅影像</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'

const props = defineProps({
  tiffImages: {
    type: Array,
    default: () => []
  },
  orientation: {
    type: String,
    default: 'horizontal'
  },
  modelValue: {
    type: Number,
    default: null
  }
})

const emit = defineEmits(['update:modelValue', 'image-selected', 'time-changed'])

const timelineTrack = ref(null)
const selectedIndex = ref(0)
const isDragging = ref(false)
const isPlaying = ref(false)
const playInterval = ref(null)
const TIMELINE_SIDE_PADDING = 3

const timelineItems = computed(() => {
  return props.tiffImages
    .filter(img => img.acquisitionDate)
    .sort((a, b) => new Date(a.acquisitionDate) - new Date(b.acquisitionDate))
    .map(img => ({
      id: img.id,
      acquisitionDate: img.acquisitionDate,
      fileName: img.fileName,
      hasWaterMask: img.hasWaterMask || false,
      image: img
    }))
})

const currentTime = computed(() => {
  if (timelineItems.value.length > 0 && selectedIndex.value >= 0) {
    return timelineItems.value[selectedIndex.value]?.acquisitionDate
  }
  return null
})

const isVertical = computed(() => props.orientation === 'vertical')

const sliderPosition = computed(() => {
  return getItemPosition(selectedIndex.value)
})

function getItemPosition(index) {
  if (timelineItems.value.length <= 1) return 50
  const padding = TIMELINE_SIDE_PADDING
  const range = 100 - 2 * padding
  return padding + (index / (timelineItems.value.length - 1)) * range
}

function selectItem(index) {
  if (index >= 0 && index < timelineItems.value.length) {
    selectedIndex.value = index
    const item = timelineItems.value[index]
    emit('update:modelValue', item.id)
    emit('image-selected', item.image)
    emit('time-changed', item.acquisitionDate)
  }
}

function previousItem() {
  if (selectedIndex.value > 0) {
    selectItem(selectedIndex.value - 1)
  }
}

function nextItem() {
  if (selectedIndex.value < timelineItems.value.length - 1) {
    selectItem(selectedIndex.value + 1)
  }
}

function playTimeline() {
  if (isPlaying.value) {
    stopPlay()
  } else {
    startPlay()
  }
}

function startPlay() {
  if (timelineItems.value.length === 0) return
  if (selectedIndex.value >= timelineItems.value.length - 1) {
    selectItem(0)
  }
  isPlaying.value = true
  playInterval.value = setInterval(() => {
    if (selectedIndex.value < timelineItems.value.length - 1) {
      nextItem()
    } else {
      selectItem(0)
    }
  }, 2000)
}

function stopPlay() {
  isPlaying.value = false
  if (playInterval.value) {
    clearInterval(playInterval.value)
    playInterval.value = null
  }
}

function startDrag(event) {
  event.preventDefault()
  event.stopPropagation()
  
  if (event.type === 'mousedown') {
    event.preventDefault()
  }
  
  isDragging.value = true
  
  const handleMove = (e) => {
    if (!isDragging.value) return
    
    e.preventDefault()
    e.stopPropagation()
    
    const rect = timelineTrack.value.getBoundingClientRect()
    const point = e.type.includes('touch') ? e.touches[0] : e
    const percentage = isVertical.value
      ? Math.max(0, Math.min(100, ((point.clientY - rect.top) / rect.height) * 100))
      : Math.max(0, Math.min(100, ((point.clientX - rect.left) / rect.width) * 100))
    
    const padding = TIMELINE_SIDE_PADDING
    const range = 100 - 2 * padding
    const normalizedPercentage = (percentage - padding) / range
    const index = Math.round(normalizedPercentage * (timelineItems.value.length - 1))
    
    selectItem(Math.max(0, Math.min(timelineItems.value.length - 1, index)))
  }
  
  const handleEnd = (e) => {
    isDragging.value = false
    document.removeEventListener('mousemove', handleMove)
    document.removeEventListener('mouseup', handleEnd)
    document.removeEventListener('touchmove', handleMove)
    document.removeEventListener('touchend', handleEnd)
  }
  
  document.addEventListener('mousemove', handleMove, { passive: false })
  document.addEventListener('mouseup', handleEnd)
  document.addEventListener('touchmove', handleMove, { passive: false })
  document.addEventListener('touchend', handleEnd)
}

function formatYearSeason(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  if (Number.isNaN(date.getTime())) return ''
  const month = date.getMonth() + 1
  const season = month <= 2 ? '冬季' : month <= 5 ? '春季' : month <= 8 ? '夏季' : month <= 11 ? '秋季' : '冬季'
  return `${date.getFullYear()}年${season}`
}

watch(() => props.modelValue, (newVal) => {
  const index = timelineItems.value.findIndex(item => item.id === newVal)
  if (index !== -1 && index !== selectedIndex.value) {
    selectedIndex.value = index
  }
})

onUnmounted(() => {
  stopPlay()
})
</script>

<style scoped>
.timeline-container {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.72) 0%, rgba(249, 248, 244, 0.88) 100%),
    linear-gradient(135deg, rgba(var(--theme-accent-rgb, 45, 89, 103), 0.06) 0%, rgba(var(--theme-accent-rgb, 45, 89, 103), 0.12) 100%);
  border: 1px solid rgba(var(--theme-accent-rgb, 45, 89, 103), 0.14);
  border-radius: 0;
  width: 100%;
  padding: 16px 18px;
  box-shadow: 0 18px 38px rgba(var(--theme-accent-rgb, 45, 89, 103), 0.08);
  margin: 0;
  user-select: none;
}

.timeline-container--vertical {
  height: auto;
  max-height: none;
  min-height: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
}

.timeline-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  color: var(--theme-text-primary, #142a31);
}

.timeline-container--vertical .timeline-header {
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
}

.timeline-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 0.02em;
  font-family: 'Cormorant Garamond', 'Songti SC', serif;
  color: var(--theme-accent-strong, #173843);
}

.current-time {
  display: flex;
  align-items: center;
  gap: 10px;
  background: rgba(var(--theme-accent-rgb, 45, 89, 103), 0.08);
  padding: 8px 14px;
  border-radius: 999px;
  backdrop-filter: blur(15px);
  border: 1px solid rgba(var(--theme-accent-rgb, 45, 89, 103), 0.14);
}

.time-label {
  font-size: 12px;
  color: var(--theme-text-secondary, rgba(20, 42, 49, 0.78));
  font-weight: 600;
}

.time-value {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.03em;
  color: var(--theme-accent-strong, #173843);
}

.timeline-wrapper {
  background: rgba(255, 255, 255, 0.56);
  border-radius: 0;
  padding: 12px 14px;
  backdrop-filter: blur(15px);
  border: 1px solid rgba(var(--theme-accent-rgb, 45, 89, 103), 0.12);
}

.timeline-container--vertical .timeline-wrapper {
  height: auto;
  max-height: none;
  min-height: 0;
  display: grid;
  grid-template-rows: minmax(220px, auto) auto auto;
  gap: 14px;
  overflow: visible;
  padding-right: 2px;
}

.timeline-track {
  position: relative;
  height: 150px;
  margin: 8px 0;
}

.timeline-container--vertical .timeline-track {
  width: 100%;
  height: 220px;
  min-height: 220px;
  margin: 0;
}

.timeline-axis {
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 6px;
  background: linear-gradient(to right, rgba(var(--theme-accent-rgb, 45, 89, 103), 0.12), rgba(var(--theme-accent-rgb, 45, 89, 103), 0.28), rgba(var(--theme-accent-rgb, 45, 89, 103), 0.12));
  border-radius: 3px;
  transform: translateY(-50%);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.timeline-container--vertical .timeline-axis {
  top: 0;
  bottom: 0;
  left: 50%;
  right: auto;
  width: 6px;
  height: auto;
  background: linear-gradient(to bottom, rgba(var(--theme-accent-rgb, 45, 89, 103), 0.12), rgba(var(--theme-accent-rgb, 45, 89, 103), 0.28), rgba(var(--theme-accent-rgb, 45, 89, 103), 0.12));
  transform: translateX(-50%);
}

.timeline-thumb {
  position: absolute;
  top: 50%;
  transform: translate(-50%, -50%);
  z-index: 4;
  display: flex;
  flex-direction: column;
  align-items: center;
  pointer-events: auto;
  cursor: grab;
}

.timeline-container--vertical .timeline-thumb {
  transform: translate(-50%, -50%);
}

.timeline-thumb:active {
  cursor: grabbing;
}

.thumb-stick {
  width: 3px;
  height: 24px;
  border-radius: 2px;
  background: rgba(var(--theme-accent-rgb, 45, 89, 103), 0.9);
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.18);
}

.timeline-container--vertical .thumb-stick {
  width: 24px;
  height: 3px;
}

.thumb-dot {
  width: 16px;
  height: 16px;
  margin-top: -1px;
  border-radius: 50%;
  background: #fff;
  border: 3px solid var(--theme-accent, #2d5967);
  box-shadow: 0 0 0 3px rgba(var(--theme-accent-rgb, 45, 89, 103), 0.16), 0 2px 8px rgba(15, 23, 42, 0.15);
}

.timeline-item {
  position: absolute;
  top: 50%;
  transform: translate(-50%, -50%);
  cursor: pointer;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 1;
}

.timeline-item:hover {
  transform: translate(-50%, -50%) scale(1.15);
  z-index: 5;
}

.item-marker {
  position: relative;
  width: 24px;
  height: 24px;
}

.marker-dot {
  width: 18px;
  height: 18px;
  background: rgba(255, 255, 255, 0.95);
  border: 3px solid rgba(var(--theme-accent-rgb, 45, 89, 103), 0.18);
  border-radius: 50%;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.12);
}

.timeline-item.hasWaterMask .marker-dot {
  background: rgba(var(--theme-accent-rgb, 45, 89, 103), 0.18);
  border-color: rgba(var(--theme-accent-rgb, 45, 89, 103), 0.48);
  box-shadow: 0 0 12px rgba(var(--theme-accent-rgb, 45, 89, 103), 0.18);
}

.timeline-item.active .marker-dot {
  width: 22px;
  height: 22px;
  background: rgba(255, 255, 255, 1);
  border-color: var(--theme-accent, #2d5967);
  box-shadow: 0 0 24px rgba(var(--theme-accent-rgb, 45, 89, 103), 0.24), 0 4px 12px rgba(15, 23, 42, 0.14);
}

.marker-pulse {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 40px;
  height: 40px;
  background: radial-gradient(circle, rgba(var(--theme-accent-rgb, 45, 89, 103), 0.28) 0%, transparent 70%);
  border-radius: 50%;
  transform: translate(-50%, -50%);
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    transform: translate(-50%, -50%) scale(1);
    opacity: 1;
  }
  50% {
    transform: translate(-50%, -50%) scale(1.8);
    opacity: 0;
  }
}

.item-tooltip {
  position: absolute;
  bottom: calc(100% + 15px);
  left: 50%;
  transform: translateX(-50%);
  background: white;
  color: #333;
  padding: 12px 16px;
  border-radius: 10px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.2);
  white-space: nowrap;
  opacity: 0;
  pointer-events: none;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 100;
}

.item-tooltip::after {
  content: '';
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  border: 8px solid transparent;
  border-top-color: white;
}

.timeline-item:hover .item-tooltip,
.timeline-item.active .item-tooltip {
  opacity: 1;
  pointer-events: auto;
  transform: translateX(-50%) translateY(-5px);
}

.tooltip-date {
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 6px;
  color: #11998e;
}

.tooltip-info {
  display: flex;
  gap: 8px;
  font-size: 12px;
}

.mask-badge,
.image-badge {
  padding: 3px 8px;
  border-radius: 6px;
  font-weight: 600;
}

.mask-badge {
  background: #e8f5f3;
  color: #11998e;
}

.image-badge {
  background: #f0fdf4;
  color: #38ef7d;
}

.item-label {
  position: absolute;
  top: calc(100% + 12px);
  left: 50%;
  transform: translateX(-50%);
  white-space: nowrap;
  font-size: 14px;
  font-weight: 600;
  color: var(--theme-text-primary, #142a31);
  text-shadow: none;
  pointer-events: none;
  opacity: 0.9;
}

.timeline-container--vertical .item-label {
  top: 50%;
  left: calc(100% + 18px);
  transform: translateY(-50%);
  white-space: normal;
  width: max-content;
  max-width: 120px;
}

.timeline-controls {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 8px;
}

.timeline-container--vertical .timeline-controls {
  flex-direction: column;
  margin-top: 0;
}

.control-btn {
  padding: 11px 22px;
  background: rgba(255, 255, 255, 0.82);
  color: var(--theme-accent-strong, #173843);
  border: 1px solid rgba(var(--theme-accent-rgb, 45, 89, 103), 0.18);
  border-radius: 999px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  backdrop-filter: blur(15px);
  letter-spacing: 0.08em;
}

.control-btn:hover:not(:disabled) {
  background: rgba(var(--theme-accent-rgb, 45, 89, 103), 0.09);
  border-color: rgba(var(--theme-accent-rgb, 45, 89, 103), 0.3);
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(var(--theme-accent-rgb, 45, 89, 103), 0.14);
}

.control-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  transform: none;
}

.play-btn.playing {
  background: rgba(var(--theme-accent-rgb, 45, 89, 103), 0.16);
  border-color: var(--theme-accent, #2d5967);
  animation: glow 2s ease-in-out infinite alternate;
}

@keyframes glow {
  from {
    box-shadow: 0 0 10px rgba(255, 215, 0, 0.3);
  }
  to {
    box-shadow: 0 0 20px rgba(255, 215, 0, 0.6);
  }
}

.timeline-legend {
  display: flex;
  justify-content: center;
  gap: 32px;
  margin-top: 6px;
  color: var(--theme-text-secondary, rgba(20, 42, 49, 0.78));
  font-size: 13px;
  font-weight: 600;
}

.timeline-container--vertical .timeline-legend {
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.legend-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
}

.legend-dot.active {
  background: var(--theme-accent, #2d5967);
  box-shadow: 0 0 10px rgba(var(--theme-accent-rgb, 45, 89, 103), 0.38);
}

.legend-dot.has-mask {
  background: rgba(var(--theme-accent-rgb, 45, 89, 103), 0.2);
  border: 2px solid rgba(var(--theme-accent-rgb, 45, 89, 103), 0.48);
}

.legend-dot.normal {
  background: white;
  border: 2px solid rgba(var(--theme-accent-rgb, 45, 89, 103), 0.22);
}

@media (max-width: 768px) {
  .timeline-container {
    padding: 8px 8px;
  }

  .timeline-container--vertical {
    min-height: auto;
  }
  
  .timeline-header {
    flex-direction: column;
    gap: 14px;
    align-items: flex-start;
  }
  
  .timeline-track {
    height: 132px;
    margin: 6px 0;
  }

  .timeline-container--vertical .timeline-track {
    min-height: 300px;
    height: 300px;
  }
  
  .timeline-wrapper {
    padding: 6px 8px;
  }
  
  .timeline-controls {
    flex-wrap: wrap;
    gap: 10px;
  }
  
  .control-btn {
    padding: 10px 20px;
    font-size: 13px;
  }
  
  .timeline-legend {
    flex-wrap: wrap;
    gap: 16px;
  }

  .timeline-container--vertical .timeline-legend {
    flex-wrap: nowrap;
    gap: 10px;
  }

  .item-label {
    font-size: 11px;
  }
}
</style>


