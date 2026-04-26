<template>
  <div class="magazine-map-container">
    <div v-if="isLoading && !showGlobe" class="loading-overlay">
      <div class="loading-content">
        <div class="loading-spinner"></div>
        <p class="loading-text">LOADING</p>
      </div>
    </div>
    
    <Globe v-if="showGlobe" class="globe-view" @back="handleGlobeBack" />
    
    <template v-else>
      <div class="nav-bar" :class="{ visible: showContent }">
        <div class="nav-copy">
          <span class="title-kicker">SCREEN DASHBOARD</span>
          <h1 class="title">中国主要湿地分布概览</h1>
          <p class="title-meta">{{ activePanelLabel }}</p>
        </div>
      </div>

      <div v-if="mapError" class="map-error-banner">
        <strong>地图加载失败</strong>
        <span>{{ mapError }}</span>
      </div>
      
      <div class="panel-left" :class="{ visible: showContent && panelsVisible, 'exit-up': !panelsVisible && showContent }">
        <div
          v-for="(panelComponent, index) in leftPanelComponents"
          :key="`left-${activePanel}-${index}`"
          class="panel-item"
          :class="index === 0 ? 'panel-area1' : 'panel-area2'"
        >
          <div class="panel-content">
            <component :is="panelComponent" />
          </div>
        </div>
      </div>
      
      <div class="panel-right" :class="{ visible: showContent && panelsVisible, 'exit-up': !panelsVisible && showContent }">
        <div
          v-for="(panelComponent, index) in rightPanelComponents"
          :key="`right-${activePanel}-${index}`"
          class="panel-item"
          :class="index === 0 ? 'panel-rtop' : 'panel-rbottom'"
        >
          <div class="panel-content">
            <component :is="panelComponent" />
          </div>
        </div>
      </div>
      
      <div ref="mapContainer" class="map-canvas"></div>
      
    <div v-show="showWetlandData" class="wetland-legend" :class="{ show: showWetlandData }">
      <div class="legend-title">湿地面积分布</div>
      <div class="legend-gradient"></div>
      <div class="legend-labels">
        <span>0</span>
        <span>200</span>
        <span>400</span>
        <span>600</span>
        <span>800+</span>
      </div>
      <div class="legend-unit">单位：万公顷</div>
    </div>
    
    <a 
      v-show="showWetlandData"
      href="https://www.mnr.gov.cn" 
      target="_blank" 
      class="wetland-source"
      :class="{ show: showWetlandData }"
    >
      数据来源：第三次全国国土调查
    </a>

    <div class="bottom-arc" :class="{ visible: showContent || showGlobe }">
      <svg class="arc-svg" viewBox="0 0 1000 500" preserveAspectRatio="none">
        <defs>
          <linearGradient id="arcGradient" x1="0" y1="0" x2="0" y2="500" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stop-color="rgba(106, 96, 88, 0.05)" />
            <stop offset="40%" stop-color="rgba(106, 96, 88, 0.25)" />
            <stop offset="70%" stop-color="rgba(106, 96, 88, 0.45)" />
            <stop offset="100%" stop-color="rgba(106, 96, 88, 0.65)" />
          </linearGradient>
        </defs>
        <path d="M 280 480 Q 500 50 720 480" fill="none" stroke="url(#arcGradient)" stroke-width="12" stroke-linecap="round" />
      </svg>
      <div class="arc-icons">
        <div 
          class="arc-icon" 
          :class="{ active: showGlobe }"
          style="left: 35%; top: 112px;" 
          title="全球湿地"
          @click="toggleGlobe"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="12" cy="12" r="9" />
            <path d="M12 3a15 15 0 0 1 0 18 15 15 0 0 1 0-18z" />
            <path d="M3 12h18" />
          </svg>
        </div>
        <div 
          v-if="!showGlobe"
          class="arc-icon panel-toggle-btn" 
          :class="{ active: !panelsVisible }"
          style="left: 45%; top: 68px;" 
          title="面板切换"
          @click="togglePanels"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M3 17l5-7 4 4 4-6 5 9" />
            <path d="M3 21h18" />
            <circle cx="7" cy="7" r="1.5" />
          </svg>
        </div>
        
        <div 
          v-if="!panelsVisible && showSubButtons && !showGlobe"
          class="sub-buttons-container"
          style="left: 45%; top: 68px;"
        >
          <div 
            v-for="i in 3" 
            :key="i"
            class="sub-button"
            :class="{ active: activePanel === i }"
            :style="getSubButtonStyle(i)"
            @click="selectPanel(i)"
          >
            {{ i }}
          </div>
        </div>
        
        <div 
          v-if="!showGlobe"
          class="arc-icon" 
          style="left: 55%; top: 68px;" 
          title="湿地分布" 
          :class="{ active: showWetlandData }" 
          @click="toggleWetlandData"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="12" cy="12" r="9" />
            <path d="M12 2a15 15 0 0 0 0 20 15 15 0 0 0 0-20z" />
            <path d="M2 12h20" />
            <circle cx="12" cy="12" r="4" />
          </svg>
        </div>
        <div 
          v-if="!showGlobe"
          class="arc-icon" 
          style="left: 65%; top: 112px;" 
          title="热力图"
          @click="toggleWetlandSpots"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="12" cy="12" r="9" />
            <circle cx="9" cy="9" r="2" />
            <circle cx="15" cy="14" r="3" />
            <circle cx="11" cy="17" r="1.5" />
          </svg>
        </div>
      </div>
    </div>
    </template>
    <!-- 湿地景区弹窗 -->
    <div v-if="spotPopupVisible" class="spot-popup" @click.stop :style="{ top: spotPopupTop + 'px', left: spotPopupLeft + 'px' }">
      <div class="spot-popup-content">
        <button class="spot-popup-close" @click="spotPopupVisible = false">×</button>
        <div class="spot-popup-image-container">
          <img :src="spotPopupImage" :alt="spotPopupTitle" class="spot-popup-image" />
        </div>
        <div class="spot-popup-info">
          <h3 class="spot-popup-title">{{ spotPopupTitle }}</h3>
          <p class="spot-popup-description">{{ spotPopupDescription }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, markRaw, ref, onMounted, onUnmounted } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { Reflector } from 'three/examples/jsm/objects/Reflector.js'
import { Line2 } from 'three/examples/jsm/lines/Line2.js'
import { LineMaterial } from 'three/examples/jsm/lines/LineMaterial.js'
import { LineGeometry } from 'three/examples/jsm/lines/LineGeometry.js'
import { screenApi } from '../api'
import WetlandOverviewCard from './WetlandOverviewCard.vue'
import WetlandRoseChart from './WetlandRoseChart.vue'
import WetlandSunburst from './WetlandSunburst.vue'
import WetlandTrendChart from './WetlandTrendChart.vue'
import WetlandProtectionBar from './WetlandProtectionBar.vue'
import WetlandSpeciesBar from './WetlandSpeciesBar.vue'
import WetlandSpeciesTrend from './WetlandSpeciesTrend.vue'
import WetlandTouristArea from './WetlandTouristArea.vue'
import EcosystemRadar from './EcosystemRadar.vue'
import SoilCarbonPie from './SoilCarbonPie.vue'
import VegetationCarbonArea from './VegetationCarbonArea.vue'
import WaterConservationLiquid from './WaterConservationLiquid.vue'
import Globe from './Globe.vue'
import { getFallbackScreenDataset, loadScreenDataset, loadWetlandSpots as loadScreenWetlandSpots } from './screenData'

const mapContainer = ref(null)
const isLoading = ref(true)
const showContent = ref(false)
const panelsVisible = ref(true)
const showSubButtons = ref(false)
const activePanel = ref(1)
const showWetlandData = ref(false)
const hoveredProvince = ref(null)
const showGlobe = ref(false)
const showWetlandSpots = ref(false)
const wetlandSpots = ref([])
const selectedSpot = ref(null)
const mapError = ref('')
const spotMarkers = ref([])
const spotPopupVisible = ref(false)
const spotPopupImage = ref('')
const spotPopupTitle = ref('')
const spotPopupDescription = ref('')
const spotPopupTop = ref(0)
const spotPopupLeft = ref(0)

let scene, camera, renderer, controls
let animationId
let chinaGroup
let raycaster
let mouse
let provinceMeshes = []
let selectedProvince = null
let activeAnimations = new Set()
let originalMaterials = new Map()
let lightPillars = []
let pillarGrowthAnimations = []
let mapCenterX = 0
let mapCenterY = 0
let mapScale = 1
let billboardSprite = null
let billboardGroup = null
let spotMarkersGroup = null
let spotSprites = []
let spotMarkerGeometry = null
let spotMarkerMaterial = null
let useLiteRendering = false
const screenDataset = ref(getFallbackScreenDataset())
const PANEL_COMPONENTS = {
  1: {
    label: '湿地总览与全国分布',
    left: [markRaw(WetlandOverviewCard), markRaw(WetlandRoseChart)],
    right: [markRaw(WetlandSunburst), markRaw(WetlandTrendChart)]
  },
  2: {
    label: '保护修复与物种趋势',
    left: [markRaw(WetlandProtectionBar), markRaw(WetlandSpeciesBar)],
    right: [markRaw(WetlandSpeciesTrend), markRaw(WetlandTouristArea)]
  },
  3: {
    label: '生态服务与碳汇能力',
    left: [markRaw(EcosystemRadar), markRaw(SoilCarbonPie)],
    right: [markRaw(VegetationCarbonArea), markRaw(WaterConservationLiquid)]
  }
}

