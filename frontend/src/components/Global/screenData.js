import api from '../../utils/api'
import { resolveAssetUrl } from '../../utils/assets'

const SCREEN_DASHBOARD_ENDPOINT = import.meta.env.VITE_SCREEN_DASHBOARD_ENDPOINT || ''
const SCREEN_WETLAND_SPOTS_ENDPOINT = import.meta.env.VITE_SCREEN_WETLAND_SPOTS_ENDPOINT || '/consult/wetlands/spots'
const CHINA_COORDINATE_BOUNDS = {
  minLng: 73,
  maxLng: 135,
  minLat: 3,
  maxLat: 54
}
const PROVINCE_CENTER_MAP = {
  '北京市': { lng: 116.40, lat: 39.90 },
  '天津市': { lng: 117.20, lat: 39.12 },
  '上海市': { lng: 121.47, lat: 31.23 },
  '重庆市': { lng: 106.55, lat: 29.57 },
  '河北省': { lng: 114.48, lat: 38.03 },
  '山西省': { lng: 112.55, lat: 37.87 },
  '辽宁省': { lng: 123.43, lat: 41.80 },
  '吉林省': { lng: 125.32, lat: 43.90 },
  '黑龙江省': { lng: 126.53, lat: 45.80 },
  '江苏省': { lng: 118.78, lat: 32.04 },
  '浙江省': { lng: 120.15, lat: 30.28 },
  '安徽省': { lng: 117.28, lat: 31.86 },
  '福建省': { lng: 119.30, lat: 26.08 },
  '江西省': { lng: 115.89, lat: 28.68 },
  '山东省': { lng: 117.00, lat: 36.65 },
  '河南省': { lng: 113.62, lat: 34.75 },
  '湖北省': { lng: 114.30, lat: 30.59 },
  '湖南省': { lng: 112.93, lat: 28.23 },
  '广东省': { lng: 113.27, lat: 23.13 },
  '海南省': { lng: 110.35, lat: 20.02 },
  '四川省': { lng: 104.07, lat: 30.67 },
  '贵州省': { lng: 106.71, lat: 26.57 },
  '云南省': { lng: 102.71, lat: 25.04 },
  '陕西省': { lng: 108.95, lat: 34.27 },
  '甘肃省': { lng: 103.82, lat: 36.07 },
  '青海省': { lng: 101.78, lat: 36.62 },
  '台湾省': { lng: 121.56, lat: 25.03 },
  '内蒙古自治区': { lng: 111.67, lat: 40.82 },
  '广西壮族自治区': { lng: 108.32, lat: 22.82 },
  '西藏自治区': { lng: 91.13, lat: 29.65 },
  '宁夏回族自治区': { lng: 106.27, lat: 38.47 },
  '新疆维吾尔自治区': { lng: 87.62, lat: 43.82 },
  '香港特别行政区': { lng: 114.17, lat: 22.32 },
  '澳门特别行政区': { lng: 113.55, lat: 22.20 }
}
const PROVINCE_ALIAS_MAP = {
  '北京': '北京市',
  '天津': '天津市',
  '上海': '上海市',
  '重庆': '重庆市',
  '河北': '河北省',
  '山西': '山西省',
  '辽宁': '辽宁省',
  '吉林': '吉林省',
  '黑龙江': '黑龙江省',
  '江苏': '江苏省',
  '浙江': '浙江省',
  '安徽': '安徽省',
  '福建': '福建省',
  '江西': '江西省',
  '山东': '山东省',
  '河南': '河南省',
  '湖北': '湖北省',
  '湖南': '湖南省',
  '广东': '广东省',
  '海南': '海南省',
  '四川': '四川省',
  '贵州': '贵州省',
  '云南': '云南省',
  '陕西': '陕西省',
  '甘肃': '甘肃省',
  '青海': '青海省',
  '台湾': '台湾省',
  '内蒙古': '内蒙古自治区',
  '广西': '广西壮族自治区',
  '西藏': '西藏自治区',
  '宁夏': '宁夏回族自治区',
  '新疆': '新疆维吾尔自治区',
  '香港': '香港特别行政区',
  '澳门': '澳门特别行政区'
}

