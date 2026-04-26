<template>
  <div class="consult-index-page" :style="themeStyle">
    <section class="consult-hero">
      <div class="consult-hero__copy">
        <p class="consult-hero__eyebrow"></p>
        <h1>云游导览</h1>
        <p class="consult-hero__desc">
          以景区档案的方式浏览湿地目的地，卡片内容直接读取数据库中的景区图片与文字信息；路线入口被收束到页面操作区，详情阅读保持完整独立。
        </p>
      </div>

      <div class="consult-hero__actions">
        <RouterLink
          to="/consult"
          :class="['consult-entry-button', { 'is-active': route.path === '/consult' }]"
        >
          <span>景区挑选</span>
        </RouterLink>

        <RouterLink
          to="/consult/travel-map"
          :class="['consult-entry-button', { 'is-active': route.path.startsWith('/consult/travel-map') }]"
        >
          <span>旅行地图</span>
        </RouterLink>
      </div>
    </section>

    <section class="consult-filter-bar">
      <button
        v-for="tab in filterTabs"
        :key="tab.value"
        :class="['consult-filter-button', { 'is-active': activeTag === tab.value }]"
        @click="activeTag = tab.value"
      >
        <span>{{ tab.label }}</span>
      </button>
      </section>

    <section class="consult-search-bar">
      <label class="consult-search-field">
        <span class="consult-search-field__label">快速检索</span>
        <input
          v-model.trim="searchKeyword"
          type="text"
          class="consult-search-field__input"
          placeholder="输入湿地名称、拼音或景区标识"
        />
      </label>

      <button
        v-if="searchKeyword"
        type="button"
        class="consult-search-reset"
        @click="searchKeyword = ''"
      >
        清空检索
      </button>
    </section>

    <section class="consult-content">
      <div v-if="loading" class="state-card">
        <p>正在加载景区数据...</p>
      </div>

      <div v-else-if="errorMessage" class="state-card">
        <p>{{ errorMessage }}</p>
      </div>

      <div v-else-if="filteredWetlands.length === 0" class="state-card">
        <p>{{ searchKeyword ? '没有匹配的湿地结果。' : '当前分类下暂无景区卡片。' }}</p>
      </div>

      <div v-else class="consult-grid">
        <article
          v-for="wetland in paginatedWetlands"
          :key="wetland.id"
          class="consult-card"
          :style="getCardStyle(wetland.primaryCategory)"
        >
          <div class="consult-card__media">
            <img :src="wetland.image" :alt="wetland.wetlandName" />
          </div>

          <div class="consult-card__body">
            <div class="consult-card__masthead">
              <div class="consult-card__tags">
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
            <p class="consult-card__subtitle">{{ wetland.coordinateRange || '坐标信息待补充' }}</p>
            <p class="consult-card__desc">{{ wetland.description || '暂无景区简介。' }}</p>

            <div class="consult-card__ledger">
              <div class="ledger-row">
                <span>湿地类型</span>
                <strong>{{ wetland.categories.join(' / ') }}</strong>
              </div>
              <div class="ledger-row">
                <span>生态信息</span>
                <strong>{{ wetland.floraFaunaInfo || wetland.ecologyInfo || '暂无生态档案。' }}</strong>
              </div>
            </div>

            <div class="consult-card__action">
              <RouterLink class="detail-link" :to="buildConsultDetailPath(wetland)">
                进入景区导览页
              </RouterLink>
            </div>
          </div>
        </article>
      </div>

      <div v-if="!loading && !errorMessage && totalPages > 1" class="pagination-bar">
        <button class="pagination-button" :disabled="currentPage === 1" @click="goToPreviousPage">
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

        <button class="pagination-button" :disabled="currentPage === totalPages" @click="goToNextPage">
          下一页
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import api from '../../utils/api'
import { resolveAssetUrl } from '../../utils/assets'
import { DEFAULT_SCENIC_ID, buildScenicPagePath } from '../../utils/scenic'

const route = useRoute()

