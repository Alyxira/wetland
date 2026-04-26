<template>
  <div class="science-page">
    <div class="background-layer">
      <div class="bg-image"></div>
      <div class="bg-overlay"></div>
    </div>

    <SystemNav />

    <main class="page-shell">
      <div v-if="pageLoading" class="state-card">
        <p>正在加载景区与珍稀动植物数据...</p>
      </div>

      <div v-else-if="pageError" class="state-card">
        <p>{{ pageError }}</p>
      </div>

      <template v-else>
        <section class="science-page-header">
          <h1>生态图鉴</h1>
        </section>

        <section class="hero-section">
          <div class="hero-copy">
            <span class="eyebrow"></span>
            <h2>{{ currentWetland?.wetlandName || '珍稀动植物档案' }}</h2>

            <div v-if="speciesLoading" class="state-card state-card--embedded">
              <p>正在加载珍稀动植物...</p>
            </div>

            <div v-else-if="speciesError" class="state-card state-card--embedded">
              <p>{{ speciesError }}</p>
            </div>

            <div v-else-if="floraFaunaList.length === 0" class="state-card state-card--embedded">
              <p>当前景区暂无珍稀动植物数据。</p>
            </div>

            <section v-else class="species-grid species-grid--featured">
              <article
                v-for="item in sortedFloraFaunaList"
                :key="item.id"
                class="species-card"
                role="button"
                tabindex="0"
                @click="openFloraDetail(item)"
                @keydown.enter="openFloraDetail(item)"
                @keydown.space.prevent="openFloraDetail(item)"
              >
                <div class="species-image-wrap">
                  <img :src="item.image" :alt="item.name" class="species-image" />
                </div>

                <div class="species-body">
                  <div class="species-head">
                    <span class="species-label"></span>
                    <span class="species-date">{{ item.createdTimeText }}</span>
                  </div>

                  <h3>{{ item.name }}</h3>
                  <p>{{ item.brief }}</p>

                  <div class="species-foot">
                    <strong>查看详情</strong>
                  </div>
                </div>
              </article>
            </section>
          </div>

          <article class="selector-panel">
            <p class="selector-label"></p>

            <div class="wetland-picker">
              <div class="wetland-picker__current">
                <span>当前景区</span>
                <strong>{{ currentWetland?.wetlandName || '未选择景区' }}</strong>
              </div>

              <div class="featured-wetlands">
                <div class="featured-wetlands__topline">
                  <span>推荐湿地</span>
                </div>

                <div class="featured-wetlands__grid">
                  <button
                    v-for="item in featuredWetlands"
                    :key="`featured-${item.id}`"
                    type="button"
                    :class="['featured-wetland-card', { 'is-active': String(item.id) === String(selectedWetlandId) }]"
                    @click="selectWetland(item, false)"
                  >
                    <span>{{ item.regionLabel }}</span>
                    <strong>{{ item.wetlandName }}</strong>
                    <em>{{ item.coordinateRange || '暂无坐标信息' }}</em>
                  </button>
                </div>
              </div>

              <div class="search-index-panel" data-y-slice-skip>
                <div class="search-index-panel__topline">
                  <span>检索湿地</span>
                  <strong>{{ filteredWetlandCount }} 个结果</strong>
                </div>

                <input
                  v-model.trim="wetlandKeyword"
                  type="text"
                  class="wetland-search"
                  placeholder="搜索湿地名称或地区"
                />

                <div class="wetland-search-actions">
                  <button
                    type="button"
                    class="wetland-search-clear"
                    :disabled="!wetlandKeyword"
                    @click="clearWetlandKeyword"
                  >
                    清空检索
                  </button>
                </div>

                <div v-if="groupedWetlands.length" class="region-index">
                  <button
                    v-for="group in groupedWetlands"
                    :key="group.region"
                    type="button"
                    class="region-index__chip"
                    @click="activeRegion = group.region"
                  >
                    {{ group.region }}
                  </button>
                </div>

                <div class="wetland-groups">
                  <section
                    v-for="group in groupedWetlands"
                    :key="group.region"
                    :ref="(el) => setGroupRef(el, group.region)"
                    class="wetland-group"
                  >
                    <div class="wetland-group__header">
                      <span>{{ group.region }}</span>
                      <strong>{{ group.items.length }}</strong>
                    </div>

                    <div class="wetland-results">
                      <button
                        v-for="item in group.items"
                        :key="item.id"
                        type="button"
                        :class="['wetland-result', { 'is-active': String(item.id) === String(selectedWetlandId) }]"
                        @click="selectWetland(item)"
                      >
                        <strong>{{ item.wetlandName }}</strong>
                        <span>{{ item.coordinateRange || '暂无坐标信息' }}</span>
                      </button>
                    </div>
                  </section>
                </div>
              </div>
            </div>

            <div class="selector-meta">
              <div class="meta-row">
                <span>景区坐标</span>
                <strong>{{ currentWetland?.coordinateRange || '暂无坐标信息' }}</strong>
              </div>
              <div class="meta-row">
                <span>物种数量</span>
                <strong>{{ floraFaunaList.length }}</strong>
              </div>
            </div>
          </article>
        </section>
      </template>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SystemNav from '../components/SystemNav.vue'
