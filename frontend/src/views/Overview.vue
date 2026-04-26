<template>
  <div class="overview-page" :style="themeStyle">
    <div class="background-layer">
      <div class="background-image" :style="backgroundImageStyle"></div>
      <div class="background-overlay"></div>
    </div>
    <SystemNav />

    <main class="page-shell">
      <section class="title-section">
        <transition name="title-fade" mode="out-in">
          <h1 :key="`title-${activeTag}`" v-y-slice>{{ currentTitle }}</h1>
        </transition>
      </section>

      <section :key="`category-nav-${activeTag}`" class="category-nav">
        <button
          v-for="tag in filterTabs"
          :key="tag.value"
          :class="['category-button', { 'is-active': activeTag === tag.value }]"
          @click="activeTag = tag.value"
        >
          <span>{{ tag.label }}</span>
        </button>
      </section>

      <section
        v-if="visibleRegionTabs.length > 1"
        :class="['region-nav', { 'is-collapsed': !showAllRegions }]"
      >
        <button
          v-for="region in visibleRegionTabs"
          :key="region.value"
          :class="['region-button', { 'is-active': activeRegion === region.value }]"
          @click="handleRegionClick(region.value)"
        >
          <span>{{ region.label }}</span>
        </button>

        <button
          v-if="regionTabs.length > 2"
          class="region-control-button"
          @click="showAllRegions ? collapseRegionList() : (showAllRegions = true)"
        >
          <span>{{ showAllRegions ? '收起列表' : '全部地区' }}</span>
        </button>
      </section>

      <section class="content-section">
        <div v-if="loading" class="state-card">
          <p>正在加载湿地数据...</p>
        </div>
        <div v-else-if="errorMessage" class="state-card">
          <p>{{ errorMessage }}</p>
        </div>
        <div v-else-if="filteredWetlands.length === 0" class="state-card">
          <p>当前分类下暂无湿地数据。</p>
        </div>

        <div v-else class="wetland-list">
          <article
            v-for="wetland in paginatedWetlands"
            :key="wetland.id"
            class="wetland-card"
            :style="getCardStyle(wetland.primaryCategory)"
            role="button"
            tabindex="0"
            @click="openDetail(wetland.id)"
            @keydown.enter="openDetail(wetland.id)"
            @keydown.space.prevent="openDetail(wetland.id)"
          >
            <div class="wetland-card__media">
              <img :src="wetland.image" :alt="wetland.wetlandName" />
            </div>

            <div class="wetland-card__body">
              <div class="wetland-card__masthead">
                <div class="wetland-tags">
                  <span
                    v-for="category in wetland.categories"
                    :key="`${wetland.id}-${category}`"
                    class="tag-chip"
                    :style="getBadgeStyle(category)"
                  >
                    {{ category }}
                  </span>
                </div>
              </div>

              <h2>{{ wetland.wetlandName }}</h2>

              <div class="wetland-ledger">
                <div class="ledger-row">
                  <span>分类</span>
                  <strong>{{ wetland.categories.join(' / ') }}</strong>
                </div>
                <div class="ledger-row">
                  <span>坐标</span>
                  <strong>{{ wetland.coordinateRange || '坐标信息待补充' }}</strong>
                </div>
              </div>

              <div class="wetland-card__action">
                <span>查看详情</span>
              </div>
            </div>
          </article>
        </div>

        <div v-if="!loading && !errorMessage && totalPages > 1" class="pagination-bar">
          <button
            class="pagination-button"
            :disabled="currentPage === 1"
            @click="goToPreviousPage"
          >
            上一页
          </button>

          <div class="pagination-info">
            <button
              v-for="page in visiblePages"
              :key="page"
              :class="['page-number', { 'is-active': currentPage === page }]"
            @click="goToPage(page)"
            >
              {{ page }}
            </button>
          </div>

          <p class="pagination-status">第 {{ currentPage }} 页 / 共 {{ totalPages }} 页</p>

          <div class="pagination-jump">
            <label class="pagination-jump__label" for="page-jump-input">跳转到第</label>
            <input
              id="page-jump-input"
              v-model="jumpPageInput"
              class="pagination-jump__input"
              type="number"
              min="1"
              :max="totalPages"
              @keydown.enter.prevent="submitJumpPage"
            />
            <span class="pagination-jump__suffix">页</span>
            <button class="pagination-button pagination-button--jump" @click="submitJumpPage">
              跳转
            </button>
          </div>

          <button
            class="pagination-button"
            :disabled="currentPage === totalPages"
            @click="goToNextPage"
          >
            下一页
          </button>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import SystemNav from '../components/SystemNav.vue'