const activePanelConfig = computed(() => PANEL_COMPONENTS[activePanel.value] || PANEL_COMPONENTS[1])
const leftPanelComponents = computed(() => activePanelConfig.value.left)
const rightPanelComponents = computed(() => activePanelConfig.value.right)
const activePanelLabel = computed(() => activePanelConfig.value.label)

const getScreenNavSafeTop = () => {
  const screenPage = document.querySelector('.screen-page')
  if (!screenPage) return 208

  const rawValue = getComputedStyle(screenPage).getPropertyValue('--screen-nav-safe-top').trim()
  const parsedValue = Number.parseFloat(rawValue)
  return Number.isFinite(parsedValue) ? parsedValue : 208
}

const getProvinceWetlandData = (provinceName) => screenDataset.value.provinceStats?.[provinceName] || null
const getTotalWetlandArea = () => screenDataset.value.overview?.totalArea || 5635

const getWetlandColor = (area) => {
  const maxArea = 814
  const minArea = 0.05
  const ratio = Math.sqrt((area - minArea) / (maxArea - minArea))
  
  const r1 = 230, g1 = 235, b1 = 235
  const r2 = 120, g2 = 145, b2 = 150
  
  const r = Math.round(r1 + (r2 - r1) * ratio)
  const g = Math.round(g1 + (g2 - g1) * ratio)
  const b = Math.round(b1 + (b2 - b1) * ratio)
  
  return (r << 16) | (g << 8) | b
}

const togglePanels = () => {
  if (panelsVisible.value) {
    panelsVisible.value = false
    setTimeout(() => {
      showSubButtons.value = true
    }, 400)
  } else {
    showSubButtons.value = false
    panelsVisible.value = true
  }
}

const selectPanel = (panelIndex) => {
  activePanel.value = panelIndex
  showSubButtons.value = false
  panelsVisible.value = true
}

const toggleGlobe = () => {
  showGlobe.value = !showGlobe.value
  
  if (showGlobe.value) {
    showWetlandData.value = false
    panelsVisible.value = false
    showSubButtons.value = false
    
    const container = mapContainer.value
    if (container) {
      container.style.display = 'none'
    }
    
    disposeThreeResources()
    
    setTimeout(() => {
      if (window.gc) {
        window.gc()
      }
    }, 500)
  } else {
    const container = mapContainer.value
    if (container) {
      container.style.display = 'block'
    }
    
    isLoading.value = true
    setTimeout(() => {
      initThree()
      isLoading.value = false
    }, 100)
  }
}

const handleGlobeBack = () => {
  showGlobe.value = false
  
  const container = mapContainer.value
  if (container) {
    container.style.display = 'block'
  }
  
  isLoading.value = true
  setTimeout(() => {
    initThree()
    isLoading.value = false
  }, 100)
}

const toggleWetlandData = () => {
  showWetlandData.value = !showWetlandData.value
  
  if (showWetlandData.value) {
    panelsVisible.value = false
    showSubButtons.value = false
    applyWetlandColors()
  } else {
    restoreOriginalColors()
    if (selectedProvince) {
      selectedProvince.userData.targetDepth = 2.5
      activeAnimations.add(selectedProvince)
      selectedProvince = null
    }
  }
}

const toggleWetlandSpots = async () => {
  showWetlandSpots.value = !showWetlandSpots.value
  
  if (showWetlandSpots.value) {
    panelsVisible.value = false
    showSubButtons.value = false
    await loadWetlandSpots()
    createSpotMarkers()
  } else {
    removeSpotMarkers()
  }
}

const loadWetlandSpots = async () => {
  try {
    wetlandSpots.value = await loadScreenWetlandSpots()
    console.log(`成功加载 ${wetlandSpots.value.length} 个湿地景区`)
  } catch (error) {
    console.error('加载湿地景区数据失败:', error)
    wetlandSpots.value = screenDataset.value.wetlandSpots || []
  }
}

const projectGeoPointToScene = (lng, lat) => {
  if (!Number.isFinite(Number(lng)) || !Number.isFinite(Number(lat)) || !Number.isFinite(mapScale)) {
    return null
  }

  return {
    x: (Number(lng) - mapCenterX) * mapScale,
    z: -(Number(lat) - mapCenterY) * mapScale
  }
}

const createSpotMarkers = () => {
  if (!scene || !camera) return
  
  removeSpotMarkers()
  
  spotMarkersGroup = new THREE.Group()
  scene.add(spotMarkersGroup)
  spotMarkerGeometry = new THREE.SphereGeometry(1.35, 10, 10)
  spotMarkerMaterial = new THREE.MeshBasicMaterial({ color: 0x4a90e2 })
  
  // 计算东南到西北的显示顺序
  const spotsWithOrder = wetlandSpots.value
    .map((spot) => {
      const projected = projectGeoPointToScene(spot.lng, spot.lat)
      if (!projected) return null
      return {
        ...spot,
        projected,
        southeastScore: (35 - Number(spot.lat)) + (Number(spot.lng) - 105)
      }
    })
    .filter(Boolean)
    .sort((a, b) => b.southeastScore - a.southeastScore)
  
  // 创建标记点，初始缩放为0
  spotsWithOrder.forEach((spot, index) => {
    // 创建小圆点标记
    const marker = new THREE.Mesh(spotMarkerGeometry, spotMarkerMaterial)
    marker.position.set(spot.projected.x, 5, spot.projected.z)
    marker.scale.set(0, 0, 0) // 初始隐藏
    marker.userData = { spot }
    
    spotMarkersGroup.add(marker)
    spotSprites.push(marker)
    
    // 延迟显示动画（东南到西北顺序）
    setTimeout(() => {
      // 使用GSAP-like简单动画
      let startTime = null
      const duration = 800 // 毫秒
      const animateScale = (timestamp) => {
        if (!startTime) startTime = timestamp
        const elapsed = timestamp - startTime
        const progress = Math.min(elapsed / duration, 1)
        // 缓动函数
        const ease = 1 - Math.pow(1 - progress, 3)
        const scale = ease
        marker.scale.set(scale, scale, scale)
        
        if (progress < 1) {
          requestAnimationFrame(animateScale)
        }
      }
      requestAnimationFrame(animateScale)
    }, index * 40)
  })
}

const removeSpotMarkers = () => {
  if (spotMarkersGroup) {
    scene.remove(spotMarkersGroup)
    spotMarkersGroup = null
  }
  spotSprites = []
  if (spotMarkerGeometry) {
    spotMarkerGeometry.dispose()
    spotMarkerGeometry = null
  }
  if (spotMarkerMaterial) {
    spotMarkerMaterial.dispose()
    spotMarkerMaterial = null
  }
}

const applyWetlandColors = () => {
  provinceMeshes.forEach(mesh => {
    const provinceName = mesh.userData.provinceName
    const data = getProvinceWetlandData(provinceName)
    
    if (!originalMaterials.has(mesh)) {
      originalMaterials.set(mesh, mesh.material.clone())
    }
    
    if (data) {
      const color = getWetlandColor(data.area)
      mesh.material = new THREE.MeshStandardMaterial({
        color: color,
        metalness: 0.05,
        roughness: 0.85,
        transparent: false,
        opacity: 1.0
      })
      mesh.userData.wetlandData = data
    } else {
      mesh.material = new THREE.MeshStandardMaterial({
        color: 0xe0e5f0,
        metalness: 0.05,
        roughness: 0.85,
        transparent: false,
        opacity: 1.0
      })
      mesh.userData.wetlandData = null
    }
  })
  
  createLightPillars()
}

const restoreOriginalColors = () => {
  provinceMeshes.forEach(mesh => {
    const originalMaterial = originalMaterials.get(mesh)
    if (originalMaterial) {
      mesh.material = originalMaterial.clone()
    }
    mesh.userData.wetlandData = null
  })
  
  removeLightPillars()
  removeBillboard()
}

const createNumberLabel = (number, height) => {
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')
  canvas.width = 384
  canvas.height = 192
  
  ctx.fillStyle = 'rgba(255, 255, 255, 0.95)'
  ctx.beginPath()
  ctx.roundRect(14, 14, 356, 164, 18)
  ctx.fill()
  
  ctx.strokeStyle = 'rgba(106, 138, 144, 0.6)'
  ctx.lineWidth = 4
  ctx.stroke()
  
  ctx.fillStyle = '#2f4540'
  ctx.font = '700 64px "Microsoft YaHei", "PingFang SC", sans-serif'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText(`${number}`, 192, 96)
  
  const texture = new THREE.CanvasTexture(canvas)
  texture.needsUpdate = true
  texture.colorSpace = THREE.SRGBColorSpace
  
  const spriteMaterial = new THREE.SpriteMaterial({
    map: texture,
    transparent: true,
    opacity: 0
  })
  
  const sprite = new THREE.Sprite(spriteMaterial)
  sprite.scale.set(12, 6, 1)
  sprite.position.y = height + 8
  sprite.type = 'TransformControlsPlane'
  
  return sprite
}

