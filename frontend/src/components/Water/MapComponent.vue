<template>
  <div class="map-container">
    <div id="map" ref="mapContainer"></div>

    <div class="wetland-panel">
      <h4>湿地大致区域</h4>
      <select
        class="wetland-select"
        :value="selectedWetlandRegion"
        @change="emit('update:selectedWetlandRegion', $event.target.value)"
      >
        <option value="">请选择区域</option>
        <option
          v-for="item in wetlandRegions"
          :key="item.key"
          :value="item.key"
        >
          {{ item.label }}
        </option>
      </select>
      <button class="refresh-btn" @click="emit('refresh-local-images')" :disabled="loadingWetlandRegions">
        {{ loadingWetlandRegions ? '刷新中...' : '刷新列表' }}
      </button>
      <p class="panel-tip" v-if="currentRegionLabel && currentRegionLabel !== '未选择区域'">
        当前：{{ currentRegionLabel }}
      </p>
      <p class="panel-tip" v-else>
        选择本地湿地影像后，地图会自动跳转到对应位置。
      </p>
    </div>

    <div class="map-legend">
      <h4>图例</h4>
      <div class="legend-item">
        <span class="legend-line image-line"></span>
        <span>湿地大致区域</span>
      </div>
      <div class="legend-item">
        <span class="legend-line mask-line"></span>
        <span>水体掩膜</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref, watch } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { waterbodyApi } from '../api'

const props = defineProps({
  imageBounds: {
    type: Object,
    default: null
  },
  activeWaterbodyFile: {
    type: String,
    default: ''
  },
  wetlandRegions: {
    type: Array,
    default: () => []
  },
  selectedWetlandRegion: {
    type: String,
    default: ''
  },
  loadingWetlandRegions: {
    type: Boolean,
    default: false
  },
  currentRegionLabel: {
    type: String,
    default: ''
  }
})

const emit = defineEmits([
  'update:selectedWetlandRegion',
  'refresh-local-images',
  'waterbody-period-change',
  'error'
])

const mapContainer = ref(null)
const map = ref(null)
const waterbodyGeoJsonData = ref({})
const waterbodyBoundsMap = ref({})
const selectedWaterbodyFile = ref('')

let imageBoundsLayer = null
let waterbodyMaskLayer = null
let lastFocusedBoundsKey = ''

onMounted(() => {
  initMap()
  loadWaterbodyMasks()
})

onUnmounted(() => {
  if (!map.value) return

  if (imageBoundsLayer) {
    map.value.removeLayer(imageBoundsLayer)
    imageBoundsLayer = null
  }
  if (waterbodyMaskLayer) {
    map.value.removeLayer(waterbodyMaskLayer)
    waterbodyMaskLayer = null
  }
  map.value.remove()
})

function initMap() {
  map.value = L.map(mapContainer.value, {
    center: [40.84, 121.87],
    zoom: 11,
    crs: L.CRS.EPSG3857,
    minZoom: 8,
    maxZoom: 18
  })

  const tiandituImgLayer = L.tileLayer(
    'https://t{s}.tianditu.gov.cn/img_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=img&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=7e6e2bcd7664b111afd34dace51c4e3b',
    {
      subdomains: ['0', '1', '2', '3', '4', '5', '6', '7'],
      attribution: '&copy; 天地图',
      maxZoom: 18,
      zIndex: 0
    }
  )

  const tiandituCiaLayer = L.tileLayer(
    'https://t{s}.tianditu.gov.cn/cia_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=cia&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=7e6e2bcd7664b111afd34dace51c4e3b',
    {
      subdomains: ['0', '1', '2', '3', '4', '5', '6', '7'],
      attribution: '&copy; 天地图',
      maxZoom: 18,
      zIndex: 1
    }
  )

  const tiandituCombinedLayer = L.layerGroup([tiandituImgLayer, tiandituCiaLayer])
  const osmLayer = L.tileLayer(
    'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
    {
      subdomains: ['a', 'b', 'c'],
      attribution: '&copy; OpenStreetMap contributors',
      maxZoom: 19,
      zIndex: 0
    }
  )

  tiandituCombinedLayer.addTo(map.value)
  L.control.layers(
    {
      '天地图影像+标注(推荐)': tiandituCombinedLayer,
      OpenStreetMap: osmLayer
    },
    {},
    { position: 'topright' }
  ).addTo(map.value)
}

