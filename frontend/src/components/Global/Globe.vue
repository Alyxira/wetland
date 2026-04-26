<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import GlobeStream3D from 'globestream3d'
import worldGeoJson from '@surbowl/world-geo-json-zh'
import * as THREE from 'globestream3d/node_modules/three/build/three.module.js'
import { screenApi } from '../api'

const containerRef = ref(null)
const tooltipRef = ref(null)
const loadingRef = ref(null)
let globeInstance = null

const emit = defineEmits(['back'])

const countryNameMap = {
  'China': '中华人民共和国',
  'United States of America': '美利坚合众国',
  'India': '印度共和国',
  'Brazil': '巴西联邦共和国',
  'Russia': '俄罗斯联邦',
  'Japan': '日本国',
  'Germany': '德意志联邦共和国',
  'United Kingdom': '大不列颠及北爱尔兰联合王国',
  'France': '法兰西共和国',
  'Italy': '意大利共和国',
  'Canada': '加拿大',
  'Australia': '澳大利亚联邦',
  'South Korea': '大韩民国',
  'Spain': '西班牙王国',
  'Mexico': '墨西哥合众国',
  'Indonesia': '印度尼西亚共和国',
  'Netherlands': '荷兰王国',
  'Saudi Arabia': '沙特阿拉伯王国',
  'Turkey': '土耳其共和国',
  'Switzerland': '瑞士联邦',
  'Poland': '波兰共和国',
  'Sweden': '瑞典王国',
  'Belgium': '比利时王国',
  'Argentina': '阿根廷共和国',
  'Norway': '挪威王国',
  'Austria': '奥地利共和国',
  'United Arab Emirates': '阿拉伯联合酋长国',
  'Nigeria': '尼日利亚联邦共和国',
  'South Africa': '南非共和国',
  'Egypt': '阿拉伯埃及共和国',
  'Singapore': '新加坡共和国',
  'Thailand': '泰王国',
  'Malaysia': '马来西亚',
  'Philippines': '菲律宾共和国',
  'Vietnam': '越南社会主义共和国',
  'Pakistan': '巴基斯坦伊斯兰共和国',
  'Bangladesh': '孟加拉人民共和国',
  'Iran': '伊朗伊斯兰共和国',
  'Israel': '以色列国',
  'Greece': '希腊共和国',
  'Portugal': '葡萄牙共和国',
  'Czech Republic': '捷克共和国',
  'Romania': '罗马尼亚',
  'Hungary': '匈牙利',
  'Ukraine': '乌克兰',
  'New Zealand': '新西兰',
  'Ireland': '爱尔兰',
  'Denmark': '丹麦王国',
  'Finland': '芬兰共和国',
  'Chile': '智利共和国',
  'Colombia': '哥伦比亚共和国',
  'Peru': '秘鲁共和国',
  'Venezuela': '委内瑞拉玻利瓦尔共和国',
  'Kazakhstan': '哈萨克斯坦共和国',
  'Morocco': '摩洛哥王国',
  'Algeria': '阿尔及利亚民主人民共和国',
  'Kenya': '肯尼亚共和国',
  'Ethiopia': '埃塞俄比亚联邦民主共和国',
  'Tanzania': '坦桑尼亚联合共和国',
  'Ghana': '加纳共和国',
  'Sudan': '苏丹共和国',
  'Myanmar': '缅甸联邦共和国',
  'Nepal': '尼泊尔联邦民主共和国',
  'Sri Lanka': '斯里兰卡民主社会主义共和国',
  'Cambodia': '柬埔寨王国',
  'Laos': '老挝人民民主共和国',
  'Mongolia': '蒙古国',
  'North Korea': '朝鲜民主主义人民共和国',
  'Afghanistan': '阿富汗伊斯兰共和国',
  'Iraq': '伊拉克共和国',
  'Syria': '阿拉伯叙利亚共和国',
  'Jordan': '约旦哈希姆王国',
  'Lebanon': '黎巴嫩共和国',
  'Kuwait': '科威特国',
  'Qatar': '卡塔尔国',
  'Bahrain': '巴林王国',
  'Oman': '阿曼苏丹国',
  'Yemen': '也门共和国',
  'Libya': '利比亚国',
  'Tunisia': '突尼斯共和国',
  'Angola': '安哥拉共和国',
  'Zimbabwe': '津巴布韦共和国',
  'Botswana': '博茨瓦纳共和国',
  'Namibia': '纳米比亚共和国',
  'Mozambique': '莫桑比克共和国',
  'Madagascar': '马达加斯加共和国',
  'Uganda': '乌干达共和国',
  'Rwanda': '卢旺达共和国',
  'Burundi': '布隆迪共和国',
  'Somalia': '索马里联邦共和国',
  'Djibouti': '吉布提共和国',
  'Eritrea': '厄立特里亚国',
  'South Sudan': '南苏丹共和国',
  'Central African Republic': '中非共和国',
  'Chad': '乍得共和国',
  'Niger': '尼日尔共和国',
  'Mali': '马里共和国',
  'Burkina Faso': '布基纳法索',
  'Senegal': '塞内加尔共和国',
  'Guinea': '几内亚共和国',
  'Sierra Leone': '塞拉利昂共和国',
  'Liberia': '利比里亚共和国',
  'Ivory Coast': '科特迪瓦共和国',
  'Cameroon': '喀麦隆共和国',
  'Gabon': '加蓬共和国',
  'Congo': '刚果共和国',
  'Democratic Republic of the Congo': '刚果民主共和国',
  'Zambia': '赞比亚共和国',
  'Malawi': '马拉维共和国',
  'Lesotho': '莱索托王国',
  'Swaziland': '斯威士兰王国',
  'Mauritania': '毛里塔尼亚伊斯兰共和国',
  'Western Sahara': '西撒哈拉',
  'Greenland': '格陵兰',
  'Iceland': '冰岛共和国',
  'Estonia': '爱沙尼亚共和国',
  'Latvia': '拉脱维亚共和国',
  'Lithuania': '立陶宛共和国',
  'Belarus': '白俄罗斯共和国',
  'Moldova': '摩尔多瓦共和国',
  'Georgia': '格鲁吉亚',
  'Armenia': '亚美尼亚共和国',
  'Azerbaijan': '阿塞拜疆共和国',
  'Turkmenistan': '土库曼斯坦',
  'Uzbekistan': '乌兹别克斯坦共和国',
  'Tajikistan': '塔吉克斯坦共和国',
  'Kyrgyzstan': '吉尔吉斯共和国',
  'Bhutan': '不丹王国',
  'Maldives': '马尔代夫共和国',
  'Brunei': '文莱达鲁萨兰国',
  'Timor-Leste': '东帝汶民主共和国',
  'Papua New Guinea': '巴布亚新几内亚独立国',
  'Fiji': '斐济共和国',
  'Solomon Islands': '所罗门群岛',
  'Vanuatu': '瓦努阿图共和国',
  'Samoa': '萨摩亚独立国',
  'Tonga': '汤加王国',
  'Micronesia': '密克罗尼西亚联邦',
  'Palau': '帕劳共和国',
  'Marshall Islands': '马绍尔群岛共和国',
  'Kiribati': '基里巴斯共和国',
  'Nauru': '瑙鲁共和国',
  'Tuvalu': '图瓦卢',
  'Cuba': '古巴共和国',
  'Haiti': '海地共和国',
  'Dominican Republic': '多米尼加共和国',
  'Jamaica': '牙买加',
  'Trinidad and Tobago': '特立尼达和多巴哥共和国',
  'Bahamas': '巴哈马国',
  'Barbados': '巴巴多斯',
  'Belize': '伯利兹',
  'Guyana': '圭亚那合作共和国',
  'Suriname': '苏里南共和国',
  'Uruguay': '乌拉圭东岸共和国',
  'Paraguay': '巴拉圭共和国',
  'Bolivia': '玻利维亚多民族国',
  'Ecuador': '厄瓜多尔共和国',
  'Panama': '巴拿马共和国',
  'Costa Rica': '哥斯达黎加共和国',
  'Nicaragua': '尼加拉瓜共和国',
  'Honduras': '洪都拉斯共和国',
  'El Salvador': '萨尔瓦多共和国',
  'Guatemala': '危地马拉共和国',
  'Taiwan': '台湾地区'
}