const createLightPillars = () => {
  removeLightPillars()
  pillarGrowthAnimations = []
  
  const maxArea = 814
  const minArea = 0.05
  const maxHeight = 55
  const minHeight = 18
  
  const processedProvinces = new Set()
  
  provinceMeshes.forEach(mesh => {
    const data = mesh.userData.wetlandData
    if (!data) return
    
    const provinceName = mesh.userData.provinceName
    if (processedProvinces.has(provinceName)) return
    processedProvinces.add(provinceName)
    
    const bbox = new THREE.Box3().setFromObject(mesh)
    const center = new THREE.Vector3()
    bbox.getCenter(center)
    
    if (provinceName === '内蒙古自治区') {
      center.z += 30
    } else if (provinceName === '甘肃省') {
      center.x += 8
      center.z -= 5
    } else if (provinceName === '陕西省') {
      center.x += 8
      center.z += 4
    }
    
    const ratio = Math.sqrt((data.area - minArea) / (maxArea - minArea))
    const targetHeight = minHeight + (maxHeight - minHeight) * ratio
    const normalizedHeight = ratio
    const radius = 2.5
    
    const pillarGroup = new THREE.Group()
    
    const geometry = new THREE.CylinderGeometry(radius, radius, 0.1, 32, 32, false)
    
    const colors = []
    const positionAttribute = geometry.attributes.position
    
    const intensityFactor = normalizedHeight
    
    const bottomR = 0.75 - intensityFactor * 0.15
    const bottomG = 0.85 - intensityFactor * 0.2
    const bottomB = 0.95 - intensityFactor * 0.1
    
    const topR = 0.25 + intensityFactor * 0.15
    const topG = 0.35 + intensityFactor * 0.25
    const topB = 0.65 + intensityFactor * 0.2
    
    for (let i = 0; i < positionAttribute.count; i++) {
      const y = positionAttribute.getY(i)
      const normalizedY = (y + 0.05) / 0.1
      
      const bottomColor = new THREE.Color(bottomR, bottomG, bottomB)
      const topColor = new THREE.Color(topR, topG, topB)
      
      const mixedColor = bottomColor.clone().lerp(topColor, normalizedY)
      
      colors.push(mixedColor.r, mixedColor.g, mixedColor.b)
    }
    
    geometry.setAttribute('color', new THREE.Float32BufferAttribute(colors, 3))
    
    const cylinderMaterial = new THREE.MeshPhongMaterial({
      vertexColors: true,
      transparent: true,
      opacity: 1.0,
      shininess: 80,
      specular: new THREE.Color(0x667788)
    })
    cylinderMaterial.userData = { baseOpacity: 1.0 }
    
    const cylinder = new THREE.Mesh(geometry, cylinderMaterial)
    cylinder.position.y = 0.05
    pillarGroup.add(cylinder)
    
    const ringConfigs = [
      { innerRadius: radius + 0.2, outerRadius: radius + 1.2, baseOpacity: 0.7 },
      { innerRadius: radius + 1.2, outerRadius: radius + 2.2, baseOpacity: 0.5 },
      { innerRadius: radius + 2.2, outerRadius: radius + 3.3, baseOpacity: 0.35 },
      { innerRadius: radius + 3.3, outerRadius: radius + 4.6, baseOpacity: 0.25 }
    ]
    
    ringConfigs.forEach((config) => {
      const ringGeometry = new THREE.RingGeometry(config.innerRadius, config.outerRadius, 64)
      const ringMaterial = new THREE.MeshBasicMaterial({
        color: new THREE.Color(0x6A8A90),
        transparent: true,
        opacity: config.baseOpacity,
        side: THREE.DoubleSide
      })
      ringMaterial.userData = { baseOpacity: config.baseOpacity, isRing: true }
      const ring = new THREE.Mesh(ringGeometry, ringMaterial)
      ring.rotation.x = -Math.PI / 2
      ring.position.y = 5
      pillarGroup.add(ring)
    })
    
    const numberLabel = createNumberLabel(data.area.toFixed(1), targetHeight)
    pillarGroup.add(numberLabel)
    
    pillarGroup.position.set(center.x, 0, center.z)
    pillarGroup.userData = {
      provinceName: provinceName,
      wetlandData: data,
      targetMesh: mesh,
      baseHeight: targetHeight,
      baseY: 0,
      currentOpacity: 0.6,
      targetOpacity: 0.6,
      floatOffset: Math.random() * Math.PI * 2,
      pulseOffset: Math.random() * Math.PI * 2,
      ringScale: 1,
      ringPhase: Math.random() * Math.PI * 2,
      numberLabel: numberLabel
    }
    
    scene.add(pillarGroup)
    lightPillars.push(pillarGroup)
    
    pillarGrowthAnimations.push({
      pillarGroup,
      cylinder,
      radius,
      normalizedHeight,
      currentHeight: 0.1,
      targetHeight,
      growthSpeed: targetHeight * 0.015,
      isComplete: false,
      bottomR,
      bottomG,
      bottomB,
      topR,
      topG,
      topB
    })
  })
}

const removeLightPillars = () => {
  lightPillars.forEach(pillar => {
    scene.remove(pillar)
    pillar.children.forEach(child => {
      if (child.geometry) child.geometry.dispose()
      if (child.material) child.material.dispose()
    })
  })
  lightPillars = []
}

const createBillboard = (mesh, data) => {
  removeBillboard()
  
  const bbox = new THREE.Box3().setFromObject(mesh)
  const center = new THREE.Vector3()
  bbox.getCenter(center)
  
  billboardGroup = new THREE.Group()
  
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')
  canvas.width = 512
  canvas.height = 400
  
  ctx.fillStyle = 'rgba(240, 245, 245, 0.95)'
  roundRect(ctx, 0, 0, 512, 400, 24)
  ctx.fill()
  
  ctx.strokeStyle = 'rgba(106, 138, 144, 0.5)'
  ctx.lineWidth = 4
  roundRect(ctx, 0, 0, 512, 400, 24)
  ctx.stroke()
  
  ctx.fillStyle = '#2f4540'
  ctx.font = '700 42px "Microsoft YaHei", "PingFang SC", sans-serif'
  ctx.textAlign = 'center'
  ctx.fillText(mesh.userData.provinceName, 256, 64)
  
  ctx.strokeStyle = 'rgba(106, 138, 144, 0.35)'
  ctx.lineWidth = 2
  ctx.beginPath()
  ctx.moveTo(40, 90)
  ctx.lineTo(472, 90)
  ctx.stroke()
  
  ctx.fillStyle = 'rgba(58, 74, 74, 0.85)'
  ctx.font = '26px "Microsoft YaHei", "PingFang SC", sans-serif'
  ctx.textAlign = 'left'
  
  const nationalRatio = ((data.area / getTotalWetlandArea()) * 100).toFixed(2)
  const provincialRatio = ((data.area / data.landArea) * 100).toFixed(2)
  
  ctx.fillText('湿地面积', 40, 140)
  ctx.fillStyle = '#233632'
  ctx.textAlign = 'right'
  ctx.fillText(`${data.area} 万公顷`, 472, 140)
  
  ctx.fillStyle = 'rgba(58, 74, 74, 0.85)'
  ctx.textAlign = 'left'
  ctx.fillText('全国排名', 40, 190)
  ctx.fillStyle = '#233632'
  ctx.textAlign = 'right'
  ctx.fillText(`第 ${data.rank} 位`, 472, 190)
  
  ctx.fillStyle = 'rgba(58, 74, 74, 0.85)'
  ctx.textAlign = 'left'
  ctx.fillText('占全国比例', 40, 240)
  ctx.fillStyle = '#233632'
  ctx.textAlign = 'right'
  ctx.fillText(`${nationalRatio}%`, 472, 240)
  
  ctx.fillStyle = 'rgba(58, 74, 74, 0.85)'
  ctx.textAlign = 'left'
  ctx.fillText('占省面积比例', 40, 290)
  ctx.fillStyle = '#233632'
  ctx.textAlign = 'right'
  ctx.fillText(`${provincialRatio}%`, 472, 290)
  
  ctx.strokeStyle = 'rgba(106, 138, 144, 0.35)'
  ctx.lineWidth = 2
  ctx.beginPath()
  ctx.moveTo(40, 316)
  ctx.lineTo(472, 316)
  ctx.stroke()
  
  ctx.fillStyle = '#5a6a6a'
  ctx.font = '20px "Microsoft YaHei", "PingFang SC", sans-serif'
  ctx.textAlign = 'center'
  ctx.fillText(`数据来源：${data.sourceName}`, 256, 360)
  
  const texture = new THREE.CanvasTexture(canvas)
  texture.needsUpdate = true
  texture.colorSpace = THREE.SRGBColorSpace
  
  const spriteMaterial = new THREE.SpriteMaterial({
    map: texture,
    transparent: true,
    opacity: 0,
    depthTest: true
  })
  
  billboardSprite = new THREE.Sprite(spriteMaterial)
  billboardSprite.scale.set(40, 31.25, 1)
  billboardSprite.position.set(center.x, 70, center.z)
  billboardSprite.userData.targetOpacity = 1
  billboardSprite.userData.currentOpacity = 0
  
  billboardGroup.add(billboardSprite)
  scene.add(billboardGroup)
}