const FALLBACK_PROVINCE_STATS = {
  '黑龙江省': { area: 514.3, rank: 3, landArea: 4730, source: 'https://m.hljnews.cn', sourceName: '黑龙江省林业和草原局' },
  '内蒙古自治区': { area: 485.52, rank: 4, landArea: 11830, source: 'https://www.cyhq.gov.cn', sourceName: '内蒙古自治区林草局' },
  '西藏自治区': { area: 543.7, rank: 2, landArea: 12284, source: 'https://www.mnr.gov.cn', sourceName: '自然资源部' },
  '青海省': { area: 814.0, rank: 1, landArea: 7212, source: 'https://www.mnr.gov.cn', sourceName: '自然资源部' },
  '新疆维吾尔自治区': { area: 394.8, rank: 5, landArea: 16649, source: 'https://www.mnr.gov.cn', sourceName: '自然资源部' },
  '四川省': { area: 123.1, rank: 6, landArea: 4860, source: 'https://sthjt.sc.gov.cn', sourceName: '四川省生态环境厅' },
  '江苏省': { area: 312.0, rank: 6, landArea: 1072, source: 'https://www.mnr.gov.cn', sourceName: '自然资源部' },
  '广东省': { area: 190.68, rank: 8, landArea: 1798, source: 'https://lyj.gd.gov.cn', sourceName: '广东省林业局' },
  '湖北省': { area: 174.7, rank: 9, landArea: 1859, source: 'https://www.mnr.gov.cn', sourceName: '自然资源部' },
  '湖南省': { area: 102.5, rank: 12, landArea: 2118, source: 'https://www.mnr.gov.cn', sourceName: '自然资源部' },
  '江西省': { area: 91.0, rank: 14, landArea: 1669, source: 'https://www.mnr.gov.cn', sourceName: '自然资源部' },
  '浙江省': { area: 111.0, rank: 11, landArea: 1055, source: 'https://www.mnr.gov.cn', sourceName: '自然资源部' },
  '安徽省': { area: 76.0, rank: 16, landArea: 1401, source: 'https://www.mnr.gov.cn', sourceName: '自然资源部' },
  '山东省': { area: 173.8, rank: 10, landArea: 1579, source: 'https://www.mnr.gov.cn', sourceName: '自然资源部' },
  '河南省': { area: 62.0, rank: 18, landArea: 1670, source: 'https://www.mnr.gov.cn', sourceName: '自然资源部' },
  '河北省': { area: 48.0, rank: 20, landArea: 1888, source: 'https://www.mnr.gov.cn', sourceName: '自然资源部' },
  '山西省': { area: 36.0, rank: 22, landArea: 1567, source: 'https://www.mnr.gov.cn', sourceName: '自然资源部' },
  '辽宁省': { area: 137.0, rank: 13, landArea: 1480, source: 'https://www.mnr.gov.cn', sourceName: '自然资源部' },
  '吉林省': { area: 99.0, rank: 15, landArea: 1911, source: 'https://www.mnr.gov.cn', sourceName: '自然资源部' },
  '云南省': { area: 56.0, rank: 19, landArea: 3941, source: 'https://www.mnr.gov.cn', sourceName: '自然资源部' },
  '贵州省': { area: 32.0, rank: 23, landArea: 1762, source: 'https://www.mnr.gov.cn', sourceName: '自然资源部' },
  '广西壮族自治区': { area: 75.0, rank: 17, landArea: 2376, source: 'https://www.mnr.gov.cn', sourceName: '自然资源部' },
  '海南省': { area: 32.0, rank: 23, landArea: 354, source: 'https://www.mnr.gov.cn', sourceName: '自然资源部' },
  '福建省': { area: 87.0, rank: 14, landArea: 1240, source: 'https://www.mnr.gov.cn', sourceName: '自然资源部' },
  '陕西省': { area: 30.0, rank: 24, landArea: 2058, source: 'https://www.mnr.gov.cn', sourceName: '自然资源部' },
  '甘肃省': { area: 169.0, rank: 10, landArea: 4258, source: 'https://www.mnr.gov.cn', sourceName: '自然资源部' },
  '宁夏回族自治区': { area: 18.0, rank: 26, landArea: 664, source: 'https://www.mnr.gov.cn', sourceName: '自然资源部' },
  '北京市': { area: 5.0, rank: 30, landArea: 164, source: 'https://www.mnr.gov.cn', sourceName: '自然资源部' },
  '天津市': { area: 17.0, rank: 27, landArea: 119, source: 'https://www.mnr.gov.cn', sourceName: '自然资源部' },
  '上海市': { area: 46.0, rank: 21, landArea: 63, source: 'https://www.mnr.gov.cn', sourceName: '自然资源部' },
  '重庆市': { area: 20.0, rank: 25, landArea: 824, source: 'https://www.mnr.gov.cn', sourceName: '自然资源部' },
  '台湾省': { area: 12.0, rank: 28, landArea: 360, source: 'https://www.mnr.gov.cn', sourceName: '自然资源部' },
  '香港特别行政区': { area: 0.5, rank: 31, landArea: 11, source: 'https://www.mnr.gov.cn', sourceName: '自然资源部' },
  '澳门特别行政区': { area: 0.05, rank: 32, landArea: 0.33, source: 'https://www.mnr.gov.cn', sourceName: '自然资源部' }
}

