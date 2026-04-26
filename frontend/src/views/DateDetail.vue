<template>
  <div class="date-detail-page" :style="themeStyle">
    <div class="background-layer">
      <div class="background-image" :style="backgroundImageStyle"></div>
      <div class="background-overlay"></div>
    </div>

    <main class="page-shell">
      <div class="page-toolbar">
        <button class="back-link" type="button" @click="goBackToDetail">
          返回到湿地详情
        </button>
      </div>

      <div class="content-area">
        <aside class="timeline-side-column">
          <section class="panel-shell timeline-panel">
            <div class="timeline-control">
              <div class="control-row timeline-row" v-if="timelineItems.length > 0">
                <TimelineSlider
                  orientation="vertical"
                  :tiff-images="tiffImagesForTimeline"
                  v-model="selectedImageId"
                  @image-selected="handleImageSelected"
                  @time-changed="handleTimeChanged"
                />
              </div>
              <p v-else class="hint-text">请先在地图中选择湿地大致区域，再通过时间轴查看对应时期。</p>
            </div>
          </section>
        </aside>

        <div class="visualization-column">
          <section class="panel-shell map-panel">
            <div class="map-section">
              <MapComponent
                ref="mapRef"
                :image-bounds="selectedImageBounds"
                :active-waterbody-file="activeWaterbodyFile"
                :wetland-regions="imageRegions"
                v-model:selected-wetland-region="selectedImageRegion"
                :loading-wetland-regions="loadingLocalImages"
                :current-region-label="selectedRegionLabel"
                @refresh-local-images="loadLocalImages"
                @waterbody-period-change="handleWaterbodyPeriodChange"
                @error="handleError"
              />
            </div>
          </section>

          <section class="panel-shell">
            <DistributionMaps
              :active-period-key="activeTimelineKey"
              :selected-region="activeAnalysisRegion"
              :image-bounds="distributionProjectionBounds"
            />
          </section>

          <section class="panel-shell">
            <HistoricalChart :wetland-name="selectedWetlandName" />
          </section>
        </div>
      </div>
    </main>

    <div class="notification" :class="notificationType" v-if="notification">
      {{ notification }}
      <button @click="notification = ''" class="close-btn">&times;</button>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MapComponent from '../components/Water/MapComponent.vue'
import HistoricalChart from '../components/Water/HistoricalChart.vue'
import DistributionMaps from '../components/Water/DistributionMaps.vue'
import TimelineSlider from '../components/Water/TimelineSlider.vue'
import { boundsToGeoJSON, imageApi, waterbodyApi } from '../components/api'
import api from '../utils/api'

const route = useRoute()
const router = useRouter()