function roundRect(ctx, x, y, width, height, radius) {
  ctx.beginPath()
  ctx.moveTo(x + radius, y)
  ctx.lineTo(x + width - radius, y)
  ctx.quadraticCurveTo(x + width, y, x + width, y + radius)
  ctx.lineTo(x + width, y + height - radius)
  ctx.quadraticCurveTo(x + width, y + height, x + width - radius, y + height)
  ctx.lineTo(x + radius, y + height)
  ctx.quadraticCurveTo(x, y + height, x, y + height - radius)
  ctx.lineTo(x, y + radius)
  ctx.quadraticCurveTo(x, y, x + radius, y)
  ctx.closePath()
}

const removeBillboard = () => {
  if (billboardSprite) {
    if (billboardSprite.material.map) {
      billboardSprite.material.map.dispose()
    }
    billboardSprite.material.dispose()
    billboardSprite = null
  }
  if (billboardGroup) {
    scene.remove(billboardGroup)
    billboardGroup = null
  }
}

const getSubButtonStyle = (index) => {
  const angles = [-54, -26, 4]
  const distance = 132
  const angle = angles[index - 1] * (Math.PI / 180)
  const x = Math.sin(angle) * distance
  const y = -Math.cos(angle) * distance
  
  return {
    transform: `translate(calc(-50% + ${x}px), calc(-50% + ${y}px))`,
    transitionDelay: `${(index - 1) * 0.08}s`
  }
}

const shouldUseLiteRendering = () => {
  const prefersReducedMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches ?? false
  const hardwareConcurrency = navigator.hardwareConcurrency || 8

  return prefersReducedMotion
    || window.innerWidth < 1600
    || window.devicePixelRatio > 1.2
    || hardwareConcurrency <= 8
}

const initThree = () => {
  const container = mapContainer.value
  const width = container.clientWidth
  const height = container.clientHeight
  mapError.value = ''
  useLiteRendering = shouldUseLiteRendering()

  scene = new THREE.Scene()
  scene.background = new THREE.Color(0xe8e4df)
  
  const fogNear = 800
  const fogFar = 1500
  scene.fog = new THREE.Fog(0xe8e4df, fogNear, fogFar)

  camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 50000)
  camera.position.set(0, 550, 280)

  renderer = new THREE.WebGLRenderer({ 
    antialias: !useLiteRendering,
    alpha: true,
    powerPreference: 'high-performance'
  })
  renderer.setSize(width, height)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, useLiteRendering ? 1 : 1.75))
  renderer.shadowMap.enabled = false
  container.appendChild(renderer.domElement)

  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = !useLiteRendering
  controls.dampingFactor = 0.05
  controls.rotateSpeed = 0.3
  controls.minDistance = 150
  controls.maxDistance = 700
  controls.maxPolarAngle = Math.PI / 4
  controls.minPolarAngle = Math.PI / 6
  controls.minAzimuthAngle = -Math.PI / 6
  controls.maxAzimuthAngle = Math.PI / 6
  controls.enablePan = true
  controls.panSpeed = 0.8
  controls.target.set(0, 0, 0)
  controls.mouseButtons = {
    LEFT: THREE.MOUSE.PAN,
    MIDDLE: THREE.MOUSE.DOLLY,
    RIGHT: THREE.MOUSE.ROTATE
  }
  
  const panLimit = 250
  controls.addEventListener('change', () => {
    controls.target.x = Math.max(-panLimit, Math.min(panLimit, controls.target.x))
    controls.target.z = Math.max(-panLimit, Math.min(panLimit, controls.target.z))
  })

  raycaster = new THREE.Raycaster()
  mouse = new THREE.Vector2()
  
  container.addEventListener('click', onMouseClick)

  const ambientLight = new THREE.AmbientLight(0xf0ebe6, 0.7)
  scene.add(ambientLight)

  const mainLight = new THREE.DirectionalLight(0xf5f0ea, 1.2)
  mainLight.position.set(400, 600, 300)
  scene.add(mainLight)

  const fillLight = new THREE.DirectionalLight(0xe8e2dc, 0.4)
  fillLight.position.set(-300, 400, -200)
  scene.add(fillLight)

  const rimLight = new THREE.DirectionalLight(0xf0ebe6, 0.3)
  rimLight.position.set(0, 200, -400)
  scene.add(rimLight)

  createBasePlane()
  loadMapData()
  animate()
}

const createBasePlane = () => {
  const gridSize = 2000
  const cellSize = 80
  const gridCount = Math.floor(gridSize / cellSize)
  const halfGrid = gridSize / 2

  const baseGroup = new THREE.Group()

  if (useLiteRendering) {
    const basePlaneGeometry = new THREE.PlaneGeometry(gridSize * 2, gridSize * 2)
    const basePlaneMaterial = new THREE.MeshBasicMaterial({
      color: 0xded8d2,
      transparent: true,
      opacity: 0.85,
      side: THREE.DoubleSide
    })
    const basePlane = new THREE.Mesh(basePlaneGeometry, basePlaneMaterial)
    basePlane.rotation.x = -Math.PI / 2
    basePlane.position.y = -1
    baseGroup.add(basePlane)
  } else {
    const mirrorGeometry = new THREE.PlaneGeometry(gridSize * 2, gridSize * 2)
    const mirror = new Reflector(mirrorGeometry, {
      clipBias: 0.003,
      textureWidth: 256,
      textureHeight: 256,
      color: 0xddd9d4
    })
    mirror.rotation.x = -Math.PI / 2
    mirror.position.y = -1
    baseGroup.add(mirror)
  }

  const overlayGeometry = new THREE.PlaneGeometry(gridSize * 2, gridSize * 2)
  const overlayMaterial = new THREE.MeshBasicMaterial({
    color: 0xd2cec9,
    transparent: true,
    opacity: 0.2,
    side: THREE.DoubleSide,
    depthWrite: true
  })
  const overlay = new THREE.Mesh(overlayGeometry, overlayMaterial)
  overlay.rotation.x = -Math.PI / 2
  overlay.position.y = -0.5
  baseGroup.add(overlay)

  const crossPositions = []
  const crossSize = 5
  
  for (let i = 0; i <= gridCount; i++) {
    for (let j = 0; j <= gridCount; j++) {
      const posX = -halfGrid + j * cellSize
      const posZ = -halfGrid + i * cellSize
      crossPositions.push(
        posX - crossSize, 0.02, posZ,
        posX + crossSize, 0.02, posZ,
        posX, 0.02, posZ - crossSize,
        posX, 0.02, posZ + crossSize
      )
    }
  }

  const crossGeom = new THREE.BufferGeometry()
  crossGeom.setAttribute('position', new THREE.Float32BufferAttribute(crossPositions, 3))
  const crossMat = new THREE.LineBasicMaterial({ color: 0x9a9e9a, transparent: true, opacity: useLiteRendering ? 0.4 : 0.6 })
  const crossLines = new THREE.LineSegments(crossGeom, crossMat)
  baseGroup.add(crossLines)

  const dotMaterial = new THREE.PointsMaterial({
    color: 0x9a9e9a,
    size: 1.0,
    transparent: true,
    opacity: 0.4,
    sizeAttenuation: true
  })

  const dotsPerCell = useLiteRendering ? 2 : 4
  const dotSpacing = cellSize / (dotsPerCell + 1)
  const dotPositions = []

  for (let i = 0; i < gridCount; i++) {
    for (let j = 0; j < gridCount; j++) {
      const cellStartX = -halfGrid + i * cellSize
      const cellStartZ = -halfGrid + j * cellSize

      for (let di = 1; di <= dotsPerCell; di++) {
        for (let dj = 1; dj <= dotsPerCell; dj++) {
          const x = cellStartX + di * dotSpacing
          const z = cellStartZ + dj * dotSpacing
          dotPositions.push(x, 0.02, z)
        }
      }
    }
  }

  const dotGeometry = new THREE.BufferGeometry()
  dotGeometry.setAttribute('position', new THREE.Float32BufferAttribute(dotPositions, 3))
  const dots = new THREE.Points(dotGeometry, dotMaterial)
  baseGroup.add(dots)

  scene.add(baseGroup)
}