const globalWetlandOverview = ref({
  totalArea: '12.1',
  totalAreaUnit: '百万平方公里',
  landPercentage: '8.1%',
  dataSource: '国家林业和草原局',
  updateTime: '2023年'
})

const wetlandTypes = ref([
  { name: '内陆沼泽', percentage: 35.2, area: '4,259,200', color: '#5A8062' },
  { name: '滨海湿地', percentage: 22.8, area: '2,758,800', color: '#6B9173' },
  { name: '湖泊湿地', percentage: 18.5, area: '2,238,500', color: '#7CA384' },
  { name: '河流湿地', percentage: 15.3, area: '1,851,300', color: '#8DB595' },
  { name: '人工湿地', percentage: 8.2, area: '992,400', color: '#9EC7A6' }
])

const majorRegions = ref([
  { name: '北美洲', area: '2.8 百万km²', percentage: '23.1%' },
  { name: '亚洲', area: '2.4 百万km²', percentage: '19.8%' },
  { name: '南美洲', area: '1.9 百万km²', percentage: '15.7%' },
  { name: '非洲', area: '1.3 百万km²', percentage: '10.7%' },
  { name: '欧洲', area: '1.0 百万km²', percentage: '8.3%' }
])

const ramsarData = ref({
  totalSites: 2521,
  totalArea: '2.57',
  totalAreaUnit: '百万平方公里',
  contractingParties: 172,
  dataSource: '拉姆萨尔公约秘书处',
  updateTime: '2024年'
})

const topRamsarCountries = ref([
  { rank: 1, country: '英国', sites: 175 },
  { rank: 2, country: '墨西哥', sites: 142 },
  { rank: 3, country: '西班牙', sites: 75 },
  { rank: 4, country: '澳大利亚', sites: 66 },
  { rank: 5, country: '中国', sites: 64 }
])

const ecologicalIndicators = ref([
  { name: '生物多样性指数', value: '8.7/10' },
  { name: '水质净化能力', value: '92%' },
  { name: '洪水调节能力', value: '87%' },
  { name: '气候调节贡献', value: '15%' }
])