import api from '../utils/api'
import { resolveAssetUrl } from '../utils/assets'

const OVERVIEW_BG_ALL = 'https://images.unsplash.com/photo-1500375592092-40eb2168fd21?auto=format&fit=crop&w=1800&q=80'
const OVERVIEW_BG_INLAND = 'https://images.unsplash.com/photo-1473773508845-188df298d2d1?auto=format&fit=crop&w=1800&q=80'
const OVERVIEW_BG_COASTAL = 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1800&q=80'
const OVERVIEW_BG_ARTIFICIAL = 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1800&q=80'

const CATEGORY_DEFS = {
  all: { label: '全部湿地', shortLabel: '全部', accent: '#2d5967', accentStrong: '#173843', textPrimary: '#142a31', textSecondary: 'rgba(20, 42, 49, 0.78)', accentRgb: '45, 89, 103', navBg: 'rgba(230, 237, 240, 0.88)', navText: '#000000', navBorder: 'rgba(45, 89, 103, 0.18)', background: 'linear-gradient(180deg, #eef3f4 0%, #f7f4ee 100%)', backgroundImage: OVERVIEW_BG_ALL },
  '内陆': { label: '内陆湿地', shortLabel: '内陆', accent: '#4f7a47', accentStrong: '#294625', textPrimary: '#23381f', textSecondary: 'rgba(35, 56, 31, 0.78)', accentRgb: '79, 122, 71', navBg: 'rgba(230, 238, 226, 0.9)', navText: '#000000', navBorder: 'rgba(79, 122, 71, 0.18)', background: 'linear-gradient(180deg, #e5eee0 0%, #f6f2e8 100%)', backgroundImage: OVERVIEW_BG_INLAND },
  '沿海': { label: '沿海湿地', shortLabel: '沿海', accent: '#2e729e', accentStrong: '#1d4964', textPrimary: '#17394f', textSecondary: 'rgba(23, 57, 79, 0.78)', accentRgb: '46, 114, 158', navBg: 'rgba(226, 236, 244, 0.9)', navText: '#000000', navBorder: 'rgba(46, 114, 158, 0.18)', background: 'linear-gradient(180deg, #e2edf5 0%, #f3f6f8 100%)', backgroundImage: OVERVIEW_BG_COASTAL },
  '人造': { label: '人造湿地', shortLabel: '人造', accent: '#ab6e31', accentStrong: '#6b441e', textPrimary: '#58361a', textSecondary: 'rgba(88, 54, 26, 0.8)', accentRgb: '171, 110, 49', navBg: 'rgba(241, 229, 216, 0.92)', navText: '#000000', navBorder: 'rgba(171, 110, 49, 0.18)', background: 'linear-gradient(180deg, #efe2d3 0%, #f7f1ea 100%)', backgroundImage: OVERVIEW_BG_ARTIFICIAL }
}

const TITLE_COPY = {
  all: '湿地秘境，全域呈现',
  '内陆': '内陆泽境，生机自成',
  '沿海': '海岸湿地，陆海共生',
  '人造': '匠心筑泽，生态新生'
}
const FALLBACK_WETLAND_IMAGE = OVERVIEW_BG_ALL
const wetlands = ref([])
const loading = ref(false)
const errorMessage = ref('')
const activeTag = ref('all')
const activeRegion = ref('all')
const showAllRegions = ref(false)
const previewRegionValues = ref([])
const currentPage = ref(1)
const jumpPageInput = ref('1')
const pageSize = 12
const router = useRouter()
const pinyinCollator = new Intl.Collator('zh-CN-u-co-pinyin', { sensitivity: 'base', numeric: true })
const PINNED_WETLAND_KEYWORDS = ['九寨沟', '红海滩', '沉湖', '上涉湖']
const PINNED_WETLAND_RANK = new Map(PINNED_WETLAND_KEYWORDS.map((name, index) => [name, index]))
const REGION_PREFIXES = [
  '内蒙古', '黑龙江', '吉林', '辽宁', '河北', '河南', '山东', '山西', '陕西', '甘肃', '青海',
  '江苏', '浙江', '安徽', '福建', '江西', '湖北', '湖南', '广东', '广西', '海南', '四川',
  '贵州', '云南', '西藏', '宁夏', '新疆', '北京', '天津', '上海', '重庆', '香港', '澳门',
  '台湾', '大兴安岭'
]
const getTheme = (tag) => CATEGORY_DEFS[tag] || CATEGORY_DEFS.all
const currentTheme = computed(() => getTheme(activeTag.value))
const currentTitle = computed(() => TITLE_COPY[activeTag.value] || TITLE_COPY.all)

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