const loadMapData = async () => {
  try {
    let chinaData = null
    let lastError = null

    for (const url of screenApi.getChinaGeoJsonFallbackUrls()) {
      try {
        const chinaRes = await fetch(url)
        if (!chinaRes.ok) {
          throw new Error(`HTTP ${chinaRes.status} when loading ${url}`)
        }
        chinaData = await chinaRes.json()
        break
      } catch (error) {
        lastError = error
      }
    }

    if (!chinaData) {
      throw lastError || new Error('China GeoJSON unavailable')
    }

    chinaGroup = new THREE.Group()
    provinceMeshes = []

    const chinaBbox = calculateBoundingBox(chinaData)

    const chinaCenterX = (chinaBbox.minX + chinaBbox.maxX) / 2
    const chinaCenterY = (chinaBbox.minY + chinaBbox.maxY) / 2

    const targetSize = 650
    const scaleX = targetSize / (chinaBbox.maxX - chinaBbox.minX)
    const scaleY = targetSize / (chinaBbox.maxY - chinaBbox.minY)
    const scale = Math.min(scaleX, scaleY)
    
    mapCenterX = chinaCenterX
    mapCenterY = chinaCenterY
    mapScale = scale

    chinaData.features.forEach(feature => {
      const geometry = feature.geometry
      const properties = feature.properties || {}
      const provinceName = properties.name || '未知'
      const adcode = properties.adcode
      
      const isProvince = adcode && 
        adcode.toString().length === 6 && 
        parseInt(adcode) % 10000 === 0 &&
        !provinceName.includes('岛') &&
        !provinceName.includes('群岛') &&
        provinceName !== '十段线'
      
      if (geometry.type === 'Polygon') {
        createChinaPolygon(geometry.coordinates, scale, chinaCenterX, chinaCenterY, provinceName, isProvince)
      } else if (geometry.type === 'MultiPolygon') {
        geometry.coordinates.forEach(polygon => {
          createChinaPolygon(polygon, scale, chinaCenterX, chinaCenterY, provinceName, isProvince)
        })
      }
    })

    scene.add(chinaGroup)
    
    setTimeout(() => {
      isLoading.value = false
      setTimeout(() => {
        showContent.value = true
      }, 600)
    }, 1500)
  } catch (error) {
    console.error('Error loading map data:', error)
    mapError.value = '中国地图数据加载失败，请检查 GeoJSON 数据源或网络连接。'
    isLoading.value = false
    showContent.value = true
  }
}

const createChinaPolygon = (coordinates, scale, centerX, centerY, provinceName, isProvince) => {
  const outerRing = coordinates[0]
  
  const shape = new THREE.Shape()
  outerRing.forEach((coord, i) => {
    const x = (coord[0] - centerX) * scale
    const y = (coord[1] - centerY) * scale
    if (i === 0) {
      shape.moveTo(x, y)
    } else {
      shape.lineTo(x, y)
    }
  })
  shape.closePath()

  if (coordinates.length > 1) {
    for (let i = 1; i < coordinates.length; i++) {
      const hole = new THREE.Path()
      coordinates[i].forEach((coord, j) => {
        const x = (coord[0] - centerX) * scale
        const y = (coord[1] - centerY) * scale
        if (j === 0) {
          hole.moveTo(x, y)
        } else {
          hole.lineTo(x, y)
        }
      })
      hole.closePath()
      shape.holes.push(hole)
    }
  }

  const extrudeSettings = {
    depth: 2.5,
    bevelEnabled: true,
    bevelThickness: 0.3,
    bevelSize: 0.15,
    bevelSegments: 1
  }

  const geometry = new THREE.ExtrudeGeometry(shape, extrudeSettings)
  
  const material = new THREE.MeshStandardMaterial({
    color: 0xf0ede8,
    metalness: 0.02,
    roughness: 0.95,
    transparent: true,
    opacity: 0.98
  })

  const mesh = new THREE.Mesh(geometry, material)
  mesh.rotation.x = -Math.PI / 2
  mesh.position.y = 0
  mesh.userData = { 
    provinceName: provinceName,
    shape: shape,
    baseDepth: 2.5,
    currentDepth: 2.5,
    targetDepth: 2.5,
    isProvince: isProvince
  }
  chinaGroup.add(mesh)
  if (isProvince) {
    provinceMeshes.push(mesh)
  }

  const edgesGeometry = new THREE.EdgesGeometry(geometry)
  const edgesPositions = edgesGeometry.attributes.position.array
  
  const linePositions = []
  const edgeCount = edgesPositions.length / 6
  
  for (let i = 0; i < edgeCount; i++) {
    const idx = i * 6
    linePositions.push(
      edgesPositions[idx], edgesPositions[idx + 1] + 0.05, edgesPositions[idx + 2],
      edgesPositions[idx + 3], edgesPositions[idx + 4] + 0.05, edgesPositions[idx + 5]
    )
  }
  
  const lineGeometry = new LineGeometry()
  lineGeometry.setPositions(linePositions)
  
  const lineMaterial = new LineMaterial({
    color: 0x8a8580,
    linewidth: 2,
    transparent: true,
    opacity: 0.7,
    resolution: new THREE.Vector2(window.innerWidth, window.innerHeight)
  })
  
  const edges = new Line2(lineGeometry, lineMaterial)
  edges.rotation.x = -Math.PI / 2
  chinaGroup.add(edges)
  
  mesh.userData.edges = edges
}

const calculateBoundingBox = (geoJson) => {
  let minX = Infinity, maxX = -Infinity
  let minY = Infinity, maxY = -Infinity

  const processCoords = (coords) => {
    coords.forEach(coord => {
      if (typeof coord[0] === 'number') {
        minX = Math.min(minX, coord[0])
        maxX = Math.max(maxX, coord[0])
        minY = Math.min(minY, coord[1])
        maxY = Math.max(maxY, coord[1])
      } else {
        processCoords(coord)
      }
    })
  }

  geoJson.features.forEach(feature => {
    processCoords(feature.geometry.coordinates)
  })

  return { minX, maxX, minY, maxY }
}

const onMouseClick = (event) => {
  const container = mapContainer.value
  const rect = container.getBoundingClientRect()
  
  mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
  mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1
  
  raycaster.setFromCamera(mouse, camera)
  
  // 收集所有可交互对象
  const allIntersectableObjects = [...provinceMeshes]
  if (showWetlandSpots.value && spotSprites.length > 0) {
    allIntersectableObjects.push(...spotSprites)
  }
  
  const intersects = raycaster.intersectObjects(allIntersectableObjects)
  
  if (intersects.length > 0) {
    const clickedObject = intersects[0].object
    
    // 检查是否点击了标记点
    if (spotSprites.includes(clickedObject)) {
      const spot = clickedObject.userData.spot
      if (spot) {
        selectedSpot.value = spot
        spotPopupTitle.value = spot.name
        spotPopupDescription.value = spot.description
        spotPopupImage.value = spot.image
        
        // 计算标记点在屏幕上的位置
        const worldPosition = new THREE.Vector3()
        clickedObject.getWorldPosition(worldPosition)
        
        // 将3D世界坐标转换为标准化设备坐标（NDC）
        const ndc = worldPosition.clone().project(camera)
        
        // 将NDC转换为屏幕像素坐标
        const container = mapContainer.value
        const rect = container.getBoundingClientRect()
        
        const x = (ndc.x * 0.5 + 0.5) * rect.width + rect.left
        const y = (1 - (ndc.y * 0.5 + 0.5)) * rect.height + rect.top
        const navSafeTop = getScreenNavSafeTop()
        const popupWidth = window.innerWidth <= 768 ? 320 : 420
        const minLeft = popupWidth / 2 + 16
        const maxLeft = window.innerWidth - popupWidth / 2 - 16
        
        // 设置弹窗位置（在标记点正上方）
        spotPopupLeft.value = Math.min(Math.max(x, minLeft), Math.max(minLeft, maxLeft))
        spotPopupTop.value = Math.max(y - 24, navSafeTop + 28)
        
        spotPopupVisible.value = true
      }
      return
    }
    
    // 否则处理省份点击
    const clickedMesh = clickedObject
    
    if (showWetlandData.value) {
      const provinceName = clickedMesh.userData.provinceName
      const data = clickedMesh.userData.wetlandData
      
      if (selectedProvince === clickedMesh) {
        selectedProvince.userData.targetDepth = 2.5
        activeAnimations.add(selectedProvince)
        selectedProvince = null
        removeBillboard()
        showAllLightPillars()
      } else {
        if (selectedProvince) {
          selectedProvince.userData.targetDepth = 2.5
          activeAnimations.add(selectedProvince)
        }
        selectedProvince = clickedMesh
        selectedProvince.userData.targetDepth = 15
        activeAnimations.add(selectedProvince)
        
        hideLightPillarForProvince(provinceName)
        
        if (data) {
          createBillboard(clickedMesh, data)
          moveCameraToProvince(clickedMesh)
        }
      }
    } else {
      if (selectedProvince === clickedMesh) {
        selectedProvince.userData.targetDepth = 2.5
        activeAnimations.add(selectedProvince)
        selectedProvince = null
      } else {
        if (selectedProvince) {
          selectedProvince.userData.targetDepth = 2.5
          activeAnimations.add(selectedProvince)
        }
        selectedProvince = clickedMesh
        selectedProvince.userData.targetDepth = 12
        activeAnimations.add(selectedProvince)
        
        moveCameraToProvince(clickedMesh)
      }
    }
  } else {
    // 点击空白处关闭弹窗
    spotPopupVisible.value = false
    
    if (selectedProvince) {
      selectedProvince.userData.targetDepth = 2.5
      activeAnimations.add(selectedProvince)
      selectedProvince = null
    }
    if (showWetlandData.value) {
      removeBillboard()
      showAllLightPillars()
    }
  }
}