const OVERVIEW_BG_ALL = 'https://images.unsplash.com/photo-1500375592092-40eb2168fd21?auto=format&fit=crop&w=1800&q=80'
const CATEGORY_DEFS = {
  all: {
    shortLabel: '全部',
    accent: '#2d5967',
    accentStrong: '#173843',
    textPrimary: '#142a31',
    textSecondary: 'rgba(20, 42, 49, 0.78)',
    accentRgb: '45, 89, 103',
    navBg: 'rgba(230, 237, 240, 0.88)',
    navText: '#000000',
    navBorder: 'rgba(45, 89, 103, 0.18)',
    background: 'linear-gradient(180deg, #eef3f4 0%, #f7f4ee 100%)',
    backgroundImage: OVERVIEW_BG_ALL
  },
  '内陆': {
    shortLabel: '内陆',
    accent: '#4f7a47',
    accentStrong: '#294625',
    textPrimary: '#23381f',
    textSecondary: 'rgba(35, 56, 31, 0.78)',
    accentRgb: '79, 122, 71',
    navBg: 'rgba(230, 238, 226, 0.9)',
    navText: '#000000',
    navBorder: 'rgba(79, 122, 71, 0.18)',
    background: 'linear-gradient(180deg, #e5eee0 0%, #f6f2e8 100%)',
    backgroundImage: 'https://images.unsplash.com/photo-1473773508845-188df298d2d1?auto=format&fit=crop&w=1800&q=80'
  },
  '沿海': {
    shortLabel: '沿海',
    accent: '#2e729e',
    accentStrong: '#1d4964',
    textPrimary: '#17394f',
    textSecondary: 'rgba(23, 57, 79, 0.78)',
    accentRgb: '46, 114, 158',
    navBg: 'rgba(226, 236, 244, 0.9)',
    navText: '#000000',
    navBorder: 'rgba(46, 114, 158, 0.18)',
    background: 'linear-gradient(180deg, #e2edf5 0%, #f3f6f8 100%)',
    backgroundImage: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1800&q=80'
  },
  '人造': {
    shortLabel: '人造',
    accent: '#ab6e31',
    accentStrong: '#6b441e',
    textPrimary: '#58361a',
    textSecondary: 'rgba(88, 54, 26, 0.8)',
    accentRgb: '171, 110, 49',
    navBg: 'rgba(241, 229, 216, 0.92)',
    navText: '#000000',
    navBorder: 'rgba(171, 110, 49, 0.18)',
    background: 'linear-gradient(180deg, #efe2d3 0%, #f7f1ea 100%)',
    backgroundImage: 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1800&q=80'
  }
}

const FALLBACK_WETLAND_IMAGE = OVERVIEW_BG_ALL
const wetlands = ref([])
const loading = ref(false)
const errorMessage = ref('')
const activeTag = ref('all')
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = 12
const pinyinCollator = new Intl.Collator('zh-CN-u-co-pinyin', { sensitivity: 'base', numeric: true })
const FEATURED_WETLAND_KEYWORDS = ['九寨沟', '红海滩', '沉湖', '上涉湖']
const KNOWN_SCENIC_ID_MAP = {
  '九寨沟': 'jiuzhaigou',
  '九寨沟湿地': 'jiuzhaigou',
  jiuzhaigou: 'jiuzhaigou',
  '若尔盖': 'ruoergai',
  '若尔盖湿地': 'ruoergai',
  ruoergai: 'ruoergai',
  '巴音布鲁克': 'bayinbuluke',
  '巴音布鲁克湿地': 'bayinbuluke',
  bayinbuluke: 'bayinbuluke',
  '扎龙': 'zhalong',
  '扎龙湿地': 'zhalong',
  zhalong: 'zhalong',
  '洞庭湖': 'dongting',
  '洞庭湖湿地': 'dongting',
  dongting: 'dongting',
  '鄱阳湖': 'poyang',
  '鄱阳湖湿地': 'poyang',
  poyang: 'poyang'
}