import api from '../utils/api'
import { resolveAssetUrl } from '../utils/assets'
import pandaQr from '../resource/二维码/大熊猫.png'
import monkeyQr from '../resource/二维码/金丝猴.png'
import muskDeerQr from '../resource/二维码/林麝.png'
import takinQr from '../resource/二维码/扭角羚.png'

const route = useRoute()
const router = useRouter()
const FALLBACK_IMAGE = resolveAssetUrl('/uploads/economy/川金丝猴.jpg')
const PRIMARY_WETLAND_KEYWORD = '九寨沟'
const FEATURED_WETLAND_KEYWORDS = ['九寨沟', '红海滩', '沉湖', '上涉湖']
const REGION_PREFIXES = [
  '北京市', '天津市', '上海市', '重庆市',
  '北京', '天津', '上海', '重庆',
  '河北', '山西', '辽宁', '吉林', '黑龙江',
  '江苏', '浙江', '安徽', '福建', '江西', '山东',
  '河南', '湖北', '湖南', '广东', '海南',
  '四川', '贵州', '云南', '陕西', '甘肃', '青海',
  '台湾', '内蒙古', '广西', '西藏', '宁夏', '新疆',
  '香港', '澳门'
]
const wetlands = ref([])
const selectedWetlandId = ref('')
const wetlandKeyword = ref('')
const floraFaunaList = ref([])
const pageLoading = ref(false)
const pageError = ref('')
const speciesLoading = ref(false)
const speciesError = ref('')
const featuredWetlands = ref([])
const activeRegion = ref('')
const groupRefs = ref({})
const pinyinCollator = new Intl.Collator('zh-CN-u-co-pinyin', { sensitivity: 'base', numeric: true })
const FEATURED_SPECIES_QR = [
  { rank: 0, keywords: ['大熊猫'], qrCode: pandaQr },
  { rank: 1, keywords: ['金丝猴', '川金丝猴'], qrCode: monkeyQr },
  { rank: 2, keywords: ['林麝'], qrCode: muskDeerQr },
  { rank: 3, keywords: ['扭角羚', '牛角岭'], qrCode: takinQr }
]