const FALLBACK_SPOTS = [
  { id: 1, name: '贵州省遵义市赤水河国家重要湿地', lng: 105.8583, lat: 28.0833, image: '/湿地图片/贵州省遵义市赤水河国家重要湿地.png', description: '长江上游唯一一条干流无筑坝的原生态河流湿地。' },
  { id: 2, name: '湖北省荆州市天鹅洲国家重要湿地', lng: 112.5667, lat: 29.8, image: '/湿地图片/湖北省荆州市天鹅洲国家重要湿地.png', description: '长江中游典型的河流故道型湿地。' },
  { id: 3, name: '江苏省南京市长江新济洲国家重要湿地', lng: 118.575, lat: 31.7917, image: '/湿地图片/江苏省南京市长江新济洲国家重要湿地.png', description: '长江下游首个国家级江心洲型湿地。' },
  { id: 4, name: '辽宁省盘锦市红海滩湿地', lng: 121.7333, lat: 40.9417, image: '/湿地图片/辽宁省盘锦市红海滩湿地.png', description: '全球保存完好的滨海芦苇沼泽湿地。' },
  { id: 6, name: '四川省阿坝藏族羌族自治州九寨沟湿地', lng: 103.9167, lat: 33.1083, image: '/湿地图片/四川省阿坝藏族羌族自治州九寨沟湿地.png', description: '世界自然遗产地代表性高山湿地。' }
]