const hideLightPillarForProvince = (provinceName) => {
  lightPillars.forEach(pillar => {
    if (pillar.userData.provinceName === provinceName) {
      pillar.userData.targetOpacity = 0
      
      const numberLabel = pillar.userData.numberLabel
      if (numberLabel && numberLabel.material) {
        numberLabel.material.opacity = 0
      }
    }
  })
}

const showAllLightPillars = () => {
  lightPillars.forEach(pillar => {
    pillar.userData.targetOpacity = 1.0
    
    const numberLabel = pillar.userData.numberLabel
    if (numberLabel && numberLabel.material && pillarGrowthAnimations.some(anim => 
      anim.pillarGroup === pillar && anim.isComplete)) {
      numberLabel.material.opacity = 1
    }
  })
}

const moveCameraToProvince = (mesh) => {
  const bbox = new THREE.Box3().setFromObject(mesh)
  const center = new THREE.Vector3()
  bbox.getCenter(center)
  
  const size = new THREE.Vector3()
  bbox.getSize(size)
  const maxDim = Math.max(size.x, size.z)
  const distance = Math.max(maxDim * 2.5, 400)
  
  const targetX = center.x
  const targetZ = center.z
  const targetCameraY = distance * 0.5
  const targetCameraZ = distance * 0.4
  
  const startTargetX = controls.target.x
  const startTargetZ = controls.target.z
  const startCameraX = camera.position.x
  const startCameraZ = camera.position.z
  const startCameraY = camera.position.y
  
  const finalCameraX = targetX
  const finalCameraZ = targetZ + targetCameraZ
  
  controls.enabled = false
  
  let progress = 0
  
  const moveCamera = () => {
    progress += 0.03
    if (progress < 1) {
      const easeProgress = 1 - Math.pow(1 - progress, 3)
      controls.target.x = startTargetX + (targetX - startTargetX) * easeProgress
      controls.target.z = startTargetZ + (targetZ - startTargetZ) * easeProgress
      camera.position.x = startCameraX + (finalCameraX - startCameraX) * easeProgress
      camera.position.z = startCameraZ + (finalCameraZ - startCameraZ) * easeProgress
      camera.position.y = startCameraY + (targetCameraY - startCameraY) * easeProgress
      requestAnimationFrame(moveCamera)
    } else {
      const clampedX = Math.max(-250, Math.min(250, targetX))
      const clampedZ = Math.max(-250, Math.min(250, targetZ))
      controls.target.x = clampedX
      controls.target.z = clampedZ
      camera.position.x = clampedX
      camera.position.z = clampedZ + targetCameraZ
      camera.position.y = targetCameraY
      camera.lookAt(clampedX, 0, clampedZ)
      controls.update()
      controls.enabled = true
    }
  }
  moveCamera()
}

const animate = () => {
  animationId = requestAnimationFrame(animate)
  if (!pageVisible || !renderer || !scene || !camera) return
  controls.update()
  
  if (activeAnimations.size > 0) {
    activeAnimations.forEach(mesh => {
      const targetDepth = mesh.userData.targetDepth
      const currentDepth = mesh.userData.currentDepth
      
      if (Math.abs(targetDepth - currentDepth) > 0.1) {
        const newDepth = currentDepth + (targetDepth - currentDepth) * 0.12
        mesh.userData.currentDepth = newDepth
        const s = newDepth / mesh.userData.baseDepth
        mesh.scale.z = s
        mesh.userData.edges.scale.z = s
      } else {
        activeAnimations.delete(mesh)
      }
    })
  }
  
  pillarGrowthAnimations.forEach(anim => {
    if (!anim.isComplete) {
      anim.currentHeight += anim.growthSpeed
      
      if (anim.currentHeight >= anim.targetHeight) {
        anim.currentHeight = anim.targetHeight
        anim.isComplete = true
        
        const numberLabel = anim.pillarGroup.userData.numberLabel
        if (numberLabel && numberLabel.material) {
          numberLabel.material.opacity = 1
        }
      }
      
      const newGeometry = new THREE.CylinderGeometry(anim.radius, anim.radius, anim.currentHeight, 32, 32, false)
      
      const colors = []
      const positionAttribute = newGeometry.attributes.position
      
      for (let i = 0; i < positionAttribute.count; i++) {
        const y = positionAttribute.getY(i)
        const normalizedY = (y + anim.currentHeight / 2) / anim.currentHeight
        
        const bottomColor = new THREE.Color(anim.bottomR, anim.bottomG, anim.bottomB)
        const topColor = new THREE.Color(anim.topR, anim.topG, anim.topB)
        
        const mixedColor = bottomColor.clone().lerp(topColor, normalizedY)
        
        colors.push(mixedColor.r, mixedColor.g, mixedColor.b)
      }
      
      newGeometry.setAttribute('color', new THREE.Float32BufferAttribute(colors, 3))
      
      anim.cylinder.geometry.dispose()
      anim.cylinder.geometry = newGeometry
      anim.cylinder.position.y = anim.currentHeight / 2
    }
  })
  
  lightPillars.forEach(pillar => {
    const targetOpacity = pillar.userData.targetOpacity
    const currentOpacity = pillar.userData.currentOpacity
    
    if (Math.abs(targetOpacity - currentOpacity) > 0.01) {
      const newOpacity = currentOpacity + (targetOpacity - currentOpacity) * 0.1
      pillar.userData.currentOpacity = newOpacity
    }
    
    const time = Date.now() * 0.001
    const pulseOffset = pillar.userData.pulseOffset
    const ringPhase = pillar.userData.ringPhase
    
    const pulseFactor = 0.85 + Math.sin(time * 2 + pulseOffset) * 0.15
    const effectiveOpacity = pillar.userData.currentOpacity * pulseFactor
    
    const ringScale = 1 + ((time + ringPhase) % 3) * 0.3
    const ringOpacity = Math.max(0.2, 1 - ((time + ringPhase) % 3) / 3)
    
    pillar.children.forEach(child => {
      if (child.material) {
        if (child.material.userData?.isRing) {
          child.scale.set(ringScale, ringScale, 1)
          child.material.opacity = ringOpacity * effectiveOpacity * (child.material.userData?.baseOpacity || 1)
        } else {
          child.material.opacity = effectiveOpacity * (child.material.userData?.baseOpacity || 1)
        }
      }
    })
  })
  
  if (billboardSprite) {
    const targetOpacity = billboardSprite.userData.targetOpacity
    const currentOpacity = billboardSprite.userData.currentOpacity
    
    if (Math.abs(targetOpacity - currentOpacity) > 0.01) {
      const newOpacity = currentOpacity + (targetOpacity - currentOpacity) * 0.15
      billboardSprite.userData.currentOpacity = newOpacity
      billboardSprite.material.opacity = newOpacity
    }
  }
  
  renderer.render(scene, camera)
}

let pageVisible = true

const handleVisibilityChange = () => {
  pageVisible = !document.hidden
}

const handleResize = () => {
  const container = mapContainer.value
  if (!container) return
  
  const width = container.clientWidth
  const height = container.clientHeight

  camera.aspect = width / height
  camera.updateProjectionMatrix()
  renderer.setSize(width, height)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, useLiteRendering ? 1 : 1.75))
}

const disposeThreeResources = () => {
  if (animationId) {
    cancelAnimationFrame(animationId)
    animationId = null
  }
  
  const container = mapContainer.value
  if (container) {
    container.removeEventListener('click', onMouseClick)
  }
  
  removeLightPillars()
  removeBillboard()
  
  provinceMeshes.forEach(mesh => {
    if (mesh.geometry) mesh.geometry.dispose()
    if (mesh.material) {
      if (Array.isArray(mesh.material)) {
        mesh.material.forEach(m => m.dispose())
      } else {
        mesh.material.dispose()
      }
    }
    if (mesh.userData.edges) {
      if (mesh.userData.edges.geometry) mesh.userData.edges.geometry.dispose()
      if (mesh.userData.edges.material) mesh.userData.edges.material.dispose()
    }
  })
  provinceMeshes = []
  originalMaterials.clear()
  activeAnimations.clear()
  
  if (scene) {
    scene.traverse((object) => {
      if (object.geometry) {
        object.geometry.dispose()
      }
      if (object.material) {
        if (Array.isArray(object.material)) {
          object.material.forEach(m => {
            if (m.map) m.map.dispose()
            m.dispose()
          })
        } else {
          if (object.material.map) object.material.map.dispose()
          object.material.dispose()
        }
      }
      if (object.renderTarget) {
        object.renderTarget.dispose()
      }
    })
    scene.clear()
    scene = null
  }
  
  if (chinaGroup) {
    chinaGroup = null
  }
  
  if (controls) {
    controls.dispose()
    controls = null
  }
  
  if (renderer) {
    const gl = renderer.getContext()
    if (gl) {
      gl.flush()
      gl.finish()
    }
    
    renderer.dispose()
    renderer.forceContextLoss()
    if (container && renderer.domElement && container.contains(renderer.domElement)) {
      container.removeChild(renderer.domElement)
    }
    renderer = null
  }
  
  camera = null
  raycaster = null
  mouse = null
  selectedProvince = null
}