const normalizeCategories = (tags) => {
  const source = String(tags || '')
    .split(/[、,，/|\s]+/)
    .map((item) => item.trim())
    .filter(Boolean)
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

const currentTheme = computed(() => CATEGORY_DEFS[activeTag.value] || CATEGORY_DEFS.all)
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

const resolvePinnedRank = (wetlandName) => {
  const normalizedName = String(wetlandName || '').trim()
  return FEATURED_WETLAND_KEYWORDS.findIndex((keyword) => normalizedName.includes(keyword))
}

const sortWetlandsForDisplay = (items) => (
  [...items].sort((left, right) => {
    const leftName = String(left.wetlandName || '')
    const rightName = String(right.wetlandName || '')
    const leftPinnedRank = resolvePinnedRank(leftName)
    const rightPinnedRank = resolvePinnedRank(rightName)
    const leftPinned = leftPinnedRank >= 0
    const rightPinned = rightPinnedRank >= 0

    if (leftPinned && rightPinned) return leftPinnedRank - rightPinnedRank
    if (leftPinned) return -1
    if (rightPinned) return 1
    return pinyinCollator.compare(leftName, rightName)
  })
)

const filteredWetlands = computed(() => (
  sortWetlandsForDisplay((activeTag.value === 'all'
    ? wetlands.value
    : wetlands.value.filter((wetland) => wetland.categories.includes(activeTag.value)))
    .filter((wetland) => {
      if (!searchKeyword.value) return true
      const keyword = searchKeyword.value.toLowerCase()
      const haystack = [
        wetland.wetlandName,
        wetland.scenicId,
        wetland.scenicSlug,
        wetland.slug,
        wetland.pinyin,
        wetland.imagePath
      ]
        .map((item) => String(item || '').toLowerCase())
        .join(' ')
      return haystack.includes(keyword)
    }))
))

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

const getBadgeStyle = (tag) => {
  const theme = CATEGORY_DEFS[tag] || CATEGORY_DEFS.all
  return {
    borderColor: `rgba(${theme.accentRgb}, 0.22)`,
    color: theme.accentStrong
  }
}

const getCardStyle = (tag) => {
  const theme = CATEGORY_DEFS[tag] || CATEGORY_DEFS.all
  return {
    '--card-accent': theme.accent,
    '--card-accent-rgb': theme.accentRgb
  }
}

const normalizeLookupValue = (value) => String(value || '').trim().toLowerCase()

const slugifyScenicId = (value) => String(value || '')
  .trim()
  .toLowerCase()
  .replace(/[_\s]+/g, '-')
  .replace(/[^a-z0-9-]/g, '')
  .replace(/-+/g, '-')
  .replace(/^-|-$/g, '')

function resolveScenicRouteId(wetland) {
  const directCandidates = [
    wetland.scenicId,
    wetland.scenicSlug,
    wetland.slug,
    wetland.pinyin
  ]

  for (const candidate of directCandidates) {
    const normalized = slugifyScenicId(candidate)
    if (normalized) return normalized
  }

  const mappedCandidates = [
    wetland.wetlandName,
    wetland.pinyin
  ]

  for (const candidate of mappedCandidates) {
    const mapped = KNOWN_SCENIC_ID_MAP[normalizeLookupValue(candidate)]
    if (mapped) return mapped
  }

  return DEFAULT_SCENIC_ID
}

function buildConsultDetailPath(wetland) {
  return buildScenicPagePath(resolveScenicRouteId(wetland), 'home')
}

const goToPage = (page) => {
  currentPage.value = page
}

const goToPreviousPage = () => {
  if (currentPage.value > 1) currentPage.value -= 1
}

const goToNextPage = () => {
  if (currentPage.value < totalPages.value) currentPage.value += 1
}

const loadWetlands = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await api.get('/wetlands')
    const records = response.data?.wetlands || []
    wetlands.value = records.map((item) => {
      const categories = normalizeCategories(item.tags)
      const imagePath = item.imagePath || item.image || item.imageUrl || item.coverImage || item.poster || ''
      return {
        ...item,
        categories,
        primaryCategory: categories[0] || '内陆',
        image: resolveAssetUrl(imagePath, FALLBACK_WETLAND_IMAGE)
      }
    })
  } catch (error) {
    console.error('加载导览景区失败:', error)
    errorMessage.value = '景区数据加载失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

watch(activeTag, () => {
  currentPage.value = 1
})

watch(searchKeyword, () => {
  currentPage.value = 1
})

watch(filteredWetlands, (items) => {
  const maxPage = Math.max(1, Math.ceil(items.length / pageSize))
  if (currentPage.value > maxPage) currentPage.value = maxPage
})

onMounted(loadWetlands)
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&display=swap');

.consult-index-page {
  --theme-accent: #2d5967;
  --theme-accent-strong: #173843;
  --theme-accent-rgb: 45, 89, 103;
  --theme-text-primary: #142a31;
  --theme-text-secondary: rgba(20, 42, 49, 0.78);
  --panel-plain: rgba(255, 255, 255, 0.2);
  --panel-plain-strong: rgba(255, 255, 255, 0.32);
  --panel-line-soft: rgba(var(--theme-accent-rgb), 0.08);
  min-height: 100%;
  position: relative;
  color: var(--theme-text-primary);
  font-family: 'Manrope', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.consult-hero,
.consult-filter-bar,
.consult-search-bar,
.state-card,
.pagination-bar,
.consult-card {
  border: 1px solid rgba(var(--theme-accent-rgb), 0.14);
  background: rgba(255, 255, 255, 0.74);
  backdrop-filter: blur(10px);
  position: relative;
  z-index: 1;
}

.consult-hero {
  display: grid;
  gap: 22px;
  padding: 36px 34px 30px;
  margin-bottom: 16px;
  text-align: center;
  border-color: transparent;
  background: var(--panel-plain);
  backdrop-filter: none;
}

.consult-hero__eyebrow,
.ledger-row span,
.detail-link,
.consult-entry-button span {
  letter-spacing: 0.03em;
}

.consult-hero__eyebrow {
  margin: 0 0 12px;
  font-size: 0.86rem;
  font-weight: 600;
  color: var(--theme-text-secondary);
}

.consult-hero h1,
.consult-card h2 {
  margin: 0;
  color: var(--theme-accent-strong);
  font-weight: 600;
}

.consult-hero h1 {
  font-size: clamp(2.55rem, 4vw, 4.05rem);
  line-height: 1.16;
}

.consult-hero__desc {
  max-width: 760px;
  margin: 0 auto;
  font-size: 0.94rem;
  line-height: 1.78;
  color: var(--theme-text-secondary);
}

.consult-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  justify-content: center;
}

.consult-entry-button {
  min-height: 52px;
  min-width: 172px;
  padding: 14px 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--panel-line-soft);
  background: var(--panel-plain-strong);
  transition: transform 0.24s ease, border-color 0.24s ease, background 0.24s ease;
}