const extractRegion = (wetlandName) => {
  const source = String(wetlandName || '').trim()
  const matchedPrefix = REGION_PREFIXES.find((region) => source.startsWith(region))
  if (matchedPrefix) return matchedPrefix

  const autonomousMatch = source.match(/^([\u4e00-\u9fa5]{2,8}(?:自治州|自治区))/)
  if (autonomousMatch) return autonomousMatch[1]

  const cityMatch = source.match(/^([\u4e00-\u9fa5]{2,8}(?:市|州|地区|盟))/)
  if (cityMatch) return cityMatch[1]

  return '其他地区'
}

const resolvePinnedRank = (wetlandName) => {
  const normalizedName = String(wetlandName || '').trim()
  for (const keyword of PINNED_WETLAND_KEYWORDS) {
    if (normalizedName.includes(keyword)) {
      return PINNED_WETLAND_RANK.get(keyword)
    }
  }
  return null
}

const sortWetlandsForDisplay = (items) => (
  [...items].sort((left, right) => {
    const leftName = String(left.wetlandName || '')
    const rightName = String(right.wetlandName || '')
    if (activeTag.value === 'all') {
      const leftPinnedRank = resolvePinnedRank(leftName)
      const rightPinnedRank = resolvePinnedRank(rightName)
      const leftPinned = leftPinnedRank != null
      const rightPinned = rightPinnedRank != null

      if (leftPinned && rightPinned) return leftPinnedRank - rightPinnedRank
      if (leftPinned) return -1
      if (rightPinned) return 1
    }

    return pinyinCollator.compare(leftName, rightName)
  })
)

const categoryWetlands = computed(() => (
  activeTag.value === 'all'
    ? wetlands.value
    : wetlands.value.filter((wetland) => wetland.categories.includes(activeTag.value))
))

const regionTabs = computed(() => {
  const counts = categoryWetlands.value.reduce((accumulator, item) => {
    accumulator[item.region] = (accumulator[item.region] || 0) + 1
    return accumulator
  }, {})

  const regionItems = Object.entries(counts)
    .sort((left, right) => pinyinCollator.compare(left[0], right[0]))
    .map(([region, count]) => ({ value: region, label: region, count }))

  return [
    { value: 'all', label: '地区不限', count: categoryWetlands.value.length },
    ...regionItems
  ]
})

const visibleRegionTabs = computed(() => {
  if (showAllRegions.value) return regionTabs.value
  const previewSet = new Set(previewRegionValues.value)
  return regionTabs.value.filter((item) => item.value === 'all' || previewSet.has(item.value))
})

const filteredWetlands = computed(() => {
  const baseList = activeRegion.value === 'all'
    ? categoryWetlands.value
    : categoryWetlands.value.filter((wetland) => wetland.region === activeRegion.value)
  return sortWetlandsForDisplay(baseList)
})
const totalPages = computed(() => Math.max(1, Math.ceil(filteredWetlands.value.length / pageSize)))
const paginatedWetlands = computed(() => {
  const startIndex = (currentPage.value - 1) * pageSize
  return filteredWetlands.value.slice(startIndex, startIndex + pageSize)
})
const visiblePages = computed(() => {
  const total = totalPages.value
  if (total <= 5) return Array.from({ length: total }, (_, index) => index + 1)
  const start = Math.max(1, Math.min(currentPage.value - 2, total - 4))
  return Array.from({ length: 5 }, (_, index) => start + index)
})
const filterTabs = computed(() => {
  const counts = {
    all: wetlands.value.length,
    '内陆': wetlands.value.filter((item) => item.categories.includes('内陆')).length,
    '沿海': wetlands.value.filter((item) => item.categories.includes('沿海')).length,
    '人造': wetlands.value.filter((item) => item.categories.includes('人造')).length
  }
  return [
    { value: 'all', label: CATEGORY_DEFS.all.shortLabel, count: counts.all },
    { value: '内陆', label: CATEGORY_DEFS['内陆'].shortLabel, count: counts['内陆'] },
    { value: '沿海', label: CATEGORY_DEFS['沿海'].shortLabel, count: counts['沿海'] },
    { value: '人造', label: CATEGORY_DEFS['人造'].shortLabel, count: counts['人造'] }
  ]
})

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
const getBadgeStyle = (tag) => {
  const theme = getTheme(tag)
  return { borderColor: `rgba(${theme.accentRgb}, 0.22)`, color: theme.accentStrong }
}
const getCardStyle = (tag) => {
  const theme = getTheme(tag)
  return { '--card-accent': theme.accent, '--card-accent-rgb': theme.accentRgb }
}
const openDetail = (wetlandId) => wetlandId && router.push({ name: 'Detail', params: { id: String(wetlandId) } })
const goToPage = (page) => {
  currentPage.value = page
}
const goToPreviousPage = () => {
  if (currentPage.value > 1) currentPage.value -= 1
}
const goToNextPage = () => {
  if (currentPage.value < totalPages.value) currentPage.value += 1
}
const submitJumpPage = () => {
  const parsedPage = Number.parseInt(jumpPageInput.value, 10)
  if (Number.isNaN(parsedPage)) {
    jumpPageInput.value = String(currentPage.value)
    return
  }
  const targetPage = Math.min(Math.max(parsedPage, 1), totalPages.value)
  currentPage.value = targetPage
}