const FALLBACK_DATASET = {
  overview: {
    protectionRate: 55,
    totalArea: 5635,
    totalAreaUnit: '万公顷',
    nationalRank: '亚洲第一',
    globalRank: '世界第四',
    sourceLabel: '国家林业和草原局、全国湿地保护规划（2022—2030 年）',
    sourceUrl: 'https://www.forestry.gov.cn'
  },
  protectionResults: {
    categories: ['保护修复项目\n(个)', '新增修复湿地\n(万公顷)', '红树林营造\n(公顷)', '互花米草防治\n(万公顷)', '湿地公园保护\n(万公顷)'],
    items: [
      { name: '累计实施保护修复项目', value: 3800, unit: '个', axisIndex: 0 },
      { name: '新增和修复湿地面积', value: 100, unit: '万公顷', axisIndex: 0 },
      { name: '红树林营造面积', value: 9200, unit: '公顷', axisIndex: 1 },
      { name: '互花米草防治面积', value: 9.73, unit: '万公顷', axisIndex: 0 },
      { name: '国家湿地公园有效保护面积', value: 240, unit: '万公顷', axisIndex: 0 }
    ],
    sourceLabel: '国家林草局 2026 年湿地保护成效发布',
    sourceUrl: 'https://www.forestry.gov.cn'
  },
  wetlandTypes: {
    items: [
      { name: '沼泽湿地', value: 1197, category: '天然湿地' },
      { name: '天然湖泊', value: 910, category: '天然湿地' },
      { name: '河流湿地', value: 487, category: '天然湿地' },
      { name: '稻田湿地', value: 3800, category: '人工湿地' },
      { name: '水库湿地', value: 200, category: '人工湿地' }
    ],
    sourceLabel: '国家林业和草原局',
    sourceUrl: 'https://www.forestry.gov.cn'
  },
  protectionSystem: {
    innerData: [
      { value: 64, name: '国际层面' },
      { value: 29, name: '国家层面' },
      { value: 601, name: '省级层面' }
    ],
    outerData: [
      { value: 64, name: '国际重要湿地', level: '国际' },
      { value: 29, name: '国家重要湿地', level: '国家' },
      { value: 903, name: '省级重要湿地', level: '省级' }
    ],
    sourceLabel: '国家林草局',
    sourceUrl: 'https://www.forestry.gov.cn'
  },
  wetlandTrend: {
    years: ['2018', '2019', '2020', '2021', '2022', '2023', '2024', '2025'],
    values: [57, 57, 64, 64, 64, 82, 82, 82],
    sourceLabel: '国家林草局《国际重要湿地名录》',
    sourceUrl: 'https://www.gov.cn/xinwen/2023-02/02/content_5739608.htm'
  },
  biodiversity: {
    items: [
      { name: '湿地水鸟', value: 296, detail: '国家重点保护水鸟：91 种' },
      { name: '湿地动物', value: 724, detail: '鱼类达 1000 多种' },
      { name: '湿地植物', value: 1691, detail: '珍稀濒危植物：73 种' }
    ],
    sourceLabel: '国家林草局湿地资源调查成果',
    sourceUrl: 'https://www.forestry.gov.cn'
  },
  endangeredSpecies: {
    years: ['2015', '2018', '2021', '2024', '2025'],
    series: [
      { name: '东方白鹳', values: [3000, 3500, 4200, 5800, 6800] },
      { name: '黑脸琵鹭', values: [3356, 3941, 5222, 6633, 6988] },
      { name: '白鹤', values: [3200, 3800, 4500, 5200, 5600] }
    ],
    sourceLabel: '国家林草局全国越冬水鸟同步调查',
    sourceUrl: 'https://www.forestry.gov.cn'
  },
  touristTrend: {
    years: ['2019', '2020', '2021', '2022', '2023', '2024'],
    values: [2.5, 2.1, 2.8, 3.0, 3.1, 3.2],
    sourceLabel: '国家林草局',
    sourceUrl: 'https://www.forestry.gov.cn'
  },
  ecosystemServices: {
    indicators: [
      { name: '涵养水源', max: 100 },
      { name: '固碳释氧', max: 100 },
      { name: '净化水质', max: 100 },
      { name: '维持生物多样性', max: 100 },
      { name: '防洪蓄洪', max: 100 },
      { name: '气候调节', max: 100 }
    ],
    values: [95, 78, 82, 98, 88, 75],
    sourceLabel: '中科院东北地理所、国家林草局',
    sourceUrl: 'https://iga.cas.cn'
  },
  soilCarbon: {
    total: 99,
    items: [
      { name: '青藏高原', value: 53 },
      { name: '东北地区', value: 33 },
      { name: '西北干旱半干旱区', value: 13 }
    ],
    sourceLabel: '中科院东北地理所《中国湿地研究报告》',
    sourceUrl: 'https://iga.cas.cn'
  },
  vegetationCarbon: {
    years: ['2000', '2005', '2010', '2015', '2020', '2023'],
    values: [4800, 5100, 5300, 5500, 5750, 5950],
    sourceLabel: '中科院东北地理所',
    sourceUrl: 'https://iga.cas.cn'
  },
  waterConservation: {
    value: 8038.53,
    maxValue: 10000,
    unit: '亿立方米/年',
    sourceLabel: '国家林草局',
    sourceUrl: 'https://www.forestry.gov.cn'
  },
  provinceStats: FALLBACK_PROVINCE_STATS,
  wetlandSpots: FALLBACK_SPOTS
}

let cachedDataset = null
let datasetPromise = null