onMounted(async () => {
  screenDataset.value = await loadScreenDataset()
  initThree()
  window.addEventListener('resize', handleResize)
  document.addEventListener('visibilitychange', handleVisibilityChange)
})

onUnmounted(() => {
  disposeThreeResources()
  window.removeEventListener('resize', handleResize)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})
</script>

<style scoped>
.magazine-map-container {
  --screen-cn-font: "Microsoft YaHei", "PingFang SC", "Noto Sans SC", sans-serif;
  --screen-en-font: "Segoe UI", "Helvetica Neue", Arial, sans-serif;
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(160deg, #ece8e3 0%, #e0dbd5 30%, #d5d0c9 60%, #cec9c2 100%);
  overflow: hidden;
}

.globe-view {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 5;
}

.magazine-map-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-image: 
    radial-gradient(ellipse at 20% 50%, rgba(200, 195, 188, 0.3) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 20%, rgba(210, 205, 198, 0.2) 0%, transparent 50%),
    radial-gradient(ellipse at 50% 80%, rgba(190, 185, 178, 0.15) 0%, transparent 50%);
  pointer-events: none;
  z-index: 0;
}

.nav-bar {
  position: absolute;
  top: var(--screen-nav-safe-top, 0px);
  left: 0;
  right: 0;
  height: 96px;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
  pointer-events: none;
  background: linear-gradient(180deg, rgba(236, 232, 227, 1) 0%, rgba(236, 232, 227, 0.85) 40%, rgba(236, 232, 227, 0) 100%);
  opacity: 0;
  transform: translateY(-100px);
  transition: opacity 0.8s ease-out, transform 0.8s ease-out;
  will-change: transform, opacity;
}

.nav-bar.visible {
  opacity: 1;
  transform: translateY(0);
  transition-delay: 0.1s;
}

.nav-bar::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 10%;
  right: 10%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(106, 96, 88, 0.2) 20%, rgba(106, 96, 88, 0.2) 80%, transparent);
}

.nav-copy {
  display: grid;
  justify-items: center;
  gap: 2px;
  width: min(720px, calc(100vw - 160px));
}

.title-kicker {
  font-family: var(--screen-en-font);
  font-size: 10px;
  letter-spacing: 0.42em;
  color: rgba(90, 82, 74, 0.76);
}

.title {
  font-family: 'Georgia', 'Times New Roman', serif;
  font-size: clamp(28px, 2.8vw, 52px);
  font-weight: 400;
  letter-spacing: clamp(3px, 0.45vw, 8px);
  color: #5a524a;
  margin: 0;
  padding: 0 20px;
  text-align: center;
  white-space: normal;
  line-height: 1.02;
  position: relative;
  text-shadow:
    0 0 40px rgba(106, 96, 88, 0.08),
    0 0 80px rgba(106, 96, 88, 0.05),
    0 0 120px rgba(106, 96, 88, 0.03),
    0 2px 6px rgba(0, 0, 0, 0.04);
}

.title::before,
.title::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 40px;
  height: 1.5px;
  background: linear-gradient(90deg, transparent, rgba(106, 96, 88, 0.25), transparent);
}

.title::before {
  right: calc(100% + 20px);
}

.title::after {
  left: calc(100% + 20px);
}

.title-meta {
  font-family: var(--screen-cn-font);
  font-size: 11px;
  letter-spacing: 0.16em;
  color: rgba(90, 82, 74, 0.82);
}

.map-error-banner {
  position: absolute;
  top: calc(var(--screen-nav-safe-top, 0px) + 92px);
  left: 50%;
  transform: translateX(-50%);
  z-index: 30;
  display: grid;
  gap: 4px;
  min-width: min(520px, calc(100vw - 48px));
  padding: 14px 18px;
  border-radius: 14px;
  border: 1px solid rgba(144, 72, 56, 0.18);
  background: rgba(255, 247, 244, 0.94);
  color: #7f4739;
  box-shadow: 0 12px 32px rgba(77, 44, 32, 0.12);
  text-align: center;
}

.map-error-banner strong {
  font-family: 'Georgia', 'Times New Roman', serif;
  font-size: 16px;
  font-weight: 600;
}

.map-error-banner span {
  font-size: 13px;
  line-height: 1.5;
}

.map-canvas {
  width: 100%;
  height: 100%;
  position: relative;
  z-index: 1;
}

.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(160deg, #ece8e3 0%, #e0dbd5 30%, #d5d0c9 60%, #cec9c2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
  animation: fadeOut 0.8s ease-out 1.2s forwards;
}

.loading-overlay[v-show="false"] {
  display: none !important;
}

.loading-content {
  text-align: center;
  animation: pulseGlow 2s ease-in-out infinite;
}

.loading-spinner {
  width: 120px;
  height: 120px;
  border: 6px solid rgba(106, 96, 88, 0.1);
  border-top-color: #6a6058;
  border-radius: 50%;
  animation: spin 1.6s linear infinite;
  margin: 0 auto 24px;
  filter: blur(1px);
}

.loading-text {
  font-family: 'Georgia', 'Times New Roman', serif;
  font-size: clamp(44px, 8vw, 120px);
  font-weight: 400;
  letter-spacing: clamp(8px, 1.4vw, 18px);
  color: #6a6058;
  margin: 0;
  line-height: 1;
  opacity: 0.9;
  text-shadow:
    0 0 60px rgba(106, 96, 88, 0.06),
    0 0 120px rgba(106, 96, 88, 0.04);
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@keyframes fadeOut {
  from { opacity: 1; }
  to { opacity: 0; pointer-events: none; }
}

.panel-left {
  position: absolute;
  top: calc(var(--screen-nav-safe-top, 0px) + 102px);
  bottom: 146px;
  left: 18px;
  transform: translateX(-60px);
  width: clamp(340px, 24vw, 470px);
  display: flex;
  flex-direction: column;
  gap: 18px;
  z-index: 10;
  opacity: 0;
  transition: opacity 0.6s ease-out, transform 0.6s ease-out;
  will-change: transform, opacity;
}

.panel-left.visible {
  opacity: 1;
  transform: translateX(0);
  transition-delay: 0.7s;
}

.panel-left.exit-up {
  opacity: 0;
  transform: translateY(-100px);
  transition: opacity 0.5s ease-in, transform 0.5s ease-in;
}

.panel-right {
  position: absolute;
  top: calc(var(--screen-nav-safe-top, 0px) + 102px);
  right: 18px;
  bottom: 146px;
  transform: translateX(60px);
  width: clamp(340px, 24vw, 470px);
  display: flex;
  flex-direction: column;
  gap: 18px;
  z-index: 10;
  opacity: 0;
  transition: opacity 0.6s ease-out, transform 0.6s ease-out;
  will-change: transform, opacity;
}

.panel-right.visible {
  opacity: 1;
  transform: translateX(0);
  transition-delay: 0.7s;
}

.panel-right.exit-up {
  opacity: 0;
  transform: translateY(-100px);
  transition: opacity 0.5s ease-in, transform 0.5s ease-in;
}

.panel-item {
  flex: 1;
  min-height: 0;
  opacity: 0;
  transform: translateY(20px);
  transition: opacity 0.5s ease-out, transform 0.5s ease-out;
}

.panel-left.visible .panel-item,
.panel-right.visible .panel-item {
  opacity: 1;
  transform: translateY(0);
}

.panel-left.visible .panel-area1 { transition-delay: 0.1s; }
.panel-left.visible .panel-area2 { transition-delay: 0.25s; }
.panel-right.visible .panel-rtop { transition-delay: 0.1s; }
.panel-right.visible .panel-rbottom { transition-delay: 0.3s; }

.panel-area1 { flex: 1.14; }
.panel-area2 { flex: 0.86; }
.panel-rtop { flex: 1.14; }
.panel-rbottom { flex: 0.86; }

.panel-content {
  width: 100%;
  height: 100%;
  background: linear-gradient(180deg, #f0ece7 0%, #e4dfd9 100%);
  border: 1px solid rgba(106, 96, 88, 0.12);
  border-radius: 12px;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.05), inset 0 1px 0 rgba(255, 255, 255, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-rendering: optimizeLegibility;
}

.panel-content > :deep(*) {
  width: 100%;
  height: 100%;
}

.panel-content :deep(.chart-container),
.panel-content :deep(.overview-card),
.panel-content :deep(.rose-chart-card) {
  width: 100%;
  height: 100%;
  min-height: 0;
}

.panel-content :deep(.card-header) {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  align-items: start;
  gap: 6px;
  padding-bottom: 10px;
}

.panel-content :deep(.card-title) {
  font-family: var(--screen-cn-font);
  font-size: clamp(19px, 1.22vw, 26px);
  line-height: 1.2;
  letter-spacing: 0.03em;
  font-weight: 700;
  color: #294238;
  writing-mode: horizontal-tb !important;
  text-orientation: mixed !important;
  word-break: keep-all;
  overflow-wrap: anywhere;
}

.panel-content :deep(.card-subtitle) {
  display: block;
  font-family: var(--screen-en-font);
  font-size: clamp(10px, 0.68vw, 13px);
  line-height: 1.35;
  letter-spacing: 0.14em;
  text-align: left;
  white-space: normal;
  word-break: break-word;
  color: rgba(55, 70, 63, 0.72);
}

.panel-content :deep(.chart-body),
.panel-content :deep(.overview-chart) {
  min-height: 0;
  width: 100%;
}

.panel-content :deep(.card-footer) {
  font-family: var(--screen-cn-font);
  font-size: clamp(11px, 0.66vw, 13px);
  line-height: 1.45;
  color: rgba(48, 64, 58, 0.78);
}

.panel-content :deep(.source-link),
.panel-content :deep(.source-label),
.panel-content :deep(.legend-name),
.panel-content :deep(.legend-value),
.panel-content :deep(.stat-value),
.panel-content :deep(.stat-unit),
.panel-content :deep(.stat-label),
.panel-content :deep(.hero-caption),
.panel-content :deep(.sunburst-center-label),
.panel-content :deep(.sunburst-center-value) {
  font-family: var(--screen-cn-font);
  text-shadow: 0 1px 0 rgba(255, 255, 255, 0.25);
}

.panel-content :deep(.source-link),
.panel-content :deep(.source-label) {
  color: rgba(48, 64, 58, 0.78);
}

.panel-content :deep(.legend-name) {
  color: #2c463b;
}

.panel-content :deep(.legend-value) {
  color: rgba(44, 70, 59, 0.82);
}

.panel-content::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.5), transparent);
}