const currentWetland = computed(() => wetlands.value.find((item) => String(item.id) === String(selectedWetlandId.value)) || null)
const getFeaturedSpeciesMeta = (name) => {
  const source = String(name || '')
  return FEATURED_SPECIES_QR.find((item) => item.keywords.some((keyword) => source.includes(keyword))) || null
}
const sortedFloraFaunaList = computed(() => {
  return [...floraFaunaList.value]
    .map((item, index) => {
      const meta = getFeaturedSpeciesMeta(item.name)
      return {
        ...item,
        qrCode: meta?.qrCode || '',
        featuredSpeciesRank: meta?.rank ?? 99,
        originalIndex: index
      }
    })
    .sort((left, right) => left.featuredSpeciesRank - right.featuredSpeciesRank || left.originalIndex - right.originalIndex)
})
const filteredWetlands = computed(() => {
  const keyword = wetlandKeyword.value.trim().toLowerCase()
  return keyword
    ? wetlands.value.filter((item) => {
      const name = String(item.wetlandName || '').toLowerCase()
      const region = String(item.regionLabel || '').toLowerCase()
      return name.includes(keyword) || region.includes(keyword)
    })
    : wetlands.value
})
const filteredWetlandCount = computed(() => filteredWetlands.value.length)
const groupedWetlands = computed(() => {
  const groups = filteredWetlands.value.reduce((acc, item) => {
    const region = item.regionLabel || '其他地区'
    if (!acc[region]) acc[region] = []
    acc[region].push(item)
    return acc
  }, {})

  return Object.entries(groups)
    .map(([region, items]) => ({
      region,
      items: [...items].sort((a, b) =>
        String(a.wetlandName || '').localeCompare(String(b.wetlandName || ''), 'zh-Hans-CN-u-co-pinyin')
      )
    }))
    .sort((a, b) => a.region.localeCompare(b.region, 'zh-Hans-CN-u-co-pinyin'))
})
const truncateText = (value, limit = 90) => {
  const source = String(value || '').trim()
  if (!source) return '暂无介绍。'
  return source.length > limit ? `${source.slice(0, limit)}...` : source
}
const formatDateTime = (value) => {
  if (!value) return '暂无时间'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '暂无时间'
  return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`
}

const findWetlandByKeyword = (keyword) => wetlands.value.find((item) => String(item?.wetlandName || '').includes(keyword))

const applyRouteWetlandId = () => {
  const preferredWetland = findWetlandByKeyword(PRIMARY_WETLAND_KEYWORD)
  const queryWetlandId = String(route.query.wetlandId || '')
  const matched = wetlands.value.find((item) => String(item.id) === queryWetlandId)
  selectedWetlandId.value = preferredWetland
    ? String(preferredWetland.id)
    : matched
      ? String(matched.id)
      : (wetlands.value[0] ? String(wetlands.value[0].id) : '')
  wetlandKeyword.value = ''
  refreshFeaturedWetlands()
}

const deriveRegionLabel = (item) => {
  const name = String(item?.wetlandName || '').trim()
  const coordinate = String(item?.coordinateRange || '').trim()
  const source = `${name} ${coordinate}`

  for (const prefix of REGION_PREFIXES) {
    if (source.includes(prefix)) return prefix
  }

  return '其他地区'
}

const refreshFeaturedWetlands = () => {
  const fixedMatches = FEATURED_WETLAND_KEYWORDS
    .map((keyword) => findWetlandByKeyword(keyword))
    .filter((item, index, list) => item && list.findIndex((candidate) => candidate?.id === item.id) === index)

  const remainingWetlands = wetlands.value
    .filter((item) => fixedMatches.every((candidate) => candidate.id !== item.id))
    .sort((left, right) => pinyinCollator.compare(String(left.wetlandName || ''), String(right.wetlandName || '')))

  featuredWetlands.value = [...fixedMatches, ...remainingWetlands].slice(0, 4)
}

const loadWetlands = async () => {
  pageLoading.value = true
  pageError.value = ''
  try {
    const response = await api.get('/wetlands')
    wetlands.value = (response.data?.wetlands || []).map((item) => ({
      ...item,
      regionLabel: deriveRegionLabel(item)
    }))
    applyRouteWetlandId()
  } catch (error) {
    console.error('加载景区数据失败:', error)
    pageError.value = '景区数据加载失败，请稍后重试。'
  } finally {
    pageLoading.value = false
  }
}

const loadFloraFauna = async (wetlandId) => {
  if (!wetlandId) {
    floraFaunaList.value = []
    return
  }
  speciesLoading.value = true
  speciesError.value = ''
  try {
    const response = await api.get(`/wetlands/${wetlandId}/flora-fauna`)
    const records = response.data?.floraFaunas || []
    floraFaunaList.value = records.map((item) => ({
      ...item,
      image: resolveAssetUrl(item.imagePath, FALLBACK_IMAGE),
      brief: truncateText(item.description),
      createdTimeText: formatDateTime(item.createdTime)
    }))
  } catch (error) {
    console.error('加载珍稀动植物失败:', error)
    speciesError.value = '珍稀动植物数据加载失败，请稍后重试。'
    floraFaunaList.value = []
  } finally {
    speciesLoading.value = false
  }
}

const openFloraDetail = (item) => {
  router.push({
    name: 'FloraDetail',
    params: { id: String(item.id) },
    query: { wetlandId: String(selectedWetlandId.value) }
  })
}

const setGroupRef = (el, region) => {
  if (!region) return
  if (el) {
    groupRefs.value[region] = el
  }
}

const selectWetland = (item, keepKeyword = true) => {
  selectedWetlandId.value = String(item.id)
  if (keepKeyword) {
    wetlandKeyword.value = item.wetlandName || ''
  }
}

const clearWetlandKeyword = () => {
  wetlandKeyword.value = ''
}

watch(activeRegion, async (value) => {
  if (!value) return
  await nextTick()
  groupRefs.value[value]?.scrollIntoView({ behavior: 'smooth', block: 'start' })
})

watch(selectedWetlandId, async (value) => {
  if (!value) return
  if (String(route.query.wetlandId || '') !== String(value)) {
    router.replace({ name: 'Science', query: { wetlandId: String(value) } })
  }
  await loadFloraFauna(value)
})

onMounted(loadWetlands)
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&display=swap');
* { margin: 0; padding: 0; box-sizing: border-box; }
.science-page { min-height: 100vh; position: relative; overflow-x: hidden; color: #17252f; background: #eef1ec; font-family: 'Manrope', 'PingFang SC', 'Microsoft YaHei', sans-serif; }
.background-layer { position: fixed; inset: 0; pointer-events: none; }
.bg-image, .bg-overlay { position: absolute; inset: 0; }
.bg-image { background: url('https://images.unsplash.com/photo-1441974231531-c6227db76b6e?auto=format&fit=crop&w=1800&q=80') center/cover no-repeat; filter: saturate(0.74) brightness(0.9); }
.bg-overlay { background: linear-gradient(180deg, rgba(245, 247, 241, 0.82) 0%, rgba(239, 243, 237, 0.94) 38%, rgba(236, 240, 233, 0.98) 100%), linear-gradient(90deg, rgba(23, 37, 47, 0.06) 0, rgba(23, 37, 47, 0.06) 1px, transparent 1px, transparent 140px); }
.page-shell { position: relative; z-index: 1; width: min(1480px, calc(100% - 48px)); margin: 0 auto; padding: 196px 0 72px; }
.science-page-header { display: grid; justify-items: center; gap: 10px; margin-bottom: 24px; text-align: center; }
.science-page-header h1 { font-size: clamp(2.6rem, 4vw, 4.1rem); line-height: 1.14; color: #183329; font-weight: 600; }
.hero-section, .species-grid { display: grid; }
.hero-section { grid-template-columns: minmax(0, 1.15fr) minmax(320px, 0.85fr); gap: 20px; margin-top: 24px; }
.hero-copy, .selector-panel, .state-card, .species-card { border: 1px solid rgba(23, 37, 47, 0.14); background: rgba(255, 255, 255, 0.72); }
.hero-copy, .selector-panel, .state-card { padding: 24px; }
.selector-panel, .state-card { padding: 24px; }
.selector-label, .species-label, .species-date { font-size: 0.82rem; letter-spacing: 0.03em; color: rgba(23, 37, 47, 0.62); }
.hero-copy { display: grid; align-content: start; gap: 18px; }
.hero-copy h2 { font-size: clamp(1.5rem, 2vw, 2rem); line-height: 1.28; color: #173227; font-weight: 600; }
.selector-panel { display: grid; align-content: start; gap: 18px; }
.wetland-picker { display: grid; gap: 12px; }
.wetland-picker__current { display: grid; gap: 6px; padding: 14px 16px; border: 1px solid rgba(23, 37, 47, 0.12); background: rgba(247, 247, 243, 0.72); }
.wetland-picker__current span, .wetland-picker__hint { font-size: 0.82rem; color: rgba(23, 37, 47, 0.64); }
.wetland-picker__current strong { font-size: 0.98rem; font-weight: 600; color: #173227; }
.featured-wetlands, .search-index-panel { display: grid; gap: 12px; }
.featured-wetlands__topline, .search-index-panel__topline, .wetland-group__header { display: flex; justify-content: space-between; gap: 12px; align-items: baseline; }
.featured-wetlands__topline span, .search-index-panel__topline span, .wetland-group__header span { font-size: 0.82rem; letter-spacing: 0.03em; color: rgba(23, 37, 47, 0.62); }
.featured-wetlands__grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.featured-wetland-card { display: grid; gap: 6px; padding: 14px; border: 1px solid rgba(23, 37, 47, 0.12); background: rgba(255, 255, 255, 0.84); color: #173227; text-align: left; cursor: pointer; transition: transform 0.2s ease, border-color 0.2s ease; min-height: 88px; }
.featured-wetland-card:hover { transform: translateY(-2px); border-color: rgba(23, 37, 47, 0.24); }
.featured-wetland-card.is-active { background: rgba(223, 235, 225, 0.88); border-color: rgba(24, 51, 41, 0.28); }
.featured-wetland-card span, .featured-wetland-card em { font-style: normal; font-size: 0.78rem; color: rgba(23, 37, 47, 0.64); }
.featured-wetland-card strong { font-size: 0.92rem; line-height: 1.4; font-weight: 600; }
.wetland-search { width: 100%; padding: 14px 16px; border: 1px solid rgba(23, 37, 47, 0.14); background: rgba(247, 247, 243, 0.96); font-size: 0.9rem; color: #173227; outline: none; min-height: 52px; }
.wetland-search-actions { display: flex; justify-content: flex-end; }
.wetland-search-clear { border: 1px solid rgba(23, 37, 47, 0.12); background: rgba(255, 255, 255, 0.84); color: #173227; padding: 8px 12px; font-size: 0.84rem; font-weight: 600; cursor: pointer; }
.wetland-search-clear:disabled { opacity: 0.5; cursor: not-allowed; }
.region-index { display: flex; flex-wrap: wrap; gap: 8px; }
.region-index__chip { border: 1px solid rgba(23, 37, 47, 0.12); background: rgba(233, 239, 234, 0.84); color: #173227; padding: 8px 12px; font-size: 0.82rem; font-weight: 500; cursor: pointer; }
.wetland-groups { display: grid; gap: 14px; max-height: 360px; overflow-y: auto; padding-right: 4px; }
.wetland-group { display: grid; gap: 10px; }
.wetland-results { display: grid; gap: 10px; }
.wetland-result { display: grid; gap: 6px; padding: 12px 14px; border: 1px solid rgba(23, 37, 47, 0.12); background: rgba(255, 255, 255, 0.84); color: #173227; text-align: left; cursor: pointer; transition: border-color 0.2s ease, transform 0.2s ease, background 0.2s ease; min-height: 72px; }
.wetland-result:hover { transform: translateY(-1px); border-color: rgba(23, 37, 47, 0.24); }
.wetland-result.is-active { background: rgba(223, 235, 225, 0.88); border-color: rgba(24, 51, 41, 0.28); }
.wetland-result strong { font-size: 0.92rem; font-weight: 600; }
.wetland-result span { font-size: 0.82rem; color: rgba(23, 37, 47, 0.66); }
.selector-meta { display: grid; gap: 12px; }
.meta-row { display: grid; gap: 10px; padding-top: 12px; border-top: 1px solid rgba(23, 37, 47, 0.1); }
.meta-row strong { font-size: 0.9rem; line-height: 1.66; font-weight: 600; }
.state-card { margin-top: 24px; }
.state-card--embedded { margin-top: 0; padding: 18px; }
.species-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; margin-top: 20px; }
.species-grid--featured { margin-top: 0; }
.species-card { display: grid; grid-template-columns: 180px minmax(0, 1fr); overflow: hidden; cursor: pointer; transition: transform 0.26s ease, border-color 0.26s ease, box-shadow 0.26s ease; min-height: 100%; }
.species-card:hover { transform: translateY(-3px); border-color: rgba(23, 37, 47, 0.32); box-shadow: 0 22px 40px rgba(23, 37, 47, 0.1); }
.species-card:focus-visible { outline: 2px solid rgba(23, 37, 47, 0.38); outline-offset: 4px; }
.species-image-wrap { min-height: 100%; background: #d7e2d8; }
.species-image { width: 100%; height: 100%; object-fit: cover; display: block; }
.species-body { display: grid; align-content: start; gap: 12px; padding: 16px; border-left: 1px solid rgba(23, 37, 47, 0.1); }
.species-head { display: flex; justify-content: space-between; gap: 12px; align-items: baseline; padding-bottom: 12px; border-bottom: 1px solid rgba(23, 37, 47, 0.1); }
.species-body h3 { font-size: clamp(1.12rem, 1.4vw, 1.32rem); line-height: 1.28; color: #173227; font-weight: 600; }
.species-body p { line-height: 1.68; color: rgba(23, 37, 47, 0.78); font-size: 0.84rem; }
.species-foot { display: flex; justify-content: space-between; gap: 12px; align-items: center; padding-top: 14px; border-top: 1px solid rgba(23, 37, 47, 0.1); }
.species-foot span, .species-foot strong { font-size: 0.78rem; letter-spacing: 0.03em; }
.species-foot strong { color: #173227; }
@media (max-width: 980px) { .page-shell { width: calc(100% - 32px); } .hero-section, .species-grid, .species-card, .featured-wetlands__grid { grid-template-columns: 1fr; } .species-body { border-left: none; border-top: 1px solid rgba(23, 37, 47, 0.1); } }
@media (max-width: 720px) { .page-shell { width: calc(100% - 24px); padding: 176px 0 64px; } .hero-copy, .selector-panel, .state-card, .species-body { padding: 18px; } .science-page-header h1 { font-size: clamp(2rem, 9vw, 3rem); } .hero-copy h2 { font-size: 1.28rem; } .species-head, .species-foot { flex-direction: column; align-items: start; } }
</style>