function isObject(value) {
  return value && typeof value === 'object' && !Array.isArray(value)
}

function deepMerge(base, incoming) {
  if (!isObject(base) || !isObject(incoming)) {
    return incoming ?? base
  }
  const result = { ...base }
  Object.keys(incoming).forEach((key) => {
    const sourceValue = incoming[key]
    const baseValue = base[key]
    if (Array.isArray(sourceValue)) {
      result[key] = sourceValue
      return
    }
    if (isObject(sourceValue) && isObject(baseValue)) {
      result[key] = deepMerge(baseValue, sourceValue)
      return
    }
    result[key] = sourceValue
  })
  return result
}

function firstDefined(...values) {
  return values.find((value) => value !== undefined && value !== null && value !== '')
}

function normalizePathSlashes(path) {
  return String(path).replace(/\\/g, '/')
}

function isFiniteCoordinate(value) {
  return Number.isFinite(Number(value))
}

function isWithinChinaBounds(lng, lat) {
  return isFiniteCoordinate(lng)
    && isFiniteCoordinate(lat)
    && Number(lng) >= CHINA_COORDINATE_BOUNDS.minLng
    && Number(lng) <= CHINA_COORDINATE_BOUNDS.maxLng
    && Number(lat) >= CHINA_COORDINATE_BOUNDS.minLat
    && Number(lat) <= CHINA_COORDINATE_BOUNDS.maxLat
}

function normalizeCoordinateNumberPair(lng, lat) {
  if (!isFiniteCoordinate(lng) || !isFiniteCoordinate(lat)) {
    return null
  }

  const direct = { lng: Number(lng), lat: Number(lat) }
  if (isWithinChinaBounds(direct.lng, direct.lat)) {
    return direct
  }

  const swapped = { lng: Number(lat), lat: Number(lng) }
  if (isWithinChinaBounds(swapped.lng, swapped.lat)) {
    return swapped
  }

  return null
}

function inferProvinceName(item) {
  const directProvince = firstDefined(item.province, item.provinceName, item.regionProvince, item.areaProvince)
  if (directProvince) {
    const normalized = String(directProvince).trim()
    if (PROVINCE_CENTER_MAP[normalized]) return normalized
    if (PROVINCE_ALIAS_MAP[normalized]) return PROVINCE_ALIAS_MAP[normalized]
  }

  const haystack = [
    item.name,
    item.wetlandName,
    item.title,
    item.address,
    item.region,
    item.location,
    item.description,
    item.summary
  ]
    .filter(Boolean)
    .map((entry) => String(entry))
    .join(' ')

  if (!haystack) return ''

  const aliases = Object.keys(PROVINCE_ALIAS_MAP).sort((a, b) => b.length - a.length)
  const matchedAlias = aliases.find((alias) => haystack.includes(alias))
  return matchedAlias ? PROVINCE_ALIAS_MAP[matchedAlias] : ''
}

function inferProvinceCenter(item) {
  const provinceName = inferProvinceName(item)
  return provinceName ? PROVINCE_CENTER_MAP[provinceName] || null : null
}