.panel-content::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border-radius: 16px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.15) 0%, transparent 50%);
  pointer-events: none;
}

.panel-placeholder {
  font-family: 'Georgia', 'Times New Roman', serif;
  font-size: 20px;
  font-weight: 400;
  letter-spacing: 10px;
  color: rgba(106, 96, 88, 0.35);
  position: relative;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-rendering: optimizeLegibility;
}

.bottom-arc {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 146px;
  z-index: 10;
  pointer-events: none;
  opacity: 0;
  transform: translateY(100%);
  transition: opacity 0.8s ease-out, transform 0.8s cubic-bezier(0.34, 1.56, 0.64, 1);
  will-change: transform, opacity;
}

.bottom-arc.visible {
  opacity: 1;
  transform: translateY(0);
  transition-delay: 0.4s;
}

.arc-svg {
  width: 100%;
  height: 100%;
  position: absolute;
  top: 0;
  left: 0;
}

.arc-icons {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.arc-icon {
  position: absolute;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(232, 228, 223, 0.96), rgba(220, 215, 208, 0.96));
  border: 1.5px solid rgba(106, 96, 88, 0.25);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  pointer-events: auto;
  color: #6a6058;
  transition: all 0.3s ease;
  padding: 17px;
  transform: translate(-50%, -50%);
}

.arc-icon:hover {
  transform: translate(-50%, -50%) scale(1.08);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.18);
  border-color: rgba(106, 96, 88, 0.5);
  color: #4a4038;
  background: linear-gradient(135deg, rgba(238, 234, 229, 0.96), rgba(225, 220, 213, 0.96));
}

.arc-icon:active {
  transform: translate(-50%, -50%) scale(0.96);
}

.arc-icon.active {
  background: linear-gradient(135deg, rgba(140, 130, 125, 0.98), rgba(120, 110, 105, 0.98));
  border-color: rgba(80, 70, 65, 0.7);
  color: #2a2520;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
}

.sub-buttons-container {
  position: absolute;
  pointer-events: auto;
  z-index: 20;
}

.sub-button {
  position: absolute;
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(58, 106, 90, 0.9), rgba(42, 74, 58, 0.95));
  border: 2px solid rgba(220, 235, 230, 0.6);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-family: 'Georgia', serif;
  font-size: 20px;
  font-weight: 600;
  color: #d8e4e0;
  opacity: 0;
  transform: translate(-50%, -50%) scale(0);
  transition: all 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.sub-buttons-container .sub-button {
  opacity: 1;
  transform: translate(-50%, -50%) scale(1);
}

.sub-button:hover {
  transform: translate(-50%, -50%) scale(1.15);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.35);
  background: linear-gradient(135deg, rgba(74, 122, 106, 0.95), rgba(58, 106, 90, 1));
}

.sub-button.active {
  background: linear-gradient(135deg, rgba(90, 138, 122, 1), rgba(74, 122, 106, 1));
  border-color: rgba(220, 235, 230, 0.9);
  box-shadow: 0 10px 35px rgba(58, 106, 90, 0.4);
}

.wetland-legend {
  position: absolute;
  left: 16px;
  bottom: 78px;
  background: rgba(240, 236, 231, 0.92);
  border: 1px solid rgba(100, 140, 180, 0.25);
  border-radius: 14px;
  padding: 14px 16px;
  z-index: 50;
  opacity: 0;
  transform: translateX(-20px);
  transition: opacity 0.5s ease, transform 0.5s ease;
}

.wetland-legend.show {
  opacity: 1;
  transform: translateX(0);
}

.legend-title {
  font-family: 'Georgia', serif;
  font-size: 17px;
  color: #5a6a6a;
  margin-bottom: 10px;
  letter-spacing: 1px;
}

.legend-gradient {
  width: min(220px, 28vw);
  height: 12px;
  background: linear-gradient(90deg, #e6ebeb, #c8d5d8, #aabfc2, #8ca9ac, #6e9396, #5a7d80);
  border-radius: 8px;
  margin-bottom: 16px;
}

.legend-labels {
  display: flex;
  justify-content: space-between;
  width: min(220px, 28vw);
  font-family: 'Georgia', serif;
  font-size: 13px;
  color: rgba(90, 106, 106, 0.7);
}

.legend-unit {
  font-family: 'Georgia', serif;
  font-size: 13px;
  color: rgba(90, 106, 106, 0.55);
  margin-top: 16px;
}

.wetland-source {
  position: absolute;
  right: 16px;
  bottom: 34px;
  font-family: 'Georgia', serif;
  font-size: 13px;
  color: rgba(90, 106, 106, 0.6);
  text-decoration: none;
  z-index: 50;
  opacity: 0;
  transform: translateX(20px);
  transition: opacity 0.5s ease 0.2s, transform 0.5s ease 0.2s, color 0.3s ease;
}

.wetland-source.show {
  opacity: 1;
  transform: translateX(0);
}

.wetland-source:hover {
  color: #4a6a8a;
  text-decoration: underline;
}

.arc-icon.active {
  background: linear-gradient(135deg, rgba(100, 140, 180, 0.25), rgba(80, 120, 160, 0.35)) !important;
  border-color: rgba(100, 140, 180, 0.55) !important;
  color: #4a6a8a !important;
}

/* 湿地景区弹窗样式 */
.spot-popup {
  position: fixed;
  z-index: 1000;
  pointer-events: none;
  transform: translate(-50%, -100%);
}

.spot-popup-content {
  pointer-events: auto;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.15), 0 10px 30px rgba(0, 0, 0, 0.1);
  max-width: 520px;
  width: 90%;
  max-height: 85vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  animation: spotPopupAppear 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  border: 1px solid rgba(255, 255, 255, 0.3);
}

@keyframes spotPopupAppear {
  0% {
    opacity: 0;
    transform: scale(0.9) translateY(20px);
  }
  100% {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.spot-popup-close {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(0, 0, 0, 0.1);
  color: #555;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.spot-popup-close:hover {
  background: rgba(255, 255, 255, 1);
  border-color: rgba(0, 0, 0, 0.2);
  color: #333;
  transform: scale(1.1);
}

.spot-popup-image-container {
  width: 100%;
  height: 240px;
  overflow: hidden;
  background: #f8f8f8;
}

.spot-popup-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.spot-popup-image:hover {
  transform: scale(1.05);
}

.spot-popup-info {
  padding: 20px;
  flex: 1;
}

.spot-popup-title {
  font-family: 'Georgia', serif;
  font-size: 22px;
  color: #333;
  margin-bottom: 16px;
  line-height: 1.3;
}

.spot-popup-description {
  font-family: 'Georgia', serif;
  font-size: 15px;
  color: #666;
  line-height: 1.6;
  margin: 0;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .nav-bar {
    height: 78px;
  }

  .title-kicker,
  .title-meta {
    letter-spacing: 0.2em;
  }

  .title {
    font-size: 26px;
    letter-spacing: 2px;
  }

  .panel-left,
  .panel-right {
    top: calc(var(--screen-nav-safe-top, 0px) + 90px);
    bottom: 132px;
    width: min(44vw, 258px);
    gap: 12px;
  }

  .bottom-arc {
    height: 132px;
  }

  .map-error-banner {
    top: calc(var(--screen-nav-safe-top, 0px) + 72px);
    min-width: calc(100vw - 24px);
  }

  .arc-icon {
    width: 64px;
    height: 64px;
    padding: 14px;
  }

  .spot-popup-content {
    flex-direction: column;
    max-height: 90vh;
  }
  
  .spot-popup-image-container {
    height: 300px;
  }
  
  .spot-popup-title {
    font-size: 28px;
  }
  
  .spot-popup-description {
    font-size: 18px;
  }
}
</style>