const wetlandDistributionData = [
  { country: '中华人民共和国', lat: 35.8617, lng: 104.1954, wetlandArea: 53.6, ramsarSites: 64 },
  { country: '加拿大', lat: 56.1304, lng: -106.3468, wetlandArea: 127.0, ramsarSites: 37 },
  { country: '俄罗斯联邦', lat: 61.5240, lng: 105.3188, wetlandArea: 143.0, ramsarSites: 35 },
  { country: '美利坚合众国', lat: 37.0902, lng: -95.7129, wetlandArea: 114.0, ramsarSites: 41 },
  { country: '巴西联邦共和国', lat: -14.2350, lng: -51.9253, wetlandArea: 108.9, ramsarSites: 27 },
  { country: '印度尼西亚', lat: -0.7893, lng: 113.9213, wetlandArea: 36.7, ramsarSites: 22 },
  { country: '欧盟', lat: 50.1100, lng: 9.6800, wetlandArea: 32.5, ramsarSites: 488 },
  { country: '澳大利亚联邦', lat: -25.2744, lng: 133.7751, wetlandArea: 8.5, ramsarSites: 66 },
  { country: '印度共和国', lat: 20.5937, lng: 78.9629, wetlandArea: 15.3, ramsarSites: 49 },
  { country: '阿根廷共和国', lat: -38.4161, lng: -63.6167, wetlandArea: 27.0, ramsarSites: 23 },
  { country: '墨西哥', lat: 23.6345, lng: -102.5528, wetlandArea: 10.5, ramsarSites: 142 },
  { country: '苏丹共和国', lat: 12.8628, lng: 30.2176, wetlandArea: 8.4, ramsarSites: 4 },
  { country: '刚果民主共和国', lat: -4.0383, lng: 21.7587, wetlandArea: 25.0, ramsarSites: 3 },
  { country: '哥伦比亚', lat: 4.5709, lng: -74.2973, wetlandArea: 9.5, ramsarSites: 8 },
  { country: '秘鲁共和国', lat: -9.1900, lng: -75.0152, wetlandArea: 6.8, ramsarSites: 13 },
  { country: '乍得共和国', lat: 15.4542, lng: 18.7322, wetlandArea: 4.9, ramsarSites: 5 },
  { country: '尼日利亚联邦共和国', lat: 9.0820, lng: 8.6753, wetlandArea: 3.1, ramsarSites: 11 },
  { country: '孟加拉人民共和国', lat: 23.6850, lng: 90.3563, wetlandArea: 7.5, ramsarSites: 2 },
  { country: '越南社会主义共和国', lat: 14.0583, lng: 108.2772, wetlandArea: 10.0, ramsarSites: 9 },
  { country: '马来西亚', lat: 4.2105, lng: 101.9758, wetlandArea: 3.4, ramsarSites: 7 }
]

const initGlobe = async () => {
  if (!containerRef.value) return

  try {
    if (loadingRef.value) {
      loadingRef.value.style.display = 'flex'
    }

    let mergedWorldData = {
      type: 'FeatureCollection',
      features: [...worldGeoJson.features]
    }
    
    try {
      let chinaMapData = null
      let lastError = null

      for (const url of screenApi.getChinaGeoJsonFallbackUrls()) {
        try {
          const chinaResponse = await fetch(url)
          if (!chinaResponse.ok) {
            throw new Error(`HTTP ${chinaResponse.status} when loading ${url}`)
          }
          chinaMapData = await chinaResponse.json()
          break
        } catch (error) {
          lastError = error
        }
      }

      if (!chinaMapData) {
        throw lastError || new Error('China GeoJSON unavailable')
      }

      const worldFeatures = [...worldGeoJson.features]
      
      const chinaIndex = worldFeatures.findIndex(f => 
        f.properties.name === '中国' || 
        f.properties.iso_a2 === 'CN' || 
        f.properties.iso_a3 === 'CHN'
      )
      
      if (chinaIndex !== -1) {
        worldFeatures.splice(chinaIndex, 1)
      }
      
      chinaMapData.features.forEach(feature => {
        if (feature.properties && feature.properties.name) {
          feature.properties = {
            ...feature.properties,
            name: feature.properties.name,
            iso_a2: 'CN',
            iso_a3: 'CHN',
            iso_n3: '156'
          }
        }
      })
      
      mergedWorldData = {
        type: 'FeatureCollection',
        features: [...worldFeatures, ...chinaMapData.features]
      }
    } catch (fetchError) {
      console.warn('中国地图数据加载失败，使用默认数据:', fetchError)
    }
    
    GlobeStream3D.registerMap('world', mergedWorldData)

    globeInstance = GlobeStream3D.init({
      dom: containerRef.value,
      map: 'world',
      mode: '3d',
      autoRotate: true,
      rotateSpeed: 0.015,
      helper: false,
      controls: 'builtIn',
      light: 'AmbientLight',
      config: {
        R: 100,
        enableZoom: true,
        zoom: 1.2,
        stopRotateByHover: true,
        earth: {
          color: '#F3F2EE'
        },
        bgStyle: {
          color: '#FAFAFA',
          opacity: 1
        },
        mapStyle: {
          areaColor: '#F3F2EE',
          lineColor: '#A9B0A6',
          opacity: 0.4
        },
        regions: {},
        hoverRegionStyle: {
          areaColor: '#E8E7E3',
          opacity: 0.5,
          show: true
        },
        spriteStyle: {
          show: false
        }
      }
    })

    globeInstance.on('mousemove', (event, mesh) => {
      if (mesh && mesh.name && tooltipRef.value) {
        const countryName = countryNameMap[mesh.name] || mesh.name
        tooltipRef.value.style.display = 'block'
        tooltipRef.value.style.left = event.clientX + 15 + 'px'
        tooltipRef.value.style.top = event.clientY + 15 + 'px'
        tooltipRef.value.textContent = countryName
      } else if (tooltipRef.value) {
        tooltipRef.value.style.display = 'none'
      }
    })

    globeInstance.on('mouseout', () => {
      if (tooltipRef.value) {
        tooltipRef.value.style.display = 'none'
      }
    })

    if (loadingRef.value) {
      loadingRef.value.style.display = 'none'
    }
    
    setTimeout(() => {
      addWetlandBars()
    }, 1000)
    
  } catch (error) {
    console.error('初始化地球失败:', error)
    if (loadingRef.value) {
      loadingRef.value.innerHTML = `
        <div style="text-align: center; color: #666;">
          <div style="font-size: 48px; margin-bottom: 20px;">⚠️</div>
          <div style="font-size: 18px; margin-bottom: 10px;">地图加载失败</div>
          <div style="font-size: 14px; color: #999;">请刷新页面重试</div>
        </div>
      `
    }
  }
}