const CATEGORY_DEFS = {
  all: { accent: '#2d5967', accentStrong: '#173843', textPrimary: '#142a31', textSecondary: 'rgba(20, 42, 49, 0.78)', accentRgb: '45, 89, 103', navBg: 'rgba(230, 237, 240, 0.88)', navText: '#000000', navBorder: 'rgba(45, 89, 103, 0.18)', background: 'linear-gradient(180deg, #eef3f4 0%, #f7f4ee 100%)', backgroundImage: 'https://images.unsplash.com/photo-1500375592092-40eb2168fd21?auto=format&fit=crop&w=1800&q=80' },
  '内陆': { accent: '#4f7a47', accentStrong: '#294625', textPrimary: '#23381f', textSecondary: 'rgba(35, 56, 31, 0.78)', accentRgb: '79, 122, 71', navBg: 'rgba(230, 238, 226, 0.9)', navText: '#000000', navBorder: 'rgba(79, 122, 71, 0.18)', background: 'linear-gradient(180deg, #e5eee0 0%, #f6f2e8 100%)', backgroundImage: 'https://images.unsplash.com/photo-1473773508845-188df298d2d1?auto=format&fit=crop&w=1800&q=80' },
  '沿海': { accent: '#2e729e', accentStrong: '#1d4964', textPrimary: '#17394f', textSecondary: 'rgba(23, 57, 79, 0.78)', accentRgb: '46, 114, 158', navBg: 'rgba(226, 236, 244, 0.9)', navText: '#000000', navBorder: 'rgba(46, 114, 158, 0.18)', background: 'linear-gradient(180deg, #e2edf5 0%, #f3f6f8 100%)', backgroundImage: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1800&q=80' },
  '人造': { accent: '#ab6e31', accentStrong: '#6b441e', textPrimary: '#58361a', textSecondary: 'rgba(88, 54, 26, 0.8)', accentRgb: '171, 110, 49', navBg: 'rgba(241, 229, 216, 0.92)', navText: '#000000', navBorder: 'rgba(171, 110, 49, 0.18)', background: 'linear-gradient(180deg, #efe2d3 0%, #f7f1ea 100%)', backgroundImage: 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1800&q=80' }
}

const mapRef = ref(null)
const notification = ref('')
const notificationType = ref('info')
const wetland = ref(null)

const localImages = ref([])
const waterbodyFiles = ref([])
const selectedImagePath = ref('')
const selectedImageRegion = ref('')
const activeWaterbodyFile = ref('')
const activeWaterbodyBounds = ref(null)
let activeBoundsRequestSeq = 0
const selectedTimelineIndex = ref(-1)
const loadingLocalImages = ref(false)
const syncingFromTimeline = ref(false)
const selectedImageId = ref(null)

const normalizeCategories = (tags) => {
  const source = String(tags || '').split(/[、,，/|\s]+/).map((item) => item.trim()).filter(Boolean)
  const categories = []
  const addCategory = (value) => {
    if (CATEGORY_DEFS[value] && !categories.includes(value)) categories.push(value)
  }
  source.forEach((item) => {
    if (item.includes('内陆')) addCategory('内陆')
    if (item.includes('沿海')) addCategory('沿海')
    if (item.includes('人造')) addCategory('人造')
  })
  if (categories.length === 0) addCategory('内陆')
  return categories
}

const currentTheme = computed(() => CATEGORY_DEFS[wetland.value?.categories?.[0] || 'all'] || CATEGORY_DEFS.all)
const themeStyle = computed(() => ({
  '--theme-accent': currentTheme.value.accent,
  '--theme-accent-strong': currentTheme.value.accentStrong,
  '--theme-accent-rgb': currentTheme.value.accentRgb,
  '--theme-text-primary': currentTheme.value.textPrimary,
  '--theme-text-secondary': currentTheme.value.textSecondary,
  '--theme-background': currentTheme.value.background,
  '--nav-bg': currentTheme.value.navBg,
  '--nav-text': currentTheme.value.navText,
  '--nav-border': currentTheme.value.navBorder
}))
const backgroundImageStyle = computed(() => ({ backgroundImage: `url(${currentTheme.value.backgroundImage})` }))
const wetlandName = computed(() => wetland.value?.wetlandName || '湿地水体反演数据')

const selectedImageBounds = computed(() => {
  const image = localImages.value.find((item) => item.filePath === selectedImagePath.value)
  return image?.bounds || null
})

const distributionProjectionBounds = computed(() => {
  return activeWaterbodyBounds.value || selectedImageBounds.value
})

const activeAnalysisRegion = computed(() => {
  const bounds = distributionProjectionBounds.value
  if (!bounds) return null

  const minLon = Number(bounds.minLon)
  const maxLon = Number(bounds.maxLon)
  const minLat = Number(bounds.minLat)
  const maxLat = Number(bounds.maxLat)
  if (![minLon, maxLon, minLat, maxLat].every((value) => Number.isFinite(value))) return null
  if (maxLon <= minLon || maxLat <= minLat) return null

  return {
    regionId: null,
    name: selectedRegionLabel.value,
    bounds: { minLon, maxLon, minLat, maxLat },
    geometry: boundsToGeoJSON(minLon, maxLon, minLat, maxLat),
    temporary: true
  }
})

const activeTimelineKey = computed(() => {
  if (selectedTimelineIndex.value < 0 || selectedTimelineIndex.value >= timelineItems.value.length) {
    return ''
  }
  return timelineItems.value[selectedTimelineIndex.value]?.key || ''
})

const allTimelineItems = computed(() => {
  const byKey = new Map()
  const maskByPeriod = new Map()

  for (const maskFile of waterbodyFiles.value) {
    const periodKey = parsePeriodKey(maskFile || '')
    if (!periodKey) continue
    if (!maskByPeriod.has(periodKey)) {
      maskByPeriod.set(periodKey, maskFile)
    }
  }

  for (const image of localImages.value) {
    const periodKey = parsePeriodKey(image.fileName || image.filePath || '')
    if (!periodKey) continue
    const regionInfo = parseRegionInfo(image.filePath || image.fileName || '')
    const compositeKey = `${regionInfo.key}__${periodKey}`
    const existing = byKey.get(compositeKey) || {
      compositeKey,
      key: periodKey,
      regionKey: regionInfo.key,
      regionLabel: regionInfo.label,
      year: 0,
      seasonOrder: 99,
      image: null,
      maskFile: maskByPeriod.get(periodKey) || null
    }
    existing.image = image
    if (!existing.maskFile) {
      existing.maskFile = maskByPeriod.get(periodKey) || null
    }
    const parsed = parsePeriodMeta(periodKey)
    existing.year = parsed.year
    existing.seasonOrder = parsed.seasonOrder
    byKey.set(compositeKey, existing)
  }

  return Array.from(byKey.values())
    .filter((item) => item.image)
    .sort((a, b) => {
      const regionCompare = a.regionLabel.localeCompare(b.regionLabel, 'zh-CN')
      if (regionCompare !== 0) return regionCompare
      if (a.year !== b.year) return a.year - b.year
      if (a.seasonOrder !== b.seasonOrder) return a.seasonOrder - b.seasonOrder
      return a.key.localeCompare(b.key)
    })
    .map((item) => ({
      ...item,
      label: formatPeriodLabel(item.key)
    }))
})

const imageRegions = computed(() => {
  const seen = new Set()
  const rows = []
  for (const item of allTimelineItems.value) {
    if (seen.has(item.regionKey)) continue
    seen.add(item.regionKey)
    rows.push({ key: item.regionKey, label: item.regionLabel })
  }
  return rows
})

const timelineItems = computed(() => {
  if (!selectedImageRegion.value) return []
  return allTimelineItems.value.filter((item) => item.regionKey === selectedImageRegion.value)
})

const selectedTimelineLabel = computed(() => {
  if (selectedTimelineIndex.value < 0 || selectedTimelineIndex.value >= timelineItems.value.length) {
    return '未选择'
  }
  return timelineItems.value[selectedTimelineIndex.value].label
})

const selectedRegionLabel = computed(() => {
  const region = imageRegions.value.find((item) => item.key === selectedImageRegion.value)
  if (!region) return '未选择区域'
  return selectedTimelineLabel.value && selectedTimelineLabel.value !== '未选择'
    ? `${region.label} · ${selectedTimelineLabel.value}`
    : region.label
})

const selectedWetlandName = computed(() => {
  const region = imageRegions.value.find((item) => item.key === selectedImageRegion.value)
  return region?.label || wetlandName.value
})

const tiffImagesForTimeline = computed(() => {
  return timelineItems.value.map(item => {
    const parsedTime = parseDateTimeFromFileName(item.image?.fileName || '')
    return {
      id: item.image?.imageId || item.key,
      fileName: item.image?.fileName || '',
      acquisitionDate: parsedTime,
      hasWaterMask: !!item.maskFile,
      filePath: item.image?.filePath || '',
      image: item.image
    }
  })
})

function parseDateTimeFromFileName(fileName) {
  if (!fileName) return null

  const key = parsePeriodKey(fileName)
  if (!key) return null

  const [yearRaw, season] = key.split('-')
  const year = parseInt(yearRaw) || 2024

  const seasonMonthMap = {
    spring: 3,
    summer: 6,
    autumn: 9,
    winter: 12
  }

  const month = seasonMonthMap[season] || 1

  return `${year}-${String(month).padStart(2, '0')}-15T00:00:00`
}

onMounted(() => {
  loadWetlandDetail()
  loadLocalImages()
  loadWaterbodyFiles()
})

async function loadWetlandDetail() {
  try {
    const response = await api.get('/wetlands')
    const records = response.data?.wetlands || []
    const matched = records.find((item) => String(item.id) === String(route.params.id || ''))
    if (!matched) return
    wetland.value = {
      ...matched,
      categories: normalizeCategories(matched.tags)
    }
  } catch (error) {
    wetland.value = null
  }
}

async function loadLocalImages() {
  loadingLocalImages.value = true
  try {
    const response = await imageApi.getLocalImages()
    localImages.value = response.data || []
  } catch (error) {
    showNotification('加载本地影像列表失败', 'error')
  } finally {
    loadingLocalImages.value = false
  }
}

async function loadWaterbodyFiles() {
  try {
    const response = await waterbodyApi.getFiles()
    waterbodyFiles.value = response?.data?.success ? (response.data.data || []) : []
  } catch (error) {
    showNotification('加载水体掩膜列表失败', 'error')
  }
}

watch(
  imageRegions,
  (regions) => {
    if (regions.length === 0) {
      selectedImageRegion.value = ''
      return
    }
    if (!selectedImageRegion.value || !regions.some((item) => item.key === selectedImageRegion.value)) {
      selectedImageRegion.value = regions[0].key
    }
  },
  { immediate: true }
)

watch(selectedImageRegion, (regionKey) => {
  if (!regionKey) {
    selectedTimelineIndex.value = -1
    selectedImagePath.value = ''
    activeWaterbodyFile.value = ''
    return
  }

  if (timelineItems.value.length === 0) {
    selectedTimelineIndex.value = -1
    selectedImagePath.value = ''
    activeWaterbodyFile.value = ''
  }

  focusMapActiveArea(true)
})

watch(
  timelineItems,
  (items) => {
    if (items.length === 0) {
      selectedTimelineIndex.value = -1
      return
    }

    if (selectedTimelineIndex.value < 0 || selectedTimelineIndex.value >= items.length) {
      selectedTimelineIndex.value = 0
      return
    }

    applyTimelineSelection(items[selectedTimelineIndex.value])
    focusMapActiveArea(true)
  },
  { immediate: true }
)

watch(selectedTimelineIndex, (idx) => {
  const item = timelineItems.value[idx]
  if (!item) return
  applyTimelineSelection(item)
  focusMapActiveArea(true)
})

watch(selectedImagePath, (newPath) => {
  if (syncingFromTimeline.value) return
  if (!newPath || timelineItems.value.length === 0) return

  const idx = timelineItems.value.findIndex((item) => item.image?.filePath === newPath)
  if (idx >= 0) {
    syncingFromTimeline.value = true
    selectedTimelineIndex.value = idx
    activeWaterbodyFile.value = timelineItems.value[idx].maskFile || ''
    syncingFromTimeline.value = false
  }
})

watch(
  activeWaterbodyFile,
  async (file) => {
    const requestSeq = ++activeBoundsRequestSeq
    if (!file) {
      activeWaterbodyBounds.value = null
      return
    }

    try {
      const response = await waterbodyApi.getBounds(file)
      if (requestSeq !== activeBoundsRequestSeq) return

      const arr = response?.data?.success ? response?.data?.data : null
      if (!Array.isArray(arr) || arr.length < 4) {
        activeWaterbodyBounds.value = null
        return
      }

      const [minLon, minLat, maxLon, maxLat] = arr.map((v) => Number(v))
      if (![minLon, minLat, maxLon, maxLat].every((v) => Number.isFinite(v))) {
        activeWaterbodyBounds.value = null
        return
      }

      activeWaterbodyBounds.value = { minLon, maxLon, minLat, maxLat }
      focusMapActiveArea(true)
    } catch (error) {
      if (requestSeq !== activeBoundsRequestSeq) return
      activeWaterbodyBounds.value = null
    }
  },
  { immediate: true }
)

function applyTimelineSelection(item) {
  if (!item) return
  syncingFromTimeline.value = true
  if (item.image?.filePath) {
    selectedImagePath.value = item.image.filePath
  }
  if (item.maskFile) {
    activeWaterbodyFile.value = item.maskFile
  }
  syncingFromTimeline.value = false
}

function handleWaterbodyPeriodChange(file) {
  if (!file || syncingFromTimeline.value || timelineItems.value.length === 0) return
  activeWaterbodyFile.value = file
  const idx = timelineItems.value.findIndex((item) => item.maskFile === file)
  if (idx >= 0) {
    selectedTimelineIndex.value = idx
  }
}

function focusMapActiveArea(force = false) {
  nextTick(() => {
    window.setTimeout(() => {
      mapRef.value?.focusActiveArea?.(force)
    }, 0)
  })
}

function handleImageSelected(image) {
  if (!image) return
  syncingFromTimeline.value = true
  if (image.filePath) {
    selectedImagePath.value = image.filePath
  }
  const item = timelineItems.value.find(i => i.image?.filePath === image.filePath)
  if (item?.maskFile) {
    activeWaterbodyFile.value = item.maskFile
  }
  syncingFromTimeline.value = false
  focusMapActiveArea(true)
}

function handleTimeChanged(time) {
  console.log('时间变化:', time)
}

function parsePeriodKey(rawName) {
  const name = (rawName || '')
    .toString()
    .replace(/\.geojson$/i, '')
    .replace(/\.tiff?$/i, '')
  if (!name) return ''

  const lower = name.toLowerCase()
  let year = lower.match(/\d{4}/)?.[0]
  if (!year) {
    const shortYear = lower.match(/(^|[^\d])(\d{2})([^\d]|$)/)?.[2]
    if (shortYear) year = `20${shortYear}`
  }

  let season = ''
  if (lower.includes('spring') || name.includes('春')) season = 'spring'
  if (lower.includes('summer') || name.includes('夏')) season = 'summer'
  if (lower.includes('autumn') || lower.includes('fall') || name.includes('秋')) season = 'autumn'
  if (lower.includes('winter') || name.includes('冬')) season = 'winter'

  if (!year || !season) return ''
  return `${year}-${season}`
}

function parseRegionInfo(rawName) {
  const normalizedPath = (rawName || '')
    .toString()
    .replace(/\\/g, '/')
  const pathParts = normalizedPath.split('/').filter(Boolean)
  const fileBase = (pathParts[pathParts.length - 1] || '')
    .replace(/\.geojson$/i, '')
    .replace(/\.tiff?$/i, '')
    .replace(/\.png$/i, '')
  const parentName = pathParts.length >= 2 ? pathParts[pathParts.length - 2] : ''
  const base = parentName && !/local-images|uploads|tiff|results/i.test(parentName)
    ? `${parentName} ${fileBase}`
    : fileBase

  let label = base
    .replace(/\d{4}/g, ' ')
    .replace(/(^|[^0-9])(\d{2})([^0-9]|$)/g, '$1 $3')
    .replace(/spring|summer|autumn|fall|winter/gi, ' ')
    .replace(/[春夏秋冬](季)?/g, ' ')
    .replace(/天遥感|遥感|satellite|sentinel|landsat/gi, ' ')
    .replace(/叶绿素a|悬浮物|浓度分布图|影像|水体|掩膜|湿地|mask|geojson|tif|tiff/gi, ' ')
    .replace(/[_\-()【】\[\]]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()

  if (base.includes('红海滩') || label.includes('红海滩')) {
    label = '红海滩湿地'
  }
  if (!label) label = '红海滩湿地'
  return {
    key: label.toLowerCase(),
    label
  }
}

function parsePeriodMeta(key) {
  const [yearRaw, season] = key.split('-')
  const year = Number(yearRaw) || 0
  const seasonOrderMap = { spring: 1, summer: 2, autumn: 3, winter: 4 }
  return { year, seasonOrder: seasonOrderMap[season] || 99 }
}

function formatPeriodLabel(key) {
  const [yearRaw, season] = key.split('-')
  const labelMap = {
    spring: '春季',
    summer: '夏季',
    autumn: '秋季',
    winter: '冬季'
  }
  return `${yearRaw || '未知年份'} ${labelMap[season] || '未知季节'}`
}

function handleError(error) {
  if (error) {
    showNotification(error, 'error')
  }
}

function showNotification(message, type = 'info') {
  notification.value = message
  notificationType.value = type
  setTimeout(() => {
    notification.value = ''
  }, 5000)
}

function goBackToDetail() {
  if (route.params.id) {
    router.push({ name: 'Detail', params: { id: String(route.params.id) } })
    return
  }
  router.push({ name: 'Overview' })
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Cormorant+Garamond:wght@500;600;700&family=Manrope:wght@400;500;600;700&display=swap');

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.date-detail-page {
  --theme-accent: #2d5967;
  --theme-accent-strong: #173843;
  --theme-accent-rgb: 45, 89, 103;
  --theme-text-primary: #142a31;
  --theme-text-secondary: rgba(20, 42, 49, 0.78);
  --theme-background: linear-gradient(180deg, #eef3f4 0%, #f7f4ee 100%);
  min-height: 100vh;
  position: relative;
  background: var(--theme-background);
  color: var(--theme-text-primary);
  font-family: 'Manrope', 'PingFang SC', sans-serif;
}

.background-layer {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.background-image,
.background-overlay {
  position: absolute;
  inset: 0;
}

.background-image {
  background-position: center;
  background-size: cover;
  filter: saturate(0.74) contrast(1.02);
  transform: scale(1.04);
}

.background-overlay {
  background:
    linear-gradient(180deg, rgba(248, 246, 240, 0.74) 0%, rgba(246, 243, 236, 0.9) 26%, rgba(244, 240, 232, 0.96) 100%),
    linear-gradient(90deg, rgba(var(--theme-accent-rgb), 0.08) 0, rgba(var(--theme-accent-rgb), 0.08) 1px, transparent 1px, transparent 132px);
}

.page-shell {
  position: relative;
  z-index: 1;
  width: min(1240px, calc(100% - 40px));
  margin: 0 auto;
  padding: 168px 0 72px;
}

.page-toolbar {
  display: flex;
  justify-content: flex-start;
  margin-bottom: 20px;
}

.back-link {
  border: 1px solid rgba(var(--theme-accent-rgb), 0.16);
  background: rgba(255, 255, 255, 0.72);
  color: rgba(var(--theme-accent-rgb), 0.95);
  padding: 11px 16px;
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  cursor: pointer;
  transition: transform 0.24s ease, box-shadow 0.24s ease, background 0.24s ease;
}

.back-link:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(var(--theme-accent-rgb), 0.14);
  background: rgba(255, 255, 255, 0.9);
}

.content-area {
  display: grid;
  grid-template-columns: minmax(220px, 260px) minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}

.panel-shell {
  min-width: 0;
}

.timeline-side-column {
  position: sticky;
  top: 88px;
  align-self: start;
  max-height: calc(100vh - 104px);
}

.visualization-column {
  display: grid;
  gap: 20px;
  min-width: 0;
}

.timeline-control,
.map-section {
  min-width: 0;
}

.timeline-panel {
  min-height: auto;
  max-height: calc(100vh - 104px);
  overflow-y: auto;
  padding-right: 4px;
}

.timeline-control {
  min-height: 100%;
}

.map-section {
  overflow: hidden;
  min-height: 560px;
}

.hint-text {
  color: var(--theme-text-secondary);
  font-size: 0.95rem;
  line-height: 1.7;
}

.notification {
  position: fixed;
  top: 24px;
  right: 24px;
  padding: 15px 18px;
  border-radius: 10px;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.2);
  display: flex;
  align-items: center;
  gap: 10px;
  z-index: 9999;
  animation: slideIn 0.3s ease;
}

.notification.success {
  background: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.notification.error {
  background: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

.notification.info {
  background: #d1ecf1;
  color: #0c5460;
  border: 1px solid #bee5eb;
}

.close-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  opacity: 0.7;
  padding: 0 5px;
}

.close-btn:hover {
  opacity: 1;
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

@media (max-width: 960px) {
  .content-area {
    grid-template-columns: 1fr;
  }

  .timeline-side-column {
    position: static;
  }

  .timeline-panel {
    min-height: auto;
  }

  .map-section {
    min-height: 420px;
  }
}

@media (max-width: 720px) {
  .page-shell {
    width: calc(100% - 24px);
    padding: 156px 0 60px;
  }

  .page-toolbar {
    margin-bottom: 16px;
  }
}
</style>