.consult-entry-button span {
  color: var(--theme-text-primary);
  font-size: 0.9rem;
  font-weight: 600;
}

.consult-entry-button.is-active,
.consult-entry-button:hover {
  transform: translateY(-2px);
  border-color: rgba(var(--theme-accent-rgb), 0.22);
  background: rgba(255, 255, 255, 0.42);
}

.consult-filter-bar {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  padding: 0;
  margin: 0 auto 16px;
  max-width: 1160px;
  border: none;
  background: transparent;
  backdrop-filter: none;
}

.consult-search-bar {
  margin: 0 auto 32px;
  padding: 18px;
  display: flex;
  align-items: end;
  gap: 14px;
  max-width: 1160px;
  border-color: transparent;
  background: var(--panel-plain);
  backdrop-filter: none;
}

.consult-search-field {
  flex: 1 1 auto;
  display: grid;
  gap: 10px;
}

.consult-search-field__label {
  font-size: 0.88rem;
  font-weight: 600;
  letter-spacing: 0.03em;
  color: var(--theme-text-secondary);
}

.consult-search-field__input {
  width: 100%;
  min-height: 52px;
  border: 1px solid var(--panel-line-soft);
  background: rgba(255, 255, 255, 0.52);
  padding: 0 16px;
  color: var(--theme-text-primary);
  font-size: 0.88rem;
  outline: none;
  transition: border-color 0.24s ease, background 0.24s ease, box-shadow 0.24s ease;
}

.consult-search-field__input:focus {
  border-color: rgba(var(--theme-accent-rgb), 0.22);
  background: rgba(255, 255, 255, 0.64);
  box-shadow: 0 0 0 3px rgba(var(--theme-accent-rgb), 0.06);
}

.consult-search-field__input::placeholder {
  color: var(--theme-text-secondary);
}

.consult-search-reset {
  min-height: 52px;
  padding: 0 18px;
  border: 1px solid var(--panel-line-soft);
  background: var(--panel-plain-strong);
  color: var(--theme-text-primary);
  font-size: 0.88rem;
  font-weight: 600;
  letter-spacing: 0.03em;
  cursor: pointer;
  transition: transform 0.24s ease, border-color 0.24s ease, background 0.24s ease;
}

.consult-search-reset:hover {
  transform: translateY(-2px);
  border-color: rgba(var(--theme-accent-rgb), 0.2);
  background: rgba(255, 255, 255, 0.42);
}

.consult-filter-button {
  border: 1px solid var(--panel-line-soft);
  background: var(--panel-plain-strong);
  padding: 16px 18px;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  transition: transform 0.24s ease, border-color 0.24s ease, background 0.24s ease;
}

.consult-filter-button span {
  color: var(--theme-text-primary);
  font-size: 0.94rem;
  letter-spacing: 0.04em;
}

.consult-filter-button.is-active,
.consult-filter-button:hover {
  transform: translateY(-2px);
  border-color: rgba(var(--theme-accent-rgb), 0.2);
  background: rgba(255, 255, 255, 0.42);
}

.state-card {
  padding: 24px;
}

.consult-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  position: relative;
  z-index: 1;
}

.consult-card {
  display: grid;
  grid-template-columns: 1fr;
  grid-template-rows: auto 1fr;
  overflow: hidden;
  transition: transform 0.28s ease, border-color 0.28s ease, box-shadow 0.28s ease;
  min-height: 100%;
}

.consult-card:hover {
  transform: translateY(-3px);
  border-color: rgba(var(--card-accent-rgb), 0.34);
  box-shadow: 0 24px 44px rgba(var(--card-accent-rgb), 0.1);
}