const customMeshes = []
let animationFrameId = null
let baseRings = []
let ringAnimationTime = 0
let lastFrameTime = 0
let barGrowthAnimations = []

const createLabelSprite = (text, wetlandArea, ramsarSites) => {
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')
  canvas.width = 800
  canvas.height = 400
  
  const gradient = ctx.createLinearGradient(0, 0, 0, 400)
  gradient.addColorStop(0, 'rgba(255, 255, 255, 0.98)')
  gradient.addColorStop(1, 'rgba(245, 245, 245, 0.95)')
  ctx.fillStyle = gradient
  ctx.strokeStyle = 'rgba(76, 102, 82, 0.9)'
  ctx.lineWidth = 4
  ctx.beginPath()
  ctx.roundRect(20, 20, 760, 360, 16)
  ctx.fill()
  ctx.stroke()
  
  ctx.strokeStyle = 'rgba(90, 128, 98, 0.3)'
  ctx.lineWidth = 2
  ctx.beginPath()
  ctx.roundRect(35, 35, 730, 330, 12)
  ctx.stroke()
  
  ctx.fillStyle = '#1A2E1A'
  ctx.font = 'bold 48px "Microsoft YaHei", sans-serif'
  ctx.textAlign = 'center'
  ctx.fillText(text, 400, 100)
  
  ctx.fillStyle = '#5A8062'
  ctx.font = '32px "Microsoft YaHei", sans-serif'
  ctx.fillText(`湿地面积: ${wetlandArea}万km²`, 400, 180)
  
  ctx.fillStyle = '#4A6B52'
  ctx.font = '28px "Microsoft YaHei", sans-serif'
  ctx.fillText(`拉姆萨尔站点: ${ramsarSites}个`, 400, 240)
  
  ctx.fillStyle = '#8A9A8E'
  ctx.font = '22px "Microsoft YaHei", sans-serif'
  ctx.fillText('数据来源: 国家林草局', 400, 310)
  
  const texture = new THREE.CanvasTexture(canvas)
  texture.needsUpdate = true
  
  const spriteMaterial = new THREE.SpriteMaterial({
    map: texture,
    transparent: true,
    depthTest: true,
    depthWrite: false
  })
  
  const sprite = new THREE.Sprite(spriteMaterial)
  sprite.scale.set(35, 17.5, 1)
  sprite.type = 'TransformControlsPlane'
  
  return sprite
}

const createGradientCylinder = (radius, height, normalizedHeight) => {
  const { CylinderGeometry, MeshPhongMaterial, Mesh, Color, Float32BufferAttribute } = THREE
  
  const geometry = new CylinderGeometry(radius, radius, height, 32, 32, false)
  
  const colors = []
  const positionAttribute = geometry.attributes.position
  
  const intensityFactor = normalizedHeight
  
  const bottomGray = 0.95 - intensityFactor * 0.15
  const topGray = 0.20 + intensityFactor * 0.15
  
  for (let i = 0; i < positionAttribute.count; i++) {
    const y = positionAttribute.getY(i)
    const normalizedY = (y + height / 2) / height
    
    const bottomColor = new Color(bottomGray, bottomGray, bottomGray)
    const topColor = new Color(topGray, topGray, topGray)
    
    const mixedColor = bottomColor.clone().lerp(topColor, normalizedY)
    
    colors.push(mixedColor.r, mixedColor.g, mixedColor.b)
  }
  
  geometry.setAttribute('color', new Float32BufferAttribute(colors, 3))
  
  const material = new MeshPhongMaterial({
    vertexColors: true,
    transparent: true,
    opacity: 0.95,
    shininess: 80,
    specular: new Color(0x666666)
  })
  
  const cylinder = new Mesh(geometry, material)
  cylinder.position.y = height / 2
  cylinder.type = 'TransformControlsPlane'
  
  return cylinder
}

const updateCylinderHeight = (cylinder, radius, currentHeight, normalizedHeight) => {
  const { CylinderGeometry, Color, Float32BufferAttribute } = THREE
  
  const newGeometry = new CylinderGeometry(radius, radius, currentHeight, 32, 32, false)
  
  const colors = []
  const positionAttribute = newGeometry.attributes.position
  
  const intensityFactor = normalizedHeight
  
  const bottomGray = 0.95 - intensityFactor * 0.15
  const topGray = 0.20 + intensityFactor * 0.15
  
  for (let i = 0; i < positionAttribute.count; i++) {
    const y = positionAttribute.getY(i)
    const normalizedY = (y + currentHeight / 2) / currentHeight
    
    const bottomColor = new Color(bottomGray, bottomGray, bottomGray)
    const topColor = new Color(topGray, topGray, topGray)
    
    const mixedColor = bottomColor.clone().lerp(topColor, normalizedY)
    
    colors.push(mixedColor.r, mixedColor.g, mixedColor.b)
  }
  
  newGeometry.setAttribute('color', new Float32BufferAttribute(colors, 3))
  
  cylinder.geometry.dispose()
  cylinder.geometry = newGeometry
  cylinder.position.y = currentHeight / 2
}