watch(
  () => props.imageBounds,
  (bounds) => {
    renderImageBounds(bounds)
  },
  { immediate: true, deep: true }
)

watch(
  () => props.activeWaterbodyFile,
  (file) => {
    if (!file || file === selectedWaterbodyFile.value) return
    if (waterbodyGeoJsonData.value[file]) {
      selectedWaterbodyFile.value = file
    }
  },
  { immediate: true }
)

watch(selectedWaterbodyFile, (file) => {
  renderWaterbodyMask()
  emit('waterbody-period-change', file || '')
})

function renderImageBounds(bounds) {
  if (!map.value) return
  if (imageBoundsLayer) {
    map.value.removeLayer(imageBoundsLayer)
    imageBoundsLayer = null
  }
  if (!bounds) return

  const minLon = Number(bounds.minLon)
  const maxLon = Number(bounds.maxLon)
  const minLat = Number(bounds.minLat)
  const maxLat = Number(bounds.maxLat)
  if (![minLon, maxLon, minLat, maxLat].every((value) => Number.isFinite(value))) return
  if (maxLon <= minLon || maxLat <= minLat) return

  const rectBounds = [[minLat, minLon], [maxLat, maxLon]]
  imageBoundsLayer = L.rectangle(rectBounds, {
    color: '#006dff',
    weight: 3,
    fill: false,
    dashArray: '8 6',
    interactive: false
  }).addTo(map.value)

  focusBounds(rectBounds)
}

async function loadWaterbodyMasks() {
  try {
    const response = await waterbodyApi.getAllGeoJson()
    if (!response?.data?.success) return

    const rawData = response.data.data || {}
    const parsed = {}
    for (const [file, value] of Object.entries(rawData)) {
      try {
        parsed[file] = typeof value === 'string' ? JSON.parse(value) : value
      } catch (e) {
        // Skip invalid GeoJSON from a single mask file without breaking the map.
      }
    }

    waterbodyGeoJsonData.value = parsed
    selectedWaterbodyFile.value = parsed[props.activeWaterbodyFile]
      ? props.activeWaterbodyFile
      : Object.keys(parsed)[0] || ''

    const boundsEntries = await Promise.all(
      Object.keys(parsed).map(async (file) => {
        try {
          const boundsResp = await waterbodyApi.getBounds(file)
          if (boundsResp?.data?.success && Array.isArray(boundsResp.data.data)) {
            const [minLon, minLat, maxLon, maxLat] = boundsResp.data.data
            return [file, [[minLat, minLon], [maxLat, maxLon]]]
          }
        } catch (e) {
          return null
        }
        return null
      })
    )

    waterbodyBoundsMap.value = Object.fromEntries(boundsEntries.filter(Boolean))
    renderWaterbodyMask()
    renderImageBounds(props.imageBounds)
  } catch (error) {
    emit('error', '加载水体掩膜失败')
  }
}

function renderWaterbodyMask() {
  if (!map.value) return
  if (waterbodyMaskLayer) {
    map.value.removeLayer(waterbodyMaskLayer)
    waterbodyMaskLayer = null
  }

  const geoJson = waterbodyGeoJsonData.value[selectedWaterbodyFile.value]
  if (!geoJson) return

  waterbodyMaskLayer = L.geoJSON(geoJson, {
    style: {
      color: '#00bcd4',
      weight: 1.5,
      fillColor: '#00bcd4',
      fillOpacity: 0.35
    }
  }).addTo(map.value)
}

function focusBounds(bounds, force = false) {
  if (!map.value || !bounds) return

  const key = JSON.stringify(bounds)
  if (!force && key === lastFocusedBoundsKey) return
  lastFocusedBoundsKey = key

  map.value.fitBounds(bounds, {
    padding: [40, 40],
    maxZoom: 13,
    animate: true,
    duration: 0.9
  })
}