const refreshRegionPreview = () => {
  const candidates = regionTabs.value
    .filter((item) => item.value !== 'all')
    .map((item) => item.value)

  const shuffled = [...candidates]
  for (let index = shuffled.length - 1; index > 0; index -= 1) {
    const swapIndex = Math.floor(Math.random() * (index + 1))
    ;[shuffled[index], shuffled[swapIndex]] = [shuffled[swapIndex], shuffled[index]]
  }

  previewRegionValues.value = shuffled.slice(0, 5)
}

const handleRegionClick = (regionValue) => {
  if (regionValue === 'all') {
    activeRegion.value = 'all'
    return
  }

  activeRegion.value = regionValue
}

const collapseRegionList = () => {
  showAllRegions.value = false
  activeRegion.value = 'all'
  refreshRegionPreview()
}

const loadWetlands = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await api.get('/wetlands')
    const records = response.data?.wetlands || []
    wetlands.value = records.map((item) => {
      const categories = normalizeCategories(item.tags)
      return {
        ...item,
        categories,
        region: extractRegion(item.wetlandName),
        primaryCategory: categories[0] || '内陆',
        image: resolveAssetUrl(item.imagePath, FALLBACK_WETLAND_IMAGE)
      }
    })
  } catch (error) {
    console.error('加载湿地总览失败:', error)
    errorMessage.value = '湿地数据加载失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

watch(activeTag, () => {
  activeRegion.value = 'all'
  showAllRegions.value = false
  currentPage.value = 1
})

watch(activeRegion, () => {
  currentPage.value = 1
})

watch(regionTabs, () => {
  const availableValues = new Set(regionTabs.value.map((item) => item.value))
  if (!availableValues.has(activeRegion.value)) activeRegion.value = 'all'
  refreshRegionPreview()
}, { immediate: true })

watch(filteredWetlands, (items) => {
  const maxPage = Math.max(1, Math.ceil(items.length / pageSize))
  if (currentPage.value > maxPage) currentPage.value = maxPage
})

watch(currentPage, (page) => {
  jumpPageInput.value = String(page)
})

onMounted(loadWetlands)
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&display=swap');
* { margin: 0; padding: 0; box-sizing: border-box; }
.overview-page { --theme-accent: #2d5967; --theme-accent-strong: #173843; --theme-accent-rgb: 45, 89, 103; --theme-text-primary: #142a31; --theme-text-secondary: rgba(20, 42, 49, 0.78); --theme-background: linear-gradient(180deg, #eef3f4 0%, #f7f4ee 100%); min-height: 100vh; position: relative; background: var(--theme-background); color: var(--theme-text-primary); font-family: 'Manrope', 'PingFang SC', 'Microsoft YaHei', sans-serif; }
.background-layer { position: fixed; inset: 0; z-index: 0; pointer-events: none; }
.background-image, .background-overlay { position: absolute; inset: 0; }
.background-image { background-position: center; background-size: cover; filter: saturate(0.78) contrast(1.02); transform: scale(1.04); }
.background-overlay { background: linear-gradient(180deg, rgba(249, 247, 242, 0.74) 0%, rgba(247, 244, 238, 0.9) 32%, rgba(245, 241, 234, 0.96) 100%), linear-gradient(90deg, rgba(var(--theme-accent-rgb), 0.08) 0, rgba(var(--theme-accent-rgb), 0.08) 1px, transparent 1px, transparent 120px); }
.page-shell { position: relative; z-index: 1; width: min(1480px, calc(100% - 48px)); margin: 0 auto; padding: 196px 0 72px; }
.title-section { display: flex; justify-content: center; margin-bottom: 30px; text-align: center; }
.title-section h1, .wetland-card h2 { color: var(--theme-accent-strong); font-weight: 600; }
.title-section h1 { max-width: 16ch; font-size: clamp(2.6rem, 4.2vw, 4.1rem); line-height: 1.18; letter-spacing: 0.02em; overflow: visible; }
.category-nav { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; margin: 0 auto 16px; max-width: 1160px; }
.category-button { border: 1px solid rgba(var(--theme-accent-rgb), 0.14); background: rgba(255, 255, 255, 0.76); padding: 16px 18px; display: flex; justify-content: center; align-items: center; cursor: pointer; transition: transform 0.24s ease, border-color 0.24s ease, background 0.24s ease; }
.category-button span { color: var(--theme-text-primary); }
.category-button span { font-size: 0.94rem; letter-spacing: 0.04em; }
.category-button.is-active, .category-button:hover { border-color: rgba(var(--theme-accent-rgb), 0.42); background: rgba(255, 255, 255, 0.88); transform: translateY(-2px); }
.region-nav { display: flex; flex-wrap: wrap; gap: 12px; margin: 0 auto 34px; align-items: center; justify-content: center; max-width: 1280px; }
.region-nav.is-collapsed { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); align-items: stretch; width: 100%; }
.region-button { border: 1px solid rgba(var(--theme-accent-rgb), 0.14); background: rgba(255, 255, 255, 0.66); padding: 12px 18px; display: inline-flex; align-items: center; justify-content: center; gap: 10px; cursor: pointer; transition: transform 0.24s ease, border-color 0.24s ease, background 0.24s ease; min-height: 48px; }
.region-button span { color: var(--theme-text-primary); }
.region-button span { font-size: 0.9rem; letter-spacing: 0.03em; }
.region-button.is-active, .region-button:hover { border-color: rgba(var(--theme-accent-rgb), 0.34); background: rgba(255, 255, 255, 0.82); transform: translateY(-1px); }
.region-control-button { border: none; background: transparent; padding: 10px 8px; display: inline-flex; align-items: center; justify-content: center; cursor: pointer; transition: color 0.24s ease, transform 0.24s ease, opacity 0.24s ease; }
.region-control-button span { color: var(--theme-text-secondary); font-size: 0.88rem; letter-spacing: 0.03em; font-weight: 600; }
.region-control-button:hover { transform: translateY(-1px); }
.region-control-button:hover span { color: var(--theme-text-primary); }
.state-card, .wetland-card { border: 1px solid rgba(var(--theme-accent-rgb), 0.14); background: rgba(255, 255, 255, 0.74); }
.state-card { padding: 24px; }
.wetland-list { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 18px; }
.pagination-bar { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-top: 28px; padding: 20px 22px; border: 1px solid rgba(var(--theme-accent-rgb), 0.14); background: rgba(255, 255, 255, 0.74); flex-wrap: wrap; }
.pagination-info { display: flex; flex-wrap: wrap; justify-content: center; gap: 10px; }
.pagination-status { font-size: 0.92rem; font-weight: 600; color: var(--theme-text-primary); white-space: nowrap; }
.pagination-jump { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.pagination-jump__label, .pagination-jump__suffix { font-size: 0.9rem; color: var(--theme-text-primary); white-space: nowrap; }
.pagination-jump__input { width: 76px; height: 42px; padding: 0 12px; border: 1px solid rgba(var(--theme-accent-rgb), 0.2); background: rgba(255, 255, 255, 0.84); color: var(--theme-text-primary); font-size: 0.94rem; font-weight: 700; text-align: center; }
.pagination-jump__input:focus { outline: 2px solid rgba(var(--theme-accent-rgb), 0.28); outline-offset: 2px; border-color: rgba(var(--theme-accent-rgb), 0.38); }
.pagination-button, .page-number { border: 1px solid rgba(var(--theme-accent-rgb), 0.2); background: rgba(255, 255, 255, 0.84); color: var(--theme-text-primary); cursor: pointer; transition: transform 0.24s ease, border-color 0.24s ease, background 0.24s ease, color 0.24s ease; }
.pagination-button { min-width: 104px; padding: 12px 18px; font-size: 0.86rem; font-weight: 600; letter-spacing: 0.03em; }
.pagination-button--jump { min-width: 88px; }
.page-number { min-width: 42px; height: 42px; padding: 0 12px; font-size: 0.94rem; font-weight: 700; }
.pagination-button:hover:not(:disabled), .page-number:hover { transform: translateY(-2px); border-color: rgba(var(--theme-accent-rgb), 0.38); }
.pagination-button:disabled { opacity: 0.42; cursor: not-allowed; }
.page-number.is-active { background: rgb(var(--theme-accent-rgb)); color: #fff; border-color: rgb(var(--theme-accent-rgb)); }
.wetland-card { display: grid; grid-template-columns: 1fr; grid-template-rows: auto 1fr; overflow: hidden; cursor: pointer; transition: transform 0.28s ease, border-color 0.28s ease, box-shadow 0.28s ease; min-height: 100%; }
.wetland-card:hover { transform: translateY(-3px); border-color: rgba(var(--card-accent-rgb), 0.36); box-shadow: 0 20px 40px rgba(var(--card-accent-rgb), 0.1); }
.wetland-card:focus-visible { outline: 2px solid rgba(var(--card-accent-rgb), 0.45); outline-offset: 4px; }
.wetland-card__media { background: #ddd; aspect-ratio: 1.08 / 0.86; }
.wetland-card__media img { width: 100%; height: 100%; object-fit: cover; display: block; filter: saturate(0.86); }
.wetland-card__body { display: grid; align-content: start; gap: 14px; padding: 18px 18px 20px; }
.wetland-card__masthead { display: flex; justify-content: flex-start; gap: 12px; align-items: start; padding-bottom: 12px; border-bottom: 1px solid rgba(var(--card-accent-rgb), 0.12); }
.wetland-tags { display: flex; flex-wrap: wrap; gap: 8px; justify-content: flex-start; }
.tag-chip { padding: 5px 10px; border: 1px solid transparent; font-size: 0.72rem; font-weight: 600; letter-spacing: 0.03em; background: rgba(255, 255, 255, 0.72); }
.wetland-card h2 { font-size: clamp(1.25rem, 1.55vw, 1.58rem); line-height: 1.34; overflow: visible; }
.wetland-ledger { display: grid; gap: 8px; }
.ledger-row { display: grid; grid-template-columns: 52px minmax(0, 1fr); gap: 10px; padding-top: 10px; border-top: 1px solid rgba(var(--card-accent-rgb), 0.08); }
.ledger-row span { font-size: 0.8rem; letter-spacing: 0.03em; color: var(--theme-text-secondary); }
.ledger-row strong { font-size: 0.88rem; line-height: 1.7; font-weight: 500; }
.wetland-card__action { margin-top: 4px; padding-top: 12px; border-top: 1px solid rgba(var(--card-accent-rgb), 0.12); display: flex; justify-content: flex-start; }
.wetland-card__action span { font-size: 0.82rem; font-weight: 600; letter-spacing: 0.03em; color: rgb(var(--card-accent-rgb)); }
.title-fade-enter-active, .title-fade-leave-active { transition: opacity 0.28s ease, transform 0.28s ease; }
.title-fade-enter-from, .title-fade-leave-to { opacity: 0; transform: translateY(14px); }
@media (max-width: 1280px) { .wetland-list { grid-template-columns: repeat(3, minmax(0, 1fr)); } .region-nav.is-collapsed { grid-template-columns: repeat(5, minmax(0, 1fr)); } }
@media (max-width: 960px) { .page-shell { width: calc(100% - 32px); } .category-nav, .wetland-list { grid-template-columns: repeat(2, minmax(0, 1fr)); } .region-nav.is-collapsed { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
@media (max-width: 720px) { .page-shell { width: calc(100% - 24px); padding: 184px 0 64px; } .title-section { margin-bottom: 24px; } .title-section h1 { font-size: clamp(2rem, 9vw, 3rem); } .category-nav, .wetland-list, .region-nav.is-collapsed { grid-template-columns: 1fr; } .category-button, .state-card, .wetland-card__body { padding: 18px; } .region-nav { gap: 8px; } .region-button, .region-control-button { width: 100%; } .region-button { justify-content: center; } .region-control-button { justify-content: center; padding-inline: 0; } .pagination-bar { flex-direction: column; padding: 18px; } .pagination-info, .pagination-jump { width: 100%; justify-content: center; } .pagination-status { text-align: center; } .pagination-button { width: 100%; } .pagination-button--jump { width: auto; min-width: 88px; } }
</style>