const createBaseRings = (radius, group) => {
  const { RingGeometry, MeshBasicMaterial, Mesh, DoubleSide, Color } = THREE
  const rings = []
  
  const ringConfigs = [
    { innerRadius: radius + 0.3, outerRadius: radius + 1.5, baseOpacity: 0.6 },
    { innerRadius: radius + 1.5, outerRadius: radius + 2.8, baseOpacity: 0.4 },
    { innerRadius: radius + 2.8, outerRadius: radius + 4.2, baseOpacity: 0.25 },
    { innerRadius: radius + 4.2, outerRadius: radius + 5.8, baseOpacity: 0.15 }
  ]
  
  ringConfigs.forEach((config) => {
    const ringGeometry = new RingGeometry(config.innerRadius, config.outerRadius, 64)
    const ringMaterial = new MeshBasicMaterial({
      color: new Color(0x5A7B5E),
      transparent: true,
      opacity: config.baseOpacity,
      side: DoubleSide
    })
    ringMaterial.userData.baseOpacity = config.baseOpacity
    
    const ring = new Mesh(ringGeometry, ringMaterial)
    ring.rotation.x = -Math.PI / 2
    ring.position.y = 1.8
    ring.type = 'TransformControlsPlane'
    group.add(ring)
    rings.push(ring)
  })
  
  return rings
}

const animateBaseRings = () => {
  const currentTime = performance.now()
  if (lastFrameTime === 0) {
    lastFrameTime = currentTime
  }
  const deltaTime = (currentTime - lastFrameTime) / 1000
  lastFrameTime = currentTime
  
  ringAnimationTime += deltaTime
  
  baseRings.forEach(ringGroup => {
    ringGroup.rings.forEach((ring) => {
      const material = ring.material
      const baseOpacity = material.userData.baseOpacity
      
      const cycleDuration = 2.0
      const phase = (ringAnimationTime % cycleDuration) / cycleDuration
      
      const scale = 1 + phase * 1.2
      const opacity = baseOpacity * (1 - phase)
      
      ring.scale.set(scale, scale, 1)
      material.opacity = Math.max(0, opacity)
    })
  })
  
  barGrowthAnimations.forEach(anim => {
    if (!anim.isComplete) {
      anim.currentHeight += anim.growthSpeed
      
      if (anim.currentHeight >= anim.targetHeight) {
        anim.currentHeight = anim.targetHeight
        anim.isComplete = true
        anim.label.visible = true
      }
      
      updateCylinderHeight(anim.cylinder, anim.radius, anim.currentHeight, anim.normalizedHeight)
    }
  })
}

const checkOcclusion = (camera) => {
  customMeshes.forEach(group => {
    const worldPos = new THREE.Vector3()
    group.getWorldPosition(worldPos)
    
    const cameraDir = new THREE.Vector3()
    camera.getWorldDirection(cameraDir)
    
    const toMeshDir = worldPos.clone().sub(camera.position).normalize()
    
    const dotProduct = cameraDir.dot(toMeshDir)
    
    const isOccluded = dotProduct < -0.1
    
    group.traverse(child => {
      if (child.material && child.material.userData.baseOpacity !== undefined) {
        if (isOccluded) {
          child.material.opacity = child.material.userData.baseOpacity * 0.15
        } else {
          child.material.opacity = child.material.userData.baseOpacity
        }
      }
    })
    
    group.userData.isOccluded = isOccluded
  })
}

const startOcclusionDetection = (camera) => {
  const animate = () => {
    checkOcclusion(camera)
    animateBaseRings()
    animationFrameId = requestAnimationFrame(animate)
  }
  animate()
}

const addWetlandBars = async () => {
  if (!globeInstance || !globeInstance.scene) return
  
  const mainContainer = globeInstance.mainContainer
  
  const { 
    Group, 
    Vector3, 
    Quaternion,
    Sprite
  } = THREE
  
  const config = globeInstance._store.getConfig()
  const R = config.R
  
  const maxArea = Math.max(...wetlandDistributionData.map(d => d.wetlandArea))
  
  wetlandDistributionData.forEach((location, index) => {
    const normalizedHeight = location.wetlandArea / maxArea
    const targetHeight = normalizedHeight * 35 + 8
    const group = new Group()
    
    const radius = 2.5
    
    const cylinder = createGradientCylinder(radius, 0.1, normalizedHeight)
    cylinder.material.userData.baseOpacity = 0.95
    group.add(cylinder)
    
    const rings = createBaseRings(radius, group)
    baseRings.push({ group, rings })
    
    const label = createLabelSprite(location.country, location.wetlandArea.toFixed(1), location.ramsarSites)
    label.position.y = targetHeight + 18
    label.material.userData.baseOpacity = 1.0
    label.visible = false
    group.add(label)
    
    const pos = lon2xyz(R, location.lng, location.lat)
    group.position.set(pos.x, pos.y, pos.z)
    
    const normal = new Vector3(pos.x, pos.y, pos.z).normalize()
    const up = new Vector3(0, 1, 0)
    const quaternion = new Quaternion()
    quaternion.setFromUnitVectors(up, normal)
    group.quaternion.copy(quaternion)
    
    group.userData.figureType = 'customBar'
    group.userData.id = `wetland-bar-${index}`
    group.userData.locationData = location
    
    mainContainer.add(group)
    customMeshes.push(group)
    
    barGrowthAnimations.push({
      cylinder,
      label,
      radius,
      normalizedHeight,
      currentHeight: 0.1,
      targetHeight,
      growthSpeed: targetHeight * 0.02,
      isComplete: false
    })
  })
  
  const camera = globeInstance.camera
  if (camera) {
    startOcclusionDetection(camera)
  }
}