function focusActiveArea(force = false) {
  const bounds = props.imageBounds
  if (!bounds) return

  const minLon = Number(bounds.minLon)
  const maxLon = Number(bounds.maxLon)
  const minLat = Number(bounds.minLat)
  const maxLat = Number(bounds.maxLat)
  if (![minLon, maxLon, minLat, maxLat].every((value) => Number.isFinite(value))) return
  focusBounds([[minLat, minLon], [maxLat, maxLon]], force)
}

defineExpose({
  focusActiveArea
})
</script>

<style scoped>
.map-container {
  position: relative;
  width: 100%;
  height: 100%;
  background: rgba(244, 241, 234, 0.88);
  border: 1px solid rgba(var(--theme-accent-rgb, 45, 89, 103), 0.12);
}

#map {
  width: 100%;
  height: 100%;
  min-height: 500px;
  background: rgba(244, 241, 234, 0.88);
}

:deep(.leaflet-container) {
  background: rgba(244, 241, 234, 0.88);
  font-family: 'Manrope', 'PingFang SC', sans-serif;
}

.wetland-panel {
  position: absolute;
  top: 18px;
  left: 18px;
  z-index: 1000;
  width: 320px;
  padding: 18px;
  border-radius: 0;
  border: 1px solid rgba(var(--theme-accent-rgb, 45, 89, 103), 0.16);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 16px 34px rgba(var(--theme-accent-rgb, 45, 89, 103), 0.12);
  backdrop-filter: blur(14px);
}

.wetland-panel h4 {
  margin: 0 0 12px;
  color: var(--theme-accent-strong, #173843);
  font-size: 22px;
  font-family: 'Cormorant Garamond', 'Songti SC', serif;
}

.wetland-select {
  width: 100%;
  border: 1px solid rgba(var(--theme-accent-rgb, 45, 89, 103), 0.16);
  border-radius: 0;
  padding: 12px 14px;
  font-size: 14px;
  color: var(--theme-text-primary, #142a31);
  background: rgba(255, 255, 255, 0.96);
}

.refresh-btn {
  width: 100%;
  margin-top: 12px;
  border: 1px solid rgba(var(--theme-accent-rgb, 45, 89, 103), 0.18);
  border-radius: 999px;
  padding: 11px 12px;
  background: rgba(var(--theme-accent-rgb, 45, 89, 103), 0.09);
  color: var(--theme-accent-strong, #173843);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  cursor: pointer;
}

.refresh-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.panel-tip {
  margin: 12px 0 0;
  color: var(--theme-text-secondary, rgba(20, 42, 49, 0.78));
  font-size: 13px;
  line-height: 1.7;
}

.map-legend {
  position: absolute;
  bottom: 20px;
  left: 18px;
  z-index: 1000;
  background: rgba(255, 255, 255, 0.92);
  padding: 14px 16px;
  border-radius: 0;
  border: 1px solid rgba(var(--theme-accent-rgb, 45, 89, 103), 0.14);
  box-shadow: 0 12px 24px rgba(var(--theme-accent-rgb, 45, 89, 103), 0.1);
  min-width: 156px;
  backdrop-filter: blur(12px);
}

.map-legend h4 {
  margin: 0 0 10px 0;
  font-size: 18px;
  color: var(--theme-accent-strong, #173843);
  border-bottom: 1px solid rgba(var(--theme-accent-rgb, 45, 89, 103), 0.12);
  padding-bottom: 10px;
  font-family: 'Cormorant Garamond', 'Songti SC', serif;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--theme-text-secondary, rgba(20, 42, 49, 0.78));
}

.legend-item:last-child {
  margin-bottom: 0;
}

.legend-line {
  width: 30px;
  height: 3px;
  border-radius: 2px;
}

.image-line {
  background: var(--theme-accent, #2d5967);
}

.mask-line {
  background: rgba(var(--theme-accent-rgb, 45, 89, 103), 0.55);
}

@media (max-width: 768px) {
  .wetland-panel {
    top: 10px;
    left: 10px;
    right: 10px;
    width: auto;
  }

  .map-legend {
    display: none;
  }
}
</style>