function normalizeSpotImage(path) {
  if (!path) return ''
  const normalizedPath = normalizePathSlashes(path)

  if (/^https?:\/\//i.test(normalizedPath)) return normalizedPath
  if (normalizedPath.startsWith('/')) return resolveAssetUrl(normalizedPath)

  return resolveAssetUrl(`/${normalizedPath}`)
}

function extractCoordinateValue(rawValue) {
  if (rawValue === undefined || rawValue === null || rawValue === '') {
    return null
  }

  const numericValue = Number(rawValue)
  if (Number.isFinite(numericValue)) {
    return numericValue
  }

  const normalized = String(rawValue).replace(/[^\d.\-]/g, '')
  if (!normalized) {
    return null
  }

  const parsed = Number(normalized)
  return Number.isFinite(parsed) ? parsed : null
}

function parseCoordinatePair(value) {
  if (!value) {
    return null
  }

  if (Array.isArray(value) && value.length >= 2) {
    const lng = extractCoordinateValue(value[0])
    const lat = extractCoordinateValue(value[1])
    if (Number.isFinite(lng) && Number.isFinite(lat)) {
      return { lng, lat }
    }
  }

  if (typeof value === 'object') {
    const lng = extractCoordinateValue(firstDefined(
      value.lng,
      value.longitude,
      value.lon,
      value.x,
      value.coordLng,
      value.locationLng
    ))
    const lat = extractCoordinateValue(firstDefined(
      value.lat,
      value.latitude,
      value.y,
      value.coordLat,
      value.locationLat
    ))

    if (Number.isFinite(lng) && Number.isFinite(lat)) {
      return { lng, lat }
    }
  }

  const text = String(value).trim()
  if (!text) {
    return null
  }

  if (/东经|西经|北纬|南纬/.test(text)) {
    return null
  }

  const matches = text.match(/-?\d+(?:\.\d+)?/g)
  if (!matches || matches.length < 2) {
    return null
  }

  return normalizeCoordinateNumberPair(Number(matches[0]), Number(matches[1]))
}

function parseCoordinateRange(value) {
  if (!value) {
    return null
  }

  const text = String(value).trim()
  if (!text) {
    return null
  }

  const lngMatch = text.match(/[东E]经?\s*([^，,；;北纬南纬]+)/)
  const latMatch = text.match(/[北N]纬?\s*(.+)$/)

  if (!lngMatch || !latMatch) {
    return parseCoordinatePair(text)
  }

  const parseDmsValue = (input) => {
    const normalized = String(input)
      .replace(/[，,]/g, '')
      .replace(/\s+/g, '')
      .replace(/[º˚]/g, '°')
      .replace(/[’']/g, '′')
      .replace(/[”"]/g, '″')
    const dmsMatch = normalized.match(/(\d+(?:\.\d+)?)°(?:(\d+(?:\.\d+)?)′)?(?:(\d+(?:\.\d+)?)″)?/)
    if (dmsMatch) {
      const degrees = Number(dmsMatch[1] || 0)
      const minutes = Number(dmsMatch[2] || 0)
      const seconds = Number(dmsMatch[3] || 0)
      return degrees + minutes / 60 + seconds / 3600
    }

    return extractCoordinateValue(normalized)
  }

  const parseRangeMiddle = (input) => {
    const parts = String(input).split(/(?:~|～|—|－|-|至)/).map((item) => item.trim()).filter(Boolean)
    if (parts.length === 0) {
      return null
    }
    if (parts.length === 1) {
      return parseDmsValue(parts[0])
    }

    const start = parseDmsValue(parts[0])
    const end = parseDmsValue(parts[1])
    if (!Number.isFinite(start) || !Number.isFinite(end)) {
      return null
    }

    return (start + end) / 2
  }

  return normalizeCoordinateNumberPair(parseRangeMiddle(lngMatch[1]), parseRangeMiddle(latMatch[1]))
}

function resolveSpotCoordinate(item, parsedCoordinate) {
  const directCoordinate = normalizeCoordinateNumberPair(
    firstDefined(
      extractCoordinateValue(firstDefined(
        item.lng,
        item.longitude,
        item.lon,
        item.x,
        item.coordLng,
        item.locationLng,
        item.wgs84Lng
      )),
      parsedCoordinate?.lng
    ),
    firstDefined(
      extractCoordinateValue(firstDefined(
        item.lat,
        item.latitude,
        item.y,
        item.coordLat,
        item.locationLat,
        item.wgs84Lat
      )),
      parsedCoordinate?.lat
    )
  )

  if (directCoordinate) {
    return directCoordinate
  }

  return inferProvinceCenter(item)
}

function normalizeWetlandSpot(item, index = 0) {
  const nestedCoordinate = firstDefined(
    item.coordinate,
    item.coordinates,
    item.position,
    item.location,
    item.locationPoint,
    item.geo,
    item.center,
    item.coordinateRange,
    item.coordinate_range,
    item.coordinatesText
  )
  const parsedCoordinate = parseCoordinateRange(nestedCoordinate) || parseCoordinatePair(nestedCoordinate)
  const coordinate = resolveSpotCoordinate(item, parsedCoordinate)

  if (!coordinate) return null

  return {
    id: firstDefined(item.id, item.spotId, item.wetlandId, item.wetlandSpotId, index + 1),
    name: firstDefined(
      item.name,
      item.wetlandName,
      item.wetland_name,
      item.title,
      item.wetlandTitle,
      item.wetland_title,
      `湿地点位 ${index + 1}`
    ),
    lng: coordinate.lng,
    lat: coordinate.lat,
    provinceName: inferProvinceName(item),
    image: normalizeSpotImage(firstDefined(
      item.image,
      item.imageUrl,
      item.imagePath,
      item.cover,
      item.coverUrl,
      item.coverImage,
      item.picture,
      item.photo,
      item.thumbnail
    )),
    description: firstDefined(
      item.description,
      item.summary,
      item.introduction,
      item.content,
      item.detail,
      item.wetlandDescription,
      item.wetland_description,
      item.intro,
      item.brief,
      ''
    )
  }
}

function normalizeWetlandSpots(payload) {
  const items = Array.isArray(payload)
    ? payload
    : payload?.wetlands || payload?.items || payload?.records || payload?.list || payload?.data || []
  if (!Array.isArray(items)) {
    return FALLBACK_SPOTS
  }
  const normalized = items
    .map((item, index) => normalizeWetlandSpot(item, index))
    .filter(Boolean)
  return normalized.length > 0 ? normalized : FALLBACK_SPOTS
}

function normalizeProvinceStats(payload) {
  if (!isObject(payload)) return FALLBACK_PROVINCE_STATS
  const normalized = {}
  Object.entries(payload).forEach(([provinceName, value]) => {
    if (!isObject(value)) return
    normalized[provinceName] = {
      area: Number(firstDefined(value.area, value.wetlandArea, value.totalArea)),
      rank: Number(firstDefined(value.rank, value.ranking, value.order)),
      landArea: Number(firstDefined(value.landArea, value.provinceArea, value.totalLandArea)),
      source: firstDefined(value.source, value.sourceUrl, 'https://www.mnr.gov.cn'),
      sourceName: firstDefined(value.sourceName, value.sourceLabel, '自然资源部')
    }
  })
  return Object.keys(normalized).length > 0 ? normalized : FALLBACK_PROVINCE_STATS
}

function normalizeDataset(payload) {
  const raw = payload?.data ?? payload ?? {}
  const merged = deepMerge(FALLBACK_DATASET, raw)
  merged.provinceStats = normalizeProvinceStats(raw.provinceStats ?? raw.provinceData ?? merged.provinceStats)
  merged.wetlandSpots = normalizeWetlandSpots(raw.wetlandSpots ?? raw.spots ?? merged.wetlandSpots)
  return merged
}

export function getFallbackScreenDataset() {
  return FALLBACK_DATASET
}

export async function loadScreenDataset(force = false) {
  if (!force && cachedDataset) {
    return cachedDataset
  }
  if (!force && datasetPromise) {
    return datasetPromise
  }

  if (!SCREEN_DASHBOARD_ENDPOINT) {
    cachedDataset = FALLBACK_DATASET
    return cachedDataset
  }

  datasetPromise = api.get(SCREEN_DASHBOARD_ENDPOINT)
    .then((response) => {
      cachedDataset = normalizeDataset(response?.data)
      return cachedDataset
    })
    .catch((error) => {
      console.warn('加载大屏数据失败，已回退到本地默认数据。', error)
      cachedDataset = FALLBACK_DATASET
      return cachedDataset
    })
    .finally(() => {
      datasetPromise = null
    })

  return datasetPromise
}

export async function loadWetlandSpots() {
  const endpoints = [...new Set([
    SCREEN_WETLAND_SPOTS_ENDPOINT,
    '/consult/wetlands/spots',
    '/wetlands'
  ].filter(Boolean))]

  for (const endpoint of endpoints) {
    try {
      const response = await api.get(endpoint)
      return normalizeWetlandSpots(response?.data)
    } catch (error) {
      console.warn(`加载湿地点位失败，已尝试端点 ${endpoint}。`, error)
    }
  }

  return FALLBACK_SPOTS
}