const lon2xyz = (R, longitude, latitude) => {
  let lon = longitude * Math.PI / 180
  let lat = latitude * Math.PI / 180
  lon = -lon
  
  const x = R * Math.cos(lat) * Math.cos(lon)
  const y = R * Math.sin(lat)
  const z = R * Math.cos(lat) * Math.sin(lon)
  
  return { x, y, z }
}

const goBack = () => {
  emit('back')
}

const cleanup = () => {
  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId)
    animationFrameId = null
  }
  
  baseRings = []
  ringAnimationTime = 0
  lastFrameTime = 0
  barGrowthAnimations = []
  
  customMeshes.forEach(mesh => {
    if (mesh.parent) {
      mesh.parent.remove(mesh)
    }
    mesh.traverse((child) => {
      if (child.geometry) {
        child.geometry.dispose()
      }
      if (child.material) {
        if (Array.isArray(child.material)) {
          child.material.forEach(m => m.dispose())
        } else {
          child.material.dispose()
        }
      }
    })
  })
  customMeshes.length = 0
  
  if (globeInstance) {
    globeInstance.destroy()
    globeInstance = null
  }
}

onMounted(() => {
  initGlobe()
})

onUnmounted(() => {
  cleanup()
})
</script>

<template>
  <div class="globe-container">
    <div class="globe-nav-bar">
      <h1 class="globe-title">全球湿地分布概览</h1>
    </div>
    
    <div class="left-panel">
        <div class="panel-header">
          <h3 class="panel-title">全球湿地概况</h3>
          <span class="panel-subtitle">GLOBAL WETLAND OVERVIEW</span>
        </div>
        
        <div class="panel-body">
          <div class="section">
            <div class="section-title">湿地总面积</div>
            <div class="stat-highlight">
              <span class="stat-value">{{ globalWetlandOverview.totalArea }}</span>
              <span class="stat-unit">{{ globalWetlandOverview.totalAreaUnit }}</span>
            </div>
            <div class="stat-detail">占全球陆地面积 {{ globalWetlandOverview.landPercentage }}</div>
          </div>
          
          <div class="section">
            <div class="section-title">各大洲湿地分布</div>
            <div class="region-list">
              <div class="region-item" v-for="region in majorRegions" :key="region.name">
                <div class="region-name">{{ region.name }}</div>
                <div class="region-bar-container">
                  <div class="region-bar" :style="{ width: region.percentage }"></div>
                </div>
                <div class="region-area">{{ region.area }}</div>
              </div>
            </div>
          </div>
          
          <div class="section">
            <div class="section-title">湿地类型构成</div>
            <div class="type-list">
              <div class="type-item" v-for="type in wetlandTypes" :key="type.name">
                <div class="type-color" :style="{ backgroundColor: type.color }"></div>
                <div class="type-info">
                  <div class="type-name">{{ type.name }}</div>
                  <div class="type-percentage">{{ type.percentage }}%</div>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <div class="panel-footer">
          <span class="source-label">数据来源：{{ globalWetlandOverview.dataSource }}</span>
        </div>
    </div>
    
    <div class="globe-center">
      <div ref="containerRef" class="globe-canvas"></div>
      <div ref="tooltipRef" class="country-tooltip"></div>
      <div ref="loadingRef" class="loading-overlay">
        <div class="loading-content">
          <div class="loading-spinner"></div>
          <p class="loading-text">LOADING</p>
        </div>
      </div>
    </div>
    
    <div class="right-panel">
        <div class="panel-header">
          <h3 class="panel-title">拉姆萨尔公约</h3>
          <span class="panel-subtitle">RAMSAR CONVENTION</span>
        </div>
        
        <div class="panel-body">
          <div class="section">
            <div class="section-title">公约概况</div>
            <div class="stat-grid">
              <div class="stat-item">
                <div class="stat-label">缔约方数量</div>
                <div class="stat-number">{{ ramsarData.contractingParties }}</div>
                <div class="stat-desc">个国家/地区</div>
              </div>
              <div class="stat-item">
                <div class="stat-label">国际重要湿地</div>
                <div class="stat-number">{{ ramsarData.totalSites }}</div>
                <div class="stat-desc">个站点</div>
              </div>
            </div>
            <div class="stat-highlight secondary">
              <span class="stat-value">{{ ramsarData.totalArea }}</span>
              <span class="stat-unit">{{ ramsarData.totalAreaUnit }}</span>
            </div>
            <div class="stat-detail">国际重要湿地总面积</div>
          </div>
          
          <div class="section">
            <div class="section-title">站点数量排名</div>
            <div class="rank-list">
              <div class="rank-item" v-for="country in topRamsarCountries" :key="country.rank">
                <div class="rank-badge">{{ country.rank }}</div>
                <div class="rank-country">{{ country.country }}</div>
                <div class="rank-sites">{{ country.sites }} 个</div>
              </div>
            </div>
          </div>
          
          <div class="section">
            <div class="section-title">生态功能指标</div>
            <div class="indicator-list">
              <div class="indicator-item" v-for="indicator in ecologicalIndicators" :key="indicator.name">
                <div class="indicator-name">{{ indicator.name }}</div>
                <div class="indicator-value">{{ indicator.value }}</div>
              </div>
            </div>
          </div>
        </div>
        
        <div class="panel-footer">
          <span class="source-label">数据来源：{{ ramsarData.dataSource }}</span>
        </div>
    </div>
    
    <div class="back-button" @click="goBack">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M19 12H5M12 19l-7-7 7-7"/>
      </svg>
      <span>返回</span>
    </div>
  </div>