.consult-card__media {
  aspect-ratio: 1.08 / 0.86;
  background: #d5ddde;
}

.consult-card__media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: saturate(0.88);
}

.consult-card__body {
  display: grid;
  align-content: start;
  gap: 14px;
  padding: 18px 18px 20px;
}

.consult-card__masthead {
  display: flex;
  justify-content: flex-start;
  gap: 12px;
  align-items: start;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(var(--card-accent-rgb), 0.12);
}

.consult-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-start;
}

.tag-chip {
  padding: 5px 10px;
  border: 1px solid transparent;
  font-size: 0.72rem;
  font-weight: 600;
  letter-spacing: 0.03em;
  background: rgba(255, 255, 255, 0.72);
}

.consult-card h2 {
  font-size: clamp(1.24rem, 1.5vw, 1.56rem);
  line-height: 1.32;
}

.consult-card__subtitle,
.consult-card__desc {
  margin: 0;
}

.consult-card__subtitle {
  font-size: 0.86rem;
  font-weight: 500;
  color: rgba(var(--card-accent-rgb), 0.86);
}

.consult-card__desc {
  font-size: 0.86rem;
  line-height: 1.72;
  color: var(--theme-text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.consult-card__ledger {
  display: grid;
  gap: 8px;
}

.ledger-row {
  display: grid;
  grid-template-columns: 60px minmax(0, 1fr);
  gap: 10px;
  padding-top: 10px;
  border-top: 1px solid rgba(var(--card-accent-rgb), 0.08);
}

.ledger-row span {
  font-size: 0.78rem;
  color: var(--theme-text-secondary);
}

.ledger-row strong {
  font-size: 0.86rem;
  line-height: 1.66;
  font-weight: 500;
}

.consult-card__action {
  margin-top: 4px;
  padding-top: 12px;
  border-top: 1px solid rgba(var(--card-accent-rgb), 0.12);
  display: flex;
  justify-content: flex-start;
}

.detail-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  padding: 0 18px;
  border: 1px solid rgba(var(--card-accent-rgb), 0.2);
  background: rgba(255, 255, 255, 0.82);
  color: rgb(var(--card-accent-rgb));
  font-size: 0.8rem;
  font-weight: 600;
  transition: transform 0.24s ease, border-color 0.24s ease, background 0.24s ease;
}

.detail-link:hover {
  transform: translateY(-2px);
  border-color: rgba(var(--card-accent-rgb), 0.42);
  background: rgba(255, 255, 255, 0.95);
}

.pagination-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-top: 28px;
  padding: 20px 22px;
  flex-wrap: wrap;
}

.pagination-info {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
}

.pagination-status {
  font-size: 0.92rem;
  font-weight: 600;
  color: var(--theme-text-primary);
}

.pagination-button,
.page-number {
  border: 1px solid rgba(var(--theme-accent-rgb), 0.2);
  background: rgba(255, 255, 255, 0.84);
  color: var(--theme-text-primary);
  cursor: pointer;
  transition: transform 0.24s ease, border-color 0.24s ease, background 0.24s ease;
}

.pagination-button {
  min-width: 104px;
  padding: 12px 18px;
  font-size: 0.86rem;
  font-weight: 600;
  letter-spacing: 0.03em;
}

.page-number {
  min-width: 42px;
  height: 42px;
  padding: 0 12px;
  font-size: 0.94rem;
  font-weight: 700;
}

.pagination-button:hover:not(:disabled),
.page-number:hover {
  transform: translateY(-2px);
  border-color: rgba(var(--theme-accent-rgb), 0.38);
}

.pagination-button:disabled {
  opacity: 0.42;
  cursor: not-allowed;
}

.page-number.is-active {
  background: rgb(var(--theme-accent-rgb));
  color: #fff;
  border-color: rgb(var(--theme-accent-rgb));
}

@media (max-width: 1280px) {
  .consult-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .consult-index-page {
    width: 100%;
  }

  .consult-filter-bar,
  .consult-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .consult-hero,
  .consult-filter-bar,
  .consult-search-bar,
  .state-card,
  .consult-card__body,
  .pagination-bar {
    padding: 18px;
  }

  .consult-hero h1 {
    font-size: clamp(2rem, 9vw, 3rem);
  }

  .consult-search-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .consult-card__action,
  .pagination-bar {
    justify-content: center;
  }

  .consult-search-reset {
    width: 100%;
  }

  .consult-filter-bar,
  .consult-grid {
    grid-template-columns: 1fr;
  }
}
</style>