</template>

<style scoped>
.globe-container {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
  background: linear-gradient(160deg, #ece8e3 0%, #e0dbd5 30%, #d5d0c9 60%, #cec9c2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.globe-nav-bar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 96px;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
  pointer-events: none;
  background: linear-gradient(180deg, rgba(236, 232, 227, 1) 0%, rgba(236, 232, 227, 0.85) 40%, rgba(236, 232, 227, 0) 100%);
}

.globe-nav-bar::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 10%;
  right: 10%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(106, 96, 88, 0.2) 20%, rgba(106, 96, 88, 0.2) 80%, transparent);
}

.globe-title {
  font-family: 'Georgia', 'Times New Roman', serif;
  font-size: clamp(32px, 3.1vw, 58px);
  font-weight: 400;
  letter-spacing: clamp(4px, 0.6vw, 10px);
  color: #5a524a;
  margin: 0;
  padding: 0 28px;
  text-align: center;
  white-space: normal;
  line-height: 1;
  position: relative;
  text-shadow:
    0 0 40px rgba(106, 96, 88, 0.08),
    0 0 80px rgba(106, 96, 88, 0.05),
    0 0 120px rgba(106, 96, 88, 0.03),
    0 2px 6px rgba(0, 0, 0, 0.04);
}

.globe-title::before,
.globe-title::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 48px;
  height: 1.5px;
  background: linear-gradient(90deg, transparent, rgba(90, 82, 74, 0.4));
}

.globe-title::before {
  left: -120px;
}

.globe-title::after {
  right: -120px;
  background: linear-gradient(90deg, rgba(90, 82, 74, 0.4), transparent);
}

.left-panel,
.right-panel {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: clamp(240px, 18vw, 340px);
  height: min(62vh, 640px);
  background: linear-gradient(180deg, #f0ece7 0%, #e4dfd9 100%);
  border: 1px solid rgba(106, 96, 88, 0.12);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.06), inset 0 1px 0 rgba(255, 255, 255, 0.5);
  z-index: 10;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-rendering: optimizeLegibility;
}

.left-panel {
  left: 16px;
}

.right-panel {
  right: 16px;
}

.panel-header {
  display: flex;
  align-items: baseline;
  gap: 16px;
  padding: 22px 24px 16px;
  border-bottom: 1px solid rgba(58, 106, 90, 0.12);
  flex-shrink: 0;
}

.panel-title {
  font-family: 'Georgia', 'Times New Roman', serif;
  font-size: 30px;
  font-weight: 400;
  color: #2a4a3a;
  margin: 0;
  letter-spacing: 3px;
}

.panel-subtitle {
  font-family: 'Georgia', serif;
  font-size: 14px;
  color: rgba(42, 74, 58, 0.5);
  letter-spacing: 2px;
}

.panel-body {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 18px 24px;
}

.panel-body::-webkit-scrollbar {
  width: 6px;
}

.panel-body::-webkit-scrollbar-track {
  background: transparent;
}

.panel-body::-webkit-scrollbar-thumb {
  background: rgba(58, 106, 90, 0.3);
  border-radius: 3px;
}

.section {
  margin-bottom: 20px;
}

.section-title {
  font-family: 'Georgia', serif;
  font-size: 18px;
  font-weight: 400;
  color: rgba(42, 74, 58, 0.7);
  margin-bottom: 12px;
  padding-left: 12px;
  border-left: 4px solid rgba(58, 106, 90, 0.5);
  letter-spacing: 1px;
}

.stat-highlight {
  background: linear-gradient(135deg, rgba(58, 106, 90, 0.08) 0%, rgba(58, 106, 90, 0.04) 100%);
  border-radius: 12px;
  padding: 18px;
  text-align: center;
  margin-bottom: 16px;
}

.stat-highlight.secondary {
  background: linear-gradient(135deg, rgba(58, 106, 90, 0.06) 0%, rgba(58, 106, 90, 0.02) 100%);
}

.stat-value {
  font-family: 'Georgia', serif;
  font-size: 44px;
  font-weight: 400;
  color: #2a4a3a;
}

.stat-unit {
  font-family: 'Georgia', serif;
  font-size: 18px;
  color: rgba(42, 74, 58, 0.6);
  margin-left: 12px;
}

.stat-detail {
  font-family: 'Georgia', serif;
  font-size: 16px;
  color: rgba(42, 74, 58, 0.55);
  text-align: center;
  letter-spacing: 2px;
}

.stat-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 12px;
}

.stat-item {
  background: rgba(58, 106, 90, 0.04);
  border-radius: 12px;
  padding: 14px;
  text-align: center;
}

.stat-label {
  font-family: 'Georgia', serif;
  font-size: 14px;
  color: rgba(42, 74, 58, 0.6);
  margin-bottom: 12px;
}

.stat-number {
  font-family: 'Georgia', serif;
  font-size: 30px;
  font-weight: 400;
  color: #2a4a3a;
}

.stat-desc {
  font-family: 'Georgia', serif;
  font-size: 13px;
  color: rgba(42, 74, 58, 0.5);
}

.region-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.region-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.region-name {
  font-family: 'Georgia', serif;
  font-size: 16px;
  color: rgba(42, 74, 58, 0.7);
  width: 88px;
  flex-shrink: 0;
}

.region-bar-container {
  flex: 1;
  height: 8px;
  background: rgba(58, 106, 90, 0.1);
  border-radius: 6px;
  overflow: hidden;
}

.region-bar {
  height: 100%;
  background: linear-gradient(90deg, rgba(58, 106, 90, 0.6) 0%, rgba(90, 138, 122, 0.8) 100%);
  border-radius: 6px;
  transition: width 0.3s ease;
}

.region-area {
  font-family: 'Georgia', serif;
  font-size: 13px;
  color: rgba(42, 74, 58, 0.5);
  width: 88px;
  text-align: right;
}

.type-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.type-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: rgba(58, 106, 90, 0.03);
  border-radius: 12px;
}

.type-color {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.type-info {
  display: flex;
  justify-content: space-between;
  flex: 1;
}

.type-name {
  font-family: 'Georgia', serif;
  font-size: 15px;
  color: rgba(42, 74, 58, 0.7);
}

.type-percentage {
  font-family: 'Georgia', serif;
  font-size: 15px;
  font-weight: 400;
  color: #2a4a3a;
}

.rank-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: rgba(58, 106, 90, 0.03);
  border-radius: 12px;
}

.rank-badge {
  width: 28px;
  height: 28px;
  background: linear-gradient(135deg, rgba(58, 106, 90, 0.7) 0%, rgba(90, 138, 122, 0.9) 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: 'Georgia', serif;
  font-size: 13px;
  font-weight: 400;
  color: #FFFFFF;
  flex-shrink: 0;
}

.rank-country {
  flex: 1;
  font-family: 'Georgia', serif;
  font-size: 15px;
  color: rgba(42, 74, 58, 0.7);
}

.rank-sites {
  font-family: 'Georgia', serif;
  font-size: 15px;
  font-weight: 400;
  color: #2a4a3a;
}

.indicator-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.indicator-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: rgba(58, 106, 90, 0.03);
  border-radius: 12px;
}

.indicator-name {
  font-family: 'Georgia', serif;
  font-size: 15px;
  color: rgba(42, 74, 58, 0.7);
}

.indicator-value {
  font-family: 'Georgia', serif;
  font-size: 16px;
  font-weight: 400;
  color: #2a4a3a;
}

.panel-footer {
  padding: 14px 24px;
  border-top: 1px solid rgba(58, 106, 90, 0.12);
  display: flex;
  justify-content: center;
  flex-shrink: 0;
}

.source-label {
  font-family: 'Georgia', serif;
  font-size: 13px;
  color: rgba(42, 74, 58, 0.55);
  letter-spacing: 2px;
}

.globe-center {
  width: 100%;
  height: 100%;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.globe-canvas {
  width: 100%;
  height: 100%;
  position: absolute;
  top: 0;
  left: 0;
  z-index: 1;
}

.country-tooltip {
  display: none;
  position: fixed;
  padding: 10px 14px;
  background: rgba(240, 236, 231, 0.95);
  color: #2a4a3a;
  border-radius: 12px;
  font-family: 'Georgia', serif;
  font-size: 16px;
  pointer-events: none;
  z-index: 9999;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(106, 96, 88, 0.15);
}

.loading-overlay {
  display: none;
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(160deg, #ece8e3 0%, #e0dbd5 30%, #d5d0c9 60%, #cec9c2 100%);
  z-index: 100;
  flex-direction: column;
  justify-content: center;
  align-items: center;
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

@keyframes spin {
  to { transform: rotate(360deg); }
}

@keyframes pulseGlow {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
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

.back-button {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 20px;
  background: linear-gradient(135deg, rgba(232, 228, 223, 0.96), rgba(220, 215, 208, 0.96));
  border: 2px solid rgba(106, 96, 88, 0.25);
  border-radius: 999px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  z-index: 100;
  transition: all 0.3s ease;
}

.back-button svg {
  width: 18px;
  height: 18px;
  color: #6a6058;
}

.back-button span {
  font-family: 'Georgia', serif;
  font-size: 15px;
  color: #6a6058;
  letter-spacing: 1px;
}

.back-button:hover {
  transform: translateX(-50%) scale(1.05);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
  border-color: rgba(106, 96, 88, 0.4);
}

@media (max-width: 1400px) {
  .left-panel,
  .right-panel {
    width: clamp(250px, 24vw, 340px);
  }
  
  .left-panel {
    left: 20px;
  }
  
  .right-panel {
    right: 20px;
  }
}

@media (max-width: 1100px) {
  .left-panel,
  .right-panel {
    display: none;
  }
}
</style>
